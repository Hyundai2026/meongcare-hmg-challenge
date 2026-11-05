# 008 보호자 웹 첫 버전 (반려견 1마리 프로필·의료정보 등록, 브라우저 저장)

- 목표: 보호자가 폰 브라우저에서 반려견 1마리의 프로필(사진, 이름, 품종, 나이·생년월일, 성별)과 의료정보(주요 질환, 알레르기, 복용 약)를 등록하고 보고 고칠 수 있는 `web/` React 앱을 서버 없이(localStorage) 만들고, 차 앱 진료 카드와 함께 쓸 프로필 형식을 문서로 정한다.
- 완료 기준 (어떻게 검증하나):
  1. `npm --prefix web ci` 후 `npm --prefix web run check`가 통과한다 (타입 검사 + Vitest + `vite build`).
     - 아래 "단계"에 이름을 적은 테스트 파일이 모두 있고 통과한다. 통과 수를 진행 로그에 적는다.
  2. (`[사용자 승인 필요]` 1이 4a·4b일 때) `node .claude/hooks/build-gate.mjs pipeline`이 Android와 웹을 모두 돌리고 "검증 파이프라인 통과."를 출력한다. 하네스 시나리오 H1~H12(아래 "확인 절차")가 기대대로 동작한다.
  3. 화면 확인 스크립트 `python web/scripts/verify_screens.py`가 종료 코드 0으로 끝난다. 시나리오 S1~S16(아래 "확인 절차")을 390×844와 360×800에서 돌린다. 저장된 스크린샷(`.claude/.cache/web/`)을 Read로 열어 직접 본다.
  4. 정적 검사 (Grep, `web/src/` 기준):
     - `localStorage`는 `web/src/data/`와 `web/src/container.ts`에만 있다.
     - `web/src/ui/` 아래에 `/data/` import 0건 (`ui`는 `domain`만 안다).
     - `dangerouslySetInnerHTML` 0건, `console.log` 0건 (의료정보가 로그에 남지 않게).
     - 앱 이름 문자열(`PawCare`)은 `web/src/config.ts`에만 있다.
  5. 이번 작업이 Android 앱을 건드리지 않았다: `git diff dev --stat -- app/ build.gradle.kts settings.gradle.kts gradle/`의 출력이 비어 있다.
  6. `git ls-files web` 출력에 `node_modules/`, `dist/`, `.env.local`이 없다. `web/package.json`의 모든 의존성 버전에 `^`, `~`가 없다.
  7. 문서:
     - `docs/data/pet-profile.md`에 필드 표, 예시, 나이 계산 규칙, 버전 규칙, 차 앱(Kotlin) 대응 표가 있다. `docs/data/pet-profile.example.json`이 있고 웹 테스트가 이 파일을 읽는다.
     - `docs/architecture.md`에 "보호자 웹" 절이 있다 (결정 표, 폴더 구조, 의존 방향, 저장과 개인정보, Firebase로 옮길 때 할 일).
     - `AGENTS.md`의 기술 스택, 명령어 표, 디렉토리 표에 `web/`(와 `docs/data/`)가 있다.
     - (`[사용자 승인 필요]` 4가 4a일 때) `README.md`에 "보호자 웹" 실행 방법과 "차 앱과 아직 연결되지 않았다"는 문장이 있다.
- 브랜치: `feat/owner-web` (메인 에이전트가 dev에서 따 두었다. 시작 커밋은 `git merge-base dev HEAD`로 확인해 진행 로그에 적는다)
- 관련 문서:
  - AGENTS.md (아키텍처 규칙의 Fake/Real 생각, 규칙 4 비밀값, 규칙 6 완료 전 확인, 규칙 8 커밋)
  - docs/architecture.md "서비스 기능 스택", "Pleos(주최측)에 문의할 것" A1 (외부 API·자체 서버 허용 여부. 서버 없이 만드는 이유)
  - docs/pleos/ux-guidelines.md "앱 심사·보안·개인정보 정책" (개인정보 처리방침, "기기에 저장하는 민감정보는 … 암호화", 로그 마스킹, 웹 페이지 연결 앱의 도메인 증빙)
  - docs/contest/requirements.md "출품 유의사항" (사실과 다른 제출 내용 금지, 이미지·폰트·라이브러리 권리), "GitHub 저장소 규칙" (README 필수 항목)
  - docs/conventions/git.md (커밋 단위, "의존성은 처음 쓰는 커밋에")
  - `.claude/hooks/build-gate.mjs`, `.githooks/pre-push`, `.github/workflows/android.yml`, `.claude/agents/tester.md`, `.claude/skills/ship/SKILL.md`, `.claude/settings.json`
  - docs/notes/xxj15/2026-11-03-dev-workflow-harness.md "확인" (하네스를 임시 저장소 시나리오로 확인한 방법)
  - docs/notes/xxj15/2026-11-03-stack-diagram-skill.md (Python Playwright 1.64.1 + Chromium 설치 기록, Windows는 `python`)

## 범위

- **필수**: 단계 0~12. 예상 2~2.5일 (에이전트 작업 기준, 사용자 확인 시간 제외).
  - 11/15 제출물에 주는 효과:
    - 기획서 DOCX: 보호자 등록 화면 스크린샷(빈 화면, 프로필, 의료정보)을 넣을 수 있다. 확인 스크립트가 같은 화면을 매번 같은 날짜로 찍어 준다.
    - 시연 영상: "보호자가 폰에서 등록 → 차에서 진료 카드" 이야기의 앞부분을 실제 화면으로 보여 준다.
    - GitHub 저장소: 웹 소스와 실행 방법이 같이 들어간다.
- **있으면 좋음**: 단계 A~E. 필수가 끝난 뒤, 단계 12(노트) 전에 한다.
- **이번 범위 밖** (사용자 결정 또는 서버 대기):
  - 진료이력 탭, 다음 진료 일정, 여러 마리 (사용자 결정)
  - Firebase(Firestore + Auth) 구현, 로그인, 웹 배포(호스팅). 문의 A1 답을 기다린다. 이번에는 **들어갈 자리(저장소 인터페이스)와 옮길 때 할 일(문서)만** 만든다.
  - 차 앱(Kotlin) 변경. 다음 계획에서 `PetProfile` 도메인 모델과 Fake(예시 JSON과 같은 값)를 만들고 진료 카드에 쓴다.
  - 웹과 차 앱 연결 (서버, QR 등)
  - PWA·오프라인 설치, 다크 모드, 다국어, 여러 탭 동시 편집 동기화
  - React 화면 컴포넌트 단위 테스트 (jsdom, Testing Library). 화면은 확인 스크립트로 본다 (설계 결정 8).
- **주행 중 안전 (AGENTS.md 규칙 3)**: 이 웹은 차량 화면이 아니라 보호자 폰 화면이다. 운전 중이 아니라 집이나 정차 중에 미리 등록하는 화면이라 규칙 3의 대상이 아니다. 차 앱 쪽 진료 카드는 다음 계획에서 규칙 3을 따른다.
- **Pleos**: SDK·API를 쓰지 않는다. 새 `pleos.car.permission.*` 없음.

## 배경과 사용자가 정한 것 (다시 묻지 않는다)

- 팀 방향 (2026-11-05): 보호자가 폰 웹에서 반려견 의료정보를 미리 등록 → 차 앱이 그 정보와 차에서 고른 증상으로 진료 카드를 자동 생성 → 병원 전화·길안내. 이 웹은 "등록" 쪽이다.
- 웹팀은 React 경험이 있다. 사용자는 안드로이드 초보이고 웹 위주다.
- 차와 웹을 잇는 서버(Firebase)는 문의 A1 답을 기다린다. 그래서 이번에는 서버 없이 만든다.
- 사용자 결정:
  1. 저장: 브라우저 localStorage 먼저. 저장소를 인터페이스로 빼서 나중에 Firebase(Firestore + Auth) 구현으로 갈아 끼운다.
  2. 위치: 같은 저장소의 `web/` 폴더. 안드로이드 프로젝트는 루트에 그대로.
  3. 범위: 반려견 1마리. 프로필(사진, 이름, 품종, 나이 또는 생년월일, 성별) + 의료정보(주요 질환, 알레르기, 복용 약). 보고 고치기.
  4. 기술: React + Vite + TypeScript, 테스트는 Vitest. "되게 간단하게". 의존성 최소, 버전은 정확히 고정.

### 디자인 참고 (사용자가 보여 준 컨셉 그림을 말로 옮긴 것)

- 폰 세로 화면. 맨 위에 "PawCare" 로고와 알림 아이콘.
- 큰 강아지 사진 → 이름 "보리 ♂" → "골든리트리버 · 12세 3개월" → [프로필 수정].
- 탭: 기본정보 / 의료정보 / 진료이력. 그림에서는 의료정보 탭이 열려 있다.
- 의료정보 탭에 "건강 정보" 표: 주요 질환 "심장질환" / 알레르기 "특정 단백질 알레르기" / 복용 중인 약 "심장약(1일 1회)". 아래 파란 [+ 의료정보 추가하기] 버튼.
- 밝은 배경, 남색·파란 포인트, 둥근 카드.
- 앱 이름은 미정이다 (웹 그림은 PawCare, 차 앱은 MeongCare). 그래서 웹에서는 이름을 상수 한 곳(`web/src/config.ts`)에서 바꾸게 한다.

## 설계 결정 (planner 판단과 근거)

구현하면서 바꾸면 "결정 / 배운 점"에 이유를 남긴다. `[사용자 승인 필요]`가 붙은 것은 "확인 필요"에서 묻는다. 아래는 **권장안 기준**이다.

### 1. 폴더 구조와 의존 방향 (앱의 ui → domain ← data와 같은 생각)

