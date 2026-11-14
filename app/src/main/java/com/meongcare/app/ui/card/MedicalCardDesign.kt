package com.meongcare.app.ui.card

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.meongcare.app.R
import com.meongcare.app.ui.guidance.GuidanceUiState
import com.meongcare.app.ui.guidance.SimulatedGuidanceNotice
import com.meongcare.app.ui.home.DesignDivider
import com.meongcare.app.ui.home.HomeIcons
import com.meongcare.app.ui.theme.DesignColors
import com.meongcare.app.ui.theme.DesignHeight
import com.meongcare.app.ui.theme.DesignRadius
import com.meongcare.app.ui.theme.DesignScale
import com.meongcare.app.ui.theme.DesignSpace
import com.meongcare.app.ui.voice.VoiceCommandButton

/* 진료 카드 화면의 디자인 시안 4번 "진료 카드 확인" 조각. 동작은 MedicalCardScreen이 넘긴 콜백 그대로다. */

private val TopTitle = TextStyle(fontSize = 40.sp, lineHeight = 50.sp, fontWeight = FontWeight.SemiBold)
private val ModeLabel = TextStyle(fontSize = 30.sp, lineHeight = 40.sp)
private val PetName = TextStyle(fontSize = 48.sp, lineHeight = 58.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.02).em)
private val PetLine = TextStyle(fontSize = 32.sp, lineHeight = 42.sp)
private val RowKey = TextStyle(fontSize = 30.sp, lineHeight = 44.sp)
private val RowValue = TextStyle(fontSize = 36.sp, lineHeight = 48.sp, fontWeight = FontWeight.Medium)
private val SummaryValue = TextStyle(fontSize = 32.sp, lineHeight = 44.sp)
private val Small = TextStyle(fontSize = 26.sp, lineHeight = 36.sp)
private val ButtonText = TextStyle(fontSize = 34.sp, lineHeight = 42.sp, fontWeight = FontWeight.SemiBold)

/**
 * 머리 (시안 그대로): [←] "진료 카드 확인"(안내 중·도착이면 "진료 카드")  ……  "● 응급 모드"  [음성 명령]
 * [←]는 이전 화면으로: 안내 전에는 증상 선택(고른 증상이 남아 고칠 수 있다), 안내 중·도착이면 홈 (카드는 남는다).
 */
@Composable
internal fun CardTopBar(
    guidance: GuidanceUiState,
    onBack: () -> Unit,
    onVoiceCommand: () -> Unit,
    scale: DesignScale,
    colors: DesignColors,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack, modifier = Modifier.size(scale.touch(96.dp))) {
            Icon(HomeIcons.Back, contentDescription = stringResource(R.string.page_back), tint = colors.homeText, modifier = Modifier.size(scale.gap(40.dp)))
        }
        Spacer(Modifier.width(scale.gap(DesignSpace.l)))
        Text(
            stringResource(if (guidance is GuidanceUiState.Idle) R.string.card_design_title else R.string.card_title_trip),
            style = scale.style(TopTitle),
            color = colors.homeText,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Box(Modifier.size(scale.gap(12.dp)).background(colors.primary, CircleShape))
        Spacer(Modifier.width(scale.gap(DesignSpace.xs)))
        Text(stringResource(R.string.card_emergency_mode), style = scale.style(ModeLabel), color = colors.homeSubText)
        Spacer(Modifier.width(scale.gap(DesignSpace.l)))
        VoiceCommandButton(onClick = onVoiceCommand, scale = scale, colors = colors)
    }
}

