package com.meongcare.app.ui.card

import com.meongcare.app.domain.card.Symptom
import com.meongcare.app.domain.pet.AgeLabel
import com.meongcare.app.domain.pet.Sex
import java.time.LocalDate

/** 카드 머리: 반려견 정보를 받았는지와 이름·칩. */
sealed interface PetHeader {
    data object Loading : PetHeader
    data object NotRegistered : PetHeader
    data object Unavailable : PetHeader

    data class Registered(
        val name: String,
        /** 사진이 없을 때 아바타 글자: 이름의 첫 코드 포인트 (이모지를 반으로 자르지 않는다. 웹 initialOf와 같다) */
        val initial: String,
        val sex: Sex?,
        val age: AgeLabel?,
        val ageIsEstimated: Boolean,
        val breed: String?,
    ) : PetHeader
}

/** 등록된 의료정보 줄. 빈 목록은 "등록된 정보 없음"으로 보인다 (빈 배열 = 등록하지 않음). */
data class MedicalRows(
    /** 기존 병력 (= 주요 질환) */
    val conditions: List<String>,
    val allergies: List<String>,
    /** "심장약 (1일 1회)"처럼 화면에 쓸 글자. 복용법이 없으면 이름만 */
    val medications: List<MedicationText>,
    /** 정보 갱신일 (보호자가 마지막으로 저장한 시각을 기기 시간대 날짜로) */
    val updatedOn: LocalDate,
)

/** 약 한 줄. 괄호 붙이기는 화면이 strings.xml로 한다. */
data class MedicationText(val name: String, val dosage: String?)

data class MedicalCardUiState(
    val pet: PetHeader,
    /** 프리셋 순서. 빈 목록 = "고른 증상 없음" */
    val symptoms: List<Symptom>,
    /** [pet]이 Registered일 때만 있다. 읽지 못한 것과 등록하지 않은 것을 섞지 않으려는 것이다 */
    val medical: MedicalRows?,
)
