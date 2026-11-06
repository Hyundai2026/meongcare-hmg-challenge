package com.meongcare.app.ui.link

import com.meongcare.app.domain.pet.PetProfileLoad

/** 보호자 웹 연결 화면 (014, 계획 설계 5절 "화면"). 정차 중에만 보인다. */
sealed interface LinkUiState {
    /** 연결 화면을 열지 않았다 */
    data object Closed : LinkUiState

    /** 연결 코드를 만드는 중 */
    data object Creating : LinkUiState

    /**
     * 보호자가 코드를 입력하기를 기다린다.
     * @param code 화면에 보이는 꼴 ("K7P 3QX")
     * @param remainingMinutes "N분 안에 입력해 주세요"의 N. 1 이상 ([shownMinutesOf])
     */
    data class Waiting(val code: String, val remainingMinutes: Int) : LinkUiState

    /** 연결됐다. 반려견 정보를 아직 받지 못했으면 [pet]이 null */
    data class Linked(val pet: LinkedPet?) : LinkUiState

    /** 코드가 만료됐다 (또는 코드가 지워졌다) */
    data object Expired : LinkUiState

    /** 서버에 닿지 못해 코드를 만들지 못했다 */
    data object Failed : LinkUiState
}

/** 연결됨 화면의 반려견 요약 */
data class LinkedPet(val name: String, val breed: String?)

/** 화면의 "N분 안에": 만료 전에는 1분 미만도 "1분 안에"로 보인다 */
fun shownMinutesOf(remainingMinutes: Int): Int = remainingMinutes.coerceAtLeast(1)

/** 받은 프로필 → 연결됨 화면의 요약. 받지 못했으면 null ("반려견 정보를 아직 받지 못했어요") */
fun linkedPetOf(load: PetProfileLoad?): LinkedPet? =
    (load as? PetProfileLoad.Loaded)?.profile?.let { LinkedPet(it.name, it.breed) }