```
web/
  README.md              팀원용 실행 방법 (Node 버전, 명령, 폰으로 열어 보기)
  package.json           의존성 정확히 고정 (^, ~ 없음), engines.node
  package-lock.json      커밋한다. CI는 npm ci
  .npmrc                 save-exact=true, engine-strict=true
  index.html             lang="ko", viewport, <link rel="icon" href="data:,">, title은 중립 문구
  tsconfig*.json         고른 Vite 버전의 react-ts 템플릿 형식을 따른다 (strict)
  vite.config.ts         plugin-react + test 설정 (environment: 'node', include: src/**/*.test.ts)
  scripts/
    verify_screens.py    폰 크기 화면 확인 + 스크린샷 (Python Playwright, 설계 결정 9)
  src/
    main.tsx             React 시작. document.title = APP_NAME. container에서 저장소를 받아 App에 넘긴다
    config.ts            APP_NAME 등 바꿀 수 있는 이름 (앱 이름은 여기 한 곳)
    container.ts         저장소 구현을 고른다 (지금은 localStorage만). 앱의 AppContainer 자리
    domain/              React·브라우저를 모른다. Node에서 테스트한다
      petProfile.ts            PetProfile 타입, 스키마 버전, 길이 제한 상수
      petProfileJson.ts        형식 문서(docs/data/pet-profile.md)와 1:1인 읽기·쓰기
      age.ts                   나이 계산, 나이 → 생년월일, 나이 표시 문구
      petProfileRepository.ts  저장소 인터페이스와 결과 타입
    data/
      localStoragePetProfileRepository.ts   localStorage 구현 (Storage를 주입받는다)
    photo/
      fitWithin.ts         줄일 크기 계산 (순수 함수)
      resizePhoto.ts       파일 → 캔버스 → JPEG data URL (브라우저 전용)
    ui/
      App.tsx, usePetProfile.ts, profileScreenReducer.ts (순수)
      profileForm.ts, medicalForm.ts (폼 값 ↔ PetProfile 변환과 검증, 순수)
      Header.tsx, EmptyState.tsx, LoadErrorState.tsx, ProfileCard.tsx, Tabs.tsx,
      BasicInfoTab.tsx, MedicalInfoTab.tsx, ProfileForm.tsx, MedicalForm.tsx, PhotoPicker.tsx, StorageNotice.tsx
      styles.css           색·둥글기 변수는 맨 위 :root에만 둔다
```

- `ui/`는 `domain/`과 `photo/`만 import한다. `data/`는 `main.tsx`·`container.ts`만 안다 (완료 기준 4).
- 라우터 라이브러리를 쓰지 않는다. 화면은 상태(`보기 / 프로필 편집 / 의료정보 편집`)로 바꾼다. 브라우저 뒤로가기는 이번에 다루지 않는다 (있으면 좋음 B).
- 스타일 라이브러리, 아이콘 라이브러리, 웹폰트를 쓰지 않는다. 글꼴은 시스템 글꼴 목록(`-apple-system, "Apple SD Gothic Neo", "Noto Sans KR", "Malgun Gothic", sans-serif`)이다. 외부 요청이 없어서 개인정보·라이선스 걱정이 없다.

### 2. 의존성: 최소, 정확히 고정

| 구분 | 패키지 | 이유 |
|---|---|---|
| dependencies | `react`, `react-dom` | 웹팀이 아는 것 |
| devDependencies | `vite`, `@vitejs/plugin-react`, `typescript`, `@types/react`, `@types/react-dom` | 빌드와 타입. plugin-react는 저장할 때 화면이 바로 바뀌게(Fast Refresh) 한다 |
| devDependencies | `vitest` | 사용자 결정. 단계 2에서 첫 테스트와 함께 넣는다 (git.md: 처음 쓰는 커밋에) |

- 넣지 않는 것: ESLint 묶음(`[사용자 승인 필요]` 3), 라우터, 상태 관리 라이브러리, 폼 라이브러리, 스키마 검증 라이브러리(zod 등), jsdom, Testing Library, `@types/node`.
- 버전은 **단계 1에서 그날 확인한 최신 안정 버전**으로 고정한다. 이 계획에 버전을 미리 적지 않는다 (추측하지 않는다).
  - `npm view <패키지> version`으로 확인 → `npm install --save-exact`.
  - `vitest`는 단계 2에서 고정할 때 고른 `vite` 버전이 그 vitest의 peer 범위 안인지 `npm view vitest@<버전> peerDependencies`로 확인한다. `npm install`에 ERESOLVE·peer 경고가 나오면 멈추고 버전을 다시 고른다.
  - 고른 버전과 Node 요구(`npm view vite@<버전> engines`)를 진행 로그와 AGENTS.md에 적는다.
- 스크립트 (`web/package.json`):

  | 이름 | 하는 일 | 누가 부르나 |
  |---|---|---|
  | `dev` | `vite` | 사람 |
  | `typecheck` | `tsc -b` (템플릿 형식) | `build`가 부른다 |
  | `build` | `npm run typecheck && vite build` | 커밋 직전 훅·Stop 훅 (설계 결정 10) |
  | `test` | `vitest run` | `check` |
  | `check` | `npm run build && npm test` | 검증 파이프라인·CI |
  | `preview` | `vite preview` | 화면 확인 |

  - 루트에서 `cd` 없이 부른다: `npm --prefix web run check`. Bash와 PowerShell에서 같은 명령이고, 권한 규칙(`[사용자 승인 필요]` 2)도 한 줄로 맞출 수 있다.

### 3. 프로필 형식과 스키마 버전 (`docs/data/pet-profile.md`)

차 앱 진료 카드, 나중의 Firestore 문서가 같은 형식을 쓴다. 그래서 웹 코드가 아니라 **문서가 기준**이고, 웹의 `petProfileJson.ts`는 그 문서를 그대로 옮긴 것이다.

- 위치: `docs/data/pet-profile.md` + `docs/data/pet-profile.example.json`. `docs/data/`는 웹·차 앱·서버가 함께 따르는 데이터 형식을 두는 곳이다 (다음에 진료 카드 형식도 여기). `[사용자 승인 필요]` 6에서 위치를 확인한다.
- **스키마 버전을 둔다** (`schemaVersion: 1`). 이유:
  - localStorage 데이터는 코드보다 오래 산다. 보호자 브라우저에 v1 데이터가 남은 채로 새 코드가 배포된다.
  - 차 앱과 서버가 같은 JSON을 읽는다. 모르는 형식을 짐작해서 읽으면 복용 약을 잘못 보여 줄 수 있다. 의료정보는 "모른다"고 보이는 편이 틀리게 보이는 것보다 안전하다.
  - 비용은 숫자 필드 하나다.
- 버전 규칙 (문서에 적는다):
  - **선택 필드를 더하는 것**은 버전을 올리지 않는다. 읽는 쪽은 모르는 필드를 무시한다. (그래서 예전 웹으로 저장하면 새 필드가 빠질 수 있다. 지금은 쓰는 곳이 하나라 받아들인다.)
  - 필드 이름·뜻·타입을 바꾸거나 지우면 버전을 올린다. 웹은 `vN → vN+1` 변환 함수를 둔다.
  - 읽는 쪽은 자기가 아는 버전보다 높으면 **읽지 않는다** (`unsupported-version`). 짐작하지 않는다.
  - v2 후보 (지금 하지 않음): Firebase Storage로 옮길 때 `photoDataUrl` → `photoUrl`, 여러 마리를 할 때 `id`.
- 필드 (기본안. TypeScript로 쓰면 아래와 같다. 구현하면서 바꾸면 이 블록과 형식 문서를 같이 고친다):

  ```ts
  // web/src/domain/petProfile.ts (기본안)
  export const PET_PROFILE_SCHEMA_VERSION = 1;
  export type Sex = 'male' | 'female';
  export interface Medication { name: string; dosage: string }      // dosage: 복용법 자유 입력. 빈 문자열 허용
  export interface MedicalInfo {
    conditions: string[];    // 주요 질환
    allergies: string[];     // 알레르기
    medications: Medication[]; // 복용 중인 약
  }
  export interface PetProfile {
    schemaVersion: 1;
    name: string;                  // 필수
    breed: string | null;
    sex: Sex | null;               // null = 입력 안 함
    birthDate: string | null;      // 'YYYY-MM-DD' (시간대 없는 날짜)
    birthDateIsEstimated: boolean; // 나이로 입력했으면 true. birthDate가 null이면 false
    photoDataUrl: string | null;   // 'data:image/jpeg;base64,...' (긴 변 512px 이하)
    medical: MedicalInfo;
    updatedAt: string;             // ISO 8601 UTC, 마지막 저장 시각
  }
  ```

  | 필드 | 제한 (검증) | 진료 카드에 쓰나 | Kotlin 대응 (제안. 차 앱 계획에서 확정) |
  |---|---|---|---|
  | `schemaVersion` | 정확히 1 | - | `Int` |
  | `name` | 앞뒤 공백 제거 후 1~20자 | O | `String` |
  | `breed` | 0~30자, 비면 `null` | O | `String?` |
  | `sex` | `male`/`female`/`null` | O (♂/♀) | `enum Sex?` |
  | `birthDate` | 오늘보다 미래 아님, 30년 전보다 앞이 아님 | O (나이로 바꿔서) | `LocalDate?` |
  | `birthDateIsEstimated` | | O ("추정") | `Boolean` |
  | `photoDataUrl` | `data:image/jpeg;base64,`로 시작, 400,000자 이하 | 선택 | `String?` |
  | `medical.conditions[]` | 항목 1~50자, 최대 20개 | O | `List<String>` |
  | `medical.allergies[]` | 항목 1~50자, 최대 20개 | O | `List<String>` |
  | `medical.medications[]` | `name` 1~50자, `dosage` 0~50자, 최대 20개 | O | `List<Medication>` |
  | `updatedAt` | ISO 8601 | O ("정보 갱신일") | `Instant` |

- **빈 배열은 "없음"이 아니라 "등록하지 않음"이다.** 차 앱 카드에는 "등록된 정보 없음"으로 보인다. "알레르기 없음(확인함)"을 따로 표시하는 것은 이번에 하지 않는다 (나중 후보로 문서에 적는다).
- 이번에 넣지 않는 것: 체중, 중성화 여부, 마이크로칩 번호. 수의사에게 쓸모 있는 정보지만 사용자가 정한 범위 밖이다. 나중에 선택 필드로 더하면 버전을 올리지 않아도 된다 (`[사용자 승인 필요]` 5의 하위 질문).
- `id`를 넣지 않는다. 한 마리라 필요 없다. 넣을 때 주의: `crypto.randomUUID()`는 보안 컨텍스트(HTTPS, localhost)에서만 있다. 폰에서 `http://<PC IP>:5173`으로 열면 없다.
- 예시 JSON(`docs/data/pet-profile.example.json`)은 컨셉 그림의 값으로 쓴다: 보리, 골든리트리버, 수컷, `2014-08-01`, 주요 질환 "심장질환", 알레르기 "특정 단백질 알레르기", 약 "심장약" / "1일 1회", `photoDataUrl: null`.
  - 웹 테스트가 이 파일을 읽는다 (형식 문서와 코드가 어긋나면 테스트가 깨진다). 차 앱 테스트도 나중에 같은 파일을 읽게 할 수 있다.
  - Vitest가 `web/` 밖 JSON import를 막으면 `web/src/domain/fixtures/`로 옮기고 형식 문서가 그 파일을 가리키게 한다 (진행 로그에 적는다).

