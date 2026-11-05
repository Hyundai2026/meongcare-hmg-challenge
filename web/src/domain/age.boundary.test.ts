/**
 * 나이 계산 경계 테스트. 기준: docs/data/pet-profile.md "나이 계산", "나이로 입력할 때".
 * 기본 케이스는 age.test.ts에 있다. 여기는 해 넘김, 윤년, 말일, 30년 경계를 본다.
 */
import { describe, expect, it } from 'vitest'
import { ageInMonths, ageLabel, birthDateFromAge, earliestBirthDate, formatAge } from './age'
import { compareLocalDate, daysInMonth, formatLocalDate, parseLocalDate, type LocalDate } from './localDate'

function day(text: string): LocalDate {
  const date = parseLocalDate(text)
  if (date === null) throw new Error(`테스트 날짜가 틀렸다: ${text}`)
  return date
}

describe('나이 계산 경계', () => {
  it('12월생은 다음 해 1월 같은 날에 1개월이 되고 그 전날에는 1개월 미만이다', () => {
    expect(ageInMonths(day('2025-12-15'), day('2026-01-15'))).toBe(1)
    expect(ageInMonths(day('2025-12-15'), day('2026-01-14'))).toBe(0)
    expect(ageLabel('2025-12-15', day('2026-01-14'))).toBe('1개월 미만')
  })

  it('12월 31일생은 1월 31일에 1개월이고 1월 30일에는 아직 아니다', () => {
    expect(ageInMonths(day('2025-12-31'), day('2026-01-31'))).toBe(1)
    expect(ageInMonths(day('2025-12-31'), day('2026-01-30'))).toBe(0)
  })

  it('해를 넘겨 12개월째 되는 날 1세가 되고 그 전날은 11개월이다', () => {
    expect(ageLabel('2025-12-20', day('2026-12-20'))).toBe('1세')
    expect(ageLabel('2025-12-20', day('2026-12-19'))).toBe('11개월')
  })

  it('태어난 날이 오늘이면 0개월이고 1개월 미만으로 표시한다', () => {
    expect(ageInMonths(day('2026-11-05'), day('2026-11-05'))).toBe(0)
    expect(ageLabel('2026-11-05', day('2026-11-05'))).toBe('1개월 미만')
  })

  it('윤년 2월 29일생은 평년 2월 28일에는 아직 1세가 아니고 3월 1일에 1세다', () => {
    // 규칙 그대로: 오늘의 일(28)이 태어난 일(29)보다 작으면 한 달을 덜 센다.
    expect(ageLabel('2024-02-29', day('2025-02-28'))).toBe('11개월')
    expect(ageLabel('2024-02-29', day('2025-03-01'))).toBe('1세')
    expect(ageLabel('2024-02-29', day('2028-02-29'))).toBe('4세')
  })

  it('31일생은 30일까지인 달에는 그 달의 개월이 차지 않는다', () => {
    expect(ageInMonths(day('2026-01-31'), day('2026-02-28'))).toBe(0)
    expect(ageInMonths(day('2026-01-31'), day('2026-03-01'))).toBe(1)
    expect(ageInMonths(day('2026-03-31'), day('2026-04-30'))).toBe(0)
  })

  it('해가 바뀌는 날 하루라도 미래인 생년월일은 나이를 보이지 않는다', () => {
    expect(ageInMonths(day('2027-01-01'), day('2026-12-31'))).toBeNull()
    expect(ageLabel('2027-01-01', day('2026-12-31'))).toBeNull()
  })

  it('정확히 30년 전 생일이면 30세다', () => {
    expect(ageLabel('1996-11-05', day('2026-11-05'))).toBe('30세')
    expect(ageLabel('1996-11-06', day('2026-11-05'))).toBe('29세 11개월')
  })

  it('개월 수 표시의 경계: 0, 1, 11, 12, 13, 24, 359, 360개월', () => {
    expect(formatAge(0)).toBe('1개월 미만')
    expect(formatAge(1)).toBe('1개월')
    expect(formatAge(11)).toBe('11개월')
    expect(formatAge(12)).toBe('1세')
    expect(formatAge(13)).toBe('1세 1개월')
    expect(formatAge(24)).toBe('2세')
    expect(formatAge(359)).toBe('29세 11개월')
    expect(formatAge(360)).toBe('30세')
  })
})

