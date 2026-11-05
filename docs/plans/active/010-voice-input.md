# 010 음성으로 증상 말하기 (증상 고르기 화면의 추가 입력)

- 목표: 정차 중 증상 고르기 화면에서 마이크 버튼을 누르고 한 문장으로 말하면, Pleos SpeechToText `ON_DEVICE`가 받아쓴 문장을 키워드 규칙으로 증상 프리셋에 매핑해 선택에 더하고, 사용자가 버튼으로 확인·수정한 뒤 지금처럼 카드를 만든다.
- 완료 기준 (어떻게 검증하나):
  1. `node .claude/hooks/build-gate.mjs pipeline`이 "검증 파이프라인 통과."를 출력한다.
  2. `./gradlew assembleRelease`가 성공한다.
  3. 단위 테스트:
     - 새 테스트가 통과한다: `FakeVoiceInputTest`, `SymptomMatcherTest`, `SymptomSentenceCorpusTest`, `MedicalCardSessionSelectTest`, `VoicePlanTest`, `SymptomVoiceViewModelTest`, `RealVoiceInputTest`, 그리고 tester가 추가한 경계 테스트.
     - 기존 테스트(시작 시점 개수를 진행 로그에 적는다)가 그대로 통과한다.
     - `git diff dev --diff-filter=MD --name-only -- app/src/test`의 출력이 비어 있다 (기존 테스트 파일은 추가만, 수정·삭제 없음).
     - 아래 `git diff dev -- <경로>`의 출력이 비어 있다. 음성이 주행 안전 규칙·길안내·카드 조합을 건드리지 않고, 나중에 음성만 뺄 수 있다는 뜻이다:
       - `app/src/main/java/com/meongcare/app/domain/driving/`, `domain/guidance/`, `domain/pet/`, `domain/hospital/`, `domain/phone/`
       - `app/src/main/java/com/meongcare/app/data/driving/`, `data/guidance/`, `data/demo/`, `data/pet/`, `data/phone/`, `data/hospital/`, `data/vehicle/`
       - `app/src/main/java/com/meongcare/app/di/SourcePlan.kt`, `di/DataMode.kt`
       - `app/src/main/java/com/meongcare/app/ui/home/HomeViewModel.kt`, `ui/home/HomeContent.kt`, `ui/home/DrivingScreen.kt`, `ui/home/ParkedContent.kt`
       - `app/src/main/java/com/meongcare/app/ui/guidance/`
       - `app/src/main/java/com/meongcare/app/ui/card/MedicalCardContent.kt`, `ui/card/MedicalCardUiState.kt`, `ui/card/MedicalCardViewModel.kt`
  4. 정적 검사 (Grep):
     - `import ai.pleos.playground.stt`는 `app/src/main/java/com/meongcare/app/data/voice/PleosSpeechToTextClient.kt`에만 있다.
     - `app/src/main/java/com/meongcare/app/ui/` 아래에 `import com.meongcare.app.data` 0건, `import com.meongcare.app.di` 0건.
     - `app/src/main/java` 전체에 `TextField` 0건 (차 앱에 글자 입력이 없다).
     - `registerApp` 0건 (`ON_DEVICE`만 쓴다. Client ID/Secret을 쓰지 않는다).
     - 병합 매니페스트(`app/build/intermediates/` 아래 `AndroidManifest.xml`)에서 `pleos.car.permission`은 `STT_SERVICE` 하나뿐이다. `RECORD_AUDIO`는 `[사용자 승인 필요]` 3의 답과 맞다. AAR이 더한 권한(예: `INTERNET`)은 진행 로그와 sdk-notes에 적는다.
     - `domain/voice/`, `ui/voice/`에 `Log.` 0건. `data/voice/`의 `Log.`는 `PleosSpeechToTextClient.kt`에만 있고 이벤트 이름·오류 이름만 남긴다 (인식 문장 없음. 단계 7에서 파일을 읽어 확인).
  5. 일반 폰 에뮬레이터 (`my_avd`): 아래 "확인 절차"의 P1~P8.
  6. Pleos 에뮬레이터 (`pleos_connect`, Cold boot, `-allow-host-audio`): 아래 "확인 절차"의 D1~D9. `adb logcat -d -t 300 *:E`에 `com.meongcare.app` 크래시와 `SecurityException`이 없다.
  7. 문서:
     - `docs/architecture.md`: "서비스 기능 스택"의 "음성 → 글자" 행이 구현 사실과 맞다. "데이터 모드" 표에 음성 입력 칸이 있다. "음성 입력 빼는 법"과 "SpeechToText 수명주기"가 있다. "패키지 구조"에 `domain/voice/`, `data/voice/`, `ui/voice/`, `di/VoicePlan.kt`가 있다.
     - `docs/pleos/sdk-notes.md` "STT `ON_DEVICE` 실행 확인"에 이번 구현에서 본 것(import 경로, 무음일 때 이벤트, `stop()` 뒤 이벤트, 콜백 스레드, 병합 매니페스트)이 관찰로 적혀 있다.
     - `README.md`의 시연 방법에 음성 입력 순서가 있다.
- 브랜치: `feat/voice-input` (dev 기준. 문서 커밋 `979c9dc`, `284245c`가 먼저 올라가 있다)
- 관련 문서:
  - AGENTS.md (아키텍처 규칙: SDK는 `data/` 래퍼에서만, 래퍼마다 Fake, 콜백→Flow, 데이터 모드. 규칙 1·2·3·7)
  - docs/pleos/sdk-notes.md "SpeechToText SDK (v3.3.1)", "STT/TTS 사용 조건 정리", **"STT `ON_DEVICE` 실행 확인 (관찰, 2026-11-05)"**
  - docs/pleos/emulator-setup.md "에뮬레이터 마이크 (Mac)", "Pleos 에뮬레이터에서 직접 해 본 것" (Virtual Console 기어 요청)
  - docs/pleos/ux-guidelines.md "주행 안전 설계 원칙"(음성 입력 우선), "Maps/Navigation 앱 가이드의 주행 중 조작 기준"(마이크: 음성 입력 시에만), "오디오 포커스", "성능 기준"(2초 안에 반응·0.25초 넘으면 응답 중 표시), "앱 심사·보안"(로그 마스킹)
  - docs/architecture.md "서비스 기능 스택", "사용하기로 한 Pleos 권한"(`STT_SERVICE` 2026-11-03 팀 승인), "데이터 모드", 문의 B1·B2
  - docs/plans/completed/009-medical-card.md (증상 프리셋, `MedicalCardSession`, 화면 조합 두 단계, 확인 절차 방식)
  - 실험 코드: 로컬 브랜치 `spike/stt-ondevice`의 `app/src/debug/java/com/meongcare/app/SttProbeActivity.kt` (dev에 합치지 않는다). 읽는 법: `git show spike/stt-ondevice:app/src/debug/java/com/meongcare/app/SttProbeActivity.kt`
    - **planner는 이 파일을 직접 읽지 못했다** (git 명령을 쓸 수 없었다). 이 계획의 SDK 호출 순서와 이벤트 처리는 sdk-notes의 관찰 기록만 근거로 했다. 단계 5-2 전에 구현자가 읽고, 다르면 이 계획을 고친다.

## 범위

- **필수** (약 1~1.5일, 두 에뮬레이터 확인 포함): 단계 0~9.
  - 11/15 제출물에 주는 효과:
    - 시연 영상: "증상을 말하면 버튼이 저절로 골라진다" 장면을 찍을 수 있다. **영상 촬영 전에 단계 0~7을 끝낸다.**
    - 기획서: "Pleos SpeechToText(`ON_DEVICE`)로 받아쓴 말을 규칙으로 증상에 매핑한다. 버튼 입력은 그대로 있다"를 구현된 사실로 쓸 수 있다.
    - APK: 시연 모드에서 음성 입력이 보인다 (`[사용자 승인 필요]` 2의 답대로).
- **있으면 좋음**: 단계 A~D. 필수가 끝난 뒤, 단계 9(노트) 전에 한다. 12/03 PT까지 미뤄도 된다.
- **이번 범위 밖**:
  - 주행 중 음성 입력 (`[사용자 승인 필요]` 1, 문의 B1). 주행 화면은 바꾸지 않는다.
  - TTS(음성 안내, 읽어 주기). 시험하지 않았다.
  - AI(Claude API)로 문장 정리. 문의 A1 뒤.
  - Gleo Interface (음성 명령).
  - 마이크 음량 표시(`onUpdatedRms`), 말소리가 작을 때 따로 알리기.
  - 주행 중 받아쓰기 제한(`UX_RESTRICTIONS_NO_VOICE_TRANSCRIPTION`) 처리. 주행 중에 쓰지 않으므로 지금은 해당 없다.

## 설계 결정 (planner 판단과 근거)

아래 코드 블록은 기본안이다. 이름과 세부는 구현하면서 바꿔도 된다. 바꾸면 이 블록을 고치고 "결정 / 배운 점"에 이유를 남긴다. `[사용자 승인 필요]`가 붙은 것은 "확인 필요"에서 묻는다.

### 1. 음성은 증상 고르기 화면 안의 추가 입력이다

