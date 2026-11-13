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
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 013 U1 확인 읽기 경계 (계획 013 설계 결정 4 "확인 읽기 규칙" 표).
 * 읽는 중 취소, 보내는 중 도착, 다시 보내기 전 도착, 간격 1ms 전, 같은 실패 반복, 확인 뒤에도 안내가 이어지는 것.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HospitalTripAckBoundaryTest {

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
        private val destination get() = when (val s = flow.value) {
            is GuidanceState.Guiding -> s.destination
            is GuidanceState.Arrived -> s.destination
            GuidanceState.Idle -> error("안내 중이 아니다")
        }
        fun remaining(d: Duration?) {
            flow.value = GuidanceState.Guiding(destination, d)
        }
        fun arrive() {
            flow.value = GuidanceState.Arrived(destination)
        }
    }

    /**
     * 읽기 결과를 차례로 준다 (다 쓰면 Pending). [sendGate]가 있으면 보내기가, [readGate]가 있으면 읽기가 그 값을 기다린다.
     * 보낸 건 참조·읽은 참조·갱신·끝 알림을 기록한다.
     */
    private class Sender(vararg results: AckRead) : CaseSender {
        val queue = ArrayDeque(results.toList())
        var reads = 0
        val readRefs = mutableListOf<CaseRef>()
        var failSend = false
        var sendGate: CompletableDeferred<Unit>? = null
        var readGate: CompletableDeferred<AckRead>? = null
        val etaUpdates = mutableListOf<Int?>()
        val finishes = mutableListOf<CaseStatus>()
        private var sent = 0

        override suspend fun send(hospital: Hospital, card: CaseCard, eta: CaseEta): SendResult {
            sendGate?.await()
            if (failSend) return SendResult.Failed("HTTP 503")
            sent++
            return SendResult.Sent(CaseRef(hospital.id!!, "case-$sent"))
        }
        override suspend fun updateEta(ref: CaseRef, eta: CaseEta): Boolean {
            etaUpdates += eta.remainingMinutes
            return true
        }
        override suspend fun finish(ref: CaseRef, status: CaseStatus, at: Instant): Boolean {
            finishes += status
            return true
        }
        override suspend fun readAck(ref: CaseRef): AckRead {
            reads++
            readRefs += ref
            readGate?.let { return it.await() }
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

    private class Fixture(val trip: HospitalTrip, val guidance: Guidance, val log: MutableList<String>) {
        val delivery get() = trip.trip.value?.delivery
    }

    private fun TestScope.fixture(sender: Sender, ackPollInterval: Duration? = 4.seconds): Fixture {
        val guidance = Guidance()
        val log = mutableListOf<String>()
        val trip = if (ackPollInterval == null) {
            HospitalTrip(
                guidance = guidance,
                senderFor = { sender },
                scope = backgroundScope,
                now = { base.plusMillis(testScheduler.currentTime) },
                log = { log += it },
            )
        } else {
            HospitalTrip(
                guidance = guidance,
                senderFor = { sender },
                scope = backgroundScope,
                now = { base.plusMillis(testScheduler.currentTime) },
                log = { log += it },
                etaInterval = 4.seconds,
                ackPollInterval = ackPollInterval,
            )
        }
        return Fixture(trip, guidance, log)
    }

    private fun TestScope.advance(d: Duration) {
        advanceTimeBy(d)
        runCurrent()
    }

    @Test
    fun `간격을 주지 않으면 실제 길안내처럼 15초마다 읽는다`() = runTest {
        val sender = Sender()
        val f = fixture(sender, ackPollInterval = null)
        f.trip.depart(choice(), card)
        runCurrent()
        advance(15.seconds - 1.milliseconds)
        assertEquals(0, sender.reads)
        advance(1.milliseconds)
        assertEquals(1, sender.reads)
        advance(15.seconds - 1.milliseconds)
        assertEquals(1, sender.reads)
        advance(1.milliseconds)
        assertEquals(2, sender.reads)
    }

    @Test
    fun `아직이면 정확히 간격마다 다시 읽고 로그를 남기지 않는다`() = runTest {
        val sender = Sender()
        val f = fixture(sender)
        f.trip.depart(choice(), card)
        runCurrent()
        advance(4.seconds)
        assertEquals(1, sender.reads)
        advance(4.seconds - 1.milliseconds)
        assertEquals(1, sender.reads)
        advance(1.milliseconds)
        assertEquals(2, sender.reads)
        assertFalse(f.log.any { it.startsWith("병원 확인") })
        assertEquals(CardDelivery.Sent(toServer = true), f.delivery)
    }

    @Test
    fun `보낸 건의 참조로 자기 건의 확인을 읽는다`() = runTest {
        val sender = Sender()
        val f = fixture(sender)
        f.trip.depart(choice(), card)
        advance(8.seconds)
        assertEquals(listOf(CaseRef("demo-fast", "case-1"), CaseRef("demo-fast", "case-1")), sender.readRefs)
    }

    @Test
    fun `읽기 실패가 이어져도 표시는 보냄 그대로이고 간격마다 다시 읽는다`() = runTest {
        val sender = Sender(*Array(10) { AckRead.Failed("네트워크") })
        val f = fixture(sender)
        f.trip.depart(choice(), card)
        advance(40.seconds)
        assertEquals(10, sender.reads)
        assertEquals(CardDelivery.Sent(toServer = true), f.delivery)
        assertEquals(listOf("병원 확인: 읽기 실패 (네트워크)"), f.log.filter { it.startsWith("병원 확인") })
    }

    @Test
    fun `실패 뒤 건이 지워지면 멈추고 실패와 건 없음 로그가 한 줄씩이다`() = runTest {
        val sender = Sender(AckRead.Failed("HTTP 503"), AckRead.Failed("HTTP 503"), AckRead.Gone, AckRead.Acknowledged(ackAt))
        val f = fixture(sender)
        f.trip.depart(choice(), card)
        advance(1.minutes)
        assertEquals(3, sender.reads)
        assertEquals(CardDelivery.Sent(toServer = true), f.delivery)
        assertEquals(
            listOf("병원 확인: 읽기 실패 (HTTP 503)", "병원 확인: 건을 읽을 수 없음"),
            f.log.filter { it.startsWith("병원 확인") },
        )
    }

    @Test
    fun `취소되는 순간 읽던 결과는 쓰지 않고 그 뒤로 읽지 않는다`() = runTest {
        val sender = Sender()
        val gate = CompletableDeferred<AckRead>()
        sender.readGate = gate
        val f = fixture(sender)
        f.trip.depart(choice(), card)
        advance(4.seconds)
        assertEquals(1, sender.reads) // 읽는 중
        f.guidance.cancel()
        runCurrent()
        gate.complete(AckRead.Acknowledged(ackAt))
        advance(1.minutes)
        assertEquals(1, sender.reads)
        assertNull(f.trip.trip.value)
        assertFalse(f.log.contains("병원 확인: 확인함"))
        assertEquals(listOf(CaseStatus.CANCELLED), sender.finishes)
    }

    @Test
    fun `확인된 뒤 취소되면 고른 병원과 확인 표시가 함께 사라진다`() = runTest {
        val sender = Sender(AckRead.Acknowledged(ackAt))
        val f = fixture(sender)
        f.trip.depart(choice(), card)
        advance(4.seconds)
        assertEquals(CardDelivery.Sent(toServer = true, acknowledgedAt = ackAt), f.delivery)
        f.guidance.cancel()
        advance(10.seconds)
        assertNull(f.trip.trip.value)
        assertEquals(1, sender.reads)
    }

    @Test
    fun `보내는 중에 도착하면 보낸 뒤에도 확인을 한 번도 읽지 않는다`() = runTest {
        val sender = Sender(AckRead.Acknowledged(ackAt))
        val sendGate = CompletableDeferred<Unit>()
        sender.sendGate = sendGate
        val f = fixture(sender)
        f.trip.depart(choice(), card)
        runCurrent()
        assertEquals(CardDelivery.Sending, f.delivery)
        f.guidance.arrive()
        runCurrent()
        sendGate.complete(Unit)
        advance(1.minutes)
        assertEquals(0, sender.reads)
        assertEquals(CardDelivery.Sent(toServer = true), f.delivery)
        assertEquals(listOf(CaseStatus.ARRIVED), sender.finishes)
    }

    @Test
    fun `다시 보내기를 기다리다 도착하면 다시 보내 성공해도 확인을 읽지 않는다`() = runTest {
        val sender = Sender(AckRead.Acknowledged(ackAt)).apply { failSend = true }
        val f = fixture(sender)
        f.trip.depart(choice(), card)
        advance(10.seconds)
        assertEquals(CardDelivery.Failed, f.delivery)
        f.guidance.arrive()
        runCurrent()
        sender.failSend = false
        f.trip.resend()
        advance(1.minutes)
        assertEquals(CardDelivery.Sent(toServer = true), f.delivery)
        assertEquals(0, sender.reads)
    }

    @Test
    fun `확인된 뒤에도 도착 예정 갱신과 도착 알림은 이어진다`() = runTest {
        val sender = Sender(AckRead.Acknowledged(ackAt))
        val f = fixture(sender)
        f.trip.depart(choice(), card)
        runCurrent()
        f.guidance.remaining(20.minutes)
        advance(4.seconds)
        assertEquals(CardDelivery.Sent(toServer = true, acknowledgedAt = ackAt), f.delivery)
        val before = sender.etaUpdates.size
        f.guidance.remaining(18.minutes)
        advance(10.seconds)
        assertEquals(before + 1, sender.etaUpdates.size)
        assertEquals(18, sender.etaUpdates.last())
        f.guidance.arrive()
        advance(10.seconds)
        assertEquals(listOf(CaseStatus.ARRIVED), sender.finishes)
        assertEquals(CardDelivery.Sent(toServer = true, acknowledgedAt = ackAt), f.delivery)
        assertEquals(1, sender.reads)
    }

    @Test
    fun `확인된 뒤 남은 분이 커져 시작값이 올라도 확인 표시가 남는다`() = runTest {
        val sender = Sender(AckRead.Acknowledged(ackAt))
        val f = fixture(sender)
        f.trip.depart(choice(), card)
        runCurrent()
        f.guidance.remaining(20.minutes)
        advance(4.seconds)
        f.guidance.remaining(25.minutes) // 재탐색
        runCurrent()
        val state = f.trip.trip.value!!
        assertEquals(25, state.startMinutes)
        assertEquals(CardDelivery.Sent(toServer = true, acknowledgedAt = ackAt), state.delivery)
    }

    @Test
    fun `도착 화면을 닫고 새로 출발하면 확인 표시는 이어지지 않고 새 건을 처음부터 읽는다`() = runTest {
        val sender = Sender(AckRead.Acknowledged(ackAt))
        val f = fixture(sender)
        f.trip.depart(choice(), card)
        advance(4.seconds)
        f.guidance.arrive()
        runCurrent()
        f.guidance.dismissArrival()
        f.trip.clear()
        runCurrent()
        assertEquals(1, sender.reads)

        f.trip.depart(choice(), card)
        runCurrent()
        assertEquals(CardDelivery.Sent(toServer = true), f.delivery)
        advance(4.seconds - 1.milliseconds)
        assertEquals(1, sender.reads)
        advance(1.milliseconds)
        assertEquals(2, sender.reads)
        assertEquals(CaseRef("demo-fast", "case-2"), sender.readRefs.last())
        assertTrue(f.delivery is CardDelivery.Sent)
        assertNull((f.delivery as CardDelivery.Sent).acknowledgedAt)
    }
}
