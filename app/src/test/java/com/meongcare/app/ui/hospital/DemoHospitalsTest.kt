package com.meongcare.app.ui.hospital

import com.meongcare.app.data.location.FakeCurrentLocationSource
import com.meongcare.app.domain.hospital.HospitalOrigin
import com.meongcare.app.domain.hospital.StatusFreshness
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.time.Duration.Companion.minutes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 계획 019 진행 로그(2026-11-13 사용자 결정): 홈 [병원 찾기]의 시연 병원은 디자인 시안 이름 5곳 고정, 가상 병원,
 * 대기시간 미확인, 카드 안 받음. 값은 프로토타입(`docs/product/design/prototype/MeongCareCar.html`의 HP0)과 같다.
 */
class DemoHospitalsTest {
    private val all = DemoHospitals.all

    @Test
    fun `시안의 병원 5곳이 이름·거리·이동 시간 그대로 가까운 순이다`() {
        assertEquals(
            listOf(
                Triple("별숲동물의료센터", 4.0, 10),
                Triple("온유동물병원", 6.1, 15),
                Triple("나래동물의료센터", 8.2, 20),
                Triple("달빛24시동물의료센터", 10.3, 25),
                Triple("가람24시동물병원", 12.5, 30),
            ),
            all.map { Triple(it.name, it.distanceKm, it.travelMinutes) },
        )
        assertEquals(listOf(1, 2, 3, 4, 5), all.map { it.rank })
    }

    @Test
    fun `가상 병원이라 시연용 표시와 시연용 번호가 있고 대기 정보가 없다`() {
        for (h in all) {
            assertTrue(h.name, h.row.hospital.isSample)
            assertEquals(h.name, "02-0000-%04d".format(20 + h.rank), h.row.hospital.phoneNumber)
            assertNull(h.name, h.row.candidate.status)
            assertEquals(h.name, StatusFreshness.NONE, h.row.freshness)
            assertNull(h.name, h.row.statusAge)
        }
    }

    @Test
    fun `카드를 받지만 서버로는 보내지 않는 시연 병원이다`() {
        for (h in all) {
            assertEquals(h.name, HospitalOrigin.BUILT_IN, h.row.candidate.origin)
            assertTrue(h.name, h.row.candidate.canReceiveCard)
            assertFalse(h.name, h.row.candidate.sendsCardToServer)
        }
    }

    @Test
    fun `목록의 이동 시간과 길안내 목적지의 이동 어림이 같다`() {
        for (h in all) {
            assertEquals(h.name, h.travelMinutes.minutes, h.row.travel)
            assertEquals(h.name, h.travelMinutes.minutes, h.row.hospital.destination.expectedTravel)
            assertEquals(h.name, h.name, h.row.hospital.destination.name)
        }
    }

    @Test
    fun `지도 위치는 0~1 안이고 현재 위치(가운데)와 겹치지 않는다`() {
        for (h in all) {
            assertTrue(h.name, h.mapX in 0f..1f && h.mapY in 0f..1f)
            assertFalse(h.name, h.mapX == 0.5f && h.mapY == 0.5f)
        }
        assertEquals("지도 위치가 서로 겹치지 않는다", all.size, all.map { it.mapX to it.mapY }.toSet().size)
    }

    @Test
    fun `목적지 좌표는 시연 위치에서 거리만큼 지도 방향으로 떨어져 있다`() {
        val origin = FakeCurrentLocationSource.PHONE_LOCATION
        for (h in all) {
            val d = h.row.hospital.destination
            val km = haversineKm(origin.latitude, origin.longitude, d.latitude, d.longitude)
            assertEquals(h.name, h.distanceKm, km, h.distanceKm * 0.05)
            // 지도 오른쪽이면 동쪽(경도 큼), 지도 위쪽이면 북쪽(위도 큼)
            if (h.mapX > 0.5f) assertTrue(h.name, d.longitude > origin.longitude)
            if (h.mapX < 0.5f) assertTrue(h.name, d.longitude < origin.longitude)
            if (h.mapY < 0.5f) assertTrue(h.name, d.latitude > origin.latitude)
            if (h.mapY > 0.5f) assertTrue(h.name, d.latitude < origin.latitude)
        }
    }

    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return 2 * r * asin(sqrt(a))
    }
}
