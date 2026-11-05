# 010 Real 길안내 (Pleos NaviHelper)

- 목표: 진료 카드의 [길 안내 시작]이 Pleos NaviHelper로 차량 내비게이션에 병원 경로를 실제로 요청하고, 내비가 보내는 남은 시간·도착·취소를 앱의 길안내 상태로 받는다. Pleos 에뮬레이터에서 안 되는 부분은 아래 "갈래"에 미리 정한 대안으로 한다.
- 완료 기준 (어떻게 검증하나). 갈래 C(아래)면 1과 7만 본다.
  1. `node .claude/hooks/build-gate.mjs pipeline`이 "검증 파이프라인 통과."를 출력한다.
  2. `.\gradlew.bat assembleRelease`가 성공한다.
  3. 단위 테스트:
     - 새 테스트가 통과한다: `NaviRemainingTimeTest`, `NaviGuidanceSourceTest`, `NaviRouteOnlyGuidanceSourceTest`(3a일 때), `NaviPlanTest`, 그리고 tester가 추가한 테스트.
     - 기존 테스트가 모두 그대로 통과한다. 개수는 단계 0에서 센 값이다.
     - `git diff dev --diff-filter=MD --name-only -- app/src/test`의 출력이 비어 있다. 기존 테스트는 고치거나 지우지 않고 추가만 한다. 예외는 U5에서 3b를 골랐을 때 설계 결정 7의 목록뿐이다.
     - `git diff dev -- <경로>`의 출력이 비어 있다. 주행 안전 규칙, 화면 조합, Fake 계약을 건드리지 않는다는 뜻이다:
       - `app/src/main/java/com/meongcare/app/ui/home/HomeViewModel.kt`, `ui/home/HomeContent.kt`, `ui/home/ParkedContent.kt`, `ui/home/DrivingScreen.kt`
       - `app/src/main/java/com/meongcare/app/ui/guidance/GuidanceViewModel.kt`
       - `app/src/main/java/com/meongcare/app/data/guidance/FakeGuidanceSource.kt`, `data/demo/`, `data/driving/`
       - `app/src/main/java/com/meongcare/app/di/SourcePlan.kt` (3b를 고르면 이 줄은 뺀다)
     - `git diff dev -U0 -- app/src/main/java/com/meongcare/app/domain/`에서 `+`/`-` 줄이 모두 주석(`*`, `/**`, `//`)이다. U4로 `poiId`를 더했으면 그 줄만 예외다.
  4. 정적 검사 (Grep):
     - `import ai.pleos.playground.navi`는 `app/src/main/java/com/meongcare/app/data/navi/PleosNaviClient.kt`에만 있다.
     - `NaviHelper(` 생성 호출은 그 파일에만 있다.
     - `data/` 밖에서 `import ai.pleos|import android.car`는 0건이다.
     - `ui/` 아래에서 `import com.meongcare.app.data`, `import com.meongcare.app.di`는 0건이다.
     - `data/navi/NaviClient.kt`, `data/navi/NaviRemainingTime.kt`, `data/guidance/Navi*.kt`에 `import android.`이 0건이다. JVM에서 테스트할 수 있어야 하기 때문이다.
     - 병합 매니페스트(`app/build/intermediates/` 아래 debug `AndroidManifest.xml`):
       - `android:name="pleos.car.permission`이 정확히 2건(`NAVI_ROUTE`, `NAVI_ROUTE_SEARCH`)이다.
       - `NAVI_CUSTOM`은 0건이다.
     - `gradle/libs.versions.toml`에 NaviHelper `3.0.5`가 있고, `app/build.gradle.kts`가 그 별칭을 1번 쓴다.
  5. Pleos 에뮬레이터 (`emulator-5554`, Cold boot): 아래 "확인 절차"의 N(Real 모드)과 H(시연 모드, 3a일 때), R1·R3를 본다.
     - `adb -s emulator-5554 logcat -d -t 500 *:E`에 `com.meongcare.app` 크래시와 `SecurityException`이 없다.
  6. 일반 폰 에뮬레이터 (`emulator-5556`): F1~F3과 R1·R2를 본다. NaviHelper 로그가 0줄이다 (만들지 않는다).
  7. 문서:
     - `docs/pleos/sdk-notes.md` NaviHelper 절에 spike 관찰 블록이 출처 태그와 함께 있다.
     - 같은 파일 "미확인"의 `TripInfo.eta` 뜻, `poiId` 필수 여부, 에뮬레이터 Navigation 앱 동작 항목이 관찰 결과로 갱신돼 있다.
     - `docs/architecture.md`가 갱신돼 있다: "서비스 기능 스택"의 길안내·도착 예정 시간·도착 감지 행, "데이터 모드"의 내비 연동 표, "NaviHelper 수명주기" 절, "패키지 구조".
     - `AGENTS.md`에 NaviHelper initialize/release를 한 곳에서 한다는 줄이 있다. release 시연 모드 문구가 U5 결정과 맞는다.
     - `README.md`의 시연 방법과 안내 문구가 U5 결정과 맞는다.
- 브랜치: `feat/real-navi-guidance` (dev `4fedbca`에서 땄다)
- 관련 문서:
  - AGENTS.md: 규칙 1·2·3·7, 아키텍처 규칙 (SDK는 `data/` 래퍼에서만, 콜백은 suspend/Flow로, Fake 유지, release는 시연 모드)
  - docs/pleos/sdk-notes.md:
    - "NaviHelper SDK (v3.0.5 기준)" (약 455~608행): 권한별 API, 생성·초기화·해제, `requestRoute(RouteInfo)` 원문 코드, 리스너 26개 시그니처, `TripInfo`/`DrivingInfo` getter
    - "미확인" (약 1046~1051행)
    - "차량 서비스가 없는 기기에서의 동작" (Vehicle 관찰 방식의 선례)
  - docs/pleos/ux-guidelines.md:
    - "멀티 윈도우 정책": Navigation 앱은 닫을 수 없다. 딥링크로 띄운 앱이 덮는다.
    - "Maps/Navigation 앱 가이드의 주행 중 조작 기준": 단일 경로 정책
  - docs/pleos/emulator-setup.md:
    - "설치 후 확인한 것": 내비 패키지 `ai.umos.maps.android.navigation.app`
    - "Pleos 에뮬레이터에서 직접 해 본 것": Virtual Console HTTP 요청
  - docs/notes/xxj15/2026-11-05-real-driving-state.md: "시연 방법"의 `vc()` 함수
  - docs/architecture.md: "서비스 기능 스택", "사용하기로 한 Pleos 권한", "데이터 모드", "Vehicle SDK 수명주기", "Car(AAOS) 연결 수명주기", "주행 상태 × 길안내 상태 화면 조합", 문의 A2
  - docs/plans/completed/:
    - 005: 결정 1의 Real 대응 표, "후속 계획(Real 길안내)으로 넘기는 질문"
    - 006: 설계 결정 1, JVM 테스트를 위한 층 나누기
    - 007: 데이터 모드 표, 확인 절차, release 확인용 사본(결정 5a)
    - 009: 카드 흐름
  - .claude/skills/verify/SKILL.md, docs/conventions/git.md

## 범위

- **필수** (약 1.5~2일, 두 에뮬레이터 확인 포함): 단계 0~9.
  - 11/15 제출물에 주는 효과:
    - **기획서 "기술 활용성"**: "병원 경로를 Pleos NaviHelper `requestRoute`로 차량 내비에 보낸다"를 확인한 사실로 쓸 수 있다. 확인하지 못한 부분은 사실대로 적는다.
    - **시연 영상**: 차량 내비 화면에 "시연용 동물병원" 경로가 뜨는 장면을 담는다.
    - **APK**: 차량 기기에서 Pleos SDK를 핵심 흐름에 쓴다 (U5 결정에 따라).
  - 순서는 spike(사실 확보)를 먼저 하고, 기획서에 쓸 사실을 문서로 남긴 다음 구현한다.
- **있으면 좋음**: A~D. 필수가 끝난 뒤, 단계 9(노트) 전에 한다.
- **이번 범위 밖**:
  - 병원 검색(카카오 로컬, 문의 A1). 병원은 지금처럼 시연용 1곳이다.
  - 경유지, 경로 옵션 변경, 재탐색.
  - `requestNavigationScreen`: `NAVI_CUSTOM_MAP`은 승인되지 않았다.
  - 주행 중 안내 시작, 음성 입력(문의 B1·B2).
  - 실차 확인.

## spike 결과에 따른 갈래

단계 1(spike)이 끝나면 아래 셋 중 하나로 정하고 진행 로그에 적는다. 갈래마다 할 단계가 다르다.

