/**
 * 의료정보 폼 값 ↔ 프로필, 검증. 순수 함수 (React를 모른다).
 * 규칙: docs/data/pet-profile.md "필드" (항목 1~50자, 묶음마다 20개까지, 복용법 0~50자)
 * - 빈 줄은 오류가 아니라 저장할 때 버린다.
 * - 오류는 화면의 줄 번호(index)로 알려 준다. 빈 줄을 버리기 전 번호다.
 */
import { charCount, LIMITS, normalizeText, type MedicalInfo, type PetProfile } from '../domain/petProfile'

export interface MedicationRow {
  name: string
  dosage: string
}

export interface MedicalFormValues {
  conditions: string[]
  allergies: string[]
  medications: MedicationRow[]
}

export type MedicalGroup = keyof MedicalFormValues

export type MedicalRowError = 'too-long' | 'dosage-too-long' | 'medication-name-required'

export interface MedicalFormErrors {
  /** `${묶음}.${줄 번호}` → 오류 */
  rows: Partial<Record<string, MedicalRowError>>
  /** 묶음에 20개를 넘게 넣음 */
  tooMany: MedicalGroup[]
}

export type MedicalFormResult = { ok: true; profile: PetProfile } | { ok: false; errors: MedicalFormErrors }

export const MEDICAL_GROUPS: MedicalGroup[] = ['conditions', 'allergies', 'medications']

export function rowKey(group: MedicalGroup, index: number): string {
  return `${group}.${index}`
}

/** 편집을 열 때의 폼 값. 비어 있는 묶음은 빈 줄 하나로 연다 (바로 입력할 수 있게). */
export function medicalFormValuesOf(medical: MedicalInfo): MedicalFormValues {
  return {
    conditions: medical.conditions.length === 0 ? [''] : [...medical.conditions],
    allergies: medical.allergies.length === 0 ? [''] : [...medical.allergies],
    medications: medical.medications.length === 0 ? [{ name: '', dosage: '' }] : medical.medications.map((m) => ({ ...m })),
  }
}

/** 폼 값을 검증하고 저장할 프로필을 만든다. 프로필의 다른 값(이름, 사진 등)은 그대로 둔다. */
export function applyMedicalForm(values: MedicalFormValues, base: PetProfile, now: Date): MedicalFormResult {
  const errors: MedicalFormErrors = { rows: {}, tooMany: [] }

  const textGroup = (group: 'conditions' | 'allergies'): string[] => {
    const kept: string[] = []
    values[group].forEach((raw, index) => {
      const text = normalizeText(raw)
      if (text.length === 0) return
      if (charCount(text) > LIMITS.medicalItemChars) errors.rows[rowKey(group, index)] = 'too-long'
      kept.push(text)
    })
    if (kept.length > LIMITS.medicalItemCount) errors.tooMany.push(group)
    return kept
  }

  const conditions = textGroup('conditions')
  const allergies = textGroup('allergies')

  const medications: MedicationRow[] = []
  values.medications.forEach((row, index) => {
    const name = normalizeText(row.name)
    const dosage = normalizeText(row.dosage)
    if (name.length === 0 && dosage.length === 0) return
    const key = rowKey('medications', index)
    if (name.length === 0) errors.rows[key] = 'medication-name-required'
    else if (charCount(name) > LIMITS.medicalItemChars) errors.rows[key] = 'too-long'
    else if (charCount(dosage) > LIMITS.dosageChars) errors.rows[key] = 'dosage-too-long'
    medications.push({ name, dosage })
  })
  if (medications.length > LIMITS.medicalItemCount) errors.tooMany.push('medications')

  if (Object.keys(errors.rows).length > 0 || errors.tooMany.length > 0) return { ok: false, errors }
  return {
    ok: true,
    profile: { ...base, medical: { conditions, allergies, medications }, updatedAt: now.toISOString() },
  }
}
