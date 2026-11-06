/**
 * 차에서 병원으로 보낸 환자 건. 형식의 기준은 docs/data/emergency-case.md다.
 * card.pet은 pet-profile 읽기 규칙으로 읽는다 (사진·형식 버전은 들어 있지 않다).
 */
import type { PetProfile } from '../petProfile'
import { petProfileFromJson } from '../petProfileJson'
import { dateFrom } from './hospitalStatus'

export const CASE_SCHEMA_VERSION = 1

export type CaseStatus = 'en_route' | 'arrived' | 'cancelled'

export interface CaseSymptom {
  code: string
  /** 화면에 보일 글자. label이 없으면 code */
  text: string
}

export interface CaseView {
  id: string
  /** false면 형식 버전을 몰라 읽지 못한 건이다. 지우기만 된다 */
  readable: boolean
  status: CaseStatus
  /** 남은 분. null이면 경로를 찾는 중 */
  remainingMinutes: number | null
  /** 도착 예정 시각 (차 시계). null이면 경로를 찾는 중 */
  etaAt: Date | null
  createdAt: Date | null
  /** null이면 반려견 정보를 받지 못했다 */
  pet: PetProfile | null
  symptoms: CaseSymptom[]
  acknowledgedAt: Date | null
  /** 예시 건 (메모리 모드의 시연용 예시) */
  isExample?: boolean
}

function isObject(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}

function caseStatusOf(value: unknown): CaseStatus {
  // 모르는 값은 "오는 중"으로 보인다 (docs/data/emergency-case.md 읽는 쪽 규칙)
  return value === 'arrived' || value === 'cancelled' ? value : 'en_route'
}

function petOf(value: unknown): PetProfile | null {
  if (!isObject(value)) return null
  const parsed = petProfileFromJson({ ...value, schemaVersion: 1, photoDataUrl: null })
  return parsed.ok ? parsed.profile : null
}

function symptomsOf(value: unknown): CaseSymptom[] {
  if (!Array.isArray(value)) return []
  const out: CaseSymptom[] = []
  for (const item of value) {
    if (!isObject(item) || typeof item.code !== 'string') continue
    const label = typeof item.label === 'string' && item.label.trim().length > 0 ? item.label : item.code
    out.push({ code: item.code, text: label })
  }
  return out
}

/** 환자 건 문서 → CaseView. 형식 버전이 다르면 readable=false로 한 줄만 보인다. */
export function caseFromJson(id: string, value: unknown): CaseView {
  const unreadable: CaseView = {
    id,
    readable: false,
    status: 'en_route',
    remainingMinutes: null,
    etaAt: null,
    createdAt: null,
    pet: null,
    symptoms: [],
    acknowledgedAt: null,
  }
  if (!isObject(value) || value.schemaVersion !== CASE_SCHEMA_VERSION) return unreadable
  const remaining = value.remainingMinutes
  const card = isObject(value.card) ? value.card : {}
  return {
    id,
    readable: true,
    status: caseStatusOf(value.status),
    remainingMinutes: typeof remaining === 'number' && Number.isFinite(remaining) ? remaining : null,
    etaAt: dateFrom(value.etaAt),
    createdAt: dateFrom(value.createdAt),
    pet: petOf(card.pet),
    symptoms: symptomsOf(card.symptoms),
    acknowledgedAt: dateFrom(value.acknowledgedAt),
  }
}

export const CASE_STATUS_TEXT: Record<CaseStatus, string> = {
  en_route: '오는 중',
  arrived: '도착했어요',
  cancelled: '취소됨',
}
