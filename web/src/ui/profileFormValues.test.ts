import { describe, expect, it } from 'vitest'
import type { LocalDate } from '../domain/localDate'
import type { PetProfile } from '../domain/petProfile'
import { applyProfileForm, profileFormValuesOf, type ProfileFormValues } from './profileFormValues'

const today: LocalDate = { year: 2026, month: 11, day: 5 }
const now = new Date('2026-11-05T00:30:00.000Z')

const empty: ProfileFormValues = profileFormValuesOf(null, today)
const form = (patch: Partial<ProfileFormValues>): ProfileFormValues => ({ ...empty, name: '보리', ...patch })

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

function errorsOf(values: ProfileFormValues, base: PetProfile | null = null) {
  const result = applyProfileForm(values, base, today, now)
  return result.ok ? {} : result.errors
}

function profileOf(values: ProfileFormValues, base: PetProfile | null = null): PetProfile {
  const result = applyProfileForm(values, base, today, now)
  if (!result.ok) throw new Error(`검증 실패: ${JSON.stringify(result.errors)}`)
  return result.profile
}

describe('프로필 폼', () => {
  it('이름이 비었거나 공백뿐이면 필수 오류다', () => {
    expect(errorsOf(form({ name: '' }))).toEqual({ name: 'name-required' })
    expect(errorsOf(form({ name: '   ' }))).toEqual({ name: 'name-required' })
  })

  it('이름은 앞뒤 공백을 지우고 20자까지다', () => {
    expect(profileOf(form({ name: '  보리  ' })).name).toBe('보리')
    expect(profileOf(form({ name: '가'.repeat(20) })).name).toHaveLength(20)
    expect(errorsOf(form({ name: '가'.repeat(21) }))).toEqual({ name: 'name-too-long' })
  })

  it('품종은 비워도 되고 비면 null로 저장한다', () => {
    expect(profileOf(form({ breed: '' })).breed).toBeNull()
    expect(profileOf(form({ breed: ' 골든리트리버 ' })).breed).toBe('골든리트리버')
    expect(errorsOf(form({ breed: '가'.repeat(31) }))).toEqual({ breed: 'breed-too-long' })
  })

  it('생년월일을 비우면 생년월일 없이 저장된다', () => {
    const profile = profileOf(form({ birthDate: '' }))
    expect(profile.birthDate).toBeNull()
    expect(profile.birthDateIsEstimated).toBe(false)
  })

  it('생년월일이 오늘보다 뒤면 오류다', () => {
    expect(errorsOf(form({ birthDate: '2026-11-06' }))).toEqual({ birth: 'birth-date-future' })
    expect(profileOf(form({ birthDate: '2026-11-05' })).birthDate).toBe('2026-11-05')
  })

  it('생년월일이 30년보다 앞이면 오류다', () => {
    expect(errorsOf(form({ birthDate: '1996-11-04' }))).toEqual({ birth: 'birth-date-too-old' })
    expect(profileOf(form({ birthDate: '1996-11-05' })).birthDate).toBe('1996-11-05')
  })

  it('없는 날짜는 오류다', () => {
    expect(errorsOf(form({ birthDate: '2025-02-29' }))).toEqual({ birth: 'birth-date-invalid' })
  })

  it('나이로 입력하면 세 0~30, 개월 0~11만 받는다', () => {
    expect(errorsOf(form({ birthMode: 'age', ageYears: '31', ageMonths: '0' }))).toEqual({ birth: 'age-invalid' })
    expect(errorsOf(form({ birthMode: 'age', ageYears: '1', ageMonths: '12' }))).toEqual({ birth: 'age-invalid' })
    expect(errorsOf(form({ birthMode: 'age', ageYears: '30', ageMonths: '1' }))).toEqual({ birth: 'age-too-old' })
    expect(profileOf(form({ birthMode: 'age', ageYears: '30', ageMonths: '0' })).birthDate).toBe('1996-11-05')
  })

  it('숫자가 아닌 나이는 오류다', () => {
    for (const ageYears of ['-1', '1.5', '열두', '1 2', '123']) {
      expect(errorsOf(form({ birthMode: 'age', ageYears, ageMonths: '' })), ageYears).toEqual({ birth: 'age-invalid' })
    }
  })

  it('나이로 입력하면 오늘에서 뺀 날을 추정 생년월일로 저장한다', () => {
    const profile = profileOf(form({ birthMode: 'age', ageYears: '12', ageMonths: '3' }))
    expect(profile.birthDate).toBe('2014-08-05')
    expect(profile.birthDateIsEstimated).toBe(true)
    expect(profileOf(form({ birthMode: 'age', ageYears: '', ageMonths: '3' })).birthDate).toBe('2026-08-05')
  })

  it('나이 칸을 모두 비우면 생년월일 없이 저장된다', () => {
    expect(profileOf(form({ birthMode: 'age', ageYears: '', ageMonths: '' })).birthDate).toBeNull()
  })

  it('추정 생년월일은 나이 모드로 열리고, 나이를 바꾸지 않으면 그 날짜가 그대로다', () => {
    const estimated: PetProfile = { ...bori, birthDate: '2014-07-20', birthDateIsEstimated: true }
    const values = profileFormValuesOf(estimated, today)
    expect(values).toMatchObject({ birthMode: 'age', ageYears: '12', ageMonths: '3' })
    expect(profileOf(values, estimated).birthDate).toBe('2014-07-20')
    expect(profileOf({ ...values, ageMonths: '4' }, estimated).birthDate).toBe('2014-07-05')
  })

  it('성별을 고르지 않아도 저장된다', () => {
    expect(profileOf(form({ sex: '' })).sex).toBeNull()
    expect(profileOf(form({ sex: 'female' })).sex).toBe('female')
  })

  it('프로필을 수정해도 의료정보와 사진은 그대로다', () => {
    const values = profileFormValuesOf(bori, today)
    const profile = profileOf({ ...values, name: '보리2' }, bori)
    expect(profile.medical).toEqual(bori.medical)
    expect(profile.photoDataUrl).toBe(bori.photoDataUrl)
    expect(profile.name).toBe('보리2')
  })

  it('처음 등록하면 의료정보는 비어 있다', () => {
    expect(profileOf(form({})).medical).toEqual({ conditions: [], allergies: [], medications: [] })
  })

  it('저장할 때 updatedAt을 받은 현재 시각으로 채운다', () => {
    expect(profileOf(form({}), bori).updatedAt).toBe('2026-11-05T00:30:00.000Z')
  })

  it('저장된 프로필을 폼으로 열면 같은 값이 보인다', () => {
    expect(profileFormValuesOf(bori, today)).toEqual({
      name: '보리',
      breed: '골든리트리버',
      sex: 'male',
      birthMode: 'date',
      birthDate: '2014-08-01',
      ageYears: '',
      ageMonths: '',
      photoDataUrl: 'data:image/jpeg;base64,AAAA',
    })
  })

  it('오류가 여러 칸에 있으면 모두 알려 준다', () => {
    expect(errorsOf(form({ name: '', breed: '가'.repeat(31), birthDate: '2027-01-01' }))).toEqual({
      name: 'name-required',
      breed: 'breed-too-long',
      birth: 'birth-date-future',
    })
  })
})
