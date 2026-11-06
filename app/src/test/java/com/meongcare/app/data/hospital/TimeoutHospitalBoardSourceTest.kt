package com.meongcare.app.data.hospital

import com.meongcare.app.domain.hospital.HospitalBoard
import com.meongcare.app.domain.hospital.HospitalOrigin
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** Real 모드 목록의 5초 시간 초과 (계획 012 "설계 결정 8"의 2). 대체하지 않는다. */
@OptIn(ExperimentalCoroutinesApi::class)
class TimeoutHospitalBoardSourceTest {

    private val board = HospitalBoard.Loaded(emptyList(), HospitalOrigin.SERVER)

    @Test
    fun `5초 안에 받으면 그 결과다`() = runTest {
        assertSame(board, TimeoutHospitalBoardSource({ delay(4_999); board }).load())
    }

    @Test
    fun `5초 안에 못 받으면 시간 초과로 받지 못했다고 한다`() = runTest {
        val log = mutableListOf<String>()
        val result = async { TimeoutHospitalBoardSource({ awaitCancellation() }, log = { log += it }).load() }
        advanceTimeBy(4_999)
        runCurrent()
        assertTrue(result.isActive)
        advanceTimeBy(2)
        runCurrent()
        assertEquals(HospitalBoard.Unavailable("시간 초과"), result.await())
        assertEquals(listOf("병원 목록: 서버 시간 초과"), log)
    }

    @Test
    fun `서버 실패는 그대로 전한다`() = runTest {
        assertEquals(HospitalBoard.Unavailable("HTTP 403"), TimeoutHospitalBoardSource({ HospitalBoard.Unavailable("HTTP 403") }).load())
    }
}
