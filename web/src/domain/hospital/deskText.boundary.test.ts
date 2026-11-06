import { describe, expect, it } from 'vitest'
import type { CaseView } from './emergencyCase'
import { WAIT_MINUTES_MAX } from './hospitalStatus'
import { STALE_AFTER_MINUTES, WAIT_PRESETS, WAIT_STEP, agoText, clampWait, clockText, etaText, isStale, sortCases } from './deskText'

// 기준: 계획 012 "설계 결정 7"(상태 입력 버튼 값, −5·+5, 0~600, 30분이 지나면 안내, "도착 예정 HH:mm · 약 N분 후",
// 도착 예정이 빠른 순·취소는 맨 아래), docs/data/hospital-status.md "오래된 정보"(정확히 30분은 유효),
// docs/data/emergency-case.md "읽는 쪽 규칙"(null이면 경로를 찾는 중)
const now = new Date(2026, 10, 6, 14, 20, 0)
const ms = (n: number) => new Date(now.getTime() + n)
const MIN = 60_000

describe('etaText 경계', () => {
  it('남은 시간은 분 단위로 올림한다 (1밀리초만 남아도 약 1분 후)', () => {
    expect(etaText(ms(1), now)).toMatch(/· 약 1분 후$/)
    expect(etaText(ms(MIN), now)).toMatch(/· 약 1분 후$/)
    expect(etaText(ms(MIN + 1), now)).toMatch(/· 약 2분 후$/)
    expect(etaText(ms(12 * MIN), now)).toMatch(/· 약 12분 후$/)
  })

  it('정확히 지금이거나 지났으면 곧 도착이다', () => {
    expect(etaText(ms(0), now)).toMatch(/· 곧 도착$/)
    expect(etaText(ms(-1), now)).toMatch(/· 곧 도착$/)
    expect(etaText(ms(-90 * MIN), now)).toMatch(/· 곧 도착$/)
  })

  it('시각은 24시간 두 자리로 쓴다', () => {
    expect(etaText(new Date(2026, 10, 6, 9, 5, 0), new Date(2026, 10, 6, 9, 0, 0))).toBe('도착 예정 09:05 · 약 5분 후')
    expect(etaText(new Date(2026, 10, 7, 0, 3, 0), new Date(2026, 10, 6, 23, 58, 0))).toBe('도착 예정 00:03 · 약 5분 후')
    expect(clockText(new Date(2026, 10, 6, 23, 59, 59))).toBe('23:59')
  })
})

describe('agoText, isStale 경계', () => {
  it('60초부터 1분 전이고 59분 59초는 59분 전이다', () => {
    expect(agoText(ms(-MIN), now)).toBe('1분 전')
    expect(agoText(ms(-(60 * MIN - 1000)), now)).toBe('59분 전')
    expect(agoText(ms(-120 * MIN), now)).toBe('2시간 전')
    expect(agoText(ms(-(25 * 60 + 1) * MIN), now)).toBe('25시간 1분 전')
  })

  it('알린 시각이 미래(시계 차이)면 방금이다', () => {
    expect(agoText(ms(3 * MIN), now)).toBe('방금')
  })

  it('정확히 30분은 유효하고 1밀리초라도 넘으면 지난 정보다', () => {
    expect(STALE_AFTER_MINUTES).toBe(30)
    expect(isStale(ms(-30 * MIN), now)).toBe(false)
    expect(isStale(ms(-30 * MIN - 1), now)).toBe(true)
    expect(isStale(ms(5 * MIN), now)).toBe(false)
  })
})

