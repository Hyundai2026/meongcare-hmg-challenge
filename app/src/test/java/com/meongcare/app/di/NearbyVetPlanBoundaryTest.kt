package com.meongcare.app.di

import com.meongcare.app.data.server.nearbyFunctionUrlOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/** 계획 015 "설계 결정 3"(`nearbyVetKindOf`)과 범위 추가(`usesDeviceLocation`)의 경계 (tester). */
class NearbyVetPlanBoundaryTest {

    @Test
    fun `release 빌드는 저장된 모드와 에뮬레이터 설정과 상관없이 근처 병원이 꺼진다`() {
        val saved = listOf(null, "", "FAKE", "DEMO", "REAL", "모름")
        saved.forEach { name ->
            val mode = resolveDataMode(isDebugBuild = false, savedName = name)
            val url = nearbyFunctionUrlOf("demo-meongcare", "127.0.0.1", isDebugBuild = false)
            // 서버 익명 계정이 있어도 함수 주소가 없으니 끈다 (hasFunction = 주소 있음 && 계정 있음)
            val serverClientExists = true
            assertEquals("saved=$name", NearbyVetKind.OFF, nearbyVetKindOf(mode, hasFunction = url != null && serverClientExists))
        }
    }

    @Test
    fun `release 빌드가 아니어도 함수 주소가 없으면 시연과 Real은 꺼진다`() {
        listOf(DataMode.DEMO, DataMode.REAL).forEach { mode ->
            val url = nearbyFunctionUrlOf("", "", isDebugBuild = true)
            assertEquals(mode.name, NearbyVetKind.OFF, nearbyVetKindOf(mode, hasFunction = url != null))
        }
    }

    @Test
    fun `Fake 모드는 함수 설정과 상관없이 가짜 근처 병원이고 실제 함수를 부르지 않는다`() {
        listOf(true, false).forEach { has ->
            val kind = nearbyVetKindOf(DataMode.FAKE, has)
            assertEquals(NearbyVetKind.FAKE, kind)
        }
    }

    @Test
    fun `Fake 모드는 어떤 기기에서도 기기 위치를 쓰지 않는다`() {
        assertFalse(usesDeviceLocation(DataMode.FAKE, isAutomotive = true))
        assertFalse(usesDeviceLocation(DataMode.FAKE, isAutomotive = false))
    }

    @Test
    fun `차량 기기는 어떤 모드에서도 기기 위치를 쓰지 않는다`() {
        DataMode.entries.forEach { mode -> assertFalse(mode.name, usesDeviceLocation(mode, isAutomotive = true)) }
    }

    @Test
    fun `release로 일반 폰에서 돌리면 기기 위치를 쓴다`() {
        val mode = resolveDataMode(isDebugBuild = false, savedName = "FAKE")
        assertEquals(true, usesDeviceLocation(mode, isAutomotive = false))
    }
}
