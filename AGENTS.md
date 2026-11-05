# AGENTS.md

이 파일은 지도다. 매 세션에 꼭 필요한 것만 두고, 자세한 내용은 `docs/`에 둔다.
여기와 docs가 실제와 다르면 코드보다 문서를 먼저 고친다.

## 프로젝트

- 2026 현대자동차그룹 차량용 앱 개발 공모전 출품작.
- 플랫폼은 **Pleos Connect** (AAOS 기반 차량 인포테인먼트)용 Android 앱이다.
- 팀명: `<팀명>` / 앱 이름: `<앱이름>` (확정되면 채운다. 제출 APK 파일명에 쓰임)
- 마감:
  - **11/15 접수 마감**
  - 11/23 서류 결과
  - **12/03~04 PT** (동작하는 앱 필수)
- 보호자용 웹(`web/`)이 함께 있다. 보호자가 폰 브라우저에서 반려견 의료정보를 등록한다. 차 앱과는 아직 연결되지 않았다.
- 필수 제출물은 기획서 DOCX 하나다. 코드, APK, 시연 영상은 가산점이다. 우선순위를 판단할 때 이걸 기준으로 삼는다.
- 요구사항, 심사 기준, 제출 체크리스트: [docs/contest/requirements.md](docs/contest/requirements.md)

## 기술 스택 (확정된 것만)

- Kotlin 2.0.0, Jetpack Compose + Material3. 공식 Pleos 샘플과 같은 값으로 맞춘다.
- minSdk 28 (Pleos 보안 요구사항), compile/target SDK 35
- AGP 8.8.2, Gradle wrapper 8.10.2 (Pleos 요구는 8.0 이상), JDK 17 이상 (소스·타깃은 17)
- 단일 `app` 모듈, MVVM (ViewModel + StateFlow), 수동 DI. 구조와 결정 근거: [docs/architecture.md](docs/architecture.md)
- 서비스 기능 스택: Room(암호화), Pleos STT/TTS `ON_DEVICE`, Pleos NaviHelper, Firebase 서버(Functions), Claude API(임시 Opus, 팀 논의 중), 카카오 로컬 API. 표와 승인된 Pleos 권한: [docs/architecture.md](docs/architecture.md) "서비스 기능 스택"
- applicationId `com.meongcare.app`, 앱 이름 **MeongCare** (2026-11-05 사용자 결정). 이름은 Android `strings.xml`의 `app_name`과 `web/src/config.ts`의 `APP_NAME` 두 곳에 있다
- Pleos Maven: `https://nexus-playground.pleos.ai/repository/maven-releases/`
- Vehicle SDK: `ai.pleos.playground:Vehicle:2.3.2`
- 그 밖의 SDK 좌표, API, 권한: [docs/pleos/sdk-notes.md](docs/pleos/sdk-notes.md)
- 에뮬레이터: [docs/pleos/emulator-setup.md](docs/pleos/emulator-setup.md). GPU 필수이고 **Cold boot로만** 켠다.
- 보호자 웹: React + Vite + TypeScript, 테스트는 Vitest. Node 22.12 이상. 버전은 `web/package.json`에 정확히 고정한다 (`^`, `~` 없음). 저장은 지금 브라우저 localStorage, 나중에 Firebase. 자세히: [docs/architecture.md](docs/architecture.md) "보호자 웹"

## 명령어

Windows에서는 `gradlew.bat`을 쓴다. Bash에서는 `./gradlew`.

| 목적 | 명령 |
|---|---|
| 빌드 | `./gradlew assembleDebug` |
| 단위 테스트 | `./gradlew testDebugUnitTest` |
| 린트 | `./gradlew lintDebug` |
| 설치 | `adb install -r app/build/outputs/apk/debug/app-debug.apk` |
| 빌드→실행→스크린샷 확인 | `/verify` 스킬 |
| 검증 파이프라인 (CI와 같음, Android + 웹) | `node .claude/hooks/build-gate.mjs pipeline` |
| 웹 의존성 설치 | `npm --prefix web ci` |
| 웹 검사 (타입·빌드·테스트) | `npm --prefix web run check` |
| 웹 개발 서버 | `npm --prefix web run dev` |
| 웹 화면 확인 | `build` → `preview` → `python web/scripts/verify_screens.py` ([web/README.md](web/README.md)) |
| dev에 반영 | `/ship` 스킬 |
| main에 반영 (요청할 때만) | `/promote` 스킬 |
| 제출용 APK | `/release-apk` 스킬 |

## 아키텍처 규칙

- Pleos SDK와 AAOS Car API는 `data/` 아래 래퍼 클래스에서만 호출한다. UI와 도메인은 `domain/`의 인터페이스(예: `DrivingStateSource`)만 의존한다.
- SDK 래퍼마다 **Fake 구현을 같이 만든다.**
  - 쓰임: GPU 없는 PC에서 개발, 단위 테스트, 시연 영상 촬영.
  - `AppContainer`가 데이터 모드(Fake / 시연 / Real)에 따라 구현을 끼운다. 구성표: [docs/architecture.md](docs/architecture.md) "데이터 모드".
  - debug 빌드는 화면 아래 debug 패널에서 모드를 바꾼다 (재시작하면 적용).
  - **release는 시연 모드다**: 주행 상태는 차량 기기면 Real, 아니면 Fake 시나리오. 길안내는 시연용 Fake이고 화면에 그렇게 알린다. Real 길안내가 생기면 다시 정한다.
