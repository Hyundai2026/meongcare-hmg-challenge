# 019 홈 화면 디자인 적용 (프로토타입 2번 "응급 진료") + 바로가기 3개

- 목표: 프로토타입 2번 홈 "응급 진료"를 정차 처음 화면(`ui/home/ParkedScreen.kt`)에 입히고, 오른쪽 바로가기 3개(병원 찾기·반려동물 카드·릴렉스(시연용))가 동작하게 한다. 시연용 샘플 반려견은 "보리" 그대로 둔다 (2026-11-13 사용자 재결정).
- 완료 기준 (어떻게 검증하나):
  1. `node .claude/hooks/build-gate.mjs pipeline`이 "검증 파이프라인 통과."를 출력한다 (Android + 웹. 웹은 바꾸지 않았으므로 그대로 통과해야 한다).
  2. 단위 테스트 (`./gradlew testDebugUnitTest`) 전체 통과. 시작할 때 개수를 진행 로그에 적는다.
     - 새 테스트가 통과한다: `DesignScaleTest`, `SamplePetProfilesTest`(또는 기존 `FakePetProfileSourceTest` 안의 새 케이스), `ParkedContentHomeFinderTest`, `HospitalTripHomeFinderTest`, `ParkedContentPageTest`, `PetCardContentTest`, `RelaxViewModelTest`, 그리고 tester가 더한 경계 테스트.
     - 고친 기존 테스트 파일은 샘플 이름 변경(단계 2) 때문인 것뿐이다. `git diff dev --diff-filter=MD --name-only -- app/src/test`의 출력이 단계 2 진행 로그에 적은 파일 목록 안에 있다. 특히 `ParkedContentFinderTest.kt`는 바뀌지 않는다 (기존 계약 "카드 없으면 찾기 단계여도 처음 화면"은 기본값에서 그대로다).
  3. 범위 확인. 아래 `git diff dev -- <경로>`의 출력이 비어 있다.
     - `web/`, `firebase/`, `docs/data/` (결정 D1 기본안. 다른 안을 고르면 진행 로그에 예외로 적는다)
     - `app/src/main/java/com/meongcare/app/domain/` 중 `domain/trip/HospitalTrip.kt`를 뺀 나머지
     - `app/src/main/java/com/meongcare/app/data/` 중 `data/pet/SamplePetProfiles.kt`, `data/pet/FakePetProfileSource.kt`(주석만)를 뺀 나머지
     - `app/src/main/AndroidManifest.xml` (새 권한 없음), `ui/home/DrivingScreen.kt`, `ui/home/HomeContent.kt`
  4. 화면 확인 (`/verify`): Pleos 태블릿 에뮬레이터, **시연 모드, 라이트, 가로만** (사용자 결정: 다크·세로·폰은 확인하지 않는다). 프로토타입 캡처(018과 같은 방법, 1682×1210, scratchpad에 두고 커밋하지 않음)와 나란히 본 결과를 진행 로그에 적는다.
     - 홈(카드 없음): 왼쪽 위 "MeongCare", 오른쪽 위 반려동물 줄(아바타 "보" + "보리" + 상태), 왼쪽 "응급 진료" + 두 줄 안내 + [응급 진료 시작 >], 오른쪽 세로선 + 바로가기 3개(140 정사각 아이콘 + 라벨), 아래 시연용 샘플 안내.
     - [응급 진료 시작] → 증상 고르기 화면 (지금 [진료 카드 만들기]와 같다).
     - [병원 찾기] → 병원 목록 → 병원 하나 → 확인 화면에 [카드 보내고 출발]이 없고 [길 안내 시작]·[병원에 전화]만 있다 → 목록 [뒤로] 두 번이면 홈.
     - [반려동물 카드] → 보리 정보(골든리트리버, 2014.08.01 출생, 심장질환, 심장약 1일 1회, 특정 단백질 알레르기, 체중 "미등록") → [←]이면 홈.
     - [릴렉스] → "시연용" 안내가 보이고, 큰 토글을 끄면 아래 항목이 흐려지고 눌리지 않는다 → [완료]이면 홈 → 다시 열면 고른 값이 남아 있다.
     - 카드를 만든 뒤 [홈]: 왼쪽에 "만든 진료 카드" 줄과 [카드 보기], [새로 만들기]가 있다.
     - 주행: debug 패널 [주행]이면 홈·반려동물 카드·릴렉스 어디서든 주행 화면으로 바뀐다. [정차]면 보던 화면으로 돌아온다.
     - `adb logcat -d -t 500 *:E`에 `com.meongcare.app` 크래시가 없다.
  5. 문서: `docs/architecture.md`에 `DesignScale` 위치, 홈 토큰, 새 화면(반려동물 카드·릴렉스)과 `parkedContentOf`의 새 인자가 적혀 있다. `docs/product/feature-facts.md`에 릴렉스가 "시연용 화면(차량에 적용하지 않음)"으로 적혀 있다.
