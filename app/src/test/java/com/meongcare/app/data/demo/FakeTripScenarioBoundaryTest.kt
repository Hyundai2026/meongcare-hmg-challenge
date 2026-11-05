package com.meongcare.app.data.demo

import com.meongcare.app.data.driving.FakeDrivingStateSource
import com.meongcare.app.data.guidance.FakeGuidanceSource
import com.meongcare.app.data.guidance.FakeGuidanceTiming
import com.meongcare.app.domain.driving.DrivingState
import com.meongcare.app.domain.guidance.Destination
import com.meongcare.app.domain.guidance.GuidanceSource
import com.meongcare.app.domain.guidance.GuidanceState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.TestScope
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

/** 계획 005 결정 3: 시나리오 전환, 취소, debug 패널 수동 조작과의 관계, 구독 수명. */
@OptIn(ExperimentalCoroutinesApi::class)
class FakeTripScenarioBoundaryTest {

    private val hospital = Destination(name = "테스트 병원", latitude = 37.0, longitude = 127.0)
    private val otherHospital = Destination(name = "다른 병원", latitude = 37.1, longitude = 127.1)

    // 길안내: 0초 시작 → 1초 3분 → 11초 2분 → 21초 1분 → 31초 도착
    private val slowTicks = FakeGuidanceTiming(
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

    private fun TestScope.startScenario(
        initialDriving: DrivingState = DrivingState.PARKED,
        guidanceTiming: FakeGuidanceTiming = slowTicks,
    ): Fixture {
        val guidance = FakeGuidanceSource(backgroundScope, guidanceTiming)
        val driving = FakeDrivingStateSource(initialDriving)
        FakeTripScenario(backgroundScope, guidance, driving, tripTiming).start()
        runCurrent()
        return Fixture(guidance, driving)
    }

    private fun TestScope.advance(time: Duration) {
        advanceTimeBy(time)
        runCurrent()
    }

    @Test
    fun `안내가 없으면 시나리오는 주행 상태를 건드리지 않는다`() = runTest {
        val f = startScenario(initialDriving = DrivingState.UNKNOWN)

        advance(1.minutes)
        assertEquals(DrivingState.UNKNOWN, f.driving.drivingState.value)

        f.driving.set(DrivingState.DRIVING) // debug 패널 [주행]
        advance(1.minutes)
        assertEquals(DrivingState.DRIVING, f.driving.drivingState.value)
    }

    @Test
    fun `남은 시간이 출발 지연보다 자주 바뀌어도 출발 지연 뒤에 주행 상태가 된다`() = runTest {
        // 0초 경로 찾는 중 → 0.5초 12분 → 1.5초 11분 → 2.5초 10분 → … (출발 지연 3초보다 자주 바뀐다)
        val f = startScenario(
            guidanceTiming = FakeGuidanceTiming(
                routeCalculationDelay = 500.milliseconds,
                initialRemaining = 12.minutes,
                remainingStep = 1.minutes,
                tickInterval = 1.seconds,
            ),
        )

        f.guidance.start(hospital)
        advance(2999.milliseconds)
        assertEquals(DrivingState.PARKED, f.driving.drivingState.value)

        advance(1.milliseconds)
        assertEquals(DrivingState.DRIVING, f.driving.drivingState.value)
    }

    @Test
    fun `출발 지연 중에 취소하고 다시 시작하면 다시 시작한 때부터 출발 지연을 잰다`() = runTest {
        val f = startScenario()

        f.guidance.start(hospital)
        advance(2.seconds)
        f.guidance.cancel()
        advance(500.milliseconds)
        f.guidance.start(hospital) // 2.5초에 다시 시작

        advance(500.milliseconds) // 3초: 처음 시작의 출발 시각
        assertEquals("처음 시작의 출발은 취소됐다", DrivingState.PARKED, f.driving.drivingState.value)

        advance(2499.milliseconds) // 5.499초
        assertEquals(DrivingState.PARKED, f.driving.drivingState.value)

        advance(1.milliseconds) // 5.5초 = 다시 시작 + 3초
        assertEquals(DrivingState.DRIVING, f.driving.drivingState.value)
    }

    @Test
    fun `안내 중에 목적지를 바꿔도 출발은 한다`() = runTest {
        val f = startScenario()

        f.guidance.start(hospital)
        advance(2.seconds)
        f.guidance.start(otherHospital)
        advance(10.seconds) // 다른 병원 도착(2초 + 31초)보다 한참 전

        assertTrue(f.guidance.state.value is GuidanceState.Guiding)
        assertEquals(DrivingState.DRIVING, f.driving.drivingState.value)
    }

    @Test
    fun `주행 중에 손으로 정차로 바꾸면 안내가 계속되어도 다시 주행으로 바꾸지 않는다`() = runTest {
        val f = startScenario()
        f.guidance.start(hospital)
        advance(3.seconds)
        assertEquals(DrivingState.DRIVING, f.driving.drivingState.value)

        f.driving.set(DrivingState.PARKED) // debug 패널 [정차]
        advance(20.seconds) // 남은 시간이 11초, 21초에 두 번 줄어든다

        assertEquals(GuidanceState.Guiding(hospital, 1.minutes), f.guidance.state.value)
        assertEquals(DrivingState.PARKED, f.driving.drivingState.value)
    }

    @Test
    fun `손으로 바꾼 주행 상태도 도착하고 주차 지연이 지나면 정차로 덮어쓴다`() = runTest {
        val f = startScenario()
        f.guidance.start(hospital)
        advance(3.seconds)

        f.driving.set(DrivingState.UNKNOWN) // debug 패널 [모름]
        advance(28.seconds) // 31초 도착
        assertEquals(GuidanceState.Arrived(hospital), f.guidance.state.value)
        assertEquals(DrivingState.UNKNOWN, f.driving.drivingState.value)

        advance(5.seconds)
        assertEquals(DrivingState.PARKED, f.driving.drivingState.value)
    }

    @Test
    fun `도착하고 주차 지연 전에 새 안내를 시작하면 주차하지 않고 계속 주행한다`() = runTest {
        val f = startScenario()
        f.guidance.start(hospital)
        advance(31.seconds)
        assertEquals(GuidanceState.Arrived(hospital), f.guidance.state.value)
        assertEquals(DrivingState.DRIVING, f.driving.drivingState.value)

        advance(2.seconds)
        f.guidance.start(otherHospital) // 33초: 도착 → 안내 중

        advance(3.seconds) // 36초: 앞 도착의 주차 시각
        assertEquals(DrivingState.DRIVING, f.driving.drivingState.value)

        advance(10.seconds)
        assertEquals(DrivingState.DRIVING, f.driving.drivingState.value)
    }

    @Test
    fun `도착 화면을 닫아도 정차 그대로이고 새 안내를 시작하면 다시 출발한다`() = runTest {
        val f = startScenario()
        f.guidance.start(hospital)
        advance(36.seconds) // 31초 도착 + 5초 주차
        assertEquals(DrivingState.PARKED, f.driving.drivingState.value)

        f.guidance.dismissArrival()
        advance(1.minutes)
        assertEquals(GuidanceState.Idle, f.guidance.state.value)
        assertEquals(DrivingState.PARKED, f.driving.drivingState.value)

        f.guidance.start(hospital)
        advance(3.seconds)
        assertEquals(DrivingState.DRIVING, f.driving.drivingState.value)
    }

    @Test
    fun `시나리오를 두 번 시작해도 길안내를 한 번만 구독한다`() = runTest {
        val guidance = ControlledGuidanceSource()
        val scenario = FakeTripScenario(backgroundScope, guidance, FakeDrivingStateSource(), tripTiming)

        scenario.start()
        scenario.start()
        runCurrent()

        assertEquals(1, guidance.state.subscriptionCount.value)
    }

    @Test
    fun `시나리오의 scope가 끝나면 길안내 구독을 끊고 주행 상태를 더 바꾸지 않는다`() = runTest {
        val guidance = ControlledGuidanceSource()
        val driving = FakeDrivingStateSource(DrivingState.PARKED)
        val scenarioScope = backgroundScope + Job()
        FakeTripScenario(scenarioScope, guidance, driving, tripTiming).start()
        runCurrent()
        assertEquals(1, guidance.state.subscriptionCount.value)

        scenarioScope.cancel()
        runCurrent()
        assertEquals(0, guidance.state.subscriptionCount.value)

        guidance.state.value = GuidanceState.Guiding(hospital, null)
        advance(1.minutes)
        assertEquals(DrivingState.PARKED, driving.drivingState.value)
    }
}

/** 상태를 테스트가 직접 바꾸는 길안내 소스. 구독 수를 보려고 쓴다. */
private class ControlledGuidanceSource : GuidanceSource {
    override val state = MutableStateFlow<GuidanceState>(GuidanceState.Idle)
    override suspend fun start(destination: Destination) = Unit
    override suspend fun cancel() = Unit
    override fun dismissArrival() = Unit
}
