/**
 * 의료정보 폼 경계 테스트. 기준: docs/data/pet-profile.md "필드" (항목 1~50자, 20개까지, dosage 0~50자,
 * 글자 수 = 코드 포인트, NFC, 빈 배열 = 등록하지 않음), 계획 008 설계 결정 6 (빈 줄은 버린다, 앞뒤 공백은 지운다).
 */
import { describe, expect, it } from 'vitest'
import type { PetProfile } from '../domain/petProfile'
import { parsePetProfile, serializePetProfile } from '../domain/petProfileJson'
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
  medical: {
    conditions: ['심장질환'],
    allergies: ['특정 단백질 알레르기'],
    medications: [{ name: '심장약', dosage: '1일 1회' }],
  },
  updatedAt: '2026-01-01T00:00:00.000Z',
}

const form = (patch: Partial<MedicalFormValues>): MedicalFormValues => ({ conditions: [], allergies: [], medications: [], ...patch })

function apply(values: MedicalFormValues, base: PetProfile = bori) {
  return applyMedicalForm(values, base, now)
}

function medicalOf(values: MedicalFormValues) {
  const result = apply(values)
  if (!result.ok) throw new Error(`검증 실패: ${JSON.stringify(result.errors)}`)
  return result.profile.medical
}

const many = (count: number, prefix: string) => Array.from({ length: count }, (_, i) => `${prefix}${i}`)

describe('의료정보 폼 글자 수 경계', () => {
  it('항목 50자 경계는 이모지도 한 글자로 센다', () => {
    expect(medicalOf(form({ allergies: ['🐟'.repeat(50)] })).allergies).toEqual(['🐟'.repeat(50)])
    expect(apply(form({ allergies: ['🐟'.repeat(51)] }))).toEqual({ ok: false, errors: { rows: { 'allergies.0': 'too-long' }, tooMany: [] } })
  })

  it('풀어 쓴 한글 항목 50자는 받고 NFC로 합쳐 저장한다', () => {
    const text = '가'.repeat(50)
    expect(medicalOf(form({ conditions: [text.normalize('NFD')] })).conditions).toEqual([text])
    expect(apply(form({ conditions: [(text + '가').normalize('NFD')] })).ok).toBe(false)
  })

  it('앞뒤 공백·탭·전각 공백은 50자에 들어가지 않는다', () => {
    expect(medicalOf(form({ conditions: ['\t ' + '가'.repeat(50) + '　'] })).conditions).toEqual(['가'.repeat(50)])
  })

  it('약 이름 50자와 복용법 50자 경계는 이모지도 한 글자로 센다', () => {
    expect(medicalOf(form({ medications: [{ name: '💊'.repeat(50), dosage: '💊'.repeat(50) }] })).medications).toEqual([
      { name: '💊'.repeat(50), dosage: '💊'.repeat(50) },
    ])
    expect(apply(form({ medications: [{ name: '💊'.repeat(51), dosage: '' }] }))).toEqual({
      ok: false,
      errors: { rows: { 'medications.0': 'too-long' }, tooMany: [] },
    })
    expect(apply(form({ medications: [{ name: '심장약', dosage: '💊'.repeat(51) }] }))).toEqual({
      ok: false,
      errors: { rows: { 'medications.0': 'dosage-too-long' }, tooMany: [] },
    })
  })
})

