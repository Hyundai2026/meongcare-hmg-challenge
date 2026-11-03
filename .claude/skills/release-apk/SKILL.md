---
name: release-apk
description: 제출용 release APK를 빌드해 apk/<팀명>_<앱이름>_release.apk로 배치하고 README의 APK 항목을 갱신한다.
disable-model-invocation: true
---

# release-apk: 제출용 APK 만들기

대회 규칙 ([docs/contest/requirements.md](../../../docs/contest/requirements.md)):

- 실행 가능한 release 빌드여야 한다.
- 경로는 `apk/<팀명>_<앱이름>_release.apk`.
- 용량이 크면 GitHub Release에 첨부한다.

## 1. 사전 확인

- AGENTS.md의 팀명과 앱 이름이 `<팀명>`/`<앱이름>` 그대로면 멈추고 사용자에게 물어본다.
- `keystore.properties`가 있는지 확인한다. 존재 여부만 보고, 내용은 읽지 않는다.
  - 없으면 멈추고 사용자에게 알린다. 키스토어는 사용자가 만들고 팀 내에서 공유해야 한다.
  - 키를 임의로 생성하지 않는다. 나중에 같은 키로 업데이트해야 하기 때문이다.
- `app/build.gradle.kts`의 release `signingConfig`가 `keystore.properties`를 읽는지 확인한다.

## 2. 빌드

```
./gradlew assembleRelease
```

## 3. 서명 확인

```
<ANDROID_HOME>/build-tools/<버전>/apksigner verify --print-certs -v app/build/outputs/apk/release/app-release.apk
```

- 서명이 없으면(unsigned) 실패로 본다.
- 출력에서 v2/v3 서명 여부를 보고에 포함한다. minSdk 28에서 Pleos가 키 rotation/lineage를 요구하기 때문이다.

## 4. 배치

```
cp app/build/outputs/apk/release/app-release.apk "apk/<팀명>_<앱이름>_release.apk"
```

- 파일 크기를 확인한다.
- 95MB가 넘으면 커밋하지 말고, GitHub Release에 올려야 한다고 사용자에게 알린다. GitHub는 100MB에서 차단한다.

## 5. 실행 확인

에뮬레이터가 연결돼 있으면:

```
adb install -r "apk/<팀명>_<앱이름>_release.apk"
```

설치한 뒤 실행해서 크래시가 없는지 본다. `/verify`의 4단계처럼 스크린샷으로 확인한다.

## 6. README 갱신

README.md의 "APK" 섹션 경로를 실제 파일명으로 바꾼다.

## 7. 보고

- 다음을 보고한다:
  - 파일 경로와 크기
  - 서명 검증 결과
  - 실행 확인 여부
- 커밋은 하지 않는다. 사용자에게 커밋할지 물어본다.
  - 커밋한다면 작업 브랜치(예: `chore/release-apk`)에서 하고, `/ship`으로 dev에 반영한다.
  - 심사위원이 보는 main에 올리려면 사용자가 `/promote`를 실행해야 한다고 알린다.
