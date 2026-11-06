# 013 시연 병원 ↔ 차 앱 연결 다듬기와 길안내 화면 완성 (smooth-trip)

- 목표: 시연에서 차 앱과 시연용 병원 웹이 끊김 없이 이어지게 하고(병원의 카드 확인이 차에 보이고, 남은 시간이 몇 초마다 갱신되고, 시연 도착 때 내비가 MeongCare를 가리지 않고, 어느 서버에 붙었는지 보이게), 길안내 중의 정차·주행 화면을 한눈에 읽히게 완성한다.
- 완료 기준 (어떻게 검증하나). `$BASE` = `5ea81ba` (이 브랜치를 딴 dev. rebase 뒤에는 `origin/dev`).
  1. `node .claude/hooks/build-gate.mjs pipeline`이 "검증 파이프라인 통과."를 출력한다 (최신 dev 위로 rebase한 뒤).
  2. `secrets.properties`에 Firebase 값이 **없는** 상태에서 `.\gradlew.bat assembleDebug assembleRelease`가 성공한다.
  3. 단위 테스트 (`.\gradlew.bat testDebugUnitTest`, `npm --prefix web test`):
     - 기존 Android 708개, 웹 321개가 그대로 통과한다 (단계 0에서 다시 센 값).
     - `git diff $BASE --diff-filter=MD --name-only -- app/src/test ":(glob)web/src/**/*.test.ts"`의 출력이 비어 있다 (`$BASE`에 있던 테스트 파일을 고치거나 지우지 않는다. 새 테스트는 새 파일에. 이 브랜치에서 만든 테스트 파일은 고쳐도 된다).
     - 새 테스트 클래스·파일이 통과한다: `NaviRouteOnlyKeepRouteTest`, `TripTimingTest`, `HospitalTripAckTest`, `HospitalTripProgressTest`, `FirestoreAckTest`, `TripPanelTest`, `AnonymousAuthRefreshTest`, `ServerConfigRefreshTest`, 웹 `caseAck.test.ts`, `serverInfo.test.ts`, `memoryHospitalDesk.ack.test.ts`, 그리고 tester가 더한 것.
  4. 보안 규칙: 사용자 터미널의 Firebase 에뮬레이터(127.0.0.1)에서 `npm --prefix firebase run check-rules`가 R1~R13을 모두 PASS로 출력한다.
  5. 시연 전 점검: `npm --prefix firebase run preflight`가 정상 환경에서 PF1~PF5 모두 PASS, 에뮬레이터를 끄면 PF1·PF2 FAIL + 종료 코드 1, `adb reverse --remove tcp:9099` 뒤에는 PF5 FAIL.
  6. 바뀌지 않아야 하는 것 (`git diff $BASE -- <경로>`가 비어 있다):
     - `di/SourcePlan.kt`, `di/NaviPlan.kt`, `di/LocationPlan.kt`, `di/HospitalPlan.kt`, `di/DataMode.kt`
     - `domain/guidance/` 전체, `data/guidance/FakeGuidanceSource.kt`, `data/guidance/NaviGuidanceSource.kt`
     - `ui/guidance/GuidanceViewModel.kt`, `ui/guidance/GuidanceUiState.kt`, `ui/home/HomeContent.kt`, `ui/home/ParkedContent.kt`
  7. 정적 검사 (Grep):
     - `import ai.pleos.playground.navi`는 `data/guidance/navi/PleosNaviClient.kt`에만 있다. `ui/` 아래에 `import com.meongcare.app.data`, `import com.meongcare.app.di`가 0건이다.
     - `import java.net`은 `data/server/UrlConnectionHttpTransport.kt`에만, `import org.json`은 `data/server/` 아래에만 있다.
     - debug 병합 매니페스트에서 `android:name="pleos.car.permission`이 정확히 2건(`NAVI_ROUTE`, `NAVI_ROUTE_SEARCH`)이다. `REORDER_TASKS`는 0건이다.
     - `ui/home/DrivingScreen.kt`와 `ui/guidance/StaticProgressBar.kt`에 `Button|clickable|Scroll|animate|TextField|Indeterminate`가 0건이다.
     - 웹: `from 'firebase`는 `web/src/data/firestoreHospitalDesk.ts`와 `web/src/hospital/container.ts`에만 있다. `web/src`(테스트 제외)에 `console.log`가 0건이다.
     - `firebase/scripts/`에 `.env`, `secrets.properties` 문자열이 0건이다 (점검 스크립트가 비밀 파일을 읽지 않는다).
  8. Pleos 에뮬레이터 + 병원 웹 나란히: 아래 "확인 절차"의 S1~S12. 특히:
     - S5: 시연 도착 순간 MeongCare가 앞에 남아 "도착했어요"가 보인다 (내비 빈 지도가 아니다). 그 순간 logcat에 `RequestRouteCancel`이 없다.
     - S3: 병원 웹의 "약 N분 후"가 약 4초마다 바뀐다 (logcat `도착 예정 갱신: 성공` 간격 4~6초).
     - S4: 병원 웹 [카드 확인] 뒤 8초 안에 차 주행 화면에 "병원이 카드를 확인했어요"가 뜬다.
  9. 주행 중 안전: 주행 화면(안내 중·도착)에서 UI 트리를 떠서 debug 패널 영역을 빼면 `EditText` 0개, 클릭할 수 있는 노드 0개, 스크롤 노드 0개. P로 바꾸면 같은 카드·처음 화면으로 돌아온다.
  10. 로그: 우리 앱 pid 로그에 좌표·반려견 이름·증상·토큰·uid가 0건이다 (S12).
  11. 문서: `docs/pleos/sdk-notes.md` `[obs-navi-emu]`에 U3 관찰, `docs/data/emergency-case.md`(카드 확인 쓰기 규칙, 시연 갱신 간격, 토큰 한계 문구 갱신), `docs/architecture.md`, `firebase/README.md`, `web/README.md`, `README.md`, `AGENTS.md`(명령어 표)가 갱신돼 있다.
- 브랜치: `feat/smooth-trip` (dev `5ea81ba`에서 땄다)
- 관련 문서:
  - AGENTS.md: 규칙 1·2·3·6·7·8, 아키텍처 규칙 (SDK는 `data/` 래퍼, 래퍼마다 Fake, 병원 웹은 `HospitalDesk`만, 차 서버 통신은 `data/server/`만)
  - docs/plans/completed/012-fastest-hospital.md: 설계 결정 3(REST 층), 5(형식), 6(규칙 R1~R8), 7(병원 웹), 8(화면 흐름), "있으면 좋음" A(확인함)·C(토큰 갱신)
  - docs/plans/completed/010-real-navi-guidance.md: 설계 결정 4(경로만 내비), U7(시연 도착 때 경로 취소 → 이번에 바꾼다)
  - docs/notes/xxj15/2026-11-06-fastest-hospital.md: "확인하지 못한 것", "주의점"
  - docs/data/emergency-case.md, firebase/firestore.rules, firebase/scripts/check-rules.mjs·emulator.mjs
  - docs/pleos/sdk-notes.md `[obs-navi-emu]` (이전 경로의 `onRouteCancelled`는 어댑터가 거른다), docs/pleos/emulator-setup.md "PC의 Firebase 에뮬레이터에 앱이 붙기"
  - docs/pleos/ux-guidelines.md: "주행 안전 설계 원칙", "생명주기와 백그라운드"(폴링 최소화), "외부 서버·네트워크"(토큰은 URL에 넣지 않는다)
  - 코드: `domain/trip/HospitalTrip.kt`·`CaseSender.kt`·`CaseCard.kt`, `data/server/*`, `data/guidance/navi/NaviClient.kt`·`PleosNaviClient.kt`, `data/guidance/NaviRouteOnlyGuidanceSource.kt`, `di/AppContainer.kt`, `ui/home/DrivingScreen.kt`·`ParkedScreen.kt`·`HomeScreen.kt`, `ui/card/MedicalCardScreen.kt`, `MainActivity.kt`, `web/src/domain/hospital/*`, `web/src/data/*HospitalDesk.ts`, `web/src/ui/hospital/*`

## 범위

- 사용자 요청 (2026-11-06 원문): "1. 시연용 동물병원 - 차 앱 사이 연결 좀 더 매끄럽게 해줘 2. 네비게이션 기능 거기 UI도 이상하고 아직 뭔가 어색해서 거기좀 완성해줘"
- 11/15 제출물과의 관계: 필수 제출물은 기획서 DOCX다. 이 작업은 **시연 영상**(차 화면 + 병원 웹 나란히)과 PT(12/03)의 완성도를 올린다. 기획서에는 "병원이 카드를 확인하면 차에 알린다"를 확인한 사실로 쓸 수 있게 된다.
- **꼭 할 것 A** (11/07~11/08, 영상 촬영 전): 단계 0~12 → **체크포인트 A: 차 + 병원 웹 시연 영상을 찍을 수 있다.**
- **꼭 할 것 B** (11/09, 늦어도 PT 전): 단계 13(익명 토큰 갱신), 14(tester), 15(문서), 16(노트).
  - 토큰 갱신은 앱을 1시간 넘게 켜 두는 PT에 필요하다. 11/15 영상에는 필요 없다. 시간이 모자라면 11/15 뒤로 미룬다.
- **있으면 좋음**: A~D (아래 "단계"). 필수가 끝난 뒤.
- **시간이 모자라면 이 순서로 자른다** (위가 먼저):
  1. 있으면 좋음 전부
  2. 단계 13 (토큰 갱신) → 11/15 뒤, PT 전으로 미룬다. 노트에 "PT 전 할 일"로 남긴다
  3. 단계 10의 실시간 연결 상태(`watchConnection`) → 서버 주소 줄과 localhost 경고만 남긴다
  4. 단계 11 (preflight 스크립트) → firebase/README에 손으로 하는 점검 목록만 적는다
  5. 단계 9-2의 탭 제목 → 카드 강조만 남긴다
  - 자르지 않는 것: 1(U3 기록), 2(U2), 3(갱신 간격), 4~6(카드 확인 핵심), 7~8(정차·주행 화면), 9-1, 12(확인), 14, 16.
