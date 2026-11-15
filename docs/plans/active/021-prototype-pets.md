# 021 시연용 샘플 반려견을 프로토타입 두 마리(왕건이·오숑이)로 바꾸고 차 화면에서 고르기

- 목표: 차 앱의 시연용 샘플 반려견 "보리"를 디자인 프로토타입의 왕건이·오숑이 두 마리로 바꾸고, 정차 중 홈(과 반려동물 카드)에서 둘 중 하나를 고를 수 있게 한다.
- 완료 기준 (어떻게 검증하나):
  1. `node .claude/hooks/build-gate.mjs pipeline`이 "검증 파이프라인 통과."를 출력한다 (웹은 바꾸지 않으므로 그대로 통과해야 한다).
  2. 단위 테스트 `./gradlew testDebugUnitTest` 전체 통과. 시작할 때의 테스트 개수를 진행 로그에 적는다.
     - 새 테스트가 통과한다: `SamplePetProfilesTest`, `SamplePetChooserTest`, `LinkedPetProfileSourceSampleChoiceTest`, `PetChoiceTest`, `SamplePetPhotoTest`, 그리고 tester가 더한 경계 테스트.
     - 고친 기존 테스트 파일은 아래 "테스트 분류"의 A·B 목록(과 단계 6을 하면 D 목록) 안에 있다. `git diff dev --diff-filter=MD --name-only -- app/src/test`의 출력을 단계 1·2 진행 로그에 적고, 목록 밖 파일이 있으면 이유를 적는다.
  3. 범위: 아래 `git diff dev -- <경로>`의 출력이 비어 있다.
     - `web/`, `firebase/`, `docs/data/pet-profile.example.json`, `docs/data/owner-link.example.json`, `docs/data/emergency-case.example.json`, `README.md`, `docs/readme/`
     - `app/src/main/AndroidManifest.xml` (새 권한 없음), `ui/home/DrivingScreen.kt`, `ui/home/HomeContent.kt` (주행 화면 고르기 없음)
  4. 정적 검사 (Grep, `app/src/main`):
     - `보리` 0건. 예외: `ui/KoreanParticles.kt`의 조사 예시 주석(샘플과 상관없다).
     - `sample_pet_photo`, `DEMO_WEIGHT`, `DEMO_TRAITS` 0건.
     - `ui/` 아래 `import com.meongcare.app.data` 는 `ui/hospital/DemoHospitals.kt` 한 곳만 남는다 (`PetCardScreen.kt` 미리보기의 data import는 이번에 뺀다).
  5. 화면 확인 (`/verify`): Pleos 태블릿 에뮬레이터, **시연 모드, 라이트, 가로**. 결과를 진행 로그에 적는다.
     - 홈 오른쪽 위: 왕건이 사진(둥근 원) + "왕건이" + [⇄ 변경]. [⇄ 변경] → 오숑이 사진 + "오숑이". 한 번 더 → 왕건이.
     - 오숑이를 고른 뒤 [반려동물 카드]: "강아지 · 비숑 프리제 · 3세 6개월", 생년월일 2023.05.08, 체중 "미등록", 특징 "차량 이동에 예민함", 질환·약·알레르기 "미등록". (단계 5를 했으면) 머리 줄 오른쪽 이름 탭 [왕건이 | 오숑이]에서 오숑이가 선택돼 있고, [왕건이] 탭 → 왕건이 정보(래브라도 리트리버, 30kg, 2014.03.12, 심장질환, "심장질환 약 복용 중(상세 약명 미등록)").
     - 오숑이를 고른 뒤 [응급 진료 시작] → 증상 선택 머리에 "오숑이", 카드 만들기 → 진료 카드 이름 "오숑이", 사진은 오숑이.
     - 카드를 만든 뒤 [홈] → [⇄ 변경]: 만든 카드가 지워진다 (결정 D2 기본안). 다시 [응급 진료 시작]이면 증상이 비어 있다.
     - 병원으로 출발한 뒤(안내 중·도착)에는 홈에 [⇄ 변경]이 없다.
     - 주행: debug 패널 [주행] → 주행 화면에 고르기 버튼이 없다. [정차] → 보던 화면으로 돌아오고 고른 반려견이 그대로다.
     - `adb logcat -d -t 500 *:E`에 `com.meongcare.app` 크래시가 없다.
  6. 문서: `docs/architecture.md`, `docs/product/feature-facts.md`에서 "보리"가 샘플 반려견을 가리키는 곳이 왕건이·오숑이로 바뀌어 있다 (단계 8).
