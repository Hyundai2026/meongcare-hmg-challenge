# 004 Android 프로젝트 초기 설정 (뼈대)

- 목표: 앱 아이디어와 무관한 Compose 앱 뼈대를 만들어, Fake 주행 상태에 따라 주행/정차 화면이 바뀌고 빌드·테스트·린트·훅·CI·화면 확인이 실제로 도는 상태로 만든다.
- 완료 기준 (어떻게 검증하나):
  1. `node .claude/hooks/build-gate.mjs pipeline`이 "검증 파이프라인 통과."를 출력한다 (= `assembleDebug testDebugUnitTest lintDebug`).
  2. `secrets.properties`·`keystore.properties` 없이 `.\gradlew.bat assembleDebug assembleRelease`가 성공하고 `app/build/outputs/apk/release/app-release-unsigned.apk`가 생긴다.
     - 이 PC에 두 파일이 이미 있으면 내용은 보지 않고 "있어서 미확인"으로 기록한다.
  3. 단위 테스트 `FakeDrivingStateSourceTest`, `HomeViewModelTest`와 tester가 추가한 테스트가 통과한다.
  4. `/verify` (일반 AVD `my_avd`, debug 빌드, Fake 모드):
     - 앱을 실행하면 정차 화면이 나온다.
     - debug 패널에서 [주행]을 누르면 주행 화면으로 바뀐다. 상태 한 줄(큰 글씨), 큰 버튼 1개 이하, 텍스트 입력 없음, 전환 애니메이션 없음.
     - [정차]를 누르면 정차 화면으로 돌아온다.
     - `adb logcat -d -t 300 *:E`에 `com.meongcare.app` 크래시가 없다.
  5. 정적 검사 (Grep):
     - `app/src/main/java/com/meongcare/app/ui/home/DrivingScreen.kt`에 `TextField` 0건
     - `import ai.pleos`, `import android.car`가 `data/` 밖에 0건
     - `ui/` 아래에 `import com.meongcare.app.data` 0건
     - 병합 매니페스트(`app/build/intermediates/` 아래 `AndroidManifest.xml`)에 `pleos.car.permission` 0건
  6. 훅: 앱 코드 커밋 때 precommit 훅이 `assembleDebug`를 돌렸고, `.claude/.cache/build-gate.json`에 `okDigest`와 `pipelineDigest`가 기록돼 있다.
  7. 문서: AGENTS.md에 "compile/target SDK 34"와 "아직 미작성" 문구가 없다. README "소스에서 빌드하기"가 JDK 17+·SDK 35 기준이다. `docs/architecture.md`가 있다.
  8. (/ship 뒤) dev push로 실행된 GitHub Actions "Android CI"가 성공한다. 이 저장소에서 gradlew가 있는 첫 CI 실행이다.
- 브랜치: `chore/project-setup`
  - dev에서 땄다. Pleos 조사 기록 커밋 `fa090e3`이 먼저 올라가 있고, /ship 때 같이 dev로 들어간다.
- 관련 문서: AGENTS.md, docs/pleos/sdk-notes.md, docs/pleos/ux-guidelines.md, docs/pleos/emulator-setup.md, docs/conventions/git.md, .claude/hooks/build-gate.mjs, .github/workflows/android.yml, .claude/skills/verify/SKILL.md

## 범위

- **필수** (반나절~하루): 단계 0~13. 단계 9는 선행 조사 결과에 따라 진행하거나 후속 계획으로 넘긴다.
- **있으면 좋음**: 단계 A~C. 필수가 끝나고 시간이 남으면 한다.
- **이번 범위 밖** (후속 계획 후보):
  - `CarUxRestrictions`로 실제 주행 상태 구현 (005 후보). 이유:
    - `android.car` Gradle 설정이 sdk-notes에 "미확인"이다.
    - 이 PC에는 Pleos 에뮬레이터가 없어 동작을 확인할 수 없다. 확인 못 한 걸 완료라고 할 수 없다 (AGENTS 규칙 6).
  - DO(`distractionOptimized`) 선언: 기획 결정 뒤에 한다 (아래 "확인 필요" 7).
  - Pleos 에뮬레이터에서 Real 모드 확인 (GPU PC 필요)
  - 정식 앱 아이콘, 팀명·앱 이름 확정, 제출용 APK(`/release-apk`), R8(minify)

## 결정 (사용자 확인, 2026-11-03)

다시 묻지 않는다.

