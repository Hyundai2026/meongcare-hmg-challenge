/**
 * 보호자 웹의 서버 모드 문구와 차 연결 상태 (계획 014). React를 모르는 순수 함수다.
 */
import type { MigrationOutcome } from '../domain/owner/migration'
import type { LinkedCar } from '../domain/owner/ownerAccount'

/**
 * 서버 모드로 들어갈 때 처음 화면 위에 보이는 한 줄.
 * 반려견을 등록하기 전이면 "이제 차와 연결할 수 있어요"라고 하지 않는다 (연결하려면 등록이 먼저다).
 * @param afterConsent 방금 동의했으면 true. 다시 열었을 때는 옮긴 경우와 실패만 알린다
 * @param hasProfile 서버 모드로 들어간 뒤 보일 반려견이 있는지
 */
export function ownerBannerOf(outcome: MigrationOutcome, afterConsent: boolean, hasProfile: boolean): string | null {
  switch (outcome.kind) {
    case 'moved':
      return `이 브라우저의 '${outcome.profile.name}' 정보를 서버로 옮겼어요.`
    case 'server-kept':
      return afterConsent ? '서버에 이미 저장된 정보가 있어 그 정보를 보여 줘요. 이 브라우저의 정보는 그대로 남아 있어요.' : null
    case 'failed':
      return '이 브라우저의 정보를 서버로 옮기지 못했어요. 브라우저의 정보는 그대로 있어요. 잠시 뒤 새로 고치면 다시 옮겨요.'
    case 'nothing':
      if (!afterConsent) return null
      return hasProfile ? '서버에 저장했어요. 이제 차와 연결할 수 있어요.' : '서버에 저장하기로 했어요. 반려견을 등록하면 차와 연결할 수 있어요.'
  }
}

/** 프로필 아래 [차와 연결] 카드의 상태 */
export type CarLinkStatus =
  /** 아직 모른다 (읽는 중). 카드를 그리지 않는다 */
  | { kind: 'unknown' }
  /** 연결한 차가 없다 (또는 동의 전, 읽지 못함) → [차와 연결하기] */
  | { kind: 'not-linked' }
  /** 연결한 차가 있다 → "차와 연결됨" + 가장 최근 연결 시각 */
  | { kind: 'linked'; count: number; lastLinkedAt: string | null }

/**
 * @param cars 연결된 차 목록. undefined = 아직 읽는 중, null = 읽지 못함 (연결은 다시 할 수 있게 연결 전으로 본다)
 */
export function carLinkStatusOf(cars: LinkedCar[] | null | undefined): CarLinkStatus {
  if (cars === undefined) return { kind: 'unknown' }
  if (cars === null || cars.length === 0) return { kind: 'not-linked' }
  const times = cars.map((c) => c.linkedAt).filter((t): t is string => t !== null).sort()
  return { kind: 'linked', count: cars.length, lastLinkedAt: times.length > 0 ? times[times.length - 1] : null }
}
