package com.meongcare.app.ui.trip

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.meongcare.app.R
import com.meongcare.app.ui.home.DesignDivider
import com.meongcare.app.ui.home.HomeIcons
import com.meongcare.app.ui.hospital.FlowHeader
import com.meongcare.app.ui.theme.DesignColors
import com.meongcare.app.ui.theme.DesignHeight
import com.meongcare.app.ui.theme.DesignRadius
import com.meongcare.app.ui.theme.DesignScale
import com.meongcare.app.ui.theme.DesignSpace
import com.meongcare.app.ui.theme.LocalDesignTokens
import com.meongcare.app.ui.theme.MeongCareTheme
import com.meongcare.app.ui.voice.VoiceCommandButton
import com.meongcare.app.ui.voice.VoiceGuideLine
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** 병원 응답 (프로토타입 `h.acc`) */
enum class TripReply { NO_CARD, ACCEPTED, REFUSED, UNKNOWN }

/**
 * "병원으로 이동 중" 화면 값 (디자인 시안 nav·주행, 2026-11-14). [tripViewOf]가 만든다.
 * @param remainingMinutes null = 경로를 찾는 중
 * @param waitText 병원 대기 줄 값. null이면 "미확인"
 * @param relaxText 릴렉스 모드 줄 값 ("켜짐 · …" / "꺼짐")
 */
data class TripView(
    val hospitalName: String,
    val remainingMinutes: Int?,
    val arrivesAt: LocalTime?,
    val reply: TripReply,
    val petName: String?,
    val waitText: String?,
    val relaxOn: Boolean,
    val relaxText: String,
    val canCall: Boolean,
)

private val Clock: DateTimeFormatter = DateTimeFormatter.ofPattern("a h:mm", Locale.KOREAN)

private val NaviLine = TextStyle(fontSize = 28.sp, lineHeight = 36.sp)
private val Name = TextStyle(fontSize = 32.sp, lineHeight = 40.sp, fontWeight = FontWeight.Medium)
private val Big = TextStyle(fontSize = 56.sp, lineHeight = 64.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.02).em)
private val Eta = TextStyle(fontSize = 30.sp, lineHeight = 38.sp)
private val Reply = TextStyle(fontSize = 30.sp, lineHeight = 38.sp, fontWeight = FontWeight.Medium)
private val Key = TextStyle(fontSize = 26.sp, lineHeight = 34.sp)
private val Value = TextStyle(fontSize = 30.sp, lineHeight = 38.sp)
private val ExtraTitle = TextStyle(fontSize = 32.sp, lineHeight = 40.sp, fontWeight = FontWeight.SemiBold)
private val ExtraSub = TextStyle(fontSize = 26.sp, lineHeight = 34.sp)
private val ButtonText = TextStyle(fontSize = 34.sp, lineHeight = 42.sp, fontWeight = FontWeight.SemiBold)

/**
 * 정차 중 "병원으로 이동 중" (사용자가 준 디자인 화면 그대로, 2026-11-14). 안내 중에 진료 카드 화면 대신 보인다.
 * 숫자(병원까지 N분·도착 예정)는 출발할 때 값이고 줄어들지 않는다 (사용자 요청).
 * [음성 명령]·[추가 증상 알리기]는 자리만 있다 (누르면 준비 중 안내).
 *
 * @param onArrive [병원 도착]: 사용자가 직접 도착 처리
 * @param onEnd [이동 종료]: 안내 취소
 */
