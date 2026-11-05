# 011 현재 위치 (Pleos NaviHelper `getCurrentLocationInfo`)

- 목표: 차 앱이 필요할 때 Pleos NaviHelper `getCurrentLocationInfo()`로 현재 위치(위도·경도·주소)를 한 번 물어 받고, debug 패널에서 그 결과를 확인할 수 있게 한다.
- 완료 기준 (어떻게 검증하나). `$BASE`는 rebase 전에는 `e57a61f`, rebase 뒤에는 `origin/dev`다 (아래 "dev 반영 순서").
  1. `node .claude/hooks/build-gate.mjs pipeline`이 "검증 파이프라인 통과."를 출력한다 (최신 dev 위로 rebase한 뒤).
  2. `.\gradlew.bat assembleRelease`가 성공한다.
  3. 단위 테스트:
     - 새 테스트가 통과한다: `FakeCurrentLocationSourceTest`, `NaviCurrentLocationSourceTest`, `DebugLocationViewModelTest`, `DebugLocationTextTest`, `LocationPlanTest`, 그리고 tester가 추가한 테스트.
     - 기존 테스트가 모두 그대로 통과한다. 개수는 단계 0에서 센 값이다.
     - `git diff $BASE --diff-filter=MD --name-only -- app/src/test`의 출력이 비어 있다. 기존 테스트는 고치거나 지우지 않는다.
  4. 바뀌지 않아야 하는 것 (`git diff $BASE -- <경로>`의 출력이 비어 있다):
     - `app/src/main/AndroidManifest.xml`, `gradle/libs.versions.toml`, `app/build.gradle.kts`: 새 권한·의존성 없음
     - `app/src/main/java/com/meongcare/app/data/guidance/navi/NaviClient.kt`
     - `app/src/main/java/com/meongcare/app/data/guidance/` 아래 `NaviGuidanceSource.kt`, `NaviRouteOnlyGuidanceSource.kt`, `FakeGuidanceSource.kt`, `RealGuidanceSource.kt`
     - `app/src/main/java/com/meongcare/app/di/` 아래 `NaviPlan.kt`, `SourcePlan.kt`, `DataMode.kt`
     - `app/src/main/java/com/meongcare/app/ui/home/`, `ui/guidance/`, `ui/card/`, `domain/guidance/`
     - `AGENTS.md`
     - `PleosNaviClient.kt`는 더하기만 한다: `git diff $BASE -U0 -- app/src/main/java/com/meongcare/app/data/guidance/navi/PleosNaviClient.kt`의 `-` 줄은 클래스 선언 1줄과 KDoc 줄뿐이다 (`trip` 본문은 그대로).
  5. 정적 검사 (Grep, `app/src/main` 기준):
     - `import ai.pleos.playground.navi`, `NaviHelper(`, `getCurrentLocationInfo`는 `data/guidance/navi/PleosNaviClient.kt`에만 있다.
     - `data/` 밖에서 `import ai.pleos|import android.car`는 0건이다.
     - `ui/` 아래에서 `import com.meongcare.app.data`, `import com.meongcare.app.di`는 0건이다.
     - `domain/location/`, `data/location/`, `data/guidance/navi/NaviLocationClient.kt`에 `import android.`이 0건이다 (JVM 테스트 가능).
     - 병합 매니페스트(`app/build/intermediates/` 아래 debug `AndroidManifest.xml`)에서 `android:name="pleos.car.permission`이 정확히 2건(`NAVI_ROUTE`, `NAVI_ROUTE_SEARCH`)이다.
  6. Pleos 에뮬레이터 (`emulator-5554`, Cold boot): 아래 "확인 절차"의 L1~L7.
  7. 일반 폰 에뮬레이터 (`emulator-5556`): P1~P4.
  8. 문서:
     - `docs/pleos/sdk-notes.md` "AAR에서 직접 확인한 것" `[aar-navi]` 절에 `getCurrentLocationInfo()`, `onCurrentLocationInfo`/`onError`, `CurrentLocationInfo` 생성자·getter가 있다.
     - 같은 파일의 "현재 위치 … 필드 정의는 미확인" 줄(지금 778행)과 "위치를 얻는 다른 방법"의 "`CurrentLocationInfo` 필드는 미확인"(지금 824행)이 확인된 내용으로 바뀌어 있다.
     - 단계 6의 관찰(응답 시간, 안내 중 동시 연결, 주소 값)이 출처 태그와 함께 있다.
     - `docs/architecture.md`의 "서비스 기능 스택" 현재 위치 행, "데이터 모드"의 현재 위치 표, "패키지 구조"가 갱신돼 있다.
- 브랜치: `feat/current-location` (010 `feat/real-navi-guidance`의 `e57a61f`에서 땄다. 010이 아직 dev에 없다)
- 관련 문서:
  - AGENTS.md: 규칙 1·2·3·7·8, 아키텍처 규칙 (SDK는 `data/` 래퍼에서만, 래퍼마다 Fake, 콜백은 suspend/Flow로)
  - docs/plans/active/010-real-navi-guidance.md: 설계 결정 1(층 나누기)·2(안내 한 번에 NaviHelper 하나), 완료 기준 4(정적 검사), "확인 절차"의 에뮬레이터 규칙
    - 010 계획의 글에는 `data/navi/`라고 되어 있지만 **실제 코드는 `data/guidance/navi/`**다. 이 계획은 실제 경로를 쓴다.
  - docs/pleos/sdk-notes.md:
    - NaviHelper 절 "권한별 API"(`NAVI_ROUTE`: `get*Info()`, 약 477행), 콜백 시그니처(685행 `onCurrentLocationInfo`)
    - "산출물로 확인"(769~778행), "AAR에서 직접 확인한 것" `[aar-navi]`(780~786행), "Pleos 에뮬레이터에서 관찰한 것" `[obs-navi-emu]`(788~799행, 793행에 현재 위치 값, 797행에 "geo fix로도 안 바뀜")
    - "위치를 얻는 다른 방법"(819~824행), "미확인"(1194행~)
  - docs/architecture.md: "서비스 기능 스택" 36행(현재 위치 결정), "사용하기로 한 Pleos 권한", "패키지 구조", "의존 방향", "데이터 모드"
  - 코드: `data/guidance/navi/NaviClient.kt`, `PleosNaviClient.kt`, `di/AppContainer.kt`, `di/NaviPlan.kt`, `di/SourcePlan.kt`, `ui/debug/DebugPanel.kt`, `MainActivity.kt`, `ui/home/HomeUiState.kt`
  - docs/conventions/git.md, .claude/skills/verify/SKILL.md

## 범위

- **필수** (약 0.5~1일, 두 에뮬레이터 확인 포함): 단계 0~8.
  - 11/15 제출물에 주는 효과:
    - **기획서 "기술 활용성"**: "현재 위치를 Pleos NaviHelper `getCurrentLocationInfo()`로 받는다"를 에뮬레이터에서 확인한 사실로 쓸 수 있다.
    - 기획의 핵심 흐름("가까운 동물병원 찾기")의 기준점이 생긴다. 병원 검색 계획이 이 인터페이스만 쓰면 된다.
  - 이 브랜치는 **010이 dev에 들어간 뒤에만** dev에 반영할 수 있다 (아래 "dev 반영 순서").