### 4. 나이 계산 (웹과 차 앱이 같은 규칙을 쓴다. 형식 문서에 적는다)

- 오늘 = 브라우저의 지역 날짜. 함수는 오늘을 인자로 받는다 (테스트에서 고정).
- 개월 수 = `(올해 - 태어난 해) × 12 + (이번 달 - 태어난 달)`. **오늘의 일(day)이 태어난 일보다 작으면 1을 뺀다.** 세 = 개월 수 ÷ 12, 남은 개월 = 개월 수 % 12.
- 표시: `12세 3개월`, 개월이 0이면 `12세`, 1년 미만이면 `3개월`, 1개월 미만이면 `1개월 미만`.
- 나이로 입력(세 0~30, 개월 0~11)하면 생년월일 = 오늘에서 그만큼 뺀 날. 그 달에 같은 날이 없으면 그 달 말일 (예: 3월 31일 - 1개월 = 2월 28일 또는 29일). `birthDateIsEstimated = true`.
  - 이 규칙이면 같은 날 다시 계산했을 때 입력한 나이가 그대로 나온다 (테스트로 1년치 날짜를 돌려 확인).
- 기본정보 탭에서 추정이면 "2014년 8월생 (나이로 입력)"처럼 보이고, 아니면 "2014년 8월 1일생". 헤더 부제는 둘 다 "12세 3개월".

### 5. 저장소 인터페이스 (Firebase로 갈아 끼울 자리)

```ts
// web/src/domain/petProfileRepository.ts (기본안)
export type LoadFailure = 'unavailable' | 'corrupted' | 'unsupported-version';
export type SaveFailure = 'quota' | 'unavailable' | 'unknown';
export type LoadResult = { ok: true; profile: PetProfile | null } | { ok: false; reason: LoadFailure };
export type SaveResult = { ok: true } | { ok: false; reason: SaveFailure };

export interface PetProfileRepository {
  /** 저장된 프로필. 없으면 profile: null */
  load(): Promise<LoadResult>;
  /** 받은 값을 그대로 저장한다 (updatedAt은 부르는 쪽이 채운다). 실패하면 이전 값은 그대로 남는다 */
  save(profile: PetProfile): Promise<SaveResult>;
}
```

- **localStorage도 `Promise`로 감싼다.** Firestore로 바꿀 때 화면 코드가 바뀌지 않게 하려는 것이다. 앱이 SDK 콜백을 `suspend`로 감싸는 것과 같은 생각이다.
- 실패는 예외가 아니라 결과 값으로 돌려준다. 테스트와 화면 분기가 단순해진다. Firestore가 생기면 `'network'` 같은 이유를 더한다.
- **localStorage 구현이 앱의 Fake 자리, Firebase 구현이 Real 자리다.** 별도 메모리 Fake는 만들지 않는다. localStorage 구현이 `Storage`(`getItem`/`setItem`)를 주입받으므로 테스트는 작은 가짜 Storage로 한다 (jsdom이 필요 없다).
- localStorage 구현 규칙:
  - 키: `owner-web.pet-profile` (고정). **앱 이름과 묶지 않는다.** 이름을 바꿔도 데이터가 사라지지 않게 하려는 것이다. `localhost:5173`을 다른 Vite 프로젝트와 같이 쓰므로 접두사를 붙인다.
  - 값: `petProfileJson.ts`로 만든 JSON 문자열 하나 (사진 포함). 사진 512px JPEG는 수십~수백 KB라 한 키에 둬도 된다.
  - `setItem`이 `QuotaExceededError`(Firefox는 `NS_ERROR_DOM_QUOTA_REACHED`)를 던지면 `quota`. 그 밖의 `DOMException`(예: `SecurityError`)은 `unavailable`, 나머지는 `unknown`.
  - `getItem`이 던지면 `unavailable`, JSON이 깨졌거나 필드가 틀리면 `corrupted`, 버전이 다르면 `unsupported-version`.
  - `window.localStorage`에 접근하는 것 자체가 던질 수 있다 (쿠키 차단 등). `container.ts`가 감싸서, 실패하면 항상 `unavailable`을 돌려주는 저장소를 넘긴다.

### 6. 화면 (컨셉 그림 기준, 폰 세로)

| 상태 | 화면 |
|---|---|
| 불러오는 중 | 헤더만 (짧아서 별도 표시 없음) |
| 빈 상태 (처음) | 헤더 + 카드: "아직 등록된 반려견이 없어요" + "병원에 갈 때 차에서 바로 진료 카드를 만들 수 있게 미리 등록해 두세요." + [반려견 등록하기] + 저장 위치 안내 (`StorageNotice`) |
| 읽기 실패 | "저장된 정보를 읽지 못했어요" + 이유별 한 줄 + (`corrupted`·`unsupported-version`일 때) [새로 등록하기]. 새로 등록해 저장할 때만 이전 값이 바뀐다 |
| 프로필 보기 | 헤더 → 큰 사진(없으면 이름 첫 글자 원) → "보리 ♂" → "골든리트리버 · 12세 3개월"(없는 값은 빼고, 둘 다 없으면 줄 없음) → [프로필 수정] → 탭 **의료정보 / 기본정보** → 탭 내용 → 저장 위치 안내 |
| 의료정보 탭 | "건강 정보" 카드: 주요 질환 / 알레르기 / 복용 중인 약 3행. 여러 개면 줄바꿈. 약은 "심장약 (1일 1회)". 비면 "등록된 정보 없음". 아래 파란 버튼: 모두 비었으면 [+ 의료정보 추가하기], 아니면 [의료정보 수정하기] |
| 기본정보 탭 | 이름, 품종, 성별, 생년월일(설계 결정 4), 나이 행 |
| 프로필 편집 | 사진 선택(미리보기, [사진 선택], [사진 빼기]) → 이름* → 품종 → 생년월일/나이 전환 (날짜 입력 또는 세·개월 숫자) → 성별 (수컷/암컷/선택 안 함) → [저장] [취소] |
| 의료정보 편집 | 세 묶음, 각 줄 = 입력칸 + [삭제], 묶음 아래 [+ 추가]. 약 줄은 약 이름 + 복용법 두 칸. [저장] [취소] |

- 탭은 **의료정보가 먼저 열린다** (컨셉 그림과 같고, 이 웹의 목적이 의료정보 등록이다). 진료이력 탭과 알림 아이콘은 두지 않는다 (기능이 없는 버튼을 보이지 않는다). `[사용자 승인 필요]` 5에서 확인한다.
- 처음 등록은 프로필 편집 화면에서 시작한다. 저장하면 프로필 보기(의료정보 탭, 비어 있음)로 간다.
- 입력 검증은 **[저장]을 누를 때** 한다. 칸 아래에 문구를 보이고 첫 오류 칸으로 스크롤·포커스한다. `maxLength` 속성에 기대지 않는다 (한국어 조합 입력 중에 잘리는 브라우저가 있는지 확인하지 않았다. 아래 "실행해서 확인할 것").
  - 문구 기본안: 이름 비었음 "이름을 입력해 주세요" / 길이 초과 "20자까지 쓸 수 있어요" / 미래 날짜 "오늘 이후 날짜는 고를 수 없어요" / 30년 초과 "30년 안쪽 날짜를 골라 주세요" / 숫자 아님·범위 밖 "0~30세, 0~11개월로 입력해 주세요" / 약 이름 없이 복용법만 "약 이름을 입력해 주세요" / 20개 초과 "20개까지 등록할 수 있어요"
  - 의료정보 편집에서 빈 줄은 저장할 때 버린다 (오류가 아니다). 앞뒤 공백은 지운다.
- 저장 실패: 편집 화면에 그대로 있고 입력값을 지우지 않는다. 화면 위 빨간 띠:
  - `quota`: "브라우저 저장 공간이 부족해서 저장하지 못했어요. 사진을 빼고 다시 저장해 보세요."
  - `unavailable`: "이 브라우저에서는 저장할 수 없어요. 시크릿 모드라면 끄고 다시 열어 주세요."
  - `unknown`: "저장하지 못했어요. 잠시 뒤 다시 시도해 주세요."
  - 저장 중에는 [저장]을 막는다 (두 번 눌러도 한 번만 저장).
- [취소]는 확인 없이 편집을 닫는다. 저장된 값은 그대로다.
- 저장 위치 안내 (`StorageNotice`, `[사용자 승인 필요]` 5): "이 정보는 지금 쓰는 브라우저에만 저장돼요. 차 앱과의 연결은 준비 중이에요." 작은 회색 글씨. 출품 유의사항(사실과 다른 내용 금지)과 개인정보 안내를 같이 해결한다.
- 모양:
  - 밝은 배경, 남색 제목·파란 버튼, 흰 둥근 카드 (반경 16px 안팎). 색 값은 `styles.css` 맨 위 변수에만 둔다.
  - 내용 폭 최대 480px, 가운데 정렬 (PC 브라우저에서 열어도 폰 모양).
  - 터치 타깃 44px 이상. 입력칸 글자 16px 이상 (iOS Safari가 포커스할 때 확대하지 않게).
  - `color-scheme: light`로 고정한다 (다크 모드 지원은 범위 밖. 다크 설정 폰에서도 읽히는지만 S15로 본다).
  - 모든 입력칸에 `<label>`을 단다. 확인 스크립트가 라벨로 칸을 찾으므로 접근성 확인도 된다.

### 7. 사진

- `<input type="file" accept="image/*">`. 고르면 바로 줄여서 미리보기에 보이고, [저장]할 때 프로필과 함께 저장한다.
- 순서 (`photo/resizePhoto.ts`):
  1. 파일이 20MB보다 크면 `too-large-file` (큰 사진을 폰 브라우저에서 디코딩하다 멈추는 것을 막는다).
  2. `URL.createObjectURL` → `Image` → `decode()`. 실패하면 `unreadable`. 끝나면 `revokeObjectURL`.
  3. `fitWithin(w, h, 512)`로 크기를 정한다. **키우지 않는다.**
  4. 캔버스를 **흰색으로 칠한 뒤** 그린다 (투명 PNG를 JPEG로 바꾸면 투명한 곳이 검게 나온다).
  5. `toDataURL('image/jpeg', 0.8)`. 400,000자를 넘으면 `too-large-result`.
