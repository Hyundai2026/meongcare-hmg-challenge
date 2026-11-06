# 014 보호자 웹 프로필을 차 앱에 연결 (owner-link)

- 목표: 보호자가 폰 웹에서 서버(Firebase)에 저장한 반려견 프로필을, 차 앱이 정차 중 연결 코드 한 번으로 받아 진료 카드(와 병원 전송)에 쓴다.
- 완료 기준 (어떻게 검증하나). `$BASE` = `c8361f6` (이 브랜치를 딴 dev. rebase 뒤에는 `origin/dev`).
  1. `node .claude/hooks/build-gate.mjs pipeline`이 "검증 파이프라인 통과."를 출력한다 (최신 dev 위로 rebase한 뒤).
  2. 서버 설정이 **없는** 상태(`secrets.properties`에 `FIREBASE_*` 없음, `web/.env.local` 없음)에서:
     - `.\gradlew.bat assembleDebug assembleRelease` 성공.
     - `npm --prefix web run check` 성공, `python web/scripts/verify_screens.py --base-url http://127.0.0.1:4173 --out .claude/.cache/web --all-widths` 통과 (보호자 웹 기존 시나리오. 안내 문구를 바꾸면 스크립트의 기대 문구만 같이 바꾼다).
     - 보호자 페이지 번들(`web/dist/assets/main-*.js`)에 Firebase가 들어가지 않는다 (U8 추천안: 설정이 있을 때만 동적 import).
  3. 단위 테스트 (`.\gradlew.bat testDebugUnitTest`, `npm --prefix web test`):
     - 기존 테스트가 모두 그대로 통과한다. 개수는 단계 0에서 센 값이다 (013 끝 Android 838 / 웹 368, 그 뒤 `fix/demo-autofix`에서 웹이 늘었다).
     - `git diff $BASE --diff-filter=MD --name-only -- app/src/test ":(glob)web/src/**/*.test.ts"`의 출력이 비어 있다 (기존 테스트 파일을 고치거나 지우지 않는다).
     - 새 테스트가 통과한다: Android `LinkCodeTest`, `PetPlanTest`, `FakeOwnerLinkTest`, `LinkedPetProfileSourceTest`, `LinkViewModelTest`, `LinkTextsTest`, `ParkedContentLinkTest`, `FirestorePetProfileJsonTest`, `FirestorePetProfileSourceTest`, `FirestoreOwnerLinkTest`, (필수 B) `LinkStoreCodecTest`, `AnonymousAuthRestoreTest` / 웹 `linkCode.test.ts`, `ownerProfileDoc.test.ts`, `ownerNotice.test.ts`, `migration.test.ts`, `linkScreenReducer.test.ts`, 그리고 tester가 더한 것.
  4. 보안 규칙: 사용자 터미널의 Firebase 에뮬레이터(127.0.0.1)에서 `npm --prefix firebase run check-rules`가 R1~R24를 모두 PASS로 출력한다.
  5. 바뀌지 않아야 하는 것 (`git diff $BASE -- <경로>`가 비어 있다):
     - `di/SourcePlan.kt`, `di/NaviPlan.kt`, `di/LocationPlan.kt`, `di/HospitalPlan.kt`, `di/DataMode.kt`, `di/TripTiming.kt`
     - `domain/guidance/`, `domain/hospital/`, `domain/trip/`, `data/guidance/`, `domain/pet/PetProfile.kt`, `data/pet/FakePetProfileSource.kt`
     - `ui/home/DrivingScreen.kt`, `ui/home/HomeContent.kt`, `ui/guidance/`
     - 웹 `web/src/domain/hospital/`, `web/src/ui/hospital/`, `web/src/data/*HospitalDesk.ts`
  6. 정적 검사 (Grep):
     - Android: `ui/` 아래 `import com.meongcare.app.data`·`import com.meongcare.app.di` 0건. `import java.net`은 `data/server/UrlConnectionHttpTransport.kt`에만, `import org.json`은 `data/server/` 아래에만. (필수 B) `javax.crypto`·`"AndroidKeyStore"`는 `data/link/KeystoreLinkStore.kt`에만.
     - debug 병합 매니페스트: `android:name="pleos.car.permission`이 정확히 2건(`NAVI_ROUTE`, `NAVI_ROUTE_SEARCH`), `android.permission.INTERNET` 1건. 새 권한 0.
     - 웹: `from 'firebase`·`import('firebase`는 `web/src/data/firestore*.ts`에만. `web/src`(테스트 제외)에 `console.log` 0건. localStorage는 `web/src/data/`·`web/src/container.ts`에만.
  7. 끝까지 확인: 아래 "확인 절차"의 L1~L12 (필수 A), L13~L14 (필수 B), P1~P3, W1~W5.
  8. 주행 중 안전 (L4): 연결 코드 화면에서 기어 D → 주행 화면 (debug 패널 영역을 빼면 `EditText` 0, 클릭 노드 0, 스크롤 노드 0), 그동안 연결 확인 요청 0회. P → 같은 코드 화면(만료 전) 또는 "코드가 만료됐어요".
  9. 로그 (L12): 우리 앱 pid 로그에 연결 코드·uid·토큰·반려견 이름(보리·콩이)·의료정보 글자 0건. 웹 `console.log` 0건.
  10. 회귀 (P2): release APK(서버 설정 없음)는 지금처럼 "보리"로 끝까지 돌고 [보호자 웹과 연결] 버튼이 보이지 않는다.
  11. 문서: `docs/data/owner-link.md`(+ 예시 JSON), `docs/data/pet-profile.md`("저장 위치"), `docs/architecture.md`, `AGENTS.md`, `README.md`, `web/README.md`, `firebase/README.md`가 갱신돼 있다.
- 브랜치: `feat/owner-link` (dev `c8361f6`에서 땄다)
- 관련 문서:
  - AGENTS.md: 규칙 1·2·3·4·6·7·8, 아키텍처 규칙 (래퍼마다 Fake, 차 서버 통신은 `data/server/`만, 보호자 웹 화면은 `PetProfileRepository`만, 프로필 형식은 `docs/data/pet-profile.md`)
  - docs/architecture.md: "서비스 기능 스택"(의료 프로필 저장 행: Room + SQLCipher 결정), "데이터 모드", "보호자 웹" 특히 "저장과 개인정보"·"Firebase로 옮길 때 할 일", "병원 웹"
  - docs/data/pet-profile.md (읽는 쪽 규칙, Kotlin 대응), docs/data/emergency-case.md (`card.pet`은 사진 제외)
  - docs/pleos/ux-guidelines.md: "지역별 기능 허용 범위"(**QR 로그인은 주행 중 모든 지역 ⛔, `UX_RESTRICTIONS_NO_SETUP`**), "앱 심사·보안·개인정보 정책"(동의, 기기 저장 민감정보 암호화, 토큰은 URL 밖, 웹 페이지 연결 앱의 도메인 증빙), "생명주기와 백그라운드"(폴링 최소화)
  - docs/plans/completed/012-fastest-hospital.md (REST 층, 보안 규칙 R1~R8, U5: 11/15 APK에 Firebase 설정 안 넣기 추천), 013-smooth-trip.md (기존 테스트를 지키는 자리, R9~R13, 토큰 갱신)
  - docs/plans/completed/008-owner-web.md, docs/notes/xxj15/2026-11-05-owner-web.md ("차 앱 연결(정차 중 QR 페어링 안)")
  - 코드: 웹 `web/src/{container.ts,main.tsx}`, `web/src/domain/petProfile*.ts`, `web/src/data/localStoragePetProfileRepository.ts`, `web/src/data/firestoreHospitalDesk.ts`(Firestore 선례), `web/src/hospital/container.ts`(`emulatorHostFor`), `web/src/ui/{App,StorageNotice,usePetProfile,profileScreenReducer,messages}.ts(x)` / 차 `domain/pet/`, `data/pet/`, `data/server/{ServerConfig,AnonymousAuth,FirestoreJson,FirestoreHospitalBoardSource}.kt`, `di/AppContainer.kt`, `ui/home/{ParkedContent,ParkedScreen,HomeScreen}.kt`, `ui/card/MedicalCardViewModel.kt`, `MainActivity.kt` / 서버 `firebase/firestore.rules`, `firebase/scripts/{check-rules,seed,emulator}.mjs`

## 범위

- 왜 지금: 기획서(다른 팀원 작성 중)가 "집에서 폰으로 미리 등록 → 차에서는 글자 입력 없이 카드"를 약속한다. 출품 유의사항(제출 내용이 사실과 다르면 선정 취소) 때문에 시연에서 실제로 보여야 한다. 지금 차 앱은 메모리 Fake "보리"만 쓴다.
- **필수 A — 11/15 시연 영상 최소 범위** (목표 11/09까지, 단계 0~9):
  - 보호자 웹: 서버 저장 동의 → (있으면) 브라우저 정보 옮기기 → 서버에 프로필 저장·수정 → [차와 연결]에서 코드 입력.
  - 차: 정차 중 처음 화면 [보호자 웹과 연결] → 큰 연결 코드 → 연결되면 받은 반려견으로 진료 카드 → (이미 있는) 병원 전송에도 그 반려견. 차 [연결 끊기].
  - 보안 규칙 R14~R24.
  - 연결은 **앱 프로세스 동안만** 유지한다 (U5 S1). 앱을 다시 켜면 다시 연결한다.
  - QR 없음, 코드만 (U4).
  - → **체크포인트 A**: 보호자 웹(폰 크기) + 차(Pleos 에뮬레이터) + 병원 웹을 나란히 찍을 수 있다.
- **필수 B — PT(12/03) 전** (단계 10~11, 목표 11/13. 늦으면 11/15 뒤):
  - 차를 다시 켜도 연결 유지 (익명 계정 자격을 Android Keystore로 암호화 저장, U5 S2).
  - 보호자 웹: 연결된 차 목록과 연결 끊기, 내 정보 지우기.
- **있으면 좋음** (A~F, 아래 "단계"): QR, 마지막 프로필 암호화 캐시(오프라인), 실제 폰 LAN 시연, 보호자 계정 업그레이드, 차 카드에 사진, 진짜 Firebase 프로젝트 + Hosting.
- **B가 11/15 전에 안 끝나면**: 9(끝까지 확인) 뒤 12(tester) → 13(문서) → 14(노트)를 먼저 하고 `/ship`한다. B는 노트 "남은 일"로 넘기고 새 계획(015)을 쓴다.
- **시간이 모자라면 이 순서로 자른다** (위가 먼저):
  1. 있으면 좋음 전부
  2. 단계 11 (웹 연결 관리·내 정보 지우기) → PT 전
  3. 단계 10 (연결 유지) → PT 전
  4. 6-2의 "브라우저 정보 옮기기" → 동의 화면만 남기고, 서버에는 새로 등록한다 (브라우저 정보는 그대로 둔다)
  5. 차 처음 화면이 보이는 동안 60초마다 다시 읽기 → 연결 직후·[진료 카드 만들기] 때만 읽는다
  - 자르지 않는 것: 1(형식), 2~3(계약·Fake·UI), 4(보안 규칙), 5, 6-1, 6-2의 동의 화면, 6-3, 7~8(차 서버 연결), 9(확인), 12(tester), 14(노트).
