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
import org.junit.Test

class PetCardContentTest {
    private val today = LocalDate.of(2026, 11, 13)

    @Test
    fun `샘플 왕건이를 카드 값으로 바꾼다`() {
        val card = petCardOf(PetProfileLoad.Loaded(SamplePetProfiles.demoPet), today) as PetCardUiState.Registered
        assertEquals("왕건이", card.name)
        assertEquals("왕", card.initial)
        assertEquals("래브라도 리트리버", card.breed)
        assertEquals(LocalDate.of(2014, 3, 12), card.birthDate)
        assertEquals(AgeLabel.YearsAndMonths(12, 8), card.age)
        assertEquals(listOf("심장질환"), card.conditions)
        assertEquals(listOf(MedicationText("심장질환 약 복용 중(상세 약명 미등록)", null)), card.medications)
        assertEquals(emptyList<String>(), card.allergies)
    }

    @Test
    fun `빈 품종과 공백 항목은 뺀다`() {
        val pet = SamplePetProfiles.demoPet.copy(
            breed = "  ",
            medical = MedicalInfo(listOf(" ", " 심장질환 "), emptyList(), listOf(Medication(" ", "1회"), Medication("약", "  "))),
        )
        val card = petCardOf(PetProfileLoad.Loaded(pet), today) as PetCardUiState.Registered
        assertNull(card.breed)
        assertEquals(listOf("심장질환"), card.conditions)
        assertEquals(listOf(MedicationText("약", null)), card.medications)
        assertEquals(emptyList<String>(), card.allergies)
    }

    @Test
    fun `프로필이 없으면 따로 상태다`() {
        assertEquals(PetCardUiState.Loading, petCardOf(null, today))
        assertEquals(PetCardUiState.NotRegistered, petCardOf(PetProfileLoad.NotRegistered, today))
        assertEquals(PetCardUiState.Unavailable, petCardOf(PetProfileLoad.Unavailable, today))
    }
}
