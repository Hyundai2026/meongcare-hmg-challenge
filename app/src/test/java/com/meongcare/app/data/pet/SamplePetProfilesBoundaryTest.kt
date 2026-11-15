package com.meongcare.app.data.pet

import com.meongcare.app.domain.pet.AgeLabel
import com.meongcare.app.domain.pet.PetProfileLoad
import com.meongcare.app.ui.pet.PetCardUiState
import com.meongcare.app.ui.pet.petCardOf
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 계획 021 완료 기준 5의 반려동물 카드 값(오숑이·왕건이)과 샘플 값의 경계.
 * 화면 그림은 `/verify`가 보고, 여기서는 카드에 들어가는 값만 본다.
 */
class SamplePetProfilesBoundaryTest {

    private fun card(pet: com.meongcare.app.domain.pet.SamplePet, today: LocalDate) =
        petCardOf(PetProfileLoad.Loaded(pet.profile), today) as PetCardUiState.Registered

    @Test
    fun `2026-11-15에 오숑이 카드는 비숑 프리제 3세 6개월이고 의료정보는 모두 비었다`() {
        val c = card(SamplePetProfiles.osyong, LocalDate.of(2026, 11, 15))
        assertEquals("오숑이", c.name)
        assertEquals("오", c.initial)
        assertEquals("비숑 프리제", c.breed)
        assertEquals(LocalDate.of(2023, 5, 8), c.birthDate)
        assertEquals(AgeLabel.YearsAndMonths(3, 6), c.age)
        assertEquals(false, c.ageIsEstimated)
        assertEquals(emptyList<String>(), c.conditions)
        assertEquals(emptyList<Any>(), c.medications)
        assertEquals(emptyList<String>(), c.allergies)
    }

    @Test
    fun `오숑이 체중은 없고 특징은 차량 이동에 예민함이다`() {
        assertEquals(null, SamplePetProfiles.osyong.weight)
        assertEquals("차량 이동에 예민함", SamplePetProfiles.osyong.traits)
    }

    @Test
    fun `2026-11-15에 왕건이 카드는 래브라도 리트리버 12세 8개월이다`() {
        val c = card(SamplePetProfiles.wanggeon, LocalDate.of(2026, 11, 15))
        assertEquals("래브라도 리트리버", c.breed)
        assertEquals(AgeLabel.YearsAndMonths(12, 8), c.age)
        assertEquals(listOf("심장질환"), c.conditions)
        assertEquals(emptyList<String>(), c.allergies)
    }

    @Test
    fun `샘플 반려견의 이름은 서로 다르고 비어 있지 않다`() {
        val names = SamplePetProfiles.all.map { it.profile.name }
        assertTrue(names.all { it.isNotBlank() })
        assertEquals(names.size, names.toSet().size)
    }

    @Test
    fun `샘플 반려견의 id는 모두 서로 다르다`() {
        val ids = SamplePetProfiles.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `샘플 반려견은 예시 JSON 보리와 다르다`() {
        SamplePetProfiles.all.forEach { pet ->
            assertNotEquals("보리", pet.profile.name)
            assertNotEquals(ExamplePetProfiles.bori, pet.profile)
        }
    }

    @Test
    fun `고르기의 처음 반려견과 demoPet은 같다`() {
        assertEquals(SamplePetProfiles.demoPet, SamplePetChooser().selected.value.profile)
    }
}
