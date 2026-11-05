package com.meongcare.app.data.guidance

import com.meongcare.app.data.guidance.navi.NaviEvent
import com.meongcare.app.domain.guidance.Destination
import com.meongcare.app.domain.guidance.GuidanceState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalCoroutinesApi::class)
class NaviGuidanceSourceTest {
    private val hospital = Destination("시연용 동물병원", 37.48544722, 127.03636666)

    @Test
    fun `안내를 시작하면 경로를 찾는 중이고 내비가 안내를 시작하면 그 남은 시간이다`() = runTest {
        val navi = FakeNaviClient()
        val source = NaviGuidanceSource(backgroundScope, navi)
        source.start(hospital)
        runCurrent()
        assertEquals(listOf(hospital), navi.requested)
        assertEquals(GuidanceState.Guiding(hospital, null), source.state.value)
        navi.send(NaviEvent.RouteStarted(2100))
        runCurrent()
        assertEquals(GuidanceState.Guiding(hospital, 35.minutes), source.state.value)
        navi.send(NaviEvent.Remaining(600))
        runCurrent()
        assertEquals(GuidanceState.Guiding(hospital, 10.minutes), source.state.value)
    }

    @Test
    fun `남은 시간이 0 이하이면 쓰지 않는다`() = runTest {
        val navi = FakeNaviClient()
        val source = NaviGuidanceSource(backgroundScope, navi)
        source.start(hospital)
        runCurrent()
        navi.send(NaviEvent.RouteStarted(2100))
        navi.send(NaviEvent.Remaining(0))
        runCurrent()
        assertEquals(GuidanceState.Guiding(hospital, 35.minutes), source.state.value)
    }

    @Test
    fun `도착 이벤트가 오면 도착이고 닫으면 안내 없음이다`() = runTest {
        val navi = FakeNaviClient()
        val source = NaviGuidanceSource(backgroundScope, navi)
        source.start(hospital)
        runCurrent()
        navi.send(NaviEvent.Arrived)
        runCurrent()
        assertEquals(GuidanceState.Arrived(hospital), source.state.value)
        assertEquals(1, navi.closedTrips)
        source.dismissArrival()
        assertEquals(GuidanceState.Idle, source.state.value)
    }

    @Test
    fun `내비에서 취소되거나 실패하면 안내 없음이다`() = runTest {
        val navi = FakeNaviClient()
        val source = NaviGuidanceSource(backgroundScope, navi)
        source.start(hospital)
        runCurrent()
        navi.send(NaviEvent.Cancelled)
        runCurrent()
        assertEquals(GuidanceState.Idle, source.state.value)
        source.start(hospital)
        runCurrent()
        navi.send(NaviEvent.Failed("connect"))
        runCurrent()
        assertEquals(GuidanceState.Idle, source.state.value)
    }

    @Test
    fun `앱에서 취소하면 안내 없음이고 내비 안내 구독을 끊는다`() = runTest {
        val navi = FakeNaviClient()
        val source = NaviGuidanceSource(backgroundScope, navi)
        source.start(hospital)
        runCurrent()
        source.cancel()
        runCurrent()
        assertEquals(GuidanceState.Idle, source.state.value)
        assertEquals(1, navi.closedTrips)
    }
}
