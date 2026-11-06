package com.meongcare.app.data.guidance

import com.meongcare.app.domain.guidance.Destination
import com.meongcare.app.domain.guidance.GuidanceState
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** 012 단계 6-2: Fake 길안내의 처음 남은 시간을 목적지의 이동 어림에서 시작한다. */
@OptIn(ExperimentalCoroutinesApi::class)
class FakeGuidanceInitialRemainingTest {

    private val hospital = Destination("시연용 바른 동물병원", 37.367, 127.1083, expectedTravel = 19.minutes)

    @Test
    fun `처음 남은 시간을 목적지 어림값으로 정할 수 있다`() = runTest {
        val source = FakeGuidanceSource(backgroundScope, initialRemainingFor = { it.expectedTravel })
        source.start(hospital)
        advanceTimeBy(1.seconds + 1.seconds / 10)
        runCurrent()
        assertEquals(GuidanceState.Guiding(hospital, 19.minutes), source.state.value)
    }

    @Test
    fun `어림값이 없으면 기본 12분이다`() = runTest {
        val source = FakeGuidanceSource(backgroundScope, initialRemainingFor = { it.expectedTravel })
        val plain = hospital.copy(expectedTravel = null)
        source.start(plain)
        advanceTimeBy(1.seconds + 1.seconds / 10)
        runCurrent()
        assertEquals(GuidanceState.Guiding(plain, 12.minutes), source.state.value)
    }

    @Test
    fun `인자를 주지 않으면 예전처럼 12분이다`() = runTest {
        val source = FakeGuidanceSource(backgroundScope)
        source.start(hospital)
        advanceTimeBy(1.seconds + 1.seconds / 10)
        runCurrent()
        assertEquals(GuidanceState.Guiding(hospital, 12.minutes), source.state.value)
    }

    @Test
    fun `어림값이 0 이하면 기본값을 쓴다`() = runTest {
        val source = FakeGuidanceSource(backgroundScope, initialRemainingFor = { 0.minutes })
        source.start(hospital)
        advanceTimeBy(1.seconds + 1.seconds / 10)
        runCurrent()
        assertEquals(GuidanceState.Guiding(hospital, 12.minutes), source.state.value)
    }
}
