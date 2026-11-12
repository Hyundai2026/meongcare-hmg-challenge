# Pleos Connect 에뮬레이터 설정

출처:
- https://document.pleos.ai/docs/connect/guide/getting-started/application-development/setup-connect-sdk-emulator
- https://document.pleos.ai/docs/connect/guide/getting-started/application-development/configure-pleos-connect-emulator

(2026-11-03 확인)

## 요구 사양

- 최신 Android Studio가 필요하다.
- **GPU 필수:**
  - 권장: RTX 4060/5060 Laptop 8GB+ 또는 RX 7700S/7600M XT급
  - 최소: RTX 3060 Laptop 6GB+ 또는 RX 6600M 8GB / RX 6700S 8GB
- AVD RAM은 4GB로 설정한다.

## 시스템 이미지 설치

1. Android Studio에서 **Tools > SDK Manager > SDK Update Sites** 탭을 열고 `+`를 누른다.
   - Name: `Pleos Connect System Image`
   - URL (Windows/Linux): `https://nexus-playground.pleos.ai/repository/raw-releases/connect-avd/2.x/crp-sys-img.xml`
   - URL (Mac): `https://nexus-playground.pleos.ai/repository/raw-releases/connect-avd/2.x/crp-arm64-sys-img.xml`
2. **SDK Platforms** 탭에서 "Show Package Details"를 켜고, Android API 34 아래의 Pleos Connect system image를 체크한 뒤 OK를 누른다.
3. Finish를 누르고 Android Studio를 재시작한다. 이전 버전 에뮬레이터가 있으면 먼저 삭제한다.

## AVD 생성

1. **Tools > Device Manager > Create device**에서 Form Factor로 **Automotive**를 고르고 **Pleos Connect v2.1.0**을 선택한다.
2. System image: **API 34**, "Pleos Connect v2.1.0 APIs"를 선택한다.
3. Advanced Settings:
   - Startup > Default boot = **Cold**
   - RAM = **4 GB**

## 명령어로 설치하기 (팀 운영안, 2026-11-03 xxj15 PC에서 확인)

Android Studio 화면 대신 명령어로 설치하는 방법이다. 결과는 위의 화면 순서와 같다. SDK 경로는 `C:\Android` 기준이다 (`ANDROID_HOME`).
Mac(Apple Silicon)은 아래 "명령어로 설치하기: Mac"을 본다. 순서는 같고 패키지 이름과 경로가 다르다.

**설치하면 42dot의 Pleos Connect SDK 라이선스에 동의하게 된다.** 동의 전에 약관을 확인한다.
- 무료이고 Pleos Connect용 앱 개발에만 쓸 수 있다.
- 재배포·수정·역분석은 금지다.

### 1. 다운로드 주소 등록

`%USERPROFILE%\.android\repositories.cfg`를 만든다. Android Studio의 SDK Update Sites에 추가하는 것과 같다.

```
### User Sources for Android SDK Manager
count=1
disp00=Pleos Connect System Image
enabled00=true
src00=https\://nexus-playground.pleos.ai/repository/raw-releases/connect-avd/2.x/crp-sys-img.xml
```

- 확인: `sdkmanager --list`에 아래 줄이 나온다.
  - `system-images;android-34;pleos_car_emulator;x86_64 | 2 | Pleos Connect v2.1.0 APIs Intel x86_64 Atom System Image`
- 패키지 정보 (위 XML 기준):
  - 파일: `crp_car_emulator.zip`, 4,087,751,964 bytes (약 4.1GB)
  - 요구: emulator 24 이상
  - 라이선스 ID: `pleos-connect-sdk-license`

### 2. 시스템 이미지 설치

라이선스 질문에 `y`를 넘겨야 한다. **PowerShell에서 `"y" | sdkmanager.bat ...`로 넘기면 전달되지 않는다.** 이 경우 "license is not accepted"로 설치를 건너뛴다.

패키지 이름의 `;`도 cmd에서 따옴표가 필요하다. 그래서 `.cmd` 파일로 만들어 실행한다.

