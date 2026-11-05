package com.meongcare.app.di

import org.junit.Assert.assertEquals
import org.junit.Test

class NaviPlanTest {
    @Test
    fun `일반 폰에서는 어느 모드든 내비를 쓰지 않는다`() {
        DataMode.entries.forEach { assertEquals(NaviUse.NONE, naviUseOf(it, isAutomotive = false)) }
    }

    @Test
    fun `차량 기기에서 Real은 전부 내비, 시연은 경로만 내비, Fake는 쓰지 않는다`() {
        assertEquals(NaviUse.FULL, naviUseOf(DataMode.REAL, isAutomotive = true))
        assertEquals(NaviUse.ROUTE_ONLY, naviUseOf(DataMode.DEMO, isAutomotive = true))
        assertEquals(NaviUse.NONE, naviUseOf(DataMode.FAKE, isAutomotive = true))
    }
}
