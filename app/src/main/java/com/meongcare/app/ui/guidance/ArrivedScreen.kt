package com.meongcare.app.ui.guidance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.meongcare.app.R
import com.meongcare.app.ui.theme.Dimens
import com.meongcare.app.ui.theme.MeongCareTheme

/**
 * 정차 + 도착 화면. 병원에 도착하면 진료 카드를 보여준다.
 *
 * - 진료 카드 내용은 아직 자리표시다 (진료 카드 기능은 후속 계획).
 * - 병원에서 보여 줄 화면이라 자동으로 닫지 않는다. [닫기]를 눌러야 처음 화면으로 돌아간다.
 * - 정차 화면이므로 세로 스크롤을 둔다 (가로 화면처럼 높이가 낮을 때 대비).
 */
@Composable
fun ArrivedScreen(
    destinationName: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Dimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
    ) {
        Text(
            text = stringResource(R.string.arrival_title, destinationName),
            style = MaterialTheme.typography.headlineMedium,
        )
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(Dimens.CardPadding),
                verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing / 2),
            ) {
                Text(
                    text = stringResource(R.string.arrival_card_title),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = stringResource(R.string.arrival_card_placeholder),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth().heightIn(min = Dimens.MinTouchTarget),
        ) {
            Text(stringResource(R.string.arrival_close), style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Preview(widthDp = 480, heightDp = 800)
@Composable
private fun ArrivedScreenPreview() {
    MeongCareTheme { ArrivedScreen(destinationName = "시연용 동물병원", onDismiss = {}) }
}
