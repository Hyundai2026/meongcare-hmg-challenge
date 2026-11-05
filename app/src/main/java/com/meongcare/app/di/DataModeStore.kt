package com.meongcare.app.di

import android.annotation.SuppressLint
import android.content.Context
import com.meongcare.app.BuildConfig

/**
 * 데이터 모드를 저장하고 읽는다.
 *
 * - debug 빌드: debug 패널에서 고른 값을 쓴다. 고른 적이 없으면 [DataMode.FAKE].
 * - release 빌드: 저장된 값과 상관없이 항상 [DataMode.DEMO] (시연 모드).
 */
class DataModeStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** 규칙은 [resolveDataMode]에 있다. */
    fun read(): DataMode = resolveDataMode(
        isDebugBuild = BuildConfig.DEBUG,
        savedName = prefs.getString(KEY_DATA_MODE, null),
    )

    /** 바꾼 값은 앱을 다시 시작해야 적용된다. */
    @SuppressLint("ApplySharedPref")
    fun write(mode: DataMode) {
        // 바로 뒤에 프로세스를 다시 시작하므로 비동기 apply 대신 commit으로 확실히 저장한다.
        prefs.edit().putString(KEY_DATA_MODE, mode.name).commit()
    }

    private companion object {
        const val PREFS_NAME = "dev_settings"
        const val KEY_DATA_MODE = "data_mode"
    }
}
