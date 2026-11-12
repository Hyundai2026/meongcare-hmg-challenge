# 017 TTS 음성 안내 (병원 가는 길의 사건을 소리로)

- 목표: 병원으로 출발한 뒤 생기는 사건(출발, 병원의 카드 확인, 도착 등)을 Pleos TextToSpeech(`ON_DEVICE`, ko-KR)로 짧게 읽어 주어, 운전자가 화면을 보지 않아도 알 수 있게 한다.
- 완료 기준 (어떻게 검증하나):
  1. `node .claude/hooks/build-gate.mjs pipeline`이 "검증 파이프라인 통과."를 출력한다.
  2. `./gradlew assembleRelease`(Windows PowerShell은 `gradlew.bat assembleRelease`)가 성공한다.
  3. 단위 테스트:
     - 새 테스트가 통과한다: `FakeSpeakerTest`, `TripAnnouncementTextTest`, `TripAnnouncerTest`, `SpeechPlanTest`, `RealSpeakerTest`, 그리고 tester가 추가한 경계 테스트.
     - 기존 테스트(시작 시점 개수를 진행 로그에 적는다)가 그대로 통과한다.
     - `git diff dev --diff-filter=MD --name-only -- app/src/test`의 출력이 비어 있다 (기존 테스트 파일은 추가만).
     - 아래 `git diff dev -- <경로>`의 출력이 비어 있다. 주행 화면·길안내·카드 전송·음성 입력을 건드리지 않았고, 소리 안내만 뺄 수 있다는 뜻이다:
       - `app/src/main/java/com/meongcare/app/ui/` 전체, `MainActivity.kt` (있으면 좋음 A·B를 하면 그 파일만 예외로 진행 로그에 적는다)
       - `domain/guidance/`, `domain/trip/HospitalTrip.kt`, `domain/trip/CaseCard.kt`, `domain/trip/CaseSender.kt`, `domain/voice/`, `domain/card/`
       - `data/guidance/`, `data/voice/`, `data/server/`, `data/hospital/`
       - `di/SourcePlan.kt`, `di/VoicePlan.kt`, `di/TripTiming.kt`, `di/DataMode.kt`
  4. 정적 검사 (Grep):
     - `import ai.pleos.playground.tts`는 `app/src/main/java/com/meongcare/app/data/speech/PleosTextToSpeechClient.kt`에만 있다.
     - `registerApp(`, `setSpeakerType(`, `setAudioZoneId(` 호출이 0건이다 (`ON_DEVICE`만 쓴다. 뒤의 둘은 뜻이 문서에 없다).
     - `app/src/main/java/com/meongcare/app/ui/` 아래에 `import com.meongcare.app.data` 0건, `import com.meongcare.app.di` 0건.
     - `domain/` 아래 `Log.` 0건. `data/speech/`의 `Log.`는 `PleosTextToSpeechClient.kt`에만 있고, 이벤트 이름·오류 이름·걸린 시간만 남긴다 (단계 7에서 파일을 읽어 확인: 말할 문장이 `Log.` 인자에 없다).
     - 병합 매니페스트(`app/build/intermediates/` 아래 `AndroidManifest.xml`)의 `pleos.car.permission`은 `NAVI_ROUTE`, `NAVI_ROUTE_SEARCH`, `STT_SERVICE`, `TTS_SERVICE` 네 개뿐이다. `RECORD_AUDIO`는 없다.
  5. Pleos 에뮬레이터 (`pleos_connect`, Cold boot): 아래 "확인 절차"의 D1~D9. **소리는 사용자가 듣고 알려 준다** (에이전트는 소리를 듣지 못한다). `adb logcat -d -t 500 *:E`에 `com.meongcare.app` 크래시와 `SecurityException`이 없다.
  6. 일반 폰 에뮬레이터 (`my_avd`): 아래 "확인 절차"의 F1~F3.
  7. 문서:
     - `docs/architecture.md`: "서비스 기능 스택"의 "음성 안내" 행이 구현 사실과 맞다. "패키지 구조"에 `domain/speech/`, `data/speech/`, `domain/trip/TripAnnouncer.kt`, `domain/trip/TripAnnouncement.kt`, `di/SpeechPlan.kt`가 있다. "데이터 모드"에 소리 안내 표가 있다. 새 절 "TextToSpeech 수명주기 (017)"와 "소리 안내 빼는 법 (017)"이 있다.
     - `docs/pleos/sdk-notes.md`에 새 절 "TTS `ON_DEVICE` 실행 확인 (관찰)"이 있고, "017·018 음성 안내·음성 대화 관련" 목록에서 017이 본 항목이 체크돼 있다.
     - `docs/pleos/ux-guidelines.md` "우리 TTS 안내가 내비 음성과 겹칠 때"에 관찰 결과가 있다.
     - `docs/product/feature-facts.md`의 TextToSpeech 관련 줄(3번 표 "음성으로 안내한다", 4번 표 TextToSpeech, 선언 권한 수)이 확인한 사실과 맞다.
     - `README.md`의 Pleos 에뮬레이터 시연 방법에 소리 안내가 있다.
- 브랜치: `feat/tts-guidance` (최신 dev `d74af7f`에서 땄다)
- 관련 문서:
  - AGENTS.md (아키텍처 규칙: SDK는 `data/` 래퍼에서만, 래퍼마다 Fake, 콜백→suspend/Flow, 데이터 모드, release는 시연 모드. 규칙 1·2·3·7)
  - `docs/pleos/sdk-notes.md`: "TextToSpeech SDK (v3.3.1)", **"AAR에서 직접 확인한 것 (2026-11-12) [aar-tts]"**(기본 usage `USAGE_MEDIA`, `setAudioAttributes`, SDK 안 대기열, `EventListener` 기본 구현 없음), "TTS 문서 조사 (017 선행)" T1~T7, 미확인 "017·018 음성 안내·음성 대화 관련 (2026-11-12 조사)", "STT/TTS 사용 조건 정리"(공모전에서 써도 된다), "앱 구현(010)에서 본 것"(`stop()` 바로 뒤 `release()` 버그)
  - `docs/pleos/ux-guidelines.md`: "오디오 포커스", "우리 TTS 안내가 내비 음성과 겹칠 때", "TTS·음성 안내 선행 묵음", "음성 인터랙션 (UI 디자인 가이드)", "주행 안전 설계 원칙"
  - `docs/architecture.md`: "서비스 기능 스택"의 "음성 안내" 행, "사용하기로 한 Pleos 권한"(`TTS_SERVICE` 2026-11-03 팀 승인), "데이터 모드", "SpeechToText 수명주기 (010)", "음성 입력 빼는 법 (010)"
  - 선례: `docs/plans/completed/010-voice-input.md`, `docs/notes/xxj15/2026-11-05-voice-input.md` (STT 래퍼, `di/VoicePlan.kt`, Fake, 빼는 법, spike 브랜치 `spike/stt-ondevice`)
  - 사건이 생기는 곳: `domain/trip/HospitalTrip.kt` (`trip: StateFlow<TripState?>`, `CardDelivery.Sent.acknowledgedAt`), `domain/guidance/GuidanceState.kt` (`Guiding`/`Arrived`), `di/AppContainer.kt`, `di/TripTiming.kt`
  - `docs/product/feature-facts.md` (11/15 기획서의 사실 표)

## 범위

- **필수** (약 1.5일. 11/13~14 목표): 단계 0~9.
  - 11/15 제출물에 주는 것:
    - 기획서(필수 제출물): 단계 8에서 `feature-facts.md`에 확인한 사실만 넣는다. 기획서 5번 "개발 계획"에 쓸 문장을 노트에 남긴다.
    - 시연 영상: "병원이 카드를 확인하자 차가 소리로 알려 준다" 장면 (확인 절차 D3·D4). 영상 촬영 전에 단계 5-2까지 끝낸다.
    - APK: release는 시연 모드라 Pleos 에뮬레이터에서 진짜 소리가 난다. 제출 APK 만들기(`/release-apk`)는 이 계획 밖이다.
- **있으면 좋음**: 단계 A~C. 필수가 끝난 뒤, 단계 9 전에. 12/03 PT까지 미뤄도 된다.
- **이번 범위 밖**:
  - 018 (길안내 중 주행 화면에서 말로 증상 더하기). 017은 018이 쓸 "말하기" 부품만 만든다.
  - 019 (연결 유지, 웹 연결 관리).
  - 남은 시간·도착 예정·병원 대기 시간이 바뀔 때의 안내 (내비 TBT 음성과 겹치고 잦다).
  - 화면 변경. 주행 화면(`DrivingScreen`)에 아무것도 더하지 않는다.
  - `HYBRID`/`SERVER` 모드, 앱 등록(Client ID/Secret).
  - `setSpeakerType`, `setAudioZoneId` (받는 값의 뜻이 문서에 없다).
  - 앱이 직접 선행 묵음 넣기 (SDK가 합성한 오디오를 앱에 돌려주지 않아 할 방법이 없다, ux-guidelines "TTS·음성 안내 선행 묵음").
  - 안드로이드 기본 TTS(`android.speech.tts.TextToSpeech`). `[사용자 승인 필요]` U4의 다른 안을 고를 때만.

## 설계 결정 (planner 판단과 근거)

아래 코드 블록은 기본안이다. 이름과 세부는 구현하면서 바꿔도 된다. 바꾸면 이 절을 고치고 "결정 / 배운 점"에 이유를 남긴다. spike(단계 1) 결과로 바뀔 수 있는 곳은 "(S번호)"로 표시했다.

### 1. 소리로 알릴 사건과 문장 (`[사용자 승인 필요]` U1·U2의 기본안)

