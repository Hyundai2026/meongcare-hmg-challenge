# prototype-pets

- 날짜: 2026-11-15
- 작성: xxj15
- 브랜치: feat/prototype-pets
- 계획: docs/plans/completed/021-prototype-pets.md
- 커밋: 7cb38bf..(이 노트)

## 무엇을 했나

차 앱의 시연용 샘플 반려견 "보리"를 디자인 프로토타입의 두 마리, 왕건이와 오숑이로 바꿨다. 정차 중 홈 위쪽의 [⇄ 변경]을 누르면 두 마리가 번갈아 바뀌고, 사진·이름·반려동물 카드·진료 카드가 모두 고른 반려견을 따른다.

## 어떻게: 핵심 결정과 이유

- 값 (사용자 결정 2026-11-15)
  - 왕건이: 래브라도 리트리버, 30kg, 2014-03-12, 수컷, 심장질환, "심장질환 약 복용 중(상세 약명 미등록)". 프로토타입 데이터는 "말티즈 3.8kg"이었는데 사진(큰 누런 개)과 맞지 않아 사용자가 바꿨다.
  - 오숑이: 비숑 프리제, 2023-05-08, 수컷, 특징 "차량 이동에 예민함". 질환·약·알레르기는 없음(빈 목록 → 화면은 "미등록"/"등록된 정보 없음").
  - 성별은 프로토타입에 없어서 사용자가 둘 다 수컷으로 정했다.
  - 사진은 프로토타입 HTML의 base64 사진을 그대로 뽑아 drawable로 넣었다. 출품물에 써도 된다고 사용자가 확인했다.
- 고르기는 홈 [⇄ 변경] 한 곳만 → 오늘 마감에 맞춘 가장 작은 범위. 프로토타입의 반려동물 카드 탭·증상 선택 [변경]은 넣지 않았다 (D3).
- 바꾸면 만든 진료 카드를 지운다 → 프로토타입 `cycle`이 고른 증상을 비우는 것과 같다. 다른 반려견의 카드가 병원에 가지 않게 (D2).
- 버튼을 숨기는 때: 샘플이 아닐 때, 보호자 웹에 연결된 동안(Fake 연결 포함), 안내 중·도착·출발한 trip이 있을 때. 주행 중에는 정차 화면 자체가 안 보인다 (AGENTS.md 규칙 3, `homeContentOf` 그대로).
- 고른 반려견은 앱 프로세스 동안만 기억한다 (D5). 릴렉스 설정·진료 기록은 반려견별로 나누지 않았다 (D4).
- 예시 JSON(`docs/data/pet-profile.example.json`, 보리)과 차 앱 샘플의 연결을 끊었다. 테스트의 `ExamplePetProfiles`가 예시 JSON 값을 따로 들고, 차 앱 샘플은 `SamplePetProfiles.wanggeon`/`osyong`이다. `demoPet` 이름은 남겨(= 왕건이) 바탕 값으로만 쓰는 테스트 약 30개를 건드리지 않았다.
- 구조: `domain/pet/SamplePet.kt`(`SamplePet`, `SamplePetChoice`), `data/pet/SamplePetChooser.kt`(메모리 StateFlow, `PetProfileSource`도 겸함), `ui/pet/PetChoice.kt`(버튼을 보일지 정하는 순수 함수). `LinkedPetProfileSource`는 바꾸지 않았다: 샘플 출처만 고르기로 바꿨다.

## 사용한 Pleos SDK / API / 권한

없음.

## 확인

- 테스트: `./gradlew testDebugUnitTest` 1525개 통과 (시작할 때 1466개).
  - 직접 쓴 것: `SamplePetProfilesTest`, `SamplePetChooserTest`, `LinkedPetProfileSourceSampleChoiceTest`, `SamplePetPhotoTest`, `PetChoiceTest`
  - tester가 쓴 경계 테스트 36개: `SamplePetChooserBoundaryTest`, `SamplePetLinkBoundaryTest`, `SamplePetProfilesBoundaryTest`, `PetChoiceBoundaryTest`, `SamplePetPhotoBoundaryTest`. 버그 없음.
- 화면 (Pleos Connect 에뮬레이터 2560×1440, 라이트, 가로):
  - 시연 모드 홈: 왕건이 사진 + "왕건이" + [⇄ 변경] → 오숑이 사진 + "오숑이".
  - 반려동물 카드(오숑이): "강아지 · 비숑 프리제 · 3세 6개월", 2023.05.08, 체중 미등록, 특징 "차량 이동에 예민함", 질환·약·알레르기 미등록.
  - 진료 카드(오숑이, 구토): 이름·사진 오숑이, "비숑 프리제 · 3세 6개월 · 수컷".
  - 카드를 만든 뒤 홈 [⇄ 변경] → 왕건이, [응급 진료 시작] 하면 증상이 비어 있다.
  - Fake 모드: [주행] → 주행 화면에 버튼 없음. [정차] → 오숑이 그대로.
  - Fake 연결 중 버튼 없음, 연결 화면에도 고른 오숑이. 끊으면 오숑이와 버튼이 돌아온다.
  - 출발한 뒤(병원으로 이동 중) 홈으로 돌아와도 버튼 없음.
  - logcat에 앱 크래시 없음.

## 시연 방법

1. 정차(P) 상태 홈 오른쪽 위 [⇄ 변경]을 누르면 왕건이 ↔ 오숑이.
2. 심장질환이 있는 왕건이로 [응급 진료 시작]을 하면 진료 카드에 기존 병력이 들어간 장면을 보일 수 있다.

## 주의점 / 남은 일

- README와 `docs/readme/` 사진·`demo.gif`에는 아직 보리가 나온다. 다시 찍어야 한다.
- 보호자 웹 샘플, `firebase/` 시드, `docs/data/*.example.json`은 아직 보리다 (후속).
- 반려견별 릴렉스 설정·진료 기록, 고른 반려견 영구 저장은 후속 (D4, D5).
- Fake 연결 reader는 읽는 순간 고른 샘플을 돌려준다. 연결된 동안 고르는 곳을 더하면(예: 증상 선택 [변경]) `linked` 조건을 꼭 같이 넣는다 (tester 지적).