- 브랜치: `feat/prototype-pets` (최신 dev `5b034b8`에서 땄다)
- 관련 문서:
  - 선례: `docs/plans/completed/019-design-home-screen.md` (2단계에 "보리 → 왕건이"를 했다가 되돌린 기록과 테스트 분류가 있다)
  - 디자인 원천: `docs/product/design/prototype/MeongCareCar.html` (번들된 한 줄 HTML. 전체를 Read하지 말고 grep으로 본다)
  - AGENTS.md 규칙 3(주행 중 화면), 규칙 6(`/verify`), 규칙 7(문서 반영)
  - `docs/pleos/ux-guidelines.md` 101행 (설정류는 주행 중 ⛔ `UX_RESTRICTIONS_NO_SETUP`)
  - `docs/data/pet-profile.md` (프로필 형식. 체중·특징·종 필드는 없다)
  - 사진 원본(커밋하지 않은 scratchpad, 프로토타입 base64에서 뽑음):
    - 왕건이 `/private/tmp/claude-501/-Users-xxj15-meongcare-hmg-challenge/6727e5e4-3118-4039-9d14-fe7d3bbe2975/scratchpad/P-DEMO-01.jpg` (480×480)
    - 오숑이 `/private/tmp/claude-501/-Users-xxj15-meongcare-hmg-challenge/6727e5e4-3118-4039-9d14-fe7d3bbe2975/scratchpad/P-DEMO-02.jpg`
    - 세션이 바뀌어 이 경로가 없으면 프로토타입 HTML의 `"pid":"P-DEMO-01"`/`"P-DEMO-02"` 항목 `photo`(data:image/jpeg;base64)를 다시 뽑는다.

## 사용자가 정한 것 (2026-11-15)

- 두 마리 다 넣고 차 화면에서 고르게 한다 (프로토타입처럼). 기존 샘플 "보리"(포메라니안)는 없어진다.
- 왕건이: 품종 **래브라도 리트리버**, 체중 **30kg** (프로토타입의 "말티즈 3.8kg"은 사진과 맞지 않아 사용자가 바꿈), 생일 2014-03-12, 질환 "심장질환", 약 "심장질환 약 복용 중(상세 약명 미등록)", 알레르기 없음.
- 오숑이: 비숑 프리제, 생일 2023-05-08, 체중 없음, 질환·약·알레르기 없음, 특이사항 "차량 이동에 예민함".
- 사진은 프로토타입 사진을 앱 drawable로 넣는다.
- 범위는 차 앱만. 보호자 웹, `firebase/` 시드, `docs/data/*.example.json`, README와 README 사진은 "후속"이다.
- D1 성별: **두 마리 다 수컷(MALE)** (사용자 결정).
- D2~D6: 계획의 기본안대로 (사용자 결정).
- 사진 사용 권리: **출품물에 써도 된다** (사용자 확인).

## 프로토타입의 반려견 고르기 (UI 근거, grep으로 확인)

- 상태: `S.dogs`(배열), `S.cur`(고른 번호, 처음 0 = 왕건이). 데이터는 `gg.pets`의 `{"pid":"P-DEMO-01","name":"왕건이",...}`, `{"pid":"P-DEMO-02","name":"오숑이","sp":"강아지","breed":"비숑 프리제","birth":"2023-05-08","weight":"","cond":"","med":"","note":"차량 이동에 예민함","allergy":"","vet":"","photo":"data:image/jpeg;base64,..."}`. **성별 필드는 없다.**
- 동작 두 가지:
  - `cycle()`: 두 마리 이상일 때 다음 반려견으로 (`S.cur=(S.cur+1)%S.dogs.length`), 고른 증상을 비우고(`S.sel=[];S.cust=[]`), 릴렉스 설정을 그 반려견 것으로 다시 읽고(`rxInit`), 토스트 "{이름}로 변경했습니다."
  - `pickDog(i)`: 같은 반려견이면 아무것도 안 함. 아니면 `S.cur=i` + 위와 같이 증상 비우기·릴렉스 다시 읽기.
- 화면 세 곳 (별도 시트·목록은 **없다**. 다 "바로 바꾸기"다):
  1. **홈 위쪽 줄** (`hdr.chg = S.dogs.length>1`): 사진(64px 원) + 이름(30/600) + 상태 점·글자 오른쪽에 `[⇄ 변경]` 버튼(`aria-label="반려동물 변경"`, 높이 96, 좌우 16, 바탕 투명, 글자색 `--selt #2F4A99`, 30/500, 32px 채운 아이콘 `M6.99 11L3 15l3.99 4v-3H14v-2H6.99v-3zM21 9l-3.99-4v3H10v2h7.01v3L21 9z` + "변경"). 누르면 `cycle`. 그 오른쪽에 세로선(2×40 `--hln`)과 [음성 명령].
  2. **반려동물 카드 머리 줄**: 사진(104) + 이름(40/600) + 보조 줄 오른쪽 끝(`margin-left:auto`)에 `role="tablist" aria-label="반려동물 선택"` 탭 줄(아래 2px `--hln`). 탭마다 높이 96, 좌우 32, 34px, 고른 탭은 `--pr` 색·600·아래 4px `--pr` 선, 아닌 탭은 `--sub` 색·400. 누르면 `pickDog(i)`. 두 마리 이상일 때만(`dog.multi`).
  3. **증상 선택 머리** (`hdr.pet`): "반려동물 {이름}" 오른쪽에 `[변경]`(높이 96, 좌우 24, 둥글기 16, 바탕 `--sf #F4F5F7`, 30/500). 누르면 `cycle`.
  - 그 밖에 `tiles4`(4칸 바로가기의 "반려동물 변경" 칸)가 있으나 앱은 3칸 바로가기를 쓰므로 넣지 않는다.
