// 근처 동물병원 함수 점검 N1~N5 (계획 015). Functions 에뮬레이터가 켜져 있어야 한다 (npm run emulators:nearby).
//   npm --prefix firebase run check-nearby
// 병원 이름·전화·좌표는 출력하지 않는다 (카카오 결과 저장·노출 최소화). 개수와 거리만 본다.
import { PROJECT_ID, signUpAnonymously } from './emulator.mjs'

const FUNCTION_URL = `http://127.0.0.1:5001/${PROJECT_ID}/asia-northeast3/nearbyVets`
const DEMO = { latitude: 37.4127212, longitude: 127.094681 } // 시연 위치 (수정구 시흥동)
const MAX_KM = 5.5

let failures = 0
function check(id, title, ok, detail = '') {
  if (!ok) failures++
  console.log(`${ok ? 'PASS' : 'FAIL'}  ${id} ${title}${detail ? ` (${detail})` : ''}`)
}

async function call(token, data) {
  try {
    const response = await fetch(FUNCTION_URL, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
      body: JSON.stringify({ data }),
      signal: AbortSignal.timeout(10_000),
    })
    return { status: response.status, body: await response.json().catch(() => null) }
  } catch (error) {
    return { status: 0, body: null, error: String(error?.message ?? error) }
  }
}

function km(a, b) {
  const r = (d) => (d * Math.PI) / 180
  const h = Math.sin(r(b.latitude - a.latitude) / 2) ** 2 +
    Math.cos(r(a.latitude)) * Math.cos(r(b.latitude)) * Math.sin(r(b.longitude - a.longitude) / 2) ** 2
  return 2 * 6371.0088 * Math.asin(Math.sqrt(h))
}

const n1 = await call(null, DEMO)
if (n1.status === 0) {
  console.log(`FAIL  함수에 닿지 않는다: ${FUNCTION_URL} (${n1.error})`)
  console.log('      → npm --prefix firebase run emulators:nearby (functions/.secret.local에 카카오 키)')
  process.exit(1)
}
check('N1', '로그인 없이 부르면 unauthenticated', n1.body?.error?.status === 'UNAUTHENTICATED', `HTTP ${n1.status}`)

const { idToken } = await signUpAnonymously()
const n3 = await call(idToken, { latitude: 999, longitude: 127 })
check('N3', '위도 999면 invalid-argument', n3.body?.error?.status === 'INVALID_ARGUMENT', `HTTP ${n3.status}`)

const n2 = await call(idToken, DEMO)
const result = n2.body?.result
const vets = Array.isArray(result?.vets) ? result.vets : null
const farthest = vets ? Math.max(0, ...vets.map((v) => km(DEMO, v))) : NaN
check(
  'N2',
  `익명 계정 + 시연 위치로 부르면 병원 목록이 오고 모두 ${MAX_KM}km 안`,
  vets !== null && farthest <= MAX_KM,
  vets ? `${vets.length}곳, 가장 먼 곳 ${farthest.toFixed(2)}km` : `HTTP ${n2.status} ${n2.body?.error?.status ?? ''}`,
)
check('N5', '응답에 schemaVersion 1, radiusMeters 5000', result?.schemaVersion === 1 && result?.radiusMeters === 5000)

const n4 = await call(idToken, DEMO)
check('N4', 'N2 직후 같은 계정으로 다시 부르면 resource-exhausted', n4.body?.error?.status === 'RESOURCE_EXHAUSTED', `HTTP ${n4.status}`)

console.log(failures === 0 ? '\n모두 통과' : `\n${failures}개 실패`)
process.exit(failures === 0 ? 0 : 1)
