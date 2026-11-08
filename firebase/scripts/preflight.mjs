// 시연 전 점검 PF1~PF6 (계획 013). 차 앱과 병원 웹이 같은 Firebase 에뮬레이터에 붙을 준비가 됐는지 본다.
//   npm --prefix firebase run preflight            점검만
//   npm --prefix firebase run preflight -- --fix   고칠 수 있는 것을 고치고 다시 점검한다
//   npm --prefix firebase run preflight -- --nearby   근처 동물병원 함수(계획 015)까지: PF5에 tcp:5001, PF7 Functions 에뮬레이터
//     - PF5 (adb reverse 없음) → adb reverse tcp:8080·tcp:9099
//     - PF3·PF6 (병원이 없거나 대기 정보가 모두 지남) → seed (--reset 없이: 병원 대기 정보만 새 시각, 환자 건은 그대로)
//     - 에뮬레이터를 켜거나 끄는 일(PF1·PF2·PF4)은 하지 않는다. 안내만 한다.
// 012 시연 준비 때 에뮬레이터가 IPv4(127.0.0.1)와 IPv6(::1)에 하나씩 떠 있어서, 차(adb reverse → 127.0.0.1)와
// 병원 웹(localhost → ::1)이 서로 다른 서버에 붙었다. 이 스크립트가 그런 상태를 잡는다.
// 비밀 파일(웹 환경 파일, 차 앱 설정 파일)은 읽지 않는다. 그 값은 화면에서 눈으로 확인한다 (마지막 안내).
import { execFileSync } from 'node:child_process'
import { randomUUID } from 'node:crypto'
import { fileURLToPath } from 'node:url'
import { PROJECT_ID } from './emulator.mjs'

const IPV4 = '127.0.0.1'
const IPV6 = '[::1]'
const FIRESTORE_PORT = 8080
const AUTH_PORT = 9099
const TIMEOUT_MS = 2_000
const ADMIN = 'Bearer owner' // 에뮬레이터 전용 관리자 토큰
const MIN_HOSPITALS = 5

const FIX = process.argv.includes('--fix')
// --nearby: 근처 동물병원 함수(계획 015)까지 본다. PF5에 tcp:5001, PF7 추가
const NEARBY = process.argv.includes('--nearby')
const FUNCTIONS_PORT = 5001
const AFTER_FIX = process.env.PREFLIGHT_AFTER_FIX === '1'

let failures = 0
const failed = new Set()
function check(id, title, ok, hint = '') {
  if (!ok) {
    failures++
    failed.add(id)
  }
  console.log(`${ok ? 'PASS' : 'FAIL'}  ${id} ${title}`)
  if (!ok && hint) console.log(`      → ${hint}`)
}

const docsUrl = (host) => `http://${host}:${FIRESTORE_PORT}/v1/projects/${PROJECT_ID}/databases/(default)/documents`
const authUrl = (host, path) => `http://${host}:${AUTH_PORT}/identitytoolkit.googleapis.com/v1/${path}?key=demo-key`

/** 응답이 오면 Response, 연결이 안 되거나 시간이 지나면 null */
async function reach(url, init = {}) {
  try {
    return await fetch(url, { ...init, signal: AbortSignal.timeout(TIMEOUT_MS) })
  } catch {
    return null
  }
}

async function json(response) {
  try {
    return await response.json()
  } catch {
    return null
  }
}

const startEmulators = 'Firebase 에뮬레이터를 켠다: npm --prefix firebase run emulators (터미널 하나를 계속 쓴다)'

// PF1·PF2
const firestoreUp = (await reach(`http://${IPV4}:${FIRESTORE_PORT}/`)) !== null
check('PF1', `Firestore 에뮬레이터가 ${IPV4}:${FIRESTORE_PORT}에서 응답한다`, firestoreUp, startEmulators)
const authUp = (await reach(`http://${IPV4}:${AUTH_PORT}/`)) !== null
check('PF2', `Auth 에뮬레이터가 ${IPV4}:${AUTH_PORT}에서 응답한다`, authUp, startEmulators)

// PF3: 차와 같은 방법 (익명 가입 → 병원 목록)
let hospitalCount = 0
let probeToken = null
if (firestoreUp && authUp) {
  const signUp = await reach(authUrl(IPV4, 'accounts:signUp'), {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ returnSecureToken: true }),
  })
  probeToken = signUp?.ok ? (await json(signUp))?.idToken ?? null : null
  if (probeToken !== null) {
    const list = await reach(`${docsUrl(IPV4)}/hospitals`, { headers: { Authorization: `Bearer ${probeToken}` } })
    hospitalCount = list?.ok ? ((await json(list))?.documents?.length ?? 0) : 0
  }
}
check(
  'PF3',
  `익명 계정으로 시연 병원 ${MIN_HOSPITALS}곳 이상을 읽는다 (지금 ${hospitalCount}곳)`,
  hospitalCount >= MIN_HOSPITALS,
  '시연 병원을 넣는다: npm --prefix firebase run seed -- --reset',
)

