# 006 Real 주행 상태 (CarUxRestrictions)

- 목표: `RealDrivingStateSource`를 AAOS `CarUxRestrictionsManager`의 주행 규제 값(`isRequiresDistractionOptimization`)으로 구현해서, Pleos 에뮬레이터에서 기어를 P↔D로 바꾸면 우리 앱이 정차 화면↔주행 화면으로 바뀌게 한다.
- 완료 기준 (어떻게 검증하나):
  1. `node .claude/hooks/build-gate.mjs pipeline`이 "검증 파이프라인 통과."를 출력한다 (`assembleDebug testDebugUnitTest lintDebug`).
  2. `.\gradlew.bat assembleRelease`가 성공한다 (keystore가 없으면 `app/build/outputs/apk/release/app-release-unsigned.apk`가 생긴다). release는 항상 Real이라 이번 변경이 바로 들어가기 때문이다.
  3. 단위 테스트:
     - 단계 1의 새 테스트(`RealDrivingStateSourceTest`)와 tester가 추가한 테스트가 통과한다.
     - 기존 테스트 89개가 그대로 통과한다.
     - `git diff dev -- app/src/main/java/com/meongcare/app/ui/home/HomeViewModel.kt app/src/test/java/com/meongcare/app/ui/home/HomeViewModelTest.kt` 출력이 비어 있다. 안전 규칙(UNKNOWN·첫 값·5초 뒤 복귀는 주행 화면)과 그 테스트 15개를 건드리지 않는다.
  4. 정적 검사 (Grep):
     - `import android.car`는 `app/src/main/java/com/meongcare/app/data/driving/CarUxRestrictionsClient.kt`에만 있다.
     - `data/` 밖에서 `import ai.pleos|import android.car` 0건.
     - `app/build.gradle.kts`에 `useLibrary("android.car")` 1건.
     - 병합 매니페스트(`app/build/intermediates/merged_manifest/debug/processDebugMainManifest/AndroidManifest.xml`, 경로가 다르면 `app/build/intermediates/` 아래에서 찾는다):
       - `android:name="pleos.car.permission` 0건, `android.car.permission` 0건
       - 결정 1에서 DO를 넣기로 했으면 `distractionOptimized` 1건, 아니면 0건
  5. 일반 폰 AVD (`my_avd`, debug):
     - Real로 전환 → 재시작 → 주행 화면 "주행 중". `adb logcat -d -s UxRestrictions`에 "FEATURE_AUTOMOTIVE 없음" 로그가 있다.
     - `adb logcat -d -t 300 *:E`에 `com.meongcare.app` 크래시(특히 `NoClassDefFoundError`)가 없다.
     - Fake로 돌아가서 005 시연의 첫 구간(정차 → [병원으로 안내] → 약 3초 뒤 주행 화면)이 그대로 동작한다.
  6. Pleos 에뮬레이터 (`pleos_connect`, Cold boot, debug, Real): 아래 "Pleos 에뮬레이터 확인 절차"의 표대로 화면이 바뀐다.
     - 앱 로그의 DO 값이 `dumpsys car_service` 덤프의 DO 값과 같다.
     - 스크린샷에서 Pleos 왼쪽 Driving view의 기어 표시와 우리 화면(P = 정차 화면, D = 주행 화면)이 맞는다.
     - DO를 선언했으면: 주행 화면의 UI 트리(`uiautomator dump`)에서 debug 패널 영역을 빼면 `android.widget.EditText` 0개, 클릭 가능한 노드 0개.
     - 홈으로 나가 7초 뒤 돌아오면, 로그에 연결 해제 → 다시 연결이 찍히고 현재 기어에 맞는 화면이 나온다.
     - `SecurityException`, 크래시 0건.
  7. 문서: `docs/architecture.md`에 "Car(AAOS) 연결 수명주기" 절이 있고 "주행 상태와 화면"의 "아직 구현하지 않았다"가 사라졌다. `docs/pleos/sdk-notes.md` "미확인"의 "Pleos 에뮬레이터·실차에서 `Car.createCar()`가 null이 아닌 `Car`를 주는지…" 항목에 관찰 결과가 적혀 있다.
- 브랜치: `feat/real-driving-state` (dev `cc834dc`에서 땄다. 조사·실험 기록 커밋 `f2cda16`이 이미 있다)
- 관련 문서:
  - AGENTS.md (규칙 1·2·3, 아키텍처 규칙)
  - docs/pleos/sdk-notes.md "주행 규제(주행/정차) 판단은 `CarUxRestrictions`로 한다" (약 322~451행): Gradle, `Car.createCar`, 레퍼런스 시그니처, 권한 판단
  - docs/pleos/ux-guidelines.md "주행 규제 (CarUxRestrictions)" (약 66~159행), 특히 "Pleos 에뮬레이터에서 직접 확인한 것"
  - docs/pleos/emulator-setup.md "Pleos 에뮬레이터에서 직접 해 본 것 (관찰, 2026-11-05)" (약 236~265행): Virtual Console HTTP 요청, 상태별 규제 값 표
  - docs/architecture.md "데이터 모드", "주행 상태와 화면", "주행 상태 × 길안내 상태 화면 조합", "Vehicle SDK 수명주기"
  - docs/plans/completed/005-navi-route-fake.md, docs/notes/xxj15/2026-11-05-navi-route-fake.md
  - docs/conventions/git.md

## 범위

- **필수** (약 1일, Pleos 에뮬레이터 확인 포함): 단계 0~8.
  - 11/15 제출물에 주는 효과: release APK가 Pleos 에뮬레이터에서 P일 때 정차 화면을 보여 준다. Pleos 에뮬레이터로 찍는 시연 영상에서 왼쪽 Driving view와 앱 상태가 맞는다.
- **있으면 좋음**: 결정 2의 답에 따라 A 또는 B 하나. 필수가 끝난 뒤, 단계 8(노트) 전에 한다.
- **이번 범위 밖**:
  - Real 길안내 (NaviHelper). 005 계획의 "후속 계획(Real 길안내)으로 넘기는 질문"이 그대로 남는다.
  - `getActiveRestrictions()`에 따른 기능별 제한. 지금 주행 화면에는 제한 대상 기능(키보드, 영상, 설정, 긴 글)이 없다. 로그에만 찍는다.
  - 디스플레이별 규제 (동승석 화면 등). Google 문서는 디스플레이마다 다를 수 있다고 한다 [g-parked-aaos]. 기본 매니저 하나만 쓴다.
  - 기어·속도 직접 읽기 (후보 B `CarPropertyManager`, 후보 C Vehicle SDK `getGear()`). 쓰지 않기로 이미 정했다.
  - 주행 중 안내 시작·음성 입력 (문의 B1·B2 답 이후).

