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
 * 진료 카드 화면 (정차 중에만). 안내 상태에 따라 머리와 버튼이 다르다:
 * - 안내 없음: "진료 카드 생성 완료" + [홈] [수정] / [가장 빨리 진료받을 곳 찾기] (병원을 고르기 전이라 [병원에 전화]는 없다, 012)
 * - 안내 중: "진료 카드" + [홈] [수정], 머리 아래 안내 영역 / [병원에 전화] [안내 취소]
 * - 도착: "진료 카드" + [홈] [수정], 머리 아래 도착 안내 영역 / [병원에 전화] [닫기]
 *
 * [홈]은 카드를 지우지 않고 처음 화면으로 간다 (처음 화면에 [카드 보기]가 있다).
 * 안내 영역([GuidancePanel], 013)은 목적지·남은 시간·도착 예정·진행 막대·카드 전송과 병원 확인을 보인다 ([panel]).
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
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val wide = maxWidth >= WideBreakpoint
        val buttonsSideBySide = maxWidth >= SideBySideButtonsBreakpoint
        CompositionLocalProvider(LocalCardScale provides if (wide) CardScale.Wide else CardScale.Narrow) {
            Column(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = Dimens.ScreenPadding, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    CardHeader(state, guidance, actions, wide)
                    if (panel != null) GuidancePanel(panel, onResend)
                    state.shownPhoneNumber?.let {
                        PhoneNumberPanel(number = it, isSample = state.isHospitalSample, onDismiss = actions.onDismissPhoneNumber)
                    }
                    CardBody(card = state.card, wide = wide)
                    DemoNotices(
                        showsPetSample = isPetProfileSample && state.card.pet is PetHeader.Registered,
                        hospitalSamplePhone = state.hospitalPhone.takeIf { state.isHospitalSample },
                        showsGuidanceSimulated = isGuidanceSimulated,
                        routeOnNavi = isRouteOnNavi,
                    )
                }
                ActionArea(
                    state = state,
                    guidance = guidance,
                    sideBySide = buttonsSideBySide,
                    onCallHospital = actions.onCallHospital,
                    onFindHospital = onFindHospital,
                    onCancelGuidance = onCancelGuidance,
                    onCloseArrival = onCloseArrival,
                )
            }
        }
    }
}

@Composable
private fun CardHeader(state: CardFlowUiState, guidance: GuidanceUiState, actions: MedicalCardActions, wide: Boolean) {
    val scale = LocalCardScale.current
    val pet = state.card.pet
    // 안내 중·도착이면 남은 시간·병원은 머리 아래 안내 영역이 보인다. 머리는 "진료 카드"로 둔다 (013)
    val title = stringResource(if (guidance is GuidanceUiState.Idle) R.string.card_title_ready else R.string.card_title_trip)
    val subtitle = if (pet is PetHeader.Registered) stringResource(R.string.card_subtitle_ready, pet.name)
    else stringResource(R.string.card_subtitle_ready_no_pet)
    val titleBlock: @Composable (Modifier) -> Unit = { modifier ->
        Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = ActionBlue, modifier = Modifier.size(if (wide) 64.dp else 52.dp)) {
                Icon(PawIcon, contentDescription = null, tint = Color.White, modifier = Modifier.padding(12.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.headlineMedium.copy(fontSize = scale.title, lineHeight = scale.title * 1.25f))
                Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    val homeButton: @Composable () -> Unit = {
        PillButton(text = stringResource(R.string.card_home), icon = Icons.Filled.Home, onClick = actions.onGoHome)
    }
    val editButton: @Composable () -> Unit = {
        PillButton(text = stringResource(R.string.card_edit), icon = null, onClick = actions.onEditSymptoms)
    }
    if (wide) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            homeButton()
            Spacer(Modifier.width(24.dp))
            titleBlock(Modifier.weight(1f))
            editButton()
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                homeButton()
                editButton()
            }
            titleBlock(Modifier)
        }
    }
}

