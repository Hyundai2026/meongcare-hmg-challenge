package com.meongcare.app.domain.hospital

import com.meongcare.app.domain.guidance.Destination
import com.meongcare.app.domain.location.CurrentLocation
import java.time.Instant
import kotlin.time.Duration.Companion.minutes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 순위 규칙 경계. 기준: 계획 012 "설계 결정 2" 표, docs/data/hospital-status.md "오래된 정보".
 */
class HospitalRankingBoundaryTest {

    private val now = Instant.parse("2026-11-06T05:00:00Z")
    private val here = CurrentLocation(37.4127212, 127.094681, "수정구 시흥동")

    /** [here]에서 정북으로 [km]. 이동 어림 = 올림(km × 1.25 ÷ 21 × 60). 2.79km ≈ 9.96분 → 10분 */
    private fun candidate(
        id: String,
        km: Double,
        wait: Int?,
        accepting: Boolean = true,
        updatedAt: Instant = now.minusSeconds(60),
        name: String = id,
    ) = HospitalCandidate(
        hospital = Hospital(Destination(name, here.latitude + km / 111.195, here.longitude), null, isSample = true, id = id),
        status = wait?.let { HospitalStatus(it, Congestion.NORMAL, accepting, updatedAt) },
        origin = HospitalOrigin.SERVER,
    )

    private fun HospitalRanking.ids(group: HospitalRanking.() -> List<RankedHospital>) = group().map { it.hospital.id }

    @Test
    fun `30분에서 1밀리초만 지나도 지난 정보다`() {
        val result = rankHospitals(
            listOf(
                candidate("just30", km = 2.79, wait = 10, updatedAt = now.minusSeconds(30 * 60)),
                candidate("over30", km = 2.79, wait = 10, updatedAt = now.minusSeconds(30 * 60).minusMillis(1)),
            ),
            here,
            now,
        )
        assertEquals(listOf("just30"), result.ids { ranked })
        assertEquals(listOf("over30"), result.ids { unranked })
        assertEquals(StatusFreshness.STALE, result.unranked.single().freshness)
    }

    @Test
    fun `5분에서 1밀리초만 더 미래여도 지난 정보다`() {
        val result = rankHospitals(
            listOf(
                candidate("future5", km = 2.79, wait = 10, updatedAt = now.plusSeconds(5 * 60)),
                candidate("future5plus", km = 2.79, wait = 10, updatedAt = now.plusSeconds(5 * 60).plusMillis(1)),
            ),
            here,
            now,
        )
        assertEquals(listOf("future5"), result.ids { ranked })
        assertEquals(listOf("future5plus"), result.ids { unranked })
    }

    @Test
    fun `지난 정보는 대기 분과 합계를 쓰지 않지만 이동과 알린 지는 남긴다`() {
        val stale = rankHospitals(
            listOf(candidate("old", km = 2.79, wait = 5, updatedAt = now.minusSeconds(90 * 60))),
            here,
            now,
        ).unranked.single()
        assertNull(stale.wait)
        assertNull(stale.total)
        assertEquals(10.minutes, stale.travel)
        assertEquals(90.minutes, stale.statusAge)
    }

    @Test
    fun `지난 정보와 정보 없음은 한 묶음에서 이동 시간 순이다`() {
        val result = rankHospitals(
            listOf(
                candidate("none-far", km = 8.0, wait = null),
                candidate("stale-near", km = 1.0, wait = 5, updatedAt = now.minusSeconds(90 * 60)),
                candidate("future-mid", km = 4.0, wait = 5, updatedAt = now.plusSeconds(10 * 60)),
                candidate("stale-closed-far", km = 12.0, wait = 0, accepting = false, updatedAt = now.minusSeconds(31 * 60)),
            ),
            here,
            now,
        )
        assertTrue(result.ranked.isEmpty())
        assertTrue(result.notAccepting.isEmpty())
        assertEquals(listOf("stale-near", "future-mid", "none-far", "stale-closed-far"), result.ids { unranked })
        assertNull(result.fastest)
        assertEquals(RankBasis.TRAVEL_PLUS_WAIT, result.basis)
    }

    @Test
    fun `위치를 모르면 대기가 같을 때 이름, 그다음 id 순이다`() {
        val result = rankHospitals(
            listOf(
                candidate("b", km = 1.0, wait = 10, name = "같은 이름"),
                candidate("a", km = 9.0, wait = 10, name = "같은 이름"),
                candidate("c", km = 0.5, wait = 10, name = "가 병원"),
            ),
            from = null,
            now = now,
        )
        assertEquals(listOf("c", "a", "b"), result.ids { ranked })
    }