@Composable
fun ParkedTripScreen(
    view: TripView,
    onBack: () -> Unit,
    onCall: () -> Unit,
    onRelax: () -> Unit,
    onEnd: () -> Unit,
    onArrive: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDesignTokens.current.colors
    var notice by rememberSaveable { mutableStateOf(false) }
    val eta = view.arrivesAt?.format(Clock)
    BoxWithConstraints(modifier = modifier.fillMaxSize().background(colors.homeBackground)) {
        val scale = DesignScale.of(maxWidth)
        Column(Modifier.fillMaxSize().padding(horizontal = scale.gap(DesignSpace.xxxl), vertical = scale.gap(DesignSpace.s))) {
            FlowHeader(stringResource(R.string.trip_title), onBack, scale, colors) {
                Spacer(Modifier.width(scale.gap(DesignSpace.l)))
                VoiceCommandButton(onClick = { notice = true }, scale = scale, colors = colors)
            }
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = scale.gap(DesignSpace.l)),
            ) {
                Spacer(Modifier.height(scale.gap(DesignSpace.s)))
                VoiceGuideLine(stringResource(R.string.trip_say_navi), scale, colors)
                if (notice) Text(stringResource(R.string.voice_not_ready), style = scale.style(NaviLine), color = colors.selectedText)
                Spacer(Modifier.height(scale.gap(DesignSpace.s)))
                NaviHint(scale, colors)
                Text(view.hospitalName, style = scale.style(Name), color = colors.homeText)
                Text(
                    view.remainingMinutes?.let { stringResource(R.string.trip_to_hospital, it) } ?: stringResource(R.string.trip_routing),
                    style = scale.style(Big),
                    color = colors.homeText,
                )
                eta?.let { Text(stringResource(R.string.depart_eta, it), style = scale.style(Eta), color = colors.homeSubText) }
                Spacer(Modifier.height(scale.gap(DesignSpace.xs)))
                Text(
                    view.petName?.let { stringResource(R.string.trip_waiting_for, com.meongcare.app.ui.withObjectParticle(it)) }
                        ?: stringResource(R.string.trip_waiting_for_none),
                    style = scale.style(Reply),
                    color = colors.selectedText,
                )
                Spacer(Modifier.height(scale.gap(DesignSpace.m)))
                DesignDivider(colors, scale)
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                    Fact(stringResource(R.string.trip_row_pet), view.petName ?: stringResource(R.string.trip_unknown), scale, colors, Modifier.weight(1f), first = true)
                    Fact(
                        stringResource(R.string.trip_row_delivery),
                        if (view.reply == TripReply.NO_CARD) stringResource(R.string.trip_delivery_no_card)
                        else eta?.let { stringResource(R.string.trip_delivery_eta, it) } ?: stringResource(R.string.trip_unknown),
                        scale,
                        colors,
                        Modifier.weight(1f),
                    )
                    Fact(stringResource(R.string.trip_row_start), view.waitText ?: stringResource(R.string.trip_wait_unknown), scale, colors, Modifier.weight(1f))
                    Fact(
                        stringResource(R.string.trip_row_relax),
                        stringResource(if (view.relaxOn) R.string.relax_summary_on else R.string.relax_summary_off),
                        scale,
                        colors,
                        Modifier.weight(1f),
                    )
                }
                DesignDivider(colors, scale)
                ExtraSymptoms(view.petName, scale, colors, onClick = { notice = true })
            }
            Row(
                Modifier.fillMaxWidth().padding(top = scale.gap(DesignSpace.xs)),
                horizontalArrangement = Arrangement.spacedBy(scale.gap(DesignSpace.l)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (view.canCall) {
                    GreyButton(scale, colors, onCall) {
                        Icon(Icons.Filled.Phone, contentDescription = null, tint = colors.primary, modifier = Modifier.size(scale.gap(34.dp)))
                        Spacer(Modifier.width(scale.gap(DesignSpace.s)))
                        Text(stringResource(R.string.trip_call), style = scale.style(ButtonText))
                    }
                }
                GreyButton(scale, colors, onRelax) {
                    Icon(Icons.Filled.Favorite, contentDescription = null, tint = colors.primary, modifier = Modifier.size(scale.gap(34.dp)))
                    Spacer(Modifier.width(scale.gap(DesignSpace.s)))
                    Text(stringResource(if (view.relaxOn) R.string.trip_relax_on else R.string.trip_relax_off), style = scale.style(ButtonText))
                }
                TextButton(onClick = onEnd, modifier = Modifier.heightIn(min = scale.touch(DesignHeight.buttonLarge))) {
                    Text(stringResource(R.string.trip_end), style = scale.style(ButtonText).copy(fontWeight = FontWeight.Medium), color = colors.homeSubText)
                }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = onArrive,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary),
                    shape = RoundedCornerShape(DesignRadius.m),
                    elevation = null,
                    contentPadding = PaddingValues(horizontal = scale.gap(DesignSpace.xxxl)),
                    modifier = Modifier.heightIn(min = scale.touch(DesignHeight.buttonLarge)).widthIn(min = scale.gap(360.dp)),
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(scale.gap(34.dp)))
                    Spacer(Modifier.width(scale.gap(DesignSpace.s)))
                    Text(stringResource(R.string.trip_arrive), style = scale.style(ButtonText))
                }
            }
        }
    }
}

