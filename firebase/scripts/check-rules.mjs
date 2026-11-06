// 보안 규칙 점검 R1~R8 (계획 012 "설계 결정 6"). 에뮬레이터를 켜고 시드한 뒤에 돌린다.
//   npm --prefix firebase run emulators   (다른 터미널)
//   npm --prefix firebase run seed -- --reset
//   npm --prefix firebase run check-rules
// 익명 계정 둘(병원 직원 A, 차 B)과 셋째(다른 차 C)를 만들어 해 본다. 점검이 만든 문서는 끝에 지운다.
import { ADMIN, DOCS, PROJECT_ID, firestore, requireEmulators, signUpAnonymously, toFields } from './emulator.mjs'

await requireEmulators()

const HID = 'demo-fast'
const CODE = 'fast-1234'
let failures = 0

function check(id, title, ok, detail = '') {
  if (!ok) failures++
  console.log(`${ok ? 'PASS' : 'FAIL'}  ${id} ${title}${ok ? '' : `  ${detail}`}`)
}

const staff = await signUpAnonymously()
const car = await signUpAnonymously()
const otherCar = await signUpAnonymously()

/** 직원이 상태를 알린다 (updatedAt = 서버 시각, 웹의 serverTimestamp()와 같다) */
async function updateStatus(token, status, extraFields = {}) {
  const fields = { status: { mapValue: { fields: toFields(status) } }, ...extraFields }
  const fieldPaths = [...Object.keys(status).map((k) => `status.${k}`), ...Object.keys(extraFields)]
  return firestore('POST', ':commit', {
    token,
    body: {
      writes: [
        {
          update: { name: `projects/${PROJECT_ID}/databases/(default)/documents/hospitals/${HID}`, fields },
          updateMask: { fieldPaths },
          updateTransforms: [{ fieldPath: 'status.updatedAt', setToServerValue: 'REQUEST_TIME' }],
        },
      ],
    },
  })
}

// R1·R2
const anonymousList = await firestore('GET', '/hospitals')
check('R1', '로그인 안 한 요청은 병원 목록을 읽지 못한다', anonymousList.status === 403, `HTTP ${anonymousList.status}`)
const list = await firestore('GET', '/hospitals', { token: car.idToken })
check('R2', '익명 계정은 병원 목록을 읽는다', list.status === 200 && (list.json?.documents?.length ?? 0) >= 5, `HTTP ${list.status}`)

// R3
const status = { waitMinutes: 15, congestion: 'normal', acceptingEmergency: true }
const notStaff = await updateStatus(staff.idToken, status)
check('R3', '직원 등록 안 한 계정은 병원 상태를 바꾸지 못한다', notStaff.status === 403, `HTTP ${notStaff.status}`)

// R4
const wrong = await firestore('PATCH', `/hospitals/${HID}/staff/${staff.localId}`, {
  token: staff.idToken,
  body: { fields: toFields({ joinCode: 'wrong' }) },
})
const right = await firestore('PATCH', `/hospitals/${HID}/staff/${staff.localId}`, {
  token: staff.idToken,
  body: { fields: toFields({ joinCode: CODE }) },
})
check('R4', '틀린 코드로는 직원 등록이 안 되고, 맞는 코드로는 된다', wrong.status === 403 && right.status === 200, `틀림 ${wrong.status}, 맞음 ${right.status}`)

// R5
const ok = await updateStatus(staff.idToken, status)
const tooLong = await updateStatus(staff.idToken, { ...status, waitMinutes: 601 })
const rename = await updateStatus(staff.idToken, status, { name: { stringValue: '바꾼 이름' } })
check(
  'R5',
  '직원은 상태를 바꾸지만 601분·이름 바꾸기는 거부된다',
  ok.status === 200 && tooLong.status === 403 && rename.status === 403,
  `정상 ${ok.status}, 601분 ${tooLong.status}, 이름 ${rename.status}`,
)

// R6
const now = new Date()
const newCase = (createdBy) => ({
  schemaVersion: 1,
  createdBy,
  createdAt: now,
  status: 'en_route',
  remainingMinutes: 12,
  etaAt: new Date(now.getTime() + 12 * 60_000),
  etaUpdatedAt: now,
  card: { pet: { name: '보리' }, symptoms: [{ code: 'COUGH', label: '기침' }] },
  acknowledgedAt: null,
})
const created = await firestore('POST', `/hospitals/${HID}/cases`, { token: car.idToken, body: { fields: toFields(newCase(car.localId)) } })
const caseName = created.json?.name ?? ''
const casePath = caseName.slice(caseName.indexOf('/documents') + '/documents'.length)
const etaUpdate = await firestore('PATCH', `${casePath}?updateMask.fieldPaths=remainingMinutes&updateMask.fieldPaths=etaUpdatedAt`, {
  token: car.idToken,
  body: { fields: toFields({ remainingMinutes: 9, etaUpdatedAt: new Date() }) },
})
const impostor = await firestore('POST', `/hospitals/${HID}/cases`, { token: car.idToken, body: { fields: toFields(newCase(otherCar.localId)) } })
const withPhoto = await firestore('POST', `/hospitals/${HID}/cases`, {
  token: car.idToken,
  body: { fields: toFields({ ...newCase(car.localId), card: { pet: { name: '보리', photoDataUrl: 'data:image/jpeg;base64,AA' }, symptoms: [] } }) },
})
check(
  'R6',
  '차는 자기 환자 건을 만들고 남은 시간을 바꾼다. createdBy가 남이거나 사진이 있으면 거부된다',
  created.status === 200 && etaUpdate.status === 200 && impostor.status === 403 && withPhoto.status === 403,
  `만들기 ${created.status}, 갱신 ${etaUpdate.status}, 남 ${impostor.status}, 사진 ${withPhoto.status}`,
)

// R7
const byOther = await firestore('GET', casePath, { token: otherCar.idToken })
const byStaff = await firestore('GET', casePath, { token: staff.idToken })
const staffList = await firestore('GET', `/hospitals/${HID}/cases`, { token: staff.idToken })
check(
  'R7',
  '다른 차 계정은 남의 환자 건을 읽지 못한다. 그 병원 직원은 읽는다',
  byOther.status === 403 && byStaff.status === 200 && staffList.status === 200,
  `다른 차 ${byOther.status}, 직원 ${byStaff.status}, 직원 목록 ${staffList.status}`,
)

// R8
const secretByCar = await firestore('GET', `/hospitalSecrets/${HID}`, { token: car.idToken })
const secretByStaff = await firestore('GET', `/hospitalSecrets/${HID}`, { token: staff.idToken })
check('R8', 'hospitalSecrets는 누구도 읽지 못한다', secretByCar.status === 403 && secretByStaff.status === 403, `차 ${secretByCar.status}, 직원 ${secretByStaff.status}`)

// 정리 (관리자 토큰)
if (casePath) await firestore('DELETE', casePath, { token: ADMIN })
await firestore('DELETE', `/hospitals/${HID}/staff/${staff.localId}`, { token: ADMIN })

console.log(failures === 0 ? '\n규칙 점검 모두 통과.' : `\n규칙 점검 실패 ${failures}건.`)
console.log(`(대상: ${DOCS})`)
process.exit(failures === 0 ? 0 : 1)
