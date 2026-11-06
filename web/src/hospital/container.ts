/**
 * 병원 웹의 저장소를 고른다 (앱의 AppContainer 자리).
 * - Firebase 설정(web/.env.local의 VITE_FIREBASE_PROJECT_ID)이 있으면 Firestore
 * - 없으면 메모리 (서버 없이 이 브라우저에서만, 시연 병원 + 예시 환자)
 * VITE_로 시작하는 값은 번들에 그대로 들어가 공개된다. Firebase 웹 설정은 비밀값이 아니지만, 그래서 커밋하지 않는 파일에만 둔다.
 */
import { createFirestoreHospitalDesk } from '../data/firestoreHospitalDesk'
import { createMemoryHospitalDesk } from '../data/memoryHospitalDesk'
import type { HospitalDesk } from '../domain/hospital/hospitalDesk'

function envValue(value: unknown): string | null {
  return typeof value === 'string' && value.trim().length > 0 ? value.trim() : null
}

export function createHospitalDesk(now: () => Date): HospitalDesk {
  const projectId = envValue(import.meta.env.VITE_FIREBASE_PROJECT_ID)
  if (projectId === null) return createMemoryHospitalDesk(now)
  const emulatorHost = envValue(import.meta.env.VITE_FIREBASE_EMULATOR_HOST)
  // 에뮬레이터는 API 키를 검사하지 않는다 (2026-11-06 확인). 진짜 프로젝트는 키가 있어야 한다.
  const apiKey = envValue(import.meta.env.VITE_FIREBASE_API_KEY) ?? (emulatorHost !== null ? 'demo-key' : null)
  if (apiKey === null) return createMemoryHospitalDesk(now)
  return createFirestoreHospitalDesk({ projectId, apiKey, emulatorHost })
}
