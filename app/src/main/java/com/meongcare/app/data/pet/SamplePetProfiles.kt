package com.meongcare.app.data.pet

import com.meongcare.app.domain.pet.MedicalInfo
import com.meongcare.app.domain.pet.Medication
import com.meongcare.app.domain.pet.PetProfile
import com.meongcare.app.domain.pet.Sex
import java.time.Instant
import java.time.LocalDate

/**
 * 시연용 샘플 반려견. 보호자 웹과 연결되기 전까지 쓴다.
 *
 * 값은 docs/data/pet-profile.example.json과 같다 (컨셉 그림의 "보리"). 그 파일을 바꾸면 여기도 바꾼다.
 * 화면에는 시연용 샘플이라고 알린다 (출품 유의사항: 사실과 다르면 선정 취소).
 */
object SamplePetProfiles {
    /** 샘플 보리의 체중·특징 (사용자 결정 2026-11-14). 반려견 프로필 형식에는 없는 값이라 화면 표시용으로만 둔다 */
    const val DEMO_WEIGHT = "2.8kg"
    const val DEMO_TRAITS = "낯선 곳에서 긴장해요, 안으면 진정해요"

    val demoPet = PetProfile(
        name = "보리",
        breed = "포메라니안",
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
