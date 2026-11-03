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
 * 상태를 아직 모르면 안전을 위해 주행 화면을 보여준다. 다음 경우가 모두 해당한다:
 * - [DrivingState.UNKNOWN]을 받았을 때
 * - 처음 켜져서 아직 상태를 받지 못했을 때
 * - 화면이 5초 넘게 없다가 돌아와서, 새 상태를 받기 전일 때 (그사이 주행이 시작됐을 수 있다)
 */
class HomeViewModel(drivingStateSource: DrivingStateSource) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = drivingStateSource.drivingState
        .map { it.toUiState() }
        .stateIn(
            scope = viewModelScope,
            // 구독이 끊기면 마지막 상태를 버리고 초기값(주행 화면)으로 돌아간다.
            // 오래된 "정차" 상태를 다시 보여주지 않기 위해서다.
            started = SharingStarted.WhileSubscribed(
                stopTimeoutMillis = STOP_TIMEOUT_MILLIS,
                replayExpirationMillis = 0,
            ),
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
