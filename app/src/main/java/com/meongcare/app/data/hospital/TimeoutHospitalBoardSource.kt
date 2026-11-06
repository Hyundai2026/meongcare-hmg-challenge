package com.meongcare.app.data.hospital

import com.meongcare.app.domain.hospital.HospitalBoard
import com.meongcare.app.domain.hospital.HospitalBoardSource
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Real 모드의 병원 목록: [timeout] 안에 받지 못하면 "받지 못했어요"다 (대체하지 않는다).
 * 계획 012 "설계 결정 8"의 "위치와 목록 둘 다 5초 시간 초과". 시연 모드는 [FallbackHospitalBoardSource]가 같은 시간을 쓴다.
 */
class TimeoutHospitalBoardSource(
    private val source: HospitalBoardSource,
    private val timeout: Duration = 5.seconds,
    private val log: (String) -> Unit = {},
) : HospitalBoardSource {

    override suspend fun load(): HospitalBoard =
        withTimeoutOrNull(timeout) { source.load() } ?: HospitalBoard.Unavailable("시간 초과").also {
            log("병원 목록: 서버 시간 초과")
        }
}
