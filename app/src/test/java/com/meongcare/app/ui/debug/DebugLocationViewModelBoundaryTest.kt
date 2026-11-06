package com.meongcare.app.ui.debug

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import app.cash.turbine.test
import com.meongcare.app.data.guidance.navi.NaviLocationClient
import com.meongcare.app.data.guidance.navi.NaviLocationEvent
import com.meongcare.app.data.location.FakeCurrentLocationSource
import com.meongcare.app.data.location.NaviCurrentLocationSource
import com.meongcare.app.domain.location.CurrentLocation
import com.meongcare.app.domain.location.CurrentLocationSource
import com.meongcare.app.testutil.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * 011 계획 설계 결정 5의 VM 경계: 상태 순서, 두 번 누르기, 결과 유지, VM 정리, 실제 위치 출처와의 연결.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DebugLocationViewModelBoundaryTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    /** [answers]를 하나씩 채울 때까지 기다리는 가짜. 부를 때마다 새 답 자리를 만든다. */
    private class QueuedSource : CurrentLocationSource {
        val answers = mutableListOf<CompletableDeferred<CurrentLocation?>>()
        val calls get() = answers.size

        override suspend fun current(): CurrentLocation? {
            val answer = CompletableDeferred<CurrentLocation?>()
            answers += answer
            return answer.await()
        }
    }

    /** 구독하면 답하지 않고 기다린다. 열린 구독 수를 센다. */
    private class SilentNaviClient : NaviLocationClient {
        var active = 0

        override fun currentLocation(): Flow<NaviLocationEvent> = flow {
            active++
            try {
                awaitCancellation()
            } finally {
                active--
            }
        }
    }

    private val here = CurrentLocation(37.4127212, 127.094681, "수정구 시흥동")

    @Test
    fun `상태는 묻지 않음, 묻는 중, 위치 순서로 바뀐다`() = runTest {
        val source = QueuedSource()
        val vm = DebugLocationViewModel(source)
        vm.uiState.test {
            assertEquals(DebugLocationUiState.NotAsked, awaitItem())
            vm.onAsk()
            assertEquals(DebugLocationUiState.Asking, awaitItem())
            source.answers.single().complete(here)
            assertEquals(DebugLocationUiState.Found(here), awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun `위치를 본 뒤 다시 물어 모르면 이전 위치 대신 묻는 중을 거쳐 모름이 된다`() = runTest {
        val source = QueuedSource()
        val vm = DebugLocationViewModel(source)
        vm.uiState.test {
            assertEquals(DebugLocationUiState.NotAsked, awaitItem())
            vm.onAsk()
            assertEquals(DebugLocationUiState.Asking, awaitItem())
            source.answers[0].complete(here)
            assertEquals(DebugLocationUiState.Found(here), awaitItem())

            vm.onAsk()
            assertEquals(DebugLocationUiState.Asking, awaitItem())
            source.answers[1].complete(null)
            assertEquals(DebugLocationUiState.Unknown, awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun `빠르게 두 번 누르고 답이 온 뒤 한 번 더 누르면 두 번만 묻는다`() = runTest {
        // 확인 절차 L2와 같은 순서
        val source = QueuedSource()
        val vm = DebugLocationViewModel(source)
        vm.onAsk()
        vm.onAsk()
        assertEquals(1, source.calls)
        assertEquals(DebugLocationUiState.Asking, vm.uiState.value)

        source.answers[0].complete(here)
        assertEquals(DebugLocationUiState.Found(here), vm.uiState.value)

        vm.onAsk()
        assertEquals(2, source.calls)
        assertEquals(DebugLocationUiState.Asking, vm.uiState.value)
    }

    @Test
    fun `묻는 중에 여러 번 눌러도 상태는 묻는 중이고 답은 한 번만 반영된다`() = runTest {
        val source = QueuedSource()
        val vm = DebugLocationViewModel(source)
        vm.uiState.test {
            assertEquals(DebugLocationUiState.NotAsked, awaitItem())
            repeat(5) { vm.onAsk() }
            assertEquals(DebugLocationUiState.Asking, awaitItem())
            assertEquals(1, source.calls)
            source.answers.single().complete(null)
            assertEquals(DebugLocationUiState.Unknown, awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun `결과는 다시 묻기 전까지 그대로이고 저절로 다시 묻지 않는다`() = runTest {
        val source = QueuedSource()
        val vm = DebugLocationViewModel(source)
        vm.onAsk()
        source.answers.single().complete(here)
        advanceTimeBy(60 * 60 * 1_000L)
        runCurrent()
        assertEquals(DebugLocationUiState.Found(here), vm.uiState.value)
        assertEquals(1, source.calls)
    }

    @Test
    fun `보는 쪽이 없는 동안 온 결과도 나중에 보면 그대로 있다`() = runTest {
        // 결정 5: 묻는 중에 주행으로 바뀌어 줄이 숨어도 VM은 계속 기다리고, 정차하면 결과를 보인다
        val source = QueuedSource()
        val vm = DebugLocationViewModel(source)
        vm.onAsk()
        source.answers.single().complete(here)
        vm.uiState.test {
            assertEquals(DebugLocationUiState.Found(here), awaitItem())
        }
    }

    @Test
    fun `factory로 만든 VM은 넘긴 위치 출처로 묻는다`() = runTest {
        val source = QueuedSource()
        val store = ViewModelStore()
        val vm = ViewModelProvider(store, DebugLocationViewModel.factory(source))[DebugLocationViewModel::class.java]
        assertEquals(DebugLocationUiState.NotAsked, vm.uiState.value)
        vm.onAsk()
        assertEquals(1, source.calls)
        source.answers.single().complete(here)
        assertEquals(DebugLocationUiState.Found(here), vm.uiState.value)
        store.clear()
    }

    @Test
    fun `VM이 정리되면 대기 시간을 기다리지 않고 내비 연결을 놓는다`() = runTest {
        val client = SilentNaviClient()
        val store = ViewModelStore()
        val vm = ViewModelProvider(
            store,
            DebugLocationViewModel.factory(NaviCurrentLocationSource(client)),
        )[DebugLocationViewModel::class.java]
        vm.onAsk()
        runCurrent()
        assertEquals(1, client.active)

        store.clear()
        runCurrent()
        assertEquals(0, client.active)
        assertEquals(0, currentTime)
    }

    @Test
    fun `내비가 답하지 않으면 대기 시간 동안 묻는 중이다가 모름이 된다`() = runTest {
        val client = SilentNaviClient()
        val vm = DebugLocationViewModel(NaviCurrentLocationSource(client))
        val timeoutMs = NaviCurrentLocationSource.DEFAULT_TIMEOUT.inWholeMilliseconds
        vm.onAsk()
        advanceTimeBy(timeoutMs - 1)
        runCurrent()
        assertEquals(DebugLocationUiState.Asking, vm.uiState.value)

        advanceTimeBy(2)
        runCurrent()
        assertEquals(DebugLocationUiState.Unknown, vm.uiState.value)
        assertEquals(0, client.active)
    }

    @Test
    fun `Fake 위치로 물으면 바로 시연 위치 문구가 나온다`() = runTest {
        // 확인 절차 P1: 바로 "위치(Fake): 37.41272, 127.09468 · 수정구 시흥동"
        val vm = DebugLocationViewModel(FakeCurrentLocationSource())
        vm.onAsk()
        runCurrent()
        assertEquals(DebugLocationUiState.Found(FakeCurrentLocationSource.DEMO_LOCATION), vm.uiState.value)
        assertEquals("위치(Fake): 37.41272, 127.09468 · 수정구 시흥동", debugLocationText(vm.uiState.value, "Fake"))
        assertEquals(0, currentTime)
    }
}
