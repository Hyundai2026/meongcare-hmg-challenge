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
- 운전 중: 음성으로 증상을 말하면 병원 연결과 길안내를 시작한다 (목표).
  - **지금(010)은 정차 중 증상 고르기 화면에서만 음성으로 증상을 말한다.** 주행 중 음성 입력은 문의 B1의 답을 보고 정하려 했지만, 2026-11-12 공지로 답이 오지 않는다.
  - **2026-11-12 사용자 결정: 길안내 중(병원 가는 길) 주행 화면에서 증상을 말로 더한다 (계획 018).** 큰 버튼 하나로 듣기를 시작하고, 더한 증상은 병원에 보낸 카드에 반영하고 소리(TTS)로 확인한다. "운전 중 처음부터 음성으로 병원 찾기"는 고르지 않았다.
  - 소리 안내(TTS)를 먼저 만든다 (계획 017). 연결 유지·웹 연결 관리(계획 019)는 그다음, PT 전.
- 정차 후: 진료 카드를 확인하고 공유한다.

근거가 된 Pleos 사실은 [docs/pleos/sdk-notes.md](pleos/sdk-notes.md)와 [ux-guidelines.md](pleos/ux-guidelines.md)에 있다.

| 영역 | 결정 | 근거 | 상태 |
|---|---|---|---|
| 의료 프로필 저장 | **차 기기에 저장하지 않는다** (014). 연결된 보호자의 프로필을 서버에서 읽어 메모리에만 둔다. 기기에 남길 것(연결 자격, 나중에 마지막 프로필)은 Room·SQLCipher 대신 **Android Keystore로 직접 암호화한 앱 전용 파일**로 한다 (사용자 결정 U5, 2026-11-06) | Pleos 보안 가이드: 기기에 저장하는 민감정보는 암호화해야 한다. 문서 하나·자격 하나에 DB는 과하다 | 결정. 11/15 범위는 저장 없음(앱을 다시 켜면 다시 연결). 연결 자격 암호화 저장은 PT 전 (계획 019, 웹의 연결된 차 관리·내 정보 지우기와 같이. 015는 근처 동물병원 검색이 썼다) |
| 음성 → 글자 | **Pleos SpeechToText `ON_DEVICE`** (`ko-KR`) + Fake | 네트워크와 앱 등록이 필요 없다. 기술 활용성 점수에 유리하다 | **결정 (2026-11-05 사용자 결정)**: Pleos 에뮬레이터에서 키 없이 한국어 인식을 확인했다 ([sdk-notes.md](pleos/sdk-notes.md) "STT `ON_DEVICE` 실행 확인"). 콘솔은 아직 "준비 중"이라 **음성은 추가 입력 수단**으로 두고, 증상 버튼 흐름은 그대로 둔다. **2026-11-12 대회 공지로 공모전에서는 써도 된다** (B2 해결). 버튼이 기본이고 음성은 추가라는 구조는 그대로다. **구현함 (010)**: 정차 중 증상 고르기 화면에서 한 문장 → 키워드 규칙(AI 없음) → 증상 선택에 더함. 주행 중 음성은 B1 뒤. `RECORD_AUDIO`는 선언하지 않았다 (Pleos 에뮬레이터에서는 음성 서비스가 대신 녹음한다) |
| 음성 안내 | Pleos TextToSpeech `ON_DEVICE` + Fake | STT와 같은 이유 | **구현함 (017, 2026-11-12 사용자 결정).** 2026-11-12 대회 공지로 공모전에서는 써도 된다. 권한 `TTS_SERVICE`는 팀 승인됨 (보호 수준 normal). 말하는 사건: 출발 / 늦게 보냄 / 못 보냄 / 병원 확인 / 도착을 안내 한 번에 한 번씩 (`domain/trip/TripAnnouncer`), 문장에는 병원 이름만. 재생은 usage `USAGE_ASSISTANCE_NAVIGATION_GUIDANCE`, 앱이 `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK`로 포커스를 잡는다 (SDK·음성 서비스는 잡지 않는다, spike S8). 우리 앱이 내비에 가려져 있어도 소리가 난다 (S11). 한계: 부팅 뒤 첫 합성은 약 6초 늦다, Pleos SDK가 읽는 문장을 기기 로그에 남긴다. 018(주행 중 증상 추가)의 확인 소리도 이것을 쓴다. 아래 "TextToSpeech 수명주기" |
| 증상 → 진료 카드 | Claude API, 구조화 출력(JSON 스키마). 진단하지 않고 말한 내용만 정리한다. 카드 생성이 길안내·병원 연결을 막지 않게 뒤에서 따로 돈다 | 정해진 형식으로 증상·복용약·병력만 뽑는다 | **임시: `claude-opus-5-5` (effort `low`). 모델과 AI 범위는 팀 논의 필요.** **지금(009)은 AI 없이 규칙으로 조합한다**: 등록된 프로필 + 큰 버튼으로 고른 증상(12개 프리셋) → `medicalCardOf`. Claude API는 A1 뒤 |
| 서버 | **Firebase** | AI 키와 장소 검색 키를 앱에 넣지 않는다 (Pleos 보안 가이드: 키 하드코딩 금지, HTTPS·TLS 1.2+). 팀의 웹 경험과 가깝다 | **구현함 (012): Firestore + 익명 Auth, Spark(무료) 요금제, Functions 없음.** 익명 토큰은 만료 5분 전에 같은 uid로 미리 갱신한다 (013). 개발·시연은 PC의 Firebase 에뮬레이터 (`firebase/`). 진짜 프로젝트("MeongCare", Spark, Firestore 서울) 연결은 아직이다. Functions(카카오·Claude 키 숨기기)는 Blaze가 필요해 배포하지 않는다. **015: 근처 동물병원 함수 `nearbyVets`(카카오 키를 이 함수만 안다)를 PC의 Functions 에뮬레이터에서만 돌린다** (`firebase/functions/`, `npm --prefix firebase run emulators:nearby`). 배포할 때 할 일: Blaze, 비밀 관리자, App Check·속도 제한 다시 정하기 |
| 병원 목록 | **제휴 병원** (병원 웹으로 대기 시간을 알리는 병원, Firestore `hospitals`). 시연 병원 5곳 | 응급에서 "현실적으로 가장 빨리 진료받을 곳"을 알려면 병원이 알린 대기 시간이 필요하다 (사용자 결정 2026-11-06) | **구현함 (012)**. **015: 카카오 로컬 검색 병원(반경 5km, 최대 15곳)을 더했다.** 시연용 예시 대기(5~60분, "(예시)" 표시)를 붙여 제휴 병원과 한 순위에 섞고 5개씩 쪽으로 보인다. 진료 카드는 제휴 병원에만. 결과는 저장하지 않는다 (카카오 운영정책). debug + PC 에뮬레이터에서만 (release는 제휴 병원만). 형식: [docs/data/nearby-vets.md](data/nearby-vets.md) |
| 병원 순위 | 이동 어림 + 대기 분이 짧은 순 (`domain/hospital/HospitalRanking.kt`) | 가까운 곳이 가장 빠른 곳은 아니다 | **구현함 (012)**. 대기 정보가 30분보다 오래됐거나 없으면 순위 뒤, 응급 접수 안 하면 따로. 위치를 모르면 대기 시간 순 |
| 이동 시간 | 직선 거리 어림 (하버사인 × 1.25 ÷ 21km/h, 화면에 "약") | 후보마다 길안내 없이 실제 경로 시간을 받는 Pleos API가 확인되지 않았다 | **구현함 (012)**. 계수는 에뮬레이터 내비 관찰 1건에 맞춘 시연용 가정이다. 출발한 뒤 병원에 보내는 남은 시간은 길안내 값이다 |
| 진료 카드 전송 | [출발]하면 자동으로 카드 + 도착 예정을 고른 병원에 보낸다. 남은 시간 갱신은 실제 길안내 1분, **시연 길안내 4초**에 한 번 이하 (`di/TripTiming.kt`, 013). 도착·취소 알림. **병원이 [카드 확인]을 누르면 차에 "병원이 진료 카드를 확인했어요"** (안내 중에만 자기 건을 시연 4초·실제 15초마다 읽는다, 013 U1) (`domain/trip/HospitalTrip.kt`) | 운전자가 따로 조작하지 않는다 (사용자 결정 2026-11-06). 확인 화면에 보내는 것·보내지 않는 것과 [카드 없이 길안내만]을 둔다 (U2) | **구현함 (012·013)**. 사진과 차 위치는 보내지 않는다 (U3). 내장 시연 병원은 확인을 흉내 내지 않는다. 형식: [docs/data/emergency-case.md](data/emergency-case.md) |
| 길안내 | Pleos NaviHelper `requestRoute(RouteInfo)` (`NaviHelper:3.0.5`) | 콘솔 "사용 가능" SDK | **구현함 (010)**: `data/guidance/navi/PleosNaviClient`. 좌표 + 이름만으로 경로 요청이 된다 (`poiId` 빈 값, Pleos 에뮬레이터 확인). 차량 기기에서만 쓴다 (아래 "데이터 모드") |
| 도착 예정 시간 | NaviHelper `onRouteStarted`의 `duration`, `onDrivingInfo`의 `destination.duration` | 에뮬레이터에서 값이 내비 화면과 같았다 | **단위는 초** (2100 = 35분, `[obs-navi-emu]`). `onTripInfo`는 오지 않았다 |
| 현재 위치 | NaviHelper `getCurrentLocationInfo()` | FusedLocation이 "준비 중"이라 대신 쓴다 (2026-11-12 대회 공지로 공모전에서는 FusedLocation도 써도 된다. 바꿀지는 정하지 않았다) | **구현함 (011)**. **016: 내비가 없는 기기(일반 폰·태블릿)의 시연·Real 모드는 고정 위치 서울 서초구 서운로 62**(37.4902566, 127.0283099, 화면 이름 "서초동", `di/LocationPlan.kt`의 `fixedLocationOf`). 015에서 넣은 기기 GPS·위치 권한은 뺐다 (일반 에뮬레이터는 GPS가 없어 기본 위치가 미국이라, 누가 어디서 돌리느냐에 따라 결과가 달랐다): `data/location/NaviCurrentLocationSource` + `PleosNaviClient.currentLocation`. 물을 때마다 연결하고 답을 받으면 release한다. 병원 목록을 열 때 한 번 묻는다 (012) + debug 패널. Pleos 에뮬레이터에서 64~263ms에 왔다. **내비 화면을 한 번 띄우기 전에는 답이 오지 않는다** (`[obs-navi-emu]`) |
| 도착 감지 | NaviHelper `onDestinationArrived` | 도착하면 카드 화면으로 넘어간다 | Real 모드에서 쓴다. **에뮬레이터에서는 내비 속 차가 움직이지 않아 확인하지 못했다** (JVM 테스트로만) |
| 병원 전화 | 안드로이드 다이얼러(`ACTION_DIAL`, 권한 없음, 걸지 않음). 안 되면 번호를 크게 보여준다 | | 구현함 (009, `data/phone/AndroidPhoneDialer`). Pleos 에뮬레이터에서 "전화" 앱이 열렸지만 번호는 보이지 않았다 ([sdk-notes.md](pleos/sdk-notes.md) "전화" 관찰). 실차는 미확인 (문의 B4). 시연용 번호 `02-0000-0000` |
| 카드 공유 | MVP: 정차 화면에 크게 보여주기. 나중에 QR·링크 | 서버 작업이 가장 적다 | 결정 |
| 운전 중 사용 | DO(`distractionOptimized`) 선언. `MainActivity`에 Google 방식 meta-data를 넣었다 (006, 2026-11-05) | 선언이 없으면 주행 중에 시스템 화면이 앱을 가려 주행 화면(남은 시간)이 보이지 않는다 | 결정. Pleos 에뮬레이터에서 동작 확인. 실차·심사 허용은 문의 B1. 안 되면 그 커밋을 revert한다 |
| 주행/정차 판단 | AAOS `CarUxRestrictionsManager`의 `isRequiresDistractionOptimization` (006) | Pleos·Google 모두 기어·속도가 아니라 규제 값에 반응하라고 한다. 권한이 필요 없다 | 결정. Pleos 에뮬레이터에서 동작 확인 |
| 보호자 등록 웹 | React + Vite + TypeScript (`web/`). 동의 전에는 브라우저 localStorage, **서버 저장에 동의하면 Firestore** (014). 차와는 **연결 코드**로 잇는다: 차가 정차 중 6자 코드를 보이고 보호자가 폰 웹에 입력한다 (TV 로그인 방식) | 보호자가 집에서 폰으로 의료정보를 미리 등록해서, 차에서는 글자를 입력하지 않게 한다 (AGENTS.md 규칙 3) | **구현함 (008·014)**. 형식: [docs/data/owner-link.md](data/owner-link.md). 아래 "보호자 웹" |

