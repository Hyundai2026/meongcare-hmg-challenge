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

## SDK 사용 가능 상태 (Playground 콘솔 기준) [create-project]

| SDK | 상태 |
|---|---|
| Vehicle State | 사용 가능 |
| NaviHelper | 사용 가능 |
| FusedLocation | 준비 중 |
| TextToSpeech | 준비 중 (Client ID/Secret 필요) |
| SpeechToText | 준비 중 (Client ID/Secret 필요) |
| Vehicle Control | 준비 중 |

- 그 밖의 SDK는 사용 신청이 필요하고, 일부는 제휴 계약이 필요하다.
- **"준비 중"인 SDK에 핵심 기능을 걸지 않는다.** 기획 전에 콘솔에서 상태를 다시 확인한다.
- 앱 심사에는 "프로젝트 ID(Key)"가 필요하다. 에뮬레이터 2.1.0부터는 CRN이 필요 없다.

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
- 공식 샘플: https://github.com/Pleos-Playground/pleos-sample-fusedlocation-app [sample-app]

### TextToSpeech SDK (v3.3.1) [tts-intro] [tts-import] [tts-perm]

- 텍스트를 음성으로 합성해 차량 오디오로 재생한다. Pleos Connect Speech Service에 연결된다. [tts-intro]
- 모드: `HYBRID`(기본), `SERVER`, `ON_DEVICE`. 서버 기반 모드(`HYBRID`, `SERVER`)는 "Pleos Playground의 My Project 자격 증명을 사용한 앱 등록"이 필요하고, `ON_DEVICE`는 필요 없다. [tts-intro]
- 의존성:

```kotlin
implementation("ai.pleos.playground:TextToSpeech:3.3.1")
```

- 권한: `<uses-permission android:name="pleos.car.permission.TTS_SERVICE" />` [tts-perm]
- 오류 콜백이 `PERMISSION_DENIED`면 매니페스트 선언과 실행 환경의 권한 허용을 확인한다. [tts-perm]

### SpeechToText SDK (v3.3.1) [stt-intro] [stt-import] [stt-perm]

- 음성을 텍스트로 바꿔 돌려준다. 명령을 실행하지는 않는다. 인식한 명령을 실행하려면 Gleo Interface가 필요하다. [stt-intro]
- 진입 클래스: `ai.pleos.playground.stt.SpeechToText` [stt-intro]
- 모드: `HYBRID`(기본, 서버 우선 후 네트워크 실패 시 온디바이스), `SERVER`, `ON_DEVICE`. 서버 기반 모드는 My Project 자격 증명으로 앱 등록이 필요하다. `ON_DEVICE`만 쓰면 등록이 필요 없다. [stt-intro]
- 의존성:

```kotlin
implementation("ai.pleos.playground:SpeechToText:3.3.1")
```

- 권한: `<uses-permission android:name="pleos.car.permission.STT_SERVICE" />` [stt-perm]

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

## 출처

- [setup] https://document.pleos.ai/docs/connect/guide/getting-started/application-development/setup-connect-sdk-emulator
- [getting-started] https://document.pleos.ai/docs/pleos-only/getting-started
- [project-setup] https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/project-setup
- [vehicle-intro] https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/intro
- [sdk-overview] https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/
- [create-project] https://document.pleos.ai/docs/pleos-only/create-my-project
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
- 공식 샘플 저장소 ([sample-app] 페이지가 링크한 Pleos-Playground 조직, main 브랜치):
  - [gh-vehicle] https://github.com/Pleos-Playground/pleos-sample-vehicle-app (`README.md`, `app/build.gradle.kts`, `gradle/libs.versions.toml`, `gradle/wrapper/gradle-wrapper.properties`, `app/src/main/AndroidManifest.xml`)
  - [gh-navi] https://github.com/Pleos-Playground/pleos-sample-navi-app (`app/build.gradle.kts`, `gradle/libs.versions.toml`)
  - [gh-fused] https://github.com/Pleos-Playground/pleos-sample-fusedlocation-app (`app/build.gradle.kts`, `gradle/libs.versions.toml`)
  - [gh-vehicle-src] https://github.com/Pleos-Playground/pleos-sample-vehicle-app (`app/src/main/java/.../MainViewModel.kt`, `.../vehicle/VehicleClient.kt`, 커밋 959981d)
- 산출물:
  - [aar-2.3.2] Pleos Maven `ai.pleos.playground:Vehicle:2.3.2` AAR (`classes.jar`를 `javap`으로, `AndroidManifest.xml`, `proguard.txt`). 2026-11-03 확인.
- 관찰:
  - [obs-no-service] 우리 앱(Real 모드)을 일반 폰 에뮬레이터(`my_avd`, API 34 google_apis)에서 실행하고 `adb logcat`으로 확인. 2026-11-03.
- Pleos 외 출처 (Pleos [faq]가 직접 링크한 Google 공식 문서):
  - [g-vehicle-property-ids] https://developer.android.com/reference/android/car/VehiclePropertyIds
  - https://developer.android.com/reference/android/car/hardware/property/CarPropertyManager
