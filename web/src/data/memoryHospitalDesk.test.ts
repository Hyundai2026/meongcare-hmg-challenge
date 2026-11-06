import { describe, expect, it } from 'vitest'
import samples from '../../../docs/data/hospitals.sample.json'
import example from '../../../docs/data/emergency-case.example.json'
import type { CaseView } from '../domain/hospital/emergencyCase'
import type { HospitalSummary } from '../domain/hospital/hospitalStatus'
import { createMemoryHospitalDesk } from './memoryHospitalDesk'
import { EXAMPLE_CASE_DOCUMENT, SAMPLE_HOSPITALS } from './sampleHospitals'

const start = new Date('2026-11-06T05:00:00Z')

describe('sampleHospitals', () => {
  it('docs/data/hospitals.sample.json과 같다', () => {
    expect(SAMPLE_HOSPITALS).toEqual(samples.hospitals)
  })

  it('예시 환자 건이 docs/data/emergency-case.example.json과 같다', () => {
    expect(EXAMPLE_CASE_DOCUMENT).toEqual(example)
  })
})

describe('createMemoryHospitalDesk', () => {
  it('시연 병원 5곳을 알린 시각이 지금 - N분인 채로 준다', async () => {
    const desk = createMemoryHospitalDesk(() => start)
    const result = await desk.listHospitals()
    if (!result.ok) throw new Error('목록 실패')
    expect(result.value.map((h) => h.id)).toEqual(['demo-fast', 'demo-main', 'demo-near', 'demo-stale', 'demo-closed'])
    expect(result.value[3].status?.updatedAt).toEqual(new Date(start.getTime() - 90 * 60_000))
    expect(desk.isLocalOnly).toBe(true)
  })

  it('상태를 알리면 지금 시각으로 바뀌고 구독자에게 알린다', async () => {
    let clock = start
    const desk = createMemoryHospitalDesk(() => clock)
    const seen: (HospitalSummary | null)[] = []
    const stop = desk.watchHospital('demo-fast', (h) => seen.push(h))
    clock = new Date(start.getTime() + 60_000)
    await desk.updateStatus('demo-fast', { waitMinutes: 30, congestion: 'high', acceptingEmergency: false })
    expect(seen).toHaveLength(2)
    expect(seen[1]?.status).toEqual({ waitMinutes: 30, congestion: 'high', acceptingEmergency: false, updatedAt: clock })
    stop()
    await desk.updateStatus('demo-fast', { waitMinutes: 0, congestion: 'low', acceptingEmergency: true })
    expect(seen).toHaveLength(2)
  })

  it('병원마다 예시 환자 1건이 지금 + 12분 도착으로 있고, 완료하면 지워진다', async () => {
    const desk = createMemoryHospitalDesk(() => start)
    const seen: CaseView[][] = []
    desk.watchCases('demo-main', (c) => seen.push(c))
    expect(seen[0]).toHaveLength(1)
    expect(seen[0][0].isExample).toBe(true)
    expect(seen[0][0].pet?.name).toBe('보리')
    expect(seen[0][0].etaAt).toEqual(new Date(start.getTime() + 12 * 60_000))
    await desk.completeCase('demo-main', seen[0][0].id)
    expect(seen[1]).toEqual([])
  })

  it('없는 병원은 열 수 없다', async () => {
    const desk = createMemoryHospitalDesk(() => start)
    expect(await desk.open('nope', null)).toEqual({ ok: false, reason: 'not-found' })
    expect(await desk.open('demo-fast', null)).toEqual({ ok: true, value: undefined })
  })
})
