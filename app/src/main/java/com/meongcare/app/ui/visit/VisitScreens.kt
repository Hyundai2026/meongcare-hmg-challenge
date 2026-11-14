package com.meongcare.app.ui.visit

import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.meongcare.app.R
import com.meongcare.app.ui.card.MedicalCardUiState
import com.meongcare.app.ui.card.PetAvatar
import com.meongcare.app.ui.card.PetHeader
import com.meongcare.app.ui.card.ageText
import com.meongcare.app.ui.card.joinedText
import com.meongcare.app.ui.card.medicationText
import com.meongcare.app.ui.card.symptomLabelRes
import com.meongcare.app.ui.home.DesignDivider
import com.meongcare.app.ui.home.DesignSwitchTrack
import com.meongcare.app.ui.theme.DesignColors
import com.meongcare.app.ui.theme.DesignHeight
import com.meongcare.app.ui.theme.DesignRadius
import com.meongcare.app.ui.theme.DesignScale
import com.meongcare.app.ui.theme.DesignSpace
import com.meongcare.app.ui.theme.LocalDesignTokens
import com.meongcare.app.ui.voice.VoiceGuideLine
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/* 디자인 시안 "병원 도착" → "진료 완료" → "귀가 중" (문구·구조 그대로, 2026-11-14). 정차 중에만 보인다. */

private val Clock: DateTimeFormatter = DateTimeFormatter.ofPattern("a h:mm", Locale.KOREAN)

private val Title = TextStyle(fontSize = 40.sp, lineHeight = 50.sp, fontWeight = FontWeight.SemiBold)
private val Mode = TextStyle(fontSize = 30.sp, lineHeight = 40.sp)
private val Hero = TextStyle(fontSize = 64.sp, lineHeight = 74.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.02).em)
private val Calm = TextStyle(fontSize = 34.sp, lineHeight = 46.sp)
private val H2 = TextStyle(fontSize = 40.sp, lineHeight = 50.sp, fontWeight = FontWeight.SemiBold)
private val Key = TextStyle(fontSize = 30.sp, lineHeight = 40.sp)
private val Value = TextStyle(fontSize = 34.sp, lineHeight = 44.sp, fontWeight = FontWeight.SemiBold)
private val Body = TextStyle(fontSize = 30.sp, lineHeight = 42.sp)
private val RowLabel = TextStyle(fontSize = 34.sp, lineHeight = 44.sp, fontWeight = FontWeight.SemiBold)
private val PetName = TextStyle(fontSize = 40.sp, lineHeight = 50.sp, fontWeight = FontWeight.SemiBold)
private val Big = TextStyle(fontSize = 64.sp, lineHeight = 76.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.02).em)
private val ButtonText = TextStyle(fontSize = 34.sp, lineHeight = 42.sp, fontWeight = FontWeight.SemiBold)

/** 머리: 제목 ……… "● 응급 모드" (뒤로 버튼 없음) */
@Composable
private fun VisitHeader(title: String, scale: DesignScale, colors: DesignColors) {
    Row(Modifier.fillMaxWidth().heightIn(min = scale.touch(96.dp)), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = scale.style(Title), color = colors.homeText, modifier = Modifier.weight(1f))
        Box(Modifier.size(scale.gap(12.dp)).background(colors.primary, CircleShape))
        Spacer(Modifier.width(scale.gap(DesignSpace.xs)))
        Text(stringResource(R.string.card_emergency_mode), style = scale.style(Mode), color = colors.homeSubText)
    }
}

@Composable
private fun VisitButton(
    text: String,
    onClick: () -> Unit,
    primary: Boolean,
    scale: DesignScale,
    colors: DesignColors,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (primary) colors.primary else colors.homeIcon,
            contentColor = if (primary) colors.onPrimary else colors.homeText,
        ),
        shape = RoundedCornerShape(DesignRadius.m),
        elevation = null,
        contentPadding = PaddingValues(horizontal = scale.gap(DesignSpace.xl)),
        modifier = modifier.heightIn(min = scale.touch(DesignHeight.buttonLarge)),
    ) {
        icon?.let {
            Icon(it, contentDescription = null, modifier = Modifier.size(scale.gap(34.dp)))
            Spacer(Modifier.width(scale.gap(DesignSpace.s)))
        }
        Text(text, style = scale.style(ButtonText), textAlign = TextAlign.Center)
    }
}