- 문구: `too-large-file` "20MB보다 작은 사진을 골라 주세요" / `unreadable` "이 사진은 열 수 없어요. 다른 사진을 골라 주세요" / `too-large-result` "사진을 줄이지 못했어요. 다른 사진을 골라 주세요". 실패해도 이전 사진은 그대로다.
- 개인정보: 캔버스로 다시 만든 JPEG에는 원본의 EXIF(촬영 위치 GPS 등)가 들어가지 않는다. 문서에 적는다.
- 용량: 512px JPEG(0.8) data URL은 수십~수백 KB로 localStorage 한도(브라우저마다 다르고 보통 출처당 수 MB)에 한참 못 미친다. 그래도 한도는 브라우저가 정하므로 `quota` 처리를 둔다.
- 화면에서 크게 보일 때 512px가 흐릿하면 768px로 올린다 (용량 다시 계산해 기록). 사용자가 예로 든 값(512)으로 시작한다.

### 8. 테스트 범위

- Vitest, `environment: 'node'`. 테스트 파일은 대상 옆에 `*.test.ts`.
- 순수 로직만 단위 테스트한다: `domain/`, `photo/fitWithin.ts`, `data/` (가짜 Storage), `ui/`의 순수 파일(`profileScreenReducer.ts`, `profileForm.ts`, `medicalForm.ts`).
- React 컴포넌트 렌더링 테스트는 하지 않는다 (jsdom·Testing Library 의존성이 늘어난다). 화면과 캔버스(`resizePhoto.ts`)는 확인 스크립트(설계 결정 9)로 실제 브라우저에서 본다.

### 9. 화면 확인: `/verify` 대신 Python Playwright 스크립트

`/verify`는 Android용이다. 웹은 아래 순서로 본다. 스크립트는 커밋해서 다시 쓸 수 있게 한다.

1. `npm --prefix web run build`
2. 미리보기 서버를 백그라운드로 띄운다 (Bash `run_in_background`): `npm --prefix web run preview -- --port 4173 --strictPort --host 127.0.0.1`
3. `python web/scripts/verify_screens.py --base-url http://127.0.0.1:4173 --out .claude/.cache/web`
4. `.claude/.cache/web/*.png`를 Read로 열어 본다.
5. 백그라운드 서버를 끈다.

스크립트 규칙:
- Python 표준 라이브러리 + `playwright`(이미 설치됨, 노트 기준 1.64.1)만 쓴다. Windows에서는 `python`이다 (`python3`는 Store 바로가기).
- Chromium만 쓴다 (설치된 것). Safari(WebKit)는 확인하지 못한다고 보고한다 (있으면 좋음 E).
- 폰 크기: 390×844, 360×800. `is_mobile=True`, `has_touch=True`, `locale='ko-KR'`.
- **날짜 고정**: `page.clock.set_fixed_time('2026-11-05T09:00:00')`. 그래야 "12세 3개월"이 매번 같다.
- 시나리오마다 새 브라우저 컨텍스트(빈 localStorage)로 시작한다.
- 칸과 버튼은 라벨·역할로 찾는다 (`get_by_label`, `get_by_role`).
- 확인용 사진은 스크립트가 표준 라이브러리(`zlib`, `struct`)로 만든 PNG를 `.claude/.cache/web/fixtures/`에 쓴다: 가로 2000×1500, 세로 1500×2000, 사진이 아닌 파일(`.png` 이름의 텍스트). 저장소에 이미지 파일을 넣지 않는다 (권리 문제 없음).
- 실패하면 어떤 시나리오의 어떤 확인이 틀렸는지 출력하고 종료 코드 1. 모두 맞으면 0.
- 콘솔 에러(`page.on('console')`의 error)와 같은 출처가 아닌 요청(`page.on('request')`에서 `data:`, `blob:`, base URL 밖)이 있으면 실패. 의료정보가 밖으로 나가지 않는다는 확인이기도 하다.
- 출력은 UTF-8로 한다 (`sys.stdout.reconfigure(encoding='utf-8')`). Windows 콘솔이 CP949라 한국어가 깨질 수 있다.
- 스크린샷 이름: `s01-empty-390.png`처럼 시나리오 번호-이름-폭.

### 10. 하네스 (`[사용자 승인 필요]` 1·2)

지금 `build-gate.mjs`는 `.kt/.kts/.java/.xml/.gradle/.toml/.pro/.properties`만 본다. 웹 파일은 검사하지 않고, 검증 파이프라인·pre-push·CI도 웹을 돌리지 않는다. 그대로 두면 웹이 깨진 채로 dev에 들어갈 수 있다.

권장안 (4a): **Android와 같은 수준으로 웹을 검사한다.**

| 언제 | Android (지금) | 웹 (추가) |
|---|---|---|
| 커밋 직전 (precommit) | 앱 코드가 들어가면 `assembleDebug` | 웹 파일이 들어가면 `npm --prefix web run build` |
| 턴 끝 (Stop) | 이번 턴에 앱 코드가 바뀌었으면 `assembleDebug` | 이번 턴에 웹 파일이 바뀌었으면 `build` |
| 검증 파이프라인 (`pipeline`, `/ship`) | `assembleDebug testDebugUnitTest lintDebug` | `npm --prefix web run check` |
| pre-push (dev·main) | 파이프라인 통과 확인, 커밋 안 된 앱 코드가 있으면 막음 | 같은 규칙에 웹 파일도 포함 |
| CI | `android.yml` | 새 `web.yml`: `setup-node`(npm 캐시, `web/package-lock.json`) → `npm ci` → `npm run check`. `web/package.json`이 없으면 건너뛴다 |

`build-gate.mjs`에서 지킬 계약 (구현 방법은 구현자가 정한다):
- 웹 대상 파일 = `web/` 아래 + `docs/data/` 아래 (웹 테스트가 예시 JSON을 읽는다). gitignore 대상(`node_modules`, `dist`)은 읽지 않는다 (지금처럼 `git ls-files --cached --others --exclude-standard`).
- 앱 코드 판별(`IGNORE`)에 `web/`을 더한다. 웹 파일이 Gradle을 돌리지 않게 한다.
- 웹 검사는 `web/package.json`이 있을 때만 한다 (지금 dev처럼 없으면 건너뛴다. `gradlew`가 없을 때와 같은 방식).
- 해시와 "이미 통과" 기록을 대상별로 둔다. 파이프라인 통과 기록은 **두 대상의 해시를 합친 값**이다. 웹만 바뀌어도 다시 돈다.
- `node_modules`가 없거나 `web/package-lock.json`이 `web/node_modules/.package-lock.json`보다 새로우면 먼저 `npm --prefix web ci`를 한다 (새 팀원 PC, 다른 팀원이 의존성을 바꾼 뒤).
- Windows에서는 `npm`이 `npm.cmd`라 `shell: true`로 부른다. 제한 시간은 Gradle과 같은 570초.
- 실패 메시지 형식, Stop 무한 루프 방지(같은 상태로 다시 멈추면 놓아줌, 3회 연속 실패)는 Android와 같게 한다.
- 파이프라인 출력의 마지막 줄 "검증 파이프라인 통과."는 그대로 둔다 (다른 계획의 완료 기준이 이 문장을 본다).
- 파일 맨 위 설명 주석과 `PIPELINE` 옆 "android.yml과 같게 유지" 주석에 웹을 더한다.

그 밖에 바꿀 하네스:
- `.claude/agents/tester.md`: 웹 대상일 때의 규칙을 더한다 (테스트 위치 `web/src/**/*.test.ts`, 실행 `npm --prefix web test`, `web/src`의 테스트가 아닌 파일은 고치지 않는다, 새 npm 의존성 금지, jsdom 없는 node 환경). 지금 문서는 `app/src/test/`와 Gradle만 말한다.
- `.claude/settings.json` (`[사용자 승인 필요]` 2):
  - allow: `Bash(npm --prefix web run *)`, `Bash(npm --prefix web test*)`, `PowerShell(npm --prefix web run *)`, `PowerShell(npm --prefix web test*)`, `Bash(python web/scripts/verify_screens.py*)`, `PowerShell(python web/scripts/verify_screens.py*)`
  - deny: `Read(**/.env.local)`, `Read(**/.env.*.local)` (나중 Firebase 설정. 지금 있는 비밀값 규칙과 같게)
  - `npm install`, `npm ci`는 그대로 묻는다 (의존성 설치 스크립트가 돌기 때문).
- `.githooks/pre-push`, `.claude/skills/ship/SKILL.md`, `docs/conventions/git.md`, `CLAUDE.md` "훅", `README.md` 팀원 가이드: "검증 파이프라인 = `assembleDebug testDebugUnitTest lintDebug`" 문구에 웹 `check`를 더한다. git.md scope 예에 `web`을 더한다.
- 하네스 확인은 001처럼 **임시 저장소 시나리오**로 한다 (아래 H1~H12). 실제 저장소에서 마지막으로 `pipeline`을 한 번 돌린다.

버린 대안:
- 4b. 파이프라인·pre-push·CI에만 넣는다 (커밋 직전·Stop은 그대로). 코드가 덜 바뀌지만, 깨진 웹 커밋이 작업 브랜치에 쌓이고 `/ship` 때 한꺼번에 드러난다.
- 4c. 하네스를 바꾸지 않는다. `/ship` 전에 `npm --prefix web run check`를 손으로 돌린다. 사람이 터미널에서 push하면 웹 검사가 빠진다.

### 11. 개인정보와 비밀값 (문서로 남길 내용)

`docs/architecture.md` "보호자 웹" 절의 "저장과 개인정보"에 적는다:
- 지금 의료정보와 사진은 **보호자 브라우저의 localStorage에 평문으로** 있다.
  - 같은 출처의 스크립트는 모두 읽을 수 있다. 그래서 외부 스크립트(분석 도구, CDN, 웹폰트)를 넣지 않고, `dangerouslySetInnerHTML`을 쓰지 않는다 (완료 기준 4). React는 글자를 이스케이프한다.
  - 브라우저가 지울 수 있다 (방문 기록 삭제, 시크릿 모드, 브라우저 정책). 서버 전까지 백업이 없다.
  - 같은 폰·브라우저를 쓰는 다른 사람이 볼 수 있다.
  - 브라우저 안에서 암호화해도 키가 같은 브라우저에 있어 실익이 적다. 서버로 옮길 때 접근 규칙으로 지킨다.
  - Pleos 보안 가이드는 "기기에 저장하는 민감정보는 … 암호화"하라고 한다 (차 앱 대상, docs/pleos/ux-guidelines.md). 웹은 Pleos 앱이 아니지만, 심사·PT에서 설명할 때 이 차이를 알고 있어야 한다.
