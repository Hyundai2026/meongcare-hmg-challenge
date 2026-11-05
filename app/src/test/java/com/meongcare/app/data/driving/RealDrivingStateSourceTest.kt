package com.meongcare.app.data.driving

import app.cash.turbine.test
import com.meongcare.app.domain.driving.DrivingState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Real 주행 상태. 규제 연결이 없는 기기(인자 없이 만든 경우)에서는 UNKNOWN만 내보낸다.
 * 규제 연결이 있으면 받은 규제 값으로 상태를 정한다 (계획 006 "설계 결정 2").
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RealDrivingStateSourceTest {

    // ---- 규제 연결이 없는 기기 (일반 폰) ----

    @Test
    fun `구독하면 모름 상태를 먼저 내보낸다`() = runTest {
        RealDrivingStateSource().drivingState.test {
            assertEquals(DrivingState.UNKNOWN, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `시간이 지나도 모름 말고 다른 상태는 내보내지 않는다`() = runTest {
        val received = mutableListOf<DrivingState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            RealDrivingStateSource().drivingState.toList(received)
        }

        advanceTimeBy(60_000)
        runCurrent()

        // 같은 상태를 연달아 내보내지 않는다는 계약(DrivingStateSource KDoc)까지 포함해 정확히 한 번
        assertEquals(listOf(DrivingState.UNKNOWN), received)
    }

    @Test
    fun `구독을 끊었다가 다시 구독해도 모름 상태를 내보낸다`() = runTest {
        val source = RealDrivingStateSource()

        source.drivingState.test {
            assertEquals(DrivingState.UNKNOWN, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        source.drivingState.test {
            assertEquals(DrivingState.UNKNOWN, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ---- 규제 연결이 있는 기기 (차량) ----

    @Test
    fun `연결해도 규제 값이 오기 전에는 모름을 먼저 내보낸다`() = runTest {
        val client = RecordingClient()

        RealDrivingStateSource(client).drivingState.test {
            assertEquals(DrivingState.UNKNOWN, awaitItem())
            assertEquals(1, client.connectCount)
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `주행 최적화가 필요하다는 규제가 오면 주행을 내보낸다`() = runTest {
        val client = RecordingClient()

        RealDrivingStateSource(client).drivingState.test {
            assertEquals(DrivingState.UNKNOWN, awaitItem())
            client.send(UxRestrictionsEvent.Changed(requiresDistractionOptimization = true))
            assertEquals(DrivingState.DRIVING, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `주행 최적화가 필요 없다는 규제가 오면 정차를 내보낸다`() = runTest {
        val client = RecordingClient()

        RealDrivingStateSource(client).drivingState.test {
            assertEquals(DrivingState.UNKNOWN, awaitItem())
            client.send(UxRestrictionsEvent.Changed(requiresDistractionOptimization = false))
            assertEquals(DrivingState.PARKED, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `규제 값을 알 수 없게 되면 모름을 내보낸다`() = runTest {
        val client = RecordingClient()

        RealDrivingStateSource(client).drivingState.test {
            assertEquals(DrivingState.UNKNOWN, awaitItem())
            client.send(UxRestrictionsEvent.Changed(requiresDistractionOptimization = false))
            assertEquals(DrivingState.PARKED, awaitItem())
            client.send(UxRestrictionsEvent.Unavailable)
            assertEquals(DrivingState.UNKNOWN, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `연결이 끊겼다가 다시 규제 값이 오면 그 상태를 내보낸다`() = runTest {
        val client = RecordingClient()

        RealDrivingStateSource(client).drivingState.test {
            assertEquals(DrivingState.UNKNOWN, awaitItem())
            client.send(UxRestrictionsEvent.Changed(requiresDistractionOptimization = true))
            assertEquals(DrivingState.DRIVING, awaitItem())
            client.send(UxRestrictionsEvent.Unavailable)
            assertEquals(DrivingState.UNKNOWN, awaitItem())
            client.send(UxRestrictionsEvent.Changed(requiresDistractionOptimization = true))
            assertEquals(DrivingState.DRIVING, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `같은 규제 값이 연달아 와도 한 번만 내보낸다`() = runTest {
        val client = RecordingClient()

        RealDrivingStateSource(client).drivingState.test {
            assertEquals(DrivingState.UNKNOWN, awaitItem())
            client.send(UxRestrictionsEvent.Changed(requiresDistractionOptimization = true))
            client.send(UxRestrictionsEvent.Changed(requiresDistractionOptimization = true))
            assertEquals(DrivingState.DRIVING, awaitItem())
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `구독을 끊으면 차량 연결을 닫는다`() = runTest {
        val client = RecordingClient()

        RealDrivingStateSource(client).drivingState.test {
            assertEquals(DrivingState.UNKNOWN, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        runCurrent()

        assertEquals(1, client.connectCount)
        assertEquals(1, client.closeCount)
    }

    @Test
    fun `다시 구독하면 새로 연결한다`() = runTest {
        val client = RecordingClient()
        val source = RealDrivingStateSource(client)

        source.drivingState.test {
            assertEquals(DrivingState.UNKNOWN, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        source.drivingState.test {
            assertEquals(DrivingState.UNKNOWN, awaitItem())
            client.send(UxRestrictionsEvent.Changed(requiresDistractionOptimization = false))
            assertEquals(DrivingState.PARKED, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        runCurrent()

        assertEquals(2, client.connectCount)
        assertEquals(2, client.closeCount)
    }

    @Test
    fun `연결 중에 예외가 나면 모름을 내보내고 흐름이 끝나지 않는다`() = runTest {
        val client = RecordingClient(failOnConnect = true)

        RealDrivingStateSource(client).drivingState.test {
            assertEquals(DrivingState.UNKNOWN, awaitItem())
            // 흐름이 예외나 완료로 끝나면 여기서 이벤트가 잡힌다
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(1, client.connectCount)
    }
}

/** 연결과 닫기 횟수를 세고, 테스트가 규제 값을 직접 보내는 가짜 창구. */
private class RecordingClient(
    private val failOnConnect: Boolean = false,
) : UxRestrictionsClient {
    var connectCount = 0
        private set
    var closeCount = 0
        private set
    private var listener: ((UxRestrictionsEvent) -> Unit)? = null

    override fun connect(onEvent: (UxRestrictionsEvent) -> Unit): AutoCloseable {
        connectCount++
        if (failOnConnect) throw IllegalStateException("차량 서비스 연결 실패 (테스트)")
        listener = onEvent
        return AutoCloseable {
            closeCount++
            listener = null
        }
    }

    fun send(event: UxRestrictionsEvent) {
        val l = checkNotNull(listener) { "연결되지 않았다" }
        l(event)
    }
}
