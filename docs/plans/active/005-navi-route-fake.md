# 005 병원 길안내 흐름 (Fake)

- 목표: "정차 중 병원 길안내 시작 → 주행 중 남은 시간 표시 → 도착하면 도착 화면"을 Fake 데이터 소스만으로 처음부터 끝까지 돌게 해서, 실제 운전 없이 시연 영상을 찍을 수 있게 한다.
- 완료 기준 (어떻게 검증하나):
  1. `node .claude/hooks/build-gate.mjs pipeline`이 "검증 파이프라인 통과."를 출력한다 (`assembleDebug testDebugUnitTest lintDebug`).
  2. 단위 테스트가 통과한다:
     - 새로 쓴 것: `FakeGuidanceSourceTest`, `FakeTripScenarioTest`, `RealGuidanceSourceTest`, `GuidanceViewModelTest`, `HomeContentTest`
     - tester가 추가한 테스트
     - 기존 테스트 26개. `HomeViewModelTest.kt`는 고치지 않는다: `git diff dev -- app/src/test/java/com/meongcare/app/ui/home/HomeViewModelTest.kt` 출력이 비어 있다.
  3. `/verify` (일반 AVD `my_avd`, debug 빌드, Fake 모드, 세로). 아래 "시연 시나리오"를 그대로 따라가며 스크린샷을 찍는다.
     - 앱 실행 → 정차 화면에 샘플 병원 이름과 [병원으로 안내] 버튼이 있다.
     - [병원으로 안내] → 정차 화면에 "경로를 찾는 중", 그다음 "병원까지 12분"과 [안내 취소]가 보인다.
     - 약 3초 뒤 → 주행 화면으로 저절로 바뀐다. 큰 글씨 "병원까지 N분"이 2초마다 1분씩 준다.
       - 주행 화면의 UI 트리(`uiautomator dump`)에서 debug 패널 영역을 빼면 `android.widget.EditText` 0개, 클릭 가능한 버튼 0개다.
     - 시작 후 약 25초 → 주행 화면에 "도착했어요"가 보인다. 진료 카드는 보이지 않는다.
     - 그 뒤 약 5초 → 정차가 되고, 도착 화면(병원 이름, 진료 카드 자리표시, [닫기])이 보인다.
     - [닫기] → 처음 정차 화면으로 돌아온다.
     - 취소 경로 1: [병원으로 안내] → 3초 안에 [안내 취소] → 정차 화면 그대로, 주행으로 바뀌지 않는다.
     - 취소 경로 2: 주행 중에 debug [정차] → 정차 화면에 남은 시간과 [안내 취소] → [안내 취소] → 처음 정차 화면이다. 남은 시간이 더 줄지 않는다.
     - 새 화면(정차+안내 중, 도착 화면)을 가로와 다크 모드에서도 본다. 글자 잘림이나 겹침이 없다.
     - Real 전환 → 재시작 → 주행 화면 "주행 중"(지금과 같음). 크래시가 없다.
     - `adb logcat -d -t 300 *:E`에 `com.meongcare.app` 크래시가 없다.
  4. 정적 검사 (Grep):
     - `app/src/main/java/com/meongcare/app/ui/home/DrivingScreen.kt`:
       - `TextField` 0건
       - `Button` 0건 (아래 결정 1의 기본안 기준. 바뀌면 1건 이하)
       - `verticalScroll|LazyColumn` 0건
       - `Animated|Crossfade|animate` 0건
     - `ui/home/HomeScreen.kt`에 `AnimatedContent|Crossfade` 0건
     - `import ai.pleos`, `import android.car`가 `data/` 밖에 0건
     - `ui/` 아래에 `import com.meongcare.app.data` 0건
     - 병합 매니페스트(`app/build/intermediates/` 아래 `AndroidManifest.xml`)에 `pleos.car.permission` 0건
     - `app/build.gradle.kts`와 `gradle/libs.versions.toml`에 `NaviHelper` 0건 (이번 범위 밖)
  5. 문서: `docs/architecture.md` "패키지 구조"에 `domain/guidance`가 있고, "주행 상태와 화면"에 아래 "화면 조합" 표가 들어 있다.
- 브랜치: `feat/navi-route-fake` (dev에서 이미 땄다)
- 관련 문서:
  - AGENTS.md (규칙 1, 3, 아키텍처 규칙)
  - docs/architecture.md ("서비스 기능 스택", "패키지 구조", "주행 상태와 화면")
  - docs/pleos/sdk-notes.md (NaviHelper 절, 약 300~480행)
  - docs/pleos/ux-guidelines.md ("주행 안전 설계 원칙", "Maps/Navigation 앱 가이드의 주행 중 조작 기준")
  - docs/plans/completed/004-project-setup.md (Fake/Real 구조, debug 패널)
  - docs/conventions/git.md

## 범위

- **필수** (1~2일): 단계 0~9. 시연 영상(11/15 가산점)에 쓸 장면이 나온다.
- **있으면 좋음**: 단계 A~C. 필수가 끝나고 시간이 남으면 한다. A는 영상 품질에 직접 도움이 되므로 먼저 한다.
- **이번 범위 밖** (후속 계획으로 남긴다):
  - **Real 길안내**: `ai.pleos.playground:NaviHelper:3.0.5` 의존성 추가, `NaviHelper` 연결 관리, `requestRoute`/`cancelRoute`/`onTripInfo`/`onDestinationArrived`를 `GuidanceSource`로 감싸기. 이번에는 Real 자리표시만 둔다. 후속 계획 전에 확인할 것은 아래 "후속 계획으로 넘기는 질문"에 있다.
  - **병원 검색** (카카오 로컬 + Firebase): 주최측 문의 A1의 답을 기다리는 중이다. 이번에는 시연용 샘플 병원 1곳을 고정값으로 쓴다.
  - **진료 카드 내용** (Claude API): 도착 화면에는 자리표시만 둔다.
  - **Real 주행 상태** (CarUxRestrictions 또는 `getGear()`): 004에서 넘긴 그대로다. 그래서 release(항상 Real)는 지금처럼 항상 주행 화면이고, 길안내를 시작할 수 없다. 004 노트의 "release 앱은 항상 주행 화면" 문제를 이번 작업이 바꾸지 않는다.
  - 음성으로 안내 시작 (STT "준비 중", 문의 B2), 병원 전화 (문의 B4).

