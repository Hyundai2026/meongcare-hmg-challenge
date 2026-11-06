package com.meongcare.app.ui.hospital

import com.meongcare.app.domain.guidance.Destination
import com.meongcare.app.domain.hospital.Congestion
import com.meongcare.app.domain.hospital.Hospital
import com.meongcare.app.domain.hospital.HospitalBoard
import com.meongcare.app.domain.hospital.HospitalBoardSource
import com.meongcare.app.domain.hospital.HospitalCandidate
import com.meongcare.app.domain.hospital.HospitalOrigin
import com.meongcare.app.domain.hospital.HospitalStatus
import com.meongcare.app.domain.hospital.StatusFreshness
import com.meongcare.app.domain.location.CurrentLocation
import com.meongcare.app.domain.location.CurrentLocationSource
import com.meongcare.app.testutil.MainDispatcherRule
import java.time.Instant
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * 병원 목록 화면 상태 경계. 기준: 계획 012 "설계 결정 8"의 2(받는 동안 문구, 30초마다 목록만, 화면이 사라지면 멈춤,
 * Real 실패 → 다시 시도, 위치 다시 찾기), `HospitalFinderViewModel`·`HospitalFinderUiState` KDoc
 * (다시 받는 동안 이전 목록을 그대로 보인다, 돌아왔을 때 깜빡이지 않는다).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HospitalFinderViewModelBoundaryTest {

    private val dispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(dispatcher)

    private val t0 = Instant.parse("2026-11-06T05:00:00Z")
    private val here = CurrentLocation(37.4127212, 127.094681, "수정구 시흥동")

    private fun board(vararg ids: String, updatedAt: Instant = t0, origin: HospitalOrigin = HospitalOrigin.SERVER) = HospitalBoard.Loaded(
        ids.map {
            HospitalCandidate(
                Hospital(Destination(it, 37.43, 127.0947), null, isSample = true, id = it),
                HospitalStatus(10, Congestion.NORMAL, true, updatedAt),
                origin,
            )
        },
        origin,
    )

    /** 부를 때마다 [next]의 답을 쓴다. [next]가 null이면 [gate]가 끝날 때까지 받는 중이다. */
    private class Boards(var next: HospitalBoard?) : HospitalBoardSource {
        var calls = 0
        var gate = CompletableDeferred<HospitalBoard>()
        override suspend fun load(): HospitalBoard {
            calls++
            return next ?: gate.await()
        }
    }

    private class Locations(var next: CurrentLocation?, var hang: Boolean = false) : CurrentLocationSource {
        var calls = 0
        val gate = CompletableDeferred<CurrentLocation?>()
        override suspend fun current(): CurrentLocation? {
            calls++
            return if (hang) gate.await() else next
        }
    }

    private fun TestScope.viewModel(boards: Boards, locations: Locations) =
        HospitalFinderViewModel(boards, locations, now = { t0.plusMillis(testScheduler.currentTime) })

    private fun TestScope.show(vm: HospitalFinderViewModel) = backgroundScope.launch { vm.uiState.collect {} }

    private fun HospitalFinderViewModel.loaded() = uiState.value as HospitalFinderUiState.Loaded

    @Test
    fun `새로 고침하는 동안 이전 목록을 그대로 보이고 받으면 새 목록이다`() = runTest(dispatcher) {
        val boards = Boards(board("a"))
        val vm = viewModel(boards, Locations(here))
        show(vm)
        runCurrent()
        assertEquals(listOf("a"), vm.loaded().ranking.ranked.map { it.hospital.id })

        boards.next = null
        vm.onRefresh()
        runCurrent()
        val refreshing = vm.loaded()
        assertTrue(refreshing.isRefreshing)
        assertEquals(listOf("a"), refreshing.ranking.ranked.map { it.hospital.id })

        boards.gate.complete(board("a", "b"))
        runCurrent()
        val after = vm.loaded()
        assertEquals(false, after.isRefreshing)
        assertEquals(setOf("a", "b"), after.ranking.ranked.map { it.hospital.id }.toSet())
    }

    @Test
    fun `30초마다 다시 받을 때 지금 시각으로 다시 매겨 30분이 지난 정보는 뒤로 간다`() = runTest(dispatcher) {
        // 열 때 29분 45초 전 정보 → 30초 뒤 다시 받으면 30분 15초 전
        val boards = Boards(board("a", updatedAt = t0.minusSeconds(29 * 60 + 45)))
        val vm = viewModel(boards, Locations(here))
        show(vm)
        runCurrent()
        assertEquals(listOf("a"), vm.loaded().ranking.ranked.map { it.hospital.id })

        advanceTimeBy(30.seconds + 1.seconds)
        runCurrent()
        assertEquals(2, boards.calls)
        val ranking = vm.loaded().ranking
        assertTrue(ranking.ranked.isEmpty())
        assertEquals(StatusFreshness.STALE, ranking.unranked.single().freshness)
    }

    @Test
    fun `다시 받기가 실패하면 다시 시도를 보이고 다음에 받으면 목록으로 돌아온다`() = runTest(dispatcher) {
        val boards = Boards(board("a"))
        val vm = viewModel(boards, Locations(here))
        show(vm)
        runCurrent()

        boards.next = HospitalBoard.Unavailable("HTTP 503")
        vm.onRefresh()
        runCurrent()
        assertEquals(HospitalFinderUiState.Failed(), vm.uiState.value)

        boards.next = board("a")
        vm.onRefresh()
        runCurrent()
        assertEquals(listOf("a"), vm.loaded().ranking.ranked.map { it.hospital.id })
    }

    @Test
    fun `다시 시도하는 동안 다시 시도 중으로 보인다`() = runTest(dispatcher) {
        val boards = Boards(HospitalBoard.Unavailable("HTTP 403"))
        val vm = viewModel(boards, Locations(here))
        show(vm)
        runCurrent()
        boards.next = null
        vm.onRefresh()
        runCurrent()
        assertEquals(HospitalFinderUiState.Failed(isRetrying = true), vm.uiState.value)
        boards.gate.complete(HospitalBoard.Unavailable("HTTP 403"))
        runCurrent()
        assertEquals(HospitalFinderUiState.Failed(isRetrying = false), vm.uiState.value)
    }

    @Test
    fun `실패 화면에서 위치 다시 찾기를 눌러도 실패 화면이고 목록은 다시 받지 않는다`() = runTest(dispatcher) {
        val boards = Boards(HospitalBoard.Unavailable("서버 설정 없음"))
        val locations = Locations(null)
        val vm = viewModel(boards, locations)
        show(vm)
        runCurrent()
        vm.onRelocate()
        runCurrent()
        assertEquals(HospitalFinderUiState.Failed(), vm.uiState.value)
        assertEquals(2, locations.calls)
        assertEquals(1, boards.calls)
    }

    @Test
    fun `서버 대신 내장 목록을 받으면 출처가 대체로 보인다`() = runTest(dispatcher) {
        val vm = viewModel(Boards(board("demo-fast", origin = HospitalOrigin.BUILT_IN_FALLBACK)), Locations(here))
        show(vm)
        runCurrent()
        assertEquals(HospitalOrigin.BUILT_IN_FALLBACK, vm.loaded().origin)
    }

    @Test
    fun `화면에 다시 돌아오면 불러오는 중 대신 마지막 목록을 보이며 위치와 목록을 다시 받는다`() = runTest(dispatcher) {
        val boards = Boards(board("a"))
        val locations = Locations(here)
        val vm = viewModel(boards, locations)
        val screen = show(vm)
        runCurrent()
        screen.cancel()
        advanceTimeBy(6.seconds) // WhileSubscribed(5초)가 지나 멈춘다
        runCurrent()

        locations.hang = true
        val seen = mutableListOf<HospitalFinderUiState>()
        backgroundScope.launch { vm.uiState.collect { seen += it } }
        runCurrent()

        assertTrue("보인 값: $seen", seen.none { it == HospitalFinderUiState.Loading })
        val shown = seen.last() as HospitalFinderUiState.Loaded
        assertTrue(shown.isRefreshing)
        assertEquals(listOf("a"), shown.ranking.ranked.map { it.hospital.id })
        assertEquals(2, locations.calls)
        assertEquals(2, boards.calls)
    }

    @Test
    fun `화면이 사라지면 30초 받기를 멈추고 다시 보이면 다시 30초마다 받는다`() = runTest(dispatcher) {
        val boards = Boards(board("a"))
        val vm = viewModel(boards, Locations(here))
        val screen = show(vm)
        runCurrent()
        advanceTimeBy(31.seconds)
        runCurrent()
        assertEquals(2, boards.calls)

        screen.cancel()
        advanceTimeBy(100.seconds)
        runCurrent()
        assertEquals(2, boards.calls)

        show(vm)
        runCurrent()
        assertEquals(3, boards.calls)
        advanceTimeBy(30.seconds)
        runCurrent()
        assertEquals(4, boards.calls)
    }

    @Test
    fun `위치를 다시 찾으면 같은 목록을 새 위치 기준으로 다시 매긴다`() = runTest(dispatcher) {
        val far = CurrentLocation(37.0, 127.0, null)
        val locations = Locations(far)
        val boards = Boards(board("a"))
        val vm = viewModel(boards, locations)
        show(vm)
        runCurrent()
        val before = vm.loaded().ranking.ranked.single().travel

        locations.next = here
        vm.onRelocate()
        runCurrent()
        val after = vm.loaded().ranking.ranked.single().travel
        assertNotEquals(before, after)
        assertEquals(1, boards.calls)
    }
}
