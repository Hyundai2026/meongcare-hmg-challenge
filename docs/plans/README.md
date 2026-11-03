# 실행 계획

여러 단계(대략 3단계 이상)이거나 여러 세션에 걸치는 작업은 코드를 쓰기 전에 계획 파일부터 만든다.
에이전트가 세션을 새로 시작해도 이 파일만 읽으면 이어서 작업할 수 있어야 한다.

- 진행 중: `docs/plans/active/NNN-짧은-이름.md`
  - 이름은 브랜치 이름과 맞춘다. 예: `feat/vehicle-speed-flow` → `002-vehicle-speed-flow.md`
- 완료되면 `docs/plans/completed/`로 옮기고, 개발 노트(`docs/notes/`)를 쓴다.
- Claude Code에서는 `planner` 서브에이전트가 이 템플릿으로 쓴다.

## 템플릿

```markdown
# NNN 제목

- 목표: (한 문장)
- 완료 기준 (어떻게 검증하나): (명령, 화면, 테스트 이름)
- 브랜치: feat/<이름>
- 관련 문서:

## 확인 필요 (시작 전에 해결)

- [사용자 승인 필요] 새 pleos.car.permission.* 권한, 기획 결정
- [선행: pleos-docs-researcher] sdk-notes.md에 없는 Pleos API

## 단계

단계 하나 = 커밋 1~3개. 예상 커밋 메시지를 적는다.

- [ ] 1. ... → `feat(scope): ...`
- [ ] N. 개발 노트 작성 → `docs(notes): ...`

## 진행 로그

- YYYY-MM-DD:

## 결정 / 배운 점
```
