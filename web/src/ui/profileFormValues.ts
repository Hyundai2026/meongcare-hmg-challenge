/**
 * 프로필 폼 값 ↔ 프로필, 검증. 순수 함수 (React를 모른다).
 * 규칙: docs/data/pet-profile.md "필드", "나이로 입력할 때"
 */
import { ageInMonths, birthDateFromAge, earliestBirthDate } from '../domain/age'
import { compareLocalDate, formatLocalDate, parseLocalDate, type LocalDate } from '../domain/localDate'
import {
  charCount,
  emptyMedicalInfo,
  LIMITS,
  normalizeText,
  PET_PROFILE_SCHEMA_VERSION,
  type PetProfile,
  type Sex,
} from '../domain/petProfile'

export type BirthMode = 'date' | 'age'

export interface ProfileFormValues {
  name: string
  breed: string
  sex: Sex | ''
  birthMode: BirthMode
  /** 'YYYY-MM-DD' 또는 '' */
  birthDate: string
  ageYears: string
  ageMonths: string
  photoDataUrl: string | null
}

export type ProfileField = 'name' | 'breed' | 'birth'

export type ProfileFieldError =
  | 'name-required'
  | 'name-too-long'
  | 'breed-too-long'
  | 'birth-date-invalid'
  | 'birth-date-future'
  | 'birth-date-too-old'
  | 'age-invalid'
  | 'age-too-old'

export type ProfileFormErrors = Partial<Record<ProfileField, ProfileFieldError>>

export type ProfileFormResult = { ok: true; profile: PetProfile } | { ok: false; errors: ProfileFormErrors }

/** 오류를 보여 줄 칸 순서 (첫 오류 칸으로 포커스한다). */
export const PROFILE_FIELD_ORDER: ProfileField[] = ['name', 'breed', 'birth']

const AGE_NUMBER = /^\d{1,2}$/

/** 편집을 열 때의 폼 값. 생년월일이 추정값이면 나이 모드로 연다. */
export function profileFormValuesOf(profile: PetProfile | null, today: LocalDate): ProfileFormValues {
  if (profile === null) {
    return { name: '', breed: '', sex: '', birthMode: 'date', birthDate: '', ageYears: '', ageMonths: '', photoDataUrl: null }
  }
  const values: ProfileFormValues = {
    name: profile.name,
    breed: profile.breed ?? '',
    sex: profile.sex ?? '',
    birthMode: 'date',
    birthDate: profile.birthDate ?? '',
    ageYears: '',
    ageMonths: '',
    photoDataUrl: profile.photoDataUrl,
  }
  const age = estimatedAgeOf(profile, today)
  if (age !== null) {
    values.birthMode = 'age'
    values.ageYears = String(age.years)
    values.ageMonths = String(age.months)
  }
  return values
}

/** 폼 값을 검증하고 저장할 프로필을 만든다. 의료정보는 base의 것을 그대로 둔다. */
export function applyProfileForm(
  values: ProfileFormValues,
  base: PetProfile | null,
  today: LocalDate,
  now: Date,
): ProfileFormResult {
  const errors: ProfileFormErrors = {}

  const name = normalizeText(values.name)
  if (name.length === 0) errors.name = 'name-required'
  else if (charCount(name) > LIMITS.nameChars) errors.name = 'name-too-long'

  const breed = normalizeText(values.breed)
  if (charCount(breed) > LIMITS.breedChars) errors.breed = 'breed-too-long'

  const birth = values.birthMode === 'date' ? birthFromDate(values.birthDate, today) : birthFromAge(values, base, today)
  if ('error' in birth) errors.birth = birth.error

  if (Object.keys(errors).length > 0 || 'error' in birth) return { ok: false, errors }

  return {
    ok: true,
    profile: {
      schemaVersion: PET_PROFILE_SCHEMA_VERSION,
      name,
      breed: breed.length === 0 ? null : breed,
      sex: values.sex === '' ? null : values.sex,
      birthDate: birth.birthDate,
      birthDateIsEstimated: birth.birthDate !== null && birth.estimated,
      photoDataUrl: values.photoDataUrl,
      medical: base?.medical ?? emptyMedicalInfo(),
      updatedAt: now.toISOString(),
    },
  }
}

type Birth = { birthDate: string | null; estimated: boolean } | { error: ProfileFieldError }

function birthFromDate(text: string, today: LocalDate): Birth {
  const trimmed = text.trim()
  if (trimmed.length === 0) return { birthDate: null, estimated: false }
  const date = parseLocalDate(trimmed)
  if (date === null) return { error: 'birth-date-invalid' }
  if (compareLocalDate(date, today) > 0) return { error: 'birth-date-future' }
  if (compareLocalDate(date, earliestBirthDate(today)) < 0) return { error: 'birth-date-too-old' }
  return { birthDate: trimmed, estimated: false }
}

function birthFromAge(values: ProfileFormValues, base: PetProfile | null, today: LocalDate): Birth {
  const yearsText = values.ageYears.trim()
  const monthsText = values.ageMonths.trim()
  if (yearsText.length === 0 && monthsText.length === 0) return { birthDate: null, estimated: false }
  if ((yearsText.length > 0 && !AGE_NUMBER.test(yearsText)) || (monthsText.length > 0 && !AGE_NUMBER.test(monthsText))) {
    return { error: 'age-invalid' }
  }
  const years = yearsText.length === 0 ? 0 : Number(yearsText)
  const months = monthsText.length === 0 ? 0 : Number(monthsText)
  if (years > LIMITS.maxAgeYears || months > 11) return { error: 'age-invalid' }
  if (years * 12 + months > LIMITS.maxAgeYears * 12) return { error: 'age-too-old' }

  // 나이를 바꾸지 않았으면 예전에 추정한 생년월일을 그대로 둔다 (저장할 때마다 날짜가 밀리지 않게).
  const before = base === null ? null : estimatedAgeOf(base, today)
  if (base !== null && before !== null && before.years === years && before.months === months) {
    return { birthDate: base.birthDate, estimated: true }
  }
  return { birthDate: formatLocalDate(birthDateFromAge(years, months, today)), estimated: true }
}

function estimatedAgeOf(profile: PetProfile, today: LocalDate): { years: number; months: number } | null {
  if (!profile.birthDateIsEstimated || profile.birthDate === null) return null
  const birth = parseLocalDate(profile.birthDate)
  const months = birth === null ? null : ageInMonths(birth, today)
  return months === null ? null : { years: Math.floor(months / 12), months: months % 12 }
}
