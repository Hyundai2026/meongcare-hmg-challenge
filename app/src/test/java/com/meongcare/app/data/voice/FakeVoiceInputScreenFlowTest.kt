package com.meongcare.app.data.voice

import com.meongcare.app.domain.card.CardSessionState
import com.meongcare.app.domain.card.MedicalCardSession
import com.meongcare.app.domain.card.Symptom.BREATHING_DIFFICULTY
import com.meongcare.app.domain.card.Symptom.COUGH
import com.meongcare.app.domain.card.Symptom.LETHARGY
import com.meongcare.app.domain.card.Symptom.VOMITING
import com.meongcare.app.domain.card.inPresetOrder
import com.meongcare.app.testutil.MainDispatcherRule
import com.meongcare.app.ui.voice.SymptomVoiceViewModel
import com.meongcare.app.ui.voice.VoiceUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds

/**
 * 계획 010 확인 절차 P2~P4를 화면 상태 수준에서 따라간다 (Fake 모드 배선과 같은 조합:
 * 기본 `FakeVoiceInput` + `MedicalCardSession` → `SymptomVoiceViewModel`).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FakeVoiceInputScreenFlowTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val session = MedicalCardSession()
    /** Main 디스패처를 바꾼 뒤(규칙 시작 뒤)에 만든다. */
    private val viewModel by lazy { SymptomVoiceViewModel(FakeVoiceInput(), session, isSimulated = true) }

    private val voice get() = viewModel.uiState.value.voice
    private val draft get() = (session.state.value as CardSessionState.Picking).draft

    @Test
    fun `P2 말로 고르기를 누르면 켜는 중 듣는 중을 거쳐 첫 예시 문장의 구토와 호흡 곤란이 골라진다`() = runTest {
        session.startPicking()

        viewModel.onStartListening()
        assertEquals(VoiceUiState.Starting, voice)

        advanceTimeBy(300.milliseconds)
        runCurrent()
        assertEquals(VoiceUiState.Listening(""), voice)

        advanceTimeBy(400.milliseconds)
        runCurrent()
        assertEquals(VoiceUiState.Listening("강아지가"), voice)

        advanceUntilIdle()
        assertEquals(
            VoiceUiState.Heard("강아지가 갑자기 토하고 숨을 가쁘게 쉬어요", listOf(BREATHING_DIFFICULTY, VOMITING)),
            voice,
        )
        assertEquals(setOf(BREATHING_DIFFICULTY, VOMITING), draft)
    }

    @Test
    fun `P3 P4 기침을 누르고 다시 말하면 기운 없음이 더해지고 카드에는 프리셋 순서로 네 증상이 적힌다`() = runTest {
        session.startPicking()
        viewModel.onStartListening()
        advanceUntilIdle()

        session.toggle(COUGH)
        viewModel.onStartListening()
        advanceUntilIdle()

        assertEquals(VoiceUiState.Heard("기침을 하고 기운이 없어요", listOf(COUGH, LETHARGY)), voice)
        assertEquals(setOf(BREATHING_DIFFICULTY, VOMITING, COUGH, LETHARGY), draft)

        session.confirm()
        val card = session.state.value as CardSessionState.Ready
        assertEquals(listOf(BREATHING_DIFFICULTY, VOMITING, COUGH, LETHARGY), card.symptoms.inPresetOrder())
        assertEquals(VoiceUiState.Idle, voice)
    }
}
