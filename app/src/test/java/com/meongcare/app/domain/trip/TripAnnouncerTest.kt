package com.meongcare.app.domain.trip

import com.meongcare.app.domain.guidance.Destination
import com.meongcare.app.domain.guidance.GuidanceState
import com.meongcare.app.domain.hospital.Congestion
import com.meongcare.app.domain.hospital.Hospital
import com.meongcare.app.domain.hospital.HospitalCandidate
import com.meongcare.app.domain.hospital.HospitalOrigin
import com.meongcare.app.domain.hospital.HospitalStatus
import com.meongcare.app.domain.speech.SpeakFailure
import com.meongcare.app.domain.speech.SpeakResult
import com.meongcare.app.domain.speech.Speaker
import java.time.Instant
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TripAnnouncerTest {

    private val base = Instant.parse("2026-11-12T05:00:00Z")
    private val name = "행복동물병원"
    private val destination = Destination(name, 37.5, 127.0)

    private fun candidate(origin: HospitalOrigin = HospitalOrigin.SERVER, hospitalName: String = name) = HospitalCandidate(
        Hospital(Destination(hospitalName, 37.5, 127.0), "02-0000-0001", isSample = false, id = "h1"),
        HospitalStatus(10, Congestion.NORMAL, true, base),
        origin,
    )

    /** 받은 문장과 시각을 기록하고, [duration] 동안 읽는 척한다. [results]가 있으면 차례로 돌려준다. */
    private class RecordingSpeaker(private val duration: Duration = 2.seconds) : Speaker {
        val started = mutableListOf<Pair<String, Long>>()
        val cancelled = mutableListOf<String>()
        val results = ArrayDeque<SpeakResult>()
        var clock: () -> Long = { 0L }

        override suspend fun speak(text: String): SpeakResult {
            started += text to clock()
            try {
                delay(duration)
            } catch (e: CancellationException) {
                cancelled += text
                throw e
            }
            return results.removeFirstOrNull() ?: SpeakResult.Done
        }

        val texts get() = started.map { it.first }
    }

    private class Fixture(
        val trip: MutableStateFlow<TripState?>,
        val guidance: MutableStateFlow<GuidanceState>,
        val speaker: RecordingSpeaker,
        val logs: MutableList<String>,
    )

    private fun TestScope.fixture(speaker: RecordingSpeaker = RecordingSpeaker()): Fixture {
        val trip = MutableStateFlow<TripState?>(null)
        val guidance = MutableStateFlow<GuidanceState>(GuidanceState.Idle)
        val logs = mutableListOf<String>()
        speaker.clock = { currentTime }
        TripAnnouncer(trip, guidance, speaker, backgroundScope, log = { logs += it }).start()
        runCurrent()
        return Fixture(trip, guidance, speaker, logs)
    }

    private fun Fixture.setDelivery(delivery: CardDelivery) = trip.update { it?.copy(delivery = delivery) }

    /**
     * 남은 일을 모두 돌린다. 안내자는 [TestScope.backgroundScope]에서 돌아서 `advanceUntilIdle`이 그 시간을 넘기지 않는다.
     * 그래서 가상 시간을 넉넉히 넘긴다 (말하기 2~3초, 출발 대기 5초보다 길게).
     */
    private fun TestScope.settle() {
        advanceTimeBy(30_000)
        runCurrent()
    }

    @Test
    fun `출발하면 카드 전송 결과를 기다렸다가 한 번 말한다`() = runTest {
        val f = fixture()

        f.trip.value = TripState(candidate(), CardDelivery.Sending)
        advanceTimeBy(1_000)
        runCurrent()
        assertEquals("결과를 기다리는 동안은 말하지 않는다", emptyList<String>(), f.speaker.texts)

        f.setDelivery(CardDelivery.Sent(toServer = true))
        settle()

        assertEquals(listOf("${name}까지 안내를 시작해요. 진료 카드를 보냈어요."), f.speaker.texts)
        assertEquals(1_000L, f.speaker.started.single().second)
    }

    @Test
    fun `전송이 오래 걸리면 출발만 말하고 나중에 보냈다고 말한다`() = runTest {
        val f = fixture()

        f.trip.value = TripState(candidate(), CardDelivery.Sending)
        advanceTimeBy(5_001)
        runCurrent()
        assertEquals(listOf("${name}까지 안내를 시작해요."), f.speaker.texts)

        f.setDelivery(CardDelivery.Sent(toServer = true))
        settle()

        assertEquals(
            listOf("${name}까지 안내를 시작해요.", "진료 카드를 ${name}에 보냈어요."),
            f.speaker.texts,
        )
    }

    @Test
    fun `카드 없이 출발했거나 근처 병원이면 카드 이야기를 하지 않는다`() = runTest {
        val f = fixture()

        f.trip.value = TripState(candidate(), CardDelivery.NotSent)
        runCurrent()
        assertEquals(listOf("${name}까지 안내를 시작해요."), f.speaker.texts)
        assertEquals("기다리지 않고 바로 말한다", 0L, f.speaker.started.single().second)

        f.trip.value = null
        runCurrent()
        f.trip.value = TripState(candidate(HospitalOrigin.NEARBY_SEARCH, "근처 동물병원"), CardDelivery.NotSent)
        settle()

        assertEquals("근처 동물병원까지 안내를 시작해요.", f.speaker.texts.last())
        assertTrue(f.speaker.texts.none { it.contains("진료 카드") })
    }

    @Test
    fun `시연용 병원은 보냈다고 말하지 않는다`() = runTest {
        val f = fixture()

        f.trip.value = TripState(candidate(HospitalOrigin.BUILT_IN), CardDelivery.Sending)
        runCurrent()
        f.setDelivery(CardDelivery.Sent(toServer = false))
        settle()

        assertEquals(listOf("${name}까지 안내를 시작해요."), f.speaker.texts)
    }

    @Test
    fun `전송이 실패하면 한 번만 알린다`() = runTest {
        val f = fixture()

        f.trip.value = TripState(candidate(), CardDelivery.Sending)
        runCurrent()
        f.setDelivery(CardDelivery.Failed)
        settle()
        f.setDelivery(CardDelivery.Sending)
        runCurrent()
        f.setDelivery(CardDelivery.Failed)
        settle()
        f.setDelivery(CardDelivery.Sending)
        runCurrent()
        f.setDelivery(CardDelivery.Sent(toServer = true))
        settle()

        assertEquals(
            listOf(
                "${name}까지 안내를 시작해요.",
                "진료 카드를 보내지 못했어요. 정차하면 다시 보낼 수 있어요.",
                "진료 카드를 ${name}에 보냈어요.",
            ),
            f.speaker.texts,
        )
    }

    @Test
    fun `병원이 확인하면 한 번만 말한다`() = runTest {
        val f = fixture()

        f.trip.value = TripState(candidate(), CardDelivery.Sent(toServer = true))
        settle()
        f.setDelivery(CardDelivery.Sent(toServer = true, acknowledgedAt = base))
        settle()
        f.trip.update { it?.copy(startMinutes = 12) }
        f.setDelivery(CardDelivery.Sent(toServer = true, acknowledgedAt = base.plusSeconds(30)))
        settle()

        assertEquals(
            listOf("${name}까지 안내를 시작해요. 진료 카드를 보냈어요.", "${name}에서 진료 카드를 확인했어요."),
            f.speaker.texts,
        )
    }

    @Test
    fun `안내를 거친 도착만 한 번 말하고 그 뒤 사건은 말하지 않는다`() = runTest {
        val f = fixture()
        f.guidance.value = GuidanceState.Arrived(destination)

        f.trip.value = TripState(candidate(), CardDelivery.Sent(toServer = true))
        settle()
        assertEquals("안내 중을 보기 전의 도착은 이전 안내다", 1, f.speaker.texts.size)

        f.guidance.value = GuidanceState.Guiding(destination, 10.minutes)
        runCurrent()
        f.guidance.value = GuidanceState.Arrived(destination)
        settle()
        f.setDelivery(CardDelivery.Sent(toServer = true, acknowledgedAt = base))
        f.guidance.value = GuidanceState.Guiding(destination, 1.minutes)
        runCurrent()
        f.guidance.value = GuidanceState.Arrived(destination)
        settle()

        assertEquals(
            listOf(
                "${name}까지 안내를 시작해요. 진료 카드를 보냈어요.",
                "${name}에 도착했어요. 진료 카드를 접수처에 보여 주세요.",
            ),
            f.speaker.texts,
        )
    }

    @Test
    fun `앞 말이 끝나기 전에 생긴 사건은 끝난 뒤 순서대로 말한다`() = runTest {
        val f = fixture(RecordingSpeaker(duration = 3.seconds))

        f.trip.value = TripState(candidate(), CardDelivery.Sent(toServer = true))
        runCurrent()
        advanceTimeBy(1_000)
        f.setDelivery(CardDelivery.Sent(toServer = true, acknowledgedAt = base))
        f.guidance.value = GuidanceState.Guiding(destination, 5.minutes)
        runCurrent()
        f.guidance.value = GuidanceState.Arrived(destination)
        settle()

        assertEquals(
            listOf(
                "${name}까지 안내를 시작해요. 진료 카드를 보냈어요." to 0L,
                "${name}에서 진료 카드를 확인했어요." to 3_000L,
                "${name}에 도착했어요. 진료 카드를 접수처에 보여 주세요." to 6_000L,
            ),
            f.speaker.started,
        )
    }

    @Test
    fun `안내가 취소되거나 닫히면 남은 말을 버리고 말하는 중이면 멈춘다`() = runTest {
        val f = fixture(RecordingSpeaker(duration = 3.seconds))

        f.trip.value = TripState(candidate(), CardDelivery.Sent(toServer = true))
        runCurrent()
        f.setDelivery(CardDelivery.Sent(toServer = true, acknowledgedAt = base))
        runCurrent()
        advanceTimeBy(1_000)
        f.trip.value = null
        settle()

        assertEquals(listOf("${name}까지 안내를 시작해요. 진료 카드를 보냈어요."), f.speaker.texts)
        assertEquals(f.speaker.texts, f.speaker.cancelled)
    }

    @Test
    fun `말하기가 실패해도 다음 사건은 말한다`() = runTest {
        val speaker = RecordingSpeaker().apply { results += SpeakResult.Failed(SpeakFailure.AUDIO) }
        val f = fixture(speaker)

        f.trip.value = TripState(candidate(), CardDelivery.Sent(toServer = true))
        settle()
        f.setDelivery(CardDelivery.Sent(toServer = true, acknowledgedAt = base))
        settle()

        assertEquals(2, f.speaker.texts.size)
        assertEquals(listOf("소리 안내: 출발(카드 보냄) → 실패(AUDIO)", "소리 안내: 병원 확인 → 끝까지"), f.logs)
    }

    @Test
    fun `다음 안내에서는 사건을 처음부터 다시 센다`() = runTest {
        val f = fixture()

        repeat(2) {
            f.trip.value = TripState(candidate(), CardDelivery.Sent(toServer = true, acknowledgedAt = base))
            settle()
            f.trip.value = null
            settle()
        }

        assertEquals(
            List(2) {
                listOf("${name}까지 안내를 시작해요. 진료 카드를 보냈어요.", "${name}에서 진료 카드를 확인했어요.")
            }.flatten(),
            f.speaker.texts,
        )
    }

    @Test
    fun `남은 시간이 바뀌어도 말하지 않는다`() = runTest {
        val f = fixture()

        f.trip.value = TripState(candidate(), CardDelivery.NotSent)
        settle()
        for (minutes in listOf(15, 14, 13, 20, 1)) {
            f.trip.update { it?.copy(startMinutes = maxOf(it.startMinutes ?: 0, minutes)) }
            f.guidance.value = GuidanceState.Guiding(destination, minutes.minutes)
            settle()
        }

        assertEquals(1, f.speaker.texts.size)
    }

    @Test
    fun `로그에는 사건 종류와 결과만 남고 병원 이름은 없다`() = runTest {
        val f = fixture()

        f.trip.value = TripState(candidate(), CardDelivery.Sending)
        runCurrent()
        f.setDelivery(CardDelivery.Failed)
        settle()
        f.setDelivery(CardDelivery.Sent(toServer = true, acknowledgedAt = base))
        f.guidance.value = GuidanceState.Guiding(destination, 3.minutes)
        runCurrent()
        f.guidance.value = GuidanceState.Arrived(destination)
        settle()

        assertEquals(
            listOf(
                "소리 안내: 출발 → 끝까지",
                "소리 안내: 카드 못 보냄 → 끝까지",
                "소리 안내: 카드 보냄 → 끝까지",
                "소리 안내: 병원 확인 → 끝까지",
                "소리 안내: 도착 → 끝까지",
            ),
            f.logs,
        )
        assertTrue(f.logs.none { it.contains(name) || it.contains("동물병원") })
    }
}
