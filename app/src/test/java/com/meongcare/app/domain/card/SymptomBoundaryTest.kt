package com.meongcare.app.domain.card

import com.meongcare.app.domain.card.Symptom.BLEEDING_OR_WOUND
import com.meongcare.app.domain.card.Symptom.BLOATED_BELLY
import com.meongcare.app.domain.card.Symptom.BREATHING_DIFFICULTY
import com.meongcare.app.domain.card.Symptom.COLLAPSE
import com.meongcare.app.domain.card.Symptom.COUGH
import com.meongcare.app.domain.card.Symptom.DIARRHEA
import com.meongcare.app.domain.card.Symptom.LETHARGY
import com.meongcare.app.domain.card.Symptom.LIMPING
import com.meongcare.app.domain.card.Symptom.NO_APPETITE
import com.meongcare.app.domain.card.Symptom.SEIZURE
import com.meongcare.app.domain.card.Symptom.SWALLOWED_OBJECT
import com.meongcare.app.domain.card.Symptom.VOMITING
import org.junit.Assert.assertEquals
import org.junit.Test

/** 계획 009 설계 결정 5: 증상 프리셋 12개와 순서 (사용자 승인 2026-11-05). */
class SymptomBoundaryTest {

    private val approvedOrder = listOf(
        BREATHING_DIFFICULTY, SEIZURE, COLLAPSE, BLEEDING_OR_WOUND, SWALLOWED_OBJECT,
        VOMITING, DIARRHEA, COUGH, LETHARGY, NO_APPETITE, LIMPING, BLOATED_BELLY,
    )

    @Test
    fun `증상 프리셋은 승인된 12개이고 선언 순서가 표의 순서다`() {
        assertEquals(approvedOrder, Symptom.entries.toList())
    }

    @Test
    fun `12개를 거꾸로 눌러도 프리셋 순서로 정렬된다`() {
        val pressedBackwards = LinkedHashSet(approvedOrder.reversed())
        assertEquals(approvedOrder, pressedBackwards.inPresetOrder())
    }

    @Test
    fun `고른 증상이 없으면 빈 목록이다`() {
        assertEquals(emptyList<Symptom>(), emptySet<Symptom>().inPresetOrder())
    }

    @Test
    fun `하나만 고르면 그 하나다`() {
        assertEquals(listOf(BLOATED_BELLY), setOf(BLOATED_BELLY).inPresetOrder())
    }
}