- **이번 범위 밖** (사용자·메인 결정):
  - 내비 화면 열기 버튼 (내비 패키지명은 관찰값이고, `requestNavigationScreen`은 승인 안 된 `NAVI_CUSTOM_MAP`이 필요)
  - 병원 목록 자동 새로 고침 (차 안에서 목록이 손 밑에서 바뀌면 위험)
  - 소리 알림 (차·병원 웹 모두)
  - [안내 취소] 뒤 MeongCare를 스스로 앞으로 가져오기 (U3 spike 결과 안 됨. 단계 1에 기록만)
  - 진짜 Firebase 프로젝트 전환, 병원 웹 호스팅 (012 있으면 좋음 B·D)
  - Real 모드(전부 내비) 실차 확인

## 사용자·메인 결정 (2026-11-06, 확정)

- **U1 카드 확인**: 넣는다. 병원 웹 환자 카드에 [카드 확인] → `acknowledgedAt`(서버 시각). 차는 안내 중에만 자기 환자 건을 몇 초마다 읽고, 확인되면 멈춘다. 카드 화면·주행 화면에 "병원이 진료 카드를 확인했어요". **서버에서 온 병원일 때만.** 내장 시연 병원(Fake 전송)은 확인을 흉내 내지 않는다.
- **U2 시연 모드(경로만 내비)**: 시연 도착·[닫기] 때 내비 경로를 **취소하지 않는다.** 내비에는 경로가 남고 다음 출발 때 새 경로로 바뀐다 (이전 경로의 `onRouteCancelled`는 어댑터가 이미 거른다). Real(전부 내비)과 앱의 [안내 취소]는 지금처럼 취소한다.
- **U3 [안내 취소] 뒤 앞으로 가져오기**: spike 끝, **안 된다** (메인, 2026-11-06). 단계 1에서 관찰만 기록한다. 시험 코드는 이미 되돌렸다.
  - 앱 [안내 취소] → `cancelRoute` → 내비가 앞으로 나옴(12:43:44.736) → 900ms 뒤 `MainActivity`에서 `startActivity(REORDER_TO_FRONT|NEW_TASK)` → `Background activity launch blocked [... callingUidHasAnyVisibleWindow: false; callingUidProcState: LAST_ACTIVITY ...] (BAL_BLOCK) result code=3`
  - 2.5초 뒤 `ActivityManager.moveTaskToFront(taskId)` → `SecurityException: ... requires android.permission.REORDER_TASKS` (새 권한이 필요하다 → 쓰지 않는다)
  - HOME_SCREEN은 내비 start 14ms 뒤 `hideTask com.meongcare.app` 한다. Android 14 (API 34).
  - 결론: 앱의 [안내 취소]와 Real 모드 취소 뒤에는 지금처럼 내비 화면이 보인다. GNB의 MeongCare 아이콘으로 돌아온다.
- **U4 주행 화면**: 아주 큰 남은 분 + "○○병원까지" + 움직이지 않는 진행 막대 + "병원이 카드를 확인했어요" 한 줄(확인됐을 때만). 버튼·입력·스크롤·애니메이션 없음. 도착이면 "도착했어요" + 정차 안내.
- 메인이 정한 것 (요청 범위 안):
  - 시연(시뮬레이션) 길안내일 때 도착 예정 갱신 최소 간격을 몇 초로 줄인다. Real 내비는 60초 유지.
  - 익명 토큰 갱신 (securetoken REST의 refresh token, 에뮬레이터 9099 포함, 012 있으면 좋음 C).
  - 정차 중 카드 화면에 **안내 영역**(목적지, 큰 남은 시간, 도착 예정 시각, 진행 막대, 병원 대기 정보, 카드 전송·확인 상태, [병원에 전화] [안내 취소])을 따로 두고, 카드 머리는 "진료 카드"로 유지. 처음 화면 패널도 같은 컴포넌트.
  - 도착 화면: "○○에 도착했어요 · 이 카드를 접수처에 보여 주세요".
  - 병원 웹: 새 환자 강조·탭 제목 표시, 연결 상태 표시. 시연 전 점검 스크립트.

## 설계 결정 (planner 제안과 근거)

코드 블록은 기본안이다. 이름과 세부는 구현하면서 바꿔도 된다. 바꾸면 이 블록을 고치고 "결정 / 배운 점"에 이유를 남긴다.

### 1. 기존 테스트를 깨지 않는 자리 고르기 (중요)

메인 제안과 다르게 둔 곳이 있다. 이유는 모두 "기존 테스트가 data class 동등성이나 interface 구현으로 묶여 있다"이다.

| 메인 제안 | 이 계획 | 이유 |
|---|---|---|
| `GuidanceUiState.Guiding`에 진행 막대용 처음 남은 시간 | **`TripState.startMinutes`** (HospitalTrip이 앱 수명으로 기억) | `GuidanceUiState.Guiding("…", 12)`를 위치 인자 동등성으로 비교하는 기존 테스트가 약 30곳이다 (GuidanceViewModelTest, MedicalCardTripFlowTest, FakeTripScenarioScreenFlowTest 등). ViewModel이 값을 채우면 모두 깨진다. 또 ViewModel은 `WhileSubscribed(5초)`라 내비가 앞에 있는 동안 구독이 끊겨, 돌아오면 시작값이 지금 값으로 다시 잡혀 막대가 0으로 돌아간다 |
| `NaviClient.trip`에 "닫을 때 경로를 남길지" | `trip(destination)`은 그대로 두고 **기본 구현 있는 오버로드** `trip(destination, onClose)`를 더한다 | 테스트용 가짜 `FakeNaviClient`, `TripRecordingNaviClient`가 `override fun trip(destination)`을 구현한다. 시그니처를 바꾸면 컴파일이 깨진다. 기본 구현은 1인자 `trip`을 부른다 (= 지금처럼 취소) |
| `CaseSender`에 자기 건 읽기 | **기본 구현이 `AckRead.Unsupported`인 메서드** | 테스트의 `RecordingSender`가 `CaseSender`를 구현한다. 기본 구현이면 그대로 컴파일되고, HospitalTrip은 Unsupported를 받으면 로그 없이 읽기를 멈춰서 기존 로그·상태 비교도 그대로다 |
| 확인 표현 | `CardDelivery.Sent(toServer, acknowledgedAt: Instant? = null)` | `CardDelivery.Sent(toServer = true)` 비교가 기존 테스트에 7곳 있다. 기본값 null이면 확인이 없는 기존 흐름에서 같다 |
| 토큰 갱신 | `AuthSession(idToken, uid)`는 그대로, 갱신 토큰·만료 시각은 `AnonymousAuth` 안에 따로 | `assertEquals(AuthSession("token-1", "uid-1"), …)`가 기존 테스트에 있다 |

- `HospitalTripTest` 130행 `assertEquals(TripState(candidate, CardDelivery.Sent(toServer = true)), …)`: 그 시점의 길안내는 남은 시간 모름(null)이라 `startMinutes`가 null로 남는다 → 그대로 통과한다. `startMinutes`를 **목록의 이동 어림으로 채우지 않는 이유**이기도 하다 (어림은 길안내 남은 시간이 아니다).
- `NaviRouteOnlyGuidanceSourceTest`의 `` `시연 길안내가 도착하면 도착이고 내비 경로도 끝낸다` ``는 "구독이 끊겼다(`closedTrips == 1`)"를 본다. U2 뒤에도 도착하면 구독은 끊는다(연결 해제). 경로만 남긴다. 그래서 그대로 통과한다. 이름이 이제 조금 어긋나지만 기존 테스트는 고치지 않는다 (결정 / 배운 점에 적는다).
- 화면 문구: 카드 머리·주행 화면 문구를 보는 JVM 테스트는 없다 (Compose UI 테스트 없음). `MedicalCardTripFlowTest` `` `안내 중에 취소하면 생성 완료 카드로 돌아간다` ``는 상태만 본다. 안 쓰게 된 `strings.xml` 항목은 지운다 (`lintDebug`의 UnusedResources, 012와 같다).

### 2. U2: 경로를 남기고 연결만 놓기

```kotlin
// data/guidance/navi/NaviClient.kt
/** 안내 구독을 끊을 때 내비 경로를 어떻게 할지 (013 U2) */
enum class RouteOnClose {
    /** 경로를 취소한다 (지금 동작). 내비 서비스가 내비 앱을 앞으로 띄운다 (`[obs-navi-emu]`, 013 U3) */
    CANCEL,
    /** 경로를 내비에 남기고 리스너 해제·release만 한다. 내비 앱이 앞으로 나오지 않는다 */
    KEEP,
}

interface NaviClient {
    fun trip(destination: Destination): Flow<NaviEvent>   // 그대로 (= 항상 CANCEL)

    /**
     * [trip]과 같다. 다만 구독이 끊길 때(도착·취소 이벤트로 이미 끝난 경우 제외) [onClose]를 한 번 불러
     * KEEP이면 `cancelRoute`를 부르지 않는다. 기본 구현은 [trip] (항상 취소).
     */
    fun trip(destination: Destination, onClose: () -> RouteOnClose): Flow<NaviEvent> = trip(destination)
}
```

- `PleosNaviClient`: 2인자를 구현하고 1인자는 `trip(destination) { RouteOnClose.CANCEL }`. `awaitClose`에서 `if (!finished.get() && onClose() == RouteOnClose.CANCEL) cancelRoute()`.
- `NaviRouteOnlyGuidanceSource`: 안내(`Trip`)마다 `@Volatile var keepRouteOnClose = false`. `followNavi`는 `navi.trip(d) { if (trip.keepRouteOnClose) KEEP else CANCEL }`.

| 일 | 구독을 끊을 때 | 근거 |
|---|---|---|
| 시연 진행이 도착 (`simulate`의 arrived) | **KEEP** (플래그를 세운 뒤 `naviJob.cancel()`) | U2. 시연 도착 순간 MeongCare가 가려지지 않게 |
| 도착 뒤 [닫기] (`dismissArrival`) | **KEEP** (이미 끊겼으면 아무 일 없음) | U2 |
| 안내 중 앱 [안내 취소] (`cancel`) | CANCEL | U2 "앱의 [안내 취소]는 지금처럼" |
| 경로 기다리는 중 `cancel` | CANCEL | 같다 |
| 안내 중 다른 목적지로 `start` (이전 안내 닫기) | CANCEL | 010: 이전 경로 취소가 새 요청보다 늦지 않게. 지금 동작 그대로 |
| 내비에서 취소 (`Cancelled` 이벤트) | (상관없음) 어댑터의 `finished`가 true라 `cancelRoute`를 부르지 않는다 | 지금 그대로 |

