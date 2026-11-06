/**
 * 저장소 구현을 고른다 (앱의 AppContainer 자리).
 * - 브라우저 localStorage: 늘 있다 (서버 설정이 없거나 동의하기 전)
 * - 서버(Firestore, 계획 014): `web/.env.local`에 Firebase 설정이 있을 때만. **설정이 있을 때만 동적 import**해서
 *   설정 없이 빌드한 보호자 페이지는 Firebase를 내려받지 않는다 (사용자 결정 U8).
 * VITE_로 시작하는 값은 번들에 그대로 들어가 공개된다. Firebase 웹 설정은 비밀값이 아니지만 커밋하지 않는 파일에만 둔다.
 */
import { clearLocalPetProfile, createLocalStoragePetProfileRepository } from './data/localStoragePetProfileRepository'
import type { FirebaseWebConfig } from './data/firestoreHospitalDesk'
import { emulatorHostFor } from './domain/hospital/serverInfo'
import type { BrowserProfileStore } from './domain/owner/migration'
import type { OwnerServer } from './domain/owner/ownerAccount'

/** 저장소를 쓸 수 없을 때(쿠키 차단 등). 항상 unavailable을 돌려준다. */
const unavailableStore: BrowserProfileStore = {
  load: async () => ({ ok: false, reason: 'unavailable' }),
  save: async () => ({ ok: false, reason: 'unavailable' }),
  clear: async () => false,
}

export function createBrowserProfileStore(): BrowserProfileStore {
  try {
    // 접근하는 것 자체가 던질 수 있다 (사이트 데이터 차단 등).
    const storage = window.localStorage
    if (storage == null) return unavailableStore
    const repository = createLocalStoragePetProfileRepository(storage)
    return { ...repository, clear: async () => clearLocalPetProfile(storage) }
  } catch {
    return unavailableStore
  }
}

function envValue(value: unknown): string | null {
  return typeof value === 'string' && value.trim().length > 0 ? value.trim() : null
}

/** 병원 웹(hospital/container.ts)과 같은 규칙: 프로젝트 ID가 없으면 서버 없음, 에뮬레이터는 키를 검사하지 않는다 */
function firebaseConfig(): FirebaseWebConfig | null {
  const projectId = envValue(import.meta.env.VITE_FIREBASE_PROJECT_ID)
  if (projectId === null) return null
  // localhost는 127.0.0.1로 맞춘다 (차 앱과 같은 에뮬레이터를 보게, 013)
  const rawHost = envValue(import.meta.env.VITE_FIREBASE_EMULATOR_HOST)
  const emulatorHost = rawHost === null ? null : emulatorHostFor(rawHost)
  const apiKey = envValue(import.meta.env.VITE_FIREBASE_API_KEY) ?? (emulatorHost !== null ? 'demo-key' : null)
  if (apiKey === null) return null
  return { projectId, apiKey, emulatorHost }
}

export interface OwnerSetup {
  browser: BrowserProfileStore
  /** 서버 설정이 없으면 null (브라우저에만 저장, 차와 연결 없음) */
  server: OwnerServer | null
}

export async function createOwnerSetup(): Promise<OwnerSetup> {
  const browser = createBrowserProfileStore()
  const config = firebaseConfig()
  if (config === null) return { browser, server: null }
  try {
    const { createFirestoreOwnerStore } = await import('./data/firestoreOwnerStore')
    return { browser, server: createFirestoreOwnerStore(config) }
  } catch {
    // 모듈을 내려받지 못했으면(네트워크) 브라우저 저장으로 계속한다
    return { browser, server: null }
  }
}
