package com.meongcare.app.data.pet

import com.meongcare.app.domain.pet.MedicalInfo
import com.meongcare.app.domain.pet.Medication
import com.meongcare.app.domain.pet.PetProfile
import com.meongcare.app.domain.pet.Sex
import java.time.Instant
import java.time.LocalDate

/**
 * docs/data/pet-profile.example.json(보호자 웹 예시)과 같은 값. 그 파일을 바꾸면 여기도 바꾼다
 * (`FirestorePetProfileJsonTest`가 대조한다). 차 앱 시연 샘플(`SamplePetProfiles`)과는 상관없다 (021).
 */
object ExamplePetProfiles {
    val bori = PetProfile(
        name = "보리",
        breed = "골든리트리버",
        sex = Sex.MALE,
        birthDate = LocalDate.of(2014, 8, 1),
        birthDateIsEstimated = false,
        photoDataUrl = null,
        medical = MedicalInfo(
            conditions = listOf("심장질환"),
            allergies = listOf("특정 단백질 알레르기"),
            medications = listOf(Medication(name = "심장약", dosage = "1일 1회")),
        ),
        updatedAt = Instant.parse("2026-11-05T00:00:00.000Z"),
    )
}
