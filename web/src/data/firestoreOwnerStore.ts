/**
 * 보호자 프로필 Firestore 저장소와 차 연결 (Firebase JS SDK, 계획 014). 앱의 Real 자리다.
 * - 보호자는 익명 계정이다. **동의하기 전에는 계정을 만들지 않는다** (status는 기억한 계정이 있는지만 본다).
 * - 경로와 형식: docs/data/owner-link.md. 보안 규칙: firebase/firestore.rules (R14~R24)
 * - 이 파일과 container.ts의 동적 import만 이 모듈을 안다 (화면은 OwnerServer만 안다). 'firebase'는 data/firestore*.ts만 import한다.
 * 로그를 남기지 않는다 (console.log 0건).
 */
import { initializeApp } from 'firebase/app'
import { connectAuthEmulator, getAuth, signInAnonymously } from 'firebase/auth'
import {
  Timestamp,
  collection,
  connectFirestoreEmulator,
  doc,
  getDoc,
  getFirestore,
  onSnapshot,
  serverTimestamp,
  setDoc,
  writeBatch,
} from 'firebase/firestore'
import { FIRESTORE_EMULATOR_PORT, hostForUrl } from '../domain/hospital/serverInfo'
import { CONSENT_VERSION } from '../domain/owner/consent'
import type { AccountStatus, LinkCarResult, LinkedCar, OwnerServer } from '../domain/owner/ownerAccount'
import { profileFromServerDocs, serverDocsOf } from '../domain/owner/ownerProfileDoc'
import type { PetProfile } from '../domain/petProfile'
import type { LoadResult, PetProfileRepository, SaveResult } from '../domain/petProfileRepository'
import type { FirebaseWebConfig } from './firestoreHospitalDesk'

const AUTH_EMULATOR_PORT = 9099

type Failure = 'denied' | 'network' | 'unknown'

function failureOf(error: unknown): Failure {
  const code = typeof error === 'object' && error !== null && 'code' in error ? String(error.code) : ''
  if (code.endsWith('permission-denied')) return 'denied'
  if (code.endsWith('unavailable') || code.endsWith('network-request-failed') || code.endsWith('deadline-exceeded')) return 'network'
  return 'unknown'
}