- 경로를 남긴 뒤 다음 출발: 새 `requestRoute` → 이전 경로의 `onRouteCancelled`가 `onRouteStarted` 전에 온다 → 어댑터가 거른다 (`[obs-navi-emu]`, 010에서 확인). S7에서 다시 본다.
- `NaviGuidanceSource`(Real, 전부 내비)는 바꾸지 않는다.
- 이 변경은 Pleos API를 새로 쓰지 않는다 (`cancelRoute`를 덜 부를 뿐). `release()`만으로는 내비가 앞에 나오지 않는 것은 011 관찰(위치용 NaviHelper release 뒤 경로 그대로)로 보아 맞을 것이나, S5·S6에서 확인한다 (O1).

### 3. 도착 예정 갱신 간격과 확인 읽기 간격 (`di/TripTiming.kt`, 순수 함수)

```kotlin
data class TripTiming(
    /** 도착 예정을 다시 보내는 최소 간격 */
    val etaInterval: Duration,
    /** 병원 확인을 읽는 간격 (단계 5에서 더한다) */
    val ackPollInterval: Duration,
)
fun tripTimingOf(isGuidanceSimulated: Boolean): TripTiming
```

| 길안내 | `etaInterval` | `ackPollInterval` | 근거 |
|---|---|---|---|
| 시연용 (`sourcePlan.isGuidanceSimulated`: Fake·시연, 경로만 내비 포함) | **4초** | **4초** | 시연 진행은 2초에 1분이 준다 (`FakeGuidanceTiming`). 4초 = 2칸 = 2분씩 바뀐다. 관찰 2(60초 간격이면 37분 → 8분으로 튐)를 없앤다 |
| 실제 (Real) | 60초 (지금 그대로, K8) | 15초 | 실제 남은 시간은 1분에 1분씩 준다. 확인은 사람이 누르므로 15초면 충분하고 폴링을 줄인다 (ux-guidelines "폴링 최소화") |

- `AppContainer`가 `tripTimingOf(sourcePlan.isGuidanceSimulated)`를 `HospitalTrip(etaInterval = …, ackPollInterval = …)`에 넘긴다. `HospitalTrip`의 기본값은 Real 값(60초, 15초)으로 둔다 → 기존 테스트 그대로.
- `SourcePlan`은 고치지 않는다 (있는 `isGuidanceSimulated`를 쓴다).

### 4. U1: 병원 카드 확인 (차 쪽 계약)

```kotlin
// domain/trip/CaseSender.kt
sealed interface AckRead {
    /** 이 전송 방식은 확인을 읽지 않는다 (Fake: 내장 시연 병원) → 읽기를 멈춘다. 로그 없음 */
    data object Unsupported : AckRead
    /** 아직 확인 전 */
    data object Pending : AckRead
    /** 병원이 확인했다 (서버 시각) */
    data class Acknowledged(val at: Instant) : AckRead
    /** 건이 없거나 읽을 수 없다 (병원 [완료]로 지움, 다른 계정. HTTP 404·403) → 멈춘다 */
    data object Gone : AckRead
    /** 이번에는 읽지 못했다 (네트워크, 401·5xx, 로그인 실패, 형식 오류). [reason]은 로그용 */
    data class Failed(val reason: String) : AckRead
}

interface CaseSender {
    // send / updateEta / finish 그대로
    /** 이 건의 병원 확인을 한 번 읽는다 (docs/data/emergency-case.md `acknowledgedAt`). 던지지 않는다 */
    suspend fun readAck(ref: CaseRef): AckRead = AckRead.Unsupported
}

// domain/trip/HospitalTrip.kt
data class CardDelivery.Sent(val toServer: Boolean, val acknowledgedAt: Instant? = null)
data class TripState(
    val candidate: HospitalCandidate,
    val delivery: CardDelivery,
    /** 진행 막대의 시작: 이번 안내에서 본 가장 큰 남은 분. 아직 모르면 null */
    val startMinutes: Int? = null,
)
class HospitalTrip(..., etaInterval: Duration = 60.seconds, ackPollInterval: Duration = 15.seconds)
```

**확인 읽기 규칙 (`HospitalTrip`, tester 기준)**

| 경우 | 동작 |
|---|---|
| 언제 시작 | 카드 전송이 `Sent`가 되고 `candidate.sendsCardToServer`가 true일 때. 처음 읽기는 보낸 뒤 `ackPollInterval` 뒤 (바로 읽지 않는다) |
| 읽지 않는 경우 | 내장 병원(Fake 전송), [카드 없이 길안내만](`NotSent`), 보내는 중, 전송 실패로 [다시 보내기]를 기다리는 동안. 다시 보내기로 성공하면 그때부터 같은 규칙 |
| `Pending` | 다음 간격에 다시 |
| `Acknowledged(at)` | `delivery = Sent(toServer = true, acknowledgedAt = at)`, 읽기를 멈춘다. 로그 `병원 확인: 확인함` (한 번) |
| `Failed(reason)` | 다음 간격에 다시. 로그 `병원 확인: 읽기 실패 (reason)`는 **이유가 바뀔 때만** (같은 실패가 이어지면 한 줄) |
| `Gone` | 멈춘다. 화면은 그대로("보냈어요"). 로그 `병원 확인: 건을 읽을 수 없음` 한 줄 |
| `Unsupported` | 멈춘다. 로그 없음 |
| 도착·취소 (phase가 끝남) | 멈춘다. 그 순간 진행 중인 읽기는 취소하고 결과를 쓰지 않는다. 그 뒤 읽기 0회 |
| [닫기] (`clear`) | 멈춘다. trip이 null |
| 확인된 뒤 도착 | 확인 표시는 남는다 (도착 카드에도 보인다). `clear`까지 |
| 확인된 뒤 취소 | 지금처럼 고른 병원을 비운다 (확인 표시도 사라진다) |

- 앱 범위 코루틴에서 돈다. 내비가 앞에 있어 MeongCare가 백그라운드여도 읽는다 (돌아왔을 때 주행 화면에 바로 보이게). 안내 한 번에 묶여 있고 도착·확인되면 멈추므로 상주 폴링이 아니다 (결정에 적는다).
- 로그에 시각·uid·병원 이름·반려견 정보를 넣지 않는다.

**진행 시작값 규칙 (`TripState.startMinutes`)**

| 길안내 상태 (출발한 trip 안에서) | `startMinutes` |
|---|---|
| 출발 직후 | null |
| `Guiding(남은 시간 null)` (경로를 찾는 중) | 바꾸지 않는다 |
| `Guiding(t)`이고 지금 값이 null | `t.toCaseMinutes()` |
| `Guiding(t)`이고 `t.toCaseMinutes()` > 지금 값 | 큰 값으로 올린다 (재탐색·내비 첫 값이 어림보다 클 때) |
| 그보다 작거나 같다 | 바꾸지 않는다 |
| 도착 | 그대로 둔다 |

### 5. 차 쪽 서버 읽기 (`FirestoreCaseSender.readAck`)

- `GET {documentsUrl}/hospitals/{hid}/cases/{cid}?mask.fieldPaths=acknowledgedAt` + `Authorization: Bearer` (F3).
- 보안 규칙상 자기 건(`createdBy == uid`)은 읽힌다. **그래서 토큰 갱신은 같은 uid를 지켜야 한다** (설계 결정 8).

| 응답 | 결과 |
|---|---|
| 200, `acknowledgedAt`이 `timestampValue` | `Acknowledged(at)` |
| 200, `acknowledgedAt`이 `nullValue`이거나 없음 | `Pending` |
| 200, `acknowledgedAt`이 다른 형식 | `Failed("형식 오류")` (짐작하지 않는다) |
| 200, JSON이 아님 | `Failed("형식 오류")` |
| 404, 403 | `Gone` (지운 건을 읽으면 규칙이 `resource.data`를 볼 수 없어 403일 수 있다 → F3에서 실제 값을 적는다) |
| 그 밖의 HTTP (401, 5xx 등) | `Failed("HTTP n")` |
| `IOException` | `Failed("네트워크")` |
| 익명 로그인 실패 | `Failed("익명 로그인 실패")` |

- `FirestoreJson`에 `ackOf(responseBody): …`를 둔다 (JVM 테스트).

### 6. 보안 규칙 강화와 점검 R9~R13 (`firebase/firestore.rules`)

직원의 확인 쓰기 갈래를 이렇게 바꾼다:

```
|| (isStaff(hid)
    && request.resource.data.diff(resource.data).affectedKeys().hasOnly(['acknowledgedAt'])
    && (!('acknowledgedAt' in resource.data) || resource.data.acknowledgedAt == null)   // 한 번만
    && request.resource.data.acknowledgedAt == request.time)                            // 서버 시각만
```

- 차 갈래는 그대로다 (`acknowledgedAt`을 바꿀 수 없다).
- `check-rules.mjs`에 더한다 (머리 주석 R1~R8 → R1~R13):
  - R9 직원은 확인 전 건의 `acknowledgedAt`을 서버 시각으로 쓴다 → 200 (REST `documents:commit` + `updateTransforms` `REQUEST_TIME`, F4)
  - R10 직원이 `acknowledgedAt`에 임의 시각(클라이언트 값)을 쓰면 → 403
  - R11 이미 확인된 건의 `acknowledgedAt`은 서버 시각으로도 다시 못 바꾼다 → 403
  - R12 차 계정은 자기 건의 `acknowledgedAt`을 쓰지 못한다 → 403. 자기 건을 읽으면 `acknowledgedAt`이 `timestampValue`로 보인다 → 200
  - R13 직원이 지운 건을 차가 읽으면 403 또는 404다 (값을 출력하고 둘 중 하나면 PASS. 설계 결정 5의 `Gone`)
- **에이전트는 Firebase 에뮬레이터를 직접 띄우지 않는다.** 012 때 Claude 세션이 `[::1]`에 띄우고 끄지 않은 에뮬레이터 때문에 차와 병원 웹이 서로 다른 서버에 붙었다 (관찰 1). 규칙 점검은 사용자 터미널의 에뮬레이터(127.0.0.1)에 대고 돌린다. 꼭 띄워야 하면 끝날 때 반드시 끄고 `preflight`로 확인한다.

### 7. 정차 중 안내 영역과 주행 화면 (차 UI)

**상태 계산 (`ui/guidance/TripPanel.kt`, 순수 함수, JVM 테스트)**

