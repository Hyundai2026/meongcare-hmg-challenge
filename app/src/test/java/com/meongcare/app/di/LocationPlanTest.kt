package com.meongcare.app.di

import org.junit.Assert.assertEquals
import org.junit.Test

class LocationPlanTest {
    @Test
    fun `차량 기기의 시연과 Real은 내비 위치를 쓴다`() {
        assertEquals(SourceKind.REAL, currentLocationKindOf(DataMode.DEMO, isAutomotive = true))
        assertEquals(SourceKind.REAL, currentLocationKindOf(DataMode.REAL, isAutomotive = true))
    }

    @Test
    fun `Fake 모드는 차량 기기에서도 Fake 위치를 쓴다`() {
        assertEquals(SourceKind.FAKE, currentLocationKindOf(DataMode.FAKE, isAutomotive = true))
    }

    @Test
    fun `일반 폰은 어느 모드든 Fake 위치를 쓴다`() {
        DataMode.entries.forEach { assertEquals(SourceKind.FAKE, currentLocationKindOf(it, isAutomotive = false)) }
    }

    @Test
    fun `release는 차량 기기에서 내비 위치, 일반 폰에서 Fake 위치다`() {
        val release = resolveDataMode(isDebugBuild = false, savedName = DataMode.FAKE.name)
        assertEquals(SourceKind.REAL, currentLocationKindOf(release, isAutomotive = true))
        assertEquals(SourceKind.FAKE, currentLocationKindOf(release, isAutomotive = false))
    }
}
