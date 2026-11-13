package com.meongcare.app.ui.card

import com.meongcare.app.data.hospital.SampleHospitals
import com.meongcare.app.data.pet.FakePetProfileSource
import com.meongcare.app.data.pet.SamplePetProfiles
import com.meongcare.app.domain.card.MedicalCardSession
import com.meongcare.app.domain.card.Symptom
import com.meongcare.app.domain.card.Symptom.BREATHING_DIFFICULTY
import com.meongcare.app.domain.card.Symptom.COUGH
import com.meongcare.app.domain.guidance.Destination
import com.meongcare.app.domain.hospital.Hospital
import com.meongcare.app.domain.pet.PetProfileLoad
import com.meongcare.app.domain.pet.PetProfileSource
import com.meongcare.app.domain.phone.PhoneDialer
import com.meongcare.app.testutil.MainDispatcherRule
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * MedicalCardViewModel의 012 인자 경계: `selectedHospital`, `onCallNumber`, `caseCard`.
 * 기준: 계획 012 K5(고르기 전 카드에서는 [병원에 전화]를 숨긴다), "설계 결정 8"의 3·5([병원에 전화]는 고른 병원 번호,
 * 확인 화면에서도 전화), U3·docs/data/emergency-case.md(사진은 보내지 않는다, 프로필을 못 받았으면 pet = null,
 * 증상은 프리셋 순서), `MedicalCardViewModel` KDoc(인자를 주지 않으면 예전처럼 고정 병원).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MedicalCardViewModelHospitalBoundaryTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val clock = Clock.fixed(Instant.parse("2026-11-06T05:00:00Z"), ZoneId.of("Asia/Seoul"))
    private val session = MedicalCardSession()
    private val dialed = mutableListOf<String>()
    private var dialerOpens = true
    private val dialer = PhoneDialer { number -> dialed += number; dialerOpens }
    private val selected = MutableStateFlow<Hospital?>(null)

    private val fast = Hospital(Destination("시연용 바른 동물병원", 37.367, 127.1083), "02-0000-0001", isSample = true, id = "demo-fast")

    private class NotYetLoadedPetProfileSource : PetProfileSource {
        override val profile: Flow<PetProfileLoad> = MutableSharedFlow(replay = 1)
    }

    private fun viewModel(
        source: PetProfileSource = FakePetProfileSource(),
        selectedHospital: MutableStateFlow<Hospital?>? = selected,
    ) = MedicalCardViewModel(source, session, SampleHospitals.demo, dialer, clock, selectedHospital)

    private fun TestScope.show(vm: MedicalCardViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        runCurrent()
    }

    private fun MedicalCardSession.makeCard(vararg symptoms: Symptom) {
        startPicking()
        symptoms.forEach(::toggle)
        confirm()
    }

    // ---- 고른 병원과 전화 ----

    @Test
    fun `병원을 고르기 전에는 전화 버튼이 없고 눌러도 전화하지 않는다`() = runTest {
        val vm = viewModel()
        show(vm)
        assertNull(vm.uiState.value.hospitalPhone)
        vm.onCallHospital()
        runCurrent()
        assertTrue(dialed.isEmpty())
        assertNull(vm.uiState.value.shownPhoneNumber)
    }

    @Test
    fun `구독하기 전 처음 값도 고른 병원을 따른다`() {
        assertNull(viewModel().uiState.value.hospitalPhone)
        selected.value = fast
        val state = viewModel().uiState.value
        assertEquals("02-0000-0001", state.hospitalPhone)
        assertEquals("시연용 바른 동물병원", state.hospitalName)
    }

    @Test
    fun `병원을 고르면 그 병원의 이름과 번호와 시연용 표시를 쓴다`() = runTest {
        val vm = viewModel()
        show(vm)
        selected.value = Hospital(Destination("제휴 동물병원", 37.4, 127.1), "031-000-0000", isSample = false, id = "real-1")
        runCurrent()
        val state = vm.uiState.value
        assertEquals("제휴 동물병원", state.hospitalName)
        assertEquals("031-000-0000", state.hospitalPhone)
        assertFalse(state.isHospitalSample)

        vm.onCallHospital()
        assertEquals(listOf("031-000-0000"), dialed)
    }

    @Test
    fun `고른 병원의 전화 앱을 열지 못하면 그 번호를 크게 보인다`() = runTest {
        dialerOpens = false
        val vm = viewModel()
        show(vm)
        selected.value = fast
        runCurrent()
        vm.onCallHospital()
        runCurrent()
        assertEquals(listOf("02-0000-0001"), dialed)
        assertEquals("02-0000-0001", vm.uiState.value.shownPhoneNumber)
    }

    @Test
    fun `고른 병원이 비워지면 다시 전화 버튼이 없고 눌러도 전화하지 않는다`() = runTest {
        val vm = viewModel()
        show(vm)
        selected.value = fast
        runCurrent()
        selected.value = null
        runCurrent()
        assertNull(vm.uiState.value.hospitalPhone)
        vm.onCallHospital()
        assertTrue(dialed.isEmpty())
    }

    @Test
    fun `고른 병원에 전화번호가 없으면 전화 버튼이 없다`() = runTest {
        val vm = viewModel()
        show(vm)
        selected.value = fast.copy(phoneNumber = null)
        runCurrent()
        assertNull(vm.uiState.value.hospitalPhone)
        vm.onCallHospital()
        assertTrue(dialed.isEmpty())
    }

    @Test
    fun `확인 화면의 전화는 받은 번호로 걸고 열지 못하면 그 번호를 보인다`() = runTest {
        val vm = viewModel()
        show(vm)
        vm.onCallNumber("02-0000-0002")
        runCurrent()
        assertEquals(listOf("02-0000-0002"), dialed)
        assertNull(vm.uiState.value.shownPhoneNumber)

        dialerOpens = false
        vm.onCallNumber("02-0000-0003")
        runCurrent()
        assertEquals("02-0000-0003", vm.uiState.value.shownPhoneNumber)
        vm.onDismissPhoneNumber()
        runCurrent()
        assertNull(vm.uiState.value.shownPhoneNumber)
    }

    @Test
    fun `selectedHospital을 주지 않으면 예전처럼 고정 병원으로 전화한다`() = runTest {
        val vm = viewModel(selectedHospital = null)
        show(vm)
        assertEquals(SampleHospitals.demo.phoneNumber, vm.uiState.value.hospitalPhone)
        vm.onCallHospital()
        assertEquals(listOf("02-0000-0000"), dialed)
    }

    // ---- 병원에 보낼 카드 ----

    @Test
    fun `카드에는 사진을 지운 반려견 정보와 프리셋 순서의 증상이 들어간다`() = runTest {
        val withPhoto = SamplePetProfiles.demoPet.copy(photoDataUrl = "data:image/jpeg;base64,AAAA")
        val vm = viewModel(FakePetProfileSource(PetProfileLoad.Loaded(withPhoto)))
        show(vm)
        session.makeCard(COUGH, BREATHING_DIFFICULTY)
        runCurrent()
        val card = vm.caseCard()
        assertEquals(withPhoto.copy(photoDataUrl = null), card.pet)
        assertEquals(listOf(BREATHING_DIFFICULTY, COUGH), card.symptoms)
    }

    @Test
    fun `반려견 정보를 읽지 못했거나 등록하지 않았거나 아직 받지 못했으면 카드의 반려견은 null이다`() = runTest {
        listOf<PetProfileSource>(
            FakePetProfileSource(PetProfileLoad.Unavailable),
            FakePetProfileSource(PetProfileLoad.NotRegistered),
            NotYetLoadedPetProfileSource(),
        ).forEach { source ->
            val vm = viewModel(source)
            show(vm)
            session.clear()
            session.makeCard(COUGH)
            runCurrent()
            val card = vm.caseCard()
            assertNull(source.toString(), card.pet)
            assertEquals(listOf(COUGH), card.symptoms)
        }
    }

    @Test
    fun `반려견 정보가 바뀌면 카드도 새 정보로 만든다`() = runTest {
        val source = FakePetProfileSource()
        val vm = viewModel(source)
        show(vm)
        session.makeCard(COUGH)
        source.set(PetProfileLoad.Loaded(SamplePetProfiles.demoPet.copy(name = "콩이")))
        runCurrent()
        assertEquals("콩이", vm.caseCard().pet?.name)
        source.set(PetProfileLoad.Unavailable)
        runCurrent()
        assertNull(vm.caseCard().pet)
    }

    @Test
    fun `카드를 만들기 전이나 증상을 고르는 중이면 카드의 증상은 비어 있다`() = runTest {
        val vm = viewModel()
        show(vm)
        assertTrue(vm.caseCard().symptoms.isEmpty())
        session.startPicking()
        session.toggle(COUGH)
        runCurrent()
        assertTrue(vm.caseCard().symptoms.isEmpty())
    }
}
