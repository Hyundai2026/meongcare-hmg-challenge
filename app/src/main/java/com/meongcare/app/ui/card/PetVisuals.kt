package com.meongcare.app.ui.card

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meongcare.app.ui.theme.CardColors

/** 발바닥 모양. 보호자 웹 PawIcon(web/src/ui/PawIcon.tsx)과 같은 그림이다 (팀이 그린 것, 라이선스 문제 없음). */
val PawIcon: ImageVector by lazy {
    fun ellipse(cx: Float, cy: Float, rx: Float, ry: Float) =
        "M${cx - rx},$cy a$rx,$ry 0 1,0 ${2 * rx},0 a$rx,$ry 0 1,0 ${-2 * rx},0Z"
    val paths = listOf(
        ellipse(7.5f, 13.5f, 3f, 3.8f),
        ellipse(12.8f, 7.8f, 3f, 3.8f),
        ellipse(19.2f, 7.8f, 3f, 3.8f),
        ellipse(24.5f, 13.5f, 3f, 3.8f),
        "M16 14.5c-4.6 0-9 5.3-9 9C7 26.2 9.2 27 11.3 27c2 0 3-.9 4.7-.9s2.7.9 4.7.9c2.1 0 4.3-.8 4.3-3.5 0-3.7-4.4-9-9-9Z",
    )
    ImageVector.Builder("Paw", 32.dp, 32.dp, 32f, 32f).apply {
        paths.forEach { addPath(PathParser().parsePathString(it).toNodes(), fill = SolidColor(Color.Black)) }
    }.build()
}

/**
 * 반려견 아바타. 지금은 이름 첫 글자를 그린다 (사진은 저작권 때문에 아직 쓰지 않는다).
 * 나중에 사진을 보여 줄 때는 이 자리에 사진을 그린다 (`PetProfile.photoDataUrl`).
 *
 * @param initial null이거나 비면 발바닥을 그린다 (반려견 정보가 없을 때)
 */
@Composable
fun PetAvatar(initial: String?, size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .background(CardColors.AvatarBackground, RoundedCornerShape(size * 0.14f)),
        contentAlignment = Alignment.Center,
    ) {
        if (initial.isNullOrEmpty()) {
            Icon(PawIcon, contentDescription = null, tint = CardColors.Accent, modifier = Modifier.size(size * 0.45f))
        } else {
            Text(
                text = initial,
                color = CardColors.Accent,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.42f).sp,
            )
        }
    }
}
