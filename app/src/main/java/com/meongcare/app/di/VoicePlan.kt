package com.meongcare.app.di

/** 음성 입력을 끼울지, 어떤 구현을 끼울지. */
enum class VoiceInputKind {
    /** 음성 입력 없음. 증상 고르기 화면에 음성 영역이 없다. */
    NONE,

    /** 예시 문장을 받아쓰는 시연용 (`FakeVoiceInput`). 화면에 시연용이라고 알린다. */
    FAKE,

    /** Pleos SpeechToText `ON_DEVICE` (`RealVoiceInput`). */
    REAL,
}

/**
 * 음성 입력을 앱 화면에서 뺄 때 false로 바꾼다 (주최측 문의 B2가 "쓰면 안 된다"일 때).
 * APK에서 SDK와 권한까지 빼는 법은 docs/architecture.md "음성 입력 빼는 법".
 */
const val VOICE_INPUT_ENABLED = true

/**
 * 데이터 모드와 기기 종류로 음성 입력 구성을 정한다. Android를 모르는 순수 함수다 (docs/plans 010 "설계 결정 7").
 *
 * [SourcePlan]에 칸을 더하지 않고 따로 뒀다. 음성만 따로 빼기 쉽고, 기존 구성표 테스트를 고치지 않아도 된다.
 *
 * | 모드 | 차량 기기 | 음성 입력 |
 * |---|---|---|
 * | Fake | 상관없음 | FAKE |
 * | 시연 | 예 (Pleos 에뮬레이터, 실차) | REAL (마이크) |
 * | 시연 | 아니오 (일반 폰) | FAKE (음성 서비스가 없다) |
 * | Real | 예 | REAL |
 * | Real | 아니오 | NONE |
 *
 * @param isAutomotive 기기에 `PackageManager.FEATURE_AUTOMOTIVE`가 있는지
 * @param enabled [VOICE_INPUT_ENABLED]. false면 모두 NONE.
 */
fun voiceInputKindOf(mode: DataMode, isAutomotive: Boolean, enabled: Boolean = VOICE_INPUT_ENABLED): VoiceInputKind {
    if (!enabled) return VoiceInputKind.NONE
    return when (mode) {
        DataMode.FAKE -> VoiceInputKind.FAKE
        DataMode.DEMO -> if (isAutomotive) VoiceInputKind.REAL else VoiceInputKind.FAKE
        DataMode.REAL -> if (isAutomotive) VoiceInputKind.REAL else VoiceInputKind.NONE
    }
}
