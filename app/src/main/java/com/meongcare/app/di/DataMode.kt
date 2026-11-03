package com.meongcare.app.di

/** 데이터를 가짜(Fake)에서 받을지, 실제 차량(Real)에서 받을지. */
enum class DataMode {
    /** 가짜 데이터. GPU 없는 PC 개발, 테스트, 시연 영상용. debug 빌드의 기본값이다. */
    FAKE,

    /** 실제 차량(또는 Pleos 에뮬레이터). release 빌드는 항상 이 모드다. */
    REAL,
}
