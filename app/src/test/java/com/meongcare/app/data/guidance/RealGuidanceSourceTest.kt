package com.meongcare.app.data.guidance

import com.meongcare.app.domain.guidance.Destination
import com.meongcare.app.domain.guidance.GuidanceState
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RealGuidanceSourceTest {

    private val hospital = Destination(name = "테스트 병원", latitude = 37.0, longitude = 127.0)

    @Test
    fun `Real 자리표시는 항상 안내 없음 상태다`() = runTest {
        val source = RealGuidanceSource(log = {})

        assertEquals(listOf(GuidanceState.Idle), source.state.toList())
    }

    @Test
    fun `안내를 시작해도 상태가 바뀌지 않는다`() = runTest {
        val logs = mutableListOf<String>()
        val source = RealGuidanceSource(log = { logs += it })

        source.start(hospital)
        source.cancel()
        source.dismissArrival()

        assertEquals(listOf(GuidanceState.Idle), source.state.toList())
        assertEquals("시작과 취소 요청을 로그로 남긴다", 2, logs.size)
    }
}
