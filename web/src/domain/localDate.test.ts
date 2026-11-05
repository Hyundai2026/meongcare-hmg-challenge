import { describe, expect, it } from 'vitest'
import { compareLocalDate, formatLocalDate, localDateOf, minusMonths, parseLocalDate } from './localDate'

describe('날짜', () => {
  it('YYYY-MM-DD를 읽고 다시 같은 글자로 쓴다', () => {
    const date = parseLocalDate('2014-08-01')
    expect(date).toEqual({ year: 2014, month: 8, day: 1 })
    expect(formatLocalDate(date!)).toBe('2014-08-01')
  })

  it('형식이 다르거나 없는 날짜는 읽지 않는다', () => {
    for (const text of ['2014-8-1', '2014/08/01', '20140801', '2014-08-01T00:00', '', '2023-02-29', '2026-13-01', '2026-00-10', '2026-04-31']) {
      expect(parseLocalDate(text), text).toBeNull()
    }
    expect(parseLocalDate('2024-02-29')).not.toBeNull()
  })

  it('기기의 지역 날짜를 쓴다', () => {
    expect(localDateOf(new Date(2026, 10, 5, 23, 59))).toEqual({ year: 2026, month: 11, day: 5 })
  })

  it('날짜를 비교한다', () => {
    expect(compareLocalDate({ year: 2026, month: 11, day: 5 }, { year: 2026, month: 11, day: 6 })).toBeLessThan(0)
    expect(compareLocalDate({ year: 2026, month: 12, day: 1 }, { year: 2026, month: 11, day: 30 })).toBeGreaterThan(0)
    expect(compareLocalDate({ year: 2026, month: 11, day: 5 }, { year: 2026, month: 11, day: 5 })).toBe(0)
  })

  it('달을 빼면 해가 바뀐다', () => {
    expect(minusMonths({ year: 2026, month: 1, day: 15 }, 1)).toEqual({ year: 2025, month: 12, day: 15 })
    expect(minusMonths({ year: 2026, month: 1, day: 15 }, 25)).toEqual({ year: 2023, month: 12, day: 15 })
  })
})
