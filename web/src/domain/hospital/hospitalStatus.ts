/**
 * 제휴 병원과 병원이 알린 상태. 형식의 기준은 docs/data/hospital-status.md다 (웹·차 앱·시드 공통).
 * 읽는 쪽은 짐작하지 않는다: 병원 필수 값이 틀리면 그 병원을 건너뛰고, status가 하나라도 틀리면 "대기 정보 없음"이다.
 */
export const HOSPITAL_SCHEMA_VERSION = 1
export const WAIT_MINUTES_MAX = 600

export type Congestion = 'low' | 'normal' | 'high'

export interface HospitalStatus {
  /** 지금 오면 예상 대기 분. 0~600 정수 */
  waitMinutes: number
  congestion: Congestion
  acceptingEmergency: boolean
  /** 마지막으로 알린 시각 */
  updatedAt: Date
}

export interface HospitalSummary {
  id: string
  name: string
  phone: string | null
  isSample: boolean
  /** null = 아직 알리지 않았거나 형식이 틀림 */
  status: HospitalStatus | null
}

/** 병원 웹에서 상태를 알릴 때 보내는 값 (updatedAt은 저장소가 서버 시각으로 채운다) */
export interface StatusInput {
  waitMinutes: number
  congestion: Congestion
  acceptingEmergency: boolean
}

export const CONGESTIONS: readonly Congestion[] = ['low', 'normal', 'high']

export const CONGESTION_TEXT: Record<Congestion, string> = { low: '여유', normal: '보통', high: '혼잡' }

function isObject(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}

function isCongestion(value: unknown): value is Congestion {
  return value === 'low' || value === 'normal' || value === 'high'
}

/**
 * 타임스탬프 값 → Date. Date, ISO 8601 문자열, `toDate()`가 있는 객체(Firestore Timestamp)를 받는다.
 * 읽을 수 없으면 null.
 */
export function dateFrom(value: unknown): Date | null {
  if (value instanceof Date) return Number.isNaN(value.getTime()) ? null : value
  if (typeof value === 'string') {
    const date = new Date(value)
    return Number.isNaN(date.getTime()) ? null : date
  }
  if (isObject(value) && typeof value.toDate === 'function') {
    const date: unknown = (value.toDate as () => unknown)()
    return date instanceof Date && !Number.isNaN(date.getTime()) ? date : null
  }
  return null
}

/** status 객체 → HospitalStatus. 하나라도 틀리면 null (대기 정보 없음). */
export function statusFromJson(value: unknown): HospitalStatus | null {
  if (!isObject(value)) return null
  const { waitMinutes, congestion, acceptingEmergency, updatedAt } = value
  if (typeof waitMinutes !== 'number' || !Number.isInteger(waitMinutes)) return null
  if (waitMinutes < 0 || waitMinutes > WAIT_MINUTES_MAX) return null
  if (!isCongestion(congestion)) return null
  if (typeof acceptingEmergency !== 'boolean') return null
  const date = dateFrom(updatedAt)
  if (date === null) return null
  return { waitMinutes, congestion, acceptingEmergency, updatedAt: date }
}

/** 병원 문서 → HospitalSummary. 버전·이름이 틀리면 null (그 병원을 건너뛴다). */
export function hospitalFromJson(id: string, value: unknown): HospitalSummary | null {
  if (!isObject(value)) return null
  if (value.schemaVersion !== HOSPITAL_SCHEMA_VERSION) return null
  const { name, phone, isSample, status } = value
  if (typeof name !== 'string' || name.trim().length === 0) return null
  return {
    id,
    name,
    phone: typeof phone === 'string' && phone.trim().length > 0 ? phone : null,
    // 불리언이 아니면 시연용으로 본다 (시연용이라고 알리는 쪽이 안전하다)
    isSample: typeof isSample === 'boolean' ? isSample : true,
    status: statusFromJson(status),
  }
}
