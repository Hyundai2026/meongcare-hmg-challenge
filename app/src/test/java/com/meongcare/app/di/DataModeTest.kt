package com.meongcare.app.di

import org.junit.Assert.assertEquals
import org.junit.Test

class DataModeTest {

    @Test
    fun `debug 빌드에서 저장된 값이 없으면 Fake다`() {
        assertEquals(DataMode.FAKE, resolveDataMode(isDebugBuild = true, savedName = null))
    }

    @Test
    fun `debug 빌드는 저장된 모드를 쓴다`() {
        assertEquals(DataMode.REAL, resolveDataMode(isDebugBuild = true, savedName = "REAL"))
        assertEquals(DataMode.FAKE, resolveDataMode(isDebugBuild = true, savedName = "FAKE"))
    }

    @Test
    fun `debug 빌드에서 모르는 값이 저장돼 있으면 Fake다`() {
        assertEquals(DataMode.FAKE, resolveDataMode(isDebugBuild = true, savedName = "UNKNOWN_MODE"))
    }

    @Test
    fun `debug 빌드는 저장된 시연 모드를 쓴다`() {
        assertEquals(DataMode.DEMO, resolveDataMode(isDebugBuild = true, savedName = "DEMO"))
    }

    @Test
    fun `모드 전환은 Fake, 시연, Real 순서로 돈다`() {
        assertEquals(DataMode.DEMO, DataMode.FAKE.next())
        assertEquals(DataMode.REAL, DataMode.DEMO.next())
        assertEquals(DataMode.FAKE, DataMode.REAL.next())
    }

    @Test
    fun `release 빌드는 저장된 값과 상관없이 Real이다`() {
        assertEquals(DataMode.REAL, resolveDataMode(isDebugBuild = false, savedName = null))
        assertEquals(DataMode.REAL, resolveDataMode(isDebugBuild = false, savedName = "FAKE"))
    }
}
