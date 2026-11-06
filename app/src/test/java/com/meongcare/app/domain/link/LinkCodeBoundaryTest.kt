package com.meongcare.app.domain.link

import java.time.Duration
import java.time.Instant
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** docs/data/owner-link.md "연결 코드"의 경계 (계획 014 단계 12: 만료 정확히 10분, 정규화 경계) */
class LinkCodeBoundaryTest {

    private val created = Instant.parse("2026-11-06T09:00:00Z")

    @Test
    fun `만료 1ms 전에는 만료가 아니고 1분 안에다`() {
        val at = created.plus(Duration.ofMinutes(10)).minusMillis(1)
        assertFalse(isExpired(created, at))
        assertEquals(Duration.ofMillis(1), remainingOf(created, at))
        assertEquals(1, remainingMinutesOf(created, at))
    }

    @Test
    fun `정확히 10분이 된 순간부터 만료다`() {
        val at = created.plus(Duration.ofMinutes(10))
        assertTrue(isExpired(created, at))
        assertEquals(Duration.ZERO, remainingOf(created, at))
        assertEquals(0, remainingMinutesOf(created, at))
    }

    @Test
    fun `정확히 N분 남으면 N분이고 1ms라도 더 남으면 N+1분이다`() {
        for (left in 1L..10L) {
            val exact = created.plus(Duration.ofMinutes(10 - left))
            assertEquals("정확히 ${left}분", left.toInt(), remainingMinutesOf(created, exact))
            if (left < 10) {
                assertEquals("${left}분 + 1ms", left.toInt() + 1, remainingMinutesOf(created, exact.minusMillis(1)))
            }
        }
    }

    @Test
    fun `차 시계가 만든 시각보다 뒤로 가도 남은 시간은 10분을 넘지 않는다`() {
        // 코드는 10분만 유효하다 (서버 규칙). 차 시계가 뒤로 맞춰져도 "11분 안에"처럼 보이면 안 된다
        val earlier = created.minusSeconds(90)
        assertFalse(isExpired(created, earlier))
        val left = remainingOf(created, earlier)
        assertTrue("남은 시간 $left", left <= LinkCode.VALIDITY)
        val minutes = remainingMinutesOf(created, earlier)
        assertTrue("${minutes}분 안에", minutes <= 10)
    }

    @Test
    fun `만드는 코드는 31종 글자를 모두 쓴다`() {
        val random = Random(2026)
        val seen = mutableSetOf<Char>()
        repeat(2_000) { seen += LinkCode.generate(random).value.toSet() }
        assertEquals(LinkCode.ALPHABET.toSet(), seen)
    }

    @Test
    fun `글자 집합의 모든 글자로 된 코드는 받는다`() {
        for (c in LinkCode.ALPHABET) {
            val text = c.toString().repeat(LinkCode.LENGTH)
            assertEquals(text, LinkCode.of(text)?.value)
        }
    }

    @Test
    fun `글자 집합 밖의 글자가 하나라도 있으면 받지 않는다`() {
        val outside = ('0'..'9') + ('A'..'Z') + ('a'..'z') - LinkCode.ALPHABET.toSet()
        for (c in outside) assertNull("$c", LinkCode.of("K7P3Q$c"))
        // 전각 글자, 하이픈, 공백이 섞인 것도 차는 받지 않는다 (차는 자기가 만든 코드만 다룬다)
        assertNull(LinkCode.of("Ｋ７Ｐ３ＱＸ"))
        assertNull(LinkCode.of("K7P-3QX"))
        assertNull(LinkCode.of("K7P3QX "))
    }

    @Test
    fun `화면 표시는 앞 세 글자와 뒤 세 글자 사이에 공백 하나다`() {
        val code = LinkCode.generate(Random(9))
        val display = code.display
        assertEquals(7, display.length)
        assertEquals(' ', display[3])
        assertEquals(code.value, display.replace(" ", ""))
    }
}
