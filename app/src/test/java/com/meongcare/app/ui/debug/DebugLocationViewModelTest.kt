package com.meongcare.app.ui.debug

import com.meongcare.app.domain.location.CurrentLocation
import com.meongcare.app.domain.location.CurrentLocationSource
import com.meongcare.app.testutil.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DebugLocationViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    /** [answer]를 채울 때까지 기다리는 가짜. 부른 횟수를 센다. */
    private class PendingSource : CurrentLocationSource {
        var calls = 0
        var answer = CompletableDeferred<CurrentLocation?>()

        override suspend fun current(): CurrentLocation? {
            calls++
            return answer.await()
        }
    }

    private val here = CurrentLocation(37.4127212, 127.094681, "수정구 시흥동")

    @Test
    fun `처음에는 묻지 않은 상태이고 위치를 묻지 않는다`() {
        val source = PendingSource()
        val vm = DebugLocationViewModel(source)
        assertEquals(DebugLocationUiState.NotAsked, vm.uiState.value)
        assertEquals(0, source.calls)
    }

    @Test
    fun `묻는 동안은 묻는 중이고 답이 오면 위치를 보여 준다`() = runTest {
        val source = PendingSource()
        val vm = DebugLocationViewModel(source)
        vm.onAsk()
        assertEquals(DebugLocationUiState.Asking, vm.uiState.value)
        source.answer.complete(here)
        assertEquals(DebugLocationUiState.Found(here), vm.uiState.value)
    }

    @Test
    fun `모르면 모름 상태가 된다`() = runTest {
        val source = PendingSource()
        val vm = DebugLocationViewModel(source)
        vm.onAsk()
        source.answer.complete(null)
        assertEquals(DebugLocationUiState.Unknown, vm.uiState.value)
    }

    @Test
    fun `묻는 중에 다시 눌러도 한 번만 묻는다`() = runTest {
        val source = PendingSource()
        val vm = DebugLocationViewModel(source)
        vm.onAsk()
        vm.onAsk()
        assertEquals(1, source.calls)
    }

    @Test
    fun `답을 받은 뒤에는 다시 물을 수 있다`() = runTest {
        val source = PendingSource()
        val vm = DebugLocationViewModel(source)
        vm.onAsk()
        source.answer.complete(null)
        source.answer = CompletableDeferred()
        vm.onAsk()
        assertEquals(2, source.calls)
        assertEquals(DebugLocationUiState.Asking, vm.uiState.value)
        source.answer.complete(here)
        assertEquals(DebugLocationUiState.Found(here), vm.uiState.value)
    }
}