```bat
@echo off
set ANDROID_HOME=C:\Android
echo y| C:\Android\cmdline-tools\latest\bin\sdkmanager.bat --sdk_root=C:\Android "system-images;android-34;pleos_car_emulator;x86_64"
```

- 설치 위치: `C:\Android\system-images\android-34\pleos_car_emulator\x86_64\`
- 이미지 안의 `devices.xml`에 기기 정의가 있다:
  - 이름 "Pleos Connect v2.1.0", ID `Pleos_Connect_v2_0`, 제조사 42dot
  - 2560×1440, mdpi(160dpi), 12.8인치, RAM 4GiB

### 3. AVD 만들기

```bat
echo no| C:\Android\cmdline-tools\latest\bin\avdmanager.bat create avd -n pleos_connect -k "system-images;android-34;pleos_car_emulator;x86_64" -d Pleos_Connect_v2_0
```

`avdmanager`가 만든 `config.ini`는 기본값이 Pleos 안내와 다르다. 에뮬레이터를 끈 상태에서 백업한 뒤 고친다.

| 키 | avdmanager 기본값 | 고친 값 | 이유 |
|---|---|---|---|
| `fastboot.forceColdBoot` | `no` | `yes` | 항상 Cold boot |
| `fastboot.forceFastBoot` | `yes` | `no` | 빠른 부팅으로 켜지는 것을 막음 |
| `hw.ramSize` | `1536M` | `4096` | Pleos 안내의 RAM 4GB |
| `hw.gpu.enabled` / `hw.gpu.mode` | `no` / `auto` | `yes` / `host` | GPU 필수 |
| `hw.keyboard` | `no` | `yes` | PC 키보드로 입력 |
| `hw.initialOrientation` | `portrait` | `landscape` | 화면이 2560×1440 가로다 |
| `disk.dataPartition.size` | `800M` | `6442450944` | 앱 설치 공간 |
| `avd.id`, `avd.name` | `<build>` | `pleos_connect` | |
| `disk.dataPartition.path` | `<temp>` | 줄 삭제 | AVD 폴더의 기본 데이터 파일을 쓰게 함 |

### 4. 켜기

```
/c/Android/emulator/emulator.exe -avd pleos_connect -no-snapshot-load -no-snapshot-save -gpu host
```

- xxj15 PC(RTX 4070 Laptop)에서 약 40초 만에 부팅됐다 (`sys.boot_completed=1`).
- 로그의 `Failed to load opengl32sw`, `Software OpenGL failed`는 INFO 줄이다. GPU를 쓰므로 문제없다.
- **에뮬레이터를 두 대 켜면** `adb`가 기기를 고르지 못한다. 두 대가 켜져 있으면 `adb -s emulator-5556 ...`처럼 기기를 지정하거나 `ANDROID_SERIAL`을 설정한다. `/verify` 스킬은 기기를 지정하지 않으므로 한 대만 켜 둔다.

### 설치 후 확인한 것 (관찰, 2026-11-03)

- `adb shell getprop`: Android 14, 모델 "Connect on x86_64 emulator". `wm size` 2560x1440, `wm density` 160.
- 화면 구성:
  - 왼쪽 Driving view: 주행 가능 거리, 속도, 기어 P/R/N/D, 차량 그림, 미디어
  - 오른쪽 앱 영역: 처음엔 내비 앱("어디로 갈까요?")
  - 아래 GNB: 공조·열선, 차량, 내비, 전화, 앱 목록
  - 패키지 이름: Driving view `ai.umos.drivingview`, 내비 `ai.umos.maps.android.navigation.app`
- 우리 debug APK(`com.meongcare.app`)를 설치하고 실행했다:
  - 앱 영역 전체(창 bounds `853,90 - 2535,1300`, 1682×1210dp, 가로)에 떴다. → [ux-guidelines.md](ux-guidelines.md) "화면 방향"의 관찰 참고
  - Fake 모드: 정차 화면과 debug 패널이 정상이다.
  - Real 모드: `VehicleConnection`의 initialize 실패 로그가 없다. 주행 상태는 Real 구현 전이라 UNKNOWN이고, 그래서 기어가 P인데도 주행 화면이 뜬다 (설계대로).

## 명령어로 설치하기: Mac (팀 운영안, 2026-11-04 xxj15 Mac에서 확인)

Apple Silicon Mac용이다. 순서는 위 Windows와 같다. SDK 경로는 Android Studio 기본값인 `~/Library/Android/sdk`다 (`ANDROID_HOME`).

**디스크를 30GB 이상 비워 둔다.** 다운로드는 4.7GB인데, 압축을 풀면 시스템 이미지만 19GB다 (`system.img` 18.3GB).

1. `~/.android/repositories.cfg`를 만든다. 주소만 Mac용(`crp-arm64-sys-img.xml`)이고 형식은 Windows와 같다.

   ```
   ### User Sources for Android SDK Manager
   count=1
   disp00=Pleos Connect System Image
   enabled00=true
   src00=https\://nexus-playground.pleos.ai/repository/raw-releases/connect-avd/2.x/crp-arm64-sys-img.xml
   ```

   - 확인: `sdkmanager --list`에 아래 줄이 나온다.
     - `system-images;android-34;pleos_car_emulator;arm64-v8a | 2 | Pleos Connect v2.1.0 APIs arm64 System Image`
   - 패키지 정보 (위 XML 기준): 파일 `crp_car_arm64_emulator.zip`, 4,669,762,816 bytes. 요구 emulator 24 이상. 라이선스 ID는 Windows와 같은 `pleos-connect-sdk-license`.
2. 시스템 이미지를 설치한다. zsh에서는 `yes |`로 라이선스 질문에 답하면 된다. compileSdk 35용 Platform이 없으면 같이 받는다.

   ```
   yes | sdkmanager "platforms;android-35" "system-images;android-34;pleos_car_emulator;arm64-v8a"
   ```

   - 설치 위치: `~/Library/Android/sdk/system-images/android-34/pleos_car_emulator/arm64-v8a/`
   - 기기 정의(`devices.xml`)는 Windows와 같다: ID `Pleos_Connect_v2_0`, 2560×1440, mdpi.
3. AVD를 만든다.

   ```
   echo no | avdmanager create avd -n pleos_connect -k "system-images;android-34;pleos_car_emulator;arm64-v8a" -d Pleos_Connect_v2_0
   ```

   - 다른 시스템 이미지의 `devices.xml`을 못 읽는다는 `Error: Could not load devices ...` 줄이 나와도 AVD는 만들어진다.
   - `config.ini`(`~/.android/avd/pleos_connect.avd/config.ini`)를 위 Windows 표대로 고친다. Mac의 avdmanager 기본값은 Windows와 두 군데 다르다:
     - `hw.ramSize`: `2G` (Windows `1536M`) → `4096`
     - `disk.dataPartition.size`: `10G` (Windows `800M`) → `6442450944`
   - 시스템 이미지의 `source.properties`는 `hw.ramSize=8192`를 권한다. 우리는 Pleos 안내대로 4GB로 둔다.
4. 켠다.

   ```
   emulator -avd pleos_connect -no-snapshot-load -no-snapshot-save -gpu host
   ```

### Mac 설치 후 확인한 것 (관찰, 2026-11-04)

- PC: MacBook, Apple M4 Pro, RAM 24GB, macOS 15.1. 에뮬레이터 36.6.11.
- 로그: `Hardware GPU requirements to run avd: pleos_connect are passed`. 그래픽은 `Apple M4 Pro` 위의 OpenGL ES 3.0 (Metal) 번역 계층이다.
- 커널 시작 후 약 12초 만에 부팅 화면이 끝났다 (`sf_stop_bootanim: 12431`ms). `sys.boot_completed=1`.
- `adb shell getprop`:
  - Android 14, 모델 "Car on arm64 emulator" (Windows는 "Connect on x86_64 emulator")
  - fingerprint `Pleos/crp_avd_arm64/crp_avd_arm64:14/RELEASE.release_connect_v2.1.0/...`
  - `wm size` 2560x1440, `wm density` 160, RAM 약 4GB
- 화면 구성은 Windows와 같다: 왼쪽 Driving view, 오른쪽 앱 영역, 아래 GNB.
- 우리 debug APK(`com.meongcare.app`)를 설치하고 실행했다: 오른쪽 앱 영역에 Fake 모드 정차 화면과 debug 패널이 정상으로 떴다.
- Virtual Console: `adb forward tcp:8088 tcp:8088` 뒤 `http://127.0.0.1:8088/`이 열린다 (페이지 제목 "Pleos Connect Virtual Console").