- 프로토타입 `run()`에는 주행 중 막기가 없다. 앱에서는 홈·반려동물 카드·증상 선택이 모두 정차 화면이라 주행 중에는 보이지 않는다 (아래 "주행 중 안전").

## 지금 코드와 바꿀 곳 (조사 결과)

- `data/pet/SamplePetProfiles.kt`: `demoPet`(보리, 포메라니안, MALE, 2014-08-01, 알레르기·심장약 있음) 하나 + `DEMO_WEIGHT`("2.8kg")·`DEMO_TRAITS`. KDoc에 "값은 docs/data/pet-profile.example.json과 같다"고 적혀 있으나 이미 품종이 다르다(2026-11-14).
- `data/pet/FakePetProfileSource.kt`: 기본값 `Loaded(SamplePetProfiles.demoPet)`, `set()`으로 바꿈. 테스트가 많이 쓴다 → 그대로 둔다.
- `data/pet/LinkedPetProfileSource.kt`: 연결 전에는 `sample.profile`을 그대로 내보낸다(`Origin.SAMPLE`). sample이 새 값을 내보내면 새 값이 그대로 화면에 간다 → 고르기를 sample 쪽에만 넣으면 된다. 연결됨이면 서버 값만 보인다(바꾸지 않음).
- `di/AppContainer.kt`: 296행 `sample = FakePetProfileSource()`, 280행 `LinkKind.FAKE -> LinkedProfileReader { Loaded(SamplePetProfiles.demoPet) }`.
- `MainActivity.kt`: 160행 `LocalSamplePetPhoto provides isPetProfileSample`, 425~426행 `sampleWeight/sampleTraits = SamplePetProfiles.DEMO_*`.
- `ui/card/PetVisuals.kt`: `LocalSamplePetPhoto`(Boolean) + `R.drawable.sample_pet_photo` 하나. 두 마리면 어느 사진인지 알아야 한다.
- `ui/home/ParkedScreen.kt` `TopBar`(176~213행): 아바타 + 이름 + 상태 + `LinkAction`(보호자 웹 연결 버튼, 019 D2로 프로토타입 [⇄ 변경] 자리에 둠).
- `ui/pet/PetCardScreen.kt`: `sampleWeight`/`sampleTraits` 인자, 미리보기가 `data.pet.SamplePetProfiles`를 import한다 (ui → data, 규칙 어긋남).
- 반려견 이름은 모두 `cardUiState.card.pet`(= `petProfileSource`)에서 온다 (홈, 증상 선택, 진료 카드, 이동 중, 도착·귀가, 릴렉스). 출처만 바꾸면 따라 바뀐다.
- 성별은 진료 카드(`MedicalCardDesign.kt` 106행 "수컷", `MedicalCardScreen.kt` 219행 ♂)와 병원에 보내는 카드(`FirestoreJson` `sex`)에 쓰인다. `PetProfile.sex`는 null을 허용한다 (null이면 안 보이고, 서버에는 nullValue).
- 진료 기록(`VisitViewModel.records`)과 릴렉스 설정(`RelaxViewModel`)은 반려견을 구분하지 않는다 (프로토타입은 반려견별). → 결정 D4.

### 테스트 분류 (`보리`, `demoPet` grep 결과)

- **A. 샘플 값 자체를 검사 → 왕건이 값으로 고친다**
  - `data/pet/FakePetProfileSourceTest.kt` 17~38행 (`보리는 예시 JSON과 같은 값이다`): 값 검사는 새 `SamplePetProfilesTest`로 옮기고, 여기서는 "처음 값은 `SamplePetProfiles.demoPet`"만 남긴다. 테스트 이름에서 "보리"를 뺀다.
  - `ui/pet/PetCardContentTest.kt` `샘플 보리를 카드 값으로 바꾼다`: 왕건이(이름·첫 글자 "왕"·래브라도 리트리버·2014-03-12·오늘 2026-11-13이면 12세 8개월·질환 ["심장질환"]·약 [`MedicationText("심장질환 약 복용 중(상세 약명 미등록)", null)`]·알레르기 []).
  - `ui/card/MedicalCardViewModelTest.kt` 40·44행 (`FakePetProfileSource()` 기본값 이름 "보리") → "왕건이".
  - `ui/card/MedicalCardViewModelBoundaryTest.kt` 145행 이름 "보리" → "왕건이". 284행 `listOf("특정 단백질 알레르기")` → 왕건이는 알레르기가 없으므로 같은 뜻의 검사로 바꾼다: `card.medical!!.conditions == listOf("심장질환")`. 163~175행은 `updatedAt` 2026-11-05에 기대므로 왕건이·오숑이의 `updatedAt`은 지금 값(`2026-11-05T00:00:00Z`)을 유지한다.
  - `data/server/FirestorePetProfileJsonBoundaryTest.kt` 166행 `SamplePetProfiles.demoPet.medical`(예시 JSON과 비교) → `ExamplePetProfiles.bori.medical`.
  - `data/server/FirestoreJsonBoundaryTest.kt` 306행 `"2014-08-01"`(demoPet 생년월일) → `"2014-03-12"`.
  - `data/pet/ExamplePetProfiles.kt` (test): 지금 `SamplePetProfiles.demoPet.copy(breed = "골든리트리버")`라서 샘플을 바꾸면 같이 바뀐다. **예시 JSON 값을 직접 다 적은 독립 값으로 바꾼다** (단계 1 첫 커밋). 이걸 쓰는 `FirestorePetProfileJsonTest`, `FirestoreJsonTest:105`, `MedicalCardContentTest`, `MedicalCardContentBoundaryTest`는 그러면 손대지 않아도 된다.
