package com.meongcare.app.ui.guidance

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meongcare.app.R
import com.meongcare.app.ui.card.LocalCardScale
import com.meongcare.app.ui.hospital.congestionText
import com.meongcare.app.ui.theme.Dimens
import com.meongcare.app.ui.theme.MeongCareTheme
import com.meongcare.app.ui.theme.TripColors
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** 이 폭 이상이면 목적지·도착 예정을 왼쪽에, 큰 남은 분을 오른쪽에 둔다. */
private val WidePanelBreakpoint = 720.dp

private val ClockFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** "14:32" */
internal fun clockText(time: LocalTime): String = time.format(ClockFormat)

/**
 * 정차 중 안내 영역 (013): 진료 카드 화면 머리 아래와 처음 화면에 같은 모양으로 보인다. **정차 화면 전용**이다
 * (주행 화면은 `DrivingScreen`이 같은 [TripPanel]로 더 적게 그린다).
 *
 * - 안내 중: 목적지, 큰 남은 분, 도착 예정 시각, 출발 때 알린 대기, 진행 막대(움직이지 않음), 전송·확인 줄
 * - 도착: "○○에 도착했어요 · 이 카드를 접수처에 보여 주세요" + 전송·확인 줄
 *
 * 색은 테마와 상관없는 고정색([TripColors])이다. [병원에 전화] [안내 취소]는 화면 아래 고정 줄에 있다 (013 K2).
 * @param onResend 전송 실패 줄의 [다시 보내기]
 */
@Composable
fun GuidancePanel(panel: TripPanel, onResend: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = if (panel.arrived) TripColors.Arrived else TripColors.Panel,
        contentColor = TripColors.OnPanel,
        modifier = modifier.fillMaxWidth(),
    ) {
        BoxWithConstraints {
            val wide = maxWidth >= WidePanelBreakpoint
            Column(
                modifier = Modifier.padding(horizontal = if (wide) 32.dp else 24.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                if (panel.arrived) ArrivedBody(panel) else GuidingBody(panel, wide)
                DeliveryChip(panel.delivery, onResend)
            }
        }
    }
}

@Composable
private fun GuidingBody(panel: TripPanel, wide: Boolean) {
    val scale = LocalCardScale.current
    val details: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val rowStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = scale.row, lineHeight = scale.row * 1.3f)
            panel.arrivesAt?.let {
                Text(stringResource(R.string.trip_arrives_at, clockText(it)), style = rowStyle, fontWeight = FontWeight.SemiBold)
            }
            panel.statusAtDeparture?.let {
                Text(
                    stringResource(R.string.trip_wait_at_departure, it.waitMinutes, congestionText(it.congestion)),
                    style = rowStyle,
                    color = TripColors.OnPanel.copy(alpha = 0.85f),
                )
            }
        }
    }
    if (wide) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Destination(panel.destinationName)
                details()
            }
            Spacer(Modifier.width(24.dp))
            BigMinutes(panel.remainingMinutes, numberSize = 88)
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Destination(panel.destinationName)
            BigMinutes(panel.remainingMinutes, numberSize = 64)
            details()
        }
    }
    panel.progress?.let { StaticProgressBar(it, track = TripColors.Track, fill = TripColors.Fill, height = 14.dp) }
}

@Composable
private fun Destination(name: String) {
    val scale = LocalCardScale.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.LocationOn, contentDescription = null, modifier = Modifier.size(32.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.trip_to_destination, name),
            style = MaterialTheme.typography.titleLarge.copy(fontSize = scale.title, lineHeight = scale.title * 1.25f),
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** "15분" (숫자를 크게). 남은 분을 모르면 "경로를 찾는 중" */
@Composable
private fun BigMinutes(minutes: Int?, numberSize: Int) {
    if (minutes == null) {
        Text(stringResource(R.string.guidance_finding_route), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        return
    }
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            text = minutes.toString(),
            fontSize = numberSize.sp,
            lineHeight = numberSize.sp,
            fontWeight = FontWeight.ExtraBold,
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = stringResource(R.string.trip_minutes_unit),
            fontSize = (numberSize * 0.4f).sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = (numberSize * 0.12f).dp),
        )
    }
}

