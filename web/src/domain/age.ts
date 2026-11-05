/**
 * 나이 계산. 규칙은 docs/data/pet-profile.md "나이 계산"이다 (차 앱도 같은 규칙).
 * 오늘은 항상 인자로 받는다 (테스트에서 고정).
 */
import { LIMITS } from './petProfile'
import { compareLocalDate, minusMonths, parseLocalDate, type LocalDate } from './localDate'

/** 생년월일부터 오늘까지 개월 수. 생년월일이 오늘보다 뒤면 null. */
export function ageInMonths(birth: LocalDate, today: LocalDate): number | null {
  if (compareLocalDate(birth, today) > 0) return null
  let months = (today.year - birth.year) * 12 + (today.month - birth.month)
  if (today.day < birth.day) months -= 1
  return months
}

/** 12세 3개월 / 12세 / 3개월 / 1개월 미만 */
export function formatAge(months: number): string {
  if (months < 1) return '1개월 미만'
  const years = Math.floor(months / 12)
  const rest = months % 12
  if (years === 0) return `${rest}개월`
  if (rest === 0) return `${years}세`
  return `${years}세 ${rest}개월`
}

/** 프로필의 생년월일 → 나이 표시. 생년월일이 없거나 읽을 수 없거나 오늘보다 뒤면 null. */
export function ageLabel(birthDate: string | null, today: LocalDate): string | null {
  if (birthDate === null) return null
  const birth = parseLocalDate(birthDate)
  if (birth === null) return null
  const months = ageInMonths(birth, today)
  return months === null ? null : formatAge(months)
}

/** 나이로 입력하면 생년월일 = 오늘에서 그만큼 뺀 날. */
export function birthDateFromAge(years: number, months: number, today: LocalDate): LocalDate {
  return minusMonths(today, years * 12 + months)
}

/** 받을 수 있는 가장 이른 생년월일 (오늘에서 30년 전). */
export function earliestBirthDate(today: LocalDate): LocalDate {
  return minusMonths(today, LIMITS.maxAgeYears * 12)
}
