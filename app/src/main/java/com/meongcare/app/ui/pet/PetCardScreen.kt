package com.meongcare.app.ui.pet

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meongcare.app.R
import com.meongcare.app.data.pet.SamplePetProfiles
import com.meongcare.app.domain.pet.PetProfileLoad
import com.meongcare.app.ui.card.PetAvatar
import com.meongcare.app.ui.card.ageText
import com.meongcare.app.ui.card.joinedText
import com.meongcare.app.ui.card.medicationText
import com.meongcare.app.ui.home.DesignDivider
import com.meongcare.app.ui.home.HomeIcons
import com.meongcare.app.ui.home.PageHeader
import com.meongcare.app.ui.theme.DesignColors
import com.meongcare.app.ui.theme.DesignScale
import com.meongcare.app.ui.theme.DesignSpace
import com.meongcare.app.ui.theme.DesignType
import com.meongcare.app.ui.theme.LocalDesignTokens
import com.meongcare.app.ui.theme.MeongCareTheme
import java.time.LocalDate

private val SectionLabel = TextStyle(fontSize = 30.sp, lineHeight = 40.sp, fontWeight = FontWeight.SemiBold)
private val RowKey = TextStyle(fontSize = 30.sp, lineHeight = 42.sp)
private val RowValue = TextStyle(fontSize = 34.sp, lineHeight = 48.sp)
private val RecordsTitle = TextStyle(fontSize = 34.sp, lineHeight = 44.sp, fontWeight = FontWeight.SemiBold)

/**
 * 반려동물 카드 (019, 프로토타입 12번). **보기 전용**이고 정차 중에만 보인다 (`parkedContentOf`).
 * 정보 수정은 보호자 웹에서 한다. 체중·주치 병원·특징은 프로필 형식에 없어 "미등록"이다 (D4).
 * 진료 기록 기능은 없어서 "아직 진료 기록이 없어요"만 보인다 (없는 기능을 약속하지 않는다).
 *
 * @param relaxSummary "환경 설정" 칸의 릴렉스 요약 (예: "기본 설정", "켜짐 · 잔잔한 음악")
 */
@Composable
fun PetCardScreen(
    state: PetCardUiState,
    relaxSummary: String,
    isSample: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDesignTokens.current.colors
    BoxWithConstraints(modifier = modifier.fillMaxSize().background(colors.homeBackground)) {
        val scale = DesignScale.of(maxWidth)
        val twoColumns = maxWidth >= 840.dp
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = scale.gap(DesignSpace.xxxl), vertical = scale.gap(DesignSpace.l)),
        ) {
            PageHeader(stringResource(R.string.pet_card_title), onBack, scale, colors)
            Spacer(Modifier.height(scale.gap(DesignSpace.xl)))
            when (state) {
                is PetCardUiState.Registered -> RegisteredBody(state, relaxSummary, isSample, twoColumns, scale, colors)
                else -> Text(
                    stringResource(
                        when (state) {
                            PetCardUiState.Loading -> R.string.card_pet_loading
                            PetCardUiState.NotRegistered -> R.string.card_pet_not_registered
                            else -> R.string.card_pet_unavailable
                        },
                    ),
                    style = scale.style(DesignType.bodyLarge),
                    color = colors.homeSubText,
                    modifier = Modifier.padding(start = scale.gap(DesignSpace.xxxl)),
                )
            }
        }
    }
}