| # | 사건 | 언제 | 문장 (기본안) | 몇 번 |
|---|---|---|---|---|
| E1 | 출발 | [출발] 또는 [카드 없이 길안내만]을 누른 직후. 카드를 보내는 중이면 결과를 최대 5초 기다린다 | 카드를 서버로 보냄: "○○까지 안내를 시작해요. 진료 카드를 보냈어요." / 그 밖(카드 없이 출발, 근처 검색 병원, 시연용 병원, 5초 안에 결과 없음, 실패): "○○까지 안내를 시작해요." | 안내마다 1번 |
| E2 | 카드 보냄 (늦게) | E1에 "보냈어요"가 들어가지 않았는데, 그 뒤 서버로 보내졌다 (다시 보내기 성공 포함) | "진료 카드를 ○○에 보냈어요." | 1번 |
| E3 | 카드 못 보냄 | 이 안내에서 처음 실패했다 | "진료 카드를 보내지 못했어요. 정차하면 다시 보낼 수 있어요." | 1번 |
| E4 | 병원이 카드 확인 | `CardDelivery.Sent.acknowledgedAt`이 처음 생겼다 (013 U1) | "○○에서 진료 카드를 확인했어요." | 1번 |
| E5 | 도착 | 길안내가 안내 중(`Guiding`)을 거친 뒤 도착(`Arrived`)이 됐다 | "○○에 도착했어요. 진료 카드를 접수처에 보여 주세요." | 1번 |

- "○○"는 고른 병원 이름(`TripState.candidate.hospital.name`)이다. 시연용 병원은 이름이 "시연용 바른 동물병원"처럼 "시연용"으로 시작한다 (`SampleHospitals`). 그래서 소리로도 시연용임이 들린다.
- 병원 이름 뒤에는 받침과 상관없는 말만 붙인다 ("까지", "에", "에서"). "이/가", "을/를"을 쓰지 않는다. 병원 이름은 서버에서 오므로 끝 글자를 미리 알 수 없다.
- 반려견 이름은 넣지 않는다. 문장이 짧아지고, 차 밖으로 들리는 소리에 반려견 정보가 덜 실린다.
- 말하지 않는 것: 남은 시간·도착 예정 변화, 병원 대기 시간, "경로를 찾는 중", 안내 취소(사용자가 직접 한 일이다. 내비 "안내 종료"는 내비가 알린다), 내비 오류, 확인 읽기 실패.
- 시연용 병원(내장 병원, `CardDelivery.Sent(toServer = false)`)은 실제로 보내지 않았으므로 "보냈어요"를 말하지 않는다 (U2 기본안). 화면도 "시연용: 실제 병원에는 보내지 않았어요"라고 한다.
- 문장은 `domain/trip/TripAnnouncement.kt`의 Kotlin 코드에 둔다 (`strings.xml`이 아니다). 이유: 문장이 계약이라 JVM 테스트로 그대로 확인하고, 소리 언어가 ko-KR 하나라 번역할 일이 없다. 기획 담당이 고칠 때는 이 파일의 표만 고친다.

### 2. 언제 말하나 (`TripAnnouncer` 규칙. tester 계약 기준)

- R1. 출발: 고른 병원이 생기면(`trip`이 null → 값) 그 안내에서 한 번. 카드가 `Sending`이면 `Sending`이 아니게 될 때까지 최대 `departureWait`(5초) 기다린다. 그 안에 `Sent(toServer = true)`면 E1 "보냈어요"를 붙인다.
- R2. 늦게 보냄: E1에 "보냈어요"가 없었고, 그 뒤 처음 `Sent(toServer = true)`가 되면 E2 한 번.
- R3. 못 보냄: 이 안내에서 처음 `Failed`가 되면 E3 한 번. 출발 대기 중에 실패해도 E1 뒤에 이어서 말한다.
- R4. 병원 확인: `acknowledgedAt`이 처음 null이 아니게 되면 E4 한 번.
- R5. 도착: 이 안내에서 `Guiding`을 본 뒤 `Arrived`가 되면 E5 한 번. 도착 뒤에는 E2~E4를 말하지 않는다 (확인 읽기도 도착하면 멈춘다, `HospitalTrip`).
- R6. 고른 병원이 비면(안내 취소, 도착 화면 [닫기]) 그 안내의 남은 말을 버리고, 말하는 중이면 멈춘다. 취소는 말하지 않는다.
- R7. 사건이 생긴 순서대로 하나씩 말한다. 앞 말이 끝나야 다음 말을 시작한다.
- R8. 말하기가 실패(`SpeakResult.Failed`)하거나 멈춰도(`Stopped`) 로그만 남기고 다시 말하지 않는다. 다음 사건은 그대로 말한다.
- R9. 주행 중이든 정차 중이든 말한다. 앱이 화면에 없어도(내비 앱이 앞) 말한다: `AppContainer`의 앱 범위(`applicationScope`)에서 돈다. 뒤에 있을 때 실제로 소리가 나는지는 S11.
- R10. 다음 안내는 사건을 처음부터 다시 센다.
- R11. 남은 시간(`startMinutes`, `Guiding.remainingTime`)이 바뀌어도 말하지 않는다.
- R12. 로그: 사건 종류와 결과만. 예: "소리 안내: 출발(카드 보냄) → 끝까지", "소리 안내: 도착 → 실패(AUDIO)". 병원 이름·문장은 넣지 않는다.

`HospitalTrip`은 고치지 않는다. 공개 상태(`trip`)와 `GuidanceSource.state`만 읽는다 (완료 기준 3). 두 상태 모두 hot `StateFlow`라 구독을 하나 더해도 연결이 늘지 않는다 (Real 길안내 3종 모두 `StateFlow`, `RealGuidanceSource`는 `flowOf(Idle)`).

```kotlin
// domain/trip/TripAnnouncement.kt (Android·Pleos를 모른다)
sealed interface TripAnnouncement {
    data class Departed(val hospitalName: String, val cardSent: Boolean) : TripAnnouncement
    data class CardSent(val hospitalName: String) : TripAnnouncement
    data object CardFailed : TripAnnouncement
    data class Acknowledged(val hospitalName: String) : TripAnnouncement
    data class Arrived(val hospitalName: String) : TripAnnouncement
}
/** 소리로 읽을 문장 (설계 결정 1의 표) */
fun TripAnnouncement.spokenText(): String
/** 로그 이름. 병원 이름 없음: "출발(카드 보냄)", "출발", "카드 보냄", "카드 못 보냄", "병원 확인", "도착" */
val TripAnnouncement.logName: String

// domain/trip/TripAnnouncer.kt
class TripAnnouncer(
    private val trip: StateFlow<TripState?>,   // HospitalTrip.trip
    private val guidance: Flow<GuidanceState>,  // GuidanceSource.state
    private val speaker: Speaker,
    private val scope: CoroutineScope,          // 앱 범위. 화면이 없어도 말한다 (R9)
    private val log: (String) -> Unit = {},
    private val departureWait: Duration = 5.seconds,
) {
    /** AppContainer init에서 한 번 부른다 */
    fun start()
}
```

- 구현 힌트: `trip.map { it?.candidate }.distinctUntilChanged().collectLatest { ... }`로 안내 하나마다 코루틴 하나. 그 안에서 사건을 `Channel`에 넣고, 하나씩 꺼내 `speaker.speak()`한다. 안내가 끝나면(null) `collectLatest`가 둘 다 취소한다 (R6).

### 3. 말하기 부품: 도메인 인터페이스 (018이 그대로 쓴다)

```kotlin
// domain/speech/Speaker.kt (Android·Pleos를 모른다)
/** 짧은 문장을 소리로 읽는다. 구현: Pleos TextToSpeech ON_DEVICE(Real), 소리 없이 시간만 보내는 Fake. */
interface Speaker {
    /**
     * [text]를 읽고, 끝나면 결과를 돌려준다.
     * - 한 번에 하나만 읽는다. 앞 문장을 읽는 중에 부르면 앞 문장이 끝난 뒤 읽는다 (부른 순서대로).
     * - 부른 코루틴을 취소하면 읽기를 멈춘다 (기다리는 중이었으면 읽지 않는다). 이때는 결과 대신 취소 예외다.
     * - 018: [SpeakResult.Done]을 받은 뒤 듣기(STT)를 시작한다.
     */
    suspend fun speak(text: String): SpeakResult
}

sealed interface SpeakResult {
    /** 끝까지 읽었다 (SDK `onDone`) */
    data object Done : SpeakResult
    /** 다 읽기 전에 멈췄다 (SDK `onStop`). 부른 쪽이 취소한 경우는 여기로 오지 않는다 */
    data object Stopped : SpeakResult
    data class Failed(val reason: SpeakFailure) : SpeakResult
}

enum class SpeakFailure {
    /** 소리 출력 문제 (`AUDIO_ERROR`) */
    AUDIO,
    /** 이 기기·지금 상태에서 쓸 수 없다 (초기화 실패, 엔진·언어·권한 문제, 시간 초과 등) */
    UNAVAILABLE,
}
```

- 사용자가 요청한 모양("말하고, 끝났는지(onDone/onStop/오류) 알 수 있는")이다. `suspend` 하나로 "말하고 → 끝나면 듣기"가 한 줄이 된다.
- 010의 `VoiceInput`은 부분 결과가 계속 와서 `Flow`였다. 말하기는 끝 결과 하나뿐이라 `suspend`로 둔다. 018에서 "읽기 시작" 이벤트가 필요해지면 그때 더한다.
- "한 번에 하나"는 `Speaker` 구현이 보장한다 (`Mutex`). SDK도 안에 대기열이 있지만([aar-tts], 추정) `onDone`에 발화 id가 없어서 여러 개를 맡기면 어느 `onDone`이 누구 것인지 모른다.

