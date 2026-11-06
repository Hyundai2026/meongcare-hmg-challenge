# 012 가장 빨리 진료받을 병원 (제휴 병원 대기시간 + 진료 카드 전송)

- 목표: 응급 상황에서 차 앱이 제휴 병원들이 알린 대기시간과 이동 시간 추정을 합쳐 "가장 빨리 진료받을 수 있는 병원" 순으로 보여 주고, 고른 병원으로 진료 카드와 도착 예정 시간을 보내며 길안내를 시작한다. 병원 쪽은 기존 `web/` 안의 시연용 병원 페이지에서 대기시간을 알리고 들어온 카드를 본다.
- 완료 기준 (어떻게 검증하나). `$BASE` = `d4aa907` (이 브랜치의 첫 커밋, rebase 뒤에는 `origin/dev`).
  1. `node .claude/hooks/build-gate.mjs pipeline`이 "검증 파이프라인 통과."를 출력한다 (최신 dev 위로 rebase한 뒤).
  2. `secrets.properties`에 Firebase 값이 **없는** 상태에서 `.\gradlew.bat assembleDebug assembleRelease`가 성공한다 (CI·새 팀원 PC와 같은 상태).
  3. 단위 테스트 (`.\gradlew.bat testDebugUnitTest`):
     - 새 테스트 클래스가 통과한다: `TravelEstimateTest`, `HospitalRankingTest`, `BuiltInHospitalBoardSourceTest`, `FallbackHospitalBoardSourceTest`, `HospitalTripTest`, `HospitalFinderViewModelTest`, `HospitalTextsTest`, `ServerConfigTest`, `HospitalPlanTest`, `FirestoreJsonTest`, `FirestoreHospitalBoardSourceTest`, `FirestoreCaseSenderTest`, `SampleHospitalsJsonTest`, 그리고 tester가 더한 것.
     - 기존 테스트가 모두 그대로 통과한다. 개수는 단계 0에서 센 값이다.
     - `git diff $BASE --diff-filter=MD --name-only -- app/src/test`의 출력이 비어 있다 (기존 테스트를 고치거나 지우지 않는다).
  4. 웹: `npm --prefix web run check` 통과. `python web/scripts/verify_screens.py --base-url http://127.0.0.1:4173 --out .claude/.cache/web --all-widths`가 기존 시나리오와 새 병원 웹 시나리오(H01~H05)를 모두 통과한다 (Firebase 설정 없이 빌드한 것으로).
  5. Firebase 에뮬레이터 (PC):
     - `npm --prefix firebase run emulators`로 Auth·Firestore 에뮬레이터가 뜬다.
     - `npm --prefix firebase run seed`가 시연 병원 5곳을 넣는다.
     - `npm --prefix firebase run check-rules`가 R1~R8을 모두 PASS로 출력한다 (아래 설계 결정 6).
  6. 바뀌지 않아야 하는 것 (`git diff $BASE -- <경로>`가 비어 있다):
     - `di/SourcePlan.kt`, `di/NaviPlan.kt`, `di/LocationPlan.kt`, `di/DataMode.kt`
     - `data/guidance/navi/` 전체, `data/guidance/NaviGuidanceSource.kt`, `data/guidance/NaviRouteOnlyGuidanceSource.kt`
     - `domain/guidance/GuidanceSource.kt`, `domain/location/`
  7. 정적 검사 (Grep, `app/src/main` 기준):
     - `HttpURLConnection`·`import java.net`은 `data/server/UrlConnectionHttpTransport.kt`에만 있다.
     - `import org.json`은 `data/server/` 아래에만 있다.
     - `ui/` 아래에 `import com.meongcare.app.data`, `import com.meongcare.app.di`가 0건이다.
     - `domain/hospital/`, `domain/trip/`, `data/server/`(전송 어댑터 1개 제외), `data/hospital/`에 `import android.`이 0건이다 (JVM 테스트 가능).
     - debug 병합 매니페스트에서 `android:name="pleos.car.permission`이 정확히 2건(`NAVI_ROUTE`, `NAVI_ROUTE_SEARCH`), `android.permission.INTERNET`이 1건이다.
     - release 병합 매니페스트에 `networkSecurityConfig`와 `usesCleartextTraffic="true"`가 없다.
     - 웹: `from 'firebase`는 `web/src/data/firestoreHospitalDesk.ts`와 `web/src/hospital/container.ts`에만 있다. `web/src`(테스트 제외)에 `console.log`가 0건이다.
  8. Pleos 에뮬레이터 + PC 병원 웹: 아래 "확인 절차"의 E1~E12.
  9. 일반 폰 에뮬레이터: P1~P4 (P2는 release APK + 비행기 모드).
  10. 주행 중 안전: 병원 목록·확인 화면에서 기어 D → 주행 화면 (debug 패널 영역을 빼면 `EditText` 0개, 클릭할 수 있는 노드 0개), P → 같은 목록·확인 화면 (E11).
  11. 로그: 우리 앱 pid 로그에 좌표·반려견 이름·증상 글자가 0건이다 (E12).
  12. 문서: `docs/data/hospital-status.md`, `docs/data/emergency-case.md`와 예시 JSON 2개가 있다. `docs/architecture.md`(서비스 기능 스택, 데이터 모드의 병원 표, 패키지 구조, "병원 웹" 절), `AGENTS.md`(기술 스택·명령어·디렉토리), `README.md`(시연 흐름), `web/README.md`, `firebase/README.md`가 갱신돼 있다.
- 브랜치: `feat/fastest-hospital` (최신 dev = 010 길안내 + 011 현재 위치 위에서 땄다. 첫 커밋 `d4aa907`은 문의 A1 답 기록)
- 관련 문서:
  - AGENTS.md: 규칙 1·2·3·4·6·7·8, 아키텍처 규칙 (SDK는 `data/` 래퍼, 래퍼마다 Fake, 보호자 웹은 저장소 인터페이스만)
  - docs/architecture.md: "서비스 기능 스택"(A1 답: 외부 API·자체 서버 허용), "데이터 모드", "주행 상태와 화면"(2단계 표), "보호자 웹"과 "Firebase로 옮길 때 할 일"
  - docs/data/pet-profile.md: 공유 형식 문서의 본보기. 카드의 반려견 부분이 이 형식을 따른다
  - docs/pleos/sdk-notes.md: NaviHelper 절 (`requestRoute` 좌표+이름만으로 됨 `[obs-navi-emu]`, `requestTrip` 미확인, `getCurrentLocationInfo` 내비 준비 전 답 없음)
  - docs/pleos/ux-guidelines.md: "주행 안전 설계 원칙", "앱 심사·보안·개인정보 정책"(HTTPS·TLS 1.2+, 로그 마스킹, 동의, 폴링 최소화, 주행 중 네트워크 끊김 대비)
  - docs/plans/completed/009-medical-card.md(카드 화면·세션), 010-real-navi-guidance.md(내비 길안내), 011-current-location.md(현재 위치, K3·K4), 008-owner-web.md(웹 구조·화면 확인 스크립트)
  - docs/notes/xxj15/2026-11-05-current-location.md "주의점/남은 일"
  - 코드: `di/AppContainer.kt`, `domain/hospital/Hospital.kt`, `data/hospital/SampleHospitals.kt`, `domain/card/MedicalCardSession.kt`, `ui/card/MedicalCardViewModel.kt`·`MedicalCardScreen.kt`, `ui/home/ParkedContent.kt`·`HomeScreen.kt`·`DrivingScreen.kt`, `ui/guidance/GuidanceViewModel.kt`, `data/guidance/FakeGuidanceSource.kt`, `MainActivity.kt`, `web/src/`(main.tsx, container.ts, domain/, data/, ui/), `web/vite.config.ts`, `web/scripts/verify_screens.py`

## 범위

- **필수** (11/06~11/07 이틀): 단계 0~16.
  - 11/06 목표: 단계 0~6 → **체크포인트 A: 차 앱만으로 가짜 데이터로 끝까지 시연된다** (시연 영상 백업을 이때 찍을 수 있다).
  - 11/07 목표: 단계 7~16 → 병원 웹, Firebase 에뮬레이터, 차·웹 연결, 처음부터 끝까지 시연.
  - 11/15 제출물에 주는 것:
    - 기획서 "창의성·차량특화성": 운전 중 여러 병원에 전화로 묻는 대신, 정차 중 한 화면에서 "이동+대기"로 고르고 출발하면 카드와 도착 예정 시간이 병원에 자동으로 간다. 주행 중에는 남은 시간이 병원에 갱신된다.
    - 기획서 "사업성": 병원 쪽 화면(제휴 병원)이 생겨 B2B 제휴 모델을 설명할 근거가 된다.
    - 기획서 "기술 활용성": NaviHelper 현재 위치(011)를 순위의 기준점으로, NaviHelper 길안내(010)를 고른 병원으로 쓴다.
    - 시연 영상: 차 화면과 병원 웹을 나란히 찍을 수 있다.
- **있으면 좋음**: A~F (필수가 끝난 뒤, 단계 16 전에). 아래 "단계".
- **시간이 모자라면 이 순서로 자른다** (위가 먼저):
  1. 있으면 좋음 전부
  2. 단계 6-2 (Fake 길안내를 목록의 이동 추정에서 시작) → 대신 목록에 "이동 시간은 어림값" 문구만 둔다
  3. 단계 9 (웹 화면 확인 시나리오) → 수동 스크린샷으로 대신한다. `npm --prefix web run check`는 그대로 한다
  4. 단계 10-2의 규칙 점검 스크립트 → Emulator UI에서 R1~R8을 손으로 해 보고 진행 로그에 적는다
  5. 단계 8의 "약 N분 후" 실시간 갱신 → 도착 예정 시각만 보인다
  6. (최후) 단계 12-3의 Real 모드 "서버만" 처리 → Real도 시연처럼 내장 병원으로 대체한다
  - 자르지 않는 것: 0~5, 7, 8의 핵심(상태 입력·환자 목록), 10의 보안 규칙, 11, 12의 시연 모드, 13, 16.
- **이번 범위 밖**:
  - 카카오 로컬 검색 병원 ("대기 정보 없음" 병원). 타입과 순위 규칙에 자리만 둔다 (설계 결정 2·5).
  - Firebase Functions, Blaze 요금제, Claude API.
  - 보호자 웹(반려견 프로필)을 Firestore로 옮기기. 차 앱의 반려견 정보는 계속 Fake "보리"다.
  - NaviHelper로 후보별 실제 소요 시간 얻기 (있으면 좋음 E, `[선행: pleos-docs-researcher]`).
  - 주행 중에 병원을 찾거나 바꾸는 기능 (문의 B1·B2 뒤).
  - 프로세스가 죽은 뒤 진행 중인 전송 복원.
  - 실차 확인.

## 설계 결정 (planner 제안과 근거)

코드 블록은 기본안이다. 이름과 세부는 구현하면서 바꿔도 된다. 바꾸면 이 블록을 고치고 "결정 / 배운 점"에 이유를 남긴다.

### 1. 이동 시간 추정: (a) 직선 거리 기반 어림값 (추천)

| 안 | 내용 | 장점 | 단점 | 판단 |
|---|---|---|---|---|
| **(a)** | 현재 위치와 병원 좌표의 직선 거리(하버사인) × 우회 계수 ÷ 평균 속도. 화면에 "약"을 붙인다 | 네트워크·SDK 없이 바로 된다. 순수 함수라 JVM 테스트로 확정할 수 있다. 시연이 어디서든 같은 값이다 | 막히는 길, 강·산을 모른다 | **고름** |
| (b) | NaviHelper로 후보마다 소요 시간만 받기 | 실제 경로 시간 | sdk-notes에 그런 API가 확인돼 있지 않다. `requestRoute`는 내비 앱을 앞으로 띄우고 안내를 시작한다 (`[obs-navi-emu]`) → 후보 비교에 못 쓴다. `requestTrip(TripRequest)`은 "동작 문서 없음", `onTripInfo`는 관찰에서 오지 않았다 | 있으면 좋음 E로 미룬다. `[선행: pleos-docs-researcher]` |
| (c) | 카카오모빌리티 길찾기 | 실제 도로 시간 | 서버(키 보관)가 필요한데 이번에는 Functions를 쓰지 않는다 | 제외 |