## 결정 (planner 판단과 근거)

구현하면서 바꾸면 "결정 / 배운 점"에 이유를 남긴다. `[사용자 승인 필요]` 표시가 있는 것은 아래 "확인 필요"에서 사용자에게 묻는다.

### 1. 도메인 인터페이스: Real(NaviHelper)에 맞추되 미확인 사항에 묶이지 않게

```kotlin
// domain/guidance/
data class Destination(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val address: String? = null,
)

sealed interface GuidanceState {
    data object Idle : GuidanceState
    /** remainingTime == null: 경로를 찾는 중이라 아직 모른다 */
    data class Guiding(val destination: Destination, val remainingTime: Duration?) : GuidanceState
    data class Arrived(val destination: Destination) : GuidanceState
}

interface GuidanceSource {
    /** 구독하면 현재 상태를 먼저 내보낸다. 같은 상태를 연달아 내보내지 않는다 (DrivingStateSource와 같은 약속). */
    val state: Flow<GuidanceState>
    suspend fun start(destination: Destination)
    suspend fun cancel()
    /** 도착 화면을 닫는다. Arrived일 때만 Idle로 바뀐다. */
    fun dismissArrival()
}
```

(시그니처는 기본안이다. 이름이나 세부는 구현하면서 바꿔도 된다. 바꾸면 이 블록을 고친다.)

- **남은 시간은 `kotlin.time.Duration`으로 둔다.**
  - `TripInfo.getEta(): Int`의 단위(초/분)를 모른다 (sdk-notes "값의 단위는 미확인").
  - 그 값이 "남은 시간"인지 "도착 시각"인지도 문서에 없다.
  - `Duration`으로 두면 변환은 Real 래퍼 안에서만 한다. 도메인과 UI는 답에 따라 바뀌지 않는다.
- **목적지는 이름과 좌표가 기본이다.** `poiId`/`poiSubId`가 필수인지 모른다 (sdk-notes 미확인).
  - 필수로 밝혀지면 `Destination`에 기본값 있는 필드(`poiId: String? = null`)를 더한다. 그러면 기존 호출부를 고치지 않아도 된다.
- **`start`/`cancel`은 `suspend`로 둔다.** Real은 `NaviHelper.initialize(onConnected, onFailed)` 연결을 기다려야 할 수 있다. Fake는 바로 돌아온다.
- **도착은 "상태"로 둔다.** `onDestinationArrived`는 일회성 이벤트지만, 화면은 사용자가 닫을 때까지 도착 화면을 유지해야 한다. 그래서 `Arrived` 상태로 남기고 `dismissArrival()`로 끝낸다.
- **실패 상태는 이번에 두지 않는다.** Fake에는 실패가 없다. Real 후속 계획에서 `onError(NaviErrorCode)`를 다룰 때 `Failed`를 더한다. sealed라서 컴파일러가 처리하지 않은 곳을 찾아 준다.
- Real 대응 (후속 계획 참고용, 이번에 구현하지 않음):

  | 도메인 | NaviHelper (sdk-notes) |
  |---|---|
  | `start(dest)` | `requestRoute(RouteInfo(...))` |
  | `Guiding(dest, null)` | `onRouteStarted` 직후 |
  | `Guiding(dest, t)` | `onTripInfo(TripInfo).eta` 또는 `onDrivingInfo(...).destination.duration` (의미·단위 미확인) |
  | `Arrived(dest)` | `onDestinationArrived(DestinationArrivedInfo)` |
  | `cancel()` / `Idle` | `cancelRoute()` / `onRouteCancelled()` |

### 2. Fake 길안내: 시간 주입과 시연 속도

- `FakeGuidanceSource(scope: CoroutineScope, timing: FakeGuidanceTiming = FakeGuidanceTiming())`
  - 상태는 `MutableStateFlow`로 들고, 안내 진행은 주입받은 `scope`에서 `delay`로 돈다.
  - 테스트에서는 `runTest`의 `backgroundScope`를 넘긴다. 그러면 `advanceTimeBy`로 가상 시간을 돌릴 수 있다.
  - 앱에서는 `AppContainer`의 앱 수명 scope를 넘긴다.
- `FakeGuidanceTiming` 기본값 (시연용. 시작 후 약 25초에 도착):

  | 값 | 기본 | 뜻 |
  |---|---|---|
  | `routeCalculationDelay` | 1초 | `Guiding(remainingTime = null)`("경로를 찾는 중")을 보여주는 시간 |
  | `initialRemaining` | 12분 | 처음 표시하는 남은 시간 |
  | `remainingStep` | 1분 | 한 번에 줄어드는 양 |
  | `tickInterval` | 2초 | 줄어드는 간격 (실제 시간) |

  - 흐름: 0초 `Guiding(null)` → 1초 `Guiding(12분)` → 3초 `11분` → … → 23초 `1분` → 25초 `Arrived`. "0분"은 내보내지 않는다.
- 규칙:
  - 안내 중에 `start`를 다시 부르면 이전 진행을 멈추고 새 목적지로 처음부터 다시 센다. NaviHelper도 목적지를 다시 정할 때 `requestRoute()`를 부른다 (sdk-notes).
  - `cancel`: Guiding이면 Idle로 가고 진행을 멈춘다. Idle·Arrived에서는 아무것도 바꾸지 않는다.
  - `dismissArrival`: Arrived일 때만 Idle로 간다.
  - Arrived에서 `start`하면 새 안내를 시작한다.

### 3. Fake 주행 상태와 Fake 길안내의 관계: 따로 두고, FAKE 전용 시나리오 클래스로 잇는다

- **채택: `data/demo/FakeTripScenario`**
  - FAKE 모드에서만 `AppContainer`가 만들어 시작한다.
  - 길안내 상태를 구독하고 `FakeDrivingStateSource`를 바꾼다:
    - Idle/Arrived → Guiding으로 바뀌면 `departDelay`(기본 3초) 뒤에 DRIVING. 그 사이에 취소되면 바꾸지 않는다.
    - Guiding → Arrived로 바뀌면 `parkDelay`(기본 5초) 뒤에 PARKED.
    - Guiding → Idle(취소)이면 주행 상태를 그대로 둔다. 실제로도 안내를 취소해도 차는 계속 달린다.
  - 지연은 `collectLatest`로 기다린다. 다음 상태가 오면 기다리던 것은 취소된다. 시간 주입은 Fake 길안내와 같은 방식이다 (scope + timing).
