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
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 013 안내 영역 상태 계산 경계 ([tripPanelOf], 계획 013 설계 결정 7 표) */
class TripPanelBoundaryTest {
    private val seoul = ZoneId.of("Asia/Seoul")
    private val name = "시연용 바른 동물병원"

    /** 서울 14:17:00.000 (분 정각) */
    private val now = Instant.parse("2026-11-06T05:17:00Z")

    private val status = HospitalStatus(10, Congestion.NORMAL, true, Instant.parse("2026-11-06T05:10:00Z"))
    private val candidate = HospitalCandidate(
        Hospital(Destination(name, 37.367, 127.1083), "02-0000-0001", isSample = true, id = "demo-fast"),
        status,
        HospitalOrigin.SERVER,
    )

    private fun trip(delivery: CardDelivery = CardDelivery.Sent(toServer = true), start: Int? = 20) =
        TripState(candidate, delivery, startMinutes = start)

    @Test
    fun `이름은 고른 병원이 아니라 길안내 상태의 목적지 이름이다`() {
        val guiding = tripPanelOf(GuidanceUiState.Guiding("내비가 알려 준 이름", 10), trip(), now, seoul)!!
        assertEquals("내비가 알려 준 이름", guiding.destinationName)
        val arrived = tripPanelOf(GuidanceUiState.Arrived("내비가 알려 준 이름"), trip(), now, seoul)!!
        assertEquals("내비가 알려 준 이름", arrived.destinationName)
    }

    @Test
    fun `분 정각에 남은 분을 더하면 그 분이 도착 예정이다`() {
        assertEquals(LocalTime.of(14, 32), tripPanelOf(GuidanceUiState.Guiding(name, 15), trip(), now, seoul)!!.arrivesAt)
        // 59.999초여도 올리지 않고 버린다
        val almost = Instant.parse("2026-11-06T05:17:59.999Z")
        assertEquals(LocalTime.of(14, 32), tripPanelOf(GuidanceUiState.Guiding(name, 15), trip(), almost, seoul)!!.arrivesAt)
    }

    @Test
    fun `도착 예정과 확인 시각은 주어진 시간대로 계산한다`() {
        val acked = trip(CardDelivery.Sent(toServer = true, acknowledgedAt = Instant.parse("2026-11-06T05:05:30Z")))
        val utc = tripPanelOf(GuidanceUiState.Guiding(name, 15), acked, now, ZoneOffset.UTC)!!
        assertEquals(LocalTime.of(5, 32), utc.arrivesAt)
        assertEquals(DeliveryLine.Acknowledged(LocalTime.of(5, 5)), utc.delivery)
        val kst = tripPanelOf(GuidanceUiState.Guiding(name, 15), acked, now, seoul)!!
        assertEquals(LocalTime.of(14, 32), kst.arrivesAt)
        assertEquals(DeliveryLine.Acknowledged(LocalTime.of(14, 5)), kst.delivery)
    }

    @Test
    fun `자정을 넘긴 확인 시각은 그날의 시와 분이다`() {
        // 서울 00:00:30
        val acked = trip(CardDelivery.Sent(toServer = true, acknowledgedAt = Instant.parse("2026-11-06T15:00:30Z")))
        assertEquals(
            DeliveryLine.Acknowledged(LocalTime.of(0, 0)),
            tripPanelOf(GuidanceUiState.Guiding(name, 15), acked, now, seoul)!!.delivery,
        )
    }

    @Test
    fun `하루를 넘는 남은 분도 도착 예정 시각만 보인다`() {
        // 14:17 + 24시간 10분 → 다음 날 14:27
        assertEquals(
            LocalTime.of(14, 27),
            tripPanelOf(GuidanceUiState.Guiding(name, 24 * 60 + 10), trip(), now, seoul)!!.arrivesAt,
        )
    }

    @Test
    fun `진행 시작이 음수면 막대가 없고 0과 1 사이의 비율은 그대로다`() {
        assertNull(tripPanelOf(GuidanceUiState.Guiding(name, 5), trip(start = -3), now, seoul)!!.progress)
        assertEquals(2f / 3f, tripPanelOf(GuidanceUiState.Guiding(name, 1), trip(start = 3), now, seoul)!!.progress!!, 0.0001f)
        assertEquals(0.5f, tripPanelOf(GuidanceUiState.Guiding(name, 10), trip(start = 20), now, seoul)!!.progress!!, 0.0001f)
    }

    @Test
    fun `도착했는데 trip이 없으면 전송 줄과 대기 정보가 없다`() {
        val panel = tripPanelOf(GuidanceUiState.Arrived(name), null, now, seoul)!!
        assertTrue(panel.arrived)
        assertEquals(DeliveryLine.None, panel.delivery)
        assertNull(panel.statusAtDeparture)
        assertNull(panel.progress)
        assertNull(panel.arrivesAt)
    }

    @Test
    fun `도착 패널도 출발할 때 받은 대기 정보를 가진다`() {
        assertEquals(status, tripPanelOf(GuidanceUiState.Arrived(name), trip(), now, seoul)!!.statusAtDeparture)
    }

    @Test
    fun `도착해도 보내는 중과 실패와 카드 없이는 그대로 줄이 된다`() {
        val lines = mapOf(
            CardDelivery.Sending to DeliveryLine.Sending,
            CardDelivery.Failed to DeliveryLine.Failed,
            CardDelivery.NotSent to DeliveryLine.NotSent,
        )
        for ((delivery, line) in lines) {
            assertEquals("$delivery", line, tripPanelOf(GuidanceUiState.Arrived(name), trip(delivery), now, seoul)!!.delivery)
        }
    }

    @Test
    fun `경로를 찾는 중에도 전송 줄과 대기 정보는 보인다`() {
        val acked = trip(CardDelivery.Sent(toServer = true, acknowledgedAt = Instant.parse("2026-11-06T05:05:00Z")))
        val panel = tripPanelOf(GuidanceUiState.Guiding(name, null), acked, now, seoul)!!
        assertFalse(panel.arrived)
        assertEquals(DeliveryLine.Acknowledged(LocalTime.of(14, 5)), panel.delivery)
        assertEquals(status, panel.statusAtDeparture)
    }
}
