package com.meongcare.app.domain.guidance

import app.cash.turbine.test
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ManualArrivalGuidanceSourceTest {
    private class Inner : GuidanceSource {
        val flow = MutableStateFlow<GuidanceState>(GuidanceState.Idle)
        var cancels = 0
        var dismisses = 0
        override val state: StateFlow<GuidanceState> = flow
        override suspend fun start(destination: Destination) { flow.value = GuidanceState.Guiding(destination, 10.minutes) }
        override suspend fun cancel() { cancels++; flow.value = GuidanceState.Idle }
        override fun dismissArrival() { dismisses++ }
    }

    private val dest = Destination("온유동물병원", 37.5, 127.0)

    @Test
    fun `병원 도착을 누르면 취소를 거치지 않고 바로 도착이고 안쪽 안내는 멈춘다`() = runTest {
        val inner = Inner()
        val src = ManualArrivalGuidanceSource(inner)
        src.state.test {
            assertEquals(GuidanceState.Idle, awaitItem())
            src.start(dest)
            val guiding = awaitItem()
            src.arriveNow(guiding)
            assertEquals(GuidanceState.Arrived(dest), awaitItem())
            expectNoEvents()
            assertEquals(1, inner.cancels)
            src.dismissArrival()
            assertEquals(GuidanceState.Idle, awaitItem())
            assertEquals(0, inner.dismisses)
        }
    }

    @Test
    fun `안내 중이 아니면 병원 도착은 아무것도 하지 않고 닫기는 안쪽에 넘긴다`() = runTest {
        val inner = Inner()
        val src = ManualArrivalGuidanceSource(inner)
        src.arriveNow(GuidanceState.Idle)
        assertEquals(0, inner.cancels)
        src.dismissArrival()
        assertEquals(1, inner.dismisses)
    }
}
