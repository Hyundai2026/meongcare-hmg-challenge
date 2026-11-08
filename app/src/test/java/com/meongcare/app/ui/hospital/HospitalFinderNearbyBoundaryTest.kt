package com.meongcare.app.ui.hospital

import com.meongcare.app.domain.guidance.Destination
import com.meongcare.app.domain.hospital.Congestion
import com.meongcare.app.domain.hospital.Hospital
import com.meongcare.app.domain.hospital.HospitalBoard
import com.meongcare.app.domain.hospital.HospitalBoardSource
import com.meongcare.app.domain.hospital.HospitalCandidate
import com.meongcare.app.domain.hospital.HospitalOrigin
import com.meongcare.app.domain.hospital.HospitalRanking
import com.meongcare.app.domain.hospital.HospitalStatus
import com.meongcare.app.domain.hospital.NearbyVetSource
import com.meongcare.app.domain.hospital.NearbyVets
import com.meongcare.app.domain.hospital.exampleStatusOf
import com.meongcare.app.domain.hospital.rankHospitals
import com.meongcare.app.domain.location.CurrentLocation
import com.meongcare.app.domain.location.CurrentLocationSource
import com.meongcare.app.testutil.MainDispatcherRule
import java.time.Instant
import kotlin.time.Duration.Companion.milliseconds
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** 계획 015 "설계 결정 4" V1~V12 + "설계 변경 V2"(한 목록 섞기)의 경계 (tester). */
@OptIn(ExperimentalCoroutinesApi::class)
class HospitalFinderNearbyBoundaryTest {

    private val dispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(dispatcher)

    private val now = Instant.parse("2026-11-08T05:00:00Z")
    private val here = CurrentLocation(37.4127212, 127.094681, "수정구 시흥동")
    private val kmPerDegree = 111.195

    /** 직선 약 1.92km → 이동 약 7분 + 대기 10분 = 17분 */
    private val partner = HospitalCandidate(
        Hospital(Destination("제휴", 37.43, 127.094681), null, isSample = true, id = "a"),
        HospitalStatus(10, Congestion.NORMAL, true, now),
        HospitalOrigin.SERVER,
    )
    private val board = HospitalBoard.Loaded(listOf(partner), HospitalOrigin.SERVER)

    private fun vet(name: String, km: Double, center: CurrentLocation = here) =
        Hospital(Destination(name, center.latitude + km / kmPerDegree, center.longitude), "031-000-0000", isSample = false)

    /** 예시 대기가 [accept]를 만족하는 근처 병원 (이름만 바꿔 가며 찾는다) */
    private fun vetWithWait(prefix: String, km: Double, accept: (Int) -> Boolean): Hospital =
        (1..500).asSequence().map { vet("$prefix$it", km) }.first { accept(exampleStatusOf(it, now).waitMinutes) }

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

    /** 부를 때마다 [answers]에서 하나씩 꺼낸다. 비면 취소될 때까지 기다린다. 취소된 수를 센다 */
    private class Nearby(vararg answers: CompletableDeferred<NearbyVets>) : NearbyVetSource {
        val queue = ArrayDeque(answers.toList())
        val centers = mutableListOf<CurrentLocation>()
        var cancelled = 0
        override suspend fun search(center: CurrentLocation): NearbyVets {
            centers += center
            try {
                return queue.removeFirstOrNull()?.await() ?: awaitCancellation()
            } catch (e: kotlin.coroutines.cancellation.CancellationException) {
                cancelled++
                throw e
            }
        }
    }

    private fun found(vararg hospitals: Hospital) = CompletableDeferred<NearbyVets>(NearbyVets.Found(hospitals.toList()))

    private fun vm(nearby: NearbyVetSource?, locations: Locations = Locations(here), boards: Boards = Boards(board)) =
        HospitalFinderViewModel(boards, locations, now = { now }, nearbySource = nearby)

    private fun HospitalFinderViewModel.loaded() = uiState.value as HospitalFinderUiState.Loaded

    private fun HospitalRanking.allNames() = (ranked + unranked + notAccepting).map { it.hospital.name }

    @Test
    fun `V1 근처 출처가 없으면 제휴 병원이 10곳을 넘어도 순위가 예전 규칙과 같다`() = runTest(dispatcher) {
        val many = (1..12).map {
            partner.copy(
                hospital = partner.hospital.copy(destination = partner.hospital.destination.copy(name = "제휴%02d".format(it)), id = "p$it"),
                status = HospitalStatus(it, Congestion.LOW, true, now),
            )
        }
        val vm = vm(nearby = null, boards = Boards(HospitalBoard.Loaded(many, HospitalOrigin.SERVER)))
        backgroundScope.launch { vm.uiState.collect {} }
        runCurrent()
        // 015 전 ViewModel은 기본 규칙(묶음당 10곳)으로 순위를 매겼다
        assertEquals(rankHospitals(many, here, now), vm.loaded().ranking)
        assertEquals(NearbyState.Off, vm.loaded().nearby)
    }

