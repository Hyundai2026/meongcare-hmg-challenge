package com.meongcare.app.data.location

import com.meongcare.app.data.guidance.navi.NaviLocationClient
import com.meongcare.app.data.guidance.navi.NaviLocationEvent
import com.meongcare.app.domain.location.CurrentLocation
import com.meongcare.app.domain.location.CurrentLocationSource
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

private typealias Script = suspend FlowCollector<NaviLocationEvent>.() -> Unit

/**
 * 011 계획 단계 5 "경계 테스트"와 설계 결정 1·6의 계약을 본다.
 * 기존 [NaviCurrentLocationSourceTest]가 다루지 않는 경계(시간 초과 앞뒤, 첫 결과만 보기, 잠금 풀림, 대기 중 취소,
 * 콜백 흐름 모양의 내비, 로그 내용)를 더한다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NaviCurrentLocationSourceBoundaryTest {

    /**
     * 구독할 때마다 [scripts]를 차례로 돌린다 (모자라면 마지막 것을 다시 쓴다).
     * 구독·해제 수와 동시에 열린 구독 수를 센다.
     */
    private class CountingClient(private val scripts: List<Script>) : NaviLocationClient {
        constructor(script: Script) : this(listOf(script))

        var subscribed = 0
        var closed = 0
        var active = 0
        var maxActive = 0

        override fun currentLocation(): Flow<NaviLocationEvent> = flow {
            val script = scripts[minOf(subscribed, scripts.lastIndex)]
            subscribed++
            active++
            maxActive = maxOf(maxActive, active)
            try {
                script()
            } finally {
                active--
                closed++
            }
        }
    }

    /** 어댑터(`callbackFlow` + `awaitClose`에서 release)와 같은 모양의 가짜. release 수를 센다. */
    private class CallbackShapedClient(
        private val onSubscribe: (ProducerScope<NaviLocationEvent>) -> Unit,
    ) : NaviLocationClient {
        val released = AtomicInteger(0)

        override fun currentLocation(): Flow<NaviLocationEvent> = callbackFlow {
            onSubscribe(this)
            awaitClose { released.incrementAndGet() }
        }
    }

    private val here = CurrentLocation(37.4127212, 127.094681, "수정구 시흥동")
    private val elsewhere = CurrentLocation(37.5665, 126.978, "중구 태평로1가")

    private fun located(location: CurrentLocation = here) =
        NaviLocationEvent.Located(location.latitude, location.longitude, location.address)

    private val timeoutMs = NaviCurrentLocationSource.DEFAULT_TIMEOUT.inWholeMilliseconds

    private fun TestScope.sourceOf(client: NaviLocationClient, logs: MutableList<String> = mutableListOf()) =
        NaviCurrentLocationSource(client = client, log = { logs += it }, timeSource = testScheduler.timeSource)

    /** 잠금이 풀리지 않아 영원히 기다리는 버그가 있으면 1분(가상 시간) 뒤에 테스트가 실패하게 한다. */
    private suspend fun CurrentLocationSource.currentOrFailIfStuck(): CurrentLocation? =
        withTimeout(1.minutes) { current() }

    // ---- 시간 초과 ----

    @Test
    fun `기본 대기 시간은 5초보다 짧지 않다`() {
        assertTrue(
            "${NaviCurrentLocationSource.DEFAULT_TIMEOUT}",
            NaviCurrentLocationSource.DEFAULT_TIMEOUT >= 5.seconds,
        )
    }

    @Test
    fun `대기 시간 직전에 온 위치는 받는다`() = runTest {
        val client = CountingClient {
            delay(timeoutMs - 1)
            emit(located())
        }
        assertEquals(here, sourceOf(client).current())
        assertEquals(0, client.active)
    }

    @Test
    fun `대기 시간이 지난 뒤에 온 위치는 버리고 모름이다`() = runTest {
        val client = CountingClient {
            delay(timeoutMs + 1)
            emit(located())
        }
        assertNull(sourceOf(client).current())
        assertEquals(timeoutMs, currentTime)
        assertEquals(1, client.closed)
        assertEquals(0, client.active)
    }

    @Test
    fun `생성자로 준 대기 시간만큼만 기다린다`() = runTest {
        val logs = mutableListOf<String>()
        val client = CountingClient { awaitCancellation() }
        val source = NaviCurrentLocationSource(
            client = client,
            timeout = 2.seconds,
            log = { logs += it },
            timeSource = testScheduler.timeSource,
        )
        assertNull(source.current())
        assertEquals(2_000, currentTime)
        assertEquals(0, client.active)
        assertEquals(1, logs.size)
        assertTrue(logs[0], "2000ms" in logs[0])
    }

    // ---- 첫 결과만 본다 (결정 1: first()) ----

    @Test
    fun `실패가 먼저 오면 뒤에 위치가 와도 모름이고 연결을 놓는다`() = runTest {
        // 결정 3 (c): 위치 쪽 리스너가 안내의 onError를 받으면 그 한 번은 모름이 된다
        val client = CountingClient {
            emit(NaviLocationEvent.Failed("CAUSE_SEARCH_ROUTE"))
            emit(located())
            awaitCancellation()
        }
        assertNull(sourceOf(client).current())
        assertEquals(0, client.active)
    }

    @Test
    fun `첫 위치가 이상하면 뒤에 좋은 위치가 와도 모름이다`() = runTest {
        val client = CountingClient {
            emit(NaviLocationEvent.Located(0.0, 0.0, "주소"))
            emit(located())
        }
        assertNull(sourceOf(client).current())
        assertEquals(0, client.active)
    }

    @Test
    fun `위치가 여러 번 와도 첫 위치를 쓰고 연결을 놓는다`() = runTest {
        var emittedAfterFirst = false
        val client = CountingClient {
            emit(located(here))
            emittedAfterFirst = true
            emit(located(elsewhere))
            awaitCancellation()
        }
        assertEquals(here, sourceOf(client).current())
        assertFalse("첫 값 뒤에는 구독이 끊겨야 한다", emittedAfterFirst)
        assertEquals(0, client.active)
    }

    // ---- 늦게 오는 실패와 예외 ----

    @Test
    fun `늦게 온 연결 실패도 모름이고 연결을 놓는다`() = runTest {
        val client = CountingClient {
            delay(300)
            emit(NaviLocationEvent.Failed("ConnectionFailed"))
            awaitCancellation()
        }
        assertNull(sourceOf(client).current())
        assertEquals(300, currentTime)
        assertEquals(0, client.active)
    }

    @Test
    fun `내비가 기다리다 예외를 내도 모름이고 연결을 놓는다`() = runTest {
        val client = CountingClient {
            delay(1_000)
            throw IllegalStateException("binder died")
        }
        assertNull(sourceOf(client).current())
        assertEquals(0, client.active)
    }

    // ---- 캐시 없음, 여러 번 불러도 됨 ----

    @Test
    fun `위치가 바뀌면 다음 묻기는 새 위치를 준다`() = runTest {
        val client = CountingClient(listOf<Script>({ emit(located(here)) }, { emit(located(elsewhere)) }))
        val source = sourceOf(client)
        assertEquals(here, source.current())
        assertEquals(elsewhere, source.current())
    }

    @Test
    fun `전에 위치를 받았어도 이번에 모르면 모름이다`() = runTest {
        val client = CountingClient(
            listOf<Script>({ emit(located()) }, { emit(NaviLocationEvent.Failed("CAUSE_UNKNOWN")) }, { awaitCancellation() }),
        )
        val source = sourceOf(client)
        assertEquals(here, source.current())
        assertNull("실패인데 이전 값을 주면 안 된다", source.current())
        assertNull("시간 초과인데 이전 값을 주면 안 된다", source.current())
        assertEquals(3, client.subscribed)
    }

    @Test
    fun `모름으로 끝난 뒤에도 다음 묻기는 새로 연결해 답을 받는다`() = runTest {
        val client = CountingClient(
            listOf<Script>(
                { awaitCancellation() }, // 시간 초과
                { emit(NaviLocationEvent.Failed("CAUSE_UNKNOWN")) },
                { }, // 아무것도 없이 끝남
                { throw IllegalStateException("binder") },
                { throw CancellationException("navi side") },
                { emit(located()) },
            ),
        )
        val source = sourceOf(client)
        repeat(5) { assertNull("${it + 1}번째", source.currentOrFailIfStuck()) }
        assertEquals(here, source.currentOrFailIfStuck())
        assertEquals(6, client.subscribed)
        assertEquals(0, client.active)
    }

    @Test
    fun `호출할 때 바로 예외를 낸 뒤에도 다음 묻기는 된다`() = runTest {
        var calls = 0
        val client = NaviLocationClient {
            calls++
            if (calls == 1) throw SecurityException("no") else flow { emit(located()) }
        }
        val source = sourceOf(client)
        assertNull(source.currentOrFailIfStuck())
        assertEquals(here, source.currentOrFailIfStuck())
    }

    // ---- 부른 쪽의 취소 ----

    @Test
    fun `부른 쪽이 취소된 뒤에도 다시 물을 수 있다`() = runTest {
        val client = CountingClient(listOf<Script>({ awaitCancellation() }, { emit(located()) }))
        val source = sourceOf(client)
        val asking = async { source.current() }
        runCurrent()
        asking.cancel()
        runCurrent()
        assertEquals(0, client.active)
        assertEquals(here, source.currentOrFailIfStuck())
        assertEquals(2, client.subscribed)
    }

    @Test
    fun `부른 쪽의 시간 제한이 먼저 끝나면 모름이 아니라 취소가 나오고 연결을 놓는다`() = runTest {
        val client = CountingClient { awaitCancellation() }
        val source = sourceOf(client)
        var returned = false
        try {
            withTimeout(1.seconds) {
                source.current()
                returned = true
            }
            fail("부른 쪽 시간 제한이 끝났는데 정상으로 끝났다")
        } catch (e: TimeoutCancellationException) {
            // 기대한 대로
        }
        assertFalse("취소가 null로 바뀌면 안 된다", returned)
        assertEquals(1_000, currentTime)
        assertEquals(0, client.active)
    }

    @Test
    fun `차례를 기다리던 호출이 취소되면 내비에 연결하지 않고 취소를 그대로 전한다`() = runTest {
        val client = CountingClient {
            delay(1_000)
            emit(located())
        }
        val source = sourceOf(client)
        val first = async { source.current() }
        val second = async { source.current() }
        runCurrent()
        assertEquals(1, client.subscribed)

        second.cancel()
        assertEquals(here, first.await())
        try {
            second.await()
            fail("취소가 null이나 값으로 바뀌면 안 된다")
        } catch (e: CancellationException) {
            // 기대한 대로
        }
        advanceUntilIdle()
        assertEquals("취소된 호출은 연결하지 않는다", 1, client.subscribed)
        assertEquals(0, client.active)
    }

    // ---- 한 번에 하나 ----

    @Test
    fun `동시에 물었는데 둘 다 답이 없으면 두 번째는 첫 번째가 끝난 뒤부터 다시 기다린다`() = runTest {
        // 결정 1: "두 번째 호출은 첫 호출이 끝난 뒤 새로 묻는다. 그래서 최악의 대기는 시간 초과의 2배다."
        val client = CountingClient { awaitCancellation() }
        val source = sourceOf(client)
        var firstAt = -1L
        var secondAt = -1L
        val first = async { source.current().also { firstAt = currentTime } }
        val second = async { source.current().also { secondAt = currentTime } }
        assertNull(first.await())
        assertNull(second.await())
        assertEquals(timeoutMs, firstAt)
        assertEquals(timeoutMs * 2, secondAt)
        assertEquals(1, client.maxActive)
        assertEquals(2, client.subscribed)
        assertEquals(0, client.active)
    }

    @Test
    fun `세 곳에서 동시에 물어도 연결은 하나씩이고 모두 답을 받는다`() = runTest {
        val client = CountingClient {
            delay(500)
            emit(located())
        }
        val source = sourceOf(client)
        val answers = List(3) { async { source.current() } }.awaitAll()
        assertEquals(List(3) { here }, answers)
        assertEquals(1, client.maxActive)
        assertEquals(3, client.subscribed)
        assertEquals(0, client.active)
    }

    // ---- 어댑터와 같은 모양(callbackFlow)의 내비 ----

    @Test
    fun `내비 답이 다른 스레드에서 와도 위치를 받고 release한다`() = runBlocking<Unit> {
        // 어댑터의 리스너 콜백은 binder 스레드에서 온다 (sdk-notes [obs-navi-emu])
        val client = CallbackShapedClient { scope ->
            thread {
                Thread.sleep(50)
                scope.trySend(located())
            }
        }
        assertEquals(here, NaviCurrentLocationSource(client).current())
        assertEquals(1, client.released.get())
    }

    @Test
    fun `콜백 흐름의 내비가 연결 실패를 알리면 모름이고 release한다`() = runTest {
        val client = CallbackShapedClient { it.trySend(NaviLocationEvent.Failed("ConnectionFailed")) }
        assertNull(sourceOf(client).current())
        assertEquals(1, client.released.get())
    }

    @Test
    fun `콜백 흐름의 내비가 답하지 않으면 대기 시간 뒤 모름이고 release한다`() = runTest {
        val client = CallbackShapedClient { }
        assertNull(sourceOf(client).current())
        assertEquals(timeoutMs, currentTime)
        assertEquals(1, client.released.get())
    }

    @Test
    fun `콜백 흐름의 내비를 기다리다 부른 쪽이 취소되면 release한다`() = runTest {
        val client = CallbackShapedClient { }
        val asking = async { sourceOf(client).current() }
        runCurrent()
        assertEquals(0, client.released.get())
        asking.cancel()
        runCurrent()
        assertEquals(1, client.released.get())
        assertTrue(asking.isCancelled)
    }

    // ---- 로그 (결정 6) ----

    @Test
    fun `성공 로그에는 걸린 시간과 주소가 있는지만 남긴다`() = runTest {
        val logs = mutableListOf<String>()
        sourceOf(CountingClient { delay(134); emit(located()) }, logs).current()
        sourceOf(CountingClient { emit(located(here.copy(address = null))) }, logs).current()
        assertEquals(2, logs.size)
        val withAddress = logs[0]
        assertTrue(withAddress, withAddress.startsWith("현재 위치: "))
        assertTrue(withAddress, "성공" in withAddress && "주소 있음" in withAddress && "134ms" in withAddress)
        val withoutAddress = logs[1]
        assertTrue(withoutAddress, "성공" in withoutAddress)
        assertFalse("주소가 없는데 있다고 쓰면 안 된다: $withoutAddress", "주소 있음" in withoutAddress)
    }

    @Test
    fun `시간 초과 로그에는 기다린 시간을 남긴다`() = runTest {
        val logs = mutableListOf<String>()
        sourceOf(CountingClient { awaitCancellation() }, logs).current()
        assertEquals(1, logs.size)
        assertTrue(logs[0], "시간 초과" in logs[0] && "${timeoutMs}ms" in logs[0])
    }

    @Test
    fun `실패 로그에는 내비 오류 이름을 남긴다`() = runTest {
        val logs = mutableListOf<String>()
        sourceOf(CountingClient { emit(NaviLocationEvent.Failed("CAUSE_SEARCH_ROUTE")) }, logs).current()
        assertEquals(1, logs.size)
        assertTrue(logs[0], "실패" in logs[0] && "CAUSE_SEARCH_ROUTE" in logs[0])
    }

    @Test
    fun `범위를 벗어난 값이 와도 로그에는 값 이상이라고만 쓰고 좌표와 주소를 남기지 않는다`() = runTest {
        val logs = mutableListOf<String>()
        sourceOf(CountingClient { emit(NaviLocationEvent.Located(37.4127212, 200.5, "수정구 시흥동")) }, logs).current()
        assertEquals(1, logs.size)
        assertTrue(logs[0], "값 이상" in logs[0])
        for (secret in listOf("37.41", "200.5", "시흥동")) {
            assertFalse("로그에 $secret: ${logs[0]}", secret in logs[0])
        }
    }

    @Test
    fun `예외 메시지에 좌표나 주소가 있어도 로그에 남기지 않는다`() = runTest {
        val logs = mutableListOf<String>()
        val message = "37.4127212,127.094681 수정구 시흥동"
        sourceOf(NaviLocationClient { throw SecurityException(message) }, logs).current()
        sourceOf(CountingClient { throw IllegalArgumentException(message) }, logs).current()
        sourceOf(CountingClient { throw CancellationException(message) }, logs).current()
        assertEquals("물을 때마다 한 줄", 3, logs.size)
        for (line in logs) {
            for (secret in listOf("37.41", "127.09", "시흥동")) {
                assertFalse("로그에 $secret: $line", secret in line)
            }
        }
    }

    @Test
    fun `동시에 물어도 로그는 물을 때마다 한 줄이다`() = runTest {
        val logs = mutableListOf<String>()
        val source = sourceOf(CountingClient { delay(100); emit(located()) }, logs)
        List(3) { async { source.current() } }.awaitAll()
        assertEquals(3, logs.size)
        assertTrue(logs.toString(), logs.all { it.startsWith("현재 위치: ") })
    }
}
