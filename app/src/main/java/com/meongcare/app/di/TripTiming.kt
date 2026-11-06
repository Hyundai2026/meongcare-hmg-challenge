package com.meongcare.app.di

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * 출발한 뒤 병원과 주고받는 간격 (013). 순수 함수라 JVM으로 시험한다.
 *
 * @param etaInterval 도착 예정을 다시 보내는 최소 간격
 * @param ackPollInterval 병원 확인(카드 확인)을 읽는 간격 (013 U1)
 */
data class TripTiming(
    val etaInterval: Duration,
    val ackPollInterval: Duration,
)

/**
 * 길안내가 시연용인지로 간격을 정한다.
 *
 * - 시연용 (Fake·시연 모드, 경로만 내비 포함): 시연 진행은 2초에 1분씩 준다 (`FakeGuidanceTiming`).
 *   60초 간격이면 병원 화면이 "37분 → 8분 → 도착"으로 튀었다 (013 관찰 2). 4초(= 2분)마다 보낸다.
 * - 실제 (Real): 남은 시간이 1분에 1분씩 준다. 지금처럼 1분에 한 번까지.
 * - 병원 확인 읽기: 시연 4초(시연은 1~2분 안에 끝난다), 실제 15초 (사람이 누르는 것이라 충분하고 폴링을 줄인다).
 *
 * @param isGuidanceSimulated `SourcePlan.isGuidanceSimulated`
 */
fun tripTimingOf(isGuidanceSimulated: Boolean): TripTiming =
    if (isGuidanceSimulated) {
        TripTiming(etaInterval = 4.seconds, ackPollInterval = 4.seconds)
    } else {
        TripTiming(etaInterval = 60.seconds, ackPollInterval = 15.seconds)
    }
