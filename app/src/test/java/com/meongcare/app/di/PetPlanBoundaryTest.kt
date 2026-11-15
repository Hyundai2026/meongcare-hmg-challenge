package com.meongcare.app.di

import com.meongcare.app.domain.link.OwnerLinkState
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/** 계획 014 설계 5절 "데이터 모드" 표의 모든 칸 */
class PetPlanBoundaryTest {

    private val linked = OwnerLinkState.Linked(Instant.parse("2026-11-06T09:00:00Z"))
    private val allStates = listOf(OwnerLinkState.Unavailable, OwnerLinkState.Unlinked, linked, OwnerLinkState.Revoked)

    @Test
    fun `서버 설정이 없으면 어느 모드도 서버 연결을 쓰지 않는다`() {
        for (mode in DataMode.entries) {
            assertNotEquals(mode.name, LinkKind.SERVER, petPlanOf(mode, hasServer = false).link)
        }
    }

    @Test
    fun `Fake 모드는 서버 설정이 있어도 서버에 연결하지 않는다`() {
        assertEquals(LinkKind.FAKE, petPlanOf(DataMode.FAKE, hasServer = true).link)
    }

    @Test
    fun `연결 전 반려견은 Real만 등록 안 됨이고 나머지는 샘플다`() {
        for (mode in DataMode.entries) {
            for (hasServer in listOf(false, true)) {
                assertEquals("$mode, 서버 $hasServer", mode != DataMode.REAL, petPlanOf(mode, hasServer).sampleBeforeLink)
            }
        }
    }

    @Test
    fun `Fake 연결은 어느 연결 상태에서도 샘플 안내를 보인다`() {
        val plan = petPlanOf(DataMode.FAKE, hasServer = true)
        for (state in allStates) assertEquals("$state", true, petProfileIsSampleOf(plan, state))
    }

    @Test
    fun `Real은 어느 연결 상태에서도 샘플 안내를 보이지 않는다`() {
        for (hasServer in listOf(false, true)) {
            val plan = petPlanOf(DataMode.REAL, hasServer)
            for (state in allStates) assertEquals("서버 $hasServer, $state", false, petProfileIsSampleOf(plan, state))
        }
    }

    @Test
    fun `시연과 서버에서 샘플 안내는 서버 연결로 받은 동안에만 사라진다`() {
        val plan = petPlanOf(DataMode.DEMO, hasServer = true)
        assertEquals(true, petProfileIsSampleOf(plan, OwnerLinkState.Unavailable))
        assertEquals(true, petProfileIsSampleOf(plan, OwnerLinkState.Unlinked))
        assertEquals(false, petProfileIsSampleOf(plan, linked))
        // 보호자가 끊으면 샘플로 돌아가므로 다시 샘플 안내
        assertEquals(true, petProfileIsSampleOf(plan, OwnerLinkState.Revoked))
    }

    @Test
    fun `시연에서 서버가 없으면 지금처럼 늘 샘플 안내다`() {
        val plan = petPlanOf(DataMode.DEMO, hasServer = false)
        for (state in allStates) assertEquals("$state", true, petProfileIsSampleOf(plan, state))
    }
}
