package com.meongcare.app.ui.hospital

import com.meongcare.app.domain.guidance.Destination
import com.meongcare.app.domain.hospital.Congestion
import com.meongcare.app.domain.hospital.Hospital
import com.meongcare.app.domain.hospital.HospitalCandidate
import com.meongcare.app.domain.hospital.HospitalOrigin
import com.meongcare.app.domain.hospital.HospitalStatus
import com.meongcare.app.domain.hospital.RankBasis
import com.meongcare.app.domain.hospital.StatusFreshness
import com.meongcare.app.domain.hospital.rankHospitals
import com.meongcare.app.domain.location.CurrentLocation
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 목록 문구에 쓰는 값 경계. 기준: 계획 012 "설계 결정 4"(정차 화면 표시 열, K4),
 * "설계 결정 8"의 2(줄 내용, 위 안내, 위치 모를 때), `finderNoticesOf`·`HospitalRowValues` KDoc.
 */
class HospitalTextsBoundaryTest {

    private val now = Instant.parse("2026-11-06T05:00:00Z")
    private val here = CurrentLocation(37.4127212, 127.094681, "수정구 시흥동")

    private fun candidate(km: Double, wait: Int?, ageSeconds: Long, accepting: Boolean = true, isSample: Boolean = true) = HospitalCandidate(
        Hospital(Destination("병원", here.latitude + km / 111.195, here.longitude), null, isSample = isSample, id = "h"),
        wait?.let { HospitalStatus(it, Congestion.HIGH, accepting, now.minusSeconds(ageSeconds)) },
        HospitalOrigin.SERVER,
    )

    private fun rowOf(candidate: HospitalCandidate, located: Boolean = true) =
        rankHospitals(listOf(candidate), here.takeIf { located }, now).let { it.ranked + it.unranked + it.notAccepting }.single()

    @Test
    fun `위 안내는 출처, 기준, 위치 출처의 모든 조합에서 표와 같다`() {
        val sourceNotice = mapOf(
            HospitalOrigin.SERVER to FinderNotice.SERVER,
            HospitalOrigin.BUILT_IN to FinderNotice.BUILT_IN,
            HospitalOrigin.BUILT_IN_FALLBACK to FinderNotice.FALLBACK,
        )
        HospitalOrigin.entries.forEach { origin ->
            RankBasis.entries.forEach { basis ->
                listOf(false, true).forEach { sampleLocation ->
                    val expected = buildList {
                        add(sourceNotice.getValue(origin))
                        if (basis == RankBasis.TRAVEL_PLUS_WAIT) {
                            add(FinderNotice.ESTIMATED_TRAVEL)
                            if (sampleLocation) add(FinderNotice.SAMPLE_LOCATION)
                        }
                    }
                    assertEquals("$origin/$basis/$sampleLocation", expected, finderNoticesOf(origin, basis, sampleLocation))
                }
            }
        }
    }

    @Test
    fun `출처 안내는 늘 맨 위에 하나만 있다`() {
        HospitalOrigin.entries.forEach { origin ->
            val notices = finderNoticesOf(origin, RankBasis.TRAVEL_PLUS_WAIT, isLocationSample = true)
            val sourceNotices = notices.filter { it in setOf(FinderNotice.SERVER, FinderNotice.BUILT_IN, FinderNotice.FALLBACK) }
            assertEquals(1, sourceNotices.size)
            assertEquals(sourceNotices.single(), notices.first())
        }
    }

    @Test
    fun `알린 지는 분 단위로 내림한다`() {
        assertEquals(0L, hospitalRowValuesOf(rowOf(candidate(2.79, 10, ageSeconds = 59))).reportedMinutesAgo)
        assertEquals(1L, hospitalRowValuesOf(rowOf(candidate(2.79, 10, ageSeconds = 60))).reportedMinutesAgo)
        assertEquals(1L, hospitalRowValuesOf(rowOf(candidate(2.79, 10, ageSeconds = 119))).reportedMinutesAgo)
        assertEquals(30L, hospitalRowValuesOf(rowOf(candidate(2.79, 10, ageSeconds = 30 * 60))).reportedMinutesAgo)
    }

    @Test
    fun `지난 정보는 이동과 알린 지만 있고 대기, 혼잡, 합계가 없다`() {
        val values = hospitalRowValuesOf(rowOf(candidate(2.79, 10, ageSeconds = 90 * 60)))
        assertEquals(StatusFreshness.STALE, values.freshness)
        assertEquals(10, values.travelMinutes)
        assertEquals(90L, values.reportedMinutesAgo)
        assertNull(values.waitMinutes)
        assertNull(values.congestion)
        assertNull(values.totalMinutes)
    }

    @Test
    fun `대기 정보가 없으면 대기, 혼잡, 알린 지, 합계가 모두 없고 이동은 있다`() {
        val values = hospitalRowValuesOf(rowOf(candidate(2.79, wait = null, ageSeconds = 0)))
        assertEquals(StatusFreshness.NONE, values.freshness)
        assertEquals(10, values.travelMinutes)
        assertNull(values.waitMinutes)
        assertNull(values.congestion)
        assertNull(values.reportedMinutesAgo)
        assertNull(values.totalMinutes)
    }

    @Test
    fun `위치를 모르면 이동과 합계가 없고 대기와 혼잡은 있다`() {
        val values = hospitalRowValuesOf(rowOf(candidate(2.79, 10, ageSeconds = 120), located = false))
        assertNull(values.travelMinutes)
        assertNull(values.totalMinutes)
        assertEquals(10, values.waitMinutes)
        assertEquals(Congestion.HIGH, values.congestion)
        assertEquals(2L, values.reportedMinutesAgo)
    }

    @Test
    fun `대기 0분이면 합계는 이동과 같다`() {
        val values = hospitalRowValuesOf(rowOf(candidate(2.79, 0, ageSeconds = 0)))
        assertEquals(0, values.waitMinutes)
        assertEquals(10, values.totalMinutes)
    }

    @Test
    fun `실제 병원이면 시연용 표시가 없다`() {
        assertFalse(hospitalRowValuesOf(rowOf(candidate(2.79, 10, ageSeconds = 0, isSample = false))).isSample)
    }

    @Test
    fun `응급 접수 안 함인 곳의 값도 쓸 만한 정보로 만든다`() {
        val values = hospitalRowValuesOf(rowOf(candidate(2.79, 0, ageSeconds = 60, accepting = false)))
        assertEquals(StatusFreshness.FRESH, values.freshness)
        assertEquals(1L, values.reportedMinutesAgo)
    }
}
