// 보안 규칙 점검 R1~R24 (계획 012 "설계 결정 6", R9~R13은 계획 013 카드 확인, R14~R24는 계획 014 보호자 연결). 에뮬레이터를 켜고 시드한 뒤에 돌린다.
//   npm --prefix firebase run emulators   (다른 터미널)
//   npm --prefix firebase run seed -- --reset
//   npm --prefix firebase run check-rules
// 익명 계정 둘(병원 직원 A, 차 B)과 셋째(다른 차 C), 보호자 둘(O, O2)을 만들어 해 본다. 점검이 만든 문서는 끝에 지운다.
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

// R9~R13: 병원 [카드 확인] (계획 013). 서버 시각으로, 확인 전인 건에 한 번만. 차는 쓰지 못하고 읽기만 한다.
/** acknowledgedAt 쓰기. serverTime이면 REQUEST_TIME 변환, 아니면 클라이언트 시각 값 */
async function writeAck(token, serverTime) {
  const name = caseName
  const write = serverTime
    ? { update: { name, fields: {} }, updateMask: { fieldPaths: [] }, updateTransforms: [{ fieldPath: 'acknowledgedAt', setToServerValue: 'REQUEST_TIME' }] }
    : { update: { name, fields: toFields({ acknowledgedAt: new Date() }) }, updateMask: { fieldPaths: ['acknowledgedAt'] } }
  return firestore('POST', ':commit', { token, body: { writes: [{ ...write, currentDocument: { exists: true } }] } })
}
const clientTime = await writeAck(staff.idToken, false)
check('R10', '직원이 확인 시각에 클라이언트 시각을 쓰면 거부된다', clientTime.status === 403, `HTTP ${clientTime.status}`)
const carAck = await writeAck(car.idToken, true)
check('R12a', '차 계정은 자기 건의 확인 시각을 쓰지 못한다', carAck.status === 403, `HTTP ${carAck.status}`)
const staffAck = await writeAck(staff.idToken, true)
check('R9', '직원은 확인 전 건의 확인 시각을 서버 시각으로 쓴다', staffAck.status === 200, `HTTP ${staffAck.status}`)
const again = await writeAck(staff.idToken, true)
check('R11', '이미 확인된 건의 확인 시각은 서버 시각으로도 다시 못 바꾼다', again.status === 403, `HTTP ${again.status}`)
const carRead = await firestore('GET', `${casePath}?mask.fieldPaths=acknowledgedAt`, { token: car.idToken })
check(
  'R12b',
  '차는 자기 건의 확인 시각을 읽는다',
  carRead.status === 200 && typeof carRead.json?.fields?.acknowledgedAt?.timestampValue === 'string',
  `HTTP ${carRead.status}, ${JSON.stringify(carRead.json?.fields?.acknowledgedAt)}`,
)
const staffDelete = await firestore('DELETE', casePath, { token: staff.idToken })
const readGone = await firestore('GET', `${casePath}?mask.fieldPaths=acknowledgedAt`, { token: car.idToken })
check(
  'R13',
  `직원이 [완료]로 지운 건을 차가 읽으면 403 또는 404다 (실제: ${readGone.status})`,
  staffDelete.status === 200 && (readGone.status === 403 || readGone.status === 404),
  `지우기 ${staffDelete.status}, 읽기 ${readGone.status}`,
)

// R14~R24: 보호자 프로필과 차 연결 (계획 014, docs/data/owner-link.md). 보호자 O·O2, 차 C·C2
const owner = await signUpAnonymously()
const owner2 = await signUpAnonymously()
const carC = car
const carC2 = otherCar
const DOC_NAME = `projects/${PROJECT_ID}/databases/(default)/documents`
const CODE_ALPHABET = '23456789ABCDEFGHJKMNPQRSTUVWXYZ'
const newCode = () => Array.from({ length: 6 }, () => CODE_ALPHABET[Math.floor(Math.random() * CODE_ALPHABET.length)]).join('')
const commit = (token, writes) => firestore('POST', ':commit', { token, body: { writes } })
const serverTime = (fieldPath) => ({ fieldPath, setToServerValue: 'REQUEST_TIME' })
const madeCodes = []