## 설계 결정 (planner 판단과 근거)

구현하면서 바꾸면 "결정 / 배운 점"에 이유를 남긴다.

### 1. 세 층으로 나눈다: JVM으로 시험할 수 있는 부분과 없는 부분을 가른다

JVM 단위 테스트에서는 `android.car` 클래스를 쓸 수 없다고 본다.
- `useLibrary("android.car")`는 컴파일 클래스패스만 준다.
- SDK의 jar를 테스트 클래스패스에 억지로 넣는 방법(`testImplementation files(.../android.car.jar)`)은 쓰지 않는다.
- 그래서 판단 로직을 `android.car`가 없는 층으로 빼서 테스트한다. `android.car`를 직접 부르는 얇은 층은 Pleos 에뮬레이터에서 실행해 확인한다.

| 층 | 파일 (`data/driving/`) | `android.car` | 확인 방법 |
|---|---|---|---|
| 판단·수명 | `RealDrivingStateSource.kt` (수정): 이벤트 → `DrivingState` 매핑, `callbackFlow`, 구독이 끝나면 close | 없음 | JVM 테스트 (가짜 client) |
| 경계 | `UxRestrictionsClient.kt` (새 파일): 인터페이스와 이벤트 타입 | 없음 | 컴파일 |
| 어댑터 | `CarUxRestrictionsClient.kt` (새 파일): `Car.createCar`, `CarUxRestrictionsManager` | **여기만** | Pleos 에뮬레이터 관찰 |
| 생성 | `UxRestrictionsClientFactory.kt` (새 파일): `FEATURE_AUTOMOTIVE` 확인 후 어댑터를 만들거나 null | 없음 (어댑터 클래스만 참조) | 일반 폰 AVD에서 Real 전환 |

경계의 기본안 (이름과 세부는 구현하면서 바꿔도 된다. 바꾸면 이 블록을 고친다):

```kotlin
/** CarUxRestrictions에서 이 앱이 쓰는 값만 옮긴 것. android.car 타입을 밖으로 내보내지 않는다. */
sealed interface UxRestrictionsEvent {
    /** 규제 값을 받았다. true = 주행 최적화(DO)가 필요한 상태 */
    data class Changed(val requiresDistractionOptimization: Boolean) : UxRestrictionsEvent
    /** 값을 알 수 없다: createCar 실패, 매니저 null, 차량 서비스 끊김, 권한 거부 등 */
    data object Unavailable : UxRestrictionsEvent
}

fun interface UxRestrictionsClient {
    /**
     * 차량 서비스에 연결하고 규제 값이 바뀔 때마다 onEvent를 부른다.
     * 연결되면 현재 값을 한 번 보낸다. 돌려받은 것을 close하면 리스너를 해제하고 연결을 끊는다 (두 번 불러도 된다).
     */
    fun connect(onEvent: (UxRestrictionsEvent) -> Unit): AutoCloseable
}

class RealDrivingStateSource(
    /** null = 주행 규제를 받을 수 없는 기기 (일반 폰). UNKNOWN만 낸다. */
    private val client: UxRestrictionsClient? = null,
) : DrivingStateSource
```

- **`RealDrivingStateSource()`(인자 없이)도 컴파일되어야 한다.** `HomeViewModelTest`의 `` `Real 주행 상태 소스로는 시간이 지나도 주행 화면 상태만 보여준다` ``가 그렇게 쓴다. 기본값 null은 "차량이 없는 기기"라는 뜻이므로 이 테스트는 고치지 않아도 계속 맞다.
- `RealDrivingStateSource`는 `android.util.Log`를 부르지 않는다. JVM 테스트에서 "not mocked" 예외가 난다 (`isReturnDefaultValues`는 켜지 않는다, 005와 같은 방침). 로그는 어댑터와 생성 함수에서만 남긴다.
- production용 Fake client는 만들지 않는다. FAKE 모드는 지금처럼 `FakeDrivingStateSource`를 쓴다. 가짜 client는 테스트 파일 안에만 둔다.

### 2. 매핑: 규제 값에 반응한다 (기어·속도로 판단하지 않는다)

| 받은 것 | `DrivingState` |
|---|---|
| 구독 직후, 아직 값 없음 | `UNKNOWN` (먼저 내보낸다) |
| `Changed(true)` | `DRIVING` |
| `Changed(false)` | `PARKED` |
| `Unavailable` | `UNKNOWN` |
| `client == null` | `UNKNOWN` (계속) |
| `connect()`가 예외를 던짐 | `UNKNOWN` (계속. 다시 시도하지 않는다. 다음 구독 때 다시 연결한다) |

- 같은 값이 연달아 오면 한 번만 내보낸다 (`distinctUntilChanged`, `DrivingStateSource` 계약).
- 근거: "apps are expected to react to the restriction information and not to the absolute driving state." [g-uxr] / "Don't attempt to determine the UX restriction state from other hardware state such as gear or speed" [g-parked-aaos]
- 결과: Pleos 에뮬레이터에서는 **기어 D에 정지(0 km/h)여도 주행 화면이다** (덤프상 Idling도 DO true). `[사용자 승인 필요]` 3에서 확인받는다.
- 구독 직후 `UNKNOWN`을 먼저 내므로, 앱을 켤 때 정차 화면 전에 주행 화면이 잠깐 보일 수 있다. `HomeViewModel`의 초기값(주행 화면)과 같은 동작이고 안전 규칙상 의도한 것이다.

### 3. Car 연결 수명주기: Flow 구독에 맞춘다 (`VehicleConnection`처럼 한 곳에 두지 않는다)

- **채택: 구독할 때 연결하고, 구독이 끝나면 끊는다.** `RealDrivingStateSource`의 `callbackFlow`가 `client.connect()`를 부르고, `awaitClose`에서 close한다.
  - 구독자는 `HomeViewModel` 하나다. `WhileSubscribed(5초)`라서 연결은 "화면이 보이는 동안 + 5초"만 산다.
  - Pleos 가이드: 백그라운드에서는 "불필요한 센서 수집을 멈춘다" (ux-guidelines "생명주기와 백그라운드"). Google 샘플도 필요 없어지면 `unregisterListener()`와 `disconnect()`를 부른다 [g-parked-aaos].
  - 다시 구독하면 새로 연결해서 현재 값을 받는다. 그 전까지는 `HomeViewModel`이 주행 화면을 보여 준다 (기존 안전 규칙과 맞물린다).
  - close가 불리는지를 JVM 테스트로 확인할 수 있다.
