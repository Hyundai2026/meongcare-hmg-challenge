package com.meongcare.app.ui.voice

import com.meongcare.app.domain.card.Symptom
import com.meongcare.app.domain.voice.VoiceFailure

/** 증상 고르기 화면의 음성 영역 상태 (docs/plans 010 "설계 결정 1"의 표). */
sealed interface VoiceUiState {
    /** 대기: [말로 증상 고르기] */
    data object Idle : VoiceUiState

    /** 누른 직후, 마이크가 켜지기 전 */
    data object Starting : VoiceUiState

    /** 듣는 중. [partial]은 받아쓰는 중인 글자 (아직 없으면 빈 글자) */
    data class Listening(val partial: String) : VoiceUiState

    /** 다 들었다. [matched]는 문장에서 찾은 증상(프리셋 순서). 비면 "찾지 못했어요" */
    data class Heard(val sentence: String, val matched: List<Symptom>) : VoiceUiState

    data class Failed(val reason: VoiceFailure) : VoiceUiState
}

/**
 * @param available 이 구성에 음성 입력이 있는지. 없으면 음성 영역을 그리지 않는다.
 * @param isSimulated 실제로 듣지 않는 시연용 음성 입력(Fake)인지. 화면에 그 사실을 알린다 (출품 유의사항).
 */
data class VoiceScreenState(
    val available: Boolean,
    val isSimulated: Boolean,
    val voice: VoiceUiState,
)
