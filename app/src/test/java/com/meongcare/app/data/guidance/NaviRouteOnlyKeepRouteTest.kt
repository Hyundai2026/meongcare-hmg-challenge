package com.meongcare.app.data.guidance

import com.meongcare.app.data.guidance.navi.NaviEvent
import com.meongcare.app.data.guidance.navi.RouteOnClose
import com.meongcare.app.domain.guidance.Destination
import com.meongcare.app.domain.guidance.GuidanceState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/** 013 U2: 시연 도착·[닫기]는 내비 경로를 남기고, 앱 [안내 취소]·목적지 변경은 경로를 취소한다. */
@OptIn(ExperimentalCoroutinesApi::class)
class NaviRouteOnlyKeepRouteTest {
    private val hospital = Destination("시연용 동물병원", 37.48544722, 127.03636666)
    private val other = Destination("시연용 바른 동물병원", 37.367, 127.1083)

    /** 시연용: 경로 계산 0초, 2초마다 1분 */
    private fun TestScope.source(navi: CloseRecordingNaviClient) = NaviRouteOnlyGuidanceSource(
        scope = backgroundScope,
        navi = navi,
        fakeOf = { initial ->
            FakeGuidanceSource(
                backgroundScope,
                FakeGuidanceTiming(routeCalculationDelay = Duration.ZERO, initialRemaining = initial ?: 12.minutes),
            )
        },
    )

    private fun TestScope.advance(d: Duration) {
        advanceTimeBy(d)
        runCurrent()
    }

    /** 내비가 2분 경로로 안내를 시작하고, 시연 진행이 4초 뒤 도착한다 */
    private fun TestScope.arrive(source: NaviRouteOnlyGuidanceSource, navi: CloseRecordingNaviClient) {
        navi.trips.last().send(NaviEvent.RouteStarted(120))
        runCurrent()
        advance(6.seconds)
        assertEquals(GuidanceState.Arrived(hospital), source.state.value)
    }

    @Test
    fun `시연 도착으로 구독을 끊을 때는 내비 경로를 남긴다`() = runTest {
        val navi = CloseRecordingNaviClient()
        val source = source(navi)
        source.start(hospital)
        runCurrent()
        arrive(source, navi)
        assertFalse(navi.trips.single().isSubscribed)
        assertEquals(RouteOnClose.KEEP, navi.trips.single().closedWith)
    }

    @Test
    fun `도착 화면을 닫아도 내비 경로를 취소하지 않는다`() = runTest {
        val navi = CloseRecordingNaviClient()
        val source = source(navi)
        source.start(hospital)
        runCurrent()
        arrive(source, navi)
        source.dismissArrival()
        runCurrent()
        assertEquals(GuidanceState.Idle, source.state.value)
        assertEquals(RouteOnClose.KEEP, navi.trips.single().closedWith)
    }

    @Test
    fun `안내 중에 앱에서 취소하면 내비 경로를 취소한다`() = runTest {
        val navi = CloseRecordingNaviClient()
        val source = source(navi)
        source.start(hospital)
        runCurrent()
        navi.trips.single().send(NaviEvent.RouteStarted(600))
        advance(2.seconds)
        source.cancel()
        runCurrent()
        assertEquals(GuidanceState.Idle, source.state.value)
        assertEquals(RouteOnClose.CANCEL, navi.trips.single().closedWith)
    }

    @Test
    fun `경로를 기다리는 중에 취소하면 내비 경로를 취소한다`() = runTest {
        val navi = CloseRecordingNaviClient()
        val source = source(navi)
        source.start(hospital)
        runCurrent()
        source.cancel()
        runCurrent()
        assertEquals(RouteOnClose.CANCEL, navi.trips.single().closedWith)
    }

    @Test
    fun `다른 목적지로 다시 시작하면 이전 경로는 취소한다`() = runTest {
        val navi = CloseRecordingNaviClient()
        val source = source(navi)
        source.start(hospital)
        runCurrent()
        navi.trips.single().send(NaviEvent.RouteStarted(600))
        runCurrent()
        source.start(other)
        runCurrent()
        assertEquals(2, navi.trips.size)
        assertEquals(RouteOnClose.CANCEL, navi.trips[0].closedWith)
        assertNull(navi.trips[1].closedWith)
    }

    @Test
    fun `경로를 남긴 뒤 새로 출발하면 내비에 새로 구독한다`() = runTest {
        val navi = CloseRecordingNaviClient()
        val source = source(navi)
        source.start(hospital)
        runCurrent()
        arrive(source, navi)
        source.dismissArrival()
        runCurrent()
        source.start(other)
        runCurrent()
        assertEquals(listOf(hospital, other), navi.trips.map { it.destination })
        assertEquals(true, navi.trips[1].isSubscribed)
        assertEquals(GuidanceState.Guiding(other, null), source.state.value)
        // 내비에 남은 이전 경로의 취소는 새 경로가 시작되기 전이라 어댑터(PleosNaviClient)가 거른다.
        // 여기서는 새 경로가 시작된 뒤 시연 진행이 이어지는지만 본다.
        navi.trips[1].send(NaviEvent.RouteStarted(300))
        runCurrent()
        assertEquals(GuidanceState.Guiding(other, 5.minutes), source.state.value)
    }
}
