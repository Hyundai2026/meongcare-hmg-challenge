# 020 카드에서 연 [동물병원 찾기]를 프로토타입 흐름으로 (응답 확인 → 비교 → 출발 확인)

- 목표: 진료 카드 화면의 [동물병원 찾기]로 여는 흐름을 프로토타입의 `search` → (응답 없음 팝업) → `hosp`(비교) → `confirm`(출발 확인)과 같은 모양·순서로 바꾸고, 병원 답변은 앱 안의 시연 응답으로 오게 한다.
- 완료 기준 (어떻게 검증하나):
  1. `node .claude/hooks/build-gate.mjs pipeline`이 "검증 파이프라인 통과."를 출력한다 (웹은 바꾸지 않으므로 그대로 통과).
  2. `./gradlew testDebugUnitTest` 전체 통과. 시작할 때 개수를 진행 로그에 적는다.
     - 새 테스트가 통과한다: `VetInquiryTest`(단계 1), `VetInquiryViewModelTest`(단계 3), tester가 더한 경계 테스트(단계 7).
     - 기존 테스트 파일은 고치지 않는다: `git diff dev --diff-filter=MD --name-only -- app/src/test`의 출력이 비어 있다.
  3. 범위 확인. 아래 `git diff dev -- <경로>`의 출력이 비어 있다.
     - `web/`, `firebase/`, `docs/data/`, `app/src/main/AndroidManifest.xml` (새 권한 없음)
     - `app/src/main/java/com/meongcare/app/data/`, `domain/trip/HospitalTrip.kt` (depart·전송·안내는 그대로 쓴다)
     - `ui/hospital/HospitalListScreen.kt`, `ui/hospital/HospitalConfirmScreen.kt`, `ui/home/ParkedContent.kt`, `ui/home/DrivingScreen.kt`
  4. 화면 확인 (`/verify`): Pleos 태블릿 에뮬레이터, **시연 모드, 라이트, 가로** (019와 같은 범위). 프로토타입 캡처(1682×1210, scratchpad, 커밋 안 함)와 나란히 본 결과를 진행 로그에 적는다.
     - 카드 화면 [동물병원 찾기] → "동물병원 응답 확인": [←], "● 응급 모드", 음성 안내 줄, 안내문, 60초 진행 막대, 병원 3줄이 모두 "답변 대기 중", 상태 글 "병원에 문의했어요 · 동물병원 웹에서 답변을 기다리는 중", [동물병원 비교] 비활성.
     - 시연 응답이 오면 배지·보조 글·상태 글이 바뀌고 [동물병원 비교]가 켜진다. [다른 병원 2곳에도 문의] → 토스트 "다른 병원 2곳에도 문의했습니다." + 줄이 늘어난다 + 버튼이 사라진다.
     - 응답 없음 시나리오(D2)에서 20초 뒤 "아직 병원 응답이 없습니다" 팝업 → 병원 고르기 전 [이 병원으로 출발 준비] 비활성 → 고르면 체크·지도 라벨 → 출발 확인. [계속 기다리기]면 팝업이 닫힌다.
     - [동물병원 비교] → 진료 시작이 빠른 순, 가장 빠른 곳 강조, 안내 문구 → 고르고 [출발 준비 / 이 병원으로] → "출발 확인".
     - 출발 확인: 이름, "오후 h:mm 도착 예정", "10분 · 4.0km · (진료 시작까지 약 N분 | 대기시간 미확인)", 파란 안내, 보내는 정보 줄, [▲ 목적지 설정하고 출발] → 길안내 시작 + 카드 화면 전송 줄이 "보냄"(시연 병원, `CardDelivery.Sent(toServer=false)`), [📞 동물병원에 전화하기]는 D3대로.
     - [←]: 출발 확인 → 비교 → 응답 확인 → 카드 화면.
     - **주행 전환**: 응답 확인·팝업·비교·출발 확인 어디서든 debug 패널 [주행]이면 주행 화면, [정차]면 같은 단계로 돌아온다. 시연 응답 시계는 주행 중에도 흐른다(벽시계 기준).
     - 홈 [병원 찾기](`DemoHospitalFindScreen`, 카드 없음)는 지금과 같다.
     - `adb logcat -d -t 500 *:E`에 `com.meongcare.app` 크래시가 없다.
  5. 문서: `docs/architecture.md`에 문의 시연(`VetInquiry`, 시연 응답 시나리오, 앱 안에서만)과 화면 흐름이 적혀 있다. `docs/product/feature-facts.md`에 "병원 답변은 시연 응답이며 병원 웹과 연결하지 않음"이 적혀 있다 (출품 유의사항: 사실과 다른 내용 금지).
