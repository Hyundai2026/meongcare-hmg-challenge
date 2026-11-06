package com.meongcare.app.data.hospital

import com.meongcare.app.domain.hospital.HospitalBoard
import com.meongcare.app.domain.hospital.HospitalBoardSource
import com.meongcare.app.domain.hospital.HospitalOrigin
import java.time.Instant

/**
 * 앱에 넣어 둔 시연용 제휴 병원 5곳 ([SampleHospitals.partners]). 네트워크 없이 바로 준다.
 *
 * 알린 시각은 받을 때마다 `지금 - N분`으로 만든다. 그래서 "정보 지난" 병원은 늘 지난 정보다.
 *
 * @param origin [HospitalOrigin.BUILT_IN] (처음부터 내장) 또는 [HospitalOrigin.BUILT_IN_FALLBACK] (서버 대신)
 */
class BuiltInHospitalBoardSource(
    private val origin: HospitalOrigin = HospitalOrigin.BUILT_IN,
    private val now: () -> Instant = Instant::now,
) : HospitalBoardSource {

    init {
        require(origin != HospitalOrigin.SERVER) { "내장 목록은 서버 출처가 될 수 없다" }
    }

    override suspend fun load(): HospitalBoard {
        val at = now()
        return HospitalBoard.Loaded(
            candidates = SampleHospitals.partners.map { it.candidateAt(at, origin) },
            origin = origin,
        )
    }
}
