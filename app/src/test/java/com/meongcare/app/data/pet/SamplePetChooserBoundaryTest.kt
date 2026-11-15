package com.meongcare.app.data.pet

import app.cash.turbine.test
import com.meongcare.app.domain.pet.PetProfileLoad
import com.meongcare.app.domain.pet.SamplePet
import com.meongcare.app.domain.pet.SamplePetChoice
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 계획 021 "데이터"의 경계: `SamplePetChoice` 계약(모르는 id 무시, 같은 id 다시 안 내보냄,
 * `selectNext`는 끝에서 처음으로, 한 마리면 그대로)과 `PetProfileSource` 계약(구독하면 현재 값을 먼저).
 */
class SamplePetChooserBoundaryTest {
    private val wanggeon = SamplePetProfiles.wanggeon
    private val osyong = SamplePetProfiles.osyong
    private val third = SamplePet(
        id = "P-TEST-03",
        profile = SamplePetProfiles.demoPet.copy(name = "셋째"),
        weight = null,
        traits = null,
    )

    @Test
    fun `세 마리면 다음으로 고를 때 차례대로 돌고 끝에서 처음으로 온다`() {
        val chooser = SamplePetChooser(listOf(wanggeon, osyong, third))
        val seen = (1..4).map {
            chooser.selectNext()
            chooser.selected.value.id
        }
        assertEquals(listOf("P-DEMO-02", "P-TEST-03", "P-DEMO-01", "P-DEMO-02"), seen)
    }

    @Test
    fun `id로 고른 뒤 다음으로 고르면 고른 반려견의 다음이다`() {
        val chooser = SamplePetChooser(listOf(wanggeon, osyong, third))
        chooser.select("P-TEST-03")
        chooser.selectNext()
        assertEquals(wanggeon, chooser.selected.value)
    }

    @Test
    fun `처음 고른 반려견은 받은 목록의 첫째다`() {
        val chooser = SamplePetChooser(listOf(osyong, wanggeon))
        assertEquals(osyong, chooser.selected.value)
    }

    @Test
    fun `다음으로 고르면 프로필도 다음 반려견으로 내보낸다`() = runTest {
        val chooser = SamplePetChooser()
        chooser.profile.test {
            assertEquals(PetProfileLoad.Loaded(wanggeon.profile), awaitItem())
            chooser.selectNext()
            assertEquals(PetProfileLoad.Loaded(osyong.profile), awaitItem())
            chooser.selectNext()
            assertEquals(PetProfileLoad.Loaded(wanggeon.profile), awaitItem())
        }
    }

    @Test
    fun `한 마리면 다음으로 골라도 프로필을 다시 내보내지 않는다`() = runTest {
        val chooser = SamplePetChooser(listOf(osyong))
        chooser.profile.test {
            assertEquals(PetProfileLoad.Loaded(osyong.profile), awaitItem())
            chooser.selectNext()
            expectNoEvents()
        }
    }

    @Test
    fun `모르는 id를 고르면 selected도 profile도 내보내지 않는다`() = runTest {
        val chooser = SamplePetChooser()
        chooser.selected.test {
            assertEquals(wanggeon, awaitItem())
            chooser.select("P-DEMO-99")
            chooser.select("")
            chooser.select("p-demo-02")
            expectNoEvents()
        }
        chooser.profile.test {
            awaitItem()
            chooser.select("P-DEMO-99")
            expectNoEvents()
        }
    }

    @Test
    fun `이미 고른 오숑이를 다시 고르면 다시 내보내지 않는다`() = runTest {
        val chooser = SamplePetChooser()
        chooser.select("P-DEMO-02")
        chooser.selected.test {
            assertEquals(osyong, awaitItem())
            chooser.select("P-DEMO-02")
            expectNoEvents()
        }
    }

    @Test
    fun `고른 뒤에 새로 구독하면 지금 고른 반려견을 먼저 받는다`() = runTest {
        val chooser = SamplePetChooser()
        chooser.select("P-DEMO-02")
        chooser.profile.test {
            assertEquals(PetProfileLoad.Loaded(osyong.profile), awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun `구독을 끊었다가 다시 구독해도 고른 반려견이 그대로다`() = runTest {
        val chooser = SamplePetChooser()
        chooser.profile.test {
            awaitItem()
            chooser.select("P-DEMO-02")
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }
        chooser.profile.test {
            assertEquals(PetProfileLoad.Loaded(osyong.profile), awaitItem())
        }
        assertEquals(osyong, chooser.selected.value)
    }

    @Test
    fun `고르기는 반려견 하나만 바꾸고 목록은 그대로다`() {
        val chooser = SamplePetChooser()
        chooser.select("P-DEMO-02")
        chooser.selectNext()
        assertEquals(SamplePetProfiles.all, chooser.pets)
    }

    @Test
    fun `selected와 profile은 늘 같은 반려견이다`() = runTest {
        val chooser = SamplePetChooser()
        chooser.profile.test {
            awaitItem()
            listOf<(SamplePetChoice) -> Unit>({ it.selectNext() }, { it.select("P-DEMO-01") }, { it.select("P-DEMO-02") })
                .forEach { action ->
                    action(chooser)
                    assertEquals(PetProfileLoad.Loaded(chooser.selected.value.profile), awaitItem())
                }
        }
    }

    @Test
    fun `두 고르기는 서로 상관없다`() {
        val a = SamplePetChooser()
        val b = SamplePetChooser()
        a.select("P-DEMO-02")
        assertEquals(osyong, a.selected.value)
        assertEquals(wanggeon, b.selected.value)
    }
}
