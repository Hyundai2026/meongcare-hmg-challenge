import { describe, expect, it } from 'vitest'
import samples from '../../../../docs/data/hospitals.sample.json'
import { dateFrom, hospitalFromJson, statusFromJson } from './hospitalStatus'

// 기준: docs/data/hospital-status.md "필드", "읽는 쪽 규칙 (차 앱, 병원 웹)"
const at = '2026-11-06T05:00:00Z'
const goodStatus = { waitMinutes: 10, congestion: 'normal', acceptingEmergency: true, updatedAt: at }
const doc = { ...samples.hospitals[0].document, status: goodStatus }

describe('hospitalFromJson 경계 (읽는 쪽 규칙)', () => {
  it.each([
    ['위도가 90보다 큼', { latitude: 90.000001 }],
    ['위도가 -90보다 작음', { latitude: -90.000001 }],
    ['경도가 180보다 큼', { longitude: 180.000001 }],
    ['경도가 -180보다 작음', { longitude: -180.000001 }],
    ['위도가 NaN', { latitude: Number.NaN }],
    ['경도가 무한대', { longitude: Number.POSITIVE_INFINITY }],
    ['위도가 문자열', { latitude: '37.367' }],
    ['경도가 null', { longitude: null }],
    ['위도가 없음', { latitude: undefined }],
  ])('좌표가 틀리면(%s) 그 병원을 건너뛴다', (_, change) => {
    expect(hospitalFromJson('demo-fast', { ...doc, ...change })).toBeNull()
  })

  it('좌표의 경계 값(±90, ±180)은 받는다', () => {
    expect(hospitalFromJson('a', { ...doc, latitude: 90, longitude: 180 })).not.toBeNull()
    expect(hospitalFromJson('a', { ...doc, latitude: -90, longitude: -180 })).not.toBeNull()
  })

  it('형식 버전이 없거나 문자열이면 건너뛴다', () => {
    const noVersion: Record<string, unknown> = { ...doc }
    delete noVersion.schemaVersion
    expect(hospitalFromJson('a', noVersion)).toBeNull()
    expect(hospitalFromJson('a', { ...doc, schemaVersion: '1' })).toBeNull()
  })

  it('이름이 없거나 문자열이 아니면 건너뛴다', () => {
    expect(hospitalFromJson('a', { ...doc, name: undefined })).toBeNull()
    expect(hospitalFromJson('a', { ...doc, name: 1 })).toBeNull()
    expect(hospitalFromJson('a', { ...doc, name: '' })).toBeNull()
  })

  it('전화번호가 문자열이 아니거나 비면 전화번호 없음이고 병원은 남는다', () => {
    for (const phone of [null, undefined, 20000000, '', '   ']) {
      const hospital = hospitalFromJson('a', { ...doc, phone })
      expect(hospital).not.toBeNull()
      expect(hospital?.phone).toBeNull()
    }
  })

  it('isSample이 없으면 시연용이고 false면 실제 병원이다', () => {
    expect(hospitalFromJson('a', { ...doc, isSample: undefined })?.isSample).toBe(true)
    expect(hospitalFromJson('a', { ...doc, isSample: false })?.isSample).toBe(false)
  })

  it('status가 null이거나 없으면 대기 정보 없음이고 병원은 남는다', () => {
    expect(hospitalFromJson('a', { ...doc, status: null })?.status).toBeNull()
    expect(hospitalFromJson('a', { ...doc, status: undefined })?.status).toBeNull()
    expect(hospitalFromJson('a', { ...doc, status: [] })?.status).toBeNull()
  })

  it('모르는 필드는 문서와 status 모두에서 무시하고 읽는다', () => {
    const hospital = hospitalFromJson('a', { ...doc, openingHours: '24h', status: { ...goodStatus, note: 'x' } })
    expect(hospital?.status?.waitMinutes).toBe(10)
  })

  it('배열이나 null 문서는 건너뛴다', () => {
    expect(hospitalFromJson('a', null)).toBeNull()
    expect(hospitalFromJson('a', [doc])).toBeNull()
  })
})

describe('statusFromJson 경계', () => {
  it.each(['waitMinutes', 'congestion', 'acceptingEmergency', 'updatedAt'])('%s가 없으면 status 전체가 대기 정보 없음이다', (key) => {
    const value: Record<string, unknown> = { ...goodStatus }
    delete value[key]
    expect(statusFromJson(value)).toBeNull()
  })

  it.each([
    ['문자열 대기 분', { waitMinutes: '10' }],
    ['NaN 대기 분', { waitMinutes: Number.NaN }],
    ['무한대 대기 분', { waitMinutes: Number.POSITIVE_INFINITY }],
    ['대문자 혼잡도', { congestion: 'Normal' }],
    ['빈 혼잡도', { congestion: '' }],
    ['숫자 응급 접수', { acceptingEmergency: 1 }],
    ['숫자 알린 시각', { updatedAt: 1762405200000 }],
    ['빈 문자열 알린 시각', { updatedAt: '' }],
  ])('%s이면 대기 정보 없음이다', (_, change) => {
    expect(statusFromJson({ ...goodStatus, ...change })).toBeNull()
  })

  it('혼잡도 세 값을 모두 읽는다', () => {
    for (const congestion of ['low', 'normal', 'high'] as const) {
      expect(statusFromJson({ ...goodStatus, congestion })?.congestion).toBe(congestion)
    }
  })

  it('소수 초가 있는 시각과 Firestore Timestamp를 읽는다', () => {
    expect(statusFromJson({ ...goodStatus, updatedAt: '2026-11-06T05:00:00.123Z' })?.updatedAt).toEqual(new Date('2026-11-06T05:00:00.123Z'))
    const date = new Date(at)
    expect(statusFromJson({ ...goodStatus, updatedAt: { toDate: () => date } })?.updatedAt).toEqual(date)
  })
})

describe('dateFrom 경계', () => {
  it('읽을 수 없는 Date나 toDate 결과는 null이다', () => {
    expect(dateFrom(new Date('x'))).toBeNull()
    expect(dateFrom({ toDate: () => new Date('x') })).toBeNull()
    expect(dateFrom({ toDate: () => 'not a date' })).toBeNull()
    expect(dateFrom(null)).toBeNull()
    expect(dateFrom(undefined)).toBeNull()
  })
})
