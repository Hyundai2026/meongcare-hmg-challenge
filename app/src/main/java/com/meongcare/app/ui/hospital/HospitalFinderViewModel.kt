package com.meongcare.app.ui.hospital

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.meongcare.app.domain.hospital.Hospital
import com.meongcare.app.domain.hospital.HospitalBoard
import com.meongcare.app.domain.hospital.HospitalBoardSource
import com.meongcare.app.domain.hospital.NearbyVetSource
import com.meongcare.app.domain.hospital.NearbyVets
import com.meongcare.app.domain.hospital.RankingRules
import com.meongcare.app.domain.hospital.exampleStatusOf
import com.meongcare.app.domain.hospital.nearbyVetRowsOf
import com.meongcare.app.domain.hospital.rankHospitals
import com.meongcare.app.domain.location.CurrentLocation
import com.meongcare.app.domain.location.CurrentLocationSource
import java.time.Instant
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 병원 목록 화면 (계획 012 "설계 결정 8"의 2, 계획 015 "설계 결정 4").
 *
 * - 화면이 보이면 현재 위치와 병원 목록을 **동시에** 받는다 (둘 다 각자 시간 초과가 있다).
 * - 화면이 보이는 동안 [refreshEvery]마다 **목록만** 다시 받는다 (위치는 다시 묻지 않는다). [onRefresh]로 바로 받는다.
 * - 화면이 사라지면 멈춘다 (`WhileSubscribed`, ux-guidelines "폴링 최소화"). 주행 중에는 화면이 없으니 멈춘다.
 * - [onRelocate] ([위치 다시 찾기])는 위치만 다시 묻는다.
 * - 근처 동물병원([nearbySource], 015)은 제휴 목록과 **따로** 찾는다. 제휴 목록은 근처 결과를 기다리지 않는다.
 *   위치를 알 때 화면이 보일 때마다 한 번 찾고, 30초 갱신 때는 다시 찾지 않는다 (쿼터·폴링 최소화).
 *   실패했을 때만 [onRefresh]가 다시 찾는다. [nearbyTimeout]이 지나면 실패로 본다.
 *
 * 고르기·뒤로는 `HospitalTrip`이 맡는다 (이 ViewModel은 목록만 안다).
 */
