package com.meongcare.app

import android.app.Application
import com.meongcare.app.di.AppContainer
import com.meongcare.app.di.DataModeStore

class MeongCareApplication : Application() {

    /** 앱 전체의 객체 보관소. 프로세스가 시작될 때 데이터 모드를 정하고 한 번 만든다. */
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(dataMode = DataModeStore(this).read())
    }
}
