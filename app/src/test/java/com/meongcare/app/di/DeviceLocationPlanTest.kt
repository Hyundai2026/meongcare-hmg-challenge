package com.meongcare.app.di

import org.junit.Assert.assertEquals
import org.junit.Test

/** 계획 015: 내비 없는 기기의 시연·Real은 기기 위치를 쓴다. */
class DeviceLocationPlanTest {

    @Test
    fun `표 6칸이 같다`() {
        val expected = mapOf(
            (DataMode.FAKE to true) to false,
            (DataMode.FAKE to false) to false,
            (DataMode.DEMO to true) to false,
            (DataMode.DEMO to false) to true,
            (DataMode.REAL to true) to false,
            (DataMode.REAL to false) to true,
        )
        DataMode.entries.forEach { mode ->
            listOf(true, false).forEach { auto ->
                assertEquals("$mode/$auto", expected.getValue(mode to auto), usesDeviceLocation(mode, auto))
            }
        }
    }

    @Test
    fun `기기 위치는 내비를 쓰지 않는 칸에서만 고른다`() {
        DataMode.entries.forEach { mode ->
            listOf(true, false).forEach { auto ->
                if (usesDeviceLocation(mode, auto)) assertEquals(SourceKind.FAKE, currentLocationKindOf(mode, auto))
            }
        }
    }
}