- 브랜치: `feat/find-vet` (dev에서 땄다, 시작 커밋 `1b72e36`)
- 관련 문서:
  - 디자인 원천: `docs/product/design/prototype/MeongCare car.html` (번들 한 줄 HTML, 382행에 상태 로직). 아래 "프로토타입 규칙"에 필요한 것을 옮겨 두었다.
  - 선례: `docs/plans/completed/019-design-home-screen.md` (`DesignScale`, 토큰, 시연 지도, 음성 자리)
  - AGENTS.md 규칙 3(주행 중 화면), 6(`/verify`), 7
  - 코드: `domain/trip/HospitalTrip.kt`, `ui/hospital/DemoHospitals.kt`, `ui/hospital/DemoHospitalFindScreen.kt`(`DemoMap`, `Pin`, `ActionButton`), `ui/voice/VoiceSlots.kt`, `ui/home/PageParts.kt`, `ui/home/HomeIcons.kt`, `ui/theme/DesignScale.kt`·`DesignTokens.kt`, `ui/card/MedicalCardScreen.kt`, `MainActivity.kt`(159~197행 `hospitalList`/`hospitalConfirm` 슬롯)

## 프로토타입 규칙 (382행에서 옮김)

- 상태 이름: `search`(응답 확인) → `hosp`(비교) → `confirm`(출발 확인). 뒤로: confirm → hosp → search → 카드.
- 처음 문의 대상: 병원 목록을 이동 시간 오름차순으로 정렬해 앞 3곳 (`DemoHospitals` 기준이면 별숲 10분, 온유 15분, 나래 20분). [다른 병원 N곳에도 문의] = 나머지 전부를 대상에 더한다. 나머지가 0이면 버튼 없음.
- 병원 한 곳의 상태:
  - 대상 아님 → `na`(문의 전). 응답 확인 화면에는 보이지 않는다.
  - 진료 불가 응답 → `no`. 배지 "진료 불가".
  - 진료 가능 응답 → `ok`(대기 분 `w`는 없을 수 있음, 응답 시각 `at`). 배지 "진료 가능", 보조 글 "진료 가능까지 {w}분 · {HH:MM} 응답", `w`가 없으면 "진료 가능 시간 미확인".
  - 응답 없음: 문의 후 20초 이하 → `wait`(배지 "답변 대기 중", 보조 글 "병원에서 답변을 기다리고 있어요"), 20초 초과 → `none`(배지 "응답 지연", 보조 글 색 warn).
- ok의 계산: 경과 `el` = 응답 뒤 지난 분(내림). 남은 대기 `wl = max(0, w - el)`. **진료 시작까지 `start = max(이동, wl)`**, 도착 후 대기 `add = max(0, wl - 이동)`(0이면 "거의 없음"). `el >= 10`이면 "응답 후 N분 경과 · 재확인 필요".
- 보이는 순서: 순위 `rk` = ok+start 있음 0, ok+start 없음 1, wait 2, 그 밖 3 → 같은 순위면 start 오름차순 → 이동 오름차순. 비교 목록은 no·na를 뺀 줄, ok가 아니면 흐리게(0.55)·고를 수 없음. 가장 빠른 곳(best) = start 있는 ok의 첫째.
- 상태 글: ok가 1곳 이상이면 "수용 가능 응답 N곳 · 다른 병원 답변도 계속 받아요", 대상이 모두 no면 "수용 가능한 병원 없음 · 다른 병원을 찾아보세요", 그 밖 "병원에 문의했어요 · 동물병원 웹에서 답변을 기다리는 중".
- 응답 없음 팝업: ok가 0곳이고 문의 후 20초가 지났고 ([계속 기다리기]로 닫지 않았으면) 뜬다. 목록 = no를 뺀 대상+비대상, 이동 오름차순, 보조 글 "이동 N분 · 응답 지연/응답 대기 중/문의 전". [이 병원으로 출발 준비] → 바로 출발 확인 (뒤로 가면 비교).
- 진행 막대: 문의 시각부터 60초.
- 출발 확인 세부: "{도착 시각} 도착 예정" (지금 + 이동 분), 둘째 줄 "{이동}분 · {km} · " + (start 있음 "진료 시작까지 약 {start}분" | 없음 "대기시간 미확인").