- 브랜치: `feat/design-home-screen` (최신 dev `5f6a628`에서 땄다)
- 관련 문서:
  - 선례: `docs/plans/completed/018-design-link-screen.md` (토큰, 화면 폭 비율, "기능은 그대로 UI만")
  - 디자인 원천: `docs/product/design/prototype/MeongCare car.html` (번들된 한 줄 HTML. 화면은 JS가 그린다)
  - AGENTS.md 규칙 3(주행 중 화면), 규칙 6(`/verify`), 규칙 7
  - `docs/data/pet-profile.md` (프로필 형식. 체중·종·주치 병원 필드는 없다)
  - 지금 코드: `ui/home/ParkedScreen.kt`, `ui/home/ParkedContent.kt`, `ui/home/HomeScreen.kt`, `ui/home/LinkRow.kt`, `ui/hospital/HospitalConfirmScreen.kt`, `domain/trip/HospitalTrip.kt`, `ui/link/LinkScreen.kt`(`LinkMetrics`), `ui/theme/DesignTokens.kt`, `ui/card/PetVisuals.kt`, `ui/card/CardTexts.kt`, `MainActivity.kt`

## 프로토타입 2번 홈 사양 (기준 앱 영역 1682×1210, px ≈ dp)

- 홈 배경 hbg. 2열 그리드 55:45, 세로 가운데, 아래 여백 72.
- 왼쪽(좌 112, 우 80): "응급 진료" 104/700, 자간 -0.03em, 줄간 1.08, htx. 위 28: 36/400 줄간 1.4 hsub "증상을 알려주시면 병원에 바로 연결할게요." + 둘째 줄 htx 500 "걱정 말고 출발하세요." 위 48: [응급 진료 시작 >] 폭 100%·최대 640, 높이 96, 좌 40·우 36, 둥글기 16, hpr 바탕, honpr 30/600, 글자와 오른쪽 화살표가 양끝.
- 오른쪽(왼쪽 2px 세로선 hln, 패딩 40 0 40 56): 3열 200 폭, 간격 24. 각 버튼 = 140×140 둥글기 28 hic 바탕 안에 48 외곽선 아이콘(hpr) + 아래 20 간격 30/500 라벨.
- 위쪽: 왼쪽 "MeongCare" 로고 글자, 오른쪽 반려동물(사진+이름+"● 연결됨") + [⇄ 변경] + [음성 명령]. 아래: 음성 힌트.
- 토큰: hpr #3E5DB4, hprh #33509F, htx #222938, hsub #697386, hic #F1F3F7, hln #E8EBF1, hbg #F8F9FB, honpr #FFFFFF. 다크는 일반 다크 토큰.
  - 지금 `DesignColors`에 있는 것: `homeBackground`(hbg), `primary`(= hpr 값), `onPrimary`(= honpr 값).
  - 없는 것: htx·hsub·hic·hln·hprh (htx·hsub·hln은 일반 tx·sub·ln과 값이 조금 다르다). 단계 1에서 `homeText`, `homeSubText`, `homeIcon`, `homeLine`, `homePrimaryPressed`로 더한다. 다크는 text, subText, surface2, line, primary 값을 쓴다.

## 지금 앱과 다른 점 (조사 결과)

- **샘플 반려견과 예시 JSON**: `docs/data/pet-profile.example.json`은 웹 테스트가 직접 읽는다 (`web/src/domain/petProfileJson.test.ts:31`이 자기 상수 `bori`(보리, 골든리트리버)와 같은지 본다. 그 밖에 `ownerBanner.test.ts`, `migration*.test.ts`, `ownerProfileDoc*.test.ts`, `profileScreenReducer.network.boundary.test.ts`, `web/scripts/verify_screens.py`). 그래서 **예시 JSON을 왕건이로 바꾸면 웹 테스트가 깨진다.** "웹은 그대로"와 "예시 JSON = 샘플"을 함께 지킬 수 없다 → 결정 D1.
  - 앱 쪽에서 예시 JSON과 샘플을 묶는 테스트: `FirestorePetProfileJsonTest`(40·47·60·98행), `FakePetProfileSourceTest`(24~27행 "보리는 예시 JSON과 같은 값이다").
