package com.meongcare.app.ui.link

import com.meongcare.app.data.link.FakeOwnerLink
import com.meongcare.app.data.pet.FakePetProfileSource
import com.meongcare.app.data.pet.LinkedPetProfileSource
import com.meongcare.app.data.pet.LinkedProfileRead
import com.meongcare.app.data.pet.LinkedProfileReader
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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * 계획 014 설계 5절 "화면"·K4, AGENTS.md 규칙 3 (주행 ↔ 정차)의 경계. 가상 시간.
 * - 만료 정확히 10분 / 1ms 전, 확인 실패·코드 없음, 주행 중 만료·연결, 만드는 중 닫기
 * - 연결됨 화면은 받은 반려견만 보인다 (샘플로 대신하지 않는다, K2)
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LinkViewModelBoundaryTest {

    private val dispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(dispatcher)

    private val start = Instant.parse("2026-11-06T09:00:00Z")
    private val kongiProfile = SamplePetProfiles.demoPet.copy(name = "콩이", breed = "말티즈")

    /** 묻기 횟수·지운 코드를 세고, 만들기·묻기 결과를 테스트가 정한다 */
    private class ScriptedLink(private val inner: FakeOwnerLink) : OwnerLinkService by inner {
        var checks = 0
        val cancelled = mutableListOf<LinkCode>()
        var failStart = false
        var startGate: CompletableDeferred<Unit>? = null
        var failNextChecks = 0

        override suspend fun start(): LinkStart {
            startGate?.await()
            return if (failStart) LinkStart.Failed("네트워크") else inner.start()
        }

        override suspend fun check(code: LinkCode): LinkCheck {
            checks++
            if (failNextChecks > 0) {
                failNextChecks--
                return LinkCheck.Failed("네트워크")
            }
            return inner.check(code)
        }

        override suspend fun cancel(code: LinkCode) {
            cancelled += code
            inner.cancel(code)
        }
    }

    private class Setup(val vm: LinkViewModel, val link: ScriptedLink, val fake: FakeOwnerLink)

    /**
     * @param codeClockOffsetMillis 코드를 만든 시각(Fake 시계)을 화면 시계보다 이만큼 늦게 둔다 (만료 1ms 전을 만들려고)
     */
    private fun TestScope.setUp(
        profile: Flow<PetProfileLoad> = MutableStateFlow(PetProfileLoad.Loaded(kongiProfile)),
        codeClockOffsetMillis: Long = 0,
        fake: FakeOwnerLink = FakeOwnerLink(Random(3), now = { start.plusMillis(testScheduler.currentTime + codeClockOffsetMillis) }),
    ): Setup {
        val link = ScriptedLink(fake)
        val vm = LinkViewModel(link, profile, now = { start.plusMillis(testScheduler.currentTime) })
        return Setup(vm, link, fake)
    }

    /** 연결 화면이 보이는 동안처럼 구독한다 (주행 화면이면 구독하지 않는다) */
    private fun TestScope.show(vm: LinkViewModel): Job = backgroundScope.launch { vm.uiState.collect {} }

    @Test
    fun `만료 1ms 전 확인에서는 아직 묻고 1분 안에를 보이고 그다음 확인에서 만료다`() = runTest(dispatcher) {
        val s = setUp(codeClockOffsetMillis = 1)
        show(s.vm)
        s.vm.open()
        runCurrent()
        val code = s.fake.pendingCode.value!!

        advanceTimeBy(600_000)
        runCurrent()
        // 화면 시계 = 만든 시각 + 9분 59.999초
        assertEquals(LinkUiState.Waiting(code.display, 1), s.vm.uiState.value)
        assertEquals(301, s.link.checks)

        advanceTimeBy(2_000)
        runCurrent()
        assertEquals(LinkUiState.Expired, s.vm.uiState.value)
        assertEquals(301, s.link.checks)
        assertTrue(code in s.link.cancelled)
    }

    @Test
    fun `정확히 10분이 된 확인에서는 묻지 않고 만료다`() = runTest(dispatcher) {
        val s = setUp()
        show(s.vm)
        s.vm.open()
        runCurrent()
        advanceTimeBy(600_000)
        runCurrent()
        assertEquals(LinkUiState.Expired, s.vm.uiState.value)
        // 0초, 2초, …, 598초에만 물었다
        assertEquals(300, s.link.checks)
    }

    @Test
    fun `확인에 실패해도 코드 화면을 유지하고 다음 간격에 다시 묻는다`() = runTest(dispatcher) {
        val s = setUp()
        s.link.failNextChecks = 3
        show(s.vm)
        s.vm.open()
        runCurrent()
        val code = s.fake.pendingCode.value!!
        advanceTimeBy(4_000)
        runCurrent()
        assertEquals(3, s.link.checks)
        assertEquals(LinkUiState.Waiting(code.display, 10), s.vm.uiState.value)
        assertTrue(s.link.cancelled.isEmpty())

        s.fake.claim()
        advanceTimeBy(2_000)
        runCurrent()
        assertTrue(s.vm.uiState.value is LinkUiState.Linked)
    }

    @Test
    fun `코드가 지워졌으면 만료 화면이고 더 묻지 않는다`() = runTest(dispatcher) {
        val s = setUp()
        show(s.vm)
        s.vm.open()
        runCurrent()
        s.fake.cancel(s.fake.pendingCode.value!!)
        advanceTimeBy(2_000)
        runCurrent()
        assertEquals(LinkUiState.Expired, s.vm.uiState.value)
        val after = s.link.checks
        advanceTimeBy(60_000)
        runCurrent()
        assertEquals(after, s.link.checks)
    }

    @Test
    fun `주행하는 동안 만료되면 다시 정차했을 때 만료를 보이고 묻지 않는다`() = runTest(dispatcher) {
        val s = setUp()
        val parked = show(s.vm)
        s.vm.open()
        runCurrent()
        val code = s.fake.pendingCode.value!!
        parked.cancel() // 기어 D: 주행 화면이 연결 화면을 대신한다
        runCurrent()
        val before = s.link.checks
        advanceTimeBy(11 * 60_000L)
        runCurrent()
        assertEquals(before, s.link.checks)

        show(s.vm) // 기어 P
        runCurrent()
        assertEquals(LinkUiState.Expired, s.vm.uiState.value)
        assertEquals(before, s.link.checks)
        assertTrue(code in s.link.cancelled)
        assertTrue(s.vm.isOpen.value)
    }

    @Test
    fun `주행하는 동안 보호자가 입력하면 다시 정차했을 때 연결됨을 보인다`() = runTest(dispatcher) {
        val s = setUp()
        val parked = show(s.vm)
        s.vm.open()
        runCurrent()
        parked.cancel()
        runCurrent()
        s.fake.claim()
        advanceTimeBy(30_000)
        runCurrent()
        // 주행 중에는 묻지 않으므로 아직 연결 전이다
        assertEquals(OwnerLinkState.Unlinked, s.fake.state.value)

        show(s.vm)
        runCurrent()
        assertEquals(LinkUiState.Linked(LinkedPet("콩이", "말티즈")), s.vm.uiState.value)
        assertTrue(s.fake.state.value is OwnerLinkState.Linked)
    }

    @Test
    fun `열기 전에는 화면을 구독해도 서버에 묻지 않는다`() = runTest(dispatcher) {
        val s = setUp()
        show(s.vm)
        runCurrent()
        advanceTimeBy(10_000)
        runCurrent()
        assertEquals(0, s.link.checks)
        assertEquals(LinkUiState.Closed, s.vm.uiState.value)
        assertFalse(s.vm.isOpen.value)
    }

    @Test
    fun `닫은 뒤에는 묻지 않는다`() = runTest(dispatcher) {
        val s = setUp()
        show(s.vm)
        s.vm.open()
        runCurrent()
        s.vm.close()
        runCurrent()
        val after = s.link.checks
        advanceTimeBy(60_000)
        runCurrent()
        assertEquals(after, s.link.checks)
    }

    @Test
    fun `닫은 뒤 다시 열면 새 코드를 만든다`() = runTest(dispatcher) {
        val s = setUp()
        show(s.vm)
        s.vm.open()
        runCurrent()
        val first = s.fake.pendingCode.value!!
        s.vm.close()
        runCurrent()
        s.vm.open()
        runCurrent()
        val second = s.fake.pendingCode.value!!
        assertNotEquals(first, second)
        assertEquals(LinkUiState.Waiting(second.display, 10), s.vm.uiState.value)
    }

    @Test
    fun `코드를 만드는 중에 닫으면 늦게 온 코드로 화면을 다시 열지 않는다`() = runTest(dispatcher) {
        val s = setUp()
        val gate = CompletableDeferred<Unit>()
        s.link.startGate = gate
        show(s.vm)
        s.vm.open()
        runCurrent()
        assertEquals(LinkUiState.Creating, s.vm.uiState.value)
        s.vm.close()
        runCurrent()
        gate.complete(Unit)
        runCurrent()
        assertFalse(s.vm.isOpen.value)
        assertEquals(LinkUiState.Closed, s.vm.uiState.value)
        advanceTimeBy(10_000)
        runCurrent()
        assertEquals(0, s.link.checks)
    }

    @Test
    fun `코드를 만들지 못한 화면에서 닫으면 지울 코드가 없다`() = runTest(dispatcher) {
        val s = setUp()
        s.link.failStart = true
        show(s.vm)
        s.vm.open()
        runCurrent()
        assertEquals(LinkUiState.Failed, s.vm.uiState.value)
        s.vm.close()
        runCurrent()
        assertTrue(s.link.cancelled.isEmpty())
        assertEquals(0, s.link.checks)
    }

    @Test
    fun `만료 화면에서 새 코드를 누르면 다시 묻기 시작한다`() = runTest(dispatcher) {
        val s = setUp()
        show(s.vm)
        s.vm.open()
        runCurrent()
        advanceTimeBy(600_000)
        runCurrent()
        assertEquals(LinkUiState.Expired, s.vm.uiState.value)
        val before = s.link.checks
        s.vm.onNewCode()
        runCurrent()
        val code = s.fake.pendingCode.value!!
        assertEquals(LinkUiState.Waiting(code.display, 10), s.vm.uiState.value)
        advanceTimeBy(2_000)
        runCurrent()
        assertEquals(before + 2, s.link.checks)
    }

    @Test
    fun `처음 화면에서 연결 끊기를 누르면 연결 전이다`() = runTest(dispatcher) {
        val s = setUp()
        show(s.vm)
        s.vm.open()
        runCurrent()
        s.fake.claim()
        advanceTimeBy(2_000)
        runCurrent()
        s.vm.close()
        assertTrue(s.fake.state.value is OwnerLinkState.Linked)
        s.vm.unlink()
        runCurrent()
        assertEquals(OwnerLinkState.Unlinked, s.fake.state.value)
    }

    @Test
    fun `연결됨 화면은 서버에서 받은 반려견만 보이고 샘플 보리를 먼저 보이지 않는다`() = runTest(dispatcher) {
        // 앱과 같은 구성: 연결 전에는 샘플 보리, 연결 뒤에는 서버에서 읽은 콩이 (LinkedPetProfileSource)
        val fake = FakeOwnerLink(Random(3), now = { start.plusMillis(testScheduler.currentTime) })
        val answer = CompletableDeferred<LinkedProfileRead>()
        val reader = LinkedProfileReader { answer.await() }
        val pets = LinkedPetProfileSource(backgroundScope, fake, FakePetProfileSource(), reader, sampleBeforeLink = true)
        backgroundScope.launch { pets.profile.collect {} } // 처음 화면이 보리를 보고 있었다
        val s = setUp(profile = pets.profile, fake = fake)
        show(s.vm)
        s.vm.open()
        runCurrent()

        fake.claim()
        advanceTimeBy(2_000)
        runCurrent()
        // 서버 응답 전: "반려견 정보를 아직 받지 못했어요"여야 한다. "보리 보호자와 연결했어요"가 아니다
        assertEquals(LinkUiState.Linked(pet = null), s.vm.uiState.value)

        answer.complete(LinkedProfileRead.Loaded(kongiProfile))
        runCurrent()
        assertEquals(LinkUiState.Linked(LinkedPet("콩이", "말티즈")), s.vm.uiState.value)
    }
}
