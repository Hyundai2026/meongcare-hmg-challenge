import { describe, expect, it } from 'vitest'
import { LINK_CODE_ALPHABET, cleanLinkCodeText, linkCodeFromQuery, normalizeLinkCode } from './linkCode'

describe('연결 코드 입력 (docs/data/owner-link.md "연결 코드")', () => {
  it('소문자·하이픈·공백을 받아 대문자 6자로 바꾼다', () => {
    expect(normalizeLinkCode('k7p-3qx')).toEqual({ ok: true, code: 'K7P3QX' })
    expect(normalizeLinkCode(' K7P 3QX ')).toEqual({ ok: true, code: 'K7P3QX' })
    expect(normalizeLinkCode('k 7 p - 3 q x')).toEqual({ ok: true, code: 'K7P3QX' })
  })

  it('6자가 아니면 보내지 않는다', () => {
    expect(normalizeLinkCode('K7P3Q')).toEqual({ ok: false, reason: 'length' })
    expect(normalizeLinkCode('K7P3QXA')).toEqual({ ok: false, reason: 'length' })
    expect(normalizeLinkCode('')).toEqual({ ok: false, reason: 'length' })
  })

  it('0·O·1·I·L이 있으면 코드에 없는 글자다', () => {
    for (const c of ['0', 'O', 'o', '1', 'I', 'i', 'L', 'l']) {
      expect(normalizeLinkCode(`K7P3Q${c}`)).toEqual({ ok: false, reason: 'characters' })
    }
    expect(normalizeLinkCode('K7P3Q!')).toEqual({ ok: false, reason: 'characters' })
  })

  it('글자 집합은 차 앱과 같은 31종이다', () => {
    expect(LINK_CODE_ALPHABET).toBe('23456789ABCDEFGHJKMNPQRSTUVWXYZ')
    expect(new Set(LINK_CODE_ALPHABET).size).toBe(31)
  })

  it('주소의 ?code= 값으로 입력칸을 채운다', () => {
    expect(linkCodeFromQuery('?code=k7p-3qx')).toBe('K7P3QX')
    expect(linkCodeFromQuery('?h=1&code=K7P%203QX')).toBe('K7P3QX')
    expect(linkCodeFromQuery('?code=')).toBeNull()
    expect(linkCodeFromQuery('')).toBeNull()
  })

  it('입력칸 정리는 검사하지 않는다', () => {
    expect(cleanLinkCodeText('ab-0 o')).toBe('AB0O')
  })
})