### 사용하기로 한 Pleos 권한 (AGENTS.md 규칙 2, 2026-11-03 팀 승인)

- `pleos.car.permission.NAVI_ROUTE_SEARCH`: 병원까지 경로 요청
- `pleos.car.permission.NAVI_ROUTE`: 현재 위치, 도착 예정 시간, 경로 정보 조회
- `pleos.car.permission.STT_SERVICE`: 음성 인식
- `pleos.car.permission.TTS_SERVICE`: 음성 안내

이 4개 밖의 `pleos.car.permission.*`은 다시 승인을 받는다. 앱 마켓 심사 전에는 Pleos 승인 범위인지 확인한다 (아래 문의).

### Pleos(주최측)에 문의할 것

문의처: appdevchallenge@hyundai-partners.com
대회 공지·FAQ·가이드를 2026-11-03에 모두 확인한 뒤 정리했다. 공지에서 이미 확인된 것은 [docs/contest/requirements.md](contest/requirements.md)에 있다.

우선순위: **A1 → B1 → B2** 순으로 급하다. 셋 다 답에 따라 기획서 내용이 바뀐다. A1은 답을 받았다 (2026-11-06, 허용).

**A. 대회 규정**

| # | 질문 | 공지에서 확인한 것 | "안 된다"면 |
|---|---|---|---|
| A1 | Pleos 외 외부 API(Claude API, 카카오 로컬 API)와 자체 서버(Firebase)를 써도 되는가? | FAQ "쓸 수 있는 SDK와 API"에는 Pleos SDK 5개만 있다. 외부 API 언급은 없다. **답: 써도 된다** (주최측 답을 사용자가 2026-11-06에 전달. 답장 원문은 저장소에 없다) | (해당 없음. 허용됨) |
| A2 | 2차 PT 현장에 인터넷(Wi-Fi)이 있는가? 노트북을 가져가 화면을 연결하는가? 발표 시간은? 주최측은 제출 APK를 어떤 환경(Pleos 에뮬레이터 버전, 실차 헤드유닛)에서 구동 확인하는가? | 에뮬레이터 구동 화면으로 시연해도 된다. 제출물은 "정상적으로 구동·시연할 수 있는지 확인"한다. 장비·네트워크·시간·확인 환경은 언급 없음 | 대비함 (007): release APK는 시연 모드라 네트워크 없이 돌고, Pleos 에뮬레이터·일반 폰 에뮬레이터 어디서든 흐름이 끝까지 돈다 |
| A3 | AI 코딩 도구로 개발해도 되는가? | "순수 창작물이어야" 한다는 조항만 있다 (표절·기수상작 금지 취지) | 규정에 따른다 |
| A4 | 오픈소스 라이브러리는 라이선스를 지키면 써도 되는가? | 제3자 권리 침해 금지 조항만 있다 | 라이선스 고지를 추가한다 |
| A5 | 심사용 GitHub 계정 ID는 언제 공지되는가? | "공지된 심사용 GitHub 계정"이라고만 되어 있고 아직 공지 없음 | |

**B. Pleos 기술**

