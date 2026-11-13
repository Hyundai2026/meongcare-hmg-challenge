package com.meongcare.app.ui.relax

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** 실내 사운드 */
enum class RelaxSound { QUIET, CALM_MUSIC }

/**
 * 릴렉스 설정 (019 D5). **시연용**: 차량 공조·음악에 적용하지 않는다. 앱 메모리에만 있고, 앱을 다시 켜면 기본값이다.
 * @param enabled 큰 토글. 꺼도 아래 설정 값은 그대로 남는다
 */
data class RelaxSettings(
    val enabled: Boolean = false,
    val sound: RelaxSound = RelaxSound.QUIET,
    val extraReassurance: Boolean = false,
) {
    /** 한 번도 바꾸지 않은 기본값인지 (반려동물 카드에 "기본 설정"으로 보인다) */
    val isDefault: Boolean get() = this == RelaxSettings()
}

/** 릴렉스 화면 상태. 액티비티 범위라 반려동물 카드의 "환경 설정" 칸도 같은 값을 본다. */
class RelaxViewModel : ViewModel() {
    private val state = MutableStateFlow(RelaxSettings())
    val settings: StateFlow<RelaxSettings> = state.asStateFlow()

    fun setEnabled(on: Boolean) = state.update { it.copy(enabled = on) }

    /** 꺼져 있으면 무시한다 (화면에서도 흐리게 막는다) */
    fun setSound(sound: RelaxSound) = state.update { if (it.enabled) it.copy(sound = sound) else it }

    /** 꺼져 있으면 무시한다 */
    fun setExtraReassurance(on: Boolean) = state.update { if (it.enabled) it.copy(extraReassurance = on) else it }
}
