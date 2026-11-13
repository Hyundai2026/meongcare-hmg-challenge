package com.meongcare.app.ui.pet

import com.meongcare.app.data.pet.SamplePetProfiles
import com.meongcare.app.domain.pet.AgeLabel
import com.meongcare.app.domain.pet.MedicalInfo
import com.meongcare.app.domain.pet.Medication
import com.meongcare.app.domain.pet.PetProfileLoad
import com.meongcare.app.ui.card.MedicationText
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 계획 019 단계 6 `petCardOf` 경계: 날짜 끝값, 빈 값, 공백 정리 규칙. */
class PetCardContentBoundaryTest {
    private val today = LocalDate.of(2026, 11, 13)

    private fun registered(pet: com.meongcare.app.domain.pet.PetProfile) =
        petCardOf(PetProfileLoad.Loaded(pet), today) as PetCardUiState.Registered

    @Test
    fun `생년월일이 없으면 생년월일과 나이가 모두 없다`() {
        val card = registered(SamplePetProfiles.demoPet.copy(birthDate = null))
        assertNull(card.birthDate)
        assertNull(card.age)
    }

    @Test
    fun `생년월일이 오늘보다 뒤면 날짜는 보이고 나이는 없다`() {
        val future = today.plusDays(1)
        val card = registered(SamplePetProfiles.demoPet.copy(birthDate = future))
        assertEquals(future, card.birthDate)
        assertNull(card.age)
    }

    @Test
    fun `오늘 태어났으면 1개월 미만이다`() {
        assertEquals(AgeLabel.UnderOneMonth, registered(SamplePetProfiles.demoPet.copy(birthDate = today)).age)
    }

    @Test
    fun `생일이 하루 남으면 아직 만 나이가 오르지 않는다`() {
        // 2014-11-14생은 2026-11-13에 11세 11개월
        assertEquals(AgeLabel.YearsAndMonths(11, 11), registered(SamplePetProfiles.demoPet.copy(birthDate = LocalDate.of(2014, 11, 14))).age)
        assertEquals(AgeLabel.Years(12), registered(SamplePetProfiles.demoPet.copy(birthDate = LocalDate.of(2014, 11, 13))).age)
    }

    @Test
    fun `나이로 입력한 생년월일은 추정으로 표시된다`() {
        val card = registered(SamplePetProfiles.demoPet.copy(birthDateIsEstimated = true))
        assertTrue(card.ageIsEstimated)
    }

    @Test
    fun `품종이 없거나 앞뒤 공백이 있으면 정리한다`() {
        assertNull(registered(SamplePetProfiles.demoPet.copy(breed = null)).breed)
        assertNull(registered(SamplePetProfiles.demoPet.copy(breed = "")).breed)
        assertEquals("말티즈", registered(SamplePetProfiles.demoPet.copy(breed = " 말티즈\t")).breed)
    }

    @Test
    fun `의료정보가 모두 비어 있으면 빈 목록이다`() {
        val card = registered(SamplePetProfiles.demoPet.copy(medical = MedicalInfo(emptyList(), emptyList(), emptyList())))
        assertEquals(emptyList<String>(), card.conditions)
        assertEquals(emptyList<String>(), card.allergies)
        assertEquals(emptyList<MedicationText>(), card.medications)
    }

    @Test
    fun `약 이름 없이 용량만 있으면 빼고 용량의 앞뒤 공백은 지운다`() {
        val card = registered(
            SamplePetProfiles.demoPet.copy(
                medical = MedicalInfo(emptyList(), emptyList(), listOf(Medication("", "1일 1회"), Medication(" 심장약 ", " 1일 2회 "))),
            ),
        )
        assertEquals(listOf(MedicationText("심장약", "1일 2회")), card.medications)
    }

    @Test
    fun `항목은 순서와 중복을 그대로 둔다`() {
        val card = registered(
            SamplePetProfiles.demoPet.copy(
                medical = MedicalInfo(listOf("심장질환", "관절염", "심장질환"), listOf("닭고기", " 닭고기 "), emptyList()),
            ),
        )
        assertEquals(listOf("심장질환", "관절염", "심장질환"), card.conditions)
        assertEquals(listOf("닭고기", "닭고기"), card.allergies)
    }

    @Test
    fun `이름 첫 글자는 이모지처럼 두 칸짜리 글자도 깨지지 않는다`() {
        assertEquals("🐶", registered(SamplePetProfiles.demoPet.copy(name = "🐶멍이")).initial)
        assertEquals("", registered(SamplePetProfiles.demoPet.copy(name = "")).initial)
    }
}