| # | 질문 | 문서에서 확인한 것 | "안 된다"면 |
|---|---|---|---|
| B1 | 주행 중에도 쓰는 앱(DO)은 Pleos에서 어떻게 선언하는가? Maps/Navigation이 아닌 앱도 주행 중 음성 입력이 허용되는가? | DO 미선언 앱은 주행 중 사용 불가. Pleos 문서에 선언 방법이 없다. 대회 개발 가이드는 "운전 중 음성을 받아 메모로 남기는" 앱을 예시로 든다. **2026-11-12 공지: 개별 기능 구현·API 동작 문의에는 답하지 않는다 → 답을 기대하지 않는다.** 같은 공지: "이번 공모전의 심사는 Pleos App Market 입점 심사가 아닙니다" | "운전 중 음성 SOS"를 출발 전·정차 중 흐름으로 바꾼다 |
| B2 | 대회 FAQ는 SpeechToText·TextToSpeech·FusedLocation을 "쓸 수 있는 SDK"로 드는데, 콘솔에서는 "준비 중"(신청 불가)이다. **언제 "신청 필요"로 바뀌는가? 2차 PT(12/03) 전에 열리는가?** 열리기 전에 `ON_DEVICE` 모드로 개발·시연해도 되는가? 그 대신 안드로이드 표준 `SpeechRecognizer`를 써도 되는가? | 콘솔 "준비 중" = 신청 불가 (2026-11-03 콘솔 직접 확인). 문서상 `ON_DEVICE`는 앱 등록 불필요지만 사용 허가는 아니다. **운영사무국 답 (2026-11-05, 사용자 전달): 따로 신청받는 절차는 없고, 제공 상태는 Playground 문서를 보라.** `ON_DEVICE`를 써도 되는지는 직접 답하지 않았다. **2026-11-12: 대회 공지가 공모전 범위에서 답했다** — "'준비 중'으로 표시되어 있더라도 개발 문서에 안내된 방법으로 에뮬레이터에서 개발·시험할 수 있습니다" ([contest/requirements.md](contest/requirements.md) "개발 범위"). 공모전에서는 STT·TTS·FusedLocation을 써도 된다. App Market 입점 심사는 이 공지의 범위 밖이다 | 기본 흐름은 큰 버튼(증상 프리셋)이다. 음성(`ON_DEVICE`)은 추가 입력으로 붙인다 (2026-11-05 결정). 안 된다고 하면 음성만 뺀다 |
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
    LocationPlan.kt           모드 × 차량 기기 → 현재 위치 출처 FAKE / REAL(내비) (순수 함수 currentLocationKindOf, 011) + 내비 아닐 때 고정 위치 (fixedLocationOf, 016)
    HospitalPlan.kt           모드 × 서버 설정 → 병원 목록 출처 BUILT_IN / SERVER_WITH_FALLBACK / SERVER_ONLY (순수 함수 hospitalSourceKindOf, 012)
    NearbyVetPlan.kt          모드 × 함수 주소 → 근처 동물병원 출처 OFF / FAKE / FUNCTION (순수 함수 nearbyVetKindOf, 015)
    DataModeStore.kt          debug에서 고른 모드를 저장 (release는 항상 DEMO)
    AppRestarter.kt           모드를 저장하고 앱을 다시 시작 (debug)
    VoicePlan.kt              모드 × 차량 기기 → 음성 입력 종류(NONE/FAKE/REAL). 화면에서 빼는 스위치 VOICE_INPUT_ENABLED
    SpeechPlan.kt             (017) 모드 × 차량 기기 → 소리 안내 종류(NONE/FAKE/REAL). 끄는 스위치 SPEECH_OUTPUT_ENABLED
  domain/driving/
    DrivingState.kt           PARKED / DRIVING / UNKNOWN
    DrivingStateSource.kt     주행 상태를 Flow로 주는 인터페이스
  domain/guidance/
    Destination.kt            길안내 목적지 (이름, 좌표, 주소?)
    GuidanceState.kt          Idle / Guiding(목적지, 남은 시간: Duration?) / Arrived(목적지)
    GuidanceSource.kt         길안내 인터페이스 (start / cancel / dismissArrival)
  domain/location/
    CurrentLocation.kt        현재 위치 (위도, 경도, 주소?)
    CurrentLocationSource.kt  현재 위치를 한 번 묻는 인터페이스 (suspend current(): CurrentLocation?, null = 모름)
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
    NaviRouteOnlyGuidanceSource.kt  경로만 차량 내비 (시연 + 차량 기기): 내비 예상 시간에서 시연용 Fake를 시작. 시연 도착·[닫기]는 내비 연결만 놓고 경로는 남긴다 (013 U2)
    navi/NaviClient.kt        내비 경계 (trip(destination[, onClose]): Flow<NaviEvent>, RouteOnClose = CANCEL/KEEP). SDK 타입 없음
    navi/NaviLocationClient.kt  내비 현재 위치 경계 (currentLocation(): Flow<NaviLocationEvent>). SDK 타입 없음 (011)
    navi/PleosNaviClient.kt   NaviHelper 어댑터. NaviHelper를 부르는 유일한 파일. 안내 한 번, 현재 위치 한 번마다 연결하고 끝나면 release
    navi/NaviTiming.kt        내비 남은 초 → 남은 시간 규칙, 안내 시작 대기 30초
  data/location/
    FakeCurrentLocationSource.kt  고정 위치를 바로 준다: DEMO_LOCATION(시흥동, Pleos 에뮬레이터 내비와 같음), PHONE_LOCATION(서초동 서운로 62, 016)
    NaviCurrentLocationSource.kt  내비 위치 해석: 시간 초과 5초, 값 검사(currentLocationOf), 한 번에 하나, 좌표 없는 로그 (JVM 테스트)
  domain/pet/                 PetProfile(형식 문서 대응), PetAge(나이 계산, 웹과 같은 규칙), PetProfileSource + PetProfileLoad
  domain/link/                (014) LinkCode(연결 코드 6자·10분, 남은 분), OwnerLinkService(연결 상태·코드 만들기·확인·끊기 계약)
  domain/card/                Symptom(12개 프리셋, 선언 순서 = 화면·카드 순서), MedicalCardSession(고르는 중/카드 있음, 메모리. 음성 결과는 select로 더하기만)
  domain/voice/
    VoiceInput.kt             한 문장 듣기 인터페이스 (listen(): Flow<VoiceEvent>), VoiceEvent, VoiceFailure
    SymptomMatcher.kt         받아쓴 문장 → 증상 프리셋 (키워드 규칙, AI 없음)
  domain/hospital/            Hospital(길안내 목적지 + 전화번호 + 시연용 여부 + 제휴 병원 id), HospitalStatus·HospitalCandidate(출처),
                              HospitalBoardSource(목록 인터페이스), TravelEstimate(직선 거리 어림), HospitalRanking(순위 규칙) (012)
                              015: NearbyVetSource·NearbyVets, nearbyVetRowsOf(거리순·5km·제휴와 겹침 제거), exampleStatusOf(예시 대기)
  domain/trip/                CaseCard(보낼 카드, 사진 제외)·CaseSender(전송 + 병원 확인 읽기 AckRead), HospitalTrip(출발·전송·갱신·도착/취소·확인 읽기·진행 시작값, FinderStep) (012·013)
                              (017) TripAnnouncement(소리로 읽을 사건과 문장), TripAnnouncer(HospitalTrip.trip·길안내 상태를 보고 사건을 순서대로 말한다)
  domain/speech/              (017) Speaker(짧은 문장을 읽고 끝 결과를 돌려준다, 한 번에 하나), SpeakResult, SpeakFailure
  domain/phone/               PhoneDialer(전화 앱 열기)
  data/hospital/
    SampleHospitals.kt        010의 고정 시연 병원(demo)과 시연용 제휴 병원 5곳(partners, hospitals.sample.json과 같은 값)
    BuiltInHospitalBoardSource.kt  내장 시연 병원 목록 (알린 시각 = 지금 - N분)
    FallbackHospitalBoardSource.kt 서버 → 5초 안에 못 받거나 실패하면 내장 (시연 모드)
    FakeCaseSender.kt         카드를 보내지 않고 메모리에 남긴 뒤 성공 (내장 병원용)
  data/server/                (012) Firebase REST. 런타임 의존성 없음 (HttpURLConnection + 내장 org.json)
                              015: NearbyFunctionConfig(함수 주소), FunctionNearbyVetSource(callable `nearbyVets` → 카카오)
    ServerConfig.kt           BuildConfig → 서버 주소 (debug 에뮬레이터 / release HTTPS만 / 설정 없으면 null)
    HttpTransport.kt          HTTP 경계 (fun interface). UrlConnectionHttpTransport.kt가 java.net을 쓰는 유일한 파일
    AnonymousAuth.kt          익명 가입 한 번, 토큰 재사용, 만료 5분 전 갱신(같은 uid, 거부되면 다시 가입) (013)
    FirestoreJson.kt          Firestore REST 값 ↔ 도메인 (형식 문서 규칙)
    FirestoreHospitalBoardSource.kt, FirestoreCaseSender.kt  목록 GET / 카드 POST / 갱신·도착·취소는 documents:commit + updateMask / 확인 읽기는 단건 GET + mask (지운 건 403 = Gone)
    FirestoreOwnerLink.kt     (014) 연결 코드 만들기(commit + exists=false + REQUEST_TIME, 409면 새 코드) / 확인(단건 GET) / 끊기(허가 DELETE). 보호자 uid는 여기만 안다
    FirestorePetProfileReader.kt  (014) 연결된 보호자 프로필 단건 GET (403 = 연결 끊김, 404 = 등록 안 됨)
  data/pet/                   SamplePetProfiles(021: 샘플 왕건이·오숑이, 예시 JSON과 다른 값), SamplePetChooser(고른 샘플, domain SamplePetChoice), FakePetProfileSource(테스트용),
                              LinkedPetProfileSource(014: 연결 상태 × 샘플 × 서버 프로필, 연결 뒤 못 읽으면 샘플로 대신하지 않음)
  data/link/                  (014) FakeOwnerLink(debug [가짜 보호자 코드 입력]), NoOwnerLink(서버 설정 없음)
  data/phone/                 AndroidPhoneDialer (ACTION_DIAL. ACTION_DIAL을 부르는 유일한 파일)
  data/demo/
    FakeTripScenario.kt       FAKE 전용: 안내 시작 → 출발(주행), 도착 → 주차(정차)
  data/vehicle/
    VehicleConnection.kt      Pleos Vehicle initialize/release를 한 곳에서
  data/voice/
    FakeVoiceInput.kt         예시 문장(SampleVoiceSentences)을 낱말씩 받아쓰는 Fake
    SpeechToTextClient.kt     Pleos STT 경계 (SttEvent. Pleos 타입 없음)
    RealVoiceInput.kt         STT 이벤트 → VoiceEvent, 시간 제한 (Pleos 없음, JVM 테스트)
    PleosSpeechToTextClient.kt  Pleos SpeechToText ON_DEVICE를 부르는 유일한 파일
  data/speech/                (017)
    FakeSpeaker.kt            소리 없이 글자 수만큼 기다리는 Fake
    TextToSpeechClient.kt     Pleos TTS 경계 (TtsEvent. Pleos 타입 없음)
    RealSpeaker.kt            TTS 이벤트 → SpeakResult, 한 번에 하나(Mutex), 시간 제한, 오디오 포커스 (Pleos 없음, JVM 테스트)
    AudioFocusGate.kt         오디오 포커스 경계. AndroidAudioFocusGate.kt가 AudioManager로 길안내 음성 포커스를 잡는다
    PleosTextToSpeechClient.kt  Pleos TextToSpeech ON_DEVICE를 부르는 유일한 파일
  di/TripTiming.kt            시연·실제 길안내별 도착 예정 갱신·병원 확인 읽기 간격 (013)
  di/PetPlan.kt               (014) 반려견 프로필·보호자 연결 구성 (petPlanOf, petProfileIsSampleOf)
  ui/theme/                   Color(+ TripColors), Type(글자 크기, DrivingText), Dimens(터치 크기), Theme
  ui/home/                    HomeViewModel, HomeUiState, HomeContent(1단계 화면 조합), ParkedContent(2단계: 정차 안), HomeScreen, ParkedScreen(처음 화면), DrivingScreen,
                              LinkRow(014: 처음 화면의 연결 줄)
  ui/link/                    (014) LinkViewModel(코드 만들기, 보이는 동안만 2초마다 확인, 만료·연결·닫기에 멈춤), LinkUiState, LinkScreen(정차 중에만)
  ui/card/                    MedicalCardViewModel, MedicalCardContent(medicalCardOf: 카드 조합), MedicalCardScreen, SymptomPickerScreen, CardTexts, PetVisuals(발바닥·아바타)
  ui/guidance/                GuidanceViewModel, GuidanceUiState, SimulatedGuidanceNotice,
                              TripPanel(tripPanelOf: 안내 영역 상태, JVM 테스트), GuidancePanel(정차 중 안내 영역), StaticProgressBar(움직이지 않는 진행 막대) (013)
  ui/hospital/                (012) HospitalFinderViewModel(위치·목록 동시에, 보이는 동안 30초마다 목록), HospitalListScreen, HospitalConfirmScreen, HospitalTexts
  ui/voice/                   SymptomVoiceViewModel, VoiceUiState, VoicePanel(증상 고르기 위 음성 영역), VoiceActions
  ui/debug/                   DebugPanel (상태 없음. 값과 동작은 MainActivity가 넘긴다), DebugLocationViewModel([위치 묻기] 상태와 문구, debug 빌드에서만 만든다)
