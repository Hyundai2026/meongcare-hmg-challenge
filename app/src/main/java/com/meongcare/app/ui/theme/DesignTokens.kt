package com.meongcare.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * 확정 디자인 프로토타입(`docs/product/design/prototype/MeongCare car.html`)의 토큰 (계획 018).
 *
 * 값은 프로토타입 JS의 `TH` 표(라이트·다크)와 화면 스타일에서 그대로 옮겼다. 프로토타입 px ≈ dp, sp
 * (기준 화면 1682×1210 = 앱의 Wide 기준과 같은 크기).
 *
 * 화면을 하나씩 옮기는 동안 Material `colorScheme`은 바꾸지 않는다. 디자인을 적용한 화면만
 * [LocalDesignTokens]를 쓴다. 모든 화면을 옮기면 colorScheme을 이 값으로 맞춘다.
 *
 * 폰트: 프로토타입은 Asta Sans다. Pleos에는 시스템 UI 폰트로 깔려 있어(docs/pleos/ux-guidelines.md) 파일을 넣지 않고
 * 기기 기본 글꼴을 쓴다 (사용자 결정 2026-11-13: 다운로드하지 않는다).
 */
@Immutable
data class DesignColors(
    /** 화면 바탕 (bg) */
    val background: Color,
    /** 처음 화면 바탕 (hbg) */
    val homeBackground: Color,
    /** 카드·버튼 표면 (sf) */
    val surface: Color,
    /** 한 단계 진한 표면, 보조 버튼 (sf2) */
    val surface2: Color,
    /** 구분선·진행 막대 바탕 (ln) */
    val line: Color,
    /** 글자 (tx) */
    val text: Color,
    /** 보조 글자 (sub) */
    val subText: Color,
    /** 메인 (pr) */
    val primary: Color,
    /** 메인 위 글자 (onpr) */
    val onPrimary: Color,
    /** 선택 바탕 (sel) */
    val selected: Color,
    /** 선택 글자·강조 글자·글자 버튼 (selt) */
    val selectedText: Color,
    /** 성공 글자 / 바탕 (ok / okb) */
    val success: Color,
    val successBackground: Color,
    /** 경고 글자 / 바탕 (warn / warnb) */
    val warning: Color,
    val warningBackground: Color,
    /** 에러 글자 / 바탕 (er / erb) */
    val error: Color,
    val errorBackground: Color,
    /** 비활성 바탕 / 글자 (dis / dist) */
    val disabled: Color,
    val disabledText: Color,
    /** 처음 화면(응급 진료) 글자 / 보조 글자 / 바로가기 아이콘 바탕 / 구분선 / 눌린 주 버튼 (htx·hsub·hic·hln·hprh, 계획 019) */
    val homeText: Color = text,
    val homeSubText: Color = subText,
    val homeIcon: Color = surface,
    val homeLine: Color = line,
    val homePrimaryPressed: Color = primary,
)

val LightDesignColors = DesignColors(
    background = Color(0xFFFFFFFF),
    homeBackground = Color(0xFFF8F9FB),
    surface = Color(0xFFF4F5F7),
    surface2 = Color(0xFFECEEF2),
    line = Color(0xFFE8EBF0),
    text = Color(0xFF1B2231),
    subText = Color(0xFF657085),
    primary = Color(0xFF3E5DB4),
    onPrimary = Color(0xFFFFFFFF),
    selected = Color(0xFFEEF2FA),
    selectedText = Color(0xFF2F4A99),
    success = Color(0xFF1B7A4B),
    successBackground = Color(0xFFE6F4EC),
    warning = Color(0xFF8A5A00),
    warningBackground = Color(0xFFFFF3DC),
    error = Color(0xFFB42318),
    errorBackground = Color(0xFFFDEDEC),
    disabled = Color(0xFFEEF0F3),
    disabledText = Color(0xFF8A93A3),
    homeText = Color(0xFF222938),
    homeSubText = Color(0xFF697386),
    homeIcon = Color(0xFFF1F3F7),
    homeLine = Color(0xFFE8EBF1),
    homePrimaryPressed = Color(0xFF33509F),
)