/** 반려견 줄: 아바타 + 이름 / "골든리트리버 · 12세 3개월 · 수컷". 정보가 없으면 안내 한 줄 */
@Composable
internal fun CardPetLine(pet: PetHeader, scale: DesignScale, colors: DesignColors) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        PetAvatar(initial = (pet as? PetHeader.Registered)?.initial, size = scale.gap(84.dp))
        Spacer(Modifier.width(scale.gap(DesignSpace.l)))
        Column(verticalArrangement = Arrangement.spacedBy(scale.gap(DesignSpace.xxs))) {
            when (pet) {
                is PetHeader.Registered -> {
                    Text(pet.name, style = scale.style(PetName), color = colors.homeText)
                    val parts = listOfNotNull(
                        pet.breed,
                        pet.age?.let { ageText(it, pet.ageIsEstimated) },
                        pet.sex?.let { sexDescription(it) },
                    )
                    if (parts.isNotEmpty()) {
                        Text(parts.joinToString(stringResource(R.string.card_chip_separator)), style = scale.style(PetLine), color = colors.homeSubText)
                    }
                }
                else -> Text(petMissingText(pet).orEmpty(), style = scale.style(PetLine), color = colors.homeSubText)
            }
        }
    }
}

/** 카드 줄: 주요 증상 / 기존 병력 / 알레르기 / 복용 약물 / 수의사 전달 (선으로 나눈다) */
@Composable
internal fun CardRows(card: MedicalCardUiState, scale: DesignScale, colors: DesignColors) {
    val none = stringResource(R.string.card_value_none_registered)
    val symptoms = card.symptoms.takeIf { it.isNotEmpty() }?.let { list -> joinedText(list.map { stringResource(symptomLabelRes(it)) }) }
    val conditions = card.medical?.conditions?.takeIf { it.isNotEmpty() }?.let { joinedText(it) }
    Column {
        DesignDivider(colors, scale)
        CardRow(stringResource(R.string.card_row_symptoms), symptoms, stringResource(R.string.card_value_no_symptoms), scale, colors, emphasized = true)
        card.medical?.let { medical ->
            DesignDivider(colors, scale)
            CardRow(stringResource(R.string.card_row_conditions), conditions, none, scale, colors)
            DesignDivider(colors, scale)
            CardRow(stringResource(R.string.card_row_allergies), medical.allergies.takeIf { it.isNotEmpty() }?.let { joinedText(it) }, none, scale, colors)
            DesignDivider(colors, scale)
            CardRow(
                stringResource(R.string.card_row_medications),
                medical.medications.takeIf { it.isNotEmpty() }?.let { list -> joinedText(list.map { medicationText(it) }) },
                none,
                scale,
                colors,
            )
        }
        // 수의사에게 한 줄로 전할 요약 (시안 "수의사 전달"): 이름 · 증상 · 기존 병력
        val summary = listOfNotNull((card.pet as? PetHeader.Registered)?.name, symptoms, conditions)
        if (summary.isNotEmpty()) {
            DesignDivider(colors, scale)
            CardRow(
                stringResource(R.string.card_row_vet_summary),
                summary.joinToString(stringResource(R.string.card_chip_separator)),
                none,
                scale,
                colors,
                valueStyle = SummaryValue,
            )
        }
        DesignDivider(colors, scale)
    }
}

