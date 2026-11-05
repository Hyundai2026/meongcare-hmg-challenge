import { describe, expect, it } from 'vitest'
import type { PetProfile } from '../domain/petProfile'
import { birthDateText, initialOf, medicationText, profileSubtitle } from './profileText'

const today = { year: 2026, month: 11, day: 5 }
const bori: PetProfile = {
  schemaVersion: 1,
  name: '보리',
  breed: '골든리트리버',
  sex: 'male',
  birthDate: '2014-08-01',
  birthDateIsEstimated: false,
  photoDataUrl: null,
  medical: { conditions: [], allergies: [], medications: [] },
  updatedAt: '2026-11-05T00:00:00.000Z',
}

describe('프로필 문구', () => {
  it('부제는 품종과 나이를 가운뎃점으로 잇는다', () => {
    expect(profileSubtitle(bori, today)).toBe('골든리트리버 · 12세 3개월')
  })

  it('부제에서 없는 값은 빼고, 둘 다 없으면 부제가 없다', () => {
    expect(profileSubtitle({ ...bori, breed: null }, today)).toBe('12세 3개월')
    expect(profileSubtitle({ ...bori, birthDate: null }, today)).toBe('골든리트리버')
    expect(profileSubtitle({ ...bori, breed: null, birthDate: null }, today)).toBeNull()
  })

  it('생년월일은 날짜로, 추정이면 달까지만 쓰고 나이로 입력했다고 알린다', () => {
    expect(birthDateText(bori)).toBe('2014년 8월 1일생')
    expect(birthDateText({ ...bori, birthDateIsEstimated: true })).toBe('2014년 8월생 (나이로 입력)')
    expect(birthDateText({ ...bori, birthDate: null })).toBeNull()
  })

  it('약은 복용법을 괄호로 붙이고, 복용법이 없으면 이름만 쓴다', () => {
    expect(medicationText({ name: '심장약', dosage: '1일 1회' })).toBe('심장약 (1일 1회)')
    expect(medicationText({ name: '심장약', dosage: '' })).toBe('심장약')
  })

  it('사진이 없을 때는 이름의 첫 글자를 쓴다', () => {
    expect(initialOf('보리')).toBe('보')
    expect(initialOf('🐶멍')).toBe('🐶')
  })
})
