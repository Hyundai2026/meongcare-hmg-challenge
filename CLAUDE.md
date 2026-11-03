@AGENTS.md

## Claude Code 전용

- **스킬**
  - `/verify`: 빌드 → 설치 → 스크린샷으로 화면 확인
  - `/release-apk`: 제출용 APK 생성
- **서브에이전트**
  - `pleos-docs-researcher`: Pleos 공식 문서를 조사하고 `docs/pleos/`에 기록
  - `contest-judge`: 심사위원 관점의 냉정한 평가
- **훅** (`.claude/settings.json`)
  - SessionStart: 마감 D-day와 진행 중인 계획을 보여준다.
  - Stop: 코드가 바뀌었으면 `assembleDebug`를 돌린다. 실패하면 멈추지 않고 이어서 고친다.
- 개인 설정은 `.claude/settings.local.json`에 둔다 (커밋되지 않음).
