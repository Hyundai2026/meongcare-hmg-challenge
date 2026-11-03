@AGENTS.md

## Claude Code 전용

### 작업 흐름

기능 하나를 만들 때 이 순서를 따른다. 1~2단계짜리 작은 수정은 계획(2)과 `tester`(5)를 건너뛰어도 된다. 대신 테스트는 직접 추가한다.

1. **브랜치**: 최신 dev에서 `feat/<이름>` 브랜치를 딴다.

   ```
   git switch dev && git pull --ff-only && git switch -c feat/<이름>
   ```
2. **계획**: `planner` 서브에이전트로 `docs/plans/active/`에 계획을 쓴다.
   - 계획에 `[사용자 승인 필요]` 항목이나 기획상 결정할 것이 있으면, 사용자에게 묻고 나서 시작한다.
3. **확인**: `[선행: pleos-docs-researcher]` 표시가 있으면 구현 전에 `pleos-docs-researcher`로 확인한다.
4. **구현**:
   - 계획의 단계를 하나씩 진행하고, 단위가 끝날 때마다 묻지 않고 커밋한다 ([docs/conventions/git.md](docs/conventions/git.md)).
   - 계획의 체크박스와 진행 로그를 갱신한다.
5. **테스트**: `tester` 서브에이전트에게 맡긴다.
   - 넘길 것: 계획 파일 경로와 대상만. 구현 설명은 넘기지 않는다. 구현에 끌려가지 않은 테스트를 받기 위해서다.
   - 버그 보고가 오면 내가 고친다.
   - 테스트는 `test:` 커밋으로 낸다.
6. **화면 확인**: UI가 바뀌었으면 `/verify`로 확인한다.
7. **마무리**:
   - `docs/notes/<git user.name>/`에 개발 노트를 쓴다.
   - 계획 파일을 `docs/plans/completed/`로 옮긴다.
   - 둘 다 커밋한다.
   - `/ship`으로 dev에 반영한다. push 단계에서 뜨는 권한 확인 창에서 사용자가 승인한다.
   - main은 건드리지 않는다. 사용자가 요청할 때만 `/promote`로 올린다.

### 도구

- **스킬**
  - `/verify`: 빌드 → 설치 → 스크린샷으로 화면 확인
  - `/release-apk`: 제출용 APK 생성
  - `/ship`: 작업 브랜치를 dev에 반영한다 (rebase → 검증 파이프라인 → `--no-ff` merge → push)
  - `/promote`: 검증된 dev를 main에 그대로 올린다. 사용자가 직접 실행할 때만 동작한다 (제출·PT 전)
- **서브에이전트** (`.claude/agents/`)
  - `planner`: 실행 계획을 커밋 단위로 써서 `docs/plans/active/`에 둔다
  - `tester`: 계약 기준으로 단위 테스트를 쓰고 실행한다. 프로덕션 코드는 고치지 않는다
  - `pleos-docs-researcher`: Pleos 공식 문서를 조사하고 `docs/pleos/`에 기록
  - `contest-judge`: 심사위원 관점의 냉정한 평가
  - 서브에이전트는 다른 서브에이전트를 부르지 못한다. 서로 넘기는 일은 메인이 한다.
- **훅** (`.claude/settings.json`)
  - SessionStart:
    - 마감 D-day와 진행 중인 계획을 보여준다.
    - main·dev에서 시작하면 작업 브랜치를 따라고 알린다.
    - git 훅(`core.hooksPath=.githooks`)이 꺼져 있으면 켠다.
  - `build-gate.mjs`: 앱 코드가 빌드되지 않는 상태로 커밋되거나 턴이 끝나지 않게 막는다.
    - 커밋 직전 (PreToolUse):
      - 커밋 메시지나 PR 본문에 Claude 트레일러가 있으면 막는다. 트레일러 자동 추가는 `attribution` 설정으로 꺼져 있다.
      - main·dev에 직접 커밋하면 막는다.
      - 앱 코드가 들어가는 커밋은 `assembleDebug`가 통과해야 진행된다.
    - 턴 종료 (Stop): 이번 턴에 앱 코드가 바뀌었으면 `assembleDebug`를 돌린다. 실패하면 멈추지 않고 이어서 고친다.
- **git 훅** (`.githooks/`)
  - `pre-push`: dev·main에 push하기 전에 검사한다. 사람이 터미널에서 push해도 걸린다.
    - 검증 파이프라인을 통과했는지 확인한다.
    - main은 `/promote`(`PROMOTE_MAIN=1`)로만, dev에 있는 커밋만 받는다.
    - main·dev 삭제는 막는다.
- **권한**
  - `git add`, `git commit`, `git switch`는 묻지 않는다.
  - push는 매번 묻는다. 이것이 `/ship`의 최종 확인이다. force push는 막혀 있다.
- 개인 설정은 `.claude/settings.local.json`에 둔다 (커밋되지 않음).
