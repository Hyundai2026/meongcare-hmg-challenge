package com.meongcare.app.ui.voice

import com.meongcare.app.domain.card.CardSessionState
import com.meongcare.app.domain.card.MedicalCardSession
import com.meongcare.app.domain.card.Symptom.BREATHING_DIFFICULTY
import com.meongcare.app.domain.card.Symptom.COUGH
import com.meongcare.app.domain.card.Symptom.LETHARGY
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SymptomVoiceViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    /** 테스트가 이벤트를 하나씩 넣는 음성 입력. 듣기 한 번마다 채널 하나. */
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

        fun send(event: VoiceEvent) {
            listens.last().trySend(event)
        }
    }

    private val session = MedicalCardSession()
    private val input = ScriptedVoiceInput()

    private fun viewModel(voiceInput: VoiceInput? = input, isSimulated: Boolean = false) =
        SymptomVoiceViewModel(voiceInput, session, isSimulated)

    private val SymptomVoiceViewModel.voice get() = uiState.value.voice
    private val draft get() = (session.state.value as CardSessionState.Picking).draft

    @Test
    fun `마이크를 누르면 켜는 중을 거쳐 듣는 중이 되고 부분 결과 글자를 보여 준다`() = runTest {
        session.startPicking()
        val vm = viewModel()

        vm.onStartListening()
        assertEquals(VoiceUiState.Starting, vm.voice)

        input.send(VoiceEvent.Ready)
        assertEquals(VoiceUiState.Listening(""), vm.voice)

        input.send(VoiceEvent.Partial("강아지가 갑자기"))
        assertEquals(VoiceUiState.Listening("강아지가 갑자기"), vm.voice)
    }

    @Test
    fun `최종 문장에서 찾은 증상을 지금 선택에 더한다`() = runTest {
        session.startPicking()
        session.toggle(COUGH)
        val vm = viewModel()

        vm.onStartListening()
        input.send(VoiceEvent.Ready)
        input.send(VoiceEvent.Final("강아지가 갑자기 토하고 숨을 가쁘게 쉬어요"))

        assertEquals(
            VoiceUiState.Heard("강아지가 갑자기 토하고 숨을 가쁘게 쉬어요", listOf(BREATHING_DIFFICULTY, VOMITING)),
            vm.voice,
        )
        assertEquals(setOf(COUGH, BREATHING_DIFFICULTY, VOMITING), draft)
    }

    @Test
    fun `찾은 증상이 없으면 선택을 바꾸지 않고 못 찾았다고 알린다`() = runTest {
        session.startPicking()
        session.toggle(COUGH)
        val vm = viewModel()

        vm.onStartListening()
        input.send(VoiceEvent.Final("내일 토요일이었으면 좋겠어요"))

        assertEquals(VoiceUiState.Heard("내일 토요일이었으면 좋겠어요", emptyList()), vm.voice)
        assertEquals(setOf(COUGH), draft)
    }

    @Test
    fun `두 번 말하면 증상이 쌓인다`() = runTest {
        session.startPicking()
        val vm = viewModel()

        vm.onStartListening()
        input.send(VoiceEvent.Final("토했어요"))
        vm.onStartListening()
        input.send(VoiceEvent.Final("기침을 하고 기운이 없어요"))

        assertEquals(2, input.listens.size)
        assertEquals(setOf(VOMITING, COUGH, LETHARGY), draft)
        assertEquals(VoiceUiState.Heard("기침을 하고 기운이 없어요", listOf(COUGH, LETHARGY)), vm.voice)
    }

    @Test
    fun `듣는 중에 취소하거나 화면이 사라지면 결과를 버리고 대기로 돌아간다`() = runTest {
        session.startPicking()
        val vm = viewModel()

        vm.onStartListening()
        input.send(VoiceEvent.Partial("토하고"))
        vm.onCancelListening()
        input.send(VoiceEvent.Final("토하고 있어요")) // 취소한 뒤에 온 결과
        assertEquals(VoiceUiState.Idle, vm.voice)
        assertEquals(emptySet<Any>(), draft)
        assertEquals(0, input.active)

        vm.onStartListening()
        input.send(VoiceEvent.Ready)
        vm.onStop()
        assertEquals(VoiceUiState.Idle, vm.voice)
        assertEquals(0, input.active)
    }

    @Test
    fun `증상 고르기가 끝나면 듣기를 멈추고 대기로 돌아간다`() = runTest {
        session.startPicking()
        val vm = viewModel()

        vm.onStartListening()
        input.send(VoiceEvent.Ready)
        session.confirm()

        assertEquals(VoiceUiState.Idle, vm.voice)
        assertEquals(0, input.active)

        // 들음 상태도 고르기가 끝나면 지운다. 다시 고르면 대기부터다.
        session.startPicking()
        vm.onStartListening()
        input.send(VoiceEvent.Final("기침해요"))
        session.cancelPicking()
        session.startPicking()
        assertEquals(VoiceUiState.Idle, vm.voice)
    }

    @Test
    fun `켜는 중이나 듣는 중에 다시 눌러도 새로 듣지 않는다`() = runTest {
        session.startPicking()
        val vm = viewModel()

        vm.onStartListening()
        vm.onStartListening()
        input.send(VoiceEvent.Ready)
        vm.onStartListening()

        assertEquals(1, input.listens.size)
        assertEquals(VoiceUiState.Listening(""), vm.voice)
    }

    @Test
    fun `실패하면 이유를 보여 주고 다시 말할 수 있다`() = runTest {
        session.startPicking()
        val vm = viewModel()

        vm.onStartListening()
        input.send(VoiceEvent.Failed(VoiceFailure.MIC))
        assertEquals(VoiceUiState.Failed(VoiceFailure.MIC), vm.voice)

        vm.onStartListening()
        assertEquals(VoiceUiState.Starting, vm.voice)
        assertEquals(2, input.listens.size)
    }

    @Test
    fun `음성 입력이 끝났는데 결과가 없으면 말소리 없음이고 예외면 쓸 수 없음이다`() = runTest {
        session.startPicking()
        val ended = viewModel(voiceInput = object : VoiceInput {
            override fun listen(): Flow<VoiceEvent> = flow { emit(VoiceEvent.Ready) }
        })
        ended.onStartListening()
        assertEquals(VoiceUiState.Failed(VoiceFailure.NO_SPEECH), ended.voice)

        val broken = viewModel(voiceInput = object : VoiceInput {
            override fun listen(): Flow<VoiceEvent> = flow { error("엔진 없음") }
        })
        broken.onStartListening()
        assertEquals(VoiceUiState.Failed(VoiceFailure.UNAVAILABLE), broken.voice)
    }

    @Test
    fun `음성 입력이 없으면 쓸 수 없음 상태이고 눌러도 아무것도 하지 않는다`() = runTest {
        session.startPicking()
        val vm = viewModel(voiceInput = null)

        assertFalse(vm.uiState.value.available)
        vm.onStartListening()
        assertEquals(VoiceUiState.Idle, vm.voice)
    }

    @Test
    fun `고르는 중이 아니면 눌러도 듣지 않는다`() = runTest {
        val vm = viewModel()

        vm.onStartListening()

        assertEquals(VoiceUiState.Idle, vm.voice)
        assertEquals(0, input.listens.size)
    }

    @Test
    fun `시연용 음성 입력이면 화면에 알린다`() = runTest {
        assertTrue(viewModel(isSimulated = true).uiState.value.isSimulated)
        assertFalse(viewModel(isSimulated = false).uiState.value.isSimulated)
        assertTrue(viewModel().uiState.value.available)
    }
}
