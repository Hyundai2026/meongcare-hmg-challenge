package com.meongcare.app.domain.card

import app.cash.turbine.test
import com.meongcare.app.domain.card.CardSessionState.None
import com.meongcare.app.domain.card.CardSessionState.Picking
import com.meongcare.app.domain.card.CardSessionState.Ready
import com.meongcare.app.domain.card.Symptom.COUGH
import com.meongcare.app.domain.card.Symptom.VOMITING
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 계획 009 설계 결정 2의 세션 전이 경계.
 * - startPicking: None → Picking(∅, null), Ready(s) → Picking(s, s), Picking → 그대로
 * - toggle: Picking일 때만 / confirm: Picking(d, _) → Ready(d), 0개도 된다
 * - cancelPicking: Picking(_, null) → None, Picking(_, p) → Ready(p) / clear: → None
 */
class MedicalCardSessionBoundaryTest {

    private fun MedicalCardSession.makeCard(vararg symptoms: Symptom) {
        startPicking()
        symptoms.forEach(::toggle)
        confirm()
    }

    @Test
    fun `증상 0개 카드를 수정하다 취소하면 카드 없음이 아니라 증상 0개 카드로 돌아간다`() {
        val session = MedicalCardSession()
        session.makeCard()
        session.startPicking()
        assertEquals(Picking(emptySet(), emptySet()), session.state.value)
        session.toggle(COUGH)
        session.cancelPicking()
        assertEquals(Ready(emptySet()), session.state.value)
    }

    @Test
    fun `수정 중에 모든 선택을 풀고 만들면 증상 0개 카드가 된다`() {
        val session = MedicalCardSession()
        session.makeCard(COUGH)
        session.startPicking()
        session.toggle(COUGH)
        session.confirm()
        assertEquals(Ready(emptySet()), session.state.value)
    }

    @Test
    fun `수정해서 만들면 새 카드가 되고 다음 수정을 취소하면 새 카드로 돌아간다`() {
        val session = MedicalCardSession()
        session.makeCard(COUGH)
        session.startPicking()
        session.toggle(VOMITING)
        session.confirm()
        assertEquals(Ready(setOf(COUGH, VOMITING)), session.state.value)

        session.startPicking()
        assertEquals(Picking(setOf(COUGH, VOMITING), setOf(COUGH, VOMITING)), session.state.value)
        session.toggle(COUGH)
        session.cancelPicking()
        assertEquals(Ready(setOf(COUGH, VOMITING)), session.state.value)
    }

    @Test
    fun `수정 중에 다시 고르기를 시작해도 선택과 돌아갈 카드가 바뀌지 않는다`() {
        val session = MedicalCardSession()
        session.makeCard(COUGH)
        session.startPicking()
        session.toggle(VOMITING)
        session.startPicking()
        assertEquals(Picking(setOf(COUGH, VOMITING), setOf(COUGH)), session.state.value)
        session.cancelPicking()
        assertEquals(Ready(setOf(COUGH)), session.state.value)
    }

    @Test
    fun `취소한 선택은 다음에 고를 때 남지 않는다`() {
        val session = MedicalCardSession()
        session.startPicking()
        session.toggle(COUGH)
        session.cancelPicking()
        session.startPicking()
        assertEquals(Picking(emptySet(), null), session.state.value)

        session.cancelPicking()
        session.makeCard(COUGH)
        session.startPicking()
        session.toggle(VOMITING)
        session.cancelPicking()
        session.startPicking()
        assertEquals(Picking(setOf(COUGH), setOf(COUGH)), session.state.value)
    }

    @Test
    fun `12개 증상을 모두 골라 카드를 만들 수 있다`() {
        val session = MedicalCardSession()
        session.makeCard(*Symptom.entries.toTypedArray())
        assertEquals(Ready(Symptom.entries.toSet()), session.state.value)
    }

    @Test
    fun `카드가 있을 때 증상 누르기와 취소는 카드를 바꾸지 않는다`() {
        val session = MedicalCardSession()
        session.makeCard(COUGH)
        session.toggle(VOMITING)
        session.cancelPicking()
        assertEquals(Ready(setOf(COUGH)), session.state.value)
    }

    @Test
    fun `수정 중에 비우면 돌아갈 카드도 버리고 카드 없음이다`() {
        val session = MedicalCardSession()
        session.makeCard(COUGH)
        session.startPicking()
        session.clear()
        assertEquals(None, session.state.value)
        session.clear()
        assertEquals(None, session.state.value)
    }

    @Test
    fun `상태 흐름은 처음에 카드 없음을 내보내고 바뀔 때마다 새 상태를 내보낸다`() = runTest {
        val session = MedicalCardSession()
        session.state.test {
            assertEquals(None, awaitItem())
            session.startPicking()
            assertEquals(Picking(emptySet(), null), awaitItem())
            session.toggle(COUGH)
            assertEquals(Picking(setOf(COUGH), null), awaitItem())
            session.confirm()
            assertEquals(Ready(setOf(COUGH)), awaitItem())
            session.clear()
            assertEquals(None, awaitItem())
        }
    }

    @Test
    fun `아무것도 바뀌지 않는 동작은 새 값을 내보내지 않는다`() = runTest {
        val session = MedicalCardSession()
        session.state.test {
            assertEquals(None, awaitItem())
            session.toggle(COUGH)
            session.confirm()
            session.cancelPicking()
            session.clear()
            expectNoEvents()

            session.startPicking()
            assertEquals(Picking(emptySet(), null), awaitItem())
            session.startPicking()
            expectNoEvents()
        }
    }
}
