package com.meongcare.app.data.voice

import ai.pleos.playground.stt.SpeechToText
import ai.pleos.playground.stt.constant.ErrorType
import ai.pleos.playground.stt.constant.Mode
import ai.pleos.playground.stt.listener.OnInitializeListener
import ai.pleos.playground.stt.listener.ResultListener
import android.content.Context
import android.util.Log
import java.util.Locale

/**
 * Pleos SpeechToText SDK(3.3.1)를 `ON_DEVICE` 모드로 부르는 유일한 곳. `ai.pleos.playground.stt`는 이 파일에서만 import한다.
 *
 * - 순서 (docs/pleos/sdk-notes.md "STT `ON_DEVICE` 실행 확인"에서 동작한 순서 그대로):
 *   `SpeechToText(context, Mode.ON_DEVICE)` → `initialize(listener)` → 성공하면 `addListener` → `setLocale(Locale.KOREA)` → `request()`.
 * - `registerApp`을 부르지 않는다. `ON_DEVICE`는 앱 등록(Client ID/Secret)이 필요 없다.
 * - 듣기 한 번마다 새로 만들고, [AutoCloseable.close]에서 `stop()` → `removeListener()` → `release()`로 놓는다.
 *   문서: release 후 다시 쓰려면 새 객체를 만든다.
 * - 오디오 포커스는 요청하지 않는다. Pleos 문서에 듣는 동안의 포커스 이야기가 없다 (sdk-notes, 2026-11-05 조사).
 * - 로그에는 이벤트 이름과 오류 이름만 남긴다. **인식한 문장은 남기지 않는다** (반려견 의료정보, Pleos 보안 가이드).
 * - `start()`는 메인 스레드에서 불린다 (화면 ViewModel에서 구독). 콜백이 어느 스레드로 오는지는 문서에 없어서 가리지 않는다.
 */
class PleosSpeechToTextClient(context: Context) : SpeechToTextClient {

    private val appContext = context.applicationContext

    override fun start(onEvent: (SttEvent) -> Unit): AutoCloseable {
        val stt = SpeechToText(appContext, Mode.ON_DEVICE)
        val session = Session(stt, onEvent)
        stt.initialize(session)
        Log.i(TAG, "initialize 요청")
        return session
    }

    /** 듣기 한 번. 초기화 결과와 인식 이벤트를 받고, 닫힌 뒤 오는 콜백은 버린다. */
    private class Session(
        private val speechToText: SpeechToText,
        private val onEvent: (SttEvent) -> Unit,
    ) : OnInitializeListener, ResultListener, AutoCloseable {

        @Volatile
        private var closed = false

        @Volatile
        private var listening = false

        private fun emit(event: SttEvent) {
            if (!closed) onEvent(event)
        }

        override fun onSuccess() {
            if (closed) return
            Log.i(TAG, "initialize 성공")
            try {
                speechToText.addListener(this)
                listening = true
                speechToText.setLocale(Locale.KOREA)
                speechToText.request()
                Log.i(TAG, "request 호출")
            } catch (e: Exception) {
                Log.w(TAG, "듣기 시작 실패: ${e.javaClass.simpleName}")
                emit(SttEvent.InitFailed(e.javaClass.simpleName))
            }
        }

        override fun onFailure(errorType: ErrorType) {
            Log.w(TAG, "initialize 실패: ${errorType.name}")
            emit(SttEvent.InitFailed(errorType.name))
        }

        override fun onReady() {
            Log.i(TAG, "onReady")
            emit(SttEvent.Ready)
        }

        override fun onStartedRecognition() = emit(SttEvent.Started)

        override fun onUpdated(stt: String, completed: Boolean) {
            if (completed) Log.i(TAG, "onUpdated 완료") // 문장은 남기지 않는다
            emit(SttEvent.Updated(stt, completed))
        }

        override fun onEndedRecognition() {
            Log.i(TAG, "onEndedRecognition")
            emit(SttEvent.Ended)
        }

        override fun onError(errorType: ErrorType) {
            Log.w(TAG, "onError: ${errorType.name}")
            emit(SttEvent.Error(errorType.name))
        }

        override fun onUpdatedEpdData(on: Long, off: Long) = Unit

        override fun onUpdatedRms(rms: Double) = Unit

        override fun close() {
            if (closed) return
            closed = true
            if (listening) {
                runQuietly("stop") { speechToText.stop() }
                runQuietly("removeListener") { speechToText.removeListener(this) }
            }
            runQuietly("release") { speechToText.release() }
            Log.i(TAG, "close")
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
        const val TAG = "PleosStt"
    }
}
