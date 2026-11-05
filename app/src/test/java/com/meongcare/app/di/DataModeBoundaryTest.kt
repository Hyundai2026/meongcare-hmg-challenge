package com.meongcare.app.di

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * 계획 007 "설계 결정 2"의 경계: 프로세스 시작 때 데이터 모드를 정하는 규칙과 debug 패널 전환 순서.
 *
 * - debug: 저장된 이름이 [DataMode] 값이면 그것, 없거나 모르는 값이면 FAKE
 * - release: 저장된 값과 상관없이 DEMO
 * - 전환 순서: FAKE → DEMO → REAL → FAKE
 */
class DataModeBoundaryTest {

    @Test
    fun `데이터 모드는 Fake, 시연, Real 세 가지다`() {
        assertEquals(setOf("FAKE", "DEMO", "REAL"), DataMode.entries.map { it.name }.toSet())
    }

    // ---- debug 빌드 ----

    @Test
    fun `debug 빌드는 저장된 이름으로 모든 모드를 그대로 되살린다`() {
        for (mode in DataMode.entries) {
            assertEquals("저장된 값: ${mode.name}", mode, resolveDataMode(isDebugBuild = true, savedName = mode.name))
        }
    }

    @Test
    fun `debug 빌드에서 빈 문자열이 저장돼 있으면 Fake다`() {
        assertEquals(DataMode.FAKE, resolveDataMode(isDebugBuild = true, savedName = ""))
    }

    @Test
    fun `debug 빌드에서 이름이 정확히 같지 않으면 모르는 값으로 보고 Fake다`() {
        // 대소문자·앞뒤 공백이 다르거나, 화면 표시 이름("시연")이 저장된 경우
        for (saved in listOf("demo", "Demo", "real", " DEMO", "DEMO ", "시연", "Real")) {
            assertEquals("저장된 값: '$saved'", DataMode.FAKE, resolveDataMode(isDebugBuild = true, savedName = saved))
        }
    }

    // ---- release 빌드 ----

    @Test
    fun `release 빌드는 어떤 값이 저장돼 있어도 시연 모드다`() {
        val savedValues = listOf(null, "", "UNKNOWN_MODE", "demo", "시연") + DataMode.entries.map { it.name }
        for (saved in savedValues) {
            assertEquals("저장된 값: $saved", DataMode.DEMO, resolveDataMode(isDebugBuild = false, savedName = saved))
        }
    }

    // ---- debug 패널 전환 ----

    @Test
    fun `모드를 전환하면 항상 다른 모드가 된다`() {
        for (mode in DataMode.entries) {
            assertNotEquals("$mode 에서 전환", mode, mode.next())
        }
    }

    @Test
    fun `어느 모드에서든 세 번 전환하면 모든 모드를 한 번씩 지나 처음 모드로 돌아온다`() {
        for (start in DataMode.entries) {
            val visited = generateSequence(start) { it.next() }.take(3).toList()
            assertEquals("$start 에서 시작", DataMode.entries.toSet(), visited.toSet())
            assertEquals("$start 에서 시작", start, start.next().next().next())
        }
    }

    @Test
    fun `debug 빌드를 처음 켜고 한 번 전환하면 시연 모드다`() {
        val first = resolveDataMode(isDebugBuild = true, savedName = null)

        assertEquals(DataMode.DEMO, first.next())
    }
}