- **이번 범위 밖**:
  - 진짜 Firebase 프로젝트 전환과 보호자·병원 웹 호스팅 (012 있으면 좋음 B·D. 여기서는 있으면 좋음 F로 이어 둔다)
  - 반려견 여러 마리 (pet-profile v2의 `id`)
  - 차 화면에서 보호자 정보 고치기 (차에서는 글자 입력 없음, 규칙 3)
  - 진짜 개인정보처리방침 URL (Pleos 앱 마켓 제출 때. 대회 제출에는 필요 없다)
  - Firebase Functions·Blaze (자동 정리, 코드 무차별 대입 막기)

## 설계 (planner 제안과 근거)

코드 블록은 기본안이다. 이름과 세부는 구현하면서 바꿔도 된다. 바꾸면 이 블록을 고치고 "결정 / 배운 점"에 이유를 남긴다.

### 1. 기존 테스트를 지키는 자리 (013 설계 결정 1과 같은 원칙)

| 하고 싶은 것 | 이 계획 | 이유 |
|---|---|---|
| 프로필이 샘플인지 표시 | `PetProfileLoad.Loaded`는 그대로. `AppContainer.isPetProfileSample: Boolean` → **`petProfileIsSample: StateFlow<Boolean>`**를 더하고, `MainActivity`가 모아서 지금 Boolean 인자로 넘긴다 | `PetProfileLoad.Loaded(…)`·`FakePetProfileSource`를 쓰는 기존 테스트가 10파일 54곳이다. 필드를 더하면 동등성 비교가 깨진다 |
| 연결 상태에 따라 프로필 출처 바꾸기 | 새 순수 Kotlin `LinkedPetProfileSource`(연결 상태 × 샘플 출처 × 서버 출처). `FakePetProfileSource`는 고치지 않는다 | 위와 같다 |
| 연결 화면 | `ParkedContent.LINK`, `parkedContentOf(…, link: LinkStep = LinkStep.None)` (기본값) | `parkedContentOf`를 위치 인자로 부르는 기존 테스트 그대로 |
| 차 자격 저장 (필수 B) | `AnonymousAuth(…, store: CredentialStore? = null)` (기본값이면 지금과 같다) | `AnonymousAuth*Test`가 지금 생성자로 부른다 |
| 웹 실패 이유 `'network'` | `LoadFailure`·`SaveFailure`에 더하고 `messages.ts`에 문구를 더한다 | 문구 표를 키 단위로 보는 기존 테스트가 없다 (Grep 확인). 읽기가 `'network'`로 실패하면 **새로 등록을 막는다** (`profileScreenReducer`의 `'unavailable'` 규칙과 같게. 서버의 기존 프로필을 빈 새 프로필로 덮어쓰지 않게) |
| 서버 클라이언트 만들기 | `AppContainer`에서 전송·익명 계정을 한 번 만들어 병원과 반려견이 같이 쓴다 → **`refactor(di)` 커밋으로 먼저** (동작 그대로) | 지금은 `hospitalSourceKind != BUILT_IN`일 때만 만든다 |

### 2. 흐름 (TV 로그인 방식)

```
[차, 정차]  [보호자 웹과 연결] ─► 코드 문서 linkCodes/{K7P3QX} 만들기 (carUid=나, createdAt=서버 시각)
            화면: "K7P 3QX" (크게) + "폰의 MeongCare 보호자 웹 → [차와 연결]에 입력하세요" + "N분 안에"
            보이는 동안 2초마다 코드 문서를 읽는다 (ownerUid가 생겼나)
[보호자 웹]  [차와 연결] → 코드 입력 → 코드 문서 읽기(carUid 알기)
            → 한 번에 쓰기(batch): 코드 claim(ownerUid=나, claimedAt=서버 시각) + 허가 petProfiles/{나}/readers/{carUid}
            → "연결했어요. 차 화면에 '콩이'가 보이는지 확인해 주세요"
[차]        ownerUid를 본다 → 코드 문서를 지운다 → petProfiles/{ownerUid}를 읽는다
            → "콩이 보호자와 연결했어요" [확인] → 처음 화면에 콩이, 진료 카드·병원 전송에 콩이
```

- 차는 보호자 쪽에 아무것도 보내지 않는다 (익명 carUid와 연결 시각만 남는다).
- 연결 해제: 차 [연결 끊기] 또는 웹 "연결된 차"에서 `readers/{carUid}`를 지운다. 그 뒤 차의 읽기는 403 → 연결 끊김.

### 3. Firestore 문서 (`docs/data/owner-link.md` 초안. 단계 1에서 확정)

| 경로 | 필드 | 쓰는 쪽 | 읽는 쪽 |
|---|---|---|---|
| `owners/{ownerUid}` | `schemaVersion: 1`, `consentVersion: 1`, `consentedAt`(서버 시각) | 보호자 웹 (동의할 때) | 보호자 |
| `petProfiles/{ownerUid}` | **pet-profile v1 필드 그대로** (`schemaVersion`, `name`, …, `updatedAt`은 ISO 문자열). 단 `photoDataUrl`은 항상 `null` (U3 추천안) | 보호자 웹 | 보호자, 연결된 차 |
| `petProfiles/{ownerUid}/private/photo` | `photoDataUrl` (data URL, 400,000자 이하) | 보호자 웹 | 보호자만 |
| `petProfiles/{ownerUid}/readers/{carUid}` | `linkCode`, `createdAt`(서버 시각) | 보호자 웹 (claim과 같은 batch) | 보호자, 그 차 |
| `linkCodes/{code}` | `schemaVersion: 1`, `carUid`, `createdAt`(서버 시각), `ownerUid`(null → 보호자 uid), `claimedAt`(null → 서버 시각) | 차 (만들기·지우기), 보호자 웹 (claim) | 차 본인, claim 전·만료 전에는 로그인한 누구나 단건만 |

- 연결 코드: 6자, 헷갈리는 글자를 뺀 대문자·숫자 (`2-9`, `I`·`L`·`O` 뺀 `A-Z`, 31종 → 약 8.9억 가지). 화면에는 `K7P 3QX`처럼 세 글자씩. 웹 입력은 소문자·하이픈·공백을 받아 정규화한다.
- 만료: `createdAt + 10분` (규칙이 서버 시각으로 판단. 만료 필드를 따로 두지 않아 차 시계를 믿지 않는다). 차 화면의 남은 시간은 차 시계로 어림한다.
- 웹에서 코드 문서를 못 읽으면(없음·만료·이미 씀이 모두 규칙상 403) 하나의 문구로 보인다: "코드를 찾지 못했거나 만료됐어요. 차에서 새 코드를 받아 주세요." (어느 경우인지 알려 주지 않는다)
- 차가 프로필을 읽을 때 사진은 따로 있어 받지 않는다. 병원에 보내는 카드도 지금처럼 사진이 없다.
- 형식 버전 규칙은 pet-profile.md를 따른다: 읽는 쪽은 짐작하지 않는다, 모르는 필드는 무시, `schemaVersion`이 1이 아니면 읽지 않는다.

### 4. 보안 규칙 (`firebase/firestore.rules`에 더한다. 기존 병원 규칙은 그대로)

| 문서 | 허용 조건 |
|---|---|
| `owners/{uid}` | get·create·delete: 본인. create는 키 목록·`consentVersion == 1`·`consentedAt == request.time`. list·update 금지 |
| `petProfiles/{uid}` | get: 본인 **또는** `readers/{request.auth.uid}`가 있는 차. **list 금지.** create·update: 본인 && `owners/{uid}`가 있음 && 키 목록(pet-profile v1 필드만) && `schemaVersion == 1` && `name`은 1~20자 문자열 && `photoDataUrl == null`. delete: 본인 |
| `…/private/photo` | 본인만 읽고 쓰고 지운다. 쓰기는 `data:image/jpeg;base64,`로 시작하고 400,000자 이하 |
| `…/readers/{carUid}` | create: 보호자 본인 && 키 `linkCode`·`createdAt` && `createdAt == request.time` && **`getAfter(linkCodes/{linkCode})`의 `ownerUid == 보호자`, `carUid == carUid`** (F1). get: 보호자 또는 그 차. list: 보호자만. delete: 보호자 또는 그 차. update 금지 |
| `linkCodes/{code}` | create: 코드가 6자 정규식 && `carUid == request.auth.uid` && `createdAt == request.time` && `ownerUid == null` && `claimedAt == null` && 키 목록. get: 차 본인, 또는 claim한 보호자, 또는 (`ownerUid == null` && `request.time < createdAt + 10분`). **list 금지.** update(claim): `ownerUid == null` && 만료 전 && 바뀐 키가 `ownerUid`·`claimedAt`뿐 && `ownerUid == request.auth.uid` && `claimedAt == request.time`. delete: 차 본인 |

- 규칙으로 막지 못하는 것 (알려진 한계, 노트·architecture에 적는다): 코드 무차별 대입 속도 제한(Functions·App Check가 없다), 남은 코드 문서 자동 정리. 6자·10분·한 번만으로 시연 범위에서는 충분하다고 본다.
- 점검 R14~R24 (`check-rules.mjs`에 더한다. 익명 계정: 보호자 O, 다른 보호자 O2, 차 C, 다른 차 C2):
  - R14 로그인 안 한 요청은 연결 코드·프로필을 만들거나 읽지 못한다
  - R15 차는 자기 uid·서버 시각·빈 ownerUid로만 코드를 만든다 (carUid가 남, 클라이언트 시각, ownerUid 채움, 형식 틀린 코드 → 403)
  - R16 연결 코드·프로필은 목록으로 읽지 못한다 (list 403)
  - R17 보호자는 만료 전·안 쓴 코드를 자기 uid·서버 시각으로 한 번만 claim한다. 이미 claim된 코드, **10분 지난 코드**(관리자 토큰으로 `createdAt`을 11분 전으로 넣은 것), 남의 uid → 403
  - R18 동의 문서 없이는 프로필을 만들지 못한다. 남의 uid 프로필은 못 쓴다. 프로필의 `photoDataUrl`은 null만, 사진 문서는 400,000자 이하 data URL만
  - R19 허가(readers)는 그 차의 코드를 이 보호자가 claim했을 때만 만든다 (claim 안 된 코드, 다른 차 uid, O2가 claim한 코드 → 403). claim + 허가 batch는 200 (F1)
  - R20 연결된 차 C는 프로필을 읽는다. C는 사진 문서·동의 문서·readers 목록을 못 읽고, 프로필을 못 쓴다. 연결 안 된 C2는 프로필을 못 읽는다
  - R21 C가 자기 readers 문서를 지우면 그 뒤 C의 프로필 읽기는 403
  - R22 O가 readers 문서를 지워도 같다. C2는 남의 readers 문서를 못 지운다
  - R23 O는 자기 프로필·사진·동의·readers를 모두 지운다. O2는 O의 것을 못 지운다
  - R24 C는 claim된 자기 코드를 읽어 `ownerUid`를 안다. C2는 그 코드를 못 읽는다. C는 자기 코드를 지운다

### 5. 차 앱

