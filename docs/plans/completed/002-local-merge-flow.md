# 002 PR 없이 로컬 merge + 검증 파이프라인으로 main 반영

- 목표: PR 리뷰 대신 검증 파이프라인을 게이트로 삼아, 작업 브랜치를 로컬에서 main에 merge하고 push한다.
- 완료 기준:
  - 임시 저장소와 bare 원격으로 pre-push 시나리오 테스트를 통과한다. 확인할 것:
    - main이 아닌 push는 그냥 통과한다.
    - 커밋 안 된 앱 코드가 있으면 막는다.
    - 체크아웃이 다르면 막는다.
    - 파이프라인이 실패하면 막는다.
    - 이미 통과한 상태면 다시 돌리지 않는다.
  - 이 브랜치를 `/ship` 절차로 main에 반영한다.
- 브랜치: chore/dev-workflow (아직 main에 반영 전이라 이어서 작업)
- 관련 문서: docs/conventions/git.md, CLAUDE.md

## 결정 (사용자 확인, 2026-11-03)

- PR은 쓰지 않는다. 브랜치 작업 → 로컬 merge → push. 게이트는 검증 파이프라인이다.

## 단계

- [x] 1. build-gate에 `pipeline`·`prepush` 모드 추가, `.githooks/pre-push`, SessionStart에서 `core.hooksPath` 설정 → `chore(harness): main push 전 검증 파이프라인 게이트 추가`
- [x] 2. `/ship` 스킬 → `chore(harness): /ship 스킬 추가`
- [x] 3. git.md, AGENTS, CLAUDE, notes README, tester 설명 갱신, PR 템플릿 삭제 → `docs: PR 대신 로컬 merge 흐름으로 규칙 변경`
- [x] 4. 계획과 개발 노트 → `docs(plans)`, `docs(notes)`

## 진행 로그

- 2026-11-03: 1~4 완료.
  - pre-push 시나리오 10개, 기존 빌드 게이트 16개, 트레일러 차단 8개를 모두 통과했다.
  - 다음 단계: `/ship`으로 main에 반영하고, 원격의 `chore/dev-workflow`를 삭제한다.
    - 이 브랜치는 원격에 올라가 있어서 rebase 대신 `git merge origin/main`을 쓴다.

## 결정 / 배운 점

- rebase를 먼저 하고 검증한 뒤 `--no-ff` merge를 한다.
  - 이렇게 하면 검증한 트리와 main에 들어가는 트리가 같다. 그래서 pre-push가 같은 검사를 다시 돌리지 않는다.
  - 검증이 실패해도 merge를 되돌릴 필요 없이 브랜치에서 고치면 된다.
- 앱 코드 해시는 줄바꿈을 무시해야 한다.
  - autocrlf=true인 Windows에서는 `git switch`나 merge가 파일을 CRLF로 다시 쓴다.
  - 그래서 내용이 같아도 해시가 바뀌어 파이프라인이 다시 돌았다. 시나리오 3에서 발견했다.
- pre-push는 검증 결과를 작업 트리 기준으로 판단한다.
  - 그래서 push할 커밋이 체크아웃돼 있고, 커밋 안 된 앱 코드가 없어야 한다.
