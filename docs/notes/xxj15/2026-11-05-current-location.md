# current-location

- 날짜: 2026-11-05
- 작성: xxj15
- 브랜치: feat/current-location (010 `feat/real-navi-guidance` 위에서 땄다. 010 다음에 dev로 간다)
- 계획: docs/plans/completed/011-current-location.md
- 커밋: `docs(plans): 011 현재 위치(NaviHelper) 계획 추가` ~ `docs(plans): 011 현재 위치 완료`. 010이 dev에 들어가면 rebase해서 해시가 바뀌므로 메시지로 적는다.

## 무엇을 했나

- 차 앱이 필요할 때 **차량 내비에 현재 위치(위도·경도·주소)를 한 번 묻는다.** 나중에 "가까운 동물병원 찾기"의 기준점이 된다.
- 아직 위치를 쓰는 사용자 화면은 없다. debug 빌드의 debug 패널에 [위치 묻기]를 두어 확인한다. 정차 중에만 보인다.

## 어떻게: 핵심 결정과 이유

- **NaviHelper `getCurrentLocationInfo()`로 받는다.**
  - FusedLocation SDK는 콘솔에서 "준비 중"이고, 새 권한(`FUSED_LOCATION`)이 필요하다.
  - NaviHelper는 이미 승인된 `NAVI_ROUTE` 권한으로 된다.
- **층을 나눴다.** 010 길안내와 같은 방식이다.
  - 도메인 `CurrentLocationSource` (`suspend fun current(): CurrentLocation?`, null = 모름)
  - SDK 없는 경계 `NaviLocationClient`
  - 해석 `NaviCurrentLocationSource`: 시간 초과, 값 검사, 한 번에 하나, 로그. JVM으로 시험한다.
  - SDK 어댑터는 `PleosNaviClient.currentLocation()`이다.
- **`PleosNaviClient`에 메서드를 더했다** (새 어댑터 파일을 만들지 않음).
  - "NaviHelper를 부르는 유일한 파일" 규칙을 지킨다.
  - 010의 `trip()`은 한 줄도 고치지 않았다.
  - 패키지(`data/guidance/navi`)를 `data/navi`로 옮기는 일은 010·011이 dev에 들어간 뒤 따로 한다.
- **`NaviClient`와 인터페이스를 따로 뒀다.** `NaviEvent`에 위치를 더하면 길안내 쪽 `when`과 시험용 가짜를 고쳐야 한다.
- **물을 때마다 NaviHelper 하나.** 연결 → 묻기 → 답을 받으면 release한다. 시간 초과나 취소 때도 release한다.
  - 계속 추적하지 않는다. 병원 검색 때 한 번 묻는 용도라 연결을 들고 있을 이유가 없다.
  - 한 번에 하나만 묻는다(`Mutex`).
- **시간 초과 5초.** 관찰한 응답은 64~263ms였다. 가장 긴 값의 3배도 5초보다 작아서 그대로 뒀다.
- **값 검사(우리 규칙).**
  - NaN, 무한대, 범위 밖, (0, 0)이면 모름이다.
  - 주소가 null, 빈 값, 공백뿐이면 "주소 모름"이다. javap로는 주소의 null 여부가 보이지 않는다.
- **데이터 모드**(`di/LocationPlan.kt`):
  - 차량 기기의 시연·Real이면 내비 위치다.
  - 그 밖(Fake 모드, 일반 폰)은 Fake 위치다. 값은 Pleos 에뮬레이터 내비와 같은 37.4127212, 127.094681, "수정구 시흥동"이다.
- **주행 중에는 debug 위치 줄을 숨긴다** (AGENTS.md 규칙 3).
- **로그에 좌표·주소를 남기지 않는다.**
  - `CurrentLocation` 태그에 한 번에 한 줄만 남긴다. 예: `현재 위치: 성공 (주소 있음, 83ms)`
  - 부른 쪽이 취소하면 결과가 아니므로 로그를 남기지 않는다.
  - `Error` 계열(`LinkageError` 등)은 잡지 않는다.

## 사용한 Pleos SDK / API / 권한

- NaviHelper 3.0.5 (010에서 이미 추가):
  - `NaviHelper(Context)`, `addListener`, `initialize(onConnected, onFailed)`, `getCurrentLocationInfo()`, `removeListener`, `release()`
  - 콜백: `onCurrentLocationInfo(CurrentLocationInfo)`, `onError(NaviErrorCode)`
  - `CurrentLocationInfo`의 필드는 `navi-data` 3.0.4 AAR을 javap로 확인했다: `latitude`/`longitude`/`heading`은 `double`, `address`는 `String` (sdk-notes `[aar-navi]`).
