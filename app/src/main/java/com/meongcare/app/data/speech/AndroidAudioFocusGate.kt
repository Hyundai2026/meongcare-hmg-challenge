package com.meongcare.app.data.speech

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.util.Log

/**
 * 안드로이드 표준 `AudioManager`로 길안내 음성의 오디오 포커스를 잡는다 (Pleos API가 아니다).
 *
 * - 요청: `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK` + `USAGE_ASSISTANCE_NAVIGATION_GUIDANCE`.
 *   오디오 포커스 가이드: NAVIGATION은 MUSIC과 "C(뮤직 duck + 내비 재생)"로 공존하는 것이 기본이고,
 *   "C를 기대하는 컨텍스트라면 요청은 원칙적으로 `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK`을 사용해야 합니다." (docs/pleos/ux-guidelines.md)
 * - usage는 `PleosTextToSpeechClient`가 재생에 쓰는 값과 같아야 한다.
 * - 늦게 받는 포커스(delayed gain)는 받지 않는다. 거절되면 그 안내는 읽지 않는다 (통화 중 등).
 * - 포커스를 잃었을 때(다른 소리가 우선) 따로 멈추지 않는다. 짧은 안내라서 끝나면 반납한다.
 */
class AndroidAudioFocusGate(context: Context) : AudioFocusGate {

    private val audioManager = context.applicationContext.getSystemService(AudioManager::class.java)

    override fun request(): AutoCloseable? {
        val manager = audioManager ?: return null
        val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(GUIDANCE_AUDIO)
            .setOnAudioFocusChangeListener { change -> Log.i(TAG, "포커스 바뀜: $change") }
            .build()
        val result = manager.requestAudioFocus(focusRequest)
        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Log.w(TAG, "포커스 거절: $result")
            return null
        }
        Log.i(TAG, "포커스 받음")
        return AutoCloseable {
            manager.abandonAudioFocusRequest(focusRequest)
            Log.i(TAG, "포커스 반납")
        }
    }

    private companion object {
        const val TAG = "SpeechFocus"

        val GUIDANCE_AUDIO: AudioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
    }
}