@Composable
private fun KeyValue(key: String, value: String, scale: DesignScale, colors: DesignColors, muted: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = scale.gap(DesignSpace.m)), verticalAlignment = Alignment.CenterVertically) {
        Text(key, style = scale.style(Key), color = colors.homeSubText, modifier = Modifier.weight(1f))
        Text(value, style = scale.style(Value), color = if (muted) colors.homeSubText else colors.homeText, textAlign = TextAlign.End)
    }
}

/** 큰 스위치 줄 (이름 + 설명 + 스위치). 줄 전체를 누른다 */
@Composable
private fun SwitchRow(title: String, sub: String, on: Boolean, onToggle: () -> Unit, scale: DesignScale, colors: DesignColors) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = scale.touch(DesignHeight.buttonLarge))
            .toggleable(value = on, role = Role.Switch, onValueChange = { onToggle() })
            .padding(vertical = scale.gap(DesignSpace.m)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(scale.gap(DesignSpace.xxs))) {
            Text(title, style = scale.style(RowLabel), color = colors.homeText)
            Text(sub, style = scale.style(Body), color = colors.homeSubText)
        }
        Spacer(Modifier.width(scale.gap(DesignSpace.l)))
        DesignSwitchTrack(on, if (on) colors.primary else colors.homeSubText.copy(alpha = 0.75f), scale)
    }
}

/**
 * "병원 도착" (시안 arrive). 진료 카드 + 동물병원·진료 시작 예상 + "진료 중" + [차 안에서 대기] / [처음으로] [진료 완료].
 *
 * @param cardSent 진료 카드를 병원에 보냈는지 ("✓ 병원 전달 완료")
 * @param startText 진료 시작 예상 (모르면 null → "대기시간 미확인")
 */
@Composable
fun ArriveScreen(
    card: MedicalCardUiState,
    hospitalName: String,
    cardSent: Boolean,
    startText: String?,
    waitInCar: Boolean,
    onToggleWaitInCar: () -> Unit,
    onHome: () -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
    called: Boolean = false,
) {
    val colors = LocalDesignTokens.current.colors
    val petName = (card.pet as? PetHeader.Registered)?.name
    BoxWithConstraints(modifier.fillMaxSize().background(colors.homeBackground)) {
        val scale = DesignScale.of(maxWidth)
        Column(Modifier.fillMaxSize().padding(horizontal = scale.gap(DesignSpace.xxxl), vertical = scale.gap(DesignSpace.s))) {
            VisitHeader(stringResource(R.string.visit_arrive_title), scale, colors)
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                VoiceGuideLine(stringResource(if (called) R.string.visit_called_say else R.string.visit_arrive_say), scale, colors)
                Spacer(Modifier.height(scale.gap(DesignSpace.l)))
                Text(stringResource(R.string.visit_arrive_hero), style = scale.style(Hero), color = colors.homeText)
                Text(
                    petName?.let { stringResource(R.string.visit_arrive_calm, it) } ?: stringResource(R.string.visit_arrive_calm_none),
                    style = scale.style(Calm),
                    color = colors.selectedText,
                )
                Spacer(Modifier.height(scale.gap(DesignSpace.xl)))
                Row(horizontalArrangement = Arrangement.spacedBy(scale.gap(DesignSpace.l))) {
                    ArriveCard(card, cardSent, scale, colors, Modifier.weight(1f))
                    Column(Modifier.weight(1f)) {
                        DesignDivider(colors, scale)
                        KeyValue(stringResource(R.string.visit_hospital), hospitalName, scale, colors)
                        DesignDivider(colors, scale)
                        KeyValue(stringResource(R.string.trip_row_start), startText ?: stringResource(R.string.trip_wait_unknown), scale, colors)
                        DesignDivider(colors, scale)
                        Spacer(Modifier.height(scale.gap(DesignSpace.m)))
                        Text(stringResource(R.string.visit_in_care), style = scale.style(H2), color = colors.homeText)
                        Text(stringResource(R.string.visit_in_care_sub), style = scale.style(Body), color = colors.homeSubText)
                        Spacer(Modifier.height(scale.gap(DesignSpace.m)))
                        DesignDivider(colors, scale)
                        SwitchRow(
                            stringResource(R.string.visit_wait_in_car),
                            stringResource(
                                when {
                                    called -> R.string.visit_called_sub
                                    waitInCar -> R.string.visit_wait_in_car_on
                                    else -> R.string.visit_wait_in_car_off
                                },
                            ),
                            waitInCar,
                            onToggleWaitInCar,
                            scale,
                            colors,
                        )
                        if (called) {
                            Row(
                                Modifier
                                    .background(colors.successBackground, RoundedCornerShape(scale.gap(DesignRadius.m)))
                                    .padding(horizontal = scale.gap(DesignSpace.l), vertical = scale.gap(DesignSpace.m)),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = colors.success, modifier = Modifier.size(scale.gap(32.dp)))
                                Spacer(Modifier.width(scale.gap(DesignSpace.s)))
                                Text(stringResource(R.string.visit_called_chip), style = scale.style(RowLabel), color = colors.success)
                            }
                        }
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(top = scale.gap(DesignSpace.s)),
                horizontalArrangement = Arrangement.spacedBy(scale.gap(DesignSpace.l)),
            ) {
                Spacer(Modifier.weight(1f))
                VisitButton(stringResource(R.string.visit_to_start), onHome, primary = false, scale = scale, colors = colors, modifier = Modifier.weight(1f))
                VisitButton(stringResource(R.string.visit_complete), onComplete, primary = true, scale = scale, colors = colors, modifier = Modifier.weight(1f))
            }
        }
    }
}

