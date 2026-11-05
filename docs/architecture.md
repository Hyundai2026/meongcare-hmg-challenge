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
| Fake/시연/Real 전환 | debug 빌드에서만 보이는 앱 안 패널로 바꾼다. 재시작하면 적용된다. release는 시연 모드 고정 (007) | 빌드 종류가 늘지 않아서 훅과 CI의 태스크 이름(`assembleDebug` 등)을 그대로 쓴다 | 빌드 flavor: `assembleFakeDebug`처럼 태스크 이름이 바뀐다 |
| 테스트 | JUnit4 + kotlinx-coroutines-test + Turbine | JVM 단위 테스트만으로 Flow와 ViewModel을 검증한다. 에뮬레이터가 필요 없다 | |
| 비밀값 | `secrets.properties` → BuildConfig. 파일이 없으면 빈 값 | CI와 새 팀원 PC에서도 빌드가 깨지지 않는다 | |

## 서비스 기능 스택 (MeongCare, 2026-11-03 결정)

서비스: 반려동물 의료 이동 코파일럿. 앱 이름은 **MeongCare** (2026-11-05 사용자 결정. 기획 초기 가칭은 PawCare Drive)
- 운전 중: 음성으로 증상을 말하면 병원 연결과 길안내를 시작한다.
- 정차 후: 진료 카드를 확인하고 공유한다.

근거가 된 Pleos 사실은 [docs/pleos/sdk-notes.md](pleos/sdk-notes.md)와 [ux-guidelines.md](pleos/ux-guidelines.md)에 있다.

| 영역 | 결정 | 근거 | 상태 |
|---|---|---|---|
| 의료 프로필 저장 | Room + 암호화 (SQLCipher) | Pleos 보안 가이드: 기기에 저장하는 민감정보는 암호화해야 한다 | 결정. **지금(009)은 기기에 저장하지 않는다**: 프로필은 메모리의 Fake("보리"). 서버(보호자 웹) 연동 때 저장 방식을 정한다 |
| 음성 → 글자 | **Pleos SpeechToText `ON_DEVICE`** (`ko-KR`) + Fake | 네트워크와 앱 등록이 필요 없다. 기술 활용성 점수에 유리하다 | **재검토 (팀 논의)**: 콘솔 "준비 중" = 신청 불가라서 지금은 쓸 수 없는 것으로 본다 (2026-11-03 콘솔 확인). STT 없이도 핵심 흐름이 돌게 할지 정한다. 열리는 시점은 문의 B2 |
| 음성 안내 | Pleos TextToSpeech `ON_DEVICE` + Fake | STT와 같은 이유 | **재검토**: STT와 같다 |
| 증상 → 진료 카드 | Claude API, 구조화 출력(JSON 스키마). 진단하지 않고 말한 내용만 정리한다. 카드 생성이 길안내·병원 연결을 막지 않게 뒤에서 따로 돈다 | 정해진 형식으로 증상·복용약·병력만 뽑는다 | **임시: `claude-opus-5-5` (effort `low`). 모델과 AI 범위는 팀 논의 필요.** **지금(009)은 AI 없이 규칙으로 조합한다**: 등록된 프로필 + 큰 버튼으로 고른 증상(12개 프리셋) → `medicalCardOf`. Claude API는 A1 뒤 |
| 서버 | **Firebase** (Functions, TypeScript) | AI 키와 장소 검색 키를 앱에 넣지 않는다 (Pleos 보안 가이드: 키 하드코딩 금지, HTTPS·TLS 1.2+). 팀의 웹 경험과 가깝다 | 결정 |
| 병원 검색 | 카카오 로컬 API (서버를 거쳐서) | 병원 이름·좌표·전화번호 | 결정 |
| 길안내 | Pleos NaviHelper `requestRoute(RouteInfo)` (`NaviHelper:3.0.5`) | 콘솔 "사용 가능" SDK | **구현함 (010)**: `data/guidance/navi/PleosNaviClient`. 좌표 + 이름만으로 경로 요청이 된다 (`poiId` 빈 값, Pleos 에뮬레이터 확인). 차량 기기에서만 쓴다 (아래 "데이터 모드") |
| 도착 예정 시간 | NaviHelper `onRouteStarted`의 `duration`, `onDrivingInfo`의 `destination.duration` | 에뮬레이터에서 값이 내비 화면과 같았다 | **단위는 초** (2100 = 35분, `[obs-navi-emu]`). `onTripInfo`는 오지 않았다 |
| 현재 위치 | NaviHelper `getCurrentLocationInfo()` | FusedLocation이 "준비 중"이라 대신 쓴다 | 결정 |
| 도착 감지 | NaviHelper `onDestinationArrived` | 도착하면 카드 화면으로 넘어간다 | Real 모드에서 쓴다. **에뮬레이터에서는 내비 속 차가 움직이지 않아 확인하지 못했다** (JVM 테스트로만) |
| 병원 전화 | 안드로이드 다이얼러(`ACTION_DIAL`, 권한 없음, 걸지 않음). 안 되면 번호를 크게 보여준다 | | 구현함 (009, `data/phone/AndroidPhoneDialer`). Pleos 에뮬레이터에서 "전화" 앱이 열렸지만 번호는 보이지 않았다 ([sdk-notes.md](pleos/sdk-notes.md) "전화" 관찰). 실차는 미확인 (문의 B4). 시연용 번호 `02-0000-0000` |
| 카드 공유 | MVP: 정차 화면에 크게 보여주기. 나중에 QR·링크 | 서버 작업이 가장 적다 | 결정 |
| 운전 중 사용 | DO(`distractionOptimized`) 선언. `MainActivity`에 Google 방식 meta-data를 넣었다 (006, 2026-11-05) | 선언이 없으면 주행 중에 시스템 화면이 앱을 가려 주행 화면(남은 시간)이 보이지 않는다 | 결정. Pleos 에뮬레이터에서 동작 확인. 실차·심사 허용은 문의 B1. 안 되면 그 커밋을 revert한다 |
| 주행/정차 판단 | AAOS `CarUxRestrictionsManager`의 `isRequiresDistractionOptimization` (006) | Pleos·Google 모두 기어·속도가 아니라 규제 값에 반응하라고 한다. 권한이 필요 없다 | 결정. Pleos 에뮬레이터에서 동작 확인 |
| 보호자 등록 웹 | React + Vite + TypeScript (`web/`). 저장은 브라우저 localStorage 먼저 (008) | 보호자가 집에서 폰으로 의료정보를 미리 등록해서, 차에서는 글자를 입력하지 않게 한다 (AGENTS.md 규칙 3). 서버 없이 먼저 만들고, 저장소 인터페이스로 Firebase를 끼울 자리를 둔다 | 결정. 서버 연동·차 앱 연결은 문의 A1 뒤. 아래 "보호자 웹" |

