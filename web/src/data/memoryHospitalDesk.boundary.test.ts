import { describe, expect, it } from 'vitest'
import type { CaseView } from '../domain/hospital/emergencyCase'
import { isStale } from '../domain/hospital/deskText'
import type { HospitalSummary } from '../domain/hospital/hospitalStatus'
import { createMemoryHospitalDesk } from './memoryHospitalDesk'

// 기준: web/src/domain/hospital/hospitalDesk.ts(인터페이스: 실패는 결과 값, 반환값을 부르면 그만 받는다,
// updatedAt은 저장소가 채운다), 계획 012 "설계 결정 7"(메모리 모드 = 시연 병원 5곳 + 예시 환자 1건, etaAt = 지금 + 12분,
// [완료] = 지우기, 메모리 구현은 코드를 보지 않는다)
const start = new Date('2026-11-06T05:00:00Z')
const MIN = 60_000

async function hospitalsOf(desk: ReturnType<typeof createMemoryHospitalDesk>): Promise<HospitalSummary[]> {
  const result = await desk.listHospitals()
  if (!result.ok) throw new Error('목록 실패')
  return result.value
}

describe('createMemoryHospitalDesk 경계', () => {
  it('시연 병원 5곳의 상태가 형식 문서 표와 같다 (정보 지난 곳, 응급 휴진 곳 포함)', async () => {
    const hospitals = await hospitalsOf(createMemoryHospitalDesk(() => start))
    const byId = new Map(hospitals.map((h) => [h.id, h]))
    expect(isStale(byId.get('demo-stale')!.status!.updatedAt, start)).toBe(true)
    expect(byId.get('demo-closed')!.status!.acceptingEmergency).toBe(false)
    expect(hospitals.filter((h) => h.id !== 'demo-stale').every((h) => !isStale(h.status!.updatedAt, start))).toBe(true)
    expect(hospitals.every((h) => h.isSample)).toBe(true)
  })

  it('없는 병원의 상태를 알리면 not-found이고 아무에게도 알리지 않는다', async () => {
    const desk = createMemoryHospitalDesk(() => start)
    const seen: (HospitalSummary | null)[] = []
    desk.watchHospital('demo-fast', (h) => seen.push(h))
    expect(await desk.updateStatus('nope', { waitMinutes: 10, congestion: 'low', acceptingEmergency: true })).toEqual({ ok: false, reason: 'not-found' })
    expect(seen).toHaveLength(1)
    expect((await hospitalsOf(desk)).map((h) => h.id)).not.toContain('nope')
  })

  it('없는 병원을 구독하면 null을 받는다', () => {
    const seen: (HospitalSummary | null)[] = []
    createMemoryHospitalDesk(() => start).watchHospital('nope', (h) => seen.push(h))
    expect(seen).toEqual([null])
  })

  it('한 병원의 상태를 바꿔도 다른 병원 구독자에게는 알리지 않는다', async () => {
    const desk = createMemoryHospitalDesk(() => start)
    const fast: (HospitalSummary | null)[] = []
    const main: (HospitalSummary | null)[] = []
    desk.watchHospital('demo-fast', (h) => fast.push(h))
    desk.watchHospital('demo-main', (h) => main.push(h))
    await desk.updateStatus('demo-fast', { waitMinutes: 45, congestion: 'high', acceptingEmergency: true })
    expect(fast).toHaveLength(2)
    expect(main).toHaveLength(1)
  })

  it('같은 병원 구독자가 둘이면 둘 다 받고 하나가 멈춰도 다른 하나는 계속 받는다', async () => {
    const desk = createMemoryHospitalDesk(() => start)
    const a: (HospitalSummary | null)[] = []
    const b: (HospitalSummary | null)[] = []
    const stopA = desk.watchHospital('demo-fast', (h) => a.push(h))
    desk.watchHospital('demo-fast', (h) => b.push(h))
    await desk.updateStatus('demo-fast', { waitMinutes: 20, congestion: 'normal', acceptingEmergency: true })
    stopA()
    await desk.updateStatus('demo-fast', { waitMinutes: 30, congestion: 'normal', acceptingEmergency: true })
    expect(a).toHaveLength(2)
    expect(b).toHaveLength(3)
    expect(b[2]?.status?.waitMinutes).toBe(30)
  })

  it('알린 상태는 목록에도 보이고 알린 시각은 저장소의 지금이다', async () => {
    let clock = start
    const desk = createMemoryHospitalDesk(() => clock)
    clock = new Date(start.getTime() + 7 * MIN)
    await desk.updateStatus('demo-stale', { waitMinutes: 0, congestion: 'low', acceptingEmergency: false })
    const stale = (await hospitalsOf(desk)).find((h) => h.id === 'demo-stale')!
    expect(stale.status).toEqual({ waitMinutes: 0, congestion: 'low', acceptingEmergency: false, updatedAt: clock })
    expect(isStale(stale.status!.updatedAt, clock)).toBe(false)
  })

  it('환자 구독을 멈추면 더 받지 않는다', async () => {
    const desk = createMemoryHospitalDesk(() => start)
    const seen: CaseView[][] = []
    const stop = desk.watchCases('demo-fast', (c) => seen.push(c))
    stop()
    await desk.completeCase('demo-fast', seen[0][0].id)
    expect(seen).toHaveLength(1)
  })

  it('예시 환자는 병원마다 따로 있고 처음 구독한 때부터 12분 뒤 도착 예정이다', () => {
    let clock = start
    const desk = createMemoryHospitalDesk(() => clock)
    const fast: CaseView[][] = []
    const main: CaseView[][] = []
    desk.watchCases('demo-fast', (c) => fast.push(c))
    clock = new Date(start.getTime() + 3 * MIN)
    desk.watchCases('demo-main', (c) => main.push(c))
    const a = fast[0][0]
    const b = main[0][0]
    expect(a.id).not.toBe(b.id)
    expect(a.etaAt).toEqual(new Date(start.getTime() + 12 * MIN))
    expect(b.etaAt).toEqual(new Date(start.getTime() + 15 * MIN))
    expect(a.isExample).toBe(true)
    expect(a.status).toBe('en_route')
    expect(a.readable).toBe(true)
    expect(a.pet?.photoDataUrl).toBeNull()
  })

  it('완료한 예시 환자는 다시 구독해도 돌아오지 않고 다른 병원의 예시는 남는다', async () => {
    const desk = createMemoryHospitalDesk(() => start)
    const first: CaseView[][] = []
    const stop = desk.watchCases('demo-fast', (c) => first.push(c))
    await desk.completeCase('demo-fast', first[0][0].id)
    stop()
    const again: CaseView[][] = []
    desk.watchCases('demo-fast', (c) => again.push(c))
    expect(again).toEqual([[]])
    const other: CaseView[][] = []
    desk.watchCases('demo-main', (c) => other.push(c))
    expect(other[0]).toHaveLength(1)
  })

  it('없는 환자 건을 완료해도 실패하지 않고 목록은 그대로다', async () => {
    const desk = createMemoryHospitalDesk(() => start)
    const seen: CaseView[][] = []
    desk.watchCases('demo-fast', (c) => seen.push(c))
    expect(await desk.completeCase('demo-fast', 'no-such-case')).toEqual({ ok: true, value: undefined })
    expect(seen[seen.length - 1]).toHaveLength(1)
  })

  it('받은 환자 목록 배열을 고쳐도 저장소는 바뀌지 않는다', () => {
    const desk = createMemoryHospitalDesk(() => start)
    const first: CaseView[][] = []
    desk.watchCases('demo-fast', (c) => first.push(c))
    first[0].length = 0
    const second: CaseView[][] = []
    desk.watchCases('demo-fast', (c) => second.push(c))
    expect(second[0]).toHaveLength(1)
  })

  it('메모리 구현은 코드를 보지 않고 서버 없이 동작한다고 알린다', async () => {
    const desk = createMemoryHospitalDesk(() => start)
    expect(await desk.open('demo-fast', 'wrong-code')).toEqual({ ok: true, value: undefined })
    expect(await desk.open('demo-fast', '')).toEqual({ ok: true, value: undefined })
    expect(desk.isLocalOnly).toBe(true)
  })
})
