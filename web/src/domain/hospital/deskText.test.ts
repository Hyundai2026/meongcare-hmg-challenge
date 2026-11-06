import { describe, expect, it } from 'vitest'
import type { CaseView } from './emergencyCase'
import { agoText, clampWait, etaText, isStale, sortCases } from './deskText'

const now = new Date(2026, 10, 6, 14, 20, 0)
const plus = (minutes: number) => new Date(now.getTime() + minutes * 60_000)

describe('etaText', () => {
  it('도착 예정 시각과 약 N분 후 (올림)', () => {
    expect(etaText(new Date(2026, 10, 6, 14, 32, 0), now)).toBe('도착 예정 14:32 · 약 12분 후')
    expect(etaText(new Date(now.getTime() + 61_000), now)).toBe('도착 예정 14:21 · 약 2분 후')
  })

  it('예정 시각이 지났으면 곧 도착', () => {
    expect(etaText(plus(-3), now)).toBe('도착 예정 14:17 · 곧 도착')
    expect(etaText(now, now)).toBe('도착 예정 14:20 · 곧 도착')
  })

  it('예정 시각이 없으면 경로를 찾는 중', () => {
    expect(etaText(null, now)).toBe('경로를 찾는 중')
  })
})

describe('agoText, isStale', () => {
  it('1분 안이면 방금, 그 뒤는 분과 시간', () => {
    expect(agoText(new Date(now.getTime() - 59_000), now)).toBe('방금')
    expect(agoText(plus(-3), now)).toBe('3분 전')
    expect(agoText(plus(-60), now)).toBe('1시간 전')
    expect(agoText(plus(-65), now)).toBe('1시간 5분 전')
  })

  it('30분이 넘으면 지난 정보다 (정확히 30분은 아직 유효)', () => {
    expect(isStale(plus(-30), now)).toBe(false)
    expect(isStale(new Date(now.getTime() - 30 * 60_000 - 1), now)).toBe(true)
  })
})

describe('clampWait', () => {
  it('−5·+5가 0~600을 넘지 않는다', () => {
    expect(clampWait(0 - 5)).toBe(0)
    expect(clampWait(600 + 5)).toBe(600)
    expect(clampWait(12.4)).toBe(12)
    expect(clampWait(Number.NaN)).toBe(0)
  })
})

describe('sortCases', () => {
  const base: CaseView = {
    id: 'x',
    readable: true,
    status: 'en_route',
    remainingMinutes: 10,
    etaAt: plus(10),
    createdAt: now,
    pet: null,
    symptoms: [],
    acknowledgedAt: null,
  }

  it('도착 → 오는 중(도착 예정 순, 예정 없음은 뒤) → 취소 순이다', () => {
    const list = [
      { ...base, id: 'cancel', status: 'cancelled' as const, etaAt: plus(1) },
      { ...base, id: 'late', etaAt: plus(20) },
      { ...base, id: 'route', etaAt: null },
      { ...base, id: 'soon', etaAt: plus(5) },
      { ...base, id: 'here', status: 'arrived' as const, etaAt: plus(30) },
    ]
    expect(sortCases(list).map((c) => c.id)).toEqual(['here', 'soon', 'late', 'route', 'cancel'])
  })

  it('같으면 id 순이고 원래 배열은 바꾸지 않는다', () => {
    const list = [{ ...base, id: 'b' }, { ...base, id: 'a' }]
    expect(sortCases(list).map((c) => c.id)).toEqual(['a', 'b'])
    expect(list.map((c) => c.id)).toEqual(['b', 'a'])
  })
})