- 계산 (`domain/hospital/TravelEstimate.kt`, 순수 함수):
  - `분 = 올림(직선km × 1.25 ÷ 21km/h × 60)`, 최소 1분. 같은 좌표도 1분이다.
  - 계수의 근거: Pleos 에뮬레이터 내비에서 시연 위치(37.4127212, 127.094681) → "시연용 동물병원"(37.48544722, 127.03636666)이 12km·35분이었다 (`[obs-navi-emu]`). 두 점의 직선은 약 9.6km다 (구현 때 함수로 계산해 진행 로그에 적는다). 12 ÷ 9.6 ≈ 1.25, 12km ÷ 35분 ≈ 21km/h.
  - **관찰 1건으로 맞춘 시연용 가정이다.** 코드 KDoc과 architecture.md에 그렇게 적는다. 기획서에도 "직선 거리 어림값"이라고 쓴다.
- 고른 뒤에는 실제 값을 쓴다: 길안내가 시작되면 병원으로 보내는 도착 예정 시간은 `GuidanceState.Guiding.remainingTime`이다 (Real·시연 차량 기기는 내비 `duration`에서 시작, 010).
- 현재 위치를 모르면(내비 준비 전, 011 노트) 이동 시간 칸을 비운다 → 설계 결정 2의 "대기 시간 순".

### 2. 순위 규칙 (`domain/hospital/HospitalRanking.kt`, 순수 함수)

```kotlin
data class RankingRules(
    val staleAfter: Duration = 30.minutes,      // 이보다 오래된 대기 정보는 "오래됨"
    val futureTolerance: Duration = 5.minutes,  // 갱신 시각이 이보다 미래면 시계가 어긋난 것으로 보고 "오래됨"
)

fun rankHospitals(
    candidates: List<HospitalCandidate>,
    from: CurrentLocation?,      // null = 위치 모름
    now: Instant,
    rules: RankingRules = RankingRules(),
): HospitalRanking

data class HospitalRanking(
    val ranked: List<RankedHospital>,       // 대기 정보가 유효하고 응급 접수 중: 합계 순
    val unranked: List<RankedHospital>,     // 대기 정보 없음·오래됨: ranked 뒤에 이동 시간 순 (선택은 된다)
    val notAccepting: List<RankedHospital>, // 유효한 정보로 "응급 접수 안 함": 이름만 따로 (선택 안 됨)
    val basis: RankBasis,                   // TRAVEL_PLUS_WAIT / WAIT_ONLY(위치 모름)
)
```

| 경우 | 규칙 |
|---|---|
| 순위 값 | `합계 = 이동 추정 + 대기 분`. 혼잡도는 표시만 하고 순위에 넣지 않는다 (대기 분에 이미 들어 있다. 설명하기 쉽다) |
| 동점 | 합계 → 이동 시간 짧은 곳 → 이름(문자열 순) → id. 같은 입력이면 항상 같은 순서 |
| 응급 접수 불가 | 정보가 유효하면 `notAccepting`. 화면에 "지금 응급 접수를 받지 않는 곳" 이름만 보이고 고를 수 없다 |
| 오래된 정보 | `now - updatedAt > 30분`이면 `unranked`, "대기 정보 N분 전"으로 표시. **정확히 30분은 유효하다.** 오래된 정보의 "응급 접수 불가"도 믿지 않고 `unranked`에 둔다 (모르는 것으로 본다) |
| 미래 시각 | `updatedAt`이 `now`보다 5분 넘게 뒤면 오래됨과 같다. 5분 안이면 0분 전으로 본다 |
| 대기 정보 없음 | `status == null` (입력 전, 형식 오류, 나중의 카카오 검색 병원) → `unranked` |
| 위치 모름 | `basis = WAIT_ONLY`. `ranked`는 대기 분 → 이름 → id 순. `unranked`는 이름 순. 이동 시간 칸은 "이동 시간 모름" |
| 개수 | 순위 목록은 10곳까지 (제휴 시연 병원은 5곳) |

- 대기 시간이 이동 중에 줄어드는 효과는 넣지 않는다. 합계는 보수적인 값이고 설명하기 쉽다.
- 1위에 "가장 빠름" 표시.

### 3. 차 앱의 Firebase 접근: Firestore REST + Identity Toolkit REST (추천)

| 항목 | A. Firebase Android SDK (google-services 없이 `FirebaseOptions` 수동 초기화) | **B. REST (고름)** |
|---|---|---|
| 의존성 | firebase BOM + firestore + auth (gRPC·protobuf 포함, APK가 커진다). Kotlin 2.0.0·AGP 8.8.2와 맞는 BOM 버전을 찾아야 한다. Pleos AAR(Hilt 등)과 충돌 가능성 | **런타임 의존성 없음**: `HttpURLConnection`, Android 내장 `org.json`, 코루틴. 테스트에만 `org.json:json` 추가 (JVM에서는 Android `org.json`이 동작하지 않는다, 009 있으면 좋음 A와 같은 이유) |
| Pleos 기기에서 동작 | Pleos Connect에 Google Play 서비스가 있는지 모른다. Auth SDK가 Play 서비스에 기대는 부분이 있을 수 있다 → **미확인 위험** | HTTP만 쓴다. 위험이 적다 |
| 실시간 | 스냅샷 리스너로 바로 받는다 | 없음. 목록 화면이 보이는 동안 30초마다 다시 받는다 + [새로 고침]. 목록은 3~5곳이고 정차 중 잠깐 보는 화면이라 충분하다 |
| 에뮬레이터 연결 | `useEmulator("10.0.2.2", 8080)` | 기본 주소만 바꾼다: `http://10.0.2.2:8080/v1/projects/<id>/databases/(default)/documents/...`, Auth `http://10.0.2.2:9099/identitytoolkit.googleapis.com/v1/accounts:signUp?key=...` |
| 단위 테스트 | SDK 클래스는 JVM에서 못 돈다. 경계 인터페이스 + 가짜가 따로 필요 | JSON 변환·URL·인증 토큰 재사용·오류 처리 전부 JVM 테스트 (가짜 `HttpTransport`) |
| 설정 없을 때 | `initializeApp`을 부르지 않게 분기 | `serverConfigOf(...)`가 null → 서버 구현을 만들지 않는다 |

- 고른 이유: 이틀 안에 끝내야 하고, Pleos 기기의 Play 서비스 여부를 확인할 시간이 없다. 실시간이 꼭 필요한 쪽(병원 웹)은 웹 SDK를 쓴다.
- 층 (`data/server/`, 011의 "경계 + SDK 없는 해석 층"과 같은 생각):

| 파일 | 하는 일 | `android` | 확인 |
|---|---|---|---|
| `ServerConfig.kt` | `serverConfigOf(projectId, apiKey, emulatorHost, isDebugBuild): ServerConfig?` (순수) | 없음 | JVM |
| `HttpTransport.kt` | `fun interface HttpTransport { suspend fun send(request: HttpRequest): HttpResponse }` | 없음 | 컴파일 |
| `UrlConnectionHttpTransport.kt` | `HttpURLConnection` 어댑터. `Dispatchers.IO`, 연결·읽기 시간 초과 5초. **로그 없음** | 없음 (`java.net`) | 에뮬레이터 |
| `AnonymousAuth.kt` | `accounts:signUp`으로 익명 계정 → `idToken`·`localId`를 프로세스 동안 재사용. 한 번에 하나(`Mutex`) | 없음 | JVM |
| `FirestoreJson.kt` | Firestore REST 값 형식(`stringValue`, `integerValue`, `doubleValue`, `booleanValue`, `nullValue`, `timestampValue`, `mapValue`, `arrayValue`) ↔ 도메인. `org.json` | 없음 | JVM |
| `FirestoreHospitalBoardSource.kt` | `GET .../documents/hospitals` → `HospitalBoard` | 없음 | JVM |
| `FirestoreCaseSender.kt` | `POST .../hospitals/{id}/cases`, `PATCH ...?updateMask.fieldPaths=...` | 없음 | JVM |

- `ServerConfig` 규칙:
  - `projectId`가 비면 null (서버 없음).
  - debug이고 `emulatorHost`가 있으면 에뮬레이터 주소 (`http://<host>:8080`, `http://<host>:9099/identitytoolkit.googleapis.com`). API 키가 비어 있으면 `demo-key`를 쓴다 (에뮬레이터는 키를 검사하지 않는 것으로 안다 → 구현 중 확인 F2).
  - **release는 `emulatorHost`를 무시한다** (평문 HTTP 금지, ux-guidelines "HTTPS·TLS 1.2+"). 운영 주소 `https://firestore.googleapis.com/v1/...`, `https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=`. API 키가 비면 null.
  - release `buildTypes`에서도 `FIREBASE_EMULATOR_HOST`를 빈 값으로 덮는다 (이중 보호).
- `secrets.properties` 새 키 (`secrets.properties.example`에 빈 값으로): `FIREBASE_PROJECT_ID`, `FIREBASE_API_KEY`, `FIREBASE_EMULATOR_HOST` (예: `10.0.2.2`). BuildConfig로 넣는다. 없으면 빈 값 (AGENTS.md 아키텍처 규칙).
- 평문 HTTP는 debug에서 에뮬레이터 주소에만 허용: `app/src/debug/res/xml/network_security_config.xml`(`10.0.2.2`, `127.0.0.1`, `localhost`) + `app/src/debug/AndroidManifest.xml`의 `android:networkSecurityConfig`. release에는 없다 (완료 기준 7).
  - `127.0.0.1`은 Pleos 에뮬레이터에서 `10.0.2.2`가 안 될 때의 대안이다: `adb reverse tcp:8080 tcp:8080`, `adb reverse tcp:9099 tcp:9099` (관찰 O1).
- REST에서 주의할 값 형식 (JSON 변환 테스트에 넣는다):
  - 빈 배열은 `values` 없는 `arrayValue: {}`로 올 수 있다.
  - 정수처럼 보이는 좌표(예: 127)는 `integerValue`(문자열)로 올 수 있다. 숫자 필드는 `doubleValue`와 `integerValue`를 모두 받는다.
- 토큰은 약 1시간 유효로 안다. 갱신은 있으면 좋음 C다. 그 전에는 안내 중 토큰이 만료되면 도착 예정 갱신이 실패하고, 로그만 남긴다 (알려진 한계).

### 4. 데이터 모드: 병원 목록 출처와 카드 전송

`di/HospitalPlan.kt`의 순수 함수 `hospitalPlanOf(mode: DataMode, hasServer: Boolean): HospitalPlan`이 정한다. **`SourcePlan`·`NaviPlan`·`LocationPlan`은 바꾸지 않는다** (007·010·011 테스트와 같은 이유: data class 동등성, 다른 질문). 현재 위치는 011 그대로다.

| 모드 | 차량 기기 | 현재 위치 (011) | 병원 목록 | 카드 전송 | 정차 화면 표시 |
|---|---|---|---|---|---|
| Fake | 상관없음 | Fake | **내장 시연 병원** | **Fake** (메모리에 남기고 성공) | "시연용 병원 정보예요" + 전송 줄 "시연용: 실제 병원에는 보내지 않았어요" |
| 시연 (release) | 예 | 내비 | 서버 설정 있음: **서버 → 5초 안에 못 받거나 실패하면 내장**. 없음: 내장 | 목록이 서버에서 왔으면 서버, 내장이면 Fake | 서버: 병원마다 "시연용" 표시(가상 병원). 대체됨: "서버에 연결하지 못해 시연용 병원 정보를 보여 줘요" |
| 시연 | 아니오 | Fake | 위와 같다 | 위와 같다 | 위 + "시연용 위치(수정구 시흥동) 기준이에요" |
| Real | 예 | 내비 | **서버만**. 설정 없음·실패 → "병원 정보를 받지 못했어요" + [다시 시도] | 서버만. 실패 → "보내지 못했어요" + [다시 보내기] | 병원이 가상이면 "시연용" |
| Real | 아니오 | Fake (K3 그대로) | 서버만 | 서버만 | 위 + "시연용 위치 기준이에요" |

- **release(시연)는 네트워크 없이도 끝까지 돈다** (문의 A2): 설정이 없거나 서버가 안 되면 내장 시연 병원 + Fake 전송으로 같은 흐름이 이어진다.
- 대체 판단은 `data/hospital/FallbackHospitalBoardSource`(순수 Kotlin): 서버 `load()`를 `withTimeoutOrNull(5.seconds)`로 감싸고, 시간 초과·`Unavailable`이면 내장 목록을 `origin = BUILT_IN_FALLBACK`으로 준다. 시간 초과 5초는 011 위치와 같은 값이고, 단계 13에서 잰 시간의 3배가 넘으면 올린다.
- 전송 구현은 **목록이 어디서 왔는지**로 고른다 (`HospitalCandidate.origin`). 서버에서 온 병원만 서버로 보낸다. 대체된 내장 병원에 서버 전송을 시도하지 않는다.
- K4 (011에서 넘어온 것): Fake 위치로 계산한 목록에는 "시연용 위치(수정구 시흥동) 기준이에요"를 보인다. 기준은 `container.locationKind == FAKE`.
- 내장 시연 병원 = `docs/data/hospitals.sample.json`과 같은 값 (`SampleHospitalsJsonTest`가 대조). `updatedAt`은 받을 때마다 `now - seedUpdatedMinutesAgo`로 만든다 (그래서 Fake에서도 "오래됨" 병원이 늘 오래됨이다).
- 출품 유의사항(사실과 다르면 선정 취소) 때문에 시연용 표시를 숨기지 않는다. 주행 화면에는 넣지 않는다 (규칙 3).

