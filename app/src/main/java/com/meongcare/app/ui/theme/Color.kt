package com.meongcare.app.ui.theme

import androidx.compose.ui.graphics.Color

// 임시 색상. Pleos 디자인 에셋(Figma)을 반영하기 전까지 쓴다.
// 기준: 글자와 배경의 대비 4.5:1 이상 (docs/pleos/ux-guidelines.md)

internal val Navy900 = Color(0xFF0F1E33)
internal val Navy700 = Color(0xFF1E3A5F)
internal val Sky300 = Color(0xFF7FB8F0)
internal val Sky100 = Color(0xFFD6E9FB)
internal val Amber500 = Color(0xFFF2A900)
internal val Gray50 = Color(0xFFF7F8FA)
internal val Gray900 = Color(0xFF121417)
internal val Gray200 = Color(0xFFE1E4E8)
internal val Gray800 = Color(0xFF2A2E33)
internal val Red600 = Color(0xFFC62828)
internal val Red300 = Color(0xFFEF9A9A)

// 다크 테마 배경: 컨셉 그림의 남색 (009). Gray50 글자와 대비 약 16:1
internal val Navy950 = Color(0xFF0A1630)
internal val Navy850 = Color(0xFF14284D)

/** 채움 버튼 바탕 ([길 안내 시작] 등). 흰 글자와 대비 약 5.7:1 */
val ActionBlue = Color(0xFF2F5BE0)
val ActionBluePressed = Color(0xFF2449B8)

/**
 * 진료 카드 몸통의 고정색 (009). 라이트·다크와 상관없이 흰 바탕 + 어두운 글자로 둔다.
 * Pleos 디자인 가이드의 Static Color(테마에 영향을 받지 않는 색)와 같은 생각이다.
 */
object CardColors {
    val Surface = Color(0xFFFFFFFF)
    /** 본문 글자. 흰 바탕과 대비 약 15:1 */
    val Text = Color(0xFF1B2133)
    /** 보조 글자 ("등록된 정보 없음", 줄 이름). 흰 바탕과 대비 약 6:1 */
    val Muted = Color(0xFF5B6478)
    val Navy = Color(0xFF1F2B5B)
    val LabelBackground = Color(0xFFF3F6FC)
    val Border = Color(0xFFDDE3EE)
    val ChipBackground = Color(0xFFE9EFFF)
    val Accent = ActionBlue
    val AvatarBackground = Color(0xFFE9EFFF)
    /** 주요 증상 강조 띠. 글자와 대비 약 7:1 */
    val SymptomBackground = Color(0xFFFFF1E0)
    val SymptomText = Color(0xFF7A3E00)
}