- 009 흐름은 그대로다: 정차 → [진료 카드 만들기] → 증상 고르기 → [카드 만들기] → 카드. `homeContentOf`·`parkedContentOf`·`medicalCardOf`는 바꾸지 않는다 (완료 기준 3).
- 증상 고르기 화면 위쪽(제목 아래)에 음성 영역을 더한다. 음성 입력이 없는 구성이면 영역이 통째로 없다 (지금 화면과 같다).

| 음성 상태 | 화면 | 버튼 |
|---|---|---|
| 대기 | "한 문장으로 말해 주세요. 예: 토하고 숨을 가쁘게 쉬어요" | [🎤 말로 증상 고르기] (64dp 이상) |
| 켜는 중 (누른 직후, 준비 전) | "마이크를 켜는 중이에요" | [듣기 취소] |
| 듣는 중 | "듣고 있어요" + 받아쓰는 글자(부분 결과)가 그대로 늘어난다 | [듣기 취소] |
| 들음 (찾음) | "알아들은 말: …" + "골랐어요: 구토, 호흡 곤란" + "아래에서 고칠 수 있어요". 해당 버튼이 선택(체크) 상태가 된다 | [다시 말하기] |
| 들음 (못 찾음) | "알아들은 말: …" + "맞는 증상을 찾지 못했어요. 버튼으로 골라 주세요" | [다시 말하기] |
| 실패 | 말소리 없음: "말소리를 듣지 못했어요" / 마이크: "마이크를 쓸 수 없어요. 버튼으로 골라 주세요" / 쓸 수 없음: "지금은 음성 입력을 쓸 수 없어요. 버튼으로 골라 주세요" | [다시 말하기] |

- 마이크는 **사용자가 버튼을 누를 때만** 켠다. 화면에 들어갈 때 저절로 켜지 않는다. 근거: "마이크: 음성 입력 시에만 허용" (ux-guidelines, Maps/Navigation 가이드), 사용자 통제.
- 누르면 바로 "켜는 중"을 보인다. 성능 기준(0.25초를 넘으면 응답 중 표시, 2초 안에 반응)을 지킨다.
- **한 번에 한 문장.** 관찰상 문장 중간에 쉬면 거기서 인식이 끝나고 이어 말한 부분은 버려진다 (sdk-notes). 그래서 이어 말하려면 [다시 말하기]를 누른다. 안내 문구에 "한 문장으로"를 쓴다.
- **최종 결과만 증상에 매핑한다.** 부분 결과는 글자만 보여 준다 (부분 결과는 "강아지가 다리를…"처럼 잘려 있을 수 있다).
- **음성 결과는 선택에 더하기만 한다** (빼지 않는다). 두 번 말하면 쌓인다. 이미 버튼으로 고른 것도 그대로다. 잘못 골라졌으면 사용자가 버튼을 눌러 푼다.
- [듣기 취소]는 그때까지 들은 것을 버린다. 들은 것을 반영하려면 말을 끝내면 된다 (관찰상 말이 끝나면 0.1~0.3초 뒤 최종 결과가 온다).
- 아래 [취소] [카드 만들기]는 지금 그대로다. 듣는 중에 [카드 만들기]를 누르면 듣기를 멈추고 지금 선택으로 카드를 만든다.
- 인식 문장은 증상 고르기 화면에만 보인다. **카드에는 넣지 않는다** (카드 조합이 그대로여야 음성만 뺄 수 있다. 넣으려면 있으면 좋음 C).
- 주행 화면에는 아무것도 더하지 않는다.

### 2. 주행·화면 전환 때 듣기를 멈춘다

- 듣기는 증상 고르기 화면이 보일 때만 산다. 다음 경우 듣기를 멈추고(`SpeechToText`를 놓는다) 들은 것을 버리고 대기로 돌아간다:
  - 세션이 고르는 중(`Picking`)이 아니게 됐다: [카드 만들기], [취소], 시스템 뒤로, [홈]. → ViewModel이 `session.state`를 보고 멈춘다 (JVM 테스트).
  - 주행(또는 모름)이 돼서 주행 화면으로 바뀌었다, 화면 회전 등으로 고르기 화면이 사라졌다. → `HomeScreen`의 SYMPTOMS 분기에서 `DisposableEffect`의 `onDispose`로 `onStop`. (기기 확인 P5, D5)
  - 앱이 뒤로 갔다 (`Lifecycle.Event.ON_STOP`). → 같은 분기의 `LifecycleEventEffect(ON_STOP)`로 `onStop`. (`lifecycle-runtime-compose` 2.9.0에 있다)
- 다시 정차하면 고르던 선택은 그대로(009 그대로)이고, 음성은 대기다.
- 이유: 주행 중에는 아무 입력도 받지 않는다는 지금 규칙(규칙 3, 006·009)을 그대로 지킨다. 주행 중에 끝난 문장을 반영하면 사용자가 모르는 사이 선택이 바뀐다.

### 3. 도메인 인터페이스와 Fake

```kotlin
// domain/voice/VoiceInput.kt (Android·Pleos를 모른다)
interface VoiceInput {
    /**
     * 한 문장을 듣는다. 구독하면 마이크를 켜고, [VoiceEvent.Final] 또는 [VoiceEvent.Failed]를 낸 뒤 끝난다.
     * 구독을 취소하면 듣기를 멈추고 들은 것을 버린다. 한 번에 하나만 구독한다.
     */
    fun listen(): Flow<VoiceEvent>
}

sealed interface VoiceEvent {
    /** 마이크가 켜졌다 (SDK `onReady`) */
    data object Ready : VoiceEvent
    /** 받아쓰는 중인 글자. 앞 값보다 길어지거나 바뀔 수 있다 */
    data class Partial(val text: String) : VoiceEvent
    /** 다 들었다. 빈 문장은 오지 않는다 (빈 문장이면 Failed(NO_SPEECH)) */
    data class Final(val text: String) : VoiceEvent
    data class Failed(val reason: VoiceFailure) : VoiceEvent
}

enum class VoiceFailure {
    /** 말소리를 듣지 못했다 (시간 초과, 최종 결과 없음, 빈 문장) */
    NO_SPEECH,
    /** 마이크·오디오 문제 */
    MIC,
    /** 이 기기·지금 상태에서 음성 인식을 쓸 수 없다 (초기화 실패, 엔진·언어·권한 문제 등) */
    UNAVAILABLE,
}
```

- 콜드 `Flow` 한 번 = 듣기 한 번. 지금 앱의 Real 주행 상태(구독할 때 연결하고 끝나면 끊는다, 006)와 같은 모양이다. 취소가 곧 `stop()`이라 화면 전환 처리가 단순해진다.
- `Fake` (`data/voice/FakeVoiceInput.kt`):
  - 생성자: `sentences: List<String> = SampleVoiceSentences.all`, 시간 값(`readyDelay` 300ms, `wordDelay` 400ms, `finalDelay` 300ms. 테스트에서 바꾼다).
  - `listen()`: `Ready` → 낱말을 하나씩 늘린 `Partial`(예: "강아지가", "강아지가 갑자기", …) → `Final(문장)` → 끝. 부를 때마다 다음 예시 문장(돌아가며).
  - `SampleVoiceSentences` (모두 증상 하나 이상으로 매핑돼야 한다. 테스트로 확인):
    1. "강아지가 갑자기 토하고 숨을 가쁘게 쉬어요" (구토, 호흡 곤란. sdk-notes에서 실제 엔진이 똑같이 받아쓴 문장)
    2. "기침을 하고 기운이 없어요" (기침, 기운 없음)
    3. "다리를 절고 밥을 안 먹어요" (절뚝거림, 식욕 없음)
  - Fake는 실패를 내지 않는다. 실패 화면은 테스트용 가짜(`flowOf(...)`)와 미리보기로 본다 (기기에서 보려면 있으면 좋음 D).

### 4. 증상 키워드 규칙 (AI 없음, 순수 함수)

```kotlin
// domain/voice/SymptomMatcher.kt
/** 받아쓴 한 문장에서 증상 프리셋을 찾는다. 진단이 아니라 말한 낱말을 버튼에 옮기는 것이다. */
fun symptomsIn(sentence: String): Set<Symptom>
```

- 규칙:
  1. **정규화**: 공백·문장부호를 모두 지운다. 받아쓰기 결과의 띄어쓰기가 들쭉날쭉하기 때문이다 ("숨을 가쁘게" / "숨을가쁘게").
  2. **포함 검사**: 증상마다 정한 키워드(공백 없는 형태)가 정규화한 문장에 들어 있으면 그 증상이다. 한 문장에서 여러 증상을 모두 찾는다.
  3. **부정**: 키워드 바로 앞이 "안"·"못"이거나, 키워드 뒤부터 다음 "고"·"는데"·"지만"·"서"("에서"는 제외)까지(최대 8글자) 안에 "않"·"없"·"안했"·"안해"·"안하"·"아니"가 있으면 그 키워드는 건너뛴다.
     - 예: "구토는 안 했어요" → 구토 아님. "설사는 없고 기침을 해요" → 기침만. "안 토했어요" → 구토 아님.
  4. **뜻에 부정이 들어간 키워드**("기운이없", "안먹", "못걷" 등)는 3을 적용하지 않는다. "기운이 없고 밥을 안 먹어요" → 기운 없음, 식욕 없음.
  5. 짧은 글자 하나("토", "숨", "피")는 키워드로 쓰지 않는다. "토요일", "토마토를"이 구토로 잡히지 않게 한다 (관찰된 받아쓰기에 "토요일"이 실제로 나왔다).
     - 키워드를 품은 다른 낱말("토마토", "커피")은 정규화한 뒤 먼저 가린다 (tester B1, 2026-11-05).
