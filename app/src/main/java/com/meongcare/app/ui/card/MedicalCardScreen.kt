package com.meongcare.app.ui.card

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Box
import com.meongcare.app.ui.theme.DesignScale
import com.meongcare.app.ui.theme.DesignSpace
import com.meongcare.app.ui.theme.DesignType
import com.meongcare.app.ui.theme.LocalDesignTokens
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meongcare.app.R
import com.meongcare.app.domain.card.CardSessionState
import com.meongcare.app.domain.card.Symptom
import com.meongcare.app.domain.pet.MedicalInfo
import com.meongcare.app.domain.pet.Medication
import com.meongcare.app.domain.pet.PetProfile
import com.meongcare.app.domain.pet.PetProfileLoad
import com.meongcare.app.domain.pet.Sex
import com.meongcare.app.ui.guidance.GuidanceUiState
import com.meongcare.app.ui.guidance.SimulatedGuidanceNotice
import com.meongcare.app.ui.guidance.GuidancePanel
import com.meongcare.app.ui.guidance.TripPanel
import com.meongcare.app.ui.theme.ActionBlue
import com.meongcare.app.ui.theme.CardColors
import com.meongcare.app.ui.theme.Dimens
import com.meongcare.app.ui.theme.MeongCareTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** 이 폭 이상이면 컨셉 그림처럼 아바타를 왼쪽에, 정보를 오른쪽에 둔다 (Pleos 가로 전체 약 1682dp, 반쪽 창은 좁은 배치). */
private val WideBreakpoint = 840.dp

/** 이 폭 이상이면 아래 버튼 두 개를 나란히 둔다. */
private val SideBySideButtonsBreakpoint = 480.dp

/**
 * 화면 폭에 따른 크기. 넓은 차 화면에서는 팔을 뻗어 읽고 누르므로 글자와 버튼을 키운다.
 * (Pleos 타입 스케일 수치는 아직 확인하지 못했다. 임시값)
 */
data class CardScale(
    val title: TextUnit,
    val name: TextUnit,
    val row: TextUnit,
    val chip: TextUnit,
    val button: TextUnit,
    val buttonHeight: Dp,
    val avatar: Dp,
    val labelWidth: Dp,
) {
    companion object {
        val Wide = CardScale(title = 36.sp, name = 40.sp, row = 26.sp, chip = 20.sp, button = 26.sp, buttonHeight = 88.dp, avatar = 240.dp, labelWidth = 200.dp)
        val Narrow = CardScale(title = 28.sp, name = 30.sp, row = 22.sp, chip = 18.sp, button = 22.sp, buttonHeight = 72.dp, avatar = 96.dp, labelWidth = 132.dp)
    }
}

val LocalCardScale = staticCompositionLocalOf { CardScale.Narrow }

/**
 * 진료 카드 화면 (정차 중에만) = 확정 디자인 시안 4번 "진료 카드 확인". 모양만 바꾸고 동작은 이전과 같다.
 * 안내 상태에 따라 머리와 버튼이 다르다:
 * - 안내 없음: "진료 카드 확인" / [동물병원 찾기] (병원을 고르기 전이라 [병원에 전화]는 없다, 012)
 * - 안내 중: "진료 카드", 머리 아래 안내 영역 / [병원에 전화] [안내 취소]
 * - 도착: "진료 카드", 머리 아래 도착 안내 영역 / [병원에 전화] [닫기]
 *
 * 버튼은 시안 그대로 [←]·[음성 명령]·[동물병원 찾기]다 (사용자 지시 2026-11-13).
 * [←]는 이전으로: 안내 전이면 증상 선택(이전 [수정], 고른 증상이 남는다), 안내 중·도착이면 처음 화면(이전 [홈], 카드는 남는다).
 * 시스템 뒤로는 지금처럼 처음 화면이다 (HomeScreen의 BackHandler).
 * 안내 영역([GuidancePanel], 013)은 목적지·남은 시간·도착 예정·진행 막대·카드 전송과 병원 확인을 보인다 ([panel]).
 * [음성 명령]은 자리만 있다 (누르면 준비 중 안내, STT는 나중).
 * 카드 몸통은 세로로 스크롤하고, 아래 버튼 줄은 스크롤 밖에 고정한다 (긴 목록이어도 버튼이 밀려나지 않게).
 */
