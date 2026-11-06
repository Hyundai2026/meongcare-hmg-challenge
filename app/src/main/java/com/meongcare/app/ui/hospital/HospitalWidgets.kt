package com.meongcare.app.ui.hospital

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.meongcare.app.ui.card.LocalCardScale
import com.meongcare.app.ui.card.Notice
import com.meongcare.app.ui.theme.ActionBlue
import com.meongcare.app.ui.theme.Dimens

/** 머리의 작은 둥근 버튼 ([뒤로], [새로 고침]). 터치 높이는 [Dimens.MinTouchTarget] 이상. */
@Composable
internal fun HospitalPillButton(text: String, onClick: () -> Unit, icon: ImageVector? = null, enabled: Boolean = true) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.heightIn(min = Dimens.MinTouchTarget),
    ) {
        icon?.let {
            Icon(it, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text = text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

/** 큰 주 버튼 (파란색) */
@Composable
internal fun HospitalPrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    val scale = LocalCardScale.current
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = ActionBlue, contentColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.heightIn(min = scale.buttonHeight),
    ) {
        icon?.let {
            Icon(it, contentDescription = null, modifier = Modifier.size(30.dp))
            Spacer(Modifier.width(12.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge.copy(fontSize = scale.button))
    }
}

/** 큰 보조 버튼 (테두리) */
@Composable
internal fun HospitalSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    borderColor: Color = MaterialTheme.colorScheme.outline,
) {
    val scale = LocalCardScale.current
    OutlinedButton(
        onClick = onClick,
        border = BorderStroke(2.dp, borderColor),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.heightIn(min = scale.buttonHeight),
    ) {
        icon?.let {
            Icon(it, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(30.dp))
            Spacer(Modifier.width(12.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge.copy(fontSize = scale.button), color = MaterialTheme.colorScheme.onSurface)
    }
}

/** 시연용 표시 같은 작은 안내 묶음 */
@Composable
internal fun NoticeBox(lines: List<String>, modifier: Modifier = Modifier) {
    if (lines.isEmpty()) return
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            lines.forEach { Notice(it) }
        }
    }
}