| 갈래 | 조건 (spike 관찰) | 할 단계 |
|---|---|---|
| **A** 전부 됨 | `requestRoute`로 내비 안내가 시작되고, 남은 시간 이벤트가 오고, **에뮬레이터에서 도착(`onDestinationArrived`)을 만들 수 있다** | 0~9 전부. Real 모드는 NaviHelper 전부를 쓴다. 시연 모드는 U5대로 한다 |
| **B** 경로만 확실함 | 경로 요청은 되지만, 남은 시간이 안 바뀌거나 도착을 에뮬레이터에서 만들 수 없다 | 0~9. Real 모드 상태 기계는 만들되 도착은 JVM 테스트로만 확인한다. 시연 모드는 3a(경로만 내비)를 권장한다. 내비 이벤트가 하나도 오지 않으면 단계 2-2를 건너뛴다. 그러면 Real 모드는 지금 자리표시 그대로이고, 3a만 한다 |
| **C** 안 됨 | 연결이 안 되거나, 내비가 요청을 받지 않거나, 승인되지 않은 권한이 필요하다 | 0, 1, 8(축소), 9. 코드는 바꾸지 않는다. 관찰을 기록하고 문의 B5를 추가한다 (U8) |

- spike는 **0.5일 안에** 끝낸다. 넘기면 그때까지 본 것으로 B나 C를 정한다.

## 설계 결정 (planner 판단과 근거)

아래 코드 블록은 기본안이다. 이름과 세부는 구현하면서 바꿔도 된다. 바꾸면 이 블록을 고치고 "결정 / 배운 점"에 이유를 남긴다.

### 1. 층을 나눈다: SDK 객체 없이 JVM으로 시험할 수 있게

- NaviHelper의 데이터 클래스(`TripInfo` 등)를 JVM 테스트에서 직접 만들 수 있는지는 확인하지 않는다.
- 대신 006과 같은 방식으로 나눈다.
  - 어댑터는 SDK 객체에서 **원시값(Int 등)만 꺼낸다.**
  - 원시값을 해석하는 일(남은 시간 변환, 상태 기계)은 `ai.pleos`도 `android`도 없는 층에서 한다.

| 층 | 파일 | `ai.pleos` | 확인 방법 |
|---|---|---|---|
| 경계 | `data/navi/NaviClient.kt`: `NaviClient`, `NaviSession`, `NaviEvent` | 없음 | 컴파일 |
| 변환 | `data/navi/NaviRemainingTime.kt`: `remainingTimeOf(raw, nowMillis): Duration?` | 없음 | JVM 테스트 |
| 상태 기계 (전부 내비) | `data/guidance/NaviGuidanceSource.kt` | 없음 | JVM 테스트 (가짜 client) |
| 상태 기계 (경로만 내비) | `data/guidance/NaviRouteOnlyGuidanceSource.kt` | 없음 | JVM 테스트 |
| 어댑터 | `data/navi/PleosNaviClient.kt`: NaviHelper 생성·초기화·리스너·해제, `Destination` → `RouteInfo` | **여기만** | Pleos 에뮬레이터 |
| 생성 | `data/navi/NaviClientFactory.kt`: `naviClientOrNull(context)` | 없음 (어댑터 클래스만 참조) | 일반 폰 |
| 구성 | `di/NaviPlan.kt`: `NaviUse`, `naviUseOf(mode, isAutomotive)` | 없음 | JVM 테스트 |

```kotlin
// data/navi/NaviClient.kt
sealed interface NaviEvent {
    data object Connected : NaviEvent
    data object ConnectionFailed : NaviEvent
    data object RouteStarted : NaviEvent
    /** 내비가 준 남은 시간 원값. 어느 콜백의 어느 필드인지, 단위가 무엇인지는 spike O6으로 정한다. */
    data class RemainingRaw(val value: Int) : NaviEvent
    data object DestinationArrived : NaviEvent
    data object RouteCancelled : NaviEvent
    data class Error(val code: String) : NaviEvent
}

fun interface NaviClient {
    /**
     * 새 NaviHelper를 만들어 연결을 시작한다.
     * 결과(Connected 또는 ConnectionFailed)와 그 뒤의 이벤트는 [onEvent]로 온다.
     */
    fun connect(onEvent: (NaviEvent) -> Unit): NaviSession
}

interface NaviSession : AutoCloseable {
    fun requestRoute(destination: Destination)
    fun cancelRoute()
    /** 리스너를 떼고 release한다. 두 번 불러도 된다. 그 뒤에 오는 콜백은 버린다. */
    override fun close()
}
```

- production용 Fake client는 만들지 않는다. 가짜 client는 테스트 파일 안에만 둔다 (006과 같은 방침).
  - 도메인 수준의 Fake는 이미 있다: `FakeGuidanceSource`.
- 상태 기계와 경계는 `android.util.Log`를 부르지 않는다. 로그 함수를 생성자로 받는다 (`RealGuidanceSource`와 같은 방식, `isReturnDefaultValues`는 켜지 않는다).
- 로그에 반려견·증상 정보는 넣지 않는다. 병원 이름과 이벤트 이름만 남긴다.

### 2. NaviHelper 수명주기: 길안내 한 번에 NaviHelper 하나

- **안내를 시작할 때 연결하고, 안내가 끝나면 해제한다.**
  - 연결: `connect` → 새 `NaviHelper(appContext)` → `initialize(onConnected, onFailed)` → `addListener`
  - 끝: 도착, 취소, 실패, 내비에서 취소
  - 해제: `close` → `removeListener` → `release()`
  - 안내 중에 다른 목적지로 다시 시작하면 같은 세션으로 `requestRoute`를 다시 부른다 (sdk-notes: 목적지를 바꿀 때도 `requestRoute`).
- **Vehicle과 같은 원칙**:
  - initialize/release는 한 파일(`PleosNaviClient.kt`)에서만 한다.
  - 같은 인스턴스를 release한 뒤 다시 쓰지 않는다. sdk-notes에 "release 후 다시 쓰려면 새 객체"라고 되어 있다.
- **Vehicle과 다른 점** (Vehicle은 앱이 보일 때만 연결한다):
  - 경로를 보내면 내비 앱이 앞으로 나올 수 있다 (spike O4). 그래서 우리 앱이 화면에서 사라져도(onStop) 도착 이벤트를 받아야 한다.
  - 프로세스 수명 내내 연결을 들고 있지도 않는다. 해제할 시점이 없어지고, 안내가 없을 때 연결을 유지할 이유가 없다. Pleos 가이드도 백그라운드에서는 불필요한 수집을 멈추라고 한다.
- **연결 대기**:
  - `requestRoute`는 `Connected`를 받은 뒤에만 부른다.
  - 연결 전에 부르면 어떻게 되는지 모르므로, 그 경우를 아예 만들지 않는다.
  - 대기 시간 `connectTimeout`의 기본값은 5초다. spike O1에서 본 연결 시간의 약 3배로 정하되, 5초보다 짧게 하지 않는다.
- **스레드**:
  - 콜백이 어느 스레드로 오는지는 R2/O1로 확인한다.
  - 상태 기계는 어느 스레드에서 불려도 되게 잠금(`synchronized`)과 `MutableStateFlow`로 만든다 (`FakeGuidanceSource`와 같은 방식).
  - NaviHelper 호출에 메인 스레드가 필요하면 어댑터가 맞춘다.
- **취소 직후 해제**: `cancelRoute()` 바로 뒤에 `release()`해도 취소가 전달되는지 spike O9로 본다. 안 되면 `RouteCancelled`를 받을 때까지, 또는 짧게(약 2초) 기다린 뒤 해제한다.

### 3. 전부 내비 (`NaviGuidanceSource`): 이벤트 → `GuidanceState` 매핑

`GuidanceSource` 계약(구독하면 현재 값을 먼저, 같은 값 연속 없음, `start`/`cancel`/`dismissArrival`의 뜻)은 그대로다.

| 지금 상태 | 받은 것 / 호출 | 다음 상태 | 내비에 하는 일 |
|---|---|---|---|
| Idle·Arrived | `start(d)` | 바로 `Guiding(d, null)` ("경로를 찾는 중") | `connect` → `Connected` 대기 → `requestRoute(d)` |
| Guiding | `start(d2)` | `Guiding(d2, null)` | 같은 세션에 `requestRoute(d2)` (연결 전이면 대기 중인 목적지를 바꾼다) |
| Guiding (연결 전) | `ConnectionFailed`, 또는 `connectTimeout` 초과 | Idle | close |
| Guiding (연결 전) | `cancel()` | Idle | 경로를 요청하지 않고 close |
| Guiding(d) | `RemainingRaw(v)`이고 `remainingTimeOf(v)`가 null이 아님 | `Guiding(d, t)` | |
| Guiding | `RemainingRaw(v)`이고 변환 결과가 null | 그대로 (이전 남은 시간을 지우지 않는다) | |
| Guiding(d) | `DestinationArrived` | `Arrived(d)` | close |
| Guiding | `RouteCancelled` (내비에서 취소) | Idle | close |
| Guiding | `Error(code)` | 기본안: `RouteStarted` 전이면 Idle + close, 뒤면 로그만. spike O11을 보고 바꿀 수 있다 | |
| Guiding | `cancel()` | Idle | `cancelRoute` → close (결정 2의 "취소 직후 해제") |
| Arrived | `dismissArrival()` | Idle | 없음 (이미 해제됨) |
| Idle·Arrived | 내비 이벤트 | 무시. 우리가 시작하지 않은 경로는 따르지 않는다 (있으면 좋음 C) | |
| Idle·Arrived | `cancel()` | 그대로 | 없음 |
| 해제 뒤 | 늦게 온 콜백 | 무시 | |