```kotlin
data class TripPanel(
    val destinationName: String,
    /** null = 경로를 찾는 중 */
    val remainingMinutes: Int?,
    /** 도착 예정 = now + 남은 분, 분 아래 버림 (병원 웹 clockText와 같다). 남은 분을 모르면 null */
    val arrivesAt: LocalTime?,
    /** 0.0(출발) ~ 1.0. 시작값이나 남은 분을 모르면 null (막대를 숨긴다) */
    val progress: Float?,
    /** 출발할 때 받은 병원 상태 (대기 분·혼잡도). 없거나 trip이 없으면 null */
    val statusAtDeparture: HospitalStatus?,
    val delivery: DeliveryLine,
    val arrived: Boolean,
)
sealed interface DeliveryLine { None; Sending; SentWaiting; Acknowledged(at: LocalTime); Sample; Failed; NotSent }

fun tripPanelOf(guidance: GuidanceUiState, trip: TripState?, now: Instant, zone: ZoneId): TripPanel?
```

| 입력 | 결과 |
|---|---|
| `GuidanceUiState.Idle` | null (패널 없음) |
| `Guiding(name, null)` | `remainingMinutes = null`, `arrivesAt = null`, `progress = null` |
| `Guiding(name, m)` | `arrivesAt = (now + m분)`의 시:분 (zone, 자정 넘김 포함), `progress = ((start − m) / start)`을 0~1로 자름, start가 null·0 이하면 null |
| `Arrived(name)` | `arrived = true`, `progress = null`, `arrivesAt = null` |
| trip == null | `delivery = None`, `statusAtDeparture = null`, `progress = null` |
| `delivery`: `Sending` / `Sent(true, null)` / `Sent(true, at)` / `Sent(false, _)` / `Failed` / `NotSent` | `Sending` / `SentWaiting` / `Acknowledged(at의 시:분)` / `Sample` / `Failed` / `NotSent` |
| 이름 | 길안내 상태의 목적지 이름을 쓴다 |

- `ui`는 `domain`의 `TripState`·`HospitalStatus`만 안다 (data·di import 없음).

**정차 화면 (안내 영역 컴포넌트 `ui/guidance/GuidancePanel.kt`)**

- 카드 화면 (`MedicalCardScreen`):
  - 머리: 안내 없음은 "진료 카드 생성 완료"(그대로), **안내 중·도착은 "진료 카드"** + 같은 부제(반려견 이름). [홈] [수정] 그대로.
  - 머리 아래 **안내 영역** (안내 중·도착일 때만):
    - 안내 중: 목적지 이름, 큰 남은 분("15분", 경로를 찾는 중이면 "경로를 찾는 중"), "도착 예정 14:32", 진행 막대(정적), "출발 때 알린 대기 10분 · 보통"(있으면), 전송·확인 줄 + [다시 보내기](실패일 때).
    - 도착: "○○에 도착했어요"(크게) + "이 카드를 접수처에 보여 주세요" + 확인 줄(확인됐으면).
    - 넓은 화면(≥ 840dp): 왼쪽 목적지·도착 예정·대기, 오른쪽 큰 남은 분, 아래 막대와 전송 줄. 좁은 화면: 세로로.
  - 전송·확인 줄 문구: 보내는 중 "진료 카드를 보내는 중…", 서버 보냄 "진료 카드를 병원에 보냈어요 · 확인을 기다리는 중", **확인 "병원이 진료 카드를 확인했어요 (14:05)"**, 시연 "시연용: 실제 병원에는 보내지 않았어요"(그대로), 실패 "보내지 못했어요" + [다시 보내기](그대로), 카드 없이 "카드 없이 출발했어요"(그대로).
  - **[병원에 전화] [안내 취소]는 지금처럼 화면 아래 고정 줄에 둔다** (K2: 카드 몸통을 스크롤해도 늘 보인다. 안내 영역 안에 같은 버튼을 또 두지 않는다).
  - 시연 문구(`DemoNotices`)는 지금처럼 카드 아래.
- 처음 화면 (`ParkedScreen`의 `CurrentCardPanel`): 안내 중·도착이면 **같은 `GuidancePanel`** + 주요 증상 줄 + [카드 보기]. 안내 없음(카드만 있음)은 지금 그대로("만든 진료 카드").
- 도착 예정 시각은 정차 화면에서만 30초마다 다시 계산한다 (P에 서 있는 동안 시각이 낡지 않게). 주행 화면에는 시각을 넣지 않는다.

**주행 화면 (`DrivingScreen`, U4)**

| 길안내 | 보이는 것 |
|---|---|
| 안내 없음 | "주행 중" + 기존 힌트 (그대로) |
| 안내 중, 남은 분 앎 | **아주 큰 숫자 "15"** + "분" / "{목적지}까지" / 진행 막대(정적, 두께 16dp 이상) / (확인됐으면) "병원이 카드를 확인했어요" |
| 안내 중, 경로를 찾는 중 | 큰 글씨 "경로를 찾는 중" / "{목적지}까지" / 막대 없음 |
| 도착 | "도착했어요" + "정차하면 진료 카드를 볼 수 있어요" (그대로) |

- 버튼·입력·스크롤·애니메이션 없음. 진행 막대는 `ui/guidance/StaticProgressBar.kt` 한 파일에 두고 정차·주행 화면이 같이 쓴다. `animate*` 없이 값 그대로 그린다 (Material 진행 표시 대신 Box 두 개로 그려도 된다).
- 시연용 표시를 넣지 않는다 (규칙 3, 010·012와 같다). 전송 상태 줄도 넣지 않는다 — **확인됐을 때 한 줄만** (U4).
- 큰 숫자 크기: 넓은 화면(앱 영역 1682dp)·좁은 화면(< 600dp) 두 값을 `Type.kt`/`Dimens`에 이름 붙여 둔다. `/verify`로 정한다. 좁은 화면에서 숫자·목적지가 잘리거나 두 줄로 깨지지 않는다 (목적지는 `maxLines = 2` + 말줄임).
- 연결: `HomeScreen`이 `tripPanelOf(guidanceUiState, cardExtras.trip, now, zone)`를 계산해 `DrivingScreen(…, panel = …)`과 정차 화면에 넘긴다. `CardExtras`에 `trip: TripState? = null`을 더하고 `MainActivity`가 `tripState`를 넘긴다.

### 8. 익명 토큰 갱신 (`AnonymousAuth`, 012 있으면 좋음 C)

- `ServerConfig`에 `refreshUrl`을 더한다 (`serverConfigOf`가 채운다): debug 에뮬레이터 `http://<host>:9099/securetoken.googleapis.com/v1/token?key=<key>`, release `https://securetoken.googleapis.com/v1/token?key=<key>` (F1). release는 에뮬레이터 주소를 쓰지 않는다 (지금 규칙 그대로).
- `AnonymousAuth(config, transport, now: () -> Instant = Instant::now, refreshBefore: Duration = 5.minutes, log: (String) -> Unit = {})`. `session(): AuthSession?`은 그대로.
- 가입 응답에서 `refreshToken`, `expiresIn`(초, 문자열·숫자 모두, 1~86400만 인정)을 더 읽는다 (F2). 받은 순간 `expiresAt = now + expiresIn`.

| 상태 | `session()` |
|---|---|
| 자격 없음 | 가입 (지금 그대로) |
| 만료 시각 모름 (`expiresIn` 없음·틀림) | 그대로 돌려준다 (갱신 안 함 = 지금 동작. 기존 테스트의 가짜 응답이 이 경우다) |
| `now < expiresAt − 5분` | 그대로 |
| `now ≥ expiresAt − 5분` (**정확히 5분 전 포함**) | 갱신 시도 |
| 갱신 성공 | 새 `id_token`·`expires_in`·`refresh_token`(없으면 이전 것). **uid는 그대로** (`user_id`가 있는데 다르면 형식 오류로 본다) |
| 갱신 실패 (네트워크·5xx·형식 오류), 아직 만료 전 | 지금 토큰을 돌려준다. 다음 부를 때 다시 시도 |
| 같은 실패, 이미 만료 | null (이번 요청 실패). 자격은 지우지 않는다 (다음에 다시 갱신) |
| 갱신 거부 HTTP 400 (갱신 토큰 무효: 에뮬레이터 재시작 등), 또는 갱신 토큰이 없는데 만료됨 | 자격을 버리고 새로 가입 (새 uid). 진행 중인 환자 건은 더 못 고친다 (그 건은 이미 사라진 경우가 대부분) |
| 동시에 여럿이 부름 | 갱신·가입은 한 번 (`Mutex`, 지금 그대로) |

- 요청: `POST refreshUrl`, `Content-Type: application/x-www-form-urlencoded`, 몸통 `grant_type=refresh_token&refresh_token=<URL 인코딩>`. **갱신 토큰을 URL에 넣지 않는다** (ux-guidelines "인증 토큰은 URL에 노출되지 않게"). API 키는 지금처럼 `?key=`.
- 로그(`HospitalTrip` 태그): `익명 토큰 갱신: 성공`, `익명 토큰 갱신: 실패 (네트워크)`, `익명 토큰: 다시 가입 (갱신 거부)`. 토큰·uid는 넣지 않는다.
- 401 응답에 반응해 한 번 다시 시도하는 것은 있으면 좋음 C다 (미리 갱신으로 충분하다고 본다).

### 9. 병원 웹 (`web/`)

```ts
// web/src/domain/hospital/hospitalDesk.ts 에 더한다
export type DeskConnection = 'local' | 'checking' | 'live' | 'offline'
export interface ServerInfo { label: string; warning: string | null }

export interface HospitalDesk {
  // 기존 그대로 +
  /** 어느 서버에 붙었는지 (화면 위에 늘 보인다). 메모리 구현은 null */
  readonly server: ServerInfo | null
  /** 실시간 연결 상태. 메모리 구현은 바로 'local' 한 번 */
  watchConnection(hospitalId: string, onChange: (state: DeskConnection) => void): () => void
  /** [카드 확인]: acknowledgedAt = 서버 시각. 이미 확인된 건은 바꾸지 않는다 */
  acknowledgeCase(hospitalId: string, caseId: string): Promise<DeskResult<void>>
}
```

- `serverInfoOf({ projectId, emulatorHost })` (`web/src/domain/hospital/serverInfo.ts`, 순수, firebase import 없음):
  - 에뮬레이터: `에뮬레이터 127.0.0.1:8080 · 프로젝트 demo-meongcare`. 진짜 프로젝트: `Firebase 프로젝트 <id>`.
  - **경고**: 호스트가 `localhost`·`::1`·`[::1]`이면 "localhost·::1은 IPv6 에뮬레이터에 붙을 수 있어요. 차 앱(adb reverse)과 같은 서버를 보려면 127.0.0.1로 맞춰 주세요." (관찰 1의 원인을 화면에서 바로 보이게)
