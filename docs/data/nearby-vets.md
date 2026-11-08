# 근처 동물병원 검색 (계획 015)

차 앱이 현재 위치 기준 반경 5km 안의 동물병원을 카카오 로컬 API로 찾는다. 카카오 호출은 Firebase Function `nearbyVets`가 한다 (키를 앱에 넣지 않는다). 요청·응답 형식은 구현(단계 5·7) 뒤 이 문서에 더한다.

## 카카오 로컬 API (확인한 것)

2026-11-08 확인. 출처:

- [kakao-ref] https://developers.kakao.com/docs/ko/local/dev-guide , https://developers.kakao.com/docs/ko/kakaomap/rest-api (REST 레퍼런스. 표 일부는 요약으로 읽음 → "요약" 표시)
- [kakao-common] https://developers.kakao.com/docs/ko/local/common (카카오맵 이해하기)
- [kakao-quota] https://developers.kakao.com/docs/ko/getting-started/quota
- [kakao-policy] https://developers.kakao.com/terms/ko/site-policies (운영 정책)

| # | 항목 | 확인한 내용 | 출처 |
|---|---|---|---|
| K1 | 주소·인증 | 키워드 검색 `GET https://dapi.kakao.com/v2/local/search/keyword.json`, 카테고리 검색 `GET .../v2/local/search/category.json`. 헤더 `Authorization: KakaoAK ${REST_API_KEY}` | kakao-ref, kakao-common |
| K2 | 동물병원 코드 | **동물병원 카테고리 그룹 코드는 없다.** 18개 코드 중 가장 가까운 것은 `HP8 병원`(사람 병원 포함). → 키워드 검색 `query=동물병원`을 쓰고, 결과의 `category_name`으로 동물병원만 남긴다. `category_name`의 실제 모양("... > 동물병원")은 문서에 예가 없다 → **미확인, 단계 5에서 실제 응답으로 확인** (응답은 저장하지 않는다) | kakao-ref (요약) |
| K3 | 파라미터 | `x`=경도, `y`=위도 (문자열). `radius` 미터, 0~20000. `sort`=`accuracy`(기본)/`distance`, `distance`는 x·y 필요. `size` 1~15 (기본 15). `page` 1~45 | kakao-ref (요약) |
| K4 | 응답 | `meta`(`total_count`, `pageable_count`, `is_end`, `same_name`), `documents[]`(`id`, `place_name`, `category_name`, `category_group_code`, `phone`, `address_name`, `road_address_name`, `x`, `y`, `place_url`, `distance`). 필드는 문자열. `distance`는 x·y를 보냈을 때만 미터 문자열. 전화가 없을 때 값은 문서에 없다 → 빈 문자열로 보고 `null`로 바꾼다 (미확인) | kakao-ref (요약) |
| K5 | 쿼터 | 하루 키워드 검색 100,000건, 카테고리 검색 100,000건. 한 달 전체 3,000,000건(바뀔 수 있음). 초당 한도는 문서에 없다. 무료 제공량을 다 쓰면 **429**. 오류 몸통 형식은 미확인 | kakao-quota, kakao-common |
| K6 | 정책 | 운영정책 제5조 20호: 사용자 환경 개선 목적 외로 카카오 데이터를 캐시하거나, 캐시한 뒤 최신으로 유지하지 않는 행위 금지 → **결과를 서버·기기에 저장하지 않는다** (메모리에서 화면에만). 제5조 31호: 쿼터 초과 금지. 제18조: 카카오 상표는 우리 서비스를 설명할 때만, 카카오가 만들었거나 제휴한 것처럼 보이게 하면 안 된다. **출처 표시 의무, 다른 내비로 길안내, 영상·캡처 노출에 대한 조항은 찾지 못했다** | kakao-policy |
| K7 | 앱 설정 | 앱 관리 페이지 [카카오맵] > [사용 설정] 상태 ON 필요 (2026-07-21부터 사용 방법·무료 쿼터 방식 변경). 무료 쿼터는 계정에서 카카오맵을 처음 켠 앱에만. REST 키에 "필요한 설정 등록"이 있다고만 적혀 있고, 서버 호출의 허용 IP 같은 설정은 찾지 못했다 → **단계 5에서 실제 호출로 확인** | kakao-common |

