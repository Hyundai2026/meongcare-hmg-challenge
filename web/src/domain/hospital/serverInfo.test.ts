import { describe, expect, it } from 'vitest'
import { CONNECTION_TEXT, serverInfoOf } from './serverInfo'

describe('serverInfoOf', () => {
  it('에뮬레이터 주소와 프로젝트를 보인다', () => {
    expect(serverInfoOf({ projectId: 'demo-meongcare', emulatorHost: '127.0.0.1' })).toEqual({
      label: '에뮬레이터 127.0.0.1:8080 · 프로젝트 demo-meongcare',
      warning: null,
    })
  })

  it('IPv6 호스트는 대괄호로 감싼다', () => {
    expect(serverInfoOf({ projectId: 'p', emulatorHost: '::1' }).label).toBe('에뮬레이터 [::1]:8080 · 프로젝트 p')
    expect(serverInfoOf({ projectId: 'p', emulatorHost: '[::1]' }).label).toBe('에뮬레이터 [::1]:8080 · 프로젝트 p')
  })

  it('localhost나 ::1이면 차 앱과 다른 서버일 수 있다고 경고한다', () => {
    for (const host of ['localhost', 'LOCALHOST', '::1', '[::1]']) {
      expect(serverInfoOf({ projectId: 'p', emulatorHost: host }).warning).toContain('127.0.0.1')
    }
  })

  it('127.0.0.1과 다른 IPv4 주소는 경고하지 않는다', () => {
    expect(serverInfoOf({ projectId: 'p', emulatorHost: '127.0.0.1' }).warning).toBeNull()
    expect(serverInfoOf({ projectId: 'p', emulatorHost: '192.168.0.10' }).warning).toBeNull()
  })

  it('진짜 프로젝트는 프로젝트 이름만 보인다', () => {
    expect(serverInfoOf({ projectId: 'meongcare-prod', emulatorHost: null })).toEqual({
      label: 'Firebase 프로젝트 meongcare-prod',
      warning: null,
    })
  })
})

describe('CONNECTION_TEXT', () => {
  it('연결 상태마다 문구가 있다', () => {
    expect(CONNECTION_TEXT.live).toBe('실시간 연결됨')
    expect(CONNECTION_TEXT.checking).toContain('확인 중')
    expect(CONNECTION_TEXT.offline).toContain('새로 고쳐')
  })
})
