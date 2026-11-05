package com.meongcare.app.di

import android.content.Context
import android.content.pm.PackageManager
import com.meongcare.app.data.demo.FakeTripScenario
import com.meongcare.app.data.driving.FakeDrivingStateSource
import com.meongcare.app.data.driving.RealDrivingStateSource
import com.meongcare.app.data.driving.uxRestrictionsClientOrNull
import com.meongcare.app.data.guidance.FakeGuidanceSource
import com.meongcare.app.data.guidance.RealGuidanceSource
import com.meongcare.app.data.hospital.SampleHospitals
import com.meongcare.app.data.vehicle.VehicleConnection
import com.meongcare.app.domain.driving.DrivingState
import com.meongcare.app.domain.driving.DrivingStateSource
import com.meongcare.app.domain.guidance.GuidanceSource
import com.meongcare.app.domain.hospital.Hospital
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

/**
 * 앱 전체에서 쓰는 객체를 만들고 들고 있는 곳 (수동 DI).
 *
 * 데이터 모드(Fake / 시연 / Real)에 따라 구현을 골라 끼운다. 무엇을 끼울지는 [sourcePlanOf]의 표가 정한다.
 * 화면 쪽은 인터페이스([DrivingStateSource], [GuidanceSource])만 보므로, 어느 쪽이 끼워졌는지 모른다.
 */
class AppContainer(context: Context, val dataMode: DataMode) {

    /** 이 프로세스에서 끼울 구현의 구성. */
    val sourcePlan: SourcePlan = sourcePlanOf(
        mode = dataMode,
        isAutomotive = context.packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE),
    )

    /** 프로세스와 수명이 같은 코루틴 범위. Fake 길안내의 시간 흐름과 시연 시나리오가 여기서 돈다. */
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** debug 패널이 상태를 바꿀 때 쓴다. 주행 상태가 Fake일 때만 화면에 연결된다. */
    val fakeDrivingStateSource = FakeDrivingStateSource()

    val drivingStateSource: DrivingStateSource = when (sourcePlan.driving) {
        SourceKind.FAKE -> fakeDrivingStateSource
        // 차량 기기면 주행 규제(CarUxRestrictions)를 받고, 일반 폰이면 UNKNOWN만 낸다.
        SourceKind.REAL -> RealDrivingStateSource(uxRestrictionsClientOrNull(context))
    }

    val guidanceSource: GuidanceSource = when (sourcePlan.guidance) {
        SourceKind.FAKE -> FakeGuidanceSource(
            scope = applicationScope,
            progressGate = if (sourcePlan.guidanceWaitsForDriving) isDrivingFlow() else null,
        )
        SourceKind.REAL -> RealGuidanceSource()
    }

    /** 병원 (길안내 목적지 + 전화번호). 병원 검색이 붙기 전까지 시연용 샘플 병원 1곳을 쓴다. */
    val sampleHospital: Hospital = SampleHospitals.demo

    /** Pleos Vehicle 연결. Real 구성에서만 만든다 (Fake에서는 Vehicle을 만들지 않는다). */
    val vehicleConnection: VehicleConnection? =
        if (sourcePlan.createsVehicleConnection) VehicleConnection(context) else null

    init {
        // 주행 상태와 길안내가 모두 Fake일 때만: 안내를 시작하면 출발하고, 도착하면 주차하는 시연 흐름을 만든다.
        if (sourcePlan.runsTripScenario) {
            FakeTripScenario(applicationScope, guidanceSource, fakeDrivingStateSource).start()
        }
    }

    /**
     * 시연 모드의 길안내 진행 조건: 주행(DRIVING) 중인지. UNKNOWN은 진행하지 않는다.
     *
     * Real 주행 상태는 메인 스레드에서 구독해야 해서(차량 연결 콜백) `flowOn(Main)`을 붙인다.
     * Fake 길안내의 진행은 [applicationScope](Default)에서 돈다.
     * 안내 중에는 화면(`HomeViewModel`)과 이 조건이 각자 차량에 연결한다 (docs/plans 007 "설계 결정 3").
     */
    private fun isDrivingFlow(): Flow<Boolean> {
        val isDriving = drivingStateSource.drivingState.map { it == DrivingState.DRIVING }
        return if (sourcePlan.driving == SourceKind.REAL) isDriving.flowOn(Dispatchers.Main) else isDriving
    }
}
