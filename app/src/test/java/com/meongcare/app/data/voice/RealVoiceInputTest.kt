package com.meongcare.app.data.voice

import com.meongcare.app.domain.voice.VoiceEvent
import com.meongcare.app.domain.voice.VoiceFailure
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class RealVoiceInputTest {

    /** 테스트가 SDK 이벤트를 직접 보내는 가짜 클라이언트. */
    private class FakeClient(private val throwOnStart: Boolean = false) : SpeechToTextClient {
        var starts = 0
        var closes = 0
        private var onEvent: ((SttEvent) -> Unit)? = null

        override fun start(onEvent: (SttEvent) -> Unit): AutoCloseable {
            if (throwOnStart) error("음성 서비스 없음")
            starts++
            this.onEvent = onEvent
            return AutoCloseable { closes++ }
        }

        fun send(vararg events: SttEvent) = events.forEach { onEvent!!.invoke(it) }
    }

    private val client = FakeClient()
    private val input = RealVoiceInput(
        client = client,
        initTimeout = 3.seconds,
        finalAfterEndedTimeout = 2.seconds,
        maxListen = 15.seconds,
    )

    /** 듣기를 시작하고 받은 이벤트를 모은다. */
    private fun TestScope.collect(into: MutableList<VoiceEvent>) = launch { input.listen().collect { into += it } }
        .also { runCurrent() }

    @Test
    fun `준비되면 부분 결과를 거쳐 최종 결과를 내고 끝난다`() = runTest {
        val got = mutableListOf<VoiceEvent>()
        val job = collect(got)

        client.send(
            SttEvent.Ready,
            SttEvent.Updated("강아지가", completed = false),
            SttEvent.Updated("강아지가 토해요", completed = false),
            SttEvent.Updated("강아지가 토해요", completed = true),
        )
        runCurrent()

        assertEquals(
            listOf(
                VoiceEvent.Ready,
                VoiceEvent.Partial("강아지가"),
                VoiceEvent.Partial("강아지가 토해요"),
                VoiceEvent.Final("강아지가 토해요"),
            ),
            got,
        )
        assertEquals(true, job.isCompleted)
    }

    @Test
    fun `부분 결과마다 다시 오는 인식 시작은 무시한다`() = runTest {
        val got = mutableListOf<VoiceEvent>()
        collect(got)

        client.send(
            SttEvent.Ready,
            SttEvent.Started,
            SttEvent.Updated("강", completed = false),
            SttEvent.Started,
            SttEvent.Updated("강아지", completed = false),
            SttEvent.Started,
        )
        runCurrent()

        assertEquals(listOf(VoiceEvent.Ready, VoiceEvent.Partial("강"), VoiceEvent.Partial("강아지")), got)
    }

    @Test
    fun `인식이 끝난 뒤 최종 결과가 오면 그대로 낸다`() = runTest {
        val got = mutableListOf<VoiceEvent>()
        collect(got)

        client.send(SttEvent.Ready, SttEvent.Updated("기침해요", false), SttEvent.Ended)
        advanceTimeBy(1.seconds)
        client.send(SttEvent.Updated("기침해요", completed = true))
        runCurrent()

        assertEquals(VoiceEvent.Final("기침해요"), got.last())
    }

    @Test
    fun `인식이 끝났는데 최종 결과가 오지 않으면 말소리 없음으로 끝난다`() = runTest {
        val got = mutableListOf<VoiceEvent>()
        val job = collect(got)

        client.send(SttEvent.Ready, SttEvent.Ended)
        advanceTimeBy(1_999.milliseconds)
        assertEquals(listOf<VoiceEvent>(VoiceEvent.Ready), got)

        advanceTimeBy(2.milliseconds)
        assertEquals(VoiceEvent.Failed(VoiceFailure.NO_SPEECH), got.last())
        assertEquals(true, job.isCompleted)
    }

    @Test
    fun `최종 결과가 비었거나 공백뿐이면 말소리 없음이다`() = runTest {
        listOf("", "   ").forEach { blank ->
            val got = mutableListOf<VoiceEvent>()
            collect(got)
            client.send(SttEvent.Ready, SttEvent.Updated(blank, completed = true))
            runCurrent()
            assertEquals(listOf(VoiceEvent.Ready, VoiceEvent.Failed(VoiceFailure.NO_SPEECH)), got)
        }
    }

    @Test
    fun `초기화가 실패하거나 시간 안에 응답이 없으면 쓸 수 없음으로 끝난다`() = runTest {
        val failed = mutableListOf<VoiceEvent>()
        collect(failed)
        client.send(SttEvent.InitFailed("ON_DEVICE_ENGINE_ERROR"))
        runCurrent()
        assertEquals(listOf<VoiceEvent>(VoiceEvent.Failed(VoiceFailure.UNAVAILABLE)), failed)

        val silent = mutableListOf<VoiceEvent>()
        collect(silent)
        advanceTimeBy(2_999.milliseconds)
        assertEquals(emptyList<VoiceEvent>(), silent)
        advanceTimeBy(2.milliseconds)
        assertEquals(listOf<VoiceEvent>(VoiceEvent.Failed(VoiceFailure.UNAVAILABLE)), silent)
    }

    @Test
    fun `오류 이름을 말소리 없음 마이크 쓸 수 없음 세 가지로 나눈다`() = runTest {
        // docs/pleos/sdk-notes.md의 ErrorType 12개
        val expected = mapOf(
            "TIMEOUT" to VoiceFailure.NO_SPEECH,
            "MIC_ERROR" to VoiceFailure.MIC,
            "AUDIO_ERROR" to VoiceFailure.MIC,
            "INVALID_REQUEST_ERROR" to VoiceFailure.UNAVAILABLE,
            "NETWORK_ERROR" to VoiceFailure.UNAVAILABLE,
            "NOT_SUPPORTED_API" to VoiceFailure.UNAVAILABLE,
            "ON_DEVICE_ENGINE_ERROR" to VoiceFailure.UNAVAILABLE,
            "PERMISSION_DENIED" to VoiceFailure.UNAVAILABLE,
            "SERVER_INTERNAL_ERROR" to VoiceFailure.UNAVAILABLE,
            "UNAUTHENTICATED" to VoiceFailure.UNAVAILABLE,
            "UNAVAILABLE_LANGUAGE" to VoiceFailure.UNAVAILABLE,
            "UNKNOWN" to VoiceFailure.UNAVAILABLE,
            "새로_생긴_오류" to VoiceFailure.UNAVAILABLE,
        )
        expected.forEach { (name, failure) ->
            val got = mutableListOf<VoiceEvent>()
            collect(got)
            client.send(SttEvent.Ready, SttEvent.Error(name))
            runCurrent()
            assertEquals(name, VoiceEvent.Failed(failure), got.last())
        }
    }

    @Test
    fun `최대 듣기 시간이 지나면 멈추고 말소리 없음이다`() = runTest {
        val got = mutableListOf<VoiceEvent>()
        val job = collect(got)
        client.send(SttEvent.Ready, SttEvent.Updated("음", false))

        advanceTimeBy(14_999.milliseconds)
        assertEquals(false, job.isCompleted)
        advanceTimeBy(2.milliseconds)

        assertEquals(VoiceEvent.Failed(VoiceFailure.NO_SPEECH), got.last())
        assertEquals(1, client.closes)
    }

    @Test
    fun `구독을 취소하거나 끝나면 클라이언트를 한 번만 닫는다`() = runTest {
        val job = collect(mutableListOf())
        client.send(SttEvent.Ready)
        job.cancel()
        runCurrent()
        assertEquals(1, client.starts)
        assertEquals(1, client.closes)

        collect(mutableListOf())
        client.send(SttEvent.Ready, SttEvent.Updated("토해요", completed = true))
        runCurrent()
        assertEquals(2, client.starts)
        assertEquals(2, client.closes)
    }

    @Test
    fun `끝난 뒤에 오는 이벤트는 무시한다`() = runTest {
        val got = mutableListOf<VoiceEvent>()
        collect(got)
        client.send(SttEvent.Ready, SttEvent.Updated("토해요", completed = true))
        runCurrent()

        client.send(SttEvent.Updated("늦게 온 글자", false), SttEvent.Error("UNKNOWN"))
        runCurrent()

        assertEquals(listOf(VoiceEvent.Ready, VoiceEvent.Final("토해요")), got)
    }

    @Test
    fun `클라이언트 시작이 예외를 던지면 쓸 수 없음으로 끝난다`() = runTest {
        val broken = RealVoiceInput(FakeClient(throwOnStart = true))

        assertEquals(listOf<VoiceEvent>(VoiceEvent.Failed(VoiceFailure.UNAVAILABLE)), broken.listen().toList())
    }
}
