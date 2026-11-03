---
name: ship
description: 작업 브랜치를 dev에 반영한다. 최신 dev 위로 rebase하고, 검증 파이프라인(assembleDebug testDebugUnitTest lintDebug)을 통과시킨 뒤, 로컬에서 --no-ff merge하고 push한다. PR 대신 쓴다. 작업이 끝나 노트까지 커밋한 뒤에 사용한다. main 반영은 /promote다.
argument-hint: "[브랜치 이름. 생략하면 현재 브랜치]"
---

# ship: 작업 브랜치 → dev

PR과 리뷰 대신 검증 파이프라인을 게이트로 삼는다. 규칙: [docs/conventions/git.md](../../../docs/conventions/git.md)

- main에는 반영하지 않는다. main은 사용자가 요청할 때 `/promote`로만 바뀐다.

대상 브랜치: $ARGUMENTS (비어 있으면 현재 브랜치)

Gradle을 돌리는 명령은 Bash timeout을 600000으로 준다.

## 1. 사전 확인

- 대상이 `dev`나 `main`이면 멈춘다.
- `git status`가 깨끗해야 한다.
  - 커밋 안 된 변경이 이 작업의 일부면 단위별로 커밋한다.
  - 이 작업과 관계없는 변경이면 멈추고 사용자에게 묻는다.
- 계획이 있던 작업이면 다음을 확인한다. 빠졌으면 먼저 하고 커밋한다.
  - 계획 파일이 `docs/plans/completed/`로 옮겨졌는가
  - 개발 노트(`docs/notes/<git user.name>/`)가 있는가
- 작은 수정은 노트가 없어도 된다.

## 2. 최신 dev 위로 올리기

```
git fetch origin
git switch <브랜치>
git rebase origin/dev
```

- 충돌이 나면 해결하고 `git rebase --continue`.
  - 다른 팀원의 의도를 판단해야 하는 충돌이면 `git rebase --abort`로 되돌린다. 그리고 사용자에게 묻는다.
- 이 브랜치가 이미 원격에 올라가 있으면(`git ls-remote --heads origin <브랜치>`에 나오면) rebase하지 않는다.
  - rebase하면 force push가 필요해지기 때문이다.
  - 대신 `git merge origin/dev`를 한다.

## 3. 검증 파이프라인

```
node .claude/hooks/build-gate.mjs pipeline
```

- CI(`.github/workflows/android.yml`)와 같은 작업을 돌린다: `assembleDebug testDebugUnitTest lintDebug`
- 통과하면 결과가 기록된다. 그래서 5단계의 pre-push 훅이 같은 검사를 다시 돌리지 않는다.
- 실패하면 이 브랜치에서 고치고 커밋한 뒤 3단계를 다시 한다.
- 이 브랜치에 UI 변경이 있으면 `/verify`로 화면도 확인한다.
  - 에뮬레이터가 없으면 확인하지 못했다고 보고에 적는다.

## 4. dev에 합치기

```
git switch dev
git merge --ff-only origin/dev
git merge --no-ff --no-edit <브랜치>
```

- 로컬에 `dev`가 없으면 `git switch dev`가 `origin/dev`를 따라가는 브랜치를 만든다.
- 2단계에서 rebase했으므로 충돌은 나지 않는다. 그리고 검증한 코드와 dev에 들어가는 코드가 같다.
- `--ff-only`가 실패하면 로컬 dev에 원격에 없는 커밋이 있다는 뜻이다. 멈추고 사용자에게 보고한다.

## 5. push

```
git push origin dev
```

- pre-push 훅(`.githooks/pre-push`)이 검증 통과를 확인한다.
- 권한 확인 창에서 사용자가 승인해야 push된다. 이것이 최종 확인이다.
- **거절(rejected, non-fast-forward)되면**: 그사이 다른 팀원이 push한 것이다.
  1. 로컬 merge만 되돌린다: `git reset --keep HEAD^`
  2. 2단계부터 다시 한다.
- 사용자가 push를 승인하지 않으면 로컬 dev에 merge가 남는다. 되돌리지 않고 그대로 보고한다.

## 6. 정리

원격에 같은 이름의 브랜치가 있으면(`git ls-remote --heads origin <브랜치>`에 나오면) 원격부터 지운다.

```
git push origin --delete <브랜치>
git fetch --prune origin
```

그다음 로컬 브랜치를 지운다.

```
git branch -d <브랜치>
```

- 순서를 지키는 이유: 원격에 올라간 브랜치는 upstream이 걸려 있다.
  - 이때 `git branch -d`는 dev가 아니라 upstream을 기준으로 merge 여부를 판단한다.
  - upstream이 브랜치보다 뒤처져 있으면 dev에 다 들어갔어도 삭제를 거부한다.
  - 원격을 지우고 prune하면 upstream이 사라져서 현재 브랜치(dev) 기준으로 판단한다.
- `-d`가 그래도 거부하면 `-D`로 지우기 전에 확인한다: `git merge-base --is-ancestor <브랜치> origin/dev`
- dev에 그대로 머문다. 다음 작업은 새 브랜치에서 시작한다.

## 7. 보고

- dev에 들어간 커밋 목록: `git log --oneline origin/dev~1..origin/dev`, merge 커밋 해시 포함
- 검증 결과: 파이프라인 통과 여부, `/verify` 여부
- 확인하지 못한 것
- main은 그대로다. 제출이나 시연용으로 main에 올리려면 사용자가 `/promote`를 요청해야 한다.
