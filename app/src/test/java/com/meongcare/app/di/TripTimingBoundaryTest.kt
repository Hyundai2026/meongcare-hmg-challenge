package com.meongcare.app.di

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
import com.meongcare.app.domain.trip.AckRead
import com.meongcare.app.domain.trip.CaseCard
import com.meongcare.app.domain.trip.CaseEta
import com.meongcare.app.domain.trip.CaseRef
import com.meongcare.app.domain.trip.CaseSender
import com.meongcare.app.domain.trip.CaseStatus
import com.meongcare.app.domain.trip.HospitalTrip
import com.meongcare.app.domain.trip.SendResult
import com.meongcare.app.domain.trip.caseCardOf
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 013 설계 결정 3 경계: `HospitalTrip`의 기본 간격은 실제 길안내 값(60초, 15초)이고,
 * `tripTimingOf` 값을 넘기면 HospitalTrip이 그 간격으로 확인을 읽고 도착 예정을 보낸다 (1ms 경계).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TripTimingBoundaryTest {

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
        fun remaining(d: Duration?) {
            flow.value = GuidanceState.Guiding((flow.value as GuidanceState.Guiding).destination, d)
        }
    }

    private class Sender : CaseSender {
        var reads = 0
        val etaUpdates = mutableListOf<Int?>()
        override suspend fun send(hospital: Hospital, card: CaseCard, eta: CaseEta) = SendResult.Sent(CaseRef(hospital.id!!, "c1"))
        override suspend fun updateEta(ref: CaseRef, eta: CaseEta): Boolean {
            etaUpdates += eta.remainingMinutes
            return true
        }
        override suspend fun finish(ref: CaseRef, status: CaseStatus, at: Instant) = true
        override suspend fun readAck(ref: CaseRef): AckRead {
            reads++
            return AckRead.Pending
        }
    }

    private val hospital = Hospital(Destination("시연용 바른 동물병원", 37.367, 127.1083), "02-0000-0001", isSample = true, id = "demo-fast")
    private val choice = RankedHospital(
        candidate = HospitalCandidate(hospital, HospitalStatus(10, Congestion.NORMAL, true, base), HospitalOrigin.SERVER),
        travel = 19.minutes,
        freshness = StatusFreshness.FRESH,
        statusAge = 2.minutes,
    )
    private val card: CaseCard = caseCardOf(SamplePetProfiles.bori, setOf(Symptom.COUGH))

    private fun TestScope.advance(d: Duration) {
        advanceTimeBy(d)
        runCurrent()
    }

    /** [timing]이 null이면 HospitalTrip 기본값으로 만든다 */
    private fun TestScope.newTrip(timing: TripTiming?): Triple<HospitalTrip, Guidance, Sender> {
        val guidance = Guidance()
        val sender = Sender()
        val now = { base.plusMillis(testScheduler.currentTime) }
        val trip = if (timing == null) {
            HospitalTrip(guidance, { sender }, backgroundScope, now = now)
        } else {
            HospitalTrip(
                guidance,
                { sender },
                backgroundScope,
                now = now,
                etaInterval = timing.etaInterval,
                ackPollInterval = timing.ackPollInterval,
            )
        }
        return Triple(trip, guidance, sender)
    }

    /** 출발한 뒤 [interval]이 되기 1ms 전에는 확인을 읽지 않고, 되면 읽는다 */
    private fun TestScope.assertFirstAckReadAt(timing: TripTiming?, interval: Duration) {
        val (trip, _, sender) = newTrip(timing)
        trip.depart(choice, card)
        runCurrent()
        advance(interval - 1.milliseconds)
        assertEquals(0, sender.reads)
        advance(1.milliseconds)
        assertEquals(1, sender.reads)
    }

    /** 내비 첫 값(30분)은 바로 보내고, 그 뒤 바뀐 값(29분)은 [interval]이 되기 1ms 전에는 보내지 않는다 */
    private fun TestScope.assertSecondEtaAt(timing: TripTiming?, interval: Duration) {
        val (trip, guidance, sender) = newTrip(timing)
        trip.depart(choice, card)
        runCurrent()
        guidance.remaining(30.minutes)
        runCurrent()
        assertEquals(listOf<Int?>(30), sender.etaUpdates)
        guidance.remaining(29.minutes)
        runCurrent()
        advance(interval - 1.milliseconds)
        assertEquals(listOf<Int?>(30), sender.etaUpdates)
        advance(1.milliseconds)
        assertEquals(listOf<Int?>(30, 29), sender.etaUpdates)
    }

    @Test
    fun `HospitalTrip의 기본 확인 읽기 간격은 실제 길안내 간격이다`() = runTest {
        assertFirstAckReadAt(timing = null, interval = tripTimingOf(isGuidanceSimulated = false).ackPollInterval)
    }

    @Test
    fun `HospitalTrip의 기본 도착 예정 간격은 실제 길안내 간격이다`() = runTest {
        assertSecondEtaAt(timing = null, interval = tripTimingOf(isGuidanceSimulated = false).etaInterval)
    }

    @Test
    fun `시연용 간격을 넘긴 HospitalTrip은 4초에 첫 확인을 읽는다`() = runTest {
        val timing = tripTimingOf(isGuidanceSimulated = true)
        assertFirstAckReadAt(timing, interval = timing.ackPollInterval)
    }

    @Test
    fun `시연용 간격을 넘긴 HospitalTrip은 4초 뒤에 바뀐 도착 예정을 보낸다`() = runTest {
        val timing = tripTimingOf(isGuidanceSimulated = true)
        assertSecondEtaAt(timing, interval = timing.etaInterval)
    }
}
