# 016 폰·태블릿 고정 시연 위치

- 목표: 차량 기기가 아닌 폰·태블릿의 시연·Real 모드 현재 위치를 서울 서초구 서운로 62(37.4902566, 127.0283099)로 고정하고, 015에서 넣은 기기 GPS 경로(코드·위치 권한)를 뺀다.
- 완료 기준 (어떻게 검증하나):
  1. `./gradlew assembleDebug testDebugUnitTest lintDebug` 통과. 새 표 테스트 `FixedLocationPlanTest`(6칸, 아래 "설계 1")가 통과한다.
  2. `app/`에서 `DeviceCurrentLocationSource`, `usesDeviceLocation`, `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `RequestMultiplePermissions` 검색 결과가 0건이다.
  3. `aapt2 dump permissions app/build/outputs/apk/debug/app-debug.apk`(Android SDK `build-tools/<버전>/aapt2`)에 `ACCESS_FINE_LOCATION`·`ACCESS_COARSE_LOCATION`이 없다. 라이브러리(NaviHelper 등) 매니페스트가 넣는 것으로 보이면 지우지 말고 "확인 필요" 그대로 진행 로그에 적는다.
  4. 화면 (`/verify`, 일반 태블릿 에뮬레이터, 아래 "확인 절차" T1~T5): 위치 권한 창이 뜨지 않고, release APK 병원 목록 이동 시간이 수 분~수십 분이다 (47,080분 같은 값이 없다). 안내 "시연용 위치(서초구 서운로 62) 기준이에요."가 보인다.
  5. 주행 중 화면은 바뀌지 않는다: 주행 중(Fake [주행])에는 debug 패널 위치 줄이 없고, 병원 목록 안내는 정차 화면에만 있다 (T5).
  6. `docs/architecture.md`, `README.md`, `firebase/README.md`에 기기 위치·위치 권한 문구가 남지 않는다 (015 노트와 completed 계획은 기록이라 그대로 둔다).
- 브랜치: feat/fixed-demo-location
- 관련 문서: [015 계획](../completed/015-nearby-vet-search.md) 진행 로그 2026-11-08 "범위 추가", [architecture.md](../../architecture.md) "현재 위치 (011)", [hospital-status.md](../../data/hospital-status.md) "시연 병원 5곳", [nearby-vets.md](../../data/nearby-vets.md)

## 사용자 결정 (2026-11-09, CYJhub)

- 폰·태블릿(차량 기기가 아닌 곳)의 시연·Real 모드는 위치를 고정한다: 서울 서초구 서운로 62 = 37.4902566, 127.0283099 (OpenStreetMap 지오코딩, "62, 서운로, 서초2동, 서초구"). debug·release 모두.
- 차량 기기(Pleos 에뮬레이터·실차)의 시연·Real은 그대로 Pleos 내비 위치.
- 기기 GPS 경로를 뺀다: `DeviceCurrentLocationSource`, `usesDeviceLocation`, 매니페스트 위치 권한 2개, MainActivity 권한 요청. Android 권한이 줄어든다 (새 권한 없음).
- 이유: 일반 에뮬레이터는 GPS가 없어 기본 위치(미국)를 준다. release를 일반 에뮬레이터에서 돌리면 제휴 병원 "이동 약 47,080분"이 나오고, 팀원마다 결과가 다르다.

## 확인 필요 (시작 전에 해결)

- [사용자 승인 필요] U1. **폰·태블릿 release에서 "가까운 곳이 가장 빠른 곳은 아니다" 이야기가 깨진다.** 제휴 시연 병원 5곳은 시흥동 기준 배치다. 서운로에서 다시 계산한 값 (하버사인, `올림(km × 1.25 ÷ 21 × 60)`, 손 계산이라 ±1분):

  | id | 시흥동 기준 (지금) | 서운로 기준 |
  |---|---|---|
  | `demo-main` | 9.59km, 2위 40분 | **약 0.89km, 이동 4분 + 대기 5분 = 1위 약 9분** |
  | `demo-fast` | 5.22km, 1위 29분 | 약 15.4km, 56분 + 10분 = 2위 약 66분 |
  | `demo-near` ("가까운") | 2.48km, 3위 69분 | 약 12.9km, 47분 + 60분 = 3위 약 107분 |
  | `demo-stale` | 8.32km | 약 6.5km (정보 지남 묶음) |
  | `demo-closed` | 2.90km | 약 12.4km (응급 접수 안 함) |

  → 폰에서는 가장 가까운 곳이 가장 빠른 곳이 되고, "가까운 동물병원"이 13km 밖이다. Pleos 에뮬레이터(시흥동)는 그대로다.
  - 기본안 (A, 이 계획의 필수 범위): 병원 배치는 그대로 둔다. 시연 영상·PT의 순위 이야기는 Pleos 에뮬레이터로 보인다. 폰 화면은 "시연용 위치(서초구 서운로 62) 기준"으로 정직하게 알리기만 한다.
  - 대안 (B, 있으면 좋음 단계 H1): 시연 영상을 폰·태블릿 에뮬레이터로 찍는다면, 폰 고정 위치용 시연 병원 배치를 따로 둔다. 내장 목록·`hospitals.sample.json`·firebase seed·hospital-status.md·관련 테스트가 바뀌고, 시드가 기기마다 다른 목록을 줄 수 없어서 서버 모드에서는 한쪽 이야기만 맞는다. D-6에는 크다.
  - 질문: **시연 영상과 PT를 어느 기기로 하나? A로 가도 되나?**
- [사용자 승인 필요] U2. **Fake 모드 위치는 시흥동 그대로 둔다** (기본안). 이유:
  - Fake 근처 병원 3곳(`FakeNearbyVetSource.DEFAULT`, 시흥동 기준 1·2.5·4km)은 서운로에서 약 10km 밖이다. `NEARBY_MAX_DISTANCE_KM`(5.5km) 거르기로 3곳이 모두 빠져 Fake 근처 묶음이 비게 된다.
  - Fake = Pleos 에뮬레이터 내비 값이라는 011 결정, 제휴 순위 이야기, 시흥동 좌표를 쓰는 테스트 약 25개(대부분 `here` 값)가 그대로 남는다.
  - 결과: debug에서 폰 Fake는 시흥동, 폰 시연·Real은 서운로로 다르다. 화면 안내와 debug 패널이 주소를 보여 주므로 어느 쪽인지 보인다.
- [사용자 승인 필요] U3. 화면에 보일 주소 글자: 기본안 **"서초구 서운로 62"** (내비 형식 "수정구 시흥동"처럼 구부터). 안내 "시연용 위치(서초구 서운로 62) 기준이에요.", debug 패널 "위치(Fake): 37.49026, 127.02831 · 서초구 서운로 62".
- Pleos API: 새로 쓰는 것 없음 (`[선행: pleos-docs-researcher]` 없음). 새 `pleos.car.permission.*` 없음.
- 미확인 (막지 않음): 서운로 기준으로 `nearbyVets` 함수(카카오)가 몇 곳을 주는지. debug 시연 + 로컬 서버에서만 보이는 기능이라 있으면 좋음 H2로 둔다.

## 설계

1. **위치 표 (`di/LocationPlan.kt`)**: `currentLocationKindOf`는 그대로 둔다 (011 표·`LocationPlanTest` 유지). `usesDeviceLocation`을 지우고 아래 순수 함수로 바꾼다.

   ```kotlin
   /** 내비를 쓰지 않는 칸의 고정 위치 (016). 내비 칸(차량 기기의 시연·Real)이면 null. */
   fun fixedLocationOf(mode: DataMode, isAutomotive: Boolean): CurrentLocation?
   ```

   | 모드 | 차량 기기 | 일반 폰·태블릿 |
   |---|---|---|
   | Fake | 시흥동 (`DEMO_LOCATION`) | 시흥동 (`DEMO_LOCATION`) |
   | 시연 | null (내비) | **서운로 (`PHONE_LOCATION`)** |
   | Real | null (내비) | **서운로 (`PHONE_LOCATION`)** |

   - 규칙: `fixedLocationOf(...) == null` ⇔ `currentLocationKindOf(...) == REAL`.
   - 값: `FakeCurrentLocationSource.PHONE_LOCATION = CurrentLocation(37.4902566, 127.0283099, "서초동")`. 출처(OSM 지오코딩, 사용자 결정 2026-11-09)를 KDoc에 적는다.
2. **`AppContainer`**:
   - `currentLocationSource`의 FAKE 가지 = `FakeCurrentLocationSource(fixedLocation)`.
   - `isLocationSample = locationKind == SourceKind.FAKE` (011 규칙으로 되돌림. 폰 시연·Real도 시연용 위치다).
   - 새 `sampleLocationName: String?` = 고정 위치의 주소 (내비면 null).
   - `usesDeviceLocation` 속성을 지운다.
3. **`MainActivity`**: 권한 요청 블록(`rememberLauncherForActivityResult` + `LaunchedEffect`)을 지운다. debug 패널 출처 이름은 `REAL → "내비"`, 그 밖 `"Fake"` (주소가 어느 위치인지 보여 준다).
4. **안내 글자**: `finder_notice_sample_location`을 `시연용 위치(%1$s) 기준이에요.`로 바꾼다. `finderNoticesOf(origin, basis, isLocationSample: Boolean)`과 그 테스트는 그대로 둔다. `HospitalListScreen`이 위치 이름을 받아 `finderNoticeText`가 글자를 채운다 (매개변수 이름·모양은 구현이 정한다. 미리보기도 함께 고친다).
5. **지울 것**: `data/location/DeviceCurrentLocationSource.kt`, `AndroidManifest.xml`의 `ACCESS_COARSE_LOCATION`·`ACCESS_FINE_LOCATION`.

## 단계

### 필수 (11/15 제출 APK·영상에 들어감)

- [ ] 1. 고정 위치로 바꾸기 (설계 1~3)
  - `feat(location): 폰·태블릿 시연·Real 위치를 서운로 62로 고정`
    - `LocationPlan.kt`(`fixedLocationOf`), `FakeCurrentLocationSource.PHONE_LOCATION`, `AppContainer`, `MainActivity`(권한 요청 제거, debug 출처 이름).
    - 테스트도 같은 커밋: `DeviceLocationPlanTest` → `FixedLocationPlanTest`(6칸 표 + "null ⇔ 내비" 규칙). `NearbyVetPlanBoundaryTest`의 기기 위치 케이스 3개를 고정 위치 기준으로 바꾼다 (Fake는 어디서나 시흥동, 차량 기기 시연·Real은 null, release + 일반 폰은 서운로). KDoc 첫 줄의 `usesDeviceLocation`도 고친다.
  - `chore(location): 기기 위치 코드와 위치 권한 제거`
    - `DeviceCurrentLocationSource.kt` 삭제, 매니페스트 권한 2줄 삭제, 남은 import 정리.
  - 확인: `./gradlew assembleDebug testDebugUnitTest`, 완료 기준 2·3.
- [ ] 2. 안내에 위치 이름 넣기 (설계 4)
  - `feat(ui): 시연용 위치 안내에 고정 위치 이름 표시`
  - 확인: `assembleDebug`. `/verify` 태블릿 T2·T3 (UI 변경이 있는 단위라 커밋 전에 본다).
- [ ] 3. 테스트 (`tester`) → `test(location): 고정 시연 위치 계약 테스트`
  - 넘길 것: 이 계획 경로, 대상 `di/LocationPlan.kt`·`AppContainer`의 위치 속성·`HospitalTexts`/`HospitalListScreen` 안내.
  - 볼 것: 6칸 표, release(`resolveDataMode(isDebugBuild = false, …)`) + 일반 폰 = 서운로, + 차량 기기 = 내비, 서운로에서 내장 시연 병원 순위가 U1 표와 같은지(A안일 때), 시흥동 Fake 근처 3곳이 Fake 모드에서 그대로 보이는지.
- [ ] 4. 화면 확인 (`/verify`, 아래 "확인 절차" T1~T5). 결과를 진행 로그에 적는다.
- [ ] 5. 문서 → `docs: 폰·태블릿 고정 시연 위치 반영`
  - `docs/architecture.md`: 39행 서비스 표 "현재 위치"(015 기기 위치 → 016 고정 위치), 93행 `LocationPlan.kt` 설명(`fixedLocationOf`), 124~125행 파일 목록(`DeviceCurrentLocationSource` 줄 삭제, Fake가 두 고정 위치를 준다), 206~214행 "현재 위치" 표 (폰 시연·Real = 서운로, 위치(Fake)).
  - `README.md`: 27행 위치 권한 문구 → "일반 폰·태블릿은 위치를 서울 서초구 서운로 62로 고정한다 (시연용). 위치 권한을 묻지 않는다". 43행 절에 같은 사실과 순위가 Pleos 에뮬레이터와 다르다는 한 줄 (U1 결과에 따라). 71행 테스트 위치 넣기 단계와 72행 "위치 권한 허용" 삭제.
  - `firebase/README.md` 57행: "시연 위치" → "Pleos 에뮬레이터 시연 위치(시흥동)" (`check-nearby.mjs`는 시흥동 그대로).
  - `docs/data/nearby-vets.md`: 54~61행 관찰 기록은 고치지 않는다. 차 앱의 폰 시연 위치가 서운로라는 한 줄만 더한다 (H2를 하면 그 결과도).
  - `docs/data/hospital-status.md` 80행: 기준 위치가 Pleos 에뮬레이터(시흥동)이고 폰·태블릿은 서운로라 순위가 다르다는 한 줄.
  - 015 노트(`docs/notes/CYJhub/2026-11-08-nearby-vet-search.md`)와 completed 계획은 고치지 않는다.
- [ ] 6. 개발 노트 작성 (`docs/notes/CYJhub/2026-11-09-fixed-demo-location.md`), 계획을 `completed/`로 옮김 → `docs(notes): 폰·태블릿 고정 시연 위치 개발 노트`, `docs(plans): 016 완료로 옮김`

### 있으면 좋음 (U1·시간에 따라)

- [ ] H1. (U1에서 B를 고르면) 폰 고정 위치용 시연 병원 배치. 시작 전에 이 계획에 설계를 따로 적는다.
- [ ] H2. debug 시연 + 로컬 서버(`emulators:nearby`)로 서운로 기준 근처 동물병원 개수·최대 거리 관찰 → `nearby-vets.md`에 기록. 필요하면 `check-nearby.mjs`에 서운로 점검 하나를 더한다.

## 확인 절차

일반 태블릿 에뮬레이터 (이 PC). 에이전트는 에뮬레이터를 직접 띄우지 않는다. 꺼져 있으면 사용자에게 켜 달라고 한다. 이 에뮬레이터에 015 때 넣은 테스트 GPS 위치가 남아 있어도 앱 결과가 같아야 한다.

| # | 구성 | 할 일 | 기대 |
|---|---|---|---|
| T1 | release APK (`/release-apk` 또는 `assembleRelease` 확인용 사본) | 앱 설치·실행 → 진료 카드 → [가장 빨리 진료받을 곳 찾기] | 권한 창 없음. 목록 이동 시간이 U1 표 정도 (A안: 1위 "시연용 동물병원" 약 9분). "시연용 위치(서초구 서운로 62) 기준이에요." |
| T2 | debug, 시연 모드 (서버 설정 없음) | 정차 → 병원 찾기, debug 패널 [위치 묻기] | T1과 같은 목록·안내. 패널 "위치(Fake): 37.49026, 127.02831 · 서초구 서운로 62" |
| T3 | debug, Fake 모드 | 정차 → 병원 찾기 | "시연용 위치(수정구 시흥동) 기준이에요.", 제휴 1위 바른 29분, Fake 근처 병원 3곳 (015와 같음) |
| T4 | debug, Real 모드 | debug 패널 [위치 묻기] | 서운로 값, 권한 창 없음, 크래시 없음 |
| T5 | debug, 시연 모드 | 안내 중 Fake [주행] | 주행 화면은 큰 남은 분 + 목적지 + 진행 막대뿐, debug 패널 위치 줄 없음. [정차]로 돌아오면 위치 줄이 다시 보인다 |
| (있으면) P1 | Pleos 에뮬레이터, debug 시연 | 병원 찾기 | 내비 위치(시흥동), "시연용 위치" 안내 없음, 012 순위 그대로 |

## 진행 로그

- 2026-11-09: 계획 작성 (planner).
- 2026-11-09: U1~U3 답 (CYJhub): U1 기본안 A (병원 배치 그대로, 화면에 위치 이름만 알림). U2 기본안 (Fake는 시흥동 그대로). **U3 "서초동"** (기본안 "서초구 서운로 62" 대신): `PHONE_LOCATION.address = "서초동"`, 안내 "시연용 위치(서초동) 기준이에요."

## 결정 / 배운 점

- 위치 표를 `currentLocationKindOf`(출처 종류)와 `fixedLocationOf`(고정 값) 두 함수로 둔다. 011의 표·테스트를 건드리지 않으려는 것이다.