@Composable
fun MedicalCardScreen(
    state: CardFlowUiState,
    guidance: GuidanceUiState,
    actions: MedicalCardActions,
    onFindHospital: () -> Unit,
    onCancelGuidance: () -> Unit,
    onCloseArrival: () -> Unit,
    isPetProfileSample: Boolean,
    isGuidanceSimulated: Boolean,
    modifier: Modifier = Modifier,
    isRouteOnNavi: Boolean = false,
    panel: TripPanel? = null,
    onResend: () -> Unit = {},
) {
    val colors = LocalDesignTokens.current.colors
    var voiceNotice by rememberSaveable { mutableStateOf(false) }
    BoxWithConstraints(modifier = modifier.fillMaxSize().background(colors.homeBackground)) {
        val wide = maxWidth >= WideBreakpoint
        val scale = DesignScale.of(maxWidth)
        // 안내 영역(GuidancePanel)은 이전 크기 단계를 쓴다
        CompositionLocalProvider(LocalCardScale provides if (wide) CardScale.Wide else CardScale.Narrow) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = scale.gap(DesignSpace.xxxl), vertical = scale.gap(DesignSpace.l)),
            ) {
                CardTopBar(
                    guidance = guidance,
                    // 이전으로: 안내 전이면 증상 선택(이전 [수정]), 안내 중·도착이면 홈(이전 [홈])
                    onBack = if (guidance is GuidanceUiState.Idle) actions.onEditSymptoms else actions.onGoHome,
                    onVoiceCommand = { voiceNotice = true },
                    scale = scale,
                    colors = colors,
                )
                if (voiceNotice) {
                    Text(
                        stringResource(R.string.voice_not_ready),
                        style = scale.style(DesignType.body),
                        color = colors.selectedText,
                        modifier = Modifier.padding(start = scale.gap(DesignSpace.xxxl), top = scale.gap(DesignSpace.xs)),
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = scale.gap(DesignSpace.xxxl), vertical = scale.gap(DesignSpace.l)),
                    verticalArrangement = Arrangement.spacedBy(scale.gap(DesignSpace.l)),
                ) {
                    if (panel != null) GuidancePanel(panel, onResend)
                    state.shownPhoneNumber?.let {
                        PhoneNumberPanel(number = it, isSample = state.isHospitalSample, onDismiss = actions.onDismissPhoneNumber)
                    }
                    CardPetLine(state.card.pet, scale, colors)
                    CardRows(state.card, scale, colors)
                    CardNotes(
                        card = state.card,
                        showsPetSample = isPetProfileSample && state.card.pet is PetHeader.Registered,
                        hospitalSamplePhone = state.hospitalPhone.takeIf { state.isHospitalSample },
                        showsGuidanceSimulated = isGuidanceSimulated,
                        routeOnNavi = isRouteOnNavi,
                        scale = scale,
                        colors = colors,
                    )
                }
                Box(Modifier.padding(horizontal = scale.gap(DesignSpace.xxxl))) {
                    CardActions(
                        state = state,
                        guidance = guidance,
                        onCallHospital = actions.onCallHospital,
                        onFindHospital = onFindHospital,
                        onCancelGuidance = onCancelGuidance,
                        onCloseArrival = onCloseArrival,
                        scale = scale,
                        colors = colors,
                    )
                }
            }
        }
    }
}

