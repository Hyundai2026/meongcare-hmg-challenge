package com.meongcare.app.data.hospital

import com.meongcare.app.domain.hospital.HospitalBoard
import com.meongcare.app.domain.hospital.HospitalBoardSource
import com.meongcare.app.domain.hospital.HospitalOrigin
import com.meongcare.app.domain.hospital.StatusFreshness
import com.meongcare.app.domain.hospital.TravelEstimate
import com.meongcare.app.domain.hospital.rankHospitals
import com.meongcare.app.domain.location.CurrentLocation
import java.time.Instant
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 내장·대체 병원 목록 경계. 기준: 계획 012 "설계 결정 4"(대체 규칙, 5초), "설계 결정 5"(시연 병원 5곳 표),
 * docs/data/hospital-status.md "시연 병원 5곳", `HospitalBoardSource` KDoc(부를 때마다 새로 받는다, 취소 말고는 던지지 않는다).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HospitalBoardSourcesBoundaryTest {

    private val demoLocation = CurrentLocation(37.4127212, 127.094681, "수정구 시흥동")
    private val t0 = Instant.parse("2026-11-06T05:00:00Z")

    private suspend fun HospitalBoardSource.loaded() = load() as HospitalBoard.Loaded

    @Test
    fun `부를 때마다 지금 시각으로 알린 시각을 새로 만든다`() = runTest {
        var clock = t0
        val source = BuiltInHospitalBoardSource(now = { clock })
        val first = source.loaded().candidates.associate { it.hospital.id to it.status!!.updatedAt }
        clock = t0.plusSeconds(3600)
        val second = source.loaded().candidates.associate { it.hospital.id to it.status!!.updatedAt }
        first.forEach { (id, at) -> assertEquals(id, at.plusSeconds(3600), second.getValue(id)) }
    }

    @Test
    fun `정보 지난 병원은 언제 받아도 지난 정보이고 나머지는 언제 받아도 유효하다`() = runTest {
        listOf(t0, t0.plusSeconds(86_400 * 30L), Instant.parse("2027-01-01T00:00:00Z")).forEach { at ->
            val board = BuiltInHospitalBoardSource(now = { at }).loaded()
            val ranking = rankHospitals(board.candidates, demoLocation, at)
            assertEquals(at.toString(), listOf("demo-stale"), ranking.unranked.map { it.hospital.id })
            assertEquals(at.toString(), listOf("demo-closed"), ranking.notAccepting.map { it.hospital.id })
        }
    }

    @Test
    fun `시연 병원은 모두 시연용이고 id와 전화번호가 겹치지 않는다`() = runTest {
        val hospitals = BuiltInHospitalBoardSource(now = { t0 }).loaded().candidates.map { it.hospital }
        assertTrue(hospitals.all { it.isSample })
        assertEquals(5, hospitals.mapNotNull { it.id }.toSet().size)
        assertEquals(setOf("02-0000-0000", "02-0000-0001", "02-0000-0002", "02-0000-0003", "02-0000-0004"), hospitals.mapNotNull { it.phoneNumber }.toSet())
        assertTrue(hospitals.all { it.id!!.matches(Regex("[a-z0-9-]+")) })
    }

    @Test
    fun `시연 병원은 모두 시연 위치에서 1점5km보다 멀다`() = runTest {
        BuiltInHospitalBoardSource(now = { t0 }).loaded().candidates.forEach {
            val d = it.hospital.destination
            val km = TravelEstimate.straightDistanceKm(demoLocation.latitude, demoLocation.longitude, d.latitude, d.longitude)
            assertTrue("${it.hospital.id}: $km km", km > 1.5)
        }
    }

    @Test
    fun `시연 위치에서 직선 거리와 이동 어림이 형식 문서의 표와 같다`() = runTest {
        // docs/data/hospital-status.md "시연 병원 5곳" 표
        val table = mapOf(
            "demo-fast" to (5.22 to 19),
            "demo-main" to (9.59 to 35),
            "demo-near" to (2.48 to 9),
            "demo-stale" to (8.32 to 30),
            "demo-closed" to (2.90 to 11),
        )
        val candidates = BuiltInHospitalBoardSource(now = { t0 }).loaded().candidates
        assertEquals(table.keys, candidates.map { it.hospital.id }.toSet())
        candidates.forEach {
            val d = it.hospital.destination
            val km = TravelEstimate.straightDistanceKm(demoLocation.latitude, demoLocation.longitude, d.latitude, d.longitude)
            val (expectedKm, expectedMinutes) = table.getValue(it.hospital.id!!)
            assertEquals(it.hospital.id, expectedKm, km, 0.005)
            assertEquals(it.hospital.id, expectedMinutes, TravelEstimate.minutesForKm(km))
        }
    }

    @Test
    fun `시연 위치에서 순위 합계는 29, 40, 69분이다`() = runTest {
        val board = BuiltInHospitalBoardSource(now = { t0 }).loaded()
        val ranking = rankHospitals(board.candidates, demoLocation, t0)
        assertEquals(listOf(29.minutes, 40.minutes, 69.minutes), ranking.ranked.map { it.total })
        assertEquals("demo-fast", ranking.fastest?.hospital?.id)
    }

    @Test
    fun `위치를 모르면 시연 병원은 대기 시간 순으로 시연용, 바른, 가까운이다`() = runTest {
        val board = BuiltInHospitalBoardSource(now = { t0 }).loaded()
        val ranking = rankHospitals(board.candidates, from = null, now = t0)
        assertEquals(listOf("demo-main", "demo-fast", "demo-near"), ranking.ranked.map { it.hospital.id })
        assertEquals(listOf("demo-stale"), ranking.unranked.map { it.hospital.id })
        assertEquals(StatusFreshness.STALE, ranking.unranked.single().freshness)
    }

    @Test
    fun `대체한 목록의 병원은 하나하나가 대체 출처이고 서버로 보내지 않는다`() = runTest {
        val board = FallbackHospitalBoardSource(server = { HospitalBoard.Unavailable("네트워크") }, now = { t0 }).loaded()
        assertEquals(HospitalOrigin.BUILT_IN_FALLBACK, board.origin)
        assertTrue(board.candidates.all { it.origin == HospitalOrigin.BUILT_IN_FALLBACK })
        assertTrue(board.candidates.none { it.sendsCardToServer })
    }

    @Test
    fun `대체한 목록의 알린 시각은 넣어 준 시계를 따른다`() = runTest {
        val board = FallbackHospitalBoardSource(server = { HospitalBoard.Unavailable("x") }, now = { t0 }).loaded()
        val stale = board.candidates.single { it.hospital.id == "demo-stale" }
        assertEquals(t0.minusSeconds(90 * 60), stale.status!!.updatedAt)
    }

    @Test
    fun `서버가 시간을 넘기면 서버 요청을 멈춘다`() = runTest {
        var serverCancelled = false
        val source = FallbackHospitalBoardSource(
            server = {
                try {
                    awaitCancellation()
                } catch (e: CancellationException) {
                    serverCancelled = true
                    throw e
                }
            },
            now = { t0 },
        )
        val result = async { source.load() }
        advanceTimeBy(5_001)
        runCurrent()
        assertEquals(HospitalOrigin.BUILT_IN_FALLBACK, (result.await() as HospitalBoard.Loaded).origin)
        assertTrue(serverCancelled)
    }

    @Test
    fun `서버가 실패해도 다음에 부르면 다시 서버에 묻는다`() = runTest {
        var calls = 0
        var fail = true
        val serverBoard = HospitalBoard.Loaded(emptyList(), HospitalOrigin.SERVER)
        val source = FallbackHospitalBoardSource(
            server = {
                calls++
                if (fail) HospitalBoard.Unavailable("HTTP 503") else serverBoard
            },
            now = { t0 },
        )
        assertEquals(HospitalOrigin.BUILT_IN_FALLBACK, source.loaded().origin)
        fail = false
        assertEquals(HospitalOrigin.SERVER, source.loaded().origin)
        assertEquals(2, calls)
    }

    @Test
    fun `대체 로그에는 이유만 있고 병원 이름과 좌표가 없다`() = runTest {
        val log = mutableListOf<String>()
        FallbackHospitalBoardSource(server = { HospitalBoard.Unavailable("HTTP 403") }, now = { t0 }, log = { log += it }).load()
        val text = log.joinToString("\n")
        assertFalse(text, text.contains("동물병원"))
        assertFalse(text, Regex("""\d{2,3}\.\d{3}""").containsMatchIn(text))
        assertEquals(1, log.size)
    }
}
