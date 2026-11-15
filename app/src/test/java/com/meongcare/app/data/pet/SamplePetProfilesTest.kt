package com.meongcare.app.data.pet

import com.meongcare.app.domain.pet.MedicalInfo
import com.meongcare.app.domain.pet.Medication
import com.meongcare.app.domain.pet.Sex
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/** 계획 021 "사용자가 정한 것"(2026-11-15)과 프로토타입 P-DEMO-01/02의 값 */
class SamplePetProfilesTest {

    @Test
    fun `왕건이는 정한 값이다`() {
        val pet = SamplePetProfiles.wanggeon
        assertEquals("P-DEMO-01", pet.id)
        assertEquals("왕건이", pet.profile.name)
        assertEquals("래브라도 리트리버", pet.profile.breed)
        assertEquals(Sex.MALE, pet.profile.sex)
        assertEquals(LocalDate.of(2014, 3, 12), pet.profile.birthDate)
        assertEquals(false, pet.profile.birthDateIsEstimated)
        assertEquals(null, pet.profile.photoDataUrl)
        assertEquals(
            MedicalInfo(listOf("심장질환"), emptyList(), listOf(Medication("심장질환 약 복용 중(상세 약명 미등록)", ""))),
            pet.profile.medical,
        )
        assertEquals(Instant.parse("2026-11-05T00:00:00Z"), pet.profile.updatedAt)
        assertEquals("30kg", pet.weight)
        assertEquals(null, pet.traits)
    }

    @Test
    fun `오숑이는 정한 값이다`() {
        val pet = SamplePetProfiles.osyong
        assertEquals("P-DEMO-02", pet.id)
        assertEquals("오숑이", pet.profile.name)
        assertEquals("비숑 프리제", pet.profile.breed)
        assertEquals(Sex.MALE, pet.profile.sex)
        assertEquals(LocalDate.of(2023, 5, 8), pet.profile.birthDate)
        assertEquals(false, pet.profile.birthDateIsEstimated)
        assertEquals(null, pet.profile.photoDataUrl)
        assertEquals(MedicalInfo(emptyList(), emptyList(), emptyList()), pet.profile.medical)
        assertEquals(Instant.parse("2026-11-05T00:00:00Z"), pet.profile.updatedAt)
        assertEquals(null, pet.weight)
        assertEquals("차량 이동에 예민함", pet.traits)
    }

    @Test
    fun `두 마리이고 왕건이가 먼저이며 id가 서로 다르다`() {
        assertEquals(listOf(SamplePetProfiles.wanggeon, SamplePetProfiles.osyong), SamplePetProfiles.all)
        assertNotEquals(SamplePetProfiles.wanggeon.id, SamplePetProfiles.osyong.id)
    }

    @Test
    fun `demoPet은 처음 고른 왕건이의 프로필이다`() {
        assertEquals(SamplePetProfiles.wanggeon.profile, SamplePetProfiles.demoPet)
    }
}
