package com.meongcare.app.di

/**
 * 현재 위치를 어디서 받는지 (011). 순수 함수라 JVM으로 시험한다.
 *
 * - 차량 기기의 시연·Real 모드: 내비(NaviHelper) → [SourceKind.REAL]
 * - 그 밖(Fake 모드, 일반 폰): Pleos 에뮬레이터와 같은 시연 위치 → [SourceKind.FAKE]. 일반 폰에는 내비 서비스가 없다.
 *
 * [sourcePlanOf]·[naviUseOf]와 따로 둔 이유: 위치와 길안내는 다른 질문이고, 병원 검색이 붙으면 규칙이 달라질 수 있다.
 * `SourcePlan`에 칸을 더하면 007 표와 그 테스트가 바뀐다.
 */
fun currentLocationKindOf(mode: DataMode, isAutomotive: Boolean): SourceKind = when {
    !isAutomotive -> SourceKind.FAKE
    mode == DataMode.DEMO || mode == DataMode.REAL -> SourceKind.REAL
    else -> SourceKind.FAKE
}

/**
 * 내비가 없는 기기에서 시연 위치 대신 **기기 위치(안드로이드 GPS)**를 쓸지 (계획 015, 사용자 결정 2026-11-08).
 * 근처 동물병원을 "지금 위치" 기준으로 찾으려는 것이다. [currentLocationKindOf]가 Fake라고 한 칸 중에서만 고른다.
 *
 * | 모드 | 차량 기기 | 일반 폰·태블릿 |
 * |---|---|---|
 * | Fake | 시연 위치 | 시연 위치 |
 * | 시연 | 내비 | **기기 위치** |
 * | Real | 내비 | **기기 위치** |
 *
 * 권한을 거부하면 위치 모름이다 (다른 위치로 대신하지 않는다).
 */
fun usesDeviceLocation(mode: DataMode, isAutomotive: Boolean): Boolean =
    !isAutomotive && (mode == DataMode.DEMO || mode == DataMode.REAL)
