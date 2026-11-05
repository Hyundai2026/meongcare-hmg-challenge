package com.meongcare.app.di

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 내비 사용 범위([naviUseOf])의 경계.
 * 기준: docs/plans/active/010-real-navi-guidance.md 설계 결정 5의 표(3a)와 단계 5-2,
 * release는 [resolveDataMode]와 함께 본다. [sourcePlanOf]의 표(007)와 어긋나지 않아야 한다.
 */
class NaviPlanBoundaryTest {
    private val devices = listOf(true, false)
    private val savedNames = listOf(null, "", "FAKE", "DEMO", "REAL", "real", "UNKNOWN")

    @Test
    fun `release 빌드는 저장된 값과 상관없이 차량 기기에서 경로만 내비로 보내고 일반 폰에서는 내비를 쓰지 않는다`() {
        for (saved in savedNames) {
            val mode = resolveDataMode(isDebugBuild = false, savedName = saved)
            assertEquals("saved=$saved", NaviUse.ROUTE_ONLY, naviUseOf(mode, isAutomotive = true))
            assertEquals("saved=$saved", NaviUse.NONE, naviUseOf(mode, isAutomotive = false))
        }
    }

    @Test
    fun `debug 빌드에서 저장된 모드를 모르면 Fake라서 차량 기기에서도 내비를 쓰지 않는다`() {
        for (saved in listOf(null, "", "real", "UNKNOWN")) {
            val mode = resolveDataMode(isDebugBuild = true, savedName = saved)
            for (automotive in devices) {
                assertEquals("saved=$saved, automotive=$automotive", NaviUse.NONE, naviUseOf(mode, automotive))
            }
        }
    }

    @Test
    fun `전부 내비는 길안내가 Real인 차량 기기 구성에서만 나온다`() {
        for (mode in DataMode.entries) {
            for (automotive in devices) {
                val plan = sourcePlanOf(mode, automotive)
                assertEquals(
                    "$mode, automotive=$automotive",
                    plan.guidance == SourceKind.REAL && automotive,
                    naviUseOf(mode, automotive) == NaviUse.FULL,
                )
            }
        }
    }

    @Test
    fun `경로만 내비는 실제 주행 중에만 줄어드는 시연용 길안내 구성에서만 나온다`() {
        var found = 0
        for (mode in DataMode.entries) {
            for (automotive in devices) {
                if (naviUseOf(mode, automotive) != NaviUse.ROUTE_ONLY) continue
                found++
                val plan = sourcePlanOf(mode, automotive)
                val where = "$mode, automotive=$automotive"
                assertEquals(where, SourceKind.FAKE, plan.guidance)
                assertEquals(where, SourceKind.REAL, plan.driving)
                assertTrue(where, plan.guidanceWaitsForDriving)
                assertFalse(where, plan.runsTripScenario)
                assertTrue("시연용 길안내라고 화면에 알린다: $where", plan.isGuidanceSimulated)
            }
        }
        assertEquals("경로만 내비가 나오는 조합 수", 1, found)
    }
}