### 사용하기로 한 Pleos 권한 (AGENTS.md 규칙 2, 2026-11-03 팀 승인)

- `pleos.car.permission.NAVI_ROUTE_SEARCH`: 병원까지 경로 요청
- `pleos.car.permission.NAVI_ROUTE`: 현재 위치, 도착 예정 시간, 경로 정보 조회
- `pleos.car.permission.STT_SERVICE`: 음성 인식
- `pleos.car.permission.TTS_SERVICE`: 음성 안내

이 4개 밖의 `pleos.car.permission.*`은 다시 승인을 받는다. 앱 마켓 심사 전에는 Pleos 승인 범위인지 확인한다 (아래 문의).

### Pleos(주최측)에 문의할 것

문의처: appdevchallenge@hyundai-partners.com
대회 공지·FAQ·가이드를 2026-11-03에 모두 확인한 뒤 정리했다. 공지에서 이미 확인된 것은 [docs/contest/requirements.md](contest/requirements.md)에 있다.

우선순위: **A1 → B1 → B2** 순으로 급하다. 셋 다 답에 따라 기획서 내용이 바뀐다.

**A. 대회 규정**

| # | 질문 | 공지에서 확인한 것 | "안 된다"면 |
|---|---|---|---|
| A1 | Pleos 외 외부 API(Claude API, 카카오 로컬 API)와 자체 서버(Firebase)를 써도 되는가? | FAQ "쓸 수 있는 SDK와 API"에는 Pleos SDK 5개만 있다. 외부 API 언급은 없다 | 카드 정리를 규칙 기반으로, 병원 목록은 미리 넣은 데이터로 바꾼다. 보호자 웹은 브라우저 저장만 쓰고, 차 앱으로 정보를 옮기는 방법을 다시 정한다 |
| A2 | 2차 PT 현장에 인터넷(Wi-Fi)이 있는가? 노트북을 가져가 화면을 연결하는가? 발표 시간은? 주최측은 제출 APK를 어떤 환경(Pleos 에뮬레이터 버전, 실차 헤드유닛)에서 구동 확인하는가? | 에뮬레이터 구동 화면으로 시연해도 된다. 제출물은 "정상적으로 구동·시연할 수 있는지 확인"한다. 장비·네트워크·시간·확인 환경은 언급 없음 | 대비함 (007): release APK는 시연 모드라 네트워크 없이 돌고, Pleos 에뮬레이터·일반 폰 에뮬레이터 어디서든 흐름이 끝까지 돈다 |
| A3 | AI 코딩 도구로 개발해도 되는가? | "순수 창작물이어야" 한다는 조항만 있다 (표절·기수상작 금지 취지) | 규정에 따른다 |
| A4 | 오픈소스 라이브러리는 라이선스를 지키면 써도 되는가? | 제3자 권리 침해 금지 조항만 있다 | 라이선스 고지를 추가한다 |
| A5 | 심사용 GitHub 계정 ID는 언제 공지되는가? | "공지된 심사용 GitHub 계정"이라고만 되어 있고 아직 공지 없음 | |

**B. Pleos 기술**

| # | 질문 | 문서에서 확인한 것 | "안 된다"면 |
|---|---|---|---|
| B1 | 주행 중에도 쓰는 앱(DO)은 Pleos에서 어떻게 선언하는가? Maps/Navigation이 아닌 앱도 주행 중 음성 입력이 허용되는가? | DO 미선언 앱은 주행 중 사용 불가. Pleos 문서에 선언 방법이 없다. 대회 개발 가이드는 "운전 중 음성을 받아 메모로 남기는" 앱을 예시로 든다 | "운전 중 음성 SOS"를 출발 전·정차 중 흐름으로 바꾼다 |
| B2 | 대회 FAQ는 SpeechToText·TextToSpeech·FusedLocation을 "쓸 수 있는 SDK"로 드는데, 콘솔에서는 "준비 중"(신청 불가)이다. **언제 "신청 필요"로 바뀌는가? 2차 PT(12/03) 전에 열리는가?** 열리기 전에 `ON_DEVICE` 모드로 개발·시연해도 되는가? 그 대신 안드로이드 표준 `SpeechRecognizer`를 써도 되는가? | 콘솔 "준비 중" = 신청 불가 (2026-11-03 콘솔 직접 확인). 문서상 `ON_DEVICE`는 앱 등록 불필요지만 사용 허가는 아니다 | 기본 흐름을 큰 버튼(증상 프리셋)으로 만들고, 음성은 열리면 붙인다 |
| B3 | 이 서비스에 맞는 앱 카테고리는 무엇인가 (Lifestyle / Mobility Services)? | 반려동물·의료 카테고리가 없다. 선언과 기능이 안 맞으면 즉시 반려 | 답에 맞춰 선언만 바꾼다 |
| B4 | 서드파티 앱이 차량에서 전화를 걸 수 있는가 (`ACTION_DIAL`)? | Telephony는 차량에서 제한될 수 있다 | 번호를 크게 보여 준다 (대비책 있음) |