**데이터 모드 (`di/PetPlan.kt`, 순수 함수 `petSourceKindOf(mode, hasServer)`)**. `SourcePlan`·`HospitalPlan`은 바꾸지 않는다.

| 모드 | 서버 설정 | 연결 전 반려견 | 연결 | 연결 뒤 | 처음 화면 |
|---|---|---|---|---|---|
| Fake | 상관없음 | 샘플 보리 | Fake (debug 패널 [가짜 보호자 코드 입력]으로 흉내) | 샘플 보리 그대로 (모두 시연용) | [보호자 웹과 연결] + "시연용 샘플 정보예요" |
| 시연 (release) | 없음 | 샘플 보리 | 없음 (버튼 숨김) | - | 지금과 같다 (P2 회귀) |
| 시연 | 있음 | 샘플 보리 (K1) | 서버 | 서버 프로필. 못 읽으면 **"반려견 정보를 받지 못했어요"** (보리로 대신하지 않는다, K2) | 연결 전: 버튼 + 샘플 안내 / 연결 뒤: "보호자 웹과 연결됨" + [연결 끊기] |
| Real | 있음 | 등록 안 됨 | 서버 | 서버 프로필 | 위와 같다 (샘플 안내 없음) |
| Real | 없음 | 등록 안 됨 | 없음 | - | "서버 설정이 없어 보호자 웹과 연결할 수 없어요" |

- release는 012 U5 추천(11/15 APK에 Firebase 설정을 넣지 않음)을 따르면 "시연 · 없음" 줄이다. 그래서 제출 APK의 동작은 바뀌지 않는다. 영상은 debug 시연 + PC Firebase 에뮬레이터로 찍는다.

**파일 (기본안)**

```
domain/link/
  OwnerLinkState.kt     Unavailable(서버 없음) / Unlinked / Linked(ownerUid는 data 쪽만 앎 → 도메인은 linkedAt만) / Revoked(보호자가 끊음, 한 번 알림)
  LinkCode.kt           코드 만들기(난수 주입)·형식 검사·화면 표시("K7P 3QX")·남은 시간 (순수)
  OwnerLinkService.kt   state: StateFlow<OwnerLinkState>, suspend start(): LinkStart, suspend check(code): LinkCheck,
                        suspend cancel(code), suspend unlink()  (실패는 결과 값, 취소 말고 던지지 않음)
data/link/
  FakeOwnerLink.kt      메모리. 코드를 만들고 claim()을 부르면 연결된다 (debug 패널·테스트)
  (필수 B) CredentialStore.kt, LinkStoreCodec.kt(순수 직렬화·버전), KeystoreLinkStore.kt(AndroidKeyStore AES-GCM, noBackupFilesDir)
data/pet/
  LinkedPetProfileSource.kt  연결 상태 × 샘플(Fake) × 서버 프로필 흐름 → PetProfileSource (순수)
data/server/
  FirestoreJson.kt      + petProfileOf(document): PetProfileRead (pet-profile 읽는 쪽 규칙 그대로)
  FirestorePetProfileSource.kt  GET petProfiles/{ownerUid}. 200 → Loaded / 404 → NotRegistered / 403 → Revoked / 그 밖 → Unavailable
  FirestoreOwnerLink.kt documents:commit + currentDocument.exists=false + REQUEST_TIME로 코드 만들기(겹치면 새 코드로 3번까지),
                        단건 GET으로 claim 확인, 코드 지우기, readers 지우기
di/
  PetPlan.kt            petSourceKindOf
  AppContainer.kt       ownerLink, petProfileSource(LinkedPetProfileSource), petProfileIsSample: StateFlow<Boolean>
ui/link/
  LinkViewModel.kt, LinkUiState.kt, LinkTexts.kt(순수 문구), LinkScreen.kt
ui/home/
  ParkedContent.kt      + LINK, parkedContentOf(…, link)
  ParkedScreen.kt       연결 줄 ([보호자 웹과 연결] / "보호자 웹과 연결됨" + [연결 끊기] 확인)
```

- 다시 읽기 (K4): 연결 직후, 정차 처음 화면이 보이는 동안 60초마다, [진료 카드 만들기]를 누를 때. 구독이 없으면(주행 화면, 백그라운드) 읽지 않는다 (ux-guidelines 폴링 최소화).
- 연결 코드 화면은 보이는 동안만 2초마다 묻고, 만료·연결·화면을 떠나면 멈춘다. 떠날 때 claim 안 된 코드는 지운다.
- 한 차는 보호자 한 명 (K5): 연결된 상태에서 새로 연결하면 이전 `readers` 문서를 먼저 지운다.
- 진입은 처음 화면(안내 없음)에서만 (K3). 안내 중·카드·병원 찾기 화면에는 연결 버튼이 없다.

**화면 (정차 중에만. 주행이면 `homeContentOf`가 먼저 주행 화면을 고른다. 주행 화면은 바꾸지 않는다)**

| 상태 | 보이는 것 | 버튼 |
|---|---|---|
| 코드 만드는 중 | "연결 코드를 만드는 중…" | [닫기] |
| 기다림 | 아주 큰 "K7P 3QX", "폰의 MeongCare 보호자 웹에서 [차와 연결]을 누르고 이 코드를 입력하세요", "N분 안에 입력해 주세요"(분 단위로만 바뀐다) | [새 코드] [닫기] |
| 연결됨 | "콩이 보호자와 연결했어요", 반려견 요약(이름·품종·나이), "이제 진료 카드에 콩이의 의료정보가 들어가요" | [확인] |
| 만료 | "코드가 만료됐어요" | [새 코드] [닫기] |
| 실패 (서버) | "서버에 연결하지 못했어요" | [다시 시도] [닫기] |
| 처음 화면 연결 줄 | 연결 전 [보호자 웹과 연결] + "폰에서 등록한 반려견 정보를 불러와요" / 연결 뒤 "보호자 웹과 연결됨" + [연결 끊기] → 확인 "연결을 끊으면 이 차에서 콩이 정보를 볼 수 없어요" [끊기] [취소] / 보호자가 끊음 "보호자가 연결을 끊었어요" (한 번) | |

- 텍스트 입력 없음. 큰 터치 타깃(`Dimens` 버튼 높이 이상). 코드 글자 크기는 `Type.kt`에 이름을 붙여 둔다(넓은·좁은 두 값, `/verify`로 정한다).
- 문구는 `strings.xml`. 시연용 표시는 지금 규칙대로 주행 화면에는 넣지 않는다.

### 6. 보호자 웹

**모드**

| 모드 | 조건 | 저장 | 차와 연결 | 아래 안내 (U6 초안) |
|---|---|---|---|---|
| 서버 없음 | `VITE_FIREBASE_*` 없음 | 브라우저 (지금 그대로) | 없음 | "이 정보는 지금 쓰는 브라우저에만 저장돼요. 이 화면은 서버에 연결되지 않아 차와 연결할 수 없어요." |
| 서버, 동의 전 | 설정 있음 + 이 브라우저에 동의한 익명 계정 없음 | 브라우저 (지금 그대로) + 위에 안내 카드 [서버에 저장하고 차와 연결] | 동의 뒤 | "이 정보는 지금 쓰는 브라우저에만 저장돼요. 차에서 쓰려면 서버에 저장해 주세요." |
| 서버, 동의 뒤 | 동의 문서 있음 | 서버 (`petProfiles/{uid}` + 사진 문서) | [차와 연결] | "이 정보는 MeongCare 서버에 저장되고, 연결한 차에서만 볼 수 있어요." (에뮬레이터면 "(시연용 로컬 서버)" 덧붙임) |

- **동의 전에는 익명 계정을 만들지 않는다.** 다시 열 때는 Firebase Auth가 기억한 계정이 있는지만 본다 (F5).
- 동의 화면 (U6 초안): 제목 "서버에 저장하고 차와 연결할까요?"
  - 저장하는 것: 반려견 사진·이름·품종·성별·생년월일·주요 질환·알레르기·복용 약
  - 쓰는 곳: 내가 연결한 차의 진료 카드, 그 차에서 내가 고른 병원으로 보내는 진료 카드 (사진은 차·병원에 보내지 않아요)
  - 보관: 내가 지울 때까지 ([내 정보 지우기], 필수 B)
  - 동의하지 않으면: 지금처럼 이 브라우저에만 저장되고, 차와는 연결할 수 없어요
  - 이 브라우저의 방문 기록·사이트 데이터를 지우면 서버의 정보를 이 브라우저에서 고칠 수 없게 돼요 (U1 익명 계정 한계)
  - 브라우저에 저장한 정보가 있으면: "이 브라우저의 '보리' 정보를 서버로 옮기고, 옮긴 뒤 이 브라우저에서는 지워요."
  - 버튼: [동의하고 서버에 저장] / [나중에]. 다른 행동을 동의로 보지 않는다 (ux-guidelines).
- 옮기기 규칙 (`migration.ts`, 순수): 브라우저 프로필 있음 + 서버 프로필 없음 → 올린다 → 성공하면 브라우저 키를 지운다 / 서버 프로필이 이미 있으면 덮어쓰지 않고 브라우저 정보는 그대로 둔다(문구로 알림) / 올리기 실패면 브라우저 정보를 지우지 않는다.
- [차와 연결] 화면: "차 화면에 보이는 6자리 코드를 입력하세요" 입력칸(대문자 자동, 자동완성 끔) + [연결하기]. `?code=` 링크로 열면 채워 둔다 (나중 QR). 프로필을 등록하기 전에는 "먼저 반려견 정보를 등록해 주세요".
  - 성공 "연결했어요. 차 화면에 '콩이'가 보이는지 확인해 주세요." / 실패: 위 3절의 문구, 네트워크 "서버에 연결하지 못했어요. 잠시 뒤 다시 해 주세요."
- (필수 B) "연결된 차" 목록(연결 시각만. 차 이름은 없다) + [연결 끊기], [내 정보 지우기](readers → 사진 → 프로필 → 동의 → 익명 계정 순서로 지우고 브라우저 모드로).
- 층 (008과 같은 의존 방향: `ui`는 `domain`만, `firebase`는 `data/firestore*.ts`만):

```
web/src/domain/owner/  linkCode.ts(정규화·검사), ownerProfileDoc.ts(프로필 ↔ 서버 문서 + 사진 분리), ownerNotice.ts(모드 → 문구),
                       consent.ts(동의 문구·버전), migration.ts, ownerAccount.ts(인터페이스: 동의·연결·연결된 차·지우기)
web/src/data/          firestoreOwnerStore.ts(PetProfileRepository + OwnerAccount 구현, 'firebase' import는 여기만)
web/src/container.ts   설정이 있으면 firestoreOwnerStore를 동적 import, 없으면 지금 그대로. 에뮬레이터 주소는 emulatorHostFor로 127.0.0.1
web/src/ui/            ConsentScreen, LinkCarScreen(+ linkScreenReducer.ts), 안내 카드, (B) LinkedCars, DeleteMyData
```

### 7. 로그와 개인정보

