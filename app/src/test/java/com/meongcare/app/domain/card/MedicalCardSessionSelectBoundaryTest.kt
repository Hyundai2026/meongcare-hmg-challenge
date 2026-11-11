package com.meongcare.app.domain.card

import com.meongcare.app.domain.card.Symptom.BREATHING_DIFFICULTY
import com.meongcare.app.domain.card.Symptom.COUGH
import com.meongcare.app.domain.card.Symptom.VOMITING
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `MedicalCardSession.select` 경계 테스트. 기준: docs/plans/active/010-voice-input.md "설계 결정 1"
 * (음성 결과는 선택에 더하기만 한다, 잘못 골라졌으면 사용자가 버튼을 눌러 푼다, [카드 만들기]는 지금 선택으로)과
 * "설계 결정 9"(`select` KDoc: 고르는 중일 때만 더한다, 이미 고른 것은 풀지 않는다).
 */
class MedicalCardSessionSelectBoundaryTest {

    private val session = MedicalCardSession()
    private val draft get() = (session.state.value as CardSessionState.Picking).draft

    @Test
    fun `음성으로 더한 증상은 카드를 만들면 버튼으로 고른 것과 함께 카드에 들어간다`() {
        session.startPicking()
        session.toggle(COUGH)
        session.select(setOf(VOMITING, BREATHING_DIFFICULTY))

        session.confirm()

        assertEquals(CardSessionState.Ready(setOf(COUGH, VOMITING, BREATHING_DIFFICULTY)), session.state.value)
    }

    @Test
    fun `음성으로 더한 증상도 버튼을 눌러 풀 수 있다`() {
        session.startPicking()
        session.select(setOf(VOMITING, BREATHING_DIFFICULTY))

        session.toggle(VOMITING)

        assertEquals(setOf(BREATHING_DIFFICULTY), draft)
    }

    @Test
    fun `버튼으로 푼 증상을 다시 말하면 다시 더해진다`() {
        session.startPicking()
        session.select(setOf(VOMITING))
        session.toggle(VOMITING)

        session.select(setOf(VOMITING))

        assertEquals(setOf(VOMITING), draft)
    }

    @Test
    fun `카드가 없던 고르기를 취소하면 음성으로 더한 증상도 남지 않는다`() {
        session.startPicking()
        session.select(setOf(VOMITING, COUGH))

        session.cancelPicking()
        assertEquals(CardSessionState.None, session.state.value)

        session.startPicking()
        assertEquals(emptySet<Symptom>(), draft)
    }

    @Test
    fun `모든 증상을 한 번에 더할 수 있다`() {
        session.startPicking()

        session.select(Symptom.entries.toSet())

        assertEquals(Symptom.entries.toSet(), draft)
    }

    @Test
    fun `카드를 둔 채 홈에 있을 때 고르면 카드도 홈 여부도 바뀌지 않는다`() {
        session.startPicking()
        session.toggle(COUGH)
        session.confirm()
        session.goHome()

        session.select(setOf(VOMITING))

        assertEquals(CardSessionState.Ready(setOf(COUGH)), session.state.value)
        assertTrue(session.atHome.value)
    }
}