/** 차가 코드 문서를 만든다 (createdAt = 서버 시각, 이미 있으면 실패) */
function createCode(token, code, { carUid, ownerUid = null, clientTime = false } = {}) {
  const fields = toFields({ schemaVersion: 1, carUid, ownerUid, claimedAt: null, ...(clientTime ? { createdAt: new Date() } : {}) })
  return commit(token, [
    {
      update: { name: `${DOC_NAME}/linkCodes/${code}`, fields },
      ...(clientTime ? {} : { updateTransforms: [serverTime('createdAt')] }),
      currentDocument: { exists: false },
    },
  ])
}
/** 보호자의 claim 쓰기 하나 */
const claimWrite = (code, ownerUid) => ({
  update: { name: `${DOC_NAME}/linkCodes/${code}`, fields: toFields({ ownerUid }) },
  updateMask: { fieldPaths: ['ownerUid'] },
  updateTransforms: [serverTime('claimedAt')],
  currentDocument: { exists: true },
})
/** 허가 문서 만들기 쓰기 하나 */
const readerWrite = (ownerUid, carUid, code) => ({
  update: { name: `${DOC_NAME}/petProfiles/${ownerUid}/readers/${carUid}`, fields: toFields({ linkCode: code }) },
  updateTransforms: [serverTime('createdAt')],
  currentDocument: { exists: false },
})
/** 차 C의 새 코드를 만들고 보호자 O가 claim + 허가를 한 번에 쓴다 */
async function link(ownerToken, ownerUid, carToken, carUid) {
  const code = newCode()
  madeCodes.push(code)
  const made = await createCode(carToken, code, { carUid })
  const batch = await commit(ownerToken, [claimWrite(code, ownerUid), readerWrite(ownerUid, carUid, code)])
  return { code, made, batch }
}

const profile = {
  schemaVersion: 1,
  name: '콩이',
  breed: '말티즈',
  sex: 'female',
  birthDate: '2020-03-01',
  birthDateIsEstimated: false,
  photoDataUrl: null,
  medical: { conditions: ['슬개골 탈구'], allergies: ['닭고기'], medications: [{ name: '관절약', dosage: '1일 1회' }] },
  updatedAt: '2026-11-06T09:00:00.000Z',
}
const profilePath = `/petProfiles/${owner.localId}`

// R14
const anonCode = await createCode(null, newCode(), { carUid: 'nobody' })
const anonProfile = await firestore('GET', profilePath)
check('R14', '로그인 안 한 요청은 연결 코드를 만들거나 프로필을 읽지 못한다', anonCode.status === 403 && anonProfile.status === 403, `코드 ${anonCode.status}, 프로필 ${anonProfile.status}`)

// R15 (F2: 같은 코드가 있으면 어떤 상태 코드인지)
const goodCode = newCode()
madeCodes.push(goodCode)
const madeOk = await createCode(carC.idToken, goodCode, { carUid: carC.localId })
const duplicate = await createCode(carC.idToken, goodCode, { carUid: carC.localId })
const forOther = await createCode(carC.idToken, newCode(), { carUid: carC2.localId })
const clientTimeCode = await createCode(carC.idToken, newCode(), { carUid: carC.localId, clientTime: true })
const prefilled = await createCode(carC.idToken, newCode(), { carUid: carC.localId, ownerUid: owner.localId })
const badId = await createCode(carC.idToken, 'AB10OI', { carUid: carC.localId })
check(
  'R15',
  `차는 자기 uid·서버 시각·빈 보호자로만 코드를 만든다 (같은 코드 다시 만들기: ${duplicate.status})`,
  madeOk.status === 200 && duplicate.status !== 200 && forOther.status === 403 && clientTimeCode.status === 403 && prefilled.status === 403 && badId.status === 403,
  `정상 ${madeOk.status}, 겹침 ${duplicate.status}, 남 ${forOther.status}, 차 시계 ${clientTimeCode.status}, 보호자 채움 ${prefilled.status}, 형식 ${badId.status}`,
)

