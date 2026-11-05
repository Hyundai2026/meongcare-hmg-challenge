import { describe, expect, it } from 'vitest'
import type { PetProfile } from '../domain/petProfile'
import {
  createLocalStoragePetProfileRepository,
  PET_PROFILE_STORAGE_KEY,
  type KeyValueStorage,
} from './localStoragePetProfileRepository'

const bori: PetProfile = {
  schemaVersion: 1,
  name: '보리',
  breed: '골든리트리버',
  sex: 'male',
  birthDate: '2014-08-01',
  birthDateIsEstimated: false,
  photoDataUrl: null,
  medical: { conditions: ['심장질환'], allergies: [], medications: [{ name: '심장약', dosage: '1일 1회' }] },
  updatedAt: '2026-11-05T00:00:00.000Z',
}

/** 메모리 Storage. failSet/failGet에 던질 에러를 넣으면 그 동작이 실패한다 (값은 바뀌지 않는다). */
class FakeStorage implements KeyValueStorage {
  values = new Map<string, string>()
  failSet: unknown = null
  failGet: unknown = null

  getItem(key: string): string | null {
    if (this.failGet !== null) throw this.failGet
    return this.values.get(key) ?? null
  }

  setItem(key: string, value: string): void {
    if (this.failSet !== null) throw this.failSet
    this.values.set(key, value)
  }
}

describe('localStorage 프로필 저장소', () => {
  it('저장된 것이 없으면 프로필 없음을 돌려준다', async () => {
    const repo = createLocalStoragePetProfileRepository(new FakeStorage())
    expect(await repo.load()).toEqual({ ok: true, profile: null })
  })

  it('저장한 뒤 읽으면 같은 프로필이다', async () => {
    const repo = createLocalStoragePetProfileRepository(new FakeStorage())
    expect(await repo.save(bori)).toEqual({ ok: true })
    expect(await repo.load()).toEqual({ ok: true, profile: bori })
  })

  it('저장 공간이 가득 차면 quota로 실패하고 이전 값은 그대로다', async () => {
    const storage = new FakeStorage()
    const repo = createLocalStoragePetProfileRepository(storage)
    await repo.save(bori)
    storage.failSet = new DOMException('full', 'QuotaExceededError')
    expect(await repo.save({ ...bori, name: '보리2' })).toEqual({ ok: false, reason: 'quota' })
    expect(await repo.load()).toEqual({ ok: true, profile: bori })
  })

  it('Firefox의 저장 공간 초과 이름도 quota로 본다', async () => {
    const storage = new FakeStorage()
    storage.failSet = new DOMException('full', 'NS_ERROR_DOM_QUOTA_REACHED')
    const repo = createLocalStoragePetProfileRepository(storage)
    expect(await repo.save(bori)).toEqual({ ok: false, reason: 'quota' })
  })

  it('저장소를 쓸 수 없으면 unavailable로 실패한다', async () => {
    const storage = new FakeStorage()
    storage.failSet = new DOMException('blocked', 'SecurityError')
    const repo = createLocalStoragePetProfileRepository(storage)
    expect(await repo.save(bori)).toEqual({ ok: false, reason: 'unavailable' })
  })

  it('알 수 없는 에러로 저장하지 못하면 unknown이다', async () => {
    const storage = new FakeStorage()
    storage.failSet = new Error('?')
    const repo = createLocalStoragePetProfileRepository(storage)
    expect(await repo.save(bori)).toEqual({ ok: false, reason: 'unknown' })
  })

  it('읽을 때 저장소가 던지면 unavailable이다', async () => {
    const storage = new FakeStorage()
    storage.failGet = new DOMException('blocked', 'SecurityError')
    const repo = createLocalStoragePetProfileRepository(storage)
    expect(await repo.load()).toEqual({ ok: false, reason: 'unavailable' })
  })

  it('깨진 값이 저장돼 있으면 corrupted다', async () => {
    const storage = new FakeStorage()
    storage.values.set(PET_PROFILE_STORAGE_KEY, '{broken')
    const repo = createLocalStoragePetProfileRepository(storage)
    expect(await repo.load()).toEqual({ ok: false, reason: 'corrupted' })
  })

  it('다른 버전이 저장돼 있으면 unsupported-version이다', async () => {
    const storage = new FakeStorage()
    storage.values.set(PET_PROFILE_STORAGE_KEY, JSON.stringify({ ...bori, schemaVersion: 2 }))
    const repo = createLocalStoragePetProfileRepository(storage)
    expect(await repo.load()).toEqual({ ok: false, reason: 'unsupported-version' })
  })

  it('앱 이름과 상관없이 정해진 키 하나에 저장한다', async () => {
    const storage = new FakeStorage()
    const repo = createLocalStoragePetProfileRepository(storage)
    await repo.save(bori)
    expect([...storage.values.keys()]).toEqual(['owner-web.pet-profile'])
  })
})
