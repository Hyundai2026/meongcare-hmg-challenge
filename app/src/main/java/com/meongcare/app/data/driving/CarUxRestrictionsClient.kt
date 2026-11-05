package com.meongcare.app.data.driving

import android.car.Car
import android.car.drivingstate.CarUxRestrictions
import android.car.drivingstate.CarUxRestrictionsManager
import android.content.Context
import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean

/**
 * AAOS `CarUxRestrictionsManager`로 주행 규제 값을 받는 [UxRestrictionsClient].
 * 이 앱에서 `android.car`를 부르는 곳은 이 파일뿐이다.
 *
 * - `FEATURE_AUTOMOTIVE`가 있는 기기에서만 만든다 ([uxRestrictionsClientOrNull]).
 *   일반 폰에서 `Car`를 부르면 예외가 난다 (docs/pleos/sdk-notes.md).
 * - 권한을 선언하지 않는다. 공개 메서드 3개에 권한 표기가 없다 (docs/plans 006 "설계 결정 6").
 * - `connect()`마다 새 `Car`를 만든다. 같은 `Car`를 다시 쓰지 않는다.
 * - 메인 스레드에서 부른다. `createCar`에 handler를 넘기지 않아서 콜백도 메인 스레드에서 온다.
 *
 * 상황별 동작은 docs/plans 006 "설계 결정 4" 표를 따른다.
 */
class CarUxRestrictionsClient(private val context: Context) : UxRestrictionsClient {

    override fun connect(onEvent: (UxRestrictionsEvent) -> Unit): AutoCloseable =
        Connection(context, onEvent).also { it.open() }

    /** 연결 하나. `close()` 뒤에 오는 콜백은 무시한다. */
    private class Connection(
        private val context: Context,
        private val onEvent: (UxRestrictionsEvent) -> Unit,
    ) : AutoCloseable {

        private val closed = AtomicBoolean(false)
        private var car: Car? = null
        private var manager: CarUxRestrictionsManager? = null

        fun open() {
            val created = try {
                // 기다리지 않고 바로 돌아온다. 초기화는 연결 콜백 안에서 한다 (Google 권장).
                // 차량 서비스가 다시 시작돼도 같은 콜백으로 다시 연결된다.
                Car.createCar(context, null, Car.CAR_WAIT_TIMEOUT_DO_NOT_WAIT) { c, ready ->
                    onLifecycleChanged(c, ready)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Car.createCar 실패", e)
                null
            } catch (e: LinkageError) {
                Log.e(TAG, "Car.createCar를 찾을 수 없다", e)
                null
            }
            if (created == null) {
                Log.w(TAG, "차량 서비스에 연결하지 못했다")
                emit(UxRestrictionsEvent.Unavailable)
                return
            }
            car = created
        }

        private fun onLifecycleChanged(c: Car, ready: Boolean) {
            if (closed.get()) return
            if (!ready) {
                // 차량 서비스가 죽었다. 다시 살아나면 ready = true로 다시 온다.
                Log.w(TAG, "차량 서비스 연결이 끊겼다")
                releaseManager()
                emit(UxRestrictionsEvent.Unavailable)
                return
            }
            try {
                val mgr = c.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE) as? CarUxRestrictionsManager
                if (mgr == null) {
                    Log.w(TAG, "CarUxRestrictionsManager가 없다")
                    emit(UxRestrictionsEvent.Unavailable)
                    return
                }
                releaseManager()
                // 등록을 먼저 해서, 현재 값을 읽는 사이에 바뀐 값을 놓치지 않는다.
                mgr.registerListener { restrictions -> report(restrictions) }
                manager = mgr
                report(mgr.currentCarUxRestrictions)
            } catch (e: SecurityException) {
                // 권한이 필요하다는 뜻이다. 권한은 사용자 승인 없이 추가하지 않는다 (AGENTS.md 규칙 2).
                Log.e(TAG, "주행 규제를 읽을 권한이 없다", e)
                emit(UxRestrictionsEvent.Unavailable)
            } catch (e: Exception) {
                Log.e(TAG, "주행 규제 연결 실패", e)
                emit(UxRestrictionsEvent.Unavailable)
            }
        }

        private fun report(restrictions: CarUxRestrictions?) {
            if (restrictions == null) {
                Log.w(TAG, "주행 규제 값이 없다")
                emit(UxRestrictionsEvent.Unavailable)
                return
            }
            val requiresDo = restrictions.isRequiresDistractionOptimization
            // dumpsys car_service의 값과 맞춰 보기 위해 남긴다.
            Log.i(TAG, "DO=$requiresDo active=0x${Integer.toHexString(restrictions.activeRestrictions)}")
            emit(UxRestrictionsEvent.Changed(requiresDo))
        }

        private fun emit(event: UxRestrictionsEvent) {
            if (!closed.get()) onEvent(event)
        }

        private fun releaseManager() {
            try {
                manager?.unregisterListener()
            } catch (_: Exception) {
                // 서비스가 이미 죽었으면 해제가 실패할 수 있다. 버리기만 하면 된다.
            }
            manager = null
        }

        override fun close() {
            if (!closed.compareAndSet(false, true)) return
            releaseManager()
            try {
                car?.disconnect()
            } catch (e: Exception) {
                Log.w(TAG, "Car.disconnect 실패", e)
            }
            car = null
            Log.i(TAG, "주행 규제 연결을 닫았다")
        }
    }

    companion object {
        const val TAG = "UxRestrictions"
    }
}
