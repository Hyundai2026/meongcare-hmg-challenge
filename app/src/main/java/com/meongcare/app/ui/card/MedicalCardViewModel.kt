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
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
) {
    /** 증상 고르기 화면에서 선택된 증상 */
    val pickingDraft: Set<Symptom> get() = (session as? CardSessionState.Picking)?.draft ?: emptySet()
}

/**
 * 반려견 프로필과 진료 카드 세션을 합쳐 카드 화면 상태를 만들고, 버튼을 세션·전화 동작으로 바꾼다.
 *
 * 주행 상태와 길안내는 모른다. 무엇을 보여 줄지는 `homeContentOf`(주행 안전)와 `parkedContentOf`(정차 안)가 정한다.
 */
class MedicalCardViewModel(
    petProfileSource: PetProfileSource,
    private val session: MedicalCardSession,
    private val hospital: Hospital,
    private val phoneDialer: PhoneDialer,
    private val clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {

    private val shownPhoneNumber = MutableStateFlow<String?>(null)

    val uiState: StateFlow<CardFlowUiState> =
        combine(petProfileSource.profile, session.state, shownPhoneNumber) { profile, state, shown ->
            uiStateOf(profile, state, shown)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = uiStateOf(profile = null, state = session.state.value, shown = null),
        )

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
        val number = hospital.phoneNumber ?: return
        if (!phoneDialer.openDialer(number)) shownPhoneNumber.value = number
    }

    /** 번호 패널 [확인] */
    fun onDismissPhoneNumber() {
        shownPhoneNumber.value = null
    }

    private fun uiStateOf(profile: PetProfileLoad?, state: CardSessionState, shown: String?) = CardFlowUiState(
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
        hospitalName = hospital.name,
        hospitalPhone = hospital.phoneNumber,
        isHospitalSample = hospital.isSample,
        shownPhoneNumber = shown,
    )

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        fun factory(
            petProfileSource: PetProfileSource,
            session: MedicalCardSession,
            hospital: Hospital,
            phoneDialer: PhoneDialer,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { MedicalCardViewModel(petProfileSource, session, hospital, phoneDialer) }
        }
    }
}
