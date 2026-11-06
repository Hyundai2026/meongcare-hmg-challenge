package com.meongcare.app.domain.hospital

/** 제휴 병원 목록을 받은 결과. */
sealed interface HospitalBoard {
    /**
     * 받았다. [candidates]가 비어 있을 수도 있다 (제휴 병원 0곳).
     * @param origin 목록이 어디서 왔는지. [candidates]의 출처와 같다. 화면 안내 문구를 고르는 데 쓴다.
     */
    data class Loaded(val candidates: List<HospitalCandidate>, val origin: HospitalOrigin) : HospitalBoard

    /**
     * 받지 못했다 (서버 설정 없음, 시간 초과, 네트워크, HTTP 오류, 형식 오류).
     * @param reason 로그용 짧은 이유 (예: "시간 초과", "HTTP 403"). 화면에는 보이지 않는다. 개인정보를 넣지 않는다.
     */
    data class Unavailable(val reason: String) : HospitalBoard
}

/**
 * 제휴 병원 목록과 각 병원이 알린 대기 상태를 받는 곳.
 *
 * 구현 (계획 012 "설계 결정 4", `di/HospitalPlan.kt`):
 * - 내장: 앱에 넣어 둔 시연 병원 5곳 (docs/data/hospitals.sample.json과 같은 값)
 * - 서버: Firestore `hospitals` 컬렉션 (REST)
 * - 대체: 서버가 5초 안에 주지 못하면 내장으로
 *
 * 나중에 카카오 로컬 검색 병원을 더하면, 그 병원은 대기 정보 없음(`status = null`) 후보로 들어온다.
 */
fun interface HospitalBoardSource {
    /**
     * 목록을 한 번 받는다. 부를 때마다 새로 받는다 (캐시 없음).
     * 취소 말고는 예외를 던지지 않는다. 실패는 [HospitalBoard.Unavailable]이다.
     */
    suspend fun load(): HospitalBoard
}
