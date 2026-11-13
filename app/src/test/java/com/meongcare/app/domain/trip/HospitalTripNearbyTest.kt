package com.meongcare.app.domain.trip

import com.meongcare.app.data.pet.SamplePetProfiles
import com.meongcare.app.domain.card.Symptom
import com.meongcare.app.domain.guidance.Destination
import com.meongcare.app.domain.guidance.GuidanceSource
import com.meongcare.app.domain.guidance.GuidanceState
import com.meongcare.app.domain.hospital.Hospital
import com.meongcare.app.domain.hospital.HospitalCandidate
import com.meongcare.app.domain.hospital.HospitalOrigin
import com.meongcare.app.domain.hospital.RankedHospital
import com.meongcare.app.domain.hospital.StatusFreshness
import java.time.Instant
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 계획 015 "설계 결정 8"의 `HospitalTrip` 계약: 근처 검색 병원에는 진료 카드를 보내지 않는다. */
@OptIn(ExperimentalCoroutinesApi::class)
class HospitalTripNearbyTest {

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

    private val vet = Hospital(Destination("근처 동물병원", 37.42, 127.09), "031-000-0000", isSample = false)

    private val nearbyChoice = RankedHospital(
        candidate = HospitalCandidate(vet, status = null, origin = HospitalOrigin.NEARBY_SEARCH),
        travel = 6.minutes,
        freshness = StatusFreshness.NONE,
        statusAge = null,
    )

    private val card = caseCardOf(SamplePetProfiles.demoPet, setOf(Symptom.COUGH))

    @Test
    fun `카드가 있어도 보내지 않고 길안내만 시작한다`() = runTest {
        val guidance = FakeGuidance()
        val asked = mutableListOf<HospitalCandidate>()
        val log = mutableListOf<String>()
        val trip = HospitalTrip(
            guidance = guidance,
            senderFor = { asked += it; error("근처 병원에는 전송 구현을 고르지 않는다") },
            scope = backgroundScope,
            now = { base.plusMillis(testScheduler.currentTime) },
            log = { log += it },
        )

        trip.depart(nearbyChoice, card)
        runCurrent()

        assertEquals(listOf(vet.destination.copy(expectedTravel = 6.minutes)), guidance.started)
        assertEquals(TripState(nearbyChoice.candidate, CardDelivery.NotSent), trip.trip.value)
        assertTrue(asked.isEmpty())
        assertTrue(log.none { it.startsWith("카드 전송") })

        guidance.flow.value = GuidanceState.Arrived(vet.destination)
        runCurrent()
        assertTrue("도착해도 아무것도 보내지 않는다", asked.isEmpty())
    }
}