- 근거:
  - Real에서 주행 상태(차량)와 길안내(Navigation 앱)는 서로 모르는 독립된 두 출처다. 두 Fake도 각자 Real과 같은 책임만 지게 해야, UI가 Real에서도 같은 코드로 돈다.
  - "차가 실제로 출발하고 주차하는 세상"을 흉내 내는 일은 FAKE 모드에만 있는 한 클래스가 맡는다. 시연에서 debug 패널을 누르지 않아도 정차 → 주행 → 도착 → 정차가 이어진다.
  - debug 패널의 수동 [정차]/[주행]/[모름]은 그대로 쓸 수 있다. 시나리오의 다음 전환이 오면 그 값으로 덮어쓴다.
- 버린 대안:
  - 따로 두기만 한다: 시연 중에 debug 패널을 손으로 눌러야 하고, 그 조작이 영상에 보인다.
  - `FakeGuidanceSource`가 `FakeDrivingStateSource`를 직접 바꾼다: 클래스 하나는 줄지만, 길안내 Fake가 Real(NaviHelper는 주행 상태를 바꾸지 않는다)과 다른 책임을 갖게 되고 테스트가 섞인다.

### 4. 화면 조합: 주행 상태와 길안내 상태를 따로 받아 조합한다

UNKNOWN은 지금처럼 주행으로 다룬다 (안전 우선, 004 결정).

| 주행 \ 안내 | 안내 없음 (Idle) | 안내 중 (Guiding) | 도착 (Arrived) |
|---|---|---|---|
| **정차** (PARKED) | 정차 화면: 샘플 병원 이름 + [병원으로 안내] | 정차 화면: "병원까지 N분" (모르면 "경로를 찾는 중") + 병원 이름 + [안내 취소] | **도착 화면**: "○○에 도착했어요" + 진료 카드 자리표시 + [닫기] |
| **주행·모름** (DRIVING/UNKNOWN) | 주행 화면 (지금 그대로): "주행 중" + 작은 안내 | 주행 화면: 큰 글씨 "병원까지 N분" (모르면 "경로를 찾는 중") + 작은 줄 병원 이름. **버튼 없음** | 주행 화면: 큰 글씨 "도착했어요" + 작은 줄 "정차하면 진료 카드를 볼 수 있어요". **버튼 없음, 진료 카드 없음** |

- **정차 + 안내 중**: 출발 전이거나 길가에 세운 때다.
  - 정차 중이므로 남은 시간, 병원 이름, 취소 버튼을 함께 보여준다.
  - 안내 취소는 여기서만 한다 (결정 5).
- **주행 + 안내 없음**: 지금 주행 화면을 그대로 쓴다. 안내 시작 버튼을 두지 않는다.
  - 병원을 고르는 일은 정차 중에 한다.
  - 운전 중 음성 SOS는 STT(준비 중)와 DO 선언(문의 B1)이 정해진 뒤에 다룬다. `[사용자 승인 필요]` 결정 3.
- **주행 + 도착**: 진료 카드는 세부 정보라 정차 중에만 보여준다. 주행 화면에서는 도착 사실만 큰 글씨로 알린다.
- 남은 시간 표시:
  - 분 단위로 올림한다 (예: 90초 → 2분).
  - 1분보다 작으면 1분으로 보여준다. "0분"이나 음수는 보이지 않는다.
  - Fake는 표시가 2초마다 바뀐다. 시연 속도 때문이다. 글자만 바뀌고 움직이는 효과는 없다.
- 이 조합 중 안전과 관계된 부분(주행·모름이면 도착해도 도착 화면을 보여주지 않는다)은 순수 함수 `homeContentOf(...)`로 빼서 JVM 테스트로 검증한다.
- **`HomeViewModel`은 고치지 않는다.** 주행 상태 쪽의 안전 규칙(UNKNOWN·첫 값·5초 뒤 복귀는 주행 화면)과 그 테스트 15개를 그대로 지키기 위해서다.
  - 길안내는 별도 `GuidanceViewModel`이 맡는다. 두 상태의 조합은 `HomeScreen`이 `homeContentOf`로 한다.

### 5. 안내 취소 경로

1. 정차 + 안내 중: [안내 취소] → Idle. 주행 상태는 바뀌지 않는다.
2. 주행 중: 앱 안에서는 취소하지 않는다 (기본안. `[사용자 승인 필요]` 결정 1).
   - Real에서는 Pleos Navigation 앱에서 취소하면 `onRouteCancelled` → Idle이 된다 (후속 계획).
   - Fake 시연에서는 debug [정차] → [안내 취소].
3. 같은 Fake로 새 목적지를 시작하면 이전 안내를 대신한다. 지금 UI에는 병원이 1곳뿐이라 Fake 테스트로만 확인한다.
4. 도착 뒤: 정차 도착 화면의 [닫기] → Idle.

### 6. 시연용 샘플 병원

- 위치: `data/hospital/SampleHospitals.kt` (`Destination` 값 1개).
  - `AppContainer`가 꺼내 `GuidanceViewModel`에 넘긴다. FAKE·REAL 모드 모두 같은 값을 쓴다.
  - ui는 data를 import하지 않는다.
- 병원 검색용 인터페이스는 이번에 만들지 않는다. 검색 조건(위치, 증상)과 출처(문의 A1)가 정해지지 않아 모양을 추측하게 되기 때문이다. 검색 계획에서 이 파일을 대체한다.
- 값의 기본안 (`[사용자 승인 필요]` 결정 2):
  - 이름: 가상의 이름 **"시연용 동물병원"**. 실제 병원 이름은 쓰지 않는다. 영상에 나오므로 제3자 권리 문제를 피한다 (requirements.md "출품 유의사항").
  - 좌표: Pleos NaviHelper 문서 예시 좌표 `37.48544722, 127.03636666`. Fake에서는 쓰이지 않고, Real 후속 계획에서 경로 요청 시험에 쓴다.
  - 주소: `null` (화면에 주소를 보여주지 않는다).

