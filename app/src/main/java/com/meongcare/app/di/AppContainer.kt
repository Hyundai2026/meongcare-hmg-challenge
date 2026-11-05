package com.meongcare.app.di

import android.content.Context
import com.meongcare.app.data.demo.FakeTripScenario
import com.meongcare.app.data.driving.FakeDrivingStateSource
import com.meongcare.app.data.driving.RealDrivingStateSource
import com.meongcare.app.data.driving.uxRestrictionsClientOrNull
import com.meongcare.app.data.guidance.FakeGuidanceSource
import com.meongcare.app.data.guidance.RealGuidanceSource
import com.meongcare.app.data.hospital.SampleHospitals
import com.meongcare.app.data.vehicle.VehicleConnection
import com.meongcare.app.domain.driving.DrivingStateSource
import com.meongcare.app.domain.guidance.Destination
import com.meongcare.app.domain.guidance.GuidanceSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * 앱 전체에서 쓰는 객체를 만들고 들고 있는 곳 (수동 DI).
 *
 * 데이터 모드에 따라 Fake 또는 Real 구현을 골라 끼운다.
 * 화면 쪽은 인터페이스([DrivingStateSource], [GuidanceSource])만 보므로, 어느 쪽이 끼워졌는지 모른다.
 */
class AppContainer(context: Context, val dataMode: DataMode) {

    /** 프로세스와 수명이 같은 코루틴 범위. Fake 길안내의 시간 흐름과 시연 시나리오가 여기서 돈다. */
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** debug 패널이 상태를 바꿀 때 쓴다. FAKE 모드에서만 화면에 연결된다. */
    val fakeDrivingStateSource = FakeDrivingStateSource()

    val drivingStateSource: DrivingStateSource = when (dataMode) {
        DataMode.FAKE -> fakeDrivingStateSource
        // 차량 기기면 주행 규제(CarUxRestrictions)를 받고, 일반 폰이면 UNKNOWN만 낸다.
        DataMode.REAL -> RealDrivingStateSource(uxRestrictionsClientOrNull(context))
    }

    val guidanceSource: GuidanceSource = when (dataMode) {
        DataMode.FAKE -> FakeGuidanceSource(applicationScope)
        DataMode.REAL -> RealGuidanceSource()
    }

    /** 길안내 목적지. 병원 검색이 붙기 전까지 시연용 샘플 병원 1곳을 쓴다. */
    val sampleHospital: Destination = SampleHospitals.demo

    /** Pleos Vehicle 연결. REAL 모드에서만 만든다 (FAKE 모드에서는 Vehicle을 만들지 않는다). */
    val vehicleConnection: VehicleConnection? = when (dataMode) {
        DataMode.FAKE -> null
        DataMode.REAL -> VehicleConnection(context)
    }

    init {
        // FAKE 모드에서만: 안내를 시작하면 출발하고, 도착하면 주차하는 시연 흐름을 만든다.
        if (dataMode == DataMode.FAKE) {
            FakeTripScenario(applicationScope, guidanceSource, fakeDrivingStateSource).start()
        }
    }
}
