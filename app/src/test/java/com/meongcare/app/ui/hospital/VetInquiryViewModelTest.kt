package com.meongcare.app.ui.hospital

import com.meongcare.app.domain.inquiry.InquiryScripts
import com.meongcare.app.domain.inquiry.InquiryStatus
import com.meongcare.app.testutil.MainDispatcherRule
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class VetInquiryViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private var now = Instant.parse("2026-11-13T06:00:00Z")
    private fun vm() = VetInquiryViewModel(clock = { now })

    @Test
    fun `다시 시작하면 처음 3곳에 새로 문의하고 쪽과 선택이 처음으로 돌아간다`() {
        val vm = vm()
        vm.restart()
        now = now.plusSeconds(10)
        vm.openCompare()
        assertEquals(InquiryPage.COMPARE, vm.page.value)
        assertEquals("온유동물병원", vm.compareSelected.value)
        vm.restart()
        assertEquals(InquiryPage.SEARCH, vm.page.value)
        assertNull(vm.compareSelected.value)
        assertTrue(vm.current().searchRows.all { it.status == InquiryStatus.WAITING })
        assertEquals(3, vm.current().searchRows.size)
    }

    @Test
    fun `다른 병원 문의는 더한 수를 한 번 알리고 다시 누르면 알리지 않는다`() {
        val vm = vm()
        vm.restart()
        vm.expand()
        assertEquals(2, vm.expandNotice.value)
        vm.consumeExpandNotice()
        vm.expand()
        assertNull(vm.expandNotice.value)
        assertEquals(5, vm.current().searchRows.size)
    }

    @Test
    fun `시나리오는 다음 문의부터 쓴다`() {
        val vm = vm()
        vm.restart()
        vm.script = InquiryScripts.NO_RESPONSE
        now = now.plusSeconds(21)
        assertEquals(false, vm.current().showNoResponsePrompt)
        vm.restart()
        now = now.plusSeconds(21)
        assertEquals(true, vm.current().showNoResponsePrompt)
        vm.dismissNoResponse()
        assertEquals(false, vm.current().showNoResponsePrompt)
    }

    @Test
    fun `출발 확인에서 뒤로 오면 비교 쪽이고 고른 것이 없으면 가장 빠른 곳`() {
        val vm = vm()
        vm.restart()
        now = now.plusSeconds(10)
        vm.onBackFromConfirm()
        assertEquals(InquiryPage.COMPARE, vm.page.value)
        assertEquals("온유동물병원", vm.compareSelected.value)
        vm.selectInCompare("별숲동물의료센터")
        vm.backToSearch()
        vm.onBackFromConfirm()
        assertEquals("별숲동물의료센터", vm.compareSelected.value)
    }
}
