package com.meongcare.app.ui.hospital

import com.meongcare.app.data.location.FakeCurrentLocationSource
import com.meongcare.app.domain.guidance.Destination
import com.meongcare.app.domain.hospital.Hospital
import com.meongcare.app.domain.hospital.HospitalCandidate
import com.meongcare.app.domain.hospital.HospitalOrigin
import com.meongcare.app.domain.hospital.RankedHospital
import com.meongcare.app.domain.hospital.StatusFreshness
import kotlin.math.hypot
import kotlin.time.Duration.Companion.minutes

/**
 * 홈 [병원 찾기]의 시연용 병원 (계획 019, 사용자 결정 2026-11-13: 디자인 시안의 이름으로 고정, 카카오 API 안 씀).
 * **가상의 병원**이다: 이름·거리·이동 시간은 시안 값이고 대기 시간은 알 수 없다("대기시간 미확인").
 * 카드를 받지 않는 병원(근처 검색과 같은 출처)이라 확인 화면에서 [길 안내 시작]만 있다. 전화번호는 없다.
 *
 * @param mapX 시연 지도 위 위치 (0~1, 왼쪽 → 오른쪽). 지도 가운데(0.5, 0.5)가 현재 위치
 * @param mapY 시연 지도 위 위치 (0~1, 위 → 아래)
 */
data class DemoHospital(
    val rank: Int,
    val row: RankedHospital,
    val distanceKm: Double,
    val travelMinutes: Int,
    val mapX: Float,
    val mapY: Float,
) {
    val name: String get() = row.hospital.name
}

object DemoHospitals {
    /** 가까운 순 (시안 순서) */
    val all: List<DemoHospital> by lazy {
        listOf(
            demo(1, "별숲동물의료센터", 4.0, 10, 0.425f, 0.55f),
            demo(2, "온유동물병원", 6.1, 15, 0.70f, 0.25f),
            demo(3, "나래동물의료센터", 8.2, 20, 0.30f, 0.42f),
            demo(4, "달빛24시동물의료센터", 10.3, 25, 0.375f, 0.84f),
            demo(5, "가람24시동물병원", 12.5, 30, 0.65f, 0.70f),
        )
    }

    /** 지도 위 방향으로, 시연 위치(서초동)에서 거리만큼 떨어진 좌표 (길안내 목적지용 어림) */
    private fun demo(rank: Int, name: String, km: Double, minutes: Int, x: Float, y: Float): DemoHospital {
        val origin = FakeCurrentLocationSource.PHONE_LOCATION
        val dx = (x - 0.5).toDouble()
        val dy = (y - 0.5).toDouble()
        val len = hypot(dx, dy).takeIf { it > 0 } ?: 1.0
        val lat = origin.latitude - dy / len * km / KM_PER_LAT
        val lon = origin.longitude + dx / len * km / KM_PER_LON
        val hospital = Hospital(
            destination = Destination(name = name, latitude = lat, longitude = lon, expectedTravel = minutes.minutes),
            phoneNumber = null,
            isSample = true,
        )
        val row = RankedHospital(
            candidate = HospitalCandidate(hospital, status = null, origin = HospitalOrigin.NEARBY_SEARCH),
            travel = minutes.minutes,
            freshness = StatusFreshness.NONE,
            statusAge = null,
        )
        return DemoHospital(rank, row, km, minutes, x, y)
    }

    private const val KM_PER_LAT = 111.0
    private const val KM_PER_LON = 88.2
}