describe('대기 입력 값 경계', () => {
  it('빠른 버튼은 계획의 8개 값이고 모두 0~600 정수다', () => {
    expect(WAIT_PRESETS).toEqual([0, 10, 20, 30, 45, 60, 90, 120])
    expect(WAIT_PRESETS.every((v) => Number.isInteger(v) && v >= 0 && v <= WAIT_MINUTES_MAX)).toBe(true)
    expect(WAIT_STEP).toBe(5)
  })

  it('0과 600 그 자체는 그대로 두고 범위 안의 값은 바꾸지 않는다', () => {
    expect(clampWait(0)).toBe(0)
    expect(clampWait(600)).toBe(600)
    expect(clampWait(45)).toBe(45)
    expect(clampWait(600 - WAIT_STEP)).toBe(595)
  })

  it('−5·+5를 아무리 눌러도 0~600 정수를 벗어나지 않는다', () => {
    let value = 0
    for (let i = 0; i < 200; i++) value = clampWait(value + WAIT_STEP)
    expect(value).toBe(600)
    for (let i = 0; i < 200; i++) value = clampWait(value - WAIT_STEP)
    expect(value).toBe(0)
    expect(Number.isInteger(clampWait(37.5))).toBe(true)
  })
})

describe('sortCases 경계', () => {
  const base: CaseView = {
    id: 'x',
    readable: true,
    status: 'en_route',
    remainingMinutes: 10,
    etaAt: ms(10 * MIN),
    createdAt: now,
    pet: null,
    symptoms: [],
    acknowledgedAt: null,
  }

  it('취소는 도착 예정이 가장 빨라도 맨 아래다', () => {
    const list: CaseView[] = [
      { ...base, id: 'cancel', status: 'cancelled', etaAt: ms(-60 * MIN) },
      { ...base, id: 'late', etaAt: ms(90 * MIN) },
    ]
    expect(sortCases(list).map((c) => c.id)).toEqual(['late', 'cancel'])
  })

  it('도착 예정이 같으면 먼저 보낸 건이 먼저다', () => {
    const list: CaseView[] = [
      { ...base, id: 'a', createdAt: ms(2 * MIN) },
      { ...base, id: 'b', createdAt: ms(1 * MIN) },
    ]
    expect(sortCases(list).map((c) => c.id)).toEqual(['b', 'a'])
  })

  it('도착 예정과 보낸 시각이 모두 없으면 id 순이다', () => {
    const list: CaseView[] = [
      { ...base, id: 'c', etaAt: null, createdAt: null },
      { ...base, id: 'a', etaAt: null, createdAt: null },
      { ...base, id: 'b', etaAt: null, createdAt: null },
    ]
    expect(sortCases(list).map((c) => c.id)).toEqual(['a', 'b', 'c'])
  })

  it('읽지 못한 건은 오는 중 묶음의 도착 예정 있는 건 뒤, 취소 앞이다', () => {
    const unreadable: CaseView = { ...base, id: 'unreadable', readable: false, etaAt: null, createdAt: null, remainingMinutes: null }
    const list: CaseView[] = [
      { ...base, id: 'cancel', status: 'cancelled' },
      unreadable,
      { ...base, id: 'soon', etaAt: ms(5 * MIN) },
    ]
    expect(sortCases(list).map((c) => c.id)).toEqual(['soon', 'unreadable', 'cancel'])
  })

  it('같은 건들이면 들어온 순서와 상관없이 늘 같은 순서다', () => {
    const list: CaseView[] = [
      { ...base, id: 'e', status: 'cancelled', etaAt: null },
      { ...base, id: 'd', etaAt: null, createdAt: null },
      { ...base, id: 'c', etaAt: ms(5 * MIN) },
      { ...base, id: 'b', status: 'arrived', etaAt: null },
      { ...base, id: 'a', etaAt: ms(5 * MIN), createdAt: ms(-MIN) },
    ]
    const expected = sortCases(list).map((c) => c.id)
    expect(sortCases([...list].reverse()).map((c) => c.id)).toEqual(expected)
    expect(sortCases([list[2], list[0], list[4], list[1], list[3]]).map((c) => c.id)).toEqual(expected)
    expect(expected[expected.length - 1]).toBe('e')
  })

  it('빈 목록은 빈 목록이다', () => {
    expect(sortCases([])).toEqual([])
  })
})
