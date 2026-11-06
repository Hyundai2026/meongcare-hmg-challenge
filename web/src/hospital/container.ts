/**
 * 병원 웹의 저장소를 고른다 (앱의 AppContainer 자리).
 * 지금은 메모리 구현뿐이다. Firebase 설정(.env.local의 VITE_FIREBASE_*)이 생기면 Firestore 구현을 고른다 (계획 012 단계 11).
 */
import { createMemoryHospitalDesk } from '../data/memoryHospitalDesk'
import type { HospitalDesk } from '../domain/hospital/hospitalDesk'

export function createHospitalDesk(now: () => Date): HospitalDesk {
  return createMemoryHospitalDesk(now)
}
