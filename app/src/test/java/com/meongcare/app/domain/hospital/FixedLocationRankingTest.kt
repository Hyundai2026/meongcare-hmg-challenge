package com.meongcare.app.domain.hospital

import com.meongcare.app.data.hospital.FakeNearbyVetSource
import com.meongcare.app.data.hospital.SampleHospitals
import com.meongcare.app.data.location.FakeCurrentLocationSource
import com.meongcare.app.di.DataMode
import com.meongcare.app.di.NearbyVetKind
import com.meongcare.app.di.fixedLocationOf
import com.meongcare.app.di.nearbyVetKindOf
import com.meongcare.app.domain.guidance.Destination
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 계획 016 단계 3 "볼 것": 고정 위치(서초동 서운로 62)에서 012 순위와 015 근처 병원 계산 (tester).
 * 기대값은 계획 U1 표(하버사인, 올림(km × 1.25 ÷ 21 × 60))를 따로 계산해 맞춘 값이다.
 */
class FixedLocationRankingTest {

    private val now = Instant.parse("2026-11-09T03:00:00Z")
    private val phone = FakeCurrentLocationSource.PHONE_LOCATION
    private val siheung = FakeCurrentLocationSource.DEMO_LOCATION
    private val partners = SampleHospitals.partners.map { it.candidateAt(now, HospitalOrigin.BUILT_IN) }

    private fun HospitalRanking.ids(rows: (HospitalRanking) -> List<RankedHospital>) = rows(this).map { it.hospital.id }
    private fun HospitalRanking.row(id: String) = (ranked + unranked + notAccepting).single { it.hospital.id == id }

    @Test
    fun `서초동에서는 시연용 동물병원이 약 9분으로 1위, 바른 66분, 가까운 107분 순이다`() {
        val ranking = rankHospitals(partners, from = phone, now = now)
        assertEquals(RankBasis.TRAVEL_PLUS_WAIT, ranking.basis)
        assertEquals(listOf("demo-main", "demo-fast", "demo-near"), ranking.ids { it.ranked })
        assertEquals(listOf(9, 66, 107), ranking.ranked.map { it.total!!.inWholeMinutes.toInt() })
        assertEquals("demo-main", ranking.fastest?.hospital?.id)
    }

    @Test
    fun `서초동에서 제휴 병원 이동 어림은 U1 표와 같다`() {
        val ranking = rankHospitals(partners, from = phone, now = now)
        val expected = mapOf("demo-main" to 4, "demo-fast" to 56, "demo-near" to 47, "demo-stale" to 24, "demo-closed" to 45)
        expected.forEach { (id, minutes) -> assertEquals(id, minutes, ranking.row(id).travel!!.inWholeMinutes.toInt()) }
    }

    @Test
    fun `서초동에서도 정보 지난 곳은 순위 밖, 응급 접수 안 하는 곳은 고를 수 없는 묶음이다`() {
        val ranking = rankHospitals(partners, from = phone, now = now)
        assertEquals(listOf("demo-stale"), ranking.ids { it.unranked })
        assertEquals(listOf("demo-closed"), ranking.ids { it.notAccepting })
    }

    @Test
    fun `서초동 이동 시간에는 수천 분 같은 값이 없다 (미국 기본 위치 문제가 없다)`() {
        val ranking = rankHospitals(partners, from = phone, now = now)
        (ranking.ranked + ranking.unranked + ranking.notAccepting).forEach {
            assertTrue("${it.hospital.id} ${it.travel}", it.travel!!.inWholeMinutes in 1..60)
        }
    }

    @Test
    fun `시흥동(Fake·Pleos 에뮬레이터)에서는 012 순위 이야기가 그대로다`() {
        val ranking = rankHospitals(partners, from = siheung, now = now)
        assertEquals(listOf("demo-fast", "demo-main", "demo-near"), ranking.ids { it.ranked })
        assertEquals(listOf(29, 40, 69), ranking.ranked.map { it.total!!.inWholeMinutes.toInt() })
    }

