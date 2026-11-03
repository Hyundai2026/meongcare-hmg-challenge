package com.meongcare.app.ui.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * debug 빌드에서만 보이는 개발자 패널. 화면 아래의 얇은 띠다.
 *
 * - Fake 모드일 때 주행 상태를 손으로 바꾼다.
 * - 주행 화면에도 보인다. 주행 화면에서 정차로 되돌릴 방법이 있어야 하기 때문이다.
 *   debug 전용이라 주행 화면 규칙(AGENTS.md 규칙 3)의 예외로 둔다.
 *
 * 상태를 들고 있지 않는다. 값과 동작은 MainActivity가 넘긴다 (ui가 data를 모르게 하기 위해).
 */
@Composable
fun DebugPanel(
    isFakeMode: Boolean,
    onSetParked: () -> Unit,
    onSetDriving: () -> Unit,
    onSetUnknown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (isFakeMode) "DEBUG · Fake" else "DEBUG · Real",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (isFakeMode) {
                OutlinedButton(onClick = onSetParked) { Text("정차") }
                OutlinedButton(onClick = onSetDriving) { Text("주행") }
                OutlinedButton(onClick = onSetUnknown) { Text("모름") }
            }
        }
    }
}
