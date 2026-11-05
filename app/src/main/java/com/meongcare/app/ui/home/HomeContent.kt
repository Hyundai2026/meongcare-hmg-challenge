package com.meongcare.app.ui.home

import com.meongcare.app.ui.guidance.GuidanceUiState

/** 홈에서 보여줄 화면 종류. */
enum class HomeContent {
    /** 정차 화면 (안내 시작·취소 포함) */
    PARKED,

    /** 도착 화면 (진료 카드). 정차 중에만 보여준다. */
    ARRIVAL,

    /** 주행 화면 (최소 정보, 버튼 없음) */
    DRIVING,
}

/**
 * 주행 상태와 길안내 상태를 합쳐 보여줄 화면을 고른다.
 *
 * - 주행 중(또는 주행 상태를 모름)이면 안내 상태와 상관없이 주행 화면이다.
 *   도착했더라도 진료 카드는 정차한 뒤에 보여준다 (AGENTS.md 규칙 3).
 * - 정차 중에 도착 상태면 도착 화면이다.
 * - 그 밖의 정차는 정차 화면이다.
 */
fun homeContentOf(home: HomeUiState, guidance: GuidanceUiState): HomeContent = when (home) {
    HomeUiState.Driving -> HomeContent.DRIVING
    HomeUiState.Parked -> if (guidance is GuidanceUiState.Arrived) HomeContent.ARRIVAL else HomeContent.PARKED
}
