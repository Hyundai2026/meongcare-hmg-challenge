package com.meongcare.app.data.voice

/**
 * Fake 음성 입력이 받아쓰는 예시 문장. 모두 증상을 하나 이상 찾아야 한다
 * (`SymptomMatcherTest`에 같은 문장으로 확인한다. 바꾸면 그 테스트도 같이 바꾼다).
 */
object SampleVoiceSentences {
    val all: List<String> = listOf(
        // Pleos 에뮬레이터에서 실제 엔진이 똑같이 받아쓴 문장 (docs/pleos/sdk-notes.md "STT `ON_DEVICE` 실행 확인")
        "강아지가 갑자기 토하고 숨을 가쁘게 쉬어요",
        "기침을 하고 기운이 없어요",
        "다리를 절고 밥을 안 먹어요",
    )
}