- 차: 새 태그 `OwnerLink`에 결과 종류·걸린 시간·HTTP 상태만 (예: `연결 코드: 만듦`, `연결: 확인됨 (14초)`, `반려견 정보: 서버 (312ms)`, `반려견 정보: 연결 끊김 (403)`). 코드·uid·토큰·반려견 이름·의료정보는 넣지 않는다.
- 웹: `console.log` 0건 (지금과 같다).
- Pleos "차량은 여러 사람이 함께 쓰는 기기": 연결하면 그 차를 쓰는 사람은 정차 중 반려견 의료정보를 본다. 그래서 차 [연결 끊기]와 웹 연결 관리를 둔다.

### 8. 권한

- **새 `pleos.car.permission.*` 없음. 새 Pleos API 없음.** 일반 Android 권한도 더하지 않는다 (`INTERNET`은 012에서 있다. Android Keystore는 권한이 필요 없다).
- 구현 중 `SecurityException`이나 다른 권한이 필요하다는 근거가 보이면 **멈추고 묻는다.**

## 확인 필요 (시작 전에 해결)

### [사용자 승인 필요]

시작 전에 U1~U3, U5~U9 답을 진행 로그에 적는다. U4는 있으면 좋음 A 전에.

- **U1. 보호자 신원**
  - **A. (추천) 익명 Auth.** 동의할 때 익명 계정을 만들고, 그 브라우저가 계정을 기억한다. 로그인 화면이 없어 시연이 빠르다. 한계: 브라우저 사이트 데이터를 지우거나 다른 폰·브라우저에서는 그 프로필을 고칠 수 없다(차 연결은 그대로 남는다. 새로 등록하면 새 프로필·새 연결). 화면과 동의 문구에 알린다. 나중에 익명 계정을 Google 등 계정으로 올리는 길이 있다 (F8, 있으면 좋음 D).
  - B. Google 로그인: 브라우저를 바꿔도 같은 계정. 진짜 프로젝트에서는 승인 도메인·OAuth 동의 화면 설정, `http://<PC IP>`에서 팝업 동작 확인이 필요하다. 약 하루 더.
  - C. 이메일·비밀번호: 가입·재설정 화면까지. 하루 이상 더.
  - 근거: 11/15 영상과 12/03 PT는 한 브라우저에서 보인다. 기획서에는 "지금은 익명 계정, 나중에 계정 연결"로 쓴다.
- **U2. 연결 코드 설계 (한 묶음)** — 추천: 아래 그대로
  - a. 누가 만드나: **차가 만들고 보호자가 폰에 입력**한다 (반대는 차에서 글자 입력이라 규칙 3 위반).
  - b. 형식·만료: 6자(헷갈리는 글자 뺌), **10분**, 서버 시각 기준.
  - c. 한 번만: claim은 `ownerUid`가 비어 있을 때만. 연결되면 차가 코드 문서를 지운다.
  - d. 차가 읽는 범위: **그 보호자의 프로필 문서 하나만** 읽는다. 사진·동의·연결 목록은 못 읽고 아무것도 못 쓴다. 목록 조회 금지.
  - e. 차 쪽 확인 단계("콩이 보호자와 연결할까요? [연결]"): **두지 않는다.** 보호자가 차 앞에서 코드를 보고 입력하므로. 대신 연결되면 반려견 이름을 크게 보이고 [연결 끊기]를 둔다.
  - f. 연결 해제: 차 [연결 끊기](정차 중, 확인 대화) + 웹 "연결된 차"(필수 B). 둘 다 `readers` 문서를 지운다.
  - g. 한 차 = 보호자 한 명, 한 보호자 = 여러 차.
- **U3. 사진**
  - **a'. (추천) Firestore에 두되 보호자 전용 문서(`petProfiles/{uid}/private/photo`)로 분리.** 차는 규칙상 사진을 읽을 수 없다(최소 권한). 차가 받는 양도 작다. 웹 저장이 문서 두 개(batch)가 된다. 형식 버전은 그대로(프로필 문서의 `photoDataUrl`은 늘 null).
  - a. 프로필 문서 안에 data URL 그대로 (400,000자, 문서 1MiB 안). 가장 단순하지만 연결된 차도 사진을 읽을 수 있다 (차가 안 받게 할 수는 있어도 규칙으로 막지는 못한다).
  - b. 사진은 서버에 올리지 않고 브라우저에만. 저장 위치가 둘로 갈린다.
  - c. Firebase Storage + `photoUrl`(형식 v2). Spark 요금제에서 지금 쓸 수 있는지 **확인하지 않았다** (F7). PT 뒤.
- **U4. QR** (있으면 좋음 A 전에 답하면 된다)
  - **추천: 11/15는 코드만** (새 의존성 없음). QR은 PT 전 있으면 좋음 A로.
  - QR을 하면: 새 의존성 ZXing core(`com.google.zxing:core`, Apache-2.0으로 알고 있다 → 버전·라이선스를 Maven Central에서 확인해 노트에), 폰이 열 수 있는 공개 주소가 필요하다 (지금은 PC 개발 서버라 같은 Wi-Fi일 때만. 진짜 Hosting(있으면 좋음 F) 뒤가 자연스럽다), `secrets.properties`에 `OWNER_WEB_URL`(없으면 QR 숨김), Pleos 심사의 도메인 증빙 해당 여부(`[선행: pleos-docs-researcher]` R1).
  - 어느 쪽이든 **정차 중에만** 보인다 (Pleos 지역별 표: QR 로그인은 주행 중 모든 지역 ⛔, `UX_RESTRICTIONS_NO_SETUP`).
- **U5. 차 기기 저장과 네트워크 끊김**
  - S1. **(11/15 추천)** 저장하지 않음. 앱을 다시 켜면 익명 uid가 바뀌어 다시 연결한다. 영상은 한 번에 찍으면 된다.
  - S2. **(PT 전 추천, 필수 B)** 연결 자격(익명 갱신 토큰·uid·보호자 uid)만 Android Keystore(AES-GCM)로 암호화해 앱 전용 파일에 저장. 새 의존성 없음. 다시 켜면 같은 uid로 갱신해 연결이 이어진다. Pleos 보안 가이드("기기에 저장하는 민감정보는 … 암호화")를 지킨다.
  - S3. (있으면 좋음 B) S2 + 마지막으로 받은 프로필도 암호화 저장 → 네트워크가 없어도 "N시간 전 받은 정보"로 카드를 만든다 (ux-guidelines "로컬 캐싱" 권장).
  - 어느 경우든 **연결된 뒤에는 보리로 대신하지 않는다** ("반려견 정보를 받지 못했어요"). 틀린 의료정보를 보이는 것보다 안전하다 (pet-profile.md "짐작하지 않는다").
  - 결정 변경 제안: architecture.md의 "의료 프로필 저장: Room + SQLCipher"는 문서 하나·자격 하나 저장에는 과하다. **Keystore로 직접 암호화한 파일**로 바꾸는 것을 제안한다 (Room·SQLCipher 의존성을 들이지 않는다).
- **U6. 브라우저에 있던 정보 옮기기, 동의 문구, 안내 문구**
  - 추천: 6절의 흐름과 초안 그대로. 동의 전에는 지금처럼 브라우저에 저장하고, 동의하면 브라우저 정보를 서버로 한 번 올리고 성공하면 브라우저에서 지운다 (architecture.md "Firebase로 옮길 때 할 일"과 같다).
  - 문구 세 개(서버 없음 / 동의 전 / 동의 뒤)와 동의 화면 초안을 그대로 쓸지, 고칠지.
  - 서버 없음 문구를 바꾸면 `web/scripts/verify_screens.py` 133행 기대 문구와 README의 "아직 연결되지 않았다" 문단도 같이 바꾼다.
- **U7. 범위 자르기**: 위 "범위"대로 할지. 필수 A = 11/15 영상(동의·서버 저장·코드 연결·카드·병원 전송·차 연결 끊기), 필수 B = PT 전(연결 유지·웹 연결 관리·내 정보 지우기).
- **U8. 보호자 페이지에 Firebase 들이기** (008·012의 "보호자 페이지 번들에는 Firebase가 들어가지 않는다" 결정을 바꾼다)
  - 추천: 설정이 있을 때만 **동적 import**. 설정 없이 빌드한 보호자 번들은 지금 크기 그대로이고, 화면 확인 스크립트의 "밖으로 가는 요청 0건"도 그대로다. 새 npm 의존성은 없다 (`firebase` 13.1.0 이미 있음).
- **U9. 시연에서 보호자 웹을 어디서 여나** (영상 촬영 방법)
  - **a. (11/15 추천) PC 크롬의 기기 모드(폰 크기)로 연다.** 지금 설정(에뮬레이터 127.0.0.1 고정) 그대로 된다.
  - b. 실제 폰 (같은 Wi-Fi, `npm --prefix web run dev -- --host`): 폰에서 PC의 Firebase 에뮬레이터에 닿아야 한다 → 에뮬레이터를 LAN 주소에도 열고(`firebase.json` host 변경, 013 K9 "127.0.0.1 고정"을 바꿈), 웹이 접속한 주소의 호스트를 에뮬레이터 주소로 쓰게 해야 한다. Windows 방화벽·학교망에서 막힐 수 있다 → 있으면 좋음 C.
  - c. 진짜 Firebase 프로젝트 + Hosting (있으면 좋음 F): 폰에서 아무 데서나 된다. 11/15 전에는 무리라고 본다.

### [선행: pleos-docs-researcher]

- **필수 단계에는 없다.** 새 Pleos API·권한을 쓰지 않는다. 주행 중 연결 화면 금지는 ux-guidelines에 이미 있다 (QR 로그인 ⛔, `NO_SETUP`).
- 있으면 좋음 A(QR) 전에: **R1** "앱 심사의 '웹 페이지를 연결 또는 활용하는 앱은 도메인 등록 증빙'이, 차 화면에 웹 주소나 QR을 보이기만 하고 앱 안에서 웹을 열지 않는 경우에도 해당하나?" (ux-guidelines "외부 서버·네트워크" [review-registration])
- 단계 10 전에 (막지 않음, 있으면 확인): **R2** "Pleos 보안 가이드가 기기 저장 민감정보의 암호화 방식(Android Keystore, 특정 라이브러리 등)을 정해 두었나? 인증 토큰을 기기에 저장하는 것에 대한 문장이 있나?"

### 기획상 판단 (planner 기본값으로 진행한다. 바꾸려면 사용자가 알려 준다)

- K1. 시연 모드에서 연결 전에는 샘플 "보리" + "시연용 샘플 정보예요" + [보호자 웹과 연결]. Real은 연결 전 "등록 안 됨".
- K2. 연결된 뒤 서버를 못 읽으면 보리로 대신하지 않고 "반려견 정보를 받지 못했어요". 카드는 증상만으로 만들 수 있고, 병원에 보내는 카드의 `card.pet`은 null이다 (emergency-case 규칙 그대로).
- K3. 연결은 정차 처음 화면(안내 없음)에서만 시작한다.
- K4. 간격: 연결 확인 2초(코드 화면이 보이는 동안만), 코드 만료 10분, 프로필 다시 읽기는 연결 직후·처음 화면이 보이는 동안 60초·[진료 카드 만들기] 때.
- K5. 한 차 = 보호자 한 명. 새로 연결하면 이전 연결을 지운다.
- K6. 웹에서 코드를 못 찾음·만료·이미 씀은 한 문구로 보인다 (어느 쪽인지 알려 주지 않는다).
- K7. release는 서버 설정이 없어 연결 버튼이 없다 (012 U5 따름). U5(012)를 바꾸면 이 표도 다시 본다.
- K8. Fake 모드 연결은 debug 패널 [가짜 보호자 코드 입력]으로 흉내 낸다 (GPU 없는 PC에서 화면 확인용).
- K9. 영상용 반려견은 보리와 다른 "콩이"(말티즈·암컷·슬개골 탈구·닭고기 알레르기·관절약)로 등록해, 샘플에서 받은 정보로 바뀌는 것이 보이게 한다. 사진은 팀원이 직접 찍은 사진(저작권).

