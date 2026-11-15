package com.meongcare.app.data.pet

import app.cash.turbine.test
import com.meongcare.app.domain.pet.PetProfileLoad
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class FakePetProfileSourceTest {

    @Test
    fun `처음 구독하면 처음 고른 샘플 프로필을 받는다`() = runTest {
        FakePetProfileSource().profile.test {
            assertEquals(PetProfileLoad.Loaded(SamplePetProfiles.demoPet), awaitItem())
        }
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
