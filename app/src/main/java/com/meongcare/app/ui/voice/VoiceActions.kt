package com.meongcare.app.ui.voice

/** 음성 영역의 동작 묶음. `MainActivity`가 [SymptomVoiceViewModel]의 함수로 채운다. */
data class VoiceActions(
    /** [말로 증상 고르기] / [다시 말하기] */
    val onStart: () -> Unit = {},
    /** [듣기 취소] */
    val onCancel: () -> Unit = {},
    /** 증상 고르기 화면이 사라졌거나 앱이 뒤로 갔다 */
    val onStop: () -> Unit = {},
)
