package com.meongcare.app.ui.hospital

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.meongcare.app.R
import com.meongcare.app.domain.hospital.RankedHospital
import com.meongcare.app.ui.card.CardScale
import com.meongcare.app.ui.card.LocalCardScale
import com.meongcare.app.ui.theme.ActionBlue
import com.meongcare.app.ui.theme.CardColors
import com.meongcare.app.ui.theme.Dimens
import com.meongcare.app.ui.theme.MeongCareTheme

/**
 * 고른 병원 확인 (정차 중에만, 계획 012 "설계 결정 6·8"의 3).
 *
 * 무엇을 보내고 무엇을 보내지 않는지 매번 보인다. [출발]을 누르는 것이 이번 전송의 동의다
 * (ux-guidelines "제시된 동의 버튼을 누르는 행동 외의 행동을 동의로 간주해서는 안 됩니다").
 * [카드 없이 길안내만]은 보내지 않을 길이다 (사용자 결정 U2).
 *
 * @param phoneNumberPanel 전화 앱을 열지 못했을 때 번호를 크게 보이는 영역 (카드 화면과 같은 것)
 * @param withCard false면 카드 없이 연 병원 찾기다 (홈 [병원 찾기], 019): 보낼 카드가 없으니 [길 안내 시작]만 있다
 */
@Composable
fun HospitalConfirmScreen(
    choice: RankedHospital,
    onBack: () -> Unit,
    onCall: (String) -> Unit,
    onDepartWithoutCard: () -> Unit,
    onDepart: () -> Unit,
    modifier: Modifier = Modifier,
    phoneNumberPanel: @Composable () -> Unit = {},
    withCard: Boolean = true,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val wide = maxWidth >= 840.dp
        val sideBySide = maxWidth >= 600.dp
        CompositionLocalProvider(LocalCardScale provides if (wide) CardScale.Wide else CardScale.Narrow) {
            val scale = LocalCardScale.current
            val values = hospitalRowValuesOf(choice)
            Column(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = Dimens.ScreenPadding, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    HospitalPillButton(stringResource(R.string.finder_back), onBack, Icons.AutoMirrored.Filled.ArrowBack)
                    Text(
                        stringResource(R.string.confirm_title),
                        style = MaterialTheme.typography.headlineMedium.copy(fontSize = scale.title),
                    )
                    phoneNumberPanel()
                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = CardColors.Surface,
                        contentColor = CardColors.Text,
                        border = BorderStroke(3.dp, ActionBlue),
                        modifier = Modifier.widthIn(max = 1100.dp).fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                choice.hospital.name,
                                style = MaterialTheme.typography.headlineMedium.copy(fontSize = scale.name),
                                fontWeight = FontWeight.Bold,
                            )
                            values.totalMinutes?.let {
                                Text(
                                    stringResource(R.string.finder_total, it),
                                    style = MaterialTheme.typography.displaySmall.copy(fontSize = scale.name),
                                    color = CardColors.Navy,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            Text(travelAndWaitText(values), style = MaterialTheme.typography.bodyLarge.copy(fontSize = scale.row))
                            Text(
                                stringResource(R.string.confirm_status, statusText(values)),
                                style = MaterialTheme.typography.bodyLarge.copy(fontSize = scale.chip),
                                color = CardColors.Muted,
                            )
                            if (values.isSample) {
                                Text(stringResource(R.string.finder_sample), style = MaterialTheme.typography.labelMedium, color = CardColors.Muted)
                            }
                        }
                    }
                    NoticeBox(
                        if (!withCard && choice.candidate.canReceiveCard) {
                            listOf(stringResource(R.string.confirm_no_card_home))
                        } else if (!choice.candidate.canReceiveCard) {
                            // 근처 검색 병원 (계획 015): 카드를 보내지 않는다. 전화로 먼저 확인하게 한다
                            listOfNotNull(
                                stringResource(R.string.confirm_nearby_no_card),
                                stringResource(R.string.confirm_nearby_call_first),
                                stringResource(R.string.confirm_nearby_source).takeIf { !values.isSample },
                            )
                        } else {
                            listOfNotNull(
                                stringResource(
                                    if (choice.candidate.sendsCardToServer) R.string.confirm_send_notice else R.string.confirm_send_notice_sample,
                                ),
                                stringResource(R.string.confirm_sends),
                                stringResource(R.string.confirm_not_sends),
                                stringResource(R.string.confirm_deleted).takeIf { choice.candidate.sendsCardToServer },
                            )
                        },
                        modifier = Modifier.widthIn(max = 1100.dp),
                    )
                }
                Actions(choice, sideBySide, onCall, onDepartWithoutCard, onDepart, withCard)
            }
        }
    }
}

@Composable
private fun Actions(
    choice: RankedHospital,
    sideBySide: Boolean,
    onCall: (String) -> Unit,
    onDepartWithoutCard: () -> Unit,
    onDepart: () -> Unit,
    withCard: Boolean,
) {
    val phone = choice.hospital.phoneNumber
    val call: (@Composable (Modifier) -> Unit)? = phone?.let {
        { modifier -> HospitalSecondaryButton(stringResource(R.string.card_call_hospital), { onCall(it) }, modifier, Icons.Filled.Phone, ActionBlue) }
    }
    // 근처 검색 병원은 카드를 받지 않으므로 [출발]·[카드 없이 길안내만] 대신 [길 안내 시작] 하나다 (계획 015)
    val nearby = !choice.candidate.canReceiveCard || !withCard
    val withoutCard: (@Composable (Modifier) -> Unit)? = if (nearby) null else { modifier ->
        HospitalSecondaryButton(stringResource(R.string.confirm_without_card), onDepartWithoutCard, modifier)
    }
    val depart: @Composable (Modifier) -> Unit = if (nearby) {
        { modifier -> HospitalPrimaryButton(stringResource(R.string.confirm_start_guidance), onDepartWithoutCard, modifier, Icons.Filled.Send) }
    } else {
        { modifier -> HospitalPrimaryButton(stringResource(R.string.confirm_depart), onDepart, modifier, Icons.Filled.Send) }
    }
    Column(
        modifier = Modifier.fillMaxWidth().padding(start = Dimens.ScreenPadding, end = Dimens.ScreenPadding, top = 8.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (sideBySide) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                call?.invoke(Modifier.weight(1f))
                withoutCard?.invoke(Modifier.weight(1f))
                depart(Modifier.weight(1.3f))
            }
        } else {
            call?.invoke(Modifier.fillMaxWidth())
            withoutCard?.invoke(Modifier.fillMaxWidth())
            depart(Modifier.fillMaxWidth())
        }
    }
}

@Preview(widthDp = 1682, heightDp = 1210)
@Composable
private fun HospitalConfirmWidePreview() {
    MeongCareTheme(darkTheme = true) {
        Surface { HospitalConfirmScreen(previewRanking().ranking.ranked.first(), {}, {}, {}, {}) }
    }
}

@Preview(widthDp = 411, heightDp = 891)
@Composable
private fun HospitalConfirmNarrowPreview() {
    MeongCareTheme(darkTheme = false) {
        Surface { HospitalConfirmScreen(previewRanking().ranking.ranked.first(), {}, {}, {}, {}) }
    }
}