- 키워드 기본안 (planner 작성. 의학 검토 아님. 바꾸려면 표만 고친다):

| 증상 | 키워드 (공백 없는 형태) | 뜻에 부정 포함 |
|---|---|---|
| 호흡 곤란 | 숨을가쁘, 숨이가쁘, 숨가쁘, 숨이가빠, 숨가빠, 숨을바쁘, 숨이바쁘, 숨이차, 숨을헐떡, 헐떡, 호흡곤란, 호흡이힘들, 호흡이어렵, 숨쉬기힘들, 숨쉬기어렵 | 숨을못쉬, 숨을잘못쉬 |
| 경련 | 경련, 발작, 경기를일으, 경기를해, 경기했 | |
| 쓰러짐 | 쓰러, 기절, 의식을잃, 정신을잃 | 의식이없, 못일어나, 일어나지못 |
| 출혈·상처 | 피가, 피를흘, 피나, 출혈, 상처, 다쳤, 다쳐, 찢어, 물렸 | |
| 이물 삼킴 | 삼켰, 삼킨, 삼켜, 이물, 주워먹, 집어먹 | |
| 구토 | 구토, 토했, 토하, 토해, 토를했, 토를해, 토를하, 게워, 구역질 | |
| 설사 | 설사, 묽은변, 변이묽, 물똥, 혈변 | |
| 기침 | 기침, 켁켁, 캑캑, 컥컥 | |
| 기운 없음 | 축처, 늘어져, 늘어졌, 늘어지, 처져, 처졌, 무기력 | 기운이없, 기운없, 힘이없, 힘없, 기력이없, 안움직, 움직이지않 |
| 식욕 없음 | | 안먹, 못먹, 먹지않, 먹지를않, 식욕이없, 식욕없, 입맛이없 |
| 절뚝거림 | 절어, 절고, 다리를절, 발을절, 절뚝, 절룩, 다리를들고, 발을들고 | 못걷, 못걸, 걷지못, 걷지를못, 다리를못 |
| 배 부풂 | 배가부풀, 배가빵빵, 배가볼록, 배가불러, 배부풂, 복부팽만 | |

  - "안쓰러워요"(가엾다)는 "쓰러" 앞이 "안"이라 규칙 3으로 걸러진다. 테스트에 넣는다.
  - "주워먹"은 "안먹"을 포함하지 않으므로 식욕 없음과 겹치지 않는다.
- 키워드 표는 같은 파일의 `Map<Symptom, Keywords>`로 둔다. 모든 `Symptom`에 키워드가 하나 이상 있어야 한다 (테스트). 프리셋을 더하면 키워드도 더해야 테스트가 통과한다.
- 화면 글자(`strings.xml`의 `symptom_*`)와는 따로 둔다. 화면 글자는 표시용, 키워드는 말하는 방식용이다.

#### 실제 문장으로 검증하는 방법

1. **JVM 이름 붙은 테스트** (`SymptomMatcherTest`): 규칙마다 대표 문장 (아래 단계 2-1).
2. **문장 모음 파일** (`SymptomSentenceCorpusTest`): `app/src/test/resources/voice/symptom-sentences.tsv`
   - 열: `문장<TAB>기대 증상(enum 이름, 쉼표, 없으면 빈칸)<TAB>출처`
   - 출처: `관찰 2026-11-05`(sdk-notes에 적힌 실제 받아쓰기), `관찰 YYYY-MM-DD 기기`(이번 확인 절차에서 나온 결과), `작성`(사람이 쓴 문장)
   - 테스트는 줄마다 `symptomsIn` 결과를 비교하고, 틀린 줄을 모두 모아 한 번에 실패 메시지로 보인다.
   - Kotlin을 몰라도 기획 담당이 문장을 한 줄씩 더할 수 있다.
   - 처음 넣을 관찰 문장 (sdk-notes "STT `ON_DEVICE` 실행 확인"):
     - "강아지가 갑자기 토하고 숨을 가쁘게 쉬어요" → `VOMITING,BREATHING_DIFFICULTY`
     - "강아지가 다리를…" → (빈칸. 부분 결과로 잘린 문장)
     - "치아가 내일 금요일이에요 내일 토요일이었으면 좋겠어요" → (빈칸)
     - "야간 예약을 위해 금요일이야" → (빈칸)
3. **기기에서 받아쓴 문장 모으기**: 확인 절차 D1~D4에서 말한 문장과 화면에 나온 "알아들은 말"(UI 트리 `uiautomator dump`로 읽는다. 로그로 남기지 않는다)을 TSV에 `관찰 YYYY-MM-DD 기기`로 더한다. 기대와 다르면 키워드를 고치거나, 고치지 않기로 하면 "결정 / 배운 점"에 이유를 적는다.

### 5. Real 구현: SDK 경계와 판단 로직을 나눈다

006의 `UxRestrictionsClient`(android.car 경계) + `RealDrivingStateSource`(JVM으로 시험하는 판단)와 같은 나눔이다.

```kotlin
// data/voice/SpeechToTextClient.kt (Pleos 타입을 밖으로 내보내지 않는다)
sealed interface SttEvent {
    data object Ready : SttEvent                       // onReady
    data object Started : SttEvent                     // onStartedRecognition (부분 결과마다 다시 온다)
    data class Updated(val text: String, val completed: Boolean) : SttEvent  // onUpdated
    data object Ended : SttEvent                       // onEndedRecognition
    data class Error(val name: String) : SttEvent      // onError(ErrorType).name
    data class InitFailed(val name: String) : SttEvent // OnInitializeListener.onFailure(ErrorType).name
}

fun interface SpeechToTextClient {
    /**
     * 새 SpeechToText를 만들어 초기화하고, 성공하면 리스너 등록 → 한국어 → request()까지 한다.
     * 돌려받은 것을 close()하면 stop() → removeListener() → release(). 두 번 불러도 된다.
     */
    fun start(onEvent: (SttEvent) -> Unit): AutoCloseable
}

// data/voice/RealVoiceInput.kt (Pleos를 import하지 않는다. JVM 테스트 대상)
class RealVoiceInput(
    private val client: SpeechToTextClient,
    private val initTimeout: Duration = 3.seconds,
    private val finalAfterEndedTimeout: Duration = 2.seconds,
    private val maxListen: Duration = 15.seconds,
) : VoiceInput

// data/voice/PleosSpeechToTextClient.kt: ai.pleos.playground.stt를 import하는 유일한 파일
class PleosSpeechToTextClient(context: Context) : SpeechToTextClient
```

- `RealVoiceInput.listen()` = `callbackFlow`:
  - `client.start { trySend(...) }`, `awaitClose { connection.close() }`.
  - 이벤트 → `VoiceEvent`:

    | 받은 것 | 내보내는 것 |
    |---|---|
    | `Ready` | `Ready` |
    | `Started` | 없음 (관찰: 부분 결과마다 다시 온다) |
    | `Updated(text, false)` | `Partial(text)` |
    | `Updated(text, true)`, 공백 아님 | `Final(text)` → 닫는다 |
    | `Updated(text, true)`, 비었거나 공백뿐 | `Failed(NO_SPEECH)` → 닫는다 |
    | `Ended` | 없음. `finalAfterEndedTimeout` 안에 최종 결과가 없으면 `Failed(NO_SPEECH)` (관찰 순서: `onEndedRecognition` 다음에 최종 결과가 온다) |
    | `Error("TIMEOUT")` | `Failed(NO_SPEECH)` |
    | `Error("MIC_ERROR" \| "AUDIO_ERROR")` | `Failed(MIC)` |
    | 그 밖의 `Error`, `InitFailed` | `Failed(UNAVAILABLE)` |
    | `initTimeout` 안에 `Ready`·`Updated`가 하나도 없음 | `Failed(UNAVAILABLE)` (음성 서비스가 없는 기기에서 콜백이 안 올 때를 막는다) |
    | 시작 후 `maxListen` | `Failed(NO_SPEECH)` |
    | 끝난 뒤에 오는 이벤트 | 무시 |
    | `client.start`가 예외 | `Failed(UNAVAILABLE)` |

  - 오류 이름은 sdk-notes의 `ErrorType` 값 목록을 그대로 쓴다. 문자열로 받는 이유: 판단 로직 파일이 Pleos를 import하지 않게 해서 JVM으로 시험하려는 것이다.
  - 시간 값은 기본안이다. 기기 확인(D4)에서 무음일 때 실제로 오는 이벤트를 보고 고친다.
