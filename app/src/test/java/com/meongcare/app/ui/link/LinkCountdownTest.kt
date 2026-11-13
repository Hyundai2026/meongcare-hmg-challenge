package com.meongcare.app.ui.link

import java.time.Duration
import java.time.Instant
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 계획 018 진행 로그 D1: 화면이 1초마다 `linkCountdownOf(createdAt, now)`로 "남은 시간 mm:ss"와 진행 막대 비율을 계산한다.
 * 유효 시간은 10분(`LinkCode.VALIDITY`). 막대 채움 = 남은 비율. 남은 초는 내림한다(KDoc).
 */
class LinkCountdownTest {

    private val created = Instant.parse("2026-11-13T09:00:00Z")

    private fun at(elapsed: Duration) = linkCountdownOf(created, created.plus(elapsed))

    @Test
    fun `만든 직후에는 10분 00초이고 막대가 가득 찬다`() {
        assertEquals(LinkCountdown("10:00", 1f), at(Duration.ZERO))
    }

    @Test
    fun `1초가 지나면 09분 59초다`() {
        assertEquals("09:59", at(Duration.ofSeconds(1)).text)
    }

    @Test
    fun `1초가 안 지났으면 남은 초를 내림해 09분 59초로 보인다`() {
        assertEquals("09:59", at(Duration.ofMillis(1)).text)
    }

    @Test
    fun `7분 0점5초가 지나면 내림해서 02분 59초다`() {
        assertEquals("02:59", at(Duration.ofMillis(7 * 60_000L + 500)).text)
    }

    @Test
    fun `5분이 지나면 05분 00초이고 막대는 절반이다`() {
        val c = at(Duration.ofMinutes(5))
        assertEquals("05:00", c.text)
        assertEquals(0.5f, c.fraction, 1e-6f)
    }

    @Test
    fun `막대 비율은 남은 밀리초 기준으로 매끄럽게 줄어든다`() {
        // 9분 지나 1분 남음 = 0.1, 9분 30초 = 0.05
        assertEquals(0.1f, at(Duration.ofMinutes(9)).fraction, 1e-6f)
        assertEquals(0.05f, at(Duration.ofSeconds(570)).fraction, 1e-6f)
    }

    @Test
    fun `만료 1ms 전에는 00분 00초로 보이지만 막대는 아직 비지 않았다`() {
        val c = at(Duration.ofMillis(600_000 - 1))
        assertEquals("00:00", c.text)
        assertTrue("fraction=${c.fraction}", c.fraction > 0f)
    }

    @Test
    fun `정확히 10분이면 00분 00초이고 막대가 비었다`() {
        assertEquals(LinkCountdown("00:00", 0f), at(Duration.ofMinutes(10)))
    }

    @Test
    fun `10분을 넘겨도 음수가 되지 않고 00분 00초다`() {
        assertEquals(LinkCountdown("00:00", 0f), at(Duration.ofHours(3)))
    }

    @Test
    fun `차 시계가 만든 시각보다 뒤로 가도 10분 00초를 넘지 않는다`() {
        assertEquals(LinkCountdown("10:00", 1f), linkCountdownOf(created, created.minusSeconds(120)))
    }

    @Test
    fun `10분 동안 매초 문구는 mm 콜론 ss 꼴이고 막대는 늘어나지 않는다`() {
        var previous = 1f
        for (s in 0..600L) {
            val c = at(Duration.ofSeconds(s))
            assertTrue("$s 초: ${c.text}", Regex("""\d\d:[0-5]\d""").matches(c.text))
            val left = 600 - s
            assertEquals("%02d:%02d".format(Locale.ROOT, left / 60, left % 60), c.text)
            assertTrue("$s 초: ${c.fraction}", c.fraction in 0f..previous)
            previous = c.fraction
        }
    }

    @Test
    fun `기기 언어가 숫자를 다르게 쓰는 로캘이어도 남은 시간은 아라비아 숫자 mm 콜론 ss다`() {
        val original = Locale.getDefault()
        try {
            for (tag in listOf("fa-IR", "ar-EG", "th-TH-u-nu-thai")) {
                Locale.setDefault(Locale.forLanguageTag(tag))
                assertEquals("로캘 $tag", "02:59", at(Duration.ofSeconds(7 * 60L + 1)).text)
            }
        } finally {
            Locale.setDefault(original)
        }
    }
}
