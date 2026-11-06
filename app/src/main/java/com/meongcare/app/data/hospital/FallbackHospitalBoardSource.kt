package com.meongcare.app.data.hospital

import com.meongcare.app.domain.hospital.HospitalBoard
import com.meongcare.app.domain.hospital.HospitalBoardSource
import com.meongcare.app.domain.hospital.HospitalOrigin
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 시연 모드의 병원 목록: 서버에서 받고, [timeout] 안에 받지 못하거나 실패하면 내장 시연 병원으로 대신한다
 * (계획 012 "설계 결정 4"). release(시연)가 네트워크 없이도 끝까지 돌게 하려는 것이다 (문의 A2).
 *
 * 대신한 목록의 출처는 [HospitalOrigin.BUILT_IN_FALLBACK]이다. 화면은 "서버에 연결하지 못해 시연용 병원 정보를
 * 보여 줘요"라고 알리고, 그 병원에는 카드를 서버로 보내지 않는다.
 *
 * 시간 초과 5초는 011 현재 위치와 같은 값이다. 실제로 잰 시간의 3배가 넘으면 올린다 (계획 012 관찰 O3).
 *
 * @param log 결과 종류와 이유만 남긴다 (예: "병원 목록: 내장 시연 (서버 시간 초과)"). 좌표·이름은 넣지 않는다.
 */
class FallbackHospitalBoardSource(
    private val server: HospitalBoardSource,
    private val timeout: Duration = 5.seconds,
    now: () -> Instant = Instant::now,
    private val log: (String) -> Unit = {},
) : HospitalBoardSource {

    private val builtIn = BuiltInHospitalBoardSource(origin = HospitalOrigin.BUILT_IN_FALLBACK, now = now)

    override suspend fun load(): HospitalBoard {
        val result = withTimeoutOrNull(timeout) { server.load() }
        val reason = when (result) {
            null -> "서버 시간 초과"
            is HospitalBoard.Unavailable -> "서버 실패: ${result.reason}"
            is HospitalBoard.Loaded -> return result
        }
        log("병원 목록: 내장 시연 ($reason)")
        return builtIn.load()
    }
}
