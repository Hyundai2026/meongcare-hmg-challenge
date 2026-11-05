# 009 진료 카드 (증상 고르기 → 진료 카드 → 병원 전화·길안내 → 도착 후 다시 보기)

- 목표: 정차 중에 증상을 큰 버튼으로 골라 Fake 반려견 프로필("보리")과 합친 진료 카드를 만들고, 그 카드에서 병원 전화(전화 앱 열기)와 길안내를 시작하며, 도착해 정차하면 같은 카드를 다시 보여 준다.
- 완료 기준 (어떻게 검증하나):
  1. `node .claude/hooks/build-gate.mjs pipeline`이 "검증 파이프라인 통과."를 출력한다.
  2. `.\gradlew.bat assembleRelease`가 성공한다.
  3. 단위 테스트:
     - 새 테스트가 통과한다: `PetAgeTest`, `FakePetProfileSourceTest`, `MedicalCardSessionTest`, `MedicalCardContentTest`, `ParkedContentTest`, `MedicalCardViewModelTest`, `MedicalCardTripFlowTest`, 그리고 tester가 추가한 경계 테스트.
     - 기존 테스트 185개가 그대로 통과한다.
     - `git diff dev --diff-filter=MD --name-only -- app/src/test`의 출력이 비어 있다. 기존 테스트 파일을 고치거나 지우지 않는다 (추가만).
     - 아래 `git diff dev -- <경로>`의 출력이 비어 있다. 주행 안전 규칙과 길안내·주행 상태 계약을 건드리지 않는다는 뜻이다:
       - `app/src/main/java/com/meongcare/app/domain/driving/`, `domain/guidance/`
       - `app/src/main/java/com/meongcare/app/data/driving/`, `data/guidance/`, `data/demo/`
       - `app/src/main/java/com/meongcare/app/di/SourcePlan.kt`, `di/DataMode.kt`
       - `app/src/main/java/com/meongcare/app/ui/home/HomeViewModel.kt`, `ui/home/HomeContent.kt`, `ui/home/DrivingScreen.kt`
       - `app/src/main/java/com/meongcare/app/ui/guidance/GuidanceViewModel.kt`
  4. 정적 검사 (Grep):
     - `app/src/main/java/com/meongcare/app/ui/` 아래에 `import com.meongcare.app.data` 0건, `import com.meongcare.app.di` 0건
     - `app/src/main/java` 전체에 `TextField` 0건 (정차 화면 메모 칸을 뺐으므로 앱에 글자 입력이 없다)
     - `ACTION_DIAL`은 `data/phone/AndroidPhoneDialer.kt`에만 있다. `ACTION_CALL` 0건
     - `app/src/main/AndroidManifest.xml`에 `<queries>` 0건, `CALL_PHONE` 0건
     - 병합 매니페스트(`app/build/intermediates/` 아래 `AndroidManifest.xml`)에 `android:name="pleos.car.permission` 0건, `CALL_PHONE` 0건
     - `domain/pet/`, `domain/card/`, `data/pet/`, `ui/card/`에 `Log.` 0건 (의료정보를 로그에 남기지 않는다)
  5. 일반 폰 에뮬레이터 (`my_avd`): 아래 "확인 절차"의 P1~P14.
  6. Pleos 에뮬레이터 (`pleos_connect`, Cold boot): 아래 "확인 절차"의 D1~D9. `adb logcat -d -t 300 *:E`에 `com.meongcare.app` 크래시와 `SecurityException`이 없다.
  7. 문서:
     - `docs/architecture.md` "주행 상태 × 길안내 상태 화면 조합"에 아래 설계 결정 1의 두 표가 있다. "패키지 구조"에 새 패키지가 있다. "데이터 모드"에 "반려견 프로필은 모든 모드에서 Fake"가 있다.
     - `docs/data/pet-profile.md`의 "Kotlin 대응"이 "(제안)"이 아니라 확정이고, 진료 카드 표시 규칙(빈 목록, 순서, 공백 항목)이 있다.
     - `README.md`의 두 시연 방법이 새 흐름([진료 카드 만들기] → 증상 → [카드 만들기] → [길 안내 시작])이고, 반려견 정보·병원 번호가 시연용이라는 문장이 있다.
     - `docs/pleos/sdk-notes.md` "전화" 절에 Pleos 에뮬레이터에서 `ACTION_DIAL`을 보낸 결과가 관찰로 적혀 있다.
- 브랜치: `feat/medical-card` (dev `cb37087`에서 땄다)
- 관련 문서:
  - AGENTS.md (아키텍처 규칙, 규칙 1·2·3·7)
  - docs/architecture.md "서비스 기능 스택"(병원 전화, 증상 → 진료 카드, 의료 프로필 저장), "패키지 구조", "데이터 모드", "주행 상태 × 길안내 상태 화면 조합", 문의 A1·B4
  - docs/data/pet-profile.md (필드, 읽는 쪽 규칙, 나이 계산, Kotlin 대응), docs/data/pet-profile.example.json ("보리")
  - web/src/domain/age.ts, web/src/ui/profileText.ts (`initialOf`, `medicationText`), web/src/ui/PawIcon.tsx (발바닥 그림)
  - docs/pleos/ux-guidelines.md "화면 구성"(가로 전체 1682×1210dp 관찰, 반쪽 창), "멀티 윈도우 정책"(딥링크로 다른 앱을 띄우면 그 앱이 덮고, 끝나면 돌아온다), "색상과 테마"(Static Color), "주행 안전 설계 원칙"(프리셋 선택 우선)
  - docs/pleos/sdk-notes.md "전화 (2026-11-03 조사)"
  - docs/contest/requirements.md "출품 유의사항"(사실과 다르면 선정 취소, 제3자 권리)
  - docs/plans/completed/005-navi-route-fake.md, 007-release-demo-mode.md (화면 조합, 시연 모드, 시연용 표시, 확인 절차 방식), 008-owner-web.md
  - .claude/skills/verify/SKILL.md, docs/conventions/git.md

## 범위

- **필수** (약 1.3일, 두 에뮬레이터 확인 포함): 단계 0~10.
  - 11/15 제출물에 주는 효과:
    - 시연 영상: 컨셉 그림의 핵심 장면(증상 고르기 → 진료 카드 → 길안내 → 도착 후 카드)을 release APK 하나로 찍을 수 있다.
    - 기획서: "차에서는 글자를 입력하지 않고 프리셋 버튼 + 미리 등록한 정보로 카드를 만든다"를 구현된 사실로 쓸 수 있다 (프로필은 시연용 샘플이라고 밝힌다).
    - APK: 도착 화면의 "진료 카드는 준비 중이에요" 자리표시가 사라진다.
- **있으면 좋음**: 단계 A~C. 필수가 끝난 뒤, 단계 10(노트) 전에 한다.
- **이번 범위 밖**:
  - 서버(Firebase) 프로필 구현, 웹과 차 앱 연결, QR 페어링 (문의 A1 이후). 지금은 인터페이스와 Fake만.
  - 진료 카드 AI 정리(Claude API). 이번에는 규칙으로 조합한다 (문의 A1 이후, 팀 논의).
  - 병원 검색(카카오 로컬). 병원은 지금처럼 시연용 1곳.
  - 사진 표시. 이름 첫 글자 아바타만 두고, 사진을 끼울 자리만 남긴다 (저작권).
  - 음성으로 증상 말하기 (STT "준비 중", 문의 B2).
  - Room 저장. 이번 프로필은 메모리의 Fake라 기기에 저장하지 않는다.
  - 주행 화면 변경 (지금 그대로. 주행 중 카드·증상 고르기는 보이지 않는다).
  - 프로세스가 죽었다 살아날 때 고른 증상 복원 (설계 결정 2).

## 설계 결정 (planner 판단과 근거)

아래 코드 블록은 기본안이다. 이름과 세부는 구현하면서 바꿔도 된다. 바꾸면 이 블록을 고치고 "결정 / 배운 점"에 이유를 남긴다. `[사용자 승인 필요]`가 붙은 것은 "확인 필요"에서 묻는다.

### 1. 화면 조합: 주행 규칙은 그대로 두고, 정차 화면 안에서 한 번 더 고른다

주행 안전 규칙(`homeContentOf`: 주행·모름이면 무조건 주행 화면)은 **손대지 않는다.** 정차 쪽(`PARKED`, `ARRIVAL`) 안에서 무엇을 보일지만 새 순수 함수가 고른다.

```kotlin
// ui/home/ParkedContent.kt
enum class ParkedContent { HOME, SYMPTOMS, CARD }

/** 정차 화면(PARKED·ARRIVAL) 안에서 보일 것. 주행 중에는 불리지 않는다 (homeContentOf가 먼저 DRIVING을 고른다). */
fun parkedContentOf(guidance: GuidanceUiState, session: CardSessionState): ParkedContent
```

- 1단계 (기존 `homeContentOf`, 바꾸지 않음):

  | 주행 \ 안내 | 안내 없음 | 안내 중 | 도착 |
  |---|---|---|---|
  | **정차** | PARKED | PARKED | ARRIVAL |
  | **주행·모름** | DRIVING | DRIVING | DRIVING |

