package com.meongcare.app.data.driving

/**
 * 차량의 주행 규제(`CarUxRestrictions`)에서 이 앱이 쓰는 값만 옮긴 것.
 * `android.car` 타입을 밖으로 내보내지 않는다. 그래야 판단 로직을 JVM 단위 테스트로 확인할 수 있다.
 */
sealed interface UxRestrictionsEvent {

    /** 규제 값을 받았다. `true`면 주행 최적화(DO)가 필요한 상태다 (`isRequiresDistractionOptimization`). */
    data class Changed(val requiresDistractionOptimization: Boolean) : UxRestrictionsEvent

    /** 값을 알 수 없다: 차량 서비스 연결 실패, 매니저 없음, 서비스 끊김, 권한 거부 등. */
    data object Unavailable : UxRestrictionsEvent
}

/**
 * 차량의 주행 규제 값을 받는 창구. 실제 구현은 `CarUxRestrictionsClient`(android.car)다.
 */
fun interface UxRestrictionsClient {

    /**
     * 차량 서비스에 연결하고, 규제 값이 바뀔 때마다 [onEvent]를 부른다. 연결되면 현재 값을 한 번 보낸다.
     *
     * 돌려받은 것을 `close()`하면 리스너를 해제하고 연결을 끊는다. 두 번 불러도 된다.
     * 메인 스레드에서 부른다.
     */
    fun connect(onEvent: (UxRestrictionsEvent) -> Unit): AutoCloseable
}
