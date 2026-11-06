package com.meongcare.app.di

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class TripTimingTest {

    @Test
    fun `시연용 길안내는 도착 예정을 4초에 한 번까지 보낸다`() {
        assertEquals(4.seconds, tripTimingOf(isGuidanceSimulated = true).etaInterval)
    }

    @Test
    fun `실제 길안내는 1분에 한 번까지 보낸다`() {
        assertEquals(60.seconds, tripTimingOf(isGuidanceSimulated = false).etaInterval)
    }

    @Test
    fun `병원 확인은 시연 4초 실제 15초마다 읽는다`() {
        assertEquals(4.seconds, tripTimingOf(isGuidanceSimulated = true).ackPollInterval)
        assertEquals(15.seconds, tripTimingOf(isGuidanceSimulated = false).ackPollInterval)
    }

    @Test
    fun `구성표에서 시연용 길안내인 모드만 짧은 간격이다`() {
        val expected = mapOf(
            (DataMode.FAKE to true) to 4.seconds,
            (DataMode.FAKE to false) to 4.seconds,
            (DataMode.DEMO to true) to 4.seconds,
            (DataMode.DEMO to false) to 4.seconds,
            (DataMode.REAL to true) to 60.seconds,
            (DataMode.REAL to false) to 60.seconds,
        )
        for ((key, interval) in expected) {
            val (mode, automotive) = key
            val plan = sourcePlanOf(mode, automotive)
            assertEquals("$mode, 차량 기기=$automotive", interval, tripTimingOf(plan.isGuidanceSimulated).etaInterval)
        }
    }
}