- **있으면 좋음**: A (필수가 끝난 뒤, 단계 8 전에).
- **이번 범위 밖**:
  - 병원 검색(카카오 로컬, 문의 A1)과 위치를 쓰는 사용자 화면. 지금은 debug 패널에서만 위치를 본다.
  - 위치 추적(주기적 갱신), 방향(`heading`)·경로 정보(`routeId`, `currentRouteIndex`).
  - FusedLocation SDK(콘솔 "준비 중", 새 권한 `FUSED_LOCATION` 필요), Android `LocationManager`(Pleos 허용 여부 미확인).
  - `data/guidance/navi/` → `data/navi/` 패키지 이동, `trip`과 `currentLocation`의 연결 코드 공통화. 둘 다 010 코드를 바꾸므로 010·011이 dev에 들어간 뒤 `refactor:`로 따로 한다.
  - 실차 확인.

## 설계 결정 (planner 판단과 근거)

아래 코드 블록은 기본안이다. 이름과 세부는 구현하면서 바꿔도 된다. 바꾸면 이 블록을 고치고 "결정 / 배운 점"에 이유를 남긴다.

### 1. 경계 모양: 도메인 인터페이스 + SDK 없는 해석 층

010 설계 결정 1과 같은 방식이다. 어댑터는 SDK 객체에서 **원시값만** 꺼내고, 해석(시간 초과, 값 검사, 주소 정리, 로그)은 `ai.pleos`도 `android`도 없는 층에서 한다.

| 층 | 파일 | `ai.pleos` | `android` | 확인 방법 |
|---|---|---|---|---|
| 도메인 | `domain/location/CurrentLocation.kt`, `domain/location/CurrentLocationSource.kt` | 없음 | 없음 | 컴파일 |
| Fake | `data/location/FakeCurrentLocationSource.kt` | 없음 | 없음 | JVM 테스트 |
| 경계 | `data/guidance/navi/NaviLocationClient.kt`: `NaviLocationClient`, `NaviLocationEvent` | 없음 | 없음 | 컴파일 |
| 해석 | `data/location/NaviCurrentLocationSource.kt`: 시간 초과, 값 검사, 주소 정리, 한 번에 하나, 로그 | 없음 | 없음 | JVM 테스트 (가짜 client) |
| 어댑터 | `data/guidance/navi/PleosNaviClient.kt`의 `currentLocation()` | **여기만** | 있음 | Pleos 에뮬레이터 |
| 구성 | `di/LocationPlan.kt`: `currentLocationKindOf(mode, isAutomotive)` | 없음 | 없음 | JVM 테스트 |
| 화면 | `ui/debug/DebugLocationViewModel.kt`(상태, 문구 함수 포함), `ui/debug/DebugPanel.kt` | 없음 | Compose만 | JVM 테스트(VM, 문구), `/verify` |

```kotlin
// domain/location/CurrentLocation.kt
/** 차의 현재 위치. 나중에 병원 검색의 기준점이 된다. */
data class CurrentLocation(
    val latitude: Double,
    val longitude: Double,
    /** 내비가 준 주소 (예: "수정구 시흥동"). 모르면 null. 빈 문자열은 넣지 않는다 */
    val address: String?,
)

// domain/location/CurrentLocationSource.kt  (suspend 함수라서 fun interface가 아니라 interface로 둔다)
interface CurrentLocationSource {
    /**
     * 지금 위치를 한 번 묻는다. 추적하지 않는다.
     * - 모르면 null: 연결 실패, 시간 초과, 내비 오류, 값 이상.
     * - 취소 말고는 예외를 던지지 않는다. 부른 코루틴이 취소되면 연결을 놓고 CancellationException을 그대로 낸다.
     * - 여러 번 불러도 된다. 부를 때마다 새로 묻는다 (캐시 없음).
     */
    suspend fun current(): CurrentLocation?
}

// data/guidance/navi/NaviLocationClient.kt
fun interface NaviLocationClient {
    /**
     * 구독하면 새 NaviHelper로 연결하고, 연결되면 현재 위치를 한 번 요청해 결과를 내보낸다.
     * 구독을 끊으면 리스너를 떼고 release한다. 경로는 건드리지 않는다 (requestRoute·cancelRoute를 부르지 않는다).
     */
    fun currentLocation(): Flow<NaviLocationEvent>
}

sealed interface NaviLocationEvent {
    /** onCurrentLocationInfo. 주소는 SDK가 준 그대로다 (null·빈 값일 수 있다) */
    data class Located(val latitude: Double, val longitude: Double, val address: String?) : NaviLocationEvent

    /** 연결 실패(onFailed), onError, SDK 호출 예외. 이유는 로그용 이름 (오류 코드 이름, 예외 클래스 이름) */
    data class Failed(val reason: String) : NaviLocationEvent
}
```

- **`NaviClient`에 메서드를 더하지 않고 인터페이스를 따로 두는 이유**:
  - `NaviClient`에 메서드를 더하면 시험용 가짜 `FakeNaviClient`(커밋됨)와 `TripRecordingNaviClient`(010 tester)를 고쳐야 한다. 기존 테스트 파일을 고치게 된다 (완료 기준 3 위반).
  - `NaviEvent`(sealed)에 위치 이벤트를 더하면 `NaviGuidanceSource.handle`의 `when`이 모든 경우를 다루지 않게 되어 컴파일이 깨진다.
  - 길안내 한 번과 위치 한 번은 다른 질문이다.
- **해석 층 `NaviCurrentLocationSource(client, timeout = DEFAULT_TIMEOUT, log: (String) -> Unit = {})`**:
  - `mutex.withLock { withTimeoutOrNull(timeout) { client.currentLocation().first() } }`의 결과를 해석한다.
    - `null`(시간 초과) → null. `first()`가 끝나거나 시간 초과로 취소되면 구독이 끊겨 어댑터가 release한다.
    - `Failed` → null.
    - `Located` → 아래 값 규칙을 통과하면 `CurrentLocation`, 아니면 null.
    - 이벤트 없이 끝남(`NoSuchElementException`), client가 던진 예외 → null.
    - **`CancellationException`은 잡지 않고 다시 던진다** (`catch (e: Exception)`을 쓸 때 주의).
  - **시간 초과 기본값 `DEFAULT_TIMEOUT = 5.seconds`**:
    - 근거: 010 spike에서 `onConnected`가 약 120ms에 왔다 (`[obs-navi-emu]`). `getCurrentLocationInfo` 응답 시간은 기록이 없다.
    - 병원 검색 전에 한 번 묻는 용도라, 사람이 기다릴 수 있는 범위에서 넉넉하게 잡는다.
    - 단계 6(L6)에서 응답 시간을 잰다. 관찰한 가장 긴 시간의 3배가 5초보다 크면 그 값으로 올린다. 줄이지는 않는다.
    - 010 길안내는 경로 계산 때문에 30초까지 기다리지만, 위치는 경로 계산이 없어서 그 값을 따르지 않는다.
  - **값 규칙** (우리 규칙이다. SDK 사실이 아니다. 순수 함수 `currentLocationOf(lat, lon, address): CurrentLocation?`로 둔다):
    - 위도·경도가 NaN이나 무한대면 null.
    - 위도가 [-90, 90], 경도가 [-180, 180] 밖이면 null. 경계값(±90, ±180)은 받는다.
    - (0.0, 0.0)이면 null. 흔히 "위치 없음"으로 쓰는 값이고, 차가 있을 수 없는 바다 위다.
    - 주소: null, 빈 문자열, 공백만 있으면 null. 아니면 앞뒤 공백을 자른다. javap로는 `getAddress()`의 null 여부가 보이지 않아서 어느 쪽이든 받는다.
  - **한 번에 하나** (`Mutex`): 위치 쪽 NaviHelper가 동시에 둘 생기지 않게 한다 (결정 3의 위험을 줄인다). 두 번째 호출은 첫 호출이 끝난 뒤 새로 묻는다. 그래서 최악의 대기는 시간 초과의 2배다.
  - 로그는 결정 6.

