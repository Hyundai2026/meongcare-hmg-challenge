package com.meongcare.app.data.link

import com.meongcare.app.domain.link.LinkCheck
import com.meongcare.app.domain.link.LinkCode
import com.meongcare.app.domain.link.LinkStart
import com.meongcare.app.domain.link.OwnerLinkService
import com.meongcare.app.domain.link.OwnerLinkState
import java.time.Instant
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 메모리 보호자 연결 (014). 서버 없이 연결 화면을 확인하고(GPU 없는 PC), 테스트에 쓴다.
 * 보호자의 코드 입력은 [claim]으로 흉내 낸다 (debug 패널 [가짜 보호자 코드 입력]).
 */
class FakeOwnerLink(
    private val random: Random = Random.Default,
    private val now: () -> Instant = Instant::now,
) : OwnerLinkService {

    private val mutableState = MutableStateFlow<OwnerLinkState>(OwnerLinkState.Unlinked)
    override val state: StateFlow<OwnerLinkState> = mutableState.asStateFlow()

    private val mutablePending = MutableStateFlow<LinkCode?>(null)

    /** 보호자가 아직 입력하지 않은 코드. 없으면 null. debug 패널이 버튼을 보일지 정한다 */
    val pendingCode: StateFlow<LinkCode?> = mutablePending.asStateFlow()

    private var claimed: LinkCode? = null

    override suspend fun start(): LinkStart {
        val code = LinkCode.generate(random)
        mutablePending.value = code
        claimed = null
        return LinkStart.Created(code, now())
    }

    override suspend fun check(code: LinkCode): LinkCheck = when {
        claimed == code -> {
            claimed = null
            mutablePending.value = null
            mutableState.value = OwnerLinkState.Linked(now())
            LinkCheck.Linked
        }
        mutablePending.value == code -> LinkCheck.Waiting
        else -> LinkCheck.Gone
    }

    override suspend fun cancel(code: LinkCode) {
        if (mutablePending.value == code) mutablePending.value = null
        if (claimed == code) claimed = null
    }

    override suspend fun unlink(): Boolean {
        mutableState.value = OwnerLinkState.Unlinked
        return true
    }

    override fun markRevoked() {
        if (mutableState.value is OwnerLinkState.Linked) mutableState.value = OwnerLinkState.Revoked
    }

    override fun acknowledgeRevoked() {
        if (mutableState.value == OwnerLinkState.Revoked) mutableState.value = OwnerLinkState.Unlinked
    }

    /**
     * 보호자가 웹에 코드를 입력한 것처럼 한다. 기다리는 코드가 없으면(취소·만료 뒤) 아무 일도 없다.
     * @return 받아들였으면 true
     */
    fun claim(code: LinkCode? = mutablePending.value): Boolean {
        if (code == null || code != mutablePending.value) return false
        claimed = code
        return true
    }
}
