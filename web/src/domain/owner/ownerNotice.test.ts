import { describe, expect, it } from 'vitest'
import { storageNoticeOf } from './ownerNotice'

describe('저장 위치 안내 (계획 014 U6)', () => {
  it('서버 없이 빌드하면 브라우저에만 저장하고 차와 연결할 수 없다고 알린다', () => {
    expect(storageNoticeOf({ kind: 'browser-only' }, 'MeongCare')).toBe(
      '이 정보는 지금 쓰는 브라우저에만 저장돼요. 이 화면은 서버에 연결되지 않아 차와 연결할 수 없어요.',
    )
  })

  it('동의 전에는 서버에 저장해야 차에서 쓴다고 알린다', () => {
    expect(storageNoticeOf({ kind: 'before-consent' }, 'MeongCare')).toBe(
      '이 정보는 지금 쓰는 브라우저에만 저장돼요. 차에서 쓰려면 서버에 저장해 주세요.',
    )
  })

  it('동의 뒤에는 서버와 연결한 차를 알린다', () => {
    expect(storageNoticeOf({ kind: 'server', emulator: false }, 'MeongCare')).toBe(
      '이 정보는 MeongCare 서버에 저장되고, 연결한 차에서만 볼 수 있어요.',
    )
  })

  it('에뮬레이터면 시연용 로컬 서버라고 덧붙인다', () => {
    expect(storageNoticeOf({ kind: 'server', emulator: true }, 'MeongCare')).toBe(
      '이 정보는 MeongCare 서버에 저장되고, 연결한 차에서만 볼 수 있어요. (시연용 로컬 서버)',
    )
  })
})
