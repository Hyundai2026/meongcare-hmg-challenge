package com.meongcare.app.ui.home

import com.meongcare.app.domain.card.CardSessionState
import com.meongcare.app.domain.card.Symptom
import com.meongcare.app.domain.link.OwnerLinkState
import com.meongcare.app.domain.trip.FinderStep
import com.meongcare.app.ui.guidance.GuidanceUiState
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

/** 계획 014 단계 3-2: 연결 화면은 정차 중 처음 화면에서만 연다 */
class ParkedContentLinkTest {

    private val idle = GuidanceUiState.Idle("시연용 동물병원")
    private val guiding = GuidanceUiState.Guiding("시연용 동물병원", 12)

    @Test
    fun `연결 단계면 연결 화면이다`() {
        assertEquals(ParkedContent.LINK, parkedContentOf(idle, CardSessionState.None, linking = true))
        assertEquals(ParkedContent.LINK, parkedContentOf(idle, CardSessionState.None, atHome = true, linking = true))
        assertEquals(ParkedContent.HOME, parkedContentOf(idle, CardSessionState.None, linking = false))
    }

    @Test
    fun `증상을 고르는 중이면 연결보다 고르기가 먼저다`() {
        val picking = CardSessionState.Picking(setOf(Symptom.COUGH), previous = null)
        assertEquals(ParkedContent.SYMPTOMS, parkedContentOf(idle, picking, linking = true))
    }

    @Test
    fun `안내 중에는 연결 화면을 보이지 않는다`() {
        assertEquals(ParkedContent.CARD, parkedContentOf(guiding, CardSessionState.None, linking = true))
        assertEquals(LinkRow.Hidden, linkRowOf(OwnerLinkState.Unlinked, guiding))
    }

    @Test
    fun `연결 단계여도 주행이면 주행 화면이다`() {
        // 주행 화면은 parkedContentOf보다 먼저 정해진다 (AGENTS.md 규칙 3)
        assertEquals(HomeContent.DRIVING, homeContentOf(HomeUiState.Driving, idle))
    }

    @Test
    fun `병원 찾기 단계 기본값은 그대로다`() {
        assertEquals(ParkedContent.HOME, parkedContentOf(idle, CardSessionState.None, finder = FinderStep.None))
    }

    @Test
    fun `연결 줄은 연결 상태를 따른다`() {
        assertEquals(LinkRow.Hidden, linkRowOf(OwnerLinkState.Unavailable, idle))
        assertEquals(LinkRow.Unlinked(), linkRowOf(OwnerLinkState.Unlinked, idle))
        assertEquals(LinkRow.Unlinked(revoked = true), linkRowOf(OwnerLinkState.Revoked, idle))
        assertEquals(LinkRow.Linked, linkRowOf(OwnerLinkState.Linked(Instant.EPOCH), idle))
    }
}
