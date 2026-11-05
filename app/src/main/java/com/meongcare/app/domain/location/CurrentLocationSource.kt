package com.meongcare.app.domain.location

/**
 * 차의 현재 위치를 묻는 곳. 위치를 계속 따라가지 않고, 필요할 때(병원 검색 전 등) 한 번 묻는다.
 *
 * 구현: 차량 기기의 시연·Real 모드는 내비(Pleos NaviHelper), 그 밖은 Fake다 (`di/LocationPlan.kt`).
 */
interface CurrentLocationSource {
    /**
     * 지금 위치를 한 번 묻는다.
     * - 모르면 null이다: 연결 실패, 시간 초과, 내비 오류, 값 이상.
     * - 취소 말고는 예외를 던지지 않는다. 부른 코루틴이 취소되면 연결을 놓고 `CancellationException`을 그대로 낸다.
     * - 여러 번 불러도 된다. 부를 때마다 새로 묻는다 (캐시 없음).
     */
    suspend fun current(): CurrentLocation?
}
