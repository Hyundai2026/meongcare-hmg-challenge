package com.meongcare.app.ui.pet

/** 고를 수 있는 샘플 반려견 하나 (ui 쪽 값. `MainActivity`가 샘플 목록에서 만든다, 021) */
data class PetChoiceItem(val id: String, val name: String)

/** 고르기 탭 하나. [selected]는 지금 고른 반려견 */
data class PetChoiceTab(val id: String, val name: String, val selected: Boolean)

/**
 * 반려견 고르기 표시 (021). [nextId]는 [⇄ 변경]을 누르면 고를 반려견이다 (끝이면 처음, 프로토타입 `cycle`).
 */
data class PetChoiceUi(val tabs: List<PetChoiceTab>, val nextId: String, val nextName: String)

/**
 * 반려견 고르기를 보일지 (021 "고르기 버튼을 보일지"). null이면 숨긴다.
 *
 * - 두 마리 미만이면 고를 것이 없다.
 * - 반려견 정보가 샘플이 아니면([isSample] false) 숨긴다. 보호자 웹에서 연결한 반려견은 바꾸지 않는다.
 * - 연결된 동안([linked], Fake 연결 포함) 숨긴다.
 * - 안내 중·도착·출발한 trip이 있으면([tripActive]) 숨긴다. 병원에 보낸 카드의 반려견을 바꾸지 않게.
 *
 * 주행 중에는 이 버튼이 있는 정차 화면 자체가 보이지 않는다 (`homeContentOf`).
 */
fun petChoiceOf(
    pets: List<PetChoiceItem>,
    selectedId: String,
    isSample: Boolean,
    linked: Boolean,
    tripActive: Boolean,
): PetChoiceUi? {
    if (pets.size < 2 || !isSample || linked || tripActive) return null
    val index = pets.indexOfFirst { it.id == selectedId }
    val next = pets[(index + 1) % pets.size]
    return PetChoiceUi(
        tabs = pets.map { PetChoiceTab(it.id, it.name, it.id == selectedId) },
        nextId = next.id,
        nextName = next.name,
    )
}
