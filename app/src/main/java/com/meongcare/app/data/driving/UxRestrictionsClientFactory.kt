package com.meongcare.app.data.driving

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log

/**
 * 이 기기에서 주행 규제를 받을 수 있으면 [CarUxRestrictionsClient]를, 아니면 `null`을 준다.
 *
 * - 일반 폰(`FEATURE_AUTOMOTIVE` 없음)에서는 `android.car` 클래스를 아예 건드리지 않는다.
 *   `Car`는 차량 기기에서만 동작하고, 다른 기기에서 부르면 예외가 난다 (docs/pleos/sdk-notes.md).
 * - 이 파일은 `android.car`를 import하지 않는다. 어댑터 클래스는 확인을 마친 뒤에만 로드된다.
 */
fun uxRestrictionsClientOrNull(context: Context): UxRestrictionsClient? {
    if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)) {
        Log.i(CarUxRestrictionsClient.TAG, "FEATURE_AUTOMOTIVE 없음: 주행 규제를 받지 않는다")
        return null
    }
    return try {
        CarUxRestrictionsClient(context.applicationContext)
    } catch (e: LinkageError) {
        Log.e(CarUxRestrictionsClient.TAG, "android.car를 불러오지 못했다", e)
        null
    }
}
