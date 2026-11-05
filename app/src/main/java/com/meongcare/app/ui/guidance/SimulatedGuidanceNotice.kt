package com.meongcare.app.ui.guidance

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.meongcare.app.R

/**
 * 시연용 길안내 안내 한 줄 (007). 진료 카드 화면 아래에 보인다. 주행 화면에는 넣지 않는다.
 * 출품 유의사항(제출 서류가 사실과 다르면 선정 취소) 때문에 숨기지 않는다.
 */
@Composable
fun SimulatedGuidanceNotice(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.guidance_simulated_notice),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}
