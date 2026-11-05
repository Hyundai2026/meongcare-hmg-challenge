package com.meongcare.app.data.guidance

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
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class FakeGuidanceSourceTest {

    private val hospital = Destination(name = "테스트 병원", latitude = 37.0, longitude = 127.0)
    private val otherHospital = Destination(name = "다른 병원", latitude = 37.1, longitude = 127.1)

    // 흐름: 0초 경로 찾는 중 → 1초 3분 → 11초 2분 → 21초 1분 → 31초 도착
    private val timing = FakeGuidanceTiming(
        routeCalculationDelay = 1.seconds,
        initialRemaining = 3.minutes,
        remainingStep = 1.minutes,
        tickInterval = 10.seconds,
    )

    private fun TestScope.newSource() = FakeGuidanceSource(backgroundScope, timing)

    private fun TestScope.advance(time: Duration) {
        advanceTimeBy(time)
        runCurrent()
    }

    @Test
    fun `처음에는 안내 중이 아니다`() = runTest {
        assertEquals(GuidanceState.Idle, newSource().state.value)
    }

    @Test
    fun `안내를 시작하면 남은 시간을 모르는 안내 중 상태를 먼저 내보낸다`() = runTest {
        val source = newSource()

        source.start(hospital)

        assertEquals(GuidanceState.Guiding(hospital, remainingTime = null), source.state.value)
    }

    @Test
    fun `경로 계산 시간이 지나면 처음 남은 시간을 내보낸다`() = runTest {
        val source = newSource()
        source.start(hospital)

        advance(1.seconds)

        assertEquals(GuidanceState.Guiding(hospital, 3.minutes), source.state.value)
    }

    @Test
    fun `시간이 지나면 남은 시간이 한 단계씩 줄어든다`() = runTest {
        val source = newSource()
        source.start(hospital)
        advance(1.seconds)

        advance(10.seconds)
        assertEquals(GuidanceState.Guiding(hospital, 2.minutes), source.state.value)

        advance(10.seconds)
        assertEquals(GuidanceState.Guiding(hospital, 1.minutes), source.state.value)
    }

    @Test
    fun `남은 시간이 다 줄면 0분 없이 도착 상태를 내보낸다`() = runTest {
        val source = newSource()
        val emitted = mutableListOf<GuidanceState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { source.state.toList(emitted) }

        source.start(hospital)
        advance(31.seconds)

        assertEquals(
            listOf(
                GuidanceState.Idle,
                GuidanceState.Guiding(hospital, null),
                GuidanceState.Guiding(hospital, 3.minutes),
                GuidanceState.Guiding(hospital, 2.minutes),
                GuidanceState.Guiding(hospital, 1.minutes),
                GuidanceState.Arrived(hospital),
            ),
            emitted,
        )
    }

    @Test
    fun `안내를 취소하면 안내 없음으로 돌아가고 시간이 지나도 더 바뀌지 않는다`() = runTest {
        val source = newSource()
        source.start(hospital)
        advance(11.seconds)

        source.cancel()
        assertEquals(GuidanceState.Idle, source.state.value)

        advance(1.minutes)
        assertEquals(GuidanceState.Idle, source.state.value)
    }

    @Test
    fun `도착 화면을 닫으면 안내 없음으로 돌아간다`() = runTest {
        val source = newSource()
        source.start(hospital)
        advance(31.seconds)
        assertEquals(GuidanceState.Arrived(hospital), source.state.value)

        source.dismissArrival()

        assertEquals(GuidanceState.Idle, source.state.value)
    }

    @Test
    fun `안내 중에 다른 목적지로 시작하면 새 목적지로 처음부터 다시 센다`() = runTest {
        val source = newSource()
        source.start(hospital)
        advance(11.seconds)
        assertEquals(GuidanceState.Guiding(hospital, 2.minutes), source.state.value)

        source.start(otherHospital)
        assertEquals(GuidanceState.Guiding(otherHospital, null), source.state.value)

        advance(1.seconds)
        assertEquals(GuidanceState.Guiding(otherHospital, 3.minutes), source.state.value)

        advance(30.seconds)
        assertEquals(GuidanceState.Arrived(otherHospital), source.state.value)
    }
}