/** 시안 MedCard: "진료 카드  ✓ 병원 전달 완료" / 반려견 / 주요 증상·지병·복용약 */
@Composable
private fun ArriveCard(card: MedicalCardUiState, sent: Boolean, scale: DesignScale, colors: DesignColors, modifier: Modifier) {
    val shape = RoundedCornerShape(scale.gap(DesignRadius.l))
    Column(
        modifier
            .border(scale.gap(2.dp).coerceAtLeast(1.dp), colors.homeLine, shape)
            .background(colors.background, shape)
            .padding(scale.gap(DesignSpace.xl)),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.visit_card), style = scale.style(Key), color = colors.homeSubText, modifier = Modifier.weight(1f))
            if (sent) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = colors.success, modifier = Modifier.size(scale.gap(30.dp)))
                Spacer(Modifier.width(scale.gap(DesignSpace.xs)))
                Text(stringResource(R.string.visit_card_sent), style = scale.style(Key).copy(fontWeight = FontWeight.SemiBold), color = colors.success)
            }
        }
        Spacer(Modifier.height(scale.gap(DesignSpace.l)))
        val pet = card.pet as? PetHeader.Registered
        Row(verticalAlignment = Alignment.CenterVertically) {
            PetAvatar(initial = pet?.initial, size = scale.gap(96.dp))
            Spacer(Modifier.width(scale.gap(DesignSpace.l)))
            Column {
                Text(pet?.name ?: stringResource(R.string.trip_unknown), style = scale.style(PetName), color = colors.homeText)
                val meta = listOfNotNull(pet?.breed, pet?.age?.let { ageText(it, pet.ageIsEstimated) })
                if (meta.isNotEmpty()) Text(meta.joinToString(stringResource(R.string.card_chip_separator)), style = scale.style(Body), color = colors.homeSubText)
            }
        }
        Spacer(Modifier.height(scale.gap(DesignSpace.l)))
        val none = stringResource(R.string.card_value_none_registered)
        DesignDivider(colors, scale)
        CardLine(
            stringResource(R.string.card_row_symptoms),
            card.symptoms.takeIf { it.isNotEmpty() }?.let { l -> joinedText(l.map { stringResource(symptomLabelRes(it)) }) }
                ?: stringResource(R.string.card_value_no_symptoms),
            scale,
            colors,
        )
        DesignDivider(colors, scale)
        CardLine(stringResource(R.string.visit_condition), card.medical?.conditions?.takeIf { it.isNotEmpty() }?.let { joinedText(it) } ?: none, scale, colors)
        DesignDivider(colors, scale)
        CardLine(
            stringResource(R.string.visit_medication),
            card.medical?.medications?.takeIf { it.isNotEmpty() }?.let { l -> joinedText(l.map { medicationText(it) }) } ?: none,
            scale,
            colors,
        )
    }
}

@Composable
private fun CardLine(key: String, value: String, scale: DesignScale, colors: DesignColors) {
    Row(Modifier.fillMaxWidth().padding(vertical = scale.gap(DesignSpace.m))) {
        Text(key, style = scale.style(Key), color = colors.homeSubText, modifier = Modifier.width(scale.gap(180.dp)))
        Text(value, style = scale.style(Body), color = colors.homeText, modifier = Modifier.weight(1f))
    }
}