@Composable
private fun PillButton(text: String, icon: ImageVector?, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        shape = CircleShape,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.heightIn(min = Dimens.MinTouchTarget),
    ) {
        icon?.let {
            Icon(it, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text = text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun CardBody(card: MedicalCardUiState, wide: Boolean) {
    val scale = LocalCardScale.current
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = CardColors.Surface,
        contentColor = CardColors.Text,
        border = BorderStroke(1.dp, CardColors.Border),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            if (wide) {
                Row(modifier = Modifier.padding(32.dp)) {
                    PetAvatar(initial = (card.pet as? PetHeader.Registered)?.initial, size = scale.avatar)
                    Spacer(Modifier.width(36.dp))
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        PetTitle(card.pet)
                        InfoRows(card)
                    }
                }
            } else {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PetAvatar(initial = (card.pet as? PetHeader.Registered)?.initial, size = scale.avatar)
                        Spacer(Modifier.width(20.dp))
                        PetTitle(card.pet, modifier = Modifier.weight(1f))
                    }
                    InfoRows(card)
                }
            }
            DisclaimerBand(updatedOn = card.medical?.updatedOn)
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

@Composable
private fun InfoRows(card: MedicalCardUiState) {
    val none = stringResource(R.string.card_value_none_registered)
    Column {
        // 주요 증상은 이번 진료의 핵심이라 강조 띠로 보인다
        InfoRow(
            icon = Icons.Outlined.Info,
            label = stringResource(R.string.card_row_symptoms),
            value = card.symptoms.takeIf { it.isNotEmpty() }?.let { list -> joinedText(list.map { stringResource(symptomLabelRes(it)) }) },
            emptyText = stringResource(R.string.card_value_no_symptoms),
            highlighted = true,
        )
        card.medical?.let { medical ->
            HorizontalDivider(color = CardColors.Border)
            InfoRow(Icons.Outlined.FavoriteBorder, stringResource(R.string.card_row_conditions), medical.conditions.joinedOrNull(), none)
            HorizontalDivider(color = CardColors.Border)
            InfoRow(Icons.Outlined.Warning, stringResource(R.string.card_row_allergies), medical.allergies.joinedOrNull(), none)
            HorizontalDivider(color = CardColors.Border)
            InfoRow(
                Icons.Outlined.CheckCircle,
                stringResource(R.string.card_row_medications),
                medical.medications.takeIf { it.isNotEmpty() }?.let { list -> joinedText(list.map { medicationText(it) }) },
                none,
            )
        }
    }
}

@Composable
private fun List<String>.joinedOrNull(): String? = takeIf { it.isNotEmpty() }?.let { joinedText(it) }

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String?, emptyText: String, highlighted: Boolean = false) {
    val scale = LocalCardScale.current
    val style = MaterialTheme.typography.bodyLarge.copy(fontSize = scale.row, lineHeight = scale.row * 1.35f)
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.Top) {
        Icon(icon, contentDescription = null, tint = CardColors.Accent, modifier = Modifier.padding(top = 4.dp).size(28.dp))
        Spacer(Modifier.width(12.dp))
        Text(text = label, style = style, color = CardColors.Muted, modifier = Modifier.width(scale.labelWidth))
        val valueModifier = if (highlighted && value != null) {
            Modifier
                .background(CardColors.SymptomBackground, RoundedCornerShape(10.dp))
                .padding(horizontal = 14.dp, vertical = 2.dp)
        } else {
            Modifier
        }
        Text(
            text = value ?: emptyText,
            style = style,
            color = when {
                value == null -> CardColors.Muted
                highlighted -> CardColors.SymptomText
                else -> CardColors.Text
            },
            fontWeight = if (value == null) FontWeight.Normal else FontWeight.Bold,
            modifier = Modifier.weight(1f, fill = false).then(valueModifier),
        )
    }
}