export function createFirestoreOwnerStore(config: FirebaseWebConfig): OwnerServer {
  const app = initializeApp({ projectId: config.projectId, apiKey: config.apiKey }, 'owner')
  const auth = getAuth(app)
  const db = getFirestore(app)
  if (config.emulatorHost !== null) {
    const host = hostForUrl(config.emulatorHost)
    connectAuthEmulator(auth, `http://${host}:${AUTH_EMULATOR_PORT}`, { disableWarnings: true })
    connectFirestoreEmulator(db, host, FIRESTORE_EMULATOR_PORT)
  }

  /** 이 브라우저가 기억한 계정. 없으면 null (동의 전) */
  async function rememberedUid(): Promise<string | null> {
    await auth.authStateReady()
    return auth.currentUser?.uid ?? null
  }

  const profileRef = (uid: string) => doc(db, 'petProfiles', uid)
  const photoRef = (uid: string) => doc(db, 'petProfiles', uid, 'private', 'photo')

  const repository: PetProfileRepository = {
    async load(): Promise<LoadResult> {
      try {
        const uid = await rememberedUid()
        if (uid === null) return { ok: false, reason: 'unavailable' }
        const [profile, photo] = await Promise.all([getDoc(profileRef(uid)), getDoc(photoRef(uid))])
        if (!profile.exists()) return { ok: true, profile: null }
        const parsed = profileFromServerDocs(profile.data(), photo.exists() ? photo.data() : null)
        return parsed.ok ? { ok: true, profile: parsed.profile } : { ok: false, reason: parsed.reason }
      } catch (error) {
        return { ok: false, reason: failureOf(error) === 'network' ? 'network' : 'unavailable' }
      }
    },

    async save(profile: PetProfile): Promise<SaveResult> {
      try {
        const uid = await rememberedUid()
        if (uid === null) return { ok: false, reason: 'unavailable' }
        const docs = serverDocsOf(profile)
        // 프로필과 사진을 한 번에 쓴다. 사진이 없으면 사진 문서를 지운다 (차는 사진 문서를 읽지 못한다)
        const batch = writeBatch(db)
        batch.set(profileRef(uid), docs.profile)
        if (docs.photoDataUrl !== null) batch.set(photoRef(uid), { photoDataUrl: docs.photoDataUrl })
        else batch.delete(photoRef(uid))
        await batch.commit()
        return { ok: true }
      } catch (error) {
        return { ok: false, reason: failureOf(error) === 'network' ? 'network' : 'unknown' }
      }
    },
  }

  return {
    emulator: config.emulatorHost !== null,
    repository,
    account: {
      async status(): Promise<AccountStatus> {
        try {
          const uid = await rememberedUid()
          if (uid === null) return 'before-consent'
          const consent = await getDoc(doc(db, 'owners', uid))
          return consent.exists() ? 'consented' : 'before-consent'
        } catch (error) {
          return failureOf(error) === 'network' ? 'network' : 'before-consent'
        }
      },

      async consent() {
        try {
          const uid = (await rememberedUid()) ?? (await signInAnonymously(auth)).user.uid
          const ref = doc(db, 'owners', uid)
          const existing = await getDoc(ref)
          if (!existing.exists()) {
            await setDoc(ref, { schemaVersion: 1, consentVersion: CONSENT_VERSION, consentedAt: serverTimestamp() })
          }
          return { ok: true }
        } catch (error) {
          return { ok: false, reason: failureOf(error) === 'network' ? 'network' : 'unknown' }
        }
      },

      async linkCar(code: string): Promise<LinkCarResult> {
        try {
          const uid = await rememberedUid()
          if (uid === null) return { ok: false, reason: 'unknown' }
          const profile = await getDoc(profileRef(uid))
          if (!profile.exists()) return { ok: false, reason: 'no-profile' }
          // 코드가 없음·만료·이미 씀은 모두 규칙상 거부(permission-denied)다. 어느 쪽인지 알리지 않는다
          const codeRef = doc(db, 'linkCodes', code)
          const codeDoc = await getDoc(codeRef)
          const carUid = codeDoc.exists() ? codeDoc.get('carUid') : null
          if (typeof carUid !== 'string' || carUid.length === 0) return { ok: false, reason: 'not-found' }
          // claim과 허가를 한 번에 쓴다 (규칙이 getAfter로 claim 결과를 본다, F1)
          const batch = writeBatch(db)
          batch.update(codeRef, { ownerUid: uid, claimedAt: serverTimestamp() })
          batch.set(doc(db, 'petProfiles', uid, 'readers', carUid), { linkCode: code, createdAt: serverTimestamp() })
          await batch.commit()
          return { ok: true }
        } catch (error) {
          const failure = failureOf(error)
          return { ok: false, reason: failure === 'denied' ? 'not-found' : failure }
        }
      },

      watchLinkedCars(listener: (cars: LinkedCar[] | null) => void): () => void {
        let stop: (() => void) | null = null
        let cancelled = false
        void rememberedUid()
          .then((uid) => {
            if (cancelled) return
            if (uid === null) {
              listener([])
              return
            }
            // 보호자는 자기 허가 목록을 읽는다 (보안 규칙 readers: list는 보호자만)
            stop = onSnapshot(
              collection(db, 'petProfiles', uid, 'readers'),
              (snapshot) => {
                listener(
                  snapshot.docs.map((car) => {
                    const at: unknown = car.get('createdAt', { serverTimestamps: 'estimate' })
                    return { linkedAt: at instanceof Timestamp ? at.toDate().toISOString() : null }
                  }),
                )
              },
              () => listener(null),
            )
          })
          .catch(() => listener(null))
        return () => {
          cancelled = true
          stop?.()
        }
      },
    },
  }
}