- **샘플 값에 기대는 앱 테스트** (단계 2에서 고칠 후보. 구현자가 다시 grep으로 확인한다):
  - 이름 "보리"를 샘플에서 읽어 비교: `MedicalCardViewModelTest:44`, `MedicalCardViewModelBoundaryTest:145`, `MedicalCardContentBoundaryTest:54`(163행 생년월일 2014-08-01 나이 계산도), `FakePetProfileSourceTest:27`, `FirestorePetProfileJsonTest`
  - "보리"가 로그에 **없어야** 한다는 검사: `HospitalTripTest:343`, `HospitalTripAckTest:288`. 이름을 바꾸면 검사가 헛돌게 되므로 "왕건이"로 바꿔야 한다.
  - 테스트 이름·주석만: `PetPlanTest`, `PetPlanBoundaryTest`, `LinkViewModelBoundaryTest`, `LinkedPetProfileSourceBoundaryTest`, `FakePetProfileSourceBoundaryTest`
  - 자기 데이터로 "보리"를 만드는 테스트(`MedicalCardContentTest:32`, `initialOf("보리")` 등)는 샘플과 상관없으므로 그대로 둔다.
  - 샘플의 알레르기·복용약 값에 기대는 테스트가 있으면(지금 "특정 단백질 알레르기", "심장약 1일 1회") 테스트 실행으로 찾는다.
  - 속성 이름 `SamplePetProfiles.bori`를 쓰는 테스트는 약 30개 파일이다.
- `PetProfile`에 **없는 필드**: 체중, 종(강아지/고양이), 주치 병원, 특징. 형식을 바꾸면 웹·서버까지 바뀌므로 이번에는 넣지 않는다. 화면에서는 "미등록"으로 보인다. 앱은 반려견만 다루므로 종은 "강아지"로 고정해 쓴다 (D4).
- 나이 표시: 앱은 "12세 8개월"(`ageText`, 웹과 같은 규칙)이다. 프로토타입은 "만 12세"다 (D4).
- 사진: `PetProfile.photoDataUrl`은 있지만 앱은 사진을 그리지 않는다 (`PetAvatar`가 이름 첫 글자). 이번에도 그대로 둔다.
- **병원 찾기를 카드 없이 열기**:
  - `HospitalTrip.openFinder()`는 카드를 보지 않는다 (출발 전이면 `FinderStep.Hospitals`). 막는 곳은 `parkedContentOf`다: 목록·확인 화면은 `session is Ready`일 때만 보인다. 기존 테스트 `ParkedContentFinderTest`의 "카드가 없으면 찾기 단계여도 처음 화면이다"가 이 계약을 지킨다.
  - 확인 화면의 [출발]은 `cardViewModel.caseCard()`를 보낸다. `caseCard()`는 카드가 없어도 null이 아니다 (증상 없는 카드). 그래서 카드 없이 연 경우에는 확인 화면이 보내는 버튼을 숨겨야 한다. 근처 검색 병원(015)의 모양([병원에 전화] + [길 안내 시작] → `depart(choice, card = null)`)을 그대로 쓰면 된다.
  - 카드 없이 출발하면 `session None` + 안내 중 → `parkedContentOf`는 지금도 CARD(등록된 의료정보만, 증상 없음)를 고른다. 도착 [닫기]는 지금 흐름 그대로다.
- 정차 화면 순서는 `parkedContentOf`가 정한다. 새 화면(반려동물 카드·릴렉스)도 여기에 더한다. 주행·모름이면 `homeContentOf`가 먼저 주행 화면을 고르므로 새 화면은 주행 중에 보이지 않는다 (규칙 3, 바꾸지 않음).
- 아이콘: 앱은 `material-icons-core`만 쓴다 (`Phone`, `Send`, `ArrowBack`). 병원·반려동물 카드·릴렉스 외곽선 아이콘은 core에 없다 (D6).
- 공용 비율: `LinkScreen.kt`의 `private class LinkMetrics(s)`(1682 기준 폭 비율 0.45~1.0, `gap`, `style`, 버튼 높이 최소 56)를 공용으로 뺄 수 있다.
- 지금 정차 처음 화면에 있는 것과 새 자리 (D2 기본안):

  | 지금 | 새 자리 |
  |---|---|
  | "정차 중" 제목 | 없앤다. 왼쪽 위 "MeongCare"(`app_name`)가 대신한다 |
  | 반려견 카드(아바타 + 이름 + 칩) | 오른쪽 위 반려동물 줄: `PetAvatar` + 이름 + 상태(연결됨이면 "● 연결됨" ok색, 샘플이면 "시연용 샘플" sub색, 그 밖에는 없음) |
  | 시연용 샘플 안내(`card_sample_pet_notice`) | 프로토타입 음성 힌트 자리(화면 아래 왼쪽), 30 sub색 |
  | [진료 카드 만들기] + 힌트 | [응급 진료 시작 >] (같은 `onStartCard`). 힌트는 두 줄 안내로 바뀐다 |
  | 만든 카드 패널 + [카드 보기] / [새로 만들기] | 카드가 있으면 왼쪽 안내 두 줄 자리에 "만든 진료 카드 · 주요 증상: …", 주 버튼은 [카드 보기 >], 그 아래 글자 버튼 [새로 만들기](안내 없을 때만) |
  | 안내 영역(`GuidancePanel`, 안내 중·도착) | 왼쪽 열 맨 위에 지금 컴포넌트 그대로 (이번에 새 스타일로 다시 그리지 않음) |
  | 보호자 웹 연결 줄 | [⇄ 변경] 자리: 연결 전 [보호자 웹과 연결], 연결됨 [연결 끊기](지금 확인 대화 그대로). 끊김(revoked)이면 반려동물 줄 아래 "보호자가 연결을 끊었어요". Hidden(서버 설정 없음·안내 중)이면 버튼 없음 |
  | debug 패널 | 그대로 (`bottomBar`) |
  | (프로토타입) [음성 명령], 음성 힌트 | 숨김 (음성 명령 기능 없음) |

