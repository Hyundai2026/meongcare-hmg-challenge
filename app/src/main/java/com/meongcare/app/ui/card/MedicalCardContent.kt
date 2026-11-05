package com.meongcare.app.ui.card

import com.meongcare.app.domain.card.Symptom
import com.meongcare.app.domain.card.inPresetOrder
import com.meongcare.app.domain.pet.PetProfile
import com.meongcare.app.domain.pet.PetProfileLoad
import com.meongcare.app.domain.pet.ageLabelOf
import java.time.LocalDate
import java.time.ZoneId

/**
 * 진료 카드 "자동 생성": 등록된 프로필과 고른 증상을 규칙으로 조합한다 (AI 없음, 문의 A1 이후 팀 논의).
 *
 * - 증상은 누른 순서가 아니라 프리셋 순서다.
 * - 의료정보 항목은 앞뒤 공백을 지우고, 비면 건너뛴다. 그 밖에는 그대로 쓴다 (짐작해서 바꾸지 않는다).
 * - 긴 목록도 자르지 않는다. 수의사에게 보여 줄 카드라 빠지는 정보가 없는 편이 낫다.
 * - 프로필을 받지 못했으면 의료정보 줄을 만들지 않는다.
 *
 * @param profile null이면 아직 받지 못했다 (Loading)
 */
fun medicalCardOf(profile: PetProfileLoad?, symptoms: Set<Symptom>, today: LocalDate, zone: ZoneId): MedicalCardUiState {
    val orderedSymptoms = symptoms.inPresetOrder()
    return when (profile) {
        null -> MedicalCardUiState(PetHeader.Loading, orderedSymptoms, medical = null)
        PetProfileLoad.NotRegistered -> MedicalCardUiState(PetHeader.NotRegistered, orderedSymptoms, medical = null)
        PetProfileLoad.Unavailable -> MedicalCardUiState(PetHeader.Unavailable, orderedSymptoms, medical = null)
        is PetProfileLoad.Loaded -> MedicalCardUiState(
            pet = headerOf(profile.profile, today),
            symptoms = orderedSymptoms,
            medical = rowsOf(profile.profile, zone),
        )
    }
}

private fun headerOf(profile: PetProfile, today: LocalDate) = PetHeader.Registered(
    name = profile.name,
    initial = initialOf(profile.name),
    sex = profile.sex,
    age = ageLabelOf(profile.birthDate, today),
    ageIsEstimated = profile.birthDateIsEstimated,
    breed = profile.breed?.trim()?.takeIf { it.isNotEmpty() },
)

private fun rowsOf(profile: PetProfile, zone: ZoneId) = MedicalRows(
    conditions = profile.medical.conditions.cleaned(),
    allergies = profile.medical.allergies.cleaned(),
    medications = profile.medical.medications.mapNotNull { medication ->
        val name = medication.name.trim()
        if (name.isEmpty()) return@mapNotNull null
        MedicationText(name = name, dosage = medication.dosage.trim().takeIf { it.isNotEmpty() })
    },
    updatedOn = profile.updatedAt.atZone(zone).toLocalDate(),
)

private fun List<String>.cleaned(): List<String> = map { it.trim() }.filter { it.isNotEmpty() }

/** 이름의 첫 코드 포인트. 빈 이름이면 "". */
fun initialOf(name: String): String =
    if (name.isEmpty()) "" else String(Character.toChars(name.codePointAt(0)))