class HospitalFinderViewModel(
    private val boardSource: HospitalBoardSource,
    private val locationSource: CurrentLocationSource,
    private val now: () -> Instant = Instant::now,
    private val refreshEvery: Duration = 30.seconds,
    private val nearbySource: NearbyVetSource? = null,
    private val nearbyTimeout: Duration = 8.seconds,
) : ViewModel() {

    private sealed interface Event {
        /** @param byUser [onRefresh]로 왔는지. false = 30초 갱신(ticker) */
        data class Refresh(val byUser: Boolean) : Event
        data object Relocate : Event
        data class NearbyDone(val result: NearbyVets) : Event
    }

    /** 근처 검색의 원재료. 줄 정리([nearbyVetRowsOf])는 제휴 목록·위치가 바뀔 때마다 다시 한다 (V12). */
    private sealed interface NearbyRaw {
        data object Off : NearbyRaw
        data object NeedsLocation : NearbyRaw
        data object Searching : NearbyRaw
        data class Done(val hospitals: List<Hospital>) : NearbyRaw
        data class Failed(val isRetrying: Boolean) : NearbyRaw
    }

    private val refreshes = Channel<Unit>(Channel.CONFLATED)
    private val relocations = Channel<Unit>(Channel.CONFLATED)

    /** 화면에 다시 돌아왔을 때 "불러오는 중"으로 깜빡이지 않게, 마지막으로 보인 목록을 들고 있는다 */
    private var lastShown: HospitalFinderUiState.Loaded? = null

    val uiState: StateFlow<HospitalFinderUiState> = flow {
        emit(lastShown?.copy(isRefreshing = true) ?: HospitalFinderUiState.Loading)
        coroutineScope {
            val nearbyResults = Channel<NearbyVets>(Channel.CONFLATED)
            var nearbyJob: Job? = null
            fun startNearby(source: NearbyVetSource, center: CurrentLocation) {
                nearbyJob?.cancel()
                nearbyJob = launch { nearbyResults.send(searchNearby(source, center)) }
            }

            val firstLocation = async { locationSource.current() }
            var board = boardSource.load()
            var location = firstLocation.await()
            var nearby: NearbyRaw = when {
                nearbySource == null -> NearbyRaw.Off
                location == null -> NearbyRaw.NeedsLocation
                else -> NearbyRaw.Searching
            }
            var shown = show(board, location, nearby)
            if (nearby == NearbyRaw.Searching) startNearby(nearbySource!!, location!!)

            val ticker = flow {
                while (true) {
                    delay(refreshEvery)
                    emit(Event.Refresh(byUser = false))
                }
            }
            merge(
                refreshes.receiveAsFlow().map { Event.Refresh(byUser = true) },
                relocations.receiveAsFlow().map { Event.Relocate },
                nearbyResults.receiveAsFlow().map { Event.NearbyDone(it) },
                ticker,
            ).collect { event ->
                when (event) {
                    is Event.Refresh -> {
                        // 근처는 사람이 누른 새로 고침에서, 실패했을 때만 다시 찾는다 (V9). 30초 갱신은 다시 찾지 않는다 (V8)
                        val retryNearby = event.byUser && nearby is NearbyRaw.Failed && location != null && nearbySource != null
                        if (retryNearby) nearby = NearbyRaw.Failed(isRetrying = true)
                        val current = shown
                        emit(
                            when (current) {
                                is HospitalFinderUiState.Loaded -> current.copy(isRefreshing = true, nearby = nearbyStateOf(nearby, board, location))
                                is HospitalFinderUiState.Failed -> HospitalFinderUiState.Failed(isRetrying = true)
                                HospitalFinderUiState.Loading -> current
                            },
                        )
                        if (retryNearby) startNearby(nearbySource!!, location!!)
                        board = boardSource.load()
                    }
                    Event.Relocate -> {
                        (shown as? HospitalFinderUiState.Loaded)?.let { emit(it.copy(isLocating = true)) }
                        location = locationSource.current()
                        val located = location
                        if (nearbySource != null) {
                            when {
                                located == null -> {
                                    nearbyJob?.cancel()
                                    nearby = NearbyRaw.NeedsLocation
                                }
                                // 위치를 몰라 못 찾았던 것만 지금 찾는다 (V7). 찾은 결과는 새 위치로 줄만 다시 정리한다
                                nearby == NearbyRaw.NeedsLocation -> {
                                    nearby = NearbyRaw.Searching
                                    startNearby(nearbySource, located)
                                }
                            }
                        }
                    }
                    is Event.NearbyDone -> {
                        nearby = when (val result = event.result) {
                            is NearbyVets.Found -> NearbyRaw.Done(result.hospitals)
                            is NearbyVets.Unavailable -> NearbyRaw.Failed(isRetrying = false)
                        }
                    }
                }
                shown = show(board, location, nearby)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = HospitalFinderUiState.Loading,
    )

    /** [새로 고침], [다시 시도], 근처 [다시 찾기] */
    fun onRefresh() {
        refreshes.trySend(Unit)
    }

    /** [위치 다시 찾기] */
    fun onRelocate() {
        relocations.trySend(Unit)
    }

    private suspend fun searchNearby(source: NearbyVetSource, center: CurrentLocation): NearbyVets = try {
        withTimeoutOrNull(nearbyTimeout) { source.search(center) } ?: NearbyVets.Unavailable("시간 초과")
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        NearbyVets.Unavailable("오류")
    }

    private fun nearbyStateOf(raw: NearbyRaw, board: HospitalBoard, location: CurrentLocation?): NearbyState = when (raw) {
        NearbyRaw.Off -> NearbyState.Off
        NearbyRaw.NeedsLocation -> NearbyState.NeedsLocation
        NearbyRaw.Searching -> NearbyState.Searching
        is NearbyRaw.Failed -> NearbyState.Failed(raw.isRetrying)
        is NearbyRaw.Done -> {
            if (location == null) {
                NearbyState.NeedsLocation
            } else {
                val partners = (board as? HospitalBoard.Loaded)?.candidates.orEmpty()
                val rows = nearbyVetRowsOf(raw.hospitals, location, partners)
                if (rows.isEmpty()) NearbyState.Empty else NearbyState.Found(rows)
            }
        }
    }

    private suspend fun FlowCollector<HospitalFinderUiState>.show(
        board: HospitalBoard,
        location: CurrentLocation?,
        nearby: NearbyRaw,
    ): HospitalFinderUiState {
        val state = when (board) {
            is HospitalBoard.Unavailable -> HospitalFinderUiState.Failed()
            is HospitalBoard.Loaded -> {
                val at = now()
                val nearbyState = nearbyStateOf(nearby, board, location)
                // 설계 변경 V2: 근처 병원에 예시 대기를 붙여 제휴 병원과 한 목록에서 이동 + 대기 순으로 섞는다
                val nearbyCandidates = (nearbyState as? NearbyState.Found)?.rows.orEmpty()
                    .map { it.candidate.copy(status = exampleStatusOf(it.hospital, at)) }
                HospitalFinderUiState.Loaded(
                    ranking = rankHospitals(board.candidates + nearbyCandidates, location, at, MERGED_RULES),
                    origin = board.origin,
                    nearby = nearbyState,
                ).also { lastShown = it }
            }
        }
        emit(state)
        return state
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        /** 제휴 + 근처를 한 목록에 섞으므로 묶음당 개수를 넉넉히 둔다. 화면은 5개씩 쪽으로 나눈다 */
        private val MERGED_RULES = RankingRules(maxPerGroup = 50)

        fun factory(
            boardSource: HospitalBoardSource,
            locationSource: CurrentLocationSource,
            nearbySource: NearbyVetSource? = null,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { HospitalFinderViewModel(boardSource, locationSource, nearbySource = nearbySource) }
        }
    }
}
