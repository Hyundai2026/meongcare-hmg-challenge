package com.meongcare.app.data.guidance

import com.meongcare.app.data.guidance.navi.NaviClient
import com.meongcare.app.data.guidance.navi.NaviEvent
import com.meongcare.app.data.guidance.navi.RouteOnClose
import com.meongcare.app.domain.guidance.Destination
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * 시험용 내비 (013 U2). 2인자 [trip]을 구현해, 구독이 끊길 때 `onClose()`가 무엇을 돌려줬는지 안내마다 기록한다.
 * [PleosNaviClient]처럼 도착·취소 이벤트로 이미 끝난 안내는 `onClose()`를 묻지 않는다.
 */
class CloseRecordingNaviClient : NaviClient {

    class Trip(val destination: Destination) {
        private val events = Channel<NaviEvent>(Channel.UNLIMITED)

        /** 구독을 끊을 때 고른 것. 아직 끊지 않았거나 이벤트로 끝났으면 null */
        var closedWith: RouteOnClose? = null
            internal set

        var isSubscribed = false
            private set

        internal fun subscribe(onClose: () -> RouteOnClose): Flow<NaviEvent> = flow {
            isSubscribed = true
            var finished = false
            try {
                for (event in events) {
                    if (event == NaviEvent.Arrived || event == NaviEvent.Cancelled) finished = true
                    emit(event)
                }
            } finally {
                isSubscribed = false
                if (!finished) closedWith = onClose()
            }
        }

        fun send(event: NaviEvent) {
            events.trySend(event)
        }
    }

    val trips = mutableListOf<Trip>()

    override fun trip(destination: Destination): Flow<NaviEvent> = trip(destination) { RouteOnClose.CANCEL }

    override fun trip(destination: Destination, onClose: () -> RouteOnClose): Flow<NaviEvent> =
        Trip(destination).also { trips += it }.subscribe(onClose)
}