### 2. SDK 호출 위치: `PleosNaviClient.kt`에 메서드를 더한다 (패키지는 그대로)

| 안 | 내용 | 장점 | 단점 |
|---|---|---|---|
| **A (고름)** | `class PleosNaviClient(context) : NaviClient, NaviLocationClient`. 같은 파일에 `currentLocation()`을 더한다 | "NaviHelper를 부르는 유일한 파일" 규칙(architecture.md 패키지 구조, 010 완료 기준 4, 파일 KDoc)과 정적 검사가 그대로 맞는다. 010의 `trip` 코드는 한 줄도 바꾸지 않는다. 연결 순서를 한 파일에서 나란히 볼 수 있다 | 파일 이름·패키지가 길안내(`guidance`) 아래라 위치가 들어가기 어색하다 |
| B | `data/location/PleosNaviLocationClient.kt`를 새로 만든다 | 패키지가 자연스럽다 | NaviHelper를 부르는 파일이 둘이 된다. 010의 문서·정적 검사 규칙을 고쳐야 하고, 연결·해제 순서 코드가 두 파일에 따로 생긴다 |

- **A를 고른 이유**: 010이 아직 dev에 없어서 010 파일을 옮기거나 규칙을 바꾸면 두 브랜치가 엉킨다. A는 010 파일에 더하기만 한다.
- **패키지**: `data/guidance/navi/`를 "NaviHelper와의 경계 묶음"으로 본다. 위치 경계 인터페이스(`NaviLocationClient.kt`)도 여기 둔다. 위치를 해석하는 구현과 Fake는 `data/location/`에 둔다. `data/navi/`로 옮기는 일은 범위 밖(후속 `refactor:`)이다.
- 어댑터 기본안 (010 `trip`과 같은 순서: 리스너 → `initialize(onConnected, onFailed)` → 연결되면 호출 → `awaitClose`에서 해제):

```kotlin
override fun currentLocation(): Flow<NaviLocationEvent> = callbackFlow {
    val helper = NaviHelper(appContext)
    val closed = AtomicBoolean(false)
    val listener = object : NaviHelperEventListener {
        override fun onCurrentLocationInfo(currentLocationInfo: CurrentLocationInfo) {
            val address: String? = currentLocationInfo.address // javap로 null 여부가 보이지 않는다
            trySend(NaviLocationEvent.Located(currentLocationInfo.latitude, currentLocationInfo.longitude, address))
        }

        override fun onError(errorCode: NaviErrorCode) {
            trySend(NaviLocationEvent.Failed(errorCode.name))
        }
    }
    try {
        helper.addListener(listener)
        helper.initialize(
            onConnected = {
                if (!closed.get()) {
                    try { helper.getCurrentLocationInfo() } catch (e: Exception) { trySend(NaviLocationEvent.Failed(e.javaClass.simpleName)) }
                }
            },
            onFailed = { error -> trySend(NaviLocationEvent.Failed(error.javaClass.simpleName)) },
        )
    } catch (e: Exception) {
        trySend(NaviLocationEvent.Failed(e.javaClass.simpleName))
    }
    awaitClose {
        closed.set(true) // 해제 뒤에 늦게 온 onConnected는 아무것도 부르지 않는다
        runCatching { helper.removeListener(listener) }
        runCatching { helper.release() }
    }
}
```

- `cancelRoute`를 부르지 않는다. 안내 중일 수 있다.
- 이 파일은 로그를 남기지 않는다 (`trip`과 같다). 좌표가 로그에 들어갈 길을 만들지 않는다.
- `heading`, `routeId`, `currentRouteIndex`는 꺼내지 않는다. 쓸 곳이 없고, 최소한만 받는다.
- KDoc을 고친다: "NaviHelper를 부르는 유일한 파일. 길안내 한 번(`trip`)과 현재 위치 한 번(`currentLocation`)마다 새 NaviHelper를 만들고 끝나면 release한다."

### 3. 수명주기: 물을 때마다 NaviHelper 하나

- 한 번 묻기 = 연결 → `getCurrentLocationInfo()` → 첫 결과 → release. 시간 초과나 부른 쪽의 취소에도 release한다 (`callbackFlow`의 `awaitClose`).
- 010과 같은 원칙: 같은 인스턴스를 release한 뒤 다시 쓰지 않는다. `initialize`/`release`는 `PleosNaviClient.kt`에서만 한다.
- 프로세스 수명 동안 연결을 들고 있지 않는 이유: 추적하지 않고 병원 검색 때 한 번 묻는다. 연결을 유지할 이유가 없고, Pleos 가이드도 불필요한 수집을 멈추라고 한다.
- **위험 (확인되지 않음): 010 길안내 중에 위치를 물으면 NaviHelper 인스턴스가 둘이 된다.**
  - 여러 인스턴스를 허용하는지는 문서에 없다. API Reference 페이지는 본문이 비어 있다 (sdk-notes 656~663행).
  - 생길 수 있는 일:
    - (a) 위치 응답이 오지 않는다.
    - (b) 위치 쪽 `release()`가 안내 쪽 연결이나 내비 경로에 영향을 준다.
    - (c) 이벤트가 섞인다. 안내 쪽 리스너가 위치 응답을 받거나, 위치 쪽 리스너가 안내의 `onError`를 받는다.
  - (c)는 받아들인다: 안내 쪽 리스너는 `onCurrentLocationInfo`를 재정의하지 않아서 무시한다. 위치 쪽이 안내 오류를 받으면 그 한 번이 "모름"이 될 뿐이다.
  - (a)·(b)는 단계 6의 L5에서 본다. **문제가 보이면 대안 F1**: `fix(ui): 안내 중에는 debug 위치 묻기를 막음`. `MainActivity`가 길안내 상태가 안내 중이면 위치 버튼을 끈다. 병원 검색은 안내를 시작하기 전에 하므로 핵심 흐름에는 영향이 없다. 관찰을 sdk-notes와 노트에 남긴다. NaviHelper 하나를 나눠 쓰는 구조 변경은 이번에 하지 않는다.

### 4. 데이터 모드별 구성: 별도 순수 함수

```kotlin
// di/LocationPlan.kt (Android를 모른다)
/** 현재 위치를 어디서 받는지 (011). 차량 기기의 시연·Real은 내비(NaviHelper), 그 밖은 Fake. */
fun currentLocationKindOf(mode: DataMode, isAutomotive: Boolean): SourceKind
```

| 모드 | 차량 기기 | 현재 위치 | 끼우는 구현 | debug 표시 |
|---|---|---|---|---|
| Fake | 상관없음 | Fake | `FakeCurrentLocationSource` | 위치(Fake) |
| 시연 (release) | 예 | 내비 | `NaviCurrentLocationSource(PleosNaviClient)` | 위치(내비) |
| 시연 | 아니오 | Fake | `FakeCurrentLocationSource` | 위치(Fake) |
| Real | 예 | 내비 | `NaviCurrentLocationSource(PleosNaviClient)` | 위치(내비) |
| Real | 아니오 | Fake | `FakeCurrentLocationSource` | 위치(Fake) |

- `SourcePlan`에 칸을 더하지 않고 `naviUseOf`도 고치지 않는 이유 (010 결정 5와 같다):
  - `SourcePlan`에 칸을 더하면 data class 동등성이 바뀌어 007 테스트가 깨진다.
  - 위치와 길안내는 다른 질문이다. 병원 검색이 붙으면 둘의 규칙이 달라질 수 있다.