### 4. Fake

- `data/speech/FakeSpeaker.kt`: 소리를 내지 않는다. `400ms + 글자당 120ms`를 기다린 뒤 `Done`. `Mutex`로 하나씩. 취소하면 바로 멈춘다. 로그를 남기지 않는다 (안내자가 남긴다).
- 쓰임: GPU 없는 PC, 일반 폰 에뮬레이터(시연 모드 포함), 단위 테스트. 화면이 없으므로 시연용 표시도 없다.

### 5. Real: 판단 로직과 SDK 경계를 나눈다 (010 STT와 같은 나눔)

```kotlin
// data/speech/TextToSpeechClient.kt (Pleos 타입을 밖으로 내보내지 않는다)
sealed interface TtsEvent {
    data object Ready : TtsEvent                         // onReady
    data object Started : TtsEvent                       // onStart
    data object Done : TtsEvent                          // onDone
    data object Stopped : TtsEvent                       // onStop
    data class Error(val name: String) : TtsEvent        // onError(ErrorType).name
    data class InitFailed(val name: String) : TtsEvent   // OnInitializeListener.onFailure(ErrorType).name
}

fun interface TextToSpeechClient {
    /**
     * 새 TextToSpeech를 만들어 초기화하고, 성공하면 리스너 등록 → 한국어 → 재생 속성 → speak(text).
     * 돌려받은 것을 close()하면 (읽는 중이면 stop() → 멈춤을 기다림) → removeEventListener() → release(). 여러 번 불러도 된다.
     */
    fun speak(text: String, onEvent: (TtsEvent) -> Unit): AutoCloseable
}

// data/speech/RealSpeaker.kt (Pleos를 import하지 않는다. JVM 테스트 대상)
class RealSpeaker(
    private val client: TextToSpeechClient,
    private val initTimeout: Duration = 3.seconds,          // (S3)
    private val baseSpeakTime: Duration = 5.seconds,        // (S5)
    private val perCharSpeakTime: Duration = 250.milliseconds,
) : Speaker

// data/speech/PleosTextToSpeechClient.kt: ai.pleos.playground.tts를 import하는 유일한 파일
class PleosTextToSpeechClient(context: Context) : TextToSpeechClient
```

- `RealSpeaker.speak()`: `Mutex` 안에서 `client.speak { channel.trySend(it) }` → 끝 이벤트를 기다림 → `finally { close() }`.

  | 받은 것 | 결과 |
  |---|---|
  | `Ready`, `Started` | 없음 (응답으로 친다) |
  | `Done` | `Done` → 닫는다 |
  | `Stopped` | `Stopped` → 닫는다 |
  | `Error("AUDIO_ERROR")` | `Failed(AUDIO)` |
  | 그 밖의 `Error`, `InitFailed` | `Failed(UNAVAILABLE)` (오류 종류가 늘 수 있어 기본 분기, Pleos 문서 권장) |
  | `initTimeout` 안에 `Ready`·`Started`·`Done`이 하나도 없음 | `Failed(UNAVAILABLE)` (음성 서비스가 없는 기기에서 콜백이 안 올 때) |
  | 시작 뒤 `baseSpeakTime + perCharSpeakTime × 글자 수`가 지남 | `Failed(UNAVAILABLE)` → 닫는다 |
  | 끝난 뒤 오는 이벤트 | 무시 |
  | `client.speak`가 예외 | `Failed(UNAVAILABLE)` |
  | 부른 코루틴 취소 | 닫는다 (한 번만) |

  - 시간은 이벤트마다 다시 재지 않는다 (010 tester가 찾은 버그와 같은 함정).
- `PleosTextToSpeechClient.speak()` 기본안 (S3·S8로 고친다):
  - `TextToSpeech(appContext, Mode.ON_DEVICE)` → `initialize(OnInitializeListener)` → `onSuccess`에서 `addEventListener(listener)` → `setLocale(Locale.KOREA)` → `setAudioAttributes(AudioAttributes.Builder().setUsage(<S8>).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())` → `speak(text)` (`isFinal`은 기본값 `true`). `onFailure`면 `InitFailed`.
  - import 경로는 sdk-notes에 있다: `ai.pleos.playground.tts.TextToSpeech`, `ai.pleos.playground.tts.constant.Mode`, `...constant.ErrorType`, `ai.pleos.playground.tts.listener.EventListener`, `...listener.OnInitializeListener`.
  - `EventListener`는 기본 구현이 없다([aar-tts]). 메서드 6개를 모두 구현한다. `onUpdatedRms`는 넘기지 않는다.
  - SDK 호출은 메인 스레드에서 한다 (`Handler(Looper.getMainLooper())`). 안내자는 `Dispatchers.Default`에서 부른다. 근거: STT는 메인에서 불렀고 콜백도 메인으로 왔다(010 관찰). 문서에는 스레드 제약이 없다. (S3)
  - `close()`:
    - 초기화 중이면: 메인에서 `release()`.
    - 읽는 중이면: `stop()` → `onStop`이 오거나 1초가 지나면 `removeEventListener()` → `release()`. **`stop()` 바로 뒤 `release()`하지 않는다** (010 STT에서 `stop()`이 서비스에 닿기 전에 연결이 끊긴 버그. TTS도 같은지 S7).
    - 끝난 뒤(`onDone`·`onStop`·`onError`)면: 바로 `removeEventListener()` → `release()`. (S5에서 끝소리가 잘리는지 본다)
  - `registerApp`, `setSpeakerType`, `setAudioZoneId`는 부르지 않는다.
  - 로그(태그 `PleosTts`): 이벤트 이름, 오류 이름, `initialize` 걸린 ms. **문장은 남기지 않는다** (병원 이름).
  - 첫 음절이 잘리고 앞에 쉼표·공백을 붙이는 우회가 들으면(S4), 그 처리는 이 파일 안에서만 한다 (도메인 문장은 그대로).

### 6. TextToSpeech 수명주기 (기본안: 말하기 한 번 = 객체 하나)

- `speak()` 한 번마다 `TextToSpeech`를 새로 만들고, 끝나면 `release()`하고 버린다. 문서: "release 후 다시 쓰려면 새 객체를 만든다." 010 STT, 006 `Car` 연결과 같은 방식이라 화면 전환·실패 복구 상태가 없다.
- "`Vehicle`의 initialize/release는 `VehicleConnection` 한 곳에서" 규칙은 Vehicle SDK 규칙이다. TTS는 이 밖이고, `RealSpeaker`가 한 곳에서 관리한다.
- **갈림길 (S3·S4)**: `initialize`가 오래 걸리거나(대략 0.5초 넘음) 새 객체의 첫 문장만 앞이 잘리면, "안내 하나 동안(출발 ~ 닫기·취소) 객체 하나"로 바꾼다. 그때는 `TextToSpeechClient`를 `open(): TtsSession`(`speak`, `close`) 모양으로 고치고 이 절과 단계 5를 먼저 고친다.

### 7. 데이터 모드별 구성: `di/SpeechPlan.kt` (`SourcePlan`·`VoicePlan`은 그대로)

```kotlin
enum class SpeechOutputKind { NONE, FAKE, REAL }

/** 소리 안내를 끌 때 false (App Market 심사 등에서 TTS가 허용되지 않을 때). APK에서 빼는 법은 설계 결정 8 */
const val SPEECH_OUTPUT_ENABLED = true

fun speechOutputKindOf(mode: DataMode, isAutomotive: Boolean, enabled: Boolean = SPEECH_OUTPUT_ENABLED): SpeechOutputKind
```

| 모드 | 차량 기기 (`FEATURE_AUTOMOTIVE`) | 소리 안내 |
|---|---|---|
| Fake | 상관없음 | FAKE (소리 없음) |
| 시연 (release) | 예 (Pleos 에뮬레이터, 실차) | **REAL** (`ON_DEVICE`) |
| 시연 (release) | 아니오 (일반 폰) | FAKE (음성 서비스가 없다) |
| Real | 예 | REAL |
| Real | 아니오 | NONE |
| 스위치(`SPEECH_OUTPUT_ENABLED`) 꺼짐 | 모두 | NONE |

- 010 `VoicePlan` 표와 같다. 따로 둔 이유도 같다 (`SourcePlanBoundaryTest`가 `SourcePlan`을 위치 인자로 만들고 `equals`로 비교한다. 소리만 빼기 쉽다).
- `AppContainer`:
  - `val speechOutputKind = speechOutputKindOf(dataMode, isAutomotive)`
  - `val speaker: Speaker? = when (kind) { NONE -> null; FAKE -> FakeSpeaker(); REAL -> RealSpeaker(PleosTextToSpeechClient(context)) }`
    - 단계 4에서는 `REAL -> null`(임시)로 두고 단계 5-2에서 바꾼다 (010과 같다).
  - `init`: `speaker`가 있으면 `TripAnnouncer(hospitalTrip.trip, guidanceSource.state, speaker, applicationScope, log = { Log.i("TripAnnouncer", it) }).start()`.
  - 018이 같은 `speaker`를 쓰도록 `val`로 연다.

### 8. 소리 안내 빼는 법 (010 "음성 입력 빼는 법"과 같은 구조)