- 연결 상태 (Firestore 구현): 병원 문서를 `onSnapshot(…, { includeMetadataChanges: true })`로 듣고 `metadata.fromCache`가 false면 `live`, true면 `checking`, 리스너 오류면 `offline` (F5). 문구: 실시간 연결됨 / 서버 연결 확인 중… / 서버 연결이 끊겼어요. 새로 고쳐 주세요.
- 카드 확인: Firestore는 `updateDoc(case, { acknowledgedAt: serverTimestamp() })`. 메모리는 `now()`를 쓰고, 이미 확인된 건은 시각을 바꾸지 않고 `ok`, 없는 건은 `not-found`.
- 새 환자 (순수 함수, `web/src/domain/hospital/caseAck.ts`):
  - `isNewCase(c)` = 읽을 수 있고 && 취소 아님 && `acknowledgedAt === null`
  - `newCaseCount(cases)`
  - `pageTitle(appName, hospitalName | null, newCount)` → 새 환자가 있으면 `(2) 새 환자 · 시연용 바른 동물병원 · MeongCare 병원`, 없으면 `시연용 바른 동물병원 · MeongCare 병원`, 병원 전이면 `MeongCare 병원`
  - `ackText(at)` → `카드 확인함 14:05` (clockText와 같은 버림)
- 화면:
  - 새 환자 카드: 강조 테두리 + "새 환자" 표시. 깜빡임·소리 없음.
  - 오는 중·도착 건에 큰 **[카드 확인]** 버튼 (확인 전만). 누르면 "확인 중…"으로 막고, 스냅샷에 확인이 오면 버튼 대신 "카드 확인함 14:05". 실패하면 "확인을 저장하지 못했어요. 다시 눌러 주세요." (이미 스냅샷이 확인이면 실패를 보이지 않는다). 취소 건에는 버튼이 없다.
  - 탭 제목: `document.title = pageTitle(…)`, 화면을 떠나면 원래 제목으로.
  - 위 안내 줄 아래 "서버: … · 실시간 연결됨" 한 줄 (+ 경고가 있으면 경고). 메모리 모드는 지금 안내 그대로.
  - 목록 순서(`sortCases`)는 바꾸지 않는다.

### 10. 시연 전 점검 스크립트 (`firebase/scripts/preflight.mjs`, `npm --prefix firebase run preflight`)

의존성 없는 Node 스크립트 (check-rules와 같은 방식, `emulator.mjs` 도우미 사용). **`web/.env*`, `secrets.properties`를 읽지 않는다.**

| # | 검사 | PASS | FAIL 때 안내 |
|---|---|---|---|
| PF1 | `127.0.0.1:8080` Firestore가 2초 안에 응답 | 응답 있음 | `npm --prefix firebase run emulators`를 켠다 |
| PF2 | `127.0.0.1:9099` Auth가 2초 안에 응답 | 응답 있음 | 같다 |
| PF3 | 차와 같은 방법(익명 가입 → 병원 목록)으로 시연 병원 5곳 이상 | 5곳 이상 | `npm --prefix firebase run seed -- --reset` |
| PF4 | `[::1]:8080`·`[::1]:9099`에 **다른** 에뮬레이터가 없다 | 응답 없음, 또는 같은 서버 (127.0.0.1에 관리자 토큰으로 쓴 표식 문서 `_preflight/<임의값>`이 `[::1]`에서도 보임) | "다른 에뮬레이터가 [::1]에 떠 있다. 그 터미널을 끈다" + `netstat -ano | findstr :8080`으로 pid 찾는 법. 표식 문서는 끝나면 지운다 |
| PF5 | `adb reverse --list`에 `tcp:8080 tcp:8080`, `tcp:9099 tcp:9099` (`ANDROID_SERIAL`을 따른다) | 둘 다 | 명령 두 줄 안내. adb가 없거나 기기가 여럿이면 FAIL + `ANDROID_SERIAL` 안내 |

- 하나라도 FAIL이면 종료 코드 1. 마지막에 "병원 웹 화면 위 '서버:' 줄이 127.0.0.1:8080인지, 차 앱 `FIREBASE_EMULATOR_HOST`가 127.0.0.1인지 눈으로 확인한다 (이 스크립트는 그 파일들을 읽지 않는다)"를 출력한다.
- 함께 제안 (K9): `firebase/firebase.json`의 `emulators.auth`·`emulators.firestore`에 `"host": "127.0.0.1"`을 넣어 IPv4로 고정한다 (F6). 그러면 `localhost`로 붙은 병원 웹은 IPv6에 아무것도 없어 "연결 끊김"이 보인다 → 잘못 붙은 것이 숨지 않는다.

### 11. 권한

- **새 `pleos.car.permission.*`: 없다.** 쓰는 Pleos API는 이미 승인된 `requestRoute`·`cancelRoute`(`NAVI_ROUTE_SEARCH`)와 `getCurrentLocationInfo`(`NAVI_ROUTE`)뿐이고, U2는 `cancelRoute`를 덜 부를 뿐이다.
- 일반 Android 권한도 더하지 않는다. U3의 `REORDER_TASKS`는 쓰지 않는다 (spike 결론).
- 구현 중 `SecurityException`이나 다른 권한이 필요하다는 근거가 보이면 **멈추고 묻는다.**

## 확인 필요 (시작 전에 해결)

### [사용자 승인 필요]

- **없다.** U1~U4와 메인 결정은 2026-11-06에 확정됐다 (위 "사용자·메인 결정"). 새 권한도 없다.

### [선행: pleos-docs-researcher]

- **없다.** 새 Pleos API를 쓰지 않는다. U2의 근거(이전 경로의 `onRouteCancelled`를 어댑터가 거름)와 U3의 근거는 에뮬레이터 관찰(`[obs-navi-emu]`)이다.

### 기획상 판단 (planner 기본값으로 진행한다. 바꾸려면 사용자가 알려 준다)

- K1. 갱신 간격: 시연 도착 예정 4초·확인 읽기 4초, 실제 도착 예정 60초·확인 읽기 15초 (설계 결정 3).
- K2. 카드 화면의 [병원에 전화] [안내 취소]는 안내 영역 안이 아니라 지금처럼 화면 아래 고정 줄 (스크롤해도 보인다). 메인 안("안내 영역 안에 버튼")과 다르다.
- K3. 병원 대기 정보는 "출발 때 알린 대기 10분 · 보통" (출발할 때 받은 값). 안내 중에는 목록을 다시 받지 않는다 (자동 새로 고침 안 함 결정과 맞춘다).
- K4. 진행 막대 시작값 = 이번 안내에서 본 가장 큰 남은 분 (목록 어림이 아니라 길안내 값).
- K5. 병원 웹 "새 환자" = 확인 전이고 취소되지 않은 건. 목록 순서는 바꾸지 않는다.
- K6. 주행 화면 확인 줄 문구 "병원이 카드를 확인했어요", 카드 화면 "병원이 진료 카드를 확인했어요 (14:05)".
- K7. 토큰은 만료 5분 전에 미리 갱신, 401 재시도는 있으면 좋음.
- K8. 확인 읽기는 MeongCare가 백그라운드여도 안내 동안 계속한다 (주행 화면으로 돌아왔을 때 바로 보이게).
- K9. `firebase.json`에 에뮬레이터 host `127.0.0.1` 고정 (설계 결정 10).

### 구현 중 확인 (Firebase 공식 문서·에뮬레이터. Pleos가 아니라 규칙 1 대상은 아니지만 추측으로 쓰지 않는다)

| # | 확인할 것 | 안 되면 |
|---|---|---|
| F1 | securetoken 갱신 REST: 주소(에뮬레이터 `:9099/securetoken.googleapis.com/v1/token?key=`), 몸통 형식(form), 응답 필드(`id_token`, `refresh_token`, `expires_in`, `user_id`). 에뮬레이터에 Node 한 줄로 해 본다 | 주소·필드 이름만 고친다. 에뮬레이터가 지원하지 않으면 에뮬레이터에서는 갱신을 건너뛰고(만료 모름 취급) 노트에 적는다 |
| F2 | `accounts:signUp` 응답의 `refreshToken`, `expiresIn` 이름과 형식 | 같다 |
| F3 | Firestore REST 단일 문서 GET + `mask.fieldPaths=acknowledgedAt`. 지운 건을 차가 읽을 때 상태 코드(403/404) | 마스크 없이 전체를 받아 `acknowledgedAt`만 읽는다 |
| F4 | REST `documents:commit`의 `updateTransforms` `setToServerValue: REQUEST_TIME` (R9·R11용) | R9·R11은 병원 웹(JS SDK)으로 손으로 해 보고 진행 로그에 적는다 |
| F5 | JS SDK `onSnapshot(ref, { includeMetadataChanges: true })`의 `metadata.fromCache`로 서버 연결 여부를 알 수 있는지, 에뮬레이터를 끄면 몇 초 만에 바뀌는지 | 연결 상태는 빼고 서버 주소 줄만 남긴다 (자르는 순서 3) |
| F6 | `firebase.json` `emulators.<이름>.host` 설정 | 넣지 않고 preflight PF4로만 막는다 |

### 관찰로 확인 (단계 12)

| # | 볼 것 | 결과가 바꾸는 것 |
|---|---|---|
| O1 | U2 뒤 시연 도착·[닫기] 때 `release()`만으로 내비가 앞에 나오지 않는가 | 나오면 sdk-notes에 적고 사용자에게 알린다 (U2 효과가 없다는 뜻) |
| O2 | 경로를 남긴 뒤 다음 출발에서 앱 안내가 이전 경로의 취소로 끝나지 않는가 (S7) | 끝나면 어댑터의 거르기 규칙을 다시 본다 (`fix(data)`) |
| O3 | 병원 웹 갱신 간격 (S3), 확인이 차에 뜨는 시간 (S4) | K1 값 조정 |
| O4 | 에뮬레이터를 끄고 켤 때 병원 웹 연결 표시가 바뀌는 시간 (S8) | 문구 조정 |

## 확인 절차

