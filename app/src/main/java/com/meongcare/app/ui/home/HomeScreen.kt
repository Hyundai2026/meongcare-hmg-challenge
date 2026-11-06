package com.meongcare.app.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.meongcare.app.domain.hospital.RankedHospital
import com.meongcare.app.domain.trip.FinderStep
import com.meongcare.app.ui.card.CardFlowUiState
import com.meongcare.app.ui.card.MedicalCardActions
import com.meongcare.app.ui.card.MedicalCardScreen
import com.meongcare.app.ui.card.SymptomPickerScreen
import com.meongcare.app.ui.guidance.GuidanceUiState

/**
 * 홈 화면. 두 단계로 보여줄 화면을 고른다.
 * 1. [homeContentOf]: 주행 중(또는 모름)이면 무조건 주행 화면이다 (AGENTS.md 규칙 3). 이 규칙은 바꾸지 않는다.
 * 2. 정차 중(PARKED·ARRIVAL)이면 [parkedContentOf]가 처음 화면 / 증상 고르기 / 진료 카드 중에서 고른다.
 *
 * 전환 애니메이션은 넣지 않는다 (주행 중 움직이는 요소를 줄인다).
 *
 * @param isGuidanceSimulated 길안내가 시연용이면 카드 화면에 그 사실을 알린다. 주행 화면은 바꾸지 않는다.
 * @param isPetProfileSample 반려견 정보가 시연용 샘플이면 정차 화면·카드에 알린다.
 * @param onCloseArrival 도착 카드 [닫기]. 도착 상태를 지우고 카드도 비운다.
 * @param finder 병원 찾기 단계 (012). 목록·확인 화면은 [hospitalList]·[hospitalConfirm]이 그린다.
 * @param onFinderBack 병원 찾기 화면의 시스템 뒤로 (= 화면의 [뒤로])
 * @param bottomBar 화면 아래에 붙는 영역. debug 빌드의 개발자 패널이 들어간다.
 */
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    guidanceUiState: GuidanceUiState,
    cardUiState: CardFlowUiState,
    cardActions: MedicalCardActions,
    onStartGuidance: () -> Unit,
    onCancelGuidance: () -> Unit,
    onCloseArrival: () -> Unit,
    isGuidanceSimulated: Boolean,
    isPetProfileSample: Boolean,
    modifier: Modifier = Modifier,
    isRouteOnNavi: Boolean = false,
    finder: FinderStep = FinderStep.None,
    onFinderBack: () -> Unit = {},
    hospitalList: @Composable () -> Unit = {},
    hospitalConfirm: @Composable (RankedHospital) -> Unit = {},
    cardExtras: CardExtras = CardExtras(),
    bottomBar: @Composable () -> Unit = {},
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (homeContentOf(uiState, guidanceUiState)) {
                    HomeContent.PARKED, HomeContent.ARRIVAL -> ParkedFlow(
                        guidanceUiState = guidanceUiState,
                        cardUiState = cardUiState,
                        cardActions = cardActions,
                        onStartGuidance = onStartGuidance,
                        onCancelGuidance = onCancelGuidance,
                        onCloseArrival = onCloseArrival,
                        isGuidanceSimulated = isGuidanceSimulated,
                        isPetProfileSample = isPetProfileSample,
                        isRouteOnNavi = isRouteOnNavi,
                        finder = finder,
                        onFinderBack = onFinderBack,
                        hospitalList = hospitalList,
                        hospitalConfirm = hospitalConfirm,
                        cardExtras = cardExtras,
                    )
                    HomeContent.DRIVING -> DrivingScreen(guidanceUiState)
                }
            }
            bottomBar()
        }
    }
}

/** 정차 중에만 불린다. */
@Composable
private fun ParkedFlow(
    guidanceUiState: GuidanceUiState,
    cardUiState: CardFlowUiState,
    cardActions: MedicalCardActions,
    onStartGuidance: () -> Unit,
    onCancelGuidance: () -> Unit,
    onCloseArrival: () -> Unit,
    isGuidanceSimulated: Boolean,
    isPetProfileSample: Boolean,
    isRouteOnNavi: Boolean,
    finder: FinderStep,
    onFinderBack: () -> Unit,
    hospitalList: @Composable () -> Unit,
    hospitalConfirm: @Composable (RankedHospital) -> Unit,
    cardExtras: CardExtras,
) {
    when (parkedContentOf(guidanceUiState, cardUiState.session, cardUiState.atHome, finder)) {
        ParkedContent.HOME -> ParkedScreen(
            card = cardUiState,
            guidance = guidanceUiState,
            actions = cardActions,
            isPetProfileSample = isPetProfileSample,
        )
        ParkedContent.SYMPTOMS -> {
            // 시스템 뒤로 = [취소]
            BackHandler(onBack = cardActions.onCancelPicking)
            SymptomPickerScreen(
                selected = cardUiState.pickingDraft,
                onToggle = cardActions.onToggleSymptom,
                onCancel = cardActions.onCancelPicking,
                onConfirm = cardActions.onConfirmSymptoms,
            )
        }
        ParkedContent.CARD -> {
            // 카드에서 시스템 뒤로 = [홈] (카드는 남는다)
            BackHandler(onBack = cardActions.onGoHome)
            MedicalCardScreen(
                state = cardUiState,
                guidance = guidanceUiState,
                actions = cardActions,
                onStartGuidance = onStartGuidance,
                onCancelGuidance = onCancelGuidance,
                onCloseArrival = onCloseArrival,
                isPetProfileSample = isPetProfileSample,
                isGuidanceSimulated = isGuidanceSimulated,
                isRouteOnNavi = isRouteOnNavi,
                delivery = cardExtras.delivery,
                onResend = cardExtras.onResend,
            )
        }
        ParkedContent.HOSPITALS -> {
            BackHandler(onBack = onFinderBack)
            hospitalList()
        }
        ParkedContent.CONFIRM_HOSPITAL -> {
            BackHandler(onBack = onFinderBack)
            (finder as? FinderStep.Confirm)?.let { hospitalConfirm(it.choice) }
        }
    }
}

/**
 * 진료 카드 화면에 012가 더한 것: 카드 전송 줄과 [다시 보내기].
 * @param delivery 출발한 뒤의 전송 상태. 출발 전이면 null (줄이 없다)
 */
data class CardExtras(
    val delivery: com.meongcare.app.domain.trip.CardDelivery? = null,
    val onResend: () -> Unit = {},
)
