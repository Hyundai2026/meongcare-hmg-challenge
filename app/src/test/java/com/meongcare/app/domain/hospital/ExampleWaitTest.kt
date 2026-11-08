package com.meongcare.app.domain.hospital

import com.meongcare.app.domain.guidance.Destination
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 계획 015 "설계 변경 V2"의 예시 대기. */
class ExampleWaitTest {

    private val now = Instant.parse("2026-11-08T05:00:00Z")

    private fun vet(name: String, lat: Double = 37.5, lon: Double = 127.0) =
        Hospital(Destination(name, lat, lon), null, isSample = false)

    @Test
    fun `같은 병원은 늘 같은 값이고 5분 단위 5~60분이다`() {
        val hospitals = (1..200).map { vet("병원$it", 37.5 + it * 0.001) }
        hospitals.forEach { h ->
            val status = exampleStatusOf(h, now)
            assertEquals(status, exampleStatusOf(h, now))
            assertTrue(status.waitMinutes in 5..60)
            assertEquals(0, status.waitMinutes % 5)
            assertTrue(status.acceptingEmergency)
            assertEquals(now, status.updatedAt)
        }
        assertTrue("값이 고르게 퍼진다", hospitals.map { exampleStatusOf(it, now).waitMinutes }.toSet().size >= 8)
    }

    @Test
    fun `혼잡도는 대기로 정한다`() {
        (1..200).map { exampleStatusOf(vet("병원$it"), now) }.forEach {
            val expected = when {
                it.waitMinutes < 15 -> Congestion.LOW
                it.waitMinutes < 35 -> Congestion.NORMAL
                else -> Congestion.HIGH
            }
            assertEquals(expected, it.congestion)
        }
    }
}
