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
}
