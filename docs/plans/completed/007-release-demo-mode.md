# 007 release 시연 모드 (주행 상태는 진짜 또는 시나리오, 길안내는 시연용)

- 목표: 제출용 release APK가 Pleos 에뮬레이터와 일반 폰 에뮬레이터 모두에서 "병원 길안내 시작 → 주행 중 남은 시간 → 도착 → 정차 후 진료 카드 자리표시" 흐름을 보여 주는 시연(DEMO) 데이터 모드로 동작하게 하고, 시연용 길안내라는 사실을 화면과 문서에 밝힌다.
- 완료 기준 (어떻게 검증하나):
  1. `node .claude/hooks/build-gate.mjs pipeline`이 "검증 파이프라인 통과."를 출력한다 (`assembleDebug testDebugUnitTest lintDebug`).
  2. `.\gradlew.bat assembleRelease`가 성공한다.
  3. 단위 테스트:
     - 새 테스트(`DataModeTest`, `SourcePlanTest`, `FakeGuidanceSourceProgressGateTest`)와 tester가 추가한 테스트가 통과한다.
     - 기존 테스트 121개가 그대로 통과한다.
     - 아래 `git diff dev -- <경로>`의 출력이 비어 있다. 안전 규칙과 도메인 계약을 건드리지 않는다는 뜻이다:
       - `app/src/main/java/com/meongcare/app/domain/` (`GuidanceSource`, `DrivingStateSource` 인터페이스)
       - `app/src/main/java/com/meongcare/app/ui/home/HomeViewModel.kt`, `app/src/test/java/com/meongcare/app/ui/home/HomeViewModelTest.kt`
       - `app/src/main/java/com/meongcare/app/data/driving/` (006 Real 주행 상태)
       - `app/src/main/java/com/meongcare/app/data/demo/FakeTripScenario.kt`
       - `app/src/main/java/com/meongcare/app/ui/home/DrivingScreen.kt` (결정 3에서 3a일 때. 3b면 이 줄을 뺀다)
  4. 정적 검사 (Grep):
     - `BuildConfig.DEBUG`는 `di/DataModeStore.kt`와 `MainActivity.kt`에만 있다.
     - `ui/` 아래에 `import com.meongcare.app.data` 0건, `import com.meongcare.app.di` 0건
     - `import android.car`는 `data/driving/CarUxRestrictionsClient.kt`에만 있다.
     - 병합 매니페스트(`app/build/intermediates/` 아래 `AndroidManifest.xml`)에 `android:name="pleos.car.permission` 0건, `android.car.permission` 0건
  5. 일반 폰 에뮬레이터 (`my_avd`): 아래 "확인 절차"의 표 P1~P4 (debug 시연 모드). 결정 5의 답에 따라 R1~R3 (release).
  6. Pleos 에뮬레이터 (`pleos_connect`, Cold boot): 아래 "확인 절차"의 표 D1~D10 (debug 시연 모드). 결정 5의 답에 따라 R1~R3 (release).
     - `adb logcat -d -t 300 *:E`에 `com.meongcare.app` 크래시와 `SecurityException`이 없다.
  7. 문서:
     - `AGENTS.md`와 `docs/architecture.md`에 "release는 항상 Real" 문구가 없고, release = 시연 모드라고 적혀 있다.
     - `docs/architecture.md` "데이터 모드"에 아래 "설계 결정 1"의 표가 있다.
     - `README.md` "테스트 환경 및 필요 설정"에 두 환경(Pleos 에뮬레이터, 일반 폰 에뮬레이터)의 시연 방법이 있고, 길안내가 시연용(실제 내비게이션 연동 전)이라는 문장이 있다.
- 브랜치: `feat/release-demo-mode` (dev `cf27ead`에서 땄다)
- 관련 문서:
  - AGENTS.md (규칙 1·2·3, 아키텍처 규칙의 "release는 항상 Real")
  - docs/contest/requirements.md "출품 유의사항" (제출 내용이 사실과 다르면 선정 취소), "GitHub 저장소 규칙" (README 필수 항목)
  - docs/architecture.md "결정" 표의 "Fake/Real 전환", "패키지 구조", "데이터 모드", "주행 상태와 화면", "Car(AAOS) 연결 수명주기", 문의 A2
  - docs/plans/completed/006-real-driving-state.md "[사용자 승인 필요] 2"와 "있으면 좋음 A" (이 계획의 초안)
  - docs/plans/completed/005-navi-route-fake.md "결정 2·3" (Fake 길안내 시간, FakeTripScenario)
  - docs/notes/xxj15/2026-11-05-navi-route-fake.md, docs/notes/xxj15/2026-11-05-real-driving-state.md ("시연 방법", Virtual Console 명령)
  - docs/pleos/emulator-setup.md "Pleos 에뮬레이터에서 직접 해 본 것" (Virtual Console HTTP 요청)
  - docs/pleos/ux-guidelines.md "외부 서버·네트워크"의 보안 가이드 ("배포용 앱에는 … 정식 배포용 서명 인증서")
  - .claude/skills/release-apk/SKILL.md, .claude/skills/verify/SKILL.md
  - docs/conventions/git.md

## 범위

- **필수** (약 1일, 두 에뮬레이터 확인 포함): 단계 0~9.
  - 11/15 제출물에 주는 효과:
    - 제출 APK를 어느 에뮬레이터에서 돌려도 핵심 흐름이 끝까지 돈다.
    - release APK에는 debug 패널이 없다. 그래서 Pleos 에뮬레이터에서 release APK로 시연 영상을 찍으면 debug 띠 없이 왼쪽 Driving view(기어)와 앱 화면이 함께 맞게 나온다.
- **있으면 좋음**: 단계 A·B. 필수가 끝난 뒤, 단계 9(노트) 전에 한다.
- **이번 범위 밖**:
  - Real 길안내 (NaviHelper). 005의 "후속 계획(Real 길안내)으로 넘기는 질문"이 그대로 남는다. Real이 생기면 release 기본 모드를 다시 정한다 (결정 4).
  - `/release-apk` 실행과 `apk/`에 APK 넣기. 팀명·앱 이름(AGENTS.md가 아직 `<팀명>`/`<앱이름>`)과 팀 정식 키스토어가 있어야 한다. 사용자가 직접 실행하는 스킬이다.
  - 주행 상태 Flow를 `shareIn`으로 묶기 (설계 결정 3).
  - Pleos `Vehicle` 연결을 시연 모드에서 만들기 (설계 결정 1).
  - 병원 검색, 진료 카드 내용, 음성 입력.

## 설계 결정 (planner 판단과 근거)

구현하면서 바꾸면 "결정 / 배운 점"에 이유를 남긴다. `[사용자 승인 필요]`가 붙은 것은 아래 "확인 필요"에서 묻는다. 아래 내용은 모두 **권장안 기준**이다.

### 1. 데이터 모드 세 개와 소스 구성표 (순수 함수)

`DataMode`에 `DEMO`를 더한다: `FAKE`, `DEMO`, `REAL`. 화면 표시 이름은 "시연".

어떤 구현을 끼울지는 `AppContainer` 안의 `when` 대신 **Android를 모르는 순수 함수**가 정한다. 그래야 JVM 테스트로 표 전체를 확인할 수 있다.

