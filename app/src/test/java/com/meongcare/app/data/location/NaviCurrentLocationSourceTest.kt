package com.meongcare.app.data.location

import com.meongcare.app.data.guidance.navi.NaviLocationClient
import com.meongcare.app.data.guidance.navi.NaviLocationEvent
import com.meongcare.app.domain.location.CurrentLocation
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NaviCurrentLocationSourceTest {

    /** 구독할 때마다 [script]를 돌린다. 구독·해제 수와 동시에 열린 구독 수를 센다. */
    private class ScriptedLocationClient(
        private val script: suspend FlowCollector<NaviLocationEvent>.() -> Unit,
    ) : NaviLocationClient {
        var subscribed = 0
        var closed = 0
        var active = 0
        var maxActive = 0

        override fun currentLocation(): Flow<NaviLocationEvent> = flow {
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

    private fun located(address: String? = "수정구 시흥동") =
        NaviLocationEvent.Located(37.4127212, 127.094681, address)

    private fun TestScope.sourceOf(client: NaviLocationClient, logs: MutableList<String> = mutableListOf()) =
        NaviCurrentLocationSource(client = client, log = { logs += it }, timeSource = testScheduler.timeSource)

    @Test
    fun `내비가 위치를 주면 좌표와 주소를 돌려준다`() = runTest {
        val client = ScriptedLocationClient { emit(located()) }
        assertEquals(CurrentLocation(37.4127212, 127.094681, "수정구 시흥동"), sourceOf(client).current())
    }

    @Test
    fun `주소가 null이거나 비었거나 공백뿐이면 주소 모름이다`() = runTest {
        for (address in listOf(null, "", "   ")) {
            val client = ScriptedLocationClient { emit(located(address)) }
            assertEquals(CurrentLocation(37.4127212, 127.094681, null), sourceOf(client).current())
        }
    }

    @Test
    fun `주소 앞뒤 공백을 자른다`() = runTest {
        val client = ScriptedLocationClient { emit(located("  수정구 시흥동 ")) }
        assertEquals("수정구 시흥동", sourceOf(client).current()?.address)
    }

    @Test
    fun `내비가 실패를 알리면 모름이다`() = runTest {
        val client = ScriptedLocationClient { emit(NaviLocationEvent.Failed("CAUSE_UNKNOWN")) }
        assertNull(sourceOf(client).current())
        assertEquals(0, client.active)
    }

    @Test
    fun `시간 안에 답이 없으면 모름이고 연결을 놓는다`() = runTest {
        val client = ScriptedLocationClient { awaitCancellation() }
        assertNull(sourceOf(client).current())
        assertEquals(NaviCurrentLocationSource.DEFAULT_TIMEOUT.inWholeMilliseconds, currentTime)
        assertEquals(1, client.closed)
        assertEquals(0, client.active)
    }

    @Test
    fun `위치를 받으면 연결을 놓는다`() = runTest {
        val client = ScriptedLocationClient {
            emit(located())
            awaitCancellation()
        }
        sourceOf(client).current()
        assertEquals(1, client.closed)
        assertEquals(0, client.active)
    }

    @Test
    fun `좌표가 숫자가 아니거나 범위를 벗어나면 모름이다`() = runTest {
        val bad = listOf(
            Double.NaN to 127.0, 37.0 to Double.NaN,
            Double.POSITIVE_INFINITY to 127.0, 37.0 to Double.NEGATIVE_INFINITY,
            90.0001 to 127.0, -90.0001 to 127.0, 37.0 to 180.0001, 37.0 to -180.0001,
        )
        for ((lat, lon) in bad) {
            assertNull("$lat, $lon", currentLocationOf(lat, lon, "주소"))
        }
        val client = ScriptedLocationClient { emit(NaviLocationEvent.Located(Double.NaN, 127.0, "주소")) }
        assertNull(sourceOf(client).current())
    }

    @Test
    fun `좌표 경계값은 받는다`() {
        for ((lat, lon) in listOf(90.0 to 180.0, -90.0 to -180.0)) {
            assertEquals(CurrentLocation(lat, lon, null), currentLocationOf(lat, lon, null))
        }
    }

    @Test
    fun `좌표가 0, 0이면 모름이다`() = runTest {
        assertNull(currentLocationOf(0.0, 0.0, "주소"))
        val client = ScriptedLocationClient { emit(NaviLocationEvent.Located(0.0, 0.0, "주소")) }
        assertNull(sourceOf(client).current())
    }

    @Test
    fun `아무것도 주지 않고 끝나면 모름이다`() = runTest {
        val client = ScriptedLocationClient { }
        assertNull(sourceOf(client).current())
        assertEquals(0, client.active)
    }

    @Test
    fun `내비 쪽에서 예외가 나도 모름으로 돌려준다`() = runTest {
        val throwing = ScriptedLocationClient { throw IllegalStateException("binder") }
        assertNull(sourceOf(throwing).current())
        assertEquals(0, throwing.active)

        val throwsOnCall = NaviLocationClient { throw SecurityException("no") }
        assertNull(sourceOf(throwsOnCall).current())

        val innerCancel = ScriptedLocationClient { throw CancellationException("navi side") }
        assertNull(sourceOf(innerCancel).current())
    }

    @Test
    fun `부른 쪽이 취소되면 연결을 놓고 취소를 그대로 전한다`() = runTest {
        val client = ScriptedLocationClient { awaitCancellation() }
        val source = sourceOf(client)
        val asking = async { source.current() }
        runCurrent()
        assertEquals(1, client.active)
        asking.cancel()
        runCurrent()
        assertEquals(0, client.active)
        try {
            asking.await()
            fail("취소가 null로 바뀌면 안 된다")
        } catch (e: CancellationException) {
            // 기대한 대로
        }
    }

    @Test
    fun `두 번 물으면 매번 새로 연결한다`() = runTest {
        val client = ScriptedLocationClient { emit(located()) }
        val source = sourceOf(client)
        source.current()
        source.current()
        assertEquals(2, client.subscribed)
        assertEquals(2, client.closed)
    }

    @Test
    fun `동시에 물어도 내비 연결은 한 번에 하나다`() = runTest {
        val client = ScriptedLocationClient {
            delay(1.seconds)
            emit(located())
        }
        val source = sourceOf(client)
        val first = async { source.current() }
        val second = async { source.current() }
        assertEquals(CurrentLocation(37.4127212, 127.094681, "수정구 시흥동"), first.await())
        assertEquals(CurrentLocation(37.4127212, 127.094681, "수정구 시흥동"), second.await())
        assertEquals(1, client.maxActive)
        assertEquals(2, client.subscribed)
    }

    @Test
    fun `로그에 좌표와 주소를 남기지 않는다`() = runTest {
        val logs = mutableListOf<String>()
        val cases = listOf<suspend FlowCollector<NaviLocationEvent>.() -> Unit>(
            { emit(located()) },
            { emit(located(null)) },
            { emit(NaviLocationEvent.Failed("CAUSE_UNKNOWN")) },
            { emit(NaviLocationEvent.Located(0.0, 0.0, "수정구 시흥동")) },
            { awaitCancellation() },
            { },
            { throw IllegalStateException("37.4127212") },
        )
        for (case in cases) {
            sourceOf(ScriptedLocationClient(case), logs).current()
        }
        assertEquals("물을 때마다 한 줄", cases.size, logs.size)
        for (line in logs) {
            assertTrue(line, line.startsWith("현재 위치: "))
            for (secret in listOf("37.41", "127.09", "시흥동")) {
                assertTrue("로그에 $secret: $line", secret !in line)
            }
        }
    }
}