- 버린 대안: `VehicleConnection`처럼 `ProcessLifecycleOwner`에 붙인 연결 객체 하나.
  - 공유 상태와 Flow 다리가 따로 필요해서 코드가 늘어난다.
  - 단일 Activity 앱이라 프로세스 수명주기와 화면 수명주기가 거의 같아서 얻는 것이 없다.
  - AGENTS.md의 "한 곳에서만 관리"는 Pleos `Vehicle`의 `initialize()/release()`에 대한 규칙이다. `Car`에는 해당하지 않는다.
- 구독자가 둘 이상이 되면 `Car` 연결도 둘이 된다 (각 `Car`가 자기 매니저와 리스너 하나를 가진다). 늘어나면 그때 `shareIn`으로 묶는다.
- 이 결정은 단계 7에서 architecture.md "Car(AAOS) 연결 수명주기" 절에 적는다.

### 4. 어댑터 `CarUxRestrictionsClient` 동작 (모두 sdk-notes에 있는 API)

| 상황 | 동작 | 보내는 이벤트 |
|---|---|---|
| `connect()` | `Car.createCar(appContext, null, Car.CAR_WAIT_TIMEOUT_DO_NOT_WAIT, listener)`. 기다리지 않고 바로 돌아온다. 초기화는 listener 안에서 한다 (sdk-notes: "it is recommended to implement all car related initialization inside CarServiceLifecycleListener.onLifecycleChanged") | - |
| `createCar`가 null을 돌려주거나 예외를 던짐 | 로그 | `Unavailable` |
| `onLifecycleChanged(car, true)` | `car.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE) as? CarUxRestrictionsManager` → `registerListener(...)` → `currentCarUxRestrictions` 읽기. 등록을 먼저 해서 그사이 바뀐 값을 놓치지 않는다 | `Changed(현재 DO)` |
| 매니저가 null이거나 현재 값이 null | 로그 | `Unavailable` |
| `onUxRestrictionsChanged(r)` | `r.isRequiresDistractionOptimization` | `Changed(...)` |
| `onLifecycleChanged(car, false)` (차량 서비스가 죽음, 다시 시작될 예정) | 이전 매니저에 `unregisterListener()`를 시도하고(예외 무시) 버린다. 다시 `true`가 오면 위 순서를 반복한다 | `Unavailable` |
| `SecurityException` | `Log.e`. **에뮬레이터에서 보이면 멈추고 사용자에게 보고한다** (권한 추가는 승인 사항) | `Unavailable` |
| `close()` | 닫힘 표시 → `unregisterListener()` → `car.disconnect()` (각각 try). 그 뒤에 오는 콜백은 무시한다 | - |

- 로그 태그 `UxRestrictions` (기본안). 이벤트마다 `DO=<값> active=0x<getActiveRestrictions()>`를 `Log.i`로 남긴다. `dumpsys`의 값과 맞춰 보기 위해서다.
- 스레드:
  - `connect()`와 `close()`는 메인에서 불린다. 구독자 `HomeViewModel`의 `viewModelScope`가 Main이다. `RealDrivingStateSource`에서 `flowOn`을 쓰지 않는다 (쓰면 Main이 없는 JVM 테스트가 깨진다). KDoc에 "메인 스레드에서 부른다"고 적는다.
  - 콜백 스레드에는 기대지 않는다. `trySend`는 스레드에 안전하다. 닫힘 표시는 `AtomicBoolean`(또는 `@Volatile`)로 둔다.
  - sdk-notes: `handler`가 null이면 createCar 콜백은 메인 스레드, `onLifecycleChanged`는 "always called in the main thread context".
- 같은 `Car`를 다시 쓰지 않는다. `connect()`마다 새로 만든다.

### 5. 일반 폰에서 죽지 않게: `android.car` 클래스를 늦게 로드한다

- sdk-notes: `Car`는 "This API works only for devices with PackageManager.FEATURE_AUTOMOTIVE. Calling this API on a device with no such feature will lead to an exception."
- 생성 함수 `uxRestrictionsClientOrNull(context): UxRestrictionsClient?` (`UxRestrictionsClientFactory.kt`, `android.car` import 없음):
  1. `context.packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)`가 false → `Log.i("FEATURE_AUTOMOTIVE 없음: 주행 규제를 받지 않는다")` → null
  2. true → `CarUxRestrictionsClient(context.applicationContext)`를 만든다. `LinkageError`(`NoClassDefFoundError`, `NoSuchMethodError`)를 잡아 로그 → null
- `CarUxRestrictionsClient`는 클래스 선언부에서 `android.car` 인터페이스를 구현하지 않는다 (`Car.CarServiceLifecycleListener`, `OnUxRestrictionsChangedListener`는 람다나 안쪽 객체로 넘긴다). 일반 폰에서 클래스 로딩 자체가 실패하지 않게 하려는 조심이다. 완료 기준 5(일반 폰 AVD Real 전환)로 실제로 확인한다.
- `AppContainer` REAL 분기: `RealDrivingStateSource(uxRestrictionsClientOrNull(context))`.

### 6. 권한: 새로 선언하지 않는다

- sdk-notes "권한" 판단 (2026-11-05): `CarUxRestrictionsManager`에는 `pleos.car.permission.*`도 `android.car.permission.*`도 선언하지 않는다. 레퍼런스의 공개 메서드 3개에 권한 표기가 없고, AOSP 문서도 "Public APIs to register for UX Restriction changes"라고 한다.
- Pleos 에뮬레이터에서 `SecurityException`이 나면 멈추고 사용자에게 묻는다. 권한을 임의로 추가하지 않는다 (AGENTS.md 규칙 2).

### 7. UI는 바꾸지 않는다

- `HomeViewModel`, `HomeScreen`, `DrivingScreen`, `ParkedScreen`, `homeContentOf`는 그대로 쓴다. Real 소스가 `PARKED`/`DRIVING`/`UNKNOWN`을 내기만 하면 005의 화면 조합 표대로 동작한다.
- Real 모드에서 길안내는 여전히 자리표시(항상 Idle)다. 그래서 정차 화면의 [병원으로 안내]를 눌러도 아무 일이 없다 → `[사용자 승인 필요]` 2.

## 확인 필요 (시작 전에 해결)

### [사용자 승인 필요]

새 `pleos.car.permission.*`은 필요 없다 (설계 결정 6). 아래는 기획 결정이다.

