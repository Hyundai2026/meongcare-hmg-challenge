package com.meongcare.app.domain.hospital

import com.meongcare.app.domain.guidance.Destination
import com.meongcare.app.domain.location.CurrentLocation
import java.time.Instant
import kotlin.time.Duration.Companion.minutes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HospitalRankingTest {

    private val now = Instant.parse("2026-11-06T05:00:00Z")
    private val here = CurrentLocation(37.4127212, 127.094681, "수정구 시흥동")

    /** [here]에서 정북으로 [km]만큼 떨어진 병원. 이동 어림 = 올림(km × 1.25 ÷ 21 × 60) */
    private fun candidate(
        id: String,
        km: Double,
        wait: Int?,
        accepting: Boolean = true,
        minutesAgo: Long = 1,
        name: String = id,
    ): HospitalCandidate {
        val latitude = here.latitude + km / 111.195
        return HospitalCandidate(
            hospital = Hospital(Destination(name, latitude, here.longitude), phoneNumber = null, isSample = true, id = id),
            status = wait?.let {
                HospitalStatus(it, Congestion.NORMAL, accepting, now.minusSeconds(minutesAgo * 60))
            },
            origin = HospitalOrigin.BUILT_IN,
        )
    }

    private fun HospitalRanking.rankedIds() = ranked.map { it.hospital.id }
    private fun HospitalRanking.unrankedIds() = unranked.map { it.hospital.id }

    @Test
    fun `이동과 대기를 더한 시간이 짧은 순이다`() {
        val result = rankHospitals(
            listOf(
                candidate("near-busy", km = 2.7, wait = 60), // 10 + 60
                candidate("far-quiet", km = 8.3, wait = 5), // 30 + 5
                candidate("mid", km = 5.5, wait = 10), // 20 + 10
            ),
            from = here,
            now = now,
        )
        assertEquals(listOf("mid", "far-quiet", "near-busy"), result.rankedIds())
        assertEquals(30.minutes, result.fastest?.total)
        assertEquals(20.minutes, result.fastest?.travel)
        assertEquals(10.minutes, result.fastest?.wait)
        assertEquals(RankBasis.TRAVEL_PLUS_WAIT, result.basis)
    }

    @Test
    fun `합이 같으면 이동 시간이 짧은 곳이 먼저다`() {
        val result = rankHospitals(
            listOf(
                candidate("far", km = 5.5, wait = 10), // 20 + 10
                candidate("near", km = 2.7, wait = 20), // 10 + 20
            ),
            from = here,
            now = now,
        )
        assertEquals(listOf("near", "far"), result.rankedIds())
    }

    @Test
    fun `합과 이동이 같으면 이름 순, 그다음 id 순이다`() {
        val result = rankHospitals(
            listOf(
                candidate("c", km = 2.7, wait = 10, name = "나 병원"),
                candidate("b", km = 2.7, wait = 10, name = "가 병원"),
                candidate("a", km = 2.7, wait = 10, name = "나 병원"),
            ),
            from = here,
            now = now,
        )
        assertEquals(listOf("b", "a", "c"), result.rankedIds())
    }

    @Test
    fun `응급 접수를 받지 않는 곳은 순위에서 빼고 따로 모은다`() {
        val result = rankHospitals(
            listOf(
                candidate("closed", km = 1.0, wait = 0, accepting = false),
                candidate("open", km = 5.5, wait = 10),
            ),
            from = here,
            now = now,
        )
        assertEquals(listOf("open"), result.rankedIds())
        assertEquals(listOf("closed"), result.notAccepting.map { it.hospital.id })
        assertTrue(result.unranked.isEmpty())
    }

    @Test
    fun `30분보다 오래된 대기 정보는 순위 뒤로 보낸다`() {
        val result = rankHospitals(
            listOf(
                candidate("stale", km = 1.0, wait = 0, minutesAgo = 31),
                candidate("fresh", km = 8.3, wait = 30),
            ),
            from = here,
            now = now,
        )
        assertEquals(listOf("fresh"), result.rankedIds())
        assertEquals(listOf("stale"), result.unrankedIds())
        val stale = result.unranked.single()
        assertEquals(StatusFreshness.STALE, stale.freshness)
        assertEquals(31.minutes, stale.statusAge)
        assertNull(stale.wait)
        assertNull(stale.total)
    }

    @Test
    fun `정확히 30분 전 정보는 아직 유효하다`() {
        val result = rankHospitals(listOf(candidate("edge", km = 2.7, wait = 10, minutesAgo = 30)), from = here, now = now)
        assertEquals(listOf("edge"), result.rankedIds())
        assertEquals(StatusFreshness.FRESH, result.ranked.single().freshness)
    }

    @Test
    fun `오래된 정보의 응급 접수 불가는 믿지 않는다`() {
        val result = rankHospitals(
            listOf(candidate("old-closed", km = 2.7, wait = 0, accepting = false, minutesAgo = 90)),
            from = here,
            now = now,
        )
        assertTrue(result.notAccepting.isEmpty())
        assertEquals(listOf("old-closed"), result.unrankedIds())
    }

    @Test
    fun `갱신 시각이 5분보다 미래면 오래된 정보로 본다`() {
        val result = rankHospitals(
            listOf(
                candidate("future6", km = 2.7, wait = 10, minutesAgo = -6),
                candidate("future5", km = 2.7, wait = 10, minutesAgo = -5),
            ),
            from = here,
            now = now,
        )
        assertEquals(listOf("future5"), result.rankedIds())
        assertEquals(0.minutes, result.ranked.single().statusAge)
        assertEquals(listOf("future6"), result.unrankedIds())
    }

    @Test
    fun `대기 정보가 없는 병원은 이동 시간 순으로 뒤에 둔다`() {
        val result = rankHospitals(
            listOf(
                candidate("none-far", km = 8.3, wait = null),
                candidate("none-near", km = 2.7, wait = null),
                candidate("ranked", km = 20.0, wait = 100),
            ),
            from = here,
            now = now,
        )
        assertEquals(listOf("ranked"), result.rankedIds())
        assertEquals(listOf("none-near", "none-far"), result.unrankedIds())
        assertEquals(StatusFreshness.NONE, result.unranked.first().freshness)
        assertNull(result.unranked.first().statusAge)
        assertEquals(10.minutes, result.unranked.first().travel)
    }

    @Test
    fun `위치를 모르면 대기 시간 순이고 이동 시간은 비운다`() {
        val result = rankHospitals(
            listOf(
                candidate("b-wait10", km = 20.0, wait = 10, name = "나"),
                candidate("a-wait5", km = 1.0, wait = 5, name = "다"),
                candidate("c-wait10", km = 1.0, wait = 10, name = "가"),
                candidate("none-z", km = 1.0, wait = null, name = "하"),
                candidate("none-a", km = 1.0, wait = null, name = "가"),
            ),
            from = null,
            now = now,
        )
        assertEquals(RankBasis.WAIT_ONLY, result.basis)
        assertEquals(listOf("a-wait5", "c-wait10", "b-wait10"), result.rankedIds())
        assertEquals(listOf("none-a", "none-z"), result.unrankedIds())
        assertTrue(result.ranked.all { it.travel == null && it.total == null })
        assertEquals(5.minutes, result.fastest?.wait)
    }

    @Test
    fun `같은 입력이면 순서가 같다`() {
        val list = listOf(
            candidate("x", km = 2.7, wait = 10),
            candidate("y", km = 2.7, wait = 10),
            candidate("z", km = 5.5, wait = 0),
        )
        val first = rankHospitals(list, here, now)
        val second = rankHospitals(list.reversed(), here, now)
        assertEquals(first.rankedIds(), second.rankedIds())
    }

    @Test
    fun `각 묶음은 10곳까지다`() {
        val many = (1..12).map { candidate("h%02d".format(it), km = it.toDouble(), wait = 0) }
        val result = rankHospitals(many, here, now)
        assertEquals(10, result.ranked.size)
        assertEquals("h01", result.ranked.first().hospital.id)
    }

    @Test
    fun `빈 목록이면 모두 비어 있다`() {
        val result = rankHospitals(emptyList(), here, now)
        assertTrue(result.ranked.isEmpty() && result.unranked.isEmpty() && result.notAccepting.isEmpty())
        assertNull(result.fastest)
    }
}
