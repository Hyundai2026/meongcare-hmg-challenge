# BMAD 도입 (기획 단계용)

- 날짜: 2026-11-03
- 작성: xxj15
- 브랜치: chore/bmad-planning
- 계획: 없음 (도구 설치)
- 출처: https://github.com/bmad-code-org/BMAD-METHOD v6.12.4 (MIT)

## 무엇을 했나

BMAD Method v6.12.4를 설치했다.

- 기획 단계(아이디어 → 제품 개요 → PRD → UX → 아키텍처)에만 쓴다.
- 결과물은 `docs/product/bmad/`에 쌓이고, 기획서 DOCX의 원천인 `docs/product/idea.md`로 옮긴다.

## 어떻게: 핵심 결정과 이유

- **기획 단계만 쓴다** (사용자 결정).
  - BMAD의 개발 흐름(스토리 → 구현 → 리뷰)은 우리 하네스(planner → 구현 → tester → /ship)와 겹친다.
  - 두 흐름이 섞이면 헷갈린다. 기획서가 가장 급하기도 하다.
- **v6.12 안정판을 쓴다** (사용자 결정).
  - v7은 알파 단계라 업데이트마다 깨지고, 정식 출시(11월 중순)가 마감 기간과 겹친다.
- **개발 단계 스킬 11개를 `skillOverrides: off`로 숨겼다.**
  - 이유: `bmad-build`는 설명상 "기능 개발을 맡기면" 자동으로 실행될 수 있어, 우리 흐름과 부딪힌다.
  - 스킬 파일은 지우지 않고 설정으로만 숨겼다. 그래야 BMAD를 업데이트해도 설정이 유지된다.
  - `bmad-project-context`도 숨겼다. AGENTS.md 블록을 고치는 스킬이다.
  - `bmad-customize`는 사용자가 직접 부를 때만 동작하게 했다.
- **설정 파일을 팀 공통과 개인으로 나눴다.**
  - 팀 공통(한국어 대화·문서)은 `_bmad/custom/config.toml`에 두고 커밋한다.
  - 개인 답변(`_bmad/config.user.toml`, 이름 포함)은 gitignore했다.
  - 개인 설정이 없는 팀원도 한국어로 동작하는 것을 확인했다.
- **설치는 비대화형으로 했다.** 설치 명령:

  ```
  npx bmad-method@6.12.4 install --modules bmm --tools claude-code --yes --no-shims --communication-language Korean --document-output-language Korean --output-folder docs/product/bmad
  ```

## 사용한 Pleos SDK / API / 권한

없음.

## 확인

- 기존 파일은 하나도 바뀌지 않았다. `git status`에서 수정·삭제가 없었다. 새로 생긴 것:
  - `_bmad/`
  - `.claude/skills/bmad-*` 29개
- 외부 호출 검토: 스크립트는 로컬 설정 해석과 git 조회만 한다. 외부로 업로드하는 코드는 없다. 웹 조사는 에이전트의 검색 도구로 한다.
- 설정 해석 확인: `uv run _bmad/scripts/resolve_config.py --key core`
  - 내 PC: 이름 xxj15, 한국어
  - 개인 설정이 없는 경우: 기본 이름 BMad, 한국어
- 아직 확인하지 못한 것:
  - `skillOverrides`가 실제로 스킬을 숨기는지는 새 세션에서 봐야 한다.
  - BMAD 워크플로를 처음부터 끝까지 돌려 보지는 않았다.

## 주의점 / 남은 일

- `bmad-help`는 개발 단계 스킬을 추천할 수 있다. 무시하고 우리 흐름을 따른다 (CLAUDE.md에 적어 둠).
- BMAD를 업데이트하면 새 개발 단계 스킬이 생길 수 있다. 업데이트 후 `skillOverrides`를 다시 확인한다.
- 팀원은 uv를 설치해야 BMAD 스크립트가 돈다 (README "처음 한 번" 6번).