    @Test
    fun `release 일반 폰의 고정 위치로 순위를 매기면 서초동 순위가 나온다`() {
        val mode = com.meongcare.app.di.resolveDataMode(isDebugBuild = false, savedName = null)
        val ranking = rankHospitals(partners, from = fixedLocationOf(mode, isAutomotive = false), now = now)
        assertEquals("demo-main", ranking.fastest?.hospital?.id)
        assertEquals(9L, ranking.fastest?.total?.inWholeMinutes)
    }

    @Test
    fun `Fake 모드에서는 폰이든 차량 기기든 가짜 근처 병원 3곳이 거리 순으로 보인다`() {
        listOf(true, false).forEach { auto ->
            assertEquals(NearbyVetKind.FAKE, nearbyVetKindOf(DataMode.FAKE, hasFunction = false))
            val from = fixedLocationOf(DataMode.FAKE, auto)!!
            val rows = nearbyVetRowsOf(FakeNearbyVetSource.DEFAULT, from, partners)
            assertEquals(
                "auto=$auto",
                listOf("시연용 근처 동물병원 가", "시연용 근처 동물병원 나", "시연용 근처 동물병원 다"),
                rows.map { it.hospital.name },
            )
            assertTrue(rows.all { it.candidate.origin == HospitalOrigin.NEARBY_SEARCH && it.freshness == StatusFreshness.NONE })
        }
    }

    @Test
    fun `가짜 근처 병원이 나오는 모드는 언제나 시흥동 위치다`() {
        DataMode.entries.forEach { mode ->
            listOf(true, false).forEach { has ->
                if (nearbyVetKindOf(mode, has) == NearbyVetKind.FAKE) {
                    listOf(true, false).forEach { auto ->
                        assertEquals("$mode/$has/$auto", siheung, fixedLocationOf(mode, auto))
                    }
                }
            }
        }
    }

    @Test
    fun `가짜 근처 병원 3곳은 서초동에서 반경 밖이라 모두 빠진다`() {
        // U2: 그래서 Fake 모드 위치를 시흥동으로 둔다. 이 조합(폰 고정 위치 + Fake 근처)은 생기지 않아야 한다.
        assertTrue(nearbyVetRowsOf(FakeNearbyVetSource.DEFAULT, phone, partners).isEmpty())
    }

    @Test
    fun `서초동 반경 안의 근처 병원은 거리 순이고 이동 어림이 붙는다`() {
        val near = hospitalAt("서초 근처 A", 37.4950, 127.0283) // 약 0.53km
        val mid = hospitalAt("서초 근처 B", 37.4902566, 127.0600) // 약 2.8km
        val far = hospitalAt("서초 먼 곳", 37.4902566, 127.1000) // 약 6.3km
        val rows = nearbyVetRowsOf(listOf(far, mid, near), phone, partners)
        assertEquals(listOf("서초 근처 A", "서초 근처 B"), rows.map { it.hospital.name })
        rows.forEach { assertEquals(estimateTravel(phone, it.hospital.destination), it.travel) }
    }

    @Test
    fun `서초동에서 제휴 시연용 동물병원과 같은 곳의 근처 결과는 빠진다`() {
        val main = SampleHospitals.demo.destination
        val same = hospitalAt(main.name, main.latitude + 0.0001, main.longitude)
        val other = hospitalAt("서초 다른 병원", main.latitude + 0.0001, main.longitude)
        val rows = nearbyVetRowsOf(listOf(same, other), phone, partners)
        assertEquals(listOf("서초 다른 병원"), rows.map { it.hospital.name })
    }

    private fun hospitalAt(name: String, latitude: Double, longitude: Double) = Hospital(
        destination = Destination(name = name, latitude = latitude, longitude = longitude),
        phoneNumber = null,
        isSample = false,
    )
}
