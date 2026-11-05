/** 프로필 값 → 화면 문구. 순수 함수. */
import { ageLabel } from '../domain/age'
import { parseLocalDate, type LocalDate } from '../domain/localDate'
import type { Medication, PetProfile, Sex } from '../domain/petProfile'

export const SEX_TEXT: Record<Sex, string> = { male: '수컷', female: '암컷' }
export const SEX_SYMBOL: Record<Sex, string> = { male: '♂', female: '♀' }

/** "골든리트리버 · 12세 3개월". 없는 값은 빼고, 둘 다 없으면 null */
export function profileSubtitle(profile: PetProfile, today: LocalDate): string | null {
  const parts = [profile.breed, ageLabel(profile.birthDate, today)].filter((p): p is string => p !== null && p.length > 0)
  return parts.length === 0 ? null : parts.join(' · ')
}

/** "2014년 8월 1일생" / 추정이면 "2014년 8월생 (나이로 입력)" */
export function birthDateText(profile: PetProfile): string | null {
  if (profile.birthDate === null) return null
  const date = parseLocalDate(profile.birthDate)
  if (date === null) return null
  return profile.birthDateIsEstimated ? `${date.year}년 ${date.month}월생 (나이로 입력)` : `${date.year}년 ${date.month}월 ${date.day}일생`
}

/** "심장약 (1일 1회)" / 복용법이 없으면 "심장약" */
export function medicationText(medication: Medication): string {
  return medication.dosage.length === 0 ? medication.name : `${medication.name} (${medication.dosage})`
}

/** 사진이 없을 때 보일 첫 글자 */
export function initialOf(name: string): string {
  return [...name][0] ?? ''
}