- `PleosSpeechToTextClient.start()`: 순서는 sdk-notes 관찰 그대로 `SpeechToText(context, Mode.ON_DEVICE)` → `initialize(OnInitializeListener)` → `onSuccess`에서 `addListener(listener)` → `setLocale(Locale.KOREA)` → `request()`. `onFailure`면 `InitFailed`.
  - `registerApp`을 부르지 않는다 (`ON_DEVICE`는 앱 등록이 필요 없다).
  - `onUpdatedEpdData`, `onUpdatedRms`는 넘기지 않는다.
  - `close()`: 한 번만 `stop()` → `removeListener(listener)` → `release()`. 각 호출을 `try`로 감싼다. 닫은 뒤 콜백은 버린다.
  - 로그(디버그 확인용): 이벤트 이름과 오류 이름만. **인식 문장은 남기지 않는다** (반려견 의료정보, Pleos 보안 가이드 "로그의 개인정보 마스킹").
  - import 경로: `SpeechToText`(`ai.pleos.playground.stt`), `Mode`(`ai.pleos.playground.stt.constant`)는 sdk-notes에 있다. `ResultListener`, `OnInitializeListener`, `ErrorType`의 패키지는 sdk-notes에 없다 → spike 코드에서 가져오고 sdk-notes에 적는다 (산출물 근거).
  - 호출 스레드: `listen()`은 ViewModel(`viewModelScope`, Main)에서 구독되므로 SDK 호출이 메인 스레드에서 일어난다. spike와 같은지 확인한다.

### 6. SpeechToText 수명주기: 듣기 한 번마다 새로 만들고 버린다

- `listen()` 구독 한 번 = `SpeechToText` 객체 하나. 끝나거나 취소되면 `release()`하고 버린다. 문서: "release 후 다시 쓰려면 새 객체를 만든다."
- 이유: 관찰상 `initialize` 성공까지 약 20ms라 매번 만들어도 느리지 않다. 프로세스 수명 객체(`VehicleConnection` 같은)를 두면 화면 전환·실패 복구 상태가 늘어난다. 006의 `Car` 연결도 같은 방식이다.
- "`Vehicle`의 initialize/release는 `VehicleConnection` 한 곳에서" 규칙은 Vehicle SDK 규칙이다. STT는 이 규칙 밖이고, `RealVoiceInput`이 한 곳에서 관리한다. architecture.md에 적는다.

### 7. 데이터 모드별 구성: `SourcePlan`은 그대로 두고 따로 정한다

```kotlin
// di/VoicePlan.kt (Android를 모르는 순수 함수)
enum class VoiceInputKind { NONE, FAKE, REAL }

/** 음성 입력을 앱 화면에서 뺄 때 false로 바꾼다 (문의 B2가 "안 된다"일 때). */
const val VOICE_INPUT_ENABLED = true

fun voiceInputKindOf(mode: DataMode, isAutomotive: Boolean, enabled: Boolean = VOICE_INPUT_ENABLED): VoiceInputKind
```

- `SourcePlan`에 칸을 더하지 않는 이유: `SourcePlanBoundaryTest`가 `SourcePlan` 전체를 `equals`로 비교하고 생성자를 위치 인자로 부른다. 칸을 더하면 기존 테스트 파일을 고쳐야 한다. 따로 두면 음성만 빼기도 쉽다.
- 구성표 (권장안. 시연 모드 칸은 `[사용자 승인 필요]` 2의 답으로 정한다):

  | 모드 | 차량 기기 (`FEATURE_AUTOMOTIVE`) | 음성 입력 | 시연용 표시 |
  |---|---|---|---|
  | Fake | 상관없음 | FAKE (예시 문장) | O |
  | 시연 | 예 (Pleos 에뮬레이터, 실차) | **REAL** (`ON_DEVICE`, 마이크) | X |
  | 시연 | 아니오 (일반 폰) | **FAKE** (예시 문장) | O |
  | Real | 예 | REAL | X |
  | Real | 아니오 | NONE (음성 서비스가 없다) | - |
  | 스위치 꺼짐 | 모두 | NONE | - |

- `AppContainer`:
  - `val voiceInputKind = voiceInputKindOf(dataMode, isAutomotive)`
  - `val voiceInput: VoiceInput? = when (kind) { NONE -> null; FAKE -> FakeVoiceInput(); REAL -> RealVoiceInput(PleosSpeechToTextClient(context)) }`
    - 단계 3-3에서는 `REAL -> null`(임시)로 두고, 단계 5-2에서 Real로 바꾼다 (009에서 `PhoneDialer { false }`를 먼저 둔 것과 같다).
  - `val isVoiceSimulated = kind == FAKE`
- **시연용 표시** (FAKE일 때, 음성 영역 안 작은 글씨, 주행 화면에는 없음): "시연용 음성 입력이에요. 실제로 듣지 않고 예시 문장을 받아써요." 출품 유의사항(사실과 다르면 선정 취소) 때문에 숨기지 않는다.

### 8. 음성 입력 빼는 법 (문의 B2 대비)

- **화면에서만 빼기**: `di/VoicePlan.kt`의 `VOICE_INPUT_ENABLED = false` 한 줄. 모든 모드가 NONE → 음성 영역이 없다. SDK와 권한은 APK에 남는다.
- **APK에서 완전히 빼기** (STT를 쓰면 안 된다는 답): 위 한 줄 + 단계 5-2 커밋 하나를 `git revert`. SDK 의존성, `STT_SERVICE` 권한, Pleos 래퍼, `AppContainer`의 REAL 연결이 함께 빠진다.
  - 그래서 단계 5-2 커밋에는 이 다섯 가지만 넣는다: `libs.versions.toml`, `app/build.gradle.kts`, `AndroidManifest.xml`, `data/voice/PleosSpeechToTextClient.kt`, `AppContainer`의 REAL 한 줄. 이후 커밋이 이 줄들을 건드리지 않게 한다.
  - 단계 7에서 되돌리기가 깨끗한지 확인한다: `git revert --no-commit <5-2 해시>` → `./gradlew assembleDebug testDebugUnitTest` 통과 → `git revert --abort`.

### 9. ViewModel과 세션

```kotlin
// domain/card/MedicalCardSession.kt에 더한다
/** 고르는 중일 때만: [symptoms]를 선택에 더한다. 이미 고른 것은 그대로 둔다 (풀지 않는다). */
fun select(symptoms: Set<Symptom>)

// ui/voice/VoiceUiState.kt
sealed interface VoiceUiState {
    data object Idle : VoiceUiState
    data object Starting : VoiceUiState
    data class Listening(val partial: String) : VoiceUiState
    /** [matched]는 프리셋 순서. 비면 "찾지 못했어요" */
    data class Heard(val sentence: String, val matched: List<Symptom>) : VoiceUiState
    data class Failed(val reason: VoiceFailure) : VoiceUiState
}
data class VoiceScreenState(val available: Boolean, val isSimulated: Boolean, val voice: VoiceUiState)

// ui/voice/SymptomVoiceViewModel.kt
class SymptomVoiceViewModel(
    private val voiceInput: VoiceInput?,   // null이면 available = false
    private val session: MedicalCardSession,
    isSimulated: Boolean,
) : ViewModel() {
    val uiState: StateFlow<VoiceScreenState>
    fun onStartListening()   // 대기·들음·실패일 때만. 켜는 중·듣는 중이면 무시
    fun onCancelListening()  // [듣기 취소] → 대기, 결과 버림
    fun onStop()             // 화면이 사라짐·앱이 뒤로 감 → 대기, 결과 버림
}
```

- `Final(text)` → `matched = symptomsIn(text)` → `session.select(matched)` → `Heard(text, matched.inPresetOrder())`.
- 세션이 `Picking`이 아니게 되면 듣기 작업을 취소하고 `Idle`.
- `MedicalCardViewModel`은 바꾸지 않는다. 음성은 별도 ViewModel이라 통째로 뺄 수 있다.
- 화면: `ui/voice/VoicePanel.kt`(위 설계 결정 1의 표), `ui/voice/VoiceActions.kt`(`onStart`, `onCancel`, `onStop`).
- `SymptomPickerScreen`에 `voice: VoiceScreenState? = null`, `voiceActions: VoiceActions = VoiceActions()`를 더한다. `null`이거나 `available = false`면 음성 영역을 그리지 않는다 (기존 미리보기·호출은 그대로 동작).
- `HomeScreen` → `ParkedFlow` → SYMPTOMS 분기로 값을 넘긴다. `MainActivity`가 `SymptomVoiceViewModel`을 만든다. `ui/`는 `di`·`data`를 import하지 않는다 (값은 `MainActivity`가 `AppContainer`에서 꺼내 넘긴다).

### 10. 권한과 매니페스트

- `pleos.car.permission.STT_SERVICE`: 2026-11-03 팀 승인 목록에 있다 (새 권한 아님). 관찰상 보호 수준 `normal`이라 선언만 하면 설치 때 허용된다. 단계 5-2에서 선언한다.
- `android.permission.RECORD_AUDIO`: `[사용자 승인 필요]` 3. 권장안은 **선언하지 않는다**.
- SDK AAR이 병합 매니페스트에 넣는 것 (관찰): `android.permission.INTERNET`("CCG Validation 체크를 위한 권한"), `<queries>` `SPEECH_START`, 최소 음성 서비스 버전 메타데이터. **지우지 않는다** (지우면 SDK 동작을 보장할 수 없다). 전이 의존성 `universal-access` AAR이 더하는 것은 단계 5-2에서 병합 매니페스트를 보고 진행 로그와 sdk-notes에 적는다.
- 오디오 포커스: 기본안은 **요청하지 않는다** (spike는 요청 없이 동작했다). `[선행: pleos-docs-researcher]` Q1 답이 "요청해야 한다"면 단계 5-2에 넣는다.

## 확인 필요 (시작 전에 해결)

### [사용자 승인 필요]

새 `pleos.car.permission.*`은 없다 (`STT_SERVICE`는 승인됨). 아래는 기획·권한 결정이다. 한 번에 묻는다. 권장안이 위 설계 결정에 반영돼 있다.

