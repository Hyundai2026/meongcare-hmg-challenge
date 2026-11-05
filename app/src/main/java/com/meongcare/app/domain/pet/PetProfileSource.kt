package com.meongcare.app.domain.pet

import kotlinx.coroutines.flow.Flow

/**
 * 반려견 프로필 출처. 화면은 이 인터페이스만 안다.
 * - 지금: Fake("보리", 시연용 샘플). 모든 데이터 모드에서 쓴다.
 * - 나중: 서버(보호자 웹이 저장한 프로필). 문의 A1 이후 (docs/architecture.md "보호자 웹").
 */
interface PetProfileSource {
    /** 구독하면 현재 값을 먼저 내보내고, 바뀔 때마다 새 값을 내보낸다. */
    val profile: Flow<PetProfileLoad>
}

sealed interface PetProfileLoad {
    data class Loaded(val profile: PetProfile) : PetProfileLoad

    /** 보호자가 아직 등록하지 않았다. */
    data object NotRegistered : PetProfileLoad

    /** 읽지 못했다 (나중: 네트워크, 모르는 형식 버전, 손상). 짐작해서 보여 주지 않는다. */
    data object Unavailable : PetProfileLoad
}
