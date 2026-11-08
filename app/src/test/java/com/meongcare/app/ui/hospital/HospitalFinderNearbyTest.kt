package com.meongcare.app.ui.hospital

import com.meongcare.app.domain.guidance.Destination
import com.meongcare.app.domain.hospital.Congestion
import com.meongcare.app.domain.hospital.Hospital
import com.meongcare.app.domain.hospital.HospitalBoard
import com.meongcare.app.domain.hospital.HospitalBoardSource
import com.meongcare.app.domain.hospital.HospitalCandidate
import com.meongcare.app.domain.hospital.HospitalOrigin
import com.meongcare.app.domain.hospital.HospitalStatus
import com.meongcare.app.domain.hospital.NearbyVetSource
import com.meongcare.app.domain.hospital.NearbyVets
import com.meongcare.app.domain.hospital.exampleStatusOf
import com.meongcare.app.domain.location.CurrentLocation
import com.meongcare.app.domain.location.CurrentLocationSource
import com.meongcare.app.testutil.MainDispatcherRule
import java.time.Instant
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** 계획 015 "설계 결정 4"의 ViewModel 계약 V1~V9. */
@OptIn(ExperimentalCoroutinesApi::class)
class HospitalFinderNearbyTest {

    private val dispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(dispatcher)

    private val now = Instant.parse("2026-11-08T05:00:00Z")
    private val here = CurrentLocation(37.4127212, 127.094681, "수정구 시흥동")

    private val partner = HospitalCandidate(
        Hospital(Destination("제휴", 37.43, 127.0947), null, isSample = true, id = "a"),
        HospitalStatus(10, Congestion.NORMAL, true, now),
        HospitalOrigin.SERVER,
    )
    private val board = HospitalBoard.Loaded(listOf(partner), HospitalOrigin.SERVER)

    private fun vet(name: String, km: Double) =
        Hospital(Destination(name, here.latitude + km / 111.195, here.longitude), "031-000-0000", isSample = false)

    private class Boards(var answer: HospitalBoard) : HospitalBoardSource {
        var calls = 0
        override suspend fun load(): HospitalBoard {
            calls++
            return answer
        }
    }

    private class Locations(var answer: CurrentLocation?) : CurrentLocationSource {
        override suspend fun current(): CurrentLocation? = answer
    }

    /** 부를 때마다 [answers]에서 하나씩 꺼낸다. 비면 끝없이 기다린다 */
    private class Nearby(vararg answers: CompletableDeferred<NearbyVets>) : NearbyVetSource {
        val queue = ArrayDeque(answers.toList())
        val centers = mutableListOf<CurrentLocation>()
        override suspend fun search(center: CurrentLocation): NearbyVets {
            centers += center
            return queue.removeFirstOrNull()?.await() ?: awaitCancellation()
        }
    }

    private fun vm(nearby: NearbyVetSource?, location: CurrentLocation? = here, boards: Boards = Boards(board)) =
        HospitalFinderViewModel(boards, Locations(location), now = { now }, nearbySource = nearby)

    private fun HospitalFinderViewModel.loaded() = uiState.value as HospitalFinderUiState.Loaded

    @Test
    fun `V1 근처 출처가 없으면 늘 Off다`() = runTest(dispatcher) {
        val vm = vm(nearby = null)
        backgroundScope.launch { vm.uiState.collect {} }
        runCurrent()
        assertEquals(NearbyState.Off, vm.loaded().nearby)
        advanceTimeBy(31.seconds)
        runCurrent()
        assertEquals(NearbyState.Off, vm.loaded().nearby)
    }

    @Test
    fun `V2 제휴 목록이 근처 결과를 기다리지 않고 먼저 보이고 결과가 오면 붙는다`() = runTest(dispatcher) {
        val answer = CompletableDeferred<NearbyVets>()
        val vm = vm(Nearby(answer))
        backgroundScope.launch { vm.uiState.collect {} }
        runCurrent()

        val first = vm.loaded()
        assertEquals(NearbyState.Searching, first.nearby)
        assertEquals("제휴", first.ranking.ranked.single().hospital.name)

        answer.complete(NearbyVets.Found(listOf(vet("나", 2.0), vet("가", 1.0))))
        runCurrent()
        val rows = (vm.loaded().nearby as NearbyState.Found).rows
        assertEquals(listOf("가", "나"), rows.map { it.hospital.name })
        // 설계 변경 V2: 근처 병원은 예시 대기를 달고 제휴 병원과 한 순위에 섞인다
        val ranked = vm.loaded().ranking.ranked
        assertEquals(setOf("제휴", "가", "나"), ranked.map { it.hospital.name }.toSet())
        assertEquals(ranked.sortedBy { it.total }.map { it.hospital.name }, ranked.map { it.hospital.name })
        ranked.filter { it.candidate.origin == HospitalOrigin.NEARBY_SEARCH }.forEach {
            assertEquals(exampleStatusOf(it.hospital, now), it.status)
        }
    }