| 항목 | 결정 | 근거 |
|---|---|---|
| UI | Jetpack Compose + Material3 | 공식 Pleos 샘플 3개가 모두 Compose. 팀이 웹(React) 위주라 Compose가 친숙 |
| 빌드 버전 | AGP 8.8.2, Kotlin 2.0.0 (+ `org.jetbrains.kotlin.plugin.compose`), Gradle wrapper 8.10.2, JDK 17 (source/target 17), compileSdk/targetSdk 35, minSdk 28 | 공식 vehicle 샘플과 같은 값 (sdk-notes "공식 샘플 앱의 빌드 값"). minSdk 28은 Pleos 요구 |
| ID | applicationId·namespace `com.meongcare.app`, 임시 라벨 "멍케어" | |
| 구조 | 단일 `app` 모듈, 패키지 `data/` `domain/` `ui/` `di/`, MVVM (ViewModel + StateFlow) | |
| DI | 수동 DI. Application이 `AppContainer`를 가진다 | |
| Fake↔Real | debug 빌드에서만 보이는 앱 안 토글. release는 항상 Real. **빌드 flavor는 쓰지 않는다** | 훅과 CI가 `assembleDebug`/`testDebugUnitTest`/`lintDebug` 태스크 이름에 의존 |
| 테스트 | JUnit4 + kotlinx-coroutines-test + Turbine | |
| 비밀값 | `secrets.properties` → BuildConfig. 파일이 없으면 빈 값으로 빌드 성공 | AGENTS 아키텍처 규칙 |
| release 서명 | `keystore.properties`가 있으면 서명, 없으면 서명 없이 빌드 성공 | |
| 권한 | 새 `pleos.car.permission.*`을 선언하지 않는다 | AGENTS 규칙 2 |

## 설계 (planner 기본안: 구현하면서 바꾸면 "결정 / 배운 점"에 이유를 남긴다)

### 패키지

```
app/src/main/java/com/meongcare/app/
  MeongCareApplication.kt     AppContainer 생성. REAL 모드일 때만 VehicleConnection 등록 (단계 9)
  MainActivity.kt             Compose 호스트. container에서 ViewModel·debug 패널을 연결
  di/
    AppContainer.kt           모드에 맞는 구현을 골라 들고 있다
    DataMode.kt               FAKE / REAL
    DataModeStore.kt          SharedPreferences. debug에서만 읽는다 (기본 FAKE). release는 항상 REAL
    AppRestarter.kt           모드를 바꾼 뒤 앱 재시작 (debug)
  domain/driving/
    DrivingState.kt           PARKED / DRIVING / UNKNOWN
    DrivingStateSource.kt     interface { val drivingState: Flow<DrivingState> }
  data/driving/
    FakeDrivingStateSource.kt MutableStateFlow(PARKED) + set(state)
    RealDrivingStateSource.kt 자리표시. UNKNOWN만 내보낸다 (CarUxRestrictions는 후속 계획)
  data/vehicle/
    VehicleConnection.kt      Vehicle initialize/release를 한 곳에서 (단계 9)
  ui/theme/                   Color.kt, Type.kt, Dimens.kt, Theme.kt
  ui/home/                    HomeUiState.kt, HomeViewModel.kt, HomeScreen.kt, ParkedScreen.kt, DrivingScreen.kt
  ui/debug/                   DebugPanel.kt (상태 없는 composable. 값과 콜백은 MainActivity가 넘긴다)
app/src/test/java/com/meongcare/app/
  testutil/MainDispatcherRule.kt
  data/driving/FakeDrivingStateSourceTest.kt
  ui/home/HomeViewModelTest.kt
```

### 의존 방향

- `ui` → `domain` 인터페이스만 쓴다. `ui`는 `data`를 import하지 않는다.
- `data` → `domain`을 구현한다.
- `di`는 전부 알고 연결만 한다.
- Pleos SDK(`ai.pleos.*`)와 AAOS Car API(`android.car.*`)는 `data/` 아래에서만 import한다.

### 데이터 모드 (Fake / Real)

- 모드는 프로세스가 시작될 때 한 번 정한다.
  - debug: `DataModeStore`의 값 (기본 FAKE)
  - release: 항상 REAL
- debug 패널에서 모드를 바꾸면 저장한 뒤 앱을 재시작한다.
  - 재시작 방법: launch intent에 `FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK`를 붙여 띄우고 `Runtime.getRuntime().exit(0)`.
  - 실행 중에 바꾸지 않는 이유: Vehicle initialize/release를 Application 한 곳에서 단순하게 유지하고, 데이터 소스 교체 중에 상태가 꼬이지 않게 하기 위해서다.
- FAKE 모드에서는 `Vehicle`을 만들지 않는다. 일반 AVD에는 Vehicle Service가 없다.

### 주행 상태 → 화면

- `HomeViewModel`이 `DrivingStateSource.drivingState`를 `StateFlow<HomeUiState>`로 바꾼다 (`stateIn(viewModelScope, WhileSubscribed(5_000), ...)`).
  - PARKED → `Parked`
  - DRIVING → `Driving`
  - UNKNOWN → `Driving` (안전 우선. "확인 필요" 8에서 다시 결정할 수 있음)
  - 초기값: `Driving`
- 주행 화면 (`DrivingScreen.kt`, AGENTS 규칙 3과 ux-guidelines "주행 안전 설계 원칙"):
  - 상태 한 줄("주행 중")을 큰 글씨로 표시한다.
  - 큰 버튼은 1개 이하로 둔다.
  - 텍스트 입력, 스크롤, 애니메이션을 넣지 않는다.
- 정차 화면 (`ParkedScreen.kt`):
  - 상태 한 줄("정차 중")
  - 예시 기능 버튼 2개
  - 텍스트 입력 예시 1개 (정차 중에만 허용되는 기능의 예)
