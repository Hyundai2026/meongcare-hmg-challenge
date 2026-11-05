/**
 * 화면 문구 경계 테스트. 기준: 계획 008 설계 결정 4 (기본정보 탭 생년월일 문구, 헤더 부제는 추정이어도 나이),
 * 설계 결정 6 (부제는 없는 값을 빼고 둘 다 없으면 줄 없음, 약은 "심장약 (1일 1회)"),
 * docs/data/pet-profile.md "나이 계산" (생년월일이 오늘보다 뒤면 나이를 보이지 않는다).
 */
import { describe, expect, it } from 'vitest'
import type { PetProfile } from '../domain/petProfile'
import { birthDateText, initialOf, profileSubtitle, SEX_SYMBOL, SEX_TEXT } from './profileText'

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

describe('프로필 문구 경계', () => {
  it('추정 생년월일이어도 부제에는 나이를 보인다', () => {
    expect(profileSubtitle({ ...bori, birthDateIsEstimated: true }, today)).toBe('골든리트리버 · 12세 3개월')
  })

  it('생년월일이 오늘보다 뒤면(기기 시계가 틀림) 부제에서 나이를 빼고, 품종도 없으면 부제가 없다', () => {
    expect(profileSubtitle({ ...bori, birthDate: '2026-11-06' }, today)).toBe('골든리트리버')
    expect(profileSubtitle({ ...bori, breed: null, birthDate: '2026-11-06' }, today)).toBeNull()
  })

  it('품종이 빈 문자열이면 없는 값으로 보고 가운뎃점을 붙이지 않는다', () => {
    expect(profileSubtitle({ ...bori, breed: '' }, today)).toBe('12세 3개월')
    expect(profileSubtitle({ ...bori, breed: '', birthDate: null }, today)).toBeNull()
  })

  it('태어난 지 1개월이 안 되면 부제에 1개월 미만으로 보인다', () => {
    expect(profileSubtitle({ ...bori, breed: null, birthDate: '2026-11-05' }, today)).toBe('1개월 미만')
  })

  it('생년월일 문구는 월·일 앞의 0을 쓰지 않는다', () => {
    expect(birthDateText({ ...bori, birthDate: '2009-01-05' })).toBe('2009년 1월 5일생')
    expect(birthDateText({ ...bori, birthDate: '2009-12-31' })).toBe('2009년 12월 31일생')
    expect(birthDateText({ ...bori, birthDate: '2009-01-05', birthDateIsEstimated: true })).toBe('2009년 1월생 (나이로 입력)')
  })

  it('성별은 수컷 ♂, 암컷 ♀다', () => {
    expect(SEX_SYMBOL).toEqual({ male: '♂', female: '♀' })
    expect(SEX_TEXT).toEqual({ male: '수컷', female: '암컷' })
  })

  it('첫 글자는 이모지를 반으로 자르지 않고, 빈 이름이면 빈 문자열이다', () => {
    const initial = initialOf('😀보리')
    expect(initial).toBe('😀')
    expect(initial).toHaveLength(2)
    expect(initialOf('')).toBe('')
  })
})