### 5. 공유 데이터 형식 (`docs/data/`, 웹·차 앱·시드가 같이 따른다)

pet-profile.md와 같은 규칙: 코드가 아니라 문서가 기준, `schemaVersion: 1`, 모르는 필드는 무시, **읽는 쪽은 짐작하지 않는다** (형식이 틀린 값은 "모름"으로).

**`docs/data/hospital-status.md` + `docs/data/hospitals.sample.json`** — 문서 `hospitals/{hospitalId}`

| 필드 | 타입 | 뜻 | 쓰는 쪽 제한 |
|---|---|---|---|
| `schemaVersion` | 숫자 | 1 | 정확히 1. 다르면 그 병원을 건너뛴다 |
| `name` | 문자열 | 병원 이름 | 1~40자. 비면 그 병원을 건너뛴다 |
| `phone` | 문자열 또는 `null` | 전화번호 | 없으면 [병원에 전화]를 숨긴다 |
| `latitude`, `longitude` | 숫자 | 좌표 (NaviHelper `RouteInfo`에 그대로 넣는 값) | 범위 밖·NaN이면 그 병원을 건너뛴다 |
| `address` | 문자열 또는 `null` | 주소 | 화면에 보이지 않는다 |
| `isSample` | 불리언 | 가상 시연 병원 | 시드는 모두 `true` |
| `status` | 객체 또는 `null` | 병원이 알린 상태. `null` = 아직 알리지 않음 | 병원 웹만 쓴다 |
| `status.waitMinutes` | 정수 | 지금 오면 예상 대기 분 | 0~600 |
| `status.congestion` | `"low"`·`"normal"`·`"high"` | 혼잡도 (화면 "여유/보통/혼잡") | |
| `status.acceptingEmergency` | 불리언 | 응급 접수를 받는지 | |
| `status.updatedAt` | 타임스탬프 (서버 시각) | 마지막으로 알린 시각 | 웹이 `serverTimestamp()`로 쓴다. 규칙이 `request.time`과 같은지 본다 |

- 읽는 쪽: `status`의 필드가 하나라도 없거나 범위·값이 틀리면 **`status` 전체를 "대기 정보 없음"**으로 본다.
- 예시 JSON은 Firestore REST 형식이 아니라 논리 형식이다. 시드용 값 `seedUpdatedMinutesAgo`와 `id`를 문서 밖에 둔다: `{ "hospitals": [ { "id": "...", "seedUpdatedMinutesAgo": 2, "document": { ... } } ] }`.
- 시연 병원 5곳 (기본안. 이름·역할은 K6. **좌표는 단계 1에서 지도로 도로 위 지점인지 확인해 고치고**, 단계 13에서 내비 경로가 나오는지 본다):

| id | 이름 | 시연 위치에서 직선 | 대기 | 혼잡 | 응급 | 알린 지 | 기대 순위 (추정) |
|---|---|---|---|---|---|---|---|
| `demo-fast` | 시연용 바른 동물병원 | 약 5km (예: 37.4450, 127.1300) | 10분 | 보통 | 받음 | 2분 | 1위 (약 18+10=28분) |
| `demo-main` | 시연용 동물병원 (기존, 010 좌표) | 약 9.6km (37.48544722, 127.03636666) | 5분 | 여유 | 받음 | 5분 | 2위 (약 35+5=40분) |
| `demo-near` | 시연용 가까운 동물병원 | 약 2km (예: 37.4300, 127.1050) | 60분 | 혼잡 | 받음 | 3분 | 3위 (약 8+60=68분) |
| `demo-stale` | 시연용 정보 지난 동물병원 | 약 6km (예: 37.3700, 127.0600) | 5분 | 여유 | 받음 | 90분 | 오래됨 묶음 |
| `demo-closed` | 시연용 응급 휴진 동물병원 | 약 3km (예: 37.3950, 127.1150) | 0분 | 보통 | **안 받음** | 10분 | 응급 접수 안 함 |

  - "가장 가까운 곳이 가장 빠른 곳은 아니다"를 한 화면에서 보이도록 짰다.
  - 전화번호는 `02-0000-0000`(기존 `demo-main`)~`02-0000-0004`. 009와 같이 걸리지 않는 형식이다.
  - 내비가 "너무 가깝다"(`CAUSE_SEARCH_ROUTE_TOO_SHORT_DISTANCE`)를 내지 않게 1.5km보다 멀리 둔다.

**`docs/data/emergency-case.md` + `docs/data/emergency-case.example.json`** — 문서 `hospitals/{hospitalId}/cases/{caseId}` (차가 만든다)

| 필드 | 타입 | 뜻 | 쓰는 쪽 |
|---|---|---|---|
| `schemaVersion` | 숫자 | 1 | 차 |
| `createdBy` | 문자열 | 보낸 차의 익명 uid | 차 (규칙이 확인) |
| `createdAt` | 타임스탬프 | 보낸 시각 (차 시계) | 차 |
| `status` | `"en_route"`·`"arrived"`·`"cancelled"` | 오는 중 / 도착 / 취소 | 차 |
| `remainingMinutes` | 정수 또는 `null` | 남은 분 (화면과 같은 올림, 최소 1). `null` = 경로를 찾는 중 | 차 |
| `etaAt` | 타임스탬프 또는 `null` | 도착 예정 시각 = 보낸 순간 + 남은 시간 (차 시계) | 차 |
| `etaUpdatedAt` | 타임스탬프 | 남은 시간을 마지막으로 보낸 시각 (차 시계) | 차 |
| `card.pet` | 객체 또는 `null` | pet-profile v1의 `name`, `breed`, `sex`, `birthDate`, `birthDateIsEstimated`, `medical`, `updatedAt`. **`photoDataUrl`, `schemaVersion`은 넣지 않는다.** 프로필을 못 받았으면 `null` | 차 |
| `card.symptoms` | `{ code, label }` 배열 | 고른 증상. `code`는 `Symptom` 이름(예: `COUGH`), `label`은 차 화면 글자(예: "기침"). 프리셋 순서 | 차 |
| `acknowledgedAt` | 타임스탬프 또는 `null` | 병원이 "확인함"을 누른 시각 (있으면 좋음 A) | 병원 웹 |

- **차의 위치는 보내지 않는다.** 병원에는 도착 예정만 필요하다.
- 시계 차이: `etaAt`은 차 시계다. 차와 병원 PC의 시계가 다르면 그만큼 틀린다. 병원 웹은 "도착 예정 HH:mm · 약 N분 후"를 자기 시계로 계산한다. 알려진 한계로 문서에 적는다.
- 웹은 `card.pet`을 pet-profile 읽기 규칙(나이 계산 포함)으로 보인다. `label`이 없으면 `code`를 보인다.

### 6. 보안·개인정보

**인증**: 차와 병원 웹 모두 Firebase **익명 로그인**. 규칙은 "로그인했는가"만이 아니라 "누구의 문서인가"로 막는다. 진짜 프로젝트에서는 콘솔에서 익명 로그인을 켠다 (있으면 좋음 B).

**병원 웹 접근 (U1, 추천 B "병원별 시연 코드")**: 병원 웹 링크에 코드를 넣는다 (`/hospital/?h=demo-fast&code=…`). 화면에 로그인 단계는 없다. 코드는 `hospitalSecrets/{id}`(클라이언트가 읽을 수 없음)에만 있고, 맞으면 그 uid를 `hospitals/{id}/staff/{uid}`에 등록한다. 등록된 uid만 그 병원의 상태를 바꾸고 환자 카드를 읽는다.

**Firestore 규칙 기본안 (`firebase/firestore.rules`)** — 규칙 없는 테스트 모드로 열지 않는다:

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    function signedIn() { return request.auth != null; }
    function isStaff(hid) {
      return signedIn() && exists(/databases/$(database)/documents/hospitals/$(hid)/staff/$(request.auth.uid));
    }
    function validStatus(s) {
      return s.keys().hasOnly(['waitMinutes', 'congestion', 'acceptingEmergency', 'updatedAt'])
        && s.waitMinutes is int && s.waitMinutes >= 0 && s.waitMinutes <= 600
        && s.congestion in ['low', 'normal', 'high']
        && s.acceptingEmergency is bool
        && s.updatedAt == request.time;
    }

    match /hospitals/{hid} {
      allow read: if signedIn();
      allow update: if isStaff(hid)
        && request.resource.data.diff(resource.data).affectedKeys().hasOnly(['status'])
        && validStatus(request.resource.data.status);
      allow create, delete: if false;          // 병원은 시드(관리자)만 만든다

      match /staff/{uid} {
        allow create: if signedIn() && uid == request.auth.uid
          && request.resource.data.keys().hasOnly(['joinCode'])
          && request.resource.data.joinCode
             == get(/databases/$(database)/documents/hospitalSecrets/$(hid)).data.joinCode;
        allow read: if signedIn() && uid == request.auth.uid;
        allow update, delete: if false;
      }

      match /cases/{cid} {
        allow create: if signedIn()
          && request.resource.data.createdBy == request.auth.uid
          && request.resource.data.schemaVersion == 1
          && request.resource.data.status == 'en_route'
          && request.resource.data.card.symptoms.size() <= 12;   // + 키 목록 hasOnly, 타입 검사
        allow read: if signedIn() && (resource.data.createdBy == request.auth.uid || isStaff(hid));
        allow update: if signedIn() && (
          (resource.data.createdBy == request.auth.uid
            && request.resource.data.diff(resource.data).affectedKeys()
               .hasOnly(['status', 'remainingMinutes', 'etaAt', 'etaUpdatedAt'])
            && request.resource.data.status in ['en_route', 'arrived', 'cancelled'])
          || (isStaff(hid)
            && request.resource.data.diff(resource.data).affectedKeys().hasOnly(['acknowledgedAt'])));
        allow delete: if isStaff(hid) || (signedIn() && resource.data.createdBy == request.auth.uid);
      }
    }

    match /hospitalSecrets/{hid} { allow read, write: if false; }
  }
}
```

- 규칙 점검 R1~R8 (`firebase/scripts/check-rules.mjs`, 의존성 없는 Node 스크립트. 에뮬레이터 Auth로 익명 계정 둘을 만들어 해 본다):
  - R1 로그인 안 한 요청은 병원 목록을 읽지 못한다
  - R2 익명 계정은 병원 목록을 읽는다
  - R3 직원 등록 안 한 계정은 병원 상태를 바꾸지 못한다
  - R4 틀린 코드로는 직원 등록이 안 되고, 맞는 코드로는 된다
  - R5 직원은 상태를 바꾸지만 `waitMinutes: 601`·이름 바꾸기는 거부된다
  - R6 차 계정은 자기 환자 건을 만들고 남은 시간을 바꾼다. `createdBy`가 남이면 거부된다
  - R7 다른 차 계정은 남의 환자 건을 읽지 못한다. 그 병원 직원은 읽는다
  - R8 `hospitalSecrets`는 누구도 읽지 못한다
- **확인 화면 안내 문구 (U2·U3, 기본안)**:
  - 제목: "{병원}으로 출발할까요?"
  - "출발하면 이 병원에 진료 카드와 도착 예정 시간이 전달돼요."
  - "보내는 것: 반려견 이름·품종·성별·나이, 고른 증상, 기존 병력, 알레르기, 복용 약, 도착 예정 시간"
  - "보내지 않는 것: 사진, 차의 위치"
  - "병원 화면에서 진료를 마치고 [완료]를 누르면 지워져요." (단계 8에서 [완료]를 만들기 때문에 사실이다)
  - Fake·대체 병원이면 첫 줄 대신 "시연용이라 실제 병원에는 보내지 않아요."
  - 버튼: [뒤로] / [병원에 전화] / [카드 없이 길안내만] (U2) / **[출발]** (전송 + 길안내)
  - ux-guidelines "동의: … 제시된 동의 버튼을 누르는 행동 외의 행동을 동의로 간주해서는 안 됩니다" → [출발]을 누르는 것이 이번 전송에 대한 동의다. 매번 보인다.
- **로그**: 태그 `HospitalTrip` 하나로, 한 일마다 한 줄. 결과 종류·개수·걸린 시간·HTTP 상태만 남긴다.
  - 예: `병원 목록: 서버 5곳 (812ms)`, `병원 목록: 내장 시연 (서버 시간 초과)`, `카드 전송: 성공`, `도착 예정 갱신: 실패 (HTTP 403)`
  - 좌표, 주소, 반려견 이름, 증상, 토큰, uid, 응답 본문은 남기지 않는다. 어댑터(`UrlConnectionHttpTransport`)는 로그를 남기지 않는다.
  - 웹은 `console.log` 0건 (008과 같다).
- **보관**: Functions가 없어 자동 삭제가 없다. 병원 웹 [완료]가 지운다. 차는 취소·도착을 상태로 알리고 지우지는 않는다 (병원이 기록을 봐야 한다). 에뮬레이터는 `seed --reset`이 비운다. 진짜 프로젝트의 남은 데이터 정리와 개인정보처리방침 URL은 architecture.md "Firebase로 옮길 때 할 일"에 더한다.
- **통신**: release는 HTTPS만 (설계 결정 3). 비밀값: `secrets.properties`(차), `web/.env.local`(웹, `VITE_`는 번들에 들어가 공개된다는 점을 문서에), 진짜 프로젝트의 병원 코드는 커밋하지 않는다 (`firebase/seed/join-codes.local.json`, gitignore).

### 7. 병원 웹 화면 (`web/` 안, `/hospital/`)

- 위치: Vite 다중 페이지. `web/hospital/index.html` → URL `/hospital/`, 진입점 `web/src/hospital/main.tsx`.
  - 라우터 라이브러리를 넣지 않는다 (008 결정 유지). 보호자 페이지 번들에 Firebase가 들어가지 않는다.
  - Vite 8의 다중 입력 설정 이름(`build.rollupOptions.input` 또는 바뀐 이름)은 구현 때 Vite 문서로 확인한다 (F6).
- 층 (008과 같은 의존 방향):

```
web/src/domain/hospital/   hospitalStatus.ts·emergencyCase.ts(타입), *Json.ts(형식 문서와 1:1 읽기·쓰기),
                           etaText.ts(도착 예정 문구), caseOrder.ts(목록 순서), statusForm.ts(입력 값 규칙),
                           hospitalDesk.ts(저장소 인터페이스)
