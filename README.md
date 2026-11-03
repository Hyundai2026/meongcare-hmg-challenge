# <앱 이름>

> <한 줄 소개>

2026 현대자동차그룹 차량용 앱 개발 공모전 출품작 · Pleos Connect

## 실행 방법

1. Pleos Connect 에뮬레이터를 설치한다. ([docs/pleos/emulator-setup.md](docs/pleos/emulator-setup.md))
2. 에뮬레이터를 **Cold boot**로 실행한다.
3. APK를 설치한다:

   ```
   adb install -r apk/<팀명>_<앱이름>_release.apk
   ```

4. 런처에서 `<앱 이름>`을 실행한다.

## 테스트 환경 및 필요 설정

- Pleos Connect Emulator v2.1.0 (Android API 34)
- <필요한 Virtual Console 설정, 권한 허용 등>

## APK

- [`apk/<팀명>_<앱이름>_release.apk`](apk/)
- 용량 문제로 Release에 올린 경우: <Release 링크>

## 소스에서 빌드하기

1. Android Studio(최신)와 JDK 17 이상을 준비한다.
2. `secrets.properties.example`을 복사해 `secrets.properties`를 만들고 Pleos Client ID/Secret을 채운다.
3. 빌드한다:

   ```
   ./gradlew assembleDebug
   ```

## 팀

- <이름 / 역할>
