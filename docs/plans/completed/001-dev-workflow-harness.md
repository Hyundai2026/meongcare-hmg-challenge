# 001 개발 워크플로 하네스 (커밋 규칙, 노트, planner/tester)

- 목표: 계획 → 구현(작은 커밋) → 테스트 → 확인 → 노트로 이어지는 작업 흐름을 문서, 서브에이전트, 훅으로 고정한다.
- 완료 기준:
  - 빌드 게이트 훅이 임시 저장소 시나리오 테스트를 통과한다. 확인할 것:
    - main 직접 커밋을 막는다.
    - 빌드가 실패하면 커밋을 막는다.
    - 턴 중에 커밋했어도 Stop에서 빌드한다.
    - 문서만 바뀐 커밋은 빌드 없이 통과한다.
  - AGENTS.md, CLAUDE.md, docs가 새 흐름과 서로 맞는다.
- 브랜치: chore/dev-workflow
- 관련 문서: AGENTS.md, CLAUDE.md, docs/plans/README.md

## 결정 (사용자 확인, 2026-11-03)

- 노트: 작업별 노트를 팀원 폴더 아래에 둔다 → `docs/notes/<git user.name>/YYYY-MM-DD-<작업>.md`
- 커밋: Claude가 논리 단위가 끝날 때마다 자동으로 커밋한다. push는 하지 않는다.
- 브랜치: 기능 브랜치 + PR

## 단계

- [x] 1. Git 규칙 문서 `docs/conventions/git.md` → `docs(conventions): Git 브랜치·커밋 규칙 추가`
- [x] 2. 노트 규칙과 템플릿 `docs/notes/README.md` → `docs(notes): 개발 노트 규칙 추가`
- [x] 3. 서브에이전트 `planner`, `tester` → `chore(harness): planner·tester 서브에이전트 추가`
- [x] 4. 빌드 게이트 훅: 작업 트리 해시 기준으로 바꾸고, 커밋 직전 검사를 추가 → `chore(harness): 커밋 직전 빌드 게이트 추가`
- [x] 5. 설정 (git 권한, push는 물어보기), PR 템플릿, AGENTS/CLAUDE/plans README 갱신 → `docs: 작업 흐름 반영`
- [x] 6. 개발 노트 작성 → `docs(notes): 개발 워크플로 하네스 노트`

## 진행 로그

- 2026-11-03: 시작.
  - 기존 Stop 훅은 커밋 안 된 변경만 봤다. 그래서 자동 커밋과 같이 쓰면 빌드 검사를 건너뛴다.
  - 해시 기준을 "작업 트리 전체의 앱 코드 내용"으로 바꾸기로 했다.

## 결정 / 배운 점

- build-gate의 커밋 명령 판별:
  - 스테이징된 앱 코드가 있으면 빌드한다.
  - 같은 명령 안에 `git add`나 `commit -a`가 있으면, 바뀐 앱 코드가 있을 때 빌드한다.
  - 그 밖에는 문서 커밋으로 보고 통과시킨다.
- Bash 도구로 heredoc을 넘기면 템플릿 문자열 안의 백슬래시 두 개가 하나로 줄어든다. 그래서 정규식이 틀린 것처럼 보였다.
  - 정규식은 훅 파일에서 바로 꺼내 확인해야 한다.
  - 백슬래시가 들어간 파일은 Write/Edit 도구로 쓴다.
