package com.meongcare.app.ui.voice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.meongcare.app.domain.card.CardSessionState
import com.meongcare.app.domain.card.MedicalCardSession
import com.meongcare.app.domain.card.inPresetOrder
import com.meongcare.app.domain.voice.VoiceEvent
import com.meongcare.app.domain.voice.VoiceFailure
import com.meongcare.app.domain.voice.VoiceInput
import com.meongcare.app.domain.voice.symptomsIn
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 증상 고르기 화면의 음성 입력. 진료 카드([com.meongcare.app.ui.card.MedicalCardViewModel])와 따로 둬서
 * 음성만 통째로 뺄 수 있게 했다 (docs/plans 010 "설계 결정 8·9").
 *
 * - 마이크는 [onStartListening]을 부를 때만 켠다. 한 번에 한 문장.
 * - 최종 문장에서 찾은 증상을 지금 선택에 **더하기만** 한다 ([MedicalCardSession.select]).
 * - 증상 고르기가 끝나거나([MedicalCardSession]이 고르는 중이 아님), 화면이 사라지면([onStop]) 듣기를 멈추고 결과를 버린다.
 *
 * @param voiceInput `null`이면 이 구성에 음성 입력이 없다 (화면에 음성 영역이 없다).
 */
class SymptomVoiceViewModel(
    private val voiceInput: VoiceInput?,
    private val session: MedicalCardSession,
    isSimulated: Boolean,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        VoiceScreenState(available = voiceInput != null, isSimulated = isSimulated, voice = VoiceUiState.Idle),
    )
    val uiState: StateFlow<VoiceScreenState> = _uiState.asStateFlow()

    private var listenJob: Job? = null

    init {
        viewModelScope.launch {
            session.state.collect { state ->
                if (state !is CardSessionState.Picking) stopListening()
            }
        }
    }

    /** [말로 증상 고르기] / [다시 말하기]. 켜는 중·듣는 중이면 무시한다. */
    fun onStartListening() {
        val input = voiceInput ?: return
        if (session.state.value !is CardSessionState.Picking) return
        when (_uiState.value.voice) {
            VoiceUiState.Starting, is VoiceUiState.Listening -> return
            else -> Unit
        }
        setVoice(VoiceUiState.Starting)
        listenJob = viewModelScope.launch {
            try {
                input.listen().collect(::onEvent)
                // Final·Failed 없이 끝났으면 말소리를 듣지 못한 것으로 본다.
                if (_uiState.value.voice.isActive()) setVoice(VoiceUiState.Failed(VoiceFailure.NO_SPEECH))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                setVoice(VoiceUiState.Failed(VoiceFailure.UNAVAILABLE))
            }
        }
    }

    /** [듣기 취소]: 그때까지 들은 것을 버리고 대기로. */
    fun onCancelListening() = stopListening()

    /** 화면이 사라졌거나 앱이 뒤로 갔다: 듣기를 멈추고 결과를 버리고 대기로. */
    fun onStop() = stopListening()

    private fun onEvent(event: VoiceEvent) {
        when (event) {
            VoiceEvent.Ready -> setVoice(VoiceUiState.Listening(partial = ""))
            is VoiceEvent.Partial -> setVoice(VoiceUiState.Listening(partial = event.text))
            is VoiceEvent.Final -> {
                val matched = symptomsIn(event.text)
                session.select(matched)
                setVoice(VoiceUiState.Heard(sentence = event.text, matched = matched.inPresetOrder()))
            }
            is VoiceEvent.Failed -> setVoice(VoiceUiState.Failed(event.reason))
        }
    }

    private fun stopListening() {
        listenJob?.cancel()
        listenJob = null
        setVoice(VoiceUiState.Idle)
    }

    private fun setVoice(voice: VoiceUiState) = _uiState.update { it.copy(voice = voice) }

    private fun VoiceUiState.isActive() = this == VoiceUiState.Starting || this is VoiceUiState.Listening

    companion object {
        fun factory(
            voiceInput: VoiceInput?,
            session: MedicalCardSession,
            isSimulated: Boolean,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { SymptomVoiceViewModel(voiceInput, session, isSimulated) }
        }
    }
}
