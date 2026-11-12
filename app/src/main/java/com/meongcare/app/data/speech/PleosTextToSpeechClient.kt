package com.meongcare.app.data.speech

import ai.pleos.playground.tts.TextToSpeech
import ai.pleos.playground.tts.constant.ErrorType
import ai.pleos.playground.tts.constant.Mode
import ai.pleos.playground.tts.listener.EventListener
import ai.pleos.playground.tts.listener.OnInitializeListener
import android.content.Context
import android.media.AudioAttributes
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import java.util.Locale

/**
 * Pleos TextToSpeech SDK(3.3.1)를 `ON_DEVICE` 모드로 부르는 유일한 곳. `ai.pleos.playground.tts`는 이 파일에서만 import한다.
 *
 * - 순서 (docs/pleos/sdk-notes.md "TTS `ON_DEVICE` 실행 확인"에서 동작한 순서 그대로):
 *   `TextToSpeech(context, Mode.ON_DEVICE)` → `initialize(listener)` → 성공하면 `addEventListener` → `setLocale(Locale.KOREA)` →
 *   `setAudioAttributes(길안내 음성)` → `speak(text, true)`.
 *   - `addEventListener`·`setAudioAttributes`는 `initialize` 뒤에 부른다 (내부 객체가 `initialize` 때 생긴다, [aar-tts] 추정).
 *   - 재생 usage는 `USAGE_ASSISTANCE_NAVIGATION_GUIDANCE`다. 부르지 않으면 SDK 기본값이 `USAGE_MEDIA`(음악과 같은 종류)다.
 *     spike S8에서 이 값이 음성 서비스의 AudioTrack에 그대로 쓰이는 것을 봤다 (docs/plans 017 "usage 고르는 규칙").
 * - `registerApp`, `setSpeakerType`, `setAudioZoneId`는 부르지 않는다. `ON_DEVICE`는 앱 등록(Client ID/Secret)이 필요 없다.
 * - 말하기 한 번마다 새로 만들고, [AutoCloseable.close]에서 놓는다. 문서: release 후 다시 쓰려면 새 객체를 만든다.
 *   - 읽는 중이면 `stop()` → `onStop`이 오거나 [STOP_GRACE_MS]가 지나면 `removeEventListener()` → `release()`.
 *     **`stop()` 바로 뒤에 `release()`하지 않는다.** spike S7에서 SDK의 연결 끊기와 `stop` 전달이 경쟁이었다
 *     (010 STT에서는 이 경쟁으로 녹음이 계속됐다).
 *   - 끝난 뒤(`onDone`·`onStop`·`onError`)면 바로 놓는다. `onDone`은 서비스가 재생을 끝낸 뒤에 와서 끝소리가 잘리지 않는다 (spike S5).
 * - SDK 호출은 모두 메인 스레드에서 한다 ([mainHandler]). [speak]는 어느 스레드에서 불러도 된다 (안내자는 `Dispatchers.Default`).
 *   콜백은 binder 스레드로 온다 (spike S3). 상태는 메인 스레드에서만 바꾼다.
 * - 로그에는 이벤트 이름, 오류 이름, 초기화 시간, 글자 수만 남긴다. **문장은 남기지 않는다** (병원 이름).
 *   다만 Pleos SDK와 음성 서비스가 문장을 기기 로그에 남긴다 (spike S13). 우리는 막을 수 없다.
 */
class PleosTextToSpeechClient(context: Context) : TextToSpeechClient {

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun speak(text: String, onEvent: (TtsEvent) -> Unit): AutoCloseable {
        val session = Session(appContext, mainHandler, text, onEvent)
        mainHandler.post { session.open() }
        return session
    }

