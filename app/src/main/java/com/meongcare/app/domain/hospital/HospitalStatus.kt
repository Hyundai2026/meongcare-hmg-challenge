package com.meongcare.app.domain.hospital

import java.time.Instant

/** 혼잡도. 화면 글자는 여유 / 보통 / 혼잡 (docs/data/hospital-status.md `status.congestion`). */
enum class Congestion { LOW, NORMAL, HIGH }

/**
 * 제휴 병원이 병원 웹에서 알린 상태 (docs/data/hospital-status.md `status`).
 *
 * 읽는 쪽은 짐작하지 않는다: 필드가 하나라도 틀리면 이 객체를 만들지 않고 "대기 정보 없음"(null)으로 둔다.
 */
data class HospitalStatus(
    /** 지금 오면 예상 대기 시간(분). 0~600 */
    val waitMinutes: Int,
    val congestion: Congestion,
    /** 지금 응급 환자를 받는지 */
    val acceptingEmergency: Boolean,
    /** 병원이 마지막으로 알린 시각 */
    val updatedAt: Instant,
)

/** 병원 목록이 어디서 왔는지. 카드를 어디로 보낼지도 이것으로 정한다 (계획 012 "설계 결정 4"). */
enum class HospitalOrigin {
    /** 서버(Firestore)에서 받은 제휴 병원. 카드를 서버로 보낸다 */
    SERVER,

    /** 앱에 넣어 둔 시연 병원 (Fake 모드, 서버 설정이 없는 시연). 카드는 보내지 않는다 (Fake 전송) */
    BUILT_IN,

    /** 서버에서 받지 못해 대신 보인 시연 병원. 카드는 보내지 않는다 (Fake 전송) */
    BUILT_IN_FALLBACK,
}

/** 순위를 매길 병원 하나: 병원 + 알린 상태(없을 수 있음) + 출처. */
data class HospitalCandidate(
    val hospital: Hospital,
    /** null = 대기 정보 없음 (아직 알리지 않음, 형식 오류, 나중의 카카오 검색 병원) */
    val status: HospitalStatus?,
    val origin: HospitalOrigin,
) {
    /** 이 병원으로 카드를 실제 서버에 보내는지. 서버에서 온 병원만 보낸다. */
    val sendsCardToServer: Boolean get() = origin == HospitalOrigin.SERVER
}
