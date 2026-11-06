package com.meongcare.app.ui.hospital

import com.meongcare.app.domain.hospital.HospitalOrigin
import com.meongcare.app.domain.hospital.HospitalRanking

/** 병원 목록 화면이 보여 줄 것. */
sealed interface HospitalFinderUiState {
    /** 처음 받는 중 */
    data object Loading : HospitalFinderUiState

    /**
     * 받았다.
     * @param origin 목록 출처 (서버 / 내장 / 서버 대신 내장). 안내 문구를 고른다.
     * @param isRefreshing 목록을 다시 받는 중 (그동안 이전 목록을 그대로 보인다)
     * @param isLocating 위치를 다시 묻는 중
     */
    data class Loaded(
        val ranking: HospitalRanking,
        val origin: HospitalOrigin,
        val isRefreshing: Boolean = false,
        val isLocating: Boolean = false,
    ) : HospitalFinderUiState

    /** 받지 못했다 (Real 모드에서 서버 실패). [다시 시도]를 보인다 */
    data class Failed(val isRetrying: Boolean = false) : HospitalFinderUiState
}
