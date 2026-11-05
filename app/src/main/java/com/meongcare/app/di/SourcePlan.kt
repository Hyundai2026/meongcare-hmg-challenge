package com.meongcare.app.di

/** 데이터 소스를 가짜로 끼울지 진짜로 끼울지. */
enum class SourceKind { FAKE, REAL }

/**
 * 데이터 모드에 따라 어떤 구현을 끼울지 정리한 표의 한 줄. [AppContainer]가 이 값대로 만든다.
 * Android를 모르는 값이라 JVM 테스트로 표 전체를 확인할 수 있다 (docs/plans 007 "설계 결정 1").
 */
data class SourcePlan(
    /** 주행 상태 */
    val driving: SourceKind,
    /** 길안내 */
    val guidance: SourceKind,
    /** `FakeTripScenario`로 Fake 주행 상태를 길안내에 맞춰 바꾼다 (주행 상태와 길안내가 모두 Fake일 때만). */
    val runsTripScenario: Boolean,
    /** Fake 길안내가 주행(DRIVING) 중에만 남은 시간을 줄인다. */
    val guidanceWaitsForDriving: Boolean,
    /** Pleos Vehicle 연결(`VehicleConnection`)을 만든다. */
    val createsVehicleConnection: Boolean,
) {
    /** 길안내가 시연용(가짜)이라서 화면에 그 사실을 알려야 하는지. */
    val isGuidanceSimulated: Boolean get() = guidance == SourceKind.FAKE
}

/**
 * 데이터 모드와 기기 종류로 [SourcePlan]을 정한다.
 *
 * @param isAutomotive 기기에 `PackageManager.FEATURE_AUTOMOTIVE`가 있는지 ([AppContainer]가 구해서 넘긴다)
 */
@Suppress("UNUSED_PARAMETER") // 시연 모드에서 쓴다 (다음 커밋)
fun sourcePlanOf(mode: DataMode, isAutomotive: Boolean): SourcePlan = when (mode) {
    DataMode.FAKE -> SourcePlan(
        driving = SourceKind.FAKE,
        guidance = SourceKind.FAKE,
        runsTripScenario = true,
        guidanceWaitsForDriving = false,
        createsVehicleConnection = false,
    )
    DataMode.REAL -> SourcePlan(
        driving = SourceKind.REAL,
        guidance = SourceKind.REAL,
        runsTripScenario = false,
        guidanceWaitsForDriving = false,
        createsVehicleConnection = true,
    )
}
