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
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * HospitalTrip 경계. 기준: 계획 012 "설계 결정 8"의 4·7, "결정 / 배운 점"(첫 내비 값은 바로, 취소 시 10초,
 * 실패해서 기다리는 중이면 바로 끝냄), `HospitalTrip`·`CaseSender` KDoc, docs/data/emergency-case.md "상태 흐름".
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HospitalTripBoundaryTest {

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
            flow.value = GuidanceState.Guiding((flow.value as GuidanceState.Guiding).destination, duration)
        }
        fun arrive() {
            flow.value = GuidanceState.Arrived((flow.value as GuidanceState.Guiding).destination)
        }
    }

    /** 보내기 결과를 테스트가 정할 수 있는 전송. [gate]가 있으면 그 값이 올 때까지 보내는 중이다. */
    private class ControlledSender : CaseSender {
        val sends = mutableListOf<Triple<Hospital, CaseCard, CaseEta>>()
        val etaAttempts = mutableListOf<CaseEta>()
        val finishes = mutableListOf<CaseStatus>()
        var gate: CompletableDeferred<SendResult>? = null
        var failSend = false
        var updateOk = true

        override suspend fun send(hospital: Hospital, card: CaseCard, eta: CaseEta): SendResult {
            sends += Triple(hospital, card, eta)
            gate?.let { return it.await() }
            return if (failSend) SendResult.Failed("HTTP 503") else SendResult.Sent(CaseRef(hospital.id!!, "case-${sends.size}"))
        }

        override suspend fun updateEta(ref: CaseRef, eta: CaseEta): Boolean {
            etaAttempts += eta
            return updateOk
        }

        override suspend fun finish(ref: CaseRef, status: CaseStatus, at: Instant): Boolean {
            finishes += status
            return true
        }
    }

    private val fast = Hospital(Destination("시연용 바른 동물병원", 37.367, 127.1083), "02-0000-0001", isSample = true, id = "demo-fast")
    private val near = Hospital(Destination("시연용 가까운 동물병원", 37.3947, 127.1112), "02-0000-0002", isSample = true, id = "demo-near")

    private fun choice(hospital: Hospital = fast, travel: Duration? = 19.minutes) = RankedHospital(
        candidate = HospitalCandidate(hospital, HospitalStatus(10, Congestion.NORMAL, true, base), HospitalOrigin.SERVER),
        travel = travel,
        freshness = StatusFreshness.FRESH,
        statusAge = 2.minutes,
    )

    private val card = caseCardOf(SamplePetProfiles.bori, setOf(Symptom.COUGH))

    private class Fixture(val trip: HospitalTrip, val guidance: FakeGuidance, val sender: ControlledSender, val log: MutableList<String>, val asked: MutableList<HospitalCandidate>)

    private fun TestScope.fixture(sender: ControlledSender = ControlledSender()): Fixture {
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

    // ---- 위치를 몰라 이동 어림이 없을 때 ----

    @Test
    fun `위치를 몰라 이동 어림이 없으면 남은 분과 도착 예정 없이 카드를 보낸다`() = runTest {
        val f = fixture()
        f.trip.depart(choice(travel = null), card)
        runCurrent()

        assertNull(f.guidance.started.single().expectedTravel)
        val eta = f.sender.sends.single().third
        assertNull(eta.remainingMinutes)
        assertNull(eta.arrivesAt)
    }

    @Test
    fun `경로를 찾는 중에는 도착 예정을 보내지 않고 내비의 첫 남은 시간은 바로 보낸다`() = runTest {
        val f = fixture()
        f.trip.depart(choice(travel = null), card)
        runCurrent()
        advanceTimeBy(30.seconds)
        runCurrent()
        assertTrue(f.sender.etaAttempts.isEmpty())

        f.guidance.remaining(20.minutes)
        runCurrent()
        assertEquals(listOf(20), f.sender.etaAttempts.map { it.remainingMinutes })
        assertEquals(base.plusSeconds(30 + 20 * 60), f.sender.etaAttempts.single().arrivesAt)
    }

    @Test
    fun `남은 시간이 0초여도 병원에는 1분으로 보낸다`() = runTest {
        val f = fixture()
        f.trip.depart(choice(), card)
        runCurrent()
        f.guidance.remaining(Duration.ZERO)
        runCurrent()
        assertEquals(listOf(1), f.sender.etaAttempts.map { it.remainingMinutes })
    }

    // ---- 보내는 중에 도착·취소 ----

    @Test
    fun `카드를 보내는 중에 도착하면 보낸 뒤 도착을 알린다`() = runTest {
        val sender = ControlledSender().apply { gate = CompletableDeferred() }
        val f = fixture(sender)
        f.trip.depart(choice(), card)
        runCurrent()
        assertEquals(CardDelivery.Sending, f.trip.trip.value?.delivery)

        f.guidance.arrive()
        runCurrent()
        assertTrue(sender.finishes.isEmpty())

        sender.gate!!.complete(SendResult.Sent(CaseRef("demo-fast", "c1")))
        runCurrent()
        assertEquals(listOf(CaseStatus.ARRIVED), sender.finishes)
        assertEquals(CardDelivery.Sent(toServer = true), f.trip.trip.value?.delivery)
    }

    @Test
    fun `카드를 보내는 중에 취소되면 보낸 뒤 취소를 알리고 고른 병원을 비운다`() = runTest {
        val sender = ControlledSender().apply { gate = CompletableDeferred() }
        val f = fixture(sender)
        f.trip.depart(choice(), card)
        runCurrent()
        f.guidance.cancel()
        runCurrent()
        assertNotNull("보내는 중이면 취소를 보낼 시간을 준다", f.trip.trip.value)

        sender.gate!!.complete(SendResult.Sent(CaseRef("demo-fast", "c1")))
        runCurrent()
        assertEquals(listOf(CaseStatus.CANCELLED), sender.finishes)
        assertNull(f.trip.trip.value)
    }

    @Test
    fun `취소를 알리는 데 10초가 넘으면 더 기다리지 않고 고른 병원을 비운다`() = runTest {
        val sender = ControlledSender().apply { gate = CompletableDeferred() } // 끝나지 않는 전송
        val f = fixture(sender)
        f.trip.depart(choice(), card)
        runCurrent()
        f.guidance.cancel()
        runCurrent()
        advanceTimeBy(10.seconds - 1.milliseconds)
        runCurrent()
        assertNotNull(f.trip.trip.value)
        advanceTimeBy(2.milliseconds)
        runCurrent()
        assertNull(f.trip.trip.value)
        f.trip.openFinder()
        assertEquals(FinderStep.Hospitals, f.trip.finder.value)
    }

    @Test
    fun `보내는 중에 취소됐는데 그 전송이 실패하면 기다리지 않고 고른 병원을 비운다`() = runTest {
        // 결정 / 배운 점: "실패해서 [다시 보내기]를 기다리는 중이면 바로 끝낸다". 실패했으면 병원에 만든 건이 없어 취소를 보낼 것도 없다.
        val sender = ControlledSender().apply { gate = CompletableDeferred() }
        val f = fixture(sender)
        f.trip.depart(choice(), card)
        runCurrent()
        f.guidance.cancel()
        runCurrent()

        sender.gate!!.complete(SendResult.Failed("HTTP 503"))
        runCurrent()

        assertNull("전송이 실패했으니 10초를 기다리지 않고 바로 비운다", f.trip.trip.value)
        f.trip.openFinder()
        assertEquals("취소한 뒤 바로 다시 찾을 수 있다", FinderStep.Hospitals, f.trip.finder.value)
        assertTrue(sender.finishes.isEmpty())
    }

    // ---- 갱신 실패 ----

    @Test
    fun `도착 예정 갱신이 실패하면 로그만 남기고 다음 변화 때 다시 보낸다`() = runTest {
        val sender = ControlledSender().apply { updateOk = false }
        val f = fixture(sender)
        f.trip.depart(choice(), card)
        runCurrent()
        f.guidance.remaining(35.minutes)
        runCurrent()
        assertEquals(listOf(35), sender.etaAttempts.map { it.remainingMinutes })
        assertTrue("도착 예정 갱신: 실패" in f.log)
        assertTrue("길안내는 계속된다", f.guidance.flow.value is GuidanceState.Guiding)

        sender.updateOk = true
        advanceTimeBy(5.seconds)
        f.guidance.remaining(34.minutes)
        runCurrent()
        assertEquals(listOf(35, 34), sender.etaAttempts.map { it.remainingMinutes })
        assertTrue("도착 예정 갱신: 성공" in f.log)
    }

    // ---- 다시 보내기 ----

    @Test
    fun `실패 뒤 다시 보내기를 여러 번 눌러도 한 번만 보낸다`() = runTest {
        val sender = ControlledSender().apply { failSend = true }
        val f = fixture(sender)
        f.trip.depart(choice(), card)
        runCurrent()
        sender.failSend = false
        repeat(3) { f.trip.resend() }
        runCurrent()
        assertEquals(2, sender.sends.size)
        assertEquals(CardDelivery.Sent(toServer = true), f.trip.trip.value?.delivery)
    }

    @Test
    fun `카드 없이 출발하면 다시 보내기를 눌러도 아무것도 보내지 않는다`() = runTest {
        val f = fixture()
        f.trip.depart(choice(), card = null)
        runCurrent()
        f.trip.resend()
        runCurrent()
        assertTrue(f.sender.sends.isEmpty())
        assertTrue(f.asked.isEmpty())
        assertEquals(CardDelivery.NotSent, f.trip.trip.value?.delivery)
    }

    @Test
    fun `카드 없이 출발해 안내 중 취소되면 고른 병원을 비우고 아무것도 보내지 않는다`() = runTest {
        val f = fixture()
        f.trip.depart(choice(), card = null)
        runCurrent()
        f.guidance.cancel()
        runCurrent()
        assertNull(f.trip.trip.value)
        assertTrue(f.sender.finishes.isEmpty())
    }

    // ---- 다음 출발 ----

    @Test
    fun `취소된 뒤에는 다시 찾고 다른 병원으로 출발할 수 있다`() = runTest {
        val f = fixture()
        f.trip.depart(choice(fast), card)
        runCurrent()
        f.guidance.cancel()
        runCurrent()
        assertNull(f.trip.trip.value)

        f.trip.openFinder()
        f.trip.choose(choice(near))
        assertEquals(FinderStep.Confirm(choice(near)), f.trip.finder.value)
        f.trip.depart(choice(near), card)
        runCurrent()

        assertEquals(listOf("demo-fast", "demo-near"), f.sender.sends.map { it.first.id })
        assertEquals(listOf(fast.name, near.name), f.guidance.started.map { it.name })
        assertEquals(near, f.trip.trip.value?.candidate?.hospital)
        assertEquals(listOf(CaseStatus.CANCELLED), f.sender.finishes)
    }

    @Test
    fun `도착 뒤 안내 없음이 닫기보다 먼저 와도 취소를 보내지 않는다`() = runTest {
        // MainActivity의 도착 [닫기]: 길안내 도착 화면을 먼저 닫고(안내 없음) 그다음 trip.clear()
        val f = fixture()
        f.trip.depart(choice(), card)
        runCurrent()
        f.guidance.arrive()
        runCurrent()
        f.guidance.dismissArrival()
        runCurrent()
        f.trip.clear()
        runCurrent()
        assertEquals(listOf(CaseStatus.ARRIVED), f.sender.finishes)
        assertNull(f.trip.trip.value)
    }

    @Test
    fun `도착 화면을 닫은 뒤 다시 출발하면 새 건으로 보낸다`() = runTest {
        val f = fixture()
        f.trip.depart(choice(fast), card)
        runCurrent()
        f.guidance.arrive()
        runCurrent()
        f.guidance.dismissArrival()
        f.trip.clear()
        runCurrent()

        f.trip.depart(choice(near), card)
        runCurrent()
        assertEquals(2, f.sender.sends.size)
        assertEquals(CardDelivery.Sent(toServer = true), f.trip.trip.value?.delivery)
        assertEquals(near, f.trip.trip.value?.candidate?.hospital)
    }

    @Test
    fun `고른 병원은 출발하면 생기고 취소되거나 도착 화면을 닫으면 비워진다`() = runTest {
        val f = fixture()
        runCurrent()
        assertNull(f.trip.selectedHospital.value)

        f.trip.depart(choice(fast), card)
        runCurrent()
        assertEquals(fast, f.trip.selectedHospital.value)

        f.guidance.cancel()
        runCurrent()
        assertNull(f.trip.selectedHospital.value)

        f.trip.depart(choice(near), card)
        runCurrent()
        assertEquals(near, f.trip.selectedHospital.value)
        f.guidance.arrive()
        runCurrent()
        assertEquals("도착 카드에는 고른 병원이 남는다", near, f.trip.selectedHospital.value)
        f.trip.clear()
        runCurrent()
        assertNull(f.trip.selectedHospital.value)
    }

    @Test
    fun `출발한 뒤에는 목록에서 다른 병원을 고를 수 없다`() = runTest {
        val f = fixture()
        f.trip.openFinder()
        f.trip.depart(choice(fast), card)
        f.trip.choose(choice(near))
        assertEquals(FinderStep.None, f.trip.finder.value)
    }

    @Test
    fun `확인 단계에서 뒤로 갔다 다른 병원을 고르면 그 병원이 확인 대상이다`() = runTest {
        val f = fixture()
        f.trip.openFinder()
        f.trip.choose(choice(fast))
        f.trip.back()
        f.trip.choose(choice(near))
        assertEquals(FinderStep.Confirm(choice(near)), f.trip.finder.value)
        f.trip.closeFinder()
        assertEquals(FinderStep.None, f.trip.finder.value)
    }
}
