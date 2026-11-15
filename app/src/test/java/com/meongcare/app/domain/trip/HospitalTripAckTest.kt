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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

/** 013 U1: 안내 중 서버 카드의 병원 확인 읽기 ([HospitalTrip], 간격 4초로 시험) */
@OptIn(ExperimentalCoroutinesApi::class)
class HospitalTripAckTest {

    private val base = Instant.parse("2026-11-06T05:00:00Z")
    private val ackAt = Instant.parse("2026-11-06T05:01:30Z")

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
        fun arrive() {
            flow.value = GuidanceState.Arrived((flow.value as GuidanceState.Guiding).destination)
        }
    }

    /** 읽기 결과를 차례로 준다. 다 쓰면 아직(Pending). [gate]가 있으면 읽기가 그 값을 기다린다 */
    private class AckSender(vararg results: AckRead) : CaseSender {
        val queue = ArrayDeque(results.toList())
        var reads = 0
        var failSend = false
        var gate: CompletableDeferred<AckRead>? = null
        override suspend fun send(hospital: Hospital, card: CaseCard, eta: CaseEta): SendResult =
            if (failSend) SendResult.Failed("HTTP 503") else SendResult.Sent(CaseRef(hospital.id!!, "c1"))
        override suspend fun updateEta(ref: CaseRef, eta: CaseEta) = true
        override suspend fun finish(ref: CaseRef, status: CaseStatus, at: Instant) = true
        override suspend fun readAck(ref: CaseRef): AckRead {
            reads++
            gate?.let { return it.await() }
            return queue.removeFirstOrNull() ?: AckRead.Pending
        }
    }

    private val hospital = Hospital(Destination("시연용 바른 동물병원", 37.367, 127.1083), "02-0000-0001", isSample = true, id = "demo-fast")

    private fun choice(origin: HospitalOrigin = HospitalOrigin.SERVER) = RankedHospital(
        candidate = HospitalCandidate(hospital, HospitalStatus(10, Congestion.NORMAL, true, base), origin),
        travel = 19.minutes,
        freshness = StatusFreshness.FRESH,
        statusAge = 2.minutes,
    )

    private val card = caseCardOf(SamplePetProfiles.demoPet, setOf(Symptom.COUGH))

    private class Fixture(val trip: HospitalTrip, val guidance: Guidance, val log: MutableList<String>)

    private fun TestScope.fixture(sender: AckSender): Fixture {
        val guidance = Guidance()
        val log = mutableListOf<String>()
        val trip = HospitalTrip(
            guidance = guidance,
            senderFor = { sender },
            scope = backgroundScope,
            now = { base.plusMillis(testScheduler.currentTime) },
            log = { log += it },
            etaInterval = 4.seconds,
            ackPollInterval = 4.seconds,
        )
        return Fixture(trip, guidance, log)
    }

    private fun TestScope.advance(d: Duration) {
        advanceTimeBy(d)
        runCurrent()
    }

    private val Fixture.delivery get() = trip.trip.value?.delivery

    @Test
    fun `서버로 보낸 뒤 간격마다 확인을 읽고 확인되면 표시하고 멈춘다`() = runTest {
        val sender = AckSender(AckRead.Pending, AckRead.Pending, AckRead.Acknowledged(ackAt))
        val f = fixture(sender)
        f.trip.depart(choice(), card)
        runCurrent()
        assertEquals(0, sender.reads)
        advance(4.seconds)
        assertEquals(1, sender.reads)
        assertEquals(CardDelivery.Sent(toServer = true), f.delivery)
        advance(8.seconds)
        assertEquals(3, sender.reads)
        assertEquals(CardDelivery.Sent(toServer = true, acknowledgedAt = ackAt), f.delivery)
        advance(1.minutes)
        assertEquals(3, sender.reads)
        assertEquals(1, f.log.count { it == "병원 확인: 확인함" })
    }

    @Test
    fun `첫 읽기는 간격이 되기 1ms 전에는 하지 않는다`() = runTest {
        val sender = AckSender()
        val f = fixture(sender)
        f.trip.depart(choice(), card)
        runCurrent()
        advance(4.seconds - 1.milliseconds)
        assertEquals(0, sender.reads)
        advance(1.milliseconds)
        assertEquals(1, sender.reads)
    }

    @Test
    fun `내장 병원으로 출발하면 확인을 한 번도 읽지 않는다`() = runTest {
        for (origin in listOf(HospitalOrigin.BUILT_IN, HospitalOrigin.BUILT_IN_FALLBACK)) {
            val sender = AckSender(AckRead.Acknowledged(ackAt))
            val f = fixture(sender)
            f.trip.depart(choice(origin), card)
            advance(1.minutes)
            assertEquals("$origin", 0, sender.reads)
            assertEquals(CardDelivery.Sent(toServer = false), f.delivery)
            f.trip.clear()
        }
    }

    @Test
    fun `카드 없이 출발하면 확인을 읽지 않는다`() = runTest {
        val sender = AckSender(AckRead.Acknowledged(ackAt))
        val f = fixture(sender)
        f.trip.depart(choice(), card = null)
        advance(1.minutes)
        assertEquals(0, sender.reads)
        assertEquals(CardDelivery.NotSent, f.delivery)
    }

    @Test
    fun `전송 실패로 다시 보내기를 기다리는 동안은 읽지 않고 다시 보내 성공하면 읽는다`() = runTest {
        val sender = AckSender(AckRead.Acknowledged(ackAt)).apply { failSend = true }
        val f = fixture(sender)
        f.trip.depart(choice(), card)
        advance(1.minutes)
        assertEquals(CardDelivery.Failed, f.delivery)
        assertEquals(0, sender.reads)
        sender.failSend = false
        f.trip.resend()
        runCurrent()
        assertEquals(CardDelivery.Sent(toServer = true), f.delivery)
        advance(4.seconds)
        assertEquals(1, sender.reads)
        assertEquals(CardDelivery.Sent(toServer = true, acknowledgedAt = ackAt), f.delivery)
    }

    @Test
    fun `도착하면 읽기를 멈추고 읽던 결과를 쓰지 않는다`() = runTest {
        val sender = AckSender()
        val gate = CompletableDeferred<AckRead>()
        sender.gate = gate
        val f = fixture(sender)
        f.trip.depart(choice(), card)
        advance(4.seconds)
        assertEquals(1, sender.reads) // 읽는 중
        f.guidance.arrive()
        runCurrent()
        gate.complete(AckRead.Acknowledged(ackAt))
        advance(1.minutes)
        assertEquals(1, sender.reads)
        assertEquals(CardDelivery.Sent(toServer = true), f.delivery)
        assertFalse(f.log.any { it.startsWith("병원 확인") })
    }

    @Test
    fun `취소되면 읽기를 멈추고 고른 병원을 비운다`() = runTest {
        val sender = AckSender()
        val f = fixture(sender)
        f.trip.depart(choice(), card)
        advance(4.seconds)
        assertEquals(1, sender.reads)
        f.guidance.cancel()
        advance(1.minutes)
        assertEquals(1, sender.reads)
        assertNull(f.trip.trip.value)
    }

    @Test
    fun `도착 화면을 닫으면 읽기를 멈춘다`() = runTest {
        val sender = AckSender()
        val f = fixture(sender)
        f.trip.depart(choice(), card)
        advance(4.seconds)
        f.trip.clear()
        advance(1.minutes)
        assertEquals(1, sender.reads)
        assertNull(f.trip.trip.value)
    }

    @Test
    fun `건이 지워졌으면 멈추고 표시는 그대로다`() = runTest {
        val sender = AckSender(AckRead.Gone)
        val f = fixture(sender)
        f.trip.depart(choice(), card)
        advance(4.seconds)
        advance(1.minutes)
        assertEquals(1, sender.reads)
        assertEquals(CardDelivery.Sent(toServer = true), f.delivery)
        assertEquals(listOf("카드 전송: 성공", "병원 확인: 건을 읽을 수 없음"), f.log.filterNot { it.startsWith("도착 예정") })
    }

    @Test
    fun `읽기 실패는 다음 간격에 다시 하고 같은 실패의 로그는 한 줄이다`() = runTest {
        val sender = AckSender(
            AckRead.Failed("네트워크"),
            AckRead.Failed("네트워크"),
            AckRead.Failed("네트워크"),
            AckRead.Failed("HTTP 503"),
            AckRead.Acknowledged(ackAt),
        )
        val f = fixture(sender)
        f.trip.depart(choice(), card)
        advance(20.seconds)
        assertEquals(5, sender.reads)
        assertEquals(
            listOf("병원 확인: 읽기 실패 (네트워크)", "병원 확인: 읽기 실패 (HTTP 503)", "병원 확인: 확인함"),
            f.log.filter { it.startsWith("병원 확인") },
        )
        assertEquals(CardDelivery.Sent(toServer = true, acknowledgedAt = ackAt), f.delivery)
    }

    @Test
    fun `확인을 지원하지 않는 전송이면 로그 없이 멈춘다`() = runTest {
        val sender = AckSender(AckRead.Unsupported)
        val f = fixture(sender)
        f.trip.depart(choice(), card)
        advance(1.minutes)
        assertEquals(1, sender.reads)
        assertEquals(listOf("카드 전송: 성공"), f.log)
    }

    @Test
    fun `확인된 뒤 도착해도 확인 표시가 남는다`() = runTest {
        val sender = AckSender(AckRead.Acknowledged(ackAt))
        val f = fixture(sender)
        f.trip.depart(choice(), card)
        advance(4.seconds)
        f.guidance.arrive()
        advance(10.seconds)
        assertEquals(CardDelivery.Sent(toServer = true, acknowledgedAt = ackAt), f.delivery)
    }

    @Test
    fun `로그에 시각과 병원 이름이 없다`() = runTest {
        val sender = AckSender(AckRead.Failed("네트워크"), AckRead.Acknowledged(ackAt))
        val f = fixture(sender)
        f.trip.depart(choice(), card)
        advance(10.seconds)
        f.guidance.arrive()
        advance(10.seconds)
        assertFalse(f.log.isEmpty())
        for (line in f.log) {
            assertFalse(line, line.contains(hospital.name))
            assertFalse(line, line.contains("2026"))
            assertFalse(line, Regex("\\d{1,2}:\\d{2}").containsMatchIn(line))
            assertFalse(line, line.contains("왕건이"))
        }
    }
}