/** 이름 ♂ + 칩. 반려견 정보가 없으면 안내 한 줄. 처음 화면도 같이 쓴다. */
@Composable
fun PetTitle(pet: PetHeader, modifier: Modifier = Modifier, color: Color = CardColors.Text) {
    val scale = LocalCardScale.current
    val nameStyle = MaterialTheme.typography.headlineMedium.copy(fontSize = scale.name, lineHeight = scale.name * 1.25f)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        when (pet) {
            is PetHeader.Registered -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = pet.name, style = nameStyle, color = color, fontWeight = FontWeight.Bold)
                    pet.sex?.let { sex ->
                        val description = sexDescription(sex)
                        Text(
                            text = " " + sexSymbol(sex),
                            style = nameStyle,
                            color = CardColors.Accent,
                            modifier = Modifier.semantics { contentDescription = description },
                        )
                    }
                }
                petChipText(pet)?.let { chip ->
                    Text(
                        text = chip,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = scale.chip, lineHeight = scale.chip * 1.3f),
                        color = CardColors.Navy,
                        modifier = Modifier
                            .background(CardColors.ChipBackground, RoundedCornerShape(10.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                    )
                }
            }
            else -> Text(text = petMissingText(pet).orEmpty(), style = MaterialTheme.typography.bodyLarge, color = color)
        }
    }
}

/** 전화 앱을 열지 못했을 때: 번호를 크게 보여 준다. 팝업 창이 아니라 화면 안 영역이다. 병원 확인 화면(012)도 쓴다. */
@Composable
internal fun PhoneNumberPanel(number: String, isSample: Boolean, onDismiss: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = CardColors.Surface,
        contentColor = CardColors.Text,
        border = BorderStroke(3.dp, ActionBlue),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.card_phone_panel_title), style = MaterialTheme.typography.bodyLarge)
            Text(number, style = MaterialTheme.typography.displayLarge, color = CardColors.Navy)
            if (com.meongcare.app.ui.SHOW_DEMO_NOTICES && isSample) Text(stringResource(R.string.card_phone_panel_sample), style = MaterialTheme.typography.labelMedium, color = CardColors.Muted)
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = ActionBlue, contentColor = Color.White),
                modifier = Modifier.heightIn(min = Dimens.MinTouchTarget).widthIn(min = 160.dp),
            ) { Text(stringResource(R.string.card_phone_panel_ok), style = MaterialTheme.typography.labelLarge) }
        }
    }
}

/** 시연용 표시 같은 작은 안내 한 줄. */
@Composable
fun Notice(text: String, modifier: Modifier = Modifier, style: TextStyle = MaterialTheme.typography.labelMedium) {
    Text(text = text, style = style, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = modifier)
}

/** 미리보기용 (ui는 data를 import하지 않는다). 값은 시연용 샘플 "보리"와 같다. */
internal val PreviewPet = PetProfile(
    name = "보리",
    breed = "골든리트리버",
    sex = Sex.MALE,
    birthDate = LocalDate.of(2014, 8, 1),
    birthDateIsEstimated = false,
    photoDataUrl = null,
    medical = MedicalInfo(listOf("심장질환"), listOf("특정 단백질 알레르기"), listOf(Medication("심장약", "1일 1회"))),
    updatedAt = Instant.parse("2026-11-05T00:00:00Z"),
)

internal fun previewCardState(shown: String? = null) = CardFlowUiState(
    session = CardSessionState.Ready(setOf(Symptom.COUGH, Symptom.BREATHING_DIFFICULTY)),
    card = medicalCardOf(
        PetProfileLoad.Loaded(PreviewPet),
        setOf(Symptom.COUGH, Symptom.BREATHING_DIFFICULTY),
        LocalDate.of(2026, 11, 5),
        ZoneId.of("Asia/Seoul"),
    ),
    hospitalName = "시연용 동물병원",
    hospitalPhone = "02-0000-0000",
    isHospitalSample = true,
    shownPhoneNumber = shown,
)

@Preview(widthDp = 1682, heightDp = 1210)
@Composable
private fun MedicalCardWidePreview() {
    MeongCareTheme(darkTheme = false) {
        Surface {
            MedicalCardScreen(previewCardState(), GuidanceUiState.Idle("시연용 동물병원"), MedicalCardActions(), {}, {}, {}, true, true)
        }
    }
}

@Preview(widthDp = 411, heightDp = 891)
@Composable
private fun MedicalCardNarrowPreview() {
    MeongCareTheme(darkTheme = false) {
        Surface {
            MedicalCardScreen(previewCardState(shown = "02-0000-0000"), GuidanceUiState.Guiding("시연용 동물병원", 12), MedicalCardActions(), {}, {}, {}, true, true)
        }
    }
}
