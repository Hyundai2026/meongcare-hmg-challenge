package com.meongcare.app.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.meongcare.app.R
import com.meongcare.app.ui.card.CardFlowUiState
import com.meongcare.app.ui.card.CardScale
import com.meongcare.app.ui.card.LocalCardScale
import com.meongcare.app.ui.card.MedicalCardActions
import com.meongcare.app.ui.card.Notice
import com.meongcare.app.ui.card.PetAvatar
import com.meongcare.app.ui.card.PetHeader
import com.meongcare.app.ui.card.PetTitle
import com.meongcare.app.ui.card.joinedText
import com.meongcare.app.ui.card.previewCardState
import com.meongcare.app.ui.card.symptomLabelRes
import com.meongcare.app.ui.guidance.GuidanceUiState
import com.meongcare.app.ui.guidance.GuidancePanel
import com.meongcare.app.ui.guidance.TripPanel
import com.meongcare.app.ui.theme.ActionBlue
import com.meongcare.app.ui.theme.CardColors
import com.meongcare.app.ui.theme.Dimens
import com.meongcare.app.ui.theme.MeongCareTheme

private val ContentMaxWidth = 960.dp
private val DialogButtonMinWidth = 160.dp

/**
 * 정차·주차 중 처음 화면: 반려견 요약 + [진료 카드 만들기].
 *
 * 만든 카드가 있거나 안내 중·도착이면([홈]으로 온 경우) 그 상태와 [카드 보기]를 먼저 보인다.
 * 안내 중·도착이면 카드 화면과 같은 안내 영역([GuidancePanel], 013)을 위에 둔다.
 * 안내가 없을 때만 [새로 만들기]로 있던 카드를 버리고 다시 만든다.
 *
 * 길안내는 진료 카드의 [길 안내 시작]에서만 시작한다 (사용자 결정 2026-11-05, docs/plans 009).
 * 글자 입력칸은 두지 않는다. 높이가 낮은 화면을 위해 세로 스크롤을 둔다 (스크롤 금지는 주행 화면에만 해당한다).
 *
 * @param isPetProfileSample 반려견 정보가 시연용 샘플이면 그 사실을 알린다
 * @param panel 안내 영역 (안내 중·도착일 때). null이면 없다
 * @param onResend 안내 영역의 [다시 보내기]
 * @param linkRow 보호자 웹 연결 줄 (014). [onOpenLink]는 [보호자 웹과 연결], [onUnlink]는 확인 뒤 [끊기]
 */
@Composable
fun ParkedScreen(
    card: CardFlowUiState,
    guidance: GuidanceUiState,
    actions: MedicalCardActions,
    isPetProfileSample: Boolean,
    modifier: Modifier = Modifier,
    panel: TripPanel? = null,
    onResend: () -> Unit = {},
    linkRow: LinkRow = LinkRow.Hidden,
    onOpenLink: () -> Unit = {},
    onUnlink: () -> Unit = {},
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
                Text(text = stringResource(R.string.parked_status), style = MaterialTheme.typography.headlineMedium)
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = CardColors.Surface,
                    contentColor = CardColors.Text,
                    border = BorderStroke(1.dp, CardColors.Border),
                    shadowElevation = 2.dp,
                    modifier = Modifier.widthIn(max = ContentMaxWidth).fillMaxWidth(),
                ) {
                    Row(modifier = Modifier.padding(28.dp), verticalAlignment = Alignment.CenterVertically) {
                        PetAvatar(initial = (card.card.pet as? PetHeader.Registered)?.initial, size = if (wide) 160.dp else 104.dp)
                        Spacer(Modifier.width(28.dp))
                        PetTitle(card.card.pet, modifier = Modifier.weight(1f))
                    }
                }
                if (isPetProfileSample && card.card.pet is PetHeader.Registered) Notice(stringResource(R.string.card_sample_pet_notice))

                val showsCurrent = card.hasCard || guidance !is GuidanceUiState.Idle
                if (showsCurrent) {
                    panel?.let { GuidancePanel(it, onResend, Modifier.widthIn(max = ContentMaxWidth)) }
                    CurrentCardPanel(card, actions.onOpenCard)
                    if (guidance is GuidanceUiState.Idle) {
                        OutlinedButton(
                            onClick = actions.onStartNewCard,
                            border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.widthIn(max = ContentMaxWidth).fillMaxWidth().heightIn(min = scale.buttonHeight),
                        ) {
                            Text(
                                stringResource(R.string.home_new_card),
                                style = MaterialTheme.typography.labelLarge.copy(fontSize = scale.button),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                } else {
                    Text(text = stringResource(R.string.home_card_hint), style = MaterialTheme.typography.bodyLarge)
                    PrimaryButton(stringResource(R.string.home_start_card), actions.onStartCard)
                }
                LinkRowPanel(linkRow, petName = (card.card.pet as? PetHeader.Registered)?.name, onOpenLink = onOpenLink, onUnlink = onUnlink)
            }
        }
    }
}

