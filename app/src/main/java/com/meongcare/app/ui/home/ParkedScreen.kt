package com.meongcare.app.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.meongcare.app.R
import com.meongcare.app.ui.card.CardFlowUiState
import com.meongcare.app.ui.card.MedicalCardActions
import com.meongcare.app.ui.card.PawIcon
import com.meongcare.app.ui.card.PetAvatar
import com.meongcare.app.ui.card.PetHeader
import com.meongcare.app.ui.card.joinedText
import com.meongcare.app.ui.card.previewCardState
import com.meongcare.app.ui.card.symptomLabelRes
import com.meongcare.app.ui.guidance.GuidanceUiState
import com.meongcare.app.ui.guidance.TripPanel
import com.meongcare.app.ui.theme.DesignColors
import com.meongcare.app.ui.theme.DesignHeight
import com.meongcare.app.ui.theme.DesignRadius
import com.meongcare.app.ui.theme.DesignScale
import com.meongcare.app.ui.theme.DesignSpace
import com.meongcare.app.ui.theme.DesignType
import com.meongcare.app.ui.theme.LocalDesignTokens
import com.meongcare.app.ui.theme.MeongCareTheme

/** 이 폭보다 좁으면 두 열을 위아래로 쌓는다 (폰 세로) */
private val TwoColumnMinWidth = 840.dp
private val DialogButtonMinWidth = 160.dp

/** 프로토타입 2번 홈 글자 (letter-spacing·굵기는 프로토타입 값) */
private val HomeTitle = TextStyle(fontSize = 104.sp, lineHeight = 112.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.03).em)
private val HomeSubtitle = TextStyle(fontSize = 36.sp, lineHeight = 50.sp, fontWeight = FontWeight.Normal, letterSpacing = (-0.01).em)
private val HomeButton = TextStyle(fontSize = 30.sp, lineHeight = 38.sp, fontWeight = FontWeight.SemiBold)
private val HomeLabel = TextStyle(fontSize = 30.sp, lineHeight = 39.sp, fontWeight = FontWeight.Medium)
private val HomeLogo = TextStyle(fontSize = 30.sp, lineHeight = 38.sp, fontWeight = FontWeight.Medium)
private val HomePetName = TextStyle(fontSize = 30.sp, lineHeight = 38.sp, fontWeight = FontWeight.SemiBold)
private val HomePetStatus = TextStyle(fontSize = 26.sp, lineHeight = 34.sp, fontWeight = FontWeight.Normal)

