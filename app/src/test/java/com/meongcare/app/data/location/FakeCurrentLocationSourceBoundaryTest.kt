package com.meongcare.app.data.location

import com.meongcare.app.domain.location.CurrentLocation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 011 계획 설계 결정 4 "Fake 위치 값"과 `CurrentLocationSource` 계약(결정 1)의 경계.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FakeCurrentLocationSourceBoundaryTest {

    private val demo = FakeCurrentLocationSource.DEMO_LOCATION

    @Test
    fun `시연 위치는 Pleos 에뮬레이터에서 받은 값과 같다`() {
        // 결정 4: CurrentLocation(37.4127212, 127.094681, "수정구 시흥동") (sdk-notes [obs-navi-emu])
        assertEquals(CurrentLocation(37.4127212, 127.094681, "수정구 시흥동"), demo)
    }

    @Test
    fun `시연 위치는 내비 값 규칙을 그대로 통과한다`() {
        // 내비 경로로 같은 값이 와도 같은 결과여야 두 에뮬레이터가 같은 위치를 쓴다
        assertEquals(demo, currentLocationOf(demo.latitude, demo.longitude, demo.address))
    }

    @Test
    fun `넘긴 위치를 바꾸지 않고 돌려준다`() = runTest {
        val southWest = CurrentLocation(-33.4489, -70.6693, null)
        assertEquals(southWest, FakeCurrentLocationSource(southWest).current())
    }

    @Test
    fun `기다리지 않고 바로 답한다`() = runTest {
        assertEquals(demo, FakeCurrentLocationSource().current())
        assertEquals(0, currentTime)
    }

    @Test
    fun `따로 만든 Fake끼리도 같은 시연 위치를 준다`() = runTest {
        assertEquals(FakeCurrentLocationSource().current(), FakeCurrentLocationSource().current())
    }

    @Test
    fun `동시에 여러 번 물어도 모두 시연 위치를 받는다`() = runTest {
        val source = FakeCurrentLocationSource()
        val answers = List(5) { async { source.current() } }.awaitAll()
        assertEquals(List(5) { demo }, answers)
    }

    @Test
    fun `모름으로 만든 Fake는 동시에 물어도 모두 모름이다`() = runTest {
        val source = FakeCurrentLocationSource(location = null)
        val answers = List(3) { async { source.current() } }.awaitAll()
        answers.forEach { assertNull(it) }
    }
}
