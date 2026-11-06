package com.meongcare.app.domain.link

import java.time.Duration
import java.time.Instant
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** docs/data/owner-link.md "연결 코드" */
class LinkCodeTest {

    @Test
    fun `코드는 헷갈리는 글자 없이 6자다`() {
        val random = Random(42)
        repeat(500) {
            val code = LinkCode.generate(random).value
            assertEquals(6, code.length)
            assertTrue(code, code.none { it in "01ILO" })
            assertTrue(code, code.all { it in LinkCode.ALPHABET })
        }
        assertEquals(31, LinkCode.ALPHABET.length)
        assertEquals(31, LinkCode.ALPHABET.toSet().size)
    }

    @Test
    fun `같은 난수면 같은 코드다`() {
        assertEquals(LinkCode.generate(Random(7)), LinkCode.generate(Random(7)))
    }

    @Test
    fun `화면에는 세 글자씩 띄어 보인다`() {
        assertEquals("K7P 3QX", LinkCode.of("K7P3QX")!!.display)
    }

    @Test
    fun `형식이 틀린 코드는 받지 않는다`() {
        assertNotNull(LinkCode.of("K7P3QX"))
        assertNull(LinkCode.of("k7p3qx"))
        assertNull(LinkCode.of("K7P 3QX"))
        assertNull(LinkCode.of("K7P3Q"))
        assertNull(LinkCode.of("K7P3QXA"))
        assertNull(LinkCode.of("K7P3Q0"))
        assertNull(LinkCode.of("K7P3QI"))
        assertNull(LinkCode.of(""))
    }

    @Test
    fun `남은 시간은 만든 시각에서 10분이고 0 아래로 내려가지 않는다`() {
        val created = Instant.parse("2026-11-06T09:00:00Z")
        assertEquals(Duration.ofMinutes(10), remainingOf(created, created))
        assertEquals(Duration.ofMinutes(4), remainingOf(created, created.plusSeconds(360)))
        assertEquals(Duration.ZERO, remainingOf(created, created.plusSeconds(600)))
        assertEquals(Duration.ZERO, remainingOf(created, created.plusSeconds(3600)))
        assertFalse(isExpired(created, created.plusSeconds(599)))
        assertTrue(isExpired(created, created.plusSeconds(600)))
    }

    @Test
    fun `남은 시간은 분 단위로 올림한다`() {
        val created = Instant.parse("2026-11-06T09:00:00Z")
        assertEquals(10, remainingMinutesOf(created, created))
        assertEquals(10, remainingMinutesOf(created, created.plusSeconds(1)))
        assertEquals(9, remainingMinutesOf(created, created.plusSeconds(60)))
        assertEquals(1, remainingMinutesOf(created, created.plusSeconds(599)))
        assertEquals(0, remainingMinutesOf(created, created.plusSeconds(600)))
    }

    @Test
    fun `로그에 찍혀도 코드가 보이지 않는다`() {
        assertFalse(LinkCode.of("K7P3QX").toString().contains("K7P3QX"))
    }
}
