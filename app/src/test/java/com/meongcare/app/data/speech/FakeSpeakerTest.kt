package com.meongcare.app.data.speech

import com.meongcare.app.domain.speech.SpeakResult
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds

class FakeSpeakerTest {

    @Test
    fun `글자 수에 맞춘 시간이 지나면 끝까지 읽었다고 돌려준다`() = runTest {
        val speaker = FakeSpeaker(baseTime = 400.milliseconds, perCharTime = 100.milliseconds)

        val result = speaker.speak("다섯글자다")

        assertEquals(SpeakResult.Done, result)
        assertEquals(400L + 5 * 100L, currentTime)
    }

    @Test
    fun `한 번에 하나씩 부른 순서대로 읽는다`() = runTest {
        val speaker = FakeSpeaker(baseTime = 1000.milliseconds, perCharTime = 0.milliseconds)
        val finished = mutableListOf<Pair<String, Long>>()

        launch { speaker.speak("첫째"); finished += "첫째" to currentTime }
        launch { speaker.speak("둘째"); finished += "둘째" to currentTime }
        launch { speaker.speak("셋째"); finished += "셋째" to currentTime }
        advanceTimeBy(3_001)

        assertEquals(listOf("첫째" to 1000L, "둘째" to 2000L, "셋째" to 3000L), finished)
    }

    @Test
    fun `취소하면 읽기를 멈추고 기다리던 다음 요청을 바로 시작한다`() = runTest {
        val speaker = FakeSpeaker(baseTime = 1000.milliseconds, perCharTime = 0.milliseconds)
        var firstFinished = false

        val first = launch { speaker.speak("첫째"); firstFinished = true }
        val second = async { speaker.speak("둘째") to currentTime }
        advanceTimeBy(300)
        runCurrent()
        first.cancel()
        runCurrent()

        val (result, finishedAt) = second.await()
        assertFalse(firstFinished)
        assertEquals(SpeakResult.Done, result)
        assertEquals("둘째는 취소 시점(300ms)부터 읽어 1300ms에 끝난다", 1300L, finishedAt)
    }
}
