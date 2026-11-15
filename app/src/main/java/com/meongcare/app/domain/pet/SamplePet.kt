package com.meongcare.app.domain.pet

import kotlinx.coroutines.flow.StateFlow

/**
 * 시연용 샘플 반려견 하나 (021).
 *
 * @param id 프로토타입의 반려견 id (예: "P-DEMO-01"). 사진을 고를 때 쓴다
 * @param weight 체중. 반려견 프로필 형식에 없는 화면 표시용 값이다. 없으면 null
 * @param traits 특징. 형식에 없는 화면 표시용 값이다. 없으면 null
 */
data class SamplePet(
    val id: String,
    val profile: PetProfile,
    val weight: String?,
    val traits: String?,
)

/**
 * 시연용 샘플 반려견 고르기 (021). 화면은 이 인터페이스만 안다.
 *
 * - [select]: 모르는 id는 무시한다. 이미 고른 id면 값을 다시 내보내지 않는다.
 * - [selectNext]: 다음 반려견으로. 끝이면 처음으로 돈다 (프로토타입 `cycle`). 한 마리면 그대로다.
 */
interface SamplePetChoice {
    val pets: List<SamplePet>
    val selected: StateFlow<SamplePet>
    fun select(id: String)
    fun selectNext()
}
