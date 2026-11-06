package com.meongcare.app.di

import org.junit.Assert.assertEquals
import org.junit.Test

/** 계획 012 "설계 결정 4"의 표 */
class HospitalPlanTest {

    @Test
    fun `Fake는 서버 설정과 상관없이 내장 병원이다`() {
        assertEquals(HospitalSourceKind.BUILT_IN, hospitalSourceKindOf(DataMode.FAKE, hasServer = true))
        assertEquals(HospitalSourceKind.BUILT_IN, hospitalSourceKindOf(DataMode.FAKE, hasServer = false))
    }

    @Test
    fun `시연은 서버가 있으면 서버이고 실패하면 내장으로 대체한다`() {
        assertEquals(HospitalSourceKind.SERVER_WITH_FALLBACK, hospitalSourceKindOf(DataMode.DEMO, hasServer = true))
    }

    @Test
    fun `시연에서 서버 설정이 없으면 내장이다`() {
        assertEquals(HospitalSourceKind.BUILT_IN, hospitalSourceKindOf(DataMode.DEMO, hasServer = false))
    }

    @Test
    fun `Real은 서버만 쓰고 대체하지 않는다`() {
        assertEquals(HospitalSourceKind.SERVER_ONLY, hospitalSourceKindOf(DataMode.REAL, hasServer = true))
        assertEquals(HospitalSourceKind.SERVER_ONLY, hospitalSourceKindOf(DataMode.REAL, hasServer = false))
    }

    @Test
    fun `release는 시연 표를 따른다`() {
        val release = resolveDataMode(isDebugBuild = false, savedName = "REAL")
        assertEquals(HospitalSourceKind.BUILT_IN, hospitalSourceKindOf(release, hasServer = false))
        assertEquals(HospitalSourceKind.SERVER_WITH_FALLBACK, hospitalSourceKindOf(release, hasServer = true))
    }
}
