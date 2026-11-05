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
    fun `시연 모드는 차량 기기에서 Real 주행 상태와 Fake 길안내를 쓰고 시나리오를 돌리지 않는다`() {
        val plan = sourcePlanOf(DataMode.DEMO, isAutomotive = true)

        assertEquals(SourceKind.REAL, plan.driving)
        assertEquals(SourceKind.FAKE, plan.guidance)
        assertFalse("주행 상태는 차가 정한다", plan.runsTripScenario)
    }

    @Test
    fun `시연 모드는 차량 기기가 아니면 Fake 주행 상태와 시나리오를 쓴다`() {
        val plan = sourcePlanOf(DataMode.DEMO, isAutomotive = false)

        assertEquals(SourceKind.FAKE, plan.driving)
        assertEquals(SourceKind.FAKE, plan.guidance)
        assertTrue(plan.runsTripScenario)
    }

    @Test
    fun `시연 모드는 주행 중일 때만 길안내 시간이 흐른다`() {
        assertTrue(sourcePlanOf(DataMode.DEMO, isAutomotive = true).guidanceWaitsForDriving)
        assertTrue(sourcePlanOf(DataMode.DEMO, isAutomotive = false).guidanceWaitsForDriving)
    }

    @Test
    fun `시연 모드는 Vehicle 연결을 만들지 않는다`() {
        assertFalse(sourcePlanOf(DataMode.DEMO, isAutomotive = true).createsVehicleConnection)
        assertFalse(sourcePlanOf(DataMode.DEMO, isAutomotive = false).createsVehicleConnection)
    }

    @Test
    fun `Fake 길안내를 쓰면 시연용 표시를 한다`() {
        assertTrue(sourcePlanOf(DataMode.FAKE, isAutomotive = false).isGuidanceSimulated)
        assertTrue(sourcePlanOf(DataMode.DEMO, isAutomotive = true).isGuidanceSimulated)
        assertTrue(sourcePlanOf(DataMode.DEMO, isAutomotive = false).isGuidanceSimulated)
    }

    @Test
    fun `Real 길안내면 시연용 표시를 하지 않는다`() {
        assertFalse(sourcePlanOf(DataMode.REAL, isAutomotive = true).isGuidanceSimulated)
    }
}
