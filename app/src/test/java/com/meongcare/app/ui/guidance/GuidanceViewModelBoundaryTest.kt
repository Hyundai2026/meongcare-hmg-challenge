package com.meongcare.app.ui.guidance

import app.cash.turbine.test
import com.meongcare.app.domain.guidance.Destination
import com.meongcare.app.domain.guidance.GuidanceSource
import com.meongcare.app.domain.guidance.GuidanceState
import com.meongcare.app.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/** 계획 005 결정 4의 남은 시간 표시 규칙(올림, 최소 1분, null)과 화면 상태 Flow 계약. */
@OptIn(ExperimentalCoroutinesApi::class)
class GuidanceViewModelBoundaryTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val sample = Destination(name = "샘플 병원", latitude = 37.0, longitude = 127.0)
    private val other = Destination(name = "다른 병원", latitude = 37.1, longitude = 127.1)
    private val source = StateOnlyGuidanceSource()

    // Main 디스패처를 바꾼 뒤(규칙이 시작된 뒤)에 만들어야 해서 lazy로 둔다.
    private val viewModel by lazy { GuidanceViewModel(source, sample) }

    /** 화면이 보고 있는 것처럼 계속 구독한다. */
    private fun TestScope.keepSubscribed() {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        runCurrent()
    }

    private fun TestScope.shownFor(state: GuidanceState): GuidanceUiState {
        source.state.value = state
        runCurrent()
        return viewModel.uiState.value
    }

    private fun TestScope.minutesShownFor(remaining: Duration): Int? =
        (shownFor(GuidanceState.Guiding(sample, remaining)) as GuidanceUiState.Guiding).remainingMinutes

    @Test
    fun `남은 시간이 정확히 N분이면 올리지 않고 N분으로 보여준다`() = runTest {
        keepSubscribed()

        assertEquals(1, minutesShownFor(60.seconds))
        assertEquals(2, minutesShownFor(120.seconds))
        assertEquals(12, minutesShownFor(12.minutes))
    }

    @Test
    fun `남은 시간이 N분을 조금이라도 넘으면 N+1분으로 올림한다`() = runTest {
        keepSubscribed()

        assertEquals(2, minutesShownFor(60_001.milliseconds))
        assertEquals(2, minutesShownFor(61.seconds))
        assertEquals(13, minutesShownFor(12.minutes + 1.seconds))
    }

    @Test
    fun `남은 시간이 0이거나 음수면 1분으로 보여준다`() = runTest {
        keepSubscribed()

        assertEquals(1, minutesShownFor(Duration.ZERO))
        assertEquals(1, minutesShownFor((-30).seconds))
        assertEquals(1, minutesShownFor((-5).minutes))
    }

    @Test
    fun `안내 중이거나 도착한 목적지 이름은 안내 중인 목적지의 이름이다`() = runTest {
        keepSubscribed()

        assertEquals(GuidanceUiState.Guiding("다른 병원", 5), shownFor(GuidanceState.Guiding(other, 5.minutes)))
        assertEquals(GuidanceUiState.Arrived("다른 병원"), shownFor(GuidanceState.Arrived(other)))
        assertEquals(GuidanceUiState.Idle("샘플 병원"), shownFor(GuidanceState.Idle))
    }

    @Test
    fun `분이 같은 남은 시간이 연달아 오면 화면 상태를 다시 내보내지 않는다`() = runTest {
        viewModel.uiState.test {
            assertEquals(GuidanceUiState.Idle("샘플 병원"), awaitItem())

            source.state.value = GuidanceState.Guiding(sample, 90.seconds)
            assertEquals(GuidanceUiState.Guiding("샘플 병원", 2), awaitItem())

            source.state.value = GuidanceState.Guiding(sample, 100.seconds)
            source.state.value = GuidanceState.Guiding(sample, 119.seconds)
            expectNoEvents()

            source.state.value = GuidanceState.Guiding(sample, 121.seconds)
            assertEquals(GuidanceUiState.Guiding("샘플 병원", 3), awaitItem())
        }
    }

    @Test
    fun `안내 시작부터 도착과 닫기까지 화면 상태가 차례로 바뀐다`() = runTest {
        viewModel.uiState.test {
            assertEquals(GuidanceUiState.Idle("샘플 병원"), awaitItem())

            source.state.value = GuidanceState.Guiding(sample, null)
            assertEquals(GuidanceUiState.Guiding("샘플 병원", null), awaitItem())

            source.state.value = GuidanceState.Guiding(sample, 12.minutes)
            assertEquals(GuidanceUiState.Guiding("샘플 병원", 12), awaitItem())

            source.state.value = GuidanceState.Guiding(sample, null) // 경로 재탐색 등으로 다시 모름
            assertEquals(GuidanceUiState.Guiding("샘플 병원", null), awaitItem())

            source.state.value = GuidanceState.Arrived(sample)
            assertEquals(GuidanceUiState.Arrived("샘플 병원"), awaitItem())

            source.state.value = GuidanceState.Idle
            assertEquals(GuidanceUiState.Idle("샘플 병원"), awaitItem())
        }
    }

    @Test
    fun `화면이 구독을 멈추고 5초가 지나야 길안내 구독을 끊는다`() = runTest {
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        runCurrent()
        assertEquals(1, source.state.subscriptionCount.value)

        collector.cancel()
        advanceTimeBy(4_999)
        runCurrent()
        assertEquals("5초 안에는 구독을 유지한다 (회전 등)", 1, source.state.subscriptionCount.value)

        advanceTimeBy(1)
        runCurrent()
        assertEquals(0, source.state.subscriptionCount.value)
    }
}

/** 상태는 테스트가 직접 바꾸고, 요청은 무시하는 길안내 소스. */
private class StateOnlyGuidanceSource : GuidanceSource {
    override val state = MutableStateFlow<GuidanceState>(GuidanceState.Idle)
    override suspend fun start(destination: Destination) = Unit
    override suspend fun cancel() = Unit
    override fun dismissArrival() = Unit
}
