package com.meongcare.app.domain.card

/**
 * 증상 프리셋 (사용자 결정 2026-11-05). 큰 버튼으로 고른다.
 * 정차 중에는 한 문장으로 말해서 고를 수도 있다 (docs/plans 010, Pleos SpeechToText `ON_DEVICE` → `symptomsIn`).
 * 음성은 추가 입력 수단이고, 버튼만으로도 같은 카드를 만든다.
 *
 * **선언 순서가 화면 순서이자 카드에 적히는 순서다.** 서둘러야 하는 것을 앞에 뒀다 (의학 검토를 거친 순서는 아니다).
 * 누른 순서가 아니라 이 순서로 카드에 적어서, 같은 선택이면 같은 카드가 나온다.
 * 화면 글자는 strings.xml(`symptom_*`)에 있다.
 */
enum class Symptom {
    BREATHING_DIFFICULTY,
    SEIZURE,
    COLLAPSE,
    BLEEDING_OR_WOUND,
    SWALLOWED_OBJECT,
    VOMITING,
    DIARRHEA,
    COUGH,
    LETHARGY,
    NO_APPETITE,
    LIMPING,
    BLOATED_BELLY,
}

/** 프리셋 순서로 정렬한 목록. */
fun Set<Symptom>.inPresetOrder(): List<Symptom> = sortedBy { it.ordinal }
