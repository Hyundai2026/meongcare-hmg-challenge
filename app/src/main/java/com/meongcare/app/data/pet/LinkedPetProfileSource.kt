package com.meongcare.app.data.pet

import com.meongcare.app.domain.link.OwnerLinkService
import com.meongcare.app.domain.link.OwnerLinkState
import com.meongcare.app.domain.pet.PetProfile
import com.meongcare.app.domain.pet.PetProfileLoad
import com.meongcare.app.domain.pet.PetProfileSource
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
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
 * - **연결된 뒤 첫 응답 전에 새로 구독하는 화면은 아무것도 받지 않는다** (받는 중). 연결 전의 샘플을 다시 보이지 않는다.
 *   이미 샘플을 보던 화면은 응답이 올 때까지 그대로 보지만, [isSample]이 true로 남아 "시연용 샘플" 안내도 그대로다.
 * - 403이면 [OwnerLinkService.markRevoked]를 부른다 → 연결 상태가 끊김으로 바뀌고 샘플로 돌아간다.
 * - 구독이 있을 때만 읽는다 (마지막 구독이 끝나고 [stopTimeoutMillis] 뒤 멈추고, 마지막 값도 잊는다). 다시 구독하면 다시 읽는다.
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

    private enum class Origin { SAMPLE, SERVER, PLACEHOLDER }

    /**
     * 내보낸 값과 출처. [load]가 null이면 서버 응답을 기다리는 중이다 (밖으로 내보내지 않는다).
     * [seq]는 값마다 새 번호라서, 같은 내용을 다시 읽은 것과 연결 상태만 바뀐 것을 구분한다.
     */
    private data class Shown(val load: PetProfileLoad?, val origin: Origin, val seq: Long)

    private val counter = AtomicLong()

    private fun shownOf(load: PetProfileLoad?, origin: Origin) = Shown(load, origin, counter.incrementAndGet())

    private val refreshes = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private val shown: SharedFlow<Shown> = link.state
        .flatMapLatest { state ->
            when (state) {
                is OwnerLinkState.Linked -> linkedProfile()
                else -> if (sampleBeforeLink) {
                    sample.profile.map { shownOf(it, Origin.SAMPLE) }
                } else {
                    flow { emit(shownOf(PetProfileLoad.NotRegistered, Origin.PLACEHOLDER)) }
                }
            }
        }
        // 멈추면 마지막 값을 잊는다: 다시 구독할 때 연결 전의 샘플이 먼저 나오지 않게
        .shareIn(scope, SharingStarted.WhileSubscribed(stopTimeoutMillis, replayExpirationMillis = 0), replay = 1)

    /**
     * 지금 연결 상태에 맞는 값만 보인다. 연결 상태가 바뀐 직후 [shown]이 아직 앞 상태의 값을 들고 있을 수 있어서
     * (다른 코루틴이 바꾼다), 연결됨이면 서버 값만, 아니면 서버 값이 아닌 것만 통과시킨다.
     */
    private val visible: Flow<Shown> = combine(link.state, shown) { state, value ->
        value.takeIf { it.load != null && it.fits(state) }
    }.filterNotNull().distinctUntilChangedBy { it.seq }

    private fun Shown.fits(state: OwnerLinkState): Boolean =
        if (state is OwnerLinkState.Linked) origin == Origin.SERVER else origin != Origin.SERVER

    override val profile: Flow<PetProfileLoad> = visible.mapNotNull { it.load }

    /** 지금 보이는 프로필이 샘플에서 왔는지. 서버 응답을 기다리는 동안은 앞의 값을 따른다 */
    val isSample: Flow<Boolean> = shown.filter { it.load != null }.map { it.origin == Origin.SAMPLE }

    /** 연결됐으면 서버 프로필을 다시 읽는다 ([진료 카드 만들기], 처음 화면이 보이는 동안). 연결 전이면 아무 일도 없다 */
    fun refresh() {
        refreshes.tryEmit(Unit)
    }

    private fun linkedProfile(): Flow<Shown> = flow {
        emit(shownOf(null, Origin.SERVER))
        readOnce()?.let { emit(shownOf(it, Origin.SERVER)) }
        refreshes.collect { readOnce()?.let { emit(shownOf(it, Origin.SERVER)) } }
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
