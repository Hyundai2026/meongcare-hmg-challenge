/**
 * 프로필 폼 경계 테스트. 기준: docs/data/pet-profile.md "필드"(쓰는 쪽 제한, 글자 수 = 코드 포인트, NFC),
 * "나이로 입력할 때", 계획 008 설계 결정 6 (입력 규칙), "결정 / 배운 점" (나이를 바꾸지 않으면 추정 생년월일 유지).
 */
import { describe, expect, it } from 'vitest'
import type { LocalDate } from '../domain/localDate'
import type { PetProfile } from '../domain/petProfile'
import { parsePetProfile, serializePetProfile } from '../domain/petProfileJson'
import { applyProfileForm, profileFormValuesOf, type ProfileFormValues } from './profileFormValues'

const today: LocalDate = { year: 2026, month: 11, day: 5 }
const now = new Date('2026-11-05T00:30:00.000Z')

const blank: ProfileFormValues = profileFormValuesOf(null, today)
const form = (patch: Partial<ProfileFormValues>): ProfileFormValues => ({ ...blank, name: '보리', ...patch })

const bori: PetProfile = {
  schemaVersion: 1,
  name: '보리',
  breed: '골든리트리버',
  sex: 'male',
  birthDate: '2014-08-01',
  birthDateIsEstimated: false,
  photoDataUrl: 'data:image/jpeg;base64,AAAA',
  medical: { conditions: ['심장질환'], allergies: [], medications: [{ name: '심장약', dosage: '1일 1회' }] },
  updatedAt: '2026-01-01T00:00:00.000Z',
}

function apply(values: ProfileFormValues, base: PetProfile | null = null, day: LocalDate = today) {
  return applyProfileForm(values, base, day, now)
}

function profileOf(values: ProfileFormValues, base: PetProfile | null = null, day: LocalDate = today): PetProfile {
  const result = apply(values, base, day)
  if (!result.ok) throw new Error(`검증 실패: ${JSON.stringify(result.errors)}`)
  return result.profile
}

function errorsOf(values: ProfileFormValues, base: PetProfile | null = null, day: LocalDate = today) {
  const result = apply(values, base, day)
  return result.ok ? {} : result.errors
}

describe('프로필 폼 이름·품종 경계', () => {
  it('이름 20자 경계는 이모지도 한 글자로 센다', () => {
    expect(profileOf(form({ name: '🐶'.repeat(20) })).name).toBe('🐶'.repeat(20))
    expect(errorsOf(form({ name: '🐶'.repeat(21) }))).toEqual({ name: 'name-too-long' })
  })

  it('풀어 쓴 한글 이름 20자는 받고 NFC로 합쳐 저장한다', () => {
    const name = '가나다라마바사아자차카타파하가나다라마바'
    expect([...name]).toHaveLength(20)
    const saved = profileOf(form({ name: name.normalize('NFD') })).name
    expect(saved).toBe(name)
    expect(saved).toBe(saved.normalize('NFC'))
    expect(errorsOf(form({ name: (name + '사').normalize('NFD') }))).toEqual({ name: 'name-too-long' })
  })

  it('앞뒤 공백·탭·전각 공백은 20자에 들어가지 않는다', () => {
    expect(profileOf(form({ name: ' \t' + '가'.repeat(20) + '　' })).name).toBe('가'.repeat(20))
  })

  it('탭·줄바꿈·전각 공백뿐인 이름은 필수 오류다', () => {
    expect(errorsOf(form({ name: '\t\n　 ' }))).toEqual({ name: 'name-required' })
  })

  it('품종 30자 경계는 이모지와 풀어 쓴 한글도 글자 단위로 센다', () => {
    expect(profileOf(form({ breed: '🐕'.repeat(30) })).breed).toBe('🐕'.repeat(30))
    expect(errorsOf(form({ breed: '🐕'.repeat(31) }))).toEqual({ breed: 'breed-too-long' })
    expect(profileOf(form({ breed: '가'.repeat(30).normalize('NFD') })).breed).toBe('가'.repeat(30))
  })

  it('공백뿐인 품종은 빈 문자열이 아니라 null로 저장한다', () => {
    expect(profileOf(form({ breed: ' \t　 ' })).breed).toBeNull()
  })
})

