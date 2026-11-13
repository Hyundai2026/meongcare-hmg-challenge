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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** 013: 진행 막대의 시작값 [TripState.startMinutes] */
@OptIn(ExperimentalCoroutinesApi::class)
class HospitalTripProgressTest {

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

    private fun choice(travel: Duration? = 19.minutes) = RankedHospital(
        candidate = HospitalCandidate(hospital, HospitalStatus(10, Congestion.NORMAL, true, base), HospitalOrigin.SERVER),
        travel = travel,
        freshness = StatusFreshness.FRESH,
        statusAge = 2.minutes,
    )

    private val card = caseCardOf(SamplePetProfiles.demoPet, setOf(Symptom.COUGH))

    private fun TestScope.departed(): Pair<HospitalTrip, Guidance> {
        val guidance = Guidance()
        val trip = HospitalTrip(guidance, { Sender() }, backgroundScope, now = { base.plusMillis(testScheduler.currentTime) })
        trip.depart(choice(), card)
        runCurrent()
        return trip to guidance
    }

    @Test
    fun `처음 알게 된 남은 분이 진행 막대의 시작이다`() = runTest {
        val (trip, guidance) = departed()
        guidance.remaining(35.minutes)
        runCurrent()
        assertEquals(35, trip.trip.value?.startMinutes)
    }

    @Test
    fun `남은 분이 시작보다 커지면 시작을 올린다`() = runTest {
        val (trip, guidance) = departed()
        guidance.remaining(35.minutes)
        runCurrent()
        guidance.remaining(40.minutes)
        runCurrent()
        assertEquals(40, trip.trip.value?.startMinutes)
    }

    @Test
    fun `경로를 찾는 중은 시작을 바꾸지 않는다`() = runTest {
        val (trip, guidance) = departed()
        assertNull(trip.trip.value?.startMinutes)
        guidance.remaining(30.minutes)
        runCurrent()
        guidance.remaining(null)
        runCurrent()
        assertEquals(30, trip.trip.value?.startMinutes)
    }

    @Test
    fun `줄어드는 남은 분은 시작을 바꾸지 않는다`() = runTest {
        val (trip, guidance) = departed()
        guidance.remaining(35.minutes)
        runCurrent()
        guidance.remaining(20.minutes)
        runCurrent()
        guidance.remaining(1.minutes)
        runCurrent()
        assertEquals(35, trip.trip.value?.startMinutes)
        guidance.arrive()
        runCurrent()
        assertEquals(35, trip.trip.value?.startMinutes)
    }

    @Test
    fun `목록의 이동 어림은 시작값이 아니다`() = runTest {
        val (trip, _) = departed()
        // 출발할 때 목록 어림(19분)을 병원에 보냈지만, 길안내가 남은 시간을 알리기 전이라 시작값은 아직 없다
        assertEquals(CardDelivery.Sent(toServer = true), trip.trip.value?.delivery)
        assertNull(trip.trip.value?.startMinutes)
    }
}
