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

## 패키지 구조

```
app/src/main/java/com/meongcare/app/
  MeongCareApplication.kt   AppContainer를 만든다
  MainActivity.kt           Compose 화면을 띄운다
  di/                       AppContainer, 데이터 모드(Fake/Real) 저장, debug 재시작
  domain/                   앱이 쓰는 개념과 인터페이스. 안드로이드·Pleos를 모른다
  data/                     domain 인터페이스의 구현 (Fake, Real). Pleos SDK와 Car API는 여기서만 쓴다
  ui/                       Compose 화면과 ViewModel, 테마
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

- `Vehicle`의 `initialize()`와 `release()`는 한 곳에서만 부른다 (AGENTS.md 아키텍처 규칙).
- Real 모드에서만 연결한다. 구체적인 위치는 구현하면서 정하고 여기에 적는다.

## 테스트 전략

- 단위 테스트(`app/src/test/`)로 Fake 데이터 소스와 ViewModel을 검증한다.
- 화면은 `/verify` 스킬로 에뮬레이터 스크린샷을 찍어 확인한다.
- Real 구현은 Pleos 에뮬레이터(GPU PC)에서 확인한다.
