package com.meongcare.app.di

/**
 * 현재 위치를 어디서 받는지 (011). 순수 함수라 JVM으로 시험한다.
 *
 * - 차량 기기의 시연·Real 모드: 내비(NaviHelper `getCurrentLocationInfo`) → [SourceKind.REAL]
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
