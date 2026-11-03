# pleos-emulator-install

- 날짜: 2026-11-03
- 작성: xxj15
- 브랜치: docs/pleos-emulator-install
- 커밋: ae71c4b

## 무엇을 했나

xxj15 PC(RTX 4070 Laptop)에 Pleos Connect 에뮬레이터 v2.1.0을 설치했다. 우리 앱을 차량 화면에 띄워서 Fake·Real 모드가 둘 다 뜨는 것을 확인했다. 코드는 바꾸지 않았다.

## 어떻게: 핵심 결정과 이유

- **Android Studio 화면 대신 명령어로 설치했다** (sdkmanager, avdmanager). 다른 팀원도 같은 결과를 그대로 재현할 수 있게, 순서를 `docs/pleos/emulator-setup.md`에 적었다.
- **Pleos SDK 라이선스(42dot)에는 사용자가 동의했다** (2026-11-03). 설치 자체가 동의로 간주되는 약관이라, 설치 전에 요점을 설명하고 확인받았다.
- **PowerShell 파이프로는 라이선스 동의가 전달되지 않았다.** 처음 두 번은 설치를 건너뛰거나 패키지를 못 찾았다. `.cmd` 파일에서 `echo y|`로 넘겨서 해결했다.
- **avdmanager 기본값을 Pleos 안내에 맞게 고쳤다.** 기본값이 Quick boot, RAM 1.5GB, GPU 꺼짐, 데이터 800MB였다. 이를 Cold boot 강제, 4GB, GPU host, 6GB로 바꿨다.

## 사용한 Pleos SDK / API / 권한

- Pleos Connect 시스템 이미지 `system-images;android-34;pleos_car_emulator;x86_64` (revision 2)
- 앱 쪽 새 SDK·권한은 없다.

## 확인

- 테스트: 해당 없음 (코드 변경 없음)
- 화면 (adb 스크린샷으로 확인):
  - Pleos 홈: Driving view, 내비 앱, GNB가 보인다.
  - 우리 앱 Fake 모드: 정차 화면과 debug 패널이 정상이다.
  - 우리 앱 Real 모드: "주행 중" 화면이 뜬다. 주행 상태 Real 구현 전이라 UNKNOWN이 나오는 것으로, 설계대로다.
- Real 모드에서 Vehicle initialize 실패 로그가 없다.

## 시연 방법

1. `/c/Android/emulator/emulator.exe -avd pleos_connect -no-snapshot-load -no-snapshot-save -gpu host`
2. `adb -s <serial> install -r app/build/outputs/apk/debug/app-debug.apk`
3. 앱 목록에서 실행하거나 `adb -s <serial> shell monkey -p com.meongcare.app -c android.intent.category.LAUNCHER 1`

## 주의점 / 남은 일

- 다음 개발: 주행 상태 Real 구현 (`CarUxRestrictions`). Virtual Console에서 기어를 바꿔 화면이 바뀌는지 확인한다.
- 우리 앱이 반쪽 세로 창이 아니라 앱 영역 전체(가로)에 떴다. 문서 설명과 달라서 `ux-guidelines.md`에 관찰로 적었다. 실행 방법에 따라 다른지는 확인하지 않았다.
- 에뮬레이터를 두 대 켜면 `/verify`의 `adb` 명령이 기기를 고르지 못한다. 한 대만 켜 두거나 `ANDROID_SERIAL`을 설정한다.
- STT·Gleo가 에뮬레이터에 들어 있는지는 아직 확인하지 않았다.
