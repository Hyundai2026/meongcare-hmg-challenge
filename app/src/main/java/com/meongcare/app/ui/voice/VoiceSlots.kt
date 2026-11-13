package com.meongcare.app.ui.voice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meongcare.app.R
import com.meongcare.app.ui.home.HomeIcons
import com.meongcare.app.ui.theme.DesignColors
import com.meongcare.app.ui.theme.DesignHeight
import com.meongcare.app.ui.theme.DesignRadius
import com.meongcare.app.ui.theme.DesignScale
import com.meongcare.app.ui.theme.DesignSpace

/*
 * 디자인 시안의 음성 자리 (계획 019). STT/TTS는 아직 연결하지 않았다 (사용자 결정 2026-11-13: 뼈대만).
 * - [VoiceCommandButton]: 시안 머리 오른쪽 [음성 명령]. 지금은 누르면 부르는 쪽이 "준비 중" 안내를 보인다.
 *   나중에 STT를 붙일 때 onClick에서 듣기를 시작하면 된다.
 * - [VoiceGuideLine]: 시안의 "🔊 음성 안내 …" 멘트 줄. 지금은 화면 글자뿐이다.
 *   나중에 TTS를 붙일 때 같은 [text]를 읽으면 된다 (화면 글자와 읽는 말이 같게).
 */

private val GuideLabel = TextStyle(fontSize = 30.sp, lineHeight = 42.sp, fontWeight = FontWeight.Medium)
private val GuideText = TextStyle(fontSize = 30.sp, lineHeight = 42.sp)
private val CommandText = TextStyle(fontSize = 30.sp, lineHeight = 38.sp, fontWeight = FontWeight.Medium)

@Composable
fun VoiceCommandButton(onClick: () -> Unit, scale: DesignScale, colors: DesignColors, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = colors.homeIcon, contentColor = colors.homeText),
        shape = RoundedCornerShape(DesignRadius.m),
        elevation = null,
        contentPadding = PaddingValues(horizontal = scale.gap(DesignSpace.l)),
        modifier = modifier.heightIn(min = scale.touch(DesignHeight.buttonLarge)),
    ) {
        Icon(HomeIcons.Mic, contentDescription = null, modifier = Modifier.size(scale.gap(32.dp)))
        androidx.compose.foundation.layout.Spacer(Modifier.size(scale.gap(DesignSpace.s)))
        Text(stringResource(R.string.voice_command), style = scale.style(CommandText))
    }
}

/** "🔊 음성 안내  {text}" */
@Composable
fun VoiceGuideLine(text: String, scale: DesignScale, colors: DesignColors, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(scale.gap(DesignSpace.m)),
    ) {
        Icon(HomeIcons.Speaker, contentDescription = null, tint = colors.primary, modifier = Modifier.size(scale.gap(32.dp)))
        Text(stringResource(R.string.voice_guide_label), style = scale.style(GuideLabel), color = colors.primary)
        Text(text, style = scale.style(GuideText), color = colors.homeText)
    }
}
