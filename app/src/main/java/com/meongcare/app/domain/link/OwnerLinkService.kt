package com.meongcare.app.domain.link

import java.time.Instant
import kotlinx.coroutines.flow.StateFlow

/**
 * 보호자 웹과 차의 연결 (014). 흐름: docs/data/owner-link.md "흐름".
 *
 * 화면은 이 인터페이스만 안다. 구현은 Fake(debug·테스트)와 서버(Firestore REST)다.
 * 보호자 uid는 data 쪽만 안다. 도메인과 화면은 "연결됐는가"만 본다.
 *
 * 실패는 결과 값이다 (취소 말고는 던지지 않는다).
 */
interface OwnerLinkService {
    val state: StateFlow<OwnerLinkState>

    /** 새 연결 코드를 만든다 (정차 중 연결 화면을 열 때) */
    suspend fun start(): LinkStart

    /** 보호자가 이 코드를 입력했는지 한 번 묻는다. 입력했으면 연결되고 [state]가 [OwnerLinkState.Linked]가 된다 */
    suspend fun check(code: LinkCode): LinkCheck

    /** 쓰지 않은 코드를 버린다 (화면을 떠날 때, [새 코드]). 실패해도 던지지 않는다 (만료되면 아무도 쓰지 못한다) */
    suspend fun cancel(code: LinkCode)

    /**
     * 연결을 끊는다. 서버의 허가를 지우고, 지우지 못해도 차는 연결 전으로 돌아간다 (보호자를 잊는다).
     * @return 서버의 허가까지 지웠으면 true
     */
    suspend fun unlink(): Boolean

    /** 프로필을 읽다가 허가가 없어졌다(403)는 것을 알았을 때 data 쪽이 부른다. 연결됨이면 [OwnerLinkState.Revoked]로 */
    fun markRevoked()

    /** 화면이 "보호자가 연결을 끊었어요"를 보였다. [OwnerLinkState.Revoked]면 연결 전으로 */
    fun acknowledgeRevoked()
}

sealed interface OwnerLinkState {
    /** 연결을 쓸 수 없다 (서버 설정 없음). 처음 화면에 연결 버튼이 없다 */
    data object Unavailable : OwnerLinkState

    /** 연결 전 */
    data object Unlinked : OwnerLinkState

    /** 보호자 웹과 연결됐다 */
    data class Linked(val linkedAt: Instant) : OwnerLinkState

    /** 보호자(또는 서버)가 연결을 끊었다. 처음 화면에 한 번 알리고 [acknowledgeRevoked][OwnerLinkService.acknowledgeRevoked]로 연결 전이 된다 */
    data object Revoked : OwnerLinkState
}

sealed interface LinkStart {
    /** @param createdAt 차 시계로 본 만든 시각. 화면의 남은 시간 어림에만 쓴다 (만료는 서버가 판단한다) */
    data class Created(val code: LinkCode, val createdAt: Instant) : LinkStart

    data class Failed(val reason: String) : LinkStart
}

sealed interface LinkCheck {
    /** 아직 보호자가 입력하지 않았다 */
    data object Waiting : LinkCheck

    /** 연결됐다 */
    data object Linked : LinkCheck

    /** 코드가 없다 (지워졌다). 화면은 만료처럼 새 코드를 권한다 */
    data object Gone : LinkCheck

    data class Failed(val reason: String) : LinkCheck
}
