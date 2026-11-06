/**
 * 화면 상태. React를 모르는 순수 함수라 Node에서 테스트한다.
 *
 * - loading: 처음 불러오는 중
 * - ready: 불러오기가 끝났다
 *   - profile null + loadError null  → 빈 화면 (처음)
 *   - profile null + loadError 있음  → 읽기 실패 화면. [새로 등록하기]로 편집을 열 수 있다
 *   - editing: 'profile' / 'medical' 이면 그 편집 화면
 *   - saving: 저장 중 (다시 저장을 시작하지 않는다)
 *   - saveError: 마지막 저장 실패 이유. 편집 화면 위에 보인다
 */
import type { PetProfile } from '../domain/petProfile'
import type { LoadFailure, LoadResult, SaveFailure, SaveResult } from '../domain/petProfileRepository'

export type Editing = 'profile' | 'medical'

export type ProfileScreenState =
  | { kind: 'loading' }
  | {
      kind: 'ready'
      profile: PetProfile | null
      loadError: LoadFailure | null
      editing: Editing | null
      saving: boolean
      saveError: SaveFailure | null
    }

export type ProfileScreenAction =
  | { type: 'loaded'; result: LoadResult }
  | { type: 'edit'; target: Editing }
  | { type: 'cancel' }
  | { type: 'save-start' }
  | { type: 'save-done'; profile: PetProfile; result: SaveResult }

export const initialProfileScreenState: ProfileScreenState = { kind: 'loading' }

export function canStartSave(state: ProfileScreenState): boolean {
  return state.kind === 'ready' && state.editing !== null && !state.saving
}

export function profileScreenReducer(state: ProfileScreenState, action: ProfileScreenAction): ProfileScreenState {
  switch (action.type) {
    case 'loaded': {
      const { result } = action
      return {
        kind: 'ready',
        profile: result.ok ? result.profile : null,
        loadError: result.ok ? null : result.reason,
        editing: null,
        saving: false,
        saveError: null,
      }
    }
    case 'edit': {
      if (state.kind !== 'ready' || state.editing !== null) return state
      // 의료정보는 등록된 프로필이 있어야 고친다. 읽을 수 없는 저장소에는 새로 등록하지 않는다.
      // 서버를 읽지 못했으면(network) 서버의 기존 프로필을 빈 새 프로필로 덮어쓰지 않게 새로 등록을 막는다 (014).
      if (action.target === 'medical' && state.profile === null) return state
      if (action.target === 'profile' && (state.loadError === 'unavailable' || state.loadError === 'network')) return state
      return { ...state, editing: action.target, saveError: null }
    }
    case 'cancel': {
      if (state.kind !== 'ready' || state.saving) return state
      return { ...state, editing: null, saveError: null }
    }
    case 'save-start': {
      if (!canStartSave(state) || state.kind !== 'ready') return state
      return { ...state, saving: true, saveError: null }
    }
    case 'save-done': {
      if (state.kind !== 'ready' || !state.saving) return state
      if (action.result.ok) {
        return { ...state, profile: action.profile, loadError: null, editing: null, saving: false, saveError: null }
      }
      return { ...state, saving: false, saveError: action.result.reason }
    }
  }
}
