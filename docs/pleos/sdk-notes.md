# Pleos Connect SDK: 검증된 사실

이 파일의 규칙:
- 공식 문서에서 확인한 것만 적고, 항목마다 출처를 단다.
- 확인하지 못한 것은 맨 아래 "미확인"에 둔다.
- 이 파일이나 공식 문서에 없는 API 이름·시그니처·의존성 좌표를 추측해서 코드에 쓰지 않는다.
- UX·주행 규제·디자인 가이드는 [ux-guidelines.md](ux-guidelines.md)에 둔다.

마지막 갱신: 2026-11-05

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
  - 기술적으로는 STT `ON_DEVICE`가 지금도 Pleos 에뮬레이터에서 동작한다 (2026-11-05 관찰, 아래 "STT `ON_DEVICE` 실행 확인"). 써도 되는지(허가)는 여전히 B2 답에 달렸다.

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
    >
    > **2026-11-05 재확인: 여전히 공식 문서와 공식 샘플 어디에도 Gear가 없다.**
    > - `vehicle.api` 패키지 목록은 19개 그대로다. `.../ai.pleos.playground.vehicle.api/gear/entry`는 "Page Not Found"(404)다. [vehicle-api]
    > - `ai.pleos.playground.vehicle.constant` 패키지에는 `ErrorCode`, `PleosPermission` 둘만 있다. `VehicleGear`는 없다. [constant-pkg]
    > - `ai.pleos.playground.vehicle.listener` 패키지의 리스너 67개에 `GearSelectionListener`가 없다. [listener-pkg]
    > - SDK 릴리스 노트 v2.3.2(2026-10-19)의 Breaking change / New API / Changed API / Deprecated API / Shutdown API 표 어디에도 Gear가 없다. [sdk-release-note]
    > - 공식 vehicle 샘플의 API 카탈로그(`VehiclePublicApiCatalog`, `enum class VehicleDomain`)는 19개 도메인만 다루고 Gear가 없다. [gh-vehicle-catalog]
    > - 샘플 매니페스트에는 `pleos.car.permission.CAR_POWERTRAIN`이 있지만, 이것이 Gear에 쓰인다는 문서·주석은 없다. [gh-vehicle]
    > - 그래서 Gear는 "문서에 없는 비공개 API"로 본다. 권한·값·지원 여부는 미확인으로 남긴다.
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
- **Vehicle SDK v2.3.2에서 속도 리스너가 삭제되었다.** SDK 릴리스 노트의 "Shutdown API" 표 (원문) [sdk-release-note]:
  - 대상 API: "VehicleSpeedListener"
  - 변경 내용: "삭제" / "리스너와 onSpeedUpdated 제거"
  - 필요한 대응: "해당 타입·콜백을 사용하는 코드 수정"
  - 이유와 대체 API는 적혀 있지 않다. Vehicle SDK로 속도를 받는 길이 없다는 위 기록과 맞는다.

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
- `Car` 클래스의 권한 상수 (원문) [g-car]:

| 상수 | 값 | 설명 (원문) |
|---|---|---|
| `Car.PERMISSION_POWERTRAIN` | `"android.car.permission.CAR_POWERTRAIN"` | "Normal permission necessary to access car's powertrain information." |
| `Car.PERMISSION_SPEED` | `"android.car.permission.CAR_SPEED"` | "Dangerous permission necessary to access car's speed." |
| `Car.PERMISSION_ENERGY` | `"android.car.permission.CAR_ENERGY"` | "Dangerous permission necessary to access car's energy information." |

- `CarPropertyManager`는 `Car.PROPERTY_SERVICE`(`"property"`, "Service name for CarPropertyManager")로 얻는다. [g-car] Gradle 설정과 `Car` 객체 만들기는 아래 `CarUxRestrictions` 절과 같다.
- **2026-11-05 재확인: Pleos 문서에 `Car.PERMISSION_POWERTRAIN`·`android.car.permission.*`·`GEAR_SELECTION`을 서드파티 앱이 받을 수 있는지에 대한 문장이 없다.**
  - 확인한 페이지: SDK API 정책, 하드웨어 접근 정책, Vehicle 권한, FAQ, 앱 제출 가이드, 주행 규제 개발 가이드. [sdk-api-policy] [hw-access] [vehicle-permission] [faq] [app-submission] [driving-restriction-dev]
  - SDK API 정책에 있는 것은 표의 한 줄뿐이다: 표준 데이터 "속도, 기어 상태, 외기 온도, 연료 레벨 등"은 "AAOS Car API". [sdk-api-policy]
  - 앱 제출 폼에는 "앱 접근 권한"(필수, "앱이 접근해야 하는 기능이 있는 경우 선택 또는 입력")과 "SDK 추가 권한"(필수, "권한 신청이 필요한 SDK를 사용하는 앱일 경우, My Project > 프로젝트 정보 > 프로젝트 ID를 입력") 항목이 있다. AAOS 권한을 따로 다루는 문장은 없다. [app-submission]
  - 공식 vehicle 샘플 매니페스트는 `android.car.permission.*`을 하나도 선언하지 않는다. 대신 Pleos Vehicle Service의 `pleos.car.permission.CAR_POWERTRAIN`, `pleos.car.permission.CAR_SPEED`를 선언한다 (주석 "Public read permissions (Vehicle Service protectionLevel="normal")"). 이름이 비슷하지만 AAOS 권한과는 다른 이름공간이다. 어느 API에 쓰이는지는 문서에 없다. [gh-vehicle]

### 주행 규제(주행/정차) 판단은 `CarUxRestrictions`로 한다

