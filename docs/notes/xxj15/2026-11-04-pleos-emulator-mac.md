# pleos-emulator-mac

- 날짜: 2026-11-04
- 작성: xxj15
- 브랜치: docs/pleos-emulator-mac
- 커밋: 49f72d2

## 무엇을 했나

Mac(Apple Silicon, M4 Pro)에 개발 환경과 Pleos Connect 에뮬레이터를 설치했다. 이제 Windows PC 없이 이 Mac에서도 앱을 빌드하고 Pleos 화면에 띄워 확인할 수 있다.

## 어떻게: 핵심 결정과 이유

- 명령어(sdkmanager/avdmanager)로 설치했다 → Windows 때와 같은 절차를 재현할 수 있고, 문서로 남기기 쉽다.
- Mac용 저장소 주소(`crp-arm64-sys-img.xml`)와 패키지 `system-images;android-34;pleos_car_emulator;arm64-v8a`를 썼다. Windows의 x86_64 이미지는 Apple Silicon에서 돌지 않는다.
- AVD RAM은 4GB로 뒀다 → 이미지의 `source.properties`는 8GB를 권하지만, Pleos 공식 안내와 Windows 설정에 맞췄다.
- 데이터 파티션은 6GB로 줄였다 (Mac 기본값 10G) → 시스템 이미지가 19GB라 디스크 여유가 32GB밖에 남지 않았다. Windows 설정과도 같다.
- 그 밖에 설치한 것:
  - SDK Platform 35: compileSdk용이고, 원래 없었다.
  - `uv` (Homebrew): BMAD용이다.
  - `local.properties`: `sdk.dir`를 적었다. gitignore 대상이다.

## 사용한 Pleos SDK / API / 권한

- 없음 (에뮬레이터 설치만)

## 확인

- `./gradlew assembleDebug` 통과 (JDK 21, Android Studio 내장)
- 에뮬레이터 Cold boot: 커널 시작 후 약 12초에 부팅 완료, GPU 요구사항 통과 로그
- debug APK 설치·실행: 앱 영역에 Fake 모드 정차 화면과 debug 패널이 정상
- Virtual Console: `adb forward tcp:8088 tcp:8088` 뒤 `http://127.0.0.1:8088/` 응답
- 자세한 관찰: [docs/pleos/emulator-setup.md](../../pleos/emulator-setup.md) "명령어로 설치하기: Mac"

## 시연 방법

```
emulator -avd pleos_connect -no-snapshot-load -no-snapshot-save -gpu host
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## 주의점 / 남은 일

- 디스크 여유가 32GB다. 에뮬레이터 데이터와 빌드 캐시가 늘면 부족해질 수 있다.
- 폰 AVD `Pixel_9_Pro_XL`과 같이 켜면 `/verify`가 기기를 고르지 못한다.
- 하지 않은 것:
  - `/stack-diagram`용 Python Playwright: 선택 사항이다.
  - BMAD `config.user.toml`의 이름: 개인 설정이라 본인이 쓴다.
