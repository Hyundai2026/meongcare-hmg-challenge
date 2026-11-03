package com.meongcare.app.ui.theme

import androidx.compose.ui.unit.dp

/**
 * 크기 값을 한곳에 모은다.
 *
 * 임시값: Pleos 공식 터치 타깃 수치는 아직 확인하지 못했다 (docs/pleos/ux-guidelines.md "미확인").
 * 차량 화면은 팔을 뻗어 누르므로 휴대폰 기준(48dp)보다 크게 잡았다.
 */
object Dimens {
    /** 일반 버튼의 최소 터치 높이 */
    val MinTouchTarget = 64.dp

    /** 주행 화면의 주 버튼 높이. 주행 중에는 버튼을 1개 이하로 둔다. */
    val DrivingPrimaryButtonHeight = 96.dp

    val ScreenPadding = 32.dp
    val ItemSpacing = 24.dp
}