- 두 화면은 애니메이션 없이 바로 바꾼다. `AnimatedContent`나 `Crossfade`를 쓰지 않는다.
- debug 패널은 두 화면 모두 아래쪽 작은 띠로 보인다.
  - 주행 화면에서도 Fake를 정차로 되돌릴 수 있어야 하기 때문이다.
  - debug 전용이라 규칙 3의 예외로 둔다. 개발 노트에 적는다.

### 테마와 UI 수치

- 시스템 다크 모드를 따른다 (`isSystemInDarkTheme()`). dynamic color는 쓰지 않는다.
- Pleos 확인값 (ux-guidelines):
  - 대비 4.5:1 이상
  - 최소 폰트 12sp
  - 주요 정보 글자 높이 5mm 이상
- 터치 타깃과 타입 스케일의 공식 수치는 미확인이다. 그래서 임시값을 `ui/theme/Dimens.kt`와 `Type.kt` 한 곳에 모으고 `// 임시값: Pleos 공식 수치 미확인` 주석을 단다.
  - 예시 임시값 (근거 없음, 바꿔도 됨): 본문 22sp, 제목 30sp, 주행 화면 상태 글자 48sp, 일반 터치 타깃 최소 64dp, 주행 화면 주 버튼 높이 96dp
- 폰트는 시스템 기본을 쓴다. Pleos에는 Asta Sans가 System UI 폰트로 깔려 있다.
- `screenOrientation`은 지정하지 않는다 (Pleos 기본: portrait, window 1개).
- targetSdk 35라 Android 15 기기에서는 edge-to-edge가 강제된다. `enableEdgeToEdge()`와 `Scaffold` 인셋을 쓴다.
- 텍스트 입력이 있는 정차 화면을 위해 `windowSoftInputMode="adjustResize"`를 둔다 (ux-guidelines "텍스트 입력과 IME").

## 확인 필요 (시작 전에 해결)

### [선행: pleos-docs-researcher]: 단계 9 앞에서 필요

1. `Vehicle` 클래스의 패키지(import 경로)는 무엇인가?
   - `initialize()`와 `release()`의 시그니처(반환값, 던지는 예외, 동기/비동기)는?
   - 공식 vehicle 샘플(`pleos-sample-vehicle-app`)은 `Vehicle`을 어디서 만들고, initialize/release를 어디서 부르나? (Application / Activity / ViewModel)
   - 문서에 없으면 받은 AAR의 클래스 목록이나 샘플 소스로 확인해도 된다. 추측이 아니라 산출물에서 확인하는 것이다. 확인 방법과 출처를 sdk-notes에 적는다.
2. `release()` 뒤에 같은 인스턴스로 `initialize()`를 다시 불러도 되나, 아니면 새 인스턴스를 만들어야 하나?
   - 기본안은 매번 새 인스턴스를 만드는 것이다.
3. Vehicle Service가 없는 기기(일반 폰 AVD)에서 `Vehicle(context)`와 `initialize()`는 어떻게 되나? (예외 / 무시 / 나중에 `onFailed`)
   - 문서에 없으면 있으면 좋음 C로 실험해서 기록한다.
4. 도메인 API 없이 `initialize()`만 부를 때 매니페스트에 필요한 것(권한, `<queries>`)이 있나?
   - 필요한 게 `pleos.car.permission.*`이면 아래 6으로 간다.

### [선행: pleos-docs-researcher]: 이번 범위 밖, 후속 계획(005) 앞에서 필요

5. `android.car`를 쓰기 위한 Gradle 설정은? (`useLibrary("android.car")` 여부)
   - `Car.createCar` → `CarUxRestrictionsManager`를 얻는 방법과 필요한 권한
   - Pleos 에뮬레이터 Virtual Console에서 기어를 바꾸면 `CarUxRestrictions`가 바뀌나? (ux-guidelines 미확인 항목)

### [사용자 승인 필요]

6. 지금은 승인을 요청할 권한이 없다. 이번 범위에서는 `pleos.car.permission.*`을 하나도 선언하지 않는다.
   - 단계 9에서 Vehicle AAR이 권한을 병합 매니페스트에 넣으면 거기서 멈추고 사용자에게 승인을 받는다.

### 기획 결정 필요

7. **DO(`distractionOptimized`) 선언 여부.** 앱 화면이 주행 중에도 보여야 하는 앱인가?
   - DO를 선언하지 않은 앱은 Pleos에서 Driving 상태일 때 쓸 수 없다 (ux-guidelines "주행 규제").
   - 이번 뼈대는 선언하지 않는다. 주행 화면은 Fake로 보여주는 데모용이다.
   - 앱 아이디어가 정해지면 결정한다.
8. **주행 상태를 모를 때(UNKNOWN) 어느 화면을 보여줄까?**
   - 기본안: 주행 화면(제한 쪽, 안전 우선)
   - 영향: release는 항상 Real이고, Real은 CarUxRestrictions를 구현하기 전까지 UNKNOWN만 낸다. 그래서 그때까지 **release APK는 항상 주행 화면**이다.
   - 11/15 APK 제출 전에 둘 중 하나를 정해야 한다.
     - 005(CarUxRestrictions 구현)를 먼저 한다.
     - DO를 선언하지 않는다면 플랫폼이 주행 중에는 앱을 막으므로, "UNKNOWN = 정차"로 바꾼다.