app/src/test/java/com/meongcare/app/
  testutil/MainDispatcherRule.kt
  data/driving/, data/guidance/, data/demo/, data/location/, data/pet/, data/voice/, di/, domain/pet/, domain/card/, domain/voice/, ui/home/, ui/guidance/, ui/card/, ui/voice/, ui/debug/   단위 테스트
app/src/test/resources/voice/symptom-sentences.tsv   받아쓴 문장 → 기대 증상 (Kotlin을 몰라도 줄을 더할 수 있다)
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
    | 시연 (release) | 예 | **경로만 차량 내비** + 남은 시간·도착은 시연용 (내비 예상 시간에서 시작, 주행 중에만 줄어듦). 시연 도착·[닫기] 때 **내비 경로는 남긴다** (013 U2: 경로를 취소하면 내비 앱이 앞으로 나와 "도착했어요"를 가린다. 다음 출발이 새 경로로 바꾼다). 앱 [안내 취소]는 경로를 취소하고, 그때는 내비 앱이 앞에 나온다 (앱이 스스로 돌아올 수 없다, U3). 화면 문구 "경로는 차량 내비게이션으로 안내해요. 앱의 남은 시간과 도착은 시연용이에요." |
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
  - **현재 위치 (011)**: `di/LocationPlan.kt`의 `currentLocationKindOf`가 정한다. 007·010의 표와 테스트는 그대로다.

    | 모드 | 차량 기기 | 현재 위치 | debug 패널 표시 |
    |---|---|---|---|
    | 시연 (release), Real | 예 | 내비 (`NaviCurrentLocationSource` + `PleosNaviClient`) | 위치(내비) |
    | 시연, Real (016) | 아니오 (일반 폰·태블릿) | **고정: 서초구 서운로 62** ("서초동", `FakeCurrentLocationSource.PHONE_LOCATION`, `fixedLocationOf`) | 위치(Fake) |
    | Fake | | Fake: Pleos 에뮬레이터와 같은 값 (37.4127212, 127.094681, "수정구 시흥동") | 위치(Fake) |

    - Fake 값을 에뮬레이터 위치로 한 이유: 두 에뮬레이터가 같은 위치를 쓰게 되어, 나중에 병원 검색 결과가 에뮬레이터마다 달라지지 않는다. 사람의 실제 위치가 아니다.
    - NaviHelper 수명: 물을 때마다 새 NaviHelper를 만들고 답을 받으면 release한다. 한 번에 하나만 묻는다. 길안내 쪽 NaviHelper와 동시에 있어도 됐다 (Pleos 에뮬레이터).
    - 위치를 쓰는 곳: 병원 목록을 열 때 한 번 (012, 정차 중), debug 패널 [위치 묻기]. 위치를 모르면(내비 준비 전) 다른 위치로 대신 계산하지 않고 "대기 시간 순"으로 보인다.
    - 로그(`CurrentLocation` 태그)에는 결과 종류와 걸린 시간만 남긴다. 좌표와 주소는 남기지 않는다.
    - 시연 준비: 에뮬레이터를 켠 뒤 내비 화면을 한 번 띄워 둔다. 그 전에는 내비가 위치 요청에 답하지 않는다.
  - **병원 목록과 카드 전송 (012)**: `di/HospitalPlan.kt`의 `hospitalSourceKindOf`가 정한다. 007·010·011의 표와 테스트는 그대로다.

    | 모드 | 서버 설정 (`secrets.properties`의 `FIREBASE_*`) | 병원 목록 | 카드 전송 | 목록 위 안내 |
    |---|---|---|---|---|
    | Fake | 상관없음 | 내장 시연 병원 5곳 | Fake (보내지 않음) | "시연용 병원 정보예요" |
    | 시연 (release) | 있음 | 서버 → 5초 안에 못 받거나 실패하면 내장 | 서버에서 온 병원만 서버, 대체된 내장 병원은 Fake | 서버: "병원 웹에서 알린 대기 시간이에요" / 대체: "서버에 연결하지 못해 시연용 병원 정보를 보여 줘요" |
    | 시연 (release) | 없음 | 내장 | Fake | "시연용 병원 정보예요" |
    | Real | 상관없음 | 서버만. 실패하면 "병원 정보를 받지 못했어요" + [다시 시도] | 서버 | |

    - **release(시연)는 네트워크 없이도 끝까지 돈다** (문의 A2). 11/15 제출 APK에는 Firebase 설정을 넣지 않는 것을 추천한다 (계획 012 U5, 아직 정하지 않음).
    - 서버 주소 (`data/server/ServerConfig.kt`): debug이고 `FIREBASE_EMULATOR_HOST`가 있으면 PC 에뮬레이터(평문 HTTP, debug의 network_security_config가 그 주소만 허용). **release는 에뮬레이터 주소를 무시하고 HTTPS만 쓴다.**
    - BuildConfig의 `FIREBASE_*`는 컴파일 상수가 아니다 (`String.valueOf`). 상수면 secrets.properties만 바꿨을 때 증분 컴파일이 예전 값을 남긴다 (012에서 겪음).
    - 로그는 `HospitalTrip` 태그 하나에 결과 종류·개수·시간·HTTP 상태만 남긴다. 반려견 이름·증상·좌표·병원 이름·토큰은 남기지 않는다.
    - 주행 화면은 바꾸지 않았다 (규칙 3). 병원 목록·확인 화면은 정차 중에만 보이고(`parkedContentOf`의 HOSPITALS·CONFIRM_HOSPITAL), 주행이면 주행 화면이 먼저다.
  - **근처 동물병원 (015)**: `di/NearbyVetPlan.kt`의 `nearbyVetKindOf`가 정한다. 012의 표와 테스트는 그대로다.

    | 모드 | 함수 주소 있음 (debug + `FIREBASE_EMULATOR_HOST`) | 없음 (release 포함) |
    |---|---|---|
    | Fake | 시연용 근처 병원 3곳 (`FakeNearbyVetSource`) | 같음 |
    | 시연, Real | Firebase Function `nearbyVets` → 카카오 (`FunctionNearbyVetSource`) | 끔 (근처 병원 없음) |

    - 근처 병원에는 시연용 예시 대기(`exampleStatusOf`)를 붙여 제휴 병원과 한 순위에 섞는다. 화면은 5개씩 쪽 ([이전]/[다음]).
    - 근처 병원은 진료 카드를 받지 않는다 (`HospitalCandidate.canReceiveCard`). 확인 화면은 [병원에 전화]·[길 안내 시작].
    - 근처 검색은 목록 화면이 보일 때 한 번. 30초 갱신 때는 다시 찾지 않고, 실패했을 때만 [새로 고침]이 다시 찾는다. 8초가 지나면 실패.
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
- **음성 입력은 `di/VoicePlan.kt`의 `voiceInputKindOf(mode, isAutomotive)`가 따로 정한다** (010).
  - `SourcePlan`에 칸을 더하지 않은 이유: `SourcePlanBoundaryTest`가 `SourcePlan`을 위치 인자로 만들고 `equals`로 비교해서, 칸을 더하면 기존 테스트를 고쳐야 한다. 따로 두면 음성만 빼기도 쉽다.

  | 모드 | 차량 기기 (`FEATURE_AUTOMOTIVE`) | 음성 입력 | 시연용 표시 |
  |---|---|---|---|
  | Fake | 상관없음 | Fake (예시 문장 3개를 돌아가며) | O |
  | 시연 | 예 (Pleos 에뮬레이터, 실차) | Real (`ON_DEVICE`, 마이크) | X |
  | 시연 | 아니오 (일반 폰) | Fake | O |
  | Real | 예 | Real | X |
  | Real | 아니오 | 없음 (음성 서비스가 없다) | - |
  | 스위치(`VOICE_INPUT_ENABLED`) 꺼짐 | 모두 | 없음 | - |

  - 시연용 표시: 음성 영역 안 작은 글씨 "시연용 음성 입력이에요. 실제로 듣지 않고 예시 문장을 받아써요." 주행 화면에는 음성 영역이 없다.
  - 음성 입력이 없으면 증상 고르기 화면에 음성 영역이 없고, 버튼 흐름은 그대로다.
