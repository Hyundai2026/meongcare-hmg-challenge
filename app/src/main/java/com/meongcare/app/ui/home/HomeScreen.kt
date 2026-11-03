package com.meongcare.app.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * 홈 화면. 주행 상태에 따라 정차 화면과 주행 화면을 바꿔 보여준다.
 *
 * 전환 애니메이션은 넣지 않는다 (주행 중 움직이는 요소를 줄인다).
 *
 * @param bottomBar 화면 아래에 붙는 영역. debug 빌드의 개발자 패널이 들어간다.
 */
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    modifier: Modifier = Modifier,
    bottomBar: @Composable () -> Unit = {},
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (uiState) {
                    HomeUiState.Parked -> ParkedScreen()
                    HomeUiState.Driving -> DrivingScreen()
                }
            }
            bottomBar()
        }
    }
}
