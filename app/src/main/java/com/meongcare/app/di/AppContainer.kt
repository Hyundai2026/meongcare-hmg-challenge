package com.meongcare.app.di

import com.meongcare.app.data.driving.FakeDrivingStateSource
import com.meongcare.app.data.driving.RealDrivingStateSource
import com.meongcare.app.domain.driving.DrivingStateSource

/**
 * 앱 전체에서 쓰는 객체를 만들고 들고 있는 곳 (수동 DI).
 *
 * 데이터 모드에 따라 Fake 또는 Real 구현을 골라 끼운다.
 * 화면 쪽은 인터페이스([DrivingStateSource])만 보므로, 어느 쪽이 끼워졌는지 모른다.
 */
class AppContainer(val dataMode: DataMode) {

    /** debug 패널이 상태를 바꿀 때 쓴다. FAKE 모드에서만 화면에 연결된다. */
    val fakeDrivingStateSource = FakeDrivingStateSource()

    val drivingStateSource: DrivingStateSource = when (dataMode) {
        DataMode.FAKE -> fakeDrivingStateSource
        DataMode.REAL -> RealDrivingStateSource()
    }
}
