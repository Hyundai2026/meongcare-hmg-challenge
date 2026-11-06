/**
 * 들어온 환자 목록 (실시간). 차에서 [출발]하면 진료 카드와 도착 예정 시간이 뜬다.
 * - 확인 전인 건은 "새 환자"로 강조한다. [카드 확인]을 누르면 차 화면에 "병원이 진료 카드를 확인했어요"가 뜬다 (계획 013).
 * - [완료]를 누르면 확인한 뒤 지운다 (보관하지 않는다).
 */
import { useState } from 'react'
import { ageLabel } from '../../domain/age'
import { ackText, isNewCase } from '../../domain/hospital/caseAck'
import { CASE_STATUS_TEXT, type CaseView } from '../../domain/hospital/emergencyCase'
import { etaText, sortCases } from '../../domain/hospital/deskText'
import type { DeskResult, HospitalDesk } from '../../domain/hospital/hospitalDesk'
import { localDateOf } from '../../domain/localDate'
import { SEX_SYMBOL, medicationText } from '../profileText'

export function CasesPanel({
  desk,
  hospitalId,
  cases,
  now,
}: {
  desk: HospitalDesk
  hospitalId: string
  cases: CaseView[]
  now: Date
}) {
  const sorted = sortCases(cases)
  return (
    <section className="hospital-panel" aria-labelledby="cases-title">
      <h2 id="cases-title" className="hospital-panel__title">
        들어온 환자 <span className="hospital-count">{cases.length}</span>
      </h2>
      {sorted.length === 0 && <p className="hospital-muted">아직 들어온 환자가 없어요.</p>}
      <ul className="hospital-cases">
        {sorted.map((item) => (
          <li key={item.id}>
            <CaseCard
              item={item}
              now={now}
              onComplete={() => desk.completeCase(hospitalId, item.id)}
              onAcknowledge={() => desk.acknowledgeCase(hospitalId, item.id)}
            />
          </li>
        ))}
      </ul>
    </section>
  )
}

function CaseCard({
  item,
  now,
  onComplete,
  onAcknowledge,
}: {
  item: CaseView
  now: Date
  onComplete: () => Promise<unknown>
  onAcknowledge: () => Promise<DeskResult<void>>
}) {
  const [confirming, setConfirming] = useState(false)
  const [busy, setBusy] = useState(false)
  const [ack, setAck] = useState<'idle' | 'busy' | 'failed'>('idle')

  const acknowledge = async () => {
    setAck('busy')
    const result = await onAcknowledge()
    // 성공하면 실시간 스냅샷이 확인 시각을 가져온다
    setAck(result.ok ? 'idle' : 'failed')
  }

  const complete = async () => {
    setBusy(true)
    await onComplete()
    setBusy(false)
    setConfirming(false)
  }

  const completeActions = confirming ? (
    <div className="hospital-case__confirm">
      <span>목록에서 지울까요?</span>
      <button type="button" className="button button--primary" disabled={busy} onClick={complete}>
        지우기
      </button>
      <button type="button" className="button button--secondary" onClick={() => setConfirming(false)}>
        취소
      </button>
    </div>
  ) : (
    <button type="button" className="button button--secondary" onClick={() => setConfirming(true)}>
      완료
    </button>
  )

  if (!item.readable) {
    return (
      <article className="hospital-case">
        <p>읽지 못한 환자 건이에요 (형식 버전이 달라요).</p>
        {completeActions}
      </article>
    )
  }

  const isNew = isNewCase(item)
  // 취소된 건에는 [카드 확인]이 없다. 이미 확인됐으면 시각을 보인다
  const ackArea =
    item.acknowledgedAt !== null ? (
      <p className="hospital-case__ack">✓ {ackText(item.acknowledgedAt)}</p>
    ) : item.status === 'cancelled' ? null : (
      <button type="button" className="button button--primary hospital-ack-button" disabled={ack === 'busy'} onClick={acknowledge}>
        {ack === 'busy' ? '확인 중…' : '카드 확인'}
      </button>
    )

  const pet = item.pet
  const age = pet === null ? null : ageLabel(pet.birthDate, localDateOf(now))
  const none = '등록된 정보 없음'
  const rows: [string, string][] =
    pet === null
      ? []
      : [
          ['기존 병력', pet.medical.conditions.join(', ') || none],
          ['알레르기', pet.medical.allergies.join(', ') || none],
          ['복용 약', pet.medical.medications.map(medicationText).join(', ') || none],
        ]

  return (
    <article className={`hospital-case hospital-case--${item.status}${isNew ? ' hospital-case--new' : ''}`}>
      <header className="hospital-case__head">
        <div>
          <p className="hospital-case__name">
            {pet === null ? '반려견 정보를 받지 못했어요' : pet.name}
            {pet?.sex != null && <span className="hospital-case__sex"> {SEX_SYMBOL[pet.sex]}</span>}
            {item.isExample === true && <span className="hospital-chip">예시</span>}
          </p>
          {pet !== null && (
            <p className="hospital-muted">
              {[age === null ? null : pet.birthDateIsEstimated ? `${age}(추정)` : age, pet.breed].filter(Boolean).join(' · ')}
            </p>
          )}
        </div>
        <div className="hospital-case__badges">
          {isNew && <span className="hospital-new-tag">새 환자</span>}
          <span className={`hospital-badge hospital-badge--${item.status}`}>{CASE_STATUS_TEXT[item.status]}</span>
        </div>
      </header>
      {item.status === 'en_route' && <p className="hospital-case__eta">{etaText(item.etaAt, now)}</p>}
      <p className="hospital-case__symptoms">
        <span className="hospital-muted">주요 증상</span>{' '}
        <strong>{item.symptoms.length === 0 ? '고른 증상 없음' : item.symptoms.map((s) => s.text).join(', ')}</strong>
      </p>
      {rows.length > 0 && (
        <dl className="hospital-case__rows">
          {rows.map(([label, value]) => (
            <div key={label}>
              <dt>{label}</dt>
              <dd>{value}</dd>
            </div>
          ))}
        </dl>
      )}
      <p className="hospital-case__disclaimer">※ 보호자가 등록한 의료정보와 차에서 고른 증상이에요. 의료 진단이 아니에요.</p>
      {ack === 'failed' && item.acknowledgedAt === null && (
        <p className="hospital-case__ack-error" role="alert">
          확인을 저장하지 못했어요. 다시 눌러 주세요.
        </p>
      )}
      <div className="hospital-case__actions">
        {ackArea}
        {completeActions}
      </div>
    </article>
  )
}
