/**
 * 병원 웹의 문구와 순서 규칙 (순수 함수, 시각은 인자로 받는다).
 */
import { WAIT_MINUTES_MAX } from './hospitalStatus'
import type { CaseView } from './emergencyCase'

/** 차 앱이 "대기 정보 지남"으로 보는 기준 (docs/data/hospital-status.md "오래된 정보") */
export const STALE_AFTER_MINUTES = 30

/** 대기 시간 빠른 버튼 */
export const WAIT_PRESETS: readonly number[] = [0, 10, 20, 30, 45, 60, 90, 120]

/** 대기 시간 [−5] [+5] */
export const WAIT_STEP = 5

function pad2(n: number): string {
  return String(n).padStart(2, '0')
}

/** 기기 시간대의 HH:mm */
export function clockText(date: Date): string {
  return `${pad2(date.getHours())}:${pad2(date.getMinutes())}`
}

/**
 * 환자 건의 도착 예정 문구.
 * - "도착 예정 14:32 · 약 12분 후" (남은 시간은 병원 PC 시계로 다시 계산한다)
 * - 예정 시각이 지났으면 "도착 예정 14:32 · 곧 도착"
 * - 예정 시각이 없으면 "경로를 찾는 중"
 */
export function etaText(etaAt: Date | null, now: Date): string {
  if (etaAt === null) return '경로를 찾는 중'
  const minutes = Math.ceil((etaAt.getTime() - now.getTime()) / 60_000)
  const rest = minutes >= 1 ? `약 ${minutes}분 후` : '곧 도착'
  return `도착 예정 ${clockText(etaAt)} · ${rest}`
}

/** 알린 지 얼마나 됐는지: "방금", "3분 전", "1시간 5분 전" */
export function agoText(at: Date, now: Date): string {
  const minutes = Math.floor((now.getTime() - at.getTime()) / 60_000)
  if (minutes < 1) return '방금'
  if (minutes < 60) return `${minutes}분 전`
  const hours = Math.floor(minutes / 60)
  const rest = minutes % 60
  return rest === 0 ? `${hours}시간 전` : `${hours}시간 ${rest}분 전`
}

/** 알린 지 30분이 넘었는지 (차 앱에서 "대기 정보 지남"으로 보인다) */
export function isStale(at: Date, now: Date): boolean {
  return now.getTime() - at.getTime() > STALE_AFTER_MINUTES * 60_000
}

/** 대기 분을 0~600 정수로 맞춘다 */
export function clampWait(minutes: number): number {
  if (!Number.isFinite(minutes)) return 0
  return Math.min(WAIT_MINUTES_MAX, Math.max(0, Math.round(minutes)))
}

const STATUS_ORDER: Record<CaseView['status'], number> = { arrived: 0, en_route: 1, cancelled: 2 }

/**
 * 들어온 환자 목록 순서: 도착한 건 → 오는 중(도착 예정이 빠른 순, 예정 없음은 뒤) → 취소는 맨 아래.
 * 같은 묶음 안에서 같으면 보낸 시각 → id 순 (같은 입력이면 항상 같은 순서).
 */
export function sortCases(cases: readonly CaseView[]): CaseView[] {
  const time = (d: Date | null) => (d === null ? Number.POSITIVE_INFINITY : d.getTime())
  return [...cases].sort(
    (a, b) =>
      STATUS_ORDER[a.status] - STATUS_ORDER[b.status] ||
      time(a.etaAt) - time(b.etaAt) ||
      time(a.createdAt) - time(b.createdAt) ||
      (a.id < b.id ? -1 : a.id > b.id ? 1 : 0),
  )
}
