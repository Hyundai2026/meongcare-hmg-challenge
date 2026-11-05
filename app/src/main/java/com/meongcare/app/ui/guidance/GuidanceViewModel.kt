package com.meongcare.app.ui.guidance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.meongcare.app.domain.guidance.Destination
import com.meongcare.app.domain.guidance.GuidanceSource
import com.meongcare.app.domain.guidance.GuidanceState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration

/**
 * 길안내 상태를 화면 상태로 바꾸고, 화면의 버튼을 길안내 요청으로 바꾼다.
 *
 * 주행 상태는 다루지 않는다. 주행 상태 쪽 안전 규칙은 `HomeViewModel`이 그대로 맡고,
 * 두 상태를 합쳐 어느 화면을 보여줄지는 `homeContentOf`가 정한다.
 *
 * @param destination 안내를 시작할 목적지. 지금은 시연용 샘플 병원이다.
 */
class GuidanceViewModel(
    private val guidanceSource: GuidanceSource,
    private val destination: Destination,
) : ViewModel() {

    val uiState: StateFlow<GuidanceUiState> = guidanceSource.state
        .map { it.toUiState() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = GuidanceUiState.Idle(destination.name),
        )

    fun onStartGuidance() {
        viewModelScope.launch { guidanceSource.start(destination) }
    }

    fun onCancelGuidance() {
        viewModelScope.launch { guidanceSource.cancel() }
    }

    fun onDismissArrival() {
        guidanceSource.dismissArrival()
    }

    private fun GuidanceState.toUiState(): GuidanceUiState = when (this) {
        GuidanceState.Idle -> GuidanceUiState.Idle(destination.name)
        is GuidanceState.Guiding -> GuidanceUiState.Guiding(
            destinationName = this.destination.name,
            remainingMinutes = remainingTime?.toDisplayMinutes(),
        )
        is GuidanceState.Arrived -> GuidanceUiState.Arrived(this.destination.name)
    }

    companion object {
        /** 화면이 잠깐 가려졌다 돌아올 때(회전 등) 다시 구독하지 않도록 기다리는 시간 */
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        fun factory(guidanceSource: GuidanceSource, destination: Destination): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { GuidanceViewModel(guidanceSource, destination) }
            }
    }
}

/**
 * 화면에 보여줄 남은 분. 분 단위로 올림하고 (90초 → 2분), 1분보다 작으면 1분으로 한다.
 * "0분"이나 음수를 보여주지 않기 위해서다.
 */
internal fun Duration.toDisplayMinutes(): Int {
    val millis = inWholeMilliseconds
    val minutes = (millis + MILLIS_PER_MINUTE - 1).floorDiv(MILLIS_PER_MINUTE)
    return minutes.coerceIn(1L, Int.MAX_VALUE.toLong()).toInt()
}

private const val MILLIS_PER_MINUTE = 60_000L
