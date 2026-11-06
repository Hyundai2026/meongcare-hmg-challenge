import { describe, expect, it } from 'vitest'
import { initialLinkScreenState, linkOutcomeMessage, linkScreenReducer, type LinkScreenState } from './linkScreenReducer'

const typed = (text: string): LinkScreenState => linkScreenReducer(initialLinkScreenState(null), { type: 'input', text })

describe('[차와 연결] 화면 (계획 014)', () => {
  it('입력칸은 공백·하이픈을 빼고 대문자로 보인다', () => {
    expect(typed('k7p-3qx').input).toBe('K7P3QX')
  })

  it('형식이 틀리면 보내지 않는다', () => {
    const short = linkScreenReducer(typed('K7P3Q'), { type: 'submit' })
    expect(short).toMatchObject({ sending: null, inputError: 'length' })
    const wrong = linkScreenReducer(typed('K7P3Q0'), { type: 'submit' })
    expect(wrong).toMatchObject({ sending: null, inputError: 'characters' })
  })

  it('보내는 중에 두 번 눌러도 한 번만 보낸다', () => {
    const sending = linkScreenReducer(typed('k7p 3qx'), { type: 'submit' })
    expect(sending.sending).toBe('K7P3QX')
    expect(linkScreenReducer(sending, { type: 'submit' })).toBe(sending)
    // 보내는 중에는 입력도 바꾸지 않는다
    expect(linkScreenReducer(sending, { type: 'input', text: 'AAAAAA' })).toBe(sending)
  })

  it('성공·코드 없음·네트워크·프로필 없음 상태가 된다', () => {
    const sending = linkScreenReducer(typed('K7P3QX'), { type: 'submit' })
    expect(linkScreenReducer(sending, { type: 'done', result: { ok: true } }).outcome).toBe('linked')
    for (const reason of ['not-found', 'network', 'no-profile', 'unknown'] as const) {
      const done = linkScreenReducer(sending, { type: 'done', result: { ok: false, reason } })
      expect(done).toMatchObject({ sending: null, outcome: reason })
    }
  })

  it('연결된 뒤에는 다시 보내지 않는다', () => {
    const sending = linkScreenReducer(typed('K7P3QX'), { type: 'submit' })
    const linked = linkScreenReducer(sending, { type: 'done', result: { ok: true } })
    expect(linkScreenReducer(linked, { type: 'submit' })).toBe(linked)
  })

  it('실패한 뒤 고쳐 입력하면 실패 문구가 사라진다', () => {
    const sending = linkScreenReducer(typed('K7P3QX'), { type: 'submit' })
    const failed = linkScreenReducer(sending, { type: 'done', result: { ok: false, reason: 'not-found' } })
    expect(linkScreenReducer(failed, { type: 'input', text: 'K7P3QY' }).outcome).toBeNull()
  })

  it('주소의 코드로 입력칸을 채워 시작한다', () => {
    expect(initialLinkScreenState('K7P3QX').input).toBe('K7P3QX')
  })

  it('코드 없음·만료·이미 씀은 한 문구다', () => {
    expect(linkOutcomeMessage('not-found', null)).toBe('코드를 찾지 못했거나 만료됐어요. 차에서 새 코드를 받아 주세요.')
    expect(linkOutcomeMessage('linked', '콩이')).toBe("연결했어요. 차 화면에 '콩이'가 보이는지 확인해 주세요.")
  })
})
