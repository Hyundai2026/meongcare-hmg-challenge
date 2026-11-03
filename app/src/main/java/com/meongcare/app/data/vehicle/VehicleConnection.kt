package com.meongcare.app.data.vehicle

import ai.pleos.playground.vehicle.Vehicle
import android.content.Context
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

/**
 * Pleos [Vehicle]의 initialize/release를 한 곳에서 관리한다 (AGENTS.md 아키텍처 규칙).
 *
 * - 앱 프로세스가 화면에 보이기 시작하면(onStart) 새 [Vehicle]을 만들고 initialize한다.
 * - 화면에서 사라지면(onStop) release하고 버린다. 다시 보이면 새로 만든다.
 *   공식 샘플도 인스턴스 하나를 만들어 initialize하고, 끝날 때 release한 뒤 다시 쓰지 않는다.
 * - Real 모드에서만 쓴다. Fake 모드와 일반 에뮬레이터에는 차량 서비스가 없다.
 *
 * 차량 기능 래퍼는 [vehicle]을 여기서 얻는다. 직접 `Vehicle`을 만들지 않는다.
 * 주의: 도메인 API 콜백을 기다리는 중에는 release하면 안 된다 (docs/pleos/sdk-notes.md).
 *       도메인 API를 쓰는 래퍼를 만들 때 이 부분을 같이 처리한다.
 */
class VehicleConnection(context: Context) : DefaultLifecycleObserver {

    private val appContext = context.applicationContext

    /** 연결돼 있으면 initialize된 Vehicle, 아니면 null */
    var vehicle: Vehicle? = null
        private set

    override fun onStart(owner: LifecycleOwner) {
        if (vehicle != null) return
        val created = Vehicle(appContext)
        try {
            created.initialize()
            vehicle = created
        } catch (e: Exception) {
            // 차량 서비스가 없는 기기 등. 앱을 죽이지 않고 연결 없이 계속한다.
            Log.w(TAG, "Vehicle initialize 실패", e)
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        val connected = vehicle ?: return
        vehicle = null
        try {
            connected.release()
        } catch (e: Exception) {
            Log.w(TAG, "Vehicle release 실패", e)
        }
    }

    private companion object {
        const val TAG = "VehicleConnection"
    }
}
