import { describe, expect, it } from 'vitest'
import samples from '../../../../docs/data/hospitals.sample.json'
import { dateFrom, hospitalFromJson, statusFromJson } from './hospitalStatus'

const at = '2026-11-06T05:00:00Z'
const goodStatus = { waitMinutes: 10, congestion: 'normal', acceptingEmergency: true, updatedAt: at }

describe('hospitalFromJson (docs/data/hospital-status.md 읽는 쪽 규칙)', () => {
  it('시연 병원 문서를 읽는다', () => {
    for (const entry of samples.hospitals) {
      const hospital = hospitalFromJson(entry.id, { ...entry.document, status: { ...entry.document.status, updatedAt: at } })
      expect(hospital?.name).toBe(entry.document.name)
      expect(hospital?.status?.waitMinutes).toBe(entry.document.status.waitMinutes)
      expect(hospital?.isSample).toBe(true)
    }
  })

  it('형식 버전이 다르거나 이름이 비면 건너뛴다', () => {
    const doc = samples.hospitals[0].document
    expect(hospitalFromJson('a', { ...doc, schemaVersion: 2 })).toBeNull()
    expect(hospitalFromJson('a', { ...doc, name: '  ' })).toBeNull()
    expect(hospitalFromJson('a', 'x')).toBeNull()
  })

  it('전화번호가 비면 없음, isSample이 불리언이 아니면 시연용으로 본다', () => {
    const doc = samples.hospitals[0].document
    const hospital = hospitalFromJson('a', { ...doc, phone: '', isSample: 'yes' })
    expect(hospital?.phone).toBeNull()
    expect(hospital?.isSample).toBe(true)
  })
})

describe('statusFromJson', () => {
  it('맞는 값을 읽는다', () => {
    expect(statusFromJson(goodStatus)).toEqual({ ...goodStatus, updatedAt: new Date(at) })
  })

  it.each([
    ['대기 분 범위 밖', { ...goodStatus, waitMinutes: 601 }],
    ['대기 분 음수', { ...goodStatus, waitMinutes: -1 }],
    ['대기 분 소수', { ...goodStatus, waitMinutes: 1.5 }],
    ['모르는 혼잡도', { ...goodStatus, congestion: 'busy' }],
    ['응급 접수가 불리언이 아님', { ...goodStatus, acceptingEmergency: 'true' }],
    ['알린 시각 없음', { ...goodStatus, updatedAt: undefined }],
    ['알린 시각을 읽을 수 없음', { ...goodStatus, updatedAt: 'yesterday' }],
    ['null', null],
  ])('%s이면 대기 정보 없음이다', (_, value) => {
    expect(statusFromJson(value)).toBeNull()
  })

  it('경계 값 0분과 600분은 받는다', () => {
    expect(statusFromJson({ ...goodStatus, waitMinutes: 0 })?.waitMinutes).toBe(0)
    expect(statusFromJson({ ...goodStatus, waitMinutes: 600 })?.waitMinutes).toBe(600)
  })
})

describe('dateFrom', () => {
  it('Date, ISO 문자열, toDate()가 있는 객체(Firestore Timestamp)를 받는다', () => {
    const date = new Date(at)
    expect(dateFrom(date)).toBe(date)
    expect(dateFrom(at)).toEqual(date)
    expect(dateFrom({ toDate: () => date })).toEqual(date)
    expect(dateFrom(12345)).toBeNull()
  })
})