## 패키지 구조

```
app/src/main/java/com/meongcare/app/
  MeongCareApplication.kt     AppContainer를 만들고, REAL 모드면 VehicleConnection을 수명주기에 붙인다
  MainActivity.kt             Compose 화면을 띄우고, debug 패널에 AppContainer의 값과 동작을 넘긴다
  di/
    AppContainer.kt           모드에 맞는 구현을 골라 들고 있다 (수동 DI)
    DataMode.kt               FAKE / DEMO(시연) / REAL, resolveDataMode(release는 DEMO), next(패널 전환 순서)
    SourcePlan.kt             모드 × 차량 기기 → 끼울 구현의 구성표 (순수 함수 sourcePlanOf)
    NaviPlan.kt               모드 × 차량 기기 → 차량 내비 사용 범위 NONE / ROUTE_ONLY / FULL (순수 함수 naviUseOf, 010)
    DataModeStore.kt          debug에서 고른 모드를 저장 (release는 항상 DEMO)
    AppRestarter.kt           모드를 저장하고 앱을 다시 시작 (debug)
  domain/driving/
    DrivingState.kt           PARKED / DRIVING / UNKNOWN
    DrivingStateSource.kt     주행 상태를 Flow로 주는 인터페이스
  domain/guidance/
    Destination.kt            길안내 목적지 (이름, 좌표, 주소?)
    GuidanceState.kt          Idle / Guiding(목적지, 남은 시간: Duration?) / Arrived(목적지)
    GuidanceSource.kt         길안내 인터페이스 (start / cancel / dismissArrival)
  data/driving/
    FakeDrivingStateSource.kt 손으로 바꾸는 Fake (처음엔 PARKED)
    RealDrivingStateSource.kt 주행 규제 값 → 주행 상태 (android.car 없음, JVM 테스트)
    UxRestrictionsClient.kt   주행 규제를 받는 경계 (인터페이스와 이벤트 타입)
    CarUxRestrictionsClient.kt  android.car로 규제를 받는 어댑터. android.car를 부르는 유일한 파일
    UxRestrictionsClientFactory.kt  FEATURE_AUTOMOTIVE면 어댑터, 아니면 null
  data/guidance/
    FakeGuidanceSource.kt     시간이 지나면 남은 시간이 줄고 도착한다 (scope·timing·진행 조건 주입)
    RealGuidanceSource.kt     자리표시. 항상 Idle (Real 모드 + 일반 폰에서만 쓰인다)
    NaviGuidanceSource.kt     전부 차량 내비 (Real + 차량 기기): 남은 시간·도착·취소를 내비 이벤트로
    NaviRouteOnlyGuidanceSource.kt  경로만 차량 내비 (시연 + 차량 기기): 내비 예상 시간에서 시연용 Fake를 시작, 시연 도착 시 내비 경로 종료
    navi/NaviClient.kt        내비 경계 (trip(destination): Flow<NaviEvent>). SDK 타입 없음
    navi/PleosNaviClient.kt   NaviHelper 어댑터. NaviHelper를 부르는 유일한 파일. 안내 한 번마다 연결하고 끝나면 release
    navi/NaviTiming.kt        내비 남은 초 → 남은 시간 규칙, 안내 시작 대기 30초
  domain/pet/                 PetProfile(형식 문서 대응), PetAge(나이 계산, 웹과 같은 규칙), PetProfileSource + PetProfileLoad
  domain/card/                Symptom(12개 프리셋, 선언 순서 = 화면·카드 순서), MedicalCardSession(고르는 중/카드 있음, 메모리)
  domain/hospital/            Hospital(길안내 목적지 + 전화번호 + 시연용 여부)
  domain/phone/               PhoneDialer(전화 앱 열기)
  data/hospital/
    SampleHospitals.kt        시연용 샘플 병원 1곳과 번호 02-0000-0000 (병원 검색이 붙기 전까지)
  data/pet/                   SamplePetProfiles("보리", 예시 JSON과 같은 값), FakePetProfileSource
  data/phone/                 AndroidPhoneDialer (ACTION_DIAL. ACTION_DIAL을 부르는 유일한 파일)
  data/demo/
    FakeTripScenario.kt       FAKE 전용: 안내 시작 → 출발(주행), 도착 → 주차(정차)
  data/vehicle/
    VehicleConnection.kt      Pleos Vehicle initialize/release를 한 곳에서
  ui/theme/                   Color, Type(글자 크기), Dimens(터치 크기), Theme
  ui/home/                    HomeViewModel, HomeUiState, HomeContent(1단계 화면 조합), ParkedContent(2단계: 정차 안), HomeScreen, ParkedScreen(처음 화면), DrivingScreen
  ui/card/                    MedicalCardViewModel, MedicalCardContent(medicalCardOf: 카드 조합), MedicalCardScreen, SymptomPickerScreen, CardTexts, PetVisuals(발바닥·아바타)
  ui/guidance/                GuidanceViewModel, GuidanceUiState, GuidanceText, SimulatedGuidanceNotice
  ui/debug/                   DebugPanel (상태 없음. 값과 동작은 MainActivity가 넘긴다)
app/src/test/java/com/meongcare/app/
  testutil/MainDispatcherRule.kt
  data/driving/, data/guidance/, data/demo/, data/pet/, domain/pet/, domain/card/, ui/home/, ui/guidance/, ui/card/   단위 테스트
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

## 데이터 모드 (Fake / 시연 / Real)

- 모드는 프로세스가 시작될 때 한 번 정한다 (`di/DataMode.kt`의 `resolveDataMode`).
  - debug: debug 패널에서 고른 값. 기본은 Fake다. 패널 버튼을 누를 때마다 Fake → 시연 → Real 순서로 바뀐다.
  - **release: 시연 모드 고정** (007, 사용자 결정 2026-11-05). 제출 APK로도 심사위원이 병원 길안내 흐름을 보게 하려는 것이다. 모드를 바꾸는 UI는 없다. Real 길안내(NaviHelper)가 생긴 뒤에도 시연 모드로 두고, 차량 기기에서는 경로만 내비로 보내기로 했다 (아래 "차량 내비", 010). `feat(di): release 빌드를 시연 모드로 고정` 커밋만 되돌리면 이전(항상 Real)으로 돌아간다.
  - **차량 내비 (010, 사용자 결정 2026-11-05)**: `di/NaviPlan.kt`의 `naviUseOf`가 `sourcePlanOf`의 길안내 칸보다 앞선다. 007의 구성표와 그 테스트는 그대로다.

    | 모드 | 차량 기기 | 길안내 |
    |---|---|---|
    | 시연 (release) | 예 | **경로만 차량 내비** + 남은 시간·도착은 시연용 (내비 예상 시간에서 시작, 주행 중에만 줄어듦). 시연 도착 시 내비 경로도 취소. 화면 문구 "경로는 차량 내비게이션으로 안내해요. 앱의 남은 시간과 도착은 시연용이에요." |
    | Real | 예 | **전부 차량 내비** (남은 시간·도착·취소를 내비 이벤트로) |
    | 그 밖 (Fake, 일반 폰) | | 지금처럼 (NaviHelper를 만들지 않는다) |

    - 시연에서 진짜 도착을 쓰지 않는 이유: Pleos 에뮬레이터에서는 기어·속도·GPS를 바꿔도 내비 속 차가 움직이지 않아 도착이 오지 않는다 (`[obs-navi-emu]`).
    - 내비가 경로를 계산하고 안내를 시작하기까지 30초까지 기다린다 (에뮬레이터에서 10~20초 걸렸다). 그동안 "경로를 찾는 중". 넘으면 시연은 기본값(12분)으로 시작하고, 전부 내비는 안내 없음으로 돌아간다.
    - 내비 이벤트 처리 (`data/guidance/navi/NaviTiming.kt`와 두 소스의 KDoc):
      - 남은 시간이 0 이하이거나 하루보다 길면 모름이다. 시연은 기본값으로 시작하고, 전부 내비는 이전 값을 둔다.
      - 내비 화면에서 경로를 취소하면("안내 종료") 앱도 안내 없음이다 (에뮬레이터 확인).
      - 내비 오류: 안내 시작 전이면 시연은 바로 기본값으로 시작하고, 전부 내비는 안내 없음이다. 시작 뒤 오류는 무시한다.
      - 내비에 이전 경로가 남아 있으면 경로 요청 직후 그 경로의 취소가 먼저 온다. 어댑터가 `onRouteStarted` 전의 취소를 거른다 (`[obs-navi-emu]`).
    - [길 안내 시작]을 누르면 내비 앱이 앞에 나와 우리 앱을 가린다. 남은 시간·카드는 GNB의 MeongCare 아이콘으로 돌아와 본다.
- debug 패널에서 모드를 바꾸면 저장하고 앱을 다시 시작한다. 실행 중에 데이터 소스를 바꾸지 않아서 상태가 꼬이지 않는다.
- 어떤 구현을 끼울지는 `di/SourcePlan.kt`의 `sourcePlanOf(mode, isAutomotive)`가 정한다. Android를 모르는 순수 함수라 표 전체를 JVM 테스트로 확인한다.

  | 모드 | 차량 기기 (`FEATURE_AUTOMOTIVE`) | 주행 상태 | 길안내 | 출발·주차 시나리오 | 주행 중에만 남은 시간이 줄어듦 | Vehicle 연결 | 시연용 표시 |
  |---|---|---|---|---|---|---|---|
  | Fake | 상관없음 | Fake | Fake | O | X | X | O |
  | 시연 | 예 (Pleos 에뮬레이터, 실차) | Real (주행 규제) | Fake | X | O | X | O |
  | 시연 | 아니오 (일반 폰 에뮬레이터) | Fake | Fake | O | O | X | O |
  | Real | 상관없음 | Real | Real (자리표시) | X | X | O | X |

  - **시연 모드가 기기에 따라 주행 상태 출처를 고르는 이유**: 심사위원이 APK를 어디서 돌릴지 모른다 (문의 A2). Pleos 에뮬레이터에서는 Virtual Console로 기어를 바꾸면 화면이 바뀌고, 왼쪽 Driving view와 앱이 맞는다. 일반 폰 에뮬레이터에서는 Real 주행 상태가 항상 UNKNOWN이라 안내를 시작할 수조차 없어서, Fake 시나리오로 흐름을 잇는다.
  - **주행 중에만 남은 시간이 줄어든다** (시연 모드): `FakeGuidanceSource`의 진행 조건(`progressGate`)에 "주행 중인지"를 넘긴다. P에 서 있는 동안에는 남은 시간이 그대로이고, 멈추면 최대 한 단계까지 더 줄고 멈춘다. 실제 내비게이션도 차가 서 있으면 남은 시간이 줄지 않는다. 도메인 인터페이스(`GuidanceSource`)는 바꾸지 않았다.
  - **시연용 표시**: 정차 화면 길안내 카드와 도착 화면에 "시연용 길안내예요. 실제 내비게이션과는 아직 연결되지 않았어요." 한 줄. 주행 화면은 바꾸지 않는다 (규칙 3). 출품 유의사항(제출 서류가 사실과 다르면 선정 취소) 때문에 숨기지 않는다.
- Fake 주행 상태에서는 Pleos `Vehicle`을 만들지 않는다. 일반 에뮬레이터에는 차량 서비스가 없기 때문이다.
- **반려견 프로필은 모든 모드에서 Fake("보리")다** (009). 서버(보호자 웹 연결) 구현이 없어서 `SourcePlan`에 칸을 두지 않았다. 화면에 "시연용 샘플 정보"라고 알린다 (`AppContainer.isPetProfileSample`). 서버 구현이 생기면 `SourcePlan`에 칸을 더한다.
- **전화는 모든 모드에서 Android 전화 앱(`AndroidPhoneDialer`)**이다. 시연용 번호는 걸리지 않는 형식이고, `ACTION_DIAL`은 걸지 않는다.
- **진료 카드 세션(`MedicalCardSession`)은 `AppContainer`의 메모리 객체**다. Fake 길안내와 같은 프로세스 수명이라 안내 시작부터 도착까지 남는다. 앱 프로세스가 죽으면 사라진다 (알려진 한계).
- `FakeTripScenario`(주행 상태와 길안내가 모두 Fake일 때)가 Fake 길안내와 Fake 주행 상태를 잇는다: 안내를 시작하면 3초 뒤 주행, 도착하면 5초 뒤 정차. 시연 영상을 찍을 때 debug 패널을 누르지 않아도 되게 하려는 것이다.
  - 두 Fake는 각자 Real과 같은 책임만 진다. Real에서 주행 상태(차량)와 길안내(내비 앱)는 서로 모르는 출처이기 때문이다.

## 주행 상태와 화면

- 주행 상태는 `PARKED` / `DRIVING` / `UNKNOWN` 세 가지다.
- Real 구현은 차량의 주행 규제(AAOS `CarUxRestrictions`)를 따른다 (006, 2026-11-05):

  | 받은 것 | 주행 상태 |
  |---|---|
  | 구독 직후 아직 값 없음, 값을 알 수 없음, 차량 기기가 아님(일반 폰) | `UNKNOWN` |
  | `isRequiresDistractionOptimization` = true | `DRIVING` |
  | false | `PARKED` |

  - 기어·속도로 직접 판단하지 않는다. 그래서 **기어 D에 멈춰 있어도(신호 대기) 주행 화면이다** (Pleos 에뮬레이터에서 이때도 규제가 걸린다. 사용자 결정 2026-11-05). 정차 화면은 사실상 P 기어(주차)에서만 보인다.
- 주행 상태를 모르면(`UNKNOWN`) 안전을 위해 주행 화면을 보여준다.
- DO를 선언했다 (위 "서비스 기능 스택"). 그래서 주행 중에도 시스템이 앱을 가리지 않고, 주행 중 안전(입력·스크롤 숨김)은 앱이 이 규칙대로 책임진다.
- 주행 화면 규칙 (AGENTS.md 규칙 3):
  - 큰 글씨의 상태 한 줄과 큰 버튼 1개 이하만 둔다.
  - 텍스트 입력, 스크롤, 애니메이션은 두지 않는다.
- 정차 화면은 일반 기능과 텍스트 입력을 둘 수 있다.

### 주행 상태 × 길안내 상태 화면 조합 (005, 2026-11-05)

주행 상태(`HomeViewModel`)와 길안내 상태(`GuidanceViewModel`)는 따로 받는다. 어느 화면을 보여줄지는 `ui/home/HomeContent.kt`의 `homeContentOf`가 정한다. 주행 상태 쪽 안전 규칙(UNKNOWN·첫 값·5초 뒤 복귀는 주행 화면)은 `HomeViewModel`이 그대로 맡는다.

| 주행 \ 안내 | 안내 없음 | 안내 중 | 도착 |
|---|---|---|---|
| **정차** | 정차 화면 (아래 2단계 표) | 정차 화면 (아래 2단계 표) | 정차 화면 (아래 2단계 표) |
| **주행·모름** | 주행 화면: "주행 중" | 주행 화면: 큰 글씨 "병원까지 N분" + 병원 이름. 버튼 없음 | 주행 화면: 큰 글씨 "도착했어요" + "정차하면 진료 카드를 볼 수 있어요". 버튼 없음 |

- 남은 시간은 분 단위로 올림하고, 1분보다 작으면 1분으로 보여준다. "0분"은 보이지 않는다.
- **2단계: 정차 안에서 (009, `ui/home/ParkedContent.kt`의 `parkedContentOf`)**. 1단계 규칙(주행·모름이면 주행 화면)은 그대로 두고, 정차일 때만 한 번 더 고른다.

  | 안내 \ 카드 세션 | 없음 | 고르는 중 | 카드 있음 |
  |---|---|---|---|
  | 안내 없음 | **처음 화면**: 보리 요약 + [진료 카드 만들기] | **증상 고르기**: 12개 큰 버튼 + [취소] [카드 만들기] | **카드** "진료 카드 생성 완료" + [홈] [수정] / [병원에 전화] [길 안내 시작] |
  | 안내 중 | **카드** (고른 증상 없음) | 증상 고르기 | **카드** "병원까지 N분" + 병원 이름 + [홈] [수정] / [병원에 전화] [안내 취소] |
  | 도착 | **카드** (고른 증상 없음) | 증상 고르기 | **카드** "도착했어요" + 병원 이름 + [홈] [수정] / [병원에 전화] [닫기] |

  - 길안내는 카드의 [길 안내 시작]에서만 시작한다 (사용자 결정 2026-11-05). 정차 첫 화면의 [병원으로 안내]와 메모 칸은 없앴다.
  - [취소]는 고르기 전으로(카드가 없었으면 처음 화면, 있었으면 그 카드). [수정]은 지금 증상이 선택된 채로 고르기.
  - **[홈]** (2026-11-05 사용자 요청): 카드를 지우지 않고 처음 화면으로 간다 (`MedicalCardSession.atHome`, `parkedContentOf`의 세 번째 인자). 처음 화면에 "만든 진료 카드"(안내 중이면 "병원까지 N분 · 병원", 도착이면 "도착했어요 · 병원") + 주요 증상 + [카드 보기]가 보인다. 안내가 없으면 [새로 만들기](카드를 버리고 빈 선택으로 고르기)도 있다. 고르기를 시작하거나 카드를 비우면 홈 상태가 풀린다.
  - 안내 전 카드 위의 [닫기]는 [홈]과 헷갈려서 뺐다. 카드 버리기는 처음 화면의 [새로 만들기]가 맡는다.
  - 도착 카드 [닫기]는 도착 상태를 지우고(`onDismissArrival`) 카드도 비운다(`onCloseCard`). 이 순서라 중간 프레임이 같은 몸통의 "생성 완료" 카드다.
  - 고르는 중이나 카드를 보는 중에 주행(또는 모름)이 되면 주행 화면이고, 다시 정차하면 고르던 선택·같은 카드다 (Pleos 에뮬레이터에서 확인).
  - 시스템 뒤로: 증상 고르기 = [취소], 카드 = [홈]. 처음 화면에서는 가로채지 않는다.
  - 시연용 표시 (주행 화면에는 넣지 않는다): 샘플 반려견 정보, 병원·번호, 길안내.
- 안내 취소는 정차 중에만 한다 (사용자 결정, 2026-11-05). 주행 중 터치를 줄이고, 확인 팝업은 주행 중 전체 화면 팝업 금지에 걸리기 때문이다. Real에서는 내비 앱에서도 취소할 수 있다.
- 주행 중에 안내를 시작하는 버튼은 아직 두지 않는다. 문의 B1(주행 중 사용 선언)과 B2(STT) 답을 보고 정한다.
- 도착 카드는 [닫기]를 누를 때까지 유지한다. 병원에서 수의사에게 보여 줄 화면이기 때문이다.
- 색: 시스템 라이트·다크를 따른다. 다크 배경은 남색(`Navy950`)이고, 카드 몸통은 테마와 상관없이 흰 바탕(`CardColors`, Static Color와 같은 생각)이다.

## Vehicle SDK 수명주기

- `Vehicle`의 `initialize()`와 `release()`는 `data/vehicle/VehicleConnection`에서만 부른다 (AGENTS.md 아키텍처 규칙).
- REAL 모드에서만 만든다. `MeongCareApplication`이 `ProcessLifecycleOwner`에 붙인다.
  - 앱이 화면에 보이면(onStart): 새 `Vehicle`을 만들고 `initialize()`
  - 화면에서 사라지면(onStop): `release()`하고 버린다
  - 같은 인스턴스를 다시 initialize하지 않는다. 공식 샘플도 그렇게 한다 (docs/pleos/sdk-notes.md "2.3.2 AAR에서 직접 확인한 것").
- `initialize()`가 실패하면(차량 서비스가 없는 기기 등) 로그만 남기고 앱은 계속 돈다.
- 차량 기능 래퍼는 `VehicleConnection.vehicle`을 쓴다. 도메인 API 콜백을 기다리는 중에 release되지 않게 하는 처리는 래퍼를 만들 때 함께 한다.

## Car(AAOS) 연결 수명주기 (006, 2026-11-05)

`Car`(`android.car`) 연결은 `Vehicle`과 다르게 관리한다. "한 곳에서만 관리"는 Pleos `Vehicle`의 `initialize()/release()`에 대한 규칙이다.

- **구독할 때 연결하고, 구독이 끝나면 끊는다.** `RealDrivingStateSource`의 `callbackFlow`가 `UxRestrictionsClient.connect()`를 부르고, `awaitClose`에서 닫는다.
  - 구독자는 `HomeViewModel` 하나이고 `WhileSubscribed(5초)`라서, 연결은 "화면이 보이는 동안 + 5초"만 산다. 다시 보이면 새로 연결해 현재 값을 받는다 (Pleos 에뮬레이터에서 확인).
  - 버린 대안: `VehicleConnection`처럼 프로세스 수명주기에 붙인 연결 객체 하나. 공유 상태와 Flow 다리가 따로 필요해 코드가 늘고, 단일 Activity 앱이라 얻는 것이 없다.
  - 구독자가 둘 이상이 되면 연결도 둘이 된다.
    - **시연 모드(차량 기기)에서 안내 중에는 연결이 둘이다**: 화면(`HomeViewModel`)과 길안내 진행 조건(`FakeGuidanceSource`의 "주행 중인지"). 진행 조건 쪽은 안내를 시작할 때 연결하고, 도착·취소하면 닫는다 (007).
    - Pleos 에뮬레이터에서 두 연결이 같은 순간 같은 값(`DO=true active=0x50` 등)을 받는 것을 확인했다. 도착하면 진행 조건 쪽 연결이 닫혔다.
    - `shareIn`으로 묶지 않았다. replay가 `HomeViewModel`의 안전 규칙(5초 넘게 화면이 없다가 돌아오면 새 값을 받기 전까지 주행 화면)과 엮여서, 그 테스트를 다시 봐야 하기 때문이다. 구독자가 더 늘면 그때 묶는다.
- `connect()`마다 새 `Car`를 만든다: `Car.createCar(context, null, CAR_WAIT_TIMEOUT_DO_NOT_WAIT, listener)`. 초기화는 연결 콜백 안에서 한다 (Google 권장). 차량 서비스가 다시 시작되면 같은 콜백이 다시 온다.
- 상황별 처리 (`CarUxRestrictionsClient`):

  | 상황 | 보내는 값 |
  |---|---|
  | 연결됨 | 리스너를 먼저 등록하고 현재 값을 읽어 `Changed(DO)` |
  | 규제 값이 바뀜 | `Changed(DO)` |
  | `createCar` 실패, 매니저 없음, 값 없음, `SecurityException`, 차량 서비스 끊김 | `Unavailable` → 주행 상태 `UNKNOWN` |
  | `close()` | 리스너 해제 → `disconnect()`. 그 뒤 콜백은 무시 |

- **일반 폰에서 죽지 않게**: `uxRestrictionsClientOrNull`이 `FEATURE_AUTOMOTIVE`를 먼저 확인하고, 없으면 `android.car` 클래스를 아예 건드리지 않는다 (일반 폰 에뮬레이터에서 확인).
- 권한을 선언하지 않는다. `pleos.car.permission.*`도 `android.car.permission.*`도 필요 없었다 (Pleos 에뮬레이터에서 `SecurityException` 없음).
- Gradle: `useLibrary("android.car")` (컴파일용). 매니페스트 `<uses-library>`는 필요 없었다 (Pleos 에뮬레이터에서 `NoClassDefFoundError` 없음).

## 보호자 웹 (008, 2026-11-05)

보호자가 폰 브라우저에서 반려견 1마리의 프로필(사진, 이름, 품종, 생년월일 또는 나이, 성별)과 의료정보(주요 질환, 알레르기, 복용 약)를 등록하고 보고 고친다. 차 앱의 진료 카드가 이 정보를 쓸 예정이다.

- 실행·확인 방법: [web/README.md](../web/README.md)
- 프로필 형식 (웹·차 앱·서버 공통): [docs/data/pet-profile.md](data/pet-profile.md)

### 결정

| 항목 | 결정 | 근거 | 버린 대안 |
|---|---|---|---|
| 위치 | 같은 저장소 `web/` (사용자 결정) | 심사위원이 저장소 하나에서 앱과 웹을 본다. 프로필 형식을 문서 하나로 맞춘다 | 별도 저장소 |
| 기술 | React + Vite + TypeScript, 테스트는 Vitest (`environment: 'node'`) | 웹팀이 React를 안다. 의존성은 이 6개뿐이고 버전은 정확히 고정한다 | ESLint (사용자 결정으로 넣지 않음. TypeScript strict가 대신), 라우터·상태 관리·폼·스키마 라이브러리, jsdom |
| 저장 | 브라우저 localStorage, 키 `owner-web.pet-profile` (사용자 결정) | 서버 규정(A1) 답을 기다리지 않고 만든다. 키는 앱 이름과 묶지 않아 이름이 바뀌어도 데이터가 남는다 | 처음부터 Firebase |
| 저장소 인터페이스 | `PetProfileRepository` (`load`/`save`, `Promise`, 실패는 결과 값) | localStorage 구현이 앱의 Fake 자리, Firebase 구현이 Real 자리다. 바꿔 끼워도 화면 코드가 그대로다 | 화면에서 localStorage 직접 호출 |
| 스키마 버전 | `schemaVersion: 1`. 모르는 버전은 읽지 않는다 | localStorage 데이터는 코드보다 오래 남는다. 차 앱이 짐작해서 약 정보를 틀리게 보여 주면 안 된다 | |
| 사진 | 브라우저에서 긴 변 512px JPEG(품질 0.8)로 줄여 data URL로 저장 | 한 키에 넣어도 용량이 작다. 캔버스로 다시 그려 EXIF(촬영 위치)가 빠진다 | 원본 저장 |
| 화면 | 탭 의료정보·기본정보 (의료정보가 먼저). 진료이력 탭·알림 아이콘은 두지 않는다 (사용자 결정) | 기능이 없는 버튼을 보이지 않는다 | 진료이력 탭을 "준비 중"으로 보이기 |
| 화면 확인 | `web/scripts/verify_screens.py` (Python Playwright, Chromium, 날짜 2026-11-05 고정) | `/verify`는 Android용이다. 폰 크기 두 폭으로 시나리오를 돌리고, 콘솔 에러와 밖으로 가는 요청이 있으면 실패다 | React 컴포넌트 단위 테스트 (jsdom 의존성) |
| 하네스 | 검증 파이프라인·pre-push·CI(`web.yml`)만 웹을 검사한다. 커밋 직전·턴 종료는 Android만 (사용자 결정 4b) | 팀이 같이 쓰는 dev를 지키면서 하네스 변경을 줄인다 | 커밋 직전·턴 종료까지 검사 |

### 폴더와 의존 방향

```
web/src/
  main.tsx, config.ts (APP_NAME), container.ts (저장소 구현 고르기)
  domain/   PetProfile 타입, 날짜·나이 계산, JSON 읽기·쓰기, 저장소 인터페이스 (React·브라우저 모름)
  data/     localStorage 구현
  photo/    사진 크기 계산(순수), 캔버스로 줄이기(브라우저)
  ui/       화면. 순수 로직(*Values.ts, profileScreenReducer.ts, profileText.ts)과 컴포넌트(*.tsx)
