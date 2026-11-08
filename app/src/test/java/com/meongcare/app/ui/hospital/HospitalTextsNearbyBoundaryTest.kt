package com.meongcare.app.ui.hospital

import com.meongcare.app.domain.guidance.Destination
import com.meongcare.app.domain.hospital.Congestion
import com.meongcare.app.domain.hospital.Hospital
import com.meongcare.app.domain.hospital.HospitalCandidate
import com.meongcare.app.domain.hospital.HospitalOrigin
import com.meongcare.app.domain.hospital.HospitalStatus
import com.meongcare.app.domain.hospital.RankedHospital
import com.meongcare.app.domain.hospital.StatusFreshness
import com.meongcare.app.domain.hospital.exampleStatusOf
import com.meongcare.app.domain.hospital.nearbyVetRowsOf
import com.meongcare.app.domain.hospital.rankHospitals
import com.meongcare.app.domain.location.CurrentLocation
import java.time.Instant
import kotlin.time.Duration.Companion.minutes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 계획 015 "설계 결정 8"·"설계 변경 V2"의 문구 고르기와 쪽 나누기 경계 (tester). */
class HospitalTextsNearbyBoundaryTest {

    private val now = Instant.parse("2026-11-08T05:00:00Z")
    private val here = CurrentLocation(37.4127212, 127.094681, null)
    private val vet = Hospital(Destination("근처", 37.42, 127.094681), null, isSample = false)

    private fun row(origin: HospitalOrigin, status: HospitalStatus?, hospital: Hospital = vet): RankedHospital =
        rankHospitals(listOf(HospitalCandidate(hospital, status, origin)), here, now).let {
            (it.ranked + it.unranked + it.notAccepting).single()
        }

    @Test
    fun `예시 대기를 단 근처 병원 줄은 예시 표시이고 이동과 대기와 합계가 있다`() {
        val status = exampleStatusOf(vet, now)
        val values = hospitalRowValuesOf(row(HospitalOrigin.NEARBY_SEARCH, status))
        assertTrue(values.isWaitExample)
        assertEquals(status.waitMinutes, values.waitMinutes)
        assertEquals(values.travelMinutes!! + values.waitMinutes!!, values.totalMinutes)
        assertEquals(status.congestion, values.congestion)
        assertEquals(StatusFreshness.FRESH, values.freshness)
    }

    @Test
    fun `대기를 붙이지 않은 근처 병원 줄은 예시 표시가 없고 대기와 합계가 없다`() {
        val values = hospitalRowValuesOf(nearbyVetRowsOf(listOf(vet), here, emptyList()).single())
        assertFalse(values.isWaitExample)
        assertNull(values.waitMinutes)
        assertNull(values.totalMinutes)
        assertEquals(StatusFreshness.NONE, values.freshness)
    }

    @Test
    fun `제휴와 내장 병원 줄은 대기가 있어도 예시 표시가 없다`() {
        val status = HospitalStatus(10, Congestion.NORMAL, true, now)
        listOf(HospitalOrigin.SERVER, HospitalOrigin.BUILT_IN, HospitalOrigin.BUILT_IN_FALLBACK).forEach { origin ->
            assertFalse(origin.name, hospitalRowValuesOf(row(origin, status)).isWaitExample)
        }
    }

    @Test
    fun `시연용 근처 병원 줄은 시연용 표시와 예시 표시가 함께 있다`() {
        val sample = vet.copy(isSample = true)
        val values = hospitalRowValuesOf(row(HospitalOrigin.NEARBY_SEARCH, exampleStatusOf(sample, now), sample))
        assertTrue(values.isSample)
        assertTrue(values.isWaitExample)
    }

    @Test
    fun `위치를 모르면 근처 병원 줄도 이동과 합계가 없다`() {
        val status = exampleStatusOf(vet, now)
        val ranked = rankHospitals(listOf(HospitalCandidate(vet, status, HospitalOrigin.NEARBY_SEARCH)), null, now)
        val values = hospitalRowValuesOf(ranked.ranked.single())
        assertNull(values.travelMinutes)
        assertNull(values.totalMinutes)
        assertEquals(status.waitMinutes, values.waitMinutes)
        assertTrue(values.isWaitExample)
    }

    @Test
    fun `찾은 줄이 있으면 FOUND이고 다시 찾는 중이 아니면 FAILED다`() {
        val rows = nearbyVetRowsOf(listOf(vet), here, emptyList())
        assertEquals(NearbySection.FOUND, nearbySectionOf(NearbyState.Found(rows)))
        assertEquals(NearbySection.FAILED, nearbySectionOf(NearbyState.Failed(isRetrying = false)))
    }

    @Test
    fun `실제 검색의 안내는 다시 찾는 중에도 위치 전송과 출처를 알린다`() {
        assertEquals(
            listOf(NearbyNotice.SENDS_LOCATION, NearbyNotice.SOURCE_KAKAO),
            nearbyNoticesOf(NearbyState.Failed(isRetrying = true), isSampleSource = false),
        )
        assertEquals(listOf(NearbyNotice.SAMPLE), nearbyNoticesOf(NearbyState.Failed(isRetrying = true), isSampleSource = true))
    }

    @Test
    fun `가짜 근처 병원 안내에는 카카오 출처나 위치 전송이 없다`() {
        val states = listOf(NearbyState.Searching, NearbyState.Empty, NearbyState.Failed(), NearbyState.Found(emptyList()))
        states.forEach { state ->
            val notices = nearbyNoticesOf(state, isSampleSource = true)
            assertFalse(state.toString(), NearbyNotice.SOURCE_KAKAO in notices)
            assertFalse(state.toString(), NearbyNotice.SENDS_LOCATION in notices)
        }
    }

    @Test
    fun `쪽 수는 5의 배수 경계에서 한 쪽씩 늘어난다`() {
        (1..60).forEach { count ->
            val expected = (count - 1) / 5 + 1
            assertEquals("count=$count", expected, pageCountOf(count))
        }
        assertEquals(1, pageCountOf(-3))
    }

    @Test
    fun `제휴 5곳과 근처 15곳을 섞으면 4쪽이다`() {
        assertEquals(4, pageCountOf(5 + 15))
        assertEquals(5, pageCountOf(5 + 15 + 1))
    }

    @Test
    fun `바로 앞의 근처 병원 합계는 최소 이동 1분과 예시 대기의 합이다`() {
        val status = HospitalStatus(5, Congestion.LOW, true, now)
        val same = Hospital(Destination("같은 자리", here.latitude, here.longitude), null, isSample = false)
        val values = hospitalRowValuesOf(row(HospitalOrigin.NEARBY_SEARCH, status, same))
        assertEquals(1, values.travelMinutes)
        assertEquals(6, values.totalMinutes)
        assertEquals(5.minutes.inWholeMinutes.toInt(), values.waitMinutes)
    }
}