- 2단계 (새 `parkedContentOf`, PARKED·ARRIVAL일 때만):

  | 안내 \ 카드 세션 | 없음 (`None`) | 고르는 중 (`Picking`) | 카드 있음 (`Ready`) |
  |---|---|---|---|
  | 안내 없음 | **HOME**: 보리 요약 + [진료 카드 만들기] | **SYMPTOMS** | **CARD** "진료 카드 생성 완료" + [수정] [닫기] / [병원에 전화] [길 안내 시작] |
  | 안내 중 | **CARD** (고른 증상 없음) | **SYMPTOMS** | **CARD** "병원까지 N분"(모르면 "경로를 찾는 중") + 병원 이름 + [수정] / [병원에 전화] [안내 취소] |
  | 도착 | **CARD** (고른 증상 없음) | **SYMPTOMS** | **CARD** "도착했어요" + 병원 이름 + [수정] / [병원에 전화] [닫기] |

- 이유:
  - 기존 화면 조합 테스트(`HomeContentTest`, `HomeContentBoundaryTest`, `FakeTripScenarioScreenFlowTest`)가 "주행 중에는 아무것도 보이지 않는다"의 안전망으로 그대로 남는다. 완료 기준 3의 `git diff`로 확인한다.
  - **주행 중(UNKNOWN 포함)에는 HOME·SYMPTOMS·CARD 어느 것도 보이지 않는다.** 주행 화면(`DrivingScreen`)은 지금 그대로다.
- "안내 중·도착 + 세션 없음"도 카드로 보인다 (증상 줄은 "고른 증상 없음"). 지금 흐름에서는 안내가 카드에서만 시작되므로 생기지 않는다. 그래도 둔 이유: Real 길안내(NaviHelper)가 생기면 내비 앱에서 시작한 안내나, 우리 앱 프로세스가 다시 살아난 경우가 생긴다. 그때도 등록된 의료정보는 보여 줄 수 있다. 화면 종류를 하나 더 만들지 않아도 된다.
- 주행으로 바뀔 때:
  - 증상을 고르는 중 → 주행 화면 ("주행 중"). 고르던 선택은 세션에 남는다. 다시 정차하면 고르던 화면 그대로다.
  - 카드를 보는 중 → 주행 화면 (안내 중이면 "병원까지 N분"). 다시 정차하면 같은 카드다.
  - 화면이 5초 넘게 가려졌다 돌아오면 지금처럼 새 주행 상태를 받기 전까지 주행 화면이다 (`HomeViewModel` 규칙 그대로).
- 버튼 동작:
  - [진료 카드 만들기] → SYMPTOMS. [카드 만들기] → CARD. [취소] → 고르기 전 상태 (없었으면 HOME, 카드가 있었으면 그 카드).
  - [수정] → SYMPTOMS (지금 고른 증상이 선택된 채로).
  - [길 안내 시작] → 지금의 `GuidanceViewModel.onStartGuidance()` 그대로. 세션은 그대로 남는다.
  - [안내 취소] → `onCancelGuidance()`. 세션이 남으므로 "진료 카드 생성 완료" 카드로 돌아간다 (다시 안내를 시작하거나 닫을 수 있다).
  - 안내 없음 카드의 [닫기] → 세션을 비운다 → HOME. **컨셉 그림에는 없는 버튼이다** (`[사용자 승인 필요]` 2).
  - 도착 카드의 [닫기] → `onDismissArrival()` 다음에 세션을 비운다 → HOME.
    - 순서 이유: 두 상태가 다른 프레임에 반영되면 중간 화면이 한 프레임 보일 수 있다. 이 순서면 중간 화면이 "생성 완료" 카드라서 도착 카드와 몸통이 같다. 그래서 거의 보이지 않는다. 단계 8에서 연속 캡처로 본다.
  - 시스템 뒤로(`BackHandler`): SYMPTOMS → [취소]와 같다. 안내 없음 CARD → SYMPTOMS([수정]과 같다). HOME과 안내 중·도착 CARD에서는 가로채지 않는다.
- **정차 첫 화면(HOME)에서 바로 가는 [병원으로 안내]는 없앤다.** 사용자가 정한 흐름(정차 → [진료 카드 만들기] → 증상 → 카드 → [길 안내 시작])대로 안내는 카드에서만 시작한다 (`[사용자 승인 필요]` 2).
- 쓰이지 않게 되는 것: `ParkedGuidanceSection`(정차 화면 길안내 영역), `ArrivedScreen`(도착 자리표시), 정차 화면 메모 칸과 그 문자열. 단계 5-5에서 지운다. `SimulatedGuidanceNotice`는 카드 화면이 쓴다.

### 2. 진료 카드 세션은 `AppContainer`가 가진 메모리 객체에 둔다

```kotlin
// domain/card/MedicalCardSession.kt (Android를 모른다)
sealed interface CardSessionState {
    data object None : CardSessionState
    /** 고르는 중. [previous]는 고르기 전의 카드 증상 (없었으면 null). [취소]하면 여기로 돌아간다. */
    data class Picking(val draft: Set<Symptom>, val previous: Set<Symptom>?) : CardSessionState
    data class Ready(val symptoms: Set<Symptom>) : CardSessionState
}

class MedicalCardSession {
    val state: StateFlow<CardSessionState>
    fun startPicking()   // None → Picking(∅, null), Ready(s) → Picking(s, s), Picking → 그대로
    fun toggle(symptom: Symptom)  // Picking일 때만
    fun confirm()        // Picking(d, _) → Ready(d). 증상 0개도 된다
    fun cancelPicking()  // Picking(_, null) → None, Picking(_, p) → Ready(p)
    fun clear()          // → None
}
```

- `AppContainer`가 하나 만들어 들고 있다 (프로세스 수명). `MedicalCardViewModel`이 받아 쓴다.
- 이유:
  - 같이 움직여야 하는 Fake 길안내 상태(`FakeGuidanceSource`)도 `AppContainer`(프로세스 수명)에 있다. 수명을 맞춰야 "안내 중인데 증상이 사라진" 상태가 생기지 않는다.
  - ViewModel에만 두면 사용자가 뒤로 가기로 Activity를 닫았다 다시 열 때 사라진다. 그런데 Fake 길안내는 프로세스에서 계속 돈다.
  - 순수 Kotlin이라 JVM 테스트가 쉽다.
- 버린 대안:
  - ViewModel 범위: 위 이유.
  - `SavedStateHandle`: 프로세스가 죽어도 남지만, 그때 Fake 길안내는 이미 사라져 있어 맞지 않는다.
  - 디스크(Room·DataStore): 반려견 증상도 민감정보처럼 다루므로 암호화가 필요하다 (Pleos 보안 가이드). 하루 범위를 넘는다.
- 알려진 한계: 프로세스가 죽으면 고른 증상이 사라진다. Real 길안내(내비 앱은 우리 프로세스와 따로 산다)가 생기면 다시 정한다. 그때도 설계 결정 1에 따라 카드는 "고른 증상 없음"으로 보인다.
- 고르는 중의 선택(`draft`)도 세션에 둔다. 고르다가 주행이 시작돼도 정차하면 이어서 고를 수 있다.

### 3. 반려견 프로필: 도메인 모델, 출처 인터페이스, Fake("보리")

```kotlin
// domain/pet/PetProfile.kt (docs/data/pet-profile.md "Kotlin 대응")
data class PetProfile(
    val name: String,
    val breed: String?,
    val sex: Sex?,
    val birthDate: LocalDate?,
    val birthDateIsEstimated: Boolean,
    /** 사진 자리. 지금 차 앱은 그리지 않는다 (이름 첫 글자 아바타). */
    val photoDataUrl: String?,
    val medical: MedicalInfo,
    val updatedAt: Instant,
)
enum class Sex { MALE, FEMALE }
data class MedicalInfo(val conditions: List<String>, val allergies: List<String>, val medications: List<Medication>)
data class Medication(val name: String, val dosage: String)

// domain/pet/PetProfileSource.kt
interface PetProfileSource {
    /** 구독하면 현재 값을 먼저 내보내고, 바뀔 때마다 새 값을 내보낸다. */
    val profile: Flow<PetProfileLoad>
}
sealed interface PetProfileLoad {
    data class Loaded(val profile: PetProfile) : PetProfileLoad
    /** 보호자가 아직 등록하지 않았다. */
    data object NotRegistered : PetProfileLoad
    /** 읽지 못했다 (나중: 네트워크, 모르는 버전, 손상). 짐작해서 보여 주지 않는다. */
    data object Unavailable : PetProfileLoad
}
```

- `schemaVersion`은 도메인 모델에 두지 않는다. JSON을 읽는 쪽(나중 서버 구현)이 1인지 확인하고 아니면 `Unavailable`을 낸다 (pet-profile.md "읽는 쪽 규칙"). 화면은 버전을 알 필요가 없다. pet-profile.md "Kotlin 대응" 표에 이렇게 적는다.
- `java.time`(`LocalDate`, `Instant`)은 API 26부터 있다. minSdk 28이라 desugaring이 필요 없다.
- `Flow`로 둔 이유: 다른 출처(`DrivingStateSource`, `GuidanceSource`)와 모양을 맞춘다. 나중 서버(Firestore)는 보호자가 웹에서 고치면 바뀐 값을 밀어 줄 수 있다.
- Fake (`data/pet/`):
  - `SamplePetProfiles.bori`: `docs/data/pet-profile.example.json`과 같은 값 (보리, 골든리트리버, 수컷, 2014-08-01, 추정 아님, 사진 없음, 심장질환 / 특정 단백질 알레르기 / 심장약·1일 1회, 2026-11-05T00:00:00Z). KDoc에 예시 JSON 경로를 적는다.
  - `FakePetProfileSource(initial = Loaded(bori))`: `MutableStateFlow`. 테스트와 debug(있으면 좋음 B)용 `set(value)`.
