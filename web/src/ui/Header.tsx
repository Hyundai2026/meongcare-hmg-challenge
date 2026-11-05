import { APP_NAME } from '../config'

/** 발바닥 모양. 아이콘 라이브러리 없이 직접 그린다. */
function PawMark() {
  return (
    <svg className="header__mark" viewBox="0 0 32 32" aria-hidden="true">
      <ellipse cx="8" cy="13" rx="3.2" ry="4" />
      <ellipse cx="13.5" cy="7.5" rx="3.2" ry="4" />
      <ellipse cx="18.5" cy="7.5" rx="3.2" ry="4" />
      <ellipse cx="24" cy="13" rx="3.2" ry="4" />
      <path d="M16 14c-4.6 0-9 5.4-9 9.2C7 26 9.2 27 11.3 27c2 0 3-.9 4.7-.9s2.7.9 4.7.9c2.1 0 4.3-1 4.3-3.8C25 19.4 20.6 14 16 14Z" />
    </svg>
  )
}

export function Header() {
  return (
    <header className="header">
      <PawMark />
      <span className="header__name">{APP_NAME}</span>
    </header>
  )
}
