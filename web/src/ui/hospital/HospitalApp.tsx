/**
 * 병원 웹 (시연용, /hospital/). 병원 직원이 예상 대기 시간·혼잡도·응급 접수 여부를 알리고,
 * 차에서 보낸 진료 카드와 도착 예정 시간을 실시간으로 본다 (계획 012 "설계 결정 7").
 *
 * 화면은 저장소 인터페이스(HospitalDesk)만 안다. 주소: /hospital/?h=<병원 id>&code=<병원 코드>
 */
import { useEffect, useState } from 'react'
import { APP_NAME } from '../../config'
import { newCaseCount, pageTitle } from '../../domain/hospital/caseAck'
import type { CaseView } from '../../domain/hospital/emergencyCase'
import type { DeskFailure, HospitalDesk } from '../../domain/hospital/hospitalDesk'
import type { HospitalSummary } from '../../domain/hospital/hospitalStatus'
import { CONNECTION_TEXT, type DeskConnection } from '../../domain/hospital/serverInfo'
import { PawIcon } from '../PawIcon'
import { CasesPanel } from './CasesPanel'
import { StatusPanel } from './StatusPanel'

/** 30초마다 지금 시각을 바꾼다 ("약 N분 후", "N분 전" 다시 계산) */
export function useNow(now: () => Date, everyMs = 30_000): Date {
  const [value, setValue] = useState(now)
  useEffect(() => {
    const timer = window.setInterval(() => setValue(now()), everyMs)
    return () => window.clearInterval(timer)
  }, [now, everyMs])
  return value
}

interface Location {
  hospitalId: string | null
  joinCode: string | null
}

function readLocation(): Location {
  const params = new URLSearchParams(window.location.search)
  return { hospitalId: params.get('h'), joinCode: params.get('code') }
}

export function HospitalApp({ desk, now }: { desk: HospitalDesk; now: () => Date }) {
  const [location, setLocation] = useState(readLocation)
  const [connection, setConnection] = useState<DeskConnection | null>(null)

  useEffect(() => {
    const onPop = () => setLocation(readLocation())
    window.addEventListener('popstate', onPop)
    return () => window.removeEventListener('popstate', onPop)
  }, [])

  const go = (hospitalId: string | null) => {
    const url = new URL(window.location.href)
    url.search = ''
    if (hospitalId !== null) url.searchParams.set('h', hospitalId)
    window.history.pushState(null, '', url)
    setLocation({ hospitalId, joinCode: null })
  }

  return (
    <div className="hospital-page">
      <header className="hospital-header">
        <PawIcon className="header__mark" />
        <span className="hospital-header__name">{APP_NAME} 병원</span>
        {location.hospitalId !== null && (
          <button type="button" className="button button--pill hospital-header__switch" onClick={() => go(null)}>
            병원 바꾸기
          </button>
        )}
      </header>
      <div className="hospital-notices" role="note">
        <p>시연용 화면이에요. 실제 병원이 아니에요.</p>
        {desk.isLocalOnly && <p>서버 없이 이 브라우저에서만 동작해요. 차 앱의 카드는 들어오지 않아요.</p>}
        {desk.server !== null && (
          <p className="hospital-server">
            서버: {desk.server.label}
            {connection !== null && connection !== 'local' && location.hospitalId !== null && (
              <span className={`hospital-connection hospital-connection--${connection}`}> · {CONNECTION_TEXT[connection]}</span>
            )}
          </p>
        )}
        {desk.server?.warning != null && <p className="hospital-server__warning">{desk.server.warning}</p>}
      </div>
      {location.hospitalId === null ? (
        <HospitalPicker desk={desk} onPick={go} />
      ) : (
        <HospitalDeskView
          key={location.hospitalId}
          desk={desk}
          hospitalId={location.hospitalId}
          joinCode={location.joinCode}
          now={now}
          onConnection={setConnection}
        />
      )}
    </div>
  )
}