## 주의

- **반드시 Cold boot로 켠다.** Quick boot로 켜면 VHAL(차량 데이터)을 쓸 수 없다.
- 기본 언어는 한국어다. 영어로 바꾸려면 차량 설정 > 일반 설정 > 언어/Language > English.
- STT를 테스트하려면 Extended Controls > Microphone에서 마이크 입력을 켠다.
- Virtual Console(차량 데이터 시뮬레이션)은 에뮬레이터 2.1.0 이상이 필요하다.
- 블루투스 동글 페어링은 지원하지 않는다.

## 에뮬레이터 마이크 (Mac)

(팀 운영안, 2026-11-05 xxj15 Mac에서 확인. 공식 가이드 아님)

기본 상태로 켜면 에뮬레이터 안에서 녹음은 되지만 소리가 무음이다. 아래 두 가지가 모두 필요했다.

1. **에뮬레이터를 켠 앱에 macOS 마이크 권한을 준다.** 시스템 설정 > 개인정보 보호 및 보안 > 마이크.
   - macOS는 에뮬레이터를 실행한 앱(터미널, VS Code, Android Studio)을 기준으로 권한을 본다. Claude Code(VS Code)에서 켰다면 **Visual Studio Code**를 켠다.
   - 권한이 거부된 상태에서 켠 에뮬레이터는 권한을 준 뒤에도 무음이었다. **에뮬레이터를 다시 켠다.**
