package com.meongcare.app.domain.driving

/** 차가 지금 주행 중인지, 정차·주차 중인지. 화면을 고르는 기준이다. */
enum class DrivingState {
    /** 정차·주차 중. 일반 기능과 텍스트 입력을 쓸 수 있다. */
    PARKED,

    /** 주행 중. 최소 정보, 큰 터치 타깃, 텍스트 입력 없음 (AGENTS.md 규칙 3). */
    DRIVING,

    /** 아직 모름. 안전을 위해 주행 중처럼 다룬다. */
    UNKNOWN,
}