- **소리 안내(TTS)는 `di/SpeechPlan.kt`의 `speechOutputKindOf(mode, isAutomotive)`가 따로 정한다** (017). 표는 음성 입력과 같다.

  | 모드 | 차량 기기 | 소리 안내 |
  |---|---|---|
  | Fake | 상관없음 | Fake (소리 없음, 로그만) |
  | 시연 (release) | 예 | Real (`ON_DEVICE`) |
  | 시연 | 아니오 (일반 폰) | Fake |
  | Real | 예 | Real |
  | Real | 아니오 | 없음 |
  | 스위치(`SPEECH_OUTPUT_ENABLED`) 꺼짐 | 모두 | 없음 |

  - 화면은 바뀌지 않는다. 소리가 없는 구성에도 시연용 표시가 없다 (보일 것이 없다).
  - `TripAnnouncer`는 `applicationScope`에서 돈다. 앱이 화면에 없어도 말한다.
- Fake 주행 상태에서는 Pleos `Vehicle`을 만들지 않는다. 일반 에뮬레이터에는 차량 서비스가 없기 때문이다.
- **반려견 프로필과 보호자 연결은 `di/PetPlan.kt`의 `petPlanOf`가 정한다** (014). `SourcePlan`은 바꾸지 않았다.

  | 모드 | 서버 설정 | 연결 | 연결 전 반려견 | 연결 뒤 |
  |---|---|---|---|---|
  | Fake | 상관없음 | Fake (debug [가짜 보호자 코드 입력]) | 고른 샘플 (왕건이·오숑이) | 연결할 때 고른 샘플 |
  | 시연 (release) | 없음 | 없음 (연결 버튼 없음) | 고른 샘플 (왕건이·오숑이) | - |
  | 시연 | 있음 | 서버 | 고른 샘플 (왕건이·오숑이) | 서버 프로필. 못 읽으면 "불러오지 못했어요" (샘플로 대신하지 않는다) |
  | Real | 있음 | 서버 | 등록 안 됨 | 서버 프로필 |
  | Real | 없음 | 없음 | 등록 안 됨 | - |

  - 화면의 "시연용 샘플 정보예요"는 `petProfileIsSample`(StateFlow)을 따른다. 서버 연결로 받은 프로필만 샘플이 아니다.
  - 서버 클라이언트(전송·익명 계정)는 서버 설정이 있고 Fake가 아닐 때 한 번 만들어 병원과 반려견이 같이 쓴다 (같은 uid여야 규칙을 통과한다).
  - 연결된 프로필은 처음 화면이 보이는 동안(앱이 앞에 있을 때) 1분마다, [진료 카드 만들기]·[새로 만들기] 때 다시 읽는다. 주행 화면·백그라운드에서는 읽지 않는다.
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
  - 큰 글씨의 상태 한 줄과 큰 버튼 1개 이하만 둔다. 안내 중에는 큰 남은 분·목적지·움직이지 않는 진행 막대·병원 확인 한 줄까지만 (013 U4). 도착 예정 시각·대기 정보·전송 상태·시연용 표시는 정차 화면에만.
  - 텍스트 입력, 스크롤, 애니메이션은 두지 않는다.
- 정차 화면은 일반 기능과 텍스트 입력을 둘 수 있다.

### 주행 상태 × 길안내 상태 화면 조합 (005, 2026-11-05)

주행 상태(`HomeViewModel`)와 길안내 상태(`GuidanceViewModel`)는 따로 받는다. 어느 화면을 보여줄지는 `ui/home/HomeContent.kt`의 `homeContentOf`가 정한다. 주행 상태 쪽 안전 규칙(UNKNOWN·첫 값·5초 뒤 복귀는 주행 화면)은 `HomeViewModel`이 그대로 맡는다.

| 주행 \ 안내 | 안내 없음 | 안내 중 | 도착 |
|---|---|---|---|
| **정차** | 정차 화면 (아래 2단계 표) | 정차 화면 (아래 2단계 표) | 정차 화면 (아래 2단계 표) |
| **주행·모름** | 주행 화면: "주행 중" | 주행 화면 (013 U4): **아주 큰 남은 분**("15분") + "○○까지" + 움직이지 않는 진행 막대 + 병원이 확인했으면 "병원이 카드를 확인했어요" 한 줄. 버튼 없음 | 주행 화면: 큰 글씨 "도착했어요" + "정차하면 진료 카드를 볼 수 있어요". 버튼 없음 |

- 남은 시간은 분 단위로 올림하고, 1분보다 작으면 1분으로 보여준다. "0분"은 보이지 않는다.
- **2단계: 정차 안에서 (009, `ui/home/ParkedContent.kt`의 `parkedContentOf`)**. 1단계 규칙(주행·모름이면 주행 화면)은 그대로 두고, 정차일 때만 한 번 더 고른다.

  | 안내 \ 카드 세션 | 없음 | 고르는 중 | 카드 있음 |
  |---|---|---|---|
  | 안내 없음 | **처음 화면**: 반려견 요약 + [진료 카드 만들기] | **증상 고르기**: 12개 큰 버튼 + [취소] [카드 만들기] | **카드** "진료 카드 생성 완료" + [홈] [수정] / [병원에 전화] [길 안내 시작] |
  | 안내 중 | **카드** (고른 증상 없음) | 증상 고르기 | **카드** "진료 카드" + [홈] [수정], 머리 아래 **안내 영역** / [병원에 전화] [안내 취소] |
  | 도착 | **카드** (고른 증상 없음) | 증상 고르기 | **카드** "진료 카드" + [홈] [수정], 머리 아래 **도착 안내 영역** / [병원에 전화] [닫기] |

  - **안내 영역** (013, `ui/guidance/GuidancePanel.kt`): 파란 고정색 패널. "○○까지", 큰 남은 분, "도착 예정 14:32"(정차 중 30초마다 다시 계산), "출발 때 알린 대기 10분 · 보통", 진행 막대(시작 = 이번 안내에서 본 가장 큰 남은 분, `TripState.startMinutes`), 흰 줄에 전송·확인 상태("진료 카드를 병원에 보냈어요 · 확인을 기다리는 중" → "병원이 진료 카드를 확인했어요 (14:05)", 실패면 [다시 보내기]). 도착이면 초록 패널 "○○에 도착했어요 · 이 카드를 접수처에 보여 주세요". [병원에 전화] [안내 취소]는 화면 아래 고정 줄에 둔다. 처음 화면([홈])도 같은 패널을 쓴다.

  - 길안내는 카드의 [길 안내 시작]에서만 시작한다 (사용자 결정 2026-11-05). 정차 첫 화면의 [병원으로 안내]와 메모 칸은 없앴다.
  - [취소]는 고르기 전으로(카드가 없었으면 처음 화면, 있었으면 그 카드). [수정]은 지금 증상이 선택된 채로 고르기.
  - **[홈]** (2026-11-05 사용자 요청): 카드를 지우지 않고 처음 화면으로 간다 (`MedicalCardSession.atHome`, `parkedContentOf`의 세 번째 인자). 처음 화면에 (안내 중·도착이면 위의 안내 영역 +) "만든 진료 카드" + 주요 증상 + [카드 보기]가 보인다. 안내가 없으면 [새로 만들기](카드를 버리고 빈 선택으로 고르기)도 있다. 고르기를 시작하거나 카드를 비우면 홈 상태가 풀린다.
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