### 7. 정차 화면의 자리표시

- "예시 기능 1/2" 버튼 줄을 길안내 영역으로 바꾼다. 문자열 `parked_example_action_1/2`도 지운다.
- 메모 입력 예시는 남긴다. 정차 중에만 쓰는 입력의 예시다. 시연 영상 전에 정리할지는 노트의 "남은 일"에 적는다.

### 패키지 (이번에 추가·변경)

```
app/src/main/java/com/meongcare/app/
  domain/guidance/
    Destination.kt            목적지 (이름, 좌표, 주소?)
    GuidanceState.kt          Idle / Guiding(destination, remainingTime: Duration?) / Arrived(destination)
    GuidanceSource.kt         길안내 인터페이스
  data/guidance/
    FakeGuidanceSource.kt     시간이 흐르면 남은 시간이 줄고 도착한다 (scope·timing 주입)
    FakeGuidanceTiming.kt     (FakeGuidanceSource.kt 안에 둬도 됨)
    RealGuidanceSource.kt     자리표시. 항상 Idle. start/cancel은 로그만 남긴다
  data/hospital/
    SampleHospitals.kt        시연용 샘플 병원
  data/demo/
    FakeTripScenario.kt       FAKE 전용: 안내 시작 → 출발, 도착 → 주차
  di/AppContainer.kt          applicationScope, fakeGuidanceSource, guidanceSource, sampleHospital, fakeTripScenario(FAKE만)
  ui/guidance/
    GuidanceUiState.kt        Idle(destinationName) / Guiding(destinationName, remainingMinutes: Int?) / Arrived(destinationName)
    GuidanceViewModel.kt      GuidanceSource + 샘플 병원 → GuidanceUiState. onStart / onCancel / onDismissArrival
    ParkedGuidanceSection.kt  정차 화면 안의 길안내 영역
    ArrivedScreen.kt          정차 + 도착: 진료 카드 자리표시
  ui/home/
    HomeContent.kt            (HomeUiState, GuidanceUiState) → PARKED / ARRIVAL / DRIVING
    HomeScreen.kt             두 상태를 받아 HomeContent로 화면을 고른다 (수정)
    ParkedScreen.kt           길안내 영역을 넣는다 (수정)
    DrivingScreen.kt          GuidanceUiState에 따라 상태 한 줄을 고른다 (수정)
  MainActivity.kt             GuidanceViewModel을 만들어 HomeScreen에 넘긴다 (수정)
app/src/test/java/com/meongcare/app/
  data/guidance/FakeGuidanceSourceTest.kt, RealGuidanceSourceTest.kt
  data/demo/FakeTripScenarioTest.kt
  ui/guidance/GuidanceViewModelTest.kt
  ui/home/HomeContentTest.kt
```

- `AppContainer.applicationScope`: `CoroutineScope(SupervisorJob() + Dispatchers.Default)`. 프로세스와 수명이 같다. Fake 길안내와 시나리오가 쓴다.
  - Real 길안내 후속 계획에서도 콜백 → Flow 변환에 쓸 수 있다.
- 새 의존성은 없다. 코루틴, lifecycle, Turbine은 이미 있다.

## 확인 필요 (시작 전에 해결)

### [사용자 승인 필요]: 기획 결정

새 `pleos.car.permission.*`은 이번 범위에서 필요 없다. Pleos API를 하나도 새로 쓰지 않는다. 아래는 모두 기획 결정이다. 기본안으로 진행해도 되는지 한 번에 확인받는다.

1. **주행 중 안내 화면에 [안내 취소] 큰 버튼 1개를 둘까?** (단계 5 전에 필요)
   - 기본안: **두지 않는다.** 취소는 정차 중에만 한다.
   - 근거:
     - 주행 중 터치를 줄인다.
     - 잘못 누르면 되돌리기 어렵다. 확인 팝업은 "주행 중 전체 화면 팝업 금지"(ux-guidelines)에 걸린다.
     - Real에서는 Navigation 앱 자체에서 취소할 수 있다.
     - 주행 화면의 버튼 1자리는 나중 기능(병원 전화 등)에 남겨 둔다.
   - 두기로 하면: 높이 `Dimens.DrivingPrimaryButtonHeight`(96dp) 버튼 1개. 완료 기준 4의 `Button`을 "1건 이하"로 바꾼다.
2. **샘플 병원 표시 이름과 좌표** (단계 3 전에 필요)
   - 기본안: 가상 이름 "시연용 동물병원", Pleos 문서 예시 좌표, 주소는 표시 안 함 (위 결정 6).
   - 시연 영상에 그대로 나온다. 원하는 이름이 있으면 알려 달라.
3. **주행 중·안내 없음 화면에 [병원으로 안내] 큰 버튼을 둘까?**
   - 기본안: **이번에는 두지 않는다.** 서비스 개요의 "운전 중 SOS"와 관계있지만 DO 선언(문의 B1)과 음성 입력(문의 B2)이 정해지지 않았다.
4. **도착 화면을 언제 끝낼까?**
   - 기본안: [닫기]를 누를 때까지 유지한다. 자동으로 닫지 않는다. 진료 카드를 병원에 보여 주는 화면이 될 것이기 때문이다.
5. **시연 속도**
   - 기본안: 시작 후 약 3초에 출발, 약 25초에 도착, 약 30초에 도착 화면 (결정 2, 3의 표).
   - 값은 `FakeGuidanceTiming`과 시나리오 timing에서 바꾼다. 영상을 찍으면서 조정해도 된다.

### [선행: pleos-docs-researcher]

- **이번 단계에는 없다.** Pleos SDK를 새로 호출하지 않는다.

### 후속 계획(Real 길안내)으로 넘기는 질문

Real 길안내 계획을 쓸 때 `[선행: pleos-docs-researcher]`로 확인한다. 이번 인터페이스는 이 답들에 묶이지 않게 잡았다.

- `TripInfo.getEta()`는 "남은 시간"인가, "도착 예정 시각"인가? 단위는 무엇인가? `DrivingInfo.destination.duration`과 무엇이 다른가?
  - 값이 없거나 이상할 때(음수, 0, 아주 큰 값) Real 래퍼는 `null`(경로 찾는 중/모름)로 바꾼다. `Duration.INFINITE`를 내보내면 화면이 "1분"을 보여준다 (005 tester 관찰).
