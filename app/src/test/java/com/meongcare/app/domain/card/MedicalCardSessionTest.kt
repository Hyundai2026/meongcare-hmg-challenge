package com.meongcare.app.domain.card

import com.meongcare.app.domain.card.CardSessionState.None
import com.meongcare.app.domain.card.CardSessionState.Picking
import com.meongcare.app.domain.card.CardSessionState.Ready
import com.meongcare.app.domain.card.Symptom.BREATHING_DIFFICULTY
import com.meongcare.app.domain.card.Symptom.COUGH
import com.meongcare.app.domain.card.Symptom.VOMITING
import org.junit.Assert.assertEquals
import org.junit.Test

class MedicalCardSessionTest {

    @Test
    fun `처음에는 카드가 없다`() {
        assertEquals(None, MedicalCardSession().state.value)
    }

    @Test
    fun `카드 없이 고르기를 시작하면 빈 선택이고 취소하면 카드 없음으로 돌아간다`() {
        val session = MedicalCardSession()
        session.startPicking()
        assertEquals(Picking(emptySet(), null), session.state.value)
        session.toggle(COUGH)
        session.cancelPicking()
        assertEquals(None, session.state.value)
    }

    @Test
    fun `카드가 있을 때 수정하면 지금 증상이 선택된 채로 고르고 취소하면 그 카드로 돌아간다`() {
        val session = MedicalCardSession()
        session.startPicking()
        session.toggle(COUGH)
        session.confirm()
        session.startPicking()
        assertEquals(Picking(setOf(COUGH), setOf(COUGH)), session.state.value)
        session.toggle(VOMITING)
        session.cancelPicking()
        assertEquals(Ready(setOf(COUGH)), session.state.value)
    }

    @Test
    fun `같은 증상을 두 번 누르면 선택이 풀린다`() {
        val session = MedicalCardSession()
        session.startPicking()
        session.toggle(COUGH)
        session.toggle(BREATHING_DIFFICULTY)
        session.toggle(COUGH)
        assertEquals(Picking(setOf(BREATHING_DIFFICULTY), null), session.state.value)
    }

    @Test
    fun `고르는 중이 아니면 증상 누르기와 만들기는 아무것도 하지 않는다`() {
        val session = MedicalCardSession()
        session.toggle(COUGH)
        session.confirm()
        session.cancelPicking()
        assertEquals(None, session.state.value)

        session.startPicking()
        session.toggle(COUGH)
        session.confirm()
        session.toggle(VOMITING)
        session.confirm()
        assertEquals(Ready(setOf(COUGH)), session.state.value)
    }

    @Test
    fun `증상 0개로도 카드를 만들 수 있다`() {
        val session = MedicalCardSession()
        session.startPicking()
        session.confirm()
        assertEquals(Ready(emptySet()), session.state.value)
    }

    @Test
    fun `고르는 중에 다시 고르기를 시작해도 선택이 그대로다`() {
        val session = MedicalCardSession()
        session.startPicking()
        session.toggle(COUGH)
        session.startPicking()
        assertEquals(Picking(setOf(COUGH), null), session.state.value)
    }

    @Test
    fun `비우면 어느 상태에서든 카드 없음이다`() {
        val session = MedicalCardSession()
        session.startPicking()
        session.clear()
        assertEquals(None, session.state.value)
        session.startPicking()
        session.confirm()
        session.clear()
        assertEquals(None, session.state.value)
    }

    @Test
    fun `프리셋 순서로 정렬하면 누른 순서와 상관없이 급한 것이 앞이다`() {
        assertEquals(listOf(BREATHING_DIFFICULTY, VOMITING, COUGH), setOf(COUGH, VOMITING, BREATHING_DIFFICULTY).inPresetOrder())
    }
}
