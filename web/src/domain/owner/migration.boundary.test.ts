import { describe, expect, it } from 'vitest'
import example from '../../../../docs/data/pet-profile.example.json'
import type { PetProfile } from '../petProfile'
import { petProfileFromJson } from '../petProfileJson'
import type { LoadResult, SaveResult } from '../petProfileRepository'
import { migrateBrowserProfile, migrationStepOf, type BrowserProfileStore } from './migration'

function bori(): PetProfile {
  const parsed = petProfileFromJson(example)
  if (!parsed.ok) throw new Error('예시가 읽히지 않는다')
  return parsed.profile
}

/** 메모리 저장소. 부른 순서를 남긴다 */
class MemoryStore implements BrowserProfileStore {
  saves: PetProfile[] = []
  clears = 0
  loadResult: LoadResult
  saveResult: SaveResult
  private readonly label: string
  private readonly calls: string[]
  constructor(label: string, calls: string[], loadResult: LoadResult, saveResult: SaveResult = { ok: true }) {
    this.label = label
    this.calls = calls
    this.loadResult = loadResult
    this.saveResult = saveResult
  }
  async load() {
    this.calls.push(`${this.label}.load`)
    return this.loadResult
  }
  async save(profile: PetProfile) {
    this.calls.push(`${this.label}.save`)
    this.saves.push(profile)
    if (this.saveResult.ok) this.loadResult = { ok: true, profile }
    return this.saveResult
  }
  async clear() {
    this.calls.push(`${this.label}.clear`)
    this.clears++
    this.loadResult = { ok: true, profile: null }
    return true
  }
}

const none: LoadResult = { ok: true, profile: null }
const PHOTO = 'data:image/jpeg;base64,/9j/4AAQ'

function stores(browserLoad: LoadResult, serverLoad: LoadResult, serverSave: SaveResult = { ok: true }) {
  const calls: string[] = []
  return { calls, browser: new MemoryStore('browser', calls, browserLoad), server: new MemoryStore('server', calls, serverLoad, serverSave) }
}

/** 계획 014 U6, migration.ts 표의 경계: 브라우저 정보를 잃지 않고, 서버를 덮어쓰지 않는다 */
describe('브라우저 정보 옮기기 경계', () => {
  it('서버를 읽은 뒤에만 올리고, 올리기가 성공한 뒤에만 브라우저를 지운다', async () => {
    const { calls, browser, server } = stores({ ok: true, profile: bori() }, none)
    await migrateBrowserProfile(browser, server)
    expect(calls.indexOf('server.load')).toBeLessThan(calls.indexOf('server.save'))
    expect(calls.indexOf('server.save')).toBeLessThan(calls.indexOf('browser.clear'))
  })

  it('사진과 저장 시각까지 브라우저 프로필 그대로 올린다', async () => {
    const profile = { ...bori(), photoDataUrl: PHOTO, updatedAt: '2025-01-02T03:04:05.000Z' }
    const { browser, server } = stores({ ok: true, profile }, none)
    expect(await migrateBrowserProfile(browser, server)).toEqual({ kind: 'moved', profile })
    expect(server.saves).toEqual([profile])
  })

  it('어떤 이유로 올리기에 실패해도 브라우저 정보는 지우지 않는다', async () => {
    for (const reason of ['network', 'quota', 'unavailable', 'unknown'] as const) {
      const { browser, server } = stores({ ok: true, profile: bori() }, none, { ok: false, reason })
      expect(await migrateBrowserProfile(browser, server), reason).toEqual({ kind: 'failed' })
      expect(browser.clears, reason).toBe(0)
    }
  })

  it('서버를 어떤 이유로 읽지 못해도 서버에 쓰지 않고 브라우저도 지우지 않는다', async () => {
    for (const reason of ['network', 'corrupted', 'unsupported-version', 'unavailable'] as const) {
      const { browser, server } = stores({ ok: true, profile: bori() }, { ok: false, reason })
      await migrateBrowserProfile(browser, server)
      expect(server.saves, reason).toEqual([])
      expect(browser.clears, reason).toBe(0)
    }
  })

  it('읽지 못한 브라우저 정보(손상·새 버전)는 옮기지도 지우지도 않는다', async () => {
    for (const reason of ['corrupted', 'unsupported-version', 'unavailable'] as const) {
      const { browser, server } = stores({ ok: false, reason }, none)
      expect(await migrateBrowserProfile(browser, server), reason).toEqual({ kind: 'nothing' })
      expect(server.saves, reason).toEqual([])
      expect(browser.clears, reason).toBe(0)
    }
  })

  it('서버 정보가 브라우저보다 오래됐어도 덮어쓰지 않는다', async () => {
    const older = { ...bori(), name: '콩이', updatedAt: '2020-01-01T00:00:00.000Z' }
    const newer = { ...bori(), updatedAt: '2026-11-06T00:00:00.000Z' }
    const { browser, server } = stores({ ok: true, profile: newer }, { ok: true, profile: older })
    expect(await migrateBrowserProfile(browser, server)).toEqual({ kind: 'server-kept' })
    expect(server.saves).toEqual([])
    expect(browser.clears).toBe(0)
  })

  it('옮긴 뒤 다시 해도 서버를 다시 쓰지 않는다', async () => {
    const { browser, server } = stores({ ok: true, profile: bori() }, none)
    await migrateBrowserProfile(browser, server)
    expect(await migrateBrowserProfile(browser, server)).toEqual({ kind: 'nothing' })
    expect(server.saves).toHaveLength(1)
  })

  it('브라우저가 비고 서버를 읽지 못하면 아무것도 쓰거나 지우지 않는다', async () => {
    const { browser, server } = stores(none, { ok: false, reason: 'network' })
    const outcome = await migrateBrowserProfile(browser, server)
    expect(['nothing', 'failed']).toContain(outcome.kind)
    expect(server.saves).toEqual([])
    expect(browser.clears).toBe(0)
  })

  it('할 일 계산은 서버 읽기 실패를 브라우저보다 먼저 본다', () => {
    expect(migrationStepOf({ ok: true, profile: bori() }, { ok: false, reason: 'network' })).toEqual({ kind: 'wait' })
  })
})
