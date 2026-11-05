package com.meongcare.app.data.guidance

import android.util.Log
import com.meongcare.app.domain.guidance.Destination
import com.meongcare.app.domain.guidance.GuidanceSource
import com.meongcare.app.domain.guidance.GuidanceState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * 실제 차량 내비게이션 길안내. **아직 자리표시다.** 항상 [GuidanceState.Idle]이고, 요청은 로그만 남긴다.
 *
 * 후속 계획에서 Pleos NaviHelper로 구현한다. 그 전에 확인할 것은
 * docs/plans 005 "후속 계획(Real 길안내)으로 넘기는 질문"에 있다
 * (`TripInfo.eta`의 단위와 뜻, 좌표만으로 경로 요청이 되는지, 에뮬레이터에서 이벤트가 오는지 등).
 *
 * @param log 로그 함수. JVM 단위 테스트에서는 `android.util.Log`를 부를 수 없어서 바꿔 끼운다.
 */
class RealGuidanceSource(
    private val log: (String) -> Unit = { Log.w(TAG, it) },
) : GuidanceSource {

    override val state: Flow<GuidanceState> = flowOf(GuidanceState.Idle)

    override suspend fun start(destination: Destination) {
        log("Real 길안내는 아직 구현 전이다. 요청 무시: ${destination.name}")
    }

    override suspend fun cancel() {
        log("Real 길안내는 아직 구현 전이다. 취소 요청 무시")
    }

    override fun dismissArrival() = Unit

    private companion object {
        const val TAG = "RealGuidanceSource"
    }
}
