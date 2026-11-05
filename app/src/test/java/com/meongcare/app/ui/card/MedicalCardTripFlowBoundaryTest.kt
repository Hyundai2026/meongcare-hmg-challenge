package com.meongcare.app.ui.card

import com.meongcare.app.data.driving.FakeDrivingStateSource
import com.meongcare.app.data.guidance.FakeGuidanceSource
import com.meongcare.app.data.guidance.FakeGuidanceTiming
import com.meongcare.app.data.hospital.SampleHospitals
import com.meongcare.app.data.pet.FakePetProfileSource
import com.meongcare.app.domain.card.CardSessionState
import com.meongcare.app.domain.card.MedicalCardSession
import com.meongcare.app.domain.card.Symptom
import com.meongcare.app.domain.card.Symptom.BREATHING_DIFFICULTY
import com.meongcare.app.domain.card.Symptom.COUGH
import com.meongcare.app.domain.card.Symptom.VOMITING
import com.meongcare.app.domain.driving.DrivingState
import com.meongcare.app.domain.phone.PhoneDialer
import com.meongcare.app.testutil.MainDispatcherRule
import com.meongcare.app.ui.guidance.GuidanceUiState
import com.meongcare.app.ui.guidance.GuidanceViewModel
import com.meongcare.app.ui.home.HomeContent
import com.meongcare.app.ui.home.HomeUiState
import com.meongcare.app.ui.home.HomeViewModel
import com.meongcare.app.ui.home.ParkedContent
import com.meongcare.app.ui.home.homeContentOf
import com.meongcare.app.ui.home.parkedContentOf
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * 진료 카드 흐름 경계 (계획 009 설계 결정 1 "주행으로 바뀔 때", "버튼 동작").
 * 주행 상태는 손으로 바꾼다 (시나리오 없음): Fake 주행 상태 + Fake 길안내 + 세 ViewModel → homeContentOf → parkedContentOf.
 *
 * 길안내 timing: 안내 시작 → 1초 2분 → 11초 1분 → 21초 도착
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MedicalCardTripFlowBoundaryTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val guidanceTiming = FakeGuidanceTiming(
        routeCalculationDelay = 1.seconds,
        initialRemaining = 2.minutes,
        remainingStep = 1.minutes,
        tickInterval = 10.seconds,
    )
    private val clock = Clock.fixed(Instant.parse("2026-11-05T00:30:00Z"), ZoneId.of("Asia/Seoul"))

    /** 화면에 보이는 것: 주행 화면이면 Driving, 정차면 HOME/SYMPTOMS/CARD */
    private sealed interface Shown {
        data object Driving : Shown
        data class Parked(val content: ParkedContent) : Shown
    }

    private val home = Shown.Parked(ParkedContent.HOME)
    private val symptoms = Shown.Parked(ParkedContent.SYMPTOMS)
    private val card = Shown.Parked(ParkedContent.CARD)

    private class Car(
        val driving: FakeDrivingStateSource,
        val homeVm: HomeViewModel,
        val guidance: GuidanceViewModel,
        val card: MedicalCardViewModel,
    ) {
        val shown: Shown
            get() = when (homeContentOf(homeVm.uiState.value, guidance.uiState.value)) {
                HomeContent.DRIVING -> Shown.Driving
                HomeContent.PARKED, HomeContent.ARRIVAL ->
                    Shown.Parked(parkedContentOf(guidance.uiState.value, card.uiState.value.session))
            }
        val session: CardSessionState get() = card.uiState.value.session
        val guidanceUi: GuidanceUiState get() = guidance.uiState.value

        fun makeCard(vararg symptoms: Symptom) {
            card.onStartCard()
            symptoms.forEach(card::onToggleSymptom)
            card.onConfirmSymptoms()
        }
    }

    private fun TestScope.newCar(): Car {
        val driving = FakeDrivingStateSource(DrivingState.PARKED)
        val guidanceSource = FakeGuidanceSource(backgroundScope, guidanceTiming)
        return Car(
            driving = driving,
            homeVm = HomeViewModel(driving),
            guidance = GuidanceViewModel(guidanceSource, SampleHospitals.demo.destination),
            card = MedicalCardViewModel(FakePetProfileSource(), MedicalCardSession(), SampleHospitals.demo, PhoneDialer { true }, clock),
        )
    }

    /** 화면이 세 상태를 구독한다 (MainActivity의 collectAsStateWithLifecycle). 취소하면 화면이 가려진 것이다. */
    private fun TestScope.showScreen(car: Car): Job {
        val screen = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            launch { car.homeVm.uiState.collect {} }
            launch { car.guidance.uiState.collect {} }
            launch { car.card.uiState.collect {} }
        }
        runCurrent()
        return screen
    }

    private fun TestScope.openCar(): Car = newCar().also { showScreen(it) }

    private fun TestScope.advance(time: Duration) {
        advanceTimeBy(time)
        runCurrent()
    }

    private fun TestScope.drive(car: Car, state: DrivingState) {
        car.driving.set(state)
        runCurrent()
    }

    @Test
    fun `안내 없는 카드를 보는 중에 주행이나 모름이 되면 주행 화면이고 정차하면 같은 카드다`() = runTest {
        val car = openCar()
        car.makeCard(COUGH, BREATHING_DIFFICULTY)
        runCurrent()
        assertEquals(card, car.shown)

        drive(car, DrivingState.DRIVING)
        assertEquals(Shown.Driving, car.shown)
        drive(car, DrivingState.UNKNOWN)
        assertEquals(Shown.Driving, car.shown)
        drive(car, DrivingState.PARKED)

        assertEquals(card, car.shown)
        assertEquals(CardSessionState.Ready(setOf(COUGH, BREATHING_DIFFICULTY)), car.session)
        assertEquals(listOf(BREATHING_DIFFICULTY, COUGH), car.card.uiState.value.card.symptoms)
    }

    @Test
    fun `안내 중 카드에서 주행하면 주행 화면이고 다시 정차하면 같은 증상의 안내 중 카드다`() = runTest {
        val car = openCar()
        car.makeCard(COUGH)
        car.guidance.onStartGuidance()
        advance(1.seconds)
        assertEquals(GuidanceUiState.Guiding("시연용 동물병원", 2), car.guidanceUi)
        assertEquals(card, car.shown)

        drive(car, DrivingState.DRIVING)
        assertEquals(Shown.Driving, car.shown)
        drive(car, DrivingState.PARKED)
        assertEquals(card, car.shown)
        assertEquals(GuidanceUiState.Guiding("시연용 동물병원", 2), car.guidanceUi)
        assertEquals(CardSessionState.Ready(setOf(COUGH)), car.session)
    }

    @Test
    fun `도착했어도 주행이나 모름이면 카드가 보이지 않고 정차해야 같은 증상의 도착 카드가 보인다`() = runTest {
        val car = openCar()
        car.makeCard(COUGH)
        car.guidance.onStartGuidance()
        runCurrent()
        drive(car, DrivingState.DRIVING)

        advance(25.seconds)
        assertEquals(GuidanceUiState.Arrived("시연용 동물병원"), car.guidanceUi)
        assertEquals(Shown.Driving, car.shown)
        drive(car, DrivingState.UNKNOWN)
        assertEquals(Shown.Driving, car.shown)

        drive(car, DrivingState.PARKED)
        assertEquals(card, car.shown)
        assertEquals(CardSessionState.Ready(setOf(COUGH)), car.session)
    }

    @Test
    fun `카드 없이 안내가 시작돼도 정차 중에는 고른 증상 없는 카드와 등록된 의료정보가 보인다`() = runTest {
        // 설계 결정 1: 안내 중·도착 + 세션 없음 → CARD (고른 증상 없음). 나중 Real 길안내 대비.
        val car = openCar()
        car.guidance.onStartGuidance()
        runCurrent()
        assertEquals(card, car.shown)
        assertEquals(CardSessionState.None, car.session)
        assertEquals(emptyList<Symptom>(), car.card.uiState.value.card.symptoms)
        assertNotNull(car.card.uiState.value.card.medical)

        drive(car, DrivingState.DRIVING)
        advance(25.seconds)
        drive(car, DrivingState.PARKED)
        assertEquals(GuidanceUiState.Arrived("시연용 동물병원"), car.guidanceUi)
        assertEquals(card, car.shown)

        car.guidance.onDismissArrival()
        car.card.onCloseCard()
        runCurrent()
        assertEquals(home, car.shown)
    }

    @Test
    fun `도착 카드의 닫기는 길안내를 먼저 끝내므로 중간 화면도 같은 증상의 카드이고 끝나면 처음 화면이다`() = runTest {
        val car = openCar()
        car.makeCard(COUGH)
        car.guidance.onStartGuidance()
        advance(25.seconds)
        assertEquals(GuidanceUiState.Arrived("시연용 동물병원"), car.guidanceUi)

        // MainActivity: onDismissArrival() 다음 onCloseCard()
        car.guidance.onDismissArrival()
        runCurrent()
        assertEquals(card, car.shown)
        assertEquals(GuidanceUiState.Idle("시연용 동물병원"), car.guidanceUi)
        assertEquals(CardSessionState.Ready(setOf(COUGH)), car.session)

        car.card.onCloseCard()
        runCurrent()
        assertEquals(home, car.shown)
        assertEquals(CardSessionState.None, car.session)
    }

    @Test
    fun `도착 카드에서 수정하다 취소하면 같은 증상의 도착 카드다`() = runTest {
        val car = openCar()
        car.makeCard(COUGH)
        car.guidance.onStartGuidance()
        advance(25.seconds)

        car.card.onEditSymptoms()
        runCurrent()
        assertEquals(symptoms, car.shown)
        assertEquals(setOf(COUGH), car.card.uiState.value.pickingDraft)

        car.card.onToggleSymptom(VOMITING)
        car.card.onCancelPicking()
        runCurrent()
        assertEquals(card, car.shown)
        assertEquals(GuidanceUiState.Arrived("시연용 동물병원"), car.guidanceUi)
        assertEquals(CardSessionState.Ready(setOf(COUGH)), car.session)
    }

    @Test
    fun `안내 중에 수정해서 만들면 안내는 이어지고 카드 증상만 바뀐다`() = runTest {
        val car = openCar()
        car.makeCard(COUGH)
        car.guidance.onStartGuidance()
        advance(1.seconds)

        car.card.onEditSymptoms()
        runCurrent()
        assertEquals(symptoms, car.shown)
        car.card.onToggleSymptom(VOMITING)
        advance(10.seconds)
        assertEquals(GuidanceUiState.Guiding("시연용 동물병원", 1), car.guidanceUi)
        assertEquals(symptoms, car.shown)

        car.card.onConfirmSymptoms()
        runCurrent()
        assertEquals(card, car.shown)
        assertEquals(CardSessionState.Ready(setOf(COUGH, VOMITING)), car.session)
        assertEquals(listOf(VOMITING, COUGH), car.card.uiState.value.card.symptoms)
        assertEquals(GuidanceUiState.Guiding("시연용 동물병원", 1), car.guidanceUi)
    }

    @Test
    fun `증상을 고르는 중에 도착해도 정차 중이면 고르던 화면 그대로다`() = runTest {
        val car = openCar()
        car.makeCard(COUGH)
        car.guidance.onStartGuidance()
        runCurrent()
        car.card.onEditSymptoms()
        car.card.onToggleSymptom(VOMITING)
        advance(25.seconds)
        assertEquals(GuidanceUiState.Arrived("시연용 동물병원"), car.guidanceUi)
        assertEquals(symptoms, car.shown)
        assertEquals(setOf(COUGH, VOMITING), car.card.uiState.value.pickingDraft)
    }

    @Test
    fun `카드가 없을 때 고르다 취소하면 처음 화면이고 카드가 있을 때 고르다 취소하면 그 카드다`() = runTest {
        val car = openCar()
        car.card.onStartCard()
        car.card.onToggleSymptom(COUGH)
        runCurrent()
        assertEquals(symptoms, car.shown)
        car.card.onCancelPicking()
        runCurrent()
        assertEquals(home, car.shown)

        car.makeCard(VOMITING)
        car.card.onEditSymptoms()
        car.card.onToggleSymptom(COUGH)
        runCurrent()
        assertEquals(symptoms, car.shown)
        car.card.onCancelPicking()
        runCurrent()
        assertEquals(card, car.shown)
        assertEquals(CardSessionState.Ready(setOf(VOMITING)), car.session)
    }

    @Test
    fun `안내 없는 카드를 닫으면 처음 화면이고 다시 만들면 빈 선택에서 시작한다`() = runTest {
        val car = openCar()
        car.makeCard(COUGH)
        car.card.onCloseCard()
        runCurrent()
        assertEquals(home, car.shown)

        car.card.onStartCard()
        runCurrent()
        assertEquals(symptoms, car.shown)
        assertEquals(CardSessionState.Picking(emptySet(), null), car.session)
    }

    @Test
    fun `증상 0개로 만든 카드도 안내 시작부터 도착까지 카드로 보인다`() = runTest {
        val car = openCar()
        car.makeCard()
        runCurrent()
        assertEquals(card, car.shown)
        assertEquals(CardSessionState.Ready(emptySet()), car.session)

        car.guidance.onStartGuidance()
        advance(25.seconds)
        assertEquals(card, car.shown)
        assertEquals(CardSessionState.Ready(emptySet()), car.session)
    }

    @Test
    fun `정차 화면이 5초 넘게 가려졌다 돌아오면 새 주행 상태를 받기 전까지 고르던 화면도 보이지 않는다`() = runTest {
        val car = newCar()
        val screen = showScreen(car)
        car.card.onStartCard()
        car.card.onToggleSymptom(COUGH)
        runCurrent()
        assertEquals(symptoms, car.shown)

        screen.cancel()
        advance(5.seconds + 1.milliseconds)
        assertEquals(HomeUiState.Driving, car.homeVm.uiState.value)
        assertEquals(Shown.Driving, car.shown)

        showScreen(car)
        assertEquals(symptoms, car.shown)
        assertEquals(setOf(COUGH), car.card.uiState.value.pickingDraft)
    }

    @Test
    fun `앱을 처음 켜서 주행 상태를 받기 전에는 카드가 있어도 주행 화면이다`() = runTest {
        val car = newCar()
        car.makeCard(COUGH)
        // 아직 화면이 구독하지 않았다 (주행 상태를 받지 못함)
        assertEquals(Shown.Driving, car.shown)

        showScreen(car)
        assertEquals(card, car.shown)
    }
}
