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

    /**
     * 이 건의 병원 확인(docs/data/emergency-case.md `acknowledgedAt`)을 한 번 읽는다 (013 U1). 던지지 않는다.
     * 기본 구현은 [AckRead.Unsupported]다 (확인을 읽지 않는 전송: 내장 시연 병원).
     */
    suspend fun readAck(ref: CaseRef): AckRead = AckRead.Unsupported
}

/** 병원 확인을 한 번 읽은 결과 (013 U1). */
sealed interface AckRead {
    /** 이 전송 방식은 확인을 읽지 않는다 → 읽기를 멈춘다. 로그 없음 */
    data object Unsupported : AckRead

    /** 아직 확인 전 */
    data object Pending : AckRead

    /** 병원이 확인했다. [at]은 서버 시각 */
    data class Acknowledged(val at: Instant) : AckRead

    /** 건이 없거나 읽을 수 없다 (병원 [완료]로 지움, 다른 계정) → 읽기를 멈춘다 */
    data object Gone : AckRead

    /** 이번에는 읽지 못했다 (네트워크, 401·5xx, 로그인 실패, 형식 오류). [reason]은 로그용 짧은 이유 (개인정보 없음) */
    data class Failed(val reason: String) : AckRead
}
