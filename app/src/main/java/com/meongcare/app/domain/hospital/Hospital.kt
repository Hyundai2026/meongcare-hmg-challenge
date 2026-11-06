package com.meongcare.app.domain.hospital

import com.meongcare.app.domain.guidance.Destination

/**
 * 병원. 길안내 목적지([Destination])와 전화번호를 함께 든다.
 * 전화번호는 길안내(NaviHelper `RouteInfo`)와 관계가 없어서 [Destination]에 넣지 않는다.
 */
data class Hospital(
    val destination: Destination,
    /** 없으면 [병원에 전화]를 숨긴다 (나중 병원 검색 결과에 번호가 없을 수 있다). */
    val phoneNumber: String?,
    /** 시연용 가상 병원이면 true. 화면에 "시연용"이라고 알린다. */
    val isSample: Boolean,
    /**
     * 제휴 병원 문서 ID (docs/data/hospital-status.md, 예: `demo-fast`). 카드를 보낼 곳이다.
     * 제휴 병원 목록이 아닌 병원(010의 고정 시연 병원 등)은 null이다.
     */
    val id: String? = null,
) {
    val name: String get() = destination.name
}
