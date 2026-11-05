import type { PetProfile } from '../domain/petProfile'
import { parsePetProfile, serializePetProfile } from '../domain/petProfileJson'
import type { PetProfileRepository, SaveFailure } from '../domain/petProfileRepository'

/** 앱 이름과 묶지 않는다. 이름을 바꿔도 저장한 정보가 사라지지 않게. */
export const PET_PROFILE_STORAGE_KEY = 'owner-web.pet-profile'

/** 테스트에서 가짜를 넘길 수 있게 필요한 것만 받는다. */
export type KeyValueStorage = Pick<Storage, 'getItem' | 'setItem'>

const QUOTA_ERROR_NAMES = new Set(['QuotaExceededError', 'NS_ERROR_DOM_QUOTA_REACHED'])

export function createLocalStoragePetProfileRepository(storage: KeyValueStorage): PetProfileRepository {
  return {
    async load() {
      let raw: string | null
      try {
        raw = storage.getItem(PET_PROFILE_STORAGE_KEY)
      } catch {
        return { ok: false, reason: 'unavailable' }
      }
      if (raw === null) return { ok: true, profile: null }
      const parsed = parsePetProfile(raw)
      return parsed.ok ? { ok: true, profile: parsed.profile } : { ok: false, reason: parsed.reason }
    },

    async save(profile: PetProfile) {
      const raw = serializePetProfile(profile)
      try {
        storage.setItem(PET_PROFILE_STORAGE_KEY, raw)
        return { ok: true }
      } catch (error) {
        return { ok: false, reason: saveFailureOf(error) }
      }
    },
  }
}

function saveFailureOf(error: unknown): SaveFailure {
  const name = typeof error === 'object' && error !== null && 'name' in error ? String(error.name) : ''
  if (QUOTA_ERROR_NAMES.has(name)) return 'quota'
  if (error instanceof DOMException) return 'unavailable'
  return 'unknown'
}
