package com.meongcare.app.di

import com.meongcare.app.data.location.FakeCurrentLocationSource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * 계획 016 "설계 1·2" 계약 (tester). 기준: 계획 표와 진행 로그의 U3 "서초동".
 * `AppContainer`는 Context가 필요해 직접 만들지 않고, 같은 두 순수 함수로 속성 규칙을 확인한다.
 */
class FixedDemoLocationContractTest {

    private val cells = DataMode.entries.flatMap { mode -> listOf(true, false).map { mode to it } }

    @Test
    fun `일반 폰·태블릿의 시연과 Real은 서운로 62 고정 위치다`() {
        listOf(DataMode.DEMO, DataMode.REAL).forEach { mode ->
            assertEquals(mode.name, FakeCurrentLocationSource.PHONE_LOCATION, fixedLocationOf(mode, isAutomotive = false))
        }
    }

    @Test
    fun `차량 기기의 시연과 Real은 고정 위치 없이 내비를 쓴다`() {
        listOf(DataMode.DEMO, DataMode.REAL).forEach { mode ->
            assertNull(mode.name, fixedLocationOf(mode, isAutomotive = true))
            assertEquals(mode.name, SourceKind.REAL, currentLocationKindOf(mode, isAutomotive = true))
        }
    }

    @Test
    fun `Fake 모드는 차량 기기든 폰이든 시흥동이다`() {
        listOf(true, false).forEach { auto ->
            assertEquals("auto=$auto", FakeCurrentLocationSource.DEMO_LOCATION, fixedLocationOf(DataMode.FAKE, auto))
        }
    }

    @Test
    fun `고정 위치가 null인 칸과 위치 출처가 내비인 칸은 정확히 같다`() {
        cells.forEach { (mode, auto) ->
            val isNavi = currentLocationKindOf(mode, auto) == SourceKind.REAL
            assertEquals("$mode/$auto", isNavi, fixedLocationOf(mode, auto) == null)
        }
    }

    @Test
    fun `위치 출처가 Fake인 칸에는 언제나 고정 위치가 있다`() {
        cells.filter { (mode, auto) -> currentLocationKindOf(mode, auto) == SourceKind.FAKE }
            .forEach { (mode, auto) -> assertNotNull("$mode/$auto", fixedLocationOf(mode, auto)) }
    }

    @Test
    fun `011 위치 출처 표는 그대로다`() {
        val expected = mapOf(
            (DataMode.FAKE to true) to SourceKind.FAKE,
            (DataMode.FAKE to false) to SourceKind.FAKE,
            (DataMode.DEMO to true) to SourceKind.REAL,
            (DataMode.DEMO to false) to SourceKind.FAKE,
            (DataMode.REAL to true) to SourceKind.REAL,
            (DataMode.REAL to false) to SourceKind.FAKE,
        )
        cells.forEach { (mode, auto) -> assertEquals("$mode/$auto", expected.getValue(mode to auto), currentLocationKindOf(mode, auto)) }
    }

    @Test
    fun `release 빌드를 일반 폰에서 돌리면 저장된 모드와 상관없이 서운로 62다`() {
        listOf(null, "", "FAKE", "DEMO", "REAL", "모름").forEach { saved ->
            val mode = resolveDataMode(isDebugBuild = false, savedName = saved)
            assertEquals("saved=$saved", FakeCurrentLocationSource.PHONE_LOCATION, fixedLocationOf(mode, isAutomotive = false))
        }
    }

    @Test
    fun `release 빌드를 차량 기기에서 돌리면 저장된 모드와 상관없이 내비 위치다`() {
        listOf(null, "", "FAKE", "DEMO", "REAL", "모름").forEach { saved ->
            val mode = resolveDataMode(isDebugBuild = false, savedName = saved)
            assertNull("saved=$saved", fixedLocationOf(mode, isAutomotive = true))
            assertEquals("saved=$saved", SourceKind.REAL, currentLocationKindOf(mode, isAutomotive = true))
        }
    }

    @Test
    fun `debug 빌드의 저장값이 없거나 모르는 값이면 Fake라 폰에서도 시흥동이다`() {
        listOf(null, "", "demo", "모름").forEach { saved ->
            val mode = resolveDataMode(isDebugBuild = true, savedName = saved)
            assertEquals("saved=$saved", FakeCurrentLocationSource.DEMO_LOCATION, fixedLocationOf(mode, isAutomotive = false))
        }
    }

    @Test
    fun `PHONE_LOCATION은 서운로 62 좌표이고 화면 이름은 서초동이다`() {
        val phone = FakeCurrentLocationSource.PHONE_LOCATION
        assertEquals(37.4902566, phone.latitude, 0.0)
        assertEquals(127.0283099, phone.longitude, 0.0)
        assertEquals("서초동", phone.address)
    }

    @Test
    fun `DEMO_LOCATION은 015까지의 시흥동 값 그대로다`() {
        val demo = FakeCurrentLocationSource.DEMO_LOCATION
        assertEquals(37.4127212, demo.latitude, 0.0)
        assertEquals(127.094681, demo.longitude, 0.0)
        assertEquals("수정구 시흥동", demo.address)
    }

    @Test
    fun `폰 고정 위치와 시흥동은 다른 위치다`() {
        assertNotEquals(FakeCurrentLocationSource.DEMO_LOCATION, FakeCurrentLocationSource.PHONE_LOCATION)
        assertNotEquals(FakeCurrentLocationSource.DEMO_LOCATION.address, FakeCurrentLocationSource.PHONE_LOCATION.address)
    }

    @Test
    fun `Fake 위치 소스는 받은 고정 위치를 그대로 돌려준다`() = runTest {
        cells.mapNotNull { (mode, auto) -> fixedLocationOf(mode, auto) }.forEach { fixed ->
            assertSame(fixed, FakeCurrentLocationSource(fixed).current())
        }
    }

    @Test
    fun `Fake 위치 소스의 기본값은 시흥동이다`() = runTest {
        assertEquals(FakeCurrentLocationSource.DEMO_LOCATION, FakeCurrentLocationSource().current())
    }

    @Test
    fun `시연용 위치 이름은 위치 출처가 Fake인 칸에만 있다`() {
        // AppContainer: isLocationSample = locationKind == FAKE, sampleLocationName = fixedLocation?.address
        cells.forEach { (mode, auto) ->
            val isLocationSample = currentLocationKindOf(mode, auto) == SourceKind.FAKE
            val name = fixedLocationOf(mode, auto)?.address
            assertEquals("$mode/$auto", isLocationSample, name != null)
        }
    }

    @Test
    fun `폰 시연과 Real의 시연용 위치 이름은 서초동, Fake는 수정구 시흥동이다`() {
        assertEquals("서초동", fixedLocationOf(DataMode.DEMO, false)?.address)
        assertEquals("서초동", fixedLocationOf(DataMode.REAL, false)?.address)
        assertEquals("수정구 시흥동", fixedLocationOf(DataMode.FAKE, false)?.address)
        assertEquals("수정구 시흥동", fixedLocationOf(DataMode.FAKE, true)?.address)
    }
}
