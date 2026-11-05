# 반려견 프로필 형식 (v1)

보호자 웹, 차 앱(진료 카드), 나중의 서버(Firestore)가 **같이 따르는** 반려견 프로필 JSON 형식이다.
코드가 아니라 이 문서가 기준이다. 형식을 바꾸면 이 문서와 예시 JSON부터 고친다.

- 예시: [pet-profile.example.json](pet-profile.example.json). 컨셉 그림의 "보리"다. 웹 테스트가 이 파일을 읽어서, 문서와 코드가 어긋나면 테스트가 깨진다.
- 웹 구현: `web/src/domain/petProfile.ts`, `web/src/domain/petProfileJson.ts`
- 지금 쓰는 곳: 보호자 웹 (브라우저 localStorage), 차 앱 (진료 카드. 아직 웹과 연결되지 않아 이 예시와 같은 값의 Fake "보리"를 쓴다, 009).

## 필드

| 필드 | 타입 | 뜻 | 쓰는 쪽 제한 | 진료 카드 |
|---|---|---|---|---|
| `schemaVersion` | 숫자 | 형식 버전. 지금은 `1` | 정확히 1 | - |
| `name` | 문자열 | 이름 (필수) | 1~20자 | O |
| `breed` | 문자열 또는 `null` | 품종 | 1~30자. 비면 `null` | O |
| `sex` | `"male"`, `"female"`, `null` | 성별. `null`은 입력 안 함 | | O (♂/♀) |
| `birthDate` | `"YYYY-MM-DD"` 또는 `null` | 생년월일. 시간대 없는 날짜 | 오늘보다 뒤가 아님, 오늘에서 30년 전보다 앞이 아님 | O (나이로 바꿔서) |
| `birthDateIsEstimated` | 불리언 | 나이로 입력해서 생년월일이 추정값이면 `true` | `birthDate`가 `null`이면 `false` | O ("추정") |
| `photoDataUrl` | 문자열 또는 `null` | 사진. `data:image/jpeg;base64,`로 시작 | 긴 변 512px 이하 JPEG, 400,000자 이하 | 선택 |
| `medical.conditions` | 문자열 배열 | 주요 질환 | 항목 1~50자, 20개까지 | O |
| `medical.allergies` | 문자열 배열 | 알레르기 | 항목 1~50자, 20개까지 | O |
| `medical.medications` | `{ name, dosage }` 배열 | 복용 중인 약 | `name` 1~50자, `dosage` 0~50자, 20개까지 | O |
| `updatedAt` | 문자열 (ISO 8601, UTC) | 마지막 저장 시각 | | O ("정보 갱신일") |

- **글자 수**는 유니코드 코드 포인트 수다 (JavaScript `[...s].length`, Kotlin `s.codePointCount(0, s.length)`).
- 쓰는 쪽은 저장하기 전에 문자열의 앞뒤 공백을 지우고 NFC로 정규화한다 (macOS에서 온 풀어 쓴 한글도 같은 글자 수가 되게).
- `dosage`는 복용법 자유 입력이다 (예: "1일 1회"). 비어 있으면 `""`다.
- **빈 배열은 "없음"이 아니라 "등록하지 않음"이다.** 진료 카드에는 "등록된 정보 없음"으로 보인다. "알레르기 없음(확인함)"을 따로 나타내는 방법은 아직 없다 (아래 "나중 후보").

## 읽는 쪽 규칙

읽는 쪽은 **짐작하지 않는다.** 의료정보는 틀리게 보이는 것보다 "읽지 못했다"고 보이는 편이 안전하다.

| 받은 것 | 결과 |
|---|---|
| JSON이 아님, 객체가 아님 | 손상 (`corrupted`) |
| `schemaVersion`이 없거나 `1`이 아님 | 버전 문제 (`unsupported-version`). 읽지 않는다 |
| 필수 필드가 없음, 타입이 틀림 (위 표의 "타입") | 손상 |
| `name`이 빈 문자열 | 손상 |
| `birthDate`가 `YYYY-MM-DD`가 아니거나 없는 날짜 (예: `2023-02-30`) | 손상 |
| `photoDataUrl`이 `data:image/jpeg;base64,`로 시작하지 않음 | 손상 |
| `updatedAt`이 `YYYY-MM-DDTHH:mm:ssZ` 형식(UTC, 초 뒤 소수 자리는 있어도 됨)이 아니거나 없는 날짜·시각 | 손상 |
| 모르는 필드 | 무시하고 읽는다 |

- 글자 수·개수 제한은 **쓰는 쪽(입력 폼)이 지킨다.** 읽는 쪽은 위 표의 타입과 형식만 확인한다. 길이가 넘친 값은 위험하지 않아서 읽는다.

## 버전 규칙

- **선택 필드를 더하는 것**은 버전을 올리지 않는다. 읽는 쪽은 모르는 필드를 무시한다.
  - 그래서 예전 코드로 다시 저장하면 새 필드가 빠질 수 있다. 지금은 쓰는 곳이 하나라 받아들인다.
- 필드 이름·뜻·타입을 바꾸거나 지우면 버전을 올린다. 웹은 `vN → vN+1` 변환 함수를 둔다.
- 읽는 쪽은 자기가 아는 버전이 아니면 읽지 않는다.

