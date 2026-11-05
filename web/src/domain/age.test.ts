import { describe, expect, it } from 'vitest'
import { ageInMonths, ageLabel, birthDateFromAge, earliestBirthDate, formatAge } from './age'
import { daysInMonth, formatLocalDate, type LocalDate } from './localDate'

const d = (year: number, month: number, day: number): LocalDate => ({ year, month, day })

describe('나이 계산', () => {
  it('태어난 달과 일을 지났으면 그만큼 개월을 센다', () => {
    expect(ageInMonths(d(2014, 8, 1), d(2026, 11, 5))).toBe(147)
    expect(ageInMonths(d(2026, 1, 5), d(2026, 3, 5))).toBe(2)
  })

  it('오늘의 일이 태어난 일보다 작으면 한 달을 덜 센다', () => {
    expect(ageInMonths(d(2026, 1, 20), d(2026, 3, 19))).toBe(1)
    expect(ageInMonths(d(2025, 11, 6), d(2026, 11, 5))).toBe(11)
  })

  it('생년월일이 오늘보다 뒤면 나이가 없다', () => {
    expect(ageInMonths(d(2026, 11, 6), d(2026, 11, 5))).toBeNull()
    expect(ageLabel('2026-11-06', d(2026, 11, 5))).toBeNull()
  })

  it('12세 3개월처럼 세와 개월을 함께 표시한다', () => {
    expect(ageLabel('2014-08-01', d(2026, 11, 5))).toBe('12세 3개월')
  })

  it('개월이 0이면 세만 표시한다', () => {
    expect(formatAge(144)).toBe('12세')
    expect(ageLabel('2014-11-05', d(2026, 11, 5))).toBe('12세')
  })

  it('1년이 안 되면 개월만 표시한다', () => {
    expect(formatAge(3)).toBe('3개월')
    expect(formatAge(11)).toBe('11개월')
  })

  it('1개월이 안 되면 1개월 미만으로 표시한다', () => {
    expect(formatAge(0)).toBe('1개월 미만')
    expect(ageLabel('2026-11-05', d(2026, 11, 5))).toBe('1개월 미만')
  })

  it('생년월일이 없거나 읽을 수 없으면 나이 표시가 없다', () => {
    expect(ageLabel(null, d(2026, 11, 5))).toBeNull()
    expect(ageLabel('2026-02-30', d(2026, 11, 5))).toBeNull()
  })
})

describe('나이로 입력', () => {
  it('나이로 입력하면 오늘에서 그만큼 뺀 날이 생년월일이다', () => {
    expect(formatLocalDate(birthDateFromAge(12, 3, d(2026, 11, 5)))).toBe('2014-08-05')
    expect(formatLocalDate(birthDateFromAge(0, 11, d(2026, 11, 5)))).toBe('2025-12-05')
  })

  it('그 달에 같은 날이 없으면 말일로 맞춘다', () => {
    expect(formatLocalDate(birthDateFromAge(0, 1, d(2026, 3, 31)))).toBe('2026-02-28')
    expect(formatLocalDate(birthDateFromAge(0, 1, d(2024, 3, 31)))).toBe('2024-02-29')
    expect(formatLocalDate(birthDateFromAge(1, 0, d(2024, 2, 29)))).toBe('2023-02-28')
  })

  it('30년 전 날짜도 같은 방식으로 뺀다', () => {
    expect(formatLocalDate(earliestBirthDate(d(2026, 11, 5)))).toBe('1996-11-05')
    expect(formatLocalDate(earliestBirthDate(d(2024, 2, 29)))).toBe('1994-02-28')
  })

  it('나이로 입력한 값은 같은 날 다시 계산하면 그대로 나온다', () => {
    for (const year of [2024, 2026]) {
      for (let month = 1; month <= 12; month++) {
        for (let day = 1; day <= daysInMonth(year, month); day++) {
          const today = d(year, month, day)
          for (let total = 0; total <= 360; total++) {
            const birth = birthDateFromAge(Math.floor(total / 12), total % 12, today)
            const back = ageInMonths(birth, today)
            if (back !== total) {
              throw new Error(`${formatLocalDate(today)}에 ${total}개월 → ${formatLocalDate(birth)} → ${back}개월`)
            }
          }
        }
      }
    }
  })
})
