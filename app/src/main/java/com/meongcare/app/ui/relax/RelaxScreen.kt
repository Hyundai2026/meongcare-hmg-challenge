package com.meongcare.app.ui.relax

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.meongcare.app.R
import com.meongcare.app.ui.home.DesignDivider
import com.meongcare.app.ui.home.DesignSwitchTrack
import com.meongcare.app.ui.home.PageHeader
import com.meongcare.app.ui.theme.DesignColors
import com.meongcare.app.ui.theme.DesignHeight
import com.meongcare.app.ui.theme.DesignRadius
import com.meongcare.app.ui.theme.DesignScale
import com.meongcare.app.ui.theme.DesignSpace
import com.meongcare.app.ui.theme.DesignType
import com.meongcare.app.ui.theme.LocalDesignTokens
import com.meongcare.app.ui.theme.MeongCareTheme

private val Heading = TextStyle(fontSize = 52.sp, lineHeight = 62.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.02).em)
private val SectionLabel = TextStyle(fontSize = 30.sp, lineHeight = 40.sp, fontWeight = FontWeight.SemiBold)
private val RowLabel = TextStyle(fontSize = 36.sp, lineHeight = 48.sp, fontWeight = FontWeight.Medium)
private val SubLabel = TextStyle(fontSize = 32.sp, lineHeight = 44.sp)
private val Segment = TextStyle(fontSize = 34.sp, lineHeight = 44.sp)
private val DoneButton = TextStyle(fontSize = 34.sp, lineHeight = 40.sp, fontWeight = FontWeight.SemiBold)

/** 꺼진 동안 아래 설정의 투명도 (프로토타입 0.4) */
private const val OFF_ALPHA = 0.4f

/**
 * 릴렉스 모드 (019, 프로토타입 11번). **시연용 화면**이다: 차량 공조·음악에 적용하지 않고, 화면 아래에 늘 그렇게 알린다.
 * 현재 환경 값은 차에서 읽은 것이 아니어서 "(예시)"를 붙인다. 프로토타입의 "환경 설정 >"(상세 공조 설정)는 넣지 않았다.
 * 글자 입력은 없다. 정차 중에만 보인다.
 *
 * @param petName null이면 "반려동물을 위한 편안한 이동"
 * @param onDone [완료]·[←]: 처음 화면으로
 */
@Composable
fun RelaxScreen(
    settings: RelaxSettings,
    petName: String?,
    onSetEnabled: (Boolean) -> Unit,
    onSetSound: (RelaxSound) -> Unit,
    onSetExtraReassurance: (Boolean) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDesignTokens.current.colors
    BoxWithConstraints(modifier = modifier.background(colors.homeBackground)) {
        val scale = DesignScale.of(maxWidth)
        val screenHeight = maxHeight
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .heightIn(min = screenHeight)
                .fillMaxWidth()
                .padding(horizontal = scale.gap(DesignSpace.xxxl), vertical = scale.gap(DesignSpace.l)),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                PageHeader(stringResource(R.string.relax_title), onDone, scale, colors)
                Spacer(Modifier.height(scale.gap(DesignSpace.xl)))
                Column(modifier = Modifier.padding(horizontal = scale.gap(DesignSpace.page))) {
                    MainToggle(settings.enabled, petName, onSetEnabled, scale, colors)
                    DesignDivider(colors, scale)
                    Column(Modifier.alpha(if (settings.enabled) 1f else OFF_ALPHA)) {
                        PetEnvironment(settings, onSetSound, scale, colors)
                        DesignDivider(colors, scale)
                        Reassurance(settings, onSetExtraReassurance, scale, colors)
                    }
                }
            }
            Column(modifier = Modifier.padding(horizontal = scale.gap(DesignSpace.page)).padding(top = scale.gap(DesignSpace.xxl))) {
                DesignDivider(colors, scale)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = scale.gap(DesignSpace.l), bottom = scale.gap(DesignSpace.m)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.relax_demo_notice),
                        style = scale.style(DesignType.body),
                        color = colors.homeSubText,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(scale.gap(DesignSpace.l)))
                    Button(
                        onClick = onDone,
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary),
                        shape = RoundedCornerShape(DesignRadius.m),
                        elevation = null,
                        contentPadding = PaddingValues(horizontal = scale.gap(DesignSpace.xxxl)),
                        modifier = Modifier.heightIn(min = scale.touch(DesignHeight.buttonLarge)).widthIn(min = scale.gap(320.dp)),
                    ) {
                        Text(stringResource(R.string.relax_done), style = scale.style(DoneButton))
                    }
                }
            }
        }
    }
}

/** "보리를 위한 편안한 이동" + 상태 + 큰 스위치 */
@Composable
private fun MainToggle(on: Boolean, petName: String?, onSet: (Boolean) -> Unit, scale: DesignScale, colors: DesignColors) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = scale.gap(DesignSpace.xs), bottom = scale.gap(DesignSpace.xxl)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(scale.gap(10.dp))) {
            Text(headingOf(petName), style = scale.style(Heading), color = colors.homeText)
            Text(
                stringResource(if (on) R.string.relax_status_on else R.string.relax_status_off),
                style = scale.style(DesignType.body),
                color = colors.homeSubText,
            )
        }
        Spacer(Modifier.width(scale.gap(DesignSpace.xxl)))
        Box(
            modifier = Modifier
                .size(scale.touch(136.dp), scale.touch(96.dp))
                .toggleable(value = on, role = Role.Switch, onValueChange = onSet),
            contentAlignment = Alignment.Center,
        ) {
            DesignSwitchTrack(on, if (on) colors.primary else colors.homeSubText.copy(alpha = 0.75f), scale)
        }
    }
}

