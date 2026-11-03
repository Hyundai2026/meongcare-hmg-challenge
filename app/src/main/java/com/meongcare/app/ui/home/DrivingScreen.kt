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
import com.meongcare.app.ui.theme.Dimens
import com.meongcare.app.ui.theme.MeongCareTheme

/**
 * 주행 중 화면. AGENTS.md 규칙 3과 docs/pleos/ux-guidelines.md "주행 안전 설계 원칙"을 지킨다.
 *
 * - 큰 글씨의 상태 한 줄과 짧은 안내만 둔다.
 * - 버튼은 1개 이하. 둔다면 [Dimens.DrivingPrimaryButtonHeight] 이상으로.
 * - 텍스트 입력, 스크롤, 애니메이션을 넣지 않는다.
 */
@Composable
fun DrivingScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Dimens.ScreenPadding),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.driving_status),
            style = MaterialTheme.typography.displayLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Dimens.ItemSpacing))
        Text(
            text = stringResource(R.string.driving_hint),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(widthDp = 960, heightDp = 540)
@Composable
private fun DrivingScreenPreview() {
    MeongCareTheme { DrivingScreen() }
}
