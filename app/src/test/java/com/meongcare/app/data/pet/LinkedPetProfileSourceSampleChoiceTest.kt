package com.meongcare.app.data.pet

import app.cash.turbine.test
import com.meongcare.app.data.link.FakeOwnerLink
import com.meongcare.app.domain.link.LinkStart
import com.meongcare.app.domain.pet.PetProfileLoad
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** 계획 021 "고른 반려견과 보호자 웹 연결": 고르기는 연결 전에만 화면에 간다 */
class LinkedPetProfileSourceSampleChoiceTest {
    private val wanggeon = SamplePetProfiles.wanggeon.profile
    private val osyong = SamplePetProfiles.osyong.profile
    private val kongi = wanggeon.copy(name = "콩이", breed = "말티즈")

    private fun TestScope.source(link: FakeOwnerLink, chooser: SamplePetChooser, sampleBeforeLink: Boolean = true) =
        LinkedPetProfileSource(backgroundScope, link, chooser, { LinkedProfileRead.Loaded(kongi) }, sampleBeforeLink)

    private suspend fun FakeOwnerLink.linkNow() {
        val code = (start() as LinkStart.Created).code
        claim(code)
        check(code)
    }

    @Test
    fun `연결 전에 고르면 새로 고른 샘플이 나온다`() = runTest {
        val chooser = SamplePetChooser()
        source(FakeOwnerLink(), chooser).profile.test {
            assertEquals(PetProfileLoad.Loaded(wanggeon), awaitItem())
            chooser.select("P-DEMO-02")
            assertEquals(PetProfileLoad.Loaded(osyong), awaitItem())
        }
    }

    @Test
    fun `연결된 동안 골라도 서버 프로필만 나온다`() = runTest {
        val link = FakeOwnerLink()
        val chooser = SamplePetChooser()
        link.linkNow()
        source(link, chooser).profile.test {
            assertEquals(PetProfileLoad.Loaded(kongi), awaitItem())
            chooser.select("P-DEMO-02")
            expectNoEvents()
        }
    }

    @Test
    fun `연결이 끊기면 마지막으로 고른 샘플로 돌아온다`() = runTest {
        val link = FakeOwnerLink()
        val chooser = SamplePetChooser()
        link.linkNow()
        source(link, chooser).profile.test {
            assertEquals(PetProfileLoad.Loaded(kongi), awaitItem())
            chooser.select("P-DEMO-02")
            link.unlink()
            assertEquals(PetProfileLoad.Loaded(osyong), awaitItem())
        }
    }

    @Test
    fun `연결 전 샘플을 보이지 않는 모드면 골라도 등록 안 됨이다`() = runTest {
        val chooser = SamplePetChooser()
        source(FakeOwnerLink(), chooser, sampleBeforeLink = false).profile.test {
            assertEquals(PetProfileLoad.NotRegistered, awaitItem())
            chooser.select("P-DEMO-02")
            expectNoEvents()
        }
    }
}
