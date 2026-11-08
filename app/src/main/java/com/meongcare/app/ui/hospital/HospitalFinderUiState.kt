package com.meongcare.app.ui.hospital

import com.meongcare.app.domain.hospital.HospitalOrigin
import com.meongcare.app.domain.hospital.HospitalRanking
import com.meongcare.app.domain.hospital.RankedHospital

/** 병원 목록 화면이 보여 줄 것. */
sealed interface HospitalFinderUiState {
    /** 처음 받는 중 */
    data object Loading : HospitalFinderUiState

    /**
     * 받았다.
     * @param origin 목록 출처 (서버 / 내장 / 서버 대신 내장). 안내 문구를 고른다.
     * @param isRefreshing 목록을 다시 받는 중 (그동안 이전 목록을 그대로 보인다)
     * @param isLocating 위치를 다시 묻는 중
     * @param nearby 제휴 목록 아래 근처 동물병원 묶음 (계획 015). 제휴 목록은 이것을 기다리지 않는다
     */
    data class Loaded(
        val ranking: HospitalRanking,
        val origin: HospitalOrigin,
        val isRefreshing: Boolean = false,
        val isLocating: Boolean = false,
        val nearby: NearbyState = NearbyState.Off,
    ) : HospitalFinderUiState

    /** 받지 못했다 (Real 모드에서 서버 실패). [다시 시도]를 보인다 */
    data class Failed(val isRetrying: Boolean = false) : HospitalFinderUiState
}

/** 근처 동물병원 묶음의 상태 (계획 015 "설계 결정 4"). */
sealed interface NearbyState {
    /** 기능 없음 (release, 서버 설정 없음). 묶음을 보이지 않는다 */
    data object Off : NearbyState

    /** 현재 위치를 몰라 찾지 않았다. 다른 위치로 대신 찾지 않는다 (011) */
    data object NeedsLocation : NearbyState

    data object Searching : NearbyState

    /** 찾았다. [rows]는 비어 있지 않다 (가까운 순, `nearbyVetRowsOf`) */
    data class Found(val rows: List<RankedHospital>) : NearbyState

    /** 반경 안에 없다 (또는 제휴 병원과 겹치는 곳을 빼고 나니 0곳) */
    data object Empty : NearbyState

    /** 찾지 못했다 (시간 초과, 서버·카카오 실패). [다시 찾기]를 보인다 */
    data class Failed(val isRetrying: Boolean = false) : NearbyState
}