- `RouteInfo`의 `poiId`/`poiSubId`가 필수인가? 좌표와 이름만으로 경로를 요청할 수 있나? (sdk-notes 미확인)
- Pleos 에뮬레이터의 기본 Navigation 앱이 `requestRoute`를 받는가? `onRouteStarted` → `onTripInfo`(주기) → `onDestinationArrived`가 실제로 오는가? 에뮬레이터에서 주행을 흉내 내 도착 이벤트를 받을 수 있나?
- Navigation 앱에서 사용자가 경로를 취소하면 `onRouteCancelled`가 오는가?
- 앱이 다시 켜질 때 진행 중인 경로를 알아내는 방법 (`getRouteStateInfo()`의 `isRouting()`, `getDestinationInfo()`)
- `NaviHelper`의 initialize/release를 어디서 관리할까? (`VehicleConnection`처럼 한 곳에서)
- 권한: `NAVI_ROUTE_SEARCH`와 `NAVI_ROUTE`는 팀 승인을 받았다 (architecture.md). 공식 샘플 매니페스트의 `NAVI_CUSTOM_ROUTE`, `NAVI_CUSTOM_ETC`가 필요해지면 `[사용자 승인 필요]`.

## 단계

단계 하나 = 커밋 1~3개. 커밋마다 `assembleDebug`가 통과해야 한다 (precommit 훅).

**시작 전 준비:**
- 위 "확인 필요"의 기본안을 사용자에게 확인받는다.
- 사용자에게 `my_avd`를 **Cold boot**로 켜 달라고 요청한다. UI 단계(5)는 커밋 전에 `/verify`로 본다 (git.md).
  - 기기가 없으면 빌드만 확인하고 커밋한다. 화면은 단계 7에서 몰아서 보고, 진행 로그에 적는다.
- 미추적 폴더 `.agents/`, `.codex/`는 이 작업과 관계없다. 스테이징하지 않는다. 파일 이름으로만 `git add`한다.

**순서 메모:** 기본 순서(인터페이스·Fake → 도메인·UI → Real)와 달리 Real 자리표시를 UI 앞(단계 3)에 둔다.
- 이번 Real은 몇 줄짜리 자리표시다.
- `AppContainer`가 REAL 분기에 끼울 구현이 있어야 배선이 빌드되고, 배선이 있어야 UI 단계마다 화면을 볼 수 있다.

### 필수

- [x] 0. 이 계획 파일 커밋 → `docs(plans): 005 길안내 Fake 계획 추가`
  - 확인: `git log -1 --stat`에 이 파일만 있다.

- [x] 1. 길안내 인터페이스와 Fake
  - `domain/guidance/Destination.kt`, `GuidanceState.kt`, `GuidanceSource.kt` (결정 1)
    - KDoc에 적을 것:
      - Real은 NaviHelper로 구현한다 (후속 계획).
      - 남은 시간을 `Duration`으로 두는 이유 (ETA 단위·의미 미확인)
  - `data/guidance/FakeGuidanceSource.kt` + `FakeGuidanceTiming` (결정 2)
  - `FakeGuidanceSourceTest` (`runTest` + `backgroundScope` + `advanceTimeBy`):
    - `` `처음에는 안내 중이 아니다`() ``
    - `` `안내를 시작하면 남은 시간을 모르는 안내 중 상태를 먼저 내보낸다`() ``
    - `` `경로 계산 시간이 지나면 처음 남은 시간을 내보낸다`() ``
    - `` `시간이 지나면 남은 시간이 한 단계씩 줄어든다`() ``
    - `` `남은 시간이 다 줄면 0분 없이 도착 상태를 내보낸다`() ``
    - `` `안내를 취소하면 안내 없음으로 돌아가고 시간이 지나도 더 바뀌지 않는다`() ``
    - `` `도착 화면을 닫으면 안내 없음으로 돌아간다`() ``
    - `` `안내 중에 다른 목적지로 시작하면 새 목적지로 처음부터 다시 센다`() ``
    - 테스트는 짧은 timing을 넘겨서 쓴다. 기본값에 기대지 않는다.
  - 커밋 (diff 200줄을 넘지 않게 둘로 나눈다):
    - `feat(domain): 길안내 인터페이스와 상태 모델 추가`
    - `feat(data): 시간이 지나면 도착하는 Fake 길안내 추가`
  - 확인: `.\gradlew.bat testDebugUnitTest --tests 'com.meongcare.app.data.guidance.FakeGuidanceSourceTest'` 통과

- [x] 2. Fake 시연 시나리오 (주행 상태 연동) → `feat(data): Fake 길안내에 맞춰 Fake 주행 상태를 바꾸는 시나리오 추가`
  - `data/demo/FakeTripScenario.kt` (결정 3)
    - 생성자: `scope`, `GuidanceSource`(구독용), `FakeDrivingStateSource`, timing(`departDelay` 3초, `parkDelay` 5초)
    - `start()`를 부르면 구독을 시작한다. 두 번 불러도 한 번만 구독한다.
  - `FakeTripScenarioTest`:
    - `` `안내를 시작하고 출발 지연이 지나면 주행 상태가 된다`() ``
    - `` `출발 지연 전에 안내를 취소하면 주행 상태로 바꾸지 않는다`() ``
    - `` `도착하고 주차 지연이 지나면 정차 상태가 된다`() ``
    - `` `주행 중에 안내를 취소해도 주행 상태는 그대로다`() ``
  - 확인: `FakeTripScenarioTest` 통과

