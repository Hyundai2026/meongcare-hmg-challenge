/**
 * 보호자 계정과 차 연결 (계획 014). 화면은 이 인터페이스만 안다. 구현은 data/firestoreOwnerStore.ts 하나다.
 * 보호자는 Firebase 익명 계정이다 (사용자 결정 U1). 동의하기 전에는 계정을 만들지 않는다.
 * 실패는 예외가 아니라 결과 값이다.
 */
import type { PetProfileRepository } from '../petProfileRepository'

export type AccountStatus = 'before-consent' | 'consented' | 'network'

export type ConsentResult = { ok: true } | { ok: false; reason: 'network' | 'unknown' }

export type LinkCarResult =
  | { ok: true }
  /** 코드를 못 찾음·만료·이미 씀 (어느 쪽인지 알리지 않는다, 계획 014 K6) */
  | { ok: false; reason: 'not-found' | 'no-profile' | 'network' | 'unknown' }

export interface OwnerAccount {
  /** 이 브라우저가 기억한 계정이 동의했는지. 계정이 없으면 before-consent */
  status(): Promise<AccountStatus>
  /** 동의: 익명 계정을 만들고 동의 문서를 쓴다 (이미 있으면 그대로) */
  consent(): Promise<ConsentResult>
  /** 차 화면의 코드로 연결한다. 코드는 normalizeLinkCode를 거친 값이다 */
  linkCar(code: string): Promise<LinkCarResult>
}

/** 서버 모드에서 화면이 쓰는 것 */
export interface OwnerServer {
  /** PC의 Firebase 에뮬레이터면 true (안내 문구에 "시연용 로컬 서버") */
  emulator: boolean
  account: OwnerAccount
  /** 동의한 뒤의 프로필 저장소 (Firestore) */
  repository: PetProfileRepository
}
