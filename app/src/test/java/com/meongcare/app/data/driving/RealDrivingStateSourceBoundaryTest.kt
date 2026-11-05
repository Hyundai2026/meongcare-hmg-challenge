package com.meongcare.app.data.driving

import app.cash.turbine.test
import com.meongcare.app.domain.driving.DrivingState
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 계획 006 단계 5: Real 주행 상태의 경계.
 *
 * 기준:
 * - 설계 결정 2의 매핑 표 (구독 직후 UNKNOWN, DO true = DRIVING, false = PARKED, 알 수 없음·연결 실패 = UNKNOWN)
 * - `DrivingStateSource` 계약: 구독하면 현재 상태를 먼저, 같은 상태를 연달아 내보내지 않는다
 * - 설계 결정 3: 구독할 때 연결, 구독이 끝나면 close 한 번, 다시 구독하면 새 연결, 구독자마다 연결 하나
 * - 설계 결정 4 스레드: connect/close는 구독한 스레드(메인)에서, 콜백 스레드에는 기대지 않는다
 * - `connect`/`close`가 던져도 흐름이 예외로 끝나지 않는다
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RealDrivingStateSourceBoundaryTest {

    // ---- 연결 시점 ----

    @Test
    fun `만들기만 하고 구독하지 않으면 차량에 연결하지 않는다`() = runTest {
        val client = ScriptedUxClient()
        val source = RealDrivingStateSource(client)

        source.drivingState
        runCurrent()

        assertEquals(0, client.connectCount)
    }

    @Test
    fun `연결하자마자 현재 규제 값을 보내도 그 값을 놓치지 않는다`() = runTest {
        // 어댑터는 연결되면 현재 값을 한 번 보낸다 (UxRestrictionsClient KDoc). connect 안에서 바로 올 수도 있다.
        val client = ScriptedUxClient(onConnect = { send -> send(UxRestrictionsEvent.Changed(false)) })
        val received = mutableListOf<DrivingState>()

        backgroundScope.launch { RealDrivingStateSource(client).drivingState.toList(received) }
        runCurrent()

        assertEquals(DrivingState.PARKED, received.last())
        assertTrue(
            "모름이 먼저 올 수는 있지만 다른 값이 끼면 안 된다: $received",
            received == listOf(DrivingState.UNKNOWN, DrivingState.PARKED) ||
                received == listOf(DrivingState.PARKED),
        )
    }

    // ---- 같은 상태를 연달아 내보내지 않는다 ----

    @Test
    fun `구독 직후 규제 값을 알 수 없다고 와도 모름을 한 번만 내보낸다`() = runTest {
        val client = ScriptedUxClient()

        RealDrivingStateSource(client).drivingState.test {
            assertEquals(DrivingState.UNKNOWN, awaitItem())
            client.send(UxRestrictionsEvent.Unavailable)
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `알 수 없음이 연달아 와도 모름을 한 번만 내보낸다`() = runTest {
        val client = ScriptedUxClient()

        RealDrivingStateSource(client).drivingState.test {
            assertEquals(DrivingState.UNKNOWN, awaitItem())
            client.send(UxRestrictionsEvent.Changed(true))
            assertEquals(DrivingState.DRIVING, awaitItem())
            client.send(UxRestrictionsEvent.Unavailable)
            client.send(UxRestrictionsEvent.Unavailable)
            assertEquals(DrivingState.UNKNOWN, awaitItem())
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `정차 규제 값이 연달아 와도 정차를 한 번만 내보낸다`() = runTest {
        val client = ScriptedUxClient()

        RealDrivingStateSource(client).drivingState.test {
            assertEquals(DrivingState.UNKNOWN, awaitItem())
            repeat(3) { client.send(UxRestrictionsEvent.Changed(false)) }
            assertEquals(DrivingState.PARKED, awaitItem())
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ---- 주행 ↔ 정차 전환 (AGENTS.md 규칙 3) ----

    @Test
    fun `기어를 P와 D로 번갈아 바꾸면 바뀔 때마다 정차와 주행을 순서대로 내보낸다`() = runTest {
        val client = ScriptedUxClient()

        RealDrivingStateSource(client).drivingState.test {
            assertEquals(DrivingState.UNKNOWN, awaitItem())
            client.send(UxRestrictionsEvent.Changed(false))
            assertEquals(DrivingState.PARKED, awaitItem())
            client.send(UxRestrictionsEvent.Changed(true))
            assertEquals(DrivingState.DRIVING, awaitItem())
            client.send(UxRestrictionsEvent.Changed(false))
            assertEquals(DrivingState.PARKED, awaitItem())
            client.send(UxRestrictionsEvent.Changed(true))
            assertEquals(DrivingState.DRIVING, awaitItem())
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `구독자가 바로 받지 못하는 사이 규제 값이 많이 와도 마지막 값을 잃지 않는다`() = runTest {
        val client = ScriptedUxClient()
        val received = mutableListOf<DrivingState>()
        backgroundScope.launch { RealDrivingStateSource(client).drivingState.toList(received) }
        runCurrent()
        assertEquals(listOf(DrivingState.UNKNOWN), received)

        // 구독자(메인 스레드)가 값을 가져가기 전에 규제 값이 여러 번 온다. 마지막 값은 주행이다.
        repeat(200) { i -> client.send(UxRestrictionsEvent.Changed(requiresDistractionOptimization = i % 2 == 1)) }
        runCurrent()

        // 마지막 값을 잃고 정차로 남으면, 실제로는 주행 중인데 정차 화면을 보여 준다.
        assertEquals(DrivingState.DRIVING, received.last())
    }

    // ---- 연결 수명 (설계 결정 3) ----

    @Test
    fun `구독하는 동안에는 시간이 지나도 연결을 유지하고 끊을 때 한 번만 닫는다`() = runTest {
        val client = ScriptedUxClient()
        val job = backgroundScope.launch { RealDrivingStateSource(client).drivingState.collect {} }
        runCurrent()

        client.send(UxRestrictionsEvent.Changed(false))
        client.send(UxRestrictionsEvent.Changed(true))
        client.send(UxRestrictionsEvent.Unavailable)
        advanceTimeBy(60_000)
        runCurrent()

        assertTrue("구독 중에는 흐름이 끝나지 않는다", job.isActive)
        assertEquals(1, client.connectCount)
        assertEquals("구독 중에는 닫지 않는다", 0, client.closeCount)

        job.cancel()
        runCurrent()

        assertEquals(1, client.closeCount)
    }

    @Test
    fun `원하는 상태를 받고 바로 끝내는 구독도 차량 연결을 닫는다`() = runTest {
        val client = ScriptedUxClient(onConnect = { send -> send(UxRestrictionsEvent.Changed(false)) })

        val state = RealDrivingStateSource(client).drivingState.first { it == DrivingState.PARKED }

        assertEquals(DrivingState.PARKED, state)
        assertEquals(1, client.connectCount)
        assertEquals(1, client.closeCount)
    }

    @Test
    fun `두 곳이 동시에 구독하면 연결도 둘이고 한쪽을 끊으면 그쪽 연결만 닫는다`() = runTest {
        val client = ScriptedUxClient()
        val source = RealDrivingStateSource(client)
        val first = mutableListOf<DrivingState>()
        val second = mutableListOf<DrivingState>()

        val firstJob = backgroundScope.launch { source.drivingState.toList(first) }
        backgroundScope.launch { source.drivingState.toList(second) }
        runCurrent()
        assertEquals(2, client.connectCount)

        client.connections[0].onEvent(UxRestrictionsEvent.Changed(false))
        runCurrent()
        assertEquals(listOf(DrivingState.UNKNOWN, DrivingState.PARKED), first)
        assertEquals("다른 구독의 연결 값이 섞이지 않는다", listOf(DrivingState.UNKNOWN), second)

        firstJob.cancel()
        runCurrent()

        assertFalse("끊은 구독의 연결은 닫힌다", client.connections[0].isOpen)
        assertTrue("남은 구독의 연결은 열려 있다", client.connections[1].isOpen)
    }

    @Test
    fun `끊은 구독의 연결에서 늦게 온 값은 예외 없이 버리고 새 구독에 섞이지 않는다`() = runTest {
        val client = ScriptedUxClient()
        val source = RealDrivingStateSource(client)

        source.drivingState.test {
            assertEquals(DrivingState.UNKNOWN, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        runCurrent()
        val stale = client.connections[0]
        assertFalse(stale.isOpen)

        source.drivingState.test {
            assertEquals(DrivingState.UNKNOWN, awaitItem())
            // 닫힌 뒤에 차량 서비스 콜백이 늦게 도착한 경우. 여기서 예외가 나면 테스트가 깨진다.
            stale.onEvent(UxRestrictionsEvent.Changed(false))
            expectNoEvents()
            client.send(UxRestrictionsEvent.Changed(true))
            assertEquals(DrivingState.DRIVING, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ---- 스레드 ----

    @Test
    fun `구독한 스레드에서 차량 연결을 열고 닫는다`() = runTest {
        val testThread = Thread.currentThread()
        val client = ScriptedUxClient()

        val job = launch { RealDrivingStateSource(client).drivingState.collect {} }
        runCurrent()
        job.cancel()
        job.join()

        assertSame("connect는 구독한 스레드(메인)에서 불린다", testThread, client.connectThread)
        assertSame("close는 구독한 스레드(메인)에서 불린다", testThread, client.closeThread)
    }

    @Test
    fun `다른 스레드에서 규제 값이 와도 그 상태를 내보낸다`() = runTest {
        val client = ScriptedUxClient()

        RealDrivingStateSource(client).drivingState.test {
            assertEquals(DrivingState.UNKNOWN, awaitItem())

            var error: Throwable? = null
            thread {
                try {
                    client.send(UxRestrictionsEvent.Changed(false))
                } catch (t: Throwable) {
                    error = t
                }
            }.join()

            assertNull("콜백 스레드에서 예외가 나면 안 된다", error)
            assertEquals(DrivingState.PARKED, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ---- connect / close 예외 ----

    @Test
    fun `닫다가 예외가 나도 구독은 취소로만 끝난다`() = runTest {
        val client = ScriptedUxClient(failOnClose = true)
        var cause: Throwable? = null

        val job = launch { RealDrivingStateSource(client).drivingState.collect {} }
        job.invokeOnCompletion { cause = it }
        runCurrent()
        job.cancel()
        job.join()

        assertEquals(1, client.closeCount)
        assertTrue("취소 말고 다른 예외로 끝나면 안 된다: $cause", cause is CancellationException)
    }

    @Test
    fun `연결에 실패한 구독도 끊을 때까지 살아 있고 끊으면 취소로만 끝난다`() = runTest {
        val client = ScriptedUxClient(failConnectTimes = Int.MAX_VALUE)
        var cause: Throwable? = null

        val job = launch { RealDrivingStateSource(client).drivingState.collect {} }
        job.invokeOnCompletion { cause = it }
        runCurrent()
        assertTrue("연결에 실패해도 흐름이 끝나지 않는다", job.isActive)

        job.cancel()
        job.join()

        assertTrue("취소 말고 다른 예외로 끝나면 안 된다: $cause", cause is CancellationException)
    }

    @Test
    fun `연결에 실패하면 같은 구독 안에서는 시간이 지나도 다시 연결하지 않고 모름만 한 번 내보낸다`() = runTest {
        val client = ScriptedUxClient(failConnectTimes = Int.MAX_VALUE)
        val received = mutableListOf<DrivingState>()

        backgroundScope.launch { RealDrivingStateSource(client).drivingState.toList(received) }
        runCurrent()
        advanceTimeBy(60_000)
        runCurrent()

        assertEquals(1, client.connectCount)
        assertEquals(listOf(DrivingState.UNKNOWN), received)
    }

    @Test
    fun `연결에 실패한 뒤 다시 구독하면 새로 연결해서 규제 값을 받는다`() = runTest {
        val client = ScriptedUxClient(failConnectTimes = 1)
        val source = RealDrivingStateSource(client)

        source.drivingState.test {
            assertEquals(DrivingState.UNKNOWN, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        source.drivingState.test {
            assertEquals(DrivingState.UNKNOWN, awaitItem())
            client.send(UxRestrictionsEvent.Changed(false))
            assertEquals(DrivingState.PARKED, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(2, client.connectCount)
    }

    @Test
    fun `연결 도중 정차 값을 보낸 뒤 예외가 나면 정차로 남지 않고 모름이 된다`() = runTest {
        val client = ScriptedUxClient(
            onConnect = { send ->
                send(UxRestrictionsEvent.Changed(false))
                throw IllegalStateException("등록 도중 실패 (테스트)")
            },
        )
        val received = mutableListOf<DrivingState>()

        backgroundScope.launch { RealDrivingStateSource(client).drivingState.toList(received) }
        runCurrent()

        // 연결이 실패했으니 그 전에 받은 정차 값은 믿을 수 없다. 정차 화면에 남으면 안 된다.
        assertEquals("받은 값: $received", DrivingState.UNKNOWN, received.last())
    }
}

/**
 * 테스트가 규제 값을 직접 보내는 가짜 창구. 연결마다 기록을 남긴다.
 *
 * @param failConnectTimes 처음 몇 번의 connect를 예외로 실패시킬지
 * @param failOnClose close가 (횟수를 센 뒤) 예외를 던질지
 * @param onConnect connect 안에서 돌려주기 전에 할 일. 현재 값을 바로 보내거나 예외를 던질 때 쓴다.
 */
private class ScriptedUxClient(
    private val failConnectTimes: Int = 0,
    private val failOnClose: Boolean = false,
    private val onConnect: ((UxRestrictionsEvent) -> Unit) -> Unit = {},
) : UxRestrictionsClient {

    val connections = CopyOnWriteArrayList<Connection>()
    private val attempts = AtomicInteger()

    val connectCount: Int get() = attempts.get()
    val closeCount: Int get() = connections.sumOf { it.closeCalls.get() }

    @Volatile var connectThread: Thread? = null
        private set

    @Volatile var closeThread: Thread? = null
        private set

    override fun connect(onEvent: (UxRestrictionsEvent) -> Unit): AutoCloseable {
        val attempt = attempts.incrementAndGet()
        connectThread = Thread.currentThread()
        if (attempt <= failConnectTimes) throw IllegalStateException("차량 서비스 연결 실패 (테스트)")
        onConnect(onEvent)
        return Connection(onEvent).also { connections += it }
    }

    /** 가장 최근에 열린 연결로 규제 값을 보낸다. 열린 연결이 없으면 테스트가 깨진다. */
    fun send(event: UxRestrictionsEvent) {
        connections.last { it.isOpen }.onEvent(event)
    }

    inner class Connection(val onEvent: (UxRestrictionsEvent) -> Unit) : AutoCloseable {
        val closeCalls = AtomicInteger()
        val isOpen: Boolean get() = closeCalls.get() == 0

        override fun close() {
            closeCalls.incrementAndGet()
            closeThread = Thread.currentThread()
            if (failOnClose) throw IllegalStateException("리스너 해제 실패 (테스트)")
        }
    }
}
