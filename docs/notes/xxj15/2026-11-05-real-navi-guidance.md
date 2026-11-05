# real-navi-guidance

- 날짜: 2026-11-05
- 작성: xxj15
- 브랜치: feat/real-navi-guidance
- 계획: docs/plans/completed/010-real-navi-guidance.md
- 커밋: 65234a9..(이 노트와 계획 이동 커밋까지)

## 무엇을 했나

진료 카드의 [길 안내 시작]을 누르면 차량 내비게이션(Pleos NaviHelper)에 병원 경로가 실제로 걸린다. 내비 앱이 앞에 나와 안내를 시작하고, 앱은 내비가 알려 준 예상 시간에서 남은 시간을 시작한다.
시연 모드(제출 APK)는 "경로만 진짜 내비"다. 에뮬레이터에서는 내비 속 차가 움직이지 않아서, 남은 시간 감소와 도착은 시연용으로 한다. 시연 도착 때 내비 경로도 끝낸다.

## 어떻게: 핵심 결정과 이유

- **데이터 모드 × 차량 기기로 내비 사용 범위를 정한다** (`di/NaviPlan.kt`의 `naviUseOf`):
  - Real + 차량 기기 → 전부 내비 (`NaviGuidanceSource`): 남은 시간·도착·취소를 내비 이벤트로 받는다.
  - 시연 + 차량 기기 → 경로만 내비 (`NaviRouteOnlyGuidanceSource`): 경로 요청·취소만 내비, 남은 시간과 도착은 시연용 Fake. 사용자 결정 (2026-11-05).
  - 일반 폰, Fake 모드 → NaviHelper를 만들지 않는다.
  - 007의 `sourcePlanOf`와 그 테스트는 고치지 않았다. "진행을 누가 하나"와 "내비에 무엇을 보내나"는 다른 질문이라 함수를 따로 뒀다.
- **경계는 `NaviClient.trip(destination): Flow<NaviEvent>` 하나**:
  - 구독하면 연결 → 경로 요청, 구독을 끊으면 (도착·취소 전이면) 경로 취소 → 해제. 안내 한 번에 NaviHelper 하나다.
  - SDK를 부르는 파일은 어댑터(`PleosNaviClient`) 하나다. 상태 기계는 SDK 없이 JVM 테스트로 확인한다.
  - 내비 앱이 앞에 나와 우리 앱이 가려져도 이벤트를 받아야 해서, 화면 수명이 아니라 안내 수명에 맞췄다.
- **남은 시간은 내비 예상 시간에서 시작** (사용자 결정): `RouteStarted.duration`(초)으로 시연 남은 시간을 시작한다. 30초 안에 안 오면 기본 12분.
- **내비 이벤트 규칙** (tester 경계 테스트와 에뮬레이터에서 찾은 버그를 고친 결과):
  - 0 이하나 하루보다 긴 남은 시간은 모름이다.
  - 내비에서 사용자가 "안내 종료"를 누르면 앱도 안내를 끝낸다.
  - 안내 시작 뒤의 내비 오류는 무시한다. 오류 한 번에 경로까지 취소하지 않으려는 것이다.
  - 내비에 이전 경로가 남아 있으면 경로 요청 직후 그 경로의 취소가 먼저 온다. 어댑터가 `onRouteStarted` 전의 취소를 거른다.
- 화면: 경로만 내비일 때 정차 카드에 "경로는 차량 내비게이션으로 안내해요. 앱의 남은 시간과 도착은 시연용이에요."를 보인다 (출품 유의사항 때문에 숨기지 않는다). 주행 화면은 바꾸지 않았다.

## 사용한 Pleos SDK / API / 권한

- `ai.pleos.playground:NaviHelper:3.0.5`
  - `NaviHelper(context)`, `addListener`, `initialize(onConnected, onFailed)`, `requestRoute(RouteInfo(latitude, longitude, poiName, poiId = "", address, poiSubId = ""))`, `cancelRoute()`, `removeListener`, `release()`
  - 리스너: `onRouteStarted`(`duration` 초), `onDrivingInfo`(`destination.duration`), `onDestinationArrived`, `onRouteCancelled`, `onError`
- 권한: `pleos.car.permission.NAVI_ROUTE_SEARCH`(경로 요청·취소), `pleos.car.permission.NAVI_ROUTE`(조회). 둘 다 2026-11-03 팀 승인.
- 근거: `docs/pleos/sdk-notes.md`의 `[aar-navi]`, `[obs-navi-emu]`