/**
 * "진료 완료" (시안 after). 진료 안내(동물병원·진료 종료 시각·동물병원 안내·다음 진료) + 귀가 설정(릴렉스 모드) /
 * [진료 안내 듣기 · 구상] [귀가 출발]. 동물병원 안내는 병원에서 받는 기능이 아직 없어 "안내 없음", 다음 진료는 "미정"이다.
 */
@Composable
fun AfterVisitScreen(
    visit: Visit,
    relaxOn: Boolean,
    relaxSummary: String,
    onToggleRelax: () -> Unit,
    onRelaxSettings: () -> Unit,
    onGoHome: () -> Unit,
    modifier: Modifier = Modifier,
    zone: ZoneId = ZoneId.systemDefault(),
    record: RecordChoice = RecordChoice.ASK,
    linked: Boolean = false,
    onSave: () -> Unit = {},
    onSkip: () -> Unit = {},
) {
    val colors = LocalDesignTokens.current.colors
    var listened by rememberSaveable { mutableStateOf(false) }
    BoxWithConstraints(modifier.fillMaxSize().background(colors.homeBackground)) {
        val scale = DesignScale.of(maxWidth)
        Column(Modifier.fillMaxSize().padding(horizontal = scale.gap(DesignSpace.xxxl), vertical = scale.gap(DesignSpace.s))) {
            VisitHeader(stringResource(R.string.visit_after_title), scale, colors)
            // 시안 SAY.after: 저장 질문이 있으면 "병원에서 보낸 안내예요. 카드에 저장할까요?", [진료 안내 듣기]면 안내 읽기
            when {
                listened -> VoiceGuideLine(stringResource(R.string.visit_listen_none), scale, colors)
                record == RecordChoice.ASK -> VoiceGuideLine(stringResource(R.string.visit_after_say_ask), scale, colors)
            }
            Row(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(top = scale.gap(DesignSpace.l)),
                horizontalArrangement = Arrangement.spacedBy(scale.gap(DesignSpace.l)),
            ) {
                Column(Modifier.weight(1f)) {
                    DesignDivider(colors, scale)
                    Spacer(Modifier.height(scale.gap(DesignSpace.l)))
                    Text(stringResource(R.string.visit_after_guide), style = scale.style(H2), color = colors.homeText)
                    KeyValue(stringResource(R.string.visit_hospital), visit.hospitalName, scale, colors)
                    KeyValue(stringResource(R.string.visit_ended_at), visit.endedAt.atZone(zone).format(Clock), scale, colors)
                    KeyValue(stringResource(R.string.visit_hospital_notes), stringResource(R.string.visit_no_notes), scale, colors, muted = true)
                    KeyValue(stringResource(R.string.visit_next), stringResource(R.string.visit_next_none), scale, colors, muted = true)
                }
                Column(Modifier.weight(1f)) {
                    DesignDivider(colors, scale)
                    Spacer(Modifier.height(scale.gap(DesignSpace.l)))
                    Text(stringResource(R.string.visit_home_setting), style = scale.style(H2), color = colors.homeText)
                    Text(stringResource(R.string.visit_home_setting_sub), style = scale.style(Body), color = colors.homeSubText)
                    SwitchRow(stringResource(R.string.relax_title), relaxSummary, relaxOn, onToggleRelax, scale, colors)
                    if (relaxOn) {
                        VisitButton(stringResource(R.string.visit_relax_change), onRelaxSettings, primary = false, scale = scale, colors = colors)
                    }
                }
            }
            when (record) {
                RecordChoice.ASK -> Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = scale.gap(DesignSpace.s))
                        .background(colors.selected, RoundedCornerShape(scale.gap(DesignRadius.m)))
                        .padding(scale.gap(DesignSpace.m)),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(scale.gap(DesignSpace.m)),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.visit_record_ask), style = scale.style(RowLabel), color = colors.selectedText)
                        Text(stringResource(R.string.visit_record_ask_sub), style = scale.style(Body), color = colors.homeSubText)
                    }
                    VisitButton(stringResource(R.string.visit_record_skip), onSkip, primary = false, scale = scale, colors = colors)
                    VisitButton(stringResource(R.string.visit_record_save), onSave, primary = true, scale = scale, colors = colors)
                }
                RecordChoice.SAVED -> Text(
                    stringResource(if (linked) R.string.visit_record_saved_web else R.string.visit_record_saved),
                    style = scale.style(RowLabel).copy(fontWeight = FontWeight.Medium),
                    color = colors.selectedText,
                )
                RecordChoice.SKIPPED -> Text(
                    stringResource(R.string.visit_record_skipped),
                    style = scale.style(RowLabel).copy(fontWeight = FontWeight.Medium),
                    color = colors.selectedText,
                )
            }
            Row(Modifier.fillMaxWidth().padding(top = scale.gap(DesignSpace.s)), horizontalArrangement = Arrangement.spacedBy(scale.gap(DesignSpace.l))) {
                Spacer(Modifier.weight(1f))
                VisitButton(stringResource(R.string.visit_listen), { listened = true }, primary = false, scale = scale, colors = colors, modifier = Modifier.weight(1f))
                VisitButton(stringResource(R.string.visit_go_home), onGoHome, primary = true, scale = scale, colors = colors, modifier = Modifier.weight(1f), icon = Icons.Filled.Home)
            }
        }
    }
}