## 설계 (구현자가 따른다)

- **순수 로직 `domain/inquiry/VetInquiry.kt`** (Android·UI 의존 없음, 시간 주입):
  - 입력: 대상 후보 `InquiryHospital(key: String, travelMinutes: Int)` 목록, 시연 응답 시나리오 `InquiryScript`(병원 key → 문의 뒤 몇 초에 무슨 답: 가능(w 또는 null)/불가/답 없음).
  - `start(now)`, `expand(now)`(나머지 병원 문의, 그 병원들의 시나리오 시계는 expand 시각부터), `dismissNoResponse()`, `snapshot(now): InquirySnapshot` (줄별 상태, 정렬된 줄, okCount, 상태 종류, canExpand/restCount, showNoResponsePrompt, progress(0~1), best, 비교 줄의 start/add/el).
  - `DemoHospitals`(ui)에 기대지 않는다. ui가 `DemoHospital`을 `InquiryHospital`로 바꿔 넘긴다.
  - 시연 응답 시나리오 기본값은 D2에서 정한 값을 `InquiryScripts.DEFAULT`로 둔다. 화면·단위 테스트 둘 다 이것을 쓴다.
- **ViewModel `ui/hospital/VetInquiryViewModel.kt`** (액티비티 범위, `MainActivity`에서 만든다):
  - `restart()`: 새 문의 (카드 [동물병원 찾기]마다). 1초마다 `snapshot(clock())`을 `StateFlow`로 낸다 (화면이 구독 중일 때만 돈다, `WhileSubscribed`).
  - 안쪽 화면 상태: `Page { SEARCH, COMPARE }`, 팝업에서 고른 병원, 비교에서 고른 병원, 토스트 1회 메시지.
  - 출발 확인은 지금처럼 `HospitalTrip.choose(row)` → `FinderStep.Confirm`. 확인 화면은 VM 스냅숏에서 같은 병원의 start를 읽는다. 확인 [←] = `trip.back()` → Hospitals, VM의 Page를 COMPARE로 둔다 (프로토타입 back 표).
- **연결**: `MainActivity`의 카드 쪽 `onFindHospital` = `{ inquiryViewModel.restart(); trip.openFinder() }`. `hospitalList` 슬롯: `finderWithoutCard`면 지금 그대로 `DemoHospitalFindScreen`, 아니면 새 `VetInquiryScreen`(SEARCH/COMPARE). `hospitalConfirm` 슬롯: `finderWithoutCard`면 지금 그대로 `HospitalConfirmScreen(withCard=false)`, 아니면 새 `DepartConfirmScreen` → [목적지 설정하고 출발] = `trip.depart(choice, cardViewModel.caseCard())`.
  - `HospitalListScreen`·`HospitalFinderViewModel`(제휴 병원·서버 목록, 012/015)은 지우지 않는다. 다만 카드 흐름에서는 더 열리지 않는다 (D4).
- **주행 중**: 새 화면은 모두 `parkedContentOf`의 HOSPITALS / CONFIRM_HOSPITAL 안에 있으므로 주행·모름이면 `homeContentOf`가 주행 화면을 먼저 고른다 (규칙 3, 바꾸지 않음). 새 화면에 글자 입력 없음, 터치 타깃 `scale.touch(...)`(최소 56).

## 확인 필요 (시작 전에 해결)

