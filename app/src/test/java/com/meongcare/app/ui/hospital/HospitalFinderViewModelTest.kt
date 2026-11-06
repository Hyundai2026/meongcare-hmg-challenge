package com.meongcare.app.ui.hospital

import com.meongcare.app.domain.guidance.Destination
import com.meongcare.app.domain.hospital.Congestion
import com.meongcare.app.domain.hospital.Hospital
import com.meongcare.app.domain.hospital.HospitalBoard
import com.meongcare.app.domain.hospital.HospitalBoardSource
import com.meongcare.app.domain.hospital.HospitalCandidate
import com.meongcare.app.domain.hospital.HospitalOrigin
import com.meongcare.app.domain.hospital.HospitalStatus
import com.meongcare.app.domain.hospital.RankBasis
import com.meongcare.app.domain.location.CurrentLocation
import com.meongcare.app.domain.location.CurrentLocationSource
import com.meongcare.app.testutil.MainDispatcherRule
import java.time.Instant
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HospitalFinderViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(dispatcher)

    private val now = Instant.parse("2026-11-06T05:00:00Z")
    private val here = CurrentLocation(37.4127212, 127.094681, "수정구 시흥동")

    private val board = HospitalBoard.Loaded(
        listOf(
            HospitalCandidate(
                Hospital(Destination("가", 37.43, 127.0947), null, isSample = true, id = "a"),
                HospitalStatus(10, Congestion.NORMAL, true, now),
                HospitalOrigin.SERVER,
            ),
        ),
        HospitalOrigin.SERVER,
    )

    private class Boards(var answer: HospitalBoard) : HospitalBoardSource {
        var calls = 0
        override suspend fun load(): HospitalBoard {
            calls++
            return answer
        }
    }

    private class Locations(var answer: CompletableDeferred<CurrentLocation?> = CompletableDeferred()) : CurrentLocationSource {
        var calls = 0
        override suspend fun current(): CurrentLocation? {
            calls++
            return answer.await()
        }
    }

    @Test
    fun `열면 위치와 목록을 함께 받는다`() = runTest(dispatcher) {
        val boards = Boards(board)
        val locations = Locations()
        val vm = HospitalFinderViewModel(boards, locations, now = { now })
        backgroundScope.launch { vm.uiState.collect {} }
        runCurrent()

        // 위치를 기다리는 동안에도 목록은 이미 받았다 (차례로가 아니라 동시에)
        assertEquals(1, boards.calls)
        assertEquals(1, locations.calls)
        assertEquals(HospitalFinderUiState.Loading, vm.uiState.value)

        locations.answer.complete(here)
        runCurrent()
        val loaded = vm.uiState.value as HospitalFinderUiState.Loaded
        assertEquals(RankBasis.TRAVEL_PLUS_WAIT, loaded.ranking.basis)
        assertEquals(HospitalOrigin.SERVER, loaded.origin)
        assertEquals(listOf("a"), loaded.ranking.ranked.map { it.hospital.id })
    }

    @Test
    fun `화면이 보이는 동안 30초마다 목록만 다시 받는다`() = runTest(dispatcher) {
        val boards = Boards(board)
        val locations = Locations(CompletableDeferred(here))
        val vm = HospitalFinderViewModel(boards, locations, now = { now })
        backgroundScope.launch { vm.uiState.collect {} }
        runCurrent()
        assertEquals(1, boards.calls)

        advanceTimeBy(29.seconds)
        runCurrent()
        assertEquals(1, boards.calls)
        advanceTimeBy(2.seconds)
        runCurrent()
        assertEquals(2, boards.calls)
        advanceTimeBy(30.seconds)
        runCurrent()
        assertEquals(3, boards.calls)
        assertEquals(1, locations.calls)
    }

    @Test
    fun `화면이 사라지면 다시 받지 않는다`() = runTest(dispatcher) {
        val boards = Boards(board)
        val vm = HospitalFinderViewModel(boards, Locations(CompletableDeferred(here)), now = { now })
        val screen = backgroundScope.launch { vm.uiState.collect {} }
        runCurrent()
        screen.cancel()
        advanceTimeBy(5.seconds + 10_000.seconds)
        runCurrent()
        // 사라진 뒤 5초(WhileSubscribed) 안에 한 번 더 받을 수는 있지만, 그 뒤로는 받지 않는다
        assertTrue("받은 횟수 ${boards.calls}", boards.calls <= 1)
    }

    @Test
    fun `새로 고침은 목록을 바로 다시 받는다`() = runTest(dispatcher) {
        val boards = Boards(board)
        val vm = HospitalFinderViewModel(boards, Locations(CompletableDeferred(here)), now = { now })
        backgroundScope.launch { vm.uiState.collect {} }
        runCurrent()
        vm.onRefresh()
        runCurrent()
        assertEquals(2, boards.calls)
        assertTrue(vm.uiState.value is HospitalFinderUiState.Loaded)
    }

    @Test
    fun `위치를 모르면 대기 시간 순이고 위치 다시 찾기는 위치만 다시 묻는다`() = runTest(dispatcher) {
        val boards = Boards(board)
        val locations = Locations(CompletableDeferred<CurrentLocation?>().apply { complete(null) })
        val vm = HospitalFinderViewModel(boards, locations, now = { now })
        backgroundScope.launch { vm.uiState.collect {} }
        runCurrent()
        assertEquals(RankBasis.WAIT_ONLY, (vm.uiState.value as HospitalFinderUiState.Loaded).ranking.basis)

        locations.answer = CompletableDeferred()
        vm.onRelocate()
        runCurrent()
        assertTrue((vm.uiState.value as HospitalFinderUiState.Loaded).isLocating)
        locations.answer.complete(here)
        runCurrent()
        val loaded = vm.uiState.value as HospitalFinderUiState.Loaded
        assertEquals(RankBasis.TRAVEL_PLUS_WAIT, loaded.ranking.basis)
        assertEquals(false, loaded.isLocating)
        assertEquals(2, locations.calls)
        assertEquals(1, boards.calls)
    }

    @Test
    fun `목록을 못 받으면 다시 시도를 보이고 다시 시도하면 다시 받는다`() = runTest(dispatcher) {
        val boards = Boards(HospitalBoard.Unavailable("HTTP 403"))
        val vm = HospitalFinderViewModel(boards, Locations(CompletableDeferred(here)), now = { now })
        backgroundScope.launch { vm.uiState.collect {} }
        runCurrent()
        assertEquals(HospitalFinderUiState.Failed(), vm.uiState.value)

        boards.answer = board
        vm.onRefresh()
        runCurrent()
        assertEquals(2, boards.calls)
        assertTrue(vm.uiState.value is HospitalFinderUiState.Loaded)
    }
}
