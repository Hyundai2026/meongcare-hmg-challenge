package com.meongcare.app.data.speech

/**
 * Pleos TextToSpeech `EventListener`·`OnInitializeListener` 이벤트를 Pleos 타입 없이 옮긴 것.
 * 판단 로직([RealSpeaker])이 Pleos를 import하지 않아서 JVM으로 시험할 수 있다.
 */
sealed interface TtsEvent {
    /** `onReady`. 관찰상 `speak()`마다 온다 (017 spike S3). */
    data object Ready : TtsEvent

    /** `onStart` */
    data object Started : TtsEvent

    /** `onDone`. 서비스가 재생을 끝내고 player를 놓은 뒤에 온다 (spike S5). */
    data object Done : TtsEvent

    /** `onStop` */
    data object Stopped : TtsEvent

    /** `onError(ErrorType)`. [name]은 `ErrorType.name` (docs/pleos/sdk-notes.md의 `ErrorType` 값). */
    data class Error(val name: String) : TtsEvent

    /** `OnInitializeListener.onFailure(ErrorType)`. [name]은 `ErrorType.name`. */
    data class InitFailed(val name: String) : TtsEvent
}

/** Pleos TextToSpeech를 여는 창구. 구현은 `PleosTextToSpeechClient` 하나뿐이다. */
fun interface TextToSpeechClient {
    /**
     * 새 TextToSpeech를 만들어 초기화하고, 성공하면 리스너 등록 → 한국어 → 재생 속성 → `speak(text)`까지 한다.
     * 이벤트는 [onEvent]로 온다 (관찰상 binder 스레드, spike S3).
     * 돌려받은 것을 `close()`하면 (읽는 중이면 `stop()` → 멈춤을 기다림) → `removeEventListener()` → `release()`. 여러 번 불러도 된다.
     */
    fun speak(text: String, onEvent: (TtsEvent) -> Unit): AutoCloseable
}