```kotlin
// di/SourcePlan.kt (기본안. 이름과 세부는 구현하면서 바꿔도 된다. 바꾸면 이 블록을 고친다)
enum class SourceKind { FAKE, REAL }

data class SourcePlan(
    val driving: SourceKind,
    val guidance: SourceKind,
    /** FakeTripScenario로 Fake 주행 상태를 길안내에 맞춰 바꾼다 (주행 상태와 길안내가 모두 Fake일 때만) */
    val runsTripScenario: Boolean,
    /** Fake 길안내가 주행(DRIVING) 중에만 남은 시간을 줄인다 (설계 결정 2) */
    val guidanceWaitsForDriving: Boolean,
    /** Pleos Vehicle 연결(VehicleConnection)을 만든다 */
    val createsVehicleConnection: Boolean,
) {
    /** 화면에 "시연용 길안내" 표시를 할지 (설계 결정 4) */
    val isGuidanceSimulated: Boolean get() = guidance == SourceKind.FAKE
}

/** @param isAutomotive 기기에 PackageManager.FEATURE_AUTOMOTIVE가 있는지 (AppContainer가 구해서 넘긴다) */
fun sourcePlanOf(mode: DataMode, isAutomotive: Boolean): SourcePlan
```

| 모드 | 차량 기기 (`FEATURE_AUTOMOTIVE`) | 주행 상태 | 길안내 | 시나리오 | 주행 중에만 시간 흐름 | Vehicle 연결 | 시연용 표시 |
|---|---|---|---|---|---|---|---|
| FAKE | 상관없음 | Fake | Fake | O | X (005 그대로) | X | O |
| **DEMO** | **예** (Pleos 에뮬레이터, 실차) | **Real** (CarUxRestrictions) | Fake | X | O | X | O |
| **DEMO** | **아니오** (일반 폰 에뮬레이터) | **Fake** | Fake | **O** | O | X | O |
| REAL | 상관없음 | Real | Real (자리표시) | X | X | O | X |

- **DEMO가 기기에 따라 주행 상태 출처를 고르는 이유** (`[사용자 승인 필요]` 1):
  - 심사위원이 APK를 어디서 돌릴지 모른다 (문의 A2 미답).
  - Pleos 에뮬레이터에서는 Real 주행 상태가 맞다. Virtual Console로 기어를 바꾸면 화면이 바뀌고, 왼쪽 Driving view와 앱이 맞는다 (006에서 확인). 차량특화성을 보여 주는 장면이다.
  - 일반 폰 에뮬레이터에서는 Real 주행 상태가 항상 UNKNOWN이다. 그래서 주행 화면만 보이고 [병원으로 안내]를 누를 수조차 없다 (006에서 확인). 이때는 Fake 주행 상태 + `FakeTripScenario`(005)로 흐름이 저절로 이어지게 한다.
  - 판별 기준은 006의 `uxRestrictionsClientOrNull`과 같은 `FEATURE_AUTOMOTIVE`다. Google 문서의 판별 방법이다 (sdk-notes "`CarUxRestrictionsManager` 얻기와 리스너 등록").
- **DEMO는 Vehicle 연결을 만들지 않는다.** 지금 `VehicleConnection.vehicle`을 쓰는 기능이 없다. 만들지 않으면 AGENTS.md의 "VehicleConnection (REAL 모드에서만…)" 문구도 그대로 맞다. Vehicle 도메인 래퍼가 생기면 이 표의 칸을 다시 정한다.
- `isAutomotive`는 `AppContainer`가 `context.packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)`로 구한다. `di/`는 Android를 써도 된다. 순수 함수에는 Boolean만 넘긴다.
- REAL 행은 지금 동작 그대로다 (Real 길안내는 자리표시).

### 2. 데이터 모드 결정과 전환도 순수 함수로

```kotlin
// di/DataMode.kt 에 둔다 (Android를 모른다)
fun resolveDataMode(isDebugBuild: Boolean, savedName: String?): DataMode
fun DataMode.next(): DataMode   // debug 패널 전환 순서: FAKE → DEMO → REAL → FAKE
```

- debug: 저장된 이름이 `DataMode` 값이면 그것, 없거나 모르는 값이면 `FAKE` (지금과 같음).
- release: 저장된 값과 상관없이 **`DEMO`** (`[사용자 승인 필요]` 4). 단계 1에서는 지금처럼 `REAL`로 두고, 단계 5에서 `DEMO`로 바꾼다. 바꾸는 커밋 하나만 되돌리면 이전으로 돌아갈 수 있게 하려는 것이다.
- `DataModeStore.read()`는 `resolveDataMode(BuildConfig.DEBUG, prefs.getString(...))`만 부른다.
- **release에는 모드를 바꾸는 UI를 두지 않는다.** debug 패널은 debug 빌드에만 있다. 숨은 전환(길게 누르기 등)도 두지 않는다. 심사위원이 모르는 기능이고, 배포 앱에서 디버그 기능을 끄라는 Pleos 보안 가이드 취지와도 맞지 않는다.

### 3. 시연 모드의 시간 흐름: 주행 중일 때만 남은 시간이 준다 (`[사용자 승인 필요]` 2)

문제: 지금 Fake 길안내는 주행 여부와 상관없이 약 25초 뒤 도착한다. Pleos 에뮬레이터에서 사람이 Virtual Console로 기어를 바꾸며 시연하면 다음과 같이 된다:
- P에 있는 동안에도 "병원까지 N분"이 줄어든다.
- 기어를 늦게 바꾸면 P에 있는 채로 도착해 버린다. 그러면 주행 화면 없이 바로 도착 화면이 나온다.

권장안 (2a): **`FakeGuidanceSource`에 "진행 조건"을 생성자 인자로 하나 더한다.** 도메인 인터페이스 `GuidanceSource`는 바꾸지 않는다.

```kotlin
class FakeGuidanceSource(
    scope: CoroutineScope,
    timing: FakeGuidanceTiming = FakeGuidanceTiming(),
    /** null이면 지금처럼 시간만으로 흐른다. 값이 있으면 true인 동안에만 남은 시간이 준다. */
    progressGate: Flow<Boolean>? = null,
)
```

- 계약:
  - 경로 계산(`routeCalculationDelay`, "경로를 찾는 중" → 처음 남은 시간)은 진행 조건과 상관없이 끝난다. 정차한 채로 안내를 시작해도 "병원까지 12분"이 바로 보인다.
  - 그 뒤로는 **간격(`tickInterval`)을 하나 세기 시작하기 전에 진행 조건이 true가 될 때까지 기다린다.** 세는 도중에 false가 되면 그 간격은 끝까지 세고, 다음 간격 전에 멈춘다. 즉 정차하면 최대 한 단계까지 더 줄고 멈춘다.
  - 도착(`Arrived`)은 마지막 간격이 지나야 오므로 주행 중에만 온다.
  - **안내 한 번(시작 → 도착 또는 취소) 동안 진행 조건은 한 번만 구독한다.** 도착하거나 취소되거나 다른 목적지로 다시 시작하면 구독을 끊는다.
    - 이유: 간격마다 `first { it }`로 새로 구독하면, Real 주행 상태는 구독할 때마다 `Car`를 새로 연결한다 (006 "Car 연결 수명주기"). 2초마다 연결과 해제가 반복된다.
    - 구현 주의: 진행 코루틴 안에서 `stateIn(this, ...)`을 쓰면 공유 코루틴이 자식으로 남는다. 그러면 도착한 뒤에도 진행 Job이 끝나지 않고 연결이 열려 있다. 끝날 때 명시적으로 취소한다.
  - `progressGate == null`이면 지금 동작과 같다. 그래서 FAKE 모드(005 시연 타임라인)와 기존 테스트가 바뀌지 않는다.
