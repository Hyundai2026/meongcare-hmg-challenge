/**
 * 서버 저장 동의 (계획 014 U6). 동의 문서(`owners/{uid}`)의 consentVersion과 화면 문구다.
 * 문구를 뜻이 바뀌게 고치면 버전을 올린다 (보안 규칙 firestore.rules의 consentVersion == 1도 같이).
 */

export const CONSENT_VERSION = 1

export const CONSENT_TITLE = '서버에 저장하고 차와 연결할까요?'

export interface ConsentItem {
  label: string
  text: string
}

export const CONSENT_ITEMS: ConsentItem[] = [
  { label: '저장하는 것', text: '반려견 사진·이름·품종·성별·생년월일·주요 질환·알레르기·복용 약' },
  {
    label: '쓰는 곳',
    text: '내가 연결한 차의 진료 카드, 그 차에서 내가 고른 병원으로 보내는 진료 카드 (사진은 차와 병원에 보내지 않아요)',
  },
  { label: '보관', text: '내가 지울 때까지' },
  { label: '동의하지 않으면', text: '지금처럼 이 브라우저에만 저장되고, 차와는 연결할 수 없어요' },
  {
    label: '알아 둘 것',
    text: '이 브라우저의 방문 기록·사이트 데이터를 지우면 서버에 저장한 정보를 이 브라우저에서 고칠 수 없게 돼요',
  },
]

/** 브라우저에 저장한 정보가 있을 때 덧붙이는 문장 */
export function consentMigrationText(petName: string): string {
  return `이 브라우저의 '${petName}' 정보를 서버로 옮기고, 옮긴 뒤 이 브라우저에서는 지워요.`
}
