package com.meongcare.app.ui.guidance

import app.cash.turbine.test
import com.meongcare.app.domain.guidance.Destination
import com.meongcare.app.domain.guidance.GuidanceSource
import com.meongcare.app.domain.guidance.GuidanceState
import com.meongcare.app.testutil.MainDispatcherRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class GuidanceViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val sample = Destination(name = "샘플 병원", latitude = 37.0, longitude = 127.0)
    private val source = RecordingGuidanceSource()
    // Main 디스패처를 바꾼 뒤(규칙이 시작된 뒤)에 만들어야 해서 lazy로 둔다.
    private val viewModel by lazy { GuidanceViewModel(source, sample) }

    @Test
    fun `안내 중이 아니면 샘플 병원 이름을 안내 시작 대상으로 보여준다`() = runTest {
        viewModel.uiState.test {
            assertEquals(GuidanceUiState.Idle("샘플 병원"), expectMostRecentItem())
        }
    }

    @Test
    fun `남은 시간을 모르면 남은 분 없이 안내 중으로 보여준다`() = runTest {
        source.state.value = GuidanceState.Guiding(sample, remainingTime = null)

        viewModel.uiState.test {
            assertEquals(GuidanceUiState.Guiding("샘플 병원", remainingMinutes = null), expectMostRecentItem())
        }
    }

    @Test
    fun `남은 시간을 분 단위로 올림해서 보여준다`() = runTest {
        viewModel.uiState.test {
            source.state.value = GuidanceState.Guiding(sample, 90.seconds)
            assertEquals(GuidanceUiState.Guiding("샘플 병원", 2), expectMostRecentItem())

            source.state.value = GuidanceState.Guiding(sample, 12.minutes)
            assertEquals(GuidanceUiState.Guiding("샘플 병원", 12), awaitItem())
        }
    }

    @Test
    fun `남은 시간이 1분보다 짧으면 1분으로 보여준다`() = runTest {
        viewModel.uiState.test {
            source.state.value = GuidanceState.Guiding(sample, 500.milliseconds)
            assertEquals(GuidanceUiState.Guiding("샘플 병원", 1), expectMostRecentItem())
        }
    }

    @Test
    fun `도착하면 도착 상태로 보여준다`() = runTest {
        source.state.value = GuidanceState.Arrived(sample)

        viewModel.uiState.test {
            assertEquals(GuidanceUiState.Arrived("샘플 병원"), expectMostRecentItem())
        }
    }

    @Test
    fun `안내 시작을 누르면 샘플 병원으로 안내를 요청한다`() = runTest {
        viewModel.onStartGuidance()

        assertEquals(listOf("start:샘플 병원"), source.requests)
    }

    @Test
    fun `안내 취소를 누르면 안내 취소를 요청한다`() = runTest {
        viewModel.onCancelGuidance()

        assertEquals(listOf("cancel"), source.requests)
    }

    @Test
    fun `도착 화면을 닫으면 도착 확인을 요청한다`() = runTest {
        viewModel.onDismissArrival()

        assertEquals(listOf("dismissArrival"), source.requests)
    }
}

/** 상태는 테스트가 직접 바꾸고, 요청은 기록만 하는 길안내 소스. */
private class RecordingGuidanceSource : GuidanceSource {
    override val state = MutableStateFlow<GuidanceState>(GuidanceState.Idle)
    val requests = mutableListOf<String>()

    override suspend fun start(destination: Destination) {
        requests += "start:${destination.name}"
    }

    override suspend fun cancel() {
        requests += "cancel"
    }

    override fun dismissArrival() {
        requests += "dismissArrival"
    }
}