- adb 규칙은 012와 같다: `ANDROID_SERIAL`(Pleos `emulator-5554`, 일반 폰 `emulator-5556`), 기어는 006 노트의 `vc()`, 스크린샷·UI 트리·로그는 `.claude/.cache/p13-<번호>.*`(커밋하지 않음). **에이전트는 Android 에뮬레이터도 Firebase 에뮬레이터도 직접 띄우지 않는다.** 꺼져 있으면 사용자에게 켜 달라고 한다.
- 로그: `adb logcat -d -s HospitalTrip`, 내비 SDK: `adb logcat -d | grep "\[NaviHelper\]\[SDK\]"`, 우리 앱만: `adb logcat -d --pid=$(adb shell pidof com.meongcare.app)`.
- 준비 (PC, 사용자 터미널):
  1. `npm --prefix firebase run emulators` → `npm --prefix firebase run seed -- --reset`
  2. `adb reverse tcp:8080 tcp:8080`, `adb reverse tcp:9099 tcp:9099` → `npm --prefix firebase run preflight` 모두 PASS
  3. 병원 웹 개발 서버 (`VITE_FIREBASE_EMULATOR_HOST=127.0.0.1`은 사용자가 확인. 에이전트는 `web/.env*`를 읽지 않는다) → `http://localhost:5173/hospital/?h=demo-fast&code=fast-1234`
  4. 차: `secrets.properties` 에뮬레이터 값(`127.0.0.1`) → debug 설치 → debug 패널 시연. Pleos는 Cold boot 뒤 내비 화면을 한 번 띄운다 (011).

### Pleos `emulator-5554` + 병원 웹 나란히 (debug 시연, 기어 P에서 시작)

| # | 조작 | 기대 |
|---|---|---|
| S0 | 병원 웹을 연다 | 위에 "서버: 에뮬레이터 127.0.0.1:8080 · 프로젝트 demo-meongcare · 실시간 연결됨". 경고 없음 |
| S1 | 차: 카드 → [가장 빨리 진료받을 곳 찾기] → 1위(바른) → [출발] | 내비가 앞에 나온다. 병원 웹에 5초 안에 "보리" 카드, **새 환자 강조**, 탭 제목 "(1) 새 환자 · …" |
| S2 | GNB의 MeongCare 아이콘 → 카드 화면 | 머리 "진료 카드". 안내 영역: 목적지, 큰 남은 분, "도착 예정 HH:mm", 막대(처음 0), "출발 때 알린 대기 10분 · 보통", "진료 카드를 병원에 보냈어요 · 확인을 기다리는 중". 아래 [병원에 전화] [안내 취소] |
| S3 | 기어 D | 주행 화면: 큰 숫자 + "시연용 바른 동물병원까지" + 막대가 2초마다 늘어난다. 병원 웹 "약 N분 후"가 약 4초마다 바뀐다. 로그 `도착 예정 갱신: 성공` 간격 4~6초 (O3). UI 트리: debug 패널 영역 빼고 `EditText` 0, 클릭 노드 0, 스크롤 노드 0 |
| S4 | 병원 웹 [카드 확인] | 병원 웹: 강조가 사라지고 "카드 확인함 HH:mm", 탭 제목에서 숫자가 빠진다. 차 주행 화면: 8초 안에 "병원이 카드를 확인했어요". 로그 `병원 확인: 확인함` 뒤로 확인 읽기 로그 없음 (O3) |
| S5 | 시연 도착까지 기다림 | **MeongCare가 앞에 남아** 주행 화면 "도착했어요". 내비 빈 지도("어디로 갈까요?")가 아니다. 그 순간 logcat에 `RequestRouteCancel`·내비 `MainActivity` START 없음 (O1). 병원 웹 "도착했어요" |
| S6 | 기어 P → 도착 카드 → [닫기] | 안내 영역 "시연용 바른 동물병원에 도착했어요 · 이 카드를 접수처에 보여 주세요" + 확인 줄. [닫기] → 처음 화면. 내비가 앞에 나오지 않는다. GNB 내비에는 그 경로가 남아 있다 (캡처) |
| S7 | 다시 찾기 → 2위 병원 → [출발] | 내비가 새 경로로 바뀐다. 앱 안내가 이전 경로 취소로 끝나지 않고 "경로를 찾는 중" → 남은 분 (O2). → P에서 [안내 취소] → **내비가 앞에 나온다 (U3 알려진 한계)**, 병원 웹 "취소됨" |
| S8 | 사용자가 Firebase 에뮬레이터를 끈다 → 다시 켜고 seed | 병원 웹 연결 표시가 "확인 중/끊김"으로 바뀌고, 다시 켜면 돌아오거나 새로 고침 안내 (O4) |
| S9 | 에뮬레이터를 끈 채 차에서 찾기 → 출발 | "서버에 연결하지 못해 시연용 병원 정보를 보여 줘요" → 전송 줄 "시연용: 실제 병원에는 보내지 않았어요", 확인 줄 없음, `병원 확인` 로그 0 |
| S10 | 다크 모드 (`adb shell cmd uimode night yes`)에서 S2·S3·S6 화면 | 글자·막대가 보이고 대비가 충분하다 (캡처). 끝나면 `night no` |
| S11 | debug Fake 모드 (Pleos)로 카드 → 출발 → 주행 → 도착 | 안내 영역·주행 화면이 Fake 길안내(목록 어림에서 시작)로 같은 모양. 확인 줄 없음 |
| S12 | 로그 | `adb logcat -d -t 1000 *:E`에 우리 앱 크래시·`SecurityException` 없음. 우리 앱 pid 로그에서 `grep -cE "37\.[34][0-9]{3}\|127\.[01][0-9]{3}\|보리\|기침\|호흡\|eyJ"`가 0 |

### 좁은 배치 (세로 반쪽)

- Pleos 에뮬레이터는 회전하면 시스템 화면이 세로를 지원하지 않는다 (011·012). 일반 폰 에뮬레이터(`emulator-5556`, 411dp) debug Fake로 카드 안내 영역·주행 화면·도착 화면을 세로·가로·다크로 `/verify` 한다 (글자 잘림·겹침 없음, 큰 숫자 한 줄).
- 일반 폰 에뮬레이터가 꺼져 있으면 미리보기(411dp, 840dp, 1682dp)만 두고 "확인하지 못함"으로 적는다.

### 병원 웹만 (메모리 모드, Playwright)

- `.env.local` 없이 빌드 → 예시 환자가 "새 환자"로 강조, 탭 제목에 (1) → [카드 확인] → "카드 확인함 HH:mm", 강조·숫자 사라짐 → 390px 폭에서 버튼이 잘리지 않음. 서버 줄 없음. 콘솔 오류 0. 보호자 페이지 `verify_screens.py` 26/26 그대로.

## 단계

단계 하나 = 커밋 1~3개. 앱 코드 커밋마다 `assembleDebug`가 통과해야 한다 (커밋 직전 훅). **웹 커밋 전에는 직접 `npm --prefix web run check`** (훅이 웹을 보지 않는다). 새 테스트는 대상 코드와 같은 커밋에, **새 파일로** 넣는다.

### 꼭 할 것 A — 11/07~11/08: 영상 촬영 전

- [ ] 0. 이 계획 파일 커밋 → `docs(plans): 013 시연 병원 연결과 길안내 화면 계획 추가`
  - `.\gradlew.bat testDebugUnitTest` 통과 수와 `npm --prefix web test` 통과 수를 진행 로그에 적는다 (기준 708 / 321).
  - `git status`에 U3 spike 코드(`MainActivity`의 `SpikeU3`)가 남아 있지 않은지 본다 (메인이 되돌렸다고 했다).

- [ ] 1. U3 관찰 기록 (문서만) → `docs(pleos): 내비 경로 취소 뒤 앱이 돌아오지 못하는 관찰 기록`
  - `docs/pleos/sdk-notes.md` `[obs-navi-emu]` 절에 관찰 블록: `cancelRoute`(`RequestRouteCancel`) → 내비 서비스가 내비 `MainActivity`를 START(`BAL_ALLOW_SAW_PERMISSION`) → 14ms 뒤 HOME_SCREEN `hideTask com.meongcare.app` → 우리 `startActivity(REORDER_TO_FRONT|NEW_TASK)` 900ms 뒤 `BAL_BLOCK result code=3`(`callingUidHasAnyVisibleWindow: false`, `LAST_ACTIVITY`) → `moveTaskToFront`는 `REORDER_TASKS` 없어 `SecurityException`. Android 14 (API 34), Pleos 에뮬레이터, 실차 아님.
  - 결론 한 줄: "`cancelRoute`는 내비 앱을 앞으로 띄우고, 앱은 스스로 돌아올 수 없다. 사용자는 GNB의 MeongCare 아이콘으로 돌아온다." `emulator-setup.md` "Pleos 에뮬레이터에서 직접 해 본 것"에 한 줄 링크.
  - 확인: `git show --stat`에 문서만.

- [ ] 2. U2: 시연 도착·[닫기] 때 내비 경로 남기기
  - 2-1. → `feat(data): 경로를 남기고 내비 연결만 놓는 안내 닫기 추가`
    - `NaviClient.kt`: `RouteOnClose`, 기본 구현 있는 2인자 `trip` (설계 결정 2). `PleosNaviClient`: 2인자 구현, 1인자는 CANCEL로 위임. KDoc에 U2·U3 근거.
    - 이 커밋은 동작이 그대로다 (아직 아무도 KEEP을 주지 않는다).
  - 2-2. → `fix(data): 시연 도착과 닫기 때 내비 경로를 남기도록 수정`
    - `NaviRouteOnlyGuidanceSource`: `Trip.keepRouteOnClose`, 도착 분기·`dismissArrival`에서 세우고 `naviJob.cancel()`. KDoc의 "3. 시연용 길안내가 도착하면 내비 경로도 취소한다"를 고친다.
    - 새 테스트 파일 `NaviRouteOnlyKeepRouteTest` + 테스트용 가짜 `CloseRecordingNaviClient`(새 파일, 2인자 `trip`을 구현해 구독이 끊길 때 `onClose()` 결과를 기록):
      - `` `시연 도착으로 구독을 끊을 때는 내비 경로를 남긴다`() ``
      - `` `도착 화면을 닫아도 내비 경로를 취소하지 않는다`() ``
      - `` `안내 중에 앱에서 취소하면 내비 경로를 취소한다`() ``
      - `` `경로를 기다리는 중에 취소하면 내비 경로를 취소한다`() ``
      - `` `다른 목적지로 다시 시작하면 이전 경로는 취소한다`() ``
      - `` `경로를 남긴 뒤 새로 출발하면 내비에 새로 구독한다`() ``
  - 확인: 기존 `NaviRouteOnlyGuidanceSource*Test` 그대로 통과, `assembleDebug`. 기기 확인은 단계 12(S5·S6·S7).

