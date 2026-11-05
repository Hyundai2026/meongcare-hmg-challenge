package com.meongcare.app.data.demo

import com.meongcare.app.data.driving.FakeDrivingStateSource
import com.meongcare.app.data.guidance.FakeGuidanceSource
import com.meongcare.app.data.guidance.FakeGuidanceTiming
import com.meongcare.app.domain.driving.DrivingState
import com.meongcare.app.domain.guidance.Destination
import com.meongcare.app.testutil.MainDispatcherRule
import com.meongcare.app.ui.guidance.GuidanceUiState
import com.meongcare.app.ui.guidance.GuidanceViewModel
import com.meongcare.app.ui.home.HomeContent
import com.meongcare.app.ui.home.HomeViewModel
import com.meongcare.app.ui.home.homeContentOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * 계획 005 완료 기준 3 "시연 시나리오"를 화면 상태 수준에서 따라간다 (FAKE 모드 배선과 같은 조합).
 * Fake 길안내 + Fake 주행 상태 + 시나리오 → HomeViewModel·GuidanceViewModel → homeContentOf.
 *
 * 짧은 timing: 0초 시작 → 1초 3분 → 3초 출발 → 11초 2분 → 21초 1분 → 31초 도착 → 36초 주차
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FakeTripScenarioScreenFlowTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val hospital = Destination(name = "시연 병원", latitude = 37.0, longitude = 127.0)

    private val guidanceTiming = FakeGuidanceTiming(
        routeCalculationDelay = 1.seconds,
        initialRemaining = 3.minutes,
        remainingStep = 1.minutes,
        tickInterval = 10.seconds,
    )
    private val tripTiming = FakeTripTiming(departDelay = 3.seconds, parkDelay = 5.seconds)

    private class Demo(
        val driving: FakeDrivingStateSource,
        private val home: HomeViewModel,
        val guidance: GuidanceViewModel,
    ) {
        val content: HomeContent get() = homeContentOf(home.uiState.value, guidance.uiState.value)
        val guidanceUi: GuidanceUiState get() = guidance.uiState.value
    }

    private fun TestScope.startDemo(): Demo {
        val driving = FakeDrivingStateSource(DrivingState.PARKED)
        val guidanceSource = FakeGuidanceSource(backgroundScope, guidanceTiming)
        FakeTripScenario(backgroundScope, guidanceSource, driving, tripTiming).start()
        val home = HomeViewModel(driving)
        val guidance = GuidanceViewModel(guidanceSource, hospital)
        // 화면이 보고 있는 것처럼 두 상태를 계속 구독한다.
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { home.uiState.collect {} }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { guidance.uiState.collect {} }
        runCurrent()
        return Demo(driving, home, guidance)
    }

    private fun TestScope.advance(time: Duration) {
        advanceTimeBy(time)
        runCurrent()
    }

    @Test
    fun `안내를 시작하면 출발하고 주행 중 도착은 주행 화면이며 주차하면 도착 화면이 되고 닫으면 처음 정차 화면이다`() = runTest {
        val demo = startDemo()
        assertEquals(HomeContent.PARKED, demo.content)
        assertEquals(GuidanceUiState.Idle("시연 병원"), demo.guidanceUi)

        demo.guidance.onStartGuidance()
        runCurrent()
        assertEquals(HomeContent.PARKED, demo.content)
        assertEquals(GuidanceUiState.Guiding("시연 병원", null), demo.guidanceUi)

        advance(1.seconds)
        assertEquals(HomeContent.PARKED, demo.content)
        assertEquals(GuidanceUiState.Guiding("시연 병원", 3), demo.guidanceUi)

        advance(2.seconds) // 3초: 출발
        assertEquals(HomeContent.DRIVING, demo.content)

        advance(28.seconds) // 31초: 도착. 아직 주행 중이라 진료 카드(도착 화면)를 보여주지 않는다
        assertEquals(GuidanceUiState.Arrived("시연 병원"), demo.guidanceUi)
        assertEquals(HomeContent.DRIVING, demo.content)

        advance(5.seconds) // 36초: 주차
        assertEquals(HomeContent.ARRIVAL, demo.content)

        demo.guidance.onDismissArrival()
        runCurrent()
        assertEquals(HomeContent.PARKED, demo.content)
        assertEquals(GuidanceUiState.Idle("시연 병원"), demo.guidanceUi)

        advance(1.minutes)
        assertEquals(HomeContent.PARKED, demo.content)
        assertEquals(GuidanceUiState.Idle("시연 병원"), demo.guidanceUi)
    }

    @Test
    fun `출발 전에 안내를 취소하면 정차 화면 그대로이고 주행으로 바뀌지 않는다`() = runTest {
        val demo = startDemo()

        demo.guidance.onStartGuidance()
        advance(2.seconds)
        demo.guidance.onCancelGuidance()
        runCurrent()
        assertEquals(HomeContent.PARKED, demo.content)
        assertEquals(GuidanceUiState.Idle("시연 병원"), demo.guidanceUi)

        advance(1.minutes)
        assertEquals(HomeContent.PARKED, demo.content)
        assertEquals(GuidanceUiState.Idle("시연 병원"), demo.guidanceUi)
    }

    @Test
    fun `주행 중에 정차로 바꾸면 정차 화면에서 남은 시간이 보이고 취소하면 처음 정차 화면이며 남은 시간이 더 줄지 않는다`() = runTest {
        val demo = startDemo()
        demo.guidance.onStartGuidance()
        advance(3.seconds)
        assertEquals(HomeContent.DRIVING, demo.content)

        demo.driving.set(DrivingState.PARKED) // debug 패널 [정차]
        runCurrent()
        assertEquals(HomeContent.PARKED, demo.content)
        assertEquals(GuidanceUiState.Guiding("시연 병원", 3), demo.guidanceUi)

        advance(10.seconds) // 13초: 남은 시간이 줄어도 정차 화면 그대로다
        assertEquals(HomeContent.PARKED, demo.content)
        assertEquals(GuidanceUiState.Guiding("시연 병원", 2), demo.guidanceUi)

        demo.guidance.onCancelGuidance()
        runCurrent()
        assertEquals(HomeContent.PARKED, demo.content)
        assertEquals(GuidanceUiState.Idle("시연 병원"), demo.guidanceUi)

        advance(1.minutes)
        assertEquals(HomeContent.PARKED, demo.content)
        assertEquals(GuidanceUiState.Idle("시연 병원"), demo.guidanceUi)
    }
}
