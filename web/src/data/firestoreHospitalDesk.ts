/**
 * Firestore 병원 웹 저장소 (Firebase JS SDK). 앱의 Real 자리다.
 * - 익명 로그인 → 링크의 병원 코드로 이 브라우저를 그 병원 직원으로 등록 → 상태·환자 건을 실시간으로 받는다.
 * - 경로와 형식: docs/data/hospital-status.md, docs/data/emergency-case.md. 보안 규칙: firebase/firestore.rules
 * - 이 파일과 hospital/container.ts만 'firebase'를 import한다 (화면은 HospitalDesk만 안다).
 * 로그를 남기지 않는다 (console.log 0건).
 */
import { initializeApp, type FirebaseApp } from 'firebase/app'
import { connectAuthEmulator, getAuth, signInAnonymously } from 'firebase/auth'
import {
  collection,
  connectFirestoreEmulator,
  deleteDoc,
  doc,
  getDoc,
  getDocs,
  getFirestore,
  onSnapshot,
  serverTimestamp,
  setDoc,
  updateDoc,
} from 'firebase/firestore'
import { caseFromJson } from '../domain/hospital/emergencyCase'
import type { DeskFailure, DeskResult, HospitalDesk } from '../domain/hospital/hospitalDesk'
import { hospitalFromJson, type HospitalSummary } from '../domain/hospital/hospitalStatus'

export interface FirebaseWebConfig {
  projectId: string
  apiKey: string
  /** 에뮬레이터 주소 (예: 127.0.0.1). 없으면 진짜 프로젝트에 붙는다 */
  emulatorHost: string | null
}

const AUTH_EMULATOR_PORT = 9099
const FIRESTORE_EMULATOR_PORT = 8080

function failureOf(error: unknown): DeskFailure {
  const code = typeof error === 'object' && error !== null && 'code' in error ? String(error.code) : ''
  if (code.endsWith('permission-denied')) return 'wrong-code'
  if (code.endsWith('unavailable') || code.endsWith('network-request-failed')) return 'network'
  return 'unknown'
}

/** IPv6 주소는 URL에서 대괄호로 감싼다 */
function hostForUrl(host: string): string {
  return host.includes(':') && !host.startsWith('[') ? `[${host}]` : host
}

export function createFirestoreHospitalDesk(config: FirebaseWebConfig): HospitalDesk {
  const app: FirebaseApp = initializeApp({ projectId: config.projectId, apiKey: config.apiKey })
  const auth = getAuth(app)
  const db = getFirestore(app)
  if (config.emulatorHost !== null) {
    const host = hostForUrl(config.emulatorHost)
    connectAuthEmulator(auth, `http://${host}:${AUTH_EMULATOR_PORT}`, { disableWarnings: true })
    connectFirestoreEmulator(db, host, FIRESTORE_EMULATOR_PORT)
  }

  let uid: Promise<string> | null = null
  const signIn = (): Promise<string> => {
    uid ??= signInAnonymously(auth)
      .then((credential) => credential.user.uid)
      .catch((error: unknown) => {
        uid = null // 다음에 다시 시도한다
        throw error
      })
    return uid
  }

  async function attempt<T>(work: () => Promise<T>): Promise<DeskResult<T>> {
    try {
      return { ok: true, value: await work() }
    } catch (error) {
      return { ok: false, reason: failureOf(error) }
    }
  }

  return {
    isLocalOnly: false,

    listHospitals: () =>
      attempt(async () => {
        await signIn()
        const snapshot = await getDocs(collection(db, 'hospitals'))
        return snapshot.docs
          .map((d) => hospitalFromJson(d.id, d.data()))
          .filter((h): h is HospitalSummary => h !== null)
          .sort((a, b) => (a.id < b.id ? -1 : a.id > b.id ? 1 : 0))
      }),

    async open(hospitalId, joinCode) {
      let me: string
      try {
        me = await signIn()
      } catch (error) {
        return { ok: false, reason: failureOf(error) }
      }
      const staffRef = doc(db, 'hospitals', hospitalId, 'staff', me)
      try {
        // 이 브라우저가 이미 직원으로 등록돼 있으면 코드 없이 연다
        if ((await getDoc(staffRef)).exists()) return { ok: true, value: undefined }
      } catch (error) {
        return { ok: false, reason: failureOf(error) }
      }
      if (joinCode === null || joinCode.length === 0) return { ok: false, reason: 'wrong-code' }
      return attempt(() => setDoc(staffRef, { joinCode }))
    },

    watchHospital(hospitalId, onChange) {
      return onSnapshot(
        doc(db, 'hospitals', hospitalId),
        (snapshot) => onChange(snapshot.exists() ? hospitalFromJson(snapshot.id, snapshot.data()) : null),
        () => onChange(null),
      )
    },

    watchCases(hospitalId, onChange) {
      return onSnapshot(
        collection(db, 'hospitals', hospitalId, 'cases'),
        (snapshot) => onChange(snapshot.docs.map((d) => caseFromJson(d.id, d.data()))),
        () => onChange([]),
      )
    },

    updateStatus: (hospitalId, input) =>
      attempt(() =>
        updateDoc(doc(db, 'hospitals', hospitalId), {
          status: {
            waitMinutes: input.waitMinutes,
            congestion: input.congestion,
            acceptingEmergency: input.acceptingEmergency,
            updatedAt: serverTimestamp(),
          },
        }),
      ),

    completeCase: (hospitalId, caseId) => attempt(() => deleteDoc(doc(db, 'hospitals', hospitalId, 'cases', caseId))),
  }
}