- SDK의 콜백 API(`onComplete`/`onFailed`)는 래퍼 안에서 `suspend` 함수나 `Flow`로 감싼다.
- `Vehicle`의 `initialize()`/`release()`는 한 곳에서만 관리한다: `data/vehicle/VehicleConnection` (REAL 모드에서만, 프로세스 수명주기에 맞춰).
  - 차량 기능 래퍼는 `VehicleConnection.vehicle`을 쓴다. `Vehicle`을 직접 만들지 않는다.
- `secrets.properties`가 없어도 빌드는 되게 한다 (빈 값을 기본값으로). 그래야 CI와 새 팀원 PC에서 깨지지 않는다.
- 보호자 웹 화면은 `PetProfileRepository`(인터페이스)만 쓴다. localStorage는 `web/src/data/`와 `web/src/container.ts`에서만 쓴다.
- 반려견 프로필 형식의 기준은 [docs/data/pet-profile.md](docs/data/pet-profile.md)다. 웹과 차 앱이 같이 따른다.

## 규칙

1. **Pleos API는 추측하지 않는다.**
   - 먼저 `docs/pleos/sdk-notes.md`를 본다.
   - 거기 없으면 `pleos-docs-researcher` 서브에이전트로 공식 문서를 확인하고, 출처와 함께 기록한 뒤에 쓴다.
2. **새 `pleos.car.permission.*` 권한은 사용자에게 먼저 확인받는다.** 승인되지 않은 권한은 앱 심사에서 탈락 사유가 된다.
3. **주행 중 화면은 최소 정보, 큰 터치 타깃, 텍스트 입력 없음.** 정차·주차 중에만 허용할 기능을 구분한다. 세부 가이드: [docs/pleos/ux-guidelines.md](docs/pleos/ux-guidelines.md)
4. **비밀값은 커밋하지 않는다.**
   - Client ID/Secret은 `secrets.properties`, 서명키는 `keystore.properties` + `*.jks`. 둘 다 gitignore 대상이다.
   - 코드에는 BuildConfig로 주입한다.
5. **3단계 이상이거나 여러 세션에 걸치는 작업은** `docs/plans/active/`에 계획을 먼저 쓴다. 형식은 [docs/plans/README.md](docs/plans/README.md).
6. **"완료"라고 말하기 전에 빌드가 통과해야 한다.** UI 변경은 `/verify`로 화면까지 확인한다. 확인하지 못한 건 확인 못 했다고 보고한다.
7. **새로 알게 된 사실과 결정은 해당 docs 파일에 반영한다.** 대화에만 남기지 않는다.
8. **작업 브랜치에서 작게 커밋하고, 검증 파이프라인을 통과해야 dev에 합친다. main은 요청할 때만 바뀐다.**
   - main과 dev에 직접 커밋하지 않는다. 작업 브랜치는 최신 dev에서 딴다.
   - 논리 단위 하나가 끝날 때마다 커밋하고, 커밋할 때마다 빌드가 통과해야 한다.
   - PR은 쓰지 않는다. 순서: 최신 dev 위로 rebase → 검증 파이프라인 → 로컬에서 `--no-ff` merge → dev push
   - main은 제출·시연용이다. 사용자가 요청할 때만 검증된 dev를 그대로 올린다.
   - 작업 브랜치는 원격에 push하지 않는다.
   - 브랜치 이름, 커밋 메시지 형식, 반영 절차: [docs/conventions/git.md](docs/conventions/git.md)
9. **작업이 끝나면 개발 노트를 남긴다.**
   - 위치: `docs/notes/<git user.name>/YYYY-MM-DD-<작업>.md`
   - 형식: [docs/notes/README.md](docs/notes/README.md)

## 디렉토리

| 경로 | 내용 |
|---|---|
| `app/` | Android 앱 모듈 (`com.meongcare.app`). 패키지 구조: [docs/architecture.md](docs/architecture.md) |
| `apk/` | 제출용 release APK (`<팀명>_<앱이름>_release.apk`) |
| `web/` | 보호자 웹 (React). 실행 방법: [web/README.md](web/README.md) |
| `docs/data/` | 웹·차 앱·서버가 같이 따르는 데이터 형식 (반려견 프로필 등) |
| `docs/contest/` | 대회 요구사항 |
| `docs/pleos/` | Pleos SDK와 에뮬레이터에 대해 검증된 사실 |
| `docs/product/` | 기획 (문제, 차량특화성, 사업성). 기획서 DOCX의 원천 |
| `docs/product/bmad/` | BMAD 기획 산출물 (브레인스토밍, 제품 개요, PRD, UX, 아키텍처) |
| `_bmad/` | BMAD 설치본 (v6.12.4). 직접 고치지 않는다. 팀 설정은 `_bmad/custom/config.toml` |
| `docs/plans/` | 실행 계획 (`active/`, `completed/`) |
| `docs/notes/` | 개발 노트. 팀원별 폴더 아래 작업별 파일 |
| `docs/conventions/` | Git 등 작업 규칙 |