2. **`-allow-host-audio`를 붙여 켠다.** `emulator -help` 원문: "Allows sending of audio from audio input devices. Otherwise, zeroes out audio."

   ```
   emulator -avd pleos_connect -no-snapshot-load -no-snapshot-save -gpu host -allow-host-audio
   ```

   - 켠 상태에서는 `adb emu avd hostmicon` / `hostmicoff`로 켜고 끈다.
- 확인 방법: STT `ResultListener.onUpdatedRms` 값. 무음일 때는 0.5 안팎, Mac 마이크가 들어오면 조용할 때 100~500, 말할 때 수천이다.
- **소리가 늦게, 겹쳐서 들어올 수 있다.** 몇 초 전에 말한 문장이 다시 인식된 적이 있다. Mac 스피커를 꺼도 그대로였으므로 스피커→마이크 메아리는 아니다. 시연 영상을 마이크로 찍을 때 주의한다.
- AAOS 앱은 user 10에서 돈다. 마이크 권한을 adb로 줄 때는 `adb shell pm grant --user 10 com.meongcare.app android.permission.RECORD_AUDIO`.

## Virtual Console (차량 데이터 시뮬레이션)

출처:
- [vc-overview] https://document.pleos.ai/docs/connect/guide/getting-started/application-development/use-vehicle-emulator
- [vc-guide] https://document.pleos.ai/docs/pleos-only/app-test-guide/virtual-console-guide

(2026-11-03 확인)

