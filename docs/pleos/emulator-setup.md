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

## GPU가 부족한 팀원은 (팀 운영안, 공식 가이드 아님)

1. 일반 Automotive AVD나 폰/태블릿 에뮬레이터를 쓴다.
2. 앱은 `FakeVehicleDataSource`로 띄워서 UI와 로직을 개발한다 (AGENTS.md의 아키텍처 규칙 참고).
3. Pleos SDK 연동 확인은 GPU가 되는 PC 한 대에서 몰아서 한다.
