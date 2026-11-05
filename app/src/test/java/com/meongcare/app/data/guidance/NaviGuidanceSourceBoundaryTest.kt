package com.meongcare.app.data.guidance

import app.cash.turbine.test
import com.meongcare.app.data.guidance.navi.NaviEvent
import com.meongcare.app.domain.guidance.Destination
import com.meongcare.app.domain.guidance.GuidanceSource
import com.meongcare.app.domain.guidance.GuidanceState
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
 * 전부 내비 길안내(Real 모드, 차량 기기)의 경계.
 * 기준: docs/plans/active/010-real-navi-guidance.md 설계 결정 2·3, `GuidanceSource` 계약,
 * `NaviClient` 계약(구독을 끊으면 경로 취소·해제), sdk-notes `[obs-navi-emu]`(초 단위, 취소 직전 0초).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NaviGuidanceSourceBoundaryTest {
    private val hospital = Destination("시연용 동물병원", 37.48544722, 127.03636666)
    private val other = Destination("다른 동물병원", 37.5, 127.0)

    private fun TestScope.source(navi: TripRecordingNaviClient) = NaviGuidanceSource(backgroundScope, navi)

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

    /** 안내를 시작하고 내비가 [seconds]초로 안내를 시작한 상태까지 간다 */
    private suspend fun TestScope.guiding(
        source: NaviGuidanceSource,
        navi: TripRecordingNaviClient,
        seconds: Int = 2100,
    ): TripRecordingNaviClient.Trip {
        source.start(hospital)
        runCurrent()
        val trip = navi.trips.last()
        trip.send(NaviEvent.RouteStarted(seconds))
        runCurrent()
        return trip
    }

    @Test
    fun `처음에는 안내 중이 아니고 내비에 연결하지 않는다`() = runTest {
        val navi = TripRecordingNaviClient()
        val source = source(navi)
        advance(1.minutes)
        assertEquals(GuidanceState.Idle, source.state.value)
        assertEquals(0, navi.totalSubscriptions)
    }

    @Test
    fun `안내를 시작하면 바로 경로를 찾는 중이 되고 내비 안내를 한 번만 구독한다`() = runTest {
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
    fun `내비가 안내를 시작한 뒤에도 도착이나 취소 전까지는 내비 안내 구독을 유지한다`() = runTest {
        val navi = TripRecordingNaviClient()
        val source = source(navi)
        val trip = guiding(source, navi)
        trip.send(NaviEvent.Remaining(2000))
        runCurrent()
        advance(10.minutes)
        assertEquals(GuidanceState.Guiding(hospital, 2000.seconds), source.state.value)
        assertTrue("안내 중에 구독을 끊으면 내비 경로가 취소된다", trip.isSubscribed)
        assertEquals(1, trip.subscriptions)
    }

    @Test
    fun `내비가 취소 직전에 보내는 0초는 0분으로 보이지 않고 이어 온 취소로 안내 없음이 된다`() = runTest {
        val navi = TripRecordingNaviClient()
        val source = source(navi)
        val seen = record(source)
        val trip = guiding(source, navi)
        // [obs-navi-emu]: cancelRoute → onDrivingInfo(duration=0) → onRouteCancelled
        trip.send(NaviEvent.Remaining(0))
        runCurrent()
        assertEquals(GuidanceState.Guiding(hospital, 35.minutes), source.state.value)
        trip.send(NaviEvent.Cancelled)
        runCurrent()
        assertEquals(
            listOf(
                GuidanceState.Idle,
                GuidanceState.Guiding(hospital, null),
                GuidanceState.Guiding(hospital, 35.minutes),
                GuidanceState.Idle,
            ),
            seen,
        )
        assertFalse(trip.isSubscribed)
    }

    @Test
    fun `하루보다 긴 남은 시간은 모름이라 이전 남은 시간을 그대로 둔다`() = runTest {
        val navi = TripRecordingNaviClient()
        val source = source(navi)
        val trip = guiding(source, navi)
        trip.send(NaviEvent.Remaining(1.days.inWholeSeconds.toInt() + 1))
        runCurrent()
        assertEquals(
            "계획 설계 결정 3: 0 이하, 하루보다 긴 값은 모름(null)이고 모름이면 이전 값을 지우지 않는다",
            GuidanceState.Guiding(hospital, 35.minutes),
            source.state.value,
        )
    }

    @Test
    fun `남은 시간은 무한대나 0 이하가 되지 않는다`() = runTest {
        val navi = TripRecordingNaviClient()
        val source = source(navi)
        val seen = record(source)
        source.start(hospital)
        runCurrent()
        val trip = navi.trips.last()
        listOf(
            NaviEvent.RouteStarted(Int.MIN_VALUE),
            NaviEvent.Remaining(-1),
            NaviEvent.Remaining(0),
            NaviEvent.Remaining(Int.MAX_VALUE),
            NaviEvent.Remaining(1),
        ).forEach { trip.send(it) }
        runCurrent()
        val times = seen.filterIsInstance<GuidanceState.Guiding>().mapNotNull { it.remainingTime }
        assertTrue("받은 남은 시간: $times", times.isNotEmpty())
        times.forEach { t ->
            assertTrue("무한대: $t", t.isFinite())
            assertTrue("0 이하: $t", t.isPositive())
        }
    }

    @Test
    fun `안내 중에 다른 목적지로 시작하면 이전 내비 안내 구독을 끊고 새 목적지를 한 번 구독한다`() = runTest {
        val navi = TripRecordingNaviClient()
        val source = source(navi)
        val seen = record(source)
        val first = guiding(source, navi)
        source.start(other)
        assertEquals(GuidanceState.Guiding(other, null), source.state.value)
        runCurrent()
        assertFalse(first.isSubscribed)
        assertEquals(2, navi.trips.size)
        val second = navi.trips[1]
        assertEquals(other, second.destination)
        assertEquals(1, second.subscriptions)
        assertTrue(second.isSubscribed)
        // 목적지를 바꾸는 사이에 안내 없음이 끼지 않는다 (계약: 이전 안내를 새 목적지로 바꾼다)
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
    fun `목적지를 바꾼 뒤 이전 안내에서 늦게 온 이벤트는 상태를 바꾸지 않는다`() = runTest {
        val navi = TripRecordingNaviClient()
        val source = source(navi)
        source.start(hospital)
        runCurrent()
        val first = navi.trips[0]
        source.start(other)
        runCurrent()
        listOf(
            NaviEvent.RouteStarted(2100),
            NaviEvent.Remaining(600),
            NaviEvent.Arrived,
            NaviEvent.Cancelled,
            NaviEvent.Failed("late"),
        ).forEach { first.send(it) }
        runCurrent()
        assertEquals(GuidanceState.Guiding(other, null), source.state.value)
        assertTrue(navi.trips[1].isSubscribed)
    }

    @Test
    fun `도착하면 내비 안내 구독을 끊고 그 뒤에 온 취소는 도착 화면을 지우지 않는다`() = runTest {
        val navi = TripRecordingNaviClient()
        val source = source(navi)
        val trip = guiding(source, navi)
        trip.send(NaviEvent.Arrived)
        runCurrent()
        assertEquals(GuidanceState.Arrived(hospital), source.state.value)
        assertFalse(trip.isSubscribed)
        trip.send(NaviEvent.Remaining(0))
        trip.send(NaviEvent.Cancelled)
        runCurrent()
        assertEquals(GuidanceState.Arrived(hospital), source.state.value)
    }

    @Test
    fun `도착 화면을 닫으면 안내 없음이 되고 내비에 다시 연결하지 않는다`() = runTest {
        val navi = TripRecordingNaviClient()
        val source = source(navi)
        val trip = guiding(source, navi)
        trip.send(NaviEvent.Arrived)
        runCurrent()
        source.dismissArrival()
        assertEquals(GuidanceState.Idle, source.state.value)
        advance(5.minutes)
        assertEquals(GuidanceState.Idle, source.state.value)
        assertEquals(1, navi.totalSubscriptions)
        assertEquals(0, navi.activeSubscriptions)
    }

    @Test
    fun `도착 화면이 떠 있을 때 다시 안내를 시작하면 새 내비 안내를 구독한다`() = runTest {
        val navi = TripRecordingNaviClient()
        val source = source(navi)
        val first = guiding(source, navi)
        first.send(NaviEvent.Arrived)
        runCurrent()
        source.start(other)
        assertEquals(GuidanceState.Guiding(other, null), source.state.value)
        runCurrent()
        assertEquals(2, navi.trips.size)
        assertEquals(other, navi.trips[1].destination)
        assertEquals(1, navi.trips[1].subscriptions)
        navi.trips[1].send(NaviEvent.RouteStarted(600))
        runCurrent()
        assertEquals(GuidanceState.Guiding(other, 10.minutes), source.state.value)
    }

    @Test
    fun `내비 안내가 시작되기 전에 연결에 실패하면 안내 없음이 되고 구독을 끊는다`() = runTest {
        val navi = TripRecordingNaviClient()
        val source = source(navi)
        source.start(hospital)
        runCurrent()
        val trip = navi.trips[0]
        trip.send(NaviEvent.Failed("ConnectionFailed"))
        runCurrent()
        assertEquals(GuidanceState.Idle, source.state.value)
        assertFalse(trip.isSubscribed)
    }

    @Test
    fun `내비 안내가 시작된 뒤에 온 내비 오류는 안내를 끝내지 않는다`() = runTest {
        val navi = TripRecordingNaviClient()
        val source = source(navi)
        val trip = guiding(source, navi)
        trip.send(NaviEvent.Failed("ROUTE_ERROR"))
        runCurrent()
        assertEquals(
            "계획 설계 결정 3: Error는 RouteStarted 전이면 Idle + close, 뒤면 로그만",
            GuidanceState.Guiding(hospital, 35.minutes),
            source.state.value,
        )
        assertTrue("내비는 계속 안내 중인데 구독을 끊으면 경로가 취소된다", trip.isSubscribed)
    }

    @Test
    fun `내비가 아무 응답도 하지 않으면 경로를 찾는 중에 계속 머물지 않고 안내 없음으로 돌아가며 구독을 끊는다`() = runTest {
        val navi = TripRecordingNaviClient()
        val source = source(navi)
        source.start(hospital)
        runCurrent()
        val trip = navi.trips[0]
        // 계획 설계 결정 2·3: 연결 대기 시간(기본 5초)을 넘기면 Idle + close.
        // 프로젝트가 내비 응답을 기다리는 가장 긴 값(경로만 내비 30초)보다 훨씬 넉넉하게 5분을 준다.
        advance(5.minutes)
        assertEquals(GuidanceState.Idle, source.state.value)
        assertFalse(trip.isSubscribed)
    }

    @Test
    fun `내비 응답을 기다리는 중에 취소하면 안내 없음이 되고 구독을 끊으며 늦게 온 안내 시작은 무시한다`() = runTest {
        val navi = TripRecordingNaviClient()
        val source = source(navi)
        source.start(hospital)
        runCurrent()
        val trip = navi.trips[0]
        source.cancel()
        assertEquals(GuidanceState.Idle, source.state.value)
        runCurrent()
        assertFalse(trip.isSubscribed)
        trip.send(NaviEvent.RouteStarted(2100))
        runCurrent()
        assertEquals(GuidanceState.Idle, source.state.value)
        assertEquals(1, navi.totalSubscriptions)
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

        val trip = guiding(source, navi)
        trip.send(NaviEvent.Arrived)
        runCurrent()
        source.cancel()
        runCurrent()
        assertEquals(GuidanceState.Arrived(hospital), source.state.value)
        assertEquals(1, navi.totalSubscriptions)
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
    }

    @Test
    fun `중간에 구독해도 지금 상태를 먼저 받고 같은 상태를 연달아 받지 않는다`() = runTest {
        val navi = TripRecordingNaviClient()
        val source = source(navi)
        val trip = guiding(source, navi)
        source.state.test {
            assertEquals(GuidanceState.Guiding(hospital, 35.minutes), awaitItem())
            trip.send(NaviEvent.Remaining(2100))
            runCurrent()
            expectNoEvents()
            trip.send(NaviEvent.Remaining(2040))
            runCurrent()
            assertEquals(GuidanceState.Guiding(hospital, 34.minutes), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