    @Test
    fun `V2 예시 대기가 짧은 근처 병원은 제휴 병원보다 앞서 1위가 될 수 있고 카드는 받지 못한다`() = runTest(dispatcher) {
        val fast = vetWithWait("빠른", 0.5) { it <= 10 }
        val slow = vetWithWait("느린", 0.5) { it >= 20 }
        val vm = vm(Nearby(found(slow, fast)))
        backgroundScope.launch { vm.uiState.collect {} }
        runCurrent()

        val ranking = vm.loaded().ranking
        assertEquals(listOf(fast.name, partner.hospital.name, slow.name), ranking.ranked.map { it.hospital.name })
        val first = ranking.fastest!!
        assertEquals(HospitalOrigin.NEARBY_SEARCH, first.candidate.origin)
        assertFalse(first.candidate.canReceiveCard)
        assertEquals(exampleStatusOf(fast, now), first.status)
        // 제휴 병원 줄은 바뀌지 않는다
        val partnerRow = ranking.ranked.single { it.hospital.name == partner.hospital.name }
        assertEquals(partner, partnerRow.candidate)
    }

    @Test
    fun `V2 섞은 목록에 근처 병원과 제휴 병원이 한 번씩만 나온다`() = runTest(dispatcher) {
        val vm = vm(Nearby(found(vet("가", 1.0), vet("가", 1.0), vet("나", 2.0))))
        backgroundScope.launch { vm.uiState.collect {} }
        runCurrent()
        val names = vm.loaded().ranking.allNames()
        assertEquals(names.toSet().size, names.size)
        assertEquals(setOf("제휴", "가", "나"), names.toSet())
    }

    @Test
    fun `근처 검색이 찾는 중이거나 실패이거나 0곳이면 순위에는 제휴 병원만 있다`() = runTest(dispatcher) {
        val searching = vm(Nearby())
        val failed = vm(Nearby(CompletableDeferred(NearbyVets.Unavailable("HTTP 500"))))
        val empty = vm(Nearby(found()))
        listOf(searching, failed, empty).forEach { v -> backgroundScope.launch { v.uiState.collect {} } }
        runCurrent()
        assertEquals(NearbyState.Searching, searching.loaded().nearby)
        assertEquals(NearbyState.Failed(), failed.loaded().nearby)
        assertEquals(NearbyState.Empty, empty.loaded().nearby)
        listOf(searching, failed, empty).forEach { assertEquals(listOf("제휴"), it.loaded().ranking.allNames()) }
    }

    @Test
    fun `V3 근처 출처가 예외를 던져도 Failed이고 제휴 목록은 그대로다`() = runTest(dispatcher) {
        val vm = vm(NearbyVetSource { throw IllegalStateException("x") })
        backgroundScope.launch { vm.uiState.collect {} }
        runCurrent()
        assertEquals(NearbyState.Failed(), vm.loaded().nearby)
        assertEquals(listOf("제휴"), vm.loaded().ranking.allNames())
        assertEquals(HospitalOrigin.SERVER, vm.loaded().origin)
    }

    @Test
    fun `V4 시간 초과면 근처 검색을 취소하고 Failed다`() = runTest(dispatcher) {
        val nearby = Nearby()
        val vm = vm(nearby)
        backgroundScope.launch { vm.uiState.collect {} }
        runCurrent()
        advanceTimeBy(8.seconds + 1.milliseconds)
        runCurrent()
        assertEquals(NearbyState.Failed(), vm.loaded().nearby)
        assertEquals(1, nearby.cancelled)
    }

    @Test
    fun `V6 위치를 모르면 새로 고침과 30초 갱신에도 근처를 찾지 않는다`() = runTest(dispatcher) {
        val nearby = Nearby(found(vet("가", 1.0)))
        val vm = vm(nearby, locations = Locations(null))
        backgroundScope.launch { vm.uiState.collect {} }
        runCurrent()
        vm.onRefresh()
        runCurrent()
        advanceTimeBy(31.seconds)
        runCurrent()
        assertTrue(nearby.centers.isEmpty())
        assertEquals(NearbyState.NeedsLocation, vm.loaded().nearby)
    }

