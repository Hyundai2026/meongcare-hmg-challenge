package com.meongcare.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.meongcare.app.R
import com.meongcare.app.ui.guidance.DeliveryLine
import com.meongcare.app.ui.guidance.GuidanceUiState
import com.meongcare.app.ui.guidance.StaticProgressBar
import com.meongcare.app.ui.guidance.TripPanel
import com.meongcare.app.ui.theme.ActionBlue
import com.meongcare.app.ui.theme.Dimens
import com.meongcare.app.ui.theme.DrivingText
import com.meongcare.app.ui.theme.MeongCareTheme
import com.meongcare.app.ui.theme.TripColors
import java.time.LocalTime

/** 이 폭보다 좁으면(세로 반쪽 창) 숫자와 글자를 줄인다. */
private val NarrowBreakpoint = 600.dp

/**
 * 주행 중 화면. AGENTS.md 규칙 3과 docs/pleos/ux-guidelines.md "주행 안전 설계 원칙"을 지킨다.
 *
 * - 안내 없음: "주행 중" + 짧은 안내
 * - 안내 중 (013 U4): **아주 큰 남은 분** + "○○까지" + 움직이지 않는 진행 막대 + (병원이 확인했으면) "병원이 카드를 확인했어요" 한 줄.
 *   남은 분을 모르면 "경로를 찾는 중". 도착 예정 시각·대기 정보·시연용 표시·전송 상태는 넣지 않는다 (정차 화면에만).
 * - 도착: "도착했어요" + 정차하면 진료 카드를 볼 수 있다는 안내. 진료 카드는 정차 뒤에 보여준다.
 * - 버튼은 두지 않는다. 안내 취소도 정차 중에만 한다 (docs/plans 005 결정 5).
 * - 텍스트 입력, 스크롤, 애니메이션을 넣지 않는다. 진행 막대도 값이 바뀌면 그대로 다시 그린다 ([StaticProgressBar]).
 *
 * @param panel 안내 상태와 고른 병원을 합친 것 ([com.meongcare.app.ui.guidance.tripPanelOf]). 없으면 길안내 상태만으로 그린다
 */
@Composable
fun DrivingScreen(guidanceUiState: GuidanceUiState, modifier: Modifier = Modifier, panel: TripPanel? = null) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val narrow = maxWidth < NarrowBreakpoint
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Dimens.ScreenPadding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (guidanceUiState) {
                is GuidanceUiState.Idle -> StatusAndHint(stringResource(R.string.driving_status), stringResource(R.string.driving_hint))
                is GuidanceUiState.Arrived ->
                    StatusAndHint(stringResource(R.string.guidance_arrived), stringResource(R.string.guidance_arrived_driving_hint))
                is GuidanceUiState.Guiding -> Guiding(
                    minutes = guidanceUiState.remainingMinutes,
                    destination = guidanceUiState.destinationName,
                    progress = panel?.progress,
                    acknowledged = panel?.delivery is DeliveryLine.Acknowledged,
                    narrow = narrow,
                )
            }
        }
    }
}

@Composable
private fun StatusAndHint(status: String, hint: String) {
    Text(text = status, style = MaterialTheme.typography.displayLarge, textAlign = TextAlign.Center)
    Spacer(Modifier.height(Dimens.ItemSpacing))
    Text(
        text = hint,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun Guiding(minutes: Int?, destination: String, progress: Float?, acknowledged: Boolean, narrow: Boolean) {
    val destinationSize: TextUnit = if (narrow) DrivingText.DestinationNarrow else DrivingText.DestinationWide
    if (minutes == null) {
        Text(stringResource(R.string.guidance_finding_route), style = MaterialTheme.typography.displayLarge, textAlign = TextAlign.Center)
    } else {
        val numberSize = if (narrow) DrivingText.MinutesNarrow else DrivingText.MinutesWide
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = minutes.toString(),
                fontSize = numberSize,
                lineHeight = numberSize,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.trip_minutes_unit),
                fontSize = numberSize * 0.36f,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = if (narrow) 20.dp else 30.dp),
            )
        }
    }
    Spacer(Modifier.height(8.dp))
    Text(
        text = stringResource(R.string.trip_to_destination, destination),
        fontSize = destinationSize,
        lineHeight = destinationSize * 1.25f,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
    if (progress != null) {
        Spacer(Modifier.height(36.dp))
        StaticProgressBar(
            progress = progress,
            track = MaterialTheme.colorScheme.surfaceVariant,
            fill = ActionBlue,
            height = if (narrow) 16.dp else 22.dp,
            modifier = Modifier.widthIn(max = 880.dp),
        )
    }
    if (acknowledged) {
        Spacer(Modifier.height(36.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(TripColors.Arrived, RoundedCornerShape(50))
                .padding(horizontal = 28.dp, vertical = 14.dp),
        ) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = TripColors.OnPanel, modifier = Modifier.size(36.dp))
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.driving_acknowledged),
                style = MaterialTheme.typography.titleLarge.copy(fontSize = destinationSize * 0.85f),
                fontWeight = FontWeight.Bold,
                color = TripColors.OnPanel,
            )
        }
    }
}

private fun previewPanel(acknowledged: Boolean) = TripPanel(
    destinationName = "시연용 바른 동물병원",
    remainingMinutes = 12,
    arrivesAt = LocalTime.of(14, 32),
    progress = 0.4f,
    statusAtDeparture = null,
    delivery = if (acknowledged) DeliveryLine.Acknowledged(LocalTime.of(14, 5)) else DeliveryLine.SentWaiting,
    arrived = false,
)

@Preview(widthDp = 1682, heightDp = 1110)
@Composable
private fun DrivingScreenGuidingWidePreview() {
    MeongCareTheme { DrivingScreen(GuidanceUiState.Guiding("시연용 바른 동물병원", 12), panel = previewPanel(acknowledged = true)) }
}

@Preview(widthDp = 411, heightDp = 800)
@Composable
private fun DrivingScreenGuidingNarrowPreview() {
    MeongCareTheme(darkTheme = true) {
        DrivingScreen(GuidanceUiState.Guiding("시연용 바른 동물병원", 12), panel = previewPanel(acknowledged = true))
    }
}

@Preview(widthDp = 960, heightDp = 540)
@Composable
private fun DrivingScreenIdlePreview() {
    MeongCareTheme { DrivingScreen(GuidanceUiState.Idle("시연용 동물병원")) }
}
