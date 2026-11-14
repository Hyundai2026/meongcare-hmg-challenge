package com.meongcare.app.ui.home

import com.meongcare.app.domain.card.CardSessionState
import com.meongcare.app.domain.trip.FinderStep
import com.meongcare.app.ui.guidance.GuidanceUiState

/** 정차 화면(PARKED·ARRIVAL) 안에서 보일 것. */
enum class ParkedContent {
    /** 처음 화면: 반려견 요약 + [진료 카드 만들기] */
    HOME,

    /** 증상 고르기 */
    SYMPTOMS,

    /** 진료 카드 (안내 없음·안내 중·도착에 따라 머리와 버튼이 다르다) */
    CARD,

    /** 가장 빨리 진료받을 병원 목록 (012) */
    HOSPITALS,

    /** 고른 병원 확인 (012) */
    CONFIRM_HOSPITAL,

    /** 보호자 웹 연결 코드 (014). 처음 화면에서만 연다 */
    LINK,

    /** 반려동물 카드 (019, 보기 전용). 처음 화면 바로가기에서 연다 */
    PET_CARD,

    /** 릴렉스 모드 (019, 시연용 화면. 차량에 적용하지 않는다) */
    RELAX,

    /** 진료 완료 (디자인 시안 after, 2026-11-14) */
    AFTER_VISIT,

    /** 귀가 중 (디자인 시안 trip, 2026-11-14) */
    GOING_HOME,

    /** 귀가 완료 (디자인 시안 fin, 2026-11-14) */
    HOME_ARRIVED,
}

/** 처음 화면 바로가기로 연 쪽 (019). 정차 중에만 보인다 */
enum class HomePage { NONE, PET_CARD, RELAX, AFTER_VISIT, GOING_HOME, HOME_ARRIVED }

/**
 * 정차 화면 안에서 보일 것을 고른다. **주행 중에는 불리지 않는다**: [homeContentOf]가 주행·모름이면 먼저
 * 주행 화면을 고른다 (AGENTS.md 규칙 3). 그 규칙은 바꾸지 않고, 정차 쪽 안에서만 한 번 더 고른다.
 *
 * | 안내 \ 카드 세션 | 없음 | 고르는 중 | 카드 있음 |
 * |---|---|---|---|
 * | 안내 없음 | HOME | SYMPTOMS | CARD (병원 찾기 중이면 HOSPITALS / CONFIRM_HOSPITAL) |
 * | 안내 중 | CARD (고른 증상 없음) | SYMPTOMS | CARD |
 * | 도착 | CARD (고른 증상 없음) | SYMPTOMS | CARD |
 *
 * 안내 중·도착인데 세션이 없으면(나중 Real 길안내에서 내비 앱으로 시작한 안내 등) 등록된 의료정보만으로 카드를 보인다.
 * 병원 찾기 단계는 카드가 있고 안내가 없을 때만 보인다 (출발하면 길안내가 시작되므로 카드로 돌아간다).
 *
 * @param atHome [홈]을 눌러 처음 화면을 보는 중이면 true. 고르는 중이 아니면 처음 화면이다 (처음 화면에 [카드 보기]가 있다).
 * @param finder 병원 찾기 단계 (012). 주행으로 바뀌었다가 정차하면 같은 단계로 돌아온다.
 * @param linking 보호자 웹 연결 화면이 열려 있으면 true (014). 증상 고르기가 먼저고, 안내 중에는 보이지 않는다.
 * @param page 처음 화면 바로가기로 연 쪽 (019). 증상 고르기·연결 화면 다음이고, 처음 화면·카드·병원 찾기보다 먼저다.
 * @param finderWithoutCard 홈 [병원 찾기]로 카드 없이 연 병원 찾기 (019). 안내가 없으면 카드 세션·[atHome]과 상관없이 목록·확인이다.
 */
fun parkedContentOf(
    guidance: GuidanceUiState,
    session: CardSessionState,
    atHome: Boolean = false,
    finder: FinderStep = FinderStep.None,
    linking: Boolean = false,
    page: HomePage = HomePage.NONE,
    finderWithoutCard: Boolean = false,
): ParkedContent =
    when {
        session is CardSessionState.Picking -> ParkedContent.SYMPTOMS
        linking && guidance is GuidanceUiState.Idle -> ParkedContent.LINK
        page == HomePage.PET_CARD -> ParkedContent.PET_CARD
        page == HomePage.RELAX -> ParkedContent.RELAX
        page == HomePage.AFTER_VISIT -> ParkedContent.AFTER_VISIT
        page == HomePage.GOING_HOME -> ParkedContent.GOING_HOME
        page == HomePage.HOME_ARRIVED -> ParkedContent.HOME_ARRIVED
        finderWithoutCard && guidance is GuidanceUiState.Idle && finder is FinderStep.Hospitals -> ParkedContent.HOSPITALS
        finderWithoutCard && guidance is GuidanceUiState.Idle && finder is FinderStep.Confirm -> ParkedContent.CONFIRM_HOSPITAL
        atHome -> ParkedContent.HOME
        session is CardSessionState.Ready && guidance is GuidanceUiState.Idle && finder is FinderStep.Hospitals ->
            ParkedContent.HOSPITALS
        session is CardSessionState.Ready && guidance is GuidanceUiState.Idle && finder is FinderStep.Confirm ->
            ParkedContent.CONFIRM_HOSPITAL
        session is CardSessionState.Ready -> ParkedContent.CARD
        else -> if (guidance is GuidanceUiState.Idle) ParkedContent.HOME else ParkedContent.CARD
    }
