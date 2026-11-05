package com.meongcare.app.data.guidance

import com.meongcare.app.data.guidance.navi.NaviClient
import com.meongcare.app.data.guidance.navi.NaviEvent
import com.meongcare.app.domain.guidance.Destination
import com.meongcare.app.domain.guidance.GuidanceSource
import com.meongcare.app.domain.guidance.GuidanceState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

/**
 * 전부 차량 내비로 하는 길안내 (Real 모드, 차량 기기).
 *
 * - 시작: 경로를 찾는 중 → 내비가 안내를 시작하면 남은 시간 → 남은 시간이 바뀔 때마다 갱신
 * - 도착 이벤트가 오면 도착 (경로는 내비가 끝낸다고 보고 취소하지 않는다)
 * - 내비에서 경로가 취소되거나 연결에 실패하면 안내 없음
 * - [cancel]: 안내 한 번을 끝낸다 → [NaviClient.trip] 구독이 끊기며 경로 취소·연결 해제
 *
 * 남은 시간이 0 이하이면 쓰지 않는다 (취소 직전에 0이 온다, `[obs-navi-emu]`).
 */
class NaviGuidanceSource(
    private val scope: CoroutineScope,
    private val navi: NaviClient,
) : GuidanceSource {

    private val mutableState = MutableStateFlow<GuidanceState>(GuidanceState.Idle)
    override val state: StateFlow<GuidanceState> = mutableState.asStateFlow()

    private var trip: Job? = null

    override suspend fun start(destination: Destination) {
        trip?.cancel()
        mutableState.value = GuidanceState.Guiding(destination, remainingTime = null)
        trip = scope.launch {
            navi.trip(destination)
                .takeWhile { event -> handle(destination, event) }
                .collect {}
        }
    }

    /** @return 계속 들을지 */
    private fun handle(destination: Destination, event: NaviEvent): Boolean = when (event) {
        is NaviEvent.RouteStarted -> {
            event.remainingSeconds.takeIf { it > 0 }?.let { mutableState.value = GuidanceState.Guiding(destination, it.seconds) }
            true
        }
        is NaviEvent.Remaining -> {
            event.remainingSeconds.takeIf { it > 0 }?.let { mutableState.value = GuidanceState.Guiding(destination, it.seconds) }
            true
        }
        NaviEvent.Arrived -> {
            mutableState.value = GuidanceState.Arrived(destination)
            false
        }
        NaviEvent.Cancelled, is NaviEvent.Failed -> {
            mutableState.value = GuidanceState.Idle
            false
        }
    }

    override suspend fun cancel() {
        if (mutableState.value !is GuidanceState.Guiding) return
        trip?.cancel()
        trip = null
        mutableState.value = GuidanceState.Idle
    }

    override fun dismissArrival() {
        if (mutableState.value is GuidanceState.Arrived) mutableState.value = GuidanceState.Idle
    }
}
