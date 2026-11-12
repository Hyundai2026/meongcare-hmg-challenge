package com.meongcare.app.data.speech

import com.meongcare.app.domain.speech.SpeakFailure
import com.meongcare.app.domain.speech.SpeakResult
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class RealSpeakerTest {

    /** 말하기 요청을 기록하고, 테스트가 이벤트를 보낼 수 있게 한다. */
    private class FakeClient(private val failWith: Exception? = null) : TextToSpeechClient {
        val texts = mutableListOf<String>()
        var closeCount = 0
        private val sessions = mutableListOf<(TtsEvent) -> Unit>()

        override fun speak(text: String, onEvent: (TtsEvent) -> Unit): AutoCloseable {
            failWith?.let { throw it }
            texts += text
            sessions += onEvent
            return AutoCloseable { closeCount++ }
        }

        fun send(vararg events: TtsEvent) = events.forEach { sessions.last()(it) }
    }

    private val short = "열 글자짜리 문장이다"

    @Test
    fun `초기화되고 끝까지 읽으면 끝까지 읽었다를 돌려주고 클라이언트를 닫는다`() = runTest {
        val client = FakeClient()
        val speaker = RealSpeaker(client)

        val result = async { speaker.speak(short) }
        runCurrent()
        client.send(TtsEvent.Ready, TtsEvent.Started)
        advanceTimeBy(4_000)
        client.send(TtsEvent.Done)

        assertEquals(SpeakResult.Done, result.await())
        assertEquals(listOf(short), client.texts)
        assertEquals(1, client.closeCount)
    }

    @Test
    fun `멈춤 이벤트가 오면 멈췄다를 돌려준다`() = runTest {
        val client = FakeClient()
        val speaker = RealSpeaker(client)

        val result = async { speaker.speak(short) }
        runCurrent()
        client.send(TtsEvent.Ready, TtsEvent.Started, TtsEvent.Stopped)

        assertEquals(SpeakResult.Stopped, result.await())
        assertEquals(1, client.closeCount)
    }

    @Test
    fun `오디오 오류는 소리 문제로 나머지 오류와 초기화 실패는 쓸 수 없음으로 나눈다`() = runTest {
        val names = listOf(
            "AUDIO_ERROR", "MIC_ERROR", "SERVER_INTERNAL_ERROR", "NETWORK_ERROR", "INVALID_REQUEST_ERROR", "TIMEOUT",
            "UNAUTHENTICATED", "PERMISSION_DENIED", "ON_DEVICE_ENGINE_ERROR", "UNKNOWN", "UNAVAILABLE_LANGUAGE",
            "NOT_SUPPORTED_API", "SOMETHING_NEW",
        )
        for (name in names) {
            val client = FakeClient()
            val result = async { RealSpeaker(client).speak(short) }
            runCurrent()
            client.send(TtsEvent.Ready, TtsEvent.Error(name))
            val expected = if (name == "AUDIO_ERROR") SpeakFailure.AUDIO else SpeakFailure.UNAVAILABLE
            assertEquals(name, SpeakResult.Failed(expected), result.await())
        }

        val client = FakeClient()
        val result = async { RealSpeaker(client).speak(short) }
        runCurrent()
        client.send(TtsEvent.InitFailed("PERMISSION_DENIED"))
        assertEquals(SpeakResult.Failed(SpeakFailure.UNAVAILABLE), result.await())
    }

    @Test
    fun `시간 안에 응답이 없으면 쓸 수 없음이다`() = runTest {
        val client = FakeClient()
        val speaker = RealSpeaker(client, initTimeout = 3.seconds)

        val result = async { speaker.speak(short) }
        advanceTimeBy(2_999)
        runCurrent()
        assertFalse(result.isCompleted)
        advanceTimeBy(2)

        assertEquals(SpeakResult.Failed(SpeakFailure.UNAVAILABLE), result.await())
        assertEquals(1, client.closeCount)
    }

    @Test
    fun `글자 수에 맞춘 최대 읽기 시간이 지나면 닫고 쓸 수 없음이다`() = runTest {
        val client = FakeClient()
        val speaker = RealSpeaker(client, baseSpeakTime = 10.seconds, perCharSpeakTime = 250.milliseconds)
        val limit = 10_000L + 250L * short.length

        val result = async { speaker.speak(short) }
        runCurrent()
        client.send(TtsEvent.Ready, TtsEvent.Started)
        advanceTimeBy(limit - 1)
        runCurrent()
        assertFalse("응답이 온 뒤에는 이벤트가 와도 시간을 다시 재지 않는다", result.isCompleted)
        advanceTimeBy(2)

        assertEquals(SpeakResult.Failed(SpeakFailure.UNAVAILABLE), result.await())
        assertEquals(1, client.closeCount)
    }

    @Test
    fun `취소하면 클라이언트를 한 번만 닫는다`() = runTest {
        val client = FakeClient()
        val speaker = RealSpeaker(client)

        val job = launch { speaker.speak(short) }
        runCurrent()
        client.send(TtsEvent.Ready, TtsEvent.Started)
        runCurrent()
        job.cancel()
        runCurrent()
        client.send(TtsEvent.Stopped)
        runCurrent()

        assertTrue(job.isCancelled)
        assertEquals(1, client.closeCount)
    }

    @Test
    fun `한 번에 하나만 읽고 다음 요청은 앞 요청이 끝난 뒤 시작한다`() = runTest {
        val client = FakeClient()
        val speaker = RealSpeaker(client)

        val first = async { speaker.speak("첫째") }
        val second = async { speaker.speak("둘째") }
        runCurrent()
        assertEquals("앞 문장이 끝나기 전에는 다음 문장을 맡기지 않는다", listOf("첫째"), client.texts)

        client.send(TtsEvent.Ready, TtsEvent.Started, TtsEvent.Done)
        runCurrent()
        assertEquals(listOf("첫째", "둘째"), client.texts)
        client.send(TtsEvent.Ready, TtsEvent.Started, TtsEvent.Done)

        assertEquals(SpeakResult.Done, first.await())
        assertEquals(SpeakResult.Done, second.await())
        assertEquals(2, client.closeCount)
    }

    @Test
    fun `끝난 뒤 오는 이벤트는 무시한다`() = runTest {
        val client = FakeClient()
        val speaker = RealSpeaker(client)

        val result = async { speaker.speak(short) }
        runCurrent()
        client.send(TtsEvent.Ready, TtsEvent.Done)
        runCurrent()
        client.send(TtsEvent.Stopped, TtsEvent.Error("AUDIO_ERROR"))
        runCurrent()

        assertEquals(SpeakResult.Done, result.await())
        assertEquals(1, client.closeCount)
    }

    /** 포커스 요청·반납을 기록한다. [grant]가 false면 거절한다. */
    private class RecordingFocus(private val grant: Boolean = true) : AudioFocusGate {
        val calls = mutableListOf<String>()
        override fun request(): AutoCloseable? {
            calls += "요청"
            if (!grant) return null
            return AutoCloseable { calls += "반납" }
        }
    }

    @Test
    fun `포커스를 받은 뒤 읽고 끝나면 반납한다`() = runTest {
        val client = FakeClient()
        val focus = RecordingFocus()
        val speaker = RealSpeaker(client, focus = focus)

        val result = async { speaker.speak(short) }
        runCurrent()
        assertEquals(listOf("요청"), focus.calls)
        assertEquals(listOf(short), client.texts)
        client.send(TtsEvent.Ready, TtsEvent.Started, TtsEvent.Done)

        assertEquals(SpeakResult.Done, result.await())
        assertEquals(listOf("요청", "반납"), focus.calls)
    }

    @Test
    fun `포커스가 거절되면 읽지 않고 소리 문제다`() = runTest {
        val client = FakeClient()
        val speaker = RealSpeaker(client, focus = RecordingFocus(grant = false))

        assertEquals(SpeakResult.Failed(SpeakFailure.AUDIO), speaker.speak(short))
        assertEquals(emptyList<String>(), client.texts)
        assertEquals(0, client.closeCount)
    }

    @Test
    fun `취소하거나 실패해도 포커스를 반납한다`() = runTest {
        val client = FakeClient()
        val focus = RecordingFocus()
        val speaker = RealSpeaker(client, focus = focus)

        val cancelled = launch { speaker.speak(short) }
        runCurrent()
        cancelled.cancel()
        runCurrent()
        val failed = async { speaker.speak(short) }
        runCurrent()
        client.send(TtsEvent.Error("AUDIO_ERROR"))
        failed.await()

        assertEquals(listOf("요청", "반납", "요청", "반납"), focus.calls)
    }

    @Test
    fun `클라이언트가 예외를 던지면 쓸 수 없음이다`() = runTest {
        val speaker = RealSpeaker(FakeClient(failWith = IllegalStateException("서비스 없음")))

        assertEquals(SpeakResult.Failed(SpeakFailure.UNAVAILABLE), speaker.speak(short))
    }
}
