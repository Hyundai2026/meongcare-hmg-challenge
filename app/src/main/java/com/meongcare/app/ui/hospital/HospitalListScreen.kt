package com.meongcare.app.ui.hospital

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.meongcare.app.R
import com.meongcare.app.domain.guidance.Destination
import com.meongcare.app.domain.hospital.Congestion
import com.meongcare.app.domain.hospital.Hospital
import com.meongcare.app.domain.hospital.HospitalCandidate
import com.meongcare.app.domain.hospital.HospitalOrigin
import com.meongcare.app.domain.hospital.HospitalStatus
import com.meongcare.app.domain.hospital.RankBasis
import com.meongcare.app.domain.hospital.RankedHospital
import com.meongcare.app.domain.hospital.rankHospitals
import com.meongcare.app.domain.location.CurrentLocation
import com.meongcare.app.ui.card.CardScale
import com.meongcare.app.ui.card.LocalCardScale
import com.meongcare.app.ui.theme.ActionBlue
import com.meongcare.app.ui.theme.CardColors
import com.meongcare.app.ui.theme.Dimens
import com.meongcare.app.ui.theme.MeongCareTheme
import java.time.Instant

private val ContentMaxWidth = 1100.dp

/**
 * 가장 빨리 진료받을 수 있는 병원 목록 (정차 중에만, 계획 012 "설계 결정 8"의 2).
 *
 * 줄 전체가 하나의 큰 터치 타깃이다. 텍스트 입력은 없다. 정차 중이라 세로 스크롤을 둔다.
 * 주행으로 바뀌면 `homeContentOf`가 주행 화면을 먼저 고르므로 이 화면은 보이지 않는다 (AGENTS.md 규칙 3).
 *
 * @param isLocationSample 현재 위치가 시연용 Fake 위치면 true ("시연용 위치 기준이에요")
 */
@Composable
fun HospitalListScreen(
    state: HospitalFinderUiState,
    isLocationSample: Boolean,
    onChoose: (RankedHospital) -> Unit,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onRelocate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val wide = maxWidth >= 840.dp
        CompositionLocalProvider(LocalCardScale provides if (wide) CardScale.Wide else CardScale.Narrow) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Dimens.ScreenPadding, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Header(
                    refreshing = (state as? HospitalFinderUiState.Loaded)?.isRefreshing == true,
                    canRefresh = state !is HospitalFinderUiState.Loading,
                    onBack = onBack,
                    onRefresh = onRefresh,
                )
                when (state) {
                    HospitalFinderUiState.Loading -> Waiting(stringResource(R.string.finder_loading))
                    is HospitalFinderUiState.Failed -> FailedPanel(state.isRetrying, onRefresh)
                    is HospitalFinderUiState.Loaded -> LoadedList(state, isLocationSample, onChoose, onRelocate)
                }
            }
        }
    }
}

@Composable
private fun Header(refreshing: Boolean, canRefresh: Boolean, onBack: () -> Unit, onRefresh: () -> Unit) {
    val scale = LocalCardScale.current
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.widthIn(max = ContentMaxWidth).fillMaxWidth()) {
        HospitalPillButton(stringResource(R.string.finder_back), onBack, Icons.AutoMirrored.Filled.ArrowBack)
        Spacer(Modifier.width(20.dp))
        Text(
            text = stringResource(R.string.finder_title),
            style = MaterialTheme.typography.headlineMedium.copy(fontSize = scale.title, lineHeight = scale.title * 1.25f),
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(12.dp))
        if (refreshing) {
            CircularProgressIndicator(modifier = Modifier.size(32.dp), strokeWidth = 3.dp)
            Spacer(Modifier.width(12.dp))
        }
        HospitalPillButton(stringResource(R.string.finder_refresh), onRefresh, Icons.Filled.Refresh, enabled = canRefresh && !refreshing)
    }
}

@Composable
private fun Waiting(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 24.dp)) {
        CircularProgressIndicator(modifier = Modifier.size(40.dp))
        Spacer(Modifier.width(20.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge.copy(fontSize = LocalCardScale.current.row))
    }
}

@Composable
private fun FailedPanel(retrying: Boolean, onRetry: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.widthIn(max = ContentMaxWidth)) {
        Text(stringResource(R.string.finder_failed), style = MaterialTheme.typography.titleLarge.copy(fontSize = LocalCardScale.current.title))
        if (retrying) {
            Waiting(stringResource(R.string.finder_loading))
        } else {
            HospitalPrimaryButton(stringResource(R.string.finder_retry), onRetry, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun LoadedList(
    state: HospitalFinderUiState.Loaded,
    isLocationSample: Boolean,
    onChoose: (RankedHospital) -> Unit,
    onRelocate: () -> Unit,
) {
    val scale = LocalCardScale.current
    val ranking = state.ranking
    NoticeBox(
        finderNoticesOf(state.origin, ranking.basis, isLocationSample).map { finderNoticeText(it) },
        modifier = Modifier.widthIn(max = ContentMaxWidth),
    )
    if (ranking.basis == RankBasis.WAIT_ONLY) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.widthIn(max = ContentMaxWidth).fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.finder_location_unknown), style = MaterialTheme.typography.bodyLarge.copy(fontSize = scale.row))
                if (state.isLocating) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                } else {
                    HospitalPillButton(stringResource(R.string.finder_relocate), onRelocate, Icons.Filled.LocationOn)
                }
            }
        }
    }
    if (ranking.ranked.isEmpty()) {
        Text(stringResource(R.string.finder_none_ranked), style = MaterialTheme.typography.bodyLarge.copy(fontSize = scale.row))
    }
    ranking.ranked.forEachIndexed { index, row ->
        HospitalRow(row = row, rank = index + 1, isFastest = index == 0, onClick = { onChoose(row) })
    }
    if (ranking.unranked.isNotEmpty()) {
        Text(
            stringResource(R.string.finder_unranked_title),
            style = MaterialTheme.typography.titleMedium.copy(fontSize = scale.chip),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp),
        )
        ranking.unranked.forEach { row ->
            HospitalRow(row = row, rank = null, isFastest = false, onClick = { onChoose(row) })
        }
    }
    if (ranking.notAccepting.isNotEmpty()) {
        Text(
            stringResource(R.string.finder_not_accepting, ranking.notAccepting.joinToString(", ") { it.hospital.name }),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.widthIn(max = ContentMaxWidth),
        )
    }
}