- **B. "로그에 이름이 없어야 한다" 검사 → 이름을 바꾸지 않으면 헛돈다 → "왕건이"로**
  - `domain/trip/HospitalTripTest.kt` 343행, `domain/trip/HospitalTripAckTest.kt` 288행.
- **C. demoPet을 아무 바탕 값으로만 쓰고 자기 자신과 비교 → 그대로 둔다** (값이 바뀌어도 통과해야 한다. 깨지면 그 이유를 진행 로그에 적고 고친다)
  - `HospitalTrip*Test`(카드 바탕), `FirestoreClients*Test`, `FirestoreJsonTest:118`, `FirestoreJsonBoundaryTest`(306행 빼고), `TripTimingBoundaryTest`, `LinkedPetProfileSource*Test`, `LinkViewModel*Test`, `LinkTexts*Test`, `PetCardContentBoundaryTest`, `MedicalCardViewModelHospitalBoundaryTest`, `MedicalCardTripFlow*Test`, `CardHomeButtonTest`.
- **D. 테스트 이름·주석에만 "샘플 보리" → 있으면 좋음 (단계 6)**
  - `di/PetPlanTest.kt` 16·25행, `di/PetPlanBoundaryTest.kt` 28행, `data/pet/FakePetProfileSourceBoundaryTest.kt` 64행, `data/pet/LinkedPetProfileSourceBoundaryTest.kt` 21·68·83·91·102행, `ui/link/LinkViewModelBoundaryTest.kt` 322~337행. "샘플 보리" → "샘플"로만 바꾼다 (동작 검사는 그대로).
- **E. 샘플과 상관없는 예시 이름 → 그대로 둔다**
  - `ui/KoreanParticlesTest.kt`, `ui/card/MedicalCardContentTest.kt`·`MedicalCardContentBoundaryTest.kt`(예시 JSON 값 `ExamplePetProfiles` + 자기 데이터), `ui/trip/TripViewTest.kt:20`, `domain/pet/PetAgeTest.kt`, `data/server/FirestorePetProfileJsonTest.kt`(예시 JSON의 보리. 테스트 이름 `예시 프로필을 읽으면 샘플 보리와 같다`만 "예시 보리와 같다"로 바꿔도 된다, 단계 6).

## 설계

### 예시 JSON과 샘플의 연결 (사용자 요청: 어떻게 끊거나 바꿀지)

- **끊는다.** 차 앱 샘플은 이제 예시 JSON(보리)과 값이 다르다. 연결은 테스트 쪽 `ExamplePetProfiles`가 맡는다.
  - `SamplePetProfiles` KDoc: "시연용 샘플 두 마리. 값의 원천은 디자인 프로토타입(`docs/product/design/prototype/MeongCareCar.html`의 P-DEMO-01/02)과 2026-11-15 사용자 결정(왕건이 품종·체중)이다. `docs/data/pet-profile.example.json`(보리, 형식 예시·보호자 웹 샘플)과는 다르다. 예시 JSON과 같은 값이 필요한 테스트는 test의 `ExamplePetProfiles`를 쓴다."
  - `ExamplePetProfiles` KDoc: "docs/data/pet-profile.example.json과 같은 값. 그 파일을 바꾸면 여기도 바꾼다 (`FirestorePetProfileJsonTest`가 대조한다). 차 앱 샘플과는 상관없다."
  - 예시 JSON·웹을 왕건이로 맞추는 일은 "후속".

### 데이터 (domain / data)

- 새 `domain/pet/SamplePet.kt`:
  - `data class SamplePet(val id: String, val profile: PetProfile, val weight: String?, val traits: String?)` — 체중·특징은 프로필 형식에 없는 화면 표시용 값 (지금 `DEMO_WEIGHT`/`DEMO_TRAITS`를 반려견마다 둔다).
  - `interface SamplePetChoice { val pets: List<SamplePet>; val selected: StateFlow<SamplePet>; fun select(id: String); fun selectNext() }` — 화면은 이것만 안다. 모르는 id는 무시, 같은 id는 값을 다시 내보내지 않는다, `selectNext()`는 끝에서 처음으로 돈다(프로토타입 `cycle`), 한 마리면 아무 일 없음.
- `data/pet/SamplePetProfiles.kt`:
  - `wanggeon = SamplePet("P-DEMO-01", PetProfile(name="왕건이", breed="래브라도 리트리버", sex=<D1>, birthDate=2014-03-12, birthDateIsEstimated=false, photoDataUrl=null, medical=MedicalInfo(conditions=["심장질환"], allergies=[], medications=[Medication("심장질환 약 복용 중(상세 약명 미등록)", "")]), updatedAt=2026-11-05T00:00:00Z), weight="30kg", traits=null)`
  - `osyong = SamplePet("P-DEMO-02", PetProfile(name="오숑이", breed="비숑 프리제", sex=<D1>, birthDate=2023-05-08, false, null, MedicalInfo([], [], []), 2026-11-05T00:00:00Z), weight=null, traits="차량 이동에 예민함")`
  - `all = listOf(wanggeon, osyong)`, `val demoPet: PetProfile get() = wanggeon.profile` (속성 이름 `demoPet`을 남겨 C 분류 약 30개 테스트 파일을 건드리지 않는다).
  - `DEMO_WEIGHT`, `DEMO_TRAITS`는 지운다.
