package com.meongcare.app.data.hospital

import com.meongcare.app.data.location.FakeCurrentLocationSource
import com.meongcare.app.domain.hospital.NearbyVets
import com.meongcare.app.domain.hospital.TravelEstimate
import com.meongcare.app.domain.location.CurrentLocation
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 계획 015 "설계 결정 7" `FakeNearbyVetSource`의 경계 (tester). */
@OptIn(ExperimentalCoroutinesApi::class)
class FakeNearbyVetSourceBoundaryTest {

    private val here = FakeCurrentLocationSource.DEMO_LOCATION

    @Test
    fun `기본 지연 600ms가 지나야 결과가 온다`() = runTest {
        val pending = async { FakeNearbyVetSource().search(here) }
        advanceTimeBy(599.milliseconds)
        runCurrent()
        assertFalse(pending.isCompleted)
        advanceTimeBy(2.milliseconds)
        runCurrent()
        assertTrue(pending.isCompleted)
        assertEquals(3, (pending.await() as NearbyVets.Found).hospitals.size)
    }

    @Test
    fun `기본 3곳은 시연 위치에서 약 1km 2점5km 4km다`() {
        val km = FakeNearbyVetSource.DEFAULT.map {
            TravelEstimate.straightDistanceKm(here.latitude, here.longitude, it.destination.latitude, it.destination.longitude)
        }
        listOf(1.0, 2.5, 4.0).zip(km).forEach { (expected, actual) ->
            assertEquals("expected≈$expected km", expected, actual, 0.15)
        }
    }

    @Test
    fun `기본 3곳은 모두 시연용이고 제휴 병원 ID가 없고 번호는 걸리지 않는 형식이다`() {
        FakeNearbyVetSource.DEFAULT.forEach { h ->
            assertTrue(h.name, h.isSample)
            assertEquals(null, h.id)
            h.phoneNumber?.let { assertTrue(it, it.matches(Regex("02-0000-00\\d\\d"))) }
        }
        assertEquals(3, FakeNearbyVetSource.DEFAULT.map { it.name }.toSet().size)
    }

    @Test
    fun `결과 함수는 부른 중심 좌표를 받는다`() = runTest {
        val seen = mutableListOf<CurrentLocation>()
        val elsewhere = CurrentLocation(37.5665, 126.978, null)
        FakeNearbyVetSource(delay = 0.milliseconds) { seen += it; NearbyVets.Found(emptyList()) }.search(elsewhere)
        assertEquals(listOf(elsewhere), seen)
    }
}