9. "멍케어"는 임시 라벨이다. AGENTS.md의 `<팀명>`과 `<앱이름>`은 이번에 채우지 않는다. 확정되면 `/release-apk` 전에 채운다.

### 구현 중 확인 (선행 아님)

10. CI(ubuntu-latest) 첫 실행에서 확인한다.
    - Pleos Maven(`nexus-playground.pleos.ai`)에 접근되는가
    - android-35 플랫폼이 없으면 자동으로 설치되는가
    - 실패하면 `ci:` 커밋으로 고친다.
11. Vehicle SDK의 R8/ProGuard 규칙은 미확인이다. 그래서 release는 `isMinifyEnabled = false`로 둔다.
12. kotlinx-coroutines-test와 Turbine 버전은 Kotlin 2.0.0에서 컴파일되는 버전으로 고정한다.
    - coroutines-test는 의존성 트리의 `kotlinx-coroutines-core` 버전에 맞춘다.
    - 트리는 `.\gradlew.bat :app:dependencies --configuration debugRuntimeClasspath`로 본다.

## 단계

단계 하나 = 커밋 1~3개. 커밋마다 빌드가 통과해야 한다 (precommit 훅이 확인).

**시작 전 준비:**
- `C:\Android\platforms\android-35`가 있는지 확인한다. 메인이 설치 중이었다. 없으면 멈추고 보고한다.
- 사용자에게 `my_avd`를 **Cold boot**로 켜 달라고 요청한다.
  - git.md 규칙: UI 단계(3, 6, 7)는 커밋 전에 `/verify`로 본다.
  - 기기가 없으면 빌드만 확인하고 커밋한다. 화면은 단계 11에서 몰아서 확인하고, 진행 로그에 적는다.

### 필수

- [ ] 0. 이 계획 파일 커밋 → `docs(plans): 004 프로젝트 초기 설정 계획 추가`
  - 확인: `git log -1 --stat`에 이 파일만 있다.

- [ ] 1. 문서 먼저 고치기 (AGENTS: "실제와 다르면 코드보다 문서를 먼저 고친다")
  - AGENTS.md 기술 스택을 위 "결정" 표 값으로 바꾼다.
    - compile/target SDK 35, AGP 8.8.2, Kotlin 2.0.0 + compose 플러그인, Gradle wrapper 8.10.2 (Pleos 요구는 8.0 이상), JDK 17 (빌드 JDK는 17 이상), Compose + Material3, MVVM, 수동 DI
    - 자세한 내용은 `docs/architecture.md`로 링크한다.
  - AGENTS.md 규칙 3의 "(… 아직 미작성)"을 `docs/pleos/ux-guidelines.md` 링크로 바꾼다.
  - AGENTS.md 디렉토리 표에서 `app/`의 "(Android Studio로 생성 예정)"을 바꾼다.
  - `docs/architecture.md`를 새로 만든다. 들어갈 것:
    - 결정 표 (결정 / 근거 / 버린 대안)
    - 위 "설계"의 패키지, 의존 방향, 데이터 모드, Vehicle 수명주기 방침, 테스트 전략
    - 실제 클래스 이름은 단계 12에서 맞춘다.
    - 버린 대안 예: XML Views, Hilt(공식 navi 샘플이 kapt로 씀), build flavor, 멀티 모듈
  - 커밋:
    - `docs: AGENTS 기술 스택을 SDK 35 기준으로 갱신`
    - `docs(architecture): 기술 스택·구조 결정 기록 추가`
  - 확인: AGENTS.md에서 Grep `compile/target SDK 34`, `아직 미작성` 각각 0건.

