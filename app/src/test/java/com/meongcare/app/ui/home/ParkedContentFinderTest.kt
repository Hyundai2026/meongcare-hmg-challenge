package com.meongcare.app.ui.home

import com.meongcare.app.domain.card.CardSessionState
import com.meongcare.app.domain.card.Symptom
import com.meongcare.app.domain.trip.FinderStep
import com.meongcare.app.ui.guidance.GuidanceUiState
import com.meongcare.app.ui.hospital.previewRanking
import org.junit.Assert.assertEquals
import org.junit.Test

/** 병원 찾기 단계 (012). 기준: 계획 012 "설계 결정 8", `parkedContentOf` 표. */
class ParkedContentFinderTest {

    private val idle = GuidanceUiState.Idle("시연용 동물병원")
    private val guiding = GuidanceUiState.Guiding("시연용 바른 동물병원", 12)
    private val ready = CardSessionState.Ready(setOf(Symptom.COUGH))
    private val confirm = FinderStep.Confirm(previewRanking().ranking.ranked.first())

    @Test
    fun `카드가 있고 안내가 없으면 찾기 단계대로 목록과 확인을 보인다`() {
        assertEquals(ParkedContent.HOSPITALS, parkedContentOf(idle, ready, finder = FinderStep.Hospitals))
        assertEquals(ParkedContent.CONFIRM_HOSPITAL, parkedContentOf(idle, ready, finder = confirm))
        assertEquals(ParkedContent.CARD, parkedContentOf(idle, ready, finder = FinderStep.None))
    }

    @Test
    fun `안내가 시작되면 찾기 단계가 남아 있어도 카드다`() {
        assertEquals(ParkedContent.CARD, parkedContentOf(guiding, ready, finder = FinderStep.Hospitals))
        assertEquals(ParkedContent.CARD, parkedContentOf(guiding, ready, finder = confirm))
    }

    @Test
    fun `증상을 고르는 중이나 홈이면 찾기 단계보다 앞선다`() {
        val picking = CardSessionState.Picking(setOf(Symptom.COUGH), previous = setOf(Symptom.COUGH))
        assertEquals(ParkedContent.SYMPTOMS, parkedContentOf(idle, picking, finder = FinderStep.Hospitals))
        assertEquals(ParkedContent.HOME, parkedContentOf(idle, ready, atHome = true, finder = FinderStep.Hospitals))
    }

    @Test
    fun `카드가 없으면 찾기 단계여도 처음 화면이다`() {
        assertEquals(ParkedContent.HOME, parkedContentOf(idle, CardSessionState.None, finder = FinderStep.Hospitals))
    }
}
