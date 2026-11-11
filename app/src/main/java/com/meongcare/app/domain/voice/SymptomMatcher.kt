package com.meongcare.app.domain.voice

import com.meongcare.app.domain.card.Symptom

/**
 * 받아쓴 한 문장에서 증상 프리셋을 찾는다. **진단이 아니라 말한 낱말을 버튼에 옮기는 것이다.**
 * AI 없이 키워드 규칙으로 찾는다 (docs/plans 010 "설계 결정 4").
 *
 * 규칙:
 * 1. 공백·문장부호를 모두 지운다 (받아쓰기의 띄어쓰기가 들쭉날쭉하다).
 *    키워드를 품은 다른 낱말("토마토를"의 "토를", "커피가"의 "피가")은 먼저 가린다 ([DECOY_WORDS]).
 * 2. 증상마다 정한 키워드가 들어 있으면 그 증상이다. 한 문장에서 여러 증상을 모두 찾는다.
 * 3. 부정: 키워드 바로 앞이 "안"·"못"이거나, 키워드 뒤 다음 "고"·"는데"·"지만"·"서"까지(최대 8글자)에
 *    부정 표현("않", "없", "안했", "안해", "안하", "아니")이 있으면 그 자리는 건너뛴다.
 *    예: "구토는 안 했어요", "안 토했어요" → 구토 아님. "토해서 기운이 없어요" → 구토와 기운 없음.
 *    "서"는 "에서"(장소)일 때는 끊지 않는다 ("구토를 집에서 안 했어요" → 구토 아님).
 * 4. 뜻에 부정이 들어간 키워드("기운이없", "안먹" 등)에는 3을 적용하지 않는다.
 * 5. 글자 하나짜리 키워드("토", "숨", "피")는 쓰지 않는다 ("토요일"이 구토로 잡히지 않게).
 */
fun symptomsIn(sentence: String): Set<Symptom> {
    val text = withoutDecoys(normalized(sentence))
    if (text.isEmpty()) return emptySet()
    return symptomKeywords.filterValues { it.matchIn(text) }.keys
}

/**
 * 증상별 키워드 (공백 없는 형태). planner 기본안이며 의학 검토를 거치지 않았다.
 * 받아쓰기가 자주 틀리는 형태도 넣는다 (예: Pleos 엔진이 "가쁘게"를 "바쁘게"로 받아썼다, 2026-11-05 기기 관찰).
 * 고칠 문장이 생기면 `app/src/test/resources/voice/symptom-sentences.tsv`에 줄을 더하고 이 표를 고친다.
 * 화면 글자(strings.xml `symptom_*`)와는 따로 둔다. 화면 글자는 표시용, 키워드는 말하는 방식용이다.
 */
internal val symptomKeywords: Map<Symptom, SymptomKeywords> = mapOf(
    Symptom.BREATHING_DIFFICULTY to SymptomKeywords(
        plain = listOf("숨을가쁘", "숨이가쁘", "숨가쁘", "숨이가빠", "숨가빠", "숨을바쁘", "숨이바쁘", "숨이차", "숨을헐떡", "헐떡", "호흡곤란", "호흡이힘들", "호흡이어렵", "숨쉬기힘들", "숨쉬기어렵"),
        negativeMeaning = listOf("숨을못쉬", "숨을잘못쉬"),
    ),
    Symptom.SEIZURE to SymptomKeywords(
        plain = listOf("경련", "발작", "경기를일으", "경기를해", "경기했"),
    ),
    Symptom.COLLAPSE to SymptomKeywords(
        plain = listOf("쓰러", "기절", "의식을잃", "정신을잃"),
        negativeMeaning = listOf("의식이없", "못일어나", "일어나지못"),
    ),
    Symptom.BLEEDING_OR_WOUND to SymptomKeywords(
        plain = listOf("피가", "피를흘", "피나", "출혈", "상처", "다쳤", "다쳐", "찢어", "물렸"),
    ),
    Symptom.SWALLOWED_OBJECT to SymptomKeywords(
        plain = listOf("삼켰", "삼킨", "삼켜", "이물", "주워먹", "집어먹"),
    ),
    Symptom.VOMITING to SymptomKeywords(
        plain = listOf("구토", "토했", "토하", "토해", "토를했", "토를해", "토를하", "게워", "구역질"),
    ),
    Symptom.DIARRHEA to SymptomKeywords(
        plain = listOf("설사", "묽은변", "변이묽", "물똥", "혈변"),
    ),
    Symptom.COUGH to SymptomKeywords(
        plain = listOf("기침", "켁켁", "캑캑", "컥컥"),
    ),
    Symptom.LETHARGY to SymptomKeywords(
        plain = listOf("축처", "늘어져", "늘어졌", "늘어지", "처져", "처졌", "무기력"),
        negativeMeaning = listOf("기운이없", "기운없", "힘이없", "힘없", "기력이없", "안움직", "움직이지않"),
    ),
    Symptom.NO_APPETITE to SymptomKeywords(
        negativeMeaning = listOf("안먹", "못먹", "먹지않", "먹지를않", "식욕이없", "식욕없", "입맛이없"),
    ),
    Symptom.LIMPING to SymptomKeywords(
        plain = listOf("절어", "절고", "다리를절", "발을절", "절뚝", "절룩", "다리를들고", "발을들고"),
        negativeMeaning = listOf("못걷", "못걸", "걷지못", "걷지를못", "다리를못"),
    ),
    Symptom.BLOATED_BELLY to SymptomKeywords(
        plain = listOf("배가부풀", "배가빵빵", "배가볼록", "배가불러", "배부풂", "복부팽만"),
    ),
)

