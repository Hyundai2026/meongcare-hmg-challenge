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
import com.meongcare.app.ui.guidance.toDisplayMinutes
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HospitalTripTest {

    private val base = Instant.parse("2026-11-06T05:00:00Z")

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
        fun remaining(duration: Duration) {
            val dest = (flow.value as GuidanceState.Guiding).destination
            flow.value = GuidanceState.Guiding(dest, duration)
        }
        fun arrive() {
            flow.value = GuidanceState.Arrived((flow.value as GuidanceState.Guiding).destination)
        }
    }

    private class RecordingSender(var failSend: Boolean = false) : CaseSender {
        val sends = mutableListOf<Pair<CaseCard, CaseEta>>()
        val etas = mutableListOf<CaseEta>()
        val finishes = mutableListOf<CaseStatus>()
        override suspend fun send(hospital: Hospital, card: CaseCard, eta: CaseEta): SendResult {
            sends += card to eta
            return if (failSend) SendResult.Failed("HTTP 503") else SendResult.Sent(CaseRef(hospital.id!!, "case-${sends.size}"))
        }
        override suspend fun updateEta(ref: CaseRef, eta: CaseEta): Boolean {
            etas += eta
            return true
        }
        override suspend fun finish(ref: CaseRef, status: CaseStatus, at: Instant): Boolean {
            finishes += status
            return true
        }
    }

    private val hospital = Hospital(Destination("시연용 바른 동물병원", 37.367, 127.1083), "02-0000-0001", isSample = true, id = "demo-fast")

    private fun choice(origin: HospitalOrigin = HospitalOrigin.SERVER, travel: Duration? = 19.minutes) = RankedHospital(
        candidate = HospitalCandidate(hospital, HospitalStatus(10, Congestion.NORMAL, true, base), origin),
        travel = travel,
        freshness = StatusFreshness.FRESH,
        statusAge = 2.minutes,
    )

    private val card = caseCardOf(
        SamplePetProfiles.demoPet.copy(photoDataUrl = "data:image/jpeg;base64,AAAA"),
        setOf(Symptom.COUGH, Symptom.BREATHING_DIFFICULTY),
    )

    private class Fixture(
        val trip: HospitalTrip,
        val guidance: FakeGuidance,
        val sender: RecordingSender,
        val log: MutableList<String>,
        val senderAskedFor: MutableList<HospitalCandidate>,
    )

    private fun TestScope.fixture(sender: RecordingSender = RecordingSender()): Fixture {
        val guidance = FakeGuidance()
        val log = mutableListOf<String>()
        val asked = mutableListOf<HospitalCandidate>()
        val trip = HospitalTrip(
            guidance = guidance,
            senderFor = { asked += it; sender },
            scope = backgroundScope,
            now = { base.plusMillis(testScheduler.currentTime) },
            log = { log += it },
        )
        return Fixture(trip, guidance, sender, log, asked)
    }

    @Test
    fun `출발하면 고른 병원으로 길안내를 시작하고 카드와 도착 예정을 보낸다`() = runTest {
        val f = fixture()
        f.trip.openFinder()
        f.trip.choose(choice())
        f.trip.depart(choice(), card)
        runCurrent()

        assertEquals(listOf(hospital.destination.copy(expectedTravel = 19.minutes)), f.guidance.started)
        assertEquals(1, f.sender.sends.size)
        val (sentCard, eta) = f.sender.sends.single()
        assertEquals(card, sentCard)
        assertEquals(19, eta.remainingMinutes)
        assertEquals(base.plusSeconds(19 * 60), eta.arrivesAt)
        assertEquals(TripState(choice().candidate, CardDelivery.Sent(toServer = true)), f.trip.trip.value)
        assertEquals(FinderStep.None, f.trip.finder.value)
        assertEquals(listOf("카드 전송: 성공"), f.log)
    }

    @Test
    fun `카드 전송이 실패해도 길안내는 계속되고 다시 보낼 수 있다`() = runTest {
        val sender = RecordingSender(failSend = true)
        val f = fixture(sender)
        f.trip.depart(choice(), card)
        runCurrent()

        assertEquals(1, f.guidance.started.size)
        assertTrue(f.guidance.flow.value is GuidanceState.Guiding)
        assertEquals(CardDelivery.Failed, f.trip.trip.value?.delivery)
        assertEquals(listOf("카드 전송: 실패 (HTTP 503)"), f.log)

        sender.failSend = false
        f.guidance.remaining(25.minutes)
        f.trip.resend()
        runCurrent()

        assertEquals(2, sender.sends.size)
        assertEquals(25, sender.sends.last().second.remainingMinutes)
        assertEquals(CardDelivery.Sent(toServer = true), f.trip.trip.value?.delivery)
    }

    @Test
    fun `실패하지 않았으면 다시 보내기는 아무것도 하지 않는다`() = runTest {
        val f = fixture()
        f.trip.depart(choice(), card)
        runCurrent()
        f.trip.resend()
        runCurrent()
        assertEquals(1, f.sender.sends.size)
    }

    @Test
    fun `카드 없이 출발하면 아무것도 보내지 않는다`() = runTest {
        val f = fixture()
        f.trip.depart(choice(), card = null)
        runCurrent()
        f.guidance.remaining(10.minutes)
        advanceTimeBy(5.minutes)
        f.guidance.arrive()
        runCurrent()

        assertEquals(1, f.guidance.started.size)
        assertTrue(f.sender.sends.isEmpty() && f.sender.etas.isEmpty() && f.sender.finishes.isEmpty())
        assertEquals(CardDelivery.NotSent, f.trip.trip.value?.delivery)
        assertTrue(f.senderAskedFor.isEmpty())
    }

    @Test
    fun `출발을 두 번 불러도 한 번만 출발한다`() = runTest {
        val f = fixture()
        f.trip.depart(choice(), card)
        f.trip.depart(choice(), card)
        runCurrent()
        assertEquals(1, f.guidance.started.size)
        assertEquals(1, f.sender.sends.size)
    }

    @Test
    fun `내비의 첫 남은 시간은 기다리지 않고 어림값을 바꾼다`() = runTest {
        val f = fixture()
        f.trip.depart(choice(travel = 19.minutes), card)
        runCurrent()
        advanceTimeBy(3.seconds)
        f.guidance.remaining(35.minutes)
        runCurrent()
        assertEquals(listOf(35), f.sender.etas.map { it.remainingMinutes })
    }

    @Test
    fun `남은 분이 바뀌고 60초가 지났을 때만 도착 예정을 다시 보낸다`() = runTest {
        val f = fixture()
        f.trip.depart(choice(), card)
        runCurrent()
        f.guidance.remaining(35.minutes) // 첫 내비 값: 바로 보낸다 (t=0)
        runCurrent()
        assertEquals(listOf(35), f.sender.etas.map { it.remainingMinutes })

        // 같은 분(올림 35분)이면 보내지 않는다
        advanceTimeBy(70.seconds)
        f.guidance.remaining(34.minutes + 30.seconds)
        runCurrent()
        assertEquals(1, f.sender.etas.size)

        // 분이 바뀌었지만 마지막으로 보낸 지 60초가 안 됐으면 60초가 될 때까지 기다렸다가 최신 값을 보낸다
        f.guidance.remaining(34.minutes) // t=70s, 마지막 전송 t=0 → 바로 보낸다
        runCurrent()
        assertEquals(listOf(35, 34), f.sender.etas.map { it.remainingMinutes })

        advanceTimeBy(10.seconds) // t=80s
        f.guidance.remaining(33.minutes)
        runCurrent()
        assertEquals(2, f.sender.etas.size)
        advanceTimeBy(20.seconds) // t=100s
        f.guidance.remaining(32.minutes) // 기다리는 중에 더 새 값이 오면 그 값을 보낸다
        runCurrent()
        assertEquals(2, f.sender.etas.size)
        advanceTimeBy(30.seconds - 1.milliseconds) // t=130s-1ms
        runCurrent()
        assertEquals(2, f.sender.etas.size)
        advanceTimeBy(2.milliseconds) // t=130s: 마지막 전송(t=70s)에서 60초
        runCurrent()
        assertEquals(listOf(35, 34, 32), f.sender.etas.map { it.remainingMinutes })
        assertEquals(base.plusSeconds(130), f.sender.etas.last().measuredAt)
    }

    @Test
    fun `도착하면 도착 상태를 보낸다`() = runTest {
        val f = fixture()
        f.trip.depart(choice(), card)
        runCurrent()
        f.guidance.remaining(1.minutes)
        runCurrent()
        f.guidance.arrive()
        runCurrent()

        assertEquals(listOf(CaseStatus.ARRIVED), f.sender.finishes)
        assertEquals(hospital, f.trip.trip.value?.candidate?.hospital) // 도착 카드에 고른 병원이 남는다
        assertTrue("도착 알림: 성공" in f.log)
    }

    @Test
    fun `안내 중에 취소되면 취소 상태를 보내고 고른 병원을 비운다`() = runTest {
        val f = fixture()
        f.trip.depart(choice(), card)
        runCurrent()
        f.guidance.cancel()
        runCurrent()

        assertEquals(listOf(CaseStatus.CANCELLED), f.sender.finishes)
        assertNull(f.trip.trip.value)
        assertTrue("취소 알림: 성공" in f.log)
    }

    @Test
    fun `실패해서 기다리는 중에 취소되면 바로 끝낸다`() = runTest {
        val f = fixture(RecordingSender(failSend = true))
        f.trip.depart(choice(), card)
        runCurrent()
        f.guidance.cancel()
        runCurrent()
        assertNull(f.trip.trip.value)
        assertTrue(f.sender.finishes.isEmpty())
    }

    @Test
    fun `출발 전의 안내 없음은 취소로 보지 않는다`() = runTest {
        val guidance = object : GuidanceSource {
            val flow = MutableStateFlow<GuidanceState>(GuidanceState.Idle)
            override val state: StateFlow<GuidanceState> = flow
            override suspend fun start(destination: Destination) = Unit // 아직 상태가 바뀌지 않는다
            override suspend fun cancel() = Unit
            override fun dismissArrival() = Unit
        }
        val sender = RecordingSender()
        val trip = HospitalTrip(guidance, { sender }, backgroundScope, now = { base })
        trip.depart(choice(), card)
        runCurrent()
        assertTrue(trip.trip.value != null)
        assertTrue(sender.finishes.isEmpty())
    }

    @Test
    fun `도착 화면을 닫으면 고른 병원을 비우고 상태는 보내지 않는다`() = runTest {
        val f = fixture()
        f.trip.depart(choice(), card)
        runCurrent()
        f.guidance.arrive()
        runCurrent()
        val finishesBefore = f.sender.finishes.toList()

        f.trip.clear()
        f.guidance.dismissArrival()
        runCurrent()

        assertNull(f.trip.trip.value)
        assertEquals(finishesBefore, f.sender.finishes)
        assertEquals(listOf(CaseStatus.ARRIVED), finishesBefore)
    }

    @Test
    fun `보내는 카드에 사진과 위치가 없다`() {
        assertNull(card.pet?.photoDataUrl)
        assertEquals(listOf(Symptom.BREATHING_DIFFICULTY, Symptom.COUGH), card.symptoms)
        // 위치는 CaseCard에 자리가 없다 (필드: pet, symptoms)
        assertEquals(listOf("pet", "symptoms"), CaseCard::class.java.declaredFields.filterNot { java.lang.reflect.Modifier.isStatic(it.modifiers) }.map { it.name }.sorted())
    }

    @Test
    fun `출처에 맞는 전송 구현을 고르고 시연 병원은 시연용으로 알린다`() = runTest {
        val f = fixture()
        f.trip.depart(choice(origin = HospitalOrigin.BUILT_IN_FALLBACK), card)
        runCurrent()
        assertEquals(HospitalOrigin.BUILT_IN_FALLBACK, f.senderAskedFor.single().origin)
        assertEquals(CardDelivery.Sent(toServer = false), f.trip.trip.value?.delivery)
        assertEquals(listOf("카드 전송: 시연용 (서버로 보내지 않음)"), f.log)
    }

    @Test
    fun `로그에 반려견 이름과 증상과 병원 이름이 없다`() = runTest {
        val f = fixture()
        f.trip.depart(choice(), card)
        runCurrent()
        f.guidance.remaining(30.minutes)
        runCurrent()
        f.guidance.arrive()
        runCurrent()
        val all = f.log.joinToString("\n")
        assertFalse(all.contains("보리"))
        assertFalse(all.contains("기침") || all.contains("COUGH") || all.contains("호흡"))
        assertFalse(all.contains("바른"))
        assertFalse(all.contains("37."))
    }

    @Test
    fun `찾기 단계는 목록, 확인, 뒤로가 이어진다`() = runTest {
        val f = fixture()
        f.trip.choose(choice()) // 찾기를 열기 전에는 고를 수 없다
        assertEquals(FinderStep.None, f.trip.finder.value)
        f.trip.openFinder()
        assertEquals(FinderStep.Hospitals, f.trip.finder.value)
        f.trip.choose(choice())
        assertEquals(FinderStep.Confirm(choice()), f.trip.finder.value)
        f.trip.back()
        assertEquals(FinderStep.Hospitals, f.trip.finder.value)
        f.trip.back()
        assertEquals(FinderStep.None, f.trip.finder.value)
    }

    @Test
    fun `출발한 뒤에는 찾기를 열지 않는다`() = runTest {
        val f = fixture()
        f.trip.depart(choice(), card)
        f.trip.openFinder()
        assertEquals(FinderStep.None, f.trip.finder.value)
    }

    @Test
    fun `병원에 보내는 남은 분은 화면의 남은 분과 같다`() {
        listOf(0.seconds, 1.seconds, 59.seconds, 60.seconds, 61.seconds, 90.seconds, 35.minutes, 34.minutes + 1.milliseconds)
            .forEach { assertEquals(it.toString(), it.toDisplayMinutes(), it.toCaseMinutes()) }
    }
}