- **모든 데이터 모드에서 Fake를 쓴다.** `SourcePlan`에 칸을 더하지 않는다.
  - 이유: Real 구현(서버)이 없다. 자리표시 Real을 만들면 REAL 모드에서 "등록 안 됨"만 보이고 얻는 것이 없다. 시연용 병원(`SampleHospitals`)도 모든 모드에서 같은 방식이다.
  - 그래서 `SourcePlanTest`·`SourcePlanBoundaryTest`를 고치지 않는다.
  - 서버 구현이 생기면 `SourcePlan`에 칸을 더한다. architecture.md "데이터 모드"에 적는다.
- `AppContainer`: `val petProfileSource: PetProfileSource = FakePetProfileSource()`, `val isPetProfileSample: Boolean = true` (Fake뿐이라서).

### 4. 나이 계산과 표시 (웹과 같은 규칙)

```kotlin
// domain/pet/PetAge.kt
/** 생년월일부터 오늘까지 개월 수. 생년월일이 오늘보다 뒤면 null. */
fun ageInMonths(birth: LocalDate, today: LocalDate): Int?
sealed interface AgeLabel {
    data class YearsAndMonths(val years: Int, val months: Int) : AgeLabel  // 12세 3개월
    data class Years(val years: Int) : AgeLabel                           // 12세
    data class Months(val months: Int) : AgeLabel                         // 3개월
    data object UnderOneMonth : AgeLabel                                  // 1개월 미만
}
fun ageLabelOf(months: Int): AgeLabel
/** 생년월일이 없거나 오늘보다 뒤면 null (나이를 보이지 않는다). */
fun ageLabelOf(birthDate: LocalDate?, today: LocalDate): AgeLabel?
```

- 규칙은 pet-profile.md "나이 계산" 그대로다: 개월 수 = (해 차이 × 12) + (달 차이), 오늘의 일 < 태어난 일이면 1을 뺀다. 세 = 몫, 남은 개월 = 나머지.
- 글자("%1$d세 %2$d개월" 등)는 `strings.xml`에 두고 composable이 붙인다. 지금 앱의 방식(`remainingTimeText`)과 같다. 분류(네 가지)는 순수 함수라 JVM으로 시험한다.
- 테스트 값은 `web/src/domain/age.test.ts`의 예와 같은 값을 쓴다. 꼭 넣을 것: `2014-08-01` / `2026-11-05` → 147개월 → `YearsAndMonths(12, 3)`.
- 카드의 나이는 이 규칙대로 "12세 3개월"이다. 컨셉 그림은 "12세"지만, 웹과 같은 규칙을 쓰기로 한 문서를 따른다.

### 5. 증상 프리셋과 순서 (`[사용자 승인 필요]` 1)

```kotlin
// domain/card/Symptom.kt. 선언 순서가 화면 순서이자 카드에 적히는 순서다.
enum class Symptom {
    BREATHING_DIFFICULTY, SEIZURE, COLLAPSE, BLEEDING_OR_WOUND, SWALLOWED_OBJECT,
    VOMITING, DIARRHEA, COUGH, LETHARGY, NO_APPETITE, LIMPING, BLOATED_BELLY,
}
```

| 순서 | enum | 화면 글자 (기본안) |
|---|---|---|
| 1 | `BREATHING_DIFFICULTY` | 호흡 곤란 |
| 2 | `SEIZURE` | 경련 |
| 3 | `COLLAPSE` | 쓰러짐 |
| 4 | `BLEEDING_OR_WOUND` | 출혈·상처 |
| 5 | `SWALLOWED_OBJECT` | 이물 삼킴 |
| 6 | `VOMITING` | 구토 |
| 7 | `DIARRHEA` | 설사 |
| 8 | `COUGH` | 기침 |
| 9 | `LETHARGY` | 기운 없음 |
| 10 | `NO_APPETITE` | 식욕 없음 |
| 11 | `LIMPING` | 절뚝거림 |
| 12 | `BLOATED_BELLY` | 배 부풂 |

- 12개: 가로 넓은 화면은 4열 × 3줄, 좁은 세로 화면은 2열 × 6줄로 맞는다.
- 순서: 숨·경련·쓰러짐·출혈·이물처럼 서둘러야 하는 것을 앞에 둔다. 운전자가 급할 때 먼저 보이게 하려는 것이다. 의학 검토를 거친 순서는 아니다. 바꾸려면 enum 순서와 문자열만 바꾸면 된다.
- **카드에는 누른 순서가 아니라 이 순서로 적는다.** 같은 선택이면 같은 카드가 나오고, 서둘러야 할 증상이 앞에 온다. 그래서 컨셉 그림의 "기침, 호흡 곤란"은 "호흡 곤란, 기침"으로 보인다.
- 화면에 "위급" 같은 판단 문구를 붙이지 않는다. 진단이 아니다.
- "기타"는 두지 않는다. 글자 입력이 없어서 카드에 담을 내용이 없다.
- 0개를 골라도 카드를 만들 수 있다. 증상 줄에 "고른 증상 없음"이 보인다. 급할 때 고르기를 건너뛰고 의료정보만 보여 줄 수 있게 하려는 것이다.
- 화면 글자는 `strings.xml`(`symptom_*`)에 둔다. 도메인 enum에는 글자가 없다.

### 6. 카드 내용을 만드는 순수 함수

```kotlin
// ui/card/MedicalCardUiState.kt
sealed interface PetHeader {
    data object Loading : PetHeader
    data object NotRegistered : PetHeader
    data object Unavailable : PetHeader
    data class Registered(
        val name: String,
        /** 사진이 없을 때 아바타 글자: 이름의 첫 코드 포인트 (웹 initialOf와 같다. 이모지를 반으로 자르지 않는다) */
        val initial: String,
        val sex: Sex?,
        val age: AgeLabel?,
        val ageIsEstimated: Boolean,
        val breed: String?,
    ) : PetHeader
}
data class MedicalRows(
    val conditions: List<String>,   // 빈 목록 = "등록된 정보 없음"
    val allergies: List<String>,
    val medications: List<String>,  // "심장약 (1일 1회)", 복용법이 비면 "심장약"
    val updatedOn: LocalDate,       // 정보 갱신일 (updatedAt을 기기 시간대 날짜로)
)
data class MedicalCardUiState(
    val pet: PetHeader,
    val symptoms: List<Symptom>,    // Symptom 선언 순서. 빈 목록 = "고른 증상 없음"
    val medical: MedicalRows?,      // pet이 Registered일 때만 있다
)

// ui/card/MedicalCardContent.kt
/** @param profile null이면 아직 받지 못했다 (Loading) */
fun medicalCardOf(profile: PetProfileLoad?, symptoms: Set<Symptom>, today: LocalDate, zone: ZoneId): MedicalCardUiState
```

- 표 줄 (사용자 결정): 주요 증상 / 기존 병력(= `medical.conditions`) / 알레르기 / 복용 약물.
- 이름 줄과 칩 (pet-profile.md "진료 카드" 칸을 따른다. 컨셉 그림보다 많다):
  - 이름 옆에 성별 기호 ♂/♀ (없으면 안 붙인다)
  - 칩: "나이 · 품종" (컨셉 그림 순서). 나이가 추정이면 "12세 3개월(추정)". 없는 값은 빼고, 둘 다 없으면 칩을 그리지 않는다.
  - 카드 아래 띠에 "정보 갱신일 2026년 11월 5일"
- 빈 값과 이상한 값:
  - 빈 목록 → 그 줄에 "등록된 정보 없음" (pet-profile.md: 빈 배열은 "없음"이 아니라 "등록하지 않음").
  - 앞뒤 공백을 지우면 빈 항목은 건너뛴다. 모두 비면 빈 목록과 같다. 내용을 짐작해 바꾸는 것이 아니라서 "읽는 쪽은 짐작하지 않는다" 규칙과 맞다.
  - 그 밖의 문자열은 그대로 쓴다 (길이 제한은 쓰는 쪽 책임, pet-profile.md).
- 긴 목록: **자르지 않는다.** 20개 × 50자도 모두 넣는다. 화면에서는 ", "로 이어 줄바꿈하고, 정차 화면이라 세로로 스크롤한다. 수의사에게 보여 줄 카드라서 빠지는 정보가 없는 편이 낫다.
- 프로필이 없을 때 (`Loading`, `NotRegistered`, `Unavailable`): 카드 머리에 기본 아바타와 안내 한 줄 ("반려견 정보를 불러오는 중이에요" / "등록된 반려견 정보가 없어요. 보호자 웹에서 등록할 수 있어요" / "반려견 정보를 불러오지 못했어요"). 의료정보 줄은 그리지 않는다. 증상 줄만 보인다. "등록된 정보 없음"이라고 쓰면 읽지 못한 것과 등록하지 않은 것이 섞이기 때문이다.
- 이 함수가 카드 "자동 생성"이다. AI 없이 프로필과 고른 증상을 조합한다. 진료 카드 생성 인터페이스(나중 Claude API 자리)는 이번에 만들지 않는다. 입력·출력 모양을 아직 모르기 때문이다 (005에서 병원 검색 인터페이스를 미룬 것과 같은 이유).
- 글자로 바꾸는 일(성별 기호, 나이, 증상 이름, "등록된 정보 없음", 날짜 형식)은 composable이 `strings.xml`로 한다.

