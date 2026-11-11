package com.meongcare.app.domain.card

import com.meongcare.app.domain.card.Symptom.BREATHING_DIFFICULTY
import com.meongcare.app.domain.card.Symptom.COUGH
import com.meongcare.app.domain.card.Symptom.VOMITING
import org.junit.Assert.assertEquals
import org.junit.Test

class MedicalCardSessionSelectTest {

    private val session = MedicalCardSession()

    @Test
    fun `고르는 중이면 여러 증상을 한 번에 더한다`() {
        session.startPicking()

        session.select(setOf(VOMITING, BREATHING_DIFFICULTY))

        assertEquals(
            CardSessionState.Picking(draft = setOf(VOMITING, BREATHING_DIFFICULTY), previous = null),
            session.state.value,
        )
    }

    @Test
    fun `이미 고른 증상은 풀지 않는다`() {
        session.startPicking()
        session.toggle(COUGH)
        session.toggle(VOMITING)

        session.select(setOf(VOMITING, BREATHING_DIFFICULTY))

        assertEquals(setOf(COUGH, VOMITING, BREATHING_DIFFICULTY), (session.state.value as CardSessionState.Picking).draft)
    }

    @Test
    fun `고르는 중이 아니면 아무것도 하지 않는다`() {
        session.select(setOf(VOMITING))
        assertEquals(CardSessionState.None, session.state.value)

        session.startPicking()
        session.toggle(COUGH)
        session.confirm()
        session.select(setOf(VOMITING))
        assertEquals(CardSessionState.Ready(setOf(COUGH)), session.state.value)
    }

    @Test
    fun `빈 집합을 고르면 바뀌지 않는다`() {
        session.startPicking()
        session.toggle(COUGH)
        val before = session.state.value

        session.select(emptySet())

        assertEquals(before, session.state.value)
    }

    @Test
    fun `카드를 고치는 중이면 고치기 전 카드는 그대로 두고 선택에만 더한다`() {
        session.startPicking()
        session.toggle(COUGH)
        session.confirm()
        session.startPicking()

        session.select(setOf(VOMITING))

        assertEquals(
            CardSessionState.Picking(draft = setOf(COUGH, VOMITING), previous = setOf(COUGH)),
            session.state.value,
        )
    }
}
