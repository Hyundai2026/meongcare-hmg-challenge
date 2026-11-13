package com.meongcare.app.ui.link

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.meongcare.app.R
import com.meongcare.app.ui.card.PetAvatar
import com.meongcare.app.ui.theme.DesignColors
import com.meongcare.app.ui.theme.DesignHeight
import com.meongcare.app.ui.theme.DesignRadius
import com.meongcare.app.ui.theme.DesignSpace
import com.meongcare.app.ui.theme.DesignType
import com.meongcare.app.ui.theme.LocalDesignTokens
import com.meongcare.app.ui.theme.MeongCareTheme
import java.time.Instant
import kotlinx.coroutines.delay

/**
 * 보호자 웹 연결 화면 (014). **정차 중에만** 보인다: 주행이면 `homeContentOf`가 먼저 주행 화면을 고른다.
 * 글자 입력칸은 없다. 코드는 차가 보이고 보호자가 폰에 입력한다 (AGENTS.md 규칙 3).
 *
 * 모양은 확정 디자인 프로토타입 1번 "차량 연결" 화면을 따른다 (계획 018): 카드 없이 왼쪽 정렬,
 * 안내문 → 큰 코드 → 진행 막대 → "남은 시간 mm:ss" → [새 코드] + 글자 버튼 [닫기].
 * 프로토타입의 "체험 모드 시작"은 앱에 없는 기능이라 넣지 않았다 (사용자 결정 2026-11-13).
 *
 * @param countdown 대기 중 남은 시간·비율 ([rememberLinkCountdown]). null이면 [LinkUiState.Waiting.remainingMinutes]로 막대만 어림한다
 */
@Composable
fun LinkScreen(
    state: LinkUiState,
    onNewCode: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    countdown: LinkCountdown? = null,
) {
    val colors = LocalDesignTokens.current.colors
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(colors.homeBackground),
    ) {
        val metrics = LinkMetrics.of(maxWidth)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = metrics.edge, vertical = metrics.gap(DesignSpace.l)),
        ) {
            Text(
                text = stringResource(R.string.link_title),
                style = metrics.style(DesignType.title),
                color = colors.text,
            )
            Spacer(Modifier.weight(1f))
            Column(
                modifier = Modifier
                    .padding(start = metrics.bodyStart, bottom = metrics.bodyBottom)
                    .widthIn(max = 1040.dp),
            ) {
                when (state) {
                    is LinkUiState.Linked -> LinkedBody(state, metrics, colors, onClose)
                    else -> WaitingBody(state, countdown, metrics, colors, onNewCode, onClose)
                }
            }
            Spacer(Modifier.weight(1f))
        }
    }
}

/**
 * 화면 크기에 맞춘 비율. 프로토타입은 앱 영역 1682×1210(차량 화면) 기준이라, 더 작은 화면(태블릿·폰)에서는
 * 글자·간격·막대·버튼을 같은 비율로 줄인다. 차량 화면에서는 1.0 (프로토타입과 같은 크기).
 */
private class LinkMetrics(val s: Float) {
    val edge: Dp = DesignSpace.xxxl * s
    val bodyStart: Dp = 56.dp * s
    val bodyBottom: Dp = DesignSpace.page * s
    val barWidth: Dp = 640.dp * s
    val buttonHeight: Dp = (DesignHeight.buttonLarge * s).coerceAtLeast(56.dp)
    val avatar: Dp = 96.dp * s
    val primaryWidth: Dp = 480.dp * s
    val icon: Dp = 36.dp * s

    fun gap(d: Dp): Dp = d * s

    fun style(base: TextStyle): TextStyle = if (s == 1f) base else base.copy(fontSize = base.fontSize * s, lineHeight = base.lineHeight * s)

    companion object {
        private val BASE_WIDTH = 1682.dp

        /** 폭 비율. 너무 작아지지 않게 0.45에서 멈춘다 (폰 세로) */
        fun of(width: Dp): LinkMetrics = LinkMetrics((width / BASE_WIDTH).coerceIn(0.45f, 1f))
    }
}