새 Pleos API·새 `pleos.car.permission.*` 권한: **없다.** `[선행: pleos-docs-researcher]` 항목 없음. 음성은 자리만(`VoiceCommandButton`, `VoiceGuideLine`).

- **D1 [사용자 승인 필요] 시연 응답을 화면에 알릴지** (출품 유의사항: 사실과 다르면 선정 취소)
  - 문제: 안내문 "동물병원 웹에서 답변하면 바로 반영됩니다."와 상태 글 "동물병원 웹에서 답변을 기다리는 중"은 이번 빌드에서 사실이 아니다 (답은 앱 안 시나리오가 낸다). 병원 이름도 가상이다.
  - 안 A (권장): 화면 모양은 프로토타입 그대로, 응답 확인 화면 오른쪽 아래(또는 진행 막대 옆)에 작은 배지 하나 "시연 응답" (019 지도의 "시연용 지도 · 가상 병원" 배지와 같은 크기·색). 기획서·영상 자막에도 "병원 답변은 시연" 명시.
  - 안 B: 화면에는 표시 없음. 기획서·시연 영상 자막·`feature-facts.md`에만 적는다. 위험: 심사위원이 APK만 보고 실제 연동으로 오해할 수 있음.
  - 안 C: 문구만 바꾼다 ("…답변하면 바로 반영됩니다" 삭제 등). 프로토타입과 달라진다.
  - 함께 정할 것: 재사용하는 시연 지도의 배지 "시연용 지도 · 가상 병원"을 이 흐름(팝업·출발 확인 지도)에서도 보일지. 기본안: 보인다 (019 결정 유지).
- **D2 (기획 결정) 시연 응답 시나리오** — 문의 시각 기준 초. 기본안:
  - 처음 3곳: 별숲(10분) 7초에 "진료 가능, 30분 뒤" → start 30 / 온유(15분) 4초에 "진료 가능, 5분 뒤" → start 15 (best) / 나래(20분) 답 없음 → 20초 뒤 "응답 지연".
  - 추가 2곳: 달빛 5초에 "진료 불가" / 가람 9초에 "진료 가능, 대기 미확인".
  - 이 기본안에서는 4초에 첫 ok가 와서 **응답 없음 팝업이 시연에 나오지 않는다.** 팝업을 영상에 보이려면 둘 중 하나를 고른다:
    - 안 a: debug 패널에 "문의 시나리오: 보통 / 응답 없음" 선택을 둔다 (debug 빌드만, release는 보통). 응답 없음 = 모두 답 없음, 다른 병원 문의 뒤 8초에 한 곳 ok.
    - 안 b: 기본 시나리오 자체를 "처음 3곳은 25초까지 답 없음 → 팝업 → [계속 기다리기] 뒤 30초에 ok"로 바꾼다 (시연 동선이 길어짐).
    - 안 c: 팝업은 구현만 하고 영상에는 넣지 않는다.
  - 사용자 메모의 "프로토타입은 나래·별숲·온유 순으로 보임"은 위 정렬 규칙(답 없으면 이동 순 → 별숲·온유·나래)과 다르다. 프로토타입 병원 데이터(HP)가 `DemoHospitals`와 다를 수 있다. 기본안: 규칙(별숲·온유·나래)을 따른다. 프로토타입과 같은 순서를 원하면 알려 달라.
- **D3 (기획 결정) [📞 동물병원에 전화하기] — 시연 병원은 번호가 없다**
  - 기본안: 버튼은 프로토타입대로 보이고, 누르면 토스트 "이 병원은 전화번호가 등록되지 않았어요."
  - 안: 비활성(흐림)으로 둔다 / 숨긴다 / 시연용 가짜 번호를 넣는다(권장하지 않음: 실제 번호로 걸릴 수 있음).
- **D4 (기획 결정) 제휴 병원(서버) 목록이 카드 흐름에서 빠지는 것**
  - 이 흐름은 `DemoHospitals`만 쓴다. 그래서 카드 → 서버 병원 → 병원 웹으로 카드가 가는 012 시연(병원 웹 `/hospital/`)은 UI에서 열리지 않는다 (코드는 남김).
  - 기본안: 받아들인다 (사용자 결정 "실제 병원 웹 연동 없음"). 병원 웹 시연이 영상·PT에 필요하면 debug 패널에서 예전 목록으로 여는 길을 남길지 정한다.
