package com.meongcare.app.ui.visit

import com.meongcare.app.domain.card.CardSessionState
import com.meongcare.app.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import com.meongcare.app.ui.guidance.GuidanceUiState
import com.meongcare.app.ui.home.HomePage
import com.meongcare.app.ui.home.ParkedContent
import com.meongcare.app.ui.home.parkedContentOf
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VisitViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @get:Rule val main = MainDispatcherRule(dispatcher)

    private val now = Instant.parse("2026-11-14T06:40:00Z")

    @Test
    fun `진료 완료는 병원·이동 시간·종료 시각을 기억하고 처음으로 가면 지운다`() {
        val vm = VisitViewModel(clock = { now })
        vm.toggleWaitInCar()
        assertTrue(vm.waitInCar.value)
        vm.complete("온유동물병원", 15)
        assertEquals(Visit("온유동물병원", 15, now), vm.visit.value)
        assertEquals(RecordChoice.ASK, vm.record.value)
        assertFalse("진료가 끝나면 차 안 대기는 꺼진다", vm.waitInCar.value)
        vm.clear()
        assertNull(vm.visit.value)
    }

    @Test
    fun `진료 완료와 귀가 중 쪽은 처음 화면보다 먼저 보인다`() {
        val idle = GuidanceUiState.Idle("병원")
        assertEquals(ParkedContent.AFTER_VISIT, parkedContentOf(idle, CardSessionState.None, page = HomePage.AFTER_VISIT))
        assertEquals(ParkedContent.GOING_HOME, parkedContentOf(idle, CardSessionState.None, page = HomePage.GOING_HOME))
        assertEquals(ParkedContent.HOME, parkedContentOf(idle, CardSessionState.None, page = HomePage.NONE))
    }

    @Test
    fun `차 안에서 대기를 켜면 잠시 뒤 진료 차례 호출이 오고 끄면 사라진다`() = runTest(dispatcher) {
        val vm = VisitViewModel(clock = { now }, callDelayMillis = 8_000)
        vm.toggleWaitInCar()
        advanceTimeBy(7_999); runCurrent()
        assertFalse(vm.called.value)
        advanceTimeBy(2); runCurrent()
        assertTrue(vm.called.value)
        vm.toggleWaitInCar()
        assertFalse(vm.called.value)
    }

    @Test
    fun `저장하면 기록이 남고 저장 안 함이면 남지 않는다`() {
        val vm = VisitViewModel(clock = { now })
        vm.complete("온유동물병원", 15, "구토")
        vm.saveRecord()
        assertEquals(RecordChoice.SAVED, vm.record.value)
        assertEquals(1, vm.records.value.size)
        vm.skipRecord()
        assertEquals("저장한 뒤에는 바뀌지 않는다", RecordChoice.SAVED, vm.record.value)
        vm.clear()
        vm.complete("별숲동물의료센터", 10)
        vm.skipRecord()
        assertEquals(RecordChoice.SKIPPED, vm.record.value)
        assertEquals("기록은 처음으로 가도 남는다", 1, vm.records.value.size)
    }

    @Test
    fun `이동 중 상태를 기록한다`() {
        val vm = VisitViewModel(clock = { now })
        vm.complete("온유동물병원", 15)
        vm.recordFeeling(TripFeeling.ANXIOUS)
        assertEquals(TripFeeling.ANXIOUS, vm.feeling.value)
        vm.clear()
        assertNull(vm.feeling.value)
    }

    @Test
    fun `귀가 완료 쪽도 처음 화면보다 먼저다`() {
        assertEquals(
            ParkedContent.HOME_ARRIVED,
            parkedContentOf(GuidanceUiState.Idle("병원"), CardSessionState.None, page = HomePage.HOME_ARRIVED),
        )
    }
}
