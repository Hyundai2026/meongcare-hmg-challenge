package com.meongcare.app.ui.home

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import app.cash.turbine.test
import com.meongcare.app.data.driving.FakeDrivingStateSource
import com.meongcare.app.data.driving.RealDrivingStateSource
import com.meongcare.app.domain.driving.DrivingState
import com.meongcare.app.domain.driving.DrivingStateSource
import com.meongcare.app.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `정차 중이면 정차 화면 상태다`() = runTest {
        val viewModel = HomeViewModel(FakeDrivingStateSource(DrivingState.PARKED))

        viewModel.uiState.test {
            assertEquals(HomeUiState.Parked, expectMostRecentItem())
        }
    }

    @Test
    fun `주행으로 바뀌면 주행 화면 상태로 바뀐다`() = runTest {
        val source = FakeDrivingStateSource(DrivingState.PARKED)
        val viewModel = HomeViewModel(source)

        viewModel.uiState.test {
            assertEquals(HomeUiState.Parked, expectMostRecentItem())
            source.set(DrivingState.DRIVING)
            assertEquals(HomeUiState.Driving, awaitItem())
        }
    }

    @Test
    fun `주행 상태를 모르면 주행 화면 상태로 보여준다`() = runTest {
        val viewModel = HomeViewModel(FakeDrivingStateSource(DrivingState.UNKNOWN))

        viewModel.uiState.test {
            assertEquals(HomeUiState.Driving, expectMostRecentItem())
        }
    }

    // ---- 첫 값 ----

    @Test
    fun `주행 상태가 처음 오기 전에는 주행 화면 상태다`() = runTest {
        val source = ManualDrivingStateSource()
        val viewModel = HomeViewModel(source)

        viewModel.uiState.test {
            assertEquals(HomeUiState.Driving, awaitItem())

            source.states.subscriptionCount.first { it > 0 }
            source.states.emit(DrivingState.PARKED)
            assertEquals(HomeUiState.Parked, awaitItem())
        }
    }

    // ---- 주행 ↔ 정차 왕복 ----

    @Test
    fun `주행에서 정차로 갔다가 다시 주행으로 가면 화면 상태도 주행 정차 주행 순서로 바뀐다`() = runTest {
        val source = FakeDrivingStateSource(DrivingState.DRIVING)
        val viewModel = HomeViewModel(source)

        viewModel.uiState.test {
            assertEquals(HomeUiState.Driving, expectMostRecentItem())
            source.set(DrivingState.PARKED)
            assertEquals(HomeUiState.Parked, awaitItem())
            source.set(DrivingState.DRIVING)
            assertEquals(HomeUiState.Driving, awaitItem())
            expectNoEvents()
        }
    }

    // ---- 도중에 UNKNOWN ----

    @Test
    fun `정차 중에 주행 상태를 모르게 되면 주행 화면 상태로 바뀐다`() = runTest {
        val source = FakeDrivingStateSource(DrivingState.PARKED)
        val viewModel = HomeViewModel(source)

        viewModel.uiState.test {
            assertEquals(HomeUiState.Parked, expectMostRecentItem())
            source.set(DrivingState.UNKNOWN)
            assertEquals(HomeUiState.Driving, awaitItem())
        }
    }

    @Test
    fun `주행 상태를 모르다가 정차로 확인되면 정차 화면 상태로 바뀐다`() = runTest {
        val source = FakeDrivingStateSource(DrivingState.UNKNOWN)
        val viewModel = HomeViewModel(source)

        viewModel.uiState.test {
            assertEquals(HomeUiState.Driving, expectMostRecentItem())
            source.set(DrivingState.PARKED)
            assertEquals(HomeUiState.Parked, awaitItem())
        }
    }

    @Test
    fun `주행 중에 주행 상태를 모르게 되어도 주행 화면 상태를 그대로 유지한다`() = runTest {
        val source = FakeDrivingStateSource(DrivingState.DRIVING)
        val viewModel = HomeViewModel(source)

        viewModel.uiState.test {
            assertEquals(HomeUiState.Driving, expectMostRecentItem())
            source.set(DrivingState.UNKNOWN)
            expectNoEvents()
            assertEquals(HomeUiState.Driving, viewModel.uiState.value)
        }
    }

    // ---- Real 자리표시 ----

    @Test
    fun `Real 주행 상태 소스로는 시간이 지나도 주행 화면 상태만 보여준다`() = runTest {
        val viewModel = HomeViewModel(RealDrivingStateSource())

        viewModel.uiState.test {
            assertEquals(HomeUiState.Driving, expectMostRecentItem())
            advanceTimeBy(60_000)
            runCurrent()
            expectNoEvents()
            assertEquals(HomeUiState.Driving, viewModel.uiState.value)
        }
    }

    // ---- 구독 취소 ----

    @Test
    fun `화면이 구독하기 전에는 주행 상태를 구독하지 않는다`() = runTest {
        val source = CountingDrivingStateSource(FakeDrivingStateSource())
        HomeViewModel(source)

        runCurrent()

        assertEquals(0, source.activeCollectors)
    }

    @Test
    fun `화면이 구독을 멈추고 5초가 지나면 주행 상태 구독을 해제한다`() = runTest {
        val source = CountingDrivingStateSource(FakeDrivingStateSource())
        val viewModel = HomeViewModel(source)

        val screen = backgroundScope.launch { viewModel.uiState.collect {} }
        runCurrent()
        assertEquals(1, source.activeCollectors)

        screen.cancel()
        runCurrent()
        advanceTimeBy(4_999)
        runCurrent()
        assertEquals("5초가 지나기 전에는 구독을 유지한다", 1, source.activeCollectors)

        advanceTimeBy(1)
        runCurrent()
        assertEquals("5초가 지나면 구독을 해제한다", 0, source.activeCollectors)
    }

    @Test
    fun `5초 안에 화면이 다시 구독하면 주행 상태를 새로 구독하지 않는다`() = runTest {
        val source = CountingDrivingStateSource(FakeDrivingStateSource())
        val viewModel = HomeViewModel(source)

        val screen = backgroundScope.launch { viewModel.uiState.collect {} }
        runCurrent()
        screen.cancel()
        runCurrent()
        advanceTimeBy(3_000)
        runCurrent()

        backgroundScope.launch { viewModel.uiState.collect {} }
        runCurrent()
        advanceTimeBy(10_000)
        runCurrent()

        assertEquals("구독을 새로 시작하지 않는다", 1, source.startCount)
        assertEquals("화면이 구독 중이면 구독을 유지한다", 1, source.activeCollectors)
    }

    @Test
    fun `구독이 해제된 사이에 주행으로 바뀌면 다시 구독할 때 주행 화면 상태가 된다`() = runTest {
        val fake = FakeDrivingStateSource(DrivingState.PARKED)
        val source = CountingDrivingStateSource(fake)
        val viewModel = HomeViewModel(source)

        val screen = backgroundScope.launch { viewModel.uiState.collect {} }
        runCurrent()
        assertEquals(HomeUiState.Parked, viewModel.uiState.value)
        screen.cancel()
        runCurrent()
        advanceTimeBy(5_001)
        runCurrent()
        assertEquals(0, source.activeCollectors)

        fake.set(DrivingState.DRIVING)

        viewModel.uiState.test {
            // 다시 구독한 순간에는 마지막으로 알던 정차 상태가 먼저 올 수 있다. 결국 주행이 되어야 한다.
            var state = awaitItem()
            if (state == HomeUiState.Parked) state = awaitItem()
            assertEquals(HomeUiState.Driving, state)
        }
    }

    @Test
    fun `ViewModel이 정리되면 5초를 기다리지 않고 주행 상태 구독을 해제한다`() = runTest {
        val source = CountingDrivingStateSource(FakeDrivingStateSource())
        val store = ViewModelStore()
        val viewModel = ViewModelProvider(store, HomeViewModel.factory(source))[HomeViewModel::class.java]

        backgroundScope.launch { viewModel.uiState.collect {} }
        runCurrent()
        assertEquals(1, source.activeCollectors)

        store.clear()
        runCurrent()

        assertEquals(0, source.activeCollectors)
    }
}

/** 값을 테스트가 직접 흘려보내는 주행 상태 소스. 구독해도 아무 값도 먼저 내보내지 않는다. */
private class ManualDrivingStateSource : DrivingStateSource {
    val states = MutableSharedFlow<DrivingState>()
    override val drivingState: Flow<DrivingState> = states
}

/** 감싼 소스를 지금 몇 곳이 구독 중인지, 구독이 몇 번 시작됐는지 센다. */
private class CountingDrivingStateSource(delegate: DrivingStateSource) : DrivingStateSource {
    var activeCollectors = 0
        private set
    var startCount = 0
        private set

    override val drivingState: Flow<DrivingState> = delegate.drivingState
        .onStart {
            activeCollectors++
            startCount++
        }
        .onCompletion { activeCollectors-- }
}
