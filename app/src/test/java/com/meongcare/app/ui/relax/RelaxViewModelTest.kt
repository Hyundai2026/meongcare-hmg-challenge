package com.meongcare.app.ui.relax

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RelaxViewModelTest {

    @Test
    fun `기본값은 꺼짐, 조용히, 추가 안심 안내 꺼짐이다`() {
        val s = RelaxViewModel().settings.value
        assertEquals(RelaxSettings(enabled = false, sound = RelaxSound.QUIET, extraReassurance = false), s)
        assertTrue(s.isDefault)
    }

    @Test
    fun `켜면 사운드와 안심 안내를 바꿀 수 있다`() {
        val vm = RelaxViewModel()
        vm.setEnabled(true)
        vm.setSound(RelaxSound.CALM_MUSIC)
        vm.setExtraReassurance(true)
        assertEquals(RelaxSettings(true, RelaxSound.CALM_MUSIC, true), vm.settings.value)
        assertFalse(vm.settings.value.isDefault)
    }

    @Test
    fun `꺼진 상태에서는 바꾸지 않는다`() {
        val vm = RelaxViewModel()
        vm.setSound(RelaxSound.CALM_MUSIC)
        vm.setExtraReassurance(true)
        assertEquals(RelaxSettings(), vm.settings.value)
    }

    @Test
    fun `껐다 켜도 설정 값은 남는다`() {
        val vm = RelaxViewModel()
        vm.setEnabled(true)
        vm.setSound(RelaxSound.CALM_MUSIC)
        vm.setEnabled(false)
        assertEquals(RelaxSound.CALM_MUSIC, vm.settings.value.sound)
        vm.setEnabled(true)
        assertEquals(RelaxSettings(true, RelaxSound.CALM_MUSIC, false), vm.settings.value)
    }
}