    /** 말하기 한 번. 초기화 결과와 재생 이벤트를 받고, 닫힌 뒤 오는 이벤트는 버린다. */
    private class Session(
        private val appContext: Context,
        private val mainHandler: Handler,
        private val text: String,
        private val onEvent: (TtsEvent) -> Unit,
    ) : OnInitializeListener, EventListener, AutoCloseable {

        private val releaseAfterGrace = Runnable { releaseNow("시간 초과") }

        @Volatile
        private var closed = false

        // 아래는 메인 스레드에서만 읽고 쓴다.
        private var tts: TextToSpeech? = null
        private var openedAt = 0L
        private var listenerAdded = false
        private var speaking = false
        private var released = false

        private fun emit(event: TtsEvent) {
            if (!closed) onEvent(event)
        }

        /** 메인 스레드. 객체를 만들고 초기화를 요청한다. */
        fun open() {
            if (closed) return
            openedAt = SystemClock.elapsedRealtime()
            try {
                val created = TextToSpeech(appContext, Mode.ON_DEVICE)
                tts = created
                created.initialize(this)
                Log.i(TAG, "initialize 요청")
            } catch (e: Exception) {
                Log.w(TAG, "initialize 실패: ${e.javaClass.simpleName}")
                emit(TtsEvent.InitFailed(e.javaClass.simpleName))
            }
        }

        override fun onSuccess() {
            mainHandler.post { startSpeaking() }
        }

        /** 메인 스레드. 초기화가 끝난 뒤 설정하고 읽기 시작한다. */
        private fun startSpeaking() {
            val created = tts ?: return
            if (closed || released) return
            Log.i(TAG, "initialize 성공 (${SystemClock.elapsedRealtime() - openedAt}ms)")
            try {
                created.addEventListener(this)
                listenerAdded = true
                created.setLocale(Locale.KOREA)
                created.setAudioAttributes(GUIDANCE_AUDIO)
                created.speak(text, true)
                speaking = true
                Log.i(TAG, "speak 호출 (${text.length}자)") // 문장은 남기지 않는다
            } catch (e: Exception) {
                Log.w(TAG, "말하기 시작 실패: ${e.javaClass.simpleName}")
                emit(TtsEvent.InitFailed(e.javaClass.simpleName))
            }
        }

        override fun onFailure(errorType: ErrorType) {
            Log.w(TAG, "initialize 실패: ${errorType.name}")
            emit(TtsEvent.InitFailed(errorType.name))
        }

        override fun onReady() = emit(TtsEvent.Ready)

        override fun onStart() {
            Log.i(TAG, "onStart")
            emit(TtsEvent.Started)
        }

        override fun onDone() {
            Log.i(TAG, "onDone")
            ended("끝")
            emit(TtsEvent.Done)
        }

        override fun onStop() {
            Log.i(TAG, "onStop")
            ended("멈춤")
            emit(TtsEvent.Stopped)
        }

        override fun onError(errorType: ErrorType) {
            Log.w(TAG, "onError: ${errorType.name}")
            ended("오류")
            emit(TtsEvent.Error(errorType.name))
        }

        override fun onUpdatedRms(rms: Double) = Unit

        /** 읽기가 끝났다. 이미 닫혔으면(멈춤을 기다리던 중이면) 지금 놓는다. emit보다 먼저 메인에 올려서 close보다 앞선다. */
        private fun ended(reason: String) {
            mainHandler.post {
                speaking = false
                if (closed) releaseNow(reason)
            }
        }

        override fun close() {
            if (closed) return
            closed = true
            mainHandler.post {
                if (speaking) {
                    runQuietly("stop") { tts?.stop() }
                    mainHandler.postDelayed(releaseAfterGrace, STOP_GRACE_MS)
                } else {
                    releaseNow("닫기")
                }
            }
        }

        /** 리스너를 떼고 SDK를 놓는다. 메인 스레드에서 한 번만. */
        private fun releaseNow(reason: String) {
            if (released) return
            released = true
            mainHandler.removeCallbacks(releaseAfterGrace)
            val created = tts ?: return
            if (listenerAdded) runQuietly("removeEventListener") { created.removeEventListener(this) }
            runQuietly("release") { created.release() }
            Log.i(TAG, "release ($reason)")
        }

        private inline fun runQuietly(name: String, block: () -> Unit) {
            try {
                block()
            } catch (e: Exception) {
                Log.w(TAG, "$name 실패: ${e.javaClass.simpleName}")
            }
        }
    }

    private companion object {
        const val TAG = "PleosTts"

        /** `stop()` 뒤 `onStop`을 기다리는 최대 시간. spike S7에서는 약 16ms에 왔다. 그 안에 오지 않아도 놓는다. */
        const val STOP_GRACE_MS = 1_000L

        /** 길안내 음성으로 재생한다 (docs/plans 017 "usage 고르는 규칙"). */
        val GUIDANCE_AUDIO: AudioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
    }
}
