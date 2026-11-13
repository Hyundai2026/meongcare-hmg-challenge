package com.meongcare.app.domain.inquiry

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VetInquiryTest {
    private val t0 = Instant.parse("2026-11-13T06:00:00Z")
    private fun at(sec: Long) = t0.plusSeconds(sec)

    private val hospitals = listOf(
        InquiryHospital("별숲동물의료센터", 10),
        InquiryHospital("온유동물병원", 15),
        InquiryHospital("나래동물의료센터", 20),
        InquiryHospital("달빛24시동물의료센터", 25),
        InquiryHospital("가람24시동물병원", 30),
    )

    private fun started(script: InquiryScript = InquiryScripts.DEFAULT) = VetInquiry(hospitals, script).also { it.start(t0) }

    @Test
    fun `시작하면 이동이 짧은 3곳이 답변 대기이고 이동 순이다`() {
        val s = started().snapshot(t0)
        assertEquals(listOf("별숲동물의료센터", "온유동물병원", "나래동물의료센터"), s.searchRows.map { it.key })
        assertTrue(s.searchRows.all { it.status == InquiryStatus.WAITING })
        assertEquals(InquirySummary.Waiting, s.summary)
        assertEquals(2, s.restCount)
        assertEquals(0, s.okCount)
        assertNull(s.best)
    }

    @Test
    fun `응답 시각 전후로 상태가 바뀌고 진료 시작까지는 이동과 남은 대기 중 긴 쪽이다`() {
        val inq = started()
        assertEquals(InquiryStatus.WAITING, inq.snapshot(at(3)).searchRows.first { it.key == "온유동물병원" }.status)
        val s = inq.snapshot(at(8))
        val onyu = s.searchRows.first { it.key == "온유동물병원" }
        assertEquals(InquiryStatus.AVAILABLE, onyu.status)
        assertEquals(15, onyu.startMinutes) // max(15, 5)
        assertEquals(0, onyu.addMinutes)
        val byul = s.searchRows.first { it.key == "별숲동물의료센터" }
        assertEquals(30, byul.startMinutes) // max(10, 30)
        assertEquals(20, byul.addMinutes)
        assertEquals(InquirySummary.Accepting(2), s.summary)
        assertEquals("온유동물병원", s.best?.key)
        assertEquals(listOf("온유동물병원", "별숲동물의료센터", "나래동물의료센터"), s.searchRows.map { it.key })
    }

    @Test
    fun `20초를 넘으면 답 없는 병원은 응답 지연이다`() {
        val inq = started()
        assertEquals(InquiryStatus.WAITING, inq.snapshot(at(20)).searchRows.first { it.key == "나래동물의료센터" }.status)
        assertEquals(InquiryStatus.DELAYED, inq.snapshot(at(21)).searchRows.first { it.key == "나래동물의료센터" }.status)
    }

    @Test
    fun `응답 뒤 시간이 지나면 남은 대기가 줄고 10분이면 재확인이 필요하다`() {
        val s = started().snapshot(at(7 + 12 * 60))
        val byul = s.searchRows.first { it.key == "별숲동물의료센터" }
        assertEquals(12, byul.elapsedMinutes)
        assertEquals(18, byul.startMinutes) // max(10, 30-12)
        assertTrue(byul.needsRecheck)
    }

    @Test
    fun `다른 병원에 문의하면 그때부터 시계가 돌고 남은 곳이 없다`() {
        val inq = started()
        assertEquals(2, inq.expand(at(30)))
        val s = inq.snapshot(at(36))
        assertEquals(0, s.restCount)
        assertEquals(InquiryStatus.UNAVAILABLE, s.searchRows.first { it.key == "달빛24시동물의료센터" }.status)
        assertEquals(InquiryStatus.WAITING, s.searchRows.first { it.key == "가람24시동물병원" }.status)
        assertFalse(s.compareRows.any { it.key == "달빛24시동물의료센터" })
        val gram = inq.snapshot(at(40)).searchRows.first { it.key == "가람24시동물병원" }
        assertEquals(InquiryStatus.AVAILABLE, gram.status)
        assertNull(gram.startMinutes)
        assertEquals(0, inq.expand(at(41)))
    }

    @Test
    fun `응답 없음 팝업은 진료 가능이 없고 20초가 지났고 닫지 않았을 때만`() {
        val inq = started(InquiryScripts.NO_RESPONSE)
        assertFalse(inq.snapshot(at(20)).showNoResponsePrompt)
        val s = inq.snapshot(at(21))
        assertTrue(s.showNoResponsePrompt)
        assertEquals(listOf(10, 15, 20, 25, 30), s.noResponseRows.map { it.travelMinutes })
        inq.dismissNoResponse()
        assertFalse(inq.snapshot(at(22)).showNoResponsePrompt)
        assertFalse(started().snapshot(at(25)).showNoResponsePrompt)
    }

    @Test
    fun `모두 진료 불가면 수용 가능한 병원 없음이다`() {
        val all = InquiryScript(hospitals.associate { it.key to ScriptedReply(1, ScriptedAnswer.Unavailable) })
        assertEquals(InquirySummary.NoneAvailable, started(all).snapshot(at(2)).summary)
    }

    @Test
    fun `진행은 60초에서 멈추고 시작 전에는 0이다`() {
        assertEquals(0.5f, started().snapshot(at(30)).progress, 0.001f)
        assertEquals(1f, started().snapshot(at(120)).progress, 0.001f)
        assertEquals(0f, VetInquiry(hospitals, InquiryScripts.DEFAULT).snapshot(t0).progress, 0.001f)
    }
}
