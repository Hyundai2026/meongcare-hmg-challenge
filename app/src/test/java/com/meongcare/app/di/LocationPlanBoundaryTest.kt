package com.meongcare.app.di

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 011 계획 설계 결정 4의 표 전체 (모드 × 차량 기기). release는 [resolveDataMode]와 함께 본다.
 *
 * | 모드 | 차량 기기 | 현재 위치 |
 * |---|---|---|
 * | Fake | 상관없음 | Fake |
 * | 시연 | 예 / 아니오 | 내비 / Fake |
 * | Real | 예 / 아니오 | 내비 / Fake |
 */
class LocationPlanBoundaryTest {

    private val table = mapOf(
        (DataMode.FAKE to true) to SourceKind.FAKE,
        (DataMode.FAKE to false) to SourceKind.FAKE,
        (DataMode.DEMO to true) to SourceKind.REAL,
        (DataMode.DEMO to false) to SourceKind.FAKE,
        (DataMode.REAL to true) to SourceKind.REAL,
        (DataMode.REAL to false) to SourceKind.FAKE,
    )

    @Test
    fun `모드와 기기의 모든 조합이 결정 4의 표와 같다`() {
        for (mode in DataMode.entries) {
            for (isAutomotive in listOf(true, false)) {
                val expected = table[mode to isAutomotive]
                    ?: error("표에 없는 조합: $mode, 차량=$isAutomotive (모드가 늘었으면 결정 4의 표부터 정한다)")
                assertEquals("$mode, 차량=$isAutomotive", expected, currentLocationKindOf(mode, isAutomotive))
            }
        }
    }

    @Test
    fun `release는 저장된 모드와 상관없이 차량 기기에서 내비, 일반 폰에서 Fake 위치다`() {
        val savedNames = listOf(null, "", "FAKE", "DEMO", "REAL", "real", "없는 모드")
        for (saved in savedNames) {
            val release = resolveDataMode(isDebugBuild = false, savedName = saved)
            assertEquals("저장값=$saved, 차량", SourceKind.REAL, currentLocationKindOf(release, isAutomotive = true))
            assertEquals("저장값=$saved, 폰", SourceKind.FAKE, currentLocationKindOf(release, isAutomotive = false))
        }
    }

    @Test
    fun `debug 첫 실행이나 모르는 저장값이면 차량 기기여도 Fake 위치다`() {
        for (saved in listOf(null, "", "real", "없는 모드")) {
            val mode = resolveDataMode(isDebugBuild = true, savedName = saved)
            assertEquals("저장값=$saved", SourceKind.FAKE, currentLocationKindOf(mode, isAutomotive = true))
        }
    }

    @Test
    fun `debug에서 시연이나 Real을 고르면 차량 기기에서는 내비, 일반 폰에서는 Fake 위치다`() {
        for (saved in listOf(DataMode.DEMO.name, DataMode.REAL.name)) {
            val mode = resolveDataMode(isDebugBuild = true, savedName = saved)
            assertEquals("저장값=$saved, 차량", SourceKind.REAL, currentLocationKindOf(mode, isAutomotive = true))
            assertEquals("저장값=$saved, 폰", SourceKind.FAKE, currentLocationKindOf(mode, isAutomotive = false))
        }
    }

    @Test
    fun `debug 패널로 모드를 한 바퀴 돌리면 차량 기기의 위치 출처가 Fake, 내비, 내비, Fake 순서다`() {
        // 확인 절차의 debug 패널 순서: Fake → 시연 → Real → Fake
        val seen = generateSequence(DataMode.FAKE) { it.next() }.take(4)
            .map { currentLocationKindOf(it, isAutomotive = true) }
            .toList()
        assertEquals(listOf(SourceKind.FAKE, SourceKind.REAL, SourceKind.REAL, SourceKind.FAKE), seen)
    }
}
