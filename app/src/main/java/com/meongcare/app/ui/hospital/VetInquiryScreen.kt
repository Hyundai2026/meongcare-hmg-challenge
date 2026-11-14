package com.meongcare.app.ui.hospital

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.meongcare.app.R
import com.meongcare.app.domain.inquiry.InquiryRow
import com.meongcare.app.domain.inquiry.InquirySnapshot
import com.meongcare.app.domain.inquiry.InquiryStatus
import com.meongcare.app.domain.inquiry.InquirySummary
import com.meongcare.app.ui.home.DesignDivider
import com.meongcare.app.ui.home.HomeIcons
import com.meongcare.app.ui.theme.DesignColors
import com.meongcare.app.ui.theme.DesignHeight
import com.meongcare.app.ui.theme.DesignRadius
import com.meongcare.app.ui.theme.DesignScale
import com.meongcare.app.ui.theme.DesignSpace
import com.meongcare.app.ui.theme.LocalDesignTokens
import com.meongcare.app.ui.voice.VoiceGuideLine
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay

private val Title = TextStyle(fontSize = 40.sp, lineHeight = 50.sp, fontWeight = FontWeight.SemiBold)
private val Mode = TextStyle(fontSize = 30.sp, lineHeight = 40.sp)
private val Body = TextStyle(fontSize = 30.sp, lineHeight = 42.sp)
private val RowName = TextStyle(fontSize = 36.sp, lineHeight = 46.sp, fontWeight = FontWeight.SemiBold)
private val RowSub = TextStyle(fontSize = 28.sp, lineHeight = 38.sp)
private val SearchName = TextStyle(fontSize = 32.sp, lineHeight = 40.sp, fontWeight = FontWeight.SemiBold)
private val BadgeText = TextStyle(fontSize = 26.sp, lineHeight = 34.sp, fontWeight = FontWeight.Medium)
private val Small = TextStyle(fontSize = 28.sp, lineHeight = 38.sp)
private val ButtonText = TextStyle(fontSize = 32.sp, lineHeight = 40.sp, fontWeight = FontWeight.SemiBold)
private val Big = TextStyle(fontSize = 40.sp, lineHeight = 48.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.02).em)
private val PopupTitle = TextStyle(fontSize = 48.sp, lineHeight = 58.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.02).em)
private val HeadCell = TextStyle(fontSize = 26.sp, lineHeight = 34.sp, fontWeight = FontWeight.Medium)

private val TimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT)

/**
 * 카드에서 연 [동물병원 찾기] (계획 020): "동물병원 응답 확인"(+ 응답 없음 팝업) 또는 "동물병원 비교".
 * 프로토타입과 같은 모양이다. 답변은 앱 안 시연 응답이다 (화면에는 표시하지 않는다, 사용자 결정 D1-B).
 *
 * @param onBack 응답 확인 [←] (카드로)
 * @param onChoose 비교에서 고른 병원으로 출발 확인 (`HospitalTrip.choose`)
 * @param onDepartSuggested 출발 제안 팝업 [여기로 출발]: 카드와 함께 바로 출발 (비교 화면을 거치지 않는다)
 * @param onDepartNearest 응답 없음 팝업 [가장 가까운 병원으로 출발]: 카드와 함께 바로 출발 + 병원 전화 연결
 */
@Composable
fun VetInquiryScreen(
    vm: VetInquiryViewModel,
    snapshot: InquirySnapshot,
    page: InquiryPage,
    compareSelected: String?,
    promptSelected: String?,
    expandNotice: Int?,
    onBack: () -> Unit,
    onChoose: (DemoHospital) -> Unit,
    modifier: Modifier = Modifier,
    onDepartNearest: (DemoHospital) -> Unit = {},
    onDepartSuggested: (DemoHospital) -> Unit = {},
) {
    val colors = LocalDesignTokens.current.colors
    // 비교 쪽에서 시스템 뒤로 = 응답 확인으로 (응답 확인에서는 HomeScreen의 BackHandler가 카드로 보낸다)
    androidx.activity.compose.BackHandler(enabled = page == InquiryPage.COMPARE, onBack = vm::backToSearch)
    BoxWithConstraints(modifier = modifier.fillMaxSize().background(colors.homeBackground)) {
        val scale = DesignScale.of(maxWidth)
        when (page) {
            InquiryPage.SEARCH -> {
                SearchPage(vm, snapshot, expandNotice, onBack, scale, colors)
                if (snapshot.showSuggestion) {
                    SuggestionPrompt(vm, snapshot, onDepartSuggested, scale, colors)
                }
                if (snapshot.showNoResponsePrompt) {
                    NoResponsePrompt(vm, snapshot, onDepartNearest, scale, colors)
                }
            }
            InquiryPage.COMPARE -> ComparePage(vm, snapshot, compareSelected, onChoose, scale, colors)
        }
    }
}

