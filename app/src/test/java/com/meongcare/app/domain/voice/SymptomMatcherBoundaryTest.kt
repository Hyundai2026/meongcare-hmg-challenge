package com.meongcare.app.domain.voice

import com.meongcare.app.domain.card.Symptom
import com.meongcare.app.domain.card.Symptom.BLEEDING_OR_WOUND
import com.meongcare.app.domain.card.Symptom.BLOATED_BELLY
import com.meongcare.app.domain.card.Symptom.BREATHING_DIFFICULTY
import com.meongcare.app.domain.card.Symptom.COLLAPSE
import com.meongcare.app.domain.card.Symptom.COUGH
import com.meongcare.app.domain.card.Symptom.DIARRHEA
import com.meongcare.app.domain.card.Symptom.LETHARGY
import com.meongcare.app.domain.card.Symptom.LIMPING
import com.meongcare.app.domain.card.Symptom.NO_APPETITE
import com.meongcare.app.domain.card.Symptom.SEIZURE
import com.meongcare.app.domain.card.Symptom.SWALLOWED_OBJECT
import com.meongcare.app.domain.card.Symptom.VOMITING
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `symptomsIn` 경계 테스트. 기준은 docs/plans/active/010-voice-input.md "설계 결정 4"의 규칙 1~5와 키워드 표다.
 * 구현의 키워드 표가 아니라 계획의 표를 여기에 옮겨 적고 비교한다.
 */
class SymptomMatcherBoundaryTest {

    /** 계획 "설계 결정 4"의 키워드 표 (공백 없는 형태). 앞: 부정 규칙을 적용하는 키워드, 뒤: 뜻에 부정이 들어간 키워드. */
    private val planKeywords: Map<Symptom, Pair<List<String>, List<String>>> = mapOf(
        BREATHING_DIFFICULTY to (
            listOf("숨을가쁘", "숨이가쁘", "숨가쁘", "숨이가빠", "숨가빠", "숨을바쁘", "숨이바쁘", "숨이차", "숨을헐떡", "헐떡", "호흡곤란", "호흡이힘들", "호흡이어렵", "숨쉬기힘들", "숨쉬기어렵") to
                listOf("숨을못쉬", "숨을잘못쉬")
            ),
        SEIZURE to (listOf("경련", "발작", "경기를일으", "경기를해", "경기했") to emptyList()),
        COLLAPSE to (listOf("쓰러", "기절", "의식을잃", "정신을잃") to listOf("의식이없", "못일어나", "일어나지못")),
        BLEEDING_OR_WOUND to (listOf("피가", "피를흘", "피나", "출혈", "상처", "다쳤", "다쳐", "찢어", "물렸") to emptyList()),
        SWALLOWED_OBJECT to (listOf("삼켰", "삼킨", "삼켜", "이물", "주워먹", "집어먹") to emptyList()),
        VOMITING to (listOf("구토", "토했", "토하", "토해", "토를했", "토를해", "토를하", "게워", "구역질") to emptyList()),
        DIARRHEA to (listOf("설사", "묽은변", "변이묽", "물똥", "혈변") to emptyList()),
        COUGH to (listOf("기침", "켁켁", "캑캑", "컥컥") to emptyList()),
        LETHARGY to (
            listOf("축처", "늘어져", "늘어졌", "늘어지", "처져", "처졌", "무기력") to
                listOf("기운이없", "기운없", "힘이없", "힘없", "기력이없", "안움직", "움직이지않")
            ),
        NO_APPETITE to (emptyList<String>() to listOf("안먹", "못먹", "먹지않", "먹지를않", "식욕이없", "식욕없", "입맛이없")),
        LIMPING to (
            listOf("절어", "절고", "다리를절", "발을절", "절뚝", "절룩", "다리를들고", "발을들고") to
                listOf("못걷", "못걸", "걷지못", "걷지를못", "다리를못")
            ),
        BLOATED_BELLY to (listOf("배가부풀", "배가빵빵", "배가볼록", "배가불러", "배부풂", "복부팽만") to emptyList()),
    )

    private val allPlanKeywords: List<Pair<Symptom, String>> =
        planKeywords.flatMap { (symptom, keywords) -> (keywords.first + keywords.second).map { symptom to it } }

    private fun assertFinds(expected: Set<Symptom>, sentence: String) =
        assertEquals("\"$sentence\"", expected, symptomsIn(sentence))

    // 규칙 2와 표

