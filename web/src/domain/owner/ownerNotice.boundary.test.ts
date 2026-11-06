import { describe, expect, it } from 'vitest'
import { APP_NAME } from '../../config'
import { storageNoticeOf, type StorageMode } from './ownerNotice'

/** 계획 014 U6 문구 표 (설계 6절 "모드")의 경계 */
describe('저장 위치 안내 경계', () => {
  it('앱 이름은 config의 APP_NAME을 그대로 쓴다', () => {
    expect(storageNoticeOf({ kind: 'server', emulator: false }, APP_NAME)).toBe(
      '이 정보는 MeongCare 서버에 저장되고, 연결한 차에서만 볼 수 있어요.',
    )
    expect(storageNoticeOf({ kind: 'server', emulator: false }, '멍케어')).toContain('멍케어 서버에')
  })

  it('서버에 저장하지 않는 모드는 서버에 저장된다고 말하지 않는다', () => {
    const local: StorageMode[] = [{ kind: 'browser-only' }, { kind: 'before-consent' }]
    for (const mode of local) {
      const text = storageNoticeOf(mode, APP_NAME)
      expect(text, mode.kind).toContain('브라우저에만 저장돼요')
      expect(text, mode.kind).not.toContain('서버에 저장되고')
      expect(text, mode.kind).not.toContain('시연용 로컬 서버')
    }
  })

  it('서버 없음 문구만 차와 연결할 수 없다고 말한다', () => {
    expect(storageNoticeOf({ kind: 'browser-only' }, APP_NAME)).toContain('차와 연결할 수 없어요')
    expect(storageNoticeOf({ kind: 'before-consent' }, APP_NAME)).not.toContain('연결할 수 없어요')
  })

  it('시연용 로컬 서버 표시는 에뮬레이터일 때 한 번만 붙는다', () => {
    const text = storageNoticeOf({ kind: 'server', emulator: true }, APP_NAME)
    expect(text.split('시연용 로컬 서버').length - 1).toBe(1)
    expect(text.endsWith('(시연용 로컬 서버)')).toBe(true)
  })
})
