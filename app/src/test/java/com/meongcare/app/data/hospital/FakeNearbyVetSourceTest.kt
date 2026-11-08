package com.meongcare.app.data.hospital

import com.meongcare.app.data.location.FakeCurrentLocationSource
import com.meongcare.app.domain.hospital.NearbyVets
import com.meongcare.app.domain.hospital.nearbyVetRowsOf
import kotlin.time.Duration
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeNearbyVetSourceTest {

    private val here = FakeCurrentLocationSource.DEMO_LOCATION

    @Test
    fun `기본 3곳은 모두 5km 안에 남고 가나다 순으로 가깝다`() = runTest {
        val found = FakeNearbyVetSource(delay = Duration.ZERO).search(here) as NearbyVets.Found
        val rows = nearbyVetRowsOf(found.hospitals, here, partners = emptyList())

        assertEquals(
            listOf("시연용 근처 동물병원 가", "시연용 근처 동물병원 나", "시연용 근처 동물병원 다"),
            rows.map { it.hospital.name },
        )
        assertTrue(rows.all { it.hospital.isSample })
    }

    @Test
    fun `번호가 없는 곳이 하나 있다`() {
        assertNull(FakeNearbyVetSource.DEFAULT[1].phoneNumber)
        assertEquals(2, FakeNearbyVetSource.DEFAULT.count { it.phoneNumber != null })
    }

    @Test
    fun `결과를 바꿔 넣을 수 있다`() = runTest {
        val source = FakeNearbyVetSource(delay = Duration.ZERO) { NearbyVets.Unavailable("시간 초과") }
        assertEquals(NearbyVets.Unavailable("시간 초과"), source.search(here))
    }
}
