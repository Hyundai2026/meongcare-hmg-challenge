package com.meongcare.app.ui.hospital

import com.meongcare.app.domain.guidance.Destination
import com.meongcare.app.domain.hospital.Congestion
import com.meongcare.app.domain.hospital.Hospital
import com.meongcare.app.domain.hospital.HospitalCandidate
import com.meongcare.app.domain.hospital.HospitalOrigin
import com.meongcare.app.domain.hospital.HospitalStatus
import com.meongcare.app.domain.hospital.RankBasis
import com.meongcare.app.domain.hospital.RankedHospital
import com.meongcare.app.domain.hospital.StatusFreshness
import java.time.Instant
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HospitalTextsTest {

    private val now = Instant.parse("2026-11-06T05:00:00Z")

    private fun row(freshness: StatusFreshness, travel: kotlin.time.Duration? = 19.minutes, age: kotlin.time.Duration? = 2.minutes) =
        RankedHospital(
            candidate = HospitalCandidate(
                Hospital(Destination("가", 37.0, 127.0), null, isSample = true, id = "a"),
                HospitalStatus(10, Congestion.HIGH, true, now),
                HospitalOrigin.SERVER,
            ),
            travel = travel,
            freshness = freshness,
            statusAge = age,
        )

    @Test
    fun `합계와 이동과 대기 분을 그대로 쓴다`() {
        val values = hospitalRowValuesOf(row(StatusFreshness.FRESH))
        assertEquals(29, values.totalMinutes)
        assertEquals(19, values.travelMinutes)
        assertEquals(10, values.waitMinutes)
        assertEquals(Congestion.HIGH, values.congestion)
        assertEquals(2L, values.reportedMinutesAgo)
    }

    @Test
    fun `알린 지 1분 안이면 0분이다 (화면은 방금)`() {
        assertEquals(0L, hospitalRowValuesOf(row(StatusFreshness.FRESH, age = 59.seconds)).reportedMinutesAgo)
    }

    @Test
    fun `위치를 모르면 이동과 합계가 없다`() {
        val values = hospitalRowValuesOf(row(StatusFreshness.FRESH, travel = null))
        assertNull(values.travelMinutes)
        assertNull(values.totalMinutes)
        assertEquals(10, values.waitMinutes)
    }

    @Test
    fun `지난 정보는 대기와 혼잡도를 보이지 않고 알린 지만 보인다`() {
        val values = hospitalRowValuesOf(row(StatusFreshness.STALE, age = 90.minutes))
        assertNull(values.waitMinutes)
        assertNull(values.congestion)
        assertNull(values.totalMinutes)
        assertEquals(90L, values.reportedMinutesAgo)
        assertEquals(19, values.travelMinutes)
    }

    @Test
    fun `출처마다 안내 문구가 다르다`() {
        assertEquals(
            listOf(FinderNotice.SERVER, FinderNotice.ESTIMATED_TRAVEL),
            finderNoticesOf(HospitalOrigin.SERVER, RankBasis.TRAVEL_PLUS_WAIT, isLocationSample = false),
        )
        assertEquals(
            listOf(FinderNotice.BUILT_IN, FinderNotice.ESTIMATED_TRAVEL, FinderNotice.SAMPLE_LOCATION),
            finderNoticesOf(HospitalOrigin.BUILT_IN, RankBasis.TRAVEL_PLUS_WAIT, isLocationSample = true),
        )
        assertEquals(
            listOf(FinderNotice.FALLBACK, FinderNotice.ESTIMATED_TRAVEL),
            finderNoticesOf(HospitalOrigin.BUILT_IN_FALLBACK, RankBasis.TRAVEL_PLUS_WAIT, isLocationSample = false),
        )
    }

    @Test
    fun `위치를 몰라 이동 시간을 쓰지 않았으면 어림과 시연 위치 안내가 없다`() {
        assertEquals(
            listOf(FinderNotice.BUILT_IN),
            finderNoticesOf(HospitalOrigin.BUILT_IN, RankBasis.WAIT_ONLY, isLocationSample = true),
        )
    }
}
