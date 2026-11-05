package com.meongcare.app.ui.card

import app.cash.turbine.test
import com.meongcare.app.data.hospital.SampleHospitals
import com.meongcare.app.data.pet.FakePetProfileSource
import com.meongcare.app.domain.card.CardSessionState
import com.meongcare.app.domain.card.MedicalCardSession
import com.meongcare.app.domain.card.Symptom.BREATHING_DIFFICULTY
import com.meongcare.app.domain.card.Symptom.COUGH
import com.meongcare.app.domain.hospital.Hospital
import com.meongcare.app.domain.pet.PetProfileLoad
import com.meongcare.app.domain.phone.PhoneDialer
import com.meongcare.app.testutil.MainDispatcherRule
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MedicalCardViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val clock = Clock.fixed(Instant.parse("2026-11-05T00:30:00Z"), ZoneId.of("Asia/Seoul"))
    private val profiles = FakePetProfileSource()
    private val session = MedicalCardSession()
    private val dialed = mutableListOf<String>()
    private var dialerOpens = true
    private val dialer = PhoneDialer { number -> dialed += number; dialerOpens }

    private fun viewModel(hospital: Hospital = SampleHospitals.demo) =
        MedicalCardViewModel(profiles, session, hospital, dialer, clock)

    @Test
    fun `처음에는 보리 요약을 보여 주고 카드가 없다`() = runTest {
        viewModel().uiState.test {
            val state = expectMostRecentItem()
            assertEquals(CardSessionState.None, state.session)
            assertEquals("보리", (state.card.pet as PetHeader.Registered).name)
            assertEquals("시연용 동물병원", state.hospitalName)
            assertEquals("02-0000-0000", state.hospitalPhone)
            assertTrue(state.isHospitalSample)
            assertNull(state.shownPhoneNumber)
        }
    }

    @Test
    fun `증상을 골라 만들면 카드에 고른 증상이 들어간다`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            vm.onStartCard()
            vm.onToggleSymptom(COUGH)
            vm.onToggleSymptom(BREATHING_DIFFICULTY)
            assertEquals(setOf(COUGH, BREATHING_DIFFICULTY), expectMostRecentItem().pickingDraft)
            vm.onConfirmSymptoms()
            val state = expectMostRecentItem()
            assertEquals(CardSessionState.Ready(setOf(COUGH, BREATHING_DIFFICULTY)), state.session)
            assertEquals(listOf(BREATHING_DIFFICULTY, COUGH), state.card.symptoms)
        }
    }

    @Test
    fun `카드에서 수정하면 지금 증상이 선택된 채로 고른다`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            vm.onStartCard()
            vm.onToggleSymptom(COUGH)
            vm.onConfirmSymptoms()
            vm.onEditSymptoms()
            assertEquals(setOf(COUGH), expectMostRecentItem().pickingDraft)
            vm.onCancelPicking()
            assertEquals(CardSessionState.Ready(setOf(COUGH)), expectMostRecentItem().session)
        }
    }

    @Test
    fun `전화 앱이 열리면 번호 패널을 띄우지 않는다`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            vm.onCallHospital()
            assertEquals(listOf("02-0000-0000"), dialed)
            assertNull(expectMostRecentItem().shownPhoneNumber)
        }
    }

    @Test
    fun `전화 앱을 열지 못하면 번호를 크게 보여 주고 확인하면 닫는다`() = runTest {
        dialerOpens = false
        val vm = viewModel()
        vm.uiState.test {
            vm.onCallHospital()
            assertEquals("02-0000-0000", expectMostRecentItem().shownPhoneNumber)
            vm.onDismissPhoneNumber()
            assertNull(expectMostRecentItem().shownPhoneNumber)
        }
    }

    @Test
    fun `병원 번호가 없으면 전화 버튼 값이 없고 전화 앱을 부르지 않는다`() = runTest {
        val vm = viewModel(SampleHospitals.demo.copy(phoneNumber = null))
        vm.uiState.test {
            assertNull(expectMostRecentItem().hospitalPhone)
            vm.onCallHospital()
            assertEquals(emptyList<String>(), dialed)
            expectNoEvents()
            assertNull(vm.uiState.value.shownPhoneNumber)
        }
    }

    @Test
    fun `프로필이 바뀌면 카드도 바뀐다`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            expectMostRecentItem()
            profiles.set(PetProfileLoad.NotRegistered)
            val state = expectMostRecentItem()
            assertEquals(PetHeader.NotRegistered, state.card.pet)
            assertNull(state.card.medical)
        }
    }

    @Test
    fun `카드를 닫으면 세션이 비고 번호 패널도 닫힌다`() = runTest {
        dialerOpens = false
        val vm = viewModel()
        vm.uiState.test {
            vm.onStartCard()
            vm.onConfirmSymptoms()
            vm.onCallHospital()
            vm.onCloseCard()
            val state = expectMostRecentItem()
            assertEquals(CardSessionState.None, state.session)
            assertNull(state.shownPhoneNumber)
        }
    }
}
