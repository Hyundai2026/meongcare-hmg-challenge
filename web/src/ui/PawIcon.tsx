/** 발바닥 모양. 아이콘 라이브러리 없이 직접 그린다. 색은 CSS fill로 정한다. */
export function PawIcon({ className }: { className?: string }) {
  return (
    <svg className={className} viewBox="0 0 32 32" aria-hidden="true">
      <ellipse cx="7.5" cy="13.5" rx="3" ry="3.8" />
      <ellipse cx="12.8" cy="7.8" rx="3" ry="3.8" />
      <ellipse cx="19.2" cy="7.8" rx="3" ry="3.8" />
      <ellipse cx="24.5" cy="13.5" rx="3" ry="3.8" />
      <path d="M16 14.5c-4.6 0-9 5.3-9 9C7 26.2 9.2 27 11.3 27c2 0 3-.9 4.7-.9s2.7.9 4.7.9c2.1 0 4.3-.8 4.3-3.5 0-3.7-4.4-9-9-9Z" />
    </svg>
  )
}