### 7. 병원 전화

- 병원 모델을 길안내 목적지에서 분리한다. 전화번호는 길안내(NaviHelper `RouteInfo`)와 관계가 없기 때문이다.

  ```kotlin
  // domain/hospital/Hospital.kt
  data class Hospital(
      val destination: Destination,
      /** 없으면 [병원에 전화]를 숨긴다 (나중 병원 검색 결과에 번호가 없을 수 있다) */
      val phoneNumber: String?,
      /** 시연용 가상 병원이면 true. 화면에 "시연용"이라고 알린다 */
      val isSample: Boolean,
  ) { val name: String get() = destination.name }

  // domain/phone/PhoneDialer.kt
  fun interface PhoneDialer {
      /** 전화 앱에 [number]를 띄운다. 걸지는 않는다. 띄울 앱이 없거나 막혀 있으면 false. */
      fun openDialer(number: String): Boolean
  }
  ```

  - `SampleHospitals.demo`가 `Hospital`이 된다. `AppContainer.sampleHospital: Hospital`. `MainActivity`는 `GuidanceViewModel`에 `sampleHospital.destination`을 넘긴다. **`GuidanceViewModel`과 `Destination`은 바꾸지 않는다.** 그래서 기존 길안내 테스트가 그대로다.
- 시연용 번호 (기본안): `02-0000-0000`. 국번이 0으로 시작해 일반 가입 번호로 쓰이지 않는 형식으로 보인다. 확실하지 않으므로 사용자에게 확인한다 (`[사용자 승인 필요]` 3). `ACTION_DIAL`은 번호를 띄우기만 하고 걸지 않는다.
- 구현 (`data/phone/AndroidPhoneDialer`, application context):
  - `Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", number, null))` + `FLAG_ACTIVITY_NEW_TASK` → `startActivity`
  - `ActivityNotFoundException`, `SecurityException`을 잡아 false. 로그에 번호를 남기지 않는다.
  - **권한이 필요 없다.** `ACTION_DIAL`은 `CALL_PHONE`이 필요 없다. 새 `pleos.car.permission.*`도 없다.
  - **`<queries>`를 넣지 않는다.** 패키지 가시성 제한(Android 11+)은 다른 앱을 조회할 때(`resolveActivity`, `queryIntentActivities`)만 걸린다. 그래서 미리 조회하지 않고 바로 `startActivity`를 부르고 예외로 대비한다. 이 판단은 Android 일반 동작이고 Pleos 문서 내용이 아니다.
  - application context를 쓰는 이유: ViewModel이 Activity를 들고 있지 않게 하려는 것이다. 그래서 `NEW_TASK`가 필요하다.
  - Pleos 멀티 윈도우: 다른 앱을 띄우면 그 앱이 우리 앱을 덮고, 끝나면 돌아온다 (ux-guidelines "멀티 윈도우 정책"). 돌아오면 세션이 남아 있어 같은 카드다.
- 실패하면 (`false`) 카드 화면 안에 패널을 띄운다: "전화 앱을 열 수 없어요. 이 번호로 전화해 주세요" + 번호(아주 큰 글씨) + "시연용 번호"(시연용 병원일 때) + [확인]. 팝업 창(Dialog)이 아니라 화면 안 영역이라 IME·inset 문제가 없다. 이 상태는 ViewModel에만 두고 세션에는 두지 않는다.
- 모든 데이터 모드에서 `AndroidPhoneDialer`를 쓴다. 걸리지 않는 시연용 번호이고 걸지도 않으므로 안전하다.
- 단계 5-4까지는 `AppContainer`에 임시로 `PhoneDialer { false }`를 둔다 (항상 번호 패널). Fake로 먼저 화면을 확인하고 단계 6에서 진짜로 바꾼다.
- 차에서 되는지는 미확인이다 (문의 B4). Pleos 에뮬레이터 결과는 관찰로만 sdk-notes에 적는다 (확인 절차 D3).

### 8. 시연용 표시 (출품 유의사항: 사실과 다르면 선정 취소)

| 무엇이 시연용인가 | 표시 조건 | 어디에 (작은 글씨 `labelMedium`, `onSurfaceVariant`, 움직임·터치 없음) |
|---|---|---|
| 반려견 정보 (Fake "보리") | `AppContainer.isPetProfileSample` (지금 항상 true) | HOME의 보리 요약 아래, 카드 아래 띠: "시연용 샘플 정보예요. 보호자 웹과는 아직 연결되지 않았어요." |
| 병원과 전화번호 | `Hospital.isSample` | 카드 화면 버튼 아래: "병원과 전화번호(02-0000-0000)는 시연용이에요. 실제로 걸리지 않아요." 번호 패널에도 "시연용 번호" |
| 길안내 | `SourcePlan.isGuidanceSimulated` (지금 그대로) | 카드 화면 버튼 아래: 기존 "시연용 길안내예요. 실제 내비게이션과는 아직 연결되지 않았어요." |

- 주행 화면에는 넣지 않는다 (규칙 3, 007과 같다).
- 값은 `MainActivity`가 `AppContainer`에서 읽어 `HomeScreen`에 넘긴다. `ui/`는 `di`·`data`를 import하지 않는다.
- 시연 모드의 [길 안내 시작]은 기존 [병원으로 안내]와 같은 `onStartGuidance()`를 부른다. 그래서 Fake 시나리오(`FakeTripScenario`: 3초 뒤 출발, 도착 5초 뒤 주차)와 시연 모드의 "주행 중에만 시간이 흐름"이 그대로 이어진다. `data/demo/`, `data/guidance/`를 고치지 않는다 (완료 기준 3).
- 카드의 면책 문구는 컨셉 그림 그대로: "※ 본 정보는 사용자가 등록한 의료정보를 기반으로 하며, 의료 진단이 아닙니다."

### 9. 화면 배치 (가로 전체와 반쪽 세로 창 둘 다)

- 폭으로 나눈다 (`BoxWithConstraints`). 기본 기준은 840dp다. Pleos 반쪽 창의 실제 폭을 단계 8에서 재고 필요하면 바꾼다.
  - 넓음 (Pleos 가로 전체 약 1682dp, 폰 가로): 컨셉 그림 배치. 카드 왼쪽에 큰 아바타(둥근 사각, 약 200~240dp), 오른쪽에 이름(크게)·칩·표. 아래 버튼 두 개 나란히.
  - 좁음 (반쪽 세로 창, 폰 세로): 카드 위쪽에 작은 아바타(약 96dp) + 이름·칩을 한 줄로, 그 아래 표. 버튼은 폭 480dp 이상이면 나란히, 아니면 위아래로.
- 스크롤: 머리(제목 띠)와 카드는 같이 세로로 스크롤한다. 아래 버튼 줄은 스크롤 밖에 고정한다. 긴 목록이어도 [병원에 전화]·[길 안내 시작]이 밀려나지 않게 하려는 것이다.
  - 알려진 한계: 폰 가로(높이 약 411dp) + debug 패널이면 스크롤 영역이 좁다 (005와 같은 한계). release에는 패널이 없다.
- 증상 고르기: 열 수 = `(폭 / 200dp)`를 2~4로 자른 값. 줄마다 `Row` + `weight(1f)` (지연 목록·실험 API를 쓰지 않는다. 스크롤 안에 지연 그리드를 넣을 수 없다). 아래 [취소] [카드 만들기] 고정.
- 터치 크기: 모든 버튼은 `Dimens.MinTouchTarget`(64dp) 이상. 증상 버튼은 80dp 정도. [진료 카드 만들기]는 `Dimens.DrivingPrimaryButtonHeight`(96dp)를 써도 된다. [수정]·[닫기] 알약 버튼도 64dp 높이다 (컨셉보다 크다).
- 선택 상태를 색만으로 나타내지 않는다: 체크 표시를 같이 그리고 `Modifier.toggleable(role = Role.Checkbox)`로 접근성 상태를 준다. UI 트리에서 `checked="true"`로 확인할 수 있다.
- 아이콘:
  - 발바닥: 웹 `PawIcon`의 경로를 `ImageVector`로 옮겨 직접 그린다 (팀이 그린 것이라 라이선스 문제가 없다).
  - 전화·안내·정보·수정 아이콘: `androidx.compose.material.icons.Icons`의 기본(core) 아이콘을 쓴다. 지금 의존성(material3, BOM 2024.04.01)으로 import되는지 단계 5-2에서 확인한다. 안 되면 BOM 아래 `material-icons-core`를 그 커밋에 더하거나 직접 그린다. `material-icons-extended`는 쓰지 않는다 (용량).
- 정차 화면(HOME): "정차 중" 제목, 보리 요약 카드(아바타 + 이름 ♂ + 칩), "증상을 고르면 등록된 의료정보와 함께 진료 카드를 만들어요.", 시연용 샘플 안내, [진료 카드 만들기]. 메모 칸은 없다. 세로 스크롤.

### 10. 색 (시스템 라이트·다크를 따른다)

- 다크: 배경을 남색(`Navy900` 근처)으로 바꾼다. 컨셉 그림과 맞춘다.
- 카드 몸통은 **라이트·다크와 상관없이 흰 바탕 + 어두운 글자**로 고정한다. Pleos 디자인 가이드의 "Static Color"(라이트·다크에 영향을 받지 않는 색)와 같은 생각이다. 라이트에서는 흰 카드가 밝은 배경에 묻히지 않게 얇은 테두리를 둔다.
- 채움 버튼([길 안내 시작], [진료 카드 만들기], [카드 만들기])은 파랑 바탕 + 흰 글자. 글자 대비 4.5:1 이상인 값을 고르고 `Color.kt`에 계산한 대비를 주석으로 적는다 (ux-guidelines "색상과 테마").
- 테마 색을 바꾸면 다크에서 **주행 화면 배경도 남색이 된다.** `DrivingScreen.kt` 코드는 바꾸지 않는다 (완료 기준 3). 주행 화면 글자 대비만 단계 8에서 다크로 확인한다.

