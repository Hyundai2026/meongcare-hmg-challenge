package com.meongcare.app.di

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 계획 007 "설계 결정 1"의 표 전체(모드 × 차량 기기)와 그 표에서 나오는 규칙.
 *
 * | 모드 | 차량 기기 | 주행 상태 | 길안내 | 시나리오 | 주행 중에만 시간 흐름 | Vehicle 연결 | 시연용 표시 |
 * |---|---|---|---|---|---|---|---|
 * | FAKE | 상관없음 | Fake | Fake | O | X | X | O |
 * | DEMO | 예 | Real | Fake | X | O | X | O |
 * | DEMO | 아니오 | Fake | Fake | O | O | X | O |
 * | REAL | 상관없음 | Real | Real | X | X | O | X |
 */
class SourcePlanBoundaryTest {

    private val deviceKinds = listOf(true, false)

    private data class Row(val mode: DataMode, val isAutomotive: Boolean, val plan: SourcePlan)

    private fun allRows(): List<Row> = DataMode.entries.flatMap { mode ->
        deviceKinds.map { isAutomotive -> Row(mode, isAutomotive, sourcePlanOf(mode, isAutomotive)) }
    }

    private fun Row.label() = "모드 $mode, 차량 기기 $isAutomotive"

    @Test
    fun `구성표는 모든 모드와 기기 조합에서 계획의 표와 같다`() {
        val fake = SourcePlan(
            driving = SourceKind.FAKE,
            guidance = SourceKind.FAKE,
            runsTripScenario = true,
            guidanceWaitsForDriving = false,
            createsVehicleConnection = false,
        )
        val demoOnCar = SourcePlan(
            driving = SourceKind.REAL,
            guidance = SourceKind.FAKE,
            runsTripScenario = false,
            guidanceWaitsForDriving = true,
            createsVehicleConnection = false,
        )
        val demoOnPhone = SourcePlan(
            driving = SourceKind.FAKE,
            guidance = SourceKind.FAKE,
            runsTripScenario = true,
            guidanceWaitsForDriving = true,
            createsVehicleConnection = false,
        )
        val real = SourcePlan(
            driving = SourceKind.REAL,
            guidance = SourceKind.REAL,
            runsTripScenario = false,
            guidanceWaitsForDriving = false,
            createsVehicleConnection = true,
        )
        val expected = mapOf(
            (DataMode.FAKE to true) to fake,
            (DataMode.FAKE to false) to fake,
            (DataMode.DEMO to true) to demoOnCar,
            (DataMode.DEMO to false) to demoOnPhone,
            (DataMode.REAL to true) to real,
            (DataMode.REAL to false) to real,
        )
        val expectedSimulated = mapOf(DataMode.FAKE to true, DataMode.DEMO to true, DataMode.REAL to false)

        assertEquals("표의 칸 수와 조합 수가 같다", expected.size, allRows().size)
        for (row in allRows()) {
            assertEquals(row.label(), expected.getValue(row.mode to row.isAutomotive), row.plan)
            assertEquals(row.label() + " 시연용 표시", expectedSimulated.getValue(row.mode), row.plan.isGuidanceSimulated)
        }
    }

    @Test
    fun `시나리오는 주행 상태와 길안내가 모두 Fake일 때만 돈다`() {
        for (row in allRows().filter { it.plan.runsTripScenario }) {
            assertEquals(row.label(), SourceKind.FAKE, row.plan.driving)
            assertEquals(row.label(), SourceKind.FAKE, row.plan.guidance)
        }
    }

    @Test
    fun `주행 중에만 시간이 흐르는 설정은 Fake 길안내에만 붙는다`() {
        for (row in allRows().filter { it.plan.guidanceWaitsForDriving }) {
            assertEquals(row.label(), SourceKind.FAKE, row.plan.guidance)
        }
    }

    @Test
    fun `Vehicle 연결은 Real 모드에서만 만든다`() {
        for (row in allRows()) {
            assertEquals(row.label(), row.mode == DataMode.REAL, row.plan.createsVehicleConnection)
        }
    }

    @Test
    fun `Fake 길안내를 쓰는 구성은 모두 시연용 표시를 하고 Real 길안내는 어느 기기에서도 하지 않는다`() {
        for (row in allRows()) {
            assertEquals(row.label(), row.plan.guidance == SourceKind.FAKE, row.plan.isGuidanceSimulated)
        }
        assertFalse(sourcePlanOf(DataMode.REAL, isAutomotive = false).isGuidanceSimulated)
    }