- [ ] 2. Gradle 뼈대와 빈 Compose 화면 (빌드되는 최소 앱)
  - **wrapper:** (메인이 바꿈. 아래 "결정 / 배운 점" 참고)
    - 공식 Gradle 8.10.2 배포판(SHA-256 검증함)의 `gradle wrapper --gradle-version 8.10.2`로 만든다.
    - 공식 샘플의 wrapper jar는 Gradle 8.10.2 공식 체크섬과 다르다. 그래서 가져오지 않는다.
  - **`.gitattributes`:**
    - `gradlew text eol=lf`, `*.bat text eol=crlf`, `*.jar binary`
    - Windows에서 커밋해도 Linux CI에서 `gradlew`가 깨지지 않게 하기 위해서다.
  - **`settings.gradle.kts`:**
    - `pluginManagement`: google, mavenCentral, gradlePluginPortal
    - `dependencyResolutionManagement`: sdk-notes의 블록 그대로 (Pleos Maven 포함, `FAIL_ON_PROJECT_REPOS`)
    - `include(":app")`
  - **루트 `build.gradle.kts`:** 플러그인 3개를 `apply false`로 둔다.
  - **`gradle/libs.versions.toml`:**
    - 샘플 이름과 값을 따른다: agp 8.8.2, kotlin 2.0.0, coreKtx 1.16.0, lifecycle 2.9.0, activityCompose 1.10.1, composeBom 2024.04.01
    - **이 커밋에서 쓰는 것만** 넣는다 (git.md 의존성 규칙).
  - **`gradle.properties`:** `org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8`, `android.useAndroidX=true`, `android.nonTransitiveRClass=true`, `kotlin.code.style=official`
  - **`app/build.gradle.kts`:**
    - 플러그인: 샘플과 같은 3개
    - namespace·applicationId `com.meongcare.app`
    - compileSdk 35, targetSdk 35, minSdk 28
    - versionCode 1, versionName `0.1.0`
    - Java 17 (`compileOptions`), Kotlin `jvmTarget = "17"`. toolchain은 쓰지 않는다. 이 PC에는 JDK 21만 있기 때문이다.
    - `buildFeatures { compose = true; buildConfig = true }`
    - release: `isMinifyEnabled = false`
  - **`AndroidManifest.xml`:**
    - application: `android:label="@string/app_name"`("멍케어"), icon, theme
    - `MainActivity`: `exported="true"`, MAIN/LAUNCHER
    - 권한 없음. `screenOrientation`과 DO meta-data도 넣지 않는다.
  - **리소스:**
    - `strings.xml`
    - `themes.xml`: parent `android:Theme.Material.Light.NoActionBar`. Material Components 라이브러리를 쓰지 않는다.
    - 임시 adaptive icon (vector)
  - **`MainActivity`:** `ComponentActivity` + `enableEdgeToEdge()` + `setContent { Text("멍케어") }`
  - **주의:**
    - **gradlew가 생기는 순간부터 build-gate가 앱 코드 커밋과 턴 종료 때마다 `assembleDebug`를 돌린다.**
      - 그래서 wrapper만 먼저 커밋하지 않는다. 빌드되는 최소 앱과 한 커밋으로 낸다.
    - 첫 빌드는 Gradle 배포판과 의존성을 받느라 오래 걸린다.
      - precommit 훅의 빌드 제한 시간은 570초다.
      - **커밋 전에 `.\gradlew.bat assembleDebug`를 직접 한 번 돌려 캐시를 채운다.**
    - `local.properties`는 만들지 않아도 된다. `ANDROID_HOME=C:\Android`가 설정돼 있다. 만들더라도 gitignore 대상이다.
  - 커밋: `build: Android 프로젝트 뼈대 추가 (Compose 빈 화면)`
  - 확인:
    - `.\gradlew.bat assembleDebug testDebugUnitTest lintDebug`가 BUILD SUCCESSFUL이다. 테스트는 아직 없어서 NO-SOURCE가 정상이다.
    - `app/build/outputs/apk/debug/app-debug.apk`가 있다.
    - 커밋 뒤 `.claude/.cache/build-gate.json`의 `okDigest`가 갱신됐다.

- [ ] 3. 테마 → `feat(ui): 라이트·다크 테마와 큰 글씨·터치 기본값 추가`
  - `ui/theme/` 4개 파일. 위 "테마와 UI 수치"를 따른다.
  - `MainActivity`를 `MeongCareTheme`으로 감싼다.
  - 확인: `assembleDebug`. 기기가 있으면 `/verify`로 라이트·다크 둘 다 본다 (`adb shell cmd uimode night yes`/`no`).

- [ ] 4. 주행 상태 인터페이스, Fake, 테스트 기반 → `feat(domain): 주행 상태 인터페이스와 Fake 구현 추가`
  - `domain/driving/DrivingState.kt`, `DrivingStateSource.kt`
    - KDoc에 적을 것: Real은 CarUxRestrictions를 기준으로 정한다. 구현은 후속 계획이다.
  - `data/driving/FakeDrivingStateSource.kt`: 초기값 PARKED, `set(state)`
  - 테스트 의존성은 여기서 처음 쓰므로 이 커밋에 넣는다: junit 4.13.2, kotlinx-coroutines-test, turbine
  - `FakeDrivingStateSourceTest`:
    - `` `처음에는 정차 상태를 내보낸다`() ``
    - `` `주행으로 바꾸면 주행 상태를 내보낸다`() ``
    - `` `같은 상태로 다시 바꾸면 새 값을 내보내지 않는다`() ``
  - 확인: `.\gradlew.bat testDebugUnitTest --tests 'com.meongcare.app.data.driving.FakeDrivingStateSourceTest'` 통과

- [ ] 5. Real 자리표시와 수동 DI
  - `data/driving/RealDrivingStateSource.kt`: UNKNOWN만 내보낸다. KDoc에 "CarUxRestrictions 구현 전 자리표시, 후속 계획"이라고 적는다.
  - `di/DataMode.kt`, `di/DataModeStore.kt`, `di/AppContainer.kt`, `MeongCareApplication.kt`
    - 매니페스트에 `android:name=".MeongCareApplication"`을 추가한다.
    - `AppContainer`가 가질 것:
      - `fakeDrivingStateSource`
      - `drivingStateSource`: 모드에 따라 Fake 또는 Real
      - `dataMode`
  - 커밋:
    - `feat(data): Real 주행 상태 자리표시 구현 추가`
    - `feat(di): Application과 AppContainer 수동 DI 추가`
  - 확인: `assembleDebug`. 기기가 있으면 실행해서 크래시가 없는지 본다.

