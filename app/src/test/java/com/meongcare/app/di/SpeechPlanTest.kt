package com.meongcare.app.di

import org.junit.Assert.assertEquals
import org.junit.Test

class SpeechPlanTest {

    @Test
    fun `구성표는 모든 모드와 기기 조합에서 계획의 표와 같다`() {
        val expected = mapOf(
            (DataMode.FAKE to false) to SpeechOutputKind.FAKE,
            (DataMode.FAKE to true) to SpeechOutputKind.FAKE,
            (DataMode.DEMO to false) to SpeechOutputKind.FAKE,
            (DataMode.DEMO to true) to SpeechOutputKind.REAL,
            (DataMode.REAL to false) to SpeechOutputKind.NONE,
            (DataMode.REAL to true) to SpeechOutputKind.REAL,
        )
        assertEquals(DataMode.entries.size * 2, expected.size)

        expected.forEach { (key, kind) ->
            val (mode, isAutomotive) = key
            assertEquals("$mode, 차량 기기=$isAutomotive", kind, speechOutputKindOf(mode, isAutomotive, enabled = true))
        }
    }

    @Test
    fun `스위치를 끄면 모든 모드와 기기에서 소리 안내가 없다`() {
        DataMode.entries.forEach { mode ->
            listOf(false, true).forEach { isAutomotive ->
                assertEquals(SpeechOutputKind.NONE, speechOutputKindOf(mode, isAutomotive, enabled = false))
            }
        }
    }

    @Test
    fun `release 빌드는 차량 기기에서 Real 소리 안내다`() {
        val releaseMode = resolveDataMode(isDebugBuild = false, savedName = "REAL")

        assertEquals(SpeechOutputKind.REAL, speechOutputKindOf(releaseMode, isAutomotive = true, enabled = true))
        assertEquals(SpeechOutputKind.FAKE, speechOutputKindOf(releaseMode, isAutomotive = false, enabled = true))
    }
}
