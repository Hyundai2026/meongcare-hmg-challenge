import { describe, expect, it } from 'vitest'
import example from '../../../../docs/data/pet-profile.example.json'
import type { PetProfile } from '../petProfile'
import { petProfileFromJson } from '../petProfileJson'
import type { LoadResult, PetProfileRepository, SaveResult } from '../petProfileRepository'
import { migrateBrowserProfile, migrationStepOf, type BrowserProfileStore } from './migration'

function bori(): PetProfile {
  const parsed = petProfileFromJson(example)
  if (!parsed.ok) throw new Error('예시가 읽히지 않는다')
  return parsed.profile
}

class MemoryStore implements BrowserProfileStore {
  saves: PetProfile[] = []
  cleared = false
  loadResult: LoadResult
  private saveResult: SaveResult
  constructor(loadResult: LoadResult, saveResult: SaveResult = { ok: true }) {
    this.loadResult = loadResult
    this.saveResult = saveResult
  }
  async load() {
    return this.loadResult
  }
  async save(profile: PetProfile) {
    this.saves.push(profile)
    return this.saveResult
  }
  async clear() {
    this.cleared = true
    return true
  }
}

const none: LoadResult = { ok: true, profile: null }

describe('브라우저 정보 옮기기 (계획 014 U6)', () => {
  it('브라우저에만 있으면 올리고 브라우저에서 지운다', async () => {
    const browser = new MemoryStore({ ok: true, profile: bori() })
    const server = new MemoryStore(none)
    expect(await migrateBrowserProfile(browser, server)).toEqual({ kind: 'moved', profile: bori() })
    expect(server.saves).toEqual([bori()])
    expect(browser.cleared).toBe(true)
  })

  it('서버에 이미 있으면 덮어쓰지 않고 브라우저 정보도 둔다', async () => {
    const kongi = { ...bori(), name: '콩이' }
    const browser = new MemoryStore({ ok: true, profile: bori() })
    const server = new MemoryStore({ ok: true, profile: kongi })
    expect(await migrateBrowserProfile(browser, server)).toEqual({ kind: 'server-kept' })
    expect(server.saves).toEqual([])
    expect(browser.cleared).toBe(false)
  })

  it('올리기에 실패하면 브라우저 정보는 그대로다', async () => {
    const browser = new MemoryStore({ ok: true, profile: bori() })
    const server = new MemoryStore(none, { ok: false, reason: 'network' })
    expect(await migrateBrowserProfile(browser, server)).toEqual({ kind: 'failed' })
    expect(browser.cleared).toBe(false)
  })

  it('둘 다 없으면 할 일이 없다', async () => {
    const browser = new MemoryStore(none)
    const server = new MemoryStore(none)
    expect(await migrateBrowserProfile(browser, server)).toEqual({ kind: 'nothing' })
    expect(server.saves).toEqual([])
  })

  it('서버를 읽지 못하면 아무것도 쓰지 않는다', async () => {
    const browser = new MemoryStore({ ok: true, profile: bori() })
    const server = new MemoryStore({ ok: false, reason: 'network' })
    expect(await migrateBrowserProfile(browser, server)).toEqual({ kind: 'failed' })
    expect(server.saves).toEqual([])
    expect(browser.cleared).toBe(false)
  })

  it('브라우저 정보가 손상됐으면 옮기지 않는다', () => {
    expect(migrationStepOf({ ok: false, reason: 'corrupted' }, none)).toEqual({ kind: 'nothing' })
  })

  it('서버 저장소만 바꿔 끼워도 같은 인터페이스다', () => {
    const server: PetProfileRepository = new MemoryStore(none)
    expect(typeof server.load).toBe('function')
  })
})