1. **주행 중 음성 입력** (단계 4 전에 필요)
   - 권장: **이번에는 정차 중 증상 고르기 화면에서만** 음성을 쓴다. 주행으로 바뀌면 듣기를 멈추고 들은 것을 버린다. 주행 화면은 그대로다.
   - 이유: 문의 B1(주행 중 사용·음성 입력 허용) 답이 없다. 주행 중에 쓰려면 받아쓴 글자를 읽고 고치는 확인 단계가 필요한데, 주행 화면은 버튼 1개 이하·입력 없음이다. 읽어 주기(TTS)는 시험하지 않았다. 주행 규제에 `UX_RESTRICTIONS_NO_VOICE_TRANSCRIPTION`(256)이 걸리면 받아쓴 글자를 보이면 안 될 수도 있다.
   - 주의: 기획 컨셉("운전 중 음성으로 증상을 말하면")과 다르다. 기획서에는 "정차 중 음성으로 증상 입력, 주행 중 음성은 대회 문의 답 뒤 확장"처럼 맞춰 써야 한다.
   - 다른 안: 주행 화면에 큰 마이크 버튼 1개 → 들은 증상을 세션에 저장만 하고 정차 후 확인. 별도 계획이 필요하다 (주행 화면·주행 상태 도메인을 바꾼다).
2. **시연 모드에서 음성을 어떻게 보여 줄지** (단계 3-3 전에 필요. release APK = 시연 모드)
   - PT를 에뮬레이터로 하면 Mac 마이크 경로가 늦고 겹친 적이 있다 (emulator-setup "에뮬레이터 마이크 (Mac)"). 현장 소음·장비도 모른다 (문의 A2).

   | 안 | 차량 기기(Pleos 에뮬레이터)에서 | 장점 | 단점 |
   |---|---|---|---|
   | **A (권장)** | Real 마이크 | 진짜 Pleos STT를 보여 준다 (기술 활용성). 심사위원이 APK를 Pleos 에뮬레이터에서 돌려도 진짜로 동작한다 | 마이크 지연·겹침, 현장 소음. 실패해도 버튼으로 이어 가지만 시연이 끊긴다 |
   | B | Fake 예시 문장 (시연용 표시) | 늘 같은 결과, 마이크 필요 없음 | 진짜 인식이 아니다. 기술 활용성 설명이 약하다 |
   | C | Real 엔진 + 앱에 넣은 녹음 문장 (`sendAudio`, `ON_DEVICE`에서만 되는 문서화된 API) | 진짜 Pleos 엔진이 실시간으로 받아쓰는 것을 마이크 없이 보여 준다 | 녹음 파일(팀원 목소리, 16 kHz mono PCM)이 필요. 버튼 하나 더. "녹음된 예시 음성"이라고 표시해야 한다. 있으면 좋음 A로 따로 만든다 |

   - 권장: **A를 필수로**, PT 대비로 **C를 있으면 좋음**으로 (12/03 전에). 시연 영상은 A로 찍되, 마이크가 말썽이면 debug Fake 모드(B와 같은 화면, 시연용 표시)로 찍는다.
   - 일반 폰에서의 시연 모드: 권장 **Fake + 시연용 표시** (심사위원이 폰 에뮬레이터로 돌려도 흐름을 본다. 길안내 Fake와 같은 생각). 다른 안: 마이크 영역 숨김.
3. **`RECORD_AUDIO` 권한** (단계 5-2 전에 필요)
   - 권장: **선언하지 않는다.** Pleos 에뮬레이터에서 앱에 권한이 없으면 음성 서비스가 자기 이름으로 대신 녹음했다 (sdk-notes 관찰). STT 권한 문서에 `RECORD_AUDIO` 언급이 없다. Pleos 가이드는 불필요한 권한 요청을 반려 사유로 든다. 권한 팝업 없이 시연된다.
   - 위험: 실차에서는 대신 녹음되지 않을 수 있다 (미확인). 그러면 `MIC_ERROR`/`PERMISSION_DENIED` → "버튼으로 골라 주세요"로 이어진다. 실차·B2 답을 보고 다시 정한다.
   - 다른 안: 선언 + 처음 [말로 증상 고르기]를 누를 때 런타임 요청 (정차 중). 커밋 1개(거부 처리 포함)가 늘고, 시연 때 권한 팝업이 한 번 뜬다. AAOS는 user 10이라 adb로 줄 때 `--user 10`이 필요하다.

### planner가 정한 것 (바꾸려면 알려 달라. 묻지 않고 진행한다)

- 마이크는 버튼을 누를 때만 켠다. 한 번에 한 문장. 최종 결과만 매핑하고 선택에 더하기만 한다.
- 인식 문장은 증상 고르기 화면에만 보이고 카드에는 넣지 않는다.
- 키워드 표 기본안(설계 결정 4). 고칠 문장이 생기면 TSV에 줄을 더하고 표를 고친다.
- `SpeechToText`는 듣기 한 번마다 새로 만들고 버린다. 오디오 포커스는 요청하지 않는다 (Q1 답 전까지).
- SDK가 더하는 `INTERNET` 권한은 그대로 둔다.
- 로그에 인식 문장을 남기지 않는다.
- Fake 예시 문장 3개 (설계 결정 3).

### [선행: pleos-docs-researcher]

단계 5-2 앞. **막지는 않는다**: 답이 "문서에 없음"이면 기본안대로 하고 기기에서 본 것을 sdk-notes에 관찰로 적는다.

- Q1. STT `request()`로 듣는 동안 오디오 포커스는 어떻게 되나? SDK(음성 서비스)가 미디어를 줄이거나 멈추는가, 앱이 `USAGE_VOICE_COMMAND`/`USAGE_ASSISTANT`로 포커스를 요청해야 하는가? (오디오 포커스 가이드, STT 가이드)
- Q2. `ResultListener` 콜백은 어느 스레드로 오는가? `initialize`/`request`/`stop`/`release`를 메인 스레드에서 불러야 하는가? `stop()` 뒤에 `onEndedRecognition`·최종 결과가 오는가? (STT API Reference, 시작·중지 페이지)
- Q3. 말이 없을 때 SDK가 `onError(TIMEOUT)`을 보내는가? EPD(끝점 검출) 대기 시간이나 최대 녹음 시간이 문서에 있는가? (`ErrorType.TIMEOUT` 설명, EPD 관련 페이지)

### 실행해서 확인할 것 (문서에 없음)

- 무음일 때 오는 이벤트와 시간 (D4) → `RealVoiceInput` 시간 값 조정.
- `stop()`·`release()` 뒤 음성 서비스 녹음이 끝나는지 (D5, `adb logcat`에서 음성 서비스의 녹음 시작·종료 줄).
- 같은 화면에서 듣기를 여러 번(새 객체) 해도 문제가 없는지 (D6·D7).
- 병합 매니페스트에 `universal-access` AAR이 더하는 것 (단계 5-2).
- Pleos 에뮬레이터에서 앱 설치 후 `STT_SERVICE`가 user 10에 `granted=true`인지 (D8).

## 확인 절차

`/verify` 규칙대로 에이전트는 에뮬레이터를 직접 띄우지 않는다. 사용자에게 Cold boot를 요청한다. 기기가 둘이면 `adb -s`로 고른다.
스크린샷과 UI 트리는 `.claude/.cache/p10-<번호>.png`, `.claude/.cache/p10-<번호>.xml`에 둔다 (커밋하지 않는다).
Pleos 에뮬레이터는 emulator-setup "에뮬레이터 마이크 (Mac)"대로 켠다: 에뮬레이터를 켠 앱에 macOS 마이크 권한, `-allow-host-audio`. 스피커로 `say`를 틀어 마이크에 넣는다 (이어폰을 빼고). 문장 사이에 몇 초 쉰다 (겹침 관찰 때문).

### 일반 폰 에뮬레이터 `my_avd` (debug, Fake 모드)

| # | 조작 | 기대 |
|---|---|---|
| P1 | [진료 카드 만들기] | 증상 고르기 위쪽에 음성 영역: 안내 문구, [말로 증상 고르기], 시연용 음성 안내. `EditText` 0개 |
| P2 | [말로 증상 고르기] | "마이크를 켜는 중" → "듣고 있어요" + 글자가 낱말씩 늘어남 → "알아들은 말: 강아지가 갑자기 토하고 숨을 가쁘게 쉬어요", "골랐어요: 호흡 곤란, 구토". 두 버튼 `checked="true"` |
| P3 | 기침을 손으로 누름 → [다시 말하기] | 두 번째 예시 문장 → 기운 없음이 더해짐. 호흡 곤란·구토·기침은 그대로 |
| P4 | [카드 만들기 (4개)] | 카드 주요 증상 "호흡 곤란, 구토, 기침, 기운 없음" (프리셋 순서). 카드에 인식 문장은 없다 |
| P5 | [수정] → [다시 말하기] → 듣는 중에 debug [주행] → [정차] | 주행 화면(음성 영역 없음) → 고르기 화면, 음성은 대기, 선택은 주행 전 그대로 |
| P6 | 듣는 중에 [듣기 취소] / 듣는 중에 [카드 만들기] | 대기, 선택 그대로 / 지금 선택으로 카드 |
| P7 | 세로·가로 × 라이트·다크로 음성 영역 각 상태(대기·듣는 중·들음) | 글자 잘림·겹침 없음. 버튼 높이 64dp 이상. 증상 버튼이 화면 밖으로 밀려도 스크롤로 닿는다. 다 보면 설정을 되돌린다 |
| P8 | debug 패널로 Real 모드 | 고르기 화면에 음성 영역이 없다 (일반 폰 Real = NONE) |

