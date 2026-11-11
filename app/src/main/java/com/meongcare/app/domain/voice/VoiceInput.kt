package com.meongcare.app.domain.voice

import kotlinx.coroutines.flow.Flow

/**
 * 음성 입력. 한 번 들을 때 한 문장을 받아쓴다 (docs/plans 010 "설계 결정 3").
 *
 * 구현: Pleos SpeechToText `ON_DEVICE`(Real), 예시 문장(Fake). 화면은 어느 쪽인지 모른다.
 */
interface VoiceInput {
    /**
     * 한 문장을 듣는다. 구독하면 마이크를 켜고, [VoiceEvent.Final] 또는 [VoiceEvent.Failed]를 낸 뒤 끝난다.
     * 구독을 취소하면 듣기를 멈추고 들은 것을 버린다. 한 번에 하나만 구독한다.
     */
    fun listen(): Flow<VoiceEvent>
}

/** 듣기 한 번 동안 오는 일. 순서: [Ready] → [Partial]* → [Final] 또는 언제든 [Failed]. */
sealed interface VoiceEvent {
    /** 마이크가 켜졌다. */
    data object Ready : VoiceEvent

    /** 받아쓰는 중인 글자. 앞 값보다 길어지거나 바뀔 수 있다. 증상을 찾는 데 쓰지 않는다. */
    data class Partial(val text: String) : VoiceEvent

    /** 다 들었다. 빈 문장은 오지 않는다 (빈 문장이면 [Failed] [VoiceFailure.NO_SPEECH]). */
    data class Final(val text: String) : VoiceEvent

    data class Failed(val reason: VoiceFailure) : VoiceEvent
}

enum class VoiceFailure {
    /** 말소리를 듣지 못했다 (시간 초과, 최종 결과 없음, 빈 문장) */
    NO_SPEECH,

    /** 마이크·오디오 문제 */
    MIC,

    /** 이 기기·지금 상태에서 음성 인식을 쓸 수 없다 (초기화 실패, 엔진·언어·권한 문제 등) */
    UNAVAILABLE,
}
