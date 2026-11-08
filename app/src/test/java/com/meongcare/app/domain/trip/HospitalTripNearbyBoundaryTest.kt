package com.meongcare.app.domain.trip

import com.meongcare.app.data.pet.SamplePetProfiles
import com.meongcare.app.domain.card.Symptom
import com.meongcare.app.domain.guidance.Destination
import com.meongcare.app.domain.guidance.GuidanceSource
import com.meongcare.app.domain.guidance.GuidanceState
import com.meongcare.app.domain.hospital.Congestion
import com.meongcare.app.domain.hospital.Hospital
import com.meongcare.app.domain.hospital.HospitalCandidate
import com.meongcare.app.domain.hospital.HospitalOrigin
import com.meongcare.app.domain.hospital.HospitalStatus
import com.meongcare.app.domain.hospital.RankedHospital
import com.meongcare.app.domain.hospital.StatusFreshness
import com.meongcare.app.domain.hospital.exampleStatusOf
import java.time.Instant
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 계획 015 "설계 결정 8"의 `HospitalTrip` 계약 경계: 근처 검색 병원에는 카드를 보내지 않는다 (tester). */
@OptIn(ExperimentalCoroutinesApi::class)
class HospitalTripNearbyBoundaryTest {

    private val base = Instant.parse("2026-11-08T05:00:00Z")

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

    /** 무엇이든 기록하는 전송 */
    private class RecordingSender : CaseSender {
        val calls = mutableListOf<String>()
        override suspend fun send(hospital: Hospital, card: CaseCard, eta: CaseEta): SendResult {
            calls += "send"
            return SendResult.Sent(CaseRef(hospital.id ?: "none", "case-1"))
        }
        override suspend fun updateEta(ref: CaseRef, eta: CaseEta): Boolean {
            calls += "eta"
            return true
        }
        override suspend fun finish(ref: CaseRef, status: CaseStatus, at: Instant): Boolean {
            calls += "finish:$status"
            return true
        }
        override suspend fun readAck(ref: CaseRef): AckRead {
            calls += "ack"
            return AckRead.Pending
        }
    }

    private val vet = Hospital(Destination("근처 동물병원", 37.42, 127.09), "031-000-0000", isSample = false)

    /** 설계 변경 V2: 섞은 목록의 근처 병원 줄은 예시 대기를 단다 */
    private val nearbyWithExample = RankedHospital(
        candidate = HospitalCandidate(vet, status = exampleStatusOf(vet, base), origin = HospitalOrigin.NEARBY_SEARCH),
        travel = 6.minutes,
        freshness = StatusFreshness.FRESH,
        statusAge = 0.minutes,
    )

    private val partnerChoice = RankedHospital(
        candidate = HospitalCandidate(
            Hospital(Destination("제휴", 37.43, 127.09), "02-0000-0001", isSample = true, id = "demo-fast"),
            HospitalStatus(10, Congestion.NORMAL, true, base),
            HospitalOrigin.SERVER,
        ),
        travel = 7.minutes,
        freshness = StatusFreshness.FRESH,
        statusAge = 0.minutes,
    )

    private val card = caseCardOf(SamplePetProfiles.bori, setOf(Symptom.COUGH))

    @Test
    fun `예시 대기를 단 근처 병원 줄로 출발해도 카드를 보내지 않고 이동 어림을 실어 길안내한다`() = runTest {
        val guidance = FakeGuidance()
        val sender = RecordingSender()
        var asked = 0
        val trip = HospitalTrip(guidance, { asked++; sender }, backgroundScope, now = { base })

        trip.depart(nearbyWithExample, card)
        runCurrent()

        assertEquals(0, asked)
        assertEquals(CardDelivery.NotSent, trip.trip.value?.delivery)
        assertEquals(listOf(vet.destination.copy(expectedTravel = 6.minutes)), guidance.started)
        assertEquals(vet, trip.selectedHospital.value)
    }