- 일반 폰에서는 `PleosNaviClient`를 만들지 않는다. 일반 폰에는 내비 서비스가 없다 (010과 같다).
- **release에서는 NaviHelper가 만들어지지 않는다**: Pleos에서는 내비 위치 구현이 끼워진다. 하지만 지금 부르는 곳은 debug 패널뿐이다. `PleosNaviClient`를 만들기만 해서는 NaviHelper가 생기지 않는다 (`callbackFlow`는 구독할 때 만든다).
- **Fake 위치 값**: `CurrentLocation(37.4127212, 127.094681, "수정구 시흥동")`. Pleos 에뮬레이터에서 받은 값이다 (`[obs-navi-emu]`, sdk-notes 793행).
  - 이유:
    - 두 에뮬레이터가 같은 위치를 쓰게 된다. 나중에 병원 검색 결과와 거리가 에뮬레이터마다 달라지지 않는다.
    - 시연 병원까지의 경로(12km·35분)도 Pleos 내비에서 본 값과 맞는다.
    - 사람의 실제 위치가 아니라 에뮬레이터 기본 위치라서 개인정보가 아니다.
  - 버린 안: 시연 병원 근처의 임의 좌표. 근거 값이 없고, 두 에뮬레이터의 위치가 달라진다.
  - 생성자: `FakeCurrentLocationSource(location: CurrentLocation? = DEMO_LOCATION)`. null을 넣으면 항상 모름이다 (테스트용).
- **"시연용" 표시**: 지금은 debug 패널에만 보이므로 출처 이름 "(Fake)"면 된다. 데이터(`address`)에 "시연용"을 넣지 않는다.
  - 사용자 화면에 위치나 위치로 찾은 결과(병원 목록)가 보이게 되면, 그때 병원 검색 계획에서 시연용 표시를 정한다 (출품 유의사항).

### 5. debug 패널 표시

- `ui/debug/DebugLocationViewModel.kt`:
  - `DebugLocationViewModel(source: CurrentLocationSource)`: `uiState: StateFlow<DebugLocationUiState>`, `onAsk()`, `factory(source)`
  - `DebugLocationUiState`: `NotAsked`, `Asking`, `Found(location: CurrentLocation)`, `Unknown`
  - `onAsk()`:
    - `Asking`이면 무시한다 (한 번만 묻는다).
    - 아니면 `Asking`으로 바꾸고 `viewModelScope`에서 `source.current()`를 부른다. 결과에 따라 `Found` 또는 `Unknown`.
    - 결과가 나온 뒤에는 다시 물을 수 있다.
  - 순수 함수 `debugLocationText(state, sourceName): String` (같은 파일 또는 `DebugLocationText.kt`):
    - `NotAsked` → `위치(내비): 아직 묻지 않음`
    - `Asking` → `위치(내비): 묻는 중…`
    - `Found` → `위치(내비): 37.41272, 127.09468 · 수정구 시흥동`. 주소가 없으면 `· 주소 모름`.
    - `Unknown` → `위치(내비): 모름`
    - 좌표는 소수 5자리(약 1m)다. `Locale.ROOT`로 형식을 맞춘다. 기기 언어의 소수점이 쉼표여도 점으로 나와야 한다.
- `DebugPanel`:
  - 새 인자 `location: DebugLocationRow?`, `onAskLocation: () -> Unit`
    - `DebugLocationRow(text: String, isAsking: Boolean)`는 `ui/debug`에 둔다.
    - `location`이 null이면 줄을 숨긴다.
  - 셋째 줄: [위치 묻기] 버튼(묻는 중에는 꺼짐) + 글자(남은 폭, 최대 2줄, 넘치면 말줄임).
  - 미리보기 하나를 더한다 (좁은 세로, 위치 결과가 있는 상태).
  - 상태는 패널이 아니라 VM이 든다. 패널은 여전히 값과 동작만 받는다.
  - `ui/debug`는 `domain`만 import한다 (`data`, `di` 없음).
  - 문구는 `strings.xml`에 넣지 않는다. 지금 debug 패널처럼 debug 전용 하드코딩이다.
- `MainActivity`:
  - `if (BuildConfig.DEBUG)` 블록 안에서 VM을 만든다. release에서는 VM도 만들지 않는다.
  - 출처 이름("내비"/"Fake")은 `container.locationKind`로 정한다.
- **주행 중에는 위치 줄을 숨긴다** (AGENTS.md 규칙 3):
  - `MainActivity`가 `uiState == HomeUiState.Parked`일 때만 `location`을 넘긴다. UNKNOWN도 주행 화면이라 숨긴다.
  - 이유: debug 패널이 주행 화면에 남는 건 "정차로 되돌리는 버튼"이 필요해서다. 위치 줄에는 그런 이유가 없으니 주행 중 정보를 늘리지 않는다.
  - 묻는 중에 주행으로 바뀌어도 VM은 계속 기다린다. 정차하면 결과를 보인다.

### 6. 개인정보

- **좌표와 주소를 logcat에 남기지 않는다.** 로그는 `NaviCurrentLocationSource`가 생성자로 받은 `log` 함수로만 남긴다 (태그 `CurrentLocation`, `AppContainer`가 `Log.i`로 연결한다). 한 번 물을 때 한 줄이다.
  - 예: `현재 위치: 성공 (주소 있음, 134ms)`, `현재 위치: 실패 (CAUSE_SEARCH_ROUTE)`, `현재 위치: 시간 초과 (5000ms)`, `현재 위치: 값 이상`
  - 걸린 시간은 단계 6에서 시간 초과 값을 정하는 근거다.
- 어댑터(`PleosNaviClient`)는 로그를 남기지 않는다.
- 위치를 저장하지 않는다. VM 상태(메모리)에만 있고, 캐시도 없다.
- 확인 방법:
  - 테스트 `로그에 좌표와 주소를 남기지 않는다`
  - 에뮬레이터: 우리 앱 pid 로그에서 좌표 문자열이 0건이다 (L7). 내비 앱이 스스로 남기는 로그는 대상이 아니라서 `--pid`로 거른다.
- 진행 로그, 노트, sdk-notes에는 에뮬레이터 위치만 적는다. 실차 위치는 적지 않는다.

### 7. 기존 코드를 지키는 범위

- 고치지 않는 것: 완료 기준 4의 경로들.
- 더하기만 하는 것: `PleosNaviClient.kt` (인터페이스 하나, 메서드 하나, import, KDoc).
- 고치는 것: `AppContainer.kt`(위치 구현과 `locationKind`), `MainActivity.kt`(debug 패널 연결), `DebugPanel.kt`(인자와 줄 하나).
- 010의 길안내 생성 줄(`PleosNaviClient(context)` 두 곳)은 그대로 둔다. 위치용 `PleosNaviClient`는 따로 만든다. 상태가 없는 클래스라 여러 개여도 된다.

## 확인 필요 (시작 전에 해결)

### [사용자 승인 필요]

- **없음.** 새 `pleos.car.permission.*`을 더하지 않는다. `getCurrentLocationInfo()`는 이미 승인·선언된 `NAVI_ROUTE`의 `get*Info()`다 (sdk-notes `[navi-perm]`, architecture.md "사용하기로 한 Pleos 권한").
- **이럴 때는 그 자리에서 멈추고 묻는다**:
  - 에뮬레이터에서 `SecurityException`이 보일 때
  - 다른 권한(예: `FUSED_LOCATION`, `NAVI_CUSTOM_*`)이 필요하다는 근거가 나올 때. 근거는 예외 메시지나 AAR 매니페스트다.
  - 이때 권한을 선언하지 않는다.

