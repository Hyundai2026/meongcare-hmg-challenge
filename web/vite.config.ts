import react from '@vitejs/plugin-react'
import { defineConfig } from 'vitest/config'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  build: {
    // 두 페이지: 보호자 웹(/)과 병원 웹(/hospital/, 012). 라우터 없이 Vite 다중 페이지로 나눈다.
    // 그래서 보호자 페이지 번들에는 병원 웹 코드(나중의 Firebase)가 들어가지 않는다.
    rolldownOptions: {
      input: {
        // web/ 기준 경로 (npm --prefix web으로 돌리면 web/이 작업 폴더다)
        main: 'index.html',
        hospital: 'hospital/index.html',
      },
    },
  },
  test: {
    // 순수 로직만 테스트한다. 화면은 web/scripts/verify_screens.py가 실제 브라우저로 본다.
    environment: 'node',
    include: ['src/**/*.test.ts'],
  },
})
