package com.meongcare.app.data.hospital

import com.meongcare.app.domain.guidance.Destination
import com.meongcare.app.domain.hospital.Congestion
import com.meongcare.app.domain.hospital.Hospital
import com.meongcare.app.domain.hospital.HospitalCandidate
import com.meongcare.app.domain.hospital.HospitalOrigin
import com.meongcare.app.domain.hospital.HospitalStatus
import java.time.Instant

/**
 * 시연용 샘플 병원.
 *
 * - 이름은 가상이다. 실제 병원 이름은 영상에 나오면 제3자 권리 문제가 생길 수 있어 쓰지 않는다
 *   (docs/contest/requirements.md "출품 유의사항").
 * - 주소는 화면에 보여주지 않는다.
 * - 전화번호 `02-0000-000N`도 시연용이다 (사용자 결정 2026-11-05). 국번이 0으로 시작해 일반 가입 번호로
 *   쓰이지 않는 형식이다. 전화 앱에 띄우기만 하고 걸지 않는다. 차에서 전화 앱이 열리는지는 문의 B4로 미확인.
 */
object SampleHospitals {
    /**
     * 010의 고정 시연 병원. 좌표는 Pleos NaviHelper 문서의 예시 좌표이고(docs/pleos/sdk-notes.md NaviHelper 절),
     * Pleos 에뮬레이터 내비에서 경로가 나오는 것을 확인했다 (010).
     * 제휴 병원 목록의 `demo-main`과 같은 곳이다.
     */
    val demo = Hospital(
        destination = Destination(
            name = "시연용 동물병원",
            latitude = 37.48544722,
            longitude = 127.03636666,
        ),
        phoneNumber = "02-0000-0000",
        isSample = true,
    )

    /** 내장 제휴 병원 한 곳: 병원 + 알린 상태 + 알린 지 몇 분 됐는지 */
    data class Partner(
        val hospital: Hospital,
        val waitMinutes: Int,
        val congestion: Congestion,
        val acceptingEmergency: Boolean,
        /** `status.updatedAt = 지금 - 이 분` (docs/data/hospital-status.md "예시 JSON의 모양") */
        val updatedMinutesAgo: Long,
    ) {
        fun candidateAt(now: Instant, origin: HospitalOrigin) = HospitalCandidate(
            hospital = hospital,
            status = HospitalStatus(
                waitMinutes = waitMinutes,
                congestion = congestion,
                acceptingEmergency = acceptingEmergency,
                updatedAt = now.minusSeconds(updatedMinutesAgo * 60),
            ),
            origin = origin,
        )
    }

    /**
     * 시연용 제휴 병원 5곳 (계획 012). **값은 docs/data/hospitals.sample.json과 같다.**
     * 그 파일을 바꾸면 여기도 바꾼다 (`SampleHospitalsJsonTest`가 대조한다).
     * "가장 가까운 곳이 가장 빠른 곳은 아니다"가 한 화면에 보이도록 짰다 (docs/data/hospital-status.md "시연 병원 5곳").
     */
    val partners: List<Partner> = listOf(
        partner("demo-fast", "시연용 바른 동물병원", "02-0000-0001", 37.367, 127.1083, 10, Congestion.NORMAL, true, 2),
        partner("demo-main", demo.name, demo.phoneNumber!!, demo.destination.latitude, demo.destination.longitude, 5, Congestion.LOW, true, 5),
        partner("demo-near", "시연용 가까운 동물병원", "02-0000-0002", 37.3947, 127.1112, 60, Congestion.HIGH, true, 3),
        partner("demo-stale", "시연용 정보 지난 동물병원", "02-0000-0003", 37.4873, 127.1017, 5, Congestion.LOW, true, 90),
        partner("demo-closed", "시연용 응급 휴진 동물병원", "02-0000-0004", 37.4113, 127.1275, 0, Congestion.NORMAL, false, 10),
    )

    private fun partner(
        id: String,
        name: String,
        phone: String,
        latitude: Double,
        longitude: Double,
        waitMinutes: Int,
        congestion: Congestion,
        accepting: Boolean,
        minutesAgo: Long,
    ) = Partner(
        hospital = Hospital(
            destination = Destination(name = name, latitude = latitude, longitude = longitude),
            phoneNumber = phone,
            isSample = true,
            id = id,
        ),
        waitMinutes = waitMinutes,
        congestion = congestion,
        acceptingEmergency = accepting,
        updatedMinutesAgo = minutesAgo,
    )
}