@Composable
private fun WaitingBody(
    state: LinkUiState,
    countdown: LinkCountdown?,
    metrics: LinkMetrics,
    colors: DesignColors,
    onNewCode: () -> Unit,
    onClose: () -> Unit,
) {
    Text(
        text = stringResource(R.string.link_instruction),
        style = metrics.style(DesignType.bodyLarge),
        color = colors.subText,
    )
    Spacer(Modifier.height(metrics.gap(DesignSpace.xl)))

    val expired = state == LinkUiState.Expired
    val code = (state as? LinkUiState.Waiting)?.code ?: "· · ·  · · ·"
    Text(
        text = code,
        style = metrics.style(DesignType.code).copy(
            fontFeatureSettings = "tnum",
            textDecoration = if (expired) TextDecoration.LineThrough else TextDecoration.None,
        ),
        color = if (state is LinkUiState.Waiting) colors.selectedText else colors.subText.copy(alpha = 0.5f),
        maxLines = 1,
        softWrap = false,
    )

    Spacer(Modifier.height(metrics.gap(DesignSpace.xxxl)))
    val fraction = when (state) {
        is LinkUiState.Waiting -> countdown?.fraction ?: (state.remainingMinutes / 10f).coerceIn(0f, 1f)
        else -> 0f
    }
    ProgressBar(fraction, metrics.barWidth, colors)

    Spacer(Modifier.height(metrics.gap(DesignSpace.m)))
    val status = metrics.style(DesignType.body)
    Box(Modifier.heightIn(min = metrics.gap(44.dp))) {
        when (state) {
            is LinkUiState.Waiting -> Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.link_remaining_label), style = status, color = colors.subText)
                Text(
                    text = countdown?.text ?: stringResource(R.string.link_remaining, state.remainingMinutes),
                    style = status.copy(fontWeight = FontWeight.Medium, fontFeatureSettings = "tnum"),
                    color = colors.text,
                )
            }
            LinkUiState.Expired -> Text(stringResource(R.string.link_expired_detail), style = status, color = colors.error)
            LinkUiState.Failed -> Text(stringResource(R.string.link_failed), style = status, color = colors.error)
            else -> Text(stringResource(R.string.link_creating), style = status, color = colors.subText)
        }
    }

    Spacer(Modifier.height(metrics.gap(DesignSpace.section)))
    Row(horizontalArrangement = Arrangement.spacedBy(metrics.gap(DesignSpace.l)), verticalAlignment = Alignment.CenterVertically) {
        if (state != LinkUiState.Creating && state != LinkUiState.Closed) {
            // 만료·실패면 [새 코드]가 주 버튼(메인색), 기다리는 중이면 보조 버튼(sf2)
            val emphasize = state == LinkUiState.Expired || state == LinkUiState.Failed
            FilledButton(
                text = stringResource(if (state == LinkUiState.Failed) R.string.link_retry else R.string.link_new_code),
                container = if (emphasize) colors.primary else colors.surface2,
                content = if (emphasize) colors.onPrimary else colors.text,
                metrics = metrics,
                onClick = onNewCode,
            )
        }
        TextButton(
            onClick = onClose,
            modifier = Modifier.heightIn(min = metrics.buttonHeight),
            shape = RoundedCornerShape(DesignRadius.m),
        ) {
            Text(
                stringResource(R.string.link_close),
                style = metrics.style(DesignType.button).copy(fontWeight = FontWeight.Medium),
                color = colors.selectedText,
            )
        }
    }
}

