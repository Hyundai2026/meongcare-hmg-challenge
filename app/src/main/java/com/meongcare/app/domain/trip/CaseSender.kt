package com.meongcare.app.domain.trip

import com.meongcare.app.domain.hospital.Hospital
import java.time.Instant

/** 카드를 보낸 결과. */
sealed interface SendResult {
    /** 보냈다. [ref]로 남은 시간·상태를 갱신한다 */
    data class Sent(val ref: CaseRef) : SendResult

    /** 보내지 못했다. [reason]은 로그용 짧은 이유 (개인정보 없음) */
    data class Failed(val reason: String) : SendResult
}

/**
 * 진료 카드와 도착 예정을 병원에 보내는 곳 (docs/data/emergency-case.md).
 *
 * 구현: 서버(Firestore REST) / Fake(메모리에 남기고 성공. 시연용 내장 병원일 때).
 * 모든 함수는 취소 말고는 예외를 던지지 않고 결과로 알린다. 전송 실패가 길안내를 막지 않게 하려는 것이다.
 */
interface CaseSender {
    /** 새 환자 건을 만든다 (상태 "오는 중"). [hospital]의 `id`가 없으면 [SendResult.Failed]다. */
    suspend fun send(hospital: Hospital, card: CaseCard, eta: CaseEta): SendResult

    /** 남은 시간을 바꾼다. 성공하면 true */
    suspend fun updateEta(ref: CaseRef, eta: CaseEta): Boolean

    /** 도착·취소를 알린다. 성공하면 true */
    suspend fun finish(ref: CaseRef, status: CaseStatus, at: Instant): Boolean
}
