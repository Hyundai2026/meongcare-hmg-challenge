package com.meongcare.app.ui.home

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.meongcare.app.R
import com.meongcare.app.ui.guidance.GuidanceUiState
import com.meongcare.app.ui.guidance.ParkedGuidanceSection
import com.meongcare.app.ui.theme.Dimens
import com.meongcare.app.ui.theme.MeongCareTheme

/**
 * 정차·주차 중 화면. 일반 기능과 텍스트 입력을 둘 수 있다.
 *
 * - 병원 길안내를 시작하고 취소한다.
 * - 메모 입력칸은 자리표시다 (정차 중에만 쓰는 입력의 예시). 기획이 정해지면 바꾼다.
 *
 * 가로 화면이나 화면 분할처럼 높이가 낮으면 내용이 넘치므로 세로 스크롤을 둔다.
 * (스크롤 금지는 주행 화면에만 해당한다.)
 */
@Composable
fun ParkedScreen(
    guidanceUiState: GuidanceUiState,
    onStartGuidance: () -> Unit,
    onCancelGuidance: () -> Unit,
    modifier: Modifier = Modifier,
    isGuidanceSimulated: Boolean = false,
) {
    var memo by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Dimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
    ) {
        Text(
            text = stringResource(R.string.parked_status),
            style = MaterialTheme.typography.headlineMedium,
        )
        ParkedGuidanceSection(
            uiState = guidanceUiState,
            onStartGuidance = onStartGuidance,
            onCancelGuidance = onCancelGuidance,
            showsSimulatedNotice = isGuidanceSimulated,
        )
        OutlinedTextField(
            value = memo,
            onValueChange = { memo = it },
            label = { Text(stringResource(R.string.parked_example_input_label)) },
            textStyle = MaterialTheme.typography.bodyLarge,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview(widthDp = 960, heightDp = 540)
@Composable
private fun ParkedScreenPreview() {
    MeongCareTheme {
        ParkedScreen(
            guidanceUiState = GuidanceUiState.Idle("시연용 동물병원"),
            onStartGuidance = {},
            onCancelGuidance = {},
        )
    }
}