- 이 방식이 Real에도 더 가깝다. 실제 내비게이션도 차가 서 있으면 남은 시간이 줄지 않는다. 주행 상태를 **읽기만** 하고 바꾸지 않으므로, 005에서 버린 대안("Fake 길안내가 주행 상태를 직접 바꾼다")과는 다르다.
- `AppContainer`는 DEMO일 때만 진행 조건을 넘긴다: `drivingStateSource.drivingState`를 `it == DrivingState.DRIVING`으로 바꾼 Flow.
  - UNKNOWN은 진행하지 않는다.
  - **Real 주행 상태일 때는 `.flowOn(Dispatchers.Main)`을 붙인다.** `RealDrivingStateSource`는 메인 스레드에서 구독해야 한다 (006 KDoc: `connect()`가 메인에서 불린다). `FakeGuidanceSource`의 진행은 `applicationScope`(Default)에서 돈다.
- **Car 연결이 둘이 된다** (화면의 `HomeViewModel` 하나, 진행 조건 하나). 진행 조건 쪽은 안내 중에만 산다.
  - 006 설계 결정 3은 "구독자가 둘 이상이 되면 연결도 둘이 된다. 그때 `shareIn`으로 묶는다"였다.
  - 이번에는 **묶지 않는다.** `shareIn`의 replay가 `HomeViewModel`의 안전 규칙(5초 뒤 돌아오면 새 값을 받기 전까지 주행 화면)과 엮인다. 그 테스트를 다시 봐야 해서 하루 범위를 넘는다.
  - Pleos 에뮬레이터에서 연결 두 개가 문제없이 같은 값을 받는지 확인한다 (확인 절차 D10). 문제가 있으면 멈추고 보고한다.
- 비용: `FakeGuidanceSource` 약 20줄 + 테스트 6~7개 + `AppContainer` 몇 줄. 약 0.3일.
- 버린 대안:
  - 2b. 그대로 둔다 (시간만으로 흐름). 비용 0. 시연하는 사람이 [병원으로 안내]를 누르자마자 기어를 D로 바꿔야 하고, P에서 시간이 주는 장면이 영상에 남는다.
  - 2c. 2b + 시연 모드만 느린 timing (예: 간격 4초). P에서 시간이 주는 문제는 그대로다.
  - 2d. `GuidanceSource`에 `pause()`/`resume()`을 더하고 시나리오가 부른다. NaviHelper에 없는 동작을 도메인에 넣게 된다. 사용자 요청("인터페이스를 Real과 다르게 비틀지 않는다")과 맞지 않는다.

### 4. 시연용 표시 (`[사용자 승인 필요]` 3)

출품 유의사항: 제출 서류의 내용이 사실과 다르면 선정이 취소될 수 있다. 그래서 길안내가 시연용이라는 사실을 숨기지 않는다.

권장안 (3a):
- **정차 화면의 길안내 카드**(`ParkedGuidanceSection`, 안내 없음·안내 중 둘 다)와 **도착 화면**(`ArrivedScreen`)에 작은 안내 한 줄을 넣는다.
  - 문구 기본안 (`strings.xml` `guidance_simulated_notice`): "시연용 길안내예요. 실제 내비게이션과는 아직 연결되지 않았어요."
  - 스타일: `MaterialTheme.typography.labelMedium` 또는 `bodySmall`, 색 `onSurfaceVariant`. 움직임과 터치는 없다.
- **주행 화면은 바꾸지 않는다** (AGENTS.md 규칙 3: 큰 글씨 상태 한 줄 + 작은 줄 하나). 안내 중 주행 화면의 작은 줄에는 이미 병원 이름 "시연용 동물병원"이 보인다.
- 표시 여부는 `SourcePlan.isGuidanceSimulated`로 정한다. FAKE와 DEMO에서는 보이고, REAL에서는 보이지 않는다. debug Fake로 찍은 영상에도 같은 표시가 남는다.
- 값 전달: `MainActivity`가 `container.sourcePlan.isGuidanceSimulated`를 읽는다 → `HomeScreen(isGuidanceSimulated = …)` → `ParkedScreen` → `ParkedGuidanceSection`, 그리고 `ArrivedScreen`. `ui/`는 `di`나 `data`를 import하지 않는다. `GuidanceSource`나 `GuidanceUiState`에 이 값을 넣지 않는다 (도메인을 비틀지 않는다).
- 문서:
  - README "테스트 환경 및 필요 설정"과 "실행 방법"에 "길안내는 시연용이다. Pleos NaviHelper 연동 전이라 정해진 시간대로 남은 시간이 줄고 도착한다. 주행/정차는 차량 기기에서 실제 주행 규제 값을 따른다"는 내용을 쓴다.
  - 기획서: 개발 노트에 "기획서 5번 '개발 계획'에 넣을 문장"을 남기고, 기획 담당에게 넘긴다. `docs/product/idea.md`는 아직 빈 템플릿이고 기획 담당이 BMAD로 채우는 중이라, 이 계획에서는 고치지 않는다 (`[사용자 승인 필요]` 3의 하위 질문).
- 버린 대안:
  - 3b. 모든 화면(주행 화면 포함) 구석에 "시연" 고정 배지. 가장 분명하지만 주행 화면에 요소가 하나 늘어난다.
  - 3c. 화면 표시 없이 README·기획서에만 쓴다. 영상과 APK만 보는 심사위원에게는 드러나지 않는다. 권장하지 않는다.

### 5. debug 패널

- 윗줄 버튼 하나가 다음 모드로 돈다 (`DataMode.next()`): "DEBUG · Fake" [시연으로 전환] → "DEBUG · 시연" [Real로 전환] → "DEBUG · Real" [Fake로 전환].
  - 시연 모드에서는 라벨에 주행 상태 출처를 붙인다: "DEBUG · 시연 (주행 Real)" / "DEBUG · 시연 (주행 Fake)". 어느 구성인지 스크린샷만 보고 알 수 있게 하려는 것이다.
  - 버튼을 세 개로 늘리지 않는다. 패널 높이가 늘면 가로 화면에서 버튼이 더 밀린다 (005에서 알려진 한계).
- 아랫줄 [정차]/[주행]/[모름]은 **주행 상태가 Fake일 때** 보인다 (`SourcePlan.driving == FAKE`). FAKE와 "DEMO + 일반 폰"이 해당한다. 지금의 `isFakeMode` 대신 이 값을 넘긴다.
- `DebugPanel`은 지금처럼 상태가 없다. 문자열과 Boolean만 받는다. `MainActivity`가 계산해서 넘긴다.

### 6. release APK 설치 확인과 서명 (`[사용자 승인 필요]` 5)