- **소리만 끄기**: `di/SpeechPlan.kt`의 `SPEECH_OUTPUT_ENABLED = false` 한 줄. 모든 모드가 NONE → 안내자가 시작되지 않는다. 화면은 원래 바뀐 것이 없다. SDK와 권한은 APK에 남는다.
- **APK에서 완전히 빼기**: 위 한 줄 + 단계 5-2 커밋을 `git revert`.
  - 그래서 단계 5-2 커밋에는 이 다섯 가지만 넣는다: `gradle/libs.versions.toml`, `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`(`TTS_SERVICE`), `data/speech/PleosTextToSpeechClient.kt`, `AppContainer`의 REAL 한 줄. 이후 커밋이 이 줄들을 되도록 건드리지 않게 한다. 건드리면 그 커밋도 되돌리기 목록에 적는다.
  - 단계 7에서 확인한다: `git revert --no-commit <5-2 해시>` → `./gradlew assembleDebug testDebugUnitTest` 통과 → `git revert --abort`.

### 9. 주행 중 안전 (AGENTS.md 규칙 3)

- 화면을 바꾸지 않는다. 주행 화면은 지금 그대로다 (큰 남은 분, 목적지, 진행 막대, 병원 확인 한 줄, 버튼 없음). 완료 기준 3의 `git diff`로 확인한다.
- 소리는 운전자가 아무것도 누르지 않아도 나온다. 주행 중 조작이 늘지 않는다.
- 문장은 두 문장 이하, 병원 이름을 빼고 약 30자 이하. 한 안내에 많아야 다섯 번(E1~E5). 잦은 안내(남은 시간·대기 시간)는 없다.
- 소리 끄기 같은 조작이 생기면 정차 화면에만 둔다 (U3, 있으면 좋음 A).

### 10. 권한, 매니페스트, 개인정보

- `pleos.car.permission.TTS_SERVICE`: 2026-11-03 팀 승인 목록에 있다 (새 권한이 아니다). 보호 수준은 문서에 없다 → S2 (`STT_SERVICE`는 `normal`이었다). 단계 5-2에서 선언한다.
- AAR이 더하는 것([aar-tts]): `INTERNET`(앱이 이미 선언), `<queries>` `SPEECH_START`(STT와 같다), 음성 서비스 최소 버전 `4.1.29`. 지우지 않는다.
- `RECORD_AUDIO`는 필요 없다 (말하기만 한다).
- 로그: 우리 앱 로그에는 사건 종류·이벤트 이름·결과만 남는다 (R12, 설계 결정 5). 음성 서비스(`ai.pleos.playground.caas`) 자체 로그에 문장이 찍히는지는 S13에서 보고 기록만 한다 (우리가 막을 수 없다).

## Spike: Pleos 에뮬레이터 확인 (단계 1)

문서에 없어 실행해서 봐야 하는 것을 먼저 본다. 결과로 설계 결정 5·6의 갈림길을 정한다.

- 장소: 이 PC (Windows, RTX 4070 Laptop), AVD `pleos_connect`. 에이전트는 에뮬레이터를 직접 켜지 않는다 (`/verify` 규칙). 사용자에게 Cold boot를 부탁한다:

  ```
  /c/Android/emulator/emulator.exe -avd pleos_connect -no-snapshot-load -no-snapshot-save -gpu host
  ```

  - PC 스피커(또는 이어폰)를 켠다. **에이전트는 소리를 듣지 못한다.** "(귀)" 표시가 있는 항목은 사용자가 듣고 알려 준다. 에이전트는 로그와 `adb shell dumpsys audio`로 재생·포커스를 본다.
  - Windows에서 에뮬레이터 소리가 PC로 나오는지 팀 문서에 없다 (Mac은 마이크 입력만 기록함). 안 나오면 S1 전에 그것부터 본다.
- 실험 코드: 로컬 브랜치 `spike/tts-ondevice` (`feat/tts-guidance`에서 딴다. **dev에 합치지 않고 push하지 않는다**. 018 spike에 다시 쓸 수 있으니 017이 끝나도 지우지 않는다).
  - TTS 의존성과 `TTS_SERVICE` 권한, debug 전용 `app/src/debug/java/com/meongcare/app/TtsProbeActivity.kt` + debug 매니페스트 등록(adb로 띄우게 exported).
  - 띄우기: `adb shell am start -n com.meongcare.app/.TtsProbeActivity`
  - 버튼: [새 객체로 짧은 문장], [같은 객체로 한 번 더], [말하는 중 한 번 더 speak], [말하는 중 stop], [stop 직후 release], [말 안 할 때 stop], [stop 직후 speak], [긴 문장 (200자 넘음)], [앞에 ", " 붙여 말하기], usage 고르기(부르지 않음 = 기본 MEDIA / `USAGE_ASSISTANCE_NAVIGATION_GUIDANCE` / `USAGE_ASSISTANT`), `setAudioAttributes`를 initialize 앞/뒤에 부르기, [5초 뒤 말하기].
  - 로그 태그 `TtsProbe`: 콜백 이름, `SystemClock.elapsedRealtime()` ms, 스레드 이름. 문장은 예시 문장만 쓴다 ("시연용 바른 동물병원까지 안내를 시작해요. 진료 카드를 보냈어요.").

### 확인 목록

| # | 볼 것 | 보는 법 | 정하는 것 |
|---|---|---|---|
| S1 | `ON_DEVICE` 한국어가 Client ID/Secret 없이 소리 나는가 | (귀) + `onStart`·`onDone` 로그 + `dumpsys audio`의 재생 목록 | 계속할지 (아래 "갈림길") |
| S2 | 에뮬레이터 음성 서비스 버전이 AAR 최소 `4.1.29` 이상인가. `TTS_SERVICE`의 보호 수준, 설치 뒤 허용(user 10) | `adb shell dumpsys package ai.pleos.playground.caas`, `adb shell dumpsys package com.meongcare.app` | 권한 처리 (`normal`이면 선언만) |
| S3 | `initialize` 걸린 시간. 콜백 순서·횟수 (`onReady`가 초기화 뒤 한 번인지 `speak()`마다인지, `onStart` → `onDone`). 콜백 스레드. 메인이 아닌 스레드에서 SDK를 불러도 되는가 | 로그 | `RealSpeaker` 표, `initTimeout`, 호출 스레드, `onReady`를 기다렸다 `speak()`할지 |
| S4 | 첫 음절 잘림: 새 객체의 첫 문장 / 같은 객체의 두 번째 문장 / 앞에 ", " 붙임 | (귀) | 객체 수명 (설계 결정 6), 앞 쉼표 우회 |
| S5 | `onDone`이 소리가 다 끝난 뒤 오는가. `onDone` 바로 뒤 `release()`해도 끝소리가 안 잘리는가. 30자 문장을 읽는 데 걸린 시간 | (귀) + `onStart`→`onDone` 시간 | `close` 순서, 최대 읽기 시간 값 |
| S6 | 말하는 중 다시 `speak()`: 줄을 서는가, 앞 문장을 끊는가, 오류인가. `onDone`이 몇 번 오는가 | (귀) + 로그 | 기록만 (우리는 `Mutex`로 하나씩 맡긴다) |
| S7 | 말하는 중 `stop()`: `onStop`만 오는가(`onDone`은 안 오는가). `stop()` 직후 `release()`하면 소리가 멈추는가 (STT처럼 서비스가 계속하지 않는가). 말하지 않을 때 `stop()`. `stop()` 직후 `speak()` | (귀) + 로그 + `dumpsys audio` | `close` 순서 |
| S8 | usage 세 가지 (기본 `USAGE_MEDIA` / `USAGE_ASSISTANCE_NAVIGATION_GUIDANCE` / `USAGE_ASSISTANT`): 들리는가. `dumpsys audio`의 재생 usage가 바뀌는가 (`setAudioAttributes`가 먹는가, initialize 앞·뒤 어디서 불러야 하는가). 포커스 스택에 누가(음성 서비스·우리 앱·없음) 무엇으로 있는가 | [5초 뒤 말하기] 동안 `adb shell dumpsys audio` + (귀) | usage 값, 단계 5-3을 할지 |
| S9 | (에뮬레이터에서 음악을 틀 수 있으면: Driving view 미디어, 앱 목록) 음악 중 말하기: 음악이 줄었다 돌아오는가, 멈춘 채인가 | (귀) | usage |
| S10 | 내비가 경로를 시작할 때 말하는가, [출발]에서 몇 초 뒤인가. 내비 음성 중 우리 말하기: 둘 다 들리는가, 한쪽이 줄거나 끊기는가, 우리 쪽이 `AUDIO_ERROR`인가 | 지금 dev 앱(debug "시연 (주행 Real)")으로 병원 [출발] → 시간 재기. probe [5초 뒤 말하기]로 내비 음성에 겹치기. (귀) + 로그 | 출발 안내 시점, usage |
| S11 | 우리 앱이 뒤에 있을 때(내비 앱이 앞) 소리가 나는가 | probe [5초 뒤 말하기] → GNB의 내비로 우리 앱을 가린다 | R9가 쓸모 있는지. 안 나면 한계로 기록 |
| S12 | 200자 넘는 문장을 끝까지 읽는가, 오류가 있는가 | (귀) + 로그 | 기록만 (우리 문장은 짧다) |
| S13 | 음성 서비스 로그에 읽은 문장이 찍히는가 | `adb logcat -d`에서 예시 문장의 낱말 검색 | 기록만. 찍히면 feature-facts "개인정보" 줄에 조건으로 적는다 |
| S14 | (있으면 좋음, 018용) TTS `onDone` 직후 STT `request()`가 바로 되는가, TTS가 말하는 중 STT를 켜면 받아쓰는가 | probe에 STT 버튼 (`PleosSpeechToTextClient` 재사용) | 018 계획 |

- AAR로 보려던 것(sdk-notes "017·018" 목록의 "AAR(`javap`)로 볼 것" 4개)은 [aar-tts]에 이미 답이 있다 (`speak(String, Boolean)` 기본값, `setAudioAttributes(AudioAttributes)`, `EventListener` 기본 구현 없음, 매니페스트). 단계 1-3에서 체크만 한다.

