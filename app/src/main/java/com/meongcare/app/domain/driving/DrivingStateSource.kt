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
 * - Real: AAOS `CarUxRestrictions`를 기준으로 정한다 (docs/pleos/ux-guidelines.md). 구현은 후속 계획이다.
 */
interface DrivingStateSource {
    val drivingState: Flow<DrivingState>
}