    @Test
    fun `위치를 모르면 지난 정보와 정보 없음은 이동 거리와 상관없이 이름 순이다`() {
        val result = rankHospitals(
            listOf(
                candidate("z", km = 0.5, wait = null, name = "하"),
                candidate("y", km = 9.0, wait = 5, updatedAt = now.minusSeconds(90 * 60), name = "가"),
                candidate("x", km = 3.0, wait = null, name = "나"),
            ),
            from = null,
            now = now,
        )
        assertEquals(listOf("y", "x", "z"), result.ids { unranked })
        assertTrue(result.unranked.all { it.travel == null })
    }

    @Test
    fun `응급 접수 안 함은 이름 순이고 1위가 될 수 없다`() {
        val result = rankHospitals(
            listOf(
                candidate("closed-near", km = 0.5, wait = 0, accepting = false, name = "다"),
                candidate("closed-b", km = 0.5, wait = 0, accepting = false, name = "가"),
                candidate("open-far", km = 20.0, wait = 120),
            ),
            here,
            now,
        )
        assertEquals("open-far", result.fastest?.hospital?.id)
        assertEquals(listOf("closed-b", "closed-near"), result.ids { notAccepting })
    }

    @Test
    fun `대기 0분이면 합계는 이동 시간과 같다`() {
        val row = rankHospitals(listOf(candidate("zero", km = 2.79, wait = 0)), here, now).ranked.single()
        assertEquals(10.minutes, row.total)
        assertEquals(0.minutes, row.wait)
    }

    @Test
    fun `11곳 이상이면 가장 빠른 10곳만 남기고 느린 곳을 뺀다`() {
        val many = (1..12).map { candidate("h%02d".format(it), km = 2.79, wait = it * 10) }.shuffled(java.util.Random(7))
        val result = rankHospitals(many, here, now)
        assertEquals((1..10).map { "h%02d".format(it) }, result.ids { ranked })
    }

    @Test
    fun `응급 접수 안 함과 정보 없음 묶음도 각각 10곳까지다`() {
        val closed = (1..11).map { candidate("c%02d".format(it), km = 1.0, wait = 0, accepting = false) }
        val none = (1..11).map { candidate("n%02d".format(it), km = it.toDouble(), wait = null) }
        val result = rankHospitals(closed + none, here, now)
        assertEquals(10, result.notAccepting.size)
        assertEquals(10, result.unranked.size)
        assertEquals("n01", result.unranked.first().hospital.id)
        assertTrue("가장 먼 곳이 빠진다", result.unranked.none { it.hospital.id == "n11" })
    }

    @Test
    fun `순위 묶음에는 30분 안에 알리고 응급 접수 중인 곳만 있고 받은 병원은 하나도 빠지지 않는다`() {
        val list = listOf(
            candidate("ok", km = 1.0, wait = 10),
            candidate("closed", km = 1.0, wait = 10, accepting = false),
            candidate("stale", km = 1.0, wait = 10, updatedAt = now.minusSeconds(45 * 60)),
            candidate("none", km = 1.0, wait = null),
            candidate("skew", km = 1.0, wait = 10, updatedAt = now.plusSeconds(6 * 60)),
        )
        val result = rankHospitals(list, here, now)
        assertTrue(result.ranked.all { it.freshness == StatusFreshness.FRESH && it.status!!.acceptingEmergency })
        val all = (result.ranked + result.unranked + result.notAccepting).mapNotNull { it.hospital.id }.sorted()
        assertEquals(list.mapNotNull { it.hospital.id }.sorted(), all)
    }

    @Test
    fun `지난 정보인지는 인자로 받은 규칙 값을 따른다`() {
        val rules = RankingRules(staleAfter = 10.minutes, futureTolerance = 1.minutes, maxPerGroup = 1)
        val result = rankHospitals(
            listOf(
                candidate("eleven", km = 1.0, wait = 1, updatedAt = now.minusSeconds(11 * 60)),
                candidate("ten", km = 1.0, wait = 1, updatedAt = now.minusSeconds(10 * 60)),
                candidate("ten-b", km = 1.0, wait = 2, updatedAt = now.minusSeconds(10 * 60)),
            ),
            here,
            now,
            rules,
        )
        assertEquals(listOf("ten"), result.ids { ranked })
        assertEquals(listOf("eleven"), result.ids { unranked })
    }
}