## 확인 필요 (시작 전에 해결)

새 Pleos API·새 `pleos.car.permission.*` 권한: **없다.** 릴렉스는 차 공조·음악을 바꾸지 않는 시연 화면이다 (사용자 결정). `[선행: pleos-docs-researcher]` 항목 없음.

- **D1 [사용자 승인 필요] 예시 JSON과 샘플 반려견** (위 "샘플 반려견과 예시 JSON" 참고)
  - 기본안 A: `docs/data/pet-profile.example.json`은 "보리" 그대로 둔다 (형식 예시이자 웹 샘플). 차 앱 `SamplePetProfiles`만 왕건이로 바꾸고, KDoc과 `docs/architecture.md`에 "차 앱 시연 샘플은 예시 JSON과 값이 다르다(2026-11-13 결정)"를 적는다. 앱 테스트 `FirestorePetProfileJsonTest`는 예시 JSON을 테스트 안의 기대값(보리)과 비교하도록, `FakePetProfileSourceTest`는 왕건이 값을 직접 보도록 고친다. 웹·`docs/data/` 변경 없음.
  - 안 B: 예시 JSON도 왕건이로 바꾼다. 웹 테스트(`petProfileJson.test.ts`의 `bori` 상수 등)와 `docs/data/pet-profile.md` 문구를 같이 고쳐야 한다. "웹은 그대로"에 어긋난다.
- **D2 (기획 결정) 프로토타입에 없는 기능의 자리**: 위 표의 기본안. 특히 "정차 중" 제목을 없애는 것, 연결 버튼을 [⇄ 변경] 자리에 두는 것.
- **D3 (기획 결정) 홈 [병원 찾기]에서 카드가 이미 있을 때**
  - 기본안: 카드가 있으면 카드를 열고 지금처럼 카드와 함께 찾는다(`onOpenCard` + `openFinder`, 확인 화면에서 [출발]로 보낼지 고름. 목록 [뒤로]는 카드 화면). 카드가 없으면 카드 없이 찾는다(목록 [뒤로]는 홈).
  - 안: 홈에서 열면 늘 카드 없이 찾는다.
  - 안내 중·도착(홈은 `atHome`일 때만 보임)에는 [병원 찾기]를 비활성으로 흐리게 둔다 (출발한 뒤에는 `openFinder`가 아무것도 하지 않는다).
- **D4 (기획 결정) 반려동물 카드의 없는 값**
  - 기본안: 체중·주치 병원·특징은 "미등록". 머리 줄은 "강아지 · 말티즈 · 12세 8개월"(체중 없음, 나이는 앱 규칙). 생년월일은 "2014.03.12 출생 · 12세 8개월"(추정이면 지금 카드처럼 "(나이로 입력)"). 성별은 프로토타입에 없어 보이지 않는다.
  - "진료 기록"은 기록 기능이 없다. 기본안: "아직 진료 기록이 없어요"만 보이고, "진료가 완료되면 기록이 표시됩니다."는 넣지 않는다 (없는 기능을 약속하지 않는다. 출품 유의사항). 프로토타입 문구를 다 넣을지 사용자가 정한다.
  - "환경 설정" 칸: 릴렉스 설정 요약(예: "릴렉스 꺼짐 · 실내 사운드 조용히")을 보인다.
