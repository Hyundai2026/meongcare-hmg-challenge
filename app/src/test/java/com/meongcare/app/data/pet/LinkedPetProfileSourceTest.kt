package com.meongcare.app.data.pet

import app.cash.turbine.test
import com.meongcare.app.data.link.FakeOwnerLink
import com.meongcare.app.domain.link.LinkStart
import com.meongcare.app.domain.link.OwnerLinkState
import com.meongcare.app.domain.pet.PetProfileLoad
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** 계획 014 설계 5절, docs/data/owner-link.md "차가 읽을 때" */
class LinkedPetProfileSourceTest {

    private val kongi = SamplePetProfiles.demoPet.copy(name = "콩이", breed = "말티즈")

    /** 차례로 돌려줄 읽기 결과. 다 쓰면 마지막 값을 계속 준다 */
    private class ScriptedReader(vararg results: LinkedProfileRead) : LinkedProfileReader {
        private val queue = ArrayDeque(results.toList())
        var reads = 0
        override suspend fun read(): LinkedProfileRead {
            reads++
            return if (queue.size > 1) queue.removeFirst() else queue.first()
        }
    }

    private fun TestScope.source(link: FakeOwnerLink, reader: LinkedProfileReader, sampleBeforeLink: Boolean = true) =
        LinkedPetProfileSource(backgroundScope, link, FakePetProfileSource(), reader, sampleBeforeLink)

    private suspend fun FakeOwnerLink.linkNow() {
        val code = (start() as LinkStart.Created).code
        claim(code)
        check(code)
    }

    @Test
    fun `연결 전에는 샘플을 낸다`() = runTest {
        val reader = ScriptedReader(LinkedProfileRead.Loaded(kongi))
        source(FakeOwnerLink(), reader).profile.test {
            assertEquals(PetProfileLoad.Loaded(SamplePetProfiles.demoPet), awaitItem())
        }
        assertEquals(0, reader.reads)
    }

    @Test
    fun `연결되면 서버 프로필을 낸다`() = runTest {
        val link = FakeOwnerLink()
        source(link, ScriptedReader(LinkedProfileRead.Loaded(kongi))).profile.test {
            assertEquals(PetProfileLoad.Loaded(SamplePetProfiles.demoPet), awaitItem())
            link.linkNow()
            assertEquals(PetProfileLoad.Loaded(kongi), awaitItem())
        }
    }

    @Test
    fun `연결 뒤 서버를 못 읽으면 샘플이 아니라 읽지 못함이다`() = runTest {
        val link = FakeOwnerLink()
        link.linkNow()
        source(link, ScriptedReader(LinkedProfileRead.Unavailable)).profile.test {
            assertEquals(PetProfileLoad.Unavailable, awaitItem())
        }
    }

    @Test
    fun `다시 읽으라고 하면 다시 읽는다`() = runTest {
        val link = FakeOwnerLink()
        link.linkNow()
        val changed = kongi.copy(medical = kongi.medical.copy(allergies = listOf("닭고기")))
        val reader = ScriptedReader(LinkedProfileRead.Loaded(kongi), LinkedProfileRead.Loaded(changed))
        val source = source(link, reader)
        source.profile.test {
            assertEquals(PetProfileLoad.Loaded(kongi), awaitItem())
            source.refresh()
            assertEquals(PetProfileLoad.Loaded(changed), awaitItem())
        }
        assertEquals(2, reader.reads)
    }

    @Test
    fun `보호자가 끊으면 연결 끊김을 한 번 알리고 샘플로 돌아간다`() = runTest {
        val link = FakeOwnerLink()
        link.linkNow()
        val source = source(link, ScriptedReader(LinkedProfileRead.Loaded(kongi), LinkedProfileRead.Revoked))
        source.profile.test {
            assertEquals(PetProfileLoad.Loaded(kongi), awaitItem())
            source.refresh()
            assertEquals(PetProfileLoad.Loaded(SamplePetProfiles.demoPet), awaitItem())
            assertEquals(OwnerLinkState.Revoked, link.state.value)
            link.acknowledgeRevoked()
            assertEquals(OwnerLinkState.Unlinked, link.state.value)
            expectNoEvents()
        }
    }

    @Test
    fun `보호자가 프로필을 지웠으면 등록 안 됨이다`() = runTest {
        val link = FakeOwnerLink()
        link.linkNow()
        source(link, ScriptedReader(LinkedProfileRead.NotRegistered)).profile.test {
            assertEquals(PetProfileLoad.NotRegistered, awaitItem())
        }
        assertEquals(OwnerLinkState.Linked::class, link.state.value::class)
    }

    @Test
    fun `Real은 연결 전 등록 안 됨이다`() = runTest {
        source(FakeOwnerLink(), ScriptedReader(LinkedProfileRead.Loaded(kongi)), sampleBeforeLink = false).profile.test {
            assertEquals(PetProfileLoad.NotRegistered, awaitItem())
        }
    }

    @Test
    fun `연결을 끊으면 샘플로 돌아간다`() = runTest {
        val link = FakeOwnerLink()
        link.linkNow()
        source(link, ScriptedReader(LinkedProfileRead.Loaded(kongi))).profile.test {
            assertEquals(PetProfileLoad.Loaded(kongi), awaitItem())
            link.unlink()
            assertEquals(PetProfileLoad.Loaded(SamplePetProfiles.demoPet), awaitItem())
        }
    }
}
