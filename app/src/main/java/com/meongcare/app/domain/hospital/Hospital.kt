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
) {
    val name: String get() = destination.name
}