/** 머리: [←] 제목  ……  "● 응급 모드" */
@Composable
internal fun FlowHeader(title: String, onBack: () -> Unit, scale: DesignScale, colors: DesignColors, trailing: @Composable () -> Unit = {}) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack, modifier = Modifier.size(scale.touch(96.dp))) {
            Icon(HomeIcons.Back, contentDescription = stringResource(R.string.page_back), tint = colors.homeText, modifier = Modifier.size(scale.gap(40.dp)))
        }
        Spacer(Modifier.width(scale.gap(DesignSpace.l)))
        Text(title, style = scale.style(Title), color = colors.homeText, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Box(Modifier.size(scale.gap(12.dp)).background(colors.primary, CircleShape))
        Spacer(Modifier.width(scale.gap(DesignSpace.xs)))
        Text(stringResource(R.string.card_emergency_mode), style = scale.style(Mode), color = colors.homeSubText)
        trailing()
    }
}

@Composable
private fun SearchPage(
    vm: VetInquiryViewModel,
    s: InquirySnapshot,
    expandNotice: Int?,
    onBack: () -> Unit,
    scale: DesignScale,
    colors: DesignColors,
) {
    LaunchedEffect(expandNotice) {
        if (expandNotice != null) {
            delay(3_000)
            vm.consumeExpandNotice()
        }
    }
    Column(Modifier.fillMaxSize().padding(horizontal = scale.gap(DesignSpace.xxxl), vertical = scale.gap(DesignSpace.l))) {
        FlowHeader(stringResource(R.string.inquiry_title), onBack, scale, colors)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = scale.gap(DesignSpace.xxxl)),
        ) {
            Spacer(Modifier.height(scale.gap(DesignSpace.s)))
            VoiceGuideLine(
                stringResource(if (s.okCount > 0) R.string.inquiry_voice_ok else R.string.inquiry_voice),
                scale,
                colors,
            )
            Spacer(Modifier.height(scale.gap(DesignSpace.s)))
            Text(stringResource(R.string.inquiry_intro, s.searchRows.size), style = scale.style(Small), color = colors.homeSubText)
            Spacer(Modifier.height(scale.gap(DesignSpace.s)))
            // 카운트다운: 막대(20초 → 0초)와 남은 시간을 한 줄에
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { ProgressBar(s.progress, scale, colors) }
                Spacer(Modifier.width(scale.gap(DesignSpace.l)))
                Text(
                    if (s.remainingSeconds > 0) stringResource(R.string.inquiry_countdown, s.remainingSeconds)
                    else stringResource(R.string.inquiry_countdown_done),
                    style = scale.style(Small),
                    color = if (s.remainingSeconds > 0) colors.homeSubText else colors.warning,
                )
            }
            Spacer(Modifier.height(scale.gap(DesignSpace.xs)))
            DesignDivider(colors, scale)
            s.searchRows.forEachIndexed { i, row ->
                if (i > 0) DesignDivider(colors, scale)
                SearchRow(row, scale, colors)
            }
            DesignDivider(colors, scale)
            if (s.restCount > 0) {
                Spacer(Modifier.height(scale.gap(DesignSpace.m)))
                FlowButton(stringResource(R.string.inquiry_expand, s.restCount), vm::expand, primary = false, scale = scale, colors = colors)
            }
            if (expandNotice != null) {
                Spacer(Modifier.height(scale.gap(DesignSpace.s)))
                Text(stringResource(R.string.inquiry_expanded, expandNotice), style = scale.style(Small), color = colors.selectedText)
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(start = scale.gap(DesignSpace.xxxl), top = scale.gap(DesignSpace.s)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                when (val summary = s.summary) {
                    is InquirySummary.Accepting -> stringResource(R.string.inquiry_status_accepting, summary.count)
                    InquirySummary.NoneAvailable -> stringResource(R.string.inquiry_status_none)
                    InquirySummary.Waiting -> stringResource(R.string.inquiry_status_waiting)
                },
                style = scale.style(Small),
                color = if (s.okCount > 0) colors.success else colors.homeSubText,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(scale.gap(DesignSpace.l)))
            FlowButton(
                stringResource(R.string.inquiry_compare),
                vm::openCompare,
                primary = true,
                enabled = s.okCount > 0,
                scale = scale,
                colors = colors,
                minWidth = 440,
            )
        }
    }
}