- `RouteStarted`는 상태를 바꾸지 않는다. "경로가 시작됐는지"만 기록해서 `Error` 처리에 쓴다.
- spike O10에서 안내 중 `requestRoute`를 다시 부르면 이전 경로의 `onRouteCancelled`가 온다는 것이 보이면, 그 취소가 새 안내를 끝내지 않게 규칙을 더한다. 진행 로그와 이 표에 함께 적는다.
- **`remainingTimeOf` 규칙은 spike O6 결과로 정한다** (남은 초인지, 분인지, 도착 시각인지). 정한 규칙과 근거는 KDoc과 sdk-notes에 적는다. 어느 규칙이든 다음을 지킨다:
  - 0 이하, 하루보다 긴 값, 해석할 수 없는 값은 `null`(모름)이다.
  - `Duration.INFINITE`는 내지 않는다. 005 tester의 관찰: 그 값이 오면 화면이 "1분"이 된다.
  - 도착 시각이라서 현재 시각이 필요하면 `nowMillis`를 인자로 받는다. 테스트가 시각을 정할 수 있어야 한다.
- 남은 시간을 `TripInfo.eta`에서 받을지 `DrivingInfo.destination.duration`에서 받을지도 O6에서 정한다. 내비 화면과 맞고 시간에 따라 바뀌는 쪽을 고른다. 어댑터가 고른 값만 `RemainingRaw`로 보낸다.

### 4. 경로만 내비 (`NaviRouteOnlyGuidanceSource`): 시연 모드 차량 기기용 (U5 3a)

에뮬레이터에서는 차가 실제로 움직이지 않는다. 그래서 내비의 남은 시간이 줄지 않거나 도착이 오지 않을 수 있다 (갈래 B). 그래도 시연 흐름이 끝까지 돌게 한다. **경로 요청과 취소만 진짜 내비로 보내고, 남은 시간과 도착은 지금의 Fake 진행(주행 중에만 줄어듦, 007)이 맡는다.**

```kotlin
class NaviRouteOnlyGuidanceSource(
    private val progress: GuidanceSource,   // FakeGuidanceSource(progressGate = 주행 중인지)
    private val client: NaviClient,
    private val scope: CoroutineScope,      // AppContainer.applicationScope
    private val log: (String) -> Unit,
) : GuidanceSource
```

| 일 | 동작 |
|---|---|
| `state` | `progress.state` 그대로 |
| `start(d)` | `progress.start(d)`를 먼저 부른다 (화면이 바로 반응한다). 그다음 `scope`에서 연결 → `Connected` 대기 → `requestRoute(d)`. 실패나 시간 초과는 로그만 남기고, Fake 진행은 계속한다 |
| 안내 중 `start(d2)` | `progress.start(d2)`, 같은 세션에 `requestRoute(d2)` |
| `cancel()` | `progress.cancel()`, 세션이 있으면 `cancelRoute` → close |
| `dismissArrival()` | `progress.dismissArrival()` |
| progress가 `Arrived`가 됨 | `cancelRoute` → close. 앱이 도착이라고 한 뒤에도 내비가 계속 안내하지 않게 한다 (U7) |
| 내비 `RouteCancelled` (사용자가 내비에서 취소)이고 progress가 Guiding | `progress.cancel()`, close |
| 내비의 `RemainingRaw`, `DestinationArrived` | 무시. 진행은 Fake가 맡는다 |

- `FakeGuidanceSource`와 기존 시연 모드 테스트는 고치지 않는다. 감싸기만 한다.
- 실차에서는 내비의 실제 ETA와 앱의 시연용 남은 시간이 다르게 보인다. 그래서 화면 안내 문구로 밝힌다 (설계 결정 6). 맞추는 일은 있으면 좋음 B다.

### 5. 데이터 모드: 내비 연동 범위는 별도 순수 함수로 (`[사용자 승인 필요]` U5)

```kotlin
// di/NaviPlan.kt (Android를 모른다)
enum class NaviUse {
    /** NaviHelper를 쓰지 않는다 */
    NONE,
    /** 경로 요청·취소만 내비로 보낸다. 남은 시간과 도착은 Fake 진행 (시연) */
    ROUTE_ONLY,
    /** 길안내 전부를 NaviHelper로 한다 */
    FULL,
}
fun naviUseOf(mode: DataMode, isAutomotive: Boolean): NaviUse
```

권장안(3a) 표:

| 모드 | 차량 기기 | `SourcePlan.guidance` (바꾸지 않음) | `NaviUse` | 끼우는 구현 | 화면 안내 (결정 6) |
|---|---|---|---|---|---|
| Fake | 상관없음 | FAKE | NONE | `FakeGuidanceSource` | 시연용 (지금 문구) |
| 시연 | 예 | FAKE | **ROUTE_ONLY** | `NaviRouteOnlyGuidanceSource(Fake(주행 중에만), client)` | **경로만 내비** (새 문구) |
| 시연 | 아니오 | FAKE | NONE | `FakeGuidanceSource` (+ 시나리오, 지금 그대로) | 시연용 (지금 문구) |
| Real | 예 | REAL | FULL | `NaviGuidanceSource(client)` | 없음 |
| Real | 아니오 | REAL | NONE | `RealGuidanceSource` (자리표시, 지금 그대로) | 없음 |

- **`SourcePlan`에 칸을 더하지 않고 함수를 따로 두는 이유**:
  - `SourcePlan`에 생성자 인자를 더하면 data class 동등성이 바뀐다. 그러면 007 테스트(`SourcePlanBoundaryTest`의 `구성표는 모든 모드와 기기 조합에서 계획의 표와 같다` 등)가 깨진다.
  - 또 "진행을 누가 하나(Fake/Real)"와 "내비에 무엇을 보내나"는 다른 질문이다.
  - 그래서 3a·3c에서는 기존 테스트를 하나도 고치지 않는다.
- **`RealGuidanceSource`(자리표시)를 지우지 않는다.**
  - 차량 기기가 아닌 곳의 Real 모드에서 쓴다. 지금 동작과 같다.
  - 그 테스트(`RealGuidanceSourceTest`, `RealGuidanceSourceBoundaryTest`)도 그대로 둔다. KDoc만 고친다.
- `AppContainer`:
  - `naviUse != NONE`일 때만 `naviClientOrNull(context)`를 부른다.
  - 그 함수는 `FEATURE_AUTOMOTIVE`가 없거나 클래스 로딩이 실패(`LinkageError`)하면 null을 준다 (`uxRestrictionsClientOrNull`과 같은 방식).
  - client가 null이면 ROUTE_ONLY는 그냥 Fake로, FULL은 `RealGuidanceSource`로 떨어진다.
  - 실제로 무엇을 끼웠는지 `val sendsRouteToNavi: Boolean`으로 연다.
- **일반 폰에서 죽지 않게**: 일반 폰에서는 NaviHelper 클래스를 만들지 않는다 (F3에서 로그 0줄로 확인).
- 선택지 (U5):
  - **3a (권장)**: 위 표.
  - **3b**: 시연 모드 차량 기기에서도 전부 내비(FULL)를 쓴다. 갈래 A이고, PT에서 쓸 수 있는 간단한 도착 방법이 있을 때만 가능하다.
    - `SourcePlan`의 시연·차량 기기 행이 `guidance = REAL`, `guidanceWaitsForDriving = false`, 시연용 표시 없음으로 바뀐다.
    - 그래서 007 테스트를 고쳐야 한다 (설계 결정 7).
  - **3c**: 시연 모드는 지금 그대로(전부 Fake) 둔다. NaviHelper는 debug의 Real 모드에서만 쓴다. 제출 APK에는 NaviHelper가 쓰이지 않는다.

### 6. 화면 안내 문구 (`[사용자 승인 필요]` U6)

- UI 인자 `isGuidanceSimulated: Boolean`을 `guidanceNotice: GuidanceNotice`로 바꾼다.
  - `GuidanceNotice`는 `ui/guidance`의 enum이다: `NONE`, `SIMULATED`, `ROUTE_ONLY`.
  - 바뀌는 곳: `HomeScreen`, `MedicalCardScreen`
  - `MainActivity`가 `container.sourcePlan`과 `container.sendsRouteToNavi`로 계산한다. ui는 di를 import하지 않는다.
