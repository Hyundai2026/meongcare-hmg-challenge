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
- Pleos 외 출처 (Pleos [driver-distraction]이 직접 링크한 Google 공식 문서):
  - [aaos-dd] https://source.android.com/docs/automotive/driver_distraction/guidelines?hl=ko
