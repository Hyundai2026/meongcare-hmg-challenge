package com.meongcare.app.domain.hospital

import com.meongcare.app.domain.guidance.Destination
import com.meongcare.app.domain.location.CurrentLocation
import kotlin.math.asin
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * 이동 시간 어림값 (계획 012 "설계 결정 1").
 *
 * 병원마다 실제 경로 시간을 길안내 없이 받는 방법이 확인되지 않아서(NaviHelper `requestRoute`는 안내를 시작한다),
 * 직선 거리로 어림한다. 화면에는 "약"을 붙이고 "직선 거리로 어림한 값"이라고 알린다.
 *
 * **시연용 가정이다.** 계수는 Pleos 에뮬레이터 내비 관찰 1건에 맞췄다: 시연 위치 → "시연용 동물병원"이
 * 내비로 12km·35분, 직선으로 9.59km였다 (docs/pleos/sdk-notes.md `[obs-navi-emu]`).
 * 12 ÷ 9.59 ≈ [DETOUR_FACTOR], 12km ÷ 35분 ≈ [AVERAGE_SPEED_KMH]. 막히는 길, 강·산은 모른다.
 */
object TravelEstimate {
    /** 도로가 직선보다 돌아가는 정도 */
    const val DETOUR_FACTOR = 1.25

    /** 평균 속도 (km/h) */
    const val AVERAGE_SPEED_KMH = 21.0

    /** 지구 평균 반지름 (km, IUGG) */
    const val EARTH_RADIUS_KM = 6371.0088

    /** 부동소수 오차로 정수 분이 한 분 올라가지 않게 빼는 아주 작은 값 */
    private const val EPSILON = 1e-9

    /** 두 좌표 사이의 직선(대권) 거리 (km). 하버사인 공식. */
    fun straightDistanceKm(fromLatitude: Double, fromLongitude: Double, toLatitude: Double, toLongitude: Double): Double {
        val dLat = Math.toRadians(toLatitude - fromLatitude)
        val dLon = Math.toRadians(toLongitude - fromLongitude)
        val h = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(fromLatitude)) * cos(Math.toRadians(toLatitude)) * sin(dLon / 2) * sin(dLon / 2)
        return 2 * EARTH_RADIUS_KM * asin(sqrt(h.coerceIn(0.0, 1.0)))
    }

    /** 직선 거리(km) → 이동 어림 분. 올림하고, 최소 1분이다 (같은 좌표도 1분). */
    fun minutesForKm(km: Double): Int {
        val minutes = km * DETOUR_FACTOR / AVERAGE_SPEED_KMH * 60
        return max(1, ceil(minutes - EPSILON).toInt())
    }
}

/** [from]에서 [to]까지 이동 어림 시간 (분 단위, 최소 1분). */
fun estimateTravel(from: CurrentLocation, to: Destination): Duration {
    val km = TravelEstimate.straightDistanceKm(from.latitude, from.longitude, to.latitude, to.longitude)
    return TravelEstimate.minutesForKm(km).minutes
}
