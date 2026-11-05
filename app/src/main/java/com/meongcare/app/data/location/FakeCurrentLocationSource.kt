package com.meongcare.app.data.location

import com.meongcare.app.domain.location.CurrentLocation
import com.meongcare.app.domain.location.CurrentLocationSource

/**
 * 정해 둔 위치를 바로 돌려준다. Fake 모드와 일반 폰에서 쓴다.
 *
 * 기본값은 Pleos 에뮬레이터의 내비가 준 현재 위치와 같다 (docs/pleos/sdk-notes.md `[obs-navi-emu]`).
 * 그래서 두 에뮬레이터가 같은 위치를 쓴다. 사람의 실제 위치가 아니라 에뮬레이터 기본 위치다.
 */
class FakeCurrentLocationSource(
    /** null이면 항상 모름이다 (테스트용). */
    private val location: CurrentLocation? = DEMO_LOCATION,
) : CurrentLocationSource {

    override suspend fun current(): CurrentLocation? = location

    companion object {
        /** Pleos 에뮬레이터 내비의 현재 위치 (2026-11-05 010 spike에서 받은 값). */
        val DEMO_LOCATION = CurrentLocation(
            latitude = 37.4127212,
            longitude = 127.094681,
            address = "수정구 시흥동",
        )
    }
}
