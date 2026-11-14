package com.meongcare.app.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.meongcare.app.domain.hospital.RankedHospital
import com.meongcare.app.domain.trip.FinderStep
import com.meongcare.app.domain.trip.TripState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.compose.LifecycleEventEffect
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
import com.meongcare.app.ui.voice.VoiceActions
import com.meongcare.app.ui.voice.VoiceScreenState

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
 * @param finderWithoutCard 홈 [병원 찾기]로 카드 없이 연 병원 찾기인지 (019)
 * @param onFindHospitalFromHome 홈 바로가기 [병원 찾기]
 * @param onFinderBack 병원 찾기 화면의 시스템 뒤로 (= 화면의 [뒤로])
 * @param voice 증상 고르기 화면의 음성 영역 (010). `null`이면 음성 영역이 없다.
 * @param voiceActions 음성 영역 동작. 증상 고르기 화면이 사라지거나 앱이 뒤로 가면 `onStop`을 부른다.
 * @param bottomBar 화면 아래에 붙는 영역. debug 빌드의 개발자 패널이 들어간다.
 * @param cardExtras 고른 병원과 카드 전송 상태 (012). 안내 영역([tripPanelOf], 013)을 여기서 만든다.
 * @param link 보호자 웹 연결 (014): 처음 화면의 연결 줄과 연결 화면. 연결 화면은 [linkScreen]이 그린다.
 * @param pages 처음 화면 바로가기로 여는 쪽 (019). 반려동물 카드는 [petCardScreen], 릴렉스는 [relaxScreen]이 그린다.
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
    finderWithoutCard: Boolean = false,
    onFindHospitalFromHome: () -> Unit = {},
    onFinderBack: () -> Unit = {},
    hospitalList: @Composable () -> Unit = {},
    hospitalConfirm: @Composable (RankedHospital) -> Unit = {},
    cardExtras: CardExtras = CardExtras(),
    link: LinkExtras = LinkExtras(),
    linkScreen: @Composable () -> Unit = {},
    pages: PageExtras = PageExtras(),
    tripViewFor: (@Composable (TripPanel) -> com.meongcare.app.ui.trip.TripView)? = null,
    tripActions: TripActions = TripActions(),
    petCardScreen: @Composable () -> Unit = {},
    relaxScreen: @Composable () -> Unit = {},
    arriveScreen: (@Composable () -> Unit)? = null,
    afterVisitScreen: @Composable () -> Unit = {},
    goingHomeScreen: @Composable () -> Unit = {},
    homeArrivedScreen: @Composable () -> Unit = {},
    voice: VoiceScreenState? = null,
    voiceActions: VoiceActions = VoiceActions(),
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
    val tripView = panel?.let { p -> tripViewFor?.invoke(p) }
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
                        finderWithoutCard = finderWithoutCard,
                        onFindHospitalFromHome = onFindHospitalFromHome,
                        onFinderBack = onFinderBack,
                        hospitalList = hospitalList,
                        hospitalConfirm = hospitalConfirm,
                        panel = panel,
                        onResend = cardExtras.onResend,
                        link = link,
                        linkScreen = linkScreen,
                        pages = pages,
                        tripView = tripView,
                        tripActions = tripActions,
                        petCardScreen = petCardScreen,
                        relaxScreen = relaxScreen,
                        arriveScreen = arriveScreen,
                        afterVisitScreen = afterVisitScreen,
                        goingHomeScreen = goingHomeScreen,
                        homeArrivedScreen = homeArrivedScreen,
                        voice = voice,
                        voiceActions = voiceActions,
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
    finderWithoutCard: Boolean,
    onFindHospitalFromHome: () -> Unit,
    onFinderBack: () -> Unit,
    hospitalList: @Composable () -> Unit,
    hospitalConfirm: @Composable (RankedHospital) -> Unit,
    panel: TripPanel?,
    onResend: () -> Unit,
    link: LinkExtras,
    linkScreen: @Composable () -> Unit,
    pages: PageExtras,
    tripView: com.meongcare.app.ui.trip.TripView?,
    tripActions: TripActions,
    petCardScreen: @Composable () -> Unit,
    relaxScreen: @Composable () -> Unit,
    arriveScreen: (@Composable () -> Unit)?,
    afterVisitScreen: @Composable () -> Unit,
    goingHomeScreen: @Composable () -> Unit,
    homeArrivedScreen: @Composable () -> Unit,
    voice: VoiceScreenState?,
    voiceActions: VoiceActions,
) {
    when (parkedContentOf(guidanceUiState, cardUiState.session, cardUiState.atHome, finder, link.linking, pages.page, finderWithoutCard)) {
        ParkedContent.HOME -> {
            // 처음 화면이 보이는 동안(앱이 앞에 있을 때만) 연결된 반려견 정보를 1분마다 다시 읽는다 (014 K4)
            val lifecycleOwner = LocalLifecycleOwner.current
            LaunchedEffect(lifecycleOwner, link.onRefreshPet) {
                lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    while (true) {
                        link.onRefreshPet()
                        delay(PET_REFRESH_MILLIS)
                    }
                }
            }
            ParkedScreen(
                card = cardUiState,
                guidance = guidanceUiState,
                actions = cardActions,
                isPetProfileSample = isPetProfileSample,
                panel = panel,
                onResend = onResend,
                linkRow = link.row,
                onOpenLink = link.onOpen,
                onUnlink = link.onUnlink,
                onFindHospital = onFindHospitalFromHome,
                onOpenRelax = { pages.onOpen(HomePage.RELAX) },
                onOpenPetCard = { pages.onOpen(HomePage.PET_CARD) },
            )
        }
        ParkedContent.PET_CARD -> {
            BackHandler(onBack = pages.onClose)
            petCardScreen()
        }
        ParkedContent.RELAX -> {
            BackHandler(onBack = pages.onClose)
            relaxScreen()
        }
        ParkedContent.AFTER_VISIT -> afterVisitScreen()
        ParkedContent.GOING_HOME -> goingHomeScreen()
        ParkedContent.HOME_ARRIVED -> homeArrivedScreen()
        ParkedContent.LINK -> {
            BackHandler(onBack = link.onClose)
            linkScreen()
        }
        ParkedContent.SYMPTOMS -> {
            // [←]·시스템 뒤로 = 처음 화면 (고르던 것은 취소하고, 만든 카드는 남긴다). 카드로 되돌아가지 않는다 (사용자 지시 2026-11-13)
            BackHandler(onBack = cardActions.onGoHome)
            // 이 화면이 사라지면(주행으로 바뀜, 다른 화면으로 감) 또는 앱이 뒤로 가면 듣기를 멈추고 들은 것을 버린다 (010 "설계 결정 2").
            val stopVoice by rememberUpdatedState(voiceActions.onStop)
            DisposableEffect(Unit) { onDispose { stopVoice() } }
            LifecycleEventEffect(Lifecycle.Event.ON_STOP) { stopVoice() }
            SymptomPickerScreen(
                selected = cardUiState.pickingDraft,
                onToggle = cardActions.onToggleSymptom,
                onCancel = cardActions.onGoHome,
                onConfirm = cardActions.onConfirmSymptoms,
                voice = voice,
                voiceActions = voiceActions,
                petName = (cardUiState.card.pet as? com.meongcare.app.ui.card.PetHeader.Registered)?.name,
            )
        }
        ParkedContent.CARD -> {
            // 카드에서 시스템 뒤로 = [홈] (카드는 남는다)
            BackHandler(onBack = cardActions.onGoHome)
            // 도착하면 진료 카드 대신 디자인 시안 "병원 도착" (2026-11-14)
            if (guidanceUiState is GuidanceUiState.Arrived && arriveScreen != null) {
                arriveScreen()
                return
            }
            // 안내 중이면 진료 카드 대신 디자인 시안 "병원으로 이동 중" (2026-11-14)
            if (guidanceUiState is GuidanceUiState.Guiding && tripView != null) {
                com.meongcare.app.ui.trip.ParkedTripScreen(
                    view = tripView,
                    onBack = cardActions.onGoHome,
                    onCall = tripActions.onCall,
                    onRelax = { pages.onOpen(HomePage.RELAX) },
                    onEnd = onCancelGuidance,
                    onArrive = tripActions.onArrive,
                )
                return
            }
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

/**
 * 보호자 웹 연결 (014).
 * @param linking 연결 화면이 열려 있는지
 * @param row 처음 화면의 연결 줄 ([linkRowOf])
 * @param onRefreshPet 연결된 반려견 정보를 다시 읽는다 (처음 화면이 보이는 동안 1분마다)
 */
data class LinkExtras(
    val linking: Boolean = false,
    val row: LinkRow = LinkRow.Hidden,
    val onOpen: () -> Unit = {},
    val onUnlink: () -> Unit = {},
    val onClose: () -> Unit = {},
    val onRefreshPet: () -> Unit = {},
)

/**
 * 처음 화면 바로가기로 여는 쪽 (019).
 * @param page 지금 열린 쪽. 주행으로 바뀌었다가 정차하면 같은 쪽으로 돌아온다
 * @param onClose 쪽을 닫고 처음 화면으로 (화면의 [←]·[완료], 시스템 뒤로)
 */
data class PageExtras(
    val page: HomePage = HomePage.NONE,
    val onOpen: (HomePage) -> Unit = {},
    val onClose: () -> Unit = {},
)

/** "병원으로 이동 중" 버튼 (2026-11-14). [onArrive]는 [병원 도착] 직접 처리 */
data class TripActions(
    val onCall: () -> Unit = {},
    val onArrive: () -> Unit = {},
)

private const val PANEL_REFRESH_MILLIS = 30_000L
private const val PET_REFRESH_MILLIS = 60_000L
