package com.meongcare.app.domain.driving

import kotlinx.coroutines.flow.Flow

/**
 * 주행 상태를 알려 주는 창구.
 *
 * - 구독하면 현재 상태를 먼저 내보내고, 상태가 바뀔 때마다 새 값을 내보낸다.
 * - 같은 상태를 연달아 내보내지 않는다.
 *
 * 구현:
 * - Fake: debug 패널에서 상태를 바꾼다. GPU 없는 PC 개발, 테스트, 시연 영상용.
 * - Real: 차량의 주행 규제(AAOS `CarUxRestrictions`)를 따른다. 주행 최적화가 필요하면 DRIVING,
 *   필요 없으면 PARKED, 알 수 없거나 차량 기기가 아니면 UNKNOWN (docs/pleos/ux-guidelines.md).
 */
interface DrivingStateSource {
    val drivingState: Flow<DrivingState>
}
