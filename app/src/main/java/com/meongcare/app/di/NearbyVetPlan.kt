package com.meongcare.app.di

/** 근처 동물병원을 어디서 찾는지 (계획 015). */
enum class NearbyVetKind {
    /** 찾지 않는다. 병원 목록에 근처 병원 묶음이 없다 (release 화면은 지금과 같다) */
    OFF,

    /** 시연용 가짜 근처 병원 3곳 (Fake 모드, debug 전용. 사용자 결정 U3) */
    FAKE,

    /** Firebase Function `nearbyVets` → 카카오 로컬 API (PC의 Firebase 에뮬레이터, debug 전용) */
    FUNCTION,
}

/**
 * 근처 동물병원 출처를 정한다 (계획 015 "설계 결정 3"). 순수 함수라 JVM으로 시험한다.
 * [hospitalSourceKindOf](012)는 바꾸지 않고 같은 방식으로 표를 하나 더했다.
 *
 * | 모드 | 함수 설정 있음 | 없음 |
 * |---|---|---|
 * | Fake | 가짜 | 가짜 |
 * | 시연 | 함수 | 끔 |
 * | Real | 함수 | 끔 |
 *
 * release는 함수 주소가 늘 없다(`nearbyFunctionUrlOf`) → 시연 모드여도 끔.
 *
 * @param hasFunction 함수 주소와 서버 익명 계정이 둘 다 있는지
 */
fun nearbyVetKindOf(mode: DataMode, hasFunction: Boolean): NearbyVetKind = when (mode) {
    DataMode.FAKE -> NearbyVetKind.FAKE
    DataMode.DEMO, DataMode.REAL -> if (hasFunction) NearbyVetKind.FUNCTION else NearbyVetKind.OFF
}
