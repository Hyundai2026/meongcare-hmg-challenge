package com.meongcare.app.domain.guidance

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * 안쪽 길안내에 [병원 도착](사용자가 직접 도착 처리, 디자인 시안 "병원으로 이동 중", 2026-11-14)을 더한다.
 * 안쪽 구현(Fake·내비)은 바꾸지 않는다.
 *
 * - [arriveNow]: 안내 중이면 그 목적지로 도착 상태를 먼저 보이고, 안쪽 안내는 취소한다 (도착을 먼저 세워서 "취소"가 보이지 않는다).
 * - [dismissArrival]: 직접 도착이면 그것을 지운다. 아니면 안쪽에 넘긴다.
 * - [start]: 직접 도착을 지우고 안쪽에서 새로 시작한다.
 */
class ManualArrivalGuidanceSource(private val inner: GuidanceSource) : GuidanceSource {

    private val manualArrival = MutableStateFlow<Destination?>(null)

    override val state: Flow<GuidanceState> =
        combine(inner.state, manualArrival) { s, manual -> manual?.let { GuidanceState.Arrived(it) } ?: s }
            .distinctUntilChanged()

    /** [병원 도착]. [current]가 안내 중일 때만 */
    suspend fun arriveNow(current: GuidanceState) {
        val destination = (current as? GuidanceState.Guiding)?.destination ?: return
        manualArrival.value = destination
        inner.cancel()
    }

    override suspend fun start(destination: Destination) {
        manualArrival.value = null
        inner.start(destination)
    }

    override suspend fun cancel() {
        if (manualArrival.value == null) inner.cancel()
    }

    override fun dismissArrival() {
        if (manualArrival.value != null) manualArrival.value = null else inner.dismissArrival()
    }
}
