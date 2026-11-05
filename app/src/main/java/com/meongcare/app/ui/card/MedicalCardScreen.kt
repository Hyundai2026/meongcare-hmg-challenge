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
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Send
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.meongcare.app.R
import com.meongcare.app.domain.card.CardSessionState
import com.meongcare.app.domain.card.Symptom
import com.meongcare.app.domain.pet.MedicalInfo
import com.meongcare.app.domain.pet.Medication
import com.meongcare.app.domain.pet.PetProfile
import com.meongcare.app.domain.pet.PetProfileLoad
import com.meongcare.app.domain.pet.Sex
import java.time.Instant
import com.meongcare.app.ui.guidance.GuidanceUiState
import com.meongcare.app.ui.guidance.SimulatedGuidanceNotice
import com.meongcare.app.ui.guidance.remainingTimeText
import com.meongcare.app.ui.theme.ActionBlue
import com.meongcare.app.ui.theme.CardColors
import com.meongcare.app.ui.theme.Dimens
import com.meongcare.app.ui.theme.MeongCareTheme
import java.time.LocalDate
import java.time.ZoneId

/** 이 폭 이상이면 컨셉 그림처럼 아바타를 왼쪽에, 정보를 오른쪽에 둔다 (Pleos 가로 전체 약 1682dp, 반쪽 창은 좁은 배치). */
private val WideBreakpoint = 840.dp

/** 이 폭 이상이면 아래 버튼 두 개를 나란히 둔다. */
private val SideBySideButtonsBreakpoint = 480.dp

private val ActionButtonHeight = 72.dp

/**
 * 진료 카드 화면 (정차 중에만). 안내 상태에 따라 머리와 버튼이 다르다:
 * - 안내 없음: "진료 카드 생성 완료" + [수정] [닫기] / [병원에 전화] [길 안내 시작]
 * - 안내 중: "병원까지 N분" + 병원 이름 + [수정] / [병원에 전화] [안내 취소]
 * - 도착: "도착했어요" + 병원 이름 + [수정] / [병원에 전화] [닫기]
 *
 * 카드 몸통은 세로로 스크롤하고, 아래 버튼 줄은 스크롤 밖에 고정한다 (긴 목록이어도 버튼이 밀려나지 않게).
 */
@Composable
fun MedicalCardScreen(
    state: CardFlowUiState,
    guidance: GuidanceUiState,
    actions: MedicalCardActions,
    onStartGuidance: () -> Unit,
    onCancelGuidance: () -> Unit,
    onCloseArrival: () -> Unit,
    isPetProfileSample: Boolean,
    isGuidanceSimulated: Boolean,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val wide = maxWidth >= WideBreakpoint
        val buttonsSideBySide = maxWidth >= SideBySideButtonsBreakpoint
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Dimens.ScreenPadding, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                CardHeader(state, guidance, actions, wide)
                state.shownPhoneNumber?.let {
                    PhoneNumberPanel(number = it, isSample = state.isHospitalSample, onDismiss = actions.onDismissPhoneNumber)
                }
                CardBody(card = state.card, wide = wide)
                if (isPetProfileSample && state.card.pet is PetHeader.Registered) {
                    Notice(stringResource(R.string.card_sample_pet_notice))
                }
            }
            ActionArea(
                state = state,
                guidance = guidance,
                sideBySide = buttonsSideBySide,
                onCallHospital = actions.onCallHospital,
                onStartGuidance = onStartGuidance,
                onCancelGuidance = onCancelGuidance,
                onCloseArrival = onCloseArrival,
                isGuidanceSimulated = isGuidanceSimulated,
            )
        }
    }
}

@Composable
private fun CardHeader(state: CardFlowUiState, guidance: GuidanceUiState, actions: MedicalCardActions, wide: Boolean) {
    val pet = state.card.pet
    val (title, subtitle) = when (guidance) {
        is GuidanceUiState.Idle -> stringResource(R.string.card_title_ready) to
            if (pet is PetHeader.Registered) stringResource(R.string.card_subtitle_ready, pet.name)
            else stringResource(R.string.card_subtitle_ready_no_pet)
        is GuidanceUiState.Guiding -> remainingTimeText(guidance) to guidance.destinationName
        is GuidanceUiState.Arrived -> stringResource(R.string.guidance_arrived) to guidance.destinationName
    }
    val buttons: @Composable () -> Unit = {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PillButton(stringResource(R.string.card_edit), onClick = actions.onEditSymptoms)
            if (guidance is GuidanceUiState.Idle) PillButton(stringResource(R.string.card_close), onClick = actions.onCloseCard)
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = ActionBlue, modifier = Modifier.size(56.dp)) {
                Icon(PawIcon, contentDescription = null, tint = Color.White, modifier = Modifier.padding(12.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.headlineMedium)
                Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (wide) buttons()
        }
        if (!wide) buttons()
    }
}

