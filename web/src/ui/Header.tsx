import { APP_NAME } from '../config'
import { PawIcon } from './PawIcon'

export function Header() {
  return (
    <header className="header">
      <PawIcon className="header__mark" />
      <span className="header__name">{APP_NAME}</span>
    </header>
  )
}