    @Test
    fun `시연용 표시는 다른 칸과 상관없이 길안내 종류로만 정해진다`() {
        val booleans = listOf(true, false)
        for (driving in SourceKind.entries) for (guidance in SourceKind.entries)
            for (scenario in booleans) for (waits in booleans) for (vehicle in booleans) {
                val plan = SourcePlan(driving, guidance, scenario, waits, vehicle)
                assertEquals(plan.toString(), guidance == SourceKind.FAKE, plan.isGuidanceSimulated)
            }
    }

    @Test
    fun `Fake 모드와 Real 모드는 기기 종류와 상관없이 같은 구성이다`() {
        for (mode in listOf(DataMode.FAKE, DataMode.REAL)) {
            assertEquals("$mode", sourcePlanOf(mode, isAutomotive = true), sourcePlanOf(mode, isAutomotive = false))
        }
    }

    @Test
    fun `시연 모드는 기기 종류에 따라 주행 상태 출처와 시나리오만 다르다`() {
        val onCar = sourcePlanOf(DataMode.DEMO, isAutomotive = true)
        val onPhone = sourcePlanOf(DataMode.DEMO, isAutomotive = false)

        assertEquals(SourceKind.REAL, onCar.driving)
        assertEquals(SourceKind.FAKE, onPhone.driving)
        assertFalse(onCar.runsTripScenario)
        assertTrue(onPhone.runsTripScenario)
        // 나머지 칸은 같다
        assertEquals(onCar.guidance, onPhone.guidance)
        assertEquals(onCar.guidanceWaitsForDriving, onPhone.guidanceWaitsForDriving)
        assertEquals(onCar.createsVehicleConnection, onPhone.createsVehicleConnection)
        assertEquals(onCar.isGuidanceSimulated, onPhone.isGuidanceSimulated)
    }

    @Test
    fun `Fake 주행 상태 조작은 Fake 모드와 일반 폰 시연 모드에서만 쓸 수 있다`() {
        // 설계 결정 5: debug 패널의 [정차]/[주행]/[모름]은 주행 상태가 Fake일 때만 보인다.
        val fakeDriving = allRows().filter { it.plan.driving == SourceKind.FAKE }.map { it.mode to it.isAutomotive }.toSet()

        assertEquals(
            setOf(DataMode.FAKE to true, DataMode.FAKE to false, DataMode.DEMO to false),
            fakeDriving,
        )
    }

    // ---- release 빌드 (resolveDataMode와 함께) ----

    private val savedValues = listOf(null, "", "FAKE", "DEMO", "REAL", "UNKNOWN_MODE")

    @Test
    fun `release 빌드는 어느 기기에서도 시연용 길안내를 표시하고 Vehicle 연결을 만들지 않는다`() {
        for (saved in savedValues) for (isAutomotive in deviceKinds) {
            val plan = sourcePlanOf(resolveDataMode(isDebugBuild = false, savedName = saved), isAutomotive)
            val label = "저장된 값 $saved, 차량 기기 $isAutomotive"

            assertEquals(label, SourceKind.FAKE, plan.guidance)
            assertTrue(label, plan.isGuidanceSimulated)
            assertTrue(label, plan.guidanceWaitsForDriving)
            assertFalse(label, plan.createsVehicleConnection)
        }
    }

    @Test
    fun `release 빌드는 차량 기기면 Real 주행 상태로, 아니면 Fake 주행 상태와 시나리오로 흐름을 잇는다`() {
        for (saved in savedValues) {
            val onCar = sourcePlanOf(resolveDataMode(isDebugBuild = false, savedName = saved), isAutomotive = true)
            val onPhone = sourcePlanOf(resolveDataMode(isDebugBuild = false, savedName = saved), isAutomotive = false)

            assertEquals("저장된 값 $saved", SourceKind.REAL, onCar.driving)
            assertFalse("저장된 값 $saved", onCar.runsTripScenario)
            assertEquals("저장된 값 $saved", SourceKind.FAKE, onPhone.driving)
            assertTrue("저장된 값 $saved", onPhone.runsTripScenario)
        }
    }
}
