package com.meongcare.app.data.guidance

import com.meongcare.app.data.guidance.navi.NAVI_ROUTE_START_TIMEOUT
import com.meongcare.app.data.guidance.navi.NaviClient
import com.meongcare.app.data.guidance.navi.NaviEvent
import com.meongcare.app.data.guidance.navi.naviRemainingTimeOf
import com.meongcare.app.domain.guidance.Destination
import com.meongcare.app.domain.guidance.GuidanceSource
import com.meongcare.app.domain.guidance.GuidanceState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration

/**
 * 전부 차량 내비로 하는 길안내 (Real 모드, 차량 기기).
 *
 * - 시작: 경로를 찾는 중 → 내비가 안내를 시작하면 남은 시간 → 남은 시간이 바뀔 때마다 갱신
 * - 도착 이벤트가 오면 도착 (경로는 내비가 끝낸다고 보고 취소하지 않는다)
 * - 내비에서 경로가 취소되면 안내 없음
 * - 내비 오류: 안내가 시작되기 전이면 안내 없음, 시작된 뒤면 무시한다 (010 설계 결정 3의 기본안)
 * - 내비가 [routeStartTimeout] 안에 안내를 시작하지 않으면 안내 없음 ("경로를 찾는 중"에 머물지 않는다)
 * - [cancel]: 안내 한 번을 끝낸다 → [NaviClient.trip] 구독이 끊기며 경로 취소·연결 해제
 *
 * 남은 시간은 [naviRemainingTimeOf]로 바꾸고, 모름(null)이면 이전 남은 시간을 그대로 둔다.
 *
 * start/cancel은 화면 쪽 스레드에서, 내비 이벤트는 scope의 스레드에서 온다. 그래서 `FakeGuidanceSource`처럼
 * 상태와 지금 안내([trip])는 잠금 안에서만 바꾸고, 끝났거나 바뀐 안내의 늦은 이벤트는 버린다.
 */
class NaviGuidanceSource(
    private val scope: CoroutineScope,
    private val navi: NaviClient,
    private val routeStartTimeout: Duration = NAVI_ROUTE_START_TIMEOUT,
) : GuidanceSource {

    private val mutableState = MutableStateFlow<GuidanceState>(GuidanceState.Idle)
    override val state: StateFlow<GuidanceState> = mutableState.asStateFlow()

    private val lock = Any()

    /** 지금 안내. 안내마다 새로 만든다 */
    private var trip: Job? = null

    override suspend fun start(destination: Destination) {
        synchronized(lock) {
            trip?.cancel()
            mutableState.value = GuidanceState.Guiding(destination, remainingTime = null)
            trip = scope.launch { follow(destination) }
        }
    }

    private suspend fun follow(destination: Destination) {
        val me = currentCoroutineContext().job
        val routeStarted = CompletableDeferred<Unit>()
        val watchdog = scope.launch {
            if (withTimeoutOrNull(routeStartTimeout) { routeStarted.await() } == null) {
                // 내비가 응답하지 않는다
                if (end(me, GuidanceState.Idle)) me.cancel()
            }
        }
        try {
            navi.trip(destination)
                .takeWhile { event -> handle(me, destination, event, routeStarted) }
                .collect {}
        } finally {
            watchdog.cancel()
        }
    }

    /** @return 계속 들을지 */
    private fun handle(
        me: Job,
        destination: Destination,
        event: NaviEvent,
        routeStarted: CompletableDeferred<Unit>,
    ): Boolean = synchronized(lock) {
        if (trip !== me) return false // 끝났거나 다른 안내로 바뀌었다
        when (event) {
            is NaviEvent.RouteStarted -> guiding(destination, event.remainingSeconds, routeStarted)
            is NaviEvent.Remaining -> guiding(destination, event.remainingSeconds, routeStarted)
            NaviEvent.Arrived -> !end(me, GuidanceState.Arrived(destination))
            NaviEvent.Cancelled -> !end(me, GuidanceState.Idle)
            is NaviEvent.Failed -> if (routeStarted.isCompleted) true else !end(me, GuidanceState.Idle)
        }
    }

    /** 내비가 안내 중이다. 남은 시간을 모르면 이전 값을 둔다. @return 계속 듣는다 */
    private fun guiding(destination: Destination, seconds: Int, routeStarted: CompletableDeferred<Unit>): Boolean {
        routeStarted.complete(Unit)
        naviRemainingTimeOf(seconds)?.let { mutableState.value = GuidanceState.Guiding(destination, it) }
        return true
    }

    /** 안내 [me]가 아직 지금 안내면 [next]로 끝낸다. @return 끝냈는지 */
    private fun end(me: Job, next: GuidanceState): Boolean = synchronized(lock) {
        if (trip !== me) return false
        trip = null
        mutableState.value = next
        true
    }

    override suspend fun cancel() {
        val ended = synchronized(lock) {
            if (mutableState.value !is GuidanceState.Guiding) return
            mutableState.value = GuidanceState.Idle
            trip.also { trip = null }
        }
        ended?.cancel()
    }

    override fun dismissArrival() {
        synchronized(lock) {
            if (mutableState.value is GuidanceState.Arrived) mutableState.value = GuidanceState.Idle
        }
    }
}
