package com.meongcare.app.data.pet

import com.meongcare.app.domain.pet.MedicalInfo
import com.meongcare.app.domain.pet.Medication
import com.meongcare.app.domain.pet.PetProfile
import com.meongcare.app.domain.pet.SamplePet
import com.meongcare.app.domain.pet.Sex
import java.time.Instant
import java.time.LocalDate

/**
 * 시연용 샘플 반려견 두 마리 (021). 보호자 웹과 연결되기 전까지 쓴다.
 *
 * 값의 원천은 디자인 프로토타입(`docs/product/design/prototype/MeongCareCar.html`의 P-DEMO-01/02)과
 * 2026-11-15 사용자 결정(왕건이 품종·체중, 두 마리 성별)이다. `docs/data/pet-profile.example.json`(보리,
 * 형식 예시·보호자 웹 샘플)과는 다르다. 예시 JSON과 같은 값이 필요한 테스트는 test의 `ExamplePetProfiles`를 쓴다.
 */
object SamplePetProfiles {
    private val updatedAt = Instant.parse("2026-11-05T00:00:00.000Z")

    val wanggeon = SamplePet(
        id = "P-DEMO-01",
        profile = PetProfile(
            name = "왕건이",
            breed = "래브라도 리트리버",
            sex = Sex.MALE,
            birthDate = LocalDate.of(2014, 3, 12),
            birthDateIsEstimated = false,
            photoDataUrl = null,
            medical = MedicalInfo(
                conditions = listOf("심장질환"),
                allergies = emptyList(),
                medications = listOf(Medication(name = "심장질환 약 복용 중(상세 약명 미등록)", dosage = "")),
            ),
            updatedAt = updatedAt,
        ),
        weight = "30kg",
        traits = null,
    )

    val osyong = SamplePet(
        id = "P-DEMO-02",
        profile = PetProfile(
            name = "오숑이",
            breed = "비숑 프리제",
            sex = Sex.MALE,
            birthDate = LocalDate.of(2023, 5, 8),
            birthDateIsEstimated = false,
            photoDataUrl = null,
            medical = MedicalInfo(conditions = emptyList(), allergies = emptyList(), medications = emptyList()),
            updatedAt = updatedAt,
        ),
        weight = null,
        traits = "차량 이동에 예민함",
    )

    /** 고르는 순서. 처음 고른 반려견은 첫째(왕건이)다 (프로토타입 `S.cur=0`) */
    val all: List<SamplePet> = listOf(wanggeon, osyong)

    /** 처음 고른 샘플(왕건이)의 프로필. 테스트와 Fake가 바탕 값으로 쓴다 */
    val demoPet: PetProfile get() = wanggeon.profile
}
