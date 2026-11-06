import { describe, expect, it } from 'vitest'
import { emulatorHostFor, serverInfoOf } from './serverInfo'

describe('emulatorHostFor', () => {
  it('localhost는 차 앱과 같은 127.0.0.1로 바꾼다', () => {
    expect(emulatorHostFor('localhost')).toBe('127.0.0.1')
    expect(emulatorHostFor(' LocalHost ')).toBe('127.0.0.1')
  })

  it('다른 주소는 앞뒤 공백만 지우고 그대로 둔다', () => {
    expect(emulatorHostFor('127.0.0.1')).toBe('127.0.0.1')
    expect(emulatorHostFor(' 192.168.0.10 ')).toBe('192.168.0.10')
    expect(emulatorHostFor('::1')).toBe('::1')
    expect(emulatorHostFor('mylocalhost')).toBe('mylocalhost')
  })

  it('바꾼 주소로 만든 서버 줄에는 경고가 없고 ::1은 여전히 경고한다', () => {
    expect(serverInfoOf({ projectId: 'p', emulatorHost: emulatorHostFor('localhost') }).warning).toBeNull()
    expect(serverInfoOf({ projectId: 'p', emulatorHost: emulatorHostFor('::1') }).warning).not.toBeNull()
  })
})