// PF4: [::1]에 "다른" 에뮬레이터가 없다. 응답이 오면 같은 서버인지 표식으로 확인한다
async function sameFirestoreOnIpv6() {
  if ((await reach(`http://${IPV6}:${FIRESTORE_PORT}/`)) === null) return true // 아무것도 없다
  if (!firestoreUp) return false
  const marker = `_preflight/${randomUUID()}`
  const put = await reach(`${docsUrl(IPV4)}/${marker}`, {
    method: 'PATCH',
    headers: { Authorization: ADMIN, 'Content-Type': 'application/json' },
    body: JSON.stringify({ fields: { at: { timestampValue: new Date().toISOString() } } }),
  })
  if (!put?.ok) return false
  const seen = await reach(`${docsUrl(IPV6)}/${marker}`, { headers: { Authorization: ADMIN } })
  await reach(`${docsUrl(IPV4)}/${marker}`, { method: 'DELETE', headers: { Authorization: ADMIN } })
  return seen?.ok === true
}

async function sameAuthOnIpv6() {
  if ((await reach(`http://${IPV6}:${AUTH_PORT}/`)) === null) return true
  if (probeToken === null) return false
  // 127.0.0.1에서 만든 계정을 [::1]에서 찾을 수 있으면 같은 서버다
  const lookup = await reach(authUrl(IPV6, 'accounts:lookup'), {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ idToken: probeToken }),
  })
  return lookup?.ok === true && ((await json(lookup))?.users?.length ?? 0) === 1
}

const ipv6Ok = (await sameFirestoreOnIpv6()) && (await sameAuthOnIpv6())
check(
  'PF4',
  `${IPV6}:${FIRESTORE_PORT}·${AUTH_PORT}에 다른 에뮬레이터가 없다`,
  ipv6Ok,
  `다른 Firebase 에뮬레이터가 ${IPV6}에 떠 있다. 그 터미널을 끈다. 찾기: netstat -ano | findstr :${FIRESTORE_PORT} (마지막 열이 pid)`,
)

// PF5: 차 앱이 PC 에뮬레이터에 닿는 길 (ANDROID_SERIAL을 따른다)
let reverseList = null
let adbProblem = ''
// adb 서버가 잠깐 바쁘면 실패할 때가 있어 한 번 더 묻는다
for (let attempt = 0; attempt < 2 && reverseList === null; attempt++) {
  try {
    reverseList = execFileSync('adb', ['reverse', '--list'], { encoding: 'utf8', stdio: ['ignore', 'pipe', 'pipe'], timeout: 5_000 })
  } catch (error) {
    adbProblem = String(error?.stderr ?? error?.message ?? error).trim().split('\n')[0]
  }
}
const hasReverse = (port) => reverseList !== null && reverseList.includes(`tcp:${port} tcp:${port}`)
check(
  'PF5',
  `adb reverse tcp:${FIRESTORE_PORT}·tcp:${AUTH_PORT}가 걸려 있다`,
  hasReverse(FIRESTORE_PORT) && hasReverse(AUTH_PORT) && (!NEARBY || hasReverse(FUNCTIONS_PORT)),
  reverseList === null
    ? `adb를 쓸 수 없다 (${adbProblem}). 기기가 여럿이면 ANDROID_SERIAL=emulator-5554 를 정하고 다시 돌린다`
    : `adb reverse tcp:${FIRESTORE_PORT} tcp:${FIRESTORE_PORT} && adb reverse tcp:${AUTH_PORT} tcp:${AUTH_PORT}`,
)

