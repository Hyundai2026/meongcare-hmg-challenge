import { describe, expect, it } from 'vitest'
import type { PetProfile } from '../domain/petProfile'
import { applyMedicalForm, medicalFormValuesOf, type MedicalFormValues } from './medicalFormValues'

const now = new Date('2026-11-05T01:00:00.000Z')
const bori: PetProfile = {
  schemaVersion: 1,
  name: '보리',
  breed: '골든리트리버',
  sex: 'male',
  birthDate: '2014-08-01',
  birthDateIsEstimated: false,
  photoDataUrl: 'data:image/jpeg;base64,AAAA',
  medical: { conditions: [], allergies: [], medications: [] },
  updatedAt: '2026-01-01T00:00:00.000Z',
}

const form = (patch: Partial<MedicalFormValues>): MedicalFormValues => ({ conditions: [], allergies: [], medications: [], ...patch })

function apply(values: MedicalFormValues) {
  return applyMedicalForm(values, bori, now)
}

function medicalOf(values: MedicalFormValues) {
  const result = apply(values)
  if (!result.ok) throw new Error(`검증 실패: ${JSON.stringify(result.errors)}`)
  return result.profile.medical
}

describe('의료정보 폼', () => {
  it('빈 줄은 저장하지 않는다', () => {
    expect(medicalOf(form({ conditions: ['', '심장질환', '   '], medications: [{ name: '', dosage: '' }] }))).toEqual({
      conditions: ['심장질환'],
      allergies: [],
      medications: [],
    })
  })

  it('항목의 앞뒤 공백을 지운다', () => {
    expect(medicalOf(form({ allergies: ['  닭고기 '], medications: [{ name: ' 심장약 ', dosage: ' 1일 1회 ' }] }))).toEqual({
      conditions: [],
      allergies: ['닭고기'],
      medications: [{ name: '심장약', dosage: '1일 1회' }],
    })
  })

  it('항목은 50자까지다', () => {
    expect(medicalOf(form({ conditions: ['가'.repeat(50)] })).conditions[0]).toHaveLength(50)
    const result = apply(form({ conditions: ['심장질환', '가'.repeat(51)] }))
    expect(result).toEqual({ ok: false, errors: { rows: { 'conditions.1': 'too-long' }, tooMany: [] } })
  })

  it('오류는 빈 줄을 버리기 전 화면의 줄 번호로 알려 준다', () => {
    const result = apply(form({ allergies: ['', '가'.repeat(51)] }))
    expect(result).toEqual({ ok: false, errors: { rows: { 'allergies.1': 'too-long' }, tooMany: [] } })
  })

  it('묶음마다 20개까지다', () => {
    const twenty = Array.from({ length: 20 }, (_, i) => `질환${i}`)
    expect(medicalOf(form({ conditions: twenty })).conditions).toHaveLength(20)
    const result = apply(form({ conditions: [...twenty, '하나 더'] }))
    expect(result).toEqual({ ok: false, errors: { rows: {}, tooMany: ['conditions'] } })
    const meds = Array.from({ length: 21 }, (_, i) => ({ name: `약${i}`, dosage: '' }))
    expect(apply(form({ medications: meds }))).toEqual({ ok: false, errors: { rows: {}, tooMany: ['medications'] } })
  })

  it('20개는 빈 줄을 버린 뒤에 센다', () => {
    const twenty = Array.from({ length: 20 }, (_, i) => `질환${i}`)
    expect(medicalOf(form({ conditions: [...twenty, '', ' '] })).conditions).toHaveLength(20)
  })

  it('복용법만 있고 약 이름이 없으면 오류다', () => {
    const result = apply(form({ medications: [{ name: '', dosage: '1일 1회' }] }))
    expect(result).toEqual({ ok: false, errors: { rows: { 'medications.0': 'medication-name-required' }, tooMany: [] } })
  })

  it('약 이름만 있고 복용법이 없어도 저장된다', () => {
    expect(medicalOf(form({ medications: [{ name: '심장약', dosage: '' }] })).medications).toEqual([{ name: '심장약', dosage: '' }])
  })

  it('약 이름과 복용법은 각각 50자까지다', () => {
    expect(apply(form({ medications: [{ name: '가'.repeat(51), dosage: '' }] }))).toMatchObject({
      ok: false,
      errors: { rows: { 'medications.0': 'too-long' } },
    })
    expect(apply(form({ medications: [{ name: '심장약', dosage: '가'.repeat(51) }] }))).toMatchObject({
      ok: false,
      errors: { rows: { 'medications.0': 'dosage-too-long' } },
    })
  })

  it('의료정보를 수정해도 프로필 기본정보와 사진은 그대로다', () => {
    const result = apply(form({ conditions: ['심장질환'] }))
    if (!result.ok) throw new Error('실패')
    const { medical, updatedAt, ...rest } = result.profile
    const { medical: _m, updatedAt: _u, ...before } = bori
    expect(rest).toEqual(before)
    expect(updatedAt).toBe('2026-11-05T01:00:00.000Z')
  })

  it('저장한 의료정보를 다시 폼으로 열면 같은 줄이 보인다', () => {
    const medical = {
      conditions: ['심장질환', '관절염'],
      allergies: ['특정 단백질 알레르기'],
      medications: [{ name: '심장약', dosage: '1일 1회' }],
    }
    const values = medicalFormValuesOf(medical)
    expect(values).toEqual(medical)
    expect(medicalOf(values)).toEqual(medical)
  })

  it('비어 있는 묶음은 빈 줄 하나로 연다', () => {
    expect(medicalFormValuesOf({ conditions: [], allergies: [], medications: [] })).toEqual({
      conditions: [''],
      allergies: [''],
      medications: [{ name: '', dosage: '' }],
    })
  })
})
