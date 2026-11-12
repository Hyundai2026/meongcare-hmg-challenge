package com.meongcare.app.data.speech

import com.meongcare.app.domain.speech.SpeakFailure
import com.meongcare.app.domain.speech.SpeakResult
import com.meongcare.app.domain.speech.Speaker
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Pleos TextToSpeech `ON_DEVICE`로 한 문장을 읽는다. SDK 이벤트를 [SpeakResult]로 바꾸는 판단만 한다
 * (SDK 호출은 [TextToSpeechClient]). docs/plans 017 "설계 결정 5"의 표:
 *
 * | 받은 것 | 결과 |
 * |---|---|
 * | Ready, Started | 없음 (응답으로 친다) |
 * | Done | Done → 닫는다 |
 * | Stopped | Stopped → 닫는다 |
 * | Error AUDIO_ERROR | Failed(AUDIO) |
 * | 그 밖의 Error, InitFailed | Failed(UNAVAILABLE) (오류 종류가 늘 수 있어서 기본 분기를 둔다, Pleos 문서 권장) |
 * | 시작 후 [initTimeout] 동안 Ready·Started·Done이 없음 | Failed(UNAVAILABLE) (음성 서비스가 없는 기기에서 콜백이 안 올 때) |
 * | 시작 후 [baseSpeakTime] + [perCharSpeakTime] × 글자 수 | Failed(UNAVAILABLE) → 닫는다 |
 * | 끝난 뒤 오는 이벤트 | 무시 |
 * | client.speak가 예외 | Failed(UNAVAILABLE) |
 * | 부른 코루틴 취소 | 닫는다 (한 번만) |
 *
 * - **한 번에 하나씩** 읽는다 ([Mutex]). Pleos 음성 서비스는 읽는 중에 새 문장을 받으면 앞 문장을 끊고,
 *   끊긴 문장에는 끝 이벤트를 보내지 않는다 (017 spike S6). 그래서 앞 문장이 끝난 뒤에 다음 문장을 맡긴다.
 * - 말하기 한 번마다 TextToSpeech를 새로 만들고 끝나면 놓는다 ("설계 결정 6", spike: 초기화 9~16ms).
 * - 시간은 이벤트가 올 때마다 다시 재지 않는다. [baseSpeakTime]이 넉넉한 이유: 부팅 뒤 첫 합성은 소리까지 약 6초 늦었다 (spike S3).
 */
class RealSpeaker(
    private val client: TextToSpeechClient,
    private val initTimeout: Duration = 3.seconds,
    private val baseSpeakTime: Duration = 10.seconds,
    private val perCharSpeakTime: Duration = 250.milliseconds,
) : Speaker {

    private val oneAtATime = Mutex()

    override suspend fun speak(text: String): SpeakResult = oneAtATime.withLock { speakNow(text) }

    private suspend fun speakNow(text: String): SpeakResult {
        val events = Channel<TtsEvent>(Channel.UNLIMITED)
        val connection = try {
            client.speak(text) { events.trySend(it) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return SpeakResult.Failed(SpeakFailure.UNAVAILABLE)
        }
        try {
            return withTimeoutOrNull(baseSpeakTime + perCharSpeakTime * text.length) { awaitEnd(events) }
                ?: SpeakResult.Failed(SpeakFailure.UNAVAILABLE)
        } finally {
            events.close()
            try {
                connection.close()
            } catch (_: Exception) {
                // 닫다가 난 예외로 결과를 막지 않는다.
            }
        }
    }

    /** 끝 이벤트까지 기다린다. 시작한 때부터 [initTimeout] 안에 첫 응답이 없으면 쓸 수 없음이다. */
    private suspend fun awaitEnd(events: ReceiveChannel<TtsEvent>): SpeakResult {
        val first = withTimeoutOrNull(initTimeout) {
            var event = events.receive()
            while (event !is TtsEvent.Ready && event !is TtsEvent.Started && resultOf(event) == null) {
                event = events.receive()
            }
            Waited(resultOf(event))
        } ?: return SpeakResult.Failed(SpeakFailure.UNAVAILABLE)
        first.end?.let { return it }
        while (true) {
            resultOf(events.receive())?.let { return it }
        }
    }

    /** [withTimeoutOrNull]의 null(시간 초과)과 "끝 이벤트 없이 응답만 옴"을 가르는 상자. */
    private class Waited(val end: SpeakResult?)
}

/** 끝 이벤트면 결과, 아니면 null. 모르는 오류 이름(새로 생긴 오류)은 쓸 수 없음으로 본다. */
internal fun resultOf(event: TtsEvent): SpeakResult? = when (event) {
    TtsEvent.Ready, TtsEvent.Started -> null
    TtsEvent.Done -> SpeakResult.Done
    TtsEvent.Stopped -> SpeakResult.Stopped
    is TtsEvent.Error ->
        SpeakResult.Failed(if (event.name == "AUDIO_ERROR") SpeakFailure.AUDIO else SpeakFailure.UNAVAILABLE)
    is TtsEvent.InitFailed -> SpeakResult.Failed(SpeakFailure.UNAVAILABLE)
}