@Composable
private fun RegisteredBody(
    pet: PetCardUiState.Registered,
    relaxSummary: String,
    isSample: Boolean,
    twoColumns: Boolean,
    scale: DesignScale,
    colors: DesignColors,
) {
    val sep = stringResource(R.string.pet_card_separator)
    val species = stringResource(R.string.pet_card_species)
    val age = pet.age?.let { ageText(it, pet.ageIsEstimated) }
    val none = stringResource(R.string.pet_card_unregistered)

    Column(modifier = Modifier.padding(horizontal = scale.gap(DesignSpace.xxxl))) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PetAvatar(initial = pet.initial, size = scale.gap(104.dp))
            Spacer(Modifier.width(scale.gap(28.dp)))
            Column(verticalArrangement = Arrangement.spacedBy(scale.gap(6.dp))) {
                Text(pet.name, style = scale.style(DesignType.title), color = colors.homeText)
                Text(
                    listOfNotNull(species, pet.breed, age).joinToString(sep),
                    style = scale.style(DesignType.body),
                    color = colors.homeSubText,
                )
            }
        }
        Spacer(Modifier.height(scale.gap(DesignSpace.xxl)))

        val health = @Composable { m: Modifier ->
            Section(stringResource(R.string.pet_card_section_health), scale, colors, m) {
                InfoRow(stringResource(R.string.pet_card_conditions), pet.conditions.takeIf { it.isNotEmpty() }?.let { joinedText(it) }, none, scale, colors, first = true)
                InfoRow(
                    stringResource(R.string.pet_card_medications),
                    pet.medications.takeIf { it.isNotEmpty() }?.let { list -> joinedText(list.map { medicationText(it) }) },
                    none, scale, colors,
                )
                InfoRow(stringResource(R.string.pet_card_allergies), pet.allergies.takeIf { it.isNotEmpty() }?.let { joinedText(it) }, none, scale, colors)
                InfoRow(stringResource(R.string.pet_card_vet), null, none, scale, colors)
            }
        }
        val basic = @Composable { m: Modifier ->
            Column(m) {
                Section(stringResource(R.string.pet_card_section_basic), scale, colors) {
                    InfoRow(stringResource(R.string.pet_card_species_breed), listOfNotNull(species, pet.breed).joinToString(sep), none, scale, colors, first = true)
                    InfoRow(
                        stringResource(R.string.pet_card_birth),
                        pet.birthDate?.let { d ->
                            listOfNotNull(stringResource(R.string.pet_card_birth_value, d.year, d.monthValue, d.dayOfMonth), age).joinToString(sep)
                        },
                        none, scale, colors,
                    )
                    InfoRow(stringResource(R.string.pet_card_weight), null, none, scale, colors)
                    InfoRow(stringResource(R.string.pet_card_traits), null, none, scale, colors)
                }
                Spacer(Modifier.height(scale.gap(DesignSpace.xxl)))
                Section(stringResource(R.string.pet_card_section_env), scale, colors) {
                    InfoRow(stringResource(R.string.pet_card_relax), relaxSummary, none, scale, colors, first = true)
                }
            }
        }
        if (twoColumns) {
            Row(horizontalArrangement = Arrangement.spacedBy(scale.gap(72.dp))) {
                health(Modifier.weight(1f))
                basic(Modifier.weight(1f))
            }
        } else {
            health(Modifier.fillMaxWidth())
            Spacer(Modifier.height(scale.gap(DesignSpace.xxl)))
            basic(Modifier.fillMaxWidth())
        }

        Spacer(Modifier.height(scale.gap(DesignSpace.xxl)))
        Text(stringResource(R.string.pet_card_edit_hint), style = scale.style(DesignType.body), color = colors.homeSubText)
        if (isSample) {
            Text(stringResource(R.string.card_sample_pet_notice), style = scale.style(DesignType.body), color = colors.homeSubText)
        }

        Spacer(Modifier.height(scale.gap(DesignSpace.xxxl)))
        Text(stringResource(R.string.pet_card_records), style = scale.style(RecordsTitle), color = colors.homeText)
        Spacer(Modifier.height(scale.gap(DesignSpace.m)))
        DesignDivider(colors, scale)
        Row(
            modifier = Modifier.padding(vertical = scale.gap(DesignSpace.l)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(HomeIcons.Record, contentDescription = null, tint = colors.homeSubText, modifier = Modifier.size(scale.gap(40.dp)))
            Spacer(Modifier.width(scale.gap(DesignSpace.l)))
            Text(stringResource(R.string.pet_card_no_records), style = scale.style(RowValue), color = colors.homeText)
        }
    }
}

/** 구역: 이름표 + 위·아래 선 */
@Composable
private fun Section(title: String, scale: DesignScale, colors: DesignColors, modifier: Modifier = Modifier, rows: @Composable () -> Unit) {
    Column(modifier) {
        Text(title, style = scale.style(SectionLabel), color = colors.homeSubText, modifier = Modifier.padding(bottom = scale.gap(DesignSpace.xxs)))
        DesignDivider(colors, scale)
        rows()
        DesignDivider(colors, scale)
    }
}

/** 한 줄: 이름(200) + 값. 값이 없으면 "미등록"을 흐리게 */
@Composable
private fun InfoRow(key: String, value: String?, none: String, scale: DesignScale, colors: DesignColors, first: Boolean = false) {
    if (!first) DesignDivider(colors, scale)
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = scale.gap(20.dp)), verticalAlignment = Alignment.CenterVertically) {
        Text(key, style = scale.style(RowKey), color = colors.homeSubText, modifier = Modifier.width(scale.gap(240.dp)))
        Spacer(Modifier.width(scale.gap(DesignSpace.l)))
        Text(
            value ?: none,
            style = scale.style(RowValue),
            color = if (value != null) colors.homeText else colors.homeSubText,
            modifier = Modifier.weight(1f),
        )
    }
}

@Preview(widthDp = 1682, heightDp = 1210)
@Composable
private fun PetCardScreenPreview() {
    MeongCareTheme(darkTheme = false) {
        Surface {
            PetCardScreen(
                state = petCardOf(PetProfileLoad.Loaded(SamplePetProfiles.demoPet), LocalDate.of(2026, 11, 13)),
                relaxSummary = "기본 설정",
                isSample = true,
                onBack = {},
            )
        }
    }
}
