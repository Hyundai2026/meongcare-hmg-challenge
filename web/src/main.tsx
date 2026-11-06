import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { APP_NAME } from './config'
import { createOwnerSetup } from './container'
import { App } from './ui/App'
import './ui/styles.css'

document.title = APP_NAME

const root = document.getElementById('root')
if (root === null) throw new Error('#root 요소가 없다')

// 서버 설정이 있으면 Firestore 모듈을 내려받은 뒤에 그린다 (계획 014 U8)
void createOwnerSetup().then((setup) => {
  createRoot(root).render(
    <StrictMode>
      <App browser={setup.browser} server={setup.server} now={() => new Date()} search={window.location.search} />
    </StrictMode>,
  )
})