```

- `ui`는 `domain`·`photo`만 import한다. `data`는 `main.tsx`·`container.ts`만 안다 (앱의 `ui → domain ← data`와 같다).
- 웹의 앱 이름은 `web/src/config.ts` 한 곳에 있다 (지금 MeongCare). 바꿀 때는 이 파일과 Android `strings.xml`을 같이 바꾼다.

### 저장과 개인정보

- 지금 의료정보와 사진은 **보호자 브라우저의 localStorage에 평문으로** 있다.
  - 같은 출처의 스크립트는 모두 읽을 수 있다. 그래서 외부 스크립트(분석 도구, CDN, 웹폰트)를 넣지 않고 `dangerouslySetInnerHTML`을 쓰지 않는다. 화면 확인 스크립트가 밖으로 가는 요청 0건을 확인한다.
  - 브라우저가 지울 수 있다 (방문 기록 삭제, 시크릿 모드). 서버가 생기기 전까지 백업이 없다.
  - 같은 폰·브라우저를 쓰는 다른 사람이 볼 수 있다.
  - 브라우저 안에서 암호화해도 키가 같은 브라우저에 있어 실익이 적다. 서버로 옮길 때 접근 규칙으로 지킨다.
  - Pleos 보안 가이드의 "기기에 저장하는 민감정보는 암호화"는 차 앱 대상이다 ([ux-guidelines.md](pleos/ux-guidelines.md)). 웹은 Pleos 앱이 아니지만, 심사·PT에서 설명할 때 이 차이를 알고 있어야 한다.
- 의료정보를 로그에 남기지 않는다 (`console.log` 0건).
- 반려동물 의료정보의 법적 분류는 확인하지 않았다. 민감정보처럼 다룬다. 보호자 이름·연락처 같은 사람 정보는 받지 않는다.
- 화면 아래에 "이 정보는 지금 쓰는 브라우저에만 저장돼요. 차 앱과의 연결은 준비 중이에요."를 보인다 (출품 유의사항: 제출 내용이 사실과 다르면 선정 취소).

### Firebase로 옮길 때 할 일 (문의 A1 이후)

- Firebase Auth로 보호자를 구분하고, Firestore 보안 규칙으로 자기 문서만 읽고 쓰게 한다. 규칙 없는 테스트 모드로 배포하지 않는다.
- `PetProfileRepository`의 Firebase 구현을 `web/src/data/`에 더하고 `container.ts`에서 고른다. 실패 이유에 `'network'`를 더한다.
- 수집 항목·목적·보유 기간을 알리고 동의 버튼을 둔다. 개인정보처리방침 URL을 만든다 (Pleos 앱 제출 폼 필수 항목).
- 브라우저에 있던 데이터는 보호자가 동의한 뒤 한 번 올리고, 올린 뒤 브라우저에서 지운다.
- 사진은 Firestore 문서가 아니라 Storage에 두고 `photoUrl`로 가리킨다 (형식 v2).
- 보호자가 자기 데이터를 지울 수 있게 한다.
- 차 앱이 웹 페이지를 연결하면 도메인 등록 증빙이 필요하다 ([ux-guidelines.md](pleos/ux-guidelines.md) "외부 서버·네트워크").
- 비밀값: Firebase 웹 설정은 `web/.env.local`(gitignore)의 `VITE_FIREBASE_*`로 받는다. 값이 없으면 localStorage 구현을 쓰고 빌드는 그대로 된다. 그때 `web/.env.example`을 같이 만든다.
  - `VITE_`로 시작하는 값은 번들에 들어가 누구나 볼 수 있다. **Claude API 키, 카카오 키는 웹에 절대 넣지 않는다** (서버에만).

## 테스트 전략

- 단위 테스트(`app/src/test/`)로 Fake 데이터 소스와 ViewModel을 검증한다.
- 화면은 `/verify` 스킬로 에뮬레이터 스크린샷을 찍어 확인한다.
- Real 구현은 Pleos 에뮬레이터(GPU PC)에서 확인한다.
  - `android.car`를 부르는 어댑터는 JVM으로 시험하지 않는다. 판단 로직(`RealDrivingStateSource`)만 가짜 client로 시험한다.
  - 주행 상태는 Virtual Console HTTP 요청으로 바꾼다 (docs/pleos/emulator-setup.md "Pleos 에뮬레이터에서 직접 해 본 것").