/**
 * "귀가 중" (시안 trip). "집까지 약 N분"(병원까지 걸린 이동 시간) + 릴렉스 상태 / 집에서 챙길 것 /
 * [진료 안내 듣기 · 구상] [릴렉스 모드 켜짐·꺼짐] [귀가 완료].
 */
@Composable
fun GoingHomeScreen(
    visit: Visit,
    relaxOn: Boolean,
    relaxSummary: String,
    onRelax: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDesignTokens.current.colors
    var listened by rememberSaveable { mutableStateOf(false) }
    BoxWithConstraints(modifier.fillMaxSize().background(colors.homeBackground)) {
        val scale = DesignScale.of(maxWidth)
        Column(Modifier.fillMaxSize().padding(horizontal = scale.gap(DesignSpace.xxxl), vertical = scale.gap(DesignSpace.s))) {
            VisitHeader(stringResource(R.string.visit_trip_title), scale, colors)
            if (listened) {
                VoiceGuideLine(stringResource(R.string.visit_listen_none), scale, colors)
            } else {
                visit.travelMinutes?.let { VoiceGuideLine(stringResource(R.string.visit_trip_say, it), scale, colors) }
            }
            Row(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(top = scale.gap(DesignSpace.xl)),
                horizontalArrangement = Arrangement.spacedBy(scale.gap(DesignSpace.xxl)),
            ) {
                Column(Modifier.weight(1f)) {
                    MovingDots(scale, colors)
                    Spacer(Modifier.height(scale.gap(DesignSpace.l)))
                    Text(
                        visit.travelMinutes?.let { stringResource(R.string.visit_trip_min, it) } ?: stringResource(R.string.visit_trip_title),
                        style = scale.style(Big),
                        color = colors.homeText,
                    )
                    Text(
                        if (relaxOn) stringResource(R.string.visit_trip_rx_on, relaxSummary) else stringResource(R.string.trip_relax_off),
                        style = scale.style(Calm),
                        color = if (relaxOn) colors.selectedText else colors.homeSubText,
                    )
                }
                Column(Modifier.weight(1f)) {
                    DesignDivider(colors, scale)
                    Spacer(Modifier.height(scale.gap(DesignSpace.l)))
                    Text(stringResource(R.string.visit_take_care), style = scale.style(H2), color = colors.homeText)
                    Spacer(Modifier.height(scale.gap(DesignSpace.s)))
                    Text(stringResource(R.string.visit_trip_no_notes), style = scale.style(Body), color = colors.homeSubText)
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = scale.gap(DesignSpace.s)), horizontalArrangement = Arrangement.spacedBy(scale.gap(DesignSpace.l))) {
                VisitButton(stringResource(R.string.visit_listen), { listened = true }, primary = false, scale = scale, colors = colors, modifier = Modifier.weight(1f))
                VisitButton(
                    stringResource(if (relaxOn) R.string.trip_relax_on else R.string.trip_relax_off),
                    onRelax,
                    primary = false,
                    scale = scale,
                    colors = colors,
                    modifier = Modifier.weight(1f),
                )
                VisitButton(stringResource(R.string.visit_trip_done), onDone, primary = true, scale = scale, colors = colors, modifier = Modifier.weight(1f))
            }
        }
    }
}

/** 귀가 중 점 세 개: 차례로 진해졌다 옅어진다 (시안 trip 점 애니메이션) */
@Composable
private fun MovingDots(scale: DesignScale, colors: DesignColors) {
    val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "dots")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 3f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            androidx.compose.animation.core.tween(durationMillis = 1_500, easing = androidx.compose.animation.core.LinearEasing),
        ),
        label = "phase",
    )
    Row(horizontalArrangement = Arrangement.spacedBy(scale.gap(DesignSpace.s))) {
        repeat(3) { i ->
            val on = phase.toInt() == i
            Box(
                Modifier
                    .size(scale.gap(16.dp))
                    .background(if (on) colors.primary else colors.primary.copy(alpha = 0.3f), CircleShape),
            )
        }
    }
}