### usage 고르는 규칙 (S8~S10)

1. 들려야 한다.
2. 사용자 음악을 멈춘 채로 두지 않는다 (줄었다가 돌아오는 것은 된다).
3. 내비 음성을 끊지 않는다.

- 셋을 지키는 것 중 `USAGE_ASSISTANCE_NAVIGATION_GUIDANCE` → `USAGE_ASSISTANT` 순으로 고른다.
  - 앞의 것이 먼저인 이유: 우리 안내가 길안내 사건이고, 오디오 포커스 가이드에서 NAVIGATION은 음악과 C(음악 duck + 내비 재생)로 공존하는 것이 기본이다 (ux-guidelines "오디오 포커스").
  - 기본값 `USAGE_MEDIA`는 음악과 같은 종류라 2를 어길 수 있어 마지막이다 ([aar-tts]: 그대로 두면 음악과 같은 미디어로 재생된다).
- 음악을 틀 수 없어 2를 못 보면 `USAGE_ASSISTANCE_NAVIGATION_GUIDANCE`로 하고 "음악과 겹침 확인 못 함"으로 적는다.
- 말하는 동안 포커스 스택에 아무도 없으면(음성 서비스도 우리 앱도 포커스를 잡지 않음) 단계 5-3을 한다: 우리 앱이 고른 usage로 `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK`을 요청하고, 끝나면(`Done`·`Stopped`·`Failed`·취소) 반납한다. 근거: 오디오 포커스 가이드 "각 오디오 스트림은 재생 시작 전 `AudioAttributes` + `AudioFocusRequest`로 포커스를 요청해야 합니다." (Android 표준 `AudioManager`라 Pleos API 추측이 아니다. 그래도 `data/speech/` 안에만 둔다)

### 출발 안내 시점 (S10)

- 기본안: [출발] 직후 (카드 결과를 최대 5초 기다림). Pleos 에뮬레이터에서 내비가 경로를 계산하고 안내를 시작하기까지 10~20초 걸렸다 (architecture.md "데이터 모드"의 차량 내비, 010 길안내 관찰). 그래서 우리 출발 안내가 내비의 경로 시작 음성보다 먼저 끝날 것으로 본다. 실차에서는 더 빠를 수 있다.
- 겹쳐서 알아듣기 어려우면: 출발 안내를 "길안내가 남은 시간을 처음 받은 뒤 N초"로 미룬다 (N = S10에서 잰 내비 음성 길이 + 1초). `TripAnnouncer`에 시점 규칙을 더하고 R1과 테스트를 고친다.

### 갈림길 (spike 결과 → 할 일)

| spike 결과 | 할 일 |
|---|---|
| 소리 나고 큰 문제 없음 | 계획대로. S의 값으로 설계 결정 5·6, 단계 5를 고친다 |
| 소리 나지만 앱이 뒤에 있으면 안 남 (S11) | 계획대로 + 한계로 기록. 시연·영상은 [출발] 뒤 GNB의 MeongCare 아이콘으로 우리 앱을 앞으로 가져온다. feature-facts에 "앱이 화면에 있을 때"를 조건으로 쓴다 |
| 새 객체의 첫 문장만 앞이 잘림 (S4) | 객체를 안내 하나 동안 유지한다 (설계 결정 6의 갈림길). 앞 쉼표가 들으면 그것도 쓴다 |
| 언제나 앞이 잘리고 우회가 안 들음 | 계획대로 + 한계로 기록 |
| `stop()` 직후 `release()`하면 계속 말함 (S7) | 기본안(`stop()` → `onStop` 또는 1초 → `release()`)이 그대로 맞다. sdk-notes에 기록 |
| 내비 음성 중 우리 소리가 거절됨 (`AUDIO_ERROR`) | 실패는 로그만 (R8). 출발 안내 시점을 미룬다 |
| 포커스를 아무도 안 잡음 (S8) | 단계 5-3 |
| **`ON_DEVICE` 소리가 안 남** (엔진 오류, 언어 없음, 권한 거부, 콜백 없음) | `[사용자 승인 필요]` U4의 답대로. 기본안: **017을 멈춘다.** sdk-notes에 결과를 기록하고, feature-facts의 TextToSpeech 줄은 "에뮬레이터에서 소리가 나지 않아 쓰지 않았다"로 고친다. 기획서에는 향후 계획으로만 쓴다. 화면은 지금 그대로다 (주행 화면의 "병원이 카드를 확인했어요" 한 줄과 도착 화면이 이미 자막 역할을 한다. 주행 화면에 자막을 더하지 않는다: 규칙 3). 이 계획을 `completed/`로 옮기며 "중단"과 이유를 적는다. 018은 소리 확인 없이 화면 확인으로 다시 계획한다 |

## 확인 필요 (시작 전에 해결)

### [사용자 승인 필요]

새 `pleos.car.permission.*`은 없다 (`TTS_SERVICE`는 2026-11-03 팀 승인). 아래는 기획 결정이다. 한 번에 묻는다. 추천안이 위 설계 결정에 반영돼 있다.

1. **U1. 소리로 알려 줄 때와 문장** (단계 3 전에 필요)
   - 추천: 설계 결정 1의 표대로 다섯 가지(출발, 늦게 보냄, 못 보냄, 병원 확인, 도착)를 안내 한 번에 한 번씩만 말한다. 남은 시간·대기 시간이 바뀔 때는 말하지 않는다 (내비 음성과 겹치고, 자주 말하면 운전에 방해가 된다).
   - 다른 안 (b): 꼭 필요한 세 가지(출발, 병원 확인, 도착)만. 시간이 모자라면 이쪽으로 줄인다 (아래 "범위를 줄여야 할 때").
   - 다른 안 (c): 문장을 고친다. 원하는 문장을 알려 주면 그대로 쓴다. 단, 병원 이름 뒤에 "이/가", "을/를"이 오는 문장은 피한다 (병원 이름의 끝 글자를 미리 몰라서 틀릴 수 있다).
2. **U2. 시연용 병원으로 출발할 때 "진료 카드를 보냈어요"라고 말할지** (단계 3 전에 필요)
   - 배경: 서버 없이 도는 제출 APK에는 시연용 병원만 있고, 카드를 실제로 보내지 않는다. 화면도 "시연용: 실제 병원에는 보내지 않았어요"라고 한다.
   - 추천: **말하지 않는다.** "시연용 바른 동물병원까지 안내를 시작해요."까지만. 소리와 화면이 같은 사실을 말한다.
   - 다른 안: 말한다. 영상에서는 자연스럽지만 실제와 다르다.
   - "보냈어요"와 "병원이 확인했어요" 소리는 PC의 Firebase 에뮬레이터 + 병원 웹으로 찍는 시연(확인 절차 D3·D4)에서 들린다.
3. **U3. 소리 끄기 버튼** (단계 3 전에 필요)
   - 추천: **이번에는 만들지 않는다.** 소리 크기는 차 볼륨으로 조절한다. 11/15까지 시간이 빠듯하다.
   - 다른 안: 정차 화면(처음 화면이나 안내 영역)에 [소리 안내 켜기/끄기] 버튼 하나. 주행 화면에는 두지 않는다 (규칙 3). 있으면 좋음 A로 PT 전에 할 수 있다.
4. **U4. 에뮬레이터에서 소리가 나지 않으면** (단계 1 전에 정해 두면 멈추지 않고 진행된다)
   - 추천: **017을 멈추고 결과만 기록한다.** 기획서에는 "향후 계획"으로 쓴다. 앱 화면은 지금 그대로다 (위 "갈림길" 마지막 줄).
   - 다른 안: 안드로이드 기본 음성 엔진으로 대신한다. Pleos 문서에 써도 되는지가 없고(sdk-notes "STT/TTS 사용 조건 정리"), 에뮬레이터에 한국어 음성이 있는지도 모른다. 추천하지 않는다. 고르면 `[선행: pleos-docs-researcher]`가 필요하다.

### planner가 정한 것 (바꾸려면 알려 달라. 묻지 않고 진행한다)

- 주행 중·정차 중 모두 말한다. 앱이 화면에 없어도 말하도록 앱 범위에서 돈다 (실제로 소리가 나는지는 S11).
- 반려견 이름은 문장에 넣지 않는다. 병원 이름 뒤에는 받침과 상관없는 말만 붙인다.
- 문장은 Kotlin 코드(`domain/trip/TripAnnouncement.kt`)에 둔다.
- 사건은 안내 하나에 한 번씩. 앞 말이 끝나야 다음 말. 안내가 끝나면(취소·닫기) 남은 말을 버리고 멈춘다. 취소는 말하지 않는다.
- 말하기 실패는 로그만 남기고 다시 말하지 않는다.
- Fake 모드는 차량 기기여도 소리가 없다 (010 음성 입력 표와 같다). 소리가 나는 영상은 시연 모드로 찍는다.
- 일반 폰의 시연 모드(제출 APK를 폰에서 돌릴 때)는 소리가 없다 (Fake). 화면에 보일 것이 없어 시연용 표시도 없다.
- `TextToSpeech` 객체는 말하기 한 번마다 만든다 (S3·S4로 바꿀 수 있다).
- usage와 오디오 포커스는 위 "usage 고르는 규칙"으로 spike 결과를 보고 정한다.
- `registerApp`, `setSpeakerType`, `setAudioZoneId`는 부르지 않는다.
- 로그에는 사건 종류와 결과만 남긴다.

### [선행: pleos-docs-researcher]

