# <앱 이름>

> <한 줄 소개>

2026 현대자동차그룹 차량용 앱 개발 공모전 출품작 · Pleos Connect

## 실행 방법

1. Pleos Connect 에뮬레이터를 설치한다. ([docs/pleos/emulator-setup.md](docs/pleos/emulator-setup.md))
2. 에뮬레이터를 **Cold boot**로 실행한다.
3. APK를 설치한다:

   ```
   adb install -r apk/<팀명>_<앱이름>_release.apk
   ```

4. 런처에서 `<앱 이름>`을 실행한다.

## 테스트 환경 및 필요 설정

- Pleos Connect Emulator v2.1.0 (Android API 34)
- <필요한 Virtual Console 설정, 권한 허용 등>

## APK

- [`apk/<팀명>_<앱이름>_release.apk`](apk/)
- 용량 문제로 Release에 올린 경우: <Release 링크>

## 소스에서 빌드하기

1. Android Studio(최신)와 JDK 17 이상을 준비한다.
2. `secrets.properties.example`을 복사해 `secrets.properties`를 만들고 Pleos Client ID/Secret을 채운다.
3. 빌드한다:

   ```
   ./gradlew assembleDebug
   ```

## 팀

- <이름 / 역할>

---

## 팀원 개발 가이드

이 저장소에는 Claude Code 하네스가 설정돼 있다. 규칙, 훅, 스킬, 서브에이전트로 이루어져 있다.
평소에는 Claude Code에 하고 싶은 일을 말로 시키면 되고, 아래 규칙은 훅이 대부분 자동으로 지킨다.

### 처음 한 번

1. clone한 뒤 dev 브랜치로 옮긴다. clone하면 main만 받아지기 때문이다.

   ```
   git switch dev
   ```

2. 저장소 이름이 바뀌기 전에 clone했다면 원격 주소를 바꾼다.

   ```
   git remote set-url origin https://github.com/Hyundai2026/meongcare-hmg-challenge.git
   ```

3. Node.js를 설치한다. 훅이 node로 동작한다.
4. 이 폴더를 Claude Code로 연다. 세션이 시작되면 git 훅(`core.hooksPath=.githooks`)이 자동으로 켜진다.
   - Claude Code 없이 터미널만 쓴다면 직접 켠다: `git config core.hooksPath .githooks`
5. (선택) 다이어그램 스킬(`/stack-diagram`)을 쓰려면 Python 3.9 이상과 Playwright가 필요하다.

   ```
   pip install playwright && python -m playwright install chromium
   ```

6. 기획 도구 BMAD를 쓰려면 [uv](https://docs.astral.sh/uv/)와 Python 3.10 이상이 필요하다.
   - BMAD가 나를 이름으로 부르게 하려면 `_bmad/custom/config.user.toml`을 만들어 아래처럼 쓴다. 이 파일은 커밋되지 않는다.

     ```
     [core]
     user_name = "이름"
     ```

### 브랜치

| 브랜치 | 역할 | 바뀌는 방법 |
|---|---|---|
| `main` | 제출·시연용. 심사위원이 보는 기본 브랜치 | 제출·PT 전에 `/promote`로만 |
| `dev` | 평소 작업이 모이는 곳 | 작업 브랜치를 `/ship`으로 반영 |
| `feat/*`, `fix/*` 등 | 작업 하나 | 여기서만 커밋 |

### 개발 순서

1. Claude Code에 하고 싶은 일을 말한다. 예: "차속 표시 기능 만들어줘"
2. Claude가 아래 순서로 진행한다.

   ```
   dev에서 브랜치 따기 → 계획(planner) → 구현(단위마다 자동 커밋) → 테스트(tester)
   → 화면 확인(/verify) → 개발 노트 → /ship으로 dev에 반영
   ```

3. **push 승인 창이 뜨면 승인한다.** 사람이 할 일은 이것뿐이다.
4. 제출이나 PT 전에는 `/promote`를 입력해 main에 올린다. 예: `/promote 11/15 제출`

### 명령어

| 명령 | 언제 | 누가 |
|---|---|---|
| `/promote` | dev를 main에 올릴 때 (제출·PT 전) | **직접 입력해야만 동작** |
| `/release-apk` | 제출용 APK를 만들 때 | **직접 입력해야만 동작** |
| `/ship` | 작업 브랜치를 dev에 반영할 때 | 보통 Claude가 실행. 직접 입력해도 됨 |
| `/verify` | 빌드한 앱을 에뮬레이터에서 스크린샷으로 확인할 때 | 보통 Claude가 실행 |
| `/stack-diagram` | 기술 스택·아키텍처 그림이 필요할 때 (기획서, PT). 예: "우리 앱 구조도 그려줘" | 말로 시키면 Claude가 실행. 공개 갤러리에는 올리지 않는다 |
| `/bmad-help` 등 BMAD 스킬 | 기획할 때: 브레인스토밍 → 제품 개요 → PRD → UX → 아키텍처. 예: "bmad로 아이디어 브레인스토밍 하자" | 말로 시키거나 직접 입력. 개발 단계 BMAD 스킬은 꺼 두었다 |

### 규칙

**Git**

- main과 dev에는 직접 커밋하지 않는다. 작업 브랜치는 원격에 push하지 않는다.
- PR은 쓰지 않는다.
  - 대신 검증 파이프라인(`assembleDebug testDebugUnitTest lintDebug`)을 통과해야 dev나 main에 push된다.
  - `--no-verify`와 force push는 쓰지 않는다.
- 커밋은 작게, 한 커밋에 한 가지 이유만 담는다.
  - 메시지 형식은 `type(scope): 요약`이다. 예: `feat(vehicle): 차속 Flow 래퍼 추가`
- 커밋 메시지에 Claude 트레일러(`Co-Authored-By: Claude`)를 넣지 않는다. 훅이 자동으로 막는다.

**작업 기록**

- 작업이 끝나면 개발 노트를 `docs/notes/<git user.name>/YYYY-MM-DD-<작업>.md`에 남긴다.

**앱 개발**

- Pleos API는 추측하지 않는다. `docs/pleos/sdk-notes.md`에 있는 것만 쓴다.
- 새 `pleos.car.permission.*` 권한은 먼저 팀에서 확인한다.
- 비밀값(`secrets.properties`, `keystore.properties`, `*.jks`)은 커밋하지 않는다.

### Claude Code 없이 직접 할 때

```
git switch dev && git pull --ff-only
git switch -c feat/<이름>
# 작업하고 작게 커밋

# dev에 반영 (/ship과 같은 순서)
git fetch origin && git rebase origin/dev
node .claude/hooks/build-gate.mjs pipeline
git switch dev && git merge --ff-only origin/dev
git merge --no-ff --no-edit feat/<이름>
git push origin dev
git branch -d feat/<이름>
```

- push가 거절되면 다른 팀원이 먼저 push한 것이다.
- 이때는 `git reset --keep HEAD^`로 로컬 merge만 되돌리고 `git fetch`부터 다시 한다.

### 더 자세한 문서

| 문서 | 내용 |
|---|---|
| [AGENTS.md](AGENTS.md) | 프로젝트 규칙 전체 (에이전트와 사람 공통) |
| [CLAUDE.md](CLAUDE.md) | Claude Code 작업 흐름, 훅, 서브에이전트 |
| [docs/conventions/git.md](docs/conventions/git.md) | 브랜치, 커밋, `/ship`, `/promote` 상세 |
| [docs/notes/README.md](docs/notes/README.md) | 개발 노트 템플릿 |
