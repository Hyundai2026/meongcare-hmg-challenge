package com.meongcare.app.data.guidance.navi

import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds

/**
 * 내비가 안내를 시작하기(`RouteStarted`)까지 기다리는 시간.
 *
 * 경로 계산과 "안내 시작" 카운트다운이 약 3초다. Pleos 에뮬레이터에서는 지도를 불러오느라 10~20초가 걸린 적이 있다 (010 진행 로그).
 */
val NAVI_ROUTE_START_TIMEOUT: Duration = 30.seconds

private val MAX_REMAINING: Duration = 1.days

/**
 * 내비가 알려 준 남은 초(`RouteStartInfo.duration`, `DrivingInfo.destination.duration`)를 남은 시간으로 바꾼다.
 *
 * 0 이하나 하루보다 긴 값은 모름(null)이다 (010 설계 결정 3). 내비는 경로를 취소하기 직전에 0을 보낸다 (`[obs-navi-emu]`).
 */
fun naviRemainingTimeOf(seconds: Int): Duration? =
    seconds.seconds.takeIf { it.isPositive() && it <= MAX_REMAINING }
