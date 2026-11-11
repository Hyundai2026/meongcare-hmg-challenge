package com.meongcare.app.ui.voice

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.meongcare.app.R
import com.meongcare.app.domain.card.Symptom
import com.meongcare.app.domain.voice.VoiceFailure
import com.meongcare.app.ui.card.joinedText
import com.meongcare.app.ui.card.symptomLabelRes
import com.meongcare.app.ui.theme.ActionBlue
import com.meongcare.app.ui.theme.Dimens
import com.meongcare.app.ui.theme.MeongCareTheme

/** 이 폭보다 좁으면 버튼을 글 아래로 내린다. */
private val NarrowWidth = 600.dp
private val ButtonMinWidth = 260.dp

/**
 * 증상 고르기 화면 위쪽의 음성 영역 (docs/plans 010 "설계 결정 1"의 표). 정차 중에만 보인다.
 * 마이크는 버튼을 누를 때만 켠다. 결과는 아래 증상 버튼에 체크로 보이고, 사용자가 버튼으로 고친다.
 */
@Composable
fun VoicePanel(state: VoiceScreenState, actions: VoiceActions, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                if (maxWidth < NarrowWidth) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        VoiceStatus(state.voice, Modifier.fillMaxWidth())
                        VoiceButton(state.voice, actions, Modifier.fillMaxWidth())
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        VoiceStatus(state.voice, Modifier.weight(1f))
                        Spacer(Modifier.width(24.dp))
                        VoiceButton(state.voice, actions, Modifier.widthIn(min = ButtonMinWidth))
                    }
                }
            }
            if (state.isSimulated) {
                Text(
                    text = stringResource(R.string.voice_simulated_notice),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** 상태 글. 바뀌면 화면 읽기 프로그램이 읽어 준다. */
@Composable
private fun VoiceStatus(voice: VoiceUiState, modifier: Modifier) {
    val (title, detail) = when (voice) {
        VoiceUiState.Idle -> stringResource(R.string.voice_prompt_title) to stringResource(R.string.voice_prompt_example)
        VoiceUiState.Starting -> stringResource(R.string.voice_starting) to null
        is VoiceUiState.Listening -> stringResource(R.string.voice_listening) to voice.partial.ifBlank { null }
        is VoiceUiState.Heard -> stringResource(R.string.voice_heard, voice.sentence) to
            if (voice.matched.isEmpty()) stringResource(R.string.voice_not_matched)
            else stringResource(R.string.voice_matched, joinedText(voice.matched.map { stringResource(symptomLabelRes(it)) }))
        is VoiceUiState.Failed -> stringResource(failureTextRes(voice.reason)) to null
    }
    Column(
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        if (detail != null) {
            Text(
                detail,
                style = MaterialTheme.typography.bodyLarge,
                color = if (voice is VoiceUiState.Listening) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun VoiceButton(voice: VoiceUiState, actions: VoiceActions, modifier: Modifier) {
    val shape = RoundedCornerShape(16.dp)
    val buttonModifier = modifier.heightIn(min = Dimens.MinTouchTarget)
    when (voice) {
        VoiceUiState.Starting, is VoiceUiState.Listening -> OutlinedButton(
            onClick = actions.onCancel,
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
            shape = shape,
            modifier = buttonModifier,
        ) { Text(stringResource(R.string.voice_cancel), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface) }
        else -> Button(
            onClick = actions.onStart,
            colors = ButtonDefaults.buttonColors(containerColor = ActionBlue, contentColor = Color.White),
            shape = shape,
            modifier = buttonModifier,
        ) {
            Icon(MicIcon, contentDescription = null, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                stringResource(if (voice == VoiceUiState.Idle) R.string.voice_start else R.string.voice_retry),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

private fun failureTextRes(reason: VoiceFailure): Int = when (reason) {
    VoiceFailure.NO_SPEECH -> R.string.voice_failed_no_speech
    VoiceFailure.MIC -> R.string.voice_failed_mic
    VoiceFailure.UNAVAILABLE -> R.string.voice_failed_unavailable
}

/**
 * 마이크 모양. core 아이콘에는 마이크가 없어서 직접 그렸다 (material-icons-extended는 용량 때문에 넣지 않는다, 009와 같은 원칙).
 */
private val MicIcon: ImageVector by lazy {
    val black = SolidColor(Color.Black)
    ImageVector.Builder("Mic", 24.dp, 24.dp, 24f, 24f).apply {
        // 머리 (둥근 막대)
        addPath(PathParser().parsePathString("M12 2.5a3.5 3.5 0 0 1 3.5 3.5v5a3.5 3.5 0 0 1 -7 0v-5A3.5 3.5 0 0 1 12 2.5Z").toNodes(), fill = black)
        // 받침 고리, 대, 바닥
        addPath(
            PathParser().parsePathString("M5.5 11a6.5 6.5 0 0 0 13 0 M12 17.5V21 M8.5 21h7").toNodes(),
            stroke = black,
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
        )
    }.build()
}

@Preview(widthDp = 1600, heightDp = 200)
@Composable
private fun VoicePanelIdleWidePreview() {
    MeongCareTheme(darkTheme = true) {
        Surface { VoicePanel(VoiceScreenState(true, isSimulated = true, voice = VoiceUiState.Idle), VoiceActions(), Modifier.padding(16.dp)) }
    }
}

@Preview(widthDp = 1600, heightDp = 200)
@Composable
private fun VoicePanelListeningWidePreview() {
    MeongCareTheme(darkTheme = false) {
        Surface { VoicePanel(VoiceScreenState(true, false, VoiceUiState.Listening("강아지가 갑자기 토하고")), VoiceActions(), Modifier.padding(16.dp)) }
    }
}

@Preview(widthDp = 1600, heightDp = 200)
@Composable
private fun VoicePanelHeardWidePreview() {
    MeongCareTheme(darkTheme = false) {
        Surface {
            VoicePanel(
                VoiceScreenState(true, false, VoiceUiState.Heard("강아지가 갑자기 토하고 숨을 가쁘게 쉬어요", listOf(Symptom.BREATHING_DIFFICULTY, Symptom.VOMITING))),
                VoiceActions(),
                Modifier.padding(16.dp),
            )
        }
    }
}

@Preview(widthDp = 411, heightDp = 360)
@Composable
private fun VoicePanelNotMatchedNarrowPreview() {
    MeongCareTheme(darkTheme = false) {
        Surface { VoicePanel(VoiceScreenState(true, true, VoiceUiState.Heard("내일 토요일이었으면 좋겠어요", emptyList())), VoiceActions(), Modifier.padding(16.dp)) }
    }
}

@Preview(widthDp = 411, heightDp = 300)
@Composable
private fun VoicePanelFailedNarrowPreview() {
    MeongCareTheme(darkTheme = true) {
        Surface { VoicePanel(VoiceScreenState(true, false, VoiceUiState.Failed(VoiceFailure.MIC)), VoiceActions(), Modifier.padding(16.dp)) }
    }
}
