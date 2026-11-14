package com.meongcare.app.ui.hospital

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meongcare.app.R
import com.meongcare.app.ui.theme.DesignColors
import com.meongcare.app.ui.theme.DesignHeight
import com.meongcare.app.ui.theme.DesignRadius
import com.meongcare.app.ui.theme.DesignScale
import com.meongcare.app.ui.theme.DesignSpace

/* 시연 지도와 버튼 (019 홈 병원 찾기에서 분리, 020 동물병원 문의 흐름이 같이 쓴다). */

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

@Composable
internal fun ActionButton(
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
 * 실제 지도 SDK를 붙이면 이 자리를 바꾼다. 홈 [병원 찾기](019)와 동물병원 문의 흐름(020)이 같이 쓴다.
 *
 * @param onSelect null이면 핀을 눌러도 아무 일도 없다
 * @param infoTitle 왼쪽 위 정보 칸 제목 (null이면 "주변 동물병원 N곳")
 * @param infoSubtitle 정보 칸 둘째 줄 (null이면 "이동시간만 확인 · 대기시간 미확인")
 * @param selectedLabel 고른 병원 핀 위에 번호 대신 보일 글 (예: "1  별숲동물의료센터  이동 10분")
 * @param showRoute 지금 위치에서 [selected]까지 대략적인 경로(꺾인 굵은 파란 선)를 그린다 (안내할 병원, 사용자 요청 2026-11-14)
 * @param showBadge "시연용 지도 · 가상 병원" 배지 (020 흐름은 보이지 않는다, 사용자 결정 D1)
 */
@Composable
internal fun DemoMap(
    hospitals: List<DemoHospital>,
    selected: DemoHospital?,
    scale: DesignScale,
    colors: DesignColors,
    modifier: Modifier,
    onSelect: ((DemoHospital) -> Unit)?,
    infoTitle: String? = null,
    infoSubtitle: String? = null,
    selectedLabel: String? = null,
    showBadge: Boolean = com.meongcare.app.ui.SHOW_DEMO_NOTICES,
    showRoute: Boolean = false,
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
            // 대략적인 경로: 지금 위치에서 가로로 간 뒤 세로로 병원까지 (실제 길이 아니라 시연용 그림)
            if (showRoute && selected != null) {
                val start = Offset(sw * 0.5f, sh * 0.5f)
                val end = Offset(sw * selected.mapX, sh * selected.mapY)
                val corner = Offset(end.x, start.y)
                val route = Path().apply {
                    moveTo(start.x, start.y)
                    lineTo(corner.x, corner.y)
                    lineTo(end.x, end.y)
                }
                drawPath(
                    route,
                    primary,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 10.dp.toPx() * scale.s.coerceAtLeast(0.6f),
                        cap = StrokeCap.Round,
                        join = androidx.compose.ui.graphics.StrokeJoin.Round,
                    ),
                )
            }
            // 현재 위치
            val me = Offset(sw * 0.5f, sh * 0.5f)
            drawCircle(primary.copy(alpha = 0.16f), radius = 60.dp.toPx() * scale.s, center = me)
            drawCircle(Color.White, radius = 22.dp.toPx() * scale.s, center = me)
            drawCircle(primary, radius = 16.dp.toPx() * scale.s, center = me)
        }
        hospitals.forEach { hosp ->
            val isSelected = hosp == selected
            val click: (() -> Unit)? = onSelect?.let { select -> { select(hosp) } }
            Pin(hosp, isSelected, w, h, scale, colors, label = selectedLabel.takeIf { isSelected }, onClick = click)
        }
        Column(
            modifier = Modifier
                .padding(scale.gap(DesignSpace.l))
                .background(colors.background, RoundedCornerShape(scale.gap(DesignRadius.m)))
                .padding(horizontal = scale.gap(DesignSpace.l), vertical = scale.gap(DesignSpace.m)),
        ) {
            Text(infoTitle ?: stringResource(R.string.demo_finder_map_title, hospitals.size), style = scale.style(MapTitle), color = colors.homeText)
            Text(infoSubtitle ?: stringResource(R.string.demo_finder_map_subtitle), style = scale.style(MapSub), color = colors.homeSubText)
        }
        if (showBadge) Text(
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

/** 번호 표(또는 [label]) + 점. 고른 병원은 크게. 표와 점을 지도 위치 위 가운데에 놓는다 */
@Composable
private fun Pin(
    hosp: DemoHospital,
    isSelected: Boolean,
    w: Dp,
    h: Dp,
    scale: DesignScale,
    colors: DesignColors,
    label: String?,
    onClick: (() -> Unit)?,
) {
    val dot = scale.gap(if (isSelected) 28.dp else 20.dp)
    val touch = scale.touch(72.dp)
    Column(
        modifier = Modifier
            .offset(x = w * hosp.mapX, y = h * hosp.mapY + dot / 2)
            .layout { measurable, constraints ->
                val p = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
                layout(p.width, p.height) { p.place(-p.width / 2, -p.height) }
            }
            .widthIn(min = touch)
            .then(if (onClick != null) Modifier.clickable(role = Role.RadioButton, onClick = onClick) else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val labelShape = RoundedCornerShape(scale.gap(DesignRadius.s))
        Box(
            modifier = Modifier
                .background(if (isSelected && label == null) colors.primary else colors.background, labelShape)
                .then(if (label != null) Modifier.border(scale.gap(2.dp).coerceAtLeast(1.dp), colors.primary, labelShape) else Modifier)
                .padding(horizontal = scale.gap(DesignSpace.s), vertical = scale.gap(DesignSpace.xxs)),
        ) {
            Text(
                label ?: hosp.rank.toString(),
                style = scale.style(PinLabel),
                color = when {
                    label != null -> colors.homeText
                    isSelected -> colors.onPrimary
                    else -> colors.primary
                },
                maxLines = 1,
            )
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