@Composable
private fun ProgressBar(fraction: Float, scale: DesignScale, colors: DesignColors) {
    Box(Modifier.fillMaxWidth().height(scale.gap(8.dp)).background(colors.homeLine, RoundedCornerShape(50))) {
        Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().background(colors.primary, RoundedCornerShape(50)))
    }
}

@Composable
private fun SearchRow(row: InquiryRow, scale: DesignScale, colors: DesignColors) {
    // 5곳이 한 화면에 다 보이게 한 줄로: 이름 · 상태 설명 ……… 배지 (사용자 요청 2026-11-14)
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = scale.gap(DesignSpace.xs)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(row.key, style = scale.style(SearchName), color = colors.homeText, modifier = Modifier.width(scale.gap(420.dp)), maxLines = 1)
        Text(
            rowSubText(row),
            style = scale.style(RowSub),
            color = when (row.status) {
                InquiryStatus.DELAYED -> colors.warning
                InquiryStatus.AVAILABLE -> colors.homeText
                else -> colors.homeSubText
            },
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.width(scale.gap(DesignSpace.m)))
        StatusBadge(row.status, scale, colors)
    }
}

@Composable
private fun rowSubText(row: InquiryRow): String = when (row.status) {
    InquiryStatus.NOT_ASKED -> stringResource(R.string.prompt_not_asked)
    InquiryStatus.WAITING -> stringResource(R.string.inquiry_sub_wait)
    InquiryStatus.DELAYED -> stringResource(R.string.inquiry_sub_delay)
    InquiryStatus.UNAVAILABLE -> stringResource(R.string.inquiry_sub_no)
    InquiryStatus.AVAILABLE -> {
        val time = row.answeredAt?.atZone(ZoneId.systemDefault())?.format(TimeFormat).orEmpty()
        when {
            row.needsRecheck -> stringResource(R.string.inquiry_sub_recheck, row.elapsedMinutes ?: 0)
            row.waitMinutes != null -> stringResource(R.string.inquiry_sub_ok, row.waitMinutes, time)
            else -> stringResource(R.string.inquiry_sub_ok_unknown, time)
        }
    }
}

@Composable
private fun StatusBadge(status: InquiryStatus, scale: DesignScale, colors: DesignColors) {
    val (text, bg, fg) = when (status) {
        InquiryStatus.AVAILABLE -> Triple(R.string.inquiry_badge_ok, colors.successBackground, colors.success)
        InquiryStatus.UNAVAILABLE -> Triple(R.string.inquiry_badge_no, colors.homeIcon, colors.homeSubText)
        InquiryStatus.DELAYED -> Triple(R.string.inquiry_badge_delay, colors.warningBackground, colors.warning)
        else -> Triple(R.string.inquiry_badge_wait, colors.homeIcon, colors.homeSubText)
    }
    Text(
        stringResource(text),
        style = scale.style(BadgeText),
        color = fg,
        modifier = Modifier
            .background(bg, RoundedCornerShape(scale.gap(DesignRadius.s)))
            .padding(horizontal = scale.gap(DesignSpace.m), vertical = scale.gap(DesignSpace.xs)),
    )
}

@Composable
internal fun FlowButton(
    text: String,
    onClick: () -> Unit,
    primary: Boolean,
    scale: DesignScale,
    colors: DesignColors,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    minWidth: Int = 0,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (primary) colors.primary else colors.homeIcon,
            contentColor = if (primary) colors.onPrimary else colors.homeText,
            disabledContainerColor = colors.disabled,
            disabledContentColor = colors.disabledText,
        ),
        shape = RoundedCornerShape(DesignRadius.m),
        elevation = null,
        contentPadding = PaddingValues(horizontal = scale.gap(DesignSpace.xxl)),
        modifier = modifier.heightIn(min = scale.touch(DesignHeight.buttonLarge)).widthIn(min = scale.gap(minWidth.dp)),
    ) {
        icon?.let {
            Icon(it, contentDescription = null, modifier = Modifier.size(scale.gap(34.dp)))
            Spacer(Modifier.width(scale.gap(DesignSpace.s)))
        }
        Text(text, style = scale.style(ButtonText), textAlign = TextAlign.Center, maxLines = 2)
    }
}

