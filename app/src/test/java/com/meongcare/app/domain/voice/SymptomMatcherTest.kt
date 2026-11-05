package com.meongcare.app.domain.voice

import com.meongcare.app.domain.card.Symptom
import com.meongcare.app.domain.card.Symptom.BREATHING_DIFFICULTY
import com.meongcare.app.domain.card.Symptom.COLLAPSE
import com.meongcare.app.domain.card.Symptom.COUGH
import com.meongcare.app.domain.card.Symptom.DIARRHEA
import com.meongcare.app.domain.card.Symptom.LETHARGY
import com.meongcare.app.domain.card.Symptom.LIMPING
import com.meongcare.app.domain.card.Symptom.NO_APPETITE
import com.meongcare.app.domain.card.Symptom.VOMITING
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SymptomMatcherTest {

    @Test
    fun `관찰한 받아쓰기 문장에서 구토와 호흡 곤란을 찾는다`() {
        // Pleos 에뮬레이터 ON_DEVICE가 실제로 받아쓴 문장 (sdk-notes "STT ON_DEVICE 실행 확인")
        assertEquals(setOf(VOMITING, BREATHING_DIFFICULTY), symptomsIn("강아지가 갑자기 토하고 숨을 가쁘게 쉬어요"))
    }

    @Test
    fun `띄어쓰기와 문장부호가 달라도 같은 증상을 찾는다`() {
        assertEquals(setOf(VOMITING, BREATHING_DIFFICULTY), symptomsIn("숨을가쁘게 쉬고 토해요!"))
        assertEquals(setOf(VOMITING, BREATHING_DIFFICULTY), symptomsIn("토 하고, 숨을 가쁘게…"))
    }

    @Test
    fun `한 문장에서 여러 증상을 모두 찾는다`() {
        assertEquals(
            setOf(COUGH, DIARRHEA, LIMPING, COLLAPSE),
            symptomsIn("기침하고 설사하더니 다리를 절다가 쓰러졌어요"),
        )
    }

    @Test
    fun `토요일 토마토 안쓰러워처럼 키워드 글자가 들어간 다른 낱말은 고르지 않는다`() {
        assertEquals(emptySet<Symptom>(), symptomsIn("내일 토요일이었으면 좋겠어요"))
        assertEquals(emptySet<Symptom>(), symptomsIn("토마토를 먹었어요"))
        assertEquals(emptySet<Symptom>(), symptomsIn("강아지가 안쓰러워요"))
    }

    @Test
    fun `안 했다거나 없다고 한 증상은 고르지 않는다`() {
        assertEquals(emptySet<Symptom>(), symptomsIn("구토는 안 했어요"))
        assertEquals(emptySet<Symptom>(), symptomsIn("안 토했어요"))
        assertEquals(setOf(COUGH), symptomsIn("설사는 없고 기침을 해요"))
        assertEquals(emptySet<Symptom>(), symptomsIn("토하지 않았어요"))
        assertEquals(setOf(DIARRHEA), symptomsIn("구토 증상은 없는데 설사를 해요"))
    }

    @Test
    fun `까닭을 잇는 서 뒤의 부정은 앞 증상에 걸리지 않는다`() {
        assertEquals(setOf(VOMITING, LETHARGY), symptomsIn("토해서 기운이 없어요"))
        assertEquals(setOf(LIMPING), symptomsIn("다리가 아파서 못 걸어요"))
    }

    @Test
    fun `장소를 뜻하는 에서는 문장을 끊지 않는다`() {
        assertEquals(emptySet<Symptom>(), symptomsIn("구토를 집에서 안 했어요"))
    }

    @Test
    fun `키워드를 품은 다른 낱말은 고르지 않는다`() {
        assertEquals(emptySet<Symptom>(), symptomsIn("토마토를 하나 먹었어요"))
        assertEquals(emptySet<Symptom>(), symptomsIn("커피가 쏟아졌어요"))
        assertEquals(emptySet<Symptom>(), symptomsIn("커피나 우유를 핥았어요"))
        assertEquals(setOf(VOMITING), symptomsIn("토마토를 먹고 토했어요"))
    }

    @Test
    fun `뜻에 부정이 들어간 증상은 그대로 고른다`() {
        assertEquals(setOf(LETHARGY, NO_APPETITE), symptomsIn("기운이 없고 밥을 안 먹어요"))
        assertEquals(setOf(BREATHING_DIFFICULTY), symptomsIn("숨을 못 쉬어요"))
        assertEquals(setOf(LIMPING), symptomsIn("다리를 못 써요"))
    }

    @Test
    fun `증상이 없는 문장과 빈 문장은 빈 결과다`() {
        assertEquals(emptySet<Symptom>(), symptomsIn("치아가 내일 금요일이에요"))
        assertEquals(emptySet<Symptom>(), symptomsIn(""))
        assertEquals(emptySet<Symptom>(), symptomsIn("   ?! "))
    }

    @Test
    fun `모든 증상 프리셋에 키워드가 하나 이상 있다`() {
        Symptom.entries.forEach { symptom ->
            assertTrue("$symptom 키워드 없음", symptomKeywords[symptom]?.all.orEmpty().isNotEmpty())
        }
    }

    @Test
    fun `키워드는 공백 없이 두 글자 이상이다`() {
        symptomKeywords.values.flatMap { it.all }.forEach { keyword ->
            assertTrue("\"$keyword\"는 두 글자 이상이어야 한다", keyword.length >= 2)
            assertTrue("\"$keyword\"에 공백·문장부호가 있다", keyword.all { it.isLetterOrDigit() })
        }
    }

    @Test
    fun `Fake 예시 문장은 모두 증상을 하나 이상 찾는다`() {
        // data/voice/SampleVoiceSentences와 같은 문장 (domain 테스트가 data를 보지 않게 직접 적는다)
        assertEquals(setOf(VOMITING, BREATHING_DIFFICULTY), symptomsIn("강아지가 갑자기 토하고 숨을 가쁘게 쉬어요"))
        assertEquals(setOf(COUGH, LETHARGY), symptomsIn("기침을 하고 기운이 없어요"))
        assertEquals(setOf(LIMPING, NO_APPETITE), symptomsIn("다리를 절고 밥을 안 먹어요"))
    }
}
