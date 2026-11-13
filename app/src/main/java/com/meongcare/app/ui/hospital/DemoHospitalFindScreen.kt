package com.meongcare.app.ui.hospital

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meongcare.app.R
import com.meongcare.app.ui.withObjectParticle
import com.meongcare.app.ui.home.DesignDivider
import com.meongcare.app.ui.home.PageHeader
import com.meongcare.app.ui.theme.DesignColors
import com.meongcare.app.ui.theme.DesignHeight
import com.meongcare.app.ui.theme.DesignRadius
import com.meongcare.app.ui.theme.DesignScale
import com.meongcare.app.ui.theme.DesignSpace
import com.meongcare.app.ui.theme.LocalDesignTokens
import com.meongcare.app.ui.theme.MeongCareTheme
import com.meongcare.app.ui.voice.VoiceCommandButton
import com.meongcare.app.ui.voice.VoiceGuideLine
import java.util.Locale

private val Caption = TextStyle(fontSize = 30.sp, lineHeight = 40.sp)
private val Rank = TextStyle(fontSize = 34.sp, lineHeight = 42.sp, fontWeight = FontWeight.SemiBold)
private val Name = TextStyle(fontSize = 34.sp, lineHeight = 44.sp, fontWeight = FontWeight.Medium)
private val Detail = TextStyle(fontSize = 28.sp, lineHeight = 38.sp)
private val Travel = TextStyle(fontSize = 34.sp, lineHeight = 42.sp)


/**
 * 홈 [병원 찾기] (계획 019, 프로토타입 5번). 정차 중에만 보인다.
 *
 * 디자인 시안의 **시연용 가상 병원 5곳**([DemoHospitals], 사용자 결정 2026-11-13)을 가까운 순 목록과 시연 지도로 보인다.
 * 지도는 실제 지도가 아니라 그린 그림이다 (화면에 "시연용 지도 · 가상 병원"을 늘 보인다).
 * 병원을 고르면 [문의 없이 바로 이동] → 확인 화면 → [길 안내 시작]. [진료 가능 여부 문의]는 준비 중이다 (사용자 결정).
 * 음성은 자리만 있다 ([VoiceCommandButton], [VoiceGuideLine]).
 *
 * @param onGoDirect 고른 병원으로 확인 화면을 연다 (`HospitalTrip.choose`)
 */
@Composable
fun DemoHospitalFindScreen(
    onBack: () -> Unit,
    onGoDirect: (DemoHospital) -> Unit,
    modifier: Modifier = Modifier,
    hospitals: List<DemoHospital> = DemoHospitals.all,
) {
    val colors = LocalDesignTokens.current.colors
    var selectedRank by rememberSaveable { mutableIntStateOf(0) }
    var notice by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = hospitals.firstOrNull { it.rank == selectedRank }
    val voiceNotReady = stringResource(R.string.voice_not_ready)
    val inquiryNotReady = stringResource(R.string.demo_finder_inquiry_not_ready)

    BoxWithConstraints(modifier = modifier.fillMaxSize().background(colors.homeBackground)) {
        val scale = DesignScale.of(maxWidth)
        val wide = maxWidth >= 840.dp
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = scale.gap(DesignSpace.xxxl), vertical = scale.gap(DesignSpace.l)),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PageHeader(stringResource(R.string.demo_finder_title), onBack, scale, colors, Modifier.weight(1f))
                VoiceCommandButton(onClick = { notice = voiceNotReady }, scale = scale, colors = colors)
            }
            Spacer(Modifier.height(scale.gap(DesignSpace.l)))
            VoiceGuideLine(
                text = selected?.let { stringResource(R.string.demo_finder_voice_selected, withObjectParticle(it.name)) }
                    ?: stringResource(R.string.demo_finder_voice_found),
                scale = scale,
                colors = colors,
            )
            Spacer(Modifier.height(scale.gap(DesignSpace.l)))
            if (wide) {
                Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(scale.gap(DesignSpace.xxxl))) {
                    ListColumn(hospitals, selected, notice, scale, colors, Modifier.weight(0.42f).fillMaxHeight(),
                        onSelect = { selectedRank = it.rank; notice = null },
                        onGoDirect = { selected?.let(onGoDirect) },
                        onInquire = { notice = inquiryNotReady },
                    )
                    DemoMap(hospitals, selected, scale, colors, Modifier.weight(0.58f).fillMaxHeight(), onSelect = {
                        selectedRank = it.rank
                        notice = null
                    })
                }
            } else {
                ListColumn(hospitals, selected, notice, scale, colors, Modifier.weight(1f).fillMaxWidth(),
                    onSelect = { selectedRank = it.rank; notice = null },
                    onGoDirect = { selected?.let(onGoDirect) },
                    onInquire = { notice = inquiryNotReady },
                )
            }
        }
    }
}

