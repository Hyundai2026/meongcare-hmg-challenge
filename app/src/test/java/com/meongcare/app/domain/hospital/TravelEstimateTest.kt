package com.meongcare.app.domain.hospital

import com.meongcare.app.domain.guidance.Destination
import com.meongcare.app.domain.location.CurrentLocation
import kotlin.time.Duration.Companion.minutes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TravelEstimateTest {

    private val demoLocation = CurrentLocation(37.4127212, 127.094681, "수정구 시흥동")

    @Test
    fun `같은 좌표면 1분이다`() {
        val here = Destination("여기", demoLocation.latitude, demoLocation.longitude)
        assertEquals(1.minutes, estimateTravel(demoLocation, here))
    }

    @Test
    fun `시연 병원까지는 내비 관찰값 35분에 가깝다`() {
        // docs/pleos/sdk-notes.md [obs-navi-emu]: 내비로 12km·35분
        val demoHospital = Destination("시연용 동물병원", 37.48544722, 127.03636666)
        val minutes = estimateTravel(demoLocation, demoHospital).inWholeMinutes
        assertTrue("어림값 $minutes 분", minutes in 32..38)
    }

    @Test
    fun `시연 병원까지 직선 거리는 약 9_6km다`() {
        val km = TravelEstimate.straightDistanceKm(37.4127212, 127.094681, 37.48544722, 127.03636666)
        assertEquals(9.59, km, 0.02)
    }

    @Test
    fun `분은 올림한다`() {
        // 21km/h ÷ 1.25 = 16.8km/h → 1분에 0.28km. 0.29km는 1분을 조금 넘는다.
        assertEquals(1, TravelEstimate.minutesForKm(0.28))
        assertEquals(2, TravelEstimate.minutesForKm(0.29))
        assertEquals(1, TravelEstimate.minutesForKm(0.0))
    }

    @Test
    fun `정확히 나누어떨어지는 거리는 한 분 더 올리지 않는다`() {
        // 2.8km × 1.25 ÷ 21 × 60 = 10분 (부동소수 오차가 있어도 11분이 되지 않는다)
        assertEquals(10, TravelEstimate.minutesForKm(2.8))
    }

    @Test
    fun `멀수록 길어진다`() {
        val near = Destination("가까운 곳", 37.4200, 127.0947)
        val far = Destination("먼 곳", 37.5000, 127.0947)
        assertTrue(estimateTravel(demoLocation, near) < estimateTravel(demoLocation, far))
    }
}
