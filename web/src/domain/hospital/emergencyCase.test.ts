import { describe, expect, it } from 'vitest'
import example from '../../../../docs/data/emergency-case.example.json'
import { caseFromJson } from './emergencyCase'

describe('caseFromJson (docs/data/emergency-case.md)', () => {
  it('예시 환자 건을 읽는다', () => {
    const view = caseFromJson('c1', example)
    expect(view.readable).toBe(true)
    expect(view.status).toBe('en_route')
    expect(view.remainingMinutes).toBe(12)
    expect(view.etaAt).toEqual(new Date('2026-11-06T05:12:00Z'))
    expect(view.pet?.name).toBe('보리')
    expect(view.pet?.medical.conditions).toEqual(['심장질환'])
    expect(view.symptoms).toEqual([
      { code: 'BREATHING_DIFFICULTY', text: '호흡 곤란' },
      { code: 'COUGH', text: '기침' },
    ])
  })

  it('예시에는 사진과 차 위치가 없다', () => {
    expect('photoDataUrl' in example.card.pet).toBe(false)
    const keys = JSON.stringify(example)
    expect(keys).not.toMatch(/latitude|longitude|photo/)
  })

  it('label이 없으면 code를 보인다', () => {
    const view = caseFromJson('c1', { ...example, card: { ...example.card, symptoms: [{ code: 'COUGH' }, { code: 'X', label: ' ' }] } })
    expect(view.symptoms.map((s) => s.text)).toEqual(['COUGH', 'X'])
  })

  it('형식 버전이 다르면 읽지 못한 건이다', () => {
    const view = caseFromJson('c1', { ...example, schemaVersion: 2 })
    expect(view.readable).toBe(false)
    expect(view.pet).toBeNull()
  })

  it('모르는 상태는 오는 중이고, 반려견 정보를 읽지 못하면 null이다', () => {
    const view = caseFromJson('c1', { ...example, status: 'lost', card: { pet: { name: '' }, symptoms: 'x' } })
    expect(view.status).toBe('en_route')
    expect(view.pet).toBeNull()
    expect(view.symptoms).toEqual([])
  })

  it('남은 시간이 null이면 경로를 찾는 중이다', () => {
    const view = caseFromJson('c1', { ...example, remainingMinutes: null, etaAt: null })
    expect(view.remainingMinutes).toBeNull()
    expect(view.etaAt).toBeNull()
  })
})
