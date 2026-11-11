package com.meongcare.app.data.voice

import com.meongcare.app.domain.voice.VoiceEvent
import com.meongcare.app.domain.voice.VoiceInput
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * 실제로 듣지 않고 예시 문장을 받아쓰는 음성 입력. Fake 모드와, 음성 서비스가 없는 일반 폰의 시연 모드에서 쓴다.
 * 화면은 이 구현일 때 "시연용 음성 입력"이라고 알린다 (출품 유의사항).
 *
 * 듣기 한 번: [VoiceEvent.Ready] → 낱말을 하나씩 늘린 [VoiceEvent.Partial] → [VoiceEvent.Final] → 끝.
 * 부를 때마다 다음 예시 문장을 쓰고, 끝에서 처음으로 돌아간다. 실패는 내지 않는다.
 *
 * @param sentences 받아쓸 문장. 비어 있으면 안 된다.
 * @param readyDelay 마이크가 켜지기까지
 * @param wordDelay 낱말 하나가 더해지는 간격
 * @param finalDelay 마지막 부분 결과 뒤 최종 결과까지
 */
class FakeVoiceInput(
    private val sentences: List<String> = SampleVoiceSentences.all,
    private val readyDelay: Duration = 300.milliseconds,
    private val wordDelay: Duration = 400.milliseconds,
    private val finalDelay: Duration = 300.milliseconds,
) : VoiceInput {

    init {
        require(sentences.isNotEmpty()) { "sentences는 비어 있으면 안 된다" }
        require(sentences.all { it.isNotBlank() }) { "빈 문장은 쓸 수 없다 (Final은 빈 문장을 내지 않는다)" }
    }

    private var next = 0

    override fun listen(): Flow<VoiceEvent> = flow {
        val sentence = sentences[next]
        next = (next + 1) % sentences.size

        delay(readyDelay)
        emit(VoiceEvent.Ready)
        val words = sentence.split(' ').filter { it.isNotEmpty() }
        for (count in 1..words.size) {
            delay(wordDelay)
            emit(VoiceEvent.Partial(words.take(count).joinToString(" ")))
        }
        delay(finalDelay)
        emit(VoiceEvent.Final(sentence))
    }
}