- 새 `data/pet/SamplePetChooser.kt`: `class SamplePetChooser(pets: List<SamplePet> = SamplePetProfiles.all) : SamplePetChoice, PetProfileSource`. 메모리(`MutableStateFlow`)에만 둔다 (D5). `profile = selected.map { PetProfileLoad.Loaded(it.profile) }`.
- `di/AppContainer.kt`: `val samplePets: SamplePetChoice = samplePetChooser`. `LinkedPetProfileSource(sample = samplePetChooser, ...)`. Fake 연결의 reader는 읽는 순간 고른 샘플(`samplePetChooser.selected.value.profile`)을 돌려준다.

### 고른 반려견과 보호자 웹 연결 (지금 동작 유지)

| 상태 | 화면의 반려견 | 고르기 |
|---|---|---|
| 연결 없음 / 연결 전 / 끊김 (Fake·시연 모드) | 고른 샘플 | 보인다 |
| Real 모드 연결 전 | 등록 안 됨 (지금과 같다) | 없다 (샘플이 아니다) |
| 서버 연결됨 | 보호자가 등록한 프로필 (지금과 같다. 못 읽으면 샘플로 대신하지 않는다) | 없다 |
| Fake 연결됨 (debug) | 연결할 때 고른 샘플 | 없다 |
| 연결 끊김·취소 뒤 | 마지막으로 고른 샘플로 돌아온다 | 보인다 |

- `LinkedPetProfileSource`는 바꾸지 않는다. 연결 판단은 지금 그대로 `ownerLink.state`가 한다.

### 고르기 버튼을 보일지 (순수 함수, JVM 테스트)

- 새 `ui/pet/PetChoice.kt`: `fun petChoiceOf(pets: List<PetChoiceItem>, selectedId: String, isSample: Boolean, linked: Boolean, tripActive: Boolean): PetChoiceUi?`
  - null(숨김): 두 마리 미만, `isSample == false`, `linked`(연결됨, Fake 포함), `tripActive`(안내 중·도착·출발한 trip이 있음).
  - 아니면 `PetChoiceUi(tabs = [(id, name, selected)], nextId, nextName)`.
  - `PetChoiceItem(id, name)`은 ui 쪽 값이다. `MainActivity`가 `container.samplePets`에서 만든다 (ui는 data를 import하지 않는다).
- 고르면 (`MainActivity`): `container.samplePets.select(id)` 다음 `cardViewModel.onCloseCard()` (D2 기본안: 만든 카드·번호 패널을 지운다, 프로토타입의 `S.sel=[]`과 같은 뜻).

### 사진

- `app/src/main/res/drawable/sample_pet_wanggeon.jpg`(P-DEMO-01), `sample_pet_osyong.jpg`(P-DEMO-02). `sample_pet_photo.jpg`(보리)는 지운다.
- `ui/card/PetVisuals.kt`: `LocalSamplePetPhoto`를 `compositionLocalOf<Int?> { null }`(@DrawableRes)로 바꾸고, `fun samplePetPhotoRes(id: String): Int?`(P-DEMO-01 → 왕건이, P-DEMO-02 → 오숑이, 그 밖 null)를 둔다. `PetAvatar`는 값이 있고 `initial`이 비지 않았으면 그 사진, 아니면 지금처럼 첫 글자.
- `MainActivity`: `LocalSamplePetPhoto provides if (isPetProfileSample) samplePetPhotoRes(selected.id) else null`.

### 주행 중 안전 (AGENTS.md 규칙 3)

- 고르기는 **정차·주차 중에만** 있다. 홈·반려동물 카드·증상 선택은 정차 화면이고, 주행·모름이면 `homeContentOf`가 먼저 주행 화면을 고른다 (`HomeContent.kt`, 바꾸지 않음. 기존 `HomeContentTest`가 지킨다). 주행 화면(`DrivingScreen.kt`)에는 고르기를 넣지 않는다.
- 근거: `docs/pleos/ux-guidelines.md` 101행, 설정류 기능은 주행 중 ⛔(`UX_RESTRICTIONS_NO_SETUP`). 반려견 바꾸기는 진료 카드에 들어갈 의료정보를 바꾸는 설정이다.
- 글자 입력 없음. 버튼·탭은 프로토타입 높이 96(비율 적용 뒤 최소 `scale.touch` 56dp).
- 주행 → 정차로 돌아와도 고른 반려견은 그대로다 (`AppContainer` 프로세스 수명).

## 확인 필요 (시작 전에 해결)

새 Pleos API·새 `pleos.car.permission.*` 권한: **없다.** `[선행: pleos-docs-researcher]` 항목 없음.