## 확인 필요 (시작 전에 해결)

### [사용자 승인 필요]

새 `pleos.car.permission.*`과 `CALL_PHONE`은 필요 없다. 아래는 기획 결정이다. 한 번에 묻는다. 권장안이 위 설계 결정에 반영돼 있다.

1. **증상 프리셋 12개와 순서** (단계 3 전에 필요): 설계 결정 5의 표. 서둘러야 하는 것을 앞에 두고, 카드에도 이 순서로 적는다 (누른 순서가 아님).
   - 권장: 표 그대로. 바꿀 항목이나 이름이 있으면 알려 달라.
2. **흐름의 빈칸 두 개** (단계 5 전에 필요):
   - 2a. 정차 첫 화면에서 바로 가는 [병원으로 안내]를 없애고, 안내는 카드의 [길 안내 시작]에서만 한다. (권장. 사용자가 정한 흐름의 해석)
   - 2b. 안내 전 카드에 [닫기]를 더한다 (컨셉 그림에 없음). 없으면 카드를 버리고 처음 화면으로 갈 방법이 안내 → 도착 → [닫기]뿐이다. (권장: 더한다, 머리 띠의 [수정] 옆)
3. **시연용 병원 번호와 문구** (단계 2 전에 필요):
   - 번호 `02-0000-0000` (기본안). 실제로 개통되지 않는 형식인지 팀이 아는 번호가 있으면 그것으로.
   - 문구 "병원과 전화번호(02-0000-0000)는 시연용이에요. 실제로 걸리지 않아요."

### planner가 정한 것 (바꾸려면 알려 달라. 묻지 않고 진행한다)

- 카드에 컨셉 그림보다 많은 것을 넣는다: 성별 ♂/♀, 나이 "12세 3개월"(그림은 "12세"), "(추정)", 정보 갱신일. pet-profile.md의 "진료 카드" 칸을 따른 것이다.
- 증상 0개로도 카드를 만들 수 있다.
- 다크 배경을 남색으로 바꾸면 주행 화면 배경도 남색이 된다 (코드는 그대로).
- 고른 증상은 앱 프로세스 메모리에만 둔다. 앱이 강제 종료되면 사라진다.

### [선행: pleos-docs-researcher]

- **없다.** 새로 쓰는 Pleos API가 없다. `ACTION_DIAL`은 Android 표준 Intent다. Pleos 문서에 허용·금지 문장이 없다는 사실은 이미 sdk-notes "전화"에 있고, 이번에는 에뮬레이터 관찰만 더한다.

### 실행해서 확인할 것 (문서에 없음)

- Pleos 에뮬레이터에서 `ACTION_DIAL`을 받는 앱이 있는지, 열리면 무엇이 뜨는지 (D3). 결과를 sdk-notes "전화"에 관찰로 적는다. 실차 결과는 아니다 (문의 B4 그대로).
- Pleos 에뮬레이터에서 우리 앱을 내비 옆 반쪽 창으로 띄울 수 있는지(window bar, 3손가락 스와이프), 그때 창 크기 (D7). 안 되면 폰 세로와 미리보기(830×1210dp)로 대신하고 "확인 못 함"으로 적는다.
- Pleos 에뮬레이터에서 `adb shell cmd uimode night yes|no`로 라이트·다크가 바뀌는지.
- material3(BOM 2024.04.01)만으로 `Icons.Filled.Phone` 등이 import되는지 (단계 5-2).
- 도착 카드 [닫기] 때 중간 화면이 한 프레임 보이는지 (P11).

## 확인 절차

`/verify` 규칙대로 에이전트는 에뮬레이터를 직접 띄우지 않는다. 사용자에게 Cold boot를 요청한다. 기기가 둘이면 `adb -s`로 고른다.
스크린샷과 UI 트리는 `.claude/.cache/p09-<번호>.png`, `.claude/.cache/p09-<번호>.xml`에 둔다 (커밋하지 않는다).
터치 크기는 UI 트리 bounds(px)를 dp로 바꿔 본다: `adb shell wm density` 값으로 나누고 160을 곱한다 (Pleos 에뮬레이터는 160dpi라 px = dp).

### 일반 폰 에뮬레이터 `my_avd` (debug, Fake 모드)

| # | 조작 | 기대 |
|---|---|---|
| P1 | 시작 | HOME: "정차 중", 아바타 "보", "보리 ♂", "12세 3개월 · 골든리트리버", 시연용 샘플 안내, [진료 카드 만들기]. `EditText` 0개 |
| P2 | [진료 카드 만들기] | SYMPTOMS: 12개 버튼(2열), 선택 0개, [취소] [카드 만들기] |
| P3 | 기침, 호흡 곤란 누름 | 두 버튼 `checked="true"`, 체크 표시 |
| P4 | [카드 만들기] | CARD "진료 카드 생성 완료" + "보리의 정보를 바탕으로…" + [수정] [닫기]. 주요 증상 "호흡 곤란, 기침" / 기존 병력 "심장질환" / 알레르기 "특정 단백질 알레르기" / 복용 약물 "심장약 (1일 1회)". 면책 문구, 정보 갱신일, 시연 안내 3가지. [병원에 전화] [길 안내 시작] |
| P5 | [수정] → 구토 추가 → [카드 만들기] | 주요 증상 "호흡 곤란, 구토, 기침" |
| P6 | [병원에 전화] | 전화 앱이 `02-0000-0000`으로 열린다 (걸리지 않음). 뒤로 → 같은 카드 |
| P7 | 전화 앱 끄기 → [병원에 전화] → [확인] → 전화 앱 켜기 | 번호 패널: 큰 번호 + "시연용 번호" + [확인]. 끄기·켜기: `adb shell cmd package resolve-activity --brief -a android.intent.action.DIAL -d tel:0200000000`로 패키지를 찾고 `pm disable-user --user 0 <패키지>` / `pm enable <패키지>`. 끄기가 안 되면 단계 5-4(임시 `PhoneDialer { false }`)의 캡처로 대신한다 |
| P8 | [길 안내 시작] | CARD "경로를 찾는 중" → "병원까지 12분" + 병원 이름 + [병원에 전화] [안내 취소]. 약 3초 뒤 주행 화면 (005 그대로, debug 패널 밖 클릭 가능 노드 0개, `EditText` 0개) |
| P9 | 주행 중 debug [정차] → [주행] | 안내 중 카드(같은 증상) → 주행 화면 |
| P10 | 기다림 | 주행 "도착했어요" → 5초 뒤 CARD "도착했어요" + 같은 증상 + [병원에 전화] [닫기] |
| P11 | [닫기] (화면 녹화 또는 연속 캡처) | HOME (P1). 중간 화면이 눈에 띄게 보이지 않는다 |
| P12 | P2에서 하나 고른 채 debug [주행] → [모름] → [정차] | 주행 화면 두 번(증상 버튼 없음) → 고르던 화면, 선택 그대로 |
| P13 | 안내 없는 카드에서 [닫기], 고르는 중 [취소], 뒤로 키 | 설계 결정 1의 버튼 동작대로 |
| P14 | 세로·가로 × 라이트·다크로 HOME, SYMPTOMS, CARD(생성 완료·안내 중·도착), 번호 패널 | 글자 잘림·겹침 없음. 모든 버튼 높이 64dp 이상. 다크 배경 남색, 카드 흰색. 주행 화면 글자가 다크에서 읽힌다. 다 보면 설정을 되돌린다 |

### Pleos 에뮬레이터 `pleos_connect` (Cold boot, debug 시연 모드 "DEBUG · 시연 (주행 Real)")

Virtual Console 명령은 006 노트의 `vc()` 함수를 쓴다.

| # | 조작 | 기대 |
|---|---|---|
| D1 | `vc "/vehicle/gear?gear=Park"`, 앱 실행 | HOME (넓은 배치, 큰 화면에서 내용이 왼쪽 위에 몰리지 않는다) |
| D2 | [진료 카드 만들기] → 두 개 고름 → [카드 만들기] | SYMPTOMS 4열 → CARD 넓은 배치 (왼쪽 아바타, 오른쪽 이름·칩·표, 버튼 나란히) |
| D3 | [병원에 전화] | **관찰**: 전화 앱이 열리는지, 무엇이 뜨는지. 안 열리면 번호 패널. 스크린샷과 `adb logcat -d -t 200`을 진행 로그에 남기고 sdk-notes에 적는다. 열렸으면 돌아와서 같은 카드인지 |
| D4 | [수정] 중 `gear=Drive` → `gear=Park` | 주행 화면 → 고르던 화면, 선택 그대로 |
| D5 | [카드 만들기] → [길 안내 시작] → `gear=Drive` | 안내 중 카드 → 주행 화면 "병원까지 N분" (007 그대로) |
| D6 | 도착까지 기다림 → `gear=Park` | 주행 "도착했어요" → 도착 카드, 같은 증상 |
| D7 | 반쪽 창 시도 (내비와 나란히: window bar 또는 3손가락 스와이프) | 되면: 좁은 배치 스크린샷, `adb shell dumpsys window`로 창 크기를 진행 로그에 적는다. 안 되면 "확인 못 함" |
| D8 | [닫기] | HOME |
| D9 | 로그 | `adb logcat -d -t 300 *:E`에 크래시·`SecurityException` 없음 |