@Composable
private fun DisclaimerBand(updatedOn: LocalDate?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardColors.LabelBackground)
            .padding(horizontal = 28.dp, vertical = 18.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(Icons.Outlined.Info, contentDescription = null, tint = CardColors.Accent, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.card_disclaimer), style = MaterialTheme.typography.bodyMedium, color = CardColors.Muted)
            updatedOn?.let {
                Text(updatedOnText(it), style = MaterialTheme.typography.labelMedium, color = CardColors.Muted)
            }
        }
    }
}

/** 시연용 표시를 한 상자에 모은다 (출품 유의사항: 사실과 다르면 선정 취소). 주행 화면에는 넣지 않는다. */
@Composable
private fun DemoNotices(showsPetSample: Boolean, hospitalSamplePhone: String?, showsGuidanceSimulated: Boolean, routeOnNavi: Boolean = false) {
    if (!showsPetSample && hospitalSamplePhone == null && !showsGuidanceSimulated) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (showsPetSample) Notice(stringResource(R.string.card_sample_pet_notice))
            hospitalSamplePhone?.let { Notice(stringResource(R.string.card_sample_hospital_notice, it)) }
            if (showsGuidanceSimulated) SimulatedGuidanceNotice(routeOnNavi = routeOnNavi)
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
            if (isSample) Text(stringResource(R.string.card_phone_panel_sample), style = MaterialTheme.typography.labelMedium, color = CardColors.Muted)
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = ActionBlue, contentColor = Color.White),
                modifier = Modifier.heightIn(min = Dimens.MinTouchTarget).widthIn(min = 160.dp),
            ) { Text(stringResource(R.string.card_phone_panel_ok), style = MaterialTheme.typography.labelLarge) }
        }
    }
}

@Composable
private fun ActionArea(
    state: CardFlowUiState,
    guidance: GuidanceUiState,
    sideBySide: Boolean,
    onCallHospital: () -> Unit,
    onFindHospital: () -> Unit,
    onCancelGuidance: () -> Unit,
    onCloseArrival: () -> Unit,
) {
    val scale = LocalCardScale.current
    val label = MaterialTheme.typography.labelLarge.copy(fontSize = scale.button)
    Column(
        modifier = Modifier.fillMaxWidth().padding(start = Dimens.ScreenPadding, end = Dimens.ScreenPadding, top = 8.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val call: (@Composable (Modifier) -> Unit)? = state.hospitalPhone?.let {
            { modifier ->
                OutlinedButton(
                    onClick = onCallHospital,
                    border = BorderStroke(2.dp, ActionBlue),
                    shape = RoundedCornerShape(20.dp),
                    modifier = modifier.heightIn(min = scale.buttonHeight),
                ) {
                    Icon(Icons.Filled.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(30.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.card_call_hospital), style = label, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
        val main: @Composable (Modifier) -> Unit = { modifier ->
            when (guidance) {
                is GuidanceUiState.Idle -> FilledAction(stringResource(R.string.card_find_hospital), Icons.Filled.Search, onFindHospital, modifier)
                is GuidanceUiState.Guiding -> OutlinedButton(
                    onClick = onCancelGuidance,
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(20.dp),
                    modifier = modifier.heightIn(min = scale.buttonHeight),
                ) { Text(stringResource(R.string.guidance_cancel), style = label, color = MaterialTheme.colorScheme.onSurface) }
                is GuidanceUiState.Arrived -> FilledAction(stringResource(R.string.card_close), null, onCloseArrival, modifier)
            }
        }
        if (sideBySide) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                call?.invoke(Modifier.weight(1f))
                main(Modifier.weight(1f))
            }
        } else {
            call?.invoke(Modifier.fillMaxWidth())
            main(Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun FilledAction(text: String, icon: ImageVector?, onClick: () -> Unit, modifier: Modifier) {
    val scale = LocalCardScale.current
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = ActionBlue, contentColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.heightIn(min = scale.buttonHeight),
    ) {
        icon?.let {
            Icon(it, contentDescription = null, modifier = Modifier.size(30.dp))
            Spacer(Modifier.width(12.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge.copy(fontSize = scale.button))
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
    MeongCareTheme(darkTheme = true) {
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