1. **`MainActivity`에 DO(`distractionOptimized`) 선언을 넣을까?** (단계 4 전에 필요)
   - 배경:
     - 넣지 않으면 실제 주행(D) 중에 시스템 화면 "주행 중에는 사용할 수 없습니다."가 우리 앱을 덮는다. 우리 주행 화면("병원까지 N분")은 실차와 Pleos 에뮬레이터에서 보이지 않는다.
     - 넣으면 보인다 (에뮬레이터에서 확인, 2026-11-05). 실차·앱 심사에서 허용되는지는 모른다 (문의 B1).
     - architecture.md "서비스 기능 스택"은 이미 "운전 중 사용: DO 선언"으로 정해 두었다. 몰랐던 것은 Pleos에서의 선언 방법이었다.
   - 선택지:
     - **1a. 넣는다. 이 계획의 별도 커밋 하나로 (권장)**
       - 좋은 점: Pleos 에뮬레이터 시연에서 우리 주행 화면이 보인다 (차량특화성). 같은 에뮬레이터 확인을 한 번에 끝낸다. B1 답이 "안 된다"면 커밋 하나만 `git revert`한다.
       - 부담: 기획서·README에 "DO 선언 허용 여부는 주최측 확인 중"이라고 써야 한다. DO를 선언한 앱은 `ActiveRestrictions`에 맞춰 기능을 직접 제한해야 한다 (Pleos 원문). 지금 주행 화면에는 제한 대상 기능이 없어서 추가 작업은 없다.
     - 1b. 넣지 않는다 (지금 그대로). 심사 위험은 없다. 대신 Real에서 주행 화면은 시스템 화면에 가려지고, 주행 중 남은 시간은 debug Fake 영상으로만 보여 줄 수 있다.
     - 1c. 별도 계획으로 미룬다. 이 계획은 깔끔해지지만, Pleos 에뮬레이터 확인을 두 번 해야 한다.
2. **release에 "시연 모드"를 둘까?** (필수 단계에는 영향 없음. 있으면 좋음 A·B를 고른다)
   - 배경:
     - 규칙은 "release는 항상 Real"이다 (AGENTS.md, architecture.md).
     - 이 계획이 끝나도 release APK의 Real 길안내는 자리표시다. 그래서 다음과 같이 보인다:
       - Pleos 에뮬레이터: P에서 정차 화면이 보이지만, [병원으로 안내]를 누르면 아무 일이 없다.
       - 일반 폰 에뮬레이터: 주행 규제를 받을 수 없어서 항상 "주행 중" 화면만 보인다.
     - 주최측이 제출 APK를 어떤 환경에서 돌려 보는지는 모른다 (문의 A2).
   - 선택지:
     - **2a. 이번에는 넣지 않는다. 바로 다음 계획(007)으로 "release 시연 모드"를 한다 (권장)**
       - 이유: 하루 크기를 지킨다. 데이터 모드 규칙과 AGENTS.md 문구를 바꾸는 별도 결정이다. NaviHelper를 하든 안 하든 필요하다 (일반 폰에서 돌려 보는 경우).
     - 2b. 이번 계획의 있으면 좋음 A로 넣는다: "주행 상태 Real + 길안내 Fake" 모드를 만들고 release에서 쓴다. 화면과 README·기획서에 "시연용 안내"라고 밝힌다. 출품 유의사항: 제출 내용이 사실과 다르면 선정이 취소될 수 있다.
     - 2c. 시연 모드 없이, Real에서 길안내가 안 되면 [병원으로 안내] 대신 "길안내 준비 중"을 보여 준다 (있으면 좋음 B). 반응 없는 버튼은 없어지지만 흐름은 보여 줄 수 없다.
     - 2d. 아무것도 하지 않는다. 권장하지 않는다.
   - 다른 길: Real 길안내(NaviHelper)를 먼저 구현한다. 가장 정공법이지만, 에뮬레이터 내비 앱이 요청을 받는지부터 확인해야 한다 (005 후속 질문). 별도 계획 크기다.
3. **기어 D에 정지(신호 대기)해 있어도 주행 화면으로 볼까?** (단계 1 전에 필요)
   - 기본안: **그렇게 본다.** 플랫폼 규제 값(Pleos 에뮬레이터: D 0 km/h도 DO true)을 그대로 따른다 (설계 결정 2).
   - 영향: 정차 화면(병원 선택, 도착 화면의 진료 카드)은 사실상 P에서만 보인다. 기획 문서의 "정차 중"이 "주차(P) 중"을 뜻하도록 표현을 맞출 필요가 있다 (기획 담당에게 알린다).
   - 버린 대안: 기어·속도로 직접 판단하기. Google 문서가 하지 말라고 한다. `CarPropertyManager` 권한을 Pleos에서 받을 수 있는지도 모른다 (sdk-notes 미확인).

### [선행: pleos-docs-researcher] (조건부. 지금 막는 것은 없다)

필요한 API(`Car.createCar` 4인자, `CarUxRestrictionsManager`, `CarUxRestrictions`, `FEATURE_AUTOMOTIVE`)는 모두 sdk-notes에 있다. 아래는 실행해 보고 막힐 때만 조사한다.

- Q1. **AAOS 기기에서 `android.car` 클래스를 쓰려면 매니페스트에 `<uses-library android:name="android.car" .../>`가 필요한가?**
  - sdk-notes: Google 문서에는 Gradle `useLibrary` 한 줄만 있고 `<uses-library>`는 나오지 않는다.
  - 기본: 넣지 않는다. 단계 3의 Pleos 에뮬레이터 확인에서 `NoClassDefFoundError: android.car.Car`가 나면 그때 조사한다.
  - 조사 결과 넣어야 한다면 `android:required="false"`로 넣는다. `true`(기본값)면 일반 폰 AVD에 설치되지 않는다.
- Q2. **4인자 `Car.createCar(...)`와 `CAR_WAIT_TIMEOUT_DO_NOT_WAIT`는 어느 API 레벨에 추가됐나?**
  - 우리 minSdk는 28이고, Pleos 에뮬레이터는 API 34다.
  - 기본: 어댑터가 `LinkageError`를 잡아 `Unavailable`로 처리하므로 막히지 않는다. `lintDebug`가 `NewApi`를 내면, 메시지의 API 레벨로 `Build.VERSION.SDK_INT` 분기를 넣고 출처를 sdk-notes에 적는다. 레퍼런스 확인이 필요하면 조사한다.

### 실행해서 확인할 것 (문서에 없음)

