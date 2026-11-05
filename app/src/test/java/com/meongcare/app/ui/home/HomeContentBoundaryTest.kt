package com.meongcare.app.ui.home

import com.meongcare.app.ui.guidance.GuidanceUiState
import org.junit.Assert.assertEquals
import org.junit.Test

/** 계획 005 결정 4 "화면 조합" 표의 모든 칸. */
class HomeContentBoundaryTest {

    @Test
    fun `화면 조합 표의 모든 칸이 계획과 같다`() {
        val idle = GuidanceUiState.Idle("병원")
        val findingRoute = GuidanceUiState.Guiding("병원", remainingMinutes = null)
        val guiding = GuidanceUiState.Guiding("병원", remainingMinutes = 1)
        val arrived = GuidanceUiState.Arrived("병원")

        val table = listOf(
            Triple(HomeUiState.Parked, idle, HomeContent.PARKED),
            Triple(HomeUiState.Parked, findingRoute, HomeContent.PARKED),
            Triple(HomeUiState.Parked, guiding, HomeContent.PARKED),
            Triple(HomeUiState.Parked, arrived, HomeContent.ARRIVAL),
            // 주행·모름(HomeUiState.Driving): 안내 상태와 상관없이 주행 화면. 도착해도 진료 카드는 정차 뒤에.
            Triple(HomeUiState.Driving, idle, HomeContent.DRIVING),
            Triple(HomeUiState.Driving, findingRoute, HomeContent.DRIVING),
            Triple(HomeUiState.Driving, guiding, HomeContent.DRIVING),
            Triple(HomeUiState.Driving, arrived, HomeContent.DRIVING),
        )

        for ((home, guidance, expected) in table) {
            assertEquals("$home + $guidance", expected, homeContentOf(home, guidance))
        }
    }
}
