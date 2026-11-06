package com.meongcare.app.ui.guidance

import com.meongcare.app.domain.guidance.Destination
import com.meongcare.app.domain.hospital.Congestion
import com.meongcare.app.domain.hospital.Hospital
import com.meongcare.app.domain.hospital.HospitalCandidate
import com.meongcare.app.domain.hospital.HospitalOrigin
import com.meongcare.app.domain.hospital.HospitalStatus
import com.meongcare.app.domain.trip.CardDelivery
import com.meongcare.app.domain.trip.TripState
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 013: 안내 영역 상태 계산 [tripPanelOf] */
class TripPanelTest {
    private val seoul = ZoneId.of("Asia/Seoul")
    private val name = "시연용 바른 동물병원"

    /** 서울 14:17:42 */
    private val now = Instant.parse("2026-11-06T05:17:42Z")

    private val status = HospitalStatus(10, Congestion.NORMAL, true, Instant.parse("2026-11-06T05:10:00Z"))
    private val candidate = HospitalCandidate(
        Hospital(Destination(name, 37.367, 127.1083), "02-0000-0001", isSample = true, id = "demo-fast"),
        status,
        HospitalOrigin.SERVER,
    )

    private fun trip(delivery: CardDelivery = CardDelivery.Sent(toServer = true), start: Int? = 20) =
        TripState(candidate, delivery, startMinutes = start)

    @Test
    fun `안내 없음이면 패널이 없다`() {
        assertNull(tripPanelOf(GuidanceUiState.Idle(name), trip(), now, seoul))
        assertNull(tripPanelOf(GuidanceUiState.Idle(name), null, now, seoul))
    }

    @Test
    fun `도착 예정은 지금에 남은 분을 더한 시각이고 분 아래는 버린다`() {
        val panel = tripPanelOf(GuidanceUiState.Guiding(name, 15), trip(), now, seoul)!!
        assertEquals(15, panel.remainingMinutes)
        assertEquals(LocalTime.of(14, 32), panel.arrivesAt) // 14:32:42 → 14:32
        // 자정을 넘긴다: 서울 23:50:10 + 15분 → 00:05
        val late = Instant.parse("2026-11-06T14:50:10Z")
        assertEquals(LocalTime.of(0, 5), tripPanelOf(GuidanceUiState.Guiding(name, 15), trip(), late, seoul)!!.arrivesAt)
    }

    @Test
    fun `경로를 찾는 중이면 남은 분과 도착 예정이 없다`() {
        val panel = tripPanelOf(GuidanceUiState.Guiding(name, null), trip(), now, seoul)!!
        assertNull(panel.remainingMinutes)
        assertNull(panel.arrivesAt)
        assertNull(panel.progress)
        assertFalse(panel.arrived)
    }

    @Test
    fun `진행은 시작 대비 지난 비율이고 0과 1 사이다`() {
        assertEquals(0f, tripPanelOf(GuidanceUiState.Guiding(name, 20), trip(start = 20), now, seoul)!!.progress!!, 0f)
        assertEquals(0.25f, tripPanelOf(GuidanceUiState.Guiding(name, 15), trip(start = 20), now, seoul)!!.progress!!, 0.0001f)
        assertEquals(0.95f, tripPanelOf(GuidanceUiState.Guiding(name, 1), trip(start = 20), now, seoul)!!.progress!!, 0.0001f)
        // 남은 분이 시작보다 크면(아직 시작값이 따라오기 전) 0으로 자른다
        assertEquals(0f, tripPanelOf(GuidanceUiState.Guiding(name, 30), trip(start = 20), now, seoul)!!.progress!!, 0f)
    }

    @Test
    fun `시작이나 남은 분을 모르면 진행 막대가 없다`() {
        assertNull(tripPanelOf(GuidanceUiState.Guiding(name, 15), trip(start = null), now, seoul)!!.progress)
        assertNull(tripPanelOf(GuidanceUiState.Guiding(name, 15), trip(start = 0), now, seoul)!!.progress)
        assertNull(tripPanelOf(GuidanceUiState.Guiding(name, null), trip(start = 20), now, seoul)!!.progress)
    }

    @Test
    fun `서버로 보낸 카드가 확인되면 확인 줄이다`() {
        val acked = trip(CardDelivery.Sent(toServer = true, acknowledgedAt = Instant.parse("2026-11-06T05:05:59Z")))
        assertEquals(DeliveryLine.Acknowledged(LocalTime.of(14, 5)), tripPanelOf(GuidanceUiState.Guiding(name, 15), acked, now, seoul)!!.delivery)
        assertEquals(DeliveryLine.SentWaiting, tripPanelOf(GuidanceUiState.Guiding(name, 15), trip(), now, seoul)!!.delivery)
    }

    @Test
    fun `시연용 전송은 확인 줄이 없다`() {
        // 확인 시각이 들어 있어도 시연용(내장 병원) 전송이면 확인으로 보이지 않는다
        val sample = trip(CardDelivery.Sent(toServer = false, acknowledgedAt = Instant.parse("2026-11-06T05:05:00Z")))
        assertEquals(DeliveryLine.Sample, tripPanelOf(GuidanceUiState.Guiding(name, 15), sample, now, seoul)!!.delivery)
    }

    @Test
    fun `다른 전송 상태는 그대로 줄이 된다`() {
        val lines = mapOf(
            CardDelivery.Sending to DeliveryLine.Sending,
            CardDelivery.Failed to DeliveryLine.Failed,
            CardDelivery.NotSent to DeliveryLine.NotSent,
        )
        for ((delivery, line) in lines) {
            assertEquals(line, tripPanelOf(GuidanceUiState.Guiding(name, 15), trip(delivery), now, seoul)!!.delivery)
        }
    }

    @Test
    fun `대기 정보는 출발할 때 받은 값이다`() {
        assertEquals(status, tripPanelOf(GuidanceUiState.Guiding(name, 15), trip(), now, seoul)!!.statusAtDeparture)
        val noStatus = TripState(candidate.copy(status = null), CardDelivery.Sent(toServer = true))
        assertNull(tripPanelOf(GuidanceUiState.Guiding(name, 15), noStatus, now, seoul)!!.statusAtDeparture)
    }

    @Test
    fun `도착이면 도착 패널이고 막대와 도착 예정이 없다`() {
        val acked = trip(CardDelivery.Sent(toServer = true, acknowledgedAt = Instant.parse("2026-11-06T05:05:00Z")))
        val panel = tripPanelOf(GuidanceUiState.Arrived(name), acked, now, seoul)!!
        assertTrue(panel.arrived)
        assertEquals(name, panel.destinationName)
        assertNull(panel.progress)
        assertNull(panel.arrivesAt)
        assertNull(panel.remainingMinutes)
        assertEquals(DeliveryLine.Acknowledged(LocalTime.of(14, 5)), panel.delivery)
    }

    @Test
    fun `trip이 없으면 전송 줄과 막대가 없다`() {
        val panel = tripPanelOf(GuidanceUiState.Guiding(name, 15), null, now, seoul)!!
        assertEquals(DeliveryLine.None, panel.delivery)
        assertNull(panel.statusAtDeparture)
        assertNull(panel.progress)
        assertEquals(LocalTime.of(14, 32), panel.arrivesAt)
    }
}
