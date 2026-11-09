package com.meongcare.app.di

import com.meongcare.app.data.location.FakeCurrentLocationSource
import org.junit.Assert.assertEquals
import org.junit.Test

/** 계획 016 "설계 1": `fixedLocationOf`의 표 6칸과 `currentLocationKindOf`와의 규칙. */
class FixedLocationPlanTest {

    @Test
    fun `표 6칸이 같다`() {
        val expected = mapOf(
            (DataMode.FAKE to true) to FakeCurrentLocationSource.DEMO_LOCATION,
            (DataMode.FAKE to false) to FakeCurrentLocationSource.DEMO_LOCATION,
            (DataMode.DEMO to true) to null,
            (DataMode.DEMO to false) to FakeCurrentLocationSource.PHONE_LOCATION,
            (DataMode.REAL to true) to null,
            (DataMode.REAL to false) to FakeCurrentLocationSource.PHONE_LOCATION,
        )
        DataMode.entries.forEach { mode ->
            listOf(true, false).forEach { auto ->
                assertEquals("$mode/$auto", expected.getValue(mode to auto), fixedLocationOf(mode, auto))
            }
        }
    }

    @Test
    fun `고정 위치가 없는 칸은 내비 칸과 같다`() {
        DataMode.entries.forEach { mode ->
            listOf(true, false).forEach { auto ->
                assertEquals("$mode/$auto", currentLocationKindOf(mode, auto) == SourceKind.REAL, fixedLocationOf(mode, auto) == null)
            }
        }
    }

    @Test
    fun `폰 고정 위치는 서운로 62이고 화면 이름은 서초동이다`() {
        val phone = FakeCurrentLocationSource.PHONE_LOCATION
        assertEquals(37.4902566, phone.latitude, 0.0)
        assertEquals(127.0283099, phone.longitude, 0.0)
        assertEquals("서초동", phone.address)
    }
}
