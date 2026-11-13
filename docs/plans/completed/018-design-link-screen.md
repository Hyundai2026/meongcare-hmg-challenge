# 018 연결 화면 디자인 적용 (프로토타입 1번 화면)

- 목표: 확정된 디자인 프로토타입의 1번 화면("차량 연결")을 차 앱의 보호자 웹 연결 화면(`ui/link/LinkScreen.kt`, 014)에 입힌다. 기능·상태·문구 의미는 그대로 두고, 다음 화면들이 같이 쓸 디자인 토큰 기반을 놓는다.
- 완료 기준 (어떻게 검증하나):
  1. `node .claude/hooks/build-gate.mjs pipeline`이 "검증 파이프라인 통과."를 출력한다.
  2. 단위 테스트:
     - 새 테스트 `LinkProgressTest`(단계 2)와 tester가 추가한 경계 테스트가 통과한다.
     - 기존 테스트(시작할 때 개수를 진행 로그에 적는다)가 그대로 통과한다.
     - `git diff dev --diff-filter=MD --name-only -- app/src/test`의 출력이 비어 있다. 기존 테스트 파일은 고치지 않고, 새 파일만 더한다.
  3. 기능을 건드리지 않았다는 확인. 아래 `git diff dev -- <경로>`의 출력이 비어 있다. 결정 D1·D3에서 다른 안을 고르면 그 파일만 예외로 진행 로그에 적는다.
     - `app/src/main/java/com/meongcare/app/ui/link/LinkViewModel.kt`, `ui/link/LinkUiState.kt`
     - `app/src/main/java/com/meongcare/app/domain/`, `app/src/main/java/com/meongcare/app/data/`, `di/`, `MainActivity.kt`
     - `ui/theme/Color.kt`, `ui/theme/Type.kt`, `ui/theme/Dimens.kt`
     - `ui/` 아래에서 `ui/link/LinkScreen.kt`, `ui/link/LinkProgress.kt`(새 파일), `ui/theme/DesignTokens.kt`(새 파일), `ui/theme/Theme.kt`를 뺀 나머지
     - `app/src/main/res/values/strings.xml`의 `link_*` 줄. 새 문자열을 더하는 것은 괜찮다.
  4. `ui/theme/Theme.kt`의 diff는 디자인 토큰을 내려 주는 `CompositionLocalProvider`를 더한 것뿐이다. `LightColors`·`DarkColors`·`MeongCareTypography`는 그대로다. 다른 화면의 색과 글자는 바뀌지 않는다.
  5. 화면 확인: `/verify`. 프로토타입 캡처와 나란히 놓고 비교한 결과를 진행 로그에 적는다.
     - 프로토타입 캡처 방법: `python`과 playwright로 `docs/product/design/prototype/MeongCare car.html`을 1682×1210 크기로 열어 1번 화면을 스크린샷한다. 결과는 scratchpad에 두고 커밋하지 않는다.
     - Pleos 태블릿 에뮬레이터(넓은 화면, 라이트·다크)에서 확인할 것:
       - 대기: 왼쪽 정렬, 코드 크기와 색(selt), 진행 막대 640×4, 남은 시간 줄, 버튼 줄([새 코드] sf2 바탕 + [닫기] 글자 버튼)
       - 연결됨: 초록 체크와 "연결 완료" 줄, 메시지, 반려견 줄, [확인] 480×96
       - 만료: 에러색 문구, [새 코드]가 pr 바탕으로 바뀜
       - 만료는 10분을 기다려야 볼 수 있다. Fake로 빨리 볼 방법이 없으면 기다리거나, 못 봤다고 진행 로그와 보고에 적는다. 실패 상태도 같다.
     - 일반 폰 에뮬레이터(`my_avd`, 좁은 화면 세로): 대기·연결됨에서 글자가 잘리거나 겹치지 않고, 코드가 한 줄에 보이고, 버튼이 화면 밖으로 나가지 않는다.
     - 주행 중: debug 패널에서 Fake 주행으로 바꾸면 연결 화면 대신 주행 화면이 나온다. 지금 동작(`homeContentOf`)이 그대로인지만 본다.
     - `adb logcat -d -t 500 *:E`에 `com.meongcare.app` 크래시가 없다.
  6. 문서: `docs/architecture.md`에 디자인 토큰 위치(`ui/theme/DesignTokens.kt`), 원천(프로토타입 TH 표), 적용 범위(지금은 연결 화면만)가 적혀 있다.