@Composable
private fun CardRow(
    key: String,
    value: String?,
    emptyText: String,
    scale: DesignScale,
    colors: DesignColors,
    emphasized: Boolean = false,
    valueStyle: TextStyle = RowValue,
) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = scale.gap(DesignSpace.l)), verticalAlignment = Alignment.Top) {
        Text(key, style = scale.style(RowKey), color = colors.homeSubText, modifier = Modifier.width(scale.gap(230.dp)))
        Text(
            value ?: emptyText,
            style = scale.style(valueStyle).let { if (emphasized && value != null) it.copy(fontWeight = FontWeight.SemiBold) else it },
            color = when {
                value == null -> colors.homeSubText
                emphasized -> colors.selectedText
                else -> colors.homeText
            },
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * 카드 아래 작은 글: 의료 진단이 아님 + 정보 갱신일 + 시연용 표시 (출품 유의사항: 사실과 다르면 선정 취소).
 * 이전 화면의 안내 띠·시연 상자와 같은 내용이다.
 */
@Composable
internal fun CardNotes(
    card: MedicalCardUiState,
    showsPetSample: Boolean,
    hospitalSamplePhone: String?,
    showsGuidanceSimulated: Boolean,
    routeOnNavi: Boolean,
    scale: DesignScale,
    colors: DesignColors,
) {
    Column(verticalArrangement = Arrangement.spacedBy(scale.gap(DesignSpace.xxs))) {
        Text(stringResource(R.string.card_disclaimer), style = scale.style(Small), color = colors.homeSubText)
        card.medical?.updatedOn?.let { Text(updatedOnText(it), style = scale.style(Small), color = colors.homeSubText) }
        if (com.meongcare.app.ui.SHOW_DEMO_NOTICES && showsPetSample) Text(stringResource(R.string.card_sample_pet_notice), style = scale.style(Small), color = colors.homeSubText)
        hospitalSamplePhone?.takeIf { com.meongcare.app.ui.SHOW_DEMO_NOTICES }?.let { Text(stringResource(R.string.card_sample_hospital_notice, it), style = scale.style(Small), color = colors.homeSubText) }
        if (com.meongcare.app.ui.SHOW_DEMO_NOTICES && showsGuidanceSimulated) SimulatedGuidanceNotice(routeOnNavi = routeOnNavi)
    }
}

/**
 * 아래 버튼 (이전과 같은 동작):
 * - 안내 없음: [동물병원 찾기] (병원을 고르기 전이라 [병원에 전화]는 없다, 012)
 * - 안내 중: [병원에 전화] [안내 취소]
 * - 도착: [병원에 전화] [닫기]
 */
@Composable
internal fun CardActions(
    state: CardFlowUiState,
    guidance: GuidanceUiState,
    onCallHospital: () -> Unit,
    onFindHospital: () -> Unit,
    onCancelGuidance: () -> Unit,
    onCloseArrival: () -> Unit,
    scale: DesignScale,
    colors: DesignColors,
) {
    Column {
        DesignDivider(colors, scale)
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = scale.gap(DesignSpace.l)),
            horizontalArrangement = Arrangement.spacedBy(scale.gap(DesignSpace.m), Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (guidance !is GuidanceUiState.Idle && state.hospitalPhone != null) {
                DesignButton(stringResource(R.string.card_call_hospital), onCallHospital, primary = false, scale = scale, colors = colors, phoneIcon = true)
            }
            when (guidance) {
                is GuidanceUiState.Idle -> DesignButton(stringResource(R.string.card_find_vet), onFindHospital, primary = true, scale = scale, colors = colors, icon = HomeIcons.Hospital)
                is GuidanceUiState.Guiding -> DesignButton(stringResource(R.string.guidance_cancel), onCancelGuidance, primary = false, scale = scale, colors = colors)
                is GuidanceUiState.Arrived -> DesignButton(stringResource(R.string.card_close), onCloseArrival, primary = true, scale = scale, colors = colors)
            }
        }
    }
}

@Composable
private fun DesignButton(
    text: String,
    onClick: () -> Unit,
    primary: Boolean,
    scale: DesignScale,
    colors: DesignColors,
    phoneIcon: Boolean = false,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (primary) colors.primary else colors.homeIcon,
            contentColor = if (primary) colors.onPrimary else colors.homeText,
        ),
        shape = RoundedCornerShape(DesignRadius.m),
        elevation = null,
        contentPadding = PaddingValues(horizontal = scale.gap(DesignSpace.xxxl)),
        modifier = Modifier.heightIn(min = scale.touch(DesignHeight.buttonLarge)).widthIn(min = scale.gap(if (primary) 420.dp else 300.dp)),
    ) {
        icon?.let {
            Icon(it, contentDescription = null, modifier = Modifier.size(scale.gap(36.dp)))
            Spacer(Modifier.width(scale.gap(DesignSpace.s)))
        }
        if (phoneIcon) {
            Icon(Icons.Filled.Phone, contentDescription = null, tint = colors.primary, modifier = Modifier.size(scale.gap(32.dp)))
            Spacer(Modifier.width(scale.gap(DesignSpace.s)))
        }
        Text(text, style = scale.style(ButtonText), color = Color.Unspecified)
    }
}