function HospitalPicker({ desk, onPick }: { desk: HospitalDesk; onPick: (id: string) => void }) {
  const [hospitals, setHospitals] = useState<HospitalSummary[] | null>(null)
  const [failed, setFailed] = useState(false)

  useEffect(() => {
    let alive = true
    void desk.listHospitals().then((result) => {
      if (!alive) return
      if (result.ok) setHospitals(result.value)
      else setFailed(true)
    })
    return () => {
      alive = false
    }
  }, [desk])

  return (
    <main className="hospital-content">
      <h1 className="hospital-title">어느 병원 화면을 열까요?</h1>
      {failed && <p className="banner banner--error">병원 목록을 받지 못했어요. 새로 고쳐 주세요.</p>}
      {hospitals === null && !failed && <p className="hospital-muted">병원 목록을 받는 중…</p>}
      <div className="hospital-picker">
        {hospitals?.map((hospital) => (
          <button key={hospital.id} type="button" className="hospital-picker__item" onClick={() => onPick(hospital.id)}>
            <span className="hospital-picker__name">{hospital.name}</span>
            {hospital.isSample && <span className="hospital-chip">시연용</span>}
          </button>
        ))}
      </div>
      {!desk.isLocalOnly && <p className="hospital-muted">서버에 연결된 화면은 병원용 링크(코드 포함)로 열어야 상태를 알릴 수 있어요.</p>}
    </main>
  )
}

const OPEN_FAILURE_TEXT: Record<DeskFailure, string> = {
  'wrong-code': '병원용 링크(코드 포함)로 열어 주세요. 코드가 없거나 맞지 않아요.',
  'not-found': '없는 병원이에요. 병원을 다시 골라 주세요.',
  network: '서버에 연결하지 못했어요. 인터넷 연결을 확인하고 새로 고쳐 주세요.',
  unavailable: '서버에 연결하지 못했어요. 새로 고쳐 주세요.',
  unknown: '병원 화면을 열지 못했어요. 새로 고쳐 주세요.',
}

function HospitalDeskView({
  desk,
  hospitalId,
  joinCode,
  now,
  onConnection,
}: {
  desk: HospitalDesk
  hospitalId: string
  joinCode: string | null
  now: () => Date
  onConnection: (state: DeskConnection | null) => void
}) {
  const [opened, setOpened] = useState<'opening' | 'open' | DeskFailure>('opening')
  const [hospital, setHospital] = useState<HospitalSummary | null | undefined>(undefined)
  const [cases, setCases] = useState<CaseView[]>([])
  const current = useNow(now)
  const newCount = newCaseCount(cases)
  const hospitalName = opened === 'open' && hospital != null ? hospital.name : null

  // 탭 제목에 새 환자 수를 보인다 (다른 탭을 보고 있어도 알 수 있게, 계획 013). 화면을 떠나면 원래 제목으로
  useEffect(() => {
    const original = document.title
    return () => {
      document.title = original
    }
  }, [])
  useEffect(() => {
    document.title = pageTitle(APP_NAME, hospitalName, newCount)
  }, [hospitalName, newCount])

  useEffect(() => {
    let alive = true
    let stops: (() => void)[] = []
    void desk.open(hospitalId, joinCode).then((result) => {
      if (!alive) return
      if (!result.ok) {
        setOpened(result.reason)
        return
      }
      setOpened('open')
      stops = [
        desk.watchHospital(hospitalId, setHospital),
        desk.watchCases(hospitalId, setCases),
        desk.watchConnection(hospitalId, onConnection),
      ]
    })
    return () => {
      alive = false
      stops.forEach((stop) => stop())
      onConnection(null)
    }
  }, [desk, hospitalId, joinCode, onConnection])

  if (opened === 'opening') return <main className="hospital-content hospital-muted">병원 화면을 여는 중…</main>
  if (opened !== 'open') {
    return (
      <main className="hospital-content">
        <p className="banner banner--error">{OPEN_FAILURE_TEXT[opened]}</p>
      </main>
    )
  }
  if (hospital === undefined) return <main className="hospital-content hospital-muted">병원 정보를 받는 중…</main>
  if (hospital === null) {
    return (
      <main className="hospital-content">
        <p className="banner banner--error">{OPEN_FAILURE_TEXT['not-found']}</p>
      </main>
    )
  }

  return (
    <main className="hospital-content">
      <h1 className="hospital-title">
        {hospital.name}
        {hospital.isSample && <span className="hospital-chip">시연용</span>}
      </h1>
      <div className="hospital-desk">
        <StatusPanel desk={desk} hospital={hospital} now={current} />
        <CasesPanel desk={desk} hospitalId={hospital.id} cases={cases} now={current} />
      </div>
    </main>
  )
}
