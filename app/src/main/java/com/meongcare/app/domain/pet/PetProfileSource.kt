package com.meongcare.app.domain.pet

import kotlinx.coroutines.flow.Flow

/**
 * 반려견 프로필 출처. 화면은 이 인터페이스만 안다.
 * - 연결 전: 시연용 샘플 반려견(왕건이·오숑이 중 고른 것, 021). Real 모드는 등록 안 됨.
 * - 연결 뒤: 서버(보호자 웹이 저장한 프로필, 014).
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