- **D5 (기획 결정) 출발 확인 둘째 줄**: 사용자 메모는 "대기시간 미확인"이지만 프로토타입은 진료 가능 응답(start 있음)이면 "진료 시작까지 약 N분"을 보인다. 기본안: 프로토타입 규칙(응답에서 고르면 "진료 시작까지 약 N분", 팝업에서 고르면 "대기시간 미확인").
- 도착 시각 표기: "오후 3:14" = `Locale.KOREAN`의 `a h:mm`, 지금 시각 + 이동 분. 시계는 VM에 주입(테스트용).

## 단계

단계 하나 = 커밋 1~3개. 커밋마다 `assembleDebug` 통과(훅).
**필수**: 1, 2, 3, 5, 6, 7, 8, 9 (11/15 영상·APK에 바로 보이는 흐름). **있으면 좋음**: 4(응답 없음 팝업). 11/14 저녁까지 6을 못 끝내면 4를 미루고 사용자에게 알린다. D2 안 a(debug 시나리오 선택)를 고르면 4에 같이 넣는다.

- [x] 1. 문의 시연 로직 (순수) → `feat(domain): 동물병원 문의 시연 응답 로직`
  - `domain/inquiry/VetInquiry.kt`, `InquiryScript`·`InquiryScripts.DEFAULT` (D2), 위 "프로토타입 규칙" 전부.
  - 새 테스트 `VetInquiryTest`(같은 커밋): 시작 직후 3곳 wait·이동 순 / 대상은 이동 짧은 3곳 / 응답 시각 전후 상태 / 20초 경계에서 wait→none / start=max(이동, wl)·add·"거의 없음" / el 경과와 10분 재확인 / 정렬 rk→start→이동 / best / expand 뒤 restCount 0·새 대상 시계 / 모두 no면 상태 "없음" / 팝업 조건(ok 0 + 20초 초과 + 닫지 않음) / progress 60초 클램프.
- [x] 2. 시연 지도 공용화 → `refactor(ui): 시연 지도를 공용 파일로 분리`
  - `DemoHospitalFindScreen.kt`의 `DemoMap`·`Pin`·`ActionButton`을 `ui/hospital/DemoMap.kt`(internal)로 옮긴다. 인자 추가: 보일 병원, 고른 병원 라벨("{순위} {이름} 이동 {분}분" 또는 "… {도착 시각} 도착 예정"), 왼쪽 위 제목 칸 보일지. 홈 [병원 찾기] 모양은 그대로 (`/verify` 전에 Preview로 비교).
- [x] 3. 응답 확인 화면 (`search`) → 커밋 2개
  - `feat(ui): 동물병원 문의 화면 상태 추가` — `VetInquiryViewModel` (+ 새 테스트 `VetInquiryViewModelTest`: restart 시 새 스냅숏, 시계 진행에 따른 갱신, expand 토스트 1회, Page 전환, 비교 선택 초기화).
  - `feat(ui): 동물병원 응답 확인 화면` — `ui/hospital/VetInquiryScreen.kt` SEARCH: 머리([←] `trip.back`, 제목, "● 응급 모드", [음성 명령] 자리), `VoiceGuideLine("진료 가능한지 병원에 문의하고 있어요.")`, 안내문, 진행 막대, 병원 줄 + 배지(톤: ok/mute/warn), 상태 글, [다른 병원 N곳에도 문의], 토스트, 오른쪽 아래 [동물병원 비교](ok 0이면 비활성). D1 표시. `MainActivity` 연결(위 "설계 · 연결"). 새 문자열은 `strings.xml`에.
- [x] 4. (있으면 좋음) 응답 없음 팝업 → `feat(ui): 병원 응답 없음 팝업`
  - 좌: 번호·이름·"이동 N분 · 상태", 체크, 경고 글, [계속 기다리기](`dismissNoResponse`), [이 병원으로 출발 준비](고르기 전 비활성 → `trip.choose(row)`, VM Page=COMPARE). 우: `DemoMap` + 고른 병원 라벨.
  - D2 안 a면 `feat(debug): 문의 시연 시나리오 선택` 커밋을 더한다 (debug 패널, 재시작 없이 다음 문의부터).
