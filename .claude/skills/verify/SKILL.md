---
name: verify
description: 앱을 빌드·테스트하고 에뮬레이터에 설치해 스크린샷으로 화면을 직접 확인한다. 기능 구현이나 UI 변경을 마친 뒤 "완료"라고 말하기 전에 사용한다.
argument-hint: "[확인할 화면/기능]"
---

# verify: 빌드 → 테스트 → 실행 → 눈으로 확인

확인 대상: $ARGUMENTS

Windows PowerShell에서는 `.\gradlew.bat`, Bash에서는 `./gradlew`를 쓴다.

## 1. 정적 검증

```
./gradlew assembleDebug testDebugUnitTest lintDebug
```

- 실패하면 고치고 다시 돌린다.
- lint는 이번 변경으로 새로 생긴 경고만 처리한다.

## 2. 기기 확인

`adb devices`로 연결된 기기를 확인한다.

- 기기가 없으면 여기서 멈추고 사용자에게 에뮬레이터를 **Cold boot로** 켜달라고 요청한다. 에뮬레이터를 직접 띄우지 않는다.
  - 사용자가 켜 달라고 하면 Bash `run_in_background`로 띄운다: `/c/Android/emulator/emulator.exe -avd <AVD> -no-snapshot-load`
  - 부팅 완료는 `adb shell getprop sys.boot_completed`가 `1`이 될 때까지 기다린다.
- 1단계 결과는 그대로 보고한다.

## 3. 설치·실행

```
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n <applicationId>/<런처 Activity 전체 이름>
```

- applicationId는 `app/build.gradle.kts`에서, 런처 Activity는 `AndroidManifest.xml`에서 확인한다.
- Pleos 권한 팝업이 뜨면 사용자에게 알린다.

## 4. 화면 확인

PowerShell의 `>` 리다이렉트는 바이너리를 깨뜨리므로 pull 방식을 쓴다:

```
adb shell screencap -p /sdcard/verify.png
adb pull /sdcard/verify.png .claude/.cache/verify.png
```

- **Git Bash에서는 먼저 `export MSYS_NO_PATHCONV=1`을 한다.**
  - 이걸 하지 않으면 Git Bash가 `/sdcard/...` 같은 기기 경로를 Windows 경로로 바꿔 버린다.
  - 그러면 `screencap`이 사용법만 출력하고 실패한다.

Read 도구로 `.claude/.cache/verify.png`를 열어 직접 본다. 확인할 것:

- 확인 대상 화면·기능이 기대대로 나오는가
- 글자 잘림, 겹침, 빈 화면이 없는가
- 주행 중 화면에 정보가 과하지 않은가 (AGENTS.md 규칙 3)

다른 화면으로 가려면 `adb shell input tap <x> <y>` 또는 `adb shell input keyevent BACK`으로 이동한 뒤 다시 캡처한다.

- 좌표와 화면 글자는 스크린샷 대신 UI 트리에서 정확히 얻는다:

  ```
  adb shell uiautomator dump /sdcard/ui.xml
  adb pull /sdcard/ui.xml .claude/.cache/ui.xml
  ```

  `text="..."`와 `bounds="[x1,y1][x2,y2]"`를 읽는다.
  - 주행 화면에 입력칸이 없는지는 `android.widget.EditText` 개수로 확인한다.
  - 버튼이 화면 밖으로 밀려나면 트리에 아예 없다.
- 레이아웃은 최소 두 가지로 본다. Pleos는 세로 창과 가로·분할 화면을 모두 쓴다 (docs/pleos/ux-guidelines.md).
  - 세로
  - 가로: `adb shell settings put system accelerometer_rotation 0`, `adb shell settings put system user_rotation 1`
  - 다크 모드도 본다: `adb shell cmd uimode night yes`
  - 확인이 끝나면 되돌린다: `user_rotation 0`, `accelerometer_rotation 1`, `night no`

크래시나 SDK 에러는 로그로 확인한다:

```
adb logcat -d -t 300 *:E
```

## 5. 보고

- 실행한 명령과 각각의 결과 (통과/실패)
- 확인한 화면과 발견한 문제
- 확인하지 못한 것 (기기 없음, 권한 거부 등)은 그대로 적는다. 확인 못 한 걸 확인했다고 말하지 않는다.