/**
 * 정차·주차 중 처음 화면 = 확정 디자인 프로토타입 2번 "응급 진료" (계획 019).
 *
 * - 위: 왼쪽 로고 글자, 오른쪽 반려견 줄(아바타·이름·상태) + 보호자 웹 연결 버튼(프로토타입 [⇄ 변경] 자리, D2).
 * - 왼쪽 열: "응급 진료" → 두 줄 안내 → [응급 진료 시작 >].
 *   진료 카드로 가는 버튼은 두지 않는다 (사용자 지시 2026-11-13).
 *   프로토타입 음성 힌트 자리에는 시연용 샘플 안내를 둔다 (음성 명령 기능은 없다).
 * - 오른쪽 열: 세로선 + 바로가기 3개 (병원 찾기, 릴렉스, 반려동물 카드).
 *
 * 길안내는 카드나 병원 찾기에서만 시작한다. 글자 입력칸은 없다. 높이가 낮은 화면을 위해 세로 스크롤을 둔다
 * (스크롤 금지는 주행 화면에만 해당한다). 크기는 화면 폭 비율([DesignScale])로 줄인다.
 *
 * @param isPetProfileSample 반려견 정보가 시연용 샘플이면 그 사실을 알린다
 * @param panel 안내 영역 (안내 중·도착일 때). null이면 없다
 * @param onResend 안내 영역의 [다시 보내기]
 * @param linkRow 보호자 웹 연결 (014). [onOpenLink]는 [보호자 웹과 연결], [onUnlink]는 확인 뒤 [끊기]
 * @param onFindHospital 바로가기 [병원 찾기]. 안내 중·도착이면 비활성
 * @param onOpenRelax 바로가기 [릴렉스] (시연용 화면)
 * @param onOpenPetCard 바로가기 [반려동물 카드]
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
    onFindHospital: () -> Unit = {},
    onOpenRelax: () -> Unit = {},
    onOpenPetCard: () -> Unit = {},
) {
    val colors = LocalDesignTokens.current.colors
    BoxWithConstraints(modifier = modifier.background(colors.homeBackground)) {
        val scale = DesignScale.of(maxWidth)
        val twoColumns = maxWidth >= TwoColumnMinWidth
        val screenHeight = maxHeight
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .heightIn(min = screenHeight)
                .fillMaxWidth()
                .padding(horizontal = scale.gap(DesignSpace.xxxl), vertical = scale.gap(DesignSpace.xl)),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            TopBar(card.card.pet, linkRow, isPetProfileSample, scale, colors, onOpenLink, onUnlink)
            val guiding = guidance !is GuidanceUiState.Idle
            val left = @Composable { m: Modifier ->
                MainColumn(card, guidance, actions, isPetProfileSample, panel, onResend, scale, colors, m)
            }
            val right = @Composable { m: Modifier ->
                Shortcuts(scale, colors, hospitalEnabled = !guiding, onFindHospital, onOpenRelax, onOpenPetCard, m)
            }
            if (twoColumns) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = scale.gap(DesignSpace.xxxl)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    left(Modifier.weight(55f).padding(start = scale.gap(112.dp), end = scale.gap(DesignSpace.page)))
                    right(
                        Modifier
                            .weight(45f)
                            .leftLine(colors, scale)
                            .padding(start = scale.gap(56.dp), top = scale.gap(DesignSpace.xxl), bottom = scale.gap(DesignSpace.xxl)),
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = scale.gap(DesignSpace.xl)),
                    verticalArrangement = Arrangement.spacedBy(scale.gap(DesignSpace.section)),
                ) {
                    left(Modifier.fillMaxWidth())
                    right(Modifier.fillMaxWidth())
                }
            }
            Spacer(Modifier.height(scale.gap(DesignSpace.page)))
        }
    }
}

/** 왼쪽 세로선 (프로토타입 border-left 2px) */
private fun Modifier.leftLine(colors: DesignColors, scale: DesignScale): Modifier = drawBehind {
    val w = scale.gap(2.dp).coerceAtLeast(1.dp).toPx()
    drawLine(colors.homeLine, Offset(w / 2, 0f), Offset(w / 2, size.height), strokeWidth = w)
}

/** 위쪽 줄: 로고 글자 / 반려견 줄 + 연결 버튼 */
@Composable
private fun TopBar(
    pet: PetHeader,
    linkRow: LinkRow,
    isSample: Boolean,
    scale: DesignScale,
    colors: DesignColors,
    onOpenLink: () -> Unit,
    onUnlink: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(PawIcon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(scale.gap(44.dp)))
        Spacer(Modifier.width(scale.gap(DesignSpace.s)))
        Text(stringResource(R.string.app_name), style = scale.style(HomeLogo), color = colors.homeText)
        Spacer(Modifier.weight(1f))
        val registered = pet as? PetHeader.Registered
        PetAvatar(initial = registered?.initial, size = scale.gap(64.dp))
        Spacer(Modifier.width(scale.gap(DesignSpace.m)))
        Column {
            Text(
                registered?.name ?: stringResource(R.string.home_pet_none),
                style = scale.style(HomePetName),
                color = colors.homeText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val status = when {
                linkRow is LinkRow.Unlinked && linkRow.revoked -> stringResource(R.string.link_revoked) to colors.error
                linkRow == LinkRow.Linked -> "● " + stringResource(R.string.home_pet_linked) to colors.success
                isSample && registered != null -> stringResource(R.string.home_pet_sample) to colors.homeSubText
                else -> null
            }
            status?.let { (text, color) -> Text(text, style = scale.style(HomePetStatus), color = color) }
        }
        LinkAction(linkRow, registered?.name, scale, colors, onOpenLink, onUnlink)
    }
}

