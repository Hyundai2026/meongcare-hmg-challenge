package com.meongcare.app.data.guidance

import com.meongcare.app.data.guidance.navi.NaviEvent
import com.meongcare.app.data.guidance.navi.RouteOnClose
import com.meongcare.app.domain.guidance.Destination
import com.meongcare.app.domain.guidance.GuidanceState
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 013 U2 경계 (계획 013 설계 결정 2 표):
 * 시연 도착·[닫기]는 경로를 남기고(KEEP), 앱 [안내 취소]·경로 대기 중 취소·다른 목적지로 다시 시작은 취소한다(CANCEL).
 * 남기는 것은 안내(Trip)마다 정해진다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NaviRouteOnlyKeepRouteBoundaryTest {
    private val hospital = Destination("시연용 동물병원", 37.48544722, 127.03636666)
    private val other = Destination("시연용 바른 동물병원", 37.367, 127.1083)

    /** 시연용: 경로 계산 0초, 2초마다 1분. 내비가 시작을 알리지 않으면 5초 뒤 기본 2분으로 시작한다 */
    private fun TestScope.source(navi: CloseRecordingNaviClient) = NaviRouteOnlyGuidanceSource(
        scope = backgroundScope,
        navi = navi,
        fakeOf = { initial ->
            FakeGuidanceSource(
                backgroundScope,
                FakeGuidanceTiming(routeCalculationDelay = Duration.ZERO, initialRemaining = initial ?: 2.minutes),
            )
        },
        routeStartTimeout = 5.seconds,
    )

    private fun TestScope.advance(d: Duration) {
        advanceTimeBy(d)
        runCurrent()
    }

    @Test
    fun `내비가 경로 시작을 알리지 않아 기본 시간으로 진행해도 도착하면 경로를 남긴다`() = runTest {
        val navi = CloseRecordingNaviClient()
        val source = source(navi)
        source.start(hospital)
        runCurrent()
        advance(5.seconds) // 시작 알림 기다리기 끝 → 기본 2분으로 시연 진행
        advance(6.seconds)
        assertEquals(GuidanceState.Arrived(hospital), source.state.value)
        assertFalse(navi.trips.single().isSubscribed)
        assertEquals(RouteOnClose.KEEP, navi.trips.single().closedWith)
    }

    @Test
    fun `내비 오류 뒤 기본 시간으로 진행해 도착해도 경로를 남긴다`() = runTest {
        val navi = CloseRecordingNaviClient()
        val source = source(navi)
        source.start(hospital)
        runCurrent()
        navi.trips.single().send(NaviEvent.Failed("connect"))
        runCurrent()
        advance(6.seconds)
        assertEquals(GuidanceState.Arrived(hospital), source.state.value)
        assertEquals(RouteOnClose.KEEP, navi.trips.single().closedWith)
    }

    @Test
    fun `경로를 남긴 다음 안내를 앱에서 취소하면 그 경로는 취소한다`() = runTest {
        val navi = CloseRecordingNaviClient()
        val source = source(navi)
        source.start(hospital)
        runCurrent()
        navi.trips[0].send(NaviEvent.RouteStarted(120))
        runCurrent()
        advance(6.seconds)
        source.dismissArrival()
        runCurrent()

        source.start(other)
        runCurrent()
        navi.trips[1].send(NaviEvent.RouteStarted(600))
        advance(2.seconds)
        source.cancel()
        runCurrent()

        assertEquals(RouteOnClose.KEEP, navi.trips[0].closedWith)
        assertEquals(RouteOnClose.CANCEL, navi.trips[1].closedWith)
        assertEquals(GuidanceState.Idle, source.state.value)
    }

    @Test
    fun `안내 중에 도착 화면 닫기를 불러도 내비 연결과 경로는 그대로다`() = runTest {
        val navi = CloseRecordingNaviClient()
        val source = source(navi)
        source.start(hospital)
        runCurrent()
        navi.trips.single().send(NaviEvent.RouteStarted(600))
        advance(2.seconds)
        source.dismissArrival()
        runCurrent()
        assertTrue(source.state.value is GuidanceState.Guiding)
        assertTrue(navi.trips.single().isSubscribed)
        assertNull(navi.trips.single().closedWith)
    }

    @Test
    fun `경로를 기다리는 중에 다른 목적지로 다시 시작하면 이전 경로는 취소한다`() = runTest {
        val navi = CloseRecordingNaviClient()
        val source = source(navi)
        source.start(hospital)
        runCurrent()
        source.start(other)
        runCurrent()
        assertEquals(2, navi.trips.size)
        assertEquals(RouteOnClose.CANCEL, navi.trips[0].closedWith)
        assertTrue(navi.trips[1].isSubscribed)
        assertEquals(GuidanceState.Guiding(other, null), source.state.value)
    }

    @Test
    fun `도착 뒤 닫기까지 해도 경로를 남긴 안내는 한 번만 닫힌다`() = runTest {
        val navi = CloseRecordingNaviClient()
        val source = source(navi)
        source.start(hospital)
        runCurrent()
        navi.trips.single().send(NaviEvent.RouteStarted(120))
        runCurrent()
        advance(6.seconds)
        source.dismissArrival()
        runCurrent()
        source.dismissArrival() // 두 번 눌러도
        runCurrent()
        assertEquals(1, navi.trips.size)
        assertEquals(RouteOnClose.KEEP, navi.trips.single().closedWith)
        assertEquals(GuidanceState.Idle, source.state.value)
    }
}