// PF6: 차 앱이 순위를 매길 대기 정보가 있다. 알린 지 30분이 넘으면 "대기 정보 지남"이라 1위가 없다
// (docs/data/hospital-status.md "오래된 정보"). 시드하고 30분이 지나면 생긴다. 시연 직전에 다시 알린다.
const STALE_AFTER_MS = 30 * 60_000
let freshCount = 0
let oldestFreshMinutesLeft = null
if (probeToken !== null) {
  const list = await reach(`${docsUrl(IPV4)}/hospitals`, { headers: { Authorization: `Bearer ${probeToken}` } })
  const documents = list?.ok ? ((await json(list))?.documents ?? []) : []
  for (const document of documents) {
    const status = document.fields?.status?.mapValue?.fields
    const accepting = status?.acceptingEmergency?.booleanValue === true
    const updatedAt = Date.parse(status?.updatedAt?.timestampValue ?? '')
    if (!accepting || Number.isNaN(updatedAt)) continue
    const age = Date.now() - updatedAt
    if (age > STALE_AFTER_MS || age < -5 * 60_000) continue
    freshCount++
    const left = Math.floor((STALE_AFTER_MS - age) / 60_000)
    oldestFreshMinutesLeft = oldestFreshMinutesLeft === null ? left : Math.min(oldestFreshMinutesLeft, left)
  }
}
// 시연 도중에 지나면 순위가 바뀐다. 모두 10분 이상 남아 있어야 통과
const MIN_MINUTES_LEFT = 10
check(
  'PF6',
  `응급 접수를 받고 대기 정보가 30분 안인 병원이 있고, 모두 ${MIN_MINUTES_LEFT}분 이상 남았다 (지금 ${freshCount}곳${
    oldestFreshMinutesLeft === null ? '' : `, 가장 먼저 지나는 곳까지 약 ${oldestFreshMinutesLeft}분`
  })`,
  freshCount > 0 && oldestFreshMinutesLeft !== null && oldestFreshMinutesLeft >= MIN_MINUTES_LEFT,
  '시연 직전에 npm --prefix firebase run preflight -- --fix (병원 대기 정보만 새로, 환자 건은 그대로) 또는 병원 웹 [병원 상태 알리기]',
)

// PF7 (--nearby): 근처 동물병원 함수가 있는 Functions 에뮬레이터가 응답한다
if (NEARBY) {
  const fn = await reach(`http://${IPV4}:${FUNCTIONS_PORT}/${PROJECT_ID}/asia-northeast3/nearbyVets`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ data: {} }),
  })
  check(
    'PF7',
    `근처 동물병원 함수가 ${IPV4}:${FUNCTIONS_PORT}에서 응답한다`,
    fn !== null && fn.status !== 404,
    'npm --prefix firebase run emulators:nearby (functions/.secret.local에 카카오 키). 함수 자체 점검: npm --prefix firebase run check-nearby',
  )
}

// --fix: 고칠 수 있는 것을 고치고 한 번만 다시 점검한다
if (FIX && !AFTER_FIX && failures > 0) {
  const run = (file, args) => {
    try {
      execFileSync(file, args, { stdio: 'inherit', timeout: 30_000 })
      return true
    } catch {
      return false
    }
  }
  let fixed = false
  if (failed.has('PF5')) {
    console.log('\n[고침] adb reverse를 건다')
    fixed = run('adb', ['reverse', `tcp:${FIRESTORE_PORT}`, `tcp:${FIRESTORE_PORT}`]) && run('adb', ['reverse', `tcp:${AUTH_PORT}`, `tcp:${AUTH_PORT}`])
    if (NEARBY) fixed = run('adb', ['reverse', `tcp:${FUNCTIONS_PORT}`, `tcp:${FUNCTIONS_PORT}`]) && fixed
  }
  if ((failed.has('PF3') || failed.has('PF6')) && firestoreUp && authUp) {
    console.log('\n[고침] 시연 병원 대기 정보를 지금 시각으로 다시 넣는다 (환자 건은 그대로)')
    fixed = run(process.execPath, [fileURLToPath(new URL('./seed.mjs', import.meta.url))]) || fixed
  }
  if (fixed) {
    console.log('\n[다시 점검]')
    try {
      execFileSync(process.execPath, [fileURLToPath(import.meta.url), ...process.argv.slice(2)], { stdio: 'inherit', env: { ...process.env, PREFLIGHT_AFTER_FIX: '1' } })
      process.exit(0)
    } catch {
      process.exit(1)
    }
  }
}

console.log(failures === 0 ? '\n시연 전 점검 모두 통과.' : `\n시연 전 점검 실패 ${failures}건.`)
console.log('눈으로 확인할 것 (이 스크립트는 설정 파일을 읽지 않는다):')
console.log(`  - 병원 웹 화면 위 "서버:" 줄이 ${IPV4}:${FIRESTORE_PORT}이고 "실시간 연결됨"인지 (localhost면 경고가 뜬다)`)
console.log(`  - 차 앱 debug 빌드의 FIREBASE_EMULATOR_HOST가 ${IPV4}인지, debug 패널이 "시연"인지`)
process.exit(failures === 0 ? 0 : 1)
