package com.meongcare.app.domain.hospital

import com.meongcare.app.domain.guidance.Destination
import com.meongcare.app.domain.location.CurrentLocation
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 계획 015 "설계 결정 2" `nearbyVetRowsOf`·`canReceiveCard`의 경계 (tester). */
class NearbyVetRowsBoundaryTest {

    private val here = CurrentLocation(37.4127212, 127.094681, "수정구 시흥동")
    private val kmPerDegree = 111.195

    private fun vet(name: String, lat: Double, lon: Double = here.longitude, phone: String? = null) =
        Hospital(Destination(name, lat, lon), phoneNumber = phone, isSample = false)

    private fun north(name: String, km: Double) = vet(name, here.latitude + km / kmPerDegree)

    private fun partner(hospital: Hospital, origin: HospitalOrigin = HospitalOrigin.SERVER) =
        HospitalCandidate(hospital.copy(id = "p-${hospital.name}"), status = null, origin = origin)

    private fun distance(lat: Double) =
        TravelEstimate.straightDistanceKm(here.latitude, here.longitude, lat, here.longitude)

    private fun names(rows: List<RankedHospital>) = rows.map { it.hospital.name }

    @Test
    fun `직선 거리가 정확히 5점5km인 곳은 남기고 그보다 조금이라도 먼 곳은 뺀다`() {
        // 직선 거리가 5.5km 이하인 가장 먼 위도를 부동소수 단위로 찾는다
        var lat = here.latitude + 5.5 / (TravelEstimate.EARTH_RADIUS_KM * Math.PI / 180)
        while (distance(lat) > 5.5) lat = Math.nextDown(lat)
        while (distance(Math.nextUp(lat)) <= 5.5) lat = Math.nextUp(lat)
        val edge = vet("경계", lat)
        val beyond = vet("바로 밖", Math.nextUp(lat))
        assertTrue(distance(edge.destination.latitude) <= 5.5)
        assertTrue(distance(beyond.destination.latitude) > 5.5)

        assertEquals(listOf("경계"), names(nearbyVetRowsOf(listOf(beyond, edge), here, emptyList())))
    }

    @Test
    fun `반경 5km를 넘어도 여유 500m 안이면 남긴다`() {
        val rows = nearbyVetRowsOf(listOf(north("5.2km", 5.2), north("5.6km", 5.6), north("20km", 20.0)), here, emptyList())
        assertEquals(listOf("5.2km"), names(rows))
    }

    @Test
    fun `제휴 병원 이름 비교는 앞뒤와 가운데 공백을 모두 무시한다`() {
        val nearby = north("바른동물병원", 2.0)
        listOf("  바른 동물병원  ", "바른  동물 병원", "\t바른동물병원\n").forEach { partnerName ->
            val p = partner(vet(partnerName, nearby.destination.latitude))
            assertTrue(partnerName, nearbyVetRowsOf(listOf(nearby), here, listOf(p)).isEmpty())
        }
        // 검색 결과 쪽 이름에 공백이 있어도 같다
        val spaced = north(" 바른 동물병원 ", 2.0)
        assertTrue(nearbyVetRowsOf(listOf(spaced), here, listOf(partner(nearby))).isEmpty())
    }

    @Test
    fun `이름이 같아도 제휴 병원과 100m보다 멀면 다른 곳이고, 100m 안이면 같은 곳이다`() {
        val nearby = north("가 동물병원", 2.0)
        val close = partner(north("가 동물병원", 2.0 + 0.09))
        val far = partner(north("가 동물병원", 2.0 + 0.11))
        assertTrue(nearbyVetRowsOf(listOf(nearby), here, listOf(close)).isEmpty())
        assertEquals(listOf("가 동물병원"), names(nearbyVetRowsOf(listOf(nearby), here, listOf(far))))
    }

    @Test
    fun `좌표가 같아도 이름이 다르면 제휴 병원과 다른 곳으로 남긴다`() {
        val nearby = north("가 동물병원", 2.0)
        val p = partner(nearby.copy(destination = nearby.destination.copy(name = "나 동물병원")))
        assertEquals(listOf("가 동물병원"), names(nearbyVetRowsOf(listOf(nearby), here, listOf(p))))
    }

