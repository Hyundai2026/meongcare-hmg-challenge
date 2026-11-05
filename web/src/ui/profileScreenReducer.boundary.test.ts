/**
 * 화면 상태 경계 테스트. 기준: 계획 008 설계 결정 6 (화면 상태 표, 저장 실패·저장 중·취소 규칙),
 * "결정 / 배운 점" (읽기 실패 화면에서 새로 등록 → 취소하면 다시 읽기 실패, unavailable이면 새로 등록하지 않음).
 */
import { describe, expect, it } from 'vitest'
import type { PetProfile } from '../domain/petProfile'
import type { LoadResult, SaveResult } from '../domain/petProfileRepository'
import {
  canStartSave,
  initialProfileScreenState,
  profileScreenReducer,
  type ProfileScreenAction,
  type ProfileScreenState,
} from './profileScreenReducer'

const bori: PetProfile = {
  schemaVersion: 1,
  name: '보리',
  breed: '골든리트리버',
  sex: 'male',
  birthDate: '2014-08-01',
  birthDateIsEstimated: false,
  photoDataUrl: null,
  medical: { conditions: [], allergies: [], medications: [] },
  updatedAt: '2026-11-05T00:00:00.000Z',
}
const bori2: PetProfile = { ...bori, name: '보리2', updatedAt: '2026-11-06T00:00:00.000Z' }

const OK: SaveResult = { ok: true }
const QUOTA: SaveResult = { ok: false, reason: 'quota' }

function run(...actions: ProfileScreenAction[]): ProfileScreenState {
  return actions.reduce(profileScreenReducer, initialProfileScreenState)
}

const loaded = (result: LoadResult): ProfileScreenAction => ({ type: 'loaded', result })
const loadedProfile = loaded({ ok: true, profile: bori })
const loadedEmpty = loaded({ ok: true, profile: null })

