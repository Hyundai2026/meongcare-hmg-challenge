package com.meongcare.app.di

/** 제휴 병원 목록을 어디서 받는지 (012). */
enum class HospitalSourceKind {
    /** 앱에 넣어 둔 시연 병원 5곳. 카드는 Fake 전송 (실제로 보내지 않는다) */
    BUILT_IN,

    /** 서버에서 받고, 5초 안에 못 받거나 실패하면 내장 시연 병원으로 대신한다 (시연: 네트워크 없이도 끝까지) */
    SERVER_WITH_FALLBACK,

    /** 서버만. 설정이 없거나 실패하면 "병원 정보를 받지 못했어요" + [다시 시도] */
    SERVER_ONLY,
}

/**
 * 병원 목록 출처를 정한다 (계획 012 "설계 결정 4"). 순수 함수라 JVM으로 시험한다.
 * 현재 위치는 011 [currentLocationKindOf] 그대로다. [sourcePlanOf]·[naviUseOf]·[currentLocationKindOf]는 바꾸지 않는다.
 *
 * | 모드 | 서버 설정 있음 | 없음 |
 * |---|---|---|
 * | Fake | 내장 | 내장 |
 * | 시연 (release) | 서버 → 실패하면 내장 | 내장 |
 * | Real | 서버만 | 서버만 (항상 실패 → 다시 시도) |
 *
 * 카드 전송은 목록이 어디서 왔는지로 고른다: 서버에서 온 병원만 서버로 보내고, 내장 병원은 Fake 전송이다.
 *
 * @param hasServer `secrets.properties`에 Firebase 설정이 있는지 (`serverConfigOf`가 null이 아닌지)
 */
fun hospitalSourceKindOf(mode: DataMode, hasServer: Boolean): HospitalSourceKind = when (mode) {
    DataMode.FAKE -> HospitalSourceKind.BUILT_IN
    DataMode.DEMO -> if (hasServer) HospitalSourceKind.SERVER_WITH_FALLBACK else HospitalSourceKind.BUILT_IN
    DataMode.REAL -> HospitalSourceKind.SERVER_ONLY
}
