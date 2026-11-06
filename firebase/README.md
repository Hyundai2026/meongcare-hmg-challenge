# firebase/: 로컬 서버(에뮬레이터)와 보안 규칙

제휴 병원 상태와 환자 건(진료 카드)을 저장하는 Firestore와, 차·병원 웹이 쓰는 익명 로그인(Auth)을
**내 PC에서 가짜로 띄운다** (Firebase Local Emulator Suite). 웹으로 치면 로컬 개발 DB 서버다.
계획: [docs/plans/completed/012-fastest-hospital.md](../docs/plans/completed/012-fastest-hospital.md) "설계 결정 6·9"

데이터 형식: [docs/data/hospital-status.md](../docs/data/hospital-status.md), [docs/data/emergency-case.md](../docs/data/emergency-case.md)

## 준비물

- Node 22.12 이상 (web/과 같다)
- Java: Firestore 에뮬레이터가 Java로 돈다. **JDK 21로 동작을 확인했다** (2026-11-06, firebase-tools 15.34.1).
- `firebase-tools`는 전역 설치하지 않는다. 이 폴더의 `package.json`에 버전을 고정했다.

## 명령 (저장소 루트에서)

| 목적 | 명령 |
|---|---|
| 처음 한 번 | `npm --prefix firebase ci` |
| 에뮬레이터 켜기 (터미널 하나를 계속 쓴다) | `npm --prefix firebase run emulators` |
| 시연 병원 5곳 넣기 | `npm --prefix firebase run seed` (`-- --reset`이면 먼저 모두 지운다) |
| 보안 규칙 점검 R1~R13 | `npm --prefix firebase run check-rules` |
| **시연 전 점검** PF1~PF6 (차·병원 웹이 같은 서버에 붙을 준비) | `npm --prefix firebase run preflight` |
| 에뮬레이터 화면 | http://localhost:4000 (Emulator UI) |

- 에뮬레이터를 끄면 데이터가 사라진다. 다시 켜면 `seed`를 다시 한다.
- 처음 켤 때 Firestore 에뮬레이터 파일을 내려받는다 (인터넷 필요).
- 포트: Auth 9099, Firestore 8080, UI 4000. Auth·Firestore는 **`127.0.0.1`(IPv4)에 고정**했다 (`firebase.json`, 계획 013). 차 앱의 `adb reverse`가 IPv4로 붙기 때문이다.
- 다른 주소로 띄웠으면 스크립트에 환경 변수로 알린다:
  `FIRESTORE_EMULATOR_HOST=[::1]:8080 FIREBASE_AUTH_EMULATOR_HOST=[::1]:9099 npm --prefix firebase run seed`

## 시연 전 점검 (`preflight`, 계획 013)

012 시연 준비 때 에뮬레이터가 IPv4(`127.0.0.1`)와 IPv6(`[::1]`)에 하나씩 떠 있어서, 차 앱(`adb reverse` → 127.0.0.1)과 병원 웹(`localhost` → ::1)이 **서로 다른 서버**에 붙었다. 차가 보낸 카드가 병원 웹에 뜨지 않았다. 시연 전에 한 번 돌린다.

| # | 보는 것 | 실패하면 |
|---|---|---|
| PF1·PF2 | `127.0.0.1:8080`(Firestore)·`9099`(Auth)가 2초 안에 응답 | `npm --prefix firebase run emulators` |
| PF3 | 익명 계정으로 시연 병원 5곳 이상을 읽는다 (차와 같은 방법) | `npm --prefix firebase run seed -- --reset` |
| PF4 | `[::1]:8080`·`9099`에 **다른** 에뮬레이터가 없다 (응답이 오면 표식 문서·계정으로 같은 서버인지 본다) | 그 에뮬레이터 터미널을 끈다. `netstat -ano \| findstr :8080`로 pid를 찾는다 |
| PF5 | `adb reverse --list`에 `tcp:8080`·`tcp:9099` | `adb reverse tcp:8080 tcp:8080`, `adb reverse tcp:9099 tcp:9099` (기기가 여럿이면 `ANDROID_SERIAL`) |
| PF6 | 응급 접수를 받고 대기 정보가 **30분 안**인 병원이 있다. 시드하거나 알린 지 30분이 지나면 차 목록에 1위가 없다 ("대기 정보 지남") | 시연 직전에 병원 웹 [병원 상태 알리기], 또는 `seed -- --reset` (환자 건도 지워진다) |

- 하나라도 실패하면 종료 코드 1이다.
- 이 스크립트는 웹 환경 파일과 차 앱 설정 파일을 **읽지 않는다**. 대신 눈으로 본다:
  - 병원 웹 화면 위 `서버:` 줄이 `에뮬레이터 127.0.0.1:8080 · … · 실시간 연결됨`인지. `localhost`·`::1`이면 빨간 경고가 뜬다.
  - 차 앱 debug 빌드의 `FIREBASE_EMULATOR_HOST`가 `127.0.0.1`인지.

## 병원 웹 링크 (시연 코드 포함)

시드가 끝나면 병원별 링크를 출력한다. 코드는 [seed/join-codes.emulator.json](seed/join-codes.emulator.json)에 있다 (에뮬레이터 전용 가짜 값이라 커밋한다).

| 병원 | 링크 (병원 웹 개발 서버 기준) |
|---|---|
| 시연용 바른 동물병원 | http://localhost:5173/hospital/?h=demo-fast&code=fast-1234 |
| 시연용 동물병원 | http://localhost:5173/hospital/?h=demo-main&code=main-1234 |
| 시연용 가까운 동물병원 | http://localhost:5173/hospital/?h=demo-near&code=near-1234 |

병원 웹을 에뮬레이터에 연결하는 방법은 [web/README.md](../web/README.md)를 본다.

## 보안 규칙 요약 ([firestore.rules](firestore.rules))

- 차와 병원 웹 모두 **익명 로그인**한다. 로그인 안 한 요청은 아무것도 못 읽는다.
- 병원 문서는 시드만 만든다. 병원 웹은 **코드가 맞을 때만** 그 병원 직원으로 등록되고(`hospitals/{id}/staff/{uid}`), 직원만 `status`를 바꾼다.
- 환자 건은 차가 만든다. `createdBy`가 자기 uid여야 하고, **사진(`photoDataUrl`)이 있으면 거부**한다. 읽기는 보낸 차와 그 병원 직원만.
- 병원 [카드 확인](`acknowledgedAt`)은 직원만, **서버 시각으로, 확인 전인 건에 한 번만** 쓴다. 차는 쓰지 못하고 읽기만 한다 (계획 013, R9~R13).
- `hospitalSecrets`(병원 코드)는 아무도 읽지 못한다.

## 진짜 Firebase 프로젝트로 바꿀 때 (계획 012 있으면 좋음 B)

코드는 그대로 두고 설정만 바꾼다. 아직 하지 않았다.

1. `npx --prefix firebase firebase login` → `firebase use --add` (진짜 프로젝트 ID)
2. 콘솔에서 Authentication → 익명 로그인 켜기
3. `firebase deploy --only firestore:rules`
4. 병원 문서·코드 넣기 (진짜 코드는 `seed/join-codes.local.json`, 커밋하지 않는다)
5. 웹 `web/.env.local`, 차 `secrets.properties`에 진짜 값
