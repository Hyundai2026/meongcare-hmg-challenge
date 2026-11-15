package com.meongcare.app.ui.card

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.meongcare.app.R
import com.meongcare.app.domain.card.Symptom
import com.meongcare.app.domain.voice.VoiceFailure
import com.meongcare.app.ui.home.DesignDivider
import com.meongcare.app.ui.home.HomeIcons
import com.meongcare.app.ui.theme.DesignColors
import com.meongcare.app.ui.theme.DesignHeight
import com.meongcare.app.ui.theme.DesignRadius
import com.meongcare.app.ui.theme.DesignScale
import com.meongcare.app.ui.theme.DesignSpace
import com.meongcare.app.ui.theme.LocalDesignTokens
import com.meongcare.app.ui.theme.MeongCareTheme
import com.meongcare.app.ui.voice.VoiceActions
import com.meongcare.app.ui.voice.VoiceScreenState
import com.meongcare.app.ui.voice.VoiceUiState
import com.meongcare.app.ui.withSubjectParticle

private val HeaderTitle = TextStyle(fontSize = 34.sp, lineHeight = 44.sp, fontWeight = FontWeight.Medium)
private val HeaderSub = TextStyle(fontSize = 34.sp, lineHeight = 44.sp)
private val HeaderPet = TextStyle(fontSize = 30.sp, lineHeight = 40.sp)
private val HeaderPetName = TextStyle(fontSize = 34.sp, lineHeight = 44.sp, fontWeight = FontWeight.SemiBold)
private val Title = TextStyle(fontSize = 48.sp, lineHeight = 60.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.02).em)
private val Subtitle = TextStyle(fontSize = 30.sp, lineHeight = 42.sp)
private val Count = TextStyle(fontSize = 32.sp, lineHeight = 42.sp, fontWeight = FontWeight.Medium)
private val CardLabel = TextStyle(fontSize = 34.sp, lineHeight = 44.sp)
private val ButtonText = TextStyle(fontSize = 34.sp, lineHeight = 42.sp, fontWeight = FontWeight.SemiBold)
private val VoiceButtonText = TextStyle(fontSize = 32.sp, lineHeight = 40.sp, fontWeight = FontWeight.Medium)
private val Hint = TextStyle(fontSize = 28.sp, lineHeight = 38.sp)
private val StatusTitle = TextStyle(fontSize = 32.sp, lineHeight = 42.sp, fontWeight = FontWeight.Medium)

/** 선택한 카드의 테두리 (시안: 선택 바탕보다 진한 파랑) */
private val SelectedBorder = Color(0xFF9DB0E0)

/** 이 폭보다 좁으면 4열 대신 2열 (폰 세로) */
private val FourColumnMinWidth = 840.dp

/**
 * 증상 고르기 (정차 중에만) = 확정 디자인 시안 3번 "응급 진료 요청 · 증상 선택".
 * 글자 입력 대신 큰 체크 카드로 여러 개를 고른다. 카드는 [Symptom] 선언 순서로 4열 × 3줄(좁은 화면은 2열)이다.
 *
 * 동작은 이전 화면과 같다 (모양만 바꿈):
 * - [←] = [onCancel] (HomeScreen이 처음 화면으로 가는 onGoHome을 넘긴다: 고르던 것은 취소, 만든 카드는 남음), [증상 확인하기] = 이전 [카드 만들기] ([onConfirm]).
 * - [음성으로 설명하기] = 이전 [말로 증상 고르기] (010 음성 입력, [VoiceActions.onStart]). 듣는 중이면 [듣기 취소],
 *   다 들었거나 실패하면 [다시 말하기]. 알아들은 말·고른 증상·실패 안내는 버튼 옆 안내 자리에 보인다.
 * - 음성 입력이 없는 구성([voice]가 null이거나 쓸 수 없음)이면 음성 버튼을 그리지 않는다.
 * 시안의 반려동물 [변경]은 넣지 않았다. 샘플 반려견은 정차 중 홈의 [⇄ 변경]에서만 고른다 (021 D3).
 *
 * @param petName 머리와 제목에 쓰는 반려견 이름. 없으면 "반려동물"
 */
