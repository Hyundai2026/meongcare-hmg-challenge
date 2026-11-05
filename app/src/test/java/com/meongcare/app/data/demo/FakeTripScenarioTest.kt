package com.meongcare.app.data.demo

import com.meongcare.app.data.driving.FakeDrivingStateSource
import com.meongcare.app.data.guidance.FakeGuidanceSource
import com.meongcare.app.data.guidance.FakeGuidanceTiming
import com.meongcare.app.domain.driving.DrivingState
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
class FakeTripScenarioTest {

    private val hospital = Destination(name = "테스트 병원", latitude = 37.0, longitude = 127.0)

    // 길안내: 0초 시작 → 31초 도착 (FakeGuidanceSourceTest와 같은 흐름)
    private val guidanceTiming = FakeGuidanceTiming(
        routeCalculationDelay = 1.seconds,
        initialRemaining = 3.minutes,
        remainingStep = 1.minutes,
        tickInterval = 10.seconds,
    )
    private val tripTiming = FakeTripTiming(departDelay = 3.seconds, parkDelay = 5.seconds)

    private class Fixture(
        val guidance: FakeGuidanceSource,
        val driving: FakeDrivingStateSource,
    )

    private fun TestScope.startScenario(): Fixture {
        val guidance = FakeGuidanceSource(backgroundScope, guidanceTiming)
        val driving = FakeDrivingStateSource(DrivingState.PARKED)
        FakeTripScenario(backgroundScope, guidance, driving, tripTiming).start()
        runCurrent()
        return Fixture(guidance, driving)
    }

    private fun TestScope.advance(time: Duration) {
        advanceTimeBy(time)
        runCurrent()
    }

    @Test
    fun `안내를 시작하고 출발 지연이 지나면 주행 상태가 된다`() = runTest {
        val f = startScenario()

        f.guidance.start(hospital)
        advance(2.seconds)
        assertEquals("출발 지연 전에는 정차 그대로다", DrivingState.PARKED, f.driving.drivingState.value)

        advance(1.seconds)
        assertEquals(DrivingState.DRIVING, f.driving.drivingState.value)
    }

    @Test
    fun `출발 지연 전에 안내를 취소하면 주행 상태로 바꾸지 않는다`() = runTest {
        val f = startScenario()

        f.guidance.start(hospital)
        advance(2.seconds)
        f.guidance.cancel()
        advance(1.minutes)

        assertEquals(DrivingState.PARKED, f.driving.drivingState.value)
    }

    @Test
    fun `도착하고 주차 지연이 지나면 정차 상태가 된다`() = runTest {
        val f = startScenario()

        f.guidance.start(hospital)
        advance(31.seconds)
        assertEquals(GuidanceState.Arrived(hospital), f.guidance.state.value)
        assertEquals("도착 직후에는 아직 주행이다", DrivingState.DRIVING, f.driving.drivingState.value)

        advance(5.seconds)
        assertEquals(DrivingState.PARKED, f.driving.drivingState.value)
    }

    @Test
    fun `주행 중에 안내를 취소해도 주행 상태는 그대로다`() = runTest {
        val f = startScenario()

        f.guidance.start(hospital)
        advance(11.seconds)
        assertEquals(DrivingState.DRIVING, f.driving.drivingState.value)

        f.guidance.cancel()
        advance(1.minutes)

        assertEquals(DrivingState.DRIVING, f.driving.drivingState.value)
    }
}
