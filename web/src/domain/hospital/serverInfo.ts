/**
 * 병원 웹이 어느 서버에 붙었는지 (계획 013). 순수 함수 (firebase를 import하지 않는다).
 *
 * 012 시연 준비 때 Firebase 에뮬레이터가 IPv4(127.0.0.1)와 IPv6(::1)에 하나씩 떠 있었고,
 * 차 앱(adb reverse → 127.0.0.1)과 병원 웹(localhost → ::1)이 서로 다른 서버에 붙어 카드가 뜨지 않았다.
 * 그래서 화면 위에 주소를 늘 보이고, localhost·::1이면 경고한다.
 */

export interface ServerInfo {
  /** 예: "에뮬레이터 127.0.0.1:8080 · 프로젝트 demo-meongcare" */
  label: string
  /** 주소가 차 앱과 어긋날 수 있으면 안내 한 줄. 없으면 null */
  warning: string | null
}

/** 실시간 연결 상태. local = 서버 없음(메모리 모드) */
export type DeskConnection = 'local' | 'checking' | 'live' | 'offline'

export const FIRESTORE_EMULATOR_PORT = 8080

const IPV6_OR_LOCALHOST = new Set(['localhost', '::1', '[::1]'])

/** IPv6 주소는 URL에서 대괄호로 감싼다 */
export function hostForUrl(host: string): string {
  return host.includes(':') && !host.startsWith('[') ? `[${host}]` : host
}

export function serverInfoOf(config: { projectId: string; emulatorHost: string | null }): ServerInfo {
  if (config.emulatorHost === null) {
    return { label: `Firebase 프로젝트 ${config.projectId}`, warning: null }
  }
  const host = config.emulatorHost.trim()
  return {
    label: `에뮬레이터 ${hostForUrl(host)}:${FIRESTORE_EMULATOR_PORT} · 프로젝트 ${config.projectId}`,
    warning: IPV6_OR_LOCALHOST.has(host.toLowerCase())
      ? 'localhost·::1은 IPv6 에뮬레이터에 붙을 수 있어요. 차 앱(adb reverse)과 같은 서버를 보려면 127.0.0.1로 맞춰 주세요.'
      : null,
  }
}

export const CONNECTION_TEXT: Record<Exclude<DeskConnection, 'local'>, string> = {
  checking: '서버 연결 확인 중…',
  live: '실시간 연결됨',
  offline: '서버 연결이 끊겼어요. 새로 고쳐 주세요.',
}
