package com.meongcare.app.data.link

import com.meongcare.app.domain.link.LinkCheck
import com.meongcare.app.domain.link.LinkCode
import com.meongcare.app.domain.link.LinkStart
import com.meongcare.app.domain.link.OwnerLinkService
import com.meongcare.app.domain.link.OwnerLinkState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** 연결을 쓸 수 없는 구성 (014: 서버 설정 없음). 처음 화면에 연결 버튼이 없다. */
object NoOwnerLink : OwnerLinkService {
    override val state: StateFlow<OwnerLinkState> = MutableStateFlow(OwnerLinkState.Unavailable)
    override suspend fun start(): LinkStart = LinkStart.Failed("연결 없음")
    override suspend fun check(code: LinkCode): LinkCheck = LinkCheck.Gone
    override suspend fun cancel(code: LinkCode) = Unit
    override suspend fun unlink(): Boolean = false
    override fun markRevoked() = Unit
    override fun acknowledgeRevoked() = Unit
}