- [x] 3. Real 자리표시, 샘플 병원, AppContainer 배선
  - `data/guidance/RealGuidanceSource.kt`
    - `state`는 항상 `Idle`이다. `start`/`cancel`은 `Log.w`만 남긴다. `dismissArrival`은 아무것도 하지 않는다.
    - KDoc: "NaviHelper 구현 전 자리표시, 후속 계획". 위 "후속 계획으로 넘기는 질문"을 가리킨다.
  - `RealGuidanceSourceTest`:
    - `` `Real 자리표시는 항상 안내 없음 상태다`() ``
    - `` `안내를 시작해도 상태가 바뀌지 않는다`() ``
    - 주의: JVM 단위 테스트에서 `android.util.Log`를 부르면 "not mocked" 예외가 난다. 이 프로젝트는 `testOptions.unitTests.isReturnDefaultValues`를 켜지 않았다.
      - 기본안: 로그 함수를 생성자로 받는다 (기본값은 `Log.w`). 테스트는 빈 함수를 넘긴다.
      - `isReturnDefaultValues`를 전체에 켜는 것은 피한다. 다른 테스트에서 실수로 부른 Android API까지 조용히 넘어가기 때문이다.
      - 고른 방법을 "결정 / 배운 점"에 적는다.
  - `data/hospital/SampleHospitals.kt` (결정 6, `[사용자 승인 필요]` 2의 답을 반영)
  - `di/AppContainer.kt`:
    - `applicationScope`
    - `fakeGuidanceSource` (`fakeDrivingStateSource`처럼 항상 만든다)
    - `guidanceSource`: FAKE → Fake, REAL → Real
    - `sampleHospital`
    - FAKE 모드에서만 `FakeTripScenario`를 만들고 `start()`한다.
  - 커밋:
    - `feat(data): Real 길안내 자리표시 구현 추가`
    - `feat(di): 길안내 소스와 시연용 샘플 병원 연결`
  - 확인: `assembleDebug`, `RealGuidanceSourceTest` 통과. 기기가 있으면 실행해서 크래시가 없는지만 본다 (화면은 아직 그대로다).

- [x] 4. 길안내 ViewModel과 화면 상태 → `feat(ui): 길안내 ViewModel과 화면 상태 추가`
  - `ui/guidance/GuidanceUiState.kt`, `GuidanceViewModel.kt`
    - 생성자: `GuidanceSource`, `Destination`(샘플 병원)
    - `uiState`: `stateIn(viewModelScope, WhileSubscribed(5_000), Idle(샘플 병원 이름))`
    - 남은 시간 → `remainingMinutes`: 분 단위로 올림하고, 1보다 작으면 1로 한다. `null`은 `null` 그대로 둔다 ("경로를 찾는 중").
    - `onStart()`는 샘플 병원으로 `start`, `onCancel()`은 `cancel`, `onDismissArrival()`은 `dismissArrival`을 부른다. 앞의 둘은 `viewModelScope.launch`로 부른다.
    - factory: `viewModelFactory { initializer { ... } }` (HomeViewModel과 같은 방식)
  - `GuidanceViewModelTest` (기존 `MainDispatcherRule` 사용):
    - `` `안내 중이 아니면 샘플 병원 이름을 안내 시작 대상으로 보여준다`() ``
    - `` `남은 시간을 모르면 남은 분 없이 안내 중으로 보여준다`() ``
    - `` `남은 시간을 분 단위로 올림해서 보여준다`() `` (90초 → 2분)
    - `` `남은 시간이 1분보다 짧으면 1분으로 보여준다`() ``
    - `` `안내 시작을 누르면 샘플 병원으로 안내를 요청한다`() ``
    - `` `안내 취소를 누르면 안내 취소를 요청한다`() ``
    - `` `도착 화면을 닫으면 도착 확인을 요청한다`() ``
    - 요청 확인용으로 테스트 안에 기록용 `GuidanceSource`를 둔다. ViewModel 테스트에 Fake의 시간 흐름을 섞지 않는다.
  - 확인: `GuidanceViewModelTest` 통과

- [x] 5. 화면 (Fake로 동작). 커밋마다 기기가 있으면 `/verify`로 그 화면을 본다.
  - 공통 규칙:
    - 주행 화면은 큰 글씨 상태 한 줄 + 작은 줄 1개. 버튼, 텍스트 입력, 스크롤, 애니메이션은 없다 (AGENTS 규칙 3, 결정 4).
    - 크기는 `Dimens`, 글자는 `MaterialTheme.typography` 값만 쓴다.
    - 문자열은 모두 `strings.xml`에 둔다.
  - 5-1. `ui/home/HomeContent.kt` + `HomeContentTest`, `HomeScreen`·`MainActivity` 배선, 정차 화면 길안내 영역
    - `homeContentOf(home: HomeUiState, guidance: GuidanceUiState): HomeContent`
      - `HomeUiState.Driving` → `DRIVING` (안내 상태와 상관없이)
      - `Parked` + `Arrived` → `ARRIVAL`
      - 그 밖의 `Parked` → `PARKED`
    - `HomeContentTest`:
      - `` `주행 중에는 도착해도 도착 화면 대신 주행 화면이다`() ``
      - `` `정차 중에 도착 상태면 도착 화면이다`() ``
      - `` `정차 중에 안내 중이거나 안내가 없으면 정차 화면이다`() ``
    - `HomeScreen`: `guidanceUiState`와 콜백 3개(`onStartGuidance`, `onCancelGuidance`, `onDismissArrival`)를 받는다. `when (homeContentOf(...))`로 고른다. 애니메이션 없이 바로 바꾼다.
      - `ARRIVAL`은 이 단계에서 임시로 정차 화면을 보여준다. 5-3에서 도착 화면으로 바꾼다.
    - `MainActivity`: `GuidanceViewModel`을 만들고(`container.guidanceSource`, `container.sampleHospital`) `HomeScreen`에 넘긴다.
    - `ui/guidance/ParkedGuidanceSection.kt`를 만들고 `ParkedScreen`의 예시 버튼 줄을 이것으로 바꾼다 (결정 7).
      - Idle: 병원 이름 + [병원으로 안내] (`Dimens.MinTouchTarget` 이상)
      - Guiding: "병원까지 N분" 또는 "경로를 찾는 중" (headline) + 병원 이름 + [안내 취소]
    - 커밋: `feat(ui): 정차 화면에서 병원 길안내 시작·취소 추가`
  - 5-2. `DrivingScreen`이 `GuidanceUiState`를 받아 상태 한 줄과 작은 줄을 고른다 (결정 4 표의 주행 행).
    - 커밋: `feat(ui): 주행 화면에 병원까지 남은 시간 표시`
  - 5-3. `ui/guidance/ArrivedScreen.kt`
    - "○○에 도착했어요"
    - 진료 카드 자리표시 카드 ("진료 카드는 준비 중이에요" 같은 한 줄)
    - [닫기]
    - 정차 화면이므로 세로 스크롤을 둬도 된다 (가로 화면 대비).
    - `HomeScreen`의 `ARRIVAL` 분기를 이 화면으로 바꾼다.
    - 커밋: `feat(ui): 도착 화면 추가 (진료 카드 자리표시)`
  - 확인:
    - `assembleDebug`, `HomeContentTest` 통과
    - 완료 기준 4의 Grep 중 `DrivingScreen.kt`·`HomeScreen.kt` 항목
    - 기기가 있으면 `/verify`로 각 커밋의 화면을 본다

