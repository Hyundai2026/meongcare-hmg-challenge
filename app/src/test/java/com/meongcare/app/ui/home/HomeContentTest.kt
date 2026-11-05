package com.meongcare.app.ui.home

import com.meongcare.app.ui.guidance.GuidanceUiState
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeContentTest {

    private val idle = GuidanceUiState.Idle("병원")
    private val guiding = GuidanceUiState.Guiding("병원", remainingMinutes = 5)
    private val findingRoute = GuidanceUiState.Guiding("병원", remainingMinutes = null)
    private val arrived = GuidanceUiState.Arrived("병원")

    @Test
    fun `주행 중에는 도착해도 도착 화면 대신 주행 화면이다`() {
        assertEquals(HomeContent.DRIVING, homeContentOf(HomeUiState.Driving, arrived))
        assertEquals(HomeContent.DRIVING, homeContentOf(HomeUiState.Driving, guiding))
        assertEquals(HomeContent.DRIVING, homeContentOf(HomeUiState.Driving, idle))
    }

    @Test
    fun `정차 중에 도착 상태면 도착 화면이다`() {
        assertEquals(HomeContent.ARRIVAL, homeContentOf(HomeUiState.Parked, arrived))
    }

    @Test
    fun `정차 중에 안내 중이거나 안내가 없으면 정차 화면이다`() {
        assertEquals(HomeContent.PARKED, homeContentOf(HomeUiState.Parked, idle))
        assertEquals(HomeContent.PARKED, homeContentOf(HomeUiState.Parked, guiding))
        assertEquals(HomeContent.PARKED, homeContentOf(HomeUiState.Parked, findingRoute))
    }
}
