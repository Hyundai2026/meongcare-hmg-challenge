package com.meongcare.app.data.demo

import com.meongcare.app.data.driving.FakeDrivingStateSource
import com.meongcare.app.domain.driving.DrivingState
import com.meongcare.app.domain.guidance.GuidanceSource
import com.meongcare.app.domain.guidance.GuidanceState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** 시연 시나리오의 시간 설정. */
data class FakeTripTiming(
    /** 안내를 시작하고 차가 출발(주행)하기까지 */
    val departDelay: Duration = 3.seconds,
    /** 도착하고 차가 주차(정차)하기까지 */
    val parkDelay: Duration = 5.seconds,
)

/**
 * FAKE 모드 전용. 길안내 상태에 맞춰 Fake 주행 상태를 바꿔서, "출발 → 주행 → 도착 → 주차"를 흉내 낸다.
 * 시연 영상을 찍을 때 debug 패널을 누르지 않아도 흐름이 이어지게 하려는 것이다.
 *
 * - 안내가 시작되면 [FakeTripTiming.departDelay] 뒤에 주행.
 * - 도착하면 [FakeTripTiming.parkDelay] 뒤에 정차.
 * - 안내를 취소하면 주행 상태를 그대로 둔다. 실제로도 안내를 취소해도 차는 계속 달린다.
 * - 기다리는 중에 길안내 상태가 바뀌면 기다리던 전환은 하지 않는다.
 *
 * Real에서는 주행 상태(차량)와 길안내(내비 앱)가 서로 모르는 출처다.
 * 그래서 두 Fake는 각자 Real과 같은 책임만 지고, 잇는 일은 이 클래스만 한다.
 */
class FakeTripScenario(
    private val scope: CoroutineScope,
    private val guidanceSource: GuidanceSource,
    private val drivingStateSource: FakeDrivingStateSource,
    private val timing: FakeTripTiming = FakeTripTiming(),
) {
    private var job: Job? = null

    /** 시나리오를 시작한다. 여러 번 불러도 한 번만 시작한다. */
    fun start() {
        if (job != null) return
        job = scope.launch {
            guidanceSource.state
                // 남은 시간이 바뀌는 것은 무시하고, 안내 단계가 바뀔 때만 반응한다.
                .map { it.phase() }
                .distinctUntilChanged()
                .collectLatest { phase ->
                    when (phase) {
                        Phase.GUIDING -> {
                            delay(timing.departDelay)
                            drivingStateSource.set(DrivingState.DRIVING)
                        }
                        Phase.ARRIVED -> {
                            delay(timing.parkDelay)
                            drivingStateSource.set(DrivingState.PARKED)
                        }
                        Phase.IDLE -> Unit
                    }
                }
        }
    }

    private enum class Phase { IDLE, GUIDING, ARRIVED }

    private fun GuidanceState.phase(): Phase = when (this) {
        GuidanceState.Idle -> Phase.IDLE
        is GuidanceState.Guiding -> Phase.GUIDING
        is GuidanceState.Arrived -> Phase.ARRIVED
    }
}