    @Test
    fun `내장 시연 병원이나 대신 보인 병원과 같은 곳도 뺀다`() {
        val nearby = north("가 동물병원", 2.0)
        listOf(HospitalOrigin.BUILT_IN, HospitalOrigin.BUILT_IN_FALLBACK).forEach { origin ->
            assertTrue(origin.name, nearbyVetRowsOf(listOf(nearby), here, listOf(partner(nearby, origin))).isEmpty())
        }
    }

    @Test
    fun `제휴 병원과 같은 곳은 위치에서 먼 제휴 병원이어도 제휴 병원 좌표로 비교한다`() {
        // 제휴 병원이 반경 밖이어도 근처 결과와 같은 곳이면 뺀다 (목록에는 제휴 병원 쪽이 보인다)
        val nearby = north("가 동물병원", 5.4)
        assertTrue(nearbyVetRowsOf(listOf(nearby), here, listOf(partner(nearby))).isEmpty())
    }

    @Test
    fun `이름과 좌표가 같으면 전화가 달라도 하나만 남는다`() {
        val a = north("가", 1.0).copy(phoneNumber = "031-000-0001")
        val b = a.copy(phoneNumber = "031-000-0002")
        assertEquals(1, nearbyVetRowsOf(listOf(a, b), here, emptyList()).size)
    }

    @Test
    fun `좌표가 같아도 이름이 다르면 둘 다 남는다`() {
        val a = north("가", 1.0)
        val b = a.copy(destination = a.destination.copy(name = "나"))
        assertEquals(listOf("가", "나"), names(nearbyVetRowsOf(listOf(b, a), here, emptyList())))
    }

    @Test
    fun `중복과 반경 밖과 제휴 병원 겹침은 개수 제한 전에 뺀다`() {
        val dup = north("가", 0.5)
        val found = List(10) { dup } + List(10) { north("밖$it", 6.0 + it) } + north("제휴", 0.6) +
            (1..15).map { north("병원%02d".format(it), 1.0 + it * 0.1) }
        val rows = nearbyVetRowsOf(found, here, partners = listOf(partner(north("제휴", 0.6))))
        assertEquals(NEARBY_MAX_ROWS, rows.size)
        assertEquals(listOf("가") + (1..14).map { "병원%02d".format(it) }, names(rows))
    }

    @Test
    fun `거리와 이름이 같으면 위도, 그다음 경도 순이다`() {
        // 같은 위도에서 동·서로 같은 거리 → 경도가 작은(서쪽) 곳이 먼저
        val d = 0.02
        val east = vet("가", here.latitude, here.longitude + d)
        val west = vet("가", here.latitude, here.longitude - d)
        val rows = nearbyVetRowsOf(listOf(east, west), here, emptyList())
        assertEquals(listOf(here.longitude - d, here.longitude + d), rows.map { it.hospital.destination.longitude })
        assertEquals(rows, nearbyVetRowsOf(listOf(west, east), here, emptyList()))
    }

    @Test
    fun `최대 개수가 0이거나 음수면 빈 목록이다`() {
        val found = listOf(north("가", 1.0), north("나", 2.0))
        assertTrue(nearbyVetRowsOf(found, here, emptyList(), max = 0).isEmpty())
        assertTrue(nearbyVetRowsOf(found, here, emptyList(), max = -1).isEmpty())
    }

    @Test
    fun `같은 좌표의 병원도 이동 어림은 최소 1분이다`() {
        val row = nearbyVetRowsOf(listOf(vet("바로 앞", here.latitude)), here, emptyList()).single()
        assertEquals(1L, row.travel?.inWholeMinutes)
    }

    @Test
    fun `근처 검색 병원만 카드를 받지 못한다`() {
        val h = north("가", 1.0)
        HospitalOrigin.entries.forEach { origin ->
            val candidate = HospitalCandidate(h, status = null, origin = origin)
            assertEquals(origin.name, origin != HospitalOrigin.NEARBY_SEARCH, candidate.canReceiveCard)
        }
    }

    @Test
    fun `예시 대기를 붙여도 근처 검색 병원은 카드를 받지 못하고 서버로도 보내지 않는다`() {
        val h = north("가", 1.0)
        val withExample = HospitalCandidate(h, exampleStatusOf(h, Instant.EPOCH), HospitalOrigin.NEARBY_SEARCH)
        assertFalse(withExample.canReceiveCard)
        assertFalse(withExample.sendsCardToServer)
    }
}
