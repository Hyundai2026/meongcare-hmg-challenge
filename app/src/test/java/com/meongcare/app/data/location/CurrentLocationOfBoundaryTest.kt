package com.meongcare.app.data.location

import com.meongcare.app.domain.location.CurrentLocation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 011 계획 설계 결정 1 "값 규칙"의 경계. 순수 함수 [currentLocationOf]를 본다.
 * - NaN·무한대, 범위 밖, (0, 0) → null. 경계값(±90, ±180)은 받는다.
 * - 주소: null, 빈 문자열, 공백뿐 → null. 아니면 앞뒤 공백을 자른다.
 */
class CurrentLocationOfBoundaryTest {

    @Test
    fun `한쪽 좌표만 0이면 받는다`() {
        for ((lat, lon) in listOf(0.0 to 127.094681, 37.4127212 to 0.0, 0.0 to -180.0, -90.0 to 0.0)) {
            assertEquals("$lat, $lon", CurrentLocation(lat, lon, null), currentLocationOf(lat, lon, null))
        }
    }

    @Test
    fun `음수 0도 0, 0으로 보고 모름이다`() {
        for ((lat, lon) in listOf(-0.0 to 0.0, 0.0 to -0.0, -0.0 to -0.0)) {
            assertNull("$lat, $lon", currentLocationOf(lat, lon, "주소"))
        }
    }

    @Test
    fun `남반구와 서반구의 음수 좌표도 받는다`() {
        val santiago = currentLocationOf(-33.4489, -70.6693, "산티아고")
        assertEquals(CurrentLocation(-33.4489, -70.6693, "산티아고"), santiago)
    }

    @Test
    fun `위도와 경도 경계값을 섞어도 받는다`() {
        val edges = listOf(90.0 to -180.0, -90.0 to 180.0, 90.0 to 127.0, 37.0 to -180.0)
        for ((lat, lon) in edges) {
            assertEquals("$lat, $lon", CurrentLocation(lat, lon, null), currentLocationOf(lat, lon, null))
        }
    }

    @Test
    fun `경계를 아주 조금이라도 넘으면 모름이다`() {
        val justOutside = listOf(
            Math.nextUp(90.0) to 127.0,
            Math.nextDown(-90.0) to 127.0,
            37.0 to Math.nextUp(180.0),
            37.0 to Math.nextDown(-180.0),
            Double.MAX_VALUE to 127.0,
            37.0 to -Double.MAX_VALUE,
        )
        for ((lat, lon) in justOutside) {
            assertNull("$lat, $lon", currentLocationOf(lat, lon, "주소"))
        }
    }

    @Test
    fun `두 좌표가 모두 숫자가 아니거나 무한대이거나 하나만 0과 섞여도 모름이다`() {
        val bad = listOf(
            Double.NaN to Double.NaN,
            Double.POSITIVE_INFINITY to Double.NEGATIVE_INFINITY,
            Double.NaN to 0.0,
            0.0 to Double.NaN,
            0.0 to Double.POSITIVE_INFINITY,
        )
        for ((lat, lon) in bad) {
            assertNull("$lat, $lon", currentLocationOf(lat, lon, "주소"))
        }
    }

    @Test
    fun `좌표는 반올림하지 않고 그대로 옮긴다`() {
        val location = currentLocationOf(37.41272123456789, 127.09468198765432, null)
        assertEquals(37.41272123456789, location!!.latitude, 0.0)
        assertEquals(127.09468198765432, location.longitude, 0.0)
    }

    @Test
    fun `주소가 탭이나 줄바꿈이나 전각 공백뿐이어도 주소 모름이다`() {
        for (address in listOf("\t", "\n", " \t\r\n ", "　", " 　 ")) {
            val location = currentLocationOf(37.4127212, 127.094681, address)
            assertEquals("주소 ${address.map { it.code }}", CurrentLocation(37.4127212, 127.094681, null), location)
        }
    }

    @Test
    fun `주소 앞뒤의 탭과 줄바꿈도 자르고 가운데 공백은 그대로 둔다`() {
        val location = currentLocationOf(37.4127212, 127.094681, "\t 성남시  수정구 시흥동 \n")
        assertEquals("성남시  수정구 시흥동", location?.address)
    }

    @Test
    fun `좌표가 이상하면 주소가 있어도 모름이다`() {
        assertNull(currentLocationOf(Double.NaN, 127.094681, "수정구 시흥동"))
        assertNull(currentLocationOf(0.0, 0.0, "수정구 시흥동"))
    }
}