web/src/data/              memoryHospitalDesk.ts(Fake), firestoreHospitalDesk.ts(Firebase JS SDK), sampleHospitals.ts
web/src/hospital/          main.tsx, container.ts(설정 있으면 Firestore, 없으면 메모리)
web/src/ui/hospital/       화면 (domain만 import)
```

```ts
// web/src/domain/hospital/hospitalDesk.ts (기본안)
export interface HospitalDesk {
  listHospitals(): Promise<Result<HospitalSummary[]>>
  open(hospitalId: string, joinCode: string | null): Promise<Result<void>>   // 메모리 구현은 코드를 보지 않는다
  watchHospital(hospitalId: string, onChange: (h: HospitalView) => void): () => void
  watchCases(hospitalId: string, onChange: (cases: CaseView[]) => void): () => void
  updateStatus(hospitalId: string, input: StatusInput): Promise<Result<void>>
  completeCase(hospitalId: string, caseId: string): Promise<Result<void>>    // [완료] = 지우기
}
```

- 화면 (큰 버튼, 병원 PC·태블릿 기준. 960px 이상은 왼쪽 상태 입력 · 오른쪽 환자 목록, 좁으면 한 줄):
  1. **병원 고르기** (`?h`가 없을 때): 시연 병원 목록을 큰 버튼으로. 고르면 `?h=<id>`. 서버 모드에서 링크에 코드가 없으면 "병원용 링크(코드 포함)로 열어 주세요".
  2. **상태 입력**: 예상 대기 [0] [10] [20] [30] [45] [60] [90] [120]분 + [−5] [+5] (0~600), 혼잡도 [여유] [보통] [혼잡], 응급 접수 [받음] [안 받음], 큰 버튼 **[병원 상태 알리기]**. 아래에 "마지막으로 알림 14:02 (3분 전)". 30분이 지나면 "차 앱에서 '대기 정보 지남'으로 보여요. 다시 알려 주세요."
  3. **들어온 환자 목록** (실시간): 도착 예정이 빠른 순, 취소는 맨 아래. 카드마다 반려견 이름·성별·나이·품종, 증상(강조), 병력·알레르기·복용 약, "도착 예정 14:32 · 약 12분 후"(30초마다 다시 계산), 상태 표시(오는 중 / 도착했어요 / 취소됨), [완료](지우기, "목록에서 지울까요?" 확인). 비었으면 "아직 들어온 환자가 없어요".
  4. (있으면 좋음 A) [확인함] → 차 카드에 "병원이 카드를 확인했어요".
- 항상 보이는 안내: "시연용 화면이에요. 실제 병원이 아니에요." 메모리 모드(Firebase 설정 없음)에서는 "서버 없이 이 브라우저에서만 동작해요. 차 앱의 카드는 들어오지 않아요."를 더한다.
- 메모리 모드는 시연 병원 5곳 + **예시 환자 1건**(emergency-case.example.json과 같은 값, "예시" 표시, `etaAt = 지금 + 12분`)으로 시작한다. 웹만으로 화면을 찍고 확인할 수 있게 한다.
- 화면 확인 스크립트(`verify_screens.py`) 새 시나리오 H01~H05 (메모리 모드, 날짜 고정):
  - H01 병원 고르기 → H02 상태 입력(30분·혼잡·안 받음 → [알리기] → "방금 알림") → H03 예시 환자 카드 → H04 [완료] → 빈 목록 → H05 넓은 화면 두 칸 배치.
  - Firebase 설정이 든 `web/.env.local`이 있으면 빌드에 들어가 "밖으로 가는 요청"으로 실패한다. 화면 확인용 빌드는 `.env.local`을 무시하는 방법(예: 빈 값을 넣은 `web/.env.verify` + `--mode verify`)을 단계 9에서 정하고 web/README.md에 적는다.

### 8. 차 앱 화면 흐름

정차 화면 2단계 표(architecture.md)에 **병원 찾기 단계**를 더한다. `parkedContentOf`에 기본값 있는 인자 `finder: FinderStep = FinderStep.NONE`을 더하고, `ParkedContent`에 `HOSPITALS`, `CONFIRM_HOSPITAL`을 더한다. 1단계 규칙(주행·모름이면 주행 화면)은 그대로다.

1. **진료 카드 (안내 없음)**: 아래 큰 버튼이 [길 안내 시작] 대신 **[가장 빨리 진료받을 곳 찾기]**. 병원을 고르기 전이라 [병원에 전화]는 숨긴다 (K5).
2. **병원 목록 (정차 중에만)**: 열 때 현재 위치를 한 번 묻고(011 `CurrentLocationSource`) 병원 목록을 받는다 (둘 다 5초 시간 초과, 동시에).
   - 받는 동안 "가까운 제휴 병원의 대기 시간을 받는 중…".
   - 줄마다: 순위, 병원 이름, 큰 글씨 합계 "약 28분", 작은 글씨 "이동 약 18분 + 대기 10분 · 보통 · 2분 전 알림", 1위 "가장 빠름", 가상 병원 "시연용". 줄 전체가 하나의 큰 터치 타깃 (높이 `scale.buttonHeight` 이상).
   - 아래 묶음 "대기 정보가 없거나 지난 곳": "이동 약 21분 · 대기 정보 90분 전". 고를 수 있다.
   - "지금 응급 접수를 받지 않는 곳: …" 이름만.
   - 위 안내 (작은 글씨): 출처(설계 결정 4), "이동 시간은 직선 거리로 어림한 값이에요", Fake 위치면 "시연용 위치(수정구 시흥동) 기준이에요".
   - **위치를 모를 때**: "현재 위치를 아직 몰라 대기 시간이 짧은 순으로 보여 줘요." + "차량 내비게이션 화면을 한 번 연 뒤 다시 찾아 주세요." + [위치 다시 찾기]. 줄에는 "이동 시간 모름 · 대기 10분".
   - 버튼: [뒤로](카드로), [새로 고침]. 화면이 보이는 동안 30초마다 목록만 다시 받는다 (위치는 다시 묻지 않는다). 화면이 사라지면 멈춘다 (`stateIn(WhileSubscribed)`, ux-guidelines "폴링 최소화").
   - Real 모드 실패: "병원 정보를 받지 못했어요" + [다시 시도].
   - 텍스트 입력 없음. 목록은 정차 중이라 스크롤해도 된다.
3. **확인 화면**: 병원 이름, "약 28분 (이동 약 18분 + 대기 10분)", "대기 정보: 2분 전 · 보통", 설계 결정 6의 안내 문구, 버튼 [뒤로] [병원에 전화] [카드 없이 길안내만] [출발].
4. **[출발]**: `HospitalTrip.depart(...)`가 (1) 고른 병원을 기억하고 (2) 그 병원으로 길안내를 시작하고 (3) 카드와 도착 예정(목록의 이동 추정)을 뒤에서 보낸다. 전송이 실패해도 길안내는 계속된다. 두 번 눌러도 한 번만 출발한다.
   - 차량 기기에서는 010처럼 내비 앱이 앞에 나온다. 전송은 앱 범위 코루틴에서 돈다.
5. **카드 (안내 중)**: 머리 "병원까지 N분 · {고른 병원}" + 전송 줄: "진료 카드를 병원에 보냈어요" / "보내는 중…" / "보내지 못했어요" + [다시 보내기] / "시연용: 실제 병원에는 보내지 않았어요" / "카드 없이 출발했어요". [병원에 전화]는 **고른 병원 번호**. [안내 취소]는 그대로.
6. **주행 화면**: 바꾸지 않는다. 이미 "병원까지 N분" + `destinationName`(= 고른 병원)이다. 전송 상태 줄도 넣지 않는다 (규칙 3).
7. **주행 중 갱신**: `HospitalTrip`이 길안내 상태를 보고 남은 분(화면과 같은 올림)이 바뀌었고 마지막으로 보낸 지 60초가 지났으면 `remainingMinutes`·`etaAt`을 보낸다. 도착하면 `arrived`, 안내 중에 취소되면(앱 [안내 취소], 내비 "안내 종료") `cancelled`. 도착 화면을 닫으면 고른 병원을 비운다 (상태는 보내지 않는다). 실패는 로그만 남기고 다음 변화 때 다시 보낸다 (ux-guidelines "주행 중 네트워크 끊김 대비").
   - 시연 모드(차량 기기)의 남은 시간은 010처럼 내비 예상 시간에서 시작해 시연용으로 준다. 그래서 병원에 가는 남은 시간도 시연용이다. 화면의 기존 시연 문구가 이를 덮는다.
8. **Fake 길안내의 처음 남은 시간** (단계 6-2, 잘라낼 수 있음): 목록의 이동 추정에서 시작한다. 우선순위: 내비 예상 시간 > 목록 추정 > 기본 12분.
   - `Destination`에 기본값 있는 `expectedTravel: Duration? = null`을 더한다 (내비는 무시한다).
   - `FakeGuidanceSource`에 기본값 있는 생성자 인자 `initialRemainingFor: (Destination) -> Duration? = { null }`을 더한다. 기본값이면 지금과 똑같다.
   - `AppContainer`: 안내 없음(NaviUse.NONE)이면 `{ it.expectedTravel }`, 경로만 내비(ROUTE_ONLY)의 `fakeOf(initial)`에서는 `initial`이 null일 때만 `{ it.expectedTravel }`. `NaviRouteOnlyGuidanceSource`는 고치지 않는다.

- 상태가 사는 곳: `domain/trip/HospitalTrip`(고른 병원, 전송 상태)과 `FinderStep`은 `AppContainer`의 메모리 객체다 (`MedicalCardSession`과 같은 수명). 목록 화면의 받은 값은 `HospitalFinderViewModel`이 든다.
- 기존 코드와 테스트를 지키는 법:
  - `GuidanceViewModel.onStartGuidance()`, `SampleHospitals.demo`, `GuidanceSource`는 그대로 둔다. 출발은 `HospitalTrip`이 `GuidanceSource.start`를 직접 부른다.
  - `MedicalCardViewModel`에 새 인자를 **맨 뒤에 기본값과 함께** 더한다 (`selectedHospital: StateFlow<Hospital?>` 등). `MedicalCardTripFlowTest`(96행)가 위치 인자로 부른다.
  - `CardFlowUiState`·`Hospital`(`id: String? = null`)·`Destination`에는 기본값 있는 필드만 더한다.
  - 도착 카드 [닫기]는 지금 동작에 `trip.clear()`를 더한다.

### 9. Firebase 에뮬레이터 준비

- 준비물:
  - Node 22.12 이상 (이미 있다), **Java**: Firestore 에뮬레이터가 Java로 돈다. firebase-tools 최신 버전이 요구하는 Java 버전을 설치 때 확인한다 (F1). JDK 17로 안 되면 (a) 요구 버전 JDK를 따로 깔고 에뮬레이터를 켤 때만 `JAVA_HOME`을 바꾸거나 (b) JDK 17을 받는 firebase-tools 버전으로 고정한다. 결정을 firebase/README.md에 적는다.
  - firebase-tools: **`firebase/package.json`의 devDependency로 정확한 버전 고정** (`save-exact`, `web/.npmrc`와 같은 설정). 전역 설치에 기대지 않는다.
- 파일 (`firebase/` 폴더. 저장소 루트를 어지럽히지 않는다):

```
firebase/
  package.json            firebase-tools 고정. scripts: emulators, seed, check-rules
  .npmrc                  save-exact=true, engine-strict=true
  firebase.json           firestore rules·indexes 경로, emulators: auth 9099, firestore 8080, ui 4000
  .firebaserc             { "projects": { "default": "demo-meongcare" } }  (demo- 접두사 = 에뮬레이터 전용 가짜 프로젝트)
  firestore.rules         설계 결정 6
  firestore.indexes.json  (비어 있음)
  seed/join-codes.emulator.json   에뮬레이터용 병원 코드 (가짜 값, 커밋함)
  scripts/seed.mjs        docs/data/hospitals.sample.json + 코드 → 에뮬레이터 REST로 넣는다. --reset이면 먼저 비운다
  scripts/check-rules.mjs R1~R8
  README.md               준비물, 명령, 링크(병원별 코드 포함 URL), 진짜 프로젝트로 바꾸는 법
