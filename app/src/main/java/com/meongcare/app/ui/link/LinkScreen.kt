package com.meongcare.app.ui.link

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meongcare.app.R
import com.meongcare.app.ui.card.CardScale
import com.meongcare.app.ui.card.LocalCardScale
import com.meongcare.app.ui.theme.ActionBlue
import com.meongcare.app.ui.theme.CardColors
import com.meongcare.app.ui.theme.Dimens
import com.meongcare.app.ui.theme.LinkText
import com.meongcare.app.ui.theme.MeongCareTheme

private val ContentMaxWidth = 960.dp

/**
 * 보호자 웹 연결 화면 (014). **정차 중에만** 보인다: 주행이면 `homeContentOf`가 먼저 주행 화면을 고른다.
 * 글자 입력칸은 없다. 코드는 차가 보이고 보호자가 폰에 입력한다 (AGENTS.md 규칙 3).
 *
 * 코드 카드는 테마와 상관없는 흰 바탕([CardColors])이라 다크 모드에서도 읽기 쉽다.
 */
@Composable
fun LinkScreen(
    state: LinkUiState,
    onNewCode: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val wide = maxWidth >= 840.dp
        CompositionLocalProvider(LocalCardScale provides if (wide) CardScale.Wide else CardScale.Narrow) {
            val scale = LocalCardScale.current
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(Dimens.ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
            ) {
                Text(
                    text = stringResource(R.string.link_title),
                    style = MaterialTheme.typography.headlineMedium,
                )
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = CardColors.Surface,
                    contentColor = CardColors.Text,
                    border = BorderStroke(1.dp, CardColors.Border),
                    shadowElevation = 2.dp,
                    modifier = Modifier.widthIn(max = ContentMaxWidth).fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(if (wide) 40.dp else 24.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        LinkBody(state, wide)
                    }
                }
                LinkButtons(state, scale, onNewCode, onClose)
            }
        }
    }
}

@Composable
private fun LinkBody(state: LinkUiState, wide: Boolean) {
    val scale = LocalCardScale.current
    val bodyStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = scale.row, textAlign = TextAlign.Center)
    val titleStyle = MaterialTheme.typography.titleLarge.copy(fontSize = scale.title, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    when (state) {
        is LinkUiState.Waiting -> {
            Text(
                text = state.code,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = if (wide) LinkText.CodeWide else LinkText.CodeNarrow,
                    lineHeight = if (wide) LinkText.CodeWide else LinkText.CodeNarrow,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp,
                ),
                color = CardColors.Navy,
                maxLines = 1,
            )
            Text(stringResource(R.string.link_instruction), style = bodyStyle)
            Text(
                stringResource(R.string.link_remaining, state.remainingMinutes),
                style = bodyStyle.copy(fontWeight = FontWeight.Bold),
                color = CardColors.Muted,
            )
        }
        is LinkUiState.Linked -> {
            val pet = state.pet
            if (pet != null) {
                Text(stringResource(R.string.link_linked_title, pet.name), style = titleStyle)
                pet.breed?.let { Text(it, style = bodyStyle, color = CardColors.Muted) }
                Text(stringResource(R.string.link_linked_hint, pet.name), style = bodyStyle)
            } else {
                Text(stringResource(R.string.link_linked_no_profile_title), style = titleStyle)
                Text(stringResource(R.string.link_linked_no_profile_hint), style = bodyStyle, color = CardColors.Muted)
            }
        }
        LinkUiState.Expired -> Text(stringResource(R.string.link_expired), style = titleStyle)
        LinkUiState.Failed -> Text(stringResource(R.string.link_failed), style = titleStyle)
        LinkUiState.Creating, LinkUiState.Closed -> Text(stringResource(R.string.link_creating), style = bodyStyle)
    }
}

@Composable
private fun LinkButtons(state: LinkUiState, scale: CardScale, onNewCode: () -> Unit, onClose: () -> Unit) {
    val modifier = Modifier.widthIn(max = ContentMaxWidth).fillMaxWidth()
    when (state) {
        is LinkUiState.Linked -> PrimaryButton(stringResource(R.string.link_confirm), scale, onClose, modifier)
        is LinkUiState.Waiting, LinkUiState.Expired, LinkUiState.Failed -> Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SecondaryButton(stringResource(R.string.link_close), scale, onClose, Modifier.weight(1f))
            PrimaryButton(
                stringResource(if (state == LinkUiState.Failed) R.string.link_retry else R.string.link_new_code),
                scale,
                onNewCode,
                Modifier.weight(1f),
            )
        }
        LinkUiState.Creating, LinkUiState.Closed -> SecondaryButton(stringResource(R.string.link_close), scale, onClose, modifier)
    }
}

@Composable
private fun PrimaryButton(text: String, scale: CardScale, onClick: () -> Unit, modifier: Modifier) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = ActionBlue, contentColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.heightIn(min = scale.buttonHeight),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge.copy(fontSize = scale.button))
    }
}

@Composable
private fun SecondaryButton(text: String, scale: CardScale, onClick: () -> Unit, modifier: Modifier) {
    OutlinedButton(
        onClick = onClick,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.heightIn(min = scale.buttonHeight),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge.copy(fontSize = scale.button), color = MaterialTheme.colorScheme.onSurface)
    }
}

@Preview(widthDp = 1682, heightDp = 1210)
@Composable
private fun LinkWaitingWidePreview() {
    MeongCareTheme(darkTheme = true) {
        Surface { LinkScreen(LinkUiState.Waiting("K7P 3QX", 9), onNewCode = {}, onClose = {}) }
    }
}

@Preview(widthDp = 840, heightDp = 1210)
@Composable
private fun LinkWaitingHalfPreview() {
    MeongCareTheme(darkTheme = false) {
        Surface { LinkScreen(LinkUiState.Waiting("K7P 3QX", 1), onNewCode = {}, onClose = {}) }
    }
}

@Preview(widthDp = 411, heightDp = 891)
@Composable
private fun LinkWaitingNarrowPreview() {
    MeongCareTheme(darkTheme = false) {
        Surface { LinkScreen(LinkUiState.Waiting("K7P 3QX", 10), onNewCode = {}, onClose = {}) }
    }
}

@Preview(widthDp = 411, heightDp = 891)
@Composable
private fun LinkLinkedNarrowPreview() {
    MeongCareTheme(darkTheme = true) {
        Surface { LinkScreen(LinkUiState.Linked(LinkedPet("콩이", "말티즈")), onNewCode = {}, onClose = {}) }
    }
}