- 시간이 되면 007 방식(debug 키로 서명한 확인용 사본, `.claude/.cache/`, 커밋 안 함)으로 release를 설치해 D1~D8을 한 번 더 본다. 하지 않으면 하지 않았다고 적는다.

## 단계

단계 하나 = 커밋 1~3개. 커밋마다 `assembleDebug`가 통과해야 한다 (precommit 훅). 테스트는 대상 코드와 같은 커밋에 넣는다.

**시작 전 준비:**
- 위 `[사용자 승인 필요]` 1~3을 사용자에게 한 번에 확인받고 답을 진행 로그에 적는다.
- 미추적 폴더 `.agents/`, `.codex/`가 있으면 스테이징하지 않는다. 파일 이름으로만 `git add`한다.
- 사용자에게 알린다: 단계 5 이후 화면 확인과 단계 8에서 `my_avd`, `pleos_connect`를 Cold boot로 켜 달라고 요청한다.

**순서 메모:** 인터페이스·Fake(1·2) → 순수 로직·ViewModel(3·4) → 화면(5, Fake로 동작) → 플랫폼 래퍼(6, 전화 앱) → tester(7) → `/verify`(8) → 문서(9) → 노트(10). 새 Pleos SDK 래퍼는 없다.

### 필수

- [x] 0. 이 계획 파일 커밋 → `docs(plans): 009 진료 카드 계획 추가`

- [x] 1. 반려견 프로필 모델과 출처 (인터페이스와 Fake)
  - 1-1. → `feat(domain): 반려견 프로필 모델과 나이 계산 추가`
    - `domain/pet/PetProfile.kt` (설계 결정 3), `domain/pet/PetAge.kt` (설계 결정 4)
    - `PetAgeTest`:
      - `` `예시 생년월일은 147개월이라 12세 3개월이다`() ``
      - `` `오늘의 일이 태어난 일보다 작으면 한 달을 뺀다`() ``
      - `` `생년월일이 오늘보다 뒤면 나이를 보이지 않는다`() ``
      - `` `12개월은 1세, 11개월은 11개월, 0개월은 1개월 미만이다`() ``
      - `` `2월 29일생은 평년 2월 28일에 아직 한 달이 차지 않는다`() `` (web `age.test.ts`와 같은 결과인지 확인하고 쓴다)
  - 1-2. → `feat(data): 반려견 프로필 출처 인터페이스와 Fake(보리) 추가`
    - `domain/pet/PetProfileSource.kt` (`PetProfileSource`, `PetProfileLoad`)
    - `data/pet/SamplePetProfiles.kt`, `data/pet/FakePetProfileSource.kt`
    - `FakePetProfileSourceTest`:
      - `` `처음 구독하면 보리 프로필을 받는다`() ``
      - `` `보리는 예시 JSON과 같은 값이다`() `` (값을 테스트에 직접 적는다. 파일 대조는 있으면 좋음 A)
      - `` `값을 바꾸면 새 값을 받고 같은 값은 다시 보내지 않는다`() ``
  - 확인: `testDebugUnitTest` 통과 (185 + 새 테스트)

- [x] 2. 병원 전화번호 (`[사용자 승인 필요]` 3의 답대로)
  - 2-1. → `refactor(domain): 병원 정보를 길안내 목적지에서 분리`
    - `domain/hospital/Hospital.kt` (`phoneNumber`는 이 커밋에서 `null`), `SampleHospitals.demo: Hospital(isSample = true)`
    - `AppContainer.sampleHospital: Hospital`, `MainActivity`는 `sampleHospital.destination`을 넘긴다
    - 동작 변화 없음. 기존 테스트 그대로 통과.
  - 2-2. → `feat(domain): 시연용 병원 번호와 전화 앱 열기 인터페이스 추가`
    - `SampleHospitals.demo.phoneNumber = "02-0000-0000"` (KDoc: 시연용, 걸리지 않는 형식, 문의 B4)
    - `domain/phone/PhoneDialer.kt`

- [x] 3. 진료 카드 순수 로직
  - 3-1. → `feat(domain): 증상 프리셋과 진료 카드 세션 추가`
    - `domain/card/Symptom.kt` (`[사용자 승인 필요]` 1의 답), `domain/card/MedicalCardSession.kt` (설계 결정 2)
    - `MedicalCardSessionTest`:
      - `` `처음에는 카드가 없다`() ``
      - `` `카드 없이 고르기를 시작하면 빈 선택이고 취소하면 카드 없음으로 돌아간다`() ``
      - `` `카드가 있을 때 수정하면 지금 증상이 선택된 채로 고르고 취소하면 그 카드로 돌아간다`() ``
      - `` `같은 증상을 두 번 누르면 선택이 풀린다`() ``
      - `` `고르는 중이 아니면 증상 누르기와 만들기는 아무것도 하지 않는다`() ``
      - `` `증상 0개로도 카드를 만들 수 있다`() ``
      - `` `비우면 어느 상태에서든 카드 없음이다`() ``
  - 3-2. → `feat(ui): 진료 카드 내용 조합 함수 추가`
    - `ui/card/MedicalCardUiState.kt`, `ui/card/MedicalCardContent.kt` (`medicalCardOf`, 설계 결정 6)
    - `MedicalCardContentTest`:
      - `` `보리와 기침, 호흡 곤란으로 카드를 만들면 컨셉 그림의 줄이 모두 있다`() ``
      - `` `증상은 누른 순서가 아니라 프리셋 순서다`() ``
      - `` `빈 의료정보 목록은 빈 줄이고 공백뿐인 항목은 건너뛴다`() ``
      - `` `복용법이 없는 약은 이름만 쓴다`() ``
      - `` `20개의 긴 항목도 자르지 않고 모두 넣는다`() ``
      - `` `프로필을 받지 못했거나 등록되지 않았거나 읽지 못했으면 의료정보 줄이 없다`() ``
      - `` `품종과 생년월일이 없으면 칩 값이 없고 미래 생년월일은 나이를 보이지 않는다`() ``
      - `` `아바타 글자는 이름의 첫 코드 포인트다`() `` ("보리" → "보", "😀보리" → "😀")
      - `` `정보 갱신일은 기기 시간대의 날짜다`() `` (`2026-11-04T15:30:00Z`, `Asia/Seoul` → 2026-11-05)
  - 3-3. → `feat(ui): 정차 화면 안에서 보일 화면을 고르는 규칙 추가`
    - `ui/home/ParkedContent.kt` (`parkedContentOf`, 설계 결정 1의 2단계 표)
    - `ParkedContentTest`:
      - `` `안내도 카드도 없으면 처음 화면이다`() ``
      - `` `고르는 중이면 안내 상태와 상관없이 증상 고르기다`() ``
      - `` `카드가 있거나 안내 중이거나 도착했으면 카드다`() ``

- [x] 4. 진료 카드 ViewModel → `feat(ui): 진료 카드 ViewModel 추가`
  - `ui/card/MedicalCardViewModel.kt`:
    - 생성자: `PetProfileSource`, `MedicalCardSession`, `Hospital`, `PhoneDialer`, `clock: Clock = Clock.systemDefaultZone()`
    - `uiState: StateFlow<CardFlowUiState>` = 프로필 + 세션 + 번호 패널을 합친다. `stateIn(WhileSubscribed(5초))`, 처음 값은 프로필 `null`(Loading) + 지금 세션.
    - `CardFlowUiState(session, card: MedicalCardUiState, hospitalName, hospitalPhone: String?, isHospitalSample, shownPhoneNumber: String?)`
    - 동작: `onStartCard`, `onToggleSymptom`, `onConfirmSymptoms`, `onCancelPicking`, `onEditSymptoms`, `onCloseCard`(세션 비움), `onCallHospital`, `onDismissPhoneNumber`
    - 주행 상태와 길안내는 모른다 (`HomeViewModel`, `GuidanceViewModel`과 같은 나눔).
  - `MedicalCardViewModelTest` (`MainDispatcherRule`, Turbine 또는 `backgroundScope` 구독, `Clock.fixed(2026-11-05, Asia/Seoul)`):
    - `` `처음에는 보리 요약을 보여 주고 카드가 없다`() ``
    - `` `증상을 골라 만들면 카드에 고른 증상이 들어간다`() ``
    - `` `전화 앱이 열리면 번호 패널을 띄우지 않는다`() ``
    - `` `전화 앱을 열지 못하면 번호를 크게 보여 주고 확인하면 닫는다`() ``
    - `` `병원 번호가 없으면 전화 버튼 값이 없고 전화 앱을 부르지 않는다`() ``
    - `` `프로필이 바뀌면 카드도 바뀐다`() ``
    - `` `카드를 닫으면 세션이 비고 처음 화면이다`() ``

