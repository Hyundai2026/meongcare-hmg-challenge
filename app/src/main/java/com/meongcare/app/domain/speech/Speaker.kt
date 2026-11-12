package com.meongcare.app.domain.speech

/**
 * 짧은 문장을 소리로 읽는다 (docs/plans 017 "설계 결정 3").
 *
 * 구현: Pleos TextToSpeech `ON_DEVICE`(Real), 소리 없이 시간만 보내는 Fake. 부르는 쪽은 어느 쪽인지 모른다.
 * 018(길안내 중 증상 추가)이 같은 부품을 쓴다: [SpeakResult.Done]을 받은 뒤 듣기(STT)를 시작한다.
 */
interface Speaker {
    /**
     * [text]를 읽고, 끝나면 결과를 돌려준다.
     * - 한 번에 하나만 읽는다. 앞 문장을 읽는 중에 부르면 앞 문장이 끝난 뒤 읽는다 (부른 순서대로).
     *   Pleos 음성 서비스는 읽는 중에 새 문장을 받으면 앞 문장을 끊기 때문에 구현이 이 순서를 지킨다 (017 spike S6).
     * - 부른 코루틴을 취소하면 읽기를 멈춘다 (기다리는 중이었으면 읽지 않는다). 이때는 결과 대신 취소 예외다.
     */
    suspend fun speak(text: String): SpeakResult
}

/** 말하기 한 번의 결과. */
sealed interface SpeakResult {
    /** 끝까지 읽었다 (SDK `onDone`). */
    data object Done : SpeakResult

    /** 다 읽기 전에 멈췄다 (SDK `onStop`). 부른 쪽이 취소한 경우는 여기로 오지 않는다. */
    data object Stopped : SpeakResult

    data class Failed(val reason: SpeakFailure) : SpeakResult
}

enum class SpeakFailure {
    /** 소리 출력 문제 (`AUDIO_ERROR`, 오디오 포커스 거절) */
    AUDIO,

    /** 이 기기·지금 상태에서 쓸 수 없다 (초기화 실패, 엔진·언어·권한 문제, 시간 초과 등) */
    UNAVAILABLE,
}
