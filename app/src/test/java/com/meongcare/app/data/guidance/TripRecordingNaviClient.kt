package com.meongcare.app.data.guidance

import com.meongcare.app.data.guidance.navi.NaviClient
import com.meongcare.app.data.guidance.navi.NaviEvent
import com.meongcare.app.domain.guidance.Destination
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * 시험용 내비. [NaviClient.trip]을 부를 때마다 안내 하나([Trip])를 따로 기록한다.
 *
 * [NaviClient] 계약: 구독하면 연결·경로 요청, 구독을 끊으면 경로 취소·연결 해제.
 * 그래서 "연결했는가"는 [Trip.subscriptions]로, "연결을 놓았는가"는 [Trip.isSubscribed]로 본다.
 * 시험은 안내마다 따로 이벤트를 넣을 수 있다 (목적지를 바꾼 뒤 이전 안내에서 늦게 온 이벤트 재현).
 */
class TripRecordingNaviClient : NaviClient {

    class Trip(val destination: Destination) {
        private val events = Channel<NaviEvent>(Channel.UNLIMITED)

        /** 이 안내를 몇 번 구독했는지 (= 내비에 몇 번 연결했는지) */
        var subscriptions = 0
            private set

        /** 지금 구독 중인 수 */
        var activeSubscriptions = 0
            private set

        val isSubscribed: Boolean get() = activeSubscriptions > 0

        val flow: Flow<NaviEvent> = flow {
            subscriptions++
            activeSubscriptions++
            try {
                for (event in events) emit(event)
            } finally {
                activeSubscriptions--
            }
        }

        fun send(event: NaviEvent) {
            events.trySend(event)
        }
    }

    /** [trip]을 부른 순서대로 */
    val trips = mutableListOf<Trip>()

    /** 모든 안내를 합친 지금 구독 수 */
    val activeSubscriptions: Int get() = trips.sumOf { it.activeSubscriptions }

    /** 모든 안내를 합친 구독 횟수 */
    val totalSubscriptions: Int get() = trips.sumOf { it.subscriptions }

    override fun trip(destination: Destination): Flow<NaviEvent> = Trip(destination).also { trips += it }.flow
}
