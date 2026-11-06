import { describe, expect, it } from 'vitest'
import { isNewCase, newCaseCount } from '../domain/hospital/caseAck'
import type { CaseView } from '../domain/hospital/emergencyCase'
import { createMemoryHospitalDesk } from './memoryHospitalDesk'

// 기준: web/src/domain/hospital/hospitalDesk.ts acknowledgeCase (acknowledgedAt = 지금 시각, 이미 확인된 건은 처음 시각 그대로),
// 계획 013 설계 결정 9 (메모리는 now()를 쓰고, 이미 확인된 건은 시각을 바꾸지 않고 ok, 없는 건은 not-found),
// "병원 웹만" 확인 절차 (예시 환자가 새 환자 → [카드 확인] → 새 환자 아님)
const start = new Date('2026-11-06T05:00:00Z')
const MIN = 60_000

describe('createMemoryHospitalDesk acknowledgeCase 경계 (계획 013)', () => {
  it('완료로 지운 건은 확인할 수 없고 not-found다', async () => {
    const desk = createMemoryHospitalDesk(() => start)
    const seen: CaseView[][] = []
    desk.watchCases('demo-fast', (c) => seen.push(c))
    const id = seen[0][0].id
    await desk.completeCase('demo-fast', id)
    const emitted = seen.length
    expect(await desk.acknowledgeCase('demo-fast', id)).toEqual({ ok: false, reason: 'not-found' })
    expect(seen).toHaveLength(emitted)
  })

  it('한 병원의 확인은 다른 병원의 건과 구독자에 닿지 않는다', async () => {
    let clock = start
    const desk = createMemoryHospitalDesk(() => clock)
    const fast: CaseView[][] = []
    const main: CaseView[][] = []
    desk.watchCases('demo-fast', (c) => fast.push(c))
    desk.watchCases('demo-main', (c) => main.push(c))
    clock = new Date(start.getTime() + MIN)
    await desk.acknowledgeCase('demo-fast', fast[0][0].id)
    expect(main).toHaveLength(1)
    expect(main[0][0].acknowledgedAt).toBeNull()
    // 다른 병원 id로 같은 건을 확인하려 하면 그 병원에는 그 건이 없다
    expect(await desk.acknowledgeCase('demo-main', fast[0][0].id)).toEqual({ ok: false, reason: 'not-found' })
  })

  it('확인한 뒤 새로 구독해도 확인 시각이 보인다', async () => {
    let clock = start
    const desk = createMemoryHospitalDesk(() => clock)
    const first: CaseView[][] = []
    const stop = desk.watchCases('demo-fast', (c) => first.push(c))
    const id = first[0][0].id
    stop()
    clock = new Date(start.getTime() + 3 * MIN)
    expect(await desk.acknowledgeCase('demo-fast', id)).toEqual({ ok: true, value: undefined })
    const later: CaseView[][] = []
    desk.watchCases('demo-fast', (c) => later.push(c))
    expect(later[0][0].acknowledgedAt).toEqual(clock)
  })

  it('구독을 끊은 쪽에는 확인을 알리지 않는다', async () => {
    const desk = createMemoryHospitalDesk(() => start)
    const seen: CaseView[][] = []
    const stop = desk.watchCases('demo-fast', (c) => seen.push(c))
    stop()
    await desk.acknowledgeCase('demo-fast', seen[0][0].id)
    expect(seen).toHaveLength(1)
  })

  it('먼저 받은 목록은 바뀌지 않고 확인은 새 목록으로 온다', async () => {
    let clock = start
    const desk = createMemoryHospitalDesk(() => clock)
    const seen: CaseView[][] = []
    desk.watchCases('demo-fast', (c) => seen.push(c))
    const before = seen[0]
    const beforeCase = before[0]
    clock = new Date(start.getTime() + MIN)
    await desk.acknowledgeCase('demo-fast', beforeCase.id)
    expect(beforeCase.acknowledgedAt).toBeNull()
    expect(seen[1]).not.toBe(before)
    expect(seen[1][0]).not.toBe(beforeCase)
    expect(seen[1][0].acknowledgedAt).toEqual(clock)
  })

  it('확인 말고 다른 내용은 그대로다', async () => {
    let clock = start
    const desk = createMemoryHospitalDesk(() => clock)
    const seen: CaseView[][] = []
    desk.watchCases('demo-fast', (c) => seen.push(c))
    clock = new Date(start.getTime() + MIN)
    await desk.acknowledgeCase('demo-fast', seen[0][0].id)
    expect({ ...seen[1][0], acknowledgedAt: null }).toEqual(seen[0][0])
  })

  it('예시 환자는 처음에 새 환자이고 확인하면 새 환자 수가 0이 된다', async () => {
    const desk = createMemoryHospitalDesk(() => start)
    const seen: CaseView[][] = []
    desk.watchCases('demo-fast', (c) => seen.push(c))
    expect(newCaseCount(seen[0])).toBe(1)
    expect(isNewCase(seen[0][0])).toBe(true)
    await desk.acknowledgeCase('demo-fast', seen[0][0].id)
    expect(newCaseCount(seen.at(-1)!)).toBe(0)
  })

  it('확인을 동시에 두 번 보내도 확인 시각은 처음 한 번의 시각이다', async () => {
    let clock = start
    const desk = createMemoryHospitalDesk(() => clock)
    const seen: CaseView[][] = []
    desk.watchCases('demo-fast', (c) => seen.push(c))
    const id = seen[0][0].id
    clock = new Date(start.getTime() + MIN)
    const first = desk.acknowledgeCase('demo-fast', id)
    clock = new Date(start.getTime() + 2 * MIN)
    const second = desk.acknowledgeCase('demo-fast', id)
    expect(await first).toEqual({ ok: true, value: undefined })
    expect(await second).toEqual({ ok: true, value: undefined })
    expect(seen.at(-1)![0].acknowledgedAt).toEqual(new Date(start.getTime() + MIN))
  })
})
