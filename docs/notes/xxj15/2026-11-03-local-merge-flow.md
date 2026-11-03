# PR 없이 로컬 merge + 검증 파이프라인

- 날짜: 2026-11-03
- 작성: xxj15
- 브랜치: chore/dev-workflow
- 계획: docs/plans/completed/002-local-merge-flow.md
- 커밋: e06eeec..4ef3894

## 무엇을 했나

PR과 리뷰를 없앴다. 이제 작업 브랜치를 `/ship` 한 번으로 main에 반영한다.

- `/ship` 순서: 최신 main 위로 rebase → 검증 파이프라인 → 로컬 `--no-ff` merge → push
- 검증 파이프라인을 통과하지 않은 코드는 main에 push되지 않는다. 사람이 터미널에서 push해도 마찬가지다.

## 어떻게: 핵심 결정과 이유

- **PR 대신 검증 파이프라인을 게이트로 쓴다.**
  - 이유: 팀이 PR 리뷰를 하지 않을 것이라 PR은 절차만 늘린다.
  - 대신 CI가 하던 검사(`assembleDebug testDebugUnitTest lintDebug`)를 push 전에 로컬에서 돌린다.
- **게이트는 git `pre-push` 훅에 둔다.** Claude Code 훅이 아니다.
  - 이유: Claude뿐 아니라 사람이 터미널에서 push해도 걸려야 한다.
  - `core.hooksPath`는 SessionStart 훅이 자동으로 켠다. 이미 다른 값이 있으면 덮어쓰지 않고 알리기만 한다.
- **rebase → 검증 → `--no-ff` merge 순서로 한다.**
  - 검증한 트리와 main에 들어가는 트리가 같아진다. 그래서 pre-push가 같은 검사를 다시 돌리지 않는다.
  - 검증이 실패하면 브랜치에서 고치면 된다. 이미 한 merge를 되돌릴 일이 없다.
  - `--no-ff`로 merge 커밋을 남긴다. 작은 커밋들이 기능 단위로 묶이고, `git revert -m 1`로 기능 하나를 통째로 되돌릴 수 있다.
- **작업 브랜치는 원격에 올리지 않는다.**
  - 올린 브랜치를 rebase하면 force push가 필요하다. force push는 막혀 있다.
- **push 권한 확인 창을 최종 확인으로 쓴다.**
  - Claude가 작업 끝에 `/ship`까지 스스로 진행한다. 사람은 push 승인만 하면 된다.
- **앱 코드 해시에서 줄바꿈을 무시한다.**
  - Windows의 autocrlf가 브랜치를 전환할 때 파일을 CRLF로 다시 써서, 불필요하게 재빌드되고 있었다.

## 사용한 Pleos SDK / API / 권한

없음.

## 확인

- pre-push: 임시 저장소와 bare 원격으로 실제 `git push` 시나리오 10개를 돌려 모두 기대대로 동작했다.
  - 기능 브랜치 push는 통과
  - `/ship`에서 이미 통과한 상태면 재실행하지 않음
  - 커밋 안 된 앱 코드가 있으면 차단
  - 다른 브랜치가 체크아웃돼 있으면 차단
  - 파이프라인이 실패하면 차단 (원격은 그대로)
  - 원격 브랜치 삭제는 통과
  - SessionStart의 `hooksPath` 설정과, 기존 값이 있을 때 덮어쓰지 않는 동작
- 기존 빌드 게이트 시나리오 16개와 트레일러 차단 8개를 다시 돌려 모두 통과했다.
- 실제 저장소에는 아직 `gradlew`가 없다. 그래서 진짜 Gradle과 함께 돌려보지는 못했다.

## 주의점 / 남은 일

- Android 프로젝트를 만든 뒤 처음 `/ship`할 때, 실제 파이프라인 시간과 pre-push 동작을 확인한다.
- Claude Code를 쓰지 않는 팀원은 `git config core.hooksPath .githooks`를 직접 한 번 실행해야 한다.
- 이 브랜치(`chore/dev-workflow`)는 PR 흐름 때 원격에 올렸다. 그래서 rebase 대신 `git merge origin/main`으로 반영한다.