```

- 명령 (저장소 루트):

| 목적 | 명령 |
|---|---|
| 처음 한 번 | `npm --prefix firebase ci` |
| 에뮬레이터 켜기 | `npm --prefix firebase run emulators` (→ `firebase emulators:start --project demo-meongcare --only auth,firestore`) |
| 시드 | `npm --prefix firebase run seed` (`-- --reset`으로 비우고 다시) |
| 규칙 점검 | `npm --prefix firebase run check-rules` |
| 병원 웹 (에뮬레이터 연결) | `web/.env.local`에 `VITE_FIREBASE_PROJECT_ID=demo-meongcare`, `VITE_FIREBASE_API_KEY=demo-key`, `VITE_FIREBASE_EMULATOR_HOST=127.0.0.1` → `npm --prefix web run dev` → `http://localhost:5173/hospital/?h=demo-fast&code=<코드>` |
| 차 앱 (에뮬레이터 연결) | `secrets.properties`에 `FIREBASE_PROJECT_ID=demo-meongcare`, `FIREBASE_API_KEY=demo-key`, `FIREBASE_EMULATOR_HOST=10.0.2.2` → debug 빌드 |

- 시드는 에뮬레이터 REST에 관리자 토큰(`Authorization: Bearer owner`)으로 넣는다. 이 방식과 데이터 비우기 주소는 구현 때 Firebase 문서로 확인한다 (F3·F5). 안 되면 Emulator UI로 한 번 넣고 `--export-on-exit`로 만든 데이터를 `--import`한다.
- `.gitignore`에 더한다: `firebase/node_modules/`, `firebase/.emulator-data/`, `firebase-debug.log`, `firestore-debug.log`, `ui-debug.log`, `firebase/seed/join-codes.local.json`.
- **검증 파이프라인·CI 영향: 없다.**
  - `build-gate.mjs`의 감시 확장자(kt·kts·java·xml·gradle·toml·pro·properties)에 `firebase/`의 파일(json·rules·mjs)이 걸리지 않는다. 커밋 직전 `assembleDebug`도 돌지 않는다.
  - 파이프라인은 `firebase/`를 검사하지 않는다. 규칙 점검은 Java와 에뮬레이터가 필요해서 CI에 넣지 않는다 (있으면 좋음 F에서 다시 본다).
  - `web/package.json`에 `firebase`가 더해지므로 CI `npm ci`가 그것도 받는다. 기존 `web.yml` 그대로 된다.
  - `docs/data/`가 바뀌면 파이프라인이 웹 검사를 다시 돈다 (지금과 같다).

### 10. 권한

- **새 `pleos.car.permission.*`: 없다.** 쓰는 Pleos API는 이미 승인·선언된 것뿐이다:
  - `requestRoute` (`NAVI_ROUTE_SEARCH`, 010) — 고른 병원 좌표로
  - `getCurrentLocationInfo` (`NAVI_ROUTE`, 011) — 순위의 기준점
- **일반 Android 권한 (pleos 아님, 규칙 2 대상 아님, 알림)**: `android.permission.INTERNET`을 더한다. 일반(normal) 권한이라 런타임 요청이 없다. Pleos 앱 제출 폼의 "앱 접근 권한"에 적을 항목이 될 수 있다 (ux-guidelines "외부 서버·네트워크": 심사 중 서버 활성화, HTTPS).
- 구현 중 `SecurityException`이나 다른 pleos 권한이 필요하다는 근거가 보이면 **멈추고 묻는다.** 권한을 선언하지 않는다.

### 11. 차 앱 코드 구조

```
domain/hospital/
  Hospital.kt              (기존) + id: String? = null
  HospitalStatus.kt        Congestion, HospitalStatus(waitMinutes, congestion, acceptingEmergency, updatedAt)
  HospitalCandidate.kt     hospital + status? + origin(SERVER/BUILT_IN/BUILT_IN_FALLBACK) + canReceiveCard
  HospitalBoardSource.kt   suspend load(): HospitalBoard (Loaded / Unavailable(reason)). 취소 말고는 던지지 않는다
  TravelEstimate.kt        estimateTravel(from, to): Duration
  HospitalRanking.kt       rankHospitals (설계 결정 2)
domain/trip/
  CaseCard.kt              CaseCard(pet: PetProfile?, symptoms: List<Symptom>), CaseEta, CaseStatus, CaseRef
  CaseSender.kt            send / updateEta / finish (결과 값, 던지지 않음)
  HospitalTrip.kt          출발(길안내+전송), 남은 시간 → 갱신, 도착·취소 → 상태, 전송 상태 StateFlow, FinderStep
data/hospital/
  SampleHospitals.kt       (기존 demo 그대로) + partners (hospitals.sample.json과 같은 값)
  BuiltInHospitalBoardSource.kt, FallbackHospitalBoardSource.kt, FakeCaseSender.kt
data/server/               설계 결정 3의 표
di/
  HospitalPlan.kt          hospitalPlanOf(mode, hasServer)
  AppContainer.kt          serverConfig, hospitalBoardSource, caseSenderFor(origin), hospitalTrip, symptomLabel
ui/hospital/
  HospitalFinderViewModel.kt, HospitalFinderUiState.kt, HospitalTexts.kt(순수 문구 함수)
  HospitalListScreen.kt, HospitalConfirmScreen.kt
ui/home/                   ParkedContent(새 값 2개, 인자 1개), HomeScreen(연결)
ui/card/                   MedicalCardScreen(Idle 버튼, 전송 줄, 고른 병원 전화), MedicalCardViewModel(맨 뒤 인자)
```

- 증상 글자(`label`)는 `strings.xml`에 있다. `AppContainer`가 `symptomLabel: (Symptom) -> String`을 만들어 서버 전송 구현에 넘긴다 (도메인은 Android 리소스를 모른다).
- `HospitalTrip`의 시간은 주입한다 (`now: () -> Instant`, `scope`). 테스트는 가상 시간으로 돌린다.

## 확인 필요 (시작 전에 해결)

### [사용자 승인 필요] — 시작 전에

> **2026-11-06 사용자 결정**: U1 = **B (병원별 코드가 든 링크)**, U2 = **둔다**, U3 = **추천대로**(진료 카드: 반려견 정보·증상·병력·알레르기·복용 약·도착 예정은 보낸다. 사진과 차 위치는 보내지 않는다. 사용자가 "증상 같은 반려견 카드는 필요하다"고 강조함), U4 = **추가한다**.

- **U1. 병원 웹 접근 제한 방식** (사용자는 "로그인 없음"이라고 했다):
  - A. 익명 로그인한 누구나 병원 상태를 바꾸고 환자 카드를 읽는다 (필드 검증만). 가장 단순하지만 링크를 아는 누구나 의료정보를 본다.
  - **B. (추천) 병원별 시연 코드를 링크에 넣는다.** 로그인 화면은 없다. 코드가 맞는 브라우저만 그 병원의 상태 입력·환자 카드를 쓴다.
  - C. 병원 계정 이메일 로그인. 하루가 더 든다.
- **U2. 확인 화면에 [카드 없이 길안내만]을 둘지.** 사용자 결정("[출발]하면 자동 전송")은 그대로 두고, 보내지 않을 길을 하나 더 두는 것이다 (Pleos 개인정보 심사: 거부권·명시적 동의). 추천: 둔다.
- **U3. 보내는 카드 내용**: 반려견 정보(pet-profile 필드)·증상·도착 예정만 보내고 **사진과 차의 위치는 보내지 않는다.** 추천대로 할지.
- **U4. 웹 의존성 추가**: `web/package.json`에 `firebase`(Firebase JS SDK, 정확한 버전 고정). 008의 "의존성 6개뿐" 결정이 바뀐다. 그리고 새 npm 프로젝트 `firebase/`(firebase-tools 고정)를 둔다.

### [사용자 승인 필요] — 나중에 (해당 단계 전에)

- **U5. 11/15 제출 APK에 Firebase 설정을 넣을지** (있으면 좋음 B 전). 추천: 넣지 않는다 → 심사위원 환경에서는 내장 시연 병원으로 끝까지 돈다. 서버 흐름은 시연 영상(에뮬레이터 스위트)으로 보인다. 진짜 프로젝트가 준비되고 확인되면 다시 정한다.
- 알림 (승인 대상 아님): `android.permission.INTERNET` 추가 (설계 결정 10).

### [선행: pleos-docs-researcher]

- **필수 단계에는 없다.** 쓰는 Pleos API(`requestRoute`, `getCurrentLocationInfo`)는 sdk-notes에 확인돼 있다.
- 있으면 좋음 E에만 필요하다: "`requestTrip(TripRequest)`은 무엇을 하나? 길안내를 시작하지 않고 `onTripInfo(TripInfo)`로 거리·소요 시간을 주나? 내비 화면이 앞으로 나오나? 여러 목적지를 연달아 물어도 되나? 권한은 `NAVI_ROUTE`(권한표의 `requestTrip()`)뿐인가?" (sdk-notes "미확인": `requestTrip`의 동작 문서 없음)

### 구현 중 확인 (Firebase 공식 문서. Pleos가 아니라 규칙 1 대상은 아니지만 추측으로 쓰지 않는다)

| # | 확인할 것 | 안 되면 |
|---|---|---|
| F1 | firebase-tools 최신 버전이 요구하는 Java 버전 | 설계 결정 9의 (a)/(b) |
| F2 | Auth 에뮬레이터의 `accounts:signUp` REST 주소 형식, 아무 API 키나 받는지 | 주소 규칙만 `ServerConfig`에서 고친다 |
| F3 | Firestore 에뮬레이터에서 `Authorization: Bearer owner`로 규칙을 건너뛰어 쓸 수 있는지 | Emulator UI + `--import` |
| F4 | Spark 요금제에서 익명 로그인을 쓸 수 있는지 | 있으면 좋음 B에서 다시 정한다 |
| F5 | 에뮬레이터 데이터 비우기 REST 주소 | 에뮬레이터를 다시 켠다 |
| F6 | Vite 8의 다중 페이지 입력 설정 이름 | 문서대로 |
| F7 | Firestore REST에서 컬렉션 GET이 하위 컬렉션(`staff`, `cases`)을 포함하지 않는지 | 필드 마스크(`mask.fieldPaths`)를 붙인다 |

### 관찰로 확인 (단계 13)