- `ROUTE_ONLY` 문구 기본안 (`strings.xml` `guidance_route_only_notice`): **"경로는 차량 내비게이션으로 안내해요. 앱의 남은 시간과 도착은 시연용이에요."**
- `SIMULATED`는 지금 문구 그대로다.
- **주행 화면(`DrivingScreen`)은 바꾸지 않는다** (AGENTS 규칙 3). 문구는 지금처럼 정차 카드에만 보인다.
- debug 패널 라벨: 시연 모드에서 경로를 내비로 보내면 "DEBUG · 시연 (주행 Real · 내비 경로)"로 한다. 스크린샷만 보고도 구성을 알 수 있게 하려는 것이다.

### 7. 기존 코드·테스트를 지키는 범위

- 고치지 않는 것: 완료 기준 3의 경로들. `GuidanceViewModel`의 분 올림 규칙(`toDisplayMinutes`)을 그대로 쓴다.
- 주석(KDoc)만 고치는 것: `domain/guidance/GuidanceSource.kt`, `GuidanceState.kt`, `data/guidance/RealGuidanceSource.kt`, `di/DataMode.kt`
- **3b를 고르면 고쳐야 하는 기존 테스트** (예상. 단계 5 전에 `testDebugUnitTest`로 실제 실패 목록을 확인한다):
  - `SourcePlanTest`:
    - `` `시연 모드는 차량 기기에서 Real 주행 상태와 Fake 길안내를 쓰고 시나리오를 돌리지 않는다` ``
    - `` `시연 모드는 주행 중일 때만 길안내 시간이 흐른다` ``
    - `` `Fake 길안내를 쓰면 시연용 표시를 한다` ``
  - `SourcePlanBoundaryTest`:
    - `` `구성표는 모든 모드와 기기 조합에서 계획의 표와 같다` ``
    - `` `시연 모드는 기기 종류에 따라 주행 상태 출처와 시나리오만 다르다` ``
    - `` `release 빌드는 어느 기기에서도 시연용 길안내를 표시하고 Vehicle 연결을 만들지 않는다` ``
  - 고칠 때는 결정 번호를 적은 `test(di):` 커밋 하나로 따로 낸다.

### 8. 실패 처리

| 상황 | 전부 내비 (Real 모드) | 경로만 내비 (시연 모드) |
|---|---|---|
| 일반 폰 | client를 만들지 않는다 → 자리표시 | client를 만들지 않는다 → Fake만 |
| 연결 실패, 연결 시간 초과 | Idle (카드가 "생성 완료"로 돌아간다). 로그 | Fake 진행은 계속한다. 로그 |
| `SecurityException` (권한) | 어댑터가 잡아 `ConnectionFailed`/`Error`로 보낸다. `Log.e`. **에뮬레이터에서 보이면 멈추고 사용자에게 보고한다** (U1) | 같다 |
| 내비 `onError` | 결정 3의 표 | 로그만 |
| 어댑터 호출에서 난 예외 | 잡아서 이벤트로 바꾼다. 앱을 죽이지 않는다 | 같다 |

- 실패를 화면에 알리는 문구는 있으면 좋음 A다. 필수에서는 Real 모드(debug)에서만 조용히 Idle로 돌아간다.

## 확인 필요 (시작 전에 해결)

### [선행: pleos-docs-researcher] (단계 1 전에)

sdk-notes에 없는 것들이다. 문서에 없으면 Pleos Maven의 `NaviHelper:3.0.5`와 `navi-data:3.0.4` AAR을 `javap`과 AAR 매니페스트로 확인한다. 출처는 `[aar-navi-3.0.5]`로 적는다 (Vehicle 2.3.2 때와 같은 방식). 그래도 모르는 것은 spike 관찰 항목(O)으로 넘긴다.

- R1. `NaviHelper.initialize(onConnected, onFailed)`의 정확한 시그니처는? (인자 타입, 반환값, 인자 없는 `initialize()`와의 차이) `NaviHelper`의 전체 클래스 이름은?
- R2. `NaviHelperEventListener`는 26개 메서드를 모두 구현해야 하는 interface인가, 기본 구현이 있는가? 콜백은 어느 스레드로 오는가?
- R3. `RouteInfo`의 Kotlin 생성자: `poiId`, `poiSubId`, `address`, `displayLatitude`/`displayLongitude`, `RouteOption`이 nullable인가? 기본값이 있는가?
- R4. `NaviHelper:3.0.5` AAR이 병합 매니페스트에 넣는 것은?
  - `uses-permission`, `<queries>`, 최소 서비스 버전 meta-data, `minSdkVersion`
  - **특히 `pleos.car.permission.*`을 스스로 넣는지, minSdk가 28보다 큰지** (공식 navi 샘플은 minSdk 33이다)
- R5. `pleos.car.permission.NAVI_ROUTE`와 `NAVI_ROUTE_SEARCH`의 protectionLevel은? (normal이면 선언만 하면 된다. dangerous면 런타임 요청이 필요하다)
- R6. 이벤트를 받는 데(`onTripInfo`, `onDrivingInfo`, `onDestinationArrived`, `onRouteCancelled`) 권한이 필요한가? 어느 권한인가? (`@RequiresPermission`)
- R7. `RouteStartInfo`의 필드, `NaviErrorCode`와 `NavigationStatus`의 값은?
- R8. `onDestinationArrived`는 어떤 조건에서 오는가? 도착하면 경로가 저절로 끝나는가? `onRoutingCompleted`는 무슨 뜻인가?

### [사용자 승인 필요]

**새 `pleos.car.permission.*`은 계획에 없다.** spike와 구현 모두 팀이 승인한 `NAVI_ROUTE_SEARCH`와 `NAVI_ROUTE`만 선언한다 (architecture.md "사용하기로 한 Pleos 권한").

**생기면 그 자리에서 멈추고 묻는다** (R·O 결과에 따라):

- U1. 다른 권한이 필요하다고 나오는 경우. 예: `NAVI_CUSTOM_ROUTE`, `NAVI_CUSTOM_ETC`. 근거는 `SecurityException` 메시지, AAR 매니페스트, `@RequiresPermission`이다. 선언하지 않고 멈춘다.
- U2. AAR의 minSdk가 28보다 커서 빌드가 깨지는 경우. 선택지:
  - minSdk를 올린다. AGENTS.md "기술 스택"이 바뀐다. 두 에뮬레이터는 API 34라서 동작에는 영향이 없다.
  - 이번에는 하지 않는다 (갈래 C).
  - `tools:overrideLibrary`는 권장하지 않는다.
- U3. 권한이 dangerous라서 런타임 권한 요청 화면이 필요한 경우. 범위가 늘어난다 (약 0.3일). 이번에 할지 묻는다.
- U4. `poiId`가 없으면 경로가 안 되는 경우. 선택지:
  - 문서 예시 POI(`36khp37hh8gqcm5nqhe5`, 42dot, 샘플 병원과 같은 좌표)를 시연에 쓴다. 이때 내비 화면에 실제 장소 이름이 보일 수 있다. `Destination`에 `poiId: String? = null`을 더한다 (005 결정 1).
  - 이번에는 하지 않는다 (갈래 C).

**spike 뒤, 단계 2 전에 한 번에 묻는다** (spike 결과 요약과 함께):

- U5. **시연 모드(= release)에서 차량 기기의 길안내를 어떻게 할까?** (설계 결정 5)
  - **3a. 경로만 진짜 내비, 남은 시간·도착은 시연용 (권장).** 갈래 A·B 모두 가능하다.
    - 에뮬레이터에서도 흐름이 끝까지 돈다.
    - 내비가 연결되지 않아도 Fake로 계속 돈다.
    - 기존 테스트를 고치지 않는다.
  - 3b. 전부 진짜 내비. 갈래 A이고 PT용 도착 방법이 간단할 때만 가능하다. 007 테스트 6개를 고쳐야 한다 (설계 결정 7).
  - 3c. 지금처럼 전부 시연용. 제출 APK에는 NaviHelper가 쓰이지 않는다. debug Real 모드에서만 쓴다.
  - 일반 폰은 어느 경우든 지금처럼 Fake + 시나리오다.
- U6. 3a의 화면 문구: "경로는 차량 내비게이션으로 안내해요. 앱의 남은 시간과 도착은 시연용이에요."로 괜찮은가?
- U7. 3a에서 앱이 도착(시연)하면 내비 경로를 취소할까? 기본안은 **취소한다**. 그대로 두면 앱은 "도착했어요"인데 내비는 계속 안내한다.
- U8. 갈래 C일 때: Real 길안내를 이번에 접고 주최측 문의 B5를 추가할까? 예: "Pleos 에뮬레이터 Navigation 앱이 NaviHelper `requestRoute`를 받는가? 받으려면 무엇이 필요한가?"

