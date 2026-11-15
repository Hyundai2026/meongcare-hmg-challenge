package com.meongcare.app.di

import com.meongcare.app.domain.link.OwnerLinkState
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 계획 014 설계 5절 "데이터 모드" 표 */
class PetPlanTest {

    private val linked = OwnerLinkState.Linked(Instant.parse("2026-11-06T09:00:00Z"))

    @Test
    fun `Fake는 샘플와 Fake 연결이다`() {
        val expected = PetPlan(LinkKind.FAKE, sampleBeforeLink = true)
        assertEquals(expected, petPlanOf(DataMode.FAKE, hasServer = false))
        assertEquals(expected, petPlanOf(DataMode.FAKE, hasServer = true))
        // Fake 연결은 연결해도 샘플이다
        assertTrue(petProfileIsSampleOf(expected, linked))
    }

    @Test
    fun `시연에서 서버 설정이 없으면 샘플이고 연결이 없다`() {
        val plan = petPlanOf(DataMode.DEMO, hasServer = false)
        assertEquals(PetPlan(LinkKind.NONE, sampleBeforeLink = true), plan)
        assertTrue(petProfileIsSampleOf(plan, OwnerLinkState.Unavailable))
    }

    @Test
    fun `시연에서 서버가 있으면 연결 전 샘플 연결 뒤 서버다`() {
        val plan = petPlanOf(DataMode.DEMO, hasServer = true)
        assertEquals(PetPlan(LinkKind.SERVER, sampleBeforeLink = true), plan)
        assertTrue(petProfileIsSampleOf(plan, OwnerLinkState.Unlinked))
        assertTrue(petProfileIsSampleOf(plan, OwnerLinkState.Revoked))
        assertFalse(petProfileIsSampleOf(plan, linked))
    }

    @Test
    fun `Real은 연결 전 등록 안 됨이다`() {
        val plan = petPlanOf(DataMode.REAL, hasServer = true)
        assertEquals(PetPlan(LinkKind.SERVER, sampleBeforeLink = false), plan)
        assertFalse(petProfileIsSampleOf(plan, OwnerLinkState.Unlinked))
        assertFalse(petProfileIsSampleOf(plan, linked))
    }

    @Test
    fun `Real에서 서버 설정이 없으면 연결이 없다`() {
        val plan = petPlanOf(DataMode.REAL, hasServer = false)
        assertEquals(PetPlan(LinkKind.NONE, sampleBeforeLink = false), plan)
        assertFalse(petProfileIsSampleOf(plan, OwnerLinkState.Unavailable))
    }

    @Test
    fun `release는 시연 표를 따른다`() {
        val release = resolveDataMode(isDebugBuild = false, savedName = "REAL")
        assertEquals(PetPlan(LinkKind.NONE, sampleBeforeLink = true), petPlanOf(release, hasServer = false))
    }
}
