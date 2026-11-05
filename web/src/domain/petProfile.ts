/**
 * 반려견 프로필. 형식의 기준은 docs/data/pet-profile.md다 (웹·차 앱·서버 공통).
 * 이 파일을 바꾸면 그 문서와 예시 JSON도 같이 바꾼다.
 */

export const PET_PROFILE_SCHEMA_VERSION = 1

/** 쓰는 쪽(입력 폼)이 지키는 제한. 글자 수는 코드 포인트 수다 (charCount). */
export const LIMITS = {
  nameChars: 20,
  breedChars: 30,
  medicalItemChars: 50,
  dosageChars: 50,
  medicalItemCount: 20,
  photoDataUrlChars: 400_000,
  maxAgeYears: 30,
} as const

export type Sex = 'male' | 'female'

export interface Medication {
  name: string
  /** 복용법 자유 입력 (예: "1일 1회"). 없으면 "" */
  dosage: string
}

/** 빈 배열은 "없음"이 아니라 "등록하지 않음"이다. */
export interface MedicalInfo {
  conditions: string[]
  allergies: string[]
  medications: Medication[]
}

export interface PetProfile {
  schemaVersion: typeof PET_PROFILE_SCHEMA_VERSION
  name: string
  breed: string | null
  sex: Sex | null
  /** 'YYYY-MM-DD' (시간대 없는 날짜) */
  birthDate: string | null
  /** 나이로 입력해서 생년월일이 추정값이면 true. birthDate가 null이면 false */
  birthDateIsEstimated: boolean
  /** 'data:image/jpeg;base64,...' */
  photoDataUrl: string | null
  medical: MedicalInfo
  /** ISO 8601 UTC. 마지막 저장 시각 */
  updatedAt: string
}

export const PHOTO_DATA_URL_PREFIX = 'data:image/jpeg;base64,'

export function emptyMedicalInfo(): MedicalInfo {
  return { conditions: [], allergies: [], medications: [] }
}

export function isMedicalInfoEmpty(medical: MedicalInfo): boolean {
  return medical.conditions.length === 0 && medical.allergies.length === 0 && medical.medications.length === 0
}

/** 글자 수 = 유니코드 코드 포인트 수 (Kotlin codePointCount와 같다). */
export function charCount(text: string): number {
  return [...text].length
}

/** 저장하기 전에 앞뒤 공백을 지우고 NFC로 정규화한다. */
export function normalizeText(text: string): string {
  return text.normalize('NFC').trim()
}
