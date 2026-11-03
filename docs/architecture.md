# 아키텍처

기술 스택과 코드 구조에 대한 결정과 그 근거를 적는다. 코드와 이 문서가 다르면 이 문서부터 고친다.

## 결정

| 항목 | 결정 | 근거 | 버린 대안 |
|---|---|---|---|
| UI | Jetpack Compose + Material3 | 공식 Pleos 샘플 3개가 모두 Compose다. 팀이 웹(React) 위주라 비슷한 방식이 배우기 쉽다 | XML Views: 코드가 많고 상태 반영을 손으로 해야 한다 |
| 빌드 값 | AGP 8.8.2, Kotlin 2.0.0, Gradle 8.10.2, JDK 17, compile/target SDK 35, minSdk 28 | 공식 vehicle 샘플과 같은 값이라 Pleos 라이브러리와 충돌할 일이 적다. minSdk 28은 Pleos 요구다 | compile/target 34: 에뮬레이터 API와 같지만, 라이브러리가 35를 요구하면 막힌다 |
| 모듈 | 단일 `app` 모듈 | 마감이 짧고 앱이 작다 | 멀티 모듈: 빌드 설정만 늘어난다 |
| 화면 상태 | MVVM (ViewModel + StateFlow), 단방향 흐름 | 안드로이드 표준이다. 테스트하기 쉽다 | |
| DI | 수동 DI. Application이 `AppContainer`를 가진다 | 라이브러리가 필요 없고 빌드가 빠르다. 바꿔 끼울 구현이 몇 개뿐이다 | Hilt: 공식 navi 샘플이 kapt로 쓴다. 설정이 많고 빌드가 느려진다 |
| Fake/Real 전환 | debug 빌드에서만 보이는 앱 안 패널로 바꾼다. 재시작하면 적용된다. release는 항상 Real | 빌드 종류가 늘지 않아서 훅과 CI의 태스크 이름(`assembleDebug` 등)을 그대로 쓴다 | 빌드 flavor: `assembleFakeDebug`처럼 태스크 이름이 바뀐다 |
| 테스트 | JUnit4 + kotlinx-coroutines-test + Turbine | JVM 단위 테스트만으로 Flow와 ViewModel을 검증한다. 에뮬레이터가 필요 없다 | |
| 비밀값 | `secrets.properties` → BuildConfig. 파일이 없으면 빈 값 | CI와 새 팀원 PC에서도 빌드가 깨지지 않는다 | |

## 서비스 기능 스택 (PawCare, 2026-11-03 결정)

서비스: 반려동물 의료 이동 코파일럿 (가칭 PawCare Drive)
- 운전 중: 음성으로 증상을 말하면 병원 연결과 길안내를 시작한다.
- 정차 후: 진료 카드를 확인하고 공유한다.

근거가 된 Pleos 사실은 [docs/pleos/sdk-notes.md](pleos/sdk-notes.md)와 [ux-guidelines.md](pleos/ux-guidelines.md)에 있다.

