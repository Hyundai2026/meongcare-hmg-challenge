package com.meongcare.app.domain.trip

import com.meongcare.app.domain.card.Symptom
import com.meongcare.app.domain.card.inPresetOrder
import com.meongcare.app.domain.pet.PetProfile
import java.time.Instant

/**
 * 병원에 보내는 진료 카드 (docs/data/emergency-case.md `card`).
 *
 * **사진과 차의 위치는 보내지 않는다** (사용자 결정 2026-11-06, 계획 012 U3). [caseCardOf]가 사진을 지운다.
 * 차의 위치는 이 타입에 자리가 없다.
 */
data class CaseCard(
    /** 사진(`photoDataUrl`)을 지운 프로필. 프로필을 받지 못했으면 null */
    val pet: PetProfile?,
    /** 프리셋 순서 */
    val symptoms: List<Symptom>,
)

/** 카드를 만든다. 사진을 지우고 증상을 프리셋 순서로 둔다. */
fun caseCardOf(pet: PetProfile?, symptoms: Set<Symptom>): CaseCard =
    CaseCard(pet = pet?.copy(photoDataUrl = null), symptoms = symptoms.inPresetOrder())

/** 환자 건 상태 (docs/data/emergency-case.md `status`). */
enum class CaseStatus {
    /** 오는 중 */
    EN_ROUTE,

    /** 도착 */
    ARRIVED,

    /** 취소 (앱 [안내 취소] 또는 내비 "안내 종료") */
    CANCELLED,
}

/**
 * 도착 예정.
 * @param remainingMinutes 남은 분 (화면과 같은 올림, 최소 1). null이면 경로를 찾는 중
 * @param measuredAt 이 값을 잰 시각 (차 시계). 도착 예정 시각 = [measuredAt] + 남은 분
 */
data class CaseEta(val remainingMinutes: Int?, val measuredAt: Instant) {
    /** 도착 예정 시각. 남은 분을 모르면 null */
    val arrivesAt: Instant? get() = remainingMinutes?.let { measuredAt.plusSeconds(it * 60L) }
}

/** 보낸 환자 건의 주소: `hospitals/{hospitalId}/cases/{caseId}` */
data class CaseRef(val hospitalId: String, val caseId: String)
