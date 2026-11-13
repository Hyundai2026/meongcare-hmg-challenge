package com.meongcare.app.ui.home

import com.meongcare.app.domain.card.CardSessionState
import com.meongcare.app.domain.card.Symptom
import com.meongcare.app.domain.trip.FinderStep
import com.meongcare.app.ui.guidance.GuidanceUiState
import com.meongcare.app.ui.hospital.DemoHospitals
import org.junit.Assert.assertEquals
import org.junit.Test

/** 계획 019 단계 4·5 `parkedContentOf`의 `page`·`finderWithoutCard` 경계, 주행 전환(AGENTS.md 규칙 3). */
class ParkedContentHomeFinderBoundaryTest {
    private val idle = GuidanceUiState.Idle("병원")
    private val guiding = GuidanceUiState.Guiding("병원", 5)
    private val routing = GuidanceUiState.Guiding("병원", null)
    private val arrived = GuidanceUiState.Arrived("병원")
    private val ready = CardSessionState.Ready(setOf(Symptom.COUGH))
    private val confirm = FinderStep.Confirm(DemoHospitals.all.first().row)

    @Test
    fun `카드 없이 연 병원 찾기는 카드가 있어도 목록·확인이다`() {
        assertEquals(ParkedContent.HOSPITALS, parkedContentOf(idle, ready, finder = FinderStep.Hospitals, finderWithoutCard = true))
        assertEquals(
            ParkedContent.CONFIRM_HOSPITAL,
            parkedContentOf(idle, ready, atHome = true, finder = confirm, finderWithoutCard = true),
        )
    }

    @Test
    fun `카드 없이 연 표시가 있어도 찾기 단계가 없으면 처음 화면이나 카드다`() {
        assertEquals(ParkedContent.HOME, parkedContentOf(idle, CardSessionState.None, finder = FinderStep.None, finderWithoutCard = true))
        assertEquals(ParkedContent.CARD, parkedContentOf(idle, ready, finder = FinderStep.None, finderWithoutCard = true))
    }

    @Test
    fun `경로를 찾는 중이거나 도착이면 카드 없이 연 병원 찾기 대신 카드다`() {
        assertEquals(ParkedContent.CARD, parkedContentOf(routing, CardSessionState.None, finder = FinderStep.Hospitals, finderWithoutCard = true))
        assertEquals(ParkedContent.CARD, parkedContentOf(arrived, CardSessionState.None, finder = confirm, finderWithoutCard = true))
        assertEquals(ParkedContent.CARD, parkedContentOf(guiding, CardSessionState.None, atHome = false, finder = confirm, finderWithoutCard = true))
    }

    @Test
    fun `연결 화면이 카드 없이 연 병원 찾기보다 먼저다`() {
        assertEquals(
            ParkedContent.LINK,
            parkedContentOf(idle, CardSessionState.None, finder = FinderStep.Hospitals, linking = true, finderWithoutCard = true),
        )
    }

    @Test
    fun `안내 중에는 연결 화면 대신 열어 둔 쪽이 보인다`() {
        assertEquals(ParkedContent.PET_CARD, parkedContentOf(guiding, CardSessionState.None, linking = true, page = HomePage.PET_CARD))
    }

    @Test
    fun `도착했어도 반려동물 카드·릴렉스를 열어 두었으면 그 쪽이다`() {
        assertEquals(ParkedContent.PET_CARD, parkedContentOf(arrived, CardSessionState.None, page = HomePage.PET_CARD))
        assertEquals(ParkedContent.RELAX, parkedContentOf(arrived, ready, page = HomePage.RELAX))
    }

    @Test
    fun `증상을 고르는 중이면 쪽과 병원 찾기 표시가 모두 있어도 증상 고르기다`() {
        assertEquals(
            ParkedContent.SYMPTOMS,
            parkedContentOf(
                guiding, CardSessionState.Picking(setOf(Symptom.COUGH), null),
                atHome = true, finder = confirm, linking = true, page = HomePage.PET_CARD, finderWithoutCard = true,
            ),
        )
    }

    @Test
    fun `주행 중이면 어느 쪽을 열어 두었든 주행 화면이고 정차하면 그 쪽으로 돌아온다`() {
        for (guidance in listOf(idle, guiding, arrived)) {
            assertEquals(HomeContent.DRIVING, homeContentOf(HomeUiState.Driving, guidance))
        }
        // 주행 → 정차: 쪽 상태는 화면 밖(MainActivity)에 남아 있으므로 같은 값으로 다시 고르면 같은 쪽이다
        assertEquals(HomeContent.PARKED, homeContentOf(HomeUiState.Parked, idle))
        assertEquals(ParkedContent.PET_CARD, parkedContentOf(idle, CardSessionState.None, page = HomePage.PET_CARD))
        assertEquals(ParkedContent.RELAX, parkedContentOf(idle, CardSessionState.None, page = HomePage.RELAX))
        assertEquals(
            ParkedContent.CONFIRM_HOSPITAL,
            parkedContentOf(idle, CardSessionState.None, finder = confirm, finderWithoutCard = true),
        )
    }
}
