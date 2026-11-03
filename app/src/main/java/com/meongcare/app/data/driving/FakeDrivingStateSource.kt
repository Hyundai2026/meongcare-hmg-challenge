package com.meongcare.app.data.driving

import com.meongcare.app.domain.driving.DrivingState
import com.meongcare.app.domain.driving.DrivingStateSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 손으로 주행 상태를 바꾸는 Fake. 처음에는 정차 상태다. */
class FakeDrivingStateSource(
    initial: DrivingState = DrivingState.PARKED,
) : DrivingStateSource {

    private val state = MutableStateFlow(initial)

    override val drivingState: StateFlow<DrivingState> = state.asStateFlow()

    fun set(newState: DrivingState) {
        state.value = newState
    }
}
