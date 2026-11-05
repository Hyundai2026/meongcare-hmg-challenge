/** 시간대 없는 날짜. month는 1~12. */
export interface LocalDate {
  year: number
  month: number
  day: number
}

const DATE_PATTERN = /^(\d{4})-(\d{2})-(\d{2})$/

function isLeapYear(year: number): boolean {
  return (year % 4 === 0 && year % 100 !== 0) || year % 400 === 0
}

export function daysInMonth(year: number, month: number): number {
  const days = [31, isLeapYear(year) ? 29 : 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31]
  return days[month - 1]
}

/** 'YYYY-MM-DD'만 받는다. 없는 날짜(2023-02-30 등)는 null. */
export function parseLocalDate(text: string): LocalDate | null {
  const match = DATE_PATTERN.exec(text)
  if (match === null) return null
  const year = Number(match[1])
  const month = Number(match[2])
  const day = Number(match[3])
  if (month < 1 || month > 12) return null
  if (day < 1 || day > daysInMonth(year, month)) return null
  return { year, month, day }
}

export function formatLocalDate(date: LocalDate): string {
  const pad = (n: number, width: number) => String(n).padStart(width, '0')
  return `${pad(date.year, 4)}-${pad(date.month, 2)}-${pad(date.day, 2)}`
}

/** 기기의 지역 날짜. */
export function localDateOf(now: Date): LocalDate {
  return { year: now.getFullYear(), month: now.getMonth() + 1, day: now.getDate() }
}

export function compareLocalDate(a: LocalDate, b: LocalDate): number {
  return a.year - b.year || a.month - b.month || a.day - b.day
}

/** months개월 전. 그 달에 같은 날이 없으면 그 달 말일이다 (3월 31일 - 1개월 = 2월 28일 또는 29일). */
export function minusMonths(date: LocalDate, months: number): LocalDate {
  const total = date.year * 12 + (date.month - 1) - months
  const year = Math.floor(total / 12)
  const month = total - year * 12 + 1
  return { year, month, day: Math.min(date.day, daysInMonth(year, month)) }
}
