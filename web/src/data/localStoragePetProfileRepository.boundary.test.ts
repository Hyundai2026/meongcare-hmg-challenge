/**
 * 저장소 경계 테스트. 기준: web/src/domain/petProfileRepository.ts의 계약
 * ("받은 값을 그대로 저장한다", "실패하면 이전 값은 그대로", "실패는 예외가 아니라 결과 값"),
 * 계획 008 설계 결정 5, docs/data/pet-profile.md "읽는 쪽 규칙", "저장 위치".
 */
import { describe, expect, it } from 'vitest'
import type { PetProfile } from '../domain/petProfile'
import { parsePetProfile } from '../domain/petProfileJson'
import type { SaveFailure } from '../domain/petProfileRepository'
import { createLocalStoragePetProfileRepository, PET_PROFILE_STORAGE_KEY, type KeyValueStorage } from './localStoragePetProfileRepository'

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

/** 메모리 Storage. failSet/failGet에 넣은 값을 던진다 (던질 때 값은 바뀌지 않는다). */
class FakeStorage implements KeyValueStorage {
  values = new Map<string, string>()
  failSet: { error: unknown } | null = null
  failGet: { error: unknown } | null = null
  getCalls = 0

  getItem(key: string): string | null {
    this.getCalls += 1
    if (this.failGet !== null) throw this.failGet.error
    return this.values.get(key) ?? null
  }

  setItem(key: string, value: string): void {
    if (this.failSet !== null) throw this.failSet.error
    this.values.set(key, value)
  }
}

function setup() {
  const storage = new FakeStorage()
  return { storage, repo: createLocalStoragePetProfileRepository(storage) }
}

describe('localStorage 저장소 읽기 경계', () => {
  it('저장된 값이 빈 문자열이면 프로필 없음이 아니라 손상이다', async () => {
    const { storage, repo } = setup()
    storage.values.set(PET_PROFILE_STORAGE_KEY, '')
    expect(await repo.load()).toEqual({ ok: false, reason: 'corrupted' })
  })

  it('저장된 값이 JSON null이면 프로필 없음이 아니라 손상이다', async () => {
    const { storage, repo } = setup()
    storage.values.set(PET_PROFILE_STORAGE_KEY, 'null')
    expect(await repo.load()).toEqual({ ok: false, reason: 'corrupted' })
  })

  it('읽을 때 DOMException이 아닌 에러가 나도 거부하지 않고 unavailable을 돌려준다', async () => {
    for (const error of [new TypeError('x'), 'string', null, undefined]) {
      const { storage, repo } = setup()
      storage.failGet = { error }
      await expect(repo.load(), String(error)).resolves.toEqual({ ok: false, reason: 'unavailable' })
    }
  })

  it('다른 키에 프로필이 있어도 정해진 키만 읽는다', async () => {
    const { storage, repo } = setup()
    storage.values.set('pawcare.pet-profile', JSON.stringify(bori))
    storage.values.set('meongcare.pet-profile', JSON.stringify(bori))
    expect(await repo.load()).toEqual({ ok: true, profile: null })
  })

  it('읽어 온 프로필을 고쳐도 저장된 값은 그대로다', async () => {
    const { repo } = setup()
    await repo.save(bori)
    const first = await repo.load()
    if (!first.ok || first.profile === null) throw new Error('읽기 실패')
    first.profile.name = '바뀜'
    first.profile.medical.conditions.push('바뀜')
    expect(await repo.load()).toEqual({ ok: true, profile: bori })
  })
})