@Composable
private fun LinkedBody(state: LinkUiState.Linked, metrics: LinkMetrics, colors: DesignColors, onClose: () -> Unit) {
    val body = metrics.style(DesignType.body)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(metrics.gap(DesignSpace.s))) {
        Icon(Icons.Filled.Check, contentDescription = null, tint = colors.success, modifier = Modifier.size(metrics.icon))
        Text(stringResource(R.string.link_done), style = body.copy(fontWeight = FontWeight.Medium), color = colors.success)
    }
    Spacer(Modifier.height(metrics.gap(DesignSpace.m)))
    val pet = state.pet
    Text(
        text = if (pet != null) stringResource(R.string.link_linked_title, pet.name) else stringResource(R.string.link_linked_no_profile_title),
        style = metrics.style(DesignType.headline),
        color = colors.text,
    )
    Spacer(Modifier.height(metrics.gap(DesignSpace.xxxl)))

    HorizontalDivider(thickness = 2.dp, color = colors.line)
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = metrics.gap(DesignSpace.xl)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(metrics.gap(DesignSpace.xl)),
    ) {
        if (pet != null) PetAvatar(initial = pet.name.take(1), size = metrics.avatar)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(metrics.gap(DesignSpace.xxs))) {
            Text(
                text = pet?.name ?: stringResource(R.string.link_linked_no_profile_hint),
                style = metrics.style(DesignType.title),
                color = colors.text,
            )
            Text(
                text = if (pet != null) stringResource(R.string.link_linked_hint, pet.name) else "",
                style = body,
                color = colors.subText,
            )
        }
        pet?.breed?.let { Text(it, style = body, color = colors.subText) }
    }
    HorizontalDivider(thickness = 2.dp, color = colors.line)

    Spacer(Modifier.height(metrics.gap(DesignSpace.section)))
    FilledButton(
        text = stringResource(R.string.link_confirm),
        container = colors.primary,
        content = colors.onPrimary,
        metrics = metrics,
        onClick = onClose,
        modifier = Modifier.width(metrics.primaryWidth),
    )
}

@Composable
private fun ProgressBar(fraction: Float, width: Dp, colors: DesignColors) {
    Box(
        Modifier
            .width(width)
            .height(4.dp)
            .background(colors.line, RoundedCornerShape(2.dp)),
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .background(colors.primary, RoundedCornerShape(2.dp)),
        )
    }
}

@Composable
private fun FilledButton(
    text: String,
    container: Color,
    content: Color,
    metrics: LinkMetrics,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content),
        shape = RoundedCornerShape(DesignRadius.m),
        elevation = null,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = metrics.gap(DesignSpace.xxxl)),
        modifier = modifier.heightIn(min = metrics.buttonHeight),
    ) {
        Text(text, style = metrics.style(DesignType.button))
    }
}

/**
 * 대기 중이면 1초마다 남은 시간을 다시 계산한다 ([linkCountdownOf]). 대기 중이 아니면 null.
 * @param since 기다리는 코드를 만든 시각 (`LinkViewModel.waitingSince`)
 */
@Composable
fun rememberLinkCountdown(since: Instant?, now: () -> Instant): LinkCountdown? {
    if (since == null) return null
    var countdown by remember(since) { mutableStateOf(linkCountdownOf(since, now())) }
    LaunchedEffect(since) {
        while (true) {
            countdown = linkCountdownOf(since, now())
            delay(1_000)
        }
    }
    return countdown
}

@Preview(widthDp = 1682, heightDp = 1210)
@Composable
private fun LinkWaitingWidePreview() {
    MeongCareTheme(darkTheme = false) {
        LinkScreen(LinkUiState.Waiting("K7P 3QX", 3), onNewCode = {}, onClose = {}, countdown = LinkCountdown("02:59", 0.3f))
    }
}

@Preview(widthDp = 1682, heightDp = 1210)
@Composable
private fun LinkWaitingWideDarkPreview() {
    MeongCareTheme(darkTheme = true) {
        LinkScreen(LinkUiState.Waiting("K7P 3QX", 3), onNewCode = {}, onClose = {}, countdown = LinkCountdown("02:59", 0.3f))
    }
}

@Preview(widthDp = 411, heightDp = 891)
@Composable
private fun LinkWaitingNarrowPreview() {
    MeongCareTheme(darkTheme = false) {
        LinkScreen(LinkUiState.Waiting("K7P 3QX", 10), onNewCode = {}, onClose = {}, countdown = LinkCountdown("09:41", 0.97f))
    }
}

@Preview(widthDp = 1682, heightDp = 1210)
@Composable
private fun LinkExpiredWidePreview() {
    MeongCareTheme(darkTheme = false) { LinkScreen(LinkUiState.Expired, onNewCode = {}, onClose = {}) }
}

@Preview(widthDp = 1682, heightDp = 1210)
@Composable
private fun LinkLinkedWidePreview() {
    MeongCareTheme(darkTheme = false) {
        LinkScreen(LinkUiState.Linked(LinkedPet("콩이", "말티즈")), onNewCode = {}, onClose = {})
    }
}
