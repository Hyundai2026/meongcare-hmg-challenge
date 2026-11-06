import { useReducer, useRef, type FormEvent } from 'react'
import type { OwnerAccount } from '../domain/owner/ownerAccount'
import {
  LINK_INPUT_ERROR_MESSAGES,
  initialLinkScreenState,
  linkOutcomeMessage,
  linkScreenReducer,
} from './linkScreenReducer'
import type { CarLinkStatus } from './ownerBanner'

const LINKED_AT = new Intl.DateTimeFormat('ko-KR', { month: 'long', day: 'numeric', hour: 'numeric', minute: '2-digit' })

/**
 * 프로필 바로 아래 [차와 연결] 카드 (계획 014). 동의 전이어도 같은 카드다: 누르면 필요할 때만 동의를 거쳐 코드 입력으로 간다.
 * 연결한 차가 있으면 "차와 연결됨"으로 바뀐다 (차가 끊으면 실시간으로 돌아온다).
 */
export function CarLinkCard({ status, onOpen }: { status: CarLinkStatus; onOpen: () => void }) {
  if (status.kind === 'unknown') return null
  if (status.kind === 'linked') {
    const when = status.lastLinkedAt === null ? null : LINKED_AT.format(new Date(status.lastLinkedAt))
    return (
      <section className="card server-card server-card--linked" aria-live="polite">
        <h2 className="server-card__title server-card__title--linked">
          <svg className="server-card__check" viewBox="0 0 20 20" aria-hidden="true">
            <circle cx="10" cy="10" r="10" fill="currentColor" />
            <path d="M5.5 10.5l3 3 6-6.5" fill="none" stroke="#fff" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
          </svg>
          차와 연결됨
        </h2>
        <p className="server-card__body">
          {when !== null ? `${when}에 연결했어요. ` : ''}차에서 진료 카드를 만들면 이 정보가 들어가요.
          {status.count > 1 ? ` (연결한 차 ${status.count}대)` : ''}
        </p>
        <button type="button" className="button button--text server-card__more" onClick={onOpen}>
          다른 차와 연결
        </button>
      </section>
    )
  }
  return (
    <section className="card server-card">
      <h2 className="server-card__title">차와 연결</h2>
      <p className="server-card__body">차를 세우고 MeongCare 앱의 [보호자 웹과 연결]을 누르면 6자리 코드가 보여요. 연결하면 차에서 글자 입력 없이 진료 카드를 만들어요.</p>
      <button type="button" className="button button--primary button--block" onClick={onOpen}>
        차와 연결하기
      </button>
    </section>
  )
}

interface Props {
  account: OwnerAccount
  /** 등록된 반려견 이름. 없으면 먼저 등록하라고 알리고 보내지 않는다 */
  petName: string | null
  /** 주소의 ?code= 값 (나중 QR) */
  prefill: string | null
  onBack: () => void
}

export function LinkCarScreen({ account, petName, prefill, onBack }: Props) {
  const [state, dispatch] = useReducer(linkScreenReducer, prefill, initialLinkScreenState)
  const stateRef = useRef(state)
  stateRef.current = state

  const submit = (event: FormEvent) => {
    event.preventDefault()
    // 두 번 눌러도 한 번만 보낸다 (렌더 전에 다시 눌러도 막히게 ref로 본다)
    const before = stateRef.current
    const next = linkScreenReducer(before, { type: 'submit' })
    if (next === before) return
    stateRef.current = next
    dispatch({ type: 'submit' })
    const code = next.sending
    if (code === null || before.sending !== null) return
    void account.linkCar(code).then((result) => dispatch({ type: 'done', result }))
  }

  const linked = state.outcome === 'linked'
  const failed = state.outcome !== null && !linked

  return (
    <form className="form card" onSubmit={submit} noValidate>
      <h1 className="form__title">차와 연결</h1>
      {petName === null ? (
        <p className="banner banner--error" role="alert">
          {linkOutcomeMessage('no-profile', null)}
        </p>
      ) : (
        <>
          <div className="field">
            <label className="field__label" htmlFor="link-code">
              차 화면에 보이는 6자리 코드를 입력하세요
            </label>
            <input
              id="link-code"
              className="input input--code"
              value={state.input}
              onChange={(e) => dispatch({ type: 'input', text: e.target.value })}
              autoComplete="off"
              autoCapitalize="characters"
              autoCorrect="off"
              spellCheck={false}
              inputMode="text"
              maxLength={12}
              placeholder="K7P 3QX"
              disabled={state.sending !== null || linked}
              aria-invalid={state.inputError !== null}
              aria-describedby={state.inputError !== null ? 'link-code-error' : 'link-code-hint'}
            />
            {state.inputError !== null ? (
              <p className="field__error" id="link-code-error">
                {LINK_INPUT_ERROR_MESSAGES[state.inputError]}
              </p>
            ) : (
              <p className="field__hint" id="link-code-hint">
                코드는 10분 동안 쓸 수 있어요.
              </p>
            )}
          </div>
          {linked && (
            <p className="banner banner--success" role="status">
              {linkOutcomeMessage('linked', petName)}
            </p>
          )}
          {failed && state.outcome !== null && (
            <p className="banner banner--error" role="alert">
              {linkOutcomeMessage(state.outcome, petName)}
            </p>
          )}
        </>
      )}
      <div className="form__actions">
        <button type="button" className="button button--secondary" onClick={onBack} disabled={state.sending !== null}>
          {linked ? '완료' : '돌아가기'}
        </button>
        {!linked && petName !== null && (
          <button type="submit" className="button button--primary" disabled={state.sending !== null}>
            {state.sending !== null ? '연결하는 중…' : '연결하기'}
          </button>
        )}
      </div>
    </form>
  )
}
