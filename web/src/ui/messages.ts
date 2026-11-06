/** 화면에 보이는 실패 문구. 이유 코드 → 문장. */
import type { LoadFailure, SaveFailure } from '../domain/petProfileRepository'

export const LOAD_FAILURE_MESSAGES: Record<LoadFailure, string> = {
  corrupted: '저장된 정보가 손상됐어요. 새로 등록하면 지금 저장된 정보 대신 새 정보가 저장돼요.',
  'unsupported-version': '더 새로운 버전에서 저장한 정보라 읽을 수 없어요. 페이지를 새로고침해 보세요.',
  unavailable: '이 브라우저에서는 저장된 정보를 읽을 수 없어요. 시크릿 모드라면 끄고 다시 열어 주세요.',
  network: '서버에 연결하지 못해 저장된 정보를 읽지 못했어요. 잠시 뒤 새로 고쳐 주세요.',
}

export const SAVE_FAILURE_MESSAGES: Record<SaveFailure, string> = {
  quota: '브라우저 저장 공간이 부족해서 저장하지 못했어요. 사진을 빼고 다시 저장해 보세요.',
  unavailable: '이 브라우저에서는 저장할 수 없어요. 시크릿 모드라면 끄고 다시 열어 주세요.',
  unknown: '저장하지 못했어요. 잠시 뒤 다시 시도해 주세요.',
  network: '서버에 연결하지 못해 저장하지 못했어요. 잠시 뒤 다시 시도해 주세요.',
}