    @Test
    fun `계획 표의 키워드는 하나하나가 그 증상 하나만 찾는다`() {
        val wrong = allPlanKeywords.mapNotNull { (symptom, keyword) ->
            val sentence = "강아지가 $keyword"
            val got = symptomsIn(sentence)
            if (got == setOf(symptom)) null else "\"$sentence\" → $got (기대: $symptom)"
        }
        assertTrue("표와 다르게 찾은 키워드:\n" + wrong.joinToString("\n"), wrong.isEmpty())
    }

    @Test
    fun `계획 표는 모든 증상 프리셋을 다룬다`() {
        assertEquals(Symptom.entries.toSet(), planKeywords.keys)
    }

    // 규칙 1: 정규화

    @Test
    fun `키워드 글자 사이마다 띄어 써도 같은 증상을 찾는다`() {
        val wrong = allPlanKeywords.mapNotNull { (symptom, keyword) ->
            val sentence = "강아지가 " + keyword.toList().joinToString(" ")
            val got = symptomsIn(sentence)
            if (got == setOf(symptom)) null else "\"$sentence\" → $got (기대: $symptom)"
        }
        assertTrue("띄어 쓰면 못 찾은 키워드:\n" + wrong.joinToString("\n"), wrong.isEmpty())
    }

    @Test
    fun `줄바꿈 탭 여러 칸 공백도 지우고 찾는다`() {
        assertFinds(setOf(BREATHING_DIFFICULTY), "숨을\n가쁘게\t쉬어요")
        assertFinds(setOf(VOMITING), "토   했어요")
        assertFinds(setOf(VOMITING), "토\r\n했어요")
    }

    @Test
    fun `여러 가지 문장부호가 키워드 사이에 끼어도 지우고 찾는다`() {
        val marks = listOf(".", ",", "!", "?", "…", "~", "·", "\"", "'", "(", ")", "-", ":", ";", "/")
        val wrong = marks.mapNotNull { mark ->
            val sentence = "토${mark}했어요"
            if (symptomsIn(sentence) == setOf(VOMITING)) null else "\"$sentence\" → ${symptomsIn(sentence)}"
        }
        assertTrue("문장부호를 지우지 못했다:\n" + wrong.joinToString("\n"), wrong.isEmpty())
    }

    @Test
    fun `숫자가 섞인 문장에서도 찾는다`() {
        assertFinds(setOf(VOMITING), "3번 토했어요")
        assertFinds(setOf(DIARRHEA), "설사를 2시간째 해요")
    }

    @Test
    fun `공백이나 문장부호뿐인 문장은 빈 결과다`() {
        listOf("", " ", "   ", "\n\t", "...!?", "…", " , . ").forEach { assertFinds(emptySet(), it) }
    }

    // 규칙 3: 부정

    @Test
    fun `부정 표현 여섯 가지를 모두 알아듣고 그 증상은 고르지 않는다`() {
        assertFinds(emptySet(), "설사를 하지 않아요") // 않
        assertFinds(emptySet(), "설사는 없어요") // 없
        assertFinds(emptySet(), "설사는 안 했어요") // 안했
        assertFinds(emptySet(), "설사는 안 해요") // 안해
        assertFinds(emptySet(), "설사는 안 하는 것 같아요") // 안하
        assertFinds(emptySet(), "설사가 아니에요") // 아니
    }

    @Test
    fun `키워드 바로 앞이 안이나 못이면 고르지 않는다`() {
        assertFinds(emptySet(), "약을 못 삼켰어요")
        assertFinds(emptySet(), "안 쓰러졌어요")
    }

    @Test
    fun `부정 표현은 고 는데 지만에서 끊겨 다음 말의 증상에만 걸린다`() {
        assertFinds(setOf(DIARRHEA), "설사를 하고 기침은 없어요")
        assertFinds(setOf(COUGH), "기침을 하는데 설사는 안 해요")
        assertFinds(setOf(VOMITING), "토하긴 했지만 설사는 하지 않아요")
        assertFinds(setOf(COUGH), "설사를 하지 않고 기침해요")
    }

    @Test
    fun `키워드 뒤 여덟 번째 글자의 부정은 걸리고 그보다 먼 부정은 걸리지 않는다`() {
        // "기침" 뒤: 소 리 가 들 린 적 은 없 → "없"이 8번째 글자
        assertFinds(emptySet(), "기침 소리가 들린 적은 없어요")
        // "기침" 뒤 8글자(을 계 속 하 더 니 지 금) 안에 부정이 없다. "없"은 그 뒤 다른 말(기운이 없어요)에 있다
        assertFinds(setOf(COUGH, LETHARGY), "기침을 계속 하더니 지금은 기운이 없어요")
    }

