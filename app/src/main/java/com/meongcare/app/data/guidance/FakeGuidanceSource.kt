package com.meongcare.app.data.guidance

import com.meongcare.app.domain.guidance.Destination
import com.meongcare.app.domain.guidance.GuidanceSource
import com.meongcare.app.domain.guidance.GuidanceState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
 */
class FakeGuidanceSource(
    private val scope: CoroutineScope,
    private val timing: FakeGuidanceTiming = FakeGuidanceTiming(),
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
        delay(timing.routeCalculationDelay)
        var remaining = timing.initialRemaining
        while (remaining.isPositive()) {
            publish(GuidanceState.Guiding(destination, remaining))
            delay(timing.tickInterval)
            remaining -= timing.remainingStep
        }
        publish(GuidanceState.Arrived(destination))
    }

    /** 이 진행이 이미 취소됐으면 상태를 바꾸지 않는다. */
    private fun CoroutineScope.publish(newState: GuidanceState) {
        synchronized(lock) {
            ensureActive()
            mutableState.value = newState
        }
    }
}
