import type { PetProfile } from './petProfile'

export type LoadFailure = 'unavailable' | 'corrupted' | 'unsupported-version'
export type SaveFailure = 'quota' | 'unavailable' | 'unknown'

export type LoadResult = { ok: true; profile: PetProfile | null } | { ok: false; reason: LoadFailure }
export type SaveResult = { ok: true } | { ok: false; reason: SaveFailure }

/**
 * 프로필 저장소. 화면은 이 인터페이스만 안다.
 * - 지금: 브라우저 저장소 구현 (data/, 앱의 Fake 자리)
 * - 나중: Firebase(Firestore) 구현 (앱의 Real 자리). 그때 'network' 같은 이유를 더한다.
 * 브라우저 저장소도 Promise로 감싸서, 바꿔 끼워도 화면 코드가 바뀌지 않게 한다.
 * 실패는 예외가 아니라 결과 값으로 돌려준다.
 */
export interface PetProfileRepository {
  /** 저장된 프로필. 없으면 profile: null */
  load(): Promise<LoadResult>
  /** 받은 값을 그대로 저장한다 (updatedAt은 부르는 쪽이 채운다). 실패하면 이전 값은 그대로 남는다 */
  save(profile: PetProfile): Promise<SaveResult>
}
