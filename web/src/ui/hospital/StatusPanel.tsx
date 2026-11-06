/**
 * 병원 상태 알리기: 예상 대기 시간, 혼잡도, 응급 접수. 큰 버튼으로 고르고 [병원 상태 알리기]로 보낸다.
 */
import { useState } from 'react'
import { WAIT_PRESETS, WAIT_STEP, agoText, clampWait, clockText, isStale } from '../../domain/hospital/deskText'
import type { HospitalDesk } from '../../domain/hospital/hospitalDesk'
import { CONGESTIONS, CONGESTION_TEXT, type HospitalSummary, type StatusInput } from '../../domain/hospital/hospitalStatus'

const DEFAULT_INPUT: StatusInput = { waitMinutes: 10, congestion: 'normal', acceptingEmergency: true }

export function StatusPanel({ desk, hospital, now }: { desk: HospitalDesk; hospital: HospitalSummary; now: Date }) {
  const status = hospital.status
  const [draft, setDraft] = useState<StatusInput>(() =>
    status === null
      ? DEFAULT_INPUT
      : { waitMinutes: status.waitMinutes, congestion: status.congestion, acceptingEmergency: status.acceptingEmergency },
  )
  const [sending, setSending] = useState(false)
  const [failed, setFailed] = useState(false)

  const send = async () => {
    setSending(true)
    setFailed(false)
    const result = await desk.updateStatus(hospital.id, draft)
    setSending(false)
    if (!result.ok) setFailed(true)
  }

  return (
    <section className="hospital-panel" aria-labelledby="status-title">
      <h2 id="status-title" className="hospital-panel__title">
        병원 상태 알리기
      </h2>

      <div className="hospital-current">
        {status === null ? (
          <p>아직 알린 상태가 없어요. 차 앱에는 "대기 정보 없음"으로 보여요.</p>
        ) : (
          <>
            <p className="hospital-current__value">
              대기 {status.waitMinutes}분 · {CONGESTION_TEXT[status.congestion]} · 응급 접수{' '}
              {status.acceptingEmergency ? '받음' : '안 받음'}
            </p>
            <p className="hospital-muted">
              마지막으로 알림 {clockText(status.updatedAt)} ({agoText(status.updatedAt, now)})
            </p>
            {isStale(status.updatedAt, now) && (
              <p className="banner banner--error">30분이 지나 차 앱에서 "대기 정보 지남"으로 보여요. 다시 알려 주세요.</p>
            )}
          </>
        )}
      </div>

      <fieldset className="hospital-field">
        <legend>예상 대기 시간</legend>
        <div className="hospital-wait">
          <button
            type="button"
            className="hospital-option hospital-option--step"
            onClick={() => setDraft({ ...draft, waitMinutes: clampWait(draft.waitMinutes - WAIT_STEP) })}
          >
            −{WAIT_STEP}
          </button>
          <output className="hospital-wait__value" aria-live="polite">
            {draft.waitMinutes}분
          </output>
          <button
            type="button"
            className="hospital-option hospital-option--step"
            onClick={() => setDraft({ ...draft, waitMinutes: clampWait(draft.waitMinutes + WAIT_STEP) })}
          >
            +{WAIT_STEP}
          </button>
        </div>
        <div className="hospital-options">
          {WAIT_PRESETS.map((minutes) => (
            <button
              key={minutes}
              type="button"
              aria-pressed={draft.waitMinutes === minutes}
              className="hospital-option"
              onClick={() => setDraft({ ...draft, waitMinutes: minutes })}
            >
              {minutes}분
            </button>
          ))}
        </div>
      </fieldset>

      <fieldset className="hospital-field">
        <legend>혼잡도</legend>
        <div className="hospital-options">
          {CONGESTIONS.map((congestion) => (
            <button
              key={congestion}
              type="button"
              aria-pressed={draft.congestion === congestion}
              className="hospital-option hospital-option--wide"
              onClick={() => setDraft({ ...draft, congestion })}
            >
              {CONGESTION_TEXT[congestion]}
            </button>
          ))}
        </div>
      </fieldset>

      <fieldset className="hospital-field">
        <legend>응급 접수</legend>
        <div className="hospital-options">
          {[true, false].map((accepting) => (
            <button
              key={String(accepting)}
              type="button"
              aria-pressed={draft.acceptingEmergency === accepting}
              className="hospital-option hospital-option--wide"
              onClick={() => setDraft({ ...draft, acceptingEmergency: accepting })}
            >
              {accepting ? '받음' : '안 받음'}
            </button>
          ))}
        </div>
      </fieldset>

      {failed && <p className="banner banner--error">알리지 못했어요. 다시 눌러 주세요.</p>}
      <button type="button" className="button button--primary button--block hospital-send" disabled={sending} onClick={send}>
        {sending ? '알리는 중…' : '병원 상태 알리기'}
      </button>
    </section>
  )
}
