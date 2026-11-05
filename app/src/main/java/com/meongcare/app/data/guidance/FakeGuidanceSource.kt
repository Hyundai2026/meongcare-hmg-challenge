package com.meongcare.app.data.guidance

import com.meongcare.app.domain.guidance.Destination
import com.meongcare.app.domain.guidance.GuidanceSource
import com.meongcare.app.domain.guidance.GuidanceState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Fake 길안내의 시간 설정. 기본값은 시연 영상용이다 (안내 시작 후 약 25초에 도착).
 *
 * 흐름 (기본값): 0초 경로 찾는 중 → 1초 12분 → 3초 11분 → … → 23초 1분 → 25초 도착.
 * "0분"은 내보내지 않는다.
 */
data class FakeGuidanceTiming(
    /** "경로를 찾는 중"(남은 시간 `null`)을 보여주는 시간 */
    val routeCalculationDelay: Duration = 1.seconds,
    /** 처음 보여주는 남은 시간 */
    val initialRemaining: Duration = 12.minutes,
    /** 한 번에 줄어드는 남은 시간 */
    val remainingStep: Duration = 1.minutes,
    /** 남은 시간이 줄어드는 간격 (실제로 흐르는 시간) */
    val tickInterval: Duration = 2.seconds,
) {
    init {
        require(initialRemaining.isPositive()) { "initialRemaining은 0보다 커야 한다" }
        require(remainingStep.isPositive()) { "remainingStep은 0보다 커야 한다" }
    }
}

/**
 * 시간이 지나면 남은 시간이 줄고 도착하는 Fake 길안내.
 *
 * - 안내 진행은 [scope]에서 돈다. 테스트는 `runTest`의 `backgroundScope`를 넘겨 가상 시간으로 돌린다.
 * - 주행 상태는 바꾸지 않는다. Real(NaviHelper)도 주행 상태를 모르기 때문이다.
 *   시연용으로 둘을 잇는 일은 `FakeTripScenario`가 한다.
 *
 * @param progressGate 진행 조건. `null`이면 시간만으로 흐른다. 값이 있으면 `true`인 동안에만 남은 시간이 준다
 *   (시연 모드: "주행 중"). 실제 내비게이션도 차가 서 있으면 남은 시간이 줄지 않는다. 계약:
 *   - 경로 계산("경로를 찾는 중" → 처음 남은 시간)은 조건과 상관없이 끝난다.
 *   - 간격([FakeGuidanceTiming.tickInterval])을 하나 세기 전에 조건이 `true`가 될 때까지 기다린다.
 *     세는 도중에 `false`가 되면 그 간격은 끝까지 센다. 그래서 멈추면 최대 한 단계까지 더 줄고 멈춘다.
 *   - 도착도 마지막 간격이 지나야 오므로 조건이 `true`일 때만 온다.
 *   - 안내 한 번(시작 → 도착·취소·다른 목적지로 다시 시작) 동안 조건을 한 번만 구독하고, 끝나면 구독을 끊는다.
 *     간격마다 새로 구독하면 Real 주행 상태는 그때마다 차량 연결을 새로 맺기 때문이다.
 */
class FakeGuidanceSource(
    private val scope: CoroutineScope,
    private val timing: FakeGuidanceTiming = FakeGuidanceTiming(),
    private val progressGate: Flow<Boolean>? = null,
) : GuidanceSource {

    private val mutableState = MutableStateFlow<GuidanceState>(GuidanceState.Idle)
    override val state: StateFlow<GuidanceState> = mutableState.asStateFlow()

    // start/cancel은 화면 쪽 스레드에서, 진행은 scope의 스레드에서 상태를 바꾼다.
    // 취소한 뒤에 이전 진행이 상태를 덮어쓰지 않게, 상태 변경은 이 잠금 안에서만 한다.
    private val lock = Any()
    private var progress: Job? = null

    override suspend fun start(destination: Destination) {
        synchronized(lock) {
            progress?.cancel()
            mutableState.value = GuidanceState.Guiding(destination, remainingTime = null)
            progress = scope.launch { runGuidance(destination) }
        }
    }

    override suspend fun cancel() {
        synchronized(lock) {
            if (mutableState.value !is GuidanceState.Guiding) return
            progress?.cancel()
            progress = null
            mutableState.value = GuidanceState.Idle
        }
    }

    override fun dismissArrival() {
        synchronized(lock) {
            if (mutableState.value is GuidanceState.Arrived) {
                mutableState.value = GuidanceState.Idle
            }
        }
    }

    private suspend fun CoroutineScope.runGuidance(destination: Destination) {
        // 진행 조건은 이 안내 동안 한 번만 구독한다. 구독은 이 진행의 자식이라 취소되면 같이 끝난다.
        val gateOpen = MutableStateFlow(false)
        val gateSubscription: Job? = progressGate?.let { gate ->
            launch { gate.collect { gateOpen.value = it } }
        }
        try {
            delay(timing.routeCalculationDelay)
            var remaining = timing.initialRemaining
            while (remaining.isPositive()) {
                publish(GuidanceState.Guiding(destination, remaining))
                if (gateSubscription != null) gateOpen.first { it }
                delay(timing.tickInterval)
                remaining -= timing.remainingStep
            }
            publish(GuidanceState.Arrived(destination))
        } finally {
            // 도착한 뒤에도 구독이 남아 있지 않게 끊는다.
            gateSubscription?.cancel()
        }
    }

    /** 이 진행이 이미 취소됐으면 상태를 바꾸지 않는다. */
    private fun CoroutineScope.publish(newState: GuidanceState) {
        synchronized(lock) {
            ensureActive()
            mutableState.value = newState
        }
    }
}