- [x] 5. 화면 (Fake로 동작)
  - 5-1. → `feat(ui): 다크 테마를 남색 배경으로 변경` (설계 결정 10)
    - `ui/theme/Color.kt`, `Theme.kt`: 다크 배경·표면 남색, 채움 버튼 파랑. 카드 고정색(흰 바탕, 글자, 칩, 띠)은 `ui/card/CardColors.kt` 또는 `Color.kt`. 대비를 주석으로 적는다.
    - 확인: 기기가 있으면 다크로 주행 화면·정차 화면만 본다.
  - 5-2. → `feat(ui): 진료 카드 화면 추가` (아직 연결하지 않는다. 미리보기로 확인)
    - `ui/card/PawIcon.kt`(웹 경로를 `ImageVector`로), `ui/card/PetAvatar.kt`(첫 글자, KDoc에 사진 자리), `ui/card/MedicalCardScreen.kt`(머리 띠·카드 몸통·면책 띠·버튼 줄·번호 패널, 넓음/좁음, 안내 없음·안내 중·도착 세 가지 머리와 버튼)
    - `strings.xml`: 카드 문자열 (줄 이름, "등록된 정보 없음", "고른 증상 없음", 나이 네 가지, "(추정)", 정보 갱신일, 면책, 시연 안내, 번호 패널, 버튼 글자 "길 안내 시작"·"병원에 전화"·"수정")
    - 미리보기: 넓음 1682×1210, 좁음 830×1210, 폰 411×891, 긴 목록, 의료정보 비어 있음, 등록 안 됨, 다크
    - 커밋이 200줄을 크게 넘으면 "카드 몸통"과 "화면 틀(머리·버튼·번호 패널)"로 나눈다.
  - 5-3. → `feat(ui): 증상 고르기 화면 추가`
    - `ui/card/SymptomPickerScreen.kt` (설계 결정 9의 열 수, 체크 표시, `toggleable`), `strings.xml`의 `symptom_*`
    - 미리보기: 넓음, 좁음, 두 개 선택
  - 5-4. → `feat(ui): 정차 화면을 진료 카드 흐름으로 연결`
    - `ui/home/ParkedScreen.kt`: HOME (설계 결정 9). 메모 칸과 `ParkedGuidanceSection` 호출을 뺀다.
    - `ui/home/HomeScreen.kt`: `PARKED`·`ARRIVAL`이면 `parkedContentOf`로 HOME / SYMPTOMS / CARD. `BackHandler`. 인자가 많아지면 카드 동작을 `MedicalCardActions` 같은 묶음으로 넘긴다.
    - `di/AppContainer.kt`: `petProfileSource`, `isPetProfileSample`, `medicalCardSession`, 임시 `phoneDialer = PhoneDialer { false }`
    - `MainActivity.kt`: `MedicalCardViewModel` 만들기, 도착 [닫기] = `onDismissArrival()` 다음 `onCloseCard()`, 시연 표시 값 넘기기
    - `MedicalCardTripFlowTest` (`FakeTripScenarioScreenFlowTest`처럼 Fake 길안내 + Fake 주행 상태 + 시나리오 + 세 ViewModel + `homeContentOf`·`parkedContentOf`):
      - `` `카드를 만들고 안내를 시작하면 출발하고 도착 후 주차하면 같은 증상의 카드이고 닫으면 처음 화면이다`() ``
      - `` `증상을 고르는 중에 주행이나 모름이 되면 주행 화면이고 정차하면 고르던 선택 그대로다`() ``
      - `` `안내 중에 취소하면 생성 완료 카드로 돌아간다`() ``
    - 확인: `/verify`로 P1~P5, P7(임시 Fake라 번호 패널), P8~P13을 먼저 본다.
  - 5-5. → `refactor(ui): 쓰지 않는 정차 길안내 영역과 도착 자리표시 제거`
    - `ui/guidance/ParkedGuidanceSection.kt`의 `ParkedGuidanceSection`과 미리보기 (`SimulatedGuidanceNotice`는 남긴다), `ui/guidance/ArrivedScreen.kt`
    - 쓰이지 않는 문자열: `parked_example_input_label`, `arrival_card_title`, `arrival_card_placeholder`, `guidance_destination_label`, `guidance_start`
    - 확인: `assembleDebug`, `lintDebug`에 새 경고 없음

- [x] 6. 전화 앱 열기 래퍼 → `feat(data): Android 전화 앱 열기 래퍼 추가`
  - `data/phone/AndroidPhoneDialer.kt` (설계 결정 7), `AppContainer`의 임시 `PhoneDialer { false }`를 바꾼다.
  - JVM 테스트는 없다 (Android `Context`·`Intent`). ViewModel 테스트가 가짜 `PhoneDialer`로 두 경우를 다룬다.
  - 확인: 완료 기준 4의 매니페스트 Grep, P6

- [x] 7. 경계 테스트 (`tester`) → `test: 진료 카드 경계 테스트 추가`
  - 메인이 `tester`에게 **이 계획 파일 경로와 대상만** 넘긴다. 구현 설명은 넘기지 않는다.
    - 대상: `ageInMonths`·`ageLabelOf`, `MedicalCardSession`, `medicalCardOf`, `parkedContentOf`, `MedicalCardViewModel`, `FakePetProfileSource`, 그리고 `homeContentOf` + `parkedContentOf`를 합친 화면 조합
    - 대상 아님: `AndroidPhoneDialer`, composable, `AppContainer`, `MainActivity`
  - 기대 동작 (계약 기준): 설계 결정 1의 두 표 전체와 버튼 동작, 결정 2의 세션 전이, 결정 4의 나이 규칙(pet-profile.md), 결정 5의 순서, 결정 6의 빈 값·공백·긴 목록·프로필 없음, 결정 7의 번호 패널 규칙
  - 기존 테스트 파일은 고치지 않는다 (완료 기준 3).
  - tester가 찾은 버그는 메인이 프로덕션 코드에서 `fix:` 커밋으로 따로 고친다.
  - 확인: `.\gradlew.bat testDebugUnitTest` 통과. 통과 수를 진행 로그에 적는다.

- [x] 8. 전체 검증 (`/verify`). 커밋은 없다. 고친 게 있으면 `fix(...)` 커밋으로 낸다.
  - `node .claude/hooks/build-gate.mjs pipeline` → "검증 파이프라인 통과."
  - `.\gradlew.bat assembleRelease` 성공
  - 완료 기준 3의 `git diff`, 완료 기준 4의 Grep
  - `my_avd`: 확인 절차 P1~P14
  - `pleos_connect`: 확인 절차 D1~D9
  - 기기가 없으면 멈추고 사용자에게 Cold boot를 요청한다. 확인하지 못한 것은 그대로 적는다.

- [x] 9. 문서 맞추기
  - `docs/architecture.md`:
    - "서비스 기능 스택": "증상 → 진료 카드" 행에 "지금(009)은 규칙으로 조합, AI 없음. Claude API는 A1 뒤 팀 논의". "병원 전화" 행에 구현됨과 D3 관찰. "의료 프로필 저장" 행에 "지금은 기기에 저장하지 않는다 (Fake). 서버 연동 때 정한다".
    - "패키지 구조": `domain/pet/`, `domain/card/`, `domain/hospital/`, `domain/phone/`, `data/pet/`, `data/phone/`, `ui/card/`, `ui/home/ParkedContent.kt`. 지운 `ParkedGuidanceSection`, `ArrivedScreen` 줄을 고친다.
    - "데이터 모드": 반려견 프로필은 모든 모드에서 Fake(보리)이고 시연용 샘플이라고 알린다. 서버 구현이 생기면 `SourcePlan`에 칸을 더한다. 전화는 모든 모드에서 Android 전화 앱.
    - "주행 상태 × 길안내 상태 화면 조합": 설계 결정 1의 두 표, 버튼 동작, 주행으로 바뀔 때. "도착 화면: 진료 카드(자리표시)" 문구를 고친다.
    - 커밋: `docs(architecture): 진료 카드 화면 조합과 패키지 기록`
  - `docs/data/pet-profile.md`: "지금 쓰는 곳"에 차 앱(Fake), "Kotlin 대응"을 확정으로 (`schemaVersion`은 도메인 모델에 없음, `PetProfileLoad`), 진료 카드 표시 규칙(빈 목록 "등록된 정보 없음", 공백 항목 건너뜀, 자르지 않음, 증상 순서) → `docs(data): 반려견 프로필 Kotlin 대응 확정`
  - `README.md`: "실행 방법"의 시연용 안내에 반려견 정보·병원 번호가 시연용이라는 문장. 두 시연 방법을 새 흐름으로 ([진료 카드 만들기] → 증상 → [카드 만들기] → [길 안내 시작] → … → 도착 카드 → [닫기]). [병원에 전화]는 전화 앱을 열기만 한다는 것 → `docs: README에 진료 카드 시연 방법 추가`
  - `docs/pleos/sdk-notes.md` "전화": D3 결과를 "관찰 (2026-11-0X, Pleos 에뮬레이터 v2.1.0, 실차 아님)"으로. `docs/pleos/ux-guidelines.md`: D7에서 반쪽 창을 띄웠으면 방법과 창 크기를 관찰로 → `docs(pleos): Pleos 에뮬레이터 전화 앱·반쪽 창 관찰 기록`
  - 확인: 완료 기준 7

