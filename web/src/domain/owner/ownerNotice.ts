/**
 * 보호자 웹 아래 안내 문구 (계획 014 U6). 저장 위치를 숨기지 않는다 (출품 유의사항: 사실과 다르면 선정 취소).
 */

export type StorageMode =
  /** 서버 설정 없이 빌드했다. 브라우저에만 저장하고 차와 연결할 수 없다 */
  | { kind: 'browser-only' }
  /** 서버는 있지만 아직 서버 저장에 동의하지 않았다 */
  | { kind: 'before-consent' }
  /** 서버에 저장한다. emulator면 PC의 시연용 서버다 */
  | { kind: 'server'; emulator: boolean }

export function storageNoticeOf(mode: StorageMode, appName: string): string {
  switch (mode.kind) {
    case 'browser-only':
      return '이 정보는 지금 쓰는 브라우저에만 저장돼요. 이 화면은 서버에 연결되지 않아 차와 연결할 수 없어요.'
    case 'before-consent':
      return '이 정보는 지금 쓰는 브라우저에만 저장돼요. 차에서 쓰려면 서버에 저장해 주세요.'
    case 'server':
      return `이 정보는 ${appName} 서버에 저장되고, 연결한 차에서만 볼 수 있어요.${mode.emulator ? ' (시연용 로컬 서버)' : ''}`
  }
}
