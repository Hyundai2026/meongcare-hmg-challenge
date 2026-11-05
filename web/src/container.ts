/**
 * 저장소 구현을 고른다 (앱의 AppContainer 자리).
 * 지금은 브라우저 localStorage뿐이다. Firebase가 생기면 설정(.env.local)이 있을 때 그 구현을 고른다.
 */
import { createLocalStoragePetProfileRepository } from './data/localStoragePetProfileRepository'
import type { PetProfileRepository } from './domain/petProfileRepository'

/** 저장소를 쓸 수 없을 때(쿠키 차단 등). 항상 unavailable을 돌려준다. */
const unavailableRepository: PetProfileRepository = {
  load: async () => ({ ok: false, reason: 'unavailable' }),
  save: async () => ({ ok: false, reason: 'unavailable' }),
}

export function createPetProfileRepository(): PetProfileRepository {
  try {
    // 접근하는 것 자체가 던질 수 있다 (사이트 데이터 차단 등).
    const storage = window.localStorage
    if (storage == null) return unavailableRepository
    return createLocalStoragePetProfileRepository(storage)
  } catch {
    return unavailableRepository
  }
}