### 기획상 판단 (사용자·기획 담당)

- K1. 시연 영상·PT 장면: 내비 화면의 경로를 보여 줄지, 어떻게 앱 사이를 오갈지 정한다. spike O4의 "어느 앱이 앞에 나오는가"에 달렸다.
- K2. 기획서 5번 "개발 계획" 문장. 단계 9 노트에 갈래·U5 결과에 맞는 초안을 남기고 기획 담당에게 넘긴다. 확인하지 않은 기능을 된다고 쓰지 않는다 (출품 유의사항).
- K3. 3a에서는 내비의 ETA와 앱의 시연용 남은 시간이 다르게 보인다. 영상에 둘이 함께 나오면 있으면 좋음 B를 할지 정한다.

### spike 관찰 목록 (단계 1에서 실행해서 확인한다. 문서에 없음)

| # | 볼 것 | 방법 | 결과가 바꾸는 것 |
|---|---|---|---|
| O0 | 의존성 해결과 전이 의존성 충돌. 병합 매니페스트에 AAR이 넣는 것 | `.\gradlew.bat :app:dependencies --configuration debugRuntimeClasspath`, 병합 매니페스트 Grep | U1·U2 |
| O1 | `initialize` 뒤 `onConnected`/`onFailed`가 언제 어느 스레드로 오는가 | 로그 (시각, 스레드 이름) | `connectTimeout`, 어댑터 스레드 처리 |
| O2 | 권한: 두 권한의 부여 상태와 종류. `NAVI_ROUTE_SEARCH`만 선언했을 때 이벤트가 오는가, `NAVI_ROUTE`도 선언하면 달라지는가 | `adb shell dumpsys package com.meongcare.app`의 권한 부분, 로그 | 선언할 권한 (최소 권한), U3 |
| O3 | 처음 상태: `getRouteStateInfo`(`isRouting`, `termsAgreement`, `naviMode`), `getCurrentLocationInfo`(에뮬레이터 현재 위치가 한국인가), `getDestinationInfo` | spike STATE 동작 | 약관 동의가 필요한가, 위치를 옮겨야 하는가 |
| O4 | 시연 병원 좌표 + 이름으로 `requestRoute` (`poiId`는 R3에 맞게 null 또는 빈 값). 내비 앱의 반응을 본다: 바로 안내하는가, 미리보기에서 사용자가 눌러야 하는가, 반응이 없거나 오류인가. **어느 앱이 앞에 나오는가.** 콜백 순서와 시각 | 스크린샷, 로그 | 갈래, K1. 위치가 한국 밖이라 실패하면 `adb -s emulator-5554 emu geo fix <경도> <위도>`로 병원 몇 km 앞으로 옮기고 다시 해 본다 |
| O5 | 문서 예시 `poiId`·`poiSubId "0"`을 넣었을 때 달라지는 것 (O4가 실패했을 때만 필수) | 같다 | U4 |
| O6 | 남은 시간: `TripInfo.eta`·`distance`, `DrivingInfo.destination.duration`·`distance`. 같은 순간 내비 화면의 남은 시간·도착 시각·거리와 기기 시각을 비교한다. 1분 이상 지나서 다시 본다 | 로그 + 스크린샷 | `remainingTimeOf` 규칙, 어느 값을 쓸지 |
| O7 | Virtual Console로 기어 D + 속도 30을 주면 위치·남은 시간·거리가 바뀌는가 | `vc()` (006 노트), 로그 | 갈래 A/B |
| O8 | 도착 만들기. 하나씩 해 보고 되면 멈춘다. (a) `geo fix`로 목적지 좌표로 옮긴다 (b) 목적지 근처에서 목적지로 몇 단계에 걸쳐 `geo fix` (c) 내비 앱 메뉴에 모의 주행 같은 기능이 있는지 화면에서 본다 (있다고 가정하지 않는다). `onDestinationArrived`가 오는가, 그 뒤 `isRouting`은 어떻게 되는가 | 로그, 스크린샷 | 갈래 A/B, PT 도착 방법 |
| O9 | 우리 `cancelRoute` → `onRouteCancelled`가 오는가, 내비 화면에서 경로가 사라지는가. `cancelRoute` 직후 `release`해도 취소되는가. 내비 앱 화면에서 사용자가 취소하면 `onRouteCancelled`가 우리에게 오는가 | 로그, 스크린샷 | 결정 2의 "취소 직후 해제", 결정 3·4의 `RouteCancelled` |
| O10 | 안내 중에 `requestRoute`를 다시 부르면 이전 경로의 `onRouteCancelled`가 오는가 | 로그 | 결정 3의 다시 시작 규칙 |
| O11 | (선택) 이상한 좌표(0, 0)로 요청했을 때 `onError` 코드 | 로그 | 결정 3의 `Error` 처리 |
| O12 | (선택) 일반 폰(`emulator-5556`)에서 INIT만: `onFailed`까지 걸리는 시간과 크래시 여부. 실제 앱은 생성 함수가 막아서 부르지 않는다 | 로그 | 안전망 확인 |

## 확인 절차

- 에이전트는 에뮬레이터를 직접 띄우지 않는다. 두 에뮬레이터가 켜져 있으므로 모든 adb 명령에 `-s`를 붙이거나 `ANDROID_SERIAL`을 정한다.
  - Git Bash: `export ANDROID_SERIAL=emulator-5554`
  - PowerShell: `$env:ANDROID_SERIAL='emulator-5554'`
  - `/verify`는 기기를 고르지 않으므로 이렇게 정한 뒤 쓴다.
- Pleos는 Cold boot로 켜져 있어야 한다. `vc "/vehicle/gear?gear=Park"`로 기어가 바뀌는지 먼저 본다.
- Virtual Console 명령은 006 노트의 `vc()`를 쓴다: `export MSYS_NO_PATHCONV=1`, `adb -s emulator-5554 forward tcp:8088 tcp:8088`, 요청마다 `/browser/session`을 먼저 보낸다.
- 스크린샷·UI 트리·로그는 `.claude/.cache/p10-<번호>.*`에 둔다. 커밋하지 않는다.
- 카드까지 가는 길: 처음 화면 [진료 카드 만들기] → 증상 하나 → [카드 만들기] → 카드.

### Pleos `emulator-5554`: debug Real 모드 (전부 내비)

준비: debug 패널을 [Real로 전환]까지 누른다 → "DEBUG · Real". 기어 P.

| # | 조작 | 기대 |
|---|---|---|
| N1 | 앱 실행 | 처음 화면. NaviHelper 연결 로그가 아직 없다 (안내 전에는 연결하지 않는다) |
| N2 | 카드 → [길 안내 시작] | 카드 "경로를 찾는 중" → (O6 값이 오면) "병원까지 N분". 로그: 연결 → `requestRoute` → 이벤트 |
| N3 | 내비 화면을 본다 (앞에 나온 창, 또는 GNB의 내비). 다시 우리 앱으로 | "시연용 동물병원" 경로 (캡처) |
| N4 | `gear=Drive` | 주행 화면 "병원까지 N분" + 병원 이름. UI 트리에서 debug 패널 영역을 빼면 `EditText` 0개, 클릭 가능한 노드 0개 |
| N5 | (갈래 A) O8에서 찾은 도착 방법 → `gear=Park` | 주행 "도착했어요" → 도착 카드 → [닫기] → 처음 화면. 로그에 release. 갈래 B면 "JVM으로만 확인"이라고 적는다 |
| N6 | 새로 안내 → P에서 [안내 취소] | 카드 "생성 완료". 내비 경로가 사라진다 (캡처). 로그에 `cancelRoute` → release |
| N7 | (O9에서 됐으면) 안내 중 내비 앱 화면에서 경로 취소 | 우리 카드가 안내 없음으로 돌아온다 |
| N8 | 로그 | `logcat -d -t 500 *:E`에 크래시와 `SecurityException`이 없다 |

### Pleos `emulator-5554`: debug 시연 모드 (경로만 내비, 3a일 때)

준비: [시연으로 전환] → "DEBUG · 시연 (주행 Real · 내비 경로)". 기어 P.

| # | 조작 | 기대 |
|---|---|---|
| H1 | 카드 → [길 안내 시작] | "병원까지 12분" + 새 안내 문구. 내비에 경로가 있다 (캡처) |
| H2 | P인 채로 10초 | 12분 그대로 (007 동작) |
| H3 | `gear=Drive` | 주행 화면에서 2초마다 준다. N4와 같은 UI 트리 규칙 |
| H4 | 도착까지 기다림 → `gear=Park` | 주행 "도착했어요"와 같은 무렵 로그에 `cancelRoute` → release, 내비 경로가 사라진다 (U7) → 도착 카드 + 새 문구 → [닫기] → 처음 화면 |
| H5 | 새로 안내 → P에서 [안내 취소] | 내비 경로가 사라진다 |
| H6 | (O9에서 됐으면) 안내 중 내비에서 취소 | 앱도 안내 없음 |
| H7 | 로그 | N8과 같다 |

