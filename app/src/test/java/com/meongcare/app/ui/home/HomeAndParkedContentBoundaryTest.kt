package com.meongcare.app.ui.home

import com.meongcare.app.domain.card.CardSessionState
import com.meongcare.app.domain.card.Symptom
import com.meongcare.app.domain.card.Symptom.COUGH
import com.meongcare.app.ui.guidance.GuidanceUiState
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 화면 조합 두 단계 (계획 009 설계 결정 1): homeContentOf(주행 안전, 바꾸지 않음) → 정차면 parkedContentOf.
 * 주행·모름(HomeUiState.Driving)이면 처음 화면·증상 고르기·카드 어느 것도 보이지 않아야 한다 (AGENTS.md 규칙 3).
 */
class HomeAndParkedContentBoundaryTest {

    private sealed interface Shown {
        data object Driving : Shown
        data class Parked(val content: ParkedContent) : Shown
    }

    private fun shown(home: HomeUiState, guidance: GuidanceUiState, session: CardSessionState): Shown =
        when (homeContentOf(home, guidance)) {
            HomeContent.DRIVING -> Shown.Driving
            HomeContent.PARKED, HomeContent.ARRIVAL -> Shown.Parked(parkedContentOf(guidance, session))
        }

    private val idle = GuidanceUiState.Idle("시연용 동물병원")
    private val findingRoute = GuidanceUiState.Guiding("시연용 동물병원", null)
    private val guiding = GuidanceUiState.Guiding("시연용 동물병원", 12)
    private val arrived = GuidanceUiState.Arrived("시연용 동물병원")
    private val allGuidance = listOf(idle, findingRoute, guiding, arrived)

    private val allSessions = listOf(
        CardSessionState.None,
        CardSessionState.Picking(emptySet(), null),
        CardSessionState.Picking(setOf(COUGH), null),
        CardSessionState.Picking(setOf(COUGH), setOf(COUGH)),
        CardSessionState.Picking(emptySet(), emptySet()),
        CardSessionState.Ready(emptySet()),
        CardSessionState.Ready(Symptom.entries.toSet()),
    )

    @Test
    fun `주행이나 모름이면 안내와 카드 세션이 무엇이든 처음 화면도 증상 고르기도 카드도 보이지 않는다`() {
        for (guidance in allGuidance) {
            for (session in allSessions) {
                assertEquals("$guidance + $session", Shown.Driving, shown(HomeUiState.Driving, guidance, session))
            }
        }
    }

    @Test
    fun `정차 중이면 도착을 포함한 모든 안내 상태에서 두 단계 표대로 보인다`() {
        for (guidance in allGuidance) {
            for (session in allSessions) {
                val expected = when (session) {
                    is CardSessionState.Picking -> ParkedContent.SYMPTOMS
                    is CardSessionState.Ready -> ParkedContent.CARD
                    CardSessionState.None -> if (guidance == idle) ParkedContent.HOME else ParkedContent.CARD
                }
                assertEquals("$guidance + $session", Shown.Parked(expected), shown(HomeUiState.Parked, guidance, session))
            }
        }
    }

    @Test
    fun `정차 중 도착이면 도착 화면 자리에 세션 없이도 카드가 보인다`() {
        assertEquals(HomeContent.ARRIVAL, homeContentOf(HomeUiState.Parked, arrived))
        assertEquals(Shown.Parked(ParkedContent.CARD), shown(HomeUiState.Parked, arrived, CardSessionState.None))
    }
}