### Pleos 에뮬레이터 `pleos_connect` (Cold boot, debug 시연 모드 "DEBUG · 시연 (주행 Real)")

Virtual Console 명령은 006 노트의 `vc()` 함수를 쓴다.

| # | 조작 | 기대 |
|---|---|---|
| D1 | `vc "/vehicle/gear?gear=Park"` → [진료 카드 만들기] → [말로 증상 고르기] → `say -v Yuna "강아지가 갑자기 토하고 숨을 가쁘게 쉬어요"` | 시연용 음성 안내 없음. 부분 결과가 늘어나고 최종 문장 → 구토·호흡 곤란 선택. 실제 받아쓴 문장을 진행 로그와 TSV에 적는다 |
| D2 | [다시 말하기] → `say -v Yuna "강아지가 다리를 절어요"` | 절뚝거림이 더해짐 |
| D3 | [다시 말하기] → `say -v Yuna "내일 토요일이었으면 좋겠어요"` | "맞는 증상을 찾지 못했어요", 선택 그대로 |
| D4 | [다시 말하기] → 말하지 않고 기다림 | 몇 초 안에 "말소리를 듣지 못했어요". 실제로 온 이벤트(오류 이름, 시간)를 로그에서 읽어 진행 로그에 적는다 |
| D5 | [다시 말하기] → 듣는 중에 `vc "/vehicle/gear?gear=Drive"` → `gear=Park` | 주행 화면 → 고르기 화면, 음성 대기, 선택 그대로. 로그에 우리 쪽 `close`와 음성 서비스 녹음 종료가 보인다 |
| D6 | [다시 말하기] → [듣기 취소] → [다시 말하기] → 말함 | 두 번째 듣기가 정상 동작 (새 객체) |
| D7 | [카드 만들기] → [길 안내 시작] → D → 도착 → P | 009 흐름 그대로, 카드에 음성으로 고른 증상 |
| D8 | `adb shell dumpsys package com.meongcare.app` | `pleos.car.permission.STT_SERVICE: granted=true` (user 10). `RECORD_AUDIO`는 `[사용자 승인 필요]` 3의 답과 맞다 |
| D9 | 로그 | `adb logcat -d -t 300 *:E`에 크래시·`SecurityException` 없음. `adb logcat -d`에서 우리 앱 로그에 인식 문장이 없다 |

- 시간이 되면 007 방식(debug 키로 서명한 확인용 사본, `.claude/.cache/`, 커밋 안 함)으로 release를 설치해 D1을 한 번 더 본다 (release = 시연 모드 = Real 음성). 하지 않으면 하지 않았다고 적는다.

## 단계

단계 하나 = 커밋 1~3개. 커밋마다 `assembleDebug`가 통과해야 한다 (precommit 훅). 테스트는 대상 코드와 같은 커밋에 넣는다.

**시작 전 준비:**
- 위 `[사용자 승인 필요]` 1~3을 사용자에게 한 번에 확인받고 답을 진행 로그에 적는다. 답이 권장안과 다르면 설계 결정과 확인 절차를 먼저 고친다.
- `[선행: pleos-docs-researcher]` Q1~Q3은 메인이 researcher에게 넘긴다. 단계 5-2 전까지 답을 받으면 된다.
- 미추적 폴더 `docs/product/bmad/`, `.agents/`, `.codex/`는 스테이징하지 않는다. 파일 이름으로만 `git add`한다.
- 시작 시점의 단위 테스트 개수를 진행 로그에 적는다 (`./gradlew testDebugUnitTest`).
- 사용자에게 알린다: 단계 4 이후 `my_avd`, 단계 5-2 이후 `pleos_connect`(마이크 설정 포함) Cold boot를 요청한다.

**순서 메모:** 인터페이스·Fake(1) → 순수 규칙(2) → 세션·ViewModel·구성표(3) → 화면(4, Fake로 동작) → Real 판단 로직(5-1) → Pleos 래퍼(5-2, 되돌리기 단위) → tester(6) → `/verify`(7) → 문서(8) → 노트(9).

### 필수

- [ ] 0. 이 계획 파일 커밋 → `docs(plans): 010 음성 증상 입력 계획 추가`

- [ ] 1. 음성 입력 인터페이스와 Fake → `feat(data): 음성 입력 인터페이스와 Fake 구현 추가`
  - `domain/voice/VoiceInput.kt` (`VoiceInput`, `VoiceEvent`, `VoiceFailure`. 설계 결정 3)
  - `data/voice/FakeVoiceInput.kt`, `data/voice/SampleVoiceSentences.kt`
  - `FakeVoiceInputTest` (가상 시간):
    - `` `준비, 늘어나는 부분 결과, 최종 결과 순서로 내고 끝난다`() ``
    - `` `부를 때마다 다음 예시 문장을 쓰고 끝에서 처음으로 돌아간다`() ``
    - `` `구독을 취소하면 더 내보내지 않는다`() ``

- [ ] 2. 증상 키워드 규칙
  - 2-1. → `feat(domain): 받아쓴 문장에서 증상을 찾는 키워드 규칙 추가`
    - `domain/voice/SymptomMatcher.kt` (`symptomsIn`, 키워드 표. 설계 결정 4)
    - `domain/card/Symptom.kt`의 KDoc "음성 입력(STT "준비 중") 대신"을 지금 사실로 고친다. **enum 값과 순서는 바꾸지 않는다.**
    - `SymptomMatcherTest`:
      - `` `관찰한 받아쓰기 문장에서 구토와 호흡 곤란을 찾는다`() `` ("강아지가 갑자기 토하고 숨을 가쁘게 쉬어요")
      - `` `띄어쓰기와 문장부호가 달라도 같은 증상을 찾는다`() `` ("숨을가쁘게 쉬고 토해요!", "토 하고 숨을 가쁘게")
      - `` `한 문장에서 여러 증상을 모두 찾는다`() ``
      - `` `토요일 토마토 안쓰러워처럼 키워드 글자가 들어간 다른 낱말은 고르지 않는다`() ``
      - `` `안 했다거나 없다고 한 증상은 고르지 않는다`() `` ("구토는 안 했어요" → ∅, "안 토했어요" → ∅, "설사는 없고 기침을 해요" → 기침)
      - `` `뜻에 부정이 들어간 증상은 그대로 고른다`() `` ("기운이 없고 밥을 안 먹어요" → 기운 없음, 식욕 없음)
      - `` `증상이 없는 문장과 빈 문장은 빈 결과다`() ``
      - `` `모든 증상 프리셋에 키워드가 하나 이상 있다`() ``
    - Fake 예시 문장 3개가 모두 증상 하나 이상으로 매핑되는지도 여기서 확인한다 (`` `Fake 예시 문장은 모두 증상을 하나 이상 찾는다`() ``). `domain` 테스트가 `data`를 import하지 않게 문장을 테스트에 직접 적고, `SampleVoiceSentences` KDoc에 "SymptomMatcherTest와 같은 문장"이라고 적는다.
  - 2-2. → `test(domain): 실제 문장 모음으로 증상 규칙 확인`
    - `app/src/test/resources/voice/symptom-sentences.tsv` (설계 결정 4 "실제 문장으로 검증하는 방법". 관찰 문장 4개 + 작성 문장 20개 안팎: 증상마다 1개 이상, 부정·오탐 사례 포함)
    - `SymptomSentenceCorpusTest`: `` `문장 모음의 모든 줄이 기대 증상과 같다`() `` (틀린 줄을 모아 한 번에 보인다), `` `문장 모음 파일이 비어 있지 않고 형식이 맞다`() ``

- [ ] 3. 세션·ViewModel·구성표
  - 3-1. → `feat(domain): 진료 카드 세션에 여러 증상 한 번에 고르기 추가`
    - `MedicalCardSession.select(symptoms)` (설계 결정 9)
    - `MedicalCardSessionSelectTest` (새 파일. 기존 `MedicalCardSessionTest`는 고치지 않는다):
      - `` `고르는 중이면 여러 증상을 한 번에 더한다`() ``
      - `` `이미 고른 증상은 풀지 않는다`() ``
      - `` `고르는 중이 아니면 아무것도 하지 않는다`() ``
      - `` `빈 집합을 고르면 바뀌지 않는다`() ``
  - 3-2. → `feat(ui): 음성 증상 입력 ViewModel 추가`
    - `ui/voice/VoiceUiState.kt`, `ui/voice/SymptomVoiceViewModel.kt` (설계 결정 9)
    - `SymptomVoiceViewModelTest` (`MainDispatcherRule`, 테스트용 `VoiceInput`는 `flow { ... }`로 직접 만든다):
      - `` `마이크를 누르면 켜는 중을 거쳐 듣는 중이 되고 부분 결과 글자를 보여 준다`() ``
      - `` `최종 문장에서 찾은 증상을 지금 선택에 더한다`() ``
      - `` `찾은 증상이 없으면 선택을 바꾸지 않고 못 찾았다고 알린다`() ``
      - `` `두 번 말하면 증상이 쌓인다`() ``
      - `` `듣는 중에 취소하거나 화면이 사라지면 결과를 버리고 대기로 돌아간다`() ``
      - `` `증상 고르기가 끝나면 듣기를 멈추고 대기로 돌아간다`() ``
      - `` `켜는 중이나 듣는 중에 다시 눌러도 새로 듣지 않는다`() ``
      - `` `실패하면 이유를 보여 주고 다시 말할 수 있다`() ``
      - `` `음성 입력이 없으면 쓸 수 없음 상태이고 눌러도 아무것도 하지 않는다`() ``
  - 3-3. → `feat(di): 데이터 모드별 음성 입력 구성 추가`
    - `di/VoicePlan.kt` (`VoiceInputKind`, `VOICE_INPUT_ENABLED`, `voiceInputKindOf`. 설계 결정 7, `[사용자 승인 필요]` 2의 답)
    - `AppContainer`: `voiceInputKind`, `voiceInput`(FAKE → `FakeVoiceInput()`, REAL → **임시 `null`**), `isVoiceSimulated`
    - `VoicePlanTest`:
      - `` `구성표는 모든 모드와 기기 조합에서 계획의 표와 같다`() ``
      - `` `스위치를 끄면 모든 모드와 기기에서 음성 입력이 없다`() ``
      - `` `release 빌드는 차량 기기에서 Real 음성이다`() `` (`resolveDataMode(isDebugBuild = false, ...)`와 함께. 답이 B면 이름과 기대를 바꾼다)

