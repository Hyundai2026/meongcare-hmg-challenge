package com.meongcare.app.domain.pet

import java.time.LocalDate

/*
 * 나이 계산. 규칙은 docs/data/pet-profile.md "나이 계산"이고, 보호자 웹(web/src/domain/age.ts)과 같다.
 * 오늘은 항상 인자로 받는다 (테스트에서 고정).
 */

/** 생년월일부터 오늘까지 개월 수. 생년월일이 오늘보다 뒤면 null. */
fun ageInMonths(birth: LocalDate, today: LocalDate): Int? {
    if (birth.isAfter(today)) return null
    var months = (today.year - birth.year) * 12 + (today.monthValue - birth.monthValue)
    if (today.dayOfMonth < birth.dayOfMonth) months -= 1
    return months
}

/** 나이 표시의 네 가지 꼴. 글자("12세 3개월")는 화면이 strings.xml로 붙인다. */
sealed interface AgeLabel {
    /** 12세 3개월 */
    data class YearsAndMonths(val years: Int, val months: Int) : AgeLabel

    /** 12세 */
    data class Years(val years: Int) : AgeLabel

    /** 3개월 */
    data class Months(val months: Int) : AgeLabel

    /** 1개월 미만 */
    data object UnderOneMonth : AgeLabel
}

fun ageLabelOf(months: Int): AgeLabel {
    if (months < 1) return AgeLabel.UnderOneMonth
    val years = months / 12
    val rest = months % 12
    return when {
        years == 0 -> AgeLabel.Months(rest)
        rest == 0 -> AgeLabel.Years(years)
        else -> AgeLabel.YearsAndMonths(years, rest)
    }
}

/** 생년월일이 없거나 오늘보다 뒤면(기기 시계가 틀림) null. 나이를 보이지 않는다. */
fun ageLabelOf(birthDate: LocalDate?, today: LocalDate): AgeLabel? {
    if (birthDate == null) return null
    return ageInMonths(birthDate, today)?.let(::ageLabelOf)
}
