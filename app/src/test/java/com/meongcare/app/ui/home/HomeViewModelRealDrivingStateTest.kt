package com.meongcare.app.ui.home

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import app.cash.turbine.test
import com.meongcare.app.data.driving.RealDrivingStateSource
import com.meongcare.app.data.driving.UxRestrictionsClient
import com.meongcare.app.data.driving.UxRestrictionsEvent
import com.meongcare.app.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * 계획 006: Real 주행 상태(가짜 규제 창구)를 홈 화면에 끼웠을 때.
 *
 * - 규제 값이 바뀌면 화면이 주행 ↔ 정차로 바뀐다 (AGENTS.md 규칙 3, 설계 결정 2·7).
 * - 차량 연결은 "화면이 보이는 동안 + 5초"만 산다 (설계 결정 3).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelRealDrivingStateTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `규제 값이 정차와 주행으로 바뀌면 화면도 정차와 주행으로 바뀐다`() = runTest {
        val client = HomeFakeUxClient()
        val viewModel = HomeViewModel(RealDrivingStateSource(client))

        viewModel.uiState.test {
            assertEquals(HomeUiState.Driving, awaitItem())
            runCurrent()

            client.send(UxRestrictionsEvent.Changed(requiresDistractionOptimization = false))
            assertEquals(HomeUiState.Parked, awaitItem())
            client.send(UxRestrictionsEvent.Changed(requiresDistractionOptimization = true))
            assertEquals(HomeUiState.Driving, awaitItem())
            client.send(UxRestrictionsEvent.Changed(requiresDistractionOptimization = false))
            assertEquals(HomeUiState.Parked, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `정차 화면에서 규제 값을 알 수 없게 되면 주행 화면으로 바뀐다`() = runTest {
        val client = HomeFakeUxClient()
        val viewModel = HomeViewModel(RealDrivingStateSource(client))

        viewModel.uiState.test {
            assertEquals(HomeUiState.Driving, awaitItem())
            runCurrent()

            client.send(UxRestrictionsEvent.Changed(requiresDistractionOptimization = false))
            assertEquals(HomeUiState.Parked, awaitItem())
            client.send(UxRestrictionsEvent.Unavailable)
            assertEquals(HomeUiState.Driving, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `화면이 떠나도 5초가 지나기 전에는 차량 연결을 닫지 않고 5초가 지나면 닫는다`() = runTest {
        val client = HomeFakeUxClient()
        val viewModel = HomeViewModel(RealDrivingStateSource(client))

        val screen = backgroundScope.launch { viewModel.uiState.collect {} }
        runCurrent()
        assertEquals(1, client.connectCount)

        screen.cancel()
        runCurrent()
        advanceTimeBy(4_999)
        runCurrent()
        assertEquals("5초가 지나기 전에는 닫지 않는다", 0, client.closeCount)

        advanceTimeBy(1)
        runCurrent()
        assertEquals("5초가 지나면 닫는다", 1, client.closeCount)
    }

    @Test
    fun `화면이 5초 넘게 없다가 돌아오면 다시 연결하고 새 규제 값을 받기 전까지 주행 화면이다`() = runTest {
        val client = HomeFakeUxClient()
        val viewModel = HomeViewModel(RealDrivingStateSource(client))

        val screen = backgroundScope.launch { viewModel.uiState.collect {} }
        runCurrent()
        client.send(UxRestrictionsEvent.Changed(requiresDistractionOptimization = false))
        runCurrent()
        assertEquals(HomeUiState.Parked, viewModel.uiState.value)

        screen.cancel()
        runCurrent()
        advanceTimeBy(5_001)
        runCurrent()
        assertEquals(1, client.closeCount)

        backgroundScope.launch { viewModel.uiState.collect {} }
        runCurrent()

        assertEquals("다시 연결한다", 2, client.connectCount)
        assertEquals("새 규제 값 전에는 예전 정차 대신 주행 화면", HomeUiState.Driving, viewModel.uiState.value)

        client.send(UxRestrictionsEvent.Changed(requiresDistractionOptimization = false))
        runCurrent()
        assertEquals(HomeUiState.Parked, viewModel.uiState.value)
    }

    @Test
    fun `ViewModel이 정리되면 5초를 기다리지 않고 차량 연결을 닫는다`() = runTest {
        val client = HomeFakeUxClient()
        val store = ViewModelStore()
        val viewModel = ViewModelProvider(
            store,
            HomeViewModel.factory(RealDrivingStateSource(client)),
        )[HomeViewModel::class.java]

        backgroundScope.launch { viewModel.uiState.collect {} }
        runCurrent()
        assertEquals(1, client.connectCount)

        store.clear()
        runCurrent()

        assertEquals(1, client.closeCount)
    }
}

/** 연결과 닫기 횟수를 세고, 테스트가 규제 값을 보내는 가짜 창구. */
private class HomeFakeUxClient : UxRestrictionsClient {
    var connectCount = 0
        private set
    var closeCount = 0
        private set
    private var listener: ((UxRestrictionsEvent) -> Unit)? = null

    override fun connect(onEvent: (UxRestrictionsEvent) -> Unit): AutoCloseable {
        connectCount++
        listener = onEvent
        return AutoCloseable {
            closeCount++
            if (listener === onEvent) listener = null
        }
    }

    fun send(event: UxRestrictionsEvent) {
        val l = checkNotNull(listener) { "연결되지 않았다" }
        l(event)
    }
}
