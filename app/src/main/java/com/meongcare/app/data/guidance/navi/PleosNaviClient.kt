package com.meongcare.app.data.guidance.navi

import ai.pleos.playground.navi.constants.NaviErrorCode
import ai.pleos.playground.navi.data.CurrentLocationInfo
import ai.pleos.playground.navi.data.DestinationArrivedInfo
import ai.pleos.playground.navi.data.DrivingInfo
import ai.pleos.playground.navi.data.RouteInfo
import ai.pleos.playground.navi.data.RouteStartInfo
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import android.content.Context
import com.meongcare.app.domain.guidance.Destination
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Pleos NaviHelper 어댑터. **NaviHelper를 부르는 유일한 파일**이다 (AGENTS.md 아키텍처 규칙).
 *
 * - 안내 한 번마다 새 `NaviHelper`를 만들고, 끝나면 `release()`한다. 내비 앱이 앞에 나와 우리 앱이 가려져도
 *   이벤트를 받아야 해서, 화면 수명이 아니라 안내 수명에 맞췄다.
 * - 순서는 Pleos 에뮬레이터에서 확인한 대로다: 리스너 등록 → `initialize(onConnected, onFailed)` → 연결되면 `requestRoute`.
 * - 좌표 + 이름만으로 경로를 요청한다 (`poiId`·`address`·`poiSubId`는 빈 문자열). 에뮬레이터에서 됐다 (`[obs-navi-emu]`).
 * - 콜백은 binder 스레드로 온다. `trySend`만 하므로 스레드를 바꾸지 않는다.
 * - 내비에 이전 경로가 남아 있으면 `requestRoute` 뒤에 그 경로의 `onRouteCancelled`가 먼저 온다. 그래서 `onRouteStarted` 전의
 *   취소는 보내지 않는다. [NaviEvent.Cancelled]는 언제나 우리가 요청한 경로가 취소됐다는 뜻이다.
 * - 권한: `NAVI_ROUTE_SEARCH`(경로 요청·취소), `NAVI_ROUTE`(조회). 팀 승인 2026-11-03.
 * - 차량 기기에서만 만든다 (`naviUseOf`). 일반 폰에는 내비 서비스가 없다.
 * - 현재 위치 한 번([currentLocation], 011)도 같은 방식이다: 물을 때마다 새 `NaviHelper`, 답을 받으면 `release()`. 경로는 건드리지 않는다.
 */
class PleosNaviClient(context: Context) : NaviClient, NaviLocationClient {
    private val appContext = context.applicationContext

    override fun trip(destination: Destination): Flow<NaviEvent> = callbackFlow {
        val helper = NaviHelper(appContext)
        val routeStarted = AtomicBoolean(false)
        val finished = AtomicBoolean(false)
        val listener = object : NaviHelperEventListener {
            override fun onRouteStarted(routeStartInfo: RouteStartInfo) {
                routeStarted.set(true)
                trySend(NaviEvent.RouteStarted(routeStartInfo.duration))
            }

            override fun onDrivingInfo(drivingInfo: DrivingInfo) {
                trySend(NaviEvent.Remaining(drivingInfo.destination.duration))
            }

            override fun onDestinationArrived(destinationArrivedInfo: DestinationArrivedInfo) {
                finished.set(true)
                trySend(NaviEvent.Arrived)
            }

            override fun onRouteCancelled() {
                // 우리 경로가 시작되기 전의 취소는 내비에 남아 있던 이전 경로의 것이다 (`[obs-navi-emu]`)
                if (!routeStarted.get()) return
                finished.set(true)
                trySend(NaviEvent.Cancelled)
            }

            override fun onError(errorCode: NaviErrorCode) {
                trySend(NaviEvent.Failed(errorCode.name))
            }
        }
        try {
            helper.addListener(listener)
            helper.initialize(
                onConnected = {
                    try {
                        helper.requestRoute(
                            RouteInfo(
                                latitude = destination.latitude,
                                longitude = destination.longitude,
                                poiName = destination.name,
                                poiId = "",
                                address = destination.address.orEmpty(),
                                poiSubId = "",
                            ),
                        )
                    } catch (e: Exception) {
                        trySend(NaviEvent.Failed(e.javaClass.simpleName))
                    }
                },
                onFailed = { error -> trySend(NaviEvent.Failed(error.javaClass.simpleName)) },
            )
        } catch (e: Exception) {
            trySend(NaviEvent.Failed(e.javaClass.simpleName))
        }
        awaitClose {
            // 도착·취소 전에 끊기면(앱에서 취소, 시연 도착) 내비 경로도 끝낸다
            runCatching { if (!finished.get()) helper.cancelRoute() }
            runCatching { helper.removeListener(listener) }
            runCatching { helper.release() }
        }
    }

    /**
     * 현재 위치를 한 번 묻는다 (권한 `NAVI_ROUTE`). 사실: docs/pleos/sdk-notes.md `[aar-navi]` "현재 위치".
     * 리스너 등록 → `initialize` → 연결되면 `getCurrentLocationInfo()` → `onCurrentLocationInfo`로 답이 온다.
     * 구독을 끊으면(답을 받음, 시간 초과, 취소) 리스너를 떼고 release한다. 안내 중일 수 있어서 `cancelRoute`는 부르지 않는다.
     * 로그를 남기지 않는다. 좌표가 로그에 들어갈 길을 만들지 않으려는 것이다.
     */
    override fun currentLocation(): Flow<NaviLocationEvent> = callbackFlow {
        val helper = NaviHelper(appContext)
        val closed = AtomicBoolean(false)
        val listener = object : NaviHelperEventListener {
            override fun onCurrentLocationInfo(currentLocationInfo: CurrentLocationInfo) {
                // javap로는 address의 null 여부가 보이지 않아서 null일 수 있는 값으로 받는다
                val address: String? = currentLocationInfo.address
                trySend(NaviLocationEvent.Located(currentLocationInfo.latitude, currentLocationInfo.longitude, address))
            }

            override fun onError(errorCode: NaviErrorCode) {
                trySend(NaviLocationEvent.Failed(errorCode.name))
            }
        }
        try {
            helper.addListener(listener)
            helper.initialize(
                onConnected = {
                    // 해제한 뒤에 늦게 온 연결 알림이면 아무것도 묻지 않는다
                    if (!closed.get()) {
                        try {
                            helper.getCurrentLocationInfo()
                        } catch (e: Exception) {
                            trySend(NaviLocationEvent.Failed(e.javaClass.simpleName))
                        }
                    }
                },
                onFailed = { error -> trySend(NaviLocationEvent.Failed(error.javaClass.simpleName)) },
            )
        } catch (e: Exception) {
            trySend(NaviLocationEvent.Failed(e.javaClass.simpleName))
        }
        awaitClose {
            closed.set(true)
            runCatching { helper.removeListener(listener) }
            runCatching { helper.release() }
        }
    }
}
