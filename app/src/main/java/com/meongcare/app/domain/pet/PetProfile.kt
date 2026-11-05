package com.meongcare.app.domain.pet

import java.time.Instant
import java.time.LocalDate

/**
 * 반려견 프로필. 형식의 기준은 docs/data/pet-profile.md다 (보호자 웹·차 앱·서버 공통).
 *
 * - `schemaVersion`은 두지 않는다. JSON을 읽는 쪽(나중 서버 구현)이 버전을 확인하고, 모르는 버전이면
 *   [PetProfileLoad.Unavailable]을 낸다. 화면은 버전을 알 필요가 없다.
 * - 빈 목록은 "없음"이 아니라 "등록하지 않음"이다.
 */
data class PetProfile(
    val name: String,
    val breed: String?,
    val sex: Sex?,
    val birthDate: LocalDate?,
    /** 나이로 입력해서 생년월일이 추정값이면 true */
    val birthDateIsEstimated: Boolean,
    /** 사진 자리. 지금 차 앱은 그리지 않는다 (이름 첫 글자 아바타). */
    val photoDataUrl: String?,
    val medical: MedicalInfo,
    /** 보호자가 마지막으로 저장한 시각 */
    val updatedAt: Instant,
)

enum class Sex { MALE, FEMALE }

data class MedicalInfo(
    /** 주요 질환 (카드의 "기존 병력") */
    val conditions: List<String>,
    val allergies: List<String>,
    val medications: List<Medication>,
)

/** [dosage]는 복용법 자유 입력 (예: "1일 1회"). 없으면 "" */
data class Medication(val name: String, val dosage: String)