| 영역 | 결정 | 근거 | 상태 |
|---|---|---|---|
| 의료 프로필 저장 | Room + 암호화 (SQLCipher) | Pleos 보안 가이드: 기기에 저장하는 민감정보는 암호화해야 한다 | 결정 |
| 음성 → 글자 | **Pleos SpeechToText `ON_DEVICE`** (`ko-KR`) + Fake | 네트워크와 앱 등록이 필요 없다. 기술 활용성 점수에 유리하다 | 결정. 단, 심사 가능 여부는 Pleos 문의 (아래) |
| 음성 안내 | Pleos TextToSpeech `ON_DEVICE` + Fake | STT와 같은 이유 | 결정. 문의는 STT와 같다 |
| 증상 → 진료 카드 | Claude API, 구조화 출력(JSON 스키마). 진단하지 않고 말한 내용만 정리한다. 카드 생성이 길안내·병원 연결을 막지 않게 뒤에서 따로 돈다 | 정해진 형식으로 증상·복용약·병력만 뽑는다 | **임시: `claude-opus-5-5` (effort `low`). 모델과 AI 범위는 팀 논의 필요** |
| 서버 | **Firebase** (Functions, TypeScript) | AI 키와 장소 검색 키를 앱에 넣지 않는다 (Pleos 보안 가이드: 키 하드코딩 금지, HTTPS·TLS 1.2+). 팀의 웹 경험과 가깝다 | 결정 |
| 병원 검색 | 카카오 로컬 API (서버를 거쳐서) | 병원 이름·좌표·전화번호 | 결정 |
| 길안내 | Pleos NaviHelper `requestRoute(RouteInfo)` | 콘솔 "사용 가능" SDK | 결정. `poiId` 없이 좌표만으로 요청되는지는 미확인 |
| 도착 예정 시간 | NaviHelper `TripInfo.eta`, `DrivingInfo.destination.duration` | navi-data AAR에서 필드 확인 | 결정. 단위(초/분)는 실기기 확인 필요 |
| 현재 위치 | NaviHelper `getCurrentLocationInfo()` | FusedLocation이 "준비 중"이라 대신 쓴다 | 결정 |
| 도착 감지 | NaviHelper `onDestinationArrived` | 도착하면 카드 화면으로 넘어간다 | 결정 |
| 병원 전화 | 안드로이드 다이얼러(`ACTION_DIAL`). 안 되면 번호를 크게 보여준다 | | 미확인 (Pleos 문서에 없음, 에뮬레이터로 시험 불가) |
| 카드 공유 | MVP: 정차 화면에 크게 보여주기. 나중에 QR·링크 | 서버 작업이 가장 적다 | 결정 |
| 운전 중 사용 | DO(`distractionOptimized`) 선언 | 음성 SOS가 운전 중에 동작해야 한다 | Pleos 선언 방법 미확인 (문의) |

### 사용하기로 한 Pleos 권한 (AGENTS.md 규칙 2, 2026-11-03 팀 승인)

- `pleos.car.permission.NAVI_ROUTE_SEARCH`: 병원까지 경로 요청
- `pleos.car.permission.NAVI_ROUTE`: 현재 위치, 도착 예정 시간, 경로 정보 조회
- `pleos.car.permission.STT_SERVICE`: 음성 인식
- `pleos.car.permission.TTS_SERVICE`: 음성 안내

이 4개 밖의 `pleos.car.permission.*`은 다시 승인을 받는다. 앱 마켓 심사 전에는 Pleos 승인 범위인지 확인한다 (아래 문의).

### Pleos(주최측)에 문의할 것

문의처: appdevchallenge@hyundai-partners.com

1. 콘솔에서 "준비 중"인 SpeechToText·TextToSpeech를 `ON_DEVICE` 모드로 쓴 앱이 심사를 통과할 수 있는가? `STT_SERVICE`·`TTS_SERVICE` 권한에 사전 승인이 필요한가?
2. 주행 중에도 쓰는 앱(DO)은 Pleos에서 어떻게 선언하는가? Maps/Navigation이 아닌 앱도 주행 중 음성 입력이 허용되는가?
3. 이 서비스에 맞는 앱 카테고리는 무엇인가 (Lifestyle / Mobility Services)?
4. 서드파티 앱이 차량에서 전화를 걸 수 있는가 (`ACTION_DIAL`)?

## 패키지 구조

```
app/src/main/java/com/meongcare/app/
  MeongCareApplication.kt     AppContainer를 만들고, REAL 모드면 VehicleConnection을 수명주기에 붙인다
  MainActivity.kt             Compose 화면을 띄우고, debug 패널에 AppContainer의 값과 동작을 넘긴다
  di/
    AppContainer.kt           모드에 맞는 구현을 골라 들고 있다 (수동 DI)
    DataMode.kt               FAKE / REAL
    DataModeStore.kt          debug에서 고른 모드를 저장 (release는 항상 REAL)
    AppRestarter.kt           모드를 저장하고 앱을 다시 시작 (debug)
  domain/driving/
    DrivingState.kt           PARKED / DRIVING / UNKNOWN
    DrivingStateSource.kt     주행 상태를 Flow로 주는 인터페이스
  data/driving/
    FakeDrivingStateSource.kt 손으로 바꾸는 Fake (처음엔 PARKED)
    RealDrivingStateSource.kt 자리표시. UNKNOWN만 낸다 (CarUxRestrictions 구현 예정)
  data/vehicle/
    VehicleConnection.kt      Pleos Vehicle initialize/release를 한 곳에서
  ui/theme/                   Color, Type(글자 크기), Dimens(터치 크기), Theme
  ui/home/                    HomeViewModel, HomeUiState, HomeScreen, ParkedScreen, DrivingScreen
  ui/debug/                   DebugPanel (상태 없음. 값과 동작은 MainActivity가 넘긴다)
app/src/test/java/com/meongcare/app/
  testutil/MainDispatcherRule.kt
  data/driving/, ui/home/     단위 테스트
```