### [선행: pleos-docs-researcher]

- **없음.** 쓰는 API는 모두 확인돼 있다:
  - `NaviHelper(Context)`, `initialize(onConnected, onFailed)`, `addListener`/`removeListener`, `release()`, `getCurrentLocationInfo()`
  - `NaviHelperEventListener.onCurrentLocationInfo`, `onError` (26개 모두 default)
  - `CurrentLocationInfo` getter
  - 출처: sdk-notes 653·685·773·778·782~783행과 메인이 2026-11-05에 javap로 확인한 내용. 메인의 확인 내용은 단계 1에서 sdk-notes에 기록한다 (AGENTS.md 규칙 1: 기록한 뒤에 쓴다).
- 남은 모르는 것은 문서 페이지 본문이 비어 있어서(sdk-notes 656~663행) 관찰로만 알 수 있다. 아래 "관찰로 확인할 것"으로 둔다.

### 관찰로 확인할 것 (단계 6, 문서에 없음)

| # | 볼 것 | 방법 | 결과가 바꾸는 것 |
|---|---|---|---|
| O1 | 010 길안내 중(안내 쪽 NaviHelper가 연결돼 있을 때)에 위치를 물으면 응답이 오는가 | L5 | 결정 3의 대안 F1 |
| O2 | 묻고 나서 결과까지 걸리는 시간. 앱을 새로 띄운 뒤 첫 묻기와 그다음 묻기 | L6, `CurrentLocation` 로그의 ms | `DEFAULT_TIMEOUT` |
| O3 | 실제 `address` 값. 빈 값이나 null이 오는 경우가 있는가. 010 spike의 값(`37.4127212, 127.094681`, "수정구 시흥동")과 같은가 | L1 | Fake 값, sdk-notes |
| O4 | 위치 쪽 `release()` 뒤에도 안내 쪽 연결과 내비 경로가 그대로인가 | L5 (안내 취소로 경로가 사라지는지) | 결정 3의 대안 F1 |

### 기획상 판단 (기본값으로 진행한다. 바꾸려면 사용자가 알려 준다)

- K1. Fake 위치 = Pleos 에뮬레이터 위치(시흥동). 근거는 결정 4.
- K2. 주행 중에는 debug 위치 줄을 숨긴다. 근거는 결정 5.
- K3. Real 모드 + 일반 폰에서 "모름" 대신 Fake 위치를 쓴다 (메인 지시). Real 길안내는 일반 폰에서 자리표시(항상 안내 없음)라서 성격이 다르다. 병원 검색이 붙을 때 다시 볼지 정한다.
- K4. (병원 검색 계획으로 넘긴다) 사용자 화면에 Fake 위치로 찾은 결과가 나올 때의 "시연용" 표시.

## 확인 절차

- 에이전트는 에뮬레이터를 직접 띄우지 않는다. 두 에뮬레이터가 켜져 있으므로 모든 adb 명령에 `-s`를 붙이거나 `ANDROID_SERIAL`을 정한다.
  - Git Bash: `export MSYS_NO_PATHCONV=1; export ANDROID_SERIAL=emulator-5554`
  - PowerShell: `$env:ANDROID_SERIAL='emulator-5554'`
  - `/verify`는 기기를 고르지 않으므로 이렇게 정한 뒤 쓴다.
- Pleos 기어는 006 노트(`docs/notes/xxj15/2026-11-05-real-driving-state.md`)의 `vc()`로 바꾼다. 먼저 `vc "/vehicle/gear?gear=Park"`로 기어가 바뀌는지 본다.
- 스크린샷·UI 트리·로그는 `.claude/.cache/p11-<번호>.*`에 둔다. 커밋하지 않는다.
- 위치 로그: `adb logcat -d -s CurrentLocation`
- 우리 앱 로그만: `adb logcat -d --pid=$(adb shell pidof com.meongcare.app)`
- debug 패널 모드 순서: Fake → 시연 → Real → Fake (누르면 앱이 다시 시작된다).

### Pleos `emulator-5554`

준비: debug 패널을 [Real로 전환]까지 누른다 → "DEBUG · Real · 내비 전부". 기어 P.

| # | 조작 | 기대 |
|---|---|---|
| L1 | 정차 화면에서 [위치 묻기] | "위치(내비): 묻는 중…" → "위치(내비): 37.41272, 127.09468 · 수정구 시흥동". 다른 값이면 그 값을 진행 로그에 적는다 (O3). `CurrentLocation` 로그 한 줄: `성공 (주소 있음, N ms)`. 좌표는 없다 |
| L2 | [위치 묻기]를 빠르게 두 번 (`input tap` 두 번 연달아) → 결과가 나온 뒤 한 번 더 | 묻는 중에는 버튼이 꺼져 있다. 로그 줄은 결과마다 하나다: 처음 두 번에 1줄, 마지막에 1줄 |
| L3 | `gear=Drive` → UI 트리 → `gear=Park` | 주행 화면에 "위치 묻기"가 없다. debug 패널 영역을 빼면 `EditText` 0개, 클릭할 수 있는 노드 0개다 (010 N4와 같다). P로 돌아오면 줄이 다시 보이고, 마지막 결과가 그대로다 |
| L4 | [Fake로 전환] → [시연으로 전환] → "DEBUG · 시연 (주행 Real) · 내비 경로", P에서 [위치 묻기] | L1과 같다 |
| L5 | 시연, P: 카드 → [길 안내 시작] → 내비가 앞에 나온다 → GNB로 우리 앱에 돌아온다 → [위치 묻기] → 내비 화면 캡처 → 우리 앱 → [안내 취소] | 위치가 온다 (O1). 위치를 물은 뒤에도 내비 화면에 경로가 있다 (캡처). 앱 카드는 안내 중 그대로다. [안내 취소] 뒤 내비 경로가 사라진다 (O4, 안내 쪽 연결이 살아 있다는 뜻). 하나라도 어긋나면 결정 3의 대안 F1 |
| L6 | 앱을 강제 종료하고 다시 띄운다 → [위치 묻기] → 한 번 더 | 두 줄의 ms를 진행 로그에 적는다 (O2). 가장 긴 값의 3배가 5초를 넘으면 `DEFAULT_TIMEOUT`을 올린다 |
| L7 | 로그 | `adb logcat -d -t 500 *:E`에 `com.meongcare.app` 크래시와 `SecurityException`이 없다. 우리 앱 pid 로그에서 `grep -cE "37\.41\|127\.09"`가 0이다 |

- 다 보면 기어를 P로 돌린다. 남은 내비 경로가 있으면 취소한다.

### 일반 폰 `emulator-5556`

| # | 조작 | 기대 |
|---|---|---|
| P1 | debug Fake (처음은 정차) → [위치 묻기] | 바로 "위치(Fake): 37.41272, 127.09468 · 수정구 시흥동" |
| P2 | debug 시연 "(주행 Fake)" → [정차] → [위치 묻기] → [주행] | 정차에서는 P1과 같다. [주행]을 누르면 위치 줄이 사라진다 |
| P3 | debug Real | 주행 화면이고 상태는 모름이다. 위치 줄이 없다. 크래시가 없다. `adb logcat -d -s CurrentLocation`이 0줄이다 (내비 위치 구현을 만들지 않는다) |
| P4 | P1 화면을 세로·가로·다크 모드로 본다 (`/verify` 4절) | 위치 줄 글자가 잘리거나 겹치지 않는다 (최대 2줄) |