describe('프로필 폼 생년월일 경계', () => {
  it('오늘이 2월 29일이면 30년 전 2월 28일까지 받고 그 전날은 받지 않는다', () => {
    const leapDay: LocalDate = { year: 2028, month: 2, day: 29 }
    expect(profileOf(form({ birthDate: '1998-02-28' }), null, leapDay).birthDate).toBe('1998-02-28')
    expect(errorsOf(form({ birthDate: '1998-02-27' }), null, leapDay)).toEqual({ birth: 'birth-date-too-old' })
  })

  it('12월 31일에 다음 해 1월 1일은 미래 날짜다', () => {
    const newYearsEve: LocalDate = { year: 2026, month: 12, day: 31 }
    expect(errorsOf(form({ birthDate: '2027-01-01' }), null, newYearsEve)).toEqual({ birth: 'birth-date-future' })
    expect(profileOf(form({ birthDate: '2026-12-31' }), null, newYearsEve).birthDate).toBe('2026-12-31')
  })

  it('날짜 모드로 저장하면 추정이 아니다', () => {
    expect(profileOf(form({ birthDate: '2014-08-01' })).birthDateIsEstimated).toBe(false)
  })

  it('날짜 모드에서는 숨은 나이 칸 값을 검사하지 않고, 나이 모드에서는 숨은 날짜 칸 값을 검사하지 않는다', () => {
    expect(profileOf(form({ birthMode: 'date', birthDate: '2014-08-01', ageYears: '99', ageMonths: '숫자아님' })).birthDate).toBe(
      '2014-08-01',
    )
    const byAge = profileOf(form({ birthMode: 'age', birthDate: '2099-01-01', ageYears: '1', ageMonths: '0' }))
    expect(byAge.birthDate).toBe('2025-11-05')
    expect(byAge.birthDateIsEstimated).toBe(true)
  })
})

describe('프로필 폼 나이 입력 경계', () => {
  it('0세 0개월은 오늘 태어난 것으로 추정해 저장한다', () => {
    const profile = profileOf(form({ birthMode: 'age', ageYears: '0', ageMonths: '0' }))
    expect(profile.birthDate).toBe('2026-11-05')
    expect(profile.birthDateIsEstimated).toBe(true)
  })

  it('0세 11개월은 받고 0세 12개월은 받지 않는다', () => {
    expect(profileOf(form({ birthMode: 'age', ageYears: '0', ageMonths: '11' })).birthDate).toBe('2025-12-05')
    expect(errorsOf(form({ birthMode: 'age', ageYears: '0', ageMonths: '12' }))).toHaveProperty('birth')
  })

  it('1월에 나이로 입력하면 지난해 날짜로 넘어간다', () => {
    const january: LocalDate = { year: 2026, month: 1, day: 31 }
    expect(profileOf(form({ birthMode: 'age', ageYears: '0', ageMonths: '2' }), null, january).birthDate).toBe('2025-11-30')
  })

  it('부호·지수·16진수·단위가 붙은 나이는 받지 않는다', () => {
    for (const text of ['+3', '1e1', '0x1', '3세', '3.0', '-0']) {
      expect(errorsOf(form({ birthMode: 'age', ageYears: text, ageMonths: '0' })), `세 ${text}`).toHaveProperty('birth')
      expect(errorsOf(form({ birthMode: 'age', ageYears: '1', ageMonths: text })), `개월 ${text}`).toHaveProperty('birth')
    }
  })

  it('추정이 아닌 생년월일을 나이 모드로 바꾸면 같은 나이여도 오늘 기준으로 새로 추정한다', () => {
    const values = { ...profileFormValuesOf(bori, today), birthMode: 'age' as const, ageYears: '12', ageMonths: '3' }
    const profile = profileOf(values, bori)
    expect(profile.birthDate).toBe('2014-08-05')
    expect(profile.birthDateIsEstimated).toBe(true)
  })

  it('추정 생년월일을 날짜 모드로 바꿔 저장하면 추정이 아니다', () => {
    const estimated: PetProfile = { ...bori, birthDate: '2014-07-20', birthDateIsEstimated: true }
    const values = { ...profileFormValuesOf(estimated, today), birthMode: 'date' as const }
    const profile = profileOf(values, estimated)
    expect(profile.birthDate).toBe('2014-07-20')
    expect(profile.birthDateIsEstimated).toBe(false)
  })

  it('나이를 바꾸지 않고 다시 저장해도 오늘에서 30년 전보다 앞선 생년월일은 저장하지 않는다', () => {
    // 30세 0개월로 보이지만 추정 생년월일(1996-11-01)이 30년 전(1996-11-05)보다 앞선 경우.
    const old: PetProfile = { ...bori, birthDate: '1996-11-01', birthDateIsEstimated: true }
    const values = profileFormValuesOf(old, today)
    expect(values).toMatchObject({ birthMode: 'age', ageYears: '30', ageMonths: '0' })
    const result = apply(values, old)
    expect(result.ok === false || (result.profile.birthDate ?? '') >= '1996-11-05', JSON.stringify(result)).toBe(true)
  })
})

