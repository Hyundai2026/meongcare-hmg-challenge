package com.meongcare.app.ui.guidance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 움직이지 않는 진행 막대 (013 U4). 정차 화면의 안내 영역과 주행 화면이 같이 쓴다.
 *
 * 주행 중 화면에 쓰므로 애니메이션을 넣지 않는다: 값이 바뀌면 그 값으로 바로 다시 그린다 (AGENTS.md 규칙 3,
 * ux-guidelines "움직이는 이미지 최소화"). Material 진행 표시는 값 변화를 애니메이션하므로 쓰지 않고 상자 두 개로 그린다.
 *
 * @param progress 0.0 ~ 1.0 (밖의 값은 자른다)
 */
@Composable
fun StaticProgressBar(
    progress: Float,
    track: Color,
    fill: Color,
    modifier: Modifier = Modifier,
    height: Dp = 16.dp,
) {
    val value = progress.coerceIn(0f, 1f)
    val shape = RoundedCornerShape(height / 2)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(track)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(value, 0f..1f) },
    ) {
        if (value > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(value)
                    .fillMaxHeight()
                    .clip(shape)
                    .background(fill),
            )
        }
    }
}
