# Pleos Connect SDK: 검증된 사실

이 파일의 규칙:
- 공식 문서에서 확인한 것만 적고, 항목마다 출처를 단다.
- 확인하지 못한 것은 맨 아래 "미확인"에 둔다.
- 이 파일이나 공식 문서에 없는 API 이름·시그니처·의존성 좌표를 추측해서 코드에 쓰지 않는다.

마지막 갱신: 2026-11-03

## 빌드 설정

- **minSdk 28 이상.** 차량 보안 요구사항(서명키 rotation/lineage) 때문이다. [project-setup]
- **Gradle 8.0 이상.** [setup]
- 예제와 에뮬레이터 이미지 모두 **API 34** 기준이다. [setup]
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

[vehicle-intro] [project-setup]

> Getting Started 페이지에는 같은 좌표가 `2.3.1`로 적혀 있어 사이트 안에서 버전이 다르다. 2.3.2를 쓰고, 의존성 해결에 실패하면 2.3.1로 시도한다.

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

## 권한

- 예: `<uses-permission android:name="pleos.car.permission.CAR_DOORS" />` [getting-started]
- 권한은 Pleos App Market과 사전 협의·승인을 받아야 한다. 승인되지 않은 권한을 선언하거나 해당 API를 호출하면 앱 심사에서 탈락한다. [sdk-overview]
- API별 권한 표: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle-permission (아직 정리하지 않음)

## 미확인 (TODO: `pleos-docs-researcher` 서브에이전트로 채운다)

- [ ] FusedLocation / NaviHelper / TextToSpeech / SpeechToText 의존성 좌표
- [ ] 속도·기어·주행 상태 API (Vehicle API reference: `/docs/pleos-only/connect-sdk-pleos/Vehicle/entry`)
- [ ] API별 필요 권한 목록
- [ ] compileSdk / targetSdk / AGP / Kotlin / JDK 권장 버전
- [ ] Jetpack Compose 지원 여부, 화면 해상도·밀도·방향
- [ ] 운전자 주의분산 가이드, 주행 중 제한 정책 → `docs/pleos/ux-guidelines.md`로 정리
- [ ] Virtual Console로 차량 데이터 시뮬레이션하는 방법
- [ ] Vehicle SDK 샘플 앱 (`/docs/pleos-only/tutorial/vehicle-sdk-sample-app`)
- [ ] 앱 카테고리 (`/docs/pleos-only/vehicle-app-planning-guide/app-category`)

## 출처

- [setup] https://document.pleos.ai/docs/connect/guide/getting-started/application-development/setup-connect-sdk-emulator
- [getting-started] https://document.pleos.ai/docs/pleos-only/getting-started
- [project-setup] https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/project-setup
- [vehicle-intro] https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/intro
- [sdk-overview] https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/
- [create-project] https://document.pleos.ai/docs/pleos-only/create-my-project
- [planning-guide] https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/intro
