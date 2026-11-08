package com.meongcare.app.di

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.meongcare.app.data.demo.FakeTripScenario
import com.meongcare.app.data.driving.FakeDrivingStateSource
import com.meongcare.app.data.driving.RealDrivingStateSource
import com.meongcare.app.data.driving.uxRestrictionsClientOrNull
import com.meongcare.app.data.guidance.FakeGuidanceSource
import com.meongcare.app.data.guidance.FakeGuidanceTiming
import com.meongcare.app.data.guidance.NaviGuidanceSource
import com.meongcare.app.data.guidance.NaviRouteOnlyGuidanceSource
import com.meongcare.app.data.guidance.navi.PleosNaviClient
import com.meongcare.app.data.guidance.RealGuidanceSource
import com.meongcare.app.data.hospital.BuiltInHospitalBoardSource
import com.meongcare.app.data.hospital.FakeCaseSender
import com.meongcare.app.data.hospital.FallbackHospitalBoardSource
import com.meongcare.app.data.hospital.FakeNearbyVetSource
import com.meongcare.app.data.hospital.SampleHospitals
import com.meongcare.app.data.hospital.TimeoutHospitalBoardSource
import com.meongcare.app.data.location.FakeCurrentLocationSource
import com.meongcare.app.data.location.NaviCurrentLocationSource
import com.meongcare.app.data.link.FakeOwnerLink
import com.meongcare.app.data.link.NoOwnerLink
import com.meongcare.app.data.pet.FakePetProfileSource
import com.meongcare.app.data.pet.LinkedPetProfileSource
import com.meongcare.app.data.pet.LinkedProfileRead
import com.meongcare.app.data.pet.LinkedProfileReader
import com.meongcare.app.data.pet.SamplePetProfiles
import com.meongcare.app.domain.link.OwnerLinkService
import com.meongcare.app.data.server.AnonymousAuth
import com.meongcare.app.data.server.FirestoreCaseSender
import com.meongcare.app.data.server.FirestoreHospitalBoardSource
import com.meongcare.app.data.server.FirestoreOwnerLink
import com.meongcare.app.data.server.FirestorePetProfileReader
import com.meongcare.app.data.server.HttpTransport
import com.meongcare.app.data.server.ServerConfig
import com.meongcare.app.data.server.UrlConnectionHttpTransport
import com.meongcare.app.data.server.serverConfigOf
import com.meongcare.app.data.server.FunctionNearbyVetSource
import com.meongcare.app.data.server.nearbyFunctionUrlOf
import com.meongcare.app.domain.hospital.HospitalBoard
import com.meongcare.app.data.phone.AndroidPhoneDialer
import com.meongcare.app.domain.card.MedicalCardSession
import com.meongcare.app.domain.pet.PetProfileSource
import com.meongcare.app.domain.phone.PhoneDialer
import com.meongcare.app.data.vehicle.VehicleConnection
import com.meongcare.app.domain.driving.DrivingState
import com.meongcare.app.domain.driving.DrivingStateSource
import com.meongcare.app.domain.guidance.GuidanceSource
import com.meongcare.app.domain.hospital.Hospital
import com.meongcare.app.domain.hospital.HospitalBoardSource
import com.meongcare.app.domain.hospital.HospitalCandidate
import com.meongcare.app.domain.hospital.NearbyVetSource
import com.meongcare.app.domain.trip.CaseSender
import com.meongcare.app.domain.trip.HospitalTrip
import com.meongcare.app.domain.location.CurrentLocationSource
import com.meongcare.app.BuildConfig
import com.meongcare.app.ui.card.symptomLabelRes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.security.SecureRandom
import kotlin.random.asKotlinRandom
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

/**
 * 앱 전체에서 쓰는 객체를 만들고 들고 있는 곳 (수동 DI).
 *
 * 데이터 모드(Fake / 시연 / Real)에 따라 구현을 골라 끼운다. 무엇을 끼울지는 [sourcePlanOf]의 표가 정한다.
 * 화면 쪽은 인터페이스([DrivingStateSource], [GuidanceSource])만 보므로, 어느 쪽이 끼워졌는지 모른다.
 */
