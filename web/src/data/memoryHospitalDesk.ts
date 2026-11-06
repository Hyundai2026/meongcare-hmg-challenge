/**
 * 서버 없는 병원 웹 저장소 (메모리, 이 브라우저 탭에서만). 앱의 Fake 자리다.
 * 시연 병원 5곳과, 병원마다 예시 환자 1건("보리", 지금 + 12분 도착)으로 시작한다.
 * 새로 고치면 처음 값으로 돌아간다. 차 앱의 카드는 들어오지 않는다.
 */
import { caseFromJson, type CaseView } from '../domain/hospital/emergencyCase'
import type { DeskResult, HospitalDesk } from '../domain/hospital/hospitalDesk'
import { hospitalFromJson, type HospitalSummary, type StatusInput } from '../domain/hospital/hospitalStatus'
import { EXAMPLE_CASE_DOCUMENT, SAMPLE_HOSPITALS } from './sampleHospitals'

const EXAMPLE_ETA_MINUTES = 12

export function createMemoryHospitalDesk(now: () => Date): HospitalDesk {
  const start = now()
  const hospitals = new Map<string, HospitalSummary>()
  for (const entry of SAMPLE_HOSPITALS) {
    const updatedAt = new Date(start.getTime() - entry.seedUpdatedMinutesAgo * 60_000)
    const hospital = hospitalFromJson(entry.id, { ...entry.document, status: { ...entry.document.status, updatedAt } })
    if (hospital !== null) hospitals.set(entry.id, hospital)
  }

  const cases = new Map<string, CaseView[]>()
  const hospitalListeners = new Map<string, Set<(h: HospitalSummary | null) => void>>()
  const caseListeners = new Map<string, Set<(c: CaseView[]) => void>>()

  function casesOf(hospitalId: string): CaseView[] {
    let list = cases.get(hospitalId)
    if (list === undefined) {
      const at = now()
      const example = caseFromJson(`example-${hospitalId}`, {
        ...EXAMPLE_CASE_DOCUMENT,
        createdAt: at,
        etaAt: new Date(at.getTime() + EXAMPLE_ETA_MINUTES * 60_000),
        etaUpdatedAt: at,
      })
      list = [{ ...example, isExample: true }]
      cases.set(hospitalId, list)
    }
    return list
  }

  function emit<T>(listeners: Map<string, Set<(v: T) => void>>, id: string, value: T) {
    listeners.get(id)?.forEach((listener) => listener(value))
  }

  function subscribe<T>(listeners: Map<string, Set<(v: T) => void>>, id: string, listener: (v: T) => void): () => void {
    let set = listeners.get(id)
    if (set === undefined) {
      set = new Set()
      listeners.set(id, set)
    }
    set.add(listener)
    return () => {
      set.delete(listener)
    }
  }

  const ok = <T>(value: T): DeskResult<T> => ({ ok: true, value })

  return {
    isLocalOnly: true,

    async listHospitals() {
      return ok([...hospitals.values()])
    },

    async open(hospitalId) {
      // 메모리 구현은 코드를 보지 않는다 (서버가 없어 지킬 데이터가 없다)
      return hospitals.has(hospitalId) ? ok(undefined) : { ok: false, reason: 'not-found' }
    },

    watchHospital(hospitalId, onChange) {
      const unsubscribe = subscribe(hospitalListeners, hospitalId, onChange)
      onChange(hospitals.get(hospitalId) ?? null)
      return unsubscribe
    },

    watchCases(hospitalId, onChange) {
      const unsubscribe = subscribe(caseListeners, hospitalId, onChange)
      onChange([...casesOf(hospitalId)])
      return unsubscribe
    },

    async updateStatus(hospitalId, input: StatusInput) {
      const hospital = hospitals.get(hospitalId)
      if (hospital === undefined) return { ok: false, reason: 'not-found' }
      const updated: HospitalSummary = { ...hospital, status: { ...input, updatedAt: now() } }
      hospitals.set(hospitalId, updated)
      emit(hospitalListeners, hospitalId, updated)
      return ok(undefined)
    },

    async completeCase(hospitalId, caseId) {
      const list = casesOf(hospitalId).filter((c) => c.id !== caseId)
      cases.set(hospitalId, list)
      emit(caseListeners, hospitalId, [...list])
      return ok(undefined)
    },
  }
}
