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
import androidx.compose.ui.draw.clip
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
 * 지금 보일 샘플 반려견 사진 (021). `MainActivity`가 반려견 정보가 시연용 샘플일 때만 고른 반려견의 사진
 * ([samplePetPhotoRes])을 넣는다. null이면 사진 없이 이름 첫 글자다 (보호자 웹에서 연결한 반려견).
 */
val LocalSamplePetPhoto = androidx.compose.runtime.compositionLocalOf<Int?> { null }

/** 샘플 반려견 id(프로토타입 P-DEMO-01/02)의 사진. 모르는 id면 null (021) */
@androidx.annotation.DrawableRes
fun samplePetPhotoRes(id: String): Int? = when (id) {
    "P-DEMO-01" -> com.meongcare.app.R.drawable.sample_pet_wanggeon
    "P-DEMO-02" -> com.meongcare.app.R.drawable.sample_pet_osyong
    else -> null
}

/**
 * 반려견 아바타. 샘플 반려견이면 그 사진(둥근 원, 디자인 시안), 아니면 이름 첫 글자를 그린다.
 * 연결한 반려견의 사진(`PetProfile.photoDataUrl`)은 아직 그리지 않는다.
 *
 * @param initial null이거나 비면 발바닥을 그린다 (반려견 정보가 없을 때)
 */
@Composable
fun PetAvatar(initial: String?, size: Dp, modifier: Modifier = Modifier) {
    val photo = LocalSamplePetPhoto.current
    if (photo != null && !initial.isNullOrEmpty()) {
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(photo),
            contentDescription = androidx.compose.ui.res.stringResource(com.meongcare.app.R.string.pet_photo_description),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            modifier = modifier.size(size).clip(androidx.compose.foundation.shape.CircleShape),
        )
        return
    }
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