- 에뮬레이터 v2.1.0 이상이 필요하다. [vc-overview]
- 실행: 터미널에서 `adb forward tcp:8088 tcp:8088`을 실행한 뒤 PC 브라우저로 `http://127.0.0.1:8088/`을 연다. [vc-overview]
- 시뮬레이션할 수 있는 것 [vc-overview] [vc-guide]:
  - Motion: "현재 기어를 P, R, N, D 중 하나로 설정합니다." 브레이크 페달 0~100%, 스티어링 각도 -540~540도, 누적 주행 거리 0~1,000,000km. [vc-guide]
  - Safety(Telltales): "주차 브레이크, 안전벨트, ABS, TPMS 등 지원되는 경고등을 선택하여 Off, On, Blink 상태로 설정합니다." [vc-guide]
  - ADAS: 크루즈 컨트롤 준비/작동 상태, 목표 속도 5.556~27.778m/s. [vc-guide]
  - 그 밖에 도어, 창문, 트렁크, 시트, 조명, 와이퍼, 에너지(잔여 거리, 배터리, 충전), 공조. [vc-overview]
- > **불일치:** 개요 페이지는 "차량 속도, 도어, 조명 등 차량 상태 변경"이 된다고 하지만 [vc-overview], 상세 가이드에는 일반 차량 속도 카드가 없고 크루즈 목표 속도만 있다 [vc-guide].
- 기어·속도를 바꿨을 때 `CarUxRestrictions`(주행 규제)가 바뀌는지는 두 페이지 모두 언급이 없다. → 에뮬레이터에서는 바뀌는 것을 관찰했다 (아래 "Pleos 에뮬레이터에서 직접 해 본 것", 2026-11-05).

### 주행 상태 시험에 관한 사실 (2026-11-05 재확인)

- 사용 순서 (원문) [vc-guide]:
  1. "Pleos Connect Virtual Console에서 **Emulation Controls**를 엽니다."
  2. "왼쪽 목록에서 테스트할 Domain을 선택하거나 검색창에서 카드 이름을 검색합니다."
  3. "카드에 차량 영역 선택 항목이 있으면 테스트할 영역을 선택합니다."
  4. "버튼, 목록 또는 슬라이더로 상태와 값을 변경합니다."
  5. "Pleos Connect Emulator와 테스트 중인 앱에서 변경 결과를 확인합니다."
- Motion 탭 카드: Gear Selection, Brake Pedal, Odometer, Steering Angle, Steering Wheel Heat. **일반 차량 속도 카드는 없다.** 속도 관련 카드는 ADAS의 Cruise Target Speed뿐이다. [vc-guide]
- Gear Selection 카드 설명 (원문 두 문장): "현재 기어를 P, R, N, D 중 하나로 설정합니다." "기어 변경에 따른 앱의 상태 전환을 확인할 수 있습니다." [vc-guide]
  - "앱의 상태 전환"이 `CarUxRestrictions` 변화를 뜻하는지는 적혀 있지 않다.
- 기어나 속도가 `CarUxRestrictions`를 바꾼다는 문장은 Pleos 문서 어디에도 없다. 확인한 곳: Virtual Console 셋업·사용 가이드, 에뮬레이터 설정 페이지, 에뮬레이터 릴리스 노트, 주행 규제 개발 가이드. [vc-overview] [vc-guide] [configure] [emulator-release-note] [driving-restriction-dev]
  - 에뮬레이터 설정 페이지는 Extended Controls를 마이크 설정에서만 말한다. "Car data"(차량 데이터) 패널 언급은 없다. VHAL은 "quick boot로 설정할 경우에는 VHAL을 이용할 수 없습니다."에만 나온다. [configure]
  - 에뮬레이터 릴리스 노트 v2.1.0: "도메인별 차량 상태/이벤트를 설정하고 앱 반응을 검증할 수 있는 테스트 환경 추가". 주행 규제 언급은 없다. [emulator-release-note]
