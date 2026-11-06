package com.meongcare.app.di

import com.meongcare.app.data.server.serverConfigOf
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 병원 출처 표 경계. 기준: 계획 012 "설계 결정 4"(모드 × 서버 설정 표, release는 네트워크 없이도 끝까지 돈다 = 문의 A2),
 * "설계 결정 3"(release는 에뮬레이터 주소를 무시, 키가 없으면 서버 없음).
 * BuildConfig 값 → [serverConfigOf] → [hospitalSourceKindOf]를 이어서 본다 (AppContainer가 하는 순서).
 */
class HospitalPlanBoundaryTest {

    private fun kindFor(isDebugBuild: Boolean, savedMode: String?, projectId: String, apiKey: String, emulatorHost: String): HospitalSourceKind {
        val mode = resolveDataMode(isDebugBuild, savedMode)
        val hasServer = serverConfigOf(projectId, apiKey, emulatorHost, isDebugBuild) != null
        return hospitalSourceKindOf(mode, hasServer)
    }

    @Test
    fun `모든 모드와 서버 설정 조합이 표와 같다`() {
        val expected = mapOf(
            (DataMode.FAKE to false) to HospitalSourceKind.BUILT_IN,
            (DataMode.FAKE to true) to HospitalSourceKind.BUILT_IN,
            (DataMode.DEMO to false) to HospitalSourceKind.BUILT_IN,
            (DataMode.DEMO to true) to HospitalSourceKind.SERVER_WITH_FALLBACK,
            (DataMode.REAL to false) to HospitalSourceKind.SERVER_ONLY,
            (DataMode.REAL to true) to HospitalSourceKind.SERVER_ONLY,
        )
        DataMode.entries.forEach { mode ->
            listOf(false, true).forEach { hasServer ->
                assertEquals("$mode/$hasServer", expected.getValue(mode to hasServer), hospitalSourceKindOf(mode, hasServer))
            }
        }
    }

    @Test
    fun `release 빌드는 Firebase 설정이 없으면 저장된 모드와 상관없이 내장 병원으로 돈다`() {
        listOf(null, "FAKE", "DEMO", "REAL", "모름").forEach { saved ->
            assertEquals("$saved", HospitalSourceKind.BUILT_IN, kindFor(false, saved, projectId = "", apiKey = "", emulatorHost = ""))
        }
    }

    @Test
    fun `release 빌드에 에뮬레이터 주소만 있고 API 키가 없으면 내장 병원이다`() {
        assertEquals(
            HospitalSourceKind.BUILT_IN,
            kindFor(false, "REAL", projectId = "demo-meongcare", apiKey = "", emulatorHost = "10.0.2.2"),
        )
    }

    @Test
    fun `release 빌드에 진짜 서버 설정이 있으면 서버이고 실패하면 내장으로 대체한다`() {
        assertEquals(
            HospitalSourceKind.SERVER_WITH_FALLBACK,
            kindFor(false, null, projectId = "meongcare", apiKey = "AIza-key", emulatorHost = "10.0.2.2"),
        )
    }

    @Test
    fun `debug 시연은 에뮬레이터 주소만 있어도 서버에 묻는다`() {
        assertEquals(
            HospitalSourceKind.SERVER_WITH_FALLBACK,
            kindFor(true, "DEMO", projectId = "demo-meongcare", apiKey = "", emulatorHost = "10.0.2.2"),
        )
    }

    @Test
    fun `debug의 저장된 모드가 없거나 모르는 값이면 Fake라 내장 병원이다`() {
        listOf(null, "", "real").forEach { saved ->
            assertEquals("$saved", HospitalSourceKind.BUILT_IN, kindFor(true, saved, projectId = "demo-meongcare", apiKey = "k", emulatorHost = "10.0.2.2"))
        }
    }
}
