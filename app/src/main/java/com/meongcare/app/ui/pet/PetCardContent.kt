package com.meongcare.app.ui.pet

import com.meongcare.app.domain.pet.AgeLabel
import com.meongcare.app.domain.pet.PetProfileLoad
import com.meongcare.app.domain.pet.ageLabelOf
import com.meongcare.app.ui.card.MedicationText
import com.meongcare.app.ui.card.initialOf
import java.time.LocalDate

/** 반려동물 카드 화면 상태 (019, 보기 전용) */
sealed interface PetCardUiState {
    data object Loading : PetCardUiState
    data object NotRegistered : PetCardUiState
    data object Unavailable : PetCardUiState

    /**
     * 등록된 반려견. 빈 목록·null은 화면에서 "미등록"이다.
     * 체중·주치 병원·특징은 반려견 프로필 형식(docs/data/pet-profile.md)에 아직 없어 늘 "미등록"으로 보인다.
     */
    data class Registered(
        val name: String,
        val initial: String,
        val breed: String?,
        val birthDate: LocalDate?,
        val age: AgeLabel?,
        val ageIsEstimated: Boolean,
        val conditions: List<String>,
        val medications: List<MedicationText>,
        val allergies: List<String>,
    ) : PetCardUiState
}

/**
 * 프로필 → 반려동물 카드. 공백 정리는 진료 카드([com.meongcare.app.ui.card.medicalCardOf])와 같은 규칙이다:
 * 앞뒤 공백을 지우고 비면 뺀다.
 *
 * @param load null이면 아직 받지 못했다
 * @param today 나이를 셀 날 (기기 시간대의 오늘)
 */
fun petCardOf(load: PetProfileLoad?, today: LocalDate): PetCardUiState = when (load) {
    null -> PetCardUiState.Loading
    PetProfileLoad.NotRegistered -> PetCardUiState.NotRegistered
    PetProfileLoad.Unavailable -> PetCardUiState.Unavailable
    is PetProfileLoad.Loaded -> load.profile.let { p ->
        PetCardUiState.Registered(
            name = p.name,
            initial = initialOf(p.name),
            breed = p.breed?.trim()?.takeIf { it.isNotEmpty() },
            birthDate = p.birthDate,
            age = ageLabelOf(p.birthDate, today),
            ageIsEstimated = p.birthDateIsEstimated,
            conditions = p.medical.conditions.cleaned(),
            medications = p.medical.medications.mapNotNull { m ->
                val name = m.name.trim()
                if (name.isEmpty()) null else MedicationText(name, m.dosage.trim().takeIf { it.isNotEmpty() })
            },
            allergies = p.medical.allergies.cleaned(),
        )
    }
}

private fun List<String>.cleaned(): List<String> = map { it.trim() }.filter { it.isNotEmpty() }
