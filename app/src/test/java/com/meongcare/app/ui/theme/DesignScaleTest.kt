package com.meongcare.app.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Test

/** 계획 019 단계 1: 프로토타입 폭 1682dp 기준 비율. */
class DesignScaleTest {

    @Test
    fun `폭 비율은 1682 기준이고 0점45~1 사이다`() {
        assertEquals(1f, DesignScale.of(1682.dp).s, 0.0001f)
        assertEquals(0.5f, DesignScale.of(841.dp).s, 0.0001f)
        assertEquals(0.45f, DesignScale.of(400.dp).s, 0.0001f)
        assertEquals(1f, DesignScale.of(2400.dp).s, 0.0001f)
    }

    @Test
    fun `간격과 글자는 비율대로 줄고 터치 높이는 56 아래로 내려가지 않는다`() {
        val half = DesignScale(0.5f)
        assertEquals(48.dp, half.gap(96.dp))
        assertEquals(15.sp, half.style(TextStyle(fontSize = 30.sp, lineHeight = 42.sp)).fontSize)
        assertEquals(21.sp, half.style(TextStyle(fontSize = 30.sp, lineHeight = 42.sp)).lineHeight)
        assertEquals(56.dp, half.touch(96.dp))
        assertEquals(96.dp, DesignScale(1f).touch(96.dp))
    }
}