describe('의료정보 폼 빈 줄·개수 경계', () => {
  it('탭·줄바꿈·전각 공백뿐인 줄은 오류가 아니라 빈 줄로 버린다', () => {
    expect(
      medicalOf(
        form({
          conditions: ['\t', '\n', '　'],
          allergies: [' 　 '],
          medications: [{ name: ' \t', dosage: '　' }],
        }),
      ),
    ).toEqual({ conditions: [], allergies: [], medications: [] })
  })

  it('빈 줄을 버려도 남은 줄은 입력 순서 그대로다', () => {
    expect(medicalOf(form({ conditions: ['', '심장질환', ' ', '관절염', ''] })).conditions).toEqual(['심장질환', '관절염'])
  })

  it('알레르기와 약도 20개는 받고 21개는 받지 않는다', () => {
    expect(medicalOf(form({ allergies: many(20, '알레르기') })).allergies).toHaveLength(20)
    expect(apply(form({ allergies: many(21, '알레르기') }))).toEqual({ ok: false, errors: { rows: {}, tooMany: ['allergies'] } })
    const meds = (count: number) => many(count, '약').map((name) => ({ name, dosage: '' }))
    expect(medicalOf(form({ medications: meds(20) })).medications).toHaveLength(20)
  })

  it('세 묶음이 모두 20개면 모두 받는다', () => {
    const medical = medicalOf(
      form({
        conditions: many(20, '질환'),
        allergies: many(20, '알레르기'),
        medications: many(20, '약').map((name) => ({ name, dosage: '1일 1회' })),
      }),
    )
    expect([medical.conditions.length, medical.allergies.length, medical.medications.length]).toEqual([20, 20, 20])
  })

  it('모두 비우면 세 묶음 모두 빈 배열로 저장한다 (등록하지 않음)', () => {
    const result = apply(medicalFormValuesOf({ conditions: [], allergies: [], medications: [] }))
    expect(result).toEqual({
      ok: true,
      profile: { ...bori, medical: { conditions: [], allergies: [], medications: [] }, updatedAt: '2026-11-05T01:00:00.000Z' },
    })
  })
})

describe('의료정보 폼 약 줄 경계', () => {
  it('약 이름이 공백뿐이고 복용법만 있으면 약 이름 오류다', () => {
    expect(apply(form({ medications: [{ name: ' \t　', dosage: '1일 1회' }] }))).toEqual({
      ok: false,
      errors: { rows: { 'medications.0': 'medication-name-required' }, tooMany: [] },
    })
  })

  it('약 이름만 있고 복용법이 공백뿐이면 복용법은 null이 아니라 빈 문자열이다', () => {
    expect(medicalOf(form({ medications: [{ name: '심장약', dosage: '  \t' }] })).medications).toEqual([{ name: '심장약', dosage: '' }])
  })

  it('약 오류의 줄 번호는 빈 줄을 버리기 전 번호다', () => {
    expect(
      apply(
        form({
          medications: [
            { name: '', dosage: '' },
            { name: '', dosage: '1일 1회' },
          ],
        }),
      ),
    ).toEqual({ ok: false, errors: { rows: { 'medications.1': 'medication-name-required' }, tooMany: [] } })
  })
})

describe('의료정보 폼 저장 결과', () => {
  it('여러 묶음의 오류를 한 번에 알려 준다', () => {
    const result = apply(
      form({
        conditions: ['가'.repeat(51)],
        allergies: many(21, '알레르기'),
        medications: [
          { name: '심장약', dosage: '' },
          { name: '', dosage: '1일 2회' },
        ],
      }),
    )
    expect(result).toEqual({
      ok: false,
      errors: {
        rows: { 'conditions.0': 'too-long', 'medications.1': 'medication-name-required' },
        tooMany: ['allergies'],
      },
    })
  })

  it('저장한 프로필은 형식 문서대로 다시 읽을 수 있다', () => {
    const result = apply(
      form({
        conditions: ['심장질환 "중증"'],
        allergies: ['닭고기\\소고기'],
        medications: [{ name: '심장약🐶', dosage: '' }],
      }),
    )
    if (!result.ok) throw new Error('검증 실패')
    expect(parsePetProfile(serializePetProfile(result.profile))).toEqual({ ok: true, profile: result.profile })
  })

  it('저장해도 넘긴 폼 값과 원래 프로필은 바뀌지 않는다', () => {
    const base = structuredClone(bori)
    const values = { ...medicalFormValuesOf(base.medical), conditions: ['  관절염 ', ''] }
    const valuesBefore = structuredClone(values)
    expect(apply(values, base).ok).toBe(true)
    expect(base).toEqual(bori)
    expect(values).toEqual(valuesBefore)
  })
})
