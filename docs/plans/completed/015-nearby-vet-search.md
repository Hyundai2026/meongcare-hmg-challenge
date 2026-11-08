# 015 근처 동물병원 검색 (카카오 로컬 API, Firebase Functions 에뮬레이터)

- 목표: 병원 목록 화면을 열면 현재 위치에서 반경 5km 안의 동물병원을 카카오 로컬 API로 찾아, 기존 제휴 병원 순위 아래에 "대기 정보 없음" 후보로 거리순으로 보여 준다. 카카오 호출은 Firebase Function(PC 에뮬레이터)이 하고, 고른 병원으로 전화와 길안내는 되지만 진료 카드는 보내지 않는다.
- 완료 기준 (어떻게 검증하나). `$BASE` = `14c3ee5` (이 브랜치를 딴 dev. rebase한 뒤에는 `origin/dev`).
  1. `node .claude/hooks/build-gate.mjs pipeline`이 "검증 파이프라인 통과."를 출력한다 (최신 dev 위로 rebase한 뒤).
  2. `secrets.properties`에 `FIREBASE_*` 값이 **없는** 상태에서 `.\gradlew.bat assembleDebug assembleRelease`가 성공한다.
  3. 단위 테스트 (`.\gradlew.bat testDebugUnitTest`):
     - 새 테스트 클래스가 통과한다: `NearbyVetRowsTest`, `NearbyVetPlanTest`, `FakeNearbyVetSourceTest`, `NearbyFunctionConfigTest`, `FunctionNearbyVetSourceTest`, `HospitalFinderNearbyTest`, `HospitalTripNearbyTest`, `HospitalTextsNearbyTest`, 그리고 tester가 더한 것.
     - 기존 테스트가 모두 그대로 통과한다. 개수는 단계 0에서 센 값이다.
     - `git diff $BASE --diff-filter=MD --name-only -- app/src/test`의 출력이 비어 있다 (기존 테스트를 고치거나 지우지 않는다. 새 동작은 새 테스트 클래스에 쓴다).
  4. 서버 함수: `npm --prefix firebase/functions test` (Node 내장 `node --test`)가 통과한다.
  5. Firebase 에뮬레이터 (PC):
     - `npm --prefix firebase run emulators:nearby`로 Auth·Firestore·**Functions** 에뮬레이터가 `127.0.0.1`에 뜬다. 기존 `npm --prefix firebase run emulators`는 그대로 동작한다.
     - `npm --prefix firebase run check-nearby`가 N1~N5를 모두 PASS로 출력한다 (아래 설계 결정 5). N2는 카카오 키가 있어야 한다.
     - `npm --prefix firebase run preflight -- --nearby`가 PF1~PF7을 PASS로 출력한다. `--nearby` 없이 돌리면 지금과 같은 PF1~PF6이다.
  6. 바뀌지 않아야 하는 것 (`git diff $BASE -- <경로>`가 비어 있다):
     - `app/.../domain/hospital/HospitalRanking.kt`, `TravelEstimate.kt`
     - `app/.../di/SourcePlan.kt`, `NaviPlan.kt`, `LocationPlan.kt`, `HospitalPlan.kt`, `DataMode.kt`
     - `app/.../data/guidance/navi/` 전체, `app/.../domain/location/`
     - `firebase/firestore.rules` (함수는 Firestore에 쓰지 않는다)
  7. 정적 검사 (Grep):
     - `dapi.kakao.com`은 `firebase/functions/` 아래에만 있다. `app/`, `web/`에 `KAKAO`(대소문자 무시)가 0건이다.
     - `git ls-files firebase/functions/.secret.local`의 출력이 비어 있고, `.gitignore`에 그 경로가 있다.
     - `HttpURLConnection`·`import java.net`은 `data/server/UrlConnectionHttpTransport.kt`에만 있다. `import org.json`은 `data/server/` 아래에만 있다.
     - `ui/` 아래에 `import com.meongcare.app.data`, `import com.meongcare.app.di`가 0건이다.
     - debug 병합 매니페스트의 `pleos.car.permission`은 지금처럼 2건(`NAVI_ROUTE`, `NAVI_ROUTE_SEARCH`)이다. release 병합 매니페스트에 `networkSecurityConfig`, `usesCleartextTraffic="true"`가 없다.
  8. 일반 태블릿 에뮬레이터 화면 확인: 아래 "확인 절차"의 E1~E9. `/verify`로 스크린샷을 남긴다.
  9. 주행 중 안전 (E3): 병원 목록·근처 병원 확인 화면에서 debug 패널로 주행 → 주행 화면 (debug 패널 영역을 빼면 클릭할 수 있는 노드 0개, `EditText` 0개). 정차 → 같은 목록·확인 화면.
  10. 로그 (E8): 우리 앱 pid 로그와 Functions 에뮬레이터 로그에 좌표·병원 이름·전화번호·토큰이 0건이다.
  11. 문서: `docs/data/nearby-vets.md`(새 문서)가 있고, `docs/architecture.md`, `docs/data/hospital-status.md`, `firebase/README.md`, `README.md`가 갱신돼 있다 (단계 11).
- 브랜치: `feat/nearby-vet-search` (dev `14c3ee5`에서 땄다)
- 관련 문서:
  - AGENTS.md: 아키텍처 규칙(외부 호출은 `data/` 래퍼 + Fake), 규칙 1·2·3·4·6·7
  - docs/architecture.md: "서비스 기능 스택"의 서버·병원 목록·병원 순위·현재 위치 행, "데이터 모드"(병원 목록 표, 현재 위치 표), "병원 웹", 패키지 구조
  - docs/data/hospital-status.md "나중 후보": 카카오 결과는 저장하지 않고 차 앱 안에서만 "대기 정보 없음" 후보
  - docs/plans/completed/012-fastest-hospital.md: 설계 결정 2(순위 규칙의 `unranked`), 3(REST + `HttpTransport`), 4(데이터 모드), 5(형식 문서 규칙), 6(로그)
  - docs/plans/completed/014-owner-link.md: 서버 클라이언트(`serverClient`)를 병원·반려견이 같이 쓰는 방식
  - firebase/README.md: 에뮬레이터 명령, IPv4 고정, preflight PF1~PF6
  - 코드: `domain/hospital/`(Hospital, HospitalStatus, HospitalRanking, TravelEstimate, HospitalBoardSource), `domain/trip/HospitalTrip.kt`, `ui/hospital/`(HospitalFinderViewModel, HospitalFinderUiState, HospitalListScreen, HospitalConfirmScreen, HospitalTexts), `di/AppContainer.kt`, `data/server/`(ServerConfig, AnonymousAuth, HttpTransport), `MainActivity.kt`, `firebase/`(firebase.json, package.json, scripts/emulator.mjs, scripts/preflight.mjs)

## 사용자 결정 (2026-11-08, CYJhub)

- D1. **키 위치**: 카카오 REST 키는 앱에 넣지 않는다. 카카오는 Firebase Function이 부르고, 개발·시연은 PC의 Firebase 에뮬레이터(Functions 포함)에서 돌린다. Blaze 실제 배포는 하지 않는다.
  - debug 빌드(시연 모드 + `secrets.properties`에 에뮬레이터 값)에서만 실제 병원이 나온다.
  - 서버 없는 release APK는 지금처럼 시연 병원만 나온다 (근처 병원 묶음 없음).
  - 키는 Functions 에뮬레이터용 비밀 파일(gitignore 대상)에 둔다.
- D2. **반경**: 5km.
- D3. **목록 표시**: 제휴 병원 순위(이동 어림 + 대기)는 그대로 둔다. 카카오 병원은 "대기 정보 없음" 후보로 거리순으로 그 아래에 붙인다. 전화·길안내는 되고, 진료 카드 전송은 제휴 병원에만 한다.

## 확인 필요 (시작 전에 해결)

### 사용자에게 물을 것