/**
 * 보호자 웹 연결 버튼 (014, 프로토타입 [⇄ 변경] 자리). 연결 전 [보호자 웹과 연결] / 연결됨 [연결 끊기] (확인 대화).
 * [LinkRow.Hidden](서버 설정 없음·안내 중·도착)이면 없다.
 */
@Composable
private fun LinkAction(
    row: LinkRow,
    petName: String?,
    scale: DesignScale,
    colors: DesignColors,
    onOpenLink: () -> Unit,
    onUnlink: () -> Unit,
) {
    if (row == LinkRow.Hidden) return
    var confirming by rememberSaveable { mutableStateOf(false) }
    Spacer(Modifier.width(scale.gap(DesignSpace.l)))
    TextButton(
        onClick = if (row == LinkRow.Linked) ({ confirming = true }) else onOpenLink,
        shape = RoundedCornerShape(DesignRadius.m),
        contentPadding = PaddingValues(horizontal = scale.gap(DesignSpace.m)),
        modifier = Modifier.heightIn(min = scale.touch(DesignHeight.buttonMedium)),
    ) {
        Text(
            stringResource(if (row == LinkRow.Linked) R.string.link_unlink else R.string.link_start),
            style = scale.style(HomeLabel),
            color = colors.primary,
        )
    }
    if (confirming && row == LinkRow.Linked) {
        val buttonHeight = scale.touch(DesignHeight.buttonMedium)
        AlertDialog(
            onDismissRequest = { confirming = false },
            text = {
                Text(
                    if (petName != null) stringResource(R.string.link_unlink_confirm, petName) else stringResource(R.string.link_unlink_confirm_no_name),
                    style = scale.style(DesignType.body),
                )
            },
            // 차 화면은 팔을 뻗어 누르므로 대화 상자 버튼도 크게 둔다
            confirmButton = {
                Button(
                    onClick = {
                        confirming = false
                        onUnlink()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary),
                    shape = RoundedCornerShape(DesignRadius.m),
                    modifier = Modifier.heightIn(min = buttonHeight).widthIn(min = DialogButtonMinWidth),
                ) {
                    Text(stringResource(R.string.link_unlink_do), style = scale.style(HomeButton))
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { confirming = false },
                    border = BorderStroke(2.dp, colors.line),
                    shape = RoundedCornerShape(DesignRadius.m),
                    modifier = Modifier.heightIn(min = buttonHeight).widthIn(min = DialogButtonMinWidth),
                ) {
                    Text(stringResource(R.string.link_cancel), style = scale.style(HomeButton), color = colors.text)
                }
            },
        )
    }
}

/** 왼쪽 열: "응급 진료" + 안내 + 주 버튼 */
@Composable
private fun MainColumn(
    card: CardFlowUiState,
    guidance: GuidanceUiState,
    actions: MedicalCardActions,
    isSample: Boolean,
    panel: TripPanel?,
    onResend: () -> Unit,
    scale: DesignScale,
    colors: DesignColors,
    modifier: Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.Start) {
        // 길안내 상태(안내 영역)는 처음 화면에 보이지 않는다 (사용자 결정 2026-11-14). 주행 화면과 진료 카드 화면이 보인다
        Text(stringResource(R.string.home_title), style = scale.style(HomeTitle), color = colors.homeText)
        Spacer(Modifier.height(scale.gap(28.dp)))
        Column(verticalArrangement = Arrangement.spacedBy(scale.gap(6.dp))) {
            Text(stringResource(R.string.home_subtitle_1), style = scale.style(HomeSubtitle), color = colors.homeSubText)
            Text(stringResource(R.string.home_subtitle_2), style = scale.style(HomeSubtitle).copy(fontWeight = FontWeight.Medium), color = colors.homeText)
        }
        Spacer(Modifier.height(scale.gap(DesignSpace.xxxl)))
        // 처음 화면에는 진료 카드로 가는 버튼([카드 보기]·[새로 만들기])을 두지 않는다 (사용자 지시 2026-11-13).
        // [응급 진료 시작]은 늘 증상 선택으로 간다: 안내 전에 만든 카드가 있으면 새로 시작, 안내 중이면 고른 증상을 고친다.
        PrimaryButton(
            text = stringResource(R.string.home_start_emergency),
            onClick = if (card.hasCard && guidance is GuidanceUiState.Idle) actions.onStartNewCard else actions.onStartCard,
            scale = scale,
            colors = colors,
        )
        if (isSample && card.card.pet is PetHeader.Registered) {
            Spacer(Modifier.height(scale.gap(DesignSpace.l)))
            Text(
                stringResource(R.string.card_sample_pet_notice),
                style = scale.style(DesignType.body),
                color = colors.homeSubText,
            )
        }
    }
}

