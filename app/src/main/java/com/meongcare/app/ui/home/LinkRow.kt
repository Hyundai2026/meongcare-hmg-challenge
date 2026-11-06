package com.meongcare.app.ui.home

import com.meongcare.app.domain.link.OwnerLinkState
import com.meongcare.app.ui.guidance.GuidanceUiState

/** 처음 화면의 보호자 웹 연결 줄 (014, 계획 설계 5절 "처음 화면 연결 줄"). */
sealed interface LinkRow {
    /** 보이지 않는다: 연결을 쓸 수 없거나(서버 설정 없음) 안내 중·도착 */
    data object Hidden : LinkRow

    /** [보호자 웹과 연결]. [revoked]면 "보호자가 연결을 끊었어요"를 같이 보인다 */
    data class Unlinked(val revoked: Boolean = false) : LinkRow

    /** "보호자 웹과 연결됨" + [연결 끊기] */
    data object Linked : LinkRow
}

/** 연결은 안내가 없을 때 처음 화면에서만 시작하고 끊는다 (계획 014 K3) */
fun linkRowOf(state: OwnerLinkState, guidance: GuidanceUiState): LinkRow = when {
    guidance !is GuidanceUiState.Idle -> LinkRow.Hidden
    else -> when (state) {
        OwnerLinkState.Unavailable -> LinkRow.Hidden
        OwnerLinkState.Unlinked -> LinkRow.Unlinked()
        OwnerLinkState.Revoked -> LinkRow.Unlinked(revoked = true)
        is OwnerLinkState.Linked -> LinkRow.Linked
    }
}