## 확인

- 테스트: 단위 테스트 368개 모두 통과 (010 이전 313개 + 010 55개).
  - 직접 쓴 것: `NaviGuidanceSourceTest`, `NaviRouteOnlyGuidanceSourceTest`, `NaviPlanTest`, `NaviTimingTest`
  - tester: 경계 테스트 42개. 처음에 7개 실패 → 버그 6개를 고쳤다.
- 검증 파이프라인 통과 (Android + 보호자 웹), `assembleRelease` 성공.
- 화면 (Pleos 에뮬레이터, debug 시연 모드):
  - 카드 → [길 안내 시작] → 내비 경로·자동 안내 시작 → 앱 "병원까지 41분" → 기어 D에서 줄어듦 → 시연 도착 → 내비 경로 취소 → "도착했어요" → [닫기] → 홈
  - 내비 화면에서 "안내 종료" → 앱이 "진료 카드 생성 완료"로 돌아감
  - 안내 중 앱 강제 종료 → 다시 켜서 안내 시작 → 이전 경로의 취소를 거르고 새 경로로 "병원까지 40분" → 시연 도착 때 `cancelRoute`
- **확인하지 못한 것**:
  - Real 모드(전부 내비)를 기기에서 보지 않았다. 에뮬레이터에서는 내비 도착 이벤트를 만들 수 없다. 단위 테스트로만 확인했다.
  - 일반 폰 에뮬레이터: 꺼져 있어서 보지 않았다. 일반 폰에서 내비를 쓰지 않는 규칙은 `naviUseOf` 테스트로만 확인했다.
  - release APK를 기기에 설치해 보지 않았다.
  - 카운트다운 중에 내비에서 취소했을 때 콜백이 오는지 보지 않았다. 지금은 그 취소를 거르므로, 30초 뒤 시연 기본값으로 시작한다.
  - 실차.

## 시연 방법

Pleos 에뮬레이터 (Cold boot, debug는 패널에서 "시연"으로. release는 그대로 시연 모드):

1. MeongCare → [진료 카드 만들기] → 증상 고르기 → [카드 만들기]
2. [길 안내 시작] → 내비 앱이 앞에 나와 "시연용 동물병원" 경로를 보이고 카운트다운 뒤 안내를 시작한다 (10~20초 걸릴 수 있다).
3. GNB의 MeongCare 아이콘으로 돌아오면 "병원까지 N분" (내비 예상 시간).
4. Virtual Console에서 기어 D → 남은 시간이 2초에 1분씩 준다 (`docs/notes/xxj15/2026-11-05-real-driving-state.md`의 `vc()`).
5. 0분이 되면 내비 경로가 사라지고 ("어디로 갈까요?"), 기어 P에서 앱에 "도착했어요" 카드가 뜬다.

## 주의점 / 남은 일

- **기획서 5번 "개발 계획"에 넣을 문장** (기획 담당에게 넘긴다. 확인한 것만):
  - "진료 카드의 길 안내는 Pleos NaviHelper SDK(`requestRoute`)로 차량 내비게이션에 병원 경로를 요청하고, 내비가 알려 주는 예상 소요 시간(`onRouteStarted`)을 앱에 보여 줍니다. 사용자가 내비에서 안내를 끝내면 앱도 함께 끝납니다."
  - "제출 APK와 시연은 경로 안내만 실제 차량 내비게이션을 쓰고, 남은 시간 감소와 도착은 시연용으로 처리합니다 (에뮬레이터에서는 내비 속 차량이 움직이지 않기 때문). 화면에도 그렇게 표시합니다."
- 있으면 좋음 중 안 한 것: 내비 연결 실패 안내(A), 앱을 다시 켤 때 진행 중인 경로 이어받기(C), 도착 재현 스크립트(D).
- 실차나 모의 주행이 되는 환경이 생기면 Real 모드(전부 내비)의 도착 이벤트를 확인한다.
- 이 작업 중에 다른 세션(011 현재 위치)이 같은 작업 폴더에서 브랜치를 바꿔서, 마무리는 worktree(`.claude/.cache/wt-010`)에서 했다. 같은 폴더에서 세션 두 개가 동시에 작업하면 브랜치가 섞일 수 있다.
