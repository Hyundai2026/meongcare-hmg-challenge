# tts-guidance (중간 반영)

- 날짜: 2026-11-12
- 작성: xxj15
- 브랜치: feat/tts-guidance
- 계획: docs/plans/active/017-tts-guidance.md (**끝나지 않음.** 단계 5까지 하고 사용자 요청으로 dev에 중간 반영했다. 계획은 `active/`에 남긴다)
- 커밋: 8989070..(이 노트)

## 무엇을 했나

병원으로 가는 길에 일어난 일을 차가 소리로 알려 준다. 알리는 순간은 다섯 가지다.

- 출발: "○○까지 안내를 시작해요. (진료 카드를 보냈어요.)"
- 카드를 늦게 보냄
- 카드를 못 보냄
- 병원이 카드를 확인함: "○○에서 진료 카드를 확인했어요."
- 도착: "○○에 도착했어요. 진료 카드를 접수처에 보여 주세요."

운전자는 화면을 보지 않아도 된다. 화면은 바꾸지 않았다.

## 어떻게: 핵심 결정과 이유

- **사용자 결정 U1~U4는 모두 추천안이다** (계획 진행 로그). 사용자가 질문 창을 닫고 "계속"이라고 해서 추천안으로 갔다.
  - U1: 다섯 가지를 안내 한 번에 한 번씩 말한다.
  - U2: 시연용 병원에는 실제로 보내지 않으니 "보냈어요"를 말하지 않는다.
  - U3: 끄기 버튼은 만들지 않는다.
  - U4: 소리가 안 나면 멈춘다.
- **먼저 Pleos 에뮬레이터 spike로 동작을 봤다** (로컬 `spike/tts-ondevice`, 결과: sdk-notes "TTS `ON_DEVICE` 실행 확인").
  - 말하는 중에 새 문장을 주면 앞 문장이 끊기고, 끊긴 문장에는 끝 신호가 없다 → `RealSpeaker`가 한 번에 하나씩(Mutex) 맡긴다.
  - SDK도 음성 서비스도 오디오 포커스를 잡지 않는다 → 앱이 길안내 음성(`USAGE_ASSISTANCE_NAVIGATION_GUIDANCE`)으로 `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK`을 잡는다 (`AndroidAudioFocusGate`).
  - 재생 종류의 기본값은 음악과 같은 `USAGE_MEDIA`다. `setAudioAttributes`로 바꾸면 그대로 반영된다.
  - 우리 앱이 내비에 가려져 있어도 소리가 난다.
  - 초기화는 9~16ms라 말하기 한 번마다 객체 하나로 간다. 다만 부팅 뒤 첫 합성은 약 6초 늦다.
  - SDK와 음성 서비스가 읽을 문장을 기기 로그에 남긴다 → 문장에는 병원 이름만 넣고 반려견 이름·증상은 넣지 않는다.
- 구조는 010 음성 입력과 같은 나눔이다.
  - `domain/speech/Speaker`(018도 쓴다)
  - `data/speech/FakeSpeaker`
  - `RealSpeaker`(판단, JVM 테스트) + `PleosTextToSpeechClient`(SDK, 유일한 import)
  - `di/SpeechPlan`(모드별 구성표)
  - 사건 규칙은 `domain/trip/TripAnnouncer`(R1~R12)다. `HospitalTrip`은 고치지 않았다.
- 빼는 법: `SPEECH_OUTPUT_ENABLED = false`, 또는 커밋 `3c706d2`·`7062938` revert (architecture.md "소리 안내 빼는 법").

## 사용한 Pleos SDK / API / 권한

- `ai.pleos.playground:TextToSpeech:3.3.1`, `Mode.ON_DEVICE`. 앱 등록(Client ID/Secret)은 하지 않았다.
- 쓴 API:
  - `TextToSpeech(context, Mode)`, `initialize(OnInitializeListener)`
  - `addEventListener`, `setLocale(Locale.KOREA)`, `setAudioAttributes`
  - `speak(text, true)`, `stop`, `removeEventListener`, `release`
  - `EventListener` 6개, `ErrorType`
- 권한 `pleos.car.permission.TTS_SERVICE`: 2026-11-03 팀 승인. 에뮬레이터에서 보호 수준 normal.
- 안드로이드 `AudioManager` 오디오 포커스 (Pleos API 아님)

## 확인

- 테스트:
  - 새 단위 테스트 36개가 통과했다: FakeSpeaker 3, TripAnnouncementText 5, TripAnnouncer 13, SpeechPlan 3, RealSpeaker 12.
  - 커밋마다 `assembleDebug`가 통과했다.
  - 전체 검사는 `/ship`의 검증 파이프라인으로 했다.
- Pleos 에뮬레이터 (spike, debug probe):
  - 한국어 TTS 재생, 콜백 순서, 끊김, `stop`, usage, 앱이 뒤에 있을 때, 229자 문장을 확인했다.
  - **소리는 귀로 듣지 않았다.** 재생 상태(AudioTrack)와 RMS 로그로 판단했다.
- **확인하지 못한 것:**
  - `tester` 경계 테스트: 시작했다가 사용자 요청으로 멈췄다.
  - 앱 흐름 D1~D9: 에뮬레이터가 확인 도중 꺼졌다.
  - 일반 폰 F1~F3
  - 첫 음절이 잘리는지, 내비 음성과 겹칠 때
  - 포커스 요청이 실제로 받아들여지는지
  - 되돌리기 점검
  - release APK

## 시연 방법

아직 앱 흐름으로 확인하지 않았다. 확인한 뒤에 쓴다 (계획 단계 7·8).

## 주의점 / 남은 일

- 남은 순서는 계획 진행 로그의 "여기서 멈춤"에 있다: tester → 에뮬레이터 확인(사용자 Cold boot + 듣기) → README·feature-facts·sdk-notes → 노트 갱신 → 계획을 completed로.
- 이제 release(시연 모드)를 Pleos 에뮬레이터에서 돌리면 실제 TTS를 부른다. 포커스가 거절되거나 엔진이 실패하면 말하지 않고 로그만 남긴다. 화면에는 영향이 없다.
- 기획서에는 확인 전이므로 "PT 전 완성 예정"으로 쓴다 (feature-facts 3·4번 표).
- 다음 계획: 018(길안내 중 말로 증상 더하기)은 `AppContainer.speaker`를 쓴다. 019는 연결 유지·웹 연결 관리다.
