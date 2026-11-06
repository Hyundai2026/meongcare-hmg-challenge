package com.meongcare.app.ui.home

import com.meongcare.app.domain.card.CardSessionState
import com.meongcare.app.domain.card.Symptom
import com.meongcare.app.domain.link.OwnerLinkState
import com.meongcare.app.domain.trip.FinderStep
import com.meongcare.app.ui.guidance.GuidanceUiState
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * 계획 014 K3 (연결은 안내가 없을 때 처음 화면에서만), AGENTS.md 규칙 3 (주행 중에는 연결 화면 없음),
 * 설계 5절 "처음 화면 연결 줄"의 경계.
 */
class ParkedContentLinkBoundaryTest {

    private val idle = GuidanceUiState.Idle("시연용 동물병원")
    private val guiding = GuidanceUiState.Guiding("시연용 동물병원", 12)
    private val arrived = GuidanceUiState.Arrived("시연용 동물병원")
    private val linked = OwnerLinkState.Linked(Instant.parse("2026-11-06T09:00:00Z"))
    private val allLinkStates = listOf(OwnerLinkState.Unavailable, OwnerLinkState.Unlinked, linked, OwnerLinkState.Revoked)

    @Test
    fun `도착했으면 연결 화면이 열려 있어도 보이지 않는다`() {
        assertNotEquals(ParkedContent.LINK, parkedContentOf(arrived, CardSessionState.None, linking = true))
        assertNotEquals(ParkedContent.LINK, parkedContentOf(arrived, CardSessionState.Ready(setOf(Symptom.COUGH)), linking = true))
        assertNotEquals(ParkedContent.LINK, parkedContentOf(arrived, CardSessionState.None, atHome = true, linking = true))
    }

    @Test
    fun `안내 중에는 처음 화면을 보는 중이어도 연결 화면이 아니다`() {
        assertNotEquals(ParkedContent.LINK, parkedContentOf(guiding, CardSessionState.None, atHome = true, linking = true))
    }

    @Test
    fun `안내 중이거나 도착했으면 연결 줄은 어느 연결 상태에서도 숨는다`() {
        for (guidance in listOf(guiding, arrived)) {
            for (state in allLinkStates) assertEquals("$guidance, $state", LinkRow.Hidden, linkRowOf(state, guidance))
        }
    }

    @Test
    fun `카드를 만든 뒤 처음 화면에서 연결 화면을 열면 연결 화면이다`() {
        val ready = CardSessionState.Ready(setOf(Symptom.COUGH))
        assertEquals(ParkedContent.LINK, parkedContentOf(idle, ready, atHome = true, linking = true))
        // 닫으면 처음 화면으로 돌아간다
        assertEquals(ParkedContent.HOME, parkedContentOf(idle, ready, atHome = true, linking = false))
    }

    @Test
    fun `연결 화면이 열려 있어도 주행이면 안내 상태와 상관없이 주행 화면이다`() {
        for (guidance in listOf(idle, guiding, arrived)) {
            assertEquals("$guidance", HomeContent.DRIVING, homeContentOf(HomeUiState.Driving, guidance))
        }
    }

    @Test
    fun `정차하면 열려 있던 연결 화면으로 돌아온다`() {
        assertEquals(HomeContent.PARKED, homeContentOf(HomeUiState.Parked, idle))
        assertEquals(ParkedContent.LINK, parkedContentOf(idle, CardSessionState.None, linking = true))
    }

    @Test
    fun `연결을 쓸 수 없으면 안내가 없어도 연결 줄이 없다`() {
        assertEquals(LinkRow.Hidden, linkRowOf(OwnerLinkState.Unavailable, idle))
    }

    @Test
    fun `보호자가 끊은 것은 연결 버튼과 함께 알린다`() {
        val row = linkRowOf(OwnerLinkState.Revoked, idle)
        assertEquals(LinkRow.Unlinked(revoked = true), row)
        // 끊김 확인 뒤(연결 전)에는 알림 없이 버튼만
        assertEquals(LinkRow.Unlinked(revoked = false), linkRowOf(OwnerLinkState.Unlinked, idle))
    }

    @Test
    fun `증상을 고르는 중이면 병원 찾기 단계가 있어도 고르기가 먼저다`() {
        val picking = CardSessionState.Picking(setOf(Symptom.COUGH), previous = setOf(Symptom.COUGH))
        assertEquals(ParkedContent.SYMPTOMS, parkedContentOf(idle, picking, finder = FinderStep.Hospitals, linking = true))
    }
}
