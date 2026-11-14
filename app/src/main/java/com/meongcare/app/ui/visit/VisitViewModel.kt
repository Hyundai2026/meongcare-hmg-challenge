package com.meongcare.app.ui.visit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.time.Instant
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 진료 한 번 (디자인 시안 `S.vis`).
 * @param travelMinutes 병원까지 걸린 이동 시간 (귀가 "집까지 약 N분", 시안 `h.m`)
 * @param endedAt 진료 종료 시각 ([진료 완료])
 * @param symptoms 진료 카드 증상 (기록용)
 */
data class Visit(
    val hospitalName: String,
    val travelMinutes: Int?,
    val endedAt: Instant,
    val symptoms: String? = null,
)

/** 진료 기록 저장 질문 (시안 ask / recSaved / recSkip) */
enum class RecordChoice { ASK, SAVED, SKIPPED }

/** 귀가 뒤 이동 중 상태 기록 (시안 rxfb) */
enum class TripFeeling { CALM, ANXIOUS }

/**
 * 병원 도착 뒤 진료 완료·귀가·귀가 완료 (디자인 시안 arrive → after → trip → fin, 2026-11-14). 앱 메모리에만 있다.
 * 병원 쪽 신호(진료 차례 호출)는 시연이다: [차 안에서 대기]를 켜고 [callDelayMillis] 뒤 호출이 온다 (병원 웹과 연결하지 않는다).
 */
class VisitViewModel(
    private val clock: () -> Instant = Instant::now,
    private val callDelayMillis: Long = 8_000L,
) : ViewModel() {
    private val _visit = MutableStateFlow<Visit?>(null)
    val visit: StateFlow<Visit?> = _visit.asStateFlow()

    private val _waitInCar = MutableStateFlow(false)
    val waitInCar: StateFlow<Boolean> = _waitInCar.asStateFlow()

    /** 진료 차례 호출이 왔는지 (차 안에서 대기 중일 때만) */
    private val _called = MutableStateFlow(false)
    val called: StateFlow<Boolean> = _called.asStateFlow()

    private val _record = MutableStateFlow(RecordChoice.ASK)
    val record: StateFlow<RecordChoice> = _record.asStateFlow()

    /** 저장한 진료 기록 (반려동물 카드 "진료 기록"에 보인다) */
    private val _records = MutableStateFlow<List<Visit>>(emptyList())
    val records: StateFlow<List<Visit>> = _records.asStateFlow()

    private val _feeling = MutableStateFlow<TripFeeling?>(null)
    val feeling: StateFlow<TripFeeling?> = _feeling.asStateFlow()

    private var callJob: Job? = null

    /** [차 안에서 대기] */
    fun toggleWaitInCar() {
        val on = !_waitInCar.value
        _waitInCar.value = on
        _called.value = false
        callJob?.cancel()
        if (on) {
            callJob = viewModelScope.launch {
                delay(callDelayMillis)
                if (_waitInCar.value) _called.value = true
            }
        }
    }

    /** [진료 완료] */
    fun complete(hospitalName: String, travelMinutes: Int?, symptoms: String? = null) {
        _visit.value = Visit(hospitalName, travelMinutes, clock(), symptoms)
        _record.value = RecordChoice.ASK
        _feeling.value = null
        stopWaiting()
    }

    /** [저장] */
    fun saveRecord() {
        val v = _visit.value ?: return
        if (_record.value != RecordChoice.ASK) return
        _records.update { listOf(v) + it.filterNot { r -> r.endedAt == v.endedAt } }
        _record.value = RecordChoice.SAVED
    }

    /** [저장 안 함] */
    fun skipRecord() {
        if (_record.value == RecordChoice.ASK) _record.value = RecordChoice.SKIPPED
    }

    /** 귀가 완료의 [편안함 기록]·[불안함 기록] */
    fun recordFeeling(feeling: TripFeeling) {
        _feeling.value = feeling
    }

    /** [처음으로] (도착 화면·귀가 완료) */
    fun clear() {
        _visit.value = null
        _record.value = RecordChoice.ASK
        _feeling.value = null
        stopWaiting()
    }

    private fun stopWaiting() {
        callJob?.cancel()
        _waitInCar.value = false
        _called.value = false
    }
}
