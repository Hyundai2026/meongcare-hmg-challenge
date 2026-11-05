package com.meongcare.app.data.guidance

import com.meongcare.app.domain.guidance.Destination
import com.meongcare.app.domain.guidance.GuidanceState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/** Fake 길안내의 진행 조건 (계획 007 "설계 결정 3"). 시연 모드에서는 "주행 중"이 조건이다. */
@OptIn(ExperimentalCoroutinesApi::class)
class FakeGuidanceSourceProgressGateTest {

    private val hospital = Destination(name = "테스트 병원", latitude = 37.0, longitude = 127.0)

    // 조건이 계속 참이면: 0초 경로 찾는 중 → 1초 3분 → 11초 2분 → 21초 1분 → 31초 도착
    private val timing = FakeGuidanceTiming(
        routeCalculationDelay = 1.seconds,
        initialRemaining = 3.minutes,
        remainingStep = 1.minutes,
        tickInterval = 10.seconds,
    )

    private fun TestScope.newSource(gate: MutableStateFlow<Boolean>) =
        FakeGuidanceSource(backgroundScope, timing, progressGate = gate)

    private fun TestScope.advance(time: Duration) {
        advanceTimeBy(time)
        runCurrent()
    }

    @Test
    fun `진행 조건이 거짓이어도 경로 계산은 끝나 처음 남은 시간을 보여준다`() = runTest {
        val gate = MutableStateFlow(false)
        val source = newSource(gate)

        source.start(hospital)
        advance(1.seconds)

        assertEquals(GuidanceState.Guiding(hospital, 3.minutes), source.state.value)
    }

    @Test
    fun `진행 조건이 거짓이면 처음 남은 시간에서 더 줄지 않는다`() = runTest {
        val gate = MutableStateFlow(false)
        val source = newSource(gate)

        source.start(hospital)
        advance(5.minutes)

        assertEquals(GuidanceState.Guiding(hospital, 3.minutes), source.state.value)
    }

    @Test
    fun `진행 조건이 참이 되면 그때부터 한 간격마다 줄어든다`() = runTest {
        val gate = MutableStateFlow(false)
        val source = newSource(gate)
        source.start(hospital)
        advance(6.seconds)

        gate.value = true
        runCurrent()
        advance(9.seconds)
        assertEquals("간격을 다 세기 전에는 그대로다", GuidanceState.Guiding(hospital, 3.minutes), source.state.value)

        advance(1.seconds)
        assertEquals(GuidanceState.Guiding(hospital, 2.minutes), source.state.value)

        advance(10.seconds)
        assertEquals(GuidanceState.Guiding(hospital, 1.minutes), source.state.value)
    }

    @Test
    fun `세는 도중에 거짓이 되면 그 간격까지만 줄고 멈춘다`() = runTest {
        val gate = MutableStateFlow(true)
        val source = newSource(gate)
        source.start(hospital)
        advance(6.seconds) // 1초부터 두 번째 간격을 세는 중

        gate.value = false
        advance(5.seconds) // 11초: 세던 간격은 끝까지 센다
        assertEquals(GuidanceState.Guiding(hospital, 2.minutes), source.state.value)

        advance(1.minutes)
        assertEquals("다음 간격은 세지 않는다", GuidanceState.Guiding(hospital, 2.minutes), source.state.value)
    }

    @Test
    fun `마지막 간격도 진행 조건이 참일 때만 지나서 도착한다`() = runTest {
        val gate = MutableStateFlow(true)
        val source = newSource(gate)
        source.start(hospital)
        advance(20.seconds)

        gate.value = false
        advance(1.seconds) // 21초: 1분을 보여주고 멈춘다
        assertEquals(GuidanceState.Guiding(hospital, 1.minutes), source.state.value)

        advance(1.minutes)
        assertEquals("조건이 거짓이면 도착하지 않는다", GuidanceState.Guiding(hospital, 1.minutes), source.state.value)

        gate.value = true
        runCurrent()
        advance(10.seconds)
        assertEquals(GuidanceState.Arrived(hospital), source.state.value)
    }

    @Test
    fun `멈춰 있는 동안 안내를 취소하면 안내 없음으로 돌아간다`() = runTest {
        val gate = MutableStateFlow(false)
        val source = newSource(gate)
        source.start(hospital)
        advance(5.seconds)

        source.cancel()
        assertEquals(GuidanceState.Idle, source.state.value)

        gate.value = true
        advance(1.minutes)
        assertEquals(GuidanceState.Idle, source.state.value)
    }

    @Test
    fun `안내 한 번에 진행 조건을 한 번만 구독하고 도착하거나 취소하면 구독을 끊는다`() = runTest {
        val gate = MutableStateFlow(true)
        val source = newSource(gate)
        assertEquals("안내 전에는 구독하지 않는다", 0, gate.subscriptionCount.value)

        source.start(hospital)
        runCurrent()
        assertEquals(1, gate.subscriptionCount.value)

        advance(21.seconds) // 간격을 여러 번 지나도
        assertEquals("간격마다 새로 구독하지 않는다", 1, gate.subscriptionCount.value)

        advance(10.seconds)
        assertEquals(GuidanceState.Arrived(hospital), source.state.value)
        assertEquals("도착하면 구독을 끊는다", 0, gate.subscriptionCount.value)

        source.dismissArrival()
        source.start(hospital)
        runCurrent()
        assertEquals(1, gate.subscriptionCount.value)
        source.cancel()
        runCurrent()
        assertEquals("취소하면 구독을 끊는다", 0, gate.subscriptionCount.value)
    }
}