/** 주 버튼: 높이 96, 최대 폭 640, 글자 왼쪽 + 화살표 오른쪽 */
@Composable
private fun PrimaryButton(text: String, onClick: () -> Unit, scale: DesignScale, colors: DesignColors) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary),
        shape = RoundedCornerShape(DesignRadius.m),
        elevation = null,
        contentPadding = PaddingValues(start = scale.gap(DesignSpace.xxl), end = scale.gap(36.dp)),
        modifier = Modifier
            .widthIn(max = scale.gap(640.dp))
            .fillMaxWidth()
            .heightIn(min = scale.touch(DesignHeight.buttonLarge)),
    ) {
        Text(text, style = scale.style(HomeButton), modifier = Modifier.weight(1f))
        Icon(HomeIcons.Chevron, contentDescription = null, modifier = Modifier.size(scale.gap(32.dp)))
    }
}

/** 오른쪽 열: 바로가기 3개 (프로토타입: 칸 폭 200, 사이 24) */
@Composable
private fun Shortcuts(
    scale: DesignScale,
    colors: DesignColors,
    hospitalEnabled: Boolean,
    onFindHospital: () -> Unit,
    onOpenRelax: () -> Unit,
    onOpenPetCard: () -> Unit,
    modifier: Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(scale.gap(DesignSpace.l), Alignment.CenterHorizontally),
    ) {
        Shortcut(HomeIcons.Hospital, stringResource(R.string.home_shortcut_hospital), scale, colors, onFindHospital, enabled = hospitalEnabled)
        Shortcut(HomeIcons.Relax, stringResource(R.string.home_shortcut_relax), scale, colors, onOpenRelax)
        Shortcut(HomeIcons.PetCard, stringResource(R.string.home_shortcut_pet_card), scale, colors, onOpenPetCard)
    }
}

@Composable
private fun Shortcut(
    icon: ImageVector,
    label: String,
    scale: DesignScale,
    colors: DesignColors,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    val tile: Dp = scale.gap(140.dp).coerceAtLeast(DesignScale.MIN_TOUCH)
    Column(
        modifier = Modifier
            .width(scale.gap(200.dp).coerceAtLeast(tile))
            .alpha(if (enabled) 1f else 0.4f)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(vertical = scale.gap(DesignSpace.s)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(scale.gap(20.dp)),
    ) {
        Box(
            modifier = Modifier.size(tile).background(colors.homeIcon, RoundedCornerShape(scale.gap(DesignRadius.xl))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(scale.gap(48.dp)))
        }
        Text(label, style = scale.style(HomeLabel), color = colors.homeText, maxLines = 1)
    }
}

@Preview(widthDp = 1682, heightDp = 1210)
@Composable
private fun ParkedScreenEmptyPreview() {
    MeongCareTheme(darkTheme = false) {
        Surface {
            ParkedScreen(
                card = previewCardState().copy(session = com.meongcare.app.domain.card.CardSessionState.None),
                guidance = GuidanceUiState.Idle("시연용 동물병원"),
                actions = MedicalCardActions(),
                isPetProfileSample = true,
                linkRow = LinkRow.Unlinked(),
            )
        }
    }
}

@Preview(widthDp = 1682, heightDp = 1210)
@Composable
private fun ParkedScreenWithCardPreview() {
    MeongCareTheme(darkTheme = false) {
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
