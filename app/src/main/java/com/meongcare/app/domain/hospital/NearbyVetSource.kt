package com.meongcare.app.domain.hospital

import com.meongcare.app.domain.location.CurrentLocation

/** 근처 동물병원 검색 반경 (미터, 사용자 결정 D2). 서버 함수(`firebase/functions`)도 같은 값을 쓴다. */
const val NEARBY_RADIUS_METERS = 5_000

/** 근처 동물병원 검색 결과 (계획 015 "설계 결정 2"). */
sealed interface NearbyVets {
    /** 찾았다. 비어 있을 수 있다 (반경 안에 없음). 순서는 보장하지 않는다. */
    data class Found(val hospitals: List<Hospital>) : NearbyVets

    /** 찾지 못했다. [reason]은 로그용 짧은 이유 ("시간 초과", "HTTP 429", "형식 오류"). 좌표·이름을 넣지 않는다. */
    data class Unavailable(val reason: String) : NearbyVets
}

/**
 * 근처 동물병원 검색. 제휴 병원이 아니라 대기 정보가 없는 병원을 찾는다 (docs/data/nearby-vets.md).
 *
 * 결과는 저장하지 않는다 (카카오 운영정책: 캐시·저장 금지). 화면이 보이는 동안 메모리에만 둔다.
 */
fun interface NearbyVetSource {
    /**
     * [center]에서 반경 [NEARBY_RADIUS_METERS] 안의 동물병원을 한 번 찾는다. 부를 때마다 새로 찾는다 (캐시 없음).
     * 취소 말고는 예외를 던지지 않는다.
     */
    suspend fun search(center: CurrentLocation): NearbyVets
}
