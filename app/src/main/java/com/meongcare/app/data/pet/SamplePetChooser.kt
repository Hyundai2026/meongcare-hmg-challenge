package com.meongcare.app.data.pet

import com.meongcare.app.domain.pet.PetProfileLoad
import com.meongcare.app.domain.pet.PetProfileSource
import com.meongcare.app.domain.pet.SamplePet
import com.meongcare.app.domain.pet.SamplePetChoice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

/**
 * 시연용 샘플 반려견 고르기 (021). 고른 반려견의 프로필을 [profile]로 내보낸다.
 * 고른 값은 메모리에만 둔다: 앱을 다시 켜면 첫째(왕건이)다 (021 D5).
 */
class SamplePetChooser(
    override val pets: List<SamplePet> = SamplePetProfiles.all,
) : SamplePetChoice, PetProfileSource {

    init {
        require(pets.isNotEmpty()) { "샘플 반려견이 하나는 있어야 한다" }
    }

    private val state = MutableStateFlow(pets.first())

    override val selected: StateFlow<SamplePet> = state.asStateFlow()

    override val profile: Flow<PetProfileLoad> = state.map { PetProfileLoad.Loaded(it.profile) }

    override fun select(id: String) {
        pets.firstOrNull { it.id == id }?.let { state.value = it }
    }

    override fun selectNext() {
        val index = pets.indexOf(state.value)
        state.value = pets[(index + 1) % pets.size]
    }
}
