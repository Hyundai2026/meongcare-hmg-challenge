package com.meongcare.app.ui.card

import com.meongcare.app.data.hospital.SampleHospitals
import com.meongcare.app.data.pet.FakePetProfileSource
import com.meongcare.app.data.pet.SamplePetProfiles
import com.meongcare.app.domain.card.CardSessionState
import com.meongcare.app.domain.card.MedicalCardSession
import com.meongcare.app.domain.card.Symptom
import com.meongcare.app.domain.card.Symptom.BREATHING_DIFFICULTY
import com.meongcare.app.domain.card.Symptom.COUGH
import com.meongcare.app.domain.card.Symptom.VOMITING
import com.meongcare.app.domain.guidance.Destination
import com.meongcare.app.domain.hospital.Hospital
import com.meongcare.app.domain.pet.AgeLabel
import com.meongcare.app.domain.pet.PetProfileLoad
import com.meongcare.app.domain.pet.PetProfileSource
import com.meongcare.app.domain.phone.PhoneDialer
import com.meongcare.app.testutil.MainDispatcherRule
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

/**
 * MedicalCardViewModel 경계. 기준: 계획 009 단계 4 (uiState = 프로필 + 세션 + 번호 패널, WhileSubscribed(5초),
 * 처음 값은 프로필 null(Loading) + 지금 세션), 설계 결정 2 (세션은 프로세스 수명), 6 (Loading 머리), 7 (번호 패널).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MedicalCardViewModelBoundaryTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val bori = SamplePetProfiles.bori
    private val seoulClock = Clock.fixed(Instant.parse("2026-11-05T00:30:00Z"), ZoneId.of("Asia/Seoul"))
    private val session = MedicalCardSession()
    private val dialed = mutableListOf<String>()
    private var dialerOpens = true
    private val dialer = PhoneDialer { number -> dialed += number; dialerOpens }

    /** 아직 첫 값을 내보내지 않은 출처 (나중 서버 구현이 불러오는 중일 때). [send]로 값을 보낸다. */
    private class NotYetLoadedPetProfileSource : PetProfileSource {
        private val values = MutableSharedFlow<PetProfileLoad>(replay = 1)
        override val profile: Flow<PetProfileLoad> = values
        fun send(value: PetProfileLoad) = check(values.tryEmit(value))
    }

    /** 구독 수를 볼 수 있는 출처 */
    private class CountingPetProfileSource(initial: PetProfileLoad) : PetProfileSource {
        val values = MutableStateFlow(initial)
        override val profile: Flow<PetProfileLoad> = values
        val subscribers: Int get() = values.subscriptionCount.value
    }

    private fun viewModel(
        source: PetProfileSource = FakePetProfileSource(),
        hospital: Hospital = SampleHospitals.demo,
        clock: Clock = seoulClock,
    ) = MedicalCardViewModel(source, session, hospital, dialer, clock)

    private fun TestScope.show(vm: MedicalCardViewModel): Job {
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        runCurrent()
        return job
    }

    private fun MedicalCardSession.makeCard(vararg symptoms: Symptom) {
        startPicking()
        symptoms.forEach(::toggle)
        confirm()
    }

    @Test
    fun `구독 전 처음 값은 프로필을 불러오는 중이고 지금 세션을 그대로 보인다`() {
        session.makeCard(COUGH, BREATHING_DIFFICULTY)
        val state = viewModel().uiState.value
        assertEquals(CardSessionState.Ready(setOf(COUGH, BREATHING_DIFFICULTY)), state.session)
        assertEquals(PetHeader.Loading, state.card.pet)
        assertEquals(listOf(BREATHING_DIFFICULTY, COUGH), state.card.symptoms)
        assertNull(state.card.medical)
        assertNull(state.shownPhoneNumber)
        assertEquals("시연용 동물병원", state.hospitalName)
    }

    @Test
    fun `화면을 다시 만들어도 같은 세션이면 고르던 증상과 카드가 남는다`() = runTest {
        val first = viewModel()
        show(first)
        first.onStartCard()
        first.onToggleSymptom(VOMITING)
        runCurrent()

        val second = viewModel()
        show(second)
        assertEquals(CardSessionState.Picking(setOf(VOMITING), null), second.uiState.value.session)
        assertEquals(setOf(VOMITING), second.uiState.value.pickingDraft)

        second.onConfirmSymptoms()
        val third = viewModel()
        show(third)
        assertEquals(CardSessionState.Ready(setOf(VOMITING)), third.uiState.value.session)
        assertEquals(listOf(VOMITING), third.uiState.value.card.symptoms)
    }

    @Test
    fun `프로필을 아직 받지 못해도 증상 고르기와 카드 만들기가 화면 상태에 반영된다`() = runTest {
        // 설계 결정 6: 프로필이 Loading이어도 카드는 "불러오는 중" 머리 + 증상 줄로 보인다.
        val source = NotYetLoadedPetProfileSource()
        val vm = viewModel(source)
        show(vm)
        assertEquals(PetHeader.Loading, vm.uiState.value.card.pet)

        vm.onStartCard()
        runCurrent()
        assertEquals(CardSessionState.Picking(emptySet(), null), session.state.value)
        assertEquals(CardSessionState.Picking(emptySet(), null), vm.uiState.value.session)

        vm.onToggleSymptom(COUGH)
        vm.onConfirmSymptoms()
        runCurrent()
        val state = vm.uiState.value
        assertEquals(CardSessionState.Ready(setOf(COUGH)), state.session)
        assertEquals(PetHeader.Loading, state.card.pet)
        assertEquals(listOf(COUGH), state.card.symptoms)
        assertNull(state.card.medical)

        source.send(PetProfileLoad.Loaded(bori))
        runCurrent()
        assertEquals("보리", (vm.uiState.value.card.pet as PetHeader.Registered).name)
        assertEquals(CardSessionState.Ready(setOf(COUGH)), vm.uiState.value.session)
    }

    @Test
    fun `프로필을 아직 받지 못해도 전화 앱을 열지 못하면 번호 패널이 뜬다`() = runTest {
        dialerOpens = false
        val vm = viewModel(NotYetLoadedPetProfileSource())
        show(vm)
        vm.onCallHospital()
        runCurrent()
        assertEquals(listOf("02-0000-0000"), dialed)
        assertEquals("02-0000-0000", vm.uiState.value.shownPhoneNumber)
    }

    @Test
    fun `오늘과 정보 갱신일은 기기 시계의 시간대 날짜로 센다`() = runTest {
        // 생년월일 2014-08-05: 서울 11월 5일이면 147개월, 11월 4일이면 146개월
        val profile = bori.copy(birthDate = LocalDate.of(2014, 8, 5))
        val at = Instant.parse("2026-11-04T15:00:00Z")

        fun ageAndUpdatedOnIn(zone: String): Pair<AgeLabel?, LocalDate?> {
            val vm = viewModel(FakePetProfileSource(PetProfileLoad.Loaded(profile)), clock = Clock.fixed(at, ZoneId.of(zone)))
            show(vm)
            val card = vm.uiState.value.card
            return (card.pet as PetHeader.Registered).age to card.medical?.updatedOn
        }

        assertEquals(AgeLabel.YearsAndMonths(12, 3) to LocalDate.of(2026, 11, 5), ageAndUpdatedOnIn("Asia/Seoul"))
        assertEquals(AgeLabel.YearsAndMonths(12, 2) to LocalDate.of(2026, 11, 5), ageAndUpdatedOnIn("UTC"))
        assertEquals(AgeLabel.YearsAndMonths(12, 2) to LocalDate.of(2026, 11, 4), ageAndUpdatedOnIn("America/Los_Angeles"))
    }

    @Test
    fun `프로필이 읽지 못함으로 바뀌어도 고른 증상과 세션은 그대로이고 다시 읽으면 의료정보가 돌아온다`() = runTest {
        val source = FakePetProfileSource()
        val vm = viewModel(source)
        show(vm)
        vm.onStartCard()
        vm.onToggleSymptom(COUGH)
        vm.onToggleSymptom(BREATHING_DIFFICULTY)
        vm.onConfirmSymptoms()

        source.set(PetProfileLoad.Unavailable)
        runCurrent()
        var state = vm.uiState.value
        assertEquals(CardSessionState.Ready(setOf(COUGH, BREATHING_DIFFICULTY)), state.session)
        assertEquals(PetHeader.Unavailable, state.card.pet)
        assertEquals(listOf(BREATHING_DIFFICULTY, COUGH), state.card.symptoms)
        assertNull(state.card.medical)

        source.set(PetProfileLoad.Loaded(bori))
        runCurrent()
        state = vm.uiState.value
        assertEquals(listOf("심장질환"), state.card.medical!!.conditions)
        assertEquals(listOf(BREATHING_DIFFICULTY, COUGH), state.card.symptoms)
    }

    @Test
    fun `번호 패널은 세션에 두지 않아서 패널이 떠도 카드 세션은 그대로다`() = runTest {
        dialerOpens = false
        val vm = viewModel()
        show(vm)
        vm.onStartCard()
        vm.onToggleSymptom(COUGH)
        vm.onConfirmSymptoms()
        vm.onCallHospital()
        runCurrent()
        assertEquals("02-0000-0000", vm.uiState.value.shownPhoneNumber)
        assertEquals(CardSessionState.Ready(setOf(COUGH)), session.state.value)
        assertEquals(CardSessionState.Ready(setOf(COUGH)), vm.uiState.value.session)
    }

    @Test
    fun `전화 앱을 여러 번 열지 못해도 번호 패널은 같은 번호이고 확인을 두 번 눌러도 닫힌 채다`() = runTest {
        dialerOpens = false
        val vm = viewModel()
        show(vm)
        vm.onCallHospital()
        vm.onCallHospital()
        runCurrent()
        assertEquals(listOf("02-0000-0000", "02-0000-0000"), dialed)
        assertEquals("02-0000-0000", vm.uiState.value.shownPhoneNumber)
        vm.onDismissPhoneNumber()
        vm.onDismissPhoneNumber()
        runCurrent()
        assertNull(vm.uiState.value.shownPhoneNumber)
    }

    @Test
    fun `전화 앱을 처음엔 열지 못해 번호를 보였어도 확인 뒤 다시 열리면 패널을 띄우지 않는다`() = runTest {
        dialerOpens = false
        val vm = viewModel()
        show(vm)
        vm.onCallHospital()
        vm.onDismissPhoneNumber()
        dialerOpens = true
        vm.onCallHospital()
        runCurrent()
        assertNull(vm.uiState.value.shownPhoneNumber)
        assertEquals(2, dialed.size)
    }

    @Test
    fun `시연용이 아닌 병원은 그 번호를 그대로 전화 앱에 넘기고 시연용 표시가 없다`() = runTest {
        dialerOpens = false
        val hospital = Hospital(Destination("우리동네 동물병원", 37.5, 127.0), phoneNumber = "031-123-4567", isSample = false)
        val vm = viewModel(hospital = hospital)
        show(vm)
        vm.onCallHospital()
        runCurrent()
        val state = vm.uiState.value
        assertEquals(listOf("031-123-4567"), dialed)
        assertEquals("031-123-4567", state.shownPhoneNumber)
        assertEquals("우리동네 동물병원", state.hospitalName)
        assertEquals("031-123-4567", state.hospitalPhone)
        assertFalse(state.isHospitalSample)
    }

    @Test
    fun `고르는 중이 아니면 증상 누르기와 만들기와 취소는 카드 없음을 바꾸지 않는다`() = runTest {
        val vm = viewModel()
        show(vm)
        vm.onToggleSymptom(COUGH)
        vm.onConfirmSymptoms()
        vm.onCancelPicking()
        runCurrent()
        assertEquals(CardSessionState.None, vm.uiState.value.session)
        assertEquals(emptySet<Symptom>(), vm.uiState.value.pickingDraft)
        assertEquals(emptyList<Symptom>(), vm.uiState.value.card.symptoms)
    }

    @Test
    fun `카드가 없을 때도 등록된 의료정보로 카드 내용을 만들어 둔다`() = runTest {
        // 안내 중·도착 + 세션 없음이면 "고른 증상 없음" 카드가 보인다 (설계 결정 1)
        val vm = viewModel()
        show(vm)
        val card = vm.uiState.value.card
        assertEquals(emptyList<Symptom>(), card.symptoms)
        assertEquals(listOf("특정 단백질 알레르기"), card.medical!!.allergies)
    }

    @Test
    fun `화면이 구독을 끊고 5초가 지나야 프로필 출처 구독을 놓는다`() = runTest {
        val source = CountingPetProfileSource(PetProfileLoad.Loaded(bori))
        val vm = viewModel(source)
        val screen = show(vm)
        assertEquals(1, source.subscribers)

        screen.cancel()
        advanceTimeBy(4_999)
        runCurrent()
        assertEquals(1, source.subscribers)

        advanceTimeBy(2)
        runCurrent()
        assertEquals(0, source.subscribers)
    }

    @Test
    fun `구독을 놓은 사이 프로필이 바뀌면 다시 구독할 때 새 프로필을 보인다`() = runTest {
        val source = CountingPetProfileSource(PetProfileLoad.Loaded(bori))
        val vm = viewModel(source)
        show(vm).cancel()
        advanceTimeBy(5_001)
        runCurrent()
        assertEquals(0, source.subscribers)

        source.values.value = PetProfileLoad.NotRegistered
        show(vm)
        assertEquals(1, source.subscribers)
        assertEquals(PetHeader.NotRegistered, vm.uiState.value.card.pet)
        assertNull(vm.uiState.value.card.medical)
    }
}