- **2026-11-12 추가: Pleos ADB 가이드에 주행 규제를 켜고 끄는 명령이 있다.** [adb-guide]
  - 절 "주행 규제 활성화/해제". 설명 원문: "주행 규제를 활성화하는 단계별 명령어와 활성화한 주행 규제를 해제하는 명령어입니다." "모든 차종에서 공통으로 사용할 수 있는 명령어입니다."

    ```
    adb shell cmd hmg_car_service --service drivingstate driving_regulation on
    adb shell cmd hmg_car_service --service drivingstate driving_regulation off
    ```

  - 이 명령이 `CarUxRestrictions`(`isRequiresDistractionOptimization()`, `ActiveRestrictions`)를 바꾼다고 적혀 있지는 않다. Pleos 에뮬레이터에서 실행해 보지 않았다. 되면 Virtual Console 없이 주행 중 화면(018 주행 중 음성 입력 등)을 시험할 수 있다.
  - 같은 페이지의 "기어 상태 변경" 절에는 터미널 명령이 없고 Virtual Console 가이드를 보라고만 한다. "모드 변경" 절은 유틸리티 모드 전환(차속 0, 주차 브레이크, 기어 P 상태에서 `/dev/vcs_simulator_rx`에 쓰는 명령)이다. [adb-guide]
  - 출처 [adb-guide]: https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/adb-command-reference-guide (FAQ - 디버깅을 위한 주요 ADB Command 활용법, 2026-11-12 확인)

### Google AAOS 에뮬레이터의 주행 시뮬레이션 (참고. Pleos 에뮬레이터에서 되는지는 미확인)

- Google 원문: "To simulate driving, you should set the *Car speed* to a non-zero value and *Gear* to something other than **P (Park)**. To simulate a parked state, all that is necessary is to set the *Gear* to **P (Park)**." Extended controls의 **Car data**에서 바꾼다. [g-emulator]
- AOSP 문서의 adb 명령 (원문) [aosp-consume]:

  ```
  adb shell dumpsys activity service com.android.car inject-vhal-event 0x11600207 0
  adb shell dumpsys activity service com.android.car inject-vhal-event 0x11400400 4
  adb shell dumpsys activity service com.android.car inject-vhal-event 0x11400400 8
  adb shell dumpsys activity service com.android.car inject-vhal-event 0x11600207 30
  ```

  - `0x11600207`은 속도(m/s), `0x11400400`은 기어(4 = Park, 8 = Drive). 문서 주석상 순서대로 Parked → (기어 D, 속도 0) Idling → (속도 30) Moving이다.
- 이 두 방법은 Pleos 문서에 없다. adb `inject-vhal-event`는 Pleos 에뮬레이터에서 거절된다 (user 빌드, 아래 관찰). Car data 패널은 확인하지 않았다.
- **Virtual Console에는 속도 카드가 없으므로**, 기어만 D로 바꾸면 AOSP 기준으로는 Idling이다. AOSP 기본 예시는 Idling도 규제(`requiresDistractionOptimization="true"`)지만, Pleos 설정은 비공개다. [aosp-uxr] [driving-restriction-dev]

### Pleos 에뮬레이터에서 직접 해 본 것 (관찰, 2026-11-05)

xxj15 PC(RTX 4070 Laptop), AVD `pleos_connect`, Cold boot. 문서가 아니라 실행해서 본 결과다.

- **adb `inject-vhal-event`는 안 된다.** `adb shell cmd car_service inject-vhal-event 0x11400400 8` → `SecurityException: The command 'inject-vhal-event' requires non-user build`. 이 에뮬레이터는 `ro.build.type` = `user`다.
- **Virtual Console 웹 페이지가 쓰는 HTTP 요청으로 기어·속도를 바꿀 수 있다.**
  - 페이지(`http://127.0.0.1:8088/`)의 스크립트를 읽고 찾은 내부 요청이다. 문서에 없으므로 에뮬레이터 버전이 바뀌면 달라질 수 있다.
  - 페이지에는 기어 버튼 외에 가속·브레이크 페달과 속도(`speed_kph`) 조작도 있다. 상세 가이드의 카드 목록에 없는 것과는 다르다.
  - 먼저 브라우저 세션 신호를 보내야 한다. 페이지는 1초마다 보낸다. 신호가 없으면 다른 요청이 `503 {"error":"emulator_inactive"}`로 거절된다.

  ```bash
  adb forward tcp:8088 tcp:8088
  VC=http://127.0.0.1:8088
  curl -X POST "$VC/browser/session?client=test"                        # 세션 신호 (요청 직전에 보낸다)
  curl -X PUT  "$VC/vehicle/gear?gear=Drive"                            # Park | Reverse | Neutral | Drive
  curl -X PUT  "$VC/vehicle/controls?speed=30&accelerator=20&brake=0"   # 속도 km/h, 페달 %
  curl         "$VC/vehicle/state"                                      # 현재 값 (JSON)
  ```

