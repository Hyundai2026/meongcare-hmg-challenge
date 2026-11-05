package com.meongcare.app.domain.guidance

import kotlinx.coroutines.flow.Flow

/**
 * 차량 내비게이션에 길안내를 맡기고, 진행 상황을 받는 창구.
 *
 * 구현:
 * - Fake: 시간이 지나면 남은 시간이 줄고 도착한다. GPU 없는 PC 개발, 테스트, 시연 영상용.
 * - Real: Pleos NaviHelper로 구현한다 (후속 계획). 지금은 자리표시다.
 *   대응: `start` → `requestRoute`, 남은 시간 → `onTripInfo`, 도착 → `onDestinationArrived`,
 *   `cancel` → `cancelRoute` (docs/plans 005 "결정 1").
 */
interface GuidanceSource {

    /**
     * 구독하면 현재 상태를 먼저 내보내고, 바뀔 때마다 새 값을 내보낸다.
     * 같은 상태를 연달아 내보내지 않는다.
     */
    val state: Flow<GuidanceState>

    /** [destination]으로 안내를 시작한다. 안내 중이면 이전 안내를 새 목적지로 바꾼다. */
    suspend fun start(destination: Destination)

    /** 안내를 취소한다. 안내 중이 아니면 아무것도 하지 않는다. */
    suspend fun cancel()

    /** 도착 화면을 닫는다. [GuidanceState.Arrived]일 때만 [GuidanceState.Idle]로 바뀐다. */
    fun dismissArrival()
}
