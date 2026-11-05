package com.meongcare.app.data.guidance

import com.meongcare.app.data.demo.FakeTripScenario
import com.meongcare.app.data.demo.FakeTripTiming
import com.meongcare.app.data.driving.FakeDrivingStateSource
import com.meongcare.app.data.driving.RealDrivingStateSource
import com.meongcare.app.data.driving.UxRestrictionsClient
import com.meongcare.app.data.driving.UxRestrictionsEvent
import com.meongcare.app.domain.driving.DrivingState
import com.meongcare.app.domain.guidance.Destination
import com.meongcare.app.domain.guidance.GuidanceState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * 시연(DEMO) 구성에서 Fake 길안내의 진행 조건이 주행 상태 출처와 맞물리는지 (계획 007 설계 결정 1·3, 확인 절차 P2~P4, D2~D10).
 *
 * 진행 조건은 계획대로 `drivingState.map { it == DrivingState.DRIVING }`로 만든다 (UNKNOWN은 진행하지 않는다).
 * `AppContainer`는 Android `Context`가 필요해서 직접 만들지 않는다. 그래서 Real 주행 상태의 `flowOn(Main)`은 여기서 보지 않는다.
 *
 * - 일반 폰: Fake 주행 상태 + `FakeTripScenario`
 * - 차량 기기: `RealDrivingStateSource` + 차량 서비스 흉내([FakeCarUxClient]). 실제 SDK는 부르지 않는다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FakeGuidanceSourceDemoModeTest {

    private val hospital = Destination(name = "테스트 병원", latitude = 37.0, longitude = 127.0)
    private val otherHospital = Destination(name = "다른 병원", latitude = 37.1, longitude = 127.1)

    // 진행 조건이 계속 참이면: 0초 경로 찾는 중 → 1초 3분 → 11초 2분 → 21초 1분 → 31초 도착
    private val timing = FakeGuidanceTiming(
        routeCalculationDelay = 1.seconds,
        initialRemaining = 3.minutes,
        remainingStep = 1.minutes,
        tickInterval = 10.seconds,
    )
    private val tripTiming = FakeTripTiming(departDelay = 3.seconds, parkDelay = 5.seconds)

    private fun Flow<DrivingState>.isDriving(): Flow<Boolean> = map { it == DrivingState.DRIVING }

    private fun TestScope.advance(time: Duration) {
        advanceTimeBy(time)
        runCurrent()
    }

    /**
     * 차량 서비스 흉내. 연결하면 현재 규제 값이 있으면 한 번 보내고, 값이 바뀌면 연결된 리스너 모두에 보낸다.
     * 연결을 몇 번 맺고 끊었는지 센다 (확인 절차 D10).
     */
    private class FakeCarUxClient(private val failsToConnect: Boolean = false) : UxRestrictionsClient {
        private val listeners = mutableListOf<(UxRestrictionsEvent) -> Unit>()
        private var current: UxRestrictionsEvent? = null

        var connectCount = 0
            private set
        var closeCount = 0
            private set
        val openConnections: Int get() = listeners.size

        override fun connect(onEvent: (UxRestrictionsEvent) -> Unit): AutoCloseable {
            connectCount++
            if (failsToConnect) throw IllegalStateException("차량 서비스에 연결하지 못했다")
            listeners += onEvent
            current?.let(onEvent)
            return AutoCloseable { if (listeners.remove(onEvent)) closeCount++ }
        }

        fun send(event: UxRestrictionsEvent) {
            current = event
            listeners.toList().forEach { it(event) }
        }
    }

    private val drivingRule = UxRestrictionsEvent.Changed(requiresDistractionOptimization = true)
    private val parkedRule = UxRestrictionsEvent.Changed(requiresDistractionOptimization = false)

    // ---- 일반 폰: Fake 주행 상태 + 시나리오 ----

    @Test
    fun `일반 폰 시연 구성은 안내를 시작하면 손대지 않아도 출발하고 도착한 뒤 주차까지 이어진다`() = runTest {
        val driving = FakeDrivingStateSource()
        val guidance = FakeGuidanceSource(backgroundScope, timing, progressGate = driving.drivingState.isDriving())
        FakeTripScenario(backgroundScope, guidance, driving, tripTiming).start()
        runCurrent()

        guidance.start(hospital)
        advance(2.seconds) // 2초: 아직 출발 전
        assertEquals(DrivingState.PARKED, driving.drivingState.value)
        assertEquals(GuidanceState.Guiding(hospital, 3.minutes), guidance.state.value)

        advance(1.seconds) // 3초: 출발
        assertEquals(DrivingState.DRIVING, driving.drivingState.value)

        advance(10.seconds) // 13초: 출발한 뒤 한 간격
        assertEquals(GuidanceState.Guiding(hospital, 2.minutes), guidance.state.value)

        advance(20.seconds) // 33초
        assertEquals(GuidanceState.Arrived(hospital), guidance.state.value)
        assertEquals("주행 중에 도착한다", DrivingState.DRIVING, driving.drivingState.value)

        advance(5.seconds) // 38초: 주차
        assertEquals(DrivingState.PARKED, driving.drivingState.value)
        assertEquals(GuidanceState.Arrived(hospital), guidance.state.value)
    }

    @Test
    fun `일반 폰 시연 구성에서 주행 중 정차로 바꾸면 최대 한 단계 더 줄고 멈췄다가 주행으로 바꾸면 다시 준다`() = runTest {
        val driving = FakeDrivingStateSource()
        val guidance = FakeGuidanceSource(backgroundScope, timing, progressGate = driving.drivingState.isDriving())
        FakeTripScenario(backgroundScope, guidance, driving, tripTiming).start()
        runCurrent()
        guidance.start(hospital)
        advance(5.seconds) // 3초에 출발, 3~13초 간격을 세는 중

        driving.set(DrivingState.PARKED) // debug [정차]
        advance(8.seconds) // 13초: 세던 간격까지 준다
        assertEquals(GuidanceState.Guiding(hospital, 2.minutes), guidance.state.value)
        advance(30.seconds) // 43초
        assertEquals(GuidanceState.Guiding(hospital, 2.minutes), guidance.state.value)

        driving.set(DrivingState.DRIVING) // debug [주행]
        advance(10.seconds) // 53초
        assertEquals(GuidanceState.Guiding(hospital, 1.minutes), guidance.state.value)
        advance(10.seconds) // 63초
        assertEquals(GuidanceState.Arrived(hospital), guidance.state.value)
        advance(5.seconds) // 68초
        assertEquals(DrivingState.PARKED, driving.drivingState.value)
    }

    @Test
    fun `일반 폰 시연 구성에서 주행 상태가 모름이면 남은 시간이 줄지 않는다`() = runTest {
        val driving = FakeDrivingStateSource(initial = DrivingState.UNKNOWN)
        val guidance = FakeGuidanceSource(backgroundScope, timing, progressGate = driving.drivingState.isDriving())

        guidance.start(hospital)
        advance(1.minutes)

        assertEquals(GuidanceState.Guiding(hospital, 3.minutes), guidance.state.value)
    }

    // ---- 차량 기기: Real 주행 상태 ----

    @Test
    fun `차량 기기 시연 구성은 규제 값이 오기 전이나 정차 규제에서는 남은 시간이 줄지 않고 주행 규제가 오면 준다`() = runTest {
        val client = FakeCarUxClient()
        val gate = RealDrivingStateSource(client).drivingState.isDriving()
        val guidance = FakeGuidanceSource(backgroundScope, timing, progressGate = gate)

        guidance.start(hospital)
        advance(1.minutes)
        assertEquals("규제 값이 오기 전(모름)", GuidanceState.Guiding(hospital, 3.minutes), guidance.state.value)

        client.send(parkedRule)
        advance(1.minutes)
        assertEquals("정차 규제", GuidanceState.Guiding(hospital, 3.minutes), guidance.state.value)

        client.send(drivingRule)
        advance(10.seconds)
        assertEquals(GuidanceState.Guiding(hospital, 2.minutes), guidance.state.value)
    }

    @Test
    fun `차량 기기 시연 구성에서 주행 중 규제 값을 알 수 없게 되면 그 간격까지만 줄고 멈춘다`() = runTest {
        val client = FakeCarUxClient()
        client.send(drivingRule)
        val guidance = FakeGuidanceSource(backgroundScope, timing, RealDrivingStateSource(client).drivingState.isDriving())
        guidance.start(hospital)
        advance(5.seconds) // 1~11초 간격을 세는 중

        client.send(UxRestrictionsEvent.Unavailable)
        advance(6.seconds) // 11초
        assertEquals(GuidanceState.Guiding(hospital, 2.minutes), guidance.state.value)

        advance(1.minutes)
        assertEquals(GuidanceState.Guiding(hospital, 2.minutes), guidance.state.value)
    }

    @Test
    fun `차량 기기 시연 구성에서 차량 연결에 실패해도 경로 계산은 끝나고 남은 시간은 줄지 않는다`() = runTest {
        val client = FakeCarUxClient(failsToConnect = true)
        val guidance = FakeGuidanceSource(backgroundScope, timing, RealDrivingStateSource(client).drivingState.isDriving())

        guidance.start(hospital)
        advance(1.seconds)
        assertEquals(GuidanceState.Guiding(hospital, 3.minutes), guidance.state.value)
        advance(5.minutes)
        assertEquals(GuidanceState.Guiding(hospital, 3.minutes), guidance.state.value)

        guidance.cancel()
        assertEquals(GuidanceState.Idle, guidance.state.value)
    }

    @Test
    fun `차량 기기 시연 구성은 안내 한 번에 차량 연결을 한 번만 맺고 도착하면 끊는다`() = runTest {
        val client = FakeCarUxClient()
        client.send(drivingRule)
        val guidance = FakeGuidanceSource(backgroundScope, timing, RealDrivingStateSource(client).drivingState.isDriving())
        runCurrent()
        assertEquals("안내 전에는 연결하지 않는다", 0, client.connectCount)

        guidance.start(hospital)
        advance(1.seconds) // 1초: 3분, 1~11초 간격을 세는 중
        assertEquals(1, client.connectCount)
        assertEquals(1, client.openConnections)

        client.send(parkedRule) // P
        advance(20.seconds) // 11초에 2분, 21초까지 멈춤
        client.send(drivingRule) // D
        advance(20.seconds) // 31초에 1분, 41초 도착

        assertEquals(GuidanceState.Arrived(hospital), guidance.state.value)
        assertEquals("멈췄다 가도 다시 연결하지 않는다", 1, client.connectCount)
        assertEquals("도착하면 연결을 끊는다", 0, client.openConnections)
        assertEquals(1, client.closeCount)

        guidance.dismissArrival()
        advance(1.minutes)
        assertEquals("도착 화면을 닫아도 다시 연결하지 않는다", 1, client.connectCount)
    }

    @Test
    fun `차량 기기 시연 구성에서 다른 목적지로 바꾸거나 취소하면 이전 차량 연결을 끊는다`() = runTest {
        val client = FakeCarUxClient()
        client.send(parkedRule)
        val guidance = FakeGuidanceSource(backgroundScope, timing, RealDrivingStateSource(client).drivingState.isDriving())
        guidance.start(hospital)
        advance(5.seconds)
        assertEquals(1, client.openConnections)

        guidance.start(otherHospital)
        advance(1.seconds)
        assertEquals(GuidanceState.Guiding(otherHospital, 3.minutes), guidance.state.value)
        assertEquals("새 안내 연결 하나만 남는다", 1, client.openConnections)
        assertEquals(2, client.connectCount)
        assertEquals(1, client.closeCount)

        guidance.cancel()
        runCurrent()
        assertEquals(GuidanceState.Idle, guidance.state.value)
        assertEquals("취소하면 연결을 끊는다", 0, client.openConnections)
        assertEquals(2, client.closeCount)
    }
}