- Pleos 에뮬레이터에서 `FEATURE_AUTOMOTIVE`가 true인지: `adb shell pm list features`에 `android.hardware.type.automotive`가 있는지 본다.
- `Car.createCar`가 null이 아닌 `Car`를 주고 `onLifecycleChanged(car, true)`가 오는지, `SecurityException`이 없는지 (sdk-notes 미확인 항목).
- DO 미선언일 때 D 0 km/h(Idling)에서도 시스템 화면이 덮는지. 지금 관찰은 D 30 km/h뿐이다.
- 시스템 화면이 사라진 뒤(P로 복귀) 우리 앱이 다시 앞에 오는지.
- 차량 서비스 재시작(`ready=false` → `true`)을 에뮬레이터에서 일으킬 수 있는지. 못 하면 JVM 테스트(가짜 client)로만 확인했다고 적는다.
- `useLibrary("android.car")`가 compileSdk 35에서 빌드되는지. SDK의 `platforms/android-35/optional/`에 `android.car`가 없어서 실패하면 멈추고 보고한다. 우회 설정을 지어내지 않는다.

## Pleos 에뮬레이터 확인 절차

단계 3(DO 없이 짧게)과 단계 6(최종)에서 쓴다. Git Bash 기준. 근거: docs/pleos/emulator-setup.md "Pleos 에뮬레이터에서 직접 해 본 것".

준비:
1. 사용자가 `pleos_connect`를 **Cold boot**로 켠다. 에이전트는 에뮬레이터를 직접 띄우지 않는다 (`/verify` 규칙). 기기가 둘이면 `adb -s <serial>`을 붙인다.
2. debug APK를 설치하고 실행한다 → debug 패널 [Real로 전환] → 재시작 → "DEBUG · Real"을 확인한다.
3. Virtual Console 요청:

```bash
export MSYS_NO_PATHCONV=1
adb forward tcp:8088 tcp:8088
VC=http://127.0.0.1:8088
# 요청마다 세션 신호를 먼저 보낸다 (없으면 503 emulator_inactive)
vc() { curl -s -X POST "$VC/browser/session?client=test" > /dev/null; curl -s -X PUT "$VC$1"; echo; }
vc "/vehicle/gear?gear=Park"
vc "/vehicle/gear?gear=Drive"
vc "/vehicle/controls?speed=30&accelerator=20&brake=0"
vc "/vehicle/controls?speed=0&accelerator=0&brake=0"   # 0으로 안 바뀌면 브라우저 Virtual Console의 브레이크로 멈춘다
curl -s "$VC/vehicle/state"                             # 현재 값 확인
```

4. 조작할 때마다 약 2초 기다린 뒤 다음을 남긴다:
   - 스크린샷: `adb shell screencap -p /sdcard/verify.png` → `adb pull /sdcard/verify.png .claude/.cache/p06-<번호>.png` (커밋하지 않는다)
   - UI 트리: `uiautomator dump`
   - 앱 로그: `adb logcat -d -s UxRestrictions`
   - 플랫폼 값: `adb shell dumpsys car_service --services CarUxRestrictionsManagerService`

기대 결과:

| # | 조작 | 덤프 DO | 앱 로그 | DO 미선언 (단계 3에서 관찰) | DO 선언 (단계 6, 결정 1a) |
|---|---|---|---|---|---|
| 1 | P | false | `DO=false` | 정차 화면 | 정차 화면 |
| 2 | D, 0 km/h | true (0x50) | `DO=true active=0x50` | 시스템 화면이 덮는지 관찰한다 (아직 관찰 안 함) | 주행 화면 "주행 중" |
| 3 | D, 30 km/h | true | 바뀐 값 없음 (주행 유지) | 시스템 화면 "주행 중에는 사용할 수 없습니다." (관찰됨) | 주행 화면 그대로 |
| 4 | D, 0 km/h → P | false | `DO=false` | 시스템 화면이 사라진 뒤 우리 앱이 정차 화면으로 앞에 오는지 관찰한다 | 정차 화면 |
| 5 | D인 채로 앱 강제 종료 후 다시 실행 (`am force-stop` → `am start`) | true | 연결 → `DO=true` | 관찰 | 처음부터 주행 화면 |
| 6 | P에서 홈(`input keyevent HOME`) → 7초 → 앱 다시 열기 | false | 연결 해제 → 다시 연결 → `DO=false` | 정차 화면 | 정차 화면 |

- DO 미선언에서 시스템 화면 뒤의 우리 Activity가 멈추고 5초가 지나면 구독이 끊긴다. 그러면 로그에 연결 해제가 찍힌다. 관찰한 대로 적는다.
- 결정 1에서 1b·1c를 골랐으면 단계 6의 기준은 "DO 미선언" 열이다.

## 단계

단계 하나 = 커밋 1~3개. 커밋마다 `assembleDebug`가 통과해야 한다 (precommit 훅).

**시작 전 준비:**
- 위 `[사용자 승인 필요]` 1~3을 사용자에게 한 번에 확인받는다.
- 미추적 폴더 `.agents/`, `.codex/`는 이 작업과 관계없다. 스테이징하지 않는다. 파일 이름으로만 `git add`한다.
- 사용자에게 알린다: 단계 3에서 `my_avd`, `pleos_connect`를 차례로 Cold boot로 켜 달라고 요청한다. GPU 때문에 하나씩 켜도 된다.

**순서 메모:** 기본 순서(인터페이스·Fake → 도메인·UI → Real)를 따른다. 도메인·UI 단계는 바꿀 것이 없어서 없다 (설계 결정 7).

### 필수

- [x] 0. 이 계획 파일 커밋 → `docs(plans): 006 Real 주행 상태 계획 추가`
  - 확인: `git log -1 --stat`에 이 파일만 있다.

- [x] 1. 주행 규제 경계와 Real 주행 상태 흐름 (JVM 테스트) → `feat(data): 주행 규제 값으로 Real 주행 상태를 정하는 흐름 추가`
  - `data/driving/UxRestrictionsClient.kt`: `UxRestrictionsEvent`, `UxRestrictionsClient` (설계 결정 1). `android.car` import 없음.
  - `data/driving/RealDrivingStateSource.kt`:
    - 생성자 `client: UxRestrictionsClient? = null`
    - `callbackFlow`: `UNKNOWN`을 먼저 보낸다 → client가 있으면 `connect { trySend(매핑) }` → `awaitClose { close }` → `distinctUntilChanged()`
    - `connect`나 `close`가 던지는 예외는 잡는다 (`connect` 실패는 `UNKNOWN`).
    - KDoc: 매핑 표(설계 결정 2), 연결 수명주기(설계 결정 3), "메인 스레드에서 구독한다". "자리표시" 문구를 지운다.
  - `RealDrivingStateSourceTest`:
    - 기존 3개는 그대로 둔다 (`RealDrivingStateSource()` = 차량 없는 기기). 클래스 KDoc의 "자리표시" 설명만 "규제 연결이 없으면 UNKNOWN만"으로 고친다.
    - 테스트 파일 안에 기록용 가짜 client를 둔다: `connect` 횟수와 `close` 횟수를 세고, 테스트가 이벤트를 보낸다.
    - 새 테스트:
      - `` `연결해도 규제 값이 오기 전에는 모름을 먼저 내보낸다`() ``
      - `` `주행 최적화가 필요하다는 규제가 오면 주행을 내보낸다`() ``
      - `` `주행 최적화가 필요 없다는 규제가 오면 정차를 내보낸다`() ``
      - `` `규제 값을 알 수 없게 되면 모름을 내보낸다`() ``
      - `` `연결이 끊겼다가 다시 규제 값이 오면 그 상태를 내보낸다`() ``
      - `` `같은 규제 값이 연달아 와도 한 번만 내보낸다`() ``
      - `` `구독을 끊으면 차량 연결을 닫는다`() ``
      - `` `다시 구독하면 새로 연결한다`() ``
      - `` `연결 중에 예외가 나면 모름을 내보내고 흐름이 끝나지 않는다`() ``
  - `AppContainer`는 아직 바꾸지 않는다 (`RealDrivingStateSource()` 그대로 → 동작 변화 없음).
  - 확인:
    - `.\gradlew.bat testDebugUnitTest --tests 'com.meongcare.app.data.driving.*'` 통과
    - `HomeViewModelTest` 통과, 완료 기준 3의 `git diff` 출력 비어 있음

