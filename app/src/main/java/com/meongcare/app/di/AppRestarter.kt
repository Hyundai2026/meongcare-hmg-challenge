package com.meongcare.app.di

import android.app.Activity
import android.content.Intent

/**
 * debug 전용: 데이터 모드를 저장하고 앱을 다시 시작한다.
 *
 * 실행 중에 데이터 소스를 갈아 끼우지 않고 재시작하는 이유:
 * Vehicle SDK의 initialize/release를 Application 한 곳에서 단순하게 유지하고,
 * 바꾸는 도중에 상태가 꼬이지 않게 하기 위해서다 (docs/architecture.md "데이터 모드").
 */
object AppRestarter {

    fun restartWith(activity: Activity, mode: DataMode) {
        DataModeStore(activity).write(mode)
        activity.startActivity(Intent.makeRestartActivityTask(activity.componentName))
        Runtime.getRuntime().exit(0)
    }
}
