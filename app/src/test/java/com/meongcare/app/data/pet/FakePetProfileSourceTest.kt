package com.meongcare.app.data.pet

import app.cash.turbine.test
import com.meongcare.app.domain.pet.MedicalInfo
import com.meongcare.app.domain.pet.Medication
import com.meongcare.app.domain.pet.PetProfileLoad
import com.meongcare.app.domain.pet.Sex
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class FakePetProfileSourceTest {

    @Test
    fun `처음 구독하면 보리 프로필을 받는다`() = runTest {
        FakePetProfileSource().profile.test {
            assertEquals(PetProfileLoad.Loaded(SamplePetProfiles.demoPet), awaitItem())
        }
    }

    @Test
    fun `보리는 예시 JSON과 같은 값이다`() {
        // docs/data/pet-profile.example.json
        val bori = SamplePetProfiles.demoPet
        assertEquals("보리", bori.name)
        assertEquals("포메라니안", bori.breed) // 예시 JSON(골든리트리버)과 품종만 다르다 (2026-11-14)
        assertEquals(Sex.MALE, bori.sex)
        assertEquals(LocalDate.of(2014, 8, 1), bori.birthDate)
        assertEquals(false, bori.birthDateIsEstimated)
        assertEquals(null, bori.photoDataUrl)
        assertEquals(
            MedicalInfo(listOf("심장질환"), listOf("특정 단백질 알레르기"), listOf(Medication("심장약", "1일 1회"))),
            bori.medical,
        )
        assertEquals(Instant.parse("2026-11-05T00:00:00Z"), bori.updatedAt)
    }

    @Test
    fun `값을 바꾸면 새 값을 받고 같은 값은 다시 보내지 않는다`() = runTest {
        val source = FakePetProfileSource()
        source.profile.test {
            awaitItem()
            source.set(PetProfileLoad.NotRegistered)
            assertEquals(PetProfileLoad.NotRegistered, awaitItem())
            source.set(PetProfileLoad.NotRegistered)
            source.set(PetProfileLoad.Unavailable)
            assertEquals(PetProfileLoad.Unavailable, awaitItem())
        }
    }
}
