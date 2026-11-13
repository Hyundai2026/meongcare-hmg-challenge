package com.meongcare.app.ui.relax

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 계획 019 단계 7 `RelaxViewModel` 경계 (D5: 메모리에만, 꺼져 있으면 바꾸지 않음). */
class RelaxViewModelBoundaryTest {

    @Test
    fun `켜기만 해도 기본값이 아니다`() {
        val vm = RelaxViewModel()
        vm.setEnabled(true)
        assertFalse(vm.settings.value.isDefault)
        vm.setEnabled(false)
        assertTrue(vm.settings.value.isDefault)
    }

    @Test
    fun `껐다 켜도 추가 안심 안내 값이 남는다`() {
        val vm = RelaxViewModel()
        vm.setEnabled(true)
        vm.setExtraReassurance(true)
        vm.setEnabled(false)
        assertTrue(vm.settings.value.extraReassurance)
        vm.setEnabled(true)
        assertEquals(RelaxSettings(true, RelaxSound.QUIET, true), vm.settings.value)
    }

    @Test
    fun `꺼진 상태에서는 저장된 값을 되돌리는 것도 무시한다`() {
        val vm = RelaxViewModel()
        vm.setEnabled(true)
        vm.setSound(RelaxSound.CALM_MUSIC)
        vm.setExtraReassurance(true)
        vm.setEnabled(false)
        vm.setSound(RelaxSound.QUIET)
        vm.setExtraReassurance(false)
        assertEquals(RelaxSettings(false, RelaxSound.CALM_MUSIC, true), vm.settings.value)
    }

    @Test
    fun `새 ViewModel은 이전 ViewModel의 값을 이어받지 않는다`() {
        val first = RelaxViewModel()
        first.setEnabled(true)
        first.setSound(RelaxSound.CALM_MUSIC)
        assertEquals(RelaxSettings(), RelaxViewModel().settings.value)
    }

    @Test
    fun `첫 값은 기본값이고 바뀔 때마다 한 번씩, 같은 값이면 나오지 않는다`() = runTest {
        val vm = RelaxViewModel()
        vm.settings.test {
            assertEquals(RelaxSettings(), awaitItem())
            vm.setEnabled(true)
            assertEquals(RelaxSettings(enabled = true), awaitItem())
            vm.setEnabled(true)
            vm.setSound(RelaxSound.QUIET)
            expectNoEvents()
            vm.setSound(RelaxSound.CALM_MUSIC)
            assertEquals(RelaxSettings(true, RelaxSound.CALM_MUSIC, false), awaitItem())
            vm.setEnabled(false)
            assertEquals(RelaxSettings(false, RelaxSound.CALM_MUSIC, false), awaitItem())
            vm.setExtraReassurance(true)
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }
}
