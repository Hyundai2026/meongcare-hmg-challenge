/**
 * [차와 연결] 화면 상태 (계획 014). React를 모르는 순수 함수라 Node에서 테스트한다.
 * 형식이 틀리면 보내지 않고, 보내는 중에는 다시 보내지 않는다.
 */
import { cleanLinkCodeText, normalizeLinkCode } from '../domain/owner/linkCode'
import type { LinkCarResult } from '../domain/owner/ownerAccount'

export type LinkOutcome = 'linked' | 'not-found' | 'no-profile' | 'network' | 'unknown'
export type LinkInputError = 'length' | 'characters'

export interface LinkScreenState {
  /** 입력칸에 보이는 값 (공백·하이픈 빼고 대문자) */
  input: string
  inputError: LinkInputError | null
  /** 보내는 중인 코드. 보내는 중이 아니면 null */
  sending: string | null
  outcome: LinkOutcome | null
}

export type LinkScreenAction = { type: 'input'; text: string } | { type: 'submit' } | { type: 'done'; result: LinkCarResult }

export function initialLinkScreenState(prefill: string | null): LinkScreenState {
  return { input: prefill ?? '', inputError: null, sending: null, outcome: null }
}

export function linkScreenReducer(state: LinkScreenState, action: LinkScreenAction): LinkScreenState {
  switch (action.type) {
    case 'input':
      if (state.sending !== null) return state
      return { ...state, input: cleanLinkCodeText(action.text), inputError: null, outcome: state.outcome === 'linked' ? 'linked' : null }
    case 'submit': {
      if (state.sending !== null || state.outcome === 'linked') return state
      const code = normalizeLinkCode(state.input)
      if (!code.ok) return { ...state, inputError: code.reason }
      return { ...state, inputError: null, sending: code.code, outcome: null }
    }
    case 'done':
      if (state.sending === null) return state
      return { ...state, sending: null, outcome: action.result.ok ? 'linked' : action.result.reason }
  }
}

export const LINK_INPUT_ERROR_MESSAGES: Record<LinkInputError, string> = {
  length: '차 화면에 보이는 6자리 코드를 모두 입력해 주세요.',
  characters: '코드에 없는 글자가 있어요. 차 화면의 코드를 다시 확인해 주세요. (0·1·I·L·O는 쓰지 않아요)',
}

/** 결과 문구. 연결됐으면 반려견 이름을 넣어 차 화면을 확인하게 한다 */
export function linkOutcomeMessage(outcome: LinkOutcome, petName: string | null): string {
  switch (outcome) {
    case 'linked':
      return petName !== null
        ? `연결했어요. 차 화면에 '${petName}'가 보이는지 확인해 주세요.`
        : '연결했어요. 차 화면을 확인해 주세요.'
    case 'not-found':
      return '코드를 찾지 못했거나 만료됐어요. 차에서 새 코드를 받아 주세요.'
    case 'no-profile':
      return '먼저 반려견 정보를 등록해 주세요.'
    case 'network':
      return '서버에 연결하지 못했어요. 잠시 뒤 다시 해 주세요.'
    case 'unknown':
      return '연결하지 못했어요. 잠시 뒤 다시 해 주세요.'
  }
}
