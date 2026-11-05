import { describe, expect, it } from 'vitest'
import type { PetProfile } from '../domain/petProfile'
import {
  canStartSave,
  initialProfileScreenState,
  profileScreenReducer as reduce,
  type ProfileScreenAction,
  type ProfileScreenState,
} from './profileScreenReducer'

const bori: PetProfile = {
  schemaVersion: 1,
  name: '보리',
  breed: null,
  sex: null,
  birthDate: null,
  birthDateIsEstimated: false,
  photoDataUrl: null,
  medical: { conditions: [], allergies: [], medications: [] },
  updatedAt: '2026-11-05T00:00:00.000Z',
}
const bori2: PetProfile = { ...bori, name: '보리2' }

function run(...actions: ProfileScreenAction[]): ProfileScreenState {
  return actions.reduce(reduce, initialProfileScreenState)
}

const loadedBori: ProfileScreenAction = { type: 'loaded', result: { ok: true, profile: bori } }

describe('화면 상태', () => {
  it('처음에는 불러오는 중이다', () => {
    expect(initialProfileScreenState).toEqual({ kind: 'loading' })
  })

  it('불러온 프로필이 없으면 빈 상태다', () => {
    expect(run({ type: 'loaded', result: { ok: true, profile: null } })).toEqual({
      kind: 'ready',
      profile: null,
      loadError: null,
      editing: null,
      saving: false,
      saveError: null,
    })
  })

  it('불러오기에 실패하면 이유와 함께 읽기 실패 상태다', () => {
    const state = run({ type: 'loaded', result: { ok: false, reason: 'corrupted' } })
    expect(state).toMatchObject({ kind: 'ready', profile: null, loadError: 'corrupted', editing: null })
  })

  it('읽기 실패 상태에서 새로 등록할 수 있고, 취소하면 다시 읽기 실패 화면이다', () => {
    const editing = run({ type: 'loaded', result: { ok: false, reason: 'corrupted' } }, { type: 'edit', target: 'profile' })
    expect(editing).toMatchObject({ editing: 'profile', loadError: 'corrupted' })
    expect(reduce(editing, { type: 'cancel' })).toMatchObject({ editing: null, loadError: 'corrupted', profile: null })
  })

  it('저장소를 쓸 수 없으면 새로 등록하지 않는다', () => {
    const state = run({ type: 'loaded', result: { ok: false, reason: 'unavailable' } }, { type: 'edit', target: 'profile' })
    expect(state).toMatchObject({ editing: null })
  })

  it('프로필이 없으면 의료정보를 고치지 않는다', () => {
    const state = run({ type: 'loaded', result: { ok: true, profile: null } }, { type: 'edit', target: 'medical' })
    expect(state).toMatchObject({ editing: null })
  })

  it('저장에 성공하면 새 프로필을 보여 주고 편집을 닫는다', () => {
    const state = run(
      { type: 'loaded', result: { ok: false, reason: 'corrupted' } },
      { type: 'edit', target: 'profile' },
      { type: 'save-start' },
      { type: 'save-done', profile: bori2, result: { ok: true } },
    )
    expect(state).toEqual({ kind: 'ready', profile: bori2, loadError: null, editing: null, saving: false, saveError: null })
  })

  it('저장에 실패하면 편집을 유지하고 이유를 남기며 이전 프로필은 그대로다', () => {
    const state = run(
      loadedBori,
      { type: 'edit', target: 'profile' },
      { type: 'save-start' },
      { type: 'save-done', profile: bori2, result: { ok: false, reason: 'quota' } },
    )
    expect(state).toMatchObject({ profile: bori, editing: 'profile', saving: false, saveError: 'quota' })
  })

  it('저장 중에는 다시 저장을 시작하지 않는다', () => {
    const saving = run(loadedBori, { type: 'edit', target: 'profile' }, { type: 'save-start' })
    expect(canStartSave(saving)).toBe(false)
    expect(reduce(saving, { type: 'save-start' })).toBe(saving)
    expect(reduce(saving, { type: 'cancel' })).toBe(saving)
  })

  it('편집 중이 아니면 저장을 시작하지 않는다', () => {
    const state = run(loadedBori)
    expect(canStartSave(state)).toBe(false)
    expect(reduce(state, { type: 'save-start' })).toBe(state)
  })

  it('편집을 취소하면 저장 실패 표시도 지운다', () => {
    const state = run(
      loadedBori,
      { type: 'edit', target: 'medical' },
      { type: 'save-start' },
      { type: 'save-done', profile: bori2, result: { ok: false, reason: 'unknown' } },
      { type: 'cancel' },
    )
    expect(state).toMatchObject({ profile: bori, editing: null, saveError: null })
  })

  it('다시 편집을 열면 이전 저장 실패 표시가 없다', () => {
    const state = run(
      loadedBori,
      { type: 'edit', target: 'profile' },
      { type: 'save-start' },
      { type: 'save-done', profile: bori2, result: { ok: false, reason: 'quota' } },
      { type: 'cancel' },
      { type: 'edit', target: 'profile' },
    )
    expect(state).toMatchObject({ editing: 'profile', saveError: null })
  })
})