// R18 (프로필은 R16·R19보다 먼저 만든다)
const noConsent = await firestore('PATCH', profilePath, { token: owner.idToken, body: { fields: toFields(profile) } })
const consent = await commit(owner.idToken, [
  {
    update: { name: `${DOC_NAME}/owners/${owner.localId}`, fields: toFields({ schemaVersion: 1, consentVersion: 1 }) },
    updateTransforms: [serverTime('consentedAt')],
    currentDocument: { exists: false },
  },
])
const saved = await firestore('PATCH', profilePath, { token: owner.idToken, body: { fields: toFields(profile) } })
const byOwner2 = await firestore('PATCH', profilePath, { token: owner2.idToken, body: { fields: toFields(profile) } })
const photoInProfile = await firestore('PATCH', profilePath, {
  token: owner.idToken,
  body: { fields: toFields({ ...profile, photoDataUrl: 'data:image/jpeg;base64,AA' }) },
})
const photoPath = `${profilePath}/private/photo`
const photoOk = await firestore('PATCH', photoPath, { token: owner.idToken, body: { fields: toFields({ photoDataUrl: 'data:image/jpeg;base64,/9j/4AAQ' }) } })
const photoBig = await firestore('PATCH', photoPath, {
  token: owner.idToken,
  body: { fields: toFields({ photoDataUrl: 'data:image/jpeg;base64,' + 'A'.repeat(400_000) }) },
})
const photoPng = await firestore('PATCH', photoPath, { token: owner.idToken, body: { fields: toFields({ photoDataUrl: 'data:image/png;base64,AA' }) } })
check(
  'R18',
  '동의 없이는 프로필을 못 만들고, 남의 프로필은 못 쓴다. 프로필의 사진은 null만, 사진 문서는 400,000자 이하 JPEG만',
  noConsent.status === 403 && consent.status === 200 && saved.status === 200 && byOwner2.status === 403 && photoInProfile.status === 403
    && photoOk.status === 200 && photoBig.status === 403 && photoPng.status === 403,
  `동의 전 ${noConsent.status}, 동의 ${consent.status}, 저장 ${saved.status}, 남 ${byOwner2.status}, 프로필 사진 ${photoInProfile.status}, 사진 ${photoOk.status}, 큰 사진 ${photoBig.status}, PNG ${photoPng.status}`,
)

// R16
const listCodes = await firestore('GET', '/linkCodes', { token: carC.idToken })
const listProfiles = await firestore('GET', '/petProfiles', { token: owner.idToken })
check('R16', '연결 코드·프로필은 목록으로 읽지 못한다', listCodes.status === 403 && listProfiles.status === 403, `코드 ${listCodes.status}, 프로필 ${listProfiles.status}`)

// R17
const expiredCode = newCode()
madeCodes.push(expiredCode)
await firestore('PATCH', `/linkCodes/${expiredCode}`, {
  token: ADMIN,
  body: { fields: toFields({ schemaVersion: 1, carUid: carC2.localId, createdAt: new Date(Date.now() - 11 * 60_000), ownerUid: null, claimedAt: null }) },
})
const expiredRead = await firestore('GET', `/linkCodes/${expiredCode}`, { token: owner.idToken })
const expiredClaim = await commit(owner.idToken, [claimWrite(expiredCode, owner.localId)])
const asOther = await commit(owner.idToken, [claimWrite(goodCode, owner2.localId)])
const unclaimedRead = await firestore('GET', `/linkCodes/${goodCode}`, { token: owner2.idToken })
const claimOnly = await commit(owner.idToken, [claimWrite(goodCode, owner.localId)])
const claimAgain = await commit(owner2.idToken, [claimWrite(goodCode, owner2.localId)])
const claimedReadByOther = await firestore('GET', `/linkCodes/${goodCode}`, { token: owner2.idToken })
check(
  'R17',
  '보호자는 만료 전·안 쓴 코드를 자기 uid·서버 시각으로 한 번만 claim한다. 10분 지난 코드는 읽지도 claim하지도 못한다',
  unclaimedRead.status === 200 && claimOnly.status === 200 && claimAgain.status === 403 && asOther.status === 403
    && expiredRead.status === 403 && expiredClaim.status === 403 && claimedReadByOther.status === 403,
  `claim 전 읽기 ${unclaimedRead.status}, claim ${claimOnly.status}, 다시 ${claimAgain.status}, 남 uid ${asOther.status}, 만료 읽기 ${expiredRead.status}, 만료 claim ${expiredClaim.status}, claim 뒤 남 읽기 ${claimedReadByOther.status}`,
)