- **D1 [사용자 승인 필요] 성별**: 프로토타입 데이터에 성별이 없다. 지금 앱은 진료 카드에 "♂/수컷"을 보이고 병원에 보내는 카드에도 `sex`를 넣는다.
  - 기본안 A: 두 마리 다 `sex = null` (모름). 진료 카드에 성별이 안 보이고, 병원 카드에는 null이 간다 (형식이 허용함). 추측한 값을 보이지 않는다.
  - 안 B: 왕건이는 지금 값 MALE을 그대로, 오숑이는 null.
  - 안 C: 사용자가 두 마리 성별을 알려 준다.
- **D2 (기획 결정) 이미 만든 카드가 있을 때 바꾸면**: 기본안 = 카드를 지운다 (프로토타입 `cycle`/`pickDog`가 고른 증상을 비운다). 안 = 카드가 있으면 고르기를 숨긴다. 안내 중·도착·출발 뒤에는 어느 안이든 고르기가 없다.
- **D3 (기획 결정) 고르는 곳**: 기본안 = 필수는 홈 [⇄ 변경](누를 때마다 다음 반려견, 프로토타입 `cycle`). 있으면 좋음은 반려동물 카드 이름 탭(단계 5), 증상 선택 머리 [변경](단계 7). 프로토타입의 토스트 "{이름}로 변경했습니다."는 넣지 않는다 (사진·이름이 바로 바뀐다). 홈에서 [⇄ 변경]은 반려견 줄 바로 오른쪽, 보호자 웹 연결 버튼(019 D2)은 그 오른쪽에 둔다 (프로토타입: 반려견 → [⇄ 변경] → 세로선 → [음성 명령]). 제출 APK는 서버 설정이 없어 연결 버튼이 없으므로 [⇄ 변경]만 보인다.
- **D4 (기획 결정) 반려견별 릴렉스 설정·진료 기록**: 프로토타입은 반려견마다 따로다 (`rxInit`, `mine`). 기본안 = 이번에는 나누지 않는다 (지금처럼 공통). 후속으로 둔다. 시연 영상에서 진료 기록을 저장한 뒤 반려견을 바꾸는 장면은 찍지 않는다.
- **D5 (기획 결정) 고른 반려견 기억**: 기본안 = 앱 프로세스 동안만. 앱을 다시 켜면 왕건이(프로토타입 `S.cur=0`).
- **D6 (기획 결정) 빈 값 표시**: "알레르기 없음"·오숑이의 "질환·약 없음"은 빈 목록으로 넣는다. 화면은 지금 규칙 그대로 (반려동물 카드는 "미등록", 진료 카드는 지금 빈 칸 처리). "없음"으로 따로 보이려면 형식에 없는 구분이 필요해서 이번에는 하지 않는다.
- **[확인 필요] 사진 사용 권리**: 두 사진은 프로토타입에 들어 있던 것이다. 누가 찍었거나 어디서 받은 사진인지, 출품물에 써도 되는지 팀이 확인한다 (`docs/product/feature-facts.md` 43행에 보리 사진도 같은 확인이 적혀 있다). 코드 작업은 막지 않지만 제출 전에 답이 있어야 한다.

## 단계

단계 하나 = 커밋 1~3개. 커밋마다 `assembleDebug` 통과 (훅이 확인).
**필수**: 1, 2, 3, 4, 8, 9, 10 (11/15 APK·시연 영상에 바로 보이는 순서). **있으면 좋음**: 5(반려동물 카드 탭) → 6(테스트 이름 정리) → 7(증상 선택 [변경]) 순서. 시간이 모자라면 5~7을 건너뛰고 진행 로그에 적는다.
Real SDK 래퍼 단계는 없다 (SDK를 쓰지 않는 메모리 값이다).

- [ ] 1. 예시 JSON 값 떼어 놓기 + 샘플 두 마리 (데이터) → 커밋 2개
  - `test(data): 예시 JSON 반려견 값을 샘플과 따로 둠`
    - `ExamplePetProfiles.bori`를 예시 JSON 값을 다 적은 독립 `PetProfile`로 (이름 보리, 골든리트리버, MALE, 2014-08-01, 추정 아님, 사진 null, 질환 ["심장질환"], 알레르기 ["특정 단백질 알레르기"], 약 [`Medication("심장약", "1일 1회")`], `2026-11-05T00:00:00Z`) + 위 KDoc.
    - `FirestorePetProfileJsonBoundaryTest.kt` 166행 → `ExamplePetProfiles.bori.medical`.
    - 동작 변화 없음: `testDebugUnitTest` 전체 통과.
  - `feat(data): 시연용 샘플 반려견을 왕건이·오숑이로 변경`
    - 새 `domain/pet/SamplePet.kt` (`SamplePet`, `SamplePetChoice`), `SamplePetProfiles` 바꾸기 (위 "데이터", 성별은 D1 결정값), KDoc(위 "예시 JSON과의 연결").
    - `DEMO_WEIGHT`/`DEMO_TRAITS`를 지우므로 `MainActivity` 425~426행은 이 커밋에서 임시로 `SamplePetProfiles.wanggeon.weight`/`traits`를 넘긴다 (단계 3에서 고른 반려견 값으로 바뀐다).
    - 새 테스트 `data/pet/SamplePetProfilesTest`: 두 마리 값 전부, id가 서로 다르다, `all` 순서(왕건이 먼저), `demoPet == wanggeon.profile`.
    - 위 "테스트 분류" A·B를 고친다. 고친 파일 목록을 진행 로그에 적는다 (완료 기준 2).
