package com.meongcare.app.data.guidance

import app.cash.turbine.test
import com.meongcare.app.domain.guidance.Destination
import com.meongcare.app.domain.guidance.GuidanceState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
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
 * 계획 005 결정 2("규칙", 흐름 표)와 [com.meongcare.app.domain.guidance.GuidanceSource] 구독 계약의 경계 조건.
 *
 * 짧은 timing의 흐름: 0초 경로 찾는 중 → 1초 3분 → 11초 2분 → 21초 1분 → 31초 도착
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FakeGuidanceSourceBoundaryTest {

    private val hospital = Destination(name = "테스트 병원", latitude = 37.0, longitude = 127.0)
    private val otherHospital = Destination(name = "다른 병원", latitude = 37.1, longitude = 127.1)

    private val timing = FakeGuidanceTiming(
        routeCalculationDelay = 1.seconds,
        initialRemaining = 3.minutes,
        remainingStep = 1.minutes,
        tickInterval = 10.seconds,
    )

    private fun TestScope.newSource(t: FakeGuidanceTiming = timing) = FakeGuidanceSource(backgroundScope, t)

    private fun TestScope.advance(time: Duration) {
        advanceTimeBy(time)
        runCurrent()
    }

    /** 구독자가 받은 값을 모두 모은다. */
    private fun TestScope.record(source: FakeGuidanceSource): List<GuidanceState> {
        val emitted = mutableListOf<GuidanceState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { source.state.toList(emitted) }
        return emitted
    }

    // ---- 구독 계약 ----

    @Test
    fun `안내 중에 구독하면 지금 상태를 먼저 받는다`() = runTest {
        val source = newSource()
        source.start(hospital)
        advance(11.seconds)

        source.state.test {
            assertEquals(GuidanceState.Guiding(hospital, 2.minutes), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `같은 상태를 연달아 내보내지 않는다`() = runTest {
        val source = newSource()
        val emitted = record(source)

        source.cancel() // 안내 없음에서 취소
        source.dismissArrival() // 안내 없음에서 도착 닫기
        source.start(hospital)
        source.start(hospital) // 경로를 찾는 중에 같은 목적지로 다시 시작
        advance(1.seconds)

        assertEquals(
            listOf(
                GuidanceState.Idle,
                GuidanceState.Guiding(hospital, null),
                GuidanceState.Guiding(hospital, 3.minutes),
            ),
            emitted,
        )
    }

    // ---- 흐름 표 ----

    // 계획 결정 2의 기본값 표. 시연 속도를 바꾸면 계획의 표와 이 테스트를 같이 고친다.
    @Test
    fun `기본 시간 설정은 1초 뒤 12분에서 시작해 2초마다 1분씩 줄고 25초에 도착한다`() = runTest {
        val source = FakeGuidanceSource(backgroundScope)
        source.start(hospital)

        advance(999.milliseconds)
        assertEquals(GuidanceState.Guiding(hospital, null), source.state.value)
        advance(1.milliseconds) // 1초
        assertEquals(GuidanceState.Guiding(hospital, 12.minutes), source.state.value)
        advance(2.seconds) // 3초
        assertEquals(GuidanceState.Guiding(hospital, 11.minutes), source.state.value)
        advance(20.seconds) // 23초
        assertEquals(GuidanceState.Guiding(hospital, 1.minutes), source.state.value)
        advance(1999.milliseconds) // 24.999초
        assertEquals(GuidanceState.Guiding(hospital, 1.minutes), source.state.value)
        advance(1.milliseconds) // 25초
        assertEquals(GuidanceState.Arrived(hospital), source.state.value)
    }

    @Test
    fun `남은 시간이 단계로 나누어떨어지지 않아도 0이나 음수 없이 도착한다`() = runTest {
        val source = newSource(timing.copy(initialRemaining = 150.seconds))
        val emitted = record(source)

        source.start(hospital)
        advance(10.minutes)

        val remainings = emitted.filterIsInstance<GuidanceState.Guiding>().mapNotNull { it.remainingTime }
        assertTrue("남은 시간을 내보낸다: $emitted", remainings.isNotEmpty())
        assertTrue("남은 시간은 모두 0보다 크다: $remainings", remainings.all { it.isPositive() })
        assertEquals("남은 시간은 줄어들기만 한다", remainings.sortedDescending(), remainings)
        assertEquals(GuidanceState.Arrived(hospital), emitted.last())
    }

    // ---- 취소 ----

    @Test
    fun `경로를 찾는 중에 취소하면 처음 남은 시간을 내보내지 않는다`() = runTest {
        val source = newSource()
        val emitted = record(source)

        source.start(hospital)
        advance(500.milliseconds)
        source.cancel()
        advance(1.minutes)

        assertEquals(
            listOf(GuidanceState.Idle, GuidanceState.Guiding(hospital, null), GuidanceState.Idle),
            emitted,
        )
    }

    @Test
    fun `남은 시간이 바뀌는 바로 그 순간에 취소해도 취소가 이긴다`() = runTest {
        val source = newSource()
        source.start(hospital)
        advanceTimeBy(11.seconds) // 11초에 줄어들 차례인 진행은 아직 돌지 않았다
        assertEquals("아직 줄기 전이다", GuidanceState.Guiding(hospital, 3.minutes), source.state.value)

        source.cancel()
        runCurrent()
        assertEquals(GuidanceState.Idle, source.state.value)

        advance(1.minutes)
        assertEquals(GuidanceState.Idle, source.state.value)
    }

    @Test
    fun `도착하는 바로 그 순간에 취소하면 도착하지 않고 안내 없음이다`() = runTest {
        val source = newSource()
        source.start(hospital)
        advanceTimeBy(31.seconds) // 31초에 도착할 차례인 진행은 아직 돌지 않았다
        assertEquals("아직 도착 전이다", GuidanceState.Guiding(hospital, 1.minutes), source.state.value)

        source.cancel()
        runCurrent()
        assertEquals(GuidanceState.Idle, source.state.value)

        advance(1.minutes)
        assertEquals(GuidanceState.Idle, source.state.value)
    }

    @Test
    fun `도착한 뒤에 취소해도 도착 상태 그대로다`() = runTest {
        val source = newSource()
        source.start(hospital)
        advance(31.seconds)

        source.cancel()

        assertEquals(GuidanceState.Arrived(hospital), source.state.value)
    }

    @Test
    fun `안내 없음에서 취소하면 아무것도 바뀌지 않는다`() = runTest {
        val source = newSource()
        val emitted = record(source)

        source.cancel()
        advance(1.minutes)

        assertEquals(listOf(GuidanceState.Idle), emitted)
    }

    @Test
    fun `취소한 뒤에 다시 시작하면 이전 남은 시간에서 이어 세지 않고 처음부터 센다`() = runTest {
        val source = newSource()
        source.start(hospital)
        advance(11.seconds)
        source.cancel()

        source.start(hospital)
        assertEquals(GuidanceState.Guiding(hospital, null), source.state.value)

        advance(1.seconds)
        assertEquals(GuidanceState.Guiding(hospital, 3.minutes), source.state.value)
    }

    // ---- 도착 닫기 ----

    @Test
    fun `안내 없음에서 도착 화면 닫기는 아무것도 바꾸지 않는다`() = runTest {
        val source = newSource()
        val emitted = record(source)

        source.dismissArrival()
        advance(1.minutes)

        assertEquals(listOf(GuidanceState.Idle), emitted)
    }

    @Test
    fun `안내 중에 도착 화면 닫기를 불러도 안내는 그대로 이어진다`() = runTest {
        val source = newSource()
        source.start(hospital)
        advance(11.seconds)

        source.dismissArrival()
        assertEquals(GuidanceState.Guiding(hospital, 2.minutes), source.state.value)

        advance(20.seconds)
        assertEquals(GuidanceState.Arrived(hospital), source.state.value)
    }

    // ---- 다시 시작 ----

    @Test
    fun `도착한 뒤에 다시 시작하면 새 안내를 처음부터 센다`() = runTest {
        val source = newSource()
        source.start(hospital)
        advance(31.seconds)
        assertEquals(GuidanceState.Arrived(hospital), source.state.value)

        source.start(otherHospital)
        assertEquals(GuidanceState.Guiding(otherHospital, null), source.state.value)

        advance(1.seconds)
        assertEquals(GuidanceState.Guiding(otherHospital, 3.minutes), source.state.value)

        advance(30.seconds)
        assertEquals(GuidanceState.Arrived(otherHospital), source.state.value)
    }

    @Test
    fun `안내 중에 같은 목적지로 다시 시작하면 처음부터 다시 센다`() = runTest {
        val source = newSource()
        source.start(hospital)
        advance(21.seconds)
        assertEquals(GuidanceState.Guiding(hospital, 1.minutes), source.state.value)

        source.start(hospital)
        assertEquals(GuidanceState.Guiding(hospital, null), source.state.value)

        advance(10.seconds) // 처음 시작부터 31초: 이전 진행이 살아 있으면 도착했을 때다
        assertEquals(GuidanceState.Guiding(hospital, 3.minutes), source.state.value)

        advance(21.seconds) // 다시 시작부터 31초
        assertEquals(GuidanceState.Arrived(hospital), source.state.value)
    }

    @Test
    fun `경로를 찾는 중에 다른 목적지로 바꾸면 이전 목적지 상태를 더 내보내지 않는다`() = runTest {
        val source = newSource()
        val emitted = record(source)

        source.start(hospital)
        advance(500.milliseconds)
        source.start(otherHospital)
        advance(1.minutes)

        assertEquals(
            listOf(
                GuidanceState.Idle,
                GuidanceState.Guiding(hospital, null),
                GuidanceState.Guiding(otherHospital, null),
                GuidanceState.Guiding(otherHospital, 3.minutes),
                GuidanceState.Guiding(otherHospital, 2.minutes),
                GuidanceState.Guiding(otherHospital, 1.minutes),
                GuidanceState.Arrived(otherHospital),
            ),
            emitted,
        )
    }
}