- [ ] 4. 화면 (Fake로 동작)
  - 4-1. → `feat(ui): 증상 고르기 화면에 음성 입력 영역 추가`
    - `ui/voice/VoicePanel.kt` (설계 결정 1의 표, 시연용 안내), `ui/voice/VoiceActions.kt`
    - `SymptomPickerScreen`: `voice`, `voiceActions` 인자 (기본값이면 지금과 같다). 음성 영역은 제목 아래, 스크롤 안.
    - `strings.xml`: `voice_*` 문자열 (설계 결정 1의 문구, 시연용 안내)
    - 아이콘: material3가 가져오는 core 아이콘 중 마이크가 있는지 확인한다 (`Icons.Filled.Mic`은 core가 아니라 extended일 수 있다). 없으면 `material-icons-extended`를 넣지 말고 `ImageVector`로 직접 그린다 (009와 같은 원칙: 용량).
    - 미리보기: 넓음 1682×1210, 좁음 411×891 × 대기·듣는 중·들음(찾음)·들음(못 찾음)·실패·시연용, 다크
  - 4-2. → `feat(ui): 음성 입력을 정차 화면 흐름에 연결`
    - `HomeScreen`: SYMPTOMS 분기에 음성 값 넘기기, `DisposableEffect`의 `onDispose { voiceActions.onStop() }`, `LifecycleEventEffect(Lifecycle.Event.ON_STOP) { voiceActions.onStop() }` (설계 결정 2)
    - `MainActivity`: `SymptomVoiceViewModel` 만들기 (`container.voiceInput`, `container.medicalCardSession`, `container.isVoiceSimulated`)
    - 확인: `/verify`로 P1~P7 (Fake 모드). P5는 화면 전환 멈춤을 기기에서 보는 유일한 확인이다.

- [ ] 5. Real 구현
  - 5-1. → `feat(data): 음성 인식 이벤트를 Flow로 바꾸는 Real 구현 추가`
    - `data/voice/SpeechToTextClient.kt` (`SttEvent`, `SpeechToTextClient`), `data/voice/RealVoiceInput.kt` (설계 결정 5의 표)
    - Pleos SDK는 아직 넣지 않는다. 화면에 연결하지 않는다.
    - `RealVoiceInputTest` (가짜 client, 가상 시간):
      - `` `준비되면 부분 결과를 거쳐 최종 결과를 내고 끝난다`() ``
      - `` `부분 결과마다 다시 오는 인식 시작은 무시한다`() ``
      - `` `인식이 끝난 뒤 최종 결과가 오면 그대로 낸다`() `` (관찰 순서: 끝남 → 최종)
      - `` `인식이 끝났는데 최종 결과가 오지 않으면 말소리 없음으로 끝난다`() ``
      - `` `최종 결과가 비었거나 공백뿐이면 말소리 없음이다`() ``
      - `` `초기화가 실패하거나 시간 안에 응답이 없으면 쓸 수 없음으로 끝난다`() ``
      - `` `오류 이름을 말소리 없음 마이크 쓸 수 없음 세 가지로 나눈다`() `` (sdk-notes `ErrorType` 12개 전부)
      - `` `최대 듣기 시간이 지나면 멈추고 말소리 없음이다`() ``
      - `` `구독을 취소하거나 끝나면 클라이언트를 한 번만 닫는다`() ``
      - `` `끝난 뒤에 오는 이벤트는 무시한다`() ``
      - `` `클라이언트 시작이 예외를 던지면 쓸 수 없음으로 끝난다`() ``
  - 5-2. `[선행: pleos-docs-researcher]` (Q1~Q3, 막지 않음) → `feat(data): Pleos SpeechToText ON_DEVICE 래퍼 추가`
    - 먼저 spike 코드를 읽는다 (`git show spike/stt-ondevice:app/src/debug/java/com/meongcare/app/SttProbeActivity.kt`). import 경로, 호출 순서, 호출 스레드가 설계 결정 5와 다르면 이 계획부터 고친다.
    - **이 커밋에는 아래 다섯 가지만 넣는다** (설계 결정 8, 되돌리기 단위):
      - `gradle/libs.versions.toml`: `speechToText = "3.3.1"`, `pleos-speech-to-text = { group = "ai.pleos.playground", name = "SpeechToText", version.ref = "speechToText" }`
      - `app/build.gradle.kts`: `implementation(libs.pleos.speech.to.text)`
      - `AndroidManifest.xml`: `<uses-permission android:name="pleos.car.permission.STT_SERVICE" />` + 승인된 범위라는 주석 (`[사용자 승인 필요]` 3이 "선언"이면 `RECORD_AUDIO`도. 그때 런타임 요청은 5-3)
      - `data/voice/PleosSpeechToTextClient.kt` (설계 결정 5)
      - `AppContainer`: REAL → `RealVoiceInput(PleosSpeechToTextClient(context))`
    - JVM 테스트 없음 (Pleos SDK·`Context`). 판단은 5-1 테스트가 다룬다.
    - 확인: 병합 매니페스트 Grep (완료 기준 4), `/verify`로 D1~D9.
  - (5-3. `[사용자 승인 필요]` 3이 "선언 + 런타임 요청"일 때만) → `feat(ui): 처음 말하기 전에 마이크 권한 요청`
    - 처음 [말로 증상 고르기]에서 `RECORD_AUDIO`를 요청하고, 거부되면 "마이크를 쓸 수 없어요. 버튼으로 골라 주세요". 시연 Fake에서는 요청하지 않는다.

- [ ] 6. 경계 테스트 (`tester`) → `test: 음성 증상 입력 경계 테스트 추가`
  - 메인이 `tester`에게 **이 계획 파일 경로와 대상만** 넘긴다. 구현 설명은 넘기지 않는다.
    - 대상: `symptomsIn`(키워드 표·정규화·부정 규칙), `FakeVoiceInput`, `RealVoiceInput`(가짜 `SpeechToTextClient`로), `MedicalCardSession.select`, `SymptomVoiceViewModel`, `voiceInputKindOf`(+ `resolveDataMode`)
    - 대상 아님: `PleosSpeechToTextClient`, composable, `AppContainer`, `MainActivity`
  - 기대 동작 (계약 기준): 설계 결정 1(더하기만, 최종 결과만, 한 문장), 2(화면이 사라지면 버림), 3(이벤트 순서와 끝), 4(규칙 1~5와 표), 5(이벤트 표와 시간), 7(구성표), 9(상태 전이)
  - 기존 테스트 파일은 고치지 않는다 (완료 기준 3).
  - tester가 찾은 버그는 메인이 프로덕션 코드에서 `fix:` 커밋으로 따로 고친다. 키워드 오탐·누락은 표를 고치는 `fix(domain):` 커밋으로 낸다.
  - 확인: `./gradlew testDebugUnitTest` 통과. 통과 수를 진행 로그에 적는다.

- [ ] 7. 전체 검증 (`/verify`). 커밋은 없다. 고친 게 있으면 `fix(...)` 커밋으로 낸다. D1~D4에서 모은 문장은 TSV에 더해 `test(domain): 기기에서 받아쓴 문장을 문장 모음에 추가`로 낸다.
  - `node .claude/hooks/build-gate.mjs pipeline` → "검증 파이프라인 통과."
  - `./gradlew assembleRelease` 성공
  - 완료 기준 3의 `git diff`, 완료 기준 4의 Grep, `PleosSpeechToTextClient.kt`의 로그 줄 읽기
  - 되돌리기 확인: `git revert --no-commit <5-2 해시>` → `./gradlew assembleDebug testDebugUnitTest` 통과 → `git revert --abort` (설계 결정 8)
  - `my_avd`: 확인 절차 P1~P8
  - `pleos_connect`: 확인 절차 D1~D9
  - 기기가 없으면 멈추고 사용자에게 Cold boot를 요청한다. 확인하지 못한 것은 그대로 적는다.