- [ ] 2. 고르기 Fake + 연결 → 커밋 1개
  - `feat(data): 샘플 반려견 고르기 추가`
    - 새 `data/pet/SamplePetChooser.kt`, `AppContainer`의 `samplePets`·`sample`·Fake reader (위 "데이터").
    - 새 테스트 `data/pet/SamplePetChooserTest`: 처음은 왕건이 / `select("P-DEMO-02")`면 `selected`와 `profile`이 오숑이 / 같은 id는 다시 내보내지 않음 / 모르는 id 무시 / `selectNext()`가 01→02→01 / 한 마리면 `selectNext()`가 그대로.
    - 새 테스트 `data/pet/LinkedPetProfileSourceSampleChoiceTest`: 연결 전 고르면 새 샘플이 나온다 / 연결됨이면 고르기를 바꿔도 서버 값만 나온다 / 끊기면 마지막으로 고른 샘플이 나온다 / `sampleBeforeLink=false`면 고르기와 상관없이 등록 안 됨.
- [ ] 3. 사진 두 장 (UI) → 커밋 1개
  - `feat(ui): 샘플 반려견마다 사진을 보임`
    - drawable 두 장 추가, `sample_pet_photo.jpg` 삭제, `PetVisuals.kt`(`LocalSamplePetPhoto: Int?`, `samplePetPhotoRes`), `MainActivity` provides.
    - `PetCardScreen`: `sampleWeight`/`sampleTraits`를 고른 샘플의 `weight`/`traits`로 (샘플일 때만, 지금 규칙 그대로). 미리보기는 `data` import를 빼고 파일 안의 미리보기 값(왕건이)을 쓴다.
    - 새 테스트 `ui/card/SamplePetPhotoTest`: 두 id가 서로 다른 null 아닌 값, 모르는 id·빈 문자열은 null.
- [ ] 4. 홈 [⇄ 변경] (UI, Fake·시연 모드로 확인) → 커밋 1~2개
  - `feat(ui): 홈에서 샘플 반려견 바꾸기`
    - `HomeIcons.Swap`(프로토타입 채운 아이콘 경로, 팀 디자인 자료).
    - 새 `ui/pet/PetChoice.kt`(`PetChoiceItem`, `PetChoiceUi`, `petChoiceOf`) + 새 테스트 `ui/pet/PetChoiceTest`: 두 마리·샘플·연결 안 됨·trip 없음이면 보임, `nextId`가 다음 반려견(끝이면 처음) / 한 마리·샘플 아님·연결됨·trip 있음이면 null / 탭의 `selected`가 고른 하나뿐.
    - `ParkedScreen`(`TopBar`)·`HomeScreen`에 인자 `petChoice: PetChoiceUi? = null`, `onSelectPet: (String) -> Unit = {}`. 반려견 줄 오른쪽에 [⇄ 변경] (프로토타입 1번 모양, 높이 `scale.touch(96.dp)`, `contentDescription` "반려동물 변경"). 누르면 `onSelectPet(nextId)`.
    - `MainActivity`: `selected`를 구독해 `petChoiceOf(...)`를 만든다 (`linked = linkState is OwnerLinkState.Linked`, `tripActive = guidanceUiState !is Idle || tripState != null`). `onSelectPet = { samplePets.select(it); cardViewModel.onCloseCard() }` (D2).
    - 새 문자열: `home_pet_change` "변경", `home_pet_change_description` "반려동물 변경".
    - 미리보기: 두 마리 + 연결 버튼 없음.
  - (UI만 따로 커밋하고 싶으면 `PetChoice.kt`+테스트를 먼저 `feat(ui): 반려견 고르기 표시 규칙 추가`로 나눈다.)
- [ ] 5. (있으면 좋음) 반려동물 카드 이름 탭 → `feat(ui): 반려동물 카드에서 샘플 반려견 고르기`
  - `PetCardScreen`에 `petChoice: PetChoiceUi? = null`, `onSelectPet`. 머리 줄 오른쪽 끝에 탭 줄 (프로토타입 2번 모양). 고른 탭 primary·SemiBold·아래 4dp 선, 아닌 탭 homeSubText. 같은 탭을 누르면 아무 일 없음.
  - 연결은 단계 4와 같은 `onSelectPet`.