val DarkDesignColors = DesignColors(
    background = Color(0xFF111317),
    // 프로토타입 다크 표에는 hbg가 없고 bg를 쓴다
    homeBackground = Color(0xFF111317),
    surface = Color(0xFF1A1D22),
    surface2 = Color(0xFF24282F),
    line = Color(0xFF2A2F37),
    text = Color(0xFFE8EAEE),
    subText = Color(0xFFA0A7B4),
    primary = Color(0xFF8FA8EC),
    onPrimary = Color(0xFF0E1422),
    selected = Color(0xFF1C2538),
    selectedText = Color(0xFFB9C8F3),
    success = Color(0xFF7FD1A4),
    successBackground = Color(0xFF15291E),
    warning = Color(0xFFE9C27A),
    warningBackground = Color(0xFF2E2412),
    error = Color(0xFFFF9B90),
    errorBackground = Color(0xFF3A1A17),
    disabled = Color(0xFF1E1F38),
    disabledText = Color(0xFF7E80A0),
)

/** 간격 단계 (프로토타입: 4·8·12·16·24·32·40·48 + 큰 여백 64·80·104) */
object DesignSpace {
    val xxs: Dp = 4.dp
    val xs: Dp = 8.dp
    val s: Dp = 12.dp
    val m: Dp = 16.dp
    val l: Dp = 24.dp
    val xl: Dp = 32.dp
    val xxl: Dp = 40.dp
    val xxxl: Dp = 48.dp
    val section: Dp = 64.dp
    val page: Dp = 80.dp
    val pageWide: Dp = 104.dp
}

/** 모서리 둥글기 (프로토타입: 기본 16, 작은 12, 큰 패널 24~28, 알약 999) */
object DesignRadius {
    val s: Dp = 12.dp
    val m: Dp = 16.dp
    val l: Dp = 24.dp
    val xl: Dp = 28.dp
    val pill: Dp = 999.dp
}

/** 버튼 높이 (프로토타입: 주 96, 보조 80, 작은 48) */
object DesignHeight {
    val buttonLarge: Dp = 96.dp
    val buttonMedium: Dp = 80.dp
    val buttonSmall: Dp = 48.dp
}

/**
 * 글자 단계 (프로토타입 기준 화면 = Wide). 좁은 화면은 [narrow]로 줄여 쓴다.
 * 줄 간격: 본문 1.4, 제목 1.2~1.3. 자간: 제목 -0.02em.
 */
object DesignType {
    /** 숫자 강조 (연결 코드) 160/500 */
    val code = TextStyle(fontSize = 160.sp, lineHeight = 160.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.1.em)

    /** 큰 제목 64/700 */
    val display = TextStyle(fontSize = 64.sp, lineHeight = 77.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.02).em)

    /** 제목 48/500 */
    val headline = TextStyle(fontSize = 48.sp, lineHeight = 62.sp, fontWeight = FontWeight.Medium, letterSpacing = (-0.01).em)

    /** 화면 제목·중간 제목 40/600 */
    val title = TextStyle(fontSize = 40.sp, lineHeight = 50.sp, fontWeight = FontWeight.SemiBold)

    /** 본문 큰 34/400 (안내문) */
    val bodyLarge = TextStyle(fontSize = 34.sp, lineHeight = 48.sp, fontWeight = FontWeight.Normal)

    /** 버튼 글자 34/600 */
    val button = TextStyle(fontSize = 34.sp, lineHeight = 40.sp, fontWeight = FontWeight.SemiBold)

    /** 본문 30/400 */
    val body = TextStyle(fontSize = 30.sp, lineHeight = 42.sp, fontWeight = FontWeight.Normal)

    /** 좁은 화면(폰 세로)에서 글자를 줄이는 비율 */
    const val NARROW_SCALE = 0.6f

    fun narrow(style: TextStyle): TextStyle = style.copy(
        fontSize = style.fontSize * NARROW_SCALE,
        lineHeight = style.lineHeight * NARROW_SCALE,
    )
}

/** 디자인 토큰 묶음. [MeongCareTheme]이 라이트·다크에 맞춰 내려 준다. */
@Immutable
data class DesignTokens(val colors: DesignColors)

val LocalDesignTokens = staticCompositionLocalOf { DesignTokens(LightDesignColors) }
