# stack-diagram 스킬 도입

- 날짜: 2026-11-03
- 작성: xxj15
- 브랜치: chore/stack-diagram-skill
- 계획: 없음 (외부 스킬 설치)
- 출처: https://github.com/caru-ini/stack-diagram @ 9e90a3b (MIT)

## 무엇을 했나

기술 스택을 말로 설명하면 공식 로고가 들어간 아키텍처 다이어그램을 만들어 주는 스킬을 프로젝트에 넣었다. 출력은 HTML, PNG(라이트/다크), React 컴포넌트다. 기획서 "개발 계획"과 PT 자료에 쓰려는 것이다.

## 어떻게: 핵심 결정과 이유

- **프로젝트에 설치했다** (`.claude/skills/stack-diagram/`). 개인 폴더에 두지 않은 이유는 팀원 모두 같은 버전을 쓰게 하기 위해서다.
- **설치 방법**: `npx skills add` 대신 원본 저장소의 `skills/stack-diagram` 폴더를 그대로 복사했다.
  - `npx skills add`는 대화형으로 물어봐서 이 환경에서 쓰기 어렵다.
  - 원본과 파일이 같은지 `diff`로 확인했다.
- **설치 전에 스크립트 4개를 검토했다.**
  - `fetch_icons.py`: 로고 파일만 받는다. 출처는 svgl, devicon, jsdelivr, AWS, 직접 지정한 URL이다.
  - `build.py`: 로컬 파일만 HTML에 넣는다.
  - `render_png.py`: 로컬 HTML을 headless Chromium으로 스크린샷한다.
  - `share.py`: 공개 갤러리에 업로드한다. `submit`을 직접 실행할 때만 동작한다.
  - 템플릿 HTML에는 스크립트가 없다.
- **공개 갤러리 공유는 하지 않는다.** CLAUDE.md에 규칙으로 적었다.
  - 스킬은 그린 뒤 공유를 한 번 제안하게 돼 있다.
  - 하지만 출품작 구조는 공개 전이라 제안 자체를 하지 않게 했다.
- **Windows에서는 `python3` 대신 `python`을 쓴다.**
  - `python3`는 Microsoft Store 바로가기라서 동작하지 않는다.

## 사용한 Pleos SDK / API / 권한

없음.

## 확인

- Playwright 1.64.1과 Chromium을 설치했다.
- 실행 확인: Kotlin, Android, GitHub Actions 로고 받기 → HTML 빌드 → PNG 라이트/다크 렌더링까지 해 봤다.
  - 로고, 그룹 박스, 화살표가 정상으로 나왔다.
  - 시험 결과물은 임시 폴더에만 만들었고, 저장소에는 넣지 않았다.

## 주의점 / 남은 일

- 팀원도 `/stack-diagram`을 쓰려면 Playwright를 설치해야 한다 (README "처음 한 번" 5번).
- Pleos 로고는 svgl, devicon에 없을 가능성이 높다. 이때는 공식 로고 URL을 `pleos=URL` 형식으로 직접 넘긴다.
- 스킬을 업데이트할 때는 스크립트를 다시 검토하고, CLAUDE.md의 커밋 해시를 고친다.
