package com.meongcare.app.data.pet

import com.meongcare.app.domain.pet.PetProfileLoad
import com.meongcare.app.domain.pet.PetProfileSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 메모리에 든 반려견 프로필. 처음에는 시연용 샘플 "보리"다. */
class FakePetProfileSource(
    initial: PetProfileLoad = PetProfileLoad.Loaded(SamplePetProfiles.demoPet),
) : PetProfileSource {

    private val state = MutableStateFlow(initial)

    override val profile: StateFlow<PetProfileLoad> = state.asStateFlow()

    /** 테스트와 debug에서 값을 바꾼다. */
    fun set(value: PetProfileLoad) {
        state.value = value
    }
}
