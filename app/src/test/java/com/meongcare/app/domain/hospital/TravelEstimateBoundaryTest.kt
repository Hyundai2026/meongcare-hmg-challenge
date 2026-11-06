package com.meongcare.app.domain.hospital

import com.meongcare.app.domain.guidance.Destination
import com.meongcare.app.domain.location.CurrentLocation
import kotlin.time.Duration.Companion.minutes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 이동 시간 어림 경계. 기준: 계획 012 "설계 결정 1"
 * (`분 = 올림(직선km × 1.25 ÷ 21km/h × 60)`, 최소 1분, 하버사인 직선 거리).
 */
class TravelEstimateBoundaryTest {

    private val demo = CurrentLocation(37.4127212, 127.094681, "수정구 시흥동")

    @Test
    fun `오가는 방향을 바꿔도 직선 거리와 이동 어림은 같다`() {
        val a = CurrentLocation(37.4127212, 127.094681, null)
        val b = Destination("병원", 37.48544722, 127.03636666)
        val there = estimateTravel(a, b)
        val back = estimateTravel(CurrentLocation(b.latitude, b.longitude, null), Destination("출발", a.latitude, a.longitude))
        assertEquals(there, back)
        assertEquals(
            TravelEstimate.straightDistanceKm(a.latitude, a.longitude, b.latitude, b.longitude),
            TravelEstimate.straightDistanceKm(b.latitude, b.longitude, a.latitude, a.longitude),
            1e-9,
        )
    }

    @Test
    fun `1m만 떨어져도 최소 1분이다`() {
        val oneMeterNorth = Destination("병원", demo.latitude + 0.001 / 111.195, demo.longitude)
        assertEquals(1.minutes, estimateTravel(demo, oneMeterNorth))
    }

    @Test
    fun `0km와 음수가 아닌 아주 작은 거리는 1분이다`() {
        assertEquals(1, TravelEstimate.minutesForKm(0.0))
        assertEquals(1, TravelEstimate.minutesForKm(1e-9))
    }

    @Test
    fun `정확히 N분 거리에서 1m만 멀어져도 한 분 올린다`() {
        // 10분 = 10 × 21 ÷ 1.25 ÷ 60 = 2.8km
        assertEquals(10, TravelEstimate.minutesForKm(2.8))
        assertEquals(11, TravelEstimate.minutesForKm(2.801))
    }

    @Test
    fun `날짜 변경선을 넘는 두 점은 짧은 쪽으로 잰다`() {
        val km = TravelEstimate.straightDistanceKm(0.0, 179.9, 0.0, -179.9)
        // 경도 0.2도 ≈ 22.2km. 지구를 반대로 도는 약 4만km가 아니다
        assertEquals(22.24, km, 0.05)
    }

    @Test
    fun `지구 반대편까지도 유한한 분이고 반 바퀴 거리다`() {
        val km = TravelEstimate.straightDistanceKm(37.0, 127.0, -37.0, -53.0)
        assertEquals(Math.PI * TravelEstimate.EARTH_RADIUS_KM, km, 0.01)
        val minutes = TravelEstimate.minutesForKm(km)
        assertTrue("분: $minutes", minutes in 70_000..72_000)
    }

    @Test
    fun `이동 어림은 늘 분 단위로 떨어진다`() {
        listOf(0.3, 1.7, 5.22, 9.59, 33.3).forEach { km ->
            val to = Destination("병원", demo.latitude + km / 111.195, demo.longitude)
            val travel = estimateTravel(demo, to)
            assertEquals("$km km", 0L, travel.inWholeSeconds % 60)
        }
    }
}
