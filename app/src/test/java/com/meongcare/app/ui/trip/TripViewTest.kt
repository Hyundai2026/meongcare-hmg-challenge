package com.meongcare.app.ui.trip

import com.meongcare.app.domain.inquiry.InquiryRow
import com.meongcare.app.domain.inquiry.InquiryStatus
import com.meongcare.app.domain.trip.CardDelivery
import com.meongcare.app.domain.trip.TripState
import com.meongcare.app.ui.guidance.DeliveryLine
import com.meongcare.app.ui.guidance.TripPanel
import com.meongcare.app.ui.hospital.DemoHospitals
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TripViewTest {
    private val panel = TripPanel("온유동물병원", 10, LocalTime.of(15, 14), 0.3f, null, DeliveryLine.Sample, arrived = false)
    private val candidate = DemoHospitals.all[1].row.candidate
    private fun view(delivery: CardDelivery, status: InquiryStatus?) = tripViewOf(
        panel, TripState(candidate, delivery), status?.let { InquiryRow("온유동물병원", 15, it) }, "보리", "대기 글", true, "켜짐",
    )

    @Test
    fun `응답 상태는 카드 없음이 먼저고 그다음 진료 가능·불가·미확인이다`() {
        assertEquals(TripReply.NO_CARD, view(CardDelivery.NotSent, InquiryStatus.AVAILABLE).reply)
        assertEquals(TripReply.ACCEPTED, view(CardDelivery.Sent(false), InquiryStatus.AVAILABLE).reply)
        assertEquals(TripReply.REFUSED, view(CardDelivery.Sent(false), InquiryStatus.UNAVAILABLE).reply)
        assertEquals(TripReply.UNKNOWN, view(CardDelivery.Sent(false), InquiryStatus.WAITING).reply)
        assertEquals(TripReply.UNKNOWN, view(CardDelivery.Sent(false), null).reply)
    }

    @Test
    fun `병원 대기는 진료 가능 답일 때만 있고 시연 병원은 전화할 수 있다`() {
        assertEquals("대기 글", view(CardDelivery.Sent(false), InquiryStatus.AVAILABLE).waitText)
        assertNull(view(CardDelivery.Sent(false), InquiryStatus.WAITING).waitText)
        val v = view(CardDelivery.Sent(false), null)
        assertEquals("출발 때 이동 시간(줄지 않음)", 15, v.remainingMinutes)
        assertEquals("온유동물병원", v.hospitalName)
        assertTrue(v.canCall)
    }
}
