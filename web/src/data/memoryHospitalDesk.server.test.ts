import { describe, expect, it } from 'vitest'
import type { DeskConnection } from '../domain/hospital/serverInfo'
import { createMemoryHospitalDesk } from './memoryHospitalDesk'

// 기준: web/src/domain/hospital/hospitalDesk.ts server·watchConnection 주석, 계획 013 설계 결정 9
// (server: 메모리 구현은 null, watchConnection: 메모리 구현은 바로 'local' 한 번)
const start = new Date('2026-11-06T05:00:00Z')

describe('createMemoryHospitalDesk 서버 표시 (계획 013)', () => {
  it('메모리 저장소는 붙은 서버가 없다', () => {
    const desk = createMemoryHospitalDesk(() => start)
    expect(desk.server).toBeNull()
    expect(desk.isLocalOnly).toBe(true)
  })

  it('연결 상태는 구독하자마자 local 한 번이고 상태를 알리거나 확인해도 더 오지 않는다', async () => {
    const desk = createMemoryHospitalDesk(() => start)
    const seen: DeskConnection[] = []
    const stop = desk.watchConnection('demo-fast', (s) => seen.push(s))
    expect(seen).toEqual(['local'])
    await desk.updateStatus('demo-fast', { waitMinutes: 5, congestion: 'low', acceptingEmergency: true })
    const cases: string[] = []
    desk.watchCases('demo-fast', (c) => cases.push(...c.map((x) => x.id)))
    await desk.acknowledgeCase('demo-fast', cases[0])
    expect(seen).toEqual(['local'])
    expect(() => stop()).not.toThrow()
  })
})