class AppContainer(context: Context, val dataMode: DataMode) {

    private val isAutomotive = context.packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)

    /** 이 프로세스에서 끼울 구현의 구성. */
    val sourcePlan: SourcePlan = sourcePlanOf(mode = dataMode, isAutomotive = isAutomotive)

    /** 차량 내비(NaviHelper)를 길안내에 쓰는 범위 (010). [sourcePlan]의 길안내 칸보다 앞선다. */
    val naviUse: NaviUse = naviUseOf(mode = dataMode, isAutomotive = isAutomotive)

    /** 프로세스와 수명이 같은 코루틴 범위. Fake 길안내의 시간 흐름과 시연 시나리오가 여기서 돈다. */
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** debug 패널이 상태를 바꿀 때 쓴다. 주행 상태가 Fake일 때만 화면에 연결된다. */
    val fakeDrivingStateSource = FakeDrivingStateSource()

    val drivingStateSource: DrivingStateSource = when (sourcePlan.driving) {
        SourceKind.FAKE -> fakeDrivingStateSource
        // 차량 기기면 주행 규제(CarUxRestrictions)를 받고, 일반 폰이면 UNKNOWN만 낸다.
        SourceKind.REAL -> RealDrivingStateSource(uxRestrictionsClientOrNull(context))
    }

    val guidanceSource: GuidanceSource = when (naviUse) {
        // Real + 차량 기기: 전부 차량 내비
        NaviUse.FULL -> NaviGuidanceSource(applicationScope, PleosNaviClient(context))
        // 시연 + 차량 기기: 경로만 차량 내비, 남은 시간(내비 예상 시간에서 시작)과 도착은 시연용
        NaviUse.ROUTE_ONLY -> NaviRouteOnlyGuidanceSource(
            scope = applicationScope,
            navi = PleosNaviClient(context),
            fakeOf = { initial ->
                FakeGuidanceSource(
                    scope = applicationScope,
                    timing = FakeGuidanceTiming().let { if (initial != null) it.copy(initialRemaining = initial) else it },
                    progressGate = if (sourcePlan.guidanceWaitsForDriving) isDrivingFlow() else null,
                    // 내비 예상 시간이 있으면 그것, 없으면 병원 목록의 이동 어림 (012)
                    initialRemainingFor = { if (initial != null) null else it.expectedTravel },
                )
            },
        )
        NaviUse.NONE -> when (sourcePlan.guidance) {
            SourceKind.FAKE -> FakeGuidanceSource(
                scope = applicationScope,
                progressGate = if (sourcePlan.guidanceWaitsForDriving) isDrivingFlow() else null,
                // 처음 남은 시간은 병원 목록의 이동 어림 (012). 없으면 기본 12분
                initialRemainingFor = { it.expectedTravel },
            )
            SourceKind.REAL -> RealGuidanceSource()
        }
    }

    /** 현재 위치를 어디서 받는지 (011, [currentLocationKindOf]). */
    val locationKind: SourceKind = currentLocationKindOf(mode = dataMode, isAutomotive = isAutomotive)

    /**
     * 현재 위치 (011). 차량 기기의 시연·Real은 내비, 그 밖은 Fake(Pleos 에뮬레이터와 같은 시연 위치)다.
     * 지금은 debug 패널만 쓴다. 내비 구현도 물을 때만 NaviHelper를 만든다.
     * 로그에는 결과 종류와 걸린 시간만 남는다 (좌표·주소 없음).
     */
    val currentLocationSource: CurrentLocationSource = when (locationKind) {
        SourceKind.REAL -> NaviCurrentLocationSource(
            client = PleosNaviClient(context),
            log = { Log.i("CurrentLocation", it) },
        )
        SourceKind.FAKE -> FakeCurrentLocationSource()
    }

    /**
     * 010의 고정 시연 병원. 012부터 카드의 병원은 병원 찾기에서 고른 곳이고([hospitalTrip]),
     * 이 값은 고르기 전 길안내 화면의 기본 이름으로만 쓰인다.
     */
    val sampleHospital: Hospital = SampleHospitals.demo

    /**
     * 서버(Firebase: Firestore + 익명 Auth, 012). `secrets.properties`에 설정이 없으면 null → 내장 시연 병원으로 돈다.
     * release는 PC 에뮬레이터 주소를 쓰지 않는다 (`serverConfigOf`).
     */
    private val serverConfig = serverConfigOf(
        projectId = BuildConfig.FIREBASE_PROJECT_ID,
        apiKey = BuildConfig.FIREBASE_API_KEY,
        emulatorHost = BuildConfig.FIREBASE_EMULATOR_HOST,
        isDebugBuild = BuildConfig.DEBUG,
    )

    /** 병원 목록 출처 (012, [hospitalSourceKindOf]의 표) */
    val hospitalSourceKind: HospitalSourceKind = hospitalSourceKindOf(dataMode, hasServer = serverConfig != null)

    private val serverTransport: HttpTransport = UrlConnectionHttpTransport()

    /**
     * 서버 클라이언트 (설정·익명 계정). 서버 설정이 있고 Fake가 아닐 때 한 번 만들어 병원과 반려견(014)이 같이 쓴다.
     * 같은 익명 계정이어야 한다: 보안 규칙상 차는 자기 uid로 만든 환자 건·연결 코드만 다룬다.
     */
    private val serverClient: Pair<ServerConfig, AnonymousAuth>? =
        serverConfig?.takeIf { dataMode != DataMode.FAKE }?.let { config ->
            // 토큰은 만료 5분 전부터 미리 갱신한다 (013). 로그에는 결과 종류만
            config to AnonymousAuth(config, serverTransport, log = { Log.i(TRIP_LOG_TAG, it) })
        }

    /** 서버를 쓰는 구성에서만 만든다. 로그 태그는 `HospitalTrip` 하나 (결과 종류·개수·시간만). */
    private val server = serverClient?.takeIf { hospitalSourceKind != HospitalSourceKind.BUILT_IN }?.let { (config, auth) ->
        val label: (com.meongcare.app.domain.card.Symptom) -> String = { context.getString(symptomLabelRes(it)) }
        FirestoreHospitalBoardSource(config, auth, serverTransport, log = { Log.i(TRIP_LOG_TAG, it) }) to
            FirestoreCaseSender(config, auth, serverTransport, label)
    }

    /** 제휴 병원 목록 (012, 계획 012 "설계 결정 4"). */
    val hospitalBoardSource: HospitalBoardSource = when (hospitalSourceKind) {
        HospitalSourceKind.BUILT_IN -> BuiltInHospitalBoardSource()
        HospitalSourceKind.SERVER_WITH_FALLBACK -> server?.let { (board, _) ->
            FallbackHospitalBoardSource(server = board, log = { Log.i(TRIP_LOG_TAG, it) })
        } ?: BuiltInHospitalBoardSource()
        HospitalSourceKind.SERVER_ONLY -> server?.let { (board, _) ->
            TimeoutHospitalBoardSource(board, log = { Log.i(TRIP_LOG_TAG, it) })
        } ?: HospitalBoardSource {
            Log.i(TRIP_LOG_TAG, "병원 목록: 서버 설정 없음")
            HospitalBoard.Unavailable("서버 설정 없음")
        }
    }

    /** 근처 동물병원 함수 주소. debug + 에뮬레이터 주소일 때만 있다 (계획 015, 배포하지 않음) */
    private val nearbyFunctionUrl: String? = nearbyFunctionUrlOf(
        projectId = BuildConfig.FIREBASE_PROJECT_ID,
        emulatorHost = BuildConfig.FIREBASE_EMULATOR_HOST,
        isDebugBuild = BuildConfig.DEBUG,
    )

    /** 근처 동물병원 출처 (계획 015 "설계 결정 3"). Fake 모드는 시연용 3곳, 시연·Real은 함수가 있을 때만. */
    val nearbyVetKind: NearbyVetKind =
        nearbyVetKindOf(dataMode, hasFunction = nearbyFunctionUrl != null && serverClient != null)

    /** null = 근처 병원 묶음 없음. 결과는 저장하지 않는다 (카카오 운영정책). 익명 계정은 병원·반려견과 같은 것 */
    val nearbyVetSource: NearbyVetSource? = when (nearbyVetKind) {
        NearbyVetKind.OFF -> null
        NearbyVetKind.FAKE -> FakeNearbyVetSource()
        NearbyVetKind.FUNCTION -> serverClient?.let { (_, auth) ->
            nearbyFunctionUrl?.let { url -> FunctionNearbyVetSource(url, auth, serverTransport, log = { Log.i(TRIP_LOG_TAG, it) }) }
        }
    }

    private val fakeCaseSender = FakeCaseSender()

    /** 병원 출처에 맞는 카드 전송 (012): 서버에서 온 병원만 서버로 보낸다. 내장 시연 병원은 Fake 전송이다. */
    private fun caseSenderFor(candidate: HospitalCandidate): CaseSender =
        server?.second?.takeIf { candidate.sendsCardToServer } ?: fakeCaseSender

    /** 반려견 프로필과 보호자 웹 연결 구성 (014, [petPlanOf]의 표) */
    val petPlan: PetPlan = petPlanOf(dataMode, hasServer = serverConfig != null)

    /** debug 패널 [가짜 보호자 코드 입력]. Fake 연결일 때만 있다 */
    val fakeOwnerLink: FakeOwnerLink? =
        if (petPlan.link == LinkKind.FAKE) FakeOwnerLink(random = SecureRandom().asKotlinRandom()) else null

    /** 서버 연결 (014). 서버 설정이 있고 Fake가 아닐 때만. 로그 태그 `OwnerLink` (결과 종류·HTTP 상태·시간만) */
    private val firestoreOwnerLink: FirestoreOwnerLink? = serverClient?.takeIf { petPlan.link == LinkKind.SERVER }?.let { (config, auth) ->
        FirestoreOwnerLink(config, auth, serverTransport, SecureRandom().asKotlinRandom(), log = { Log.i(OWNER_LINK_LOG_TAG, it) })
    }

    /** 보호자 웹 연결 (014). 연결 코드는 `SecureRandom`으로 만든다 */
    val ownerLink: OwnerLinkService = when (petPlan.link) {
        LinkKind.FAKE -> fakeOwnerLink ?: NoOwnerLink
        LinkKind.SERVER -> firestoreOwnerLink ?: NoOwnerLink
        LinkKind.NONE -> NoOwnerLink
    }

    private val linkedProfileReader: LinkedProfileReader = when (petPlan.link) {
        // Fake 연결은 연결해도 샘플 보리다 (화면에 시연용이라고 알린다)
        LinkKind.FAKE -> LinkedProfileReader { LinkedProfileRead.Loaded(SamplePetProfiles.bori) }
        LinkKind.SERVER -> serverClient?.let { (config, auth) ->
            FirestorePetProfileReader(
                config = config,
                auth = auth,
                transport = serverTransport,
                ownerUid = { firestoreOwnerLink?.ownerUid },
                log = { Log.i(OWNER_LINK_LOG_TAG, it) },
            )
        } ?: LinkedProfileReader { LinkedProfileRead.Unavailable }
        LinkKind.NONE -> LinkedProfileReader { LinkedProfileRead.Unavailable }
    }

    private val linkedPetProfileSource = LinkedPetProfileSource(
        scope = applicationScope,
        link = ownerLink,
        sample = FakePetProfileSource(),
        reader = linkedProfileReader,
        sampleBeforeLink = petPlan.sampleBeforeLink,
    )

    /**
     * 반려견 프로필 (014). 연결 전(또는 연결 없음)에는 샘플 "보리"(Real은 등록 안 됨), 서버 연결 뒤에는 보호자가 등록한 프로필이다.
     * 연결된 뒤 읽지 못하면 샘플로 대신하지 않는다 ([LinkedPetProfileSource]).
     */
    val petProfileSource: PetProfileSource = linkedPetProfileSource

    /** 연결된 반려견 정보를 다시 읽는다 ([진료 카드 만들기], 처음 화면이 보이는 동안 1분마다). 연결 전이면 아무 일도 없다 */
    fun refreshPetProfile() = linkedPetProfileSource.refresh()

    /**
     * 반려견 정보가 시연용 샘플인지. 화면에 그 사실을 알린다 (출품 유의사항). 서버 연결로 받은 정보만 샘플이 아니다.
     * 연결 직후 서버 응답 전에는 화면에 아직 샘플이 남아 있으므로 그동안도 샘플이다 ([LinkedPetProfileSource.isSample]).
     */
    val petProfileIsSample: StateFlow<Boolean> = combine(ownerLink.state, linkedPetProfileSource.isSample) { state, fromSample ->
        petProfileIsSampleOf(petPlan, state) || fromSample
    }.stateIn(applicationScope, SharingStarted.WhileSubscribed(5_000), petProfileIsSampleOf(petPlan, ownerLink.state.value))

    /** 고른 증상과 진료 카드. Fake 길안내와 같은 프로세스 수명이라 안내 시작부터 도착까지 남는다. */
    val medicalCardSession = MedicalCardSession()

    /** 전화 앱 열기 (모든 데이터 모드). 열지 못하면 화면이 번호를 크게 보여 준다. */
    val phoneDialer: PhoneDialer = AndroidPhoneDialer(context)

    /**
     * 병원을 고르고 출발한 뒤의 일 (012): 길안내 시작, 카드·도착 예정 전송, 도착·취소 알림.
     * 로그는 `HospitalTrip` 태그에 결과 종류만 남긴다 (반려견 이름·증상·좌표·병원 이름 없음).
     */
    private val tripTiming = tripTimingOf(sourcePlan.isGuidanceSimulated)

    val hospitalTrip = HospitalTrip(
        guidance = guidanceSource,
        senderFor = ::caseSenderFor,
        scope = applicationScope,
        log = { Log.i(TRIP_LOG_TAG, it) },
        // 시연 길안내는 2초에 1분씩 줄어서 도착 예정과 병원 확인을 몇 초마다 주고받는다 (013, [tripTimingOf])
        etaInterval = tripTiming.etaInterval,
        ackPollInterval = tripTiming.ackPollInterval,
    )

    /** Pleos Vehicle 연결. Real 구성에서만 만든다 (Fake에서는 Vehicle을 만들지 않는다). */
    val vehicleConnection: VehicleConnection? =
        if (sourcePlan.createsVehicleConnection) VehicleConnection(context) else null

    init {
        // 주행 상태와 길안내가 모두 Fake일 때만: 안내를 시작하면 출발하고, 도착하면 주차하는 시연 흐름을 만든다.
        if (sourcePlan.runsTripScenario) {
            FakeTripScenario(applicationScope, guidanceSource, fakeDrivingStateSource).start()
        }
    }

    /**
     * 시연 모드의 길안내 진행 조건: 주행(DRIVING) 중인지. UNKNOWN은 진행하지 않는다.
     *
     * Real 주행 상태는 메인 스레드에서 구독해야 해서(차량 연결 콜백) `flowOn(Main)`을 붙인다.
     * Fake 길안내의 진행은 [applicationScope](Default)에서 돈다.
     * 안내 중에는 화면(`HomeViewModel`)과 이 조건이 각자 차량에 연결한다 (docs/plans 007 "설계 결정 3").
     */
    private fun isDrivingFlow(): Flow<Boolean> {
        val isDriving = drivingStateSource.drivingState.map { it == DrivingState.DRIVING }
        return if (sourcePlan.driving == SourceKind.REAL) isDriving.flowOn(Dispatchers.Main) else isDriving
    }
}

/** 병원 찾기·카드 전송 로그 태그 (012). `adb logcat -s HospitalTrip` */
private const val TRIP_LOG_TAG = "HospitalTrip"

/** 보호자 웹 연결 로그 태그 (014). `adb logcat -s OwnerLink` */
private const val OWNER_LINK_LOG_TAG = "OwnerLink"