- 다 보면 기어를 P로 돌리고, 남은 내비 경로가 있으면 취소한다. `geo fix`로 옮긴 위치는 진행 로그에 적는다.

### 일반 폰 `emulator-5556`

| # | 조작 | 기대 |
|---|---|---|
| F1 | debug Fake | 009 흐름 그대로: 카드 → 길안내 → 저절로 도착 → 도착 카드 |
| F2 | debug 시연 "(주행 Fake)" | 흐름 그대로. 지금 문구("시연용 길안내예요…") |
| F3 | debug Real | 주행 화면(모름). 크래시 없음. `logcat -d | grep -ci navi`로 우리 NaviHelper 로그 0줄 |

### release (확인용 사본)

`keystore.properties`가 있으면 그 서명을 쓴다 (내용은 읽지 않는다). 없으면 007 결정 5a처럼 debug 키로 서명한 사본을 `.claude/.cache/`에 만든다. 제출용이 아니다.

| # | 환경 | 기대 |
|---|---|---|
| R1 | 둘 다: `adb uninstall com.meongcare.app` → 설치 → 실행 | 크래시 없음. UI 트리에 "DEBUG" 0건 |
| R2 | 일반 폰 | F2 흐름이 손대지 않아도 끝까지 돈다 |
| R3 | Pleos | 3a: H1~H5. 3c: 007의 D1~D9 |

## 단계

단계 하나 = 커밋 1~3개. 앱 코드가 들어가는 커밋마다 `assembleDebug`가 통과해야 한다 (커밋 직전 훅).

**시작 전 준비:**
- 위 `[선행: pleos-docs-researcher]` R1~R8을 메인이 researcher에게 맡긴다. 결과가 sdk-notes에 반영된 뒤 단계 1을 시작한다.
- 사용자에게 알린다:
  - spike는 Pleos 에뮬레이터의 내비 앱에 시연 병원 경로를 실제로 걸고, 필요하면 `adb emu geo fix`로 에뮬레이터 위치를 바꾼다. 끝나면 경로를 취소한다.
  - U5~U8은 spike 결과를 보고 묻는다.
- `adb devices`에 `emulator-5554`, `emulator-5556`이 모두 보이는지 확인한다.

**순서 메모:** 기본 순서는 인터페이스·Fake → 도메인·UI → Real 래퍼다. 여기서는 맨 앞에 spike를 둔다. 경계와 상태 기계(단계 2·3)는 SDK 없이 JVM 테스트로 만들고, 어댑터(단계 4)는 그 뒤에 붙인다. 시연 모드를 바꾸는 커밋(5-2)은 따로 둔다. 그 커밋 하나만 되돌리면 시연 모드가 전부 Fake로 돌아간다 (007의 release 고정 커밋과 같은 방식).

### 필수

- [ ] 0. 이 계획 파일 커밋 → `docs(plans): 010 Real 길안내(NaviHelper) 계획 추가`
  - `.\gradlew.bat testDebugUnitTest`를 돌려 기존 테스트 통과 수를 진행 로그에 적는다 (완료 기준 3의 기준값).
  - 확인: `git log -1 --stat`에 이 파일만 있다.

- [ ] 1. `[선행: pleos-docs-researcher]` spike: 에뮬레이터에서 NaviHelper 확인 (0.5일 이내, **앱 코드 커밋 없음**) → `docs(pleos): Pleos 에뮬레이터 NaviHelper 시험 결과 기록`
  - 1-1. 임시 코드 (**커밋하지 않는다**):
    - `gradle/libs.versions.toml`과 `app/build.gradle.kts`에 `ai.pleos.playground:NaviHelper:3.0.5`를 넣는다.
    - `AndroidManifest.xml`에 승인된 권한을 넣는다. 처음엔 `NAVI_ROUTE_SEARCH`만 넣고, O2를 본 뒤 `NAVI_ROUTE`를 더한다.
    - `app/src/debug/java/com/meongcare/app/data/navi/spike/NaviSpikeReceiver.kt`와 `app/src/debug/AndroidManifest.xml`(exported receiver)을 만든다. debug 소스셋이라 release에는 들어가지 않는다.
      - 동작(action): `INIT`, `ROUTE`(extra: `lat`, `lon`, 선택 `poiId`·`poiSubId`), `CANCEL`, `STATE`(`getRouteStateInfo`, `getDestinationInfo`, `getCurrentLocationInfo`), `RELEASE`
      - 목적지 이름 기본값은 코드에 둔다. adb로 한글 인자를 넘기면 깨질 수 있다.
      - NaviHelper 객체는 프로세스 하나에 하나만 둔다. 앱(MainActivity)을 켜 둔 채로 시험한다.
      - 로그 태그 `NaviSpike`. 콜백마다 이름, `System.currentTimeMillis()`, 스레드 이름, 값의 `toString()`을 찍는다.
    - 명령 예 (Git Bash):

      ```bash
      export MSYS_NO_PATHCONV=1; export ANDROID_SERIAL=emulator-5554
      R=com.meongcare.app/.data.navi.spike.NaviSpikeReceiver
      adb shell am broadcast -n $R -a com.meongcare.app.spike.INIT
      adb shell am broadcast -n $R -a com.meongcare.app.spike.ROUTE --ed lat 37.48544722 --ed lon 127.03636666
      adb logcat -s NaviSpike
      ```

  - 1-2. "spike 관찰 목록"의 O0~O10을 차례로 본다. O11·O12는 시간이 남으면 한다.
    - U1~U4 조건이 보이면 그 자리에서 멈추고 묻는다.
  - 1-3. 기록:
    - `docs/pleos/sdk-notes.md` NaviHelper 절에 관찰 블록을 둔다. 형식은 기존 "전화" 관찰과 같다: `> **관찰 (날짜, Pleos 에뮬레이터 v2.1.0, NaviHelper 3.0.5, 선언한 권한, 실차 아님):**`
      - 명령, 값, 스크린샷에서 읽은 내비 화면 값을 쓴다.
      - 출처 태그는 `[obs-navi-emu]`이고, "출처" 목록에 정의를 더한다.
    - "미확인" 1047·1048·1051행을 체크하거나 관찰을 덧붙인다. R에서 AAR로 확인한 것은 `[aar-navi-3.0.5]`로 적는다.
    - 도착이나 위치를 만드는 방법을 찾았으면 `docs/pleos/emulator-setup.md` "Pleos 에뮬레이터에서 직접 해 본 것"에도 적는다. 이때 커밋을 하나 더 낸다: `docs(pleos): 에뮬레이터에서 내비 위치·도착 만드는 방법 기록`
  - 1-4. 정리:
    - 임시 파일을 `.claude/.cache/010-navi-spike/`에 복사한다 (gitignore 대상, 다시 시험할 때 쓴다).
    - 작업 트리에서 되돌린다. 의존성과 권한 줄도 되돌린다. 단계 4에서 커밋한다.
    - `git status`에 앱 코드 변경이 없는 것을 확인한 뒤 문서를 커밋한다.
  - 1-5. 갈래(A/B/C)와 O 요약을 진행 로그에 적는다. U5~U8(+ 생긴 U1~U4)을 사용자에게 한 번에 묻고 답을 진행 로그에 적는다.
  - 확인: 문서 커밋에 앱 코드가 없다 (`git show --stat`).

