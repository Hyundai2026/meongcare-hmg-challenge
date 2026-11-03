# Pleos Connect SDK: 검증된 사실

이 파일의 규칙:
- 공식 문서에서 확인한 것만 적고, 항목마다 출처를 단다.
- 확인하지 못한 것은 맨 아래 "미확인"에 둔다.
- 이 파일이나 공식 문서에 없는 API 이름·시그니처·의존성 좌표를 추측해서 코드에 쓰지 않는다.
- UX·주행 규제·디자인 가이드는 [ux-guidelines.md](ux-guidelines.md)에 둔다.

마지막 갱신: 2026-11-03

## 빌드 설정

- **minSdk 28 이상.** 차량 보안 요구사항(서명키 rotation/lineage) 때문이다. [project-setup] [setup]
  - 원문: "minSDK 버전은 Android API 28 이상이어야 합니다." [setup]
- **Gradle 8.0 이상.** [setup]
- 개발 언어: "Java, Kotlin" [setup]
- 에뮬레이터 이미지는 **API 34** (Android 14) 기준이다. [setup]
- `setup` 페이지에는 JDK, AGP, Kotlin, compileSdk/targetSdk 버전이 적혀 있지 않다. Android Studio는 "최신 버전"이라고만 한다. [setup]
- **Maven 저장소:** `https://nexus-playground.pleos.ai/repository/maven-releases/` [getting-started] [project-setup]

`settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://nexus-playground.pleos.ai/repository/maven-releases/") }
    }
}
```

**Vehicle SDK:**

```kotlin
implementation("ai.pleos.playground:Vehicle:2.3.2")
```

[vehicle-intro] [project-setup] [sample-app]

> **불일치:** Getting Started 페이지에는 같은 좌표가 `2.3.1`로 적혀 있다. 공식 샘플 저장소의 `gradle/libs.versions.toml`도 `vehicleSdk = "2.3.1"`이다 (README와 튜토리얼 페이지는 2.3.2). 2.3.2를 쓰고, 의존성 해결에 실패하면 2.3.1로 시도한다. [getting-started] [gh-vehicle] [sample-app]

### 공식 샘플 앱의 빌드 값 (튜토리얼 페이지 "Sample App release 기준" 표) [sample-app]

Pleos가 "권장 버전"이라고 명시한 것은 아니고, 샘플 앱이 이 값으로 빌드·검증되었다는 표다.

| 항목 | 값 |
|---|---|
| Sample App | `1.0.0-sdk35` |
| Vehicle SDK | `2.3.2` |
| Vehicle Service | `2.3.22` |
| AVD | `Pleos_Connect_v2.1`, Android 14 |
| JDK | 17 |
| Gradle wrapper | `8.10.2` |
| Android Gradle Plugin | `8.8.2` |
| Kotlin | `2.0.0` |
| compileSdk / targetSdk | 35 |
| Sample App minSdk | 23 (Playground에 배포하는 제품 앱은 별도로 API 28 이상 필요) |

> **불일치:** 에뮬레이터 이미지는 API 34인데 [setup], 샘플 앱은 compileSdk/targetSdk 35로 빌드한다 [sample-app]. 둘이 충돌하는 것은 아니지만(34 기기에서 35 타깃 앱 실행), 기존 "예제도 API 34 기준"이라는 기록은 샘플 앱에는 맞지 않는다.

### 공식 샘플 저장소의 실제 설정 (문서 그대로 인용)

샘플 저장소는 [sample-app] 페이지가 링크한 `github.com/Pleos-Playground` 조직 저장소다. main 브랜치, 2026-11-03 확인.

`pleos-sample-vehicle-app/app/build.gradle.kts` [gh-vehicle]:

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

val targetAndroidSdk = 35
// android { ... }
namespace = "ai.pleos.playground.sample.vehicle"
compileSdk = targetAndroidSdk
defaultConfig {
    minSdk = 23
    targetSdk = targetAndroidSdk
}
compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
kotlinOptions {
    jvmTarget = "17"
}
buildFeatures {
    compose = true
    buildConfig = true
}
```

(줄 단위 인용을 이어 붙인 것이다. 들여쓰기와 블록 위치는 원본과 다를 수 있다.)

`pleos-sample-vehicle-app/gradle/libs.versions.toml` [versions]와 [plugins] [gh-vehicle]:

```toml
agp = "8.8.2"
kotlin = "2.0.0"
coreKtx = "1.16.0"
junit = "4.13.2"
androidxJunit = "1.2.1"
espressoCore = "3.6.1"
lifecycle = "2.9.0"
activityCompose = "1.10.1"
composeBom = "2024.04.01"
vehicleSdk = "2.3.1"
kotlinReflect = "2.0.0"

android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }

vehicle-sdk = { module = "ai.pleos.playground:Vehicle", version.ref = "vehicleSdk" }
```

- Gradle wrapper: `distributionUrl=https\://services.gradle.org/distributions/gradle-8.10.2-bin.zip` [gh-vehicle]

다른 공식 샘플의 값 [gh-navi] [gh-fused]:

| 샘플 | AGP | Kotlin | JDK (`jvmTarget`) | compile/target | minSdk | UI |
|---|---|---|---|---|---|---|
| pleos-sample-vehicle-app | 8.8.2 | 2.0.0 | 17 | 35 / 35 | 23 | Compose + Material3 |
| pleos-sample-fusedlocation-app | 8.8.2 | 2.0.0 | 17 | 35 / 35 | 24 | Compose + Material3 |
| pleos-sample-navi-app | 8.8.0 | 1.9.22 | 11 | 35 / 35 | 33 | Compose + Material3, Hilt(kapt) |

## 앱 형태와 UI 프레임워크

- **일반 Android Activity 앱이다.**
  - "앱 구조 및 동작 모델" 페이지는 Activity와 `AndroidManifest.xml`의 `screenOrientation`을 기준으로 화면 동작을 설명한다. [app-structure]
  - 공식 샘플 앱은 `MAIN`/`LAUNCHER` intent-filter를 가진 일반 `MainActivity` 하나로 동작한다. [gh-vehicle]
- **Android for Cars App Library(템플릿)는 문서에 언급이 없다.** 강제한다는 문장도, 금지한다는 문장도 찾지 못했다. [app-structure] [app-category] [sdk-api-policy] [hw-access]
- **Jetpack Compose:** 공식 문서 본문에는 Compose/Views 언급이 없다. 공식 샘플 3개는 모두 Compose(`buildFeatures { compose = true }`, `androidx.compose.material3`)를 쓴다. [gh-vehicle] [gh-navi] [gh-fused]
  - vehicle 샘플 README: `MainActivity` "hosts Compose and forwards UI actions." Compose UI 테스트는 `connectedDebugAndroidTest`로 돌린다. [gh-vehicle]
