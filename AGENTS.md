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
- 필수 제출물은 기획서 DOCX 하나다. 코드, APK, 시연 영상은 가산점이다. 우선순위를 판단할 때 이걸 기준으로 삼는다.
- 요구사항, 심사 기준, 제출 체크리스트: [docs/contest/requirements.md](docs/contest/requirements.md)

## 기술 스택 (확정된 것만)

- Kotlin. minSdk 28 (Pleos 보안 요구사항), compile/target SDK 34, Gradle 8.0+
- Pleos Maven: `https://nexus-playground.pleos.ai/repository/maven-releases/`
- Vehicle SDK: `ai.pleos.playground:Vehicle:2.3.2`
- 그 밖의 SDK 좌표, API, 권한: [docs/pleos/sdk-notes.md](docs/pleos/sdk-notes.md)
- 에뮬레이터: [docs/pleos/emulator-setup.md](docs/pleos/emulator-setup.md). GPU 필수이고 **Cold boot로만** 켠다.

## 명령어

Windows에서는 `gradlew.bat`을 쓴다. Bash에서는 `./gradlew`.

| 목적 | 명령 |
|---|---|
| 빌드 | `./gradlew assembleDebug` |
| 단위 테스트 | `./gradlew testDebugUnitTest` |
| 린트 | `./gradlew lintDebug` |
| 설치 | `adb install -r app/build/outputs/apk/debug/app-debug.apk` |
| 빌드→실행→스크린샷 확인 | `/verify` 스킬 |
| 제출용 APK | `/release-apk` 스킬 |

## 아키텍처 규칙

- Pleos SDK는 `data/` 아래 래퍼 클래스에서만 호출한다. UI와 도메인은 인터페이스(예: `VehicleDataSource`)만 의존한다.
- SDK 래퍼마다 **Fake 구현을 같이 만든다.**
  - 쓰임: GPU 없는 PC에서 개발, 단위 테스트, 시연 영상 촬영.
  - debug 빌드에서 Fake와 Real을 전환할 수 있게 한다.
- SDK의 콜백 API(`onComplete`/`onFailed`)는 래퍼 안에서 `suspend` 함수나 `Flow`로 감싼다.
- `Vehicle`의 `initialize()`/`release()`는 한 곳(Application 또는 lifecycle-aware 객체)에서만 관리한다.
- `secrets.properties`가 없어도 빌드는 되게 한다 (빈 값을 기본값으로). 그래야 CI와 새 팀원 PC에서 깨지지 않는다.

## 규칙

1. **Pleos API는 추측하지 않는다.**
   - 먼저 `docs/pleos/sdk-notes.md`를 본다.
   - 거기 없으면 `pleos-docs-researcher` 서브에이전트로 공식 문서를 확인하고, 출처와 함께 기록한 뒤에 쓴다.
2. **새 `pleos.car.permission.*` 권한은 사용자에게 먼저 확인받는다.** 승인되지 않은 권한은 앱 심사에서 탈락 사유가 된다.
3. **주행 중 화면은 최소 정보, 큰 터치 타깃, 텍스트 입력 없음.** 정차·주차 중에만 허용할 기능을 구분한다. (세부 가이드는 `docs/pleos/ux-guidelines.md`, 아직 미작성)
4. **비밀값은 커밋하지 않는다.**
   - Client ID/Secret은 `secrets.properties`, 서명키는 `keystore.properties` + `*.jks`. 둘 다 gitignore 대상이다.
   - 코드에는 BuildConfig로 주입한다.
5. **3단계 이상이거나 여러 세션에 걸치는 작업은** `docs/plans/active/`에 계획을 먼저 쓴다. 형식은 [docs/plans/README.md](docs/plans/README.md).
6. **"완료"라고 말하기 전에 빌드가 통과해야 한다.** UI 변경은 `/verify`로 화면까지 확인한다. 확인하지 못한 건 확인 못 했다고 보고한다.
7. **새로 알게 된 사실과 결정은 해당 docs 파일에 반영한다.** 대화에만 남기지 않는다.
8. **작업 브랜치에서 작게 커밋한다.**
   - main에 직접 커밋하지 않는다.
   - 논리 단위 하나가 끝날 때마다 커밋하고, 커밋할 때마다 빌드가 통과해야 한다.
   - push와 PR은 사용자가 요청할 때만 한다.
   - 브랜치 이름, 커밋 메시지 형식, 머지 방식: [docs/conventions/git.md](docs/conventions/git.md)
9. **작업이 끝나면 개발 노트를 남긴다.**
   - 위치: `docs/notes/<git user.name>/YYYY-MM-DD-<작업>.md`
   - 형식: [docs/notes/README.md](docs/notes/README.md)

## 디렉토리

| 경로 | 내용 |
|---|---|
| `app/` | Android 앱 모듈 (Android Studio로 생성 예정) |
| `apk/` | 제출용 release APK (`<팀명>_<앱이름>_release.apk`) |
| `docs/contest/` | 대회 요구사항 |
| `docs/pleos/` | Pleos SDK와 에뮬레이터에 대해 검증된 사실 |
| `docs/product/` | 기획 (문제, 차량특화성, 사업성). 기획서 DOCX의 원천 |
| `docs/plans/` | 실행 계획 (`active/`, `completed/`) |
| `docs/notes/` | 개발 노트. 팀원별 폴더 아래 작업별 파일 |
| `docs/conventions/` | Git 등 작업 규칙 |