- **결과: 주행 상태와 주행 규제가 실제로 바뀐다.** `adb shell dumpsys car_service --services CarDrivingStateService` / `CarUxRestrictionsManagerService`로 봤다.

  | 조작 | Driving State | UX 규제 (`DO` = requiresDistractionOptimization) |
  |---|---|---|
  | 기어 P | 0 (Parked) | DO: false, 제한 0 |
  | 기어 D, 0 km/h | 1 (Idling) | DO: true, 제한 0x50 (`NO_VIDEO` + `NO_SETUP`) |
  | 기어 D, 30 km/h | 2 (Moving) | DO: true, 제한 0x50 |
  | 다시 D 0 km/h → P | 1 → 0 | DO: false |

  - 덤프의 기본 설정(baseline): unknown은 DO true(제한 0x1ff), parked는 DO false, idling·moving은 DO true(제한 0x50). **Idling(기어 D, 정지)도 규제가 걸린다.**
- 이 요청들은 시연 영상 촬영과 테스트를 손 조작 없이 재현하는 데 쓸 수 있다.
- 앱이 내비 경로를 취소(`cancelRoute`)하면 내비 앱이 앞으로 나와 우리 앱을 가리고, 우리 앱은 스스로 돌아올 수 없다 (Android 14 백그라운드 실행 제한, 2026-11-06). 자세히: [sdk-notes.md](sdk-notes.md) `[obs-navi-emu]` "경로 취소 뒤 앞에 나오는 앱".

출처 추가 (2026-11-05):
- [configure] https://document.pleos.ai/docs/connect/guide/getting-started/application-development/configure-pleos-connect-emulator
- [emulator-release-note] https://document.pleos.ai/docs/pleos-only/release-note/emulator-release-note
- [driving-restriction-dev] https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/driving-restriction-development-guide
- Pleos 외 (Pleos가 링크하지 않은 Google·AOSP 공식 문서):
  - [g-emulator] https://developer.android.com/training/cars/testing/emulator ("Simulate driving")
  - [aosp-consume] https://source.android.com/docs/automotive/driver_distraction/consume ("Testing")
  - [aosp-uxr] https://source.android.com/docs/automotive/driver_distraction/car_uxr

### PC의 Firebase 에뮬레이터에 앱이 붙기 (관찰, 2026-11-06, 계획 012 O1)

- `adb reverse tcp:8080 tcp:8080`, `adb reverse tcp:9099 tcp:9099` + 앱 `secrets.properties`의 `FIREBASE_EMULATOR_HOST=127.0.0.1`로 Pleos 에뮬레이터 앱이 PC의 Firestore·Auth 에뮬레이터에 붙었다. 병원 목록 첫 요청(익명 가입 포함) 159ms, 다음 71ms.
- `adb reverse`의 PC 쪽은 IPv4 `127.0.0.1`로 붙는다 (PC 에뮬레이터가 `[::1]`에만 열려 있을 때는 닿지 않았다).
- `10.0.2.2`(Android 에뮬레이터에서 본 PC)로 바로 붙는 것은 해 보지 않았다.
- PC 에뮬레이터가 꺼져 있으면 연결이 바로 거부되어 시연 모드는 약 2초 만에 내장 시연 병원으로 대체했다.
- 앱이 debug이고 `network_security_config`가 그 주소의 평문 HTTP를 허용해야 한다 (release는 안 된다).

