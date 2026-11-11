package com.meongcare.app.data.voice

import com.meongcare.app.domain.voice.VoiceEvent
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds

class FakeVoiceInputTest {

    @Test
    fun `준비, 늘어나는 부분 결과, 최종 결과 순서로 내고 끝난다`() = runTest {
        val input = FakeVoiceInput(sentences = listOf("기침을 하고 기운이 없어요"))

        assertEquals(
            listOf(
                VoiceEvent.Ready,
                VoiceEvent.Partial("기침을"),
                VoiceEvent.Partial("기침을 하고"),
                VoiceEvent.Partial("기침을 하고 기운이"),
                VoiceEvent.Partial("기침을 하고 기운이 없어요"),
                VoiceEvent.Final("기침을 하고 기운이 없어요"),
            ),
            input.listen().toList(),
        )
    }

    @Test
    fun `부를 때마다 다음 예시 문장을 쓰고 끝에서 처음으로 돌아간다`() = runTest {
        val input = FakeVoiceInput(sentences = listOf("하나", "둘"))

        val finals = List(3) { input.listen().last() }

        assertEquals(
            listOf(VoiceEvent.Final("하나"), VoiceEvent.Final("둘"), VoiceEvent.Final("하나")),
            finals,
        )
    }

    @Test
    fun `구독을 취소하면 더 내보내지 않는다`() = runTest {
        val input = FakeVoiceInput(
            sentences = listOf("기침을 하고 기운이 없어요"),
            readyDelay = 300.milliseconds,
            wordDelay = 400.milliseconds,
        )
        val received = mutableListOf<VoiceEvent>()
        val job = launch { input.listen().collect { received += it } }

        advanceTimeBy(800.milliseconds) // 준비(300ms) + 첫 낱말(700ms)까지
        job.cancel()
        advanceTimeBy(10_000.milliseconds)

        assertEquals(listOf(VoiceEvent.Ready, VoiceEvent.Partial("기침을")), received)
    }

    @Test
    fun `기본 예시 문장은 비어 있지 않다`() {
        assert(SampleVoiceSentences.all.isNotEmpty())
        assert(SampleVoiceSentences.all.all { it.isNotBlank() })
    }
}