@Composable
private fun PillButton(text: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        shape = CircleShape,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.heightIn(min = Dimens.MinTouchTarget),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun CardBody(card: MedicalCardUiState, wide: Boolean) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = CardColors.Surface,
        contentColor = CardColors.Text,
        border = BorderStroke(1.dp, CardColors.Border),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            if (wide) {
                Row(modifier = Modifier.padding(28.dp)) {
                    PetAvatar(initial = (card.pet as? PetHeader.Registered)?.initial, size = 220.dp)
                    Spacer(Modifier.width(32.dp))
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        PetTitle(card.pet)
                        InfoRows(card, labelWidth = 180.dp)
                    }
                }
            } else {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PetAvatar(initial = (card.pet as? PetHeader.Registered)?.initial, size = 96.dp)
                        Spacer(Modifier.width(20.dp))
                        PetTitle(card.pet, modifier = Modifier.weight(1f))
                    }
                    InfoRows(card, labelWidth = 132.dp)
                }
            }
            DisclaimerBand(updatedOn = card.medical?.updatedOn)
        }
    }
}

/** 이름 ♂ + 칩. 반려견 정보가 없으면 안내 한 줄. */
@Composable
fun PetTitle(pet: PetHeader, modifier: Modifier = Modifier, color: Color = CardColors.Text) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when (pet) {
            is PetHeader.Registered -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = pet.name, style = MaterialTheme.typography.headlineMedium, color = color, fontWeight = FontWeight.Bold)
                    pet.sex?.let { sex ->
                        val description = sexDescription(sex)
                        Text(
                            text = " " + sexSymbol(sex),
                            style = MaterialTheme.typography.headlineMedium,
                            color = CardColors.Accent,
                            modifier = Modifier.semantics { contentDescription = description },
                        )
                    }
                }
                petChipText(pet)?.let { chip ->
                    Text(
                        text = chip,
                        style = MaterialTheme.typography.bodyMedium,
                        color = CardColors.Navy,
                        modifier = Modifier
                            .background(CardColors.ChipBackground, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                    )
                }
            }
            else -> Text(text = petMissingText(pet).orEmpty(), style = MaterialTheme.typography.bodyLarge, color = color)
        }
    }
}

@Composable
private fun InfoRows(card: MedicalCardUiState, labelWidth: Dp) {
    val none = stringResource(R.string.card_value_none_registered)
    Column {
        InfoRow(
            icon = Icons.Outlined.Info,
            label = stringResource(R.string.card_row_symptoms),
            value = card.symptoms.takeIf { it.isNotEmpty() }?.let { list -> joinedText(list.map { stringResource(symptomLabelRes(it)) }) },
            emptyText = stringResource(R.string.card_value_no_symptoms),
            labelWidth = labelWidth,
        )
        card.medical?.let { medical ->
            HorizontalDivider(color = CardColors.Border)
            InfoRow(Icons.Outlined.FavoriteBorder, stringResource(R.string.card_row_conditions), medical.conditions.joinedOrNull(), none, labelWidth)
            HorizontalDivider(color = CardColors.Border)
            InfoRow(Icons.Outlined.Warning, stringResource(R.string.card_row_allergies), medical.allergies.joinedOrNull(), none, labelWidth)
            HorizontalDivider(color = CardColors.Border)
            InfoRow(
                Icons.Outlined.CheckCircle,
                stringResource(R.string.card_row_medications),
                medical.medications.takeIf { it.isNotEmpty() }?.let { list -> joinedText(list.map { medicationText(it) }) },
                none,
                labelWidth,
            )
        }
    }
}

