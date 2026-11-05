/**
 * 프로필 ↔ JSON. 규칙은 docs/data/pet-profile.md "읽는 쪽 규칙"을 그대로 옮긴 것이다.
 * 읽는 쪽은 짐작하지 않는다: 형식이 틀리면 손상, 버전이 다르면 읽지 않는다.
 */
import { parseLocalDate } from './localDate'
import {
  PET_PROFILE_SCHEMA_VERSION,
  PHOTO_DATA_URL_PREFIX,
  type MedicalInfo,
  type Medication,
  type PetProfile,
  type Sex,
} from './petProfile'

export type ParseFailure = 'corrupted' | 'unsupported-version'
export type ParseResult = { ok: true; profile: PetProfile } | { ok: false; reason: ParseFailure }

/** 알려진 필드만 정해진 순서로 쓴다. */
export function serializePetProfile(profile: PetProfile): string {
  const value: PetProfile = {
    schemaVersion: profile.schemaVersion,
    name: profile.name,
    breed: profile.breed,
    sex: profile.sex,
    birthDate: profile.birthDate,
    birthDateIsEstimated: profile.birthDateIsEstimated,
    photoDataUrl: profile.photoDataUrl,
    medical: {
      conditions: [...profile.medical.conditions],
      allergies: [...profile.medical.allergies],
      medications: profile.medical.medications.map((m) => ({ name: m.name, dosage: m.dosage })),
    },
    updatedAt: profile.updatedAt,
  }
  return JSON.stringify(value)
}

export function parsePetProfile(raw: string): ParseResult {
  let value: unknown
  try {
    value = JSON.parse(raw)
  } catch {
    return CORRUPTED
  }
  return petProfileFromJson(value)
}

/** 이미 JSON.parse한 값에서 읽는다. */
export function petProfileFromJson(value: unknown): ParseResult {
  if (!isObject(value)) return CORRUPTED
  if (value.schemaVersion !== PET_PROFILE_SCHEMA_VERSION) return { ok: false, reason: 'unsupported-version' }

  const { name, breed, sex, birthDate, birthDateIsEstimated, photoDataUrl, medical, updatedAt } = value
  if (typeof name !== 'string' || name.length === 0) return CORRUPTED
  if (!isStringOrNull(breed)) return CORRUPTED
  if (!(sex === null || isSex(sex))) return CORRUPTED
  if (!(birthDate === null || (typeof birthDate === 'string' && parseLocalDate(birthDate) !== null))) return CORRUPTED
  if (typeof birthDateIsEstimated !== 'boolean') return CORRUPTED
  if (!(photoDataUrl === null || (typeof photoDataUrl === 'string' && photoDataUrl.startsWith(PHOTO_DATA_URL_PREFIX)))) {
    return CORRUPTED
  }
  if (typeof updatedAt !== 'string' || Number.isNaN(Date.parse(updatedAt))) return CORRUPTED
  const medicalInfo = medicalFromJson(medical)
  if (medicalInfo === null) return CORRUPTED

  return {
    ok: true,
    profile: {
      schemaVersion: PET_PROFILE_SCHEMA_VERSION,
      name,
      breed,
      sex,
      birthDate,
      birthDateIsEstimated,
      photoDataUrl,
      medical: medicalInfo,
      updatedAt,
    },
  }
}

const CORRUPTED: ParseResult = { ok: false, reason: 'corrupted' }

function medicalFromJson(value: unknown): MedicalInfo | null {
  if (!isObject(value)) return null
  const { conditions, allergies, medications } = value
  if (!isStringArray(conditions) || !isStringArray(allergies)) return null
  if (!Array.isArray(medications)) return null
  const meds: Medication[] = []
  for (const item of medications) {
    if (!isObject(item) || typeof item.name !== 'string' || typeof item.dosage !== 'string') return null
    meds.push({ name: item.name, dosage: item.dosage })
  }
  return { conditions: [...conditions], allergies: [...allergies], medications: meds }
}

function isObject(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}

function isStringOrNull(value: unknown): value is string | null {
  return value === null || typeof value === 'string'
}

function isStringArray(value: unknown): value is string[] {
  return Array.isArray(value) && value.every((item) => typeof item === 'string')
}

function isSex(value: unknown): value is Sex {
  return value === 'male' || value === 'female'
}
