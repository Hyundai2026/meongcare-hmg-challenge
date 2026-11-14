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
    fun `시작하면 가까운 5곳 모두에 문의하고 이동 순으로 답변 대기다`() {
        val s = started().snapshot(t0)
        assertEquals(hospitals.map { it.key }, s.searchRows.map { it.key })
        assertTrue(s.searchRows.all { it.status == InquiryStatus.WAITING })
        assertEquals(InquirySummary.Waiting, s.summary)
        assertEquals(0, s.restCount)
        assertEquals(0, s.okCount)
        assertNull(s.best)
        assertEquals("별숲동물의료센터", s.nearest?.key)
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
        assertEquals(InquiryStatus.UNAVAILABLE, s.searchRows.first { it.key == "달빛24시동물의료센터" }.status)
        assertEquals(InquirySummary.Accepting(2), s.summary)
        assertEquals("온유동물병원", s.best?.key)
        assertEquals(listOf("온유동물병원", "별숲동물의료센터"), s.searchRows.take(2).map { it.key })
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
    fun `진료 불가는 비교와 가장 가까운 병원에서 빠지고 대기 미확인 가능은 시간이 없다`() {
        val s = started().snapshot(at(10))
        assertFalse(s.compareRows.any { it.key == "달빛24시동물의료센터" })
        val gram = s.searchRows.first { it.key == "가람24시동물병원" }
        assertEquals(InquiryStatus.AVAILABLE, gram.status)
        assertNull(gram.startMinutes)
        assertEquals(0, VetInquiry(hospitals, InquiryScripts.DEFAULT).also { it.start(t0) }.expand(at(1)))
    }

    @Test
    fun `응답 대기 시간 20초가 지나도 진료 가능 답이 없으면 팝업이고 가장 가까운 병원을 고른다`() {
        val inq = started(InquiryScripts.NO_RESPONSE)
        assertFalse(inq.snapshot(at(20)).showNoResponsePrompt)
        val s = inq.snapshot(at(21))
        assertTrue(s.showNoResponsePrompt)
        assertEquals("별숲동물의료센터", s.nearest?.key)
        assertEquals(listOf(10, 15, 20, 25, 30), s.noResponseRows.map { it.travelMinutes })
        // [조금 더 기다리기]: 20초를 다시 센다
        inq.dismissNoResponse(at(22))
        assertFalse(inq.snapshot(at(30)).showNoResponsePrompt)
        assertEquals(12, inq.snapshot(at(30)).remainingSeconds)
        assertFalse(inq.snapshot(at(42)).showNoResponsePrompt)
        assertTrue(inq.snapshot(at(43)).showNoResponsePrompt)
        assertFalse(started().snapshot(at(25)).showNoResponsePrompt)
    }

    @Test
    fun `가장 가까운 병원이 진료 불가면 그다음 가까운 곳이다`() {
        val script = InquiryScript(mapOf("별숲동물의료센터" to ScriptedReply(2, ScriptedAnswer.Unavailable)))
        assertEquals("온유동물병원", started(script).snapshot(at(21)).nearest?.key)
    }

    @Test
    fun `모두 진료 불가면 수용 가능한 병원 없음이다`() {
        val all = InquiryScript(hospitals.associate { it.key to ScriptedReply(1, ScriptedAnswer.Unavailable) })
        assertEquals(InquirySummary.NoneAvailable, started(all).snapshot(at(2)).summary)
    }

    @Test
    fun `카운트다운은 20초에서 0초로 줄고 막대도 1에서 0으로 준다`() {
        val inq = started()
        assertEquals(20, inq.snapshot(t0).remainingSeconds)
        assertEquals(1f, inq.snapshot(t0).progress, 0.001f)
        assertEquals(10, inq.snapshot(at(10)).remainingSeconds)
        assertEquals(0.5f, inq.snapshot(at(10)).progress, 0.001f)
        assertEquals(1, inq.snapshot(t0.plusMillis(19_500)).remainingSeconds)
        assertEquals(0, inq.snapshot(at(20)).remainingSeconds)
        assertEquals(0, inq.snapshot(at(90)).remainingSeconds)
        assertEquals(0f, inq.snapshot(at(90)).progress, 0.001f)
    }

    @Test
    fun `20초가 끝났을 때 진료 가능 답이 있으면 진료 시작까지 가장 짧은 곳을 제안한다`() {
        val inq = started()
        assertFalse("20초 전에는 제안하지 않는다", inq.snapshot(at(19)).showSuggestion)
        val s = inq.snapshot(at(20))
        assertTrue(s.showSuggestion)
        assertEquals("온유동물병원", s.suggestion?.key) // 온유 15분 < 별숲 30분 < 가람(미확인)
        assertFalse(s.showNoResponsePrompt)
        inq.dismissSuggestion()
        assertFalse(inq.snapshot(at(25)).showSuggestion)
        inq.start(at(30))
        assertTrue("새 문의면 다시 제안한다", inq.snapshot(at(50)).showSuggestion)
    }

    @Test
    fun `시간을 아는 곳이 없으면 진료 가능 답 중 먼저 오는 곳을 제안한다`() {
        val script = InquiryScript(mapOf("나래동물의료센터" to ScriptedReply(3, ScriptedAnswer.Available(null))))
        val s = started(script).snapshot(at(20))
        assertEquals("나래동물의료센터", s.suggestion?.key)
        assertTrue(s.showSuggestion)
    }

    @Test
    fun `응답이 없으면 제안 대신 응답 없음 팝업이다`() {
        val s = started(InquiryScripts.NO_RESPONSE).snapshot(at(21))
        assertNull(s.suggestion)
        assertFalse(s.showSuggestion)
        assertTrue(s.showNoResponsePrompt)
    }
}
