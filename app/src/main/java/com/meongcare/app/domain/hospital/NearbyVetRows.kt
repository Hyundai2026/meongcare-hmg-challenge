package com.meongcare.app.domain.hospital

import com.meongcare.app.domain.location.CurrentLocation

/** 반경 + 여유. 서버 결과를 믿지만 이보다 먼 곳은 한 번 더 뺀다 (km). 정확히 이 값은 남긴다. */
const val NEARBY_MAX_DISTANCE_KM = NEARBY_RADIUS_METERS / 1000.0 + 0.5

/** 이름이 같은 제휴 병원을 같은 곳으로 보는 직선 거리 (km) */
const val NEARBY_SAME_PLACE_KM = 0.1

/** 근처 병원 묶음에 보일 최대 개수 */
const val NEARBY_MAX_ROWS = 15

/**
 * 근처 검색으로 찾은 병원을 화면 줄로 정리한다. 순수 함수다 (같은 입력이면 항상 같은 순서). 계획 015 "설계 결정 2".
 *
 * | 경우 | 규칙 |
 * |---|---|
 * | 줄 모양 | 출처 [HospitalOrigin.NEARBY_SEARCH], 대기 정보 없음([StatusFreshness.NONE]), 이동은 [estimateTravel] |
 * | 순서 | 직선 거리 → 이름 → 위도 → 경도 |
 * | 반경 밖 | [NEARBY_MAX_DISTANCE_KM]보다 먼 곳은 뺀다 |
 * | 제휴 병원과 같은 곳 | 이름이 같고(공백을 모두 뺀 뒤 비교) 직선 [NEARBY_SAME_PLACE_KM] 안인 제휴 병원이 있으면 뺀다 (제휴 쪽에 대기 정보가 있다) |
 * | 검색 결과 안에서 중복 | 이름과 좌표가 같으면 하나만 |
 * | 개수 | 정리한 뒤 앞에서 [max]곳 |
 */
fun nearbyVetRowsOf(
    found: List<Hospital>,
    from: CurrentLocation,
    partners: List<HospitalCandidate>,
    max: Int = NEARBY_MAX_ROWS,
): List<RankedHospital> {
    val partnerPlaces = partners.map { it.hospital }
    return found
        .distinctBy { Triple(it.name, it.destination.latitude, it.destination.longitude) }
        .map { it to distanceKm(from, it) }
        .filter { (_, km) -> km <= NEARBY_MAX_DISTANCE_KM }
        .filterNot { (hospital, _) -> partnerPlaces.any { isSamePlace(it, hospital) } }
        .sortedWith(
            compareBy<Pair<Hospital, Double>> { it.second }
                .thenBy { it.first.name }
                .thenBy { it.first.destination.latitude }
                .thenBy { it.first.destination.longitude },
        )
        .take(max.coerceAtLeast(0))
        .map { (hospital, _) ->
            RankedHospital(
                candidate = HospitalCandidate(hospital, status = null, origin = HospitalOrigin.NEARBY_SEARCH),
                travel = estimateTravel(from, hospital.destination),
                freshness = StatusFreshness.NONE,
                statusAge = null,
            )
        }
}

private fun distanceKm(from: CurrentLocation, to: Hospital): Double =
    TravelEstimate.straightDistanceKm(from.latitude, from.longitude, to.destination.latitude, to.destination.longitude)

private fun isSamePlace(partner: Hospital, nearby: Hospital): Boolean =
    normalizedName(partner.name) == normalizedName(nearby.name) &&
        TravelEstimate.straightDistanceKm(
            partner.destination.latitude,
            partner.destination.longitude,
            nearby.destination.latitude,
            nearby.destination.longitude,
        ) <= NEARBY_SAME_PLACE_KM

private fun normalizedName(name: String): String = name.filterNot { it.isWhitespace() }