/**
 * @param plain 부정 규칙(3)을 적용하는 키워드
 * @param negativeMeaning 뜻에 부정이 들어 있어 부정 규칙을 적용하지 않는 키워드
 */
internal class SymptomKeywords(
    val plain: List<String> = emptyList(),
    val negativeMeaning: List<String> = emptyList(),
) {
    val all: List<String> get() = plain + negativeMeaning

    fun matchIn(text: String): Boolean =
        negativeMeaning.any { it in text } || plain.any { keyword -> occurrences(text, keyword).any { !isNegatedAt(text, it, keyword.length) } }
}

private val NEGATION_PREFIXES = setOf('안', '못')
private val NEGATION_WORDS = listOf("않", "없", "안했", "안해", "안하", "아니")
private val CLAUSE_ENDS = listOf("고", "는데", "지만")
private const val CAUSE_END = '서' // "토해서", "아파서". "에서"는 장소라서 끊지 않는다.
private const val NEGATION_WINDOW = 8

/**
 * 키워드를 품었지만 증상이 아닌 낱말. 공백 없는 형태로 적는다.
 * 고칠 문장이 생기면 키워드 표와 같이 여기에 더한다.
 */
internal val DECOY_WORDS = listOf(
    "토마토", // "토마토를 하나" → "토를하"
    "커피", // "커피가", "커피나" → "피가", "피나"
)

/** 규칙 1: 글자와 숫자만 남긴다. */
private fun normalized(sentence: String): String = sentence.filter { it.isLetterOrDigit() }

/** 규칙 1: [DECOY_WORDS]를 글자가 아닌 칸으로 바꿔, 앞뒤 글자와 이어 붙어 키워드가 되지 않게 한다. */
private fun withoutDecoys(text: String): String =
    DECOY_WORDS.fold(text) { acc, decoy -> acc.replace(decoy, " ".repeat(decoy.length)) }.trim()

private fun occurrences(text: String, keyword: String): Sequence<Int> =
    generateSequence(text.indexOf(keyword).takeIf { it >= 0 }) { prev ->
        text.indexOf(keyword, prev + 1).takeIf { it >= 0 }
    }

/** 규칙 3. [start]에서 시작하는 길이 [length]의 키워드가 부정됐는지. */
private fun isNegatedAt(text: String, start: Int, length: Int): Boolean {
    if (start > 0 && text[start - 1] in NEGATION_PREFIXES) return true
    val afterStart = start + length
    val after = text.substring(afterStart, minOf(text.length, afterStart + NEGATION_WINDOW))
    val causeEnd = after.indices.firstOrNull { i -> after[i] == CAUSE_END && text[afterStart + i - 1] != '에' }
    val clauseEnd = (CLAUSE_ENDS.mapNotNull { end -> after.indexOf(end).takeIf { it >= 0 } } + listOfNotNull(causeEnd))
        .minOrNull() ?: after.length
    val window = after.substring(0, clauseEnd)
    return NEGATION_WORDS.any { it in window }
}