- **필수 경로에는 없다.** 쓰는 API(생성자·`Mode.ON_DEVICE`, `initialize`, `addEventListener`/`removeEventListener`, `setLocale`, `setAudioAttributes`, `speak`, `stop`, `release`, `EventListener` 6개, `OnInitializeListener`, `ErrorType` 12개, 권한 `TTS_SERVICE`)는 모두 sdk-notes에 있다 (문서 또는 [aar-tts]).
- 남은 모름(콜백 순서, 겹침, 포커스, 첫 음절 잘림, 뒤에서 소리)은 2026-11-12 조사(T1~T7)에서 "문서에 없음"으로 끝났다. 조사로는 답이 없어서 spike(단계 1)로 본다.
- U4에서 다른 안(안드로이드 기본 음성)을 고를 때만: "Pleos Connect 앱이 `android.speech.tts.TextToSpeech`를 써도 되는가, Pleos 에뮬레이터에 한국어 음성 엔진이 있는가".

## 확인 절차 (단계 7)

`/verify` 규칙대로 에이전트는 에뮬레이터를 직접 띄우지 않는다. 사용자에게 Cold boot를 부탁한다. 기기가 둘이면 `adb -s`로 고른다. 스크린샷과 UI 트리는 `.claude/.cache/p17-<번호>.png`, `.xml`에 둔다 (커밋하지 않는다). 소리는 사용자가 듣고 알려 준다.

### Pleos 에뮬레이터 `pleos_connect` (Cold boot, debug, 패널 "DEBUG · 시연 (주행 Real)")

기어는 Virtual Console로 바꾼다 (`adb forward tcp:8088 tcp:8088` → `http://127.0.0.1:8088/`, 또는 006 노트의 `vc()`).

| # | 조작 | 기대 |
|---|---|---|
| D1 | 서버 설정 없이: 기어 P → [진료 카드 만들기] → 증상 → [카드 만들기] → [가장 빨리 진료받을 곳 찾기] → "시연용 바른 동물병원" → [출발] | 5초 안에 "시연용 바른 동물병원까지 안내를 시작해요." (U2 답대로). 내비 앱이 앞에 나온다. `adb logcat -s TripAnnouncer`에 "소리 안내: 출발 → 끝까지" (병원 이름 없음) |
| D2 | 기어 D → 남은 시간이 줄다 도착 | "시연용 바른 동물병원에 도착했어요. 진료 카드를 접수처에 보여 주세요." 한 번. 우리 앱이 뒤에 있을 때(내비가 앞)와 GNB로 앞에 가져왔을 때를 S11 결과에 맞춰 본다 |
| D3 | PC의 Firebase 에뮬레이터 + 병원 웹 (README "병원 웹", 먼저 `npm --prefix firebase run preflight -- --fix`) → 서버 병원으로 [출발] | "…까지 안내를 시작해요. 진료 카드를 보냈어요." |
| D4 | D3 안내 중 병원 웹에서 [카드 확인] | 몇 초 안에 "…에서 진료 카드를 확인했어요." 한 번. 30초 더 기다려도 다시 말하지 않는다. 주행 화면의 "병원이 카드를 확인했어요" 줄은 지금처럼 보인다 |
| D5 | [카드 없이 길안내만] → 도착까지 | "…까지 안내를 시작해요."만. 도착 때 E5 |
| D6 | 출발 안내를 말하는 중에 [안내 취소] (기어 P) | 말이 멈추고 그 뒤 아무것도 말하지 않는다. 몇 초 뒤에도 음성 서비스가 계속 말하지 않는다 (S7) |
| D7 | 내비의 경로 시작 음성과 우리 출발 안내 | 겹침 결과를 진행 로그에 적는다 (S10의 결정대로 동작) |
| D8 | (가능하면) D3처럼 서버 목록을 받은 뒤 Firebase 에뮬레이터를 끄고 [출발] | 출발 안내 뒤 "진료 카드를 보내지 못했어요. 정차하면 다시 보낼 수 있어요." 한 번 |
| D9 | 로그와 권한 | `adb logcat -d -t 500 *:E`에 크래시·`SecurityException` 없음. `adb logcat -d -s PleosTts TripAnnouncer HospitalTrip`에 병원 이름("동물병원")이 없다. `adb shell dumpsys package com.meongcare.app`에 `pleos.car.permission.TTS_SERVICE: granted=true` |

- 시간이 되면 007 방식(debug 키로 서명한 확인용 사본, `.claude/.cache/`, 커밋 안 함)으로 release를 설치해 D1·D2를 한 번 더 본다 (release = 시연 모드 = Real 소리). 하지 않으면 하지 않았다고 적는다.

### 일반 폰 에뮬레이터 `my_avd` (debug)

| # | 조작 | 기대 |
|---|---|---|
| F1 | Fake 모드에서 카드 → 병원 → [출발] → (시나리오가 주행) → 도착 → (시나리오가 정차) | 소리 없음. `adb logcat -s TripAnnouncer`에 "출발", "도착"이 순서대로 한 번씩, 결과 "끝까지". 화면은 지금과 같다 |
| F2 | 시연 모드로 F1 | F1과 같다 (FAKE) |
| F3 | Real 모드 | `TripAnnouncer` 로그 없음 (NONE). 크래시 없음 |

## 단계

단계 하나 = 커밋 1~3개. 커밋마다 `assembleDebug`가 통과해야 한다 (커밋 직전 훅). 테스트는 대상 코드와 같은 커밋에 넣는다.

**시작 전 준비:**
- 위 `[사용자 승인 필요]` U1~U4를 사용자에게 한 번에 확인받고 답을 진행 로그에 적는다. 답이 추천안과 다르면 설계 결정 1·2·7과 확인 절차를 먼저 고친다.
- 시작 시점의 단위 테스트 개수를 진행 로그에 적는다 (`./gradlew testDebugUnitTest`. 2026-11-12 기록은 1,293개).
- 파일 이름으로만 `git add`한다. 미추적 폴더(`docs/product/bmad/`, `.agents/`, `.codex/` 등)를 올리지 않는다.
- 사용자에게 알린다: 단계 1과 단계 7에서 Pleos 에뮬레이터 Cold boot와 소리 듣기를 부탁한다. 단계 7에서 `my_avd`도.

**순서 메모:** spike(1) → 인터페이스·Fake(2) → 사건 규칙(3, Fake로 동작) → 구성(4) → Real 판단 로직(5-1) → Pleos 래퍼(5-2, 되돌리기 단위) → tester(6) → 검증(7) → 문서(8) → 노트(9). 사용자가 바로 에뮬레이터를 켤 수 없으면 2~4를 먼저 해도 된다 (spike 결과에 기대지 않는다). **5-1 전에는 단계 1을 끝낸다.**

### 필수

- [x] 0. 이 계획 파일 커밋 → `docs(plans): 017 TTS 음성 안내 계획 추가` (`8989070`)

- [ ] 1. Spike: Pleos 에뮬레이터에서 TTS 확인 (약 2~3시간, 사용자 귀 필요)
  - 1-1. 로컬 `spike/tts-ondevice` 브랜치에 probe를 만든다 (위 "Spike"). 이 브랜치의 커밋은 dev에 합치지 않는다.
  - 1-2. 사용자에게 Cold boot를 부탁하고 S1~S13을 본다 (S14는 시간이 되면). 결과를 이 계획 진행 로그에 바로 적는다.
  - 1-3. `feat/tts-guidance`로 돌아와 기록 → `docs(pleos): TTS ON_DEVICE 에뮬레이터 확인 결과 기록`
    - `docs/pleos/sdk-notes.md`: 새 절 "TTS `ON_DEVICE` 실행 확인 (관찰, 날짜)" (PC, AVD, 음성 서비스 버전, S1~S13 결과). "017·018" 목록의 해당 항목 체크 (AAR 항목 4개 포함). 미확인 "Pleos 에뮬레이터에 On-device STT/TTS 엔진이 있어…"에 TTS 결과.
    - `docs/pleos/ux-guidelines.md`: "우리 TTS 안내가 내비 음성과 겹칠 때", "TTS·음성 안내 선행 묵음", "미확인"에 관찰.
    - `docs/pleos/emulator-setup.md`: Windows에서 에뮬레이터 소리 출력에 대해 새로 안 것이 있으면.
  - 1-4. 갈림길을 정하고 이 계획의 설계 결정 5·6, 단계 5-2·5-3, 출발 안내 시점을 고친다 → `docs(plans): 017 spike 결과로 재생 방식 확정`
  - **소리가 안 나면**: "갈림길" 마지막 줄과 U4의 답대로 한다. 1-3 커밋까지 하고 멈춘다.

- [ ] 2. 말하기 인터페이스와 Fake → `feat(data): 말하기 인터페이스와 Fake 구현 추가`
  - `domain/speech/Speaker.kt` (`Speaker`, `SpeakResult`, `SpeakFailure`. 설계 결정 3)
  - `data/speech/FakeSpeaker.kt` (설계 결정 4)
  - `FakeSpeakerTest` (가상 시간):
    - `` `글자 수에 맞춘 시간이 지나면 끝까지 읽었다고 돌려준다`() ``
    - `` `한 번에 하나씩 부른 순서대로 읽는다`() ``
    - `` `취소하면 읽기를 멈추고 기다리던 다음 요청을 바로 시작한다`() ``

