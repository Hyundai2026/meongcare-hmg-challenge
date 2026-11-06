import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { APP_NAME } from '../config'
import { HospitalApp } from '../ui/hospital/HospitalApp'
import { createHospitalDesk } from './container'
import '../ui/styles.css'
import '../ui/hospital/hospital.css'

document.title = `${APP_NAME} 병원 (시연용)`

const root = document.getElementById('root')
if (root === null) throw new Error('#root 요소가 없다')

const now = () => new Date()
const desk = createHospitalDesk(now)

createRoot(root).render(
  <StrictMode>
    <HospitalApp desk={desk} now={now} />
  </StrictMode>,
)