- [x] 6. 경계 테스트 (`tester`) → `test: 길안내 Fake와 화면 상태 경계 테스트 추가`
  - 메인이 `tester`에게 **이 계획 파일 경로와 대상만** 넘긴다. 구현 설명은 넘기지 않는다.
    - 대상: `FakeGuidanceSource`, `FakeTripScenario`, `RealGuidanceSource`, `GuidanceViewModel`, `homeContentOf`
  - 기대 동작 (계약 기준):
    - 결정 2의 "규칙"과 흐름 표
    - 결정 3의 시나리오 전환과 취소 시 동작
    - 결정 4의 화면 조합 표와 남은 시간 표시 규칙 (올림, 최소 1분, `null`)
    - 구독 계약: 구독하면 현재 상태를 먼저 받고, 같은 상태를 연달아 받지 않는다
    - Arrived에서 `cancel`, Idle에서 `dismissArrival`이 아무것도 바꾸지 않는다
  - tester가 찾은 버그는 메인이 프로덕션 코드에서 `fix:` 커밋으로 따로 고친다.
  - 확인: `.\gradlew.bat testDebugUnitTest` 통과. 통과 수와 실패 수를 진행 로그에 적는다.

- [x] 7. 전체 검증 (`/verify`). 커밋은 없다. 고친 게 있으면 `fix(...)` 커밋으로 낸다.
  - `node .claude/hooks/build-gate.mjs pipeline` → "검증 파이프라인 통과."
  - 완료 기준 3의 시나리오를 모두 돈다.
    - 시간 맞추기:
      - "도착했어요"(주행) 화면은 약 25~30초 사이에만 보인다.
      - `uiautomator dump`를 1~2초 간격으로 반복해서 글자가 나오면 캡처한다.
      - 잘 안 되면 있으면 좋음 B([바로 도착])를 먼저 한다.
    - 버튼 좌표는 `uiautomator dump`에서 얻는다.
    - 새 화면(정차+안내 중, 도착)을 가로와 다크 모드로도 본다. 확인한 뒤 회전·다크 설정을 되돌린다.
  - 완료 기준 4의 Grep을 모두 돌린다.
  - 기기가 없으면 멈추고 사용자에게 Cold boot를 요청한다. 확인하지 못한 건 그대로 적는다.

- [x] 8. 문서 맞추기
  - `docs/architecture.md`:
    - "패키지 구조"에 이번에 추가한 패키지를 넣는다.
    - "주행 상태와 화면"에 결정 4의 화면 조합 표와 취소 경로를 넣는다.
    - "데이터 모드"에 FAKE 전용 `FakeTripScenario`를 한 줄로 적는다.
    - "서비스 기능 스택" 표의 길안내·도착 감지 행은 고치지 않는다. Real 구현 전이다.
  - `docs/pleos/sdk-notes.md` "미확인"에 한 줄 더한다: `TripInfo.eta`가 남은 시간인지 도착 시각인지 (출처: 이번 계획의 판단. 문서·클래스에 없음).
  - 커밋:
    - `docs(architecture): 길안내 구조와 주행·안내 화면 조합 기록`
    - `docs(pleos): TripInfo.eta 의미 미확인 항목 추가`
  - 확인: 완료 기준 5

- [ ] 9. 개발 노트 작성 (`docs/notes/`)
  - `docs/notes/xxj15/YYYY-MM-DD-navi-route-fake.md`. 형식은 `docs/notes/README.md` 템플릿을 따른다.
    - 사용한 Pleos SDK: 없음. 권한: 없음. NaviHelper는 후속 계획이다.
    - 시연 방법: 결정 2·3의 타임라인과 조작 순서 (debug 빌드, Fake 모드)
    - 결정: Fake 둘을 시나리오로 잇는 이유, 주행 중 취소 버튼을 두지 않은 이유, 화면 조합 표 링크
    - 남은 일:
      - Real 길안내
      - 병원 검색
      - 진료 카드
      - 메모 예시 자리표시 정리
      - release(항상 Real)에서는 길안내가 동작하지 않는다
  - 이 계획을 `docs/plans/completed/`로 옮기고 진행 로그를 갱신한다.
  - 커밋:
    - `docs(notes): navi-route-fake`
    - `docs(plans): 005 길안내 Fake 완료`
  - 확인: `git status`에 이 작업의 변경이 남지 않았다 (`.agents/`, `.codex/` 제외). 그다음 `/ship`.

### 있으면 좋음

- [ ] A. debug 패널 접기 → `feat(ui): debug 패널 접기 추가`
  - 시연 영상에 "DEBUG · Fake" 띠가 보이지 않게 한다. 시나리오가 주행 상태를 알아서 바꾸므로, 영상 중에는 패널이 필요 없다.
  - 접힌 상태에서는 화면 구석에 작은 펼치기 손잡이만 남긴다. debug 빌드에서만 보인다.
  - 확인: `/verify`로 접기·펼치기, 접힌 상태에서 시연 시나리오 한 바퀴
- [ ] B. debug 패널에 [도착] 버튼 (FAKE만) → `feat(ui): debug 패널에 Fake 바로 도착 버튼 추가`
  - `FakeGuidanceSource.arriveNow()`: Guiding일 때만 바로 Arrived로 바꾼다. 테스트를 같은 커밋에 넣는다.
  - 쓰임: `/verify`와 영상 다시 찍기를 빠르게 한다.
  - 좁은 세로 화면에서 아랫줄 버튼 4개가 깨지지 않는지 본다 (004에서 같은 문제가 있었다).
- [ ] C. 시연 영상 촬영 절차를 노트에 적는다 (단계 9 노트에 포함)
  - `adb shell screenrecord /sdcard/demo.mp4` → `adb pull`. 세로·가로 중 어느 쪽으로 찍을지는 영상 담당과 정한다.

