package com.meongcare.app.ui.pet

import com.meongcare.app.data.pet.SamplePetChooser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 계획 021 "고르기 버튼을 보일지"의 경계. 숨김 조건(두 마리 미만, 샘플 아님, 연결됨, trip 있음)은
 * 하나라도 걸리면 숨기고, 보일 때는 탭이 목록 순서 그대로이며 고른 것은 하나뿐이다.
 */
class PetChoiceBoundaryTest {
    private val wanggeon = PetChoiceItem("P-DEMO-01", "왕건이")
    private val osyong = PetChoiceItem("P-DEMO-02", "오숑이")
    private val third = PetChoiceItem("P-TEST-03", "셋째")

    @Test
    fun `숨김 조건이 하나라도 있으면 숨긴다`() {
        val flags = listOf(false, true)
        for (isSample in flags) for (linked in flags) for (tripActive in flags) {
            val ui = petChoiceOf(listOf(wanggeon, osyong), wanggeon.id, isSample, linked, tripActive)
            val shouldShow = isSample && !linked && !tripActive
            if (shouldShow) {
                assertNotNull("isSample=$isSample linked=$linked trip=$tripActive", ui)
            } else {
                assertNull("isSample=$isSample linked=$linked trip=$tripActive", ui)
            }
        }
    }

    @Test
    fun `한 마리면 샘플이고 연결 안 됐고 trip이 없어도 숨긴다`() {
        assertNull(petChoiceOf(listOf(osyong), osyong.id, isSample = true, linked = false, tripActive = false))
    }

    @Test
    fun `빈 목록이면 고른 id가 무엇이든 숨긴다`() {
        assertNull(petChoiceOf(emptyList(), "", isSample = true, linked = false, tripActive = false))
        assertNull(petChoiceOf(emptyList(), "P-DEMO-01", isSample = true, linked = false, tripActive = false))
    }

    @Test
    fun `세 마리면 다음은 고른 것 바로 뒤이고 끝이면 처음이다`() {
        val pets = listOf(wanggeon, osyong, third)
        fun next(selected: PetChoiceItem) =
            petChoiceOf(pets, selected.id, isSample = true, linked = false, tripActive = false)!!.let { it.nextId to it.nextName }
        assertEquals("P-DEMO-02" to "오숑이", next(wanggeon))
        assertEquals("P-TEST-03" to "셋째", next(osyong))
        assertEquals("P-DEMO-01" to "왕건이", next(third))
    }

    @Test
    fun `탭은 목록 순서와 이름 그대로이고 고른 탭은 하나뿐이다`() {
        val pets = listOf(wanggeon, osyong, third)
        val ui = petChoiceOf(pets, osyong.id, isSample = true, linked = false, tripActive = false)!!
        assertEquals(pets.map { it.id }, ui.tabs.map { it.id })
        assertEquals(pets.map { it.name }, ui.tabs.map { it.name })
        assertEquals(listOf("P-DEMO-02"), ui.tabs.filter { it.selected }.map { it.id })
    }

    @Test
    fun `다음 반려견은 고른 반려견과 다르다`() {
        val pets = listOf(wanggeon, osyong)
        for (selected in pets) {
            val ui = petChoiceOf(pets, selected.id, isSample = true, linked = false, tripActive = false)!!
            assertNotEquals("selected=${selected.id}", selected.id, ui.nextId)
        }
    }

    @Test
    fun `nextId로 고르기를 고르면 프로토타입 cycle과 같은 반려견이 된다`() {
        val chooser = SamplePetChooser()
        val items = chooser.pets.map { PetChoiceItem(it.id, it.profile.name) }
        repeat(3) {
            val ui = petChoiceOf(items, chooser.selected.value.id, isSample = true, linked = false, tripActive = false)!!
            val viaCycle = SamplePetChooser().also { c -> c.select(chooser.selected.value.id); c.selectNext() }
            chooser.select(ui.nextId)
            assertEquals(viaCycle.selected.value, chooser.selected.value)
            assertEquals(chooser.selected.value.profile.name, ui.nextName)
        }
    }
}