- 권한: `pleos.car.permission.NAVI_ROUTE`. 새 권한은 없다.

## 확인

- 테스트:
  - `testDebugUnitTest` **420개 통과**. 시작할 때는 324개였다.
  - 내가 더한 30개: Fake 3, 내비 위치 해석 15, debug VM 5, 문구 3, LocationPlan 4
  - tester가 더한 경계 테스트 66개: 시간 초과 경계, 동시 묻기, 취소, callbackFlow 해제, 좌표 경계, 9개 언어의 숫자 형식 등
  - 기존 테스트는 고치거나 지우지 않았다.
- 검증 파이프라인 통과, `assembleRelease` 성공.
- Pleos 에뮬레이터(`emulator-5554`):
  - L1 시연 모드에서 [위치 묻기] → "위치(내비): 37.41272, 127.09468 · 수정구 시흥동"
  - L2 빠르게 두 번 눌러도 한 번만 묻는다.
  - L3 기어 D에서 위치 줄이 사라지고 `EditText`는 0개다. P로 돌아오면 마지막 결과가 그대로 있다.
  - L4 Real 모드도 같다.
  - L5 시연 길안내 중에 위치를 물어도 82ms에 답이 왔다. 위치 쪽 release 뒤에도 내비 경로가 그대로였고, 앱의 [안내 취소]로 경로가 사라졌다. NaviHelper 두 개를 같이 써도 된다.
  - L6 앱을 다시 띄운 뒤 첫 묻기 263ms, 다음 83ms.
  - L7 크래시와 `SecurityException`이 없다. 우리 앱 로그에 좌표는 0건이다.
  - Fake 모드: "위치(Fake): …"가 바로 나오고 NaviHelper를 부르지 않는다. [주행]이면 줄이 사라진다. 다크 모드에서도 잘 보인다.
- **확인하지 못한 것**:
  - 일반 폰 에뮬레이터(`emulator-5556`)가 꺼져 있었다. P1·P2·P4 일부는 Pleos 에뮬레이터의 Fake 모드로 대신 봤다.
  - P3(일반 폰의 Real 모드)는 `LocationPlanTest` 표 테스트로만 확인했다.
  - 세로 화면은 보지 못했다.
  - 실차는 보지 못했다.

## 시연 방법

1. Pleos 에뮬레이터를 Cold boot로 켠다. **내비 앱을 한 번 앞에 띄운다** (GNB의 내비 아이콘). 그 전에는 내비가 위치 요청에 답하지 않는다.
2. debug 빌드를 설치하고 debug 패널을 [시연으로 전환]이나 [Real로 전환]까지 누른다.
3. 기어 P(정차)에서 패널 셋째 줄의 [위치 묻기]를 누른다 → "위치(내비): 37.41272, 127.09468 · 수정구 시흥동"
4. 로그: `adb logcat -s CurrentLocation` → `현재 위치: 성공 (주소 있음, N ms)`

**기획서 5번 "개발 계획"에 넣을 문장** (확인한 것만):

> 차량의 현재 위치는 Pleos NaviHelper `getCurrentLocationInfo()`로 받는다 (Pleos 에뮬레이터에서 확인, 응답 0.3초 이내). 새 권한 없이 길안내와 같은 `NAVI_ROUTE` 권한을 쓴다. 이 위치를 가까운 동물병원 검색의 기준점으로 쓸 예정이다.

## 주의점 / 남은 일

- **내비 지도가 준비되기 전에는 위치 요청이 조용히 버려진다.**
  - 에뮬레이터가 다시 시작된 뒤 내비 화면을 한 번도 안 띄운 상태였다. 내비 로그는 "navi not ready"였다.
  - 실차 시동 직후에도 그런지는 모른다. 병원 검색을 붙일 때는 "위치 모름"일 때의 화면(다시 묻기, 지역 고르기 등)을 정해야 한다.
- 병원 검색(카카오 로컬, 문의 A1)과 연결하는 일이 남았다.
- K3: Real 모드 + 일반 폰에서도 Fake 위치다. 병원 검색 때 다시 본다.
- K4: 사용자 화면에 Fake 위치로 찾은 결과가 나올 때 "시연용" 표시를 할지 정해야 한다.
- 정리(`refactor`)가 남았다: `data/guidance/navi` → `data/navi` 패키지 이동, `trip`과 `currentLocation`의 연결 코드 공통화.
- 다른 시스템 앱 몇 개는 현재 위치를 좌표째 logcat에 남긴다. 우리 앱은 남기지 않는다.