### 구현 중 확인 (Firebase 공식 문서·에뮬레이터. Pleos가 아니라 규칙 1 대상은 아니지만 추측으로 쓰지 않는다)

| # | 확인할 것 | 안 되면 |
|---|---|---|
| F1 | batch(claim + readers 만들기)에서 규칙 `getAfter()`로 claim 결과를 보는지 | 순서대로 두 번 쓴다 (claim → 허가). 차는 claim을 본 뒤 프로필이 403이면 10초까지 다시 읽고, 그래도 안 되면 "연결을 마치지 못했어요" |
| F2 | REST `documents:commit`에 `currentDocument.exists=false` + `updateTransforms REQUEST_TIME`으로 새 문서를 만들 수 있는지, 같은 ID가 있으면 어떤 상태 코드인지 | `createDocument?documentId=`로 만들고 `createdAt`은 따로 transform. 상태 코드는 진행 로그에 |
| F3 | 규칙에서 `resource.data.createdAt + duration.value(10, 'm')`와 `request.time` 비교 | 차가 `expiresAt`을 쓰게 하고 규칙은 `expiresAt == request.time + 10분`을 본다 |
| F4 | REST 단건 GET에서 문서가 없을 때 규칙(`resource == null`)이 403인지 404인지 (차의 NotRegistered·Revoked 구분) | 403/404를 둘 다 "연결 끊김 또는 등록 안 됨"으로 묶고, 연결 확인은 `readers/{나}` 단건 GET으로 따로 본다 |
| F5 | JS SDK 익명 로그인 유지(`authStateReady`, 기본 local persistence)가 동작하는지. 동의 전 계정을 만들지 않고 "기억한 계정이 있는지"만 볼 수 있는지 | 동의 여부를 브라우저 저장소의 표시로도 남긴다 |
| F6 | 익명 사용자 지우기(`deleteUser`)가 Auth 에뮬레이터에서 되는지 (필수 B) | 데이터만 지우고 계정은 남긴다 (노트에) |
| F7 | Firebase Storage를 Spark 요금제에서 지금 쓸 수 있는지 (U3 c를 고를 때만) | U3 a' |
| F8 | 익명 계정을 나중에 다른 계정에 연결하기(`linkWithPopup` 등), 익명 계정 자동 정리 설정이 있는지 | 있으면 좋음 D에서 다시 본다 |
| F9 | JS SDK가 정수 `1`을 `integerValue`로 저장하는지 (차는 `integerValue`·정수 `doubleValue` 모두 받는다. 정보용) | 없음 |

### 관찰로 확인 (단계 9, 10)

| # | 볼 것 | 결과가 바꾸는 것 |
|---|---|---|
| O1 | 웹 [연결하기] → 차 화면 "연결했어요"까지 걸리는 시간 | K4 간격 |
| O2 | Pleos 에뮬레이터에서 Android Keystore AES-GCM 키 만들기·암복호 (필수 B) | 안 되면 사용자에게 알리고 S1로 둔다 |
| O3 | 앱을 다시 켜면 같은 uid로 갱신되고 연결이 이어지는지 / Firebase 에뮬레이터를 다시 켜면(데이터 없음) "연결이 끊겼어요"가 되는지 (필수 B) | 문구 |
| O4 | 연결 코드 화면에서 주행으로 바뀌는 동안 연결 확인 요청이 멈추는지 (로그 `OwnerLink`) | 멈추지 않으면 고친다 |

## 확인 절차

- adb 규칙은 012·013과 같다: `ANDROID_SERIAL`(Pleos `emulator-5554`, 일반 폰 `emulator-5556`), 기어는 006 노트의 `vc()`, 스크린샷·UI 트리·로그는 `.claude/.cache/p14-<번호>.*`(커밋하지 않음). **에이전트는 Android 에뮬레이터도 Firebase 에뮬레이터도 직접 띄우지 않는다.** 꺼져 있으면 사용자에게 켜 달라고 한다. 에이전트는 `web/.env*`, `secrets.properties`를 읽지 않는다.
- 로그: `adb logcat -d -s OwnerLink HospitalTrip`, 우리 앱만: `adb logcat -d --pid=$(adb shell pidof com.meongcare.app)`.
- 준비 (사용자 터미널): `npm --prefix firebase run emulators` → `npm --prefix firebase run seed -- --reset` → `npm --prefix firebase run preflight -- --fix` 모두 PASS → 웹 개발 서버(에뮬레이터 설정) → 차 debug(에뮬레이터 설정 `127.0.0.1`) → debug 패널 시연. Pleos는 Cold boot 뒤 내비 화면을 한 번 띄운다.

### 필수 A: Pleos `emulator-5554` + 보호자 웹(PC 크롬 기기 모드, U9 a) + 병원 웹

| # | 조작 | 기대 |
|---|---|---|
| L1 | 보호자 웹(서버 모드, 브라우저에 008 때 저장한 보리가 있음)을 연다 | 지금 화면 + 위에 [서버에 저장하고 차와 연결] 카드, 아래 "동의 전" 문구. Emulator UI에 익명 계정이 아직 없다 |
| L2 | 카드 → 동의 화면 → [동의하고 서버에 저장] | "서버에 저장했어요", 아래 "동의 뒤" 문구. Emulator UI에 `owners/{uid}`, `petProfiles/{uid}`(photoDataUrl null), `private/photo`. 개발자 도구에서 `owner-web.pet-profile` 키가 없다 |
| L3 | 웹에서 콩이로 고친다 (K9) → 새로 고침 | 콩이가 그대로 보인다 (서버에서 읽음) |
| L4 | 차: 처음 화면(보리 + 샘플 안내) → [보호자 웹과 연결] → 코드 화면 → 기어 D → UI 트리 → P | D: 주행 화면, `EditText` 0·클릭 0·스크롤 0, `OwnerLink` 연결 확인 로그가 멈춘다(O4). P: 같은 코드 화면 |
| L5 | 웹 [차와 연결] → 코드를 소문자·하이픈으로 입력 → [연결하기] | 웹 "연결했어요…". 차가 몇 초 안에 "콩이 보호자와 연결했어요" (O1) → [확인] → 처음 화면에 콩이, 샘플 안내 없음, "보호자 웹과 연결됨". Emulator UI에서 그 코드 문서가 지워졌다 |
| L6 | 차: [진료 카드 만들기] → 증상 → 카드 → [가장 빨리 진료받을 곳 찾기] → 1위 → [출발] | 카드에 콩이의 병력·알레르기·복용 약. 병원 웹 환자 카드에 콩이 (사진 없음) |
| L7 | 웹에서 알레르기를 바꾼다 → 차 처음 화면에서 60초 기다리거나 [진료 카드 만들기] | 바뀐 알레르기, 정보 갱신일 |
| L8 | 웹: 틀린 코드 / 이미 쓴 코드 / 차에서 [닫기]로 지운 코드 | 모두 "코드를 찾지 못했거나 만료됐어요…" (K6). 만료는 R17로 확인 (10분을 기다리지 않는다) |
| L9 | 차 [연결 끊기] → 확인 [끊기] | 처음 화면이 보리 + 샘플 안내로. Emulator UI에서 `readers/{carUid}`가 없다 |
| L10 | 다시 연결한 뒤 `adb reverse --remove tcp:8080` → [진료 카드 만들기] → 다시 `adb reverse tcp:8080 tcp:8080` | "반려견 정보를 받지 못했어요" (보리가 아니다, K2) → 다시 닿으면 콩이 |
| L11 | 앱을 강제 종료하고 다시 켠다 (S1) | 연결 전 상태(보리). 필수 B 뒤에는 L13 |
| L12 | 로그 | `*:E`에 우리 앱 크래시·`SecurityException` 없음. 우리 앱 pid 로그에서 `grep -cE "보리\|콩이\|슬개골\|닭고기\|eyJ\|[2-9A-HJKMNP-Z]{3} ?[2-9A-HJKMNP-Z]{3}"`가 0 (코드 패턴은 오탐이 있으면 줄을 눈으로 본다) |

### 필수 B

| # | 조작 | 기대 |
|---|---|---|
| L13 | 연결한 뒤 앱을 강제 종료하고 다시 켠다 / Firebase 에뮬레이터를 다시 켜고 seed한 뒤 앱을 다시 켠다 | 같은 uid로 갱신, 콩이 그대로 (O3) / "연결이 끊겼어요" 한 번 → 보리 |
| L14 | 웹 "연결된 차" [연결 끊기] → 차 처음 화면 (60초 안) / 웹 [내 정보 지우기] | 차 "보호자가 연결을 끊었어요" → 보리 / Emulator UI에서 그 보호자 문서 모두 없음, 웹은 서버 없음·동의 전 화면 |

### 일반 폰 `emulator-5556` (GPU 없는 PC 확인)

| # | 조작 | 기대 |
|---|---|---|
| P1 | debug Fake: [보호자 웹과 연결] → debug [가짜 보호자 코드 입력] → [확인] → [연결 끊기] | 코드 화면·연결됨·처음 화면 연결 줄. 세로·가로·다크에서 글자 잘림·겹침 없음 (`/verify`) |
| P2 | release APK (서버 설정 없음) + 비행기 모드 | 지금처럼 보리로 끝까지. [보호자 웹과 연결] 없음 |
| P3 | debug 시연 + 에뮬레이터 설정(`10.0.2.2` 또는 adb reverse) | L5와 같은 연결 |

### 보호자 웹만 (Playwright, 에이전트가 할 수 있는 범위)

| # | 조작 | 기대 |
|---|---|---|
| W1 | 설정 없이 빌드 → `verify_screens.py --all-widths` | 기존 시나리오 모두 통과 (안내 문구만 U6대로) |
| W2 | 설정 없이 빌드한 `dist/assets` | 보호자 `main-*.js`에 firebase 코드 없음, 크기가 지금과 비슷 (U8) |
| W3 | 사용자 에뮬레이터에 붙은 개발 서버: 동의 전 → 동의 → 저장 → 새로 고침 | L1~L3과 같다. 콘솔 오류 0, `console.log` 0 |
| W4 | `?code=k7p-3qx`로 연다 | 입력칸에 `K7P3QX`(또는 `K7P 3QX`)가 채워져 있다 |
| W5 | 390px 폭 동의 화면·연결 화면 | 버튼·문구 잘림 없음 |

## 단계

