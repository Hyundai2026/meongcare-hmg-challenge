package com.meongcare.app.domain.hospital

import com.meongcare.app.domain.guidance.Destination
import com.meongcare.app.domain.location.CurrentLocation
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 계획 015 "설계 변경 V2" `exampleStatusOf`의 경계 (tester). */
class ExampleWaitBoundaryTest {

    private val now = Instant.parse("2026-11-08T05:00:00Z")

    private fun vet(name: String, lat: Double = 37.5, lon: Double = 127.0, phone: String? = null) =
        Hospital(Destination(name, lat, lon), phone, isSample = false)

    @Test
    fun `대기 값은 시각과 상관없이 같은 병원이면 같다`() {
        val h = vet("가 동물병원")
        val later = now.plusSeconds(3_600 * 24)
        assertEquals(exampleStatusOf(h, now).waitMinutes, exampleStatusOf(h, later).waitMinutes)
        assertEquals(exampleStatusOf(h, now).congestion, exampleStatusOf(h, later).congestion)
        assertEquals(later, exampleStatusOf(h, later).updatedAt)
    }

    @Test
    fun `전화번호나 시연용 표시가 달라도 이름과 좌표가 같으면 같은 값이다`() {
        val a = vet("가 동물병원", phone = "031-000-0000")
        val b = a.copy(phoneNumber = null, isSample = true)
        assertEquals(exampleStatusOf(a, now), exampleStatusOf(b, now))
    }

    @Test
    fun `예시 대기는 순위에서 늘 쓸 만한 정보이고 응급 접수 중이다`() {
        val here = CurrentLocation(37.5, 127.0, null)
        val hospitals = (1..30).map { vet("병원$it", 37.5 + it * 0.001) }
        val ranking = rankHospitals(
            hospitals.map { HospitalCandidate(it, exampleStatusOf(it, now), HospitalOrigin.NEARBY_SEARCH) },
            here,
            now,
            RankingRules(maxPerGroup = 50),
        )
        assertEquals(30, ranking.ranked.size)
        assertTrue(ranking.unranked.isEmpty())
        assertTrue(ranking.notAccepting.isEmpty())
        assertTrue(ranking.ranked.all { it.freshness == StatusFreshness.FRESH })
    }

    @Test
    fun `5분부터 60분까지 12가지 값이 모두 나올 수 있다`() {
        val waits = (1..2_000).map { exampleStatusOf(vet("병원$it", 37.0 + it * 0.0001), now).waitMinutes }.toSet()
        assertEquals((5..60 step 5).toSet(), waits)
    }
}
