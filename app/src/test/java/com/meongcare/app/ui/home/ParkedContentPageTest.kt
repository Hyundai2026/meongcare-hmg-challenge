package com.meongcare.app.ui.home

import com.meongcare.app.domain.card.CardSessionState
import com.meongcare.app.domain.card.Symptom
import com.meongcare.app.domain.trip.FinderStep
import com.meongcare.app.ui.guidance.GuidanceUiState
import org.junit.Assert.assertEquals
import org.junit.Test

class ParkedContentPageTest {
    private val idle = GuidanceUiState.Idle("병원")
    private val ready = CardSessionState.Ready(setOf(Symptom.COUGH))

    @Test
    fun `쪽이 없으면 지금과 같다`() {
        assertEquals(ParkedContent.HOME, parkedContentOf(idle, CardSessionState.None, page = HomePage.NONE))
        assertEquals(ParkedContent.CARD, parkedContentOf(idle, ready, page = HomePage.NONE))
    }

    @Test
    fun `반려동물 카드와 릴렉스는 처음 화면·카드·병원 찾기보다 먼저다`() {
        assertEquals(ParkedContent.PET_CARD, parkedContentOf(idle, CardSessionState.None, page = HomePage.PET_CARD))
        assertEquals(ParkedContent.RELAX, parkedContentOf(idle, ready, atHome = true, page = HomePage.RELAX))
        assertEquals(ParkedContent.PET_CARD, parkedContentOf(idle, ready, finder = FinderStep.Hospitals, page = HomePage.PET_CARD))
        assertEquals(ParkedContent.RELAX, parkedContentOf(GuidanceUiState.Guiding("병원", 5), ready, page = HomePage.RELAX))
    }

    @Test
    fun `증상 고르기와 연결 화면이 쪽보다 먼저다`() {
        assertEquals(ParkedContent.SYMPTOMS, parkedContentOf(idle, CardSessionState.Picking(emptySet(), null), page = HomePage.RELAX))
        assertEquals(ParkedContent.LINK, parkedContentOf(idle, CardSessionState.None, linking = true, page = HomePage.PET_CARD))
    }
}

class ParkedContentHomeFinderTest {
    private val idle = GuidanceUiState.Idle("병원")
    private val choice = com.meongcare.app.ui.hospital.DemoHospitals.all.first().row

    @Test
    fun `카드 없이 연 병원 찾기는 카드 세션과 상관없이 목록·확인이다`() {
        assertEquals(ParkedContent.HOSPITALS, parkedContentOf(idle, CardSessionState.None, finder = FinderStep.Hospitals, finderWithoutCard = true))
        assertEquals(
            ParkedContent.CONFIRM_HOSPITAL,
            parkedContentOf(idle, CardSessionState.None, atHome = true, finder = FinderStep.Confirm(choice), finderWithoutCard = true),
        )
    }

    @Test
    fun `표시가 없으면 지금처럼 처음 화면이다`() {
        assertEquals(ParkedContent.HOME, parkedContentOf(idle, CardSessionState.None, finder = FinderStep.Hospitals))
    }

    @Test
    fun `안내 중이면 카드다`() {
        assertEquals(
            ParkedContent.CARD,
            parkedContentOf(GuidanceUiState.Guiding("병원", 5), CardSessionState.None, finder = FinderStep.Hospitals, finderWithoutCard = true),
        )
    }

    @Test
    fun `증상 고르기와 쪽이 먼저다`() {
        assertEquals(
            ParkedContent.SYMPTOMS,
            parkedContentOf(idle, CardSessionState.Picking(emptySet(), null), finder = FinderStep.Hospitals, finderWithoutCard = true),
        )
        assertEquals(
            ParkedContent.RELAX,
            parkedContentOf(idle, CardSessionState.None, finder = FinderStep.Hospitals, page = HomePage.RELAX, finderWithoutCard = true),
        )
    }
}
