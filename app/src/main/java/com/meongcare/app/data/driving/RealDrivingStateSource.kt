package com.meongcare.app.data.driving

import com.meongcare.app.domain.driving.DrivingState
import com.meongcare.app.domain.driving.DrivingStateSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * 실제 차량의 주행 상태. **아직 자리표시다.** 항상 [DrivingState.UNKNOWN]만 내보낸다.
 *
 * 후속 계획에서 AAOS `CarUxRestrictionsManager`로 구현한다 (docs/pleos/ux-guidelines.md "주행 규제").
 * 그 전에 확인할 것 (docs/pleos/sdk-notes.md "미확인"):
 * - `android.car` 라이브러리를 쓰기 위한 Gradle 설정
 * - Pleos 에뮬레이터에서 기어를 바꾸면 CarUxRestrictions가 바뀌는지
 */
class RealDrivingStateSource : DrivingStateSource {
    override val drivingState: Flow<DrivingState> = flowOf(DrivingState.UNKNOWN)
}
