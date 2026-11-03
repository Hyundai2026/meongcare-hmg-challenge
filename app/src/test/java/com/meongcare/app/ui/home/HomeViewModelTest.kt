package com.meongcare.app.ui.home

import app.cash.turbine.test
import com.meongcare.app.data.driving.FakeDrivingStateSource
import com.meongcare.app.domain.driving.DrivingState
import com.meongcare.app.testutil.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

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
}