    @Test
    fun `V7 위치를 다시 찾으면 새 위치를 중심으로 찾는다`() = runTest(dispatcher) {
        val elsewhere = CurrentLocation(37.5665, 126.978, null)
        val nearby = Nearby(found(vet("시청 근처", 1.0, center = elsewhere)))
        val locations = Locations(null)
        val vm = vm(nearby, locations = locations)
        backgroundScope.launch { vm.uiState.collect {} }
        runCurrent()

        locations.answer = elsewhere
        vm.onRelocate()
        runCurrent()
        assertEquals(listOf(elsewhere), nearby.centers)
        assertEquals(listOf("시청 근처"), (vm.loaded().nearby as NearbyState.Found).rows.map { it.hospital.name })
    }

    @Test
    fun `V7 위치를 다시 찾았는데 또 모르면 찾지 않고 NeedsLocation이다`() = runTest(dispatcher) {
        val nearby = Nearby(found(vet("가", 1.0)))
        val vm = vm(nearby, locations = Locations(null))
        backgroundScope.launch { vm.uiState.collect {} }
        runCurrent()
        vm.onRelocate()
        runCurrent()
        assertTrue(nearby.centers.isEmpty())
        assertEquals(NearbyState.NeedsLocation, vm.loaded().nearby)
    }

    @Test
    fun `찾은 뒤 위치를 다시 찾아 모르게 되면 근처 줄과 순위의 근처 병원이 사라진다`() = runTest(dispatcher) {
        val locations = Locations(here)
        val vm = vm(Nearby(found(vet("가", 1.0))), locations = locations)
        backgroundScope.launch { vm.uiState.collect {} }
        runCurrent()
        assertTrue(vm.loaded().nearby is NearbyState.Found)

        locations.answer = null
        vm.onRelocate()
        runCurrent()
        assertEquals(NearbyState.NeedsLocation, vm.loaded().nearby)
        assertEquals(listOf("제휴"), vm.loaded().ranking.allNames())
    }

    @Test
    fun `V8 30초 갱신 중에도 찾는 중인 근처 검색은 계속되고 결과가 붙는다`() = runTest(dispatcher) {
        val answer = CompletableDeferred<NearbyVets>()
        val nearby = Nearby(answer)
        // 갱신 간격을 시간 초과(8초)보다 짧게 두어, 찾는 중에 갱신이 오게 한다
        val boards = Boards(board)
        val vm = HospitalFinderViewModel(boards, Locations(here), now = { now }, refreshEvery = 3.seconds, nearbySource = nearby)
        backgroundScope.launch { vm.uiState.collect {} }
        runCurrent()
        advanceTimeBy(3.seconds + 1.milliseconds)
        runCurrent()
        assertEquals(2, boards.calls)
        assertEquals(NearbyState.Searching, vm.loaded().nearby)

        answer.complete(NearbyVets.Found(listOf(vet("가", 1.0))))
        runCurrent()
        assertEquals(1, nearby.centers.size)
        assertTrue(vm.loaded().nearby is NearbyState.Found)
    }

    @Test
    fun `V9 0곳일 때 새로 고침해도 근처를 다시 찾지 않는다`() = runTest(dispatcher) {
        val nearby = Nearby(found())
        val boards = Boards(board)
        val vm = vm(nearby, boards = boards)
        backgroundScope.launch { vm.uiState.collect {} }
        runCurrent()
        vm.onRefresh()
        runCurrent()
        assertEquals(2, boards.calls)
        assertEquals(1, nearby.centers.size)
        assertEquals(NearbyState.Empty, vm.loaded().nearby)
    }

    @Test
    fun `V9 찾는 중에 새로 고침해도 두 번째 검색을 시작하지 않는다`() = runTest(dispatcher) {
        val nearby = Nearby()
        val vm = vm(nearby)
        backgroundScope.launch { vm.uiState.collect {} }
        runCurrent()
        vm.onRefresh()
        runCurrent()
        assertEquals(1, nearby.centers.size)
        assertEquals(0, nearby.cancelled)
        assertEquals(NearbyState.Searching, vm.loaded().nearby)
    }

    @Test
    fun `V9 다시 찾기도 실패하면 다시 찾기 버튼이 나오는 Failed로 돌아온다`() = runTest(dispatcher) {
        val nearby = Nearby(CompletableDeferred(NearbyVets.Unavailable("x")), CompletableDeferred(NearbyVets.Unavailable("y")))
        val vm = vm(nearby)
        backgroundScope.launch { vm.uiState.collect {} }
        runCurrent()
        vm.onRefresh()
        runCurrent()
        assertEquals(2, nearby.centers.size)
        assertEquals(NearbyState.Failed(isRetrying = false), vm.loaded().nearby)
        assertEquals(listOf("제휴"), vm.loaded().ranking.allNames())
    }