- [ ] 3. 시연 길안내의 도착 예정 갱신 간격 → `feat(di): 시연 길안내의 도착 예정 갱신 간격 단축`
  - `di/TripTiming.kt` (`etaInterval`만. `ackPollInterval`은 단계 5에서 더한다), `AppContainer`가 `HospitalTrip(etaInterval = …)`에 넘긴다.
  - 새 `TripTimingTest`: `` `시연용 길안내는 도착 예정을 4초에 한 번까지 보낸다`() ``, `` `실제 길안내는 1분에 한 번까지 보낸다`() ``, `` `구성표에서 시연용 길안내인 모드만 짧은 간격이다`() `` (`sourcePlanOf` 모든 모드×기기와 함께).
  - 확인: `HospitalTrip*Test` 그대로 통과 (기본값은 60초).

- [ ] 4. 카드 확인과 진행 시작값 (도메인)
  - 4-1. → `docs(data): 카드 확인 쓰기 규칙과 시연 갱신 간격 기록`
    - `docs/data/emergency-case.md`: `acknowledgedAt`은 병원 웹 [카드 확인]이 **서버 시각으로 한 번만** 쓴다, 차는 안내 중 자기 건의 이 필드만 읽는다(시연 4초·실제 15초), 도착·취소·확인되면 그만 읽는다. 상태 흐름의 "1분에 1번 이하"를 "실제 길안내 1분, 시연 길안내 4초에 1번 이하"로. "(계획 012 있으면 좋음 A)" 꼬리표를 뺀다. [확인함] → [카드 확인].
    - 확인: `npm --prefix web run check` (docs/data가 웹 검사 대상이다).
  - 4-2. → `feat(domain): 병원 확인 읽기 계약과 진행 시작값 추가`
    - `AckRead`, `CaseSender.readAck`(기본 `Unsupported`), `CardDelivery.Sent.acknowledgedAt`, `TripState.startMinutes`, `HospitalTrip`의 시작값 갱신(설계 결정 4 표).
    - 새 `HospitalTripProgressTest`: `` `처음 알게 된 남은 분이 진행 막대의 시작이다`() ``, `` `남은 분이 시작보다 커지면 시작을 올린다`() ``, `` `경로를 찾는 중은 시작을 바꾸지 않는다`() ``, `` `줄어드는 남은 분은 시작을 바꾸지 않는다`() ``, `` `목록의 이동 어림은 시작값이 아니다`() ``.
  - 4-3. → `feat(domain): 안내 중 서버 카드의 병원 확인 읽기 추가`
    - `HospitalTrip(…, ackPollInterval = 15.seconds)`, 확인 읽기(설계 결정 4 "확인 읽기 규칙" 표 전체).
    - 새 `HospitalTripAckTest` (가상 시간, 가짜 `GuidanceSource`·읽기 결과를 차례로 주는 가짜 `CaseSender`):
      - `` `서버로 보낸 뒤 간격마다 확인을 읽고 확인되면 표시하고 멈춘다`() ``
      - `` `첫 읽기는 간격이 되기 1ms 전에는 하지 않는다`() ``
      - `` `내장 병원으로 출발하면 확인을 한 번도 읽지 않는다`() ``
      - `` `카드 없이 출발하면 확인을 읽지 않는다`() ``
      - `` `전송 실패로 다시 보내기를 기다리는 동안은 읽지 않고 다시 보내 성공하면 읽는다`() ``
      - `` `도착하면 읽기를 멈추고 읽던 결과를 쓰지 않는다`() ``
      - `` `취소되면 읽기를 멈추고 고른 병원을 비운다`() ``
      - `` `도착 화면을 닫으면 읽기를 멈춘다`() ``
      - `` `건이 지워졌으면 멈추고 표시는 그대로다`() ``
      - `` `읽기 실패는 다음 간격에 다시 하고 같은 실패의 로그는 한 줄이다`() ``
      - `` `확인을 지원하지 않는 전송이면 로그 없이 멈춘다`() ``
      - `` `확인된 뒤 도착해도 확인 표시가 남는다`() ``
      - `` `로그에 시각과 병원 이름이 없다`() ``
  - 확인: 기존 `HospitalTripTest`·`HospitalTripBoundaryTest` 그대로 통과.

- [ ] 5. 카드 확인 (서버 읽기) → `feat(data): Firestore 환자 건의 확인 시각 읽기 추가`
  - `FirestoreJson.ackOf`, `FirestoreCaseSender.readAck` (설계 결정 5 표, F3), `TripTiming.ackPollInterval`, `AppContainer` 연결.
  - 새 `FirestoreAckTest`: `` `확인 시각이 있으면 확인됨이다`() ``, `` `확인 시각이 null이거나 없으면 아직이다`() ``, `` `확인 시각 형식이 틀리면 실패다`() ``, `` `404와 403은 건 없음이다`() ``, `` `401과 5xx는 실패다`() ``, `` `네트워크 오류는 실패다`() ``, `` `요청은 자기 건 주소에 acknowledgedAt만 마스크하고 Bearer 토큰을 붙인다`() ``.
  - 단계 3에서 만든 `TripTimingTest`에 더한다 (이 브랜치에서 만든 파일이라 고쳐도 된다): `` `병원 확인은 시연 4초 실제 15초마다 읽는다`() ``.

- [ ] 6. 보안 규칙 → `feat(firebase): 카드 확인 한 번과 서버 시각 규칙 강화`
  - `firestore.rules` (설계 결정 6), `check-rules.mjs` R9~R13 (F4). 머리 주석·README의 R1~R8 → R1~R13.
  - 확인: **사용자 터미널의 에뮬레이터에서** `npm --prefix firebase run check-rules` → R1~R13 PASS (완료 기준 4). R13의 실제 코드(403/404)를 진행 로그와 설계 결정 5에 적는다.

- [ ] 7. 정차 화면의 안내 영역
  - 7-1. → `feat(ui): 안내 영역 상태 계산 추가`
    - `ui/guidance/TripPanel.kt` (`tripPanelOf`, 설계 결정 7 표).
    - 새 `TripPanelTest`: `` `안내 없음이면 패널이 없다`() ``, `` `도착 예정은 지금에 남은 분을 더한 시각이고 분 아래는 버린다`() `` (23:50 + 15분 → 00:05 포함), `` `진행은 시작 대비 지난 비율이고 0과 1 사이다`() ``, `` `시작이나 남은 분을 모르면 진행 막대가 없다`() ``, `` `서버로 보낸 카드가 확인되면 확인 줄이다`() ``, `` `시연용 전송은 확인 줄이 없다`() ``, `` `대기 정보는 출발할 때 받은 값이다`() ``, `` `도착이면 도착 패널이고 막대와 도착 예정이 없다`() ``, `` `trip이 없으면 전송 줄과 막대가 없다`() ``.
  - 7-2. → `feat(ui): 진료 카드와 처음 화면에 안내 영역 추가`
    - `ui/guidance/GuidancePanel.kt`, `ui/guidance/StaticProgressBar.kt`, `MedicalCardScreen`(머리 "진료 카드", 안내 영역, 도착 문구, 전송·확인 줄을 패널로 옮김), `ParkedScreen`(`CurrentCardPanel`이 같은 패널), `HomeScreen`·`CardExtras.trip`·`MainActivity` 연결, `strings.xml`(새 문구, 안 쓰게 된 문구 삭제), 미리보기 (1682dp·840dp·411dp, 라이트·다크, 안내 중·경로 찾는 중·확인됨·도착).
  - 확인: `assembleDebug lintDebug`. `/verify`(Pleos, debug Fake): 카드 → 출발 → P에서 카드 화면·처음 화면 캡처. 텍스트 입력 0.

- [ ] 8. 주행 화면 → `feat(ui): 주행 화면을 큰 남은 시간과 진행 막대로 변경`
  - `DrivingScreen(guidanceUiState, modifier, panel: TripPanel? = null)` (설계 결정 7 표). 큰 숫자 크기 이름 붙이기. 안 쓰게 된 `remainingTimeText`·문구 정리.
  - 확인: `/verify`(Pleos debug Fake): 기어 D → UI 트리(완료 기준 9) + 캡처, 다크 캡처. 완료 기준 7의 DrivingScreen Grep.

- [ ] 9. 병원 웹 카드 확인과 새 환자 표시
  - 9-1. → `feat(web): 환자 건 카드 확인 저장 추가`
    - `HospitalDesk.acknowledgeCase` (메모리·Firestore), `caseAck.ts`(`isNewCase`, `newCaseCount`, `pageTitle`, `ackText`).
    - 새 `caseAck.test.ts`: 새 환자 판정(확인됨·취소·읽지 못한 건 제외), 개수, 제목 세 가지, `ackText` 분 버림. 새 `memoryHospitalDesk.ack.test.ts`: 확인하면 `watchCases`에 시각이 오고, 두 번 눌러도 처음 시각 그대로, 없는 건은 `not-found`.
  - 9-2. → `feat(web): 새 환자 강조와 카드 확인 버튼 추가`
    - `CasesPanel`(강조, [카드 확인], 확인 시각, 실패 문구), `HospitalApp`(탭 제목), `hospital.css`.
  - 확인: 커밋마다 `npm --prefix web run check`. Playwright 메모리 모드 ("확인 절차" 병원 웹만).

- [ ] 10. 병원 웹 서버 주소와 연결 상태 → `feat(web): 병원 웹의 서버 주소와 연결 상태 표시`
  - `serverInfo.ts`(`serverInfoOf`), `HospitalDesk.server`·`watchConnection`(메모리 `local`, Firestore F5), `HospitalApp` 안내 줄.
  - 새 `serverInfo.test.ts`: 에뮬레이터 주소 문구, IPv6 호스트는 대괄호, `localhost`·`::1`·`[::1]`이면 경고, `127.0.0.1`은 경고 없음, 진짜 프로젝트 문구.
  - 확인: `npm --prefix web run check`, `from 'firebase` Grep (완료 기준 7).

- [ ] 11. 시연 전 점검 → `chore(firebase): 시연 전 서버와 adb reverse 점검 스크립트 추가`
  - `scripts/preflight.mjs`(설계 결정 10), `package.json` 스크립트 `preflight`, `firebase/README.md` 명령 표, (K9) `firebase.json` host 고정(F6).
  - 확인: 완료 기준 5 (사용자 터미널 에뮬레이터 기준). `firebase/scripts`에 `.env`·`secrets.properties` 0건.

