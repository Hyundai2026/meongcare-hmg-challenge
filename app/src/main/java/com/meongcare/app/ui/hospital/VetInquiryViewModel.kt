package com.meongcare.app.ui.hospital

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meongcare.app.domain.inquiry.InquiryHospital
import com.meongcare.app.domain.inquiry.InquiryScript
import com.meongcare.app.domain.inquiry.InquiryScripts
import com.meongcare.app.domain.inquiry.InquirySnapshot
import com.meongcare.app.domain.inquiry.VetInquiry
import java.time.Instant
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

/** 동물병원 문의 흐름 안의 쪽 (계획 020). 출발 확인은 `HospitalTrip`의 `FinderStep.Confirm`이 맡는다 */
enum class InquiryPage { SEARCH, COMPARE }

/**
 * 카드에서 연 [동물병원 찾기]의 문의 상태 (계획 020). 액티비티 범위. 병원은 시안의 시연 병원([DemoHospitals]).
 * 답변은 [script]의 시연 응답이다 (앱 안에서만, 병원 웹과 연결하지 않는다).
 *
 * 화면이 구독하는 동안 [tickMillis]마다 [snapshot]을 다시 계산한다 (주행으로 바뀌어도 시계는 벽시계라 계속 흐른다).
 *
 * @param clock 지금 시각 (테스트에서 바꾼다)
 */
class VetInquiryViewModel(
    private val clock: () -> Instant = Instant::now,
    private val tickMillis: Long = 1_000L,
) : ViewModel() {

    val hospitals: List<DemoHospital> = DemoHospitals.all

    /** 다음 문의에 쓸 시연 응답 시나리오 (debug 패널에서 바꾼다, 계획 020 D2 안 a) */
    var script: InquiryScript = InquiryScripts.DEFAULT

    private var inquiry = newInquiry()
    private val version = MutableStateFlow(0)

    private val _page = MutableStateFlow(InquiryPage.SEARCH)
    val page: StateFlow<InquiryPage> = _page.asStateFlow()

    /** 비교 화면에서 고른 병원 이름 */
    private val _compareSelected = MutableStateFlow<String?>(null)
    val compareSelected: StateFlow<String?> = _compareSelected.asStateFlow()

    /** 응답 없음 팝업에서 고른 병원 이름 */
    private val _promptSelected = MutableStateFlow<String?>(null)
    val promptSelected: StateFlow<String?> = _promptSelected.asStateFlow()

    /** [다른 병원 N곳에도 문의] 뒤 한 번 보일 N. 보이고 나면 [consumeExpandNotice] */
    private val _expandNotice = MutableStateFlow<Int?>(null)
    val expandNotice: StateFlow<Int?> = _expandNotice.asStateFlow()

    private val ticker = flow {
        while (true) {
            emit(Unit)
            delay(tickMillis)
        }
    }

    val snapshot: StateFlow<InquirySnapshot> = combine(version, ticker) { _, _ -> current() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), current())

    /** 지금 상태 (화면 밖에서 바로 읽을 때) */
    fun current(): InquirySnapshot = inquiry.snapshot(clock())

    fun now(): Instant = clock()

    /** 카드 [동물병원 찾기]: 새 문의 */
    fun restart() {
        inquiry = newInquiry().also { it.start(clock()) }
        _page.value = InquiryPage.SEARCH
        _compareSelected.value = null
        _promptSelected.value = null
        _expandNotice.value = null
        bump()
    }

    fun expand() {
        val added = inquiry.expand(clock())
        if (added > 0) _expandNotice.value = added
        bump()
    }

    fun consumeExpandNotice() {
        _expandNotice.value = null
    }

    fun dismissNoResponse() {
        inquiry.dismissNoResponse(clock())
        _promptSelected.value = null
        bump()
    }

    /** 출발 제안 [다른 병원 비교]: 제안을 닫고 비교 쪽으로 */
    fun compareInsteadOfSuggestion() {
        inquiry.dismissSuggestion()
        bump()
        openCompare()
    }

    fun selectInPrompt(name: String) {
        _promptSelected.value = name
    }

    /** [동물병원 비교]: 가장 빠른 곳을 미리 고른다 */
    fun openCompare() {
        _compareSelected.value = current().best?.key
        _page.value = InquiryPage.COMPARE
    }

    fun backToSearch() {
        _page.value = InquiryPage.SEARCH
    }

    /** 출발 확인에서 [←]: 비교로 (프로토타입 뒤로 순서) */
    fun onBackFromConfirm() {
        if (_compareSelected.value == null) _compareSelected.value = current().best?.key
        _page.value = InquiryPage.COMPARE
    }

    fun selectInCompare(name: String) {
        _compareSelected.value = name
    }

    fun hospitalNamed(name: String?): DemoHospital? = hospitals.firstOrNull { it.name == name }

    private fun newInquiry() = VetInquiry(hospitals.map { InquiryHospital(it.name, it.travelMinutes) }, script)

    private fun bump() {
        version.value = version.value + 1
    }
}
