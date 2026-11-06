import { CONSENT_ITEMS, CONSENT_TITLE, consentMigrationText } from '../domain/owner/consent'

interface Props {
  /** 브라우저에 저장한 반려견 이름. 있으면 옮긴다는 문장을 더한다 */
  browserPetName: string | null
  busy: boolean
  error: 'network' | 'unknown' | null
  onAgree: () => void
  onLater: () => void
}

/** 서버 저장 동의 화면. 다른 행동을 동의로 보지 않는다 (명시적인 [동의하고 서버에 저장]만) */
export function ConsentScreen({ browserPetName, busy, error, onAgree, onLater }: Props) {
  return (
    <section className="form card" aria-labelledby="consent-title">
      <h1 className="form__title" id="consent-title">
        {CONSENT_TITLE}
      </h1>
      <dl className="consent">
        {CONSENT_ITEMS.map((item) => (
          <div className="consent__row" key={item.label}>
            <dt className="consent__label">{item.label}</dt>
            <dd className="consent__text">{item.text}</dd>
          </div>
        ))}
      </dl>
      {browserPetName !== null && <p className="form__lead">{consentMigrationText(browserPetName)}</p>}
      {error !== null && (
        <p className="banner banner--error" role="alert">
          {error === 'network' ? '서버에 연결하지 못했어요. 잠시 뒤 다시 해 주세요.' : '저장하지 못했어요. 잠시 뒤 다시 해 주세요.'}
        </p>
      )}
      <div className="form__actions">
        <button type="button" className="button button--secondary" onClick={onLater} disabled={busy}>
          나중에
        </button>
        <button type="button" className="button button--primary" onClick={onAgree} disabled={busy}>
          {busy ? '저장하는 중…' : '동의하고 연결하기'}
        </button>
      </div>
    </section>
  )
}
