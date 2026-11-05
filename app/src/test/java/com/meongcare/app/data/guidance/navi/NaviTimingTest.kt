package com.meongcare.app.data.guidance.navi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class NaviTimingTest {
    @Test
    fun `내비의 남은 초를 남은 시간으로 바꾼다`() {
        assertEquals(35.minutes, naviRemainingTimeOf(2100))
        assertEquals(1.seconds, naviRemainingTimeOf(1))
        assertEquals(1.days, naviRemainingTimeOf(86_400))
    }

    @Test
    fun `0 이하나 하루보다 긴 값은 모름이다`() {
        assertNull(naviRemainingTimeOf(0))
        assertNull(naviRemainingTimeOf(-60))
        assertNull(naviRemainingTimeOf(86_401))
        assertNull(naviRemainingTimeOf(Int.MAX_VALUE))
        assertNull(naviRemainingTimeOf(Int.MIN_VALUE))
    }
}
