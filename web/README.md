# 웹: 보호자 웹(/)과 병원 웹(/hospital/)

두 페이지가 한 프로젝트에 있다 (Vite 다중 페이지). 라우터는 없다.

- **보호자 웹** `/`: 아래 설명
- **병원 웹** `/hospital/` (시연용, 012): 아래 "병원 웹" 절

## 보호자 웹

보호자가 폰 브라우저에서 반려견 프로필과 의료정보(주요 질환, 알레르기, 복용 약)를 미리 등록하는 웹이다. 서버 저장에 동의하면 차와 연결해 차 앱의 진료 카드가 이 정보를 쓴다 (014).

| 모드 | 언제 | 저장 | 차와 연결 |
|---|---|---|---|
| 서버 없음 | Firebase 설정 없이 빌드 (기본) | 그 브라우저(localStorage)에만 | 없음 |
| 동의 전 | 설정 있음 + 이 브라우저가 동의한 적 없음 | 브라우저 + [서버에 저장하고 차와 연결] 카드 | 동의 뒤 |
| 동의 뒤 | 동의한 익명 계정을 이 브라우저가 기억함 | Firestore (사진은 보호자만 읽는 문서) | [차와 연결]에 차 화면의 6자리 코드 |

- 연결 형식과 보안 규칙: [docs/data/owner-link.md](../docs/data/owner-link.md)
- 보호자는 익명 계정이다. 브라우저 사이트 데이터를 지우면 서버의 정보를 이 브라우저에서 고칠 수 없다 (동의 화면에 알린다).
- `?code=K7P3QX`로 열면 연결 코드 입력칸이 미리 채워진다 (나중 QR용).
- 프로필 형식: [docs/data/pet-profile.md](../docs/data/pet-profile.md) (웹·차 앱 공통)
- 구조와 결정: [docs/architecture.md](../docs/architecture.md) "보호자 웹"

## 필요한 것

- Node.js 22.12 이상 (22, 24 또는 26 이상). `node -v`로 확인한다.

## 명령 (저장소 루트에서)

| 목적 | 명령 |
|---|---|
| 처음 한 번, 의존성이 바뀐 뒤 | `npm --prefix web ci` |
| 개발 서버 (저장하면 바로 바뀜) | `npm --prefix web run dev` → http://localhost:5173 |
| 빌드 (타입 검사 포함) | `npm --prefix web run build` |

PC 브라우저에서 폰 크기로 보려면 개발자 도구(F12)의 기기 모드(Ctrl+Shift+M)를 켠다.

## 병원 웹 (/hospital/, 012)

제휴 병원 직원이 예상 대기 시간·혼잡도·응급 접수를 알리고, 차에서 보낸 진료 카드와 도착 예정을 실시간으로 보는 **시연용** 화면이다.
구조: [docs/architecture.md](../docs/architecture.md) "병원 웹". 형식: [docs/data/hospital-status.md](../docs/data/hospital-status.md), [docs/data/emergency-case.md](../docs/data/emergency-case.md)

| 모드 | 언제 | 어떻게 |
|---|---|---|
| 메모리 | `web/.env.local`이 없을 때 (기본) | 서버 없이 이 브라우저에서만. 시연 병원 5곳 + 병원마다 예시 환자 1건. 차 앱의 카드는 들어오지 않는다. 새로 고치면 처음으로 돌아간다 |
| Firebase | `web/.env.local`에 `VITE_FIREBASE_*`가 있을 때 | 익명 로그인 → 링크의 병원 코드로 직원 등록 → 상태와 환자 건을 실시간으로 |

### 로컬 에뮬레이터에 붙여 보기 (차 앱과 연결)