## SpeechToText 수명주기 (010, 2026-11-05)

- **듣기 한 번 = `SpeechToText` 객체 하나.** `RealVoiceInput.listen()`을 구독하면 `PleosSpeechToTextClient.start()`가 새 객체를 만들어 `initialize` → `addListener` → `setLocale(Locale.KOREA)` → `request()`까지 한다. 끝나거나 구독이 취소되면 `stop()` → (인식 끝을 기다림) → `removeListener()` → `release()`하고 버린다. Pleos 문서: release한 뒤 다시 쓰려면 새 객체를 만든다.
  - **`stop()` 바로 뒤에 `release()`하지 않는다.** SDK의 `stop()`은 서비스에 보내는 일을 코루틴으로 미루고, `release()`는 그 자리에서 서비스 연결을 끊는다. 바로 이어 부르면 `stop`이 서비스에 닿지 않아 음성 서비스가 계속 녹음한다 (2026-11-05 Pleos 에뮬레이터에서 봄. 앱을 닫아도 계속됐고, 다음 `request()`가 와서야 멈췄다). 그래서 `onEndedRecognition`이 오거나 1초가 지나면 놓는다. 에뮬레이터에서는 `stop()` 뒤 `onEndedRecognition`이 오지 않았고, 서비스는 `stop()` 1~7ms 뒤에 녹음을 멈췄다.
  - 이유: `initialize` 성공까지 약 20ms라 매번 만들어도 느리지 않다. 프로세스 수명 객체를 두면 화면 전환·실패 복구 상태가 늘어난다. `Car` 연결(006)과 같은 방식이다.
  - "`Vehicle`의 initialize/release는 `VehicleConnection` 한 곳에서" 규칙은 Vehicle SDK 규칙이다. STT는 이 규칙 밖이고, `RealVoiceInput`이 한 곳에서 관리한다.
- 듣기가 끝나는 때 (모두 객체를 놓는다):
  - 최종 결과(`onUpdated(completed=true)`)나 오류가 옴
  - 시간 제한: 시작 뒤 3초 안에 응답(준비·받아쓰기)이 없으면 "쓸 수 없음", 인식이 끝난 뒤 2초 안에 최종 결과가 없으면 "말소리 없음", 시작 뒤 15초가 지나면 "말소리 없음". 시간은 이벤트가 올 때마다 다시 재지 않는다. 값은 기본안이다 (Pleos 문서에 무음 대기·최대 녹음 시간이 없다).
  - [듣기 취소], 고르는 중이 아니게 됨(카드 만들기·취소), 증상 고르기 화면이 사라짐(주행 화면으로 바뀜), 앱이 화면에서 사라짐(`ON_STOP`) → `SymptomVoiceViewModel`이 구독을 취소한다.
- 오디오 포커스는 요청하지 않는다. 콜백이 어느 스레드로 오는지 가리지 않는다 (`Channel`로 받는다). 둘 다 Pleos 문서에 없다 (sdk-notes "듣는 동안의 오디오 포커스, 콜백 스레드, 끝점 검출").
- 결과는 **더하기만** 한다. 찾은 증상을 `MedicalCardSession.select`로 지금 선택에 더하고, 빼는 것은 버튼으로 한다. 인식한 문장은 음성 영역에만 보이고 카드와 로그에는 남기지 않는다 (반려견 의료정보).

## 음성 입력 빼는 법 (010, 문의 B2 대비)

- 2026-11-12 대회 공지로 B2는 공모전 범위에서 풀렸다 (STT를 써도 된다). 이 절차는 나중에 App Market 입점 심사에서 STT가 허용되지 않을 때를 위해 남겨 둔다.

- **화면에서만 빼기**: `di/VoicePlan.kt`의 `VOICE_INPUT_ENABLED = false` 한 줄. 모든 모드가 "없음"이 되어 음성 영역이 사라진다. SDK와 권한은 APK에 남는다.
- **APK에서 완전히 빼기** (STT를 쓰면 안 된다는 답이 오면): 위 한 줄 + 아래 두 커밋을 새것부터 `git revert` (한 번에: `git revert <2의 해시> <1의 해시>`).
  1. `feat(data): Pleos SpeechToText ON_DEVICE 래퍼 추가`
  2. `fix(data): STT를 멈출 때 음성 서비스가 녹음을 끝낸 뒤에 놓기` (1의 래퍼만 고친 커밋)
  - SDK 의존성(`gradle/libs.versions.toml`, `app/build.gradle.kts`), `STT_SERVICE` 권한(`AndroidManifest.xml`), `PleosSpeechToTextClient.kt`, `AppContainer`의 Real 연결이 함께 빠진다. Real 자리는 "없음"이 된다.
  - 이 파일과 줄은 다른 커밋에서 되도록 건드리지 않는다. 건드리면 그 커밋도 같이 되돌려야 한다.
  - 2026-11-05에 확인했다: 두 커밋을 되돌린 상태에서 `assembleDebug`와 단위 테스트(433개)가 통과했다.
- 주행 화면, 길안내, 진료 카드 조합 코드는 음성 작업(010)에서 바뀌지 않았다. 음성을 빼도 버튼 흐름은 그대로다.

## TextToSpeech 수명주기 (017, 2026-11-12)

- **말하기 한 번 = `TextToSpeech` 객체 하나.** `RealSpeaker.speak()`가 오디오 포커스를 받고 `PleosTextToSpeechClient.speak()`를 부른다. 그러면 새 객체를 만들어 `initialize` → (성공) `addEventListener` → `setLocale(Locale.KOREA)` → `setAudioAttributes(길안내 음성)` → `speak(text, true)`까지 한다. 끝나면 `removeEventListener()` → `release()`하고 포커스를 반납한다.
  - 초기화는 9~16ms라 매번 만들어도 느리지 않다 (spike). 느린 것은 객체가 아니라 음성 엔진의 첫 합성이다: 부팅 뒤 첫 안내만 약 6초 늦다.
  - SDK 호출은 메인 스레드에서 한다. 콜백은 binder 스레드로 온다. `RealSpeaker`는 이벤트를 `Channel`로 받는다.
- **한 번에 하나.** 음성 서비스는 읽는 중에 새 문장을 받으면 앞 문장을 끊고, 끊긴 문장에는 끝 콜백을 보내지 않는다 (spike S6). 그래서 `RealSpeaker`가 `Mutex`로 앞 문장이 끝난 뒤에 다음 문장을 맡긴다.
- 닫기: 읽는 중이면 `stop()` → `onStop`(약 16ms)이 오거나 1초가 지나면 놓는다. `stop()` 바로 뒤 `release()`하지 않는다 (STT 010과 같은 경쟁). 끝난 뒤면 바로 놓는다. `onDone`은 재생이 끝난 뒤에 와서 끝소리가 잘리지 않는다.
- 시간 제한: 시작 뒤 3초 안에 응답(`onReady`·`onStart`)이 없으면 쓸 수 없음. 시작 뒤 10초 + 글자당 0.25초가 지나면 쓸 수 없음. 시간은 이벤트마다 다시 재지 않는다.
- 오디오 포커스: `AndroidAudioFocusGate`가 `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK` + `USAGE_ASSISTANCE_NAVIGATION_GUIDANCE`로 요청한다. 거절되면 그 안내는 읽지 않는다 (`Failed(AUDIO)`). 이 usage 값은 `PleosTextToSpeechClient`와 `AndroidAudioFocusGate` 두 곳에 있다. 바꿀 때는 같이 바꾼다.
- 로그: `PleosTts`(이벤트·오류 이름, 초기화 ms, 글자 수), `SpeechFocus`(받음·거절·반납), `TripAnnouncer`(사건 종류와 결과). 문장과 병원 이름은 남기지 않는다. **다만 Pleos SDK와 음성 서비스가 문장을 기기 로그에 남긴다** (spike S13). 그래서 문장에 반려견 이름·증상을 넣지 않는다.

