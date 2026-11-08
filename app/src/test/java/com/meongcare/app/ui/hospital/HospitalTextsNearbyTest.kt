package com.meongcare.app.ui.hospital

import org.junit.Assert.assertEquals
import org.junit.Test

/** 계획 015 "설계 결정 8"의 근처 묶음 문구 고르기. */
class HospitalTextsNearbyTest {

    @Test
    fun `상태마다 묶음 모양이 표와 같다`() {
        assertEquals(NearbySection.HIDDEN, nearbySectionOf(NearbyState.Off))
        assertEquals(NearbySection.NEEDS_LOCATION, nearbySectionOf(NearbyState.NeedsLocation))
        assertEquals(NearbySection.SEARCHING, nearbySectionOf(NearbyState.Searching))
        assertEquals(NearbySection.FOUND, nearbySectionOf(NearbyState.Found(emptyList())))
        assertEquals(NearbySection.EMPTY, nearbySectionOf(NearbyState.Empty))
        assertEquals(NearbySection.FAILED, nearbySectionOf(NearbyState.Failed()))
        assertEquals(NearbySection.RETRYING, nearbySectionOf(NearbyState.Failed(isRetrying = true)))
    }

    @Test
    fun `위치를 보내지 않았으면 안내가 없다`() {
        listOf(true, false).forEach { sample ->
            assertEquals(emptyList<NearbyNotice>(), nearbyNoticesOf(NearbyState.Off, sample))
            assertEquals(emptyList<NearbyNotice>(), nearbyNoticesOf(NearbyState.NeedsLocation, sample))
        }
    }

    @Test
    fun `가짜면 시연용만, 실제 검색이면 위치 전송과 출처를 알린다`() {
        val states = listOf(NearbyState.Searching, NearbyState.Found(emptyList()), NearbyState.Empty, NearbyState.Failed())
        states.forEach { state ->
            assertEquals(listOf(NearbyNotice.SAMPLE), nearbyNoticesOf(state, isSampleSource = true))
            assertEquals(
                listOf(NearbyNotice.SENDS_LOCATION, NearbyNotice.SOURCE_KAKAO),
                nearbyNoticesOf(state, isSampleSource = false),
            )
        }
    }
}
