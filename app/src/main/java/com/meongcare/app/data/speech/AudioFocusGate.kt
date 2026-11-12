package com.meongcare.app.data.speech

/**
 * 소리 안내를 읽는 동안 오디오 포커스를 잡는다 (docs/plans 017 단계 5-3).
 *
 * Pleos TextToSpeech SDK도 음성 서비스도 포커스를 잡지 않았다 (spike S8: 말하는 동안 포커스 스택이 비어 있음).
 * 오디오 포커스 가이드는 "각 오디오 스트림은 재생 시작 전 `AudioAttributes` + `AudioFocusRequest`로 포커스를 요청해야 합니다."라고 한다.
 * 그래서 앱이 직접 잡는다. 판단 로직([RealSpeaker])이 Android를 모르게 경계를 둔다.
 */
fun interface AudioFocusGate {
    /** 포커스를 요청한다. 받으면 반납할 것을, 거절되면 null을 돌려준다. */
    fun request(): AutoCloseable?

    companion object {
        /** 포커스를 잡지 않는다 (테스트, 포커스가 필요 없는 구성). 언제나 받은 것으로 친다. */
        val NONE = AudioFocusGate { AutoCloseable {} }
    }
}