| # | 볼 것 | 결과가 바꾸는 것 |
|---|---|---|
| O1 | Pleos 에뮬레이터 앱에서 호스트 PC의 에뮬레이터(`10.0.2.2:8080`, `:9099`)에 닿는가 | 안 되면 `adb reverse` + `127.0.0.1`. emulator-setup.md에 기록 |
| O2 | 시연 병원 5곳의 좌표로 내비 경로가 나오는가 (`requestRoute`) | 안 나오는 좌표는 고치고 hospitals.sample.json·Kotlin·시드를 같이 바꾼다 |
| O3 | 목록 받기에 걸리는 시간 (익명 가입 포함, 첫 번째·그다음) | 시간 초과 5초 (가장 긴 값의 3배가 넘으면 올린다) |
| O4 | 내비가 앞에 나온 동안(우리 앱 백그라운드) 카드 전송과 도착 예정 갱신이 되는가 | 안 되면 노트에 한계로 적는다 |

### 기획상 판단 (기본값으로 진행한다. 바꾸려면 사용자가 알려 준다)

- K1. 이동 시간은 직선 거리 어림값 + "약" 표시, 계수 1.25 · 21km/h (설계 결정 1).
- K2. 순위 규칙 (설계 결정 2): 오래됨 30분, 응급 접수 불가는 따로, 동점은 이동 시간 → 이름, 위치 모르면 대기 시간 순.
- K3. 위치를 모를 때 Fake 위치로 바꾸지 않고 "대기 시간 순"으로 보인다 (사실과 다른 위치로 계산하지 않는다).
- K4. 데이터 모드 표 (설계 결정 4): 시연은 서버 → 내장으로 대체, Real은 대체하지 않는다.
- K5. 병원을 고르기 전 카드에서는 [병원에 전화]를 숨긴다. 확인 화면에서 전화할 수 있다.
- K6. 시연 병원 5곳의 이름·역할 (설계 결정 5 표). 모두 가상이고 "시연용"을 붙인다.
- K7. 주행 화면은 바꾸지 않는다 (전송 상태 줄을 넣지 않는다).
- K8. 도착 예정 갱신은 남은 분이 바뀌었고 60초가 지났을 때만 (1분에 한 번 이하).
- K9. PT(12/03)에서 서버 흐름을 보일 빌드(debug 시연 모드로 할지 등)는 이번에 정하지 않는다. 노트의 남은 일로 둔다.

## 확인 절차

- adb 규칙은 011 "확인 절차"와 같다: `ANDROID_SERIAL`(Pleos `emulator-5554`, 일반 폰 `emulator-5556`), 기어는 006 노트의 `vc()`, 스크린샷·로그는 `.claude/.cache/p12-<번호>.*`(커밋하지 않음), 에이전트는 에뮬레이터를 직접 띄우지 않는다.
- 앱 로그: `adb logcat -d -s HospitalTrip`, 우리 앱만: `adb logcat -d --pid=$(adb shell pidof com.meongcare.app)`.

### 준비 (PC)

1. `npm --prefix firebase run emulators` (별도 터미널) → `npm --prefix firebase run seed -- --reset`
2. `web/.env.local`(에뮬레이터) → `npm --prefix web run dev` → 브라우저 `http://localhost:5173/hospital/?h=demo-fast&code=<코드>` (firebase/README.md의 링크)
3. `secrets.properties`에 에뮬레이터 값 → debug 빌드 설치. Pleos 에뮬레이터는 Cold boot 뒤 **내비 화면을 한 번 띄운다** (011).

### Pleos `emulator-5554` + PC 병원 웹 (debug, 시연 모드, 기어 P)

| # | 조작 | 기대 |
|---|---|---|
| E1 | 병원 웹: 대기 10분·보통·받음 → [병원 상태 알리기] | "마지막으로 알림 HH:mm (방금)" |
| E2 | 차: [진료 카드 만들기] → 증상 → [카드 만들기] → [가장 빨리 진료받을 곳 찾기] | 5초 안에 목록. 1위 "시연용 바른 동물병원 약 N분 (이동 약 N분 + 대기 10분)", 오래됨 묶음에 "정보 지난", "응급 접수 안 함" 1곳. `HospitalTrip` 로그 `병원 목록: 서버 5곳 (N ms)` (O3) |
| E3 | 병원 웹에서 대기 120분으로 알림 → 차 목록에서 [새로 고침] (또는 30초 기다림) | 순위가 바뀐다 |
| E4 | 차: 1위 줄 → 확인 화면 → [출발] | 확인 화면 문구가 설계 결정 6과 같다. 내비가 앞에 나와 그 병원 경로를 계산한다 (O2). 병원 웹 환자 목록에 5초 안에 "보리" 카드와 "도착 예정 HH:mm · 약 N분 후"가 뜬다 (O4) |
| E5 | GNB로 차 앱에 돌아온다 → [병원에 전화] | 카드 머리 "병원까지 N분 · {고른 병원}", "진료 카드를 병원에 보냈어요". 전화 앱이 열린다 (009와 같이 번호는 안 보일 수 있다) |
| E6 | 기어 D | 주행 화면 "병원까지 N분" + 고른 병원 이름. 남은 분이 바뀌는 동안 병원 웹의 도착 예정이 1분에 한 번 이하로 바뀐다 |
| E7 | 시연 도착까지 기다림 → 기어 P → [닫기] | 병원 웹 "도착했어요". 차는 처음 화면 |
| E8 | 다시 찾기 → 출발 → [안내 취소] | 병원 웹 "취소됨" |
| E9 | 병원 웹 [완료] | 목록에서 사라진다. Emulator UI(4000)에 그 문서가 없다 |
| E10 | Firebase 에뮬레이터를 끄고 차에서 찾기 → 출발 | 5초 안에 "서버에 연결하지 못해 시연용 병원 정보를 보여 줘요" + 내장 5곳. 출발 뒤 "시연용: 실제 병원에는 보내지 않았어요" |
| E11 | 목록 화면, 확인 화면 각각에서 기어 D → UI 트리 → P | 주행 화면. debug 패널 영역을 빼면 `EditText` 0개, 클릭 노드 0개. P면 같은 목록·확인 화면 (목록 폴링은 주행 중 멈췄다가 다시 돈다) |
| E12 | 로그 | `adb logcat -d -t 1000 *:E`에 우리 앱 크래시·`SecurityException` 없음. 우리 앱 pid 로그에서 `grep -cE "37\.[34][0-9]{3}\|127\.[01][0-9]{3}\|보리\|기침\|호흡"`이 0 |

- 그 밖: 내비 준비 전(Cold boot 직후 내비를 안 띄움)에 찾기 → "현재 위치를 아직 몰라 대기 시간이 짧은 순으로…" + [위치 다시 찾기] → 내비를 띄운 뒤 [위치 다시 찾기] → 합계 순.
- O1이 안 되면 `adb reverse tcp:8080 tcp:8080`, `adb reverse tcp:9099 tcp:9099`, `FIREBASE_EMULATOR_HOST=127.0.0.1`로 다시 한다.

### 일반 폰 `emulator-5556`

| # | 조작 | 기대 |
|---|---|---|
| P1 | debug Fake: 카드 → 찾기 → 1위 → [출발] | "시연용 위치(수정구 시흥동) 기준이에요", 내장 5곳 같은 순서. 출발 → (6-2를 했으면) 목록의 이동 추정에서 시작하는 남은 시간 → 도착 → 주차 → 도착 카드, "시연용: 실제 병원에는 보내지 않았어요" |
| P2 | release APK(Firebase 설정 없음) + **비행기 모드** | P1과 같은 흐름이 끝까지 돈다 (문의 A2) |
| P3 | debug 시연 + 에뮬레이터 값(`10.0.2.2`) + PC 에뮬레이터 켬 | 서버 목록, 병원 웹에 카드가 뜬다 |
| P4 | 목록·확인 화면을 세로·가로·다크로 (`/verify` 4절) | 글자가 잘리거나 겹치지 않는다. 줄 높이가 터치 타깃 이상 |

## 단계

단계 하나 = 커밋 1~3개. 앱 코드 커밋마다 `assembleDebug`가 통과해야 한다 (커밋 직전 훅). **웹 커밋 전에는 직접 `npm --prefix web run check`** (훅이 웹을 보지 않는다).

### 필수 — 11/06: 차 앱을 가짜 데이터로 끝까지

- [x] 0. 이 계획 파일 커밋 → `docs(plans): 012 가장 빨리 진료받을 병원 계획 추가`
  - `[사용자 승인 필요]` U1~U4 답을 진행 로그에 적은 뒤 시작한다.
  - `.\gradlew.bat testDebugUnitTest`의 통과 수와 `npm --prefix web test`의 통과 수를 진행 로그에 적는다 (완료 기준 3의 기준값).

- [x] 1. 공유 데이터 형식 문서
  - 1-1. → `docs(data): 제휴 병원 상태 형식과 시연 병원 목록 추가`
    - `docs/data/hospital-status.md`, `docs/data/hospitals.sample.json` (설계 결정 5). 좌표를 지도로 확인해 고친다. 직선 거리 계산 결과(설계 결정 1의 9.6km 포함)를 진행 로그에 적는다.
  - 1-2. → `docs(data): 환자 건(진료 카드 전송) 형식 추가`
    - `docs/data/emergency-case.md`, `docs/data/emergency-case.example.json` ("보리" + 호흡 곤란·기침). Firestore 경로, 쓰는 쪽(차/병원 웹), 읽는 쪽 규칙, 시계 차이 한계.
  - 확인: `npm --prefix web run check` (docs/data가 웹 검사 대상이다).

- [x] 2. 이동 시간과 순위 (순수 Kotlin)
  - 2-1. → `feat(domain): 병원 상태 모델과 이동 시간 어림 추가`
    - `HospitalStatus.kt`, `HospitalCandidate.kt`, `Hospital.id`, `TravelEstimate.kt`
    - `TravelEstimateTest`: `` `같은 좌표면 1분이다`() ``, `` `시연 병원까지는 내비 관찰값 35분에 가깝다`() `` (±3분), `` `분은 올림한다`() ``, `` `멀수록 길어진다`() ``
  - 2-2. → `feat(domain): 이동과 대기를 합친 병원 순위 규칙 추가`
    - `HospitalRanking.kt` (설계 결정 2)
    - `HospitalRankingTest`:
      - `` `이동과 대기를 더한 시간이 짧은 순이다`() ``
      - `` `합이 같으면 이동 시간이 짧은 곳이 먼저다`() ``
      - `` `합과 이동이 같으면 이름 순, 그다음 id 순이다`() ``
      - `` `응급 접수를 받지 않는 곳은 순위에서 빼고 따로 모은다`() ``
      - `` `30분보다 오래된 대기 정보는 순위 뒤로 보낸다`() ``
      - `` `정확히 30분 전 정보는 아직 유효하다`() ``
      - `` `오래된 정보의 응급 접수 불가는 믿지 않는다`() ``
      - `` `갱신 시각이 5분보다 미래면 오래된 정보로 본다`() ``
      - `` `대기 정보가 없는 병원은 이동 시간 순으로 뒤에 둔다`() ``
      - `` `위치를 모르면 대기 시간 순이고 이동 시간은 비운다`() ``
      - `` `시연 병원 5곳은 바른, 시연용, 가까운 순이다`() ``

- [x] 3. 병원 목록·카드 전송 인터페이스와 Fake
  - 3-1. → `feat(domain): 병원 목록과 카드 전송 인터페이스 추가`
    - `HospitalBoardSource.kt`, `domain/trip/CaseCard.kt`, `CaseSender.kt`
  - 3-2. → `feat(data): 내장 시연 병원 목록과 Fake 카드 전송 추가`
    - `SampleHospitals.partners`, `BuiltInHospitalBoardSource`(clock 주입), `FallbackHospitalBoardSource`, `FakeCaseSender`
    - `libs.versions.toml` + `testImplementation`에 `org.json:json` (버전은 Maven Central에서 확인해 고정, 라이선스를 노트에)
    - `BuiltInHospitalBoardSourceTest`: `` `시연 병원 5곳을 내장 출처로 준다`() ``, `` `갱신 시각은 지금에서 시드 분만큼 뺀 값이다`() ``
    - `FallbackHospitalBoardSourceTest`: `` `서버가 주면 서버 목록이다`() ``, `` `서버가 5초 안에 답하지 않으면 내장으로 대체한다`() ``, `` `서버 실패면 내장으로 대체했다고 알린다`() ``, `` `취소는 그대로 전한다`() ``
    - `SampleHospitalsJsonTest`: `` `내장 시연 병원이 hospitals.sample.json과 같다`() `` (경로 `../docs/data/hospitals.sample.json`)