// R19 (F1: claim + 허가 batch에서 getAfter)
const unclaimed = newCode()
madeCodes.push(unclaimed)
await createCode(carC.idToken, unclaimed, { carUid: carC.localId })
const readerWithoutClaim = await commit(owner.idToken, [readerWrite(owner.localId, carC.localId, unclaimed)])
const readerOtherCar = await commit(owner.idToken, [readerWrite(owner.localId, carC2.localId, goodCode)])
const readerByOwner2 = await commit(owner2.idToken, [readerWrite(owner2.localId, carC.localId, goodCode)])
const linked = await link(owner.idToken, owner.localId, carC.idToken, carC.localId)
check(
  'R19',
  '허가는 그 차의 코드를 이 보호자가 claim했을 때만 만든다. claim + 허가 한 번에 쓰기는 된다',
  readerWithoutClaim.status === 403 && readerOtherCar.status === 403 && readerByOwner2.status === 403 && linked.made.status === 200 && linked.batch.status === 200,
  `claim 없음 ${readerWithoutClaim.status}, 다른 차 ${readerOtherCar.status}, 다른 보호자 ${readerByOwner2.status}, 코드 ${linked.made.status}, batch ${linked.batch.status}`,
)

// R20
const carReads = await firestore('GET', profilePath, { token: carC.idToken })
const carPhoto = await firestore('GET', photoPath, { token: carC.idToken })
const carConsent = await firestore('GET', `/owners/${owner.localId}`, { token: carC.idToken })
const carListReaders = await firestore('GET', `${profilePath}/readers`, { token: carC.idToken })
const carWrites = await firestore('PATCH', profilePath, { token: carC.idToken, body: { fields: toFields(profile) } })
const car2Reads = await firestore('GET', profilePath, { token: carC2.idToken })
check(
  'R20',
  '연결된 차는 프로필을 읽는다. 사진·동의·허가 목록은 못 읽고 프로필을 못 쓴다. 연결 안 된 차는 못 읽는다',
  carReads.status === 200 && carReads.json?.fields?.name?.stringValue === '콩이' && carPhoto.status === 403 && carConsent.status === 403
    && carListReaders.status === 403 && carWrites.status === 403 && car2Reads.status === 403,
  `읽기 ${carReads.status}, 사진 ${carPhoto.status}, 동의 ${carConsent.status}, 허가 목록 ${carListReaders.status}, 쓰기 ${carWrites.status}, 다른 차 ${car2Reads.status}`,
)

// R24
const carCode = await firestore('GET', `/linkCodes/${linked.code}`, { token: carC.idToken })
const car2Code = await firestore('GET', `/linkCodes/${linked.code}`, { token: carC2.idToken })
const car2DeleteCode = await firestore('DELETE', `/linkCodes/${linked.code}`, { token: carC2.idToken })
const carDeleteCode = await firestore('DELETE', `/linkCodes/${linked.code}`, { token: carC.idToken })
const deletedCodeRead = await firestore('GET', `/linkCodes/${linked.code}`, { token: carC.idToken })
check(
  'R24',
  `차는 claim된 자기 코드를 읽어 보호자를 알고 지운다. 다른 차는 못 읽고 못 지운다 (지운 코드 다시 읽기: ${deletedCodeRead.status})`,
  carCode.status === 200 && carCode.json?.fields?.ownerUid?.stringValue === owner.localId && car2Code.status === 403 && car2DeleteCode.status === 403 && carDeleteCode.status === 200,
  `읽기 ${carCode.status}, 다른 차 읽기 ${car2Code.status}, 다른 차 지우기 ${car2DeleteCode.status}, 지우기 ${carDeleteCode.status}`,
)

