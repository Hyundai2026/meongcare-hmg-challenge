package com.meongcare.app.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

/** 계획 019 단계 1 `DesignScale` 경계: 폭 끝값, 터치 하한, 글자 스타일의 빈 값·상대 단위. */
class DesignScaleBoundaryTest {

    @Test
    fun `폭이 0이거나 무한대여도 비율은 0점45~1 안이다`() {
        assertEquals(0.45f, DesignScale.of(0.dp).s, 0.0001f)
        assertEquals(1f, DesignScale.of(Dp.Infinity).s, 0.0001f)
    }

    @Test
    fun `가장 작게 줄이는 경계값 근처에서 비율이 이어진다`() {
        // 0.45 × 1682 = 756.9
        assertEquals(0.45f, DesignScale.of(756.dp).s, 0.0001f)
        assertEquals(800f / 1682f, DesignScale.of(800.dp).s, 0.0001f)
        assertEquals(1681f / 1682f, DesignScale.of(1681.dp).s, 0.0001f)
    }

    @Test
    fun `비율이 1이어도 56dp보다 작은 버튼은 56dp가 된다`() {
        assertEquals(56.dp, DesignScale(1f).touch(40.dp))
        assertEquals(56.dp, DesignScale(1f).touch(56.dp))
    }

    @Test
    fun `가장 작은 비율에서 프로토타입 버튼 96은 56dp로 올라가고 간격은 그대로 줄어든다`() {
        val min = DesignScale.of(400.dp)
        assertEquals(56.dp, min.touch(96.dp))
        assertEquals(43.2f, min.gap(96.dp).value, 0.001f)
        assertEquals(0.dp, min.gap(0.dp))
    }

    @Test
    fun `비율이 1이면 스타일을 그대로 돌려준다`() {
        val base = TextStyle(fontSize = 30.sp, lineHeight = 42.sp)
        assertSame(base, DesignScale(1f).style(base))
    }

    @Test
    fun `글자를 줄여도 굵기와 자간은 그대로다`() {
        val base = TextStyle(fontSize = 104.sp, lineHeight = 112.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.03).em)
        val scaled = DesignScale(0.5f).style(base)
        assertEquals(FontWeight.Bold, scaled.fontWeight)
        assertEquals((-0.03).em, scaled.letterSpacing)
        assertEquals(52.sp, scaled.fontSize)
        assertEquals(56.sp, scaled.lineHeight)
    }

    @Test
    fun `줄 간격이 지정되지 않은 스타일도 줄일 수 있고 줄 간격은 지정되지 않은 채로 남는다`() {
        val scaled = DesignScale(0.5f).style(TextStyle(fontSize = 30.sp))
        assertEquals(15.sp, scaled.fontSize)
        assertEquals(TextUnit.Unspecified, scaled.lineHeight)
    }

    @Test
    fun `줄 간격이 글자 크기 배수(em)면 배수는 그대로라 실제 줄 간격이 글자와 같은 비율로 준다`() {
        // 30sp × 1.4em = 42sp → 절반이면 15sp × 1.4em = 21sp. em에 비율을 또 곱하면 0.7em(10.5sp)이 된다.
        val scaled = DesignScale(0.5f).style(TextStyle(fontSize = 30.sp, lineHeight = 1.4.em))
        assertEquals(15.sp, scaled.fontSize)
        assertEquals(1.4.em, scaled.lineHeight)
    }
}