## dev 반영 순서 (010 다음)

이 브랜치는 010의 `e57a61f` 위에 있다. **011을 010보다 먼저 dev에 넣지 않는다.** 그러면 010 커밋이 010의 검증 없이 011과 함께 들어간다.

1. 010을 마무리하고 `/ship`으로 dev에 넣는다 (010 계획의 남은 단계).
2. 그동안 011은 `e57a61f` 위에서 진행한다.
   - 010에 011이 필요로 하는 수정이 생기면(예: `PleosNaviClient`, `AppContainer`) 010 위로 옮긴다: `git rebase --onto feat/real-navi-guidance <기준> feat/current-location`
   - 그때 `<기준>`(지금은 `e57a61f`)을 새 010 끝 커밋으로 바꿔 진행 로그에 적는다.
3. 010이 dev에 들어간 뒤 011을 옮긴다:

   ```
   git fetch origin
   git rebase --onto origin/dev e57a61f feat/current-location   # e57a61f = 진행 로그에 적힌 <기준>
   git log --oneline origin/dev..HEAD                             # 011 커밋만 보여야 한다
   ```

   - `/ship`의 `git rebase origin/dev`만 쓰면 안 되는 이유: 010이 ship 때 rebase돼 해시가 바뀌었으면, 옛 010 커밋까지 다시 적용하려 한다. `--onto`는 두 경우 모두 맞다.
   - 충돌이 날 만한 곳: `AppContainer.kt`, `MainActivity.kt`, `PleosNaviClient.kt`, `docs/pleos/sdk-notes.md`, `docs/architecture.md`. 둘 다 살리는 쪽으로 푼다.
   - rebase로 들어온 010 변경이 `PleosNaviClient`·`AppContainer`·`MainActivity`·`DebugPanel`을 건드렸으면 L1과 P1을 다시 본다.
4. 완료 기준 1~5를 `$BASE = origin/dev`로 다시 돌린다. 그다음 `/ship`. 이때 `/ship` 안의 rebase는 할 일이 없다.

## 단계

단계 하나 = 커밋 1~3개. 앱 코드가 들어가는 커밋마다 `assembleDebug`가 통과해야 한다 (커밋 직전 훅).

**시작 전 (메인):**

- 작업 트리에 **010 tester 결과로 보이는 미추적 파일 2개**가 있다: `app/src/test/java/com/meongcare/app/data/guidance/NaviGuidanceSourceBoundaryTest.kt`, `TripRecordingNaviClient.kt`.
  - 011 커밋에 넣지 않는다.
  - 권장: `git switch feat/real-navi-guidance` → 010 단계 6의 `test:` 커밋으로 낸다 → `git switch feat/current-location`. 010에서 추적되는 파일이 되므로 011로 돌아오면 작업 트리에서 사라진다.
  - 이렇게 하면 010 끝 커밋이 `e57a61f`보다 앞으로 간다. 011의 `<기준>`은 그대로 `e57a61f`다.
- `adb devices`에 `emulator-5554`, `emulator-5556`이 보이는지 확인한다 (단계 3부터 필요).

### 필수

- [x] 0. 이 계획 파일 커밋 → `docs(plans): 011 현재 위치(NaviHelper) 계획 추가`
  - 위 "시작 전" 정리 뒤에 `.\gradlew.bat testDebugUnitTest`를 돌려 통과 수를 진행 로그에 적는다 (완료 기준 3의 기준값).
  - 확인: `git log -1 --stat`에 이 파일만 있다.

- [ ] 1. AAR로 확인한 현재 위치 API를 sdk-notes에 기록 → `docs(pleos): NaviHelper 현재 위치 API를 AAR로 확인한 내용 기록`
  - 메인이 2026-11-05에 javap로 확인한 사실을 쓴다 (이 계획을 받을 때 넘겨받은 내용). 구현(단계 4)보다 먼저 기록한다 (AGENTS.md 규칙 1).
  - `docs/pleos/sdk-notes.md` "AAR에서 직접 확인한 것 … [aar-navi]" 절에 더한다:
    - `NaviHelper` 3.0.5: `public final void getCurrentLocationInfo()`. 반환값이 없고, 결과는 리스너 콜백으로 온다.
    - `NaviHelperEventListener`: `default void onCurrentLocationInfo(CurrentLocationInfo)`, `default void onError(NaviErrorCode)`
    - `ai.pleos.playground.navi.data.CurrentLocationInfo` (`navi-data` 3.0.4):
      - 생성자 `(double latitude, double longitude, double heading, String address, String routeId, Integer currentRouteIndex)`
      - getter `getLatitude(): double`, `getLongitude(): double`, `getHeading(): double`, `getAddress(): String`, `getRouteId(): String`, `getCurrentRouteIndex(): Integer`
      - **javap 출력으로는 `address`의 null 여부가 보이지 않는다** → 앱은 null과 빈 문자열을 "주소 모름"으로 다룬다.
    - 연결 순서와 스레드는 길안내와 같다: 리스너 등록 → `initialize(onConnected, onFailed)` → 연결되면 호출. `onConnected`는 main, 리스너 콜백은 binder 스레드 (`[obs-navi-emu]`).
  - 778행 "현재 위치: … 필드 정의는 미확인." → "필드 정의 확인 → 위 `[aar-navi]`". 단위·좌표계 설명은 붙이지 않는다 (확인하지 않았다).
  - 824행 "`CurrentLocationInfo` 필드는 미확인" → 확인됨으로 고치고 `[aar-navi]`를 붙인다.
  - "미확인" 목록에 더한다:
    - `- [ ] CurrentLocationInfo.address가 null·빈 값으로 오는 경우가 있는지 (javap로 안 보임) → 011 단계 6 관찰`
    - `- [ ] NaviHelper 인스턴스 둘을 동시에 연결해도 되는지 (길안내 중 위치 묻기) → 011 단계 6 관찰`
  - "출처" 목록에 `[aar-navi]` 정의가 없으면 한 줄 더한다 (2026-11-05 현재 `[aar-navi-data]`만 있다). 010이 같은 줄을 넣어 rebase 때 겹치면 하나만 남긴다.
  - 확인: `git show --stat`에 문서만 있다.

- [ ] 2. 도메인 인터페이스, Fake, 내비 위치 해석 (SDK 없음)
  - 2-1. → `feat(domain): 현재 위치 인터페이스와 Fake 위치 추가`
    - `domain/location/CurrentLocation.kt`, `domain/location/CurrentLocationSource.kt` (결정 1의 블록)
    - `data/location/FakeCurrentLocationSource.kt` (결정 4: `DEMO_LOCATION`, 생성자로 null도 받음)
    - `FakeCurrentLocationSourceTest`:
      - `` `기본값은 Pleos 에뮬레이터와 같은 시연 위치를 바로 돌려준다`() ``
      - `` `여러 번 불러도 같은 값을 준다`() ``
      - `` `위치를 null로 만들면 항상 모름이다`() ``
  - 2-2. → `feat(data): 내비 현재 위치 경계와 시간 초과 처리 추가`
    - `data/guidance/navi/NaviLocationClient.kt` (결정 1의 블록)
    - `data/location/NaviCurrentLocationSource.kt`: `currentLocationOf`, `DEFAULT_TIMEOUT = 5.seconds`, `Mutex`, 로그 (결정 1·6)
    - `NaviCurrentLocationSourceTest` (`runTest`, 가상 시간, 테스트 파일 안의 가짜 client. 구독 수와 해제 수를 센다):
      - `` `내비가 위치를 주면 좌표와 주소를 돌려준다`() ``
      - `` `주소가 null이거나 비었거나 공백뿐이면 주소 모름이다`() ``
      - `` `주소 앞뒤 공백을 자른다`() ``
      - `` `내비가 실패를 알리면 모름이다`() ``
      - `` `시간 안에 답이 없으면 모름이고 연결을 놓는다`() ``
      - `` `위치를 받으면 연결을 놓는다`() ``
      - `` `좌표가 숫자가 아니거나 범위를 벗어나면 모름이다`() ``
      - `` `좌표가 0, 0이면 모름이다`() ``
      - `` `아무것도 주지 않고 끝나면 모름이다`() ``
      - `` `내비 쪽에서 예외가 나도 모름으로 돌려준다`() ``
      - `` `부른 쪽이 취소되면 연결을 놓고 취소를 그대로 전한다`() ``
      - `` `두 번 물으면 매번 새로 연결한다`() ``
      - `` `동시에 물어도 내비 연결은 한 번에 하나다`() ``
      - `` `로그에 좌표와 주소를 남기지 않는다`() ``
  - 확인: 두 테스트 클래스 통과, `assembleDebug`. 완료 기준 5의 `import android.` 검사.

