package com.meongcare.app.domain.hospital

import com.meongcare.app.domain.guidance.Destination
import com.meongcare.app.domain.location.CurrentLocation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 계획 015 "설계 결정 2"의 `nearbyVetRowsOf` 표. */
class NearbyVetRowsTest {

    private val here = CurrentLocation(37.4127212, 127.094681, "수정구 시흥동")

    /** 위도 1도 ≈ 111.195km (지구 반지름 6371.0088km) */
    private val kmPerDegree = 111.195

    /** [here]에서 정북으로 [km]만큼 떨어진 근처 병원 */
    private fun vet(name: String, km: Double, phone: String? = null) = Hospital(
        Destination(name, here.latitude + km / kmPerDegree, here.longitude),
        phoneNumber = phone,
        isSample = false,
    )

    private fun partner(name: String, km: Double) = HospitalCandidate(
        Hospital(Destination(name, here.latitude + km / kmPerDegree, here.longitude), null, isSample = true, id = name),
        status = null,
        origin = HospitalOrigin.SERVER,
    )

    private fun names(rows: List<RankedHospital>) = rows.map { it.hospital.name }

    @Test
    fun `줄은 근처 검색 출처, 대기 정보 없음, 이동 어림을 든다`() {
        val row = nearbyVetRowsOf(listOf(vet("가", 2.0, "031-000-0000")), here, partners = emptyList()).single()

        assertEquals(HospitalOrigin.NEARBY_SEARCH, row.candidate.origin)
        assertNull(row.status)
        assertEquals(StatusFreshness.NONE, row.freshness)
        assertNull(row.statusAge)
        assertNull(row.total)
        assertEquals(estimateTravel(here, row.hospital.destination), row.travel)
        assertFalse(row.candidate.canReceiveCard)
        assertFalse(row.candidate.sendsCardToServer)
        assertEquals("031-000-0000", row.hospital.phoneNumber)
    }

    @Test
    fun `직선 거리 짧은 순이고 같으면 이름 순이다`() {
        val rows = nearbyVetRowsOf(
            listOf(vet("다", 3.0), vet("나", 1.0), vet("가", 3.0), vet("라", 0.5)),
            here,
            partners = emptyList(),
        )
        assertEquals(listOf("라", "나", "가", "다"), names(rows))
    }

    @Test
    fun `같은 입력이면 입력 순서와 상관없이 같은 순서다`() {
        val input = listOf(vet("가", 1.0), vet("나", 2.0), vet("다", 2.0), vet("라", 4.0))
        val expected = nearbyVetRowsOf(input, here, emptyList())
        assertEquals(expected, nearbyVetRowsOf(input.reversed(), here, emptyList()))
        assertEquals(expected, nearbyVetRowsOf(input.shuffled(kotlin.random.Random(7)), here, emptyList()))
    }

    @Test
    fun `반경과 여유를 넘는 곳은 빼고 경계 안쪽은 남긴다`() {
        val rows = nearbyVetRowsOf(listOf(vet("안", 5.49), vet("밖", 5.6)), here, emptyList())
        assertEquals(listOf("안"), names(rows))
    }

    @Test
    fun `이름이 같고 100m 안인 제휴 병원이 있으면 뺀다`() {
        val rows = nearbyVetRowsOf(
            listOf(vet("바른 동물병원", 2.0), vet("먼 동물병원", 3.0)),
            here,
            partners = listOf(partner("바른동물병원", 2.05), partner("먼 동물병원", 3.5)),
        )
        // "바른 동물병원"은 공백을 뺀 이름이 같고 50m 안 → 뺀다. "먼 동물병원"은 이름은 같지만 500m 떨어짐 → 남긴다
        assertEquals(listOf("먼 동물병원"), names(rows))
    }

    @Test
    fun `이름과 좌표가 같은 검색 결과는 하나만 남긴다`() {
        val rows = nearbyVetRowsOf(listOf(vet("가", 1.0), vet("가", 1.0), vet("가", 1.2)), here, emptyList())
        assertEquals(2, rows.size)
    }

    @Test
    fun `정리한 뒤 앞에서 최대 개수만 남긴다`() {
        val found = (1..20).map { vet("병원%02d".format(it), it * 0.2) }
        val rows = nearbyVetRowsOf(found, here, emptyList())
        assertEquals(NEARBY_MAX_ROWS, rows.size)
        assertEquals("병원01", rows.first().hospital.name)
        assertEquals(listOf("병원01", "병원02"), names(nearbyVetRowsOf(found, here, emptyList(), max = 2)))
    }

    @Test
    fun `빈 입력이면 빈 목록이다`() {
        assertTrue(nearbyVetRowsOf(emptyList(), here, listOf(partner("가", 1.0))).isEmpty())
    }

    @Test
    fun `제휴와 내장 병원은 카드를 받을 수 있다`() {
        listOf(HospitalOrigin.SERVER, HospitalOrigin.BUILT_IN, HospitalOrigin.BUILT_IN_FALLBACK).forEach { origin ->
            assertTrue(origin.name, partner("가", 1.0).copy(origin = origin).canReceiveCard)
        }
    }
}
