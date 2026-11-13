package com.meongcare.app.domain.trip

import com.meongcare.app.domain.guidance.Destination
import com.meongcare.app.domain.guidance.GuidanceSource
import com.meongcare.app.domain.guidance.GuidanceState
import com.meongcare.app.ui.hospital.DemoHospitals
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 홈 [병원 찾기]: 카드 없이 연 병원 찾기 (계획 019) */
class HospitalTripHomeFinderTest {
    private class StillGuidance : GuidanceSource {
        override val state: StateFlow<GuidanceState> = MutableStateFlow(GuidanceState.Idle)
        override suspend fun start(destination: Destination) = Unit
        override suspend fun cancel() = Unit
        override fun dismissArrival() = Unit
    }

    private val choice = DemoHospitals.all.first().row

    @Test
    fun `카드 없이 열면 목록이고 표시가 켜진다`() = runTest {
        val trip = HospitalTrip(StillGuidance(), { error("보내지 않는다") }, backgroundScope)
        trip.openFinderWithoutCard()
        assertEquals(FinderStep.Hospitals, trip.finder.value)
        assertTrue(trip.finderWithoutCard.value)
    }

    @Test
    fun `목록 뒤로·닫기·출발·비우기면 표시가 꺼진다`() = runTest {
        val trip = HospitalTrip(StillGuidance(), { error("보내지 않는다") }, backgroundScope)
        trip.openFinderWithoutCard()
        trip.choose(choice)
        trip.back()
        assertTrue("확인 → 목록은 아직 켜져 있다", trip.finderWithoutCard.value)
        trip.back()
        assertFalse(trip.finderWithoutCard.value)

        trip.openFinderWithoutCard()
        trip.closeFinder()
        assertFalse(trip.finderWithoutCard.value)

        trip.openFinderWithoutCard()
        trip.choose(choice)
        trip.depart(choice, card = null)
        assertFalse(trip.finderWithoutCard.value)
        assertEquals(FinderStep.None, trip.finder.value)

        trip.clear()
        assertFalse(trip.finderWithoutCard.value)
    }

    @Test
    fun `출발한 뒤에는 열리지 않는다`() = runTest {
        val trip = HospitalTrip(StillGuidance(), { error("보내지 않는다") }, backgroundScope)
        trip.depart(choice, card = null)
        trip.openFinderWithoutCard()
        assertEquals(FinderStep.None, trip.finder.value)
        assertFalse(trip.finderWithoutCard.value)
    }

    @Test
    fun `기존 openFinder는 표시를 켜지 않는다`() = runTest {
        val trip = HospitalTrip(StillGuidance(), { error("보내지 않는다") }, backgroundScope)
        trip.openFinder()
        assertFalse(trip.finderWithoutCard.value)
    }

    @Test
    fun `시연 병원은 카드를 받는 앱 내장 병원이고 가까운 순이다`() {
        val all = DemoHospitals.all
        assertEquals(listOf(1, 2, 3, 4, 5), all.map { it.rank })
        assertTrue(all.all { it.row.candidate.canReceiveCard && !it.row.candidate.sendsCardToServer && it.row.hospital.isSample })
        assertEquals(all.sortedBy { it.distanceKm }, all)
    }
}
