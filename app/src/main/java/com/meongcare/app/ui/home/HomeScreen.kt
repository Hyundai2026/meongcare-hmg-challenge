package com.meongcare.app.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.meongcare.app.domain.hospital.RankedHospital
import com.meongcare.app.domain.trip.FinderStep
import com.meongcare.app.domain.trip.TripState
import com.meongcare.app.ui.card.CardFlowUiState
import com.meongcare.app.ui.card.MedicalCardActions
import com.meongcare.app.ui.card.MedicalCardScreen
import com.meongcare.app.ui.card.SymptomPickerScreen
import com.meongcare.app.ui.guidance.GuidanceUiState
import com.meongcare.app.ui.guidance.TripPanel
import com.meongcare.app.ui.guidance.tripPanelOf
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.delay

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
 * @param cardExtras 고른 병원과 카드 전송 상태 (012). 안내 영역([tripPanelOf], 013)을 여기서 만든다.
 */
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    guidanceUiState: GuidanceUiState,
    cardUiState: CardFlowUiState,
    cardActions: MedicalCardActions,
    onFindHospital: () -> Unit,
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
    // 안내 영역의 도착 예정 시각이 정차 중에 낡지 않게 30초마다 다시 계산한다 (주행 화면은 시각을 보이지 않는다)
    val tick by produceState(0L) {
        while (true) {
            delay(PANEL_REFRESH_MILLIS)
            value = value + 1
        }
    }
    val panel = remember(guidanceUiState, cardExtras.trip, tick) {
        tripPanelOf(guidanceUiState, cardExtras.trip, Instant.now(), ZoneId.systemDefault())
    }
    Surface(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (homeContentOf(uiState, guidanceUiState)) {
                    HomeContent.PARKED, HomeContent.ARRIVAL -> ParkedFlow(
                        guidanceUiState = guidanceUiState,
                        cardUiState = cardUiState,
                        cardActions = cardActions,
                        onFindHospital = onFindHospital,
                        onCancelGuidance = onCancelGuidance,
                        onCloseArrival = onCloseArrival,
                        isGuidanceSimulated = isGuidanceSimulated,
                        isPetProfileSample = isPetProfileSample,
                        isRouteOnNavi = isRouteOnNavi,
                        finder = finder,
                        onFinderBack = onFinderBack,
                        hospitalList = hospitalList,
                        hospitalConfirm = hospitalConfirm,
                        panel = panel,
                        onResend = cardExtras.onResend,
                    )
                    HomeContent.DRIVING -> DrivingScreen(guidanceUiState, panel = panel)
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
    onFindHospital: () -> Unit,
    onCancelGuidance: () -> Unit,
    onCloseArrival: () -> Unit,
    isGuidanceSimulated: Boolean,
    isPetProfileSample: Boolean,
    isRouteOnNavi: Boolean,
    finder: FinderStep,
    onFinderBack: () -> Unit,
    hospitalList: @Composable () -> Unit,
    hospitalConfirm: @Composable (RankedHospital) -> Unit,
    panel: TripPanel?,
    onResend: () -> Unit,
) {
    when (parkedContentOf(guidanceUiState, cardUiState.session, cardUiState.atHome, finder)) {
        ParkedContent.HOME -> ParkedScreen(
            card = cardUiState,
            guidance = guidanceUiState,
            actions = cardActions,
            isPetProfileSample = isPetProfileSample,
            panel = panel,
            onResend = onResend,
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
                onFindHospital = onFindHospital,
                onCancelGuidance = onCancelGuidance,
                onCloseArrival = onCloseArrival,
                isPetProfileSample = isPetProfileSample,
                isGuidanceSimulated = isGuidanceSimulated,
                isRouteOnNavi = isRouteOnNavi,
                panel = panel,
                onResend = onResend,
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
 * 고른 병원과 카드 전송 (012·013): 안내 영역의 전송·확인 줄과 진행 막대, [다시 보내기].
 * @param trip 출발한 뒤의 상태. 출발 전이면 null (전송 줄과 막대가 없다)
 */
data class CardExtras(
    val trip: TripState? = null,
    val onResend: () -> Unit = {},
)

private const val PANEL_REFRESH_MILLIS = 30_000L