- 의료정보를 로그에 남기지 않는다 (`console.log` 0건). 사진의 EXIF(위치)는 다시 인코딩하면서 빠진다.
- 반려동물 의료정보의 법적 분류는 확인하지 않았다. 민감정보처럼 다룬다. 보호자 이름·연락처 같은 사람 정보는 이번에 받지 않는다.
- **서버(Firebase)로 옮길 때 할 일** (문의 A1 이후):
  - Firebase Auth로 보호자를 구분하고, Firestore 보안 규칙으로 자기 문서만 읽고 쓰게 한다. 규칙 없는 테스트 모드로 배포하지 않는다.
  - 수집 항목·목적·보유 기간을 알리고 동의 버튼을 둔다. 개인정보처리방침 URL을 만든다 (Pleos 앱 제출 폼 필수 항목, ux-guidelines).
  - 브라우저에 있던 데이터는 보호자가 동의한 뒤 한 번 올리고, 올린 뒤 브라우저에서 지운다.
  - 사진은 Firestore 문서가 아니라 Storage에 두고 `photoUrl`로 가리킨다 (스키마 v2).
  - 보호자가 자기 데이터를 지울 수 있게 한다.
  - 차 앱이 웹 페이지를 연결하면 도메인 등록 증빙이 필요하다 (ux-guidelines "외부 서버·네트워크").
- 비밀값 방향 (이번에는 파일을 만들지 않는다):
  - 나중 Firebase 설정은 `web/.env.local`(gitignore)에서 `VITE_FIREBASE_*`로 받는다. 값이 없으면 localStorage 구현을 쓰고 빌드는 그대로 된다 (AGENTS.md 규칙 4, secrets 기본값 원칙과 같다). 그때 `web/.env.example`을 같이 만든다.
  - `VITE_`로 시작하는 값은 번들에 그대로 들어가 누구나 볼 수 있다. **Claude API 키, 카카오 키는 웹에 절대 넣지 않는다** (서버에만).
  - Firebase 웹 설정값은 브라우저에 보이는 값이고, 보안은 Auth와 보안 규칙이 맡는다. 그래도 저장소에는 넣지 않는다.
  - 이번 단계 1에서 `.gitignore`에 `web/.env.local`, `web/.env.*.local`을 미리 넣는다.

## 확인 필요 (시작 전에 해결)

### [사용자 승인 필요]

새 `pleos.car.permission.*`은 필요 없다. 아래를 한 번에 묻는다. 모두 권장안이 위 설계 결정에 반영돼 있다.

1. **하네스에 웹 검사를 넣을까?** (단계 4·5 전에 필요. 그 전 단계는 진행해도 된다)
   - **4a. Android와 같게: 커밋 직전·Stop·파이프라인·pre-push·CI 모두 (권장).** 설계 결정 10. 약 0.5일.
   - 4b. 파이프라인·pre-push·CI만.
   - 4c. 바꾸지 않는다. `/ship` 전에 손으로 `npm --prefix web run check`.
   - 함께 바뀌는 것: `tester.md`에 웹 규칙, `git.md`·`ship` 스킬·`CLAUDE.md`·README 팀원 가이드의 파이프라인 설명, 새 `.github/workflows/web.yml`.
2. **Claude Code 권한 (`.claude/settings.json`)** (단계 5에서 바꾼다)
   - **2a. `npm --prefix web run *`, `npm --prefix web test`, `python web/scripts/verify_screens.py`는 묻지 않게, `.env.local` 읽기는 막기 (권장).** `npm install`·`npm ci`는 계속 묻는다.
   - 2b. 그대로 둔다 (웹 명령마다 확인 창).
3. **ESLint를 넣을까?** (단계 1 전에 필요. 의존성이 달라진다)
   - **3a. 넣지 않는다. TypeScript `strict` 타입 검사를 "린트" 자리로 쓴다 (권장).** "되게 간단하게"와 맞는다.
   - 3b. 넣는다 (Vite 템플릿 구성: `eslint`, `@eslint/js`, `typescript-eslint`, `eslint-plugin-react-hooks`, `eslint-plugin-react-refresh`, `globals` 정도). React Hooks 실수(의존성 배열)를 잡아 준다. `check`에 `lint`가 더해진다.
4. **README(심사위원용)에 웹을 넣을까?** (단계 11 전에 필요)
   - **4a. 지금 짧은 "보호자 웹 (선택)" 절: 실행 명령 3줄 + "정보는 그 브라우저에만 저장되고, 차 앱과는 아직 연결되지 않았다" (권장).** main을 보는 심사위원이 `web/` 폴더를 보고 궁금해할 수 있다. 사실대로 쓴다.
   - 4b. 서버로 차 앱과 연결된 뒤에 넣는다. 그때까지는 `web/README.md`에만 둔다.
5. **화면 기본안을 그대로 갈까?** (단계 6 전에 필요)
   - 필수 입력은 **이름 하나**. 나머지는 비워도 저장된다 (보호자가 모르는 값이 있어도 등록을 막지 않는다).
   - 탭은 **의료정보 / 기본정보** 두 개, 의료정보가 먼저 열린다. **진료이력 탭은 숨긴다** (대안: "준비 중"으로 보이되 누를 수 없게). 알림 아이콘은 두지 않는다.
   - 저장 위치 안내 문구: "이 정보는 지금 쓰는 브라우저에만 저장돼요. 차 앱과의 연결은 준비 중이에요." (빈 화면과 프로필 화면 아래)
   - 체중·중성화 여부는 이번에 넣지 않는다 (사용자가 정한 범위 그대로). 진료 카드에 필요하면 다음에 선택 필드로 더한다.
6. **형식 문서 위치** `docs/data/pet-profile.md` (+ 예시 JSON)로 괜찮은가? (단계 2 전에 필요. 기본안대로 가도 되는 가벼운 질문)

### [선행: pleos-docs-researcher]

- **없다.** Pleos SDK·API를 쓰지 않는다.

### 실행해서 확인할 것 (문서에 없음, 추측하지 않는다)

- 로컬 `node -v`, `npm -v`. 고른 Vite 버전의 Node 요구를 만족하지 않으면 멈추고 사용자에게 Node 업데이트를 부탁한다 (설치는 사용자가 한다). CI의 `node-version`과 `engines.node`를 같은 메이저로 맞춘다.
- `python -c "import playwright; print(playwright.__version__)"`와 Chromium 설치 여부. 없으면 `pip install playwright && python -m playwright install chromium` (README "처음 한 번" 5번)을 사용자에게 부탁한다.
- Vitest가 `web/` 밖의 `docs/data/pet-profile.example.json`을 import할 수 있는지 (막히면 설계 결정 3의 대안).
- 폰 사진의 회전(EXIF orientation)이 캔버스로 줄인 뒤에도 맞는지, iPhone HEIC 사진을 고르면 어떻게 되는지. Chromium 스크립트와 합성 이미지로는 확인할 수 없다. 실제 폰 확인은 있으면 좋음 D. 못 하면 못 했다고 노트에 적는다. 열 수 없는 사진은 `unreadable` 문구로 대비한다.
- 한국어 입력 중 글자 수 제한이 이상하게 동작하는지 (그래서 `maxLength` 대신 저장할 때 검증한다).
- 512px 사진이 390px 폭 화면에서 흐릿한지 (설계 결정 7).
- Playwright로 localStorage를 다른 키로 가득 채워 `QuotaExceededError`를 실제로 일으킬 수 있는지 (S11). 안 되면 S11은 단위 테스트로만 확인했다고 적는다.

## 확인 절차

### 화면 (`web/scripts/verify_screens.py`, 날짜 고정 2026-11-05)

| # | 조작 | 기대 |
|---|---|---|
| S1 | 빈 localStorage로 연다 | 헤더 `APP_NAME`("PawCare"), "아직 등록된 반려견이 없어요", [반려견 등록하기], 저장 위치 안내. `document.title`이 `APP_NAME` |
| S2 | [반려견 등록하기] → 이름 비운 채 [저장] | 이름 칸 아래 "이름을 입력해 주세요". localStorage에 `owner-web.pet-profile` 키 없음 |
| S3 | 이름 21자 → [저장] / 생년월일 2026-12-01 → [저장] / 나이 모드에서 개월 12 → [저장] | 각각 길이·미래 날짜·범위 문구. 저장 안 됨 |
| S4 | 이름 "보리", 품종 "골든리트리버", 생년월일 2014-08-01, 수컷, 가로 2000×1500 사진 선택 | 미리보기에 사진 |
| S5 | [저장] | 프로필 보기: "보리 ♂", "골든리트리버 · 12세 3개월", [프로필 수정], 의료정보 탭이 열려 있고 3행 모두 "등록된 정보 없음", [+ 의료정보 추가하기]. 저장된 JSON: `schemaVersion` 1, `photoDataUrl`이 `data:image/jpeg`로 시작, 페이지에서 그 이미지를 열면 512×384 |
| S6 | 기본정보 탭 | 이름, 품종, 성별 수컷, "2014년 8월 1일생", 나이 "12세 3개월" |
| S7 | [프로필 수정] → 세로 1500×2000 사진 → [저장] | 저장된 사진 384×512 |
| S8 | 의료정보 탭 → [+ 의료정보 추가하기] → 질환 "심장질환", 알레르기 "특정 단백질 알레르기", 약 "심장약"/"1일 1회", 질환에 빈 줄 하나 더 → [저장] | 표: 주요 질환 심장질환 / 알레르기 특정 단백질 알레르기 / 복용 중인 약 심장약 (1일 1회). 빈 줄 없음. 버튼이 [의료정보 수정하기]. 저장된 JSON의 `medical`이 예시 JSON과 같은 값 |
| S9 | 새로고침 | S8 화면 그대로 |
| S10 | [프로필 수정] → 이름 "보리2" → [취소] | 이름 "보리" 그대로. 저장된 JSON도 그대로 |
| S11 | 다른 키로 저장 공간을 가득 채움 → [프로필 수정] → 이름 "보리3" → [저장] | 저장 실패 띠(`quota` 문구), 편집 화면 유지, 입력값 "보리3" 유지, 저장된 이름은 "보리". 채운 키를 지우고 [저장] → 프로필 보기 "보리3" |
| S12 | `owner-web.pet-profile`에 `{broken` → 새로고침 | "저장된 정보를 읽지 못했어요" + [새로 등록하기] |
| S13 | 프로필 편집에서 사진이 아닌 파일 선택 | "이 사진은 열 수 없어요" 문구, 이전 미리보기 그대로 |
| S14 | 360×800에서 S1, S5, S8 | `document.documentElement.scrollWidth <= window.innerWidth` (가로 스크롤 없음). 스크린샷에서 글자 잘림·겹침 없음 |
| S15 | `color_scheme='dark'`로 S5 | 밝은 화면 그대로, 글자가 보인다 |
| S16 | 모든 시나리오 | 콘솔 에러 0건, 같은 출처 밖 요청 0건 |

