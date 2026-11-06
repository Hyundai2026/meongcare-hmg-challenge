import { describe, expect, it } from 'vitest'
import example from '../../../docs/data/pet-profile.example.json'
import type { PetProfile } from '../domain/petProfile'
import { petProfileFromJson } from '../domain/petProfileJson'
import type { LoadFailure, SaveFailure } from '../domain/petProfileRepository'
import { LOAD_FAILURE_MESSAGES, SAVE_FAILURE_MESSAGES } from './messages'
import { canStartSave, initialProfileScreenState, profileScreenReducer, type ProfileScreenAction, type ProfileScreenState } from './profileScreenReducer'

function bori(): PetProfile {
  const parsed = petProfileFromJson(example)
  if (!parsed.ok) throw new Error('예시가 읽히지 않는다')
  return parsed.profile
}

const run = (...actions: ProfileScreenAction[]): ProfileScreenState => actions.reduce(profileScreenReducer, initialProfileScreenState)

/** 계획 014 설계 1절: 읽기가 'network'로 실패하면 새로 등록을 막는다 (서버의 기존 프로필을 덮어쓰지 않게) */
describe("'network' 실패 경계", () => {
  it('서버를 읽지 못한 동안에는 의료정보 편집도 열리지 않는다', () => {
    const failed = run({ type: 'loaded', result: { ok: false, reason: 'network' } })
    expect(profileScreenReducer(failed, { type: 'edit', target: 'medical' })).toBe(failed)
    expect(canStartSave(failed)).toBe(false)
  })

  it('막힌 상태에서 저장을 시작해도 저장하지 않는다', () => {
    const failed = run({ type: 'loaded', result: { ok: false, reason: 'network' } })
    expect(profileScreenReducer(failed, { type: 'save-start' })).toBe(failed)
  })

  it('다시 읽어서 되면 등록과 편집을 열 수 있다', () => {
    const recovered = run(
      { type: 'loaded', result: { ok: false, reason: 'network' } },
      { type: 'loaded', result: { ok: true, profile: null } },
      { type: 'edit', target: 'profile' },
    )
    expect(recovered).toMatchObject({ kind: 'ready', loadError: null, editing: 'profile' })
  })

  it('손상(corrupted)은 지금처럼 새로 등록할 수 있다', () => {
    const corrupted = run({ type: 'loaded', result: { ok: false, reason: 'corrupted' } }, { type: 'edit', target: 'profile' })
    expect(corrupted).toMatchObject({ editing: 'profile' })
  })

  it('저장이 network로 실패하면 이전 프로필은 그대로이고 다시 저장할 수 있다', () => {
    const editing = run({ type: 'loaded', result: { ok: true, profile: bori() } }, { type: 'edit', target: 'medical' }, { type: 'save-start' })
    const changed = { ...bori(), medical: { ...bori().medical, allergies: ['닭고기'] } }
    const failed = profileScreenReducer(editing, { type: 'save-done', profile: changed, result: { ok: false, reason: 'network' } })
    expect(failed).toMatchObject({ profile: bori(), editing: 'medical', saving: false, saveError: 'network' })
    expect(canStartSave(failed)).toBe(true)
  })

  it('모든 읽기·저장 실패 이유에 문구가 있다', () => {
    const loads: LoadFailure[] = ['unavailable', 'corrupted', 'unsupported-version', 'network']
    const saves: SaveFailure[] = ['quota', 'unavailable', 'unknown', 'network']
    for (const reason of loads) expect(LOAD_FAILURE_MESSAGES[reason].length, reason).toBeGreaterThan(0)
    for (const reason of saves) expect(SAVE_FAILURE_MESSAGES[reason].length, reason).toBeGreaterThan(0)
  })

  it('network 문구는 새로 등록을 권하지 않는다', () => {
    // 새로 등록하면 서버의 기존 프로필을 덮어쓸 수 있다
    expect(LOAD_FAILURE_MESSAGES.network).not.toContain('새로 등록')
  })
})