- [ ] 3. 길안내 사건 안내 (Fake로 동작)
  - 3-1. → `feat(domain): 길안내 사건별 소리 안내 문장 추가`
    - `domain/trip/TripAnnouncement.kt` (설계 결정 1의 표, U1·U2의 답)
    - `TripAnnouncementTextTest`:
      - `` `출발 안내는 병원까지 안내를 시작한다고 말하고 카드를 보냈을 때만 덧붙인다`() ``
      - `` `사건마다 표의 문장을 그대로 말한다`() ``
      - `` `병원 이름 뒤에 받침에 따라 바뀌는 말을 붙이지 않는다`() `` (받침 있는 이름, 없는 이름, 영문·숫자로 끝나는 이름)
      - `` `모든 문장은 두 문장 이하이고 병원 이름을 빼면 30자 안팎이다`() ``
      - `` `로그 이름에는 병원 이름이 들어가지 않는다`() ``
  - 3-2. → `feat(domain): 길안내 사건을 소리로 알리는 안내자 추가`
    - `domain/trip/TripAnnouncer.kt` (설계 결정 2의 R1~R12)
    - `TripAnnouncerTest` (가상 시간, 테스트용 `Speaker`는 테스트 안에서 직접 만든다: 받은 문장을 기록하고, 끝을 테스트가 정할 수 있게):
      - `` `출발하면 카드 전송 결과를 기다렸다가 한 번 말한다`() ``
      - `` `전송이 오래 걸리면 출발만 말하고 나중에 보냈다고 말한다`() ``
      - `` `카드 없이 출발했거나 근처 병원이면 카드 이야기를 하지 않는다`() ``
      - `` `시연용 병원은 보냈다고 말하지 않는다`() ``
      - `` `전송이 실패하면 한 번만 알린다`() ``
      - `` `병원이 확인하면 한 번만 말한다`() ``
      - `` `안내를 거친 도착만 한 번 말하고 그 뒤 사건은 말하지 않는다`() ``
      - `` `앞 말이 끝나기 전에 생긴 사건은 끝난 뒤 순서대로 말한다`() ``
      - `` `안내가 취소되거나 닫히면 남은 말을 버리고 말하는 중이면 멈춘다`() ``
      - `` `말하기가 실패해도 다음 사건은 말한다`() ``
      - `` `다음 안내에서는 사건을 처음부터 다시 센다`() ``
      - `` `남은 시간이 바뀌어도 말하지 않는다`() ``
      - `` `로그에는 사건 종류와 결과만 남고 병원 이름은 없다`() ``

- [ ] 4. 구성과 연결 → `feat(di): 데이터 모드별 소리 안내 구성 추가`
  - `di/SpeechPlan.kt` (`SpeechOutputKind`, `SPEECH_OUTPUT_ENABLED`, `speechOutputKindOf`. 설계 결정 7)
  - `AppContainer`: `speechOutputKind`, `speaker` (FAKE → `FakeSpeaker()`, REAL → **임시 `null`**), `init`에서 `TripAnnouncer(...).start()` (로그 태그 `TripAnnouncer`)
  - `SpeechPlanTest`:
    - `` `구성표는 모든 모드와 기기 조합에서 계획의 표와 같다`() ``
    - `` `스위치를 끄면 모든 모드와 기기에서 소리 안내가 없다`() ``
    - `` `release 빌드는 차량 기기에서 Real 소리 안내다`() `` (`resolveDataMode(isDebugBuild = false, ...)`와 함께)
  - 확인: 일반 폰 에뮬레이터에서 F1 (로그). 화면이 바뀌지 않으므로 스크린샷은 흐름 확인용으로만.

- [ ] 5. Real 구현
  - 5-1. → `feat(data): TTS 이벤트를 말하기 결과로 바꾸는 Real 구현 추가`
    - `data/speech/TextToSpeechClient.kt` (`TtsEvent`, `TextToSpeechClient`), `data/speech/RealSpeaker.kt` (설계 결정 5의 표. spike 값 반영)
    - Pleos SDK는 아직 넣지 않는다. `AppContainer`도 아직 `REAL -> null`이다.
    - `RealSpeakerTest` (가짜 client, 가상 시간):
      - `` `초기화되고 끝까지 읽으면 끝까지 읽었다를 돌려주고 클라이언트를 닫는다`() ``
      - `` `멈춤 이벤트가 오면 멈췄다를 돌려준다`() ``
      - `` `오디오 오류는 소리 문제로 나머지 오류와 초기화 실패는 쓸 수 없음으로 나눈다`() `` (sdk-notes `ErrorType` 12개 전부 + 모르는 이름)
      - `` `시간 안에 응답이 없으면 쓸 수 없음이다`() ``
      - `` `글자 수에 맞춘 최대 읽기 시간이 지나면 닫고 쓸 수 없음이다`() ``
      - `` `취소하면 클라이언트를 한 번만 닫는다`() ``
      - `` `한 번에 하나만 읽고 다음 요청은 앞 요청이 끝난 뒤 시작한다`() ``
      - `` `끝난 뒤 오는 이벤트는 무시한다`() ``
      - `` `클라이언트가 예외를 던지면 쓸 수 없음이다`() ``
  - 5-2. → `feat(data): Pleos TextToSpeech ON_DEVICE 래퍼 추가`
    - **이 커밋에는 아래 다섯 가지만 넣는다** (설계 결정 8, 되돌리기 단위):
      - `gradle/libs.versions.toml`: `textToSpeech = "3.3.1"`, `pleos-text-to-speech = { group = "ai.pleos.playground", name = "TextToSpeech", version.ref = "textToSpeech" }` (주석: 017, `ON_DEVICE`만)
      - `app/build.gradle.kts`: `implementation(libs.pleos.text.to.speech)`
      - `app/src/main/AndroidManifest.xml`: `<uses-permission android:name="pleos.car.permission.TTS_SERVICE" />` + 2026-11-03 승인 범위라는 주석
      - `data/speech/PleosTextToSpeechClient.kt` (설계 결정 5, spike로 정한 usage·순서·close)
      - `AppContainer`: REAL → `RealSpeaker(PleosTextToSpeechClient(context))`
    - JVM 테스트 없음 (Pleos SDK·`Context`). 판단은 5-1 테스트가 맡는다.
    - 확인: 병합 매니페스트 Grep (완료 기준 4), Pleos 에뮬레이터에서 D1 한 번.
  - 5-3. (S8에서 말하는 동안 아무도 포커스를 잡지 않을 때만) → `feat(data): 소리 안내 동안 오디오 포커스 요청`
    - `data/speech/` 안에 Android `AudioManager`로 `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK`(고른 usage)을 요청하고, `Done`·`Stopped`·`Failed`·취소에서 반납한다. 거절되면 말하지 않고 `Failed(AUDIO)`.
    - 5-2의 다섯 줄을 건드리지 않게 따로 둔다 (예: `RealSpeaker`에 넘기는 `AudioFocusGate` 인터페이스 + Android 구현). JVM 테스트는 가짜 gate로 `RealSpeakerTest`에 더한다.
    - 하지 않으면 "포커스 확인 결과"와 함께 진행 로그에 적는다.

- [ ] 6. 경계 테스트 (`tester`) → `test: 소리 안내 경계 테스트 추가`
  - 메인이 `tester`에게 **이 계획 파일 경로와 대상만** 넘긴다. 구현 설명은 넘기지 않는다.
    - 대상: `TripAnnouncement`(문장·로그 이름), `TripAnnouncer`(가짜 `Speaker`와 `MutableStateFlow`로), `FakeSpeaker`, `RealSpeaker`(가짜 `TextToSpeechClient`로), `speechOutputKindOf`(+ `resolveDataMode`)
    - 대상 아님: `PleosTextToSpeechClient`, `AppContainer`, 5-3의 Android 구현
  - 기대 동작 (계약 기준): 설계 결정 1(사건·문장·횟수), 2(R1~R12), 3(하나씩, 취소, 결과), 4, 5(이벤트 표와 시간), 7(구성표)
  - 기존 테스트 파일은 고치지 않는다 (완료 기준 3).
  - tester가 찾은 버그는 메인이 프로덕션 코드에서 `fix:` 커밋으로 따로 고친다.
  - 확인: `./gradlew testDebugUnitTest` 통과. 통과 수를 진행 로그에 적는다.

- [ ] 7. 전체 검증. 커밋은 없다. 고친 것이 있으면 `fix(...)` 커밋으로 낸다.
  - `node .claude/hooks/build-gate.mjs pipeline` → "검증 파이프라인 통과."
  - `./gradlew assembleRelease` 성공
  - 완료 기준 3의 `git diff`, 완료 기준 4의 Grep, `PleosTextToSpeechClient.kt`의 로그 줄 읽기
  - 되돌리기 확인: `git revert --no-commit <5-2 해시>` → `./gradlew assembleDebug testDebugUnitTest` 통과 → `git revert --abort` (5-2 파일을 고친 `fix` 커밋이 있으면 함께)
  - `pleos_connect`: 확인 절차 D1~D9 (`/verify`로 설치·흐름 화면, 소리는 사용자)
  - `my_avd`: F1~F3
  - 기기가 없으면 멈추고 사용자에게 Cold boot를 부탁한다. 확인하지 못한 것은 그대로 적는다.

