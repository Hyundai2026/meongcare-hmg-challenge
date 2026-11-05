package com.meongcare.app.di

/** 차량 내비(NaviHelper)를 길안내에 얼마나 쓰는지 (010). */
enum class NaviUse {
    /** 쓰지 않는다 ([sourcePlanOf]의 길안내 그대로) */
    NONE,

    /** 경로만 내비, 남은 시간·도착은 시연용 (시연 모드 + 차량 기기) */
    ROUTE_ONLY,

    /** 전부 내비 (Real 모드 + 차량 기기) */
    FULL,
}

/**
 * 데이터 모드와 기기로 내비 사용 범위를 정한다. 순수 함수라 JVM으로 시험한다.
 * [sourcePlanOf]의 표(007)는 바꾸지 않고, 내비를 쓸 때만 길안내 구현을 바꿔 끼운다.
 * 일반 폰에는 내비 앱이 없으므로 NaviHelper를 만들지 않는다.
 */
fun naviUseOf(mode: DataMode, isAutomotive: Boolean): NaviUse = when {
    !isAutomotive -> NaviUse.NONE
    mode == DataMode.REAL -> NaviUse.FULL
    mode == DataMode.DEMO -> NaviUse.ROUTE_ONLY
    else -> NaviUse.NONE
}