@Composable
private fun List<String>.joinedOrNull(): String? = takeIf { it.isNotEmpty() }?.let { joinedText(it) }

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String?, emptyText: String, labelWidth: Dp) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.Top) {
        Icon(icon, contentDescription = null, tint = CardColors.Accent, modifier = Modifier.padding(top = 3.dp).size(24.dp))
        Spacer(Modifier.width(10.dp))
        Text(text = label, style = MaterialTheme.typography.bodyLarge, color = CardColors.Muted, modifier = Modifier.width(labelWidth))
        Text(
            text = value ?: emptyText,
            style = MaterialTheme.typography.bodyLarge,
            color = if (value == null) CardColors.Muted else CardColors.Text,
            fontWeight = if (value == null) FontWeight.Normal else FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun DisclaimerBand(updatedOn: LocalDate?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardColors.LabelBackground)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(Icons.Outlined.Info, contentDescription = null, tint = CardColors.Accent, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.card_disclaimer), style = MaterialTheme.typography.labelMedium, color = CardColors.Muted)
            updatedOn?.let {
                Text(updatedOnText(it), style = MaterialTheme.typography.labelMedium, color = CardColors.Muted)
            }
        }
    }
}

/** 전화 앱을 열지 못했을 때: 번호를 크게 보여 준다. 팝업 창이 아니라 화면 안 영역이다. */
@Composable
private fun PhoneNumberPanel(number: String, isSample: Boolean, onDismiss: () -> Unit) {
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
    onStartGuidance: () -> Unit,
    onCancelGuidance: () -> Unit,
    onCloseArrival: () -> Unit,
    isGuidanceSimulated: Boolean,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(start = Dimens.ScreenPadding, end = Dimens.ScreenPadding, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val call: (@Composable (Modifier) -> Unit)? = state.hospitalPhone?.let {
            { modifier ->
                OutlinedButton(
                    onClick = onCallHospital,
                    border = BorderStroke(2.dp, ActionBlue),
                    shape = RoundedCornerShape(16.dp),
                    modifier = modifier.heightIn(min = ActionButtonHeight),
                ) {
                    Icon(Icons.Filled.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.card_call_hospital), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
        val main: @Composable (Modifier) -> Unit = { modifier ->
            when (guidance) {
                is GuidanceUiState.Idle -> FilledAction(stringResource(R.string.card_start_guidance), Icons.Filled.Send, onStartGuidance, modifier)
                is GuidanceUiState.Guiding -> OutlinedButton(
                    onClick = onCancelGuidance,
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(16.dp),
                    modifier = modifier.heightIn(min = ActionButtonHeight),
                ) { Text(stringResource(R.string.guidance_cancel), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface) }
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
        if (state.isHospitalSample && state.hospitalPhone != null) {
            Notice(stringResource(R.string.card_sample_hospital_notice, state.hospitalPhone))
        }
        if (isGuidanceSimulated) SimulatedGuidanceNotice()
    }
}

@Composable
private fun FilledAction(text: String, icon: ImageVector?, onClick: () -> Unit, modifier: Modifier) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = ActionBlue, contentColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.heightIn(min = ActionButtonHeight),
    ) {
        icon?.let {
            Icon(it, contentDescription = null)
            Spacer(Modifier.width(12.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/** 시연용 표시 같은 작은 안내 한 줄. */
@Composable
fun Notice(text: String, modifier: Modifier = Modifier) {
    Text(text = text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = modifier)
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

private fun previewState(guidancePhone: String? = "02-0000-0000", shown: String? = null) = CardFlowUiState(
    session = CardSessionState.Ready(setOf(Symptom.COUGH, Symptom.BREATHING_DIFFICULTY)),
    card = medicalCardOf(
        PetProfileLoad.Loaded(PreviewPet),
        setOf(Symptom.COUGH, Symptom.BREATHING_DIFFICULTY),
        LocalDate.of(2026, 11, 5),
        ZoneId.of("Asia/Seoul"),
    ),
    hospitalName = "시연용 동물병원",
    hospitalPhone = guidancePhone,
    isHospitalSample = true,
    shownPhoneNumber = shown,
)

@Preview(widthDp = 1682, heightDp = 1210)
@Composable
private fun MedicalCardWidePreview() {
    MeongCareTheme(darkTheme = true) {
        Surface {
            MedicalCardScreen(previewState(), GuidanceUiState.Idle("시연용 동물병원"), MedicalCardActions(), {}, {}, {}, true, true)
        }
    }
}

@Preview(widthDp = 411, heightDp = 891)
@Composable
private fun MedicalCardNarrowPreview() {
    MeongCareTheme(darkTheme = false) {
        Surface {
            MedicalCardScreen(previewState(shown = "02-0000-0000"), GuidanceUiState.Guiding("시연용 동물병원", 12), MedicalCardActions(), {}, {}, {}, true, true)
        }
    }
}
