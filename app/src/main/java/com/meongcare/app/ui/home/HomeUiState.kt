package com.meongcare.app.ui.home

/** 홈 화면이 무엇을 보여줄지. */
sealed interface HomeUiState {
    /** 정차 화면: 일반 기능과 텍스트 입력을 쓸 수 있다. */
    data object Parked : HomeUiState

    /** 주행 화면: 최소 정보, 큰 터치 타깃, 텍스트 입력 없음 (AGENTS.md 규칙 3). */
    data object Driving : HomeUiState
}
