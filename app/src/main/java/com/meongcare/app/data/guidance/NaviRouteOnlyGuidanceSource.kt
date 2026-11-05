package com.meongcare.app.data.guidance

import com.meongcare.app.data.guidance.navi.NaviClient
import com.meongcare.app.data.guidance.navi.NaviEvent
import com.meongcare.app.domain.guidance.Destination
import com.meongcare.app.domain.guidance.GuidanceSource
import com.meongcare.app.domain.guidance.GuidanceState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * 시연 모드(차량 기기)의 길안내: **경로만 진짜 내비, 남은 시간과 도착은 시연용** (010, 사용자 결정 2026-11-05).
 *
 * Pleos 에뮬레이터에서는 내비 속 차가 움직이지 않아 진짜 도착이 오지 않는다 (`[obs-navi-emu]`). 그래서:
 * 1. 내비에 경로를 요청한다 (내비 앱이 앞에 나와 안내를 시작한다).
 * 2. 내비가 알려 준 예상 시간(`RouteStarted`)으로 시연용 길안내([fakeOf])를 시작한다 (사용자 결정: 내비 예상 시간에서 시작).
 *    [routeStartTimeout] 안에 오지 않으면 시연용 기본 시간으로 시작한다.
 * 3. 시연용 길안내가 도착하면 내비 경로도 취소한다 (안내 한 번을 끝낸다).
 *
 * 시연용 길안내는 기존 Fake(`FakeGuidanceSource`)를 안내마다 새로 만들어 쓴다. Fake 코드는 바꾸지 않는다.
 *
 * @param fakeOf 처음 남은 시간(null이면 기본값)으로 시연용 길안내를 만든다
 */
class NaviRouteOnlyGuidanceSource(
    private val scope: CoroutineScope,
    private val navi: NaviClient,
    private val fakeOf: (initialRemaining: Duration?) -> GuidanceSource,
    private val routeStartTimeout: Duration = DEFAULT_ROUTE_START_TIMEOUT,
) : GuidanceSource {

    private val mutableState = MutableStateFlow<GuidanceState>(GuidanceState.Idle)
    override val state: StateFlow<GuidanceState> = mutableState.asStateFlow()

    private var trip: Job? = null
    private var simulation: Job? = null
    private var fake: GuidanceSource? = null

    override suspend fun start(destination: Destination) {
        stop()
        mutableState.value = GuidanceState.Guiding(destination, remainingTime = null)
        val events = navi.trip(destination)
        val routeStarted = CompletableDeferred<Int?>()
        trip = scope.launch {
            events.collect { event ->
                if (event is NaviEvent.RouteStarted) routeStarted.complete(event.remainingSeconds.takeIf { it > 0 })
            }
        }
        simulation = scope.launch {
            val seconds = withTimeoutOrNull(routeStartTimeout) { routeStarted.await() }
            val demo = fakeOf(seconds?.seconds)
            fake = demo
            demo.start(destination)
            demo.state.collect { state ->
                // 시연 길안내가 Idle로 돌아온 것(취소)은 [cancel]이 처리한다
                if (state !is GuidanceState.Idle) mutableState.value = state
                if (state is GuidanceState.Arrived) {
                    trip?.cancel() // 내비 경로도 끝낸다
                    trip = null
                }
            }
        }
    }

    override suspend fun cancel() {
        if (mutableState.value !is GuidanceState.Guiding) return
        stop()
        mutableState.value = GuidanceState.Idle
    }

    override fun dismissArrival() {
        if (mutableState.value !is GuidanceState.Arrived) return
        fake?.dismissArrival()
        simulation?.cancel()
        simulation = null
        fake = null
        mutableState.value = GuidanceState.Idle
    }

    private suspend fun stop() {
        trip?.cancel()
        trip = null
        fake?.cancel()
        simulation?.cancel()
        simulation = null
        fake = null
    }

    companion object {
        /** 내비가 안내를 시작하기까지 (카운트다운 약 3초) 기다리는 시간 */
        val DEFAULT_ROUTE_START_TIMEOUT: Duration = 10.seconds
    }
}
