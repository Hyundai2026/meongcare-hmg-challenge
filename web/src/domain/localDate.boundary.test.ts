/**
 * 날짜 경계 테스트. 기준: docs/data/pet-profile.md "birthDate" ('YYYY-MM-DD', 없는 날짜는 손상),
 * "나이 계산" (오늘 = 기기의 지역 날짜).
 */
import { describe, expect, it } from 'vitest'
import { daysInMonth, formatLocalDate, localDateOf, minusMonths, parseLocalDate } from './localDate'

describe('날짜 경계', () => {
  it('윤년 규칙: 2000년 2월 29일은 있고 2100년·2023년 2월 29일은 없다', () => {
    expect(parseLocalDate('2000-02-29')).toEqual({ year: 2000, month: 2, day: 29 })
    expect(parseLocalDate('2024-02-29')).toEqual({ year: 2024, month: 2, day: 29 })
    expect(parseLocalDate('2100-02-29')).toBeNull()
    expect(parseLocalDate('2023-02-29')).toBeNull()
    expect(daysInMonth(2000, 2)).toBe(29)
    expect(daysInMonth(2100, 2)).toBe(28)
  })

  it('월·일 범위 밖이나 30일까지인 달의 31일은 없는 날짜다', () => {
    for (const text of ['2026-00-10', '2026-13-01', '2026-01-00', '2026-01-32', '2026-04-31', '2026-06-31', '2026-09-31', '2026-11-31']) {
      expect(parseLocalDate(text), text).toBeNull()
    }
    expect(parseLocalDate('2026-12-31')).toEqual({ year: 2026, month: 12, day: 31 })
  })

  it('앞뒤 공백·줄바꿈·부호·시각이 붙거나 자릿수가 다르면 날짜로 읽지 않는다', () => {
    for (const text of [' 2026-01-01', '2026-01-01 ', '2026-01-01\n', '+2026-01-01', '2026-01-01T00:00', '20260101', '26-01-01', '2026/01/01', '']) {
      expect(parseLocalDate(text), JSON.stringify(text)).toBeNull()
    }
  })

  it('전각 숫자는 날짜로 읽지 않는다', () => {
    expect(parseLocalDate('２０２６-０１-０１')).toBeNull()
  })

  it('날짜를 쓸 때 월·일을 두 자리로 채운다', () => {
    expect(formatLocalDate({ year: 2026, month: 1, day: 5 })).toBe('2026-01-05')
  })

  it('오늘은 기기 시간대의 날짜다: 지역 자정 직후와 직전', () => {
    expect(localDateOf(new Date(2026, 0, 1, 0, 0, 0, 0))).toEqual({ year: 2026, month: 1, day: 1 })
    expect(localDateOf(new Date(2025, 11, 31, 23, 59, 59, 999))).toEqual({ year: 2025, month: 12, day: 31 })
  })

  it('달을 빼서 해가 여러 번 바뀌어도 맞고, 없는 날은 말일로 맞춘다', () => {
    expect(minusMonths({ year: 2026, month: 1, day: 15 }, 13)).toEqual({ year: 2024, month: 12, day: 15 })
    expect(minusMonths({ year: 2026, month: 1, day: 31 }, 25)).toEqual({ year: 2023, month: 12, day: 31 })
    expect(minusMonths({ year: 2024, month: 12, day: 31 }, 1)).toEqual({ year: 2024, month: 11, day: 30 })
    expect(minusMonths({ year: 2024, month: 3, day: 30 }, 1)).toEqual({ year: 2024, month: 2, day: 29 })
  })
})
