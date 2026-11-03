@AGENTS.md

## Claude Code 전용

### 작업 흐름

기능 하나를 만들 때 이 순서를 따른다. 기획은 아래 "기획 단계: BMAD"를 따른다. 1~2단계짜리 작은 수정은 계획(2)과 `tester`(5)를 건너뛰어도 된다. 대신 테스트는 직접 추가한다.

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

### 기획 단계: BMAD (v6.12.4)

기획(아이디어 → 기획서 DOCX)은 BMAD 스킬로 한다. 개발은 위의 작업 흐름을 따른다.
기획 담당 팀원용 안내서: [docs/product/bmad-guide.md](docs/product/bmad-guide.md)

쓰는 스킬 (대략 이 순서):

1. `bmad-brainstorming`, `bmad-forge-idea`: 아이디어를 내고, 약점을 찔러 본다.
2. `bmad-deep-recon`: 시장과 경쟁 서비스를 조사한다. 사업성의 근거가 된다.
3. `bmad-product-brief` 또는 `bmad-prfaq`: 제품 개요를 쓴다.
4. `bmad-prd`: 요구사항을 정리한다.
5. `bmad-ux`: 주행 중·정차 중 화면 경험을 설계한다 (AGENTS.md 규칙 3).
6. `bmad-architecture`: 기술 구조를 정리한다. 기획서 5번 "개발 계획"에 쓴다.

언제든 쓸 수 있는 것:

- `bmad-help`: 다음에 할 일을 알려 준다.
- `bmad-party-mode`: 여러 관점으로 토론한다.
- `bmad-advanced-elicitation`: 결과물을 비판적으로 다시 본다.
- 에이전트: Mary(분석가), John(PM), Sally(UX), Winston(아키텍트)

규칙:

- **산출물 위치**: `docs/product/bmad/planning-artifacts/`에 쌓인다.
  - 확정된 내용은 `docs/product/idea.md`로 옮긴다. 기획서 DOCX는 idea.md를 원천으로 만든다.
  - 기획서 점검은 `contest-judge`로 한다.
- **개발 단계 스킬은 숨겼다** (`.claude/settings.json`의 `skillOverrides`).
  - 숨긴 것: build, dev, sprint, epics/stories, code-review, retrospective 등
  - `bmad-help`가 이 스킬들을 추천해도 쓰지 않는다. 개발은 위의 작업 흐름(planner → 구현 → tester → /ship)으로 한다.
  - `bmad-project-context`도 숨겼다. AGENTS.md를 고치는 스킬이기 때문이다.
- **Pleos SDK 내용은 BMAD 산출물에도 추측으로 쓰지 않는다** (AGENTS.md 규칙 1).
  - 기능이 어떤 SDK에 기대는지는 `docs/pleos/sdk-notes.md`의 "사용 가능 상태"로 확인한다.
- **개인 이름 설정**: `_bmad/custom/config.user.toml`에 `[core]` `user_name = "이름"`을 쓴다. 이 파일은 gitignore 대상이다.
  - 팀 공통 설정(한국어)은 `_bmad/custom/config.toml`에 있다.
- **업데이트할 때**: `npx bmad-method@<버전> install --action update --tools claude-code --yes`
  - 업데이트 후에는 새로 생긴 개발 단계 스킬이 없는지 보고, `skillOverrides`에 추가한다.

### 도구

- **스킬**
  - `/verify`: 빌드 → 설치 → 스크린샷으로 화면 확인
  - `/release-apk`: 제출용 APK 생성
  - `/ship`: 작업 브랜치를 dev에 반영한다 (rebase → 검증 파이프라인 → `--no-ff` merge → push)
  - `/promote`: 검증된 dev를 main에 그대로 올린다. 사용자가 직접 실행할 때만 동작한다 (제출·PT 전)
  - `/stack-diagram`: 기술 스택·아키텍처 다이어그램을 공식 로고와 함께 HTML/PNG로 만든다.
    - 외부 스킬이다: caru-ini/stack-diagram@9e90a3b, MIT. 스크립트는 2026-11-03에 검토했다.
    - **공개 갤러리 공유(SKILL.md 7단계, `share.py submit`)는 하지 않는다. 제안도 하지 않는다.** 출품작 구조는 아직 공개 전이다.
    - Windows에서는 스킬 문서의 `python3` 대신 `python`을 쓴다. `python3`는 Microsoft Store 바로가기라서 동작하지 않는다.
    - 기획서·PT에 쓸 결과물은 `docs/product/diagrams/`에 둔다. HTML과 PNG(라이트/다크)를 같이 둔다.
    - 필요한 것: Python 3.9 이상, `pip install playwright && python -m playwright install chromium`
    - 업데이트 방법: 원본 저장소의 `skills/stack-diagram`을 이 폴더에 덮어쓰고, 위의 커밋 해시를 고친다. 그 전에 스크립트를 다시 검토한다.
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
