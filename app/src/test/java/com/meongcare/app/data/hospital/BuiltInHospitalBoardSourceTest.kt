package com.meongcare.app.data.hospital

import com.meongcare.app.domain.hospital.HospitalBoard
import com.meongcare.app.domain.hospital.HospitalOrigin
import com.meongcare.app.domain.hospital.rankHospitals
import com.meongcare.app.domain.location.CurrentLocation
import java.time.Instant
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BuiltInHospitalBoardSourceTest {

    private val now = Instant.parse("2026-11-06T05:00:00Z")

    @Test
    fun `시연 병원 5곳을 내장 출처로 준다`() = runTest {
        val board = BuiltInHospitalBoardSource(now = { now }).load() as HospitalBoard.Loaded
        assertEquals(HospitalOrigin.BUILT_IN, board.origin)
        assertEquals(
            listOf("demo-fast", "demo-main", "demo-near", "demo-stale", "demo-closed"),
            board.candidates.map { it.hospital.id },
        )
        assertTrue(board.candidates.all { it.origin == HospitalOrigin.BUILT_IN && it.hospital.isSample })
        assertTrue(board.candidates.none { it.sendsCardToServer })
    }

    @Test
    fun `갱신 시각은 지금에서 시드 분만큼 뺀 값이다`() = runTest {
        var clock = now
        val source = BuiltInHospitalBoardSource(now = { clock })
        val first = source.load() as HospitalBoard.Loaded
        assertEquals(now.minusSeconds(2 * 60), first.candidates.first { it.hospital.id == "demo-fast" }.status?.updatedAt)
        assertEquals(now.minusSeconds(90 * 60), first.candidates.first { it.hospital.id == "demo-stale" }.status?.updatedAt)

        clock = now.plusSeconds(3600)
        val later = source.load() as HospitalBoard.Loaded
        assertEquals(clock.minusSeconds(2 * 60), later.candidates.first { it.hospital.id == "demo-fast" }.status?.updatedAt)
    }

    @Test
    fun `대체 출처로 만들 수 있다`() = runTest {
        val board = BuiltInHospitalBoardSource(HospitalOrigin.BUILT_IN_FALLBACK, now = { now }).load() as HospitalBoard.Loaded
        assertEquals(HospitalOrigin.BUILT_IN_FALLBACK, board.origin)
        assertTrue(board.candidates.all { it.origin == HospitalOrigin.BUILT_IN_FALLBACK })
    }

    @Test(expected = IllegalArgumentException::class)
    fun `서버 출처로는 만들 수 없다`() {
        BuiltInHospitalBoardSource(HospitalOrigin.SERVER)
    }

    @Test
    fun `시연 위치에서 시연 병원 5곳은 바른, 시연용, 가까운 순이다`() = runTest {
        // docs/data/hospital-status.md "시연 병원 5곳" 표
        val board = BuiltInHospitalBoardSource(now = { now }).load() as HospitalBoard.Loaded
        val ranking = rankHospitals(board.candidates, CurrentLocation(37.4127212, 127.094681, "수정구 시흥동"), now)
        assertEquals(listOf("demo-fast", "demo-main", "demo-near"), ranking.ranked.map { it.hospital.id })
        assertEquals(listOf(29.minutes, 40.minutes, 69.minutes), ranking.ranked.map { it.total })
        assertEquals(listOf("demo-stale"), ranking.unranked.map { it.hospital.id })
        assertEquals(listOf("demo-closed"), ranking.notAccepting.map { it.hospital.id })
    }
}
