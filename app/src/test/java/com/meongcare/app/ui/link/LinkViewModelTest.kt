package com.meongcare.app.ui.link

import com.meongcare.app.data.link.FakeOwnerLink
import com.meongcare.app.data.pet.SamplePetProfiles
import com.meongcare.app.domain.link.LinkCheck
import com.meongcare.app.domain.link.LinkCode
import com.meongcare.app.domain.link.LinkStart
import com.meongcare.app.domain.link.OwnerLinkService
import com.meongcare.app.domain.link.OwnerLinkState
import com.meongcare.app.domain.pet.PetProfileLoad
import com.meongcare.app.testutil.MainDispatcherRule
import java.time.Instant
import kotlin.random.Random
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** 계획 014 단계 3-1. 가상 시간으로 2초 간격·10분 만료를 본다 */
@OptIn(ExperimentalCoroutinesApi::class)
class LinkViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(dispatcher)

    private val start = Instant.parse("2026-11-06T09:00:00Z")
    private val kongi = SamplePetProfiles.demoPet.copy(name = "콩이", breed = "말티즈")

    /** 묻기 횟수와 지운 코드를 센다 */
    private class CountingLink(private val inner: FakeOwnerLink) : OwnerLinkService by inner {
        var checks = 0
        val cancelled = mutableListOf<LinkCode>()
        var failStart = false
        override suspend fun start(): LinkStart = if (failStart) LinkStart.Failed("네트워크") else inner.start()
        override suspend fun check(code: LinkCode): LinkCheck {
            checks++
            return inner.check(code)
        }
        override suspend fun cancel(code: LinkCode) {
            cancelled += code
            inner.cancel(code)
        }
    }

    private fun TestScope.setUp(profile: StateFlow<PetProfileLoad> = MutableStateFlow(PetProfileLoad.Loaded(kongi))): Triple<LinkViewModel, CountingLink, FakeOwnerLink> {
        val clock = { start.plusMillis(testScheduler.currentTime) }
        val fake = FakeOwnerLink(Random(3), now = clock)
        val link = CountingLink(fake)
        return Triple(LinkViewModel(link, profile, now = clock), link, fake)
    }

    /** 화면이 보이는 동안처럼 구독한다 */
    private fun TestScope.show(vm: LinkViewModel): Job = backgroundScope.launch { vm.uiState.collect {} }

    @Test
    fun `열면 코드를 만들어 보인다`() = runTest(dispatcher) {
        val (vm, _, fake) = setUp()
        show(vm)
        vm.open()
        runCurrent()
        assertTrue(vm.isOpen.value)
        assertEquals(LinkUiState.Waiting(fake.pendingCode.value!!.display, 10), vm.uiState.value)
    }

    @Test
    fun `보이는 동안 2초마다 연결됐는지 묻는다`() = runTest(dispatcher) {
        val (vm, link, _) = setUp()
        show(vm)
        vm.open()
        runCurrent()
        assertEquals(1, link.checks)
        advanceTimeBy(1_999)
        runCurrent()
        assertEquals(1, link.checks)
        advanceTimeBy(1)
        runCurrent()
        assertEquals(2, link.checks)
        advanceTimeBy(10_000)
        runCurrent()
        assertEquals(7, link.checks)
    }

    @Test
    fun `화면이 사라지면 묻지 않는다`() = runTest(dispatcher) {
        val (vm, link, _) = setUp()
        val shown = show(vm)
        vm.open()
        runCurrent()
        shown.cancel()
        runCurrent()
        val before = link.checks
        advanceTimeBy(60_000)
        runCurrent()
        assertEquals(before, link.checks)
        // 다시 보이면 같은 코드로 다시 묻는다 (주행 → 정차)
        show(vm)
        runCurrent()
        assertEquals(before + 1, link.checks)
        assertTrue(vm.uiState.value is LinkUiState.Waiting)
    }

    @Test
    fun `10분이 지나면 만료를 보이고 묻지 않는다`() = runTest(dispatcher) {
        val (vm, link, fake) = setUp()
        show(vm)
        vm.open()
        runCurrent()
        val code = fake.pendingCode.value!!
        advanceTimeBy(9 * 60_000L)
        runCurrent()
        assertEquals(LinkUiState.Waiting(code.display, 1), vm.uiState.value)
        advanceTimeBy(60_000)
        runCurrent()
        assertEquals(LinkUiState.Expired, vm.uiState.value)
        assertTrue(code in link.cancelled)
        val after = link.checks
        advanceTimeBy(60_000)
        runCurrent()
        assertEquals(after, link.checks)
    }

    @Test
    fun `새 코드를 누르면 이전 코드를 지우고 새로 만든다`() = runTest(dispatcher) {
        val (vm, link, fake) = setUp()
        show(vm)
        vm.open()
        runCurrent()
        val first = fake.pendingCode.value!!
        vm.onNewCode()
        runCurrent()
        val second = fake.pendingCode.value!!
        assertTrue(first in link.cancelled)
        assertEquals(LinkUiState.Waiting(second.display, 10), vm.uiState.value)
    }

    @Test
    fun `연결되면 반려견 이름을 보이고 묻기를 멈춘다`() = runTest(dispatcher) {
        val (vm, link, fake) = setUp()
        show(vm)
        vm.open()
        runCurrent()
        fake.claim()
        advanceTimeBy(2_000)
        runCurrent()
        assertEquals(LinkUiState.Linked(LinkedPet("콩이", "말티즈")), vm.uiState.value)
        assertTrue(fake.state.value is OwnerLinkState.Linked)
        val after = link.checks
        advanceTimeBy(60_000)
        runCurrent()
        assertEquals(after, link.checks)
        // [확인]은 닫기다. 연결된 코드는 지우지 않는다 (data 쪽이 지운다)
        vm.close()
        runCurrent()
        assertFalse(vm.isOpen.value)
        assertTrue(link.cancelled.isEmpty())
    }

    @Test
    fun `코드 만들기에 실패하면 다시 시도를 보인다`() = runTest(dispatcher) {
        val (vm, link, _) = setUp()
        link.failStart = true
        show(vm)
        vm.open()
        runCurrent()
        assertEquals(LinkUiState.Failed, vm.uiState.value)
        link.failStart = false
        vm.onNewCode()
        runCurrent()
        assertTrue(vm.uiState.value is LinkUiState.Waiting)
    }

    @Test
    fun `닫으면 claim 안 된 코드를 지운다`() = runTest(dispatcher) {
        val (vm, link, fake) = setUp()
        show(vm)
        vm.open()
        runCurrent()
        val code = fake.pendingCode.value!!
        vm.close()
        runCurrent()
        assertFalse(vm.isOpen.value)
        assertEquals(LinkUiState.Closed, vm.uiState.value)
        assertEquals(listOf(code), link.cancelled)
    }

    @Test
    fun `열려 있으면 다시 열어도 코드가 바뀌지 않는다`() = runTest(dispatcher) {
        val (vm, _, fake) = setUp()
        show(vm)
        vm.open()
        runCurrent()
        val code = fake.pendingCode.value
        vm.open()
        runCurrent()
        assertEquals(code, fake.pendingCode.value)
    }
}