- [x] 2. `android.car` 어댑터 → `feat(data): CarUxRestrictionsManager로 주행 규제를 받는 래퍼 추가`
  - `app/build.gradle.kts`의 `android { }`에 `useLibrary("android.car")`. 의존성은 처음 쓰는 커밋에 같이 넣는다 (git.md).
  - `data/driving/CarUxRestrictionsClient.kt` (설계 결정 4, 5)
  - `data/driving/UxRestrictionsClientFactory.kt`: `uxRestrictionsClientOrNull(context)` (설계 결정 5)
  - 확인:
    - `assembleDebug`, `testDebugUnitTest`(useLibrary가 단위 테스트를 깨지 않는다), `lintDebug`(새 경고만 처리. `NewApi`가 나오면 확인 필요 Q2)
    - `useLibrary`로 빌드가 실패하면 멈추고 보고한다 ("실행해서 확인할 것" 마지막 항목).

- [x] 3. Real 모드에 연결하고 두 에뮬레이터에서 먼저 확인 → `feat(di): Real 모드에 차량 주행 규제 연결`
  - `di/AppContainer.kt` REAL 분기: `RealDrivingStateSource(uxRestrictionsClientOrNull(context))`
  - `domain/driving/DrivingStateSource.kt` KDoc: "구현은 후속 계획이다" → Real 기준 한 줄 (CarUxRestrictions DO: true = 주행, false = 정차, 모름 = UNKNOWN)
  - 빠른 확인 (실패를 일찍 찾기 위해서다. 전체 확인은 단계 6에서 한다):
    - `my_avd`: Real 전환 → "주행 중", "FEATURE_AUTOMOTIVE 없음" 로그, 크래시 없음 (완료 기준 5의 첫 두 줄)
    - `pleos_connect` (**DO 없이**): "Pleos 에뮬레이터 확인 절차"의 표 1~4행, "DO 미선언" 열을 관찰해서 진행 로그에 적는다. `pm list features`에 automotive가 있는지도 본다.
    - `NoClassDefFoundError: android.car.*` → 멈춘다. 확인 필요 Q1을 메인에게 넘긴다.
    - `SecurityException` → 멈추고 사용자에게 보고한다 (설계 결정 6).
    - 기기가 없으면 빌드만 확인하고 커밋한다. 확인하지 못했다고 진행 로그에 적고 단계 6에서 몰아서 본다.

- [x] 4. DO 선언 (`[사용자 승인 필요]` 1에서 **1a**일 때만) → `feat(manifest): MainActivity에 주행 중 사용(DO) 선언 추가`
  - `AndroidManifest.xml`의 `MainActivity` 안에 `<meta-data android:name="distractionOptimized" android:value="true" />`
    - XML 주석: 근거(ux-guidelines "DO 미선언 앱", Google 방식, Pleos 에뮬레이터에서 동작 확인)와 "실차·심사 허용은 문의 B1 확인 전"
  - 커밋 본문에 적는다: B1 답이 "안 된다"면 이 커밋을 `git revert`한다.
  - 확인: 병합 매니페스트에 `distractionOptimized` 1건. 1b·1c면 이 단계를 건너뛰고 진행 로그에 적는다.

- [x] 5. 경계 테스트 (`tester`) → `test: Real 주행 상태 경계 테스트 추가`
  - 메인이 `tester`에게 **이 계획 파일 경로와 대상만** 넘긴다. 구현 설명은 넘기지 않는다.
    - 대상: `RealDrivingStateSource` (가짜 `UxRestrictionsClient`로)
    - 대상 아님: `CarUxRestrictionsClient`, `uxRestrictionsClientOrNull` (`android.car`·`PackageManager` 필요. JVM으로 시험하지 않는다)
  - 기대 동작 (계약 기준):
    - 설계 결정 2의 매핑 표
    - `DrivingStateSource` 계약: 구독하면 현재 상태를 먼저 받고, 같은 상태를 연달아 받지 않는다
    - 설계 결정 3: 구독이 끝나면 close가 정확히 한 번 불리고, 다시 구독하면 새로 연결한다
    - `connect`/`close`가 예외를 던져도 흐름이 예외로 끝나지 않는다
  - tester가 찾은 버그는 메인이 프로덕션 코드에서 `fix:` 커밋으로 따로 고친다.
  - 확인: `.\gradlew.bat testDebugUnitTest` 통과. 통과 수와 실패 수를 진행 로그에 적는다.

- [x] 6. 전체 검증 (`/verify`). 커밋은 없다. 고친 게 있으면 `fix(...)` 커밋으로 낸다.
  - `node .claude/hooks/build-gate.mjs pipeline` → "검증 파이프라인 통과."
  - `.\gradlew.bat assembleRelease` 성공 (완료 기준 2)
  - 완료 기준 4의 Grep을 모두 돌린다.
  - `my_avd`: 완료 기준 5 전부 (Real 전환, 크래시 없음, Fake 시연 첫 구간)
  - `pleos_connect`: "Pleos 에뮬레이터 확인 절차" 표 1~6행 (결정 1의 답에 맞는 열). 완료 기준 6 전부.
    - 주행 화면은 가로와 세로 중 Pleos 기본 창으로 본다. 글자 잘림·겹침이 없어야 한다.
    - 확인이 끝나면 기어를 P로 돌려 둔다.
  - 기기가 없으면 멈추고 사용자에게 Cold boot를 요청한다. 확인하지 못한 건 그대로 적는다.

