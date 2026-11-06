package com.meongcare.app.ui.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.meongcare.app.ui.theme.MeongCareTheme

/**
 * debug 빌드에서만 보이는 개발자 패널. 화면 아래의 얇은 띠다.
 *
 * - 주행 상태가 Fake일 때 주행 상태를 손으로 바꾼다 (Fake 모드, 일반 폰의 시연 모드).
 * - 데이터 모드를 Fake → 시연 → Real 순서로 바꾼다 (앱이 다시 시작된다).
 * - 주행 화면에도 보인다. 주행 화면에서 정차로 되돌릴 방법이 있어야 하기 때문이다.
 *   debug 전용이라 주행 화면 규칙(AGENTS.md 규칙 3)의 예외로 둔다.
 *
 * 좁은 세로 화면(Pleos 기본은 세로, 창 1개)에서도 깨지지 않게 줄을 나눈다:
 * 윗줄은 모드 표시와 전환 버튼, 다음 줄은 Fake 상태 버튼.
 * - [location]이 있으면 셋째 줄에 [위치 묻기]와 결과를 보인다 (011). 주행 중에는 화면 쪽이 null을 넘겨 숨긴다.
 *
 * 상태를 들고 있지 않는다. 값과 동작은 MainActivity가 넘긴다 (ui가 data를 모르게 하기 위해).
 */
@Composable
fun DebugPanel(
    modeLabel: String,
    nextModeLabel: String,
    showsFakeDrivingControls: Boolean,
    onSetParked: () -> Unit,
    onSetDriving: () -> Unit,
    onSetUnknown: () -> Unit,
    onToggleDataMode: () -> Unit,
    modifier: Modifier = Modifier,
    location: DebugLocationRow? = null,
    onAskLocation: () -> Unit = {},
    onFakeOwnerClaim: (() -> Unit)? = null,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = modeLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                // 누르면 저장하고 앱을 다시 시작한다
                DebugButton(
                    label = nextModeLabel,
                    onClick = onToggleDataMode,
                )
            }
            if (showsFakeDrivingControls) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DebugButton("정차", onSetParked, Modifier.weight(1f))
                    DebugButton("주행", onSetDriving, Modifier.weight(1f))
                    DebugButton("모름", onSetUnknown, Modifier.weight(1f))
                }
            }
            // Fake 보호자 연결 (014): 연결 코드 화면에서만. 보호자가 웹에 코드를 입력한 것처럼 한다
            if (onFakeOwnerClaim != null) {
                DebugButton("가짜 보호자 코드 입력", onFakeOwnerClaim)
            }
            if (location != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DebugButton("위치 묻기", onAskLocation, enabled = !location.isAsking)
                    Text(
                        text = location.text,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun DebugButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        modifier = modifier,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Preview(widthDp = 360)
@Composable
private fun DebugPanelNarrowPreview() {
    MeongCareTheme {
        DebugPanel(
            modeLabel = "DEBUG · 시연 (주행 Fake)",
            nextModeLabel = "Real로 전환",
            showsFakeDrivingControls = true,
            onSetParked = {},
            onSetDriving = {},
            onSetUnknown = {},
            onToggleDataMode = {},
        )
    }
}

@Preview(widthDp = 360)
@Composable
private fun DebugPanelLocationPreview() {
    MeongCareTheme {
        DebugPanel(
            modeLabel = "DEBUG · Real · 내비 전부",
            nextModeLabel = "Fake로 전환",
            showsFakeDrivingControls = false,
            onSetParked = {},
            onSetDriving = {},
            onSetUnknown = {},
            onToggleDataMode = {},
            location = DebugLocationRow("위치(내비): 37.41272, 127.09468 · 수정구 시흥동", isAsking = false),
        )
    }
}
