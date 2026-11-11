package com.meongcare.app.data.voice

import com.meongcare.app.domain.card.Symptom.BREATHING_DIFFICULTY
import com.meongcare.app.domain.card.Symptom.COUGH
import com.meongcare.app.domain.card.Symptom.LETHARGY
import com.meongcare.app.domain.card.Symptom.LIMPING
import com.meongcare.app.domain.card.Symptom.NO_APPETITE
import com.meongcare.app.domain.card.Symptom.VOMITING
import com.meongcare.app.domain.voice.VoiceEvent
import com.meongcare.app.domain.voice.symptomsIn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * `FakeVoiceInput` 경계 테스트. 기준: docs/plans/active/010-voice-input.md "설계 결정 3"
 * (Ready → 낱말을 하나씩 늘린 Partial → Final → 끝, 부를 때마다 다음 예시 문장, 실패 없음, 시간 값 300/400/300ms)과
 * `VoiceInput` KDoc (Final 또는 Failed를 낸 뒤 끝난다, Final은 빈 문장이 아니다).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FakeVoiceInputBoundaryTest {

    private val noDelay = Duration.ZERO

    private fun fake(sentences: List<String> = SampleVoiceSentences.all) =
        FakeVoiceInput(sentences = sentences, readyDelay = noDelay, wordDelay = noDelay, finalDelay = noDelay)

    /** 이벤트와 그 이벤트가 나온 가상 시각(ms). */
    private suspend fun TestScope.timed(input: FakeVoiceInput): List<Pair<Long, VoiceEvent>> {
        val got = mutableListOf<Pair<Long, VoiceEvent>>()
        input.listen().collect { got += currentTime to it }
        return got
    }

    @Test
    fun `시간 값마다 맡은 간격을 지킨다`() = runTest {
        val input = FakeVoiceInput(
            sentences = listOf("기침을 하고 기운이 없어요"),
            readyDelay = 100.milliseconds,
            wordDelay = 200.milliseconds,
            finalDelay = 50.milliseconds,
        )

        val got = timed(input)

        assertEquals("준비는 readyDelay 뒤", 100L to VoiceEvent.Ready, got.first())
        val partialTimes = got.filter { it.second is VoiceEvent.Partial }.map { it.first }
        assertEquals("부분 결과 사이는 wordDelay", List(partialTimes.size - 1) { 200L }, partialTimes.zipWithNext { a, b -> b - a })
        assertEquals("최종 결과는 마지막 부분 결과 뒤 finalDelay", partialTimes.last() + 50L, got.last().first)
    }

    @Test
    fun `기본 시간 값은 계획의 준비 0점3초 낱말 0점4초 최종 0점3초다`() = runTest {
        val got = timed(FakeVoiceInput(sentences = listOf("토하고 숨을 가쁘게 쉬어요")))

        assertEquals(300L to VoiceEvent.Ready, got.first())
        val partialTimes = got.filter { it.second is VoiceEvent.Partial }.map { it.first }
        assertTrue(partialTimes.zipWithNext { a, b -> b - a }.all { it == 400L })
        assertEquals(partialTimes.last() + 300L, got.last().first)
    }

    @Test
    fun `낱말이 하나뿐인 문장도 준비로 시작해 그 문장의 최종 결과로 끝난다`() = runTest {
        val events = fake(listOf("토했어요")).listen().toList()

        assertEquals(VoiceEvent.Ready, events.first())
        assertEquals(VoiceEvent.Final("토했어요"), events.last())
        events.drop(1).dropLast(1).forEach { assertEquals(VoiceEvent.Partial("토했어요"), it) }
    }

    @Test
    fun `예시 문장마다 부분 결과는 첫 낱말에서 시작해 한 낱말씩 길어진다`() = runTest {
        val input = fake()
        SampleVoiceSentences.all.forEach { sentence ->
            val partials = input.listen().toList().filterIsInstance<VoiceEvent.Partial>().map { it.text }
            val words = sentence.split(' ')

            assertEquals(sentence, words.indices.map { words.take(it + 1).joinToString(" ") }, partials)
        }
    }

    @Test
    fun `예시 문장을 두 바퀴 돌아도 실패 없이 매번 최종 결과 하나로 끝난다`() = runTest {
        val input = fake()
        val finals = List(SampleVoiceSentences.all.size * 2) {
            val events = input.listen().toList()
            assertEquals(VoiceEvent.Ready, events.first())
            assertTrue("실패를 내지 않는다", events.none { it is VoiceEvent.Failed })
            assertEquals("최종 결과는 하나이고 마지막이다", 1, events.count { it is VoiceEvent.Final })
            events.last() as VoiceEvent.Final
        }

        assertEquals(
            (SampleVoiceSentences.all + SampleVoiceSentences.all).map { VoiceEvent.Final(it) },
            finals,
        )
        assertTrue("최종 결과는 빈 문장이 아니다", finals.all { it.text.isNotBlank() })
    }

    @Test
    fun `기본 예시 문장은 계획의 세 문장이고 계획에 적은 증상을 찾는다`() {
        val expected = listOf(
            "강아지가 갑자기 토하고 숨을 가쁘게 쉬어요" to setOf(VOMITING, BREATHING_DIFFICULTY),
            "기침을 하고 기운이 없어요" to setOf(COUGH, LETHARGY),
            "다리를 절고 밥을 안 먹어요" to setOf(LIMPING, NO_APPETITE),
        )

        assertEquals(expected.map { it.first }, SampleVoiceSentences.all)
        expected.forEach { (sentence, symptoms) -> assertEquals(sentence, symptoms, symptomsIn(sentence)) }
    }
}
