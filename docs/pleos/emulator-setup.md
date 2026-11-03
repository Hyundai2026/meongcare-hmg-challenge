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

## 주의

- **반드시 Cold boot로 켠다.** Quick boot로 켜면 VHAL(차량 데이터)을 쓸 수 없다.
- 기본 언어는 한국어다. 영어로 바꾸려면 차량 설정 > 일반 설정 > 언어/Language > English.
- STT를 테스트하려면 Extended Controls > Microphone에서 마이크 입력을 켠다.
- Virtual Console(차량 데이터 시뮬레이션)은 에뮬레이터 2.1.0 이상이 필요하다.
- 블루투스 동글 페어링은 지원하지 않는다.

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
- 기어·속도를 바꿨을 때 `CarUxRestrictions`(주행 규제)가 바뀌는지는 두 페이지 모두 언급이 없다 → 미확인.

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
