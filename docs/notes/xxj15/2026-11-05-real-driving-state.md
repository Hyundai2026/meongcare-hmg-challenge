# 진짜 주행 상태 읽기 (Real 주행 상태 + 주행 중 사용 선언)

- 날짜: 2026-11-05
- 작성: xxj15
- 브랜치: feat/real-driving-state
- 계획: docs/plans/completed/006-real-driving-state.md
- 커밋: f2cda16..(이 노트까지)

## 무엇을 했나

Real 모드에서 앱이 **차의 실제 주행 상태**를 따라가게 했다. 지금까지 Real은 항상 "모름"이라 주행 화면만 보였다.

- 기어 P(주차)면 정차 화면을, 기어 D면(멈춰 있어도) 주행 화면을 보여 준다.
- 매니페스트에 "주행 중 사용(DO)" 선언을 넣었다. 그래서 주행 중에도 Pleos가 우리 앱을 "주행 중에는 사용할 수 없습니다." 화면으로 가리지 않는다. 우리 주행 화면("병원까지 N분")이 실제로 보인다.
- 시연 영상에서 왼쪽 Driving view(기어·속도)와 우리 앱 화면이 이제 맞는다.

## 어떻게: 핵심 결정과 이유

자세한 내용: [docs/architecture.md](../../architecture.md) "주행 상태와 화면", "Car(AAOS) 연결 수명주기"

- **기어·속도가 아니라 차의 "주행 규제" 값을 읽는다** (AAOS `CarUxRestrictionsManager`의 `isRequiresDistractionOptimization`).
  - Pleos와 Google 모두 이 방식을 권한다. 지역마다 규제가 달라서 앱이 기어·속도로 직접 판단하지 말라고 한다.
  - 권한이 필요 없다. 새 `pleos.car.permission.*`도 없다.
  - 버린 대안: AAOS `CarPropertyManager`로 기어·속도 읽기(권한 필요, Pleos 허용 여부 모름), Vehicle SDK `getGear()`(문서에 없는 API).
- **기어 D에 멈춰 있어도 주행 화면이다** (사용자 결정). Pleos가 이때도 규제를 걸기 때문이다. 정차 화면은 사실상 P에서만 보인다.
- **DO 선언을 넣었다** (사용자 결정). 심사에서 허용되는지는 주최측 문의 B1 답을 기다린다. 안 된다고 하면 `11b8709` 커밋만 revert한다.
- **세 층으로 나눴다.**
  - 판단 로직(`RealDrivingStateSource`)은 `android.car`를 모른다. 그래서 JVM 단위 테스트로 확인한다.
  - `android.car`를 부르는 곳은 `CarUxRestrictionsClient` 파일 하나뿐이다. Pleos 에뮬레이터에서 실행해 확인했다.
  - 일반 폰(`FEATURE_AUTOMOTIVE` 없음)에서는 `android.car`를 아예 건드리지 않고 "모름"으로 둔다. Real 모드로 바꿔도 죽지 않는다.
- **차량 연결은 화면이 보일 때만 산다.** 화면이 사라지고 5초 뒤 끊고, 다시 보이면 새로 연결한다. Pleos 가이드의 "백그라운드에서는 불필요한 센서 수집을 멈춘다"와 맞는다.

## 사용한 Pleos SDK / API / 권한

- **AAOS Car API**: `Car.createCar(context, null, CAR_WAIT_TIMEOUT_DO_NOT_WAIT, listener)`, `CarUxRestrictionsManager.registerListener / unregisterListener / getCurrentCarUxRestrictions`, `CarUxRestrictions.isRequiresDistractionOptimization`
- **Gradle**: `useLibrary("android.car")` (컴파일용. Android SDK 35에 들어 있다)
- **매니페스트**: `MainActivity`에 `<meta-data android:name="distractionOptimized" android:value="true" />`
- **권한**: 새로 선언한 것 없음 (`pleos.car.permission.*` 0개, `android.car.permission.*` 0개). `<uses-library>`도 필요 없었다.
- Pleos SDK는 새로 쓰지 않았다.

## 확인

