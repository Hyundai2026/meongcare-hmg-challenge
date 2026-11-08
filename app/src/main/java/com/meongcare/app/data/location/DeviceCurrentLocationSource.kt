package com.meongcare.app.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal
import com.meongcare.app.domain.location.CurrentLocation
import com.meongcare.app.domain.location.CurrentLocationSource
import kotlin.coroutines.resume
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 기기의 안드로이드 위치(GPS·네트워크)로 현재 위치를 한 번 묻는다 (계획 015, 사용자 결정 2026-11-08).
 *
 * 차량 기기가 아닐 때(일반 폰·태블릿 에뮬레이터)의 시연·Real 모드에서 쓴다. 차량 기기는 내비 위치를 쓴다.
 * - 위치 권한이 없으면 묻지 않고 null이다 (권한 창은 화면이 띄운다).
 * - [timeout] 안에 새 위치가 없으면 [maxLastKnownAge]보다 새 마지막 위치를 쓴다. 그것도 없으면 null.
 *   다른 위치(시연 위치)로 대신하지 않는다 (011 규칙).
 * - Google Play 서비스(FusedLocation) 없이 `LocationManager`만 쓴다 (새 의존성 없음).
 * - 로그를 남기지 않는다 (좌표).
 */
class DeviceCurrentLocationSource(
    private val context: Context,
    private val timeout: Duration = 5.seconds,
    private val maxLastKnownAge: Duration = 10.minutes,
) : CurrentLocationSource {

    override suspend fun current(): CurrentLocation? {
        if (!hasPermission(context)) return null
        val manager = context.getSystemService(LocationManager::class.java) ?: return null
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .filter { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }
        if (providers.isEmpty()) return null
        val fresh = withTimeoutOrNull(timeout) { currentFrom(manager, providers.first()) }
        return (fresh ?: lastKnown(manager, providers))?.toCurrentLocation()
    }

    @SuppressLint("MissingPermission") // hasPermission으로 확인했다
    private suspend fun currentFrom(manager: LocationManager, provider: String): Location? =
        suspendCancellableCoroutine { continuation ->
            val signal = CancellationSignal()
            continuation.invokeOnCancellation { signal.cancel() }
            try {
                LocationManagerCompat.getCurrentLocation(manager, provider, signal, ContextCompat.getMainExecutor(context)) { location ->
                    if (continuation.isActive) continuation.resume(location)
                }
            } catch (_: SecurityException) {
                if (continuation.isActive) continuation.resume(null)
            }
        }

    @SuppressLint("MissingPermission")
    private fun lastKnown(manager: LocationManager, providers: List<String>): Location? {
        val now = SystemClock.elapsedRealtimeNanos()
        return providers
            .mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }
            .filter { now - it.elapsedRealtimeNanos <= maxLastKnownAge.inWholeNanoseconds }
            .maxByOrNull { it.elapsedRealtimeNanos }
    }

    private fun Location.toCurrentLocation(): CurrentLocation? {
        if (!latitude.isFinite() || latitude !in -90.0..90.0) return null
        if (!longitude.isFinite() || longitude !in -180.0..180.0) return null
        return CurrentLocation(latitude = latitude, longitude = longitude, address = null)
    }

    companion object {
        /** 화면이 요청할 권한 */
        val PERMISSIONS = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

        fun hasPermission(context: Context): Boolean = PERMISSIONS.any {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }
}
