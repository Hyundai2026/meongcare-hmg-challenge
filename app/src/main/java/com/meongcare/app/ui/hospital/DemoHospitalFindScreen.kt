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
private val ButtonText = TextStyle(fontSize = 30.sp, lineHeight = 38.sp, fontWeight = FontWeight.SemiBold)
private val MapTitle = TextStyle(fontSize = 30.sp, lineHeight = 40.sp, fontWeight = FontWeight.SemiBold)
private val MapSub = TextStyle(fontSize = 26.sp, lineHeight = 36.sp)
private val PinLabel = TextStyle(fontSize = 26.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold)
private val Badge = TextStyle(fontSize = 20.sp, lineHeight = 26.sp)

/** 지도 바탕 (시안 지도 타일의 회색조) */
private val MapGround = Color(0xFFECEEF1)
private val MapRoad = Color(0xFFFFFFFF)
private val MapWater = Color(0xFFD9DEE5)
private val MapPark = Color(0xFFE2E7E2)

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
                    DemoMap(hospitals, selected, scale, colors, Modifier.weight(0.58f).fillMaxHeight()) {
                        selectedRank = it.rank
                        notice = null
                    }
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

@Composable
private fun ActionButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    container: Color,
    content: Color,
    scale: DesignScale,
    colors: DesignColors,
    modifier: Modifier,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = container, contentColor = content,
            disabledContainerColor = colors.homeIcon, disabledContentColor = colors.disabledText,
        ),
        shape = RoundedCornerShape(DesignRadius.m),
        elevation = null,
        contentPadding = PaddingValues(horizontal = scale.gap(DesignSpace.m)),
        modifier = modifier.heightIn(min = scale.touch(DesignHeight.buttonLarge)),
    ) {
        Text(text, style = scale.style(ButtonText), textAlign = TextAlign.Center, maxLines = 2)
    }
}

/**
 * 시연 지도: 실제 지도가 아닌 그림 (도로·강·공원 + 현재 위치 + 병원 번호 핀). 핀을 눌러도 고른다.
 * 실제 지도 SDK를 붙이면 이 자리를 바꾼다.
 */
@Composable
private fun DemoMap(
    hospitals: List<DemoHospital>,
    selected: DemoHospital?,
    scale: DesignScale,
    colors: DesignColors,
    modifier: Modifier,
    onSelect: (DemoHospital) -> Unit,
) {
    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(scale.gap(DesignRadius.l)))
            .background(MapGround),
    ) {
        val w = maxWidth
        val h = maxHeight
        val primary = colors.primary
        Canvas(Modifier.fillMaxSize()) {
            val sw = size.width
            val sh = size.height
            // 강 (왼쪽 위)
            val river = Path().apply {
                moveTo(0f, sh * 0.10f)
                cubicTo(sw * 0.12f, sh * 0.02f, sw * 0.22f, sh * 0.08f, sw * 0.30f, 0f)
                lineTo(sw * 0.42f, 0f)
                cubicTo(sw * 0.30f, sh * 0.14f, sw * 0.14f, sh * 0.14f, 0f, sh * 0.26f)
                close()
            }
            drawPath(river, MapWater)
            // 공원
            drawRoundRect(MapPark, Offset(sw * 0.72f, sh * 0.30f), androidx.compose.ui.geometry.Size(sw * 0.14f, sh * 0.10f), androidx.compose.ui.geometry.CornerRadius(24f))
            drawRoundRect(MapPark, Offset(sw * 0.12f, sh * 0.64f), androidx.compose.ui.geometry.Size(sw * 0.12f, sh * 0.08f), androidx.compose.ui.geometry.CornerRadius(24f))
            // 작은 길 (격자)
            val thin = 3.dp.toPx() * scale.s.coerceAtLeast(0.6f)
            for (i in 1..11) {
                val x = sw * i / 12f
                drawLine(MapRoad, Offset(x - sw * 0.02f, 0f), Offset(x + sw * 0.02f, sh), thin)
            }
            for (i in 1..9) {
                val y = sh * i / 10f
                drawLine(MapRoad, Offset(0f, y + sh * 0.015f), Offset(sw, y - sh * 0.015f), thin)
            }
            // 큰 길
            val thick = thin * 3f
            drawLine(MapRoad, Offset(sw * 0.15f, sh), Offset(sw * 0.62f, 0f), thick, StrokeCap.Round)
            drawLine(MapRoad, Offset(0f, sh * 0.58f), Offset(sw, sh * 0.46f), thick, StrokeCap.Round)
            drawLine(MapRoad, Offset(sw * 0.55f, sh), Offset(sw * 0.95f, sh * 0.12f), thick, StrokeCap.Round)
            // 현재 위치
            val me = Offset(sw * 0.5f, sh * 0.5f)
            drawCircle(primary.copy(alpha = 0.16f), radius = 60.dp.toPx() * scale.s, center = me)
            drawCircle(Color.White, radius = 22.dp.toPx() * scale.s, center = me)
            drawCircle(primary, radius = 16.dp.toPx() * scale.s, center = me)
        }
        hospitals.forEach { hosp ->
            Pin(hosp, hosp == selected, w, h, scale, colors) { onSelect(hosp) }
        }
        Column(
            modifier = Modifier
                .padding(scale.gap(DesignSpace.l))
                .background(colors.background, RoundedCornerShape(scale.gap(DesignRadius.m)))
                .padding(horizontal = scale.gap(DesignSpace.l), vertical = scale.gap(DesignSpace.m)),
        ) {
            Text(stringResource(R.string.demo_finder_map_title, hospitals.size), style = scale.style(MapTitle), color = colors.homeText)
            Text(stringResource(R.string.demo_finder_map_subtitle), style = scale.style(MapSub), color = colors.homeSubText)
        }
        Text(
            stringResource(R.string.demo_finder_map_badge),
            style = scale.style(Badge),
            color = colors.homeSubText,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(scale.gap(DesignSpace.m))
                .background(colors.background, RoundedCornerShape(scale.gap(DesignRadius.s)))
                .padding(horizontal = scale.gap(DesignSpace.s), vertical = scale.gap(DesignSpace.xxs)),
        )
    }
}

/** 번호 표 + 점. 고른 병원은 크게, 파란 테두리 */
@Composable
private fun Pin(hosp: DemoHospital, isSelected: Boolean, w: Dp, h: Dp, scale: DesignScale, colors: DesignColors, onClick: () -> Unit) {
    val touch = scale.touch(72.dp)
    val dot = scale.gap(if (isSelected) 28.dp else 20.dp)
    Column(
        modifier = Modifier
            .offset(x = w * hosp.mapX - touch / 2, y = h * hosp.mapY - touch)
            .width(touch)
            .clickable(role = Role.RadioButton, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .background(if (isSelected) colors.primary else colors.background, RoundedCornerShape(scale.gap(DesignRadius.s)))
                .padding(horizontal = scale.gap(DesignSpace.s), vertical = scale.gap(DesignSpace.xxs)),
        ) {
            Text(hosp.rank.toString(), style = scale.style(PinLabel), color = if (isSelected) colors.onPrimary else colors.primary)
        }
        Spacer(Modifier.height(scale.gap(DesignSpace.xs)))
        Box(
            Modifier
                .size(dot)
                .background(colors.primary, CircleShape)
                .border(scale.gap(3.dp), Color.White, CircleShape),
        )
    }
}

@Preview(widthDp = 1682, heightDp = 1210)
@Composable
private fun DemoHospitalFindPreview() {
    MeongCareTheme(darkTheme = false) {
        Surface { DemoHospitalFindScreen(onBack = {}, onGoDirect = {}) }
    }
}
