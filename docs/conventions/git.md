# Git 규칙

사람과 에이전트 모두에게 적용된다. 커밋 직전 검사 중 일부는 Claude Code 훅이 강제한다 (`.claude/hooks/build-gate.mjs`).

## 브랜치

- `main`은 PR로만 바꾼다. **main에 직접 커밋하지 않는다.**
  - 훅이 Claude의 main 커밋을 막는다.
  - 저장소의 첫 커밋만 예외다.
- 작업 하나(계획 하나 또는 기능 하나)마다 브랜치를 하나 만든다. 최신 main에서 딴다.

  ```
  git switch main && git pull
  git switch -c feat/<짧은-이름>
  ```

- 접두사는 커밋 type과 맞춘다: `feat/`, `fix/`, `refactor/`, `docs/`, `chore/`
- 이름은 영어 소문자와 `-`로 쓴다. 예: `feat/vehicle-speed-flow`

## 커밋 단위: 작게

- **한 커밋 = 한 가지 이유의 변경.**
  - 커밋 메시지 요약을 "그리고" 없이 한 줄로 쓸 수 없으면 쪼갠다.
- **커밋할 때마다 빌드가 통과해야 한다.**
  - 앱 코드가 들어간 커밋은 훅이 `assembleDebug`를 먼저 돌린다.
- 크기 기준: diff 200줄 이하를 권장한다. 생성된 코드와 리소스는 줄 수에서 빼고 센다.
- 쪼개는 기준:
  - 리팩터링과 기능 변경을 섞지 않는다. 동작은 그대로 두고 구조만 바꾼 건 `refactor:`로 먼저 커밋한다.
  - 포맷팅이나 이름만 바꾼 변경은 따로 커밋한다.
  - 의존성 추가(`build.gradle.kts`, `libs.versions.toml`)는 그 의존성을 처음 쓰는 커밋에 같이 넣는다. 쓰지 않는 의존성만 먼저 넣지 않는다.
  - 테스트는 대상 코드와 같은 커밋에 넣는 게 좋다. `tester`가 나중에 쓴 테스트는 `test:` 커밋으로 따로 낸다.
- SDK 래퍼를 만들 때 커밋을 나누는 예:
  1. `feat(data): VehicleDataSource 인터페이스와 Fake 구현`
  2. `feat(ui): 차속 표시 화면 (Fake로 동작)`
  3. `feat(data): Pleos Vehicle SDK로 Real 구현`
  4. `test(data): Real 래퍼의 콜백→Flow 변환 테스트`
- 계획 파일의 단계 하나가 커밋 1~3개 정도가 되게 한다.

## 커밋 메시지

```
<type>(<scope>): <요약>

<본문: 왜 바꿨는지. 선택>
```

- **type**:

  | type | 쓰는 경우 |
  |---|---|
  | `feat` | 기능 추가 |
  | `fix` | 버그 수정 |
  | `refactor` | 동작 변화 없는 구조 변경 |
  | `test` | 테스트만 바뀜 |
  | `docs` | 문서만 바뀜 |
  | `build` | Gradle, 의존성 |
  | `ci` | GitHub Actions |
  | `chore` | 그 밖의 것 (하네스 설정 등) |

- **scope**: 선택. `data`, `ui`, `domain`, `vehicle`, `harness`, `notes`, `plans` 등. 바뀐 영역을 한 단어로 쓴다.
- **요약**: 한국어 50자 이내. 마침표 없이, "~ 추가", "~ 수정"처럼 명사형으로 끝낸다.
  - 예: `feat(vehicle): 차속 Flow 래퍼 추가`
- 본문에는 "무엇"보다 "왜"를 쓴다. 관련 계획이 있으면 경로를 적는다.

## 스테이징

- 파일을 이름으로 지정해서 올린다: `git add <파일>...`
  - `git add -A`, `git add .`, `git commit -a`는 쓰지 않는다. 다른 단위의 변경이나 비밀값이 섞여 들어간다.
- 커밋 전에 `git status`로 예상하지 못한 파일이 없는지 본다.

## 에이전트(Claude)의 커밋

- **논리 단위가 하나 끝날 때마다 묻지 않고 커밋한다.** 사용자가 이 규칙으로 승인했다 (2026-11-03).
  - 몰아서 커밋한 뒤 나중에 쪼개지 않는다.
- 커밋 전 확인:
  - 앱 코드 커밋: 훅이 빌드를 확인한다.
  - UI 변경이 들어간 단위: 커밋 전에 `/verify`로 화면까지 본다.
  - 문서만 바뀐 커밋: 추가 확인 없음.
- **push, PR 생성, main 머지는 사용자가 요청할 때만 한다.**
- 실패한 커밋을 `--amend`로 고치는 건 아직 push하지 않은 커밋에만 한다.

## Push와 PR

- PR은 기능 브랜치를 push한 뒤 연다.
  - 본문은 `.github/pull_request_template.md`를 따른다.
  - 관련 계획과 개발 노트를 링크한다.
- CI(`.github/workflows/android.yml`)가 통과해야 머지한다.
- **머지는 "Create a merge commit"으로 한다.** squash는 쓰지 않는다. 잘게 나눈 커밋 기록이 사라진다.
- 머지한 브랜치는 지운다.
- force push 금지. `.claude/settings.json`에서 막혀 있다.