- 앱은 `CarUxRestrictions`로 주행 중 제한 상태를 확인한다. 속도·기어로 직접 판단하라는 문장은 없다. 자세한 내용은 [ux-guidelines.md](ux-guidelines.md#주행-규제-caruxrestrictions)에 있다. [driver-distraction] [driving-restriction-dev]

#### Pleos 문서에는 구현 방법이 없다 (2026-11-05 조사)

- 다음 페이지 어디에도 Gradle 설정(`useLibrary`), `Car.createCar`, `CarUxRestrictionsManager`를 얻는 코드, 리스너 등록 코드, 필요한 권한이 없다:
  - 주행 규제 개발 가이드 (한글판·영문판) [driving-restriction-dev]
  - 주행 안전을 고려한 설계 [driver-distraction], FAQ [faq], SDK API 정책 [sdk-api-policy], 하드웨어 접근 정책 [hw-access], Vehicle 권한 [vehicle-permission]
- 주행 규제 개발 가이드에 있는 것 (원문) [driving-restriction-dev]:
  - "앱은 AAOS CarUxRestrictionsManager를 통해 CarUxRestrictions 값을 얻어 주행 규제 상태를 확인하고, 사양에 맞게 동작하도록 구현합니다."
  - "`CarUxRestrictionsManager`에 `registerListener()`를 등록하면 `onUxRestrictionsChanged()` 콜백으로 변경된 `CarUxRestrictions`를 수신합니다."
  - 동영상 재생 제한 예시 코드 하나 ([ux-guidelines.md](ux-guidelines.md#앱이-주행-규제-상태를-아는-방법-driver-distraction-driving-restriction-dev))
  - Google 레퍼런스 링크 두 개: "AAOS CarUxRestrictionsManager" → [g-uxr-manager], "CarUxRestrictions" → [g-uxr]
- 공식 샘플 저장소에도 없다.
  - vehicle 샘플의 `app/build.gradle.kts`에 `useLibrary`가 없다. 매니페스트에 `android.car.permission.*`과 `<uses-library android:name="android.car">`가 없다. [gh-vehicle]
  - 조직 저장소는 여전히 5개다. `pleos-guide` 저장소는 비어 있다("This repository is empty."). [gh-org]
- 그래서 아래 구현 방법은 **Google 공식 문서**를 출처로 적는다.

#### Gradle 설정 (Google 문서) [g-parked-aaos]

- 원문: "To listen to UX restrictions, first add a dependency on the `android.car` library in your app module's `build.gradle` file. This is an extension to the Android SDK that provides APIs that are specific to Android Automotive OS."

```kotlin
android {
    ...
    useLibrary("android.car")
}
```

- 이 문서에는 `<uses-library>` 매니페스트 항목이 나오지 않는다. Gradle 한 줄만 있다.

#### `CarUxRestrictionsManager` 얻기와 리스너 등록 (Google 문서 원문 코드) [g-parked-aaos]

```kotlin
val car = Car.createCar(context) ?: return
val carUxRestrictionsManager =
    car.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE) as? CarUxRestrictionsManager ?: return

// You can either read the state directly ...
val currentUxRestrictions = carUxRestrictionsManager.currentCarUxRestrictions

// or listen to state changes
carUxRestrictionsManager.registerListener { carUxRestrictions: CarUxRestrictions ->
    // Handle UX restrictions
}

// Don't forget to teardown and release resources when they're no longer needed
carUxRestrictionsManager.unregisterListener()
car.disconnect()
```

- 원문: "Use the `CarUxRestrictionsManager` to read UX restriction state. Don't attempt to determine the UX restriction state from other hardware state such as gear or speed, because UX restrictions may vary from display to display within a vehicle." [g-parked-aaos]
- 원문 (Tip): "You can use a runtime check to support both Android Automotive OS and other form factors with the same code by only calling `Car.createCar()` on Android Automotive OS devices." 같은 페이지의 판별 코드: `val isCar = packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)` [g-parked-aaos]
- 원문: "The only value provided by `CarUxRestrictions` that your app refers to is the return value of `isRequiresDistractionOptimization`. Other values are only relevant for activities that are marked as distraction optimized." [g-parked-aaos]
  - 이 문장은 parked 앱(동영상·게임·브라우저) 가이드에 있다. DO를 선언한 앱은 `getActiveRestrictions()`도 봐야 한다는 Pleos 문장과 같은 뜻이다. [driving-restriction-dev]

#### 레퍼런스 시그니처 (원문) [g-car] [g-car-lifecycle] [g-uxr-manager] [g-uxr-listener] [g-uxr]

`android.car.Car`:

```
public static Car createCar (Context context)
public static Car createCar (Context context, Handler handler)
public static Car createCar (Context context, Handler handler, long waitTimeoutMs, Car.CarServiceLifecycleListener statusChangeListener)
public Object getCarManager (String serviceName)
public T getCarManager (Class<T> serviceClass)
public boolean isConnected ()
public void disconnect ()
public static final String CAR_UX_RESTRICTION_SERVICE   // "uxrestriction"
public static final long CAR_WAIT_TIMEOUT_DO_NOT_WAIT   // 0
public static final long CAR_WAIT_TIMEOUT_WAIT_FOREVER  // -1
```

- 클래스 설명: "This API works only for devices with PackageManager.FEATURE_AUTOMOTIVE. Calling this API on a device with no such feature will lead to an exception." [g-car]
  - 우리 앱에 주는 뜻: 일반 폰 에뮬레이터에서 Real 모드로 `Car.createCar()`를 부르면 예외가 난다. `FEATURE_AUTOMOTIVE` 확인을 먼저 한다.
- `createCar(Context)`, `createCar(Context, Handler)`: "Creates new Car object which connected synchronously to Car Service and ready to use." 반환값 "Car object if operation succeeded, otherwise null." "Instance created with this should be disconnected from car service by calling disconnect() before the passed `Context` is released." `handler`가 null이면 콜백은 앱 메인 스레드에서 실행된다. [g-car]
- `createCar(Context, ServiceConnection)`과 `createCar(Context, ServiceConnection, Handler)`는 deprecated다. "use createCar(android.content.Context, android.os.Handler) instead." `connect()`도 deprecated다. [g-car]
- 4인자 `createCar(..., waitTimeoutMs, statusChangeListener)` [g-car]:
  - "If car service is not ready within the given time, it will return a Car instance in disconnected state."
  - "Blocking main thread forever can lead into getting ANR (Application Not Responding)"
  - "it is recommended to implement all car related initialization inside CarServiceLifecycleListener.onLifecycleChanged(Car, boolean)"
- `Car.CarServiceLifecycleListener`: `public abstract void onLifecycleChanged (Car car, boolean ready)`. "This is always called in the main thread context." `ready`: "when `true`, car service is ready and all accesses are ok Otherwise car service has crashed or killed and will be restarted" [g-car-lifecycle]
- `getCarManager(String)`: "Matching service manager or null if there is no such service." [g-car]
- `disconnect()`: "This can be called while disconnected. Once disconnect is called, all Car*Managers from this instance become invalid" [g-car]

`android.car.drivingstate.CarUxRestrictionsManager` (공개 메서드는 이 3개뿐):

```
public CarUxRestrictions getCurrentCarUxRestrictions ()
public void registerListener (CarUxRestrictionsManager.OnUxRestrictionsChangedListener listener)
public void unregisterListener ()
```

- `registerListener`: "If a listener has already been registered, it has to be unregistered before registering the new one." [g-uxr-manager]
- `OnUxRestrictionsChangedListener`: `public abstract void onUxRestrictionsChanged (CarUxRestrictions restrictionInfo)`, "Called when the UX restrictions due to a car's driving state changes." [g-uxr-listener]

`android.car.drivingstate.CarUxRestrictions`:

```
public boolean isRequiresDistractionOptimization ()
public int getActiveRestrictions ()
public boolean isSameRestrictions (CarUxRestrictions other)
```

- 클래스 설명 (원문) [g-uxr]:
  - "When there are no restrictions in place, for example when the car is parked, isRequiresDistractionOptimization() returns false."
  - "When the driving state changes, causing the UX restrictions to come in effect, isRequiresDistractionOptimization() returns true."
  - "for the same driving state of the vehicle, could vary depending on the OEM and the market. For example, when the car is idling, the set of active UX restrictions will depend on the car maker"
  - "apps are expected to react to the restriction information and not to the absolute driving state."
- `isRequiresDistractionOptimization()`: "If the app has a foreground activity that has not been distraction optimized, the app has to switch to another activity that is distraction optimized. Failing that, the system will stop the foreground activity." [g-uxr]
- 상수 값: `UX_RESTRICTIONS_BASELINE` 0, `NO_DIALPAD` 1, `NO_FILTERING` 2, `LIMIT_STRING_LENGTH` 4, `NO_KEYBOARD` 8, `NO_VIDEO` 16, `LIMIT_CONTENT` 32, `NO_SETUP` 64, `NO_TEXT_MESSAGE` 128, `NO_VOICE_TRANSCRIPTION` 256, `FULLY_RESTRICTED` 511 [g-uxr]

#### 권한

- Google 레퍼런스의 `CarUxRestrictionsManager` 공개 메서드 3개에는 권한 표기가 없다. [g-uxr-manager]
- AOSP 문서의 "APIs available to apps" 표: `CarUxRestrictionsManager`는 "Public APIs to register for UX Restriction changes." 이 페이지에서 권한이 붙은 것은 설정 저장(`Car.PERMISSION_CAR_UX_RESTRICTIONS_CONFIGURATION`)뿐이다. [aosp-uxr]
- 반대로 절대 주행 상태(`CarDrivingStateManager`)는 "@SystemApis"이고 `android.car.permission.CAR_DRIVING_STATE`로 보호된다. 원문: 앱은 UI·UX에 관한 것은 "monitor restrictions exposed by the CarUxRestrictionsManager and not an absolute driving state exposed by the CarDrivingStateManager" [aosp-consume]
- Pleos 문서에도 이 API에 권한이나 승인이 필요하다는 문장이 없다.
- **판단 (2026-11-05):** `CarUxRestrictionsManager`에는 `pleos.car.permission.*`도 `android.car.permission.*`도 선언하지 않는다. Pleos 에뮬레이터에서 `SecurityException`이 나지 않는지 실행해서 확인한다.

#### AOSP 기본 주행 상태와 규제 매핑 (참고, Pleos 설정이 아님) [aosp-uxr]

- 원문: "The state of gear selection and speed is converted to one of three drivingstates":
  - Parked: "Gear in Park."
  - Idling: "Gear not in Park and speed is zero."
  - Moving: "Gear not in Park and speed isn't zero."
- 쓰는 VHAL 프로퍼티: `PERF_VEHICLE_SPEED`, `GEAR_SELECTION`, `PARKING_BRAKE_ON`
- 예시 XML: parked는 제한 없음, idling은 `car:requiresDistractionOptimization="true"` `car:uxr="no_video|no_config"`, moving은 `car:uxr="fully_restricted"`. 원문: "The mapping illustrated above is pre-determined and configured as an XML resource."
- Pleos가 실제로 쓰는 매핑은 공개되지 않았다: "UX 규제 피그마 사양서는 Pleos Playground에 공개하지 않습니다." "플랫폼이 지역별 규제 사양에 맞는 ActiveRestrictions를 내려주므로" 앱은 받은 값만 반영한다. [driving-restriction-dev]

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

**보호 수준과 런타임 요청 (2026-11-05 조사, 010 계획 R5)** [navi-perm] [gh-navi-src]

- **문서에 없음 → 미확인.** 권한 페이지(한글판·영문판)에는 protectionLevel(normal/dangerous), 런타임 요청 필요 여부, 권한이 없을 때의 동작이 없다.
  - 페이지에 있는 것은 이 두 문장과 위 표, 매니페스트 예시뿐이다.
    - "NaviHelper API는 기능에 따라 Android 퍼미션을 요구합니다."
    - "API별 정확한 요구 퍼미션은 API Reference의 `@RequiresPermission`에서 확인할 수 있습니다."
  - 대조: FusedLocation 권한 페이지는 `dangerous`라고 명시한다 [fused-perm]. NaviHelper 페이지에는 그런 문장이 없다.
  - API Reference 상세 페이지는 본문이 비어 있어 `@RequiresPermission`도 볼 수 없다 (아래 "API Reference 목차" 참고).
- 오류 처리 페이지의 점검 항목 (원문): "사용하는 API에 필요한 퍼미션을 선언했는지" [navi-error]. "선언"만 말하고 런타임 허용은 말하지 않는다.
- 공식 샘플 (참고, 보호 수준의 근거는 아니다) [gh-navi-src]:
  - 런타임 권한 요청 코드가 없다. `MainActivity.kt`, `NaviSdkScreen.kt`, `MyApplication.kt` 어디에도 `requestPermissions`, `ActivityResultContracts`, `checkSelfPermission`이 없다.
  - 매니페스트에는 `uses-permission` 5줄만 있고 XML 주석이 없다. vehicle 샘플의 "protectionLevel="normal"" 같은 주석이 없다.
  - API 호출 예외는 `catch (e: Exception)`으로 잡고, `SecurityException`은 따로 표시한다: `is SecurityException -> "SecurityException: ${message.orEmpty()}"`

**이벤트 수신에 필요한 권한 (2026-11-05 조사, 010 계획 R6)** [navi-perm] [navi-events]

- **문서에 없음 → 미확인.** 권한 표는 요청 메서드만 다룬다. `addListener()`와 콜백(`onTripInfo`, `onDrivingInfo`, `onDestinationArrived`, `onRouteCancelled` 등)에 필요한 권한은 권한 페이지에도 이벤트 리스너 페이지(한글판·영문판)에도 없다.
- 관련 원문: "정보 조회 API는 값을 직접 반환하지 않으며, 대응하는 이벤트 콜백에서 결과를 처리합니다." [navi-dest]
  - `get*Info()` 요청에는 `NAVI_ROUTE`가 필요하다 (위 표). 요청 없이 Navigation 앱이 보내는 이벤트에도 권한이 필요한지는 문서에 없다.

#### 호환성 [api-compat]

- API 호환성 표 원문: NaviHelper SDK는 "내비게이션이 탑재된 차량에 한해 전체 호환".
- 내비게이션 탑재 여부를 확인하는 공개 API는 없다 (위 [faq] 항목 참고).
- 공식 샘플 README의 요구 사항: Pleos 내비게이션 서비스를 쓸 수 있는 기기 또는 에뮬레이터, 호환되는 내비게이션 앱이 설치되어 기본 내비게이션 앱으로 설정되어 있을 것. 샘플은 내비게이션 응답을 직접 만들지 않는다. [gh-navi-src]
- **AAR의 minSdk와 라이브러리가 스스로 넣는 매니페스트 항목 (010 계획 R4): 문서에 없음 → 미확인** (2026-11-05). [navi-import] [navi-perm] [navi-intro]
  - Importing 페이지에는 Maven 저장소와 `implementation("ai.pleos.playground:NaviHelper:3.0.5")`뿐이다. minSdk, 전이 의존성, 라이브러리가 넣는 `uses-permission`·`<queries>`, 필요한 내비 서비스 버전이 없다.
  - 공식 navi 샘플은 `minSdk = 33`이다 (근처에 주석 없음). 매니페스트에 `<queries>`, `<uses-sdk>`, `tools:overrideLibrary`가 없다. 그래서 샘플만으로는 AAR의 minSdk를 알 수 없다. [gh-navi-src]

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

**`initialize` 두 형태 (2026-11-05 조사, 010 계획 R1)** [navi-init] [navi-error] [sdk-release-note]

- 전체 클래스 이름: `ai.pleos.playground.navi.helper.NaviHelper`. 문서 예시의 import와 샘플의 import가 같다. [navi-init] [gh-navi-src]
- 문서 예시 (원문 Kotlin. 줄바꿈은 복원한 것) [navi-init]:

```kotlin
import ai.pleos.playground.navi.helper.NaviHelper
val naviHelper = NaviHelper(applicationContext)
naviHelper.initialize(
    onConnected = {
        // 최초 서비스 연결이 성공했습니다. 리스너를 등록하고 API를 호출합니다.
    },
    onFailed = { throwable ->
        // 초기 연결 재시도가 모두 실패했습니다.
    })
```

- 문서 예시 (원문 Java) [navi-init]:

```java
import ai.pleos.playground.navi.helper.NaviHelper;
import kotlin.Unit;
NaviHelper naviHelper = new NaviHelper(getApplicationContext());
naviHelper.initialize(
    () -> {
        // 최초 서비스 연결이 성공했습니다. 리스너를 등록하고 API를 호출합니다.
        return Unit.INSTANCE;
    },
    throwable -> {
        // 초기 연결 재시도가 모두 실패했습니다.
        return Unit.INSTANCE;
    });
```

- 예시로 알 수 있는 것: `onConnected`는 인자가 없고, `onFailed`는 인자 하나를 받는다 (예시의 이름은 `throwable`). Java 예시가 `kotlin.Unit`을 반환하므로 두 인자는 Kotlin 함수 타입으로 보인다 (추정).
- **문서에 없음 → 미확인:** `onFailed` 인자의 정확한 타입, `initialize`의 반환형, 재시도 횟수·간격, 콜백이 오는 스레드. API Reference의 `initialize` 페이지는 본문이 비어 있다. [navi-api-ref]
- 두 형태의 차이 (원문) [navi-init]:
  - `initialize(onConnected, onFailed)`: "서비스 연결 결과를 콜백으로 받습니다. 최초 서비스 연결이 성공하면 `onConnected`가, 초기 연결 재시도가 모두 실패하면 `onFailed`가 호출됩니다."
  - `initialize()`: "인자 없이 SDK를 초기화합니다."
  - "초기화가 수행되어야 다른 API들이 정상 동작합니다."
  - "연결 상태에 대한 콜백을 제공하는 API 사용을 권장합니다."
- 콜백 형태는 v3.0.5에서 추가됐다. 릴리스 노트 New API 원문: "초기화 결과 콜백 지원 추가(initialize(onConnected, onFailed))" [sdk-release-note]
- 오류가 오는 경로 (원문) [navi-error]:
  - "NaviHelper에서는 초기 서비스 연결 실패와 Navigation 앱의 요청 처리 오류가 서로 다른 경로로 전달됩니다."
  - "초기 연결 결과가 필요하면 `initialize()`에 콜백을 전달합니다."
  - "Navigation 앱에서 발생한 오류는 `NaviHelperEventListener.onError()`로 전달됩니다."
  - 오류 처리 페이지 예시의 `onFailed` 주석: "// 연결 실패 원인을 기록하거나 사용자에게 안내합니다."
- 공식 샘플은 문서 권장과 다르게 인자 없는 `initialize()`를 쓴다 [gh-navi-src]:
  - `onConnected`를 기다리지 않고 바로 `addListener(this)`를 부른다.
  - 생성·초기화·등록 전체를 `try { ... } catch (e: Exception)`으로 감싼다. 실패하면 `runCatching { helper?.release() }`를 부른다.
  - 해제는 `removeListener(this)` → `release()` 순서다.

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
- > **불일치 (2026-11-05): Java 생성자의 위경도 순서.** 페이지마다 예시가 다르다. Kotlin은 이름 인자(`latitude = ...`)로 쓰면 이 문제가 없다.
  > - Destinations, Waypoints 페이지: 경도 먼저, 인자 9개 [navi-dest] [navi-waypoints]
  >   `new RouteInfo(127.03636666, 37.48544722, null, null, "42dot", "36khp37hh8gqcm5nqhe5", "서울시 강남구 남부순환로 2621", "0", RouteOption.RECOMMENDED)`
  > - Route requests with waypoints 페이지: 위도 먼저, 인자 8개 (`RouteOption` 없음) [navi-route-wp]
  >   `new RouteInfo(37.48544722, 127.03636666, null, null, "42dot", "36khp37hh8gqcm5nqhe5", "서울시 강남구 남부순환로 2621", "0")`
  > - SDK 릴리스 노트 NaviHelper v3.0.5 Breaking change (원문): "데이터 모델 생성자의 좌표 인자 순서 변경(longitude, latitude → latitude, longitude; BookmarkInfo, CurrentLocationInfo, DestinationInfo, RecentDestinationInfo, RequestWaypointInfo, RouteInfo, WaypointInfo)" [sdk-release-note]
  > - 릴리스 노트로 보면 3.0.5는 위도가 먼저이고, 경도가 먼저인 두 페이지는 바뀌기 전 예시로 보인다 (추정). `RequestWaypointInfo`의 Java 예시도 같은 식으로 갈린다. 실제 순서는 AAR(`javap`)로 확인한다.
- 공식 샘플은 `displayLatitude`/`displayLongitude`를 생략하고 `latitude`, `longitude`, `poiId`, `poiName`, `poiSubId`, `address`만 넘긴다. [gh-navi-src]

**`RouteInfo` 인자의 nullable 여부와 기본값 (2026-11-05 조사, 010 계획 R3)** [navi-dest] [navi-waypoints] [navi-route-wp] [navi-error] [gh-navi-src]

문서와 샘플 어디에도 생성자 선언이 없다. API Reference의 `RouteInfo` 페이지와 생성자 페이지(`.../route-info/-route-info`)는 본문이 비어 있다 [navi-api-ref]. 아래는 공식 예시가 실제로 넘기는 값이다.

| 인자 | Destinations·Waypoints (Kotlin) | Route with waypoints (Kotlin) | 공식 샘플 (3.0.5) | Java 예시 | 문서에서 알 수 있는 것 |
|---|---|---|---|---|---|
| `displayLatitude`, `displayLongitude` | `null` | 생략 | 생략 | `null, null` | `null`을 넘기는 예시가 있다. Kotlin에서 생략하는 예시도 있다. 기본값은 문서에 없다 |
| `poiName` | `"42dot"` | `"42dot"` | `"42dot"` | 값 | 항상 값을 넘긴다 |
| `poiId` | `"36khp37hh8gqcm5nqhe5"` | 같음 | 같음 | 같음 | 항상 값을 넘긴다. nullable 여부·기본값은 문서에 없다 |
| `poiSubId` | `"0"` | `"0"` | `"0"` | `"0"` | 위와 같다 |
| `address` | 주소 문자열 | 같음 | 같음 | 같음 | 위와 같다 |
| `RouteOption` | 생략 | 생략 | 생략 | `RouteOption.RECOMMENDED` (9개 인자 예시) / 없음 (8개 인자 예시) | Kotlin 예시는 모두 생략한다. 기본값은 문서에 없다 |

- **좌표와 이름만으로 경로를 요청할 수 있는지: 문서에 없음 → 미확인.** 모든 예시가 실제 POI ID(`36khp37hh8gqcm5nqhe5`)를 넘긴다.
  - 관련 문장은 오류 처리 페이지의 점검 항목 하나다 (원문): "좌표, POI ID 및 경유지 인덱스가 올바른지" [navi-error]
- 외부 장소 검색 결과로 경로를 요청할 때 `poiId`/`poiSubId`에 무엇을 넣어야 하는지도 문서에 없다 → 미확인.
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
  - **2026-11-05 재확인: 여전히 비어 있다.** 시도한 페이지:
    - `.../ai.pleos.playground.navi.helper/navi-helper/entry`, `.../navi-helper/initialize`, `.../navi-helper/request-route`
    - `.../ai.pleos.playground.navi.data/route-info/entry`, `.../route-info/-route-info` (생성자)
    - `.../ai.pleos.playground.navi.constants/navi-error-code/entry`, `.../navigation-status/entry`
    - `.../ai.pleos.playground.navi.helper.listener/navi-helper-event-listener/entry`, `.../navi-helper-event-listener/on-destination-arrived`
    - 영문판 `/en/docs/...`의 `initialize`
  - 옛 경로 `/en/api-reference/connect-sdk-pleos/NaviHelper/...`는 404다.
- 패키지별 타입 목록 (패키지 목차 페이지) [navi-api-ref]:
  - `navi.helper`: `NaviHelper` 하나. `navi.helper.listener`: `NaviHelperEventListener` 하나.
  - `navi.constants` (15개): `BookmarkType`, `CameraType`, `CautionPointType`, `NaviErrorCode`, `NavigationStatus`, `NaviMode`, `RouteOption`, `RoutePointPoiType`, `RoutePointSettingType`, `RoutePointType`, `TrafficCongestionType`, `TrafficEventType`, `TurnType`, `WaypointCmdType`, `WaypointIndex`
  - `navi.data` (31개): `BookmarkInfo`, `CameraAlert`, `CautionPoint`, `ChargerOperator`, `ChargingPlan`, `CurrentLocationInfo`, `DestinationArrivedInfo`, `DestinationInfo`, `DrivingInfo`, `DrivingState`, `NavigationScreenRequest`, `RecentDestinationInfo`, `RequestWaypointInfo`, `RouteInfo`, `RouteOverviewShowInfo`, `RoutePointInfo`, `RouteStartInfo`, `RouteStateInfo`, `RouteWithWaypointsRequest`, `TBTInfo`, `TrafficCongestionInfo`, `TrafficCongestionSummary`, `TrafficEventInfo`, `TrafficInfo`, `TrafficStatus`, `TripInfo`, `TripRequest`, `TripSummary`, `WaypointArrivedInfo`, `WaypointChangedInfo`, `WaypointInfo`

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

**기본 구현과 콜백 스레드 (2026-11-05 조사, 010 계획 R2)** [navi-events] [gh-navi-src]

- 문서 원문: "필요한 콜백만 재정의하고, SDK 초기화 후 리스너를 등록합니다." (영문판: "Override only the callbacks your app needs, and register the listener after initializing the SDK.")
- 문서 예시는 Kotlin(`object : NaviHelperEventListener { ... }`)과 Java(`new NaviHelperEventListener() { ... }`) 모두 26개 중 3개만 재정의한다: `onRouteStarted`, `onRoutingCompleted`, `onError`.
- 그러나 "기본 구현(빈 메서드)이 있다"는 문장은 없다. 공식 샘플은 26개를 모두 재정의한다 (`onCleared`를 빼고 `override fun on...` 26개). → 기본 구현이 있는지는 AAR(`javap`)로 확정한다.
- **콜백이 오는 스레드: 문서에 없음 → 미확인.** 한글판·영문판 모두 스레드 언급이 없다.
  - 샘플도 스레드를 바꾸지 않는다. 콜백에서 `MutableStateFlow.update {}`만 부른다 (`Dispatchers`, `Handler`, `Looper`, `launch`, `withContext` 없음). 그래서 샘플에서도 스레드를 알 수 없다.
- 해제 원문: "리스너가 더 이상 필요하지 않거나 SDK를 해제하기 전에, 등록할 때 사용한 동일한 리스너 인스턴스를 제거합니다." [navi-events]

**이벤트 분류 (이벤트 리스너 페이지 표 원문)** [navi-events]

| 분류 | 콜백 |
|---|---|
| 경로 탐색 | `onRouteStarted()`, `onRoutingCompleted()`, `onRouteCancelled()`, `onReRouteEnded()`, `onRouteOptionChanged()` |
| 목적지와 경유지 | `onDestinationChanged()`, `onDestinationArrived()`, `onWaypointChanged()`, `onWayPointArrived()` |
| 정보 조회 | `onBookmarkInfo()`, `onBookMarkChanged()`, `onRecentDestinationInfo()`, `onCurrentLocationInfo()`, `onDestinationInfo()`, `onWayPointInfo()`, `onRouteStateInfo()`, `onTBTInfo()`, `onChargerOperatorInfo()`, `onTripInfo()`, `onNavigationStatus()`, `onDrivingInfo()`, `onCurrentRoadSpeedLimit()`, `onCameraAlert()`, `onCautionPointInfo()` |
| 오류 | `onError()` |

- 이 표에는 설명 열이 없다. `onRouteEnded()`는 표에 없다.

**경로 요청 결과 콜백 (Destinations 페이지 표 원문)** [navi-dest]

| 콜백 | 설명 |
|---|---|
| `onRouteStarted(routeStartInfo)` | Navigation 앱이 경로 탐색을 시작하면 호출됩니다. |
| `onRoutingCompleted()` | 경로 탐색이 완료되면 호출됩니다. |
| `onDestinationChanged()` | 목적지가 설정되거나 제거되면 호출됩니다. |
| `onError(errorCode)` | 경로 요청을 처리하는 중 오류가 발생하면 호출됩니다. |

- 같은 페이지 원문: "`requestRoute()` 호출이 정상적으로 수행되더라도 경로 탐색 완료를 의미하지 않습니다." "최종 결과는 이벤트 콜백으로 확인합니다."
- `cancelRoute()` 뒤에 어떤 콜백이 오는지는 문서에 없다. `onRouteCancelled()`는 "경로 탐색" 분류에 이름만 있고 설명이 없다.

**도착과 경로 종료 (2026-11-05 조사, 010 계획 R8)** [navi-dest] [navi-events] [navi-waypoints] [sdk-release-note]

- **`onRoutingCompleted`의 뜻: 확인됨.** 경로 탐색(계산)이 끝났다는 알림이다.
  - 원문: "경로 탐색이 완료되면 호출됩니다." [navi-dest] 이벤트 리스너 예시 주석: "// 경로 탐색 완료를 처리합니다." [navi-events]
  - 릴리스 노트 v3.0.5 Deprecated API (원문): "경로 탐색 완료 콜백 사용 중단 예정(onRouteEnded → onRoutingCompleted 사용 권장)" [sdk-release-note]
    - 릴리스 노트는 `onRouteEnded`를 "경로 탐색 완료 콜백"이라고 부른다. `onRoutingCompleted`는 그 대체다.
  - 릴리스 노트 New API (원문): "내비 이벤트 리스너 확장(onCautionPointInfo, onRoutingCompleted, onTripInfo)" [sdk-release-note]
  - 도착이나 안내 종료를 뜻한다는 문장은 어디에도 없다.
- **`onDestinationArrived`가 오는 조건: 문서에 없음 → 미확인.** "목적지와 경유지" 분류에 이름만 있다. [navi-events]
  - 경유지 쪽에만 문장이 있다 (원문): "경유지에 도착하면 `onWayPointArrived()`가 호출될 수 있습니다." ("수 있습니다"다) [navi-waypoints]
- **도착하면 경로가 저절로 끝나는지: 문서에 없음 → 미확인.** 도착 뒤에 `onRouteCancelled`나 `onDestinationChanged`가 이어서 오는지도 문서에 없다. spike에서 관찰해야 한다.

**`NaviErrorCode`, `NavigationStatus`, `RouteStartInfo` (2026-11-05 조사, 010 계획 R7)** [navi-error] [sdk-release-note] [navi-api-ref]

- `NaviErrorCode` (오류 처리 페이지 표 원문) [navi-error]:

| 값 | 설명 |
|---|---|
| `CAUSE_LOW_BATTERY` | 배터리가 부족합니다. |
| `CAUSE_SEARCH_ROUTE_OVER_DISTANCE` | 경로 탐색 가능 거리를 초과했습니다. |
| `CAUSE_SEARCH_ROUTE_TOO_SHORT_DISTANCE` | 현재 위치와 목적지 사이의 거리가 너무 짧습니다. |
| `CAUSE_SEARCH_ROUTE` | 경로 탐색 서버에서 오류가 발생했습니다. |

  - 원문: "오류 코드의 최신 목록은 NaviHelper API Reference에서 확인합니다." 그런데 API Reference의 `NaviErrorCode` 페이지는 본문이 비어 있다. **위 4개 말고 다른 값이 있는지는 미확인이다.**
  - 오류 처리 페이지의 점검 항목 (원문): "SDK 연결이 완료된 후 API를 호출했는지", "사용하는 API에 필요한 퍼미션을 선언했는지", "좌표, POI ID 및 경유지 인덱스가 올바른지", "현재 Navigation 앱 상태에서 요청을 처리할 수 있는지"
- **`NavigationStatus` 값: 문서에 없음 → 미확인.** API Reference 페이지 본문이 비어 있다. 샘플도 `toString()`으로 로그만 찍는다.
  - 릴리스 노트 v3.0.5 (원문): Breaking change "내비 상태 콜백 인자 타입 및 상태 값 변경(onNavigationStatus: RouteDriving → NavigationStatus)", Changed API "상수/Enum 추가 및 변경(CameraType, CautionPointType, NavigationStatus, RoutePointType)" [sdk-release-note]
- **`RouteStartInfo` 필드: 문서에 없음 → 미확인.** API Reference 페이지 본문이 비어 있다. 샘플은 `onRouteStarted`에서 `$routeStartInfo`를 로그로 찍기만 한다. [gh-navi-src] `navi-data` AAR을 `javap`로 확인한다.
- 같은 릴리스 노트의 다른 데이터 변경 (원문): "목적지/경유지 데이터 필드명 변경(DestinationInfo, WaypointInfo: totalDistance/totalTime → remainDistance/remainTime)" [sdk-release-note]
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
- 현재 위치: `getCurrentLocationInfo()` → `onCurrentLocationInfo(CurrentLocationInfo)` (권한 `NAVI_ROUTE`, `get*Info()`). 필드 정의는 확인했다 → 아래 `[aar-navi]` "현재 위치". 좌표계와 단위 설명은 문서에 없다.

#### AAR에서 직접 확인한 것 (2026-11-05, `NaviHelper-3.0.5.aar`, `navi-data-3.0.4.aar`를 Pleos Maven에서 받아 javap) [aar-navi]

- `NaviHelper(Context)`. `initialize()`와 `initialize(onConnected: () -> Unit, onFailed: (Throwable) -> Unit)` 둘 다 반환형 `void`. `release()`, `addListener`/`removeListener`, `requestRoute(RouteInfo)`, `cancelRoute()`, `requestReRoute()`, `getRouteStateInfo()`, `getCurrentLocationInfo()`, `getDestinationInfo()` 등. **`getTripInfo()`·`getDrivingInfo()`는 없다** (이벤트로만 온다).
- `NaviHelperEventListener`는 interface이고 **26개 메서드가 모두 `default`(빈 구현)**다. 필요한 콜백만 재정의하면 된다.
- `RouteInfo` Kotlin 생성자 (이름과 null 여부, `@Nullable`/`@NotNull` 기준): `latitude: Double`, `longitude: Double`, `displayLatitude: Double?`, `displayLongitude: Double?`, `poiName: String`, `poiId: String`, `address: String`, `poiSubId: String`, `routeOption: RouteOption`, `poiProvider: String`, `isMobileRequest: Boolean`. 기본값 생성자가 있지만 **`poiId`, `address`, `poiSubId`에는 기본값이 없다** (컴파일러가 값을 요구했다). 위도가 먼저다.
- `TripInfo(distance: Int, eta: Int, estimatedSocAtDest: Int, energyConsumption: Double, summary: TripSummary)`.
- **현재 위치** (2026-11-05 추가 확인, 011 계획):
  - `NaviHelper`: `public final void getCurrentLocationInfo()`. 반환값이 없고, 결과는 리스너 콜백으로 온다.
  - `NaviHelperEventListener`: `default void onCurrentLocationInfo(CurrentLocationInfo)`, `default void onError(NaviErrorCode)`.
  - `ai.pleos.playground.navi.data.CurrentLocationInfo` (`navi-data` 3.0.4):
    - 생성자 `(double latitude, double longitude, double heading, String address, String routeId, Integer currentRouteIndex)`
    - getter: `getLatitude(): double`, `getLongitude(): double`, `getHeading(): double`, `getAddress(): String`, `getRouteId(): String`, `getCurrentRouteIndex(): Integer`
    - **javap 출력으로는 `address`의 null 여부가 보이지 않는다.** 그래서 앱은 null과 빈 문자열을 "주소 모름"으로 다룬다.
  - 연결 순서와 스레드는 길안내와 같은 것으로 본다: 리스너 등록 → `initialize(onConnected, onFailed)` → 연결되면 호출. `onConnected`는 main, 리스너 콜백은 binder 스레드다 (`[obs-navi-emu]`).
- AAR 매니페스트: `minSdkVersion 23`(navi-data, navi-aidl도 23). **`pleos.car.permission`을 스스로 넣지 않는다.** `<queries><intent><action android:name="ai.pleos.playground.intent.action.NAVI_START"/></intent></queries>`와 서비스 버전 meta-data(`min_version_name 2.0.1` 등)를 넣는다.

#### Pleos 에뮬레이터에서 관찰한 것 (2026-11-05, v2.1.0, 실차 아님. 010 spike) [obs-navi-emu]

debug 빌드의 임시 수신기에서 application context로 `NaviHelper`를 만들고 adb로 명령을 보냈다. 권한은 `NAVI_ROUTE_SEARCH`, `NAVI_ROUTE` 두 개만 선언했다 (`dumpsys package`에 `NAVI_ROUTE_SEARCH: granted=true`, 런타임 요청 UI 없음).

- `initialize(onConnected, onFailed)` → **약 120ms 뒤 `onConnected`가 main 스레드로** 왔다. 리스너 콜백은 **binder 스레드**(`binder:<pid>_N`)로 온다.
- 처음 상태: `RouteStateInfo(termsAgreement=true, naviMode=SAFE_GUIDING, isRouting=false, …)`. 현재 위치 `37.4127212, 127.094681` ("수정구 시흥동", 한국).
- **`poiId`·`address`·`poiSubId`를 빈 문자열로 두고 좌표(시연 병원 37.48544722, 127.03636666) + `poiName`만으로 `requestRoute` → 됐다.** 내비 앱이 앞으로 나와 우리 앱을 가리고, "시연용 동물병원" 경로 3개(추천 35분·12km)와 "③ 안내 시작" 카운트다운을 보인 뒤 저절로 안내를 시작했다.
- 그때 콜백 순서: `onRouteStarted(RouteStartInfo(duration=2100, distance=12000, tollFare=700, name=시연용 동물병원, …))` → `onRouteEnded` → `onNavigationStatus(ROUTE_DRIVE)` → `onRouteStateInfo(naviMode=ROUTE_GUIDING, isRouting=false)`. **`duration`은 초(2100 = 35분), `distance`는 미터(12000 = 12km)**다 (내비 화면의 "35분 · 12km"와 같다). `onRouteStarted`는 카운트다운이 끝나 안내가 시작될 때 왔다. `onTripInfo`는 오지 않았다.
- `onDrivingInfo(DrivingInfo(destination=DrivingState(name=…, duration=2100, distance=12000, …)))`가 안내 중에 왔다 (같은 단위로 보인다).
- **차가 움직이지 않는다**: Virtual Console로 기어 D + 속도 80을 주거나 `adb emu geo fix`로 위치를 병원 쪽으로 옮겨도, 내비의 현재 위치와 남은 시간·거리(2100초·12000m)는 그대로였다. 그래서 **에뮬레이터에서는 남은 시간 감소와 `onDestinationArrived`를 볼 수 없었다.** 내비 화면에 모의 주행 기능이 있는지는 보지 않았다.
- `cancelRoute()` → `onDrivingInfo(duration=0, distance=0)` → `onRouteCancelled` → `onNavigationStatus(FREE_DRIVE)` → `naviMode=SAFE_GUIDING`. 내비 화면에서 경로가 사라졌다.
- 이 동안 `logcat *:E`에 우리 앱의 크래시나 `SecurityException`은 없었다.
- 앱(010 debug, 시연 모드)으로 더 본 것 (2026-11-05 오후). SDK 자체 로그(`[NaviHelper][SDK]` 태그)가 logcat에 남아서 콜백 순서를 볼 수 있다. 내비 서비스 버전은 `getServiceVersion = 3.0.4`였다.
  - **내비 화면에서 사용자가 "안내 종료"를 누르면 우리 리스너에 `onRouteCancelled`가 온다.** 앱도 안내 없음으로 돌아갔다.
  - **내비에 이전 경로가 남은 채로 `requestRoute`를 하면, 약 0.3~0.6초 뒤 이전 경로의 `onRouteCancelled`가 먼저 온다.** 그 뒤 내비는 새 경로를 계산하고, 약 6초 뒤 `onRouteStarted`가 온다.
    - 재현: 안내 중에 우리 앱을 `am force-stop` → 정리(`cancelRoute`)가 돌지 않아 내비에 경로가 남는다 → 앱을 다시 켜서 안내 시작.
    - 그래서 어댑터(`PleosNaviClient`)는 `onRouteStarted` 전의 `onRouteCancelled`를 무시한다. 그러지 않으면 앱은 안내를 끝냈는데 내비는 새 경로를 안내하는 어긋남이 생겼다 (실제로 봤다).
    - 반대로 카운트다운 중에 사용자가 내비에서 취소하면 그 취소도 무시된다. 그때는 전부 내비는 30초 뒤 안내 없음, 경로만 내비는 30초 뒤 시연용 기본 시간으로 시작한다. 카운트다운 중 취소에 콜백이 오는지는 보지 않았다.

#### Pleos 에뮬레이터에서 관찰한 것: 경로 취소 뒤 앞에 나오는 앱 (2026-11-06, 실차 아님. 013 U3) [obs-navi-emu]

Android 14 (API 34), Pleos 에뮬레이터, 012 debug 시연 모드(경로만 내비). 우리 앱이 앞에 있는 상태에서 `cancelRoute()`를 불렀다.

- **`cancelRoute()`를 부르면 내비 서비스가 내비 앱을 앞으로 띄운다.** 우리 앱이 앞에 있어도 그렇다.
  - logcat 순서: 우리 `[NaviHelper][SDK] cancelRoute` → 내비 `requestToNaviProvider(RequestRouteCancel)` → `ActivityTaskManager: Background activity start for ai.umos.maps.android.navigation.app allowed because SYSTEM_ALERT_WINDOW permission is granted.` → `START u10 {flg=0x10000000 cmp=ai.umos.maps.android.navigation.app/.MainActivity} ... (BAL_ALLOW_SAW_PERMISSION)` → 14ms 뒤 `HOME_SCREEN ... hideTask ComponentInfo{com.meongcare.app/com.meongcare.app.MainActivity}`.
  - 화면에는 경로가 지워진 내비 지도("어디로 갈까요?")가 보인다. 시연 도착 때 앱이 경로를 취소하자 우리 "도착했어요" 대신 이 지도가 나왔다 (012 동작). 앱의 [안내 취소]도 같다.
- **우리 앱은 스스로 다시 앞으로 돌아올 수 없다.**
  - 내비가 앞에 나온 900ms 뒤 `MainActivity`에서 `startActivity(FLAG_ACTIVITY_REORDER_TO_FRONT | FLAG_ACTIVITY_NEW_TASK)` → `Background activity launch blocked [... callingUidHasAnyVisibleWindow: false; callingUidProcState: LAST_ACTIVITY ...]` → `(BAL_BLOCK) result code=3`. 그때 우리 Activity는 `CREATED`(멈춤) 상태였다.
  - 2.5초 뒤 `ActivityManager.moveTaskToFront(taskId, 0)` → `SecurityException: Permission Denial: moveTaskToFront() ... requires android.permission.REORDER_TASKS`. 이 권한은 쓰지 않기로 했다 (새 권한, 그리고 Android 10부터 백그라운드 실행 제한도 받는다).
- 결론: **`cancelRoute`는 내비 앱을 앞으로 띄우고, 앱은 스스로 돌아올 수 없다.** 사용자는 GNB의 MeongCare 아이콘으로 돌아온다.
  - 그래서 013부터 시연 모드(경로만 내비)는 시연 도착과 도착 화면 [닫기] 때 `cancelRoute`를 부르지 않고 리스너 해제·`release()`만 한다 (계획 013 U2). 내비에는 경로가 남고, 다음 `requestRoute`가 새 경로로 바꾼다.
  - 앱의 [안내 취소]와 Real 모드(전부 내비)의 취소는 지금처럼 `cancelRoute`를 부른다 → 내비가 앞에 나온다 (알려진 한계).

#### Pleos 에뮬레이터에서 관찰한 것: 현재 위치 (2026-11-05, 실차 아님. 011 단계 6) [obs-navi-emu]

011 debug 빌드의 `PleosNaviClient.currentLocation()`으로 봤다. 물을 때마다 새 `NaviHelper` → `initialize` → `getCurrentLocationInfo()` → 답을 받으면 `release()`.

- **내비 지도가 준비되기 전에는 답이 오지 않는다.**
  - 상황: 에뮬레이터 framework가 다시 시작된 뒤(`BOOT_COMPLETED`), 내비 화면을 한 번도 띄우지 않았다.
  - 이때 내비 앱 로그: `connectNaviProviderEventFlow - navi not ready … OnCurrentLocationInfoRequest, Status(appStatus=AuthComplete, engineStatus=NaviEngineComplete, mapStatus=MapInitComplete)`
  - 우리 리스너에는 `onCurrentLocationInfo`도 `onError`도 오지 않았다. 요청이 조용히 버려졌다. 두 번 물어 두 번 다 5초 시간 초과였다.
  - 다른 시스템 앱의 요청(`GetBookmarkInfo`)도 같은 로그로 버려졌다.
  - 내비 앱(`ai.umos.maps.android.navigation.app/.MainActivity`)을 한 번 앞에 띄우자 `naviReadyStatus … mapStatus=MapComplete`가 되었다. 그 뒤로는 바로 답이 왔다.
  - 그래서 **시연 전에 내비 화면을 한 번 띄워 둔다.** 앱은 답이 없으면 시간 초과 뒤 "모름"으로 처리한다.
- 준비된 뒤의 응답:
  - `onCurrentLocationInfo`는 binder 스레드로 왔다.
  - 걸린 시간(연결부터 답까지): 앱을 새로 띄운 뒤 첫 묻기 263ms, 그 뒤 64~83ms.
  - 값: `CurrentLocationInfo(latitude=37.4127212, longitude=127.094681, heading=0.0, address=수정구 시흥동, routeId=, currentRouteIndex=0)`. 010 spike와 같은 값이다.
  - 주소는 매번 "수정구 시흥동"이었다. null이나 빈 값은 보지 못했다.
  - 안내 중에는 `routeId`에 경로 ID가 들어 있었다.
- **NaviHelper 두 개를 같이 써도 됐다** (010 시연 길안내 중에 위치 묻기).
  - 위치 쪽 응답은 82ms였다.
  - 위치 쪽을 `release()`한 뒤에도 내비 화면의 경로가 그대로 있었다.
  - 그 뒤 앱의 [안내 취소] → 길안내 쪽 `cancelRoute()`로 내비 경로가 사라졌다. 길안내 쪽 연결이 살아 있었다는 뜻이다.
- `SecurityException`, 크래시는 없었다. 권한은 `NAVI_ROUTE`, `NAVI_ROUTE_SEARCH` 둘뿐이다.
- 로그:
  - 우리 앱 pid 로그에는 좌표가 0건이다.
  - 다른 시스템 앱 몇 개(`[GAA]`, `NavigationEventListener`)는 `CurrentLocationInfo`를 좌표째 logcat에 남긴다. 우리가 바꿀 수 없다.

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
- **NaviHelper의 현재 위치:** `getCurrentLocationInfo()` → `onCurrentLocationInfo(CurrentLocationInfo)`. 권한은 `NAVI_ROUTE`(`get*Info()`). 내비게이션 탑재 차량에서만 호환된다. `CurrentLocationInfo` 필드는 확인했다 (위도·경도·방향 `double`, 주소 `String`) → NaviHelper 절 `[aar-navi]`. [navi-perm] [api-compat] [gh-navi-src] [aar-navi]

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
- **에뮬레이터:** STT/TTS intro 원문 "SpeechToText SDK를 사용하려면 Android Studio에 Pleos Connect Emulator를 설치하고 AVD를 설정합니다." 설정 항목에 "마이크 접근 설정", "Cold boot 설정"이 있다. [stt-intro] [tts-intro] 에뮬레이터에 On-device 엔진이 들어 있는지, 실제로 인식되는지는 문서에 없다. → **Pleos 에뮬레이터에서 직접 실행해 동작을 확인했다** (2026-11-05, 아래 "STT `ON_DEVICE` 실행 확인"). 마이크 설정은 [emulator-setup.md](emulator-setup.md#주의) 참고.
- **Android 표준 `SpeechRecognizer` / `android.speech.tts.TextToSpeech`:** Pleos 문서(SDK 개요, SDK API 정책, 하드웨어 접근 정책, API 호환성, FAQ, 오디오 포커스 가이드, TTS 선행 묵음 가이드)에 허용·금지 언급이 없다 → 미확인.
  - 관련 정책 문장: "동일하거나 유사한 기능의 API가 AAOS 표준에 이미 존재하는 경우, 해당 표준 API 사용을 권장하며 Pleos Connect SDK에서는 중복 기능을 제공하지 않습니다." [sdk-api-policy] Pleos가 STT/TTS SDK를 따로 제공하므로, 이 문장만으로 표준 API 사용 가능 여부를 판단할 수 없다.
- **음성 출력의 오디오 포커스:** 재생 전 `AudioAttributes` + `AudioFocusRequest`로 포커스를 요청한다. 컨텍스트 매핑에서 NAVIGATION은 `NAVIGATION`, `ASSISTANCE_NAVIGATION_GUIDANCE`, VOICE_COMMAND는 `VOICE_COMMAND`, `ASSISTANT`. TTS SDK가 어느 usage로 재생하는지는 문서에 없다. 자세한 내용은 [ux-guidelines.md](ux-guidelines.md#오디오-포커스와-음성-안내) [audio-focus]

### STT `ON_DEVICE` 실행 확인 (관찰, 2026-11-05)

xxj15 Mac(M4 Pro), AVD `pleos_connect`(arm64, Cold boot). 문서가 아니라 실행해서 본 결과다. 실험 코드는 로컬 브랜치 `spike/stt-ondevice`(dev에 합치지 않음)의 debug 화면 `SttProbeActivity`다.

- **결론: Client ID/Secret 없이 `ON_DEVICE` 모드로 한국어 인식이 된다.** 콘솔 상태는 "준비 중"인 채다. 심사 제출 앱에서 써도 되는지는 여전히 미확인이다 (위 "`ON_DEVICE`와 '준비 중'의 관계", 문의 B2).
- SDK 받기: `ai.pleos.playground:SpeechToText:3.3.1`이 Pleos Maven에서 그대로 받아진다. 전이 의존성은 `ai.pleos.playground.security:universal-access:0.6.0` 하나다.
- AAR 매니페스트 (산출물):
  - `android.permission.INTERNET`을 선언한다 (주석: "CCG Validation 체크를 위한 권한").
  - `<queries>`로 `ai.pleos.playground.caas.intent.action.SPEECH_START`를 찾는다.
  - 요구 음성 서비스(CAAS) 최소 버전: `4.1.29` (versionCode 4001029).
- 에뮬레이터 쪽 (`adb shell`):
  - 음성 서비스 `ai.pleos.playground.caas` **4.2.10-coral-aosp**가 들어 있다. `SPEECH_START`를 받는 서비스는 `ai.pleos.playground.caas.service.speech.SpeechService`다.
  - `pleos.car.permission.STT_SERVICE`는 이 패키지가 정의하고 보호 수준이 **`normal`**이다. 매니페스트에 선언만 하면 설치할 때 허용된다 (`granted=true`).
- 클래스 시그니처 (`javap`, 3.3.1):
  - `SpeechToText(Context, Mode)`, `initialize(OnInitializeListener)`, `addListener(ResultListener)`, `setLocale(Locale)`, `request()`, `stop()`, `sendAudio(byte[])`, `completeAudioSend()`, `release()`
  - `OnInitializeListener`: `onSuccess()`, `onFailure(ErrorType)`
  - `ResultListener`: `onUpdated(String, Boolean)`, `onUpdatedEpdData(Long, Long)`, `onStartedRecognition()`, `onEndedRecognition()`, `onError(ErrorType)`, `onReady()`, `onUpdatedRms(Double)`
  - 문서 목차에 없는 멤버: `getCallApi()`, `getAmbientAi()`, `registerApp`의 람다 오버로드 2개
- **PCM 입력 (`sendAudio`)**: Mac `say -v Yuna`로 만든 문장을 16 kHz, 16-bit, mono로 바꿔 3,200 bytes(100ms)씩 넣었다.
  - 순서: `initialize` → `onSuccess` → `addListener` → `setLocale(Locale.KOREA)` → `sendAudio` 반복 → `completeAudioSend`. `request()` 없이 된다.
  - 결과: 넣기 시작한 지 약 1초 뒤부터 `onUpdated(completed=false)`로 글자가 점점 늘었다. 그 뒤 `onEndedRecognition`, `onUpdated(completed=true, "강아지가 갑자기 토하고 숨을 가쁘게 쉬어요")`가 왔다. 넣은 문장과 글자가 똑같다.
  - 부분 결과마다 `onStartedRecognition`이 다시 온다. 한 번만 오는 이벤트로 다루면 안 된다.
  - `initialize` 성공까지 약 20ms 걸렸다. 네트워크·앱 등록 호출은 없었다.
- **마이크 입력 (`request`)**: `onReady`까지 오고, 음성 서비스 로그에 16 kHz 녹음 시작(`AudioRecorderImpl start`)이 찍힌다. 에뮬레이터가 Mac 마이크 소리를 받지 못해 인식 결과까지는 확인하지 못했다 → 미확인. 에뮬레이터 Extended Controls > Microphone을 켜고 사람이 직접 말해 확인한다.
- **`RECORD_AUDIO`**: AAOS 앱은 운전자 계정 **user 10**에서 돈다. `adb shell pm grant`는 기본이 user 0이라 `--user 10`을 붙여야 한다.
  - 앱에 권한이 없을 때: 서비스 로그 `callerIdentityForRecordAudio(...) mode=1`. 녹음은 음성 서비스(`ai.pleos.playground.caas`) 이름으로 시작됐다.
  - 권한이 있을 때: `mode=0`이고 녹음이 우리 앱 이름으로 시작됐다.
  - 우리 앱에 `RECORD_AUDIO`가 꼭 필요한지는 문서에 없다. 권한이 없을 때 실차에서도 이렇게 대신 녹음되는지는 미확인이다.

### Gleo Interface (음성 명령으로 앱 기능 호출) [gleo-core] [gleo-deliver]

- Gleo AI가 앱이 정의한 function을 호출하는 function calling 방식이다. 앱은 App Schema(JSON)를 등록하고, Gleo AI는 Gleo Action으로 앱을 부르며, 앱은 Callback으로 응답한다. [gleo-core]
- 스키마 전달: 파일명은 "`schema.json`이어야 합니다.", "파일은 앱의 assets/ 디렉토리에 위치시켜야 합니다." 원문: "App Schema를 전달받으면, 당사 가이드라인을 준수하는지 검토합니다." "승인 후에는 통합이 자동으로 활성화됩니다." [gleo-deliver]
- 원문: "현재 가이드는 개발이 진행됨에 따라 지속적으로 개선될 예정입니다." [gleo-core]
- **SDK 의존성과 권한이 없다.** Gleo 가이드 6개 페이지 어디에도 Gradle 좌표와 `pleos.car.permission.*` 권한이 없다. 앱은 Android Intent로 받고 Broadcast로 응답한다. [gleo-core] [gleo-schema] [gleo-callback] [gleo-examples] [gleo-deliver] [gleo-radio]
  - Pleos Maven에도 이름에 "gleo"가 들어간 아티팩트가 없다 (2026-11-03 검색. 같은 방법으로 `NaviHelper`는 검색됨). [maven-search]
- 가이드 구성 (사이드바 "Gleo Interface 구현 가이드"): Gleo Interface(core-concepts), App Schema 작성 가이드, Callback Schema 작성 가이드, 예제 코드, App Schema를 Gleo에 전달하기, Use Cases(라디오 앱 하나). [gleo-core]

#### 사용 가능 상태와 신청 (2026-11-03 조사)

- **신청 자격, 신청 방법, 제휴·계약 필요 여부: 문서에 없다.** 아래 페이지에 해당 문장이 없다.
  - Gleo 가이드 6개 페이지 [gleo-core] [gleo-schema] [gleo-callback] [gleo-examples] [gleo-deliver] [gleo-radio]
  - 프로젝트 생성·SDK 사용 신청 [create-project], SDK 개요 [sdk-overview], 개발 가이드 소개 [dev-guide-intro]
  - 앱 제출 가이드 [app-submission], 앱 심사 가이드와 하위 페이지(기능·성능·안정성, 등록 정보) [app-review]
- **SDK 목록 어디에도 Gleo가 없다.**
  - 프로젝트 생성 페이지의 상태별 목록: 준비 중 "FusedLocation, TextToSpeech, SpeechToText, Vehicle Control SDK", 사용 가능 "Vehicle State, NaviHelper SDK" [create-project]
  - 콘솔 (2026-11-03 팀원 확인)에도 없다. [console-2026-11-03]
  - SDK 개요의 목록: "Vehicle SDK", "NaviHelper SDK", "TextToSpeech SDK", "SpeechToText SDK", "FusedLocation SDK". Gleo는 사이드바 항목 "Gleo Interface 구현 가이드"로만 나온다. [sdk-overview]
  - 대회 FAQ의 사용 가능 SDK 목록과 대회 개발 가이드에도 Gleo가 없다. [contest-faq] [contest-dev]
- Developer Journey 페이지의 개발·연동 단계 설명에 Gleo가 한 번 나온다: "including app feature implementation, SDK usage, and Gleo Interface integration." 자격이나 절차는 없다. [dev-journey]
- 문의처:
  - deliver-schema 원문: "해당 부분에 대해 추가적인 안내가 필요하다면 이 문서의 저자에게 문의해 주시기 바랍니다." 저자 연락처는 페이지에 없다. [gleo-deliver]
  - 사이트 공통 하단의 연락처는 `partnership@pleos.ai`다. Gleo 문의 창구라는 문장은 없다. [gleo-deliver]
- 참고 (Gleo 언급은 없는 일반 규정): "제휴 계약 필요 상태로 표시된 항목의 경우에는 웹 사이트를 통한 신청 방법을 제공하지 않습니다." "서비스 계약 담당자에게 별도 문의하여 제휴 계약을 진행하신 후 사용 가능한 항목입니다." [create-project]

#### 스키마 검토 기간과 전달 경로

- **검토 기간: 문서에 없다.**
  - deliver-schema는 "검토합니다", "승인 후에는 통합이 자동으로 활성화됩니다."라고만 한다. [gleo-deliver]
  - 앱 제출·심사 가이드에도 Gleo, 스키마, 심사 기간에 대한 문장이 없다. [app-submission] [app-review]
- **전달 경로도 문서에 없다.** APK 안의 `assets/schema.json`이 앱 심사 때 같이 검토되는지, 따로 보내는지 적혀 있지 않다. [gleo-deliver]

#### 에뮬레이터와 개발 중 테스트

- **에뮬레이터에 Gleo AI가 들어 있는지: 문서에 없다.**
  - 개발 환경 구성(에뮬레이터 setup) 페이지에 Gleo, 음성, 어시스턴트, 마이크 언급이 없다. [setup]
  - 에뮬레이터 릴리스 노트(v2.1.0, 2026-10-19만 있음)에도 Gleo 언급이 없다. 제거 항목은 "LLM SDK 제공 종료", "CRN 제공 종료", "블루투스 동글 연동 제공 종료"다. [emulator-release-note]
  - Resources 페이지의 에뮬레이터 설명: "The emulator provides the Pleos Connect system UI (including the Home screen, GNB, driving view, and multi-window mode)". Gleo는 나오지 않는다. [resources]
- **테스트 도구, 디버그 모드, 승인 전 로컬 시험 방법: 문서에 없다.**
  - Gleo 가이드 6개 페이지에 테스트 방법이 없다.
  - 앱 테스트 가이드(폼 팩터 테스트, Virtual Console)에도 Gleo가 없다. Virtual Console 탭은 Vehicle, Motion, ADAS, Convenience, Safety, Energy, Climate다. [form-factor-test] [virtual-console]
- **Gleo AI가 스키마를 읽는 시점** (원문) [gleo-deliver]:
  - "Gleo AI 앱(Gleo AI를 실행하고 타사 앱과 상호작용하는 앱)이 부팅되면, `AssetManager`를 사용하여 모든 시스템 또는 타사 앱을 스캔합니다."
  - "`assets/` 디렉토리에서 발견된 모든 `schema.json` 파일은 읽혀 데이터베이스에 저장됩니다." "해당 프로세스는 **부팅 시점에 한 번만** 실행됩니다."
  - AAOS 환경: "Gleo AI 앱이 설치(ADB install 또는 앱 마켓을 통해 설치)되거나 삭제될 때," "앱의 `assets/` 디렉토리에서 `schema.json` 파일을 읽고 데이터베이스를 갱신합니다."
  - 영문판도 주어가 "When Gleo AI App is installed (via ADB install or App Market) or uninstalled"다. **타사 앱을 설치할 때 다시 읽는다는 문장은 없다.** [gleo-deliver]
  - 이 로딩과 "승인 후 자동 활성화"의 관계는 문서에 없다. 승인 전 스키마도 읽히는지 알 수 없다.

#### 앱 쪽 구현 (원문) [gleo-schema] [gleo-examples] [gleo-callback]

**App Schema 필드** [gleo-schema]

- 원문 정의:
  - `packageName`(`string`): "Android 애플리케이션의 패키지명입니다."
  - `appDescription`(`string`): "앱의 주요 목적을 명확히 나타내는 간결한 설명입니다."
  - `version`(`string`): "Pleos 앱 마켓에 등록된 애플리케이션 버전입니다."
  - `actionCalls`(`array`): "음성 명령으로 호출할 수 있는 function 목록입니다 (최대 3개)."
  - 항목마다 `type`("function"으로 고정)과 `function`(`object`)
- `function` 안의 필드:
  - `component`(`string`, 기본값 "activity"): "Android에서 Gleo AI가 function을 호출하는 방식을 지정합니다."
    - `"activity"`: "Android의 context.startActivity() 메서드를 사용하여 action을 실행합니다."
    - `"service"`: "Android의 context.sendBroadcast() 메서드를 사용하여 action을 전송합니다."
  - `name`, `description`
  - `parameters`: `type`("object"), `properties`, `required`(`array<string>`)
    - `properties` 원문: "허용 가능한 값의 범위가 정해져 있는 경우, `enum`에 해당 문자열 값의 목록을 지정할 수 있습니다."
- 작성 규칙:
  - 원문: "모든 description은 영어로 작성해야 합니다." "Description 내에 구체적인 예시를 제공하지 마세요."
  - (요약) parameter `type`은 string, number, boolean, array 중 하나다. `enum`은 문자열만 되고 `null`은 지원하지 않는다.
- > **불일치:** 예시 JSON에는 `"invokeWord" : "리마인더"`가 있지만, 필드 정의 목록에는 `invokeWord`가 없다. 한글판과 영문판이 같다. [gleo-schema]

예시 JSON (원문 중 function 1개. 줄바꿈과 들여쓰기는 원본과 다를 수 있다) [gleo-schema]:

```json
{
  "packageName": "ai.dot42.pleos.reminderapp",
  "appDescription": "A reminder app that lets you schedule tasks and receive notifications at the right time.",
  "invokeWord" : "리마인더",
  "version" : "1.0.0",
  "actionCalls": [
    {
      "type": "function",
      "function": {
        "name": "ADD_REMINDER",
        "description": "Add a reminder that will notify the user at the given date and time.",
        "parameters": {
          "type": "object",
          "properties": {
            "title": {
              "type": "string",
              "description": "Title of the reminder."
            },
            "dateTime": {
              "type": "string",
              "description": "Date and time to trigger the reminder notification, in ISO 8601 date and time format."
            }
          },
          "required": ["title", "dateTime"]
        },
        "component": "activity"
      }
    }
  ]
}
```

**Gleo Action 받기** [gleo-deliver] [gleo-examples]

- 원문: "Gleo Action을 수신하려면 각 function에 대한 action filter를 정의해야 합니다." [gleo-deliver]

```xml
<activity android:name=".YourActivity">
    <intent-filter>
        <action android:name="ADD_REMINDER" />
        <category android:name="android.intent.category.DEFAULT" />
    </intent-filter>
</activity>
```

- 문서 예시에는 `android:exported`가 없다. `component`="service"용 `<receiver>` 매니페스트 예시도 없다. [gleo-deliver] [gleo-examples]
- Gleo AI가 만드는 Gleo Action JSON (원문 예시): `{"packageName": "ai.dot42.pleos.reminderapp", "action": "ADD_REMINDER", "actionParameters": {"title": "미팅", "dateTime": "2025-02-20T06:00:00Z"}}` [gleo-examples]
- Intent 변환 규칙 원문 [gleo-examples]:
  - "`action` 값은 Intent의 action으로 설정됩니다."
  - "`packageName` 값은 대상 앱을 지정하는 데 사용됩니다."
  - "`actionParameters` JSON 객체는 파싱되며, 각 key-value 쌍이 개별적으로 Intent에 추가됩니다 (putExtra)."
- Gleo AI 쪽 예시 코드: `intent.action = "ADD_REMINDER"`, `intent.putExtra("ACTION_PARAM", <JSON 문자열>)`, `intent.setPackage("ai.dot42.pleos.reminderapp")`, 그다음 `context.startActivity(intent)`("activity") 또는 `context.sendBroadcast(intent)`("service"). [gleo-examples]
- > **불일치:** 본문은 key-value마다 extra를 따로 넣는다고 한다. 그런데 같은 페이지의 코드는 파라미터 전체를 JSON 문자열 하나로 `"ACTION_PARAM"` extra에 넣고, 수신 예시도 그것을 파싱한다. 실제 형식은 미확인이다. [gleo-examples]

수신 예시 (원문) [gleo-examples]:

```kotlin
// component = "activity"
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // ...
        val bundle = intent?.extras
        bundle?.let { data ->
            runCatching {
                val actionParam = data.getString("ACTION_PARAM")
                // ...
            }.onFailure {
                Log.e(logTag, "Failed to get intent extras: $it")
            }
        }
    }
}

// component = "service"
class CustomReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent
    ) {
        if (intent.action == "ADD_REMINDER") {
            val rawJson = intent.getStringExtra("ACTION_PARAM") ?: "{}"
            try {
                // JSON parsing
                val json = JSONObject(rawJson)
                val title = json.optString("title", "No Title")
                val dateTime = json.optString("dateTime", "No DateTime")
                // To Do..
                Log.d("ReminderReceiver", "Title: $title, DateTime: $dateTime")
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("ReminderReceiver", "Failed to parse actionParameters")
            }
        }
    }
}
```

**Callback 응답** [gleo-callback] [gleo-examples]

- 원문: "Gleo AI는 다음 형식의 Broadcast를 수신해야 합니다." "Gleo AI는 Android 서비스이므로 broadcast 수신이 가능합니다." [gleo-examples]
- 필드 (원문) [gleo-callback]:
  - "functionName(string, required): Callback을 발생시킨 intent action의 이름입니다."
  - "status(enum, 필수): 앱에서 Gleo Action을 호출한 의 상태를 나타냅니다." ("호출한 의"는 원문 표기 그대로) 값: "success", "asking", "fail"
    - success: "Action이 정상적으로 처리되었을 때 사용됩니다."
    - asking: "Action을 실행하기 위해 추가 정보(예: 필수 parameter)가 필요하거나 사용자 확인 같은 후속 상호작용이 필요한 경우 사용됩니다."
    - fail: "Action이 완료될 수 없을 때 사용됩니다."
  - "message(string, 필수): 호출 결과에 대한 짧은 설명으로, 완전한 문장으로 작성되어야 합니다." (예시는 모두 영어)
  - "mode(enum, 선택, 기본값은 "voice"): Gleo AI가 사용자에게 답변을 전달하는 방법을 나타냅니다." 값: "voice", "beep", "silent"
  - "data(string, 선택): 요청 결과를 JSON 객체 배열 문자열 형식(key-value 형식의 객체들)으로 표현한 값입니다."
- **타임아웃** 원문: "참고로, Gleo는 timeout을 위해 7초 후 Action을 취소합니다." [gleo-callback]

응답 보내기 (원문) [gleo-examples]:

```kotlin
import android.content.Intent
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

fun sendGleoBroadcast(
    functionName: String,
    status: String,
    message: String,
    data: String? = null
) {
    runCatching {
        val jsonObject = JSONObject().apply {
            put("functionName", functionName)
            put("status", status)
            put("message", message)
            data?.let { put("data", it) }
        }
        Intent("ai.pleos.playground.intent.action.APP_ACTION_RESPONSE").apply {
            putExtra("packageName", packageName)
            putExtra("response", jsonObject.toString())
        }.also {
            sendBroadcast(it)
            Log.d("ThirdPartyApp", "respondAction success jsonObject: $jsonObject")
        }
    }.onFailure {
        Log.e("ThirdPartyApp", "respondAction onFailure >> $it")
    }
}
```

- 예시 코드의 응답 Intent에는 `setPackage`가 없다. 응답 broadcast를 보낼 때 필요한 권한도 문서에 없다. [gleo-examples]
- 예시 코드에는 `mode` 필드를 넣는 부분이 없다 (기본값 "voice"). [gleo-examples] [gleo-callback]

### 전화 (2026-11-03 조사)

- Pleos 문서에 서드파티 앱의 전화 걸기(`Intent.ACTION_DIAL`/`ACTION_CALL`, 블루투스 연결 폰 통화)에 대한 허용·금지 문장이 없다 → 미확인.
- 관련 사실:
  - 하드웨어 접근 정책: Telephony(전화)는 차량에서 제한될 수 있는 모바일용 API로 나열되어 있다. [hw-access]
  - 지역별 주행 규제 표: 북미는 주행 중 "다이얼패드" 사용 제한(`UX_RESTRICTIONS_NO_DIALPAD(1)`). [driving-policy] → [ux-guidelines.md](ux-guidelines.md#지역별-기능-허용-범위-driving-상태)
  - 오디오 포커스 가이드에 CALL 컨텍스트(`CALL`, `VOICE_COMMUNICATION`, `VOICE_COMMUNICATION_SIGNALING`)가 있다. 통화 중 MUSIC 요청은 거부(R)된다. [audio-focus]
  - 에뮬레이터 2.1.0에서 "블루투스 동글 연동 제공 종료". 에뮬레이터로 폰 연결 통화를 시험할 수 없다. [emulator-release-note]
  - 디자인 가이드의 System Color 토큰 분류에 "Phone"이 있다. 서드파티 앱의 전화 연동 방법은 문서에 없다. [design-colors]
- > **관찰 (2026-11-05, Pleos 에뮬레이터 v2.1.0, 실차 아님. 009 진료 카드):** 우리 앱(application context, `FLAG_ACTIVITY_NEW_TASK`, 권한·`<queries>` 없음)에서 `ACTION_DIAL` + `tel:02-0000-0000`을 보내면 `ai.umos.callandmessage/ai.umos.mainscreen.MainScreenActivity`("전화" 앱)가 열렸다 (logcat `START u10 {act=android.intent.action.DIAL ... cmp=ai.umos.callandmessage/...} result code=0`, 예외 없음).
  > - 전화 앱은 "최근 통화" 탭으로 열리고, **넘긴 번호는 화면에 보이지 않았다.** 폰이 연결되지 않은 에뮬레이터라서인지, 이 앱이 DIAL의 번호를 쓰지 않는지는 모른다 (블루투스 동글 연동 종료로 시험할 수 없다).
  > - 뒤로 가거나 우리 앱을 다시 열면 같은 진료 카드로 돌아왔다.
  > - 실차·앱 심사에서 허용되는지는 여전히 문의 B4다. 일반 폰 에뮬레이터(구글 전화 앱)에서는 번호가 채워진 다이얼 화면이 열렸다.

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
- [x] AAOS Car API(`android.car`, `CarPropertyManager`, `CarUxRestrictionsManager`)를 쓰기 위한 Gradle 설정: Google 문서의 `useLibrary("android.car")`. **Pleos 문서와 공식 샘플에는 없다** (2026-11-05) → 위 "주행 규제(주행/정차) 판단은 `CarUxRestrictions`로 한다"
- [x] `CarUxRestrictionsManager`를 얻는 방법(`Car.createCar` → `getCarManager(Car.CAR_UX_RESTRICTION_SERVICE)`)과 리스너 등록: Google 문서로 확인. 레퍼런스에 권한 표기 없음 (2026-11-05)
- [ ] 서드파티 앱이 Pleos에서 `Car.PERMISSION_SPEED`(dangerous), `Car.PERMISSION_POWERTRAIN`을 받을 수 있는지, 심사에서 별도 승인이 필요한지. **2026-11-05 재확인: Pleos 문서에 없음** (SDK API 정책, 하드웨어 접근 정책, Vehicle 권한, FAQ, 앱 제출 가이드). Pleos 문의 또는 에뮬레이터에서 권한 부여 여부 관찰이 필요하다.
- [x] Pleos 에뮬레이터에서 `Car.createCar()`와 `CarUxRestrictionsManager`가 동작하는지 → 동작한다 (2026-11-05 관찰, 우리 앱 006 구현).
  - 4인자 `createCar(context, null, CAR_WAIT_TIMEOUT_DO_NOT_WAIT, listener)`로 연결했다. 권한 선언 없이 `SecurityException`이 나지 않았다. 매니페스트 `<uses-library android:name="android.car">` 없이도 `NoClassDefFoundError`가 나지 않았다. `useLibrary("android.car")`(compileSdk 35)로 린트 `NewApi` 경고도 없었다.
  - 앱 로그: 기어 P → `DO=false active=0x0`, 기어 D(정지·30km/h) → `DO=true active=0x50`. `dumpsys`의 값과 같다.
  - 일반 폰 에뮬레이터(`FEATURE_AUTOMOTIVE` 없음)에서는 `Car`를 부르지 않고 UNKNOWN으로 둔다. 크래시 없음.
  - 실차는 미확인.
- [x] Pleos에서 기어 D + 속도 0(AOSP의 Idling)일 때 규제(DO)가 걸리는지 → 에뮬레이터 덤프로는 걸린다 (2026-11-05, [emulator-setup.md](emulator-setup.md#pleos-에뮬레이터에서-직접-해-본-것-관찰-2026-11-05)). 앱에서 `isRequiresDistractionOptimization()` 값을 직접 찍어 보는 것은 구현 때 확인한다. Pleos 정책표는 Parking/Driving 둘뿐이고 매핑 사양은 비공개다. → [ux-guidelines.md](ux-guidelines.md#미확인)
- [ ] Android for Cars App Library를 써도 되는지 (허용·금지 모두 문서에 없음)
- [ ] Pleos가 명시한 "권장" compileSdk/targetSdk/AGP/Kotlin/JDK (문서는 샘플 기준값만 제시)
- [ ] 화면 해상도(px)와 밀도(dpi) → [ux-guidelines.md](ux-guidelines.md#미확인)
- [x] `Vehicle` import 경로와 `initialize()`/`release()` 시그니처, 2.3.2 해결 여부 → 위 "2.3.2 AAR에서 직접 확인한 것" (2026-11-03)
- [x] 차량 서비스가 없는 기기에서 `initialize()`의 동작 → 위 "차량 서비스가 없는 기기에서의 동작" (관찰, 2026-11-03)
- [ ] 차량 서비스 연결 실패를 앱이 알 수 있는 방법 (도메인 API `onFailed`로 오는지, 별도 콜백이 있는지)
- [ ] Gear 도메인(`getGear()`): 필요한 권한, `VehicleGear` 값, 실제 지원 여부. 문서에는 없고 AAR에만 있다. **2026-11-05 재확인: API Reference(패키지 목록, `gear/entry` 404, constant·listener 패키지), SDK 릴리스 노트 v2.3.2, 공식 샘플 카탈로그 모두에 없음.**
- [ ] 같은 `Vehicle` 인스턴스를 `release()`한 뒤 다시 `initialize()`해도 되는지. 우리 앱은 매번 새로 만들어 이 문제를 피한다.

### PawCare 기획 관련 (2026-11-03 조사)

- [x] STT/TTS 생성자, 모드 지정, `setLocale`과 지원 언어(`ko-KR` 포함), 결과 리스너, 앱 등록 방법 → 위 TextToSpeech / SpeechToText 절
- [x] NaviHelper 목적지 요청(`requestRoute(RouteInfo)`), 도착 이벤트(`onDestinationArrived`), 권한별 API → 위 NaviHelper 절
- [x] Pleos 에뮬레이터에서 STT `ON_DEVICE`가 Client ID/Secret 없이 동작하는지 → 동작한다 (PCM 입력으로 한국어 인식 확인, 2026-11-05). 마이크 입력은 결과까지 미확인. 위 "STT `ON_DEVICE` 실행 확인"
- [ ] 콘솔에서 "준비 중"인 STT/TTS를 `ON_DEVICE` 모드로 심사 제출 앱에서 써도 되는지. `STT_SERVICE`/`TTS_SERVICE` 권한에 사전 승인이 필요한지. (문서에 없음. Pleos 문의 필요)
- [ ] Pleos 에뮬레이터에 On-device STT/TTS 엔진이 있어 실제로 인식·합성되는지 (GPU PC에서 직접 실행해 확인)
- [x] `Mode.ON_DEVICE` 상수 이름 → `HYBRID`, `SERVER`, `ON_DEVICE` 확인 (AAR `javap`, 2026-11-03)
- [ ] Android 표준 `SpeechRecognizer`, `android.speech.tts.TextToSpeech`를 Pleos에서 써도 되는지, `RECORD_AUDIO`가 필요한지
- [ ] 서드파티 앱의 전화 걸기 (`ACTION_DIAL`/`ACTION_CALL`, 블루투스 연결 폰 통화) 허용 여부
- [ ] Android 표준 `LocationManager`(`ACCESS_FINE_LOCATION`) 위치를 Pleos에서 받을 수 있는지
- [x] NaviHelper 데이터 클래스 필드 → NaviHelper 절 "산출물로 확인" (`TripInfo.eta`, `DrivingState.duration/distance`, `CurrentLocationInfo` 좌표). 단위는 미확인 (2026-11-03)
- [ ] `CurrentLocationInfo.address`가 null이나 빈 값으로 오는 경우가 있는지 (javap로는 안 보인다). 에뮬레이터에서는 매번 "수정구 시흥동"이었다 (011). 실차 등 다른 위치에서는 미확인
- [x] NaviHelper 인스턴스 둘을 동시에 연결해도 되는지 (길안내 중에 현재 위치 묻기) → 에뮬레이터에서 됐다. 한쪽을 release해도 다른 쪽 연결과 내비 경로는 그대로다 (011, NaviHelper 절 "현재 위치" 관찰). 실차는 미확인
- [ ] 내비 지도가 준비되지 않았을 때(`mapStatus=MapInitComplete`, "navi not ready") 버려진 get 요청에 답을 줄 방법이 있는지. 에뮬레이터에서는 내비 화면을 한 번 띄워야 했다 (011). 실차에서 시동 직후에도 그런지는 미확인
- [ ] `TripInfo.eta`가 "도착까지 남은 시간"인지 "도착 예정 시각"인지, `DrivingInfo.destination.duration`과 무엇이 다른지. getter 이름만 있고 뜻을 설명하는 문서·클래스 주석이 없다 (2026-11-05, Fake 길안내 계획 005에서 정리). 우리 앱은 남은 시간을 `Duration`으로 받아 변환을 Real 래퍼 안에 가둔다.
- [ ] `RouteInfo`의 `poiId`/`poiSubId`가 필수인지, 외부 장소 검색 결과(좌표+이름)만으로 경로를 요청할 수 있는지. **2026-11-05 재확인: 여전히 문서에 없음** → 아래 010 R3
- [ ] `requestTrip(TripRequest)`의 동작 (설명 문서 없음)
- [ ] 샘플 매니페스트의 `NAVI_CUSTOM_ROUTE`, `NAVI_CUSTOM_ETC`가 어느 API에 필요한지
- [ ] Pleos 에뮬레이터에서 NaviHelper 요청을 받는 기본 Navigation 앱이 동작하는지
- [ ] Gleo Interface(음성 명령 연동) 신청 자격과 사용 가능 상태: **문서에 없음** (2026-11-03 조사). Gleo 가이드 6쪽, 프로젝트 생성·SDK 신청, SDK 개요, 앱 제출·심사 가이드에 자격·신청·제휴 문장이 없다. SDK 목록과 콘솔에도 Gleo가 없다. Pleos·주최측에 문의해야 한다 → 위 "Gleo Interface" 절
- [ ] Gleo 스키마 검토 기간, 전달 경로(앱 심사와 함께인지 따로인지): 문서에 없음 (2026-11-03)
- [ ] Pleos 에뮬레이터에 Gleo AI가 들어 있는지, 승인 전 스키마를 로컬에서 시험하는 방법: 문서에 없음 (setup, 에뮬레이터 릴리스 노트, 앱 테스트 가이드 확인, 2026-11-03). GPU PC에서 직접 확인하거나 문의해야 한다.
- [ ] Gleo Action의 extra 형식: 본문(key별 `putExtra`)과 코드(`ACTION_PARAM` JSON 하나)가 다르다 (불일치)
- [ ] App Schema의 `invokeWord` 필드 정의 (예시에만 있음)
- [ ] `component`="service"일 때 `<receiver>` 선언 방법, `android:exported` 값, 응답 broadcast(`APP_ACTION_RESPONSE`)에 필요한 권한

### 010 진짜 길안내 관련 (2026-11-05 조사, 계획 `docs/plans/active/010-real-navi-guidance.md` R1~R8)

문서와 공식 샘플로 조사한 결과다. API Reference 상세 페이지는 모두 본문이 비어 있어서, 남은 것은 AAR(`javap`, 매니페스트)과 에뮬레이터 관찰로 확인해야 한다. 자세한 내용은 위 NaviHelper 절에 있다.

- [x] R1 일부: 전체 클래스 이름 `ai.pleos.playground.navi.helper.NaviHelper`, 문서 예시의 Kotlin/Java 호출 형태, 두 형태의 차이 (문서 원문) → "`initialize` 두 형태"
- [ ] R1 나머지: `onFailed` 인자의 정확한 타입, `initialize`의 반환형, 재시도 횟수·간격. 문서에 없음 → AAR
- [ ] R2: `NaviHelperEventListener` 기본 구현. 문서는 "필요한 콜백만 재정의하고"라고 쓰고 예시에서 3개만 재정의하지만, 기본 구현이 있다고 쓴 문장은 없다 → AAR. 콜백 스레드는 문서·샘플 모두에 없다 → 관찰
- [ ] R3: `poiId`/`poiSubId`/`address`의 nullable 여부와 기본값, `displayLatitude`/`displayLongitude`·`RouteOption`의 기본값. 문서에 없음 → AAR. Java 위치 인자 순서는 페이지마다 다르다 (불일치, 릴리스 노트는 3.0.5부터 위도 먼저). 좌표와 이름만으로 경로 요청이 되는지는 문서에 없음 → spike
- [ ] R4: AAR의 minSdk, 라이브러리가 스스로 넣는 권한·`<queries>`·서비스 버전 meta-data. 문서에 없음 → AAR
- [ ] R5: `NAVI_ROUTE`/`NAVI_ROUTE_SEARCH`의 protectionLevel, 런타임 요청 필요 여부. 문서에 없음. 공식 샘플에는 런타임 요청 코드가 없다 → 에뮬레이터 관찰
- [ ] R6: 이벤트 수신(`addListener`, `onTripInfo`/`onDrivingInfo`/`onDestinationArrived`/`onRouteCancelled`)에 필요한 권한. 문서에 없음 → AAR(`@RequiresPermission`)·관찰
- [x] R7 일부: `NaviErrorCode` 문서 값 4개 (`CAUSE_LOW_BATTERY`, `CAUSE_SEARCH_ROUTE_OVER_DISTANCE`, `CAUSE_SEARCH_ROUTE_TOO_SHORT_DISTANCE`, `CAUSE_SEARCH_ROUTE`) [navi-error]
- [ ] R7 나머지: `NaviErrorCode` 전체 목록 (문서는 "최신 목록은 API Reference"라고 하지만 그 페이지가 비어 있다), `NavigationStatus` 값, `RouteStartInfo` 필드. 문서에 없음 → AAR
- [x] R8 일부: `onRoutingCompleted`는 "경로 탐색이 완료되면 호출됩니다". v3.0.5에서 `onRouteEnded`("경로 탐색 완료 콜백")를 대체한다 [navi-dest] [sdk-release-note]
- [ ] R8 나머지: `onDestinationArrived`가 오는 조건, 도착하면 경로가 저절로 끝나는지 (뒤이어 `onRouteCancelled`/`onDestinationChanged`가 오는지). 문서에 없음 → spike 관찰

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
- [constant-pkg] https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.constant/entry (2026-11-05 확인)
- [listener-pkg] https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.listener/entry (2026-11-05 확인)
- [sdk-release-note] https://document.pleos.ai/docs/pleos-only/release-note/sdk-release-note (Vehicle SDK v2.3.2, 2026-10-19 릴리스. 2026-11-05 확인. 같은 페이지의 "NaviHelper v3.0.5" 절도 2026-11-05 확인)
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
- [navi-api-ref] https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/entry (하위: `.../navi-helper/ai.pleos.playground.navi.helper/navi-helper/entry`, `.../ai.pleos.playground.navi.data/entry`, `.../ai.pleos.playground.navi.constants/entry`, `.../ai.pleos.playground.navi.helper.listener/entry`. 2026-11-05 재확인)
- [navi-error] https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/error-handling (2026-11-05 확인)
- [navi-waypoints] https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/waypoint-settings (2026-11-05 확인)
- [navi-route-wp] https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/route-with-waypoints (2026-11-05 확인)
- [navi-events], [navi-perm]은 2026-11-05에 영문판(`https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/NaviHelper/...`)과 대조했다. 내용이 같다.
- [api-compat] https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/api-compatibility-availability
- [navi-app-guide] https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/navigation-app-development-guide
- [audio-focus] https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/audio-focus-development-guide/audio-focus-development
- [driving-policy] https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/driving-restiction-policy
- [design-colors] https://document.pleos.ai/docs/connect/guide/docs-design/foundations/colors
- [emulator-release-note] https://document.pleos.ai/docs/pleos-only/release-note/emulator-release-note
- [gleo-core] https://document.pleos.ai/docs/pleos-only/app-integration-guide/guide/core-concepts
- [gleo-deliver] https://document.pleos.ai/docs/pleos-only/app-integration-guide/guide/deliver-schema (영문판 `/en/docs/...`도 대조)
- [gleo-schema] https://document.pleos.ai/docs/pleos-only/app-integration-guide/guide/app-capability (영문판 대조)
- [gleo-callback] https://document.pleos.ai/docs/pleos-only/app-integration-guide/guide/action_callback
- [gleo-examples] https://document.pleos.ai/docs/pleos-only/app-integration-guide/guide/examples
- [gleo-radio] https://document.pleos.ai/docs/pleos-only/app-integration-guide/use-cases/radio-app
- [dev-guide-intro] https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/intro
- [app-review] https://document.pleos.ai/docs/pleos-only/distribute/app-review-guide (하위 `review-functionality-performance-stability`, `review-app-registration-info`도 확인)
- [form-factor-test] https://document.pleos.ai/docs/pleos-only/app-test-guide/form-factor-test-guide
- [virtual-console] https://document.pleos.ai/docs/pleos-only/app-test-guide/virtual-console-guide
- [resources] https://developers.pleos.ai/playground/discover/resources (`pleos.ai/playground/discover/resources`가 301로 옮겨 감, 2026-11-03)
- [dev-journey] https://developers.pleos.ai/playground/discover/developer-journey
- 공식 샘플 저장소 ([sample-app] 페이지가 링크한 Pleos-Playground 조직, main 브랜치):
  - [gh-vehicle] https://github.com/Pleos-Playground/pleos-sample-vehicle-app (`README.md`, `app/build.gradle.kts`, `gradle/libs.versions.toml`, `gradle/wrapper/gradle-wrapper.properties`, `app/src/main/AndroidManifest.xml`)
  - [gh-navi] https://github.com/Pleos-Playground/pleos-sample-navi-app (`app/build.gradle.kts`, `gradle/libs.versions.toml`)
  - [gh-fused] https://github.com/Pleos-Playground/pleos-sample-fusedlocation-app (`app/build.gradle.kts`, `gradle/libs.versions.toml`)
  - [gh-vehicle-src] https://github.com/Pleos-Playground/pleos-sample-vehicle-app (`app/src/main/java/.../MainViewModel.kt`, `.../vehicle/VehicleClient.kt`, 커밋 959981d)
  - [gh-navi-src] https://github.com/Pleos-Playground/pleos-sample-navi-app (`README.md`, `app/src/main/AndroidManifest.xml`, `app/src/main/java/ai/pleos/playground/navidevapp/screen/vm/NaviHelperViewModel.kt`, `.../data/model/SdkApi.kt`, `.../data/sample/NaviTestData.kt`, main 브랜치, 2026-11-03)
    - 2026-11-05 추가 확인: `MainActivity.kt`, `MyApplication.kt`, `screen/NaviSdkScreen.kt`, `screen/vm/NaviSdkBaseViewModel.kt`, `app/build.gradle.kts`, `gradle/libs.versions.toml` (`pleosNaviHelperVersion = "3.0.5"`). 저장소의 커밋은 `76ed592` ("Initial commit", 2026-10-16) 하나뿐이다.
  - 조직 저장소는 5개다: vehicle, fusedlocation, navi 샘플과 `pleos-guide`, `pleos-playground-api`. **STT/TTS 공식 샘플은 없다.** (2026-11-03)
  - [gh-org] https://github.com/Pleos-Playground (GitHub API `orgs/Pleos-Playground/repos`로 2026-11-05 재확인: 여전히 5개. `pleos-guide`는 빈 저장소)
  - [gh-vehicle-catalog] https://github.com/Pleos-Playground/pleos-sample-vehicle-app (`app/src/main/java/ai/pleos/playground/sample/vehicle/explorer/PublicApiPolicy.kt`, `.../explorer/VehicleDomain.kt`, main 브랜치, 2026-11-05)
- 산출물:
  - [aar-2.3.2] Pleos Maven `ai.pleos.playground:Vehicle:2.3.2` AAR (`classes.jar`를 `javap`으로, `AndroidManifest.xml`, `proguard.txt`). 2026-11-03 확인.
  - [aar-navi-data] Pleos Maven `ai.pleos.playground:navi-data:3.0.4` AAR과 `NaviHelper:3.0.5` pom (`javap`). 2026-11-03 확인.
  - [aar-navi] Pleos Maven `ai.pleos.playground:NaviHelper:3.0.5`, `navi-data:3.0.4` AAR (`javap`, AAR 매니페스트). 2026-11-05 확인 (010 spike, 011 현재 위치).
  - [aar-stt] Pleos Maven `ai.pleos.playground:SpeechToText:3.3.1` AAR (`javap`). 2026-11-03 확인.
  - [maven-search] Pleos Maven 검색 API, 2026-11-03 확인. `https://nexus-playground.pleos.ai/service/rest/v1/search?repository=maven-releases&q=gleo` (`q=Gleo*`도) → items 비어 있음. 대조로 `q=NaviHelper` → 2.0.2, 2.0.3, 3.0.5.
  - [maven-meta] Pleos Maven `maven-metadata.xml` (2026-11-03 확인):
    - https://nexus-playground.pleos.ai/repository/maven-releases/ai/pleos/playground/SpeechToText/maven-metadata.xml
    - https://nexus-playground.pleos.ai/repository/maven-releases/ai/pleos/playground/TextToSpeech/maven-metadata.xml
    - https://nexus-playground.pleos.ai/repository/maven-releases/ai/pleos/playground/NaviHelper/maven-metadata.xml (versions 2.0.2, 2.0.3, 3.0.5; latest 3.0.5; lastUpdated 2026-10-19)
- 관찰:
  - [obs-no-service] 우리 앱(Real 모드)을 일반 폰 에뮬레이터(`my_avd`, API 34 google_apis)에서 실행하고 `adb logcat`으로 확인. 2026-11-03.
- Pleos 외 출처 (Pleos [faq]가 직접 링크한 Google 공식 문서):
  - [g-vehicle-property-ids] https://developer.android.com/reference/android/car/VehiclePropertyIds
  - https://developer.android.com/reference/android/car/hardware/property/CarPropertyManager
- Pleos 외 출처 (Pleos [driving-restriction-dev]가 직접 링크한 Google 공식 문서, 2026-11-05 확인):
  - [g-uxr-manager] https://developer.android.com/reference/android/car/drivingstate/CarUxRestrictionsManager
  - [g-uxr] https://developer.android.com/reference/android/car/drivingstate/CarUxRestrictions
- Pleos 외 출처 (Pleos가 링크하지 않은 Google·AOSP 공식 문서. Pleos 문서에 구현 방법이 없어서 참고했다. 2026-11-05 확인):
  - [g-uxr-listener] https://developer.android.com/reference/android/car/drivingstate/CarUxRestrictionsManager.OnUxRestrictionsChangedListener
  - [g-car] https://developer.android.com/reference/android/car/Car
  - [g-car-lifecycle] https://developer.android.com/reference/android/car/Car.CarServiceLifecycleListener
  - [g-parked-aaos] https://developer.android.com/training/cars/parked/automotive-os ("Meet driver distraction requirements" > "Listen to user experience restrictions")
  - [g-emulator] https://developer.android.com/training/cars/testing/emulator ("Simulate driving")
  - [aosp-uxr] https://source.android.com/docs/automotive/driver_distraction/car_uxr
  - [aosp-consume] https://source.android.com/docs/automotive/driver_distraction/consume
