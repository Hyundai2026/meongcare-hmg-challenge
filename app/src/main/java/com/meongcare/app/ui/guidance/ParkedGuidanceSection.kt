package com.meongcare.app.ui.guidance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.meongcare.app.R
import com.meongcare.app.ui.theme.Dimens
import com.meongcare.app.ui.theme.MeongCareTheme

/**
 * 정차 화면 안의 길안내 영역.
 *
 * - 안내 없음: 병원 이름 + [병원으로 안내]
 * - 안내 중: 남은 시간 + 병원 이름 + [안내 취소]. 안내 취소는 정차 중에만 한다 (주행 화면에는 버튼이 없다).
 * - 도착: 이 영역 대신 도착 화면이 보이므로 아무것도 그리지 않는다.
 */
@Composable
fun ParkedGuidanceSection(
    uiState: GuidanceUiState,
    onStartGuidance: () -> Unit,
    onCancelGuidance: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (uiState is GuidanceUiState.Arrived) return

    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(Dimens.CardPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing / 2),
        ) {
            when (uiState) {
                is GuidanceUiState.Idle -> {
                    Text(
                        text = stringResource(R.string.guidance_destination_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(text = uiState.destinationName, style = MaterialTheme.typography.titleLarge)
                    Button(
                        onClick = onStartGuidance,
                        modifier = Modifier.fillMaxWidth().heightIn(min = Dimens.MinTouchTarget),
                    ) {
                        Text(stringResource(R.string.guidance_start), style = MaterialTheme.typography.labelLarge)
                    }
                }
                is GuidanceUiState.Guiding -> {
                    Text(text = remainingTimeText(uiState), style = MaterialTheme.typography.headlineMedium)
                    Text(
                        text = uiState.destinationName,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedButton(
                        onClick = onCancelGuidance,
                        modifier = Modifier.fillMaxWidth().heightIn(min = Dimens.MinTouchTarget),
                    ) {
                        Text(stringResource(R.string.guidance_cancel), style = MaterialTheme.typography.labelLarge)
                    }
                }
                is GuidanceUiState.Arrived -> Unit
            }
        }
    }
}

@Preview(widthDp = 480)
@Composable
private fun ParkedGuidanceSectionIdlePreview() {
    MeongCareTheme {
        ParkedGuidanceSection(GuidanceUiState.Idle("시연용 동물병원"), onStartGuidance = {}, onCancelGuidance = {})
    }
}

@Preview(widthDp = 480)
@Composable
private fun ParkedGuidanceSectionGuidingPreview() {
    MeongCareTheme {
        ParkedGuidanceSection(GuidanceUiState.Guiding("시연용 동물병원", 12), onStartGuidance = {}, onCancelGuidance = {})
    }
}
