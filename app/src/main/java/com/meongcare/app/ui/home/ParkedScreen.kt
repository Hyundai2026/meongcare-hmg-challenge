package com.meongcare.app.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.meongcare.app.R
import com.meongcare.app.domain.pet.PetProfileLoad
import com.meongcare.app.ui.card.Notice
import com.meongcare.app.ui.card.PetAvatar
import com.meongcare.app.ui.card.PetHeader
import com.meongcare.app.ui.card.PetTitle
import com.meongcare.app.ui.card.PreviewPet
import com.meongcare.app.ui.card.medicalCardOf
import com.meongcare.app.ui.theme.ActionBlue
import com.meongcare.app.ui.theme.CardColors
import com.meongcare.app.ui.theme.Dimens
import com.meongcare.app.ui.theme.MeongCareTheme
import java.time.LocalDate
import java.time.ZoneId

/**
 * 정차·주차 중 처음 화면: 반려견 요약 + [진료 카드 만들기].
 *
 * 길안내는 진료 카드의 [길 안내 시작]에서만 시작한다 (사용자 결정 2026-11-05, docs/plans 009).
 * 글자 입력칸은 두지 않는다. 높이가 낮은 화면을 위해 세로 스크롤을 둔다 (스크롤 금지는 주행 화면에만 해당한다).
 *
 * @param isPetProfileSample 반려견 정보가 시연용 샘플이면 그 사실을 알린다
 */
@Composable
fun ParkedScreen(
    pet: PetHeader,
    isPetProfileSample: Boolean,
    onStartCard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Dimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
    ) {
        Text(text = stringResource(R.string.parked_status), style = MaterialTheme.typography.headlineMedium)
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = CardColors.Surface,
            contentColor = CardColors.Text,
            border = BorderStroke(1.dp, CardColors.Border),
            modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth(),
        ) {
            Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                PetAvatar(initial = (pet as? PetHeader.Registered)?.initial, size = 112.dp)
                Spacer(Modifier.width(24.dp))
                PetTitle(pet, modifier = Modifier.weight(1f))
            }
        }
        if (isPetProfileSample && pet is PetHeader.Registered) Notice(stringResource(R.string.card_sample_pet_notice))
        Text(text = stringResource(R.string.home_card_hint), style = MaterialTheme.typography.bodyLarge)
        Button(
            onClick = onStartCard,
            colors = ButtonDefaults.buttonColors(containerColor = ActionBlue, contentColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth().heightIn(min = Dimens.DrivingPrimaryButtonHeight),
        ) {
            Text(text = stringResource(R.string.home_start_card), style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Preview(widthDp = 960, heightDp = 540)
@Composable
private fun ParkedScreenPreview() {
    MeongCareTheme {
        Surface {
            ParkedScreen(
                pet = medicalCardOf(PetProfileLoad.Loaded(PreviewPet), emptySet(), LocalDate.of(2026, 11, 5), ZoneId.of("Asia/Seoul")).pet,
                isPetProfileSample = true,
                onStartCard = {},
            )
        }
    }
}
