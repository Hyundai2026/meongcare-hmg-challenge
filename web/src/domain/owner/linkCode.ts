/**
 * 차와 연결하는 코드 (계획 014). 규칙: docs/data/owner-link.md "연결 코드".
 * 차 화면에 "K7P 3QX"처럼 보이는 6자를 보호자가 폰에 입력한다. 차 앱(LinkCode.kt)과 같은 글자 집합이다.
 */

/** 대문자·숫자 31종. 헷갈리는 0·1·I·L·O는 쓰지 않는다 */
export const LINK_CODE_ALPHABET = '23456789ABCDEFGHJKMNPQRSTUVWXYZ'
export const LINK_CODE_LENGTH = 6

export type LinkCodeInput = { ok: true; code: string } | { ok: false; reason: 'length' | 'characters' }

/** 공백·하이픈을 빼고 대문자로 바꾼다 (화면 입력칸이 보이는 값) */
export function cleanLinkCodeText(text: string): string {
  return text.replace(/[\s-]/g, '').toUpperCase()
}

/** 입력 → 코드. 6자가 아니면 'length', 코드에 없는 글자가 있으면 'characters' */
export function normalizeLinkCode(text: string): LinkCodeInput {
  const cleaned = cleanLinkCodeText(text)
  if ([...cleaned].some((c) => !LINK_CODE_ALPHABET.includes(c))) return { ok: false, reason: 'characters' }
  if (cleaned.length !== LINK_CODE_LENGTH) return { ok: false, reason: 'length' }
  return { ok: true, code: cleaned }
}

/** 주소의 `?code=`(나중 QR) 값으로 입력칸을 채운다. 없으면 null. 값은 정리만 하고 검사는 보낼 때 한다 */
export function linkCodeFromQuery(search: string): string | null {
  const value = new URLSearchParams(search).get('code')
  if (value === null) return null
  const cleaned = cleanLinkCodeText(value).slice(0, LINK_CODE_LENGTH * 2)
  return cleaned.length > 0 ? cleaned : null
}