## 의존 방향

```
ui ──▶ domain ◀── data
         ▲
         └── di (전부 알고 연결만 한다)
```

- `ui`는 `domain`의 인터페이스만 쓴다. `data`를 import하지 않는다.
- `data`는 `domain`을 구현한다.
- Pleos SDK(`ai.pleos.*`)와 AAOS Car API(`android.car.*`)는 `data/` 아래에서만 import한다 (AGENTS.md 아키텍처 규칙).

## 데이터 모드 (Fake / Real)

- 모드는 프로세스가 시작될 때 한 번 정한다.
  - debug: debug 패널에서 고른 값. 기본은 Fake다.
  - release: 항상 Real이다.
- debug 패널에서 모드를 바꾸면 저장하고 앱을 다시 시작한다. 실행 중에 데이터 소스를 바꾸지 않아서 상태가 꼬이지 않는다.
- Fake 모드에서는 Pleos `Vehicle`을 만들지 않는다. 일반 에뮬레이터에는 차량 서비스가 없기 때문이다.

## 주행 상태와 화면

- 주행 상태는 `PARKED` / `DRIVING` / `UNKNOWN` 세 가지다.
- Real 구현은 AAOS `CarUxRestrictions`를 기준으로 한다 (docs/pleos/ux-guidelines.md). 아직 구현하지 않았다.
- 주행 상태를 모르면(`UNKNOWN`) 안전을 위해 주행 화면을 보여준다.
  - 앱 아이디어와 DO(주행 중 사용) 선언 여부가 정해지면 다시 정한다.
- 주행 화면 규칙 (AGENTS.md 규칙 3):
  - 큰 글씨의 상태 한 줄과 큰 버튼 1개 이하만 둔다.
  - 텍스트 입력, 스크롤, 애니메이션은 두지 않는다.
- 정차 화면은 일반 기능과 텍스트 입력을 둘 수 있다.

## Vehicle SDK 수명주기

- `Vehicle`의 `initialize()`와 `release()`는 `data/vehicle/VehicleConnection`에서만 부른다 (AGENTS.md 아키텍처 규칙).
- REAL 모드에서만 만든다. `MeongCareApplication`이 `ProcessLifecycleOwner`에 붙인다.
  - 앱이 화면에 보이면(onStart): 새 `Vehicle`을 만들고 `initialize()`
  - 화면에서 사라지면(onStop): `release()`하고 버린다
  - 같은 인스턴스를 다시 initialize하지 않는다. 공식 샘플도 그렇게 한다 (docs/pleos/sdk-notes.md "2.3.2 AAR에서 직접 확인한 것").
- `initialize()`가 실패하면(차량 서비스가 없는 기기 등) 로그만 남기고 앱은 계속 돈다.
- 차량 기능 래퍼는 `VehicleConnection.vehicle`을 쓴다. 도메인 API 콜백을 기다리는 중에 release되지 않게 하는 처리는 래퍼를 만들 때 함께 한다.

## 테스트 전략

- 단위 테스트(`app/src/test/`)로 Fake 데이터 소스와 ViewModel을 검증한다.
- 화면은 `/verify` 스킬로 에뮬레이터 스크린샷을 찍어 확인한다.
- Real 구현은 Pleos 에뮬레이터(GPU PC)에서 확인한다.
