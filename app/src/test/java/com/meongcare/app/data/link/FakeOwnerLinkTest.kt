package com.meongcare.app.data.link

import com.meongcare.app.domain.link.LinkCheck
import com.meongcare.app.domain.link.LinkStart
import com.meongcare.app.domain.link.OwnerLinkState
import java.time.Instant
import kotlin.random.Random
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeOwnerLinkTest {

    private val at = Instant.parse("2026-11-06T09:00:00Z")
    private fun link() = FakeOwnerLink(Random(1), now = { at })

    @Test
    fun `코드를 만들면 기다림이고 claim하면 연결됨이다`() = runTest {
        val link = link()
        val created = link.start() as LinkStart.Created
        assertEquals(at, created.createdAt)
        assertEquals(created.code, link.pendingCode.value)
        assertEquals(LinkCheck.Waiting, link.check(created.code))
        assertEquals(OwnerLinkState.Unlinked, link.state.value)

        assertTrue(link.claim())
        assertEquals(LinkCheck.Linked, link.check(created.code))
        assertEquals(OwnerLinkState.Linked(at), link.state.value)
        assertNull(link.pendingCode.value)
    }

    @Test
    fun `연결을 끊으면 연결 전이다`() = runTest {
        val link = link()
        val code = (link.start() as LinkStart.Created).code
        link.claim()
        link.check(code)

        assertTrue(link.unlink())
        assertEquals(OwnerLinkState.Unlinked, link.state.value)
    }

    @Test
    fun `취소한 코드는 claim해도 연결되지 않는다`() = runTest {
        val link = link()
        val code = (link.start() as LinkStart.Created).code
        link.cancel(code)

        assertFalse(link.claim(code))
        assertEquals(LinkCheck.Gone, link.check(code))
        assertEquals(OwnerLinkState.Unlinked, link.state.value)
    }

    @Test
    fun `보호자가 끊으면 끊김을 거쳐 연결 전이 된다`() = runTest {
        val link = link()
        val code = (link.start() as LinkStart.Created).code
        link.claim()
        link.check(code)

        link.markRevoked()
        assertEquals(OwnerLinkState.Revoked, link.state.value)
        link.acknowledgeRevoked()
        assertEquals(OwnerLinkState.Unlinked, link.state.value)
    }

    @Test
    fun `연결 전에는 끊김 표시가 없다`() {
        val link = link()
        link.markRevoked()
        assertEquals(OwnerLinkState.Unlinked, link.state.value)
    }
}
