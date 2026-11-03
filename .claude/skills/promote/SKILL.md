---
name: promote
description: dev를 main에 반영한다. 제출(11/15)이나 PT 전처럼 사용자가 요청할 때만 쓴다. 검증 파이프라인을 다시 확인한 뒤 main을 dev로 fast-forward하고 push한다.
argument-hint: "[반영 이유. 예: 11/15 제출]"
disable-model-invocation: true
---

# promote: dev → main

main은 심사위원이 보는 브랜치다. GitHub 기본 브랜치이고, 제출본(소스와 `apk/`)이 여기 있어야 한다.
그래서 사용자가 요청할 때만, 이미 검증된 dev 상태 그대로 올린다.

- 규칙: [docs/conventions/git.md](../../../docs/conventions/git.md)
- 반영 이유: $ARGUMENTS

Gradle을 돌리는 명령은 Bash timeout을 600000으로 준다.

## 1. 사전 확인

- `git status`가 깨끗해야 한다. 아니면 멈추고 사용자에게 묻는다.
- `git fetch origin`
- 이번에 main에 들어갈 변경을 보여준다.

  ```
  git log --oneline origin/main..origin/dev
  ```

  - 비어 있으면 반영할 것이 없다. 멈추고 보고한다.
- 제출용이면 [docs/contest/requirements.md](../../../docs/contest/requirements.md)의 체크리스트와 아래를 확인한다. 빠진 게 있으면 사용자에게 알린다. 진행 여부는 사용자가 정한다.
  - `apk/<팀명>_<앱이름>_release.apk`가 dev에 있는가 (`/release-apk`)
  - README 필수 4개 항목

## 2. dev 검증

```
git switch dev
git merge --ff-only origin/dev
node .claude/hooks/build-gate.mjs pipeline
```

- 실패하면 멈춘다. main에는 아무것도 하지 않는다.
- 고치는 작업은 평소처럼 작업 브랜치 → `/ship`으로 dev에 넣은 뒤, `/promote`를 다시 한다.
- UI가 있는 앱이면 `/verify`로 화면도 확인한다. 제출본이 실제로 실행되는지 보는 마지막 기회다.

## 3. main을 dev로 올리기

```
git switch main
git merge --ff-only origin/dev
```

- main에는 dev에 있는 커밋만 올라간다. 그래서 fast-forward가 된다.
- `--ff-only`가 실패하면 main에 dev에 없는 커밋이 있다는 뜻이다. 멈추고 사용자에게 보고한다.

## 4. push

Git Bash에서:

```
PROMOTE_MAIN=1 git push origin main
```

PowerShell에서:

```
$env:PROMOTE_MAIN='1'; git push origin main; Remove-Item Env:PROMOTE_MAIN
```

- pre-push 훅이 확인하는 것:
  - `PROMOTE_MAIN=1`인가
  - push할 커밋이 `origin/dev`에 있는가
  - 검증 파이프라인을 통과했는가 (2단계에서 통과했으면 다시 돌리지 않는다)
- 권한 확인 창에서 사용자가 승인해야 push된다.

## 5. 마무리

```
git switch dev
```

- 작업은 dev에서 다시 시작한다.

## 6. 보고

- main에 들어간 커밋 목록 (1단계의 `git log` 결과)
- main의 새 커밋 해시. `origin/main`과 `origin/dev`가 같은 커밋인지
- 검증 결과: 파이프라인, `/verify`
- 제출 체크리스트에서 빠진 것