- [ ] 6. 홈 ViewModel과 주행/정차 화면 (Fake로 동작)
  - `ui/home/HomeUiState.kt`, `HomeViewModel.kt`
    - 매핑과 초기값은 위 "주행 상태 → 화면"을 따른다.
    - factory는 `viewModelFactory { initializer { ... } }`로 만들고 `AppContainer`에서 꺼낸다.
  - `HomeScreen.kt`, `ParkedScreen.kt`, `DrivingScreen.kt`
    - 상태 수집은 `collectAsStateWithLifecycle`로 한다.
    - 주행 화면 규칙을 지킨다. `DrivingScreen.kt`는 따로 파일로 둬서 Grep 검사가 되게 한다.
  - 매니페스트 `MainActivity`에 `windowSoftInputMode="adjustResize"`를 넣는다.
  - 처음 쓰는 의존성: `lifecycle-viewmodel-compose`, `lifecycle-runtime-compose` (lifecycle 2.9.0)
  - `testutil/MainDispatcherRule.kt`
  - `HomeViewModelTest`:
    - `` `정차 중이면 정차 화면 상태다`() ``
    - `` `주행으로 바뀌면 주행 화면 상태로 바뀐다`() ``
    - `` `주행 상태를 모르면 주행 화면 상태로 보여준다`() ``
  - 커밋 (diff가 200줄을 넘으면 둘로 나눈다):
    - `feat(ui): 홈 ViewModel과 화면 상태 추가`
    - `feat(ui): 주행·정차 화면 추가 (Fake로 동작)`
  - 확인:
    - `HomeViewModelTest` 통과
    - `DrivingScreen.kt`에서 Grep `TextField` 0건
    - 기기가 있으면 `/verify`로 정차 화면을 본다.

- [ ] 7. debug 패널: Fake 상태 조작과 모드 전환
  - `ui/debug/DebugPanel.kt`
    - `BuildConfig.DEBUG`일 때만 홈 화면 아래에 작은 띠로 보인다. "DEBUG" 표시를 단다.
    - FAKE 모드: [정차] [주행] [모름] 버튼
    - 공통: 현재 모드를 표시하고 [Fake↔Real 전환] 버튼을 둔다.
    - 상태는 들고 있지 않는다. 값과 콜백은 `MainActivity`가 `AppContainer`에서 꺼내 넘긴다. 그래서 ui가 data를 import하지 않는다.
  - `di/AppRestarter.kt`: 모드를 저장한 뒤 재시작한다 (위 "데이터 모드").
  - 커밋:
    - `feat(ui): debug 전용 Fake 주행 상태 조작 패널 추가`
    - `feat(di): debug 전용 Fake·Real 모드 전환 추가`
  - 확인:
    - `assembleDebug`
    - 기기가 있으면 `/verify`: 정차 → [주행] → 주행 화면 → [정차] → 정차 화면
    - 버튼 좌표는 `adb shell uiautomator dump /sdcard/ui.xml` → `adb pull`로 찾는다.

- [ ] 8. 비밀값과 release 서명
  - **`secrets.properties`:**
    - 루트에 있으면 읽고, 없으면 빈 문자열을 쓴다.
    - `buildConfigField("String", "PLEOS_CLIENT_ID", ...)`와 `PLEOS_CLIENT_SECRET`을 만든다. 키 이름은 `secrets.properties.example`과 같다. 따옴표를 이스케이프한다.
  - **`keystore.properties`:**
    - 루트에 있을 때만 `signingConfigs.create("release")`를 만들고 release에 연결한다.
    - `storeFile`은 app 모듈 기준 `file(...)`로 해석한다. 예시 값 `../release.jks`는 저장소 루트를 가리킨다.
    - 파일이 없으면 서명 설정 없이 둔다. 그러면 unsigned APK가 나온다.
  - **비밀값 파일은 Read하지 않는다** (`.claude/settings.json` deny). 키스토어를 만들지 않는다 (`/release-apk` 규칙).
  - 커밋:
    - `build: secrets.properties 값을 BuildConfig로 주입`
    - `build: keystore.properties가 있을 때만 release 서명`
  - 확인:
    - 두 파일의 존재 여부만 확인한다.
    - 없으면 `.\gradlew.bat assembleDebug assembleRelease`가 성공하고 `app-release-unsigned.apk`가 생긴다.
    - `secrets.properties`가 **없을 때만** `app/build/generated/source/buildConfig/debug/com/meongcare/app/BuildConfig.java`에서 두 필드가 `""`인지 본다. 있으면 값이 노출되므로 보지 않는다.
    - 서명 경로는 키스토어가 없어 확인하지 못한다. 진행 로그에 그렇게 적는다.