단계 하나 = 커밋 1~3개. 앱 코드 커밋마다 `assembleDebug`가 통과해야 한다 (커밋 직전 훅). **웹 커밋 전에는 직접 `npm --prefix web run check`** (훅이 웹을 보지 않는다). 새 테스트는 대상 코드와 같은 커밋에, **새 파일로** 넣는다.

### 필수 A — 11/15 영상 (목표 11/09)

- [x] 0. 이 계획 파일 커밋 → `docs(plans): 014 보호자 웹 프로필 차 연결 계획 추가`
  - `[사용자 승인 필요]` U1~U3, U5~U9 답을 진행 로그에 적은 뒤 시작한다. 답에 따라 설계 절을 고친다.
  - `.\gradlew.bat testDebugUnitTest`와 `npm --prefix web test`의 통과 수를 진행 로그에 적는다 (완료 기준 3의 기준값).

- [x] 1. 형식 문서 → `docs(data): 보호자 프로필 서버 저장과 차 연결 형식 추가`
  - `docs/data/owner-link.md` (설계 3절: 경로·필드·쓰는 쪽·읽는 쪽·코드 형식·만료·한 번만·읽는 범위·알려진 한계), `docs/data/owner-link.example.json` (논리 형식: 동의·프로필(콩이 아님, 예시 보리 그대로 써서 pet-profile 예시와 대조)·사진 자리·허가·코드 claim 전/후).
  - `docs/data/pet-profile.md` "저장 위치" 표의 "서버 (나중)" 줄을 `petProfiles/{ownerUid}` + 사진 분리로 바꾼다. "지금 쓰는 곳" 문장도.
  - 확인: `npm --prefix web run check` (docs/data가 웹 검사 대상이다).

- [x] 2. 차: 연결 계약과 Fake (화면 없음)
  - 2-1. → `feat(domain): 보호자 연결 계약과 연결 코드 규칙 추가`
    - `domain/link/` (`OwnerLinkState`, `LinkCode`, `OwnerLinkService`, `LinkStart`·`LinkCheck` 결과 타입)
    - 새 `LinkCodeTest`: `` `코드는 헷갈리는 글자 없이 6자다`() ``, `` `같은 난수면 같은 코드다`() ``, `` `화면에는 세 글자씩 띄어 보인다`() ``, `` `형식이 틀린 코드는 받지 않는다`() ``, `` `남은 시간은 만든 시각에서 10분이고 0 아래로 내려가지 않는다`() ``, `` `남은 시간은 분 단위로 올림한다`() ``
  - 2-2. → `feat(data): Fake 보호자 연결과 연결 상태별 프로필 출처 추가`
    - `data/link/FakeOwnerLink`, `data/pet/LinkedPetProfileSource`, `di/PetPlan.kt`
    - 새 `PetPlanTest`: `` `Fake는 샘플 보리와 Fake 연결이다`() ``, `` `시연에서 서버 설정이 없으면 샘플 보리이고 연결이 없다`() ``, `` `시연에서 서버가 있으면 연결 전 샘플 연결 뒤 서버다`() ``, `` `Real은 연결 전 등록 안 됨이다`() ``, `` `Real에서 서버 설정이 없으면 연결이 없다`() ``
    - 새 `FakeOwnerLinkTest`: `` `코드를 만들면 기다림이고 claim하면 연결됨이다`() ``, `` `연결을 끊으면 연결 전이다`() ``, `` `취소한 코드는 claim해도 연결되지 않는다`() ``
    - 새 `LinkedPetProfileSourceTest`: `` `연결 전에는 샘플을 낸다`() ``, `` `연결되면 서버 프로필을 낸다`() ``, `` `연결 뒤 서버를 못 읽으면 샘플이 아니라 읽지 못함이다`() ``, `` `보호자가 끊으면 연결 끊김을 한 번 알리고 샘플로 돌아간다`() ``, `` `보호자가 프로필을 지웠으면 등록 안 됨이다`() ``, `` `Real은 연결 전 등록 안 됨이다`() ``

- [x] 3. 차: 연결 화면 (Fake로 동작)
  - 3-1. → `feat(ui): 보호자 연결 코드 화면 추가`
    - `ui/link/` (`LinkViewModel`: 열면 코드 만들기, 구독 중에만 2초마다 확인, 만료·연결·떠남에 멈춤, 떠날 때 코드 지우기 / `LinkUiState` / `LinkTexts` / `LinkScreen` 넓은·좁은 미리보기 1682dp·840dp·411dp, 라이트·다크)
    - 새 `LinkViewModelTest` (가상 시간): `` `열면 코드를 만들어 보인다`() ``, `` `보이는 동안 2초마다 연결됐는지 묻는다`() ``, `` `화면이 사라지면 묻지 않는다`() ``, `` `10분이 지나면 만료를 보이고 묻지 않는다`() ``, `` `새 코드를 누르면 이전 코드를 지우고 새로 만든다`() ``, `` `연결되면 반려견 이름을 보이고 묻기를 멈춘다`() ``, `` `코드 만들기에 실패하면 다시 시도를 보인다`() ``, `` `닫으면 claim 안 된 코드를 지운다`() ``
    - 새 `LinkTextsTest`: `` `남은 시간은 N분 안에다`() ``, `` `1분 미만은 1분 안에다`() ``, `` `연결됨 문구에 반려견 이름이 들어간다`() ``
  - 3-2. → `feat(ui): 처음 화면에 보호자 연결 줄과 연결 끊기 추가`
    - `ParkedContent.LINK` + `parkedContentOf(…, link: LinkStep = LinkStep.None)`, `ParkedScreen` 연결 줄(5절 표), `HomeScreen` 연결(기본값 있는 인자), `AppContainer`(`ownerLink`, `petProfileSource` = `LinkedPetProfileSource`, `petProfileIsSample`), `MainActivity`, debug 패널 [가짜 보호자 코드 입력](Fake 연결 + 코드 화면일 때만), `strings.xml`
    - 새 `ParkedContentLinkTest`: `` `연결 단계면 연결 화면이다`() ``, `` `증상을 고르는 중이면 연결보다 고르기가 먼저다`() ``, `` `안내 중에는 연결 화면을 보이지 않는다`() ``, `` `연결 단계여도 주행이면 주행 화면이다`() `` (`homeContentOf`와 함께)
  - 확인: `assembleDebug lintDebug`. `/verify` 일반 폰 Fake (P1). Pleos 에뮬레이터가 켜져 있으면 Fake 모드로 L4의 D/P도 본다.

- [x] 4. 보안 규칙 → `feat(firebase): 보호자 프로필과 차 연결 보안 규칙 추가`
  - `firestore.rules` (설계 4절), `check-rules.mjs` R14~R24 (머리 주석·마지막 출력 R1~R24), `firebase/README.md` "보안 규칙 요약"·명령 표.
  - F1·F2·F3·F4를 규칙 점검으로 확인하고 결과를 진행 로그에 적는다. 안 되면 표의 "안 되면"대로 규칙·설계를 고친다.
  - 확인: **사용자 터미널의 에뮬레이터에서** `npm --prefix firebase run check-rules` → R1~R24 PASS (완료 기준 4). 규칙 파일을 고치면 에뮬레이터가 바로 다시 읽는다 (013 F4).

- [x] 5. 웹 도메인 (순수) → `feat(web): 연결 코드 입력과 보호자 서버 문서 규칙 추가`
  - `web/src/domain/owner/` (`linkCode.ts`, `ownerProfileDoc.ts`, `ownerNotice.ts`, `consent.ts`, `migration.ts`, `ownerAccount.ts` 인터페이스)
  - `petProfileRepository.ts`의 실패 이유에 `'network'`, `messages.ts` 문구, `profileScreenReducer.ts`: 읽기가 `'network'`면 새로 등록을 막는다.
  - 새 Vitest:
    - `linkCode.test.ts`: 소문자·하이픈·공백 정규화, 6자가 아니면 거부, `0`·`O`·`1`·`I`·`L`이 있으면 거부, `?code=` 값 읽기
    - `ownerProfileDoc.test.ts`: `pet-profile.example.json` → 서버 문서(사진 null) + 사진 문서 → 다시 합치면 같은 프로필, 사진 없으면 사진 문서 없음, 서버 문서의 모르는 필드 무시, `schemaVersion`이 1이 아니면 읽지 않음
    - `ownerNotice.test.ts`: 서버 없음·동의 전·동의 뒤·에뮬레이터 문구
    - `migration.test.ts`: 브라우저만 있으면 올리고 지움, 서버에 이미 있으면 덮어쓰지 않음, 올리기 실패면 브라우저 그대로, 둘 다 없으면 할 일 없음
    - (`profileScreenReducer`의 `'network'` 새로 등록 막기는 새 파일 `profileScreenReducer.network.test.ts`)

- [x] 6. 웹 Firestore와 화면
  - 6-1. → `feat(web): 보호자 프로필 Firestore 저장소 추가`
    - `web/src/data/firestoreOwnerStore.ts` (기억한 익명 계정 확인, 동의 → 익명 가입 + 동의 문서, 프로필·사진 batch 저장, 읽기(사진 합치기), 코드 읽기 → claim + 허가 batch(F1)), `container.ts` 동적 import(U8) + `emulatorHostFor`, `main.tsx` 비동기 시작.
    - 확인: `npm --prefix web run check`, W1·W2.
  - 6-2. → `feat(web): 서버 저장 동의와 브라우저 정보 옮기기 화면 추가`
    - 안내 카드, `ConsentScreen`, 옮기기 연결, `StorageNotice` 모드별 문구 (U6). 문구를 바꾸면 `verify_screens.py`의 기대 문구도 같은 커밋에서 바꾼다.
  - 6-3. → `feat(web): 차와 연결 화면 추가`
    - `LinkCarScreen` + `linkScreenReducer.ts`, `?code=` 미리 채우기, 프로필 없으면 막기, 결과 문구.
    - 새 `linkScreenReducer.test.ts`: 형식이 틀리면 보내지 않음, 보내는 중 두 번 눌러도 한 번, 성공·코드 없음·네트워크·프로필 없음 상태
  - 확인: 커밋마다 `npm --prefix web run check`. W3~W5 (사용자 에뮬레이터에 붙은 개발 서버, Playwright).

- [x] 7. 차: 서버 클라이언트 정리 → `refactor(di): 서버 클라이언트를 병원과 반려견이 같이 쓰게 정리`
  - 동작 그대로. 전송·익명 계정을 `serverConfig != null && mode != FAKE`일 때 한 번 만든다. 병원 쪽 구성표(`hospitalSourceKindOf`)와 결과는 그대로.
  - 확인: 기존 테스트 모두 통과, `HospitalPlan.kt` diff 없음.

