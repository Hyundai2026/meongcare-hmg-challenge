package com.meongcare.app.di

/** 데이터를 가짜(Fake)에서 받을지, 실제 차량(Real)에서 받을지. */
enum class DataMode {
    /** 가짜 데이터. GPU 없는 PC 개발, 테스트, 시연 영상용. debug 빌드의 기본값이다. */
    FAKE,

    /**
     * 시연. 제출 APK로 핵심 흐름을 보여 주기 위한 모드다.
     * - 주행 상태: 차량 기기(`FEATURE_AUTOMOTIVE`)면 Real(주행 규제), 아니면 Fake + 출발·주차 시나리오.
     * - 길안내: 시연용 Fake (Pleos NaviHelper 연동 전). 주행 중일 때만 남은 시간이 준다.
     * 화면에 시연용 길안내라고 알린다. 구성표는 [sourcePlanOf].
     * **release 빌드는 이 모드다** (Real 길안내가 생기면 다시 정한다).
     */
    DEMO,

    /** 실제 차량(또는 Pleos 에뮬레이터). 길안내는 아직 자리표시라서 debug에서만 고른다. */
    REAL,
}

/**
 * 프로세스가 시작될 때 쓸 데이터 모드를 정한다. Android를 모르는 순수 함수다.
 *
 * - debug 빌드: 저장된 이름이 [DataMode] 값이면 그것, 없거나 모르는 값이면 [DataMode.FAKE].
 * - release 빌드: 저장된 값과 상관없이 [DataMode.DEMO]. 제출 APK로 핵심 흐름을 보여 주기 위해서다.
 *   Real 길안내(NaviHelper)가 생기면 다시 정한다.
 *
 * @param savedName debug 패널에서 골라 저장한 모드 이름
 */
/** debug 패널에서 누를 때마다 바뀌는 순서: Fake → 시연 → Real → Fake. */
fun DataMode.next(): DataMode = when (this) {
    DataMode.FAKE -> DataMode.DEMO
    DataMode.DEMO -> DataMode.REAL
    DataMode.REAL -> DataMode.FAKE
}

fun resolveDataMode(isDebugBuild: Boolean, savedName: String?): DataMode {
    if (!isDebugBuild) return DataMode.DEMO
    return DataMode.entries.firstOrNull { it.name == savedName } ?: DataMode.FAKE
}
