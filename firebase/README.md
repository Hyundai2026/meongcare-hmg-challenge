# firebase/: 로컬 서버(에뮬레이터)와 보안 규칙

제휴 병원 상태와 환자 건(진료 카드)을 저장하는 Firestore와, 차·병원 웹이 쓰는 익명 로그인(Auth)을
**내 PC에서 가짜로 띄운다** (Firebase Local Emulator Suite). 웹으로 치면 로컬 개발 DB 서버다.
계획: [docs/plans/active/012-fastest-hospital.md](../docs/plans/active/012-fastest-hospital.md) "설계 결정 6·9"

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
| 보안 규칙 점검 R1~R8 | `npm --prefix firebase run check-rules` |
| 에뮬레이터 화면 | http://localhost:4000 (Emulator UI) |

- 에뮬레이터를 끄면 데이터가 사라진다. 다시 켜면 `seed`를 다시 한다.
- 처음 켤 때 Firestore 에뮬레이터 파일을 내려받는다 (인터넷 필요).
- 포트: Auth 9099, Firestore 8080, UI 4000. 다른 주소로 띄웠으면 스크립트에 환경 변수로 알린다:
  `FIRESTORE_EMULATOR_HOST=[::1]:8080 FIREBASE_AUTH_EMULATOR_HOST=[::1]:9099 npm --prefix firebase run seed`

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
- `hospitalSecrets`(병원 코드)는 아무도 읽지 못한다.

## 진짜 Firebase 프로젝트로 바꿀 때 (계획 012 있으면 좋음 B)

코드는 그대로 두고 설정만 바꾼다. 아직 하지 않았다.

1. `npx --prefix firebase firebase login` → `firebase use --add` (진짜 프로젝트 ID)
2. 콘솔에서 Authentication → 익명 로그인 켜기
3. `firebase deploy --only firestore:rules`
4. 병원 문서·코드 넣기 (진짜 코드는 `seed/join-codes.local.json`, 커밋하지 않는다)
5. 웹 `web/.env.local`, 차 `secrets.properties`에 진짜 값