- [x] 5. 동물병원 비교 (`hosp`) → `feat(ui): 동물병원 비교 화면`
  - 머리 [←](Page=SEARCH), 표 머리 "진료 시작이 빠른 순 / 이동 / 도착 후 대기 / 진료 시작까지", 줄(이름·meta·상태 글·큰 숫자 "약 N분"), ok 아닌 줄 흐림·고를 수 없음, best 강조, 안내 "진료 시작까지 = 이동시간과 병원이 알려 준 진료 가능 시간 중 더 긴 쪽 · 모두 추정치이며 즉시 진료를 보장하지 않습니다", [출발 준비 / 이 병원으로](고르기 전 비활성, 기본 선택 = best는 프로토타입 확인 후) → `trip.choose(row)`.
- [x] 6. 출발 확인 (`confirm`) → `feat(ui): 출발 확인 화면`
  - 새 `ui/hospital/DepartConfirmScreen.kt`: 머리 [←] "출발 확인", "● 응급 모드", [음성 명령] 자리. 왼쪽 이름, "{오후 h:mm} 도착 예정"(큰 글), 둘째 줄(D5), 파란 글 "{반려견 이름}의 진료 카드와 도착 시간을 병원에 미리 보낼게요. 천천히, 안전하게 가세요."(이름 없으면 조사 처리 `KoreanParticles`), "병원에 보내는 정보 · 진료 카드 · 선택한 증상 · 도착 예정 시각", [▲ 목적지 설정하고 출발] = `trip.depart(choice, cardViewModel.caseCard())`, [📞 동물병원에 전화하기](D3). 오른쪽 `DemoMap` + 라벨 "… 도착 예정".
  - [출발]이 이번 전송의 동의다 (012 원칙 유지: 보내는 정보를 버튼 바로 위에 보인다).
  - 출발 뒤 카드 화면의 전송 줄·안내 영역은 지금 그대로 동작해야 한다 (확인만, 코드 변경 없음).
- [ ] 7. 테스트 (`tester`) → `test(domain): 동물병원 문의 시연 경계 테스트`
  - 넘길 것: 이 계획 파일 경로, 대상 `domain/inquiry/VetInquiry.kt`, `ui/hospital/VetInquiryViewModel.kt`. 구현 설명은 넘기지 않는다.
  - `testDebugUnitTest` 전체 통과. 완료 기준 2·3의 diff 결과를 진행 로그에 적는다.
- [ ] 8. 화면 확인 (`/verify`) + 문서 → 고칠 것이 있으면 `fix(ui): ...`
  - 완료 기준 4의 절차 (주행 전환 포함). 프로토타입 캡처와 비교 결과를 진행 로그에 적고, 사용자에게 에뮬레이터 화면을 보여 준다.
  - `docs(architecture): 동물병원 문의 시연 흐름 기록` — 완료 기준 5 (`docs/architecture.md`, `docs/product/feature-facts.md`).
- [ ] 9. 개발 노트 작성 (`docs/notes/CYJhub/2026-11-13-find-vet.md`) → `docs(notes): find-vet`
  - 계획 파일을 `docs/plans/completed/`로 옮기는 것도 같이 커밋한다 (`docs(plans): 020 완료로 옮김`).

## 진행 로그

- 2026-11-13: 계획 작성. 브랜치 `feat/find-vet`(dev에서, `1b72e36`까지). 사용자 결정(CYJhub): 병원 답변은 앱 안 시연만, 카드는 출발 시 보냄(BUILT_IN → Fake 전송), 화면은 프로토타입과 같게·시연 표시 최소, 음성은 자리만, 홈 [병원 찾기]는 그대로, 기존 기능(depart·전송·안내 영역) 유지.