- [ ] 9. [선행: pleos-docs-researcher] Vehicle 연결 수명주기 (Real 쪽 SDK 래퍼)
  - 선행 조사("확인 필요" 1~4)가 안 끝났으면 이 단계를 건너뛰고 후속 계획으로 넘긴다. 진행 로그에 적는다.
  - **처음 쓰는 의존성:**
    - `ai.pleos.playground:Vehicle:2.3.2` (`vehicle-sdk`)
      - 해결에 실패하면 2.3.1로 시도한다 (sdk-notes 불일치 메모).
    - `androidx.lifecycle:lifecycle-process` (lifecycle 2.9.0)
  - **`data/vehicle/VehicleConnection.kt`** (`DefaultLifecycleObserver`):
    - `onStart`: 새 `Vehicle(context)`를 만들고 `initialize()`를 부른다. 예외는 잡아서 로그만 남긴다. 크래시시키지 않는다.
    - `onStop`: `release()`를 부르고 참조를 버린다.
    - 나중에 Real 래퍼가 `Vehicle`이 필요하면 `VehicleConnection`에서 얻는다. 래퍼가 직접 만들지 않는다.
  - **`MeongCareApplication`:** REAL 모드일 때만 `ProcessLifecycleOwner`에 등록한다.
    - 이유: Application에는 종료 콜백이 없다. ux-guidelines는 백그라운드로 가면 불필요한 수집을 멈추라고 한다.
    - 공식 샘플이 다른 위치에서 부르고 그 이유가 문서에 있으면 샘플 방식을 따른다. 그 결정은 "결정 / 배운 점"에 남긴다.
  - 도메인 API는 부르지 않는다. 그래서 권한도 선언하지 않는다.
    - sdk-notes의 "콜백을 기다리는 중에는 release하지 않는다"는 도메인 API를 쓰는 후속 작업에서 처리한다.
  - **병합 매니페스트를 검사한다:**
    - 빌드 뒤 `app/build/intermediates/` 아래 병합된 `AndroidManifest.xml`에서 Grep `pleos.car.permission`.
    - **1건이라도 있으면 멈추고 사용자에게 보고한다** ([사용자 승인 필요]).
  - sdk-notes.md를 갱신한다 (출처 포함): `Vehicle` import 경로, `initialize()`/`release()` 시그니처, 2.3.2 해결 결과, 샘플의 호출 위치
  - 커밋:
    - `feat(vehicle): Vehicle 초기화·해제를 프로세스 수명주기 한 곳에서 관리`
    - `docs(pleos): Vehicle 초기화 API와 2.3.2 해결 결과 기록`
  - 확인:
    - `assembleDebug` 통과
    - `.\gradlew.bat :app:dependencies --configuration debugRuntimeClasspath`에 `ai.pleos.playground:Vehicle`이 있다.
    - 병합 매니페스트 권한 0건
    - Grep `import ai.pleos`가 `data/` 밖에 0건
    - Pleos 에뮬레이터에서의 실제 동작은 **확인 못 함** (GPU 없음). 그대로 기록한다.

- [ ] 10. 경계 테스트 (`tester`) → `test: 주행 상태와 홈 ViewModel 경계 테스트 추가`
  - 메인이 `tester` 서브에이전트에 이 계획 경로를 넘긴다.
  - 기대 동작:
    - 주행 → 정차 → 주행 왕복
    - UNKNOWN 처리
    - 구독을 취소했을 때
    - `RealDrivingStateSource`가 UNKNOWN을 낸다
  - tester가 찾은 버그는 프로덕션 코드에서 `fix:` 커밋으로 따로 고친다.
  - 확인: `.\gradlew.bat testDebugUnitTest` 통과. 통과 수와 실패 수를 진행 로그에 적는다.

- [ ] 11. 전체 검증 (`/verify`, 하네스). 커밋은 없다. 고친 게 있으면 `fix(...)` 커밋으로 낸다.
  - `node .claude/hooks/build-gate.mjs pipeline` → "검증 파이프라인 통과."
  - 훅 동작을 기록한다. 진행 로그에 적을 것:
    - 단계 2 이후 앱 코드 커밋 때 precommit 빌드가 돌았는지
    - Stop 훅이 돌았는지
    - `.claude/.cache/build-gate.json`의 `okDigest`/`pipelineDigest`
  - `/verify` (my_avd, debug, Fake):
    1. `adb install -r app/build/outputs/apk/debug/app-debug.apk`
    2. `adb shell am start -n com.meongcare.app/.MainActivity`
    3. 정차 화면 스크린샷
    4. [주행] → 주행 화면 스크린샷 (완료 기준 4)
    5. [정차] → 정차 화면으로 돌아오는지
    6. `adb logcat -d -t 300 *:E`
  - 기기가 없으면 멈추고 사용자에게 Cold boot를 요청한다. 확인 못 한 건 확인 못 했다고 적는다.
  - 완료 기준 5의 Grep 검사를 모두 돌린다.