/**
 * 출발 제안 팝업 (사용자 결정 2026-11-14): 응답 대기 시간(20초)이 끝났을 때 진료 가능 답이 있으면
 * 진료 시작까지가 가장 짧은 병원을 "○○으로 출발할까요?"로 제안한다. [여기로 출발]은 비교 없이 바로 출발한다.
 */
@Composable
private fun SuggestionPrompt(
    vm: VetInquiryViewModel,
    s: InquirySnapshot,
    onDepartSuggested: (DemoHospital) -> Unit,
    scale: DesignScale,
    colors: DesignColors,
) {
    val row = s.suggestion ?: return
    val h = vm.hospitalNamed(row.key) ?: return
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = scale.gap(DesignSpace.xxxl), vertical = scale.gap(DesignSpace.xl))
                .background(colors.background, RoundedCornerShape(scale.gap(DesignRadius.xl)))
                .padding(scale.gap(DesignSpace.xxl)),
            horizontalArrangement = Arrangement.spacedBy(scale.gap(DesignSpace.xxl)),
        ) {
            Column(Modifier.weight(0.48f).fillMaxHeight().verticalScroll(rememberScrollState())) {
                Text(
                    stringResource(R.string.suggest_title, com.meongcare.app.ui.withDirectionParticle(h.name)),
                    style = scale.style(PopupTitle),
                    color = colors.homeText,
                )
                Spacer(Modifier.height(scale.gap(DesignSpace.s)))
                Text(stringResource(R.string.suggest_body), style = scale.style(Body), color = colors.homeSubText)
                Spacer(Modifier.height(scale.gap(DesignSpace.l)))
                DesignDivider(colors, scale)
                Row(Modifier.fillMaxWidth().padding(vertical = scale.gap(DesignSpace.m)), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(h.name, style = scale.style(RowName), color = colors.homeText)
                        Text(
                            row.startMinutes?.let { stringResource(R.string.suggest_detail_start, h.travelMinutes, it) }
                                ?: stringResource(R.string.suggest_detail_unknown, h.travelMinutes),
                            style = scale.style(RowSub),
                            color = colors.homeSubText,
                        )
                    }
                    StatusBadge(row.status, scale, colors)
                }
                DesignDivider(colors, scale)
                Spacer(Modifier.height(scale.gap(DesignSpace.m)))
                Text(stringResource(R.string.depart_sends), style = scale.style(Small), color = colors.homeSubText)
                Spacer(Modifier.height(scale.gap(DesignSpace.l)))
                Row(horizontalArrangement = Arrangement.spacedBy(scale.gap(DesignSpace.m))) {
                    FlowButton(stringResource(R.string.suggest_compare), vm::compareInsteadOfSuggestion, primary = false, scale = scale, colors = colors)
                    FlowButton(
                        stringResource(R.string.suggest_go),
                        { onDepartSuggested(h) },
                        primary = true,
                        scale = scale,
                        colors = colors,
                        modifier = Modifier.weight(1f),
                        icon = HomeIcons.Navigate,
                    )
                }
            }
            DemoMap(
                hospitals = vm.hospitals,
                selected = h,
                scale = scale,
                colors = colors,
                modifier = Modifier.weight(0.52f).fillMaxHeight(),
                onSelect = null,
                infoTitle = h.name,
                infoSubtitle = row.startMinutes?.let { stringResource(R.string.suggest_detail_start, h.travelMinutes, it) }
                    ?: stringResource(R.string.suggest_detail_unknown, h.travelMinutes),
                selectedLabel = stringResource(R.string.map_label_travel, h.rank, h.name, h.travelMinutes),
                showBadge = false,
            )
        }
    }
}

/**
 * "아직 병원 응답이 없습니다" 팝업: 응답 대기 시간(20초)이 지나도 진료 가능 답이 없으면 뜬다.
 * [계속 기다리기] 또는 [가장 가까운 병원으로 출발](카드와 함께 출발 + 병원 전화 연결, 사용자 결정 2026-11-14).
 */
