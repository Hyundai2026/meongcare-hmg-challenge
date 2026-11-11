package com.meongcare.app.di

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * `voiceInputKindOf` + `resolveDataMode` 경계 테스트. 기준: docs/plans/active/010-voice-input.md "설계 결정 7"의 구성표,
 * `[사용자 승인 필요]` 2의 답(시연 모드: 차량 기기는 Real, 일반 폰은 Fake), `resolveDataMode` KDoc
 * (debug: 저장된 이름이 DataMode 값이면 그것, 아니면 FAKE / release: 저장값과 상관없이 DEMO).
 */
class VoicePlanBoundaryTest {

    private val savedNames = listOf(null, "", "FAKE", "DEMO", "REAL", "real", "모르는 값")

    @Test
    fun `debug 빌드는 저장한 모드와 기기에 따라 음성 구성이 정해진다`() {
        val expected = mapOf(
            ("FAKE" to true) to VoiceInputKind.FAKE,
            ("FAKE" to false) to VoiceInputKind.FAKE,
            ("DEMO" to true) to VoiceInputKind.REAL,
            ("DEMO" to false) to VoiceInputKind.FAKE,
            ("REAL" to true) to VoiceInputKind.REAL,
            ("REAL" to false) to VoiceInputKind.NONE,
        )
        expected.forEach { (key, kind) ->
            val (saved, isAutomotive) = key
            val mode = resolveDataMode(isDebugBuild = true, savedName = saved)
            assertEquals("$saved / 차량 기기 $isAutomotive", kind, voiceInputKindOf(mode, isAutomotive, enabled = true))
        }
    }

    @Test
    fun `debug 빌드에서 저장값이 없거나 모르는 값이면 Fake 모드라 기기와 상관없이 Fake 음성이다`() {
        listOf(null, "", "real", "모르는 값").forEach { saved ->
            listOf(true, false).forEach { isAutomotive ->
                val mode = resolveDataMode(isDebugBuild = true, savedName = saved)
                assertEquals("$saved / $isAutomotive", VoiceInputKind.FAKE, voiceInputKindOf(mode, isAutomotive, enabled = true))
            }
        }
    }

    @Test
    fun `release 빌드는 저장값이 무엇이든 차량 기기에서 Real 일반 폰에서 Fake 음성이다`() {
        savedNames.forEach { saved ->
            val mode = resolveDataMode(isDebugBuild = false, savedName = saved)
            assertEquals("$saved", VoiceInputKind.REAL, voiceInputKindOf(mode, isAutomotive = true, enabled = true))
            assertEquals("$saved", VoiceInputKind.FAKE, voiceInputKindOf(mode, isAutomotive = false, enabled = true))
        }
    }

    @Test
    fun `스위치를 끄면 release 빌드에서도 음성 입력이 없다`() {
        savedNames.forEach { saved ->
            val mode = resolveDataMode(isDebugBuild = false, savedName = saved)
            listOf(true, false).forEach { isAutomotive ->
                assertEquals(VoiceInputKind.NONE, voiceInputKindOf(mode, isAutomotive, enabled = false))
            }
        }
    }

    @Test
    fun `스위치 값을 넘기지 않으면 VOICE_INPUT_ENABLED를 따른다`() {
        DataMode.entries.forEach { mode ->
            listOf(true, false).forEach { isAutomotive ->
                assertEquals(
                    "$mode / $isAutomotive",
                    voiceInputKindOf(mode, isAutomotive, enabled = VOICE_INPUT_ENABLED),
                    voiceInputKindOf(mode, isAutomotive),
                )
            }
        }
    }

    @Test
    fun `스위치가 켜져 있으면 음성 영역이 없는 구성은 일반 폰 Real 모드 하나뿐이다`() {
        val none = DataMode.entries.flatMap { mode -> listOf(true, false).map { mode to it } }
            .filter { (mode, isAutomotive) -> voiceInputKindOf(mode, isAutomotive, enabled = true) == VoiceInputKind.NONE }

        assertEquals(listOf(DataMode.REAL to false), none)
    }
}
