package com.meongcare.app.di

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SourcePlanTest {

    @Test
    fun `Fake 모드는 주행 상태와 길안내 모두 Fake이고 시나리오를 돌린다`() {
        for (isAutomotive in listOf(true, false)) {
            val plan = sourcePlanOf(DataMode.FAKE, isAutomotive)

            assertEquals(SourceKind.FAKE, plan.driving)
            assertEquals(SourceKind.FAKE, plan.guidance)
            assertTrue(plan.runsTripScenario)
            assertFalse("Fake 모드는 시간만으로 흐른다 (005 그대로)", plan.guidanceWaitsForDriving)
            assertFalse(plan.createsVehicleConnection)
        }
    }

    @Test
    fun `Real 모드는 주행 상태와 길안내 모두 Real이고 Vehicle 연결을 만든다`() {
        for (isAutomotive in listOf(true, false)) {
            val plan = sourcePlanOf(DataMode.REAL, isAutomotive)

            assertEquals(SourceKind.REAL, plan.driving)
            assertEquals(SourceKind.REAL, plan.guidance)
            assertFalse(plan.runsTripScenario)
            assertFalse(plan.guidanceWaitsForDriving)
            assertTrue(plan.createsVehicleConnection)
        }
    }

    @Test
    fun `Fake 길안내를 쓰면 시연용 표시를 한다`() {
        assertTrue(sourcePlanOf(DataMode.FAKE, isAutomotive = false).isGuidanceSimulated)
    }

    @Test
    fun `Real 길안내면 시연용 표시를 하지 않는다`() {
        assertFalse(sourcePlanOf(DataMode.REAL, isAutomotive = true).isGuidanceSimulated)
    }
}
