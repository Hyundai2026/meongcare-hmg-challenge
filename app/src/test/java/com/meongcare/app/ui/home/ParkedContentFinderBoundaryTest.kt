package com.meongcare.app.ui.home

import com.meongcare.app.data.hospital.FakeCaseSender
import com.meongcare.app.domain.card.CardSessionState
import com.meongcare.app.domain.card.Symptom
import com.meongcare.app.domain.guidance.Destination
import com.meongcare.app.domain.guidance.GuidanceSource
import com.meongcare.app.domain.guidance.GuidanceState
import com.meongcare.app.domain.trip.FinderStep
import com.meongcare.app.domain.trip.HospitalTrip
import com.meongcare.app.ui.guidance.GuidanceUiState
import com.meongcare.app.ui.hospital.previewRanking
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 병원 찾기 단계(HOSPITALS, CONFIRM_HOSPITAL) 경계. 기준: `parkedContentOf` KDoc 표, 계획 012 "설계 결정 8"
 * (병원 목록은 정차 중에만, 1단계 규칙은 그대로), 완료 기준 10(목록·확인 화면에서 D → 주행 화면, P → 같은 화면),
 * AGENTS.md 규칙 3.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ParkedContentFinderBoundaryTest {

    private val idle = GuidanceUiState.Idle("시연용 동물병원")
    private val guiding = GuidanceUiState.Guiding("시연용 바른 동물병원", 12)
    private val arrived = GuidanceUiState.Arrived("시연용 바른 동물병원")
    private val choice = previewRanking().ranking.ranked.first()

    private val sessions = listOf(
        CardSessionState.None,
        CardSessionState.Picking(setOf(Symptom.COUGH), previous = null),
        CardSessionState.Ready(setOf(Symptom.COUGH)),
    )
    private val finders = listOf(FinderStep.None, FinderStep.Hospitals, FinderStep.Confirm(choice))

    /** KDoc 표를 그대로 옮긴 기대값 */
    private fun expected(guidance: GuidanceUiState, session: CardSessionState, atHome: Boolean, finder: FinderStep): ParkedContent = when {
        session is CardSessionState.Picking -> ParkedContent.SYMPTOMS
        atHome -> ParkedContent.HOME
        session is CardSessionState.Ready && guidance is GuidanceUiState.Idle -> when (finder) {
            FinderStep.None -> ParkedContent.CARD
            FinderStep.Hospitals -> ParkedContent.HOSPITALS
            is FinderStep.Confirm -> ParkedContent.CONFIRM_HOSPITAL
        }
        session is CardSessionState.Ready -> ParkedContent.CARD
        guidance is GuidanceUiState.Idle -> ParkedContent.HOME
        else -> ParkedContent.CARD
    }

    @Test
    fun `안내, 카드 세션, 홈, 찾기 단계의 모든 조합이 표와 같다`() {
        for (guidance in listOf(idle, guiding, arrived)) for (session in sessions) for (atHome in listOf(false, true)) for (finder in finders) {
            assertEquals(
                "$guidance / $session / atHome=$atHome / $finder",
                expected(guidance, session, atHome, finder),
                parkedContentOf(guidance, session, atHome, finder),
            )
        }
    }

    @Test
    fun `찾기 단계 화면은 카드가 있고 안내가 없을 때만 나온다`() {
        for (guidance in listOf(idle, guiding, arrived)) for (session in sessions) for (atHome in listOf(false, true)) for (finder in finders) {
            val content = parkedContentOf(guidance, session, atHome, finder)
            if (content == ParkedContent.HOSPITALS || content == ParkedContent.CONFIRM_HOSPITAL) {
                assertEquals(idle, guidance)
                assertEquals(CardSessionState.Ready(setOf(Symptom.COUGH)), session)
                assertEquals(false, atHome)
            }
        }
    }

    private class StillGuidance : GuidanceSource {
        val flow = MutableStateFlow<GuidanceState>(GuidanceState.Idle)
        override val state: StateFlow<GuidanceState> = flow
        override suspend fun start(destination: Destination) {
            flow.value = GuidanceState.Guiding(destination, null)
        }
        override suspend fun cancel() {
            flow.value = GuidanceState.Idle
        }
        override fun dismissArrival() = Unit
    }

    @Test
    fun `목록과 확인 화면에서 주행하면 주행 화면이고 다시 정차하면 같은 단계다`() = runTest {
        val trip = HospitalTrip(StillGuidance(), { FakeCaseSender() }, backgroundScope)
        val ready = CardSessionState.Ready(setOf(Symptom.COUGH))

        trip.openFinder()
        assertEquals(HomeContent.DRIVING, homeContentOf(HomeUiState.Driving, idle))
        assertEquals(HomeContent.PARKED, homeContentOf(HomeUiState.Parked, idle))
        assertEquals(ParkedContent.HOSPITALS, parkedContentOf(idle, ready, finder = trip.finder.value))

        trip.choose(choice)
        assertEquals(HomeContent.DRIVING, homeContentOf(HomeUiState.Driving, idle))
        assertEquals(HomeContent.PARKED, homeContentOf(HomeUiState.Parked, idle))
        assertEquals(ParkedContent.CONFIRM_HOSPITAL, parkedContentOf(idle, ready, finder = trip.finder.value))
    }

    @Test
    fun `출발하면 찾기 단계가 닫히고 안내 중 카드가 보인다`() = runTest {
        val guidance = StillGuidance()
        val trip = HospitalTrip(guidance, { FakeCaseSender() }, backgroundScope)
        val ready = CardSessionState.Ready(setOf(Symptom.COUGH))
        trip.openFinder()
        trip.choose(choice)
        trip.depart(choice, card = null)
        runCurrent()
        assertEquals(FinderStep.None, trip.finder.value)
        assertEquals(ParkedContent.CARD, parkedContentOf(guiding, ready, finder = trip.finder.value))
        // 주행 중에는 안내 상태와 상관없이 주행 화면이다
        assertEquals(HomeContent.DRIVING, homeContentOf(HomeUiState.Driving, guiding))
    }

    @Test
    fun `안내가 끝나 도착 화면이면 찾기 단계가 남아 있어도 카드다`() {
        val ready = CardSessionState.Ready(setOf(Symptom.COUGH))
        assertEquals(HomeContent.ARRIVAL, homeContentOf(HomeUiState.Parked, arrived))
        assertEquals(ParkedContent.CARD, parkedContentOf(arrived, ready, finder = FinderStep.Hospitals))
        assertEquals(ParkedContent.CARD, parkedContentOf(arrived, ready, finder = FinderStep.Confirm(choice)))
    }
}