- 지금 저장소에는 `keystore.properties`가 없다 (`keystore.properties.example`만 있다). 그래서 `assembleRelease` 결과는 `app/build/outputs/apk/release/app-release-unsigned.apk`다.
- Android는 서명 없는 APK를 설치하지 않는다 (일반 Android 동작. 보통 `INSTALL_PARSE_FAILED_NO_CERTIFICATES`). 단계 7에서 한 번 시도해서 실제 메시지를 진행 로그에 적는다.
- 그래서 release 코드 경로(`BuildConfig.DEBUG == false` → DEMO, debug 패널 없음)를 기기에서 보려면 서명이 필요하다. 선택지:
  - **5a. 이번 확인은 debug 키로 서명한 "확인용 사본"으로 한다 (권장).**
    - `apksigner sign --ks %USERPROFILE%\.android\debug.keystore …`로 `.claude/.cache/`에 사본을 만든다. 커밋하지 않는다. 키를 새로 만들지 않는다 (debug 빌드가 이미 쓰는 키).
    - apksigner가 정렬 문제를 내면 build-tools의 `zipalign`으로 먼저 정렬한다.
    - 제출용이 아니다. Pleos 보안 가이드는 배포용 앱에 테스트·디버그 인증서를 쓰지 말라고 한다. 노트에 적는다.
  - 5b. 팀 정식 키스토어를 지금 만든다. 사용자가 만들고 팀에서 공유한다 (`/release-apk` 규칙: 에이전트가 임의로 키를 만들지 않는다). 11/15 APK에 어차피 필요하다. 만들어지면 `assembleRelease`가 서명된 APK를 낸다.
  - 5c. release 설치 확인을 하지 않는다. 순수 함수 테스트와 debug 시연 모드로만 확인한다. 권장하지 않는다. release만의 경로(debug 패널 없음, 새로 설치했을 때의 기본 모드)를 보지 못한다.
- 어느 쪽이든 설치 전에 `adb uninstall com.meongcare.app`을 한다. 서명이 다르면 덮어쓰기가 안 되고, 처음 설치했을 때의 기본값(DEMO)을 보려는 것이다.

## 확인 필요 (시작 전에 해결)

### [사용자 승인 필요]

새 `pleos.car.permission.*`은 필요 없다. Pleos API를 새로 쓰지 않는다 (006의 `CarUxRestrictions`를 그대로 쓴다). 아래는 기획 결정이다. 한 번에 묻는다. 모두 권장안이 위 설계 결정에 반영돼 있다.

1. **시연 모드의 주행 상태를 기기에 따라 고를까?** (단계 3 전에 필요)
   - **1a. 차량 기기(`FEATURE_AUTOMOTIVE`)면 Real(기어로 바뀜), 아니면 Fake + 자동 시나리오 (권장)**
     - 어느 에뮬레이터에서도 흐름이 끝까지 돈다. Pleos에서는 기어 조작이 화면에 반영된다.
   - 1b. 항상 Real. 일반 폰 에뮬레이터에서는 주행 화면만 보이고 안내를 시작할 수 없다.
   - 1c. 항상 Fake + 시나리오. 어디서나 같지만, Pleos 에뮬레이터에서 기어(왼쪽 Driving view)와 앱 화면이 따로 논다.
2. **시연 모드에서 남은 시간이 주행 중에만 줄게 할까?** (단계 2 전에 필요)
   - **2a. 그렇게 한다 (권장).** 설계 결정 3. 약 0.3일. 경로 계산은 정차 중에도 끝난다. 정차하면 최대 한 단계 더 줄고 멈춘다.
   - 2b. 지금처럼 시간만으로 흐른다. 비용 0. 시연자가 바로 기어를 바꿔야 하고, P에서 시간이 주는 장면이 남는다. 2b를 고르면 단계 2를 건너뛴다.
   - 2c. 2b + 시연 모드만 느린 timing.
3. **"시연용 길안내" 표시를 어디에 둘까?** (단계 4 전에 필요)
   - **3a. 정차 화면의 길안내 카드와 도착 화면에 작은 한 줄. 주행 화면은 그대로 (권장).** 설계 결정 4.
   - 3b. 모든 화면 구석에 고정 배지 (주행 화면 포함).
   - 3c. 문서에만. 권장하지 않는다.
   - 하위 질문:
     - 문구 "시연용 길안내예요. 실제 내비게이션과는 아직 연결되지 않았어요."로 괜찮은가?
     - 기획서 반영은 노트의 문장을 기획 담당에게 넘기는 것으로 충분한가, 아니면 이 계획에서 `docs/product/idea.md` "개발 범위" 표에 한 줄을 넣을까? (기본안: 넘긴다)
4. **release를 시연 모드로 고정할까?** (단계 5 전에 필요)
   - **4a. 고정한다. 바꾸는 UI 없음 (권장).** AGENTS.md·architecture.md의 "release는 항상 Real"을 바꾼다. NaviHelper Real 길안내가 생기면 release 기본을 다시 정한다 (예: Real로 돌리거나, 내비 연결이 되면 Real·안 되면 시연).
   - 4b. release에 숨은 전환을 둔다. 권장하지 않는다 (설계 결정 2).
5. **release APK 설치 확인을 어떤 서명으로 할까?** (단계 7 전에 필요)
   - **5a. debug 키로 서명한 확인용 사본 (권장).** 막히는 것이 없다. 팀 정식 키는 `/release-apk` 전에 따로 만든다.
   - 5b. 팀 정식 키스토어를 지금 만든다 (사용자 작업).
   - 5c. 확인하지 않는다.

### [선행: pleos-docs-researcher]

- **없다.** 새로 쓰는 Pleos API가 없다. `FEATURE_AUTOMOTIVE`, `CarUxRestrictions`는 sdk-notes에 있다.

### 실행해서 확인할 것 (문서에 없음)

- Pleos 에뮬레이터에서 한 앱이 `Car` 연결 두 개를 동시에 가져도 두 리스너가 같은 규제 값을 받는지 (확인 절차 D10).
- 서명 없는 release APK를 `adb install`하면 어떤 메시지가 나오는지.
- release 빌드(디버그 불가)에서도 `adb logcat -s UxRestrictions`에 앱 로그가 보이는지. 보이지 않으면 화면과 `dumpsys`로만 확인했다고 적는다.
- Quick boot로 켠 Pleos 에뮬레이터에서는 VHAL을 쓸 수 없다 (emulator-setup "주의"). 이때 시연 모드는 주행 상태를 받지 못해 주행 화면만 보일 수 있다. 확인하지 않는다. README에 Cold boot를 강조하는 것으로 대신한다.

## 확인 절차

`/verify` 규칙대로 에이전트는 에뮬레이터를 직접 띄우지 않는다. 사용자에게 Cold boot를 요청한다. 기기가 둘이면 `ANDROID_SERIAL` 또는 `adb -s`로 고른다.
스크린샷과 UI 트리는 `.claude/.cache/p07-<번호>.png`, `.claude/.cache/p07-<번호>.xml`에 둔다 (커밋하지 않는다).

### 일반 폰 에뮬레이터 `my_avd` (debug, 시연 모드)

준비: debug 패널 [시연으로 전환] → 재시작 → "DEBUG · 시연 (주행 Fake)"

| # | 조작 | 기대 |
|---|---|---|
| P1 | 시작 | 정차 화면, 길안내 카드에 시연 안내 문구와 [병원으로 안내]. `adb logcat -d -s UxRestrictions`에 "FEATURE_AUTOMOTIVE 없음" |
| P2 | [병원으로 안내] | "경로를 찾는 중" → "병원까지 12분" + 시연 문구 + [안내 취소]. 약 3초 뒤 주행 화면 "병원까지 N분" |
| P3 | 기다림 | 주행 "도착했어요" → 약 5초 뒤 도착 화면 (진료 카드 자리표시 + 시연 문구 + [닫기]) → [닫기] → P1 화면 |
| P4 | 주행 중 debug [정차] → 6초 기다림 → [주행] | [정차] 뒤 남은 시간이 최대 한 단계까지 줄고 멈춘다 (2a). [주행] 뒤 다시 준다 |

- 주행 화면 UI 트리 (debug 패널 영역 제외): `android.widget.EditText` 0개, 클릭 가능한 노드 0개.
- 정차 화면(안내 중)과 도착 화면은 가로와 다크 모드에서도 본다. 시연 문구가 잘리거나 겹치지 않는다. 다 보면 설정을 되돌린다.