- **D5 (기획 결정) 릴렉스 기본값과 저장**
  - 기본안: 큰 토글 **꺼짐**, 실내 사운드 "조용히", 추가 안심 안내 꺼짐. 앱 메모리(`RelaxViewModel`, 액티비티 범위)에만 둔다. 앱을 다시 켜면 기본값으로 돌아간다. 저장하지 않는다.
  - "현재 환경 22°C · 바람 보통 · 환기 후 내기"는 차에서 읽은 값이 아니므로 "예시" 표시를 붙인다(예: "현재 환경(예시)"). 화면 위에 "시연용 화면이에요. 차량 공조·음악에는 아직 적용하지 않아요."를 늘 보인다.
  - "환경 설정 >" 글자 버튼: 숨긴다. [완료]는 홈으로 돌아간다.
  - 추가 안심 안내 토글의 설명 문구는 프로토타입 문구를 쓰되, 실제로 안내를 보내는 것처럼 읽히면 "(시연용)"을 붙인다. 구현자가 프로토타입 문구를 보고 진행 로그에 적는다.
- **D6 아이콘** (기본안. 바꾸려면 알려 달라)
  - 기본안: Material Symbols(Apache 2.0)의 외곽선 아이콘 3개(병원 `local_hospital`, 반려동물 카드 `badge` 또는 `pets`, 릴렉스 `spa`)의 SVG 경로만 `ui/home/HomeIcons.kt`에 `ImageVector`로 옮긴다 (`PawIcon`과 같은 방식, 출처·라이선스 주석). 의존성은 더하지 않는다 (`material-icons-extended`는 무겁고 이 PC 빌드 메모리가 빠듯하다).
  - 프로토타입 아이콘 모양과 똑같이 하려면 프로토타입 SVG의 출처(팀이 그린 것인지, 어느 라이브러리인지)를 디자인 담당에게 확인해야 한다. 확인되면 그 경로로 바꾼다.
- 복용약 표기: 샘플은 `Medication(name = "심장질환 약 (상세 약명 미등록)", dosage = "")`로 둔다 (dosage가 비면 카드에 이름만 나온다). 다른 문구를 원하면 알려 달라.

## 단계

단계 하나 = 커밋 1~3개. 각 커밋마다 `assembleDebug` 통과(훅이 확인).
**필수**는 1~5, 8~10이다 (11/15 시연 영상·APK에 바로 보이는 순서). 6(반려동물 카드)과 7(릴렉스)은 사용자가 요청한 것이지만, 11/14 밤까지 5를 끝내지 못하면 7을 뒤로 미루고 홈의 [릴렉스] 버튼을 숨긴 채 제출한다 (사용자에게 먼저 알린다).

- [ ] 1. 공용 비율과 홈 토큰
  - `refactor(ui): 화면 폭 비율 계산을 DesignScale로 공용화`
    - 새 파일 `ui/theme/DesignScale.kt`: `class DesignScale(val s: Float)` + `gap(d)`, `style(base)`, `companion fun of(width: Dp)`(1682 기준, 0.45~1.0). `LinkMetrics`는 `DesignScale`을 받아 자기 크기(코드·막대 등)만 계산하게 바꾼다. 연결 화면 모양은 그대로.
    - 새 테스트 `DesignScaleTest`(같은 커밋): 1682dp → 1.0, 841dp → 0.5, 400dp → 0.45, 2400dp → 1.0, `gap(96.dp)`·`style` 크기가 비율대로.
  - `feat(ui): 홈 디자인 색 토큰 추가`
    - `DesignColors`에 `homeText`, `homeSubText`, `homeIcon`, `homeLine`, `homePrimaryPressed` (라이트는 위 사양 값, 다크는 일반 다크 토큰 값). 홈 버튼 바탕은 `primary`, 글자는 `onPrimary`를 쓴다.
