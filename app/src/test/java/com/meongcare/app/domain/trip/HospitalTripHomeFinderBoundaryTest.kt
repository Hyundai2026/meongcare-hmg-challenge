package com.meongcare.app.domain.trip

import app.cash.turbine.test
import com.meongcare.app.data.hospital.FakeCaseSender
import com.meongcare.app.data.pet.SamplePetProfiles
import com.meongcare.app.domain.card.Symptom
import com.meongcare.app.domain.guidance.Destination
import com.meongcare.app.domain.guidance.GuidanceSource
import com.meongcare.app.domain.guidance.GuidanceState
import com.meongcare.app.domain.hospital.HospitalCandidate
import com.meongcare.app.ui.hospital.DemoHospitals
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 계획 019 단계 4 `openFinderWithoutCard`·`finderWithoutCard` 경계: 안내 취소·도착 뒤, 시연 병원 출발, 연속 값. */
@OptIn(ExperimentalCoroutinesApi::class)
class HospitalTripHomeFinderBoundaryTest {

    private class FakeGuidance : GuidanceSource {
        val flow = MutableStateFlow<GuidanceState>(GuidanceState.Idle)
        val started = mutableListOf<Destination>()
        override val state: StateFlow<GuidanceState> = flow
        override suspend fun start(destination: Destination) {
            started += destination
            flow.value = GuidanceState.Guiding(destination, null)
        }
        override suspend fun cancel() {
            if (flow.value is GuidanceState.Guiding) flow.value = GuidanceState.Idle
        }
        override fun dismissArrival() {
            if (flow.value is GuidanceState.Arrived) flow.value = GuidanceState.Idle
        }
    }

    private val demo = DemoHospitals.all.first()
    private val card = caseCardOf(SamplePetProfiles.demoPet, setOf(Symptom.COUGH))

    @Test
    fun `시연 병원으로 카드와 함께 출발하면 시연 전송을 고르고 서버로는 보내지 않는다`() = runTest {
        // 사용자 결정 2026-11-13: 시안 병원도 카드를 받는다 (앱 안에서만, BUILT_IN → Fake 전송)
        val guidance = FakeGuidance()
        val asked = mutableListOf<HospitalCandidate>()
        val trip = HospitalTrip(guidance, { asked += it; FakeCaseSender() }, backgroundScope)

        trip.openFinder()
        trip.choose(demo.row)
        trip.depart(demo.row, card)
        runCurrent()

        assertEquals(listOf(demo.row.candidate), asked)
        // 시연 전송이 실패하지 않는다 (병원 ID가 있어야 FakeCaseSender가 성공한다, 2026-11-14 버그 수정)
        assertEquals(CardDelivery.Sent(toServer = false), trip.trip.value?.delivery)
        assertFalse(demo.row.candidate.sendsCardToServer)
        assertEquals(listOf(demo.row.hospital.destination.copy(expectedTravel = demo.travelMinutes.minutes)), guidance.started)
    }

    @Test
    fun `홈에서 카드 없이 출발하면 보내지 않고 길안내만 한다`() = runTest {
        val guidance = FakeGuidance()
        val trip = HospitalTrip(guidance, { error("카드가 없으면 전송 구현을 고르지 않는다") }, backgroundScope)

        trip.openFinderWithoutCard()
        trip.choose(demo.row)
        trip.depart(demo.row, card = null)
        runCurrent()

        assertEquals(TripState(demo.row.candidate, CardDelivery.NotSent), trip.trip.value)
        assertFalse(trip.finderWithoutCard.value)
    }

    @Test
    fun `안내 중이나 도착 화면에서는 홈 병원 찾기가 열리지 않는다`() = runTest {
        val guidance = FakeGuidance()
        val trip = HospitalTrip(guidance, { error("보내지 않는다") }, backgroundScope)
        trip.openFinderWithoutCard()
        trip.depart(demo.row, card = null)
        runCurrent()

        trip.openFinderWithoutCard()
        assertEquals(FinderStep.None, trip.finder.value)
        assertFalse(trip.finderWithoutCard.value)

        guidance.flow.value = GuidanceState.Arrived(demo.row.hospital.destination)
        runCurrent()
        trip.openFinderWithoutCard()
        assertEquals(FinderStep.None, trip.finder.value)
        assertFalse(trip.finderWithoutCard.value)
    }

    @Test
    fun `도착 화면을 닫으면 다시 카드 없이 열 수 있다`() = runTest {
        val guidance = FakeGuidance()
        val trip = HospitalTrip(guidance, { error("보내지 않는다") }, backgroundScope)
        trip.openFinderWithoutCard()
        trip.depart(demo.row, card = null)
        runCurrent()
        guidance.flow.value = GuidanceState.Arrived(demo.row.hospital.destination)
        runCurrent()

        trip.clear()
        trip.openFinderWithoutCard()
        assertEquals(FinderStep.Hospitals, trip.finder.value)
        assertTrue(trip.finderWithoutCard.value)
    }

    @Test
    fun `안내가 취소되면 고른 병원이 비고 다시 카드 없이 열 수 있다`() = runTest {
        val guidance = FakeGuidance()
        val trip = HospitalTrip(guidance, { error("보내지 않는다") }, backgroundScope)
        trip.openFinderWithoutCard()
        trip.depart(demo.row, card = null)
        runCurrent()

        guidance.cancel()
        runCurrent()
        assertNull(trip.trip.value)
        assertFalse(trip.finderWithoutCard.value)

        trip.openFinderWithoutCard()
        assertEquals(FinderStep.Hospitals, trip.finder.value)
        assertTrue(trip.finderWithoutCard.value)
    }

    @Test
    fun `목록에서 고르고 확인 화면에 있는 동안 표시가 유지된다`() = runTest {
        val trip = HospitalTrip(FakeGuidance(), { error("보내지 않는다") }, backgroundScope)
        trip.openFinderWithoutCard()
        trip.choose(demo.row)
        assertEquals(FinderStep.Confirm(demo.row), trip.finder.value)
        assertTrue(trip.finderWithoutCard.value)
        trip.choose(DemoHospitals.all[1].row)
        assertEquals(FinderStep.Confirm(DemoHospitals.all[1].row), trip.finder.value)
        assertTrue(trip.finderWithoutCard.value)
    }

    @Test
    fun `열지 않은 상태에서 뒤로·닫기를 눌러도 표시는 꺼진 채다`() = runTest {
        val trip = HospitalTrip(FakeGuidance(), { error("보내지 않는다") }, backgroundScope)
        trip.back()
        trip.closeFinder()
        trip.choose(demo.row)
        assertEquals(FinderStep.None, trip.finder.value)
        assertFalse(trip.finderWithoutCard.value)
    }

    @Test
    fun `표시는 처음 false, 열면 true, 목록 뒤로면 false 순서로 나온다`() = runTest {
        val trip = HospitalTrip(FakeGuidance(), { error("보내지 않는다") }, backgroundScope)
        trip.finderWithoutCard.test {
            assertFalse(awaitItem())
            trip.openFinderWithoutCard()
            assertTrue(awaitItem())
            trip.choose(demo.row)
            trip.back()
            expectNoEvents()
            trip.back()
            assertFalse(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