- [x] 8. 차: 서버 연결 (REST)
  - 8-1. → `feat(data): Firestore 반려견 프로필 읽기 추가`
    - `FirestoreJson.petProfileOf`, `FirestorePetProfileSource` (구독 중 60초마다, `refresh` 트리거, 403·404 구분은 F4)
    - 새 `FirestorePetProfileJsonTest`: `` `예시 프로필을 읽으면 샘플 보리와 같다`() `` (예시 JSON을 REST 형식으로 바꿔서), `` `형식 버전이 1이 아니면 읽지 않는다`() ``, `` `필수 필드가 없거나 타입이 틀리면 손상이다`() ``, `` `없는 날짜의 생년월일은 손상이다`() ``, `` `updatedAt이 UTC 형식이 아니면 손상이다`() ``, `` `빈 배열은 values 없이 와도 읽는다`() ``, `` `모르는 필드는 무시한다`() ``, `` `정수 doubleValue 형식 버전도 읽는다`() ``
    - 새 `FirestorePetProfileSourceTest`: `` `연결된 보호자의 프로필 문서를 Bearer 토큰으로 읽는다`() ``, `` `403은 연결 끊김이다`() ``, `` `404는 등록 안 됨이다`() ``, `` `네트워크 오류와 5xx는 읽지 못함이다`() ``, `` `구독하지 않으면 다시 읽지 않는다`() ``, `` `로그에 반려견 이름과 uid가 없다`() ``
  - 8-2. → `feat(data): Firestore 연결 코드와 연결 끊기 추가`
    - `FirestoreOwnerLink` (설계 5절), `AppContainer`가 `petSourceKindOf`대로 끼운다, 로그 태그 `OwnerLink`.
    - 새 `FirestoreOwnerLinkTest`: `` `코드 문서는 서버 시각과 빈 보호자로 새로 만든다`() ``, `` `코드가 이미 있으면 새 코드로 다시 만들고 세 번까지 한다`() ``, `` `보호자가 claim하면 연결됨이고 코드 문서를 지운다`() ``, `` `claim 전이면 기다림이다`() ``, `` `연결을 끊으면 허가 문서를 지우고 연결 전이다`() ``, `` `새로 연결하면 이전 허가를 먼저 지운다`() ``, `` `토큰은 헤더에만 있고 URL에 없다`() ``, `` `로그에 코드와 uid가 없다`() ``
  - 확인: 서버 설정 없이 `assembleDebug assembleRelease`, 완료 기준 6의 Android Grep. P3.

- [x] 9. 처음부터 끝까지 확인 (`/verify` 포함) → **체크포인트 A**
  - "확인 절차"의 L1~L12, P1~P3, W1~W5. O1·O4를 진행 로그에 적는다.
  - 고친 것이 있으면 `fix(...)` 커밋.
  - **여기까지면 11/15 시연 영상을 찍을 수 있다** (진행 로그에 적는다). 영상 순서 제안 (기획 담당에게 노트로 전달): 폰 크기 보호자 웹에서 콩이 등록(동의) → 차(정차) [보호자 웹과 연결] → 웹에 코드 입력 → 차 "콩이 보호자와 연결했어요" → 진료 카드에 콩이의 의료정보 → 병원 찾기·출발 → 병원 웹에 콩이 카드 → 주행 화면 → [카드 확인].

### 필수 B — PT 전 (목표 11/13, 늦으면 11/15 뒤)

- [ ] 10. 차를 다시 켜도 연결 유지 (U5 S2) → **015로 넘김**
  - 10-1. → `feat(data): 연결 자격 암호화 저장소 추가`
    - `data/link/CredentialStore`(인터페이스), `MemoryCredentialStore`, `LinkStoreCodec`(순수: 버전·필드·깨진 값은 없음으로), `KeystoreLinkStore`(AndroidKeyStore AES-GCM 키, 앱 전용 `noBackupFilesDir` 파일. 키·암호문·평문을 로그에 남기지 않는다)
    - 새 `LinkStoreCodecTest`: `` `저장한 자격을 그대로 읽는다`() ``, `` `모르는 버전은 없음으로 본다`() ``, `` `깨진 값은 없음으로 보고 던지지 않는다`() ``, `` `지우면 없음이다`() ``
  - 10-2. → `feat(data): 저장된 익명 계정으로 연결 다시 잇기 추가`
    - `AnonymousAuth(…, store = null)`: 저장된 갱신 토큰이 있으면 가입 대신 갱신(같은 uid), 갱신될 때마다 저장, 거부(400)면 저장을 지우고 새로 가입 + 연결을 "끊김"으로. `FirestoreOwnerLink`가 보호자 uid를 같이 저장·복원.
    - 새 `AnonymousAuthRestoreTest`: `` `저장된 갱신 토큰이 있으면 가입하지 않고 같은 uid로 갱신한다`() ``, `` `갱신될 때마다 새 갱신 토큰을 저장한다`() ``, `` `저장된 토큰이 거부되면 저장을 지우고 새로 가입한다`() ``, `` `저장소가 없으면 지금과 같다`() ``, `` `저장 실패는 던지지 않고 메모리로 계속한다`() ``
  - 확인: L13, O2·O3. 완료 기준 6의 `javax.crypto` Grep.
  - `[선행: pleos-docs-researcher]` R2는 막지 않는다 (답이 오면 노트에 반영).

- [ ] 11. 웹 연결 관리 → **015로 넘김**
  - 11-1. → `feat(web): 연결된 차 목록과 연결 끊기 추가`
  - 11-2. → `feat(web): 내 정보 지우기 추가` (F6)
  - 새 Vitest: 지우는 순서(readers → 사진 → 프로필 → 동의 → 계정)와 중간 실패 때 남는 것을 순수 함수로 (`deleteOrder.test.ts`)
  - 확인: `npm --prefix web run check`, L14.

### 마무리

- [x] 12. tester → `test: …` (tester가 쓴 테스트를 영역별로 커밋)
  - 넘길 것: 이 계획 파일 경로와 대상만. 대상: `LinkCode`, `petSourceKindOf`, `FakeOwnerLink`, `LinkedPetProfileSource`, `LinkViewModel`, `LinkTexts`, `parkedContentOf`(연결 단계), `FirestoreJson.petProfileOf`, `FirestorePetProfileSource`, `FirestoreOwnerLink`, (B) `LinkStoreCodec`, `AnonymousAuth`(복원), 웹 `web/src/domain/owner/*`, `linkScreenReducer`, `profileScreenReducer`(`'network'`).
  - 대상 아님: Compose 화면, `AppContainer`, `KeystoreLinkStore`, `firestoreOwnerStore.ts`(SDK), 규칙 점검 스크립트.
  - 계약 기준: 설계 3·4·5·6절의 표와 경계 (만료 정확히 10분, 확인 간격 1ms 전, 연결 중 403·404, 코드 겹침 세 번, 정규화 경계, 옮기기 실패).
  - 버그 보고는 메인이 `fix:`로 고친다. UI가 바뀌면 `/verify` 다시.

- [x] 13. 문서 갱신
  - 13-1. → `docs(architecture): 보호자 연결 구조와 반려견 데이터 모드 반영`
    - "서비스 기능 스택": 의료 프로필 저장 행(U5 결과, Room+SQLCipher → Keystore 파일 제안이 승인되면), 보호자 등록 웹 행(서버 저장·연결 코드). "데이터 모드"에 `petSourceKindOf` 표, "반려견 프로필은 모든 모드에서 Fake" 문장 고치기. "보호자 웹" 결정 표(저장·의존성·번들), "저장과 개인정보", "Firebase로 옮길 때 할 일"에서 한 것 지우기·남은 것(진짜 프로젝트·개인정보처리방침 URL·코드 무차별 대입 한계). 패키지 구조(`domain/link`, `data/link`, `ui/link`, `di/PetPlan.kt`).
  - 13-2. → `docs: README와 AGENTS에 보호자 연결 반영`
    - `AGENTS.md`: 프로젝트의 보호자 웹 줄("차 앱과는 아직 연결되지 않았다" 고치기), 아키텍처 규칙(보호자 웹 `firebase`는 `web/src/data/firestore*.ts`만), 디렉토리 표 `docs/data/` 설명.
    - `README.md` 21·52행 문단(연결 방법, 서버 없으면 보리), `web/README.md`(서버 모드·동의·연결 화면), `firebase/README.md`(R1~R24).
  - 확인: 문서만 바뀐 커밋. 링크가 맞는지 본다.

- [x] 14. 개발 노트 작성 (`docs/notes/`)
  - `docs/notes/xxj15/YYYY-MM-DD-owner-link.md` (형식: docs/notes/README.md). 넣을 것:
    - 결정과 이유: U1~U9 답, TV 로그인 방식, 사진 분리, 보리로 대신하지 않는 이유, 기존 테스트를 지킨 자리(설계 1절)
    - 사용한 Pleos API·권한: 새 것 없음. 주행 중 연결 화면 금지 근거(QR 로그인 ⛔)
    - 새 의존성 없음 (QR을 했으면 ZXing과 라이선스)
    - 확인한 것 / 확인하지 못한 것 (실제 폰, 진짜 Firebase 프로젝트, 실차, 코드 무차별 대입 제한 없음)
    - **기획서 5번 "개발 계획"에 넣을 문장** (확인한 것만). 예: "보호자는 폰 웹에서 반려견 프로필과 의료정보를 등록하고 서버 저장에 동의한다. 차에서는 정차 중 [보호자 웹과 연결]을 누르면 6자리 연결 코드가 크게 뜨고, 보호자가 폰에 그 코드를 입력하면 차가 그 반려견 정보로 진료 카드를 만든다. 차에서는 글자를 입력하지 않는다. 연결된 차는 그 반려견의 진료용 정보만 읽을 수 있고 사진은 읽을 수 없으며, 보호자나 차 어느 쪽에서든 연결을 끊을 수 있다. 연결 화면은 주행 중에는 보이지 않는다 (Pleos 주행 규제: QR 로그인·설정 제한)."
    - 남은 일: 있으면 좋음 중 안 한 것, 012 U5·K9, B를 미뤘다면 그것
  - 이 계획을 `docs/plans/completed/`로 옮기고 진행 로그를 마친다.
  - 커밋: `docs(notes): owner-link`, `docs(plans): 014 보호자 웹 프로필 차 연결 완료`
  - 확인: `git status`에 남은 변경이 없다. 그다음 `/ship`.

### 있으면 좋음 (필수가 끝난 뒤, 단계 14 전에)

- [ ] A. `[선행: pleos-docs-researcher]` R1, `[사용자 승인 필요]` U4 → QR: 연결 코드 화면에 QR(`OWNER_WEB_URL/?code=…`), 설정이 없으면 숨김 → `feat(ui): 연결 코드 화면에 QR 추가` (ZXing core 버전·라이선스를 같은 커밋에)
- [ ] B. 마지막 프로필 암호화 캐시 (U5 S3): 네트워크가 없으면 "N시간 전 받은 정보" → `feat(data): 받은 반려견 프로필 암호화 캐시 추가`
- [ ] C. 실제 폰 LAN 시연 (U9 b): 에뮬레이터를 LAN 주소에도 열기, 웹이 접속 주소로 에뮬레이터를 찾기 → `chore(firebase): 폰에서 보호자 웹이 에뮬레이터에 붙는 설정 추가`
- [ ] D. 보호자 계정 업그레이드 (F8): 익명 계정을 다른 계정에 연결 → 따로 계획
- [ ] E. 차 진료 카드에 사진 (U3이면 규칙·형식을 다시 정해야 한다) → 따로 계획
- [ ] F. 진짜 Firebase 프로젝트 + 보호자·병원 웹 Hosting (012 있으면 좋음 B·D) → 따로 계획

## 진행 로그