describe('프로필 폼 열기·저장 경계', () => {
  it('품종·성별·생년월일·사진이 없는 프로필은 빈 칸과 날짜 모드로 연다', () => {
    const minimal: PetProfile = { ...bori, breed: null, sex: null, birthDate: null, photoDataUrl: null }
    expect(profileFormValuesOf(minimal, today)).toEqual({
      name: '보리',
      breed: '',
      sex: '',
      birthMode: 'date',
      birthDate: '',
      ageYears: '',
      ageMonths: '',
      photoDataUrl: null,
    })
  })

  it('열고 고치지 않고 저장하면 updatedAt 말고는 같은 값이다', () => {
    const cases: PetProfile[] = [
      bori,
      { ...bori, birthDate: '2016-02-29', birthDateIsEstimated: true },
      { ...bori, breed: null, sex: null, birthDate: null, photoDataUrl: null },
      { ...bori, sex: 'female', birthDate: '1996-11-05' },
    ]
    for (const base of cases) {
      const saved = profileOf(profileFormValuesOf(base, today), base)
      expect(saved, JSON.stringify(base)).toEqual({ ...base, updatedAt: now.toISOString() })
    }
  })

  it('사진을 뺀 폼으로 저장하면 사진이 null이다', () => {
    const values = { ...profileFormValuesOf(bori, today), photoDataUrl: null }
    expect(profileOf(values, bori).photoDataUrl).toBeNull()
  })

  it('처음 등록한 프로필은 schemaVersion 1이고 형식 문서대로 다시 읽을 수 있다', () => {
    const variants: ProfileFormValues[] = [
      form({}),
      form({ name: '🐶보리', breed: '골든리트리버', sex: 'female', birthDate: '2014-08-01' }),
      form({ birthMode: 'age', ageYears: '30', ageMonths: '0' }),
      form({ name: '보리'.normalize('NFD'), birthMode: 'age', ageYears: '', ageMonths: '' }),
    ]
    for (const values of variants) {
      const profile = profileOf(values)
      expect(profile.schemaVersion).toBe(1)
      expect(profile.updatedAt.endsWith('Z')).toBe(true)
      expect(parsePetProfile(serializePetProfile(profile)), JSON.stringify(values)).toEqual({ ok: true, profile })
    }
  })

  it('저장해도 넘긴 폼 값과 원래 프로필은 바뀌지 않는다', () => {
    const base = structuredClone(bori)
    const values = { ...profileFormValuesOf(base, today), name: '  보리2 ' }
    const valuesBefore = structuredClone(values)
    expect(profileOf(values, base).name).toBe('보리2')
    expect(base).toEqual(bori)
    expect(values).toEqual(valuesBefore)
  })
})