@Composable
fun SymptomPickerScreen(
    selected: Set<Symptom>,
    onToggle: (Symptom) -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    voice: VoiceScreenState? = null,
    voiceActions: VoiceActions = VoiceActions(),
    petName: String? = null,
) {
    val colors = LocalDesignTokens.current.colors
    BoxWithConstraints(modifier = modifier.fillMaxSize().background(colors.homeBackground)) {
        val scale = DesignScale.of(maxWidth)
        val columns = if (maxWidth >= FourColumnMinWidth) 4 else 2
        val voiceOn = voice != null && voice.available
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = scale.gap(DesignSpace.xxxl), vertical = scale.gap(DesignSpace.l))) {
            Header(petName, onCancel, scale, colors)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = scale.gap(DesignSpace.page), vertical = scale.gap(DesignSpace.m)),
            ) {
                Text(
                    if (petName.isNullOrBlank()) stringResource(R.string.symptoms_title_unnamed)
                    else stringResource(R.string.symptoms_title_named, withSubjectParticle(petName)),
                    style = scale.style(Title),
                    color = colors.homeText,
                )
                Spacer(Modifier.height(scale.gap(DesignSpace.s)))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        stringResource(R.string.symptoms_select_all),
                        style = scale.style(Subtitle),
                        color = colors.homeSubText,
                        modifier = Modifier.weight(1f),
                    )
                    if (selected.isNotEmpty()) {
                        Spacer(Modifier.width(scale.gap(DesignSpace.l)))
                        Text(stringResource(R.string.symptoms_selected_count, selected.size), style = scale.style(Count), color = colors.primary)
                    }
                }
                Spacer(Modifier.height(scale.gap(DesignSpace.m)))
                Symptom.entries.chunked(columns).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(scale.gap(DesignSpace.s))) {
                        row.forEach { symptom ->
                            SymptomToggle(
                                symptom = symptom,
                                checked = symptom in selected,
                                onToggle = { onToggle(symptom) },
                                scale = scale,
                                colors = colors,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        // 마지막 줄이 덜 차도 카드 폭을 맞춘다
                        repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                    Spacer(Modifier.height(scale.gap(DesignSpace.s)))
                }
            }
            BottomBar(selected, voice.takeIf { voiceOn }, voiceActions, onConfirm, scale, colors)
        }
    }
}

