import type { CaseView } from './emergencyCase'
import type { HospitalSummary, StatusInput } from './hospitalStatus'

export type DeskFailure = 'unavailable' | 'wrong-code' | 'not-found' | 'network' | 'unknown'
export type DeskResult<T> = { ok: true; value: T } | { ok: false; reason: DeskFailure }

/**
 * 병원 웹 저장소. 화면은 이 인터페이스만 안다 (보호자 웹의 PetProfileRepository와 같은 방식).
 * - 메모리 구현: 서버 없이 이 브라우저에서만 동작 (시연 병원 5곳 + 예시 환자 1건)
 * - Firestore 구현: 병원별 코드로 직원 등록 → 실시간으로 상태와 환자 건을 받는다
 * 실패는 예외가 아니라 결과 값으로 돌려준다.
 */
export interface HospitalDesk {
  /** 서버 없이 이 브라우저에서만 동작하는지 (화면에 알린다) */
  readonly isLocalOnly: boolean
  /** 병원 고르기 화면의 목록 */
  listHospitals(): Promise<DeskResult<HospitalSummary[]>>
  /** 병원 화면을 연다. 서버 구현은 joinCode로 이 브라우저를 그 병원 직원으로 등록한다 */
  open(hospitalId: string, joinCode: string | null): Promise<DeskResult<void>>
  /** 병원 문서를 실시간으로 받는다. 반환값을 부르면 그만 받는다 */
  watchHospital(hospitalId: string, onChange: (hospital: HospitalSummary | null) => void): () => void
  /** 들어온 환자 건을 실시간으로 받는다 */
  watchCases(hospitalId: string, onChange: (cases: CaseView[]) => void): () => void
  /** 상태를 알린다 (updatedAt은 저장소가 채운다) */
  updateStatus(hospitalId: string, input: StatusInput): Promise<DeskResult<void>>
  /** [완료]: 환자 건을 지운다 */
  completeCase(hospitalId: string, caseId: string): Promise<DeskResult<void>>
  /**
   * [카드 확인] (계획 013 U1): acknowledgedAt = 서버 시각. 이미 확인된 건은 처음 시각 그대로 둔다.
   * 차는 안내 중에 이 시각을 읽어 "병원이 진료 카드를 확인했어요"를 보인다.
   */
  acknowledgeCase(hospitalId: string, caseId: string): Promise<DeskResult<void>>
}
