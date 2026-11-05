package com.meongcare.app.data.driving

import com.meongcare.app.domain.driving.DrivingState
import com.meongcare.app.domain.driving.DrivingStateSource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * 실제 차량의 주행 상태. 차량의 주행 규제(`CarUxRestrictions`) 값을 따른다.
 *
 * 매핑 (docs/plans 006 "설계 결정 2"):
 * - 규제 값이 오기 전, 값을 알 수 없을 때, 규제 연결이 없는 기기(일반 폰) → [DrivingState.UNKNOWN]
 * - 주행 최적화가 필요함 (`isRequiresDistractionOptimization` = true) → [DrivingState.DRIVING]
 * - 필요 없음 (false) → [DrivingState.PARKED]
 *
 * 기어·속도로 직접 판단하지 않는다. Google과 Pleos 모두 규제 값에 반응하라고 한다
 * (docs/pleos/ux-guidelines.md "주행 상태가 아니라 규제 값에 반응한다").
 * 그래서 기어 D에 멈춰 있어도(Idling) 주행이다 (Pleos 에뮬레이터에서 규제가 걸리는 것을 확인).
 *
 * 연결 수명: 구독할 때 연결하고, 구독이 끝나면 끊는다. 메인 스레드에서 구독한다.
 *
 * android.car를 직접 부르지 않는다. 그 일은 [UxRestrictionsClient] 구현이 한다.
 *
 * @param client 주행 규제를 받는 창구. `null`이면 규제를 받을 수 없는 기기라서 UNKNOWN만 낸다.
 */
class RealDrivingStateSource(
    private val client: UxRestrictionsClient? = null,
) : DrivingStateSource {

    override val drivingState: Flow<DrivingState> = callbackFlow {
        trySend(DrivingState.UNKNOWN)
        val connection: AutoCloseable? = client?.let { c ->
            try {
                c.connect { event -> trySend(event.toDrivingState()) }
            } catch (e: Exception) {
                // 연결에 실패하면 이번 구독 동안은 모름으로 둔다. 다음 구독 때 다시 연결한다.
                trySend(DrivingState.UNKNOWN)
                null
            }
        }
        awaitClose {
            try {
                connection?.close()
            } catch (_: Exception) {
                // 닫다가 난 예외로 구독 종료를 막지 않는다.
            }
        }
    }
        // 상태 값이라 최신 값만 의미가 있다. 구독자가 늦게 받는 사이 값이 몰려도 버퍼가 넘쳐
        // 최신 값을 잃지 않게, 밀린 값은 버리고 마지막 값만 남긴다 (tester가 찾음: 66개 이상 몰리면 최신 값이 버려졌다).
        .conflate()
        .distinctUntilChanged()
}

internal fun UxRestrictionsEvent.toDrivingState(): DrivingState = when (this) {
    is UxRestrictionsEvent.Changed ->
        if (requiresDistractionOptimization) DrivingState.DRIVING else DrivingState.PARKED
    UxRestrictionsEvent.Unavailable -> DrivingState.UNKNOWN
}