/** "← 응급 진료 요청 · 증상 선택"  ……  "반려동물 왕건이" */
@Composable
private fun Header(petName: String?, onBack: () -> Unit, scale: DesignScale, colors: DesignColors) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.material3.IconButton(onClick = onBack, modifier = Modifier.size(scale.touch(96.dp))) {
            Icon(HomeIcons.Back, contentDescription = stringResource(R.string.symptoms_cancel), tint = colors.homeText, modifier = Modifier.size(scale.gap(40.dp)))
        }
        Spacer(Modifier.width(scale.gap(DesignSpace.l)))
        Text(stringResource(R.string.symptoms_header), style = scale.style(HeaderTitle), color = colors.homeText)
        Text(" · " + stringResource(R.string.symptoms_header_step), style = scale.style(HeaderSub), color = colors.homeSubText)
        Spacer(Modifier.weight(1f))
        if (!petName.isNullOrBlank()) {
            Text(stringResource(R.string.symptoms_header_pet), style = scale.style(HeaderPet), color = colors.homeSubText)
            Spacer(Modifier.width(scale.gap(DesignSpace.m)))
            Text(petName, style = scale.style(HeaderPetName), color = colors.homeText, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** 체크 카드 하나 (시안 128보다 낮춘 104, 사용자 요청: 4열 × 3줄, 왼쪽 이름, 오른쪽 체크 칸). 색만으로 알리지 않게 체크 표시와 접근성 상태(checkbox)를 준다 */
@Composable
private fun SymptomToggle(
    symptom: Symptom,
    checked: Boolean,
    onToggle: () -> Unit,
    scale: DesignScale,
    colors: DesignColors,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(scale.gap(DesignRadius.m))
    Row(
        modifier = modifier
            .heightIn(min = scale.touch(104.dp))
            .background(if (checked) colors.selected else colors.surface, shape)
            .border(scale.gap(2.dp).coerceAtLeast(1.dp), if (checked) SelectedBorder else colors.homeLine, shape)
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = { onToggle() })
            .padding(horizontal = scale.gap(DesignSpace.xxl)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(symptomLabelRes(symptom)),
            style = scale.style(CardLabel),
            color = colors.homeText,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(scale.gap(DesignSpace.s)))
        val box = scale.gap(44.dp)
        val boxShape = RoundedCornerShape(scale.gap(DesignRadius.s) * 0.75f)
        Box(
            modifier = Modifier
                .size(box)
                .background(if (checked) colors.primary else Color.Transparent, boxShape)
                .border(scale.gap(2.dp).coerceAtLeast(1.dp), if (checked) colors.primary else colors.homeSubText.copy(alpha = 0.5f), boxShape),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) Icon(Icons.Filled.Check, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(box * 0.7f))
        }
    }
}

