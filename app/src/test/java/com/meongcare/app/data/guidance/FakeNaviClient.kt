package com.meongcare.app.data.guidance

import com.meongcare.app.data.guidance.navi.NaviClient
import com.meongcare.app.data.guidance.navi.NaviEvent
import com.meongcare.app.domain.guidance.Destination
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.receiveAsFlow

/** 시험용 내비. 요청한 목적지를 적고, 시험이 [send]로 이벤트를 넣는다. 구독이 끊기면 [closedTrips]가 는다. */
class FakeNaviClient : NaviClient {
    val requested = mutableListOf<Destination>()
    var closedTrips = 0
        private set
    private var events = Channel<NaviEvent>(Channel.UNLIMITED)

    override fun trip(destination: Destination): Flow<NaviEvent> {
        requested += destination
        val channel = Channel<NaviEvent>(Channel.UNLIMITED)
        events = channel
        return channel.receiveAsFlow().onCompletion { closedTrips++ }
    }

    fun send(event: NaviEvent) {
        events.trySend(event)
    }
}
