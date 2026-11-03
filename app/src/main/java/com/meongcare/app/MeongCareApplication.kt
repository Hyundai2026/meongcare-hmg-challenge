package com.meongcare.app

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import com.meongcare.app.di.AppContainer
import com.meongcare.app.di.DataModeStore

class MeongCareApplication : Application() {

    /** 앱 전체의 객체 보관소. 프로세스가 시작될 때 데이터 모드를 정하고 한 번 만든다. */
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(context = this, dataMode = DataModeStore(this).read())

        // Vehicle initialize/release는 앱이 보이고 사라질 때에 맞춘다 (REAL 모드만).
        // Application에는 종료 콜백이 없어서 프로세스 수명주기를 쓴다.
        container.vehicleConnection?.let { ProcessLifecycleOwner.get().lifecycle.addObserver(it) }
    }
}
