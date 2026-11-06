// 시연 병원 5곳과 병원별 코드를 에뮬레이터 Firestore에 넣는다 (계획 012 단계 10).
// 값의 기준: docs/data/hospitals.sample.json. 코드: seed/join-codes.emulator.json (local 파일이 있으면 그것).
//   npm --prefix firebase run seed            병원 문서를 덮어쓴다 (환자 건은 그대로)
//   npm --prefix firebase run seed -- --reset 먼저 모두 지운다
import { existsSync, readFileSync } from 'node:fs'
import { ADMIN, firestore, requireEmulators, resetFirestore, toFields } from './emulator.mjs'

const root = new URL('../../', import.meta.url)
const samples = JSON.parse(readFileSync(new URL('docs/data/hospitals.sample.json', root), 'utf8'))
const localCodes = new URL('firebase/seed/join-codes.local.json', root)
const codesFile = existsSync(localCodes) ? localCodes : new URL('firebase/seed/join-codes.emulator.json', root)
const { codes } = JSON.parse(readFileSync(codesFile, 'utf8'))

await requireEmulators()
if (process.argv.includes('--reset')) {
  await resetFirestore()
  console.log('Firestore를 비웠다.')
}

const now = Date.now()
for (const entry of samples.hospitals) {
  const updatedAt = new Date(now - entry.seedUpdatedMinutesAgo * 60_000)
  const document = { ...entry.document, status: { ...entry.document.status, updatedAt } }
  const put = await firestore('PATCH', `/hospitals/${entry.id}`, { token: ADMIN, body: { fields: toFields(document) } })
  if (put.status !== 200) throw new Error(`${entry.id} 시드 실패: HTTP ${put.status} ${JSON.stringify(put.json)}`)
  const code = codes[entry.id]
  if (typeof code !== 'string') throw new Error(`${entry.id}의 코드가 없다`)
  const secret = await firestore('PATCH', `/hospitalSecrets/${entry.id}`, { token: ADMIN, body: { fields: toFields({ joinCode: code }) } })
  if (secret.status !== 200) throw new Error(`${entry.id} 코드 시드 실패: HTTP ${secret.status}`)
  console.log(`넣음 ${entry.id}  병원 웹: http://localhost:5173/hospital/?h=${entry.id}&code=${code}`)
}
console.log(`시연 병원 ${samples.hospitals.length}곳을 넣었다 (코드: ${codesFile.pathname.split('/').pop()}).`)
