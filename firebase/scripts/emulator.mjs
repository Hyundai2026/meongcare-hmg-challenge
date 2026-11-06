// Firebase 에뮬레이터 REST 도우미 (의존성 없음, Node 22+ 내장 fetch). seed.mjs와 check-rules.mjs가 같이 쓴다.
// 에뮬레이터 전용이다. 진짜 프로젝트에는 쓰지 않는다 ("Bearer owner"는 에뮬레이터만 받는 관리자 토큰이다).

export const PROJECT_ID = process.env.FIREBASE_PROJECT_ID ?? 'demo-meongcare'
const FIRESTORE = `http://${process.env.FIRESTORE_EMULATOR_HOST ?? '127.0.0.1:8080'}`
const AUTH = `http://${process.env.FIREBASE_AUTH_EMULATOR_HOST ?? '127.0.0.1:9099'}`

export const DOCS = `${FIRESTORE}/v1/projects/${PROJECT_ID}/databases/(default)/documents`
export const ADMIN = 'owner'

/** JS 값 → Firestore REST 값. Date는 timestampValue, 정수는 integerValue(문자열). */
export function toValue(value) {
  if (value === null || value === undefined) return { nullValue: null }
  if (value instanceof Date) return { timestampValue: value.toISOString() }
  if (typeof value === 'boolean') return { booleanValue: value }
  if (typeof value === 'number') return Number.isInteger(value) ? { integerValue: String(value) } : { doubleValue: value }
  if (typeof value === 'string') return { stringValue: value }
  if (Array.isArray(value)) return { arrayValue: value.length === 0 ? {} : { values: value.map(toValue) } }
  return { mapValue: { fields: toFields(value) } }
}

export function toFields(object) {
  return Object.fromEntries(Object.entries(object).map(([k, v]) => [k, toValue(v)]))
}

/** 에뮬레이터가 켜져 있는지 확인한다. 아니면 안내하고 끝낸다. */
export async function requireEmulators() {
  try {
    await fetch(`${FIRESTORE}/`)
    await fetch(`${AUTH}/`)
  } catch {
    console.error('Firebase 에뮬레이터가 꺼져 있다. 다른 터미널에서 먼저 켠다: npm --prefix firebase run emulators')
    process.exit(1)
  }
}

/** Firestore REST 요청. token이 null이면 로그인 안 한 요청이다. */
export async function firestore(method, path, { token = null, body } = {}) {
  const headers = { 'Content-Type': 'application/json' }
  if (token !== null) headers.Authorization = `Bearer ${token}`
  const response = await fetch(`${DOCS}${path}`, { method, headers, body: body === undefined ? undefined : JSON.stringify(body) })
  const text = await response.text()
  return { status: response.status, json: text.length > 0 ? JSON.parse(text) : null }
}

/** 에뮬레이터의 Firestore 데이터를 모두 지운다 (에뮬레이터 전용 주소). */
export async function resetFirestore() {
  const response = await fetch(`${FIRESTORE}/emulator/v1/projects/${PROJECT_ID}/databases/(default)/documents`, { method: 'DELETE' })
  if (!response.ok) throw new Error(`Firestore 비우기 실패: HTTP ${response.status}`)
}

/** Auth 에뮬레이터에 익명 계정을 만든다. { idToken, localId } */
export async function signUpAnonymously() {
  const response = await fetch(`${AUTH}/identitytoolkit.googleapis.com/v1/accounts:signUp?key=demo-key`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ returnSecureToken: true }),
  })
  if (!response.ok) throw new Error(`익명 가입 실패: HTTP ${response.status}`)
  const json = await response.json()
  return { idToken: json.idToken, localId: json.localId }
}