- [ ] 8. 문서 맞추기
  - `docs/architecture.md`:
    - "서비스 기능 스택": "음성 → 글자" 행에 "구현함 (010): 정차 중 증상 고르기 화면에서 한 문장 → 키워드 규칙 → 증상 선택에 더함. 주행 중 음성은 B1 뒤". 맨 위 서비스 설명 "운전 중: 음성으로 증상을 말하면…"에 지금 범위를 적는다 (`[사용자 승인 필요]` 1의 답).
    - "패키지 구조": `domain/voice/`, `data/voice/`, `ui/voice/`, `di/VoicePlan.kt`
    - "데이터 모드": 음성 입력 구성표 (설계 결정 7), `SourcePlan`에 넣지 않은 이유
    - 새 절 "음성 입력 빼는 법" (설계 결정 8), "SpeechToText 수명주기" (설계 결정 6)
    - 커밋: `docs(architecture): 음성 증상 입력 구성과 빼는 법 기록`
  - `docs/pleos/sdk-notes.md` "STT `ON_DEVICE` 실행 확인": import 경로, 무음일 때 이벤트(D4), `stop()`·`release()` 뒤(D5), 콜백 스레드, 연속 듣기(D6), 병합 매니페스트(AAR이 더한 권한), `STT_SERVICE` user 10 허용(D8). researcher 답(Q1~Q3)은 출처와 함께 해당 절에. `docs/pleos/emulator-setup.md` "에뮬레이터 마이크 (Mac)"에 새로 본 것이 있으면 → `docs(pleos): STT 래퍼 구현 중 관찰 기록`
  - `README.md`: 시연 방법에 음성 순서 (Pleos 에뮬레이터: 마이크 설정 → [말로 증상 고르기] → 한 문장. 일반 폰: Fake 예시 문장, 시연용 표시) → `docs: README에 음성 입력 시연 방법 추가`
  - 확인: 완료 기준 7

- [ ] 9. 개발 노트 작성 (`docs/notes/`)
  - `docs/notes/xxj15/YYYY-MM-DD-voice-input.md`. 형식은 `docs/notes/README.md` 템플릿.
    - 사용한 API: Pleos SpeechToText 3.3.1 `ON_DEVICE` (`SpeechToText`, `initialize`, `addListener`, `setLocale`, `request`, `stop`, `removeListener`, `release`, `ResultListener`), 권한 `pleos.car.permission.STT_SERVICE`(승인됨), `RECORD_AUDIO`(답대로), AAR이 더한 `INTERNET`
    - 결정: 추가 입력 수단, 정차 중에만, 한 문장·더하기만, 키워드 규칙과 검증 방법, 듣기마다 새 객체, 모드별 구성, 빼는 법
    - 시연 방법 (영상 촬영용): Pleos 에뮬레이터 마이크 설정 + `say` 또는 사람 목소리 + Virtual Console 순서, 일반 폰 Fake 순서. 마이크가 겹칠 때 대처 (문장 사이 쉬기, Fake 모드로 찍기)
    - **기획서 5번 "개발 계획"에 넣을 문장** (기획 담당에게). 예: "정차 중 증상 고르기 화면에서 마이크 버튼을 누르고 한 문장으로 말하면, Pleos SpeechToText SDK(`ON_DEVICE`, 차량 내 엔진, 네트워크 불필요)가 받아쓴 문장을 키워드 규칙으로 증상 버튼에 옮긴다. 사용자는 버튼으로 확인·수정한다. 버튼만으로도 같은 카드를 만들 수 있다. 주행 중 음성 입력은 대회 문의 답에 따라 확장한다." (`[사용자 승인 필요]` 1·2의 답에 맞춰 고친다)
    - 남은 일: 주행 중 음성(B1), STT 사용 허가(B2), 실차 `RECORD_AUDIO`, 오디오 포커스(Q1 답), 하지 않은 있으면 좋음
  - 이 계획을 `docs/plans/completed/`로 옮기고 진행 로그를 갱신한다.
  - 커밋:
    - `docs(notes): voice-input`
    - `docs(plans): 010 음성 증상 입력 완료`
  - 확인: `git status`에 이 작업의 변경이 남지 않았다. 그다음 `/ship`.

### 있으면 좋음 (필수가 끝난 뒤, 단계 9 전에. 12/03 PT 전까지 미뤄도 된다)

- [ ] A. 녹음 예시 문장으로 Real 엔진 시연 (`[사용자 승인 필요]` 2의 C) → `feat(data): 녹음한 예시 문장을 Pleos 음성 엔진에 넣기`
  - `SpeechToTextClient`에 `startWithAudio(pcm: ByteArray, onEvent)`: `initialize` → `addListener` → `setLocale` → `sendAudio` 3,200 bytes(100ms)씩 → `completeAudioSend()` (sdk-notes 관찰 순서, `request()` 없음). `ON_DEVICE`에서만 된다 (문서).
  - 녹음 파일: 팀원 목소리 16 kHz, 16-bit, mono PCM을 `assets/`에. macOS `say` 음성은 배포 라이선스를 확인하지 않았으므로 쓰지 않는다 (출품 유의사항: 제3자 권리).
  - 음성 영역에 작은 [예시 음성 들려주기]와 "녹음된 예시 음성이에요" 표시. Real 음성일 때만.
- [ ] B. 처음 화면에서 바로 말하기 → `feat(ui): 처음 화면에 말로 카드 만들기 추가`
  - [말로 카드 만들기] = 고르기 시작 + 바로 듣기. 처음 화면이 바뀌므로 `ParkedScreen` 미리보기와 P1을 다시 본다.
- [ ] C. 보호자가 말한 문장을 카드에 넣기 → `feat(ui): 진료 카드에 보호자가 말한 내용 추가`
  - 세션에 마지막 인식 문장을 두고 카드에 "보호자가 말한 내용" 줄. `medicalCardOf`·`MedicalCardUiState`가 바뀌므로 완료 기준 3의 목록에서 뺀다. 음성만 빼기가 어려워진다 (설계 결정 8과 충돌. 하려면 사용자에게 먼저 묻는다).
- [ ] D. debug 패널에서 Fake 음성 결과 고르기 → `feat(ui): debug 패널에서 Fake 음성 결과 바꾸기`
  - 예시 문장 / 못 찾음 / 말소리 없음 / 마이크 / 쓸 수 없음. 실패 화면을 기기와 영상에서 볼 수 있다. 패널이 길어지면 가로 화면에서 밀린다 (005·009의 한계).

## 진행 로그

- 2026-11-05: 계획 작성.
  - 브랜치 `feat/voice-input`(dev 기준)에 문서 커밋 2개가 먼저 있다: `979c9dc`(STT `ON_DEVICE`가 키 없이 동작), `284245c`(STT 사용 결정과 마이크 시험 결과).
  - 시작 시점의 코드:
    - 증상 고르기(`SymptomPickerScreen`): 제목·안내 + 12개 버튼(2~4열) + [취소] [카드 만들기]. 음성 없음.
    - `MedicalCardSession`: `startPicking`, `toggle`, `confirm`, `cancelPicking`, `clear`, `goHome`, `openCard`, `startNew`. 여러 개를 한 번에 고르는 함수는 없다.
    - `Symptom` KDoc에 "음성 입력(STT "준비 중") 대신"이 남아 있다.
    - Pleos SpeechToText 의존성·`STT_SERVICE` 권한 없음. 매니페스트에 `uses-permission` 없음.
    - `SourcePlanBoundaryTest`가 `SourcePlan`을 위치 인자로 만들고 `equals`로 비교한다 → `SourcePlan`에 칸을 더하지 않기로 했다 (설계 결정 7).
  - planner는 spike 파일을 읽지 못했다 (위 "관련 문서").
- 2026-11-05: 시작 전 준비.
  - `[사용자 승인 필요]` 답 (모두 권장안): 1 = 정차 중에만, 2 = 시연 모드 차량 기기는 실제 마이크(A) + 녹음 대비책(C, 있으면 좋음 A), 일반 폰은 Fake + 시연용 표시, 3 = `RECORD_AUDIO` 선언하지 않음.
  - `[선행: pleos-docs-researcher]` Q1~Q3은 researcher에게 넘겼다 (단계 5-2 전까지).
  - 시작 시점 단위 테스트: 313개 통과.
  - 단계 0 커밋 `80e13c4`.
- 2026-11-05: 단계 1 `074a70d`, 2-1 `bd5118b`, 2-2 `881d880`.
- 2026-11-05: researcher Q1~Q3 답 — **세 질문의 핵심(포커스 주체, 콜백·호출 스레드, 무음 시간)은 모두 문서에 없다.** 기본안대로 간다 (포커스 요청 안 함, 메인 스레드에서 호출, 시간 값은 D4에서 조정). 새로 확인한 것: `ErrorType`은 늘어날 수 있으니 `when`에 기본 분기를 두라고 권장한다 (설계 결정 5의 "그 밖의 `Error` → UNAVAILABLE"과 맞다). `stop()` 뒤에는 `onEndedRecognition()`이 온다는 문장만 있다. 자세히: sdk-notes "듣는 동안의 오디오 포커스, 콜백 스레드, 끝점 검출".

## 결정 / 배운 점

- 2-1: 절뚝거림 키워드에 "다리를절", "발을절"을 더했다. "다리를 절다가"가 기본안 표(절어·절고)로는 잡히지 않았다.
