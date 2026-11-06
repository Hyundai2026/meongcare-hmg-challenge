import { describe, expect, it } from 'vitest'
import example from '../../../../docs/data/emergency-case.example.json'
import { caseFromJson } from './emergencyCase'
import { etaText } from './deskText'

// 기준: docs/data/emergency-case.md "필드", "card.pet", "card.symptoms", "읽는 쪽 규칙 (병원 웹)"
describe('caseFromJson 경계', () => {
  it('문서가 객체가 아니면 읽지 못한 건이고 id는 남아 지울 수 있다', () => {
    for (const value of [null, undefined, 'x', 1, [example]]) {
      const view = caseFromJson('c1', value)
      expect(view.readable).toBe(false)
      expect(view.id).toBe('c1')
    }
  })

  it('형식 버전이 없거나 문자열이면 읽지 못한 건이다', () => {
    const noVersion: Record<string, unknown> = { ...example }
    delete noVersion.schemaVersion
    expect(caseFromJson('c1', noVersion).readable).toBe(false)
    expect(caseFromJson('c1', { ...example, schemaVersion: '1' }).readable).toBe(false)
  })

  it('도착과 취소 상태를 읽고 상태가 없으면 오는 중이다', () => {
    expect(caseFromJson('c1', { ...example, status: 'arrived' }).status).toBe('arrived')
    expect(caseFromJson('c1', { ...example, status: 'cancelled' }).status).toBe('cancelled')
    expect(caseFromJson('c1', { ...example, status: undefined }).status).toBe('en_route')
    expect(caseFromJson('c1', { ...example, status: 'ARRIVED' }).status).toBe('en_route')
  })

  it('남은 분이 숫자가 아니면 null이고 도착 예정이 없으면 경로를 찾는 중으로 보인다', () => {
    for (const remainingMinutes of ['12', Number.NaN, Number.POSITIVE_INFINITY, null, undefined]) {
      expect(caseFromJson('c1', { ...example, remainingMinutes }).remainingMinutes).toBeNull()
    }
    const view = caseFromJson('c1', { ...example, remainingMinutes: null, etaAt: null })
    expect(etaText(view.etaAt, new Date('2026-11-06T05:00:00Z'))).toBe('경로를 찾는 중')
  })

  it('도착 예정 시각을 읽을 수 없으면 null이다', () => {
    expect(caseFromJson('c1', { ...example, etaAt: 'soon' }).etaAt).toBeNull()
    expect(caseFromJson('c1', { ...example, etaAt: 1762405920000 }).etaAt).toBeNull()
  })

  it('Firestore Timestamp로 온 시각을 읽는다', () => {
    const eta = new Date('2026-11-06T05:12:00Z')
    const created = new Date('2026-11-06T05:00:00Z')
    const acknowledged = new Date('2026-11-06T05:03:00Z')
    const view = caseFromJson('c1', {
      ...example,
      etaAt: { toDate: () => eta },
      createdAt: { toDate: () => created },
      acknowledgedAt: { toDate: () => acknowledged },
    })
    expect(view.etaAt).toEqual(eta)
    expect(view.createdAt).toEqual(created)
    expect(view.acknowledgedAt).toEqual(acknowledged)
  })

  it('카드가 없거나 객체가 아니면 반려견 정보 없음, 증상 없음이다', () => {
    for (const card of [undefined, null, 'x', []]) {
      const view = caseFromJson('c1', { ...example, card })
      expect(view.readable).toBe(true)
      expect(view.pet).toBeNull()
      expect(view.symptoms).toEqual([])
    }
  })

  it('반려견 정보에 사진이 들어 있어도 보이지 않는다', () => {
    const card = { ...example.card, pet: { ...example.card.pet, photoDataUrl: 'data:image/jpeg;base64,AAAA' } }
    const view = caseFromJson('c1', { ...example, card })
    expect(view.pet?.name).toBe('보리')
    expect(view.pet?.photoDataUrl).toBeNull()
  })

  it('반려견 정보가 pet-profile 규칙으로 읽히지 않으면 null이고 증상은 그대로 보인다', () => {
    const card = { ...example.card, pet: { ...example.card.pet, birthDate: '2023-02-30' } }
    const view = caseFromJson('c1', { ...example, card })
    expect(view.pet).toBeNull()
    expect(view.symptoms.map((s) => s.text)).toEqual(['호흡 곤란', '기침'])
  })

  it('증상의 객체가 아닌 항목과 code가 문자열이 아닌 항목은 빼고 순서는 그대로 둔다', () => {
    const symptoms = ['COUGH', null, { label: '기침' }, { code: 1, label: 'x' }, { code: 'SEIZURE', label: '발작' }, { code: 'COUGH', label: 5 }]
    const view = caseFromJson('c1', { ...example, card: { ...example.card, symptoms } })
    expect(view.symptoms).toEqual([
      { code: 'SEIZURE', text: '발작' },
      { code: 'COUGH', text: 'COUGH' },
    ])
  })

  it('모르는 필드는 무시하고 읽는다', () => {
    const view = caseFromJson('c1', { ...example, hospitalNote: 'x', card: { ...example.card, extra: true } })
    expect(view.readable).toBe(true)
    expect(view.pet?.name).toBe('보리')
  })

  it('예시 건은 확인 시각이 없다', () => {
    expect(caseFromJson('c1', example).acknowledgedAt).toBeNull()
  })
})