- [x] 10. 개발 노트 작성 (`docs/notes/`)
  - `docs/notes/xxj15/YYYY-MM-DD-medical-card.md`. 형식은 `docs/notes/README.md` 템플릿.
    - 사용한 API: 새 Pleos SDK 없음, 새 권한 없음. Android `ACTION_DIAL`(권한 없음).
    - 결정: 화면 조합 두 단계, 세션 위치, 프로필은 모든 모드 Fake, 증상 순서, 카드 규칙, 전화 대비책, 시연 표시, 색
    - 시연 방법 (영상 촬영용): release APK + Pleos 에뮬레이터 + Virtual Console 순서 (D1~D8), 일반 폰 에뮬레이터 순서
    - **기획서 5번 "개발 계획"에 넣을 문장** (기획 담당에게). 예: "차에서는 글자를 입력하지 않는다. 정차 중 큰 버튼으로 증상을 고르면, 보호자가 미리 등록한 의료정보(기존 병력·알레르기·복용 약물)와 합쳐 진료 카드를 규칙에 따라 만든다. 카드에서 병원 전화(전화 앱 열기)와 길안내를 시작하고, 도착해 정차하면 같은 카드를 수의사에게 보여 준다. 현재 반려견 정보는 시연용 샘플이며, 보호자 웹과의 연결과 AI 정리는 대회 규정 확인 뒤 진행한다."
    - 남은 일: 서버 프로필 구현(A1), AI 정리(A1·팀 논의), 실차 전화(B4), 프로세스 재시작 시 증상 복원, 사진 표시, 하지 않은 있으면 좋음
  - 이 계획을 `docs/plans/completed/`로 옮기고 진행 로그를 갱신한다.
  - 커밋:
    - `docs(notes): medical-card`
    - `docs(plans): 009 진료 카드 완료`
  - 확인: `git status`에 이 작업의 변경이 남지 않았다. 그다음 `/ship`.

### 있으면 좋음 (필수가 끝난 뒤, 단계 10 전에)

- [ ] A. 보리 샘플과 예시 JSON 파일 대조 → `test(data): 보리 샘플이 예시 JSON과 같은지 확인`
  - 웹처럼 `docs/data/pet-profile.example.json`을 읽어 `SamplePetProfiles.bori`와 비교한다. 문서와 코드가 어긋나면 테스트가 깨진다.
  - JVM 테스트에서는 Android `org.json`이 동작하지 않는다. `testImplementation`으로 JSON 라이브러리(예: `org.json:json`)를 더한다. 버전은 Maven Central에서 확인해 `libs.versions.toml`에 고정한다. 라이선스를 노트에 적는다 (문의 A4).
  - 파일 경로는 테스트 작업 폴더(`app/`) 기준 `../docs/data/pet-profile.example.json`.
- [ ] B. debug 패널에서 Fake 프로필 바꾸기 → `feat(ui): debug 패널에서 Fake 반려견 정보 바꾸기`
  - 보리 / 의료정보 없음 / 등록 안 됨 / 읽기 실패. 빈 값 화면을 기기에서 보고 영상에 쓸 수 있다.
  - 패널이 세 줄이 되면 가로 화면에서 더 밀린다 (005의 한계). 접기와 같이 생각한다.
- [ ] C. 넓은 화면에서 카드 글자 키우기 → `feat(ui): 넓은 화면에서 진료 카드 글자 키우기`
  - Pleos 화면에서 우리 글자가 시스템 UI보다 작아 보였다 (ux-guidelines 관찰 2026-11-05). 카드의 넓은 배치에서만 이름·표 글자를 한 단계 키운다. 전체 타입 스케일은 Pleos 수치를 확인한 뒤에 바꾼다.

## 진행 로그

- 2026-11-05: 계획 작성.
  - 브랜치 `feat/medical-card`는 dev(`cb37087`, 008 보호자 웹과 앱 이름 MeongCare 반영 뒤)에서 땄다.
  - 시작 시점의 코드:
    - 정차 화면(`ParkedScreen`): "정차 중" + 길안내 영역(시연용 동물병원 + [병원으로 안내] / 안내 중 남은 시간 + [안내 취소]) + 메모 입력칸.
    - 도착 화면(`ArrivedScreen`): "도착했어요" + 병원 이름 + "진료 카드는 준비 중이에요" + [닫기].
    - `Destination`(이름·좌표·주소)에 전화번호가 없다. `SampleHospitals.demo`는 `Destination`이다.
    - 차 앱에 반려견 프로필 코드가 없다.
    - 테마: 다크 배경 `Gray900`, 기본색 `Sky300`. 아이콘 의존성 없음 (material3만).
    - 단위 테스트 185개.
- 2026-11-05: `[사용자 승인 필요]` 답 (사용자):
  1. 증상 12개와 순서: 표 그대로. 카드에도 프리셋 순서로 적는다.
  2. 2a 정차 첫 화면의 [병원으로 안내]를 없애고 카드에서만 안내한다. 2b 안내 전 카드에 [닫기]를 더한다.
  3. 시연용 번호 `02-0000-0000`, 문구 그대로.
  - 두 에뮬레이터(`pleos_connect` = emulator-5554, `my_avd` = emulator-5556)는 이미 켜져 있다.
- 2026-11-05: 단계 0~6 완료 (`a345e75`..`4b4acd9`). 단위 테스트 226개 통과 (185 + 41). 완료 기준 4 Grep 통과.
  - Pleos 에뮬레이터(debug Fake, 라이트)에서 HOME → SYMPTOMS(4열) → CARD(넓은 배치)를 먼저 봤다. 컨셉 그림 배치대로 나온다.
- 2026-11-05: 단계 7~10 완료 (`9d5de18`..). 
  - tester 경계 테스트 80개. 버그 1건(프로필 첫 값이 늦는 출처에서 `combine`이 멈춤) → `fix(ui)` (`onStart { emit(null) }`). **단위 테스트 306개 통과.**
  - 검증 파이프라인 통과 (Android + 웹), `assembleRelease` 성공. 완료 기준 3의 `git diff` 비어 있음, 기존 테스트 파일 수정·삭제 없음. 완료 기준 4 Grep 통과.
  - Pleos 에뮬레이터 (D): D1 HOME, D2 SYMPTOMS 4열·CARD 넓은 배치, D3 `ACTION_DIAL` → "전화" 앱이 열리지만 번호는 안 보임 (sdk-notes 관찰), 돌아오면 같은 카드, D4 고르는 중 D → 주행 화면 → P → 선택 그대로, D5 [길 안내 시작] → D → 주행, D6 도착 → P → "도착했어요" 카드(같은 증상), D8 [닫기] → HOME, D9 에러 로그 없음. **D7(반쪽 창)은 확인하지 못했다.**
  - 일반 폰 (P): P1~P4, P6(구글 전화 앱에 `0200000000`이 채워져 열림), 다크 모드 좁은 배치(남색 배경, 흰 카드). P7(전화 앱 끄기), P11(닫기 연속 캡처), P13(뒤로 키)은 기기에서 따로 보지 않았다 (P11·P13 동작은 JVM 흐름 테스트로만).
  - release를 기기에 설치해 다시 보지는 않았다 (빌드만 확인).

## 결정 / 배운 점

- tester 버그 1: `PetProfileSource`에 "구독 즉시 값을 내보낸다"를 못 박는 대신 ViewModel을 고쳤다. 나중 서버 구현은 첫 값이 늦을 수 있기 때문이다.
- 알려진 한계: 폰 세로 + debug 패널에서는 아래 버튼 영역이 커서 카드가 짧게 보인다 (스크롤). 칩 "골든리트/리버"처럼 한국어가 낱말 중간에서 줄바꿈된다 (007과 같은 문제).
- Pleos 전화 앱이 번호를 보여 주지 않아서, 실차에서도 그렇다면 카드에 병원 번호를 늘 보이게 하는 편이 낫다. 지금은 시연 안내 줄에 번호가 보인다. B4 답을 보고 정한다.

- 아이콘: material3가 이미 `material-icons-core`를 가져와서 새 의존성 없이 `Icons.Filled.Phone`·`Send`·`Check`, `Icons.Outlined.Info`·`FavoriteBorder`·`Warning`·`CheckCircle`을 썼다. 카드 줄 아이콘: 증상 Info, 병력 FavoriteBorder, 알레르기 Warning, 약 CheckCircle.
- 카드 고정색(`CardColors`)과 채움 버튼색(`ActionBlue`)은 `ui/theme/Color.kt`에 두었다 (계획의 `ui/card/CardColors.kt` 대신).
- 화면 글자 함수는 `ui/card/CardTexts.kt`, 발바닥·아바타는 `ui/card/PetVisuals.kt`, 버튼 묶음은 `MedicalCardActions`.
- 미리보기용 보리 값은 `ui/card/MedicalCardScreen.kt`의 `PreviewPet`에 직접 적었다. ui가 `data`(SamplePetProfiles)를 import하면 완료 기준 4에 걸린다.
- **strings.xml 끝 공백이 사라진다**: `card_list_separator`(", "), `card_chip_separator`(" · ")가 "호흡 곤란,기침"으로 붙어 나왔다. 따옴표로 감쌌다 (`fix(ui)` 커밋).
- 옛 `ParkedGuidanceSection.kt`는 `SimulatedGuidanceNotice.kt`로 이름을 바꾸고 그 함수만 남겼다. `ArrivedScreen.kt`와 쓰지 않는 문자열 6개(`arrival_close` 포함)를 지웠다.
- 도착 [닫기]는 `MainActivity`에서 `onDismissArrival()` 다음 `onCloseCard()`를 부른다. 정차 쪽 고르기와 시스템 뒤로는 `HomeScreen`의 `ParkedFlow`가 한다.
- Pleos 에뮬레이터 시간대가 `America/Los_Angeles`라 정보 갱신일(2026-11-05T00:00Z)이 "11월 4일"로 보인다. 규칙(기기 시간대 날짜)대로다.