## 소리 안내 빼는 법 (017)

- **소리만 끄기**: `di/SpeechPlan.kt`의 `SPEECH_OUTPUT_ENABLED = false` 한 줄. 모든 모드가 "없음"이 되어 안내자가 시작되지 않는다. 화면은 원래 바뀐 것이 없다. SDK와 권한은 APK에 남는다.
- **APK에서 완전히 빼기**: 위 한 줄 + 아래 두 커밋을 새것부터 `git revert` (한 번에: `git revert <2의 해시> <1의 해시>`).
  1. `feat(data): Pleos TextToSpeech ON_DEVICE 래퍼 추가` (의존성, `TTS_SERVICE` 권한, `PleosTextToSpeechClient.kt`, `AppContainer`의 Real 연결)
  2. `feat(data): 소리 안내 동안 오디오 포커스 요청` (`AppContainer`의 Real 줄을 다시 고쳤다)
  - Real 자리는 `null`(소리 없음)이 된다. 안내자·Fake·판단 로직은 남는다.
  - 이 파일과 줄은 다른 커밋에서 되도록 건드리지 않는다.

## 보호자 웹 (008, 2026-11-05)

보호자가 폰 브라우저에서 반려견 1마리의 프로필(사진, 이름, 품종, 생년월일 또는 나이, 성별)과 의료정보(주요 질환, 알레르기, 복용 약)를 등록하고 보고 고친다. 서버 저장에 동의하면 차와 연결해 차 앱의 진료 카드가 이 정보를 쓴다 (014).

- 연결 형식·보안 규칙: [docs/data/owner-link.md](data/owner-link.md), `firebase/firestore.rules` (R14~R24)

- 실행·확인 방법: [web/README.md](../web/README.md)
- 프로필 형식 (웹·차 앱·서버 공통): [docs/data/pet-profile.md](data/pet-profile.md)

### 결정

| 항목 | 결정 | 근거 | 버린 대안 |
|---|---|---|---|
| 위치 | 같은 저장소 `web/` (사용자 결정) | 심사위원이 저장소 하나에서 앱과 웹을 본다. 프로필 형식을 문서 하나로 맞춘다 | 별도 저장소 |
| 기술 | React + Vite + TypeScript, 테스트는 Vitest (`environment: 'node'`) | 웹팀이 React를 안다. 의존성은 처음 6개에 012에서 `firebase`(13.1.0) 하나를 더했다 (사용자 결정 U4). 보호자 페이지는 **서버 설정이 있을 때만 Firebase를 동적 import**한다 (014 U8: 설정 없이 빌드한 보호자 번들에는 Firebase가 없고 내려받지도 않는다). 버전은 정확히 고정한다 | ESLint (사용자 결정으로 넣지 않음. TypeScript strict가 대신), 라우터·상태 관리·폼·스키마 라이브러리, jsdom |
| 저장 | 동의 전: 브라우저 localStorage, 키 `owner-web.pet-profile` (008). **동의 뒤: Firestore `petProfiles/{uid}` + 사진은 보호자 전용 `private/photo`** (014 U3·U6). 동의하면 브라우저 정보를 한 번 옮기고 브라우저에서 지운다 (서버에 이미 있으면 덮어쓰지 않는다) | 서버 저장은 동의 뒤에만. 사진은 차가 읽지 못하게 따로 둔다 | 처음부터 Firebase, Firebase Storage(Spark 사용 가능 여부 미확인) |
| 저장소 인터페이스 | `PetProfileRepository` (`load`/`save`, `Promise`, 실패는 결과 값, 서버에 닿지 못하면 `'network'`). 서버 모드는 `OwnerServer`(계정 `OwnerAccount` + 저장소) | localStorage 구현이 앱의 Fake 자리, Firebase 구현(`data/firestoreOwnerStore.ts`)이 Real 자리다. 바꿔 끼워도 화면 코드가 그대로다 | 화면에서 localStorage 직접 호출 |
| 보호자 계정 | Firebase **익명 계정** (014 U1). 동의할 때 만든다 (동의 전에는 만들지 않는다) | 시연이 빠르다. 브라우저 사이트 데이터를 지우면 그 프로필을 고칠 수 없다 → 동의 문구에 알린다 | Google 로그인 (약 하루 더), 이메일 |
| 차 연결 | 프로필 아래 카드 하나: [차와 연결하기] → (동의 전이면 동의) → 바로 코드 입력 → 코드 claim + 허가(`readers/{차}`)를 한 번에 쓴다. 허가 목록을 실시간으로 봐서 카드가 "차와 연결됨"으로 바뀐다 (`watchLinkedCars`). `?code=`로 열면 미리 채운다 (나중 QR) | 차에서 글자를 입력하지 않는다 | QR (11/15에는 넣지 않음, U4) |
| 스키마 버전 | `schemaVersion: 1`. 모르는 버전은 읽지 않는다 | localStorage 데이터는 코드보다 오래 남는다. 차 앱이 짐작해서 약 정보를 틀리게 보여 주면 안 된다 | |
| 사진 | 브라우저에서 긴 변 512px JPEG(품질 0.8)로 줄여 data URL로 저장 | 한 키에 넣어도 용량이 작다. 캔버스로 다시 그려 EXIF(촬영 위치)가 빠진다 | 원본 저장 |
| 화면 | 탭 의료정보·기본정보 (의료정보가 먼저). 진료이력 탭·알림 아이콘은 두지 않는다 (사용자 결정) | 기능이 없는 버튼을 보이지 않는다 | 진료이력 탭을 "준비 중"으로 보이기 |
| 화면 확인 | `web/scripts/verify_screens.py` (Python Playwright, Chromium, 날짜 2026-11-05 고정) | `/verify`는 Android용이다. 폰 크기 두 폭으로 시나리오를 돌리고, 콘솔 에러와 밖으로 가는 요청이 있으면 실패다 | React 컴포넌트 단위 테스트 (jsdom 의존성) |
| 하네스 | 검증 파이프라인·pre-push·CI(`web.yml`)만 웹을 검사한다. 커밋 직전·턴 종료는 Android만 (사용자 결정 4b) | 팀이 같이 쓰는 dev를 지키면서 하네스 변경을 줄인다 | 커밋 직전·턴 종료까지 검사 |

### 폴더와 의존 방향

```
web/src/
  main.tsx, config.ts (APP_NAME), container.ts (저장소 구현 고르기, 설정이 있을 때만 Firestore 모듈을 동적 import)
  domain/   PetProfile 타입, 날짜·나이 계산, JSON 읽기·쓰기, 저장소 인터페이스 (React·브라우저 모름)
  domain/owner/  (014) 연결 코드 입력, 프로필 ↔ 서버 문서(사진 분리), 저장 위치 안내, 동의 문구, 브라우저 정보 옮기기, OwnerAccount 인터페이스
  data/     localStorage 구현, firestoreOwnerStore.ts (014, 'firebase'를 import하는 보호자 쪽 유일한 파일)
  photo/    사진 크기 계산(순수), 캔버스로 줄이기(브라우저)
  ui/       화면. 순수 로직(*Values.ts, profileScreenReducer.ts, profileText.ts)과 컴포넌트(*.tsx)
```

- `ui`는 `domain`·`photo`만 import한다. `data`는 `main.tsx`·`container.ts`만 안다 (앱의 `ui → domain ← data`와 같다).
- 웹의 앱 이름은 `web/src/config.ts` 한 곳에 있다 (지금 MeongCare). 바꿀 때는 이 파일과 Android `strings.xml`을 같이 바꾼다.

### 저장과 개인정보

- 동의 전에는 의료정보와 사진이 **보호자 브라우저의 localStorage에 평문으로** 있다. 동의 뒤에는 Firestore에 있고 보안 규칙(R14~R24)으로 지킨다: 보호자 본인만 쓰고, 연결한 차는 프로필 문서 하나만 읽는다 (사진은 못 읽는다).
  - 같은 출처의 스크립트는 모두 읽을 수 있다. 그래서 외부 스크립트(분석 도구, CDN, 웹폰트)를 넣지 않고 `dangerouslySetInnerHTML`을 쓰지 않는다. 화면 확인 스크립트가 밖으로 가는 요청 0건을 확인한다.
  - 브라우저가 지울 수 있다 (방문 기록 삭제, 시크릿 모드). 서버가 생기기 전까지 백업이 없다.
  - 같은 폰·브라우저를 쓰는 다른 사람이 볼 수 있다.
  - 브라우저 안에서 암호화해도 키가 같은 브라우저에 있어 실익이 적다. 서버로 옮길 때 접근 규칙으로 지킨다.
  - Pleos 보안 가이드의 "기기에 저장하는 민감정보는 암호화"는 차 앱 대상이다 ([ux-guidelines.md](pleos/ux-guidelines.md)). 웹은 Pleos 앱이 아니지만, 심사·PT에서 설명할 때 이 차이를 알고 있어야 한다.
