package com.meongcare.app.data.speech

import com.meongcare.app.domain.speech.SpeakResult
import com.meongcare.app.domain.speech.Speaker
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * 소리를 내지 않고 읽는 데 걸릴 만큼만 기다리는 말하기. Fake 모드와, 음성 서비스가 없는 일반 폰의 시연 모드에서 쓴다
 * (docs/plans 017 "설계 결정 4").
 *
 * - 걸리는 시간: [baseTime] + 글자당 [perCharTime]. 그 뒤 [SpeakResult.Done].
 * - 한 번에 하나씩, 부른 순서대로 ([Mutex]는 기다린 순서대로 넘겨준다).
 * - 취소하면 바로 멈추고 다음 요청이 시작된다.
 * - 화면이 없으므로 시연용 표시도 없다. 로그도 남기지 않는다 (안내자가 남긴다).
 */
class FakeSpeaker(
    private val baseTime: Duration = 400.milliseconds,
    private val perCharTime: Duration = 120.milliseconds,
) : Speaker {

    private val oneAtATime = Mutex()

    override suspend fun speak(text: String): SpeakResult = oneAtATime.withLock {
        delay(baseTime + perCharTime * text.length)
        SpeakResult.Done
    }
}