- [ ] 12. 처음부터 끝까지 확인 (`/verify` 포함) → **체크포인트 A**
  - "확인 절차"의 S0~S12, 좁은 배치, 병원 웹만. O1~O4 결과를 진행 로그에 적는다.
  - 고친 것이 있으면 `fix(...)` 커밋. 새로 안 Pleos 사실은 → `docs(pleos): …` (sdk-notes `[obs-navi-emu]`).
  - **여기까지면 차 + 병원 웹 시연 영상을 찍을 수 있다** (진행 로그에 적는다).

### 꼭 할 것 B — 11/09 (시간이 없으면 토큰 갱신만 11/15 뒤, PT 전으로)

- [ ] 13. 익명 토큰 갱신
  - 13-1. → `feat(data): 익명 토큰 갱신 주소 추가`
    - `ServerConfig.refreshUrl`, `serverConfigOf`. 새 `ServerConfigRefreshTest`: `` `에뮬레이터면 9099의 securetoken 주소다`() ``, `` `release는 https securetoken만 쓰고 에뮬레이터 호스트가 없다`() ``, `` `IPv6 에뮬레이터 호스트는 대괄호로 감싼다`() ``.
  - 13-2. → `feat(data): 만료 전 익명 토큰 갱신 추가`
    - `AnonymousAuth` (설계 결정 8 표, F1·F2), `AppContainer`가 `log`를 넘긴다.
    - 새 `AnonymousAuthRefreshTest`: `` `만료 5분 전이 되기 1ms 전에는 갱신하지 않는다`() ``, `` `만료 5분 전이 되면 갱신 토큰으로 새 토큰을 받는다`() ``, `` `갱신해도 uid는 그대로다`() ``, `` `갱신 응답의 uid가 다르면 실패로 본다`() ``, `` `갱신에 실패해도 만료 전이면 지금 토큰을 쓴다`() ``, `` `만료 뒤 갱신에 실패하면 이번에는 null이고 다음에 다시 갱신한다`() ``, `` `갱신이 거부되면 새로 가입한다`() ``, `` `만료 시각을 모르면 갱신하지 않는다`() ``, `` `동시에 불러도 갱신은 한 번이다`() ``, `` `갱신 토큰은 몸통에만 있고 URL에 없다`() ``, `` `로그에 토큰과 uid가 없다`() ``.
  - 확인: 기존 `FirestoreClients*Test` 그대로 통과. F1을 에뮬레이터에 Node 한 줄로 해 본 결과를 진행 로그에. 기기에서는 S1~S5를 짧게 다시 본다 (1시간 만료는 기다리지 않는다. "확인하지 못한 것"에 적는다).

- [ ] 14. tester → `test: …` (tester가 쓴 테스트를 영역별로 커밋)
  - 넘길 것: 이 계획 파일 경로와 대상만. 대상: `NaviRouteOnlyGuidanceSource`(닫기 규칙), `tripTimingOf`, `HospitalTrip`(확인 읽기·시작값), `FirestoreJson.ackOf`·`FirestoreCaseSender.readAck`, `ServerConfig.refreshUrl`, `AnonymousAuth`(갱신), `tripPanelOf`, 웹 `caseAck.ts`·`serverInfo.ts`·메모리 저장소 `acknowledgeCase`.
  - 대상 아님: `PleosNaviClient`, Compose 화면, `AppContainer`, `firestoreHospitalDesk.ts`(SDK), `preflight.mjs`.
  - 계약 기준: 설계 결정 2·3·4·5·7·8·9의 표와 경계 (정확히 5분 전, 간격 1ms 전, 읽는 중 도착·취소, 같은 실패 반복, 자정 넘는 도착 예정).
  - 버그 보고는 메인이 `fix:`로 고친다. UI가 바뀌면 `/verify` 다시.

- [ ] 15. 문서 갱신
  - 15-1. → `docs(architecture): 카드 확인과 경로 남기기와 안내 영역 반영`
    - "서비스 기능 스택": 진료 카드 전송 행(갱신 간격 시연 4초·실제 60초, 병원 카드 확인), 서버 행(익명 토큰 미리 갱신).
    - "데이터 모드" 차량 내비 표: 시연 도착 시 "내비 경로도 취소" → **"경로를 남긴다 (013 U2)"**, `[안내 취소]`·Real 취소는 내비가 앞에 나온다는 U3 한 줄.
    - "주행 상태 × 길안내" 표의 주행 화면 칸(큰 남은 분 + 목적지 + 정적 막대 + 확인 한 줄)과 "주행 화면 규칙", 2단계 표의 카드 칸("진료 카드" + 안내 영역).
    - "병원 웹" 절: 카드 확인, 새 환자, 서버 주소·연결 표시, preflight. "패키지 구조": `di/TripTiming.kt`, `ui/guidance/TripPanel.kt`·`GuidancePanel.kt`.
  - 15-2. → `docs: README와 AGENTS에 시연 전 점검과 카드 확인 반영`
    - `AGENTS.md` 명령어 표에 `npm --prefix firebase run preflight` 한 줄. `README.md` 시연 흐름(카드 확인, 도착 때 앱이 남음, [안내 취소] 뒤 GNB로 돌아오기). `web/README.md`: 에뮬레이터 호스트는 `127.0.0.1`(localhost 아님), 화면 위 서버 줄 보는 법. `firebase/README.md`: preflight, R1~R13.
  - 확인: 문서만 바뀐 커밋. 링크가 맞는지 본다.

- [ ] 16. 개발 노트 작성 (`docs/notes/`)
  - `docs/notes/xxj15/YYYY-MM-DD-smooth-trip.md` (형식: docs/notes/README.md). 넣을 것:
    - 결정과 이유: U1~U4, 기존 테스트를 지키려고 자리를 바꾼 것(설계 결정 1), 간격 값, 토큰 갱신 규칙, preflight를 만든 이유(관찰 1)
    - 사용한 Pleos API·권한: 새 권한 없음, `cancelRoute`를 덜 부르게 된 것, U3 관찰
    - 확인한 것 / 확인하지 못한 것 (1시간 뒤 토큰, 실차, Real 모드, 좁은 배치가 미리보기뿐이면 그것)
    - **기획서 5번 "개발 계획"에 넣을 문장** (확인한 것만). 예: "병원 직원이 병원 화면에서 진료 카드의 [카드 확인]을 누르면, 운전 중인 보호자의 차 화면에 '병원이 카드를 확인했어요'가 표시된다. 주행 중 화면은 남은 시간과 진행 막대, 확인 한 줄만 큰 글씨로 보이고 버튼이 없다."
    - 남은 일: 있으면 좋음 중 안 한 것, 012에서 넘어온 U5(11/15 APK에 Firebase 설정)·K9(PT 빌드), 토큰 갱신을 미뤘다면 PT 전 할 일로
  - 이 계획을 `docs/plans/completed/`로 옮기고 진행 로그를 마친다.
  - 커밋: `docs(notes): smooth-trip`, `docs(plans): 013 시연 병원 연결과 길안내 화면 완료`
  - 확인: `git status`에 남은 변경이 없다. 그다음 `/ship`.

### 있으면 좋음 (필수가 끝난 뒤, 단계 16 전에)

- [ ] A. 병원 웹 화면 확인 시나리오 (012 단계 9의 H01~H05 + 카드 확인 H06) → `test(web): 병원 웹 화면 확인 시나리오 추가`
- [ ] B. 401 응답에 토큰을 한 번 갱신하고 다시 보내기 (`FirestoreHospitalBoardSource`·`FirestoreCaseSender`) → `feat(data): 인증 거부 때 토큰 갱신 후 한 번 다시 보내기`
- [ ] C. Real 모드(전부 내비, debug)에서 카드 확인·주행 화면을 Pleos 에뮬레이터로 한 번 본다 (도착은 에뮬레이터에서 볼 수 없다)
- [ ] D. 실제 길안내에서 확인 읽기 간격을 처음 3분 4초 → 그 뒤 15초로 (폴링 줄이기) → `feat(domain): 병원 확인 읽기 간격 점점 늘리기`

## 진행 로그

- 2026-11-06: 계획 작성.
  - 브랜치 `feat/smooth-trip`은 dev `5ea81ba`(012 + `fix/hospital-web-open`)에서 땄다.
  - 시작 시점의 코드:
    - `HospitalTrip`: 도착 예정 갱신 60초, 확인 읽기 없음. `CaseSender`는 send/updateEta/finish.
    - `NaviRouteOnlyGuidanceSource`: 시연 도착 때 `naviJob.cancel()` → `PleosNaviClient.awaitClose`가 `cancelRoute()` → 내비가 앞으로 나와 MeongCare를 가린다 (관찰 3).
    - `AnonymousAuth`: 한 번 가입, 갱신 없음.
    - 주행 화면: `displayLarge`(48sp) "병원까지 N분" + 병원 이름. 카드 화면 머리가 안내 중 "병원까지 N분"으로 바뀐다.
    - 병원 웹: `acknowledgedAt`을 읽기만 하고 쓰는 곳이 없다. 새 환자 표시·연결 표시 없음.
  - 메인 세션 관찰 (2026-11-06): 관찰 1~7 (Firebase 에뮬레이터 두 벌 IPv4/IPv6, ETA 60초 간격, 내비가 MeongCare를 덮음, 휑한 주행 화면, 안내 영역 없음, 토큰 갱신 없음, 병원 웹 새 환자 표시 없음). IPv6 쪽 에뮬레이터는 메인이 정리했다.
  - U3 spike (메인, 2026-11-06): **안 됨** (위 "사용자·메인 결정" U3). 시험 코드는 되돌렸다. 단계 1에서 기록한다.
  - 기준값: Android 708, 웹 321 (012 끝). 단계 0에서 다시 센다.

## 결정 / 배운 점

- 진행 막대 시작값을 `GuidanceUiState`가 아니라 `TripState`에 둔 이유, `NaviClient`·`CaseSender`에 기본 구현을 둔 이유: 설계 결정 1.
- `NaviRouteOnlyGuidanceSourceTest`의 `` `시연 길안내가 도착하면 도착이고 내비 경로도 끝낸다` ``는 U2 뒤 "연결을 놓는다"만 맞다 (경로는 남는다). 기존 테스트는 고치지 않는다. 새 테스트 `NaviRouteOnlyKeepRouteTest`가 경로를 남기는 것을 본다.
