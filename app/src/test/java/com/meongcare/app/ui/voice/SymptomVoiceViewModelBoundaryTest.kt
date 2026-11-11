package com.meongcare.app.ui.voice

import com.meongcare.app.domain.card.CardSessionState
import com.meongcare.app.domain.card.MedicalCardSession
import com.meongcare.app.domain.card.Symptom
import com.meongcare.app.domain.card.Symptom.BLOATED_BELLY
import com.meongcare.app.domain.card.Symptom.COUGH
import com.meongcare.app.domain.card.Symptom.SEIZURE
import com.meongcare.app.domain.card.Symptom.VOMITING
import com.meongcare.app.domain.voice.VoiceEvent
import com.meongcare.app.domain.voice.VoiceFailure
import com.meongcare.app.domain.voice.VoiceInput
import com.meongcare.app.testutil.MainDispatcherRule
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * `SymptomVoiceViewModel` 경계 테스트. 기준: docs/plans/active/010-voice-input.md
 * "설계 결정 1"(최종 결과만 매핑, 더하기만, 듣는 중 [카드 만들기]는 지금 선택으로), "설계 결정 2"(고르는 중이 아니게 되거나
 * 화면이 사라지면 듣기를 멈추고 들은 것을 버리고 대기, 선택은 그대로), "설계 결정 9"(상태 전이).
 */
class SymptomVoiceViewModelBoundaryTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    /** 테스트가 이벤트를 하나씩 넣는 음성 입력. 듣기 한 번마다 채널 하나. [active]는 지금 구독 중인 듣기 수. */
    private class ScriptedVoiceInput : VoiceInput {
        val listens = mutableListOf<Channel<VoiceEvent>>()
        var active = 0

        override fun listen(): Flow<VoiceEvent> = flow {
            val events = Channel<VoiceEvent>(Channel.UNLIMITED)
            listens += events
            active++
            try {
                for (event in events) {
                    emit(event)
                    if (event is VoiceEvent.Final || event is VoiceEvent.Failed) break
                }
            } finally {
                active--
            }
        }

        fun send(vararg events: VoiceEvent) = events.forEach { listens.last().trySend(it) }
    }

    private val session = MedicalCardSession()
    private val input = ScriptedVoiceInput()
    /** Main 디스패처를 바꾼 뒤(규칙 시작 뒤)에 만든다. */
    private val vm by lazy { SymptomVoiceViewModel(input, session, isSimulated = false) }

    private val voice get() = vm.uiState.value.voice
    private val draft get() = (session.state.value as CardSessionState.Picking).draft

    @Test
    fun `부분 결과에 증상 낱말이 있어도 최종 결과 전에는 선택을 바꾸지 않고 최종 문장의 증상만 더한다`() = runTest {
        session.startPicking()
        vm.onStartListening()

        input.send(VoiceEvent.Ready, VoiceEvent.Partial("강아지가 토하고"))
        assertEquals(VoiceUiState.Listening("강아지가 토하고"), voice)
        assertEquals(emptySet<Symptom>(), draft)

        input.send(VoiceEvent.Final("기침해요"))
        assertEquals(setOf(COUGH), draft)
        assertEquals(VoiceUiState.Heard("기침해요", listOf(COUGH)), voice)
    }

    @Test
    fun `들음 상태에는 이번 문장에서 찾은 증상만 말한 순서가 아니라 프리셋 순서로 보인다`() = runTest {
        session.startPicking()
        session.toggle(VOMITING)
        vm.onStartListening()

        input.send(VoiceEvent.Final("배가 빵빵하고 경련을 해요"))

        assertEquals(VoiceUiState.Heard("배가 빵빵하고 경련을 해요", listOf(SEIZURE, BLOATED_BELLY)), voice)
        assertEquals(setOf(VOMITING, SEIZURE, BLOATED_BELLY), draft)
    }

    @Test
    fun `이미 버튼으로 고른 증상을 말해도 들음 상태에 찾은 증상으로 보이고 선택은 그대로 남는다`() = runTest {
        session.startPicking()
        session.toggle(COUGH)
        vm.onStartListening()

        input.send(VoiceEvent.Final("기침하고 토해요"))

        assertEquals(VoiceUiState.Heard("기침하고 토해요", listOf(VOMITING, COUGH)), voice)
        assertEquals(setOf(COUGH, VOMITING), draft)
    }

    @Test
    fun `실패하면 이유마다 실패 상태가 되고 들은 부분 결과로 선택을 바꾸지 않는다`() = runTest {
        session.startPicking()
        session.toggle(COUGH)
        VoiceFailure.entries.forEach { reason ->
            vm.onStartListening()
            input.send(VoiceEvent.Ready, VoiceEvent.Partial("토했어요"), VoiceEvent.Failed(reason))

            assertEquals(VoiceUiState.Failed(reason), voice)
            assertEquals(setOf(COUGH), draft)
        }
        assertEquals(VoiceFailure.entries.size, input.listens.size)
    }

    @Test
    fun `들은 뒤 화면이 사라지면 대기로 돌아가고 이미 더한 선택은 그대로다`() = runTest {
        session.startPicking()
        vm.onStartListening()
        input.send(VoiceEvent.Final("토했어요"))

        vm.onStop()

        assertEquals(VoiceUiState.Idle, voice)
        assertEquals(setOf(VOMITING), draft)
    }

    @Test
    fun `실패한 뒤 화면이 사라지면 대기로 돌아간다`() = runTest {
        session.startPicking()
        vm.onStartListening()
        input.send(VoiceEvent.Failed(VoiceFailure.NO_SPEECH))

        vm.onStop()

        assertEquals(VoiceUiState.Idle, voice)
    }

    @Test
    fun `켜는 중에 화면이 사라지면 듣기를 멈추고 늦게 온 결과는 버린다`() = runTest {
        session.startPicking()
        vm.onStartListening()
        assertEquals(VoiceUiState.Starting, voice)

        vm.onStop()
        input.send(VoiceEvent.Ready, VoiceEvent.Final("토했어요"))

        assertEquals(VoiceUiState.Idle, voice)
        assertEquals(0, input.active)
        assertEquals(emptySet<Symptom>(), draft)
    }

    @Test
    fun `듣는 중에 카드를 만들면 지금 선택으로 카드가 되고 늦게 온 결과는 카드에 들어가지 않는다`() = runTest {
        session.startPicking()
        session.toggle(COUGH)
        vm.onStartListening()
        input.send(VoiceEvent.Ready, VoiceEvent.Partial("토하고"))

        session.confirm()
        input.send(VoiceEvent.Final("토하고 설사해요"))

        assertEquals(CardSessionState.Ready(setOf(COUGH)), session.state.value)
        assertEquals(VoiceUiState.Idle, voice)
        assertEquals(0, input.active)
    }

    @Test
    fun `듣는 중에 홈으로 가면 듣기를 멈추고 대기로 돌아간다`() = runTest {
        session.startPicking()
        vm.onStartListening()
        input.send(VoiceEvent.Ready)

        session.goHome()

        assertEquals(VoiceUiState.Idle, voice)
        assertEquals(0, input.active)
    }

    @Test
    fun `카드를 만든 뒤 다시 고르기를 시작하면 대기에서 새로 말할 수 있다`() = runTest {
        session.startPicking()
        vm.onStartListening()
        input.send(VoiceEvent.Final("토했어요"))
        session.confirm()

        session.startPicking()
        assertEquals(VoiceUiState.Idle, voice)

        vm.onStartListening()
        assertEquals(VoiceUiState.Starting, voice)
        input.send(VoiceEvent.Final("기침해요"))

        assertEquals(2, input.listens.size)
        assertEquals(setOf(VOMITING, COUGH), draft)
    }

    @Test
    fun `듣기를 취소한 뒤 다시 말하면 새로 듣고 그 결과를 더한다`() = runTest {
        session.startPicking()
        vm.onStartListening()
        input.send(VoiceEvent.Ready, VoiceEvent.Partial("토하"))
        vm.onCancelListening()

        vm.onStartListening()
        assertEquals(VoiceUiState.Starting, voice)
        input.send(VoiceEvent.Ready, VoiceEvent.Final("기침해요"))

        assertEquals(2, input.listens.size)
        assertEquals(0, input.active)
        assertEquals(VoiceUiState.Heard("기침해요", listOf(COUGH)), voice)
        assertEquals(setOf(COUGH), draft)
    }
}
