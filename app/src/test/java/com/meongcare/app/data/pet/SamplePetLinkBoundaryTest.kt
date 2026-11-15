package com.meongcare.app.data.pet

import app.cash.turbine.test
import com.meongcare.app.data.link.FakeOwnerLink
import com.meongcare.app.domain.link.LinkStart
import com.meongcare.app.domain.pet.PetProfileLoad
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 계획 021 "고른 반려견과 보호자 웹 연결" 표의 경계.
 *
 * Fake 연결(debug)은 계획 "데이터"대로 읽는 순간 고른 샘플을 돌려주는 reader로 짠다
 * (`AppContainer`의 `LinkKind.FAKE` 구성과 같은 모양). `AppContainer`는 Context가 있어야 만들 수 있어서
 * 같은 부품(`SamplePetChooser` + `LinkedPetProfileSource` + `FakeOwnerLink`)으로 맞춰 본다.
 */
class SamplePetLinkBoundaryTest {
    private val wanggeon = SamplePetProfiles.wanggeon.profile
    private val osyong = SamplePetProfiles.osyong.profile

    /** 계획 "데이터": Fake 연결의 reader는 읽는 순간 고른 샘플(`selected.value.profile`)을 돌려준다 */
    private fun fakeLinkReader(chooser: SamplePetChooser) =
        LinkedProfileReader { LinkedProfileRead.Loaded(chooser.selected.value.profile) }

    private fun TestScope.fakeLinkedSource(link: FakeOwnerLink, chooser: SamplePetChooser) =
        LinkedPetProfileSource(backgroundScope, link, chooser, fakeLinkReader(chooser), sampleBeforeLink = true)

    private suspend fun FakeOwnerLink.linkNow() {
        val code = (start() as LinkStart.Created).code
        claim(code)
        check(code)
    }

    @Test
    fun `오숑이를 고르고 Fake 연결하면 연결 뒤에도 오숑이다`() = runTest {
        val link = FakeOwnerLink()
        val chooser = SamplePetChooser()
        fakeLinkedSource(link, chooser).profile.test {
            assertEquals(PetProfileLoad.Loaded(wanggeon), awaitItem())
            chooser.select("P-DEMO-02")
            assertEquals(PetProfileLoad.Loaded(osyong), awaitItem())
            link.linkNow()
            assertEquals(PetProfileLoad.Loaded(osyong), awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun `Fake 연결 뒤 끊으면 연결 전에 고른 샘플로 돌아온다`() = runTest {
        val link = FakeOwnerLink()
        val chooser = SamplePetChooser()
        chooser.select("P-DEMO-02")
        fakeLinkedSource(link, chooser).profile.test {
            assertEquals(PetProfileLoad.Loaded(osyong), awaitItem())
            link.linkNow()
            assertEquals(PetProfileLoad.Loaded(osyong), awaitItem())
            link.unlink()
            assertEquals(PetProfileLoad.Loaded(osyong), awaitItem())
            expectNoEvents()
        }
        assertEquals(SamplePetProfiles.osyong, chooser.selected.value)
    }

    @Test
    fun `Fake 연결된 동안 고르기를 바꿔도 다시 읽기 전에는 화면 반려견이 그대로다`() = runTest {
        val link = FakeOwnerLink()
        val chooser = SamplePetChooser()
        fakeLinkedSource(link, chooser).profile.test {
            assertEquals(PetProfileLoad.Loaded(wanggeon), awaitItem())
            link.linkNow()
            assertEquals(PetProfileLoad.Loaded(wanggeon), awaitItem())
            chooser.select("P-DEMO-02")
            expectNoEvents()
        }
    }

    @Test
    fun `연결이 없을 때 고르기를 여러 번 바꾸면 바뀔 때마다 화면 반려견이 따라간다`() = runTest {
        val chooser = SamplePetChooser()
        LinkedPetProfileSource(backgroundScope, FakeOwnerLink(), chooser, fakeLinkReader(chooser), sampleBeforeLink = true)
            .profile.test {
                assertEquals(PetProfileLoad.Loaded(wanggeon), awaitItem())
                chooser.selectNext()
                assertEquals(PetProfileLoad.Loaded(osyong), awaitItem())
                chooser.selectNext()
                assertEquals(PetProfileLoad.Loaded(wanggeon), awaitItem())
                chooser.select("P-DEMO-01")
                expectNoEvents()
            }
    }

    @Test
    fun `연결 전에 고르기를 바꾸면 샘플인지 표시는 계속 샘플이다`() = runTest {
        val chooser = SamplePetChooser()
        val source = LinkedPetProfileSource(backgroundScope, FakeOwnerLink(), chooser, fakeLinkReader(chooser), true)
        source.isSample.test {
            assertEquals(true, awaitItem())
            chooser.select("P-DEMO-02")
            assertEquals(true, awaitItem())
        }
    }
}
