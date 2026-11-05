# 보호자 웹

보호자가 폰 브라우저에서 반려견 프로필과 의료정보(주요 질환, 알레르기, 복용 약)를 미리 등록하는 웹이다. 차 앱의 진료 카드가 이 정보를 쓸 예정이다.

- 지금은 **서버 없이 그 브라우저(localStorage)에만 저장**한다. 차 앱과는 아직 연결되지 않았다.
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
