package com.meongcare.app.ui.hospital

import org.junit.Assert.assertEquals
import org.junit.Test

/** 계획 015 "설계 변경 V2": 5개씩 쪽. */
class HospitalPagingTest {

    @Test
    fun `5개씩 나눈 쪽 수이고 0개여도 1쪽이다`() {
        assertEquals(5, PAGE_SIZE)
        assertEquals(1, pageCountOf(0))
        assertEquals(1, pageCountOf(1))
        assertEquals(1, pageCountOf(5))
        assertEquals(2, pageCountOf(6))
        assertEquals(4, pageCountOf(20))
        assertEquals(4, pageCountOf(16))
    }
}
