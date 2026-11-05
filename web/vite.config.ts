import react from '@vitejs/plugin-react'
import { defineConfig } from 'vitest/config'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  test: {
    // 순수 로직만 테스트한다. 화면은 web/scripts/verify_screens.py가 실제 브라우저로 본다.
    environment: 'node',
    include: ['src/**/*.test.ts'],
  },
})
