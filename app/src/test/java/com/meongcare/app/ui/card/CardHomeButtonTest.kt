package com.meongcare.app.ui.card

import app.cash.turbine.test
import com.meongcare.app.data.hospital.SampleHospitals
import com.meongcare.app.data.pet.FakePetProfileSource
import com.meongcare.app.domain.card.CardSessionState
import com.meongcare.app.domain.card.MedicalCardSession
import com.meongcare.app.domain.card.Symptom.COUGH
import com.meongcare.app.domain.phone.PhoneDialer
import com.meongcare.app.testutil.MainDispatcherRule
import com.meongcare.app.ui.guidance.GuidanceUiState
import com.meongcare.app.ui.home.ParkedContent
import com.meongcare.app.ui.home.parkedContentOf
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** [홈] 버튼: 카드를 지우지 않고 처음 화면으로 가고, [카드 보기]로 돌아온다. */
class CardHomeButtonTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val idle = GuidanceUiState.Idle("시연용 동물병원")
    private val guiding = GuidanceUiState.Guiding("시연용 동물병원", 12)
    private val arrived = GuidanceUiState.Arrived("시연용 동물병원")

    private fun sessionWithCard() = MedicalCardSession().apply {
        startPicking()
        toggle(COUGH)
        confirm()
    }

    @Test
    fun `홈을 누르면 카드는 남고 처음 화면이며 카드 보기로 같은 카드에 돌아온다`() {
        val session = sessionWithCard()
        session.goHome()
        assertTrue(session.atHome.value)
        assertEquals(CardSessionState.Ready(setOf(COUGH)), session.state.value)
        assertEquals(ParkedContent.HOME, parkedContentOf(idle, session.state.value, session.atHome.value))

        session.openCard()
        assertFalse(session.atHome.value)
        assertEquals(ParkedContent.CARD, parkedContentOf(idle, session.state.value, session.atHome.value))
    }

    @Test
    fun `안내 중이나 도착해도 홈을 누르면 처음 화면이다`() {
        val session = sessionWithCard()
        session.goHome()
        listOf(guiding, arrived).forEach {
            assertEquals(ParkedContent.HOME, parkedContentOf(it, session.state.value, session.atHome.value))
        }
    }

    @Test
    fun `고르는 중에 홈을 누르면 고르기를 취소하고 처음 화면이다`() {
        val session = sessionWithCard()
        session.startPicking()
        session.toggle(COUGH)
        session.goHome()
        assertEquals(CardSessionState.Ready(setOf(COUGH)), session.state.value)
        assertEquals(ParkedContent.HOME, parkedContentOf(idle, session.state.value, session.atHome.value))
    }

    @Test
    fun `처음 화면에서 고르기를 시작하면 홈 상태가 풀리고 증상 고르기다`() {
        val session = sessionWithCard()
        session.goHome()
        session.startPicking()
        assertFalse(session.atHome.value)
        assertEquals(ParkedContent.SYMPTOMS, parkedContentOf(idle, session.state.value, session.atHome.value))
    }

    @Test
    fun `새로 만들기는 있던 카드를 버리고 빈 선택으로 고른다`() {
        val session = sessionWithCard()
        session.goHome()
        session.startNew()
        assertEquals(CardSessionState.Picking(emptySet(), null), session.state.value)
        assertFalse(session.atHome.value)
        session.cancelPicking()
        assertEquals(CardSessionState.None, session.state.value)
    }

    @Test
    fun `카드를 비우면 홈 상태도 풀린다`() {
        val session = sessionWithCard()
        session.goHome()
        session.clear()
        assertFalse(session.atHome.value)
        assertEquals(ParkedContent.HOME, parkedContentOf(idle, session.state.value, session.atHome.value))
    }

    @Test
    fun `ViewModel의 홈과 카드 보기가 화면 상태에 반영되고 번호 패널을 닫는다`() = runTest {
        val session = sessionWithCard()
        val clock = Clock.fixed(Instant.parse("2026-11-05T00:30:00Z"), ZoneId.of("Asia/Seoul"))
        val vm = MedicalCardViewModel(FakePetProfileSource(), session, SampleHospitals.demo, PhoneDialer { false }, clock)
        vm.uiState.test {
            vm.onCallHospital()
            assertEquals("02-0000-0000", expectMostRecentItem().shownPhoneNumber)
            vm.onGoHome()
            val home = expectMostRecentItem()
            assertTrue(home.atHome)
            assertTrue(home.hasCard)
            assertEquals(null, home.shownPhoneNumber)
            vm.onOpenCard()
            assertFalse(expectMostRecentItem().atHome)
            vm.onStartNewCard()
            assertEquals(CardSessionState.Picking(emptySet(), null), expectMostRecentItem().session)
        }
    }
}
