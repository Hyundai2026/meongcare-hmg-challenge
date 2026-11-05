package com.meongcare.app.data.guidance

import app.cash.turbine.test
import com.meongcare.app.domain.guidance.Destination
import com.meongcare.app.domain.guidance.GuidanceState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Fake 길안내 진행 조건(계획 007 "설계 결정 3")의 경계 조건.
 *
 * 계약:
 * - 경로 계산은 조건과 상관없이 끝난다.
 * - 간격 하나를 세기 전에 조건이 참이 될 때까지 기다린다. 세던 간격은 끝까지 센다.
 * - 안내 한 번(시작 → 도착·취소·다른 목적지로 다시 시작) 동안 조건을 한 번만 구독하고, 끝나면 끊는다.
 * - 조건이 `null`이면 지금(시간만으로 흐름)과 같다.
 * - [com.meongcare.app.domain.guidance.GuidanceSource] 구독 계약: 현재 상태를 먼저 받고, 같은 상태를 연달아 받지 않는다.
 *
 * 조건이 계속 참일 때의 흐름: 0초 경로 찾는 중 → 1초 3분 → 11초 2분 → 21초 1분 → 31초 도착
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FakeGuidanceSourceProgressGateBoundaryTest {

    private val hospital = Destination(name = "테스트 병원", latitude = 37.0, longitude = 127.0)
    private val otherHospital = Destination(name = "다른 병원", latitude = 37.1, longitude = 127.1)

    private val timing = FakeGuidanceTiming(
        routeCalculationDelay = 1.seconds,
        initialRemaining = 3.minutes,
        remainingStep = 1.minutes,
        tickInterval = 10.seconds,
    )

    /**
     * 구독할 때마다 횟수를 세는 진행 조건. Real 주행 상태처럼 구독할 때 연결하고 구독이 끝나면 끊는 출처를 흉내 낸다.
     * `MutableStateFlow.subscriptionCount`는 그 순간의 구독 수라서, 간격마다 끊고 다시 구독해도 1로 보인다.
     * 그래서 누적 횟수로 센다.
     */
    private class RecordingGate(initial: Boolean) {
        private val value = MutableStateFlow(initial)
        var subscribed = 0
            private set
        var unsubscribed = 0
            private set
        val active: Int get() = subscribed - unsubscribed

        val flow: Flow<Boolean> = value
            .onStart { subscribed++ }
            .onCompletion { unsubscribed++ }

        fun open() {
            value.value = true
        }

        fun close() {
            value.value = false
        }
    }

    private fun TestScope.newSource(gate: Flow<Boolean>?) = FakeGuidanceSource(backgroundScope, timing, progressGate = gate)

    private fun TestScope.advance(time: Duration) {
        advanceTimeBy(time)
        runCurrent()
    }

    private fun TestScope.record(source: FakeGuidanceSource): List<GuidanceState> {
        val emitted = mutableListOf<GuidanceState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { source.state.collect { emitted += it } }
        return emitted
    }

    private fun TestScope.recordTimeline(source: FakeGuidanceSource): List<Pair<Long, GuidanceState>> {
        val emitted = mutableListOf<Pair<Long, GuidanceState>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            source.state.collect { emitted += testScheduler.currentTime to it }
        }
        return emitted
    }

    private fun guiding(destination: Destination, remaining: Duration?) = GuidanceState.Guiding(destination, remaining)

    private fun GuidanceState.destinationOrNull(): Destination? = when (this) {
        GuidanceState.Idle -> null
        is GuidanceState.Guiding -> destination
        is GuidanceState.Arrived -> destination
    }

    // ---- 진행 조건이 없을 때, 늘 참일 때 ----

    @Test
    fun `진행 조건이 늘 참이면 진행 조건이 없을 때와 같은 시각에 같은 상태를 내보낸다`() = runTest {
        val withoutGate = newSource(gate = null)
        val alwaysOpen = newSource(gate = MutableStateFlow(true))
        val withoutGateTimeline = recordTimeline(withoutGate)
        val alwaysOpenTimeline = recordTimeline(alwaysOpen)

        withoutGate.start(hospital)
        alwaysOpen.start(hospital)
        advance(1.minutes)

        val expected = listOf(
            0L to GuidanceState.Idle,
            0L to guiding(hospital, null),
            1_000L to guiding(hospital, 3.minutes),
            11_000L to guiding(hospital, 2.minutes),
            21_000L to guiding(hospital, 1.minutes),
            31_000L to GuidanceState.Arrived(hospital),
        )
        assertEquals("진행 조건이 없으면 시간만으로 흐른다", expected, withoutGateTimeline)
        assertEquals(expected, alwaysOpenTimeline)
    }

    // ---- 경로 계산 ----

    @Test
    fun `경로를 찾는 중에 참이었다가 거짓이 되면 처음 남은 시간에서 멈춘다`() = runTest {
        val gate = MutableStateFlow(false)
        val source = newSource(gate)
        source.start(hospital)

        advance(300.milliseconds)
        gate.value = true
        advance(300.milliseconds)
        gate.value = false
        advance(400.milliseconds) // 1초: 경로 계산이 끝났을 때는 거짓
        assertEquals(guiding(hospital, 3.minutes), source.state.value)

        advance(1.minutes)
        assertEquals(guiding(hospital, 3.minutes), source.state.value)
    }

    @Test
    fun `경로를 찾는 중에 참이 되면 경로 계산이 끝나자마자 첫 간격을 센다`() = runTest {
        val gate = MutableStateFlow(false)
        val source = newSource(gate)
        source.start(hospital)

        advance(500.milliseconds)
        gate.value = true
        advance(500.milliseconds) // 1초
        assertEquals(guiding(hospital, 3.minutes), source.state.value)

        advance(9.seconds) // 10초
        assertEquals(guiding(hospital, 3.minutes), source.state.value)
        advance(1.seconds) // 11초
        assertEquals(guiding(hospital, 2.minutes), source.state.value)
    }

    @Test
    fun `값을 하나도 보내지 않는 진행 조건이면 경로 계산은 끝나고 남은 시간은 줄지 않는다`() = runTest {
        val silent = flow<Boolean> { awaitCancellation() }
        val source = newSource(silent)

        source.start(hospital)
        advance(1.seconds)
        assertEquals(guiding(hospital, 3.minutes), source.state.value)

        advance(10.minutes)
        assertEquals(guiding(hospital, 3.minutes), source.state.value)
    }

    // ---- 간격 세기 ----

    @Test
    fun `참을 거듭 받아도 한 간격에 한 단계씩만 준다`() = runTest {
        val gate = MutableSharedFlow<Boolean>(replay = 1)
        gate.tryEmit(true)
        backgroundScope.launch {
            while (true) {
                delay(500.milliseconds)
                gate.emit(true)
            }
        }
        val source = newSource(gate)

        source.start(hospital)
        advance(10.seconds)
        assertEquals("10초", guiding(hospital, 3.minutes), source.state.value)
        advance(1.seconds)
        assertEquals("11초", guiding(hospital, 2.minutes), source.state.value)
        advance(9.seconds)
        assertEquals("20초", guiding(hospital, 2.minutes), source.state.value)
        advance(1.seconds)
        assertEquals("21초", guiding(hospital, 1.minutes), source.state.value)
        advance(10.seconds)
        assertEquals("31초", GuidanceState.Arrived(hospital), source.state.value)
    }

    @Test
    fun `멈췄다가 다시 참이 되면 그 시각부터 한 간격을 새로 센다`() = runTest {
        val gate = MutableStateFlow(true)
        val source = newSource(gate)
        source.start(hospital)
        advance(13.seconds) // 11초에 2분, 지금은 11~21초 간격을 세는 중

        gate.value = false
        advance(8.seconds) // 21초: 세던 간격은 끝까지 센다
        assertEquals(guiding(hospital, 1.minutes), source.state.value)

        advance(19.seconds) // 40초
        assertEquals(guiding(hospital, 1.minutes), source.state.value)

        gate.value = true
        advance(9.seconds) // 49초: 멈추기 전에 흐른 시간은 셈에 넣지 않는다
        assertEquals(guiding(hospital, 1.minutes), source.state.value)
        advance(1.seconds) // 50초
        assertEquals(GuidanceState.Arrived(hospital), source.state.value)
    }

    @Test
    fun `도착한 뒤 정차한 채로 다시 안내를 시작하면 처음 남은 시간에서 멈춘다`() = runTest {
        val gate = MutableStateFlow(true)
        val source = newSource(gate)
        source.start(hospital)
        advance(31.seconds)
        assertEquals(GuidanceState.Arrived(hospital), source.state.value)

        gate.value = false
        source.dismissArrival()
        source.start(hospital)
        advance(1.seconds)
        assertEquals(guiding(hospital, 3.minutes), source.state.value)

        advance(1.minutes)
        assertEquals("이전 안내에서 참이던 조건이 남지 않는다", guiding(hospital, 3.minutes), source.state.value)
    }

    // ---- 구독 계약 (GuidanceSource) ----

    @Test
    fun `진행 조건이 같은 값을 거듭 보내도 같은 상태를 연달아 내보내지 않는다`() = runTest {
        val gate = MutableSharedFlow<Boolean>(replay = 1)
        gate.tryEmit(false)
        var open = false
        backgroundScope.launch {
            while (true) {
                delay(500.milliseconds)
                gate.emit(open)
            }
        }
        val source = newSource(gate)
        val emitted = record(source)

        source.start(hospital)
        advance(30.seconds)
        assertEquals(
            listOf(GuidanceState.Idle, guiding(hospital, null), guiding(hospital, 3.minutes)),
            emitted,
        )

        open = true
        advance(1.minutes)
        assertEquals(
            listOf(
                GuidanceState.Idle,
                guiding(hospital, null),
                guiding(hospital, 3.minutes),
                guiding(hospital, 2.minutes),
                guiding(hospital, 1.minutes),
                GuidanceState.Arrived(hospital),
            ),
            emitted,
        )
    }

    @Test
    fun `멈춰 있는 동안 구독하면 지금 남은 시간을 먼저 받고 그 뒤로는 받지 않는다`() = runTest {
        val gate = MutableStateFlow(false)
        val source = newSource(gate)
        source.start(hospital)
        advance(5.seconds)

        source.state.test {
            assertEquals(guiding(hospital, 3.minutes), awaitItem())
            advance(1.minutes)
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `멈춰 있는 동안 도착 화면 닫기를 불러도 안내는 그대로 이어진다`() = runTest {
        val gate = MutableStateFlow(false)
        val source = newSource(gate)
        source.start(hospital)
        advance(5.seconds)

        source.dismissArrival()
        assertEquals(guiding(hospital, 3.minutes), source.state.value)

        gate.value = true
        advance(10.seconds)
        assertEquals(guiding(hospital, 2.minutes), source.state.value)
    }

    @Test
    fun `도착한 뒤에는 진행 조건이 바뀌어도 도착 화면을 닫을 때까지 도착 상태다`() = runTest {
        val gate = MutableStateFlow(true)
        val source = newSource(gate)
        source.start(hospital)
        advance(31.seconds)
        assertEquals(GuidanceState.Arrived(hospital), source.state.value)

        gate.value = false
        advance(1.minutes)
        assertEquals(GuidanceState.Arrived(hospital), source.state.value)
        gate.value = true
        advance(1.minutes)
        assertEquals(GuidanceState.Arrived(hospital), source.state.value)

        source.dismissArrival()
        assertEquals(GuidanceState.Idle, source.state.value)
        gate.value = false
        advance(1.seconds)
        gate.value = true
        advance(1.minutes)
        assertEquals(GuidanceState.Idle, source.state.value)
    }

    // ---- 다른 목적지로 다시 시작 ----

    @Test
    fun `안내 중에 다른 목적지로 다시 시작하면 남은 시간을 처음부터 다시 세고 이전 진행은 끼어들지 않는다`() = runTest {
        val gate = MutableStateFlow(true)
        val source = newSource(gate)
        val emitted = record(source)
        source.start(hospital)
        advance(15.seconds) // 11초에 2분, 11~21초 간격을 세는 중

        source.start(otherHospital)
        advance(1.seconds) // 16초
        assertEquals(guiding(otherHospital, 3.minutes), source.state.value)
        advance(5.seconds) // 21초: 이전 안내라면 1분이 됐을 시각
        assertEquals(guiding(otherHospital, 3.minutes), source.state.value)
        advance(5.seconds) // 26초
        assertEquals(guiding(otherHospital, 2.minutes), source.state.value)

        advance(2.minutes)
        assertEquals(GuidanceState.Arrived(otherHospital), source.state.value)
        val afterRestart = emitted.dropWhile { it != guiding(otherHospital, null) }
        assertTrue(
            "다시 시작한 뒤에 이전 목적지 상태가 나오지 않는다: $emitted",
            afterRestart.isNotEmpty() && afterRestart.none { it.destinationOrNull() == hospital },
        )
    }

    // ---- 구독 횟수와 해제 ----

    @Test
    fun `안내 한 번 동안 멈췄다 가기를 여러 번 해도 진행 조건은 한 번만 구독하고 도착하면 끊는다`() = runTest {
        val gate = RecordingGate(initial = false)
        val source = newSource(gate.flow)
        runCurrent()
        assertEquals("안내 전에는 구독하지 않는다", 0, gate.subscribed)

        source.start(hospital)
        advance(1.seconds) // 1초: 3분, 멈춤
        assertEquals(1, gate.subscribed)

        gate.open()
        advance(5.seconds)
        gate.close()
        advance(20.seconds) // 11초에 2분, 26초까지 멈춤
        gate.open()
        advance(5.seconds)
        gate.close()
        advance(20.seconds) // 36초에 1분, 51초까지 멈춤
        assertEquals(guiding(hospital, 1.minutes), source.state.value)
        assertEquals("간격마다 다시 구독하지 않는다", 1, gate.subscribed)
        assertEquals(1, gate.active)

        gate.open()
        advance(10.seconds) // 61초
        assertEquals(GuidanceState.Arrived(hospital), source.state.value)
        assertEquals(1, gate.subscribed)
        assertEquals("도착하면 구독을 끊는다", 0, gate.active)

        source.dismissArrival()
        advance(1.minutes)
        assertEquals("도착 화면을 닫아도 다시 구독하지 않는다", 1, gate.subscribed)
    }

    @Test
    fun `경로를 찾는 중에 취소하면 진행 조건 구독을 끊고 안내 없음으로 남는다`() = runTest {
        val gate = RecordingGate(initial = true)
        val source = newSource(gate.flow)
        source.start(hospital)
        advance(500.milliseconds)

        source.cancel()
        runCurrent()
        assertEquals(GuidanceState.Idle, source.state.value)
        assertEquals(0, gate.active)

        advance(1.minutes)
        assertEquals(GuidanceState.Idle, source.state.value)
        assertEquals(0, gate.active)
    }

    @Test
    fun `멈춰 있는 동안 다른 목적지로 다시 시작하면 이전 구독을 끊고 새 안내도 경로 계산은 끝난다`() = runTest {
        val gate = RecordingGate(initial = false)
        val source = newSource(gate.flow)
        source.start(hospital)
        advance(5.seconds)
        assertEquals(1, gate.subscribed)

        source.start(otherHospital)
        runCurrent()
        assertEquals(guiding(otherHospital, null), source.state.value)
        assertEquals("이전 안내의 구독은 끊는다", 1, gate.unsubscribed)
        assertEquals("새 안내의 구독은 하나다", 1, gate.active)

        advance(1.seconds)
        assertEquals(guiding(otherHospital, 3.minutes), source.state.value)
        advance(1.minutes)
        assertEquals(guiding(otherHospital, 3.minutes), source.state.value)
        assertEquals(2, gate.subscribed)
        assertEquals(1, gate.active)

        source.cancel()
        runCurrent()
        assertEquals(0, gate.active)
    }
}
