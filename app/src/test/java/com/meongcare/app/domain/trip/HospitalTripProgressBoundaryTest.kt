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
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** 013 진행 시작값 [TripState.startMinutes] 경계 (계획 013 설계 결정 4 "진행 시작값 규칙" 표) */
@OptIn(ExperimentalCoroutinesApi::class)
class HospitalTripProgressBoundaryTest {

    private val base = Instant.parse("2026-11-06T05:00:00Z")

    private class Guidance : GuidanceSource {
        val flow = MutableStateFlow<GuidanceState>(GuidanceState.Idle)
        override val state: StateFlow<GuidanceState> = flow
        override suspend fun start(destination: Destination) {
            flow.value = GuidanceState.Guiding(destination, null)
        }
        override suspend fun cancel() {
            if (flow.value is GuidanceState.Guiding) flow.value = GuidanceState.Idle
        }
        override fun dismissArrival() {
            if (flow.value is GuidanceState.Arrived) flow.value = GuidanceState.Idle
        }
        private val destination get() = (flow.value as GuidanceState.Guiding).destination
        fun remaining(duration: Duration?) {
            flow.value = GuidanceState.Guiding(destination, duration)
        }
        fun arrive() {
            flow.value = GuidanceState.Arrived(destination)
        }
    }

    private class Sender : CaseSender {
        override suspend fun send(hospital: Hospital, card: CaseCard, eta: CaseEta) = SendResult.Sent(CaseRef(hospital.id!!, "c1"))
        override suspend fun updateEta(ref: CaseRef, eta: CaseEta) = true
        override suspend fun finish(ref: CaseRef, status: CaseStatus, at: Instant) = true
    }

    private val hospital = Hospital(Destination("시연용 바른 동물병원", 37.367, 127.1083), "02-0000-0001", isSample = true, id = "demo-fast")

    private fun choice(origin: HospitalOrigin = HospitalOrigin.SERVER) = RankedHospital(
        candidate = HospitalCandidate(hospital, HospitalStatus(10, Congestion.NORMAL, true, base), origin),
        travel = 19.minutes,
        freshness = StatusFreshness.FRESH,
        statusAge = 2.minutes,
    )

    private val card = caseCardOf(SamplePetProfiles.demoPet, setOf(Symptom.COUGH))

    private fun TestScope.newTrip(): Pair<HospitalTrip, Guidance> {
        val guidance = Guidance()
        val trip = HospitalTrip(guidance, { Sender() }, backgroundScope, now = { base.plusMillis(testScheduler.currentTime) })
        return trip to guidance
    }

    @Test
    fun `남은 시간은 올림한 분으로 시작값이 된다`() = runTest {
        val cases = listOf(90.seconds to 2, 20.seconds to 1, 34.minutes + 1.seconds to 35, 12.minutes to 12)
        for ((remaining, expected) in cases) {
            val (trip, guidance) = newTrip()
            trip.depart(choice(), card)
            runCurrent()
            guidance.remaining(remaining)
            runCurrent()
            assertEquals("$remaining", expected, trip.trip.value?.startMinutes)
        }
    }

    @Test
    fun `카드 없이 출발해도 진행 시작값을 기억한다`() = runTest {
        val (trip, guidance) = newTrip()
        trip.depart(choice(), card = null)
        runCurrent()
        assertNull(trip.trip.value?.startMinutes)
        guidance.remaining(15.minutes)
        runCurrent()
        assertEquals(15, trip.trip.value?.startMinutes)
        assertEquals(CardDelivery.NotSent, trip.trip.value?.delivery)
    }

    @Test
    fun `내장 병원으로 출발해도 진행 시작값을 기억한다`() = runTest {
        for (origin in listOf(HospitalOrigin.BUILT_IN, HospitalOrigin.BUILT_IN_FALLBACK)) {
            val (trip, guidance) = newTrip()
            trip.depart(choice(origin), card)
            runCurrent()
            guidance.remaining(15.minutes)
            runCurrent()
            assertEquals("$origin", 15, trip.trip.value?.startMinutes)
            assertEquals("$origin", CardDelivery.Sent(toServer = false), trip.trip.value?.delivery)
        }
    }

    @Test
    fun `경로를 다시 찾은 뒤 더 큰 남은 분이면 시작을 올린다`() = runTest {
        val (trip, guidance) = newTrip()
        trip.depart(choice(), card)
        runCurrent()
        guidance.remaining(30.minutes)
        runCurrent()
        guidance.remaining(null) // 재탐색 중
        runCurrent()
        assertEquals(30, trip.trip.value?.startMinutes)
        guidance.remaining(40.minutes)
        runCurrent()
        assertEquals(40, trip.trip.value?.startMinutes)
        guidance.remaining(30.minutes)
        runCurrent()
        assertEquals(40, trip.trip.value?.startMinutes)
    }

    @Test
    fun `같은 남은 분이 다시 와도 시작은 그대로다`() = runTest {
        val (trip, guidance) = newTrip()
        trip.depart(choice(), card)
        runCurrent()
        guidance.remaining(30.minutes)
        runCurrent()
        guidance.remaining(29.minutes + 30.seconds) // 올림하면 30분
        runCurrent()
        assertEquals(30, trip.trip.value?.startMinutes)
    }

    @Test
    fun `도착 화면을 닫고 새로 출발하면 시작값은 비었다가 새 안내의 값이 된다`() = runTest {
        val (trip, guidance) = newTrip()
        trip.depart(choice(), card)
        runCurrent()
        guidance.remaining(35.minutes)
        runCurrent()
        guidance.arrive()
        runCurrent()
        assertEquals(35, trip.trip.value?.startMinutes)
        guidance.dismissArrival()
        trip.clear()
        runCurrent()

        trip.depart(choice(), card)
        runCurrent()
        assertNull(trip.trip.value?.startMinutes)
        guidance.remaining(10.minutes)
        runCurrent()
        assertEquals(10, trip.trip.value?.startMinutes)
    }
}