@Composable
private fun NaviHint(scale: DesignScale, colors: DesignColors) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(HomeIcons.Navigate, contentDescription = null, tint = colors.primary, modifier = Modifier.size(scale.gap(30.dp)))
        Spacer(Modifier.width(scale.gap(DesignSpace.s)))
        Text(stringResource(R.string.trip_navi), style = scale.style(NaviLine), color = colors.homeSubText)
    }
}

/** 네 칸 중 하나: 이름(작게) / 값. 칸 사이는 세로선 */
@Composable
private fun Fact(key: String, value: String, scale: DesignScale, colors: DesignColors, modifier: Modifier, first: Boolean = false) {
    Row(modifier.fillMaxHeight()) {
        if (!first) Box(Modifier.width(scale.gap(2.dp).coerceAtLeast(1.dp)).fillMaxHeight().background(colors.homeLine))
        Column(Modifier.padding(horizontal = scale.gap(DesignSpace.l), vertical = scale.gap(DesignSpace.s))) {
            Text(key, style = scale.style(Key), color = colors.homeSubText)
            Spacer(Modifier.height(scale.gap(DesignSpace.xxs)))
            Text(value, style = scale.style(Value), color = colors.homeText)
        }
    }
}

@Composable
private fun ExtraSymptoms(petName: String?, scale: DesignScale, colors: DesignColors, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = scale.gap(DesignSpace.s)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(HomeIcons.Mic, contentDescription = null, tint = colors.primary, modifier = Modifier.size(scale.gap(44.dp)))
        Spacer(Modifier.width(scale.gap(DesignSpace.l)))
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.trip_extra_title), style = scale.style(ExtraTitle), color = colors.homeText)
            Text(
                stringResource(R.string.trip_extra_sub, petName ?: stringResource(R.string.trip_pet_fallback)),
                style = scale.style(ExtraSub),
                color = colors.homeSubText,
            )
        }
    }
}

@Composable
private fun GreyButton(scale: DesignScale, colors: DesignColors, onClick: () -> Unit, content: @Composable () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = colors.homeIcon, contentColor = colors.homeText),
        shape = RoundedCornerShape(DesignRadius.m),
        elevation = null,
        contentPadding = PaddingValues(horizontal = scale.gap(DesignSpace.xl)),
        modifier = Modifier.heightIn(min = scale.touch(DesignHeight.buttonLarge)),
    ) { content() }
}

private val PreviewView = TripView(
    hospitalName = "별숲동물의료센터",
    remainingMinutes = 10,
    arrivesAt = LocalTime.of(15, 14),
    reply = TripReply.UNKNOWN,
    petName = "왕건이",
    waitText = null,
    relaxOn = true,
    relaxText = "켜짐 · 잔잔한 음악",
    canCall = true,
)

@Preview(widthDp = 1682, heightDp = 1210)
@Composable
private fun ParkedTripPreview() {
    MeongCareTheme(darkTheme = false) { Surface { ParkedTripScreen(PreviewView, {}, {}, {}, {}, {}) } }
}

