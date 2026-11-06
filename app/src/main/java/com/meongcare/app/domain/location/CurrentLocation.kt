package com.meongcare.app.domain.location

/** 차의 현재 위치. 나중에 병원 검색의 기준점이 된다. */
data class CurrentLocation(
    val latitude: Double,
    val longitude: Double,
    /** 내비가 준 주소 (예: "수정구 시흥동"). 모르면 null이다. 빈 문자열은 넣지 않는다. */
    val address: String?,
)