describe('localStorage 저장소 저장 경계', () => {
  it('저장 실패 이유: 이름이 QuotaExceededError면 quota, 다른 DOMException은 unavailable, 나머지는 unknown', async () => {
    const cases: Array<[string, unknown, SaveFailure]> = [
      ['QuotaExceededError DOMException', new DOMException('full', 'QuotaExceededError'), 'quota'],
      ['Firefox NS_ERROR_DOM_QUOTA_REACHED', new DOMException('full', 'NS_ERROR_DOM_QUOTA_REACHED'), 'quota'],
      ['SecurityError', new DOMException('blocked', 'SecurityError'), 'unavailable'],
      ['InvalidStateError', new DOMException('state', 'InvalidStateError'), 'unavailable'],
      ['TypeError', new TypeError('x'), 'unknown'],
      ['문자열', 'QuotaExceededError', 'unknown'],
      ['null', null, 'unknown'],
      ['undefined', undefined, 'unknown'],
    ]
    for (const [label, error, reason] of cases) {
      const { storage, repo } = setup()
      storage.failSet = { error }
      await expect(repo.save(bori), label).resolves.toEqual({ ok: false, reason })
    }
  })

  it('어떤 이유로 저장에 실패해도 이전 값은 그대로다', async () => {
    for (const error of [new DOMException('full', 'QuotaExceededError'), new DOMException('blocked', 'SecurityError'), new Error('?')]) {
      const { storage, repo } = setup()
      await repo.save(bori)
      storage.failSet = { error }
      expect((await repo.save({ ...bori, name: '보리2', medical: { conditions: [], allergies: [], medications: [] } })).ok).toBe(false)
      storage.failSet = null
      expect(await repo.load(), error.name).toEqual({ ok: true, profile: bori })
    }
  })

  it('처음 저장이 실패하면 아무것도 남지 않는다', async () => {
    const { storage, repo } = setup()
    storage.failSet = { error: new DOMException('full', 'QuotaExceededError') }
    await repo.save(bori)
    storage.failSet = null
    expect(storage.values.size).toBe(0)
    expect(await repo.load()).toEqual({ ok: true, profile: null })
  })

  it('저장 실패 뒤 원인이 사라지면 다시 저장할 수 있다', async () => {
    const { storage, repo } = setup()
    storage.failSet = { error: new DOMException('full', 'QuotaExceededError') }
    expect(await repo.save(bori)).toEqual({ ok: false, reason: 'quota' })
    storage.failSet = null
    expect(await repo.save(bori)).toEqual({ ok: true })
    expect(await repo.load()).toEqual({ ok: true, profile: bori })
  })

  it('손상되었거나 다른 버전인 값 위에 새로 저장하면 새 값으로 바뀐다', async () => {
    for (const broken of ['{broken', JSON.stringify({ schemaVersion: 2, id: 'x' })]) {
      const { storage, repo } = setup()
      storage.values.set(PET_PROFILE_STORAGE_KEY, broken)
      expect(await repo.save(bori), broken).toEqual({ ok: true })
      expect(await repo.load(), broken).toEqual({ ok: true, profile: bori })
    }
  })

  it('저장은 읽을 수 없는 저장소에서도 읽기를 거치지 않고 쓴다', async () => {
    const { storage, repo } = setup()
    storage.failGet = { error: new DOMException('blocked', 'SecurityError') }
    expect(await repo.save(bori)).toEqual({ ok: true })
    expect(storage.getCalls).toBe(0)
  })

  it('다시 저장하면 이전 값을 덮어쓰고 다른 키는 건드리지 않는다', async () => {
    const { storage, repo } = setup()
    storage.values.set('other-app.key', 'keep')
    await repo.save(bori)
    const next: PetProfile = { ...bori, name: '보리2', updatedAt: '2026-11-06T00:00:00.000Z' }
    await repo.save(next)
    expect(await repo.load()).toEqual({ ok: true, profile: next })
    expect(storage.values.get('other-app.key')).toBe('keep')
    expect([...storage.values.keys()].sort()).toEqual(['other-app.key', PET_PROFILE_STORAGE_KEY].sort())
  })

  it('받은 updatedAt을 바꾸지 않고 그대로 저장한다', async () => {
    const { repo } = setup()
    const old: PetProfile = { ...bori, updatedAt: '2001-01-01T00:00:00.000Z' }
    await repo.save(old)
    expect(await repo.load()).toEqual({ ok: true, profile: old })
  })

  it('save를 부른 뒤 넘긴 객체를 고쳐도 저장되는 값은 부른 때의 값이다', async () => {
    const { repo } = setup()
    const profile: PetProfile = { ...bori, medical: { ...bori.medical, conditions: [...bori.medical.conditions] } }
    const pending = repo.save(profile)
    profile.name = '바뀜'
    profile.medical.conditions.push('바뀜')
    await pending
    expect(await repo.load()).toEqual({ ok: true, profile: bori })
  })

  it('저장한 값은 형식 문서의 JSON이라 다른 읽는 쪽(차 앱)도 그대로 읽는다', async () => {
    const { storage, repo } = setup()
    await repo.save(bori)
    const raw = storage.values.get(PET_PROFILE_STORAGE_KEY)
    expect(typeof raw).toBe('string')
    expect((JSON.parse(raw ?? '') as { schemaVersion: unknown }).schemaVersion).toBe(1)
    expect(parsePetProfile(raw ?? '')).toEqual({ ok: true, profile: bori })
  })

  it('400,000자 사진이 든 프로필도 저장하고 그대로 읽는다', async () => {
    const { repo } = setup()
    const prefix = 'data:image/jpeg;base64,'
    const withPhoto: PetProfile = { ...bori, photoDataUrl: prefix + 'A'.repeat(400_000 - prefix.length) }
    expect(await repo.save(withPhoto)).toEqual({ ok: true })
    expect(await repo.load()).toEqual({ ok: true, profile: withPhoto })
  })
})
