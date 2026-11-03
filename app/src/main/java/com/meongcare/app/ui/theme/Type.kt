package com.meongcare.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * 글자 크기.
 *
 * 확인된 기준 (docs/pleos/ux-guidelines.md): 최소 12sp, 주요 정보는 글자 높이 5mm 이상.
 * 아래 값은 임시값이다: Pleos 타입 스케일 수치는 아직 확인하지 못했다.
 * 폰트는 시스템 기본을 쓴다 (Pleos에는 Asta Sans가 시스템 폰트로 깔려 있다).
 */
internal val MeongCareTypography = Typography(
    // 주행 화면의 상태 한 줄처럼, 한눈에 읽어야 하는 글자
    displayLarge = TextStyle(fontSize = 48.sp, lineHeight = 56.sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontSize = 30.sp, lineHeight = 38.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 22.sp, lineHeight = 30.sp),
    bodyMedium = TextStyle(fontSize = 18.sp, lineHeight = 26.sp),
    labelLarge = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp),
)