### Pleos 에뮬레이터 `pleos_connect` (Cold boot, debug, 시연 모드)

준비: debug 패널을 [시연으로 전환]까지 누른다 → "DEBUG · 시연 (주행 Real)". Virtual Console 명령은 006 노트의 `vc()` 함수를 쓴다 (`export MSYS_NO_PATHCONV=1`, `adb forward tcp:8088 tcp:8088`, 요청마다 `/browser/session` 먼저).

| # | 조작 | 기대 |
|---|---|---|
| D1 | `vc "/vehicle/gear?gear=Park"`, 앱 실행 | 정차 화면 + 시연 문구 + [병원으로 안내] |
| D2 | [병원으로 안내] | "병원까지 12분" + [안내 취소] |
| D3 | P인 채로 10초 기다림. 그 사이 두 번 캡처 | 두 번 모두 "병원까지 12분" (2a). 2b면 줄어든다 |
| D4 | `vc "/vehicle/gear?gear=Drive"` (원하면 `controls?speed=30…`) | 주행 화면 "병원까지 N분"이 2초마다 준다. 작은 줄 "시연용 동물병원" |
| D5 | 중간에 `gear=Park` | 정차 화면: 남은 시간 + [안내 취소]. 3초 뒤와 9초 뒤 캡처 값이 같다 |
| D6 | `gear=Drive` | 다시 준다 |
| D7 | 1분 다음 간격까지 기다림 | 주행 "도착했어요" + "정차하면 진료 카드를 볼 수 있어요" |
| D8 | `gear=Park` | 도착 화면 + 시연 문구 + [닫기] |
| D9 | [닫기] | D1 화면 |
| D10 | 로그 | `adb logcat -d -s UxRestrictions`: D2~D8 동안 연결이 두 개(화면, 진행 조건)이고 같은 `DO=` 값을 받는다. D8 도착 뒤에는 진행 조건 쪽 연결이 닫힌다. `SecurityException` 없음 |

- 취소 경로: D2 다음에 P에서 [안내 취소] → D1 화면. 10초 뒤에도 그대로.
- 주행 화면 UI 트리 (debug 패널 영역 제외): `EditText` 0개, 클릭 가능한 노드 0개.
- 다 보면 기어를 P로 돌려 둔다.

### release (결정 5에서 5a 또는 5b일 때)

| # | 환경 | 기대 |
|---|---|---|
| R1 | 둘 다: `adb uninstall com.meongcare.app` → release APK 설치 → 실행 | 크래시 없음. UI 트리에 "DEBUG" 글자 0건 (debug 패널 없음) |
| R2 | `my_avd` | P2~P3 흐름이 손대지 않아도 끝까지 돈다 (주행 상태 Fake + 시나리오) |
| R3 | `pleos_connect` | D1~D9 흐름이 기어 조작대로 돈다 (주행 상태 Real) |

## 단계

단계 하나 = 커밋 1~3개. 커밋마다 `assembleDebug`가 통과해야 한다 (precommit 훅).

**시작 전 준비:**
- 위 `[사용자 승인 필요]` 1~5를 사용자에게 한 번에 확인받는다. 답을 진행 로그에 적는다.
- 미추적 폴더 `.agents/`, `.codex/`는 이 작업과 관계없다. 스테이징하지 않는다. 파일 이름으로만 `git add`한다.
- 사용자에게 알린다: 단계 3 이후 화면 확인과 단계 7에서 `my_avd`, `pleos_connect`를 Cold boot로 켜 달라고 요청한다. 둘을 같이 켜도 된다 (006에서 64GB PC로 함께 띄웠다).

**순서 메모:** 기본 순서(인터페이스·Fake → 도메인·UI → Real)를 따르되, 새 Real SDK 래퍼는 없다. 모드 결정 규칙(순수 함수) → Fake 길안내 확장 → 배선 → UI → release 기본값 순서다. release 기본값은 따로 커밋해서 그것만 되돌릴 수 있게 한다.

### 필수

- [x] 0. 이 계획 파일 커밋 → `docs(plans): 007 release 시연 모드 계획 추가`
  - 확인: `git log -1 --stat`에 이 파일만 있다.

- [x] 1. 모드 결정 규칙을 순수 함수로 분리 (동작 변화 없음) → `refactor(di): 데이터 모드와 소스 구성 결정을 순수 함수로 분리`
  - `di/DataMode.kt`: `resolveDataMode(isDebugBuild, savedName)` (release는 아직 `REAL`)
  - `di/SourcePlan.kt`: `SourceKind`, `SourcePlan`, `sourcePlanOf(mode, isAutomotive)`. 이 단계에는 FAKE·REAL 행만 있다 (설계 결정 1 표의 첫·마지막 행).
  - `di/DataModeStore.kt`: `read()`가 `resolveDataMode`를 부른다.
  - `di/AppContainer.kt`: `when (dataMode)` 세 군데와 `init`의 시나리오 조건을 `sourcePlan` 값으로 바꾼다. `val sourcePlan`을 public으로 둔다.
  - 테스트 (`app/src/test/java/com/meongcare/app/di/`):
    - `DataModeTest`:
      - `` `debug 빌드에서 저장된 값이 없으면 Fake다`() ``
      - `` `debug 빌드는 저장된 모드를 쓴다`() ``
      - `` `debug 빌드에서 모르는 값이 저장돼 있으면 Fake다`() ``
      - `` `release 빌드는 저장된 값과 상관없이 Real이다`() `` (단계 5에서 바꾼다)
    - `SourcePlanTest`:
      - `` `Fake 모드는 주행 상태와 길안내 모두 Fake이고 시나리오를 돌린다`() ``
      - `` `Real 모드는 주행 상태와 길안내 모두 Real이고 Vehicle 연결을 만든다`() ``
      - `` `Fake 길안내를 쓰면 시연용 표시를 한다`() ``, `` `Real 길안내면 시연용 표시를 하지 않는다`() ``
  - 확인: `testDebugUnitTest` 통과 (121 + 새 테스트). 기기가 있으면 debug Fake 시연이 005 그대로인지만 본다.

- [x] 2. Fake 길안내의 진행 조건 (`[사용자 승인 필요]` 2가 **2a**일 때만) → `feat(data): Fake 길안내가 진행 조건이 참일 때만 남은 시간을 줄이게 한다`
  - `data/guidance/FakeGuidanceSource.kt`: 생성자 인자 `progressGate: Flow<Boolean>? = null` (설계 결정 3의 계약). KDoc에 계약과 "Real NaviHelper도 차가 서 있으면 남은 시간이 줄지 않는다"는 근거를 적는다.
  - `FakeGuidanceSourceProgressGateTest` (`runTest` + `backgroundScope`, 진행 조건은 `MutableStateFlow<Boolean>`, 짧은 timing):
    - `` `진행 조건이 거짓이어도 경로 계산은 끝나 처음 남은 시간을 보여준다`() ``
    - `` `진행 조건이 거짓이면 처음 남은 시간에서 더 줄지 않는다`() ``
    - `` `진행 조건이 참이 되면 그때부터 한 간격마다 줄어든다`() ``
    - `` `세는 도중에 거짓이 되면 그 간격까지만 줄고 멈춘다`() ``
    - `` `마지막 간격도 진행 조건이 참일 때만 지나서 도착한다`() ``
    - `` `멈춰 있는 동안 안내를 취소하면 안내 없음으로 돌아간다`() ``
    - `` `안내 한 번에 진행 조건을 한 번만 구독하고 도착하거나 취소하면 구독을 끊는다`() `` (`MutableStateFlow.subscriptionCount`로 센다)
  - `AppContainer`는 아직 넘기지 않는다 (동작 변화 없음).
  - 확인: `FakeGuidanceSourceTest`, `FakeGuidanceSourceBoundaryTest`, 새 테스트 모두 통과. 기존 Fake 테스트 파일은 고치지 않는다.
  - 2b·2c를 골랐으면 이 단계를 건너뛰고 진행 로그에 적는다. 2c면 단계 3에서 DEMO용 `FakeGuidanceTiming`만 넘긴다.