- [x] 2. ~~시연용 샘플 반려견을 왕건이로~~ → 취소: 보리 그대로 (2026-11-13 사용자 재결정). 속성 이름 `bori` → `demoPet` 변경(d7b2bb5)만 남는다.
  - `refactor(data): 샘플 반려견 속성 이름을 demoPet으로`
    - `SamplePetProfiles.bori` → `SamplePetProfiles.demoPet`. 쓰는 곳(`AppContainer`, `FakePetProfileSource`, 테스트 약 30개 파일)의 이름만 바꾼다. 값·동작은 그대로라 테스트가 모두 통과해야 한다. 앞으로 이름이 또 바뀌어도 코드 이름은 그대로 둘 수 있게 하려는 것이다.
  - `feat(data): 시연용 샘플 반려견을 왕건이로 변경`
    - `SamplePetProfiles.demoPet`: 이름 "왕건이", 품종 "말티즈", 성별 MALE(지금 값 유지), 생년월일 2014-03-12, 추정 아님, 사진 없음, 지병 ["심장질환"], 알레르기 [] (미등록), 복용약 [`Medication("심장질환 약 (상세 약명 미등록)", "")`], `updatedAt`은 지금 값 유지. KDoc에 "예시 JSON(보리)과 다르다, 2026-11-13 사용자 결정"을 적는다.
    - 위 "샘플 값에 기대는 앱 테스트"를 고친다. `./gradlew testDebugUnitTest`로 깨지는 테스트를 모두 찾고, 샘플에 묶인 것만 왕건이 값으로 바꾼다. 로그에 이름이 없어야 한다는 검사(`HospitalTripTest:343`, `HospitalTripAckTest:288`)는 "왕건이"로 바꾼다. 고친 파일 목록을 진행 로그에 적는다 (완료 기준 2).
    - 미리보기 값(`MedicalCardScreen.kt` 499~501행 "보리")과 주석("보리")도 왕건이로 맞춘다 (`PetPlan.kt`, `PetProfileSource.kt`, `AppContainer.kt`, `FakePetProfileSource.kt`). 웹 파일은 건드리지 않는다.
  - `docs: 차 앱 시연 샘플 반려견 이름을 왕건이로`
    - `README.md` 21행, `docs/product/feature-facts.md` 48~49행, `docs/architecture.md` 158·310~312·357행. `docs/data/`는 그대로(D1 A).
- [ ] 3. 홈 화면 레이아웃 (Fake·시연 모드로 확인) → 커밋 2개
  - `feat(ui): 홈 바로가기 아이콘 추가` — `ui/home/HomeIcons.kt` (D6).
  - `feat(ui): 정차 처음 화면을 디자인 시안의 응급 진료 화면으로`
    - `ParkedScreen.kt`를 다시 그린다. `BoxWithConstraints` + `DesignScale.of(maxWidth)`, 바탕 `homeBackground`, 세로 스크롤은 남긴다 (태블릿은 높이가 1210보다 낮다).
    - 위쪽 줄: 왼쪽 `app_name` 로고 글자, 오른쪽 반려동물 줄 + 연결 버튼(D2 표).
    - 2열(55:45). 폭이 좁으면(예: 840dp 미만) 위아래로 쌓는다. 이번에 눈으로 확인하지는 않는다.
    - 왼쪽 열: (안내 중·도착이면 `GuidancePanel`) → "응급 진료" → 두 줄 안내(또는 만든 카드 줄) → 주 버튼([응급 진료 시작 >] = `actions.onStartCard`, 카드가 있으면 [카드 보기 >] = `actions.onOpenCard`) → 카드가 있고 안내가 없으면 글자 버튼 [새로 만들기] = `actions.onStartNewCard`.
    - 오른쪽 열: 세로선 + 바로가기 3개. 이 커밋에서는 콜백을 `ParkedScreen`의 새 인자(`onFindHospital`, `onOpenPetCard`, `onOpenRelax`, 기본값 `{}`)로만 받는다. 연결은 4~7단계.
    - 아래: 시연용 샘플 안내(샘플이고 반려견이 있을 때).
    - 지금의 `LinkRowPanel` 확인 대화는 그대로 쓰고, 버튼 모양만 토큰으로 바꾼다.
    - 새 문자열(`home_title` "응급 진료", `home_subtitle` 두 줄, `home_start_emergency` "응급 진료 시작", `home_shortcut_hospital` "병원 찾기", `home_shortcut_pet_card` "반려동물 카드", `home_shortcut_relax` "릴렉스", `home_pet_linked` "연결됨", `home_pet_sample` "시연용 샘플"). 쓰지 않게 된 문자열(`parked_status`, `home_card_hint`, `home_start_card` 등)은 린트가 경고하면 지운다.
    - Preview: Wide 라이트 × (카드 없음 / 카드 있음 / 안내 중).