- [ ] 6. (있으면 좋음) 주석·미리보기·테스트 이름 정리 → 커밋 1~2개
  - `docs(app): 샘플 반려견 주석과 미리보기를 왕건이로` — 완료 기준 4의 `보리` 0건을 만든다: `PetPlan.kt`(10·19·29~31·45행), `PetProfileSource.kt` 7행, `AppContainer.kt` 279·302행, `FakePetProfileSource.kt` 9행, `PetVisuals.kt` 40·46행, `PetCardScreen.kt` 156행 주석, `MedicalCardScreen.kt` 274~282행 미리보기 값(주석 "값은 시연용 샘플과 같다"도 왕건이 값에 맞춘다), `SymptomPickerScreen.kt` 165·332·351행, `TripScreens.kt` 280행, `RelaxScreen.kt` 136·276행, `SymptomPickerScreen.kt` 93행 "한 마리만 다루므로 넣지 않았다"는 단계 7을 안 하면 "증상 선택에는 넣지 않았다(021 D3)"로.
    - 주의: 완료 기준 4는 이 커밋에 기댄다. 단계 6을 건너뛰면 **주석·미리보기 줄만이라도** 이 커밋을 낸다 (필수 쪽으로 올린다). 테스트 이름 정리만 있으면 좋음이다.
  - `test: 테스트 이름의 샘플 보리를 샘플로` — "테스트 분류" D (+ E의 `FirestorePetProfileJsonTest` 이름).
- [ ] 7. (있으면 좋음) 증상 선택 머리 [변경] → `feat(ui): 증상 선택에서 샘플 반려견 바꾸기`
  - `SymptomPickerScreen`에 `petChoice`, `onSelectPet`. "반려동물 {이름}" 오른쪽 [변경] (프로토타입 3번 모양). 누르면 `samplePets.select(nextId)` + 고르던 증상을 비운다 (`cardViewModel.onCancelPicking()` 뒤 `onStartCard()`, 또는 세션에 "고르던 것 비우기"가 있으면 그것. 구현 전에 `MedicalCardSession`을 읽고 진행 로그에 적는다). 음성 듣는 중이면 멈춘다(`voiceActions.onStop`).
- [ ] 8. 테스트 (`tester`) → `test(...): 샘플 반려견 고르기 경계 테스트`
  - 넘길 것: 이 계획 파일 경로, 대상 `domain/pet/SamplePet.kt`, `data/pet/SamplePetProfiles.kt`, `data/pet/SamplePetChooser.kt`, `ui/pet/PetChoice.kt`, `ui/card/PetVisuals.kt`(`samplePetPhotoRes`), `di/AppContainer.kt`의 샘플 연결(Fake reader). 구현 설명은 넘기지 않는다.
  - `testDebugUnitTest` 전체 통과. 완료 기준 2·3·4의 확인 결과를 진행 로그에 적는다.
- [ ] 9. 화면 확인 (`/verify`) → 고칠 것이 있으면 `fix(ui): ...`
  - 완료 기준 5의 절차. 프로토타입 홈과 나란히 본 결과(사진 크기·[⇄ 변경] 위치), 주행 전환 결과를 진행 로그에 적는다. 다 되면 사용자에게 에뮬레이터 화면을 바로 보여 준다.
  - `docs: 차 앱 시연 샘플 반려견을 왕건이·오숑이로` — 완료 기준 6:
    - `docs/architecture.md` 158행(`SamplePetProfiles` 설명, `SamplePetChooser`·`SamplePetChoice` 추가), 310~312행 표("샘플 보리" → "고른 샘플(왕건이·오숑이)"), 357행.
    - `docs/product/feature-facts.md` 43행(샘플 반려견 두 마리, 사진은 프로토타입 사진·사용 권리 확인 필요, 왕건이 체중 30kg은 정해 둔 값), 53·54행("보리" → "샘플 반려견 왕건이·오숑이, 정차 중 홈에서 고름").
    - `docs/data/pet-profile.md` 8행, `docs/data/owner-link.md` 72행: 차 앱 설명 한 줄씩만 ("이 예시와 같은 값의 샘플 보리" → "차 앱 시연 샘플(예시와 값이 다름, 021)", "샘플 보리로" → "샘플로"). **예시 JSON은 고치지 않는다.**
- [ ] 10. 개발 노트 작성 (`docs/notes/xxj15/2026-11-15-prototype-pets.md`) → `docs(notes): prototype-pets`
  - 계획 파일을 `docs/plans/completed/`로 옮기는 것도 커밋한다 (`docs(plans): 021 완료로 옮김`).
  - 그다음 `/ship`. 제출용 APK가 필요하면 사용자가 `/promote` → `/release-apk`를 요청한다 (스스로 하지 않는다).

## 후속 (이번 범위 밖)

- 보호자 웹 샘플(`web/`)과 `MeongCareWeb.html` 프로토타입의 반려견을 차 앱과 맞추기.
- `firebase/` 시드의 반려견, `docs/data/*.example.json`(보리)을 왕건이·오숑이로 바꿀지 결정 (바꾸면 웹 테스트 `petProfileJson.test.ts` 등과 앱 `ExamplePetProfiles`를 같이 고친다. 019 D1 기록 참고).
- `README.md`와 `docs/readme/*.png`·`demo.gif`의 보리 화면을 새 화면으로 다시 찍기.
- 반려견별 릴렉스 설정·진료 기록 (D4).
- 고른 반려견을 앱 재시작 뒤에도 기억하기 (D5).

## 진행 로그

- 2026-11-15: 계획 작성. 브랜치 `feat/prototype-pets`(dev `5b034b8`). 사용자 결정은 위 "사용자가 정한 것".
- 2026-11-15: 사용자 결정 D1 = 두 마리 다 MALE, D2~D6 기본안, 사진 사용 가능. 구현 시작.

## 결정 / 배운 점
