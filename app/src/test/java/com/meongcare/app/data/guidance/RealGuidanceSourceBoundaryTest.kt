package com.meongcare.app.data.guidance

import com.meongcare.app.domain.guidance.Destination
import com.meongcare.app.domain.guidance.GuidanceState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** 계획 005 단계 3: Real 자리표시는 항상 Idle, dismissArrival은 아무것도 하지 않는다. */
class RealGuidanceSourceBoundaryTest {

    private val hospital = Destination(name = "테스트 병원", latitude = 37.0, longitude = 127.0)

    @Test
    fun `안내를 시작한 뒤에 구독해도 안내 없음을 먼저 받는다`() = runTest {
        val source = RealGuidanceSource(log = {})

        source.start(hospital)

        assertEquals(GuidanceState.Idle, source.state.first())
    }

    @Test
    fun `도착 화면 닫기는 로그도 남기지 않는다`() = runTest {
        val logs = mutableListOf<String>()
        val source = RealGuidanceSource(log = { logs += it })

        source.dismissArrival()

        assertEquals(emptyList<String>(), logs)
        assertEquals(GuidanceState.Idle, source.state.first())
    }
}
