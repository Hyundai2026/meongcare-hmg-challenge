package com.meongcare.app.di

import org.junit.Assert.assertEquals
import org.junit.Test

class VoicePlanTest {

    @Test
    fun `구성표는 모든 모드와 기기 조합에서 계획의 표와 같다`() {
        val expected = mapOf(
            (DataMode.FAKE to false) to VoiceInputKind.FAKE,
            (DataMode.FAKE to true) to VoiceInputKind.FAKE,
            (DataMode.DEMO to false) to VoiceInputKind.FAKE,
            (DataMode.DEMO to true) to VoiceInputKind.REAL,
            (DataMode.REAL to false) to VoiceInputKind.NONE,
            (DataMode.REAL to true) to VoiceInputKind.REAL,
        )
        assertEquals(DataMode.entries.size * 2, expected.size)

        expected.forEach { (key, kind) ->
            val (mode, isAutomotive) = key
            assertEquals("$mode, 차량 기기=$isAutomotive", kind, voiceInputKindOf(mode, isAutomotive, enabled = true))
        }
    }

    @Test
    fun `스위치를 끄면 모든 모드와 기기에서 음성 입력이 없다`() {
        DataMode.entries.forEach { mode ->
            listOf(false, true).forEach { isAutomotive ->
                assertEquals(VoiceInputKind.NONE, voiceInputKindOf(mode, isAutomotive, enabled = false))
            }
        }
    }

    @Test
    fun `release 빌드는 차량 기기에서 Real 음성이고 일반 폰에서 Fake 음성이다`() {
        val releaseMode = resolveDataMode(isDebugBuild = false, savedName = "REAL")

        assertEquals(VoiceInputKind.REAL, voiceInputKindOf(releaseMode, isAutomotive = true, enabled = true))
        assertEquals(VoiceInputKind.FAKE, voiceInputKindOf(releaseMode, isAutomotive = false, enabled = true))
    }
}
