package com.meongcare.app.data.hospital

import com.meongcare.app.domain.hospital.Hospital
import com.meongcare.app.domain.trip.CaseCard
import com.meongcare.app.domain.trip.CaseEta
import com.meongcare.app.domain.trip.CaseRef
import com.meongcare.app.domain.trip.CaseSender
import com.meongcare.app.domain.trip.CaseStatus
import com.meongcare.app.domain.trip.SendResult
import java.time.Instant

/**
 * 시연용 카드 전송: 아무 데도 보내지 않고 메모리에 남긴 뒤 성공한다.
 * 내장 시연 병원(Fake 모드, 서버 대체)일 때 쓴다. 화면은 "시연용: 실제 병원에는 보내지 않았어요"라고 알린다.
 */
class FakeCaseSender : CaseSender {

    /** 보낸 기록 (테스트와 debug 확인용). 화면에는 쓰지 않는다. */
    data class Record(val ref: CaseRef, val card: CaseCard, val etas: List<CaseEta>, val status: CaseStatus)

    private val lock = Any()
    private val records = mutableMapOf<CaseRef, Record>()
    private var next = 1

    val sent: List<Record> get() = synchronized(lock) { records.values.toList() }

    override suspend fun send(hospital: Hospital, card: CaseCard, eta: CaseEta): SendResult = synchronized(lock) {
        val hospitalId = hospital.id ?: return@synchronized SendResult.Failed("병원 ID 없음")
        val ref = CaseRef(hospitalId = hospitalId, caseId = "fake-${next++}")
        records[ref] = Record(ref, card, listOf(eta), CaseStatus.EN_ROUTE)
        SendResult.Sent(ref)
    }

    override suspend fun updateEta(ref: CaseRef, eta: CaseEta): Boolean = synchronized(lock) {
        val record = records[ref] ?: return@synchronized false
        records[ref] = record.copy(etas = record.etas + eta)
        true
    }

    override suspend fun finish(ref: CaseRef, status: CaseStatus, at: Instant): Boolean = synchronized(lock) {
        val record = records[ref] ?: return@synchronized false
        records[ref] = record.copy(status = status)
        true
    }
}