- [사용자 승인 필요] **U1. 새 npm 의존성**: `firebase/functions/package.json`에 `firebase-functions`, `firebase-admin`을 정확한 버전으로 고정해 넣는다. 앱(`app/`)과 웹(`web/`)에는 아무것도 더하지 않는다. 버전은 단계 0의 F2에서 정한다.
- [사용자 승인 필요] **U2. 차 위치가 밖으로 나간다**: 지금까지는 "차의 위치를 보내지 않는다"였다 (012 U3, 병원에 보내는 카드 기준). 이번에는 근처 병원을 찾으려고 현재 좌표를 우리 Function → 카카오로 보낸다. 기본안:
  - 좌표를 소수 셋째 자리(약 100m)로 반올림해서 보낸다. 반경 5km 검색에는 충분하다.
  - 함수·앱 로그에 좌표를 남기지 않는다. 함수는 아무것도 저장하지 않는다.
  - 근처 병원 묶음 위에 작은 글씨로 "근처 병원을 찾으려고 현재 위치(약 100m 단위)를 카카오에 보내요"를 보인다.
  - 이 기본안대로 갈지, 따로 켜는 동의 버튼이 필요한지 정한다.
- [사용자 승인 필요] **U3. Fake 모드의 가짜 근처 병원**: Fake 모드(debug 전용)에서 "시연용 근처 동물병원" 3곳을 보일지 정한다. 기본안은 보인다(시연용 표시 포함). 그래야 GPU·서버 없이 화면을 만들고 확인할 수 있다. release(시연 모드)에는 들어가지 않는다.
- [사용자 승인 필요] **U4. 시연 영상·기획서에 실제 병원**: debug 빌드로 찍으면 실제 동물병원 이름과 전화번호가 화면에 나온다. 영상과 기획서 캡처에 그대로 쓸지, 근처 병원 묶음을 가리거나 Fake로 찍을지 정한다. K6(카카오 정책)의 답에 따라 달라질 수 있다.
- [사용자 작업] **U5. 카카오 키 발급**: 누구 계정으로 카카오 Developers 앱을 만들지 정한다. REST API 키를 받아 `firebase/functions/.secret.local`에만 넣는다 (커밋하지 않는다). 카카오맵/로컬 사용 설정이 따로 필요한지는 K7.
- [사용자 작업] **U6. JDK 21 설치 (이 PC)**: `firebase/README.md`는 Firestore 에뮬레이터를 JDK 21로 확인했다고 적는다. 이 PC에는 없다. 단계 0에서 `java -version`으로 확인하고, 없으면 Temurin 21 같은 JDK 21을 설치한다.
  - Android 빌드는 JDK 17 소스·타깃 그대로다. Gradle이 쓰는 JDK(`JAVA_HOME` 또는 Android Studio JBR)를 바꾸지 않게, 에뮬레이터 터미널에서만 JDK 21이 `PATH` 앞에 오게 하는 방법을 firebase/README.md에 적는다.
  - firebase-tools 15.34.1이 정말 JDK 21을 요구하는지는 F1에서 확인한다.

새 `pleos.car.permission.*`은 필요 없다. 현재 위치는 이미 승인된 `NAVI_ROUTE`(011)를 쓰고, 인터넷 권한은 012에서 넣었다.

### [선행: 메인 웹 확인(카카오)] 카카오 로컬 API (단계 5·7 전에)

`pleos-docs-researcher`는 Pleos 문서 전용이다. 아래는 **메인 에이전트가 카카오 Developers 공식 문서를 웹으로 확인**하고, 출처 URL과 확인 날짜를 붙여 `docs/data/nearby-vets.md` "카카오 로컬 API (확인한 것)"에 적는다 (단계 0-3). 계획 안의 값은 모두 확인 전 기본안이다.

- K1. 엔드포인트와 인증: 키워드 검색과 카테고리 검색의 주소, 메서드, 인증 헤더 형식(`Authorization: KakaoAK <키>`인지).
- K2. 동물병원 카테고리 그룹 코드가 있는가? 없으면 키워드 검색(`query=동물병원`)을 쓴다. 키워드 결과에서 동물병원만 남기려면 어느 필드(예: `category_name`)를 보면 되는가?
- K3. 파라미터: 중심 좌표(`x`=경도, `y`=위도 순서인지), 반경(`radius`, 단위·최대값), 정렬(`sort=distance`가 중심 좌표를 요구하는지), `size` 최대값, `page` 최대값.
- K4. 응답 필드: 이름·전화·좌표·주소·거리 필드 이름과 타입(좌표가 문자열인지), 전화가 없을 때의 값, `meta`의 끝 여부 필드.
- K5. 쿼터: 일일·초당 호출 한도, 넘으면 오는 HTTP 상태와 오류 형식.
- K6. 정책:
  - 결과를 저장·캐시하면 안 되는 범위. 메모리에 잠깐 들고 화면에 보이는 것, 서버 함수가 중계만 하는 것은 되는가?
  - 출처 표시 의무 (로고·문구·위치).
  - 결과 좌표로 다른 내비(Pleos NaviHelper)에 길안내를 요청해도 되는가?
  - 공모전 시연 영상·기획서에 결과 화면을 써도 되는가 (U4).
- K7. 앱 등록: REST 키를 서버에서 쓸 때 플랫폼 등록·허용 IP 같은 설정이 필요한가? 카카오맵/로컬 API를 앱 설정에서 따로 켜야 하는가?

### [선행: 메인 웹 확인(Firebase)] Functions 에뮬레이터 (단계 5 전에)

Firebase 공식 문서와 npm을 메인이 확인하고, 결과를 `firebase/README.md`와 이 계획의 "결정 / 배운 점"에 적는다.

- F1. firebase-tools 15.34.1의 에뮬레이터가 요구하는 Java 버전. Functions 에뮬레이터만 쓰면 Java가 필요 없는지.
- F2. `firebase-functions`, `firebase-admin`의 지금 버전, 지원 Node 런타임(22 지원 여부), ESM(`"type": "module"`) 지원 여부. firebase-tools 15.34.1과 맞는 조합.
- F3. 비밀값: `defineSecret`을 쓰면 Functions 에뮬레이터가 `functions/.secret.local`에서 값을 읽는가? `demo-` 프로젝트 ID(`demo-meongcare`)에서도 되는가?
- F4. callable(`onCall`) HTTP 규약: 요청 몸통 `{"data": ...}`, 헤더 `Authorization: Bearer <ID 토큰>`, 성공 `{"result": ...}`, 실패 `{"error": {"status", "message"}}`와 HTTP 상태 대응. Functions 에뮬레이터가 **Auth 에뮬레이터의 익명 토큰**을 검증해 `request.auth`를 채우는가?
- F5. Functions 에뮬레이터 주소 형식(`http://<host>:5001/<project>/<region>/<함수 이름>`인지), 기본 포트, `firebase.json`에서 host를 `127.0.0.1`로 고정하는 방법.
- F6. `demo-` 프로젝트의 Functions 에뮬레이터에서 바깥 HTTP(카카오) 호출이 막히지 않는가?
- F7. 에뮬레이터만 쓸 때 Blaze가 필요 없는가 (배포에만 필요한지).

Pleos API는 새로 쓰지 않는다. `getCurrentLocationInfo`는 011에서 확인했고 sdk-notes.md에 있다.

## 범위

- **필수** (11/08~11/12): 단계 0~12.
  - 11/08~11/09: 단계 0~4 → **체크포인트 A: Fake 모드로 근처 병원 묶음·확인 화면·출발까지 화면이 끝난다.** 서버·키 없이 시연 영상 백업을 찍을 수 있다.
  - 11/10~11/11: 단계 5~8 → Functions 에뮬레이터 + 카카오 + 앱 연결, 끝까지 확인.
  - 11/12: 단계 9~12 (테스트, 화면 확인, 문서, 노트). 11/13~11/15는 기획서·영상·APK 여유.
  - 11/15 제출물에 주는 것:
    - 기획서 "기술 활용성": 외부 API(카카오 로컬)를 키를 숨긴 서버 함수로 쓴다. Pleos 현재 위치가 검색 중심이다.
    - 기획서 "사업성": 제휴 병원(대기 정보) vs 비제휴 병원(대기 정보 없음)의 차이를 한 화면에서 보인다. 제휴의 가치를 설명할 근거가 된다.
    - 시연 영상: debug 빌드로 실제 근처 병원이 나오는 장면 (U4에 따라).
    - APK: release는 바뀌지 않는다 (D1). 제출 APK의 위험이 늘지 않는다.