- [ ] 4. 홈 [병원 찾기] (카드 없이 열기) → 커밋 2개
  - `feat(trip): 카드 없이 병원 찾기 열기`
    - `HospitalTrip`에 `openFinderWithoutCard()`와 `val finderWithoutCard: StateFlow<Boolean>`을 더한다. 출발 전에만 열리고, `back()`으로 None이 되거나 `closeFinder()`·`depart()`·`clear()`이면 false로 돌아간다. 기존 `openFinder()`는 바꾸지 않는다(false 유지).
    - 새 테스트 `HospitalTripHomeFinderTest`: 열면 Hospitals + true / 출발한 뒤에는 열리지 않음 / 목록 [뒤로]·닫기·출발이면 false / 기존 `openFinder()`는 false.
  - `feat(ui): 홈 병원 찾기 바로가기 연결`
    - `parkedContentOf`에 인자 `finderWithoutCard: Boolean = false`. true이고 안내 없음이고 `finder`가 Hospitals/Confirm이면, 카드 세션·`atHome`과 상관없이(단 증상 고르는 중·연결 화면이 먼저) HOSPITALS / CONFIRM_HOSPITAL. 기본값 false에서는 지금 표와 같다 → 기존 테스트 그대로 통과.
    - 새 테스트 `ParkedContentHomeFinderTest`: None + true → HOSPITALS·CONFIRM_HOSPITAL / 안내 중이면 CARD / Picking이 먼저 / false면 지금처럼 HOME.
    - `HospitalConfirmScreen`에 인자 `withCard: Boolean = true`. false면 근처 병원과 같은 버튼([병원에 전화] + [길 안내 시작] → `onDepartWithoutCard`)과 "진료 카드 없이 길 안내만 해요" 안내(새 문자열)를 보인다. 보내는 것·보내지 않는 것 안내는 빼도 된다.
    - `MainActivity`·`HomeScreen`: 홈 [병원 찾기] = D3 기본안 (카드 있음 → `onOpenCard()` + `openFinder()`, 없음 → `openFinderWithoutCard()`). 안내 중·도착이면 버튼 비활성.
- [ ] 5. 정차 화면 안의 새 쪽 상태 → `feat(ui): 정차 화면에 반려동물 카드·릴렉스 쪽 추가`
  - 새 `enum class HomePage { NONE, PET_CARD, RELAX }` (`ui/home/`). `ParkedContent`에 `PET_CARD`, `RELAX`. `parkedContentOf`에 인자 `page: HomePage = HomePage.NONE`: 증상 고르는 중·연결 화면 다음, 병원 찾기·`atHome`보다 먼저.
  - 상태는 `MainActivity`의 `rememberSaveable`에 둔다 (주행 화면으로 바뀌었다가 정차하면 같은 쪽으로 돌아온다). `HomeScreen`에는 `LinkExtras`처럼 묶음(`PageExtras(page, onOpen, onClose)`)과 화면 칸 `petCardScreen`, `relaxScreen`을 더한다. 시스템 뒤로 = 닫기.
  - 이 커밋에서는 두 화면 칸이 제목과 [←]만 있는 빈 화면이어도 된다.
  - 새 테스트 `ParkedContentPageTest`: NONE이면 지금과 같음 / PET_CARD·RELAX가 HOME·CARD·HOSPITALS보다 먼저 / Picking과 연결 화면은 page보다 먼저.
  - 주행 중에는 `homeContentOf`가 먼저 주행 화면을 고르므로 page와 상관없이 주행 화면이다 (기존 `HomeContentTest`가 지킨다. 바꾸지 않는다).
- [ ] 6. 반려동물 카드 화면 (보기 전용, 정차 중에만) → 커밋 1~2개
  - `feat(ui): 반려동물 카드 화면 추가`
    - 새 `ui/pet/PetCardContent.kt`: 순수 함수 `petCardOf(load: PetProfileLoad?, today: LocalDate): PetCardUiState`. 값: 이름, 첫 글자, 품종, 나이(`ageLabelOf`), 생년월일(+추정 여부), 지병, 복용약(`MedicationText`), 알레르기. 공백 정리는 `medicalCardOf`와 같은 규칙. 로딩·미등록·못 읽음은 따로 상태.
    - 새 테스트 `PetCardContentTest`: 보리 → 골든리트리버·2014-08-01·12세 3개월(오늘 2026-11-13 고정)·알레르기 1개 / 빈 품종은 null / 미등록·못 읽음·로딩.
    - 새 `ui/pet/PetCardScreen.kt`: 프로토타입 배치(헤더 "← 반려동물 카드", 아바타 + 이름 40/600 + 보조 줄 30, 두 열 "건강 정보" / "기본 정보" + "환경 설정", "정보 수정은 보호자 웹에서 할 수 있어요.", "진료 기록"). D4 기본안. 없는 값은 "미등록". 샘플이면 시연용 샘플 안내. `DesignScale` 사용.
    - 프로필은 화면 칸이 열려 있을 때만 `container.petProfileSource.profile`을 구독한다 (`MainActivity`의 `petCardScreen` 람다 안에서 `collectAsStateWithLifecycle(initialValue = null)`).
