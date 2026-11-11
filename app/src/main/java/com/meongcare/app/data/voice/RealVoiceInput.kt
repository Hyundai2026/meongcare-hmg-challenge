package com.meongcare.app.data.voice

import com.meongcare.app.domain.voice.VoiceEvent
import com.meongcare.app.domain.voice.VoiceFailure
import com.meongcare.app.domain.voice.VoiceInput
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Pleos SpeechToText `ON_DEVICE`로 한 문장을 듣는다. SDK 이벤트를 [VoiceEvent]로 바꾸는 판단만 한다
 * (SDK 호출은 [SpeechToTextClient]). docs/plans 010 "설계 결정 5"의 표:
 *
 * | 받은 것 | 내보내는 것 |
 * |---|---|
 * | Ready | Ready |
 * | Started | 없음 (부분 결과마다 다시 온다) |
 * | Updated(완료 아님) | Partial |
 * | Updated(완료), 글자 있음 | Final → 끝 |
 * | Updated(완료), 비었거나 공백뿐 | Failed(NO_SPEECH) → 끝 |
 * | Ended | 없음. 그때부터 [finalAfterEndedTimeout] 안에 최종 결과가 없으면 Failed(NO_SPEECH) |
 * | Error TIMEOUT | Failed(NO_SPEECH) |
 * | Error MIC_ERROR·AUDIO_ERROR | Failed(MIC) |
 * | 그 밖의 Error, InitFailed | Failed(UNAVAILABLE) (오류 종류가 늘 수 있어서 기본 분기를 둔다, Pleos 문서 권장) |
 * | 시작 후 [initTimeout] 동안 Ready·Updated·Ended가 없음 (Started만 와도) | Failed(UNAVAILABLE) (음성 서비스가 없는 기기에서 콜백이 안 올 때) |
 * | 시작 후 [maxListen] | Failed(NO_SPEECH) |
 * | 끝난 뒤 오는 이벤트 | 무시 |
 * | client.start가 예외 | Failed(UNAVAILABLE) |
 *
 * 듣기 한 번마다 SpeechToText를 새로 만들고 끝나면 놓는다 ("설계 결정 6"). 구독을 취소해도 놓는다.
 * 시간 값은 기본안이다. Pleos 문서에 무음 대기 시간·최대 녹음 시간이 없어서 기기에서 보고 고친다.
 */
class RealVoiceInput(
    private val client: SpeechToTextClient,
    private val initTimeout: Duration = 3.seconds,
    private val finalAfterEndedTimeout: Duration = 2.seconds,
    private val maxListen: Duration = 15.seconds,
) : VoiceInput {

    override fun listen(): Flow<VoiceEvent> = channelFlow {
        val sttEvents = Channel<SttEvent>(Channel.UNLIMITED)
        val connection = try {
            client.start { sttEvents.trySend(it) }
        } catch (e: Exception) {
            send(VoiceEvent.Failed(VoiceFailure.UNAVAILABLE))
            return@channelFlow
        }
        try {
            val end = withTimeoutOrNull(maxListen) { relay(sttEvents, this@channelFlow) }
            send(end ?: VoiceEvent.Failed(VoiceFailure.NO_SPEECH))
        } finally {
            sttEvents.close()
            try {
                connection.close()
            } catch (_: Exception) {
                // 닫다가 난 예외로 듣기 종료를 막지 않는다.
            }
        }
    }

    /**
     * 끝(Final·Failed)이 나올 때까지 중간 이벤트를 [out]으로 넘기고, 끝 이벤트를 돌려준다.
     * 시간은 이벤트가 올 때마다 다시 재지 않는다. 시작한 때부터 [initTimeout], 인식이 끝난 때부터 [finalAfterEndedTimeout]을 한 번씩 잰다.
     */
    private suspend fun relay(events: ReceiveChannel<SttEvent>, out: SendChannel<VoiceEvent>): VoiceEvent {
        val relay = Relay(events, out)
        // 1. 첫 응답: Ready·Updated나 인식 끝. Started만 오는 것은 응답으로 치지 않는다.
        val first = withTimeoutOrNull(initTimeout) { Waited(relay.receiveUntil { responded || ended }) }
            ?: return VoiceEvent.Failed(VoiceFailure.UNAVAILABLE)
        first.end?.let { return it }
        // 2. 인식이 끝날 때까지. 상한은 바깥의 maxListen이다.
        relay.receiveUntil { ended }?.let { return it }
        // 3. 인식이 끝난 뒤 최종 결과
        return withTimeoutOrNull(finalAfterEndedTimeout) { relay.receiveUntil { false } }
            ?: VoiceEvent.Failed(VoiceFailure.NO_SPEECH)
    }

    /** [withTimeoutOrNull]의 null(시간 초과)과 "끝 이벤트 없이 조건을 채움"을 가르는 상자. */
    private class Waited(val end: VoiceEvent?)

    /** SDK 이벤트를 하나씩 받아 표대로 바꾼다. 받은 것의 상태(응답했는지, 인식이 끝났는지)를 들고 있다. */
    private class Relay(
        private val events: ReceiveChannel<SttEvent>,
        private val out: SendChannel<VoiceEvent>,
    ) {
        var responded = false
            private set
        var ended = false
            private set

        /** [done]이 참이 될 때까지 받는다. 그 사이에 끝 이벤트가 오면 그것을 돌려주고, 아니면 null. */
        suspend fun receiveUntil(done: Relay.() -> Boolean): VoiceEvent? {
            while (!done()) {
                handle(events.receive())?.let { return it }
            }
            return null
        }

        private suspend fun handle(event: SttEvent): VoiceEvent? {
            when (event) {
                SttEvent.Ready -> {
                    responded = true
                    out.send(VoiceEvent.Ready)
                }
                SttEvent.Started -> Unit
                is SttEvent.Updated -> {
                    responded = true
                    if (event.completed) {
                        return if (event.text.isBlank()) VoiceEvent.Failed(VoiceFailure.NO_SPEECH) else VoiceEvent.Final(event.text)
                    }
                    out.send(VoiceEvent.Partial(event.text))
                }
                SttEvent.Ended -> ended = true
                is SttEvent.Error -> return VoiceEvent.Failed(failureOf(event.name))
                is SttEvent.InitFailed -> return VoiceEvent.Failed(VoiceFailure.UNAVAILABLE)
            }
            return null
        }
    }
}

/** `ErrorType.name` → 화면에 보일 실패 이유. 모르는 이름(새로 생긴 오류)은 쓸 수 없음으로 본다. */
internal fun failureOf(errorName: String): VoiceFailure = when (errorName) {
    "TIMEOUT" -> VoiceFailure.NO_SPEECH
    "MIC_ERROR", "AUDIO_ERROR" -> VoiceFailure.MIC
    else -> VoiceFailure.UNAVAILABLE
}