    @Test
    fun `아니라고 바로잡으면 앞 증상은 빼고 뒤 증상만 고른다`() {
        assertFinds(setOf(COUGH), "구토가 아니라 기침이에요")
    }

    @Test
    fun `같은 키워드가 부정된 자리와 아닌 자리에 다 있으면 고른다`() {
        assertFinds(setOf(COUGH), "아까는 기침을 안 했는데 지금은 기침을 해요")
        assertFinds(setOf(VOMITING), "구토는 안 했는데 방금 토했어요")
    }

    // 규칙 4: 뜻에 부정이 들어간 키워드

    @Test
    fun `뜻에 부정이 든 키워드는 앞에 안 못이 붙거나 뒤에 부정 표현이 와도 고른다`() {
        assertFinds(setOf(BREATHING_DIFFICULTY), "숨을 잘 못 쉬어요")
        assertFinds(setOf(NO_APPETITE, LETHARGY), "밥을 못 먹고 기운이 없어요")
        assertFinds(setOf(LETHARGY, COLLAPSE), "힘이 없어서 일어나지 못해요")
        assertFinds(setOf(NO_APPETITE, LETHARGY), "안 먹고 안 움직여요")
        assertFinds(setOf(LIMPING), "다리를 못 써요")
    }

    @Test
    fun `주워 먹었다는 말은 이물 삼킴이고 식욕 없음으로 고르지 않는다`() {
        assertFinds(setOf(SWALLOWED_OBJECT), "산책하다 뭘 주워 먹었어요")
        assertFinds(setOf(SWALLOWED_OBJECT), "휴지를 집어 먹었어요")
    }

    // 규칙 5: 글자 하나, 키워드 글자가 들어간 다른 낱말

    @Test
    fun `숨 피 토 한 글자만으로는 고르지 않는다`() {
        assertFinds(emptySet(), "숨을 쉬어요")
        assertFinds(emptySet(), "피부가 빨개요")
        assertFinds(emptySet(), "강아지가 토라졌어요")
        assertFinds(emptySet(), "토끼를 봤어요")
        assertFinds(emptySet(), "숨바꼭질을 했어요")
    }

    @Test
    fun `토마토를 먹었다는 말은 구토로 고르지 않는다`() {
        // 계획 규칙 5: "토요일", "토마토를"이 구토로 잡히지 않게 한다.
        assertFinds(emptySet(), "강아지가 토마토를 하나 먹었어요")
        assertFinds(emptySet(), "토마토를 하루에 하나씩 먹어요")
    }

    @Test
    fun `토요일에 토했다는 말은 구토로 고른다`() {
        assertFinds(setOf(VOMITING), "토요일에 토했어요")
    }

    // 표의 낱말을 흔히 말하는 꼴 (계획 단계 6: 키워드 누락은 표를 고쳐 맞춘다)

    @Test
    fun `숨이 가빠요처럼 가쁘다를 흔히 말하는 꼴로도 호흡 곤란을 찾는다`() {
        assertFinds(setOf(BREATHING_DIFFICULTY), "숨이 가빠요")
        assertFinds(setOf(BREATHING_DIFFICULTY), "갑자기 숨이 가빠졌어요")
    }

    @Test
    fun `못 걸어요처럼 걷다를 흔히 말하는 꼴로도 절뚝거림을 찾는다`() {
        assertFinds(setOf(LIMPING), "잘 못 걸어요")
        assertFinds(setOf(LIMPING), "다리가 아파서 못 걸어요")
    }

    @Test
    fun `늘어졌어요 처졌어요처럼 지난 일로 말해도 기운 없음을 찾는다`() {
        assertFinds(setOf(LETHARGY), "축 늘어졌어요")
        assertFinds(setOf(LETHARGY), "강아지가 많이 처졌어요")
    }

    @Test
    fun `결과는 같은 증상을 한 번만 담는다`() {
        assertFinds(setOf(VOMITING), "토했어요 또 토하고 구토를 해요")
    }

    @Test
    fun `출혈 상처 배 부풂처럼 다른 증상도 한 문장에서 같이 찾는다`() {
        assertFinds(
            setOf(BLEEDING_OR_WOUND, BLOATED_BELLY, SEIZURE),
            "다른 개한테 물렸는데 배가 빵빵하고 경련을 해요",
        )
    }
}