- [x] 3. 시연 모드 추가와 debug 패널 전환
  - 3-1. → `feat(di): 시연(DEMO) 데이터 모드 추가`
    - `DataMode.DEMO` (KDoc: 주행 상태는 차량 기기면 Real·아니면 Fake 시나리오, 길안내는 Fake, 제출 APK용)
    - `sourcePlanOf`에 DEMO 두 행 (설계 결정 1 표). `[사용자 승인 필요]` 1의 답이 1b·1c면 그 답대로 한 행으로 만든다.
    - `AppContainer`:
      - `isAutomotive` 계산
      - `sourcePlan.guidanceWaitsForDriving`이면 `FakeGuidanceSource(applicationScope, progressGate = …)`. Real 주행 상태면 `.flowOn(Dispatchers.Main)` (설계 결정 3)
      - 클래스 KDoc의 "Fake 또는 Real"을 세 모드로 고친다
    - `SourcePlanTest`에 더한다:
      - `` `시연 모드는 차량 기기에서 Real 주행 상태와 Fake 길안내를 쓰고 시나리오를 돌리지 않는다`() ``
      - `` `시연 모드는 차량 기기가 아니면 Fake 주행 상태와 시나리오를 쓴다`() ``
      - `` `시연 모드는 주행 중일 때만 길안내 시간이 흐른다`() `` (2a일 때)
      - `` `시연 모드는 Vehicle 연결을 만들지 않는다`() ``
  - 3-2. → `feat(ui): debug 패널에서 시연 모드로 전환`
    - `DataMode.next()` + `DataModeTest`에 `` `모드 전환은 Fake, 시연, Real 순서로 돈다`() ``
    - `DebugPanel`: `isFakeMode` 대신 `modeLabel: String`, `nextModeLabel: String`, `showsFakeDrivingControls: Boolean` (설계 결정 5). 미리보기도 고친다.
    - `MainActivity`: 라벨 계산과 `AppRestarter.restartWith(this, container.dataMode.next())`
  - 확인:
    - `assembleDebug`, 테스트 통과
    - 기기가 있으면: 확인 절차 P1~P3 (`my_avd`). `pleos_connect`가 켜져 있으면 D1~D4만 먼저 본다. 실패를 일찍 찾기 위해서다. 전체 확인은 단계 7에서 한다.

- [x] 4. 시연용 표시 (`[사용자 승인 필요]` 3의 답대로. 아래는 3a) → `feat(ui): 시연용 길안내 안내 문구 표시`
  - `strings.xml`에 `guidance_simulated_notice`
  - `HomeScreen`에 `isGuidanceSimulated: Boolean` 인자 → `ParkedScreen` → `ParkedGuidanceSection` (안내 없음·안내 중), `ArrivedScreen`
  - `DrivingScreen`은 바꾸지 않는다 (완료 기준 3의 `git diff`)
  - `MainActivity`: `container.sourcePlan.isGuidanceSimulated`를 넘긴다
  - 미리보기에 시연 문구가 있는 경우를 하나씩 더한다
  - 확인: `assembleDebug`. 기기가 있으면 `/verify`로 정차(안내 없음·안내 중)·도착 화면을 세로·가로·다크로 본다.
  - 3b면 `HomeScreen`의 바깥 `Box`에 배지를 둔다. 완료 기준 3에서 `DrivingScreen.kt` 줄을 빼고, 주행 화면 UI 트리에서 배지가 클릭 불가인지 본다.

- [x] 5. release를 시연 모드로 (`[사용자 승인 필요]` 4가 **4a**일 때)
  - 5-1. → `feat(di): release 빌드를 시연 모드로 고정`
    - `resolveDataMode`의 release 분기를 `DEMO`로 바꾼다
    - `DataModeTest`의 release 테스트를 `` `release 빌드는 저장된 값과 상관없이 시연 모드다`() ``로 바꾼다
    - KDoc: `DataMode`(REAL의 "release 빌드는 항상 이 모드다" 삭제, DEMO에 "release 빌드는 이 모드다"), `DataModeStore` 클래스 설명, `AppRestarter`의 debug 전용 설명
    - 커밋 본문: NaviHelper Real이 생기면 release 기본을 다시 정한다. 이 커밋만 되돌리면 이전(항상 Real)으로 돌아간다.
  - 5-2. → `docs: release 데이터 모드 규칙을 시연 모드로 변경`
    - `AGENTS.md` 아키텍처 규칙: "release는 항상 Real이다" → "release는 시연 모드다 (주행 상태는 차량 기기면 Real, 아니면 Fake 시나리오. 길안내는 시연용 Fake). Real 길안내가 생기면 다시 정한다. 자세한 표: architecture.md '데이터 모드'". `AppContainer`가 "Fake나 Real을 끼운다" 문구도 세 모드에 맞춘다. 한두 줄로 둔다 (AGENTS.md는 지도다).
    - `docs/architecture.md`:
      - "결정" 표의 "Fake/Real 전환" 행
      - "데이터 모드" 절: 세 모드, 설계 결정 1의 표, release 고정과 이유, 결정 3(주행 중에만 시간 흐름)
  - 확인: `assembleRelease` 성공. `DataModeTest` 통과.

- [x] 6. 경계 테스트 (`tester`) → `test: 시연 모드 결정과 Fake 길안내 진행 조건 경계 테스트 추가`
  - 메인이 `tester`에게 **이 계획 파일 경로와 대상만** 넘긴다. 구현 설명은 넘기지 않는다.
    - 대상: `resolveDataMode`, `DataMode.next`, `sourcePlanOf`, `FakeGuidanceSource`(진행 조건)
    - 대상 아님: `AppContainer`, `DataModeStore`, `MainActivity`, `DebugPanel` (Android `Context`·Compose 필요)
  - 기대 동작 (계약 기준):
    - 설계 결정 1의 표 전체 (모드 × 차량 기기)
    - 설계 결정 2의 규칙 (debug 기본 FAKE, 모르는 값 FAKE, release DEMO, 전환 순서)
    - 설계 결정 3의 계약 (경로 계산은 조건과 무관, 간격 전 대기, 세던 간격은 끝까지, 도착도 조건 필요, 한 번만 구독, 끝나면 구독 해제, `null`이면 지금과 같음)
    - `GuidanceSource` 구독 계약: 구독하면 현재 상태를 먼저 받고, 같은 상태를 연달아 받지 않는다
  - tester가 찾은 버그는 메인이 프로덕션 코드에서 `fix:` 커밋으로 따로 고친다.
  - 확인: `.\gradlew.bat testDebugUnitTest` 통과. 통과 수와 실패 수를 진행 로그에 적는다.

