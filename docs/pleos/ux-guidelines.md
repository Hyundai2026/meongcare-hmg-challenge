# Pleos Connect UX·주행 안전 가이드: 검증된 사실

이 파일의 규칙:
- 공식 문서에서 확인한 것만 적고, 항목마다 출처를 단다.
- 확인하지 못한 것은 맨 아래 "미확인"에 둔다. 추측으로 채우지 않는다.
- SDK 좌표·API·권한은 [sdk-notes.md](sdk-notes.md)에 둔다.

마지막 갱신: 2026-11-03

## 화면 구성

### 화면 모델 [app-structure]

- 화면은 **Driving view 영역**과 **앱 영역**으로 나뉜다.
  - Driving view는 운전자 쪽 고정 영역이다. 앱이 가리거나 종료할 수 없다.
  - 앱 영역은 차량과 디스플레이 구성에 따라 window 1개 또는 2개로 나뉜다. **최대 2개 앱**을 동시에 띄운다.
- 앱 형태는 일반 Activity다. 화면 방향은 `AndroidManifest.xml`의 `screenOrientation`으로 정한다.

### 화면 방향 [app-structure] [design-layout]

- **`screenOrientation`을 지정하지 않으면 portrait이고 window 하나를 쓴다.**
- `screenOrientation`을 `landscape`로 지정하면 window 두 개 영역을 쓴다. 두 앱이 동시에 뜨면 각 앱은 보통 portrait이다.
- 권장: portrait에는 핵심 기능과 주요 정보를, landscape에는 부가 정보와 확장 기능을 둔다. [app-structure]
- 디자인 가이드: 한 방향 전용 앱(예: 가로 전용 게임)이 아니면 "Landscape, Portrait 두 가지 레이아웃에 적절하게 조절되도록 설계해야 합니다." [design-layout]
- 사용자가 window bar나 제스처로 위치와 크기를 바꿀 수 있다. 크기가 바뀌어도 핵심 기능을 쓸 수 있어야 한다. [app-structure]
- > **관찰 (2026-11-03, Pleos 에뮬레이터 v2.1.0):** `screenOrientation`을 지정하지 않은 우리 앱을 adb로 실행했더니 portrait 반쪽 창이 아니었다. Driving view 오른쪽 앱 영역 전체(1682×1210dp, 가로 설정)에 떴다. 내비 앱은 그 뒤로 갔다.
  > 문서의 "지정하지 않으면 portrait, window 하나"와 다르다. 실행 방법(adb 실행, 앱 목록에서 실행)이나 이미 떠 있던 앱에 따라 달라지는지는 확인하지 않았다.
  > → 세로 반쪽과 가로 전체 두 가지 레이아웃을 모두 만든다는 방향은 그대로다. [emulator-setup.md](emulator-setup.md#설치-후-확인한-것-관찰-2026-11-03)

### 멀티 윈도우 정책 [app-structure] [design-window]

- 새 앱은 현재 표시 중인 앱의 방향을 따른다. landscape 앱이 있으면 새 앱도 landscape, portrait 앱 두 개가 있으면 새 앱은 primary window에 portrait로 뜬다. [app-structure]
- 분할 비율: 앱 2개면 1:1(기본), 앱 1개면 2:0. 후방 카메라 같은 3:0 전체화면 예외가 계획되어 있다. [design-window]
  - 원문: "앱 2개 실행 시: 1:1"
- 3손가락 스와이프나 window bar로 두 앱 위치를 바꾸거나 landscape/portrait를 전환한다. 3손가락 스와이프로 window를 닫는다. [design-window]
- Navigation 앱은 닫을 수 없다. 첫 실행 시 primary는 Navigation, secondary는 Media다. [design-window]
- "Driving view의 landscape view는 삭제되었고, resize handle이 제공되지 않습니다." [design-window]
- 딥링크로 앱 A가 앱 B를 띄우면 B가 A를 덮고, B의 task가 끝나면 A로 돌아온다. [design-window]
- Connect-W: 실행 앱 최대 2개, 운전석·조수석 양쪽에 App library, 조수석 App library에서 띄운 앱은 조수석 window에서만 실행된다. [design-window]

> **불일치:** Window Policy 페이지는 "새 앱은 기존 앱과 같은 방향으로 뜬다"는 정책과 "새 앱은 항상 primary window에 landscape로 뜨고 기존 primary는 secondary로, 기존 secondary는 제거된다"는 정책을 둘 다 적고 있다. 어느 쪽이 현재 적용되는지는 문서에 없다. "앱 구조 및 동작 모델" 페이지는 전자만 적는다. [design-window] [app-structure]

### 디스플레이 종류와 크기 [design-layout] [form-factor-test]

- Connect-S (Standard): "17인치, 14.6인치, 12.9인치(Connect-L) 세 가지 크기를 제공합니다." [design-layout]
- Connect-W (Wide): Connect-S보다 넓고 주로 고급 차종에 들어간다. 차량 상태에 따라 높이가 늘어나는 Connect-W-Extended가 있다. [design-layout]
- Connect-WF (Waterfall): Wide보다 해상도가 낮다. 수치는 없다. [design-layout]
- 상태바, GNB의 App library·공조 패널, 키보드 같은 System UI가 앱 위에 떠서 앱 window를 잠시 가릴 수 있다. [design-layout]
- 차종마다 해상도와 화면비가 다르므로 다양한 해상도에 맞게 설계한다. 다국어를 지원한다. [design-intro]
- 폼팩터 테스트 가이드의 예시 프리셋: 12.9인치 Portrait, 14.6인치 Landscape. dp/DPI/px 값은 테스트 도구 화면에서 확인하라고만 한다. [form-factor-test]

### 해상도 관련 단서 (공식 스펙 아님)

- 앱 심사 제출의 IVI 스크린샷 권장 해상도: "2560 x 1380 px", "1700 x 1212 px", "840 x 121 px" (문서 표기 그대로). PNG/JPEG, 장당 최대 8MB, 최대 8개. [app-submission]
  - > **불일치:** 심사 가이드의 등록정보 페이지는 세 번째 값을 "840 xX 1212 px"로 적고 '규격'이라고 부른다 (원문: "해상도 규격 : 2560 xX 1380 px, 1700 xX 1212 px, 840 xX 1212 px"). 최소 2장, 최대 8장. [review-registration]
- 공식 vehicle 샘플 README의 스크린샷은 "2560 x 1440 Android 14 emulator"에서 찍었다. [gh-vehicle]
- 디스플레이 해상도(px)와 밀도(dpi) 공식 표는 찾지 못했다 → 미확인.

### 생명주기와 백그라운드 [app-structure]

- 화면에서 사라진 앱은 종료되지 않고 백그라운드에 남을 수 있다. 표시 중인 앱이 닫히면 최근 사용 순으로 다른 앱이 다시 뜬다.
- 포그라운드 진입 시: 상태를 복원하고 화면 크기·방향에 맞게 UI를 다시 구성한다.
- 백그라운드 전환 시: 화면 갱신, 애니메이션, 불필요한 센서 수집을 멈춘다.
- 백그라운드 상주, 타이머, 폴링, 네트워크 요청을 최소화한다. 프로세스가 재생성되어도 사용자 데이터와 진행 상태를 복원한다.

## 주행 규제 (CarUxRestrictions)

### DO(Distraction Optimization) 미선언 앱 [driving-policy]

- "앱에서 Distraction Optimization(DO)를 선언하지 않았을 때, 주행 상황에 따라 앱 사용 가능 여부가 달라집니다."

| 상황 | DO 미선언 시 앱 사용 |
|---|---|
| Parking | 사용 가능 |
| Driving | 사용 불가 |

- "주행 규제는 차량의 속도·기어·브레이크 등 하드웨어 신호와 지역별 안전 규정에 따라 적용됩니다." 플랫폼 버전과 양산 차량에 따라 정책이 다를 수 있다. [driving-policy]
- **DO를 선언하는 방법은 Pleos 문서에 없다.** Pleos는 "AAOS 운전자 주의 분산 행동 가이드라인을 준수"하라며 Google 문서를 링크한다 [driver-distraction]. 그 Google 문서의 선언 방식은 아래와 같다 [aaos-dd]:

```xml
<activity android:name=".DistractionOptimizedMainActivity"....>
....
<meta-data android:name="distractionOptimized" android:value="true"/>
</activity>
```

  - Google 문서 원문: "Current foreground activity can continue to run only when it is tagged as Distraction Optimized in the manifest." 플랫폼은 매니페스트 선언만 확인한다. [aaos-dd]
  - Pleos 에뮬레이터/실차에서 이 선언이 그대로 동작하는지는 Pleos 문서로 확인하지 못했다 → 미확인.
  - 2026-11-03 재확인: 주행 규제 개발 가이드, 지역별 주행 규제 정책, CCP 개발 가이드, Maps/Navigation·Music/Audio 앱 개발 가이드 어디에도 `distractionOptimized`나 선언 방법이 없다. 주행 규제 개발 가이드에는 "DO 미선언 앱의 상황별 사용 가능 여부는 지역별 주행 규제 정책을 참조하세요."만 있다. [driving-restriction-dev] [driving-policy] [ccp-guide] [navi-app-guide] [music-app-guide]
  - 앱 카테고리에 따라 DO 선언이 허용·거부되는지도 문서에 없다 → 미확인.

### 지역별 기능 허용 범위 (Driving 상태) [driving-policy]

Parking 상태에서는 모든 지역에서 모든 기능이 허용된다.

| 기능 | 북미 | 유럽 | 그 외 지역 | 관련 UX restriction |
|---|---|---|---|---|
| 키보드 | ⛔ | ✅ | ✅ | `UX_RESTRICTIONS_NO_KEYBOARD(8)` |
| 다이얼패드 | ⛔ | ✅ | ✅ | `UX_RESTRICTIONS_NO_DIALPAD(1)` |
| 검색 | 음성한정 | ✅ | ✅ | `UX_RESTRICTIONS_NO_KEYBOARD(8)`, `UX_RESTRICTIONS_NO_FILTERING(2)` |
| 미디어 앱 로그인, 설정, 검색 | ⛔ | ⛔ | ✅ | `UX_RESTRICTIONS_NO_SETUP(64)`, `UX_RESTRICTIONS_NO_KEYBOARD(8)`, `UX_RESTRICTIONS_NO_FILTERING(2)` |
| 디지털키, 도어, 시트, 차량 설정 | ⛔ | ⛔ | ⛔ | `UX_RESTRICTIONS_NO_SETUP(64)` |
| 동영상 시청 | ⛔ | ⛔ | ⛔ | `UX_RESTRICTIONS_NO_VIDEO(16)` |
| 내비게이션 검색 | ⛔ | ✅ | ✅ | `UX_RESTRICTIONS_NO_KEYBOARD(8)`, `UX_RESTRICTIONS_NO_FILTERING(2)` |
| QR 로그인 | ⛔ | ⛔ | ⛔ | `UX_RESTRICTIONS_NO_SETUP(64)` |
| 매뉴얼 (대용량 텍스트) | ⛔ | ⛔ | ⛔ | `UX_RESTRICTIONS_LIMIT_CONTENT(32)`, `UX_RESTRICTIONS_LIMIT_STRING_LENGTH(4)` |

- "표의 「검색」은 웹 검색이 아니라, 차량 인포테인먼트 시스템 내 통합 검색을 의미합니다."
- **한국은 별도 항목이 없다.** 표는 북미/유럽/그 외 지역으로만 나뉜다. 한국이 "그 외 지역"에 해당한다는 문장은 없다 → 미확인.
- 지역 코드는 `product.sw.target_region`(`KOR`, `EUR`, `USA`/`CAN`)으로 읽는다. [faq]
- 스크롤 제한과 속도·기어 임계값은 이 페이지에 없다.

### 앱이 주행 규제 상태를 아는 방법 [driver-distraction] [driving-restriction-dev]

- "앱은 `CarUxRestrictions`를 기준으로 주행 중 제한 상태를 확인하고, 제한 대상 기능의 UI와 동작을 적절히 축소하거나 비활성화해야 합니다." [driver-distraction]
- "`CarUxRestrictionsManager`에 `registerListener()`를 등록하면 `onUxRestrictionsChanged()` 콜백으로 변경된 `CarUxRestrictions`를 수신합니다." [driving-restriction-dev]
- "DO를 선언한 앱도 주행 규제가 활성화되면 `CarUxRestrictions`와 `ActiveRestrictions`를 확인하여 기능별 UX 제한을 직접 적용해야 합니다." [driving-restriction-dev]
- 플랫폼이 지역별 사양에 맞는 `ActiveRestrictions`를 내려주므로 앱은 받은 값만 반영하면 된다. [driving-restriction-dev]
- 동영상 재생 제한 예시 (원문 코드 블록 일부 발췌) [driving-restriction-dev]:

```
if (mCurrentCarUxRestrictions.isRequiresDistractionOptimization()
        && (mCurrentCarUxRestrictions.getActiveRestrictions()
        & CarUxRestrictions.UX_RESTRICTIONS_NO_VIDEO) != 0)
    stopPlayingVideo();
```

- Vehicle SDK의 DrivingMode는 주행/정차 상태가 아니라 "드라이브 모드, 크루즈 컨트롤 정보 조회"다. 속도·기어는 AAOS Car API로 읽는다. 자세한 내용은 [sdk-notes.md](sdk-notes.md#속도기어주행-상태)에 있다.

## 주행 안전 설계 원칙 [driver-distraction]

- "AAOS 운전자 주의 분산 행동 가이드라인을 준수하고, 아래 UX 원칙에 유의해 설계·개발합니다."
- 사용 가능 범위와 기능 제한은 주행 상태, 지역, 앱 카테고리에 따라 다르다.
- 영상: "주행 규제 상태에서 영상 재생을 시작하거나 재개하지 않도록 처리하고"
- "주행 중에는 운전자의 시선을 과도하게 끄는 동영상, 애니메이션 및 움직이는 이미지를 최소화하거나 제한합니다." 자동 애니메이션과 반복되는 화면 전환을 피한다.
- 오디오는 오디오 포커스 정책을 따른다. 중요한 음성 안내가 미디어를 멈추거나 볼륨을 조절할 수 있다.
- 큰 터치 타깃, 높은 대비, 최소한의 텍스트를 쓴다. 버튼 크기와 간격은 Pleos Connect UI Design Guide를 따른다. 이 페이지에는 수치가 없다.
- 운전 정보는 "짧은 시간 안에" 이해할 수 있어야 한다.
- 검색·로그인에도 키보드 입력보다 프리셋 선택, 최근 항목, 즐겨찾기, 음성 입력을 우선한다.
- 자주 쓰는 기능은 첫 화면 또는 상단에 둔다. 드롭다운과 다단계 메뉴를 남용하지 않는다.
- 차량이 제공하면 음성 명령과 물리 버튼을 지원한다.
- "게임, 인터넷 서핑, 피트니스 등 운전자의 주의를 분산시킬 수 있는 활동을 유도하지 않습니다."

### 앱 심사의 주의분산 기준 [review-distraction] [app-review]

- 운전 중 운전자와 계속 상호작용하는 앱은 주의분산을 최소화하도록 설계해야 한다. 제출 전에 Pleos Connect UI Design Guide를 확인한다. 가이드를 따라도 심사에서 추가 위험이 발견되면 거부될 수 있다. [review-distraction]
- "차량 운행 안전에 위험 요소 또는 안전상의 문제를 초래할 가능성이 있다고 판단되는 경우 심사가 거부될 수 있습니다." [app-review]
- 미디어 앱(동영상, 게임, 브라우저) [review-distraction]:
  - "모든 미디어 앱은 정차 중에만 사용 가능합니다."
  - "운전이 시작되면 앱의 UI를 숨깁니다." 재생은 일시 중지한다.
  - "운전 중에는 동영상, 게임, 브라우저 앱의 '일시 중지'를 해제할 수 없습니다."
  - "운전과 관련되지 않은 알림은 사용할 수 없습니다." (헤드업 알림 금지)
  - 백그라운드에서 돌아오면 마지막 상태를 복원한다.
  - 동영상 앱은 미디어 세션과 연동하고, 여러 미디어를 동시에 재생하지 않는다.

## 앱 카테고리와 주행 중 허용 범위 [app-category]

- 카테고리: Maps/Navigation, Mobility Services, Lifestyle, Entertainment, Business, Music/Audio, Kids, Social/Communication
- "앱 카테고리와 주행 상태에 따라 주행 중 사용할 수 있는 기능의 범위가 달라질 수 있습니다."
- 주행 중 제한적으로 허용될 수 있는 기능 예시 (실제 적용은 주행 상태와 지역 정책에 따름):
  - Music/Audio: 앨범 아트 같은 단순 시각 정보, 재생/일시정지/건너뛰기, 오디오 스트리밍, 라디오 채널 변경, 오디오북 재생
  - Maps/Navigation: 경로 안내와 실시간 교통, "간결한 지도", TBT 음성 안내, 경로 검색, 더 빠른 경로 안내, 주변 교통 정보
  - Mobility Services: 주행과 밀접한 차량 관리, 즉시 필요한 위치 기반 정보, EV 충전소 검색과 경유지 설정, 주변 주유소 검색, 주행 중 갈 수 있는 주차 위치 확인
- Lifestyle, Entertainment, Business, Kids, Social/Communication의 주행 중 허용 범위는 이 페이지에 없다.
- 앱 심사 제출 시 Pleos App Market 카테고리를 필수로 고른다. [app-submission]
- 카테고리별 핵심 목적 (원문) [app-category]:
  - Maps/Navigation: "위치 기반 길 안내"
  - Mobility Services: "이동 수단/이동 서비스 제공"
  - Lifestyle: "일상생활 관리 및 향상"
  - Entertainment: "오락 및 여가 제공"
  - Business: "업무 및 생산성 지원"
  - Music/Audio: "오디오 콘텐츠 중심"
  - Kids: "아동 대상 콘텐츠"
  - Social/Communication: "사람 간 소통 및 네트워크"
- 의료·건강·반려동물 카테고리나 예시 앱은 없다. [app-category]

### 카테고리 선언과 심사 [navi-app-guide] [music-app-guide] [review-registration]

- Maps/Navigation 앱 가이드 원문: "모든 앱은 개발 및 등록 시 앱 카테고리를 필수적으로 선언해야 합니다." 매니페스트 인텐트 필터로 선언한다 (원문 코드):

```
<intent-filter>
<action android:name="android.intent.action.MAIN" />
<category android:name="android.intent.category.APP_MAPS" />
</intent-filter>
```

- Music/Audio 앱 가이드는 다른 방식을 쓴다: "`<application>` 요소의 `android:appCategory` 속성을 `"audio"`로 설정합니다." → `<application  ...  android:appCategory="audio" />` [music-app-guide]
- Lifestyle, Mobility Services 등 나머지 카테고리의 매니페스트 선언 값은 문서에 없다 → 미확인.
- "선언된 카테고리와 실제 기능 불일치 시 심사에서 즉시 반려됩니다." "만약 앱 심사 중 기능이 변경된다면 반드시 재심사를 요청해야 합니다." [navi-app-guide]
- 등록정보 심사: "카테고리 지정 시에는 앱에 가장 적합한 카테고리를 선택해야 합니다." 맞지 않으면 심사 측이 임의로 바꿀 수 있다. [review-registration]

### Maps/Navigation 앱 가이드의 주행 중 조작 기준 [navi-app-guide]

Maps/Navigation 카테고리 앱 대상 문서다 ("Pleos Connect의 Maps/Navigation 앱을 개발할 때 반영해야 할 기본 정책은 다음과 같습니다."). 다른 카테고리에 같은 기준이 적용되는지는 문서에 없다.

- 주행 중 조작 금지: "복잡한 메뉴 탐색 (3 depth 초과)", "긴 리스트 스크롤", "영상, 애니메이션, 광고 노출"
- 주행 중 허용: "음성 입력", "단일 터치 기반 주요 기능", "즐겨찾기 목적지 선택"
- 권한: "위치 권한: 필수", "마이크: 음성 입력 시에만 허용", "불필요한 권한 요청 시 심사에서 반려됩니다."
- 시스템 연동: "Pleos Connect 및 AAOS 공식 API 사용을 권장합니다." "비공식 센서/API 사용은 금지되어 있습니다."
- UX 심사: "터치 영역 최소 크기 충족", "주행 핵심 정보 상시 노출", "주행 중 전체 화면 팝업 금지"
- 즉시 반려: "주행 중 광고 노출", "영상·게임 요소 포함", "복수 내비 동시 경로 안내", "카테고리 위반 기능 제공", "OEM/AAOS 정책 위반"
- 단일 경로 정책: "AAOS 환경에서는 하나의 Navigation 앱만 Active Route 상태를 가질 수 있기 때문에 복수 Navigation 앱의 음성 안내·경로 출력은 허용하지 않습니다." `CarAppFocusManager.requestAppFocus`로 경로 소유권을 다룬다.
  - 참고: NaviHelper SDK는 경로 요청을 Pleos Connect Navigation 앱으로 보내고 결과를 콜백으로 돌려준다. [navi-intro] (sdk-notes.md의 NaviHelper 절)

### 음성 입력에 대한 다른 근거

- 주행 안전 설계 원칙: 검색·로그인에 키보드보다 프리셋, 최근 항목, 즐겨찾기, 음성 입력을 우선한다. [driver-distraction]
- 지역별 주행 규제 표: 북미의 "검색"은 주행 중 "음성한정"("`음성한정`은 음성으로만 사용 가능함을 의미합니다."). [driving-policy]

## 오디오 포커스와 음성 안내

### 오디오 포커스 [audio-focus]

- "각 오디오 스트림은 재생 시작 전 `AudioAttributes` + `AudioFocusRequest`로 포커스를 요청해야 합니다."
- "C를 기대하는 컨텍스트(예: NAVIGATION이 MUSIC과 동시)라면 요청은 원칙적으로 `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK`을 사용해야 합니다."
- 포커스 상실: `AUDIOFOCUS_LOSS`는 즉시 중단, `AUDIOFOCUS_LOSS_TRANSIENT`는 일시정지 후 복귀 가능, `AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK`은 볼륨 감쇄.
- Gleo 음성 명령: "Gleo 음성 명령은 일반 오디오보다 우선 처리하되, EMERGENCY, CALL 등 매트릭스에서 Reject로 정의된 컨텍스트에는 우선하지 않습니다." "Gleo가 호출되어 앱이 `AUDIOFOCUS_LOSS_TRANSIENT`를 수신하면, 즉시 재생을 중단하고 필요 시 오디오 포커스를 반납(abandon)해야 합니다."
- 컨텍스트별 usage 매핑 (페이지 표, `USAGE_` 접두어 없이 표기):

| CarAudioContext | AttributeUsages |
|---|---|
| MUSIC | `MUSIC`, `UNKNOWN`, `GAME`, `MEDIA` |
| NAVIGATION | `NAVIGATION`, `ASSISTANCE_NAVIGATION_GUIDANCE` |
| VOICE_COMMAND | `VOICE_COMMAND`, `ASSISTANT` |
| CALL_RING | `CALL_RING`, `NOTIFICATION_RINGTONE` |
| CALL | `CALL`, `VOICE_COMMUNICATION`, `VOICE_COMMUNICATION_SIGNALING` |
| ALARM | `ALARM` |
| NOTIFICATION | `NOTIFICATION`, `NOTIFICATION_*` |
| SYSTEM_SOUND | `SYSTEM_SOUND`, `ASSISTANCE_SONIFICATION`, `ASSISTANCE_ACCESSIBILITY` |
| EMERGENCY | `EMERGENCY` |
| SAFETY | `SAFETY` |
| VEHICLE_STATUS | `VEHICLE_STATUS` |
| ANNOUNCEMENT | `ANNOUNCEMENT` |

- 검증 예: CALL 중 MUSIC 요청은 R(실패), MUSIC 중 CALL 요청은 E(통화가 선점), EMERGENCY 중 MUSIC/NOTIFICATION 요청은 R.
- Interaction Matrix의 셀 값은 이미지로만 있다. 서드파티 앱이 쓸 수 있는 usage 목록은 문서에 없다 → 미확인.

### TTS·음성 안내 선행 묵음 [voice-preroll]

- 적용 범위: TTS 음성 안내, 녹음 음원 안내 멘트, 내비게이션·알림·상태 안내 등 모든 음성 출력.
- 선행 묵음은 최소 150ms, 기본 권장 150~200ms. 오디오 데이터 앞에 무음 패딩을 넣는 방식을 우선하고, 재생 지연만으로 처리하는 방식은 권장하지 않는다.
- Pleos TextToSpeech SDK가 이를 자동으로 처리하는지는 문서에 없다 → 미확인.

## 앱 심사·보안·개인정보 정책

### 개인정보 처리방침과 약관 [review-registration] [app-submission] [review-privacy]

- "사용자의 개인정보 또는 위치정보를 활용하는 앱은 서비스 이용 약관과 개인정보처리 방침을 함께 제출해야 하며, 앱의 개발자와 관리자가 적법한지 증명할 수 있어야 합니다." [review-registration]
- "앱 전용 방침이 없는 경우, 해당 앱에 적용되는 회사 공통 개인정보처리방침을 제출할 수 있습니다." [review-registration]
- "앱에서 위치정보 서비스를 사용하는 경우, '위치정보 이용약관 및 기타 정보 처리방침'을 함께 제출해야 합니다." [review-registration]
- 제출 폼의 개인정보처리방침 항목(필수): "개인정보처리방침을 확인할 수 있는 URL 입력", "서비스 국가 또는 지역별 언어로 작성된 내용을 모두 확인이 가능해야 함" [app-submission]
- 동의: 수집 항목, 처리 목적, 보유 기간, 거부권을 알리고 명시적 동의 절차를 구현한다. "제시된 동의 버튼을 누르는 행동 외의 행동을 동의로 간주해서는 안 됩니다." 접근권한은 "서비스에 필요한 범위 내로 최소화해야 합니다." [review-privacy]
- 제3자 제공 시 제공처와 목적을 알린다. 사용자 동의 없이 데이터를 빼내거나 제3자에게 보내는 행위는 금지다. [review-privacy]
- 위치정보를 저장·이용하면 국가별 법령에 따른 등록·신고 내용 확인을 요청받을 수 있다. [review-privacy]
- 설계 원칙: "앱에서 사용자의 위치, 음성, 계정 정보 등을 요구할 때 “왜 필요한지” 명확히 해야 하며, 동의 플로우를 간단하고 이해하기 쉽게 설계해야 합니다." "불필요한 개인정보(PII)는 저장하지 않고, 로그에 개인정보가 포함될 경우 마스킹하여 기록합니다." "차량은 여러 사람이 함께 쓰는 기기이므로, 보안 및 개인 정보 보호에 유의하여 앱을 설계해야 합니다." [security-privacy]
- 건강정보·민감정보에 대한 별도 기준은 개인정보 심사 페이지와 설계 원칙 페이지에 없다 → 미확인. [review-privacy] [security-privacy]

### 외부 서버·네트워크 [app-submission] [review-registration] [app-security] [hw-access]

- 외부 서버 호출을 금지하는 문장은 찾지 못했다. 외부 서버를 전제로 한 조건만 있다:
  - "외부 서버와 연결이 필요한 앱인 경우 심사 중 바로 서비스 연결이 가능하도록 앱 제출 전 앱 운영에 필요한 구성 요소들을 미리 활성화해주시기 바랍니다." [app-submission]
  - "웹 앱과 같이 웹 페이지를 연결 또는 활용하는 앱의 경우, 해당 웹 페이지의 도메인 등록 증빙을 제출해야 합니다." [review-registration]
  - 로그인이 필요하면 "모든 기능 및 콘텐츠에 대한 접근이 활성화된 계정을 제공 바랍니다." [app-submission]
- 보안 가이드 [app-security]:
  - "외부 통신에는 HTTPS 등 암호화가 적용된 안전한 프로토콜을 사용해야 합니다."
  - "TLS를 사용하는 통신에는 최소 TLS 1.2 이상을 사용해야 하며, TLS 1.3 사용을 권장합니다."
  - "앱에서 연결하는 서버는 서비스 제공에 필요한 신뢰할 수 있는 대상이어야 합니다."
  - "인증 토큰, 비밀 키 등 보안상 비밀로 관리해야 하는 정보가 소스코드, 리소스 또는 설정 파일에 하드코딩되지 않도록 해야 합니다."
  - "인증 토큰은 Authorization 헤더 등 URL에 노출되지 않는 적절한 방식으로 전달해야 합니다."
  - "기기에 저장하는 민감정보는 저장 위치와 관계없이 암호화하여 보호해야 합니다."
  - "앱 출시 전에는 관련 로그 출력을 비활성화하거나 민감정보를 마스킹 처리해야 합니다."
  - "배포용 앱에는 테스트 또는 디버그용 인증서 대신 정식 배포용 서명 인증서를 사용해야 합니다."
  - "최종 배포 앱에서는 앱 디버깅, 디버그 빌드 설정 및 WebView 원격 디버깅 기능을 비활성화해야 합니다."
- 주행 중 네트워크: "주행 중 셀룰러 또는 Wi-Fi 연결이 끊기거나 대역폭이 급격히 낮아지는 상황에 대비해야 합니다." 로컬 캐싱, 재시도·타임아웃, 오프라인 안내 UI를 권장한다. [hw-access]
- Maps/Navigation 앱 가이드(해당 카테고리 대상): "위치 데이터는 암호화하여 처리해야 합니다." "HTTPS 통신을 사용해야 합니다." "개인정보 처리방침을 반드시 제공해야 합니다." "불필요한 위치 데이터 저장은 금지됩니다." [navi-app-guide]
- LLM API, 지도·장소 검색 API 같은 특정 외부 서비스에 대한 언급은 없다 → 미확인.

### 의료·건강 관련 콘텐츠 [review-content]

- 의료, 진단, 응급, 반려동물에 대한 별도 심사 기준은 콘텐츠 안전 페이지에 없다.
- 관련 원문:
  - "적법한 승인이 되지 않은 식/약품이나 건강 또는 의료기구 등 처방전 없이 처방약을 판매 또는 구매를 용이하게 해서는 안 됩니다." (승인되지 않은 물품)
  - "사용자의 신체에 손상을 입히거나, 위험한 상황에 처하게 만드는 내용의 콘텐츠가 포함되어서는 안 됩니다." (위험한 내용의 콘텐츠)
- 앱 설명: "App Market 실적, 순위, 매출 등 사실 확인이 힘든 내용이 포함되어서는 안 됩니다." 의료 표현(예: "진단")을 따로 제한하는 문장은 없다. [review-registration]

### 성능 기준 [review-function]

- 앱은 10초 이내에 실행되고 콘텐츠를 로드해야 한다. 특정 버튼은 2초 이내에 반응하거나 동작 중임을 UI로 알려야 한다. "입력 후 차량 시스템의 응답 시간이 0.25초를 넘지 않게 하세요."
- 사용자에게 기기 재시작이나 핵심 기능과 무관한 시스템 설정 변경을 요구하지 않는다.

## 디자인 가이드

### 리소스 [design-intro]

- Figma 파일:
  - Pleos Connect display guide: https://document.pleos.ai/img/Pleos_Connect_display_guide_215.fig
  - Pleos Connect design assets: https://document.pleos.ai/img/Pleos_Connect_design_assets_215.fig
- 디자인 가이드 섹션: Getting Started(Display Layout, Status bar, GNB, App library, Window Policy), Foundations(Colors, Typography, Iconography, App Icon, Animation, Shadow), Components(Basic Button 등)

### 텍스트와 가독성 [design-intro] [design-typography]

- 주요 정보(속도, 내비게이션, 경고) 글자 높이 "5 mm 이상", 보조 텍스트 3 mm 이상. [design-intro]
- 글자 수: 영어 한 메시지 최대 120자 또는 24단어, 일본어 최대 30자, 한 줄 최대 80자, 문장·문단은 3줄 이하. [design-intro]
- 폰트: Asta Sans (Google Fonts, 한글·영문 지원). System UI 폰트로 설치되어 있다. [design-typography]
- 최소 폰트 크기 12sp (Android 접근성 기준). 실제 픽셀 높이 "약 18~20px 정도의 높이를 확보하는 것이 안전합니다.", "실제 픽셀 기준 최소 높이: 18px 이상". [design-typography]
- 가독성을 위해 "1.5배 이상" 배율을 권장한다 (어느 속성인지 본문에 명시 없음). [design-typography]
- Headline/Title/Body/Label 타입 스케일 수치는 SVG 이미지에만 있다 → 미확인. [design-typography]

### 색상과 테마 [design-intro] [design-colors]

- 대비: 아이콘, 텍스트, 이미지 대비율 4.5:1 이상. 색을 과하게 쓰지 않는다. [design-intro]
- "야간/주간에 따른 조명 환경에 따라 UI 밝기와 대비를 자동 조정하여 가독성을 유지합니다." [design-intro]
- 라이트·다크 테마가 있다. Static Color는 "라이트, 다크 테마에 영향을 받지 않으며, 항상 raw값을 유지하는 컬러입니다." [design-colors]
- System Color 토큰 분류: Background, Button, Controller, Climate, Dropdown, Dim, Driving, Fields, Gleo AI, Icon, Informative, Keyboard, Media, Overlay, Phone, Regulation, Surface, Switch, Text. 실제 색상값은 이미지에만 있다. [design-colors]
- 기본 테마가 다크인지는 문서에 없다 → 미확인.

### 응답 시간 [design-intro]

- 사용자 입력 후 시스템 응답은 0.25초를 넘기지 않는다. 더 걸리면 "응답 중" 상태를 보여준다.
- 운전자가 1~2초 안에 정보를 파악할 수 있어야 한다.

### 버튼 [design-button]

- 상태: Pressed("터치로 클릭 중인 상태일 때 True로 전환"), Disabled("True일 경우, Button이 제어불가능한 상태"), Loading("True일 경우, Button내 Loader가 표시되고 제어불가한 상태가 됨")
- 높이·최소 터치 타깃·패딩·라운드 수치는 이미지(`basic_size.svg`)에만 있다 → 미확인.

## 텍스트 입력과 IME [ime-guide]

- IME가 뜨면 IME inset만큼 view가 줄어드는 것은 "Pleos Connect 플랫폼 정책이 아니라 AOSP 기본 동작입니다."
- EditText/TextField가 있는 window: "IME 표시 시 view가 IME에 의해 위로 밀려 올라가는 것이 사양입니다."
- EditText/TextField가 없는 window: "IME 표시 시 view가 앵커링되지 않아야 합니다." 앱 수정이 필요하다.
- Dialog는 inset 적용 대상을 `statusBars`와 `navigationBars`로 한정해 IME inset 영향을 받지 않게 한다 (App Dialog 예시):

```kotlin
val dialog = Dialog(this)
val window = dialog.window
window?.let { win ->
    val params = win.attributes
    val typesToFit = WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars()
    params.setFitInsetsTypes(typesToFit)
    window.attributes = params
}
dialog.setContentView(R.layout...)
dialog.show()
```

- Activity 유형은 "아래 Google 가이드를 기준으로 별도 처리 검토가 필요합니다." Compose(`imePadding` 등) 안내는 없다.

## 미확인

- [ ] 디스플레이별 해상도(px), 밀도(dpi), dp 기준 window 크기 (Display Layout, Window Policy, 폼팩터 테스트 가이드 모두 수치 없음)
- [ ] 최소 터치 타깃 크기와 버튼 치수 (Basic Button 페이지의 이미지에만 있음)
- [ ] 타입 스케일 수치, 색상값, 기본 테마가 다크인지
- [ ] Pleos 문서 기준의 DO 선언 방법. Google 방식(`distractionOptimized` meta-data)이 Pleos에서 그대로 동작하는지
- [ ] 한국(`KOR`)에 적용되는 주행 규제 열 (표에는 북미/유럽/그 외 지역만 있음)
- [ ] 주행 중 스크롤·리스트 길이 제한, 주행 판정 속도 임계값
- [ ] Virtual Console에서 기어/속도를 바꾸면 `CarUxRestrictions`가 바뀌는지 (테스트 방법)
- [ ] Window Policy의 두 가지 앱 실행 정책 중 현재 적용되는 것
- [ ] Lifestyle, Mobility Services 등 Maps/Navigation·Music/Audio 외 카테고리의 매니페스트 선언 값과 주행 중 허용 범위 (2026-11-03 조사, 문서에 없음)
- [ ] Maps/Navigation 앱이 아닌 앱도 주행 중 "음성 입력"이 허용되는지 (허용 목록은 Maps/Navigation 앱 가이드에만 있음)
- [ ] 카테고리에 따라 DO 선언이 허용·거부되는지
- [ ] 건강·의료 정보(반려동물 병력·복용약 포함)에 대한 심사 기준, "진단" 같은 의료 표현 제한 (콘텐츠 안전·개인정보 심사 페이지에 없음)
- [ ] LLM API, 지도·장소 검색 API 등 외부 서비스 호출에 대한 별도 조건 (금지 문장은 못 찾음. HTTPS/TLS 1.2+, 도메인 증빙(웹 페이지 연결 시), 심사 중 서버 활성화 조건만 확인)
- [ ] 서드파티 앱이 쓸 수 있는 `AudioAttributes` usage 목록과 Interaction Matrix 셀 값 (이미지에만 있음)
- [ ] 화면 스크린샷 규격 세 번째 값: "840 x 121 px"(제출 가이드) vs "840 xX 1212 px"(심사 가이드)

## 출처

- [app-structure] https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/app-structure-screen-model
- [app-category] https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/app-category
- [driver-distraction] https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/driver-distraction-guideline
- [driving-policy] https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/driving-restiction-policy
- [driving-restriction-dev] https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/driving-restriction-development-guide
- [ime-guide] https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/ime-window-resize-handling-guide
- [faq] https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/vehicle-app-faq
- [design-intro] https://document.pleos.ai/docs/connect/guide/docs-design/Introduction/intro
- [design-layout] https://document.pleos.ai/docs/connect/guide/docs-design/getting-started/layout
- [design-window] https://document.pleos.ai/docs/connect/guide/docs-design/getting-started/window
- [design-typography] https://document.pleos.ai/docs/connect/guide/docs-design/foundations/typography
- [design-colors] https://document.pleos.ai/docs/connect/guide/docs-design/foundations/colors
- [design-button] https://document.pleos.ai/docs/connect/guide/docs-design/components/actions/basic-button
- [form-factor-test] https://document.pleos.ai/docs/pleos-only/app-test-guide/form-factor-test-guide
- [app-review] https://document.pleos.ai/docs/pleos-only/distribute/app-review-guide
- [review-distraction] https://document.pleos.ai/docs/pleos-only/distribute/app-review-guide/review-driver-distraction-elements
- [app-submission] https://document.pleos.ai/docs/pleos-only/distribute/app-submission-guide
- [gh-vehicle] https://github.com/Pleos-Playground/pleos-sample-vehicle-app (Pleos 튜토리얼 페이지가 링크한 공식 샘플, `README.md`)
- [hw-access] https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/platform-api-architecture
- [security-privacy] https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/security-privacy
- [navi-intro] https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/intro
- [navi-app-guide] https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/navigation-app-development-guide
- [music-app-guide] https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/music-audio-app-developement-guide
- [ccp-guide] https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/ccp-rotary-controller-development
- [audio-focus] https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/audio-focus-development-guide/audio-focus-development
- [voice-preroll] https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/voice-guidance-preroll-silence-guide
- [app-security] https://document.pleos.ai/docs/pleos-only/app-registration-guide/app-security-guide
- [review-content] https://document.pleos.ai/docs/pleos-only/distribute/app-review-guide/review-content-safety
- [review-function] https://document.pleos.ai/docs/pleos-only/distribute/app-review-guide/review-functionality-performance-stability
- [review-registration] https://document.pleos.ai/docs/pleos-only/distribute/app-review-guide/review-app-registration-info
- [review-privacy] https://document.pleos.ai/docs/pleos-only/distribute/app-review-guide/review-user-privacy-security
- Pleos 외 출처 (Pleos [driver-distraction]이 직접 링크한 Google 공식 문서):
  - [aaos-dd] https://source.android.com/docs/automotive/driver_distraction/guidelines?hl=ko