- 의료정보를 로그에 남기지 않는다 (`console.log` 0건).
- 반려동물 의료정보의 법적 분류는 확인하지 않았다. 민감정보처럼 다룬다. 보호자 이름·연락처 같은 사람 정보는 받지 않는다.
- 화면 아래에 저장 위치를 늘 보인다 (`domain/owner/ownerNotice.ts`, 출품 유의사항: 제출 내용이 사실과 다르면 선정 취소): 서버 없음 "이 정보는 지금 쓰는 브라우저에만 저장돼요. 이 화면은 서버에 연결되지 않아 차와 연결할 수 없어요." / 동의 전 "…차에서 쓰려면 서버에 저장해 주세요." / 동의 뒤 "이 정보는 MeongCare 서버에 저장되고, 연결한 차에서만 볼 수 있어요." (에뮬레이터면 "(시연용 로컬 서버)")

### Firebase로 옮기기 (014에서 한 것과 남은 것)

- 한 것: 익명 Auth로 보호자 구분, 보안 규칙(R14~R24), Firestore 저장소와 `'network'`, 수집 항목·쓰는 곳·보관을 알리는 동의 화면과 명시적인 [동의하고 서버에 저장], 동의 뒤 브라우저 정보 한 번 옮기고 지우기, 사진은 보호자 전용 문서로 분리.
- 남은 것: 개인정보처리방침 URL (Pleos 앱 제출 폼 필수 항목. 대회 제출에는 필요 없다), 사진을 Storage + `photoUrl`(형식 v2)로 옮길지 (Spark에서 쓸 수 있는지 미확인), 보호자가 자기 데이터를 지우기·연결된 차 관리 (PT 전, 다음 계획), 연결 코드 마구 대입 제한(Functions·App Check 필요, 지금은 6자·10분·한 번만).
- 차 앱이 웹 페이지를 연결하면 도메인 등록 증빙이 필요하다 ([ux-guidelines.md](pleos/ux-guidelines.md) "외부 서버·네트워크").
- 비밀값: Firebase 웹 설정은 `web/.env.local`(gitignore)의 `VITE_FIREBASE_*`로 받는다. 값이 없으면 localStorage 구현을 쓰고 빌드는 그대로 된다. 그때 `web/.env.example`을 같이 만든다.
  - `VITE_`로 시작하는 값은 번들에 들어가 누구나 볼 수 있다. **Claude API 키, 카카오 키는 웹에 절대 넣지 않는다** (서버에만).
- (012에서 더함) 진짜 프로젝트로 바꿀 때: 콘솔에서 익명 로그인 켜기, `firebase deploy --only firestore:rules`, 병원 문서·병원 코드 넣기(진짜 코드는 커밋하지 않는다), API 키 제한(웹 도메인·Android 앱), 남은 환자 건 정리 방법, 개인정보처리방침에 "병원에 보내는 진료 카드" 항목. 순서: [firebase/README.md](../firebase/README.md)

## 병원 웹 (012, 2026-11-06)

시연용 병원 화면. 병원 직원이 예상 대기 시간·혼잡도·응급 접수를 알리고, 차에서 보낸 진료 카드와 도착 예정을 실시간으로 본다.

- 위치: `web/` 안의 둘째 페이지 `/hospital/` (Vite 다중 페이지, `build.rolldownOptions.input`). 라우터를 넣지 않았다. 보호자 페이지 번들에는 Firebase가 들어가지 않는다.
- 저장소: `HospitalDesk` 인터페이스 (`web/src/domain/hospital/hospitalDesk.ts`). 메모리 구현(서버 없음, 시연 병원 5곳 + 예시 환자 1건)과 Firestore 구현(`web/src/data/firestoreHospitalDesk.ts`). `web/.env.local`의 `VITE_FIREBASE_*`가 있으면 Firestore다.
- 접근: 링크 `/hospital/?h=<병원 id>&code=<병원 코드>`. 로그인 화면은 없다. 코드가 맞는 브라우저(익명 계정)만 그 병원 직원으로 등록돼 상태를 바꾸고 환자 카드를 읽는다 (사용자 결정 U1).
- 서버: `firebase/` (에뮬레이터 설정, 보안 규칙 `firestore.rules`, 시드, 규칙 점검 R1~R13). 사용법: [firebase/README.md](../firebase/README.md)
- 데이터 형식: [docs/data/hospital-status.md](data/hospital-status.md), [docs/data/emergency-case.md](data/emergency-case.md)
- 보관: 병원 [완료]가 환자 건을 지운다. 차는 도착·취소를 상태로 알리고 지우지 않는다. 자동 삭제(Functions)는 없다.
- 화면에 늘 "시연용 화면이에요. 실제 병원이 아니에요."를 보인다. 메모리 모드면 "서버 없이 이 브라우저에서만 동작해요"를 더한다.
- (013) **[카드 확인]**: 오는 중·도착 건에 큰 버튼. `acknowledgedAt` = 서버 시각, 한 번만 (보안 규칙 R9~R13). 확인 전 건은 "새 환자"로 주황 테두리 강조, 탭 제목 "(N) 새 환자 · 병원 · MeongCare 병원". 깜빡임·소리는 없다.
- (013) **서버 주소와 연결 상태**: 안내 줄 아래 "서버: 에뮬레이터 127.0.0.1:8080 · 프로젝트 … · 실시간 연결됨"(`metadata.fromCache`). 호스트가 `localhost`·`::1`이면 빨간 경고 (012 시연 준비 때 차와 병원 웹이 IPv4·IPv6 서로 다른 에뮬레이터에 붙었다).
- (013) **시연 전 점검**: `npm --prefix firebase run preflight` (PF1~PF6: 127.0.0.1 응답, 시드, [::1]의 다른 에뮬레이터, adb reverse, 30분 안 대기 정보). [firebase/README.md](../firebase/README.md)

## 동물병원 문의 흐름 (020, 2026-11-13)

카드 [동물병원 찾기] → "동물병원 응답 확인"(+ 20초 무응답 팝업) → "동물병원 비교" → "출발 확인" → `HospitalTrip.depart(choice, card)`.

- 규칙: `domain/inquiry/VetInquiry.kt` (순수, 시간 주입). 진료 시작까지 = max(이동, 남은 대기). 프로토타입 382행 규칙을 옮겼다 (계획 020 "프로토타입 규칙").
- 답변은 **앱 안 시연 응답**(`InquiryScripts.DEFAULT`/`NO_RESPONSE`, debug 패널에서 고름). 병원 웹과 연결하지 않는다. 병원은 `DemoHospitals`(BUILT_IN, 카드는 Fake 전송).
- 화면: `ui/hospital/VetInquiryScreen.kt`, `DepartConfirmScreen.kt`, 상태 `VetInquiryViewModel`(액티비티 범위). 시연 지도는 `DemoMap.kt` 공용.
- 연결: `MainActivity`의 `SHOW_INQUIRY_FLOW`. false면 예전 제휴 병원 목록(012). 홈 [병원 찾기]는 019의 `DemoHospitalFindScreen` 그대로.
- 주행 중에는 `homeContentOf`가 먼저 주행 화면을 고른다 (새 화면은 정차 쪽 HOSPITALS/CONFIRM_HOSPITAL 안).

## 테스트 전략

- 단위 테스트(`app/src/test/`)로 Fake 데이터 소스와 ViewModel을 검증한다.
- 화면은 `/verify` 스킬로 에뮬레이터 스크린샷을 찍어 확인한다.
- Real 구현은 Pleos 에뮬레이터(GPU PC)에서 확인한다.
  - `android.car`를 부르는 어댑터는 JVM으로 시험하지 않는다. 판단 로직(`RealDrivingStateSource`)만 가짜 client로 시험한다.
  - Pleos SpeechToText를 부르는 어댑터(`PleosSpeechToTextClient`)도 같다. 판단 로직(`RealVoiceInput`)만 가짜 client로 시험한다.
  - Pleos TextToSpeech(`PleosTextToSpeechClient`)와 `AndroidAudioFocusGate`도 같다. 판단 로직(`RealSpeaker`)을 가짜 client·가짜 포커스로, 안내 규칙(`TripAnnouncer`)을 가짜 `Speaker`로 시험한다.
- 증상 키워드 규칙은 이름 붙은 테스트와 함께, 받아쓴 문장 모음(`app/src/test/resources/voice/symptom-sentences.tsv`)으로도 시험한다. Pleos 에뮬레이터에서 실제로 받아쓴 문장은 출처에 "관찰 날짜"를 적어 더한다.
  - 주행 상태는 Virtual Console HTTP 요청으로 바꾼다 (docs/pleos/emulator-setup.md "Pleos 에뮬레이터에서 직접 해 본 것").
