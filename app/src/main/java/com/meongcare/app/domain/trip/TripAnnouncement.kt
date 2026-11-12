package com.meongcare.app.domain.trip

/**
 * 병원 가는 길에 소리로 알리는 사건 (docs/plans 017 "설계 결정 1", 사용자 결정 U1·U2 2026-11-12).
 *
 * 문장 규칙:
 * - 병원 이름 뒤에는 받침과 상관없는 말("까지", "에", "에서")만 붙인다. 병원 이름은 서버에서 와서 끝 글자를 모른다.
 * - 반려견 이름·증상은 넣지 않는다. Pleos SDK가 읽을 문장을 기기 로그에 남기기 때문이다 (017 spike S13).
 * - 두 문장 이하로 짧게 쓴다. 기획 담당이 문장을 고칠 때는 [spokenText]만 고친다.
 */
sealed interface TripAnnouncement {
    /** 출발. [cardSent]는 카드를 실제 서버로 보냈을 때만 true (시연용 병원은 false, U2) */
    data class Departed(val hospitalName: String, val cardSent: Boolean) : TripAnnouncement

    /** 출발 안내 뒤에 카드가 서버로 보내졌다 (늦게 보냄, 다시 보내기 성공) */
    data class CardSent(val hospitalName: String) : TripAnnouncement

    /** 카드를 보내지 못했다 */
    data object CardFailed : TripAnnouncement

    /** 병원이 [카드 확인]을 눌렀다 (013) */
    data class Acknowledged(val hospitalName: String) : TripAnnouncement

    /** 도착했다 */
    data class Arrived(val hospitalName: String) : TripAnnouncement
}

/** 소리로 읽을 문장. */
fun TripAnnouncement.spokenText(): String = when (this) {
    is TripAnnouncement.Departed ->
        "${hospitalName}까지 안내를 시작해요." + if (cardSent) " 진료 카드를 보냈어요." else ""
    is TripAnnouncement.CardSent -> "진료 카드를 ${hospitalName}에 보냈어요."
    TripAnnouncement.CardFailed -> "진료 카드를 보내지 못했어요. 정차하면 다시 보낼 수 있어요."
    is TripAnnouncement.Acknowledged -> "${hospitalName}에서 진료 카드를 확인했어요."
    is TripAnnouncement.Arrived -> "${hospitalName}에 도착했어요. 진료 카드를 접수처에 보여 주세요."
}

/** 로그에 남길 이름. 병원 이름은 넣지 않는다. */
val TripAnnouncement.logName: String
    get() = when (this) {
        is TripAnnouncement.Departed -> if (cardSent) "출발(카드 보냄)" else "출발"
        is TripAnnouncement.CardSent -> "카드 보냄"
        TripAnnouncement.CardFailed -> "카드 못 보냄"
        is TripAnnouncement.Acknowledged -> "병원 확인"
        is TripAnnouncement.Arrived -> "도착"
    }