- [x] 7. 전체 검증 (`/verify`). 커밋은 없다. 고친 게 있으면 `fix(...)` 커밋으로 낸다.
  - `node .claude/hooks/build-gate.mjs pipeline` → "검증 파이프라인 통과."
  - `.\gradlew.bat assembleRelease` 성공
  - 완료 기준 3의 `git diff`, 완료 기준 4의 Grep
  - `my_avd`: 확인 절차 P1~P4
  - `pleos_connect`: 확인 절차 D1~D10과 취소 경로
  - release (결정 5):
    - 서명 없는 APK `adb install` 시도 → 메시지를 진행 로그에 적는다
    - 5a: `apksigner sign`으로 `.claude/.cache/`에 확인용 사본 → `apksigner verify --print-certs` → R1~R3
    - 5b: `keystore.properties`가 있는지(내용은 읽지 않는다) → 서명된 `app-release.apk` → `apksigner verify` → R1~R3
    - 5c: 하지 않았다고 적는다
  - 기기가 없으면 멈추고 사용자에게 Cold boot를 요청한다. 확인하지 못한 것은 그대로 적는다.

- [x] 8. 문서 맞추기
  - `README.md`:
    - "실행 방법": release APK는 시연 모드로 켜진다. Pleos 에뮬레이터는 **Cold boot**로 켠다.
    - "테스트 환경 및 필요 설정":
      - Pleos 에뮬레이터: Virtual Console(`adb forward tcp:8088 tcp:8088` → 브라우저 `http://127.0.0.1:8088/` → Emulation Controls → Motion → Gear Selection)으로 P에서 [병원으로 안내] → D → (남은 시간이 준다) → 도착 → P → 도착 화면. 근거: emulator-setup [vc-overview] [vc-guide]
      - 일반 폰·태블릿 에뮬레이터: [병원으로 안내]만 누르면 출발·도착·주차가 자동으로 이어진다 (약 30초)
      - **길안내는 시연용이다.** Pleos NaviHelper 연동 전이라 정해진 시간대로 남은 시간이 줄고 도착한다. 병원은 가상의 "시연용 동물병원"이다. 주행/정차는 차량 기기에서 실제 주행 규제 값을 따른다
      - 확인한 환경: Pleos Connect 에뮬레이터 (API 34), 일반 폰 에뮬레이터 (API 34). 그 밖의 환경은 확인하지 않았다
    - "소스에서 빌드하기" 4번: debug 패널에서 Fake → 시연 → Real 순으로 바뀐다. "release 빌드는 항상 Real 모드다"를 "release 빌드는 시연 모드다"로.
  - `docs/architecture.md`:
    - "패키지 구조": `di/DataMode.kt`(FAKE / DEMO / REAL, `resolveDataMode`, `next`), `di/SourcePlan.kt`, `DataModeStore.kt` 설명("release는 항상 REAL" → 시연)
    - "Car(AAOS) 연결 수명주기": 시연 모드 안내 중에는 연결이 둘(화면, 길안내 진행 조건)이 된다. 묶지 않은 이유(설계 결정 3)와 D10 관찰 결과
    - 문의 A2 행의 "'안 된다'면" 칸: 시연 모드로 대비했다고 적는다
  - `docs/pleos/emulator-setup.md`: D1~D9를 하면서 Virtual Console 요청이 문서와 다르게 동작했으면 고친다
  - 커밋:
    - `docs: README에 시연 모드 실행 방법과 시연용 길안내 안내 추가`
    - `docs(architecture): 시연 모드 패키지와 Car 연결 두 개 기록`
  - 확인: 완료 기준 7

- [x] 9. 개발 노트 작성 (`docs/notes/`)
  - `docs/notes/xxj15/YYYY-MM-DD-release-demo-mode.md`. 형식은 `docs/notes/README.md` 템플릿을 따른다.
    - 사용한 API: 새 Pleos SDK 없음, 새 권한 없음. 006의 `CarUxRestrictions`를 시연 모드에서도 쓴다.
    - 결정: 세 모드와 구성표, 기기에 따라 주행 상태 출처를 고른 이유, 주행 중에만 시간이 흐르게 한 이유, 시연 표시 위치, release 고정
    - 시연 방법 (영상 촬영용): release APK + Pleos 에뮬레이터 + Virtual Console 순서 (확인 절차 D1~D9), 일반 폰 에뮬레이터 순서. `adb shell screenrecord` 방법
    - **기획서 5번 "개발 계획"에 넣을 문장** (기획 담당에게 넘긴다). 예: "제출 APK는 시연 모드로 동작한다. 주행/정차는 차량의 주행 규제 값(AAOS CarUxRestrictions)을 실제로 따르고, 병원 길안내는 Pleos NaviHelper 연동 전이라 시연용 시뮬레이션이다."
    - 남은 일:
      - Real 길안내 (NaviHelper). 생기면 release 기본 모드를 다시 정한다
      - 팀 정식 키스토어와 팀명·앱 이름 → `/release-apk` (사용자 실행)
      - 결정 5에서 5a였다면: 확인용 사본은 debug 키로 서명했다. 제출용이 아니다
      - 주행 상태 `shareIn` (구독자가 더 늘면)
      - 정차 화면 메모 입력칸 자리표시 (있으면 좋음 B를 하지 않았다면)
  - 이 계획을 `docs/plans/completed/`로 옮기고 진행 로그를 갱신한다.
  - 커밋:
    - `docs(notes): release-demo-mode`
    - `docs(plans): 007 release 시연 모드 완료`
  - 확인: `git status`에 이 작업의 변경이 남지 않았다 (`.agents/`, `.codex/` 제외). 그다음 `/ship`.

### 있으면 좋음 (필수가 끝난 뒤, 단계 9 전에)

- [ ] A. 시연 모드 전용 시간 값 → `feat(di): 시연 모드 길안내 시간 값 조정`
  - 단계 7이나 영상 촬영에서 2초 간격이 너무 빠르거나 느리면 한다. DEMO일 때만 `FakeGuidanceTiming`을 다르게 넘긴다 (FAKE 기본값과 그 테스트는 그대로).
  - 확인: `SourcePlanTest` 또는 `AppContainer` 쪽 값 확인, 확인 절차 D3~D7 다시
- [ ] B. 정차 화면의 메모 입력칸 자리표시 정리 → `refactor(ui): 정차 화면 메모 자리표시 제거`
  - release APK를 보는 심사위원에게 의미 없는 "메모 (정차 중에만 입력)" 칸이 보인다 (005 노트 "남은 일").
  - 지울지, 다른 정차 기능 자리로 둘지는 기획 결정이다. 사용자에게 먼저 묻는다.
  - 확인: `/verify`로 정차 화면 세로·가로

## 진행 로그

- 2026-11-05: 계획 작성.
  - 브랜치 `feat/release-demo-mode`는 dev(`cf27ead`, 006 반영 뒤)에서 땄다.
  - 시작 시점의 코드:
    - `DataMode`는 FAKE·REAL 둘이다. `DataModeStore.read()`는 release면 REAL이다.
    - `AppContainer`가 `when (dataMode)`로 고른다. FAKE면 `FakeTripScenario`를 돌린다.
    - `FakeGuidanceSource`는 시간으로만 흐른다 (약 25초에 도착).
    - debug 패널은 Fake↔Real 토글이다.
    - 단위 테스트 121개.
    - `keystore.properties` 없음 → release는 unsigned APK.

