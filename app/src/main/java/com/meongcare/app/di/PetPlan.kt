package com.meongcare.app.di

import com.meongcare.app.domain.link.OwnerLinkState

/** 보호자 웹 연결을 무엇으로 하는지 (014). */
enum class LinkKind {
    /** 연결 없음. 처음 화면에 연결 버튼이 없다 */
    NONE,

    /** 메모리 Fake. debug 패널 [가짜 보호자 코드 입력]으로 연결을 흉내 낸다. 연결해도 그때 고른 샘플 반려견이다 (021) */
    FAKE,

    /** 서버 (Firestore REST) */
    SERVER,
}

/**
 * 반려견 프로필 구성 (014).
 * @param sampleBeforeLink 연결 전(또는 연결 없음)에 샘플 반려견(왕건이·오숑이 중 고른 것)을 보이는지. false면 "등록 안 됨"이다
 */
data class PetPlan(val link: LinkKind, val sampleBeforeLink: Boolean)

/**
 * 반려견 프로필 출처를 정한다 (계획 014 설계 5절). 순수 함수라 JVM으로 시험한다.
 * [sourcePlanOf]·[hospitalSourceKindOf]는 바꾸지 않는다.
 *
 * | 모드 | 서버 설정 | 연결 | 연결 전 반려견 |
 * |---|---|---|---|
 * | Fake | 상관없음 | Fake | 고른 샘플 |
 * | 시연 (release) | 없음 | 없음 | 고른 샘플 (지금과 같다) |
 * | 시연 | 있음 | 서버 | 고른 샘플 |
 * | Real | 있음 | 서버 | 등록 안 됨 |
 * | Real | 없음 | 없음 | 등록 안 됨 |
 *
 * @param hasServer `secrets.properties`에 Firebase 설정이 있는지 (`serverConfigOf`가 null이 아닌지)
 */
fun petPlanOf(mode: DataMode, hasServer: Boolean): PetPlan = when (mode) {
    DataMode.FAKE -> PetPlan(LinkKind.FAKE, sampleBeforeLink = true)
    DataMode.DEMO -> PetPlan(if (hasServer) LinkKind.SERVER else LinkKind.NONE, sampleBeforeLink = true)
    DataMode.REAL -> PetPlan(if (hasServer) LinkKind.SERVER else LinkKind.NONE, sampleBeforeLink = false)
}

/**
 * 화면에 "시연용 샘플 정보예요"를 보일지. 서버 연결로 받은 프로필만 샘플이 아니다.
 * Fake 연결은 연결해도 고른 샘플이라서 계속 샘플이다.
 */
fun petProfileIsSampleOf(plan: PetPlan, state: OwnerLinkState): Boolean = when (plan.link) {
    LinkKind.FAKE -> true
    LinkKind.NONE -> plan.sampleBeforeLink
    LinkKind.SERVER -> plan.sampleBeforeLink && state !is OwnerLinkState.Linked
}
