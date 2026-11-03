# dev 통합 브랜치 + main 보호

- 날짜: 2026-11-03
- 작성: xxj15
- 브랜치: chore/dev-branch-flow
- 계획: docs/plans/completed/003-dev-branch-flow.md
- 커밋: 5bb0155..(이 노트까지)

## 무엇을 했나

평소 작업은 dev에 모으고, main은 제출·시연할 때만 바뀌게 했다.

- 작업 브랜치(`feat/*`)는 dev에서 따고, `/ship`으로 dev에 반영한다.
- main은 사용자가 `/promote`를 실행할 때만, 검증된 dev 상태 그대로 올라간다.

## 어떻게: 핵심 결정과 이유

- **main = 심사위원이 보는 제출본, dev = 통합 브랜치.**
  - GitHub 기본 브랜치는 main으로 둔다. 심사위원은 저장소에 들어오면 기본 브랜치의 소스와 `apk/`를 본다.
  - 그래서 main에는 개발 중인 코드가 섞이면 안 된다.
- **main은 dev를 fast-forward로만 따라간다.**
  - main에 dev에 없는 커밋이 생기지 않는다.
  - 그래서 pre-push가 `merge-base --is-ancestor` 하나로 이 규칙을 검사할 수 있다.
- **main push 허가는 `PROMOTE_MAIN=1` 환경 변수로 한다.**
  - git pre-push 훅에는 push 옵션이 전달되지 않는다.
  - `/promote`가 이 변수를 붙여서 push한다. 사람도 같은 방법으로 할 수 있다.
- **`/promote`는 사용자만 실행할 수 있다** (`disable-model-invocation`).
  - "main은 요청할 때만"을 Claude가 스스로 어기지 못하게 하기 위해서다.
- **dev에도 직접 커밋하지 않는다.**
  - 기능 브랜치 → dev 흐름을 유지하면, 여러 팀원이 동시에 작업해도 서로 덜 꼬인다.
- **main·dev 브랜치 삭제는 pre-push에서 막는다.**
  - GitHub 브랜치 보호의 일부 기능은 비공개 저장소에서 유료일 수 있다. 그래서 로컬 훅에서도 막아 둔다.

## 사용한 Pleos SDK / API / 권한

없음.

## 확인

- dev/main 흐름 시나리오 10개를 bare 원격에 실제 `git push`로 돌려 모두 기대대로 동작했다.
  - dev push는 파이프라인을 거쳐 통과
  - `PROMOTE_MAIN` 없는 main push 차단
  - `/promote` 방식 push 통과 (파이프라인 재실행 없음)
  - main·dev 직접 커밋 차단
  - `/ship` 방식 반영
  - dev에 없는 커밋의 main push 차단
  - main·dev 삭제 차단
  - 파이프라인 실패 시 dev push 차단
  - SessionStart 알림
- 기존 빌드 게이트 시나리오 16개와 트레일러 차단 8개를 다시 돌려 모두 통과했다.
- 실제 저장소에는 아직 `gradlew`가 없다. 그래서 진짜 Gradle과 함께 돌려보지는 못했다.

## 주의점 / 남은 일

- 팀원이 처음 clone하면 main만 받는다. `git switch dev`로 dev를 만들고 거기서 작업 브랜치를 딴다.
- 이번 변경은 `/ship`으로 dev에만 들어간다.
  - main에는 여전히 예전 규칙(main에 직접 반영하는 흐름)이 남아 있다.
  - 팀원이 main을 clone해도 새 규칙을 받게 하려면 `/promote`가 한 번 필요하다.
- GitHub 설정: `main`과 `dev`에 "Block force pushes"(+ "Restrict deletions")를 켠다. "Require a pull request"는 켜지 않는다.