- 화면 방향, 멀티 윈도우, 디스플레이 종류는 [ux-guidelines.md](ux-guidelines.md#화면-구성)에 정리했다.

## SDK 사용 가능 상태 (Playground 콘솔 기준) [create-project] [console-2026-11-03]

**2026-11-03에 팀원이 Playground 콘솔에 로그인해 직접 확인했다.** 문서의 상태표와 같다. 콘솔은 SDK를 기능 항목 단위로 보여 준다.

| SDK | 콘솔 항목 | 상태 |
|---|---|---|
| Vehicle State | 11개: 차량 제원 정보, 차량 주행 정보(브레이크 페달, 드라이브 모드, 크루즈 컨트롤, 주행 거리), 도어/트렁크/프렁크, EV 및 연료, 내부 온도 및 공조, 내부/외부 라이트, 차량 안전 상태(경고등: 안전벨트, 브레이크, 에어백, 엔진 오일, 타이어 공기압), 좌석 설정, 사이드미러, 통행료 시스템, 창문 (모두 "조회") | 11개 모두 사용 가능 |
| NaviHelper | 경로 정보 조회 ("현재 설정된 경로 정보, 위치 정보, TBT 등을 조회합니다."), 경로 탐색 설정 ("경로 탐색 옵션을 설정합니다.") | 2개 모두 사용 가능 |
| FusedLocation | 정밀 위치 정보 조회 | 준비 중 |
| TextToSpeech | TextToSpeech | 준비 중 (문서: Client ID/Secret 필요) |
| SpeechToText | SpeechToText | 준비 중 (문서: Client ID/Secret 필요) |
| Vehicle Control | 8개: 도어/트렁크/프렁크 제어, 공조 설정, 주행 정보 설정, EV 충전 설정, 창문 제어, 좌석 제어, 사이드미러 제어, 라이트 제어 | 8개 모두 준비 중 |

- Vehicle State의 "차량 주행 정보"에도 속도·기어는 없다. 아래 "속도·기어·주행 상태"와 맞는다.
- NaviHelper 콘솔 항목과 권한의 대응은 콘솔에 적혀 있지 않다. 설명으로 보면 "경로 정보 조회"는 `NAVI_ROUTE`(`get*Info()`), "경로 탐색 설정"은 `NAVI_ROUTE_SEARCH`(`requestRoute()` 등)에 해당하는 것으로 보인다 (추정). `NAVI_CUSTOM_MAP`(`requestNavigationScreen()`)에 해당하는 항목은 없다.
- 차량은 **읽기만** 된다. 공조·창문 등을 바꾸는 Vehicle Control은 전부 준비 중이다.
- 상태의 뜻 (문서 원문) [create-project]:
  - 준비 중: "API 제공 준비 중인 상태로 신청 버튼이 나타나지 않습니다." "해당 상태에서는 신청할 수 없으므로 신청 필요 상태로 변경된 후 진행하시기 바랍니다."
  - 사용 가능: "신청 없이 사용 가능한 SDK이거나 신청이 승인되어 사용 가능한 상태입니다." "Vehicle State와 NaviHelper SDK가 이 상태에 해당합니다. 이 두 SDK는 신청 없이 바로 사용 가능합니다."
  - 신청 필요: "신청해야 사용 가능한데 아직 신청하지 않는 상태입니다."
- **해석 (2026-11-03 팀 판단): "준비 중"인 SDK는 지금 쓸 수 없는 것으로 본다.** `ON_DEVICE`가 앱 등록 없이 동작한다는 STT/TTS 문서의 문장은 사용 허가가 아니다. 열리는 시점은 주최측에 묻는다 ([architecture.md](../architecture.md#pleos주최측에-문의할-것) B2).

- 그 밖의 SDK는 사용 신청이 필요하고, 일부는 제휴 계약이 필요하다.
- **"준비 중"인 SDK에 핵심 기능을 걸지 않는다.** 기획 전에 콘솔에서 상태를 다시 확인한다.
- "준비 중"은 신청 버튼이 없는 상태다. STT/TTS의 `ON_DEVICE` 모드는 Client ID/Secret 없이 동작하도록 문서화되어 있지만, 콘솔 상태와의 관계는 문서에 없다. → 아래 "STT/TTS 사용 조건 정리" [create-project] [stt-intro]
- 다만 Pleos Maven에는 SpeechToText/TextToSpeech 3.3.1(2026-10-13), NaviHelper 3.0.5(2026-10-19)가 게시되어 있다. [maven-meta]
- > **불일치 (2026-11-03):** 대회 FAQ "어떤 SDK와 API를 쓸 수 있나요?"의 답은 "Vehicle SDK, NaviHelper SDK, TextToSpeech SDK, SpeechToText SDK, FusedLocation SDK입니다."다. 콘솔에서 "준비 중"인 3개가 들어 있다. FAQ는 "각 SDK의 현재 제공 상태와 사용 조건은 SDK 문서 페이지에서 확인해 주세요."라고만 한다. [contest-faq]
  > 대회 개발 가이드의 앱 예시에도 "음성 메모 앱"이 있다: "SpeechToText SDK로 운전 중 음성을 받아 메모로 남깁니다." [contest-dev]
  > 주최측이 STT를 쓰는 앱을 기대한다는 근거다. 다만 실제로 쓰는 방법(콘솔 신청, `ON_DEVICE`)은 여전히 미확인이다 → 주최측 문의 ([architecture.md](../architecture.md#pleos주최측에-문의할-것) B2)
- 앱 심사에는 "프로젝트 ID(Key)"가 필요하다. 에뮬레이터 2.1.0부터는 CRN이 필요 없다.

### 프로젝트 생성 화면의 입력 항목 [create-project] [app-submission]

경로: Pleos Playground 로그인 → Workspace > My Project → 프로젝트 생성. 회원 가입 단계에는 아래 항목이 없다.

| 항목 | 문서 설명 (원문) | 나중에 변경 |
|---|---|---|
| 프로젝트 이름 | "앱 이름 또는 앱 개발 과제의 이름을 입력합니다." | "이 값은 나중에 변경 가능합니다." |
| 개발사명 | "소속 회사의 이름을 입력합니다." | 언급 없음 |
| 카테고리 | "개발하려는 앱의 카테고리를 선택합니다." "카테고리를 정의하고 시작하실 것을 권장합니다." | 언급 없음 |
| 애플리케이션 설명 | "개발하려는 앱에 대한 간단한 설명을 작성합니다." | 언급 없음 |

- 필수 여부, 글자 수, 개인 개발자나 사업자 등록 여부에 대한 규칙은 없다.
- 개발사명이 App Market에 공개되는지는 문서에 없다. 앱 제출 가이드는 "앱 제출 전 Pleos App Market에 등록할 개발자(또는 개발사) 정보가 정확한지 확인합니다."라고 한다. [app-submission]
- 우리 프로젝트의 입력값: 개발사명은 팀명(확정 전이면 임시 팀명), 카테고리는 주최측 답(architecture.md 문의 B3)이 오기 전까지 잠정값을 쓴다.

## SDK API 정책 (AAOS 표준 우선) [sdk-api-policy] [hw-access]

- "Pleos Connect SDK는 동일 혹은 유사 기능의 API가 AAOS에 추가될 경우 플랫폼 파편화를 최소화하고 앱의 이식성을 극대화하기 위해 AAOS Alignment 정책을 준수합니다." [sdk-api-policy]
- AAOS 표준 API가 같은 기능을 제공하면 그것을 쓴다. Pleos SDK는 표준으로 안 되는 기능이나 제조사 전용 보안·제어에만 자체 API를 둔다. [sdk-api-policy]
- **표준 차량 데이터(예: 속도, 기어 상태, 외기온, 연료량)는 AAOS Car API로 얻는다.** 제조사 전용 기능(시트 제어, 특수 조명, 전용 보안 인증 등)은 Pleos Connect SDK로 얻는다. [sdk-api-policy]
- SDK API가 나중에 AAOS 표준에 들어가도 기존 SDK API는 최소 2개 메이저 버전 동안 호환을 유지한다. [sdk-api-policy]
- 차종에 따라 지원 API가 다르다. [sdk-api-policy]
- "보안과 시스템 안정성을 위해 차량 하드웨어에 직접 접근할 수 없습니다." [hw-access]
- Telephony, SMS, 일부 센서(자이로, 근접), 캘린더, 연락처 같은 모바일용 API는 차량에서 제한될 수 있다. 지원 여부를 확인한 뒤 선택 기능으로 만든다. [hw-access]

## Vehicle SDK 사용 패턴 [getting-started]

```kotlin
val vehicle = Vehicle(context)
vehicle.initialize()

val door = vehicle.getDoor()
door.getCurrentDoorPosition(
    area = DoorArea(DoorValue.ROW_1_LEFT),
    onComplete = { pos: Int? -> /* 현재 문 위치 사용 */ },
    onFailed = { e: Exception -> /* Vehicle SDK 에러 처리 */ },
)
```

- `release()`는 SDK가 더 이상 필요 없을 때(예: `onDestroy`) 호출한다. 콜백을 기다리는 중에는 호출하지 않는다.
- 도메인 객체는 `vehicle.getXxx()`로만 얻는다. 직접 생성하지 않는다.
- 도메인마다 4가지를 제공한다:
  - `checkXxxCapabilities`: 지원 여부와 허용값 조회
  - get / set
  - 리스너 콜백
- 결과는 모두 비동기 `onComplete` / `onFailed`로 돌아온다 (IPC).
- 도메인 목록 [vehicle-intro]: Brake, CarInfo, Display, Door, DrivingMode, EvBattery, Fuel, HVAC, Light, Odometer, Safety, Seat, SideMirror, Steeringwheel, Tire, Toll, TurnSignal, Window, Wiper
- vehicle 샘플 README에 따르면 2.3.2는 19개 도메인에 deprecated 아닌 public 메서드 442개를 노출한다. [gh-vehicle]

### 2.3.2 AAR에서 직접 확인한 것 (산출물 기준, 2026-11-03) [aar-2.3.2]

문서가 아니라 Pleos Maven에서 받은 `ai.pleos.playground:Vehicle:2.3.2` AAR을 `javap`과 AAR 매니페스트로 확인했다.

- **좌표 해결**: 2.3.2가 Pleos Maven에서 정상 해결된다.
  - 전이 의존성: `vehicle-aidl:1.1.1`, `vehicle-data:1.1.11`
  - `vehicle-aidl`이 `com.google.dagger:hilt-android:2.51.1`을 끌고 온다. 우리 앱은 Hilt 플러그인을 쓰지 않으므로 라이브러리로만 들어온다.
- **`Vehicle` 클래스 시그니처** (`ai.pleos.playground.vehicle.Vehicle`):

  ```
  public Vehicle(android.content.Context)
  public final void initialize()
  public final void release()
  public final ai.pleos.playground.vehicle.api.<Domain> get<Domain>()   // 도메인별
  ```

  - `initialize()`/`release()`는 반환값이 없는 동기 함수다. 공식 샘플은 `initialize()`를 try/catch로 감싼다. [gh-vehicle-src]
- **AAR이 병합 매니페스트에 스스로 넣는 것**:
  - `<uses-permission android:name="ai.pleos.playground.service.vehicle.VEHICLE_BINDING" />`
    - 차량 서비스에 연결하기 위한 권한이다. `pleos.car.permission.*`이 아니다.
  - `<queries>`에 action `ai.pleos.playground.vehicle.VEHICLE_START`
  - meta-data: 필요한 차량 서비스 최소 버전 `min_version_name` 2.3.13 (`min_version_code` 65)
- **소비자 R8 규칙이 AAR에 들어 있다** (`proguard.txt`: `-keep class ai.pleos.playground.vehicle.api.implement.* { *; }` 등). 앱에서 R8을 켜도 SDK 쪽 규칙은 자동으로 적용된다.
- **공식 샘플의 initialize/release 위치**: `AndroidViewModel`이 `Vehicle(application)`을 만든다. `init`에서 `initialize()`, `onCleared()`에서 `release()`를 부른다. 한 인스턴스를 release한 뒤 다시 initialize하지 않는다. [gh-vehicle-src]
- **차량 서비스가 없는 기기에서의 동작** (관찰. 일반 폰 에뮬레이터 API 34, Vehicle 2.3.2, 2026-11-03) [obs-no-service]
  - `initialize()`는 **예외 없이 바로 끝난다.**
  - 이후 SDK가 백그라운드에서 서비스 연결을 시도한다.
    - 대상: `ai.pleos.playground.service.vehicle/.VehicleService` (action `VEHICLE_START`)
    - 약 0.5초 간격으로 31번 시도한다 (`currentRetryCount` 0~30, 약 15초).
  - 끝나면 로그 `E [Pleos][SDK]b: Connect Service is Failed cause : null`를 남긴다. 앱에 따로 알리는 콜백은 보이지 않았다.
  - 앱은 죽지 않는다.
  - **주의:** 그래서 `initialize()`가 성공했다고 연결된 것이 아니다. 실제 연결 여부는 도메인 API 호출의 `onFailed`로 알게 될 것으로 보이지만, 아직 확인하지 않았다.

## 속도·기어·주행 상태

### Vehicle SDK에는 속도·기어 도메인이 없다

- `ai.pleos.playground.vehicle.api` 패키지의 클래스 19개 중 속도·기어 이름의 클래스는 없다. [vehicle-api]
  - > **불일치 (2026-11-03):** 2.3.2 AAR의 `Vehicle` 클래스에는 `getGear()`가 있다. 반환형은 `ai.pleos.playground.vehicle.api.Gear`다. [aar-2.3.2]
    > 도메인 getter도 문서의 19개가 아니라 20개다 (Gear 추가).
    > `Gear` 인터페이스의 메서드:
    > - `getCurrentGearSelectionState(onComplete: (VehicleGear) -> Unit, onFailed: (Exception) -> Unit)`
    > - `registerGearSelection(GearSelectionListener)`
    > - `unregisterGearSelection(GearSelectionListener)`
    >
    > 문서에는 없으므로 필요한 권한, `VehicleGear` 값, 지원 여부는 미확인이다. 주행 상태 Real 구현(후속 계획)에서 `CarUxRestrictions`의 대안으로 검토한다.
- 도메인 설명 (원문) [vehicle-intro]:
  - DrivingMode: "드라이브 모드, 크루즈 컨트롤 정보 조회"
  - Brake: "브레이크 페달, 주차 브레이크 상태 조회"
  - Odometer: "총 주행 거리, 트립 주행 거리/시간, 주행 거리 단위 조회"
  - CarInfo: "차량 기본 정보(외부 크기, 제조사, 연식 등) 조회"
- 즉 **DrivingMode는 "주행 중/정차 중" 상태가 아니라 드라이브 모드와 크루즈 컨트롤이다.**
- DrivingMode 메서드 이름 (API Reference 사이드바) [driving-mode-api]:
  `checkCruiseControlStateCapabilities`, `checkCruiseControlTargetSpeedCapabilities`, `checkDrivingModeStateCapabilities`, `getCruiseControlState`, `getCruiseControlTargetSpeed`, `getCurrentDrivingModeState`, `registerCruiseControlState`, `registerCruiseControlTargetSpeed`, `registerDrivingModeState`, `unregisterCruiseControlState`, `unregisterCruiseControlTargetSpeed`, `unregisterDrivingModeState`
- 관련 상수 클래스: `ai.pleos.playground.vehicle.constant.control` 패키지에 `DrivingModeType`, `CruiseControl`이 있다. 값은 미확인. [constant-control]
- Brake 메서드 이름 [brake-api]: `checkBrakePedalStatusCapabilities`, `getCurrentBrakePedalStatus`, `registerBrakePedalStatus`, `unregisterBrakePedalStatus`
  - > **불일치:** intro는 Brake가 "주차 브레이크 상태 조회"도 한다고 하지만 [vehicle-intro], API Reference 목록에는 브레이크 페달 메서드 4개만 보인다 [brake-api].
- Odometer는 거리·주행 시간·잔여 거리만 다룬다. 속도 메서드는 없다. [odometer-api]

### 속도는 AAOS `CarPropertyManager`로 읽는다

- Pleos FAQ "클러스터 속도 조회 방법": 클러스터는 "`PERF_VEHICLE_SPEED` 프로퍼티로 차량 속도를 표시하므로", 앱도 AAOS `CarPropertyManager`로 `PERF_VEHICLE_SPEED`를 조회한다. 필요한 권한을 선언한 뒤 현재 값을 읽거나 변경 이벤트를 구독한다. GPS 기반 속도는 클러스터 값과 다를 수 있다. [faq]
  - FAQ에는 코드와 권한 문자열이 없다. Google 문서를 보라고만 링크한다. [faq]
- Google 공식 레퍼런스 (Pleos FAQ가 링크한 문서) [g-vehicle-property-ids]:

| 프로퍼티 | 타입 | 변경 모드 | 읽기 권한 (원문) |
|---|---|---|---|
| `PERF_VEHICLE_SPEED` | `Float`, m/s, 전진 +/후진 − | `CHANGE_MODE_CONTINUOUS` | "Dangerous permission Car.PERMISSION_SPEED to read property." |
| `GEAR_SELECTION` | `Integer` | `CHANGE_MODE_ONCHANGE` | "Normal permission Car.PERMISSION_POWERTRAIN to read property." |
| `PARKING_BRAKE_ON` | `Boolean` | `CHANGE_MODE_ONCHANGE` | "Normal permission Car.PERMISSION_POWERTRAIN to read property." |

- 이 권한들은 `pleos.car.permission.*`이 아니라 AAOS `Car.PERMISSION_*`이다. Pleos에서 서드파티 앱에 허용되는지는 미확인이다.

### 주행 규제(주행/정차) 판단은 `CarUxRestrictions`로 한다

- 앱은 `CarUxRestrictions`로 주행 중 제한 상태를 확인한다. 속도·기어로 직접 판단하라는 문장은 없다. 자세한 내용은 [ux-guidelines.md](ux-guidelines.md#주행-규제-caruxrestrictions)에 있다. [driver-distraction] [driving-restriction-dev]

## 기타 SDK

### NaviHelper SDK (v3.0.5 기준) [navi-intro] [navi-import] [navi-perm]

- 설명: "경로 안내 제어와 내비게이션 정보 조회를 위한 API를 제공합니다." [sdk-overview]
- 기능: 목적지 설정·확인·취소, 경로 옵션 변경과 재탐색, 경유지 추가·조회·삭제, 경유지 포함 경로 요청, 경로 탐색·주행 상태 이벤트 수신, 내비게이션 정보 조회와 Navigation 화면 요청. [navi-intro]
- 요청은 Pleos Connect Navigation 앱으로 가고, 결과와 주행 변화는 `NaviHelperEventListener` 콜백으로 돌아온다. 실제로 오는 이벤트는 Navigation 앱의 구현과 상태에 따라 다르다. [navi-intro]
- 의존성:

```kotlin
implementation("ai.pleos.playground:NaviHelper:3.0.5")
```

- 권한 (원문):
  - `pleos.car.permission.NAVI_ROUTE`: `get*Info()`, `requestTrip()` 등
  - `pleos.car.permission.NAVI_ROUTE_SEARCH`: 경로 요청, 경유지 추가/삭제, 경로 취소 및 재탐색 등
  - `pleos.car.permission.NAVI_CUSTOM_MAP`: `requestNavigationScreen()`
  - API별 정확한 권한은 API Reference의 `@RequiresPermission`에서 확인한다.
- 공식 샘플: https://github.com/Pleos-Playground/pleos-sample-navi-app [sample-app]

#### 권한별 API (Declaring permissions 페이지 표) [navi-perm]

| 권한 | API |
|---|---|
| `pleos.car.permission.NAVI_ROUTE` | `get*Info()`, `requestTrip()` |
| `pleos.car.permission.NAVI_ROUTE_SEARCH` | `requestRoute()`, `requestRouteWithWaypoints()`, `cancelRoute()`, `requestReRoute()`, `addWaypoint()`, `removeWaypoint()`, `changeRouteOption()` |
| `pleos.car.permission.NAVI_CUSTOM_MAP` | `requestNavigationScreen()` |

> **불일치:** 공식 navi 샘플의 매니페스트는 위 3개 외에 `pleos.car.permission.NAVI_CUSTOM_ROUTE`, `pleos.car.permission.NAVI_CUSTOM_ETC`도 선언한다. 문서에는 이 두 권한이 없다. [gh-navi-src]

#### 호환성 [api-compat]

- API 호환성 표 원문: NaviHelper SDK는 "내비게이션이 탑재된 차량에 한해 전체 호환".
- 내비게이션 탑재 여부를 확인하는 공개 API는 없다 (위 [faq] 항목 참고).
- 공식 샘플 README의 요구 사항: Pleos 내비게이션 서비스를 쓸 수 있는 기기 또는 에뮬레이터, 호환되는 내비게이션 앱이 설치되어 기본 내비게이션 앱으로 설정되어 있을 것. 샘플은 내비게이션 응답을 직접 만들지 않는다. [gh-navi-src]

#### 생성과 초기화 [navi-init] [gh-navi-src]

- 생성: `val naviHelper = NaviHelper(applicationContext)` (Java: `new NaviHelper(getApplicationContext())`) [navi-init]
- 초기화: `naviHelper.initialize(onConnected, onFailed)` 또는 인자 없이 `naviHelper.initialize()`. `onConnected`는 첫 서비스 연결 성공 후, `onFailed`는 초기 재연결 시도가 모두 실패한 뒤 호출된다. [navi-init]
- 해제: 리스너를 먼저 제거하고 `naviHelper.release()` (보통 `onDestroy()`). release 후 다시 쓰려면 새 객체를 만든다. [navi-init]
- 공식 샘플 코드 (원문) [gh-navi-src]:

```kotlin
helper = NaviHelper(context)
helper.initialize()
helper.addListener(this)

helper.removeListener(this)
helper.release()
```

#### 목적지·경로 요청 (원문 코드) [navi-dest] [navi-routes]

```kotlin
// 목적지 설정
naviHelper.requestRoute(
    RouteInfo(
        latitude = 37.48544722,
        longitude = 127.03636666,
        displayLongitude = null,
        displayLatitude = null,
        poiName = "42dot",
        poiId = "36khp37hh8gqcm5nqhe5",
        poiSubId = "0",
        address = "서울시 강남구 남부순환로 2621"
    )
)
// 목적지 확인
naviHelper.getDestinationInfo()
// onDestinationInfo callback으로 데이터 수신
override fun onDestinationInfo(destinationInfo: DestinationInfo) {
    Log.d(logTag, "onDestinationInfo $destinationInfo")
}
// 목적지 취소
naviHelper.cancelRoute()
```

- 목적지를 다시 설정할 때도 `requestRoute()`를 부른다. [navi-dest]
- **Java 생성자는 경도를 먼저 받는다:** `new RouteInfo(127.03636666, 37.48544722, null, null, "42dot", "36khp37hh8gqcm5nqhe5", "서울시 강남구 남부순환로 2621", "0", RouteOption.RECOMMENDED)` (경도, 위도, 표시용 경도, 표시용 위도, poiName, poiId, address, poiSubId, RouteOption 순). [navi-dest]
- 공식 샘플은 `displayLatitude`/`displayLongitude`를 생략하고 `latitude`, `longitude`, `poiId`, `poiName`, `poiSubId`, `address`만 넘긴다. [gh-navi-src]
- `poiId`/`poiSubId`가 필수인지, 외부 장소 검색 결과(좌표만 있는 경우)로 경로를 요청할 때 어떤 값을 넣어야 하는지는 문서에 없다 → 미확인.
- 경로 옵션: `naviHelper.changeRouteOption(RouteOption.HIGHWAY)`, 결과는 `onRouteOptionChanged(routeOption: RouteOption)`. 재탐색: `naviHelper.requestReRoute()`. [navi-routes]
  - `RouteOption` 값: `RECOMMENDED`, `SHORT_DISTANCE`, `SHORT_TIME`, `FREE`, `HIGHWAY`, `COMFORTABLE`, `SCHOOL_ZONE` [navi-routes]
  - 원문: "경로 옵션 변경과 재탐색은 활성 경로가 있는 상태에서 요청합니다. 처리 결과와 실제로 전달되는 콜백은 Navigation 앱의 상태에 따라 달라질 수 있습니다." [navi-routes]
- 요청이 정상 수행되어도 경로 탐색이 끝났다는 뜻이 아니다. 결과는 이벤트 콜백으로 확인한다. 정보 조회 API는 값을 반환하지 않고 콜백으로 준다. [navi-dest]
- `requestTrip(TripRequest)`: 공식 샘플의 요청 데이터 (원문) [gh-navi-src]. 이 API가 정확히 무엇을 하는지 설명하는 문서는 찾지 못했다 → 미확인.

```kotlin
val tripRequest = TripRequest(
    latitude = 37.41275833,
    longitude = 127.09481944,
    poiId = "36khp2hdj2cfcm5nqhe3",
    poiName = "소프트웨어드림센터",
    poiSubId = "0"
)
```

- 경유지 포함 요청: `RouteWithWaypointsRequest(destination = routeInfo, waypoints = listOf(waypointInfo))`를 `requestRouteWithWaypoints(...)`에 넘긴다. [gh-navi-src]
- Navigation 화면 요청: 샘플은 `NavigationScreenRequest.RouteOptionList`를 `requestNavigationScreen(...)`에 넘긴다. [gh-navi-src]

#### `NaviHelper` 메서드 (API Reference 목차) [navi-api-ref]

`Constructor`, `addListener`, `addWaypoint`, `cancelRoute`, `changeRouteOption`, `getBookmarkInfo`, `getChargerOperatorInfo`, `getCurrentLocationInfo`, `getDestinationInfo`, `getRecentDestinationInfo`, `getRouteStateInfo`, `getTBTInfo`, `getWaypointInfo`, `initialize`, `release`, `removeListener`, `removeWaypoint`, `requestNavigationScreen`, `requestReRoute`, `requestRouteWithWaypoints`, `requestRoute`, `requestTrip`

- 패키지: `ai.pleos.playground.navi.constants`, `ai.pleos.playground.navi.data`, `ai.pleos.playground.navi.helper`, `ai.pleos.playground.navi.helper.listener` [navi-api-ref]
- 개별 메서드·데이터 클래스 페이지는 클라이언트 렌더링이라 본문이 비어 있다 (`.../route-state-info/entry`, `.../trip-info/entry`, `.../driving-info/entry`, `.../destination-arrived-info/entry` 확인).

#### `NaviHelperEventListener` 콜백 (공식 샘플 원문 시그니처) [gh-navi-src] [navi-events]

```kotlin
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener

override fun onRouteStarted(routeStartInfo: RouteStartInfo)
@Deprecated("Deprecated by NaviHelperEventListener")
override fun onRouteEnded()
override fun onRoutingCompleted()
override fun onReRouteEnded()
override fun onRouteOptionChanged(routeOption: RouteOption)
override fun onRouteCancelled()
override fun onWaypointChanged(waypointChangedInfo: WaypointChangedInfo)
override fun onDestinationArrived(destinationArrivedInfo: DestinationArrivedInfo)
override fun onWayPointArrived(waypointArrivedInfo: WaypointArrivedInfo)
override fun onError(errorCode: NaviErrorCode)
override fun onCurrentLocationInfo(currentLocationInfo: CurrentLocationInfo)
override fun onRouteStateInfo(routeStateInfo: RouteStateInfo)
override fun onDestinationInfo(destinationInfo: DestinationInfo)
override fun onWayPointInfo(waypointInfoList: List<WaypointInfo>)
override fun onRecentDestinationInfo(recentDestinationInfoList: List<RecentDestinationInfo>)
override fun onBookmarkInfo(bookmarkInfoList: List<BookmarkInfo>)
override fun onCurrentRoadSpeedLimit(speedLimit: Int)
override fun onCameraAlert(cameraAlertList: List<CameraAlert>)
override fun onDrivingInfo(drivingInfo: DrivingInfo)
override fun onBookMarkChanged()
override fun onDestinationChanged()
override fun onNavigationStatus(status: NavigationStatus)
override fun onTBTInfo(tbtInfoList: List<TBTInfo>)
override fun onChargerOperatorInfo(chargerOperatorList: List<ChargerOperator>)
override fun onCautionPointInfo(cautionPointList: List<CautionPoint>)
override fun onTripInfo(tripInfo: TripInfo)
```

- 데이터 클래스 import 경로: `ai.pleos.playground.navi.data.*`, 상수: `ai.pleos.playground.navi.constants.NaviErrorCode`, `NavigationStatus`, `RouteOption`, `WaypointIndex` [gh-navi-src]
- 문서 원문: "Navigation 앱의 구현 시나리오에 따라 전달되는 이벤트가 달라질 수 있습니다." [navi-events]
- **도착 이벤트:** `onDestinationArrived(DestinationArrivedInfo)`가 있다. 실제로 언제 오는지는 Navigation 앱 구현에 따른다.
- **ETA·남은 거리:** 이름이 관련 있어 보이는 `RouteStateInfo`(`getRouteStateInfo()`/`onRouteStateInfo`), `TripInfo`(`onTripInfo`), `DrivingInfo`(`onDrivingInfo`), `TBTInfo`가 있지만, **필드 정의는 확인하지 못했다.** 공식 샘플도 객체를 `toString()`으로 로그에 찍기만 하고 필드에 접근하지 않는다. → 미확인.
  - **산출물로 확인 (2026-11-03)** [aar-navi-data]: 데이터 클래스는 `NaviHelper:3.0.5`가 아니라 전이 의존성 `ai.pleos.playground:navi-data:3.0.4`의 `ai.pleos.playground.navi.data` 패키지에 있다. getter 목록 (`javap`):
    - `TripInfo`: `getDistance(): Int`, **`getEta(): Int`**, `getEstimatedSocAtDest(): Int`, `getEnergyConsumption(): Double`, `getSummary(): TripSummary`
    - `DrivingInfo`: `getDestination(): DrivingState`, `getWaypoints(): List<DrivingState>`
      - `navi.data.DrivingState`: `getName()`, **`getDuration(): Int`**, **`getDistance(): Int`**, 충전 관련 필드
    - `CurrentLocationInfo`: **`getLatitude()`, `getLongitude()`**, `getHeading()`, `getAddress()`, `getRouteId()`, `getCurrentRouteIndex()`
    - `DestinationArrivedInfo`: `getDuration()`, `getDistance()`, `getName()`, `getAddress()`
    - `RouteStateInfo`: `getTermsAgreement()`, `getNaviMode()`, `isRouting()`, `getRouteOption()`, 충전 관련, `getTrafficStatus()` (ETA 없음)
  - **값의 단위(초/분, 미터)는 문서에도 클래스에도 없다.** → 미확인. 실제 값을 찍어 봐야 한다.
  - 참고: `NaviHelper` 3.0.5의 전이 의존성은 `navi-data:3.0.4`, `navi-aidl:3.0.1`, `kotlin-stdlib:1.9.25`, `core-ktx:1.8.0`, `kotlinx-coroutines-android:1.6.3`이다 (pom).
- 현재 위치: `getCurrentLocationInfo()` → `onCurrentLocationInfo(CurrentLocationInfo)` (권한 `NAVI_ROUTE`, `get*Info()`). 필드 정의는 미확인.

### FusedLocation SDK [fused-intro] [fused-setup] [fused-perm]

- 설명: "FusedLocation SDK는 앱이 차량 위치 데이터를 수신할 수 있도록 API를 제공합니다." [fused-intro]
- 주요 API: `getLastKnownLocation()`, `registerFusedLocationCallback()`, `unregisterFusedLocationCallback()`, `isSupported()`, `showLocationAccessBlockedPopup()`, `addProximityAlert()`, `removeProximityAlert()` [fused-intro]
- 의존성:

```kotlin
implementation("ai.pleos.playground:FusedLocation:1.1.3")
```

- 권한: `<uses-permission android:name="pleos.car.permission.FUSED_LOCATION" />`. protection level은 `dangerous`이므로 앱이 권한 요청과 결과 처리를 직접 해야 한다. [fused-perm]
- 차량 개인정보 설정이 위치 접근을 막으면 `showLocationAccessBlockedPopup()`으로 안내한다. 이 API가 권한을 주지는 않는다. 콜백의 `null`과 `onError(Exception)`을 처리한다. [fused-perm]
- `ACCESS_COARSE_LOCATION`/`ACCESS_FINE_LOCATION`/`ACCESS_BACKGROUND_LOCATION`의 동작은 SDK 문서가 보장하지 않는다. [fused-perm]
  - 원문: "Google Fused Location SDK의 설명을 Pleos Connect의 동작 보장으로 해석하면 안 됩니다." [fused-perm]
- 공식 샘플: https://github.com/Pleos-Playground/pleos-sample-fusedlocation-app [sample-app]
- 호환성: API 호환성 표에서 FusedLocation SDK는 "모델에 따라 다름", 확인 방법은 `isSupported` API. 원문: "FusedLocation SDK 가용 여부는 FusedLocation SDK `isSupported` API로 확인할 수 있습니다." [api-compat]
- 사용 순서: 객체 생성 → `isSupported()` 확인 → `initialize()`. 지원하지 않는 환경이면 FusedLocation 기능을 제공하지 않게 처리하라고 권장한다. [fused-intro]

### 위치를 얻는 다른 방법 (2026-11-03 조사)

- **Android 표준 `LocationManager`:** Pleos 문서(FusedLocation 3개 페이지, 하드웨어 접근 정책, SDK API 정책, API 호환성, FAQ)에 허용·금지 언급이 없다 → 미확인.
  - 관련 정책: 하드웨어 접근 정책은 모바일 전용 API가 차량에서 지원되지 않거나 제한될 수 있으니 지원 여부를 확인한 뒤 선택(Optional) 구조로 만들라고 한다. [hw-access]
  - Maps/Navigation 앱 가이드: "Pleos Connect 및 AAOS 공식 API 사용을 권장합니다.", "비공식 센서/API 사용은 금지되어 있습니다." (Maps/Navigation 앱 대상 문서) [navi-app-guide]
- **NaviHelper의 현재 위치:** `getCurrentLocationInfo()` → `onCurrentLocationInfo(CurrentLocationInfo)`. 권한은 `NAVI_ROUTE`(`get*Info()`). 내비게이션 탑재 차량에서만 호환된다. `CurrentLocationInfo` 필드는 미확인. [navi-perm] [api-compat] [gh-navi-src]

### TextToSpeech SDK (v3.3.1) [tts-intro] [tts-import] [tts-perm]

- 텍스트를 음성으로 합성해 차량 오디오로 재생한다. Pleos Connect Speech Service에 연결된다. [tts-intro]
- 모드: `HYBRID`(기본), `SERVER`, `ON_DEVICE`. 서버 기반 모드(`HYBRID`, `SERVER`)는 "Pleos Playground의 My Project 자격 증명을 사용한 앱 등록"이 필요하고, `ON_DEVICE`는 필요 없다. [tts-intro]
- 의존성:

```kotlin
implementation("ai.pleos.playground:TextToSpeech:3.3.1")
```

- 권한: `<uses-permission android:name="pleos.car.permission.TTS_SERVICE" />` [tts-perm]
- 오류 콜백이 `PERMISSION_DENIED`면 매니페스트 선언과 실행 환경의 권한 허용을 확인한다. [tts-perm]
  - 권한 페이지에는 보호 수준(normal/dangerous), 런타임 요청 필요 여부, 별도 승인 필요 여부가 적혀 있지 않다. [tts-perm]
- 생성 (원문) [tts-intro]:

```kotlin
import ai.pleos.playground.tts.TextToSpeech
import ai.pleos.playground.tts.constant.Mode

val textToSpeech = TextToSpeech(applicationContext, Mode.HYBRID)
```

  - 원문: "`Mode`를 생략하면 `HYBRID`가 적용됩니다." 모드는 생성자 인자로 넘긴다. TTS 프로그래밍 가이드 페이지(초기화, 합성 시작·중지, 앱 등록)에는 모드를 바꾸는 API가 없다. [tts-intro] [tts-init] [tts-start] [tts-appreg]
  - 원문: "합성된 음성을 차량의 오디오 시스템으로 재생합니다." [tts-intro]
- 초기화·해제: `textToSpeech.initialize(object : OnInitializeListener { ... })` 또는 `textToSpeech.initialize()`. 해제는 `textToSpeech.removeEventListener(eventListener)` 후 `textToSpeech.release()`. release 후 다시 쓰려면 새 객체를 만든다. [tts-init]
- 언어: `setLocale(locale: Locale)`. `speak()` 전에 호출한다. 예: `textToSpeech.setLocale(Locale.KOREA)` [tts-locale]
  - 지원 locale: `ko-KR`, `en-US`, `fr-CA`, `es-US`, `es-MX` [tts-locale]
  - 호출하지 않으면 같은 앱에서 이전에 설정한 값, 그것도 없으면 시스템 locale을 쓴다. [tts-locale]
- 합성 (원문): `textToSpeech.speak("안녕하세요. 무엇을 도와드릴까요?")`, 중지 `textToSpeech.stop()`. 여러 조각은 마지막 조각 전까지 `isFinal = false`. 중지되면 `EventListener`의 `onStop()`이 호출된다. [tts-start]
- 앱 등록 (서버 모드만): 초기화 후 `textToSpeech.registerApp(clientID = clientId, clientSecret = clientSecret, listener = object : OnServerConnectionListener { onConnected() / onFailed(errorType: ErrorType) })`. `onConnected()` 이후에 서버 요청을 시작한다. `ON_DEVICE`만 쓰면 등록하지 않아도 된다. [tts-appreg]
- Pleos Maven 게시 버전: 2.1.2, 2.1.3, 2.1.3.1, 2.1.3.2, 2.1.5.1, 3.3.0, 3.3.1 (latest/release 3.3.1, lastUpdated 2026-10-13). [maven-meta]
- 호환성: API 호환성 표에서 TextToSpeech·SpeechToText SDK는 "전체 호환". [api-compat]

### SpeechToText SDK (v3.3.1) [stt-intro] [stt-import] [stt-perm]

- 음성을 텍스트로 바꿔 돌려준다. 명령을 실행하지는 않는다. 인식한 명령을 실행하려면 Gleo Interface가 필요하다. [stt-intro]
- 진입 클래스: `ai.pleos.playground.stt.SpeechToText` [stt-intro]
- 모드: `HYBRID`(기본, 서버 우선 후 네트워크 실패 시 온디바이스), `SERVER`, `ON_DEVICE`. 서버 기반 모드는 My Project 자격 증명으로 앱 등록이 필요하다. `ON_DEVICE`만 쓰면 등록이 필요 없다. [stt-intro]
  - `ON_DEVICE` 원문: 차량 내 On-device 엔진으로 인식하며 네트워크와 앱 등록이 필요 없다. "이 모드만 사용하는 경우 앱 등록은 필요하지 않습니다." [stt-intro]
  - `SERVER` 원문: "네트워크 연결과 앱 등록이 필요하며, 네트워크 연결에 실패해도 On-device 엔진으로 전환하지 않습니다." [stt-intro]
- 의존성:

```kotlin
implementation("ai.pleos.playground:SpeechToText:3.3.1")
```

- 권한: `<uses-permission android:name="pleos.car.permission.STT_SERVICE" />` [stt-perm]
  - 권한 페이지에는 보호 수준, 런타임 요청 필요 여부, `android.permission.RECORD_AUDIO` 필요 여부, 별도 승인 필요 여부가 적혀 있지 않다. 선언하지 않으면 초기화나 API 호출이 실패할 수 있고 `PERMISSION_DENIED`가 올 수 있다. [stt-perm]
- 생성 (원문) [stt-intro]:

```kotlin
import ai.pleos.playground.stt.SpeechToText
import ai.pleos.playground.stt.constant.Mode
val speechToText = SpeechToText(applicationContext, Mode.HYBRID)
```

  - 원문: "`Mode`를 생략하면 `HYBRID`가 적용됩니다." [stt-intro]
  - 문서 코드에 나온 상수는 `Mode.HYBRID`뿐이다. `Mode` enum 페이지 본문이 비어 있어 `Mode.ON_DEVICE`라는 상수 이름은 코드로 확인하지 못했다. intro 본문의 모드 이름 표기는 `ON_DEVICE`다. [stt-intro] [stt-api-ref]
  - **산출물로 확인 (2026-11-03):** `SpeechToText-3.3.1.aar`의 `ai.pleos.playground.stt.constant.Mode`에는 `HYBRID`, `SERVER`, `ON_DEVICE` 세 상수가 있다 (`javap`). [aar-stt]
- 사용 순서: 생성 → `initialize()` → `addListener()` → (선택) `setLocale()` → (서버 모드면) 앱 등록 → `request()` → `stop()` → `removeListener()` 후 `release()` [stt-intro]
- 초기화·해제: `speechToText.initialize(object : OnInitializeListener { ... })` 또는 `speechToText.initialize()`. 해제는 `speechToText.removeListener(resultListener)` 후 `speechToText.release()`. release 후 다시 쓰려면 새 객체를 만든다. [stt-init]
- 언어: `setLocale(locale: Locale)`, 예 `speechToText.setLocale(Locale.KOREA)`. 지원 locale은 `ko-KR`, `en-US`, `fr-CA`, `es-US`, `es-MX`. 미호출 시 이전 설정값, 없으면 시스템 locale. [stt-locale]
- 결과 리스너 `ResultListener` (원문 시그니처) [stt-listener]:
  - `override fun onUpdated(stt: String, completed: Boolean)`: 인식 텍스트 갱신. `completed`가 `true`면 그 결과가 완료된 것.
  - `override fun onError(errorType: ErrorType)`
  - 그 밖에 `onReady()`, `onStartedRecognition()`, `onUpdatedEpdData(on, off)`, `onUpdatedRms(rms)`, `onEndedRecognition()` (이름만 확인, 시그니처 미확인)
  - 원문: "인식 이벤트를 놓치지 않도록 `request()` 또는 `sendAudio()`를 호출하기 전에 리스너를 등록하세요."
- 시작·중지 (원문): `speechToText.addListener(resultListener)` → `speechToText.request()`, 중지 `speechToText.stop()`. 원문: "서버 기반 인식이 필요한 모드에서는 앱 등록 또는 UA 인증도 완료해야 합니다." [stt-start]
- 앱이 가진 PCM 보내기: `sendAudio(ByteArray)` 후 `completeAudioSend()`. **`ON_DEVICE` 모드에서만** 쓸 수 있다. 예시 포맷은 16 kHz, 16-bit, mono PCM, 100ms(3,200 bytes) 청크 (필수 형식이라고 하지는 않음). [stt-audio]
- 앱 등록 (서버 모드만): `speechToText.registerApp(clientID = clientId, clientSecret = clientSecret, listener = object : OnServerConnectionListener { onConnected() / onFailed(errorType: ErrorType) })`. Client ID/Secret은 My Project 생성 시 발급된다. [stt-appreg]
- `ErrorType` 값: `AUDIO_ERROR`, `INVALID_REQUEST_ERROR`, `MIC_ERROR`, `NETWORK_ERROR`, `NOT_SUPPORTED_API`("현재 모드 또는 실행 환경에서 지원하지 않는 API입니다."), `ON_DEVICE_ENGINE_ERROR`, `PERMISSION_DENIED`, `SERVER_INTERNAL_ERROR`, `TIMEOUT`, `UNAUTHENTICATED`, `UNAVAILABLE_LANGUAGE`, `UNKNOWN` [stt-error]
- `SpeechToText` 클래스 멤버 (API Reference 목차): `Companion`, `Constructor`, `addListener`, `completeAudioSend`, `initialize`, `registerApp`, `release`, `removeListener`, `request`, `sendAudio`, `setLocale`, `stop`. 모드를 바꾸는 setter는 없다. 상세 페이지 본문은 비어 있다. [stt-api-ref]
- Pleos Maven 게시 버전: 2.1.2, 2.1.3, 2.1.3.1, 2.1.3.1.1, 2.1.3.2, 3.3.0, 3.3.1 (latest/release 3.3.1, lastUpdated 2026-10-13). [maven-meta]

### STT/TTS 사용 조건 정리 (2026-11-03 조사)

- **콘솔 "준비 중"의 의미:** 신청 버튼이 없고 "Comming Soon" 배지가 붙는다(문서 표기 그대로). 원문: "해당 상태에서는 신청할 수 없으므로 **신청 필요** 상태로 변경된 후 진행하시기 바랍니다." [create-project]
- **Client ID/Secret:** 콘솔 프로젝트 정보의 SDK 항목에서 확인한다. 원문: "앱 등록 등 서버 연결을 요구하는 SDK를 사용할 때 필요한 정보입니다." 해당 SDK는 TextToSpeech, SpeechToText. [create-project]
- **`ON_DEVICE`와 "준비 중"의 관계:** STT/TTS 문서는 `ON_DEVICE`에 앱 등록이 필요 없다고만 하고, 콘솔 상태와의 관계는 어느 문서에도 없다. 권한(`STT_SERVICE`/`TTS_SERVICE`)에 "사전 협의 및 승인"이 필요한지도 STT/TTS 권한 페이지에는 없다. 반면 SDK 개요는 "퍼미션은 Pleos App Market과 사전 협의 및 승인된 범위 내에서만 사용할 수 있습니다."라고 한다. [sdk-overview] → **심사 제출 앱에서 `ON_DEVICE`를 써도 되는지는 미확인.**
- **에뮬레이터:** STT/TTS intro 원문 "SpeechToText SDK를 사용하려면 Android Studio에 Pleos Connect Emulator를 설치하고 AVD를 설정합니다." 설정 항목에 "마이크 접근 설정", "Cold boot 설정"이 있다. [stt-intro] [tts-intro] 에뮬레이터에 On-device 엔진이 들어 있는지, 실제로 인식되는지는 문서에 없다 → 미확인 (GPU PC에서 직접 실행해 확인 필요). 마이크 설정은 [emulator-setup.md](emulator-setup.md#주의) 참고.
- **Android 표준 `SpeechRecognizer` / `android.speech.tts.TextToSpeech`:** Pleos 문서(SDK 개요, SDK API 정책, 하드웨어 접근 정책, API 호환성, FAQ, 오디오 포커스 가이드, TTS 선행 묵음 가이드)에 허용·금지 언급이 없다 → 미확인.
  - 관련 정책 문장: "동일하거나 유사한 기능의 API가 AAOS 표준에 이미 존재하는 경우, 해당 표준 API 사용을 권장하며 Pleos Connect SDK에서는 중복 기능을 제공하지 않습니다." [sdk-api-policy] Pleos가 STT/TTS SDK를 따로 제공하므로, 이 문장만으로 표준 API 사용 가능 여부를 판단할 수 없다.
- **음성 출력의 오디오 포커스:** 재생 전 `AudioAttributes` + `AudioFocusRequest`로 포커스를 요청한다. 컨텍스트 매핑에서 NAVIGATION은 `NAVIGATION`, `ASSISTANCE_NAVIGATION_GUIDANCE`, VOICE_COMMAND는 `VOICE_COMMAND`, `ASSISTANT`. TTS SDK가 어느 usage로 재생하는지는 문서에 없다. 자세한 내용은 [ux-guidelines.md](ux-guidelines.md#오디오-포커스와-음성-안내) [audio-focus]

### Gleo Interface (음성 명령으로 앱 기능 호출) [gleo-core] [gleo-deliver]

- Gleo AI가 앱이 정의한 function을 호출하는 function calling 방식이다. 앱은 App Schema(JSON)를 등록하고, Gleo AI는 Gleo Action으로 앱을 부르며, 앱은 Callback으로 응답한다. [gleo-core]
- 스키마 전달: 파일명은 "`schema.json`이어야 합니다.", "파일은 앱의 assets/ 디렉토리에 위치시켜야 합니다." 원문: "App Schema를 전달받으면, 당사 가이드라인을 준수하는지 검토합니다." "승인 후에는 통합이 자동으로 활성화됩니다." [gleo-deliver]
- 신청 자격, 계약 필요 여부, 사용 가능 상태는 문서에 없다 → 미확인.

### 전화 (2026-11-03 조사)

- Pleos 문서에 서드파티 앱의 전화 걸기(`Intent.ACTION_DIAL`/`ACTION_CALL`, 블루투스 연결 폰 통화)에 대한 허용·금지 문장이 없다 → 미확인.
- 관련 사실:
  - 하드웨어 접근 정책: Telephony(전화)는 차량에서 제한될 수 있는 모바일용 API로 나열되어 있다. [hw-access]
  - 지역별 주행 규제 표: 북미는 주행 중 "다이얼패드" 사용 제한(`UX_RESTRICTIONS_NO_DIALPAD(1)`). [driving-policy] → [ux-guidelines.md](ux-guidelines.md#지역별-기능-허용-범위-driving-상태)
  - 오디오 포커스 가이드에 CALL 컨텍스트(`CALL`, `VOICE_COMMUNICATION`, `VOICE_COMMUNICATION_SIGNALING`)가 있다. 통화 중 MUSIC 요청은 거부(R)된다. [audio-focus]
  - 에뮬레이터 2.1.0에서 "블루투스 동글 연동 제공 종료". 에뮬레이터로 폰 연결 통화를 시험할 수 없다. [emulator-release-note]
  - 디자인 가이드의 System Color 토큰 분류에 "Phone"이 있다. 서드파티 앱의 전화 연동 방법은 문서에 없다. [design-colors]

## 권한

- 예: `<uses-permission android:name="pleos.car.permission.CAR_DOORS" />` [getting-started]
- 권한은 Pleos App Market과 사전 협의·승인을 받아야 한다. 승인되지 않은 권한을 선언하거나 해당 API를 호출하면 앱 심사에서 탈락한다. [sdk-overview]
- 사용하는 도메인에 맞는 권한만 선언한다. [vehicle-permission]

### Vehicle SDK 도메인별 대표 읽기 권한 [vehicle-permission]

| 도메인 | `android:name` |
|---|---|
| Door | `pleos.car.permission.CAR_DOORS` |
| Window, Wiper | `pleos.car.permission.CAR_WINDOWS` |
| HVAC | `pleos.car.permission.CAR_CLIMATE` |
| DrivingMode, Brake, Odometer | `pleos.car.permission.CAR_DRIVE` |
| EvBattery, Fuel | `pleos.car.permission.CAR_ENERGY` |
| CarInfo, Display | `pleos.car.permission.CAR_INFO` |
| Light, TurnSignal | `pleos.car.permission.CAR_LIGHTS` |
| SideMirror | `pleos.car.permission.CAR_MIRRORS` |
| Safety, Tire | `pleos.car.permission.CAR_SAFETY` |
| Seat | `pleos.car.permission.CAR_SEATS` |
| Toll | `pleos.car.permission.CAR_TOLL` |

- 제어(set)는 `CONTROL_CAR_*` 접두사를 쓴다. 문서 예시는 `CONTROL_CAR_DOORS` 하나뿐이고 전체 목록은 `PleosPermission` 레퍼런스에 있다고 한다. [vehicle-permission]
- 표는 대표값이다. 호출 전에 각 API의 `@RequiresPermission`을 확인한다. [vehicle-permission]
- 권한이 없으면 `onFailed`에 `Exception`이 오고, "`VehicleException`인 경우 `ERROR_SECURITY_PERMISSION`에 해당할 수 있습니다." [vehicle-permission]

> **불일치:** 공식 vehicle 샘플의 매니페스트는 위 표에 없는 권한도 선언한다 [gh-vehicle]:
> `pleos.car.permission.CAR_CONNECT_VENDOR_CONTROL`, `CAR_MILEAGE`, `CAR_POWERTRAIN`, `CAR_SPEED`, `CAR_TIRES`, `READ_CAR_DISPLAY_UNITS`, `READ_CAR_STEERING`, `READ_ADAS_STATES`, `READ_WINDSHIELD_WIPERS`, `CONTROL_WINDSHIELD_WIPERS`.
> 매니페스트 주석은 이 권한들을 "Public read permissions (Vehicle Service protectionLevel="normal")", "Public control permissions (Vehicle Service protectionLevel="normal")"로 설명한다.
> 어느 API가 어느 권한을 요구하는지는 문서 표와 샘플이 다르므로, 실제로 쓸 API의 `@RequiresPermission`을 확인하고 **AGENTS.md 규칙 2에 따라 사용자 승인을 먼저 받는다.**

### 기타 SDK 권한 요약

| SDK | 권한 | 출처 |
|---|---|---|
| NaviHelper | `pleos.car.permission.NAVI_ROUTE`, `NAVI_ROUTE_SEARCH`, `NAVI_CUSTOM_MAP` | [navi-perm] |
| FusedLocation | `pleos.car.permission.FUSED_LOCATION` (dangerous) | [fused-perm] |
| TextToSpeech | `pleos.car.permission.TTS_SERVICE` | [tts-perm] |
| SpeechToText | `pleos.car.permission.STT_SERVICE` | [stt-perm] |

## 실행 환경 정보 (FAQ) [faq]

- 실행 플랫폼: `android.os.Build.DEVICE` (`ro.product.device`) 값이 `CONNECT_S`, `CONNECT_L`, `CONNECT_W`. "이 값은 실차에서만 유효하게 제공되므로, 에뮬레이터에서는 플랫폼을 구분하는 용도로 사용할 수 없습니다."
- 지역 코드: `product.sw.target_region` 값이 `KOR`, `EUR`, `USA`/`CAN`. 이 값으로 "지역별 UX 정책을 적용해야 합니다."
- 내비게이션 탑재 여부를 확인하는 공개 API는 없다 ("현재 퍼블릭 오픈하지 않고 있습니다").
- 배터리 잔량은 Vehicle SDK의 `getEvBatteryStateOfCharge`로 읽는다.
- 지원 코덱은 플랫폼마다 다르다. "에뮬레이터의 지원 결과를 실차 환경에 그대로 적용하지 마시기 바랍니다."

## 미확인 (TODO: `pleos-docs-researcher` 서브에이전트로 채운다)

- [x] FusedLocation / NaviHelper / TextToSpeech / SpeechToText 의존성 좌표 (2026-11-03)
- [x] 속도·기어·주행 상태 API: Vehicle SDK에는 없고 AAOS Car API를 쓴다 (2026-11-03). 남은 것은 아래 항목.
- [x] API별 필요 권한 목록: 대표 표 확보 (2026-11-03). 전체 상수 목록은 아래 항목.
- [x] compileSdk / targetSdk / AGP / Kotlin / JDK: 공식 샘플 기준값 확보 (2026-11-03). Pleos "권장값" 명시는 없음.
- [x] Jetpack Compose: 문서 언급은 없고 공식 샘플 3개가 모두 Compose (2026-11-03)
- [x] 화면 방향 → [ux-guidelines.md](ux-guidelines.md#화면-구성) (2026-11-03). 해상도·밀도는 아래 항목.
- [x] 운전자 주의분산 가이드, 주행 중 제한 정책 → [ux-guidelines.md](ux-guidelines.md) (2026-11-03)
- [x] Virtual Console로 차량 데이터 시뮬레이션하는 방법 → [emulator-setup.md](emulator-setup.md#virtual-console-차량-데이터-시뮬레이션) (2026-11-03)
- [x] Vehicle SDK 샘플 앱 (`/docs/pleos-only/tutorial/vehicle-sdk-sample-app`) (2026-11-03)
- [x] 앱 카테고리 → [ux-guidelines.md](ux-guidelines.md#앱-카테고리와-주행-중-허용-범위) (2026-11-03)
- [ ] DrivingMode 메서드 시그니처, `@RequiresPermission`, `DrivingModeType`/`CruiseControl` 상수 값. API Reference 상세 페이지는 클라이언트 렌더링이라 WebFetch로 본문이 비어 있다 (`.../driving-mode/get-current-driving-mode-state`, `.../constant.control/driving-mode-type/entry`, `/en/` 경로 모두). 브라우저로 직접 확인해야 한다.
- [ ] Brake 도메인의 주차 브레이크 API (intro와 API 목록이 다름)
- [ ] `PleosPermission` 전체 상수 목록 (`.../constant/pleos-permission/entry`도 본문이 비어 있음). 샘플 매니페스트의 `CAR_SPEED`, `CAR_POWERTRAIN` 등이 어느 API에 쓰이는지.
- [ ] AAOS Car API(`android.car`, `CarPropertyManager`, `CarUxRestrictionsManager`)를 쓰기 위한 Gradle 설정 (`useLibrary` 등). Pleos 문서에 없다.
- [ ] 서드파티 앱이 Pleos에서 `Car.PERMISSION_SPEED`(dangerous), `Car.PERMISSION_POWERTRAIN`을 받을 수 있는지, 심사에서 별도 승인이 필요한지
- [ ] Android for Cars App Library를 써도 되는지 (허용·금지 모두 문서에 없음)
- [ ] Pleos가 명시한 "권장" compileSdk/targetSdk/AGP/Kotlin/JDK (문서는 샘플 기준값만 제시)
- [ ] 화면 해상도(px)와 밀도(dpi) → [ux-guidelines.md](ux-guidelines.md#미확인)
- [x] `Vehicle` import 경로와 `initialize()`/`release()` 시그니처, 2.3.2 해결 여부 → 위 "2.3.2 AAR에서 직접 확인한 것" (2026-11-03)
- [x] 차량 서비스가 없는 기기에서 `initialize()`의 동작 → 위 "차량 서비스가 없는 기기에서의 동작" (관찰, 2026-11-03)
- [ ] 차량 서비스 연결 실패를 앱이 알 수 있는 방법 (도메인 API `onFailed`로 오는지, 별도 콜백이 있는지)
- [ ] Gear 도메인(`getGear()`): 필요한 권한, `VehicleGear` 값, 실제 지원 여부. 문서에는 없고 AAR에만 있다.
- [ ] 같은 `Vehicle` 인스턴스를 `release()`한 뒤 다시 `initialize()`해도 되는지. 우리 앱은 매번 새로 만들어 이 문제를 피한다.

### PawCare 기획 관련 (2026-11-03 조사)

- [x] STT/TTS 생성자, 모드 지정, `setLocale`과 지원 언어(`ko-KR` 포함), 결과 리스너, 앱 등록 방법 → 위 TextToSpeech / SpeechToText 절
- [x] NaviHelper 목적지 요청(`requestRoute(RouteInfo)`), 도착 이벤트(`onDestinationArrived`), 권한별 API → 위 NaviHelper 절
- [ ] 콘솔에서 "준비 중"인 STT/TTS를 `ON_DEVICE` 모드로 심사 제출 앱에서 써도 되는지. `STT_SERVICE`/`TTS_SERVICE` 권한에 사전 승인이 필요한지. (문서에 없음. Pleos 문의 필요)
- [ ] Pleos 에뮬레이터에 On-device STT/TTS 엔진이 있어 실제로 인식·합성되는지 (GPU PC에서 직접 실행해 확인)
- [x] `Mode.ON_DEVICE` 상수 이름 → `HYBRID`, `SERVER`, `ON_DEVICE` 확인 (AAR `javap`, 2026-11-03)
- [ ] Android 표준 `SpeechRecognizer`, `android.speech.tts.TextToSpeech`를 Pleos에서 써도 되는지, `RECORD_AUDIO`가 필요한지
- [ ] 서드파티 앱의 전화 걸기 (`ACTION_DIAL`/`ACTION_CALL`, 블루투스 연결 폰 통화) 허용 여부
- [ ] Android 표준 `LocationManager`(`ACCESS_FINE_LOCATION`) 위치를 Pleos에서 받을 수 있는지
- [x] NaviHelper 데이터 클래스 필드 → NaviHelper 절 "산출물로 확인" (`TripInfo.eta`, `DrivingState.duration/distance`, `CurrentLocationInfo` 좌표). 단위는 미확인 (2026-11-03)
- [ ] `RouteInfo`의 `poiId`/`poiSubId`가 필수인지, 외부 장소 검색 결과(좌표+이름)만으로 경로를 요청할 수 있는지
- [ ] `requestTrip(TripRequest)`의 동작 (설명 문서 없음)
- [ ] 샘플 매니페스트의 `NAVI_CUSTOM_ROUTE`, `NAVI_CUSTOM_ETC`가 어느 API에 필요한지
- [ ] Pleos 에뮬레이터에서 NaviHelper 요청을 받는 기본 Navigation 앱이 동작하는지
- [ ] Gleo Interface(음성 명령 연동) 신청 자격과 사용 가능 상태

## 출처

- [contest-faq] https://appdevchallenge.auto-hmg.io/faq (대회 사이트, 2026-11-03 확인)
- [contest-dev] https://appdevchallenge.auto-hmg.io/guide/development (대회 사이트, 2026-11-03 확인)
- [setup] https://document.pleos.ai/docs/connect/guide/getting-started/application-development/setup-connect-sdk-emulator
- [getting-started] https://document.pleos.ai/docs/pleos-only/getting-started
- [project-setup] https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/project-setup
- [vehicle-intro] https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/intro
- [sdk-overview] https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/
- [create-project] https://document.pleos.ai/docs/pleos-only/create-my-project
- [console-2026-11-03] Pleos Playground 콘솔 (https://pleos.ai/playground, 로그인 필요). 2026-11-03 팀원이 직접 확인한 SDK 항목 목록
- [app-submission] https://document.pleos.ai/docs/pleos-only/distribute/app-submission-guide
- [planning-guide] https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/intro
- [sample-app] https://document.pleos.ai/docs/pleos-only/tutorial/vehicle-sdk-sample-app
- [app-structure] https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/app-structure-screen-model
- [app-category] https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/app-category
- [sdk-api-policy] https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/pleos-connect-sdk-api
- [hw-access] https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/platform-api-architecture
- [driver-distraction] https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/driver-distraction-guideline
- [driving-restriction-dev] https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/driving-restriction-development-guide
- [faq] https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/vehicle-app-faq
- [vehicle-permission] https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle-permission
- [vehicle-api] https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.api/entry
- [driving-mode-api] https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.api/driving-mode/entry
- [brake-api] https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.api/brake/entry
- [odometer-api] https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.api/odometer/entry
- [constant-control] https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.constant.control/entry
- [navi-intro] https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/intro
- [navi-import] https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/importing-sdk
- [navi-perm] https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/declaring-permissions
- [fused-intro] https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/FusedLocation/intro
- [fused-setup] https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/FusedLocation/project-setup
- [fused-perm] https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/FusedLocation/declaring-permissions
- [tts-intro] https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech/intro
- [tts-import] https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech/importing-sdk
- [tts-perm] https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech/declaring-permissions
- [stt-intro] https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/SpeechToText/intro
- [stt-import] https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/SpeechToText/importing-sdk
- [stt-perm] https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/SpeechToText/declaring-permissions
- [stt-init] https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/SpeechToText/initialization-and-release
- [stt-listener] https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/SpeechToText/result-listeners
- [stt-locale] https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/SpeechToText/set-locale
- [stt-appreg] https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/SpeechToText/app-registration
- [stt-start] https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/SpeechToText/start-and-stop
- [stt-audio] https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/SpeechToText/sending-audio-data
- [stt-error] https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/SpeechToText/error-code
- [stt-api-ref] https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/SpeechToText/entry (하위: `.../speech-to-text/ai.pleos.playground.stt/speech-to-text/entry`, `.../ai.pleos.playground.stt.constant/mode/entry`)
- [tts-init] https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech/initialization-and-release
- [tts-locale] https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech/set-locale
- [tts-start] https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech/start-and-stop
- [tts-appreg] https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech/app-registration
- [navi-init] https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/initialization-and-release
- [navi-events] https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/event-listeners
- [navi-dest] https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/destinations
- [navi-routes] https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/routes
- [navi-api-ref] https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/entry (하위: `.../navi-helper/ai.pleos.playground.navi.helper/navi-helper/entry`, `.../ai.pleos.playground.navi.data/entry`)
- [api-compat] https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/api-compatibility-availability
- [navi-app-guide] https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/navigation-app-development-guide
- [audio-focus] https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/audio-focus-development-guide/audio-focus-development
- [driving-policy] https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/driving-restiction-policy
- [design-colors] https://document.pleos.ai/docs/connect/guide/docs-design/foundations/colors
- [emulator-release-note] https://document.pleos.ai/docs/pleos-only/release-note/emulator-release-note
- [gleo-core] https://document.pleos.ai/docs/pleos-only/app-integration-guide/guide/core-concepts
- [gleo-deliver] https://document.pleos.ai/docs/pleos-only/app-integration-guide/guide/deliver-schema
- 공식 샘플 저장소 ([sample-app] 페이지가 링크한 Pleos-Playground 조직, main 브랜치):
  - [gh-vehicle] https://github.com/Pleos-Playground/pleos-sample-vehicle-app (`README.md`, `app/build.gradle.kts`, `gradle/libs.versions.toml`, `gradle/wrapper/gradle-wrapper.properties`, `app/src/main/AndroidManifest.xml`)
  - [gh-navi] https://github.com/Pleos-Playground/pleos-sample-navi-app (`app/build.gradle.kts`, `gradle/libs.versions.toml`)
  - [gh-fused] https://github.com/Pleos-Playground/pleos-sample-fusedlocation-app (`app/build.gradle.kts`, `gradle/libs.versions.toml`)
  - [gh-vehicle-src] https://github.com/Pleos-Playground/pleos-sample-vehicle-app (`app/src/main/java/.../MainViewModel.kt`, `.../vehicle/VehicleClient.kt`, 커밋 959981d)
  - [gh-navi-src] https://github.com/Pleos-Playground/pleos-sample-navi-app (`README.md`, `app/src/main/AndroidManifest.xml`, `app/src/main/java/ai/pleos/playground/navidevapp/screen/vm/NaviHelperViewModel.kt`, `.../data/model/SdkApi.kt`, `.../data/sample/NaviTestData.kt`, main 브랜치, 2026-11-03)
  - 조직 저장소는 5개다: vehicle, fusedlocation, navi 샘플과 `pleos-guide`, `pleos-playground-api`. **STT/TTS 공식 샘플은 없다.** (2026-11-03)
- 산출물:
  - [aar-2.3.2] Pleos Maven `ai.pleos.playground:Vehicle:2.3.2` AAR (`classes.jar`를 `javap`으로, `AndroidManifest.xml`, `proguard.txt`). 2026-11-03 확인.
  - [aar-navi-data] Pleos Maven `ai.pleos.playground:navi-data:3.0.4` AAR과 `NaviHelper:3.0.5` pom (`javap`). 2026-11-03 확인.
  - [aar-stt] Pleos Maven `ai.pleos.playground:SpeechToText:3.3.1` AAR (`javap`). 2026-11-03 확인.
  - [maven-meta] Pleos Maven `maven-metadata.xml` (2026-11-03 확인):
    - https://nexus-playground.pleos.ai/repository/maven-releases/ai/pleos/playground/SpeechToText/maven-metadata.xml
    - https://nexus-playground.pleos.ai/repository/maven-releases/ai/pleos/playground/TextToSpeech/maven-metadata.xml
    - https://nexus-playground.pleos.ai/repository/maven-releases/ai/pleos/playground/NaviHelper/maven-metadata.xml (versions 2.0.2, 2.0.3, 3.0.5; latest 3.0.5; lastUpdated 2026-10-19)
- 관찰:
  - [obs-no-service] 우리 앱(Real 모드)을 일반 폰 에뮬레이터(`my_avd`, API 34 google_apis)에서 실행하고 `adb logcat`으로 확인. 2026-11-03.
- Pleos 외 출처 (Pleos [faq]가 직접 링크한 Google 공식 문서):
  - [g-vehicle-property-ids] https://developer.android.com/reference/android/car/VehiclePropertyIds
  - https://developer.android.com/reference/android/car/hardware/property/CarPropertyManager