- [ ] 7. 릴렉스 화면 (시연용, 차량에 적용하지 않음) → 커밋 1~2개
  - `feat(ui): 릴렉스 시연 화면 추가`
    - 새 `ui/relax/RelaxViewModel.kt`: `StateFlow<RelaxSettings(enabled, sound: QUIET|CALM_MUSIC, extraReassurance)>`. `setEnabled`, `setSound`, `setExtraReassurance`. 꺼져 있으면 sound·reassurance 변경은 무시하고, 값은 그대로 남긴다. 메모리에만 (D5).
    - 새 테스트 `RelaxViewModelTest`: 기본값 / 켜고 바꾸기 / 끈 상태에서 바꾸기 무시 / 껐다 켜도 값 유지.
    - 새 `ui/relax/RelaxScreen.kt`: "← 릴렉스 모드", 시연용 안내(늘 보임), "{이름}를 위한 편안한 이동" + 큰 토글(꺼짐 설명 "꺼짐 · 저장한 설정은 그대로 유지됩니다."), "반려동물 환경"(실내 사운드 세그먼트 [조용히|잔잔한 음악], 현재 환경(예시)), "보호자 안심"(토글 + 설명), [완료]. 꺼지면 아래 항목 투명도를 낮추고 `enabled = false`. 터치 타깃은 높이 80 이상(비율 적용 후 최소 56). 글자 입력 없음.
    - 이름이 없으면(미등록 등) "반려동물을 위한 편안한 이동".
    - 반려동물 카드의 "환경 설정" 칸이 이 상태의 요약을 보인다 (같은 ViewModel, 액티비티 범위).
- [ ] 8. 테스트 (`tester`) → `test(ui): 홈 디자인·바로가기 경계 테스트`
  - 넘길 것: 이 계획 파일 경로, 대상 `ui/theme/DesignScale.kt`, `domain/trip/HospitalTrip.kt`(`openFinderWithoutCard`), `ui/home/ParkedContent.kt`, `ui/pet/PetCardContent.kt`, `ui/relax/RelaxViewModel.kt`, `data/pet/SamplePetProfiles.kt`.
  - `testDebugUnitTest` 전체 통과. 완료 기준 2·3의 diff 확인 결과를 진행 로그에 적는다.
- [ ] 9. 화면 확인 (`/verify`) → 고칠 것이 있으면 `fix(ui): ...`
  - 완료 기준 4의 절차. 태블릿 에뮬레이터, 시연 모드, 라이트, 가로만. 프로토타입 캡처와 비교 결과, 주행 전환 결과를 진행 로그에 적는다. 다 되면 사용자에게 에뮬레이터 화면을 바로 보여 준다.
  - `docs(architecture): DesignScale·홈 토큰·새 정차 화면 기록` — 완료 기준 5. `docs/product/feature-facts.md`에 "릴렉스: 시연용 화면, 차량 공조·음악에 적용하지 않음", "반려동물 카드: 보기 전용, 체중·주치 병원은 아직 필드 없음"을 적는다.
- [ ] 10. 개발 노트 작성 (`docs/notes/CYJhub/2026-11-13-design-home-screen.md`) → `docs(notes): design-home-screen`
  - 계획 파일을 `docs/plans/completed/`로 옮기는 것도 같이 커밋한다 (`docs(plans): 019 완료로 옮김`).

## 진행 로그

- 2026-11-13: 계획 작성. 브랜치 `feat/design-home-screen`(dev `5f6a628`). 사용자 결정(CYJhub, 2026-11-13): 2번 화면은 정차 처음 화면에 적용, 샘플 "보리" → "왕건이"(차 앱만), 바로가기 3개 동작(릴렉스는 시연용 화면만, 새 권한·SDK 없음), [변경]·[음성 명령]은 범위 밖, 확인은 태블릿 시연 모드 라이트 가로만.

- 2026-11-13: 사용자 결정 (CYJhub): D1 → **기본안 A** (차 앱 샘플만 왕건이, 예시 JSON·웹은 보리 그대로, 다르다는 것을 문서에 적음). D2 → **기본안** (프로토타입 빈자리에 배치). D3~D6·복용약 표기 → 기본안. 릴렉스는 시연용 화면만 (차량 공조·음악에 적용하지 않음, 새 권한 없음). 다크·세로 확인은 하지 않는다.

## 결정 / 배운 점
- 2026-11-13: 사용자 재결정 (CYJhub): "그냥 보리로 해" → 샘플 반려견은 **보리 그대로**. 왕건이 값 변경과 테스트 수정(13개)은 되돌렸다. D1은 더 이상 해당 없음 (예시 JSON = 샘플 유지). 2단계 완료 처리.
