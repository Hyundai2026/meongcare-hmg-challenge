package com.meongcare.app.data.driving

import app.cash.turbine.test
import com.meongcare.app.domain.driving.DrivingState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Real은 CarUxRestrictions 구현 전 자리표시다 (계획 004 "설계", 단계 5).
 * 그때까지는 UNKNOWN만 내보내야 한다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RealDrivingStateSourceTest {

    @Test
    fun `구독하면 모름 상태를 먼저 내보낸다`() = runTest {
        RealDrivingStateSource().drivingState.test {
            assertEquals(DrivingState.UNKNOWN, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `시간이 지나도 모름 말고 다른 상태는 내보내지 않는다`() = runTest {
        val received = mutableListOf<DrivingState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            RealDrivingStateSource().drivingState.toList(received)
        }

        advanceTimeBy(60_000)
        runCurrent()

        // 같은 상태를 연달아 내보내지 않는다는 계약(DrivingStateSource KDoc)까지 포함해 정확히 한 번
        assertEquals(listOf(DrivingState.UNKNOWN), received)
    }

    @Test
    fun `구독을 끊었다가 다시 구독해도 모름 상태를 내보낸다`() = runTest {
        val source = RealDrivingStateSource()

        source.drivingState.test {
            assertEquals(DrivingState.UNKNOWN, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        source.drivingState.test {
            assertEquals(DrivingState.UNKNOWN, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