/** 만든 카드와 [카드 보기]. 안내 중·도착이면 위에 안내 영역이 따로 있다 */
@Composable
private fun CurrentCardPanel(card: CardFlowUiState, onOpenCard: () -> Unit) {
    val scale = LocalCardScale.current
    val title = stringResource(R.string.home_current_card_title)
    val symptoms = card.card.symptoms
        .takeIf { it.isNotEmpty() }
        ?.let { list -> stringResource(R.string.home_symptoms_line, joinedText(list.map { stringResource(symptomLabelRes(it)) })) }
        ?: stringResource(R.string.card_value_no_symptoms)
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(2.dp, ActionBlue),
        modifier = Modifier.widthIn(max = ContentMaxWidth).fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleLarge.copy(fontSize = scale.title), fontWeight = FontWeight.Bold)
            Text(text = symptoms, style = MaterialTheme.typography.bodyLarge.copy(fontSize = scale.row))
            PrimaryButton(stringResource(R.string.home_open_card), onOpenCard)
        }
    }
}

/**
 * 보호자 웹 연결 줄 (014). 연결 전: [보호자 웹과 연결] / 연결됨: "보호자 웹과 연결됨" + [연결 끊기] (확인 대화).
 * 정차 중 처음 화면에만 있다 (주행 화면에는 없다).
 */
@Composable
private fun LinkRowPanel(row: LinkRow, petName: String?, onOpenLink: () -> Unit, onUnlink: () -> Unit) {
    if (row == LinkRow.Hidden) return
    val scale = LocalCardScale.current
    var confirming by rememberSaveable { mutableStateOf(false) }
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.widthIn(max = ContentMaxWidth).fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when (row) {
                is LinkRow.Unlinked -> {
                    if (row.revoked) {
                        Text(
                            stringResource(R.string.link_revoked),
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = scale.row),
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Text(stringResource(R.string.link_start_hint), style = MaterialTheme.typography.bodyLarge.copy(fontSize = scale.row))
                    LinkButton(stringResource(R.string.link_start), onOpenLink)
                }
                LinkRow.Linked -> {
                    Text(
                        stringResource(R.string.link_status_linked),
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = scale.row),
                        fontWeight = FontWeight.Bold,
                    )
                    LinkButton(stringResource(R.string.link_unlink)) { confirming = true }
                }
                LinkRow.Hidden -> Unit
            }
        }
    }
    if (confirming && row == LinkRow.Linked) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            text = {
                Text(
                    if (petName != null) stringResource(R.string.link_unlink_confirm, petName) else stringResource(R.string.link_unlink_confirm_no_name),
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = scale.row),
                )
            },
            // 차 화면은 팔을 뻗어 누르므로 대화 상자 버튼도 크게 둔다
            confirmButton = {
                Button(
                    onClick = {
                        confirming = false
                        onUnlink()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ActionBlue, contentColor = Color.White),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.heightIn(min = scale.buttonHeight).widthIn(min = DialogButtonMinWidth),
                ) {
                    Text(stringResource(R.string.link_unlink_do), style = MaterialTheme.typography.labelLarge.copy(fontSize = scale.button))
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { confirming = false },
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.heightIn(min = scale.buttonHeight).widthIn(min = DialogButtonMinWidth),
                ) {
                    Text(
                        stringResource(R.string.link_cancel),
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = scale.button),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            },
        )
    }
}

@Composable
private fun LinkButton(text: String, onClick: () -> Unit) {
    val scale = LocalCardScale.current
    OutlinedButton(
        onClick = onClick,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth().heightIn(min = scale.buttonHeight),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge.copy(fontSize = scale.button), color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun PrimaryButton(text: String, onClick: () -> Unit) {
    val scale = LocalCardScale.current
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = ActionBlue, contentColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.widthIn(max = ContentMaxWidth).fillMaxWidth().heightIn(min = Dimens.DrivingPrimaryButtonHeight),
    ) {
        Text(text = text, style = MaterialTheme.typography.titleLarge.copy(fontSize = scale.button))
    }
}

@Preview(widthDp = 1682, heightDp = 1210)
@Composable
private fun ParkedScreenWithCardPreview() {
    MeongCareTheme(darkTheme = true) {
        Surface {
            ParkedScreen(
                card = previewCardState().copy(atHome = true),
                guidance = GuidanceUiState.Guiding("시연용 동물병원", 12),
                actions = MedicalCardActions(),
                isPetProfileSample = true,
            )
        }
    }
}
