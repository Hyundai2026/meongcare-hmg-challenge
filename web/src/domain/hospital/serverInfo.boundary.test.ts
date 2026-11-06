import { describe, expect, it } from 'vitest'
import { hostForUrl, serverInfoOf } from './serverInfo'

// 기준: 계획 013 설계 결정 9 serverInfoOf
// - 에뮬레이터: "에뮬레이터 127.0.0.1:8080 · 프로젝트 demo-meongcare", 진짜 프로젝트: "Firebase 프로젝트 <id>"
// - 호스트가 localhost·::1·[::1]이면 경고 (관찰 1: 병원 웹이 IPv6 에뮬레이터에 붙어 차와 다른 서버를 봤다)

const WARNING =
  'localhost·::1은 IPv6 에뮬레이터에 붙을 수 있어요. 차 앱(adb reverse)과 같은 서버를 보려면 127.0.0.1로 맞춰 주세요.'

describe('serverInfoOf 경계', () => {
  it('경고 문구는 계획의 문구 그대로다', () => {
    expect(serverInfoOf({ projectId: 'demo-meongcare', emulatorHost: 'localhost' })).toEqual({
      label: '에뮬레이터 localhost:8080 · 프로젝트 demo-meongcare',
      warning: WARNING,
    })
  })

  it('앞뒤 공백이 있어도 localhost나 ::1이면 경고한다', () => {
    for (const host of [' localhost', 'localhost ', ' ::1 ', '\t[::1]\n']) {
      expect(serverInfoOf({ projectId: 'p', emulatorHost: host }).warning).toBe(WARNING)
    }
  })

  it('앞뒤 공백은 지우고 주소를 보인다', () => {
    expect(serverInfoOf({ projectId: 'p', emulatorHost: ' 127.0.0.1 ' }).label).toBe('에뮬레이터 127.0.0.1:8080 · 프로젝트 p')
    expect(serverInfoOf({ projectId: 'p', emulatorHost: ' ::1 ' }).label).toBe('에뮬레이터 [::1]:8080 · 프로젝트 p')
  })

  it('대소문자가 섞인 localhost도 경고한다', () => {
    expect(serverInfoOf({ projectId: 'p', emulatorHost: 'LocalHost' }).warning).toBe(WARNING)
  })

  it('localhost가 들어 있을 뿐인 다른 이름은 경고하지 않는다', () => {
    expect(serverInfoOf({ projectId: 'p', emulatorHost: 'localhost.example' }).warning).toBeNull()
    expect(serverInfoOf({ projectId: 'p', emulatorHost: 'mylocalhost' }).warning).toBeNull()
  })

  it('진짜 프로젝트는 에뮬레이터 포트를 보이지 않고 경고도 없다', () => {
    const info = serverInfoOf({ projectId: 'demo-meongcare', emulatorHost: null })
    expect(info.label).toBe('Firebase 프로젝트 demo-meongcare')
    expect(info.label).not.toContain('8080')
    expect(info.warning).toBeNull()
  })
})

describe('hostForUrl', () => {
  it('IPv4와 이름은 그대로, IPv6는 대괄호로 한 번만 감싼다', () => {
    expect(hostForUrl('127.0.0.1')).toBe('127.0.0.1')
    expect(hostForUrl('localhost')).toBe('localhost')
    expect(hostForUrl('::1')).toBe('[::1]')
    expect(hostForUrl('[::1]')).toBe('[::1]')
  })
})
