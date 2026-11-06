package com.meongcare.app.data.location

import com.meongcare.app.data.guidance.navi.NaviLocationClient
import com.meongcare.app.data.guidance.navi.NaviLocationEvent
import com.meongcare.app.domain.location.CurrentLocation
import com.meongcare.app.domain.location.CurrentLocationSource
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 차량 내비(NaviHelper)에 현재 위치를 묻는다 (011).
 *
 * - 물을 때마다 [client]를 새로 구독한다. 첫 결과를 받거나 [timeout]이 지나면 구독을 끊는다. 그러면 어댑터가 연결을 놓는다.
 * - 한 번에 하나만 묻는다. 위치를 묻는 NaviHelper가 동시에 둘 생기지 않게 하려는 것이다.
 *   두 번째 호출은 첫 호출이 끝난 뒤 새로 묻는다.
 * - 로그는 한 번 물을 때 한 줄이다. **좌표와 주소는 넣지 않는다** (개인정보). 결과 종류와 걸린 시간만 남긴다.
 */
class NaviCurrentLocationSource(
    private val client: NaviLocationClient,
    private val timeout: Duration = DEFAULT_TIMEOUT,
    private val log: (String) -> Unit = {},
    private val timeSource: TimeSource = TimeSource.Monotonic,
) : CurrentLocationSource {

    private val mutex = Mutex()

    override suspend fun current(): CurrentLocation? = mutex.withLock {
        val started = timeSource.markNow()
        fun elapsed() = "${started.elapsedNow().inWholeMilliseconds}ms"

        val event = try {
            withTimeoutOrNull(timeout) { client.currentLocation().first() }
                ?: run {
                    log("현재 위치: 시간 초과 (${timeout.inWholeMilliseconds}ms)")
                    return@withLock null
                }
        } catch (e: CancellationException) {
            // 부른 쪽이 취소됐으면 그대로 전한다. 아니면 내비 쪽에서 난 취소라서 모름으로 본다.
            currentCoroutineContext().ensureActive()
            log("현재 위치: 오류 (${e.javaClass.simpleName}, ${elapsed()})")
            return@withLock null
        } catch (e: NoSuchElementException) {
            log("현재 위치: 응답 없음 (${elapsed()})")
            return@withLock null
        } catch (e: Exception) {
            log("현재 위치: 오류 (${e.javaClass.simpleName}, ${elapsed()})")
            return@withLock null
        }

        when (event) {
            is NaviLocationEvent.Failed -> {
                log("현재 위치: 실패 (${event.reason}, ${elapsed()})")
                null
            }
            is NaviLocationEvent.Located -> {
                val location = currentLocationOf(event.latitude, event.longitude, event.address)
                when {
                    location == null -> log("현재 위치: 값 이상 (${elapsed()})")
                    location.address == null -> log("현재 위치: 성공 (주소 없음, ${elapsed()})")
                    else -> log("현재 위치: 성공 (주소 있음, ${elapsed()})")
                }
                location
            }
        }
    }

    companion object {
        /**
         * 위치를 기다리는 최대 시간.
         * 010 spike에서 연결(`onConnected`)은 약 120ms에 왔다. 위치 응답 시간은 011 단계 6에서 잰다.
         * 병원 검색 전에 한 번 묻는 용도라, 사람이 기다릴 수 있는 범위에서 넉넉하게 잡았다.
         */
        val DEFAULT_TIMEOUT: Duration = 5.seconds
    }
}

/**
 * 내비가 준 값을 앱의 [CurrentLocation]으로 바꾼다. 쓸 수 없는 값이면 null(모름)이다.
 * 아래는 SDK 사실이 아니라 우리 규칙이다.
 * - 위도·경도가 NaN이나 무한대, 위도가 [-90, 90] 밖, 경도가 [-180, 180] 밖이면 null. 경계값은 받는다.
 * - (0, 0)이면 null. 흔히 "위치 없음"으로 쓰는 값이고, 차가 있을 수 없는 바다 위다.
 * - 주소: null, 빈 문자열, 공백뿐이면 null. 아니면 앞뒤 공백을 자른다.
 *   javap로는 SDK `getAddress()`의 null 여부가 보이지 않아서 어느 쪽이든 받는다.
 */
fun currentLocationOf(latitude: Double, longitude: Double, address: String?): CurrentLocation? {
    if (!latitude.isFinite() || !longitude.isFinite()) return null
    if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return null
    if (latitude == 0.0 && longitude == 0.0) return null
    return CurrentLocation(
        latitude = latitude,
        longitude = longitude,
        address = address?.trim()?.takeIf { it.isNotEmpty() },
    )
}