@Composable
private fun ArrivedBody(panel: TripPanel) {
    val scale = LocalCardScale.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(56.dp))
        Spacer(Modifier.width(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(R.string.trip_arrived_title, panel.destinationName),
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = scale.title, lineHeight = scale.title * 1.25f),
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.trip_arrived_hint),
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = scale.row, lineHeight = scale.row * 1.3f),
            )
        }
    }
}

/** 흰 바탕의 전송·확인 줄. 실패면 [다시 보내기]. [DeliveryLine.None]이면 없다 */
@Composable
private fun DeliveryChip(line: DeliveryLine, onResend: () -> Unit) {
    val scale = LocalCardScale.current
    val (icon: ImageVector, iconTint: Color, text: String) = when (line) {
        DeliveryLine.None -> return
        // 시연 전송 줄은 보이지 않는다 (2026-11-14 사용자 결정, [com.meongcare.app.ui.SHOW_DEMO_NOTICES])
        DeliveryLine.Sample -> if (!com.meongcare.app.ui.SHOW_DEMO_NOTICES) return else Triple(Icons.Outlined.Info, TripColors.ChipMuted, stringResource(R.string.delivery_sample))
        DeliveryLine.Sending -> Triple(Icons.Filled.Send, TripColors.ChipMuted, stringResource(R.string.delivery_sending))
        DeliveryLine.SentWaiting -> Triple(Icons.Filled.Send, TripColors.Panel, stringResource(R.string.delivery_sent_waiting))
        DeliveryLine.Sent -> Triple(Icons.Filled.Send, TripColors.Panel, stringResource(R.string.delivery_sent))
        is DeliveryLine.Acknowledged ->
            Triple(Icons.Filled.CheckCircle, TripColors.Ack, stringResource(R.string.delivery_acknowledged, clockText(line.at)))
        DeliveryLine.Failed -> Triple(Icons.Outlined.Warning, TripColors.Warn, stringResource(R.string.delivery_failed))
        DeliveryLine.NotSent -> Triple(Icons.Outlined.Info, TripColors.ChipMuted, stringResource(R.string.delivery_not_sent))
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(TripColors.Chip, RoundedCornerShape(18.dp))
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(30.dp))
        Spacer(Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = scale.chip, lineHeight = scale.chip * 1.3f),
            color = if (line is DeliveryLine.Failed) TripColors.Warn else TripColors.ChipText,
            fontWeight = if (line is DeliveryLine.Acknowledged) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f),
        )
        if (line == DeliveryLine.Failed) {
            Spacer(Modifier.width(12.dp))
            OutlinedButton(
                onClick = onResend,
                shape = CircleShape,
                border = BorderStroke(2.dp, TripColors.Warn),
                modifier = Modifier.heightIn(min = Dimens.MinTouchTarget),
            ) {
                Text(stringResource(R.string.delivery_resend), style = MaterialTheme.typography.labelLarge, color = TripColors.ChipText)
            }
        }
    }
}

private val previewStatus = com.meongcare.app.domain.hospital.HospitalStatus(
    10,
    com.meongcare.app.domain.hospital.Congestion.NORMAL,
    true,
    java.time.Instant.parse("2026-11-06T05:10:00Z"),
)

@Preview(widthDp = 1600, heightDp = 360)
@Composable
private fun GuidancePanelWidePreview() {
    MeongCareTheme {
        GuidancePanel(
            TripPanel("시연용 바른 동물병원", 15, LocalTime.of(14, 32), 0.4f, previewStatus, DeliveryLine.Acknowledged(LocalTime.of(14, 5)), arrived = false),
            onResend = {},
        )
    }
}

@Preview(widthDp = 411, heightDp = 520)
@Composable
private fun GuidancePanelNarrowPreview() {
    MeongCareTheme(darkTheme = true) {
        GuidancePanel(
            TripPanel("시연용 바른 동물병원", 15, LocalTime.of(14, 32), 0.4f, previewStatus, DeliveryLine.Failed, arrived = false),
            onResend = {},
        )
    }
}

@Preview(widthDp = 1600, heightDp = 300)
@Composable
private fun GuidancePanelArrivedPreview() {
    MeongCareTheme {
        GuidancePanel(
            TripPanel("시연용 바른 동물병원", null, null, null, previewStatus, DeliveryLine.Acknowledged(LocalTime.of(14, 5)), arrived = true),
            onResend = {},
        )
    }
}