- [ ] 3. debug 패널에 현재 위치 (Fake로 동작 확인)
  - 3-1. → `feat(ui): debug 현재 위치 상태와 문구 추가`
    - `ui/debug/DebugLocationViewModel.kt`: VM, `DebugLocationUiState`, `DebugLocationRow`, `debugLocationText` (결정 5)
    - `DebugLocationViewModelTest` (`MainDispatcherRule`, 테스트 안의 가짜 `CurrentLocationSource`):
      - `` `처음에는 묻지 않은 상태이고 위치를 묻지 않는다`() ``
      - `` `묻는 동안은 묻는 중이고 답이 오면 위치를 보여 준다`() ``
      - `` `모르면 모름 상태가 된다`() ``
      - `` `묻는 중에 다시 눌러도 한 번만 묻는다`() ``
      - `` `답을 받은 뒤에는 다시 물을 수 있다`() ``
    - `DebugLocationTextTest`:
      - `` `좌표는 소수 다섯째 자리까지 점으로 보여 준다`() `` (기본 Locale을 `Locale.GERMANY`로 바꿔도 점인지 본다. 테스트 끝에 되돌린다)
      - `` `주소가 없으면 주소 모름이라고 쓴다`() ``
      - `` `상태마다 출처 이름을 붙인다`() ``
  - 3-2. → `feat(ui): debug 패널에 현재 위치 묻기 추가`
    - `DebugPanel`: `location`, `onAskLocation` 인자, 셋째 줄, 미리보기
    - `AppContainer`: `val currentLocationSource: CurrentLocationSource = FakeCurrentLocationSource()`. 이 커밋에서는 모든 모드가 Fake다. KDoc에 "단계 4-2에서 차량 기기는 내비로" 같은 말은 쓰지 않고 지금 동작만 적는다.
    - `MainActivity`: debug 블록에서 VM 생성, 정차일 때만 `location`을 넘긴다, 출처 이름 "Fake"
    - 확인: `assembleDebug`. `/verify`로 일반 폰 P1·P2·P4. Pleos에서도 Fake 모드 P1이 같은지 한 번 본다.

- [ ] 4. Pleos 어댑터와 데이터 모드 연결
  - 4-1. → `feat(data): Pleos NaviHelper로 현재 위치 묻기 추가`
    - `PleosNaviClient`가 `NaviLocationClient`도 구현한다. `currentLocation()`은 결정 2의 기본안이다. KDoc을 고친다.
    - 새 의존성과 권한은 없다 (NaviHelper 3.0.5와 `NAVI_ROUTE`는 010에 있다).
    - 확인: `assembleDebug`. 완료 기준 4의 `PleosNaviClient` diff 검사(`-` 줄), 완료 기준 5의 Grep과 병합 매니페스트 검사.
  - 4-2. → `feat(di): 차량 기기의 시연·Real 모드에서 내비 현재 위치 사용`
    - `di/LocationPlan.kt`: `currentLocationKindOf` (결정 4의 표)
    - `LocationPlanTest`:
      - `` `차량 기기의 시연과 Real은 내비 위치를 쓴다`() ``
      - `` `Fake 모드는 차량 기기에서도 Fake 위치를 쓴다`() ``
      - `` `일반 폰은 어느 모드든 Fake 위치를 쓴다`() ``
      - `` `release는 차량 기기에서 내비 위치, 일반 폰에서 Fake 위치다`() `` (`resolveDataMode(isDebugBuild = false, …)`와 함께)
    - `AppContainer`:
      - `val locationKind: SourceKind`
      - `currentLocationSource`를 분기한다. REAL이면 `NaviCurrentLocationSource(client = PleosNaviClient(context), log = { Log.i("CurrentLocation", it) })`
    - `MainActivity`: 출처 이름을 `locationKind`로 정한다 ("내비"/"Fake").
    - 확인: `testDebugUnitTest`. Pleos L1~L4, 일반 폰 P3.
    - L1에서 `SecurityException`이 보이면 멈추고 사용자에게 보고한다 ("확인 필요" 참고).

- [ ] 5. 경계 테스트 (`tester`) → `test: 현재 위치 경계 테스트 추가`
  - 메인이 `tester`에게 **이 계획 파일 경로와 대상만** 넘긴다. 구현 설명은 넘기지 않는다.
    - 대상: `NaviCurrentLocationSource`(`currentLocationOf` 포함), `FakeCurrentLocationSource`, `currentLocationKindOf`, `DebugLocationViewModel`, `debugLocationText`
    - 대상 아님: `PleosNaviClient`, `AppContainer`, `MainActivity`, `DebugPanel`(Compose). SDK, Context, Compose가 필요하다.
  - 기대 동작 (계약 기준):
    - `CurrentLocationSource` 계약 (결정 1의 KDoc): 모르면 null, 취소 말고는 예외 없음, 여러 번 불러도 됨, 캐시 없음
    - 시간 초과: 시간 안에 답이 없으면 null이고, 내비 구독이 끊겨 있다 (연결을 놓았다)
    - 연결 실패(`Failed`), 이벤트 없이 끝남, client 예외 → null. 어느 경우든 구독이 남지 않는다.
    - `Located` → 값. 첫 값을 받은 뒤 구독이 남지 않는다.
    - 주소: null, `""`, `"   "` → null. 앞뒤 공백 자름.
    - 좌표 규칙: NaN, 무한대, 범위 밖, (0, 0) → null. 경계값(±90, ±180)은 받는다.
    - 동시 호출: 내비 구독은 한 번에 최대 하나. 두 호출 모두 결과를 받는다.
    - 부른 쪽 취소: `CancellationException`이 그대로 나오고(null로 바뀌지 않음) 구독이 끊긴다.
    - 로그: 호출마다 한 줄, 좌표·주소 문자열 없음
    - 결정 4의 표 전체 (모드 × 기기, release는 `resolveDataMode`와 함께)
    - VM 상태 순서와 두 번 누르기, 문구의 Locale 독립
  - tester가 찾은 버그는 메인이 `fix:` 커밋으로 따로 고친다.
  - 확인: `.\gradlew.bat testDebugUnitTest` 통과. 통과 수와 실패 수를 진행 로그에 적는다.

