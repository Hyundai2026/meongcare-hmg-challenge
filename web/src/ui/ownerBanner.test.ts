import { describe, expect, it } from 'vitest'
import example from '../../../docs/data/pet-profile.example.json'
import { petProfileFromJson } from '../domain/petProfileJson'
import { carLinkStatusOf, ownerBannerOf } from './ownerBanner'

const bori = (() => {
  const parsed = petProfileFromJson(example)
  if (!parsed.ok) throw new Error('예시가 읽히지 않는다')
  return parsed.profile
})()

describe('서버 모드 안내 한 줄', () => {
  it('등록하기 전에 동의했으면 차와 연결할 수 있다고 하지 않고 등록을 권한다', () => {
    expect(ownerBannerOf({ kind: 'nothing' }, true, false)).toBe('서버에 저장하기로 했어요. 반려견을 등록하면 차와 연결할 수 있어요.')
  })

  it('반려견이 있는 채로 동의했으면 차와 연결할 수 있다고 한다', () => {
    expect(ownerBannerOf({ kind: 'nothing' }, true, true)).toBe('서버에 저장했어요. 이제 차와 연결할 수 있어요.')
  })

  it('옮겼으면 이름을 넣어 알린다', () => {
    expect(ownerBannerOf({ kind: 'moved', profile: bori }, true, true)).toBe("이 브라우저의 '보리' 정보를 서버로 옮겼어요.")
  })

  it('다시 열었을 때는 옮긴 경우와 실패만 알린다', () => {
    expect(ownerBannerOf({ kind: 'nothing' }, false, false)).toBeNull()
    expect(ownerBannerOf({ kind: 'server-kept' }, false, true)).toBeNull()
    expect(ownerBannerOf({ kind: 'failed' }, false, true)).toContain('옮기지 못했어요')
  })
})

describe('[차와 연결] 카드 상태', () => {
  it('읽는 중이면 카드를 그리지 않는다', () => {
    expect(carLinkStatusOf(undefined)).toEqual({ kind: 'unknown' })
  })

  it('연결한 차가 없거나 읽지 못하면 연결하기다', () => {
    expect(carLinkStatusOf([])).toEqual({ kind: 'not-linked' })
    expect(carLinkStatusOf(null)).toEqual({ kind: 'not-linked' })
  })

  it('연결한 차가 있으면 연결됨이고 가장 최근 연결 시각을 보인다', () => {
    expect(
      carLinkStatusOf([{ linkedAt: '2026-11-06T06:47:00.000Z' }, { linkedAt: null }, { linkedAt: '2026-11-06T07:10:00.000Z' }]),
    ).toEqual({ kind: 'linked', count: 3, lastLinkedAt: '2026-11-06T07:10:00.000Z' })
  })

  it('연결 시각을 모르면 시각 없이 연결됨이다', () => {
    expect(carLinkStatusOf([{ linkedAt: null }])).toEqual({ kind: 'linked', count: 1, lastLinkedAt: null })
  })
})