## 진행 로그

- 2026-11-05: 계획 작성.
  - 브랜치 `feat/navi-route-fake`는 dev(`b01c2de`)에서 땄다.
  - 시작 시점의 코드: 004의 뼈대 (주행/정차 화면, Fake/Real 주행 상태, debug 패널, VehicleConnection). 길안내 관련 코드는 없다.
- 2026-11-05: "확인 필요" 1~5를 사용자가 모두 기본안으로 승인했다.
  - 주행 중 [안내 취소] 버튼 없음, 샘플 병원 "시연용 동물병원", 주행 중 [병원으로 안내] 버튼 없음, 도착 화면은 [닫기]까지 유지, 시연 속도 기본값.
- 2026-11-05: 단계 0~5 완료 (커밋 `59bb64d`~`e51b1b4`).
  - 새 단위 테스트 25개 통과: FakeGuidanceSource 8, FakeTripScenario 4, RealGuidanceSource 2, GuidanceViewModel 8, HomeContent 3.
  - 기기(`my_avd`)가 연결되지 않아 단계 5의 커밋마다 `/verify`는 하지 못했다. 빌드만 확인했다. 화면은 단계 7에서 몰아서 본다.
- 2026-11-05: 단계 6 완료 (`029481c`). tester가 경계 테스트 38개를 더했다. 전체 89개 통과, 실패 0. 계약 위반 버그 없음.
  - tester 관찰 (계약 밖): `toDisplayMinutes`에 `Duration.INFINITE`가 오면 덧셈이 넘쳐 "1분"이 된다. 지금 Fake는 그 값을 내지 않고, 계약상 "모름"은 `null`이다. → Real 후속 계획에서 ETA를 모르면 `INFINITE`가 아니라 `null`로 바꾼다 (아래 후속 질문에 추가).
- 2026-11-05: 단계 8 완료 (`d4017b7`, `4e3a25a`). 단계 7 중 기기 없이 되는 것: 검증 파이프라인 통과, 완료 기준 4의 Grep 모두 0건.
  - 병합 매니페스트의 `pleos.car.permission` 1건은 우리 매니페스트의 XML 주석이다. 실제 선언(`android:name="pleos.car.permission`)은 0건.
- 2026-11-05: 단계 7 완료. 사용자 요청으로 `my_avd`를 Cold boot해서 `/verify` (1080x2400, 420dpi, debug, Fake).
  - 시연 시나리오: 정차(목적지 + [병원으로 안내]) → "경로를 찾는 중" → 정차 "병원까지 12분" + [안내 취소] → 저절로 주행 "병원까지 10분"·"8분" → 주행 "도착했어요" → 저절로 정차 + 도착 화면 → [닫기] → 처음 정차 화면. 모두 기대대로.
  - 주행 화면 UI 트리 (debug 패널 제외): `EditText` 0개, 클릭 가능한 노드 0개.
  - 취소 경로 1: 시작 2초 뒤 취소 → 처음 정차 화면. 8초가 지나도 주행으로 바뀌지 않았다.
  - 취소 경로 2: 주행 "병원까지 11분" 중 debug [정차] → 정차 "병원까지 8분" + [안내 취소] → 취소 → 처음 정차 화면. 5초 뒤에도 그대로였고 도착도 오지 않았다.
  - 가로·다크: 정차+안내 중, 도착 화면 모두 글자 잘림·겹침 없음.
  - Real 전환 → "주행 중"(이전과 같음) → Fake 복귀. 크래시 없음 (`logcat *:E`, crash 버퍼에 `meongcare` 없음).
  - **발견해서 고친 것** (`905dc12`): 도착 화면 제목 "시연용 동물병원에 도착했어요"가 세로 화면에서 "요" 한 글자만 다음 줄로 넘어갔다. "도착했어요"와 병원 이름을 두 줄로 나눴다. 다시 설치해서 확인했다.
  - **알려진 한계** (고치지 않음): 휴대폰 가로 화면(높이 약 411dp)에서는 debug 패널이 높이의 약 1/3을 차지해 [안내 취소]·[닫기]가 화면 아래로 밀린다. 스크롤하면 닿는다(도착 화면에서 확인). release에는 패널이 없다. 가로로 시연 영상을 찍으면 있으면 좋음 A(패널 접기)로 해결한다.

## 결정 / 배운 점

- **`FakeGuidanceSource`는 상태 변경을 잠금(`synchronized`) 안에서 한다.**
  - `start`/`cancel`은 화면 쪽(Main)에서, 안내 진행은 `applicationScope`(Default)에서 상태를 바꾼다.
  - 잠금이 없으면, 취소한 직후 이전 진행이 `delay`에서 깨어나 상태를 덮어쓸 수 있다. 진행은 잠금 안에서 `ensureActive()`를 확인한 뒤에만 상태를 바꾼다.
- **`RealGuidanceSource`의 로그 함수는 생성자로 받는다** (기본값 `Log.w`). 테스트는 기록용 함수를 넘긴다. `isReturnDefaultValues`는 켜지 않았다.
- **`AppContainer`는 `fakeGuidanceSource`를 따로 열어 두지 않았다.** 지금은 debug 패널이 Fake 길안내를 직접 조작하지 않는다. 있으면 좋음 B([도착] 버튼)를 하면 그때 연다.
- `GuidanceViewModelTest`는 ViewModel을 `by lazy`로 만든다. 테스트 클래스 필드에서 바로 만들면 `MainDispatcherRule`이 Main을 바꾸기 전에 `viewModelScope`가 생길 수 있다.
- 남은 시간 문구("병원까지 N분" / "경로를 찾는 중")는 정차·주행 화면이 같이 쓰므로 `ui/guidance/GuidanceText.kt`의 `remainingTimeText`로 뺐다.
- 카드 안쪽 여백용 `Dimens.CardPadding`(24dp)을 더했다.
- **도착 화면 제목을 결정 4의 "○○에 도착했어요" 한 문장에서 "도착했어요" + 병원 이름 두 줄로 바꿨다.** 한국어는 글자 단위로 줄이 바뀌어서, 이름이 길면 마지막 글자만 다음 줄로 떨어진다 (`/verify`에서 발견).