- S1~S13은 390×844에서 돌린다. 스크린샷을 Read로 열어 위 "디자인 참고"와 비교한다: 밝은 배경, 남색·파란 포인트, 둥근 카드, 큰 사진, 이름+성별 기호, 부제, [프로필 수정], 탭, "건강 정보" 표, 파란 버튼. (진료이력 탭과 알림 아이콘은 `[사용자 승인 필요]` 5의 기본안대로면 없다.)
- 시나리오는 화면 단계(6~8)마다 그 단계에서 볼 수 있는 부분만 먼저 넣고, 단계 10에서 전부 돌린다.

### 하네스 (단계 4·5, `[사용자 승인 필요]` 1이 4a일 때. 4b면 H3·H4·H9·H10을 뺀다)

임시 저장소(스크래치 폴더)에 이 저장소의 `.claude/hooks/build-gate.mjs`를 복사하고, 가짜 `gradlew`(종료 코드를 파일로 조절)와 가짜 `web/package.json`(`build`·`check` 스크립트가 `node -e`로 `FAIL` 파일이 있으면 1로 끝남, `node_modules/.package-lock.json`을 미리 만들어 `npm ci`를 피함)을 둔다.

| # | 상황 | 기대 |
|---|---|---|
| H1 | `web/package.json` 없음, 앱 코드 커밋 | 지금처럼 `assembleDebug`만 |
| H2 | 문서만 커밋 | 아무것도 돌지 않는다 |
| H3 | 웹 파일만 커밋 | 웹 `build`만 돈다. Gradle은 돌지 않는다 |
| H4 | 웹 `build` 실패 상태로 웹 파일 커밋 | 커밋을 막고 실패 끝부분을 보여 준다 |
| H5 | 앱 코드만 커밋 | `assembleDebug`만. 웹은 돌지 않는다 |
| H6 | `pipeline` | Android 파이프라인과 웹 `check`가 모두 돈다. "검증 파이프라인 통과." |
| H7 | 같은 상태로 `pipeline` 다시 | "이미 검증 파이프라인을 통과했다" |
| H8 | H7 뒤 `docs/data/` 파일만 바꿔 커밋 → `pipeline` | 다시 돈다 (웹 대상에 포함) |
| H9 | 이번 턴에 웹 파일만 바꾸고 `build` 실패 → Stop | exit 2. 고치지 않고 다시 Stop → 놓아준다 |
| H10 | 웹 `node_modules`(gitignore)에 `.xml`·`.properties` 파일이 있음 → Stop | 앱 코드 해시가 바뀌지 않아 Gradle을 돌리지 않는다 |
| H11 | 커밋 안 된 웹 변경이 있는 채로 bare 원격의 dev에 push | pre-push가 막는다 |
| H12 | 회귀: main 직접 커밋, Claude 트레일러 커밋, `PROMOTE_MAIN` 없는 main push | 지금처럼 막는다 |

## 단계

단계 하나 = 커밋 1~3개. 앱 코드(Kotlin)는 건드리지 않는다. 파일 이름으로만 `git add`한다. 미추적 폴더 `.agents/`, `.codex/`는 이 작업과 관계없다. 스테이징하지 않는다.

**시작 전 준비:**
- 위 `[사용자 승인 필요]` 1~6을 사용자에게 한 번에 묻고 답을 진행 로그에 적는다. 3(ESLint)은 단계 1 전에 꼭 필요하다. 1·2는 단계 4 전까지만 정해지면 된다.
- "실행해서 확인할 것"의 Node·Python Playwright를 먼저 확인한다.

**순서 메모:** 기본 순서(인터페이스·Fake → 도메인·UI → Real → tester → 확인 → 노트)를 웹에 맞췄다. 저장소 인터페이스와 localStorage 구현(Fake 자리)을 먼저 하고, 하네스로 검사를 건 다음 화면을 만든다. Real(Firebase)은 이번 범위 밖이다.

### 필수

- [x] 0. 이 계획 파일 커밋 → `docs(plans): 008 보호자 웹 계획 추가`
  - 확인: `git log -1 --stat`에 이 파일만 있다.

- [x] 1. 웹 프로젝트 뼈대와 화면 확인 스크립트
  - **`.gitignore`부터 고친다. 그다음에 `npm install`을 한다.** 순서를 바꾸면 `node_modules` 안의 `.xml`·`.properties`가 build-gate의 앱 코드 해시에 들어가 Gradle이 돌고 해시가 느려진다.
    - 더할 줄: `web/node_modules/`, `web/dist/`, `web/.env.local`, `web/.env.*.local`, `*.tsbuildinfo`
  - 참고용으로 스크래치 폴더에 `npm create vite@<고른 버전> <이름> -- --template react-ts`를 만들어 tsconfig·vite.config 형식만 본다. `web/`에는 설계 결정 1의 파일만 손으로 둔다 (템플릿의 ESLint 설정, 예제 이미지, `App.css`는 가져오지 않는다. 3b면 ESLint 설정은 가져온다).
  - `web/src/config.ts`의 `APP_NAME = 'PawCare'`, `main.tsx`의 `document.title`, 헤더만 있는 `App.tsx`, `styles.css`의 색·둥글기 변수.
  - 이 단계의 `package.json` 스크립트는 `dev`, `typecheck`, `build`, `preview`만 둔다. `test`·`check`는 `vitest`와 함께 단계 2-2에서 더한다 (git.md: 의존성은 처음 쓰는 커밋에).
  - Node 메이저를 정해 `engines.node`에 적는다 (로컬 `node -v`와 고른 Vite의 요구를 둘 다 만족하는 값). 단계 5의 CI도 이 값을 쓴다.
  - `web/README.md`: 목적 한 줄, 필요한 Node, `npm --prefix web ci` / `run dev` / `run check`, 화면 확인 순서(설계 결정 9), 같은 Wi-Fi의 폰으로 열기(`npm --prefix web run dev -- --host` → `http://<PC IP>:5173`. 학교·회사 망에서는 막힐 수 있다).
  - 커밋:
    - 1-1. → `build(web): React·Vite·TypeScript 보호자 웹 뼈대 추가` (`.gitignore`, `web/` 설정 파일, `package-lock.json`, `src/main.tsx`, `src/App.tsx`, `src/config.ts`, `src/ui/styles.css`, `web/README.md`)
    - 1-2. → `chore(web): 폰 크기 화면 확인 스크립트 추가` (`web/scripts/verify_screens.py`. 이 단계에서는 S1의 헤더·`document.title`과 S16만. 시나리오는 화면 단계마다 같은 커밋에서 늘린다)
  - 확인: `npm --prefix web run build` 성공, 설계 결정 9 순서로 스크립트 종료 코드 0, `.claude/.cache/web/s01-*.png`에 헤더가 보인다. 완료 기준 6의 `git ls-files web` 확인.

- [x] 2. 프로필 형식 문서와 도메인
  - 2-1. → `docs(data): 반려견 프로필 형식 문서와 예시 JSON 추가`
    - `docs/data/pet-profile.md`: 목적(웹·차 앱·서버가 같이 따름), 필드 표(설계 결정 3), 빈 배열의 뜻, 나이 계산 규칙과 예(설계 결정 4), 버전 규칙, 브라우저 저장 위치(키 `owner-web.pet-profile`), Kotlin 대응 표(제안), 나중 후보(체중, 중성화, "없음 확인", `id`, `photoUrl`)
    - `docs/data/pet-profile.example.json` (설계 결정 3의 값)
  - 2-2. → `feat(web): 반려견 프로필 형식과 나이 계산 추가` (`vitest`를 여기서 넣는다)
    - `domain/petProfile.ts`, `domain/petProfileJson.ts` (`serializePetProfile`, `parsePetProfile` → 성공 또는 `corrupted`/`unsupported-version`), `domain/age.ts`
    - `domain/age.test.ts`:
      - `태어난 달과 일을 지났으면 그만큼 개월을 센다`
      - `오늘의 일이 태어난 일보다 작으면 한 달을 덜 센다`
      - `12세 3개월처럼 세와 개월을 함께 표시한다` (2014-08-01, 오늘 2026-11-05)
      - `개월이 0이면 세만 표시한다`
      - `1년이 안 되면 개월만 표시한다`
      - `1개월이 안 되면 1개월 미만으로 표시한다`
      - `나이로 입력하면 오늘에서 그만큼 뺀 날이 생년월일이다`
      - `그 달에 같은 날이 없으면 말일로 맞춘다` (3월 31일 - 1개월)
      - `나이로 입력한 값은 같은 날 다시 계산하면 그대로 나온다` (한 해의 모든 날 × 여러 나이)
    - `domain/petProfileJson.test.ts`:
      - `예시 JSON 파일을 읽을 수 있다` (`docs/data/pet-profile.example.json`)
      - `저장한 프로필을 다시 읽으면 같은 값이다`
      - `schemaVersion이 없거나 1이 아니면 버전 문제로 읽지 않는다`
      - `JSON이 깨졌으면 손상으로 읽지 않는다`
      - `필수 필드가 빠졌거나 타입이 틀리면 손상으로 읽지 않는다`
      - `모르는 필드는 무시하고 읽는다`
      - `birthDate 형식이 YYYY-MM-DD가 아니면 손상으로 읽지 않는다`
  - 확인: `npm --prefix web run check` 통과. 테스트 수를 진행 로그에 적는다.

