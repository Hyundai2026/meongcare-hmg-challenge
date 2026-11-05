package com.meongcare.app.domain.pet

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** 기대값은 보호자 웹 web/src/domain/age.test.ts와 같다 (docs/data/pet-profile.md "나이 계산"). */
class PetAgeTest {
    private fun d(year: Int, month: Int, day: Int) = LocalDate.of(year, month, day)

    @Test
    fun `예시 생년월일은 147개월이라 12세 3개월이다`() {
        assertEquals(147, ageInMonths(d(2014, 8, 1), d(2026, 11, 5)))
        assertEquals(AgeLabel.YearsAndMonths(12, 3), ageLabelOf(d(2014, 8, 1), d(2026, 11, 5)))
    }

    @Test
    fun `오늘의 일이 태어난 일보다 작으면 한 달을 뺀다`() {
        assertEquals(1, ageInMonths(d(2026, 1, 20), d(2026, 3, 19)))
        assertEquals(11, ageInMonths(d(2025, 11, 6), d(2026, 11, 5)))
    }

    @Test
    fun `생년월일이 오늘보다 뒤면 나이를 보이지 않는다`() {
        assertNull(ageInMonths(d(2026, 11, 6), d(2026, 11, 5)))
        assertNull(ageLabelOf(d(2026, 11, 6), d(2026, 11, 5)))
        assertNull(ageLabelOf(null, d(2026, 11, 5)))
    }

    @Test
    fun `12개월은 1세, 11개월은 11개월, 0개월은 1개월 미만이다`() {
        assertEquals(AgeLabel.Years(1), ageLabelOf(12))
        assertEquals(AgeLabel.Months(11), ageLabelOf(11))
        assertEquals(AgeLabel.UnderOneMonth, ageLabelOf(0))
        assertEquals(AgeLabel.Years(12), ageLabelOf(d(2014, 11, 5), d(2026, 11, 5)))
    }

    @Test
    fun `2월 29일생은 평년 2월 28일에 아직 한 달이 차지 않는다`() {
        // 웹 age.boundary.test.ts: 평년 2월 28일에는 아직 1세가 아니고 3월 1일에 1세다
        assertEquals(11, ageInMonths(d(2024, 2, 29), d(2025, 2, 28)))
        assertEquals(12, ageInMonths(d(2024, 2, 29), d(2025, 3, 1)))
    }
}
