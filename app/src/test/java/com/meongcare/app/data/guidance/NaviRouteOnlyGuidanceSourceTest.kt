package com.meongcare.app.data.guidance

import com.meongcare.app.data.guidance.navi.NaviEvent
import com.meongcare.app.domain.guidance.Destination
import com.meongcare.app.domain.guidance.GuidanceState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class NaviRouteOnlyGuidanceSourceTest {
    private val hospital = Destination("시연용 동물병원", 37.48544722, 127.03636666)
    private val starts = mutableListOf<Duration?>()

    /** 시연용: 처음 남은 시간(없으면 12분), 경로 계산 0초, 2초마다 1분 */
    private fun TestScope.source(navi: FakeNaviClient) = NaviRouteOnlyGuidanceSource(
        scope = backgroundScope,
        navi = navi,
        fakeOf = { initial ->
            starts += initial
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

    @Test
    fun `경로를 내비에 요청하고 내비 예상 시간에서 시연용 남은 시간이 시작한다`() = runTest {
        val navi = FakeNaviClient()
        val source = source(navi)
        source.start(hospital)
        runCurrent()
        assertEquals(listOf(hospital), navi.requested)
        assertEquals(GuidanceState.Guiding(hospital, null), source.state.value)
        navi.send(NaviEvent.RouteStarted(2100))
        runCurrent()
        assertEquals(listOf<Duration?>(35.minutes), starts)
        assertEquals(GuidanceState.Guiding(hospital, 35.minutes), source.state.value)
        advance(2.seconds)
        assertEquals(GuidanceState.Guiding(hospital, 34.minutes), source.state.value)
    }

    @Test
    fun `내비가 시간 안에 안내를 시작하지 않으면 시연용 기본 시간으로 시작한다`() = runTest {
        val navi = FakeNaviClient()
        val source = source(navi)
        source.start(hospital)
        advance(30.seconds)
        assertEquals(listOf<Duration?>(null), starts)
        assertEquals(GuidanceState.Guiding(hospital, 12.minutes), source.state.value)
    }

    @Test
    fun `시연 길안내가 도착하면 도착이고 내비 경로도 끝낸다`() = runTest {
        val navi = FakeNaviClient()
        val source = source(navi)
        source.start(hospital)
        runCurrent()
        navi.send(NaviEvent.RouteStarted(120))
        runCurrent()
        advance(6.seconds)
        assertEquals(GuidanceState.Arrived(hospital), source.state.value)
        assertEquals(1, navi.closedTrips)
        source.dismissArrival()
        assertEquals(GuidanceState.Idle, source.state.value)
    }

    @Test
    fun `앱에서 취소하면 안내 없음이고 내비 경로도 끝낸다`() = runTest {
        val navi = FakeNaviClient()
        val source = source(navi)
        source.start(hospital)
        runCurrent()
        navi.send(NaviEvent.RouteStarted(2100))
        runCurrent()
        source.cancel()
        runCurrent()
        assertEquals(GuidanceState.Idle, source.state.value)
        assertEquals(1, navi.closedTrips)
        advance(10.seconds)
        assertEquals(GuidanceState.Idle, source.state.value)
    }
}
