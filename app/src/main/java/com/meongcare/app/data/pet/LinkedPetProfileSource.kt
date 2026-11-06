package com.meongcare.app.data.pet

import com.meongcare.app.domain.link.OwnerLinkService
import com.meongcare.app.domain.link.OwnerLinkState
import com.meongcare.app.domain.pet.PetProfile
import com.meongcare.app.domain.pet.PetProfileLoad
import com.meongcare.app.domain.pet.PetProfileSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.shareIn

/** 연결된 보호자의 프로필을 한 번 읽은 결과 (docs/data/owner-link.md "차가 읽을 때"). */
sealed interface LinkedProfileRead {
    data class Loaded(val profile: PetProfile) : LinkedProfileRead

    /** 404: 보호자가 프로필을 지웠다 */
    data object NotRegistered : LinkedProfileRead

    /** 403: 연결 허가가 없어졌다 (보호자나 차가 끊었다) */
    data object Revoked : LinkedProfileRead

    /** 네트워크·5xx·형식 오류 */
    data object Unavailable : LinkedProfileRead
}

/** 연결된 보호자의 프로필을 읽는 쪽 (서버). 실패는 결과 값이다 */
fun interface LinkedProfileReader {
    suspend fun read(): LinkedProfileRead
}

/**
 * 연결 상태에 따라 반려견 프로필 출처를 바꾼다 (014). 순수 Kotlin이라 JVM으로 시험한다.
 *
 * | 연결 상태 | 내보내는 것 |
 * |---|---|
 * | 연결 없음·연결 전·끊김 | [sampleBeforeLink]면 샘플([sample]), 아니면 등록 안 됨 |
 * | 연결됨 | 서버 프로필. 연결 직후 한 번, 그 뒤 [refresh]마다 읽는다 |
 *
 * - **연결된 뒤 읽지 못하면 샘플로 대신하지 않는다** (Unavailable). 틀린 의료정보를 보이는 것보다 안전하다.
 * - 403이면 [OwnerLinkService.markRevoked]를 부른다 → 연결 상태가 끊김으로 바뀌고 샘플로 돌아간다.
 * - 구독이 있을 때만 읽는다 (마지막 구독이 끝나고 [stopTimeoutMillis] 뒤 멈춘다). 다시 구독하면 다시 읽는다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LinkedPetProfileSource(
    scope: CoroutineScope,
    private val link: OwnerLinkService,
    private val sample: PetProfileSource,
    private val reader: LinkedProfileReader,
    private val sampleBeforeLink: Boolean,
    stopTimeoutMillis: Long = 5_000,
) : PetProfileSource {

    private val refreshes = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    override val profile: SharedFlow<PetProfileLoad> = link.state
        .flatMapLatest { state ->
            when (state) {
                is OwnerLinkState.Linked -> linkedProfile()
                else -> if (sampleBeforeLink) sample.profile else flowOf(PetProfileLoad.NotRegistered)
            }
        }
        .shareIn(scope, SharingStarted.WhileSubscribed(stopTimeoutMillis), replay = 1)

    /** 연결됐으면 서버 프로필을 다시 읽는다 ([진료 카드 만들기], 처음 화면이 보이는 동안). 연결 전이면 아무 일도 없다 */
    fun refresh() {
        refreshes.tryEmit(Unit)
    }

    private fun linkedProfile(): Flow<PetProfileLoad> = flow {
        readOnce()?.let { emit(it) }
        refreshes.collect { readOnce()?.let { emit(it) } }
    }

    /** null이면 끊김이다: 상태가 바뀌면 [flatMapLatest]가 샘플로 바꾼다 */
    private suspend fun readOnce(): PetProfileLoad? = when (val read = reader.read()) {
        is LinkedProfileRead.Loaded -> PetProfileLoad.Loaded(read.profile)
        LinkedProfileRead.NotRegistered -> PetProfileLoad.NotRegistered
        LinkedProfileRead.Unavailable -> PetProfileLoad.Unavailable
        LinkedProfileRead.Revoked -> {
            link.markRevoked()
            null
        }
    }
}
