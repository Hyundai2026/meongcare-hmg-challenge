package com.meongcare.app.ui.home

import com.meongcare.app.domain.card.CardSessionState
import com.meongcare.app.domain.card.Symptom
import com.meongcare.app.ui.guidance.GuidanceUiState
import org.junit.Assert.assertEquals
import org.junit.Test

class ParkedContentTest {
    private val idle = GuidanceUiState.Idle("시연용 동물병원")
    private val guiding = GuidanceUiState.Guiding("시연용 동물병원", 12)
    private val findingRoute = GuidanceUiState.Guiding("시연용 동물병원", null)
    private val arrived = GuidanceUiState.Arrived("시연용 동물병원")
    private val allGuidance = listOf(idle, guiding, findingRoute, arrived)

    @Test
    fun `안내도 카드도 없으면 처음 화면이다`() {
        assertEquals(ParkedContent.HOME, parkedContentOf(idle, CardSessionState.None))
    }

    @Test
    fun `고르는 중이면 안내 상태와 상관없이 증상 고르기다`() {
        allGuidance.forEach {
            assertEquals(ParkedContent.SYMPTOMS, parkedContentOf(it, CardSessionState.Picking(setOf(Symptom.COUGH), null)))
            assertEquals(ParkedContent.SYMPTOMS, parkedContentOf(it, CardSessionState.Picking(emptySet(), emptySet())))
        }
    }

    @Test
    fun `카드가 있거나 안내 중이거나 도착했으면 카드다`() {
        allGuidance.forEach {
            assertEquals(ParkedContent.CARD, parkedContentOf(it, CardSessionState.Ready(emptySet())))
        }
        listOf(guiding, findingRoute, arrived).forEach {
            assertEquals(ParkedContent.CARD, parkedContentOf(it, CardSessionState.None))
        }
    }
}