- [ ] 2. (갈래 A·B) 경계, 남은 시간 변환, 전부 내비 상태 기계
  - 2-1. → `feat(data): 내비 연결 경계와 남은 시간 변환 추가`
    - `data/navi/NaviClient.kt` (설계 결정 1의 블록)
    - `data/navi/NaviRemainingTime.kt`: `remainingTimeOf` (O6로 정한 규칙, KDoc에 근거와 sdk-notes 링크)
    - `NaviRemainingTimeTest`:
      - `` `0 이하는 모름이다`() ``
      - `` `하루보다 긴 값은 모름이다`() ``
      - `` `관찰한 단위대로 남은 시간으로 바꾼다`() `` (spike에서 본 실제 값 한 쌍을 예로 쓴다)
      - `` `무한대 시간을 내지 않는다`() ``
      - 도착 시각 규칙이면 `` `이미 지난 도착 시각은 모름이다`() ``도 넣는다.
    - U4로 `poiId`가 필요해졌으면 그 전에 따로 커밋한다: `feat(domain): 길안내 목적지에 POI 식별자 추가` (`Destination`에 `poiId: String? = null`, `SampleHospitals`에 값)
  - 2-2. → `feat(data): 내비 이벤트를 길안내 상태로 바꾸는 소스 추가`
    - `data/guidance/NaviGuidanceSource.kt` (설계 결정 2·3). 생성자: `client`, `connectTimeout`, `log`, 필요하면 `now`.
    - `NaviGuidanceSourceTest` (`runTest`, 가상 시간, 테스트 파일 안의 기록용 가짜 client):
      - `` `처음에는 안내 중이 아니고 내비에 연결하지 않는다`() ``
      - `` `안내를 시작하면 바로 남은 시간 모르는 안내 중이 되고 연결된 뒤에 경로를 요청한다`() ``
      - `` `연결되기 전에는 경로를 요청하지 않는다`() ``
      - `` `연결에 실패하면 안내 없음으로 돌아가고 연결을 닫는다`() ``
      - `` `연결을 기다리다 시간이 지나면 안내 없음으로 돌아간다`() ``
      - `` `남은 시간 값을 받으면 남은 시간을 보여준다`() ``
      - `` `알 수 없는 남은 시간 값이 오면 이전 값을 그대로 둔다`() ``
      - `` `도착 이벤트를 받으면 도착 상태가 되고 연결을 닫는다`() ``
      - `` `내비에서 경로를 취소하면 안내 없음으로 돌아가고 연결을 닫는다`() ``
      - `` `안내를 취소하면 경로 취소를 요청하고 연결을 닫는다`() ``
      - `` `연결을 기다리는 중에 취소하면 경로를 요청하지 않는다`() ``
      - `` `안내 중에 다른 목적지로 시작하면 같은 연결로 새 경로를 요청한다`() ``
      - `` `안내 중이 아닐 때 온 내비 이벤트는 무시한다`() ``
      - `` `도착 화면을 닫으면 안내 없음이 되고 내비를 부르지 않는다`() ``
      - `` `연결을 닫은 뒤에 온 이벤트는 상태를 바꾸지 않는다`() ``
    - 갈래 B인데 내비 이벤트가 하나도 오지 않았으면 2-2를 건너뛰고 진행 로그에 적는다.
  - 확인: 두 테스트 클래스 통과, `assembleDebug`.

- [ ] 3. (U5 = 3a, 갈래 A·B) 경로만 내비 → `feat(data): 경로만 내비로 보내는 시연 길안내 추가`
  - `data/guidance/NaviRouteOnlyGuidanceSource.kt` (설계 결정 4)
  - `NaviRouteOnlyGuidanceSourceTest` (progress는 짧은 timing의 `FakeGuidanceSource`):
    - `` `상태는 진행 소스를 그대로 따른다`() ``
    - `` `안내를 시작하면 진행을 먼저 시작하고 연결된 뒤 경로를 내비로 보낸다`() ``
    - `` `내비 연결에 실패해도 진행은 계속된다`() ``
    - `` `진행이 도착하면 내비 경로를 취소하고 연결을 닫는다`() ``
    - `` `안내를 취소하면 진행과 내비 경로를 함께 취소한다`() ``
    - `` `내비에서 경로를 취소하면 진행도 취소한다`() ``
    - `` `내비의 남은 시간과 도착 이벤트는 진행을 바꾸지 않는다`() ``
    - `` `도착 화면 닫기는 진행에만 전한다`() ``
  - 3b·3c면 건너뛴다.
  - 확인: 테스트 통과. 기존 `FakeGuidanceSource*Test`가 그대로 통과한다.

- [ ] 4. `[선행: pleos-docs-researcher]` Pleos 어댑터와 Real 모드 연결
  - 4-1. → `feat(data): Pleos NaviHelper 어댑터와 경로 권한 추가`
    - 의존성: `libs.versions.toml`에 `naviHelper = "3.0.5"`와 라이브러리 별칭, `build.gradle.kts`에 `implementation`. git.md 규칙대로 처음 쓰는 이 커밋에 넣는다.
    - `AndroidManifest.xml`에 O2로 정한 승인 권한(`NAVI_ROUTE_SEARCH`, 필요하면 `NAVI_ROUTE`)을 넣는다. 주석에 "2026-11-03 팀 승인, architecture.md"를 적는다.
      - 하나만 필요했으면 완료 기준 4의 "정확히 2건"을 1건으로 고친다.
    - `data/navi/PleosNaviClient.kt`:
      - R1~R3대로 생성·초기화·리스너·해제를 한다. 26개 콜백 중 쓰는 것만 이벤트로 바꾸고, 나머지는 비워 둔다.
      - `Destination` → `RouteInfo`: sdk-notes 원문 코드의 이름 붙은 인자를 쓴다.
      - 모든 SDK 호출을 try로 감싼다 (설계 결정 8).
      - 해제 뒤에 온 콜백은 버린다.
    - `data/navi/NaviClientFactory.kt`: `naviClientOrNull(context)`. `FEATURE_AUTOMOTIVE`가 없으면 null이고 로그를 남긴다. `LinkageError`도 null.
    - 확인:
      - `assembleDebug`
      - 완료 기준 4의 병합 매니페스트 Grep: `pleos.car.permission`이 승인된 것만 있다. 아니면 멈추고 U1.
  - 4-2. → `feat(di): Real 모드 차량 기기에서 NaviHelper 길안내 연결`
    - `di/NaviPlan.kt`: `NaviUse`, `naviUseOf`. 이 커밋에서는 시연 모드가 어느 기기에서나 `NONE`이다.
    - `NaviPlanTest`. 시연·차량 기기 행 테스트는 5-2에서 더한다. 3c면 여기서 `` `시연 모드는 어느 기기에서도 내비를 쓰지 않는다`() ``를 넣는다.
      - `` `Fake 모드는 내비를 쓰지 않는다`() ``
      - `` `시연 모드 일반 폰은 내비를 쓰지 않는다`() ``
      - `` `Real 모드 차량 기기는 길안내 전부를 내비로 한다`() ``
      - `` `Real 모드 일반 폰은 내비를 쓰지 않는다`() ``
    - `AppContainer`: `naviUse`, `naviClient`, `guidanceSource` 분기 (설계 결정 5), `sendsRouteToNavi`
    - KDoc만 고친다: `RealGuidanceSource`("차량 기기가 아닌 곳의 Real 모드 자리표시"), `GuidanceSource`, `GuidanceState.Guiding`의 남은 시간 설명(O6 결과), `DataMode.REAL`
    - 확인: `testDebugUnitTest`. Pleos debug Real로 N1~N4·N6 (가능하면 N5·N7), 일반 폰 F3.

- [ ] 5. (U5 = 3a) 시연 모드 차량 기기에서 경로를 내비로
  - 5-1. → `feat(ui): 경로만 내비로 보낼 때의 안내 문구 추가`
    - `ui/guidance/GuidanceNotice.kt`, `strings.xml` `guidance_route_only_notice` (U6 문구)
    - `SimulatedGuidanceNotice`가 종류를 받는다. `HomeScreen`·`MedicalCardScreen` 인자를 `guidanceNotice`로 바꾸고 미리보기를 고친다.
    - `MainActivity`가 `GuidanceNotice`를 계산한다. 이 커밋 시점에는 `ROUTE_ONLY`가 아직 나오지 않으므로 화면은 그대로다.
    - 확인: `assembleDebug`. 기기가 있으면 일반 폰 F2에서 문구가 그대로인지 본다.
  - 5-2. → `feat(di): 시연 모드 차량 기기에서 경로를 내비로 전송`
    - `naviUseOf(DEMO, isAutomotive = true)`를 `ROUTE_ONLY`로 바꾼다.
    - `NaviPlanTest`에 더한다:
      - `` `시연 모드 차량 기기는 경로만 내비로 보낸다`() ``
      - `` `release 빌드는 차량 기기에서 경로만 내비로 보내고 일반 폰에서는 내비를 쓰지 않는다`() `` (`resolveDataMode`와 함께)
    - `AppContainer`: ROUTE_ONLY면 시연용 `FakeGuidanceSource(progressGate)`를 `NaviRouteOnlyGuidanceSource`로 감싼다.
    - `MainActivity`: 안내 종류 `ROUTE_ONLY`, debug 라벨 "(주행 Real · 내비 경로)"
    - KDoc: `DataMode.DEMO`, `resolveDataMode`("Real 길안내가 생기면 다시 정한다" → 이번 결정)
    - 커밋 본문: "U5 3a. 이 커밋만 되돌리면 시연 모드가 전부 Fake로 돌아간다."
    - 확인: `testDebugUnitTest`. Pleos debug 시연 H1~H5, 일반 폰 F2.
  - **3b를 골랐으면** 5-2 대신:
    - `sourcePlanOf`의 시연·차량 기기 행을 `guidance = REAL`, `guidanceWaitsForDriving = false`로 바꾼다. `naviUseOf`는 FULL이다.
    - 설계 결정 7의 기존 테스트를 고쳐 따로 커밋한다: `test(di): 시연 모드 차량 기기를 내비 길안내로 바꾼 결정 반영`
    - 5-1의 `ROUTE_ONLY` 문구는 만들지 않는다.
  - **3c면** 단계 5를 건너뛴다.

