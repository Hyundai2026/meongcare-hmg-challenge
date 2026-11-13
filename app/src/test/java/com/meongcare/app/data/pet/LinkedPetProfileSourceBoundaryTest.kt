package com.meongcare.app.data.pet

import app.cash.turbine.test
import com.meongcare.app.data.link.FakeOwnerLink
import com.meongcare.app.domain.link.LinkStart
import com.meongcare.app.domain.link.OwnerLinkState
import com.meongcare.app.domain.pet.PetProfileLoad
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * 계획 014 설계 5절·K2·K4, docs/data/owner-link.md "차가 읽을 때"의 경계.
 * - 연결된 뒤에는 샘플 "보리"로 대신하지 않는다 (틀린 의료정보를 보이지 않는다)
 * - 구독이 없으면 읽지 않는다 (폴링 최소화), 다시 구독하면 다시 읽는다 (KDoc)
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LinkedPetProfileSourceBoundaryTest {

    private val bori = PetProfileLoad.Loaded(SamplePetProfiles.demoPet)
    private val kongiProfile = SamplePetProfiles.demoPet.copy(name = "콩이", breed = "말티즈")
    private val kongi = PetProfileLoad.Loaded(kongiProfile)

    /** 차례로 돌려줄 읽기 결과. 다 쓰면 마지막 값을 계속 준다 */
    private class ScriptedReader(vararg results: LinkedProfileRead) : LinkedProfileReader {
        private val queue = ArrayDeque(results.toList())
        var reads = 0
        override suspend fun read(): LinkedProfileRead {
            reads++
            return if (queue.size > 1) queue.removeFirst() else queue.first()
        }
    }

    /** 서버 응답을 테스트가 정할 때까지 기다리는 읽기 (느린 네트워크) */
    private class GatedReader : LinkedProfileReader {
        var reads = 0
        val answer = CompletableDeferred<LinkedProfileRead>()
        override suspend fun read(): LinkedProfileRead {
            reads++
            return answer.await()
        }
    }

    private fun TestScope.source(link: FakeOwnerLink, reader: LinkedProfileReader, sampleBeforeLink: Boolean = true) =
        LinkedPetProfileSource(backgroundScope, link, FakePetProfileSource(), reader, sampleBeforeLink)

    private suspend fun FakeOwnerLink.linkNow() {
        val code = (start() as LinkStart.Created).code
        claim(code)
        check(code)
    }

    /** 화면 하나가 구독한 것처럼 받은 값을 모은다 */
    private fun TestScope.watch(source: LinkedPetProfileSource): MutableList<PetProfileLoad> {
        val seen = mutableListOf<PetProfileLoad>()
        backgroundScope.launch { source.profile.collect { seen += it } }
        return seen
    }

    @Test
    fun `연결된 뒤 서버 응답을 기다리는 동안 새로 보는 화면에 샘플 보리를 보이지 않는다`() = runTest {
        val link = FakeOwnerLink()
        val reader = GatedReader()
        val source = source(link, reader)
        val home = watch(source)
        runCurrent()
        assertEquals(listOf(bori), home)

        link.linkNow()
        runCurrent()
        assertEquals(1, reader.reads)

        // 연결됨 화면·진료 카드처럼 연결 뒤에 새로 구독하는 화면
        val linkedScreen = watch(source)
        runCurrent()
        assertFalse("연결된 뒤 보리가 보였다: $linkedScreen", bori in linkedScreen)

        reader.answer.complete(LinkedProfileRead.Loaded(kongiProfile))
        runCurrent()
        assertEquals(listOf(kongi), linkedScreen)
    }

    @Test
    fun `구독이 멈춘 사이 연결되면 다시 구독할 때 샘플 보리를 먼저 보이지 않는다`() = runTest {
        val link = FakeOwnerLink()
        val reader = GatedReader()
        val source = source(link, reader)
        source.profile.test { assertEquals(bori, awaitItem()) }
        advanceTimeBy(10_000) // 구독이 없어 멈춘다
        runCurrent()

        link.linkNow()
        val seen = watch(source)
        runCurrent()
        assertFalse("연결된 뒤 보리가 보였다: $seen", bori in seen)
        reader.answer.complete(LinkedProfileRead.Loaded(kongiProfile))
        runCurrent()
        assertEquals(kongi, seen.last())
    }

    @Test
    fun `연결 전에는 다시 읽으라고 해도 서버를 읽지 않는다`() = runTest {
        val reader = ScriptedReader(LinkedProfileRead.Loaded(kongiProfile))
        val source = source(FakeOwnerLink(), reader)
        val seen = watch(source)
        runCurrent()
        source.refresh()
        runCurrent()
        assertEquals(0, reader.reads)
        assertEquals(listOf(bori), seen)
    }

    @Test
    fun `구독이 없으면 다시 읽으라고 해도 서버를 읽지 않고 다시 구독하면 다시 읽는다`() = runTest {
        val link = FakeOwnerLink()
        link.linkNow()
        val reader = ScriptedReader(LinkedProfileRead.Loaded(kongiProfile))
        val source = source(link, reader)
        val home = backgroundScope.launch { source.profile.collect {} }
        runCurrent()
        assertEquals(1, reader.reads)

        home.cancel() // 주행 화면·백그라운드: 구독 없음
        advanceTimeBy(10_000)
        runCurrent()
        source.refresh()
        runCurrent()
        assertEquals(1, reader.reads)

        val seen = watch(source)
        runCurrent()
        assertEquals(2, reader.reads)
        assertEquals(kongi, seen.last())
    }

    @Test
    fun `연결 뒤 읽기에 실패하는 동안 샘플을 한 번도 내보내지 않고 다시 읽어 되면 서버 프로필이다`() = runTest {
        val link = FakeOwnerLink()
        link.linkNow()
        val reader = ScriptedReader(LinkedProfileRead.Unavailable, LinkedProfileRead.Unavailable, LinkedProfileRead.Loaded(kongiProfile))
        val source = source(link, reader)
        val seen = watch(source)
        runCurrent()
        source.refresh()
        runCurrent()
        source.refresh()
        runCurrent()
        assertFalse("$seen", bori in seen)
        assertEquals(listOf(PetProfileLoad.Unavailable, PetProfileLoad.Unavailable, kongi), seen)
    }

    @Test
    fun `연결을 끊었다가 다시 연결하면 서버를 다시 읽는다`() = runTest {
        val link = FakeOwnerLink()
        val reader = ScriptedReader(LinkedProfileRead.Loaded(kongiProfile))
        val source = source(link, reader)
        val seen = watch(source)
        runCurrent()
        link.linkNow()
        runCurrent()
        link.unlink()
        runCurrent()
        link.linkNow()
        runCurrent()
        assertEquals(2, reader.reads)
        assertEquals(kongi, seen.last())
    }

    @Test
    fun `Real에서 보호자가 끊으면 샘플이 아니라 등록 안 됨이다`() = runTest {
        val link = FakeOwnerLink()
        link.linkNow()
        val source = source(link, ScriptedReader(LinkedProfileRead.Loaded(kongiProfile), LinkedProfileRead.Revoked), sampleBeforeLink = false)
        val seen = watch(source)
        runCurrent()
        source.refresh()
        runCurrent()
        assertEquals(OwnerLinkState.Revoked, link.state.value)
        assertEquals(PetProfileLoad.NotRegistered, seen.last())
        assertFalse("$seen", bori in seen)
    }

    @Test
    fun `보호자가 끊은 뒤에는 다시 읽으라고 해도 서버를 읽지 않는다`() = runTest {
        val link = FakeOwnerLink()
        link.linkNow()
        val reader = ScriptedReader(LinkedProfileRead.Revoked)
        val source = source(link, reader)
        val seen = watch(source)
        runCurrent()
        assertEquals(OwnerLinkState.Revoked, link.state.value)
        assertEquals(1, reader.reads)
        source.refresh()
        runCurrent()
        link.acknowledgeRevoked()
        source.refresh()
        runCurrent()
        assertEquals(1, reader.reads)
        assertEquals(bori, seen.last())
    }

    @Test
    fun `보호자가 프로필을 지웠다가 다시 등록하면 다시 읽을 때 서버 프로필이다`() = runTest {
        val link = FakeOwnerLink()
        link.linkNow()
        val source = source(link, ScriptedReader(LinkedProfileRead.NotRegistered, LinkedProfileRead.Loaded(kongiProfile)))
        val seen = watch(source)
        runCurrent()
        source.refresh()
        runCurrent()
        assertEquals(listOf(PetProfileLoad.NotRegistered, kongi), seen)
        assertEquals(OwnerLinkState.Linked::class, link.state.value::class)
    }
}
