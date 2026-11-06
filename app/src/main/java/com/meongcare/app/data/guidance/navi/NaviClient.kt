package com.meongcare.app.data.guidance.navi

import com.meongcare.app.domain.guidance.Destination
import kotlinx.coroutines.flow.Flow

/**
 * 차량 내비(Pleos NaviHelper)와의 경계. SDK 타입을 밖으로 내보내지 않아서, 길안내 구현을 JVM으로 시험할 수 있다.
 * 구현: [PleosNaviClient] (SDK를 부르는 유일한 파일). 사실: docs/pleos/sdk-notes.md "NaviHelper" (`[obs-navi-emu]`).
 */
interface NaviClient {
    /**
     * 안내 한 번. 구독하면 내비에 연결하고, 연결되면 [destination]으로 경로를 요청한 뒤 이벤트를 내보낸다.
     * 구독을 끊으면: 아직 도착·취소 전이면 경로를 취소하고, 연결을 놓는다.
     */
    fun trip(destination: Destination): Flow<NaviEvent>

    /**
     * [trip]과 같다. 다만 구독을 끊을 때(도착·취소 이벤트로 이미 끝난 경우 제외) [onClose]를 한 번 불러,
     * [RouteOnClose.KEEP]이면 경로를 취소하지 않고 연결만 놓는다 (013 U2).
     * 기본 구현은 [trip]이다 (항상 취소).
     */
    fun trip(destination: Destination, onClose: () -> RouteOnClose): Flow<NaviEvent> = trip(destination)
}

/** 안내 구독을 끊을 때 내비 경로를 어떻게 할지 (013 U2). */
enum class RouteOnClose {
    /**
     * 경로를 취소한다 (`cancelRoute`). 내비 서비스가 내비 앱을 앞으로 띄워 우리 앱을 가리고,
     * 우리 앱은 스스로 돌아올 수 없다 (`[obs-navi-emu]` "경로 취소 뒤 앞에 나오는 앱", 013 U3).
     */
    CANCEL,

    /** 경로를 내비에 남기고 리스너 해제·`release()`만 한다. 다음 `requestRoute`가 새 경로로 바꾼다. */
    KEEP,
}

/** 내비에서 오는 일. 값의 단위는 Pleos 에뮬레이터 관찰로 정했다 (duration 초, distance 미터). */
sealed interface NaviEvent {
    /** 내비가 경로 안내를 시작했다 (`onRouteStarted`). 남은 시간은 초 */
    data class RouteStarted(val remainingSeconds: Int) : NaviEvent

    /** 안내 중 남은 시간이 바뀌었다 (`onDrivingInfo`의 목적지 남은 시간, 초) */
    data class Remaining(val remainingSeconds: Int) : NaviEvent

    /** 목적지에 도착했다 (`onDestinationArrived`). 에뮬레이터에서는 볼 수 없었다 (차가 움직이지 않는다) */
    data object Arrived : NaviEvent

    /** 경로가 취소됐다 (`onRouteCancelled`, 내비 화면에서 사용자가 취소한 경우 포함) */
    data object Cancelled : NaviEvent

    /** 연결 실패나 경로 오류. 이유는 로그용 이름 */
    data class Failed(val reason: String) : NaviEvent
}
