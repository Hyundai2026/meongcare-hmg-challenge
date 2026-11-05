package com.meongcare.app.ui.card

import com.meongcare.app.data.demo.FakeTripScenario
import com.meongcare.app.data.demo.FakeTripTiming
import com.meongcare.app.data.driving.FakeDrivingStateSource
import com.meongcare.app.data.guidance.FakeGuidanceSource
import com.meongcare.app.data.guidance.FakeGuidanceTiming
import com.meongcare.app.data.hospital.SampleHospitals
import com.meongcare.app.data.pet.FakePetProfileSource
import com.meongcare.app.domain.card.CardSessionState
import com.meongcare.app.domain.card.MedicalCardSession
import com.meongcare.app.domain.card.Symptom.BREATHING_DIFFICULTY
import com.meongcare.app.domain.card.Symptom.COUGH
import com.meongcare.app.domain.driving.DrivingState
import com.meongcare.app.domain.phone.PhoneDialer
import com.meongcare.app.testutil.MainDispatcherRule
import com.meongcare.app.ui.guidance.GuidanceUiState
import com.meongcare.app.ui.guidance.GuidanceViewModel
import com.meongcare.app.ui.home.HomeContent
import com.meongcare.app.ui.home.HomeViewModel
import com.meongcare.app.ui.home.ParkedContent
import com.meongcare.app.ui.home.homeContentOf
import com.meongcare.app.ui.home.parkedContentOf
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * 진료 카드 흐름을 화면 상태 수준에서 따라간다 (FAKE 모드 배선과 같은 조합):
 * Fake 길안내 + Fake 주행 상태 + 시나리오 + 세 ViewModel → homeContentOf → parkedContentOf.
 *
 * 짧은 timing: 안내 시작 → 1초 3분 → 3초 출발 → 31초 도착 → 36초 주차
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MedicalCardTripFlowTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val guidanceTiming = FakeGuidanceTiming(
        routeCalculationDelay = 1.seconds,
        initialRemaining = 3.minutes,
        remainingStep = 1.minutes,
        tickInterval = 10.seconds,
    )
    private val tripTiming = FakeTripTiming(departDelay = 3.seconds, parkDelay = 5.seconds)
    private val clock = Clock.fixed(Instant.parse("2026-11-05T00:30:00Z"), ZoneId.of("Asia/Seoul"))

    /** 화면에 보이는 것: 주행 화면이면 DRIVING, 정차면 HOME/SYMPTOMS/CARD */
    private sealed interface Shown {
        data object Driving : Shown
        data class Parked(val content: ParkedContent) : Shown
    }

    private class Demo(
        val driving: FakeDrivingStateSource,
        private val home: HomeViewModel,
        val guidance: GuidanceViewModel,
        val card: MedicalCardViewModel,
    ) {
        val shown: Shown
            get() = when (homeContentOf(home.uiState.value, guidance.uiState.value)) {
                HomeContent.DRIVING -> Shown.Driving
                HomeContent.PARKED, HomeContent.ARRIVAL ->
                    Shown.Parked(parkedContentOf(guidance.uiState.value, card.uiState.value.session))
            }
        val guidanceUi: GuidanceUiState get() = guidance.uiState.value
        val session: CardSessionState get() = card.uiState.value.session

        /** MainActivity의 도착 [닫기]와 같은 순서 */
        fun closeArrival() {
            guidance.onDismissArrival()
            card.onCloseCard()
        }
    }

    private fun TestScope.startDemo(): Demo {
        val driving = FakeDrivingStateSource(DrivingState.PARKED)
        val guidanceSource = FakeGuidanceSource(backgroundScope, guidanceTiming)
        FakeTripScenario(backgroundScope, guidanceSource, driving, tripTiming).start()
        val home = HomeViewModel(driving)
        val guidance = GuidanceViewModel(guidanceSource, SampleHospitals.demo.destination)
        val card = MedicalCardViewModel(FakePetProfileSource(), MedicalCardSession(), SampleHospitals.demo, PhoneDialer { true }, clock)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { home.uiState.collect {} }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { guidance.uiState.collect {} }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { card.uiState.collect {} }
        runCurrent()
        return Demo(driving, home, guidance, card)
    }

    private fun TestScope.advance(time: Duration) {
        advanceTimeBy(time)
        runCurrent()
    }

    @Test
    fun `카드를 만들고 안내를 시작하면 출발하고 도착 후 주차하면 같은 증상의 카드이고 닫으면 처음 화면이다`() = runTest {
        val demo = startDemo()
        assertEquals(Shown.Parked(ParkedContent.HOME), demo.shown)

        demo.card.onStartCard()
        demo.card.onToggleSymptom(COUGH)
        demo.card.onToggleSymptom(BREATHING_DIFFICULTY)
        runCurrent()
        assertEquals(Shown.Parked(ParkedContent.SYMPTOMS), demo.shown)

        demo.card.onConfirmSymptoms()
        runCurrent()
        assertEquals(Shown.Parked(ParkedContent.CARD), demo.shown)

        demo.guidance.onStartGuidance()
        runCurrent()
        assertEquals(Shown.Parked(ParkedContent.CARD), demo.shown)
        assertEquals(GuidanceUiState.Guiding("시연용 동물병원", null), demo.guidanceUi)

        advance(3.seconds) // 출발
        assertEquals(Shown.Driving, demo.shown)

        advance(28.seconds) // 31초 도착 (주행 중)
        assertEquals(Shown.Driving, demo.shown)
        assertEquals(GuidanceUiState.Arrived("시연용 동물병원"), demo.guidanceUi)

        advance(5.seconds) // 주차
        assertEquals(Shown.Parked(ParkedContent.CARD), demo.shown)
        assertEquals(CardSessionState.Ready(setOf(COUGH, BREATHING_DIFFICULTY)), demo.session)
        assertEquals(listOf(BREATHING_DIFFICULTY, COUGH), demo.card.uiState.value.card.symptoms)

        demo.closeArrival()
        runCurrent()
        assertEquals(Shown.Parked(ParkedContent.HOME), demo.shown)
        assertEquals(CardSessionState.None, demo.session)
        assertEquals(GuidanceUiState.Idle("시연용 동물병원"), demo.guidanceUi)
    }

    @Test
    fun `증상을 고르는 중에 주행이나 모름이 되면 주행 화면이고 정차하면 고르던 선택 그대로다`() = runTest {
        val demo = startDemo()
        demo.card.onStartCard()
        demo.card.onToggleSymptom(COUGH)
        runCurrent()

        demo.driving.set(DrivingState.DRIVING)
        runCurrent()
        assertEquals(Shown.Driving, demo.shown)

        demo.driving.set(DrivingState.UNKNOWN)
        runCurrent()
        assertEquals(Shown.Driving, demo.shown)

        demo.driving.set(DrivingState.PARKED)
        runCurrent()
        assertEquals(Shown.Parked(ParkedContent.SYMPTOMS), demo.shown)
        assertEquals(setOf(COUGH), demo.card.uiState.value.pickingDraft)
    }

    @Test
    fun `안내 중에 취소하면 생성 완료 카드로 돌아간다`() = runTest {
        val demo = startDemo()
        demo.card.onStartCard()
        demo.card.onToggleSymptom(COUGH)
        demo.card.onConfirmSymptoms()
        demo.guidance.onStartGuidance()
        advance(1.seconds)
        assertEquals(GuidanceUiState.Guiding("시연용 동물병원", 3), demo.guidanceUi)

        demo.guidance.onCancelGuidance()
        runCurrent()
        assertEquals(GuidanceUiState.Idle("시연용 동물병원"), demo.guidanceUi)
        assertEquals(Shown.Parked(ParkedContent.CARD), demo.shown)
        assertEquals(CardSessionState.Ready(setOf(COUGH)), demo.session)
    }
}