/** 병원 한 줄. 줄 전체가 버튼이다. [rank]가 null이면 순위 밖(대기 정보 없음·지남) 줄이다. */
@Composable
private fun HospitalRow(row: RankedHospital, rank: Int?, isFastest: Boolean, onClick: () -> Unit) {
    val scale = LocalCardScale.current
    val values = hospitalRowValuesOf(row)
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = if (isFastest) CardColors.Surface else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (isFastest) CardColors.Text else MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(if (isFastest) 3.dp else 1.dp, if (isFastest) ActionBlue else MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .widthIn(max = ContentMaxWidth)
            .fillMaxWidth()
            .heightIn(min = scale.buttonHeight)
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Row(modifier = Modifier.padding(horizontal = 24.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(if (scale == CardScale.Wide) 56.dp else 44.dp)
                    .background(if (rank != null) ActionBlue else MaterialTheme.colorScheme.outline, CircleShape),
            ) {
                Text(
                    text = rank?.toString() ?: "–",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = row.hospital.name,
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = scale.row),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (isFastest) Chip(stringResource(R.string.finder_fastest), ActionBlue, Color.White)
                    if (values.isSample) Chip(stringResource(R.string.finder_sample), CardColors.ChipBackground, CardColors.Navy)
                }
                Text(
                    text = hospitalRowDetail(values),
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = scale.chip),
                    color = if (isFastest) CardColors.Muted else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            values.totalMinutes?.let {
                Spacer(Modifier.width(16.dp))
                Text(
                    text = stringResource(R.string.finder_total, it),
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = scale.title),
                    fontWeight = FontWeight.Bold,
                    color = if (isFastest) CardColors.Navy else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun Chip(text: String, background: Color, content: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium.copy(fontSize = LocalCardScale.current.chip * 0.85f),
        color = content,
        modifier = Modifier
            .background(background, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp),
    )
}

/** 미리보기용 (ui는 data를 import하지 않는다). 값은 docs/data/hospitals.sample.json과 같다. */
internal fun previewRanking(locationKnown: Boolean = true): HospitalFinderUiState.Loaded {
    val now = Instant.parse("2026-11-06T05:00:00Z")
    fun c(id: String, name: String, lat: Double, lon: Double, wait: Int, congestion: Congestion, accepting: Boolean, ago: Long) =
        HospitalCandidate(
            Hospital(Destination(name, lat, lon), "02-0000-0000", isSample = true, id = id),
            HospitalStatus(wait, congestion, accepting, now.minusSeconds(ago * 60)),
            HospitalOrigin.BUILT_IN,
        )
    val candidates = listOf(
        c("demo-fast", "시연용 바른 동물병원", 37.367, 127.1083, 10, Congestion.NORMAL, true, 2),
        c("demo-main", "시연용 동물병원", 37.48544722, 127.03636666, 5, Congestion.LOW, true, 5),
        c("demo-near", "시연용 가까운 동물병원", 37.3947, 127.1112, 60, Congestion.HIGH, true, 3),
        c("demo-stale", "시연용 정보 지난 동물병원", 37.4873, 127.1017, 5, Congestion.LOW, true, 90),
        c("demo-closed", "시연용 응급 휴진 동물병원", 37.4113, 127.1275, 0, Congestion.NORMAL, false, 10),
    )
    val here = CurrentLocation(37.4127212, 127.094681, "수정구 시흥동").takeIf { locationKnown }
    return HospitalFinderUiState.Loaded(rankHospitals(candidates, here, now), HospitalOrigin.BUILT_IN)
}

@Preview(widthDp = 1682, heightDp = 1210)
@Composable
private fun HospitalListWidePreview() {
    MeongCareTheme(darkTheme = true) {
        Surface { HospitalListScreen(previewRanking(), true, {}, {}, {}, {}) }
    }
}

@Preview(widthDp = 411, heightDp = 891)
@Composable
private fun HospitalListNarrowUnknownLocationPreview() {
    MeongCareTheme(darkTheme = false) {
        Surface { HospitalListScreen(previewRanking(locationKnown = false), true, {}, {}, {}, {}) }
    }
}
