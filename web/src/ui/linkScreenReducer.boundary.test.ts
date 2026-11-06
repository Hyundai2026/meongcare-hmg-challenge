import { describe, expect, it } from 'vitest'
import {
  LINK_INPUT_ERROR_MESSAGES,
  initialLinkScreenState,
  linkOutcomeMessage,
  linkScreenReducer,
  type LinkScreenAction,
  type LinkScreenState,
} from './linkScreenReducer'

const run = (state: LinkScreenState, ...actions: LinkScreenAction[]): LinkScreenState => actions.reduce(linkScreenReducer, state)
const typed = (text: string) => run(initialLinkScreenState(null), { type: 'input', text })

/** 계획 014 설계 6절 "[차와 연결] 화면"과 3절(K6) 문구의 경계 */
describe('[차와 연결] 화면 경계', () => {
  it('보내는 중이 아닐 때 온 결과는 무시한다', () => {
    const idle = typed('K7P3QX')
    expect(linkScreenReducer(idle, { type: 'done', result: { ok: true } })).toBe(idle)
  })

  it('결과가 두 번 와도 첫 결과만 쓴다', () => {
    const sending = run(typed('K7P3QX'), { type: 'submit' })
    const failed = linkScreenReducer(sending, { type: 'done', result: { ok: false, reason: 'network' } })
    expect(linkScreenReducer(failed, { type: 'done', result: { ok: true } })).toBe(failed)
  })

  it('네트워크 실패 뒤 같은 코드로 다시 보낼 수 있다', () => {
    const failed = run(typed('K7P3QX'), { type: 'submit' }, { type: 'done', result: { ok: false, reason: 'network' } })
    const again = linkScreenReducer(failed, { type: 'submit' })
    expect(again).toMatchObject({ sending: 'K7P3QX', outcome: null })
  })

  it('코드 없음 뒤 고쳐 입력해 다시 보내면 고친 코드를 보낸다', () => {
    const state = run(
      typed('K7P3QX'),
      { type: 'submit' },
      { type: 'done', result: { ok: false, reason: 'not-found' } },
      { type: 'input', text: 'dpr-vfx' },
      { type: 'submit' },
    )
    expect(state).toMatchObject({ sending: 'DPRVFX', outcome: null, inputError: null })
  })

  it('형식 오류는 다시 입력하면 사라진다', () => {
    const wrong = run(typed('K7P3Q'), { type: 'submit' })
    expect(wrong.inputError).toBe('length')
    expect(linkScreenReducer(wrong, { type: 'input', text: 'K7P3QX' }).inputError).toBeNull()
  })

  it('주소에서 채운 값도 보낼 때 정규화한다', () => {
    const state = run(initialLinkScreenState('k7p-3qx'), { type: 'submit' })
    expect(state.sending).toBe('K7P3QX')
  })

  it('6자보다 길게 붙여 넣어도 잘라서 보내지 않는다', () => {
    const state = run(typed('K7P3QXK7P'), { type: 'submit' })
    expect(state).toMatchObject({ sending: null, inputError: 'length' })
  })

  it('빈 입력으로 누르면 보내지 않는다', () => {
    expect(run(initialLinkScreenState(null), { type: 'submit' })).toMatchObject({ sending: null, inputError: 'length' })
  })

  it('형식이 틀려 막힌 뒤에도 이전 결과 문구를 보내는 중으로 바꾸지 않는다', () => {
    const failed = run(typed('K7P3QX'), { type: 'submit' }, { type: 'done', result: { ok: false, reason: 'network' } })
    const wrong = run(failed, { type: 'input', text: 'K7P' }, { type: 'submit' })
    expect(wrong).toMatchObject({ sending: null, inputError: 'length' })
  })

  it('연결된 뒤에는 결과가 또 와도 연결됨이다', () => {
    const linked = run(typed('K7P3QX'), { type: 'submit' }, { type: 'done', result: { ok: true } })
    expect(linkScreenReducer(linked, { type: 'done', result: { ok: false, reason: 'not-found' } }).outcome).toBe('linked')
  })

  it('결과 문구는 계획의 문구 그대로다', () => {
    expect(linkOutcomeMessage('network', null)).toBe('서버에 연결하지 못했어요. 잠시 뒤 다시 해 주세요.')
    expect(linkOutcomeMessage('no-profile', null)).toContain('먼저 반려견 정보를 등록해 주세요')
    expect(linkOutcomeMessage('not-found', '콩이')).toBe('코드를 찾지 못했거나 만료됐어요. 차에서 새 코드를 받아 주세요.')
  })

  it('연결 문구에 이름이 없으면 따옴표 없이 차 화면을 확인하라고 한다', () => {
    const text = linkOutcomeMessage('linked', null)
    expect(text.startsWith('연결했어요.')).toBe(true)
    expect(text).not.toContain("''")
    expect(text).not.toContain('null')
  })

  it('형식 오류 문구는 빠진 글자와 없는 글자를 구분한다', () => {
    expect(LINK_INPUT_ERROR_MESSAGES.length).toContain('6자리')
    expect(LINK_INPUT_ERROR_MESSAGES.characters).toContain('0·1·I·L·O')
  })
})
