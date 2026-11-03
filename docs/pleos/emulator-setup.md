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
