package com.meongcare.app.ui.home

import com.meongcare.app.domain.card.CardSessionState
import com.meongcare.app.ui.guidance.GuidanceUiState

/** 정차 화면(PARKED·ARRIVAL) 안에서 보일 것. */
enum class ParkedContent {
    /** 처음 화면: 반려견 요약 + [진료 카드 만들기] */
    HOME,

    /** 증상 고르기 */
    SYMPTOMS,

    /** 진료 카드 (안내 없음·안내 중·도착에 따라 머리와 버튼이 다르다) */
    CARD,
}

/**
 * 정차 화면 안에서 보일 것을 고른다. **주행 중에는 불리지 않는다**: [homeContentOf]가 주행·모름이면 먼저
 * 주행 화면을 고른다 (AGENTS.md 규칙 3). 그 규칙은 바꾸지 않고, 정차 쪽 안에서만 한 번 더 고른다.
 *
 * | 안내 \ 카드 세션 | 없음 | 고르는 중 | 카드 있음 |
 * |---|---|---|---|
 * | 안내 없음 | HOME | SYMPTOMS | CARD |
 * | 안내 중 | CARD (고른 증상 없음) | SYMPTOMS | CARD |
 * | 도착 | CARD (고른 증상 없음) | SYMPTOMS | CARD |
 *
 * 안내 중·도착인데 세션이 없으면(나중 Real 길안내에서 내비 앱으로 시작한 안내 등) 등록된 의료정보만으로 카드를 보인다.
 */
fun parkedContentOf(guidance: GuidanceUiState, session: CardSessionState): ParkedContent = when (session) {
    is CardSessionState.Picking -> ParkedContent.SYMPTOMS
    is CardSessionState.Ready -> ParkedContent.CARD
    CardSessionState.None -> if (guidance is GuidanceUiState.Idle) ParkedContent.HOME else ParkedContent.CARD
}