- [ ] 6. 경계 테스트 (`tester`) → `test: 내비 길안내 매핑과 경로만 내비 시연 경계 테스트 추가`
  - 메인이 `tester`에게 **이 계획 파일 경로와 대상만** 넘긴다. 구현 설명은 넘기지 않는다.
    - 대상: `remainingTimeOf`, `NaviGuidanceSource`, `NaviRouteOnlyGuidanceSource`, `naviUseOf`
    - 대상 아님: `PleosNaviClient`, `NaviClientFactory`, `AppContainer`, `MainActivity`, Compose 화면 (SDK·Context·Compose가 필요하다)
  - 기대 동작 (계약 기준):
    - 설계 결정 2의 연결 규칙: 안내 한 번에 연결 한 번, 연결 전에는 요청하지 않음, 끝나면 반드시 닫음, 닫은 뒤 이벤트 무시
    - 설계 결정 3·4의 표 전체
    - 설계 결정 5의 표: 모드 × 기기. release는 `resolveDataMode`와 함께 본다.
    - `GuidanceSource` 구독 계약: 구독하면 현재 상태를 먼저, 같은 값은 연달아 오지 않는다.
    - `remainingTimeOf`가 `Duration.INFINITE`나 음수를 내지 않는다.
  - tester가 찾은 버그는 메인이 `fix:` 커밋으로 따로 고친다.
  - 확인: `.\gradlew.bat testDebugUnitTest` 통과. 통과 수와 실패 수를 진행 로그에 적는다.

- [ ] 7. 전체 검증 (`/verify`, 두 에뮬레이터). 커밋은 없다. 고친 게 있으면 `fix(...)` 커밋으로 낸다.
  - `node .claude/hooks/build-gate.mjs pipeline` → "검증 파이프라인 통과."
  - `.\gradlew.bat assembleRelease` 성공
  - 완료 기준 3의 `git diff`와 완료 기준 4의 Grep을 모두 돌린다.
  - `emulator-5554`: N1~N8, (3a) H1~H7
  - `emulator-5556`: F1~F3
  - release 사본: R1~R3
  - 새로 알게 된 Pleos 사실이 있으면 sdk-notes에 적는다. 단계 8에서 커밋한다.
  - 기기가 꺼져 있으면 멈추고 사용자에게 Cold boot를 요청한다. 확인하지 못한 것은 그대로 적는다.

- [ ] 8. 문서 맞추기
  - `docs/architecture.md`:
    - "서비스 기능 스택"의 길안내, 도착 예정 시간, 도착 감지 행: 상태 칸에 확인한 것과 못 한 것을 적는다. 예: "`poiId` 없이 좌표로 요청됨 (에뮬레이터 관찰)", "단위: …"
    - "데이터 모드": 설계 결정 5의 표(내비 연동). release 문단의 "Real 길안내가 생기면 다시 정한다"를 U5 결정으로 바꾼다.
    - 새 절 "NaviHelper 수명주기": 설계 결정 2와 8
    - "패키지 구조": `data/navi/`, `data/guidance/Navi*`, `di/NaviPlan.kt`, `ui/guidance/GuidanceNotice`
    - 갈래 C면 이 파일에서는 문의 B5(U8)와 서비스 기능 스택 상태만 고친다.
  - `AGENTS.md` (지도라서 한두 줄만):
    - 아키텍처 규칙에 "NaviHelper의 `initialize()`/`release()`는 `data/navi/PleosNaviClient` 한 곳에서만 한다. 길안내 한 번마다 새로 만들고 끝나면 release한다. 자세히: architecture.md"를 더한다.
    - "release는 시연 모드다 … 길안내는 시연용 Fake …" 문구를 U5에 맞게 고친다.
  - `README.md`:
    - "실행 방법"의 안내 블록: 3a면 "차량 기기에서는 [길 안내 시작]이 Pleos NaviHelper로 차량 내비에 병원 경로를 보낸다. 앱의 남은 시간과 도착은 시연용이다 (주행 중에만 줄어든다). 일반 폰 에뮬레이터는 전부 시연용."
    - "Pleos 에뮬레이터에서 시연하기"에 내비 화면 확인 단계를 넣는다. O3·O4에서 위치를 옮겨야 했으면 그 명령도 넣는다.
  - 커밋:
    - `docs(architecture): NaviHelper 길안내 구조와 내비 연동 표 기록`
    - `docs: README와 AGENTS.md에 내비 경로 시연 반영`
    - 단계 7에서 새 사실이 있었으면 `docs(pleos): …`
  - 확인: 완료 기준 7

- [ ] 9. 개발 노트 작성 (`docs/notes/`)
  - `docs/notes/xxj15/YYYY-MM-DD-real-navi-guidance.md`. 형식은 `docs/notes/README.md` 템플릿을 따른다.
    - 사용한 Pleos SDK와 권한: NaviHelper 3.0.5 (`requestRoute`, `cancelRoute`, 쓴 콜백들), 선언한 권한
    - 결정: 층 나누기, 안내 한 번에 NaviHelper 하나, 갈래 결과, U5~U7 답, `SourcePlan`을 고치지 않은 이유
    - 시연 방법 (영상·PT용): Pleos 에뮬레이터 순서(H 또는 N), 내비 화면 보는 법(K1), 위치·도착 명령, 일반 폰 순서
    - **기획서 5번 "개발 계획"에 넣을 문장** (K2, 기획 담당에게 넘긴다). 확인한 것만 쓴다.
    - 남은 일: 있으면 좋음 중 안 한 것, 실차 확인, 문의 B5(있으면)
  - 이 계획을 `docs/plans/completed/`로 옮기고 진행 로그를 갱신한다.
  - 커밋:
    - `docs(notes): real-navi-guidance`
    - `docs(plans): 010 Real 길안내 완료`
  - 확인: `git status`에 이 작업의 변경이 남지 않았다 (spike 임시 파일 포함). 그다음 `/ship`.

### 있으면 좋음 (필수가 끝난 뒤, 단계 9 전에)

- [ ] A. 내비 연결 실패 안내 (Real 모드) → `feat(ui): 내비 연결 실패 안내 추가`
  - 연결 실패나 시간 초과 뒤 카드에 "내비게이션에 연결하지 못했어요" 한 줄을 보인다.
  - 도메인 `GuidanceState`는 바꾸지 않는다. `AppContainer`가 data 쪽의 실패 Flow를 따로 열고, `MainActivity`가 넘긴다.
  - 확인: JVM 테스트, Pleos에서 내비 연결을 막을 방법이 없으면 미리보기로만 본다고 적는다.
- [ ] B. 경로만 내비에서 처음 남은 시간을 내비의 첫 ETA로 맞추기 (K3) → `feat(data): 시연 길안내 시작 시간을 내비 ETA로 맞추기`
  - `FakeGuidanceSource`에 기본값 있는 시작 값 인자를 더한다. 기존 테스트는 그대로 통과해야 한다.
  - 완료 기준 3의 `FakeGuidanceSource.kt` diff 항목을 이 커밋만큼 예외로 적는다.
- [ ] C. 앱이 다시 켜질 때 진행 중인 경로 이어받기 → `feat(data): 진행 중인 내비 경로 이어받기`
  - `getRouteStateInfo().isRouting`과 `getDestinationInfo`를 쓴다. 권한 `NAVI_ROUTE`는 승인된 것이다.
  - 우리가 시작하지 않은 경로를 어떻게 보여 줄지는 009 결정 1의 "안내 중 + 세션 없음 → 카드(고른 증상 없음)"를 따른다.
- [ ] D. (갈래 A) 도착 재현을 영상 촬영용 스크립트로 노트에 정리한다 (`geo fix` 순서와 간격).

## 진행 로그

- 2026-11-05: 계획 작성.
  - 브랜치 `feat/real-navi-guidance`는 dev(`4fedbca`, 009와 카드 홈 버튼 반영 뒤)에서 땄다.
  - 시작 시점의 코드:
    - `GuidanceSource` 구현은 둘이다: `FakeGuidanceSource`(scope·timing·`progressGate`), `RealGuidanceSource`(항상 Idle 자리표시).
    - 진료 카드 [길 안내 시작] → `GuidanceViewModel.onStartGuidance()` → `GuidanceSource.start(SampleHospitals.demo.destination)`
    - 데이터 모드는 `sourcePlanOf`로 정한다. release는 시연 모드이고 길안내는 어느 기기에서나 Fake다 (007).
    - `NaviHelper` 의존성과 `pleos.car.permission` 선언은 없다.
    - 에뮬레이터 두 대가 켜져 있다: Pleos `emulator-5554`, 일반 폰 `emulator-5556`.

## 결정 / 배운 점
