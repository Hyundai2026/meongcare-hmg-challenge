package com.meongcare.app.data.location

import com.meongcare.app.domain.location.CurrentLocation
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FakeCurrentLocationSourceTest {

    @Test
    fun `기본값은 Pleos 에뮬레이터와 같은 시연 위치를 바로 돌려준다`() = runTest {
        val location = FakeCurrentLocationSource().current()
        assertEquals(CurrentLocation(37.4127212, 127.094681, "수정구 시흥동"), location)
    }

    @Test
    fun `여러 번 불러도 같은 값을 준다`() = runTest {
        val source = FakeCurrentLocationSource()
        assertEquals(source.current(), source.current())
    }

    @Test
    fun `위치를 null로 만들면 항상 모름이다`() = runTest {
        val source = FakeCurrentLocationSource(location = null)
        assertNull(source.current())
        assertNull(source.current())
    }
}
