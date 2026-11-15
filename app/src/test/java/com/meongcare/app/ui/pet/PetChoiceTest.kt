package com.meongcare.app.ui.pet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** 계획 021 "고르기 버튼을 보일지" */
class PetChoiceTest {
    private val wanggeon = PetChoiceItem("P-DEMO-01", "왕건이")
    private val osyong = PetChoiceItem("P-DEMO-02", "오숑이")
    private val pets = listOf(wanggeon, osyong)

    private fun choice(
        pets: List<PetChoiceItem> = this.pets,
        selectedId: String = wanggeon.id,
        isSample: Boolean = true,
        linked: Boolean = false,
        tripActive: Boolean = false,
    ) = petChoiceOf(pets, selectedId, isSample, linked, tripActive)

    @Test
    fun `두 마리 샘플이고 연결 안 됐고 trip이 없으면 보인다`() {
        val ui = choice()!!
        assertEquals(
            listOf(PetChoiceTab("P-DEMO-01", "왕건이", true), PetChoiceTab("P-DEMO-02", "오숑이", false)),
            ui.tabs,
        )
        assertEquals("P-DEMO-02", ui.nextId)
        assertEquals("오숑이", ui.nextName)
    }

    @Test
    fun `마지막을 골랐으면 다음은 처음이다`() {
        val ui = choice(selectedId = osyong.id)!!
        assertEquals("P-DEMO-01", ui.nextId)
        assertEquals(listOf(false, true), ui.tabs.map { it.selected })
    }

    @Test
    fun `한 마리면 숨긴다`() {
        assertNull(choice(pets = listOf(wanggeon)))
        assertNull(choice(pets = emptyList()))
    }

    @Test
    fun `샘플이 아니면 숨긴다`() {
        assertNull(choice(isSample = false))
    }

    @Test
    fun `연결된 동안 숨긴다`() {
        assertNull(choice(linked = true))
    }

    @Test
    fun `trip이 있으면 숨긴다`() {
        assertNull(choice(tripActive = true))
    }

    @Test
    fun `고른 id가 목록에 없으면 아무것도 고르지 않은 탭과 첫째를 다음으로 낸다`() {
        val ui = choice(selectedId = "P-DEMO-99")!!
        assertEquals(listOf(false, false), ui.tabs.map { it.selected })
        assertEquals("P-DEMO-01", ui.nextId)
    }
}
