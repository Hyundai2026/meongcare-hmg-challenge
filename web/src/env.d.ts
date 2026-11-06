/// <reference types="vite/client" />

/** web/.env.local (커밋하지 않음). 형식: web/.env.example */
interface ImportMetaEnv {
  readonly VITE_FIREBASE_PROJECT_ID?: string
  readonly VITE_FIREBASE_API_KEY?: string
  readonly VITE_FIREBASE_EMULATOR_HOST?: string
}