- 카카오 결과 화면에는 "장소 정보: 카카오" 정도로 출처를 적는다 (의무 조항은 못 찾았지만, 제휴처럼 보이지 않게 하려는 것).

## Firebase Functions 에뮬레이터 (확인한 것)

2026-11-08 확인. 출처:

- [fb-install] https://firebase.google.com/docs/emulator-suite/install_and_configure
- [fb-local] https://firebase.google.com/docs/functions/local-emulator
- [fb-env] https://firebase.google.com/docs/functions/config-env
- [fb-callable] https://firebase.google.com/docs/functions/callable-reference
- [fb-start] https://firebase.google.com/docs/functions/get-started , https://firebase.google.com/docs/functions/manage-functions
- [fb-connect] https://firebase.google.com/docs/emulator-suite/connect_functions
- 버전: `npm view <패키지> version time engines` (npm 웹 페이지는 403)

| # | 항목 | 확인한 내용 | 출처 |
|---|---|---|---|
| F1 | Java | 에뮬레이터 모음은 JDK 11 이상. Firestore·Realtime DB·Storage 일부가 Java 기반. Functions·Auth만 쓸 때 Java가 필요 없는지는 적혀 있지 않다. 이 PC는 JDK 21 (U6) | fb-install |
| F2 | 버전 | `firebase-functions` 7.4.0 (2026-09-15, engines node>=18), `firebase-admin` 14.5.0 (2026-09-23, engines node>=22). Functions 런타임은 Node 20·22 완전 지원(24는 문서에 없음). ESM(`"type": "module"`) 지원 | npm view, fb-start |
| F3 | 비밀값 | 에뮬레이터는 `.secret.local` 파일로 비밀값을 덮어쓴다. 없으면 실제 비밀 관리자에 접근하려 한다. 파일 위치(함수 소스 폴더)·형식(`KEY=값`)과 `demo-` 프로젝트에서 되는지는 문서에 없다 → **단계 5에서 확인** | fb-env |
| F4 | callable 규약 | POST, `Content-Type: application/json`, 몸통 `{"data": ...}`(다른 필드가 있으면 잘못된 요청), 선택 `Authorization: Bearer <ID 토큰>` → 백엔드가 검증하고 무효면 거절. 성공 200 + `result`(문서 예시는 `response`로 적혀 있어 일관되지 않음 → 단계 5에서 확인). 실패 `{"error": {"message", "status", "details"}}`: 400 `INVALID_ARGUMENT`, 401 `UNAUTHENTICATED`, 500 `INTERNAL`. 에뮬레이터가 Auth 에뮬레이터 토큰을 검증하는지는 문서에 없다 → 단계 5 | fb-callable |
| F5 | 주소·포트 | `http://$HOST:$PORT/$PROJECT/$REGION/$NAME`, 기본 포트 5001, 기본 지역 `us-central1`(함수에 지역을 정하면 그 지역). `firebase.json`의 `host` 키는 읽은 페이지에 없다(예시는 `port`만) → 기존 `firebase.json`의 다른 에뮬레이터 설정 방식을 따른다 | fb-install, fb-local, fb-connect |
| F6 | 바깥 호출 | `demo-` 프로젝트는 에뮬레이터하고만 통신하고, 에뮬레이터가 없는 Firebase 자원에 접근하면 실패한다. 외부 HTTP(카카오) 호출 제한은 문서에 없다 → 단계 5에서 확인 | fb-connect |
| F7 | Blaze | 에뮬레이터는 어느 프로젝트에서나 된다. Blaze는 **배포할 때만** 필요 | fb-start |
