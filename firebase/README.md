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
| 보안 규칙 점검 R1~R24 | `npm --prefix firebase run check-rules` |
| **시연 전 점검** PF1~PF6 (차·병원 웹이 같은 서버에 붙을 준비) | `npm --prefix firebase run preflight` (고칠 수 있는 것까지 고치려면 `-- --fix`) |
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
| PF6 | 응급 접수를 받고 대기 정보가 **30분 안**인 병원이 있고, 모두 **10분 이상** 남았다. 시드하거나 알린 지 30분이 지나면 차 목록에 1위가 없다 ("대기 정보 지남"), 시연 도중에 지나면 순위가 바뀐다 | `preflight -- --fix` (시드를 `--reset` 없이 다시: 대기 정보만 새 시각, 환자 건은 그대로. 병원 웹에서 바꾼 대기 분도 시드 값으로 돌아간다) |

- 하나라도 실패하면 종료 코드 1이다.
- **`-- --fix`**: PF5면 `adb reverse`를 걸고, PF3·PF6이면 시드를 `--reset` 없이 다시 넣은 뒤 한 번 더 점검한다. 에뮬레이터를 켜거나 끄는 일(PF1·PF2·PF4)은 하지 않고 안내만 한다.
- 이 스크립트는 웹 환경 파일과 차 앱 설정 파일을 **읽지 않는다**. 대신 눈으로 본다:
  - 병원 웹 화면 위 `서버:` 줄이 `에뮬레이터 127.0.0.1:8080 · … · 실시간 연결됨`인지. `localhost`·`::1`이면 빨간 경고가 뜬다.
  - 차 앱 debug 빌드의 `FIREBASE_EMULATOR_HOST`가 `127.0.0.1`인지.

## 근처 동물병원 함수 (`functions/`, 계획 015)

차 앱이 지금 위치 기준 5km 안의 동물병원을 카카오 로컬 API로 찾을 때 부르는 callable 함수 `nearbyVets`다. **배포하지 않고 PC 에뮬레이터에서만** 돌린다 (Blaze 불필요). 형식과 확인 사항: [docs/data/nearby-vets.md](../docs/data/nearby-vets.md)

1. 카카오 Developers에서 앱을 만들고 [카카오맵] > [사용 설정]을 ON, REST API 키를 받는다.
2. `firebase/functions/.secret.local.example`을 `.secret.local`로 복사하고 `KAKAO_REST_API_KEY=` 뒤에 키를 넣는다. **`.secret.local`은 커밋하지 않는다** (gitignore). 키는 앱·웹·`secrets.properties`에 넣지 않는다.
3. `npm --prefix firebase/functions ci` (처음 한 번) → `npm --prefix firebase run emulators:nearby`.
4. 확인: `npm --prefix firebase run check-nearby` (N1 로그인 없으면 거절, N2 Pleos 에뮬레이터 시연 위치(시흥동)로 15곳 안팎·모두 5.5km 안, N3 잘못된 위도 거절, N4 5초 안 다시 부르면 거절, N5 형식). 병원 이름·전화는 출력하지 않는다.
5. 차 앱: debug 빌드, `secrets.properties`에 `FIREBASE_PROJECT_ID=demo-meongcare`·`FIREBASE_EMULATOR_HOST=127.0.0.1`, debug 패널에서 **시연** 모드, `adb reverse tcp:5001 tcp:5001` (8080·9099와 함께).

- 함수 테스트: `npm --prefix firebase/functions test` (`node --test`, 손으로 만든 가짜 카카오 응답).
- 함수는 좌표를 약 100m(소수 셋째 자리)로 반올림해 카카오에 보내고, 결과를 저장하지 않는다. 로그에는 결과 종류·개수·ms·카카오 HTTP 상태만.
- 남용 방지: 로그인(익명) 필수, 같은 계정 5초에 한 번·1시간 30번 (프로세스 메모리). 배포할 때 App Check·진짜 속도 제한을 다시 정한다.

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
- 보호자 프로필과 차 연결 (계획 014, 형식 [docs/data/owner-link.md](../docs/data/owner-link.md), R14~R24):
  - 보호자는 **동의 문서**(`owners/{uid}`, 서버 시각)가 있어야 자기 프로필(`petProfiles/{uid}`)을 쓴다. 프로필의 `photoDataUrl`은 늘 null이고, 사진은 보호자만 읽는 `private/photo`에 둔다.
  - 연결 코드(`linkCodes/{code}`)는 차가 자기 uid·서버 시각으로 만든다. 보호자는 **만료(10분) 전, 안 쓴 코드를 한 번만** claim하고, 같은 한 번에 쓰기로 허가(`readers/{차 uid}`)를 만든다. 코드·프로필 목록 조회는 막는다.
  - 차는 허가가 있을 때 **프로필 문서 하나만** 읽는다 (사진·동의·허가 목록은 못 읽고, 아무것도 못 쓴다). 허가를 지우면(차·보호자 모두 가능) 읽기는 403, 허가는 있는데 프로필이 없으면 404다.
  - 규칙으로 막지 못하는 것: 코드 마구 대입(속도 제한은 Functions·App Check가 필요), 남은 코드 문서 자동 정리.

## 진짜 Firebase 프로젝트로 바꿀 때 (계획 012 있으면 좋음 B)

코드는 그대로 두고 설정만 바꾼다. 아직 하지 않았다.

1. `npx --prefix firebase firebase login` → `firebase use --add` (진짜 프로젝트 ID)
2. 콘솔에서 Authentication → 익명 로그인 켜기
3. `firebase deploy --only firestore:rules`
4. 병원 문서·코드 넣기 (진짜 코드는 `seed/join-codes.local.json`, 커밋하지 않는다)
5. 웹 `web/.env.local`, 차 `secrets.properties`에 진짜 값
