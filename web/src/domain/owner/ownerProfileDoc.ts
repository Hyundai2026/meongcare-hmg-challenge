/**
 * 프로필 ↔ 서버 문서 (계획 014). 형식: docs/data/owner-link.md "문서".
 * - 프로필 문서(`petProfiles/{uid}`)는 pet-profile v1 필드 그대로지만 photoDataUrl은 늘 null이다.
 * - 사진은 보호자만 읽는 문서(`petProfiles/{uid}/private/photo`)에 따로 둔다. 연결된 차는 사진을 읽지 못한다.
 * firebase를 모르는 순수 함수다.
 */
import { PHOTO_DATA_URL_PREFIX, type PetProfile } from '../petProfile'
import { petProfileFromJson, type ParseResult } from '../petProfileJson'

export interface ServerProfileDocs {
  /** `petProfiles/{uid}`에 쓰는 값 (photoDataUrl: null) */
  profile: PetProfile
  /** `private/photo`의 photoDataUrl. 사진이 없으면 null (사진 문서를 지운다) */
  photoDataUrl: string | null
}

/** 저장할 프로필 → 서버 문서 두 개 */
export function serverDocsOf(profile: PetProfile): ServerProfileDocs {
  return {
    profile: {
      schemaVersion: profile.schemaVersion,
      name: profile.name,
      breed: profile.breed,
      sex: profile.sex,
      birthDate: profile.birthDate,
      birthDateIsEstimated: profile.birthDateIsEstimated,
      photoDataUrl: null,
      medical: {
        conditions: [...profile.medical.conditions],
        allergies: [...profile.medical.allergies],
        medications: profile.medical.medications.map((m) => ({ name: m.name, dosage: m.dosage })),
      },
      updatedAt: profile.updatedAt,
    },
    photoDataUrl: profile.photoDataUrl,
  }
}

/**
 * 서버에서 읽은 두 문서 → 프로필. pet-profile의 읽는 쪽 규칙 그대로다 (모르는 필드 무시, 버전이 1이 아니면 읽지 않음).
 * 사진 문서가 없거나 형식이 틀리면 사진 없이 읽는다 (사진 때문에 의료정보를 못 보이게 하지 않는다).
 * @param photoData 사진 문서의 값. 문서가 없으면 null
 */
export function profileFromServerDocs(profileData: unknown, photoData: unknown): ParseResult {
  if (typeof profileData !== 'object' || profileData === null || Array.isArray(profileData)) {
    return petProfileFromJson(profileData)
  }
  const parsed = petProfileFromJson({ ...profileData, photoDataUrl: null })
  if (!parsed.ok) return parsed
  const photo = photoDataUrlOf(photoData)
  return photo === null ? parsed : { ok: true, profile: { ...parsed.profile, photoDataUrl: photo } }
}

function photoDataUrlOf(photoData: unknown): string | null {
  if (typeof photoData !== 'object' || photoData === null || !('photoDataUrl' in photoData)) return null
  const value = photoData.photoDataUrl
  return typeof value === 'string' && value.startsWith(PHOTO_DATA_URL_PREFIX) ? value : null
}
