package com.meongcare.app.ui.guidance

/** 길안내 영역이 무엇을 보여줄지. */
sealed interface GuidanceUiState {
    val destinationName: String

    /** 안내 중이 아니다. [destinationName]은 안내를 시작할 병원이다. */
    data class Idle(override val destinationName: String) : GuidanceUiState

    /** 안내 중이다. [remainingMinutes]가 `null`이면 경로를 찾는 중이다. 값이 있으면 1 이상이다. */
    data class Guiding(override val destinationName: String, val remainingMinutes: Int?) : GuidanceUiState

    /** 도착했다. */
    data class Arrived(override val destinationName: String) : GuidanceUiState
}