- 브랜치: `feat/design-link-screen` (최신 dev `b7b6c20`에서 땄다)
- 관련 문서:
  - AGENTS.md 규칙 3(주행 중 화면: 정차 중에만, 글자 입력 없음), 규칙 6(`/verify`), 규칙 7
  - 디자인 원천: `docs/product/design/prototype/MeongCare car.html`. 디자인 도구가 묶어 내보낸 HTML이고 화면은 JS로 그린다. 토큰은 JS의 `TH` 표에 있다.
  - `docs/pleos/ux-guidelines.md` 387행: 폰트 Asta Sans가 Pleos에 System UI 폰트로 설치되어 있다 [design-typography]
  - 지금 화면: `ui/link/LinkScreen.kt`, `ui/link/LinkUiState.kt`, `ui/link/LinkViewModel.kt`, `ui/card/PetVisuals.kt`(`PetAvatar(initial, size)`), `ui/theme/*`
  - 선례: `docs/plans/completed/014-owner-link.md`

## 프로토타입 1번 화면 사양 (실제 스타일 코드에서 뽑음. px ≈ dp, 기준 1682×1210 = Wide)

- 헤더: 왼쪽 로고(앱 아이콘 모양) + 제목.
- 본문: 세로 가운데, 왼쪽 정렬. 왼쪽 여백 104, 아래 여백 80. 그림자 없음.
- 대기:
  - 안내문: 34/400, 줄간 1.4, sub색. 아래 간격 32.
  - 코드: 160/500, 자간 .1em, 줄간 1, 숫자 tabular, selt색. 만료되면 sub색 + 취소선 + 투명도 .5.
  - 진행 막대: 위 간격 48, 폭 640, 높이 4, 둥글기 2. 바탕 ln, 채움 ac(#3E5DB4). 채움 길이 = 남은 비율.
  - 진행 막대 아래 16: 30/400 sub색 "남은 시간 " + mm:ss(500, tx색, tabular).
    - 생성 중이면 "연결 코드 생성 중"
    - 만료면 er색 "연결 코드 만료 · 새 코드를 발급하세요."
    - 실패면 er색 오류 문구
  - 버튼 줄: 위 간격 64, 버튼 사이 24.
    - 주 버튼: 높이 96, 좌우 48, 둥글기 16, 34/600. 평소에는 sf2 바탕 + tx 글자, 만료면 pr 바탕 + onpr 글자.
    - 글자 버튼: 바탕 없음, selt 글자, 34/500, 높이 96, 좌우 32.
- 연결됨: max-width 1040.
  - 초록 체크 36 + "연결 완료"(30/500, ok색)
  - 아래 16: 메시지 48/500, 줄간 1.3
  - 아래 48: 반려동물 줄. 위·아래 2px 구분선, 위아래 패딩 32, 간격 32. 사진 96 원형, 이름 40/600 + 보조 30. 오른쪽에 "지병 · …"/"복용약 · …" 30 sub색.
  - 위 64: [시작] 폭 480, 높이 96, 둥글기 16, pr 바탕 + onpr 글자, 34/600.
- 토큰 (TH 표):
  - light: pg #E4E7EC, bg #FFFFFF, sf #F4F5F7, sf2 #ECEEF2, ln #E8EBF0, tx #1B2231, sub #657085, pr #3E5DB4, onpr #FFFFFF, sel #EEF2FA, selt #2F4A99, ok #1B7A4B, okb #E6F4EC, warn #8A5A00, warnb #FFF3DC, er #B42318, erb #FDEDEC, dis #EEF0F3, dist #8A93A3, hbg #F8F9FB
  - dark: bg #111317, sf #1A1D22, sf2 #24282F, ln #2A2F37, tx #E8EAEE, sub #A0A7B4, pr #8FA8EC, onpr #0E1422, sel #1C2538, selt #B9C8F3, ok #7FD1A4, okb #15291E, warn #E9C27A, warnb #2E2412, er #FF9B90, erb #3A1A17, dis #1E1F38, dist #7E80A0
    - dark에는 pg·hbg 값이 없다. 확인 필요 Q2.
  - 숫자 차이: 화면 사양의 진행 막대 바탕(ln)은 #E3E7EE, 연결됨의 ok는 #1D7348이다. TH 표의 값(#E8EBF0, #1B7A4B)과 다르다. 토큰은 TH 표 값을 쓴다. 확인 필요 Q3.
  - 글자 단계: 160/112/96(숫자), 64~72(큰 제목), 48~56(제목), 36~40/600(화면 제목), 32~34/500(본문 큰), 30/500(본문), 16~24(작은). 줄간 본문 1.4, 제목 1.2~1.3, 자간 -0.02em.
  - 간격: 4·8·12·16·24·32·40·48(+64·80·104). 둥글기: 16(기본)·12·24~28·원형·999. 버튼 높이: 96/80/48.

## 지금 앱과 다른 점 (조사 결과)

- `LinkUiState.Waiting(code, remainingMinutes)`: 남은 시간을 **분**으로만 준다. ViewModel은 `pollIntervalMillis`(2초)마다 한 번 내보낸다. mm:ss를 1초마다 바꾸려면 ViewModel에 1초 박자를 따로 두고 계약도 바꿔야 한다. 그러면 기존 테스트 7곳(`LinkViewModelTest` 75·124·146행, `LinkViewModelBoundaryTest` 109·143·249·299행의 `Waiting(code, N)` 비교)이 영향을 받는다.
- `Expired`·`Failed`에는 코드가 없다. 그래서 프로토타입처럼 "취소선 그은 코드"를 보일 수 없다.
- `LinkedPet(name, breed)`뿐이다. `PetProfile`에는 `photoDataUrl`, `conditions`, `medications`가 있지만, `LinkedPet`에 더하면 `LinkTextsTest`의 `LinkedPet("콩이", "말티즈")` 비교가 깨진다. 앱 어디에서도 사진을 아직 그리지 않는다. `PetAvatar(initial, size)`가 그 자리를 맡고 있다.
- 코드 띄어쓰기는 이미 `LinkCode.display`가 한다("K7P 3QX"). 새 함수는 필요 없다.
- 버튼 문구와 순서:
  - 지금: 대기·만료·실패 = [닫기] + [새 코드/다시 시도], 연결됨 = [확인], 생성 중 = [닫기]
  - 프로토타입: [새 코드 발급] + 글자 버튼("체험 모드 시작"/"닫기")
  - "체험 모드"는 앱에 없는 기능이다.
- 제목은 앱이 "보호자 웹과 연결"(`link_title`), 프로토타입이 "차량 연결"이다.
- 로고: 런처 아이콘은 `mipmap-anydpi/ic_launcher.xml`(적응형)이다. Compose `painterResource`로 적응형 아이콘을 읽으면 오류가 난다. 그래서 `drawable/ic_launcher_foreground.xml`을 둥근 바탕 위에 그린다.
- 연결 화면은 정차 중에만 보인다. 주행이면 `homeContentOf`가 주행 화면을 고른다. 이 동작은 바꾸지 않는다.

## 확인 필요 (시작 전에 해결)

결정 D1~D5는 기본안대로 진행해도 기능이 바뀌지 않는다. 사용자가 다른 안을 고를 때만 계획을 고친다.

- **D1 [사용자 승인 필요] 남은 시간 표시**
  - 기본안 A: ViewModel 계약은 그대로 둔다. 진행 막대는 `remainingMinutes / 10`으로 그린다(10칸으로 줄어든다). 막대 아래 줄은 지금 문구 "N분 안에 입력해 주세요"(`link_remaining`)를 프로토타입 위치와 스타일로 보인다. 기존 테스트 영향 없음.
  - 안 B: `Waiting`에 `remainingSeconds`를 더하고, ViewModel에 1초 박자를 둔다(폴링 2초와 따로). 화면은 "남은 시간 mm:ss"가 되고 막대도 부드럽게 줄어든다. 단점: ViewModel 계약이 바뀐다. 위 7곳의 기존 테스트를 고쳐야 하고, 1초 박자 테스트를 새로 써야 한다. D-2에 위험이 크다.
- **D2 [사용자 승인 필요] 폰트**
  - 기본안: 폰트 파일을 넣지 않는다. 지금처럼 시스템 기본 폰트를 쓴다. ux-guidelines에 따르면 Pleos에는 Asta Sans가 System UI 폰트로 깔려 있으므로, 차에서는 Asta Sans로 보일 것으로 본다. 에뮬레이터에서 눈으로 확인하고 진행 로그에 적는다. 일반 폰은 Roboto/Noto로 보인다.
  - 안: Google Fonts에서 Asta Sans 원본 TTF와 OFL 라이선스를 받아 `res/font`에 넣는다. 네트워크가 필요하고 APK 용량이 는다. 프로토타입 안의 woff2는 유니코드 범위로 잘린 조각 약 100개라 쓰지 않는다.
- **D3 [사용자 승인 필요] 연결됨의 반려동물 줄**
  - 기본안: 지금 있는 정보만 쓴다. 왼쪽에 `PetAvatar`(이름 첫 글자, 96 원형), 이름 40/600, 품종 30(sub색).
    - 사진·지병·복용약 칸은 이번에 넣지 않는다. 받으려면 `LinkedPet`을 바꿔야 하고, 그러면 기존 테스트가 깨진다.
    - 반려견 정보를 받지 못한 경우(`pet == null`)에는 반려동물 줄 없이 "연결 완료" + 메시지만 보인다.
  - 안: `LinkedPet`에 사진·지병·복용약 요약을 더한다. `LinkUiState.kt`가 바뀌고 `LinkTextsTest`를 고쳐야 한다.
- **D4 문구** (기획 결정)
  - 기본안: `strings.xml`의 `link_*`를 그대로 쓴다. 제목 "보호자 웹과 연결", 버튼 "새 코드"/"다시 시도"/"닫기"/"확인".
  - 새로 더하는 것은 연결됨의 "연결 완료" 줄 하나뿐이다(`link_linked_badge` 등). 문구를 프로토타입("차량 연결", "새 코드 발급", "시작")으로 바꿀지는 사용자가 정한다.
- **D5 버튼 배치** (기획 결정)
  - 기본안: 프로토타입 순서를 따른다. "체험 모드 시작"은 넣지 않는다.
    - 대기: [새 코드](sf2) + [닫기](글자 버튼)
    - 만료: [새 코드](pr) + [닫기]
    - 실패: [다시 시도](pr) + [닫기]
    - 생성 중: [닫기](글자 버튼)만
    - 연결됨: [확인](pr, 480×96)
- **D6 토큰을 두는 곳** (기본안. 바꾸려면 알려 달라)
  - 별도 파일 `ui/theme/DesignTokens.kt`에 둔다: 색 light/dark, 글자 단계, 간격, 둥글기, 버튼 높이. `MeongCareTheme`이 `LocalDesignTokens`로 내려 준다.
  - 전역 `colorScheme`에는 넣지 않는다. 다른 화면이 바뀌지 않게 하려는 것이다. 지금은 연결 화면만 쓰고, 다음 화면들이 하나씩 옮겨 온다.
- **D7 좁은 화면 크기** (기본안)
  - 넓은 화면(840dp 이상)은 프로토타입 값을 그대로 쓴다.
  - 좁은 화면은 지금 값을 따른다. 코드 72sp(`LinkText.CodeNarrow`와 같음), 본문은 지금 `CardScale.Narrow` 크기, 왼쪽 여백 24, 진행 막대는 화면 폭에 맞춘다.
  - 버튼 줄이 넘치면 세로로 쌓는다.
- Q2 (디자인 확인): 다크 테마의 pg·hbg 값이 TH 표에 없다. 연결 화면은 bg만 쓰므로 이번에는 막히지 않는다. 다음 화면 전에 디자인 담당에게 묻는다.
- Q3 (디자인 확인): 화면 사양의 진행 막대 바탕 #E3E7EE와 ok #1D7348이 TH 표 값과 다르다. 기본안은 TH 표 값이다. 차이가 작아 눈으로 비교할 때는 문제가 되지 않을 것으로 본다.
- Pleos API, 새 권한: 없다. 이 작업은 Compose UI만 바꾼다.

## 단계

단계 하나 = 커밋 1~3개.

- [x] 1. 디자인 토큰 기반 → `feat(ui): 디자인 프로토타입 토큰 추가`
  - 새 파일 `ui/theme/DesignTokens.kt`
    - `DesignColors`: 위 TH 표의 light/dark 값. 다크에 없는 pg·hbg는 비워 두고 주석으로 Q2를 남긴다.
    - 글자 단계(`DesignType`), 간격(`DesignSpace`), 둥글기, 버튼 높이
    - `LocalDesignTokens`
  - `Theme.kt`: `MeongCareTheme`이 `darkTheme`에 맞는 토큰을 `CompositionLocalProvider`로 내려 준다. colorScheme·typography는 그대로다.
  - 아직 아무 화면도 쓰지 않으므로 화면은 바뀌지 않는다. `assembleDebug` 통과.
- [x] 2. 연결 화면에 디자인 적용 → 커밋 1~2개
  - `feat(ui): 연결 화면 대기 상태를 디자인 시안에 맞춤`
    - 새 파일 `ui/link/LinkProgress.kt`의 순수 함수 `linkProgressOf(remainingMinutes: Int): Float`: `remainingMinutes / 10`을 0~1로 자른다. 기준은 `LinkCode.VALIDITY`다.
    - 새 테스트 `LinkProgressTest`(같은 커밋): 10분이면 1, 5분이면 0.5, 0 이하면 0, 10 초과면 1
    - `LinkScreen.kt`: 흰 카드(`CardColors`, 그림자 2dp)를 걷어 낸다. 화면 바탕은 토큰 bg로 칠한다.
    - 헤더(로고 + `link_title`), 본문(세로 가운데, 왼쪽 정렬, 여백 104/80)
    - 대기·생성 중·만료·실패의 안내문, 코드, 진행 막대, 상태 줄, 버튼 줄. 위 사양과 D1·D5·D7 기본안을 따른다.
    - 코드에는 `fontFeatureSettings = "tnum"`과 자간 .1em을 준다. Monospace는 뺀다.
  - `feat(ui): 연결 화면 연결됨 상태를 디자인 시안에 맞춤`
    - 체크 + "연결 완료" 줄, 메시지(`link_linked_title`/`link_linked_no_profile_title`), 반려동물 줄(D3 기본안), [확인]
    - 새 문자열은 "연결 완료" 하나뿐이다.
  - Preview를 갱신한다: Wide 라이트·다크 × 대기·연결됨·만료, Narrow × 대기·연결됨.
  - 각 커밋마다 `assembleDebug` 통과.
- [x] 3. 테스트 (`tester`) → `test(ui): 연결 화면 진행 막대 비율 경계 테스트`
  - 넘길 것: 이 계획 파일 경로, 대상 `ui/link/LinkProgress.kt`, `ui/theme/DesignTokens.kt`
  - `testDebugUnitTest` 전체 통과. 완료 기준 2의 diff 출력이 비어 있는지 확인한다.
- [x] 4. 화면 확인 + 문서 → `docs(architecture): 디자인 토큰 위치와 적용 범위 기록`
  - 완료 기준 5의 `/verify` 절차를 한다. 비교 결과와 D2 폰트 관찰을 진행 로그에 적는다. 화면이 시안과 다르면 2단계로 돌아가 `fix(ui): ...`로 고친다.
  - `docs/architecture.md`에 기록한다: 토큰 파일, 원천(프로토타입 TH 표), 적용 범위, "다음 화면은 LocalDesignTokens로 옮긴다". 완료 기준 6.
  - 완료 기준 3·4의 `git diff dev` 확인 결과도 진행 로그에 적는다.
- [x] 5. 개발 노트 작성 (`docs/notes/CYJhub/2026-11-13-design-link-screen.md`) → `docs(notes): design-link-screen`
  - 계획 파일을 `docs/plans/completed/`로 옮기는 것도 같이 커밋한다.

## 진행 로그

- 2026-11-13: 계획 작성. 브랜치 `feat/design-link-screen`(dev `b7b6c20`). `docs/product/design/`은 아직 추적되지 않는 상태다. 프로토타입 HTML을 커밋할지는 사용자에게 묻는다.

- 2026-11-13: 사용자 결정 (CYJhub): "디자인은 프로토타입대로, 글꼴은 없으면 최대한 비슷한 것으로(다운로드로 시간 끌지 말 것), 체험 모드 시작은 뺀다."
  - D1 → **mm:ss + 진행 막대** (프로토타입). 단 `LinkUiState.Waiting`과 기존 테스트는 바꾸지 않는다: `LinkViewModel`에 새 `waitingSince: StateFlow<Instant?>`(Waiting이면 createdAt)를 더하고, 화면이 1초마다 순수 함수 `linkCountdownOf(createdAt, now)`로 "02:59"와 남은 비율을 계산한다. 기존 "N분 안에" 문구 자리는 "남은 시간 mm:ss"로 바뀐다.
  - D2 → 폰트 파일을 넣지 않는다 (Pleos는 Asta Sans가 시스템 UI 폰트, 이 PC 에뮬레이터는 기본 한글 글꼴).
  - D3 → 기본안 (아바타 첫 글자·이름·품종). D4 → 기본안 (지금 문구, 새 문자열 "연결 완료"·"남은 시간"). D5 → 기본안 (주 버튼 앞, [닫기] 글자 버튼, 체험 모드 없음).
  - 프로토타입 HTML(`docs/product/design/prototype/MeongCare car.html`, 3.3MB)을 디자인 원본으로 이 브랜치에 커밋한다.

- 2026-11-13: 단계 1 토큰 (`78b4397`), 단계 2 연결 화면 (`24d5657`). 태블릿 에뮬레이터(앱 영역 약 1280dp)에서 프로토타입 크기(1682 기준) 그대로는 넘쳐서, 화면 폭 비율(0.45~1.0)로 글자·간격·버튼을 같이 줄이게 고쳤다 (`e6dea05`). 차량 화면(1682)에서는 1.0.
- 2026-11-13: 에뮬레이터(시연 모드, 서버 있음)에서 대기 화면 확인: 왼쪽 정렬 안내문 → 큰 코드 → 진행 막대 → "남은 시간 09:53"(1초마다) → [새 코드] + 글자 버튼 [닫기]. 사용자 결정: **다크 모드·세로 화면 확인은 빼고 이 모습으로 마무리**, 다음 화면으로 넘어간다. 만료·실패·연결됨 상태는 화면으로 보지 않았다 (미리보기 코드만).

- 2026-11-13: tester 37개 (전체 1366). 버그 1개: 기기 언어가 페르시아어 등이면 "۰۲:۵۹" → `Locale.ROOT`로 고침. 완료 기준 예외: `MainActivity.kt`(waitingSince 수집·`rememberLinkCountdown`)와 `LinkViewModel.kt`는 D1 결정으로 바뀌었다. 파일 이름은 계획의 `LinkProgress`가 아니라 `LinkCountdown`이다.

## 결정 / 배운 점