- [x] 3. 저장소 인터페이스와 localStorage 구현 → `feat(web): 프로필 저장소 인터페이스와 localStorage 구현 추가`
  - `domain/petProfileRepository.ts` (설계 결정 5), `data/localStoragePetProfileRepository.ts`, `container.ts` (`window.localStorage` 접근 실패도 감싼다)
  - `data/localStoragePetProfileRepository.test.ts` (가짜 Storage, `DOMException`은 Node 전역에 있다):
    - `저장된 것이 없으면 프로필 없음을 돌려준다`
    - `저장한 뒤 읽으면 같은 프로필이다`
    - `저장 공간이 가득 차면 quota로 실패하고 이전 값은 그대로다`
    - `Firefox의 저장 공간 초과 이름도 quota로 본다`
    - `저장소를 쓸 수 없으면 unavailable로 실패한다` (`setItem`이 `SecurityError`)
    - `읽을 때 저장소가 던지면 unavailable이다`
    - `깨진 값이 저장돼 있으면 corrupted다`
    - `다른 버전이 저장돼 있으면 unsupported-version이다`
    - `앱 이름과 상관없이 정해진 키 하나에 저장한다`
  - 확인: `npm --prefix web run check` 통과. 완료 기준 4의 `localStorage` Grep.

- [x] 4. 하네스: build-gate 웹 검사 (`[사용자 승인 필요]` 1이 4a·4b일 때. 4c면 건너뛰고 진행 로그에 적는다)
  - 4-1. → `refactor(harness): build-gate 검사 대상을 목록으로 정리` (동작 변화 없음. 대상별 감시 규칙·해시·작업을 한 표로 묶는다. H1·H2·H5·H12로 확인)
  - 4-2. → `chore(harness): build-gate에 웹 검사 추가` (설계 결정 10의 계약. `.githooks/pre-push` 설명 주석 포함)
  - 확인: 하네스 시나리오 H1~H12 (4b면 H3·H4·H9·H10 제외). 결과를 진행 로그에 적는다. 그다음 실제 저장소에서 `node .claude/hooks/build-gate.mjs pipeline` → 두 대상 모두 돌고 "검증 파이프라인 통과."
  - 주의: 정규식·백슬래시가 들어간 파일은 Write/Edit 도구로 쓴다 (001 노트).

- [x] 5. 하네스: CI, tester, 권한, 문서 (`[사용자 승인 필요]` 1·2의 답대로)
  - 5-1. → `ci: 보호자 웹 검사 워크플로 추가` (`.github/workflows/web.yml`. `node-version`은 단계 1에서 정한 메이저. `web/package.json`이 없으면 건너뛴다)
  - 5-2. → `chore(harness): tester와 권한 설정을 보호자 웹에 맞춤` (`.claude/agents/tester.md` 웹 규칙, `.claude/settings.json` 2a)
  - 5-3. → `docs: 검증 파이프라인 설명에 웹 검사 추가` (`docs/conventions/git.md` 파이프라인 설명과 scope `web`, `.claude/skills/ship/SKILL.md`, `CLAUDE.md` "훅", `README.md` 팀원 가이드 "규칙"의 파이프라인 문구)
  - 확인: `web.yml` 문법은 push 뒤 CI에서만 확인된다. 이 단계에서는 YAML을 다시 읽어 `android.yml`과 같은 트리거·건너뛰기 방식인지 본다. dev push 뒤 CI 결과는 `/ship` 보고에 적는다.

- [ ] 6. 화면 상태, 빈 화면, 프로필 등록·수정, 프로필 보기 (`[사용자 승인 필요]` 5의 답대로)
  - 6-1. → `feat(web): 화면 상태 관리와 빈 화면·읽기 실패 화면 추가`
    - `ui/profileScreenReducer.ts`(순수), `ui/usePetProfile.ts`, `Header`, `EmptyState`, `LoadErrorState`, `StorageNotice`
    - `ui/profileScreenReducer.test.ts`:
      - `불러온 프로필이 없으면 빈 상태다`
      - `불러오기에 실패하면 이유와 함께 읽기 실패 상태다`
      - `저장에 성공하면 새 프로필을 보여 주고 편집을 닫는다`
      - `저장에 실패하면 편집을 유지하고 이유를 남기며 이전 프로필은 그대로다`
      - `저장 중에는 다시 저장을 시작하지 않는다`
      - `편집을 취소하면 저장 실패 표시도 지운다`
    - 스크립트: S1, S12 (S12는 6-2의 [새로 등록하기] 뒤에 끝까지 확인해도 된다)
  - 6-2. → `feat(web): 반려견 프로필 등록·수정 폼 추가`
    - `ui/profileForm.ts`(순수: 폼 값 ↔ 프로필, 검증 오류 코드), `ProfileForm.tsx` (사진 칸은 6-2에서는 자리만)
    - `ui/profileForm.test.ts`:
      - `이름이 비었거나 공백뿐이면 필수 오류다`
      - `이름은 앞뒤 공백을 지우고 20자까지다`
      - `품종은 비워도 되고 비면 null로 저장한다`
      - `생년월일이 오늘보다 뒤면 오류다`
      - `생년월일이 30년보다 앞이면 오류다`
      - `나이로 입력하면 세 0~30, 개월 0~11만 받는다`
      - `숫자가 아닌 나이는 오류다`
      - `성별을 고르지 않아도 저장된다`
      - `프로필을 수정해도 의료정보와 사진은 그대로다`
      - `저장할 때 updatedAt을 받은 현재 시각으로 채운다`
    - 스크립트: S2, S3, S10
  - 6-3. → `feat(web): 프로필 화면과 기본정보 탭 추가`
    - `ProfileCard`(사진 또는 이름 첫 글자, 이름+성별 기호, 부제, [프로필 수정]), `Tabs`, `BasicInfoTab`, 의료정보 탭 자리(빈 표)
    - 스크립트: S4(사진 빼고), S5(프로필 카드 부분만. 사진은 단계 7, 의료정보 탭 내용과 버튼은 단계 8), S6, S9(프로필만), S14(S1·S5만), S15
  - 확인: `npm --prefix web run check`, 스크립트 종료 코드 0, 스크린샷 S1·S5·S6을 Read로 본다.

- [ ] 7. 사진 → `feat(web): 사진을 긴 변 512px JPEG로 줄여 저장`
  - `photo/fitWithin.ts`, `photo/resizePhoto.ts`, `PhotoPicker.tsx` (설계 결정 7)
  - `photo/fitWithin.test.ts`:
    - `가로가 긴 사진은 가로를 최대 길이에 맞춘다`
    - `세로가 긴 사진은 세로를 최대 길이에 맞춘다`
    - `이미 작은 사진은 키우지 않는다`
    - `정사각형은 두 변 모두 최대 길이다`
    - `줄인 크기는 정수이고 1px보다 작아지지 않는다`
  - 스크립트: S4·S5의 사진 확인, S7, S13
  - 확인: `check` 통과, 스크립트 0, S5 스크린샷에서 사진이 보이고 흐릿한지 본다 (설계 결정 7의 768px 판단을 진행 로그에 적는다).

- [ ] 8. 의료정보 → `feat(web): 의료정보 탭과 의료정보 입력 추가`
  - `ui/medicalForm.ts`(순수), `MedicalInfoTab.tsx`, `MedicalForm.tsx`
  - `ui/medicalForm.test.ts`:
    - `빈 줄은 저장하지 않는다`
    - `항목의 앞뒤 공백을 지운다`
    - `항목은 50자까지다`
    - `묶음마다 20개까지다`
    - `복용법만 있고 약 이름이 없으면 오류다`
    - `약 이름만 있고 복용법이 없어도 저장된다`
    - `의료정보를 수정해도 프로필 기본정보와 사진은 그대로다`
    - `저장한 의료정보를 다시 폼으로 열면 같은 줄이 보인다`
  - 스크립트: S5(의료정보 탭의 빈 표와 [+ 의료정보 추가하기]), S8, S9(의료정보 포함), S11, S14(S8 화면)
  - 확인: `check` 통과, 스크립트 0, S8 스크린샷을 "디자인 참고"와 비교한다.

- [ ] 9. 경계 테스트 (`tester`) → `test(web): 프로필 형식·나이·저장소·폼 경계 테스트 추가`
  - 메인이 `tester`에게 **이 계획 파일 경로와 대상만** 넘긴다. 구현 설명은 넘기지 않는다.
    - 대상: `web/src/domain/`, `web/src/data/localStoragePetProfileRepository.ts`, `web/src/photo/fitWithin.ts`, `web/src/ui/profileScreenReducer.ts`, `web/src/ui/profileForm.ts`, `web/src/ui/medicalForm.ts`
    - 대상 아님: React 컴포넌트, `resizePhoto.ts`, `container.ts`, `main.tsx` (브라우저 필요. 화면 스크립트가 본다)
    - 단계 5를 건너뛰었으면(`tester.md`에 웹 규칙 없음) 테스트 위치(`web/src/**/*.test.ts`)·실행 명령(`npm --prefix web test`)·"새 의존성 금지"만 함께 알린다. 이것은 구현 설명이 아니다.
  - 기대 동작 (계약 기준): `docs/data/pet-profile.md` 전체(필드 제한, 빈 배열의 뜻, 버전 규칙, 나이 규칙), 설계 결정 5의 저장소 계약(실패해도 이전 값 유지, 이유 구분), 설계 결정 6의 입력 규칙, 설계 결정 7의 크기 규칙.
  - 자주 빠지는 경계: 윤년 2월 29일생, 12월→1월 넘어가는 개월 계산, 정확히 30년 전 날짜, 20자·50자 경계(이모지·한글 조합 글자 포함), 20개 경계, `null`과 빈 문자열.
  - tester가 찾은 버그는 메인이 `fix(web): ...` 커밋으로 따로 고친다.
  - 확인: `npm --prefix web run check` 통과. 통과·실패 수를 진행 로그에 적는다.

- [ ] 10. 전체 확인. 커밋은 없다. 고친 게 있으면 `fix(web): ...`로 낸다.
  - `npm --prefix web ci` (깨끗한 설치) → `npm --prefix web run check`
  - (4a·4b) `node .claude/hooks/build-gate.mjs pipeline` → "검증 파이프라인 통과."
  - 화면 스크립트 S1~S16 전부, 두 폭. 스크린샷을 모두 Read로 본다.
  - 완료 기준 4·5·6의 Grep과 `git diff`
  - 확인하지 못한 것(Safari, 실제 폰 사진 회전·HEIC 등)은 그대로 적는다.

