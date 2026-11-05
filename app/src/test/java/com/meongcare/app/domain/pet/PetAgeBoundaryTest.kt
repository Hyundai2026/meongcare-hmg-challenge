package com.meongcare.app.domain.pet

import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 나이 계산 경계. 기준: docs/data/pet-profile.md "나이 계산", "나이로 입력할 때".
 * 기대값은 보호자 웹 web/src/domain/age.boundary.test.ts와 같다 (웹과 차 앱이 같은 규칙).
 * 기본 케이스는 PetAgeTest에 있다. 여기는 해 넘김, 윤년, 말일, 30년, 웹에서 나이로 입력한 값을 본다.
 */
class PetAgeBoundaryTest {
    private fun d(text: String) = LocalDate.parse(text)

    /**
     * pet-profile.md "나이로 입력할 때": 생년월일 = 오늘에서 그만큼 뺀 날.
     * 그 달에 같은 날이 없으면 그 달 말일이다. 보호자 웹이 이렇게 저장한 값을 차 앱이 읽는다.
     */
    private fun birthDateEnteredAsAge(totalMonths: Int, today: LocalDate): LocalDate {
        val month = YearMonth.from(today).minusMonths(totalMonths.toLong())
        return month.atDay(minOf(today.dayOfMonth, month.lengthOfMonth()))
    }

    @Test
    fun `12월생은 다음 해 1월 같은 날에 1개월이 되고 그 전날에는 1개월 미만이다`() {
        assertEquals(1, ageInMonths(d("2025-12-15"), d("2026-01-15")))
        assertEquals(0, ageInMonths(d("2025-12-15"), d("2026-01-14")))
        assertEquals(AgeLabel.Months(1), ageLabelOf(d("2025-12-15"), d("2026-01-15")))
        assertEquals(AgeLabel.UnderOneMonth, ageLabelOf(d("2025-12-15"), d("2026-01-14")))
    }

    @Test
    fun `12월 31일생은 1월 31일에 1개월이고 1월 30일에는 아직 아니다`() {
        assertEquals(1, ageInMonths(d("2025-12-31"), d("2026-01-31")))
        assertEquals(0, ageInMonths(d("2025-12-31"), d("2026-01-30")))
    }

    @Test
    fun `해를 넘겨 12개월째 되는 날 1세가 되고 그 전날은 11개월이다`() {
        assertEquals(AgeLabel.Years(1), ageLabelOf(d("2025-12-20"), d("2026-12-20")))
        assertEquals(AgeLabel.Months(11), ageLabelOf(d("2025-12-20"), d("2026-12-19")))
    }

    @Test
    fun `태어난 날이 오늘이면 0개월이고 나이를 숨기지 않고 1개월 미만으로 보인다`() {
        assertEquals(0, ageInMonths(d("2026-11-05"), d("2026-11-05")))
        assertEquals(AgeLabel.UnderOneMonth, ageLabelOf(d("2026-11-05"), d("2026-11-05")))
    }

    @Test
    fun `윤년 2월 29일생은 평년 2월 28일에 11개월, 3월 1일에 1세, 다음 윤년 2월 29일에 4세다`() {
        assertEquals(AgeLabel.Months(11), ageLabelOf(d("2024-02-29"), d("2025-02-28")))
        assertEquals(AgeLabel.Years(1), ageLabelOf(d("2024-02-29"), d("2025-03-01")))
        assertEquals(AgeLabel.Years(4), ageLabelOf(d("2024-02-29"), d("2028-02-29")))
    }

    @Test
    fun `31일생은 30일까지인 달에는 그 달의 개월이 차지 않는다`() {
        assertEquals(0, ageInMonths(d("2026-01-31"), d("2026-02-28")))
        assertEquals(1, ageInMonths(d("2026-01-31"), d("2026-03-01")))
        assertEquals(0, ageInMonths(d("2026-03-31"), d("2026-04-30")))
    }

    @Test
    fun `해가 바뀌는 날 하루라도 미래인 생년월일은 나이를 보이지 않는다`() {
        assertNull(ageInMonths(d("2027-01-01"), d("2026-12-31")))
        assertNull(ageLabelOf(d("2027-01-01"), d("2026-12-31")))
        assertNull(ageLabelOf(d("2100-01-01"), d("2026-11-05")))
    }

    @Test
    fun `정확히 30년 전 생일이면 30세이고 하루 늦게 태어났으면 29세 11개월이다`() {
        assertEquals(AgeLabel.Years(30), ageLabelOf(d("1996-11-05"), d("2026-11-05")))
        assertEquals(AgeLabel.YearsAndMonths(29, 11), ageLabelOf(d("1996-11-06"), d("2026-11-05")))
    }

    @Test
    fun `개월 수 표시의 경계는 0, 1, 11, 12, 13, 24, 359, 360개월이다`() {
        assertEquals(AgeLabel.UnderOneMonth, ageLabelOf(0))
        assertEquals(AgeLabel.Months(1), ageLabelOf(1))
        assertEquals(AgeLabel.Months(11), ageLabelOf(11))
        assertEquals(AgeLabel.Years(1), ageLabelOf(12))
        assertEquals(AgeLabel.YearsAndMonths(1, 1), ageLabelOf(13))
        assertEquals(AgeLabel.Years(2), ageLabelOf(24))
        assertEquals(AgeLabel.YearsAndMonths(29, 11), ageLabelOf(359))
        assertEquals(AgeLabel.Years(30), ageLabelOf(360))
    }

    @Test
    fun `보호자가 나이로 입력한 생년월일은 같은 날 다시 계산하면 입력한 나이 그대로다`() {
        // pet-profile.md: "이 규칙이면 같은 날 다시 계산했을 때 입력한 나이가 그대로 나온다."
        // 윤년(2024)과 평년(2025)의 모든 날 × 0세 0개월 ~ 30세 0개월
        val failures = mutableListOf<String>()
        for (year in listOf(2024, 2025)) {
            var today = LocalDate.of(year, 1, 1)
            while (today.year == year) {
                for (total in 0..360) {
                    val birth = birthDateEnteredAsAge(total, today)
                    val back = ageInMonths(birth, today)
                    if (back != total || ageLabelOf(birth, today) != ageLabelOf(total)) {
                        failures += "$today 에 ${total}개월 → $birth → ${back}개월"
                    }
                }
                today = today.plusDays(1)
            }
        }
        assertEquals(emptyList<String>(), failures.take(10))
    }
}