- [x] 4. 출발 조정 (`domain/trip/HospitalTrip.kt`) → `feat(domain): 출발 때 카드 전송과 도착 예정 갱신 추가`
  - 설계 결정 8의 4·7. `FinderStep`도 여기.
  - `HospitalTripTest` (가짜 `GuidanceSource`·`CaseSender`, 가상 시간):
    - `` `출발하면 고른 병원으로 길안내를 시작하고 카드와 도착 예정을 보낸다`() ``
    - `` `카드 전송이 실패해도 길안내는 계속되고 다시 보낼 수 있다`() ``
    - `` `카드 없이 출발하면 아무것도 보내지 않는다`() ``
    - `` `출발을 두 번 불러도 한 번만 출발한다`() ``
    - `` `남은 분이 바뀌고 60초가 지났을 때만 도착 예정을 다시 보낸다`() ``
    - `` `도착하면 도착 상태를 보낸다`() ``
    - `` `안내 중에 취소되면 취소 상태를 보낸다`() ``
    - `` `도착 화면을 닫으면 고른 병원을 비우고 상태는 보내지 않는다`() ``
    - `` `보내는 카드에 사진과 위치가 없다`() ``
    - `` `내장 병원은 Fake 전송으로, 서버 병원은 서버 전송으로 보낸다`() ``
    - `` `로그에 반려견 이름과 증상이 없다`() ``

- [x] 5. 차 화면: 목록과 확인 (Fake로 동작)
  - 5-1. → `feat(ui): 가장 빨리 진료받을 병원 목록 화면 추가`
    - `HospitalFinderViewModel`(위치·목록 동시에, 30초 폴링은 구독 중에만, [새로 고침], [위치 다시 찾기]), `HospitalFinderUiState`, `HospitalTexts`, `HospitalListScreen`, 미리보기 (넓은·좁은)
    - `HospitalFinderViewModelTest`: `` `열면 위치와 목록을 함께 받는다`() ``, `` `화면이 보이는 동안 30초마다 목록을 다시 받는다`() ``, `` `화면이 사라지면 다시 받지 않는다`() ``, `` `위치를 모르면 대기 시간 순 안내를 보인다`() ``, `` `Real에서 목록을 못 받으면 다시 시도를 보인다`() ``, `` `병원을 고르면 확인 단계다`() ``
    - `HospitalTextsTest`: `` `합계는 약 N분, 내역은 이동 약 N분 + 대기 N분이다`() ``, `` `알린 지 1분 안이면 방금이다`() ``, `` `위치를 모르면 이동 시간 모름이다`() ``, `` `출처마다 안내 문구가 다르다`() ``
  - 5-2. → `feat(ui): 병원 확인 화면과 출발 연결`
    - `HospitalConfirmScreen` (설계 결정 6의 문구, 버튼 4개), `ParkedContent`의 `HOSPITALS`·`CONFIRM_HOSPITAL`, `parkedContentOf(…, finder)`, `HomeScreen` 연결, 시스템 뒤로 = [뒤로]
  - 확인: `assembleDebug`, 완료 기준 7의 Grep 일부 (`ui/`의 data·di import 0건, domain·data/hospital의 `import android.` 0건).

- [x] 6. 고른 병원으로 길안내·전화 연결 → **체크포인트 A**
  - 6-1. → `feat(di): 고른 병원으로 길안내와 전화 연결`
    - `AppContainer`: `hospitalBoardSource`(이 커밋에서는 모든 모드가 내장), `caseSenderFor`, `hospitalTrip`, `symptomLabel`. `MainActivity` 연결.
    - `MedicalCardScreen`: Idle 버튼 교체, 전송 줄, 고른 병원 전화. `MedicalCardViewModel` 맨 뒤 인자.
    - 도착 카드 [닫기]에 `trip.clear()`.
  - 6-2. (잘라낼 수 있음) → `feat(data): Fake 길안내를 목록의 이동 어림에서 시작`
    - 설계 결정 8의 8. `FakeGuidanceSource` 새 인자 테스트를 같은 커밋에: `` `처음 남은 시간을 목적지 어림값으로 정할 수 있다`() ``, `` `어림값이 없으면 기본 12분이다`() ``
  - 확인: `/verify`로 일반 폰 P1, P4. Pleos 에뮬레이터 Fake 모드에서도 목록 → 출발 → 주행 → 도착을 한 번 본다. **여기까지면 차 앱만으로 시연 영상을 찍을 수 있다** (진행 로그에 적는다).

### 필수 — 11/07: 병원 웹 → Firebase → 연결

- [ ] 7. 병원 웹 도메인과 메모리 저장소
  - 7-1. → `feat(web): 병원 상태와 환자 건 형식 읽기·쓰기 추가`
    - `web/src/domain/hospital/` (타입, `*Json.ts`, `etaText.ts`, `caseOrder.ts`, `statusForm.ts`, `hospitalDesk.ts`)
    - Vitest: `hospitalStatusJson.test.ts`(hospitals.sample.json을 읽는다, 범위 밖 대기 → 대기 정보 없음, 모르는 혼잡도 → 대기 정보 없음, 모르는 버전 → 건너뜀), `emergencyCaseJson.test.ts`(emergency-case.example.json, 사진 필드가 없다, `label` 없으면 `code`), `etaText.test.ts`(`도착 예정 14:32 · 약 12분 후`, 지난 시각 → `곧 도착`, `null` → `경로를 찾는 중`), `caseOrder.test.ts`(도착 예정 순, 취소는 맨 아래), `statusForm.test.ts`(−5·+5가 0~600을 넘지 않는다)
  - 7-2. → `feat(web): 병원 웹 메모리 저장소 추가`
    - `memoryHospitalDesk.ts`, `sampleHospitals.ts` + `sampleHospitals.test.ts`(docs JSON과 같다), 예시 환자 1건
  - 확인: `npm --prefix web run check`.

- [ ] 8. 병원 웹 화면
  - 8-1. → `feat(web): 병원 웹 페이지(/hospital/)와 병원 고르기 추가`
    - `web/hospital/index.html`, `vite.config.ts` 다중 입력 (F6), `web/src/hospital/main.tsx`·`container.ts`(이 커밋은 메모리만), 병원 고르기, 시연 안내
  - 8-2. → `feat(web): 병원 상태 입력 화면 추가`
  - 8-3. → `feat(web): 들어온 환자 목록 화면 추가` ([완료] 포함, 30초마다 "약 N분 후" 다시 계산)
  - 확인: 커밋마다 `npm --prefix web run check`. `npm --prefix web run dev`로 `/`(보호자)와 `/hospital/`이 둘 다 열린다.

- [ ] 9. (잘라낼 수 있음) 병원 웹 화면 확인 시나리오 → `test(web): 병원 웹 화면 확인 시나리오 추가`
  - `verify_screens.py`에 H01~H05 (설계 결정 7). 화면 확인용 빌드가 `.env.local`을 무시하는 방법을 정해 web/README.md에 적는다.
  - 확인: 완료 기준 4.

- [ ] 10. Firebase 에뮬레이터 구성
  - 10-1. → `chore(firebase): Firebase 에뮬레이터 구성과 보안 규칙 추가`
    - `firebase/` (설계 결정 9의 파일 중 package.json, .npmrc, firebase.json, .firebaserc, firestore.rules, firestore.indexes.json, README.md), `.gitignore`
    - F1 확인 결과를 README와 진행 로그에.
  - 10-2. → `chore(firebase): 시연 병원 시드와 규칙 점검 스크립트 추가`
    - `seed/join-codes.emulator.json`, `scripts/seed.mjs`(F3·F5), `scripts/check-rules.mjs`(R1~R8)
  - 확인: 완료 기준 5. 규칙 점검이 실패하면 규칙을 고치고 같은 단계에서 다시 커밋한다.

- [ ] 11. 병원 웹 Firestore 연결 → `feat(web): 병원 웹 Firestore 저장소 추가`
  - `web/package.json`에 `firebase` (정확한 버전), `firestoreHospitalDesk.ts`(익명 로그인 → `open`에서 직원 등록 → `onSnapshot` 두 개 → `serverTimestamp()`로 상태 쓰기 → [완료] 삭제), `container.ts`가 `VITE_FIREBASE_*`로 고른다, `web/.env.example`, `vite-env.d.ts` 타입.
  - 확인: `npm --prefix web run check` (설정 없이 → 메모리). 에뮬레이터로 E1·E9를 웹만으로 본다 (Emulator UI에서 문서가 바뀐다). 브라우저 콘솔 에러 0.

- [ ] 12. 차 앱 서버 연결 (REST)
  - 12-1. → `feat(data): 서버 설정과 Firestore REST 값 변환 추가`
    - `ServerConfig.kt`, `HttpTransport.kt`, `FirestoreJson.kt`, BuildConfig 3개(release는 에뮬레이터 주소를 빈 값으로), `secrets.properties.example`
    - `ServerConfigTest`: `` `프로젝트 ID가 없으면 서버를 쓰지 않는다`() ``, `` `debug에서 에뮬레이터 주소가 있으면 에뮬레이터로 간다`() ``, `` `release는 에뮬레이터 주소를 무시한다`() ``, `` `release에서 API 키가 없으면 서버를 쓰지 않는다`() ``
    - `FirestoreJsonTest`: `` `병원 문서를 읽는다`() ``, `` `좌표가 정수 값으로 와도 읽는다`() ``, `` `빈 배열은 values 없이 와도 읽는다`() ``, `` `상태 필드가 틀리면 대기 정보 없음이다`() ``, `` `모르는 형식 버전의 병원은 건너뛴다`() ``, `` `환자 건을 형식 문서대로 쓴다`() `` (emergency-case.example.json과 비교), `` `사진과 위치는 쓰지 않는다`() ``
  - 12-2. → `feat(data): 익명 로그인과 서버 병원 목록·카드 전송 추가`
    - `AnonymousAuth.kt`, `FirestoreHospitalBoardSource.kt`, `FirestoreCaseSender.kt`, `UrlConnectionHttpTransport.kt`
    - `FirestoreHospitalBoardSourceTest`: `` `익명 가입은 한 번만 하고 토큰을 다시 쓴다`() ``, `` `HTTP 오류면 Unavailable이다`() ``, `` `응답이 JSON이 아니면 Unavailable이다`() ``, `` `요청에 Bearer 토큰을 붙인다`() ``
    - `FirestoreCaseSenderTest`: `` `카드는 POST로 만들고 문서 이름을 돌려준다`() ``, `` `도착 예정 갱신은 바꾼 필드만 updateMask로 보낸다`() ``, `` `실패는 던지지 않고 결과로 돌려준다`() ``
  - 12-3. → `feat(di): 데이터 모드별 병원 출처와 카드 전송 연결`
    - `HospitalPlan.kt` + `HospitalPlanTest`: `` `Fake는 내장 병원과 Fake 전송이다`() ``, `` `시연은 서버가 있으면 서버이고 실패하면 내장으로 대체한다`() ``, `` `시연에서 서버 설정이 없으면 내장이다`() ``, `` `Real은 서버만 쓰고 대체하지 않는다`() ``, `` `release는 시연 표를 따른다`() ``
    - `AppContainer` 연결, `HospitalTrip` 로그 연결(`Log.i("HospitalTrip", …)`)
    - `AndroidManifest.xml`에 `INTERNET`, `app/src/debug/AndroidManifest.xml` + `app/src/debug/res/xml/network_security_config.xml`
  - 확인: `assembleDebug assembleRelease`(설정 없이), 완료 기준 7 전체. P3.

- [ ] 13. 처음부터 끝까지 확인 (`/verify` 포함)
  - "확인 절차"의 E1~E12, P1~P4. O1~O4 결과를 진행 로그에 적는다.
  - 고친 것이 있으면 `fix(...)` 커밋. 관찰은 → `docs(pleos): Pleos 에뮬레이터에서 호스트 PC 서버 접속 관찰 기록` (`docs/pleos/emulator-setup.md`, O1), 좌표를 고쳤으면 `docs(data): …`.
  - 시연 영상을 찍는다면 이때 (차 화면 + 병원 웹 나란히).

- [ ] 14. tester → `test: …` (tester가 쓴 테스트를 영역별로 커밋)
  - 넘길 것: 이 계획 파일 경로와 대상만. 대상: `TravelEstimate`, `rankHospitals`, `FallbackHospitalBoardSource`, `HospitalTrip`, `ServerConfig`, `FirestoreJson`, `AnonymousAuth`, `FirestoreCaseSender`, `hospitalPlanOf`, `HospitalFinderViewModel`, `parkedContentOf`(새 단계), 웹 `web/src/domain/hospital/*`.
  - 버그 보고는 메인이 `fix:`로 고친다. 고친 뒤 UI가 바뀌었으면 `/verify`를 다시.

