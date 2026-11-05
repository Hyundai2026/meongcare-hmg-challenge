package com.meongcare.app.data.pet

import app.cash.turbine.test
import app.cash.turbine.turbineScope
import com.meongcare.app.domain.pet.PetProfileLoad
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** PetProfileSource 계약: "구독하면 현재 값을 먼저 내보내고, 바뀔 때마다 새 값을 내보낸다." */
class FakePetProfileSourceBoundaryTest {
    private val bori = SamplePetProfiles.bori

    @Test
    fun `처음 값을 정해 만들면 그 값을 먼저 받는다`() = runTest {
        FakePetProfileSource(PetProfileLoad.NotRegistered).profile.test {
            assertEquals(PetProfileLoad.NotRegistered, awaitItem())
        }
        FakePetProfileSource(PetProfileLoad.Unavailable).profile.test {
            assertEquals(PetProfileLoad.Unavailable, awaitItem())
        }
    }

    @Test
    fun `늦게 구독하면 처음 값이 아니라 지금 값을 먼저 받는다`() = runTest {
        val source = FakePetProfileSource()
        source.set(PetProfileLoad.Unavailable)
        source.profile.test {
            assertEquals(PetProfileLoad.Unavailable, awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun `구독자가 둘이면 둘 다 지금 값과 새 값을 받는다`() = runTest {
        val source = FakePetProfileSource()
        turbineScope {
            val first = source.profile.testIn(backgroundScope)
            val second = source.profile.testIn(backgroundScope)
            assertEquals(PetProfileLoad.Loaded(bori), first.awaitItem())
            assertEquals(PetProfileLoad.Loaded(bori), second.awaitItem())
            source.set(PetProfileLoad.NotRegistered)
            assertEquals(PetProfileLoad.NotRegistered, first.awaitItem())
            assertEquals(PetProfileLoad.NotRegistered, second.awaitItem())
            first.cancelAndIgnoreRemainingEvents()
            second.cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `내용이 같은 다른 프로필 객체로 바꾸면 다시 보내지 않고 내용이 다르면 보낸다`() = runTest {
        val source = FakePetProfileSource()
        source.profile.test {
            awaitItem()
            source.set(PetProfileLoad.Loaded(bori.copy()))
            expectNoEvents()
            val choco = bori.copy(name = "초코")
            source.set(PetProfileLoad.Loaded(choco))
            assertEquals(PetProfileLoad.Loaded(choco), awaitItem())
        }
    }

    @Test
    fun `등록 안 됨이 되었다가 다시 보리가 되면 보리를 다시 받는다`() = runTest {
        val source = FakePetProfileSource()
        source.profile.test {
            assertEquals(PetProfileLoad.Loaded(bori), awaitItem())
            source.set(PetProfileLoad.NotRegistered)
            assertEquals(PetProfileLoad.NotRegistered, awaitItem())
            source.set(PetProfileLoad.Loaded(bori))
            assertEquals(PetProfileLoad.Loaded(bori), awaitItem())
        }
    }
}
