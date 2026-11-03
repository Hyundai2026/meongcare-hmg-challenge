# Android 프로젝트 초기 설정 (뼈대)

- 날짜: 2026-11-03
- 작성: xxj15
- 브랜치: chore/project-setup
- 계획: docs/plans/completed/004-project-setup.md
- 커밋: fa090e3..(이 노트까지)

## 무엇을 했나

앱 아이디어와 상관없이 필요한 바탕을 깔았다. 실제 기능은 아직 없다.

- **앱 화면**: "멍케어"(임시 이름) 앱이 정차 화면과 주행 화면을 주행 상태에 따라 바꿔 보여준다.
- **데이터**: 지금은 가짜(Fake) 주행 상태로 동작한다. 화면 아래 개발자 패널에서 상태를 바꾼다.
- **차량 SDK**: Pleos Vehicle SDK를 켜고 끄는 일을 한 곳(`VehicleConnection`)에서 맡는다. Real 모드에서만 동작한다.
- **검사**: 빌드, 테스트 26개, 린트, 커밋 직전 빌드 훅, 화면 확인(`/verify`)이 모두 실제로 돈다.

## 어떻게: 핵심 결정과 이유

자세한 표: [docs/architecture.md](../../architecture.md)

- **Compose, SDK 35, AGP 8.8.2, Kotlin 2.0.0**
  - 공식 Pleos 샘플과 같은 값이다. 팀이 웹(React) 위주라 Compose가 익숙하다.
- **수동 DI (`AppContainer`) + debug 앱 안 토글**
  - 빌드 종류를 늘리지 않아 훅과 CI의 태스크 이름이 그대로다.
- **Gradle wrapper를 검증한 공식 배포판으로 직접 만들었다.**
  - 공식 샘플의 wrapper jar가 Gradle 공식 체크섬과 맞지 않았기 때문이다.
- **상태를 모르면 주행 화면** (안전 우선)
  - 처음 켜질 때, UNKNOWN일 때, 5초 넘게 화면이 없다가 돌아올 때 모두 해당한다.
  - 마지막 경우는 tester가 찾아서 고쳤다 (`replayExpirationMillis = 0`).
- **Vehicle은 매번 새로 만든다.**
  - 앱이 보이면 새 인스턴스를 만들어 initialize하고, 사라지면 release하고 버린다. 공식 샘플도 인스턴스를 재사용하지 않는다.
- **debug 패널은 주행 화면에도 보인다** (규칙 3의 예외).
  - debug 전용이고, 주행 화면에서 정차로 되돌릴 방법이 필요해서다.

## 사용한 Pleos SDK / API / 권한

- **SDK**: `ai.pleos.playground:Vehicle:2.3.2`. `Vehicle(Context)`, `initialize()`, `release()`만 쓴다. 도메인 API는 아직 쓰지 않는다.
- **선언한 `pleos.car.permission.*`**: 0개.
- **SDK가 스스로 넣는 것**: `ai.pleos.playground.service.vehicle.VEHICLE_BINDING` 권한과 `<queries>` (차량 서비스 연결용).

## 확인

- **단위 테스트 26개 통과**
  - Fake 주행 상태 8개
  - Real 자리표시 3개
  - HomeViewModel 15개 (tester가 19개를 작성했다)
- **검증 파이프라인** (`assembleDebug testDebugUnitTest lintDebug`) 통과. 린트는 경고만 있다 (버전 고정 관련).
- **화면** (`/verify`, 일반 폰 에뮬레이터, debug, Fake)
  - 확인한 것:
    - 정차 → 주행 → 정차 전환
    - 주행 화면에 입력칸 0개
    - Real 전환과 Fake 복귀
    - 가로 화면, 다크 모드
    - 크래시 0건
  - 여기서 레이아웃 문제 2건을 찾아 고쳤다:
    - 좁은 화면에서 debug 패널이 깨졌다.
    - 가로 화면에서 입력칸이 눌렸다.
- **서명**
  - 비밀값 파일이 없어도 debug와 release가 빌드된다.
  - 서명 경로는 저장소 밖의 버리는 테스트 키로 확인하고 지웠다.

## 시연 방법

1. debug APK를 설치하고 실행한다 → 정차 화면.
2. 아래 개발자 패널의 [주행]을 누른다 → 주행 화면 ("주행 중" 큰 글씨, 입력칸 없음).
3. [정차]를 누른다 → 정차 화면으로 돌아온다.
4. [Real로 전환]을 누른다 → 앱이 다시 켜지고 Real 모드가 된다. 아직 실제 주행 상태를 못 읽어서 주행 화면이 나온다.

## 주의점 / 남은 일

- **release 앱은 지금 항상 주행 화면이다.**
  - Real 주행 상태가 아직 UNKNOWN만 내기 때문이다.
  - 11/15 제출 APK 전에 둘 중 하나를 정한다:
    - Real 주행 상태(005)를 먼저 구현한다.
    - DO를 선언하지 않는다면 "UNKNOWN = 정차"로 바꾼다.
- **Real 주행 상태 구현 (005 후보)**: 후보는 `CarUxRestrictions`, 또는 Vehicle SDK 2.3.2 AAR에만 있는 `getGear()`.
- **`initialize()`가 끝났다고 차량 서비스에 연결된 것은 아니다.** 서비스가 없어도 예외가 없다. 도메인 래퍼를 만들 때 연결 실패를 어떻게 알지 정해야 한다.
- **이 PC(RTX 4070)는 Pleos 에뮬레이터 사양이 된다.** Pleos 시스템 이미지를 설치하면 Real 모드를 확인할 수 있다.
- **재현 안 된 현상**: 에뮬레이터 부팅 직후 첫 실행 1회에 정차 대신 주행 화면이 보였다.