- [ ] 15. 문서 갱신
  - 15-1. → `docs(architecture): 제휴 병원 순위와 카드 전송 구조 기록`
    - 서비스 기능 스택: 서버 행("Firebase Functions" → Firestore + 익명 Auth, Spark, Functions 없음), 병원 검색 행(제휴 병원 목록, 카카오는 나중), 새 행(병원 순위, 카드 전송, 이동 시간 어림). 데이터 모드에 설계 결정 4의 표. 패키지 구조. "보호자 웹" 결정 표의 의존성 줄. 새 절 "병원 웹 (012)"(설계 결정 6·7·9 요약). "Firebase로 옮길 때 할 일"에 남은 데이터 정리·병원 코드·API 키 제한 메모. 011의 "release에서는 위치용 NaviHelper가 만들어지지 않는다"를 고친다 (이제 목록을 열 때 묻는다).
  - 15-2. → `docs: 에이전트 지도와 README에 병원 웹·Firebase 반영`
    - `AGENTS.md`: 기술 스택(Firebase 줄), 명령어 표(에뮬레이터·시드·규칙 점검), 디렉토리 표(`firebase/`, `web/`에 병원 웹).
    - `README.md`: 시연 흐름(찾기 → 목록 → 확인 → 출발), 시연용 표시, "병원 웹 (선택)" 절. `web/README.md`: `/hospital/`, `.env.local`, 화면 확인 빌드.
  - 확인: 문서만 바뀐 커밋. 링크가 맞는지 본다.

- [ ] 16. 개발 노트와 마무리
  - `docs/notes/xxj15/2026-11-07-fastest-hospital.md` (형식: docs/notes/README.md). 넣을 것:
    - 결정과 이유 (REST를 고른 이유, 이동 시간 어림의 근거와 한계, 순위 규칙, 시연 코드 방식, 대체 동작)
    - 사용한 Pleos API·권한 (새 권한 없음), 새 일반 권한 INTERNET, 새 의존성(`firebase` 웹, firebase-tools, 테스트 `org.json:json`)과 라이선스
    - 확인한 것 / 확인하지 못한 것 (실차, 진짜 Firebase 프로젝트, 토큰 만료)
    - 기획서 5번 "개발 계획"에 넣을 문장 (확인한 것만). 예: "제휴 병원이 병원용 웹 화면에서 예상 대기 시간과 응급 접수 여부를 알리면, 차 앱은 NaviHelper로 받은 현재 위치에서의 이동 시간 어림값과 합쳐 가장 빨리 진료받을 수 있는 순서로 보여 준다. 운전자가 정차 중 병원을 고르고 출발하면 진료 카드와 도착 예정 시간이 그 병원 화면에 바로 뜨고, 주행 중에는 남은 시간이 갱신된다. 서버는 Firebase(Firestore, 익명 인증)이고, 서버에 닿지 못하면 시연용 병원 정보로 같은 흐름을 이어 간다."
    - 남은 일: 있으면 좋음 중 안 한 것, K9(PT 빌드), U5, 카카오 병원 붙이기, 보호자 웹 Firestore 이전, `data/guidance/navi` → `data/navi` 정리(011에서 넘어옴)
  - 이 계획을 `docs/plans/completed/`로 옮기고 진행 로그를 마친다.
  - 커밋: `docs(notes): fastest-hospital`, `docs(plans): 012 가장 빨리 진료받을 병원 완료`
  - 확인: `git status`에 남은 변경이 없다. 그다음 `/ship`.

### 있으면 좋음 (필수가 끝난 뒤, 단계 16 전에)

- [ ] A. 병원 "확인함" → 차 카드에 표시 → `feat(web): 환자 카드 확인함 버튼 추가`, `feat(data): 병원의 카드 확인을 차 카드에 표시`
  - 차는 안내 중 카드 화면이 보이는 동안만 자기 환자 건을 30초마다 읽는다. 주행 화면에는 넣지 않는다.
- [ ] B. 진짜 Firebase 프로젝트로 바꾸기 (**사용자가 프로젝트를 만든 뒤**, U5 답 뒤)
  - 코드는 바꾸지 않는다. 바꾸는 것: `firebase use --add`(별칭), `firebase deploy --only firestore:rules`, 콘솔에서 익명 로그인 켜기(F4), 시연 병원 5곳·코드 넣기(방법은 이때 정한다. 콘솔 직접 입력이 가장 단순), `secrets.properties`, `web/.env.local`.
  - API 키에 Android 앱 제한을 걸면 REST 요청에 패키지·서명 헤더가 필요할 수 있다 → 이때 확인한다.
  - 커밋: `docs(firebase): 진짜 Firebase 프로젝트 전환 방법 기록` (값은 커밋하지 않는다)
- [ ] C. 익명 토큰 갱신 (`securetoken` REST, 401이면 한 번 갱신) → `feat(data): 만료된 익명 토큰 갱신`
- [ ] D. 병원 웹 Firebase Hosting 배포 (B 뒤) → `chore(firebase): 병원 웹 호스팅 설정 추가`
- [ ] E. `[선행: pleos-docs-researcher]` NaviHelper로 후보별 실제 소요 시간 (`requestTrip`) spike → 되면 순위의 이동 시간을 바꾸는 계획을 따로 쓴다
- [ ] F. 규칙 점검을 Vitest + `firebase emulators:exec`로 (CI에는 Java가 필요해 넣지 않는다) → `test(firebase): 보안 규칙 자동 점검`

## 진행 로그

- 2026-11-06: 계획 작성.
  - 브랜치 `feat/fastest-hospital`는 최신 dev(010 길안내, 011 현재 위치 포함)에서 땄다. 첫 커밋 `d4aa907` `docs: 문의 A1 답(외부 API·자체 서버 허용) 기록`.
  - 시작 시점의 코드:
    - 카드(안내 없음)의 [길 안내 시작]은 고정된 가상 "시연용 동물병원"(`SampleHospitals.demo`) 한 곳으로 간다. [병원에 전화]도 그 번호(02-0000-0000).
    - 현재 위치(`CurrentLocationSource`)는 debug 패널에서만 쓴다.
    - 매니페스트에 `INTERNET`이 없다. Firebase·HTTP 코드가 없다.
    - `web/`은 보호자 페이지 하나(라우터 없음), 의존성 6개.
  - 사용자 결정 (2026-11-06): A1 답(외부 API·자체 서버 허용), Firebase Local Emulator Suite로 먼저 개발, 병원 웹은 `web/` 안에, [출발] 때 자동 전송 + 주행 중 갱신, 제휴 병원만, Firestore Spark(Functions·Blaze 없음).
  - 사용자 결정 U1~U4 (2026-11-06): U1 B(병원별 코드 링크), U2 [카드 없이 길안내만] 둔다, U3 추천대로(사진·차 위치 제외, 진료 카드는 보낸다), U4 `firebase` 웹 의존성과 `firebase/` 폴더 추가.
  - 사용자가 진짜 Firebase 프로젝트 "MeongCare"(Spark)를 만들고 Firestore(서울)를 만들었다 (2026-11-06). 연결은 있으면 좋음 B에서 한다.
  - 기준값 (단계 0): Android `testDebugUnitTest` 464개 통과 (실패 0, 건너뜀 0), 웹 `npm --prefix web test` 19파일 216개 통과.
  - 단계 0~6 완료 → **체크포인트 A** (2026-11-06 오전). 커밋 `6721c5c` ~ 이 커밋. 단위 테스트 532개 통과 (기준 464 + 68).
  - 단계 1: 시연 병원 좌표를 근처 역 부근으로 정했다 (바른=정자역, 가까운=판교역, 정보 지난=수서역, 응급 휴진=야탑역 부근). 직선 거리(하버사인): 바른 5.22km, 시연용 9.59km(설계 결정 1의 9.6km 맞음), 가까운 2.48km, 정보 지난 8.32km, 응급 휴진 2.90km. 순위 29·40·69분.
  - 단계 3: `org.json:json` **20250517** (Maven Central 최신, 2025-05-17). 테스트 전용. 라이선스 Public Domain. 테스트가 `docs/data/`를 읽게 `app/build.gradle.kts`에서 시스템 속성으로 위치를 넘기고 테스트 입력으로 등록했다.
  - 단계 6 확인 (Pleos `emulator-5554`, debug 시연 모드, 주행 Real · 내비 경로):
    - 카드(안내 없음): [가장 빨리 진료받을 곳 찾기]만, [병원에 전화] 없음 (K5).
    - 목록: 내비 현재 위치 209ms. 1위 바른 약 29분 → 시연용 40분 → 가까운 69분, "대기 정보가 없거나 지난 곳"에 정보 지난(이동 약 30분 · 대기 정보 90분 전), "응급 접수를 받지 않는 곳: 응급 휴진". 안내 "시연용 병원 정보예요", "직선 거리로 어림". 다크 모드 이상 없음.
    - 확인 화면에서 기어 D → 주행 화면, `EditText` 0개, 클릭 노드 1개(debug 패널 버튼). P → 같은 확인 화면 (E11 일부).
    - [출발] → 내비가 앞에 나와 "시연용 바른 동물병원" 경로 시작. **O2(바른): 내비 7.0km · 900초(15분)** (어림 19분). 로그 `카드 전송: 시연용 (서버로 보내지 않음)` → 내비 첫 값으로 `도착 예정 갱신: 성공` (기다리지 않음) → 1분 뒤 한 번 더.
    - 카드(안내 중): "병원까지 15분 · 시연용 바른 동물병원", 전송 줄 "시연용: 실제 병원에는 보내지 않았어요", [병원에 전화]는 02-0000-0001.
    - D로 시연 도착 → `도착 알림: 성공` → P → 도착 카드 → [닫기] → 처음 화면.
    - [카드 없이 길안내만] → "카드 없이 출발했어요", 전송 로그 없음. [안내 취소] → 카드가 안내 없음으로 돌아오고 고른 병원이 비워진다.
    - 시스템 뒤로: 확인 → 목록 → 카드.
    - `*:E`에 우리 앱 크래시·`SecurityException` 없음. 우리 앱 pid 로그에서 좌표·"보리"·증상 0건.
    - **확인하지 못한 것**: 좁은(세로) 배치. Pleos 에뮬레이터는 회전하면 시스템 화면이 세로를 지원하지 않아 앱 창이 잘린다 (011과 같음). 일반 폰 에뮬레이터(`emulator-5556`)가 꺼져 있어 P1~P4는 아직이다. 미리보기(411dp)는 있다.
    - 6-2: Fake 길안내는 이제 목록의 이동 어림에서 시작한다 (차량 기기 시연은 내비 예상 시간이 앞선다).

## 결정 / 배운 점

- `HospitalBoardSource`를 `fun interface`로 했다 (함수 하나, 테스트에서 람다로 가짜를 만든다).
- 남은 분 올림(`toCaseMinutes`)은 도메인에 같은 규칙으로 두고, 화면의 `toDisplayMinutes`와 같은지 테스트로 묶었다 (ui → domain 의존을 만들지 않으려고).
- 도착 예정 갱신 규칙에 하나를 더했다: 처음 보낸 값이 목록의 **어림값**이면 내비가 준 첫 남은 시간은 60초를 기다리지 않고 바로 보낸다 (병원이 실제 값을 빨리 보게). 그다음부터는 K8대로 1분에 한 번 이하. 60초 안에 바뀌면 60초가 될 때 최신 값을 보낸다 (`collectLatest`).
- 취소 처리: 실패해서 [다시 보내기]를 기다리는 중이면 바로 끝내고, 보냈거나 보내는 중이면 "취소"를 보낼 시간을 10초까지 준다.
- `MedicalCardViewModel`은 고른 병원(`selectedHospital`)이 있으면 그 병원을 쓰고, 고르기 전이면 전화 번호를 비운다. 인자를 주지 않으면 예전 동작이다 (기존 테스트 그대로).
- 카드 화면 파라미터 `onStartGuidance` → `onFindHospital`로 이름을 바꿨다 (UI 테스트가 없어 영향 없음). 안 쓰게 된 `card_start_guidance` 문구를 지웠다 (lint UnusedResources).
- 시연 병원 좌표 관찰: 바른(정자역 부근)까지 내비는 7.0km·15분, 직선 5.22km. 계수 1.25·21km/h로는 19분이라 어림이 더 길다. 1건만으로 계수를 바꾸지 않는다 (관찰은 노트에 남긴다).
- Compose 컴파일러가 data class에 `$stable` static 필드를 붙인다. 리플렉션으로 필드를 셀 때는 static을 뺀다.