- [x] 7. 문서 맞추기
  - `docs/architecture.md`:
    - "패키지 구조": `data/driving/`에 `UxRestrictionsClient`, `CarUxRestrictionsClient`, `UxRestrictionsClientFactory`를 넣고, `RealDrivingStateSource` 설명에서 "자리표시"를 지운다.
    - "주행 상태와 화면": Real 구현 완료, 매핑 표, "D 정지도 주행 화면" (결정 3의 답)
    - 새 절 "Car(AAOS) 연결 수명주기": 설계 결정 3(구독 단위, 버린 대안), 4(상황별 처리), 5(`FEATURE_AUTOMOTIVE` 확인)
    - "서비스 기능 스택" 표의 "운전 중 사용" 행: 결정 1의 결과
    - 의존 방향의 "`android.car.*`는 `data/` 아래에서만"은 그대로 맞는지 본다.
  - `docs/pleos/sdk-notes.md` "미확인":
    - "Pleos 에뮬레이터·실차에서 `Car.createCar()`가 null이 아닌…" 항목에 에뮬레이터 관찰 결과를 적는다 (실차는 미확인으로 남긴다).
    - Q1·Q2를 조사했거나 lint로 알게 된 것이 있으면 출처와 함께 적는다.
  - `docs/pleos/ux-guidelines.md`:
    - "Pleos 에뮬레이터에서 직접 확인한 것"에 우리 앱의 관찰을 적는다: DO 미선언 D 0 km/h, P 복귀 뒤 동작, DO 선언 시 화면.
    - "2026-11-05 기준 우리 AndroidManifest.xml에는 `distractionOptimized` 선언이 없다" 문장을 결정 1의 결과로 고친다.
  - `docs/pleos/emulator-setup.md`: 단계 3·6에서 HTTP 요청이 문서와 다르게 동작했으면(속도 0 등) 고친다.
  - 커밋:
    - `docs(architecture): Real 주행 상태와 Car 연결 수명주기 기록`
    - `docs(pleos): Pleos 에뮬레이터에서 앱의 주행 규제 연결 관찰 기록`
  - 확인: 완료 기준 7

- [x] 8. 개발 노트 작성 (`docs/notes/`)
  - `docs/notes/xxj15/YYYY-MM-DD-real-driving-state.md`. 형식은 `docs/notes/README.md` 템플릿을 따른다.
    - 사용한 API: AAOS `Car.createCar`(4인자), `CarUxRestrictionsManager`, `CarUxRestrictions.isRequiresDistractionOptimization`. Gradle `useLibrary("android.car")`. 권한 없음. (DO를 넣었으면 매니페스트 meta-data도)
    - 결정: 세 층 구조, 구독 단위 연결, 규제 값을 따르는 이유(D 정지 = 주행)
    - 시연 방법: Pleos 에뮬레이터 Cold boot → Real 전환 → Virtual Console 요청 순서 (위 절차). 시연 영상 촬영에 그대로 쓸 수 있게 쓴다.
    - 남은 일:
      - 결정 2의 후속 (release 시연 모드 또는 Real 길안내)
      - 문의 B1 답에 따라 DO 유지 또는 revert
      - 기획서 5번 "개발 계획"에 CarUxRestrictions 사용과 "정차 = P" 표현 반영 (기획 담당)
      - README "테스트 환경"에 Pleos 에뮬레이터 + Virtual Console로 Real 모드를 보는 방법
  - 이 계획을 `docs/plans/completed/`로 옮기고 진행 로그를 갱신한다.
  - 커밋:
    - `docs(notes): real-driving-state`
    - `docs(plans): 006 Real 주행 상태 완료`
  - 확인: `git status`에 이 작업의 변경이 남지 않았다 (`.agents/`, `.codex/` 제외). 그다음 `/ship`.

### 있으면 좋음 (결정 2의 답에 따라 하나만. 단계 8 전에 한다)

- [ ] A. (2b를 골랐을 때) release 시연 모드: 주행 상태 Real + 길안내 Fake
  - `DataMode`에 세 번째 값(이름 미정)을 더한다. 주행 상태는 Real, 길안내는 `FakeGuidanceSource`를 쓴다. `FakeTripScenario`는 돌리지 않는다 (주행 상태는 차가 정한다).
  - release에서 이 모드를 쓰도록 `DataModeStore.read()`를 바꾼다. debug 패널에도 선택지를 넣는다.
  - 화면에 "시연용 안내" 표시를 넣는다. README·기획서에 시뮬레이션이라고 밝힌다.
  - AGENTS.md·architecture.md의 "release는 항상 Real" 문구를 바꾼다.
  - Pleos 에뮬레이터 시연: [병원으로 안내] → Virtual Console D → 주행 화면 "병원까지 N분" → 도착 → P → 도착 화면. 결정 1이 1a일 때만 주행 화면이 보인다.
  - 커밋 예:
    - `feat(di): 주행 상태 Real과 시연용 길안내를 묶은 데이터 모드 추가`
    - `feat(ui): 시연용 길안내 표시 추가`
    - `docs: release 데이터 모드 규칙 변경 기록`
  - 커밋 3개 이상이 되므로 별도 계획(007)으로 하는 편을 권장한다 (결정 2a).
- [ ] B. (2c를 골랐을 때) Real 길안내를 쓸 수 없으면 [병원으로 안내] 대신 "길안내 준비 중" 표시
  - `GuidanceSource`에 "쓸 수 있는지"를 알리는 값을 더한다 (Fake = true, Real 자리표시 = false). `GuidanceViewModel`, `ParkedGuidanceSection`이 따른다.
  - 커밋 예:
    - `feat(domain): 길안내 사용 가능 여부 추가`
    - `feat(ui): 길안내를 쓸 수 없으면 준비 중 표시`
  - 확인: `GuidanceViewModelTest`에 테스트 추가, `/verify`로 Real 모드 정차 화면

## 진행 로그

- 2026-11-05: 계획 작성.
  - 브랜치 `feat/real-driving-state`는 dev(`cc834dc`, 005 반영 뒤)에서 땄다. 조사·실험 기록 커밋 `f2cda16`(docs만)이 이미 있다.
  - 시작 시점의 코드: `RealDrivingStateSource`는 `flowOf(UNKNOWN)` 자리표시다. `AndroidManifest.xml`에 DO 선언이 없다. `app/build.gradle.kts`에 `useLibrary`가 없다. 단위 테스트 89개.
