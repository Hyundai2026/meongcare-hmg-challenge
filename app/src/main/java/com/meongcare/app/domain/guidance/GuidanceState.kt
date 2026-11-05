package com.meongcare.app.domain.guidance

import kotlin.time.Duration

/** 길안내가 지금 어떤 상태인지. */
sealed interface GuidanceState {

    /** 안내 중이 아니다. */
    data object Idle : GuidanceState

    /**
     * [destination]으로 안내 중이다.
     *
     * @param remainingTime 도착까지 남은 시간. `null`이면 경로를 찾는 중이라 아직 모른다.
     *   [Duration]으로 두는 이유: NaviHelper `TripInfo.eta`의 단위(초/분)와 뜻(남은 시간/도착 시각)을
     *   아직 모른다 (docs/pleos/sdk-notes.md). 변환은 Real 구현 안에서만 한다.
     */
    data class Guiding(val destination: Destination, val remainingTime: Duration?) : GuidanceState

    /**
     * [destination]에 도착했다.
     *
     * 도착은 한 번 오는 이벤트지만, 화면은 사용자가 닫을 때까지 도착 화면을 유지해야 해서 상태로 둔다.
     * [GuidanceSource.dismissArrival]로 끝낸다.
     */
    data class Arrived(val destination: Destination) : GuidanceState
}
