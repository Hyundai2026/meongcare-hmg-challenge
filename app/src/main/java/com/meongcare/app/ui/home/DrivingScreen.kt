package com.meongcare.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.meongcare.app.R
import com.meongcare.app.ui.guidance.GuidanceUiState
import com.meongcare.app.ui.guidance.remainingTimeText
import com.meongcare.app.ui.theme.Dimens
import com.meongcare.app.ui.theme.MeongCareTheme

/**
 * 주행 중 화면. AGENTS.md 규칙 3과 docs/pleos/ux-guidelines.md "주행 안전 설계 원칙"을 지킨다.
 *
 * - 큰 글씨의 상태 한 줄과 짧은 안내 한 줄만 둔다.
 *   - 안내 없음: "주행 중"
 *   - 안내 중: "병원까지 N분" (모르면 "경로를 찾는 중") + 병원 이름
 *   - 도착: "도착했어요" + 정차하면 진료 카드를 볼 수 있다는 안내. 진료 카드는 정차 뒤에 보여준다.
 * - 버튼은 두지 않는다. 안내 취소도 정차 중에만 한다 (docs/plans 005 결정 5).
 * - 텍스트 입력, 스크롤, 애니메이션을 넣지 않는다.
 */
@Composable
fun DrivingScreen(guidanceUiState: GuidanceUiState, modifier: Modifier = Modifier) {
    val status: String
    val hint: String
    when (guidanceUiState) {
        is GuidanceUiState.Idle -> {
            status = stringResource(R.string.driving_status)
            hint = stringResource(R.string.driving_hint)
        }
        is GuidanceUiState.Guiding -> {
            status = remainingTimeText(guidanceUiState)
            hint = guidanceUiState.destinationName
        }
        is GuidanceUiState.Arrived -> {
            status = stringResource(R.string.guidance_arrived)
            hint = stringResource(R.string.guidance_arrived_driving_hint)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Dimens.ScreenPadding),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = status,
            style = MaterialTheme.typography.displayLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Dimens.ItemSpacing))
        Text(
            text = hint,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(widthDp = 960, heightDp = 540)
@Composable
private fun DrivingScreenPreview() {
    MeongCareTheme { DrivingScreen(GuidanceUiState.Idle("시연용 동물병원")) }
}

@Preview(widthDp = 480, heightDp = 800)
@Composable
private fun DrivingScreenGuidingPreview() {
    MeongCareTheme { DrivingScreen(GuidanceUiState.Guiding("시연용 동물병원", 12)) }
}
