package com.meongcare.app.domain.guidance

/**
 * 길안내 목적지 (예: 동물병원).
 *
 * 이름과 좌표만 필수로 둔다. Real(NaviHelper `RouteInfo`)에서 `poiId`가 필수인지는 아직 모른다
 * (docs/pleos/sdk-notes.md NaviHelper 절 "미확인"). 필수로 밝혀지면 기본값이 있는 필드를 더한다.
 */
data class Destination(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val address: String? = null,
)
