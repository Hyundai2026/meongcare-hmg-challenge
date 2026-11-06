package com.meongcare.app.data.guidance

import com.meongcare.app.data.guidance.navi.NAVI_ROUTE_START_TIMEOUT
import com.meongcare.app.data.guidance.navi.NaviClient
import com.meongcare.app.data.guidance.navi.NaviEvent
import com.meongcare.app.data.guidance.navi.RouteOnClose
import com.meongcare.app.data.guidance.navi.naviRemainingTimeOf
import com.meongcare.app.domain.guidance.Destination
import com.meongcare.app.domain.guidance.GuidanceSource
import com.meongcare.app.domain.guidance.GuidanceState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration

/**
 * 시연 모드(차량 기기)의 길안내: **경로만 진짜 내비, 남은 시간과 도착은 시연용** (010, 사용자 결정 2026-11-05).
 *
 * Pleos 에뮬레이터에서는 내비 속 차가 움직이지 않아 진짜 도착이 오지 않는다 (`[obs-navi-emu]`). 그래서:
 * 1. 내비에 경로를 요청한다 (내비 앱이 앞에 나와 안내를 시작한다).
 * 2. 내비가 알려 준 예상 시간(`RouteStarted`)으로 시연용 길안내([fakeOf])를 시작한다 (사용자 결정: 내비 예상 시간에서 시작).
 *    - [routeStartTimeout] 안에 오지 않거나, 그 전에 내비 오류가 오면 시연용 기본 시간으로 시작한다.
 *    - 예상 시간이 모름([naviRemainingTimeOf]가 null)이어도 기본 시간이다.
 * 3. 시연용 길안내가 도착하면 내비 연결을 놓되 **경로는 남긴다** (013 U2). `cancelRoute`는 내비 앱을 앞으로 띄워
 *    우리 "도착했어요"를 가리기 때문이다 (`[obs-navi-emu]` "경로 취소 뒤 앞에 나오는 앱"). 도착 화면 [닫기]도 같다.
 *    남은 경로는 다음 출발의 `requestRoute`가 바꾼다 (이전 경로의 취소 알림은 어댑터가 거른다).
 *    앱의 [안내 취소]와 다른 목적지로 다시 시작할 때는 지금처럼 경로를 취소한다.
 * 4. 내비에서 경로가 취소되면 시연용 길안내도 끝낸다 (안내 없음). 안내가 시작되기 전이어도 같다.
 * 5. 내비의 남은 시간·도착, 안내가 시작된 뒤의 오류는 무시한다. 진행은 시연용이 맡는다.
 *
 * 시연용 길안내는 기존 Fake(`FakeGuidanceSource`)를 안내마다 새로 만들어 쓴다. Fake 코드는 바꾸지 않는다.
 *
 * start/cancel은 화면 쪽 스레드에서, 내비 이벤트와 시연 진행은 scope의 스레드에서 온다. 그래서 상태와 지금 안내([current])는
 * 잠금 안에서만 바꾸고, 끝났거나 바뀐 안내([Trip])에서 늦게 온 것은 버린다.
 *
 * @param fakeOf 처음 남은 시간(null이면 기본값)으로 시연용 길안내를 만든다
 */
