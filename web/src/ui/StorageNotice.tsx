/** 저장 위치 안내 (계획 014 U6). 문구는 domain/owner/ownerNotice.ts가 정한다 */
export function StorageNotice({ text }: { text: string }) {
  return (
    <p className="notice">
      <svg className="notice__icon" viewBox="0 0 20 20" aria-hidden="true">
        <circle cx="10" cy="10" r="8.25" fill="none" stroke="currentColor" strokeWidth="1.5" />
        <rect x="9.25" y="8.5" width="1.5" height="6" rx="0.75" fill="currentColor" />
        <circle cx="10" cy="6" r="1" fill="currentColor" />
      </svg>
      <span>{text}</span>
    </p>
  )
}
