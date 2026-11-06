package com.meongcare.app.ui.link

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.meongcare.app.domain.link.LinkCheck
import com.meongcare.app.domain.link.LinkCode
import com.meongcare.app.domain.link.LinkStart
import com.meongcare.app.domain.link.OwnerLinkService
import com.meongcare.app.domain.link.isExpired
import com.meongcare.app.domain.link.remainingMinutesOf
import com.meongcare.app.domain.pet.PetProfileLoad
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 보호자 웹 연결 화면 (014). TV 로그인 방식: 차가 코드를 보이고, 보호자가 폰 웹에 입력한다.
 *
 * - [open]: 코드를 만든다. 열려 있는 동안 [isOpen]이 true다 (처음 화면 대신 연결 화면).
 * - **[uiState]를 구독하는 동안에만** [pollIntervalMillis]마다 연결됐는지 묻는다. 주행 화면으로 바뀌면 연결 화면이
 *   그려지지 않아 구독이 끊기고 묻기도 멈춘다 (주행 중에는 연결 화면을 보이지 않는다, 폴링 최소화).
 * - 만료(차 시계로 10분)·연결·닫기에 묻기를 멈춘다. 닫거나 새 코드를 받으면 쓰지 않은 코드를 지운다.
 *
 * @param petProfile 연결됨 화면에 반려견 이름을 보이려고 받는다 (`LinkedPetProfileSource`)
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LinkViewModel(
    private val link: OwnerLinkService,
    private val petProfile: Flow<PetProfileLoad>,
    private val now: () -> Instant = Instant::now,
    private val pollIntervalMillis: Long = POLL_INTERVAL_MILLIS,
) : ViewModel() {

    private sealed interface Phase {
        data object Closed : Phase
        data object Creating : Phase
        data class Waiting(val code: LinkCode, val createdAt: Instant) : Phase
        data object Linked : Phase
        data object Expired : Phase
        data object Failed : Phase
    }

    private val phase = MutableStateFlow<Phase>(Phase.Closed)
    private var createJob: Job? = null

    /** 연결 화면이 열려 있는지. 처음 화면 대신 연결 화면을 보인다 (`parkedContentOf`) */
    val isOpen: StateFlow<Boolean> = phase
        .map { it != Phase.Closed }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val uiState: StateFlow<LinkUiState> = phase
        .flatMapLatest { current ->
            when (current) {
                is Phase.Waiting -> waiting(current)
                Phase.Linked -> petProfile.map<PetProfileLoad, PetProfileLoad?> { it }.onStart { emit(null) }
                    .map { LinkUiState.Linked(linkedPetOf(it)) }
                else -> flowOf(staticUiOf(current))
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(0), staticUiOf(phase.value))

    /** [보호자 웹과 연결]: 이미 열려 있으면 아무 일도 없다 */
    fun open() {
        if (phase.value != Phase.Closed) return
        create()
    }

    /** [새 코드]·[다시 시도]: 쓰지 않은 코드를 지우고 새로 만든다 */
    fun onNewCode() {
        discardWaitingCode()
        create()
    }

    /** 처음 화면의 [연결 끊기] → 확인 [끊기] */
    fun unlink() {
        viewModelScope.launch { link.unlink() }
    }

    /** [닫기]·[확인]·시스템 뒤로: 쓰지 않은 코드를 지우고 닫는다 */
    fun close() {
        createJob?.cancel()
        discardWaitingCode()
        phase.value = Phase.Closed
    }

    private fun create() {
        createJob?.cancel()
        phase.value = Phase.Creating
        createJob = viewModelScope.launch {
            phase.value = when (val result = link.start()) {
                is LinkStart.Created -> Phase.Waiting(result.code, result.createdAt)
                is LinkStart.Failed -> Phase.Failed
            }
        }
    }

    private fun discardWaitingCode() {
        val waiting = phase.value as? Phase.Waiting ?: return
        // 화면이 사라져도 지우기는 끝까지 한다 (실패해도 만료되면 아무도 쓰지 못한다)
        viewModelScope.launch { link.cancel(waiting.code) }
    }

    /** 기다리는 동안: 남은 시간을 보이고, [pollIntervalMillis]마다 묻는다. 구독이 끊기면 이 흐름도 멈춘다 */
    private fun waiting(current: Phase.Waiting): Flow<LinkUiState> = flow {
        while (true) {
            val at = now()
            if (isExpired(current.createdAt, at)) {
                link.cancel(current.code)
                phase.value = Phase.Expired
                return@flow
            }
            emit(LinkUiState.Waiting(current.code.display, shownMinutesOf(remainingMinutesOf(current.createdAt, at))))
            when (link.check(current.code)) {
                LinkCheck.Linked -> {
                    phase.value = Phase.Linked
                    return@flow
                }
                LinkCheck.Gone -> {
                    phase.value = Phase.Expired
                    return@flow
                }
                // 실패는 잠깐 닿지 못한 것일 수 있다. 화면은 그대로 두고 다음에 다시 묻는다
                LinkCheck.Waiting, is LinkCheck.Failed -> Unit
            }
            delay(pollIntervalMillis)
        }
    }

    private fun staticUiOf(current: Phase): LinkUiState = when (current) {
        Phase.Closed -> LinkUiState.Closed
        Phase.Creating -> LinkUiState.Creating
        is Phase.Waiting -> LinkUiState.Waiting(
            current.code.display,
            shownMinutesOf(remainingMinutesOf(current.createdAt, now())),
        )
        Phase.Linked -> LinkUiState.Linked(pet = null)
        Phase.Expired -> LinkUiState.Expired
        Phase.Failed -> LinkUiState.Failed
    }

    companion object {
        /** 연결 화면이 보이는 동안 연결됐는지 묻는 간격 (계획 014 K4) */
        const val POLL_INTERVAL_MILLIS = 2_000L

        fun factory(link: OwnerLinkService, petProfile: Flow<PetProfileLoad>): ViewModelProvider.Factory = viewModelFactory {
            initializer { LinkViewModel(link, petProfile) }
        }
    }
}