@Composable
private fun NoResponsePrompt(
    vm: VetInquiryViewModel,
    s: InquirySnapshot,
    onDepartNearest: (DemoHospital) -> Unit,
    scale: DesignScale,
    colors: DesignColors,
) {
    val selected = vm.hospitalNamed(s.nearest?.key)
    val selectedName = selected?.name
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = scale.gap(DesignSpace.xxxl), vertical = scale.gap(DesignSpace.xl))
                .background(colors.background, RoundedCornerShape(scale.gap(DesignRadius.xl)))
                .padding(scale.gap(DesignSpace.xxl)),
            horizontalArrangement = Arrangement.spacedBy(scale.gap(DesignSpace.xxl)),
        ) {
            Column(Modifier.weight(0.48f).fillMaxHeight()) {
                Text(stringResource(R.string.prompt_title), style = scale.style(PopupTitle), color = colors.homeText)
                Spacer(Modifier.height(scale.gap(DesignSpace.s)))
                Text(stringResource(R.string.prompt_body), style = scale.style(Body), color = colors.homeSubText)
                Spacer(Modifier.height(scale.gap(DesignSpace.l)))
                DesignDivider(colors, scale)
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    s.noResponseRows.forEachIndexed { i, row ->
                        val h = vm.hospitalNamed(row.key) ?: return@forEachIndexed
                        if (i > 0) DesignDivider(colors, scale)
                        PromptRow(h, row, h.name == selectedName, scale, colors)
                    }
                }
                DesignDivider(colors, scale)
                Spacer(Modifier.height(scale.gap(DesignSpace.m)))
                Text(stringResource(R.string.prompt_warning), style = scale.style(Small), color = colors.warning)
                selected?.let {
                    Text(stringResource(R.string.prompt_nearest_note, it.name), style = scale.style(Small), color = colors.homeSubText)
                }
                Spacer(Modifier.height(scale.gap(DesignSpace.m)))
                Row(horizontalArrangement = Arrangement.spacedBy(scale.gap(DesignSpace.m))) {
                    FlowButton(stringResource(R.string.prompt_wait), vm::dismissNoResponse, primary = false, scale = scale, colors = colors)
                    FlowButton(
                        stringResource(R.string.prompt_go_nearest),
                        { selected?.let(onDepartNearest) },
                        primary = true,
                        enabled = selected != null,
                        scale = scale,
                        colors = colors,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            DemoMap(
                hospitals = vm.hospitals,
                selected = selected,
                scale = scale,
                colors = colors,
                modifier = Modifier.weight(0.52f).fillMaxHeight(),
                onSelect = null,
                infoTitle = selected?.name ?: stringResource(R.string.prompt_map_title),
                infoSubtitle = selected?.let { stringResource(R.string.map_travel_unknown_wait, it.travelMinutes) }
                    ?: stringResource(R.string.prompt_map_subtitle),
                selectedLabel = selected?.let { stringResource(R.string.map_label_travel, it.rank, it.name, it.travelMinutes) },
                showBadge = false,
            )
        }
    }
}

@Composable
private fun PromptRow(h: DemoHospital, row: InquiryRow, isSelected: Boolean, scale: DesignScale, colors: DesignColors) {
    val status = when (row.status) {
        InquiryStatus.DELAYED -> stringResource(R.string.inquiry_badge_delay)
        InquiryStatus.WAITING -> stringResource(R.string.inquiry_badge_wait)
        else -> stringResource(R.string.prompt_not_asked)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected) colors.selected else Color.Transparent)
            .heightIn(min = scale.touch(100.dp))
            .padding(horizontal = scale.gap(DesignSpace.m), vertical = scale.gap(DesignSpace.s)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(h.rank.toString(), style = scale.style(RowName), color = if (isSelected) colors.primary else colors.homeSubText, modifier = Modifier.width(scale.gap(56.dp)))
        Column(Modifier.weight(1f)) {
            Text(h.name, style = scale.style(RowName), color = colors.homeText, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(stringResource(R.string.prompt_row_detail, h.travelMinutes, status), style = scale.style(RowSub), color = colors.homeSubText)
        }
        if (isSelected) Icon(Icons.Filled.Check, contentDescription = null, tint = colors.primary, modifier = Modifier.size(scale.gap(40.dp)))
    }
}

