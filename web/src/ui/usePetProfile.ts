import { useCallback, useEffect, useReducer, useRef } from 'react'
import type { PetProfile } from '../domain/petProfile'
import type { PetProfileRepository } from '../domain/petProfileRepository'
import { canStartSave, initialProfileScreenState, profileScreenReducer, type Editing } from './profileScreenReducer'

/** 저장소와 화면 상태를 잇는다. */
export function usePetProfile(repository: PetProfileRepository) {
  const [state, dispatch] = useReducer(profileScreenReducer, initialProfileScreenState)
  const stateRef = useRef(state)
  stateRef.current = state

  useEffect(() => {
    let active = true
    void repository.load().then((result) => {
      if (active) dispatch({ type: 'loaded', result })
    })
    return () => {
      active = false
    }
  }, [repository])

  const edit = useCallback((target: Editing) => dispatch({ type: 'edit', target }), [])
  const cancel = useCallback(() => dispatch({ type: 'cancel' }), [])

  const save = useCallback(
    async (profile: PetProfile) => {
      // 두 번 눌러도 한 번만 저장한다 (렌더 전에 다시 눌러도 막히게 ref로 본다).
      if (!canStartSave(stateRef.current)) return
      stateRef.current = profileScreenReducer(stateRef.current, { type: 'save-start' })
      dispatch({ type: 'save-start' })
      const result = await repository.save(profile)
      dispatch({ type: 'save-done', profile, result })
    },
    [repository],
  )

  return { state, edit, cancel, save }
}
