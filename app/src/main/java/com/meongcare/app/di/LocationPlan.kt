package com.meongcare.app.di

import com.meongcare.app.data.location.FakeCurrentLocationSource
import com.meongcare.app.domain.location.CurrentLocation

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
 * 내비를 쓰지 않는 칸의 **고정 위치** (계획 016, 사용자 결정 2026-11-09). 내비 칸(차량 기기의 시연·Real)이면 null.
 *
 * 일반 에뮬레이터에는 GPS가 없어(기본 위치가 미국) 기기 위치를 쓰면 누가 어디서 돌리느냐에 따라 결과가 달라진다.
 * 그래서 폰·태블릿은 위치를 고정한다. 015의 기기 위치(안드로이드 GPS)를 대신한다.
 *
 * | 모드 | 차량 기기 | 일반 폰·태블릿 |
 * |---|---|---|
 * | Fake | 시흥동 ([FakeCurrentLocationSource.DEMO_LOCATION]) | 시흥동 |
 * | 시연 | null (내비) | 서초동 서운로 62 ([FakeCurrentLocationSource.PHONE_LOCATION]) |
 * | Real | null (내비) | 서초동 서운로 62 |
 *
 * 규칙: `fixedLocationOf(...) == null` ⇔ `currentLocationKindOf(...) == REAL`.
 */
fun fixedLocationOf(mode: DataMode, isAutomotive: Boolean): CurrentLocation? = when {
    currentLocationKindOf(mode, isAutomotive) == SourceKind.REAL -> null
    mode == DataMode.FAKE -> FakeCurrentLocationSource.DEMO_LOCATION
    else -> FakeCurrentLocationSource.PHONE_LOCATION
}