/** 받침이 있으면 "을", 없으면(또는 한글이 아니면) "를" */
@Composable
private fun headingOf(name: String?): String {
    if (name.isNullOrBlank()) return stringResource(R.string.relax_heading_no_name)
    val last = name.last()
    val hasFinal = last in '가'..'힣' && (last - '가') % 28 != 0
    return stringResource(if (hasFinal) R.string.relax_heading_with_eul else R.string.relax_heading_with_reul, name)
}

/** 반려동물 환경: 실내 사운드 [조용히 | 잔잔한 음악] + 현재 환경(예시) */
@Composable
private fun PetEnvironment(settings: RelaxSettings, onSetSound: (RelaxSound) -> Unit, scale: DesignScale, colors: DesignColors) {
    val enabled = settings.enabled
    Column(modifier = Modifier.padding(top = scale.gap(36.dp))) {
        Text(stringResource(R.string.relax_section_pet), style = scale.style(SectionLabel), color = colors.homeSubText)
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = scale.gap(128.dp)).padding(vertical = scale.gap(DesignSpace.m)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.relax_sound), style = scale.style(RowLabel), color = colors.homeText, modifier = Modifier.width(scale.gap(360.dp)))
            Spacer(Modifier.width(scale.gap(DesignSpace.xxl)))
            Row(
                modifier = Modifier
                    .weight(1f)
                    .widthIn(max = scale.gap(720.dp))
                    .background(colors.homeIcon, RoundedCornerShape(scale.gap(18.dp)))
                    .padding(scale.gap(DesignSpace.xxs)),
                horizontalArrangement = Arrangement.spacedBy(scale.gap(DesignSpace.xxs)),
            ) {
                listOf(RelaxSound.QUIET to R.string.relax_sound_quiet, RelaxSound.CALM_MUSIC to R.string.relax_sound_calm).forEach { (sound, label) ->
                    val selected = settings.sound == sound
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = scale.touch(DesignHeight.buttonLarge))
                            .background(if (selected) colors.background else colors.homeIcon, RoundedCornerShape(scale.gap(14.dp)))
                            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton) { onSetSound(sound) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            stringResource(label),
                            style = scale.style(Segment).copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal),
                            color = if (selected) colors.selectedText else colors.homeSubText,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = scale.gap(DesignSpace.xxs), bottom = scale.gap(DesignSpace.xl)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.relax_env), style = scale.style(SubLabel), color = colors.homeSubText, modifier = Modifier.width(scale.gap(360.dp)))
            Spacer(Modifier.width(scale.gap(DesignSpace.xxl)))
            Text(stringResource(R.string.relax_env_value), style = scale.style(RowLabel), color = colors.homeText)
        }
    }
}

/** 보호자 안심: 추가 안심 안내 스위치 (줄 전체를 누른다) */
@Composable
private fun Reassurance(settings: RelaxSettings, onSet: (Boolean) -> Unit, scale: DesignScale, colors: DesignColors) {
    val on = settings.extraReassurance
    Column(modifier = Modifier.padding(top = scale.gap(36.dp))) {
        Text(stringResource(R.string.relax_section_guardian), style = scale.style(SectionLabel), color = colors.homeSubText)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = scale.touch(144.dp))
                .toggleable(value = on, enabled = settings.enabled, role = Role.Switch, onValueChange = onSet)
                .padding(vertical = scale.gap(DesignSpace.m)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(scale.gap(DesignSpace.xs))) {
                Text(stringResource(R.string.relax_reassurance), style = scale.style(RowLabel), color = colors.homeText)
                Text(stringResource(R.string.relax_reassurance_description), style = scale.style(DesignType.body), color = colors.homeSubText)
            }
            Spacer(Modifier.width(scale.gap(DesignSpace.xxl)))
            Box(Modifier.width(scale.gap(136.dp)), contentAlignment = Alignment.Center) {
                DesignSwitchTrack(on, if (settings.enabled && on) colors.primary else colors.disabled, scale)
            }
        }
    }
}

/** 켜짐·꺼짐 없이 설정만 ("조용히", "잔잔한 음악 · 추가 안심 안내"). 시안 `rxSum()` */
@Composable
fun relaxSettingsText(settings: RelaxSettings): String = listOfNotNull(
    stringResource(if (settings.sound == RelaxSound.QUIET) R.string.relax_sound_quiet else R.string.relax_sound_calm),
    if (settings.extraReassurance) stringResource(R.string.relax_reassurance) else null,
).joinToString(stringResource(R.string.pet_card_separator))

/** 반려동물 카드 "환경 설정" 칸의 요약 */
@Composable
fun relaxSummaryOf(settings: RelaxSettings): String {
    if (settings.isDefault) return stringResource(R.string.relax_summary_default)
    val parts = listOfNotNull(
        stringResource(if (settings.enabled) R.string.relax_summary_on else R.string.relax_summary_off),
        stringResource(if (settings.sound == RelaxSound.QUIET) R.string.relax_sound_quiet else R.string.relax_sound_calm),
        if (settings.extraReassurance) stringResource(R.string.relax_reassurance) else null,
    )
    return parts.joinToString(stringResource(R.string.pet_card_separator))
}

@Preview(widthDp = 1682, heightDp = 1210)
@Composable
private fun RelaxScreenPreview() {
    MeongCareTheme(darkTheme = false) {
        Surface {
            RelaxScreen(RelaxSettings(), "보리", {}, {}, {}, {})
        }
    }
}
