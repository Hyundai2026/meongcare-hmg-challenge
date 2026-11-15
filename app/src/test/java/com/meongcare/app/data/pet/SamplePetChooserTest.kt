package com.meongcare.app.data.pet

import app.cash.turbine.test
import com.meongcare.app.domain.pet.PetProfileLoad
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** 계획 021 "데이터": 고르기 규칙 (프로토타입 `cycle`/`pickDog`) */
class SamplePetChooserTest {
    private val wanggeon = SamplePetProfiles.wanggeon
    private val osyong = SamplePetProfiles.osyong

    @Test
    fun `처음은 왕건이다`() = runTest {
        val chooser = SamplePetChooser()
        assertEquals(wanggeon, chooser.selected.value)
        chooser.profile.test { assertEquals(PetProfileLoad.Loaded(wanggeon.profile), awaitItem()) }
    }

    @Test
    fun `id로 고르면 고른 반려견과 프로필이 바뀐다`() = runTest {
        val chooser = SamplePetChooser()
        chooser.profile.test {
            awaitItem()
            chooser.select("P-DEMO-02")
            assertEquals(PetProfileLoad.Loaded(osyong.profile), awaitItem())
        }
        assertEquals(osyong, chooser.selected.value)
    }

    @Test
    fun `같은 id를 다시 골라도 다시 내보내지 않는다`() = runTest {
        val chooser = SamplePetChooser()
        chooser.profile.test {
            awaitItem()
            chooser.select("P-DEMO-01")
            expectNoEvents()
        }
    }

    @Test
    fun `모르는 id는 무시한다`() {
        val chooser = SamplePetChooser()
        chooser.select("P-DEMO-99")
        chooser.select("")
        assertEquals(wanggeon, chooser.selected.value)
    }

    @Test
    fun `다음으로 고르면 끝에서 처음으로 돈다`() {
        val chooser = SamplePetChooser()
        chooser.selectNext()
        assertEquals(osyong, chooser.selected.value)
        chooser.selectNext()
        assertEquals(wanggeon, chooser.selected.value)
    }

    @Test
    fun `한 마리면 다음으로 골라도 그대로다`() {
        val chooser = SamplePetChooser(listOf(osyong))
        chooser.selectNext()
        assertEquals(osyong, chooser.selected.value)
    }

    @Test
    fun `목록은 받은 그대로다`() {
        assertEquals(SamplePetProfiles.all, SamplePetChooser().pets)
    }
}