## 나이 계산 (웹과 차 앱이 같은 규칙)

`오늘`은 기기의 지역 날짜다. 함수는 오늘을 인자로 받는다.

- 개월 수 = `(올해 - 태어난 해) × 12 + (이번 달 - 태어난 달)`. **오늘의 일이 태어난 일보다 작으면 1을 뺀다.**
- 세 = 개월 수 ÷ 12 (몫), 남은 개월 = 개월 수 % 12
- 표시

  | 경우 | 표시 | 예 |
  |---|---|---|
  | 1년 이상, 남은 개월 있음 | `N세 M개월` | `12세 3개월` |
  | 1년 이상, 남은 개월 0 | `N세` | `12세` |
  | 1개월 이상 1년 미만 | `M개월` | `3개월` |
  | 1개월 미만 | `1개월 미만` | |
  | 생년월일이 오늘보다 뒤 (기기 시계가 틀림) | 나이를 보이지 않는다 | |

- 예: 생년월일 `2014-08-01`, 오늘 `2026-11-05` → 147개월 → `12세 3개월`

### 나이로 입력할 때

보호자가 생년월일을 모르면 나이(세, 개월)로 입력한다.

- 입력 범위: 세 0~30, 개월 0~11 (정수). 합쳐서 30세 0개월을 넘지 않는다.
- 생년월일 = 오늘에서 그만큼 뺀 날. 그 달에 같은 날이 없으면 그 달 말일이다 (예: 3월 31일 - 1개월 = 2월 28일, 윤년이면 29일). `birthDateIsEstimated = true`.
- 이 규칙이면 같은 날 다시 계산했을 때 입력한 나이가 그대로 나온다.
- "30년 전"도 같은 방식으로 뺀다 (2월 29일 - 30년 = 2월 28일).

## 저장 위치

| 어디 | 키 / 경로 | 비고 |
|---|---|---|
| 보호자 웹 (지금) | localStorage `owner-web.pet-profile` | JSON 문자열 하나 (사진 포함). 앱 이름과 묶지 않아서 이름을 바꿔도 데이터가 그대로다 |
| 서버 (나중) | Firestore 문서 (미정) | 문의 A1 이후. docs/architecture.md "보호자 웹" |

## Kotlin 대응 (차 앱, 009에서 확정)

구현: `app/src/main/java/com/meongcare/app/domain/pet/PetProfile.kt`, 나이 계산 `PetAge.kt`.

| JSON | Kotlin |
|---|---|
| `schemaVersion` | 도메인 모델에 두지 않는다. JSON을 읽는 쪽(나중 서버 구현)이 1인지 확인하고, 아니면 `PetProfileLoad.Unavailable`을 낸다 |
| `name` | `String` |
| `breed` | `String?` |
| `sex` | `enum class Sex { MALE, FEMALE }`, `Sex?` |
| `birthDate` | `LocalDate?` |
| `birthDateIsEstimated` | `Boolean` |
| `photoDataUrl` | `String?` |
| `medical.conditions`, `medical.allergies` | `List<String>` |
| `medical.medications` | `List<Medication>` (`name: String`, `dosage: String`) |
| `updatedAt` | `Instant` |

- 출처는 `PetProfileSource.profile: Flow<PetProfileLoad>`다: `Loaded(profile)` / `NotRegistered`(보호자가 아직 등록 안 함) / `Unavailable`(읽지 못함. 짐작해서 보여 주지 않는다).

## 진료 카드 표시 규칙 (차 앱)

- 줄: 주요 증상(차에서 고른 것) / 기존 병력(= `conditions`) / 알레르기 / 복용 약물.
- 빈 목록은 "등록된 정보 없음"이다 (빈 배열 = 등록하지 않음). 증상을 고르지 않았으면 "고른 증상 없음".
- 항목의 앞뒤 공백을 지우고, 비면 건너뛴다. 그 밖에는 그대로 쓴다. 긴 목록도 자르지 않는다.
- 약은 "심장약 (1일 1회)", 복용법이 없으면 "심장약".
- 증상은 고른 순서가 아니라 프리셋 순서(`domain/card/Symptom.kt` 선언 순서)로 적는다.
- 프로필을 받지 못했으면(`NotRegistered`, `Unavailable`, 아직 받는 중) 의료정보 줄을 그리지 않고 안내 한 줄만 보인다.
- 나이는 위 "나이 계산" 규칙대로 "12세 3개월"이고, 추정이면 "(추정)"을 붙인다. 정보 갱신일은 `updatedAt`을 기기 시간대 날짜로 바꾼 것이다.

## 나중 후보 (지금 하지 않음)

- 선택 필드로 더하기 (버전 그대로): 체중, 중성화 여부, 마이크로칩 번호, "알레르기 없음 확인함" 같은 확인 표시
- 버전을 올려야 하는 것 (v2): 사진을 서버 저장소로 옮기며 `photoDataUrl` → `photoUrl`, 여러 마리를 위한 `id`
  - `crypto.randomUUID()`는 보안 컨텍스트(HTTPS, localhost)에서만 있다. 폰에서 `http://<PC IP>:5173`으로 열면 없다.
