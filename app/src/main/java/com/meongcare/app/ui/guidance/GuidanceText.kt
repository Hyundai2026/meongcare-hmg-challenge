package com.meongcare.app.ui.guidance

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.meongcare.app.R

/** 안내 중 상태 한 줄: "병원까지 N분", 남은 시간을 아직 모르면 "경로를 찾는 중". 정차·주행 화면이 같이 쓴다. */
@Composable
fun remainingTimeText(guiding: GuidanceUiState.Guiding): String =
    guiding.remainingMinutes
        ?.let { stringResource(R.string.guidance_remaining_minutes, it) }
        ?: stringResource(R.string.guidance_finding_route)
