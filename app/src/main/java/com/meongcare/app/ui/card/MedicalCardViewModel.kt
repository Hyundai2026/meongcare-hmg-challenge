package com.meongcare.app.ui.card

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.meongcare.app.domain.card.CardSessionState
import com.meongcare.app.domain.card.MedicalCardSession
import com.meongcare.app.domain.card.Symptom
import com.meongcare.app.domain.hospital.Hospital
import com.meongcare.app.domain.pet.PetProfileLoad
import com.meongcare.app.domain.pet.PetProfileSource
import com.meongcare.app.domain.phone.PhoneDialer
import com.meongcare.app.domain.trip.CaseCard
import com.meongcare.app.domain.trip.caseCardOf
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn

/** 진료 카드 흐름(처음 화면·증상 고르기·카드)이 보여 줄 것. */
data class CardFlowUiState(
    val session: CardSessionState,
    val card: MedicalCardUiState,
    val hospitalName: String,
    /** null이면 [병원에 전화]를 숨긴다 */
    val hospitalPhone: String?,
    val isHospitalSample: Boolean,
    /** 전화 앱을 열지 못했을 때 크게 보여 줄 번호. null이면 번호 패널이 없다 */
    val shownPhoneNumber: String?,
    /** [홈]을 눌러 카드를 둔 채 처음 화면을 보는 중 */
    val atHome: Boolean = false,
) {
    /** 처음 화면에 [카드 보기]를 둘 카드가 있는지 */
    val hasCard: Boolean get() = session is CardSessionState.Ready

    /** 증상 고르기 화면에서 선택된 증상 */
    val pickingDraft: Set<Symptom> get() = (session as? CardSessionState.Picking)?.draft ?: emptySet()
}

/**
 * 반려견 프로필과 진료 카드 세션을 합쳐 카드 화면 상태를 만들고, 버튼을 세션·전화 동작으로 바꾼다.
 *
 * 주행 상태와 길안내는 모른다. 무엇을 보여 줄지는 `homeContentOf`(주행 안전)와 `parkedContentOf`(정차 안)가 정한다.
 *
 * @param hospital 고정 병원 (010까지의 시연 병원). [selectedHospital]이 없을 때만 쓴다.
 * @param selectedHospital 병원 찾기에서 고른 병원 (012). 주면 [hospital] 대신 이것을 쓰고, 아직 고르지 않았으면(null)
 *   [병원에 전화]를 숨긴다 (계획 012 K5). 주지 않으면 예전처럼 [hospital]이다.
 */
class MedicalCardViewModel(
    petProfileSource: PetProfileSource,
    private val session: MedicalCardSession,
    private val hospital: Hospital,
    private val phoneDialer: PhoneDialer,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val selectedHospital: StateFlow<Hospital?>? = null,
) : ViewModel() {

    /** 지금 카드가 쓰는 병원. 고르기 전이면 null */
    private val currentHospital: Hospital? get() = if (selectedHospital != null) selectedHospital.value else hospital

    /** 마지막으로 받은 프로필. 병원에 보낼 카드를 만들 때 쓴다 */
    @Volatile
    private var latestProfile: PetProfileLoad? = null

    private val shownPhoneNumber = MutableStateFlow<String?>(null)

    /**
     * 프로필이 아직 오지 않은 동안은 null(불러오는 중)로 시작한다. `combine`은 모든 흐름이 한 번씩 값을 내야
     * 움직이므로, 이렇게 하지 않으면 첫 프로필이 늦게 오는 출처(나중 서버)에서 버튼을 눌러도 화면이 바뀌지 않는다.
     */
    private val profileOrLoading: Flow<PetProfileLoad?> =
        petProfileSource.profile.map<PetProfileLoad, PetProfileLoad?> { it }.onStart { emit(null) }

    val uiState: StateFlow<CardFlowUiState> =
        combine(profileOrLoading, session.state, shownPhoneNumber, session.atHome, selectedHospital ?: flowOf(hospital)) { profile, state, shown, atHome, chosen ->
            latestProfile = profile
            uiStateOf(profile, state, shown, atHome, chosen)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = uiStateOf(profile = null, state = session.state.value, shown = null, atHome = session.atHome.value, chosen = currentHospital),
        )

    /** [홈]: 카드를 지우지 않고 처음 화면으로 */
    fun onGoHome() {
        shownPhoneNumber.value = null
        session.goHome()
    }

    /** 처음 화면의 [카드 보기] */
    fun onOpenCard() = session.openCard()

    /** 처음 화면의 [새로 만들기]: 있던 카드를 버리고 다시 고른다 */
    fun onStartNewCard() = session.startNew()

    /** [진료 카드 만들기] */
    fun onStartCard() = session.startPicking()

    fun onToggleSymptom(symptom: Symptom) = session.toggle(symptom)

    /** [카드 만들기] */
    fun onConfirmSymptoms() = session.confirm()

    /** 증상 고르기 [취소] */
    fun onCancelPicking() = session.cancelPicking()

    /** 카드 [수정]: 지금 증상이 선택된 채로 다시 고른다 */
    fun onEditSymptoms() = session.startPicking()

    /** 카드 [닫기]: 카드를 버리고 처음 화면으로 */
    fun onCloseCard() {
        shownPhoneNumber.value = null
        session.clear()
    }

    /** [병원에 전화]: 전화 앱을 열고, 열지 못하면 번호를 크게 보여 준다 */
    fun onCallHospital() {
        currentHospital?.phoneNumber?.let(::onCallNumber)
    }

    /** 병원 확인 화면의 [병원에 전화] (012): 아직 고르기 전인 병원 번호로 */
    fun onCallNumber(number: String) {
        if (!phoneDialer.openDialer(number)) shownPhoneNumber.value = number
    }

    /**
     * 병원에 보낼 카드 (012): 지금 고른 증상 + 받은 반려견 정보. 사진은 [caseCardOf]가 지운다.
     * 프로필을 아직 받지 못했거나 읽지 못했으면 반려견 정보 없이(null) 만든다.
     */
    fun caseCard(): CaseCard = caseCardOf(
        pet = (latestProfile as? PetProfileLoad.Loaded)?.profile,
        symptoms = (session.state.value as? CardSessionState.Ready)?.symptoms ?: emptySet(),
    )

    /** 번호 패널 [확인] */
    fun onDismissPhoneNumber() {
        shownPhoneNumber.value = null
    }

    private fun uiStateOf(profile: PetProfileLoad?, state: CardSessionState, shown: String?, atHome: Boolean, chosen: Hospital?) = CardFlowUiState(
        session = state,
        card = medicalCardOf(
            profile = profile,
            symptoms = when (state) {
                CardSessionState.None -> emptySet()
                is CardSessionState.Picking -> state.draft
                is CardSessionState.Ready -> state.symptoms
            },
            today = LocalDate.now(clock),
            zone = clock.zone,
        ),
        hospitalName = chosen?.name ?: hospital.name,
        hospitalPhone = chosen?.phoneNumber,
        isHospitalSample = chosen?.isSample ?: hospital.isSample,
        shownPhoneNumber = shown,
        atHome = atHome,
    )

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        fun factory(
            petProfileSource: PetProfileSource,
            session: MedicalCardSession,
            hospital: Hospital,
            phoneDialer: PhoneDialer,
            selectedHospital: StateFlow<Hospital?>? = null,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { MedicalCardViewModel(petProfileSource, session, hospital, phoneDialer, selectedHospital = selectedHospital) }
        }
    }
}