- **있으면 좋음**: 아래 A~E (필수가 끝난 뒤, 단계 11 전에).
- **시간이 모자라면 이 순서로 자른다** (위가 먼저):
  1. 있으면 좋음 전부
  2. 단계 6의 preflight PF7 → firebase/README.md에 손으로 하는 점검 순서만 적는다
  3. 단계 6의 `check-nearby` 스크립트 → Emulator UI·curl로 N1~N5를 손으로 해 보고 진행 로그에 적는다
  4. (최후) 단계 5~8 전체 → Fake 근처 병원(시연용 표시)으로만 제출하고, 기획서에는 "카카오 로컬 연동은 서버 함수로 PT 전 완성"이라고 사실대로 쓴다
  - 자르지 않는 것: 1~4(도메인·UI·안전 규칙), 7의 키 비노출, 9, 11, 12.
- **이번 범위 밖**:
  - Functions 실제 배포, Blaze 요금제, 진짜 Firebase 프로젝트 연결.
  - release APK에서 근처 병원 보이기.
  - 카카오 결과 저장·캐시 (정책상 하지 않는다).
  - 카카오 병원에 진료 카드 보내기, 카카오 병원의 대기 시간.
  - 주행 중 검색, 주행 화면 변경.
  - 영업시간·야간 응급 여부 필터 (카카오 응답에 있는지 모름).
  - Pleos 에뮬레이터·실차 확인 (이 PC에 GPU 에뮬레이터가 없다. 있으면 좋음 E).
  - 연결 코드 마구 대입 제한 (Functions가 생겨도 이 계획에서는 하지 않는다. architecture.md "Firebase로 옮기기"의 남은 것 그대로).

## 설계 결정 (planner 제안과 근거)

코드 블록은 기본안이다. 이름과 세부는 구현하면서 바꿔도 된다. 바꾸면 이 블록을 고치고 "결정 / 배운 점"에 이유를 남긴다. tester는 아래 "계약"을 기준으로 테스트를 쓴다.

### 1. 기존 순위 규칙은 건드리지 않고 묶음을 하나 더한다

- `rankHospitals`(012)와 `HospitalRanking`은 그대로 둔다 (완료 기준 6). 카카오 병원을 `rankHospitals`에 넣으면 제휴 병원의 "대기 정보가 없거나 지난 곳" 묶음과 섞이고, `maxPerGroup` 10곳 제한에 걸린다.
- 대신 근처 병원은 따로 받아 따로 정리한다 (`nearbyVetRowsOf`, 순수 함수). 화면 순서: 순위 → 제휴의 대기 정보 없음·지남 → 응급 접수 안 함 → **근처 동물병원 (대기 정보 없음)**.

### 2. 도메인 계약 (`domain/hospital/`, `android` import 없음)

```kotlin
// HospitalStatus.kt: 값 하나를 더한다
enum class HospitalOrigin { SERVER, BUILT_IN, BUILT_IN_FALLBACK, NEARBY_SEARCH }

data class HospitalCandidate(...) {
    val sendsCardToServer: Boolean get() = origin == HospitalOrigin.SERVER   // 그대로
    /** 진료 카드를 보낼 수 있는 병원인지. 근처 검색 병원(제휴 아님)은 false */
    val canReceiveCard: Boolean get() = origin != HospitalOrigin.NEARBY_SEARCH
}
```

```kotlin
// NearbyVetSource.kt
/** 근처 동물병원 검색 반경 (사용자 결정 D2). 서버 함수도 같은 값을 쓴다 */
const val NEARBY_RADIUS_METERS = 5_000

sealed interface NearbyVets {
    /** 찾았다. 비어 있을 수 있다 (반경 안에 없음). 순서는 보장하지 않는다 */
    data class Found(val hospitals: List<Hospital>) : NearbyVets
    /** 찾지 못했다. reason은 로그용 짧은 이유 ("시간 초과", "HTTP 429", "형식 오류"). 좌표·이름을 넣지 않는다 */
    data class Unavailable(val reason: String) : NearbyVets
}

fun interface NearbyVetSource {
    /**
     * [center]에서 반경 [NEARBY_RADIUS_METERS] 안의 동물병원을 한 번 찾는다. 부를 때마다 새로 찾는다 (캐시·저장 없음).
     * 취소 말고는 예외를 던지지 않는다.
     */
    suspend fun search(center: CurrentLocation): NearbyVets
}
```

```kotlin
// NearbyVetRows.kt (순수 함수)
fun nearbyVetRowsOf(
    found: List<Hospital>,
    from: CurrentLocation,
    partners: List<HospitalCandidate>,
    max: Int = 15,
): List<RankedHospital>
```

`nearbyVetRowsOf` 계약 (`NearbyVetRowsTest`):

| 경우 | 규칙 |
|---|---|
| 줄 모양 | `RankedHospital(candidate = HospitalCandidate(h, status = null, origin = NEARBY_SEARCH), travel = estimateTravel(from, h.destination), freshness = NONE, statusAge = null)` |
| 순서 | 직선 거리(`TravelEstimate.straightDistanceKm`) 짧은 순 → 이름 → 위도 → 경도. 같은 입력이면 항상 같은 순서 |
| 반경 밖 | 직선 거리가 5.5km(반경 + 여유 500m)보다 먼 곳은 뺀다 (서버 결과를 믿지만 한 번 더 막는다). 정확히 5.5km는 남긴다 |
| 제휴 병원과 같은 곳 | 이름이 같고(앞뒤 공백·가운데 공백을 뺀 뒤 비교) 직선 100m 안인 제휴 병원이 `partners`에 있으면 뺀다 (제휴 쪽이 대기 정보가 있다) |
| 카카오 안에서 중복 | 이름과 좌표가 같으면 하나만 |
| 개수 | 정리한 뒤 앞에서 `max`곳 |
| 빈 입력 | 빈 목록 |

### 3. 데이터 모드: 근처 병원 출처 (`di/NearbyVetPlan.kt`, 순수 함수)

`HospitalPlan`(012)은 바꾸지 않는다 (완료 기준 6). 같은 방식으로 표를 하나 더한다.

```kotlin
enum class NearbyVetKind { OFF, FAKE, FUNCTION }
fun nearbyVetKindOf(mode: DataMode, hasFunction: Boolean): NearbyVetKind
```

| 모드 | 함수 설정 있음 (`hasFunction`) | 없음 |
|---|---|---|
| Fake (debug 전용) | FAKE | FAKE (U3 기본안. U3가 "안 보인다"면 OFF) |
| 시연 (release·debug) | FUNCTION | OFF |
| Real | FUNCTION | OFF |

- `hasFunction` = 함수 주소(설계 결정 6의 `nearbyFunctionUrlOf`)가 null이 아니고 서버 클라이언트(`serverClient`, 익명 계정)가 있다.
- release는 함수 주소가 늘 null이다 → 시연 모드여도 OFF. **release 화면은 지금과 같다** (D1).
- OFF면 목록 화면에 근처 병원 묶음이 아예 없다.
- `NearbyVetPlanTest`: 표의 6칸 전부.

### 4. 화면 상태와 ViewModel (`ui/hospital/`)

```kotlin
// HospitalFinderUiState.kt
sealed interface NearbyState {
    data object Off : NearbyState                 // 기능 없음: 묶음을 보이지 않는다
    data object NeedsLocation : NearbyState       // 위치를 몰라 찾지 않았다
    data object Searching : NearbyState
    data class Found(val rows: List<RankedHospital>) : NearbyState   // 비어 있지 않다
    data object Empty : NearbyState               // 5km 안에 없다 (또는 정리 뒤 0곳)
    data class Failed(val isRetrying: Boolean = false) : NearbyState
}

data class Loaded(
    val ranking: HospitalRanking,
    val origin: HospitalOrigin,
    val isRefreshing: Boolean = false,
    val isLocating: Boolean = false,
    val nearby: NearbyState = NearbyState.Off,    // 새 필드 (기본값이라 기존 테스트가 그대로 통과)
) : HospitalFinderUiState
```

```kotlin
class HospitalFinderViewModel(
    boardSource: HospitalBoardSource,
    locationSource: CurrentLocationSource,
    now: () -> Instant = Instant::now,
    refreshEvery: Duration = 30.seconds,
    nearbySource: NearbyVetSource? = null,        // null = Off (기존 호출은 그대로)
    nearbyTimeout: Duration = 8.seconds,
)
```

ViewModel 계약 (`HospitalFinderNearbyTest`, 가상 시간):