// R21
const readerPath = `${profilePath}/readers/${carC.localId}`
const carUnlinks = await firestore('DELETE', readerPath, { token: carC.idToken })
const afterCarUnlink = await firestore('GET', profilePath, { token: carC.idToken })
check('R21', '차가 자기 허가를 지우면 그 뒤 프로필 읽기는 403이다', carUnlinks.status === 200 && afterCarUnlink.status === 403, `지우기 ${carUnlinks.status}, 읽기 ${afterCarUnlink.status}`)

// R22
const relinked = await link(owner.idToken, owner.localId, carC.idToken, carC.localId)
await firestore('DELETE', `/linkCodes/${relinked.code}`, { token: carC.idToken })
const car2DeletesReader = await firestore('DELETE', readerPath, { token: carC2.idToken })
const ownerListsReaders = await firestore('GET', `${profilePath}/readers`, { token: owner.idToken })
const ownerUnlinks = await firestore('DELETE', readerPath, { token: owner.idToken })
const afterOwnerUnlink = await firestore('GET', profilePath, { token: carC.idToken })
check(
  'R22',
  '보호자가 허가를 지워도 차의 읽기는 403이다. 다른 차는 남의 허가를 지우지 못한다. 보호자는 허가 목록을 본다',
  relinked.batch.status === 200 && car2DeletesReader.status === 403 && ownerListsReaders.status === 200 && (ownerListsReaders.json?.documents?.length ?? 0) === 1
    && ownerUnlinks.status === 200 && afterOwnerUnlink.status === 403,
  `다시 연결 ${relinked.batch.status}, 다른 차 지우기 ${car2DeletesReader.status}, 목록 ${ownerListsReaders.status}, 보호자 지우기 ${ownerUnlinks.status}, 읽기 ${afterOwnerUnlink.status}`,
)

// R23 (F4: 허가가 있는데 프로필이 없으면 404)
const third = await link(owner.idToken, owner.localId, carC.idToken, carC.localId)
await firestore('DELETE', `/linkCodes/${third.code}`, { token: carC.idToken })
const owner2Deletes = await firestore('DELETE', profilePath, { token: owner2.idToken })
const owner2DeletesPhoto = await firestore('DELETE', photoPath, { token: owner2.idToken })
const deletes = []
deletes.push(await firestore('DELETE', photoPath, { token: owner.idToken }))
deletes.push(await firestore('DELETE', profilePath, { token: owner.idToken }))
const missingProfile = await firestore('GET', profilePath, { token: carC.idToken })
deletes.push(await firestore('DELETE', readerPath, { token: owner.idToken }))
deletes.push(await firestore('DELETE', `/owners/${owner.localId}`, { token: owner.idToken }))
check(
  'R23',
  `보호자는 자기 프로필·사진·허가·동의를 모두 지운다. 다른 보호자는 못 지운다 (허가는 있고 프로필이 없을 때 차의 읽기: ${missingProfile.status})`,
  third.batch.status === 200 && owner2Deletes.status === 403 && owner2DeletesPhoto.status === 403 && deletes.every((r) => r.status === 200) && missingProfile.status === 404,
  `다른 보호자 ${owner2Deletes.status}/${owner2DeletesPhoto.status}, 지우기 ${deletes.map((r) => r.status).join('/')}, 프로필 없음 읽기 ${missingProfile.status}`,
)

// 정리 (관리자 토큰): 이 점검이 만든 연결 코드·보호자 문서
for (const code of madeCodes) await firestore('DELETE', `/linkCodes/${code}`, { token: ADMIN })
for (const path of [readerPath, photoPath, profilePath, `/owners/${owner.localId}`]) await firestore('DELETE', path, { token: ADMIN })

// 정리 (관리자 토큰)
if (casePath) await firestore('DELETE', casePath, { token: ADMIN })
await firestore('DELETE', `/hospitals/${HID}/staff/${staff.localId}`, { token: ADMIN })

console.log(failures === 0 ? '\n규칙 점검 모두 통과.' : `\n규칙 점검 실패 ${failures}건.`)
console.log(`(대상: ${DOCS})`)
process.exit(failures === 0 ? 0 : 1)
