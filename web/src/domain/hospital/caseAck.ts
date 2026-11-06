/**
 * 병원 [카드 확인]과 새 환자 표시 (계획 013 U1). 순수 함수, 시각은 인자로 받는다.
 * 확인 시각(acknowledgedAt)의 쓰기 규칙: docs/data/emergency-case.md "카드 확인".
 */
import { clockText } from './deskText'
import type { CaseView } from './emergencyCase'

/** 새 환자: 읽을 수 있고, 취소되지 않았고, 아직 확인 전인 건 */
export function isNewCase(item: CaseView): boolean {
  return item.readable && item.status !== 'cancelled' && item.acknowledgedAt === null
}

export function newCaseCount(cases: readonly CaseView[]): number {
  return cases.filter(isNewCase).length
}

/**
 * 탭 제목. 다른 탭을 보고 있어도 새 환자를 알 수 있게 한다.
 * - 새 환자가 있으면 "(2) 새 환자 · 시연용 바른 동물병원 · MeongCare 병원"
 * - 없으면 "시연용 바른 동물병원 · MeongCare 병원", 병원을 고르기 전이면 "MeongCare 병원"
 */
export function pageTitle(appName: string, hospitalName: string | null, newCount: number): string {
  const base = `${appName} 병원`
  if (hospitalName === null) return base
  const named = `${hospitalName} · ${base}`
  return newCount > 0 ? `(${newCount}) 새 환자 · ${named}` : named
}

/** "카드 확인함 14:05" (병원 PC 시간대, 분 아래 버림) */
export function ackText(at: Date): string {
  return `카드 확인함 ${clockText(at)}`
}
