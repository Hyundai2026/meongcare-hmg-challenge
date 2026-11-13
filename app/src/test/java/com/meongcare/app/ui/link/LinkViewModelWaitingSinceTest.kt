package com.meongcare.app.ui.link

import com.meongcare.app.data.link.FakeOwnerLink
import com.meongcare.app.data.pet.SamplePetProfiles
import com.meongcare.app.domain.link.LinkCheck
import com.meongcare.app.domain.link.LinkCode
import com.meongcare.app.domain.link.LinkStart
import com.meongcare.app.domain.link.OwnerLinkService
import com.meongcare.app.domain.pet.PetProfileLoad
import com.meongcare.app.testutil.MainDispatcherRule
import java.time.Instant
import kotlin.random.Random
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * 계획 018 진행 로그 D1: `LinkViewModel.waitingSince`는 대기 중이면 코드를 만든 시각, 아니면 null.
 * 화면은 이 값과 `vm.now()`로 `linkCountdownOf`를 계산한다. 기존 `uiState`는 그대로다.
 * AGENTS.md 규칙 3: 주행 중에는 연결 화면을 그리지 않는다(uiState 구독이 끊긴다).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LinkViewModelWaitingSinceTest {

    private val dispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(dispatcher)

    private val start = Instant.parse("2026-11-13T09:00:00Z")

    private class GatedLink(private val inner: FakeOwnerLink) : OwnerLinkService by inner {
        var startGate: CompletableDeferred<Unit>? = null
        var failStart = false
        var gone = false

        override suspend fun start(): LinkStart {
            startGate?.await()
            return if (failStart) LinkStart.Failed("네트워크") else inner.start()
        }

        override suspend fun check(code: LinkCode): LinkCheck = if (gone) LinkCheck.Gone else inner.check(code)
    }

    private class Setup(val vm: LinkViewModel, val link: GatedLink, val fake: FakeOwnerLink)

    private fun TestScope.setUp(): Setup {
        val clock = { start.plusMillis(testScheduler.currentTime) }
        val fake = FakeOwnerLink(Random(7), now = clock)
        val link = GatedLink(fake)
        val vm = LinkViewModel(
            link,
            MutableStateFlow(PetProfileLoad.Loaded(SamplePetProfiles.bori)),
            now = clock,
        )
        return Setup(vm, link, fake)
    }

    private fun TestScope.show(vm: LinkViewModel): Job = backgroundScope.launch { vm.uiState.collect {} }

    @Test
    fun `열기 전에는 대기 중이 아니므로 null이다`() = runTest(dispatcher) {
        val s = setUp()
        runCurrent()
        assertNull(s.vm.waitingSince.value)
    }

    @Test
    fun `코드를 만드는 중에는 null이다`() = runTest(dispatcher) {
        val s = setUp()
        s.link.startGate = CompletableDeferred()
        show(s.vm)
        s.vm.open()
        runCurrent()
        assertEquals(LinkUiState.Creating, s.vm.uiState.value)
        assertNull(s.vm.waitingSince.value)
    }

    @Test
    fun `코드를 받으면 그 코드를 만든 시각이 된다`() = runTest(dispatcher) {
        val s = setUp()
        advanceTimeBy(5_000)
        runCurrent()
        show(s.vm)
        s.vm.open()
        runCurrent()
        assertTrue(s.vm.uiState.value is LinkUiState.Waiting)
        assertEquals(start.plusSeconds(5), s.vm.waitingSince.value)
    }

    @Test
    fun `대기 중에는 시간이 흘러도 만든 시각이 그대로다`() = runTest(dispatcher) {
        val s = setUp()
        show(s.vm)
        s.vm.open()
        runCurrent()
        advanceTimeBy(123_000)
        runCurrent()
        assertTrue(s.vm.uiState.value is LinkUiState.Waiting)
        assertEquals(start, s.vm.waitingSince.value)
    }

    @Test
    fun `화면이 구독하지 않아도 대기 중이면 만든 시각을 준다`() = runTest(dispatcher) {
        val s = setUp()
        s.vm.open()
        runCurrent()
        assertEquals(start, s.vm.waitingSince.value)
    }

    @Test
    fun `3분 지나면 화면 계산이 07분 00초이고 uiState의 7분 안에와 맞는다`() = runTest(dispatcher) {
        val s = setUp()
        show(s.vm)
        s.vm.open()
        runCurrent()
        advanceTimeBy(180_000)
        runCurrent()
        val since = s.vm.waitingSince.value!!
        assertEquals(start.plusSeconds(180), s.vm.now())
        assertEquals(LinkCountdown("07:00", 0.7f), linkCountdownOf(since, s.vm.now()))
        assertEquals(7, (s.vm.uiState.value as LinkUiState.Waiting).remainingMinutes)
    }

    @Test
    fun `보호자가 코드를 입력해 연결되면 null이 된다`() = runTest(dispatcher) {
        val s = setUp()
        show(s.vm)
        s.vm.open()
        runCurrent()
        s.fake.claim()
        advanceTimeBy(2_000)
        runCurrent()
        assertTrue(s.vm.uiState.value is LinkUiState.Linked)
        assertNull(s.vm.waitingSince.value)
    }

    @Test
    fun `10분이 지나 만료되면 null이 된다`() = runTest(dispatcher) {
        val s = setUp()
        show(s.vm)
        s.vm.open()
        runCurrent()
        advanceTimeBy(600_000)
        runCurrent()
        assertEquals(LinkUiState.Expired, s.vm.uiState.value)
        assertNull(s.vm.waitingSince.value)
    }

    @Test
    fun `서버에서 코드가 지워져 만료로 바뀌면 null이 된다`() = runTest(dispatcher) {
        val s = setUp()
        show(s.vm)
        s.vm.open()
        runCurrent()
        s.link.gone = true
        advanceTimeBy(2_000)
        runCurrent()
        assertEquals(LinkUiState.Expired, s.vm.uiState.value)
        assertNull(s.vm.waitingSince.value)
    }

    @Test
    fun `코드를 만들지 못하면 null이다`() = runTest(dispatcher) {
        val s = setUp()
        s.link.failStart = true
        show(s.vm)
        s.vm.open()
        runCurrent()
        assertEquals(LinkUiState.Failed, s.vm.uiState.value)
        assertNull(s.vm.waitingSince.value)
    }

    @Test
    fun `닫으면 null이 된다`() = runTest(dispatcher) {
        val s = setUp()
        show(s.vm)
        s.vm.open()
        runCurrent()
        s.vm.close()
        runCurrent()
        assertNull(s.vm.waitingSince.value)
    }

    @Test
    fun `만료 뒤 새 코드를 받으면 새로 만든 시각이 된다`() = runTest(dispatcher) {
        val s = setUp()
        show(s.vm)
        s.vm.open()
        runCurrent()
        advanceTimeBy(600_000)
        runCurrent()
        assertNull(s.vm.waitingSince.value)

        advanceTimeBy(30_000)
        runCurrent()
        s.vm.onNewCode()
        runCurrent()
        assertEquals(start.plusSeconds(630), s.vm.waitingSince.value)
        assertEquals(LinkCountdown("10:00", 1f), linkCountdownOf(s.vm.waitingSince.value!!, s.vm.now()))
    }

    @Test
    fun `대기 중 새 코드를 받으면 만드는 동안 null이었다가 새 시각이 된다`() = runTest(dispatcher) {
        val s = setUp()
        show(s.vm)
        s.vm.open()
        runCurrent()
        advanceTimeBy(60_000)
        runCurrent()
        val gate = CompletableDeferred<Unit>()
        s.link.startGate = gate
        s.vm.onNewCode()
        runCurrent()
        assertNull(s.vm.waitingSince.value)
        gate.complete(Unit)
        runCurrent()
        val second = s.vm.waitingSince.value
        assertEquals(start.plusSeconds(60), second)
        assertNotEquals(start, second)
    }

    @Test
    fun `주행 중이라 화면을 그리지 않는 동안에도 만든 시각은 남아 정차하면 남은 시간이 이어진다`() = runTest(dispatcher) {
        val s = setUp()
        val parked = show(s.vm)
        s.vm.open()
        runCurrent()
        parked.cancel() // 주행: 연결 화면 구독이 끊긴다
        advanceTimeBy(240_000)
        runCurrent()
        assertEquals(start, s.vm.waitingSince.value)

        show(s.vm) // 다시 정차
        runCurrent()
        assertTrue(s.vm.uiState.value is LinkUiState.Waiting)
        assertEquals("06:00", linkCountdownOf(s.vm.waitingSince.value!!, s.vm.now()).text)
    }

    @Test
    fun `주행 중에 10분이 지나고 정차하면 만료로 바뀌고 null이 된다`() = runTest(dispatcher) {
        val s = setUp()
        val parked = show(s.vm)
        s.vm.open()
        runCurrent()
        parked.cancel()
        advanceTimeBy(700_000)
        runCurrent()

        show(s.vm)
        runCurrent()
        assertEquals(LinkUiState.Expired, s.vm.uiState.value)
        assertNull(s.vm.waitingSince.value)
    }
}
