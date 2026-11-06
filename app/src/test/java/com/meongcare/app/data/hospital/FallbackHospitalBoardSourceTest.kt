package com.meongcare.app.data.hospital

import com.meongcare.app.domain.hospital.HospitalBoard
import com.meongcare.app.domain.hospital.HospitalBoardSource
import com.meongcare.app.domain.hospital.HospitalOrigin
import java.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
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

@OptIn(ExperimentalCoroutinesApi::class)
class FallbackHospitalBoardSourceTest {

    private val now = Instant.parse("2026-11-06T05:00:00Z")
    private val serverBoard = HospitalBoard.Loaded(emptyList(), HospitalOrigin.SERVER)

    private fun source(server: HospitalBoardSource, log: MutableList<String> = mutableListOf()) =
        FallbackHospitalBoardSource(server = server, now = { now }, log = { log += it })

    @Test
    fun `서버가 주면 서버 목록이다`() = runTest {
        val log = mutableListOf<String>()
        val result = source({ delay(4_900); serverBoard }, log).load()
        assertSame(serverBoard, result)
        assertTrue(log.isEmpty())
    }

    @Test
    fun `서버가 5초 안에 답하지 않으면 내장으로 대체한다`() = runTest {
        val log = mutableListOf<String>()
        val result = async { source({ awaitCancellation() }, log).load() }
        advanceTimeBy(4_999)
        runCurrent()
        assertTrue(result.isActive)
        advanceTimeBy(2)
        runCurrent()
        val board = result.await() as HospitalBoard.Loaded
        assertEquals(HospitalOrigin.BUILT_IN_FALLBACK, board.origin)
        assertEquals(5, board.candidates.size)
        assertEquals(listOf("병원 목록: 내장 시연 (서버 시간 초과)"), log)
    }

    @Test
    fun `서버 실패면 내장으로 대체했다고 알린다`() = runTest {
        val log = mutableListOf<String>()
        val board = source({ HospitalBoard.Unavailable("HTTP 403") }, log).load() as HospitalBoard.Loaded
        assertEquals(HospitalOrigin.BUILT_IN_FALLBACK, board.origin)
        assertTrue(board.candidates.none { it.sendsCardToServer })
        assertEquals(listOf("병원 목록: 내장 시연 (서버 실패: HTTP 403)"), log)
    }

    @Test
    fun `취소는 그대로 전한다`() = runTest {
        val started = CompletableDeferred<Unit>()
        val job = async {
            source({
                started.complete(Unit)
                awaitCancellation()
            }).load()
        }
        started.await()
        job.cancel()
        val error = runCatching { job.await() }.exceptionOrNull()
        assertTrue(error is CancellationException)
    }
}