/**
 * "귀가 완료" (시안 fin). 오늘 진료 기록(또는 저장 안 함) · 동물병원 · 다음 진료 / 추가 안내 없음 /
 * 릴렉스를 켰으면 이동 중 상태 기록([편안함 기록]·[불안함 기록]) / [처음으로].
 */
@Composable
fun HomeArrivedScreen(
    visit: Visit,
    petName: String?,
    record: RecordChoice,
    relaxOn: Boolean,
    feeling: TripFeeling?,
    onFeeling: (TripFeeling) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDesignTokens.current.colors
    BoxWithConstraints(modifier.fillMaxSize().background(colors.homeBackground)) {
        val scale = DesignScale.of(maxWidth)
        Column(Modifier.fillMaxSize().padding(horizontal = scale.gap(DesignSpace.xxxl), vertical = scale.gap(DesignSpace.s))) {
            Row(Modifier.fillMaxWidth().heightIn(min = scale.touch(96.dp)), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.visit_fin_title), style = scale.style(Title), color = colors.homeText)
            }
            Row(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(top = scale.gap(DesignSpace.l)),
                horizontalArrangement = Arrangement.spacedBy(scale.gap(DesignSpace.l)),
            ) {
                Column(Modifier.weight(1f)) {
                    DesignDivider(colors, scale)
                    Spacer(Modifier.height(scale.gap(DesignSpace.l)))
                    Text(
                        stringResource(if (record == RecordChoice.SAVED) R.string.visit_fin_today else R.string.visit_fin_not_saved),
                        style = scale.style(H2),
                        color = colors.homeText,
                    )
                    DesignDivider(colors, scale)
                    KeyValue(stringResource(R.string.visit_hospital), visit.hospitalName, scale, colors)
                    DesignDivider(colors, scale)
                    KeyValue(stringResource(R.string.visit_next), stringResource(R.string.visit_next_none), scale, colors, muted = true)
                    if (record == RecordChoice.SAVED) {
                        Text(stringResource(R.string.visit_fin_saved_hint), style = scale.style(Body), color = colors.homeSubText)
                    }
                }
                Column(Modifier.weight(1f)) {
                    DesignDivider(colors, scale)
                    Spacer(Modifier.height(scale.gap(DesignSpace.l)))
                    Text(stringResource(R.string.visit_fin_no_notes), style = scale.style(H2), color = colors.homeText)
                    Text(stringResource(R.string.visit_fin_rest), style = scale.style(Body), color = colors.homeSubText)
                    if (relaxOn) {
                        Spacer(Modifier.height(scale.gap(DesignSpace.l)))
                        if (feeling != null) {
                            Text(
                                stringResource(if (feeling == TripFeeling.CALM) R.string.visit_fin_fb_ok else R.string.visit_fin_fb_no),
                                style = scale.style(Body).copy(fontWeight = FontWeight.Medium),
                                color = colors.selectedText,
                            )
                        } else {
                            DesignDivider(colors, scale)
                            Spacer(Modifier.height(scale.gap(DesignSpace.m)))
                            Text(
                                petName?.let { stringResource(R.string.visit_fin_fb_q, it) } ?: stringResource(R.string.visit_fin_fb_q_none),
                                style = scale.style(Body),
                                color = colors.homeSubText,
                            )
                            Spacer(Modifier.height(scale.gap(DesignSpace.s)))
                            Row(horizontalArrangement = Arrangement.spacedBy(scale.gap(DesignSpace.m))) {
                                VisitButton(stringResource(R.string.visit_fin_fb_calm), { onFeeling(TripFeeling.CALM) }, primary = false, scale = scale, colors = colors, modifier = Modifier.weight(1f))
                                VisitButton(stringResource(R.string.visit_fin_fb_anxious), { onFeeling(TripFeeling.ANXIOUS) }, primary = false, scale = scale, colors = colors, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = scale.gap(DesignSpace.s))) {
                Spacer(Modifier.weight(2f))
                VisitButton(stringResource(R.string.visit_to_start), onDone, primary = true, scale = scale, colors = colors, modifier = Modifier.weight(1f))
            }
        }
    }
}