- [ ] 6. 전체 검증 (`/verify`, 두 에뮬레이터). 커밋은 없다. 고친 게 있으면 `fix(...)` 커밋으로 낸다.
  - `node .claude/hooks/build-gate.mjs pipeline` → "검증 파이프라인 통과."
  - `.\gradlew.bat assembleRelease` 성공
  - 완료 기준 3·4·5의 `git diff`와 Grep을 모두 돌린다 (`$BASE = e57a61f`).
  - `emulator-5554`: L1~L7. O1~O4 결과를 진행 로그에 적는다.
  - `emulator-5556`: P1~P4
  - 결과에 따라:
    - O2: 3배가 5초를 넘으면 → `fix(data): 현재 위치 대기 시간을 관찰값에 맞춤` (KDoc에 관찰값)
    - O1·O4가 실패하면 → `fix(ui): 안내 중에는 debug 위치 묻기를 막음` (결정 3의 F1). L5를 다시 본다.
  - 기기가 꺼져 있으면 멈추고 사용자에게 Cold boot를 요청한다. 확인하지 못한 것은 그대로 적는다.

- [ ] 7. 문서 맞추기
  - `docs/pleos/sdk-notes.md` → `docs(pleos): 에뮬레이터 현재 위치 관찰 기록`
    - "Pleos 에뮬레이터에서 관찰한 것" `[obs-navi-emu]` 절에 011 관찰을 더한다 (날짜, 에뮬레이터 버전, 실차 아님): O1~O4의 결과. 응답 시간(ms), 주소 값, 길안내 중 동시 연결의 결과.
    - 단계 1에서 더한 "미확인" 두 줄을 관찰 결과로 체크하거나 덧붙인다.
  - `docs/architecture.md` → `docs(architecture): 현재 위치 구조와 데이터 모드 기록`
    - "서비스 기능 스택" 현재 위치 행의 상태 칸: "구현함 (011): `data/location/NaviCurrentLocationSource` + `PleosNaviClient.currentLocation`. 물을 때마다 연결하고 release. debug 패널에서만 쓴다 (병원 검색 전). 에뮬레이터 확인: …"
    - "데이터 모드": 결정 4의 표와 Fake 값의 근거. NaviHelper 수명 한 줄: "현재 위치는 물을 때마다 새 NaviHelper, 답을 받으면 release (011)". L5 결과(동시 연결 가능 여부)도 적는다.
    - "패키지 구조": `domain/location/`, `data/location/`, `data/guidance/navi/NaviLocationClient.kt`, `di/LocationPlan.kt`, `ui/debug/DebugLocationViewModel.kt`. `navi/PleosNaviClient.kt` 설명에 현재 위치를 더한다.
  - `AGENTS.md`, `README.md`는 고치지 않는다. NaviHelper를 부르는 파일은 여전히 하나이고, debug 전용 기능이라 시연 방법이 바뀌지 않는다.
  - 확인: 완료 기준 8

- [ ] 8. 개발 노트 작성 (`docs/notes/`)
  - 010이 이미 dev에 들어갔으면 **노트를 쓰기 전에** "dev 반영 순서" 3을 먼저 한다. rebase로 커밋 해시가 바뀌어 노트의 범위가 틀려지지 않게 하려는 것이다. 아직이면 노트의 "커밋"에 해시 대신 커밋 메시지 범위를 적는다.
  - `docs/notes/xxj15/YYYY-MM-DD-current-location.md`. 형식은 `docs/notes/README.md` 템플릿을 따른다.
    - 사용한 Pleos SDK와 권한: NaviHelper 3.0.5 `getCurrentLocationInfo()`, `onCurrentLocationInfo`, `onError`, 권한 `NAVI_ROUTE` (새 권한 없음)
    - 결정: 층 나누기, `PleosNaviClient`에 더한 이유(안 A), 물을 때마다 NaviHelper 하나, 시간 초과 값과 근거(O2), Fake 값, 주행 중 숨김, 로그에 좌표 없음
    - 확인: 테스트 수, L·P 결과, O1~O4
    - 시연 방법: Pleos와 일반 폰에서 debug 패널로 위치 보는 순서
    - **기획서 5번 "개발 계획"에 넣을 문장** (기획 담당에게 넘긴다). 확인한 것만 쓴다. 예: "차량의 현재 위치는 Pleos NaviHelper `getCurrentLocationInfo()`로 받는다 (Pleos 에뮬레이터에서 확인). 병원 검색의 기준점으로 쓸 예정이다."
    - 남은 일: 병원 검색 연결, K3·K4, 후속 `refactor`(패키지 이동, 연결 코드 공통화), 실차 확인
  - 이 계획을 `docs/plans/completed/`로 옮기고 진행 로그를 갱신한다.
  - 커밋:
    - `docs(notes): current-location`
    - `docs(plans): 011 현재 위치 완료`
  - 확인: `git status`에 이 작업의 변경이 남지 않았다. 그다음 위 "dev 반영 순서"대로 `/ship`한다 (010 다음).

### 있으면 좋음 (필수가 끝난 뒤, 단계 8 전에)

- [ ] A. release 확인용 사본으로 위치를 묻지 않는지 확인 (커밋 없음)
  - 010 "확인 절차"의 release 사본 방식을 쓴다 (`keystore.properties`가 없으면 debug 키로 서명한 사본을 `.claude/.cache/`에 둔다. 제출용이 아니다).
  - Pleos와 일반 폰 모두: UI 트리에 "DEBUG"와 "위치 묻기"가 0건이다. `adb logcat -d -s CurrentLocation`이 0줄이다.

## 진행 로그

- 2026-11-05: 계획 작성.
  - 브랜치 `feat/current-location`은 010 `feat/real-navi-guidance`의 `e57a61f`("docs: 차량 내비 길안내 연결 기록")에서 땄다. **`<기준>` = `e57a61f`.** 010은 아직 dev에 없다.
  - 시작 시점의 코드:
    - NaviHelper 의존성(`pleos-navihelper` 3.0.5)과 권한 `NAVI_ROUTE_SEARCH`·`NAVI_ROUTE`가 이미 있다 (010).
    - `data/guidance/navi/NaviClient.kt`: `trip(destination): Flow<NaviEvent>` 하나. `PleosNaviClient`가 NaviHelper를 부르는 유일한 파일이다. 로그는 남기지 않는다.
    - 시험용 가짜: `FakeNaviClient`(커밋됨). 미추적 상태로 `TripRecordingNaviClient`, `NaviGuidanceSourceBoundaryTest`가 있다 (010 tester 결과로 보인다).
    - 현재 위치 코드는 없다. `domain/location/`, `data/location/`도 없다.
    - `DebugPanel` 인자: `modeLabel`, `nextModeLabel`, `showsFakeDrivingControls`, 정차·주행·모름 버튼, 모드 전환.
    - `HomeUiState`는 `Parked`/`Driving`(UNKNOWN 포함)이다.
- 2026-11-05: 단계 0.
  - "시작 전"의 미추적 파일 2개는 010 세션이 별도 worktree(`.claude/.cache/wt-010`, `feat/real-navi-guidance`)로 옮겼다. 이 작업 트리에는 없다.
  - 010 세션과 정한 것: 010은 `NaviClient.kt`·`PleosNaviClient.kt`를 더 고치지 않는다. 010이 더 고치는 곳은 `NaviGuidanceSource.kt`, `NaviRouteOnlyGuidanceSource.kt`, 새 `NaviRemainingTime.kt`, 테스트, 문서(sdk-notes 포함). 010이 `emulator-5554`로 확인하는 동안에는 이쪽에서 설치하지 않는다.
  - 기존 테스트 기준값: `testDebugUnitTest` **324개 통과, 실패 0**.

## 결정 / 배운 점
