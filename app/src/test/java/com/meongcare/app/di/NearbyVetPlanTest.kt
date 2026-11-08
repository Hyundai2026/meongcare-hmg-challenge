package com.meongcare.app.di

import org.junit.Assert.assertEquals
import org.junit.Test

/** 계획 015 "설계 결정 3"의 표 6칸. */
class NearbyVetPlanTest {

    @Test
    fun `구성표 6칸이 표와 같다`() {
        val expected = mapOf(
            (DataMode.FAKE to true) to NearbyVetKind.FAKE,
            (DataMode.FAKE to false) to NearbyVetKind.FAKE,
            (DataMode.DEMO to true) to NearbyVetKind.FUNCTION,
            (DataMode.DEMO to false) to NearbyVetKind.OFF,
            (DataMode.REAL to true) to NearbyVetKind.FUNCTION,
            (DataMode.REAL to false) to NearbyVetKind.OFF,
        )
        DataMode.entries.forEach { mode ->
            listOf(true, false).forEach { hasFunction ->
                assertEquals("$mode/$hasFunction", expected.getValue(mode to hasFunction), nearbyVetKindOf(mode, hasFunction))
            }
        }
    }
}