| # | 경우 | 기대 |
|---|---|---|
| V1 | `nearbySource == null` | 모든 `Loaded`의 `nearby == Off`. 내보내는 상태가 지금과 같다 |
| V2 | 위치를 알고 근처 검색이 느림 | 제휴 목록 `Loaded(nearby = Searching)`이 **근처 결과를 기다리지 않고** 먼저 나온다. 결과가 오면 같은 `ranking`에 `nearby = Found(...)` |
| V3 | 근처 검색 실패 (`Unavailable`) | `nearby = Failed()`. `ranking`·`origin`은 그대로 |
| V4 | 근처 검색이 `nearbyTimeout`(8초)을 넘김 | `nearby = Failed()`. 제휴 목록은 그 전에 이미 보였다 |
| V5 | 근처 결과 0곳, 또는 정리(`nearbyVetRowsOf`) 뒤 0곳 | `nearby = Empty` |
| V6 | 위치 모름 (`locationSource.current() == null`) | `nearbySource.search`를 **부르지 않는다**. `nearby = NeedsLocation`. 다른 위치로 대신 찾지 않는다 (011 규칙) |
| V7 | V6 뒤 `onRelocate()`로 위치를 얻음 | 그때 한 번 찾는다 (`Searching` → 결과) |
| V8 | 30초 목록 갱신(ticker) | 근처 검색을 **다시 부르지 않는다** (쿼터·폴링 최소화). 갱신된 `ranking`에 이전 `nearby`를 그대로 둔다 |
| V9 | `onRefresh()` | 제휴 목록을 다시 받는다. `nearby`가 `Failed`일 때만 근처도 다시 찾는다 (`Failed(isRetrying = true)` → 결과). `Found`·`Empty`면 다시 찾지 않는다 |
| V10 | 화면이 사라짐 (구독 끝) | 진행 중인 근처 검색을 취소한다. 다시 보이면 처음처럼 다시 찾는다 |
| V11 | 제휴 목록 실패 (`Failed`, Real 모드) | 지금과 같다 (`Failed` 상태에는 근처 묶음이 없다. 알려진 한계) |
| V12 | `nearbyVetRowsOf`에 넘기는 `partners` | 마지막으로 받은 제휴 목록(`HospitalBoard.Loaded.candidates`) |

### 5. 서버 함수 (`firebase/functions/`, 기본안. F2~F5 확인 뒤 확정)

- 이름 `nearbyVets`, **callable(`onCall`)**, 지역 `asia-northeast3`(Firestore 서울과 같게).
  - callable을 고른 이유: 익명 Auth 토큰 검증을 프레임워크가 해서 `request.auth`로 바로 쓴다 (F4). HTTP(`onRequest`) + `verifyIdToken`은 코드가 더 많다.
  - 앱은 Firebase SDK 없이 callable 규약대로 HTTP POST를 보낸다 (012 설계 결정 3과 같은 이유: 런타임 의존성 없음).
- 언어: 의존성을 줄이려고 **JavaScript(ESM, 빌드 단계 없음)**. `firebase/scripts/*.mjs`와 같다.
- 파일:

| 파일 | 하는 일 |
|---|---|
| `firebase/functions/package.json` | `firebase-functions`, `firebase-admin` 정확한 버전 (U1, F2). `engines.node`. 스크립트 `test` = `node --test` |
| `firebase/functions/index.js` | `onCall` 등록, 인증·입력 검사, 속도 제한, 카카오 호출, 응답 |
| `firebase/functions/nearby.mjs` | 순수 함수: `parseInput(data)`, `roundCoordinate(x)`, `toVets(kakaoJson)`, `RateLimiter`. 네트워크·Firebase 모름 |
| `firebase/functions/nearby.test.mjs` | `node:test`로 위 순수 함수 시험 (손으로 만든 가짜 카카오 응답. **실제 카카오 응답을 저장하지 않는다**, K6) |
| `firebase/functions/.secret.local.example` | `KAKAO_REST_API_KEY=` (빈 값, 커밋) |
| `firebase/functions/.secret.local` | 진짜 키. **gitignore** |

- 요청·응답 형식은 `docs/data/nearby-vets.md`에 둔다 (pet-profile.md와 같은 규칙: 문서가 기준, `schemaVersion: 1`, 모르는 필드는 무시, 읽는 쪽은 짐작하지 않는다):

```json
// 요청 (callable 몸통)
{ "data": { "latitude": 37.4127212, "longitude": 127.094681 } }
// 성공
{ "result": { "schemaVersion": 1, "radiusMeters": 5000,
  "vets": [ { "name": "○○동물병원", "phone": "031-000-0000", "latitude": 37.41, "longitude": 127.09, "address": "경기 성남시 …" } ] } }
```

- 함수 계약 (`nearby.test.mjs` + `check-nearby`):

| # | 경우 | 결과 |
|---|---|---|
| S1 | 로그인 안 함 (`request.auth` 없음) | `unauthenticated` |
| S2 | 위도·경도가 숫자가 아니거나 범위 밖(위도 -90~90, 경도 -180~180), NaN | `invalid-argument` |
| S3 | 같은 uid가 5초 안에 다시 부름, 또는 1시간에 30번 넘게 | `resource-exhausted`. 기억은 프로세스 메모리 (에뮬레이터는 한 프로세스라 충분. 배포하면 다시 정한다) |
| S4 | 키가 없음 (`.secret.local` 없음·빈 값) | `failed-precondition` |
| S5 | 카카오 호출 | 좌표를 소수 셋째 자리로 반올림(U2) → 키워드 `동물병원`, 반경 5000, 거리순, 한 페이지(크기는 K3의 최대값, 15 예상). 시간 초과 4초. 반경·개수는 **클라이언트가 정하지 못한다** |
| S6 | 카카오 HTTP 오류·시간 초과 | `unavailable` (카카오 응답 본문을 그대로 돌려주지 않는다) |
| S7 | 카카오 결과 변환 (`toVets`) | 이름이 비면 뺀다. 좌표가 숫자로 바뀌지 않으면 뺀다. 전화가 빈 문자열이면 `null`. 동물병원이 아닌 항목은 K2에서 정한 필드로 뺀다. 최대 15곳 |
| S8 | 저장 | Firestore·파일에 아무것도 쓰지 않는다. 캐시 없음 |
| S9 | 로그 | 결과 종류·개수·걸린 ms·카카오 HTTP 상태만. 좌표·병원 이름·전화·uid·키 없음 |

- 남용 방지는 S1·S3·S5까지가 최소한이다. App Check, 진짜 IP 제한은 배포할 때 다시 정한다 (범위 밖).
- 에뮬레이터 설정:
  - `firebase.json`: `"functions": { "source": "functions" }`, `emulators.functions`를 `127.0.0.1:5001`에 고정 (F5).
  - `firebase/package.json`: 새 스크립트 `emulators:nearby` = `firebase emulators:start --project demo-meongcare --only auth,firestore,functions`, `check-nearby` = `node scripts/check-nearby.mjs`. **기존 `emulators`는 그대로 둔다** (함수 의존성을 설치하지 않은 팀원도 지금처럼 쓴다).
  - 처음 한 번: `npm --prefix firebase/functions ci`.
- `firebase/scripts/check-nearby.mjs` (의존성 없음, `emulator.mjs`의 익명 가입을 쓴다):
  - N1 로그인 없이 부르면 `unauthenticated`
  - N2 익명 계정 + 시연 위치(37.4127212, 127.094681)로 부르면 `vets`가 배열이고 모두 5.5km 안 (개수만 출력, 이름 출력 안 함)
  - N3 위도 `999`면 `invalid-argument`
  - N4 N2 직후 같은 계정으로 다시 부르면 `resource-exhausted`
  - N5 응답에 `schemaVersion: 1`, `radiusMeters: 5000`
- preflight (`--nearby`일 때만): PF7 `127.0.0.1:5001`이 2초 안에 응답, PF5에 `tcp:5001` 추가 (`--fix`면 `adb reverse tcp:5001 tcp:5001`).

### 6. 앱의 함수 호출 (`data/server/`, Real 래퍼)

```kotlin
// NearbyFunctionConfig.kt (순수)
/**
 * 근처 병원 함수 주소. 배포하지 않으므로(D1) debug + 에뮬레이터 주소가 있을 때만 있다.
 * `http://<host>:5001/<projectId>/asia-northeast3/nearbyVets` (F5 확인 뒤 확정). 그 밖은 null.
 */
