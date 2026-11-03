package com.meongcare.app.data.driving

import app.cash.turbine.test
import com.meongcare.app.domain.driving.DrivingState
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class FakeDrivingStateSourceTest {

    @Test
    fun `처음에는 정차 상태를 내보낸다`() = runTest {
        val source = FakeDrivingStateSource()

        source.drivingState.test {
            assertEquals(DrivingState.PARKED, awaitItem())
        }
    }

    @Test
    fun `주행으로 바꾸면 주행 상태를 내보낸다`() = runTest {
        val source = FakeDrivingStateSource()

        source.drivingState.test {
            assertEquals(DrivingState.PARKED, awaitItem())
            source.set(DrivingState.DRIVING)
            assertEquals(DrivingState.DRIVING, awaitItem())
        }
    }

    @Test
    fun `같은 상태로 다시 바꾸면 새 값을 내보내지 않는다`() = runTest {
        val source = FakeDrivingStateSource()

        source.drivingState.test {
            assertEquals(DrivingState.PARKED, awaitItem())
            source.set(DrivingState.PARKED)
            expectNoEvents()
        }
    }

    @Test
    fun `정차에서 주행으로 갔다가 정차를 거쳐 다시 주행으로 가면 바뀐 상태를 순서대로 모두 내보낸다`() = runTest {
        val source = FakeDrivingStateSource()

        source.drivingState.test {
            assertEquals(DrivingState.PARKED, awaitItem())
            source.set(DrivingState.DRIVING)
            assertEquals(DrivingState.DRIVING, awaitItem())
            source.set(DrivingState.PARKED)
            assertEquals(DrivingState.PARKED, awaitItem())
            source.set(DrivingState.DRIVING)
            assertEquals(DrivingState.DRIVING, awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun `주행 중에 모름으로 바꾸면 모름 상태를 내보낸다`() = runTest {
        val source = FakeDrivingStateSource()

        source.drivingState.test {
            assertEquals(DrivingState.PARKED, awaitItem())
            source.set(DrivingState.DRIVING)
            assertEquals(DrivingState.DRIVING, awaitItem())
            source.set(DrivingState.UNKNOWN)
            assertEquals(DrivingState.UNKNOWN, awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun `모름에서 정차로 돌아오면 정차 상태를 내보낸다`() = runTest {
        val source = FakeDrivingStateSource()

        source.drivingState.test {
            assertEquals(DrivingState.PARKED, awaitItem())
            source.set(DrivingState.UNKNOWN)
            assertEquals(DrivingState.UNKNOWN, awaitItem())
            source.set(DrivingState.PARKED)
            assertEquals(DrivingState.PARKED, awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun `상태가 바뀐 뒤에 구독하면 처음 상태가 아니라 지금 상태를 먼저 받는다`() = runTest {
        val source = FakeDrivingStateSource()
        source.set(DrivingState.DRIVING)

        source.drivingState.test {
            assertEquals(DrivingState.DRIVING, awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun `구독을 취소한 사이에 바뀐 상태는 다시 구독하면 지금 상태 하나로 받는다`() = runTest {
        val source = FakeDrivingStateSource()

        source.drivingState.test {
            assertEquals(DrivingState.PARKED, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        source.set(DrivingState.DRIVING)
        source.set(DrivingState.UNKNOWN)

        source.drivingState.test {
            assertEquals(DrivingState.UNKNOWN, awaitItem())
            expectNoEvents()
        }
    }
}
