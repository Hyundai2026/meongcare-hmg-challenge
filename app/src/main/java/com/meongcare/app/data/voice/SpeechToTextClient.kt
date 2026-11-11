package com.meongcare.app.data.voice

/**
 * Pleos SpeechToText `ResultListener`·`OnInitializeListener` 이벤트를 Pleos 타입 없이 옮긴 것.
 * 판단 로직([RealVoiceInput])이 Pleos를 import하지 않아서 JVM으로 시험할 수 있다.
 */
sealed interface SttEvent {
    /** `onReady` */
    data object Ready : SttEvent

    /** `onStartedRecognition`. 관찰상 부분 결과마다 다시 온다. */
    data object Started : SttEvent

    /** `onUpdated(stt, completed)` */
    data class Updated(val text: String, val completed: Boolean) : SttEvent

    /** `onEndedRecognition`. 관찰상 이 다음에 최종 결과가 온다. */
    data object Ended : SttEvent

    /** `onError(ErrorType)`. [name]은 `ErrorType.name` (docs/pleos/sdk-notes.md의 `ErrorType` 값). */
    data class Error(val name: String) : SttEvent

    /** `OnInitializeListener.onFailure(ErrorType)`. [name]은 `ErrorType.name`. */
    data class InitFailed(val name: String) : SttEvent
}

/** Pleos SpeechToText를 여는 창구. 구현은 `PleosSpeechToTextClient` 하나뿐이다. */
fun interface SpeechToTextClient {
    /**
     * 새 SpeechToText를 만들어 초기화하고, 성공하면 리스너 등록 → 한국어 → `request()`까지 한다.
     * 이벤트는 [onEvent]로 온다 (어느 스레드에서 올지 모른다).
     * 돌려받은 것을 `close()`하면 `stop()` → `removeListener()` → `release()`. 여러 번 불러도 된다.
     */
    fun start(onEvent: (SttEvent) -> Unit): AutoCloseable
}
