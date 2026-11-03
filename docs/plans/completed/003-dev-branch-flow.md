# 003 dev 통합 브랜치 + main 보호

- 목표: 평소 작업은 dev에 반영하고, main은 사용자가 요청할 때만 dev에서 올린다.
- 완료 기준:
  - pre-push 시나리오 테스트를 통과한다. 확인할 것:
    - dev push는 검증 파이프라인을 거친다.
    - `PROMOTE_MAIN` 없이 main에 push하면 막힌다.
    - dev에 없는 커밋을 main에 push하면 막힌다.
    - main·dev 브랜치를 지우는 push는 막힌다.
    - `/promote` 방식의 push는 통과한다.
  - dev 직접 커밋을 막는다 (precommit).
  - 이 브랜치를 새 `/ship`으로 dev에 반영한다.
- 브랜치: chore/dev-branch-flow (dev에서 땀)
- 관련 문서: docs/conventions/git.md, .claude/skills/ship, .claude/skills/promote

## 결정 (사용자 확인, 2026-11-03)

- 평소 흐름: 기능 브랜치 → `/ship` → dev. dev에도 직접 커밋하지 않는다.
- main: 사용자가 요청할 때만 `/promote`로 dev를 반영한다. 예: 11/15 제출 전, PT 전.
- GitHub 기본 브랜치는 main으로 둔다. 심사위원이 제출본을 보게 하기 위해서다.

## 단계

- [x] 1. build-gate:
  - dev 직접 커밋 차단
  - pre-push 규칙: dev는 파이프라인, main은 `PROMOTE_MAIN` + dev 포함 여부 + 파이프라인, 보호 브랜치 삭제 차단
  - SessionStart에서 main에 있으면 알림
  - → `chore(harness): dev 통합 브랜치와 main 보호 게이트`
- [x] 2. `/ship`의 대상을 dev로 바꾸고 `/promote` 스킬 추가 → `chore(harness): /ship은 dev로, main은 /promote로`
- [x] 3. CI를 dev push에도 실행 → `ci: dev push에도 검증 실행`
- [x] 4. git.md, AGENTS, CLAUDE, notes README, tester 설명, release-apk 갱신 → `docs: dev 통합 브랜치 흐름으로 규칙 변경`
- [x] 5. 계획과 개발 노트

## 진행 로그

- 2026-11-03:
  - main에서 dev를 만들어 push했다 (f1dd4f6). `chore/dev-branch-flow`를 dev에서 땄다.
  - 1~5 완료. 시나리오 테스트를 모두 통과했다.
    - dev/main 흐름 10개 (bare 원격에 실제 push)
    - 기존 빌드 게이트 16개
    - 트레일러 차단 8개
  - 다음 단계: `/ship`으로 dev에 반영한다. main 반영(`/promote`)은 사용자가 요청할 때 한다.

## 결정 / 배운 점

- main은 fast-forward로만 올린다. main과 dev가 같은 커밋을 가리키면 "main은 dev에 있는 것만"이라는 규칙을 `git merge-base --is-ancestor` 하나로 검사할 수 있다.
- main push 허가는 환경 변수 `PROMOTE_MAIN=1`로 한다.
  - git pre-push 훅은 push 옵션(`-o`)을 받지 못한다.
  - 사람도 같은 방법으로 promote할 수 있다.
- 브랜치 규칙(main 직접 push 금지, 삭제 금지)은 Android 프로젝트가 없어도 적용한다. 빌드 검증만 `gradlew`가 있을 때 돈다.