/** "동물병원 비교": 진료 시작이 빠른 순. 진료 가능이 아니면 흐리게·고를 수 없다 */
@Composable
private fun ComparePage(
    vm: VetInquiryViewModel,
    s: InquirySnapshot,
    selectedName: String?,
    onChoose: (DemoHospital) -> Unit,
    scale: DesignScale,
    colors: DesignColors,
) {
    val selected = vm.hospitalNamed(selectedName)
    Column(Modifier.fillMaxSize().padding(horizontal = scale.gap(DesignSpace.xxxl), vertical = scale.gap(DesignSpace.l))) {
        FlowHeader(stringResource(R.string.compare_title), vm::backToSearch, scale, colors)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = scale.gap(DesignSpace.xxxl)),
        ) {
            Spacer(Modifier.height(scale.gap(DesignSpace.l)))
            Row(Modifier.fillMaxWidth().padding(bottom = scale.gap(DesignSpace.s))) {
                Text(stringResource(R.string.compare_head_order), style = scale.style(HeadCell), color = colors.homeSubText, modifier = Modifier.weight(1f))
                HeadCellText(stringResource(R.string.compare_head_travel), scale, colors)
                HeadCellText(stringResource(R.string.compare_head_add), scale, colors)
                HeadCellText(stringResource(R.string.compare_head_start), scale, colors, wide = true)
            }
            DesignDivider(colors, scale)
            s.compareRows.forEachIndexed { i, row ->
                if (i > 0) DesignDivider(colors, scale)
                CompareRow(row, row.key == selectedName, row.key == s.best?.key, scale, colors) { vm.selectInCompare(row.key) }
            }
            DesignDivider(colors, scale)
            Spacer(Modifier.height(scale.gap(DesignSpace.m)))
            Text(stringResource(R.string.compare_note), style = scale.style(Small), color = colors.homeSubText)
        }
        Row(Modifier.fillMaxWidth().padding(top = scale.gap(DesignSpace.m)), horizontalArrangement = Arrangement.End) {
            FlowButton(
                stringResource(R.string.compare_go),
                { selected?.let(onChoose) },
                primary = true,
                enabled = selected != null,
                scale = scale,
                colors = colors,
                minWidth = 440,
            )
        }
    }
}

@Composable
private fun HeadCellText(text: String, scale: DesignScale, colors: DesignColors, wide: Boolean = false) {
    Text(
        text,
        style = scale.style(HeadCell),
        color = colors.homeSubText,
        textAlign = TextAlign.End,
        modifier = Modifier.width(scale.gap(if (wide) 260.dp else 200.dp)),
    )
}

@Composable
private fun CompareRow(row: InquiryRow, isSelected: Boolean, isBest: Boolean, scale: DesignScale, colors: DesignColors, onClick: () -> Unit) {
    val selectable = row.status == InquiryStatus.AVAILABLE
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (selectable) 1f else 0.55f)
            .background(if (isSelected) colors.selected else Color.Transparent)
            .clickable(enabled = selectable, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = scale.gap(DesignSpace.m), vertical = scale.gap(DesignSpace.l)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(scale.gap(DesignSpace.xxs))) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(row.key, style = scale.style(RowName), color = if (isSelected) colors.selectedText else colors.homeText)
                if (isBest) {
                    Spacer(Modifier.width(scale.gap(DesignSpace.s)))
                    Text(
                        stringResource(R.string.compare_fastest),
                        style = scale.style(BadgeText),
                        color = colors.onPrimary,
                        modifier = Modifier
                            .background(colors.primary, RoundedCornerShape(scale.gap(DesignRadius.s)))
                            .padding(horizontal = scale.gap(DesignSpace.s), vertical = scale.gap(DesignSpace.xxs)),
                    )
                }
            }
            Text(rowSubText(row), style = scale.style(RowSub), color = if (row.status == InquiryStatus.DELAYED) colors.warning else colors.homeSubText)
        }
        CellText(stringResource(R.string.compare_minutes, row.travelMinutes), scale, colors)
        CellText(
            when (val add = row.addMinutes) {
                null -> stringResource(R.string.compare_unknown)
                0 -> stringResource(R.string.compare_add_none)
                else -> stringResource(R.string.compare_minutes, add)
            },
            scale,
            colors,
        )
        Text(
            row.startMinutes?.let { stringResource(R.string.compare_about_minutes, it) } ?: stringResource(R.string.compare_unknown),
            style = scale.style(Big),
            color = if (isBest) colors.primary else colors.homeText,
            textAlign = TextAlign.End,
            modifier = Modifier.width(scale.gap(260.dp)),
        )
    }
}

@Composable
private fun CellText(text: String, scale: DesignScale, colors: DesignColors) {
    Text(text, style = scale.style(Body), color = colors.homeText, textAlign = TextAlign.End, modifier = Modifier.width(scale.gap(200.dp)))
}

/** 시각 표기 "오후 3:14" (계획 020) */
internal fun koreanClock(at: Instant, zone: ZoneId = ZoneId.systemDefault()): String =
    at.atZone(zone).format(DateTimeFormatter.ofPattern("a h:mm", Locale.KOREAN))