    @Test
    fun `V9 다시 찾는 동안에도 제휴 목록은 보이고 새로 고침이 끝난다`() = runTest(dispatcher) {
        val nearby = Nearby(CompletableDeferred(NearbyVets.Unavailable("x")))
        val vm = vm(nearby)
        backgroundScope.launch { vm.uiState.collect {} }
        runCurrent()
        vm.onRefresh()
        runCurrent()
        val state = vm.loaded()
        assertEquals(NearbyState.Failed(isRetrying = true), state.nearby)
        assertFalse(state.isRefreshing)
        assertEquals(listOf("제휴"), state.ranking.allNames())
    }

    @Test
    fun `V10 화면이 사라지면 진행 중인 근처 검색을 취소한다`() = runTest(dispatcher) {
        val nearby = Nearby()
        val vm = vm(nearby)
        val screen = backgroundScope.launch { vm.uiState.collect {} }
        runCurrent()
        assertEquals(1, nearby.centers.size)

        screen.cancel()
        advanceTimeBy(5.seconds + 1.milliseconds)
        runCurrent()
        assertEquals(1, nearby.cancelled)
    }

    @Test
    fun `V10 화면이 다시 보이면 찾은 결과가 있어도 처음처럼 다시 찾는다`() = runTest(dispatcher) {
        val second = CompletableDeferred<NearbyVets>()
        val nearby = Nearby(found(vet("가", 1.0)), second)
        val vm = vm(nearby)
        val screen = backgroundScope.launch { vm.uiState.collect {} }
        runCurrent()
        assertTrue(vm.loaded().nearby is NearbyState.Found)

        screen.cancel()
        advanceTimeBy(6.seconds)
        runCurrent()
        backgroundScope.launch { vm.uiState.collect {} }
        runCurrent()
        assertEquals(2, nearby.centers.size)
        assertEquals(NearbyState.Searching, vm.loaded().nearby)

        second.complete(NearbyVets.Found(listOf(vet("나", 2.0))))
        runCurrent()
        assertEquals(listOf("나"), (vm.loaded().nearby as NearbyState.Found).rows.map { it.hospital.name })
    }

    @Test
    fun `V11 제휴 목록을 받지 못하면 Failed이고 다시 받으면 이미 찾은 근처 결과와 함께 보인다`() = runTest(dispatcher) {
        val nearby = Nearby(found(vet("가", 1.0)))
        val boards = Boards(HospitalBoard.Unavailable("시간 초과"))
        val vm = vm(nearby, boards = boards)
        backgroundScope.launch { vm.uiState.collect {} }
        runCurrent()
        assertEquals(HospitalFinderUiState.Failed(), vm.uiState.value)

        boards.answer = board
        vm.onRefresh()
        runCurrent()
        val state = vm.loaded()
        assertEquals(setOf("제휴", "가"), state.ranking.allNames().toSet())
        assertTrue(state.nearby is NearbyState.Found)
        assertEquals(1, nearby.centers.size)
    }

    @Test
    fun `V12 새로 받은 제휴 목록에 같은 병원이 생기면 근처 줄에서 빠지고 제휴 쪽만 남는다`() = runTest(dispatcher) {
        val same = vet("겹치는 동물병원", 1.0)
        val boards = Boards(board)
        val vm = vm(Nearby(found(same)), boards = boards)
        backgroundScope.launch { vm.uiState.collect {} }
        runCurrent()
        assertTrue(vm.loaded().nearby is NearbyState.Found)

        val samePartner = HospitalCandidate(same.copy(id = "b"), HospitalStatus(15, Congestion.NORMAL, true, now), HospitalOrigin.SERVER)
        boards.answer = HospitalBoard.Loaded(listOf(partner, samePartner), HospitalOrigin.SERVER)
        vm.onRefresh()
        runCurrent()
        val state = vm.loaded()
        assertEquals(NearbyState.Empty, state.nearby)
        val rows = (state.ranking.ranked + state.ranking.unranked).filter { it.hospital.name == same.name }
        assertEquals(1, rows.size)
        assertEquals(HospitalOrigin.SERVER, rows.single().candidate.origin)

        // 제휴 목록에서 다시 빠지면 근처 쪽이 돌아온다 (검색은 다시 하지 않는다)
        boards.answer = board
        vm.onRefresh()
        runCurrent()
        assertEquals(listOf(same.name), (vm.loaded().nearby as NearbyState.Found).rows.map { it.hospital.name })
    }

    @Test
    fun `근처 병원은 15곳까지만 섞고 순위는 묶음당 10곳에 잘리지 않는다`() = runTest(dispatcher) {
        val vets = (1..20).map { vet("근처%02d".format(it), 0.2 * it) }
        val vm = vm(Nearby(found(*vets.toTypedArray())))
        backgroundScope.launch { vm.uiState.collect {} }
        runCurrent()
        val state = vm.loaded()
        assertEquals(15, (state.nearby as NearbyState.Found).rows.size)
        assertEquals(16, state.ranking.allNames().size)
    }
}
