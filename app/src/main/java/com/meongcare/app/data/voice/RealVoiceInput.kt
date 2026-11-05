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
 * | Ended | 없음. [finalAfterEndedTimeout] 안에 최종 결과가 없으면 Failed(NO_SPEECH) |
 * | Error TIMEOUT | Failed(NO_SPEECH) |
 * | Error MIC_ERROR·AUDIO_ERROR | Failed(MIC) |
 * | 그 밖의 Error, InitFailed | Failed(UNAVAILABLE) (오류 종류가 늘 수 있어서 기본 분기를 둔다, Pleos 문서 권장) |
 * | [initTimeout] 동안 Ready·Updated가 없음 | Failed(UNAVAILABLE) (음성 서비스가 없는 기기에서 콜백이 안 올 때) |
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

    /** 끝(Final·Failed)이 나올 때까지 중간 이벤트를 [out]으로 넘기고, 끝 이벤트를 돌려준다. */
    private suspend fun relay(events: ReceiveChannel<SttEvent>, out: SendChannel<VoiceEvent>): VoiceEvent {
        var responded = false
        var ended = false
        while (true) {
            val wait = when {
                ended -> finalAfterEndedTimeout
                !responded -> initTimeout
                else -> Duration.INFINITE
            }
            val event = if (wait.isInfinite()) events.receive() else withTimeoutOrNull(wait) { events.receive() }
            when (event) {
                null -> return VoiceEvent.Failed(if (ended || responded) VoiceFailure.NO_SPEECH else VoiceFailure.UNAVAILABLE)
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
        }
    }
}

/** `ErrorType.name` → 화면에 보일 실패 이유. 모르는 이름(새로 생긴 오류)은 쓸 수 없음으로 본다. */
internal fun failureOf(errorName: String): VoiceFailure = when (errorName) {
    "TIMEOUT" -> VoiceFailure.NO_SPEECH
    "MIC_ERROR", "AUDIO_ERROR" -> VoiceFailure.MIC
    else -> VoiceFailure.UNAVAILABLE
}