fun nearbyFunctionUrlOf(projectId: String, emulatorHost: String, isDebugBuild: Boolean): String?
```

`NearbyFunctionConfigTest`: 프로젝트 ID가 비면 null / release면 호스트가 있어도 null / debug + 호스트 없음 null / IPv6 호스트는 `[...]`로 감싼다 (`serverConfigOf`와 같은 규칙) / 앞뒤 공백 무시.

```kotlin
// FunctionNearbyVetSource.kt
class FunctionNearbyVetSource(
    private val url: String,
    private val auth: AnonymousAuth,          // 병원·반려견과 같은 익명 계정 (014 serverClient)
    private val transport: HttpTransport,
    private val log: (String) -> Unit,
) : NearbyVetSource
```

`FunctionNearbyVetSourceTest` (가짜 `HttpTransport`, 가짜 익명 가입 응답):

| 경우 | 기대 |
|---|---|
| 요청 | `POST url`, 헤더 `Authorization: Bearer <idToken>`, 몸통 `{"data":{"latitude":…,"longitude":…}}`. 반올림은 서버가 한다 (앱은 그대로 보낸다) |
| 익명 계정 없음 (`auth.session() == null`) | `Unavailable("인증 실패")`, 함수를 부르지 않는다 |
| 200 + 올바른 `result` | `Found`. 각 항목 → `Hospital(Destination(name, lat, lon, address), phoneNumber = phone?.ifBlank { null }, isSample = false, id = null)` |
| 항목 하나가 틀림 (이름 빔·41자 넘음, 좌표 범위 밖·문자열, 필드 없음) | 그 항목만 뺀다 |
| `schemaVersion`이 1이 아님, `result`·`vets` 없음, JSON 아님 | `Unavailable("형식 오류")` |
| HTTP 401·403·429·500, callable `error` 몸통 | `Unavailable("HTTP <상태>")` |
| transport 예외 (연결 실패) | `Unavailable("네트워크")`. 예외를 밖으로 던지지 않는다 (취소는 다시 던진다) |
| 로그 | `근처 병원: N곳 (Xms)` / `근처 병원: 실패 (HTTP 429)` 꼴. 좌표·이름·토큰 없음 |

- 시간 초과는 ViewModel의 `nearbyTimeout`(8초)과 `UrlConnectionHttpTransport`의 연결·읽기 5초가 맡는다. 카카오 4초 + 함수 시작 시간을 생각해 8초로 둔다. 단계 8에서 잰 시간의 3배가 넘으면 올린다.
- 평문 HTTP: debug의 `network_security_config`가 이미 `10.0.2.2`·`127.0.0.1`·`localhost`를 허용한다 (포트 무관). 새 설정 없음. `adb reverse tcp:5001 tcp:5001`.
- `secrets.properties` 새 키 없음: 함수 주소는 기존 `FIREBASE_PROJECT_ID`·`FIREBASE_EMULATOR_HOST`로 만든다.

### 7. Fake 구현 (`data/hospital/FakeNearbyVetSource.kt`)

- 생성자로 결과를 바꿀 수 있다: `FakeNearbyVetSource(delay: Duration = 600.milliseconds, result: (CurrentLocation) -> NearbyVets = { 기본 3곳 })`. 기본 지연은 화면에서 "찾는 중"을 볼 수 있게 짧게 둔다.
- 기본 3곳: 시연 위치(수정구 시흥동) 기준 직선 약 1km·2.5km·4km, 이름 "시연용 근처 동물병원 가/나/다", `isSample = true`, 전화 `02-0000-0010`~`0012` (009와 같이 걸리지 않는 형식), 하나는 전화 없음(`null`)으로 [병원에 전화]가 숨는 것을 보인다. 좌표는 단계 2에서 도로 위 지점인지 지도로 확인한다.
- 실제 카카오 응답을 본떠 저장하지 않는다 (가상의 이름·좌표).
- `FakeNearbyVetSourceTest`: 기본 3곳이 모두 `nearbyVetRowsOf`를 통과해 남고(5.5km 안), 거리순이 가/나/다다.

### 8. 화면 (정차 중에만. 주행 화면은 바꾸지 않는다, 규칙 3)

- **병원 목록** (`HospitalListScreen`): 기존 묶음 아래에 "근처 동물병원 · 대기 정보 없음 (5km 안, 가까운 순)" 제목과 상태별 내용.

  | `nearby` | 보이는 것 |
  |---|---|
  | `Off` | 아무것도 없음 (지금 화면과 같다) |
  | `Searching` | 작은 진행 표시 + "근처 동물병원을 찾는 중…" |
  | `Found` | 줄마다 기존 `HospitalRow`와 같은 큰 터치 타깃. 순위 자리 "–", "이동 약 N분 · 대기 정보 없음", 큰 합계 글씨 없음. `isSample`이면 "시연용" |
  | `Empty` | "5km 안에서 찾은 동물병원이 없어요" |
  | `NeedsLocation` | "현재 위치를 몰라 근처 동물병원은 찾지 않았어요" (위 기존 [위치 다시 찾기]가 함께 찾는다) |
  | `Failed` | "근처 동물병원을 찾지 못했어요" + [다시 찾기] (= `onRefresh`). `isRetrying`이면 진행 표시 |

  - 묶음 위 작은 글씨: U2 위치 안내 문구, K6에서 정한 출처 표시(기본안 "장소 정보: 카카오"). `Off`면 둘 다 없다.
  - 줄 문구는 `HospitalTexts`의 순수 함수로 고른다 → `HospitalTextsNearbyTest` (예: `nearbySectionOf(state): NearbySection` 열거, `hospitalRowValuesOf`에 `isNearby`). 기존 `HospitalTextsTest`는 고치지 않는다.
  - 텍스트 입력 없음. 정차 중이라 세로 스크롤은 지금처럼 된다.
- **근처 병원 확인 화면** (`HospitalConfirmScreen`, `choice.candidate.canReceiveCard == false`일 때):
  - 병원 이름, "이동 약 N분 · 대기 정보 없음", "시연용"(Fake일 때).
  - 안내: "제휴 병원이 아니라 진료 카드를 보낼 수 없어요." / "대기 정보가 없어요. 출발 전에 전화로 진료할 수 있는지 확인해 주세요." / 출처 표시.
  - 버튼: [뒤로], [병원에 전화](번호가 있을 때), **[길 안내 시작]** = `trip.depart(choice, card = null)`. [출발]·[카드 없이 길안내만]은 보이지 않는다.
  - 제휴 병원 확인 화면은 지금과 같다.
- **출발 뒤**: 카드 화면의 전송 줄은 기존 "카드 없이 출발했어요"(`CardDelivery.NotSent`)를 그대로 쓴다. 문구 바꾸기는 있으면 좋음 A.
- **`HospitalTrip` 계약** (`HospitalTripNearbyTest`):
  - `depart(choice, card)`에서 `choice.candidate.canReceiveCard == false`면 `card`가 있어도 무시한다: `delivery = NotSent`, `senderFor`를 부르지 않는다, 병원 확인 읽기(ack)를 하지 않는다.
  - 길안내는 그대로 시작한다 (`Destination`에 `expectedTravel = choice.travel`).
  - 도착·취소 때 아무것도 보내지 않는다.
- **주행 중**: 목록·확인 화면은 `parkedContentOf`의 HOSPITALS·CONFIRM_HOSPITAL이라 주행이면 주행 화면이 먼저다 (012 그대로). 주행 화면에는 길안내 목적지 이름만 나온다. 근처 검색은 목록 화면이 보일 때만 돌고(V10), 주행 화면에서는 돌지 않는다.

### 9. 저장·로그·키

- 카카오 결과는 `HospitalFinderViewModel`(화면 수명)과 출발 뒤 `HospitalTrip.trip`(안내 수명, 고른 1곳)에만 메모리로 있다. 파일·DB·Firestore·SharedPreferences에 쓰지 않는다. 앱을 다시 켜면 없다.
- 로그 태그는 012의 `HospitalTrip` 하나를 쓴다.
- 키는 `firebase/functions/.secret.local`에만 있다. 앱·웹·`secrets.properties`·BuildConfig에 없다 (완료 기준 7).

## 단계

단계 하나 = 커밋 1~3개. 앱 코드 커밋은 훅이 `assembleDebug`를 확인한다.

- [x] 0. 준비
  - 0-1. 이 계획 커밋 → `docs(plans): 015 근처 동물병원 검색 계획`
  - 0-2. 기준 테스트 수 세기: `.\gradlew.bat testDebugUnitTest` 후 `app/build/test-results/testDebugUnitTest/*.xml`의 tests 합계를 진행 로그에 적는다.
  - 0-3. U1~U6 답 받기. K1~K7, F1~F7을 메인이 웹으로 확인해 `docs/data/nearby-vets.md`("카카오 로컬 API (확인한 것)", 출처 URL·날짜)와 진행 로그에 적는다. 확인 못 한 항목은 "미확인"으로 남긴다 → `docs(data): 카카오 로컬 API와 Functions 에뮬레이터 확인 사항 기록`
  - 0-4. `java -version` (U6). JDK 21이 없으면 사용자에게 설치를 부탁하고 단계 5 전까지 끝낸다. 단계 1~4는 Java 없이 진행한다.
- [x] 1. 도메인 계약 (설계 결정 2)
  - `HospitalOrigin.NEARBY_SEARCH`, `canReceiveCard`, `NearbyVetSource`·`NearbyVets`·`NEARBY_RADIUS_METERS`, `nearbyVetRowsOf` + `NearbyVetRowsTest`.
  - `HospitalOrigin`에 값을 더하면 `finderNoticesOf`의 `when`이 컴파일되지 않는다. 목록 출처가 `NEARBY_SEARCH`일 일은 없지만, 새 안내 값(`FinderNotice.NEARBY` 등)으로 받고 KDoc에 이유를 적는다.
  - → `feat(domain): 근처 동물병원 검색 인터페이스와 거리순 정리 규칙`
- [x] 2. Fake와 구성표 (설계 결정 3·7)
  - `FakeNearbyVetSource` + `FakeNearbyVetSourceTest`. 좌표를 지도로 확인한다.
  - `di/NearbyVetPlan.kt` + `NearbyVetPlanTest`.
  - → `feat(data): 근처 동물병원 Fake 구현`, `feat(di): 근처 동물병원 출처 구성표`
- [x] 3. ViewModel (설계 결정 4)
  - `NearbyState`, `Loaded.nearby`, 생성자 인자 2개, V1~V12 + `HospitalFinderNearbyTest` (새 클래스. 기존 `HospitalFinderViewModelTest`는 고치지 않는다).
  - → `feat(ui): 병원 목록에서 근처 동물병원을 따로 찾기`
- [x] 4. 화면과 연결 (Fake로 동작, 설계 결정 8) → **체크포인트 A**
  - 4-1. `HospitalTrip.depart`의 카드 막기 + `HospitalTripNearbyTest` → `feat(trip): 제휴 병원이 아니면 진료 카드를 보내지 않음`
  - 4-2. 목록 묶음, 문구 순수 함수 + `HospitalTextsNearbyTest`, strings.xml, 미리보기 → `feat(ui): 병원 목록 아래 근처 동물병원 묶음`
  - 4-3. 근처 병원 확인 화면 변형, `MainActivity` 연결, `AppContainer`에 `nearbyVetKind`와 Fake 연결(FUNCTION은 단계 7까지 OFF로 둔다) → `feat(ui): 근처 동물병원 확인 화면과 Fake 연결`
  - `/verify` (일반 태블릿 에뮬레이터, Fake 모드): E1·E2·E3. 스크린샷을 진행 로그에 적는다.
- [x] 5. 서버 함수 `[선행: 메인 웹 확인(카카오)]` `[선행: 메인 웹 확인(Firebase)]` (U1·U5·U6 필요, 설계 결정 5)
  - 5-1. `firebase/functions/`(package.json, nearby.mjs, nearby.test.mjs, index.js, `.secret.local.example`), `.gitignore`에 `firebase/functions/node_modules/`·`firebase/functions/.secret.local` → `feat(firebase): 카카오 근처 동물병원 검색 함수`
  - 5-2. `firebase.json` functions 설정, `firebase/package.json`의 `emulators:nearby` → `chore(firebase): Functions 에뮬레이터 설정`
  - 확인: `npm --prefix firebase/functions ci`, `npm --prefix firebase/functions test`, `npm --prefix firebase run emulators:nearby`가 세 에뮬레이터를 띄운다.
- [x] 6. 함수 점검 스크립트 (설계 결정 5)
  - `firebase/scripts/check-nearby.mjs` N1~N5 → `feat(firebase): 근처 검색 함수 점검 N1~N5`
  - preflight `--nearby`(PF7, PF5의 5001) → `feat(firebase): 시연 전 점검에 Functions 에뮬레이터 추가`
- [x] 7. 앱의 함수 호출 (Real 래퍼, 설계 결정 6) `[선행: 메인 웹 확인(Firebase)]` F4·F5
  - `NearbyFunctionConfig.kt` + `NearbyFunctionConfigTest`, `FunctionNearbyVetSource` + `FunctionNearbyVetSourceTest` → `feat(server): 근처 동물병원 함수 호출 래퍼`
  - `AppContainer`: `serverClient`의 익명 계정을 같이 써서 FUNCTION 연결 → `feat(di): 시연·Real 모드에서 근처 검색 함수 연결`
- [x] 8. 끝까지 확인 (일반 태블릿 에뮬레이터 + PC 에뮬레이터)
  - 아래 "확인 절차" E4~E9. 걸린 시간(목록, 근처 검색)을 진행 로그에 적는다. 고칠 것이 나오면 `fix:` 커밋.
- [ ] 9. 테스트 (`tester`)
  - 메인이 `tester`에게 이 계획 경로와 대상(설계 결정 2·3·4·6·8의 계약, `firebase/functions/nearby.mjs`)만 넘긴다.
  - → `test(hospital): 근처 동물병원 계약 테스트` (버그 보고는 메인이 `fix:`로 고친다)
- [ ] 10. `/verify`
  - Fake 모드와 시연 모드(서버 있음) 목록·근처 확인 화면, 넓은 화면·좁은 화면, 라이트·다크. 주행 전환(E3).
- [ ] 11. 문서 갱신 → `docs: 근처 동물병원 검색 반영`
  - `docs/data/nearby-vets.md`: 요청·응답 형식, 읽기 규칙, 반경, 저장 금지, 카카오 확인 사항(K1~K7, 출처).
  - `docs/data/hospital-status.md` "나중 후보"의 카카오 줄 → "했음 (015): nearby-vets.md".
  - `docs/architecture.md`: 서비스 기능 스택(서버 행: Functions 에뮬레이터, 병원 목록 행), 데이터 모드(근처 병원 표), 현재 위치 행(위치를 쓰는 곳에 근처 검색), 패키지 구조(새 파일), "Firebase로 옮기기"(배포할 때 할 일: Blaze, 비밀 관리자, App Check·속도 제한 다시 정하기).
  - `firebase/README.md`: 준비물(JDK 21을 에뮬레이터 터미널에만, `npm --prefix firebase/functions ci`, `.secret.local`), 명령 표(`emulators:nearby`, `check-nearby`, `preflight -- --nearby`), 포트 5001, `adb reverse tcp:5001`.
  - `README.md`: "일반 폰·태블릿 에뮬레이터에서 시연하기"와 로컬 서버 시연 절차에 근처 병원 순서. release APK에는 근처 병원이 없다는 것.
  - `AGENTS.md`: 명령어 표에 Functions 관련 두 줄이 필요한지 보고, 넣으면 한 줄씩만.
  - 문서 안의 "계획 015"(연결 자격 암호화 저장) 언급은 이 계획 번호와 겹친다. 아래 "결정 / 배운 점" 참고.
- [ ] 12. 개발 노트 작성 (`docs/notes/CYJhub/2026-11-DD-nearby-vet-search.md`), 계획을 `docs/plans/completed/`로 옮기기 → `docs(notes): 근처 동물병원 검색 개발 노트`, `docs(plans): 015 완료로 옮김`

### 있으면 좋음

- [ ] A. 카드 전송 줄 문구: 근처 병원으로 출발하면 "제휴 병원이 아니라 진료 카드를 보내지 않았어요" → `feat(ui): 비제휴 병원 출발 문구`
- [ ] B. 카카오 두 번째 페이지까지 받기 (최대 30곳. 쿼터 K5를 보고 정한다)
- [ ] C. Fake 모드 debug 패널에 근처 검색 결과 바꾸기(정상/빈 결과/실패/느림) → 화면에서 E5~E7을 Fake로도 본다
- [ ] D. 검증 파이프라인에 `npm --prefix firebase/functions test` 넣기 (하네스 변경이라 사용자 확인 필요)
- [ ] E. GPU PC의 Pleos 에뮬레이터에서 내비 현재 위치로 근처 검색 확인 (내비 화면을 한 번 띄운 뒤)

## 확인 절차

이 PC에는 Pleos 에뮬레이터가 없다. **일반 태블릿 에뮬레이터**(차량 기기 아님)에서 확인한다. 이 기기에서 현재 위치는 모든 모드에서 Fake(수정구 시흥동)이고, 시연 모드의 주행 상태도 Fake(debug 패널)다. 그래서 아래는 내비 위치·실제 주행 규제로는 확인하지 않는다 (있으면 좋음 E).

| # | 준비 | 할 일 | 기대 |
|---|---|---|---|
| E1 | debug, Fake 모드 | 카드 → [가장 빨리 진료받을 곳 찾기] | 제휴 목록이 먼저 뜨고, 아래 "근처 동물병원" 묶음이 "찾는 중" → 시연용 3곳(가까운 순, "이동 약 N분 · 대기 정보 없음", 큰 합계 없음) |
| E2 | E1 | 근처 병원 하나 → 확인 → [길 안내 시작] | 확인 화면에 [출발]·[카드 없이 길안내만]이 없다. 안내가 시작되고 카드 전송 줄이 "카드 없이 출발했어요". `HospitalTrip` 로그에 "카드 전송" 줄이 없다. 전화 없는 곳은 [병원에 전화]가 없다 |
| E3 | E1·E2 화면 | debug 패널로 주행 → 정차 | 주행 화면(debug 패널 밖 클릭 노드 0, `EditText` 0) → 같은 목록·확인 화면 |
| E4 | debug, 시연 모드, `secrets.properties`에 `FIREBASE_PROJECT_ID=demo-meongcare`·`FIREBASE_EMULATOR_HOST=127.0.0.1`, `emulators:nearby` + `seed` + 키, `adb reverse` 8080·9099·5001, `preflight -- --nearby` 통과 | 병원 찾기 | 제휴 목록(서버) 위에 그대로, 아래 실제 근처 동물병원(시흥동 기준) 15곳 이하, 거리순, "시연용" 없음, 위치 안내·출처 표시 |
| E5 | E4에서 Functions만 끔 (`emulators`로 다시 켬) | 병원 찾기 | 제휴 목록은 바로 뜬다. 8초 안에 "근처 동물병원을 찾지 못했어요" + [다시 찾기]. 함수를 켜고 [다시 찾기] → 목록 |
| E6 | E4 | 근처 병원 → [병원에 전화] | 전화 앱이 그 번호로 열린다 (걸지 않음) |
| E7 | E4 | 근처 병원 → [길 안내 시작] → 도착 → [닫기] | Fake 길안내 흐름이 012와 같다. 병원 웹에 환자 건이 생기지 않는다 |
| E8 | E4~E7 | `adb logcat --pid=<앱 pid>`, Functions 에뮬레이터 터미널 로그 | 좌표·병원 이름·전화·토큰 0건 |
| E9 | release APK (`secrets.properties`에 Firebase 값 없이 빌드), 비행기 모드 | 처음부터 끝까지 | 근처 병원 묶음이 없고 012~014 흐름이 지금과 같다 |

- 느린 네트워크·빈 결과·위치 모름은 화면으로 만들기 어려워 JVM 테스트(V2·V4·V5·V6)로 확인한다. 화면으로도 보려면 있으면 좋음 C.

## 진행 로그

- 2026-11-08: 계획 작성 (planner). 사용자 결정 D1~D3 반영.
- 2026-11-08: U1~U6 답 (CYJhub).
  - U1 승인: `firebase/functions/`에 `firebase-functions`, `firebase-admin` (정확한 버전 고정).
  - U2 기본안: 좌표 약 100m 반올림 + 로그·저장 없음 + 목록에 위치 안내 문구. 따로 동의 버튼은 두지 않는다.
  - U3 기본안: Fake 모드에 시연용 근처 병원 3곳을 보인다.
  - U4 보류: K6(카카오 정책)을 확인한 뒤 다시 묻는다. 그때까지 화면에 출처 표시.
  - U5 완료: CYJhub 계정으로 카카오 Developers 앱 생성, REST 키 발급, 카카오맵 사용 설정 ON. 키는 아직 어디에도 넣지 않았다.
  - U6 완료: Temurin JDK 21.0.13 설치 (winget). 설치 프로그램이 시스템 `JAVA_HOME`과 PATH를 JDK 21로 바꿨다 → Android 빌드도 JDK 21로 돈다 (README "JDK 21에서 확인함").
- 2026-11-08: 0-2 기준 테스트 `testDebugUnitTest` **1014개, 실패 0** (51초).
- 2026-11-08: 0-3 K1~K7, F1~F7 웹 확인 → `docs/data/nearby-vets.md`. 요점:
  - K2: 동물병원 카테고리 코드가 없다 → 키워드 `동물병원` + `category_name`으로 거르기. `category_name` 모양은 단계 5에서 실제 응답으로 확인.
  - K3: radius ≤ 20000m, size ≤ 15, page ≤ 45, `sort=distance`는 x·y 필요. K5: 하루 100,000건, 넘으면 429.
  - K6: 캐시·저장 금지(운영정책 5조 20호), 제휴처럼 보이게 하는 상표 사용 금지(18조). 출처 표시 의무·영상 노출 조항은 못 찾음 → U4 다시 묻기.
  - K7: 카카오맵 사용 설정 ON 필요(사용자 완료). 허용 IP 등은 단계 5에서 실제 호출로 확인.
  - F2: `firebase-functions` 7.4.0, `firebase-admin` 14.5.0 (둘 다 2주 넘음). F7: 에뮬레이터는 Blaze 불필요.
  - U4 답 (CYJhub): 영상·기획서에 실제 병원을 그대로 쓴다. 화면에 "장소 정보: 카카오"와 "대기 정보 없음"을 보이고, 특정 병원을 클로즈업하지 않는다. 제휴처럼 보이는 문구는 쓰지 않는다 (K6 18조).
  - 미확인(단계 5에서 확인): F3 `.secret.local` 위치·`demo-` 동작, F4 성공 키(`result`)·에뮬레이터 토큰 검증, F6 바깥 호출.
- 2026-11-08: 단계 1·2 완료 (`a62b0ac`, `d25d72f`, `39ab245`). `HospitalOrigin`에 값이 늘어 `HospitalTextsBoundaryTest`의 출처→안내 표에 `NEARBY_SEARCH → FinderNotice.NEARBY` 한 줄을 더했다 (표가 `entries`를 다 돈다).
- 2026-11-08: **순서 변경 (사용자 결정)**: 실제 카카오 검색을 먼저 보려고 단계 5를 3·4보다 먼저 한다. 실제 응답 모양(K2 `category_name` 등)을 일찍 확인하는 이점도 있다.
- 2026-11-08: 이 PC의 Node 26.13.0은 `firebase-tools` 15.34.1(engines 20·22·24)이 설치를 거부해서, 사용자 결정으로 **Node 22.23.4 LTS로 바꿨다** (Node 26 제거는 관리자 확인 필요). 웹 engines(22.12 이상)도 맞는다.
- 2026-11-08: 단계 5 코드: `firebase/functions/` (JS ESM, `node --test` 7개 통과). `firebase.json`에 functions(`127.0.0.1:5001`), `emulators:nearby` 스크립트.
- 2026-11-08: 단계 5 확인 (사용자가 `.secret.local`에 키를 넣음): `emulators:nearby` 기동, 손으로 N1~N5 확인 → N1 401, N3 400, **N2 200·15곳·최대 2.8km·238ms**, N4 429, N5 `schemaVersion 1`·`radiusMeters 5000`. F3·F4·F6·K7 확인, K2 거르기 동작 (`kakaoCount 15 → 15`). 기록: `docs/data/nearby-vets.md` `[obs-nearby-emu]`. 함수 로그에 `kakaoCount`(거르기 전 개수)를 더했다.
- 2026-11-08: 단계 3 (`567e503`, V1~V9 테스트 9개), 단계 4-1·4-2·4-3 (`9184318`, `7ccdacc`, `21ab59d`), 단계 7 (`6a4ce5c`, `4449ad3`) 완료.
- 2026-11-08: **범위 추가 (사용자 결정)**: 사용자가 "지금 위치 기준"을 요구. 이 PC의 태블릿 에뮬레이터는 내비가 없어 위치가 늘 시연 위치(시흥동)였다. → **내비 없는 기기의 시연·Real 모드는 안드로이드 기기 위치(GPS)를 쓴다** (`usesDeviceLocation`, `DeviceCurrentLocationSource`, `1357328`). 새 권한 `ACCESS_FINE_LOCATION`·`ACCESS_COARSE_LOCATION`(Android 권한, `pleos.car.permission` 아님)을 사용자가 승인 (debug·release 모두). 병원 찾기를 열 때 권한 창, 허용하면 위치를 다시 묻는다. 차량 기기는 그대로 내비 위치.
  - release를 일반 폰에서 돌리면 위치 권한 창이 뜨고, 거부하면 제휴 목록이 대기 시간 순(WAIT_ONLY)이 된다 (예전에는 시연 위치 기준). 근처 묶음은 release에 없다 (함수 주소 없음).
- 2026-11-08: **체크포인트 (일반 태블릿 에뮬레이터 + PC Firebase 에뮬레이터, 시연 모드)**: 서울시청(37.5665, 126.978) 위치로 병원 찾기 → 제휴 5곳(서버) + **근처 동물병원 14곳(실제 카카오)**, 거리순, 위치 안내·출처 표시. 로그 `근처 병원: 14곳 (3053ms)`. 확인 화면·길안내·주행 전환(E2·E3·E6·E7)은 아직.
  - 함정 1: 사용자가 값을 `secrets.properties.example`에 넣었다 → `secrets.properties`로 옮기고 예시 파일은 되돌림. `secrets.properties`와 BuildConfig는 권한 설정상 Claude가 읽지 못한다 (앱 동작으로 확인).
  - 함정 2: 이 에뮬레이터(API 36 Google Play 태블릿)는 `adb emu geo fix`가 GPS에 닿지 않았다 (`last location=null`). `appops set com.android.shell android:mock_location allow` + `cmd location providers add-test-provider gps` + `set-test-provider-location gps --location lat,lon`으로 넣었다. 실행 스크립트 `-Lat -Lon`에 넣음.
  - 함정 3: 앱을 막 켠 첫 서버 요청이 5초 시간 초과로 내장 병원으로 대신했다 (느린 에뮬레이터). [새로 고침]으로 서버 5곳.
  - 관찰: 근처 묶음이 제휴 목록·지난 곳 아래라 넓은 화면에서도 두세 번 스크롤해야 보인다 → 화면 배치는 /verify 때 다시 본다.
- 2026-11-08: **설계 변경 (사용자 결정, 체크포인트를 본 뒤)** → 설계 결정 1·4·8을 아래 "설계 변경 V2"로 대신한다.
  - 근처(카카오) 병원에 **시연용 예시 대기 시간**을 붙이고, 제휴 병원과 **한 목록에서 이동 + 대기 순**으로 섞는다. 1위 "가장 빠름"이 카카오 병원일 수 있다.
  - **5개씩 페이지로** 보인다 ([이전]/[다음], 쪽 번호). 세로 스크롤을 줄인다.
  - "제휴 병원이 아니라 대기 정보가 없어요" 문구는 뺀다. 대신 줄마다 "(예시)", 목록 위 "장소 정보: 카카오 · 대기 시간은 시연용 예시예요" 한 줄 (사용자 선택: 작게 '예시' 표시. 실존 병원에 사실이 아닌 대기 정보가 실제처럼 보이지 않게, 출품 유의사항).
  - 진료 카드는 그대로 제휴 병원에만 (`canReceiveCard`). 카카오 병원 확인 화면은 [병원에 전화]·[길 안내 시작].

- 2026-11-08: 설계 변경 V2 구현 (`a4fc629`, `a70c465`). 확인 (서울시청 위치, 시연 모드, 서버 있음): 제휴 5곳 + 카카오 14곳이 한 순위, 1위 카카오 병원 "이동 약 7분 + 대기 5분(예시)", 5개씩 4쪽, [다음] → "2 / 4쪽". 예시 대기 해시는 FNV만으로 고르지 않아 murmur3 마무리를 더했다 (`ExampleWaitTest`가 잡음).
  - 관찰: 쪽을 넘겨도 스크롤 위치가 그대로라 아래쪽에 머문다 → /verify 때 다시 본다.

### 설계 변경 V2 (2026-11-08)

- 예시 대기: `domain/hospital/ExampleWait.kt`의 `exampleStatusOf(hospital, now)`. 병원 이름·좌표로 정한 값이라 같은 병원은 늘 같다 (5~60분, 5분 단위). 혼잡도는 대기로 정한다 (15분 미만 여유, 35분 미만 보통, 그 이상 혼잡). 응급 접수 중, 알린 시각 = 지금 (늘 FRESH). 실제 정보가 아니다.
- 순위: ViewModel이 `rankHospitals(제휴 + 근처(예시 상태), 위치, 지금, maxPerGroup = 50)`. 근처는 여전히 `nearbyVetRowsOf`로 거른 뒤(반경·제휴와 같은 곳·중복·15곳) 넣는다. `NearbyState`는 근처 검색 진행 상태 줄(찾는 중·실패·위치 모름·없음)에만 쓴다.
- 화면: 목록 = `ranked` + `unranked`를 이어 5개씩 쪽. 응급 접수 안 함 줄은 마지막 쪽 아래. 쪽 번호는 목록이 바뀌면 범위 안으로 맞춘다.
- 문구: 근처 병원 줄은 "이동 약 N분 + 대기 M분(예시)" · "대기 시간 예시". 확인 화면: "이 병원에는 진료 카드를 보낼 수 없어요." / "대기 시간은 시연용 예시예요. 출발 전에 전화로 확인해 주세요." / "장소 정보: 카카오".

- 2026-11-08: **단계 6·8 완료**. 6: `check-nearby` N1~N5 모두 PASS (15곳, 최대 2.80km), `preflight -- --nearby` PF7 PASS (`2956b4d`, `acd32a9`). 쪽을 넘기면 맨 위로 (`fcaf909`).
  - 8 (태블릿 에뮬레이터 + PC Firebase, 시연 모드, 서울시청): E2 카카오 병원 확인 화면에 [병원에 전화]·[길 안내 시작]만 → 시작하면 3초 뒤 주행 화면(큰 남은 분 + "○○까지" + 진행 막대) → 도착 "○○에 도착했어요" + "카드 없이 출발했어요", `HospitalTrip` 로그에 카드 전송 줄 없음. E3 주행 화면 정보 최소. E6 [병원에 전화] → 전화 앱이 그 번호로 열림 (걸지 않음). E7 도착 → [닫기]. E8 앱 로그·함수 로그는 개수·ms만.
  - E9 (release)는 확인하지 않았다: release APK는 `/release-apk`(사용자 실행)로만 만든다. 근거는 `nearbyFunctionUrlOf`(release면 null) 테스트와 `nearbyVetKindOf` 표.
  - 관찰: 카카오 병원 확인 화면의 안내 상자("이 병원에는 진료 카드를 보낼 수 없어요" 등)는 넓은 화면에서도 버튼 아래로 밀려 스크롤해야 보인다.

## 결정 / 배운 점

- 계획 번호: active·completed의 최대(014) + 1 = 015. 그런데 `docs/architecture.md`, `docs/plans/completed/014-owner-link.md`, `docs/notes/xxj15/2026-11-06-owner-link.md`가 "연결 자격 암호화 저장은 PT 전 (계획 015로 넘김)"이라고 적고 있다. 그 작업은 다음 번호(016 이후)로 계획을 쓰고, 단계 11에서 architecture.md의 언급만 "다음 계획"으로 고친다. 완료된 계획과 남의 노트는 기록이라 고치지 않는다 (사용자에게 알린다).
- 기존 `rankHospitals`·`HospitalPlan`을 바꾸지 않고 근처 병원을 따로 정리한다 (설계 결정 1·3). 012의 순위 테스트와 구성표 테스트를 다시 볼 일이 없다.
