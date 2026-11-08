package com.meongcare.app.data.hospital

import com.meongcare.app.domain.guidance.Destination
import com.meongcare.app.domain.hospital.Hospital
import com.meongcare.app.domain.hospital.NearbyVetSource
import com.meongcare.app.domain.hospital.NearbyVets
import com.meongcare.app.domain.location.CurrentLocation
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay

/**
 * 근처 동물병원 검색의 Fake (계획 015 "설계 결정 7"). Fake 모드(debug 전용)에서 쓴다 (사용자 결정 U3).
 *
 * - 서버·카카오 키 없이 근처 병원 묶음을 만들고 확인하려는 것이다.
 * - 실제 카카오 응답을 본떠 저장하지 않는다. 이름·좌표·번호는 모두 가상이다.
 *
 * @param delay 화면에서 "찾는 중"을 볼 수 있게 두는 짧은 지연
 * @param result 찾은 결과. 테스트는 실패·빈 결과·느림을 넣는다
 */
class FakeNearbyVetSource(
    private val delay: Duration = 600.milliseconds,
    private val result: (CurrentLocation) -> NearbyVets = { NearbyVets.Found(DEFAULT) },
) : NearbyVetSource {

    override suspend fun search(center: CurrentLocation): NearbyVets {
        if (delay.isPositive()) delay(delay)
        return result(center)
    }

    companion object {
        /**
         * 시연 위치(수정구 시흥동, `FakeCurrentLocationSource.DEMO_LOCATION`) 기준 직선 약 1km(북동)·2.5km(남)·4km(서).
         * 전화번호는 [SampleHospitals]와 같은 걸리지 않는 형식이다. "나"는 번호가 없어 [병원에 전화]가 숨는다.
         * Fake 모드는 길안내도 Fake라 좌표가 도로 위인지는 확인하지 않았다 (Pleos 에뮬레이터에서 쓰지 않는다).
         */
        val DEFAULT: List<Hospital> = listOf(
            sample("시연용 근처 동물병원 가", 37.4191212, 127.102681, "02-0000-0010"),
            sample("시연용 근처 동물병원 나", 37.3902212, 127.094681, null),
            sample("시연용 근처 동물병원 다", 37.4127212, 127.049381, "02-0000-0012"),
        )

        private fun sample(name: String, latitude: Double, longitude: Double, phone: String?) = Hospital(
            destination = Destination(name = name, latitude = latitude, longitude = longitude),
            phoneNumber = phone,
            isSample = true,
        )
    }
}