## 폼팩터 테스트

출처: https://document.pleos.ai/docs/pleos-only/app-test-guide/form-factor-test-guide (2026-11-03 확인)

- Display preset과 Orientation(`Portrait`/`Landscape`)을 골라 가상 디스플레이(`DISPLAY2` 창)를 만들어 레이아웃을 확인하는 도구가 있다. 예시: "12.9인치 Portrait 가상 디스플레이를 생성하면 다음과 같이 표시됩니다."
- 요구: Go 1.22 이상, 포트 `8095`, `8096` 비워 두기.
- 확인 항목: 요소 잘림·겹침, 텍스트·이미지·아이콘 크기와 정렬, 방향별 레이아웃, 터치 입력과 화면 전환.
- 도구 설치·실행 절차와 프리셋 목록은 아직 정리하지 않았다.

## GPU가 부족한 팀원은 (팀 운영안, 공식 가이드 아님)

1. 일반 Automotive AVD나 폰/태블릿 에뮬레이터를 쓴다.
2. 앱은 debug 빌드의 **Fake 모드**(기본값)로 띄워서 UI와 로직을 개발한다.
   - 화면 아래 debug 패널에서 주행 상태를 바꾼다.
   - 구조: [docs/architecture.md](../architecture.md) "데이터 모드"
3. Pleos SDK 연동 확인은 GPU가 되는 PC 한 대에서 몰아서 한다.

## 일반 AVD 권장 설정 (팀 운영안, 공식 가이드 아님)

기본값으로 만든 폰 AVD는 PC 키보드 입력이 안 되고 화면이 느리다. 2026-11-03에 `my_avd`에서 겪었다.
AVD 폴더의 `config.ini` (`%USERPROFILE%\.android\avd\<이름>.avd\config.ini`)를 에뮬레이터를 끈 상태에서 고친다. 고치기 전에 백업한다.

| 키 | 값 | 이유 |
|---|---|---|
| `hw.keyboard` | `yes` | PC 키보드로 입력 |
| `hw.gpu.enabled` | `yes` | 그래픽카드로 화면을 그림 (끄면 CPU만 써서 느림) |
| `hw.gpu.mode` | `host` | PC의 그래픽카드를 그대로 씀 |
| `hw.ramSize` | `4096M` | Pleos 가이드의 AVD RAM과 같게 |
| `vm.heapSize` | `512M` | |
| `hw.cpu.ncore` | `8` | `my_avd`에서는 6코어까지 잡혔다 |
| `fastboot.forceColdBoot` | `yes` | 항상 Cold boot (빠른 부팅으로 켜지는 것을 막음) |
| `fastboot.forceFastBoot` | `no` | |

- 확인 방법:
  - GPU: `adb shell dumpsys SurfaceFlinger | grep GLES`에 그래픽카드 이름이 나온다.
  - RAM: `adb shell cat /proc/meminfo`
- 에뮬레이터 업데이트: `sdkmanager "emulator"`. 2026-11-03 기준 37.2.13.

## 팀 PC 사양 메모

- xxj15 PC: NVIDIA GeForce RTX 4070 Laptop GPU, RAM 64GB.
  - Pleos 에뮬레이터 권장 사양(RTX 4060 Laptop 이상)을 넘는다. Pleos SDK 연동 확인을 이 PC에서 할 수 있다.
  - 2026-11-03 Pleos 에뮬레이터 설치 완료. AVD 이름 `pleos_connect` (위 "명령어로 설치하기"). 폰 AVD `my_avd`도 그대로 있다.
- xxj15 Mac: Apple M4 Pro, RAM 24GB.
  - 2026-11-04 Pleos 에뮬레이터(arm64) 설치 완료. AVD 이름 `pleos_connect` (위 "명령어로 설치하기: Mac"). 폰 AVD `Pixel_9_Pro_XL`도 있다.
  - 두 대가 같이 켜지면 `/verify`가 기기를 고르지 못한다. 하나만 켠다.