- [ ] 11. 문서 맞추기
  - 11-1. → `docs: AGENTS.md에 보호자 웹 추가`
    - 프로젝트: "보호자용 웹(`web/`)이 함께 있다. 보호자가 폰 브라우저에서 반려견 의료정보를 등록한다."
    - 기술 스택: "보호자 웹: React + Vite + TypeScript, Vitest. 버전은 `web/package.json`에 정확히 고정. 저장은 지금 브라우저 localStorage, 나중에 Firebase. 자세히: docs/architecture.md '보호자 웹'". Node 요구 메이저.
    - 아키텍처 규칙 한 줄: "웹 화면은 `PetProfileRepository`만 쓴다. localStorage는 `web/src/data/`에서만. 프로필 형식은 `docs/data/pet-profile.md`가 기준이다 (웹·차 앱 공통)."
    - 명령어 표: 웹 검사 `npm --prefix web run check`, 웹 개발 서버 `npm --prefix web run dev`, 웹 화면 확인 (`build` → `preview` → `python web/scripts/verify_screens.py`)
    - 디렉토리 표: `web/`, `docs/data/`
    - AGENTS.md는 지도다. 한두 줄씩만.
  - 11-2. → `docs(architecture): 보호자 웹 구조와 저장·개인정보 원칙 기록`
    - "서비스 기능 스택" 표에 행: "보호자 등록 웹 | React + Vite + TS, 저장은 localStorage 먼저 | 서버 없이 먼저 만들고, 저장소 인터페이스로 Firebase를 끼울 자리를 둔다 | 결정 (서버 연동은 A1 이후)"
    - 새 절 "보호자 웹 (008)": 결정 표(설계 결정 2·5·8·9·10과 버린 대안), 폴더 구조와 의존 방향(설계 결정 1), 화면 상태(설계 결정 6 요약), 저장과 개인정보·Firebase로 옮길 때 할 일·비밀값 방향(설계 결정 11)
    - 문의 A1의 "안 된다면" 칸: "웹은 브라우저 저장만 쓰고, 차 앱으로 옮기는 방법을 다시 정한다"
  - 11-3. → `docs: README에 보호자 웹 실행 방법 추가` (`[사용자 승인 필요]` 4가 4a일 때)
    - "보호자 웹 (선택)" 절: 무엇인지 한 줄, 필요한 Node, `npm --prefix web ci` → `npm --prefix web run dev` → `http://localhost:5173` (PC 브라우저의 기기 모드로 폰 크기), "정보는 그 브라우저에만 저장되고, 차 앱과는 아직 연결되지 않았다."
    - "테스트 환경": 웹을 확인한 환경 (Chromium, 폰 크기 390×844·360×800). Safari·실제 폰은 확인한 경우에만 적는다.
  - 확인: 완료 기준 7

- [ ] 12. 개발 노트 작성 (`docs/notes/`)
  - `docs/notes/xxj15/YYYY-MM-DD-owner-web.md`. 형식은 `docs/notes/README.md` 템플릿.
    - 무엇을 했나 (보호자 관점 1~3줄)
    - 결정: localStorage 먼저 + 인터페이스, 스키마 버전, 형식 문서를 기준으로 둔 이유, 의존성 최소, 화면 확인 방식, 하네스 변경 (승인 결과)
    - 사용한 Pleos SDK / API / 권한: 없음
    - 확인: 테스트 수, 스크립트 결과, 확인하지 못한 것
    - 시연 방법: 영상용 순서 (빈 화면 → 등록 → 사진 → 의료정보 → 새로고침해도 남음). 폰 크기로 보는 방법
    - **기획서 5번 "개발 계획"에 넣을 문장** (기획 담당에게 넘긴다). 예: "보호자는 폰 웹에서 반려견 프로필과 의료정보(질환·알레르기·복용약)를 미리 등록한다. 첫 버전은 브라우저에 저장하며, 서버(Firebase) 연동과 차 앱 연결은 대회 규정 확인(외부 서버 허용 여부) 뒤 진행한다."
    - 남은 일: 차 앱 `PetProfile` 도메인 모델·Fake(예시 JSON 값)와 진료 카드, Firebase 구현(A1 뒤), 있으면 좋음 중 하지 않은 것, 앱 이름 확정 시 `web/src/config.ts`와 Android `strings.xml` 두 곳을 바꾼다
  - 이 계획을 `docs/plans/completed/`로 옮기고 진행 로그를 갱신한다.
  - 커밋:
    - `docs(notes): owner-web`
    - `docs(plans): 008 보호자 웹 완료`
  - 확인: `git status`에 이 작업의 변경이 남지 않았다 (`.agents/`, `.codex/` 제외). 그다음 `/ship`.

### 있으면 좋음 (필수가 끝난 뒤, 단계 12 전에. 11/15에 도움이 되는 순서)

- [ ] A. 시연 영상용 녹화 → `chore(web): 화면 확인 스크립트에 녹화 옵션 추가`
  - `verify_screens.py --video`: Playwright `record_video_dir`로 S1→S4→S5→S8 흐름을 폰 크기로 녹화한다 (`.claude/.cache/web/video/`, 커밋하지 않음). 사진은 팀원이 직접 찍은 강아지 사진을 쓴다 (권리 문제, 출품 유의사항).
- [ ] B. 브라우저 뒤로가기로 편집 닫기 → `feat(web): 뒤로가기로 편집 화면을 닫는다`
  - `history.pushState`/`popstate`만으로. 라우터 라이브러리는 쓰지 않는다. 스크립트 시나리오 하나 더한다.
- [ ] C. 이 브라우저의 정보 지우기 → `feat(web): 이 브라우저에 저장한 정보 지우기`
  - 기본정보 탭 아래 [이 브라우저에서 정보 지우기] + 확인. 저장소 인터페이스에 `clear()`를 더한다 (Firebase에서도 "내 데이터 지우기"가 필요하다). 테스트 2개, 시나리오 하나.
- [ ] D. 실제 폰으로 확인 (사용자 도움 필요)
  - 같은 Wi-Fi에서 `--host`로 열고, 폰 카메라 사진(세로로 찍은 것)과 iPhone 사진으로 회전·HEIC를 본다. 결과를 진행 로그와 노트에 적는다. 문제가 있으면 따로 `fix(web)`.
- [ ] E. Safari 엔진으로 스크립트 돌리기
  - `python -m playwright install webkit`이 필요하다 (다운로드. 사용자에게 먼저 묻는다). 스크립트에 `--browser webkit` 옵션.

## 진행 로그

- 2026-11-05: 계획 작성.
  - 시작 시점: `web/` 없음. build-gate·pre-push·CI는 Android만 검사한다. `tester.md`는 `app/src/test/`와 Gradle만 다룬다. `.gitignore`에 웹 항목 없음.
  - Python Playwright 1.64.1 + Chromium은 2026-11-03에 설치했다 (stack-diagram 노트). 로컬 Node 버전은 아직 확인하지 않았다.
- 2026-11-05: 시작. 시작 커밋 `90ef20b` (dev). 로컬 Node v24.18.0, npm 11.16.0, Python Playwright import 확인.
  - `[사용자 승인 필요]` 답 (사용자, 2026-11-05):
    1. 하네스: **4b** (파이프라인·pre-push·CI만). 커밋 직전·Stop은 그대로. 하네스 확인에서 H3·H4·H9·H10을 뺀다. 설계 결정 10 표의 "커밋 직전", "턴 끝" 행은 하지 않는다.
    2. 권한: **2a**
    3. ESLint: **3a** (넣지 않는다)
    4. README: **4a** (지금 짧게 넣는다)
    5. 화면 기본안 그대로 (필수는 이름, 탭은 의료정보·기본정보, 진료이력 탭·알림 아이콘 숨김, 저장 위치 안내 문구)
    6. 형식 문서 위치 `docs/data/pet-profile.md` 그대로
- 2026-11-05: 단계 0~5 완료 (`3f227f4`..`87325c5`).
  - 고른 버전 (2주 넘은 안정 버전): react·react-dom 19.3.1, vite 8.3.5, @vitejs/plugin-react 6.1.2, typescript 7.0.2, @types/react·@types/react-dom 19.3.0, vitest 5.0.5. Node 요구 `^22.12.0 || ^24.0.0 || >=26.0.0` (vitest 5 기준). CI는 Node 24.
  - Vitest가 `web/` 밖의 `docs/data/pet-profile.example.json`을 import한다 (대안 불필요). `resolveJsonModule`을 켰다.
  - 단위 테스트 34개 통과 (age 12, localDate 5, petProfileJson 7, 저장소 10).
  - 화면 스크립트 S1 두 폭 통과. 헤더 스크린샷 확인.
  - 하네스 시나리오 17개 통과 (임시 저장소): H1, H1b(웹 없을 때 Android만), H2, H3'(precommit은 웹을 안 돌림), H5, H6, H7, H8, H10'(node_modules의 .xml·.properties 무시), H11, H11b, H12a~c, H13(웹 의존성 없음 → 멈춤, 설치 안 함), H14(웹 실패), H14b(Android 실패면 웹은 안 돔). 실제 저장소 `pipeline` 통과.

## 결정 / 배운 점

- **파이프라인은 웹 의존성을 자동 설치하지 않는다** (계획의 설계 결정 10과 다름). 사용자가 "npm install·ci는 계속 묻는다"(2a)를 골랐는데, 파이프라인이 `npm ci`를 하면 그 확인을 건너뛴다. 대신 `npm ls --depth=0`으로 설치 상태를 보고, 없거나 `package.json`과 다르면 멈추고 `npm --prefix web ci`를 하라고 알린다. mtime 비교는 체크아웃 때마다 거짓 경보가 나서 쓰지 않았다.
- **하네스는 refactor 커밋을 따로 두지 않았다.** 4b라 precommit·stop은 그대로여서 바뀐 범위가 작다 (`digestOf`/`changedFiles`로 일반화 + pipeline 분기).
- `App.tsx`는 계획의 폴더 구조대로 `src/ui/App.tsx`에 두었다 (단계 1 커밋 목록의 `src/App.tsx`가 아님).
- 날짜 계산은 `domain/localDate.ts`로 따로 뺐다 (`age.ts`가 쓰고, 폼 검증도 쓴다).
- 형식 문서에 계획에 없던 규칙 둘을 더했다: 글자 수 = 코드 포인트 수, 저장 전 NFC 정규화. 나이 입력은 합쳐서 30세 0개월까지 (30세 11개월은 "30년 전보다 앞이 아님"과 충돌).