## 결정 / 배운 점
- 2026-11-13: 사용자 결정 (CYJhub): D1 → **안 B** (화면에 시연 표시 없음, 프로토타입 문구 그대로. 시연인 점은 기획서·영상 자막·feature-facts.md에만). D2 → **안 a** (debug 패널에서 문의 시나리오 보통/응답 없음 선택, 기본 시나리오는 계획 기본안). D4 → 기본안 (받아들임). D3·D5 → 기본안.
- 2026-11-13: 1~6단계 완료. 커밋: c74616b(로직+테스트), 9d704c7(지도 공용화), 0d8cab2(VM+테스트), 35f7ee2(응답 확인·팝업·비교·출발 확인, 팝업은 3단계 화면 커밋에 같이), 이번 커밋(debug 시나리오). 에뮬레이터에서 확인: 카드 [동물병원 찾기] → 응답 확인(4초·7초 진료 가능) → 비교(온유 가장 빠름 약 15분) → 출발 확인(오후 h:mm 도착 예정) → [목적지 설정하고 출발] → 길안내(주행 화면 "온유동물병원까지"). 응답 없음 시나리오에서 20초 뒤 팝업, 고르면 체크·지도 라벨·경고 문구. 결정 D1-B대로 이 흐름에는 시연 표시 없음(지도 배지도 끔).
- 2026-11-14: 사용자 결정 (CYJhub) 반영 (커밋 4afa185):
  - 처음부터 시안 병원 5곳 모두에 문의한다 (`FIRST_ASK = 5`). [다른 병원 N곳에도 문의]는 남는 곳이 없어 나오지 않는다. 5줄이 태블릿 한 화면에 보이게 한 줄짜리 행으로 줄였다.
  - 응답 대기 시간 20초를 카운트다운으로 보인다 (막대 20초 → 0초, "남은 시간 N초").
  - 20초가 끝났을 때 진료 가능 답이 있으면 "○○으로 출발할까요?" 팝업: 진료 시작까지(이동 + 도착 후 대기)가 가장 짧은 곳. [여기로 출발] = 비교 화면 없이 카드와 함께 바로 출발. [다른 병원 비교]로 비교 화면도 갈 수 있다.
  - 20초 동안 진료 가능 답이 없으면 "아직 답변한 병원이 없어요" 팝업: [조금 더 기다리기](20초를 다시 센다) 또는 [가장 가까운 병원으로 안내](진료 불가가 아닌 곳 중 가장 가까운 곳으로 카드와 함께 출발 + 병원 전화 연결).
  - 시안 병원에 시연용 번호 02-0000-0021~0025를 넣었다 (전화 연결용, 실제로 걸리지 않는 0000 국번). debug "응답 없음" 시나리오는 모든 병원 무응답.
  - 에뮬레이터 확인: 보통 시나리오 20초 뒤 "온유동물병원으로 출발할까요?" → [여기로 출발] → 길안내. 응답 없음 시나리오 20초 뒤 팝업 → [가장 가까운 병원으로 안내] → 전화 앱에 0200000021(별숲)이 열림.
- 2026-11-14: 사용자 요청 반영: 카운트다운 막대를 1초 선형 애니메이션으로 부드럽게, 상태 색(진료 가능 초록·대기 파랑·지연 주황·불가 빨강: 왼쪽 색 막대·줄 배경·진한 배지), 답변 온 병원은 "이동 N분 · 대기 N분 · 총 약 N분"(숫자 파란 굵게), 출발 제안·응답 없음·출발 확인 지도에 지금 위치→병원 꺾인 경로(시연용 그림). 에뮬레이터에서 5줄 모두 보임·제안 팝업 경로 확인.
- 2026-11-14: 사용자 결정: 처음 화면에서 길안내 상태 영역(GuidancePanel)을 뺀다 (주행 화면·진료 카드 화면에는 그대로). 시안 병원에 시연 ID(demo-vet-N)를 넣어 시연 카드 전송 실패 버그 수정 (4c8d359).
- 2026-11-14: 사용자 결정: 출발 제안 팝업 버튼을 [이 병원으로 출발 준비]로 바꾸고, 비교 경로와 같이 "출발 확인" 화면을 거친다 ([←]는 그 병원을 고른 비교 쪽).
