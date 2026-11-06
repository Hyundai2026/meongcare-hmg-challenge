import { describe, expect, it } from 'vitest'
import type { CaseView } from '../domain/hospital/emergencyCase'
import { createMemoryHospitalDesk } from './memoryHospitalDesk'

const start = new Date('2026-11-06T05:00:00Z')

describe('createMemoryHospitalDesk acknowledgeCase (계획 013)', () => {
  it('확인하면 지금 시각이 확인 시각으로 구독자에게 온다', async () => {
    let clock = start
    const desk = createMemoryHospitalDesk(() => clock)
    const seen: CaseView[][] = []
    const stop = desk.watchCases('demo-fast', (c) => seen.push(c))
    const example = seen[0][0]
    expect(example.acknowledgedAt).toBeNull()

    clock = new Date(start.getTime() + 90_000)
    const result = await desk.acknowledgeCase('demo-fast', example.id)
    expect(result).toEqual({ ok: true, value: undefined })
    expect(seen).toHaveLength(2)
    expect(seen[1][0].acknowledgedAt).toEqual(clock)
    stop()
  })

  it('두 번 눌러도 처음 확인 시각 그대로다', async () => {
    let clock = start
    const desk = createMemoryHospitalDesk(() => clock)
    const seen: CaseView[][] = []
    desk.watchCases('demo-fast', (c) => seen.push(c))
    const id = seen[0][0].id
    clock = new Date(start.getTime() + 60_000)
    await desk.acknowledgeCase('demo-fast', id)
    const first = clock
    clock = new Date(start.getTime() + 120_000)
    const again = await desk.acknowledgeCase('demo-fast', id)
    expect(again.ok).toBe(true)
    expect(seen.at(-1)?.[0].acknowledgedAt).toEqual(first)
  })

  it('없는 건은 not-found다', async () => {
    const desk = createMemoryHospitalDesk(() => start)
    expect(await desk.acknowledgeCase('demo-fast', 'nope')).toEqual({ ok: false, reason: 'not-found' })
  })
})
