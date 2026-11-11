package com.meongcare.app.data.voice

import com.meongcare.app.domain.voice.VoiceEvent
import com.meongcare.app.domain.voice.VoiceFailure
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * `RealVoiceInput` 경계 테스트. 기준: docs/plans/active/010-voice-input.md "설계 결정 5"의 이벤트 표와 시간,
 * `VoiceInput` KDoc (구독하면 켜고, Final 또는 Failed를 낸 뒤 끝난다, 취소하면 멈춘다),
 * `SpeechToTextClient` KDoc (이벤트는 어느 스레드에서 올지 모른다, close는 여러 번 불러도 된다).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RealVoiceInputBoundaryTest {

    /** 테스트가 SDK 이벤트를 보내는 가짜 클라이언트. [onStart]는 start() 안에서 바로 보낼 이벤트. */
    private class ScriptedClient(
        private val failFirstStarts: Int = 0,
        private val onStart: ((SttEvent) -> Unit) -> Unit = {},
    ) : SpeechToTextClient {
        val starts = AtomicInteger()
        val closes = AtomicInteger()
        private var onEvent: ((SttEvent) -> Unit)? = null

        override fun start(onEvent: (SttEvent) -> Unit): AutoCloseable {
            if (starts.incrementAndGet() <= failFirstStarts) error("음성 서비스 없음")
            this.onEvent = onEvent
            onStart(onEvent)
            return AutoCloseable { closes.incrementAndGet() }
        }

        fun send(vararg events: SttEvent) = events.forEach { onEvent!!.invoke(it) }
    }

    private fun realInput(client: SpeechToTextClient) = RealVoiceInput(
        client = client,
        initTimeout = 3.seconds,
        finalAfterEndedTimeout = 2.seconds,
        maxListen = 15.seconds,
    )

    private fun TestScope.collect(input: RealVoiceInput, into: MutableList<VoiceEvent>): Job =
        launch { input.listen().collect { into += it } }.also { runCurrent() }

    @Test
    fun `구독하기 전에는 마이크를 켜지 않는다`() = runTest {
        val client = ScriptedClient()
        val input = realInput(client)

        input.listen()
        input.listen()
        runCurrent()

        assertEquals(0, client.starts.get())
    }

    @Test
    fun `시작하는 도중에 바로 온 초기화 실패도 쓸 수 없음으로 끝나고 한 번만 닫는다`() = runTest {
        val client = ScriptedClient(onStart = { it(SttEvent.InitFailed("PERMISSION_DENIED")) })

        val events = realInput(client).listen().toList()

        assertEquals(listOf<VoiceEvent>(VoiceEvent.Failed(VoiceFailure.UNAVAILABLE)), events)
        assertEquals(1, client.closes.get())
    }

    @Test
    fun `시작하는 도중에 최종 결과까지 와도 그대로 내고 한 번만 닫는다`() = runTest {
        val client = ScriptedClient(onStart = { send ->
            send(SttEvent.Ready)
            send(SttEvent.Updated("토해요", completed = false))
            send(SttEvent.Updated("토해요", completed = true))
        })

        val events = realInput(client).listen().toList()

        assertEquals(listOf(VoiceEvent.Ready, VoiceEvent.Partial("토해요"), VoiceEvent.Final("토해요")), events)
        assertEquals(1, client.closes.get())
    }

    @Test
    fun `부분 결과가 한꺼번에 많이 와도 하나도 잃지 않고 최종 결과로 끝난다`() = runTest {
        val count = 500
        val client = ScriptedClient(onStart = { send ->
            send(SttEvent.Ready)
            repeat(count) {
                send(SttEvent.Started)
                send(SttEvent.Updated("글자 $it", completed = false))
            }
            send(SttEvent.Updated("다 들었어요", completed = true))
        })

        val events = realInput(client).listen().toList()

        assertEquals(VoiceEvent.Ready, events.first())
        assertEquals(List(count) { VoiceEvent.Partial("글자 $it") }, events.drop(1).dropLast(1))
        assertEquals(VoiceEvent.Final("다 들었어요"), events.last())
    }

    @Test
    fun `다른 스레드에서 이벤트가 와도 순서대로 내고 최종 결과로 끝난다`() = runBlocking {
        val client = ScriptedClient(onStart = { send ->
            Thread {
                Thread.sleep(20)
                send(SttEvent.Ready)
                repeat(30) { send(SttEvent.Updated("부분 $it", completed = false)) }
                send(SttEvent.Ended)
                send(SttEvent.Updated("강아지가 토해요", completed = true))
            }.start()
        })

        val events = withTimeout(5_000) { realInput(client).listen().toList() }

        assertEquals(
            listOf(VoiceEvent.Ready) + List(30) { VoiceEvent.Partial("부분 $it") } + VoiceEvent.Final("강아지가 토해요"),
            events,
        )
        assertEquals(1, client.closes.get())
    }

    @Test
    fun `준비 없이 부분 결과가 먼저 와도 초기화 시간이 지났다고 끝내지 않는다`() = runTest {
        val client = ScriptedClient()
        val got = mutableListOf<VoiceEvent>()
        val job = collect(realInput(client), got)

        advanceTimeBy(1.seconds)
        client.send(SttEvent.Updated("강아지가", completed = false))
        advanceTimeBy(5.seconds)
        assertFalse(job.isCompleted)

        client.send(SttEvent.Updated("강아지가 토해요", completed = true))
        runCurrent()
        assertEquals(listOf(VoiceEvent.Partial("강아지가"), VoiceEvent.Final("강아지가 토해요")), got)
    }

    @Test
    fun `준비된 뒤에는 말이 없어도 최대 듣기 시간까지 기다린다`() = runTest {
        val client = ScriptedClient()
        val got = mutableListOf<VoiceEvent>()
        val job = collect(realInput(client), got)
        client.send(SttEvent.Ready)

        advanceTimeBy(14_999.milliseconds)
        assertFalse(job.isCompleted)
        assertEquals(listOf<VoiceEvent>(VoiceEvent.Ready), got)

        advanceTimeBy(2.milliseconds)
        assertEquals(listOf(VoiceEvent.Ready, VoiceEvent.Failed(VoiceFailure.NO_SPEECH)), got)
        assertEquals(1, client.closes.get())
    }

    @Test
    fun `인식 시작만 오고 준비나 받아쓰기가 없으면 시작 뒤 초기화 시간에 쓸 수 없음으로 끝난다`() = runTest {
        // 표: "initTimeout 안에 Ready·Updated가 하나도 없음 → Failed(UNAVAILABLE)". Started는 "없음"(내보내지 않는다).
        val client = ScriptedClient()
        val got = mutableListOf<VoiceEvent>()
        collect(realInput(client), got)

        advanceTimeBy(1.seconds)
        client.send(SttEvent.Started)
        advanceTimeBy(1.seconds)
        client.send(SttEvent.Started)
        advanceTimeBy(1_001.milliseconds) // 시작 뒤 3.001초

        assertEquals(listOf<VoiceEvent>(VoiceEvent.Failed(VoiceFailure.UNAVAILABLE)), got)
    }

    @Test
    fun `인식이 끝난 뒤 최종 결과를 기다리는 시간은 다른 이벤트가 와도 늘어나지 않는다`() = runTest {
        // 표: "Ended → finalAfterEndedTimeout 안에 최종 결과가 없으면 Failed(NO_SPEECH)"
        val client = ScriptedClient()
        val got = mutableListOf<VoiceEvent>()
        collect(realInput(client), got)

        client.send(SttEvent.Ready, SttEvent.Ended)
        advanceTimeBy(1_500.milliseconds)
        client.send(SttEvent.Started)
        advanceTimeBy(501.milliseconds) // 끝난 뒤 2.001초

        assertEquals(listOf(VoiceEvent.Ready, VoiceEvent.Failed(VoiceFailure.NO_SPEECH)), got)
    }

    @Test
    fun `인식이 끝난 뒤 기다리는 시간 바로 전에 온 최종 결과는 낸다`() = runTest {
        val client = ScriptedClient()
        val got = mutableListOf<VoiceEvent>()
        collect(realInput(client), got)

        client.send(SttEvent.Ready, SttEvent.Ended)
        advanceTimeBy(1_999.milliseconds)
        client.send(SttEvent.Updated("기침해요", completed = true))
        runCurrent()

        assertEquals(listOf(VoiceEvent.Ready, VoiceEvent.Final("기침해요")), got)
    }

    @Test
    fun `최종 결과가 탭 줄바꿈 전각 공백뿐이어도 말소리 없음이다`() = runTest {
        listOf("\t\n ", "　", " \r\n\t ").forEach { blank ->
            val client = ScriptedClient()
            val got = mutableListOf<VoiceEvent>()
            collect(realInput(client), got)
            client.send(SttEvent.Ready, SttEvent.Updated(blank, completed = true))
            runCurrent()

            assertEquals(listOf(VoiceEvent.Ready, VoiceEvent.Failed(VoiceFailure.NO_SPEECH)), got)
        }
    }

    @Test
    fun `어떤 이유로 끝나든 클라이언트를 정확히 한 번 닫는다`() = runTest {
        suspend fun endsWithOneClose(name: String, end: suspend TestScope.(ScriptedClient, Job) -> Unit) {
            val client = ScriptedClient()
            val job = collect(realInput(client), mutableListOf())
            end(this, client, job)
            advanceTimeBy(30.seconds) // 남은 시간 장치가 다시 닫지 않는지

            assertTrue(name, job.isCompleted)
            assertEquals(name, 1, client.starts.get())
            assertEquals(name, 1, client.closes.get())
        }

        endsWithOneClose("최종 결과") { c, _ -> c.send(SttEvent.Ready, SttEvent.Updated("토해요", true)); runCurrent() }
        endsWithOneClose("빈 최종 결과") { c, _ -> c.send(SttEvent.Ready, SttEvent.Updated(" ", true)); runCurrent() }
        endsWithOneClose("초기화 실패") { c, _ -> c.send(SttEvent.InitFailed("ON_DEVICE_ENGINE_ERROR")); runCurrent() }
        endsWithOneClose("오류") { c, _ -> c.send(SttEvent.Ready, SttEvent.Error("MIC_ERROR")); runCurrent() }
        endsWithOneClose("초기화 시간 초과") { _, _ -> advanceTimeBy(3_001.milliseconds) }
        endsWithOneClose("끝난 뒤 최종 결과 없음") { c, _ -> c.send(SttEvent.Ready, SttEvent.Ended); advanceTimeBy(2_001.milliseconds) }
        endsWithOneClose("최대 듣기 시간") { c, _ -> c.send(SttEvent.Ready); advanceTimeBy(15_001.milliseconds) }
        endsWithOneClose("구독 취소") { c, job -> c.send(SttEvent.Ready); job.cancel(); runCurrent() }
    }

    @Test
    fun `최종 결과 뒤에는 시간이 지나도 다른 실패를 더 내지 않는다`() = runTest {
        val client = ScriptedClient()
        val got = mutableListOf<VoiceEvent>()
        collect(realInput(client), got)
        client.send(SttEvent.Ready, SttEvent.Ended, SttEvent.Updated("토해요", completed = true))
        runCurrent()

        advanceTimeBy(30.seconds)

        assertEquals(listOf(VoiceEvent.Ready, VoiceEvent.Final("토해요")), got)
    }

    @Test
    fun `취소한 뒤 SDK가 늦게 이벤트를 보내도 예외 없이 버린다`() = runTest {
        val client = ScriptedClient()
        val got = mutableListOf<VoiceEvent>()
        val job = collect(realInput(client), got)
        client.send(SttEvent.Ready, SttEvent.Updated("토하", completed = false))
        runCurrent()
        job.cancel()
        runCurrent()

        // 문서: stop() 뒤 onEndedRecognition이 온다 (sdk-notes "시작·중지")
        client.send(SttEvent.Ended, SttEvent.Updated("토해요", completed = true), SttEvent.Error("UNKNOWN"))
        runCurrent()

        assertEquals(listOf(VoiceEvent.Ready, VoiceEvent.Partial("토하")), got)
        assertEquals(1, client.closes.get())
    }

    @Test
    fun `시작이 예외를 던진 다음 듣기에서는 새로 시작한다`() = runTest {
        val client = ScriptedClient(failFirstStarts = 1, onStart = { send ->
            send(SttEvent.Ready)
            send(SttEvent.Updated("기침해요", completed = true))
        })
        val input = realInput(client)

        assertEquals(listOf<VoiceEvent>(VoiceEvent.Failed(VoiceFailure.UNAVAILABLE)), input.listen().toList())
        assertEquals(listOf(VoiceEvent.Ready, VoiceEvent.Final("기침해요")), input.listen().toList())
        assertEquals(2, client.starts.get())
        assertEquals(1, client.closes.get())
    }

    @Test
    fun `듣기 한 번마다 클라이언트를 새로 시작하고 닫는다`() = runTest {
        val client = ScriptedClient(onStart = { send ->
            send(SttEvent.Ready)
            send(SttEvent.Updated("토해요", completed = true))
        })
        val input = realInput(client)

        repeat(3) { input.listen().toList() }

        assertEquals(3, client.starts.get())
        assertEquals(3, client.closes.get())
    }
}