- 2026-11-06: 계획 작성.
  - 브랜치 `feat/owner-link`는 dev `c8361f6`(013 smooth-trip + `fix/demo-autofix`)에서 땄다.
  - 시작 시점의 코드:
    - 차: 반려견은 모든 모드에서 `FakePetProfileSource`("보리"), `AppContainer.isPetProfileSample = true`. 서버 클라이언트(`AnonymousAuth`, Firestore REST)는 병원용으로만 만들고, 익명 자격은 메모리에만 있다 (앱을 다시 켜면 새 uid).
    - 웹: 보호자 페이지는 localStorage(`owner-web.pet-profile`)만. 번들에 Firebase 없음. 안내 "이 정보는 지금 쓰는 브라우저에만 저장돼요. 차 앱과의 연결은 준비 중이에요."
    - 서버: `firestore.rules`에 병원·환자 건 규칙만 (R1~R13).
  - 사용자 결정 (이전): 외부 서버 허용(A1), Firebase Spark·Functions 없음(012), 차에서 글자 입력 금지, 서버 설정이 없으면 보리로 끝까지, 새 Pleos 권한 없음. 이 기능은 2026-11-06 사용자가 다음 작업으로 골랐다.
- 2026-11-06: **사용자 승인 (U1~U9 모두 추천안)**.
  - U1 익명 Auth. U4 11/15는 코드만 (QR은 있으면 좋음 A).
  - U2·U3(a' 사진 보호자 전용 문서)·U5(S1 → 필수 B에서 S2, Room+SQLCipher → Keystore 파일 제안 승인)·U6·U7·U8(동적 import)·U9(a PC 크롬 폰 크기)는 "추천대로"로 한꺼번에 승인.
  - 기준값 (단계 0): Android `testDebugUnitTest` **838**개 통과, 웹 `npm --prefix web test` **371**개(35 파일) 통과.
- 2026-11-06: 단계 1~3 완료 (`13316c8`~`d979a61`). Android 838 → 883.
  - 이름을 바꾼 것: `petSourceKindOf` → `petPlanOf`(+ `petProfileIsSampleOf`), `LinkStep` → `linking: Boolean`, 연결 줄은 `LinkRow`/`linkRowOf`, 화면 연결 묶음은 `LinkExtras`. 서버 설정 없는 구성용 `NoOwnerLink`.
  - 프로필 다시 읽기: `LinkedPetProfileSource.refresh()`. 처음 화면이 보이는 동안(`repeatOnLifecycle(STARTED)`) 60초마다 + [진료 카드 만들기] 때. 구독이 없으면 5초 뒤 멈춘다 (`shareIn WhileSubscribed`).
  - Pleos 에뮬레이터 Fake 확인: 처음 화면 연결 줄 → 코드 화면("YK9 GCA", 10분 안에) → 주행: 주행 화면만, `EditText` 0, 클릭은 debug 패널뿐 → 정차: 같은 코드 → debug [가짜 보호자 코드 입력] → "보리 보호자와 연결했어요" → [확인] → "보호자 웹과 연결됨" + [연결 끊기] → 확인 대화 → [끊기] → 연결 전. 다크 모드 코드 화면 이상 없음. 오류 로그 없음.
  - 확인 중 고친 것: 샘플 안내가 "보호자 웹과는 아직 연결되지 않았어요"라 Fake 연결 뒤 "연결됨"과 어긋났다 → "보호자 웹에서 등록한 정보가 아니에요". 확인 대화 버튼이 작은 글자 버튼이었다 → 160dp × 카드 버튼 높이.
- 2026-11-06: 단계 4~9 완료 (`427f82e`~`a0012b4`) → **체크포인트 A: 11/15 영상을 찍을 수 있다.** Android 883 → 911, 웹 371 → 407.
  - 보안 규칙 R1~R24 + R19b 모두 PASS (사용자 터미널 에뮬레이터). F1 batch에서 `getAfter`로 claim을 본다: 된다. F2 같은 코드 다시 만들기: **409**. F3 `createdAt + duration.value(10, 'm')`: 된다 (11분 전 코드는 읽기·claim 403). F4 허가는 있고 프로필이 없으면 **404**, 허가가 없으면 403, 지운 코드 읽기 403.
  - 바꾼 것: 같은 차를 허가가 남은 채 다시 연결하면 웹의 `set`이 update가 되어 거부됐다 → readers에 같은 조건의 `update` 허용 (R19b).
  - 이름: 차 `FirestorePetProfileSource` → `FirestorePetProfileReader`(`LinkedProfileReader`). 웹 `ownerAccount.ts`에 `OwnerServer`(계정 + 저장소 + emulator). 보호자 웹 Firebase 앱 이름 `owner`.
  - W1 서버 설정 없이 빌드: `verify_screens.py --all-widths` 26/26 (안내 문구만 browser-only로). W2 보호자 `main-*.js`에 Firebase 없음, `index.html` modulepreload에도 없음 (`firestoreOwnerStore`·Firebase는 별도 청크, 설정이 있을 때만 내려받음).
  - 끝까지 (debug 시연 + PC Firebase 에뮬레이터 + 폰 크기 Chromium, 웹 개발 서버는 `.env.example`의 에뮬레이터 값):
    - L1~L3 브라우저의 콩이 → 동의 → "옮겼어요", 브라우저 키 없음, 새로 고쳐도 서버의 콩이 + [차와 연결], 안내 "(시연용 로컬 서버)". 콘솔 오류 0, 가로 넘침 0.
    - L5 웹에 `dpr-vfx` 입력 → "연결했어요…'콩이'" → 차 "콩이 보호자와 연결했어요"(웹 제출 후 약 2초, O1) → 코드 문서 404(지워짐) → 처음 화면 콩이, 샘플 안내 없음, "보호자 웹과 연결됨".
    - L6 [진료 카드 만들기] 때 다시 읽음(로그) → 카드에 슬개골 탈구·닭고기·관절약 → 1위 병원 [출발] → 서버 환자 건 `card.pet` = 콩이·닭고기, 사진 없음.
    - L7 웹에서 알레르기 "닭고기, 소고기" 저장 → 차 처음 화면에 들어올 때 다시 읽음 → 새 카드에 반영.
    - L8 틀린 코드·쓴 코드·차에서 [닫기]로 지운 코드 → 모두 "코드를 찾지 못했거나 만료됐어요…". W4 `?code=k7p-3qx` → `K7P3QX`.
    - L9 차 [연결 끊기] → 확인 → 보리 + 샘플 안내, 서버 readers·linkCodes 비었음.
    - L11 강제 종료 후 다시 켜면 연결 전(보리). L12 우리 앱 pid 로그 123줄에 반려견 이름·의료정보·uid·토큰·코드 0건, 오류·`SecurityException` 0.
    - 확인 중 고친 것: [새로 만들기]는 프로필을 다시 읽지 않았다 → [진료 카드 만들기]와 같이 다시 읽는다.
  - **확인하지 못한 것**: L10 서버에 닿지 못할 때 "불러오지 못했어요"(보리 아님) — `adb reverse`를 지워도 열린 keep-alive 연결로 계속 닿았다 (013과 같음). 단위 테스트(`LinkedPetProfileSourceTest`, `FirestorePetProfileReaderTest`)로만. L4 시연 모드에서 기어 D(Virtual Console)는 보지 않았다 (Fake 모드에서 D/P 확인함). P1~P3 일반 폰 에뮬레이터는 꺼져 있었다. release APK 설치 확인은 안 했다 (`assembleRelease` 성공).
  - 알려진 한계: 앱을 다시 켜면 차의 익명 uid가 바뀌어 이전 허가 문서가 서버에 남는다 (차는 읽지 못한다). 웹 "연결된 차"(필수 B)에서 지울 수 있게 한다.
  - 다음: 필수 B(10·11)는 새 계획(015)으로 넘기고 12(tester) → 13(문서) → 14(노트) → `/ship` (범위 절의 규칙대로. 영상 촬영 준비를 먼저 dev에 올린다).
- 2026-11-06: 단계 12~14 완료. **필수 A로 마무리하고 필수 B(10·11)는 계획 015로 넘긴다** (범위 절의 규칙: 영상 촬영 준비를 먼저 dev에 올린다).
  - tester: Android 103, 웹 52 경계 테스트. 버그 3개를 찾았고 고쳤다 (`c612641` 연결 직후 서버 응답 전 샘플 보리, `3dcc186` 차 시계가 뒤로 가면 남은 시간 10분 초과, `d198d81` 24시·60초 updatedAt). Android **1014**, 웹 **459** 모두 통과.
  - 고친 뒤 에뮬레이터에서 다시 연결해 연결됨 화면·처음 화면이 콩이(샘플 안내 없음)인 것을 확인했다.
  - 노트: docs/notes/xxj15/2026-11-06-owner-link.md

## 결정 / 배운 점

- 공유 흐름(`shareIn replay=1`)의 마지막 값은 출처가 바뀌어도 새 구독자에게 먼저 간다. 연결 상태처럼 "지금 무엇이 맞는 값인가"가 바뀌는 곳에서는 값에 출처를 달고, 구독하는 쪽에서 지금 상태와 맞는지 다시 본다. 멈출 때 마지막 값을 잊게(`replayExpirationMillis = 0`) 한다.
- Firestore 규칙은 `resource`를 보지 않는 get 조건이면 없는 문서에 404를 준다. 이것으로 "등록 안 됨(404)"과 "연결 끊김(403)"을 나눴다.
- `adb reverse`를 지워도 열린 keep-alive 연결은 살아 있다. 네트워크 끊김은 기기에서 재현하기 어렵다 (013과 같음).

## 사용자 승인 요약 (한눈에. 자세한 것은 "확인 필요")

| # | 묻는 것 | 추천 |
|---|---|---|
| U1 | 보호자 신원 | 익명 Auth (브라우저를 지우면 고칠 수 없다는 것을 알림). 계정 연결은 나중 |
| U2 | 연결 코드 | 차가 만들고 폰에 입력, 6자·10분·한 번만, 차는 프로필 문서 하나만 읽기, 차 쪽 확인 단계 없음, 양쪽에서 해제, 차 하나 = 보호자 하나 |
| U3 | 사진 | Firestore 보호자 전용 문서로 분리 (차는 못 읽음). Storage는 Spark 확인 뒤 PT 이후 |
| U4 | QR | 11/15는 코드만. QR(ZXing, 새 의존성)은 PT 전 있으면 좋음 |
| U5 | 차 기기 저장 | 11/15 저장 안 함 → PT 전 자격만 Keystore 암호화 저장. 연결 뒤 보리로 대신하지 않음. Room+SQLCipher 결정을 Keystore 파일로 바꾸기 |
| U6 | 기존 데이터·동의·문구 | 동의 전엔 브라우저 저장, 동의하면 한 번 옮기고 브라우저에서 지움. 6절 문구 초안 |
| U7 | 범위 | 필수 A(11/15 영상) / 필수 B(PT 전) / 있으면 좋음 |
| U8 | 보호자 페이지에 Firebase | 설정이 있을 때만 동적 import |
| U9 | 영상에서 보호자 웹을 어디서 | 11/15는 PC 크롬 폰 크기. 실제 폰은 있으면 좋음 |
