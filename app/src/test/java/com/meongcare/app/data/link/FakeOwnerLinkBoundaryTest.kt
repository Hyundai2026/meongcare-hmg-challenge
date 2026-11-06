package com.meongcare.app.data.link

import app.cash.turbine.test
import com.meongcare.app.domain.link.LinkCheck
import com.meongcare.app.domain.link.LinkCode
import com.meongcare.app.domain.link.LinkStart
import com.meongcare.app.domain.link.OwnerLinkState
import java.time.Instant
import kotlin.random.Random
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** OwnerLinkService 계약(KDoc)과 계획 014 U2·K5의 경계를 Fake로 */
class FakeOwnerLinkBoundaryTest {

    private var clock = Instant.parse("2026-11-06T09:00:00Z")
    private fun link() = FakeOwnerLink(Random(11), now = { clock })

    private suspend fun FakeOwnerLink.newCode(): LinkCode = (start() as LinkStart.Created).code

    @Test
    fun `새 코드를 받으면 이전 코드는 쓸 수 없다`() = runTest {
        val link = link()
        val first = link.newCode()
        val second = link.newCode()
        assertNotEquals(first, second)
        assertFalse(link.claim(first))
        assertEquals(LinkCheck.Gone, link.check(first))
        assertEquals(LinkCheck.Waiting, link.check(second))
        assertEquals(OwnerLinkState.Unlinked, link.state.value)
    }

    @Test
    fun `기다리는 코드가 없으면 보호자 입력 흉내는 아무 일도 하지 않는다`() = runTest {
        val link = link()
        assertFalse(link.claim())
        assertEquals(OwnerLinkState.Unlinked, link.state.value)
    }

    @Test
    fun `다른 코드를 물으면 claim된 코드가 있어도 연결되지 않는다`() = runTest {
        val link = link()
        val code = link.newCode()
        link.claim(code)
        val other = LinkCode.of(if (code.value == "222222") "333333" else "222222")!!
        assertEquals(LinkCheck.Gone, link.check(other))
        assertEquals(OwnerLinkState.Unlinked, link.state.value)
        assertEquals(LinkCheck.Linked, link.check(code))
    }

    @Test
    fun `연결된 뒤 같은 코드를 다시 물으면 코드 없음이고 연결은 그대로다`() = runTest {
        val link = link()
        val code = link.newCode()
        link.claim(code)
        assertEquals(LinkCheck.Linked, link.check(code))
        val linked = link.state.value
        assertEquals(LinkCheck.Gone, link.check(code))
        assertEquals(linked, link.state.value)
    }

    @Test
    fun `연결 전에 끊어도 던지지 않고 연결 전이다`() = runTest {
        val link = link()
        assertTrue(link.unlink())
        assertEquals(OwnerLinkState.Unlinked, link.state.value)
    }

    @Test
    fun `보호자가 끊었다는 알림 전에 차에서 끊어도 연결 전이다`() = runTest {
        val link = link()
        val code = link.newCode()
        link.claim(code)
        link.check(code)
        link.markRevoked()
        link.unlink()
        assertEquals(OwnerLinkState.Unlinked, link.state.value)
    }

    @Test
    fun `연결된 동안에는 끊김 확인을 눌러도 연결이 풀리지 않는다`() = runTest {
        val link = link()
        val code = link.newCode()
        link.claim(code)
        link.check(code)
        link.acknowledgeRevoked()
        assertTrue(link.state.value is OwnerLinkState.Linked)
    }

    @Test
    fun `연결된 차가 새로 연결하면 새로 연결한 시각으로 연결됨이다`() = runTest {
        val link = link()
        val first = link.newCode()
        link.claim(first)
        link.check(first)
        assertEquals(OwnerLinkState.Linked(clock), link.state.value)

        // 새 코드를 받는 동안에도 이전 연결은 그대로다 (아직 새 보호자가 입력하지 않았다)
        clock = clock.plusSeconds(120)
        val second = link.newCode()
        assertEquals(OwnerLinkState.Linked(Instant.parse("2026-11-06T09:00:00Z")), link.state.value)
        link.claim(second)
        assertEquals(LinkCheck.Linked, link.check(second))
        assertEquals(OwnerLinkState.Linked(clock), link.state.value)
    }

    @Test
    fun `연결 상태 흐름은 연결 전에서 시작해 바뀔 때마다 알린다`() = runTest {
        val link = link()
        link.state.test {
            assertEquals(OwnerLinkState.Unlinked, awaitItem())
            val code = link.newCode()
            expectNoEvents()
            link.claim(code)
            link.check(code)
            assertEquals(OwnerLinkState.Linked(clock), awaitItem())
            link.markRevoked()
            assertEquals(OwnerLinkState.Revoked, awaitItem())
            link.acknowledgeRevoked()
            assertEquals(OwnerLinkState.Unlinked, awaitItem())
        }
    }

    @Test
    fun `연결되면 debug 패널의 기다리는 코드가 사라진다`() = runTest {
        val link = link()
        val code = link.newCode()
        assertEquals(code, link.pendingCode.value)
        link.claim(code)
        link.check(code)
        assertNull(link.pendingCode.value)
        assertFalse(link.claim())
    }
}
