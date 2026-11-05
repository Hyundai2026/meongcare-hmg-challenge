import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { APP_NAME } from './config'
import { App } from './ui/App'
import './ui/styles.css'

document.title = APP_NAME

const root = document.getElementById('root')
if (root === null) throw new Error('#root 요소가 없다')

createRoot(root).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
