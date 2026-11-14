package com.meongcare.app

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.meongcare.app.ui.card.PetHeader
import com.meongcare.app.ui.home.HomePage
import com.meongcare.app.ui.hospital.DemoHospitalFindScreen
import com.meongcare.app.ui.home.TripActions
import com.meongcare.app.ui.trip.tripViewOf
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.meongcare.app.ui.hospital.DepartConfirmScreen
import com.meongcare.app.ui.hospital.VetInquiryScreen
import com.meongcare.app.ui.hospital.VetInquiryViewModel
import com.meongcare.app.domain.inquiry.InquiryScripts
import com.meongcare.app.ui.home.PageExtras
import com.meongcare.app.ui.pet.PetCardScreen
import com.meongcare.app.ui.pet.petCardOf
import com.meongcare.app.ui.relax.RelaxScreen
import com.meongcare.app.ui.relax.RelaxViewModel
import com.meongcare.app.ui.relax.relaxSummaryOf
import com.meongcare.app.ui.relax.relaxSettingsText
import java.time.LocalDate
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.meongcare.app.di.AppRestarter
import com.meongcare.app.di.DataMode
import com.meongcare.app.di.NaviUse
import com.meongcare.app.di.NearbyVetKind
import com.meongcare.app.di.SourceKind
import com.meongcare.app.di.SourcePlan
import com.meongcare.app.di.next
import com.meongcare.app.domain.driving.DrivingState
import com.meongcare.app.ui.card.MedicalCardActions
import com.meongcare.app.ui.card.MedicalCardViewModel
import com.meongcare.app.ui.card.PhoneNumberPanel
import com.meongcare.app.ui.debug.DebugLocationRow
import com.meongcare.app.ui.debug.DebugLocationUiState
import com.meongcare.app.ui.debug.DebugLocationViewModel
import com.meongcare.app.ui.debug.DebugPanel
import com.meongcare.app.ui.debug.debugLocationText
import com.meongcare.app.ui.guidance.GuidanceViewModel
import com.meongcare.app.ui.home.CardExtras
import com.meongcare.app.ui.home.LinkExtras
import com.meongcare.app.ui.home.linkRowOf
import com.meongcare.app.ui.link.LinkScreen
import com.meongcare.app.ui.link.rememberLinkCountdown
import com.meongcare.app.ui.link.LinkViewModel
import com.meongcare.app.ui.home.HomeScreen
import com.meongcare.app.ui.hospital.HospitalConfirmScreen
import com.meongcare.app.ui.hospital.HospitalFinderViewModel
import com.meongcare.app.ui.hospital.HospitalListScreen
import com.meongcare.app.ui.home.HomeUiState
import com.meongcare.app.ui.home.HomeViewModel
import com.meongcare.app.ui.theme.MeongCareTheme
import com.meongcare.app.ui.voice.SymptomVoiceViewModel
import com.meongcare.app.ui.voice.VoiceActions

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as MeongCareApplication).container

        setContent {
            MeongCareTheme {
                val homeViewModel: HomeViewModel =
                    viewModel(factory = HomeViewModel.factory(container.drivingStateSource))
                val uiState by homeViewModel.uiState.collectAsStateWithLifecycle()
                val guidanceViewModel: GuidanceViewModel = viewModel(
                    factory = GuidanceViewModel.factory(container.guidanceSource, container.sampleHospital.destination),
                )
                val guidanceUiState by guidanceViewModel.uiState.collectAsStateWithLifecycle()
                val cardViewModel: MedicalCardViewModel = viewModel(
                    factory = MedicalCardViewModel.factory(
                        petProfileSource = container.petProfileSource,
                        session = container.medicalCardSession,
                        hospital = container.sampleHospital,
                        phoneDialer = container.phoneDialer,
                        selectedHospital = container.hospitalTrip.selectedHospital,
                    ),
                )
                val trip = container.hospitalTrip
                val inquiryViewModel: VetInquiryViewModel = viewModel()
                var inquiryNoResponse by rememberSaveable { mutableStateOf(false) }
                val finder by trip.finder.collectAsStateWithLifecycle()
                val finderWithoutCard by trip.finderWithoutCard.collectAsStateWithLifecycle()
                val tripState by trip.trip.collectAsStateWithLifecycle()
                val cardUiState by cardViewModel.uiState.collectAsStateWithLifecycle()
                val linkViewModel: LinkViewModel = viewModel(
                    factory = LinkViewModel.factory(container.ownerLink, container.petProfileSource.profile),
                )
                val linking by linkViewModel.isOpen.collectAsStateWithLifecycle()
                val linkState by container.ownerLink.state.collectAsStateWithLifecycle()
                val isPetProfileSample by container.petProfileIsSample.collectAsStateWithLifecycle()
                // 처음 화면 바로가기로 연 쪽 (019). 주행으로 바뀌었다가 정차하면 같은 쪽으로 돌아온다
                var homePage by rememberSaveable { mutableStateOf(HomePage.NONE) }
                val relaxViewModel: RelaxViewModel = viewModel()
                val visitViewModel: com.meongcare.app.ui.visit.VisitViewModel = viewModel()
                val visit by visitViewModel.visit.collectAsStateWithLifecycle()
                val waitInCar by visitViewModel.waitInCar.collectAsStateWithLifecycle()
                val called by visitViewModel.called.collectAsStateWithLifecycle()
                val recordChoice by visitViewModel.record.collectAsStateWithLifecycle()
                val visitRecords by visitViewModel.records.collectAsStateWithLifecycle()
                val feeling by visitViewModel.feeling.collectAsStateWithLifecycle()
                // 릴렉스 화면을 진료 완료·귀가 중에서 열면 닫을 때 그 쪽으로 돌아간다
                var relaxReturn by rememberSaveable { mutableStateOf(HomePage.NONE) }
                val relaxSettings by relaxViewModel.settings.collectAsStateWithLifecycle()
                val cardActions = remember(cardViewModel) {
                    MedicalCardActions(
                        // [진료 카드 만들기] 때 연결된 반려견 정보를 다시 읽는다 (014 K4)
                        onStartCard = {
                            container.refreshPetProfile()
                            cardViewModel.onStartCard()
                        },
                        onToggleSymptom = cardViewModel::onToggleSymptom,
                        onConfirmSymptoms = cardViewModel::onConfirmSymptoms,
                        onCancelPicking = cardViewModel::onCancelPicking,
                        onEditSymptoms = cardViewModel::onEditSymptoms,
                        onCloseCard = cardViewModel::onCloseCard,
                        onCallHospital = cardViewModel::onCallHospital,
                        onDismissPhoneNumber = cardViewModel::onDismissPhoneNumber,
                        onGoHome = cardViewModel::onGoHome,
                        onOpenCard = cardViewModel::onOpenCard,
                        // [새로 만들기]도 새 카드를 만드는 시작이라 같이 다시 읽는다
                        onStartNewCard = {
                            container.refreshPetProfile()
                            cardViewModel.onStartNewCard()
                        },
                    )
                }

                val voiceViewModel: SymptomVoiceViewModel = viewModel(
                    factory = SymptomVoiceViewModel.factory(
                        voiceInput = container.voiceInput,
                        session = container.medicalCardSession,
                        isSimulated = container.isVoiceSimulated,
                    ),
                )
                val voiceUiState by voiceViewModel.uiState.collectAsStateWithLifecycle()
                val voiceActions = remember(voiceViewModel) {
                    VoiceActions(
                        onStart = voiceViewModel::onStartListening,
                        onCancel = voiceViewModel::onCancelListening,
                        onStop = voiceViewModel::onStop,
                    )
                }

                androidx.compose.runtime.CompositionLocalProvider(
                    com.meongcare.app.ui.card.LocalSamplePetPhoto provides isPetProfileSample,
                ) {
                HomeScreen(
                    uiState = uiState,
                    guidanceUiState = guidanceUiState,
                    cardUiState = cardUiState,
                    cardActions = cardActions,
                    // 020: 카드 [동물병원 찾기] = 시안 병원 3곳에 새로 문의하고 "동물병원 응답 확인"을 연다 (답변은 앱 안 시연 응답)
                    onFindHospital = {
                        inquiryViewModel.restart()
                        trip.openFinder()
                    },
                    onCancelGuidance = guidanceViewModel::onCancelGuidance,
                    // 도착 상태를 먼저 지우고 카드를 비운다. 중간 프레임이 "생성 완료" 카드라 거의 보이지 않는다.
                    onCloseArrival = {
                        guidanceViewModel.onDismissArrival()
                        cardViewModel.onCloseCard()
                        trip.clear()
                    },
                    isGuidanceSimulated = container.sourcePlan.isGuidanceSimulated,
                    isPetProfileSample = isPetProfileSample,
                    isRouteOnNavi = container.naviUse == NaviUse.ROUTE_ONLY,
                    finder = finder,
                    finderWithoutCard = finderWithoutCard,
                    // 홈 [병원 찾기]: 카드와 상관없이 늘 시연용 병원 목록 (처음 화면에서 카드로 가지 않는다, 사용자 지시 2026-11-13)
                    onFindHospitalFromHome = trip::openFinderWithoutCard,
                    onFinderBack = trip::back,
                    hospitalList = hospitalList@{
                        if (finderWithoutCard) {
                            DemoHospitalFindScreen(onBack = trip::back, onGoDirect = { trip.choose(it.row) })
                            return@hospitalList
                        }
                        // 020: 카드에서 연 병원 찾기 = 동물병원 응답 확인 / 비교. 제휴 병원 목록(012)은 아래에 코드로 남는다
                        if (SHOW_INQUIRY_FLOW) {
                            val snapshot by inquiryViewModel.snapshot.collectAsStateWithLifecycle()
                            val page by inquiryViewModel.page.collectAsStateWithLifecycle()
                            val compareSelected by inquiryViewModel.compareSelected.collectAsStateWithLifecycle()
                            val promptSelected by inquiryViewModel.promptSelected.collectAsStateWithLifecycle()
                            val expandNotice by inquiryViewModel.expandNotice.collectAsStateWithLifecycle()
                            VetInquiryScreen(
                                vm = inquiryViewModel,
                                snapshot = snapshot,
                                page = page,
                                compareSelected = compareSelected,
                                promptSelected = promptSelected,
                                expandNotice = expandNotice,
                                onBack = trip::back,
                                onChoose = { trip.choose(it.row) },
                                // 응답 없음 → 가장 가까운 병원으로 카드와 함께 바로 출발 (전화는 걸지 않는다, 사용자 결정 2026-11-14)
                                onDepartNearest = { nearest -> trip.depart(nearest.row, cardViewModel.caseCard()) },
                            )
                            return@hospitalList
                        }
                        val finderViewModel: HospitalFinderViewModel = viewModel(
                            factory = HospitalFinderViewModel.factory(
                                container.hospitalBoardSource,
                                container.currentLocationSource,
                                container.nearbyVetSource,
                            ),
                        )
                        val finderState by finderViewModel.uiState.collectAsStateWithLifecycle()
                        HospitalListScreen(
                            state = finderState,
                            isLocationSample = container.isLocationSample,
                            sampleLocationName = container.sampleLocationName,
                            isNearbySample = container.nearbyVetKind == NearbyVetKind.FAKE,
                            onChoose = trip::choose,
                            onBack = trip::back,
                            onRefresh = finderViewModel::onRefresh,
                            onRelocate = finderViewModel::onRelocate,
                        )
                    },
                    hospitalConfirm = hospitalConfirm@{ choice ->
                        val demo = inquiryViewModel.hospitalNamed(choice.hospital.name)
                        if (SHOW_INQUIRY_FLOW && !finderWithoutCard && demo != null) {
                            DepartConfirmScreen(
                                hospital = demo,
                                startMinutes = inquiryViewModel.current().searchRows.firstOrNull { it.key == demo.name }?.startMinutes,
                                petName = (cardUiState.card.pet as? PetHeader.Registered)?.name,
                                now = inquiryViewModel.now(),
                                onBack = {
                                    inquiryViewModel.onBackFromConfirm()
                                    trip.back()
                                },
                                onDepart = { trip.depart(choice, cardViewModel.caseCard()) },
                                onCall = cardViewModel::onCallNumber,
                            )
                            return@hospitalConfirm
                        }
                        HospitalConfirmScreen(
                            choice = choice,
                            onBack = trip::back,
                            onCall = cardViewModel::onCallNumber,
                            onDepartWithoutCard = { trip.depart(choice, card = null) },
                            onDepart = { trip.depart(choice, cardViewModel.caseCard()) },
                            withCard = !finderWithoutCard,
                            phoneNumberPanel = {
                                cardUiState.shownPhoneNumber?.let {
                                    PhoneNumberPanel(number = it, isSample = choice.hospital.isSample, onDismiss = cardViewModel::onDismissPhoneNumber)
                                }
                            },
                        )
                    },
                    cardExtras = CardExtras(trip = tripState, onResend = trip::resend),
                    link = LinkExtras(
                        linking = linking,
                        row = linkRowOf(linkState, guidanceUiState),
                        onOpen = {
                            container.ownerLink.acknowledgeRevoked()
                            linkViewModel.open()
                        },
                        onUnlink = linkViewModel::unlink,
                        onClose = linkViewModel::close,
                        onRefreshPet = container::refreshPetProfile,
                    ),
                    linkScreen = {
                        val linkUiState by linkViewModel.uiState.collectAsStateWithLifecycle()
                        val waitingSince by linkViewModel.waitingSince.collectAsStateWithLifecycle()
                        LinkScreen(
                            state = linkUiState,
                            onNewCode = linkViewModel::onNewCode,
                            onClose = linkViewModel::close,
                            countdown = rememberLinkCountdown(waitingSince, linkViewModel::now),
                        )
                    },
                    // 병원으로 이동 중 (디자인 시안, 2026-11-14)
                    tripViewFor = { tripPanel ->
                        val inquiry = inquiryViewModel.current()
                        val reply = inquiry.searchRows.firstOrNull { it.key == tripPanel.destinationName }
                        val waitText = reply?.startMinutes?.let { stringResource(R.string.trip_wait_value, it) }
                        // 도착 예정은 출발할 때 한 번 정해 두고 바꾸지 않는다 (사용자 요청: 숫자가 줄어들지 않게)
                        val fixedEta = androidx.compose.runtime.remember(tripPanel.destinationName) { tripPanel.arrivesAt }
                        tripViewOf(
                            panel = tripPanel,
                            trip = tripState,
                            reply = reply,
                            petName = (cardUiState.card.pet as? PetHeader.Registered)?.name,
                            waitText = waitText,
                            relaxOn = relaxSettings.enabled,
                            relaxText = "",
                            arrivesAt = fixedEta,
                        )
                    },
                    tripActions = TripActions(
                        onCall = { tripState?.candidate?.hospital?.phoneNumber?.let(cardViewModel::onCallNumber) },
                        onArrive = {
                            lifecycleScope.launch { container.guidanceSource.arriveNow(container.guidanceSource.state.first()) }
                        },
                    ),
                    pages = PageExtras(
                        page = homePage,
                        onOpen = { homePage = it },
                        onClose = {
                            homePage = if (homePage == HomePage.RELAX) relaxReturn else HomePage.NONE
                            relaxReturn = HomePage.NONE
                        },
                    ),
                    // 병원 도착 → 진료 완료 → 귀가 중 (디자인 시안, 2026-11-14)
                    arriveScreen = {
                        val arrivedName = (guidanceUiState as? com.meongcare.app.ui.guidance.GuidanceUiState.Arrived)?.destinationName.orEmpty()
                        val reply = inquiryViewModel.current().searchRows.firstOrNull { it.key == arrivedName }
                        val travel = tripState?.candidate?.hospital?.destination?.expectedTravel?.inWholeMinutes?.toInt()
                        com.meongcare.app.ui.visit.ArriveScreen(
                            card = cardUiState.card,
                            hospitalName = arrivedName,
                            cardSent = tripState?.delivery is com.meongcare.app.domain.trip.CardDelivery.Sent,
                            startText = reply?.startMinutes?.let { stringResource(R.string.trip_wait_value, it) },
                            waitInCar = waitInCar,
                            called = called,
                            onToggleWaitInCar = visitViewModel::toggleWaitInCar,
                            onHome = {
                                visitViewModel.clear()
                                guidanceViewModel.onDismissArrival()
                                cardViewModel.onCloseCard()
                                trip.clear()
                            },
                            onComplete = {
                                visitViewModel.complete(
                                    arrivedName,
                                    travel,
                                    cardUiState.card.symptoms.takeIf { it.isNotEmpty() }?.joinToString(", ") { s -> getString(com.meongcare.app.ui.card.symptomLabelRes(s)) },
                                )
                                // 시안: 진료가 끝나면 귀가용 릴렉스를 켜 둔다
                                relaxViewModel.setEnabled(true)
                                guidanceViewModel.onDismissArrival()
                                cardViewModel.onCloseCard()
                                trip.clear()
                                homePage = HomePage.AFTER_VISIT
                            },
                        )
                    },
                    afterVisitScreen = {
                        val v = visit
                        if (v == null) {
                            homePage = HomePage.NONE
                        } else {
                            com.meongcare.app.ui.visit.AfterVisitScreen(
                                visit = v,
                                relaxOn = relaxSettings.enabled,
                                relaxSummary = if (relaxSettings.enabled) relaxSettingsText(relaxSettings) else stringResource(R.string.relax_summary_off),
                                onToggleRelax = { relaxViewModel.setEnabled(!relaxSettings.enabled) },
                                onRelaxSettings = {
                                    relaxReturn = HomePage.AFTER_VISIT
                                    homePage = HomePage.RELAX
                                },
                                onGoHome = { homePage = HomePage.GOING_HOME },
                                record = recordChoice,
                                linked = linkState is com.meongcare.app.domain.link.OwnerLinkState.Linked,
                                onSave = visitViewModel::saveRecord,
                                onSkip = visitViewModel::skipRecord,
                            )
                        }
                    },
                    goingHomeScreen = {
                        val v = visit
                        if (v == null) {
                            homePage = HomePage.NONE
                        } else {
                            com.meongcare.app.ui.visit.GoingHomeScreen(
                                visit = v,
                                relaxOn = relaxSettings.enabled,
                                relaxSummary = relaxSettingsText(relaxSettings),
                                onRelax = {
                                    relaxReturn = HomePage.GOING_HOME
                                    homePage = HomePage.RELAX
                                },
                                onDone = { homePage = HomePage.HOME_ARRIVED },
                            )
                        }
                    },
                    homeArrivedScreen = {
                        val v = visit
                        if (v == null) {
                            homePage = HomePage.NONE
                        } else {
                            com.meongcare.app.ui.visit.HomeArrivedScreen(
                                visit = v,
                                petName = (cardUiState.card.pet as? PetHeader.Registered)?.name,
                                record = recordChoice,
                                relaxOn = relaxSettings.enabled,
                                feeling = feeling,
                                onFeeling = { f ->
                                    visitViewModel.recordFeeling(f)
                                    // 시안: 불안함이면 다음 이동은 불안 완화 설정(조용히 + 추가 안심 안내)
                                    if (f == com.meongcare.app.ui.visit.TripFeeling.ANXIOUS) {
                                        relaxViewModel.setSound(com.meongcare.app.ui.relax.RelaxSound.QUIET)
                                        relaxViewModel.setExtraReassurance(true)
                                    }
                                },
                                onDone = {
                                    visitViewModel.clear()
                                    relaxViewModel.setEnabled(false)
                                    homePage = HomePage.NONE
                                },
                            )
                        }
                    },
                    petCardScreen = {
                        // 열려 있을 때만 프로필을 구독한다
                        val profile by container.petProfileSource.profile.collectAsStateWithLifecycle(initialValue = null)
                        PetCardScreen(
                            state = petCardOf(profile, LocalDate.now()),
                            relaxSummary = relaxSummaryOf(relaxSettings),
                            isSample = isPetProfileSample,
                            onBack = { homePage = HomePage.NONE },
                            sampleWeight = com.meongcare.app.data.pet.SamplePetProfiles.DEMO_WEIGHT,
                            sampleTraits = com.meongcare.app.data.pet.SamplePetProfiles.DEMO_TRAITS,
                            records = visitRecords.map { r ->
                                listOfNotNull(
                                    r.endedAt.atZone(java.time.ZoneId.systemDefault())
                                        .format(java.time.format.DateTimeFormatter.ofPattern("M월 d일 a h:mm", java.util.Locale.KOREAN)),
                                    r.hospitalName,
                                    stringResource(R.string.card_emergency_mode),
                                    r.symptoms,
                                ).joinToString(" · ")
                            },
                        )
                    },
                    relaxScreen = {
                        RelaxScreen(
                            settings = relaxSettings,
                            petName = (cardUiState.card.pet as? PetHeader.Registered)?.name,
                            onSetEnabled = relaxViewModel::setEnabled,
                            onSetSound = relaxViewModel::setSound,
                            onSetExtraReassurance = relaxViewModel::setExtraReassurance,
                            onDone = {
                                homePage = relaxReturn
                                relaxReturn = HomePage.NONE
                            },
                        )
                    },
                    voice = voiceUiState,
                    voiceActions = voiceActions,
                    bottomBar = {
                        if (BuildConfig.DEBUG) {
                            val fake = container.fakeDrivingStateSource
                            val locationViewModel: DebugLocationViewModel =
                                viewModel(factory = DebugLocationViewModel.factory(container.currentLocationSource))
                            val locationState by locationViewModel.uiState.collectAsStateWithLifecycle()
                            DebugPanel(
                                modeLabel = debugModeLabel(container.dataMode, container.sourcePlan) +
                                    if (container.naviUse != NaviUse.NONE) " · 내비 ${if (container.naviUse == NaviUse.FULL) "전부" else "경로"}" else "",
                                nextModeLabel = "${container.dataMode.next().debugName()}${
                                    if (container.dataMode.next() == DataMode.DEMO) "으로" else "로"
                                } 전환",
                                showsFakeDrivingControls = container.sourcePlan.driving == SourceKind.FAKE,
                                onSetParked = { fake.set(DrivingState.PARKED) },
                                onSetDriving = { fake.set(DrivingState.DRIVING) },
                                onSetUnknown = { fake.set(DrivingState.UNKNOWN) },
                                onToggleDataMode = {
                                    AppRestarter.restartWith(this@MainActivity, container.dataMode.next())
                                },
                                // 주행 중(모름 포함)에는 위치 줄을 숨긴다. 주행 화면 정보를 늘리지 않는다 (AGENTS.md 규칙 3).
                                location = if (uiState == HomeUiState.Parked) {
                                    DebugLocationRow(
                                        text = debugLocationText(
                                            locationState,
                                            sourceName = if (container.locationKind == SourceKind.REAL) "내비" else "Fake",
                                        ),
                                        isAsking = locationState == DebugLocationUiState.Asking,
                                    )
                                } else {
                                    null
                                },
                                onAskLocation = locationViewModel::onAsk,
                                inquiryScenarioLabel = if (inquiryNoResponse) "문의 시연: 응답 없음" else "문의 시연: 보통",
                                onToggleInquiryScenario = {
                                    inquiryNoResponse = !inquiryNoResponse
                                    inquiryViewModel.script = if (inquiryNoResponse) InquiryScripts.NO_RESPONSE else InquiryScripts.DEFAULT
                                },
                                onFakeOwnerClaim = container.fakeOwnerLink?.let { fakeLink ->
                                    val pending by fakeLink.pendingCode.collectAsStateWithLifecycle()
                                    val claim: () -> Unit = { fakeLink.claim() }
                                    if (linking && pending != null && uiState == HomeUiState.Parked) claim else null
                                },
                            )
                        }
                    },
                )
                }
            }
        }
    }
}

/** debug 패널에 보이는 모드 이름. */
private fun DataMode.debugName(): String = when (this) {
    DataMode.FAKE -> "Fake"
    DataMode.DEMO -> "시연"
    DataMode.REAL -> "Real"
}

/** 예: "DEBUG · 시연 (주행 Real)". 스크린샷만 보고도 어느 구성인지 알 수 있게 시연 모드는 주행 상태 출처를 붙인다. */
private fun debugModeLabel(mode: DataMode, plan: SourcePlan): String {
    val drivingSource = if (plan.driving == SourceKind.REAL) "Real" else "Fake"
    return if (mode == DataMode.DEMO) "DEBUG · 시연 (주행 $drivingSource)" else "DEBUG · ${mode.debugName()}"
}

/** 카드 [동물병원 찾기]를 동물병원 문의 흐름(020)으로 연다. false면 예전 제휴 병원 목록(012) */
private const val SHOW_INQUIRY_FLOW = true
