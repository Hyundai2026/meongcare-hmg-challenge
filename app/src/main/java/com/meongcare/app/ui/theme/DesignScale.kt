package com.meongcare.app.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 화면 크기에 맞춘 비율 (계획 018·019). 디자인 프로토타입은 앱 영역 1682×1210(차량 화면) 기준이라,
 * 더 작은 화면(태블릿·폰)에서는 글자·간격·버튼을 같은 비율로 줄인다. 차량 화면에서는 1.0 (프로토타입과 같은 크기).
 */
class DesignScale(val s: Float) {

    /** 간격·크기를 비율대로 */
    fun gap(d: Dp): Dp = d * s

    /**
     * 글자 크기와 줄 간격을 비율대로. 굵기·자간은 그대로.
     * 지정되지 않은 값은 그대로 두고, 줄 간격이 글자 크기 배수(em)면 배수를 그대로 둔다 (글자 크기를 따라 이미 줄어든다).
     */
    fun style(base: TextStyle): TextStyle {
        if (s == 1f) return base
        return base.copy(
            fontSize = base.fontSize.scaledSp(),
            lineHeight = base.lineHeight.scaledSp(),
        )
    }

    private fun TextUnit.scaledSp(): TextUnit = if (isSp) this * s else this

    /** 버튼 높이: 비율대로 줄이되 터치 타깃이 56dp보다 작아지지 않게 */
    fun touch(d: Dp): Dp = (d * s).coerceAtLeast(MIN_TOUCH)

    companion object {
        /** 프로토타입 앱 영역 폭 */
        val BASE_WIDTH: Dp = 1682.dp

        /** 가장 작게 줄이는 비율 (폰 세로) */
        const val MIN_SCALE = 0.45f

        val MIN_TOUCH: Dp = 56.dp

        /** 폭 비율. [MIN_SCALE]~1.0 */
        fun of(width: Dp): DesignScale = DesignScale((width / BASE_WIDTH).coerceIn(MIN_SCALE, 1f))
    }
}
