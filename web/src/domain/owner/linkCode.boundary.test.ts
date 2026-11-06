import { describe, expect, it } from 'vitest'
import ownerLink from '../../../../docs/data/owner-link.example.json'
import { LINK_CODE_ALPHABET, linkCodeFromQuery, normalizeLinkCode } from './linkCode'

/** docs/data/owner-link.md "연결 코드"의 경계 (계획 014 단계 12: 정규화 경계) */
describe('연결 코드 입력 경계', () => {
  it('글자 집합의 31종은 소문자로 입력해도 모두 받는다', () => {
    for (const c of LINK_CODE_ALPHABET) {
      expect(normalizeLinkCode(c.repeat(6))).toEqual({ ok: true, code: c.repeat(6) })
      expect(normalizeLinkCode(c.toLowerCase().repeat(6))).toEqual({ ok: true, code: c.repeat(6) })
    }
  })

  it('영문·숫자 중 글자 집합에 없는 것은 대소문자 모두 받지 않는다', () => {
    const all = '0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz'
    for (const c of all) {
      const expected = LINK_CODE_ALPHABET.includes(c.toUpperCase())
      expect(normalizeLinkCode(`K7P3Q${c}`).ok, c).toBe(expected)
    }
  })

  it('폰 키보드가 넣는 여러 공백(탭·줄바꿈·NBSP·전각 공백)도 지운다', () => {
    for (const text of ['K7P\t3QX', 'K7P\n3QX', 'K7P 3QX', 'K7P　3QX', ' k7p3qx ']) {
      expect(normalizeLinkCode(text), JSON.stringify(text)).toEqual({ ok: true, code: 'K7P3QX' })
    }
  })

  it('하이픈이 여러 개여도 지운다', () => {
    expect(normalizeLinkCode('--k7p--3qx--')).toEqual({ ok: true, code: 'K7P3QX' })
  })

  it('공백·하이픈만 입력하면 길이가 모자란다', () => {
    expect(normalizeLinkCode(' - - ')).toEqual({ ok: false, reason: 'length' })
  })

  it('정리한 뒤 5자·7자는 보내지 않는다', () => {
    expect(normalizeLinkCode('k7p-3q')).toEqual({ ok: false, reason: 'length' })
    expect(normalizeLinkCode('k7p-3qx-a')).toEqual({ ok: false, reason: 'length' })
  })

  it('이모지·한글이 섞이면 코드에 없는 글자다', () => {
    expect(normalizeLinkCode('K7P3Q🐶')).toMatchObject({ ok: false, reason: 'characters' })
    expect(normalizeLinkCode('K7P3Q가')).toMatchObject({ ok: false, reason: 'characters' })
  })

  it('예시 문서의 코드는 형식이 맞다', () => {
    const reader = ownerLink['petProfiles/owner-uid-1/readers/car-uid-1']
    expect(normalizeLinkCode(reader.linkCode)).toEqual({ ok: true, code: reader.linkCode })
  })

  it('주소의 + 와 인코딩된 공백·하이픈도 지운다', () => {
    expect(linkCodeFromQuery('?code=k7p+3qx')).toBe('K7P3QX')
    expect(linkCodeFromQuery('?code=k7p%2D3qx')).toBe('K7P3QX')
  })

  it('주소의 코드가 공백뿐이면 채우지 않는다', () => {
    expect(linkCodeFromQuery('?code=%20%20')).toBeNull()
    expect(linkCodeFromQuery('?code=-')).toBeNull()
  })

  it('주소에 code가 없으면 다른 값이 있어도 채우지 않는다', () => {
    expect(linkCodeFromQuery('?c=K7P3QX&codes=K7P3QX')).toBeNull()
  })
})