class NaviRouteOnlyGuidanceSource(
    private val scope: CoroutineScope,
    private val navi: NaviClient,
    private val fakeOf: (initialRemaining: Duration?) -> GuidanceSource,
    private val routeStartTimeout: Duration = NAVI_ROUTE_START_TIMEOUT,
) : GuidanceSource {

    private val mutableState = MutableStateFlow<GuidanceState>(GuidanceState.Idle)
    override val state: StateFlow<GuidanceState> = mutableState.asStateFlow()

    private val lock = Any()

    /** 지금 안내. 안내마다 새로 만든다 */
    private var current: Trip? = null

    /** 안내 한 번: 내비 경로([naviJob])와 시연 진행([simulation], [fake]) */
    private class Trip(val destination: Destination) {
        /** 내비가 안내를 시작하며 알려 준 예상 시간. 모르거나 내비 오류면 null */
        val routeStarted = CompletableDeferred<Duration?>()

        @Volatile var naviJob: Job? = null

        @Volatile var simulation: Job? = null

        @Volatile var fake: GuidanceSource? = null

        /** 구독을 끊을 때 내비 경로를 남길지. 시연 도착·[닫기]만 true다 (013 U2) */
        @Volatile var keepRouteOnClose = false

        /** 내비 구독을 끊고(경로 취소) 시연 진행을 멈춘다 */
        suspend fun close() {
            naviJob?.cancel()
            simulation?.cancel()
            fake?.cancel()
        }
    }

    override suspend fun start(destination: Destination) {
        // 이전 안내를 먼저 끝낸다: 이전 내비 경로 취소가 새 경로 요청보다 늦게 가지 않게.
        // 그동안 상태는 이전 그대로다 (중간에 안내 없음이 끼지 않는다).
        synchronized(lock) { current.also { current = null } }?.close()
        synchronized(lock) {
            val trip = Trip(destination)
            current = trip
            mutableState.value = GuidanceState.Guiding(destination, remainingTime = null)
            trip.naviJob = scope.launch { followNavi(trip) }
            trip.simulation = scope.launch { simulate(trip) }
        }
    }

    private suspend fun followNavi(trip: Trip) {
        var cancelledOnNavi = false
        navi.trip(trip.destination) { if (trip.keepRouteOnClose) RouteOnClose.KEEP else RouteOnClose.CANCEL }
            .takeWhile { event ->
                when (event) {
                    is NaviEvent.RouteStarted -> trip.routeStarted.complete(naviRemainingTimeOf(event.remainingSeconds))
                    is NaviEvent.Failed -> trip.routeStarted.complete(null) // 시작 전이면 기다리지 않는다. 뒤면 무시
                    NaviEvent.Cancelled -> cancelledOnNavi = true
                    is NaviEvent.Remaining, NaviEvent.Arrived -> Unit // 진행은 시연용이 맡는다
                }
                !cancelledOnNavi
            }
            .collect {}
        if (cancelledOnNavi) endFromNavi(trip)
    }

    /** 내비에서 경로가 취소됐다 → 안내 중이면 시연도 끝낸다 */
    private suspend fun endFromNavi(trip: Trip) {
        synchronized(lock) {
            if (current !== trip || mutableState.value !is GuidanceState.Guiding) return
            current = null
            mutableState.value = GuidanceState.Idle
        }
        trip.simulation?.cancel()
        trip.fake?.cancel()
    }

    private suspend fun simulate(trip: Trip) {
        val initial = withTimeoutOrNull(routeStartTimeout) { trip.routeStarted.await() }
        val demo = fakeOf(initial)
        synchronized(lock) {
            if (current !== trip) return
            trip.fake = demo
        }
        try {
            demo.start(trip.destination)
            demo.state.collect { state ->
                val arrived = synchronized(lock) {
                    if (current !== trip) return@collect
                    // 시연 진행이 안내 없음으로 돌아온 것(취소)은 [cancel]과 [endFromNavi]가 처리한다
                    if (state !is GuidanceState.Idle) mutableState.value = state
                    state is GuidanceState.Arrived
                }
                if (arrived) {
                    // 내비 연결만 놓고 경로는 남긴다 (013 U2): 경로를 취소하면 내비가 앞으로 나와 "도착했어요"를 가린다
                    trip.keepRouteOnClose = true
                    trip.naviJob?.cancel()
                }
            }
        } finally {
            // 이 안내가 끝났는데(취소, 목적지 변경) 시연 진행이 막 시작됐을 수 있다. 남아서 돌지 않게 멈춘다
            if (synchronized(lock) { current !== trip }) withContext(NonCancellable) { demo.cancel() }
        }
    }

    override suspend fun cancel() {
        val ended = synchronized(lock) {
            if (mutableState.value !is GuidanceState.Guiding) return
            mutableState.value = GuidanceState.Idle
            current.also { current = null }
        }
        ended?.close()
    }

    override fun dismissArrival() {
        val ended = synchronized(lock) {
            if (mutableState.value !is GuidanceState.Arrived) return
            mutableState.value = GuidanceState.Idle
            current.also { current = null }
        } ?: return
        ended.fake?.dismissArrival()
        ended.simulation?.cancel()
        // [닫기]도 경로를 남긴다 (013 U2). 도착할 때 이미 연결을 놓았으면 아무 일도 없다
        ended.keepRouteOnClose = true
        ended.naviJob?.cancel()
    }
}
