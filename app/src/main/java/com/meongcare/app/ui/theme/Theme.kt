package com.meongcare.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private val LightColors = lightColorScheme(
    primary = Navy700,
    onPrimary = Gray50,
    primaryContainer = Sky100,
    onPrimaryContainer = Navy900,
    secondary = Amber500,
    onSecondary = Gray900,
    background = Gray50,
    onBackground = Gray900,
    surface = Gray50,
    onSurface = Gray900,
    surfaceVariant = Gray200,
    onSurfaceVariant = Gray800,
    error = Red600,
)

private val DarkColors = darkColorScheme(
    primary = Sky300,
    onPrimary = Navy900,
    primaryContainer = Navy700,
    onPrimaryContainer = Sky100,
    secondary = Amber500,
    onSecondary = Gray900,
    // 컨셉 그림처럼 남색 배경 (009). 주행 화면 배경도 같이 남색이 된다.
    background = Navy950,
    onBackground = Gray50,
    surface = Navy950,
    onSurface = Gray50,
    surfaceVariant = Navy850,
    onSurfaceVariant = Gray200,
    error = Red300,
)

/**
 * 앱 테마. 시스템의 라이트·다크 설정을 따른다.
 * 차량은 주간/야간에 따라 시스템 테마가 바뀌므로 dynamic color는 쓰지 않는다.
 */
@Composable
fun MeongCareTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // 디자인 프로토타입 토큰 (018). 디자인을 적용한 화면만 쓴다
    CompositionLocalProvider(
        LocalDesignTokens provides DesignTokens(if (darkTheme) DarkDesignColors else LightDesignColors),
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = MeongCareTypography,
            content = content,
        )
    }
}
