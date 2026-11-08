# nearby-vet-search

- 날짜: 2026-11-08
- 작성: CYJhub
- 브랜치: feat/nearby-vet-search
- 계획: docs/plans/completed/015-nearby-vet-search.md
- 커밋: 0b5e8bc..(merge 직전)

## 무엇을 했나

진료 카드에서 [가장 빨리 진료받을 곳 찾기]를 누르면, 지금 위치에서 5km 안의 **실제 동물병원**을 카카오 로컬 API로 찾아 제휴 병원과 한 목록에 "이동 + 대기"가 짧은 순으로 보여 준다. 5개씩 쪽으로 넘긴다. 근처 병원의 대기 시간은 시연용 예시이고 화면에 "(예시)"로 알린다. 근처 병원은 전화와 길안내만 되고, 진료 카드는 제휴 병원에만 보낸다.

## 어떻게: 핵심 결정과 이유

- **카카오 키는 Firebase Function `nearbyVets`에만 둔다** (사용자 결정) → Pleos 보안 가이드(키 하드코딩 금지)와 팀 결정. 배포(Blaze) 대신 PC의 Functions 에뮬레이터에서만 돌린다. 버린 대안: 앱에 키를 BuildConfig로 넣기 (APK에서 키가 보인다).
- **반경 5km, 최대 15곳** (사용자 결정) → 카카오 키워드 검색 한 페이지가 15곳이다. 동물병원 카테고리 코드가 없어 키워드 `동물병원` + `category_name`으로 거른다.
- **좌표를 약 100m로 반올림해 보내고 결과를 저장하지 않는다** (U2, 카카오 운영정책 5조 20호) → 차 위치가 처음으로 밖에 나가는 기능이라 최소화했다. 화면에 "현재 위치(약 100m 단위)를 카카오에 보내요. 저장하지 않아요."
- **내비 없는 기기(폰·태블릿)는 안드로이드 기기 위치** (사용자 결정, 위치 권한 추가) → "지금 위치 기준"을 이 PC의 일반 에뮬레이터에서도 보려고. 차량 기기는 그대로 Pleos 내비 위치.
- **근처 병원에 시연용 예시 대기를 붙여 제휴 병원과 섞고 5개씩 쪽** (사용자 결정, 체크포인트 뒤 설계 변경 V2) → "가장 빨리 진료받을 곳"을 실제 병원으로 보여 주려는 것. 실존 병원에 사실이 아닌 대기 정보가 실제처럼 보이지 않게 줄마다 "(예시)" (출품 유의사항). 예시 값은 병원 이름·좌표의 해시라 새로 고쳐도 순위가 흔들리지 않는다.
- **제휴 목록은 근처 검색을 기다리지 않는다**, 근처는 8초 시간 초과, 30초 갱신 때 다시 찾지 않는다 → 응급 화면이 느린 외부 호출에 묶이지 않게, 쿼터·폴링 최소화.
- 기존 `rankHospitals`·`HospitalPlan`은 바꾸지 않고 `nearbyVetRowsOf`·`nearbyVetKindOf`를 더했다 → 012 테스트를 건드리지 않는다.

## 사용한 Pleos SDK / API / 권한

- Pleos: 새 SDK·새 `pleos.car.permission.*` 없음. 차량 기기의 현재 위치는 기존 NaviHelper(011).
- 외부 API: 카카오 로컬 키워드 검색 `GET dapi.kakao.com/v2/local/search/keyword.json` (Function에서만). 확인 사항과 출처: `docs/data/nearby-vets.md`.
- Android 권한 (새로, 사용자 승인): `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION` (내비 없는 기기에서만 묻는다).
- 서버: `firebase-functions` 7.4.0, `firebase-admin` 14.5.0 (`firebase/functions/`에만).

## 확인

- 테스트: `testDebugUnitTest` 1014 → **1137개 모두 통과**, 함수 `node --test` **26개 모두 통과**. tester가 계약 기준 경계 테스트 106개(Android 87, 함수 19)를 더했고 버그 3개를 잡았다 → 고침 (`f404590`): 근처 검색이 없을 때도 순위 묶음 제한이 바뀌던 것(V1), 앱이 `schemaVersion` "1"·1.5를 1로 읽던 것, 함수가 "127.09abc" 같은 좌표를 받던 것.
- `/verify`: `assembleDebug testDebugUnitTest lintDebug` 통과 (새 린트 경고 1개 = 쓰지 않게 된 문구, 지움). 가로·라이트, 세로·다크로 목록·확인 화면 확인. 세로에서는 5개 + 쪽 버튼이 스크롤 없이 한 화면. 근처 검색 실패 → [다시 찾기] → 14곳 확인. 앱 에러 로그 0.
- 서버: `check-nearby` N1~N5 모두 PASS (시흥동 기준 15곳, 최대 2.80km), `preflight -- --nearby` PF7 PASS.
- 화면 (일반 태블릿 에뮬레이터 + PC Firebase 에뮬레이터, 시연 모드, 서울시청 위치): 제휴 5곳 + 카카오 14곳이 한 순위, 1위 카카오 병원 "이동 약 7분 + 대기 5분(예시)", 5개씩 4쪽. 카카오 병원 → 확인 화면([병원에 전화]·[길 안내 시작]) → 주행 화면 → "○○에 도착했어요" + "카드 없이 출발했어요". 전화 앱이 그 번호로 열림.
- 확인 못 한 것: release APK(E9, `/release-apk`는 사용자 실행), Pleos 에뮬레이터의 내비 위치로 근처 검색 (이 PC에 GPU가 없다).

## 시연 방법

1. `firebase/functions/.secret.local`에 카카오 REST 키 → `npm --prefix firebase run emulators:nearby` → `npm --prefix firebase run seed -- --reset` → `npm --prefix firebase run preflight -- --nearby --fix`.
2. `secrets.properties`에 `FIREBASE_PROJECT_ID=demo-meongcare`, `FIREBASE_EMULATOR_HOST=127.0.0.1`. debug 빌드 설치, debug 패널에서 **시연** 모드.
3. 이 PC: `& "$HOME\meongcare-run.ps1" -NoBuild -Lat 37.5665 -Lon 126.9780` (에뮬레이터 위치를 테스트 위치로 넣고 adb reverse 8080·9099·5001).
4. 진료 카드 → [가장 빨리 진료받을 곳 찾기] → 위치 권한 허용 → 목록 (5개씩) → 카카오 병원 → [길 안내 시작].

## 주의점 / 남은 일

- 이 PC의 태블릿 에뮬레이터(API 36)는 `adb emu geo fix`·Extended controls 위치가 GPS에 닿지 않는다 → 테스트 위치 기능(`cmd location providers set-test-provider-location`)으로 넣는다.
- Firebase 에뮬레이터는 Node 26을 거부한다 (Node 22로 바꿨다). Firestore 에뮬레이터는 JDK 11 이상 (JDK 21 확인).
- 앱을 막 켠 첫 서버 요청이 느린 에뮬레이터에서 5초를 넘겨 내장 병원으로 대신할 때가 있다 → [새로 고침].
- 카카오 병원 확인 화면의 안내 상자가 버튼 아래로 밀려 스크롤해야 보인다.
- 배포하면: Blaze, 비밀 관리자, App Check, 진짜 속도 제한. release에도 근처 병원을 넣을지 다시 정한다.
- 계획 번호 015를 원래 "연결 자격 암호화 저장"에 쓰려 했다 → 그 작업은 다음 계획 번호로.