- **단위 테스트 121개 통과** (기존 89 + 새 32)
  - 직접 쓴 것 9개: 규제 값 → 주행 상태 매핑, 같은 값 한 번만, 구독 끝나면 연결 닫기, 다시 구독하면 새 연결, 연결 실패 처리
  - tester가 쓴 경계 테스트 23개. **버그 1개를 찾았다**: 규제 값이 66개 넘게 몰리면 최신 값이 버려졌다(실제 주행인데 정차로 남을 수 있음). `.conflate()`로 고쳤다.
- **검증 파이프라인** 통과, release 빌드 성공
- **Pleos 에뮬레이터** (debug, Real 모드):
  - DO 선언 없이: D(멈춤·주행)에서 Pleos가 앱을 가렸고, P로 돌아오면 정차 화면이 앞에 왔다.
  - DO 선언 후: P → 정차, D 멈춤 → 주행, D 30km/h → 주행(가려지지 않음, 입력칸 0), P → 정차. D에서 앱을 다시 켜도 주행 화면. 앱이 뒤로 가면 5초 뒤 연결을 닫고, 돌아오면 다시 연결.
  - 크래시·권한 오류 없음.
- **일반 폰 에뮬레이터**: Real → "주행 중"(모름), 크래시 없음. Fake 시연 흐름(005) 그대로.

## 시연 방법 (Pleos 에뮬레이터, 기어로 화면 바꾸기)

1. Pleos 에뮬레이터를 **Cold boot**로 켠다.
2. 앱을 설치·실행하고 debug 패널에서 [Real로 전환]을 누른다. "DEBUG · Real"이 보이면 된다.
3. 기어는 PC 브라우저의 Virtual Console(`adb forward tcp:8088 tcp:8088` → `http://127.0.0.1:8088/`)에서 바꾼다. 명령으로도 바꿀 수 있다 (Git Bash):

   ```bash
   export MSYS_NO_PATHCONV=1
   adb forward tcp:8088 tcp:8088
   VC=http://127.0.0.1:8088
   vc() { curl -s -X POST "$VC/browser/session?client=demo" > /dev/null; curl -s -X PUT "$VC$1"; echo; }
   vc "/vehicle/gear?gear=Drive"                             # 주행 화면으로 바뀐다
   vc "/vehicle/controls?speed=30&accelerator=20&brake=0"    # 왼쪽에 30km/h, 앱은 주행 화면 그대로
   vc "/vehicle/controls?speed=0&accelerator=0&brake=0"
   vc "/vehicle/gear?gear=Park"                              # 정차 화면으로 돌아온다
   ```

   - 세션 신호(`/browser/session`)를 먼저 보내지 않으면 `503 emulator_inactive`가 온다. 출처: [emulator-setup.md](../../pleos/emulator-setup.md) "Pleos 에뮬레이터에서 직접 해 본 것".
4. 앱 로그로 값을 볼 수 있다: `adb logcat -s UxRestrictions` → `DO=true active=0x50` / `DO=false active=0x0`

## 주의점 / 남은 일

- **Real 모드에서는 아직 [병원으로 안내]를 눌러도 아무 일도 없다.** 진짜 길안내(NaviHelper)가 없기 때문이다. 그래서 다음 작업은 **release 시연 모드(주행 상태는 진짜 + 길안내는 가짜)**다 (계획 007 예정, 사용자 결정).
- 주최측 문의 B1에 이렇게 구체적으로 묻는다: "Google 방식 `distractionOptimized` meta-data가 Pleos 에뮬레이터에서 동작한다. 실차와 앱 심사에서도 Lifestyle·Mobility Services 카테고리 앱에 허용되는가?"
- 기획서 5번 "개발 계획"에 반영할 것 (기획 담당): 주행/정차 판단에 `CarUxRestrictions` 사용, "정차 화면 = P 기어(주차)"라는 표현.
- README "테스트 환경"에 Pleos 에뮬레이터 + Virtual Console로 Real 모드를 보는 방법을 넣는다.
- Pleos 화면에서 글자·버튼이 작아 보이는 문제는 그대로다 (ux-guidelines "텍스트와 가독성"의 관찰). 공식 타입 스케일 확인 후 고친다.
- Pleos에서는 홈 키로 앱이 뒤로 가지 않았다. 테스트할 때는 다른 앱(설정)을 띄운다.
