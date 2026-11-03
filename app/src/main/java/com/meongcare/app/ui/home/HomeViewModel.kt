package com.meongcare.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.meongcare.app.domain.driving.DrivingState
import com.meongcare.app.domain.driving.DrivingStateSource
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * 주행 상태를 홈 화면 상태로 바꾼다.
 *
 * 상태를 아직 모르면(처음 켜질 때, [DrivingState.UNKNOWN]) 안전을 위해 주행 화면을 보여준다.
 */
class HomeViewModel(drivingStateSource: DrivingStateSource) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = drivingStateSource.drivingState
        .map { it.toUiState() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = HomeUiState.Driving,
        )

    companion object {
        /** 화면이 잠깐 가려졌다 돌아올 때(회전 등) 다시 구독하지 않도록 기다리는 시간 */
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        fun factory(drivingStateSource: DrivingStateSource): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { HomeViewModel(drivingStateSource) }
            }
    }
}

private fun DrivingState.toUiState(): HomeUiState = when (this) {
    DrivingState.PARKED -> HomeUiState.Parked
    DrivingState.DRIVING, DrivingState.UNKNOWN -> HomeUiState.Driving
}
