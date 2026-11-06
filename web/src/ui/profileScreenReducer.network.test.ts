import { describe, expect, it } from 'vitest'
import { LOAD_FAILURE_MESSAGES, SAVE_FAILURE_MESSAGES } from './messages'
import { initialProfileScreenState, profileScreenReducer } from './profileScreenReducer'

/** 계획 014: 서버(Firestore)를 읽지 못했을 때 */
describe("읽기가 'network'로 실패하면", () => {
  const failed = profileScreenReducer(initialProfileScreenState, { type: 'loaded', result: { ok: false, reason: 'network' } })

  it('새로 등록을 막는다 (서버의 기존 프로필을 빈 새 프로필로 덮어쓰지 않게)', () => {
    expect(profileScreenReducer(failed, { type: 'edit', target: 'profile' })).toBe(failed)
  })

  it('읽기 실패로 남는다', () => {
    expect(failed).toMatchObject({ kind: 'ready', profile: null, loadError: 'network' })
  })

  it('읽기·저장 실패 문구가 있다', () => {
    expect(LOAD_FAILURE_MESSAGES.network).toContain('서버에 연결하지 못해')
    expect(SAVE_FAILURE_MESSAGES.network).toContain('서버에 연결하지 못해')
  })

  it("저장이 'network'로 실패하면 편집 화면에 남는다", () => {
    const ready = profileScreenReducer(initialProfileScreenState, { type: 'loaded', result: { ok: true, profile: null } })
    const editing = profileScreenReducer(ready, { type: 'edit', target: 'profile' })
    const saving = profileScreenReducer(editing, { type: 'save-start' })
    const done = profileScreenReducer(saving, {
      type: 'save-done',
      profile: { schemaVersion: 1, name: '콩이', breed: null, sex: null, birthDate: null, birthDateIsEstimated: false, photoDataUrl: null, medical: { conditions: [], allergies: [], medications: [] }, updatedAt: '2026-11-06T00:00:00.000Z' },
      result: { ok: false, reason: 'network' },
    })
    expect(done).toMatchObject({ editing: 'profile', saving: false, saveError: 'network' })
  })
})