describe('화면 상태 경계', () => {
  it('불러오는 중에는 편집·저장·취소·저장 결과를 받지 않는다', () => {
    for (const action of [
      { type: 'edit', target: 'profile' },
      { type: 'edit', target: 'medical' },
      { type: 'cancel' },
      { type: 'save-start' },
      { type: 'save-done', profile: bori, result: OK },
    ] as ProfileScreenAction[]) {
      expect(profileScreenReducer(initialProfileScreenState, action), action.type).toEqual({ kind: 'loading' })
    }
    expect(canStartSave(initialProfileScreenState)).toBe(false)
  })

  it('손상·버전 문제일 때는 새로 등록할 수 있고, 저장소를 쓸 수 없을 때는 아니다', () => {
    expect(run(loaded({ ok: false, reason: 'corrupted' }), { type: 'edit', target: 'profile' })).toMatchObject({ editing: 'profile' })
    expect(run(loaded({ ok: false, reason: 'unsupported-version' }), { type: 'edit', target: 'profile' })).toMatchObject({
      editing: 'profile',
    })
    expect(run(loaded({ ok: false, reason: 'unavailable' }), { type: 'edit', target: 'profile' })).toMatchObject({ editing: null })
  })

  it('읽기 실패 상태에서는 의료정보 편집이 열리지 않는다', () => {
    expect(run(loaded({ ok: false, reason: 'corrupted' }), { type: 'edit', target: 'medical' })).toMatchObject({ editing: null })
  })

  it('읽기 실패 상태에서 새로 등록해 저장하면 실패 표시가 사라지고 프로필 화면이다', () => {
    const state = run(
      loaded({ ok: false, reason: 'corrupted' }),
      { type: 'edit', target: 'profile' },
      { type: 'save-start' },
      { type: 'save-done', profile: bori, result: OK },
    )
    expect(state).toEqual({ kind: 'ready', profile: bori, loadError: null, editing: null, saving: false, saveError: null })
  })

  it('읽기 실패 상태에서 새로 등록하다 저장에 실패하면 편집이 남고, 취소하면 다시 읽기 실패 화면이다', () => {
    const failed = run(
      loaded({ ok: false, reason: 'unsupported-version' }),
      { type: 'edit', target: 'profile' },
      { type: 'save-start' },
      { type: 'save-done', profile: bori, result: QUOTA },
    )
    expect(failed).toEqual({
      kind: 'ready',
      profile: null,
      loadError: 'unsupported-version',
      editing: 'profile',
      saving: false,
      saveError: 'quota',
    })
    expect(profileScreenReducer(failed, { type: 'cancel' })).toMatchObject({
      profile: null,
      loadError: 'unsupported-version',
      editing: null,
      saveError: null,
    })
  })

  it('빈 화면에서 처음 등록에 실패하고 취소하면 다시 빈 화면이다', () => {
    const state = run(
      loadedEmpty,
      { type: 'edit', target: 'profile' },
      { type: 'save-start' },
      { type: 'save-done', profile: bori, result: { ok: false, reason: 'unavailable' } },
      { type: 'cancel' },
    )
    expect(state).toEqual({ kind: 'ready', profile: null, loadError: null, editing: null, saving: false, saveError: null })
  })

  it('저장을 두 번 시작해도 저장 결과 한 번으로 저장 중이 끝난다', () => {
    const state = run(
      loadedProfile,
      { type: 'edit', target: 'profile' },
      { type: 'save-start' },
      { type: 'save-start' },
      { type: 'save-done', profile: bori2, result: OK },
    )
    expect(state).toMatchObject({ profile: bori2, editing: null, saving: false })
  })

  it('저장 중에 취소가 와도 저장에 성공하면 저장된 프로필을 보여 준다', () => {
    const state = run(
      loadedProfile,
      { type: 'edit', target: 'profile' },
      { type: 'save-start' },
      { type: 'cancel' },
      { type: 'save-done', profile: bori2, result: OK },
    )
    expect(state).toMatchObject({ profile: bori2, saving: false, editing: null })
  })

  it('저장 실패 뒤 다시 저장을 시작하면 실패 표시를 지우고 저장 중이 된다', () => {
    const failed = run(
      loadedProfile,
      { type: 'edit', target: 'profile' },
      { type: 'save-start' },
      { type: 'save-done', profile: bori2, result: QUOTA },
    )
    expect(canStartSave(failed)).toBe(true)
    const retry = profileScreenReducer(failed, { type: 'save-start' })
    expect(retry).toMatchObject({ saving: true, saveError: null, editing: 'profile', profile: bori })
    expect(canStartSave(retry)).toBe(false)
    expect(profileScreenReducer(retry, { type: 'save-done', profile: bori2, result: OK })).toMatchObject({
      profile: bori2,
      editing: null,
      saveError: null,
    })
  })

  it('의료정보 저장에 실패해도 보이는 프로필은 이전 값이다', () => {
    const withMedical: PetProfile = { ...bori, medical: { conditions: ['심장질환'], allergies: [], medications: [] } }
    const state = run(
      loadedProfile,
      { type: 'edit', target: 'medical' },
      { type: 'save-start' },
      { type: 'save-done', profile: withMedical, result: { ok: false, reason: 'unknown' } },
    )
    expect(state).toMatchObject({ profile: bori, editing: 'medical', saveError: 'unknown', saving: false })
  })

  it('저장을 시작하지 않았는데 온 저장 결과는 무시한다', () => {
    const editing = run(loadedProfile, { type: 'edit', target: 'profile' })
    expect(profileScreenReducer(editing, { type: 'save-done', profile: bori2, result: OK })).toEqual(editing)
    const viewing = run(loadedProfile)
    expect(profileScreenReducer(viewing, { type: 'save-done', profile: bori2, result: OK })).toEqual(viewing)
  })

  it('편집 중이 아닐 때 취소는 아무것도 바꾸지 않는다', () => {
    const viewing = run(loadedProfile)
    expect(profileScreenReducer(viewing, { type: 'cancel' })).toEqual(viewing)
    const loadError = run(loaded({ ok: false, reason: 'corrupted' }))
    expect(profileScreenReducer(loadError, { type: 'cancel' })).toEqual(loadError)
  })
})
