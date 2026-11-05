package com.meongcare.app.ui.debug

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.meongcare.app.domain.location.CurrentLocation
import com.meongcare.app.domain.location.CurrentLocationSource
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * debug 패널의 [위치 묻기]. debug 빌드에서만 만든다 (011).
 * 병원 검색이 붙기 전까지 현재 위치가 실제로 오는지 눈으로 보는 용도다. 결과는 메모리에만 있다.
 */
class DebugLocationViewModel(
    private val source: CurrentLocationSource,
) : ViewModel() {

    private val state = MutableStateFlow<DebugLocationUiState>(DebugLocationUiState.NotAsked)
    val uiState: StateFlow<DebugLocationUiState> = state.asStateFlow()

    /** 묻는 중이면 무시한다. 결과가 나온 뒤에는 다시 물을 수 있다. */
    fun onAsk() {
        if (state.value == DebugLocationUiState.Asking) return
        state.value = DebugLocationUiState.Asking
        viewModelScope.launch {
            val location = source.current()
            state.value = if (location != null) DebugLocationUiState.Found(location) else DebugLocationUiState.Unknown
        }
    }

    companion object {
        fun factory(source: CurrentLocationSource): ViewModelProvider.Factory = viewModelFactory {
            initializer { DebugLocationViewModel(source) }
        }
    }
}

sealed interface DebugLocationUiState {
    data object NotAsked : DebugLocationUiState
    data object Asking : DebugLocationUiState
    data class Found(val location: CurrentLocation) : DebugLocationUiState
    data object Unknown : DebugLocationUiState
}

/** debug 패널의 위치 줄에 넘기는 값. */
data class DebugLocationRow(val text: String, val isAsking: Boolean)

/**
 * 위치 줄 글자. 예: "위치(내비): 37.41272, 127.09468 · 수정구 시흥동".
 * 좌표는 소수 다섯째 자리(약 1m)까지, 기기 언어와 상관없이 소수점은 점이다.
 *
 * @param sourceName 위치 출처 ("내비" 또는 "Fake")
 */
fun debugLocationText(state: DebugLocationUiState, sourceName: String): String {
    val body = when (state) {
        DebugLocationUiState.NotAsked -> "아직 묻지 않음"
        DebugLocationUiState.Asking -> "묻는 중…"
        DebugLocationUiState.Unknown -> "모름"
        is DebugLocationUiState.Found -> {
            val (lat, lon, address) = state.location
            String.format(Locale.ROOT, "%.5f, %.5f", lat, lon) + " · " + (address ?: "주소 모름")
        }
    }
    return "위치($sourceName): $body"
}
