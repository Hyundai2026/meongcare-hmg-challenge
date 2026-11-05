package com.meongcare.app.domain.card

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** 진료 카드 세션 상태. */
sealed interface CardSessionState {
    /** 카드가 없다. */
    data object None : CardSessionState

    /** 증상을 고르는 중. [previous]는 고르기 전 카드의 증상(없었으면 null)이다. [취소]하면 여기로 돌아간다. */
    data class Picking(val draft: Set<Symptom>, val previous: Set<Symptom>?) : CardSessionState

    /** 카드가 있다. 증상 0개도 된다. */
    data class Ready(val symptoms: Set<Symptom>) : CardSessionState
}

/**
 * 고른 증상과 카드를 들고 있는 메모리 객체. `AppContainer`가 하나 만들어 프로세스 내내 쓴다.
 *
 * 안내 시작 → 주행 → 도착까지 카드가 남아야 해서, 같은 프로세스 수명인 Fake 길안내와 수명을 맞췄다
 * (docs/plans 009 "설계 결정 2"). 기기에 저장하지 않으므로 앱 프로세스가 죽으면 사라진다.
 */
class MedicalCardSession {
    private val _state = MutableStateFlow<CardSessionState>(CardSessionState.None)
    val state: StateFlow<CardSessionState> = _state.asStateFlow()

    private val _atHome = MutableStateFlow(false)

    /** [홈]을 눌러 카드를 둔 채 처음 화면을 보는 중인지. 카드를 열거나 고르기를 시작하거나 비우면 false가 된다. */
    val atHome: StateFlow<Boolean> = _atHome.asStateFlow()

    /** [홈]: 카드를 지우지 않고 처음 화면으로. 고르는 중이었으면 고르기를 취소한다. */
    fun goHome() {
        cancelPicking()
        _atHome.value = true
    }

    /** 처음 화면의 [카드 보기] */
    fun openCard() {
        _atHome.value = false
    }

    /** 처음 화면의 [새로 만들기]: 있던 카드를 버리고 빈 선택으로 고른다. */
    fun startNew() {
        clear()
        startPicking()
    }

    /** 고르기 시작. 카드가 있으면 그 증상이 선택된 채로 고른다. 이미 고르는 중이면 그대로. */
    fun startPicking() = _state.update { state ->
        _atHome.value = false
        when (state) {
            CardSessionState.None -> CardSessionState.Picking(draft = emptySet(), previous = null)
            is CardSessionState.Ready -> CardSessionState.Picking(draft = state.symptoms, previous = state.symptoms)
            is CardSessionState.Picking -> state
        }
    }

    /** 고르는 중일 때만: 선택돼 있으면 풀고, 아니면 고른다. */
    fun toggle(symptom: Symptom) = _state.update { state ->
        if (state !is CardSessionState.Picking) return@update state
        val draft = if (symptom in state.draft) state.draft - symptom else state.draft + symptom
        state.copy(draft = draft)
    }

    /**
     * 고르는 중일 때만: [symptoms]를 선택에 더한다. 이미 고른 것은 그대로 둔다 (풀지 않는다).
     * 음성 입력이 찾은 증상을 넣을 때 쓴다 (docs/plans 010 "설계 결정 1": 음성은 더하기만 한다).
     */
    fun select(symptoms: Set<Symptom>) = _state.update { state ->
        if (state !is CardSessionState.Picking) return@update state
        state.copy(draft = state.draft + symptoms)
    }

    /** 고르는 중일 때만: 고른 증상으로 카드를 만든다. */
    fun confirm() = _state.update { state ->
        if (state is CardSessionState.Picking) CardSessionState.Ready(state.draft) else state
    }

    /** 고르는 중일 때만: 고르기 전으로 돌아간다 (카드가 없었으면 없음, 있었으면 그 카드). */
    fun cancelPicking() = _state.update { state ->
        if (state !is CardSessionState.Picking) return@update state
        state.previous?.let { CardSessionState.Ready(it) } ?: CardSessionState.None
    }

    /** 카드를 버린다. */
    fun clear() {
        _state.value = CardSessionState.None
        _atHome.value = false
    }
}
