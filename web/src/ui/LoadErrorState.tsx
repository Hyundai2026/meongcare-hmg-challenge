import type { LoadFailure } from '../domain/petProfileRepository'
import { LOAD_FAILURE_MESSAGES } from './messages'

export function LoadErrorState({ reason, onStartNew }: { reason: LoadFailure; onStartNew: () => void }) {
  return (
    <section className="card empty" role="alert">
      <h1 className="empty__title">저장된 정보를 읽지 못했어요</h1>
      <p className="empty__body">{LOAD_FAILURE_MESSAGES[reason]}</p>
      {reason !== 'unavailable' && (
        <button type="button" className="button button--primary button--block" onClick={onStartNew}>
          새로 등록하기
        </button>
      )}
    </section>
  )
}