/** 음성 안내 자리: 대기 중이면 "목록에 없는 증상은 말로 알려 주세요.", 그 밖에는 010과 같은 상태 글. 바뀌면 화면 읽기 프로그램이 읽어 준다 */
@Composable
private fun VoiceHintOrStatus(voice: VoiceUiState, scale: DesignScale, colors: DesignColors, modifier: Modifier) {
    val (title, detail) = when (voice) {
        VoiceUiState.Idle -> stringResource(R.string.symptoms_voice_hint) to null
        VoiceUiState.Starting -> stringResource(R.string.voice_starting) to null
        is VoiceUiState.Listening -> stringResource(R.string.voice_listening) to voice.partial.ifBlank { null }
        is VoiceUiState.Heard -> stringResource(R.string.voice_heard, voice.sentence) to
            if (voice.matched.isEmpty()) stringResource(R.string.voice_not_matched)
            else stringResource(R.string.voice_matched, joinedText(voice.matched.map { stringResource(symptomLabelRes(it)) }))
        is VoiceUiState.Failed -> stringResource(failureTextRes(voice.reason)) to null
    }
    Column(modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
        Text(
            title,
            style = scale.style(Hint),
            color = if (voice == VoiceUiState.Idle) colors.homeSubText else colors.homeText,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (detail != null) {
            Text(detail, style = scale.style(Hint), color = colors.selectedText, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** 아래: [🎤 음성으로 설명하기] + 안내 ……… [증상 확인하기] */
@Composable
private fun BottomBar(
    selected: Set<Symptom>,
    voice: VoiceScreenState?,
    voiceActions: VoiceActions,
    onConfirm: () -> Unit,
    scale: DesignScale,
    colors: DesignColors,
) {
    Column(modifier = Modifier.padding(horizontal = scale.gap(DesignSpace.page))) {
        DesignDivider(colors, scale)
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = scale.gap(DesignSpace.m)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (voice != null) {
                VoiceButton(voice.voice, voiceActions, scale, colors)
                Spacer(Modifier.width(scale.gap(DesignSpace.l)))
                // 대기 중이면 안내, 듣는 중·들은 뒤·실패면 그 상태를 같은 자리에 보인다 (시연용 음성 문구는 넣지 않는다, 사용자 결정 2026-11-13)
                VoiceHintOrStatus(voice.voice, scale, colors, Modifier.weight(1f))
            } else {
                Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.width(scale.gap(DesignSpace.l)))
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary),
                shape = RoundedCornerShape(DesignRadius.m),
                elevation = null,
                contentPadding = PaddingValues(horizontal = scale.gap(DesignSpace.xxxl)),
                modifier = Modifier.heightIn(min = scale.touch(DesignHeight.buttonLarge)).widthIn(min = scale.gap(420.dp)),
            ) {
                Text(stringResource(R.string.symptoms_check), style = scale.style(ButtonText))
            }
        }
    }
}

/** 대기·다 들음·실패면 [음성으로 설명하기]/[다시 말하기] (onStart), 켜는 중·듣는 중이면 [듣기 취소] (onCancel) */
@Composable
private fun VoiceButton(voice: VoiceUiState, actions: VoiceActions, scale: DesignScale, colors: DesignColors) {
    val listening = voice == VoiceUiState.Starting || voice is VoiceUiState.Listening
    Button(
        onClick = if (listening) actions.onCancel else actions.onStart,
        colors = ButtonDefaults.buttonColors(containerColor = colors.homeIcon, contentColor = colors.homeText),
        shape = RoundedCornerShape(DesignRadius.m),
        elevation = null,
        contentPadding = PaddingValues(horizontal = scale.gap(DesignSpace.xl)),
        modifier = Modifier.heightIn(min = scale.touch(DesignHeight.buttonLarge)),
    ) {
        Icon(HomeIcons.Mic, contentDescription = null, tint = colors.primary, modifier = Modifier.size(scale.gap(32.dp)))
        Spacer(Modifier.width(scale.gap(DesignSpace.s)))
        Text(
            stringResource(
                when {
                    listening -> R.string.voice_cancel
                    voice == VoiceUiState.Idle -> R.string.symptoms_voice_explain
                    else -> R.string.voice_retry
                },
            ),
            style = scale.style(VoiceButtonText),
        )
    }
}

private fun failureTextRes(reason: VoiceFailure): Int = when (reason) {
    VoiceFailure.NO_SPEECH -> R.string.voice_failed_no_speech
    VoiceFailure.MIC -> R.string.voice_failed_mic
    VoiceFailure.UNAVAILABLE -> R.string.voice_failed_unavailable
}

@Preview(widthDp = 1682, heightDp = 1210)
@Composable
private fun SymptomPickerWidePreview() {
    MeongCareTheme(darkTheme = false) {
        Surface {
            SymptomPickerScreen(
                selected = setOf(Symptom.VOMITING, Symptom.BREATHING_DIFFICULTY),
                onToggle = {}, onCancel = {}, onConfirm = {},
                voice = VoiceScreenState(available = true, isSimulated = true, voice = VoiceUiState.Idle),
                petName = "왕건이",
            )
        }
    }
}

@Preview(widthDp = 1682, heightDp = 1210)
@Composable
private fun SymptomPickerHeardPreview() {
    MeongCareTheme(darkTheme = false) {
        Surface {
            SymptomPickerScreen(
                selected = setOf(Symptom.COUGH, Symptom.BREATHING_DIFFICULTY, Symptom.VOMITING),
                onToggle = {}, onCancel = {}, onConfirm = {},
                voice = VoiceScreenState(
                    available = true,
                    isSimulated = false,
                    voice = VoiceUiState.Heard("강아지가 갑자기 토하고 숨을 가쁘게 쉬어요", listOf(Symptom.BREATHING_DIFFICULTY, Symptom.VOMITING)),
                ),
                petName = "왕건이",
            )
        }
    }
}

@Preview(widthDp = 411, heightDp = 891)
@Composable
private fun SymptomPickerNarrowPreview() {
    MeongCareTheme(darkTheme = false) {
        Surface {
            SymptomPickerScreen(
                selected = setOf(Symptom.COUGH),
                onToggle = {}, onCancel = {}, onConfirm = {},
                voice = VoiceScreenState(available = true, isSimulated = true, voice = VoiceUiState.Idle),
            )
        }
    }
}
