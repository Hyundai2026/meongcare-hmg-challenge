package com.meongcare.app.ui.card

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.meongcare.app.R
import com.meongcare.app.domain.card.Symptom
import com.meongcare.app.ui.theme.ActionBlue
import com.meongcare.app.ui.theme.Dimens
import com.meongcare.app.ui.theme.MeongCareTheme
import com.meongcare.app.ui.voice.VoiceActions
import com.meongcare.app.ui.voice.VoicePanel
import com.meongcare.app.ui.voice.VoiceScreenState
import com.meongcare.app.ui.voice.VoiceUiState

private val SymptomButtonHeight = 88.dp
private val SymptomColumnWidth = 200.dp

/**
 * 증상 고르기 (정차 중에만). 글자 입력 대신 큰 버튼으로 여러 개를 고른다.
 * 버튼 순서는 [Symptom] 선언 순서다. 아래 [취소] [카드 만들기]는 스크롤 밖에 고정한다.
 *
 * @param voice 음성 입력 영역 (010). `null`이거나 음성 입력이 없는 구성이면 그리지 않는다.
 */
@Composable
fun SymptomPickerScreen(
    selected: Set<Symptom>,
    onToggle: (Symptom) -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    voice: VoiceScreenState? = null,
    voiceActions: VoiceActions = VoiceActions(),
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val columns = (maxWidth / SymptomColumnWidth).toInt().coerceIn(2, 4)
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Dimens.ScreenPadding, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(stringResource(R.string.symptoms_title), style = MaterialTheme.typography.headlineMedium)
                Text(
                    stringResource(R.string.symptoms_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (voice != null && voice.available) {
                    VoicePanel(state = voice, actions = voiceActions)
                }
                Spacer(Modifier.height(4.dp))
                Symptom.entries.chunked(columns).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        row.forEach { symptom ->
                            SymptomToggle(
                                symptom = symptom,
                                checked = symptom in selected,
                                onToggle = { onToggle(symptom) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        // 마지막 줄이 덜 차도 버튼 폭을 맞춘다
                        repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = Dimens.ScreenPadding, end = Dimens.ScreenPadding, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f).heightIn(min = 72.dp),
                ) { Text(stringResource(R.string.symptoms_cancel), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface) }
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(containerColor = ActionBlue, contentColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(2f).heightIn(min = 72.dp),
                ) {
                    Text(
                        text = if (selected.isEmpty()) stringResource(R.string.symptoms_confirm)
                        else stringResource(R.string.symptoms_confirm_count, selected.size),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

/** 증상 버튼 하나. 선택 상태를 색만으로 나타내지 않도록 체크 표시를 같이 그리고, 접근성 상태(checkbox)를 준다. */
@Composable
private fun SymptomToggle(symptom: Symptom, checked: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (checked) ActionBlue else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (checked) Color.White else MaterialTheme.colorScheme.onSurface,
        border = if (checked) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .height(SymptomButtonHeight)
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = { onToggle() }),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (checked) {
                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(text = stringResource(symptomLabelRes(symptom)), style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
        }
    }
}

@Preview(widthDp = 1682, heightDp = 1210)
@Composable
private fun SymptomPickerWidePreview() {
    MeongCareTheme(darkTheme = true) {
        Surface {
            SymptomPickerScreen(
                selected = setOf(Symptom.COUGH, Symptom.BREATHING_DIFFICULTY, Symptom.VOMITING),
                onToggle = {}, onCancel = {}, onConfirm = {},
                voice = VoiceScreenState(
                    available = true,
                    isSimulated = false,
                    voice = VoiceUiState.Heard("강아지가 갑자기 토하고 숨을 가쁘게 쉬어요", listOf(Symptom.BREATHING_DIFFICULTY, Symptom.VOMITING)),
                ),
            )
        }
    }
}

@Preview(widthDp = 411, heightDp = 891)
@Composable
private fun SymptomPickerNarrowPreview() {
    MeongCareTheme(darkTheme = false) {
        Surface {
            SymptomPickerScreen(
                selected = setOf(Symptom.COUGH),
                onToggle = {}, onCancel = {}, onConfirm = {},
                voice = VoiceScreenState(available = true, isSimulated = true, voice = VoiceUiState.Idle),
            )
        }
    }
}