    @Test
    fun `V3 실패하면 Failed이고 제휴 목록은 그대로다`() = runTest(dispatcher) {
        val vm = vm(Nearby(CompletableDeferred(NearbyVets.Unavailable("HTTP 429"))))
        backgroundScope.launch { vm.uiState.collect {} }
        runCurrent()
        assertEquals(NearbyState.Failed(), vm.loaded().nearby)
        assertEquals(HospitalOrigin.SERVER, vm.loaded().origin)
    }

    @Test
    fun `V4 8초가 지나면 실패로 본다`() = runTest(dispatcher) {
        val vm = vm(Nearby(CompletableDeferred()))
        backgroundScope.launch { vm.uiState.collect {} }
        runCurrent()
        advanceTimeBy(7.9.seconds)
        runCurrent()
        assertEquals(NearbyState.Searching, vm.loaded().nearby)
        advanceTimeBy(0.2.seconds)
        runCurrent()
        assertEquals(NearbyState.Failed(), vm.loaded().nearby)
    }

    @Test
    fun `V5 결과가 없거나 정리 뒤 0곳이면 Empty다`() = runTest(dispatcher) {
        val empty = vm(Nearby(CompletableDeferred(NearbyVets.Found(emptyList()))))
        backgroundScope.launch { empty.uiState.collect {} }
        runCurrent()
        assertEquals(NearbyState.Empty, empty.loaded().nearby)

        val far = vm(Nearby(CompletableDeferred(NearbyVets.Found(listOf(vet("먼 곳", 9.0))))))
        backgroundScope.launch { far.uiState.collect {} }
        runCurrent()
        assertEquals(NearbyState.Empty, far.loaded().nearby)
    }

    @Test
    fun `V6 위치를 모르면 찾지 않고 V7 위치를 얻으면 그때 찾는다`() = runTest(dispatcher) {
        val nearby = Nearby(CompletableDeferred(NearbyVets.Found(listOf(vet("가", 1.0)))))
        val locations = Locations(null)
        val vm = HospitalFinderViewModel(Boards(board), locations, now = { now }, nearbySource = nearby)
        backgroundScope.launch { vm.uiState.collect {} }
        runCurrent()
        assertEquals(NearbyState.NeedsLocation, vm.loaded().nearby)
        assertTrue(nearby.centers.isEmpty())

        locations.answer = here
        vm.onRelocate()
        runCurrent()
        assertEquals(listOf(here), nearby.centers)
        assertTrue(vm.loaded().nearby is NearbyState.Found)
    }

    @Test
    fun `V8 30초 갱신은 근처를 다시 찾지 않고 이전 결과를 둔다`() = runTest(dispatcher) {
        val nearby = Nearby(CompletableDeferred(NearbyVets.Found(listOf(vet("가", 1.0)))))
        val boards = Boards(board)
        val vm = vm(nearby, boards = boards)
        backgroundScope.launch { vm.uiState.collect {} }
        runCurrent()
        advanceTimeBy(31.seconds)
        runCurrent()
        assertEquals(2, boards.calls)
        assertEquals(1, nearby.centers.size)
        assertTrue(vm.loaded().nearby is NearbyState.Found)
    }

    @Test
    fun `V8 30초 갱신은 실패한 근처 검색도 다시 찾지 않는다`() = runTest(dispatcher) {
        val nearby = Nearby(CompletableDeferred(NearbyVets.Unavailable("x")))
        val vm = vm(nearby)
        backgroundScope.launch { vm.uiState.collect {} }
        runCurrent()
        advanceTimeBy(31.seconds)
        runCurrent()
        assertEquals(1, nearby.centers.size)
        assertEquals(NearbyState.Failed(), vm.loaded().nearby)
    }

    @Test
    fun `V9 새로 고침은 실패했을 때만 근처를 다시 찾는다`() = runTest(dispatcher) {
        val retry = CompletableDeferred<NearbyVets>()
        val nearby = Nearby(CompletableDeferred(NearbyVets.Unavailable("x")), retry)
        val vm = vm(nearby)
        backgroundScope.launch { vm.uiState.collect {} }
        runCurrent()
        assertEquals(NearbyState.Failed(), vm.loaded().nearby)

        vm.onRefresh()
        runCurrent()
        assertEquals(2, nearby.centers.size)
        assertEquals(NearbyState.Failed(isRetrying = true), vm.loaded().nearby)

        retry.complete(NearbyVets.Found(listOf(vet("가", 1.0))))
        runCurrent()
        assertTrue(vm.loaded().nearby is NearbyState.Found)

        vm.onRefresh()
        runCurrent()
        assertEquals("찾은 뒤에는 새로 고침해도 다시 찾지 않는다", 2, nearby.centers.size)
    }
}
