import { describe, expect, it } from 'vitest'
import { ackText, isNewCase, newCaseCount, pageTitle } from './caseAck'
import type { CaseView } from './emergencyCase'

const base: CaseView = {
  id: 'c1',
  readable: true,
  status: 'en_route',
  remainingMinutes: 12,
  etaAt: new Date('2026-11-06T05:12:00Z'),
  createdAt: new Date('2026-11-06T05:00:00Z'),
  pet: null,
  symptoms: [],
  acknowledgedAt: null,
}

describe('isNewCase', () => {
  it('확인 전인 오는 중·도착 건은 새 환자다', () => {
    expect(isNewCase(base)).toBe(true)
    expect(isNewCase({ ...base, status: 'arrived' })).toBe(true)
  })

  it('확인된 건, 취소된 건, 읽지 못한 건은 새 환자가 아니다', () => {
    expect(isNewCase({ ...base, acknowledgedAt: new Date('2026-11-06T05:01:00Z') })).toBe(false)
    expect(isNewCase({ ...base, status: 'cancelled' })).toBe(false)
    expect(isNewCase({ ...base, readable: false })).toBe(false)
  })
})

describe('newCaseCount', () => {
  it('새 환자만 센다', () => {
    const cases: CaseView[] = [
      base,
      { ...base, id: 'c2', status: 'arrived' },
      { ...base, id: 'c3', acknowledgedAt: new Date('2026-11-06T05:01:00Z') },
      { ...base, id: 'c4', status: 'cancelled' },
    ]
    expect(newCaseCount(cases)).toBe(2)
    expect(newCaseCount([])).toBe(0)
  })
})

describe('pageTitle', () => {
  it('새 환자가 있으면 개수를 앞에 붙인다', () => {
    expect(pageTitle('MeongCare', '시연용 바른 동물병원', 2)).toBe('(2) 새 환자 · 시연용 바른 동물병원 · MeongCare 병원')
  })

  it('새 환자가 없으면 병원 이름과 앱 이름', () => {
    expect(pageTitle('MeongCare', '시연용 바른 동물병원', 0)).toBe('시연용 바른 동물병원 · MeongCare 병원')
  })

  it('병원을 고르기 전이면 앱 이름만', () => {
    expect(pageTitle('MeongCare', null, 3)).toBe('MeongCare 병원')
  })
})

describe('ackText', () => {
  it('확인 시각을 시:분으로 보이고 초는 버린다', () => {
    const at = new Date(2026, 10, 6, 14, 5, 59)
    expect(ackText(at)).toBe('카드 확인함 14:05')
  })
})