@Composable
private fun ListColumn(
    hospitals: List<DemoHospital>,
    selected: DemoHospital?,
    notice: String?,
    scale: DesignScale,
    colors: DesignColors,
    modifier: Modifier,
    onSelect: (DemoHospital) -> Unit,
    onGoDirect: () -> Unit,
    onInquire: () -> Unit,
) {
    Column(modifier) {
        Text(stringResource(R.string.demo_finder_caption), style = scale.style(Caption), color = colors.homeSubText)
        Spacer(Modifier.height(scale.gap(DesignSpace.m)))
        DesignDivider(colors, scale)
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            hospitals.forEach { h ->
                HospitalRow(h, h == selected, scale, colors) { onSelect(h) }
                DesignDivider(colors, scale)
            }
        }
        Spacer(Modifier.height(scale.gap(DesignSpace.m)))
        Row(horizontalArrangement = Arrangement.spacedBy(scale.gap(DesignSpace.l))) {
            ActionButton(
                stringResource(R.string.demo_finder_go_direct), onGoDirect, enabled = selected != null,
                container = colors.homeIcon, content = colors.homeText, scale = scale, colors = colors, modifier = Modifier.weight(1f),
            )
            ActionButton(
                stringResource(R.string.demo_finder_inquire), onInquire, enabled = true,
                container = colors.primary, content = colors.onPrimary, scale = scale, colors = colors, modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(scale.gap(DesignSpace.s)))
        Text(
            notice ?: stringResource(R.string.demo_finder_hint),
            style = scale.style(Caption),
            color = if (notice != null) colors.selectedText else colors.homeSubText,
        )
    }
}

/** 목록 한 줄: 순위 / 이름 + "4.0km · 대기시간 미확인" / "이동 10분". 고르면 바탕이 바뀐다 */
@Composable
private fun HospitalRow(h: DemoHospital, isSelected: Boolean, scale: DesignScale, colors: DesignColors, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = scale.touch(114.dp))
            .background(if (isSelected) colors.selected else Color.Transparent, RoundedCornerShape(scale.gap(DesignRadius.s)))
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = scale.gap(DesignSpace.m), vertical = scale.gap(DesignSpace.s)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            h.rank.toString(), style = scale.style(Rank), color = colors.primary,
            modifier = Modifier.width(scale.gap(48.dp)), textAlign = TextAlign.Start,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(h.name, style = scale.style(Name), color = if (isSelected) colors.selectedText else colors.homeText, maxLines = 1)
            Text(
                stringResource(R.string.demo_finder_row_detail, String.format(Locale.ROOT, "%.1f", h.distanceKm)),
                style = scale.style(Detail),
                color = colors.homeSubText,
            )
        }
        Text(stringResource(R.string.demo_finder_travel, h.travelMinutes), style = scale.style(Travel), color = colors.homeText)
    }
}

@Preview(widthDp = 1682, heightDp = 1210)
@Composable
private fun DemoHospitalFindPreview() {
    MeongCareTheme(darkTheme = false) {
        Surface { DemoHospitalFindScreen(onBack = {}, onGoDirect = {}) }
    }
}