    @Test
    fun `근처 병원으로 카드 없이 출발해도 같다`() = runTest {
        val guidance = FakeGuidance()
        var asked = 0
        val trip = HospitalTrip(guidance, { asked++; RecordingSender() }, backgroundScope, now = { base })
        trip.depart(nearbyWithExample, card = null)
        runCurrent()
        assertEquals(0, asked)
        assertEquals(TripState(nearbyWithExample.candidate, CardDelivery.NotSent), trip.trip.value)
    }

    @Test
    fun `근처 병원 안내 중 남은 시간이 바뀌어도 병원 확인을 읽지 않고 아무것도 보내지 않는다`() = runTest {
        val guidance = FakeGuidance()
        val sender = RecordingSender()
        val trip = HospitalTrip(guidance, { sender }, backgroundScope, now = { base }, etaInterval = 1.seconds, ackPollInterval = 1.seconds)
        trip.depart(nearbyWithExample, card)
        runCurrent()
        guidance.flow.value = GuidanceState.Guiding(vet.destination, 5.minutes)
        advanceTimeBy(30.seconds)
        guidance.flow.value = GuidanceState.Guiding(vet.destination, 3.minutes)
        advanceTimeBy(30.seconds)
        runCurrent()
        assertTrue(sender.calls.toString(), sender.calls.isEmpty())
        // 진행 막대 시작값은 그대로 쌓인다 (카드와 상관없는 길안내 화면 기능)
        assertEquals(5, trip.trip.value?.startMinutes)
    }

    @Test
    fun `근처 병원 안내를 취소하면 아무것도 보내지 않고 고른 병원을 비운다`() = runTest {
        val guidance = FakeGuidance()
        val sender = RecordingSender()
        val trip = HospitalTrip(guidance, { sender }, backgroundScope, now = { base })
        trip.depart(nearbyWithExample, card)
        runCurrent()
        guidance.cancel()
        runCurrent()
        assertTrue(sender.calls.isEmpty())
        assertNull(trip.trip.value)
        assertNull(trip.selectedHospital.value)
    }

    @Test
    fun `근처 병원으로 출발한 뒤 다시 보내기를 눌러도 아무것도 보내지 않는다`() = runTest {
        val guidance = FakeGuidance()
        val sender = RecordingSender()
        val trip = HospitalTrip(guidance, { sender }, backgroundScope, now = { base })
        trip.depart(nearbyWithExample, card)
        runCurrent()
        trip.resend()
        runCurrent()
        assertTrue(sender.calls.isEmpty())
        assertEquals(CardDelivery.NotSent, trip.trip.value?.delivery)
    }

    @Test
    fun `근처 병원 안내를 닫은 뒤 제휴 병원으로 출발하면 카드를 보낸다`() = runTest {
        val guidance = FakeGuidance()
        val sender = RecordingSender()
        val asked = mutableListOf<HospitalCandidate>()
        val trip = HospitalTrip(guidance, { asked += it; sender }, backgroundScope, now = { base })
        trip.depart(nearbyWithExample, card)
        runCurrent()
        guidance.flow.value = GuidanceState.Arrived(vet.destination)
        runCurrent()
        trip.clear()
        guidance.dismissArrival()

        trip.depart(partnerChoice, card)
        runCurrent()
        assertEquals(listOf(partnerChoice.candidate), asked)
        assertEquals("send", sender.calls.first())
        assertEquals(CardDelivery.Sent(toServer = true), trip.trip.value?.delivery)
    }

    @Test
    fun `이미 근처 병원으로 출발했으면 제휴 병원으로 다시 출발해도 무시한다`() = runTest {
        val guidance = FakeGuidance()
        val sender = RecordingSender()
        val trip = HospitalTrip(guidance, { sender }, backgroundScope, now = { base })
        trip.depart(nearbyWithExample, card)
        runCurrent()
        trip.depart(partnerChoice, card)
        runCurrent()
        assertTrue(sender.calls.isEmpty())
        assertEquals(nearbyWithExample.candidate, trip.trip.value?.candidate)
        assertEquals(1, guidance.started.size)
    }
}