- 2026-11-05: 사용자 결정 (`[사용자 승인 필요]` 1~3 모두 권장안).
  - 1 → **1a**: DO 선언을 이 계획의 별도 커밋으로 넣는다 (단계 4).
  - 2 → **2a**: release 시연 모드는 다음 계획(007)으로 한다. 있으면 좋음 A·B는 하지 않는다.
  - 3 → 기어 D에 정지해 있어도 주행 화면이다 (규제 값을 그대로 따른다).
  - 확인 (시작 전): `C:\Android\platforms\android-35\optional\android.car.jar`가 있고 `optional.json`에 `android.car`가 있다. `javap`로 `Car.createCar(Context, Handler, long, CarServiceLifecycleListener)`, `CarUxRestrictionsManager.registerListener/unregisterListener/getCurrentCarUxRestrictions`, `CarUxRestrictions.isRequiresDistractionOptimization/getActiveRestrictions`가 있는 것을 봤다.

- 2026-11-05: 단계 0~4 완료 (`63c517b`, `64f3dc1`, `03a46ac`, `5ffc044`, `11b8709`).
  - 단계 1: `RealDrivingStateSourceTest` 12개(기존 3 + 새 9) 통과. `HomeViewModelTest` 15개 통과, `HomeViewModel.kt`·`HomeViewModelTest.kt`의 `git diff dev` 비어 있음.
  - 단계 2: `useLibrary("android.car")`로 `assembleDebug testDebugUnitTest lintDebug` 통과. 린트 새 경고 없음(기존 `GradleDependency`·`AndroidGradlePluginVersion`·`OldTargetApi`만). `NewApi` 없음 → Q2 조사 불필요.
  - 단계 3 빠른 확인 (Pleos 에뮬레이터, DO 없이, debug Real):
    - `pm list features`에 `android.hardware.type.automotive` 있음. `NoClassDefFoundError` 없음 → Q1(`<uses-library>`) 불필요.
    - 표 1행 P: 정차 화면, 로그 `DO=false active=0x0`.
    - 표 2행 D 0km/h: 덤프 Driving State 1, DO true(0x50). 앱 로그 `DO=true active=0x50`. **시스템 화면 "주행 중에는 사용할 수 없습니다."가 앱을 가린다.** UI 트리에는 가려진 우리 앱의 "주행 중"이 남아 있다.
    - 표 3행 D 30km/h: Driving State 2. 시스템 화면이 계속 가린다. 앱 로그 새 줄 없음(값이 같아서).
    - 표 4행 P 복귀: `DO=false`. 시스템 화면이 사라지고 우리 앱이 정차 화면으로 앞에 온다.
    - `SecurityException` 없음.
    - `my_avd`(일반 폰) 확인은 단계 6으로 미뤘다 (에뮬레이터를 하나씩 켠다).
  - 단계 4: 결정 1a대로 DO meta-data를 넣었다. 병합 매니페스트에 `distractionOptimized` 1건.

- 2026-11-05: 단계 5~8 완료.
  - 단계 5 (`8529ce8`): tester가 경계 테스트 23개를 더했다 (`RealDrivingStateSourceBoundaryTest` 18, `HomeViewModelRealDrivingStateTest` 5). 처음 결과는 121개 중 120 통과, 1 실패.
    - **찾은 버그**: 규제 값이 구독자가 받기 전에 66개 이상 몰리면 `callbackFlow` 기본 버퍼(64)가 넘쳐 `trySend`가 조용히 실패하고 최신 값이 버려졌다. 실제로는 주행인데 정차로 남을 수 있었다.
    - 고침 (`eaf9726`): `.conflate()`로 최신 값만 남긴다. 그 뒤 121개 모두 통과.
  - 단계 6 (`/verify`):
    - 검증 파이프라인 통과, `assembleRelease` 성공.
    - Grep: `android.car` import는 `data/driving/CarUxRestrictionsClient.kt` 하나. `ui`의 `data` import 0. `RealDrivingStateSource`의 `Log` 0. 병합 매니페스트에 `pleos.car.permission`·`android.car.permission` 선언 0, `<uses-library>` 0, `distractionOptimized` 1. `HomeViewModel.kt`·`HomeViewModelTest.kt`의 `git diff dev` 비어 있음.
    - Pleos 에뮬레이터 (DO 선언, debug Real), 표 "DO 선언" 열:
      - 1 P → 정차 화면, `DO=false`
      - 2 D 0km/h → 주행 화면 "주행 중", `DO=true active=0x50`. 시스템 화면이 가리지 않는다.
      - 3 D 30km/h → 주행 화면 그대로. 왼쪽 Driving view(D, 30)와 앱이 맞는다. 주행 화면 `EditText` 0.
      - 4 P → 정차 화면, `DO=false`
      - 5 D에서 앱 강제 종료 후 다시 실행 → 처음부터 주행 화면, `DO=true`
      - 6 Pleos는 홈 키로 앱이 뒤로 가지 않았다(맨 앞에 남음). 대신 설정 앱을 띄워 뒤로 보내자 약 5초 뒤 `주행 규제 연결을 닫았다`, 다시 열자 `DO=false`로 새로 연결하고 정차 화면.
      - conflate 수정 뒤 1~4를 다시 돌려 같은 결과. 크래시 없음.
    - 일반 폰 에뮬레이터(`emulator-5556`, `FEATURE_AUTOMOTIVE` 없음): Real 전환 → "주행 중", 로그 `FEATURE_AUTOMOTIVE 없음: 주행 규제를 받지 않는다`, 크래시 없음. Fake 복귀 → 안내 시작 → 주행 "병원까지 10분" → 정차 → 안내 취소까지 005 흐름 그대로.
  - 단계 7 (`7694555`, `6bb6c4f`): architecture.md("서비스 기능 스택" 두 행, 패키지 구조, 주행 상태 매핑, "Car(AAOS) 연결 수명주기" 새 절, 테스트 전략), sdk-notes 미확인 항목 해결, ux-guidelines 관찰·DO 선언 문장.
  - 단계 8: 노트 `docs/notes/xxj15/2026-11-05-real-driving-state.md`. 있으면 좋음 A·B는 결정 2a에 따라 하지 않았다.

## 결정 / 배운 점

- **`RealDrivingStateSource`에 `.conflate()`를 더했다** (계획의 매핑 표에는 없던 것). 상태 값이라 최신 값만 의미가 있다. tester가 찾은 버퍼 넘침 버그 때문이다.
- Pleos에서는 홈 키로 앱이 뒤로 가지 않는다. 표 6행은 설정 앱을 띄우는 방법으로 확인했다.
- 단계 3에서 미룬 `my_avd` 확인은 Pleos 에뮬레이터를 끄지 않고 일반 폰 에뮬레이터를 같이 띄워서 했다 (PC 메모리 64GB). `ANDROID_SERIAL`로 기기를 골랐다.

