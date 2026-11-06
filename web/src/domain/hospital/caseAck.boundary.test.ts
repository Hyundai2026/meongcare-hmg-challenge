import { describe, expect, it } from 'vitest'
import { ackText, isNewCase, newCaseCount, pageTitle } from './caseAck'
import type { CaseView } from './emergencyCase'

// 기준: 계획 013 설계 결정 9 "새 환자" (isNewCase = 읽을 수 있고 && 취소 아님 && acknowledgedAt === null,
// pageTitle 세 가지 모양, ackText = "카드 확인함 14:05", clockText와 같은 분 아래 버림), K5

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

describe('isNewCase 경계', () => {
  it('도착한 건도 확인되면 새 환자가 아니다', () => {
    expect(isNewCase({ ...base, status: 'arrived', acknowledgedAt: new Date('2026-11-06T05:20:00Z') })).toBe(false)
  })

  it('경로를 찾는 중인 건(남은 시간 없음)도 확인 전이면 새 환자다', () => {
    expect(isNewCase({ ...base, remainingMinutes: null, etaAt: null })).toBe(true)
  })

  it('반려견 정보를 받지 못한 건도 확인 전이면 새 환자다', () => {
    expect(isNewCase({ ...base, pet: null, symptoms: [] })).toBe(true)
  })

  it('예시 건도 확인 전이면 새 환자다', () => {
    expect(isNewCase({ ...base, isExample: true })).toBe(true)
  })

  it('읽지 못한 건은 취소 전이고 확인 전이어도 새 환자가 아니다', () => {
    expect(isNewCase({ ...base, readable: false, status: 'arrived' })).toBe(false)
  })

  it('취소된 건은 확인 전이어도 새 환자가 아니다', () => {
    expect(isNewCase({ ...base, status: 'cancelled', acknowledgedAt: null })).toBe(false)
  })
})

describe('newCaseCount 경계', () => {
  it('읽지 못한 건과 취소된 건만 있으면 0이다', () => {
    expect(newCaseCount([{ ...base, readable: false }, { ...base, id: 'c2', status: 'cancelled' }])).toBe(0)
  })

  it('목록 순서와 상관없이 센다', () => {
    const cases: CaseView[] = [
      { ...base, id: 'a', status: 'cancelled' },
      { ...base, id: 'b' },
      { ...base, id: 'c', acknowledgedAt: new Date('2026-11-06T05:01:00Z') },
      { ...base, id: 'd', status: 'arrived' },
      { ...base, id: 'e', readable: false },
    ]
    expect(newCaseCount(cases)).toBe(2)
    expect(newCaseCount([...cases].reverse())).toBe(2)
  })
})

describe('pageTitle 경계', () => {
  it('새 환자가 한 명이면 (1)이다', () => {
    expect(pageTitle('MeongCare', '시연용 바른 동물병원', 1)).toBe('(1) 새 환자 · 시연용 바른 동물병원 · MeongCare 병원')
  })

  it('두 자리 수도 그대로 붙인다', () => {
    expect(pageTitle('MeongCare', '시연용 바른 동물병원', 12)).toBe('(12) 새 환자 · 시연용 바른 동물병원 · MeongCare 병원')
  })

  it('앱 이름은 받은 값을 쓴다', () => {
    expect(pageTitle('멍케어', '바른', 0)).toBe('바른 · 멍케어 병원')
    expect(pageTitle('멍케어', null, 0)).toBe('멍케어 병원')
  })

  it('병원을 고르기 전이면 새 환자 수가 있어도 앱 이름만이다', () => {
    expect(pageTitle('MeongCare', null, 1)).toBe('MeongCare 병원')
  })
})

describe('ackText 경계', () => {
  it('한 자리 시와 분은 두 자리로 보인다', () => {
    expect(ackText(new Date(2026, 10, 6, 9, 5, 0))).toBe('카드 확인함 09:05')
  })

  it('자정은 00:00이고 23시 59분 59.999초는 23:59다', () => {
    expect(ackText(new Date(2026, 10, 7, 0, 0, 30))).toBe('카드 확인함 00:00')
    expect(ackText(new Date(2026, 10, 6, 23, 59, 59, 999))).toBe('카드 확인함 23:59')
  })
})
