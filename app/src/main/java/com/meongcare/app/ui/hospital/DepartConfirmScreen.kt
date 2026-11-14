package com.meongcare.app.ui.hospital

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.meongcare.app.R
import com.meongcare.app.ui.home.DesignDivider
import com.meongcare.app.ui.theme.DesignScale
import com.meongcare.app.ui.theme.DesignSpace
import com.meongcare.app.ui.theme.LocalDesignTokens
import com.meongcare.app.ui.voice.VoiceCommandButton
import java.time.Instant
import java.util.Locale

private val Name = TextStyle(fontSize = 34.sp, lineHeight = 44.sp, fontWeight = FontWeight.Medium)
private val Eta = TextStyle(fontSize = 64.sp, lineHeight = 76.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.02).em)
private val Detail = TextStyle(fontSize = 32.sp, lineHeight = 44.sp)
private val Message = TextStyle(fontSize = 36.sp, lineHeight = 52.sp, fontWeight = FontWeight.Medium)
private val Sends = TextStyle(fontSize = 28.sp, lineHeight = 40.sp)

/**
 * "출발 확인" (계획 020, 프로토타입 confirm). 고른 병원, 도착 예정 시각, 보내는 정보를 보이고
 * [목적지 설정하고 출발]이 이번 카드 전송의 동의다 (012 원칙: 보내는 정보를 버튼 바로 위에 보인다).
 * 시연 병원은 앱 안 시연 전송이다 (병원 웹으로 가지 않는다).
 *
 * @param startMinutes 진료 가능 답변에서 고른 병원의 진료 시작까지 분. null이면 "대기시간 미확인"
 * @param petName 반려견 이름 (없으면 이름 없는 문구)
 * @param onDepart 카드와 함께 출발 (`HospitalTrip.depart(choice, card)`)
 */
@Composable
fun DepartConfirmScreen(
    hospital: DemoHospital,
    startMinutes: Int?,
    petName: String?,
    now: Instant,
    onBack: () -> Unit,
    onDepart: () -> Unit,
    modifier: Modifier = Modifier,
    onCall: (String) -> Unit = {},
) {
    val colors = LocalDesignTokens.current.colors
    var notice by rememberSaveable { mutableStateOf<Int?>(null) }
    // 시스템 뒤로 = [←] (비교로)
    androidx.activity.compose.BackHandler(onBack = onBack)
    BoxWithConstraints(modifier = modifier.fillMaxSize().background(colors.homeBackground)) {
        val scale = DesignScale.of(maxWidth)
        val eta = koreanClock(now.plusSeconds(hospital.travelMinutes * 60L))
        Column(Modifier.fillMaxSize().padding(horizontal = scale.gap(DesignSpace.xxxl), vertical = scale.gap(DesignSpace.l))) {
            FlowHeader(stringResource(R.string.depart_title), onBack, scale, colors) {
                androidx.compose.foundation.layout.Spacer(Modifier.padding(start = scale.gap(DesignSpace.l)))
                VoiceCommandButton(onClick = { notice = R.string.voice_not_ready }, scale = scale, colors = colors)
            }
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(top = scale.gap(DesignSpace.l)),
                horizontalArrangement = Arrangement.spacedBy(scale.gap(DesignSpace.xxxl)),
            ) {
                Column(
                    Modifier
                        .weight(0.46f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(start = scale.gap(DesignSpace.xxxl)),
                ) {
                    Text(hospital.name, style = scale.style(Name), color = colors.homeText)
                    Text(stringResource(R.string.depart_eta, eta), style = scale.style(Eta), color = colors.homeText)
                    val km = String.format(Locale.ROOT, "%.1f", hospital.distanceKm)
                    Text(
                        if (startMinutes != null) stringResource(R.string.depart_detail_start, hospital.travelMinutes, km, startMinutes)
                        else stringResource(R.string.depart_detail_unknown, hospital.travelMinutes, km),
                        style = scale.style(Detail),
                        color = colors.homeSubText,
                    )
                    Spacer(Modifier.height(scale.gap(DesignSpace.xl)))
                    DesignDivider(colors, scale)
                    Spacer(Modifier.height(scale.gap(DesignSpace.xl)))
                    Text(
                        if (petName.isNullOrBlank()) stringResource(R.string.depart_message_no_pet)
                        else stringResource(R.string.depart_message, petName + "의"),
                        style = scale.style(Message),
                        color = colors.selectedText,
                    )
                    Spacer(Modifier.height(scale.gap(DesignSpace.l)))
                    Text(stringResource(R.string.depart_sends), style = scale.style(Sends), color = colors.homeSubText)
                    Spacer(Modifier.height(scale.gap(DesignSpace.xl)))
                    FlowButton(
                        stringResource(R.string.depart_go),
                        onDepart,
                        primary = true,
                        scale = scale,
                        colors = colors,
                        modifier = Modifier.fillMaxWidth(),
                        icon = com.meongcare.app.ui.home.HomeIcons.Navigate,
                    )
                    Spacer(Modifier.height(scale.gap(DesignSpace.m)))
                    FlowButton(
                        stringResource(R.string.depart_call),
                        { hospital.row.hospital.phoneNumber?.let(onCall) ?: run { notice = R.string.depart_no_phone } },
                        primary = false,
                        scale = scale,
                        colors = colors,
                        modifier = Modifier.fillMaxWidth(),
                        icon = Icons.Filled.Phone,
                    )
                    notice?.let {
                        Spacer(Modifier.height(scale.gap(DesignSpace.s)))
                        Text(stringResource(it), style = scale.style(Sends), color = colors.selectedText)
                    }
                }
                DemoMap(
                    hospitals = listOf(hospital),
                    selected = hospital,
                    scale = scale,
                    colors = colors,
                    modifier = Modifier.weight(0.54f).fillMaxHeight(),
                    onSelect = null,
                    infoTitle = hospital.name,
                    infoSubtitle = stringResource(R.string.depart_eta, eta),
                    selectedLabel = stringResource(R.string.map_label_travel, hospital.rank, hospital.name, hospital.travelMinutes),
                    showBadge = false,
                )
            }
        }
    }
}
