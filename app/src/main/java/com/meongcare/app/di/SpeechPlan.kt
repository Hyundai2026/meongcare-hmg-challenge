package com.meongcare.app.di

/** 소리 안내(TTS)를 끼울지, 어떤 구현을 끼울지. */
enum class SpeechOutputKind {
    /** 소리 안내 없음. 안내자를 시작하지 않는다. */
    NONE,

    /** 소리를 내지 않고 시간만 보내는 Fake (`FakeSpeaker`). 화면이 없어 시연용 표시도 없다. */
    FAKE,

    /** Pleos TextToSpeech `ON_DEVICE` (`RealSpeaker`). */
    REAL,
}

/**
 * 소리 안내를 끌 때 false로 바꾼다 (App Market 심사 등에서 TTS가 허용되지 않을 때).
 * APK에서 SDK와 권한까지 빼는 법은 docs/architecture.md "소리 안내 빼는 법".
 */
const val SPEECH_OUTPUT_ENABLED = true

/**
 * 데이터 모드와 기기 종류로 소리 안내 구성을 정한다. Android를 모르는 순수 함수다 (docs/plans 017 "설계 결정 7").
 *
 * [SourcePlan]에 칸을 더하지 않고 따로 뒀다 (음성 입력 [voiceInputKindOf]와 같은 이유). 표도 음성 입력과 같다.
 *
 * | 모드 | 차량 기기 | 소리 안내 |
 * |---|---|---|
 * | Fake | 상관없음 | FAKE |
 * | 시연 | 예 (Pleos 에뮬레이터, 실차) | REAL |
 * | 시연 | 아니오 (일반 폰) | FAKE (음성 서비스가 없다) |
 * | Real | 예 | REAL |
 * | Real | 아니오 | NONE |
 *
 * @param isAutomotive 기기에 `PackageManager.FEATURE_AUTOMOTIVE`가 있는지
 * @param enabled [SPEECH_OUTPUT_ENABLED]. false면 모두 NONE.
 */
fun speechOutputKindOf(mode: DataMode, isAutomotive: Boolean, enabled: Boolean = SPEECH_OUTPUT_ENABLED): SpeechOutputKind {
    if (!enabled) return SpeechOutputKind.NONE
    return when (mode) {
        DataMode.FAKE -> SpeechOutputKind.FAKE
        DataMode.DEMO -> if (isAutomotive) SpeechOutputKind.REAL else SpeechOutputKind.FAKE
        DataMode.REAL -> if (isAutomotive) SpeechOutputKind.REAL else SpeechOutputKind.NONE
    }
}