- [ ] 8. 문서 맞추기
  - `docs/architecture.md` → `docs(architecture): 소리 안내 구성과 빼는 법 기록`
    - "서비스 기능 스택"의 "음성 안내" 행: "구현함 (017)" + 말하는 사건, usage, 한계(S11 등)
    - "패키지 구조": `domain/speech/`, `data/speech/`, `domain/trip/TripAnnouncement.kt`·`TripAnnouncer.kt`, `di/SpeechPlan.kt`
    - "데이터 모드": 소리 안내 표 (설계 결정 7)
    - 새 절 "TextToSpeech 수명주기 (017)" (설계 결정 6 + spike 결과), "소리 안내 빼는 법 (017)" (설계 결정 8)
  - `docs/pleos/sdk-notes.md`, `docs/pleos/ux-guidelines.md`: 단계 1-3 뒤 구현에서 새로 본 것 (import 경로 확인, 병합 매니페스트, D6·D7 결과, `TTS_SERVICE` user 10) → `docs(pleos): TTS 래퍼 구현 중 관찰 기록`
  - `README.md`: Pleos 에뮬레이터 시연 방법에 소리 안내 (어떤 사건에 무엇을 말하는지, PC 스피커, 병원 확인 소리는 Firebase 에뮬레이터 + 병원 웹), 일반 폰은 소리 없음 → `docs: README에 소리 안내 시연 방법 추가`
  - `docs/product/feature-facts.md` → `docs(product): 기획서 사실 표에 소리 안내 반영`
    - **11/15 기획서가 이 표를 따르므로 확인 절차에서 확인한 것만 쓴다.**
    - 1번 표에 "소리 안내" 줄 (무엇을 말하나, 확인한 곳: Pleos 에뮬레이터 (+ Firebase 에뮬레이터·병원 웹), 근거 노트)
    - 2번 표에 조건 (예: Pleos 에뮬레이터에서만, 앱이 뒤에 있을 때(S11), 시연용 병원은 "보냈어요"를 말하지 않음, 일반 폰은 소리 없음)
    - 3번 표 "음성으로 안내한다" 줄: 확인된 부분을 빼고 남는 것(예: "남은 시간을 소리로 알려 준다", "실차에서")만 둔다
    - 4번 표 TextToSpeech 줄, "선언한 Pleos 권한"을 네 개로
    - 5번에 기획서 5번 "개발 계획" 문장 (노트와 같은 것)
    - 맨 위 "기준"의 dev 커밋과 계획 목록, "자동 테스트" 수
  - 확인: 완료 기준 7

- [ ] 9. 개발 노트 작성 (`docs/notes/`)
  - `docs/notes/xxj15/YYYY-MM-DD-tts-guidance.md`. 형식은 `docs/notes/README.md` 템플릿.
    - 사용한 API: Pleos TextToSpeech 3.3.1 `ON_DEVICE` (`TextToSpeech`, `initialize`, `addEventListener`, `setLocale`, `setAudioAttributes`, `speak`, `stop`, `removeEventListener`, `release`, `EventListener`, `OnInitializeListener`, `ErrorType`), 권한 `pleos.car.permission.TTS_SERVICE`(승인됨), (5-3을 했으면) Android `AudioManager` 포커스
    - 결정: 사건과 문장, 말하는 규칙, 말하기 부품 모양(018), 객체 수명, usage를 고른 근거(spike), 모드별 구성, 빼는 법
    - 시연 방법 (영상 촬영용): Pleos 에뮬레이터 Cold boot, PC 소리, 시연 모드, Firebase 에뮬레이터 + 병원 웹으로 D3·D4 장면, Virtual Console 기어 순서, [출발] 뒤 GNB로 앱을 앞에 가져오기(S11 결과에 따라). 화면 녹화에 PC 소리가 함께 담기는지 짧게 먼저 시험한다.
    - **기획서 5번 "개발 계획"에 넣을 문장** (기획 담당에게). 예: "병원으로 출발하면 Pleos TextToSpeech SDK(`ON_DEVICE`: 차 안의 음성 엔진이라 네트워크와 앱 등록이 필요 없다)가 '○○까지 안내를 시작해요', 병원 직원이 카드를 확인하면 '○○에서 진료 카드를 확인했어요', 도착하면 '진료 카드를 접수처에 보여 주세요'를 읽어 준다. 운전자는 화면을 보지 않아도 되고, 남은 시간처럼 자주 바뀌는 정보는 읽지 않아 내비 음성과 겹치지 않는다." (U1·U2 답과 확인 결과에 맞춰 고친다)
    - 남은 일: 실차, S11 등 한계, 있으면 좋음, 018에 넘길 것
  - 이 계획을 `docs/plans/completed/`로 옮기고 진행 로그를 갱신한다.
  - 커밋:
    - `docs(notes): tts-guidance`
    - `docs(plans): 017 TTS 음성 안내 완료`
  - 확인: `git status`에 이 작업의 변경이 남지 않았다. 그다음 `/ship`.

### 있으면 좋음 (필수가 끝난 뒤, 단계 9 전에. 12/03 PT 전까지 미뤄도 된다)

- [ ] A. 정차 화면에 소리 안내 켜기/끄기 (U3에서 "만든다"를 고르면 필수로 옮긴다) → `feat(ui): 정차 화면에 소리 안내 켜고 끄기 추가`
  - 처음 화면이나 안내 영역에 버튼 하나. 주행 화면에는 두지 않는다. 끄면 안내자가 말하지 않는다 (`TripAnnouncer`에 켜짐 상태를 넘긴다). 저장은 프로세스 메모리 (앱을 다시 켜면 켜짐).
  - 화면이 바뀌므로 `/verify`로 넓음·좁음·다크를 본다. 완료 기준 3의 `ui/` 예외로 적는다.
- [ ] B. debug 패널 [소리 시험] → `feat(ui): debug 패널에 소리 안내 시험 추가`
  - 정차 중에만 보인다. 누르면 예시 문장을 지금 `speaker`로 읽고 결과(끝까지/멈춤/실패 이유)를 한 줄로 보인다. GPU 없는 PC·폰에서 Fake 동작, Pleos 에뮬레이터에서 Real 동작을 길안내 없이 확인한다.
- [ ] C. spike S14 (018용: TTS 뒤 STT, TTS 중 STT). 결과는 sdk-notes "017·018" 목록에 적는다 → `docs(pleos): TTS와 STT 번갈아 쓰기 관찰 기록`

## 범위를 줄여야 할 때

11/14 밤까지 필수를 못 끝낼 것 같으면 아래 순서로 뺀다. 뺀 것은 진행 로그에 적고 PT 전에 한다.

1. 있으면 좋음 A~C 전부.
2. E2(늦게 보냄)·E3(못 보냄) → 출발·병원 확인·도착 세 사건만 (U1의 (b)). R2·R3과 그 테스트를 빼고 설계 결정 1 표에 "뺌"으로 적는다.
3. 단계 5-3 (오디오 포커스). 필요하다고 나와도 한계로 기록하고 PT 전에.
4. 확인 절차 D7·D8, release 사본 확인.

빼지 않는 것: spike(단계 1), Fake/Real 분리와 5-2의 되돌리기 단위, 로그 규칙, tester(단계 6), D1~D4·D9, `feature-facts.md` 갱신(기획서가 이것을 따른다).

## 018에 넘길 것

- `Speaker.speak()`가 `SpeakResult.Done`을 돌려준 뒤 STT를 시작한다. `AppContainer.speaker`를 그대로 쓴다.
- 한 번에 하나만 읽는다. 안내자(`TripAnnouncer`)와 018의 확인 소리가 같은 `speaker`를 쓰면 부른 순서대로 이어진다. "안내를 끊고 바로 듣기"가 필요하면 018에서 `Speaker`에 우선순위나 끊기를 더한다 (017은 만들지 않는다).
- 알려진 위험: 길안내 중 정차해서 증상 고르기 화면의 음성 입력(010)으로 듣는 중에 안내 소리가 나면 STT가 그 소리를 받아쓸 수 있다. 017에서는 막지 않는다 (드물다). 018에서 STT와 TTS를 번갈아 쓰는 규칙을 정할 때 같이 본다.
- spike S14 결과(했다면)와 sdk-notes "017·018" 목록의 STT·TTS 번갈아 쓰기 항목.

## 진행 로그

- 2026-11-12: 계획 작성 (planner).
  - 브랜치 `feat/tts-guidance`는 dev `d74af7f`에서 땄다.
  - 시작 시점의 코드:
    - TextToSpeech 의존성·`TTS_SERVICE` 권한 없음. 매니페스트의 `uses-permission`은 `NAVI_ROUTE_SEARCH`, `NAVI_ROUTE`, `INTERNET`, `STT_SERVICE`.
    - `HospitalTrip`의 공개 상태: `trip: StateFlow<TripState?>` (`delivery`: `NotSent`/`Sending`/`Sent(toServer, acknowledgedAt)`/`Failed`), `finder`, `selectedHospital`. 내부 `Phase`(EnRoute/Arrived/Cancelled)는 `private`이라 안내자는 `trip`과 `GuidanceSource.state`만 읽는다.
    - `GuidanceSource.state`의 구현(Fake, Navi, NaviRouteOnly)은 모두 `StateFlow`다.
    - 주행 화면은 "병원이 카드를 확인했어요" 한 줄과 도착 화면을 이미 보인다 (013).
    - 시연용 병원 이름은 모두 "시연용 …"으로 시작한다 (`SampleHospitals`).
  - 테스트 개수는 시작 전 준비에서 적는다.
- 2026-11-12: 시작 전 준비.
  - U1~U4는 **모두 추천안**이다. 사용자가 질문 창을 닫고 "계속"이라고 했다. 그래서 추천안으로 진행한다. 바꾸자는 말이 있으면 설계 결정 1·2·7을 먼저 고친다.
    - U1: 다섯 가지
    - U2: 시연용 병원에서는 "보냈어요"를 말하지 않는다
    - U3: 끄기 버튼은 만들지 않는다
    - U4: 소리가 안 나면 017을 멈춘다
  - 시작 시점 단위 테스트: 1,293개 (dev `309e9b6`에서 같은 날 실행. `d74af7f`는 문서만 바뀌었다).
  - Pleos 에뮬레이터 `pleos_connect`가 Cold boot로 켜져 있다 (`emulator-5554`). 사용자 확인 없이 에이전트가 직접 켰다. `/verify` 규칙과 다르다. 앞으로는 사용자에게 부탁한다.

## 결정 / 배운 점