- [ ] 12. 문서 마무리
  - README.md "소스에서 빌드하기":
    - JDK 17 이상 (21에서 확인함)
    - Android SDK Platform 35와 Build-Tools 35
    - `ANDROID_HOME` 또는 `local.properties`의 `sdk.dir`
    - Android Studio 없이 gradlew만으로 빌드된다.
    - `secrets.properties`는 선택이다. 없어도 빌드된다.
    - debug 빌드는 Fake 모드와 debug 패널로 시작한다.
    - 명령 3개: `assembleDebug`, `testDebugUnitTest`, `lintDebug`
  - `docs/architecture.md`를 실제 클래스 이름, 바뀐 결정과 맞춘다.
  - `docs/pleos/emulator-setup.md` "GPU가 부족한 팀원은"의 `FakeVehicleDataSource`를 실제 방식(debug 패널의 Fake 모드)으로 바꾼다.
  - AGENTS.md 아키텍처 규칙의 Fake/Real 전환 문구를 실제와 맞춘다 ("debug 패널, 재시작 시 적용, release는 항상 Real").
  - 커밋:
    - `docs: README 소스에서 빌드하기 갱신`
    - `docs: 아키텍처 문서와 Fake 모드 안내를 코드에 맞춤`
  - 확인:
    - README 절차가 단계 11에서 실제로 돌린 명령과 같다.
    - 완료 기준 7의 Grep이 통과한다.

- [ ] 13. 개발 노트 작성 (`docs/notes/`)
  - `docs/notes/xxj15/YYYY-MM-DD-project-setup.md`. 형식은 `docs/notes/README.md` 템플릿을 따른다.
    - 사용한 SDK: Vehicle 2.3.2 (initialize/release만). 권한은 없다.
    - 시연 방법: debug 패널로 주행/정차 전환
    - debug 패널이 주행 화면에도 보이는 예외와 그 이유
    - 확인 못 한 것: Pleos 에뮬레이터, release 서명
  - 이 계획을 `docs/plans/completed/`로 옮긴다. 진행 로그도 갱신한다.
  - 커밋:
    - `docs(notes): 프로젝트 초기 설정`
    - `docs(plans): 004 프로젝트 초기 설정 완료`
  - 확인: `git status`가 깨끗하다. 그다음 `/ship`.

**/ship 뒤:** GitHub Actions "Android CI"의 dev 실행 결과를 확인한다 (완료 기준 8).
- `gh`가 있으면 `gh run list --workflow android.yml --branch dev -L 1`
- 없으면 사용자에게 Actions 탭을 확인해 달라고 요청한다.
- 실패하면 바로 `fix/`나 `ci/` 작업 브랜치에서 고친다 (git.md).

### 있으면 좋음

- [ ] A. adb로 Fake 주행 상태 바꾸기 (debug 전용) → `feat(ui): adb로 Fake 주행 상태를 바꾸는 debug 진입점 추가`
  - 방법:
    - `MainActivity`를 `launchMode="singleTop"`으로 두고, `onCreate`/`onNewIntent`에서 extra `fake_driving_state`를 읽는다.
    - debug에서만, FAKE 모드일 때만 반영한다.
    - debug 패널 접기 버튼도 같이 둔다.
  - 쓰임:
    - 시연 영상을 찍을 때 패널 없이 상태를 바꾼다 (11/15 가산점).
    - `/verify`를 좌표 없이 자동화한다.
  - 확인: `adb shell am start -n com.meongcare.app/.MainActivity --es fake_driving_state DRIVING` 뒤 스크린샷이 주행 화면이다.
- [ ] B. 가로 화면과 다크 모드 확인 (`/verify`)
  - 회전: `adb shell settings put system accelerometer_rotation 0` 후 `adb shell settings put system user_rotation 1`
  - 다크 모드: `adb shell cmd uimode night yes`
  - Pleos 디자인 가이드는 가로·세로 레이아웃 모두 대응하라고 한다 (ux-guidelines "화면 방향").
  - 확인: 두 화면 모두 글자 잘림이나 겹침이 없다. 문제가 있으면 `fix(ui): ...`
- [ ] C. 일반 AVD에서 Real 모드 전환 시 동작 기록 (단계 9를 했을 때만)
  - debug 패널에서 Real로 전환하고 재시작한 뒤 logcat을 본다.
  - Vehicle Service가 없을 때 `initialize()`가 어떻게 되는지 sdk-notes "미확인"에 관찰 결과로 적는다. 관찰이라고 표시한다.
  - 크래시가 나면 `fix(vehicle): ...`로 막는다.

## 진행 로그

- 2026-11-03: 계획 작성.
  - 브랜치 `chore/project-setup`는 dev에서 땄고 `fa090e3`이 올라가 있다.
  - 이 PC 환경:
    - JDK 21, `ANDROID_HOME=C:\Android`
    - platforms 34/36 (35는 메인이 설치 중), build-tools 35.0.0/36.0.0
    - AVD `my_avd` (android-34 google_apis x86_64)
    - Pleos 에뮬레이터와 Android Studio는 없다.

- 2026-11-03 (메인):
  - android-35 플랫폼 설치 완료 (`sdkmanager "platforms;android-35"`).
  - Gradle 8.10.2 배포판을 받아 SHA-256을 공식 값과 대조했다 (`31c55713…`). 일치한다.

## 결정 / 배운 점

- **wrapper를 공식 샘플에서 가져오지 않는다.**
  - 샘플의 `gradle-wrapper.jar` SHA-256(`e996d452…`)이 Gradle 8.10.2 공식 wrapper 체크섬(`2db75c40…`)과 다르다.
  - 8.7~9.1의 다른 공식 체크섬과도 맞지 않았다.
  - 검증되지 않은 실행 파일을 저장소에 넣지 않기 위해, 검증한 배포판으로 직접 만든다.
