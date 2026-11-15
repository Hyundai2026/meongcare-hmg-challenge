package com.meongcare.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 계획 018 "토큰 (TH 표)"와 프로토타입 원본(`docs/product/design/prototype/MeongCareCar.html`의 `TH.light`/`TH.dark`)의 값.
 * 다크 hbg는 원본에서 `TH.dark.bg`로 채운다.
 */
class DesignTokensTest {

    private fun hex(c: Color): String = "#%06X".format(c.value.shr(32).toLong() and 0xFFFFFF)

    private fun table(c: DesignColors) = mapOf(
        "bg" to hex(c.background), "hbg" to hex(c.homeBackground), "sf" to hex(c.surface), "sf2" to hex(c.surface2),
        "ln" to hex(c.line), "tx" to hex(c.text), "sub" to hex(c.subText), "pr" to hex(c.primary),
        "onpr" to hex(c.onPrimary), "sel" to hex(c.selected), "selt" to hex(c.selectedText), "ok" to hex(c.success),
        "okb" to hex(c.successBackground), "warn" to hex(c.warning), "warnb" to hex(c.warningBackground),
        "er" to hex(c.error), "erb" to hex(c.errorBackground), "dis" to hex(c.disabled), "dist" to hex(c.disabledText),
    )

    @Test
    fun `라이트 색은 TH 라이트 표와 같다`() {
        val expected = mapOf(
            "bg" to "#FFFFFF", "hbg" to "#F8F9FB", "sf" to "#F4F5F7", "sf2" to "#ECEEF2", "ln" to "#E8EBF0",
            "tx" to "#1B2231", "sub" to "#657085", "pr" to "#3E5DB4", "onpr" to "#FFFFFF", "sel" to "#EEF2FA",
            "selt" to "#2F4A99", "ok" to "#1B7A4B", "okb" to "#E6F4EC", "warn" to "#8A5A00", "warnb" to "#FFF3DC",
            "er" to "#B42318", "erb" to "#FDEDEC", "dis" to "#EEF0F3", "dist" to "#8A93A3",
        )
        assertEquals(expected, table(LightDesignColors))
    }

    @Test
    fun `다크 색은 TH 다크 표와 같고 hbg는 다크 bg와 같다`() {
        val expected = mapOf(
            "bg" to "#111317", "hbg" to "#111317", "sf" to "#1A1D22", "sf2" to "#24282F", "ln" to "#2A2F37",
            "tx" to "#E8EAEE", "sub" to "#A0A7B4", "pr" to "#8FA8EC", "onpr" to "#0E1422", "sel" to "#1C2538",
            "selt" to "#B9C8F3", "ok" to "#7FD1A4", "okb" to "#15291E", "warn" to "#E9C27A", "warnb" to "#2E2412",
            "er" to "#FF9B90", "erb" to "#3A1A17", "dis" to "#1E1F38", "dist" to "#7E80A0",
        )
        assertEquals(expected, table(DarkDesignColors))
    }

    @Test
    fun `모든 토큰 색은 불투명하다`() {
        for (c in listOf(LightDesignColors, DarkDesignColors)) {
            val all = with(c) {
                listOf(
                    background, homeBackground, surface, surface2, line, text, subText, primary, onPrimary, selected,
                    selectedText, success, successBackground, warning, warningBackground, error, errorBackground,
                    disabled, disabledText,
                )
            }
            all.forEach { assertEquals(1f, it.alpha, 0f) }
        }
    }

    @Test
    fun `간격은 4 8 12 16 24 32 40 48과 큰 여백 64 80 104다`() {
        assertEquals(
            listOf(4, 8, 12, 16, 24, 32, 40, 48, 64, 80, 104).map { it.dp },
            with(DesignSpace) { listOf(xxs, xs, s, m, l, xl, xxl, xxxl, section, page, pageWide) },
        )
    }

    @Test
    fun `둥글기는 기본 16 작은 12 큰 24와 28 알약 999다`() {
        assertEquals(16.dp, DesignRadius.m)
        assertEquals(12.dp, DesignRadius.s)
        assertEquals(24.dp, DesignRadius.l)
        assertEquals(28.dp, DesignRadius.xl)
        assertEquals(999.dp, DesignRadius.pill)
    }

    @Test
    fun `버튼 높이는 96 80 48이다`() {
        assertEquals(96.dp, DesignHeight.buttonLarge)
        assertEquals(80.dp, DesignHeight.buttonMedium)
        assertEquals(48.dp, DesignHeight.buttonSmall)
    }

    @Test
    fun `연결 코드 글자는 160 500 자간 0점1em 줄간 1이다`() {
        assertEquals(160.sp, DesignType.code.fontSize)
        assertEquals(160.sp, DesignType.code.lineHeight)
        assertEquals(FontWeight.Medium, DesignType.code.fontWeight)
        assertEquals(0.1.em, DesignType.code.letterSpacing)
    }

    @Test
    fun `안내문은 34 400 줄간 1점4이고 버튼은 34 600 본문은 30이다`() {
        assertEquals(34.sp, DesignType.bodyLarge.fontSize)
        assertEquals(FontWeight.Normal, DesignType.bodyLarge.fontWeight)
        assertEquals(34f * 1.4f, DesignType.bodyLarge.lineHeight.value, 0.5f)
        assertEquals(34.sp, DesignType.button.fontSize)
        assertEquals(FontWeight.SemiBold, DesignType.button.fontWeight)
        assertEquals(30.sp, DesignType.body.fontSize)
    }

    @Test
    fun `화면 제목은 40 600이고 제목 48 메시지 줄간은 1점3이다`() {
        assertEquals(40.sp, DesignType.title.fontSize)
        assertEquals(FontWeight.SemiBold, DesignType.title.fontWeight)
        assertEquals(48.sp, DesignType.headline.fontSize)
        assertEquals(48f * 1.3f, DesignType.headline.lineHeight.value, 0.5f)
    }

    @Test
    fun `좁은 화면 글자는 크기와 줄간만 같은 비율로 줄고 굵기와 자간은 그대로다`() {
        val n = DesignType.narrow(DesignType.code)
        assertEquals(160f * DesignType.NARROW_SCALE, n.fontSize.value, 1e-3f)
        assertEquals(160f * DesignType.NARROW_SCALE, n.lineHeight.value, 1e-3f)
        assertEquals(DesignType.code.fontWeight, n.fontWeight)
        assertEquals(DesignType.code.letterSpacing, n.letterSpacing)
    }
}