- 2026-11-05: 단계 0~5 완료 (`539b7ea`, `f5ea3eb`, `93d51a2`, `a98a760`, `aeab752`, `5938d57`, `a9334b5`, `4ac09df`, `68eaf5c`).
  - 단계 1: `DataModeTest` 4, `SourcePlanTest` 4 → 전체 129 통과.
  - 단계 2: `FakeGuidanceSourceProgressGateTest` 7 통과. 기존 Fake 길안내·시나리오 테스트 그대로 통과.
  - 단계 3: `SourcePlanTest` DEMO 4개 더함, `DataModeTest` 2개 더함 → 142 통과.
    - Pleos 에뮬레이터 빠른 확인 (debug 시연, "DEBUG · 시연 (주행 Real)"): D1 정차 화면, D2 [병원으로 안내] → "병원까지 12분", **D3 P인 채로 8초 → 12분 그대로**, D4 기어 D → 3초 뒤 10분, 7초 뒤 7분. 로그에 차량 연결 두 개(화면, 진행 조건)가 같은 순간 같은 `DO=true active=0x50`을 받았다. 이어서 D에서 도착하고 P로 바꾸자 도착 화면이 떴다.
  - 단계 4: Pleos에서 정차(안내 중) 카드 아래 시연 문구 확인. `DrivingScreen.kt`의 `git diff dev` 0줄.
  - 단계 5: release → DEMO. `4ac09df`는 단계 3-2에서 `next()`를 `resolveDataMode`의 KDoc과 함수 사이에 넣어 설명이 엉뚱하게 붙은 것을 바로잡은 커밋이다.

- 2026-11-05: 단계 6~9 완료.
  - 단계 6 (`89add10`): tester가 경계 테스트 42개를 더했다 (`DataModeBoundaryTest` 8, `SourcePlanBoundaryTest` 11, `FakeGuidanceSourceProgressGateBoundaryTest` 15, `FakeGuidanceSourceDemoModeTest` 8). 184개 모두 통과, 계약 위반 버그 없음.
    - **tester가 짚은 계약 충돌**: "세던 간격은 끝까지 센다"와 "도착은 조건이 참일 때만"이 마지막 간격에서 부딪혔다. 구현은 정차 중에도 도착했다.
    - 고침 (`58a7cd9`): 도착 쪽을 우선한다. 마지막 간격을 다 센 뒤에도 조건이 거짓이면 다시 참이 될 때까지 도착을 미룬다. 테스트 1개 추가 → 185개 통과.
  - 단계 7:
    - 검증 파이프라인 통과, `assembleRelease` 성공.
    - Pleos 에뮬레이터 (debug 시연, "DEBUG · 시연 (주행 Real)"): D1 정차+시연 문구, D2 "병원까지 12분", **D3 P로 10초 동안 12분 그대로**, D4 D·30km/h → 10분 → 5분 (주행 화면 `EditText` 0), **D5 P → 3초 뒤·9초 뒤 모두 4분**, D6 D → 2분, D7 주행 "도착했어요", D8 P → 도착 화면+시연 문구, D9 [닫기] → D1. 취소 경로: P에서 시작 → 취소 → 10초 뒤에도 처음 화면.
    - D10 로그: 화면 연결과 진행 조건 연결이 같은 순간 같은 값을 받았다 (`DO=false` 두 번, `DO=true` 두 번 …). 도착(D7) 순간 `주행 규제 연결을 닫았다`(진행 조건 쪽), 그 뒤로는 한 줄씩. 취소해도 진행 조건 쪽 연결이 닫혔다. `SecurityException`·크래시 없음.
    - 일반 폰 에뮬레이터 (debug 시연, "DEBUG · 시연 (주행 Fake)"): P1 정차+시연 문구. 계획은 "FEATURE_AUTOMOTIVE 없음" 로그를 기대했지만, 이 구성은 주행 상태가 처음부터 Fake라 차량 연결 코드를 부르지 않아 로그가 없다 (설계대로). P2 안내 → 약 3초 뒤 주행, 그때부터 감소. P4 주행 중 [정차] → 8분에서 멈춤(6초 뒤도 같음) → [주행] → 다시 감소. P3 도착 → 도착 화면+시연 문구 → [닫기]. 가로·다크: 정차(안내 중) 화면은 debug 패널 때문에 시연 문구와 [안내 취소]가 아래로 밀린다(005의 알려진 한계, release에는 패널 없음). 도착 화면은 가로·다크에서 문구까지 보인다.
    - release: 서명 없는 APK 설치 → `INSTALL_PARSE_FAILED_NO_CERTIFICATES`. 결정 5a대로 `zipalign` + `apksigner`(기존 debug 키)로 `.claude/.cache/release-check-debugsigned.apk`를 만들었다 (git 제외, 제출용 아님). `apksigner verify`: CN=Android Debug.
      - R1: 두 에뮬레이터 모두 `adb uninstall` 후 설치 → 실행 → 크래시 없음, UI 트리 "DEBUG" 0건, 처음부터 시연 모드.
      - R2 (일반 폰): [병원으로 안내]만 눌러 출발 → 도착 → 도착 화면 → [닫기]까지 저절로.
      - R3 (Pleos): P에서 12분 유지 → D로 감소 → 도착 → P → 도착 화면 → [닫기].
      - 고친 경우(마지막 간격 도중 정차)는 기기에서 재현하지 못했다. 간격이 2초라 화면을 읽고 기어를 바꾸는 사이 이미 도착했다. JVM 테스트로만 확인했다.
    - adb 데몬이 두 번 끊겼다 (`cannot connect to daemon`). 다시 실행해서 이어 갔다.
  - 단계 8 (`23c014b`, `a9ae36f`): README "실행 방법"·"테스트 환경"(두 에뮬레이터 시연 방법, 시연용 길안내 안내), architecture.md(패키지 구조, Car 연결 두 개, 문의 A2 행).
  - 단계 9: 노트 `docs/notes/xxj15/2026-11-05-release-demo-mode.md`. 있으면 좋음 A·B는 하지 않았다.

## 결정 / 배운 점

- **도착은 주행 중에만** (`58a7cd9`): 설계 결정 3의 "세던 간격은 끝까지 센다"를 마지막 간격에서는 "다 세되 도착은 미룬다"로 좁혔다.
- 일반 폰 시연 구성은 주행 상태가 Fake라 `uxRestrictionsClientOrNull`을 부르지 않는다. 그래서 "FEATURE_AUTOMOTIVE 없음" 로그가 없다.
- 시연 문구가 휴대폰 세로 화면에서 "아/직"처럼 낱말 중간에서 줄이 바뀐다 (한국어 글자 단위 줄바꿈). 고치지 않았다.


- `DataMode.next()`를 넣을 때 `resolveDataMode`의 KDoc과 함수 사이에 끼워 넣는 실수를 했다 (KDoc이 `next()`에 붙음). 별도 `refactor` 커밋으로 바로잡았다.
- debug 패널의 다음 모드 라벨은 받침에 맞춰 "시연으로 전환" / "Real로 전환" / "Fake로 전환"이다.

- 2026-11-05: 사용자 결정 (`[사용자 승인 필요]` 1~5 모두 권장안).
  - 1 → **1a**: 시연 모드의 주행 상태는 차량 기기면 Real, 아니면 Fake + 시나리오.
  - 2 → **2a**: 주행 중일 때만 남은 시간이 준다 (단계 2를 한다).
  - 3 → **3a**: 정차 화면 길안내 카드와 도착 화면에 시연 안내 한 줄. 주행 화면은 그대로. 기획서 문장은 노트로 기획 담당에게 넘긴다.
  - 4 → **4a**: release는 시연 모드로 고정, 전환 UI 없음.
  - 5 → **5a**: release 설치 확인은 debug 키로 서명한 확인용 사본(`.claude/.cache/`, 커밋 안 함). 팀 키스토어는 제출 준비 때 따로.
