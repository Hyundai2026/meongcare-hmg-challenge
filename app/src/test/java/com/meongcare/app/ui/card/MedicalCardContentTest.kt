package com.meongcare.app.ui.card

import com.meongcare.app.data.pet.SamplePetProfiles
import com.meongcare.app.domain.card.Symptom.BREATHING_DIFFICULTY
import com.meongcare.app.domain.card.Symptom.COUGH
import com.meongcare.app.domain.card.Symptom.SEIZURE
import com.meongcare.app.domain.pet.AgeLabel
import com.meongcare.app.domain.pet.MedicalInfo
import com.meongcare.app.domain.pet.Medication
import com.meongcare.app.domain.pet.PetProfileLoad
import com.meongcare.app.domain.pet.Sex
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MedicalCardContentTest {
    private val today = LocalDate.of(2026, 11, 5)
    private val seoul = ZoneId.of("Asia/Seoul")
    private val bori = SamplePetProfiles.demoPet

    private fun card(profile: PetProfileLoad?, vararg symptoms: com.meongcare.app.domain.card.Symptom) =
        medicalCardOf(profile, symptoms.toSet(), today, seoul)

    @Test
    fun `보리와 기침, 호흡 곤란으로 카드를 만들면 컨셉 그림의 줄이 모두 있다`() {
        val result = card(PetProfileLoad.Loaded(bori), COUGH, BREATHING_DIFFICULTY)
        assertEquals(
            PetHeader.Registered(
                name = "보리",
                initial = "보",
                sex = Sex.MALE,
                age = AgeLabel.YearsAndMonths(12, 3),
                ageIsEstimated = false,
                breed = "골든리트리버",
            ),
            result.pet,
        )
        assertEquals(listOf(BREATHING_DIFFICULTY, COUGH), result.symptoms)
        assertEquals(
            MedicalRows(
                conditions = listOf("심장질환"),
                allergies = listOf("특정 단백질 알레르기"),
                medications = listOf(MedicationText("심장약", "1일 1회")),
                updatedOn = LocalDate.of(2026, 11, 5),
            ),
            result.medical,
        )
    }

    @Test
    fun `증상은 누른 순서가 아니라 프리셋 순서다`() {
        assertEquals(listOf(SEIZURE, COUGH), card(PetProfileLoad.Loaded(bori), COUGH, SEIZURE).symptoms)
        assertEquals(emptyList<Any>(), card(PetProfileLoad.Loaded(bori)).symptoms)
    }

    @Test
    fun `빈 의료정보 목록은 빈 줄이고 공백뿐인 항목은 건너뛴다`() {
        val profile = bori.copy(
            medical = MedicalInfo(
                conditions = emptyList(),
                allergies = listOf("  ", " 닭고기 "),
                medications = listOf(Medication(" ", "1일 1회")),
            ),
        )
        val medical = card(PetProfileLoad.Loaded(profile)).medical!!
        assertEquals(emptyList<String>(), medical.conditions)
        assertEquals(listOf("닭고기"), medical.allergies)
        assertEquals(emptyList<MedicationText>(), medical.medications)
    }

    @Test
    fun `복용법이 없는 약은 이름만 쓴다`() {
        val profile = bori.copy(medical = bori.medical.copy(medications = listOf(Medication("심장약", ""), Medication("진통제", "  "))))
        assertEquals(
            listOf(MedicationText("심장약", null), MedicationText("진통제", null)),
            card(PetProfileLoad.Loaded(profile)).medical!!.medications,
        )
    }

    @Test
    fun `20개의 긴 항목도 자르지 않고 모두 넣는다`() {
        val long = List(20) { i -> "가".repeat(49) + i }
        val profile = bori.copy(medical = bori.medical.copy(conditions = long))
        assertEquals(long, card(PetProfileLoad.Loaded(profile)).medical!!.conditions)
    }

    @Test
    fun `프로필을 받지 못했거나 등록되지 않았거나 읽지 못했으면 의료정보 줄이 없다`() {
        assertEquals(PetHeader.Loading, card(null, COUGH).pet)
        assertEquals(PetHeader.NotRegistered, card(PetProfileLoad.NotRegistered, COUGH).pet)
        assertEquals(PetHeader.Unavailable, card(PetProfileLoad.Unavailable, COUGH).pet)
        listOf(null, PetProfileLoad.NotRegistered, PetProfileLoad.Unavailable).forEach {
            val result = card(it, COUGH)
            assertNull(result.medical)
            assertEquals(listOf(COUGH), result.symptoms)
        }
    }

    @Test
    fun `품종과 생년월일이 없으면 칩 값이 없고 미래 생년월일은 나이를 보이지 않는다`() {
        val noChip = card(PetProfileLoad.Loaded(bori.copy(breed = "  ", birthDate = null))).pet as PetHeader.Registered
        assertNull(noChip.breed)
        assertNull(noChip.age)
        val future = card(PetProfileLoad.Loaded(bori.copy(birthDate = LocalDate.of(2026, 11, 6)))).pet as PetHeader.Registered
        assertNull(future.age)
    }

    @Test
    fun `추정 생년월일이면 추정이라고 표시한다`() {
        val estimated = card(PetProfileLoad.Loaded(bori.copy(birthDateIsEstimated = true))).pet as PetHeader.Registered
        assertEquals(true, estimated.ageIsEstimated)
    }

    @Test
    fun `아바타 글자는 이름의 첫 코드 포인트다`() {
        assertEquals("보", initialOf("보리"))
        assertEquals("😀", initialOf("😀보리"))
        assertEquals("", initialOf(""))
    }

    @Test
    fun `정보 갱신일은 기기 시간대의 날짜다`() {
        val profile = bori.copy(updatedAt = Instant.parse("2026-11-04T15:30:00Z"))
        assertEquals(LocalDate.of(2026, 11, 5), card(PetProfileLoad.Loaded(profile)).medical!!.updatedOn)
        assertEquals(
            LocalDate.of(2026, 11, 4),
            medicalCardOf(PetProfileLoad.Loaded(profile), emptySet(), today, ZoneId.of("UTC")).medical!!.updatedOn,
        )
    }
}
