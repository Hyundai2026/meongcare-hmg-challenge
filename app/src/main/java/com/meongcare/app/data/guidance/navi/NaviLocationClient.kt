package com.meongcare.app.data.guidance.navi

import kotlinx.coroutines.flow.Flow

/**
 * 차량 내비(Pleos NaviHelper)에 현재 위치를 묻는 경계. SDK 타입을 밖으로 내보내지 않아서, 위치 해석을 JVM으로 시험할 수 있다.
 * 구현: [PleosNaviClient] (SDK를 부르는 유일한 파일). 사실: docs/pleos/sdk-notes.md NaviHelper 절 `[aar-navi]` "현재 위치".
 *
 * 길안내([NaviClient])와 따로 둔 이유: 길안내 한 번과 위치 한 번은 다른 질문이다.
 * [NaviEvent]에 위치를 더하면 길안내 쪽 `when`과 시험용 가짜를 고쳐야 한다 (docs/plans 011 "설계 결정 1").
 */
fun interface NaviLocationClient {
    /**
     * 구독하면 새 NaviHelper로 연결하고, 연결되면 현재 위치를 한 번 요청해 결과를 내보낸다.
     * 구독을 끊으면 리스너를 떼고 연결을 놓는다(release). 경로는 건드리지 않는다.
     */
    fun currentLocation(): Flow<NaviLocationEvent>
}

/** 현재 위치를 물었을 때 내비에서 오는 일. */
sealed interface NaviLocationEvent {
    /** `onCurrentLocationInfo`. 값은 SDK가 준 그대로다 (주소는 null이나 빈 값일 수 있다). */
    data class Located(val latitude: Double, val longitude: Double, val address: String?) : NaviLocationEvent

    /** 연결 실패, `onError`, SDK 호출 예외. 이유는 로그용 이름(오류 코드 이름, 예외 클래스 이름)이다. */
    data class Failed(val reason: String) : NaviLocationEvent
}
