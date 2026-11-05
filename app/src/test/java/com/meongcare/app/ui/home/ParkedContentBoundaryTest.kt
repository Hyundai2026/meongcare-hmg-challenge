package com.meongcare.app.ui.home

import com.meongcare.app.domain.card.CardSessionState
import com.meongcare.app.domain.card.Symptom.BREATHING_DIFFICULTY
import com.meongcare.app.domain.card.Symptom.COUGH
import com.meongcare.app.domain.card.Symptom.VOMITING
import com.meongcare.app.ui.guidance.GuidanceUiState
import com.meongcare.app.ui.home.ParkedContent.CARD
import com.meongcare.app.ui.home.ParkedContent.HOME
import com.meongcare.app.ui.home.ParkedContent.SYMPTOMS
import org.junit.Assert.assertEquals
import org.junit.Test

/** 계획 009 설계 결정 1의 2단계 표 (정차 화면 안에서 보일 것)의 모든 칸. */
class ParkedContentBoundaryTest {

    // 안내 상태 (안내 중은 "경로를 찾는 중", 1분, 긴 시간)
    private val idle = GuidanceUiState.Idle("시연용 동물병원")
    private val findingRoute = GuidanceUiState.Guiding("시연용 동물병원", remainingMinutes = null)
    private val guidingOneMinute = GuidanceUiState.Guiding("시연용 동물병원", remainingMinutes = 1)
    private val guidingLong = GuidanceUiState.Guiding("시연용 동물병원", remainingMinutes = 600)
    private val arrived = GuidanceUiState.Arrived("시연용 동물병원")

    // 카드 세션
    private val none = CardSessionState.None
    private val pickingNothing = CardSessionState.Picking(emptySet(), previous = null)
    private val pickingSome = CardSessionState.Picking(setOf(COUGH), previous = null)
    private val editing = CardSessionState.Picking(setOf(COUGH, VOMITING), previous = setOf(COUGH))
    private val editingEmptyCard = CardSessionState.Picking(emptySet(), previous = emptySet())
    private val readyEmpty = CardSessionState.Ready(emptySet())
    private val ready = CardSessionState.Ready(setOf(BREATHING_DIFFICULTY, COUGH))

    /** 표의 "안내 없음" 줄 */
    private val idleRow = mapOf(
        none to HOME,
        pickingNothing to SYMPTOMS,
        pickingSome to SYMPTOMS,
        editing to SYMPTOMS,
        editingEmptyCard to SYMPTOMS,
        readyEmpty to CARD,
        ready to CARD,
    )

    /** 표의 "안내 중" 줄과 "도착" 줄 (세션이 없어도 카드, 증상 줄은 "고른 증상 없음") */
    private val guidingOrArrivedRow = mapOf(
        none to CARD,
        pickingNothing to SYMPTOMS,
        pickingSome to SYMPTOMS,
        editing to SYMPTOMS,
        editingEmptyCard to SYMPTOMS,
        readyEmpty to CARD,
        ready to CARD,
    )

    @Test
    fun `안내가 없으면 세션이 없을 때만 처음 화면이고 고르는 중이면 증상 고르기, 카드가 있으면 카드다`() {
        for ((session, expected) in idleRow) {
            assertEquals("$idle + $session", expected, parkedContentOf(idle, session))
        }
    }

    @Test
    fun `경로를 찾는 중을 포함해 안내 중이면 세션이 없어도 카드이고 고르는 중이면 증상 고르기다`() {
        for (guidance in listOf(findingRoute, guidingOneMinute, guidingLong)) {
            for ((session, expected) in guidingOrArrivedRow) {
                assertEquals("$guidance + $session", expected, parkedContentOf(guidance, session))
            }
        }
    }

    @Test
    fun `도착했으면 세션이 없어도 카드이고 고르는 중이면 증상 고르기다`() {
        for ((session, expected) in guidingOrArrivedRow) {
            assertEquals("$arrived + $session", expected, parkedContentOf(arrived, session))
        }
    }

    @Test
    fun `목적지 이름은 화면 선택에 영향을 주지 않는다`() {
        assertEquals(HOME, parkedContentOf(GuidanceUiState.Idle(""), none))
        assertEquals(CARD, parkedContentOf(GuidanceUiState.Guiding("", null), none))
        assertEquals(CARD, parkedContentOf(GuidanceUiState.Arrived(""), none))
    }
}