1. 터미널 1: `npm --prefix firebase run emulators` (켜 둔다. [firebase/README.md](../firebase/README.md))
2. 터미널 2: `npm --prefix firebase run seed -- --reset` → 병원별 링크가 나온다
3. `web/.env.example`을 `web/.env.local`로 복사한다 (Windows: `copy web.env.example web.env.local`). 처음 세 줄이 에뮬레이터 값이다.
   - 에뮬레이터 호스트는 `127.0.0.1`이다. **`localhost`로 써도 병원 웹이 `127.0.0.1`로 바꿔 붙는다** (`emulatorHostFor`). `localhost`는 브라우저가 IPv6(`::1`)로 먼저 붙을 수 있어서, 차 앱(`adb reverse` → IPv4)과 다른 에뮬레이터를 볼 수 있었다 (2026-11-06 실제로 생겼다). 화면 위 "서버:" 줄에 붙은 주소가 보이고, `::1`이면 빨간 경고가 뜬다.
4. `npm --prefix web run dev` → http://localhost:5173/hospital/?h=demo-fast&code=fast-1234
5. 차 앱 debug 빌드를 시연 모드로 두고 `secrets.properties`에 에뮬레이터 값을 넣는다 (`secrets.properties.example`). [출발]하면 카드가 이 화면에 "새 환자"로 뜬다. [카드 확인]을 누르면 차 화면에 "병원이 카드를 확인했어요"가 뜬다 (계획 013).
6. 시연 직전: `npm --prefix firebase run preflight -- --fix` (PF1~PF6). 시드한 지 30분이 지나면 대기 정보가 "지남"이 되어 차 목록에 1위가 없다. `--fix`가 대기 정보를 새로 넣고(환자 건은 그대로) adb reverse도 건다.

- 코드가 없거나 틀리면 "병원용 링크(코드 포함)로 열어 주세요"가 나온다. 한 번 등록된 브라우저는 그 병원을 코드 없이 다시 열 수 있다.
- `VITE_`로 시작하는 값은 빌드 결과에 그대로 들어가 공개된다. `.env.local`은 커밋하지 않는다.
- **`.env.local`이 있으면 `build`·`preview`도 Firebase 모드가 된다** (보호자 웹도 014부터 서버 모드가 된다). 아래 화면 확인 스크립트(보호자 웹)는 **서버 설정 없이 빌드한 것**을 기준으로 하므로, 돌리기 전에 `.env.local`을 잠시 치운다. 병원 웹을 메모리 모드로 볼 때도 같다.
- 보호자 웹도 같은 설정으로 에뮬레이터에 붙는다: `http://127.0.0.1:5173/` → 동의 → [차와 연결]. 차 앱과 연결하는 순서는 루트 [README.md](../README.md) "보호자 웹과 차 연결".

## 폰으로 열어 보기

1. PC와 폰을 같은 Wi-Fi에 연결한다.
2. `npm --prefix web run dev -- --host`
3. 터미널에 나오는 `Network: http://<PC IP>:5173`을 폰 브라우저로 연다.

학교·회사 망에서는 기기끼리 연결이 막혀 있을 수 있다.

## 화면 확인 (스크린샷)

Python Playwright가 필요하다 (`pip install playwright && python -m playwright install chromium`). Windows에서는 `python3` 대신 `python`을 쓴다.

1. `npm --prefix web run build`
2. 다른 터미널에서 `npm --prefix web run preview -- --port 4173 --strictPort --host 127.0.0.1`
3. `python web/scripts/verify_screens.py --base-url http://127.0.0.1:4173 --out .claude/.cache/web`
4. `.claude/.cache/web/*.png`를 열어 본다. 날짜는 2026-11-05로 고정해서 찍는다.

- 시나리오 일부만: `--only S01 S08`. 모든 시나리오를 390·360 두 폭으로: `--all-widths` (dev에 반영하기 전 전체 확인)
- 실패하면 어떤 시나리오의 무엇이 틀렸는지 출력하고 종료 코드 1이다. 콘솔 에러나 다른 주소로 가는 요청이 있어도 실패다.
- Windows에서 미리보기 서버를 끈 뒤에도 vite 프로세스가 남으면 `npm --prefix web ci`가 `EPERM`으로 실패한다. 남은 프로세스를 끄고 다시 한다.
