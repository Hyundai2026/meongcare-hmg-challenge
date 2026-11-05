package com.meongcare.app.data.guidance

import app.cash.turbine.test
import com.meongcare.app.data.guidance.navi.NaviEvent
import com.meongcare.app.domain.guidance.Destination
import com.meongcare.app.domain.guidance.GuidanceSource
import com.meongcare.app.domain.guidance.GuidanceState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * 경로만 내비 길안내(시연 모드, 차량 기기)의 경계.
 * 기준: docs/plans/active/010-real-navi-guidance.md 설계 결정 4·8, `GuidanceSource` 계약, `NaviClient` 계약,
 * 사용자 결정(남은 시간은 내비 예상 시간에서 시작, 30초 안에 안 오면 기본값, 시연 도착 시 내비 경로 종료).
 *
 * 시연 진행은 AppContainer와 같은 방식으로 만든다: 기본 시간 설정(경로 계산 1초, 12분, 2초마다 1분)에
 * 처음 남은 시간만 내비 값으로 바꾼다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NaviRouteOnlyGuidanceSourceBoundaryTest {
    private val hospital = Destination("시연용 동물병원", 37.48544722, 127.03636666)
    private val other = Destination("다른 동물병원", 37.5, 127.0)

    /** 시연 진행을 만들 때 받은 처음 남은 시간 (null = 기본값) */
    private val starts = mutableListOf<Duration?>()

    private fun TestScope.source(navi: TripRecordingNaviClient, gate: Flow<Boolean>? = null) =
        NaviRouteOnlyGuidanceSource(
            scope = backgroundScope,
            navi = navi,
            fakeOf = { initial ->
                starts += initial
                FakeGuidanceSource(
                    scope = backgroundScope,
                    timing = FakeGuidanceTiming().let { if (initial != null) it.copy(initialRemaining = initial) else it },
                    progressGate = gate,
                )
            },
        )

    private fun TestScope.advance(d: Duration) {
        advanceTimeBy(d)
        runCurrent()
    }

    /** 구독자가 받은 상태를 순서대로 모은다 */
    private fun TestScope.record(source: GuidanceSource): List<GuidanceState> {
        val seen = mutableListOf<GuidanceState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { source.state.collect { seen += it } }
        return seen
    }

    /** 안내를 시작하고, 내비가 [seconds]초로 안내를 시작하고, 시연 진행이 첫 남은 시간을 보일 때까지 간다 */
    private suspend fun TestScope.guiding(
        source: NaviRouteOnlyGuidanceSource,
        navi: TripRecordingNaviClient,
        destination: Destination = hospital,
        seconds: Int = 2100,
    ): TripRecordingNaviClient.Trip {
        source.start(destination)
        runCurrent()
        val trip = navi.trips.last()
        trip.send(NaviEvent.RouteStarted(seconds))
        runCurrent()
        advance(1.seconds) // 시연 진행의 경로 계산
        return trip
    }

    @Test
    fun `안내를 시작하면 바로 경로를 찾는 중이 되고 내비 안내를 한 번 구독한다`() = runTest {
        val navi = TripRecordingNaviClient()
        val source = source(navi)
        source.start(hospital)
        assertEquals(GuidanceState.Guiding(hospital, null), source.state.value)
        runCurrent()
        assertEquals(1, navi.trips.size)
        assertEquals(hospital, navi.trips[0].destination)
        assertEquals(1, navi.trips[0].subscriptions)
        assertTrue(navi.trips[0].isSubscribed)
    }

    @Test
    fun `내비 예상 시간을 받은 뒤에도 시연이 끝날 때까지 내비 경로 구독을 유지한다`() = runTest {
        val navi = TripRecordingNaviClient()
        val source = source(navi)
        val trip = guiding(source, navi)
        assertEquals(GuidanceState.Guiding(hospital, 35.minutes), source.state.value)
        advance(20.seconds)
        assertTrue(source.state.value is GuidanceState.Guiding)
        assertTrue("안내 중에 구독을 끊으면 내비 경로가 취소된다", trip.isSubscribed)
        assertEquals(1, trip.subscriptions)
    }

    @Test
    fun `30초가 되기 직전에 온 내비 예상 시간에서 시연 남은 시간이 시작한다`() = runTest {
        val navi = TripRecordingNaviClient()
        val source = source(navi)
        source.start(hospital)
        runCurrent()
        advance(29.seconds)
        assertEquals(GuidanceState.Guiding(hospital, null), source.state.value)
        assertTrue(starts.isEmpty())
        navi.trips[0].send(NaviEvent.RouteStarted(2100))
        runCurrent()
        advance(1.seconds)
        assertEquals(listOf<Duration?>(35.minutes), starts)
        assertEquals(GuidanceState.Guiding(hospital, 35.minutes), source.state.value)
    }

    @Test
    fun `30초가 지난 뒤 온 내비 예상 시간은 이미 시작한 시연 남은 시간을 바꾸지 않는다`() = runTest {
        val navi = TripRecordingNaviClient()
        val source = source(navi)
        source.start(hospital)
        runCurrent()
        advance(30.seconds)
        advance(1.seconds)
        assertEquals(GuidanceState.Guiding(hospital, 12.minutes), source.state.value)
        navi.trips[0].send(NaviEvent.RouteStarted(2100))
        runCurrent()
        advance(1.seconds)
        assertEquals(listOf<Duration?>(null), starts)
        assertEquals(GuidanceState.Guiding(hospital, 12.minutes), source.state.value)
        assertTrue(navi.trips[0].isSubscribed)
    }

    @Test
    fun `내비 예상 시간이 0 이하이면 시연용 기본 시간으로 시작한다`() = runTest {
        val navi = TripRecordingNaviClient()
        val source = source(navi)
        guiding(source, navi, seconds = 0)
        assertEquals(GuidanceState.Guiding(hospital, 12.minutes), source.state.value)
        source.cancel()
        guiding(source, navi, seconds = -60)
        assertEquals(GuidanceState.Guiding(hospital, 12.minutes), source.state.value)
        assertEquals(listOf<Duration?>(null, null), starts)
    }

    @Test
    fun `하루보다 긴 내비 예상 시간은 쓰지 않고 시연용 기본 시간으로 시작한다`() = runTest {
        val navi = TripRecordingNaviClient()
        val source = source(navi)
        guiding(source, navi, seconds = 1.days.inWholeSeconds.toInt() + 1)
        assertEquals(
            "계획 설계 결정 3: 하루보다 긴 값은 모름(null)이다. 모르면 시연용 기본 시간이다",
            listOf<Duration?>(null),
            starts,
        )
        assertEquals(GuidanceState.Guiding(hospital, 12.minutes), source.state.value)
    }

    @Test
    fun `내비 연결에 실패하면 30초를 다 기다리지 않고 시연용 기본 시간으로 시작한다`() = runTest {
        val navi = TripRecordingNaviClient()
        val source = source(navi)
        source.start(hospital)
        runCurrent()
        navi.trips[0].send(NaviEvent.Failed("ConnectionFailed"))
        runCurrent()
        // 기본 시간 설정의 경로 계산(1초) + 여유 1초
        advance(2.seconds)
        assertEquals(
            "계획 설계 결정 4·8: 실패는 로그만 남기고 시연 진행은 계속한다. 실패한 뒤에는 기다릴 예상 시간이 없다",
            GuidanceState.Guiding(hospital, 12.minutes),
            source.state.value,
        )
    }

    @Test
    fun `내비 화면에서 경로를 취소하면 시연 길안내도 안내 없음이 되고 내비 구독을 끊는다`() = runTest {
        val navi = TripRecordingNaviClient()
        val source = source(navi)
        val trip = guiding(source, navi)
        // [obs-navi-emu]: 취소하면 onDrivingInfo(duration=0) → onRouteCancelled 순서로 온다
        trip.send(NaviEvent.Remaining(0))
        trip.send(NaviEvent.Cancelled)
        runCurrent()
        assertEquals(
            "계획 설계 결정 4: 내비 RouteCancelled이고 진행이 Guiding이면 progress.cancel(), close",
            GuidanceState.Idle,
            source.state.value,
        )
        assertFalse(trip.isSubscribed)
        advance(5.minutes)
        assertEquals(GuidanceState.Idle, source.state.value)
    }

    @Test
    fun `내비가 안내를 시작하기 전에 내비에서 경로를 취소하면 시연을 시작하지 않는다`() = runTest {
        val navi = TripRecordingNaviClient()
        val source = source(navi)
        source.start(hospital)
        runCurrent()
        navi.trips[0].send(NaviEvent.Cancelled)
        runCurrent()
        assertEquals(GuidanceState.Idle, source.state.value)
        advance(1.minutes)
        assertEquals(GuidanceState.Idle, source.state.value)
        assertTrue("시연 진행을 만들지 않는다: $starts", starts.isEmpty())
    }

    @Test
    fun `내비의 남은 시간과 도착 이벤트는 시연 진행을 바꾸지 않는다`() = runTest {
        val navi = TripRecordingNaviClient()
        val source = source(navi)
        val trip = guiding(source, navi)
        trip.send(NaviEvent.Remaining(60))
        trip.send(NaviEvent.Arrived)
        runCurrent()
        assertEquals(GuidanceState.Guiding(hospital, 35.minutes), source.state.value)
        advance(2.seconds)
        assertEquals(GuidanceState.Guiding(hospital, 34.minutes), source.state.value)
    }

    @Test
    fun `내비 예상 시간을 기다리는 중에 취소하면 30초가 지나도 시연이 시작되지 않고 내비 구독도 끊긴다`() = runTest {
        val navi = TripRecordingNaviClient()
        val source = source(navi)
        val seen = record(source)
        source.start(hospital)
        runCurrent()
        val trip = navi.trips[0]
        source.cancel()
        assertEquals(GuidanceState.Idle, source.state.value)
        runCurrent()
        assertFalse(trip.isSubscribed)
        trip.send(NaviEvent.RouteStarted(2100))
        advance(1.minutes)
        assertEquals(GuidanceState.Idle, source.state.value)
        assertTrue("시연 진행을 만들지 않는다: $starts", starts.isEmpty())
        assertEquals(
            listOf(GuidanceState.Idle, GuidanceState.Guiding(hospital, null), GuidanceState.Idle),
            seen,
        )
    }

    @Test
    fun `내비 예상 시간을 기다리는 중에 다른 목적지로 시작하면 이전 목적지로 돌아가지 않는다`() = runTest {
        val navi = TripRecordingNaviClient()
        val source = source(navi)
        val seen = record(source)
        source.start(hospital)
        runCurrent()
        val first = navi.trips[0]
        source.start(other)
        runCurrent()
        assertFalse(first.isSubscribed)
        val second = navi.trips[1]
        assertEquals(other, second.destination)
        assertEquals(1, second.subscriptions)
        first.send(NaviEvent.RouteStarted(600))
        runCurrent()
        advance(31.seconds)
        assertEquals("이전 안내의 예상 시간(10분)을 쓰지 않는다", listOf<Duration?>(null), starts)
        assertEquals(GuidanceState.Guiding(other, 12.minutes), source.state.value)
        val afterSwitch = seen.dropWhile { it != GuidanceState.Guiding(other, null) }
        assertTrue("바꾼 뒤 받은 상태: $afterSwitch", afterSwitch.isNotEmpty())
        afterSwitch.forEach { state ->
            assertTrue("바꾼 뒤 이전 목적지나 안내 없음이 보였다: $state", state is GuidanceState.Guiding && state.destination == other)
        }
    }

    @Test
    fun `안내 중에 다른 목적지로 바꾸면 이전 내비 구독을 끊고 중간에 안내 없음이 끼지 않는다`() = runTest {
        val navi = TripRecordingNaviClient()
        val source = source(navi)
        val seen = record(source)
        val first = guiding(source, navi)
        source.start(other)
        assertEquals(GuidanceState.Guiding(other, null), source.state.value)
        runCurrent()
        assertFalse(first.isSubscribed)
        assertTrue(navi.trips[1].isSubscribed)
        advance(5.seconds)
        assertEquals(
            listOf(
                GuidanceState.Idle,
                GuidanceState.Guiding(hospital, null),
                GuidanceState.Guiding(hospital, 35.minutes),
                GuidanceState.Guiding(other, null),
            ),
            seen,
        )
    }

    @Test
    fun `시연이 도착한 뒤 늦게 온 내비 취소는 도착 화면을 지우지 않는다`() = runTest {
        val navi = TripRecordingNaviClient()
        val source = source(navi)
        val trip = guiding(source, navi, seconds = 120)
        advance(10.seconds)
        assertEquals(GuidanceState.Arrived(hospital), source.state.value)
        assertFalse(trip.isSubscribed)
        trip.send(NaviEvent.Remaining(0))
        trip.send(NaviEvent.Cancelled)
        runCurrent()
        assertEquals(GuidanceState.Arrived(hospital), source.state.value)
    }

    @Test
    fun `도착 화면을 닫은 뒤에는 안내 없음이 그대로이고 내비에 다시 연결하지 않는다`() = runTest {
        val navi = TripRecordingNaviClient()
        val source = source(navi)
        guiding(source, navi, seconds = 120)
        advance(10.seconds)
        source.dismissArrival()
        assertEquals(GuidanceState.Idle, source.state.value)
        advance(5.minutes)
        assertEquals(GuidanceState.Idle, source.state.value)
        assertEquals(1, navi.totalSubscriptions)
        assertEquals(0, navi.activeSubscriptions)
    }

    @Test
    fun `도착 화면이 떠 있을 때 다시 안내를 시작하면 새 내비 안내의 예상 시간으로 처음부터 시작한다`() = runTest {
        val navi = TripRecordingNaviClient()
        val source = source(navi)
        guiding(source, navi, seconds = 120)
        advance(10.seconds)
        assertEquals(GuidanceState.Arrived(hospital), source.state.value)
        guiding(source, navi, destination = other, seconds = 600)
        assertEquals(GuidanceState.Guiding(other, 10.minutes), source.state.value)
        assertEquals(2, navi.trips.size)
        assertTrue(navi.trips[1].isSubscribed)
        assertEquals(listOf<Duration?>(2.minutes, 10.minutes), starts)
    }

    @Test
    fun `안내 중이 아닐 때 취소하면 아무것도 하지 않는다`() = runTest {
        val navi = TripRecordingNaviClient()
        val source = source(navi)
        val seen = record(source)
        source.cancel()
        runCurrent()
        assertEquals(listOf<GuidanceState>(GuidanceState.Idle), seen)
        assertEquals(0, navi.totalSubscriptions)

        guiding(source, navi, seconds = 120)
        advance(10.seconds)
        source.cancel()
        runCurrent()
        assertEquals(GuidanceState.Arrived(hospital), source.state.value)
    }

    @Test
    fun `안내 중에 도착 화면 닫기를 불러도 안내와 내비 구독은 그대로다`() = runTest {
        val navi = TripRecordingNaviClient()
        val source = source(navi)
        val trip = guiding(source, navi)
        source.dismissArrival()
        runCurrent()
        assertEquals(GuidanceState.Guiding(hospital, 35.minutes), source.state.value)
        assertTrue(trip.isSubscribed)
        advance(2.seconds)
        assertEquals(GuidanceState.Guiding(hospital, 34.minutes), source.state.value)
    }

    @Test
    fun `안내를 취소하면 시연 진행이 듣던 주행 상태 구독도 끊긴다`() = runTest {
        val navi = TripRecordingNaviClient()
        val driving = MutableStateFlow(true)
        var gateSubscribers = 0
        val gate = flow {
            gateSubscribers++
            try {
                emitAll(driving)
            } finally {
                gateSubscribers--
            }
        }
        val source = source(navi, gate)
        val trip = guiding(source, navi)
        assertEquals(1, gateSubscribers)
        source.cancel()
        runCurrent()
        assertEquals(GuidanceState.Idle, source.state.value)
        assertEquals("Real 주행 상태는 구독마다 차량 연결을 맺는다", 0, gateSubscribers)
        assertFalse(trip.isSubscribed)
    }

    @Test
    fun `구독하면 지금 상태를 먼저 받고 시연 진행이 시작될 때 경로를 찾는 중을 다시 받지 않는다`() = runTest {
        val navi = TripRecordingNaviClient()
        val source = source(navi)
        source.start(hospital)
        runCurrent()
        source.state.test {
            assertEquals(GuidanceState.Guiding(hospital, null), awaitItem())
            navi.trips[0].send(NaviEvent.RouteStarted(2100))
            runCurrent()
            expectNoEvents()
            advance(1.seconds)
            assertEquals(GuidanceState.Guiding(hospital, 35.minutes), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