describe('나이로 입력 경계', () => {
  it('1월에 1개월을 빼면 지난해 12월이다', () => {
    expect(formatLocalDate(birthDateFromAge(0, 1, day('2026-01-15')))).toBe('2025-12-15')
    expect(formatLocalDate(birthDateFromAge(1, 1, day('2026-01-15')))).toBe('2024-12-15')
  })

  it('윤년 3월 31일에서 1개월을 빼면 2월 29일, 평년이면 2월 28일이다', () => {
    expect(formatLocalDate(birthDateFromAge(0, 1, day('2024-03-31')))).toBe('2024-02-29')
    expect(formatLocalDate(birthDateFromAge(0, 1, day('2025-03-31')))).toBe('2025-02-28')
  })

  it('2월 29일에서 1년을 빼면 2월 28일, 4년을 빼면 2월 29일이다', () => {
    expect(formatLocalDate(birthDateFromAge(1, 0, day('2024-02-29')))).toBe('2023-02-28')
    expect(formatLocalDate(birthDateFromAge(4, 0, day('2024-02-29')))).toBe('2020-02-29')
  })

  it('31일에서 30일까지인 달로 빼면 그 달 말일이다', () => {
    expect(formatLocalDate(birthDateFromAge(0, 1, day('2026-05-31')))).toBe('2026-04-30')
    expect(formatLocalDate(birthDateFromAge(0, 3, day('2026-05-31')))).toBe('2026-02-28')
  })

  it('0세 0개월이면 생년월일은 오늘이다', () => {
    expect(formatLocalDate(birthDateFromAge(0, 0, day('2026-11-05')))).toBe('2026-11-05')
  })

  it('가장 이른 생년월일은 오늘에서 30년 전이고 2월 29일이면 2월 28일이다', () => {
    expect(formatLocalDate(earliestBirthDate(day('2026-11-05')))).toBe('1996-11-05')
    expect(formatLocalDate(earliestBirthDate(day('2026-01-01')))).toBe('1996-01-01')
    expect(formatLocalDate(earliestBirthDate(day('2028-02-29')))).toBe('1998-02-28')
    expect(formatLocalDate(earliestBirthDate(day('2024-02-29')))).toBe('1994-02-28')
  })

  it('30세 0개월로 입력한 생년월일은 가장 이른 생년월일과 같다', () => {
    for (const today of ['2026-11-05', '2028-02-29', '2025-12-31', '2026-03-31']) {
      expect(birthDateFromAge(30, 0, day(today)), today).toEqual(earliestBirthDate(day(today)))
    }
  })

  it('윤년과 평년의 모든 날에 0세 0개월부터 30세 0개월까지 입력하면 같은 나이가 다시 나온다', () => {
    const failures: string[] = []
    for (const year of [2024, 2025]) {
      for (let month = 1; month <= 12; month++) {
        for (let d = 1; d <= daysInMonth(year, month); d++) {
          const today: LocalDate = { year, month, day: d }
          const earliest = earliestBirthDate(today)
          for (let years = 0; years <= 30; years++) {
            for (let months = 0; months <= 11; months++) {
              const total = years * 12 + months
              if (total > 360) continue
              const birth = birthDateFromAge(years, months, today)
              const label = `${formatLocalDate(today)} ${years}세 ${months}개월 → ${formatLocalDate(birth)}`
              if (parseLocalDate(formatLocalDate(birth)) === null) failures.push(`없는 날짜: ${label}`)
              else if (ageInMonths(birth, today) !== total) failures.push(`나이가 달라짐: ${label}`)
              else if (ageLabel(formatLocalDate(birth), today) !== formatAge(total)) failures.push(`표시가 달라짐: ${label}`)
              else if (compareLocalDate(birth, earliest) < 0) failures.push(`30년보다 앞: ${label}`)
            }
          }
        }
      }
    }
    expect(failures.slice(0, 10)).toEqual([])
  })
})
